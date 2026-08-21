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
