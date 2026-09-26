Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1), its type arm: a
`ContextValue` named `evaluationYear` whose `type` annotation is a number
with two decimals, where the name fixes a whole `Number` with 0 decimals,
the sign unspecified and no unit.

    WF-098 FAIL /rules/rules/2/versions/0/condition/left/type: context value
    'evaluationYear' is annotated with a type other than the one its name
    fixes: a whole Number with 0 decimals, the sign unspecified and no unit

**Why the arm exists.** Nothing in a document declares the type of a
context value; the name fixes it (ch. 4.1), and every executor derives the
value from the name and reads the annotation nowhere (RUNTIME-SEMANTICS
ch. 2.10). So a wrong annotation reaches no reader that could notice it.
Measured 2026-09-26 on this document before the arm: `verifier: PASS`, and
on the two cases of `corpus/golden/jutterswet-rekenjaar` `ref` answered
`oracle: PASS (13 expectations)` and the ASP backend `clingo: PASS (13
expectations)` for each, and both Drools branches `PLAYED 2`. Every reader
answered the year, and the wire said the year has two decimals.

**The arm is the verifier's alone.** No executor reads the annotation, so
none has anything to refuse, and the executors are unchanged.

**The document.** `corpus/golden/jutterswet-rekenjaar/ir.json` with one
annotation changed: the `evaluationYear` that is the left operand of the
condition of `Kenmerktoekenning laat gemeld` (`indien het Rekenjaar groter
is dan zijn vondstjaar`) carries `whole: false, decimals: 2` instead of
`whole: true, decimals: 0`. A comparison operand was chosen because no
other rule reads the annotation there, so WF-098 is the only report.

Falsifiable, arm-exact (measured 2026-09-26): let the `evaluationYear`
half of `context_value_type_refusal` in `spec/verifier/lib/checks.ml`
accept any `Number` and this case turns clean while its sister
`wf098-rekendatum-met-tijd` stays refused; drop the type arm and both turn
clean, and gate 3 fails on both. Cost: over the golden, rood and negatief
zones and the parity zone (824 documents) the arm moved the verdict of
these two witnesses and of no other document. `expect.txt` pins the arm.
