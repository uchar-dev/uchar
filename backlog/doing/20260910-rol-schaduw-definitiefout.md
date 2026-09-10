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
