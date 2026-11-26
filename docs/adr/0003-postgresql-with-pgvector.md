# ADR-0003: PostgreSQL with pgvector instead of a dedicated vector database

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

We need relational data (issues, results, reviews, jobs) *and* vector similarity search (duplicates, retrieval for replies, kNN baseline). There will be well under 100,000 vectors.

## Decision

Use one **PostgreSQL 18** database with the **[pgvector](https://github.com/pgvector/pgvector)** extension:

- Docker image `pgvector/pgvector:pg18` for local development, Testcontainers and CI.
- An HNSW index with cosine distance: `CREATE INDEX … USING hnsw (embedding vector_cosine_ops)`.
- Vector queries are written in plain SQL with `JdbcClient` (`ORDER BY embedding <=> :q LIMIT :k`).

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| Dedicated vector DB (Qdrant, Weaviate, Pinecone) | Built for vectors; nice tooling | Another service to run and keep in sync with Postgres; overkill at our scale; harder to test |
| Elasticsearch / OpenSearch | Hybrid search built in | Heavy to run; a second source of truth |
| Spring AI `PgVectorStore` abstraction | Less code | Hides the SQL we want to understand and explain; its own table layout |

## Consequences

- One database to run, back up and test, using the same image everywhere.
- Joining vectors with relational filters (`created_at < :t`, labels) is trivial.
- A vector column has a fixed dimension, so switching embedding models needs a migration (or a separate table while comparing models in evals).
- Explaining HNSW and cosine distance becomes a good interview topic.
