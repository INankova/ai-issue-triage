package io.github.inankova.triage.issues;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A GitHub issue as it looked at the last sync (DESIGN §7).
 *
 * <p>GitHub is the source of truth and this app never edits issues, so the entity is read-only: no setters, no public
 * constructor, and {@link Immutable} tells Hibernate never to write it. The sync writes rows with a SQL upsert
 * (ROADMAP P1.4). Column names follow Spring Boot's default naming strategy ({@code githubId} → {@code github_id}).
 */
@Entity
@Immutable
public class Issue {

    @Id
    private Long id;

    private long githubId;

    /** Owner and name, for example {@code spring-projects/spring-boot}. */
    private String repo;

    private int number;

    private String title;

    /** Markdown body; GitHub allows it to be empty, which arrives as {@code null}. */
    private String body;

    @Enumerated(EnumType.STRING)
    private IssueState state;

    /** GitHub's reason for the current state, such as {@code completed} or {@code not_planned}; may be null. */
    private String stateReason;

    /** Label names, stored as a PostgreSQL {@code text[]}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> labels;

    private String authorLogin;

    @Column(name = "author_is_bot")
    private boolean authorBot;

    private String htmlUrl;

    private int commentsCount;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant closedAt;

    /** Hash of the fields that matter for triage; a new hash means the issue needs re-processing (P1.4). */
    private String contentHash;

    /** When this row was last written by the sync. */
    private Instant syncedAt;

    /** Only Hibernate creates instances, when it loads rows. */
    protected Issue() {}

    public Long getId() {
        return id;
    }

    public long getGithubId() {
        return githubId;
    }

    public String getRepo() {
        return repo;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public IssueState getState() {
        return state;
    }

    public String getStateReason() {
        return stateReason;
    }

    public List<String> getLabels() {
        return List.copyOf(labels);
    }

    public String getAuthorLogin() {
        return authorLogin;
    }

    public boolean isAuthorBot() {
        return authorBot;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
