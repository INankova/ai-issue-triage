-- Enable pgvector so later migrations can add vector columns and HNSW indexes (ADR-0003, DESIGN §9).
-- IF NOT EXISTS makes this safe on a database where the extension was already installed by hand.
CREATE EXTENSION IF NOT EXISTS vector;
