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

## Acceptatie (aangescherpt bij oppakken, 2026-09-20)

Stand hermeten vóór de wijziging, machinaal over de hele boom (688
`ir.json`): NUL gedelimiteerde literalwaarden, NUL `Onbekend`-getypeerde
literalen, en 16 `Enumeratie`-getypeerde literalen in 13 bestanden — alle
handgeschreven of uit de Catala-frontend, geen enkele uit een `bron.rgs`.
De kaartzin klopt: geen levende divergentie, wel een dekkingsgat. Gate 10
basislijn: PASS, 322 tests, 63 rgs-goldens.

- (a) `ExpressieBouwer` schrijft voor een `ENUM_LITERAL` het KALE label
  (uit de tokentekst, niet uit de whitespace-gevouwen bronsnippet) met
  het `Enumeratie`-datatype van de context; `bron` houdt de
  gedelimiteerde snippet. De afleidingsregel: het domein komt van het
  SCHRIJFDOEL (het gedeclareerde datatype van het doelattribuut) of van
  de ANDERE VERGELIJKINGSZIJDE. Waar geen van beide een `Enumeratie`
  aanwijst blijft de emissie een luide `Onopgelost`-marker met reden —
  het bestaande markerpatroon, dat sinds 2026-09-19 een WARNING krijgt —
  nooit een gok. Emittergetuigen in `IrEmitterTest`, rood-eerst: eerst
  de huidige gedelimiteerde+sentinelemissie vastgelegd, daarna omgezet.
- (b) Eén `bron.rgs`-golden met een enumeratieliteral in beide
  contexten, handberekende verwachtingen VOORAF genoteerd, gespeeld
  door ref en clingo; Drools gespeeld of benoemde EXCLUDE in beide
  kanaallijsten van `scripts/check-drools.sh` (voorstel, zuiver
  additief — de parallelle kaarten raken hetzelfde script).
- (c) De `Literal`-rij in `spec/IR-SPEC.md` ch. 4.1 zegt vandaag "an
  enumeratiewaarde keeps its `'…'` delimiters, which readers strip
  (`Literalen.enumLabel`)". Die zin beschrijft de FRONTEND, niet de
  wire: WF-039 kruist het kale label byte-exact tegen het gedeclareerde
  domein, dus de gedelimiteerde vorm is op de wire ill-formed. De zin
  wordt gecorrigeerd plus één alinea in de rij WF-081/084/095/tekstlexeem
  (voorstel, beschermd pad); WF-039 blijft byte-exact zoals hij is.
- (d) De toka-meting: de zone draagt 30 documenten met
  enumeratiedeclaraties (alle 30 `ir.json`, één gedeclareerd domein).
  Meet of de emitterwijziging daar bytes beweegt en of een herpin nodig
  is; meld de uitkomst en welke fixtures raken in het Result, maar
  herschrijf geen toka-fixture — dat oordeel is aan de maintainer.
- (e) Alle veertien gates groen, elk los gedraaid; frontend-wijziging
  betekent een herpin van de toolchain door de maintainer na de landing.

May touch: `frontends/regelspraak/**`, nieuwe goldenmap onder
`corpus/golden/`, `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), `spec/IR-SPEC.md` ch. 4.1 (voorstel), kaartverplaatsing.
Out of scope: de wire-vorm zelf (WF-039/WF-034 bewegen niet), B22, en
de consumentenkant — wat een backend met een kaal label DOET is niet
deze kaart.
