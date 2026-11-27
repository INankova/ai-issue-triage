package io.github.inankova.triage.ingestion.internal;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * Settings for the GitHub REST API client, bound from {@code triage.github.*}.
 *
 * @param baseUrl the API root; only a GitHub Enterprise server would need a different one
 * @param token a read-only token from {@code GITHUB_TOKEN} (P0.8); blank means unauthenticated, 60 requests/hour
 */
@ConfigurationProperties("triage.github")
public record GitHubProperties(
        @DefaultValue("https://api.github.com") URI baseUrl, String token) {

    public boolean hasToken() {
        return StringUtils.hasText(token);
    }

    /** Never prints the token, so logging these properties can't leak it. */
    @Override
    public String toString() {
        return "GitHubProperties[baseUrl=" + baseUrl + ", token=" + (hasToken() ? "****" : "none") + "]";
    }
}
