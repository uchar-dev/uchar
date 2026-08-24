Violates WF-079 (`spec/IR-SPEC.md` ch. 3.1; `RUNTIME-SEMANTICS.md` ch. 7.7):
two unconditional, version-free computing rules (`BEREKENING`) write one
non-laddered attribute target — `Jutter.uitkomst` — with no explicit
priority. `uitkomst bij grootgezin` computes `10.00` and `uitkomst bij kleine
beurs` computes `20.00`; both fire on every `Jutter`.

Pins the PHASE5D-DIFF §8.2 divergence witness ahead of the differential
square. The flat form is verifier-clean under the old single-writer rules
(WF-026 binds allocations, WF-050 binds ladder targets — neither a bare
attribute with two rules), yet it is **un-pinnable in every zone**: ref
oscillates 10 ↔ 20 and faults on the fixpoint budget (ch. 5.7), clingo carries
both values in one silent stable model, so ref ≢ clingo and no expectation
makes the pair pass. WF-079 refuses it: at most one unconditional computing
rule per non-laddered target — priority is a ladder (explicit) or `ongeordend`
(explicitly absent), never implicit agenda order.

The exemptions the fence must respect stay clean corpus-wide: `KENMERKTOEKENNING`
pairs (the penningrecht `= waar` pair, monotone) are outside the rule, guarded
case splits (`anchor/jutterswet-rekenkamer-overzee`, the gewone/overzeese
pair) carry their own priority through their voorwaarden, and versioned forms
group by stripped name.

Old verifier: `PASS`. New verifier: `WF-079 FAIL … two unconditional computing
rules write target (jutter, uitkomst) with no ladder`.
