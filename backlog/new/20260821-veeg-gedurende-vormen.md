# Veeg: samengestelde gedurende-conditievormen

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 3).

Goal: de gedurende-conditievormen die v0 wel uitdrukt maar WF-055/056
en de vertaling nu buiten houden (samengestelde condities onder
gedurende) — spec-delta (WF-tekst + negatiefs die versmallen),
encodering in de clip/fold-machinerie, ankers differentieel.

Acceptance: rood-eerst; ankers door gates 5–6; verifier-delta met
negatiefs; alle acht gates groen; twee lenzen.

May touch: `spec/`, `backends/clingo/`, `ref/`, `corpus/`,
`backlog/` (ref alleen als de vorm daar ook hekt — expliciet melden).

Out of scope: anchored buckets en rate-restatement (aparte flank).
