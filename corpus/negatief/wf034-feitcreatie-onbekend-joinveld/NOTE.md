Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fact creation** (ch. 3.4)
whose `joinVeld` — the bean field the final hop of the creation path is READ
through — names a role *no declaration carries*.

    WF-034 FAIL /feitCreaties/0/joinVeld: 'bestaatniet' is not a declared role
    of 'seinvoering': the final hop's bean field resolves against no
    declaration

The clause is the one described in full in the sibling NOTE
(`wf034-feitcreatie-onbekend-zetveld`): each of the creation's v0 bean fields
spells a declared role of the relation its symbolic sister names. `joinVeld`
is the **final hop's** field, so its scope is the fact type named by
`joinRol` — here `seinvoering`, whose roles are `post` and `sein`. The
untouched golden spells it `post`, the *opposite* role of the binding; the
self-typed `anchor/jutterswet-erflijn` spells the same field with the *bound*
role. Both are legitimate, which is why the arm crosses against both roles and
asks no more.

Where the binding itself does not resolve, this arm stays silent: an
undeclared `joinRol.feittype`, or a binding naming no declared role, is
WF-028's report and is already loud. This arm is only about the bean field
sitting beside it.

**Measured**, on this document against `corpus/golden/jutterswet-postwacht/case.json`:

    ref       : oracle: PASS (10 expectations)
    clingo    : clingo: PASS (10 expectations)
    bean model: ERROR ConstraintEvaluationException: Error evaluating
                constraint '' in  and in more rules

The two OCaml executors replay it to the pinned expectations — they never read
the field — while the third dies inside the engine on a constraint it cannot
even name. That is the second of the four crash shapes this family carries;
the family's full measurement table (14 of 14 positions verifier-clean, both
OCaml executors clean on all 14) is in the sibling NOTE.

*Measured breadth:* zero outside the four `wf034-feitcreatie-onbekend-*`
witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
field repointed, a one-line diff; schema-valid, since
`ResolvedFeitCreatie.joinVeld` is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: replace the `spells_role … (at ^ "/joinVeld") …` line
of `wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) by `[]` and
this case, alone in all four zones, turns clean — 500 documents compared, one
verdict moved, no report elsewhere.
