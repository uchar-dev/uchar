# Veeg: de null-datatype-loader-strengheidskloof

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 3).

Goal: de kloof dichten waar clingo "unscalable datatype" weigert en
ref accepteert — symmetrie kiezen (loader-weigering aan beide kanten
of een expliciete recorded delta met getuige), rood-eerst
gedemonstreerd.

Acceptance: beide executors gedragen zich identiek of de delta staat
expliciet met getuige; alle acht gates groen; twee lenzen.

May touch: `ref/lib/case.ml`, `backends/clingo/`, `corpus/rood/`,
`spec/ASP-TRANSLATION.md`, `backlog/`.

Out of scope: verifier-wijzigingen tenzij de kloof daar hoort.

May-touch-verruiming (2026-08-21, tijdens de kaart): de reproductie
toont dat de kloof verifier-BEREIKBAAR is (verifier PASS op een
attribuut zonder datatype; ref laadt en draait, clingo fault) — de
kloof hoort dus bij de bron: een nieuwe WF-regel. Conform de
out-of-scope-clausule ("tenzij de kloof daar hoort") verruimt de
kaart may-touch met spec/IR-SPEC.md, spec/verifier/ en
corpus/negatief/ voor WF-059; de loader-weigering in de gedeelde
case-loader blijft als backstop (het WF-057/naamhek-precedent:
de verifier normeert, de fence is de laatste linie).
