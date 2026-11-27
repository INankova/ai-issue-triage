package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One entry from GitHub's "list repository issues" endpoint, with only the fields we use. GitHub sends about 40
 * fields; the rest are ignored. JSON names are snake_case ({@code state_reason} → {@code stateReason}).
 *
 * <p>The endpoint returns pull requests too: only they carry a {@code pull_request} object, so {@code pullRequest} is
 * non-null exactly for pull requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubIssue(
        long id,
        int number,
        String title,
        String body,
        String state,
        String stateReason,
        List<Label> labels,
        User user,
        String htmlUrl,
        int comments,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt,
        PullRequestRef pullRequest) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String login, String type) {

        /** GitHub Apps such as Dependabot have the type {@code Bot}. */
        public boolean isBot() {
            return "Bot".equals(type);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestRef(String url) {}
}
