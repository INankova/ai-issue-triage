package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One event from an issue's timeline: labelled, commented, closed, and so on. Events of different kinds carry
 * different fields; this record maps what we use and leaves the rest null.
 *
 * @param event the kind of event, for example {@code cross-referenced}
 * @param source for a {@code cross-referenced} event, the issue or pull request that mentioned this issue
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubTimelineEvent(String event, Instant createdAt, Source source) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String type, SourceIssue issue) {}

    /** The mentioning issue or pull request; it may live in another repository. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SourceIssue(int number, GitHubIssue.PullRequestRef pullRequest, Repository repository) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Repository(String fullName) {}
}
