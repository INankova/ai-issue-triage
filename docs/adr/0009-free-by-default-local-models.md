# ADR-0009: $0 by default with local models; hosted provider implemented but optional

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

The author doesn't want to pay for AI usage for now, but the project should still show a production-style integration with a hosted LLM provider, ready to switch on later.

The development machine, checked on 2026-11-26: AMD Ryzen 5 7530U (6 cores / 12 threads), 16 GB RAM, integrated Radeon graphics and no dedicated GPU. Local models will run on the CPU.

## Decision

- **`local` is the default profile and costs $0.** Chat goes through Ollama, running natively on Windows rather than in Docker. Embeddings use Spring AI's in-process ONNX model.
- **The hosted provider (Anthropic Claude) is fully implemented behind the `cloud` profile** but stays off until an API key exists (P3.9):
  - model names and prices live in `application-cloud.yml`;
  - contract tests run against a WireMock stub of the provider API (`spring.ai.anthropic.base-url` pointed at WireMock). They cover request mapping, structured-output parsing and rate-limit/overload errors, with no real calls and no cost;
  - switching on means setting `ANTHROPIC_API_KEY` and starting with the `cloud` profile, as documented in the README (stretch goal S9).
- Nothing in the build, the tests or a local run requires a paid key.
- The public demo serves precomputed results with no live model calls, so the server needs no LLM at all.

### Working within CPU-only hardware

- Use small quantised models (about 1–4B parameters) for iteration. Try a 7–8B model only for final comparison runs; expect it to be slow and memory-hungry on 16 GB.
- Keep inputs short (truncate the body, shorten stack traces). On a CPU, reading the prompt dominates latency.
- Iterate on a fixed 50–100 issue dev subset, and run the 300-issue test set overnight.
- Import all ~5,000 issues (free, no LLM involved), but classify only the eval sets and a ~500-issue demo subset. Embed everything, since in-process ONNX is fast.
- Cap Docker/WSL memory with `.wslconfig` so IntelliJ, Docker and the model fit in RAM together.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| Pay for a hosted model from the start | Best accuracy; fast | Costs money now, which isn't wanted |
| Free tiers of hosted providers | Hosted quality at $0 | Daily caps, frequently changing terms, possible use of data for training. Revisit in P3.7 if local quality is too low (issue data is public, so privacy isn't the concern) |
| Local only, with no hosted code at all | Simplest | Loses the provider-abstraction story, and switching later becomes a bigger change |

## Consequences

- AI cost so far: **$0**. Reported numbers come from local models and say so (e.g. "measured with a 3B model on a laptop CPU"), which is an honest and memorable detail.
- Accuracy will probably be lower than with a large hosted model, so the baselines, prompt design and dynamic few-shot examples matter more. When a key is added, the same eval produces hosted rows for direct comparison.
- Latency numbers reflect CPU inference and are not representative of production.
- Interview story: *"It runs at $0 on a laptop; the hosted provider sits behind the same gateway and is switched on by configuration."*
