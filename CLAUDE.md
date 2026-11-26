# CLAUDE.md

Guidance for AI coding assistants working in this repository.

**Project:** AI Issue Triage ([README](README.md), [DESIGN](docs/DESIGN.md)). This is a portfolio project for junior Java roles: **the author must understand and be able to explain every line.** Optimise for clarity and learning, not cleverness.

## Working agreement

- Work on one roadmap step at a time ([ROADMAP](docs/ROADMAP.md), IDs like `P3.2`). Read the step's *Done when* criteria and linked design section before starting.
- Before a larger change, state the plan in a few lines. Keep diffs small and reviewable.
- Write tests first for domain logic. Every change must keep `./mvnw verify` (Windows: `.\mvnw.cmd verify`) green.
- When a step is complete, tick it in `docs/ROADMAP.md` in the same change. Write an ADR (`docs/adr/`) for significant decisions.
- The author comes from C#/.NET (ASP.NET Core, EF Core, xUnit, Clean Architecture). When introducing a Java or Spring concept, give a one-line .NET comparison in the explanation, not in code comments.
- Never commit, push or open pull requests unless asked.

## Stack

Java 25 · Spring Boot 4.2.x · Maven Wrapper · PostgreSQL 18 + pgvector · Flyway · Spring Data JPA (CRUD) + `JdbcClient` (vector SQL, job queue) · Spring AI 2.1.x · Thymeleaf + htmx · Testcontainers · JUnit · AssertJ · Mockito · ArchUnit · Resilience4j · Spotless (palantir-java-format) · JaCoCo

## Rules

- **The default profile is `local`** (Ollama + in-process ONNX embeddings, $0). Building, testing and running locally must never require a paid API key. Hosted-provider code (`cloud` profile) is tested only against WireMock stubs.
- **No real LLM or GitHub calls in `./mvnw verify`.** Stub `ChatModel` and `EmbeddingModel`; use MockRestServiceServer or WireMock with recorded fixtures.
- **All LLM and embedding calls go through the `llm` module gateway**, which handles logging, tokens, cost and the budget cap. No other module imports Spring AI chat or embedding classes.
- Prompts live in `src/main/resources/prompts/<purpose>/v<N>.st`. Never edit a released version; add `v<N+1>`.
- **Issue text is untrusted input.** Keep the delimiters, validate every model output, and never let model output trigger a side effect without human approval.
- Secrets only through environment variables. Never commit `.env`, keys or tokens, and never log them.
- Never commit issue text or data dumps. Eval datasets store issue numbers and labels only.
- Respect the module boundaries in DESIGN §6. The ArchUnit tests must pass.
- Use records for DTOs and value objects. Use constructor injection only. No Lombok.
- Database changes go through Flyway migrations. Never edit an applied migration.
- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) messages: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.

## Commands

Fill these in as they become available.

| Task | Command |
|---|---|
| Build and test | `.\mvnw.cmd verify` |
| Run locally | `.\mvnw.cmd spring-boot:run` (starts `compose.yaml` automatically) |
| Format (palantir-java-format; `verify` fails on unformatted code) | `.\mvnw.cmd spotless:apply` |
| Coverage report | `.\mvnw.cmd verify`, then open `target/site/jacoco/index.html` |
| Eval | see [docs/EVALUATION.md](docs/EVALUATION.md) §7 |
