Violates WF-056 (`spec/IR-SPEC.md`), arm `WF-056.condition-not-check`: the
gedurende condition of the pro-rata fold is an `Unresolved` marker, not a
check of an own temporal characteristic, so the fold has no true periods to
prorate over.

The RegelSpraak frontend's own output for the jutterswet-bakenmeester
golden with its condition naming "dienstvrij", a characteristic the document
does not declare: the frontend writes the condition as `Unresolved` and
accounts for it with a warning (WF-113). Measured 2026-11-03: the verifier
before this arm passed the document, and `ref` refused it at evaluation
with REF-009.
