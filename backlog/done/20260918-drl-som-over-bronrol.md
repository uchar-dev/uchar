# drl-som-over-bronrol: de tellende vouw speelt, de sommerende nog niet

Rest van de rolvelden-kaart (2026-09-18), door de lens precies
afgebakend: sinds de per-rol-afleiding speelt een AANTAL over een
geschreven rol van een herhaald typepaar in beide Drools-takken, maar
een SOM/MIN/MAX over dezelfde symbolische `bronRol` speelt alleen in
exec-model; de DRL-tekst-tak geeft `MARKER: TODO aggregatie SOM`. Op
main bestond deze takongelijkheid niet — beide takken weigerden de vorm
— dus de split is door de rolvelden-kaart nieuw bereikbaar geworden en
bewust buiten het corpus gehouden (de nieuwe golden vouwt uitsluitend
AANTAL).

Meettabel van de lens: herhaald paar — verifier PASS, ref PASS, clingo
PASS, exec PLAYED, drl MARKER; uniek paar — alle vijf groen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de marker letterlijk op een eigen invoer met
  `bronRol` over een herhaald paar.
- (b) De DRL-tekst-tak lowert SOM/MIN/MAX over een symbolische
  `bronRol` langs dezelfde per-rol-afleiding als AANTAL; gedeelde
  vormen weigeren blijft per construct met reden.
- (c) Dual-branchgetuigen + mutatieproef; daarna kan een corpusgetuige
  de vorm pinnen (kanaaltellings-consequentie als
  beschermd-padvoorstel).
- (d) Alle veertien gates groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/test/**`, eventueel één corpusgolden +
`scripts/check-drools.sh`-telling (voorstel), deze kaart en haar
verplaatsing binnen `backlog/`.

---

Result:

**Rood eerst, letterlijk.** Een eigen, verifier-schone invoer — een
reductie van `jutterswet-dubbelverbinding` tot drie waardevouwen over drie
van de vier feittypen die Haven en Schuit verbinden, met de handberekende
verwachtingen — gemeten op de jar uit de commit vóór de wijziging:

```
verifier : PASS
ref      : oracle: PASS (9 expectations)
clingo   : clingo: PASS (9 expectations)
exec     : PLAYED 1
drl      : EXCLUDE drl-transpiler MARKER: regel: liggertal van een haven —
           TODO aggregatie SOM: de som van de romplengte van alle liggers van de Haven
```

Eén vouw per document gemeten zeggen MIN en MAX hetzelfde:

```
drl (MIN): TODO aggregatie MINIMUM: de minimale waarde van de romplengte van
           alle winterschuiten van de Haven
drl (MAX): TODO aggregatie MAXIMUM: de maximale waarde van de romplengte van
           alle keurschuiten van de Haven
```

**De uniek-paar-spiegel**, hetzelfde document met de drie zusterfeittypen
verwijderd en verder letter voor letter gelijk: verifier PASS, ref PASS,
clingo PASS, exec PLAYED 1, drl PLAYED 1. De meettabel van de lens is
daarmee exact gereproduceerd, en de splitsing hangt aan de
bereikafleiding en aan niets anders.

Dezelfde rood-meting staat als getuigenrij in het commitverloop: commit
`4d1f693b` draagt de getuigenklasse zonder de productie-edit, en zes
rijen vallen daar om met de marker woordelijk in de foutregel, terwijl de
twee weigeringsrijen en de uniek-paar-spiegel er groen naast staan.

**Groen ná.** SOM, MIN en MAX spelen in beide takken op precies de pins
die ref en clingo al verifieerden (`drl: PLAYED 1` per vouw, en `PLAYED 1`
op het document met alle drie naast elkaar).

**Implementatie.** Twee dingen, en niets meer.

1. `DrlTranspiler.scope` krijgt als LAATSTE arm — na de geschreven
   elementnaam en na de bij-paar-zoektocht — de symbolische `bronRol` van
   de wire: `gebondenRolpaar(rolBindingen.get(f))` levert
   `[gebonden, zuster]`, en de zusterrol is het backref-veld op het
   element waar de join al op leest. Een meervoudige zusterrol draagt geen
   scalair veld en valt door naar de bestaande weigering. Omdat de arm
   alleen draait wanneer de twee bestaande armen niets opleverden, blijft
   elke join die het model al afleidde ongewijzigd. De
   §13.3.9-opslagrichtingvraag (`navVeld` → `FactModelGenerator`) staat er
   ongewijzigd achteraan: een gebonden rol is nog geen beanveld.
2. De tak kreeg de bindingen niet eens aangereikt. `DrlTranspiler` krijgt
   een vijfde constructorparameter (`rolBindingen`, identiteitsgesleuteld
   zoals `bronPaden`) en `CaseRunner.drlTak` geeft `loaded.rolBindingen()`
   door.

`AANTAL` blijft ongemoeid waar het al werkte; waar een telling vroeger
markerde omdat noch de geschreven naam noch het paar uitkwam (het
zelfgetypeerde geval) lowert zij nu ook — dezelfde arm, dezelfde richting.

**Sweep, corpusbreed, beide takken, vóór en ná** (alle 199 `ir.json`
onder `corpus/golden`, inclusief `anchor/`, 398 statusregels):

```
exec-gespeeld 150 -> 150      DRL-tekst-gespeeld 123 -> 123
kanaalwisselingen: 0
```

De kanaal-nulmeting die de kaart verwachtte klopt dus, maar de aanname
eronder niet: **drie statusregels bewegen wél**, alle drie in de DRL-tak,
alle drie EXCLUDE → EXCLUDE, en alle drie omdat de SOM-marker verdwijnt en
het volgende niet-gelowerde construct van hetzelfde document bovenkomt.

| golden | vóór | ná |
|---|---|---|
| `anchor/jutterswet-bergingstoezicht` | `MARKER: regel: toezichtopbrengst van een berger — TODO aggregatie SOM` | `MARKER: verdeling: verdeel de opbrengst van een berging — onopgelost verdeling (naar_rato)` |
| `anchor/jutterswet-wachtersketen` | `MARKER: regel: opvolgerrang van een wachter — TODO aggregatie SOM` | `MARKER: objectcreatie: werf de opvolger — onopgelost terugkoppeling naar Wachter` |
| `synth2-budgetgrens-onder` | idem | idem |

De laatste twee zijn het ZELFGETYPEERDE paar (twee gelijkgetypte
zusterrollen van één feittype), waar de bij-paar-zoektocht er per
constructie twee telt. Dat was dus een tweede, al corpus-zichtbare
takongelijkheid — `wachtersketen` speelde `PLAYED 2` in exec tegenover een
marker in de DRL-tak — die door dezelfde arm sluit. Geen van de drie
wisselt van kanaal, dus `scripts/check-drools.sh` beweegt niet en er is
geen beschermd-padvoorstel voor een telling.

**Getuigen bij naam** (10, in
`backends/drools/src/test/.../cases/BronRolVouwWitnessTest.java`, op
in-memory varianten van `jutterswet-dubbelverbinding`; de golden zelf is
niet aangeraakt):

- `de_som_over_een_geschreven_bronrol_speelt_in_beide_takken` — 19 / 20 / leeg
- `de_minimale_waarde_over_een_bronrol_speelt_in_beide_takken` — leeg / 7 / leeg
- `de_maximale_waarde_over_een_bronrol_speelt_in_beide_takken` — 20 / leeg / leeg
- `de_drie_vouwen_naast_elkaar_kiezen_elk_hun_eigen_feittype` — negen cellen
  in één document; S1 zit in alle drie de bereiken maar onder drie
  verschillende rollen, dus twee rollen die één veld zouden delen
  verplaatsen minstens één cel
- `de_lege_verzameling_leest_de_leegwaarde_in_plaats_van_leeg` — de
  andere helft van de leegwaarde-wet (de drie rijen hierboven pinnen de
  leeg-cellen; deze pint `of 0 als die er niet zijn` → H3 = 0)
- `de_verwisselde_bronrol_is_een_benoemd_verschil_geen_stil_gelijkspel` —
  staande mutatieflank
- `het_uniek_verbonden_paar_blijft_spelen` — de regressiespiegel
- `een_vouw_zonder_bronrol_blijft_in_beide_takken_geweigerd`
- `een_meervoudige_zusterrol_levert_geen_scalair_joinveld`
- `het_zelfgetypeerde_bereik_lowert_nu_ook_in_de_drl_tak` — op
  `anchor/jutterswet-wachtersketen`, gemeten op het dekkingsmanifest omdat
  die golden in de DRL-tak hoe dan ook op haar objectcreatie blijft
  weigeren; de rij vraagt ook het omgekeerde: dezelfde tak zonder de
  bindingen geeft de marker terug

De eerste acht draaien via `BeideTakken`, dus elke rij eist hetzelfde van
béíde takken. De handberekening: H1 heeft de liggers S1 (12) en S2 (7)
→ 19, H2 heeft S3 (20) → 20, H3 geen → leeg; winterschuiten alleen bij H2
(S1, S2) → min 7; keurschuiten alleen bij H1 (S1 12, S3 20) → max 20. Ref
en clingo klopten bij de eerste run op alle negen cellen.

**Eén bestaande getuige omgezet.**
`HerhaaldTypepaarWitnessTest.een_consument_zonder_geschreven_rol_vindt_het_paar_nog_altijd_ambigu`
hield de `bronRol` van de golden vast en weigerde tóch — hij pinde dus de
takongelijkheid alsof het de ambiguïteitswet was. Zijn fixture haalt de
binding nu van de wire, wat zijn zin altijd al beweerde; hij blijft rood
zodra de afleiding gaat gokken (mutatie b hieronder). Gemeten op die vorm
weigert het hele vierkant haar trouwens, en dat is waarom geen corpusgolden
haar kan dragen:

```
verifier : WF-048 FAIL /resolvedRegels/0/waarde: bindingless instance fold:
           4 fact types relate 'Haven' to 'Schuit' — not uniquely derivable
ref      : EVALUATION FAULT: the connecting role from "Haven" to "Schuit" is
           ambiguous (4 candidate roles); the pinned runtime derives null there
clingo   : TRANSLATION FAULT: <dezelfde zin>
```

**Mutatieproeven, feitelijk gedraaid en hersteld.**

- (a) *De rolkeuze verwisseld* — `scope` neemt `paar[0]` (de gebonden rol)
  in plaats van `paar[1]` (de zuster): 6 rijen rood, elk met een benoemde
  weigering, bv. `de_som_over_een_geschreven_bronrol_speelt_in_beide_takken`
  → `drl-transpiler MARKER: … aggregatie-join: het beanmodel leidt voor de
  rol 'ligger' op Schuit geen referentieveld naar Haven af (§13.3.9
  opslagrichting)`. De zelfgetypeerde rij overleeft deze mutatie — daar
  dragen beide rollen een veld — en dat is precies waarom de
  kruistype-rijen ernaast staan.
- (b) *De per-rol-afleiding omzeild* — een typepaar-gok geforceerd
  (`verbindendeRollen(elem, subject).get(0)` vóór de bindingsarm): 7 rijen
  rood, en de twee ambiguïteitsgetuigen zijn erbij, beide met
  `Expected Excluded to be thrown, but nothing was thrown` —
  `een_vouw_zonder_bronrol_blijft_in_beide_takken_geweigerd` en
  `een_consument_zonder_geschreven_rol_vindt_het_paar_nog_altijd_ambigu`.
  De waarderijen vallen mee om met een benoemd verschil
  (`keurtal: verwacht 20, werkelijk 12`), dus de gok is ook inhoudelijk
  zichtbaar fout.

Beide mutaties zijn na meting teruggedraaid; de werkboom was daarna weer
schoon (`git status` leeg).

**Gerichte checks.** `mvn -o -f backends/drools/pom.xml package` mét
tests: **447 tests, 0 fouten, 0 errors, 0 skips** (was 437; vloer 239).
`scripts/check-drools.sh`: PASS, `played 150`, kanaalvloer 49 niet-PLAYED
goldens bij naam, JUnit 447. `scripts/check-reftype-derivation.sh`: PASS,
108 unit-annotaties over 22 goldens (vloer 74), 100 replays, **21
DRL-gespeeld (vloer 13)** — beide taktellingen ongewijzigd.

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

**May touch, geverifieerd** (`git diff --name-only main...HEAD`): 5
bestanden. Vier binnen de limitatieve lijst (`DrlTranspiler.java`, twee
bestanden onder `backends/drools/src/test/`, de kaartverplaatsing). **Eén
buiten de lijst, met reden:**
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`
— één aanroep (`DrlTranspiler.build(…, loaded.rolBindingen())`). De
bindingen komen uit `IrLoader.Loaded` en er is precies één plek waar de
DRL-tak wordt gebouwd; zonder die regel ontvangt de tak de wire-binding
nooit en is de hele kaart onuitvoerbaar — de getuigen lopen ook via
`CaseRunner.speelCase`. Geen andere regel van dat bestand is aangeraakt.
`scripts/` en corpus zijn ongemoeid gebleven; er is geen corpusgolden
toegevoegd.

**Registervoorstel** voor `spec/CONSTRUCTS.md` rij 4 (*Fact types and role
navigation*) — niet zelf geland. De landingsnotitie van 2026-09-18 draagt
een tussenzin die nu onwaar is; die zin vervangen en de nieuwe landing
eronder zetten:

> ~~(witness: a SOM, whose argument is an attribute name, stays a marker in
> the DRL-text branch)~~ → *(witness: a SOM whose wire carries NO range at
> all — no written role, no `bronRol` — stays a refusal in both Drools
> branches, and the verifier refuses it too on WF-048)*.
>
> **Landed (2026-09-19):** both Drools branches now resolve a fold's RANGE
> the same way. A value fold (SOM/TOTAAL/MINIMUM/MAXIMUM) takes an
> ATTRIBUTE name as its argument, so the written-name arm that
> disambiguates a count does not apply to it and the by-pair search is all
> that is left — ambiguous by construction on a repeated pair AND on a
> self-typed one. The exec branch had read the ch. 4.3 `bronRol` since
> 2026-08-30; the DRL-text branch was never handed the bindings at all, so
> it markered exactly the folds its sister played. It now takes them and
> consults them LAST, after the written name and the by-pair search, so
> every join the model already derived is untouched, and the §13.3.9
> storage-direction question still follows: a bound role is not yet a bean
> field. Measured corpus-wide in both branches: exec 150 → 150, DRL-text
> 123 → 123, **no golden changes channel**, and three DRL-text refusals
> move DEEPER — `anchor/jutterswet-bergingstoezicht`,
> `anchor/jutterswet-wachtersketen` and `synth2-budgetgrens-onder` lose
> their fold marker and surface their next unlowered construct. A fold
> with NO range on the wire keeps the loud refusal in every executor
> (verifier WF-048, ref and clingo faulting in the same sentence, both
> Drools branches refusing per construct).

**Bewuste restscope.**

1. *De bindingsresolutie staat nu in twee exemplaren* —
   `DrlTranspiler.gebondenRolpaar` naast
   `ExecModelTranspiler.gebondenRolpaar`, woordelijk hetzelfde. Eén
   gedeelde afleiding is beter en de plek ligt voor de hand
   (`FactModelGenerator`, waar de gedeelde weigeringszinnen al wonen),
   maar dat raakt twee bestanden buiten de limitatieve May touch die
   andere pariteitskaarten delen. De getuigen die beide takken hetzelfde
   vragen zijn wat de twee voorlopig bij elkaar houdt; het samenvoegen is
   een eigen, kleine kaart.
2. *De weigering bij een meervoudige zusterrol blijft generiek.* Met beide
   rollen meervoudig levert de binding geen scalair joinveld en valt de
   vouw door naar de algemene marker (`TODO aggregatie SOM`) naast exec's
   `aggregatie buiten Phase-C scope`. Beide weigeren dus per construct,
   maar niet in dezelfde woorden zoals de fold-join-naad dat wel doet. Het
   scherper maken van één kant zou de takken juist uit elkaar zetten; de
   eerlijke ingreep is weer één gedeelde zin, en die hoort bij punt 1.
3. *De temporele set-vouwen lezen de binding niet.*
   `tijdreeksSomAlleSet` en `temporeelSetAccumulate` in dezelfde tak
   leiden hun join nog met `verbindendeRol` alleen af. Geen corpusgolden
   combineert een tijdreeksvouw met een herhaald of zelfgetypeerd paar, en
   de vorm valt door naar een marker, niet naar een verkeerde verzameling.
   Buiten scope gehouden; een eigen meting waard.
4. *De conservatieve exec-arm* (enkelvoudige elementrol met meervoudige
   zusterrol) en de `voorwaarts`-join die de DRL-tak niet kent blijven
   staan zoals ze stonden — deze kaart raakt ze niet.
