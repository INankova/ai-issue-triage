package io.github.inankova.triage.issues;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Read access to synced issues. It extends the bare {@link Repository} rather than {@code JpaRepository}, so it
 * exposes only the queries declared here and no save or delete methods: {@link IssueWriter} does the writing.
 */
public interface IssueRepository extends Repository<Issue, Long> {

    /** Finds an issue by its repository and number, such as {@code spring-projects/spring-boot} #101. */
    Optional<Issue> findByRepoAndNumber(String repo, int number);

    /**
     * The latest GitHub {@code updated_at} stored for a repository, or empty if none of its issues are stored yet.
     * An incremental sync asks GitHub for everything updated since then.
     */
    @Query("select max(i.updatedAt) from Issue i where i.repo = :repo")
    Optional<Instant> findNewestUpdatedAt(String repo);
}
