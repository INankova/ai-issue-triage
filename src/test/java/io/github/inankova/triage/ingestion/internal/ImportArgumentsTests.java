package io.github.inankova.triage.ingestion.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ImportArgumentsTests {

    @Test
    void defaultsToTheSpringBootRepoAndFiveThousandIssues() {
        ImportArguments arguments = ImportArguments.from(new DefaultApplicationArguments());

        assertThat(arguments).isEqualTo(new ImportArguments("spring-projects/spring-boot", 5000));
    }

    @Test
    void readsTheRepoAndLimitOptions() {
        ImportArguments arguments = ImportArguments.from(
                new DefaultApplicationArguments("--repo=spring-projects/spring-framework", "--limit=500"));

        assertThat(arguments).isEqualTo(new ImportArguments("spring-projects/spring-framework", 500));
    }

    @Test
    void rejectsALimitThatIsNotAPositiveWholeNumber() {
        assertThatThrownBy(() -> ImportArguments.from(new DefaultApplicationArguments("--limit=0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImportArguments.from(new DefaultApplicationArguments("--limit=lots")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
