Violates WF-037 (`spec/IR-SPEC.md` ch. 4.6; the `BuiltinCall` table of
ch. 4.3): a `granularity` facet on a builtin whose kind does not bucket — the
`yearOf` call of a rule value carrying `"granularity": "year"`.

    WF-037 FAIL /rules/rules/0/versions/0/value: granularity facet on
    a non-bucketing kind

**Why the facet names nothing here.** A bucket is the period into which
`timeProportionalPart` and `dayCount` divide a timeline, and those two
kinds are the only ones the facet is admitted on (ch. 4.3). `yearOf` reads
the year of one date and divides nothing, so the facet states a bucket that no
evaluation of this node uses. Since L4 `granularity` is a field of
`BuiltinCall` alone (delta B21), so the arm is reached on the builtin kinds
only. Its witness until L4, `wf037-aantalgevulde-granulariteit`, carried the
facet on an aggregation, which cannot be written since, and was removed with
reason (`corpus/negatief/README.md`, L4).

**Every executor refuses it in the verifier's words.** Measured 2026-09-24,
after the L4 fix round, against the five cases of the source golden: the
reference interpreter faults on every case (`EVALUATION FAULT: granularity
facet on a non-bucketing kind`), the ASP backend refuses the translation with
the same sentence, and the Drools loader refuses the document on the node with
the same sentence, on both transpiler branches. Until that fix round the three
disagreed: the reference interpreter faulted with a sentence of its own
(`granularity-bucketed aggregation not implemented in this card`), while the
ASP backend passed all five and the Drools backend played all five, both
silently ignoring the facet.

**The document.** `corpus/golden/jutterswet-paasvondst/ir.json` with one field
added to the value of its first rule: `"granularity": "year"` on the
`yearOf` call, after `arguments`.

Falsifiable, arm-exact: emptying the arm `Some _, _` of the granularity match
in `walk_expr` (`spec/verifier/lib/checks.ml`) turns this case clean and
nothing else — 703 documents compared over golden, rood and negatief, one
verdict moved.
