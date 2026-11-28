package io.github.inankova.triage.ingestion;

import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Runs an import and records it in {@code import_run} (ROADMAP P1.5). The run row is saved before the import starts
 * and updated when it ends, whether it succeeds or fails.
 */
@Service
public class IssueImportService {

    private final IssueSync sync;

    private final ImportRunRepository runs;

    private final Clock clock;

    public IssueImportService(IssueSync sync, ImportRunRepository runs, Clock clock) {
        this.sync = sync;
        this.runs = runs;
        this.clock = clock;
    }

    /**
     * Imports the {@code limit} most recently created issues of a repository.
     *
     * @return the finished run, with its counts
     * @throws RuntimeException whatever made the import fail, after the failure has been recorded
     */
    public ImportRun importRecent(String repo, int limit) {
        ImportRun run = runs.save(ImportRun.start(repo, clock.instant()));
        try {
            SyncResult result = sync.importRecent(repo, limit);
            run.succeed(result, clock.instant());
            return runs.save(run);
        } catch (RuntimeException e) {
            run.fail(e.getMessage() != null ? e.getMessage() : e.getClass().getName(), clock.instant());
            runs.save(run);
            throw e;
        }
    }
}
