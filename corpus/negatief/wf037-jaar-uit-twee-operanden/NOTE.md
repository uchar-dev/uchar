Violates WF-037 (`spec/IR-SPEC.md` ch. 4.6; the `BuiltinCall` table of
ch. 4.3): a builtin of a one-operand kind with two operands — `yearOf`
reading `zijn vondstdatum` twice.

    WF-037 FAIL /rules/rules/0/versions/0/value: builtin arity 2,
    expected 1

**Why the count is fixed.** The scalar kinds and the date-part extractors
take one operand (WF-037: "one operand for the scalar and extractor kinds"):
`yearOf` reads the year of one date. A second operand is a reading the kind
does not have, whatever it holds; here it repeats the first, so the operand
count is the whole defect. No witness pinned this arm before the L4 fix round:
no corpus file in the repository's history carries its sentence.

**The executors refuse it, each with its own sentence.** Measured 2026-09-24
against the five cases of the source golden: the reference interpreter faults
at evaluation (`EVALUATION FAULT: yearOf expects one argument`; re-measured
2026-09-25, until L5 it named the kind `jaar uit`), the ASP backend at
translation (`TRANSLATION FAULT: a part extractor expects one argument`), and
the Drools backend excludes the rule at transpilation (`transpiler SKIP`).

**The document.** `corpus/golden/jutterswet-paasvondst/ir.json` with the one
argument of the `yearOf` in its first rule's value repeated as a second.

Falsifiable, arm-exact: emptying the one-operand branch of the builtin-arity
clause in `walk_expr` (`spec/verifier/lib/checks.ml`) turns this case clean
and nothing else — 706 documents compared over golden, rood and negatief, one
verdict moved.
