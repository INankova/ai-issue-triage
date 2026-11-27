package io.github.inankova.triage.issues;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Read access to synced issues. It extends the bare {@link Repository} rather than {@code JpaRepository}, so it
 * exposes only the queries declared here and no save or delete methods: issues are written by the sync (P1.4).
 */
public interface IssueRepository extends Repository<Issue, Long> {

    /** Finds an issue by its repository and number, such as {@code spring-projects/spring-boot} #101. */
    Optional<Issue> findByRepoAndNumber(String repo, int number);
}
