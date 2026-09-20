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

## Result (2026-09-20)

Beide sluitingen geland. De kaart heet naar de twaalf afkortingen, maar de
tabel telt er dertig en dat is wat de regel nu vergelijkt: WF-017 roept
`time_unit_of_spelling` aan — dezelfde opzoeking waarop WF-035/WF-038 de
duurrekenkunde routeren — in zowel de gedeclareerde-naam-clausule als arm (a),
zodat de regel en de semantiek niet meer uiteen kunnen lopen. De vier
Drools-dagsoortplekken lezen de kalendertabel eerst; die volgorde staat op één
plek (`Tijdrekening.dagsoortVoorEenheid`) en de frontend-typering leest hem
ook, zodat vijf lezers hem niet los van elkaar kunnen verliezen.

**Hermeten op 598e3060 (binair md5 `97239d1f8c2706a2548594b0541b5f34`), alle
vijf metingen van de kaart bevestigd**: de twaalf als registersleutel PASS; een
gedeclareerde `wk` PASS; de frontend emitteert `Dagsoort de wk (mv: wks)` met
exit 0, `dagsoorten ["wk"]`, `dagsoortPerEenheid {wk, wks}` en `diagnostics []`;
`jutterswet-vervaltermijn` draagt `wk`/`mnd`/`kw` als levende kalendereenheid;
en de OCaml-lezers zijn onverschillig (PASS + `ref` 7/7/8 op de drie cases).

**Rood-eerst, drie getuigen, elk precies één regel**:
`wf017-eenheidsleutel-schaduwt-kalenderafkorting` (sleutel, afkorting),
`wf017-dagsoortnaam-schaduwt-kalenderafkorting` (gedeclareerde naam) en
`wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` (`secondes` — geen
afkorting, en de klasse die een halve reparatie zou laten staan). Alle drie
PASS onder het binair van vóór de kaart.

**Nul over-afwijzing**: baseline en landed-stand over de vier corpuszones plus
de pariteitszone (699 documenten) zijn regel voor regel identiek — geen verdict
en geen rapportregel beweegt. **Arm-exact**: met alleen de verbreding inert
(de vergelijking terug naar de achttien) worden over alle 702 documenten
precies de drie nieuwe getuigen schoon en verandert nergens anders een regel.

**De frontend beweegt mee, en verder dan de sleutelkant.** De wacht vergelijkt
met dezelfde tabel; een gedeclareerde NAAM in de tabel is daar een ERROR, die
de emissie tegenhoudt. Dat is bewust geen waarschuwing-en-laten-vallen zoals de
meervoudsvorm krijgt: de naam laten vallen zou zijn §9.8-definitie laten wijzen
naar een dagsoort die niet meer gedeclareerd is, en dat weigert de verifier op
zichzelf. Zeven getuigen in `DagsoortKalenderspellingTest` (vijf rood vóór de
wijziging) plus de dertig-spellingenpin in `TijdrekeningTest`. Hiermee gaat ook
een oudere, niet-gekaarte flank dicht: `Dagsoort de uren (mv: urens)` emitteerde
vóór deze kaart met exit 0 een document dat WF-017 al jaren weigert.

**De tabel wordt nu ook werkelijk hoofdletterongevoelig gelezen.** De
lensronde vond een tweede manier waarop de uitvoerders op deze tabel uiteen
konden lopen, zonder dagsoort: `RegelSpraakModel.normaliseerTijdseenheid` gaf
in zijn default-tak de ORIGINELE string terug in plaats van de kleingeletterde
selector, waardoor alleen de negen `-en`-meervouden een hoofdletter
overleefden — die worden door de reducer VERVANGEN, en de vervanging was
toevallig kleingeletterd. Gemeten vóór de reparatie: `chronoUnit` gaf WEEKS
voor `weken` én `WEKEN` maar null voor `WK`, `Wk`, `' wk '` en `Sec`; een
verifier-schoon `jutterswet-bergingsklok` met eenheid `WK` en niets
geregistreerd spleet 3-tegen-2 — verifier, `ref` en de ASP-backend rekenden in
weken (die trimmen en kleinschrijven vóór de opzoeking), beide Drools-takken
weigerden de regel luid als onbekende tijdseenheid. Dat is een tegenvoorbeeld
bij precies de twee zinnen die deze kaart normatief maakte, dus de
grondoorzaak is gerepareerd in plaats van de zinnen gekwalificeerd. Beslisregel
gemeten: (a) gate 10 en 11 blijven byte-identiek (64 rgs-goldens, 45 toka-.rgs,
29 replayslices, 30 toolchain-gepinde `ir.json`) en (b) de splitsing dicht —
beide takken spelen de kalenderverwachtingen die `ref` bevestigt. Getuige:
`DagsoortOpzoekvolgordeWitnessTest.een_hoofdletterspelling_is_dezelfde_kalendereenheid`
plus een hoofdletter/trim-rij in `TijdrekeningTest`; mutatieproef uitgevoerd en
teruggedraaid (default-tak terug → precies die rij valt om, met de oude
`transpiler SKIP` als reden, de andere vier blijven groen).

**De splijting, gemeten en gesloten.** Splijtdocument: `jutterswet-bergingsklok`
met zijn eenheid als `wk` gespeld én `wk` geregistreerd. Vóór de ommekeer las
`ref` de kalender (2026-03-31 / 2026-02-17 / 5 / 5) waar beide Drools-takken de
dagsoort liepen (2026-04-15 / 2026-02-01 / 3 / 3) — `PLAYED 1` op de
dagsoortverwachtingen. Na de ommekeer spelen beide takken de
kalenderverwachtingen die `ref` bevestigt (13 expectations). Omdat het
verbrede WF-017 dat document afwijst, woont de getuige in de Drools-suite
(`DagsoortOpzoekvolgordeWitnessTest`, vijf rijen: de waarde in beide takken, de
DRL-tekst, de hoofdletterspelling hieronder, en twee tegenmetingen dat een
spelling buiten de tabel nog steeds de dagsoortwandeling is). Mutatieproef
uitgevoerd en teruggedraaid: de gedeelde
volgorde terugdraaien laat de getuige falen bij naam in beide takken en over
acht cellen; één enkele plek terugdraaien (DrlTranspiler §6.11) laat alleen
`drl-tekst` vallen en alleen op de vier datumcellen.

**Alle veertien gates groen.** Gate 12: `played 165` (pin 165) en DRL-tekst 142
(pin 142) — geen kanaal bewoog, dus `scripts/check-drools.sh` is niet
aangeraakt; geen enkel gate-script is aangeraakt. Gate 11 (pariteitszone)
PASS: geen toka-fixture beweegt onder de frontend-wacht. De "252 golden schoon"
van gate 2 en 3 is hun gezamenlijke telling van `corpus/golden` (218) plus
`corpus/rood` (34), niet 252 goldens.

**Meegenomen, met reden.** (1) De spellingstabel werd overal aan
`RUNTIME-SEMANTICS.md` ch. 1.2 toegeschreven; ch. 1.2 is het leegregime en de
tabel staat normatief in ch. 3.8. Gecorrigeerd in de alinea's die deze kaart
toch herschreef, in de verifier-comment, in de vier WF-017-NOTE's en in de
nieuwe code/tests. (2) `Tijdrekening.chronoUnit` struikelde over een `Eenheid`
zonder afkorting — een vorm die de wire toelaat en die `ref` als "geen eenheid"
leest; nu null in plaats van een NPE, met een getuige in `TijdrekeningTest` die
beide lezers op die vorm zet. (3) De hoofdletterongevoeligheid hierboven.
(4) De drop-waarschuwing van de wacht droeg een hardgecodeerd regelnummer 0,
dat een auteur in een document met meerdere dagsoorten nergens heen wijst; hij
draagt nu de regel van zijn eigen declaratie, gepind in de frontend-suite.

**Bewust NIET gedaan.** De producent-diagnostiek op de stille `putIfAbsent`
(arm (b)'s frontendkant) blijft buiten scope, zoals de kaart zegt.

**Toolchain-herpin (uit te voeren door de hoofdsessie).** De frontend wijzigt,
dus `SPEC_REV` bumpen en de conformance-PR in `regelspraak-java` moet DRIE
dingen overnemen: (1) de dertig-spellingenwacht bij de dagsoortregistratie;
(2) het ERROR-antwoord op een gedeclareerde dagsoortnaam die in de tabel staat;
(3) de omgekeerde OPZOEKVOLGORDE die ch. 3.8 nu normatief stelt — kalendertabel
eerst — die de toolchain op vier eigen Drools-plekken draagt, plus de
hoofdletterongevoelige tabelopzoeking die daaronder ligt. Nuttig bij het
snijden van die PR: van de vier frontend-`main`-bestanden die deze kaart
wijzigt bereiken er twee de Drools-build via de vendored slice
(`kern/Tijdrekening.java` en `model/RegelSpraakModel.java`); de pom-excludes
laten `resolver/**` weg, dus `DefinitionResolver` en `ExpressieBouwer` zijn
puur frontend — gemeten aan de gebouwde `target/classes`.

**Registervoorstel** voor `spec/CONSTRUCTS.md` rij 23, aan te vullen achter de
WF-017-landing van 2026-09-10:

> **Verbreed (2026-09-20, WF-017 gedeclareerde-naam-clausule + arm (a),
> IR-SPEC ch. 2.9):** de botsingslijst is niet langer de achttien namen van de
> frontend-wacht maar de **dertig spellingen** van de normatieve tijdseenheid-
> tabel (`RUNTIME-SEMANTICS.md` ch. 3.8), vergeleken via de tabelopzoeking
> zelf, zodat regel en duurrekenkunde één bron delen. De twaalf die erbij komen
> zijn de tien afkortingen (`jr`, `mnd`, `kw`, `wk`, `dg`, `u`, `min`, `sec`,
> `s`, `ms`) en de twee tweede meervoudsvormen `secondes`/`millisecondes`;
> getuigen `wf017-eenheidsleutel-schaduwt-kalenderafkorting`,
> `wf017-dagsoortnaam-schaduwt-kalenderafkorting`,
> `wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm`, nul over-afwijzing
> gemeten over 699 documenten. Anders dan de achttien was deze klasse
> PRODUCEERBAAR, dus de frontend-wacht bewoog mee (spec-first): een
> gedeclareerde naam in de tabel is daar een ERROR die de emissie tegenhoudt,
> een meervoudsvorm blijft waarschuwing-en-laten-vallen. **Leesvolgorde
> gelijkgetrokken:** ch. 3.8 stelt de volgorde normatief — kalendertabel eerst,
> dagsoortregister in de val-door-tak — en beide Drools-takken lazen op alle
> vier hun dagsoortplekken andersom, waardoor de uitvoerders op één toegelaten
> document uiteenliepen (gemeten: `ref` 2026-03-31/5 tegen Drools
> 2026-04-15/3). Nu één gedeelde lezer (`Tijdrekening.dagsoortVoorEenheid`),
> ook door de frontend-typering gebruikt; getuige
> `DagsoortOpzoekvolgordeWitnessTest` in de Drools-suite, want het
> splijtdocument is sinds de verbreding niet meer welgevormd. **Tweede
> splitsing op dezelfde tabel mee gedicht:** ch. 3.8 noemt de herkenning
> hoofdletterongevoelig, maar de meervoudsreducer gaf een onbekende spelling in
> zijn oorspronkelijke casus terug, zodat alleen de negen `-en`-meervouden een
> hoofdletter overleefden; een verifier-schoon document met eenheid `WK` liet
> verifier/`ref`/ASP in weken rekenen terwijl beide Drools-takken de regel luid
> weigerden. Spelling wordt nu overal getrimd en kleingeletterd vóór de
> opzoeking, gate 10 en 11 byte-identiek gemeten.
