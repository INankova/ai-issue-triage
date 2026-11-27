package io.github.inankova.triage.ingestion.internal;

import java.time.Duration;

/** GitHub refused a request because of a rate limit and told us how long to wait (403 or 429). */
public class GitHubRateLimitException extends GitHubApiException {

    private final Duration waitTime;

    public GitHubRateLimitException(int status, String githubMessage, Duration waitTime) {
        super(status, githubMessage);
        this.waitTime = waitTime;
    }

    /** How long to wait before trying again, worked out from the response headers. */
    public Duration waitTime() {
        return waitTime;
    }
}
