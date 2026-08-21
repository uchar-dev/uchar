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

Result (2026-08-21): de kloof is bij de bron gedicht met WF-059 —
elk gedeclareerd attribuut draagt een datatype, het attribuut-pendant
van WF-018 (waarvan de parameterkant al dicht bleek: mijn
parameter-negatief werd rood-eerst door WF-018 verworpen, niet door
de nieuwe regel — dus geschrapt). Rood-eerst dubbel gedemonstreerd:
de levende asymmetrie (verifier PASS + ref PASS + clingo
"unscalable datatype" op dezelfde schone input) en gate 3 rood op het
negatief vóór de check bestond. De gedeelde case-loader weigert een
waarde tegen een datatype-loze declaratie als laatste linie (beide
executors identiek; het bericht noemt WF-059 voor attributen en
WF-018 voor parameters — de lens ving dat één codepad beide regels
backstopt). De lenzen escaleerden terecht BLOCKING: een nieuwe
normatieve MUST was geen van de twee kaartroutes en is een
maintainersbesluit — als genummerde vraag voorgelegd met het bewijs
(nul null-datatypen in het corpus; conformance byte-identiek; de
tracked-delta-route heropent luid als de mirror ooit legitiem null
emit); de maintainer ratificeerde WF-059, waarna de provenance-regel
in het IR-SPEC-blok is gezet naar het WF-057-precedent. Ook naar
lens-vondst: het blok verhuisde van ch. 2.1 naar ch. 2.2 (bij de
attribuutregels), de wf059-functie staat nu ná wf057 (de
commentaar-ordening), en S14 noemt dateTime naast tekst/enum/boolean
als operandposities waar de unscalable-fault de laatste linie
blijft. May-touch-verruiming (spec/IR-SPEC.md, spec/verifier/,
corpus/negatief/) staat op de kaart genoteerd met de
out-of-scope-clausule als grond. Bewust gelaten: het
regel-leest-datatype-loos-attribuut-pad heeft geen eigen
corpus-getuige (het document is nu per constructie verwerpbaar;
gate 3 dekt).
