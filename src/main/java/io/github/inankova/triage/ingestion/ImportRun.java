package io.github.inankova.triage.ingestion;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/**
 * One import from GitHub and its outcome, a row of {@code import_run} (DESIGN §7). It is saved as {@code RUNNING}
 * when the import starts, so a crashed import still leaves a trace, and saved again when it ends.
 *
 * <p>The state changes go through {@link #start}, {@link #succeed} and {@link #fail} instead of setters, so a run can
 * only move from {@code RUNNING} to a final state, once.
 */
@Entity
public class ImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // the database assigns the id
    private Long id;

    private String repo;

    private Instant startedAt;

    private Instant finishedAt;

    private int issuesSeen;

    private int issuesUpserted;

    @Enumerated(EnumType.STRING)
    private ImportRunStatus status;

    private String error;

    /** For Hibernate only; new runs come from {@link #start}. */
    protected ImportRun() {}

    public static ImportRun start(String repo, Instant now) {
        ImportRun run = new ImportRun();
        run.repo = repo;
        run.startedAt = now;
        run.status = ImportRunStatus.RUNNING;
        return run;
    }

    public void succeed(SyncResult result, Instant now) {
        finish(ImportRunStatus.SUCCEEDED, now);
        this.issuesSeen = result.seen();
        this.issuesUpserted = result.upserted();
    }

    public void fail(String error, Instant now) {
        finish(ImportRunStatus.FAILED, now);
        this.error = error;
    }

    private void finish(ImportRunStatus finalStatus, Instant now) {
        if (status != ImportRunStatus.RUNNING) {
            throw new IllegalStateException("Import run " + id + " has already finished as " + status);
        }
        this.status = finalStatus;
        this.finishedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getRepo() {
        return repo;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getIssuesSeen() {
        return issuesSeen;
    }

    public int getIssuesUpserted() {
        return issuesUpserted;
    }

    public ImportRunStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
