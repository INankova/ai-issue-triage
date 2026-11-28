package io.github.inankova.triage.ingestion;

import org.springframework.data.repository.ListCrudRepository;

/** Stores import runs. Plain CRUD is enough: a run is saved when it starts and again when it ends. */
public interface ImportRunRepository extends ListCrudRepository<ImportRun, Long> {}
