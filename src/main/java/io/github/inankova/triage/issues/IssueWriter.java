package io.github.inankova.triage.issues;

import static io.github.inankova.triage.issues.SqlTimestamps.utc;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The only code that writes the {@code issue} table; everything else reads through {@link IssueRepository}.
 *
 * <p>Writes are upserts keyed on GitHub's id: a new issue is inserted, a known one is overwritten with GitHub's
 * current data. Running the same sync twice therefore leaves the table exactly as one run would (idempotent).
 */
@Component
public class IssueWriter {

    private static final String UPSERT = """
            INSERT INTO issue (github_id, repo, number, title, body, state, state_reason, labels,
                               author_login, author_is_bot, html_url, comments_count,
                               created_at, updated_at, closed_at, content_hash, synced_at)
            VALUES (:githubId, :repo, :number, :title, :body, :state, :stateReason, :labels,
                    :authorLogin, :authorIsBot, :htmlUrl, :commentsCount,
                    :createdAt, :updatedAt, :closedAt, :contentHash, now())
            ON CONFLICT (github_id) DO UPDATE SET
                repo           = EXCLUDED.repo,
                number         = EXCLUDED.number,
                title          = EXCLUDED.title,
                body           = EXCLUDED.body,
                state          = EXCLUDED.state,
                state_reason   = EXCLUDED.state_reason,
                labels         = EXCLUDED.labels,
                author_login   = EXCLUDED.author_login,
                author_is_bot  = EXCLUDED.author_is_bot,
                html_url       = EXCLUDED.html_url,
                comments_count = EXCLUDED.comments_count,
                created_at     = EXCLUDED.created_at,
                updated_at     = EXCLUDED.updated_at,
                closed_at      = EXCLUDED.closed_at,
                content_hash   = EXCLUDED.content_hash,
                synced_at      = EXCLUDED.synced_at
            """;

    private final JdbcClient jdbc;

    public IssueWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts the issue, or updates it if an issue with the same GitHub id is already stored. */
    public void upsert(IssueSnapshot issue) {
        jdbc.sql(UPSERT)
                .param("githubId", issue.githubId())
                .param("repo", issue.repo())
                .param("number", issue.number())
                .param("title", issue.title())
                .param("body", issue.body())
                .param("state", issue.state().name())
                .param("stateReason", issue.stateReason())
                .param("labels", issue.labels().toArray(new String[0])) // becomes a PostgreSQL text[]
                .param("authorLogin", issue.authorLogin())
                .param("authorIsBot", issue.authorIsBot())
                .param("htmlUrl", issue.htmlUrl())
                .param("commentsCount", issue.commentsCount())
                .param("createdAt", utc(issue.createdAt()))
                .param("updatedAt", utc(issue.updatedAt()))
                .param("closedAt", utc(issue.closedAt()))
                .param("contentHash", ContentHash.of(issue.title(), issue.body()))
                .update();
    }
}
