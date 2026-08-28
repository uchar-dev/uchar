# Ongelijk-met-één-leeg wordt waar, uniform over alle typen (Tabel 44)

Goal: maintainer decision 2026-08-28, after source verification against
the official RegelSpraak spec (typeringen §5.6, Tabel 43/44: inequality
with exactly one empty operand is TRUE, for every datatype; both empty
is FALSE) and the triage proof that production Drools follows it. uchar
today deviates for text/enum/boolean only (any empty → false) — an
undocumented, internally inconsistent cell (date inequality already
follows the numeric one-empty-true rule). Close it: **inequality with
exactly one empty operand is WAAR, uniform over every type** — numeric,
percentage, text, enum, boolean, date, and the ch. 11 constructor/sum
values (whose "deliberately not complements over leeg" sentence falls
for the ≠ half; equality cells stay exactly as they are, including the
non-numeric any-empty-equals-false mirror of v0's guard).

Deliver:
1. `spec/RUNTIME-SEMANTICS.md` ch. 1.2d: the inequality row becomes
   universal (exactly-one-empty → waar; both-empty → onwaar); the
   "deliberate asymmetry" note now covers equality only; ch. 11's
   constructor-value comparison cell updated for ≠; worked examples
   updated and their WV tests verbatim in ref (existing assertions may
   move ONLY where they pin the old cell — list each one).
2. `ref/`: the non-numeric inequality branch (and the constructor-value
   ≠) yields waar on exactly one empty operand.
3. `backends/clingo/`: the same cell in the symbolic equality encoding.
4. Corpus re-pin, HAND-COMPUTED (this is the rare legitimate
   expectation change — the spec itself moves; every touched case gets
   a fresh hand calculation in the card Result): find EVERY case
   pinning the old cell (at least jutterswet-borgvlag,
   jutterswet-vondstetiket, jutterswet-keurstaat leeg-corner cases;
   search the whole corpus including catala goldens and rood) and
   re-pin to the Tabel-44 outcome. Gate 2/9 byte-identity for ir.json
   files must hold (only case*.json expectations move).
5. Gate 12: borgvlag and vondstetiket stop diverging — their
   value-divergence entries MUST be removed (the staleness check
   enforces it) and the played count rises by 2. Show the square
   closing on them: same case, same answer in ref, clingo AND Drools.
6. Red-first: quote the pre-change ref/clingo outputs on a
   one-empty-inequality case, then the post-change agreement.

Acceptance:
- All twelve gates green; gate 12 played 43 / value-divergence 5.
- The re-pinned expectations each carry a hand computation in the
  Result; no expectation changed except those pinning the old cell.
- ch. 1.2d reads uniformly; the WV tests cover one-empty and
  both-empty for text, enum, boolean, date AND a constructor value.

May touch: `spec/RUNTIME-SEMANTICS.md`, `ref/`, `backends/clingo/`,
`corpus/**/case*.json` (old-cell pins only), `scripts/check-drools.sh`
(the two entries), `ref/test/test_ref.ml` (WV additions; existing
assertions only where they pin the old cell — enumerate them).
Out of scope: the equality cells (they stay), `spec/IR-SPEC.md` (no WF
change — this is runtime semantics), `spec/schema/`,
`spec/CONSTRUCTS.md` (serial at merge), any toolchain commit.
