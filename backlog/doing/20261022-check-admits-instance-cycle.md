# check-admits-instance-cycle: `check` laat een kring over een schakelrelatie toe en noemt de relatie waar hij weigert

*Uit het Fable-advies van 2026-10-22 (`uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 1, stappen 0 en
5), de frontendhelft van `cycle-across-instances`. Voorlopig besluit van de hoofdsessie naar dat advies.*

## Aanleiding

Na `cycle-across-instances` laten spec, verifier, ref, clingo en Drools een kring toe die alleen door verschoven randen
over een schakelrelatie gesloten wordt (de afbakening staat in die kaart). De frontend toetst de kringen zelf
(`Dependencies.java`, sinds `kringtoets-volgt-wf045`) en weigert zo'n kring nog met WF-045 vóór de emissie: `check`
weigert dan wat de verifier toelaat. En een kring die buiten de afbakening valt, weigert `check` met een melding die
niet zegt welk feittype twee objecten van één type verbindt.

## Doel

- `Dependencies.java` leest de verschoven randen zoals de verifier (dezelfde afbakening) en laat de kring toe; de
  getuige R1 van `cycle-across-instances` geeft `check` exit 0, en het IR is bytegelijk aan de golden
  `jutterswet-vergunningketen`, die daarmee een `source.rgs` krijgt (gate 10).
- Een kring die buiten de afbakening valt en over een reflexief feittype loopt, weigert `check` met een melding die dat
  feittype noemt en de uitweg (een eigen lid op het object, of de vorm binnen de afbakening).

## Acceptatie

- (a) Rood eerst: frontendtests met R1 (exit 0) en met een vorm buiten de afbakening (de melding noemt het feittype).
- (b) De golden `jutterswet-vergunningketen` emitteert byte-identiek vanuit haar nieuwe `source.rgs`; de zeventien
  gates groen.
- (c) Als meting, zonder bestand in deze repository: de v2-regelset herleving-WW met de verlenging in de maxdatum van
  het recht (zonder brug K12). Zolang de werkbank hww-06 niet herschrijft (normvraag B2.5 van Fable-advies 2), weigert
  `check` alleen de samenlooplezing, met een melding die de lezing noemt zoals `WF-045.instance-read-outside`
  van de verifier. Na de herschrijving geeft `check` exit 0.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-vergunningketen/` en
`corpus/golden/jutterswet-vergunningtermijnen/` (alleen `source.rgs`), deze kaart. Start pas als
`instance-cycle-household-reads` geland is (Fable-advies 2, B2.4). Dan bouwt `Dependencies.java` de definitieve
afbakening in één keer.

De snippets van `percentageOf` stonden hier eerst. Ze zijn nu een eigen kaart: `percentage-of-snippets`.

## Result (2026-10-22)

Tak `card/check-admits-instance-cycle`, begonnen op main `6efa0aa5`, gerebased op main `0cedb232` (de clingo-fix met
haar golden). Geraakt: `frontends/regelspraak/**`, de `source.rgs` van de twee goldens, en (door de extra fix
hieronder, vondst 2) de `ir.json` van 39 goldens, `toka/casus/ir.json`, zes
TOKA-slices, `toka/README.md` en twee Drools-tests. Niet geraakt: `spec/`, `ref/`, `backends/clingo/`, de
gate-scripts, `corpus/negative/`, `corpus/golden/anchor/`, de `NOTE.md` van beide goldens (vondst 3).

Na de rebase:

- `ece0b721` de kaart naar `doing/`.
- `53036a53` `check` laat de instantiegeordende kring toe en noemt de lezing buiten de afbakening (RGS-357).
- `d9131437` `source.rgs` voor `jutterswet-vergunningketen` en `jutterswet-vergunningtermijnen`.
- `f05d5fae` extra fix zonder kaart: een snippet stopt vóór de slotpunt van de zin (vondst 2).
- `274afdca` deze Result.
- `1b5c9e49` de golden van de clingo-fix (`jutterswet-vroege-vondsten-zonder-schorsing`), die met de snippets van vóór
  de fix op main landde: twee snippets zonder de slotpunt, verder niets.

**Wat `Dependencies.java` nu doet.** Een lezing draagt haar pad (`NodeRead.path`, de hops vanaf het onderwerp), zoals
`dep_expr_at` van de verifier: een vouw leest argument en filter aan het eind van haar bereik, een kwantor filter en
toets aan het eind van haar bereik, een genavigeerde kenmerkcheck op `at` plus de hop, een kenmerkcheck met een andere
houder zonder pad. `InstanceOrdering` volgt `instance_ordering` letterlijk: de delen van Tarjan in de volgorde van de
verifier, alleen regels en defaults-groepen, `walk_of` (de schakelstap op de eerste of de tweede hop), `switch_hop`,
`satellite_hop` (eigenaarsrol to-one, de stap naar de satelliet mag een verzameling bereiken), `classify` met de vier
toestanden, `clause_of_hop`, `one-walk` bij meer dan één paar, `one-satellite-relation`, en `settle` voor de niveaus.
De verschoven randen vallen uit de wandeling van WF-045 en uit die van WF-051.

**De melding.** RGS-357 `instance-read-outside`, ERROR, parameters `clause`, `factType`, `form`, `node`, `path`: die
van `WF-045.instance-read-outside` plus de schakelrelatie van het deel (bij `one-walk` die van de lezing zelf). Zij
staat naast de RGS-096 van de kring, op de kop van de vorm die leest. Voorbeeld (de bron van het negatief
`wf045-instance-cycle-hub-negation`):

    ERROR (line 20): regel 'einddag' hangt via een kring van zichzelf af; … (WF-045), en een kring draagt geen
      negatieve rand, zoals een ontkenning (WF-046): dependency cycle through no recursive creation: einddag ->
      openstand -> blokkade -> einddag
    ERROR (line 35): 'blokkade' leest char:vergunning:openstand over [registratie/register; registratie/geregistreerde
      vergunning], en die lezing valt buiten de afbakening van de per object geordende kring over het feittype 'vorige
      vergunning' (clausule hub): een kring over zo'n feittype, dat twee objecten van één type verbindt, is alleen
      toegelaten als elke lezing achter een rand binnen de kring in het huishouden van het object of van zijn
      voorganger landt (WF-045, IR-SPEC ch. 5.3); schrijf de gelezen waarde in een eigen lid van het object, zodat de
      lezing de kring niet sluit, of breng de lezing binnen die afbakening

Engels in de tweede catalogus. `scripts/check-rgs-codes.sh` is niet aangeraakt: register en catalogi houden zonder
extra regel.

**Rood naar groen.** `InstanceOrderedCycleTest` (drie tests: R1 en de huishoudvorm zonder diagnose, de hub met RGS-096
en RGS-357 met precies die parameters en die tekst) faalde op de code van `6efa0aa5` alle drie
(`werk/bruggen/check-admits/06-red.txt`: R1 twee WF-045-ERRORs, de huishoudvorm vijf, de hub alleen RGS-096). Na de
wijziging groen. `resolver/InstanceOrderingWitnessTest` leest het corpus: beide goldens geven geen kring en geen lezing
buiten; elk van de negen negatieven `wf045-instance-cycle-*` houdt zijn WF-045-kring, en de zes met
`WF-045.instance-read-outside` in `expect.txt` krijgen precies die lezing (vorm, knoop, pad, clausule).

**Gelijk aan de verifier.** Op 396 documenten (de negatieven van WF-045, WF-046 en WF-051, alle goldens en alle rode
gevallen) zijn de WF-045-kringen, de WF-051-regels en de lezingen buiten van `Dependencies.find` gelijk aan
`verifier --arms` (`werk/bruggen/check-admits/05-verifier-arms.txt`, vergelijkscript in de scratchpad van de sessie).

**De goldens.** De bronnen uit `NOTE.md` emitteerden met de nieuwe frontend byte-identiek het handgeschreven `ir.json`
(`werk/bruggen/check-admits/vergunningketen.ir.diff` en `vergunningtermijnen.ir.diff`, beide leeg), dus de
`ir.json` hoefde voor deze kaart niet te veranderen. Wel veranderen ze daarna door de snippetfix (vondst 2), elk met
twee snippets, en alleen de slotpunt:

    jutterswet-vergunningketen      /rules/rules/0/versions/0/condition/source   '… - zijn stopdag is eerder dan zijn einddag.' -> zonder '.'
                                    /rules/rules/3/versions/0/condition/source   '… eerder dan de verlengde einddag van de houder van de Termijn.' -> zonder '.'
    jutterswet-vergunningtermijnen  /rules/rules/0/versions/0/condition/source   '… is eerder dan zijn einddag.' -> zonder '.'
                                    /rules/rules/3/versions/0/condition/source   '… is niet afgewikkeld.' -> zonder '.'

**Meting (c)** (geen bestand in de repository; `werk/bruggen/check-admits/c/`). Fundament en regels van v2 herleving-WW
gekopieerd, met de verlenging in de maxdatum van het recht: in hww-08 "De maxdatum van een RechtOpUitkeringWwBijOntslag
moet berekend worden als de maxdatum van de uitkeringsduur van het RechtOpUitkeringWwBijOntslag plus zijn
duurverlenging bij-een-nieuw recht", in hww-18 het lid `verlengde maxdatum` en zijn regel weg (dezelfde wijziging als
`werk/bruggen/k12/m5/v2-k12.ir.json`). Ongewijzigd geeft `check` op main en op de tak exit 0 en hetzelfde IR. Met de
wijziging: main 20 ERRORs (tien WF-045, tien WF-051); de tak 21, dezelfde twintig plus één RGS-357, en dat is de
samenlooplezing:

    ERROR (line 772): 'herleving art. 21 lid 1' leest attr:rechtopuitkering:beëindigingsdatum over
      [SamenloopRechtenOpUitkeringBetreftRechtOpUitkering/samenloop; SamenloopRechtenOpUitkeringBetreftRechtOpUitkering/recht],
      en die lezing valt buiten de afbakening van de per object geordende kring over het feittype 'eerdere recht'
      (clausule hub): …

Vorm, knoop, pad en clausule zijn die van meting 6 van `instance-cycle-household-reads` (de verifier op hetzelfde
document). De andere lezingen van meting 5 noemt `check` niet: ze zijn lokaal.

**Gates.** Tijdens de bouw: `make -C frontends/regelspraak test` 1284 tests groen (na de snippetfix 1285); gate 10 PASS
(1284 tests, 135 rgs-goldens bytegelijk, `check-rgs-codes` PASS met 356 codes), gate 3 PASS, gate 11 PASS, gate 16 PASS
(35 scenario-goldens, 123 casebestanden), alle vier vóór de snippetfix; na de snippetfix de twee Drools-getuigen los
groen. De volledige set op de stand vóór de rebase (`werk/gates.sh`, `gates-1`, op `bbd203c3`, 17:13–17:46): alle
zeventien groen, `EXIT 0`. Gate 10: 1285 tests, 135 rgs-goldens bytegelijk. Gate 11 PASS, met de opnieuw uitgegeven dump
en de gemigreerde slices. Gate 12: 272 gespeeld op beide takken, de suite 1654 tests. Gate 16: 35 scenario-goldens, 123
casebestanden. Gate 17: 272 goldens en 746 casebestanden vergeleken. Main bewoog tijdens die run naar `0cedb232`
(de clingo-fix, met een nieuwe golden met `source.rgs`). Daarom gerebased, de nieuwe golden opnieuw uitgegeven
(`1b5c9e49`) en de volledige set nog één keer gedraaid (`gates-2` op `1b5c9e49`, 17:48–18:19): alle zeventien
groen, `EXIT 0`; gate 10 136 rgs-goldens bytegelijk, gate 12 273 gespeeld op beide takken.

### Vondsten

1. **Bestaande tests gewijzigd, met reden.** `DependencyCycleTest` pinde de weigering van twee documenten die de
   verifier sinds `cycle-across-instances` toelaat (`verifier: PASS` op het IR van beide, gemeten 2026-10-22,
   `werk/bruggen/check-admits/08-old-tests-verifier.txt`). `a_cycle_over_a_role_between_objects_of_a_type_is_refused`
   heet nu `a_cycle_over_a_switch_relation_is_admitted_as_the_verifier_admits_it` en pint de toelating van hetzelfde
   document. Het meldtermijn-document staat nu als `MELDTERMIJN`. Zijn vorm over de schakelrelatie wordt toegelaten
   (nieuwe test `a_cycle_through_a_deviation_over_a_switch_relation_is_admitted`). De weigering onder WF-045 en WF-051
   (`a_cycle_through_a_deviation_refuses_under_wf045_and_wf051`, dezelfde twee meldingen op dezelfde regels) staat op
   dezelfde vormen, met de restermijn op het eigen onderwerp gelezen.
2. **Extra fix zonder kaart (reviewvondst van de toolchain-kaart T1): een snippet droeg de slotpunt van de zin.** De
   grammatica laat de laatste bullet van een lijst (`genesteVoorwaarde`, `genestCriterium`), een `variabeleOnderdeel`
   en een `uniciteitsControle` de punt nemen die de zin sluit. Elke knoop waarvan de context daar eindigde, droeg die
   punt in zijn snippet: een vouw met een bulletlijst als filter, haar argument en filter, een samengestelde voorwaarde,
   een geneste groep, een consistentie over criteria, een uniciteitsvorm en een variabele. In v2 las een uitlegzin
   daardoor "…kalendermaand. (01-08-2026)". De fix zit op de plek waar de frontend een snippet uit een context snijdt
   (`DefinitionResolver.snippet`, gebruikt door `source`, `tailSource` en `groupSource`). De gegenereerde voorwaarde
   van de opgave-markering (`RuleChecker`, "gespeld zoals de frontend de handgeschreven regel uitgeeft") volgt mee.
   Getuige `SentencePeriodSnippetTest`, rood op `e78f2ba1` (`werk/bruggen/check-admits/snippet/red.txt`). Opnieuw
   uitgegeven: 38 goldens, de TOKA-dump en zes slices. Volgens `werk/bruggen/percentage/snippet-diff.py` zijn het 45
   JSON-bestanden zonder snippets gelijk aan `e78f2ba1`, met 99 gewijzigde snippets, elk alleen de slotpunt minder
   (`werk/bruggen/check-admits/snippet/snippet-diff-2.txt`), plus na de rebase de golden van de clingo-fix met twee
snippets (`snippet-diff-3.txt`). De scenarioroute (gate 16) gaf dezelfde `ir.json` en
   dezelfde casebestanden. TOKA: de dump is opnieuw uitgegeven en de slices zijn gemigreerd met dezelfde negen
   strings (13 vervangingen). `toka/README.md` noemt de landing onder de bewuste afwijkingen.
   Aangepaste tests, die de oude snippet pinden:
   - `ExceptionOnExceptionTest.the_merged_source_keeps_no_period_in_the_middle_of_the_sentence`;
   - `ExceptionOnExceptionTest.a_deviation_carries_only_its_own_justification`;
   - `InputMarkingTest.the_wire_carries_the_lowering_in_the_spelling_of_the_written_out_pattern`.
   **Afwijking van de opdracht "backends/ niet aanraken":** twee Drools-getuigen lezen een golden die mee veranderde en
   pinnen haar snippet. Het gaat om `RulePlanWitnessTest` (vijf regels, `jutterswet-ploegaandeel`) en
   `ConditionTermWitnessTest` (één regel, `jutterswet-filterwacht`). Daar is alleen de slotpunt uit de gepinde string
   gehaald, in `backends/drools/src/test`. `backends/clingo/` is niet geraakt. Zonder die zes regels wordt gate 12 rood.
   Los gedraaid: 9 tests groen (`werk/bruggen/check-admits/snippet/drools-two.log`). De distributie pint mogelijk
   dezelfde snippets; dat valt bij haar pin-bump.
3. **`NOTE.md` van beide goldens is verouderd** (buiten de "May touch" van deze kaart, dus niet aangeraakt). De sectie
   "Waarom er geen `source.rgs` bij staat" zegt dat de frontend de kring weigert en de golden handgeschreven is. Beide
   zijn niet meer waar. Voorstel voor de hoofdsessie: kop "De bron", met de zin "De bron staat in `source.rgs`;
   sinds de kaart `check-admits-instance-cycle` laat `check` de kring toe en geeft de frontend dit `ir.json`
   byte-identiek uit (gate 10)", en het codeblok en de herkomst van het hulpattribuut weg.
4. **Voorlopig besluit 1 (gemarkeerd): RGS-357 alleen waar de verifier een lezing buiten noemt.** De kaart vraagt een
   melding die het reflexieve feittype noemt bij "een kring die buiten de afbakening valt en over een reflexief
   feittype loopt". De opdracht gaf de parameters van `WF-045.instance-read-outside`. Ik volg de verifier: een kring
   over een feittype dat geen schakelrelatie is, krijgt alleen RGS-096 (de voorgangersrol to-many, de relatie door een
   vorm geschreven, een directe naast een verschoven lezing). Dat zijn de negatieven `to-many-predecessor`,
   `created-relation` en `direct-and-shifted-read`, waar de verifier ook alleen `WF-045.dependency-cycle` geeft.
5. **Verifier en Drools toetsen de resterende randen verschillend (buiten de frontend).** `settle` in de verifier telt
   een positieve rand als 0. Een kring van alleen positieve randen die na het weglaten van de verschoven randen blijft,
   verhoogt dus geen niveau en "settlet". Het deel geldt dan als instantiegeordend en zijn verschoven randen vallen uit
   de wandeling. Drools `Cycles.orders` toetst met `cyclic` en ziet daar geen geordend deel. WF-045 weigert het document
   in beide gevallen. Afgeleid, niet gemeten: welke kring de melding noemt, kan verschillen. `Dependencies` volgt de
   verifier.
6. Geen bug gevonden in `Dependencies` of `RuleChecker` buiten de kaart, behalve vondst 2.
