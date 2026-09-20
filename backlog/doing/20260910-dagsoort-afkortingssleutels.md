# De twaalf afkortingen: WF-017 dekt achttien namen, de kalendertabel kent er dertig

Goal: de flank sluiten die WF-017's sleutelarm op 2026-09-10 bewust open liet,
of vastleggen waarom hij open blijft. Arm (a) (ch. 2.9) weigert een sleutel van
`dagsoortPerEenheid` die samenvalt met een van de **achttien** ingebouwde
namen — het enkelvoud en het meervoud van de negen eenheden. De normatieve
kalendertabel (`spec/RUNTIME-SEMANTICS.md` ch. 1.2) herkent er **dertig**. De
twaalf afkortingen daartussen passeren zowel arm (a) als de gedeclareerde-naam-
clausule, en anders dan de achttien is deze vorm **niet wire-only**: de eigen
frontend produceert hem diagnosticloos. Daaronder ligt een tweede kwestie die
deze kaart óók moet adresseren: de uitvoerders lezen de eenheid in
**verschillende volgorde**, en dat is de eigenlijke bron van de divergentie.

## De meting (2026-09-10)

De twaalf spellingen die tussen de achttien en de dertig vallen:

    jr  mnd  kw  wk  dg  u  min  sec  s  ms  secondes  millisecondes

1. **Als registersleutel: verifier PASS.** `jutterswet-schouwrooster` met alle
   twaalf toegevoegd als sleutel (waarde `schouwdag`) is `verifier: PASS` —
   arm (a) ziet ze niet.
2. **Als gedeclareerde naam: ook PASS.** De clausule over `/dagsoorten`
   vergelijkt met dezelfde achttien, dus een dagsoort die `wk` heet is
   verifier-schoon. De flank zit dus in de héle regel, niet alleen in arm (a).
3. **Niet wire-only — de frontend produceert het.** Bron
   `Dagsoort de wk (mv: wks);` plus een dagsoortdefinitie:

       frontend exit=0
       dagsoorten:         ["wk"]
       dagsoortPerEenheid: {"wk": "wk", "wks": "wk"}
       diagnostics:        []                       <- leeg, geen WARNING

   De registratiewacht in de frontend dekt exact dezelfde achttien namen, dus
   ook daar is de afkorting geen bezwaar. Dit is het scherpe verschil met de
   achttien: die zijn wire-only (grammatica + wacht), deze twaalf niet.
4. **Levende dragers in het corpus.** `jutterswet-vervaltermijn` gebruikt `wk`,
   `mnd` en `kw` als gewone kalendereenheid (naast `dagen`, `maand`, `week`).
   Dat is dus geen hypothetische spelling.
5. **De OCaml-lezers zijn onverschillig, gemeten.** Diezelfde golden met
   `dagsoorten: ["schouwdag"]` en `{"wk": "schouwdag", "mnd": …, "kw": …}` is
   `verifier: PASS` en repliceert in `ref` byte-identiek: `oracle: PASS` met
   7 / 7 / 8 expectations op de drie cases, precies als de onaangeroerde
   golden. De verifier (WF-035/WF-038), `ref` en de ASP-backend raadplegen
   namelijk **eerst** de kalendertabel en komen bij de dagsoortregistratie
   alleen in de `None`-tak.

## De eigenlijke divergentiebron: de opzoekvolgorde van Drools

De Drools-backend keert die volgorde om. Op alle vier zijn dagsoortposities
vraagt hij `model.dagsoortVoorEenheid(<afkorting>)` **vóór** de kalendertabel
en valt pas op de kalenderweg terug als dat null oplevert:

    backends/drools/.../model/ExecModelTranspiler.java   §6.11 datumrekenkunde
    backends/drools/.../model/ExecModelTranspiler.java   §6.10 tijdsduur
    backends/drools/.../DrlTranspiler.java               §6.11 datumrekenkunde
    backends/drools/.../DrlTranspiler.java               §6.10 tijdsduur

Dat botst met de normatieve zin in `RUNTIME-SEMANTICS.md` ch. 1.2: "A spelling
outside the table is a declared day kind" — buiten de tabel, dus de tabel gaat
voor. Gevolg: een document dat `wk` als dagsoorteenheid registreert én `wk` als
kalendereenheid gebruikt, splijt de uitvoerders — drie lezen kalenderrekenkunde,
Drools leest dagsoortrekenkunde. Dat is een echte differentiële breuk, geen
smaakkwestie, en hij is vandaag met verifier-schone invoer bereikbaar.

## Wat er te beslissen valt

1. **Verbreedt arm (a) (en de gedeclareerde-naam-clausule) naar de
   dertig-spellingentabel?** Dan is de vergelijkingslijst niet langer de
   achttien van de registratiewacht maar de tabel van ch. 1.2 — één bron van
   waarheid met de semantiek. Consequentie: de frontend-wacht moet mee, anders
   weigert de IR wat zijn eigen producent maakt (spec-first, conformance-kant).
   Meet vooraf wat het kost: draagt een golden of pariteitsfragment vandaag een
   afkorting als dagsoortsleutel? (Op 2026-09-10: nee.)
2. **Of conformeert de Drools-opzoekvolgorde aan ch. 1.2** — tabel eerst,
   registratie in de `None`-tak, zoals de drie andere lezers? Dat is een
   backend-kaart, en hij haalt de tegenstrijdigheid op de wire niet weg (het
   document blijft twee dingen tegelijk zeggen), maar hij haalt de
   uitvoerdersplitsing weg.
3. **Of allebei**, in welke volgorde. De twee vragen zijn onafhankelijk: (1)
   gaat over wat de wire mag dragen, (2) over hoe een uitvoerder een
   toegelaten document leest.
4. **Blijft `secondes`/`millisecondes` erbij?** Die twee zijn geen afkorting
   maar een tweede meervoudsvorm; ze vallen om dezelfde reden buiten de
   achttien. Waarschijnlijk hetzelfde antwoord, maar benoem het.

## Acceptance

- Rood-eerst per gesloten clausule: een negatief dat onder de verifier van vóór
  de kaart PASS is en daarna precies één regel met WF-017 en positie. Minstens
  twee: een afkorting als registersleutel en een afkorting als gedeclareerde
  dagsoortnaam. De twee klassen — afkorting én tweede meervoudsvorm — worden
  apart gedekt.
- Arm-exact: alleen de nieuwe clausule weg → alleen de nieuwe getuigen weer
  schoon, over de volle vier-zone-sweep plus de pariteitszone.
- Nul over-afwijzing, gedateerd gemeten, met baseline en landed-stand apart
  benoemd — inclusief de twaalf afkortingen als gewone kalendereenheid, die
  verifier-schoon MOETEN blijven (`jutterswet-vervaltermijn` is de drager: `wk`,
  `mnd`, `kw`; die golden mag geen byte bewegen).
- De frontend-wacht beweegt mee en krijgt zijn getuige in de frontend-JUnit-
  suite (gate 10), rood-eerst: eerst de huidige diagnosticloze emissie
  vastgelegd, dan de wacht.
- De Drools-volgorde beweegt mee op alle vier de gemeten plekken en krijgt een
  gate-12-getuige die de splitsing vandaag zou aantonen en na de kaart niet
  meer. Het splijtdocument (`wk` als dagsoorteenheid geregistreerd én als
  kalendereenheid gebruikt) wordt onder het verbrede WF-017 verifier-AFGEWEZEN,
  dus de getuige is een test direct op beide transpilers in de Drools-JUnit-
  suite; de rood-eerst-meting op het nog wire-bereikbare document staat in het
  dossier.
- ch. 2.9 en ch. 1.2 vertellen na afloop de werkelijke dekking; de flank-alinea
  in ch. 2.9 wordt vervangen door de landing, en `secondes`/`millisecondes`
  worden daar expliciet benoemd.
- Alle veertien gates groen (de regel hierboven zei dertien; de gateset is sinds
  het schrijven van deze kaart gegroeid).

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/negatief/**`,
`corpus/golden/**`, `backlog/`, `frontends/regelspraak`, `backends/drools` —
het besluit hieronder snijdt de kaart opnieuw en neemt beide kanten mee.

## Out of scope

- De dagsoort-duurvormen zelf (§6.10/§6.11) — `20260910-dagsoort-semantiek.md`.
- De waardehelft van `dagsoortPerEenheid` (WF-034 arm (s), dicht sinds
  2026-09-09) en de sleutelbotsingen die WF-017 op 2026-09-10 sloot
  (`backlog/done/20260909-wf017-sleutel-en-spellingsbotsing.md`).
- De producent-diagnostiek op de stille `putIfAbsent` — dezelfde done-kaart
  noemt hem als conformance-werk; verwante frontendkant, andere vraag.

## Maintainersbesluit (2026-09-19): verbod naar dertig én één leesvolgorde

Beide sluitingen in één kaart: het WF-017-verbod gaat alle dertig
kalenderspellingen dekken (met negatieve getuigen per klasse), én de
opzoekvolgorde van de eenheid wordt in alle uitvoerders gelijkgetrokken
— de gemeten bron van de divergentie dicht mee.
