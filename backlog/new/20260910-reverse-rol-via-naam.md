# De tegenrol van een zelfreferentieel feittype: op de wire is hij een naam, in de case-invoer nog een typepaar

Goal: vaststellen — en vastleggen — wat een ROLLINK IN EEN CASE betekent voor de
richting die de case NIET levert. De aanleiding is een fix in de
UWV-werkbank-toolchain (commit `ba75f11`) die de tegenrol niet langer via het
typepaar maar via de geschreven rolnaam oplost. De meting hieronder laat zien
dat uchar die ambiguïteit op de wire al structureel niet heeft — de RolBinding
noemt (feittype, rol) bij NAAM, en frontend, verifier, `ref` en de ASP-backend
doen de zelfreferentiële navigatie in beide richtingen goed. Maar op precies de
laag die de werkbank-fix raakt (de scenario-/verzoek-invoer; bij ons de
case-invoer) staat de typepaar-route nog, en daar zit géén naamprobleem maar
een **gemeten waarde-divergentie**: `ref`/ASP lezen de niet-geleverde richting
LEEG, Drools leest hem VOL. Beide Drools-takken, en niet alleen bij
zelfreferentie — maar wél alleen bij een **1:N**-relatie; bij een 1:1-relatie
lezen alle drie leeg. Die discriminant zit in het bonenmodel en staat in de
meting hieronder.

## Probleem

**Wat `ba75f11` repareert (eigen samenvatting).** In de werkbank-toolchain
zetten twee invoerpaden — de scenario-uitvoerder en de mapper die een
service-verzoek naar bonen vertaalt — een inline geschreven rol op een object.
Bij een naar-veel-rol ligt het referentieveld niet op de houder maar op elk
doel: het veld heet naar de TEGENROL. Die tegenrol werd opgezocht via het
TYPEPAAR (doeltype, houdertype). Bij een zelfreferentieel feittype — beide
rollen op hetzelfde objecttype, in de werkbank een keten waarin één exemplaar
aan meerdere andere exemplaren van datzelfde type voorafgaat — is dat paar per
constructie ambigu: de zoektocht telt
twee kandidaten en levert niets op. Er won dus GEEN verkeerde rol; het pad
brak. De fix brengt de inline-paden op hetzelfde principe dat het
top-niveau-relatiepad al hanteerde: de geschreven rolnaam noemt precies één
feittype, en de ANDERE rol van dat feittype is het te schrijven
terugverwijzingsveld. De by-paar-route blijft als defensieve terugval staan.
Erbij, uit de review: een naam oplossen betekent niet dat het VELD bestaat —
het feitmodel materialiseert bewust geen referentieveld voor een ambigu
typepaar of voor een naar-veel-tegenrol — dus wordt de setter afgetast en een
ontbrekend veld als domeindiagnose gemeld in plaats van als reflectiecrash; de
mapper toetst daarbij ook het objecttype van het opgegeven label tegen het
doeltype van de rol.

**Waarom dat bij uchar anders ligt.** Sinds de Phase-3-refactor draagt de wire
symbolische rolbindingen: `RolBinding{feittype, rol}`, `verbindendeRol`, en
genavigeerde lezingen gebonden per WF-053 (`spec/IR-SPEC.md` ch. 4.1a;
`spec/CONSTRUCTS.md` rij 4 — "Fact types and role navigation", de rij die
`RolBinding`, `navigatieBindingen` en WF-053 draagt). WF-053 eist resolutie op
de GEDECLAREERDE rolnaam ("the plural does not bind"). `ref` en de ASP-backend
delen daarvoor één functie (`Eval.bound_role`); de typepaar-afleiding is alleen
terugval, en de twee helften ervan gedragen zich verschillend op een
zelfgetypeerd paar:

- `Eval.connecting_role` (`ref/lib/eval.ml` ~176-178) WEIGERT LUID: "the
  connecting role from X to Y is ambiguous (%d candidate roles); the pinned
  runtime derives null there".
- `Eval.connecting_pair` (~216-220) levert `None` via de `Blocked`-arm — een
  BEWUSTE STILLE null, geen fout. De code zegt waarom: een zelfgetypeerd
  tweerollig feittype blokkeert de afleiding omdat de gepinde runtime daar ook
  null afleidt; de expliciete `verbindendeRol`-binding is de eenduidige route
  (IR-SPEC ch. 3.3).

De conclusie staat: de klasse van defect die `ba75f11` wegneemt — een by-paar
zoektocht die stil de VERKEERDE rol kiest — kan op dat pad niet ontstaan. Geen
van beide helften gokt.

Waar de typepaar-route bij ons wél nog staat, is in de case-invoer van de
Drools-backend (`backends/drools/.../cases/CaseRunner.java`, `zetRollen`): voor
een geschreven NAAR-VEEL-rol zoekt hij het opslagdragende veld via
`model.verbindendeRol(doelType, houderType)` — dezelfde aanroepvorm die
`ba75f11` vervangt. Bij een zelfgetypeerd paar levert die null en weigert de
backend luid met reden. Dat is dus geen stille fout, maar wel de plek waar het
werkbank-antwoord zou landen — en juist dáár blijkt de echte vraag een andere
te zijn.

## Meting (2026-09-10)

Alles in een eigen wegwerp-kopie van de repo op de HEAD van deze tak; de
werkbank-repo is alleen gelezen. De getuige is zelfdragend: onderstaande bron
is alles wat nodig is.

**De bron** (`bron.rgs`), een zelfreferentieel feittype met twee verschillend
genoemde rollen op hetzelfde objecttype, een naar-één- en een naar-veel-kant,
en twee regels die elk via de ÁNDERE rol navigeren, zodat verwisseling in de
uitkomst zichtbaar wordt:

    Objecttype de Wachter (mv: Wachters) (bezield)
        het rangnummer Numeriek (niet-negatief geheel getal);
        de voorliggerrang Numeriek (niet-negatief geheel getal);
        het opvolgeraantal Numeriek (niet-negatief geheel getal);

    Feittype ketenband
        de voorligger (mv: voorliggers) Wachter
        de opvolger (mv: opvolgers) Wachter
    Eén voorligger heeft meerdere opvolgers

    Regel voorliggerrang
        geldig altijd
            De voorliggerrang van een Wachter moet berekend worden als het rangnummer van de voorligger van de Wachter.

    Regel opvolgeraantal
        geldig altijd
            Het opvolgeraantal van een Wachter moet berekend worden als het aantal opvolgers van de Wachter.

**De frontend draagt de vorm en bindt bij NAAM.** `make ir` emitteert
`objecttypen=1 feittypen=1 resolvedRegels=2`, met `voorligger`
`meervoudig:false` en `opvolger` `meervoudig:true` op hetzelfde objecttype. De
naar-één-lezing krijgt `navigatieBindingen: [{feittype:"ketenband",
rol:"voorligger"}]`, de telling krijgt `bronRol: {feittype:"ketenband",
rol:"opvolger"}`. Verifier: `verifier: PASS`.

Drie casussen op dezelfde wire, drie manieren om dezelfde relatie te leveren.
Instanties W1/W2/W3 met rangnummer 10/20/30; de relatie is: W1 is voorligger
van W2 en van W3. Verwachtingen eerst met de hand berekend, daarna gedraaid.
De handrekening-kolom is berekend onder lezing **(ii)** hieronder — een link is
GERICHT, de niet-geleverde richting leest leeg. Onder lezing (i) zouden casus 2
en 3 andere getallen dragen; dat is precies de beslissing die deze kaart open
stelt, niet een aanname van de tabel.

| casus | geleverde rollen | handrekening (`voorliggerrang` / `opvolgeraantal`) |
|---|---|---|
| 1 | `W1{opvolgers:[W2,W3]}`, `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | leeg,10,10 / 2,0,0 |
| 2 | alleen `W1{opvolgers:[W2,W3]}` | leeg,leeg,leeg / 2,0,0 |
| 3 | alleen `W2{voorligger:[W1]}`, `W3{voorligger:[W1]}` | leeg,10,10 / 0,0,0 |

Uitkomsten:

| casus | `ref` | ASP-backend | Drools (exec-model) | Drools (DRL-tekst) |
|---|---|---|---|---|
| 1 | `oracle: PASS (6 expectations)` | `clingo: PASS (6 expectations)` | `EXCLUDE` (reden onder) | `EXCLUDE` (idem) |
| 2 | `oracle: PASS (6 expectations)` | `clingo: PASS (6 expectations)` | `EXCLUDE` (idem) | — |
| 3 | `oracle: PASS (6 expectations)` | `clingo: PASS (6 expectations)` | **`FAIL case3.json: attribuut opvolgeraantal: verwacht 0, werkelijk 2`** | **idem, letterlijk** |

De Drools-weigering bij casus 1 en 2 luidt letterlijk:

    EXCLUDE role 'opvolgers' (Wachter -> Wachter) reaches no to-one reference
    field: the written role name resolves to 'opvolger', which is TO-MANY — its
    storage is the far end's field, not a scalar reference here, and neither
    direction of the type pair derives a to-one role

Dat is precies het gat dat `ba75f11` in de werkbank dicht: de tegenrol via de
naam zou hier `voorligger` opleveren, en dat veld BESTAAT op de boon — casus 3
bewijst het, want daar schrijft dezelfde `zetRollen` die naar-één-rol wél en
leest `voorliggerrang` correct 10/10.

**Wat casus 3 blootlegt is geen naamprobleem.** Drools SPEELT die casus en geeft
een ander getal: `opvolgeraantal` van W1 is 2 waar `ref` en de ASP-backend 0
zeggen. De oorzaak is de opslagvorm, niet de resolutie: `ref` bewaart de links
GERICHT, per rolsleutel op de instantie die ze aanlevert (`ref/lib/case.ml`;
`World.links`), en de ASP-backend deelt die caselezer, terwijl het bonenmodel
per TO-ONE ROL een veld draagt en per TO-MANY rol géén.

**De discriminant, gemeten in `FactModelGenerator.leden`.** De afleiding kent
twee lussen die een scalair referentieveld opleveren — de by-paar-lus voor
kruistypische paren en `zelfpaarRollen()` voor zelfgetypeerde — en beide slaan
een naar-veel-rol over ("its storage is the far end's field"). Daaruit volgt:

- **1:N** (één rol to-one, de zusterrol to-many): het model draagt precies
  ÉÉN veld, het veld van de TO-ONE rol; de naar-veel-lezing is een **join over
  datzelfde veld**. Een eenzijdige to-one-levering is daardoor van BEIDE kanten
  zichtbaar. Dat is de gemeten divergentie: Drools leest VOL waar `ref` en de
  ASP-backend LEEG lezen. Casus 3 hierboven en de kruistypische controle
  eronder zijn allebei deze vorm.
- **1:1** (beide rollen to-one): het model draagt TWEE velden, elke richting
  zijn eigen opslag. Een eenzijdige levering laat de andere richting dus ook in
  Drools leeg — daar is **geen divergentie**, alle drie de uitvoerders lezen
  leeg. De zusterkaart `20260910-zelfref-rol-materialisatie.md` meet die vorm
  (haar flank A) en vindt drie keer hetzelfde antwoord.

Die discriminant is wat de beslissing hieronder draagt: (i) en (ii) verschuiven
de divergentie tussen deze twee vormen, ze heffen hem niet op.

**Het is niet zelfreferentie-specifiek.** Dezelfde meting kruistypisch, met
`Feittype ligplaats / de werf Werf / de schuit (mv: schuiten) Schuit / Eén werf
telt meerdere schuiten` en één regel `Het schuitaantal van een Werf … als het
aantal schuiten van de Werf`, casus `S1{werf:[W1]}`, `S2{werf:[W1]}`, W1 zonder
rollen:

    ref      oracle: PASS (1 expectations)     (schuitaantal = 0)
    ASP      clingo: PASS (1 expectations)     (schuitaantal = 0)
    Drools   FAIL case1.json: attribuut schuitaantal: verwacht 0, werkelijk 2

Levert de casus BEIDE richtingen (`W1{schuiten:[S1,S2]}` erbij), dan zeggen alle
drie 2 en meldt Drools `PLAYED 1`. De kruistypische naar-veel-levering gaat bij
Drools wél door de by-paar-route heen — dat paar is niet ambigu — dus de
weigering hierboven is echt de zelfreferentie-arm.

**Corpusdekking van de vorm zelf.** Over de hele groene zone dragen precies
DRIE goldens een gewoon (niet-wederkerig) zelfgetypeerd feittype:
`anchor/jutterswet-erflijn` (twee stuks: `opvolging` en `erflijn`),
`anchor/jutterswet-wachtersketen` en `synth2-budgetgrens-onder`. Van geen
enkel zo'n feittype levert een casus BEIDE rolrichtingen; alleen `erflijn`
levert er één (`opvolger`). De zelfreferentiële navigatie in twee richtingen is
dus vandaag nergens gepind — dat is het gat dat stap 2 hieronder vult. Geteld
2026-09-10; hertellen bij het landen.

**Er is al een anker dat de GERICHTE lezing pint.**
`corpus/golden/anchor/jutterswet-penninguitgifte/case2.json` levert
`J3{penningen:[P1], dossier:[D1]}` en niets op P1, en pint
`inconsistent: houderscontrole` op P1 — de consistentieregel "het aantal houder
van de Penning is groter of gelijk aan 1", gebonden aan
`{feittype:"penningbezit", rol:"houder"}`. Die regel vuurt alleen omdat P1's
`houder` LEEG leest terwijl J3 P1 onder zijn `penningen` noemt. Dat anker is
handgeschreven met absolute waarden en permanent gepind.

**De kosten van het spiegelende alternatief, gemeten.** In de wegwerp-kopie is
`Case.materialise_reciprocal` gemuteerd zodat hij ELKE tweerollige relatie
spiegelt (de zusterrol als sleutel), niet alleen de wederkerige. Uitkomst over
de hele groene zone:

    427 casussen, 1 mislukking
    corpus/golden/anchor/jutterswet-penninguitgifte/case2.json ::
      MISMATCH: expected inconsistent (a channel fact from rule "houderscontrole"
      on "P1"), but the channel carries: penningcontrole × P1
    test_ref: all 489 worked examples pass

Eén anker-verwachting, verder niets. En juist dat anker staat al om een ANDERE
reden buiten gate 12 (`scripts/check-drools.sh`: `transpiler SKIP: AANTAL over
a to-one element role whose sister role is plural`), zodat de onenigheid tussen
de uitvoerders er nooit zichtbaar is geweest. Gate 12 kent vandaag precies één
waarde-divergentie (`anchor/jutterswet-kustwacht`, negatie-stratificatie); de
klasse hierboven staat niet op die lijst en niet op de kanaallijst.

**Wat de spec hierover zegt.** `spec/RUNTIME-SEMANTICS.md` ch. 4.1 spreekt zich
uit over precies één geval: "A reciprocal relation entry holds in **both
directions**" — en de caselezer materialiseert de omgekeerde poot dan ook
alleen voor `wederkerig`. Over de GEWONE relatie zegt de spec niets; de
gerichte lezing is nergens uitgesproken, alleen geïmplementeerd en (per
ongeluk of niet) in één anker gepind.

## Voorgestelde aanpak

Spec-first, en de eerste stap is een BESLISSING, geen wijziging. De vraag is:
**vestigt één geleverde poot de relatie?** Deze kaart beantwoordt hem NIET; ze
legt de twee antwoorden met hun gemeten prijs naast elkaar en laat de keuze aan
de bouwkaart die het besluit neemt.

Beide antwoorden moeten gelezen worden met de discriminant hierboven in de
hand: 1:N draagt één veld en spiegelt daardoor in Drools, 1:1 draagt er twee en
spiegelt niet.

**(i) Een relatie is symmetrisch.** Eén geleverde poot vestigt de relatie, en de
tegenrol leest hem. Wat dit antwoord feitelijk steunt is de opslagvorm van de
gepinde productieruntime op de **1:N**-vorm: één veld, dat de join van beide
kanten leest — daar gedraagt Drools zich vandaag al symmetrisch, en `ref`/de
ASP-backend niet. Wat het NIET steunt is de 1:1-vorm: daar draagt het
bonenmodel twee velden en spiegelt Drools evenmin.

Gemeten prijs, met de caselezer van `ref` gemuteerd zodat hij ELKE tweerollige
relatie spiegelt:

- Over de groene zone: 427 casussen, 1 mislukking — de ankerverwachting
  `penninguitgifte/case2` (hierboven letterlijk), plus 489 worked examples die
  alle blijven slagen. Die ankerverwachting moet dan herzien worden; een
  beschermd pad, dus maintainerbesluit.
- **Nieuwe divergentie (a), zelfgetypeerd 1:1.** Op de bron `beide` van de
  zusterkaart, met een casus die alleen de heenpoot levert, gaat `ref` onder de
  mutatie PASSEN met `W2.voorgangerrang = 10`, terwijl Drools daar
  `verwacht 10, werkelijk leeg` meet.
- **Nieuwe divergentie (b), kruistypisch 1:1.** Op een probe `Baas`/`Knecht`
  waarin alleen `K1.baas = B1` geleverd wordt (verifier PASS) lezen vandaag alle
  drie uitvoerders leeg en speelt Drools de golden (PLAYED); onder de mutatie
  levert `ref` `B1.knechtrang = 20` en Drools nog steeds leeg.

Antwoord (i) haalt de divergentie dus **niet weg** — het verplaatst hem van de
1:N-vormen naar de 1:1-vormen die vandaag juist wél overeenstemmen. Wil (i) de
drie uitvoerders werkelijk laten samenvallen, dan is er een tweede ingreep
nodig: zie de onbeheerde naad hieronder.

**(ii) Een link is gericht.** De case levert per rolsleutel; de niet-geleverde
richting leest leeg. Dan zijn `ref` en de ASP-backend goed op beide vormen, en
volgt Drools ze op de **1:1**-vorm al (twee velden, geen spiegeling — gemeten,
geen divergentie). Alleen op de **1:N**-vorm kan Drools niet volgen: daar is er
maar één veld en is de naar-veel-lezing een join over datzelfde veld, zodat de
boon niet kan onderscheiden welke richting geleverd is. De case-invoerlaag moet
die vorm dan weigeren, of de casus moet beide poten leveren. Prijs: een
WF-clausule of een caselezer-regel die een eenzijdige levering afwijst wanneer
de andere richting GELEZEN wordt, plus negatiefs; en gate 12 krijgt er op de
1:N-vorm een reden-dragende weigeringsklasse bij in plaats van een divergentie.

*Nummerkandidaat:* WF-100, nadrukkelijk als KANDIDAAT (bijgewerkt 2026-09-10:
WF-098 is inmiddels bezet door de contextwaarde-namenregel, ch. 4.1, kaart
`20260909-constante-naam-gesloten-verzameling`, en WF-099 door de
begrenzing-grenseenheidsregel, ch. 4.6, kaart
`20260909-begrensd-grens-verifier-omheining`). Dezelfde kandidaat wordt
genoemd door `20260910-geneste-aggregatie-rolketen.md` voor de padregel van een
meerstaps aggregatiebereik. Wie het eerst landt gebruikt het nummer, de ander
schuift op.

**De onbeheerde naad: de case-invoerlaag moet dan schrijven.** Onder antwoord
(i) volgt de 1:1-vorm niet vanzelf. Om Drools daar met `ref` te laten
samenvallen moet de tegenpoot ECHT GESCHREVEN worden — een SPIEGELENDE
SCHRIJFACTIE in de case-invoerlaag, die het tweede beanveld vult. Dat is geen
naamresolutie (de rol is al eenduidig) en geen bonenmaterialisatie (het veld
bestaat al); het is een derde ding: wat een case-invoer BETEKENT voor de
richting die ze niet noemt. Die vraag hoort bij DEZE kaart, of bij de bouwkaart
die het besluit neemt — niet bij de zusterkaart over `ccdf58f`, die uitsluitend
over materialisatie gaat. Onder antwoord (ii) valt de naad weg: dan schrijft de
case-invoer niets extra's en is weigeren het werk.

Wat er in beide gevallen moet gebeuren, in deze volgorde:

1. **Spec eerst** (voorstel, beschermd pad): de zin die vandaag alleen over de
   wederkerige relatie gaat, uitbreiden tot een uitspraak over ELKE relatie, met
   een worked example dat de niet-geleverde richting noemt. Zonder die zin is er
   geen norm om een gate tegen te draaien.

   **De plek is zelf een vraag.** De bestaande zin staat in
   `spec/RUNTIME-SEMANTICS.md` ch. 4.1, dat "The form and its recipients" heet
   en onder hoofdstuk 4, "Allocation (verdeling)", valt. Een ALGEMENE
   case-invoerwet — wat een geleverde rollink betekent voor de richting die de
   case niet noemt — staat daar structureel vreemd: verdeling is een
   berekeningsvorm, niet de invoerlaag. Twee opties, en de kaart moet er één
   kiezen: (a) een eigen case-invoersectie waar de wet thuishoort, met een
   verwijzing vanuit ch. 4.1 naar de wederkerige bijzonderheid; of (b) de zin
   in ch. 4.1 laten en in de commit vastleggen waarom hij daar mag blijven
   staan. Optie (a) is het voorstel; ze houdt ch. 4.1 over verdeling.
2. **Corpusgetuigen die BEIDE navigatierichtingen pinnen.** De bron hierboven
   is de getuige: één zelfreferentieel feittype, een naar-één- en een
   naar-veel-kant, twee regels die elk via de andere rol navigeren, en
   verwachtingen die asymmetrisch zijn (A→B ≠ B→A). Dat is wat vandaag ontbreekt
   — geen enkele golden pint de zelfreferentiële navigatie in twee richtingen —
   en het is de regressiedetector voor de resolutie, los van welk antwoord (i)/
   (ii) wint. Minstens de volledig geleverde casus 1 hoort erin; welke van
   casus 2/3 mee kan, volgt uit de beslissing.
3. **De Drools-case-invoer.** Pas ná (1) mag `zetRollen` de tegenrol via de
   NAAM oplossen (de werkbank-zet), want vandaag zou dat de weigering op casus
   1/2 wegnemen en er bij casus 2 een divergentie voor terugleggen: de naar-één
   -poot geschreven vanuit een naar-veel-levering laat `voorliggerrang` in
   Drools 10 lezen waar `ref` leeg leest. Onder antwoord (i) is dat juist het
   gewenste gedrag en is de zet de laatste stap; onder (ii) mag hij niet komen
   en moet de weigering blijven, met een scherpere reden.
4. **Gate 12 bijwerken** (`EXPECTED_CHANNEL` / `EXPECTED_PLAYED`,
   `scripts/check-drools.sh` — voorstel, beschermd pad) naar wat er dan
   werkelijk speelt.

Meet vóór het landen alles opnieuw: de tabellen hierboven, de 427/1-mutatie en
de gate-12-lijsten kunnen intussen bewogen zijn.

## Acceptance

- Eén beslissing, expliciet gemotiveerd, met de kosten van het niet-gekozen
  antwoord erbij — en met de ankerverwachting van `penninguitgifte/case2`
  benoemd in plaats van omzeild. De beslissing spreekt zich uit over BEIDE
  vormen van de discriminant (1:1 en 1:N) en zegt wat er met de twee onder (i)
  gemeten NIEUWE divergenties gebeurt; een antwoord dat alleen de 1:N-vorm
  regelt is onvolledig.
- De spiegelende schrijfactie in de case-invoerlaag (de onbeheerde naad) is
  belegd: uitgevoerd, of met reden afgewezen — niet stilzwijgend overgeslagen.
- De spec-zin geland (voorstel) vóór enige uitvoerderswijziging; een
  worked example met concrete verwachte uitkomst voor de niet-geleverde
  richting, verbatim overgenomen in `ref/test/`.
- Corpusgetuige(n) die de zelfreferentiële navigatie in BEIDE richtingen
  pinnen, met met de hand berekende absolute waarden, en `ref` ≡ ASP-backend
  erop.
- Rood-eerst: de getuige faalt aantoonbaar vóór de wijziging (bij (ii): de
  negatiefs met verifier-uitkomst vóór en ná).
- De Drools-kant expliciet: draagt met dezelfde gepinde waarden, of weigert
  reden-dragend — en de gate-12-kanaalvloer bijgewerkt, niet omzeild.
- De meting hierboven HERMETEN op de kop van de kaart en de uitkomst gepind.
- Alle dertien gates groen.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/verifier/**`,
`spec/schema/**`, `corpus/golden/anchor/**`, `scripts/check-drools.sh` (alle
**voorstel** — beschermde paden); `ref/**`, `backends/clingo/**`,
`backends/drools/**`, `corpus/golden/**`, `corpus/negatief/**`, `backlog/`.

## Out of scope

- **De bonen-materialisatie van zelfgetypeerde naar-één-rollen in de
  Drools-backend** (`FactModelGenerator`, `zelfpaarRollen`,
  `heeftReferentieVeld`). Dat is de zusterkaart bij commit `ccdf58f`, die het
  materialisatiekanaal onderzoekt. Deze kaart gaat over RESOLUTIE en NAVIGATIE:
  welke rol wint, en wat een geleverde link betekent. De twee raken elkaar op
  één punt — de naam-route kan een veld noemen dat niet gematerialiseerd is —
  en dat punt is hier bewijsmateriaal, geen werkterrein. Wat NIET naar die
  zusterkaart gaat is de spiegelende schrijfactie in de case-invoerlaag: het
  veld bestaat daar al, alleen de vraag of de invoer het moet vullen staat open,
  en die vraag hoort hier (zie "de onbeheerde naad").
- De wederkerige zelfrelatie (§13.3.9.2): gedragen sinds 2026-08-30, in `ref`
  én Drools gespiegeld, en door ch. 4.1 al normatief belegd. Alleen de GEWONE
  tweerollige relatie is hier open.
- De frontend: de emissie is hier bewijsmateriaal. Ze draagt de vorm al en
  bindt bij naam; er is niets te repareren.
- WF-053 zelf: de bindingsregel doet wat ze belooft en wordt niet heropend.
