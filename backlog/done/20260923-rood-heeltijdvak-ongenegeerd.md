# rood-heeltijdvak-ongenegeerd: de tweeling die de nieuwe weigering pint

Voorstel uit de heeltijdvak-leegtest-kaart (2026-09-23): een rode
tweeling van `corpus/rood/leegnegatie-heel-tijdvak` met
`negatie: false` zou de weigering van de ONgenegeerde
heel-tijdvak-leeg/gevuld-test blijvend in alle drie de uitvoerders
bewaken (ref en clingo weigeren met de ch.-3.9-zin, de Drools-loader
sinds die kaart met `Predicaat.periode`). Vandaag pint alleen de
JUnit-getuige de Drools-kant en de suite de OCaml-kant. Micro-kaart:
document + NOTE + check-rood-Drools-regel (**voorstel**).

## Acceptatie

- (a) Het rode document, rood-eerst tegen een jar zonder de
  loaderweigering (die speelde); gate 8 telt hem met beide fragmenten.
- (b) Alle veertien gates groen; geen pinbeweging.

May touch: `corpus/rood/**`, `scripts/check-rood.sh` (**voorstel**),
kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Branch `card/rood-duo`, samen met `rood-asofdate-kalender`.

- (a) `corpus/rood/leegtest-heel-tijdvak`: de tweeling van
  `leegnegatie-heel-tijdvak` met `negatie: false` (de `bron`-zin
  zonder "niet"), dezelfde case. NOTE met de handberekening volgens
  ch. 3.9 en per uitvoerder de reden van de weigering. ref en clingo
  pinnen de ch.-3.9-zin; `ref --bodemzin` tegen de lijst van de gate
  geeft geen HIT.
- (b) Rood-eerst: de jar van `8b685402` (basis van de
  heeltijdvak-leegtest-kaart, vóór `38bd97f0`) via `git archive` in
  /tmp gebouwd met `mvn -o package`. Daar geeft DroolsRoodMain NOFAULT
  en spelen beide takken een groene kopie met de handwaarden. De
  huidige jar geeft in beide takken EXCLUDE `Predicaat.periode`.
- (c) Drools-been: een `drools`-fragment kan alleen op FAULT. Eerst
  aantonen dat de gate een fragment op deze EXCLUDE weigert, dan één
  regel in de uitsluitingslijst (**voorstel**, beschermd pad).
- (d) Gates 1–14 groen, elk los. Gate 8 telt het document. Gate 12
  veegt alleen `corpus/golden`, dus de pins 181/160 bewegen niet.

## Result (2026-09-23)

**Geland als voorstel, alle veertien gates groen.** Het document is
`corpus/rood/leegtest-heel-tijdvak` (commits `72936b35`, `5f314873`).
Het is `leegnegatie-heel-tijdvak` met `negatie: false` en een `bron`
zonder "niet", met een byte-gelijke case. De Drools-regel staat in
`8f24ad3d` (**beschermd-pad-voorstel**, `scripts/check-rood.sh`).

### Wat elke uitvoerder doet (worktree, HEAD)

```
verifier: PASS
oracle: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
clingo: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
EXCLUDE loader: form not carried by the v0 Drools backend: Predicaat.periode (a leeg/gevuld test under a whole-period qualifier (ch. 3.9): ref executes neither the leeg fold (touched days) nor the gevuld fold (days(B)), so this backend does not answer the form either)
```

Op een groene kopie (de handwaarden als `expected`) geven de OCaml-runners
de ruwe fout: ref `EVALUATION FAULT: whole-period leeg/gevuld test is
ch. 3.9 scope, not executed here` en clingo `TRANSLATION FAULT: …`
(zelfde zin), beide rc 1. De handwaarden volgens ch. 3.9 (drempel
touched-days, jaarrooster van de kwalificatie): S1 raakt bucket 2025
op 365 dagen, 0 daarvan leeg, dus onwaar en meldpremie leeg. S2 heeft
geen segmenten, raakt geen bucket en is dus vacuüm waar: meldpremie 40.
`ref --bodemzin` tegen de 36 zinnen van de gate: `PINS 2`, geen HIT.

### Rood-eerst (Drools)

Jar uit `git archive 8b685402` (basis van de heeltijdvak-leegtest-kaart,
vóór `38bd97f0`) in /tmp, `mvn -o package`: `Tests run: 604, Failures:
0`, BUILD SUCCESS. Vergeleken met de jar van deze worktree:

```
=== OUD jar (8b685402)
-- DroolsRoodMain (rood document)
NOFAULT ran to a model without faulting
rc=1
-- exec-tak, groene kopie met de ch.-3.9-handwaarden
PLAYED 1
-- DRL-tekst-tak, groene kopie
PLAYED 1
=== NU jar
-- DroolsRoodMain (rood document)
EXCLUDE loader: form not carried by the v0 Drools backend: Predicaat.periode (…)
rc=2
-- exec-tak, groene kopie: EXCLUDE loader: … Predicaat.periode (…)
-- DRL-tekst-tak, groene kopie: EXCLUDE loader: … Predicaat.periode (…)
```

De OCaml-weigering is ouder (2026-09-11). De runners van `cc0e6a1b`
geven op dit document al `expected fault observed`, dus het rood-eerst
betreft alleen het Drools-been.

### Waarom geen `drools`-fragment

Het been pint een fragment alleen op token FAULT. Een loaderweigering
is EXCLUDE. Aangetoond met gate 8 zelf: `"drools": "Predicaat.periode"`
tijdelijk in de case, de gate gedraaid, exit 1:

```
FAIL: leegtest-heel-tijdvak — Drools did not FAULT (pinned drools="Predicaat.periode"), observed EXCLUDE:
EXCLUDE loader: form not carried by the v0 Drools backend: Predicaat.periode (…)
```

Daarom één regel in de uitsluitingslijst, direct onder die van de
tweeling, met de categorie EXCLUDE, de meting op de oude jar en de
grond. Het been controleert van die regel de categorie. Sleutel en
grond ("ref executes neither the leeg fold") bewaakt
`LeegNegatieWitnessTest` (r. 129–130), en die test draait in de
bouwstap van gate 8 zelf, want het been herbouwt de jar met de
JUnit-suite. Gemeten in fixronde 2, zie open punt 3.

### Wat het document voor ref en clingo toevoegt

Mutant in een /tmp-kopie van HEAD: de heel-tijdvak-arm in ref én in
clingo versmald tot `negatie = true`.

```
== leegnegatie-heel-tijdvak
oracle: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
clingo: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
== leegtest-heel-tijdvak
EXPECTED FAULT MISMATCH: wanted "whole-period leeg/gevuld test is ch. 3.9 scope, not executed here" in:
temporal attribute "lengte" in a scalar context is refused (ch. 3.3; no point-in-time read exists — the collapses are the only bridges)
ref rc=1
EXPECTED FAULT MISMATCH: wanted "whole-period leeg/gevuld test is ch. 3.9 scope, not executed here" in:
TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)
clingo rc=1
```

De oude tweeling laat de mutant door; deze valt erop in beide.
ref's gewone leeg-arm bewaakt `periode` niet en laat de kwalificatie
vallen; clingo komt op zijn restarm. Dat staat nu in de NOTE.

### Gates

Elk los, na `eval $(opam env)`. De gates 2–7 en 9–14 draaiden op de
eerste versie van de gate-commit (vóór een amend dat alleen de
getuigezin in de redenregel preciezer maakte; na de rebase is dat
`8f24ad3d`). Wat daarna kwam raakt alleen die redenregel, een
NOTE-alinea en de kaarten. Gate 8 draaide opnieuw op `5f314873`, gate 1
op de eindstand. 1 PASS · 2 PASS (280 bestanden, vloer 192) · 3 PASS
(280 schoon; 440 negatief, vloer 350) · 4 PASS (640 cases) · 5 PASS
(ref-suite 806; 595 cases) · 6 PASS (523 checks; 595 cases) · 7 PASS
(559; 36 overlevers) · 8 PASS (45 rood, was 43; 90 fragmenten, was 86;
geen bodemzin; Drools-been 10 gepind / 35 met reden, was 9/34) · 9 PASS
(201; 12 goldens) · 10 PASS (409 tests; 70 goldens) · 11 PASS (29/29/29)
· 12 PASS (played 182 = pin, DRL-tekst 161 = pin, JUnit 620) · 13 PASS
(131 twists, 128 replays, 25 DRL, 0 overlevers) · 14 PASS (130; 2
goldens). Gate 12 en 13 vegen alleen `corpus/golden` (`find
corpus/golden` in beide scripts) en `check-drools.sh` is onaangeraakt,
dus de pins bewogen niet.

### Beschermd pad en may-touch

- **Voorstel** `scripts/check-rood.sh`: één regel
  `leegtest-heel-tijdvak|EXCLUDE …` in de Drools-uitsluitingslijst.
  De twee commentaarblokken in dezelfde commit horen bij de
  asOfDate-kaart (zie daar).
- Verbreding volgens de aanwijzing van de maintainer: geen in de
  eerste ronde. In fixronde 1, onder mandaat van de hoofdsessie:
  **voorstel** `spec/RUNTIME-SEMANTICS.md` (één rij, `78e90ae4`) en
  `corpus/golden/jutterswet-waardemelding/NOTE.md` (`1250d15b`).
  Fixronde 1b: **voorstel** `spec/RUNTIME-SEMANTICS.md` ch. 1.2
  (`3b628a2b`). Fixronde 2: **voorstel** `scripts/check-rood.sh` (kop en
  lus van het Drools-been, zeven oudere redenen, de leegtest-regel, een
  nieuwe gevuldtest-regel), **voorstel** `spec/RUNTIME-SEMANTICS.md`
  (ch. 3.12 en ch. 1.2, drie documenten), het nieuwe document
  `corpus/rood/gevuldtest-heel-tijdvak`, en als verbreding de NOTEs van
  drie oudere rode documenten en opnieuw die van waardemelding.

### Registertekst (hoofdsessie)

`spec/CONSTRUCTS.md`, rij `— Predicaatsoort`: de slotzin "The form is
normed in ch. 3.9 but executed by no executor, and no corpus document
carries it." is na deze landing onwaar. Vervangen door:

"The form is normed in ch. 3.9 but executed by no executor. Since card
`rood-heeltijdvak-ongenegeerd` (2026-09-23) the red twins
`corpus/rood/leegtest-heel-tijdvak` and
`corpus/rood/gevuldtest-heel-tijdvak` (soort LEEG and GEVULD, `negatie:
false`, otherwise the negated twin's document and case) hold that
refusal in all three executors: gate 8 pins the ch. 3.9 sentence in ref
and the ASP backend and lists each case as a Drools loader EXCLUDE
(`Predicaat.periode`: the leg checks the category, and the backend's
`LeegNegatieWitnessTest`, which gate 8 runs when it rebuilds the jar,
the key and the ground). A jar from before the loader refusal played
green copies of both cases, with the ch. 3.9 hand values as expected, in
both branches. Three mutants of the OCaml arms, each measured on all
four documents, show what the documents add: an arm narrowed to the
negated form passes both negated documents and fails both unnegated
ones; an arm narrowed to soort LEEG passes both leeg documents and fails
both gevuld ones; and an arm that exempts exactly (GEVULD, negated)
fails only the fourth twin, `corpus/rood/gevuldnegatie-heel-tijdvak`
(GEVULD, `negatie: true`, refused by the Drools loader as
`Predicaat.negatie` on the open threshold), which closes the square
soort × negatie. Applied to the Drools negated refusal as well, that
last mutant leaves the backend's suite green and turns only that case to
NOFAULT, so for that corner the case is the only guard on the Drools
refusal."

Gecontroleerd tegen de nieuwe RUNTIME-SEMANTICS-rij (fixronde 1): die
zegt "executed by no executor" en "gate 8 pins the OCaml sentence in
both OCaml executors and lists each case as a Drools loader EXCLUDE".
De zin hierboven is daarop aangescherpt. Eerst stond er "pins that
refusal in all three executors … by an EXCLUDE entry naming
`Predicaat.periode`", en dat beloofde meer dan het been toetst.

### Open punten

Stand na fixronde 1: 1 en 2 afgehandeld, 3 gemeld en niet gekaart.

1. **Afgehandeld in `78e90ae4` (voorstel).** De gecommitte rij wijkt
   af van de voorsteltekst hieronder; de commit is maatgevend (zie
   Fixronde 1). `spec/RUNTIME-SEMANTICS.md`, tabelrij "Whole-period
   qualifier, the **leeg/gevuld** tests" (beschermd, zone spec): "no
   corpus case" en
   "implemented in the Drools runtime alone" kloppen niet meer. Voorstel:
   "specified (ch. 3.9), **executed by no executor** — the two OCaml
   executors refuse both tests by name, and since 2026-09-23 the Drools
   loader refuses them too (`Predicaat.negatie` for a negated test,
   whose threshold is open; `Predicaat.periode` for an unnegated one).
   Two red cases pin the refusals in all three executors:
   `corpus/rood/leegnegatie-heel-tijdvak` (negated) and
   `corpus/rood/leegtest-heel-tijdvak` (unnegated). The threshold clause
   the leeg test needs (touched-days rather than calendar days) is stated
   in ch. 3.9 and implemented in the Drools runtime, which the loader no
   longer lets reach it; the sibling that landed above deliberately did
   not take it along, because a clause with no witness on this side would
   be a green subset".
2. **Afgehandeld in `1250d15b`**, met een eigen formulering (zie
   Fixronde 1). `corpus/golden/jutterswet-waardemelding/NOTE.md`
   r. 47–48 (zone golden, buiten may-touch) noemde als rode tweeling
   alleen
   `leegnegatie-heel-tijdvak`. Voorstel: "… is the red twin
   `corpus/rood/leegnegatie-heel-tijdvak`, and for the unnegated test
   `corpus/rood/leegtest-heel-tijdvak`."
3. **Gemeld, niet gekaart** (gatecontractbesluit, hoofdsessie
   2026-09-23). **Gecorrigeerd in fixronde 2 (lens).** Eerst stond
   hier dat gate 8 groen blijft als `Predicaat.periode` een andere
   EXCLUDE wordt, en dat alleen de JUnit-getuige dat ziet. Dat klopt
   niet voor deze regel. Het been herbouwt de jar met `mvn -o package`,
   dus met de JUnit-suite, en `LeegNegatieWitnessTest` r. 129 bewaakt
   de sleutel. Gemeten op een /tmp-kopie van HEAD met de sleutel
   veranderd in `Predicaat.kwalificatie`:

   ```
   [ERROR]   LeegNegatieWitnessTest.de_ongenegeerde_test_onder_geheel_periode_weigert_in_beide_takken:129 exec-model refuses whole-period LEEG: form not carried by the v0 Drools backend: Predicaat.kwalificatie (…) ==> expected: <true> but was: <false>
   [ERROR] Tests run: 620, Failures: 1, Errors: 0, Skipped: 0
   [INFO] BUILD FAILURE
   ```

   Gate 8 wordt dan rood in de bouwstap van het been. Het been bewaakt
   dus de categorie, en de getuige in de bouwstap sleutel en grond. Wat
   van het punt overblijft: een regel waarvan de weigering geen
   JUnit-getuige heeft, wordt alleen op categorie bewaakt. Welke van
   de andere regels dat zijn, is niet per regel gemeten. Het voorstel
   (een optionele, machinaal getoetste sleutel per regel) blijft een
   besluit van de maintainer, want het verandert het contract van de
   gate.

### Fixronde 1 (hoofdsessie, 2026-09-23)

Twee open punten opgelost, elk met een eigen commit, onder mandaat van
de hoofdsessie:

- `78e90ae4`, **beschermd-pad-voorstel** `spec/RUNTIME-SEMANTICS.md`,
  alleen de rij "Whole-period qualifier, the **leeg/gevuld** tests" in
  ch. 3.12. Ze droeg "not executed by either OCaml executor … no corpus
  case" en "implemented in the Drools runtime alone". Nu staat er:
  door geen uitvoerder uitgevoerd, alle drie weigeren benoemd. De
  OCaml-uitvoerders doen dat in dezelfde woorden, de Drools-loader op
  `Predicaat.negatie` (drempel open, ch. 1.2) en sinds vandaag op
  `Predicaat.periode` (ref voert geen van beide vouwen uit). Twee rode
  documenten houden de weigeringen vast, en gate 8 pint de OCaml-zin
  en houdt Drools als EXCLUDE. De drempelclausule staat in de
  Drools-runtime, maar geen document bereikt haar zolang de loader
  weigert. De rest van de rij is ongewijzigd.
- `1250d15b`, `corpus/golden/jutterswet-waardemelding/NOTE.md`
  (**may-touch-verbreding**, alleen de NOTE; `ir.json` en `case.json`
  onaangeroerd). De NOTE noemt nu beide rode tweelingen, elk met de
  eigen grond.

**Gefixt in fixronde 1b (`3b628a2b`).** Buiten het mandaat van
fixronde 1 gezien en toen nog niet aangeraakt:
`spec/RUNTIME-SEMANTICS.md` r. 96 (ch. 1.2, de leeg/gevuld-rij) zei
over de genegeerde test onder heel-tijdvak "ref and both Drools
branches refuse the form by name until it is chosen". De ASP-backend
weigert de vorm sinds de clingo-leeg-negatie-kaart ook, maar de zin
noemde hem niet: onvolledig, niet onwaar. Zie Fixronde 1b.

Gates na de fixronde, elk los na `eval $(opam env)`: 1 PASS · 2 PASS
(280 bestanden, vloer 192) · 4 PASS (640 cases) · 8 PASS (45 rood; 90
fragmenten, geen bodemzin; Drools-been 10 gepind / 35 met reden) · 12
PASS (played 182 = pin, DRL-tekst 161 = pin, JUnit 620).

### Fixronde 1b (hoofdsessie, 2026-09-23)

- `3b628a2b`, **beschermd-pad-voorstel** `spec/RUNTIME-SEMANTICS.md`,
  alleen de zin in de ch.-1.2-rij over de genegeerde leegtest onder
  heel-tijdvak. Eerst stond er: "ref and both Drools branches refuse the
  form by name until it is chosen". Nu staat er: "all three executors
  refuse the form by name: ref and the ASP backend in the same words,
  because neither executes any whole-period leeg/gevuld test, and both
  Drools branches in the loader as `Predicaat.negatie`, on that open
  threshold. Red case `corpus/rood/leegnegatie-heel-tijdvak` holds the
  refusal; its twin `corpus/rood/leegtest-heel-tijdvak` holds the
  refusal of the unnegated test, which ch. 3.9 norms and no executor
  runs, ch. 3.12". "until it is chosen" is weggelaten: voor ref en de
  ASP-backend klopte dat niet. Zij weigeren ook als de drempel gekozen
  is, zolang ze de vouw niet uitvoeren.
- Gate 1 opnieuw, los na `eval $(opam env)`: PASS.
- Open punten 1 (sleuteltoets, gemeld en niet gekaart) en 2
  (ERROR-token van de replay-runner, naar de Drools-lijn) staan
  ongewijzigd.

### Fixronde 2 (lens: EERST FIXEN, 2026-09-23)

Eén blokkerende vondst en zeven adviezen. Elk punt heeft een of meer
eigen commits. De punten van deze kaart staan hier, die van de
zusterkaart in haar Result.

- **Punt 1, blokkerend** (gedeeld met de zusterkaart, details daar):
  het commentaar dat ik in `8f24ad3d` in de kop van het Drools-been
  zette, beschreef een scheiding die de code niet maakt. Hersteld in
  `662d75a1` (gate 8, voorstel), `52c642cc` (javadoc) en `3789e7ce`
  (case-schema, voorstel). **Bijvangst, zelfde mechanisme**
  (`f210c278`): zeven oudere uitsluitingsredenen in gate 8 (voorstel)
  en de NOTEs van `unknown-role-target-label`,
  `periodevenster-tijdlijnkenmerk` en `datumsetbron-somvouw`
  (may-touch-verbreding binnen `corpus/rood`) gaven als grond "FAULT is
  gereserveerd voor een echte fout bij fire". Die premisse klopt niet.
  De conclusie bleef wel waar, want het zijn allemaal EXCLUDEs. Nu staat
  er: het been pint alleen op FAULT, en een loader- of
  transpilerweigering is een EXCLUDE. Categorieën en gevallen zijn niet
  veranderd.
- **Punt 2** (`906a9321`): open punt 3 en de CONSTRUCTS-zin
  gecorrigeerd. Het been herbouwt de jar met de JUnit-suite, en
  `LeegNegatieWitnessTest` r. 129 bewaakt de sleutel. Gemeten met een
  sleutelmutant: `Tests run: 620, Failures: 1`, `BUILD FAILURE`. De
  redenregel (voorstel) zegt dat nu ook.
- **Punt 3**: de gevuld-tweeling `corpus/rood/gevuldtest-heel-tijdvak`
  (`19777ed7`). Dat is `leegtest-heel-tijdvak` met `soort: GEVULD` en
  een aangepaste `bron`, met een byte-gelijke case. Handwaarden volgens
  ch. 3.9 (days(B)): S1 365/365 waar, S2 vacuüm waar, beide meldpremie
  40. De NOTE zegt dat die case de twee drempels niet scheidt. Gemeten:

  ```
  verifier: PASS
  oracle: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
  clingo: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
  EXCLUDE loader: form not carried by the v0 Drools backend: Predicaat.periode (…)   (huidige jar)
  NOFAULT ran to a model without faulting                                            (jar van 8b685402)
  PLAYED 1 / PLAYED 1   (oude jar, exec en DRL-tekst, groene kopie S1 40, S2 40)
  PLAYED 0 / PLAYED 0   (oude jar, replay op het rode document zelf)
  ref --bodemzin: PINS 2, geen HIT
  ```

  Mutant met de OCaml-arm versmald tot `soort = Leeg`: de twee
  leeg-documenten blijven groen, dit document valt. ref geeft
  `temporal attribute "lengte" in a scalar context is refused (ch.
  3.3; …)` en clingo `TRANSLATION FAULT: condition kind not translated
  (no corpus case exercises it)`, een bodemzin. Met de arm versmald tot
  `negatie = true` vallen beide ongenegeerde documenten (rc 1 in beide
  uitvoerders). Verder: de uitsluitingsregel (`29ed49de`, voorstel), de
  RUNTIME-SEMANTICS-passages in ch. 3.12 en ch. 1.2 (`3002ac46`,
  voorstel, nu met "a negated gevuld test under the qualifier has no
  corpus case"), en de NOTE van waardemelding, de leegtest-NOTE en de
  CONSTRUCTS-zin (`d3c3a870`).
- **Punt 6** (`434f027d`): in de leegtest-NOTE "two fields changed" en
  "pins a fragment only on a FAULT". De redenregel (voorstel) en de
  NOTE maken nu onderscheid tussen de groene kopie (PLAYED 1), de rode
  run (NOFAULT) en de replay op het rode document zelf (PLAYED 0,
  opnieuw gemeten op de jar van 8b685402).

Gates na fixronde 2, elk los na `eval $(opam env)`: 1 PASS · 2 PASS
(281 bestanden, vloer 192) · 3 PASS (281 schoon; 440 negatief, vloer
350) · 4 PASS (641 cases) · 8 PASS (46 rood, vloer 26; 92 fragmenten,
geen bodemzin; Drools-been 10 gepind / 36 met reden) · 12 PASS (played
182 = pin, DRL-tekst 161 = pin, JUnit 620). De pins van gate 12 bewogen
niet.

Nog open, ongewijzigd: open punt 3 (de sleuteltoets, in gecorrigeerde
vorm; gemeld, niet gekaart). Waargenomen, niet gedaan: de genegeerde
gevuld-test onder heel-tijdvak heeft geen corpusdrager en ook geen
JUnit-getuige; alleen de proefmeting van de heeltijdvak-leegtest-kaart
dekt hem. De twee mutanten hierboven laten zien dat de drie documenten
elke versmalling langs soort of negatie vangen; een arm die precies
(GEVULD, genegeerd) uitzondert, vangen ze niet.

### Fixronde 3 (hoofdsessie, 2026-09-23)

**Rebase.** De branch staat op main `3a140fe4` (28 commits, geen
conflict; main raakte `corpus/rood` en `check-rood.sh` niet). Gate 8
direct daarna: PASS, 46 rood. Commit-hashes in dit Result verwijzen
naar de branch na de rebase. De oude hashes zijn per commitonderwerp
omgezet, 51 plaatsen in de twee rood-Results.

**De vierde tweeling, `corpus/rood/gevuldnegatie-heel-tijdvak`**
(`80cf5f32`). Dat is `leegnegatie-heel-tijdvak` met `soort: GEVULD` en
"niet gevuld" in de `bron`. Langs `gevuldtest-heel-tijdvak` afgeleid
(negatie op true, "niet" erbij) geeft het dezelfde bytes, en de case
delen alle vier. De vorm stond in fixronde 2 als waargenomen gat; dat
gat is nu dicht. Handwaarden: de drempel is open, maar hier geven
beide kandidaten hetzelfde (S1 0 van 365 dagen, dus onwaar en leeg; S2
vacuüm waar, dus 40). Gemeten:

```
verifier: PASS
oracle: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
clingo: expected fault observed (whole-period leeg/gevuld test is ch. 3.9 scope, not executed here)
EXCLUDE loader: form not carried by the v0 Drools backend: Predicaat.negatie (a negated leeg/gevuld test under a whole-period qualifier (ch. 3.9): the threshold of the negated test is not normed)
ref --bodemzin: PINS 2, geen HIT (36 zinnen)
```

**Rood-eerst, met een correctie op de opdracht.** Op de jar van
8b685402 speelde deze vorm niet, want de loader weigerde hem daar al:
`Predicaat.negatie` bestaat sinds `2de5d935`/`ff4b8300`, en die zijn
voorouders van 8b685402. De opdracht verwachtte "NOFAULT / PLAYED,
zoals bij de andere drie", maar gemeten is dit:

```
=== jar van 8b685402
EXCLUDE loader: … Predicaat.negatie …   (rode run, beide groene kopieën, beide takken, replay)
=== jar van 1ef9d818 (ouder van 2de5d935, vóór de Drools-loader negatie las)
-- DroolsRoodMain (rood document)
NOFAULT ran to a model without faulting
-- groene kopie (hand: S1 leeg, S2 40): exec / DRL-tekst
FAIL case1.json: verwacht leeg, werkelijk 40
FAIL case1.json: verwacht leeg, werkelijk 40
-- groene kopie (ongenegeerd: S1 40, S2 40): exec / DRL-tekst
PLAYED 1
PLAYED 1
-- replay op het rode document zelf: exec / DRL-tekst
PLAYED 0
PLAYED 0
```

Vóór `2de5d935` gaven beide takken dus stil het ongenegeerde antwoord.
Dat is sterker dan "speelde": een aantoonbaar fout antwoord. Voor
`leegnegatie-heel-tijdvak` geldt hetzelfde: ook die weigerde op
8b685402 al. Zijn NOTE (van de clingo-leeg-negatie-kaart) claimt geen
ander rood-eerst.

**De mutatieproef voor precies deze hoek.** Een mutant sluit (GEVULD,
genegeerd) uit in de ref-arm, in de clingo-arm (beide:
`Predicaat { soort = Leeg; … } | Predicaat { soort = Gevuld; …;
negatie = false; … }`) en in de Drools-negatieweigering (`periode !=
null && soort == LEEG`). ref en clingo op de vier documenten:

```
== leegnegatie / leegtest / gevuldtest: ref en clingo: expected fault observed (ongewijzigd)
== gevuldnegatie
   ref:    temporal attribute "lengte" in a scalar context is refused (ch. 3.3; …)
   clingo: TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)
```

De Drools-mutantjar (`mvn -o package`: `Tests run: 620, Failures: 0`,
BUILD SUCCESS) geeft op de drie tweelingen ongewijzigd EXCLUDE, en op
dit document `NOFAULT ran to a model without faulting`. Gate 8 zelf
gedraaid op die mutantboom (een losse `git init` in /tmp, geen
worktree): exit 1, precies drie FAIL-regels, alle drie op dit
document:

```
FAIL: corpus/rood/gevuldnegatie-heel-tijdvak/case1.json — ref did not observe the expected fault:
FAIL: corpus/rood/gevuldnegatie-heel-tijdvak/case1.json — clingo did not observe the expected fault:
FAIL: reason drift — gevuldnegatie-heel-tijdvak is listed as EXCLUDE but Drools now reports NOFAULT; correct the reason category to match the observed status:
```

Voor deze hoek is het rode document dus de enige bewaker van de
Drools-weigering. De JUnit-getuige dekt de genegeerde weigering alleen
voor LEEG. Dat staat in de NOTE, in de uitsluitingsregel (`e47cc971`,
voorstel), in de RUNTIME-SEMANTICS-rij ("for the negated gevuld test
the listed category is the only guard", `2a4acdf6`, voorstel) en in de
CONSTRUCTS-zin hierboven.

**Vier documenten, het vierkant soort × negatie.** De RUNTIME-SEMANTICS-
passages in ch. 3.12 en ch. 1.2 (`2a4acdf6`, voorstel), de NOTEs van
waardemelding, leegtest en gevuldtest, de voorgestelde CONSTRUCTS-zin
en één nieuwe zin in de NOTE van `leegnegatie-heel-tijdvak`
(may-touch-verbreding binnen `corpus/rood`) noemen nu de vier
documenten (`07ad33b2`).

Gates na fixronde 3, elk los na `eval $(opam env)`: 1 PASS · 2 PASS
(282 bestanden, vloer 192) · 3 PASS (282 schoon; 441 negatief, met het
negatief van de meegenomen WF-022-kaart) · 4 PASS (642 cases) · 8 PASS
(47 rood, vloer 26; 94 fragmenten, geen bodemzin; Drools-been 10 gepind
/ 37 met reden) · 12 PASS (played 182 = pin, DRL-tekst 161 = pin, JUnit
620).

Nog open, ongewijzigd: open punt 3 (de sleuteltoets; gemeld, niet
gekaart). Het gat van de genegeerde gevuld-test is dicht.
