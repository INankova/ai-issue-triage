package io.github.inankova.triage.issues;

import java.time.Instant;
import java.util.List;

/**
 * An issue as GitHub reports it at one moment: the input to {@link IssueWriter#upsert}. It doesn't depend on the
 * GitHub client's types, so the {@code issues} module never needs to know where the data came from.
 */
public record IssueSnapshot(
        long githubId,
        String repo,
        int number,
        String title,
        String body,
        IssueState state,
        String stateReason,
        List<String> labels,
        String authorLogin,
        boolean authorIsBot,
        String htmlUrl,
        int commentsCount,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt) {}
