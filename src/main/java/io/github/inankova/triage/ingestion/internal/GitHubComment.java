package io.github.inankova.triage.ingestion.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One comment from GitHub's "list issue comments" endpoint, with only the fields we use.
 *
 * @param authorAssociation the author's role in the repository, such as {@code MEMBER} or {@code CONTRIBUTOR}; it
 *     tells maintainers' comments apart (EVALUATION §2.3)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubComment(long id, GitHubUser user, String authorAssociation, String body, Instant createdAt) {}
