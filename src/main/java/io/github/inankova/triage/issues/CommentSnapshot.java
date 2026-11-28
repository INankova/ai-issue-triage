package io.github.inankova.triage.issues;

import java.time.Instant;

/**
 * A comment on an issue as GitHub reports it; the input to {@link IssueDiscussionWriter#saveComments}.
 *
 * @param authorAssociation the author's role in the repository, such as {@code MEMBER} or {@code NONE}
 */
public record CommentSnapshot(
        long githubId, String authorLogin, String authorAssociation, String body, Instant createdAt) {}
