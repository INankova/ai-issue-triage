package io.github.inankova.triage.issues;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.inankova.triage.TestcontainersConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The upsert behind every sync (ROADMAP P1.4). Assertions read with SQL, the same way the writer writes. */
@DataJpaTest
@Import({TestcontainersConfiguration.class, IssueWriter.class})
class IssueWriterTests {

    private final IssueWriter writer;

    private final JdbcClient jdbc;

    IssueWriterTests(@Autowired IssueWriter writer, @Autowired JdbcClient jdbc) {
        this.writer = writer;
        this.jdbc = jdbc;
    }

    @Test
    void insertsANewIssueWithTheHashOfItsTitleAndBody() {
        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug")));

        assertThat(rowCount()).isEqualTo(1);
        assertThat(column("title")).isEqualTo("Startup fails");
        assertThat(column("array_to_string(labels, '|')")).isEqualTo("type: bug");
        assertThat(column("content_hash"))
                .isEqualTo("5c936f11c23cf1433b9704b113399ee28fe5e466169f442c67426643b215a166");
    }

    @Test
    void upsertingTheSameIssueTwiceKeepsOneRowAndTheSameHash() {
        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug")));
        String hash = column("content_hash");

        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug")));

        assertThat(rowCount()).isEqualTo(1);
        assertThat(column("content_hash")).isEqualTo(hash);
    }

    @Test
    void anEditedTitleUpdatesTheRowAndGivesItANewHash() {
        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug")));
        String hash = column("content_hash");

        writer.upsert(snapshot("Startup fails after upgrade", "Steps to reproduce", List.of("type: bug")));

        assertThat(rowCount()).isEqualTo(1);
        assertThat(column("title")).isEqualTo("Startup fails after upgrade");
        assertThat(column("content_hash")).isNotEqualTo(hash);
    }

    @Test
    void newLabelsAreSavedButKeepTheHashBecauseTheModelNeverSeesThem() {
        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug")));
        String hash = column("content_hash");

        writer.upsert(snapshot("Startup fails", "Steps to reproduce", List.of("type: bug", "status: duplicate")));

        assertThat(column("array_to_string(labels, '|')")).isEqualTo("type: bug|status: duplicate");
        assertThat(column("content_hash")).isEqualTo(hash);
    }

    private static IssueSnapshot snapshot(String title, String body, List<String> labels) {
        return new IssueSnapshot(
                9_000_101L,
                "spring-projects/spring-boot",
                101,
                title,
                body,
                IssueState.OPEN,
                null,
                labels,
                "example-user-1",
                false,
                "https://github.com/spring-projects/spring-boot/issues/101",
                2,
                Instant.parse("2025-03-01T10:15:30Z"),
                Instant.parse("2025-03-04T08:00:00Z"),
                null);
    }

    private int rowCount() {
        return jdbc.sql("SELECT count(*) FROM issue").query(Integer.class).single();
    }

    private String column(String expression) {
        return jdbc.sql("SELECT " + expression + " FROM issue WHERE github_id = 9000101")
                .query(String.class)
                .single();
    }
}
