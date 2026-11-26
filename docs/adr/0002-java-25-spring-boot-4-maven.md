# ADR-0002: Java 25, Spring Boot 4.2, Maven

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

The target roles are junior Java/backend positions, mostly in enterprise teams. The stack should be current, so it shows we keep up, but stable enough not to slow the project down.

Facts checked on 2026-11-26:
- [start.spring.io](https://start.spring.io) defaults to **Spring Boot 4.2.0** and offers Java 17, 21, 25 and 27.
- Java 25 is the latest LTS release; Java 27 is not an LTS release.
- For Boot 4.2, Initializr selects **Spring AI 2.1.0**, while **Spring Modulith** is only at a milestone (`2.2.0-M1`). The Initializr htmx starter isn't offered for Boot 4.2.
- The development machine currently has JDK 21 (Temurin) installed.

## Decision

- **Java 25 (LTS)**, installed alongside JDK 21.
- **Spring Boot 4.2.x**: start on the latest stable release and let Dependabot handle patch upgrades.
- **Maven** with the Maven Wrapper (`mvnw`), so nobody needs Maven installed.
- Library versions come from the Spring Boot and Spring AI BOMs; don't pin versions by hand unless necessary.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| Java 21 | Already installed; very widely used | Not the newest LTS; 25 costs two clicks to install |
| Gradle (Kotlin DSL) | Fast and flexible; popular | Maven is more common in enterprise job ads and simpler for a first Spring project |
| Spring Boot 4.1.x | Spring Modulith GA and the htmx starter are available | Older release line; 4.2 brings Spring AI 2.1 and a longer support window ahead |

## Consequences

- Module boundaries are enforced with ArchUnit for now; Spring Modulith can be adopted once a GA release supports Boot 4.2 ([ADR-0008](0008-modular-monolith-enforced-boundaries.md)).
- htmx is served as a static asset instead of through a starter ([ADR-0006](0006-server-side-ui-thymeleaf-htmx.md)).
- If any tool lags behind Java 25, temporarily drop to 21 (already installed) and note it here.
