# frontend-heelperiode-in-aantalfilter: de frontend zet een subselectie met een heelperiode in een aggregatie niet om

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit het
verslag van stap 3a van die kaart. Bestaande fout van `frontends/regelspraak`. Nagemeten op `95947ae0f`
(2026-11-03): de fout staat er nog zoals beschreven.*

## Aanleiding

Een die/dat-subselectie met een heelperiodekwalificatie op de operand van een aggregatie, zoals "het aantal posten van
het Kustvak die gedurende het gehele jaar bemand zijn", zet de frontend niet om: hij schrijft een `Unresolved`-filter
en een waarschuwing. De vorm zelf is in de IR uit te drukken. Voor de getuigen van `20260923-drl-aantal-filter-zink`
is het filter met de hand in de IR ingevuld; die IR is verifier-schoon, en ref, clingo en beide Drools-takken spelen
haar. Sinds de landing van `drl-teksttak-op-truth-maintenance` staan die twee documenten als handgeschreven golden in
het corpus, zonder `source.rgs`, met de bron als toelichting in hun `NOTE.md`: `jutterswet-aantalfilter-gehele-jaar`
en `jutterswet-aantalfilter-gehele-jaar-creatieguard`.

Op `95947ae0f` geeft `make ir` over de bron uit de `NOTE.md` van `jutterswet-aantalfilter-gehele-jaar` exit 0 en twee
keer (regel 15 en 20):

    WARNING (line 15): een 'die/dat'-subselectie op een aggregatie-operand wordt nog niet omgezet; de aggregatie blijft
    buiten beschouwing in plaats van alle instanties mee te nemen (§5.6) — 'gedurende het gehele jaar bemand zijn'

en van `-creatieguard` één keer (regel 23). Het verschil met het handgeschreven `ir.json` is in beide documenten alleen
dit: elk filter is `{"node": "Unresolved", "source": …}` in plaats van de `CharacteristicCheck` `bemand` met
`"wholePeriod": {"node": "WholePeriod", "granularity": "year", "negated": false}`, en `diagnostics` draagt de
WARNINGs in plaats van `[]`. De `source`-records zijn gelijk.

## Acceptatie (aangescherpt bij het oppakken)

- (a) De frontend zet de subselectie met een heelperiode om naar het filter dat de twee goldens met de hand dragen,
  byte voor byte (`cmp` van de uitvoer van `make ir` met het `ir.json` van de golden).
- (b) De twee goldens krijgen hun `source.rgs`; gate 10 houdt ze byte voor byte gelijk. Hun `NOTE.md` verwijst naar de
  bron in `source.rgs`.
- (c) Een frontendtest, rood eerst; de frontendsuite en gate 10 groen; aan het eind `make gates` groen.

May touch: `frontends/regelspraak/**`, de twee goldens (`source.rgs` erbij, `NOTE.md`), kaartverplaatsing.

Out of scope: `ref/`, `backends/`, `spec/`.

## Result

Bouwer, 2026-11-03, branch `card/frontend-time-bugs` op main `95947ae0f`, samen met de kaart
`dagentelling-onbekende-voorwaarde`. Niet gemerged, niet gepusht.

### Commits

- `cfc929699` beide kaarten naar doing
- `e9ce472f7` frontend: een heelperiode in een subselectie wordt het vouwfilter
- en de commit van deze Results

### De meting op main (`95947ae0f`)

Onder "Aanleiding". De fout stond er nog.

### Wat er in de frontend veranderde

`ExpressionBuilder.elementCharacteristicCheckFilter` krijgt een parameter `wholePeriods`. Het filter van een
aggregatie (`aggregationFilter`, één operand) geeft `true` mee: een kwalificatie "(niet) gedurende de/het gehele
jaar|maand" wordt dan het veld `period` van de kenmerktoets van het element (`RuleChecker.wholePeriod`, dat daarvoor
pakketzichtbaar werd), ook op een kenmerk dat een type onder het element declareert (de versmallende host). Een
subselectie is geen clip, dus de kwalificatie is daar een gewoon veld van de toets (IR-SPEC ch. 4.5; WF-097 omheint
alleen de clips) en het filter leest haar per element (RUNTIME-SEMANTICS ch. 3.9). Op een kenmerk dat niet
tijdsafhankelijk is, heeft de kwalificatie geen lezing (WF-043): dan geeft de frontend de §8.2-ERROR `RGS-207`, zoals
`RuleChecker.checkWholePeriod` doet in een regelvoorwaarde, en houdt het filter de marker met `RGS-069`.

Met de bron uit `NOTE.md` geeft `make ir` nu exit 0 zonder diagnose, en `cmp` met het handgeschreven `ir.json` is
voor beide goldens gelijk. Het handgeschreven IR is niet veranderd; er was geen fout in.

De goldens: `corpus/golden/jutterswet-aantalfilter-gehele-jaar/source.rgs` en
`corpus/golden/jutterswet-aantalfilter-gehele-jaar-creatieguard/source.rgs`, de bron letterlijk uit hun `NOTE.md`. In
beide `NOTE.md` vervangt de sectie "De bron (`source.rgs`)" de sectie "Waarom er geen `source.rgs` bij staat"; de
handberekening en de herkomst blijven.

### Nieuwe codes

Geen. `RGS-207` (bestaand, `whole-period-on-non-temporal-characteristic`) heeft een tweede plek; `check-rgs-codes.sh`:
`362 codes, 384 sites`.

### Rood en mutaties

Rood eerst: `WholePeriodSubselectionTest` (4 tests) op de code van `95947ae0f`, in een losse worktree: 4 rood. Voor de
bron van de golden letterlijk:

    the_source_of_the_golden_emits_its_filter_in_both_places() ✘ [WARNING (line 15): een 'die/dat'-subselectie op
    een aggregatie-operand wordt nog niet omgezet; de aggregatie blijft buiten beschouwing in plaats van alle
    instanties mee te nemen (§5.6) — 'gedurende het gehele jaar bemand zijn', WARNING (line 20): een
    'die/dat'-subselectie op een aggregatie-operand wordt nog niet omgezet; de aggregatie blijft buiten beschouwing
    in plaats van alle instanties mee te nemen (§5.6) — 'gedurende het gehele jaar bemand zijn'] ==> expected:
    <true> but was: <false>

Op de branch: 4 groen.

Mutaties (niet gecommit, in een losse worktree, elk alleen):

| Mutatie | Rood |
|---|---|
| M9 de kwalificatie komt niet op de toets (`period = null`) | alle vier |
| M10 een kwalificatie op een niet-tijdsafhankelijk kenmerk wordt niet geweigerd | `a_characteristic_that_is_not_time_dependent_takes_no_qualifier` |
| M11 de aggregatie zet de kwalificatie niet om (`wholePeriods` `false`, de oude weigering) | alle vier |

### Floors en pins

Niet veranderd. Gate 10 telt `175 rgs golden(s) re-emit byte-identically (floor 80)`, was 173; de floor
`EXPECTED_MIN_GOLDENS=80` blijft.

### Voorgestelde CONSTRUCTS-tekst

Een aanvulling op rij 29 (Whole-period qualifier), aan het eind:

> **Frontend (2026-11-03, kaarten `frontend-heelperiode-in-aantalfilter` en `dagentelling-onbekende-voorwaarde`):**
> de RegelSpraak-frontend zet een die/dat-subselectie met de kwalificatie op de operand van een aggregatie om naar de
> kenmerktoets van het element met `wholePeriod`, met de §8.2-ERROR waar het kenmerk niet tijdsafhankelijk is; de goldens
> `jutterswet-aantalfilter-gehele-jaar` en `jutterswet-aantalfilter-gehele-jaar-creatieguard` emitteren sindsdien uit
> hun `source.rgs`. In de voorwaarde van een clip (totaal en tijdsevenredig deel, WF-097) en van een dagentelling
> (WF-058) weigert de frontend de kwalificatie met een marker, waar zij haar eerder stil liet vallen. Het filter van een
> kwantorvouw en van een unietak weigert de subselectie met een kwalificatie nog.

### Voorlopige besluiten

1. **Alleen het filter van een aggregatie** zet de kwalificatie om. Het filter van een kwantorvouw
   (`ExpressionBuilder.subselection`) en van een tak van een instantie-unie (`buildInstanceUnion`) weigeren de
   subselectie met een kwalificatie zoals op main: daar draagt geen golden de vorm, en de uitvoerders zijn er niet op
   gemeten.
2. **`RGS-207` en niet een eigen code** voor een kwalificatie op een kenmerk dat niet tijdsafhankelijk is: het is dezelfde
   weigering als in een regelvoorwaarde, in de woorden van de verifier (WF-043). Naast die ERROR staat de bestaande
   `RGS-069`-WARNING van de aggregatie, zoals bij de ERROR van `identityFilter`.

### Open punten

- De javadoc van `backends/drools/src/test/java/dev/uchar/regelspraak/cases/WholePeriodCountFilterWitnessTest.java`
  (regels 32–38) zegt dat de frontend deze vorm niet omzet en dat de goldens geen `source.rgs` dragen. Dat klopt na
  deze kaart niet meer; `backends/` ligt buiten de "May touch" van deze kaart en is de zone van een parallelle bouwer.
  Voorstel voor de hoofdsessie: die zinnen vervangen door "written in RegelSpraak; the frontend emits their IR from
  `source.rgs`, which gate 10 holds byte for byte".

### `make gates`

Eén run in de worktree, op 2026-11-03 09:46–09:52, op de tree van `764c718ec` (gedraaid toen de tip nog
`9249e6011` heette; daarna zijn alleen twee onderwerpregels ingekort, de trees zijn gelijk). Geen andere gate-set
liep bij de start. De samenvatting:

    ocaml rc=0 0s ocaml-build: fresh (stamp newer than 229 source files)
    frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
    jar rc=0 3s drools-jar: rebuilt (5 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver; 3s)
    01-self-contained rc=0 1s
    02-ir-schema rc=0 123s
    04-case-schema rc=0 7s
    03-verifier rc=0 40s
    05-oracle rc=0 23s
    06-clingo rc=0 272s
    07-noop rc=0 14s
    08-red rc=0 144s
    09-catala rc=0 5s
    10-regelspraak rc=0 234s
    11-toka-parity rc=0 222s
    12-drools rc=0 329s
    13-reftype rc=0 288s
    14-flogic rc=0 2s
    15-graph rc=0 48s
    16-scenario rc=0 40s
    17-readings rc=0 250s
    after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
    end 2026-11-03 09:52:29 332s red=0
    logs: out/gates/20261103-094657/
