# frontend-bron-annotatie-in-defaults: `// Bron:`-annotaties overleven het liften naar een defaults-groep

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting, gemeten
in het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(M6, kaartbranch): twee geannoteerde blokken (een basis en haar
`Uitzondering op`) geven een IR waarin geen van beide annotatieteksten nog
voorkomt; de verifier zegt niets. De lowering zet `bron: null` op de groep
en op elke default (`RuleChecker.java:331–351`, "no per-group bron", "No
per-default bron"). De werkbank-regelsets hangen hun herleidbaarheid per
wetslid aan precies die annotaties, en de handankers laten zien dat de wire
er ruimte voor heeft: `PrioritizedDefaults.bron` en `Default.bron` dragen
daar per default de wetsverwijzing (`anchor/ww-herleving-inkomen/ir.json`:
"art. 21 lid 2 onderdeel a: …").

## Ontwerp

- De `// Bron:`-annotatie van een gelifte regel landt in `Default.bron` van
  de default die eruit ontstaat; de annotatie van de basisregel bovendien
  (of alleen) in `PrioritizedDefaults.bron`; bij nesting op de default in
  de `ExceptionGroup`. Geen nieuwe wire-velden.
- De `Regel`-stub die per gelifte regel in `regels` achterblijft houdt zijn
  eigen `bron` zoals nu.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst JUnit-getuige met het M6-document: beide annotatieteksten
  staan in de uitgezonden IR op de verwachte plaats.
- (b) De bestaande extensie-goldens (`jutterswet-bergingsdrempel`,
  `jutterswet-wrakrecht`, `jutterswet-wachtgeld`) blijven byte-identiek als
  zij geen annotaties dragen; dragen zij die wel, dan worden hun `ir.json`
  opnieuw uitgezonden en verklaart de kaart het verschil.
- (c) `DIVERGENCES.md` §4 beschrijft de plaats van de annotatie.
- (d) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, de drie extensie-goldens (alleen
heruitzending), kaartverplaatsing.

Niet in deze kaart: annotaties op andere geliftende vormen dan de
defaults-extensie.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-trio` (tweede van drie), na
`frontend-regelgroep-sluit-bij-uitzondering`.

1. M6 reproduceren: twee geannoteerde blokken, geen annotatietekst in de
   IR, verifier PASS.
2. Plaatsing: de annotatie van een gelifte regel in `Default.bron` van de
   default die eruit ontstaat; die van de basis bovendien of alleen in
   `PrioritizedDefaults.bron` — kiezen en motiveren langs de handankers
   (`anchor/ww-herleving-inkomen` en de andere defaults-ankers); bij
   nesting op de default in de `ExceptionGroup`. Geen nieuwe wire-velden;
   de `Regel`-stub in `regels` houdt zijn eigen `bron` zoals nu.
3. Rood-eerst JUnit-getuige met het M6-document: beide annotatieteksten op
   de verwachte plaats in de IR; mutatieproef met cmp-herstel.
4. De drie extensie-goldens dragen geen annotaties (gemeten): zij blijven
   byte-identiek, geen heruitzending, gates 5/6/12 bewegen niet.
5. `DIVERGENCES.md` §4 beschrijft de plaats.
6. Gate 10 byte-identiek (70 rgs-goldens), alle veertien gates groen, elk
   als losse aanroep.

## Result (2026-09-23)

Uitgevoerd zonder nieuw wireveld. De annotatie van een gelifte regel
landt op de `Default.bron` van de default die zij wordt, en die van de
basis bovendien op `PrioritizedDefaults.bron`; een lid van een ongeordend
blok landt op de groep die zijn naam draagt en op haar enige default. De
drie extensie-goldens dragen geen annotaties: byte-identiek, geen
heruitzending, geen mandaat gebruikt, en gates 5, 6 en 12 bewegen niet.
Alle veertien gates groen. Dit is een HERPIN-kaart.

### 1. Reproductie (basis `143fd3d3`, na de eerste kaart van de branch)

M6-probe (buiten de repository): `// Bron: art. 4 lid 1 Jutterswet` boven
de basis, `// Bron: art. 4 lid 2 Jutterswet` boven haar `Uitzondering op`.

    vóór:  0 voorkomens van "art. 4" in de IR; PrioritizedDefaults.bron null,
           Default.bron [null, null]; diagnostics []; verifier: PASS
    na:    PrioritizedDefaults.bron "art. 4 lid 1 Jutterswet";
           Default.bron ["art. 4 lid 2 Jutterswet", "art. 4 lid 1 Jutterswet"];
           diagnostics []; verifier: PASS
    json-diff vóór/na: precies drie bron-velden van null naar de annotatietekst

### 2. De keuze en haar motivering (verbatim)

**Bovendien, niet alleen.** De annotatie van de basisregel staat op de
basisdefault én op `PrioritizedDefaults.bron`. Motivering langs de
handankers: in alle negen ankers onder `corpus/golden/anchor/` met een
defaults-groep draagt elke default, de laagste inbegrepen, een eigen
`bron` naast die van de groep — `ww-herleving-inkomen`: de groep "art. 15
tot en met 21: …", de basisdefault "art. 15: …"; `jutterswet-hardheid`:
de groep "art. 3 en 5", de defaults "art. 5" en "art. 3";
`jutterswet-uitsluiting`: de groep "art. 4", de drie lagen elk "art. 4,
… laag". De tekst alleen op de groep zou de basisdefault een vorm geven
die geen anker heeft, en de citatie per default kwijtraken waar de
herleidbaarheid per wetslid aan hangt. De groepstekst van de ankers is een
overkoepelende verwijzing ("art. 3 en 5") die de frontend niet kan
schrijven zonder tekst te verzinnen; de groep draagt de naam van de basis,
dus zij krijgt de tekst van dat blok. Verworpen: de groep de samengevoegde
annotaties van de hele familie geven; dat herhaalt de tekst van elke
default en verandert zodra er een uitzondering bijkomt.

Verder: meerdere annotaties op één blok worden in bronvolgorde verbonden
met "; ", een lege wordt weggelaten zoals de emitter hem uit
`bronAnnotaties` weglaat. Een ongeordend lid geeft zijn tekst aan groep én
default (de vorm van `jutterswet-wrakconflict`). De `Regel`-stub in
`regels` verandert niet: het schema laat daar alleen `node`, `naam` en
`soort` toe, er is geen eigen `bron` om te houden. Nesting is niet
gebouwd (dat is de kaart dialect-uitzondering-op-uitzondering), maar elke
gelifte regel wordt op één plek een default (`RuleChecker.defaultVan`),
dus een geneste default draagt de annotatie van zijn blok vanzelf; wat
`ExceptionGroup.bron` zelf draagt, beslist die kaart.

**Bijvangst, in deze kaart opgelost.** Een annotatie boven de kop
`Uitzonderingen ongeordend:` hoort bij geen regel (de annotatie hoort bij
de regel direct eronder) en verdween stil. Zij krijgt nu een WARNING die
haar noemt, in plaats van aan een lid gehangen te worden dat de auteur
niet koos.

### 3. Rood-eerst en mutatie

Getuige `DefaultsBronAnnotatieTest` (nieuw bestand, bestaande tests niet
aangeraakt): vier plaatsingstests en twee tegenproeven (geen annotatie
geeft overal null; de stub blijft zonder `bron`).

    tegen de oude RuleChecker (md5 a4f9be2e09df6c123b0badce7080b569):
      6 tests, 2 successful, 4 failed:
      m6_…        "de uitzonderingsdefault (hoogste) draagt haar eigen annotatie
                   ==> expected: <art. 4 lid 2 Jutterswet> but was: <null>"
      meerdere_…  "expected: <art. 3 Jutterswet; art. 5 Jutterswet> but was: <null>"
      ongeordend_… "expected: <art. 9 lid 1 Jutterswet> but was: <null>"
      kopannotatie "verwachtte precies één melding: [] ==> expected: <1> but was: <0>"
    na de fix:                              6 successful
    mutant A (Default.bron weer null):      3 failed (m6, meerdere, ongeordend)
    mutant B (PrioritizedDefaults.bron null): 3 failed (m6, meerdere, ongeordend)
    mutant C (kopwaarschuwing uit):         1 failed (kopannotatie)
    elke mutant teruggezet met cp, cmp gelijk (md5 b9e6b9626db152d022099131a727ae74)

### 4. De extensie-goldens

Gemeten: de `bron.rgs` van `jutterswet-bergingsdrempel`, `-wrakrecht` en
`-wachtgeld` dragen nul `// Bron:`-regels (de enige goldenbronnen met
annotaties, `jutterswet-schouwrooster` en `jutterswet-hardheidsoverschot`,
gebruiken de extensie niet). Door de nieuwe frontend gehaald: cmp-gelijk
aan de gecommitte `ir.json`, alle drie. Geen `ir.json` aangeraakt.

### 5. Gates, elk als losse aanroep na `eval $(opam env)`

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  278 documenten, vloer 192
     3 check-verifier           PASS  278 golden schoon; 440 negatief, vloer 350
     4 check-case-schema        PASS  638 casusbestanden
     5 check-oracle             PASS  ref-suite 806, vloer 489; 595 cases, vloer 427
     6 check-clingo             PASS  suite 523, vloer 83; 595 cases ref = clingo
     7 check-noop               PASS  559 weerleggen; 36 overlevers
     8 check-rood               PASS  43 rood, vloer 26; 86 fragmenten; Drools 9 gepind, 34 niet-gedragen
     9 check-catala             PASS  201 checks, 12 goldens
    10 check-regelspraak        PASS  421 JUnit (415 + 6), vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek, 27 tests, 29 cases nul divergenties
    12 check-drools             PASS  gespeeld 182 (pin), DRL-tekst 161 (pin), JUnit 620
    13 check-reftype-derivation PASS  131 twists over 27 goldens, 128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks, 2 goldens

Geen vloer of pin aangeraakt.

### 6. Herpin

Het vendored frontend-slice beweegt: `RuleChecker.java`, `DIVERGENCES.md`
en de nieuwe test. De emissie verandert voor elk geannoteerd
extensiedocument (bron niet langer null); geen gecommit document is er
een. De herpin in regelspraak-java doet de hoofdsessie na de merge.

### 7. Registervoorstel — `spec/CONSTRUCTS.md` rij 3

Aan het eind van de IR-deltakolom, na "… the machine-readable legal-source
id stays open (LRML-audit C1's optional tail)":

> **Follow-up (2026-09-23, card `frontend-bron-annotatie-in-defaults`):
> the annotations survive the lift into a defaults group.** A rule the
> dialect's `Uitzondering op` / `Uitzonderingen ongeordend` lifts leaves
> `resolvedRegels`, and with it the `bronAnnotaties` that carry its
> `// Bron:` citation; the lowering also wrote `bron: null` on every group
> and default, so every annotation on an extension block vanished,
> verifier-clean. It now lands, with no new wire field, on the
> `Default.bron` of the default the rule becomes, and the base's also on
> the `PrioritizedDefaults.bron` of the group named after it — the shape
> of every hand-written defaults anchor, where each default carries its
> own citation beside the group's. Several annotations on one block are
> joined with "; "; an annotation above an unordered block's header
> belongs to no rule and draws a WARNING instead of vanishing. No
> committed document carries an annotated extension block, so nothing
> re-emits. A HERPIN card.

### 8. Open punten

1. Een `// Bron:`-annotatie boven een `Regelgroep`-kop, of boven welke
   niet-regelconstructie ook, verdwijnt nog steeds stil. Ouder gedrag en
   buiten de defaults-extensie (de uitsluiting van de kaart).
2. Wat `ExceptionGroup.bron` draagt bij geneste uitzonderingen beslist de
   kaart dialect-uitzondering-op-uitzondering; deze kaart legt alleen vast
   dat de geneste default de annotatie van zijn blok krijgt.

### Fixronde 1 (2026-09-23, hoofdsessie)

**Open punt 1 gesloten in `9fd44dbe`.** Eerst gemeten of binding kon.
Het schema geeft `ResolvedRegelGroep` wel een verplicht `bron`-veld,
maar de emitter schrijft daar het v0-bronfragment van de groepsnaam in
(`g.bron().snippet()`, "berging"). Een wetsverwijzing op die plek zou een
gespiegeld veld een tweede betekenis geven. De kleinste stap is daarom
de waarschuwing: een `// Bron:` boven een `Regelgroep`-kop krijgt dezelfde
WARNING als die boven `Uitzonderingen ongeordend:`, met §9.10.
`kopAnnotatie` is daarvoor algemeen gemaakt; de melding voor het
ongeordende blok bleef letterlijk gelijk. Een WARNING komt in de
`diagnostics` van de IR, maar geen bron in welke zone ook draagt een
`Regelgroep`, dus niets beweegt.

    getuige BronAnnotatiePlaatsTest (nieuw)
    tegen RuleChecker md5 b304844d2b62d7692fea1d54529d0ca1 (tip 4d5da739):
      2 tests, 1 successful, 1 failed —
      "verwachtte precies één melding: [] ==> expected: <1> but was: <0>";
      de tegenproef (annotatie onder de kop hoort bij het eerste lid) groen
    na de fix:                              2 successful
    mutant R (Regelgroep-kop niet gemeld):  1 failed
    teruggezet met cp, cmp gelijk (md5 a4d57e8008af789c5e7ca0410333822a)

Gemeten waar annotaties in de praktijk staan: 736 `// Bron:`-regels in
de 513 werkbankbronnen.

- 220 boven een Regel;
- 164 boven een Situatie (de scenariolaag);
- 351 boven een definitie: Objecttype 110, Feittype 99, Domein 97,
  Parameter 27, Extensie 15, Eenheidsysteem 3;
- 1 boven een Beslistabel.

Een annotatie boven een definitie krijgt bewust geen waarschuwing. Zij
documenteert de declaratie en de wire heeft er geen plek voor (open punt
3). Honderden waarschuwingen op gewoon gebruik zouden ruis zijn, en de
golden `jutterswet-schouwrooster` (annotatie boven een Dagsoort) zou
een melding in zijn `diagnostics` krijgen.

**Bijvangst, opgelost in `e8d030cf`.** Een annotatie boven een
`Beslistabel` verdween ook stil: elke rij wordt in een eigen tokenstroom
geparsed, waar geen commentaar boven staat. Gemeten op de build van
`cd37c5a9`: `diagnostics: []`, geen `bronAnnotaties` op de twee ontlede
regels, "art. 8" nergens in de IR. Een tabel is regels, dus de annotatie
landt nu op elke ontlede regel. Dat volgt de meerversieregel van IR-SPEC
ch. 3.1: één commentaarblok boven een gedeelde kop, per afgevlakte regel
gestempeld.

    tegen RuleChecker md5 4fb882fd89a6cc97ff8973d33e3addf5:
      3 tests, 2 successful, 1 failed —
      "rij 'Vondstwaarde 1' mist de annotatie ==> expected: <[art. 8 Jutterswet]> but was: <[]>"
    na de fix:                                   3 successful
    mutant B (tabelannotatie niet doorgegeven):  1 failed
    teruggezet met cp, cmp gelijk (md5 f42fbaf463d3bd58b9d41548bdbfa2b4)

Geen golden en geen pariteitsbron draagt een geannoteerde beslistabel.
Op tip `e8d030cf`: gate 10 PASS (451 tests, 70 rgs-goldens
byte-identiek), gate 1 PASS, gate 11 PASS (45 .rgs byte-identiek, de twee
beslistabellen van de pariteitszone inbegrepen). Het vendored slice
beweegt verder: HERPIN.

Aanvulling op het registervoorstel voor rij 3, als laatste zin:

> The same WARNING covers an annotation above a `Regelgroep` header, whose
> own `bron` is the v0 snippet of its name; and an annotation above a
> decision table, which vanished because each row is re-parsed in a
> stream of its own, now lands on every rule the table desugars to (the
> multi-version precedent of ch. 3.1). An annotation above a definition
> stays a comment: the wire has no slot for a declaration's provenance.

### Open punten, bijgewerkt

1. Gesloten (hierboven).
2. Ongewijzigd: wat `ExceptionGroup.bron` draagt, beslist de kaart
   dialect-uitzondering-op-uitzondering.
3. Nieuw, gemeten: een annotatie boven een definitie (351 van de 736 in
   de werkbank) haalt de wire niet, want de IR heeft geen plek voor de
   herkomst van een declaratie. Dat is een wirebesluit (rij 3 noemt de
   gestructureerde `Bron` als open delta), geen frontendfix.

### Fixronde 2 (2026-09-23, hoofdsessie; lensadvies 5)

`89ed0f46`: de "; "-koppeling van meerdere annotaties op één blok is
niet terug te draaien. Eén annotatie die zelf "; " bevat, leest daarna
precies als twee, en zo'n tekst staat al op de wire (de groepsverwijzing
van golden `jutterswet-samenloop`). `DIVERGENCES.md` §4 legt dat verlies
van onderscheid vast. De wire verandert niet: een lijst bewaren zou een
nieuw veld vergen (optie (b) van het voorstel, niet genomen). Wie de
annotaties gescheiden wil houden, schrijft er één per blok.
