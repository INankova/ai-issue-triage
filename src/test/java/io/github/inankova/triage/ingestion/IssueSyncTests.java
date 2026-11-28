package io.github.inankova.triage.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.inankova.triage.TestcontainersConfiguration;
import io.github.inankova.triage.ingestion.internal.GitHubClient;
import io.github.inankova.triage.ingestion.internal.GitHubProperties;
import io.github.inankova.triage.issues.IssueRepository;
import io.github.inankova.triage.issues.IssueWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Import and incremental sync end to end, with GitHub replaced by recorded responses and a real PostgreSQL in
 * Testcontainers (ROADMAP P1.4). The recorded pages hold five entries, two of them pull requests.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, IssueWriter.class})
class IssueSyncTests {

    private static final String REPO = "spring-projects/spring-boot";

    private static final String RECENT_FIRST_PAGE = "https://api.github.com/repos/spring-projects/spring-boot/issues"
            + "?state=all&sort=created&direction=desc&per_page=100";

    private static final String RECENT_SECOND_PAGE = "https://api.github.com/repositories/6296790/issues"
            + "?state=all&sort=created&direction=desc&per_page=100"
            + "&after=Y3Vyc29yOnYyOpLPAAABofhs6tDPAAAAAYP7m_M%3D&page=2";

    /** The newest update among the imported issues is #52951's, at 2026-11-27T10:46:48Z. */
    private static final String CHANGES_SINCE_NEWEST_UPDATE = "https://api.github.com/repos/spring-projects/spring-boot"
            + "/issues?state=all&sort=updated&direction=asc&since=2026-11-27T10:46:48Z&per_page=100";

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer github =
            MockRestServiceServer.bindTo(builder).build();

    private final JdbcClient jdbc;

    private final IssueSync sync;

    IssueSyncTests(@Autowired IssueWriter writer, @Autowired IssueRepository issues, @Autowired JdbcClient jdbc) {
        this.jdbc = jdbc;
        GitHubClient client =
                new GitHubClient(builder, new GitHubProperties(URI.create("https://api.github.com"), "test-token"));
        this.sync = new IssueSync(client, writer, issues);
    }

    @Test
    void importSkipsPullRequestsAndImportingTwiceKeepsTheRowCount() {
        expectRecentIssues();
        expectRecentIssues();

        SyncResult first = sync.importRecent(REPO, 500);
        SyncResult second = sync.importRecent(REPO, 500);

        assertThat(first).isEqualTo(new SyncResult(5, 3));
        assertThat(second).isEqualTo(new SyncResult(5, 3));
        assertThat(rowCount()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT number FROM issue ORDER BY number")
                        .query(Integer.class)
                        .list())
                .containsExactly(52848, 52939, 52951);
        github.verify();
    }

    @Test
    void syncFetchesChangesSinceTheNewestUpdateAndGivesAnEditedIssueANewHash() {
        expectRecentIssues();
        String editedPage = fixtureText("issues-page-1.json")
                .replace("Example title for #52939", "Example title for #52939 (edited)");
        github.expect(requestTo(CHANGES_SINCE_NEWEST_UPDATE))
                .andRespond(withSuccess(editedPage, MediaType.APPLICATION_JSON));

        sync.importRecent(REPO, 500);
        String editedHashBefore = hashOf(52939);
        String untouchedHashBefore = hashOf(52951);

        SyncResult result = sync.syncChanges(REPO);

        assertThat(result).isEqualTo(new SyncResult(3, 2)); // one of the three entries is a pull request
        assertThat(rowCount()).isEqualTo(3);
        assertThat(hashOf(52939)).isNotEqualTo(editedHashBefore);
        assertThat(hashOf(52951)).isEqualTo(untouchedHashBefore);
        github.verify();
    }

    @Test
    void syncNeedsAnImportFirst() {
        assertThatThrownBy(() -> sync.syncChanges(REPO)).isInstanceOf(IllegalStateException.class);
        github.verify(); // nothing was requested
    }

    private void expectRecentIssues() {
        github.expect(requestTo(RECENT_FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, "<" + RECENT_SECOND_PAGE + ">; rel=\"next\""));
        github.expect(requestTo(RECENT_SECOND_PAGE))
                .andRespond(withSuccess(fixture("issues-page-2.json"), MediaType.APPLICATION_JSON));
    }

    private int rowCount() {
        return jdbc.sql("SELECT count(*) FROM issue").query(Integer.class).single();
    }

    private String hashOf(int number) {
        return jdbc.sql("SELECT content_hash FROM issue WHERE number = ?")
                .param(number)
                .query(String.class)
                .single();
    }

    private static ClassPathResource fixture(String name) {
        return new ClassPathResource("fixtures/github/" + name);
    }

    private static String fixtureText(String name) {
        try {
            return fixture(name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
