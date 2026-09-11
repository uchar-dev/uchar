# De twee ongelijke plekken tussen de Drools-takken

Goal: de twee plaatsen waar de DRL-tekst-tak een vraag NIET stelt die de
exec-model-tak wél stelt, gelijktrekken — met dezelfde gedeelde afleiding en
dezelfde redenzin, en met een rood-eerst-getuige per plek. Beide zijn gemeten en
geen van beide wordt door het corpus geraakt, dus dit is geen bugfix onder druk
maar het sluiten van twee gaten voordat een golden erin valt.

## Waar dit vandaan komt

De kaart `20260910-drools-ongepoorte-rollezers.md` poortte zes lezers van het
§13.3.9 referentieveld in beide takken. Bij het meten daarvan kwamen twee
plekken boven die op geen enkele lijst stonden.

### A. `DrlTranspiler.scope(...)` stelt de gedeelde vraag niet

`private Frag scope(Objecttype elem, Objecttype subject, Expr.Functie f)`
(~r. 3071, eindigend op
`Frag.todo("aggregatie-bereik", arg != null ? arg.onderwerpBron() : f.bron().snippet())`)
leidt de aggregatie-join af — eerst via de geschreven elementnaam
(`verbindendeRolViaElementnaam`), dan via het paar (`verbindendeRol`) — en emit
`<rol> == $subject` zodra één van beide een rol oplevert. Een rol is nog geen
veld: `FactModelGenerator.leden` leidt er alleen één af als de rol enkelvoudig
is, uniek bereikbaar, en haar naam niet door een gedeclareerd lid is ingenomen.
De exec-tegenhanger (`ExecModelTranspiler.aggregatie`) vraagt dat sinds
2026-08-30 (`heeftReferentieVeld` + `aggregatieReden`); deze tak vraagt niets.

**Kanaalrisico: gemeten nul.** De hele golden-verzameling door de DRL-tak
gehaald (167 goldens met case-bestanden, `--drl`): **0 kale
`EXCLUDE drl compile`-uitkomsten**. De twee goldens die dit gat vóór die kaart
lieten zien — `anchor/jutterswet-erflijn` en `anchor/jutterswet-gemeenteberging`
— worden sindsdien door de nieuwe DRL-feitcreatiepoort eerder geweigerd, mét de
§13.3.9-zin, dus die getuige reproduceert **niet meer**. De derde kandidaat,
`anchor/jutterswet-bergingstoezicht`, markert in deze tak op een andere grond
(`TODO aggregatie SOM`) en bereikt `scope` niet.

Gevolg voor het kaartwerk: **de rood-eerst-getuige moet een synthetisch
JUnit-model zijn**, gebouwd zoals de zeven in
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/RolveldlezerWitnessTest.java`
— een golden-`ir.json` van de wire, in het geheugen gevarieerd, naar een
tijdelijk bestand, en door `CaseRunner.viaDrlTekst` gehaald. Dát is het werk;
de poort zelf is één regel.

### B. De DRL-feitcreatiepoort toetst voorwaardelijk waar de exec-poort dat niet doet

`ExecModelTranspiler.feitCreatie` weigert onvoorwaardelijk:

    if (anker == null || doel == null || fc.zetVeld() == null || fc.joinVeld() == null)
        return skip("feitcreatie (onopgelost): " + fc.naam());

`DrlTranspiler.feitCreatie` zet zijn §13.3.9-poort achter

    if (anker != null && doel != null && fc.zetVeld() != null && fc.joinVeld() != null) { … }

en laat een feitcreatie met een null anker/doel/zet/join daarna gewoon door naar
de emissie. Eén constructie, twee antwoorden: in exec een reden-dragende
weigering, in DRL niets. Hetzelfde beginsel als hierboven — één vraag in beide
takken.

**Kanaalrisico: gemeten nul.** `corpus/golden` draagt 4 feitcreaties; geen
enkele heeft een null anker, doel, `zetVeld` of `joinVeld`. Ook hier moet de
getuige dus synthetisch zijn.

## Aanpak

1. **A**: één `navVeld("aggregatie-join", elem, rol, subject)` in `scope`, vóór
   `Frag.ok(...)`, in de vorm van de zes poorten van de vorige kaart — geen
   nieuwe afleiding, geen nieuwe redenzin. Bij een weigering de bestaande
   marker-route (`Frag.todo`), zodat `navReden` de generieke tekst aanscherpt.
   Let op de VOLGORDE: de exec-tak kent bovendien een aparte,
   CONSERVATIEVE weigering voor de enkelvoudige elementrol met meervoudige
   zusterrol; die niet overnemen zonder eigen meting — hij verschuift kanalen
   (zie de waarschuwing in `aggregatieReden`).
2. **B**: de voorwaarde om de DRL-poort laten vallen en de null-gevallen even
   luid weigeren als exec dat doet, met de bestaande marker-vorm
   (`// onopgelost feitcreatie (onopgelost)` + `eval(true)`).
3. Per plek een rood-eerst-getuige in `RolveldlezerWitnessTest` of een
   zusterklasse: eerst rood draaien met de poort weg, dan groen.

## Acceptance

- Beide plekken gepoort, elk met een getuige die zonder de poort rood is.
- Gate 12 groen met `EXPECTED_PLAYED` **ongewijzigd** (de DRL-tak is geen
  gate-12-pad, dus dit moet triviaal gelden — bevestig het, neem het niet aan).
- Gate 13 groen met de DRL-vloer ongewijzigd (nu: 13 PLAYED door de tekst-tak).
- Gate 11 groen (29/29 PLAYED).
- De JUnit-teller stijgt; vloer `EXPECTED_MIN_TESTS` ongemoeid.

May touch: `backends/drools/**`, `backlog/`.

## Out of scope

- De CONSERVATIEVE zusterrol-weigering in `aggregatie` / `toOneAantalReader`
  opheffen: die verschuift gate-12-kanalen en heeft een eigen meting nodig.
- De exec-tak: die stelt beide vragen al.
- `ref/`, `backends/clingo`, `spec/verifier/**`.

## Result (2026-09-11)

Beide plekken gepoort in `DrlTranspiler`, elk met rood-eerst-getuigen in een
nieuwe zusterklasse `TakgelijkheidWitnessTest` (vijf tests: twee per poort, plus
één tegenproef). De DRL-tak stelt nu bij de aggregatie-join dezelfde gedeelde
vraag als de exec-tak, en weigert een onopgeloste feitcreatie onvoorwaardelijk.

### De sweep — de nulclaim hermeten op deze HEAD

De hele golden-verzameling met case-bestanden door de DRL-tak, vóór de poorten:

    goldens met cases : 168   (de kaart mat er 167; er is er één bijgekomen)
    PLAYED 87   EXCLUDE 71   FAIL 8   ERROR 2
    kale "EXCLUDE drl compile": 0

De nulclaim van de kaart houdt stand. Ná de poorten is de verdeling
**identiek** — 87 / 71 / 8 / 2, en nog steeds 0 kale compile-uitkomsten. Precies
twee regels in de sweep veranderen, en alleen van REDEN, niet van kanaal:

    anchor/jutterswet-erflijn
      voor : MARKER feitcreatie: erf de directe opvolger — feitcreatie: het
             beanmodel leidt voor de rol 'erfvolger' op Wachter geen
             referentieveld naar Wachter af (§13.3.9 opslagrichting)
      na   : MARKER regel: erfvolgeraantal van een wachter — aggregatie-join:
             het beanmodel leidt voor de rol 'erflater' op Wachter geen
             referentieveld naar Wachter af (§13.3.9 opslagrichting)

    anchor/jutterswet-gemeenteberging
      voor : MARKER feitcreatie: berging door de kustgemeente — feitcreatie: …
      na   : MARKER regel: aantal bergende gemeenten van een vondst —
             aggregatie-join: het beanmodel leidt voor de rol 'geborgen vondst'
             op Gemeente geen referentieveld naar Vondst af (…)

Dat zijn de twee goldens die de kaart noemt als "reproduceert niet meer": ze
weigerden sinds de vorige kaart bij de feitcreatie en weigeren nu één constructie
eerder, bij de aggregatie-join. Beide blijven EXCLUDE. Ze laten wel zien dat de
nieuwe poort door echt corpusmateriaal wordt geraakt — de getuigen moesten
synthetisch zijn omdat geen golden de plek met een ONTBREKEND veld bereikt en
overleeft, niet omdat de plek onbereikbaar is.

### A. `DrlTranspiler.scope` — de aggregatie-join

Eén regel: `if (rol != null && navVeld("aggregatie-join", elem, rol, subject))`
vóór `Frag.ok(...)`; een weigering valt door naar de bestaande
`Frag.todo("aggregatie-bereik", …)`, waar `navReden` de generieke tekst
aanscherpt. De join die deze tak emit is `<rol> == $subject`, dus het veld wordt
op het ELEMENT gelezen — dat is de zijde waaraan de vraag wordt gesteld.

Twee synthetische modellen uit `jutterswet-vondsttelling` van de wire, allebei
verifier-schoon, ref-schoon én clingo-schoon, en allebei gemeten vóór de poort:

**A1 — onafgeleid** (beide rollen van `vondstaanmelding` meervoudig, dus geen
veld aan weerszijden), met de eigen case van de golden:

    verifier: PASS
    ref     : oracle: PASS (7 expectations)
    clingo  : clingo: PASS (7 expectations)
    exec    : EXCLUDE transpiler SKIP: regel: aantal aangemelde vondsten —
              regel (de aggregatie-join heeft geen beanveld — … beide zijden
              meervoudig (§13.3.9 opslagrichting))
    DRL vóór: EXCLUDE drl compile: [Message [id=1, … text=Unable to Analyse
              Expression aanmelder == $subject: [Error: unable to resolve method
              using strict-mode: i…
    DRL ná  : EXCLUDE drl-transpiler MARKER: regel: aantal aangemelde vondsten —
              aggregatie-join: het beanmodel leidt voor de rol 'aanmelder' op
              Vondst geen referentieveld naar Jutter af (§13.3.9 opslagrichting)

Deze vorm heeft een scheidsrechter: ref én clingo antwoorden allebei, dus een
golden van deze vorm zou kunnen bestaan. De poort levert hier de diagnose: een
kale compilerfout die rol noch lid noch limiet noemde en het hele pakket
meesleurde, wordt de zin die de exec-tak al drukte.

**A2 — verdrongen** (een gedeclareerd Numeriek `aanmelder` op Vondst), met een
case zonder rollen, zodat niet de case-invoerpoort maar de lowering antwoordt:

    verifier: PASS
    ref     : oracle: PASS (2 expectations)
    clingo  : clingo: PASS (2 expectations)
    exec    : EXCLUDE transpiler SKIP — de §13.3.9-naamdelingszin
    DRL vóór: PLAYED 1
    DRL ná  : EXCLUDE drl-transpiler MARKER: regel: aantal aangemelde vondsten —
              aggregatie-join: de rol 'aanmelder' op Vondst deelt haar naam met
              een gedeclareerd lid — het beanveld draagt dat attribuut/kenmerk en
              niet de referentie (leden(): attribuut > kenmerk > referentie), dus
              de koppeling naar Jutter heeft geen veld (§13.3.9 opslagrichting)

Dit is de scherpste helft: het veld BESTAAT en draagt een `BigDecimal`, dus
`aanmelder == $subject` compileert in de mvel-dialect en matcht nooit. De tak
ANTWOORDDE — één constructie, twee antwoorden, en de stille was deze. De waarde
die hij gaf klopte toevallig (er wordt geen koppeling gelegd, dus ref telt ook
nul), maar alleen omdat `CaseRunner`'s eigen poort weigert zo'n koppeling te
schrijven.

### B. De DRL-feitcreatiepoort — de onvoorwaardelijke weigering

De voorwaarde `if (anker != null && doel != null && zetVeld != null &&
joinVeld != null)` is vervallen; ervóór staat nu dezelfde onvoorwaardelijke
weigering die `ExecModelTranspiler.feitCreatie` doet, in de bestaande markervorm
(`// onopgelost feitcreatie (onopgelost)` + `eval(true)`).

**Correctie op de kaart.** De kaart schrijft dat zo'n feitcreatie "gewoon door
naar de emissie" ging en in DRL "niets" opleverde. Gemeten is dat niet stil maar
een onherleidbare crash:

    null doel , DRL vóór: ERROR NullPointerException:
                          Cannot read field "naam" because "doel" is null
    null anker, DRL vóór: ERROR NullPointerException:
                          Cannot read field "naam" because "anker" is null
    null zetVeld / joinVeld, DRL vóór: ERROR NullPointerException:
                          Cannot invoke "String.trim()" because "naam" is null

De melding noemt de constructie niet — "naam is null" kan overal vandaan komen —
en hij sleurt de hele transpile mee, waar de exec-tak die ene constructie bij
naam weigert en doorgaat. De ongelijkheid is dus groter dan de kaart dacht, niet
kleiner.

**Welke van de vier posities welgevormd is.** Gemeten met de verifier: WF-034
dekt `zetVeld`, `joinVeld` en per tussenstap `veld`/`type`, maar NIET `anker` en
`doel`:

    fc-null-zetVeld : WF-034 FAIL /feitCreaties/0/zetVeld: the created
                      relation's bean field is absent: …
    fc-null-joinVeld: WF-034 FAIL /feitCreaties/0/joinVeld: the final hop's
                      bean field is absent: …
    fc-null-anker   : PASS
    fc-null-doel    : PASS

De twee getuigen staan dus op `doel` en `anker`, de welgevormde helft. `doel` is
bovendien de bereikbare: ref draait dat document tot een model uit
(`oracle: PASS (10 expectations)`), dus een golden van die vorm zou kunnen
bestaan; bij `anker` faalt ref (`EVALUATION FAULT: missing FeitCreatie.anker in
IR node`) en clingo faalt bij allebei (`TRANSLATION FAULT: missing
FeitCreatie.doel` resp. `… anker`). Beide gepoort — het is één constructie en
één vraag, en `zetVeld`/`joinVeld` vallen er onder dezelfde regel bij, ook al kan
geen welgevormd document ze dragen.

    DRL ná (beide): EXCLUDE drl-transpiler MARKER: feitcreatie: vakbeseining
                    door het kustvak — onopgelost feitcreatie (onopgelost):
                    vakbeseining door het kustvak
    exec (beide)  : EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het
                    kustvak — feitcreatie (onopgelost): vakbeseining door het
                    kustvak

### Rood-groen

De vijf getuigen eerst rood gedraaid met `DrlTranspiler.java` teruggezet: **4
failures/errors, 1 pass**. De ene die groen bleef is de tegenproef, wat klopt.
Elke failure is de gemeten fout zelf:

    de_drl_aggregatiejoin_weigert_een_join_zonder_veld
        AssertionFailedError: drl compile: … Unable to Analyse Expression
        aanmelder == $subject … ==> expected: <true> but was: <false>
    de_drl_aggregatiejoin_weigert_het_verdrongen_veld
        AssertionFailedError: de DRL-tak weigert de vorm met een reden:
        ==> expected: <EXCLUDE> but was: <PLAYED>
    de_drl_feitcreatie_weigert_een_onopgelost_doel
        NullPointerException: Cannot read field "naam" because "doel" is null
    de_drl_feitcreatie_weigert_een_onopgelost_anker
        NullPointerException: Cannot read field "naam" because "anker" is null

Daarna groen: 5/5. De tegenproef (`de_ongestoorde_aggregatie_speelt_nog`) telt
hier zwaarder dan bij de vorige kaart: `scope` ligt op het pad van ELKE
aggregatie die deze tak verlaagt, dus een vraag die één streepje te streng staat
zou de tak leegmaken. De ongestoorde golden speelt in beide takken.

### Kanaalcontrole en gates

- gate 1: PASS.
- gate 11: PASS — 29/29 cases PLAYED door Drools, 0 per-vorm EXCLUDE, 0
  FAIL-of-ERROR; ref 29/29, clingo 29/29. Het onafhankelijke
  over-afwijzingssignaal beweegt niet.
- gate 12: PASS — **played 123, gelijk aan de gepinde `EXPECTED_PLAYED`**; 44
  auto-excluded, 1 waardedivergentie, 2 no-case; kanaalvloer 47 niet-PLAYED
  goldens bij naam. JUnit **308** tests (was 303), vloer 239 ongemoeid.
  `scripts/check-drools.sh` is niet aangeraakt.
- gate 13: PASS — 83 getwiste annotaties over 16 goldens (vloer 74), 74 replays,
  **14 daarvan PLAYED door de DRL-tekst-tak** (vloer 13), 0 survivors. De tak
  speelt er één MEER dan de 13 die de vorige kaart mat; dat komt van de golden
  die er sinds die meting bij is gekomen, niet van deze poorten — de sweep
  hierboven telt vóór en ná even veel PLAYED.
- De sweep zelf is de brede kanaalcontrole van de DRL-tak, die geen enkele gate
  draait: verdeling vóór en ná identiek, 0 kale compile-uitkomsten.

### Wat bewust niet bewoog

- De CONSERVATIEVE zusterrol-weigering in `ExecModelTranspiler.aggregatie` (en
  haar tegenhanger in `toOneAantalReader`) is NIET overgenomen in de DRL-tak en
  niet opgeheven. De kaart houdt dat expliciet buiten scope; hij verschuift
  gate-12-kanalen en heeft een eigen meting nodig. Gevolg, vastgelegd: de takken
  zijn nu gelijk in de VELD-vraag en blijven ongelijk in die conservatieve
  weigering — de exec-tak weigert daar een vorm die de DRL-tak lowert.
  `anchor/jutterswet-gemeenteberging` is daar het levende voorbeeld van: exec
  weigert hem op de zusterrolgrond, de DRL-tak op de veldgrond.
- `ref/`, `backends/clingo`, `spec/verifier/`, `corpus/golden`, `scripts/`,
  `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `spec/IR-SPEC.md`: niet aangeraakt. ref,
  clingo en de verifier zijn alleen GEDRAAID, als meetinstrument.
- `RolveldlezerWitnessTest` is niet gewijzigd: die klasse is de lezersaudit
  BINNEN elke tak en die is afgesloten; deze twee zijn ongelijkheden TUSSEN de
  takken en krijgen daarom een eigen klasse.

### Voorstel op een beschermd pad

`spec/CONSTRUCTS.md`, rij 4, in de **Landed (2026-09-11)**-alinea. De zin

> Twee ongelijkheden TUSSEN de takken bleven over en zijn gekaart
> (`backlog/new/20260911-drl-scope-en-nulveld-poort.md`);

wordt met deze kaart een dode verwijzing. Voorgestelde vervanging (één zin):

> Twee ongelijkheden TUSSEN de takken bleven over en zijn op 2026-09-11 gesloten
> (`backlog/done/20260911-drl-scope-en-nulveld-poort.md`): de DRL-tak stelt de
> gedeelde veldvraag nu ook bij de aggregatie-join, en weigert een onopgeloste
> feitcreatie onvoorwaardelijk zoals de exec-tak dat doet;
