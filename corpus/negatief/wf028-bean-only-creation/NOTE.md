Violates WF-028 (`spec/IR-SPEC.md` ch. 3.4): a **fact creation** carrying only
the v0 bean fields — every symbolic key removed, so nothing on the wire says
which relation is created.

    WF-028 FAIL /feitCreaties/0: fact creation carries only bean fields; the
    symbolic form (ch. 3.4) is what executes, and nothing on the wire says
    which relation is created

**What was removed.** All five extension keys of the untouched golden's single
creation: `feittype`, `ankerRol`, `doelRol`, `joinRol`, and the one
`tussenstappen[0].rolBinding`. What remains is the record exactly as v0 spells
it: `anker`, `doel`, `zetVeld`, `joinVeld`, and a `Tussenstap` with `type`,
`veld` and `viaOuderVeld`.

**The gap this closes.** WF-028's all-or-nothing rule reads "once ANY symbolic
field is present…", and the clause opened with a matching escape: a
bean-field-only record was declared *uncheckable*, on the ground that its
relational content is not on the wire. That reasoning inverted the rule. The
content is not on the wire, and that **is** the defect — the same reading the
executors already had.

**Measured** on this document against `corpus/golden/jutterswet-postwacht/case.json`,
before the arm:

    verifier  : PASS
    ref       : EVALUATION FAULT: fact creation "vakbeseining door het
                kustvak" carries only bean fields; the symbolic form
                (IR-SPEC ch. 3.4) is what executes
    clingo    : TRANSLATION FAULT: the same sentence
    bean model: PLAYED 1

This is the one place in the fifth wave's four planes where the three
executors disagree about whether the document **exists** at all: two refuse to
run it, the third replays it to the pinned expectations, because it reads
precisely the fields that are left. A verifier that admits such a document
makes the IR's meaning depend on which engine reads it, which is the one thing
an engine-neutral IR may not do. The rule was already written down — ch. 3.4
says the symbolic form is what executes — it simply had no arm and no witness.

Note where this sits relative to its siblings. `wf028-half-symbolic-creation`
pins the *partial* form (some symbolic keys present, others missing) and
`wf028-create-along-final-hop` the *created relation equals the final hop*
prohibition; this is the third corner, the form with none of them. Together
the three make the all-or-nothing rule total: none, some, all.

*Measured breadth:* **zero** documents lose anything. All 8 fact creations in
golden, rood, negatief and the parity zone already carry the symbolic form —
`wf028-half-symbolic-creation`, the nearest shape, keeps its `feittype` and so
is still refused by the partial arm, for its own reason and with its own
message. The four-zone sweep (500 documents) moves no other verdict and no
other report.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — five
keys deleted, no other change. The shape is schema-valid: the symbolic keys
are v1 extension keys, optional on the v0 wire, which is exactly why the
verifier and not the schema is the fence.

Falsifiable, arm-exact: replace the `if not any_symbolic then [ … ]` violation
of `wf028` (`spec/verifier/lib/checks.ml`) by `[]` and this case, alone in all
four zones, turns clean — 500 documents compared, one verdict moved, no report
elsewhere.
