# CI bootstrap (proposal)

Goal: A GitHub Actions workflow that runs `scripts/check-self-contained.sh`
on every push and pull request — the first mechanized gate run, replacing
run-before-commit discipline with enforcement. CODEOWNERS already exists;
branch protection waits on the visibility decision (`spec/PLAN.md` §9,
decision 10: free-plan branch protection requires a public repo).

Acceptance: the workflow file is valid (actionlint or `act` dry-run, output
shown); it fails on a deliberately planted violation and passes on a clean
tree (red-then-green shown locally).

Depends on: a remote (none exists yet); the visibility decision for branch
protection.

May touch: `.github/` (protected path — proposal; the maintainer merges),
`backlog/`

Out of scope: the schema/round-trip gate (own card); the acceptance job
pulling the held-out split (needs the private held-out repo to exist first).
