package io.github.inankova.triage.issues;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Helps the writers pass timestamps to plain SQL. */
final class SqlTimestamps {

    private SqlTimestamps() {}

    /** The PostgreSQL driver accepts {@code OffsetDateTime} for a timestamptz parameter, but not {@code Instant}. */
    static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
