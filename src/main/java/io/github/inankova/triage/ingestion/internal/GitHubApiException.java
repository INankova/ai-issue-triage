package io.github.inankova.triage.ingestion.internal;

/** A GitHub API call returned an error status. The message includes GitHub's own explanation when it sent one. */
public class GitHubApiException extends RuntimeException {

    private final int status;

    public GitHubApiException(int status, String githubMessage) {
        super("GitHub API returned HTTP " + status + (githubMessage == null ? "" : ": " + githubMessage));
        this.status = status;
    }

    /** The HTTP status code, for example 401 for a bad token or 404 for an unknown repository. */
    public int status() {
        return status;
    }
}
