package io.github.inankova.triage.ingestion;

/** The life of an import run: it starts {@code RUNNING} and ends {@code SUCCEEDED} or {@code FAILED}. */
public enum ImportRunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED
}
