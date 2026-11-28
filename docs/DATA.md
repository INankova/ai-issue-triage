# Data report

What the imported issues look like, measured before any evaluation dataset is built (ROADMAP P1.7). The findings decide the label mapping (P2.1), the dataset splits (P2.3) and the duplicate pairs (P2.4).

| | |
|---|---|
| **Snapshot** | Import run #2, 2026-11-28 |
| **Data** | The 5,000 most recently created issues of [spring-projects/spring-boot](https://github.com/spring-projects/spring-boot): #46734–#52970, created 2025-08-08 to 2026-11-27 |
| **Not imported** | 1,112 pull requests (6,112 entries seen) |
| **Reproduce** | Run the queries below against the local database (`docker exec -it ai-issue-triage-postgres-1 psql -U triage -d triage`). Numbers move a little whenever the import runs again. |

This report holds counts only, no issue text ([CLAUDE.md](../CLAUDE.md)).

## Key findings

1. **Only 18% of the issues are usable for classification.** 898 of 5,000 have no excluded label and exactly one category. Three labels exclude most issues: `type: dependency-upgrade` (33%), `status: forward-port` (29%) and `type: task` (24%).
2. **The four main categories are balanced; QUESTION is rare.** Among usable issues there are 247 BUG, 247 INVALID, 243 ENHANCEMENT and 149 DOCUMENTATION issues. QUESTION (`for: stackoverflow`) appears on only **28 issues** in total, and only 2 fall in the most recent 300 usable issues. The planned test mix of 50 questions (EVALUATION §2.2) is impossible with this data.
3. **The `needsInfo` proxy has no data.** `status: waiting-for-feedback` (DESIGN §8.1) appears on **0** issues. Maintainers remove it once feedback arrives, so the current labels can't serve as ground truth. Only the label history (timeline `labeled` and `unlabeled` events) still shows it.
4. **There will be few duplicate pairs.** 140 issues are marked as duplicates. Comments on 43 of them name an original ("duplicate of #N" or an issue link), but only **20** of those originals are in our database and older than the duplicate. That is far below the 100 pairs EVALUATION §2.3 asks for. Of the 46 references found, 21 point to issues older than our window, so a longer import would recover most of them.
5. **No issue has a bot author.** The dependency-upgrade issues are opened by team members, not by a GitHub App. The exclusion therefore has to work through labels; the bot filter alone excludes nothing here.
6. **Category conflicts are rare and follow one pattern.** Only 18 kept issues have two categories: 16 are QUESTION + INVALID (most likely questions redirected to Stack Overflow and also marked invalid) and 2 are DOCUMENTATION + INVALID. The precedence rule QUESTION > INVALID > DOCUMENTATION handles both.
7. **Bodies vary a lot in length.** The median usable BUG body is about 700 characters, but QUESTION bodies have a median of about 2,800 and a 90th percentile of about 17,700 characters. The prompt will need to truncate long bodies (P3.2); at roughly 4 characters per token, 4,000 characters is about 1,000 tokens.
8. **Recent months have fewer usable issues.** About 100 a month in autumn 2025 drop to about 25 a month in 2026. The newest 300 usable issues therefore stretch over 8.5 months (2026-03-13 to 2026-11-26).

## Consequences for Phase 2

| Step | What to do |
|---|---|
| Before P2.1 | **Import a longer history**, for example `--limit=20000` (about 250 requests and 5 minutes). That gives more QUESTION and DOCUMENTATION issues, a larger train pool, and puts more duplicate originals in the database. Then re-run this report. |
| P2.1 | Keep the precedence QUESTION > INVALID > DOCUMENTATION > BUG > ENHANCEMENT; the data confirms the only conflicts it needs to settle. Leave issues with no category out (422 kept issues, mostly `status: declined`, `status: duplicate`, no labels, `for: external-project`). Decide whether `needsInfo` stays in v1 (then it needs label history from timelines) or moves to a later version. |
| P2.3 | Set the test mix from the real distribution instead of the planned one. If QUESTION stays rare, report its numbers separately with a confidence interval instead of padding it, or drop it from the macro average. |
| P2.4 | Expect about 20 pairs from comment rule 1 on this snapshot. Add the longer history first, then rule 2 (maintainer `#N` references), then decide on a classic token for cross-references (ROADMAP P1.6). |

## Method

Every query uses one view, which applies the label mapping of DESIGN §8.1 and the exclusion rules of EVALUATION §2.1. A **kept** issue has no bot author and no excluded label. A **usable** issue is kept and has exactly one category.

```sql
CREATE TEMP VIEW labelled AS
SELECT i.*,
       array_remove(ARRAY[
           CASE WHEN i.labels && ARRAY['type: bug', 'type: regression'] THEN 'BUG' END,
           CASE WHEN 'type: enhancement' = ANY (i.labels) THEN 'ENHANCEMENT' END,
           CASE WHEN i.labels && ARRAY['type: documentation', 'type: wiki-documentation'] THEN 'DOCUMENTATION' END,
           CASE WHEN 'for: stackoverflow' = ANY (i.labels) THEN 'QUESTION' END,
           CASE WHEN 'status: invalid' = ANY (i.labels) THEN 'INVALID' END
       ], NULL) AS categories,
       i.labels && ARRAY['type: task', 'type: dependency-upgrade', 'type: epic',
                         'status: forward-port', 'status: back-port'] AS has_excluded_label
FROM issue i
WHERE i.repo = 'spring-projects/spring-boot';
```

## 1. Overview

```sql
SELECT count(*) AS issues, min(created_at)::date AS first_created, max(created_at)::date AS last_created,
       count(*) FILTER (WHERE state = 'OPEN') AS open, count(*) FILTER (WHERE state = 'CLOSED') AS closed
FROM labelled;

SELECT coalesce(state_reason, '(none)') AS state_reason, count(*) AS issues
FROM labelled WHERE state = 'CLOSED' GROUP BY 1 ORDER BY 2 DESC;
```

| Issues | First created | Last created | Open | Closed |
|---:|---|---|---:|---:|
| 5,000 | 2025-08-08 | 2026-11-27 | 129 | 4,871 |

| State reason (closed) | Issues |
|---|---:|
| completed | 4,085 |
| not_planned | 683 |
| duplicate | 103 |

Almost everything is closed, so the final labels are mostly in place. The 26 issues still `status: waiting-for-triage` are all open; the oldest is from 2026-03-11.

## 2. Labels

```sql
SELECT label, count(*) AS issues, round(100.0 * count(*) / (SELECT count(*) FROM labelled), 1) AS percent
FROM labelled, unnest(labels) AS label
GROUP BY label ORDER BY issues DESC, label;

SELECT cardinality(labels) AS labels, count(*) AS issues FROM labelled GROUP BY 1 ORDER BY 1;
```

| Label | Issues | % |
|---|---:|---:|
| type: dependency-upgrade | 1,664 | 33.3 |
| status: forward-port | 1,446 | 28.9 |
| status: team-only | 1,206 | 24.1 |
| type: task | 1,199 | 24.0 |
| type: bug | 701 | 14.0 |
| type: documentation | 380 | 7.6 |
| status: superseded | 373 | 7.5 |
| status: invalid | 268 | 5.4 |
| type: enhancement | 247 | 4.9 |
| status: declined | 152 | 3.0 |
| for: external-project | 139 | 2.8 |
| status: duplicate | 119 | 2.4 |
| type: regression | 76 | 1.5 |
| type: wiki-documentation | 31 | 0.6 |
| for: stackoverflow | 28 | 0.6 |
| status: pending-design-work | 28 | 0.6 |
| status: waiting-for-triage | 26 | 0.5 |
| theme: grpc | 25 | 0.5 |
| for: upgrade-attention | 18 | 0.4 |
| status: feedback-provided | 13 | 0.3 |
| status: blocked | 11 | 0.2 |
| 13 rarer labels | 1–5 each | ≤ 0.1 |

Most issues carry one or two labels: 71 have none, 1,766 have one, 3,074 have two and 89 have three. `status: waiting-for-feedback` is not in the list at all (finding 3).

## 3. Exclusions

```sql
SELECT count(*) FILTER (WHERE author_is_bot) AS bot_authors,
       count(*) FILTER (WHERE has_excluded_label) AS excluded_label,
       count(*) FILTER (WHERE has_excluded_label AND cardinality(categories) > 0) AS excluded_but_categorised,
       count(*) FILTER (WHERE NOT author_is_bot AND NOT has_excluded_label) AS kept
FROM labelled;

SELECT label, count(*) AS issues
FROM labelled, unnest(labels) AS label
WHERE label IN ('type: task', 'type: dependency-upgrade', 'type: epic', 'status: forward-port', 'status: back-port')
GROUP BY label ORDER BY issues DESC;
```

| Bot authors | With an excluded label | Excluded but categorised | Kept |
|---:|---:|---:|---:|
| 0 | 3,662 | 797 | 1,338 |

| Excluded label | Issues |
|---|---:|
| type: dependency-upgrade | 1,664 |
| status: forward-port | 1,446 |
| type: task | 1,199 |
| status: back-port | 3 |

792 of the 797 "excluded but categorised" issues are forward ports: copies of a bug or enhancement for another branch. They carry the category of the original, so keeping them would count the same issue twice.

## 4. Categories

```sql
SELECT cardinality(categories) AS categories, count(*) AS issues
FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label GROUP BY 1 ORDER BY 1;

SELECT categories[1] AS category, count(*) AS issues, round(100.0 * count(*) / sum(count(*)) OVER (), 1) AS percent
FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 1
GROUP BY 1 ORDER BY 2 DESC;

SELECT array_to_string(categories, ' + ') AS combination, count(*) AS issues
FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) > 1
GROUP BY 1 ORDER BY 2 DESC;

SELECT coalesce(label, '(no labels)') AS label, count(*) AS issues
FROM labelled LEFT JOIN LATERAL unnest(labels) AS label ON true
WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 0
GROUP BY 1 ORDER BY 2 DESC LIMIT 10;
```

| Categories per kept issue | Issues |
|---:|---:|
| 0 | 422 |
| 1 | 898 |
| 2 | 18 |

| Category (usable issues) | Issues | % |
|---|---:|---:|
| BUG | 247 | 27.5 |
| INVALID | 247 | 27.5 |
| ENHANCEMENT | 243 | 27.1 |
| DOCUMENTATION | 149 | 16.6 |
| QUESTION | 12 | 1.3 |

| Conflict | Issues | Winner by precedence |
|---|---:|---|
| QUESTION + INVALID | 16 | QUESTION |
| DOCUMENTATION + INVALID | 2 | INVALID |

With the precedence applied, the classes would be BUG 247, INVALID 249, ENHANCEMENT 243, DOCUMENTATION 149 and QUESTION 28 (916 issues).

| Most common labels on kept issues without a category | Issues |
|---|---:|
| status: declined | 123 |
| status: duplicate | 95 |
| (no labels) | 71 |
| for: external-project | 65 |
| status: superseded | 62 |
| status: waiting-for-triage | 26 |
| status: feedback-provided | 7 |

These stay out of classification, as EVALUATION §2.1 says. `for: external-project` (the problem belongs to another project) is close to INVALID in spirit; P2.1 can decide whether to map it.

## 5. Flags

```sql
SELECT count(*) FILTER (WHERE 'type: regression' = ANY (labels)) AS regression,
       count(*) FILTER (WHERE 'status: waiting-for-feedback' = ANY (labels)) AS waiting_for_feedback,
       count(*) FILTER (WHERE 'status: duplicate' = ANY (labels) OR state_reason = 'duplicate') AS duplicate
FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 1;
```

| `regression` | `waiting-for-feedback` (needsInfo proxy) | Duplicate |
|---:|---:|---:|
| 34 | 0 | 23 |

34 regressions (14% of the 247 bugs) are enough to report the `regression` flag. `needsInfo` has no ground truth in the current labels (finding 3).

## 6. Text lengths

```sql
SELECT count(*) FILTER (WHERE body IS NULL OR body = '') AS empty_bodies,
       percentile_disc(0.5) WITHIN GROUP (ORDER BY length(coalesce(body, ''))) AS body_p50,
       percentile_disc(0.9) WITHIN GROUP (ORDER BY length(coalesce(body, ''))) AS body_p90,
       percentile_disc(0.99) WITHIN GROUP (ORDER BY length(coalesce(body, ''))) AS body_p99,
       max(length(coalesce(body, ''))) AS body_max,
       count(*) FILTER (WHERE length(coalesce(body, '')) > 6000) AS body_over_6000,
       percentile_disc(0.5) WITHIN GROUP (ORDER BY length(title)) AS title_p50,
       max(length(title)) AS title_max
FROM labelled;

SELECT categories[1] AS category,
       percentile_disc(0.5) WITHIN GROUP (ORDER BY length(coalesce(body, ''))) AS body_p50,
       percentile_disc(0.9) WITHIN GROUP (ORDER BY length(coalesce(body, ''))) AS body_p90
FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 1
GROUP BY 1 ORDER BY 1;
```

All lengths are in characters.

| | Median | 90th percentile | 99th percentile | Max |
|---|---:|---:|---:|---:|
| Body, all issues | 97 | 1,567 | 7,141 | 59,027 |
| Title, all issues | 44 | | | 235 |

177 bodies are empty and 67 are longer than 6,000 characters. The all-issue median is low because forward ports and dependency upgrades have almost no body.

| Usable issues by category | Body median | Body 90th percentile |
|---|---:|---:|
| BUG | 707 | 4,245 |
| DOCUMENTATION | 428 | 2,035 |
| ENHANCEMENT | 399 | 1,919 |
| INVALID | 1,485 | 6,420 |
| QUESTION | 2,839 | 17,714 |

## 7. Issues over time

```sql
SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, count(*) AS issues,
       count(*) FILTER (WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 1) AS usable
FROM labelled GROUP BY 1 ORDER BY 1;
```

| Month | Issues | Usable |
|---|---:|---:|
| 2025-08 (from the 8th) | 226 | 39 |
| 2025-09 | 305 | 100 |
| 2025-10 | 465 | 115 |
| 2025-11 | 380 | 115 |
| 2025-12 | 255 | 72 |
| 2026-01 | 307 | 54 |
| 2026-02 | 281 | 64 |
| 2026-03 | 422 | 85 |
| 2026-04 | 330 | 65 |
| 2026-05 | 278 | 21 |
| 2026-06 | 184 | 32 |
| 2026-07 | 213 | 25 |
| 2026-08 | 268 | 25 |
| 2026-09 | 290 | 33 |
| 2026-10 | 463 | 32 |
| 2026-11 (to the 27th) | 333 | 21 |

The window covers 16 months, so a per-year count would hide the trend: usable issues per month fall by about three quarters from autumn 2025 to 2026.

## 8. Time split preview

The planned split takes the newest usable issues for testing (EVALUATION §2.2). This query shows what that would give on today's data, before stratification.

```sql
WITH usable AS (
    SELECT categories[1] AS category, created_at, row_number() OVER (ORDER BY created_at DESC) AS newest_first
    FROM labelled WHERE NOT author_is_bot AND NOT has_excluded_label AND cardinality(categories) = 1
), split AS (
    SELECT *, CASE WHEN newest_first <= 300 THEN '1 test' WHEN newest_first <= 500 THEN '2 dev' ELSE '3 train' END AS part
    FROM usable
)
SELECT part, count(*) AS issues, min(created_at)::date AS from_date, max(created_at)::date AS to_date,
       count(*) FILTER (WHERE category = 'BUG') AS bug, count(*) FILTER (WHERE category = 'ENHANCEMENT') AS enhancement,
       count(*) FILTER (WHERE category = 'DOCUMENTATION') AS documentation,
       count(*) FILTER (WHERE category = 'QUESTION') AS question, count(*) FILTER (WHERE category = 'INVALID') AS invalid
FROM split GROUP BY part ORDER BY part;
```

| Part | Issues | From | To | BUG | ENH | DOC | QUESTION | INVALID |
|---|---:|---|---|---:|---:|---:|---:|---:|
| Test (newest 300) | 300 | 2026-03-13 | 2026-11-26 | 110 | 69 | 41 | 2 | 78 |
| Dev (next 200) | 200 | 2025-12-13 | 2026-03-13 | 47 | 50 | 37 | 4 | 62 |
| Train (the rest) | 398 | 2025-08-10 | 2025-12-12 | 90 | 124 | 71 | 6 | 107 |

The planned test mix was BUG 80 · ENHANCEMENT 70 · DOCUMENTATION 50 · QUESTION 50 · INVALID 50. BUG, ENHANCEMENT and INVALID are available, DOCUMENTATION is close, QUESTION is not (finding 2). A train pool of 398 issues is small for few-shot examples and the kNN baseline; a longer history fixes that too.

## 9. Duplicates

```sql
WITH duplicates AS (
    SELECT * FROM labelled WHERE 'status: duplicate' = ANY (labels) OR state_reason = 'duplicate'
), refs AS (
    SELECT d.id AS issue_id, d.number, d.created_at,
           coalesce((regexp_match(c.body, 'duplicate (?:of|by) #([0-9]+)', 'i'))[1],
                    (regexp_match(c.body, 'github\.com/spring-projects/spring-boot/issues/([0-9]+)'))[1])::int AS referenced
    FROM duplicates d JOIN issue_comment c ON c.issue_id = d.id
)
SELECT (SELECT count(*) FROM duplicates) AS duplicates,
       (SELECT count(*) FROM duplicates d WHERE EXISTS (SELECT 1 FROM issue_comment c WHERE c.issue_id = d.id)) AS with_comments,
       (SELECT count(*) FROM issue_comment c JOIN duplicates d ON d.id = c.issue_id
        WHERE c.author_association IN ('MEMBER', 'OWNER', 'COLLABORATOR')) AS maintainer_comments,
       (SELECT count(DISTINCT issue_id) FROM refs WHERE referenced IS NOT NULL) AS with_rule1_reference,
       (SELECT count(DISTINCT r.issue_id) FROM refs r JOIN issue o ON o.number = r.referenced
        WHERE o.repo = 'spring-projects/spring-boot' AND o.created_at < r.created_at) AS original_in_db_and_earlier;

SELECT author_association, count(*) AS comments
FROM issue_comment c JOIN labelled d ON d.id = c.issue_id
WHERE 'status: duplicate' = ANY (d.labels) OR d.state_reason = 'duplicate'
GROUP BY 1 ORDER BY 2 DESC;
```

| Duplicates | With comments | Maintainer comments | Name an original (rule 1) | Original in DB and older |
|---:|---:|---:|---:|---:|
| 140 | 101 | 147 | 43 | 20 |

| Comment author | Comments |
|---|---:|
| MEMBER | 146 |
| NONE | 66 |
| CONTRIBUTOR | 46 |
| COLLABORATOR | 1 |

```sql
WITH refs AS (
    SELECT DISTINCT d.id, d.created_at,
           coalesce((regexp_match(c.body, 'duplicate (?:of|by) #([0-9]+)', 'i'))[1],
                    (regexp_match(c.body, 'github\.com/spring-projects/spring-boot/issues/([0-9]+)'))[1])::int AS ref
    FROM issue d JOIN issue_comment c ON c.issue_id = d.id
    WHERE 'status: duplicate' = ANY (d.labels) OR d.state_reason = 'duplicate'
)
SELECT count(*) FILTER (WHERE ref IS NOT NULL) AS references_found,
       count(*) FILTER (WHERE ref < 46734) AS older_than_window,
       count(*) FILTER (WHERE ref >= 46734 AND NOT EXISTS (SELECT 1 FROM issue o WHERE o.number = ref)) AS not_an_imported_issue,
       count(*) FILTER (WHERE EXISTS (SELECT 1 FROM issue o WHERE o.number = ref AND o.created_at >= refs.created_at))
           AS newer_than_duplicate
FROM refs;
```

Rule 1 finds 46 references (some duplicates name more than one issue). Only 20 make valid pairs:

| Where the referenced issue is | References |
|---|---:|
| In our database and older than the duplicate (valid pair) | 20 |
| Older than our window (number below #46734) | 21 |
| In our database but newer than the duplicate | 4 |
| Inside the window but not an imported issue (a pull request, or deleted) | 1 |

Maintainers write more than half of the comments on duplicates, so rule 2 (their `#N` references) is worth adding. The binding limit is the window: most missing originals are simply older. Timeline cross-references would add candidates, but our token can't see them yet (ROADMAP P1.6).
