package io.github.inankova.triage.ingestion;

/**
 * What a run of {@link DuplicateDiscussionSync} did.
 *
 * @param issues duplicate issues whose discussion was fetched
 * @param comments comments saved
 * @param crossReferences cross-references saved, before duplicates of the same source are dropped
 */
public record DiscussionSyncResult(int issues, int comments, int crossReferences) {}
