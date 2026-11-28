package io.github.inankova.triage.ingestion.internal;

import java.util.List;
import org.springframework.boot.ApplicationArguments;

/**
 * The options of the import command: {@code --repo=owner/name} and {@code --limit=N}, both optional.
 *
 * @param repo the repository to import from
 * @param limit how many issues to import, newest first
 */
public record ImportArguments(String repo, int limit) {

    static final String DEFAULT_REPO = "spring-projects/spring-boot";

    static final int DEFAULT_LIMIT = 5000;

    public static ImportArguments from(ApplicationArguments arguments) {
        String repo = option(arguments, "repo", DEFAULT_REPO);
        String limitText = option(arguments, "limit", String.valueOf(DEFAULT_LIMIT));
        int limit;
        try {
            limit = Integer.parseInt(limitText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--limit must be a whole number, got: " + limitText, e);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("--limit must be positive, got: " + limit);
        }
        return new ImportArguments(repo, limit);
    }

    /** The last value given for {@code --name}, or the default when the option is missing. */
    private static String option(ApplicationArguments arguments, String name, String defaultValue) {
        List<String> values = arguments.getOptionValues(name);
        return values == null || values.isEmpty() ? defaultValue : values.getLast();
    }
}
