package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Reads issues from the GitHub REST API (DESIGN §12).
 *
 * <p>GitHub splits long lists into pages and puts the address of the next page in the {@code Link} response header.
 * That address uses an opaque cursor, so the client follows it as-is instead of building {@code page=2} itself.
 *
 * <p>Each page request is protected twice (ROADMAP P1.3):
 *
 * <ul>
 *   <li>Rate limits: when GitHub answers 403 or 429 because of a rate limit, the client waits as long as GitHub asks
 *       (the {@code retry-after} or {@code x-ratelimit-reset} header) and tries again, up to three times.
 *   <li>Transient failures: a 5xx response or a network error such as a timeout is retried by Resilience4j, three
 *       attempts in total, waiting 1 s and then 2 s.
 * </ul>
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private static final int PAGE_SIZE = 100; // the maximum GitHub allows

    private static final int MAX_ATTEMPTS = 3; // for 5xx responses and network errors

    private static final int MAX_RATE_LIMIT_WAITS = 3;

    /** Added to a rate-limit reset time in case our clock and GitHub's differ slightly. */
    private static final Duration CLOCK_SKEW_MARGIN = Duration.ofSeconds(1);

    /** GitHub's advice for a secondary rate limit that doesn't say how long to wait. */
    private static final Duration DEFAULT_RATE_LIMIT_WAIT = Duration.ofMinutes(1);

    /** An owner and a repository name, such as {@code spring-projects/spring-boot}. */
    private static final Pattern REPO = Pattern.compile("([A-Za-z0-9-]+)/([A-Za-z0-9._-]+)");

    /** The next-page entry in a Link header: {@code <https://...>; rel="next"}. */
    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUE_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    private final URI baseUrl;

    private final Clock clock;

    private final Sleeper sleeper;

    private final Retry retry;

    @Autowired
    public GitHubClient(RestClient.Builder builder, GitHubProperties properties) {
        this(builder, properties, Clock.systemUTC(), Thread::sleep, Duration.ofSeconds(1));
    }

    /** For tests: a fixed clock, a sleeper that only records, and a tiny backoff. */
    GitHubClient(
            RestClient.Builder builder,
            GitHubProperties properties,
            Clock clock,
            Sleeper sleeper,
            Duration initialBackoff) {
        this.baseUrl = properties.baseUrl();
        this.clock = clock;
        this.sleeper = sleeper;
        this.restClient = builder.defaultHeaders(headers -> {
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set("X-GitHub-Api-Version", "2022-11-28");
                    if (properties.hasToken()) {
                        headers.setBearerAuth(properties.token());
                    }
                })
                .build();
        this.retry = Retry.of(
                "github",
                RetryConfig.custom()
                        .maxAttempts(MAX_ATTEMPTS)
                        .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoff, 2.0))
                        .retryOnException(GitHubClient::isTransient)
                        .build());
        retry.getEventPublisher()
                .onRetry(event -> log.warn(
                        "GitHub request failed ({}), retry {} in {} ms",
                        event.getLastThrowable(),
                        event.getNumberOfRetryAttempts(),
                        event.getWaitInterval().toMillis()));
    }

    /**
     * Fetches issues of a repository, newest first, following pages until {@code limit} issues are collected or
     * there are no more pages. Includes pull requests, which GitHub lists on the same endpoint.
     *
     * @param repo owner and name, for example {@code spring-projects/spring-boot}
     * @param limit the most issues to return
     * @throws GitHubApiException if GitHub answers with an error status, after any retries
     */
    public List<GitHubIssue> fetchIssues(String repo, int limit) {
        List<GitHubIssue> issues = new ArrayList<>();
        URI next = firstPage(repo);
        while (next != null && issues.size() < limit) {
            Page page = fetchPage(next);
            issues.addAll(page.issues());
            next = page.next();
        }
        return List.copyOf(issues.subList(0, Math.min(limit, issues.size())));
    }

    private URI firstPage(String repo) {
        Matcher matcher = REPO.matcher(repo);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected a repository as owner/name, got: " + repo);
        }
        return UriComponentsBuilder.fromUri(baseUrl)
                .path("/repos/{owner}/{name}/issues")
                .queryParam("state", "all") // closed issues carry the maintainers' final labels
                .queryParam("sort", "created")
                .queryParam("direction", "desc")
                .queryParam("per_page", PAGE_SIZE)
                .build(matcher.group(1), matcher.group(2));
    }

    /** Resilience4j retries transient failures; rate limits are handled one level down. */
    private Page fetchPage(URI uri) {
        return retry.executeSupplier(() -> fetchPageWithinRateLimits(uri));
    }

    private Page fetchPageWithinRateLimits(URI uri) {
        for (int waits = 0; ; waits++) {
            try {
                return requestPage(uri);
            } catch (GitHubRateLimitException e) {
                if (waits == MAX_RATE_LIMIT_WAITS) {
                    throw e;
                }
                log.warn(
                        "GitHub rate limit reached ({}), waiting {} s",
                        e.getMessage(),
                        e.waitTime().toSeconds());
                sleep(e.waitTime());
            }
        }
    }

    /** Requests one page. The URI is used exactly as given, so the cursor in a next link isn't re-encoded. */
    private Page requestPage(URI uri) {
        return restClient.get().uri(uri).exchange((request, response) -> {
            if (response.getStatusCode().isError()) {
                int status = response.getStatusCode().value();
                String message = errorMessage(response);
                Duration wait = rateLimitWait(status, response.getHeaders(), message);
                if (wait != null) {
                    throw new GitHubRateLimitException(status, message, wait);
                }
                throw new GitHubApiException(status, message);
            }
            List<GitHubIssue> issues = response.bodyTo(ISSUE_LIST);
            return new Page(issues == null ? List.of() : issues, nextLink(response.getHeaders()));
        });
    }

    /**
     * How long GitHub asks us to wait, or {@code null} if the error is not a rate limit. Follows GitHub's guide
     * "Rate limits for the REST API": a {@code retry-after} header first, then {@code x-ratelimit-remaining: 0} with
     * the reset time, otherwise at least a minute. A 403 without any of these means "no permission", not a limit.
     */
    private Duration rateLimitWait(int status, HttpHeaders headers, String message) {
        if (status != 403 && status != 429) {
            return null;
        }
        String retryAfter = headers.getFirst("retry-after");
        if (retryAfter != null) {
            return Duration.ofSeconds(Long.parseLong(retryAfter.trim()));
        }
        String reset = headers.getFirst("x-ratelimit-reset");
        if ("0".equals(headers.getFirst("x-ratelimit-remaining")) && reset != null) {
            Instant resetAt = Instant.ofEpochSecond(Long.parseLong(reset.trim()));
            Duration untilReset = Duration.between(clock.instant(), resetAt).plus(CLOCK_SKEW_MARGIN);
            return untilReset.compareTo(CLOCK_SKEW_MARGIN) < 0 ? CLOCK_SKEW_MARGIN : untilReset;
        }
        boolean mentionsRateLimit =
                message != null && message.toLowerCase(Locale.ROOT).contains("rate limit");
        return status == 429 || mentionsRateLimit ? DEFAULT_RATE_LIMIT_WAIT : null;
    }

    private void sleep(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // let the caller see that the thread was asked to stop
            throw new IllegalStateException("Interrupted while waiting for a GitHub rate limit", e);
        }
    }

    /** Worth another try: GitHub had a server error, or the request didn't complete (timeout, connection reset). */
    private static boolean isTransient(Throwable e) {
        return (e instanceof GitHubApiException api && api.status() >= 500) || e instanceof ResourceAccessException;
    }

    /** GitHub explains errors in a JSON {@code message}; a proxy in front of it may send HTML instead. */
    private static String errorMessage(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            GitHubError error = response.bodyTo(GitHubError.class);
            return error == null ? null : error.message();
        } catch (RestClientException notJson) {
            return null;
        }
    }

    private static URI nextLink(HttpHeaders headers) {
        String link = headers.getFirst(HttpHeaders.LINK);
        if (link == null) {
            return null;
        }
        Matcher matcher = NEXT_LINK.matcher(link);
        return matcher.find() ? URI.create(matcher.group(1)) : null;
    }

    /** One page of results; {@code next} is null on the last page. */
    private record Page(List<GitHubIssue> issues, URI next) {}

    /** Waits for a while; tests replace it so that waiting minutes for a rate limit takes no time. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubError(String message) {}
}
