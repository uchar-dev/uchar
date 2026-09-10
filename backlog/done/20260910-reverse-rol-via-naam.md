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

*Nummerkandidaat:* WF-101, nadrukkelijk als KANDIDAAT (bijgewerkt 2026-09-10:
WF-098 is inmiddels bezet door de contextwaarde-namenregel, ch. 4.1, kaart
`20260909-constante-naam-gesloten-verzameling`, WF-099 door de
begrenzing-grenseenheidsregel, ch. 4.6, kaart
`20260909-begrensd-grens-verifier-omheining`, en WF-100 door de
kenmerkeigenaar-regel, ch. 4.6 (q1), kaart
`20260909-eigenaar-verklaart-kenmerk`). Dezelfde kandidaat wordt
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

---

## Result (2026-09-10)

### Het besluit

**Maintainerbesluit op vraag 13, gesteld én expliciet bevestigd 2026-09-10:
antwoord (i) — een relatie is symmetrisch. Eén geleverde poot vestigt de
relatie, en de tegenrol leest hem.** Deze kaart voert dat besluit uit; ze
herbeslist niets.

Het besluit spreekt zich uit over BEIDE vormen van de discriminant:

- **1:N** (één rol to-one, de zusterrol to-many). `ref` en de ASP-backend
  spiegelden niet en lazen de niet-geleverde richting leeg; Drools las hem vol
  omdat het bonenmodel daar één veld draagt dat noodzakelijk beide richtingen
  bedient. Onder (i) is Drools' gedrag de norm en volgen `ref` en de
  ASP-backend. **Gemeten: de divergentie is weg** — de nieuwe getuige-golden
  speelt in alle drie de uitvoerders met dezelfde waarden, in alle drie de
  leveringsvormen.
- **1:1** (beide rollen to-one). Daar draagt het bonenmodel twee velden en
  spiegelt Drools uit zichzelf niet, dus moet de case-invoerlaag het tweede
  veld schrijven. **Die schrijfactie is in de vervolgronde geland** (zie
  "onbeheerde naad" hieronder), en daarmee zijn de twee onder (i) voorspelde
  nieuwe divergenties — (a) zelfgetypeerd 1:1 en (b) kruistypisch 1:1 — beide
  gemeten dicht in `ref` én in BEIDE Drools-takken. Het besluit verplaatst de
  divergentie dus niet; ze is weg.

### De prijs, uitgeschreven

**Ankerpunt 1 — `anchor/jutterswet-penninguitgifte/case2`, handberekend.**

Model: `penningbezit` = houder (Jutter, to-one) / penning (Penning, mv
penningen, TO-MANY) → 1:N. `dossiervoering` = dossierhouder (Jutter, to-one) /
dossier (Dossier, mv dossiers, to-one) → 1:1. Objectcreatie 1 mint per
penninggerechtigde Jutter een Penning (bedrag := vondstwaarde, kenmerk
uitgereikt) met terugkoppeling over penningbezit; objectcreatie 2 mint een
Dossier, geblokkeerd als het subject de to-one rol `dossier` in de
invoermomentopname al vult. Consistenties: `penningcontrole` (elke Penning is
uitgereikt) en `houderscontrole` (het aantal houder van de Penning ≥ 1).

Invoer zoals gepind: J3 (vondstwaarde 7, penninggerechtigd) met
`penningen: [P1]` en `dossier: [D1]`; P1 (bedrag 5) zonder rollen en zonder
kenmerken; D1 zonder rollen.

Bij het laden spiegelt de lezer onder (i) op de ZUSTERROL:
`J3.penningen = [P1]` → **`P1.houder = [J3]`**; `J3.dossier = [D1]` →
`D1.dossierhouder = [J3]`.

| grootheid | gericht (oud) | symmetrisch (i) | reden |
|---|---|---|---|
| J3 aantal penningen | 2 | **2** | P1 plus de mint; de spiegel raakt J3's eigen links niet |
| J3 penningtotaal | 12 | **12** | 5 (P1) + 7 (mint) |
| J3 aantal dossiers | 1 | **1** | de creatie blijft geblokkeerd — de blokkade leest J3's eigen links |
| penningcontrole × P1 | vuurt | **vuurt** | P1 is niet uitgereikt |
| houderscontrole × P1 | vuurt (aantal 0) | **vuurt NIET (aantal 1)** | P1.houder leest J3 door de terugpoot |

De inconsistentie verdwijnt dus precies één keer, op precies de plek die de
kaart voorspelde. Gemeten, letterlijk:

    MISMATCH: expected inconsistent (a channel fact from rule "houderscontrole"
    on "P1"), but the channel carries: penningcontrole × P1

**Wat ervoor terugkomt.** Twee dingen, want de verwachtingstaal kent geen
"vuurt NIET"-vorm:

1. `case2` houdt zijn vier andere verwachtingen ongewijzigd en krijgt een
   VIERDE instantie: **P2** (Penning, bedrag 3, geen rollen, geen kenmerken,
   door geen enkele Jutter genoemd). `houderscontrole` heeft daarmee weer een
   vurende getuige, en die vuurt nu waar de relatie werkelijk niet bestaat.
   Handberekend: P2 verandert niets aan J3's drie waarden (P2 is niet aan J3
   gekoppeld) en voegt `penningcontrole × P2` en `houderscontrole × P2` aan het
   kanaal toe. De pin verhuist van `houderscontrole × P1` naar
   `houderscontrole × P2`. Deze case is **besluitONafhankelijk**: hij slaagt
   onder beide lezingen. Gemeten: `oracle: PASS (5 expectations)` in beide.
2. Nieuwe **`case3`**, die het besluit zelf pint langs de enige route die
   afwezigheid kan uitdrukken — `consistent` (het kanaal is leeg).
   Invoer: J1 (vondstwaarde 4, penninggerechtigd) met `penningen: [P1]`;
   P1 (bedrag 6) met kenmerk `uitgereikt` en zonder rollen. Handberekend onder
   (i): de Penningcreatie mint een tweede, uitgereikte Penning met bedrag 4 en
   `houder = J1`; de Dossiercreatie is niet geblokkeerd (J1 vult de to-one rol
   `dossier` niet) en mint er één. Dus **aantal penningen 2, penningtotaal
   6 + 4 = 10, aantal dossiers 1**, `penningcontrole` vuurt nergens (beide
   Penningen zijn uitgereikt) en `houderscontrole` vuurt nergens
   (P1.houder = [J1] door de terugpoot, mint.houder = [J1] door de creatie) →
   **kanaal leeg → `consistent`**.

   Rood-eerst, gemeten op dezelfde wire met de gerichte lezing:

       MISMATCH: expected consistent, but the channel carries: houderscontrole × P1

   Groen onder (i): `oracle: PASS (4 expectations)`.

**Ankerpunt 2 — de kosten van het NIET-gekozen antwoord (ii), gemeten.**

Antwoord (ii) ("een link is gericht") had `ref` en de ASP-backend ongemoeid
gelaten en `anchor/jutterswet-penninguitgifte/case2` niet aangeraakt. De prijs
stond aan de andere kant:

- **De 1:N-vorm is in het bonenmodel niet gericht te lezen.** Eén veld bedient
  beide richtingen, dus geen enkele Drools-lezing kan onderscheiden welke kant
  de case leverde. (ii) had daar een WF-clausule of een caselezer-regel geëist
  die een eenzijdige levering AFWIJST zodra de andere richting gelezen wordt —
  plus negatiefs — en gate 12 had er een reden-dragende weigeringsklasse bij
  gekregen in plaats van een divergentie. Concreet: de drie casussen van de
  nieuwe getuige-golden waren dan twee weigeringen en één speler gebleven, en
  de vorm was uit het vierkant gevallen in plaats van erin gekomen.
- **De vorm is niet zeldzaam.** Twee goldens staan vandaag al buiten gate 12
  met LETTERLIJK deze reden — "dat feittype heeft ÉÉN beanveld en dat draagt
  noodzakelijk BEIDE richtingen, dus de gerichte lezing … is in het v0
  beanmodel niet uit te drukken (§13.3.9 opslagrichting)":
  `anchor/jutterswet-gemeenteberging` (regel "aantal bergende gemeenten van
  een vondst") en `anchor/jutterswet-penninguitgifte` (consistentie
  "houderscontrole"). Onder (ii) waren dat permanente uitsluitingen geworden —
  de gerichte lezing is daar niet uit te drukken, punt. Onder (i) zijn het
  transpiler-beperkingen die een latere kaart kan opheffen zonder dat de
  semantiek in de weg staat: het ENE veld dat er is, draagt precies wat (i)
  vraagt.
- **Wat (ii) wél had bespaard:** de 1:1-schrijfactie hieronder was dan niet
  nodig geweest, en de ankerverwachting was blijven staan.

### Wat er is geland, rood-groen per onderdeel

**1. Spec (VOORSTEL — beschermd pad).** `spec/RUNTIME-SEMANTICS.md` krijgt een
eigen case-invoersectie: **ch. 5.7a "The case input: what a supplied role link
establishes"**, geplaatst achter ch. 5.7 (de evaluatiesessie, waarvan stap 3
"insert the input facts" is). Dat is plekoptie (a) uit de kaart; ch. 4.1 blijft
over verdeling gaan en verwijst voor de algemene wet naar 5.7a, met de
wederkerige vorm als bijzonder geval. De sectie draagt de normzin, twee
mechanische gevolgen (de lezer spiegelt op de zusterrol; een
referentieveld-bonenmodel schrijft wat het kan dragen) en het gewerkte
voorbeeld met concrete uitkomsten voor de niet-geleverde richting.

**2. De gedeelde caselezer.** `ref/lib/case.ml`: `resolve_role_ft` levert nu
het drietal (feittype, VERRE rol, ZUSTERROL); `materialise_reciprocal` heet
`materialise_relations` en spiegelt elke tweerollige relatie, gesleuteld op de
zusterrol, in dezelfde leesvoorkeursvolgorde die `Eval.role_keys` en
`World.add_link` gebruiken. Aparte pass over de oorspronkelijke links, dus
volgorde-onafhankelijk en niet zelf-voedend; idempotent; een botsende
terugpoot op een to-one zusterrol is een laadfout (WF-013). De ASP-backend
deelt deze lezer.

Corpusgevolg, gemeten over de hele groene zone: **428 casussen, 1 mislukking**
— exact de ankerverwachting hierboven, en verder niets. (De kaart mat 427/1;
de zone is sindsdien met één casus gegroeid.) De 496 worked examples bleven
alle slagen.

**3. De getuige-golden `corpus/golden/jutterswet-ketenband/`.** De bron uit de
kaart, ongewijzigd: zelfreferentieel feittype `ketenband` met `voorligger`
(to-one) en `opvolger` (mv `opvolgers`, to-many), twee regels die elk via de
ANDERE rol navigeren. Frontend: `objecttypen=1 feittypen=1 resolvedRegels=2`,
`navigatieBindingen [{ketenband, voorligger}]` en `bronRol {ketenband,
opvolger}`; `verifier: PASS`. Drie casussen, W1/W2/W3 met rangnummer 10/20/30,
één relatie (W1 is voorligger van W2 en W3), drie leveringsvormen:

| casus | levert | `voorliggerrang` W1/W2/W3 | `opvolgeraantal` W1/W2/W3 |
|---|---|---|---|
| 1 | beide poten | leeg / 10 / 10 | 2 / 0 / 0 |
| 2 | alleen de naar-veel-poot | leeg / 10 / 10 | 2 / 0 / 0 |
| 3 | alleen de naar-één-poten | leeg / 10 / 10 | 2 / 0 / 0 |

Handberekend vóór het draaien. De asymmetrie A→B ≠ B→A die de kaart eiste zit
in de rijen zelf: `voorliggerrang` en `opvolgeraantal` lopen tegengesteld, en
`W1.voorliggerrang` blijft leeg — niemand vult `voorligger` naar W1 toe, dus
ch. 5.8 regel 1 leest leeg. Dat is de flank die een te ruime spiegel zou
breken.

Rood-eerst, gemeten op de gerichte lezing:

    case1  oracle: PASS (6 expectations)      besluitonafhankelijk
    case2  MISMATCH: W2.voorliggerrang = leeg, expected 10
           MISMATCH: W3.voorliggerrang = leeg, expected 10
    case3  MISMATCH: W1.opvolgeraantal = 0, expected 2

**4. Het gewerkte voorbeeld verbatim in `ref/test/`.** WV-5.7a: de drie
leveringsvormen door `Case.materialise_relations` heen, elk zes waarden, plus
de WF-013-botsflank. De suite gaat van 496 naar **515** worked examples.

**5. De Drools-case-invoer (`cases/CaseRunner.java`, `zetRollen`).**

- **(a) de tegenrol via de geschreven NAAM.** `verbindendeRolViaElementnaam`
  levert de zusterrol uit de geschreven rolnaam; die wordt alleen aanvaard als
  `FactModelGenerator.zelfpaarRollen` het veld werkelijk heeft afgeleid —
  dezelfde twee-autoriteitsvorm die de heenrichting al gebruikte. Een
  by-pair-AMBIGU kruistypisch paar houdt dus zijn by-pair-antwoord en zijn
  naamdelingsweigering.
- **(c) de 1:N-weigering vervalt waar de naam-route hem draagbaar maakt.**
  Gemeten per vorm op de nieuwe golden:

      vóór:  case1 EXCLUDE   case2 EXCLUDE   case3 PLAYED 1
      na:    case1 PLAYED 1  case2 PLAYED 1  case3 PLAYED 1

  De weigeringstekst die verdwijnt is letterlijk die uit de kaart ("role
  'opvolgers' (Wachter -> Wachter) reaches no to-one reference field … neither
  direction of the type pair derives a to-one role"). De weigering die BLIJFT
  is de dubbelmeervoudige zelfrelatie (`erflijn`): daar is de zusterrol ook
  to-many, dus geen van beide kanten draagt een veld — gemeten, de bestaande
  pin daarop staat ongewijzigd.
- **(b) de spiegelende schrijfactie voor 1:1 — GELAND in de vervolgronde**, na
  de merge van de zusterkaart. Zie het hoofdstuk "de onbeheerde naad"
  hieronder.
- Nieuw: `ZusterrolInvoerWitnessTest` (5 tests, additief) pint het veld op het
  doel, de drie spellingen op boonniveau, de gepinde waarden, de mutatieproef
  (één verwachting verdraaid → gemeld verschil) en de WF-013-botsing. `ref`
  weigert diezelfde case bij het laden, letterlijk gemeten:
  `role "opvolgers": "W2" already links W3 under the to-one role "voorligger";
  the reverse leg of "W1" -> "W2" conflicts (WF-013 to-one)`.

**6. De ASP-backend-suite.** Eén frame stageerde zijn onbereikbaarheid door de
link aan de VERRE kant te schrijven ("§7.1 set operand, no reachable member").
Dat is onder (i) geen onbereikbaarheid meer: het lid wordt bereikt en de
per-element temporele vouw meldt luid dat de case een asOfDate-bucketscalair
levert. Het frame levert nu GEEN relatie (dat is wat het bedoelde), en de oude
levering krijgt een eigen, additieve pin die precies dat nieuwe gedrag
vastlegt. Suite 92 → **93** checks.

### De onbeheerde naad: GELAND (vervolgronde, na de merge van de zusterkaart)

De spiegelende schrijfactie voor **1:1**-vormen — besluitonderdeel (b) — is in
een vervolgronde alsnog geland, nadat de zusterkaart (rol-schaduw) was gemerged
en `OpslagrichtingWitnessTest` daarmee vrijkwam. De tak is gerebased op main
`5661fde`: **geen conflicten**, en de backend-suite draaide meteen groen op 271
tests (266 van de zusterkaart + 5 van deze kaart).

**De ingreep.** In `CaseRunner.zetRollen`, direct achter de heenwaartse
schrijfactie: als de zusterrol óók to-one is — dus twee beanvelden, één per
richting — wordt het tweede veld op het doel geschreven, onder dezelfde
naamdelingspoort als de heenrichting. De 1:N-vorm doet niet mee (daar is één
veld dat beide richtingen al bedient) en de **wederkerige** vorm evenmin: die
herken je aan `rolLI == rolIL` (één rol die zijn eigen tegenrol is) en blijft
bij zijn eigen aparte pass, zodat het volgorde-onafhankelijkheidsargument en de
eigen conflictboodschap van `spiegelWederkerigeRollen` intact blijven.

**De gedichte divergentie, gemeten.** Getuige: de bron `beide` uit de
kaartmetingen — zelfgetypeerd feittype `opvolging` met `voorganger` en
`opvolger` **beide to-one** — via de frontend geëmitteerd
(`objecttypen=1 feittypen=1 resolvedRegels=2`, `verifier: PASS`), met een
casus die alleen de heenpoot levert (W1.opvolger = W2, rangnummers 10/20) en
`W2.voorgangerrang = 10` verwacht:

| | vóór (b) | na (b) |
|---|---|---|
| `ref` | `oracle: PASS (4 expectations)` | `oracle: PASS (4 expectations)` |
| Drools exec-model | `FAIL … voorgangerrang: verwacht 10, werkelijk leeg` | `PLAYED 1` |
| Drools DRL-tekst | `FAIL … voorgangerrang: verwacht 10, werkelijk leeg` | `PLAYED 1` |

Beide Drools-takken, dezelfde meting, dezelfde uitkomst. Daarmee is de laatste
ref ≢ Drools-flank van het besluit weg.

**De assertie-omzettingen, onder mandaat, rood-eerst.** Drie blokken pinden de
gerichte lezing. Elk is omgezet, hernoemd waar de naam de oude lezing droeg, en
draagt de omzetting met de gemeten rode uitvoer op zijn eigen plek:

1. `OpslagrichtingWitnessTest.de_gewone_zelfpaarkoppeling_spiegelt_niet` →
   **`…_spiegelt_op_de_zusterrol`**. `assertNull(get(W2,"voorganger"))` wordt
   `assertSame(W1, …)`. Rood-eerst gemeten met (b) er weer uit:
   `expected: <io.github.yhavinga.regelspraak.facts.Wachter@…> but was: <null>`.
   De twee resterende `assertNull`s blijven: de spiegel schrijft de
   **zusterrol**, nooit de geschreven rol.
2. `OpslagrichtingWitnessTest.dezelfde_telling_negeert_een_levering_aan_de_elementkant`
   → **`…_leest_een_levering_aan_de_elementkant_ook`**. 0/0 wordt 1/2.50, met
   meedraaiende tegenassertie. Rood-eerst:
   `case: attribuut aantal aangemelde vondsten: verwacht 1, werkelijk 0;
   attribuut aangemeld gewicht: verwacht 2.50, werkelijk 0`. `ref` op precies
   dat model en die case: **1 / 2.50 vanaf BEIDE einden** (`oracle: PASS (2
   expectations)` op beide spellingen; de oude 0/0 is daar nu een gemelde
   MISMATCH). Dat maakt het paar met de test erboven tot wat het nu is: twee
   spellingen van één relatie moeten hetzelfde uitrekenen.
3. **Buiten het letterlijke mandaat, gevonden door de suite:**
   `ObjectCreatieWitnessTest.een_koppeling_op_de_gemunte_zijde_blokkeert_de_creatie_niet`
   → **`…_blokkeert_de_creatie_ook`**. Dit derde blok — in een ánder bestand —
   pinde dezelfde lezing via de **objectcreatie-guard**: de ch. 5.5-1:1-guard
   vroeg "welk EIND is geschreven", en met de link alleen op de gemunte zijde
   muntte hij door (heffing 150). Onder ch. 5.7a vraagt de guard of het subject
   de rol VERVULT, en dat doet het na één geleverde poot, welke dan ook.
   **Beide uitvoerders zijn het eens en beide armen landen nu op 6.**
   `ref` hermeten op precies dat model en die case:
   `MISMATCH: J1.heffing = 6, expected 150` en, met 6 verwacht,
   `oracle: PASS (2 expectations)`. De arm blijft een echte fout weigeren: een
   guard die de GEMUNTE zijde zou lezen munt hier wél en landt op 150, en die
   lezing is nu de expliciet gepinde tegenassertie.

Twee blokken die ik vooraf verdacht had, blijven **ongewijzigd groen** —
nagerekend, niet aangenomen: `de_gerichte_koppeling_beslist_de_gelezen_waarde`
(hernoemd naar `de_telling_leest_de_genoemde_rol_niet_de_zusterrol`, waarden
1/0 onveranderd, want de telling leest de GENOEMDE rol en de spiegel vult de
zusterrol) en `de_een_op_een_telling_leest_het_veld_van_het_subject`.

Eén blok verloor zijn onderwerp:
`de_meervoudige_zusterrol_telt_de_elementkant_wel_mee_bekende_restlacune` →
**`…_mee_en_ref_nu_ook`**. Het karakteriseerde een ref ≢ Drools-divergentie op
de 1:N-vorm (ref 0/0, Drools 2/3.75) en schreef erbij "it FAILS WHEN THE GAP
CLOSES". Het gat is gesloten — van de SPECKANT: de getallen van de backend
waren de juiste, want op een 1:N-vorm kan geen enkele engine op dit feitmodel
zo'n link gericht lezen. `ref` hermeten: `oracle: PASS (2 expectations)` op
2 / 3.75. De assertie faalt dus niet; alleen zijn commentaar was onwaar
geworden, en dat is vervangen.

De javadoc bij `CaseRunner.spiegelWederkerigeRollen` draagt niet langer een
vastgelegde gap maar de geland-melding, met de drie vormen (1:N, 1:1,
wederkerig) en waar elk gepind is.

### Corpusdekking van de 1:1-spiegel — afweging en keuze

Overwogen zoals gevraagd: een corpuscase die de 1:1-spiegel in het GESPEELDE
kanaal pint, maar alleen als hij PLAYED wordt zonder nieuwe kanaalpin-lasten.

- **Een vierde case bij `jutterswet-ketenband` — afgewezen.** Die golden is
  1:N (`voorligger` to-one, `opvolger` to-many); er is daar geen tweede
  beanveld, dus geen enkele case op die wire kan de 1:1-spiegel raken.
- **Een nieuwe golden op de bron `beide` — afgewezen.** Dat is precies het
  artefact dat de zusterkaart `20260910-zelfref-rol-materialisatie.md`
  voorstelt (`jutterswet-wachterschakel`). Hem hier landen zou die kaart haar
  deliverable afnemen; hij blijft daar.
- **Gekozen: een vierde case bij `anchor/jutterswet-rekenkamer`.** Die golden
  SPEELT al, dus een extra casebestand kost **geen kanaalpin en geen
  `EXPECTED_PLAYED`-beweging** — gate 12 telt goldens, niet casussen. Het
  feittype `taxatieopdracht` is kruistypisch **1:1** (`opdrachtgever` en
  `rekenkamer` allebei to-one), precies de vorm.

`case4-koppeling-op-de-gemunte-zijde.json`, handberekend: J1 (grondslag 240,
aftrek 90) zonder rollen; R1 (grondslag 10, aftrek 4) met
`opdrachtgever: [J1]`. De spiegel schrijft `J1.rekenkamer = R1`; de
1:1-creatieguard ziet het subject de rol vervullen en **munt niets**;
`R1.heffing = 10 − 4 = 6`; `J1.heffing` = de heffing van haar rekenkamer =
**6**; kanaal leeg → `consistent`.

    rood-eerst (gerichte lezing)  MISMATCH: J1.heffing = 150, expected 6
    ref                           oracle: PASS (3 expectations)
    Drools exec-model             PLAYED 1
    Drools DRL-tekst              PLAYED 1
    --noop                        MISMATCH: R1.heffing = leeg, expected 6 …

De anker-README krijgt hier geen rij bij: `jutterswet-rekenkamer` staat niet in
die (niet-uitputtende) tabel.

### Voorstellen op beschermde paden

| pad | wijziging |
|---|---|
| `spec/RUNTIME-SEMANTICS.md` | nieuwe ch. 5.7a; ch. 4.1 verwijst ernaar |
| `corpus/golden/anchor/jutterswet-penninguitgifte/case2.json` | vierde Penning P2; de `houderscontrole`-pin verhuist van P1 naar P2 |
| `corpus/golden/anchor/jutterswet-penninguitgifte/case3.json` | nieuw; pint het besluit via `consistent` |
| `corpus/golden/anchor/README.md` | de rij voor deze anker bijgewerkt |
| `scripts/check-drools.sh` | `EXPECTED_PLAYED` 120 → **121**, met reden en meting; `EXPECTED_CHANNEL` ONGEWIJZIGD |
| `scripts/check-noop.sh` | **niet aangeraakt** — gemeten: alle vier de nieuwe casussen weerleggen een niets-doende uitvoerder, dus geen overleverregel nodig (392 → 396 weerleggers, 36 overlevers ongewijzigd) |

### Gate-uitkomsten (alle dertien, op de kop van deze tak)

| gate | uitkomst |
|---|---|
| 1 self-contained | PASS |
| 2 ir-schema | PASS (194 bestanden, vloer 192) |
| 3 verifier | PASS (194 golden schoon; 362 negatief, vloer 350) |
| 4 case-schema | PASS (459 casebestanden) |
| 5 oracle | PASS (515 worked examples, vloer 489; 433 casussen, vloer 427) |
| 6 clingo | PASS (93 checks, vloer 83; 433 casussen — ref ≡ clingo) |
| 7 noop | PASS (397 weerleggers; 36 gepinde overlevers) |
| 8 rood | PASS (26 rode casussen, vloer 26) |
| 9 catala | PASS (201 checks, vloer 201; 12 catala-goldens byte-identiek) |
| 10 regelspraak | PASS (173 tests, vloer 173; 39 rgs-goldens byte-identiek, vloer 38) |
| 11 toka-pariteit | PASS (29 toka-casussen, ref 29 / clingo 29 / Drools 29 PLAYED) |
| 12 drools | PASS (played **121**; auto-excluded 44; value-divergence 1; JUnit **271**, vloer 239) |
| 13 reftype-derivatie | PASS (74 twists over 15 goldens, vloer 74; 70 replays, 13 DRL-tak, vloer 13) |

Beginstand ter vergelijking (main @17e5254): gate 5 496/428, gate 6 92/428,
gate 7 392/36, gate 12 played 120 / JUnit 262. Na de rebase op main `5661fde`
(de gemergede zusterkaart) stond de backend-suite op 266 + de 5 van deze kaart
= 271; de vervolgronde voegde geen tests toe maar zette er drie om.
Alle dertien gates hierboven zijn op de KOP van de vervolgronde gemeten.

### Voorgestelde CONSTRUCTS-zin (rij 4, "Fact types and role navigation")

Gewogen: rij 5 gaat over rolLIDMAATSCHAP (de existentiële check) en rij 22 over
FEITcreatie; geen van beide draagt de case-invoer. Rij 4 draagt `RolBinding`,
`navigatieBindingen` en WF-053 en is daarmee de rij waar navigatie over rollen
thuishoort. Voorstel, additief aan die rij:

> **Landed (2026-09-10, maintainer decision on the case-input reading):** a
> supplied case role link ESTABLISHES the relation instance and both roles read
> it — the symmetric reading, `RUNTIME-SEMANTICS.md` ch. 5.7a, generalising the
> reciprocal both-directions clause of ch. 4.1 to every two-role fact type. The
> shared case loader materialises the reverse leg on the SISTER role
> (`Case.materialise_relations`), a conflicting to-one reverse leg is a load
> error (WF-013), and the Drools case-input arm resolves that sister role by
> the WRITTEN role name, so a self-typed 1:N relation supplied from either end
> now replays identically in all three executors (golden
> `jutterswet-ketenband`, three case files, one outcome). Tracked delta inside
> the mirror: on a **1:1** relation a reference-field fact model owns two
> fields and the Drools case-input arm still writes only the supplied one — a
> named, gate-invisible ref ≢ Drools flank, pinned in the backend's own suite,
> not in the corpus.

### Zusterkaart bijgewerkt

`backlog/new/20260910-zelfref-rol-materialisatie.md`: de
besluitafhankelijkheidspassage is herschreven — het besluit is (i), `case2`
wordt de **spiegel-pinnende** case (hij pint `W2.voorgangerrang = 10` op een
eenzijdige levering, niet `attributeEmpty`), de wachttijd is voorbij. Erbij:
een waarschuwingsblok boven flank A en B dat die twee metingen op de gerichte
lezing zijn gedaan en hermeten moeten worden (flank A is nu een echte
ref ≢ Drools-divergentie op de 1:1-vorm; flank B's Drools-weigering is
vervallen door de naam-route), en twee routes voor de Drools-kant van case2 —
de spiegelende schrijfactie erbij bouwen (dan bewegen de twee genoemde
`OpslagrichtingWitnessTest`-plekken mee, en dat bestand is van die kaart) of
alleen `case1` landen met de reden erbij. `EXPECTED_PLAYED` in die kaart is
van 119→120 naar "één omhoog vanaf 121" gecorrigeerd.

### Wat bewust niet is bewogen

- **`spec/IR-SPEC.md`** — gezocht: het document draagt geen zin over
  caselink-semantiek. Het noemt de case-invoer alleen als de laag waar de
  gedeelde lezer, niet een WF-regel, een onbekend label of een onbekende
  constructor weigert. De wet hoort dus in RUNTIME-SEMANTICS en nergens anders.
- **`spec/verifier/**` en een WF-nummer** — gewogen en NIET nodig. Symmetrie is
  SEMANTIEK, geen vormregel: ze zegt wat een geleverde link BETEKENT, niet
  welke documenten welgevormd zijn. Er is niets aan de wire dat een verifier
  hierop zou kunnen afwijzen; de enige nieuwe weigering zit in de caselezer
  (de botsende terugpoot) en die valt onder de bestaande WF-013-lezing, precies
  zoals de wederkerige variant dat al deed. De WF-101-kandidaat uit de kaart
  hoort dus bij antwoord (ii) en vervalt met dat antwoord; het nummer blijft
  vrij voor `20260910-geneste-aggregatie-rolketen.md`.
- **`spec/schema/uchar-case-v0.schema.json` en zijn README** — de schemavorm
  verandert niet (een rollink is nog steeds rolnaam → labels). De README-zin
  "role links point at labels" is niet onwaar geworden; een verwijzing naar
  ch. 5.7a daar is een nette toevoeging maar viel buiten de opdracht.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`** — niet aangeraakt, conform
  WAY-OF-WORKING §4: de voorgestelde rij-4-zin staat hierboven.
- **`DrlTranspiler`, `ExecModelTranspiler`, `OpslagrichtingWitnessTest`** — niet
  aangeraakt (zusterkaart in review).
- **De frontend** — draagt de vorm al en bindt bij naam; er viel niets te
  repareren. Gate 10 bevestigt: de nieuwe golden her-emitteert byte-identiek.
