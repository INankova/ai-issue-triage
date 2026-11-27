package io.github.inankova.triage.ingestion.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class GitHubPropertiesTests {

    @Test
    void toStringNeverRevealsTheToken() {
        GitHubProperties properties = new GitHubProperties(URI.create("https://api.github.com"), "github_pat_secret");

        assertThat(properties.toString()).doesNotContain("github_pat_secret").contains("token=****");
    }
}
