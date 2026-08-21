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
`corpus/golden/anchor/`, `scripts/check-noop.sh`, `backlog/`.

Out of scope: pro-rata-doelen (eigen fold-machinerie, WF-056).
