# Zelfreferentiële to-one rollen: uchar draagt ze al — pin het met een getuige-golden

Goal: **Er valt aan de dracht niets te bouwen.** De werkbank-fix `ccdf58f`
(repo `regelspraak-werkbank/toolchain`) sluit in de toolchain een gat dat
uchars eigen Drools-poort al gesloten heeft, met dezelfde beslissing en
dezelfde drie tegenpinnen. De meting hieronder laat alle vier lenzen groen
zien, mét de niet-zelfreferentiële controle ernaast. Wat er wél ontbreekt is
**corpusdekking**: geen enkele PLAYED golden vult een gewone (niet-wederkerige)
zelfgetypeerde **to-one** rol vanuit **case-invoer** en navigeert erover. Deze
kaart voegt één golden toe die dat doet, zodat het PLAYED-kanaal van gate 12 en
de vierkantsvergelijking (ref ≡ clingo ≡ Drools) die vorm bewaken. Kleine
kaart, bewust.

Geen WF-kandidaat: dit is een backend-drachtvraag, geen
welgevormdheidseigenschap van de wire. De verifier heeft hier niets te
weigeren.

> **HET DEKKINGSGAT IS GROTENDEELS GEDICHT (2026-09-10) — deze kaart is nog een
> RESTKAART.** De kern hierboven ("geen enkele PLAYED golden vult een gewone
> zelfgetypeerde to-one rol vanuit case-invoer en navigeert erover") is
> ingehaald door `20260910-reverse-rol-via-naam.md`. Gemeten op de kop van die
> tak:
>
> - `corpus/golden/jutterswet-ketenband` — zelfgetypeerd feittype `ketenband`,
>   rol `voorligger` **to-one**, in `case1` en `case3` vanuit **case-invoer**
>   gevuld en door de regel `voorliggerrang` **genavigeerd**. Alle drie de
>   casussen PLAYED in gate 12, `ref` ≡ clingo ≡ Drools. Dat is precies de
>   vorm die deze kaart wilde bewaken, in het gespeelde kanaal.
> - `corpus/golden/anchor/jutterswet-rekenkamer/case4-koppeling-op-de-gemunte-zijde.json`
>   — een **1:1**-relatie (twee to-one rollen, twee beanvelden) eenzijdig
>   geleverd, PLAYED, en de spiegelende schrijfactie is er de reden van.
>   Kruistypisch.
>
> **Wat er overblijft — de reden dat de kaart niet dicht kan:** een
> **ZELFGETYPEERDE 1:1**-relatie, eenzijdig geleverd, in het corpus. De
> ketenband-golden is 1:N; de rekenkamer-case is kruistypisch. De
> zelfgetypeerde 1:1-vorm (de bron `beide` hieronder) hangt vandaag alleen in
> `OpslagrichtingWitnessTest` — een backend-eigen unittest op een in-memory
> modelvariant, buiten de wire om. Dát is nog steeds ongepind in gates 5, 6 en
> 12. Voorstel aan de hoofdsessie: de kaart behouden en versmallen tot precies
> dat ene punt, of hem sluiten als de restdekking het niet waard wordt geacht.
> De hoofdsessie beslist bij de merge.

## Probleem

**Wat de werkbank-fix doet.** In de toolchain leidde de fact-bean-generator
zijn referentievelden per **typepaar** af: voor elk ander objecttype dat via
precies één feittype bereikbaar is, kreeg de houder een veld genoemd naar de
rol aan de andere kant. Die zoektocht slaat een zelfreferentieel feittype
volledig over — als houder en doelwit hetzelfde objecttype zijn, matchen beide
rollen en is er geen unieke uitkomst. Gevolg: de gegenereerde bean droeg voor
zo'n rol **geen veld**, terwijl de transpilers wél via de *geschreven* rolnaam
navigeren. De ExecModel-bouw stierf daarop met een ontbrekende-getter-fout.
Luid, niet stil — maar de vorm was onuitvoerbaar.

De fix voegt een tweede afleidingslus toe: voor een niet-wederkerig feittype
met twee **verschillend genoemde** rollen die allebei door hetzelfde
objecttype gevuld worden, materialiseert hij **één scalair referentieveld per
to-ONE rol**. Drie tegenpinnen, waarvan er TWEE expliciet in de commit staan:
een wederkerig feittype leidt niets af (een referentie kan niet weten welk eind
hij aanduidt) en een to-many rol leidt niets af (de opslag zit aan het verre
eind). De derde — een ambigu typepaar blijft ongedekt — staat er niet met zoveel
woorden; ze volgt impliciet uit de afleiding, die alleen een uniek oplossende
rolnaam materialiseert. De commit haalt daarnaast de gedeelde
it-fixture-bean bij en pint de drie vormen (één-op-één, één-op-veel,
wederkerig) in een nieuwe unittest. Twee buurgaten blijven volgens de
commit-tekst open: het schrijven van de omgekeerde poot van een
zelfreferentiële **to-many** rol gooit nog, en de naam-dedup
(attribuut wint van rol) kan een rolveld verdringen.

**Welke laag dit is, en wat uchars equivalent ervan is.** Dit is de
Drools-**fact-bean-materialisatielaag**: de laag die van een resolved model
Java-beans met velden afleidt. uchars wire draagt die laag niet. Waar de
toolchain een beanveld nodig heeft, draagt de v0-wire een symbolische
`RolBinding {feittype, rol}` — gemeten op de bron hieronder:

    "navigatieRol": "opvolger",
    "navigatieBindingen": [ { "node": "RolBinding",
                              "feittype": "opvolging", "rol": "opvolger" } ]

Het uchar-equivalent van de geraakte generator zit uitsluitend in de
Drools-backend, op twee plekken:

- `backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java`
  — `zelfpaarRollen()` doet precies wat de werkbank-fix doet, met precies
  dezelfde drie tegenpinnen, en `zelfpaarRol()` dekt daarnaast de wederkerige
  §13.3.9.2-vorm die de werkbank-fix uitdrukkelijk *niet* dekt. Geland op kaart
  `backlog/done/20260830-drools-zelfgetypeerd-creatiepaar.md`, met de
  afgewogen alternatieven in de javadoc.
- `backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`
  — `zetRollen()` lost een geschreven rolsleutel via de by-naam-autoriteit op
  en accepteert die **alleen** als hij een van de afgeleide zelfpaarvelden
  noemt, zodat een treffer nooit naar een setter wijst die de bean niet heeft.

Conclusie: **(b)** uit de drie mogelijke uitkomsten. De poort draagt de vorm
al; er is geen parallel gat en geen wire-kwestie. Wat de kaart oplevert is een
getuige.

**Verwantschap, niet geclaimd.** Rolresolutie-via-naam op de wire en in alle
uitvoerders is de zusterkaart over `ba75f11`. Deze kaart raakt daar aan (de
by-naam-route ís hoe `zetRollen` het veld vindt), maar claimt de
resolutiekant niet — hier gaat het uitsluitend over het
Drools-materialisatie- en replay-kanaal.

## Meting (2026-09-10)

Vier eigen bronnen door de frontend, alle vier schema-geldig tegen
`spec/schema/regelspraak-ir-v0.schema.json` en `spec/schema/uchar-case-v0.schema.json`,
en byte-identiek her-uitgestoten (`cmp` groen — gate 10's eis). Verifier, ref,
clingo en de Drools-replay-jar, in die volgorde.

**1. `zelfref` — de hoofdvorm.** `Objecttype de Wachter` met `rang` en
`opvolgerrang`; `Feittype opvolging` met `de voorganger` / `de opvolger`,
beide to-one (`Eén voorganger heeft één opvolger`); één regel:
"De opvolgerrang van een Wachter moet gesteld worden op de rang van zijn
opvolger." Case vult beide poten (W1.opvolger = W2, W2.opvolger = W1);
rangen 10 en 20. Let op: een gedeclareerd meervoud (`mv: opvolgers` op de
rol) maakt de rol niet naar-veel — de cardinaliteit komt uit de
`Eén … heeft één …`-zin, en de IR draagt hier `meervoudig: false` naast
`meervoud: "opvolgers"`.

    verifier  verifier: PASS
    ref       oracle: PASS (2 expectations)
    clingo    clingo: PASS (2 expectations)
    drools    PLAYED 1

**Mutatieproef op diezelfde case** (verwachting 20 → 99), zodat `PLAYED` niet
als lege groenheid gelezen wordt:

    drools    FAIL case_mut.json: attribuut opvolgerrang: verwacht 99, werkelijk 20

Het is dus een echte waardevergelijking: 20 en 10 komen er uit, zoals ref en
clingo ze pinnen.

**2. `beide` — één veld per to-one rol, beide richtingen.** Zelfde feittype,
nu twee regels: `opvolgerrang` over `zijn opvolger` en `voorgangerrang` over
`zijn voorganger`. Case schrijft W1.opvolger = W2 en W2.voorganger = W1.

    verifier  PASS   ref  PASS (2)   clingo  PASS (2)   drools  PLAYED 1

Dit is de directe tegenhanger van wat de werkbank-fix mogelijk maakt: beide
to-one rollen dragen hun eigen veld en zijn allebei navigeerbaar.

**3. `veel` — de to-many zusterrol.** `Eén voorganger heeft meerdere
opvolgers`, navigatie over de to-ONE kant (`zijn voorganger`), case schrijft
vanaf de veel-KANT (W2.voorganger = W1) — W1 is hier de veel-HOUDER, W2 een van
zijn opvolgers, en de geschreven rol is de to-one rol op W2.

    verifier  PASS   ref  PASS (1)   clingo  PASS (1)   drools  PLAYED 1

**4. `kruisref` — de controle.** Twee **verschillende** objecttypen
(`Baas` / `Knecht`), to-one, exact dezelfde regelvorm
("De bazenrang van een Knecht ... op de rang van zijn baas").

    verifier  PASS   ref  PASS (1)   clingo  PASS (1)   drools  PLAYED 1

Het onderscheid zelfreferentieel/niet-zelfreferentieel maakt in uchar dus
**geen verschil** — precies het verschil dat in de toolchain vóór `ccdf58f`
het onderscheid tussen bouwen en niet-bouwen was.

> **ACHTERHAALD DOOR HET BESLUIT VAN 2026-09-10 — flank A en flank B hieronder
> zijn gemeten op de GERICHTE lezing; de uitkomsten zijn OMGEKEERD.** De
> maintainer heeft vraag 13 van `20260910-reverse-rol-via-naam.md` beantwoord
> met **(i): een relatie is symmetrisch — één geleverde poot vestigt hem, en de
> tegenrol leest hem** (`RUNTIME-SEMANTICS.md` ch. 5.7a). `ref` en de
> ASP-backend spiegelen sindsdien elke tweerollige relatie op de zusterrol, en
> de Drools-case-invoer doet hetzelfde: hij lost de zusterrol via de geschreven
> naam op (1:N) én schrijft het tweede beanveld (1:1).
>
> **Flank A pint voortaan de SYMMETRISCHE lezing.** Hermeten op de bron
> `beide` met alleen de heenpoot geschreven (W1.opvolger = W2, rangnummers
> 10/20), verwachting `W2.voorgangerrang = 10`:
>
>     verifier PASS
>     ref                   oracle: PASS (4 expectations)
>     Drools exec-model     PLAYED 1
>     Drools DRL-tekst      PLAYED 1
>
> Drie uitvoerders, hetzelfde antwoord — maar het OMGEKEERDE van wat hieronder
> staat. Vóór de spiegelende schrijfactie was ditzelfde drietal
> `ref PASS / exec-model FAIL … verwacht 10, werkelijk leeg / DRL-tekst idem`;
> die divergentie is gemeten dicht. Wat de flank nog steeds scheidt is de
> ZUSTERROL van de geschreven rol: de spiegel vult `W2.voorganger`, nooit
> `W2.opvolger` (`OpslagrichtingWitnessTest`, omgezet bij het besluit).
>
> **Flank B's Drools-weigering is vervallen**: de naam-route bereikt daar het
> veld van de to-one zusterrol. Hermeet die regel voordat er iets van gepind
> wordt.

**Flank A — de omgekeerde poot wordt niet afgeleid, in geen van de drie.**
Alleen de heenpoot geschreven (W1.opvolger = W2), en dan `W2.voorgangerrang`
gevraagd:

    ref       MISMATCH: W2.voorgangerrang = leeg, expected 10
    clingo    MISMATCH: W2.voorgangerrang is underived (leeg), expected 10
    drools    FAIL case2.json: attribuut voorgangerrang: verwacht 10, werkelijk leeg

Drie uitvoerders, hetzelfde antwoord. Een gewoon zelfpaar spiegelt niet; de
wederkerige §13.3.9.2-vorm doet dat wél. **Waarom Drools hier meedoet:** dit is
de **1:1**-vorm, en daar draagt het bonenmodel TWEE velden — één per to-one rol
— zodat elke richting zijn eigen opslag heeft en een eenzijdige levering de
andere richting ook in Drools leeg laat. Bij een **1:N**-relatie is dat anders:
daar draagt het model één veld (dat van de to-one rol) en is de naar-veel-lezing
een join over datzelfde veld, zodat een eenzijdige levering van beide kanten
zichtbaar wordt en Drools VOL leest waar ref en clingo LEEG lezen. Die
divergentie is gemeten en beschreven op de zusterkaart
`20260910-reverse-rol-via-naam.md`; ze raakt deze vorm niet. Met de juiste
verwachting
(`attributeEmpty`) is deze flank groen in alle drie:

    ref  PASS (2)   clingo  PASS (2)   drools  PLAYED 1

en hij faalt onder `--noop` (`MISMATCH: W1.opvolgerrang = leeg, expected 20`),
dus gate 7 heeft er geen overlever-regel voor nodig.

**Flank B — de to-many rol schrijven vanaf de veel-houder.** W1 krijgt
`"opvolger": ["W2"]` terwijl `opvolger` to-many is:

    ref       MISMATCH: ... = leeg   (accepteert de schrijfactie, leidt niets af)
    clingo    idem
    drools    EXCLUDE role 'opvolger' (Wachter -> Wachter) reaches no to-one
              reference field: the written role name resolves to 'opvolger',
              which is TO-MANY — its storage is the far end's field, not a
              scalar reference here, and neither direction of the type pair
              derives a to-one role

Een kanaalasymmetrie, niet een verkeerd antwoord: Drools weigert de hele
golden luid en met reden, ref en clingo leveren `leeg`. Dit is uchars
tegenhanger van het buurgat dat de werkbank-commit zelf als openstaand noemt.
**Geen acute divergentie.**

**Corpusdekking, geteld 2026-09-10.** Documenten met een gewoon
(niet-wederkerig) zelfgetypeerd feittype:

| document | vorm | rolvulling | gate-12-kanaal |
| --- | --- | --- | --- |
| `corpus/golden/anchor/jutterswet-erflijn` | TWEE zelfgetypeerde feittypen: `erflijn` (beide rollen meervoudig) én `opvolging` (beide to-one) | case-invoer; `case.json` vult de to-one rol `opvolger` op W1/W2/W3 | EXCLUDE (`transpiler SKIP: aggregation join with BOTH sides plural`) |
| `corpus/golden/anchor/jutterswet-wachtersketen` | beide to-one | **objectcreatie**, `roles: {}` | PLAYED |
| `corpus/golden/synth2-budgetgrens-onder` | beide to-one | **objectcreatie**, `roles: {}` | PLAYED |
| `corpus/rood/finite-chain-over-budget` | beide to-one | — | rode zone |
| `corpus/rood/runaway-recursion` | beide to-one | — | rode zone |

De wederkerige vorm is wél vanuit case-invoer gedekt
(`corpus/golden/anchor/jutterswet-maatschap`, `corpus/golden/jutterswet-aanmelderkrediet`,
beide PLAYED), maar dat is §13.3.9.2 — de andere vorm.

**Het gat, precies.** Geen enkele **PLAYED** golden vult een gewone
zelfgetypeerde **to-one** rol vanuit **case-invoer** en navigeert erover. De
kwalificatie PLAYED is dragend, en `anchor/jutterswet-erflijn` laat zien waarom
het gat toch dicht lijkt: die golden vult de eerste helft wél — zijn `case.json`
zet de to-one rol `opvolger` van feittype `opvolging` op W1/W2/W3 — maar hij
mist beide andere helften. Geen enkele regel NAVIGEERT over `opvolging`; alle
bindingen in zijn `resolvedRegels` noemen `erfvolger`, de meervoudige rol van
het feittype `erflijn`. En de golden staat op EXCLUDE in gate 12, om die tweede
reden: de aggregatiejoin over `erflijn` met beide zijden meervoudig. Het
vierkant sluit er dus niet omheen. De enige plek waar het volledige pad vandaag
loopt is
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/OpslagrichtingWitnessTest.java`
— een backend-eigen unittest, deels op een in-memory modelvariant, buiten de
wire om. Gate 5, gate 6 en gate 13 zien die vorm nooit; het vierkant staat er
niet op gepind.

## Voorgestelde aanpak

Eén golden toevoegen, verder niets. Geen backendwijziging, geen specwijziging,
geen schemawijziging.

1. **Nieuwe golden** `corpus/golden/jutterswet-wachterschakel/` (naam vrij; hij
   hoort niet in `corpus/golden/anchor/` — dat is een beschermd pad voor
   handgeschreven waarden, en deze golden komt uit de frontend). Inhoud:
   `bron.rgs` = bron 2 (`beide`) hierboven, `ir.json` = de emitteruitvoer,
   plus twee casebestanden:
   - `case1.json` — beide poten geschreven, `opvolgerrang` en
     `voorgangerrang` gepind op absolute waarden (20 en 10). Dit is de
     hoofdgetuige.
   - `case2.json` — alleen de heenpoot geschreven. **De vraag is beantwoord:
     hij pint de SPIEGEL, niet de afwezigheid ervan** (zie hieronder).

   **Besluitafhankelijkheid: BESLIST (maintainer, 2026-09-10, vraag 13 van
   `20260910-reverse-rol-via-naam.md`).** Het antwoord is **(i): een relatie is
   symmetrisch — één geleverde poot vestigt hem, en de tegenrol leest hem**,
   normatief in `RUNTIME-SEMANTICS.md` ch. 5.7a. Wat dat voor deze twee cases
   betekent:

   - `case1.json` blijft besluitonafhankelijk: beide poten geleverd, 20 en 10.
   - `case2.json` draait om. Hij pint niet langer `attributeEmpty` op de
     zusterrol maar de **waarde die door de niet-geleverde richting komt**:
     alleen `W1.opvolger = W2` geleverd geeft `W2.voorgangerrang = 10`. Hij is
     daarmee de **spiegel-pinnende** case en de regressiedetector van het
     besluit, niet zijn tegendeel. De wachttijd is voorbij; hij mag landen.
   - **Maar niet zomaar groen in alle vier de lenzen.** Deze bron is de
     **1:1-vorm** (twee to-one rollen, dus twee beanvelden). `ref` en clingo
     spiegelen sinds het besluit; de **Drools-case-invoer doet de tweede
     schrijfactie nog niet**, dus daar leest `voorgangerrang` leeg. Zolang die
     schrijfactie er niet is, valt deze golden met zo'n case2 uit het
     PLAYED-kanaal (een waarde-divergentie, geen weigering). Twee routes,
     kies er één en zeg welke: **(a)** de spiegelende schrijfactie in
     `CaseRunner.zetRollen` erbij bouwen — dan bewegen twee assertieplekken in
     `OpslagrichtingWitnessTest` mee (`de_gewone_zelfpaarkoppeling_spiegelt_niet`
     en `dezelfde_telling_negeert_een_levering_aan_de_elementkant`, beide pinnen
     de gerichte lezing), en dat bestand is van DEZE kaart; of **(b)** case2
     voorlopig niet leveren en alleen case1 landen, met de reden hier.
2. **Gate 12** — met alleen `case1.json` komt de golden PLAYED binnen:
   `EXPECTED_CHANNEL` ongewijzigd, `EXPECTED_PLAYED` één omhoog vanaf de dan
   geldende stand (**121** sinds `20260910-reverse-rol-via-naam.md`, niet 119).
   Met een spiegel-pinnende `case2.json` erbij hangt het kanaal aan route (a)
   of (b) hierboven — meet het, neem het niet aan. Beide zijn een **beschermd
   pad**: als voorstel indienen, niet mergen.
3. **De overige gates bewegen niet.** Gate 5's `EXPECTED_MIN_CASES` (427) en
   gate 6's telling zijn ondergrenzen — groei vraagt geen bijstelling. Gate 7
   is gemeten: beide cases falen onder `--noop`, dus geen overlever-regel.
   Gate 2, 4 en 10 zijn gemeten groen op deze artefacten.
4. **Gate 13, expliciete keuze.** De `Numeriek`-annotaties in deze bron dragen
   `eenheid: null`, dus de golden valt **buiten** de twistverzameling en
   verschuift geen enkele gate-13-telling. Wil je de vorm ook door de
   annotatie-onverschilligheidsproef halen, geef `rang` dan een eenheid — dan
   komt hij in de twistverzameling en moet de DRL-tak-vloer opnieuw gemeten
   worden. Dat is extra werk zonder aangetoond risico; **default: geen
   eenheid**, en de reden hier vastleggen.
5. **Flank B niet in een golden.** Een case die de to-many rol vanaf de
   veel-houder schrijft, laat de hele golden uit het PLAYED-kanaal vallen
   (Drools EXCLUDE, zie meting). Als die flank gepind moet worden, hoort hij
   als extra assertie in `OpslagrichtingWitnessTest`, niet in het corpus.

## Acceptance

- `corpus/golden/jutterswet-wachterschakel/` bestaat met `bron.rgs`,
  `ir.json` en twee casebestanden; de `ir.json` is frontend-uitvoer en
  her-emitteert byte-identiek.
- De golden speelt in **alle vier** de lenzen met dezelfde absolute
  waarden: verifier PASS, ref PASS, clingo PASS, Drools PLAYED — en de
  mutatieproef (één verwachting verdraaid) faalt aantoonbaar in de
  Drools-replay, zodat PLAYED geen lege groenheid is.
- `scripts/check-drools.sh`: `EXPECTED_PLAYED` één omhoog vanaf de dan
  geldende stand (121 sinds `20260910-reverse-rol-via-naam.md`),
  `EXPECTED_CHANNEL` ongewijzigd — **als voorstel** ingediend (beschermd pad).
  Met een spiegel-pinnende `case2.json` erbij: eerst meten, zie punt 1.
- De besluitafhankelijkheid van `case2.json` is BESLIST en verwerkt: het
  antwoord is (i) (symmetrisch, `RUNTIME-SEMANTICS.md` ch. 5.7a), dus case2 is
  de **spiegel-pinnende** case en pint `W2.voorgangerrang = 10` op een
  eenzijdige levering. Deze kaart zegt expliciet welke van de twee routes uit
  punt 1 ze neemt voor de Drools-kant van die case, en `OpslagrichtingWitnessTest`
  is bijgewerkt of niet aangeraakt volgens die keuze.
- De gate-13-keuze uit punt 4 staat met reden in de commit of in de golden.
- Alle dertien gates groen.

May touch: `corpus/golden/**`, `scripts/check-drools.sh` (**voorstel** —
beschermd pad), `backlog/`.

## Out of scope

- Elke wijziging aan `backends/drools/**`: de dracht is gemeten aanwezig.
- De wire, `spec/schema/**`, `spec/verifier/**`, `ref/**`,
  `backends/clingo/**` — alle drie de uitvoerders leveren vandaag het juiste
  antwoord op deze vorm.
- Rolresolutie via naam op de wire en in de uitvoerders: dat is de
  zusterkaart over `ba75f11`.
- De wederkerige §13.3.9.2-vorm en zijn naamdelingsweigering: geland
  2026-08-30 en gedekt door twee spelende goldens.
- Het schrijven van de omgekeerde poot van een zelfreferentiële **to-many**
  rol (flank B): luide weigering aan de Drools-kant, `leeg` in ref en clingo,
  geen divergentie — een eigen kaart waard als iemand die vorm nodig heeft,
  niet deze.
