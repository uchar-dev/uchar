Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (m)): a **fact creation** (ch. 3.4)
with an intermediate step whose `veld` is **absent** — the v0 bean field a
bean-model executor navigates, left null.

    WF-034 FAIL /rules/feitCreaties/0/tussenstappen/0/veld: the hop's bean field is
    absent: the v0 half of a fact creation is what a bean-model executor
    navigates and writes, and an absent field names nothing

**The empty half of the naming class.** The fifth wave crossed the four bean
fields whenever they carried a name, and said nothing when they carried none —
the same split the earlier waves kept meeting: a *filled* field that resolves
against nothing, and a field that is simply not there. Both switch the same
consumer off. This clause closes the empty half for all four fields at once,
as ONE list-driven arm over the (path, field, value) triples, and the reason
is falsifiability rather than economy: with one witness, four separate
sub-arms are not separately falsifiable — deleting any of the other three
moves **0** of the 503 four-zone documents, which is a clause with no
counterexample. One arm, one witness, one mutant that frees exactly this
document.

**All four measured, none silent.** Each of the four fields nulled one at a
time on `corpus/golden/jutterswet-postwacht` (baseline: `PLAYED 1`), with
`ref` and clingo replaying all four to their pinned expectations (they are
blind to the bean half by construction) and the verifier admitting all four:

    zetVeld               EXCLUDE transpiler SKIP: feitcreatie: vakbeseining
                          door het kustvak — feitcreatie (onopgelost)
    joinVeld              the same loud skip
    tussenstappen[].veld  ERROR ConstraintEvaluationException: Error
                          evaluating constraint '' in  and in more rules
    tussenstappen[].type  ERROR NullPointerException: Cannot read field
                          "naam" because "ot" is null

Two of the four crash the engine, with **exactly** the exceptions the sibling
NOTEs `wf034-feitcreatie-onbekend-tussenstapveld` and
`wf034-feitcreatie-onbekend-tussenstaptype` already pin for the *filled*
nonsense name — the absent field and the unresolvable one arrive at the same
place. (Both of those two are Drools-internal exception shapes and are quoted
as observed at `drools.version` 8.44.0.Final; the `IllegalStateException` the
other siblings quote comes from the ported runtime itself and is ours.) The
other two do not crash but drop the whole creation, loudly — which is the
third executor behaviour again: the document neither faults nor answers, while
`ref` and clingo answer it in full.

This witness takes the **crashing** half, `tussenstappen[].veld`, so the
document pins the sharpest of the four; the list-driven arm covers the other
three and the sweep below is what stands for them.

*Measured cost, four zones:* **zero**. Every one of the 36 bean name positions
and all 10 `tussenstappen[].type` positions in the 500 documents that predate
this fix round is present; nothing that was standing loses its verdict.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
value nulled, a one-line diff; schema-valid, since `Tussenstap.veld` is
`["string","null"]` on the v0 wire and the key stays required.

Falsifiable, arm-exact: make the presence arm of `wf034_feitcreatie_beanvelden`
(`spec/verifier/lib/checks.ml`) return `[]` and this case, alone in all four
zones, turns clean — the resolution arms above it take a `None` and say
nothing, which is exactly the silence this clause exists to end.
