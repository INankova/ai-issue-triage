package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The author of an issue or comment, with only the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUser(String login, String type) {

    /** GitHub Apps such as Dependabot have the type {@code Bot}. */
    public boolean isBot() {
        return "Bot".equals(type);
    }
}
