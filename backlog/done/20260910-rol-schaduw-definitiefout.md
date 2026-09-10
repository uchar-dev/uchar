# Rol-schaduw: de wire is eenduidig, de vierde lezer in de Drools-backend niet

Goal: de werkbank-fix `8fbb170` (repo `regelspraak-werkbank/toolchain`) maakt
een to-één rol die zijn naam deelt met een attribuut of kenmerk tot een
**modeldefinitiefout**. Voor uchar is die conclusie **niet** over te nemen: de
v0-wire draagt rollen en objecttypeleden in gescheiden structuren, dus de
botsing is op de wire niet eens een botsing — ref en clingo antwoorden er
gemeten correct op. Wat de fix wél blootlegt is een **gat in uchars eigen
Drools-backend**: dezelfde naam-dedup zit daar ongewijzigd in
`FactModelGenerator.leden`, drie van de vier lezers van dat referentieveld
weigeren de vorm al luid (sinds 2026-08-30), en de vierde — de genavigeerde
**waardelezing** — leest stilzwijgend het verkeerde veld en antwoordt `leeg`
waar ref en clingo `0` schrijven. Deze kaart sluit die vierde lezer, legt de
naamruimtescheiding als spec-zin vast, en pint de case-sleutel-arm van de
rol-vs-rol-ambiguïteit met een rood-getuige.

**Geen WF-kandidaat**, met reden — zie "Wat er niet gebeurt" hieronder. De
enige vorm die om een regel zou kunnen vragen (rol-vs-rol) heeft al een gekozen
lezing in `IR-SPEC.md` ch. 4.3 ("a name matching several reachable roles stays a
loud executor fault, never a guess"); wat ontbreekt is één getuige, geen regel.

## Probleem

**Wat `8fbb170` repareert.** De fact-bean-afleiding in de toolchain kent per
objecttype één Java-eigenschap per identifier en ontdubbelt in vaste volgorde:
attribuut wint van kenmerk, kenmerk wint van referentie. Een to-één rol wiens
naam al bezet is door een attribuut, een kenmerk of een eerder afgeleide rol van
een ánder feittype verloor daardoor stil zijn referentieveld, terwijl de
transpilers wél over de geschreven rolnaam bleven navigeren. De commit-tekst
noemt drie waarneembare gevolgen: de DRL-tak liet de hele build vallen, de
exec-model-tak leverde een lege lezing of een join die nooit matcht, en een
rolschrijfactie sloeg reflectief stuk. De contractvalidatie kon het niet zien,
omdat die tegen de al-ontdubbelde ledenlijst valideert. De fix laat de
afleidingspas zijn afgewezen rolkandidaten teruggeven naast de leden — één pas,
zodat detectie en materialisatie niet uit elkaar kunnen lopen — en de resolver
meldt elke afgewezen kandidaat als ERROR op de declaratieregel van het
objecttype. Twee tegenvormen blijven uitdrukkelijk legaal: kenmerk+attribuut die
een naam delen zónder rol, en een naar-veel-rolnaam die samenvalt met een
attribuut aan de één-zijde (daar leidt het beanmodel geen veld af).

**Welke laag dat is.** Dit is de **fact-bean-materialisatielaag**, niet de
taallaag. De vlakke naamruimte waarin de botsing ontstaat is een eigenschap van
het Java-bean-doelformaat, niet van RegelSpraak en niet van een IR.

**Waar uchar structureel anders zit.** De v0-wire draagt rollen in `feittypen`
en leden in `objecttypen`, en een genavigeerde lezing noemt haar rolhop en haar
attribuut in **verschillende velden**: `navigatiePad` (plus de
`navigatieBindingen`-extensie, `RolBinding {feittype, rol}`) tegenover
`attribuut`. `WF-002` zegt bovendien met zoveel woorden dat attributen en
kenmerken aparte naamruimten zijn. Er is dus geen wire-positie waarop een rol en
een attribuut om dezelfde plek strijden — gemeten hieronder.

**Maar uchar draagt de dedup wel, één laag lager.** `backends/drools`
(de gate-12-poort van de productieruntime) heeft
`FactModelGenerator.leden` mét exact dezelfde dedup-commentaarregel en **zonder**
de afwijslijst van `8fbb170`. Sinds 2026-08-30 weigeren drie lezers van dat
verdrongen veld de vorm luid, met de reden erbij: de case-rolinvoer
(`CaseRunner.naamdelingsPoort`), de objectcreatie-terugkoppeling en de
aggregatie-join. Alle drie vragen de gedeelde `FactModelGenerator.heeftReferentieVeld`,
en alle drie zijn gepind in `OpslagrichtingWitnessTest` sectie 3. De **vierde**
lezer — de gewone genavigeerde waardelezing, `ExecModelTranspiler.navReader`
(één hop) en `navKetenReader` (keten), plus `navReactiviteit` en
`navComparisonRole` die dezelfde hops bouwen — vraagt hem **niet**: die roept
`typedGetter(factClass(subject), rol)` rechtstreeks aan en krijgt de getter van
het verdringende attribuut of kenmerk terug.

## Meting (2026-09-10)

Uitgevoerd in een schone kopie van de werkkop, met de frontend uit
`frontends/regelspraak` (jar-only), de OCaml-verifier, `ref/`, `backends/clingo`
en de bestaande gebouwde jar van `backends/drools` (beide transpilertakken).
Alle verwachtingen zijn eerst met de hand bepaald.

### De bron

Eigen materiaal, gemodelleerd naar `corpus/golden/synth-navigatiepremie`. Het
attribuut `de werf` op `Schuit` schaduwt de to-één rol `werf` die hetzelfde
objecttype uit het feittype `ligplaats` krijgt:

    Objecttype de Werf (mv: Werven)
        het tarief Numeriek (geheel getal);

    Objecttype de Schuit (mv: Schuiten)
        de werf Numeriek (geheel getal);
        de premie Numeriek (getal met 2 decimalen);
        het product Numeriek (geheel getal);

    Feittype ligplaats
        de werf Werf
        de schuit (mv: schuiten) Schuit
    Eén werf telt meerdere schuiten

    Regel premie van een schuit
        geldig altijd
            De premie van een Schuit moet berekend worden als 20% van het
            tarief van de werf van de Schuit.

    Regel product van een schuit
        geldig altijd
            Het product van een Schuit moet berekend worden als de werf van
            de Schuit maal 2.

De twee regels lezen dezelfde naam in de twee betekenissen: de eerste
navigeert, de tweede leest het attribuut. Handrekening met
`Werf.tarief = 312`, `Schuit.werf = 5` en de link gelegd: `premie = 62,40`,
`product = 10`. Zonder link: `premie = 0`, `product = 10` — de nul volgt uit
`RUNTIME-SEMANTICS.md` ch. 1.2, de coalesce-rij voor "percentage van"
(`10% van leeg = 0`): een mislukte to-één navigatie leest leeg, en leeg
coalesceert daar naar 0.

Vier varianten ernaast: **kenmerkschaduw** (`is werf kenmerk (bijvoeglijk)` in
plaats van het attribuut), de **legale tegenvorm** (attribuut `de schuit` op
`Werf` — de naar-veel-rolnaam aan de één-zijde), een **controle** zonder
schaduw (attribuut hernoemd naar `kade`), en een **handgeschreven wire-vorm**
voor rol-vs-rol (twee feittypen die `Schuit` allebei een to-één rol `ligger`
geven, ontward met expliciete `navigatieBindingen`).

### Frontend (gate 10)

De frontend emitteert **stil**: exit 0, lege stderr, geen WARNING, geen ERROR.

    ir: objecttypen=2 feittypen=1 resolvedRegels=2 geschreven naar …

En hij resolveert **allebei de betekenissen correct**, structureel gescheiden:

| bron | `attribuut` | `objecttype` | `navigatiePad` | `navigatieBindingen` |
| --- | --- | --- | --- | --- |
| `het tarief van de werf van de Schuit` | `tarief` | `Werf` | `["werf"]` | `[{ligplaats, werf}]` |
| `de werf van de Schuit` | `werf` | `Schuit` | `[]` | — |

De uitzondering is **rol-vs-rol**: daar resolveert de navigatie helemaal niet
(WF-012's v0-spiegelnotitie, "fail-safe: een niet-unieke match levert geen
navigatie"). De frontend schrijft dan een `AttribuutRef` met
`attribuut = "tarief van de ligger"` op `Schuit` en een lege `navigatiePad`, en
zegt daar **niets** over — exit 0 op een document dat de eigen verifier afwijst.

### Verifier (gate 3)

| document | uitkomst |
| --- | --- |
| attribuutschaduw | `verifier: PASS` |
| kenmerkschaduw | `verifier: PASS` |
| naar-veel-naamdeling (legaal) | `verifier: PASS` |
| rol-vs-rol, handgeschreven wire | `verifier: PASS` |
| rol-vs-rol zoals de frontend hem emitteert | `verifier: FAIL` — `WF-034 … 'tarief van de ligger' is neither an attribute nor a characteristic of 'Schuit'` (plus WF-034/WF-035 op de sentinel-typen) |

De PASS op de eerste vier is **juist**, geen gat: de wire is er eenduidig.

### De vier uitvoerders

Exitcodes: `0` = PASS/PLAYED, `1` = waarde-afwijking, `2` = weigering/faul.

| vorm | ref | clingo | Drools exec-model | Drools DRL-tekst |
| --- | --- | --- | --- | --- |
| controle (geen schaduw), met link | 0 | 0 | PLAYED | PLAYED |
| controle (geen schaduw), zonder link | 0 | 0 | PLAYED | PLAYED |
| naar-veel-naamdeling (legaal) | 0 | 0 | PLAYED | PLAYED |
| attribuutschaduw, met link | 0 | 0 | EXCLUDE (2) | EXCLUDE (2) |
| attribuutschaduw, zonder link, attribuut **gevuld** | 0 | 0 | **FAIL (1)** | EXCLUDE (2) |
| attribuutschaduw, zonder link, attribuut **leeg** | 0 | 0 | PLAYED | EXCLUDE (2) |
| kenmerkschaduw, met link | 0 | 0 | EXCLUDE (2) | EXCLUDE (2) |
| kenmerkschaduw, zonder link, kenmerk gezet | 0 | 0 | **FAIL (1)** | EXCLUDE (2) |
| rol-vs-rol (handgeschreven wire) | **2** | **2** | EXCLUDE (2) | EXCLUDE (2) |

De kernregels, letterlijk:

    EXCLUDE role 'werf' resolves to the to-one role 'werf' on Schuit, which shares
      its name with a declared member — the bean cannot carry the reference field
      (name-sharing bean limit: attribute > characteristic > reference)

    FAIL …: attribuut premie: verwacht 0, werkelijk leeg          ← exec-model-tak

    EXCLUDE drl compile: [Message [id=1, … level=ERROR, path=file0.drl, line=10 …
      text=Unable to Analyse Expression …                          ← DRL-tak

    CASE ERROR: role key "ligger" is ambiguous from "Schuit"        ← ref én clingo

De twee Drools-weigeringen op de rol-vs-rol-vorm zijn **niet reden-dragend**:
geen van beide noemt de ambiguïteit. De exec-model-tak meldt
`EXCLUDE transpiler SKIP: regel: premie van een schuit — regel (waarde buiten
scope)`, de DRL-tak `EXCLUDE drl-transpiler MARKER: regel: premie van een
schuit — TODO navigatie: het tarief van de ligger van de Schuit`. Beide zijn de
generieke scope-/TODO-markering die elke niet-gedragen vorm krijgt; de reden die
ref en clingo wél geven (`role key … is ambiguous`) ontbreekt aan Drools-kant.

Drie dingen die eruit volgen:

1. **De vierde lezer is stil.** Zodra de case de rol níét schrijft (dus de
   `naamdelingsPoort` niet raakt) maar de regel er wél overheen navigeert, leest
   de exec-model-tak het verdringende lid in plaats van de relatie en levert
   `leeg` waar ref en clingo `0` schrijven. Geen ERROR, geen reden, geen
   weigering: een gewone waarde-afwijking.
2. **De afwijking is data-afhankelijk.** Met het schaduwende attribuut **leeg**
   speelt dezelfde vorm groen mee in `ref`, clingo én de exec-model-tak (alle
   drie `0`, Drools PLAYED); de DRL-tak weigert de build ook daar
   (`EXCLUDE drl compile: … Unable to Analyse Expression …`, exit 2) — die tak
   is dus niet data-afhankelijk, hij valt op elke schaduwvorm om. Een golden van
   deze vorm kan dus PLAYED en groen zijn in het kanaal dat gate 12 bewaakt en
   tóch een kapot model dragen — precies het geval dat "een groene
   deelverzameling is niet groen" beschrijft.
3. **De twee Drools-takken divergeren op één model**, exact zoals de
   werkbank-commit beschrijft: de DRL-tak weigert de hele build, de
   exec-model-tak antwoordt stil verkeerd. Gate 13 draait **beide** takken.

### Corpusdekking

Machinale sweep over `corpus/golden`, `corpus/rood`, `corpus/negatief` en de
parity-zone: voor elke to-één rol is gekruist of zijn naam samenvalt met een
attribuut- of kenmerknaam van het objecttype dat het referentieveld zou dragen.

    documenten gescand: 166 golden + 26 rood + 355 negatief + 30 parity = 577
    botsingen gevonden: 0

Geen enkel bestaand document verandert dus van gedrag, in geen enkele lens. Dat
maakt dit een kaart over een **bereikbare, ongedekte** vorm, niet over gemeten
corpusschade.

## Verdict per lens

| lens | verdict |
| --- | --- |
| frontend (gate 10) | **draagt** — resolveert beide betekenissen correct en scheidt ze op de wire. Stil, maar terecht stil. Eén uitzondering: bij rol-vs-rol emitteert hij zonder diagnostiek een document dat de verifier afwijst. |
| wire + verifier (gates 2/3) | **draagt** — de botsing bestaat op de wire niet; PASS is de juiste uitkomst. Rol-vs-rol is op de wire uitdrukbaar en verifier-schoon, en dát is de gekozen lezing (ch. 4.3: luide uitvoerdersfout, nooit een gok) — maar de case-sleutel-arm ervan heeft geen getuige. |
| ref, clingo (gates 5/6) | **draagt** — correcte waarden op alle schaduwvormen; bij rol-vs-rol een identieke, luide caseweigering in beide. |
| Drools exec-model (gate 12) | **stil fout** op de leeslus; luid en correct op de drie eerder gesloten lezers. |
| Drools DRL-tekst (gate 13-been) | **weigert** — luid, maar met een blote compilerfout in plaats van een reden. |

## Wat er niet gebeurt, en waarom

- **Geen definitiefout in uchar.** De werkbank moet weigeren omdat haar
  doelformaat de vorm niet kan dragen. uchars wire kan hem wél dragen en twee
  van de drie uitvoerders voeren hem correct uit. Weigeren zou over-afwijzing
  zijn: een document dat aantoonbaar één betekenis heeft, ongeldig verklaren
  omdat één backend-vorm hem niet kan opslaan.
- **Geen nieuw WF-nummer.** Er is geen wire-eigenschap die vandaag ontbreekt.
  Als de onderhouder tóch strengheid wil, is de plek de WF-002/WF-012-buurt en
  de prijs staat hierboven; die keuze hoort dan in een eigen kaart met een
  eigen negatief, niet hier.
- **Geen frontend-weigering.** Wel een open vraag (punt 4 hieronder) over de
  rol-vs-rol-stilte, die een echt kapot document oplevert.

## Voorgestelde aanpak

Spec-first, in deze volgorde. Het meeste werk zit in stap 2.

1. **Spec-zin (voorstel, beschermd pad).** In `spec/IR-SPEC.md`, in de
   WF-002/WF-012-buurt, één alinea die vastlegt wat de meting laat zien:
   rolnamen en objecttypeledennamen zijn **aparte naamruimten** op de v0-wire;
   een to-één rol die zijn naam deelt met een attribuut of kenmerk van het
   objecttype dat de relatie draagt is **welgevormd**, want de genavigeerde
   lezing noemt haar rolhop (`navigatiePad`/`navigatieBindingen`) en haar
   attribuut in verschillende velden. Mét de eerlijkheidsnotitie: een
   **bean-vormige** consument met één eigenschap per identifier kan die vorm
   niet dragen en moet hem per vorm weigeren — dat is een backend-limiet, geen
   welgevormdheidsregel. Dit is de zin die de weigering in stap 2 rechtvaardigt
   in plaats van hem als willekeur te laten staan.
2. **`backends/drools`: de vierde lezer sluiten.** Rood eerst — een vierde
   assertie in `OpslagrichtingWitnessTest` sectie 3, in de vorm van de drie die
   er al staan: model + case eerst door `ref/` gehaald, dan de weigering
   verwacht. Daarna in `ExecModelTranspiler` de gedeelde
   `FactModelGenerator.heeftReferentieVeld` vragen op elke rolhop die een
   genavigeerde lezing legt: `navReader`'s GENAVIGEERD-tak, `navKetenReader`'s
   hops (élke hop, niet alleen de eerste), `navReactiviteit`'s hopketen en
   `navComparisonRole`. Uitkomst: dezelfde luide, reden-dragende weigering die
   de andere drie lezers al geven, met de naam van het verdringende lid erin —
   niet een stil `leeg`. **Één afleiding, één vraag**: net als bij de bestaande
   drie mag de check niet naast `leden()` opnieuw afgeleid worden.
3. **De DRL-tak.** Meten of dezelfde poort de blote compilerfout vervangt door
   dezelfde reden. Zo ja: doen (het is de goedkope helft). Zo nee: in de commit
   vastleggen waarom de DRL-tak zijn build-fout houdt, zodat gate 13 er niet
   op faalt.
4. **Rood-getuige voor de case-sleutel-arm.** `corpus/rood/<slug>/` met de
   handgeschreven rol-vs-rol-wire en een case die beide `ligger`-links onder één
   sleutel aanbiedt; `expectedFault` met `ref` en `clingo` op het gemeten
   fragment `role key "ligger" is ambiguous`. Dit pint de tweede helft van
   ch. 4.3's wet: `corpus/rood/ambiguous-fold-role-name` pint vandaag de
   **fold**-arm, de **case-invoer**-arm is ongepind terwijl beide uitvoerders
   hem al identiek weigeren.
5. **Frontend-diagnostiek (beslissen, niet noodzakelijk doen).** Bij rol-vs-rol
   levert de frontend exit 0 op een document dat de verifier afwijst. De
   goedkope lezing: dat is WF-034's werk en de frontend hoeft niets. De strenge:
   een WARNING met de niet-geresolveerde navigatie erin. Beslis het hier en leg
   de reden vast; het is een `frontends/regelspraak`-wijziging met een
   JUnit-getuige, geen wire-wijziging.
6. **Geen golden.** Elke golden van deze vorm valt na stap 2 uit het
   PLAYED-kanaal (Drools EXCLUDE) en kost dus een naam met reden in
   `EXPECTED_CHANNEL` in `scripts/check-drools.sh` — een beschermd pad, voor een
   vorm waarvan de weigering het gewenste antwoord is. `EXPECTED_PLAYED` blijft
   staan: het script zegt op regel ~369 met zoveel woorden dat een NIEUW
   toegevoegde golden die niet speelt nooit is meegeteld ("a NEWLY ADDED golden
   that does not play was never counted: pin it and leave EXPECTED_PLAYED
   alone"). De prijs is dus één kanaalpin, en daarop rust dit besluit. De
   getuige hoort in de eigen JUnit-suite van de backend (stap 2), die gate 12 al
   draait.

## Acceptance

- **Rood eerst, aantoonbaar.** De nieuwe assertie in
  `OpslagrichtingWitnessTest` faalt op de huidige jar met de gemeten
  waarde-afwijking (`werkelijk leeg`) en slaagt daarna met de reden-dragende
  weigering. De reden bevat de naam van het verdringende lid.
- **De vier hop-lezers apart geraakt.** Per lezer (`navReader`,
  `navKetenReader`, `navReactiviteit`, `navComparisonRole`) een getuige of, per
  lezer die er geen krijgt, één zin waarom niet — geen verzamelassertie die drie
  van de vier ongemeten laat.
- **Nul over-afwijzing, gemeten.** De legale tegenvorm (naar-veel-rolnaam gelijk
  aan een attribuut aan de één-zijde) blijft PLAYED, en de volledige
  golden-sweep van gate 12 houdt `EXPECTED_PLAYED` op 119 zonder wijziging aan
  `EXPECTED_CHANNEL`. `EXPECTED_MIN_TESTS` (239) is een vloer en hoeft bij groei
  niet mee.
- **Beide Drools-takken opnieuw gemeten** op de zes probevormen hierboven, met
  de exitcodes in de commit; de exec-model-tak levert op geen enkele daarvan nog
  exit 1.
- **De rood-getuige** faalt in ref én clingo met het gepinde fragment en gate 8
  is groen.
- **De spec-alinea** is als voorstel ingediend en noemt de meting (nul botsingen
  over de vier zones, 2026-09-10) als grond.
- **Alle dertien gates groen**, met gate 12 en gate 13 expliciet apart gedraaid.

May touch: `backends/drools/**`, `spec/IR-SPEC.md` (**voorstel** — beschermd
pad), `corpus/rood/**`, `frontends/regelspraak/**` (alleen bij besluit 5),
`backlog/`.

## Out of scope

- `ref/`, `backends/clingo`, `spec/verifier/**`, `spec/schema/**`: alle drie
  leveren gemeten het juiste antwoord op elke schaduwvorm, en de wire heeft geen
  regel nodig.
- Een definitiefout of WF-regel voor de naamdeling zelf — uitdrukkelijk
  afgewezen hierboven, met reden en prijs.
- De **zelfreferentiële** to-één rolmaterialisatie: eigen kaart
  (`20260910-zelfref-rol-materialisatie.md`), die dezelfde dedup als open flank
  noemt en hiernaar doorverwijst.
- De wederkerige §13.3.9.2-naamdeling: geland 2026-08-30, gepind in
  `OpslagrichtingWitnessTest`.
- De **fold**-arm van de rol-vs-rol-ambiguïteit: al gepind door
  `corpus/rood/ambiguous-fold-role-name`.
- De sleutel- en spellingsbotsingen van dagsoorten: eigen kaart,
  `20260909-wf017-sleutel-en-spellingsbotsing.md`. Verwant alleen in vorm (een
  naam die een andere naam verdringt), niet in laag.

## Result (2026-09-10)

### Hermeting op deze kop

De kaart mat op `45157e8`; alles hieronder is opnieuw gemeten op
`17e5254` + de claim, met een verse jar, de frontend uit
`frontends/regelspraak` (jar-only), de OCaml-verifier, `ref/` en
`backends/clingo`. De matrix reproduceert **exact**, inclusief de twee rijen
die het gedrag dragen:

| vorm | ref | clingo | exec-model | DRL-tekst |
| --- | --- | --- | --- | --- |
| controle (geen schaduw), met link | 0 | 0 | PLAYED | PLAYED |
| controle (geen schaduw), zonder link | 0 | 0 | PLAYED | PLAYED |
| naar-veel-naamdeling (legaal) | 0 | 0 | PLAYED | PLAYED |
| attribuutschaduw, met link | 0 | 0 | EXCLUDE | EXCLUDE |
| attribuutschaduw, zonder link, attribuut gevuld | 0 | 0 | **FAIL (1)** | EXCLUDE |
| attribuutschaduw, zonder link, attribuut leeg | 0 | 0 | PLAYED | EXCLUDE |
| kenmerkschaduw, met link | 0 | 0 | EXCLUDE | EXCLUDE |
| kenmerkschaduw, zonder link, kenmerk gezet | 0 | 0 | **FAIL (1)** | EXCLUDE |
| rol-vs-rol (handgeschreven wire) | 2 | 2 | EXCLUDE | EXCLUDE |

Letterlijk, uit de hermeting:

    FAIL attr-geenlink-gevuld.case.json: attribuut premie: verwacht 0, werkelijk leeg
    EXCLUDE role 'werf' resolves to the to-one role 'werf' on Schuit, which shares
      its name with a declared member — the bean cannot carry the reference field
      (name-sharing bean limit: attribute > characteristic > reference)
    EXCLUDE drl compile: [Message [id=1, … level=ERROR, path=file0.drl, line=10 …
      text=Unable to Analyse Expression …
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"      ← ref én clingo, exit 2

De drie luide lezers staan ongewijzigd (`OpslagrichtingWitnessTest` sectie 3,
groen). Verifier: PASS op alle vier de schaduwdocumenten en op de
handgeschreven rol-vs-rol-wire; FAIL (WF-034/WF-035) op de rol-vs-rol zoals de
frontend hem emitteert. Frontend: exit 0, **nul bytes stderr**,
`diagnostics: []`. De corpussweep is opnieuw gedraaid op deze kop: 167 golden +
26 rood + 362 negatief + 30 parity = **585 documenten, nul botsingen**.

### Wat er landde

**1. De poort, in beide takken.** Elke lezer die het referentieveld **aanraakt**
vraagt nu de gedeelde `FactModelGenerator.heeftReferentieVeld` — één afleiding,
één vraag, precies zoals de drie eerder gesloten lezers. Niet alleen de
navigerende lezers: zie punt 1b.

- exec-model (`ExecModelTranspiler`): `navReader`'s GENAVIGEERD-tak,
  `navKetenReader` op **elke** hop, `navComparisonRole`, en
  `navReactiviteit`'s hopketen én enkelvoudige join. De weigering draagt de
  reden via het veld `navReden` (het `aggregatieReden`-patroon, gereset in
  `meet(...)` zodat een reden nooit in een andere constructie belandt) en wordt
  gelezen op de vier generieke skip-plekken: regel-waarde, regel-voorwaarde en
  de twee objectcreatie-armen.
- DRL-tekst (`DrlTranspiler`): centraal in `naviResolve`, de ene plek waar die
  tak navigatie oplost — GENAVIGEERD en elke KETEN-hop. `meet(...)` geeft de
  reden voorrang boven de tekst van de TODO-markering.

De **redentekst** staat op één plek: `FactModelGenerator.geenReferentieveldReden`,
naast de afleiding die hij uitlegt. Beide takken en alle lezers halen hem daar,
zodat één limiet nooit twee bewoordingen krijgt; de asserties pinnen fragmenten,
niet de hele zin.

**1b. Er bleek een VIJFDE lezer, en die was óók stil fout.** De
§8.1.7-rollidmaatschapscheck **navigeert niet** — hij null-test hetzelfde veld om
te beslissen of het subject een rol vervult (`werf != null`). Daarom viel hij
buiten de vier navigatie-lezers, en daarom was hij de ergste van de familie: een
verdringend attribuut is niet-null zodra het een waarde draagt, dus de check las
die waarde als bewijs van rolvervulling. Gemeten op een verifier-schoon,
ref-schoon model (dezelfde `Schuit`/`werf`-schaduw, voorwaarde "de Schuit een
schuit is", case zonder rolinvoer):

    verifier: PASS
    ref     : oracle: PASS (1 expectations)     — S1.toeslag blijft leeg
    clingo  : clingo: PASS (1 expectations)     — idem
    Drools exec-model : FAIL  verwacht leeg, werkelijk 8
    Drools DRL-tekst  : FAIL  verwacht leeg, werkelijk 8

Beide takken identiek, en **pre-existent**: de navigatiepoort raakte hem niet.
Nu gepoort op beide plekken met dezelfde gedeelde vraag — de exec-tak in de
`Conditie.Rollidmaatschap`-arm, de DRL-tak in dezelfde arm die anders de
`rollidmaatschap`-marker geeft — en met een eigen reden-prefix, zodat de twee
soorten lezers in het gate-rapport uit elkaar te houden zijn:

    EXCLUDE transpiler SKIP: regel: toeslag van een schuit — regel
      (rollidmaatschapscheck: de rol 'werf' op Schuit deelt haar naam met een
      gedeclareerd lid — …): toeslag van een schuit
    EXCLUDE drl-transpiler MARKER: regel: toeslag van een schuit —
      rollidmaatschapscheck: de rol 'werf' op Schuit deelt haar naam met een
      gedeclareerd lid — …

**Eerlijke populatietelling.** Gepoort zijn nu: de vier navigatie-lezers **en**
de rollidmaatschapscheck, naast de drie die 2026-08-30 al gesloten werden.
**Ongepoort én ongemeten** blijven: de far-side rollidmaatschap-join, de twee
aggregatie-bronbindingen buiten de al gepoorte join, de genavigeerde
kenmerk-tijdreeks, de verdeling (zes leesplekken op één afgeleide `join`), de
feitcreatie, en de naam-kopie in `toOneAantalReader` — die laatste test de
naamdeling met een eigen `heeftLid`-afleiding in plaats van de gedeelde vraag te
stellen. Alle zeven met regelnummers gekaart in
`backlog/new/20260910-drools-ongepoorte-rollezers.md`. Dat de vijfde lezer pas
bij een tweede lezing gevonden werd, is in die kaart het bewijsstuk dat de klasse
echt stil fout kán zijn.

Uitkomst, gemeten na de poort, op alle acht de probevormen: geen enkele
exit 1 meer in de exec-model-tak, geen `ConstraintEvaluationException` meer, en
in **beide** takken dezelfde zin:

    EXCLUDE transpiler SKIP: regel: premie van een schuit — regel (genavigeerde
      lezing: de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid —
      het beanveld draagt dat attribuut/kenmerk en niet de referentie (leden():
      attribuut > kenmerk > referentie), dus de koppeling naar Werf heeft geen
      veld (§13.3.9 opslagrichting)): premie van een schuit

    EXCLUDE drl-transpiler MARKER: regel: premie van een schuit — genavigeerde
      lezing: de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid — …

De controle en de legale tegenvorm blijven PLAYED in beide takken: nul
over-afwijzing.

**2. Rood eerst, met echte testuitvoer.** Vier nieuwe asserties in
`OpslagrichtingWitnessTest` sectie 3, elk op een eigen lezer, elk op een
in-memory variant van `synth-navigatiepremie` waarvan de verwachting eerst door
`ref/` is gehaald. Op de code zónder poort:

    Tests run: 14, Failures: 3, Errors: 0, Skipped: 0
      de_genavigeerde_waardelezing_weigert_het_verdrongen_veld:222
        Expected …CaseRunner.Excluded to be thrown, but nothing was thrown.
      de_enkelvoudige_hoplezing_weigert_hetzelfde_veld:234
        Expected …CaseRunner.Excluded to be thrown, but nothing was thrown.
      de_genavigeerde_vergelijking_weigert_hetzelfde_veld:246
        Unexpected exception type thrown, expected: <…CaseRunner.Excluded>
        but was: <org.drools.modelcompiler.constraints.ConstraintEvaluationException>

Met de poort: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`. De twee
"nothing was thrown" zijn precies de stille waarde-afwijking (`speelCase` gaf
de mismatchregel terug in plaats van te weigeren); de derde is de
ongeclassificeerde fire-fout van de vergelijkingskant.

De **vierde** assertie, voor de vijfde lezer, is in een eigen ronde rood
getoond — de drie poorten van hierboven raakten hem niet:

    Tests run: 15, Failures: 1, Errors: 0, Skipped: 0
      de_rollidmaatschapscheck_weigert_hetzelfde_veld:264
        Expected …CaseRunner.Excluded to be thrown, but nothing was thrown.

en daarna groen: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`.

**Per lezer**, zoals de acceptance eist:

- `navKetenReader` — getuige `de_genavigeerde_waardelezing_weigert_het_verdrongen_veld`
  (de ref zoals de frontend hem emitteert, `navigatiePad ["werf"]`).
- `navReader` — getuige `de_enkelvoudige_hoplezing_weigert_hetzelfde_veld`
  (hetzelfde model en dezelfde case met het pad leeg en de hop bij haar rol
  genoemd; `ref` is onverschillig voor het verschil, de twee lezers moeten dat
  ook zijn).
- `navComparisonRole` — getuige `de_genavigeerde_vergelijking_weigert_hetzelfde_veld`
  (de voorwaarde-operand genavigeerd, de waarde teruggebracht tot een eigen
  attribuut, zodat de weigering alleen van de vergelijkingslezer kan komen).
- `navReactiviteit` — **poort zonder eigen getuige**, met de reden: die methode
  wordt alleen bereikt vanaf het scalaire waardepad *nadat* dat pad zelf
  lowerde, en beide waardelezers poorten nu elke hop die ze lopen, dus een
  veldloze hop kan er niet aankomen. De poort blijft staan omdat `navRefs`
  argumentposities loopt die het waardepad niet noodzakelijk zelf lowert, en
  omdat een stil weggelaten far-side watch een **verouderde waarde** oplevert
  in plaats van een weigering — het enige gevolg dat die methode juist moet
  voorkomen. Ze weigert de constructie dus, in plaats van de hop over te slaan.
- `Conditie.Rollidmaatschap` (de §8.1.7-check) — getuige
  `de_rollidmaatschapscheck_weigert_hetzelfde_veld`, apart rood getoond.

JUnit-teller: 262 → **266**, nul skips. Vloer `EXPECTED_MIN_TESTS` (239)
ongemoeid.

**3. De spec-alinea (voorstel, beschermd pad).** `spec/IR-SPEC.md` §2.6, direct
na WF-013, in de WF-002/WF-012-buurt. Geen nieuw WF-nummer. Letterlijk:

> **Roles and object-type members are separate namespaces.** WF-002 states
> that attributes and characteristics are separate namespaces; roles are a
> third. A role MAY share its name, case-insensitively, with an attribute
> or a characteristic of an object type the same fact type relates. Such a
> document is **well-formed**, and no rule above narrows it. The reason is
> structural: a navigated read names its role hop and its attribute in
> *different* fields — `navigatiePad`, plus the `navigatieBindingen`
> extension of ch. 4.1a, against `attribuut` (in v1 the unified `path`
> against `attribute`, row 4) — so a role name and a member name never
> compete for one wire position. The reference interpreter and the ASP
> backend both answer such a document correctly today, with no special
> case. It is a REACHABLE but UNINHABITED shape: swept 2026-09-10 over
> every corpus zone — 555 documents — the collision occurs zero times, so
> no existing case pins this and the paragraph is what states it.
>
> The neighbouring shape — one written name matching *several* reachable
> roles — is a different question and keeps its own answer: a loud executor
> fault, never a guess (ch. 4.3). Its two arms are pinned unevenly. The
> navigation arm is refused statically by WF-053. The case-input arm, where
> a case supplies links under a role key that resolves to two roles, is
> refused identically by both executors but while LOADING the case, which
> the red corpus's expected-fault contract does not reach; that gap is
> carded in `backlog/new/20260910-rood-zone-laadfouten.md`.
>
> *Consumer note, recorded rather than hidden.* A consumer that
> materialises each object type as a record with **one property per
> identifier** — a Java bean, a struct, a row — cannot hold that shape: it
> has one slot where the document has two names, so one of the two is
> displaced. This bites wherever the relation's scalar reference lands,
> which is at the end whose *connecting role is to-one* — not only the many
> side of a one-to-many fact type, but each end of a one-to-one one, and a
> self-typed pair on the single bean it has.
>
> That is a limit of the target format, not a well-formedness rule, and
> this specification does not lift it into one: rejecting the document
> would deny a program that has exactly one meaning, which the other
> executors compute. What such a consumer does instead is refuse the
> affected construct per form, loudly and naming the displaced reference,
> rather than reading the displacing member in the role's place. The
> Drools backend does exactly that in both of its transpiler branches, at
> every reader that touches the field: the navigated single-hop, chain,
> comparison and reactivity reads, and the ch. 8.1.7 role-membership
> check, which does not navigate but null-tests the same field. Those
> refusals are pinned by that backend's own test suite and by gate 12, not
> by `spec/verifier/` — a verifier that enforced them would be enforcing
> one target format's shape on every consumer. Its remaining unaudited
> readers of that field are carded in
> `backlog/new/20260910-drools-ongepoorte-rollezers.md`.

De 555 is de **publiek verifieerbare** helft van de sweep (golden 167 + rood 26
+ negatief 362); de parity-zone telt in de kaart mee (585) maar wordt in het
spec-document niet genoemd, omdat dat document zelfstandig moet lezen.

### De DRL-tak: gemeten, en dezelfde poort gegeven

De kaart liet de keuze open. Gemeten: dezelfde vraag vervangt de blote
compilerfout wél door dezelfde reden, en het kost één poort op één plek
(`naviResolve`) plus de voorrangsregel in `meet(...)` — de goedkope helft, dus
gedaan. De tak is niet data-afhankelijk: hij viel op **elke** schaduwvorm om,
ook met het verdringende attribuut leeg, en doet dat nu ook als
reden-dragende MARKER. Gate 13 draait beide takken en beweegt niet: 13
DRL-tekst-replays PLAYED, vloer 13.

De vijfde lezer (punt 1b) zit in dezelfde tak op een **tweede** plek — de
`Rollidmaatschap`-arm die anders de `rollidmaatschap`-marker geeft — omdat die
lezer niet door `naviResolve` loopt. Dat is precies de vorm van het gat: de
centrale navigatiepoort dekt navigatie, niet elke aanraking van het veld.

### De rode getuige: NIET geland, met de meting erbij

De kaart vroeg een rood-getuige voor de **case-sleutel**-arm. Die is
**geblokkeerd**, en de blokkade is met een echte gate-run gemeten, niet
beredeneerd. Het document en de case zijn gebouwd (twee feittypen die `Schuit`
allebei een to-één `ligger` geven; de case biedt beide links onder één sleutel
aan). Verifier PASS, en beide uitvoerders weigeren identiek:

    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     ref  exit 2
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     clingo exit 2

Maar de fout valt bij het **laden van de case**, niet bij de evaluatie:
`Case.load_file` lost elke rolsleutel op (`resolve_role_ft`) voordat
`expectedFault` überhaupt uit het bestand wordt gelezen. Beide runners gooien
dan `Case.Error` en eindigen op exit 2, waar gate 8 exit 0 eist. Gemeten met de
kandidaat-map in `corpus/rood/`:

    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — ref did not observe
      the expected fault:
    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — clingo did not observe
      the expected fault:
    FAIL: ambiguous-case-role-key carries no drools fragment and is not in the
      Drools exclusion list — pin a fragment or list it with a reason:

**Drie** van de drie benen vallen dus om, niet twee: de twee uitvoerderbenen op
de exitcode, en het Drools-been omdat de case noch een `drools`-fragment noch
een reden-regel in `scripts/check-rood.sh` draagt.

Dit is geen bug maar de **contractgrens van de zone**, en die staat zwart op
wit in het case-schema: *"The red-path alternative to `expected`
(corpus/rood): **evaluation** of this case MUST fail observably in every
executor that carries its form."* Een laadfout valt daar per definitie buiten.
Hem toelaten vergt **vier** artefacten, waarvan er twee beschermd zijn en twee
uitdrukkelijk out of scope op deze kaart:
`spec/schema/uchar-case-v0.schema.json` (**beschermd**), `ref/bin/runner.ml`,
`backends/clingo/bin/clingo_runner.ml`, en `scripts/check-rood.sh`
(**beschermd**) voor de reden-regel of het fragment van het Drools-been. De
kandidaat-map is daarom **verwijderd**; gate 8 staat op 26, vloer 26.

Wat de kaart als "de tweede helft van ch. 4.3's wet" beschreef, is daarmee
preciezer geworden: de **navigatie**-arm van de rol-vs-rol-ambiguïteit is niet
ongepind — de verifier wijst hem statisch af met `WF-053 … role name 'ligger'
is ambiguous from 'Schuit'; name the hop with an explicit role binding`, gepind
door `corpus/negatief/wf053-bindingless-ambiguous`. Alleen de **case-sleutel**
-arm is ongepind, en die is met de zone zoals ze vandaag is niet te pinnen.

**Vervolgkaart aangemaakt:** `backlog/new/20260910-rood-zone-laadfouten.md` —
de rode zone een laadfout-arm geven (schema-zin verbreden van *evaluation* naar
*loading or evaluation*, beide runners `expectedFault` laten lezen vóór de
wereld wordt geladen, het Drools-been een reden-regel geven, en deze getuige
als eerste bewoner), of uitdrukkelijk niet-doen en dat in
`corpus/rood/README.md` vastleggen. De kaart draagt de gemeten gate-8-uitvoer
en de vier artefacten met hun beschermingsstatus. Het bredere belang staat er
ook in: de hele klasse case-laadfouten — onbekende labels, verkeerd getypeerde
rolvullers, dubbelzinnige rolsleutels, scalaire levering op een temporeel
attribuut — is vandaag in **geen enkele** zone gepind, want de negatief-zone
dekt alleen `ir.json`.

### Besluit 5 — de frontend-stilte

**Geen frontend-wijziging.** Vastgelegd als **bekende stilte**: bij rol-vs-rol
emitteert de frontend exit 0, nul bytes stderr en `diagnostics: []` op een
document dat de eigen verifier afwijst (WF-034/WF-035 op de sentinel-typen). De
reden om hem te laten staan: dit is WF-012's v0-fail-safe die zich gedraagt
zoals `IR-SPEC.md` hem beschrijft ("v0 resolves navigation fail-safe (a
non-unique match returns 'no navigation' downstream); v1 moves the guarantee to
declaration time"), en de wal staat er al — gate 3 vangt het document. De
strenge lezing (een WARNING met de niet-geresolveerde navigatie erin) is een
**conformance**-kwestie tussen frontend en verifier, niet een wire- of
semantiekkwestie, en ze kost een `frontends/regelspraak`-wijziging met een
JUnit-getuige. Buiten deze kaart gehouden, hier genoteerd.

### Besluit 6 — geen golden

Bevestigd, en de prijs is nu gemeten in plaats van geschat: elke golden van
deze vorm valt na de poort uit het PLAYED-kanaal in **beide** takken, en kost
dus een naam met reden in `EXPECTED_CHANNEL` in `scripts/check-drools.sh` — een
beschermd pad — voor een vorm waarvan de weigering het gewenste antwoord is.
`EXPECTED_PLAYED` blijft 120, `EXPECTED_CHANNEL` ongewijzigd. De getuigen
staan in de eigen JUnit-suite van de backend, die gate 12 draait.

### Gates

**Alle dertien**, zelf gedraaid op deze kop, na de wijziging:

| gate | uitkomst |
| --- | --- |
| 1 self-contained | PASS |
| 2 ir-schema | PASS (193 bestanden valideren en round-trippen, vloer 192) |
| 3 verifier | PASS (193 golden schoon; 362 negatief afgewezen, vloer 350) |
| 4 case-schema | PASS (454 case-bestanden valideren) |
| 5 oracle | PASS (ref-suite 496 worked examples, vloer 489; 428 cases exact, vloer 427) |
| 6 clingo | PASS (backend-suite 92 checks, vloer 83; 428 cases ref ≡ clingo) |
| 7 no-op | PASS (392 cases weerleggen een niets-doende uitvoerder; 36 gepinde survivors) |
| 8 rood | PASS (26 rode cases, vloer 26; Drools-been 6 fault-pinned, 20 not-carried) |
| 9 catala | PASS (frontend-suite 201 checks, vloer 201; 12 catala-goldens byte-identiek) |
| 10 regelspraak | PASS (JUnit 173 tests, vloer 173; 38 rgs-goldens byte-identiek, vloer 38) |
| 11 toka-parity | PASS (45 .rgs byte-identiek, vloer 45; TOKA-suite 27 tests; ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED / 0 EXCLUDE / 0 FAIL-or-ERROR) |
| 12 drools | PASS (**played 120**, ongewijzigd; JUnit **266**, vloer 239; 44 auto-excluded, 1 value-divergence, 2 no-case; kanaalvloer 47 met naam gepind) |
| 13 reftype-derivation | PASS (74 twists over 15 goldens, vloer 74; 70 replays, 13 door de DRL-tekst-tak PLAYED, vloer 13; 0 survivors) |

Gates 12 en 13 zijn apart gedraaid, zoals de acceptance eist. Gate 13 raakt de
poort niet: de twist verandert eenheidsannotaties, nooit rolnamen of
ledennamen, dus `heeftReferentieVeld` beantwoordt op elk getwist document
dezelfde vraag met hetzelfde antwoord — gemeten en niet alleen beredeneerd, de
tellers zijn identiek aan de vorige kop. Gate 11 is het onafhankelijkste
signaal voor over-afwijzing naast gate 12: 29 van 29 TOKA-cases blijven PLAYED
in de Drools-backend, nul nieuwe per-vorm-weigeringen.

### Voorgestelde CONSTRUCTS-zin

Er is er één, en hij hoort op **rij 4** (*Fact types and role navigation*) —
niet op een eigen rij: dit is geen nieuw construct maar een grens van de
bestaande navigatiecontract-rij, precies waar rij 4 al "fix the navigation
contract (reference-field assumption)" zegt.

> **Landed (2026-09-10):** rolnamen en objecttypeledennamen zijn gescheiden
> naamruimten op de v0-wire (`IR-SPEC.md` §2.6, in de WF-002/WF-012-buurt,
> zonder nieuw WF-nummer): een rol die zijn naam deelt met een attribuut of
> kenmerk van een objecttype dat hetzelfde feittype relateert is **welgevormd**,
> want een genavigeerde lezing noemt haar rolhop
> (`navigatiePad`/`navigatieBindingen`) en haar attribuut in verschillende
> velden. Weigeren zou over-afwijzing zijn: `ref` en de ASP-backend voeren de
> vorm gemeten correct uit. De **bean-vormige** consument is de uitzondering —
> het scalaire referentieveld landt aan het einde wiens verbindende rol to-één
> is, dus niet alleen aan de veel-zijde maar ook op beide einden van een
> 1:1-feittype en op de ene bean van een zelfgetypeerd paar — en die is nu ook
> zo geïmplementeerd: `backends/drools` weigert de vorm per constructie in
> **beide** takken, met het verdrongen referentieveld erbij, waar de
> exec-model-tak tot 2026-09-10 stil `leeg` las (gemeten: `verwacht 0,
> werkelijk leeg` op een verifier-schoon, ref-schoon document) en de DRL-tak op
> een blote compilerfout omviel. **Vijf** lezers gepoort: de vier navigerende
> (`navReader`, `navKetenReader`, `navComparisonRole`, `navReactiviteit`; de
> DRL-tak centraal in `naviResolve`) en de §8.1.7-rollidmaatschapscheck, die
> niet navigeert maar hetzelfde veld null-test en tot deze kaart in **beide**
> takken een regel vuurde die ref en clingo ongevuurd laten (`verwacht leeg,
> werkelijk 8`). Vier rood-eerst-getuigen in `OpslagrichtingWitnessTest`, de
> redenzin gedeeld in `FactModelGenerator.geenReferentieveldReden`; nul
> over-afwijzing gemeten (gate 12 `played 120` ongewijzigd,
> `EXPECTED_CHANNEL` ongemoeid; gate 11 29/29 TOKA-cases PLAYED). De vorm is
> bereikbaar maar onbewoond: 585 documenten over vier zones, nul botsingen.
> **Twee gaten blijven open en zijn gekaart, niet gedicht:** de overige lezers
> van dat veld (far-side rolcheck, twee aggregatie-bronbindingen, kenmerk-
> tijdreeks, verdeling, feitcreatie, plus de tweede afleiding in
> `toOneAantalReader`) zijn ongepoort én ongemeten —
> `backlog/new/20260910-drools-ongepoorte-rollezers.md`; en de
> **case-sleutel**-arm van de rol-vs-rol-ambiguïteit heeft geen rode getuige,
> want beide uitvoerders weigeren hem identiek (`role key "ligger" is ambiguous
> from "Schuit"`) maar bij het LADEN van de case, en het rode zonecontract dekt
> alleen evaluatiefouten — `backlog/new/20260910-rood-zone-laadfouten.md`. De
> navigatie-arm van diezelfde ambiguïteit is wél gepind: WF-053,
> `corpus/negatief/wf053-bindingless-ambiguous`.

### Wat bewust niet bewoog

- **Geen WF-regel, geen definitiefout.** De kaart schreef de redenering uit; de
  hermeting bevestigt haar grond: `ref` en clingo geven op elke schaduwvorm het
  juiste antwoord, en de wire heeft geen positie waarop rol en lid botsen.
- **`ref/`, `backends/clingo`, `spec/verifier/**`, `spec/schema/**`,
  `frontends/regelspraak/**`**: niet aangeraakt. De frontend is alleen
  read-only gebruikt om de meetdocumenten te emitteren.
- **`scripts/check-rood.sh`**: niet aangeraakt. Er was geen reden-regel nodig,
  want de rode case landde niet — en zou hij geland zijn, dan was die regel als
  voorstel gemarkeerd.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`**: niet aangeraakt; de rij-4-zin
  staat hierboven als voorstel.
- **De aggregatie-lezer en de case-invoerpoort**: ongewijzigd. De nieuwe poort
  zit op de vier navigatie-lezers en op de rollidmaatschapscheck; de drie eerder
  gesloten lezers vragen dezelfde gedeelde functie en zijn niet verplaatst.
  Alleen hun redenzin verhuisde naar `FactModelGenerator`, letterlijk gelijk.
- **De zeven ongepoorte lezers.** Bewust NIET meegenomen: ze zijn ongemeten, en
  meten kost per lezer een eigen model plus vier uitvoerderruns. Ze zijn met
  regelnummer en met een aanpak per lezer gekaart in
  `backlog/new/20260910-drools-ongepoorte-rollezers.md`, samen met de
  §8.1.7-meting als bewijs dat de klasse echt stil fout kan zijn. Dat is een
  bekende, benoemde restlacune — geen groene deelverzameling die als groen wordt
  gepresenteerd.
- **De generieke skip-teksten** van de aggregatie- en globals-armen: ongemoeid.
  `navReden` verdringt nooit een bestaande scherpe reden; hij vervangt alleen de
  generieke scope-notitie op de vier plekken waar een gepoorte lezer null kan
  hebben teruggegeven.
- **Geen nieuw WF-nummer, ook niet voor het consumentgedrag.** De drie
  normatieve MUSTs die de eerste versie van de spec-alinea droeg zijn
  teruggeschreven naar beschrijvend proza: `IR-SPEC.md`'s bindende conventie
  eist bij elke MUST een `WF-###` met verifier-check, golden en negatief, en dit
  gedrag hoort niet in `spec/verifier/` thuis — een verifier die het afdwong,
  zou één doelformaat aan elke consument opleggen. De pinnende artefacten zijn
  de JUnit-suite van de backend en gate 12, en de alinea zegt dat nu ook.
