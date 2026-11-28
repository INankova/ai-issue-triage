package io.github.inankova.triage.ingestion;

import io.github.inankova.triage.ingestion.internal.GitHubClient;
import io.github.inankova.triage.ingestion.internal.GitHubIssue;
import io.github.inankova.triage.issues.IssueRepository;
import io.github.inankova.triage.issues.IssueSnapshot;
import io.github.inankova.triage.issues.IssueState;
import io.github.inankova.triage.issues.IssueWriter;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Copies issues from GitHub into the database (DESIGN §6.3, ROADMAP P1.4). Pull requests are skipped: GitHub lists
 * them on the issues endpoint, but they are not issues to triage.
 *
 * <p>Each issue is upserted on its own, without one big transaction. If a run fails halfway, the issues written so
 * far stay, and running it again is safe because the upsert is idempotent.
 */
@Service
public class IssueSync {

    private final GitHubClient github;

    private final IssueWriter writer;

    private final IssueRepository issues;

    public IssueSync(GitHubClient github, IssueWriter writer, IssueRepository issues) {
        this.github = github;
        this.writer = writer;
        this.issues = issues;
    }

    /** First import: the {@code limit} most recently created issues and pull requests of a repository. */
    public SyncResult importRecent(String repo, int limit) {
        return save(repo, github.fetchIssues(repo, limit));
    }

    /**
     * Incremental sync: everything updated on GitHub since the newest update already stored. That time comes from
     * GitHub's own clock, so a difference between our clock and GitHub's can't make us miss an update.
     *
     * @throws IllegalStateException if nothing has been imported for the repository yet
     */
    public SyncResult syncChanges(String repo) {
        Instant since = issues.findNewestUpdatedAt(repo)
                .orElseThrow(
                        () -> new IllegalStateException("No issues stored for " + repo + " yet; run an import first"));
        return save(repo, github.fetchIssuesUpdatedSince(repo, since));
    }

    private SyncResult save(String repo, List<GitHubIssue> fetched) {
        int upserted = 0;
        for (GitHubIssue issue : fetched) {
            if (issue.pullRequest() != null) {
                continue;
            }
            writer.upsert(toSnapshot(repo, issue));
            upserted++;
        }
        return new SyncResult(fetched.size(), upserted);
    }

    private static IssueSnapshot toSnapshot(String repo, GitHubIssue issue) {
        return new IssueSnapshot(
                issue.id(),
                repo,
                issue.number(),
                issue.title(),
                issue.body(),
                IssueState.valueOf(issue.state().toUpperCase(Locale.ROOT)), // GitHub sends "open" or "closed"
                issue.stateReason(),
                issue.labels().stream().map(GitHubIssue.Label::name).toList(),
                issue.user().login(),
                issue.user().isBot(),
                issue.htmlUrl(),
                issue.comments(),
                issue.createdAt(),
                issue.updatedAt(),
                issue.closedAt());
    }
}
