# ADR-0008: Modular monolith with boundaries enforced by tests

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

Reviewers judge structure within minutes. Classic "layer" packages (`controller`, `service`, `repository`) mix every feature together and let anything call anything. The author already knows Clean Architecture from a .NET solution with separate Domain, Application, Infrastructure and API projects.

## Decision

- Organise code **by feature** into the modules listed in [DESIGN §6.2](../DESIGN.md).
- Each module exposes a small public API (classes in its top-level package). Anything in a sub-package named `internal` is private to the module.
- Enforce this with **ArchUnit** tests that run in every build:
  - Nothing outside module X may access `X.internal..`.
  - Only the `llm` module may depend on Spring AI chat and embedding APIs.
  - There are no cycles between modules.
- Adopt **Spring Modulith** verification (and its generated module documentation) once a GA release supports our Spring Boot version.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| Technical layers | Familiar to everyone | Features couple together; boundaries exist only in people's heads |
| Multi-module Maven build | Compiler-enforced isolation | More build ceremony for a solo project; can be introduced later |
| One Maven module per Clean Architecture layer (as in the author's previous .NET project) | Strict separation | Heavy for this size; package-per-feature plus tests gives most of the benefit |

## Consequences

- Boundary violations fail CI instead of slowly rotting the design.
- Easy to explain: *"this is the triage module; it only talks to `llm` and `issues` through their public interfaces."*
- Moving to Spring Modulith or a multi-module build later stays straightforward.
