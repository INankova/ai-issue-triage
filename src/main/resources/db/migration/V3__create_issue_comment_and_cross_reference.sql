-- Comments, fetched only for the issues that need them: duplicates for the ground truth (P1.6), later RAG sources.
CREATE TABLE issue_comment (
    id                 bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    issue_id           bigint      NOT NULL REFERENCES issue (id) ON DELETE CASCADE,
    github_id          bigint      NOT NULL UNIQUE,      -- GitHub's comment id; the upsert key
    author_login       text        NOT NULL,
    author_association text        NOT NULL,             -- MEMBER, OWNER, COLLABORATOR, CONTRIBUTOR, NONE, ...
    body               text,
    created_at         timestamptz NOT NULL
);
CREATE INDEX issue_comment_issue_id ON issue_comment (issue_id);

-- Issues and pull requests that mentioned an issue: "cross-referenced" events from its timeline (EVALUATION §2.3).
CREATE TABLE issue_cross_reference (
    id                     bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    issue_id               bigint      NOT NULL REFERENCES issue (id) ON DELETE CASCADE,
    source_repo            text        NOT NULL,         -- where the mention was written; may be another project
    source_number          integer     NOT NULL,
    source_is_pull_request boolean     NOT NULL,
    created_at             timestamptz NOT NULL,         -- when the first mention from this source happened
    UNIQUE (issue_id, source_repo, source_number)
);
