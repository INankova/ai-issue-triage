package io.github.inankova.triage.ingestion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Rate limits and retries (ROADMAP P1.3). The clock is fixed and sleeping is recorded instead of done, so a wait of
 * minutes takes no time in the test. Rate-limit responses follow the format in GitHub's documentation ("Rate limits
 * for the REST API"); a real one can't be recorded without exhausting the limit.
 */
class GitHubClientRetryTests {

    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

    private static final String REPO = "spring-projects/spring-boot";

    private static final String FIRST_PAGE = "https://api.github.com/repos/spring-projects/spring-boot/issues"
            + "?state=all&sort=created&direction=desc&per_page=100";

    private static final String PRIMARY_LIMIT_BODY = """
            {"message": "API rate limit exceeded for user ID 1001.",
             "documentation_url": "https://docs.github.com/rest/overview/rate-limits-for-the-rest-api"}
            """;

    private static final String SECONDARY_LIMIT_BODY = """
            {"message": "You have exceeded a secondary rate limit. Please wait a few minutes before you try again.",
             "documentation_url": "https://docs.github.com/rest/overview/rate-limits-for-the-rest-api"}
            """;

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer github =
            MockRestServiceServer.bindTo(builder).build();

    private final List<Duration> sleeps = new ArrayList<>();

    private final GitHubClient client = new GitHubClient(
            builder,
            new GitHubProperties(URI.create("https://api.github.com"), "test-token"),
            Clock.fixed(NOW, ZoneOffset.UTC),
            sleeps::add,
            Duration.ofMillis(1));

    @Test
    void waitsUntilThePrimaryRateLimitResetsThenRecovers() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-ratelimit-limit", "5000")
                        .header("x-ratelimit-remaining", "0")
                        .header(
                                "x-ratelimit-reset",
                                String.valueOf(NOW.plusSeconds(120).getEpochSecond()))
                        .body(PRIMARY_LIMIT_BODY));
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        List<GitHubIssue> issues = client.fetchIssues(REPO, 500);

        assertThat(issues).hasSize(3);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(121)); // until the reset, plus a second for clock skew
        github.verify();
    }

    @Test
    void honoursRetryAfterOnASecondaryRateLimitThenRecovers() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("retry-after", "30")
                        .body(SECONDARY_LIMIT_BODY));
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchIssues(REPO, 500)).hasSize(3);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(30));
        github.verify();
    }

    @Test
    void waitsAMinuteOnASecondaryRateLimitWithoutRetryAfter() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SECONDARY_LIMIT_BODY));
        github.expect(requestTo(FIRST_PAGE)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.fetchIssues(REPO, 500)).isEmpty();
        assertThat(sleeps).containsExactly(Duration.ofMinutes(1));
    }

    @Test
    void failsAtOnceOnA403ThatIsNotARateLimit() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Resource not accessible by personal access token\"}"));

        assertThatThrownBy(() -> client.fetchIssues(REPO, 500))
                .isInstanceOfSatisfying(
                        GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(403));
        assertThat(sleeps).isEmpty();
        github.verify(); // exactly one request: no retry
    }

    @Test
    void givesUpWhenStillRateLimitedAfterThreeWaits() {
        github.expect(times(4), requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("retry-after", "5")
                        .body(SECONDARY_LIMIT_BODY));

        assertThatThrownBy(() -> client.fetchIssues(REPO, 500)).isInstanceOf(GitHubRateLimitException.class);
        assertThat(sleeps).hasSize(3);
        github.verify();
    }

    @Test
    void retriesAServerErrorThenRecovers() {
        github.expect(requestTo(FIRST_PAGE)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchIssues(REPO, 500)).hasSize(3);
        github.verify();
    }

    @Test
    void retriesATimeoutThenRecovers() {
        github.expect(requestTo(FIRST_PAGE)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchIssues(REPO, 500)).hasSize(3);
        github.verify();
    }

    @Test
    void givesUpAfterThreeAttemptsOnServerErrors() {
        github.expect(times(3), requestTo(FIRST_PAGE)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetchIssues(REPO, 500))
                .isInstanceOfSatisfying(
                        GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(503));
        github.verify();
    }

    private static ClassPathResource fixture(String name) {
        return new ClassPathResource("fixtures/github/" + name);
    }
}
