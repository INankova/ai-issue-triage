package io.github.inankova.triage.issues;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.inankova.triage.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The sync writes issues with plain SQL (P1.4) and the rest of the app reads them through JPA. These tests insert
 * rows with SQL, the way the sync will, and check that {@link Issue} maps every column (ROADMAP P1.1).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class IssueRepositoryTests {

    private static final String SPRING_BOOT = "spring-projects/spring-boot";

    private final IssueRepository issues;

    private final JdbcClient jdbc;

    IssueRepositoryTests(@Autowired IssueRepository issues, @Autowired JdbcClient jdbc) {
        this.issues = issues;
        this.jdbc = jdbc;
    }

    @Test
    void findsAClosedIssueByRepoAndNumberWithEveryFieldMapped() {
        insertClosedDuplicateIssue();

        Issue issue = issues.findByRepoAndNumber(SPRING_BOOT, 101).orElseThrow();

        assertThat(issue.getId()).isNotNull();
        assertThat(issue.getGithubId()).isEqualTo(9_000_101L);
        assertThat(issue.getRepo()).isEqualTo(SPRING_BOOT);
        assertThat(issue.getNumber()).isEqualTo(101);
        assertThat(issue.getTitle()).isEqualTo("Startup fails after upgrade");
        assertThat(issue.getBody()).isEqualTo("Steps to reproduce: ...");
        assertThat(issue.getState()).isEqualTo(IssueState.CLOSED);
        assertThat(issue.getStateReason()).isEqualTo("duplicate");
        assertThat(issue.getLabels()).containsExactly("type: bug", "status: duplicate");
        assertThat(issue.getAuthorLogin()).isEqualTo("octocat");
        assertThat(issue.isAuthorBot()).isFalse();
        assertThat(issue.getHtmlUrl()).isEqualTo("https://github.com/spring-projects/spring-boot/issues/101");
        assertThat(issue.getCommentsCount()).isEqualTo(3);
        assertThat(issue.getCreatedAt()).isEqualTo(Instant.parse("2025-03-01T10:15:30Z"));
        assertThat(issue.getUpdatedAt()).isEqualTo(Instant.parse("2025-03-04T08:00:00Z"));
        assertThat(issue.getClosedAt()).isEqualTo(Instant.parse("2025-03-04T08:00:00Z"));
        assertThat(issue.getContentHash()).isEqualTo("hash-101");
        assertThat(issue.getSyncedAt()).isEqualTo(Instant.parse("2026-11-26T12:00:00Z"));
    }

    @Test
    void mapsAnOpenBotIssueWithoutBodyOrLabels() {
        insertOpenBotIssueWithoutBodyOrLabels();

        Issue issue = issues.findByRepoAndNumber(SPRING_BOOT, 102).orElseThrow();

        assertThat(issue.getState()).isEqualTo(IssueState.OPEN);
        assertThat(issue.getBody()).isNull();
        assertThat(issue.getStateReason()).isNull();
        assertThat(issue.getClosedAt()).isNull();
        assertThat(issue.getLabels()).isEmpty();
        assertThat(issue.isAuthorBot()).isTrue();
    }

    @Test
    void doesNotFindTheSameNumberInAnotherRepo() {
        insertClosedDuplicateIssue();

        assertThat(issues.findByRepoAndNumber("spring-projects/spring-framework", 101))
                .isEmpty();
    }

    @Test
    void findsTheNewestUpdateTimeOfARepoForTheNextIncrementalSync() {
        insertClosedDuplicateIssue(); // updated 2025-03-04T08:00:00Z
        insertOpenBotIssueWithoutBodyOrLabels(); // updated 2025-03-02T09:00:00Z

        assertThat(issues.findNewestUpdatedAt(SPRING_BOOT)).contains(Instant.parse("2025-03-04T08:00:00Z"));
        assertThat(issues.findNewestUpdatedAt("spring-projects/spring-framework"))
                .isEmpty();
    }

    @Test
    void findsDuplicatesByLabelOrByStateReason() {
        insertClosedDuplicateIssue(); // #101: labelled status: duplicate
        insertOpenBotIssueWithoutBodyOrLabels(); // #102: not a duplicate
        jdbc.sql("""
                INSERT INTO issue (github_id, repo, number, title, body, state, state_reason, labels,
                                   author_login, author_is_bot, html_url, comments_count,
                                   created_at, updated_at, closed_at, content_hash, synced_at)
                VALUES (9000103, 'spring-projects/spring-boot', 103, 'Closed as duplicate, no label', NULL,
                        'CLOSED', 'duplicate', ARRAY['type: bug'], 'octocat', false,
                        'https://github.com/spring-projects/spring-boot/issues/103', 1,
                        '2025-03-05T09:00:00Z', '2025-03-06T09:00:00Z', '2025-03-06T09:00:00Z',
                        'hash-103', '2026-11-26T12:00:00Z')
                """).update();

        assertThat(issues.findDuplicates(SPRING_BOOT))
                .extracting(Issue::getNumber)
                .containsExactly(101, 103);
        assertThat(issues.findDuplicates("spring-projects/spring-framework")).isEmpty();
    }

    private void insertClosedDuplicateIssue() {
        jdbc.sql("""
                INSERT INTO issue (github_id, repo, number, title, body, state, state_reason, labels,
                                   author_login, author_is_bot, html_url, comments_count,
                                   created_at, updated_at, closed_at, content_hash, synced_at)
                VALUES (9000101, 'spring-projects/spring-boot', 101, 'Startup fails after upgrade',
                        'Steps to reproduce: ...', 'CLOSED', 'duplicate', ARRAY['type: bug', 'status: duplicate'],
                        'octocat', false, 'https://github.com/spring-projects/spring-boot/issues/101', 3,
                        '2025-03-01T10:15:30Z', '2025-03-04T08:00:00Z', '2025-03-04T08:00:00Z',
                        'hash-101', '2026-11-26T12:00:00Z')
                """).update();
    }

    private void insertOpenBotIssueWithoutBodyOrLabels() {
        jdbc.sql("""
                INSERT INTO issue (github_id, repo, number, title, body, state, state_reason, labels,
                                   author_login, author_is_bot, html_url, comments_count,
                                   created_at, updated_at, closed_at, content_hash, synced_at)
                VALUES (9000102, 'spring-projects/spring-boot', 102, 'Upgrade to a newer dependency',
                        NULL, 'OPEN', NULL, '{}',
                        'dependabot[bot]', true, 'https://github.com/spring-projects/spring-boot/issues/102', 0,
                        '2025-03-02T09:00:00Z', '2025-03-02T09:00:00Z', NULL,
                        'hash-102', '2026-11-26T12:00:00Z')
                """).update();
    }
}
