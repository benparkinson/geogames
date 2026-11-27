---
name: write-plan
description: Write or refresh an implementation plan in docs/plans that a fresh Sonnet session implements with /ship-plan. Use for "write a plan (for Sonnet) to ...", "the codebase moved on, update the plan".
argument-hint: <feature description | plan name --refresh>
---

# Write a plan for another session

The plan is read cold by a cheaper model in a new session, so it must carry every decision, and nothing that the CLAUDE.md files already say.

## Before writing

- Read the code the feature touches: enough to name the files and functions, no more.
- If a design question is genuinely the user's to decide (a product choice, a trade-off with no default), ask it first, with a recommendation. Decide everything else yourself and write it down as a decision.
- Go through this checklist and put each item that applies into the plan:
  - A JPA entity or table changes: a new Flyway migration (never edit an applied one), and the manual Postgres test, which needs a Neon dev branch from the user.
  - An API response changes: the new field starts optional; `api-shapes.json` regenerated and the client fixture updated in the same PR.
  - Scoring changes: `model/Scoring` and `client/src/games/common/scoring.ts` and their shared table.
  - What is stored or who receives requests changes: the `/privacy` page.
  - New data or a new cache: heap measured with `scripts/measure-heap.sh`; nothing cached per round, pair or game.
  - Usage statistics (`stat_*` tables): their own PR and migration.
  - Metrics (`GameMetrics`, a tag, a timer): the panels to add or change in `grafana/geoggernaut.dashboard.json` (PromQL names: `.` → `_`, counters end in `_total`), and a note that the owner re-imports it after deploy.
  - UI: theme tokens and `client/src/ui/` controls only, mobile with the keyboard up, light and dark.

## The plan

Save it to `docs/plans/<kebab-name>.md` in the main checkout (`git worktree list | head -1`). It is gitignored: never commit it. Keep it under about 8 KB:

```
# <Feature>

<Two or three sentences: what the user sees and why.>

## Decisions
- <Each choice made, with the one-line reason when it isn't obvious.>

## PR 1: <name> (branch `<branch>`, base `main`)
- Files: <path>: <what changes>
- Tests: <the tests to add or change, and what each proves>
- Check: <the scripts/check-*.sh to run>

## PR 2: <name> (branch `<branch>`, base `<PR 1 branch>`)
...

## Merge order
PR 1, then PR 2 (...). <Say whether each can deploy alone.>

## Out of scope
- <What not to do, so the implementer doesn't wander.>
```

Split PRs as the root CLAUDE.md says: the backend and contract first, separate backend concerns (such as stats) next, the frontend last. Each PR passes CI and leaves the product working. A small change is one PR.

## Refreshing a plan (`--refresh`)

When main has moved on: `git fetch`, read the commits since the plan's date that touch its files (`git log --since=<date> --stat origin/main -- <paths>`), and correct the file names, steps and PR bases that no longer match. Mark the PRs that have already merged as done. Don't rewrite what is still right.

## Then

Report the plan's path, the PR list in one line each, and the command that starts the work in a fresh session: `/ship-plan <plan-name>`.
