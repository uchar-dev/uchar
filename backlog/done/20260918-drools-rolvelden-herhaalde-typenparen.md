# drools-rolvelden-herhaalde-typenparen: geschreven rol kiest het beanveld

Goal: Het gegenereerde Drools-feitenmodel draagt rol-adresseerbare
referentievelden wanneer meer dan één feittype hetzelfde
objecttypepaar verbindt. Zowel aggregatie als §13.4.7-feitcreatie kan
dan de uniek geschreven rol gebruiken, in plaats van te stranden op
een ambigue typepaarafleiding. De twee constructen blijven in één kaart
omdat hun huidige weigering uit exact hetzelfde
`FactModelGenerator`-contract komt; ze in twee sessies bouwen zou
dezelfde bestanden en hetzelfde opslagbesluit dubbel raken.

Evidence:

- In de oorspronkelijke `PHASE_C` speelde AANTAL over de geschreven
  elementrol `vertrekkende reiziger` terwijl meerdere feittypen
  `Luchthaven` en `Reiziger` verbonden. De rolnaam selecteerde uniek
  het juiste feittype en beide oorspronkelijke Drools-armen leverden de
  gepinde telling.
- De oorspronkelijke `PHASE_G_FEIT` (vier gevallen) en
  `PHASE_GELDIG` (twee datumgevallen) creëerden een feit over een al
  bestaande `Vlucht`-`Passagier`-relatie. De geschreven rol `reis`
  selecteerde de opslag, met anker- en doelguards en een
  geldig-vanafvenster. Beide oorspronkelijke armen speelden.
- Uchar resolveert die geschreven rollen nog steeds, maar
  `FactModelGenerator.leden` vraagt voor gewone cross-typevelden eerst
  om één uniek objecttypepaar. Bij herhaalde paren is dat antwoord null;
  exec geeft `SKIP ... geen referentieveld` en classic DRL een marker.
  Dit is geen bronambiguïteit: de geschreven rol is uniek.

Acceptance:

- Rood-eerst: voeg kleine, zelfgeschreven backendfixtures toe die de
  twee huidige redenen exact reproduceren:
  `aggregatie-join ... geen referentieveld` en
  `feitcreatie ... geen referentieveld`. Leg daarnaast vast dat de
  oorspronkelijke toolchainfixtures voor de zeven gevallen in beide
  manifests `LOWERED` waren en differentieel vuurden.
- `FactModelGenerator` leidt voor een gewone cross-type-relatie één
  scalar referentieveld per uniek resolveerbare to-one-rol af, ook
  wanneer hetzelfde typepaar via andere feittypen voorkomt. De
  veldnaam blijft de rolnaam en het Java-type het type dat die rol
  vult. Bestaande unieke-paren en self-pairs blijven ongewijzigd.
- Verander `RegelSpraakModel.verbindendeRol(typeA,typeB)` niet in een
  gokfunctie. Een consument zonder geschreven/symbolische rol blijft
  een herhaald typepaar luid ambigu vinden. Naamdeling met een
  attribuut/kenmerk, een niet-unieke rolnaam en een to-many-rol zonder
  opslagdragende overzijde blijven afzonderlijk benoemde weigeringen.
- `CaseRunner`, `ContractValidator` en beide transpilers gebruiken bij
  een expliciete `bronRol`/elementrol/`joinVeld`/`zetVeld` hetzelfde
  rol-adresseerbare contract. De invoer- en creatiepaden schrijven
  beide relationele richtingen die RUNTIME-SEMANTICS ch. 5.7a vereist;
  een veld dat alleen de juiste telling oplevert maar de verkeerde
  relatie draagt is niet voldoende.
- Voeg de eigen, rechtenvrije, bron-gedragen golden
  `corpus/golden/jutterswet-dubbelverbinding/` toe met twee feittypen
  over hetzelfde objecttypepaar. De cases scheiden minimaal:
  1. aggregatie over elk van beide geschreven rollen;
  2. feitcreatie wel/niet door ankerkenmerk;
  3. feitcreatie wel/niet door doelkenmerk of doelvergelijking;
  4. vóór en binnen het geldigheidsvenster;
  5. de aangemaakte koppeling aan beide kanten.
  De golden regenereert byte-identiek uit `bron.rgs` en speelt met
  absolute verwachtingen in ref, clingo, exec-model en classic DRL.
- Pin ook de oude `PHASE_C`-telling en de zes
  `PHASE_G_FEIT`/`PHASE_GELDIG`-uitkomsten als backend-JUnitgevallen,
  zonder TOKA-materiaal of een private padverwijzing in artifactzones.
- Verhoog `scripts/check-drools.sh` `EXPECTED_PLAYED` met precies één
  vanaf de bij oppakken gemeten basis voor
  `jutterswet-dubbelverbinding`. Geen bestaande niet-PLAYED golden mag
  door een onbedoelde verbreding van kanaal wisselen.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-regelspraak.sh`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/ContractValidator.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
de minimaal benodigde rolresolutiehelper onder
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java`,
`backends/drools/src/test/**`,
`corpus/golden/jutterswet-dubbelverbinding/`,
`scripts/check-drools.sh` uitsluitend voor de door deze kaart
toegevoegde played telling (beschermd-padvoorstel), deze kaart en bij
afronding haar verplaatsing binnen `backlog/`.

Out of scope: het kiezen van een rol zonder geschreven binding,
naamdelingsvormen, beide-zijden-to-many, ambigue/wederkerige self-pairs,
de conservatieve bestaande opslagrichtinggevallen
`jutterswet-bergingstoezicht`, `jutterswet-erflijn`,
`jutterswet-gemeenteberging` en `jutterswet-penninguitgifte`,
objectcreatie-semantiek, `bronPad`, en de geaccepteerd
dimensioneel-invalide multi-globalfixtures. Geen TOKA-materiaal mag
buiten `toka/` komen. Deze kaart deelt het feitenmodel en beide
transpilers met andere pariteitskaarten en wordt serieel gepland.

---

Result:

**Rood eerst, letterlijk.** Vier getuigen in
`backends/drools/src/test/.../cases/HerhaaldTypepaarWitnessTest.java`,
geschreven en gedraaid vóór elke productie-edit, op modellen die uit de
nieuwe golden zijn teruggebracht tot één constructie per getuige
(commit 89269cbd). De gemeten uitkomsten:

- exec, telling: `transpiler SKIP: regel: liggertal van een haven —
  regel (de aggregatie-join heeft geen beanveld — het beanmodel leidt
  voor de rol 'thuishaven' op Schuit geen referentieveld naar Haven af
  (§13.3.9 opslagrichting)): liggertal van een haven`
- drl, telling: `drl-transpiler MARKER: regel: liggertal van een haven —
  aggregatie-join: het beanmodel leidt voor de rol 'thuishaven' op
  Schuit geen referentieveld naar Haven af (§13.3.9 opslagrichting)`
- exec, feitcreatie: `transpiler SKIP: feitcreatie: winterberging door
  de haven — feitcreatie (het beanmodel leidt voor de rol 'thuishaven'
  op Schuit geen referentieveld naar Haven af): winterberging door de
  haven`
- drl, feitcreatie: `drl-transpiler MARKER: feitcreatie: winterberging
  door de haven — feitcreatie: het beanmodel leidt voor de rol
  'thuishaven' op Schuit geen referentieveld naar Haven af (§13.3.9
  opslagrichting)`

Na de implementatie vallen alle vier om ("Expected Excluded to be
thrown, but nothing was thrown"); ze zijn omgezet naar de nieuwe wet en
hun voor-meting staat letterlijk in de klasse-javadoc.

**De oorspronkelijke toolchainfixtures** (read-only naslag, niets
gekopieerd). De zeven gevallen zitten in drie manifesten waarvan de
`minDiffed`-vloer gelijk is aan het aantal fixtures, dus élke fixture
werd gediffd: `PHASE_C` 14 van 14, `PHASE_G_FEIT` 6 van 6, `PHASE_GELDIG`
12 van 12. De harnasfunctie eist eerst manifestpariteit — LOWERED op
béíde back-ends voor elke constructie, met een allowlist die alleen een
fixture buiten deze zeven noemt — en vuurt daarna beide KieBases en
vergelijkt de uitkomst. De zeven: één AANTAL-fixture waarin de
geschreven elementrol een paar met meer dan één feittype eenduidig
maakt; vier feitcreatiegevallen (ongeconditioneerd, ankerkenmerk,
doelkenmerk, doelvergelijking); twee gedateerde feitcreatiegevallen
(binnen en vóór het venster). Alle zeven dus LOWERED in beide armen en
differentieel gevuurd.

**Implementatie.** `RegelSpraakModel.verbindendeRollen(houder, doelwit)`
beantwoordt de veldvraag per NAAM in plaats van per PAAR: een rol telt
mee als `verbindendeRolViaNaam` met haar eigen naam precies haar
oplevert. `FactModelGenerator.leden` leidt daaruit één scalair
referentieveld per uniek adresseerbare to-one-rol af. `verbindendeRol`
zelf is ongewijzigd en blijft ambigu.

`ContractValidator` en `ExecModelTranspiler` hoefden niet gewijzigd:
beide vragen het al aan dezelfde `leden`/`heeftReferentieVeld`, dus de
claim "één afleiding" hield stand. `CaseRunner.zetRollen` kreeg één
gedeelde membership-toets (`geldigRolveld`) die de bestaande
zelfpaar-route en de nieuwe kruistyperoute samenvat; daarmee lopen de
heenrichting én de zusterrichting bij naam. `DrlTranspiler.feitCreatie`
schrijft de relatie nu idempotent — zonder die guard vonden creatie en
telling op een herhaald paar geen fixpunt (`FAIL case1/case3: geen
fixpunt (kringverwijzing?)`), omdat `no-loop` alleen de eigen
hertrigger blokkeert.

**Feitenmodel van bestaande goldens, expliciet gemeten.** Het
gegenereerde feitenmodel van alle 199 `ir.json` onder `corpus/golden`
is vóór en ná gedumpt en per objecttype vergeleken. Vier blokken
veranderen, en drie daarvan horen bij de nieuwe golden. De enige
bestaande golden die beweegt is `anchor/jutterswet-bergingstoezicht` —
zelf een herhaald paar — dat `Berger.berging` en `Berging.toezichthouder`
krijgt. Alle andere feitenmodellen zijn byte-identiek.

**De nieuwe golden.** `corpus/golden/jutterswet-dubbelverbinding/`,
eigen en rechtenvrij, bron-gedragen: vier feittypen over het paar
Haven–Schuit (`thuisligging`, `winterberging`, `keurberging`,
`vlagvoering`). De verwachtingen zijn eerst met de hand uitgerekend en
pas daarna gedraaid; alle vier de cases klopten in ref bij de eerste
run. De berekening, kort:

- `liggertal` = het aantal liggers. H1 heeft S1 en S2, H2 heeft S3, H3
  heeft S4 → 2 / 1 / 1.
- `winterberging` vuurt per veilige haven binnen het venster
  (`geldig vanaf 01-03-2026`) en zet de winterhaven van elke ligger.
  Binnen het venster: H1 (veilig) → S1, S2; H2 (veilig) → S3; H3 (niet
  veilig) → niets. Dus `wintertal` 2 / 1 / 0; vóór het venster 0 / 0 / 0.
- `keurberging` joint over de rol van het ándere feittype van hetzelfde
  paar (`een winterschuit van de Haven`) en vuurt bij kadegeld > 30. H1
  (40) → S1, S2; H2 (25) valt af ondanks haar winterschuit; H3 (50)
  heeft er geen. Dus `keurtal` 2 / 0 / 0, en vóór het venster overal 0
  omdat er dan geen winterschuiten zijn.
- `ligprijs` = het kadegeld van de winterhaven, indien de Schuit
  gekeurd is — de aangemaakte koppeling gelezen vanaf de Schuit.
  S1 (gekeurd, winterhaven H1) 40; S2 (niet gekeurd) leeg; S3 (gekeurd,
  winterhaven H2) 25; S4 (gekeurd, geen winterhaven) leeg.
- `vlagprijs` en `vlaglengte` lezen de 1:1-relatie vanaf beide kanten:
  S1 → 40 en H1 → 12; de rest leeg.
- case4 zet de twee rollen van hetzelfde paar op één Schuit: S2 ligt in
  H1 en overwintert in H2. Liggertal 2 / 0, wintertal 0 / 1, keurtal
  0 / 1 (H2 heeft 35 > 30), ligprijs S2 = 35. Zouden de twee rollen op
  één veld vallen, dan klopt geen van deze vijf cellen.

Groen in alle vier uitvoerders: verifier PASS; ref
`oracle: PASS (21/21/21/13 expectations)`; clingo
`clingo: PASS` op dezelfde vier; Drools exec `PLAYED 4`; Drools
classic DRL `PLAYED 4`.

**Sweep, corpusbreed, beide takken, vóór en ná** (alle 199 `ir.json`,
inclusief `anchor/`): exec-gespeeld 148 → 149, DRL-tekst-gespeeld
121 → 122. Precies twee kanaalwisselingen, beide van de nieuwe golden.
Eén statusregel bewoog zonder kanaalwissel:
`anchor/jutterswet-bergingstoezicht` blijft EXCLUDE in beide takken maar
weigert nu dieper — op `verdeling (onopgelost naar_rato)` in plaats van
op het ontbrekende joinveld. `jutterswet-erflijn`,
`jutterswet-gemeenteberging` en `jutterswet-penninguitgifte` bewogen
geen letter.

**Getuigen bij naam** (18, allemaal dubbel-tak waar de vorm dat toelaat):
`de_bij_paar_afleiding_blijft_ambigu_en_raadt_niet`,
`het_beanmodel_draagt_een_veld_per_toone_rol`,
`de_telling_over_elke_geschreven_elementrol_kiest_haar_eigen_feittype`,
`de_verwisselde_rol_is_een_benoemd_verschil_geen_stil_gelijkspel`,
`feitcreatie_zonder_voorwaarde_legt_de_relatie_op_elke_ligger`,
`feitcreatie_met_ankerkenmerk_vuurt_alleen_voor_de_veilige_haven`,
`feitcreatie_met_doelkenmerk_koppelt_alleen_de_gekeurde_liggers`,
`feitcreatie_met_doelvergelijking_koppelt_alleen_de_lange_liggers`,
`feitcreatie_binnen_en_voor_het_geldigheidsvenster`,
`de_geschreven_rol_landt_op_het_veld_van_dat_feittype`,
`beide_spellingen_van_een_relatie_schrijven_hetzelfde_veld`,
`de_een_op_een_relatie_krijgt_beide_velden_geschreven`,
`de_aangemaakte_koppeling_staat_op_het_veld_van_haar_eigen_feittype`,
`de_aangemaakte_koppeling_wordt_aan_beide_kanten_gelezen`,
`een_niet_unieke_rolnaam_leidt_geen_veld_af`,
`naamdeling_met_een_gedeclareerd_lid_blijft_een_eigen_weigering`,
`een_meervoudige_rol_zonder_opslagdragende_overzijde_blijft_geweigerd`,
`een_consument_zonder_geschreven_rol_vindt_het_paar_nog_altijd_ambigu`.

**Mutatieproeven, feitelijk gedraaid en hersteld.**

- (a) rolkeuze verwisseld — `zetRollen` kiest de LAATSTE afgeleide rol
  in plaats van de geschreven: 8 fouten + 3 errors van de 18. Onder meer
  `de_geschreven_rol_landt_op_het_veld_van_dat_feittype` (`to-one rol
  'vlaghaven' krijgt een tweede, ander doel — WF-013`),
  `de_aangemaakte_koppeling_staat_op_het_veld_van_haar_eigen_feittype`
  ("the creation wrote the winterberging leg: expected Haven, was null")
  en alle tellingen. De creatiekant van dezelfde mutatie staat
  permanent als flank in de suite: `zetVeld` verlegd naar `keurhaven`
  maakt zowel `wintertal` als `ligprijs` rood in beide takken.
- (b) maar één relationele richting geschreven — de 1:1-spiegelschrijving
  uitgezet: 3 fouten, waaronder
  `de_een_op_een_relatie_krijgt_beide_velden_geschreven` ("the Haven-side
  spelling ALSO writes the Schuit's own leg: expected Haven, was null")
  en, via `vlagprijs`, de golden-rijen zelf. De zusterrol niet meer bij
  naam oplossen: 3 fouten + 1 error, met
  `beide_spellingen_van_een_relatie_schrijven_hetzelfde_veld` als luide
  weigering (`role 'liggers' (Haven -> Schuit) reaches no to-one
  reference field`).
- (c) de ambiguïteitsweigering weggehaald (`verbindendeRol` geeft de
  laatste kandidaat in plaats van null): 3 fouten, met
  `de_bij_paar_afleiding_blijft_ambigu_en_raadt_niet` ("expected null but
  was vlaghaven") en de luide-ambigu-getuige
  `een_consument_zonder_geschreven_rol_vindt_het_paar_nog_altijd_ambigu`
  ("Expected Excluded to be thrown, but nothing was thrown").

**Gerichte checks.** `mvn -q -o -f backends/drools/pom.xml package` mét
tests: 427 tests, 0 fouten (was 409; vloer 239).
`scripts/check-regelspraak.sh`: PASS, 280 tests, 54 rgs-goldens
re-emitteren byte-identiek (vloer 38). `scripts/check-drools.sh`: PASS,
`played 149`. `scripts/check-reftype-derivation.sh`: PASS, 108
unit-annotaties over 22 goldens, 100 replays, 21 DRL-gespeeld —
ongewijzigd, want de nieuwe golden draagt 22 `type`-annotaties zonder
eenheid en komt dus niet in de twist-set.

**Veertien gates** (`eval $(opam env)`, elk `> /dev/null 2>&1`):

```
self-contained: 0      rood: 0
ir-schema: 0           catala: 0
verifier: 0            regelspraak: 0
case-schema: 0         toka-parity: 0
oracle: 0              drools: 0
clingo: 0              reftype-derivation: 0
noop: 0                flogic: 0
```

**Scriptwijziging** (beschermd pad, gemandateerd door deze kaart):
`scripts/check-drools.sh` `EXPECTED_PLAYED` 148 → 149, met datum
2026-09-18, kaartnaam en reden in het script — de nieuwe golden speelt,
gemeten vóór en ná in beide takken, en geen andere golden wisselt van
kanaal. Geen andere regel van dat script is aangeraakt.

**May touch, geverifieerd** (`git diff --name-only main...HEAD`): 13
bestanden, alle binnen de limitatieve lijst. `ContractValidator.java` en
`ExecModelTranspiler.java` stonden op de lijst maar hoefden niet.

**Registervoorstel** voor `spec/CONSTRUCTS.md` rij 4 (*Fact types and
role navigation*) — niet zelf geland:

> **Landed (2026-09-18):** the bean-model reference derivation runs per
> ROLE, not per TYPE PAIR. Where more than one feittype relates the same
> two object types the by-pair authority (`verbindendeRol`) is ambiguous
> by construction, and the derivation used to ask it first — so a
> document whose author wrote a unique role name got no reference field
> at all and every reader refused at once. `verbindendeRollen` asks by
> NAME (a role counts when asking for its own name answers that role),
> and the generator derives one scalar field per uniquely addressable
> to-one role. `verbindendeRol` itself is unchanged and still ambiguous:
> a consumer without a written or symbolic role keeps the loud refusal
> (witness: a SOM, whose argument is an attribute name, stays a marker in
> the DRL-text branch). The three genuine refusals keep their own
> sentences — a shared role name derives nothing, a to-many role stores
> at the far end, a declared member of the same name displaces the
> reference. Golden `jutterswet-dubbelverbinding` (four feittypen over
> one pair, four cases) plays in ref ≡ clingo ≡ both Drools branches;
> gate 12 `EXPECTED_PLAYED` 148 → 149, DRL-text 121 → 122, no other
> golden changed channel. `backends/drools`'s DRL-text relation write is
> idempotent since the same card, mirroring the exec branch — without it
> creation and fold ping-pong forever on a repeated pair.

**Bewuste restscope.**

1. *Het doelgehoste feitcreatiegeval kan niet in het corpus.* `ref`
   FAULT op een kenmerkcheck die op een ander objecttype is gehost
   (WF-100, onderhouderbesluit 2026-09-10), en de frontend maakt van een
   doelvergelijking een `Onopgelost`. Beide Drools-takken lowéren de
   vorm wél. Dat is een latente drie-uitvoerder-ongelijkheid die geen
   gate ziet; de twee getuigen die haar pinnen zeggen dat ter plaatse.
   De golden gebruikt daarom twee anker-gehoste voorwaarden (een kenmerk
   en een vergelijking) en discrimineert per doel via de joinrol.
2. *SOM/MIN/MAX over een herhaald paar splitst de takken.* Gemeten op
   een variant van de nieuwe golden: exec `PLAYED` (via de symbolische
   `bronRol` op de wire), DRL-tekst `MARKER: TODO aggregatie SOM` (die
   tak leest geen wire-binding en heeft alleen de by-pair-zoektocht). De
   golden vouwt daarom uitsluitend AANTAL. Dat is dezelfde
   takongelijkheid die `anchor/jutterswet-bergingstoezicht` al droeg;
   opheffen vergt een eigen kaart.
3. *De conservatieve opslagrichtingweigeringen blijven staan* — de
   enkelvoudige elementrol met meervoudige zusterrol, in beide takken,
   met hun eigen reden. Deze kaart raakt ze niet.
4. Buiten scope gebleven zoals gevraagd: rolkeuze zonder geschreven
   binding, naamdelingsvormen als taal, beide-zijden-to-many,
   ambigue/wederkerige self-pairs, objectcreatie-semantiek, `bronPad`,
   en de dimensioneel-invalide multi-globalfixtures.
