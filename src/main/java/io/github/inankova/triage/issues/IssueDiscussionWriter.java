package io.github.inankova.triage.issues;

import static io.github.inankova.triage.issues.SqlTimestamps.utc;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Writes the discussion around an issue: its comments and the issues and pull requests that mentioned it (P1.6). The
 * issue itself must already be stored; rows are linked to it through its GitHub id.
 *
 * <p>Saving the same data again changes nothing (idempotent), like {@link IssueWriter}.
 */
@Component
public class IssueDiscussionWriter {

    /** An edited comment keeps its GitHub id, so the upsert overwrites the body. */
    private static final String UPSERT_COMMENT = """
            INSERT INTO issue_comment (issue_id, github_id, author_login, author_association, body, created_at)
            VALUES ((SELECT id FROM issue WHERE github_id = :issueGithubId),
                    :githubId, :authorLogin, :authorAssociation, :body, :createdAt)
            ON CONFLICT (github_id) DO UPDATE SET
                author_association = EXCLUDED.author_association,
                body               = EXCLUDED.body
            """;

    /** One row per mentioning source; a source that mentions the issue again is already recorded. */
    private static final String INSERT_CROSS_REFERENCE = """
            INSERT INTO issue_cross_reference (issue_id, source_repo, source_number, source_is_pull_request, created_at)
            VALUES ((SELECT id FROM issue WHERE github_id = :issueGithubId),
                    :sourceRepo, :sourceNumber, :sourceIsPullRequest, :createdAt)
            ON CONFLICT (issue_id, source_repo, source_number) DO NOTHING
            """;

    private final JdbcClient jdbc;

    public IssueDiscussionWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void saveComments(long issueGithubId, List<CommentSnapshot> comments) {
        for (CommentSnapshot comment : comments) {
            jdbc.sql(UPSERT_COMMENT)
                    .param("issueGithubId", issueGithubId)
                    .param("githubId", comment.githubId())
                    .param("authorLogin", comment.authorLogin())
                    .param("authorAssociation", comment.authorAssociation())
                    .param("body", comment.body())
                    .param("createdAt", utc(comment.createdAt()))
                    .update();
        }
    }

    public void saveCrossReferences(long issueGithubId, List<CrossReferenceSnapshot> references) {
        for (CrossReferenceSnapshot reference : references) {
            jdbc.sql(INSERT_CROSS_REFERENCE)
                    .param("issueGithubId", issueGithubId)
                    .param("sourceRepo", reference.sourceRepo())
                    .param("sourceNumber", reference.sourceNumber())
                    .param("sourceIsPullRequest", reference.sourceIsPullRequest())
                    .param("createdAt", utc(reference.createdAt()))
                    .update();
        }
    }
}
