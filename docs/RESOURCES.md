# Resources

Curated official documentation for every part of the project. All links were checked on 2026-11-26. Unversioned Spring links point to the current stable release.

**Versions at the time of writing:** start.spring.io defaults to Spring Boot **4.2.0** and offers Java 17, 21, 25 and 27. For Boot 4.2 it selects Spring AI **2.1.0**; Spring Modulith is at **2.2.0-M1** (a milestone). Docker image: `pgvector/pgvector:pg18`.

## Setup

| Resource | Use it for |
|---|---|
| [Adoptium (Temurin JDK)](https://adoptium.net/) | JDK 25 (`winget install EclipseAdoptium.Temurin.25.JDK`) |
| [Install WSL](https://learn.microsoft.com/en-us/windows/wsl/install) | Required by Docker Desktop on Windows |
| [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/) | Containers for PostgreSQL, Testcontainers and Ollama (optional) |
| [GitHub personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) | The `GITHUB_TOKEN` for the API (5,000 requests/hour) |
| [Ollama](https://ollama.com/) · [nomic-embed-text](https://ollama.com/library/nomic-embed-text) | Free local chat and embedding models |
| [Claude Platform docs](https://platform.claude.com/docs) | Hosted model docs, API keys, spend limits, pricing |
| [IntelliJ HTTP Client](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html) | `.http` request files for manual API testing |

## Spring Boot and Spring Framework

| Resource | Use it for |
|---|---|
| [Spring Initializr](https://start.spring.io) | Generating the project skeleton |
| [Spring Boot reference](https://docs.spring.io/spring-boot/index.html) | Everything Boot |
| [Dev services: Docker Compose and Testcontainers at dev time](https://docs.spring.io/spring-boot/reference/features/dev-services.html) | `compose.yaml` started automatically |
| [Testing with Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) | `@ServiceConnection`, integration tests |
| [Database initialization (Flyway)](https://docs.spring.io/spring-boot/how-to/data-initialization.html) | Migrations |
| [Calling REST services (`RestClient`)](https://docs.spring.io/spring-boot/reference/io/rest-client.html) · [Framework REST clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) | The GitHub client |
| [MockRestServiceServer](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-client.html) | Testing the GitHub client without network calls |
| [Task execution and scheduling](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html) | Scheduled sync, executors, virtual threads |
| [Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) | Health checks and metrics |
| [Cloud Native Buildpacks](https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html) | Building the Docker image |
| [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/index.html) | Repositories and entities |
| [Spring Security](https://docs.spring.io/spring-security/reference/index.html) | Reviewer login, CSRF |
| [Spring Modulith](https://docs.spring.io/spring-modulith/reference/index.html) | Module verification (stretch goal S3) |
| [Spring guides](https://spring.io/guides) · [dev.java learning](https://dev.java/learn/) · [Baeldung: Spring Boot](https://www.baeldung.com/spring-boot) | Tutorials when a concept is new |

## Spring AI

| Resource | Use it for |
|---|---|
| [Spring AI reference](https://docs.spring.io/spring-ai/reference/index.html) | Start here |
| [ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | Calling chat models |
| [Prompts and templates](https://docs.spring.io/spring-ai/reference/api/prompt.html) | Versioned `.st` prompt files |
| [Structured output converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html) | Mapping model output to `TriageResult` |
| [Anthropic chat](https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html) · [Ollama chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) | Hosted and local providers |
| [ONNX Transformers embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/onnx.html) · [Ollama embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html) | Embedding options compared in P5.4 |
| [PGvector store](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html) | Reference for pgvector setup (we write our own SQL, per ADR-0003) |
| [Evaluation testing](https://docs.spring.io/spring-ai/reference/api/testing.html) | Spring AI's evaluator ideas (LLM-as-judge) |
| [Observability](https://docs.spring.io/spring-ai/reference/observability/index.html) | Token and latency metrics |
| [Testcontainers support](https://docs.spring.io/spring-ai/reference/api/testcontainers.html) | Containers for model services and vector stores in tests |
| [MCP overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) | Stretch goal S1 |

## GitHub API

| Resource | Use it for |
|---|---|
| [Issues REST API](https://docs.github.com/en/rest/issues/issues) | Listing and fetching issues (`state`, `since`, `labels`) |
| [Issue comments](https://docs.github.com/en/rest/issues/comments) · [Timeline events](https://docs.github.com/en/rest/issues/timeline) | Duplicate ground truth (P1.6, P2.4) |
| [Pagination](https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api) | `Link` header handling |
| [Rate limits](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api) | Primary and secondary limits, headers |
| [Validating webhook deliveries](https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries) | HMAC `X-Hub-Signature-256` |
| [Webhook events and payloads](https://docs.github.com/en/webhooks/webhook-events-and-payloads) | The `issues` event |
| [smee.io](https://smee.io/) | Forwarding webhooks to localhost |
| [GitHub Actions](https://docs.github.com/en/actions) · [Dependabot](https://docs.github.com/en/code-security/dependabot) | CI and dependency updates |

## Database

| Resource | Use it for |
|---|---|
| [pgvector](https://github.com/pgvector/pgvector) | Vector types, `<=>` operator, HNSW indexes |
| [pgvector Docker image](https://hub.docker.com/r/pgvector/pgvector) | `pgvector/pgvector:pg18` |
| [PostgreSQL SELECT (incl. `FOR UPDATE SKIP LOCKED`)](https://www.postgresql.org/docs/current/sql-select.html) | The job queue |
| [PostgreSQL INSERT (incl. `ON CONFLICT`)](https://www.postgresql.org/docs/current/sql-insert.html) | Idempotent upserts |

## Testing and quality

| Resource | Use it for |
|---|---|
| [JUnit user guide](https://docs.junit.org/current/user-guide/) | Tests, parameterised tests |
| [AssertJ](https://assertj.github.io/doc/) · [Mockito](https://site.mockito.org/) | Assertions and mocks |
| [Testcontainers for Java](https://java.testcontainers.org/) | Real PostgreSQL in tests |
| [WireMock](https://wiremock.org/docs/) | HTTP stubs, as an alternative to MockRestServiceServer |
| [ArchUnit user guide](https://www.archunit.org/userguide/html/000_Index.html) | Module boundary rules |
| [JaCoCo Maven plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html) · [Spotless Maven plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven) | Coverage and formatting |
| [Resilience4j](https://resilience4j.readme.io/docs/getting-started) | Retry, rate limiter, circuit breaker, bulkhead |
| [Bucket4j](https://bucket4j.com/) | Rate limiting public endpoints |
| [Gatling](https://gatling.io/docs/) | Load testing (stretch goal S7) |

## UI and documentation

| Resource | Use it for |
|---|---|
| [Thymeleaf](https://www.thymeleaf.org/documentation.html) · [htmx](https://htmx.org/docs/) | Server-rendered UI with partial updates |
| [springdoc-openapi](https://springdoc.org/) | OpenAPI UI (P8.7) |
| [Mermaid](https://mermaid.js.org/intro/) | Diagrams that render on GitHub |
| [ADR resources](https://adr.github.io/) | Architecture decision records |
| [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) | Commit message format |
| [Claude Code docs](https://code.claude.com/docs/en/overview) | The AI coding assistant used in this project |
