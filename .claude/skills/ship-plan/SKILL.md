---
name: ship-plan
description: Implement a plan from docs/plans or a small task or bug fix on its own git worktree, run the matching checks, and raise a PR. Use for "read and implement <plan>", "on a worktree ... raise a PR", "investigate and fix this bug".
argument-hint: <plan name | task | bug report> [--base <branch>]
---

# Ship one change as one PR

The argument is a plan name (`daily-variety`, `daily-variety.md`, `@docs/plans/x.md`, a scratchpad path), a plan and a phase (`easy-mode phase 2`), or a description of a task or bug.

## 1. Find the plan

`docs/plans/` is gitignored, so **plans exist only in the main checkout**, never in a new worktree. Find the main checkout with `git worktree list | head -1` and read the plan there. When a scratchpad path is given, read it as it is. If no plan matches the name, list `docs/plans/` and ask.

Read the whole plan once. When it has several phases or PRs and no phase was named, do the first one not yet merged (`gh pr list --state all --search <branch>` tells you).

## 2. Make the worktree

Always work in a new worktree, as other agents edit the main checkout at the same time:

```
git fetch -q origin
git worktree add .claude/worktrees/<branch> -b <branch> origin/<base>
```

`<base>` is `main` unless `--base` was given or the plan stacks this PR on an earlier one: then it is that earlier PR's branch. Use a short kebab-case branch name, the plan's if it gives one. Then work only inside that worktree.

A fresh worktree has no `target/node` and no `client/node_modules`. Before the frontend checks, put the main checkout's Node first on `PATH` (`export PATH="<main>/target/node:$PATH"`); `check-frontend.sh` runs `npm ci` itself when `node_modules` is missing.

## 3. Implement

- Follow the plan's decisions. If the code has moved on and a step no longer fits, take the smallest change that keeps the plan's intent, and say so in the PR.
- **Bug fixes start with a failing test** that reproduces the report, then the fix that makes it pass.
- Follow the CLAUDE.md rules for the area: a Flyway migration for any entity change, `api-shapes.json` and the client fixture for an API change, the `/privacy` page when what is stored changes, theme tokens and never raw colours.

## 4. Check

Run only what the change touches, from the worktree:

| Touched | Run |
|---|---|
| Java | `bash scripts/check-java.sh` |
| `client/` | `bash scripts/check-frontend.sh` |
| API, persistence, migrations, `HttpServerIT` | `bash scripts/check-it.sh` (add `-Dit.test=<Class>` and `-Dskip.frontend` when you can) |
| loaders, `processed/` | `bash scripts/check-processed-data.sh` |
| versions, CI, test markers | `check-versions.sh`, `check-no-skipped-tests.sh` |

Run e2e locally only when you changed something a browser test drives and CI can't tell you sooner. Read a log in `target/check-logs/` only when the summary isn't enough. Fix until they pass. Lower a coverage threshold only when the user says so; add tests instead.

## 5. Commit, push, PR

- Never commit a plan file or anything under `docs/plans/`. Don't link to plans from code, CLAUDE.md or the PR: they aren't in the repo.
- **A plan with more than one PR:** end the PR title with the plan's file name (no `.md`) and `X/Y`, so the stack can be put in order from the PR list: `League table UI (challenge-replies 3/4)`. X is this PR's number in the plan and Y the plan's total, merged PRs included. Start the description with the same, e.g. `Plan challenge-replies, PR 3/4.` (the name only, no path or link). A one-PR plan or a task gets neither.
- Commit with a message that says what changed and why. Push with `git push -u origin <branch>`.
- `gh pr create --draft --base <base>`: always a draft, and leave it one (CI runs nothing until it is ready; `/fix-pr` does that when its turn to merge comes). The local checks above are the only ones it has had. If `gh` isn't on PATH, use `"/c/Program Files/GitHub CLI/gh.exe"`. The description: what changed, how it was checked, anything left out or done differently from the plan, and for a stack, the merge order and which PR this one builds on.
- Wait for nothing.

## 6. Stop

Report the PR link and, when the plan has more phases, the command for the next one (`/ship-plan <plan> phase N`) in a fresh session (`/clear`). Don't start the next PR unless asked.
