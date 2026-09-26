# WAY-OF-WORKING — how sessions operate in this repo

*Read first: `CLAUDE.md` (the constitution), then the card you were given.
`spec/PLAN.md` §8 carries the rationale behind this regime. This file is the
operating manual; if an ad-hoc instruction conflicts with it, say so before
proceeding.*

## 1. Pace and parallelism

There are no deadlines. Coherence beats throughput; when in doubt, do less,
serially.

- The default is **one session, one card, one branch**.
- Work runs in parallel **only** when it is one of:
  1. **Research or review** — read-only; the deliverable is a report or a
     new backlog card; it touches no files another session touches.
  2. **Truly disjoint work** — the cards' "may touch" sets share no files
     and no directories, including index/registry files both would append
     to.
- Never in parallel: spec conventions and metamodel, schema shape, emitter
  design, anything that edits the same document. A merge conflict means the
  cards were cut wrong — stop and re-cut; don't resolve-and-continue.

## 2. The backlog

`backlog/` is tracked and is the coordination ledger: `new/`, `doing/`,
`done/`. The maintainer is the scheduler: sessions are launched *with* a
card ("work `backlog/new/<card>.md`"); they do not self-select work.

Card format (`backlog/new/YYYYMMDD-<slug>.md`):

```markdown
# <title>
Goal: <one paragraph — the artifact this card produces>
Acceptance: <runnable commands + expected outcome; the gate(s) that must be green>
May touch: <explicit files/dirs — the disjointness contract>
Out of scope: <explicit>
```

Claim = `git mv backlog/new/X.md backlog/doing/X.md` in the card's first
commit. Done = the merge moves the card to `done/` and appends a short
`Result:` note (what landed, what was deliberately left).

## 3. Branches, worktrees, merging

- One card = one branch `card/<slug>` = one worktree
  (`git worktree add ../wt/<slug> -b card/<slug>` from the main checkout).
- Merge to `main` fast-forward only (`git merge --ff-only card/<slug>`;
  rebase the branch first if `main` moved), then push; never commit to
  `main` directly. There is one maintainer, so no PR requirement: the
  remote is backup and CI, not review infrastructure. History stays
  linear — no merge commits.
- Protected paths (`CLAUDE.md`): the card branch stays unmerged as a
  *proposal*; the maintainer reviews and merges it (also fast-forward), or
  instructs the session to.

## 4. Session lifecycle

1. Read `CLAUDE.md`, this file, and your card. If gates are red at start,
   report and stop — never build on red.
2. Work the card. When adding a gate: show it red, then green.
3. Report honestly: what passed (with real output), what is a marker or gap,
   what stayed out of scope. A green subset is not green.
4. End: fast-forward merge with the card moved to `done/`; for protected
   paths, leave the branch as the proposal. A card session does not update
   `spec/PLAN.md` or `spec/CONSTRUCTS.md` itself: proposed row or plan text
   goes in the `Result:` note, and the main session lands the registry
   serially at the card's landing (`CLAUDE.md`, Protected paths). Touch
   `CLAUDE.md` only if an invariant changed.

## 5. Source material: the private repositories

Two private repositories are legitimate **read-only** source material during
development:

- the **regelspraak-java** toolchain — the IR being mirrored
  (mirror-then-diverge, `spec/IR-SPEC.md` § Method; the founding review's
  citations are pinned to its commit `b85dff3`); archived 2026-09-26, its
  successor `uchar-dev/regelspraak-toolchain` consumes this repository as a
  submodule;
- the **calculemus course** — origin of the Jutterswet casus.

The maintainer supplies their local locations when launching a session.
During development, referring to these repos **by name** in working prose —
`spec/PLAN.md`, backlog cards, reviews, draft-status headers — is fine and
normal. The boundary is §6.

## 6. The self-containedness gate

Everything this repo ultimately publishes must carry its full meaning
without access to the maintainer's machine or private repositories.

- **Working prose** — `spec/PLAN.md`, `spec/IR-REVIEW.md`, `backlog/`,
  `CLAUDE.md`, this file, top-level READMEs — may reference the private
  repos by name during development.
- **Artifact zones** — `corpus/golden/`, `corpus/negatief/`,
  `corpus/rood/`, `spec/schema/`, `spec/verifier/`, `ref/`, `backends/`,
  `frontends/` — are self-contained **from their first commit**: no
  machine-local paths, no private-repo names, no file:line citations into
  private code. Code and data never carry provenance; git history and the
  plan do.
- **Graduation.** Placeholder READMEs inside still-empty zones may name the
  private toolchain; they are listed as explicit exceptions in
  `scripts/check-self-contained.sh`, and that list only shrinks — sweeping a
  README clean and dropping its exception is part of the zone's first real
  delivery. **Graduated (Phase-1 exit, 2026-08-19):** `spec/IR-SPEC.md` and
  `spec/RUNTIME-SEMANTICS.md` are in the gated set — the normative documents
  read stand-alone, mechanically enforced.
- **Rights tripwire.** Corpus data must contain no TOKA material
  (`corpus/README.md`); the same script trips on it.

Run `scripts/check-self-contained.sh` before every commit, and the full gate
set before every landing — locally (maintainer decision 2026-09-26: no
GitHub Actions; `.github/workflows/gates.yml` documents the order and runs
by hand only).

## 7. Definition of done

A card is done when: its acceptance commands pass with output shown; all
repo gates are green; the card sits in `done/` with a `Result:` note; and
nothing outside its "may touch" set changed. Anything less is reported as
not done — plainly, not hedged.
