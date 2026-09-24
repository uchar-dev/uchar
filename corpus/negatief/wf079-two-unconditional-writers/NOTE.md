Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1; `RUNTIME-SEMANTICS.md` ch. 7.7):
two unconditional computing rules (`BEREKENING`), each a form with one
always-valid version, write one attribute target without prioritized defaults
— `Jutter.uitkomst` — with no explicit priority. `uitkomst bij grootgezin`
computes `10.00` and `uitkomst bij kleine beurs` computes `20.00`; both fire
on every `Jutter`.

Pins the PHASE5D-DIFF §8.2 divergence witness ahead of the differential
square. The flat form is verifier-clean under the old single-writer rules
(WF-026 binds allocations, WF-050 binds defaults targets — neither a bare
attribute with two rules), yet it is **un-pinnable in every zone**: ref
oscillates 10 ↔ 20 and faults on the fixpoint budget (ch. 5.7), clingo carries
both values in one silent stable model, so ref ≢ clingo and no expectation
makes the pair pass. WF-079 refuses it: at most one unconditional computing
rule per target without defaults — priority is a defaults group (explicit)
or `ongeordend` (explicitly absent), never implicit agenda order.

The exemptions the fence must respect stay clean corpus-wide: pairs of
characteristic attributions, versions with neither `presentation` nor
`initialisation` (the penningrecht `= waar` pair, monotone), are outside the
rule, guarded case splits (`anchor/jutterswet-rekenkamer-overzee`, the
gewone/overzeese pair) carry their own priority through their conditions,
and the versions of one form are a list under that form: the rule compares
unconditional computing versions of different forms, and only where their
validity overlaps.

Old verifier: `PASS`. New verifier: `WF-079 FAIL … two unconditional computing
rules write target (jutter, uitkomst) with no prioritized defaults`.

Since L4 the two writers are two forms of one version each, both tagged
`presentation: "BEREKENING"` and without `condition` (deltas B19, B20); the
rule reads "computing" from the tag. `expect.txt` pins the arm.
