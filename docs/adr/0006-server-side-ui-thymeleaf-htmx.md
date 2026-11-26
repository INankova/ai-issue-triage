# ADR-0006: Server-rendered UI with Thymeleaf and htmx

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

The project is backend-focused, but recruiters need something they can click, and reviewers need a usable queue. Frontend work must not eat the schedule.

## Decision

- **Thymeleaf** templates rendered by Spring MVC, plus **[htmx](https://htmx.org/docs/)** for partial page updates (accept or correct without a full reload).
- htmx is served as a static asset from `src/main/resources/static`, because the Initializr htmx starter isn't offered for Spring Boot 4.2 at the time of writing.
- Minimal CSS (one small classless or utility stylesheet). No JavaScript build step.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| React / Angular SPA | Rich interactivity; frontend signal | Separate build, an API surface to design and maintain; big time sink; wrong focus for backend roles |
| REST API + Swagger UI only | Fast | Nothing for non-technical recruiters to look at; no review workflow |

## Consequences

- One deployable, and no CORS or token handling for the UI.
- Controllers are testable with MockMvc, including htmx fragment responses.
- Interactivity is limited, which is acceptable for a review queue and dashboards.
