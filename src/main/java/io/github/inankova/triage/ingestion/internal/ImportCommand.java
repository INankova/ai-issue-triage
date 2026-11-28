package io.github.inankova.triage.ingestion.internal;

import io.github.inankova.triage.ingestion.DiscussionSyncResult;
import io.github.inankova.triage.ingestion.DuplicateDiscussionSync;
import io.github.inankova.triage.ingestion.ImportRun;
import io.github.inankova.triage.ingestion.IssueImportService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The import command (ROADMAP P1.5, P1.6). It exists only with the {@code import} profile, which also turns the web
 * server off ({@code application-import.yml}), so the app imports and then exits:
 *
 * <pre>
 * .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=import" "-Dspring-boot.run.arguments=--limit=5000"
 * </pre>
 *
 * <p>It imports the issues, then fetches the comments and cross-references of the duplicates among them. A failure
 * throws, so the process ends with a non-zero exit code.
 */
@Component
@Profile("import")
class ImportCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportCommand.class);

    private final IssueImportService imports;

    private final DuplicateDiscussionSync discussions;

    ImportCommand(IssueImportService imports, DuplicateDiscussionSync discussions) {
        this.imports = imports;
        this.discussions = discussions;
    }

    @Override
    public void run(ApplicationArguments args) {
        ImportArguments arguments = ImportArguments.from(args);
        log.info("Importing up to {} issues from {}", arguments.limit(), arguments.repo());
        ImportRun run = imports.importRecent(arguments.repo(), arguments.limit());
        log.info(
                "Imported {}: {} issues upserted ({} entries seen) in {} s, import run #{}",
                run.getRepo(),
                run.getIssuesUpserted(),
                run.getIssuesSeen(),
                Duration.between(run.getStartedAt(), run.getFinishedAt()).toSeconds(),
                run.getId());

        DiscussionSyncResult discussion = discussions.syncDuplicates(arguments.repo());
        log.info(
                "Fetched {} comments and {} cross-references for {} duplicate issues",
                discussion.comments(),
                discussion.crossReferences(),
                discussion.issues());
    }
}
