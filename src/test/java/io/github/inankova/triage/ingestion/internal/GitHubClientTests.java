package io.github.inankova.triage.ingestion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests the client against responses recorded from the real GitHub API on 2026-11-27, anonymised: titles, bodies and
 * human logins are replaced, the structure is real. MockRestServiceServer stands in for GitHub, so no request leaves
 * the machine (ROADMAP P1.2).
 */
class GitHubClientTests {

    private static final String REPO = "spring-projects/spring-boot";

    private static final String FIRST_PAGE = "https://api.github.com/repos/spring-projects/spring-boot/issues"
            + "?state=all&sort=created&direction=desc&per_page=100";

    /** From the recorded Link header: GitHub pages with an opaque cursor and its own URL form, so we follow it. */
    private static final String SECOND_PAGE = "https://api.github.com/repositories/6296790/issues"
            + "?state=all&sort=created&direction=desc&per_page=100"
            + "&after=Y3Vyc29yOnYyOpLPAAABofhs6tDPAAAAAYP7m_M%3D&page=2";

    private static final String PAGE_1_LINK = "<" + SECOND_PAGE + ">; rel=\"next\"";

    /** A last page links back to the previous one but has no rel="next". */
    private static final String PAGE_2_LINK = "<https://api.github.com/repositories/6296790/issues"
            + "?state=all&sort=created&direction=desc&per_page=100"
            + "&page=1&before=Y3Vyc29yOnYyOpLPAAABofhsuAjPAAAAAYP7jzY%3D>; rel=\"prev\"";

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer github =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void fetchesIssuesAcrossTwoPagesByFollowingTheLinkHeader() {
        github.expect(requestTo(FIRST_PAGE))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, PAGE_1_LINK));
        github.expect(requestTo(SECOND_PAGE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(fixture("issues-page-2.json"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, PAGE_2_LINK));

        List<GitHubIssue> issues = client("test-token").fetchIssues(REPO, 500);

        assertThat(issues).extracting(GitHubIssue::number).containsExactly(52951, 52939, 52925, 52848, 52822);
        github.verify();
    }

    @Test
    void stopsBeforeTheNextPageOnceTheLimitIsReached() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, PAGE_1_LINK));

        List<GitHubIssue> issues = client("test-token").fetchIssues(REPO, 2);

        assertThat(issues).extracting(GitHubIssue::number).containsExactly(52951, 52939);
        github.verify(); // only one request was expected: following the next link would have failed the test
    }

    @Test
    void fetchesIssuesUpdatedSinceAGivenTimeOldestUpdateFirst() {
        github.expect(requestTo("https://api.github.com/repos/spring-projects/spring-boot/issues"
                        + "?state=all&sort=updated&direction=asc&since=2026-11-27T10:46:48Z&per_page=100"))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        List<GitHubIssue> issues =
                client("test-token").fetchIssuesUpdatedSince(REPO, Instant.parse("2026-11-27T10:46:48Z"));

        assertThat(issues).hasSize(3);
        github.verify();
    }

    @Test
    void mapsTheFieldsWeUseFromARecordedResponse() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON));

        List<GitHubIssue> issues = client("test-token").fetchIssues(REPO, 500);

        GitHubIssue closed = issues.get(0);
        assertThat(closed.id()).isEqualTo(6_644_879_444L);
        assertThat(closed.number()).isEqualTo(52951);
        assertThat(closed.title()).isEqualTo("Example title for #52951");
        assertThat(closed.body()).isEqualTo("Example body for #52951.");
        assertThat(closed.state()).isEqualTo("closed");
        assertThat(closed.stateReason()).isEqualTo("completed");
        assertThat(closed.labels())
                .extracting(GitHubIssue.Label::name)
                .containsExactly("type: task", "status: forward-port");
        assertThat(closed.user().login()).isEqualTo("example-user-1");
        assertThat(closed.user().isBot()).isFalse();
        assertThat(closed.htmlUrl()).isEqualTo("https://github.com/spring-projects/spring-boot/issues/52951");
        assertThat(closed.comments()).isZero();
        assertThat(closed.createdAt()).isEqualTo(Instant.parse("2026-11-27T10:46:27Z"));
        assertThat(closed.updatedAt()).isEqualTo(Instant.parse("2026-11-27T10:46:48Z"));
        assertThat(closed.closedAt()).isEqualTo(Instant.parse("2026-11-27T10:46:48Z"));
        assertThat(closed.pullRequest()).isNull();

        GitHubIssue open = issues.get(1);
        assertThat(open.state()).isEqualTo("open");
        assertThat(open.stateReason()).isNull();
        assertThat(open.closedAt()).isNull();

        assertThat(issues.get(2).pullRequest()).isNotNull();
    }

    @Test
    void recognisesBotAuthorsAndEmptyBodies() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-2.json"), MediaType.APPLICATION_JSON));

        List<GitHubIssue> issues = client("test-token").fetchIssues(REPO, 500);

        assertThat(issues.get(0).body()).isNull();
        assertThat(issues.get(1).user().login()).isEqualTo("dependabot[bot]");
        assertThat(issues.get(1).user().isBot()).isTrue();
    }

    @Test
    void turnsAnErrorResponseIntoGitHubApiException() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fixture("error-401.json")));

        assertThatThrownBy(() -> client("test-token").fetchIssues(REPO, 500))
                .isInstanceOfSatisfying(GitHubApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(401);
                    assertThat(e.getMessage()).isEqualTo("GitHub API returned HTTP 401: Bad credentials");
                });
    }

    @Test
    void sendsNoAuthorizationHeaderWithoutAToken() {
        github.expect(requestTo(FIRST_PAGE))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // A blank token is what ${GITHUB_TOKEN:} gives when the variable isn't set.
        assertThat(client("").fetchIssues(REPO, 500)).isEmpty();
    }

    @Test
    void rejectsARepoThatIsNotOwnerSlashName() {
        GitHubClient client = client("test-token");

        assertThatThrownBy(() -> client.fetchIssues("spring-boot", 500)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.fetchIssues("spring-projects/../admin", 500))
                .isInstanceOf(IllegalArgumentException.class);
        github.verify(); // nothing was sent
    }

    private GitHubClient client(String token) {
        return new GitHubClient(builder, new GitHubProperties(URI.create("https://api.github.com"), token));
    }

    private static ClassPathResource fixture(String name) {
        return new ClassPathResource("fixtures/github/" + name);
    }
}
