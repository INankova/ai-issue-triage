# Roadmap

The step-by-step plan for AI Issue Triage, and the **single source of truth for progress**. Tick a box (`- [x]`) in the same commit or pull request that completes the step.

**Legend:** `[x]` done · `[ ]` not done · ⭐ part of the MVP · 🏁 milestone

## Progress

| Phase | Steps | Done | Estimate |
|---|---|---|---|
| 0. Foundations | 17 | 17 | 8–12 h |
| 1. Data ingestion | 7 | 3 | 12–16 h |
| 2. Ground truth and eval dataset | 6 | 0 | 10–14 h |
| 3. LLM classification ⭐ | 9 | 0 | 14–18 h |
| 4. Review UI | 6 | 0 | 10–14 h |
| 5. Embeddings and duplicates | 6 | 0 | 14–18 h |
| 6. Jobs, webhooks, resilience | 6 | 0 | 12–16 h |
| 7. Suggested replies | 4 | 0 | 10–14 h |
| 8. Polish and launch | 7 | 0 | 14–20 h |
| **Core total** | **68** | **20** | **≈ 100–140 h** |
| 9. Stretch goals (pick 1–3) | 9 | 0 | 5–20 h each |
| 10. Job-search packaging | 5 | 0 | 4–6 h |

**MVP = Phases 0–3 + P4.1**, about 45–60 h. From that point the project is worth putting on your CV, so start applying then; don't wait for the full version.

**Cost: $0.** Everything runs on free local models ([ADR-0009](adr/0009-free-by-default-local-models.md)). The hosted provider is implemented in P3.9 but switched on only later, if you choose to (S9).

## Milestones

| | Milestone | Reached when |
|---|---|---|
| 🏁 M1 | Walking skeleton | App + database + CI are green (end of Phase 0). ✅ Reached 2026-11-26 |
| 🏁 M2 | Data in | ~5,000 issues imported, eval sets frozen, baseline measured (end of Phase 2) |
| 🏁 M3 | **MVP** | LLM triage with measured accuracy, visible on a page (Phase 3 + P4.1) |
| 🏁 M4 | Full pipeline | Review workflow, duplicates, background jobs, webhooks (end of Phase 6) |
| 🏁 M5 | Launch | Replies, deployed demo, polished README (end of Phase 8) |

## How to work through a step

1. Create a branch per step, e.g. `feat/p1-2-github-client`.
2. Re-read the step's *Done when* criteria and the linked design section.
3. Write tests first for domain logic, then the code.
4. Run `./mvnw verify` (on Windows: `.\mvnw.cmd verify`) until it's green.
5. Tick the box here and add an ADR if you made a significant decision.
6. Open a pull request (even when working solo), let CI pass, then merge. Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) messages.

**Global definition of done:** tests pass in CI, no secrets in code, docs updated, and you can explain every line.

---

## Phase 0: Foundations (8–12 h)

- [x] **P0.0 Choose the project and dataset.** Verified Spring Boot's labels and issue counts ([ADR-0007](adr/0007-dataset-spring-boot-issues.md)). *Done 2026-11-26.*
- [x] **P0.1 Write the documentation set:** DESIGN, ROADMAP, EVALUATION, ADRs 0001–0009, RESOURCES, CLAUDE.md. *Done 2026-11-26.*
- [x] **P0.2 Initialise the local git repository** on branch `main`. *Done 2026-11-26.*
- [x] **P0.3 First commit:** `docs: add design, roadmap, evaluation plan and ADRs`. *Done 2026-11-26.*
- [x] **P0.4 IntelliJ IDEA installed.** Recommended: update to the latest version (Help → Check for Updates).
- [x] **P0.5 Git installed** (2.45.1). Optional: `winget upgrade Git.Git`.
- [x] **P0.6 Install JDK 25:** Temurin 25.0.5.1 installed with winget; `JAVA_HOME` and `PATH` point to it, and JDK 21 stays installed as a fallback. *Done 2026-11-26.*
- [x] **P0.7 Install WSL 2 and Docker Desktop:** WSL 3.0.5 (with Ubuntu) and Docker Desktop with engine 29.9.3 on the WSL 2 backend; `docker run --rm hello-world` prints its greeting. *Done 2026-11-26.*
- [x] **P0.8 GitHub token:** a fine-grained personal access token with read-only access to public repositories and a 90-day expiry, saved as the user environment variable `GITHUB_TOKEN`.
  *Done 2026-11-26:* the rate-limit endpoint reports `limit: 5000`. The token expires on 2027-02-24, so renew it before then.
- [x] **P0.9 Local LLM:** `winget install Ollama.Ollama`, then pull a small chat model and `nomic-embed-text`.
  *Done 2026-11-26:* Ollama 0.40.18 runs on `localhost:11434` with no account. `ollama list` shows `llama3.2` (3B, 2.0 GB) and `nomic-embed-text` (274 MB, 768-dimension vectors), and `llama3.2` answers a test prompt.
- [x] **P0.10 Local AI performance check ($0):** cap Docker's memory with a `.wslconfig` file (e.g. `memory=4GB`) so IntelliJ, Docker and Ollama fit in 16 GB of RAM. Then time the model with `ollama run llama3.2 --verbose "Classify: NullPointerException on startup"` and note the prompt and eval rates (tokens/s). A hosted API key is **not** needed now ([ADR-0009](adr/0009-free-by-default-local-models.md), stretch goal S9).
  *Done 2026-11-26:* `%USERPROFILE%\.wslconfig` sets `memory=4GB`, and Docker now reports 3.8 GiB instead of 8 GB. `llama3.2` (3B) on Ollama 0.40.18, measured on battery power:

  | Measurement | Result |
  |---|---|
  | Model load (cold start) | ~5 s |
  | Prompt eval (reading the input) | 74–81 tokens/s at first, then 43–47 tokens/s a few minutes later (likely energy saver or CPU heat) |
  | Eval (writing the output) | 14–16 tokens/s at first, then 10–11 tokens/s |
  | ~1,100-token triage prompt with a short JSON answer | 17–32 s |

  With IntelliJ open, `./mvnw verify` (Testcontainers) and the model ran together. Available RAM stayed above 1 GB and page-file use stayed at ~5%, so nothing swapped.
- [x] **P0.11 Public GitHub repository:** create `ai-issue-triage` without an auto-generated README, push, choose a licence (MIT suggested), protect `main` (require a pull request and, once CI exists, a passing build), and add PR and issue templates.
  *Done 2026-11-26:* public at [INankova/ai-issue-triage](https://github.com/INankova/ai-issue-triage) under the MIT licence. `main` requires a pull request; the passing-build check is added in P0.15. A PR template and two issue forms (bug, enhancement) are in `.github/`. Commits use the GitHub noreply address.
- [x] **P0.12 Generate the Spring Boot skeleton** with [start.spring.io](https://start.spring.io) (exact settings in the setup guide):
  - Maven, Java 25, Boot 4.2.x
  - Dependencies: Spring Web, Validation, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Actuator, Docker Compose Support, Testcontainers, Thymeleaf, Configuration Processor
  - No AI dependencies yet ([ADR-0004](adr/0004-spring-ai-for-llm-integration.md)).

  *Done 2026-11-26:* Boot 4.2.0, Java 25, package `io.github.inankova.triage`, main class `TriageApplication`. `./mvnw verify` passes: the context test starts `pgvector/pgvector:pg18` with Testcontainers and Flyway connects to it.
- [x] **P0.13 Database through Docker Compose:** `compose.yaml` with `pgvector/pgvector:pg18`, started automatically by `spring-boot:run`. `/actuator/health` shows each component (`show-components: always`), and `ActuatorHealthTests` checks that `db` is `UP`. *Done 2026-11-26.*
- [x] **P0.14 First migration and first integration test:** `V1__init.sql` runs `CREATE EXTENSION IF NOT EXISTS vector`, and a `@SpringBootTest` with Testcontainers (`@ServiceConnection`) proves the migrations run.
  *Done 2026-11-26:* `FlywayMigrationTests` checks that Flyway applied version 1 and that `vector` is in `pg_extension`. It runs against `pgvector/pgvector:pg18` in Testcontainers, so no local database is needed.
- [x] **P0.15 CI:** `.github/workflows/ci.yml` with JDK 25, a Maven cache, and `./mvnw verify` on every push and pull request. Add the CI badge to the README.
  *Done 2026-11-26:* the workflow runs `./mvnw verify` on JDK 25 (Temurin) with the Maven cache, for every pull request and every push to `main`. A run takes about a minute. A deliberately failing test turned PR #2 red before it was reverted, and the `main` ruleset requires the `build` check.
- [x] **P0.16 Quality tooling:** Spotless (format check in CI), a JaCoCo coverage report, Dependabot (Maven + GitHub Actions), and a first ArchUnit test (no cycles between packages).
  *Done 2026-11-26:* `./mvnw verify` runs `ArchitectureTests` (no cycles between modules), writes the JaCoCo report to `target/site/jacoco/`, and runs `spotless:check` with palantir-java-format (fix with `spotless:apply`). `.github/dependabot.yml` checks Maven and GitHub Actions weekly.

🏁 **M1: walking skeleton** ✅ reached 2026-11-26

## Phase 1: Data ingestion (12–16 h)

Design: [DESIGN §7](DESIGN.md#7-data-model), [§12](DESIGN.md#12-external-integrations)

- [x] **P1.1 Issue schema and domain:** a migration for `issue` and `import_run`, a JPA entity and a repository, plus a repository test with Testcontainers.
  *Done 2026-11-26:* `V2__create_issue_and_import_run.sql` creates both tables. In the new `issues` module, `Issue` is a read-only entity (`@Immutable`, no setters), because GitHub is the source of truth and the sync will write rows with a SQL upsert (P1.4). `IssueRepository` exposes only `findByRepoAndNumber`. `IssueRepositoryTests` (`@DataJpaTest` + Testcontainers) inserts rows with SQL and checks every mapped column. Hibernate now validates the entities against the Flyway schema at startup (`ddl-auto: validate`). The `ImportRun` entity follows in P1.5, where it is first used.
- [x] **P1.2 GitHub client:** Spring `RestClient`, typed records containing only the fields we use, the token header, pagination through the `Link` header, and error mapping. Test with MockRestServiceServer and JSON fixtures recorded from real responses.
  *Done 2026-11-27:* `GitHubClient` (in `ingestion.internal`) fetches issues newest first, following the `Link` header's next URL as-is, because GitHub pages with an opaque cursor. It stops at a limit and turns error statuses into `GitHubApiException` with GitHub's message. The token comes from `triage.github.token` (`GITHUB_TOKEN`) and is masked in `GitHubProperties.toString()`. `GitHubClientTests` cover two pages, the limit, field mapping (including bots and pull requests), a 401 error, the missing-token case and repo validation. The fixtures in `src/test/resources/fixtures/github/` were recorded without a token on 2026-11-27 and anonymised: titles, bodies and human logins are replaced.
- [x] **P1.3 Rate limits and retries:** read `x-ratelimit-*` headers and wait until the reset; Resilience4j retry for 5xx errors and timeouts; handle secondary limits (`403`/`429` with `retry-after`).
  *Done 2026-11-27:* on a rate-limited `403`/`429`, `GitHubClient` waits as GitHub asks, following its rate-limit guide:
  - `retry-after` first;
  - then `x-ratelimit-remaining: 0` with the reset time, plus 1 s for clock skew;
  - otherwise one minute.

  It waits at most three times per request. A `403` that is not a rate limit fails at once. Resilience4j (`resilience4j-retry` 2.4.0, configured in code) retries 5xx responses and network errors: three attempts, backing off 1 s and then 2 s. HTTP timeouts are set through `spring.http.clients.*` (connect 10 s, read 30 s). `GitHubClientRetryTests` use a fixed `Clock` and a recording `Sleeper`. They cover hitting the primary and secondary limits and recovering, giving up after the retries, a timeout, and a non-limit `403`.
- [ ] **P1.4 Idempotent upsert and incremental sync:** `ON CONFLICT (github_id) DO UPDATE`, a `content_hash`, the `since` parameter with the last sync time, and pull requests skipped (they have a `pull_request` field).
  *Done when:* importing twice gives the same row count, and a changed issue gets a new hash.
- [ ] **P1.5 Import command:** `POST /admin/imports` (or a command-line runner on an `import` profile) taking a repo and a limit, and writing `import_run` statistics.
  *Done when:* about 5,000 recent issues are imported locally in one run, within rate limits.
- [ ] **P1.6 Comments for the duplicate subset:** fetch comments and timeline cross-references only for issues labelled `status: duplicate`.
  *Done when:* comments are stored for that subset and the count is logged.
- [ ] **P1.7 Data exploration report:** `docs/DATA.md` covering label distribution, bot share, conflicting labels, body lengths and issues per year, with the SQL queries included.
  *Done when:* the report is committed and its findings feed P2.1.

## Phase 2: Ground truth and evaluation dataset (10–14 h)

Design: [EVALUATION](EVALUATION.md)

- [ ] **P2.1 Label mapping in code** (`issues` module), following [DESIGN §8.1](DESIGN.md#81-taxonomy-and-ground-truth-mapping), with the precedence rules checked against the conflict counts from P1.7.
  *Done when:* a parameterised test covers a table of label sets → expected category.
- [ ] **P2.2 Noise filter:** exclude bot authors, excluded labels and pull requests.
  *Done when:* it's tested and DATA.md shows before/after counts.
- [ ] **P2.3 Splits and frozen datasets:** split by time into an older train pool, a dev set (~200) and a stratified test set (~300). Store the files in `src/test/resources/eval/` with **issue numbers and expected labels only**, a fixed random seed and dataset version `v1`.
  *Done when:* the files are committed and one command reproduces them.
- [ ] **P2.4 Duplicate ground truth:** extract issue → original pairs ([EVALUATION §2.3](EVALUATION.md#23-duplicate-pairs-v1)), keeping only earlier issues in the same repo, and spot-check 30 by hand.
  *Done when:* the pairs file is committed and the spot-check precision is recorded.
- [ ] **P2.5 Eval runner:** an `evaluation` module with a `Classifier` interface, a runner, metrics (precision, recall, F1, macro-F1, confusion matrix, coverage curve) and JSON + Markdown reports.
  *Done when:* unit tests check the metrics math against hand-computed examples.
- [ ] **P2.6 Baselines:** B0 (majority class) and B1 (keyword rules), recorded in the [experiment log](EVALUATION.md#8-experiment-log).
  *Done when:* the baseline rows exist in the log.

🏁 **M2: data in, baseline measured**

## Phase 3: LLM classification ⭐ (14–18 h)

Design: [DESIGN §8](DESIGN.md#8-triage)

- [ ] **P3.1 Spring AI and the `llm` gateway:** add Spring AI with Ollama, an `LlmGateway` interface, the `local` profile as the default, and the `llm_call` table for tokens, cost and latency.
  *Done when:* a manual smoke call works on `local`, and unit tests use a stubbed `ChatModel`.
- [ ] **P3.2 Prompt v1 and output contract:** the `TriageResult` record, `prompts/triage/v1.st`, delimiters and truncation.
  *Done when:* a golden test confirms the rendered prompt for a fixture issue.
- [ ] **P3.3 Validation, retry, fallback:** Bean Validation, one retry that includes the error, then `INVALID_OUTPUT`.
  *Done when:* tests feed malformed, missing and out-of-range outputs.
- [ ] **P3.4 Persist results:** a `triage_result` migration and end-to-end classification of an issue.
  *Done when:* an integration test with a stubbed model passes.
- [ ] **P3.5 Confidence routing:** a threshold property that splits results into `AUTO_ACCEPTED` and `NEEDS_REVIEW`.
  *Done when:* tests cover both sides of the threshold.
- [ ] **P3.6 Eval, v1 → v3:** iterate the prompt on a fixed 50–100 issue subset of the **dev** split (fast enough on a CPU). Run the **test** split overnight, and only for v1 and the final version. Log every run.
  *Done when:* the README results table shows real numbers next to the baselines.
- [ ] **P3.7 Model comparison ($0):** run the same prompt on 2–3 local models of different sizes (e.g. ~1B, ~3B and, if RAM allows, ~7–8B). Compare macro-F1, coverage at 95% precision, p50/p95 latency and memory use. Choose the default and record it in ADR-0010. Hosted rows are added later (S9).
  *Done when:* the comparison table and ADR-0010 are committed.
- [ ] **P3.8 Cost guardrails:** a content-hash cache, a daily budget cap in the gateway (it matters once the `cloud` profile is on), and cost metrics.
  *Done when:* a test proves the cap stops calls, and re-importing doesn't re-classify unchanged issues.
- [ ] **P3.9 Hosted provider, implemented but off:** add the Spring AI Anthropic integration behind the `cloud` profile, with model names and prices in `application-cloud.yml`. Contract-test it against a WireMock stub of the provider API (`spring.ai.anthropic.base-url` pointed at WireMock), covering request mapping, structured-output parsing and rate-limit/overload errors. Document in the README how to switch it on.
  *Done when:* the contract tests pass with no API key and no network access, and the README explains the switch.

## Phase 4: Review UI (10–14 h)

Design: [ADR-0006](adr/0006-server-side-ui-thymeleaf-htmx.md)

- [ ] **P4.1 ⭐ Issues page:** a list showing predicted vs. actual labels, filters (disagreements, needs review), and an issue detail page with the rationale.
  *Done when:* MockMvc tests pass and the page works on a laptop screen.

🏁 **M3: MVP**. Start applying for jobs now.

- [ ] **P4.2 Review actions:** accept or correct with htmx partial updates, stored in a `review_decision` table.
  *Done when:* both actions are stored and tested.
- [ ] **P4.3 Uncertainty-first queue:** order the queue by lowest confidence and show counts per status.
  *Done when:* the ordering is tested.
- [ ] **P4.4 Metrics dashboard:** latest eval results, live agreement rate from reviews, cost per day.
  *Done when:* the dashboard renders from real data.
- [ ] **P4.5 Security:** Spring Security form login for reviewer actions (credentials from environment variables), CSRF protection, read-only anonymous browsing.
  *Done when:* tests cover 401, 403 and allowed requests.
- [ ] **P4.6 Corrections → data:** export reviewed items as a new labelled set, and document how it could extend the train pool (never the test set).
  *Done when:* the export works and the rules are documented.

## Phase 5: Embeddings and duplicates (14–18 h)

Design: [DESIGN §9](DESIGN.md#9-duplicate-detection), [ADR-0003](adr/0003-postgresql-with-pgvector.md)

- [ ] **P5.1 Embeddings through the gateway:** Spring AI Transformers (ONNX) by default, an `issue_embedding` table with an HNSW index, and text preprocessing.
  *Done when:* the migration creates the index and the embedding code is tested with a stub.
- [ ] **P5.2 Backfill job:** embed all imported issues in batches.
  *Done when:* every issue has an embedding and the duration is recorded.
- [ ] **P5.3 Similar-issues search:** a `JdbcClient` query using `<=>`, restricted to earlier issues and excluding the issue itself, plus an endpoint and a "Similar issues" panel.
  *Done when:* an integration test with known vectors returns the expected order.
- [ ] **P5.4 Retrieval eval and embedding model comparison:** recall@k and MRR on the duplicate pairs, comparing ONNX with Ollama `nomic-embed-text` (both free). Record the choice in an ADR.
  *Done when:* the comparison and the ADR are committed.
- [ ] **P5.5 Duplicate judge:** an LLM judge over the top-k candidates with a validated verdict, stored in `duplicate_candidate`.
  *Done when:* precision and recall of the final decisions are in the experiment log.
- [ ] **P5.6 kNN baseline and dynamic few-shot:** B2 (kNN label vote), then a new prompt version with examples retrieved from the train pool.
  *Done when:* both rows are in the experiment log.

## Phase 6: Jobs, webhooks, resilience (12–16 h)

Design: [DESIGN §11](DESIGN.md#11-background-processing), [ADR-0005](adr/0005-postgres-backed-job-queue.md)

- [ ] **P6.1 Job queue:** a `job` table and workers using `FOR UPDATE SKIP LOCKED`, virtual threads, and a concurrency limit per job type.
  *Done when:* a concurrency test (N workers, M jobs) shows each job processed exactly once.
- [ ] **P6.2 Retries and dead jobs:** exponential backoff, the `DEAD` status, and an admin page.
  *Done when:* tests cover retry and dead-lettering.
- [ ] **P6.3 Pipeline wiring:** upsert → EMBED → CLASSIFY + DUPLICATE_CHECK → DRAFT_REPLY, with idempotency keys.
  *Done when:* an end-to-end integration test with stubs passes.
- [ ] **P6.4 Webhook endpoint:** HMAC SHA-256 verification with a constant-time comparison, deduplication by delivery ID, and a `202` response after enqueueing. Try it with smee.io against your own sandbox repo.
  *Done when:* unit tests cover valid, invalid and missing signatures, and you've seen one real delivery arrive.
- [ ] **P6.5 Resilience4j:** timeouts, a rate limiter and a circuit breaker around the LLM and GitHub calls, with metrics.
  *Done when:* fault-injection tests pass.
- [ ] **P6.6 Scheduled incremental sync** as a fallback for missed webhooks.
  *Done when:* the schedule runs and is tested.

🏁 **M4: full pipeline**

## Phase 7: Suggested replies (10–14 h)

Design: [DESIGN §10](DESIGN.md#10-suggested-replies-rag)

- [ ] **P7.1 Knowledge base:** resolved issues with maintainer answers, embedded; optionally a few chunked reference-doc pages.
  *Done when:* the sources are stored and searchable.
- [ ] **P7.2 Reply drafting:** grounding and citation rules, the Stack Overflow redirect template for `QUESTION`, and specific follow-up questions for `needsInfo`.
  *Done when:* outputs are validated and every citation must come from the retrieved sources.
- [ ] **P7.3 Review and approve replies** in the UI. Never auto-post.
  *Done when:* approval works and is tested.
- [ ] **P7.4 Reply eval:** a rubric (grounded, cites sources, correct, polite) on ~30 items, scored by an LLM judge with a manual agreement check.
  *Done when:* the numbers are in the experiment log.

## Phase 8: Polish and launch (14–20 h)

- [ ] **P8.1 Observability:** the metrics from [DESIGN §15](DESIGN.md#15-observability), Spring AI observations, structured logs, and an optional Grafana profile.
  *Done when:* the metrics are visible locally.
- [ ] **P8.2 Security pass:** verify the [DESIGN §13](DESIGN.md#13-security-and-privacy) checklist and run the adversarial eval set.
  *Done when:* the injection success rate is 0, or any failures are documented.
- [ ] **P8.3 Container image:** `spring-boot:build-image` and a `compose.prod.yaml`.
  *Done when:* the image runs locally with environment variables only.
- [ ] **P8.4 Deploy with demo mode:** the demo serves precomputed results only, with no live model calls, so the server needs no Ollama. Choose a free hosting option (ADR), seed a snapshot and make it read-only. If no free option fits, publish the video and the one-command local run instead.
  *Done when:* the README links to a working public demo, or to the video and local-run instructions.
- [ ] **P8.5 Eval in GitHub Actions:** an `eval.yml` workflow (manual trigger) that runs a small local model with Ollama on a ~20-issue subset (free on public repositories) and puts a summary in the job output. Switch it to the hosted model once S9 is done.
  *Done when:* a run completes and shows its summary.
- [ ] **P8.6 Final README:** GIF or 2-minute video, architecture diagram, results tables, decisions, limitations, and "How I used AI tools".
  *Done when:* someone who hasn't seen the project understands it in 2 minutes.
- [ ] **P8.7 API docs and developer ergonomics:** springdoc OpenAPI UI and `http/` request files for the IntelliJ HTTP client.
  *Done when:* both are committed and work.

🏁 **M5: launch**

## Phase 9: Stretch goals (pick 1–3)

- [ ] **S1 MCP server** exposing tools such as `search_similar_issues`, `get_triage` and `triage_stats` (Spring AI MCP server starter).
- [ ] **S2 GitHub App** on your own sandbox repo that posts *approved* labels and replies.
- [ ] **S3 Spring Modulith** verification and generated module docs, once a GA release supports our Boot version.
- [ ] **S4 Confidence calibration chart** (reliability diagram) in the README.
- [ ] **S5 Batch backfill** through a provider batch API (cheaper, asynchronous).
- [ ] **S6 Second repository** (spring-framework) with per-repo label mapping configuration.
- [ ] **S7 Load test** (Gatling) of import + classify with a stubbed LLM.
- [ ] **S8 Hybrid retrieval** (full-text + vectors, reciprocal rank fusion) for replies, measured.
- [ ] **S9 Switch on the hosted provider**, when you're ready to pay: an API key with a monthly spending limit, then run the test set with 1–2 hosted models and add their rows to the comparison. Expect roughly $0.10–$3 per 300-issue eval run ([DESIGN §14](DESIGN.md#14-cost-model)).

## Phase 10: Job-search packaging (4–6 h)

- [ ] **J1 CV bullets** containing real numbers from the experiment log.
- [ ] **J2 A short write-up or LinkedIn post** about one interesting finding (e.g. "the kNN baseline beat my first prompt").
- [ ] **J3 A 2-minute demo video.**
- [ ] **J4 Interview stories:** practise 5 STAR stories from this project.
- [ ] **J5 Pin the repo** on your GitHub profile and add a profile README.
