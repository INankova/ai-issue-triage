package io.github.inankova.triage.ingestion;

import io.github.inankova.triage.ingestion.internal.GitHubClient;
import io.github.inankova.triage.ingestion.internal.GitHubComment;
import io.github.inankova.triage.ingestion.internal.GitHubTimelineEvent;
import io.github.inankova.triage.issues.CommentSnapshot;
import io.github.inankova.triage.issues.CrossReferenceSnapshot;
import io.github.inankova.triage.issues.Issue;
import io.github.inankova.triage.issues.IssueDiscussionWriter;
import io.github.inankova.triage.issues.IssueRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Fetches comments and timeline cross-references, but only for issues marked as duplicates (ROADMAP P1.6). They are
 * the raw material for the duplicate pairs in EVALUATION §2.3; fetching them for every issue would cost two requests
 * per issue for data we don't use.
 *
 * <p>Known limit: GitHub leaves {@code cross-referenced} events out of the timeline (REST and GraphQL alike) when the
 * request uses a fine-grained token with read-only access to public repositories, as ours does. Without a token, or
 * with a classic token, they are included. With our token this class therefore stores comments only.
 */
@Service
public class DuplicateDiscussionSync {

    private final GitHubClient github;

    private final IssueRepository issues;

    private final IssueDiscussionWriter writer;

    public DuplicateDiscussionSync(GitHubClient github, IssueRepository issues, IssueDiscussionWriter writer) {
        this.github = github;
        this.issues = issues;
        this.writer = writer;
    }

    public DiscussionSyncResult syncDuplicates(String repo) {
        List<Issue> duplicates = issues.findDuplicates(repo);
        int comments = 0;
        int crossReferences = 0;
        for (Issue duplicate : duplicates) {
            List<CommentSnapshot> issueComments = github.fetchComments(repo, duplicate.getNumber()).stream()
                    .map(DuplicateDiscussionSync::toSnapshot)
                    .toList();
            List<CrossReferenceSnapshot> issueReferences = github.fetchTimeline(repo, duplicate.getNumber()).stream()
                    .filter(DuplicateDiscussionSync::isCrossReference)
                    .map(DuplicateDiscussionSync::toSnapshot)
                    .toList();
            writer.saveComments(duplicate.getGithubId(), issueComments);
            writer.saveCrossReferences(duplicate.getGithubId(), issueReferences);
            comments += issueComments.size();
            crossReferences += issueReferences.size();
        }
        return new DiscussionSyncResult(duplicates.size(), comments, crossReferences);
    }

    /** A mention from another issue or pull request whose repository we know. */
    private static boolean isCrossReference(GitHubTimelineEvent event) {
        return "cross-referenced".equals(event.event())
                && event.source() != null
                && event.source().issue() != null
                && event.source().issue().repository() != null;
    }

    private static CommentSnapshot toSnapshot(GitHubComment comment) {
        // GitHub shows the authors of deleted accounts as "ghost"; the user object may also be missing.
        String login = comment.user() != null ? comment.user().login() : "ghost";
        return new CommentSnapshot(
                comment.id(), login, comment.authorAssociation(), comment.body(), comment.createdAt());
    }

    private static CrossReferenceSnapshot toSnapshot(GitHubTimelineEvent event) {
        GitHubTimelineEvent.SourceIssue source = event.source().issue();
        return new CrossReferenceSnapshot(
                source.repository().fullName(), source.number(), source.pullRequest() != null, event.createdAt());
    }
}
