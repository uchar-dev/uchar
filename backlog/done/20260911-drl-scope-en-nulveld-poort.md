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
nieuwe zusterklasse `TakgelijkheidWitnessTest`. De DRL-tak stelt nu bij de
aggregatie-join dezelfde gedeelde vraag als de exec-tak, en weigert een
onopgeloste feitcreatie onvoorwaardelijk.

In de fixronde kwam er een DERDE plek bij die dezelfde vorm heeft en die de kaart
niet kende: de VIJFDE wire-nullable positie van de feitcreatie, `naam` (sectie C).
Zeven getuigen in totaal: twee per poort, één tegenproef, en twee voor `naam`.

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

Die twee redenwijzigingen raken geen gate-pad: **gate 12 meet de EXEC-tak** (hij
geeft geen vlag mee) en de DRL-tak komt er niet in voor. `EXPECTED_CHANNEL`
vergelijkt bovendien alleen het kanaalveld, niet de reden, dus zelfs een
reden-wijziging in de exec-tak zou de vloer niet bewegen. De sweep hierboven is
het enige oog dat deze tak überhaupt over de volle corpusbreedte bekijkt; gate 13
kijkt ernaar op de 14 goldens van zijn eigen twist-verzameling.

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

### C. De vijfde nullable positie: `naam` (fixronde)

`ResolvedFeitCreatie.naam` is `["string","null"]` **én** required, net als de vier
van sectie B, en **geen enkele welgevormdheidsregel dekt hem**: WF-034 gaat over
`zetVeld`, `joinVeld` en de tussenstappen, en WF-094 clausule (d) — die precies
deze "null in plaats van een naam"-klasse sluit — geldt voor de vijf GESLEUTELDE
declaratietabellen (`objecttypen`, `parameters`, `eenheidstelsels`, `feittypen`,
`dimensies`), terwijl `feitCreaties` een LIJST is. Gemeten op
`jutterswet-postwacht` met `naam: null`:

    verifier: PASS
    ref     : oracle: PASS (10 expectations)
    clingo  : clingo: PASS (10 expectations)
    exec    : PLAYED 1   — de null gaat rechtstreeks de regelkop in
    DRL vóór: ERROR NullPointerException: Cannot invoke
              "String.replace(java.lang.CharSequence, java.lang.CharSequence)"
              because "naam" is null

Drie executors draaien dit document, dus het gelijkgetrokken antwoord is hier
SPELEN, niet weigeren: een weigering zou een limiet verzinnen die de wire niet
heeft. De DRL-tak heeft nu één naamloze terugval voor alle drie de plekken waar
`fc.naam()` langskwam (de twee markerroutes en de regelkop), in de vorm van de
knoopsoort-terugval van de loader-klem.

**Welk regelgedrag exec vertoont, en dat de tak dat nu ook doet.** `PLAYED` alleen
zegt niets; de case moet de creatie ZIEN. Dat doet hij: `V1.vakseinaantal` telt de
seinen van een kustvak via de GECREËERDE `vakbeseining`-relatie, en 2 is alleen
bereikbaar als de naamloze regel gevuurd heeft. Mutatieprobe 2 → 99:

    exec : FAIL attribuut vakseinaantal: verwacht 99, werkelijk 2
    DRL  : FAIL attribuut vakseinaantal: verwacht 99, werkelijk 2

**Een flank die de terugval zelf opende, en meteen dicht.** Een DRL-pakket weigert
dubbele regelnamen, dus één VLAKKE terugvalnaam maakte van twee naamloze
feitcreaties een nieuwe ongelijkheid:

    DRL (vlakke terugval) : EXCLUDE drl compile: … Duplicate rule name:
                            <unnamed ResolvedFeitCreatie>
    exec                  : PLAYED 1

Dat zou de gesloten ongelijkheid door een andere vervangen. De terugval draagt
daarom de WIRE-POSITIE van de constructie (`<unnamed ResolvedFeitCreatie 0>`), de
enige onderscheider die een null-naam overlaat; de lus die de feitcreaties emit
geeft die index mee. Beide takken spelen daarna beide creaties. De sweep vóór en
ná deze wijziging is **byte-identiek** (`diff`: geen enkel verschil), dus voor een
feitcreatie mét naam verandert er niets.

**Gemeld, niet gekaart.** `naam: null` op `feitCreaties` valt buiten WF-094
clausule (d) omdat die clausule de vijf gesleutelde tabellen noemt. Dit is
dezelfde klasse — schema-geldig, verifier-schoon, ref- en clingo-schoon, en een
`NullPointerException` in het identifier-contract van de bean-uitvoerder — die
clausule (d) daar sloot. Een uitbreiding van WF-094 (of een eigen regel) naar de
naamdragende LIJST-constructies is daarmee een kandidaat voor een latere
WF-ronde. Deze kaart raakt `spec/` niet aan en kaart het niet; het staat hier
gemeld.

### Rood-groen

De zeven getuigen eerst rood gedraaid met `DrlTranspiler.java` teruggezet.

**De twee poorten** (vijf getuigen, de `naam`-fix nog niet geschreven): **4
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

**De `naam`-terugval** (de twee nieuwe getuigen, alleen die fix teruggezet, de
twee poorten blijven staan): **2 errors, 5 pass** — de vijf van hierboven blijven
groen, wat bevestigt dat de derde plek los staat van de eerste twee:

    de_drl_feitcreatie_speelt_een_naamloze_creatie
        NullPointerException: Cannot invoke "String.replace(…)" because
        "naam" is null
    twee_naamloze_creaties_botsen_niet_op_hun_terugvalnaam
        NullPointerException: Cannot invoke "String.replace(…)" because
        "naam" is null

De tweede daarvan is ook rood tegen een VLAKKE terugval, en dan met de andere
fout (`Duplicate rule name`) — daarom staat hij er.

Daarna groen: 7/7. De tegenproef (`de_ongestoorde_aggregatie_speelt_nog`) telt
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
  goldens bij naam. JUnit **310** tests (was 303), vloer 239 ongemoeid.
  `scripts/check-drools.sh` is niet aangeraakt. Deze gate meet de EXEC-tak, dus
  geen van de drie wijzigingen ligt op zijn pad; hij is hier het
  over-afwijzingssignaal, niet de meting van het werk.
- gate 13: PASS — 83 getwiste annotaties over 16 goldens (vloer 74), 74 replays,
  **14 daarvan PLAYED door de DRL-tekst-tak** (vloer 13), 0 survivors. De tak
  speelt er één MEER dan de 13 die de vorige kaart mat; dat komt van de golden
  die er sinds die meting bij is gekomen, niet van deze poorten — de sweep
  hierboven telt vóór en ná even veel PLAYED.
- De sweep zelf is de brede kanaalcontrole van de DRL-tak, die geen enkele gate
  draait: verdeling vóór en ná identiek, 0 kale compile-uitkomsten. Na de
  `naam`-terugval is de sweep bovendien byte-identiek aan die van vóór de
  fixronde.

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
  BINNEN elke tak en die is afgesloten; deze drie zijn ongelijkheden TUSSEN de
  takken en krijgen daarom een eigen klasse.
- De WF-kant van sectie C: `naam: null` op `feitCreaties` is gemeld en NIET
  gekaart. Een uitbreiding van WF-094 clausule (d) naar de naamdragende
  lijst-constructies is een beschermd-padbeslissing en hoort in een WF-ronde, niet
  in een backend-kaart. De backend is nu null-veilig; dat vervangt de wire-regel
  niet, precies zoals de loader-terugval van 2026-09-10 clausule (d) niet verving.
- De exec-tak is op geen enkel punt gewijzigd — ook niet waar hij een null
  regelnaam rechtstreeks de Drools-regelkop in geeft. Het gelijktrekken is
  eenzijdig gebeurd, in de tak die crashte.

### Voorstel op een beschermd pad

`spec/CONSTRUCTS.md`, rij 4, in de **Landed (2026-09-11)**-alinea. De zin

> Twee ongelijkheden TUSSEN de takken bleven over en zijn gekaart
> (`backlog/new/20260911-drl-scope-en-nulveld-poort.md`);

wordt met deze kaart een dode verwijzing. Voorgestelde vervanging (één zin):

> Twee ongelijkheden TUSSEN de takken bleven over en zijn op 2026-09-11 gesloten
> (`backlog/done/20260911-drl-scope-en-nulveld-poort.md`), met een derde van
> dezelfde vorm erbij: de DRL-tak stelt de gedeelde veldvraag nu ook bij de
> aggregatie-join, weigert een onopgeloste feitcreatie onvoorwaardelijk zoals de
> exec-tak dat doet, en speelt een NAAMLOZE feitcreatie in plaats van erop te
> crashen;
