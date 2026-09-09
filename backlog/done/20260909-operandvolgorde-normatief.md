# Operand-evaluatievolgorde normatief spellen (Begrensd, TIJDSDUUR)

Goal: RUNTIME-SEMANTICS legt de evaluatievolgorde van multi-operand-
knopen normatief vast waar die volgorde executor-zichtbaar is via het
§9.5-kanaal. Vandaag is de volgorde bij twee knopen alleen af te lezen
uit de ref-binary: `Begrensd` evalueert basis→maximum→minimum en
TIJDSDUUR/ABSOLUTE_TIJDSDUUR evalueert "tot" vóór "van", maar beide
volgen uit OCamls ONgespecificeerde argumentvolgorde bij gelabelde
aanroepen (`Arith.clamp`; gemeten op ocaml-base-compiler 5.4.1,
native én bytecode gelijk — een flambda-build zou kunnen flippen).
De toolchain-Let-port (20260901-toolchain-let-port, Result) heeft die
volgorde in haar statische spoorwandeling gepind op "wat de
referentie-binary doet"; dat hoort andersom: de spec spelt de
volgorde, ref bewijst haar.

Aanleiding: de eindverificatie van de Let-port (2026-09-09) mat dat de
volgorde het §9.5-kanaal raakt — een companion-dragende operand die
vóór dan wel ná een barrière-knallende lezing evalueert, deponeert wel
of niet. Zolang de spec zwijgt is ref's toevallige volgorde de facto
norm.

Acceptance:
- RUNTIME-SEMANTICS draagt per multi-operand-knoop met kanaal-zichtbare
  volgorde (minimaal Begrensd en de tijdsduurfuncties; sweep de andere
  knopen en verklaar per knoop volgorde-zichtbaar of -onzichtbaar) de
  normatieve volgorde met een gewerkt voorbeeld.
- ref maakt de volgorde expliciet (lets in plaats van gelabelde
  argumentvolgorde) zodat de binary niet langer de drager is; gedrag
  byte-identiek op het corpus (0 pin-bewegingen verwacht).
- Een getuige pint de volgorde observeerbaar (kanaalfeit-telling) in
  ref én clingo; alle dertien gates groen.

May touch: spec/RUNTIME-SEMANTICS.md, ref/**, backends/clingo/**
(indien de vertaling de volgorde spelt), corpus/golden/** (getuige),
backlog/.

Out of scope: toolchain-wijzigingen (de spoorwandeling daar volgt
vanzelf bij de eerstvolgende conformance-cyclus); Drools-backend-
wijzigingen tenzij de getuige een divergentie blootlegt.

---

## Result (2026-09-09)

**De ontwerpkeuze: wire order, links naar rechts — NIET de feitelijke
volgorde.** De kaart beval aan de huidige feitelijke volgorde te
normaliseren tenzij de sweep een knoop vond waar die aantoonbaar
inconsistent is met een verwante knoop. Die is gevonden, twee keer:

1. **Binnen `Begrensd` zelf.** Ch. 2.7 spelt al "minimum applied first",
   en `Arith.clamp` past de grenzen ook in die volgorde toe. De
   *evaluatie* liep precies andersom (maximum eerst). Eén knoop met twee
   tegengestelde richtingen erin.
2. **De clingo-vertaler liep al in wire order.** Zijn tijdsduur-arm doet
   `operand_day a; operand_day b;` en daarna `expr_term … a` vóór
   `expr_term … b` — van vóór tot. ref deed het omgekeerd. Een norm op de
   feitelijke ref-volgorde had de vertaler moeten omgooien; wire order
   kost hem niets.

Daar komt bij dat wire order de ENIGE volgorde is die het document al
spelt — §5.10 ("left to right in wire order"), de fold (ch. 10.2),
`links`/`rechts`, WF-073's velddeclaratievolgorde. De twee toevallige
volgordes waren de enige twee rechts-naar-links-plekken in het systeem,
en ze waren toevallig: gemeten op deze build evalueert OCaml de
argumenten van een applicatie van rechts naar links, en `List.map`
documenteert zijn resultaat maar niet zijn toepassingsvolgorde (alleen
`List.iter`/`fold_left` leggen die vast). De spec spelt nu; ref bewijst.

**Kosten, expliciet — gecorrigeerd in de fixronde.** Een eerdere versie
van deze Results zei dat de spoorwandeling in de toolchain "vanzelf
volgt bij de eerstvolgende conformance-cyclus". Dat was ONWAAR, en het
is nagemeten: `scripts/uchar-conformance.sh` daar vergelijkt alleen
gevendorde bronbytes tegen `SPEC_REV` en emitter-goldens byte-voor-byte,
en zegt zelf dat het uchars gates "trusts, and does not re-run". Er
draait geen enkele casus. Een semantische wijziging is voor dat script
onzichtbaar; er volgt niets vanzelf.

Het zijn bovendien DRIE gepinde plekken, niet twee, elk in twee
transpilers: `Begrensd` max-eerst (`DrlTranspiler.java:2267`), de
tijdsduur-lus omgekeerd (`:2276`) en `leegwaarde` vóór de bijdragers
(`:2278-2279`), gespiegeld in `ExecModelTranspiler.java:5169-5190`. Een
arbitertest — `ch_10_6_begrensd_evalueert_maximum_voor_minimum`,
`ExecModelDifferentialTest.java:5737-5769` — vuurt echte
Drools-sessies en pint de nu-foute volgorde met absolute
kanaalfeit-tellingen (1 en 0), met in zijn javadoc `ref/lib/eval.ml` als
bewijs aangevoerd: precies het bewijs dat deze kaart wegneemt.

Twee dingen die de afwijking verkleinen, ook nagemeten — **maar zie
fixronde 2 (F1/F4): beide waren te ruim geformuleerd.** De uitgestoten
`rsBegrens(basis, min, max)` en `rsTijdsduur(van, tot)` lopen op
vuurmoment al goed, dus voor DIE TWEE rijen zit de afwijking in de
statische boekhouding; de aggregatierij is op vuurtijd echter LUI en
wijkt daar wél op runtime af. En de tweede walk die al goed loopt is niet
`leesVolgorde` (dode code, nul aanroepers sinds `118b771`) maar
`DrlTranspiler.verzamelBinderDefinities:1611-1635`, die wél levend is —
de interne tegenspraak bestaat dus, alleen tussen andere functies dan ik
eerst schreef.

Gevolg: `backlog/new/20260909-toolchain-spoorwandeling-ch511.md`
geschreven, met de vier plekken, de om te keren arbitertest, de
volgordetabel op `record Pad` (`DrlTranspiler.java:2192-2217`) en de
acceptance. Ch. 5.11 draagt de bijbehorende notitie: tot die kaart landt
is de pin een **gemeten normafwijking op de kanaalfeit-volgorde**, geen
tweede lezing van de norm.

Op het corpus kost de keuze niets — zie de meting.

### De sweep (RUNTIME-SEMANTICS ch. 5.11)

Elke expressie- en conditieknoop met een oordeel. **Zichtbaar** =
minstens twee operandposities kunnen elk falen, een §9.5-feit
deponeren of een ch.-10.6-need forceren. **Gedwongen** = de datastroom
legt de volgorde al vast. **Onzichtbaar** = hooguit één operandpositie
evalueert ooit.

| knoop | oordeel | norm |
|---|---|---|
| `Binair`, `Vergelijking` | zichtbaar | `links`, dan `rechts` |
| `Begrensd` | zichtbaar | `basis`, `minimum`, `maximum` |
| tijdsduur / absolute tijdsduur | zichtbaar | `van` (arg 0), dan `tot` (arg 1) |
| aggregatie (som/totaal/aantal/min/max/eerste/laatste) | zichtbaar | de bijdragers in wire order (instantiefold: het filter per lid, leden in foldvolgorde), dan de `leegwaarde` |
| `concatenatie`, `TekstReeks` | zichtbaar | deelvolgorde |
| `ListLiteral`, `StructLiteral` | zichtbaar | element- resp. gedeclareerde veldvolgorde (WF-073) |
| `Invoke` | zichtbaar | argumentvolgorde, dan het lichaam |
| `Fold` | zichtbaar | `list`, `init`, dan de elementen in lijstvolgorde |
| `MapList`, `FilterList` | zichtbaar | `list`, dan elementvolgorde |
| `Samengesteld` | zichtbaar | ch. 5.10 (wire order + kortsluiting) |
| `Let` | gedwongen | het lichaam stuurt; de `value` draait op de EERSTE lezing, op de plek van die lezing |
| `Match`, `IfThenElse` | gedwongen | scrutinee/conditie kiest de arm; de niet-gekozen tak evalueert niets |
| `Afgerond`, wortel, absolute waarde, jaar/maand/dag uit, eerste paasdag, `ListLength`, `FieldAccess`, `Construct`, `DimensieCel` | onzichtbaar | één operand |
| `TemporalRead` | onzichtbaar | alleen `datum`; `basis` NOEMT de tijdlijn en evalueert nooit (ch. 3.3) |
| `Predicaat` (leeg/gevuld) | onzichtbaar | één operand |
| `Literal`, `AttribuutRef`, `ParameterRef`, `VariabeleRef`, `Constante`, `BinderRef`, `Kenmerk`, `Regelversie`, `Rollidmaatschap`, `ExprOnopgelost`, `ConditieOnopgelost` | onzichtbaar | geen operanden |

**Aangevuld in de fixronde (E4) — de sweep was niet uitputtend tegen zijn
eigen kop.** Negen ontbrekende knopen alsnog beoordeeld; ref-geweigerde
vormen krijgen hun oordeel mét de weigering genoemd, want de spec is
engine-neutraal:

| knoop | oordeel | norm |
|---|---|---|
| `Periode` (de periodeconditie: `vanaf`, `tot`) | **zichtbaar** | `vanaf`, dan `tot`. Corpusaanwezig (`jutterswet-vormenkabinet` plus twee negatief-gevallen); ref weigert de vorm bij evaluatie, dus gesteld maar nog niet uitgevoerd |
| aggregatie over een `dimensieSelectie` (de DERDE bijdragerbron; WF-037 laat hem toe op SOM/MINIMUM/MAXIMUM) — toegevoegd in fixronde 2 | **zichtbaar** | de cellen in de labelvolgorde van de selectie, dan de `leegwaarde`; ref weigert de vorm |
| `datum met` / `datum-tijd met` | **zichtbaar** | argumentvolgorde; WF-037 laat de ariteit open. ref weigert de vorm |
| `tijdsevenredig deel` | onzichtbaar | WF-056 beperkt de basis tot een KALE temporele attribuutverwijzing, dus alleen het filter is datagevoelig — één foutdragende positie |
| `aantal dagen in` | onzichtbaar | geen expressieargumenten; het filter draagt de conditie |
| uur/minuut/seconde/milliseconde uit | onzichtbaar | één operand; ref weigert ze |
| `Predicaat` elfproef / numeriek met exact / dagsoort | onzichtbaar | één operand; ref weigert ze |
| `VerreRollidmaatschap` | onzichtbaar | geen expressieoperanden; ref weigert de vorm |
| `Lambda` | geen operandpositie | nooit een waarde (WF-069): alleen de `functie`-sleuf van een `Fold`/`MapList`, per element toegepast onder díe rij |

Regel- en vormniveau (welke regel eerst vuurt, welk lid van een
defaults-groep) valt hier NIET onder: ch. 5.2 maakt het fixpunt
volgorde-onafhankelijk en ch. 7.1 legt de defaults-ketting op prioriteit
vast. Zo staat het ook in de sectie.

### Wat ref en clingo nu dragen

- **ref** sequenceert elke multi-operandknoop met `let`s en loopt elke
  operandlijst door twee expliciet gesequenceerde wandelingen
  (`map_in_order` / `filter_in_order`) — twaalf plekken. De binary is
  niet langer de drager.
- **De som's `leegwaarde`** stond vóór de bijdragers (buiten wire
  order); nu erna. WF-037 beperkt hem tot de som, dus dit is de enige arm
  die er ooit een evalueert — voor elke andere soort verwerpt de
  verifier hem (`wf037-fallback-on-minimum`), zodat de verplaatsing niets
  anders raakt.
- **backends/clingo** had dezelfde onbepaaldheid in zijn Begrensd-arm
  (`let lo = … and hi = …`, met een effectvolle rechterkant die faalt,
  emit en `ctx.fresh` muteert). Nu twee `let`s, minimum eerst.

### De meting: nul pinbewegingen

Vóór de ref-wijziging: gate 5 = 425 cases exact, gate 6 = 425 replays
ref ≡ clingo. Ná: **425 / 425**, identiek. Ná de clingo-wijziging
nogmaals gate 6: **425**. De volgordekeuze is op het corpus van vandaag
onzichtbaar — precies waarom er getuigen bij moesten.

### De getuigen (rood-eerst, handberekend, met mutant gemeten)

| getuige | zone | pint | mutant |
|---|---|---|---|
| `synth3-operandvolgorde` | golden | `Binair` links vóór rechts, via de KANAALFEIT-TELLING; case1 pint `consistent` (0 feiten — de afwezigheid IS de pin), case2 is de controle die bewijst dat de deling wél deponeert zodra ze bereikt wordt | rechts-vóór-links: `MISMATCH: expected consistent, but the channel carries: vangstsom van een vangst × V1` |
| `begrensd-reads-bounds-in-order` | rood | `Begrensd` minimum vóór maximum, in BEIDE executors: ref door WELKE fout valt (`division by numeric zero` vóór de barrière in het maximum), clingo door WELKE grens de vertaler als eerste weigert | ref omgekeerd: `EXPECTED FAULT DID NOT OCCUR`. clingo omgekeerd: rapport zegt `a Begrensd maximum bound …`, gepinde fragment afwezig |
| `tijdsduur-reads-van-before-tot` | rood | `van` vóór `tot` in ref, via de Rekendatum-fout vóór de barrière | ref omgekeerd: `EXPECTED FAULT DID NOT OCCUR`. clingo: ONVERANDERD |

**Eerlijk over de clingo-kant.** Het golden-getuige draait in ref én
clingo (en, zo bleek, ook in Drools — de volle differentiaalvierkant).
Maar de ASP-codering sequenceert niet: haar enige volgorde-afgeleide
constructie is de §5.10-prefixbewaking, die alleen werkt omdat die
triggers monotoon zijn in de prefix. Voor Begrensd is haar volgorde een
VERTAALvolgorde (welke grens ze eerst weigert), niet een
evaluatievolgorde — genoeg om een omkering te vangen, en zo opgeschreven.
Voor tijdsduur pint ze niets: ze draagt de vorm en faalt, maar in beide
richtingen hetzelfde. Dat staat met zoveel woorden in de NOTE en in
ch. 5.11, in plaats van als sterk gepresenteerd te worden.

**Waarom Begrensd geen golden kon worden:** de ASP-backend weigert een
`Begrensd`-grens die geen grond-literal is (een lege grens is ref's GEEN
grens, wat de kale join niet kan uitdrukken). Zowel een deling als een
binderlezing draagt atomen. Voor tijdsduur sluit een andere deur: de
enige expressie-deponeerder die beide executors dragen is een deling
door een lege deler, en die moet een tijdsduur-operand via `datum + duur`
bereiken — waarop clingo een EENHEIDdragende multiplicatieve operand
weigert. Vandaar rood in plaats van goud, met de reden in de NOTE.

### Blootgelegde divergentie (gemeld, niet gerepareerd — buiten scope)

**De Drools-backend evalueert een `Begrensd` MAXIMUM vóór MINIMUM**, de
omgekeerde ch.-5.11-volgorde. In beide richtingen gemeten op precies deze
vorm:

| deling staat in | ref | Drools |
|---|---|---|
| het `minimum` (het rode geval) | **faalt** | draait door naar een model |
| het `maximum` | schrijft 100, geen fout | **faalt**, `Breuk deling door 0` |

Spiegelbeelden — dus een VOLGORDE-divergentie, geen ontbrekende controle:
Drools evalueert de deling wél, alleen van de andere kant. Met die reden
in de uitsluitingslijst van `check-rood.sh`. Voor `Binair` houdt Drools
ch. 5.11 wél — vandaar dat de golden spelend binnenkwam.

Twee dingen die de maintainer kan willen naplannen (buiten may-touch
gelaten, dus NIET aangeraakt): (a) `ASP-TRANSLATION.md` S18 kan een
notitie gebruiken dat ref's positionele barrière en clingo's
lichaamsbrede bewaking uiteenlopen zodra een deponering vóór een
barrière staat — ch. 5.11 legt die grens nu vast, S18 zwijgt erover;
(b) de Drools-Begrensd-volgorde.

### Fixronde 2026-09-09 (lens 1)

De lens reproduceerde de gates en alle getuigen en weerlegde de kaart op
één blokkerend en drie ernstige punten. Alle vier verwerkt.

**B1 — de toolchain-consequentie.** Zie "Kosten, expliciet" hierboven: de
zin was onwaar, is nagemeten en gecorrigeerd, en er ligt een vervolgkaart
`backlog/new/20260909-toolchain-spoorwandeling-ch511.md`. Twee dingen die
de lens niet had en die de verificatie opleverde, staan er in: de
uitgestoten `rsBegrens(basis, min, max)` loopt op vuurmoment al goed, en
een tweede walk in hetzelfde bestand (`leesVolgorde`) draagt de drie
ch.-5.11-volgordes al — de correctie heft dus ook een interne tegenspraak
op.

**E2 — de artefactbelofte dekte 5 van 13 zichtbare rijen.** Terecht: de
lens keerde beide geordende wandelingen in `eval.ml` om en élke gate
bleef groen. Nu draagt **elke** zichtbare rij een artefact. Twee gewerkte
voorbeelden per knoopsoort in `ref/test/test_ref.ml`, met een ander
device dan de corpusgetuigen: twee operandposities die elk MET EEN EIGEN
BOODSCHAP falen (`1 ÷ 0` tegen een `Onopgelost`-knoop), gewisseld tussen
het paar — de overlevende boodschap noemt de positie die eerst liep.
Waarom niet de kanaaltelling: een insert is idempotent per (regel ×
subject), dus twee deponeringen uit één knoop vallen samen tot één feit
en kunnen de posities niet uit elkaar houden. Waarom niet de barrière:
een gesloten foldlichaam (WF-069) heeft geen omsluitende binder in scope,
en juist die rijen moesten erbij.

Twee mutanten gedraaid, en ze sterven nu:

| mutant | gevolg |
|---|---|
| beide wandelingen rechts-naar-links (resultaatvolgorde intact) — de mutant van de lens — plus `leegwaarde` vóór de bijdragers | **18 falende voorbeelden**: som-bijdragers, som-leegwaarde, concat, tekstreeks, listliteral, structliteral, invoke, maplist, filterlist — beide richtingen elk |
| `Vergelijking` rechts-eerst + `Fold` init-eerst | **4 falende voorbeelden** |

Vóór de fixronde bleef élke gate groen onder de eerste mutant.

**E3 — de som-leegwaarde-flip had geen getuige, en de tekst verzweeg de
onvoorwaardelijkheid.** Beide gerepareerd. De ordehelft (bijdragers vóór
leegwaarde) staat in `ref/test` als paar. De onvoorwaardelijkheid — de
leegwaarde evalueert ook mét gevulde bijdragers, vóór én na deze kaart —
is nu een rood corpusgeval, `corpus/rood/som-fallback-always-evaluated`,
handberekend: bijdragers 5 en 1 geven som 6, en de ongebruikte
`1 ÷ 0` faalt er toch overheen. Gemeten tegen de luiheidsmutant (de
leegwaarde alleen evalueren als geen bijdrager gevuld is): `EXPECTED
FAULT DID NOT OCCUR` in gate 8 én een falend voorbeeld in `test_ref`.
Ch. 5.11 draagt er nu een eigen alinea over, want "de bijdragers, dan de
leegwaarde" leest anders dan het is.

**E4 — de sweep was niet uitputtend.** Negen knopen aangevuld; zie de
tabel hierboven.

**Klein.** (a) `corpus/rood/**` staat hieronder als may-touch-verbreding.
(b) Leeg-propagatie is als VIERDE vroegtijdige beëindiging opgenomen (een
lege `list` maakt een `Fold` af zonder zijn `init` te lezen). (c) De
sleutelvolgorde delegeert nu naar het SCHEMA (`$defs/Begrensd`:
`node`, `basis`, `minimum`, `maximum` — byte-gepind door gate 2) in
plaats van naar `IR-SPEC.md`, dat de knoop `Clamp` noemt waar de wire
`Begrensd` zegt.

### Fixronde 2 2026-09-09 (lens 2)

Lens 2 bevestigde code, corpus en alle dertien gates (beide
mutanttellingen exact, nul rebase-conflicten met main `3777c20`) en
weerlegde vier zinnen plus twee ongepinde deelclaims. Alle zes verwerkt.

**F1 — de divergentie-alinea klopte niet op de rij die het hardst
spreekt.** Ik schreef dat de toolchain-afwijking "in de statische
boekhouding zit en niet in wat draait". Dat geldt voor twee van de drie
rijen, niet voor de derde. Gemeten: de clamp- én de tijdsduurrij stoten
`rsBegrens(basis, min, max)` en `rsTijdsduur(van, tot)` uit en lopen op
vuurtijd dus al in de norm-volgorde — een STERKER argument dan ik maakte.
Maar de aggregatierij is op vuurtijd **lui**: vier plekken evalueren de
leegwaarde alleen bij lege bijdragers (twee ternaries, een
transpile-time platgeklapte boolean, en een fold die zijn stand-in
alleen bij `cnt == 0` bereikt). Dat is precies de luiheid die ch. 5.11's
onvoorwaardelijke clausule verbiedt en die mijn rode geval bestaat om te
weerleggen — een RUNTIME-afwijking, geen boekhouding. Alinea per rij
herschreven; de vervolgkaart heeft nu een acceptance-punt dat **de TERM
onvoorwaardelijk maakt**, niet alleen de wandeling omdraait, en noemt dat
het zwaarste punt.

**F2 — "gate 2 maakt de sleutelvolgorde een machinaal gecontroleerd
feit" was onwaar, en experimenteel gebroken.** De lens zette `maximum`
vóór `minimum` in een golden; gates 2, 3 en 5 bleven groen, want
`check-ir-schema` her-emitteert in de sleutelvolgorde van het document
zelf en JSON Schema kent geen ordeningsvocabulaire. Teruggebracht tot
wat waar is: het schema is de **conventie** die de volgorde spelt; wat
haar machinaal pint is smaller — gates 9 en 10 her-emitteren 50 van de
192 documenten (38 `bron.rgs`, 12 `bron.catala_en`) byte-identiek uit
bron; voor de overige 142 handgeschreven documenten controleert niets
het. De clausule zelf lijdt er niet onder: de volgorde die ertoe doet is
die waarin een consument operanden LEEST, en die is per zichtbare rij
gepind. **Open vraag voor de maintainer, geen bouwplicht:** hoort een
sleutelvolgorde-check bij gate 2? Vandaag rust de wire-conventie op de
emitters en op review.

**F3 — de leeg-propagatie-bullet was onwaar zoals geschreven.** Alleen
`Fold` beëindigt vroeg (een lege `list` is het antwoord, `init` draait
niet). Bij `ListLiteral`, `StructLiteral`, `Invoke` en `MapList`
evalueert ref ALLE operanden en toetst daarna op leegte — een leeg
operand kost zijn opvolgers niets. De bullet sprak zichzelf al tegen
("once all its arguments are in"). Herschreven naar wat ref doet: leeg
propageert in het RESULTAAT, niet door de WANDELING. Lens-mutant E (de
bullet letterlijk implementeren) passeert daarmee terecht: gates 5/6/7
groen bij de lens, `test_ref` 489 groen bij mij — na de herschrijving is
dat correct gedrag van de tekst, geen gat.

**F4 — de vervolgkaart citeerde dode code.** `leesVolgorde`/
`forceerVolgorde` heeft nul aanroepers sinds `118b771`; ik voerde hem op
als de levende tegenhanger. De LEVENDE tegen-geordende wandeling is
`DrlTranspiler.verzamelBinderDefinities:1611-1635` (min→max op `:1622`,
argumenten→leegwaarde op `:1625-1626`). Vervangen, `leesVolgorde`
expliciet als dode code benoemd, de pin bijgewerkt (`SPEC_REV` staat op
`3777c20`, niet `3023b40`), en de negen-rijen-acceptance teruggeschaald:
zes vormen vallen al onder de uitsluitingszin op `:2218-2221`, `datum
met` onder de bestaande `Functie`-rij, en `Vergelijking`/`Periode`
hebben daar geen `Expr`-vorm.

**F5 — ongepinde deelclaim: de instantiefold.** "the filter per member,
members in fold order" had geen artefact: alle 23 voorbeelden gebruikten
de gelijste-argumenten-tak, dus lens-mutant C (de twee
`filter_in_order`-plekken en de temporele set-map omgekeerd) passeerde
alles. Nu twee voorbeeldparen erbij over een instantiefold met twee
leden wier filter verschillend faalt (`merk` = 1 kiest de deling,
`merk` = 0 de marker), voor BEIDE gefilterde wandelingen — de
`Instances`-tak die een `aantal` neemt en de `Values`-tak die een
set-lezende `som` neemt. Mutant C in de exacte lensvorm doodt nu
**precies die 4** voorbeelden en niets anders; een strakkere variant
(`filter_in_order` zelf omgekeerd) doodt er 6, de twee
`FilterList`-voorbeelden erbij.

**F6 — `dimensieSelectie` ontbrak** als derde bijdragerbron (WF-037 laat
hem toe op SOM/MINIMUM/MAXIMUM). Toegevoegd, met het oordeel én de
ref-weigering erbij, zoals bij elke andere geweigerde vorm.

**Redactie.** `KenmerkCheck` → `Kenmerk` en `Onopgelost` →
`ExprOnopgelost`/`ConditieOnopgelost` (de wire-namen); een inleidende zin
benoemt nu dat de tabel wire-namen gebruikt en dat de `Functie`-familie
één wire-knoop is die op `kind` onderscheidt, dus met kleine letters in
de brontaalspelling. De motivering voor het schema boven `IR-SPEC.md` is
versterkt: de naamsplitsing is systematisch — `Clamp`/`Begrensd`,
`Rounding`/`Afgerond`, `TextComposition`/`TekstReeks` — en een clausule
over SLEUTELvolgorde moet de sleutels noemen zoals een document ze
draagt. "22 voorbeelden" was fout: het waren er 23 (18 + 4 + 1), nu 27.

**Commitmarkering, eerlijk gemeld.** De markering is inconsistent en dat
is mijn inconsistentie: de commit die `spec/RUNTIME-SEMANTICS.md` raakte
draagt "(voorstel, beschermd pad)" in zijn onderwerp, maar de twee
commits die `scripts/` raakten — óók een beschermd pad — dragen dat
label niet. Binnen de bestanden idem: de `EXPECTED_PLAYED`-wijziging in
`check-drools.sh` kreeg een gedateerd herkomstcommentaar, de drie
uitsluitingsregels in `check-rood.sh` alleen een reden zonder datum (de
lijst daar is daarin zelf gemengd). Niet met terugwerkende kracht
herschreven — de geschiedenis is lineair en de hele branch is een
voorstel — maar de maintainer leest het hier in plaats van het te moeten
afleiden.

### Gatestand op de kop (alle DERTIEN groen)

Vanuit de worktree; `sh`/`bash` per shebang; `eval $(opam env)`; gates
8/12/13 hebben de jar herbouwd.

1 PASS · 2 = **192** · 3 = **192 golden / 339 negatief** · 4 = **453** ·
5 = **427** (2 zonder case: `jutterswet-seizoenvondst`,
`jutterswet-vormenkabinet`) · 6 = **427** · 7 = **391** / 36 survivors
(GEEN nieuwe: beide golden-cases pinnen een positieve waarde, dus een
no-op executor faalt ze) · 8 = **26** rode gevallen (Drools-leg 6
fault-pinned / 20 met reden) · 9 = 201 checks / 12 catala-goldens ·
10 = 173 tests / 38 rgs-goldens · 11 = 45 / 2 holdouts / 27 / 29 —
**onbewogen** · 12 = played **119** / channel floor 47 /
value-divergence 1 / no-case 2 / JUnit **223** · 13 = **74** twists /
15 goldens / 0 survivors. Daarnaast `test_ref` **489** gewerkte
voorbeelden groen (462 → 489: 26 volgordevoorbeelden + 1
onvoorwaardelijkheid; 23 in fixronde 1, 4 in fixronde 2 voor de
instantiefold).

Stand t.o.v. aanvang: golden+rood 188 → 192, negatief 339 → 339, cases
448 → 453, replays 425 → 427, rood 23 → 26, played 118 → 119, JUnit 223
→ 223, twists 74 → 74, test_ref 462 → 489.

`git status --short` leeg; de branch blijft **ONGEMERGED**. Beschermd
pad: `spec/RUNTIME-SEMANTICS.md`. May-touch-verbredingen, expliciet
gemeld: `scripts/` (de kaart voorzag EXPECTED_PLAYED en een kanaalregel
met reden — het werden `check-drools.sh` en twee, nu drie, regels in
`check-rood.sh`) en `corpus/rood/**` (de kaart noemde alleen
`corpus/golden/**` bij de getuige, maar schreef "rood-eerst waar een
getuige landt", en drie van de vier getuigen bleken alleen rood te
kunnen).
