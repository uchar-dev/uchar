# Anchor corpus

Hand-written golden cases with **absolute expected values**, permanently
pinned. This subset is the parity-independent ground truth of the
differential square (`spec/PLAN.md` §8): backend agreement is never
accepted as truth by itself — every expectation here is computed by hand
from the normative semantics (`spec/RUNTIME-SEMANTICS.md`), and no
emitter, interpreter, or solver output is ever pasted back in.

Anchor cases differ from the emitted goldens one directory up in two
ways: there is no `bron.rgs` (the `ir.json` is authored directly against
`spec/IR-SPEC.md`), and they may carry the optional **extension fields**
of the decided wire-entry discipline (IR-SPEC, Method) — v1 constructs
the pinned v0 emitter cannot produce yet. Everything else holds
unchanged: schema-valid and byte-round-tripping (gate 2),
verifier-clean (gate 3), case files valid (gate 4), and replayed with
exact expectation match by both executors (gates 5 and 6).

| Case | Exercises |
|---|---|
| `jutterswet-bergingstoezicht` | explicit role bindings (WF-047): **two** fact types over the same object-type pair (Berging–Berger), which the bindingless type-pair derivation cannot resolve; a proportional allocation carrying `verbindendeRol` and an instance-fold sum carrying `bronRol`. The `bergingstoezicht` relation is anchor-authored on top of the art. 9 berging casus to force the two-relation shape. Expectations: shares 60/40 of a pot of 100 by weights 60/40; the supervised-yield sum 100+40=140 for the supervisor, the 0-fallback for the other |
