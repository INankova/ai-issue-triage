package io.github.inankova.triage.issues;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.inankova.triage.TestcontainersConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Comments and cross-references of an issue, stored for the duplicate ground truth (ROADMAP P1.6). */
@DataJpaTest
@Import({TestcontainersConfiguration.class, IssueWriter.class, IssueDiscussionWriter.class})
class IssueDiscussionWriterTests {

    private static final long ISSUE_GITHUB_ID = 9_000_101L;

    private final IssueWriter issueWriter;

    private final IssueDiscussionWriter writer;

    private final JdbcClient jdbc;

    IssueDiscussionWriterTests(
            @Autowired IssueWriter issueWriter, @Autowired IssueDiscussionWriter writer, @Autowired JdbcClient jdbc) {
        this.issueWriter = issueWriter;
        this.writer = writer;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void storeTheIssue() {
        issueWriter.upsert(new IssueSnapshot(
                ISSUE_GITHUB_ID,
                "spring-projects/spring-boot",
                101,
                "Startup fails",
                "Steps to reproduce",
                IssueState.CLOSED,
                "duplicate",
                List.of("status: duplicate"),
                "example-user-1",
                false,
                "https://github.com/spring-projects/spring-boot/issues/101",
                2,
                Instant.parse("2025-03-01T10:15:30Z"),
                Instant.parse("2025-03-04T08:00:00Z"),
                Instant.parse("2025-03-04T08:00:00Z")));
    }

    @Test
    void savesCommentsLinkedToTheirIssue() {
        writer.saveComments(ISSUE_GITHUB_ID, List.of(comment(1, "Example comment 1."), comment(2, "Duplicate of #99")));

        assertThat(jdbc.sql("""
                        SELECT c.author_association || '|' || c.body
                        FROM issue_comment c JOIN issue i ON i.id = c.issue_id
                        WHERE i.github_id = 9000101
                        ORDER BY c.created_at
                        """).query(String.class).list())
                .containsExactly("MEMBER|Example comment 1.", "MEMBER|Duplicate of #99");
    }

    @Test
    void savingCommentsAgainUpdatesAnEditedBodyWithoutDuplicatingRows() {
        writer.saveComments(ISSUE_GITHUB_ID, List.of(comment(1, "Example comment 1.")));

        writer.saveComments(ISSUE_GITHUB_ID, List.of(comment(1, "Example comment 1, edited.")));

        assertThat(jdbc.sql("SELECT body FROM issue_comment")
                        .query(String.class)
                        .list())
                .containsExactly("Example comment 1, edited.");
    }

    @Test
    void savesEachCrossReferenceSourceOnce() {
        CrossReferenceSnapshot fromPullRequest = new CrossReferenceSnapshot(
                "spring-projects/spring-boot", 52635, true, Instant.parse("2025-03-02T10:00:00Z"));
        CrossReferenceSnapshot fromOtherRepo =
                new CrossReferenceSnapshot("apache/grails-core", 16871, true, Instant.parse("2025-03-02T11:00:00Z"));

        writer.saveCrossReferences(ISSUE_GITHUB_ID, List.of(fromPullRequest, fromOtherRepo, fromPullRequest));
        writer.saveCrossReferences(ISSUE_GITHUB_ID, List.of(fromPullRequest));

        assertThat(jdbc.sql("""
                        SELECT source_repo || '#' || source_number || '|' || source_is_pull_request
                        FROM issue_cross_reference
                        ORDER BY created_at
                        """).query(String.class).list())
                .containsExactly("spring-projects/spring-boot#52635|true", "apache/grails-core#16871|true");
    }

    private static CommentSnapshot comment(long id, String body) {
        return new CommentSnapshot(
                id,
                "example-user-2",
                "MEMBER",
                body,
                Instant.parse("2025-03-02T00:00:00Z").plusSeconds(id));
    }
}
