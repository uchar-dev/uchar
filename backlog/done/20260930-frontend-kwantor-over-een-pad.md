# frontend-kwantor-over-een-pad: een kwantor of vouw over een pad van twee relaties

*Grens gevonden door een regelsetbouwer, 2026-09-30 (brug K6 in zijn
clusterdocument), gemeten door de hoofdsessie op uchar `c16cb4d6` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in `/tmp/v2/probe/k6/`
(source.rgs, bron-brug.rgs, scenario.rgs, cases/; alleen lezen). Luid in alle
uitvoerders (de frontend lowert de zin niet en laat een marker op de draad
staan; ref en clingo falen op de onopgeloste voorwaarde, de exec-tak
markeert), geen fout antwoord. Op verzoek van de eigenaar geschreven
(2026-09-30), zodat de grens niet vergeten wordt; geen bouwdatum.*

## Aanleiding

Een kwantorvouw noemt precies één relatie vanaf het onderwerp van de regel
(IR-SPEC ch. 4.5): "ten minste één van de vondsten van de Jutter". Zodra
de verzameling twee stappen ver ligt — het onderwerp, een to-one-rol naar
een tussenobject, en de to-many-rol van dat tussenobject — weigert de
frontend: "de kwantorvouw over 'ten minste één van de vondsten van de
melder van de Melding' wordt NIET gelowerd: een meerstapsbereik: de zin
stapelt nog een verzameling onder de gekwantificeerde, en een kwantorvouw
noemt precies één relatie vanaf het regelonderwerp (IR-SPEC ch. 4.5)".
Hetzelfde geldt voor een vouw over twee relaties in een waarde.

De omweg is een hulpkenmerk op het tussenobject, afgeleid met een kwantor
van één stap, dat de regel dan één stap ver leest (bron-brug.rgs, PASS in
de drie uitvoerders). Dat werkt, maar het kost per tussenobject een lid
dat de bron niet kent, en de regel zegt niet meer wat de norm zegt: de
norm spreekt over de leden van de groep van het onderwerp, de regel over
een kenmerk van de groep. Een regelset met een tweelaagse structuur (een
object, zijn groep, de leden van die groep) loopt hier bij elke norm over
de leden tegenaan.

Dit is geen bug: de grens staat in het IR. Het opheffen is een
ontwerpkeuze met twee wegen, hieronder als acceptatie (b).

## Minimale reproductie

`c16cb4d6`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Jutter (mv: Jutters) (bezield)
    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (geheel getal);
        is zwaar kenmerk (bijvoeglijk);
    Objecttype de Melding (mv: Meldingen)
        is dringend kenmerk (bijvoeglijk);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten
    Feittype MeldingVanJutter
        de melder Jutter
        de melding (mv: meldingen) Melding
    Eén melder heeft meerdere meldingen

    Regel zwaar
        geldig altijd
            Een Vondst is zwaar
            indien zijn gewicht groter is dan 10.

    Regel dringend
        geldig altijd
            Een Melding is dringend
            indien ten minste één van de vondsten van de melder van de Melding zwaar is.

    Scenario: Jutter J met Vondst V (gewicht 12) en Melding M (melder J); verwacht: V is zwaar, M is
    dringend.

    R1  source.rgs zoals hierboven.
        nu: check exit 0 met twee WARNINGs ("… wordt NIET gelowerd: een meerstapsbereik …" en
        "onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8)
        … blijft als marker op de wire staan"); scenario MARKER "transpiler SKIP: regel: dringend —
        regel (voorwaarde buiten Phase-B scope): dringend"; ref "EVALUATION FAULT: Unresolved
        condition reached in evaluation"; clingo "TRANSLATION FAULT: Unresolved condition reached in
        translation: this rule fails observably".
    R2  bron-brug.rgs: het hulpkenmerk `de zware vondst` op de Jutter ("Een Jutter heeft de zware
        vondst indien ten minste één van de vondsten van de Jutter zwaar is") en de regel leest
        "indien de melder van de Melding de zware vondst heeft".
        nu: check exit 0 zonder meldingen; scenario PASS.

## Acceptatie

- (a) Meet de vormruimte in de frontend en de drie uitvoerders: de kwantor
  "alle", "ten minste één" en "geen" over to-one → to-many en over to-many →
  to-many; een vouw (aantal, som, minimum, maximum, eerste, laatste) over
  twee relaties in een waarde; drie stappen; de toets op de elementen als
  kenmerk, als vergelijking en als leeg-toets; en de omweg van R2 als de
  vorm die moet blijven spelen.
- (b) Eén van twee wegen, door de eigenaar te kiezen. *Standaard:* een
  lowering in de frontend die het hulpkenmerk (of, voor een vouw, het
  hulpattribuut) op het tussenobject zelf genereert, zoals de
  opgave-markering haar leden genereert (DIVERGENCES §7): de auteur
  schrijft de zin van de norm, het IR, de verifier en de uitvoerders blijven
  ongewijzigd, de gegenereerde namen zijn vast en staan in het rapport; de
  regel die het pad leest wordt de lezing van één stap. *Alternatief:* een
  pad in de kwantorvouw van het IR (IR-SPEC ch. 4.5 en de verifier), met de
  lowering in ref, clingo en beide Drools-takken; groter, maar zonder
  gegenereerde leden. Rood-eerst JUnit-getuige met R1.
- (c) Golden (werknaam `jutterswet-dringende-melding`), handgerekend, in
  ref ≡ clingo ≡ Drools-exec; de pins van gate 10 en 12 als **voorstel**
  waar de golden telt; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). DIVERGENCES: een eigen sectie voor
  de lowering, of IR-SPEC ch. 4.5 bij het alternatief.

May touch (standaardweg): `frontends/regelspraak/**`, `corpus/golden/<nieuwe
golden>/`, `scripts/check-*.sh` (pins, **voorstel**), kaartverplaatsing.
Niet: `ref/`, `backends/clingo/`, `backends/drools/`, `spec/` (bij het
alternatief anders, dan eerst een spec-voorstel). NB: geen bouwdatum; te
wegen zodra een regelset meer dan één zo'n brug draagt.

## Aanvulling (2026-10-01, uit de landing van drools-exec-voorwaarde-twee-rollen-ver)

Een telling over een pad van meer dan één stap kapt de frontend stil af:
"het aantal havenjutters van de haven van de jutter van de Vondst" komt op
de draad als een telling met `sourceRole` `JutterHeeftVondst.jutter` en
argument `jutter`, dus over de jutter van de Vondst; `check` meldt niets
(geen WARNING meerstapsbereik), de verifier laat het document door, en ref
en clingo antwoorden 1 waar 2 hoort. Een telling over een bereik van meer
dan één stap is volgens WF-102 (c) ongeldig. Dat is ernstiger dan de
weigering hierboven: een stil fout antwoord in drie uitvoerders.
Reproductie: `/tmp/v2/probe/vtr-bouw/`, vorm x-veel-aantal (gemaakt door
maak.py; alleen lezen). De acceptatie van deze kaart omvat de vorm: de
lowering (standaardweg) of, tot die er is, een benoemde weigering in de
frontend moet ook de telling dekken.

## Aanvulling (2026-10-02, uit de ronde frontend G4+G5)

De ronde nam de standaardweg van deze kaart (een gegenereerd lid, geen pad
op de draad) voor één vorm: een WAARDEVOUW over één verzameling gevolgd
door to-one stappen ("de maximale waarde van het gewicht van de weging van
de vondst van alle jutters van de Ploeg"). Het elementtype krijgt een lid
"<lid> via <rol 1>, … en <rol n>", een gegenereerde regel zet het uit de
keten, en de vouw loopt erover (DIVERGENCES, "A fold over a set and to-one
steps after it"; golden `jutterswet-zwaarste-weging`). Een som of telling
daarover telt per element.

Open blijven, gemeten op de kop van de ronde (`126c71cd`):
- R1 van deze kaart, de kwantor over een pad (to-one → to-many: "ten
  minste één van de vondsten van de melder van de Melding"): ongewijzigd,
  `check exit=3` met de WARNING "… wordt NIET gelowerd: een
  meerstapsbereik …", ref `EVALUATION FAULT: Unresolved condition …`,
  clingo `TRANSLATION FAULT …`, exectak `EXCLUDE …`. Luid. De standaardweg
  hier is een gegenereerd KENMERK op het tussenobject (de melder), van
  dezelfde vorm als het lid van de ronde.
- De telling over een pad van meer dan één stap (de vorm van de eerste
  Aanvulling, "het aantal havenjutters van de haven van de jutter van de
  Vondst"): op de kop van de ronde nog STIL afgekapt tot een telling over
  `JutterHeeftVondst.jutter` (gemeten op de reproductie
  `/tmp/v2/probe/vtr-bouw/proef/veel-aantal/`: `check exit=3` alleen om de
  exectak, de telling op de draad met `sourceRole`
  `JutterHeeftVondst.jutter`). Een telling telt exemplaren, dus het lid
  van de ronde past er niet op. **Luid sinds de naronde van 2026-10-02**
  (`backlog/done/20261002-naronde-klein-g4-g5.md`): waar de emitter nog
  een bereik zou schrijven (de kop op naam, of het typepaar van onderwerp
  en element) en de geschreven bron verder loopt, weigert de frontend met
  een ERROR die het pad noemt ("… is een telling over een pad van 3
  stappen (jutter, haven, havenjutters): deze frontend telt alleen over
  één relatie vanaf het onderwerp (IR-SPEC ch. 4.3, WF-102 (c)); schrijf
  de telling op het bereikte object of noem één relatie"), `check` exit 1;
  ook een anker van een ander type ("het aantal havenjutters van de
  Haven" op een Vondst) en een selectie van een unie met een stap na haar
  kop, die eveneens stil afkapten. Waar geen bereik te schrijven is,
  houden de vormen hun eerdere luide weigering (een vouw zonder bereik,
  een geschreven stap, de marker met WARNING bij "van alle …"). De
  lowering van een telling over een pad (een lid per element telt hier
  niet; een vlakke vereniging onder `count` is op de draad niet
  uitdrukbaar, WF-102 (c)) blijft open met deze kaart.
- Een vouw over een to-many stap NA de verzameling ("de inhoud van de
  kisten van de vondst van alle jutters"): de oude WARNING en marker
  (luid).

## Besluit en acceptatie (2026-10-20, eigenaar; vervangt (b)–(d) hierboven)

Gemeten op main `4f3d2a8e` (proeven en tabel: `uchar-dev/werk/kwantor-pad/meting.md`, buiten de repository): het IR
draagt een kwantor over een pad al. `QuantifierFold` heeft sinds 2026-09-16 een `sourcePath` (IR-SPEC ch. 4.5,
WF-108 (c)), de verifier toetst hem, en ref en clingo voeren hem uit als platte vereniging (RUNTIME-SEMANTICS ch.
3.4b; golden `jutterswet-ketenkwantor`, handgeschreven). De frontend schrijft hem niet: een kenmerktoets over een pad
geeft de WARNING meerstapsbereik plus een marker (EF-019 in alle uitvoerders), een vergelijking of leeg/gevuld-toets
over een pad de ERROR "setlezing buiten een vouwbron". De Drools-loader weigert `sourcePath` op een `QuantifierFold`
(DROOLS-026). Een waardevouw over een pad speelt al overal (`sourcePath`, in Drools via
`transpiler/core/SourcePath.java`), ook to-many → to-many en over drie stappen. Het aantal over een pad weigert de
frontend; het IR laat die grens bewust open (IR-SPEC ch. 4.3, "No `sourcePath` at a selection").

De eigenaar koos 2026-10-20 de weg **pad op de draad**: de frontend schrijft `QuantifierFold.sourcePath`, de
Drools-backend leert hem. Geen spec- of IR-wijziging, geen gegenereerde leden. Aanleiding nu: de v2-regelset van de
werkbank omzeilt de grens met brug K6 (een kwantor to-one → to-many in de voorwaarde van een `Uitzondering op`) en
met een regelset-intern feittype voor een gevuld-toets die in het CGM twee verzamelingen ver ligt.

Acceptatie:
- (a) Rood eerst: een JUnit-getuige met R1 die faalt op main.
- (b) De frontend lowert een kwantor over een pad van twee of meer stappen naar `QuantifierFold` met `sourcePath`:
  alle kwantoren (alle, geen, ten minste/ten hoogste/precies n, ook ten minste één), elke toets (kenmerk, ook een
  kenmerk dat als "is een <naam>" gelezen wordt; vergelijking; leeg/gevuld), to-one en to-many stappen in elke
  volgorde, en in de voorwaarde van een gewone regel en van een defaults-groep. Wat niet bindt, blijft luid.
- (c) Drools: de loader laat `sourcePath` op een `QuantifierFold` toe en beide takken voeren hem uit via de
  bestaande `SourcePath`-planning (één analyse, twee weergaven). Wat het beanmodel niet draagt, weigert luid met een
  code uit het register.
- (d) Golden `jutterswet-dringende-melding` met `source.rgs` en `scenario.rgs`, handgerekend in `NOTE.md` vóór een
  uitvoerder draaide, met ten minste R1, de vorm van K6 (in een `Uitzondering op`), een gevuld-toets over drie stappen
  (to-one, to-many, to-many), een kwantor over to-many → to-many en een tellende kwantor; ref ≡ clingo ≡ Drools (beide
  takken).
- (e) Het aantal over een pad blijft een benoemde weigering, ongewijzigd.
- (f) DIVERGENCES: de sectie over het meerstapsbereik zegt wat nu gelowerd wordt. Alle zestien gates groen op de
  eindstand. Pins en vloeren in beschermde scripts (onder meer de pin van `jutterswet-ketenkwantor` en
  `EXPECTED_PLAYED` in `scripts/check-drools.sh`) als voorstel, met reden; de tekst voor `CLAUDE.md` en
  `spec/CONSTRUCTS.md` in de Result.

May touch: `frontends/regelspraak/**`, `backends/drools/**` (niet de productieruntime onder `runtime/`),
`corpus/golden/jutterswet-dringende-melding/`, `scripts/check-*.sh` (pins en vloeren, voorstel), de kaart.
Niet: `ref/`, `backends/clingo/`, `spec/`, andere goldens (behalve een pin die verandert omdat een golden gaat spelen).
Landen na akkoord van de eigenaar.

## Result (2026-10-20)

Pad op de draad, zoals besloten. Commits op `card/frontend-kwantor-over-een-pad` (op main `af10faf3`):

- `756e6c34` frontend: de kwantor over een pad lowert naar `QuantifierFold.sourcePath`.
- `1822bdce` golden `jutterswet-dringende-melding`: `source.rgs`, `scenario.rgs` en `NOTE.md` met de handberekening,
  gecommit vóór een uitvoerder op de golden draaide.
- `a942dbee` Drools: de lader draagt `sourcePath` op een `QuantifierFold`, de TMS-analyse plant het pad; register.
- `8f39906a` golden: `ir.json` en de vier cases uit de frontend; gate-12-pins (voorstel).

Geen wijziging van `spec/`, `ref/`, `backends/clingo/`, de productieruntime of andere goldens.

### Wat er veranderde

- **Frontend** (`ExpressionBuilder`, `RuleChecker`). Een ledenzin met meer dan één geschreven stap (de
  kenmerkroute, `quantifiedSubject`) loopt dezelfde wandeling als het bereik van een waardevouw (`walkRange`, nu
  gesplitst in `rangeSegments` en `walkSegments`): ledennaam en onderwerp worden segmenten, het anker wordt na de
  langste naam van het regelonderwerp afgeknipt (typenaam in de keten, of naam/meervoud van een rol die de keten
  vult) en de rest gaat als voorvoegsel terug naar het kenmerk. De vergelijkings- en leeg/gevuld-route
  (`quantifierFold`) neemt de al opgeloste `rangePath` over als `sourcePath` in plaats van te weigeren. Een
  wandeling die niet bindt houdt de WARNING `RGS-213`, nu met de reden van de wandeling (de stap die geen rol bindt,
  een anker dat het onderwerp niet is); een pad dat helemaal niet te lezen is (bezittelijk anker "van zijn melder")
  houdt `PART.fold-multi-step`, waarvan de tekst nu die restgroep beschrijft. Nieuw: ERROR **`RGS-348`** wanneer de
  toets van een kwantor over een pad een operand heeft dat de frontend vanaf het regelonderwerp bouwde en dat iets
  anders leest dan een eigen attribuut van het element (of een instantievouw): vanaf het element leidt over een pad
  geen weg terug (p14). Over één stap blijft de terugbinding zoals zij was (q02). Een operand dat `elementRead`
  opnieuw vanaf het element bouwde ("de leeftijd van de vinder van de Vondst") bindt. De emitter schreef
  `sourcePath` op een kwantorvouw al; die bleef ongewijzigd. De telling over een pad blijft geweigerd (p10, `RGS-131`
  in die vorm; `RGS-072` waar nog een bereik geschreven zou worden).
- **Drools**. `IrLoader` laat `sourcePath` op een `QuantifierFold` door (vorm eerst, via `roleBindingList`).
  `TmsTranspiler.quantifierContributions` kiest `pathRange` (de bestaande `SourcePath`-planning, met `Exists` over
  tussenknopen zodat een lid één keer telt) of `oneStepRange`. Een lidtoets die kan falen of deponeren laat elk lid
  zijn plaats dragen; waar het pad meer routes per lid toelaat (een stap naar veel gevolgd door een naar één) zou de
  bijdrageregel het lid per route binden en de herhaling het dubbel lezen, dus die vorm weigert luid met de nieuwe
  **`DROOLS-291`** (`quantifier-member-two-routes`, LIMIT). Waar het pad samenkomt (`plan.meets()`) speelt zij.
  **`DROOLS-026`** (lader) en **`DROOLS-258`** (transpiler) vallen nergens meer; zij blijven in het register met de
  nieuwe soort `Kind.RETIRED` (`spec/proposals/MESSAGE-CODES.md` §3.2: "a retired code stays in its registry marked
  retired"). `scripts/check-drools-codes.sh` (voorstel) laat die soort toe, eist haar niet als gevallen code en
  faalt als een plek een gepensioneerde code toch noemt; tot nu toe was er geen mechanisme om een code te laten
  vervallen (nooit eerder gebeurd).
- **DIVERGENCES**: nieuwe sectie "A quantifier over a written path — the range is the path (2026-10-20)", plus
  verwijzingen in "§13.4.1 A navigated read binds its written role" (de matrix), "§13.4.15.45 …" (het afknippen van
  het anker over een pad) en "A count over a written path — refused" (wat deze kaart wel en niet deed).

### Rode getuige (acceptatie (a))

`QuantifierOverPathTest.r1_lowers_to_a_quantifier_fold_over_the_path`, op de onveranderde frontend van `ef4189bb`
(volledige uitvoer `uchar-dev/werk/kwantor-pad/bouwer/red-witness.txt`):

    r1_lowers_to_a_quantifier_fold_over_the_path() ✘ onverwachte diagnostiek: [WARNING (line 24): de kwantorvouw
    over 'ten minste één van de vondsten van de melder van de Melding' wordt NIET gelowerd: een meerstapsbereik: …,
    WARNING (line 24): onopgelost: … blijft als marker op de wire staan] ==> expected: <true> but was: <false>
    [ 1 tests failed ]

Groen sinds `756e6c34`, met zeven rijen erbij (elke kwantor en toets, stappen in elke volgorde, de rechtvaardiging
van een uitzondering, RGS-348, een lezing vanaf het element, een stap die niet bindt, de telling over een pad).
Drools: `QuantifierPathWitnessTest` (samenkomend pad met deponerende toets speelt in beide takken; twee routes:
DROOLS-291 in beide takken), documenten onder `backends/drools/src/test/resources/quantifier-path/`.

### Proeftabel (`werk/kwantor-pad/probe/`, vóór op `af10faf3`-stand, na op `8f39906a`)

s02 draagt in de werkmap de tikfout "R2 is niet na een startperiode"; gedraaid met "R2 is niet gestart". Uitvoer:
`werk/kwantor-pad/bouwer/probes-before.txt` en `probes-after.txt`.

| proef | vóór: frontend; ref/clingo; exec; drl | na |
|---|---|---|
| p01 ten minste één, kenmerk | exit 0 + 2 WARNINGs; EF-019 in alle vier | exit 0 schoon; PASS in alle vier |
| p02 alle, kenmerk | idem | idem |
| p03 geen, kenmerk | idem | idem |
| p04 vergelijking | ERROR setlezing, exit 1 | exit 0 schoon; PASS in alle vier |
| p05 leeg-toets | ERROR setlezing | idem |
| p06a/p06b to-many → to-many | WARNINGs; EF-019 | exit 0 schoon; PASS in alle vier |
| p07 drie stappen, vergelijking | ERROR setlezing | exit 0 schoon; PASS in alle vier |
| p08 som / p11 som veel-veel | schoon; alle vier "leeg" waar de proef 0 verwacht | ongewijzigd (proefverwachting, geen uitvoerdersverschil) |
| p09 maximum, p12 omweg R2, q01, q02, s01 | schoon; PASS in alle vier | ongewijzigd |
| p10 aantal over pad | ERROR RGS-131, exit 1 | ongewijzigd (acceptatie (e)) |
| p13 Uitzondering op (K6) | WARNINGs; EF-019 (Drools: regel "defaults K:Melding.dringend") | exit 0 schoon; PASS in alle vier |
| p14 toets leest het onderwerp | ERROR setlezing | ERROR **RGS-348** die de lezing 'de drempel van de Melding' noemt; geen document |
| q03 tellend over pad | WARNINGs; EF-019 | exit 0 schoon; PASS in alle vier |
| s02 gevuld over drie stappen | ERROR setlezing | exit 0 schoon; PASS in alle vier |

Daarnaast los gemeten (niet in de werkmap): meerwoordig kenmerk na het anker ("boven de norm"), nominaal kenmerk
("een recht op grond-van-de-wia is"), "niet"/"geen" in het anker, die/dat-deelselectie over een pad (filter op het
element, zoals bij een waardevouw), to-one → to-one ("alle ploegen van de vinder van de Vondst") en to-one → to-many →
to-one met een gedeelde kist: alle schoon en PASS in de vier uitvoerders; "van zijn melder" en een stap die geen rol
bindt: WARNING plus marker.

### Golden `jutterswet-dringende-melding` (acceptatie (d))

Vijf vormen (lid 2–6 van de eigen casustekst Art. 3l in `NOTE.md`): R1; K6 als `Uitzondering op voorrang` met
"heeft geen voorrang" en het nominale kenmerk "een wrakstuk"; gevuld over drie stappen (melder, vondsten, wegingen);
`alle` met vergelijking over jutters → vondsten; `ten minste twee` over vondsten → kist, waar een kist via twee
vondsten bereikt wordt. Handberekening tegen uitkomst: alle 24 verwachtingen in vier cases zoals gerekend; ref en
clingo PASS (10, 5, 5, 4), exec-model en DRL-tekst PLAYED 4. De scheidende cellen: M3 (bereik, niet populatie), M5 in
case 2 (de tweede tussenvondst), P3/P4 (lege vereniging is waar onder `alle`), J1 in case 4 (vereniging telt K1 één
keer: 1, voorkomens zouden 2 geven). `--noop` faalt op elke case. `ir.json` uit `source.rgs` alleen is bytegelijk
aan dat uit bron plus scenario (gate 10 en 16).

### `jutterswet-ketenkwantor` (meting, niets toegevoegd)

- De frontend schrijft de twee zinnen van haar `NOTE.md` (met een eigen model van haar `ir.json`, bron in
  `werk/kwantor-pad/bouwer/ketenkwantor-source.rgs`) **bytegelijk** naar het handgeschreven `ir.json`.
- Drools: speelt niet; de weigering schoof een laag dieper, van `EXCLUDE DROOLS-026` (lader) naar `EXCLUDE
  DROOLS-067` ("sourcePath step 1 (Boot -> Vondst) has no bean field": de many-to-many `berging`), in beide takken
  dezelfde zin, zoals de pin van 2026-09-22 al vermoedde. Pin verplaatst met reden (voorstel).

### Gates (acceptatie (f))

`bash werk/gates.sh … gates-1` op `8f39906a`, `werk/kwantor-pad/gates-1.summary.txt`: **EXIT 0**, alle zestien
`rc=0` (13:29–13:48). 1 PASS; 2 349 bestanden; 3 349 golden schoon, 511 negatief; 4 879 cases; 5 ref-suite 1304,
835 cases; 6 clingo 2198 checks, 835 cases; 7 790 cases, 45 survivors; 8 44 rode cases; 9 241 checks; 10 JUnit 1218,
126 rgs-goldens bytegelijk, 348 RGS-codes; 11 29 TOKA-cases; 12 261 gespeeld in beide takken, JUnit 1632, 291
Drools-codes waarvan 2 gepensioneerd; 13 43 twists, 122 replays; 14 150 checks; 15 273 van 310 goldens vergeleken
(was 271 van 309: de nieuwe golden en `jutterswet-ketenkwantor`, die de lader nu laadt), 126 op de bronroute; 16 31
scenario-goldens, 106 cases.

### Veranderde tests (gedrag dat deze kaart bewust verandert)

- `SilentMarkerTest.a_multi_step_range_reports_the_stacked_set` → `…_reports_the_step_that_does_not_bind`: de zin
  "alle wachten van alle ploegen van de Haven" lowert nu; de rij gebruikt "alle kades" (geen rol) en pint de
  stapweigering in plaats van "meerstapsbereik".
- `NonTemporalFoldTest.a_multi_step_range_on_the_characteristic_form_stays_a_marker` →
  `…_lowers_to_its_path`: dezelfde zin, nu een `QuantifierFold` met `sourcePath` [havenindeling.ploeg,
  schouwindeling.wacht] en zonder `sourceRole`.
- `MarkerReasonPinTest.SANCTIONED` + `RuleChecker.java#quantifierFold` (1) en `MarkerReportTest.CLASSIFICATION`
  + die familie als ERROR-route: de marker naast RGS-348 heeft reden null, de ERROR is haar diagnose (het patroon
  van `checkWholePeriod`). Beide tests vragen precies dat bij een nieuwe null-plek.
- Drools `WholePeriodQuantifierFoldWitnessTest.a_source_path_is_refused_per_form` →
  `a_source_path_reaches_the_record_and_a_disconnected_walk_is_refused_per_form`: de lader draagt het pad in het
  record; hetzelfde onsamenhangende pad weigert nu in de planning (DROOLS-064), in beide takken gelijk.

### Pins en vloeren (voorstellen in beschermde scripts)

- `scripts/check-drools.sh`: `EXPECTED_PLAYED` 260 → 261 (de nieuwe golden; reden en datum bij de constante);
  `jutterswet-ketenkwantor` `EXCLUDE DROOLS-026` → `EXCLUDE DROOLS-067` met reden. Het aantal EXCLUDE-pins blijft 46.
- `scripts/check-drools-codes.sh`: de soort `RETIRED` (zie boven). Vloeren niet verhoogd: `EXPECTED_MIN_RGS_CODES=347`
  (nu 350, na de fixronde), `EXPECTED_MIN_RGS_MESSAGES=363` (366, idem), `EXPECTED_MIN_DROOLS_CODES=289` (291), `EXPECTED_MIN_DROOLS_MESSAGES=325`
  (326); het zijn ondergrenzen.

### Voorgestelde tekst voor `CLAUDE.md` (gate 12)

In "`scripts/check-drools-codes.sh`, called first, holds the register to the sources in both directions (every code
raised, every raised code registered, …)": vervang "every code raised" door "every code raised but one of the kind
`RETIRED`, which no place may name (a code no place raises any more stays in the register so that its number is never
reused; DROOLS-026 and DROOLS-258 since the card `frontend-kwantor-over-een-pad`, 2026-10-20)". En "each of the 45
`EXCLUDE` pins" → "each of the 46 `EXCLUDE` pins": al op main `af10faf3` staan er 46 (gemeten met
`awk '/^EXPECTED_CHANNEL=\(/,/^\)/' scripts/check-drools.sh | grep -c '|EXCLUDE DROOLS-'`); deze kaart verandert het
aantal niet.

### Voorgestelde rijtekst voor `spec/CONSTRUCTS.md` (rij 40, achteraan in de notitiekolom)

**Landed (2026-10-20, card `frontend-kwantor-over-een-pad`) — the multi-step range has a producer route and a Drools
lowering, and this NARROWS the words "handgeschreven — de producentroute bestaat niet" and "beide Drools-takken
weigeren `Kwantorvouw.bronPad`" above:** the RegelSpraak frontend lowers a quantifier whose members lie two relations or
more from the subject to the fold with `sourcePath` — every quantifier, a characteristic (nominal ones included), a
comparison or a leeg/gevuld test as the matrix, to-one and to-many steps in any order, in a rule's condition and in a
default's justification — over the walk a value fold's range takes; an operand of the matrix built from the rule
subject is refused over a path (RGS-348); where the element has the rule subject's own type, a read of the subject in the
matrix is refused over a path and over one step (RGS-349), and so is a read in a subselection whose onderwerp names that
type (RGS-350, value folds included); and a count over a path stays refused. It writes the two sentences of
`jutterswet-ketenkwantor` byte-identically to that golden's hand-written `ir.json`. Both Drools branches carry the
path through the `SourcePath` planning of the value folds (a member reached along two routes counts once); a matrix
that can fault or deposit over a path with more than one route per member is refused by name (DROOLS-291), and
`jutterswet-ketenkwantor` is refused one layer deeper, on its many-to-many `berging` (DROOLS-067). Witness
`corpus/golden/jutterswet-dringende-melding`, played by all four executors (gate 12 `EXPECTED_PLAYED` 260 → 261).

### Open, met reden

- De toets die over een pad het regelonderwerp leest, weigert (RGS-348) in plaats van te binden. Binden kan alleen
  waar elke stap terug naar één is (een pad van louter stappen naar veel); dat is een eigen keuze, niet gevraagd.
- DROOLS-291: een deponerende of falende lidtoets over een pad met meer routes per lid. Ref en clingo spelen die vorm
  (proef `two-routes`); Drools zou per lid de vroegste route moeten nemen, wat `MemberTest` zonder lididentiteit niet
  kan. Geen golden heeft de vorm.
- Een kwantor over een pad met een bezittelijk anker ("van zijn melder") blijft een WARNING met marker, zoals de
  waardevouw over zo'n anker een ERROR blijft.
- `PLAN.md` hoeft volgens mij niet te veranderen; de scope bleef binnen de kaart.

### Vondsten buiten de kaart

- `CLAUDE.md` noemt 45 `EXCLUDE`-pins voor gate 12; op main zijn het er 46 (zie boven).
- In de werkmap: s02 heeft de tikfout "R2 is niet na een startperiode"; p08 en p11 verwachten 0 voor een som over
  niets, waar alle vier uitvoerders (terecht, ch. 1.2) leeg geven.

## Fixronde na de review (2026-10-20)

Een leesreview van de tak vond twee dingen; de hoofdsessie mat ze na (proeven `werk/kwantor-pad/review/`) en besloot.
Eén commit op de tak, `bed92c3f` (frontend; de kaart in de commit erna). Geen golden, geen verwachting en geen
bestaande bewering veranderd. Proeven vóór op `b120cd03`, na op `bed92c3f`, met `review/run.sh` door frontend,
verifier, ref, clingo en beide Drools-takken; uitvoer `werk/kwantor-pad/review/before-fix.txt`,
`werk/kwantor-pad/fixronde/before.txt`, `before-2.txt` en `after.txt`; eigen proeven in `werk/kwantor-pad/fixronde/`.

### Punt 1 — stil fout antwoord bij een onderwerp van het elementtype

Waar onderwerp en element hetzelfde type hebben, las de frontend in de toets van een kwantor (het operand naast het
gekwantificeerde, gebouwd vanaf het regelonderwerp) en in het filter van een deelselectie ("die aan de volgende
voorwaarde voldoen: …", per element gelezen) een lezing van het onderwerp als lezing van het element: het IR heeft
daar geen verwijzing naar het buitenste onderwerp. RGS-348 viel niet, omdat de lezing een eigen attribuut van het
elementtype is. Nu weigert de frontend met een ERROR die de lezing noemt en zegt waarom:

- **RGS-349** (`quantifier-test-reads-subject-of-element-type`, `RuleChecker.quantifierFold`/`subjectRead`): elke
  lezing van het onderwerp in dat operand ("de leeftijd van de Jutter", "zijn leeftijd", "de drempel van de ploeg van
  de Jutter"), over een pad en over één stap. Bij verschillende typen blijft het zoals het was (RGS-348 over een pad,
  terugbinding over één stap: p14 en q02 ongewijzigd).
- **RGS-350** (`subselection-names-subject-of-element-type`, `RuleChecker.compoundElementFilter`): een lezing in een
  deelselectie waarvan het geschreven onderwerp eindigt op de naam van het type ("… van de Jutter"), in het filter van
  een waardevouw en van een kwantor, over een pad en over één stap. "zijn leeftijd" in een deelselectie is die van het
  element en bindt. Op een waardevouw staat de bestaande WARNING RGS-069 (deelselectie niet omgezet) ernaast.

De waardevouw met filter was de bestaande fout uit de review (op main al stil fout, zelfde oorzaak); de eenstapsvormen
zijn gemeten en zaten in dezelfde code (de waardevouw over één stap in `compoundElementFilter`, de kwantor "alle" over
één stap in `quantifierFold`), dus ze krijgen dezelfde weigering.

| proef | vóór | na |
|---|---|---|
| f1-pad-zelfde-type (kwantor, pad, "de leeftijd van de Jutter") | exit 0 schoon; alle vier "J1 lacks kistplichtig" | ERROR RGS-349, geen document |
| e5 (idem met "zijn leeftijd") | exit 0 schoon; alle vier fout | ERROR RGS-349 |
| e11 (kwantor "alle", één stap over Begeleiding) | exit 0 schoon; PASS alleen omdat het scenario de lezingen niet scheidt | ERROR RGS-349 |
| e12 (kwantor, pad, "de drempel van de ploeg van de Jutter") | ERROR RGS-348 | ERROR RGS-349 |
| f1-een-stap-zelfde-type, e7 ("ten minste één", één stap) | ERROR setlezing buiten een vouwbron | ongewijzigd |
| f3b-som (som over pad, filter) | exit 0 schoon; alle vier leeg waar 50 | ERROR RGS-350 + WARNING RGS-069 |
| e1 (som over één stap, filter) | exit 0 schoon; alle vier leeg waar 50 | idem |
| e2 (aantal over één stap, filter) | exit 0 schoon; alle vier 0 waar 1 | idem |
| e8 (kwantor over pad met deelselectie) | exit 0 schoon; PASS, scenario scheidt niet | ERROR RGS-350 |
| e10 (som over pad, filter "de drempel van de ploeg van de Jutter") | exit 0 schoon; PASS: beide lezingen geven hier dezelfde ploeg | ERROR RGS-350 + RGS-069 (dezelfde naam, dus ook geweigerd) |
| f3-vouw-filter-zelfde-type (aantal over pad) | ERROR RGS-131 | RGS-131 plus RGS-350 en RGS-069 |
| e3, e4 ("zijn leeftijd" in het filter, één stap en pad), e9 (element van een ander type), e6 | exit 0 schoon; PASS in alle vier | ongewijzigd |

### Punt 2 — "niet een <kenmerk>" onder een kwantor

In de vragende volgorde ("… niet een wrakstuk is") komt `niet` in het anker; de EEN-vorm zocht de naam "niet wrakstuk"
en bond niets. In de stellende volgorde ("… is niet een wrakstuk", de `NIET EEN`-tak van de grammatica, regel 1004)
hield `conditionFromNonToplevel` de vouw er helemaal buiten. Nu leest `elementCharacteristicCheck` een `niet` vóór de
naam als ontkenning van het zelfstandige kenmerk (eerst de hele naam, dan de naam na `niet`), en geeft de stellende tak
zijn `NIET` door aan de vouw. Het IR van f2-pad-niet-een is op de snippets na gelijk aan dat van f2-pad-geen. Een naam
na "niet een" die geen zelfstandig kenmerk van het element is, blijft geweigerd (WARNING RGS-213, rij in de test).
De verwachtingen van de f2-proeven nagerekend: M1 heeft melder J1, J1 heeft V1, V1 is geen wrakstuk (niet gezet), dus
M1 dringend; J1 heeft V1, dus kistplichtig; J2 heeft geen vondsten, en "ten minste één" over niets is onwaar.

| proef | vóór | na |
|---|---|---|
| f2-pad-niet-een, f2-een-stap-niet-een | exit 0, WARNING RGS-213 "'wrakstuk' is geen kenmerk van Vondst" + marker; EF-019 in ref en clingo, fout in beide Drools-takken | exit 0 schoon; PASS in alle vier |
| n3 (alle, pad), n4 (geen van, pad) | idem | exit 0 schoon; PASS in alle vier |
| n7, n8 (stellend in een opsomming, pad en één stap) | WARNING "de vorm 'is niet een <naam>' bond geen kenmerk" + marker; EF-019 | exit 0 schoon; PASS in alle vier |
| f2-pad-geen | PASS in alle vier | ongewijzigd |
| n6 ("indien de Vondst niet een wrakstuk is", geen kwantor), n1 (met "hij") | WARNING + marker; syntaxfout | ongewijzigd: kaarttekst hieronder |

### Tests

- Nieuw, rood op `b120cd03` (uitvoer `werk/kwantor-pad/fixronde/red-witness.txt`), groen op `bed92c3f`, in
  `QuantifierOverPathTest`: `a_test_that_reads_a_subject_of_the_element_type_is_refused`,
  `a_filter_that_names_a_subject_of_the_element_type_is_refused` (met een rij die bindt: "zijn leeftijd" in het
  filter) en `niet_een_under_a_quantifier_is_the_negated_characteristic` (met een rij die blijft weigeren).
- Bijgewerkt omdat deze ronde precies dat verandert: `MarkerReasonPinTest.SANCTIONED` en
  `MarkerReportTest.CLASSIFICATION`. De plek `RuleChecker#quantifierFold` noemt nu RGS-348 of RGS-349 als diagnose
  van haar marker (aantal blijft 1); nieuw is `RuleChecker#compoundElementFilter` (1), met RGS-350 als diagnose.
- Alle 126 rgs-goldens bytegelijk; geen van de 179 RegelSpraak-bronnen in de repository geeft RGS-349 of RGS-350.

### Register

`diagnostic-codes.tsv` 348 → 350 codes, beide catalogi 364 → 366 entries; vloeren ongewijzigd (ondergrenzen).

### Gates

`bash werk/gates.sh … gates-2` op `bed92c3f`, `werk/kwantor-pad/gates-2.summary.txt`: **EXIT 0**, alle zestien
`rc=0` (14:27–14:47). Verschil met gates-1: gate 10 JUnit 1221 (was 1218, de drie nieuwe tests) en 350 RGS-codes (was
348); verder dezelfde aantallen (2 349 bestanden; 3 349 golden schoon, 511 negatief; 5 835 cases; 6 835 cases; 10 126
rgs-goldens bytegelijk; 11 29 TOKA-cases; 12 261 gespeeld in beide takken, JUnit 1632; 15 273 van 310 goldens; 16 31
scenario-goldens, 106 cases).

### Niet gedaan, met reden

- "niet een" buiten een kwantor (n6, n1): een andere route (`characteristicCheckPredicateBare`, en voor "hij" de
  grammatica), luid, niet gevraagd. Kaarttekst hieronder.
- Sub- en supertypen: de weigering geldt bij precies hetzelfde type, zoals besloten. Een eigen lezing van een onderwerp
  van een supertype of subtype van het element valt over een pad volgens de code onder RGS-348 (haar type is niet het
  elementtype); gemeten heb ik dat niet, en over één stap of in een deelselectie ook niet.

### Kaarttekst voor `backlog/new/20261020-nominal-negation-in-question-order.md` (niet gebouwd)

    # nominal-negation-in-question-order: "niet een <kenmerk>" in de vragende volgorde

    *Gevonden in de fixronde na de review van frontend-kwantor-over-een-pad (2026-10-20), gemeten op `bed92c3f`;
    proeven `uchar-dev/werk/kwantor-pad/fixronde/n1-subject-niet-een` en `n6-subject-de-niet-een` (alleen lezen).
    Luid, geen fout antwoord.*

    Tabel 16 (§8.1.8.3) ontkent een zelfstandig kenmerk met "is niet een <k>". In de stellende volgorde ("hij is niet
    een wrakstuk") heeft de grammatica daar een tak voor (`nonToplevelOperator`, `NIET EEN kaleNaam`), en die speelt
    (proef `n9-subject-stellend-niet-een`: PASS in ref, clingo en beide Drools-takken). In de vragende
    volgorde van een voorwaarde niet: in "indien de Vondst niet een wrakstuk is" komt `niet` in het onderwerp, de
    EEN-vorm zoekt de naam "niet wrakstuk", die bindt niets, en de voorwaarde blijft een marker met de WARNING
    "onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp" (EF-019 in ref en clingo, een
    evaluatiefout in beide Drools-takken). "indien hij niet een wrakstuk is" is een syntaxfout ("no viable alternative
    at input 'nieteen'"). Onder een kwantor leest de frontend de vorm sinds de fixronde van 2026-10-20 wel
    (`RuleChecker.elementCharacteristicCheck`); "geen wrakstuk" speelt in elke gemeten vorm (proeven n2, n7–n9,
    f2-pad-geen).

    Acceptatie:
    - (a) Rood eerst: een JUnit-getuige met beide zinnen.
    - (b) "indien <onderwerp> niet een <zelfstandig kenmerk> is" lowert naar de ontkende kenmerktoets, gelijk aan
      "geen <k>", voor het onderwerp zelf, "hij" en een navigatie naar één; na "niet een" blijft een ander soort
      kenmerk of een naam die niets bindt luid geweigerd.
    - (c) DIVERGENCES (§13.4.15.45 of de tabel van vorm 1 in §4); alle zestien gates groen.

    May touch: `frontends/regelspraak/**`.

## Landing (2026-10-20, hoofdsessie)

Een leesreview van de tak (copilot, GPT-6 Sol) vond de twee punten van de fixronde hierboven; de hoofdsessie mat ze na
op main en op de tak (`uchar-dev/werk/kwantor-pad/review/`). Na de fixronde resolvet de v2-regelset van de werkbank (33
bronnen, `fundament/rules` en `herleving-ww/rules`) met de frontend van de tak zonder melding, en het IR is bytegelijk
aan dat van main: de nieuwe weigeringen raken v2 niet. Herbaseerd op main `b91185de` (alleen backlog). In de
landingscommit: `CLAUDE.md` (gate 12: 46 `EXCLUDE`-pins, de registersoort `RETIRED`), rij 40 van `spec/CONSTRUCTS.md`
en de kaart `backlog/new/20261020-nominal-negation-in-question-order.md`. Geland na akkoord van de eigenaar.
