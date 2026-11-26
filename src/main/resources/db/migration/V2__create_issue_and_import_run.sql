-- Issues mirrored from GitHub (DESIGN §7). GitHub is the source of truth; the sync upserts rows by github_id (P1.4).
CREATE TABLE issue (
    id             bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    github_id      bigint      NOT NULL UNIQUE,          -- GitHub's global id; the upsert key
    repo           text        NOT NULL,                 -- owner/name, e.g. spring-projects/spring-boot
    number         integer     NOT NULL,                 -- the #number shown on GitHub, unique per repo
    title          text        NOT NULL,
    body           text,                                 -- GitHub sends null for an empty body
    state          text        NOT NULL CHECK (state IN ('OPEN', 'CLOSED')),
    state_reason   text,                                 -- e.g. completed, not_planned; kept as GitHub sends it
    labels         text[]      NOT NULL DEFAULT '{}',
    author_login   text        NOT NULL,
    author_is_bot  boolean     NOT NULL,
    html_url       text        NOT NULL,
    comments_count integer     NOT NULL DEFAULT 0,
    created_at     timestamptz NOT NULL,
    updated_at     timestamptz NOT NULL,
    closed_at      timestamptz,
    content_hash   text        NOT NULL,                 -- changes when the triage-relevant fields change (P1.4)
    synced_at      timestamptz NOT NULL,                 -- when the sync last wrote this row
    UNIQUE (repo, number)
);

-- One row per import run, with counts for the import statistics (P1.5).
CREATE TABLE import_run (
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo            text        NOT NULL,
    started_at      timestamptz NOT NULL,
    finished_at     timestamptz,                         -- null while the run is in progress
    issues_seen     integer     NOT NULL DEFAULT 0,
    issues_upserted integer     NOT NULL DEFAULT 0,
    status          text        NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    error           text                                 -- failure message when status is FAILED
);
