package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Reads issues from the GitHub REST API (DESIGN §12).
 *
 * <p>GitHub splits long lists into pages and puts the address of the next page in the {@code Link} response header.
 * That address uses an opaque cursor, so the client follows it as-is instead of building {@code page=2} itself.
 */
@Component
public class GitHubClient {

    private static final int PAGE_SIZE = 100; // the maximum GitHub allows

    /** An owner and a repository name, such as {@code spring-projects/spring-boot}. */
    private static final Pattern REPO = Pattern.compile("([A-Za-z0-9-]+)/([A-Za-z0-9._-]+)");

    /** The next-page entry in a Link header: {@code <https://...>; rel="next"}. */
    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUE_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    private final URI baseUrl;

    public GitHubClient(RestClient.Builder builder, GitHubProperties properties) {
        this.baseUrl = properties.baseUrl();
        this.restClient = builder.defaultHeaders(headers -> {
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set("X-GitHub-Api-Version", "2022-11-28");
                    if (properties.hasToken()) {
                        headers.setBearerAuth(properties.token());
                    }
                })
                .build();
    }

    /**
     * Fetches issues of a repository, newest first, following pages until {@code limit} issues are collected or
     * there are no more pages. Includes pull requests, which GitHub lists on the same endpoint.
     *
     * @param repo owner and name, for example {@code spring-projects/spring-boot}
     * @param limit the most issues to return
     * @throws GitHubApiException if GitHub answers with an error status
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

    /** Requests one page. The URI is used exactly as given, so the cursor in a next link isn't re-encoded. */
    private Page fetchPage(URI uri) {
        return restClient.get().uri(uri).exchange((request, response) -> {
            if (response.getStatusCode().isError()) {
                throw new GitHubApiException(response.getStatusCode().value(), errorMessage(response));
            }
            List<GitHubIssue> issues = response.bodyTo(ISSUE_LIST);
            return new Page(issues == null ? List.of() : issues, nextLink(response.getHeaders()));
        });
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubError(String message) {}
}
