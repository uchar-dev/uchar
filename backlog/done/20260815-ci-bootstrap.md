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

Result: `.github/workflows/gates.yml` added: one job running
`scripts/check-self-contained.sh` on every push and pull request — the
first mechanized gate run. Red-then-green shown locally with real output:
a planted machine-local path in artifact zone `ref/` makes the gate exit 1
with the zone named; removing it restores PASS/exit 0. Deviation from the
acceptance wording: `actionlint`/`act` are not installed on this machine,
so the workflow file was validated as YAML with the expected job shape
instead; the authoritative validation is the first Actions run after this
proposal merges. Still waiting on the visibility decision (PLAN §9,
decision 10): branch protection — and with it mechanized CODEOWNERS
enforcement — needs a public repo or a paid plan; until then the gate runs
but protected paths remain convention. Deliberately left: the
schema/round-trip gate (own card, blocked on the emitter) and the held-out
acceptance job (needs the private held-out repo). This branch is a
PROPOSAL: `.github/` is a protected path — the maintainer merges
(fast-forward).

Addendum at merge (maintainer review): the branch was restacked onto the
schema merge and gates.yml gained the gate-2 job (`check-ir-schema.sh`,
with a jsonschema install step on the runner), so the first Actions run
executes both gates — the review's correction that CI must not lag the
gate set.
