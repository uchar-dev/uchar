Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fact creation** (ch. 3.4)
whose intermediate step is joined through a bean field naming a role *no
declaration carries*.

    WF-034 FAIL /feitCreaties/0/tussenstappen/0/veld: 'bestaatniet' is not a
    declared role of 'kustligging': the hop's bean field resolves against no
    declaration

The clause is the one described in full in the sibling NOTE
(`wf034-feitcreatie-onbekend-zetveld`): each of the creation's v0 bean fields
spells a declared role of the relation its symbolic sister names. This is the
per-hop one — its scope is the fact type of that step's own `rolBinding`, here
`kustligging`, whose roles are `kustvak` and `post`; the untouched golden
spells it `kustvak`.

The step's companion flag `viaOuderVeld` decides which of the two beans the
field is read ON (the parent's forward reference or the child's back
reference), and it does not change the question this arm asks: either way the
name must be a role of that hop's relation. The arm is therefore indifferent
to the flag, and the corpus exercises both settings — `false` here and in
`anchor/jutterswet-erflijn`, `true` in `anchor/jutterswet-gemeenteberging`.

**Measured**, on this document against `corpus/golden/jutterswet-postwacht/case.json`:

    ref       : oracle: PASS (10 expectations)
    clingo    : clingo: PASS (10 expectations)
    bean model: ERROR ConstraintEvaluationException: Error evaluating
                constraint '' in  and in more rules

The same nameless engine death as the `joinVeld` sibling, and for the same
reason: the hop is compiled into a pattern constraint that reads the field
reflectively, so an unresolvable field takes the constraint down rather than
the consequence. (Drools' own exception, quoted as observed at
`drools.version` 8.44.0.Final.) The family's full measurement table (14 of 14 positions
verifier-clean, both OCaml executors clean on all 14) is in the sibling NOTE.

*Measured breadth:* zero outside the four `wf034-feitcreatie-onbekend-*`
witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
field repointed, a one-line diff; schema-valid, since `Tussenstap.veld` is
`["string","null"]` on the v0 wire.

Falsifiable, arm-exact: replace the `spells_role … (tat ^ "/veld") …` line of
`wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) by `[]` and
this case, alone in all four zones, turns clean — 503 documents compared, one
verdict moved, no report elsewhere.
