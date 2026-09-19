# v8-enum-emitter: de frontend emittert enumeratieliteralen welgevormd

Uit `spec/proposals/V1-WIREMIGRATIE.md` V8, **BESLIST (2026-09-19,
maintainer): route (a)** — nu, als eigen v0-kaart naar het model van de
tekstlexeem-kaart (`backlog/done/20260918-tekstlexeem-canonicalisatie.md`).

De stand (V8, hermeten bij oppakken): de wire is dubbel gehekt (WF-039
op het label, WF-034 (r) op het sentineltype) en nul van de documenten
draagt de gedelimiteerde vorm — er is geen levende divergentie en geen
stil fout antwoord. Wat er wél is: de RegelSpraak-frontend kan een
enumeratieliteral niet welgevormd emitteren — hij schrijft de snippet
mét delimiters én laat het type op het onbekend-sentinel staan — en
daardoor draagt geen enkele `bron.rgs`-golden er een.

Eerlijk prijskaartje (uit het besluit): dit is niet één emitterregel.
Naast het strippen van de delimiters moet de frontend de literal ook
TYPEREN, en dat is contextafhankelijk werk — welke `Enumeratie` bedoeld
is volgt uit het schrijfdoel of de vergelijkingszijde. Het precedent
(tekstlexeem) deed alleen het strippen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De emitter stript de delimiters én typt de literal tegen de
  context; emittergetuige(n) in de frontend-JUnit-suite (rood-eerst:
  eerst de huidige gedelimiteerde+sentinel-emissie vastleggen).
- (b) Eén `bron.rgs`-golden met een enumeratieliteral, handberekende
  verwachtingen, gespeeld door ref en clingo; Drools gespeeld of
  benoemde EXCLUDE (kanaallijst-voorstel).
- (c) De zin in `spec/IR-SPEC.md` ch. 4.1 die de gedelimiteerde vorm
  als wire-vorm beschrijft wordt gecorrigeerd (voorstel, beschermd
  pad); WF-039 blijft byte-exact zoals hij is.
- (d) De toka-meting uit het besluit: de zone draagt 30 documenten met
  enumeratiedeclaraties — meet of daar een emitterdivergentie ontstaat
  (gate 11) en of een herpin nodig is; meld de uitkomst in het Result.
- (e) Alle veertien gates groen; frontend-wijziging betekent een
  herpin van de toolchain door de maintainer na de landing.

May touch: `frontends/regelspraak/**`, nieuwe goldenmap onder
`corpus/golden/`, `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), `spec/IR-SPEC.md` ch. 4.1 (voorstel), kaartverplaatsing.
Out of scope: de wire-vorm zelf (WF-039/WF-034 bewegen niet), B22.
