# ADR-0005: Postgres-backed job queue instead of a message broker

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

Pipeline steps (embed, classify, duplicate check, draft reply) are slow (LLM calls take 1–10 s), rate-limited and can fail. They must be retried, must survive application restarts, and must never run twice for the same input.

## Decision

Implement a small queue on the existing PostgreSQL database:

- A `job` table with `type`, `payload jsonb`, `status`, `attempts`, `run_after`, `locked_by`, `locked_at`, `last_error` and a unique `idempotency_key`.
- Workers claim jobs with `SELECT … FOR UPDATE SKIP LOCKED LIMIT n`, so concurrent workers never take the same row.
- Workers run on virtual threads, with a concurrency limit per job type.
- Retries use exponential backoff with jitter. After `max_attempts` a job becomes `DEAD` and is visible on an admin page.
- Jobs are enqueued **in the same transaction** as the domain change.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| `@Async` only | Trivial | Work is lost on restart; no retries or visibility |
| RabbitMQ / Kafka | Industry standard at scale | Extra infrastructure and the dual-write problem; no benefit at our volume |
| JobRunr / db-scheduler | Proven libraries | Hide the mechanism we want to understand and demonstrate; we can switch later if needed |

## Consequences

- No dual-write problem: the domain change and its follow-up job commit or roll back together, as in an outbox pattern.
- We own roughly 200 lines of concurrency code, so it needs a dedicated test in which several workers compete for the same jobs.
- It's a strong interview story: *"why not Kafka?"* has a reasoned answer.
