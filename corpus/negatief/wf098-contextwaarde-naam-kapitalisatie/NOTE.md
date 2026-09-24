Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1): a **context value** spelled
`EvaluationDate` where the closed set holds `evaluationDate`. One capital, one
character — the name is admissible up to case and inadmissible on the wire.

    WF-098 FAIL /rules/rules/0/versions/0/value/date/name: context value
    'EvaluationDate' is not one of the two admissible names evaluationDate,
    evaluationYear: the context-value name is a closed set, spelled byte for
    byte

**This witness carries the byte-exactness decision** (maintainer decision
2026-09-10). Its sister
(`wf098-onbekende-contextwaarde-naam`) would be refused under either
comparison; this one is refused only under the byte-exact one, so the two
witnesses fall independently and a case-insensitive relapse is measurable as a
one-document move.

**The measurement that decides byte over case-insensitive**, taken 2026-09-10 on
`corpus/golden/anchor/jutterswet-penningvintage` — the anchor golden this
document is a hand-broken copy of — with the one-character change of that day
(on the v0 wire, `Rekendatum` → `rekendatum`), against the state that predates
this card:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 2 expectations | **PASS, 2 expectations** |
| ASP backend (`case.json`) | PASS, 2 expectations | **PASS, 2 expectations** |

Three consumers accepted the recapitalised name and answered identically,
because on that day `ref` (`ref/lib/eval.ml`) and the ASP backend
(`backends/clingo/lib/translate.ml`) both compared `norm n = norm "Rekendatum"`.
The Drools backend did not: its constant reader was
`"Rekendatum".equals(c.naam())` (`DrlTranspiler.rekenConstante`, and
`Expr.isRekenConstante` on the exec-model branch), byte-exact, so it would not
have recognised the name and would have degraded the operand rather than
diagnosed it. The two readers of the SAME field disagreed, and the disagreement
was case — a fourth reader, the day-kind one (`kern/Dagsoorten`), used
`equalsIgnoreCase` on `de dag`, so the split was inside one backend as well as
across them.

**Why the divergence gets a wire rule and not a differential witness.** The
acceptance criterion this card started from asked for a golden that Drools
actually plays carrying a recapitalised session-value name. There can be no such
document, and the reason is structural rather than a gap to be filled: of the
three `evaluationDate` carriers the golden and red zones hold, the anchor golden
is excluded by the Drools loader for an unrelated reason (`Expr:TemporalRead`)
and the other two are red-corpus documents, which are required to fail.
Admitting a purpose-built player would mean shipping a document whose whole
content is a spelling three consumers read one way and the fourth another —
which is the defect, not a test of it. Closing the wire is the other route: with
the name byte-exact at verification time, a document carrying `EvaluationDate`
never reaches an executor at all, so the normalisation inside `ref` and the ASP
backend becomes unreachable on any verifier-clean document and the Drools
readers need not move. That is the route taken, and it is why `ref`, the ASP
backend and the Drools backend are untouched by this card. It left one honest
residue: a HAND-BUILT document that skipped the verifier still reached the
executors with a recapitalised name and still split them — the wire rule bounds
conforming documents, not every input a runner can be handed. Since the L4 fix
round that residue is closed as well: every executor refuses the name at load
with the verifier's own sentence.

**Cost: none** (measured 2026-09-10 on the v0 wire). Over the four zones plus
the parity zone — 578 documents before these two witnesses, 580 after — the
wire carried **14** `Constante` nodes over
**6** documents in exactly **two** spellings, `de dag` (11) and `Rekendatum` (3),
both admissible verbatim. Not one document predating the rule moves.

A hand-broken variant of
`corpus/golden/anchor/jutterswet-penningvintage/ir.json` — one character
recapitalised, a one-line diff (`evaluationDate` → `EvaluationDate`);
schema-valid, since the schema leaves a context value's `name` a free string
for this rule to close.

Falsifiable, arm-exact: replace the `| Constante { naam; _ } -> (…)` branch of
`walk_expr` in `spec/verifier/lib/checks.ml` with `| Constante _ -> []` and this
case turns clean together with its one sister — 580 documents compared, exactly
two verdicts moved, not one report line elsewhere. Relax the comparison from
`List.mem n [ … ]` to a `norm` one and this case turns clean ALONE, its sister
staying refused: the ci relapse is a one-document move.

Since L4 the node is a `ContextValue` (delta B23) and its name is written in
`name`: the bound day of a day-kind definition is its own node, `BoundDay`,
and a context value named `de dag` falls outside the set like any other name
(witness `wf098-de-dag-als-contextwaarde`). A context value without a name
cannot be written since L4. Since L5 the closed set holds the two names
`evaluationDate` and `evaluationYear` (delta B6); the v0 spelling `Rekendatum`
falls outside it (witness `wf098-v0-naam-rekendatum`), and this witness,
which until L5 carried `rekendatum`, recapitalises the L5 name instead, so it
keeps pinning that the set is compared byte for byte. `expect.txt` pins the
arm.
