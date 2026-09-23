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
`corpus/rood/leegtest-heel-tijdvak` (commits `31b156b6`, `8dd7764b`).
Het is `leegnegatie-heel-tijdvak` met `negatie: false` en een `bron`
zonder "niet", met een byte-gelijke case. De Drools-regel staat in
`84ed8713` (**beschermd-pad-voorstel**, `scripts/check-rood.sh`).

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
eerste versie van de gate-commit (`3b86800c`, daarna geamendeerd tot
`84ed8713`: alleen de getuigezin in de redenregel preciezer). Wat daarna
kwam raakt alleen die redenregel, een NOTE-alinea en de kaarten. Gate 8
draaide opnieuw op `8dd7764b`, gate 1 op de eindstand. 1 PASS · 2 PASS
(280 bestanden, vloer 192) · 3 PASS (280 schoon; 440 negatief, vloer
350) · 4 PASS (640 cases) · 5 PASS (ref-suite 806; 595 cases) · 6 PASS
(523 checks; 595 cases) · 7 PASS (559; 36 overlevers) · 8 PASS (45 rood,
was 43; 90 fragmenten, was 86; geen bodemzin; Drools-been 10 gepind / 35
met reden, was 9/34) · 9 PASS (201; 12 goldens) · 10 PASS (409 tests; 70
goldens) · 11 PASS (29/29/29) · 12 PASS (played 182 = pin, DRL-tekst 161
= pin, JUnit 620) · 13 PASS (131 twists, 128 replays, 25 DRL, 0
overlevers) · 14 PASS (130; 2 goldens). Gate 12 en 13 vegen alleen
`corpus/golden` (`find corpus/golden` in beide scripts) en
`check-drools.sh` is onaangeraakt, dus de pins bewogen niet.

### Beschermd pad en may-touch

- **Voorstel** `scripts/check-rood.sh`: één regel
  `leegtest-heel-tijdvak|EXCLUDE …` in de Drools-uitsluitingslijst.
  De twee commentaarblokken in dezelfde commit horen bij de
  asOfDate-kaart (zie daar).
- Verbreding volgens de aanwijzing van de maintainer: geen in de
  eerste ronde. In fixronde 1, onder mandaat van de hoofdsessie:
  **voorstel** `spec/RUNTIME-SEMANTICS.md` (één rij, `a4fcc645`) en
  `corpus/golden/jutterswet-waardemelding/NOTE.md` (`ec77dbd3`).

### Registertekst (hoofdsessie)

`spec/CONSTRUCTS.md`, rij `— Predicaatsoort`: de slotzin "The form is
normed in ch. 3.9 but executed by no executor, and no corpus document
carries it." is na deze landing onwaar. Vervangen door:

"The form is normed in ch. 3.9 but executed by no executor. Since card
`rood-heeltijdvak-ongenegeerd` (2026-09-23) the red twin
`corpus/rood/leegtest-heel-tijdvak` (`negatie: false`, otherwise the
negated twin's document and case) holds that refusal in all three
executors: gate 8 pins the ch. 3.9 sentence in ref and the ASP backend
and lists the case as a Drools loader EXCLUDE (`Predicaat.periode`: the
leg checks the category, and the backend's `LeegNegatieWitnessTest`,
which gate 8 runs when it rebuilds the jar, the key and the ground). A jar from
before the loader refusal played the document in both branches against
the ch. 3.9 hand values, and a mutant that narrows the OCaml arms to the
negated form passes the negated twin and fails this one."

Gecontroleerd tegen de nieuwe RUNTIME-SEMANTICS-rij (fixronde 1): die
zegt "executed by no executor" en "gate 8 pins the OCaml sentence in
both OCaml executors and lists each case as a Drools loader EXCLUDE".
De zin hierboven is daarop aangescherpt. Eerst stond er "pins that
refusal in all three executors … by an EXCLUDE entry naming
`Predicaat.periode`", en dat beloofde meer dan het been toetst.

### Open punten

Stand na fixronde 1: 1 en 2 afgehandeld, 3 gemeld en niet gekaart.

1. **Afgehandeld in `a4fcc645` (voorstel).** De gecommitte rij wijkt
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
2. **Afgehandeld in `ec77dbd3`**, met een eigen formulering (zie
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

- `a4fcc645`, **beschermd-pad-voorstel** `spec/RUNTIME-SEMANTICS.md`,
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
- `ec77dbd3`, `corpus/golden/jutterswet-waardemelding/NOTE.md`
  (**may-touch-verbreding**, alleen de NOTE; `ir.json` en `case.json`
  onaangeroerd). De NOTE noemt nu beide rode tweelingen, elk met de
  eigen grond.

**Gefixt in fixronde 1b (`55601502`).** Buiten het mandaat van
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

- `55601502`, **beschermd-pad-voorstel** `spec/RUNTIME-SEMANTICS.md`,
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
