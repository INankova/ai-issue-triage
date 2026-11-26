# ADR-0004: Spring AI behind our own `llm` gateway

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

We need chat completions with structured output, embeddings, the ability to switch providers (free local models by default, hosted models as an option), and metrics for every call.

## Decision

- Use **Spring AI 2.1.x** (version chosen by the Spring Boot 4.2 BOM in Initializr): `ChatClient` for chat and `EmbeddingModel` for embeddings.
- Providers: **Ollama** (profile `local`, the default, $0) and a hosted provider (profile `cloud`), which is implemented and contract-tested but off until an API key exists ([ADR-0009](0009-free-by-default-local-models.md)). Embeddings default to Spring AI's in-process ONNX model and are revisited after the embedding eval (P5.4).
- **All LLM and embedding calls go through our `llm` module.** It owns model selection, timeouts, retries, call logging (`llm_call`), token and cost accounting, and the daily budget cap. No other module imports Spring AI chat or embedding classes, and an ArchUnit rule checks this.
- Add AI dependencies only when a step needs them (P3.1, P5.1), not in the initial skeleton.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| LangChain4j | Mature; "AI Services" interfaces; many integrations | Less Spring-native; Spring AI is what Spring teams increasingly expect |
| A provider's official Java SDK | Newest API features first; full control | Locks us to one provider; more glue code |
| Raw HTTP | Maximum control | The most code to write and maintain |

## Consequences

- Switching between local and hosted models is a profile and configuration change.
- Micrometer observations come built in.
- If we need a provider feature that Spring AI doesn't expose yet, we call that provider's SDK *behind the same gateway interface*. It stays a local change.
- The gateway is a natural place for unit tests, stubs and the cost dashboard.
