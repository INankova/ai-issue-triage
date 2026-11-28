package io.github.inankova.triage.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImportRunTests {

    private static final Instant STARTED = Instant.parse("2026-11-28T12:00:00Z");

    private static final Instant FINISHED = Instant.parse("2026-11-28T12:01:15Z");

    @Test
    void startsRunningWithoutAFinishTime() {
        ImportRun run = ImportRun.start("spring-projects/spring-boot", STARTED);

        assertThat(run.getStatus()).isEqualTo(ImportRunStatus.RUNNING);
        assertThat(run.getRepo()).isEqualTo("spring-projects/spring-boot");
        assertThat(run.getStartedAt()).isEqualTo(STARTED);
        assertThat(run.getFinishedAt()).isNull();
    }

    @Test
    void succeedingRecordsTheCountsAndTheFinishTime() {
        ImportRun run = ImportRun.start("spring-projects/spring-boot", STARTED);

        run.succeed(new SyncResult(6210, 5000), FINISHED);

        assertThat(run.getStatus()).isEqualTo(ImportRunStatus.SUCCEEDED);
        assertThat(run.getIssuesSeen()).isEqualTo(6210);
        assertThat(run.getIssuesUpserted()).isEqualTo(5000);
        assertThat(run.getFinishedAt()).isEqualTo(FINISHED);
        assertThat(run.getError()).isNull();
    }

    @Test
    void failingRecordsTheError() {
        ImportRun run = ImportRun.start("spring-projects/spring-boot", STARTED);

        run.fail("GitHub API returned HTTP 401: Bad credentials", FINISHED);

        assertThat(run.getStatus()).isEqualTo(ImportRunStatus.FAILED);
        assertThat(run.getError()).isEqualTo("GitHub API returned HTTP 401: Bad credentials");
        assertThat(run.getFinishedAt()).isEqualTo(FINISHED);
    }

    @Test
    void aFinishedRunCannotFinishAgain() {
        ImportRun run = ImportRun.start("spring-projects/spring-boot", STARTED);
        run.succeed(new SyncResult(10, 8), FINISHED);

        assertThatThrownBy(() -> run.fail("too late", FINISHED)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> run.succeed(new SyncResult(1, 1), FINISHED)).isInstanceOf(IllegalStateException.class);
    }
}
