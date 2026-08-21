# Tijdlijn-standing: de standing-norm voor temporele rekentargets

Besluit maintainer 2026-08-21: gekozen in het werkmenu.

Goal: de ch. 7.8-standing-norm (een berekende leeg verdringt geen
input; input staat defeasibel) uitvoeren voor temporele rekentargets,
waarmee het resterende deel van het ch. 6d-hek daar vervalt: de
tijdlijn-input als inputtl, staand tenzij de regel effectief schrijft,
segment-consistent met refs write_timeline-lezing onder de norm. De
kaart bepaalt eerst het precieze normgedrag per segment (staat de hele
input-tijdlijn of per bucket?) en legt dat als RS-tekstvoorstel naast
de implementatie — dat deelbesluit gaat als genummerde vraag naar de
maintainer VOOR de merge als het niet dwingend uit ch. 7.8 volgt.

Acceptance: rood-eerst (het hek weigert vandaag); nieuwe ankers pinnen
de drie hoeken (schrijvend, flank, leeg-doorval) temporeel; ref ≡
clingo; alle acht gates groen; twee lenzen.

May touch: `ref/lib/`, `backends/clingo/`, `spec/RUNTIME-SEMANTICS.md`
(ch. 7.8-aanvulling), `spec/ASP-TRANSLATION.md` (ch. 6d-notitie),
`corpus/golden/anchor/`, `scripts/check-noop.sh`, `backlog/`, en
(verruimd in de verificatieronde, protected-path-voorstel)
`spec/verifier/lib/desugar.ml` — het gedeelde domeinpredicaat
`standing_temporal_targets` hoort naast zijn scalaire broer.

Out of scope: pro-rata-doelen (eigen fold-machinerie, WF-056).

Result: the standing norm reaches temporal targets. RS ch. 7.8 gained
the timeline arm: a standing pro-rata target (shape-exact shared
domain, `standing_temporal_targets` beside its scalar sibling in
`Ir_v0.Desugar` — a protected-path change, disclosed) decides only on
a gevuld result; an empty fold writes nothing and the case's input
timeline stands WHOLE. Red-first: ref erased the input with an empty
write, clingo's fence refused both corners. ref: no-write on an empty
result; clingo: `inputtl/5` standing by NAF against the fold's
per-bucket effectiveness atom; the ch. 6d fence retired for standing
temporal targets, retained for multi-writer/versioned. Two additive
borgdeel cases pin it (`case5-input-blijft`, a reason-carrying gate-7
survivor; `case6-berekend-over-input`), 113 cases, all gates green.

The card-promised numbered question WAS due — the spec lens rightly
blocked the self-discharge: the whole-vs-per-bucket reading is not
forcing (per bucket, case6 erases input on a bucket the fold is leeg
on). Asked before the merge; the maintainer ratified the
WHOLE-TIMELINE reading (2026-08-21): one decision per target, the
timeline write replaces whole, input and computed never interleave.
Honest notes: the acceptance's three corners collapse to two for a
voorwaarde-less fold (no scalar condition exists on pro-rata rules —
the flank and the leeg fall-through are the same observable corner);
"out of scope: pro-rata-doelen" meant the WF-056 fold/bucket
machinery, which is verified untouched (the standing wrapper is purely
additive); the breaker lens held on all seven attack surfaces
(per-subject NAF, cross-target, overlap, exotic boundaries, scale);
the clip-over-standing-input interaction is reasoned but uncased —
recorded for a future clip card.
