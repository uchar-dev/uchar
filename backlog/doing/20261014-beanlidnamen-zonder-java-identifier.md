# beanlidnamen-zonder-java-identifier: een lid waarvan de naam geen Java-identifier of DRL-eigenschap oplevert

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit de
besluiten 24, 27 en 28 van die kaart. Bestaand gedrag van de Drools-backend op beide takken, buiten het corpus.*

## Aanleiding

De beangenerator (`schema/FactModelGenerator`) maakt van de naam van een attribuut of kenmerk een Java-veld met
`Namen.ident`. Sinds de kaart leidt één hulp in `transpiler/core/Namen` (`lidnamen`) veld, getter, setter en
DRL-eigenschap af; de generator gebruikt haar ook. Drie grenzen zijn gemeten, geen ervan raakt een golden:

- **Een naam met een cijfer vooraan of een teken dat geen Java-identifierteken is.** `Namen.ident` houdt alles van
  `\p{N}`, dus ook `²`, `³` en `½`, terwijl `Character.isJavaIdentifierPart` daar `false` geeft. Een lid "m²", "3 M" of
  "3e lid" geeft zo een Java-veld dat niet compileert: de bouw van de beans faalt luid, maar als crash, niet als
  benoemde weigering per vorm.
- **Een DRL-sleutelwoord als lidnaam.** Achttien woorden (`in`, `is`, `with`, `drools`, `nil`, …) werken niet als
  linkerkant van een DRL-constraint. De DRL-weergave weigert zo'n lid luid met het label `drl:`; de exec-model-tak
  speelt het. Onder de strenge gate 12 is dat een verschil tussen de takken, dus rood zodra een golden zo'n lid krijgt.
  De bouwer van stap 2a2 mat dat een getteraanroep in de tekst overal leest.
- **Een lid als "İş".** De generator schrijft veld `iş` met getter `getIş`; sinds de kaart vindt de tak die getter
  (vroeger zocht hij `getİş` en las het lid niet). Dat is opgelost; het staat hier omdat elke nieuwe afleiding het
  opnieuw moet halen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet welke van deze namen de IR toelaat (verifier, IR-SPEC §2.1) en wat ref en clingo ermee doen.
- (b) Elke toegelaten naam wordt een geldig, deterministisch en injectief Java-veld, of wordt per vorm geweigerd met
  een benoemde reden via het `Coverage`-manifest, op beide takken in dezelfde zin. Nooit een crash bij de bouw.
- (c) De DRL-weergave leest een lid met een van de achttien woorden via de getter, zodat beide takken het spelen.
- (d) Eigen documenten met zulke namen, rood eerst, door ref, clingo en beide takken; klopt het vierkant, dan een golden
  (de pin van gate 12 als voorstel).

May touch: `backends/drools/**` (`schema/`, `transpiler/core/Namen`, `transpiler/tms/DrlRendering`), `corpus/**`,
`scripts/check-drools.sh` (**voorstel**, pin).

Out of scope: `ref/`, `backends/clingo`, `spec/` (alleen meten; een spec-vraag wordt een eigen voorstel).

## Result

Bouwer drools-residue, 2026-11-06, branch `card/drools-residue`. Logs en proefdocumenten in `werk/drools-residue/`
(`logs/`, `probe/lidnamen/`, `probe/lidnamen-botsing/`).

**(a) Wat de IR toelaat en wat ref en clingo doen.** WF-057 (IR-SPEC ch. 2.1) weert alleen `"`, `\` en tekens onder
U+0020; elke andere naam is toegelaten. Gemeten op `anchor/jutterswet-waardestelling` met het gelezen attribuut
(`taxatiewaarde`) en het geschreven attribuut (`geschatte waarde`) elk hernoemd tot `3e lid`, `3 M`, `m²`, `½ deel`,
`final`, `class`, `in`, `is`, `with`, `drools`, `nil` (22 documenten, 4 cases elk): de verifier geeft PASS, ref en
clingo PASS op elke case (`probe/lidnamen/meting-ref-clingo.log`). De Drools-takken vóór de reparatie
(`probe/lidnamen/meting-oud.log`): `3e lid`, `3 M`, `m²`, `½ deel`, `final`, `class` gaven op beide takken
`ERROR IllegalStateException: gegenereerde fact-beans compileren niet` (een crash bij de bouw); `in`, `is`, `with`,
`drools`, `nil` als gelezen lid: exec-model `PLAYED 4`, DRL-tekst `EXCLUDE DROOLS-058`.

**(b) Elke toegelaten naam een geldig Java-veld, of een benoemde weigering.** `Namen.ident` neemt als woord alleen de
letters en cijfers die een Java-identifier kan dragen, een cijferteken van de categorie `No` in zijn
compatibiliteitsvorm (`m²` → `m2`, `½` → `12`), en zet een `x`/`X` voor een resultaat dat met een cijfer begint
(`3e lid` → `x3eLid`/`X3eLid`), zodat `ident` van het resultaat dezelfde Pascal-vorm geeft die de reflectiebrug van de
runtime leest. `FactModelGenerator.generate` schrijft het veld van een woord van Java als `final_`; getter en setter
blijven die van de eigenschap (`getFinal`). Een lid waarvan de getter een methode van `java.lang.Object` is
(`class` → `getClass`) kan geen bean dragen: `DroolsBackend` weigert het document op beide takken met de nieuwe code
DROOLS-317 (`FactModelGenerator.objectGetter`, register en beide catalogi), in dezelfde zin. Deterministisch: één
functie, geen toestand. Injectief is `ident` niet en was het niet (`premie som`/`premie-som`); twee leden op één
eigenschap weigert de bestaande botsingstoets per vorm (gemeten: `m²` naast `m2` op één type geeft op beide takken
`EXCLUDE DROOLS-054: bean name collision …`, `probe/lidnamen-botsing/`). Na de reparatie
(`probe/lidnamen/meting-nieuw.log`): 20 van de 22 documenten `PLAYED 4` op beide takken, de twee met `class` `EXCLUDE
DROOLS-317` op beide; geen crash.

**(c) De DRL-weergave leest een lid zonder DRL-eigenschap via zijn getter.** `DrlRendering.fieldName` spelt zo'n lid
als `getIn()` of `isIs()` (de vorm die de bean draagt), in binding, constraint en na een patroonvariabele; de meting
van stap 2a2 (`DrlPropertyWitnessTest`) las de getteraanroep overal als het lid. `Namen.lidnamen` houdt ook een woord
van Java buiten de DRL-eigenschappen. DROOLS-058 is daarmee `RETIRED` (register, `CLAUDE.md` gate 12).

**(d) Eigen documenten, rood eerst, en een golden.** In de suite `MemberNameWitnessTest`: elf namen als gelezen en als
geschreven lid op beide takken (`BothBranches.green`), `class` als benoemde weigering, en `Namen.property` als
Java-identifier; rood op de oude code (`logs/card2-rood-bewaard.log`: 3 van 3), groen erna. Golden
`jutterswet-lidnamen` (alleen `ir.json`, drie cases, `NOTE.md`; de RegelSpraak-grammatica laat `in` en `is` niet als
lidnaam toe): leden `m²`, `final`, `in`, `3e lid` en kenmerk `is`. Rood op de jar van vóór de kaart
(`logs/card2-golden-rood.log`: beide takken `ERROR … compileren niet`); nu verifier PASS, ref en clingo PASS, beide
takken `PLAYED 3` en `READINGS 3` (`logs/card2-golden-groen.log`), `--noop` faalt op elke case. Gate 12:
`EXPECTED_PLAYED` 352 → 353 (`scripts/check-drools.sh`, met de reden); na de rebase op main `bc3249090`, dat zelf
354 had (`jutterswet-keurquotient`, `jutterswet-macht-van-nul`), 355 met de drie redenregels.

**Gewijzigde tests (gedrag dat de kaart verandert).**
- `PropertyWitnessTest.a_member_without_a_java_name_fails_loudly_at_the_bean_source` pinde de crash; nu
  `a_member_without_a_java_name_as_written_gets_one` (`3 M`, `3e lid`, `m²`, `final` bouwen, met de getter en setter van
  `Namen.lidnamen`).
- `DrlPropertyWitnessTest`: de twee rijen die de weigering van `in`, `drools`, `nil` pinden, verwachten nu `compiles`
  (een kenmerk gespeld als `isIn()`, `isDrools()`).
- `ReadNameWitnessTest.a_read_name_is_a_java_identifier_also_outside_the_corpus` en
  `DrlFunctionNamesWitnessTest.RULES` pinden namen die `Namen.ident` afleidt: `$artikel32Inconsistent`, `$x12Deel`,
  `$x3eLid`, `$x23`, `toets_x2JarigeTermijn`, `toets_artikel22En`.

**Gates.** `logs/card2-gates/summary.txt`: 3, 4, 5, 6, 7, 13, 15, 17 groen; gate 2 rood op de round-trip van de nieuwe
`ir.json` (UTF-8 in plaats van `²`), hersteld en groen (`logs/card2-gate2.log`); gate 12 rood op de twee naampins
hierboven, hersteld (`logs/card2-namepins.log`) en opnieuw gedraaid (`logs/card2-gate12-b.log`): groen, 353
gespeeld op beide takken, JUnit 1880. Gate 1 groen.

**Voor de distributie (pin-bump).** Bean-eigenschappen van leden met een cijfer vooraan of een cijferteken heten
anders (`x3eLid`, `m2`); een document met zo'n lid compileerde voorheen niet, dus er verschuift geen werkende naam.
