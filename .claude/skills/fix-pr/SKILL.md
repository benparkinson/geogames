---
name: fix-pr
description: Fix a pull request whose CI checks fail or that has merge conflicts, including a PR in a stack, and push the fix. Use for "the build/e2e/unit-frontend failed", "PR N has merge conflicts", "main broke after merging".
argument-hint: [PR number | branch | run URL]
---

# Fix a red or conflicting PR

With no argument, use the PR of the current branch (`gh pr view`). If `gh` isn't on PATH, use `"/c/Program Files/GitHub CLI/gh.exe"`. For a failure on `main`, take the run URL or `gh run list --branch main --limit 3`.

## 1. Get onto the branch

Check whether a worktree already has the branch (`git worktree list`) and work there; otherwise `git fetch -q origin` and `git worktree add .claude/worktrees/<branch> <branch>`. Never fix on `main`: for a failure on `main`, cut a new branch from `origin/main`.

For the frontend checks in a fresh worktree, put the main checkout's Node on PATH (`export PATH="<main>/target/node:$PATH"`, main = `git worktree list | head -1`).

## 2. Failing checks

```
gh pr checks <pr>
gh run view <run-id> --log-failed | tail -150
```

Read only the failed job's log, and grep it rather than reading it whole. `test` is the aggregate and only says another job failed. Reproduce locally with the matching script:

| CI job | Local |
|---|---|
| `lint-backend` | `bash scripts/check-java.sh` (runs `spotless:apply`) |
| `lint-frontend` | `bash scripts/check-frontend.sh`, plus `check-versions.sh` and `check-no-skipped-tests.sh` |
| `unit-backend` | `bash scripts/check-java.sh` (JaCoCo gate included) |
| `unit-frontend` | `bash scripts/check-frontend.sh` (the coverage thresholds need `npx vitest run --coverage` in `client/`) |
| `integration`, `build` | `bash scripts/check-it.sh -Dit.test=<Class>` |
| `contract-frontend` | regenerate `api-shapes.json` as in the root CLAUDE.md, then fix the client fixtures |
| `data` | `bash scripts/check-processed-data.sh` |
| `e2e` | see `e2e/CLAUDE.md`; read the failing spec and the Playwright error first, it's usually enough |
| `docker` | read the log: usually an `ERROR` line logged at startup or a migration that fails on Postgres |

- **Coverage below a threshold**: add tests for the uncovered branches of the change. Lower the threshold only when the user says so.
- **e2e failing once, with no link to the change**: rerun it once (`gh run rerun <run-id> --failed`). If it fails again, it's real: fix it. Never add `retries`, `.skip` or `.fixme`.
- Fix the cause, not the test, unless the test asserts what the change meant to change (a renamed label, a new layout).

## 3. Merge conflicts

```
git fetch -q origin
git merge origin/<base>      # or rebase, if the branch has never been reviewed
```

- Regenerate generated files rather than merging them by hand: `client/package-lock.json` (`npm install` in `client/`), `api-shapes.json` (the contract update), anything under `processed/` (its loader).
- In a CLAUDE.md conflict, keep what `main` now says and add this branch's change on top of it; don't bring back lines `main` removed.
- In a Flyway conflict (two migrations with the same version), renumber this branch's migration to the next free version. Never edit a migration that is already on `main`.
- Then run the checks for the areas in conflict.

## 4. Stacks

When the PR's base is another feature branch, fix the lowest broken PR first, then bring each PR above it up to date in order (`git merge origin/<branch below>`), pushing each one. If a lower PR has merged and was deleted, retarget the next one with `gh pr edit <pr> --base main`.

## 5. Push and report

Commit with a message that names the failure fixed. Push without force unless you rebased; after a rebase use `--force-with-lease`. Report what broke, why, and what you changed, in a few lines.
