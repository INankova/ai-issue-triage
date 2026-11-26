# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-26

## Context

This is a solo portfolio project that reviewers and interviewers will read. The most common interview question about a project is *"why did you choose X?"*, and the honest answer is easy to forget a few months later.

## Decision

Record every significant decision as a short Architecture Decision Record in `docs/adr/`, using [0000-template.md](0000-template.md).

- Number the files sequentially (`0001-…`).
- Once an ADR is accepted, don't rewrite it. If a decision changes, write a new ADR and mark the old one *Superseded*.
- A decision is "significant" if it would take more than a day to reverse, or if an interviewer could reasonably ask about it.

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| No written decisions | No effort | Reasons get lost; weaker interview answers |
| A long design document only | One place to read | Decisions get buried and history is lost when the document is edited |

## Consequences

- The repository carries a readable history of trade-offs that reviewers can scan in minutes.
- Each ADR doubles as interview preparation.
- Writing one costs about 15 minutes per decision.
