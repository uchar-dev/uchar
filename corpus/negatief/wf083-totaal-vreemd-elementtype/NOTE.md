# wf083-totaal-vreemd-elementtype

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), clause (c), at the **element
type of the subject's own read**. A `total` without a range (no `sourceRole`,
no `sourcePath`) is the subject's own read, so the type it folds over is the
subject type, and its `elementType` must name that type (equality in the
WF-001 form). Here it names another declared type.

    WF-083 FAIL /rules/rules/2/versions/0/value/elementType: the
    elementType 'Vondst' of a total without a range is not the fold subject
    'Jutter': without a range the fold is the subject's own read, so its
    elementType must name the subject type

The document is `corpus/golden/jutterswet-tempototaal` with one change: the
third rule, "het bewaargeld van een Jutter is het totaal van zijn
bewaarvergoeding per maand", carries `elementType: "Vondst"` instead of
`"Jutter"`. Its one argument still reads `bewaarvergoeding per maand` on
`Jutter`, the subject, so the source's own anchor (the other half of clause
(c), `wf083-vouwbron-vreemd-objecttype`) is satisfied, and every other rule of
the document is the golden's.

Why it has to exist. Until the L2 fix round no executor read the field at
this position: the temporal total reads the subject's own timeline. So the
field could name any declared type and nothing checked it. Since the fix
round the executors check it too, with the verifier's sentence. Measured in the L2 fix round
(2026-09-24) on this document with the golden's `case.json`: the verifier
of the L2 union (`bd2c48de`) answered `verifier: PASS`, and `ref` and the ASP
backend both answered the golden's seven pinned expectations unchanged
(`oracle: PASS (7 expectations)`, `clingo: PASS (7 expectations)`). The arm
added in that round refuses it; before it, the document said something about
the fold that no reader checked.

Falsifiable, arm-exact: remove the `total` element-type arm from the
`Functie` case of `walk_expr` in `spec/verifier/lib/checks.ml` and this case
turns clean; no other report over golden, rood, negatief and the parity zone
changes (measured: the arm fires on this document alone).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
