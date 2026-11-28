package io.github.inankova.triage.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.inankova.triage.TestcontainersConfiguration;
import io.github.inankova.triage.ingestion.internal.GitHubClient;
import io.github.inankova.triage.ingestion.internal.GitHubProperties;
import io.github.inankova.triage.issues.IssueDiscussionWriter;
import io.github.inankova.triage.issues.IssueRepository;
import io.github.inankova.triage.issues.IssueSnapshot;
import io.github.inankova.triage.issues.IssueState;
import io.github.inankova.triage.issues.IssueWriter;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Comments and cross-references are fetched only for duplicates (ROADMAP P1.6). Issue #52634 was closed as a
 * duplicate; #52939 is open and must cost no request. The responses for #52634 are recorded and anonymised.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, IssueWriter.class, IssueDiscussionWriter.class})
class DuplicateDiscussionSyncTests {

    private static final String REPO = "spring-projects/spring-boot";

    private static final String ISSUES = "https://api.github.com/repos/spring-projects/spring-boot/issues/";

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer github =
            MockRestServiceServer.bindTo(builder).build();

    private final IssueWriter issueWriter;

    private final JdbcClient jdbc;

    private final DuplicateDiscussionSync sync;

    DuplicateDiscussionSyncTests(
            @Autowired IssueWriter issueWriter,
            @Autowired IssueRepository issues,
            @Autowired IssueDiscussionWriter discussionWriter,
            @Autowired JdbcClient jdbc) {
        this.issueWriter = issueWriter;
        this.jdbc = jdbc;
        GitHubClient client =
                new GitHubClient(builder, new GitHubProperties(URI.create("https://api.github.com"), "test-token"));
        this.sync = new DuplicateDiscussionSync(client, issues, discussionWriter);
    }

    @Test
    void fetchesCommentsAndCrossReferencesOnlyForDuplicatesAndCanRunTwice() {
        issueWriter.upsert(issue(52634, IssueState.CLOSED, "duplicate", List.of("status: superseded")));
        issueWriter.upsert(issue(52939, IssueState.OPEN, null, List.of("status: waiting-for-triage")));
        for (int run = 0; run < 2; run++) {
            github.expect(requestTo(ISSUES + "52634/comments?per_page=100"))
                    .andRespond(withSuccess(fixture("comments-52634.json"), MediaType.APPLICATION_JSON));
            github.expect(requestTo(ISSUES + "52634/timeline?per_page=100"))
                    .andRespond(withSuccess(fixture("timeline-52634.json"), MediaType.APPLICATION_JSON));
        }

        DiscussionSyncResult first = sync.syncDuplicates(REPO);
        DiscussionSyncResult second = sync.syncDuplicates(REPO);

        // 4 comments; 3 of the 19 timeline events are cross-references
        assertThat(first).isEqualTo(new DiscussionSyncResult(1, 4, 3));
        assertThat(second).isEqualTo(new DiscussionSyncResult(1, 4, 3));
        assertThat(count("issue_comment")).isEqualTo(4);
        assertThat(jdbc.sql("SELECT source_repo || '#' || source_number FROM issue_cross_reference ORDER BY created_at")
                        .query(String.class)
                        .list())
                .containsExactly(
                        "apache/grails-core#16871", "apache/grails-core#16878", "spring-projects/spring-boot#52635");
        github.verify(); // no request was made for #52939
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    private static IssueSnapshot issue(int number, IssueState state, String stateReason, List<String> labels) {
        return new IssueSnapshot(
                9_000_000L + number,
                REPO,
                number,
                "Example title for #" + number,
                "Example body for #" + number + ".",
                state,
                stateReason,
                labels,
                "example-user-1",
                false,
                "https://github.com/spring-projects/spring-boot/issues/" + number,
                4,
                Instant.parse("2026-11-06T03:00:00Z"),
                Instant.parse("2026-11-13T08:27:36Z"),
                state == IssueState.CLOSED ? Instant.parse("2026-11-13T08:27:36Z") : null);
    }

    private static ClassPathResource fixture(String name) {
        return new ClassPathResource("fixtures/github/" + name);
    }
}
