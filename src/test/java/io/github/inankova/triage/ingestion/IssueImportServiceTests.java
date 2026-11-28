package io.github.inankova.triage.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.inankova.triage.TestcontainersConfiguration;
import io.github.inankova.triage.ingestion.internal.GitHubApiException;
import io.github.inankova.triage.ingestion.internal.GitHubClient;
import io.github.inankova.triage.ingestion.internal.GitHubProperties;
import io.github.inankova.triage.issues.IssueRepository;
import io.github.inankova.triage.issues.IssueWriter;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Every import leaves an {@code import_run} row with its outcome (ROADMAP P1.5). */
@DataJpaTest
@Import({TestcontainersConfiguration.class, IssueWriter.class})
class IssueImportServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-28T12:00:00Z");

    private static final String REPO = "spring-projects/spring-boot";

    private static final String FIRST_PAGE = "https://api.github.com/repos/spring-projects/spring-boot/issues"
            + "?state=all&sort=created&direction=desc&per_page=100";

    private static final String SECOND_PAGE = "https://api.github.com/repositories/6296790/issues"
            + "?state=all&sort=created&direction=desc&per_page=100"
            + "&after=Y3Vyc29yOnYyOpLPAAABofhs6tDPAAAAAYP7m_M%3D&page=2";

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer github =
            MockRestServiceServer.bindTo(builder).build();

    private final EntityManager entityManager;

    private final JdbcClient jdbc;

    private final IssueImportService service;

    IssueImportServiceTests(
            @Autowired IssueWriter writer,
            @Autowired IssueRepository issues,
            @Autowired ImportRunRepository runs,
            @Autowired EntityManager entityManager,
            @Autowired JdbcClient jdbc) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        GitHubClient client =
                new GitHubClient(builder, new GitHubProperties(URI.create("https://api.github.com"), "test-token"));
        this.service =
                new IssueImportService(new IssueSync(client, writer, issues), runs, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsASuccessfulRunWithItsCounts() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withSuccess(fixture("issues-page-1.json"), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, "<" + SECOND_PAGE + ">; rel=\"next\""));
        github.expect(requestTo(SECOND_PAGE))
                .andRespond(withSuccess(fixture("issues-page-2.json"), MediaType.APPLICATION_JSON));

        ImportRun run = service.importRecent(REPO, 500);

        assertThat(run.getStatus()).isEqualTo(ImportRunStatus.SUCCEEDED);
        assertThat(storedRun()).isEqualTo("spring-projects/spring-boot|SUCCEEDED|5|3|no error");
    }

    @Test
    void recordsAFailedRunWithTheErrorAndStillThrows() {
        github.expect(requestTo(FIRST_PAGE))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fixture("error-401.json")));

        assertThatThrownBy(() -> service.importRecent(REPO, 500)).isInstanceOf(GitHubApiException.class);

        assertThat(storedRun())
                .isEqualTo("spring-projects/spring-boot|FAILED|0|0|GitHub API returned HTTP 401: Bad credentials");
    }

    /** Reads the row with SQL after flushing JPA's pending writes, so we see what is really in the table. */
    private String storedRun() {
        entityManager.flush();
        return jdbc.sql("""
                        SELECT repo || '|' || status || '|' || issues_seen || '|' || issues_upserted
                               || '|' || coalesce(error, 'no error')
                        FROM import_run
                        WHERE started_at IS NOT NULL AND finished_at IS NOT NULL
                        """).query(String.class).single();
    }

    private static ClassPathResource fixture(String name) {
        return new ClassPathResource("fixtures/github/" + name);
    }
}
