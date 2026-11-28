package io.github.inankova.triage.ingestion;

/**
 * What one import or sync did.
 *
 * @param seen entries GitHub returned, pull requests included
 * @param upserted issues written to the database (pull requests are skipped)
 */
public record SyncResult(int seen, int upserted) {}
