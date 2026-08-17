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
| `jutterswet-aanmeldquotum` | the temporal read (`TemporalRead`, WF-049): the art. 1 shape — a value read at an instant that is *not* the evaluation date (`asOfDate` is null). A temporal attribute read at a date attribute (multi-segment timeline with a gap year) and a granular-parameter vintage read at the same date. Expectations: the 2025 segment (12) and vintage (4) for a 2025 registration; the gap year 2026 reads to leeg while the vintage (5) still resolves; a missing registration date reads both to leeg (empty index propagates) |
| `jutterswet-penningvintage` | the ch. 8 vintage read, worked example 8.5 verbatim: a granular parameter read at the `Rekendatum` constant with `asOfDate` 2026-03-15 → the 2026 segment (5). The session-start Parameterset coverage check is reachable on this document: the three red variants of ch. 8.5 (evaluation date 2028; the timeline absent; asOfDate null) fault at session start in **both** executors — demonstrated in the card's verification, not admissible as goldens |
| `jutterswet-uitsluiting` | the art. 4 exception ladder (`DefaultLadder`, WF-050/051; RUNTIME-SEMANTICS ch. 7.6.1): three rungs on the characteristic `uitgesloten` — refused → excluded; refused ∧ herroepen → a do-not-attribute decision displaces (the Marrit shape); ∧ vervallen → excluded again — plus a negated consumer (`penninggerechtigd` unless excluded) that forces the ladder below the negative reader in the strata. Four case files: w / wh / whv pin the hand-run outcomes of `spec/ASP-TRANSLATION.md` ch. 6.3 (against this document's simplified consumer); the bare case is the added null witness — no grounds, not excluded, entitled |
| `jutterswet-vondstwaardering` | the attribute ladder with leeg fall-through and the initialisation rung (ch. 7.1/7.3, worked example 7.6.2): three rungs — erkend → basisbedrag, kandidaat → kandidaatbedrag, initialisation → 25. Six cases: the top rung decides (50); a leeg consequence falls through one rung (30) and to the initialisation (25); the initialisation alone (25); a non-deciding ladder leaves the input standing (40); a deciding rung overwrites the input (50, the defeasible-input lowering in clingo) |
