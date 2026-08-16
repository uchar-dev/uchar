# Verifier v0: typed OCaml loader + WF checks on the wire format

Goal: `spec/verifier/` as an OCaml dune project in two parts: (1) a typed
AST of the `regelspraak-ir-v0` wire format plus a strict yojson loader —
built as a reusable library, so the Phase-2 reference interpreter in
`ref/` links the same loader instead of reinventing it; (2) a verifier
executable that runs every WF rule checkable on v0 dumps (per the WF
index in `spec/IR-SPEC.md`), reporting per rule pass/fail with the
offending JSON path. Exhaustive `match` over all node kinds — adding a
node without deciding its checks must not compile. For every implemented
rule: one failing case in `corpus/negatief/` (a minimal hand-broken
variant of the golden dump, with a note stating the rule it violates).
Gate 3 (`scripts/check-verifier.sh`) builds the project, requires every
`corpus/golden/**/ir.json` verifier-clean and every
`corpus/negatief/**/ir.json` rejected with the declared rule; wired into
CI. The WF index cells move from OPEN to their artifacts.

Acceptance: `scripts/check-verifier.sh` green, shown red first on a
deliberately broken case; gates 1–2 stay green; every implemented WF
check has a failing negatief case and a WF-index `Verifier check` cell
matching the implementation; per-rule golden witnesses accrue with the
corpus and stay tracked OPEN in the index (amended at delivery: the
original wording promised the golden half too, which needs corpus
content this card does not produce).

May touch: `spec/verifier/` (protected — merge mandate),
`corpus/negatief/`, `scripts/` (protected), `.github/` (protected),
`spec/IR-SPEC.md` (protected; WF index + conventions cells only),
`CLAUDE.md` (protected; the gate list grows by gate 3 — constitutional
obligation, added to this contract at delivery), `backlog/`.

Out of scope: the `ref/` interpreter itself; WF rules needing typing or
cross-layer data absent from v0 dumps (their index cells stay OPEN with
the reason); schema changes.

Result: spec/verifier/ landed as a dune project: typed AST + strict
loader of the full regelspraak-ir-v0 wire format (reusable by ref/),
and checks for 38 WF rules; gate 3 wired in scripts/ and CI; 38
negatief cases; the WF index rewritten to match the implementation
(WF-008/010/019/023/028/044 OPEN with reasons). Verified by three
adversarial reviewers (1 blocker — unitless-literal unit adoption —
plus 7 minors, all fixed; two loader narrowings documented as
deliberate). Deliberately left: per-rule golden witnesses (accrue with
the corpus), the OPEN rules above, and deep-nesting path-building
performance (documented).
