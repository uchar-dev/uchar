Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1): a **context value** spelled
`rekendatum` where the closed set holds `Rekendatum`. One capital, one
character — the name is admissible up to case and inadmissible on the wire.

    WF-098 FAIL /resolvedRegels/0/waarde/datum/naam: context value
    'rekendatum' is not one of the three admissible names Rekendatum,
    Rekenjaar, de dag: the context-value name is a closed set, spelled byte
    for byte

**This witness carries the byte-exactness decision** (maintainer decision
2026-09-10). Its sister
(`wf098-onbekende-contextwaarde-naam`) would be refused under either
comparison; this one is refused only under the byte-exact one, so the two
witnesses fall independently and a case-insensitive relapse is measurable as a
one-document move.

**The measurement that decides byte over case-insensitive**, taken 2026-09-10 on
`corpus/golden/anchor/jutterswet-penningvintage` — the anchor golden this
document is a hand-broken copy of — with the identical one-character change,
against the state that predates this card:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 2 expectations | **PASS, 2 expectations** |
| ASP backend (`case.json`) | PASS, 2 expectations | **PASS, 2 expectations** |

Three consumers accept the recapitalised name and answer identically, because
`ref` (`ref/lib/eval.ml`) and the ASP backend (`backends/clingo/lib/translate.ml`)
both compare `norm n = norm "Rekendatum"`. The Drools backend does not: its
constant reader is `"Rekendatum".equals(c.naam())` (`DrlTranspiler.rekenConstante`,
and `Expr.isRekenConstante` on the exec-model branch), byte-exact, so it would
not recognise this name and would degrade the operand rather than diagnose it.
The two readers of the SAME field disagree, and the disagreement is
case — a fourth reader, the day-kind one (`kern/Dagsoorten`), uses
`equalsIgnoreCase` on `de dag`, so the split is inside one backend as well as
across them.

**Why the divergence gets a wire rule and not a differential witness.** The
acceptance criterion this card started from asked for a golden that Drools
actually plays carrying a recapitalised `Rekendatum`. There can be no such
document, and the reason is structural rather than a gap to be filled: of the
three `Rekendatum` carriers the corpus holds, the anchor golden is excluded by
the Drools loader for an unrelated reason (`Expr:TemporalRead`) and the other
two are red-corpus documents, which are required to fail. Admitting a
purpose-built player would mean shipping a document whose whole content is a
spelling three consumers read one way and the fourth another — which is the
defect, not a test of it. Closing the wire is the other route: with the name
byte-exact at verification time, a document carrying `rekendatum` never reaches
an executor at all, so the normalisation inside `ref` and the ASP backend
becomes unreachable on any verifier-clean document and the Drools readers need
not move. That is the route taken, and it is why `ref`, the ASP backend and the
Drools backend are untouched by this card. It leaves one honest residue: a
HAND-BUILT document that skips the verifier still reaches the executors with a
recapitalised name and still splits them — the wire rule bounds conforming
documents, not every input a runner can be handed.

**Cost: none.** Over the four zones plus the parity zone — 578 documents before
these two witnesses, 580 after — the wire carries **14** `Constante` nodes over
**6** documents in exactly **two** spellings, `de dag` (11) and `Rekendatum` (3),
both admissible verbatim. Not one document predating the rule moves.

A hand-broken variant of `corpus/golden/anchor/jutterswet-penningvintage/ir.json`
— one character recapitalised, a one-line diff; schema-valid, since `naam` is
typed `["string", "null"]` on the v0 wire.

Falsifiable, arm-exact: replace the `| Constante { naam; _ } -> (…)` branch of
`walk_expr` in `spec/verifier/lib/checks.ml` with `| Constante _ -> []` and this
case turns clean together with its one sister — 580 documents compared, exactly
two verdicts moved, not one report line elsewhere. Relax the comparison from
`List.mem n [ … ]` to a `norm` one and this case turns clean ALONE, its sister
staying refused: the ci relapse is a one-document move.
