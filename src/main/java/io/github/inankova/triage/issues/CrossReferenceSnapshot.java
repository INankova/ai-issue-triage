package io.github.inankova.triage.issues;

import java.time.Instant;

/**
 * Another issue or pull request that mentioned an issue, from the issue's timeline; the input to
 * {@link IssueDiscussionWriter#saveCrossReferences}.
 *
 * @param sourceRepo where the mention was written, for example {@code spring-projects/spring-boot}
 * @param sourceNumber the mentioning issue's or pull request's number in that repository
 */
public record CrossReferenceSnapshot(
        String sourceRepo, int sourceNumber, boolean sourceIsPullRequest, Instant createdAt) {}
