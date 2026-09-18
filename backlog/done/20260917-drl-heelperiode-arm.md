# drl-heelperiode-arm: de DRL-tekst-tak lowert ook de heelperiodematrix

Goal: De classic-DRL-tak van `backends/drools` lowert de al normatieve
`Kwantorvouw` met een §8.2-heelperiodematrix, met dezelfde uitkomst en
dezelfde luide grenzen als de exec-model-tak. Dit sluit één van de
gemeten oude-toolchainregressies: in de oorspronkelijke differentiële
fixture `PHASE_GP_ALLE` speelden beide Drools-armen vijf gevallen
(lege verzameling/vacuüm waar, een tijdlijngat, alle leden boven de
grens, exact-op-de-grens en één tegenvoorbeeld); in uchar speelt exec
die vorm en geeft classic DRL nog een `TODO §8.2 kwantorvouw`-marker.

Evidence:

- De huidige publieke getuigen zijn `jutterswet-ploegpremie`,
  `jutterswet-keurploeg`, `jutterswet-reddingsvloot` en
  `jutterswet-sleepvloot`; samen pinnen ze ALLE, tellende kwantoren,
  filter en NIET.
- De runtime-helpers bestaan al:
  `rsGeheelPeriodeAlle`, `rsGeheelPeriodeGeen` en
  `rsGeheelPeriodeTelling`. De exec-model-tak en de oorspronkelijke
  toolchain-DRL-tak leveren het patroon
  collectList-accumulate + result-var-eval.
- De niet-temporele `Kwantorvouw` speelt al in beide huidige takken.
  Deze kaart verbreedt die classic-DRL-lowering; zij ontwerpt geen
  nieuwe kwantorsemantiek.

Acceptance:

- Rood-eerst: bouw de backend en leg voor minstens één ALLE- en één
  tellende golden de huidige `EXCLUDE drl-transpiler MARKER: ...
  een §8.2-heelperiodematrix` vast:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-ploegpremie/ir.json \
    corpus/golden/jutterswet-ploegpremie/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-reddingsvloot/ir.json \
    corpus/golden/jutterswet-reddingsvloot/case*.json
  ```

- De classic-DRL-tak draagt alle vijf kwantoren (ALLE, GEEN,
  TEN_MINSTE, TEN_HOOGSTE, PRECIES), de ledenfilter en de
  NIET-prefix-semantiek: omklap bij ALLE/GEEN en telling-landing bij
  de drempelkwantoren. Lege verzameling, ontbrekend segment,
  gedeeltelijke dekking en grensgelijkheid blijven onderscheiden.
- Alle cases van de vier genoemde goldens eindigen zowel zonder vlag
  als met `--drl` in `PLAYED`; hun verwachtingen wijzigen niet.
  De gate-12 exec-model-kanaaltelling verandert dus niet. De
  backend-JUnit-suite moet de vier dual-branch replays blijvend pinnen,
  zodat `--drl` geen eenmalige handmeting blijft.
- De lowering hergebruikt de gedeelde runtimehelpers; zij kopieert de
  heelperiodetabellen niet als afwijkende MVEL-logica. Wat de
  exec-model-tak om een inhoudelijke reden weigert, weigert classic DRL
  per construct met dezelfde as en een reden, nooit met een
  compile-fout of stille lege fold.
- Noteer in het `Result:` de precieze wijziging voor
  `spec/CONSTRUCTS.md` rij 40 en voor verouderde
  "standing channel split"-proza. Een wijziging aan normatieve
  semantiek is niet nodig en heeft geen kaartmandaat.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en daarna alle veertien
  gates zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/runtime/`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`
alleen wanneer de dual-branch testnaad dat vereist,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Een noodzakelijk scriptpinvoorstel is
beperkt tot `scripts/check-drools.sh` of
`scripts/check-reftype-derivation.sh` en blijft als beschermd-padvoorstel
voor de maintainer; groei van een ondergrens alleen is geen reden om
een script te wijzigen.

Out of scope: niet-temporele kwantorproductie in de RegelSpraak-frontend
(`20260917-niettemporele-vouw-uit-bron.md`), een geneste vouw als matrix,
`bronPad`, local bindings, standing-value writes, prioritized defaults
en enige wijziging aan schema/verifier/ref/clingo of corpusverwachtingen.
Deze kaart deelt `DrlTranspiler.java` met de andere Drools-pariteitskaarten
van 2026-09-18 en wordt daarom serieel gepland, niet parallel.

---

Result: de classic-DRL-tak lowert de §8.2-heelperiodematrix van een
`Kwantorvouw`, voor alle vijf de kwantoren, mét ledenfilter en
NIET-prefix. De kanaalsplitsing die deze knoop droeg is dicht.

**Rood-eerst, letterlijk gemeten** (`mvn -q -o -f backends/drools/pom.xml
package`, daarna de runner per golden). Met `--drl`, exit 2:

```
EXCLUDE drl-transpiler MARKER: regel: premiegerechtigd bij dagvergoeding — TODO §8.2 kwantorvouw (ALLE over Berger) — een §8.2-heelperiodematrix; die wordt alleen in de exec-model-tak gelowerd: de dagvergoeding van alle bergers van de Bergingsploeg gedurende het gehele jaar groter is dan 40
EXCLUDE drl-transpiler MARKER: regel: premiewaardig bij keurvergoeding — TODO §8.2 kwantorvouw (ALLE over Keurmeester) — een §8.2-heelperiodematrix; die wordt alleen in de exec-model-tak gelowerd: de keurvergoeding van alle keurmeesters van de Keurploeg die aangemeld zijn gedurende het gehele jaar groter is dan 40
EXCLUDE drl-transpiler MARKER: regel: inzetbaar bij vaartijd — TODO §8.2 kwantorvouw (TEN_MINSTE over Reddingssloep) — een §8.2-heelperiodematrix; die wordt alleen in de exec-model-tak gelowerd: de vaartijd van ten minste 2 van de sloepen van de Reddingsvloot gedurende het gehele jaar groter is dan 20
EXCLUDE drl-transpiler MARKER: regel: inzetbaar bij sleeptijd — TODO §8.2 kwantorvouw (TEN_MINSTE over Sleepboot) — een §8.2-heelperiodematrix; die wordt alleen in de exec-model-tak gelowerd: de sleeptijd van ten minste 2 van de boten van de Sleepvloot niet gedurende het gehele jaar groter is dan 20
```

Zonder vlag, exit 0, in dezelfde meting: `ploegpremie PLAYED 7`,
`keurploeg PLAYED 7`, `reddingsvloot PLAYED 6`, `sleepvloot PLAYED 2`.

**Groen ná, beide takken.** Met `--drl` én zonder vlag, exit 0 in alle
acht runs: `ploegpremie PLAYED 7`, `keurploeg PLAYED 7`,
`reddingsvloot PLAYED 6`, `sleepvloot PLAYED 2`. Geen verwachting
gewijzigd; geen corpusbestand aangeraakt.

Corpusbrede `--drl`-sweep over de 156 goldens met cases, vóór en ná de
wijziging: **101 → 105 PLAYED**, en de diff bestaat uit precies deze vier
regels — geen enkele andere golden verandert van kanaal of van reden.

**De lowering.** `DrlTranspiler.heelperiodeVouw` wordt door
`kwantorvouw` aangeroepen nadat de join en de die/dat-subselectie in het
elementpatroon staan; die twee worden de accumulate-SOURCE, ongewijzigd,
zodat er één definitie van "welke leden" blijft. Wat de tak schrijft is
een `collectList` over de bijdragers plus één `eval` over de verzamelde
`List<List<Segment>>` — het patroon dat de exec-tak en de oorspronkelijke
toolchain-DRL-tak al lowerden. Voorbeeld uit
`jutterswet-reddingsvloot` (tellend, mét filter):

```
$subject : Reddingsvloot(  )
accumulate( $kv1 : Reddingssloep( vloot == $subject, gekeurd == true ) ; $kvtl1 : collectList( $kv1.getVaartijd() ) )
eval( rsGeheelPeriodeTelling($kvtl1, "JAAR", 2, new java.math.BigDecimal("20")) >= 2 )
```

en uit `jutterswet-sleepvloot` (dezelfde vorm onder de NIET-prefix):

```
eval( ($kvtl1.size() - rsGeheelPeriodeTelling($kvtl1, "JAAR", 2, new java.math.BigDecimal("20"))) >= 2 )
```

Geen heelperiodetabel is als MVEL herschreven: de emittertekst draagt een
aanroep en een vergelijking, meer niet. De drie gedeelde runtimehelpers
(`rsGeheelPeriodeAlle`, `rsGeheelPeriodeGeen`, `rsGeheelPeriodeTelling`)
zijn hetzelfde als die de exec-tak leest, dus de twee takken kunnen op
het heelperiodeantwoord niet uit elkaar lopen. De NIET-prefix klapt onder
ALLE/GEEN de kwantor om (∀c.¬P = ¬∃c.P) en landt onder een drempel op de
telling (`count(¬P) = n − count(P)`), waarbij `n` de grootte van de
verzamelde lijst is — `collectList` bewaart de null van een lid zónder
tijdlijn, dus dat lid telt mee en voldoet vacuüm, precies zoals ch. 3.9
het pint.

Elke vorm die deze arm níet draagt weigert per construct met dezelfde as
en dezelfde reden als de exec-tak — geen compile-fout, geen stille lege
vouw: matrixvergelijking zonder enkelperiodiek tijdlijnattribuut van het
elementtype, niet-numerieke heelperiodevergelijking, tijdlijn-vs-tijdlijn,
matrixgrens zonder exacte herschaalfactoren, datumvergelijking als
matrix, niet-scalaire matrixgrens, matrixgrens buiten de scalaire scope,
en de restvorm buiten de §8.2-verzameling. Elke afketsing rolt haar
clausules terug via `kvTerug`, dus een gemarkeerde regel houdt geen
weesclausule over.

**Getuigen bij naam.** Alles in
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/`.

- `HeelperiodeKwantorvouwWitnessTest` — 19 → 27 rijen (24 na de
  hoofdslag, 27 na de fixronde hieronder). De replay
  (`speel`) draait nu elke cel door BEIDE takken en eist dat zij
  hetzelfde verdict lezen, dus alle bestaande rijen (vacuüm, gat,
  gedeeltelijke dekking, grensgelijkheid, GEEN ≠ ¬ALLE, filter, de
  NIET-omklap, de drie tellende kwantoren, de lege verzameling onder een
  drempel, de NIET onder een drempel) zijn differentiële cellen geworden.
  Nieuw ernaast: `een_gat_midden_in_het_jaar_breekt_de_dekking_net_zo_goed`
  (elf van twaalf maanden gedekt — een lowering die alleen naar de
  buitenste tijdlijngrenzen kijkt leest dit verkeerd),
  `een_lid_zonder_tijdlijn_voldoet_vacuum_en_keert_geen_om` (ontbrekend
  segment tegenover lege verzameling: twee verschillende vacua, en maar
  één ervan houdt onder GEEN),
  `de_telling_scheidt_het_ontbrekende_segment_van_het_gat`,
  `de_drempelvergelijking_ligt_op_de_grens_en_niet_ernaast`
  (af-bij-één van beide kanten),
  `beide_takken_spelen_de_vier_corpusgoldens` (de blijvende pin: 22
  cellen, beide takken) en
  `een_mutant_per_golden_wordt_door_beide_takken_verworpen` (per golden
  één AANWEZIGE verwachting omgeklapt; PLAYED alleen is geen getuige,
  want een tak die stil niets lowert haalt elke AFWEZIG-cel gratis).
- `NietTemporeleKwantorvouwWitnessTest.de_drl_markertekst_scheidt_de_redenen`
  — de premisse verschoof: `ploegpremie` was daar de weigering die
  werkelijk exec-only was, en leest nu PLAYED. De rij houdt wat zij
  bewaakte: de geneste vouw draagt haar EIGEN reden, en geen weigering
  beweert nog dat een vorm alleen in de exec-tak zou lowerden.
- `CaseRunner.speelCase(Path, JsonNode, boolean)` is de enige
  productieregel die de testnaad vroeg: dezelfde comparator-naad door
  beide takken, package-private, zonder productieaanroeper.

De backend-suite groeide van 400 naar 408 checks, 0 skips.

**Mutatieproeven, feitelijk uitgevoerd en hersteld.**

1. Kwantor verwisseld (`alle = (kwantor == GEEN) != niet`) — **rood**:
   9 checks falen, waaronder
   `alle_leden_het_hele_jaar_boven_de_grens_is_waar`,
   `geen_enkel_lid_haalt_de_matrix_dus_de_geen_vouw_is_waar`,
   `een_lid_zonder_tijdlijn_voldoet_vacuum_en_keert_geen_om`,
   `de_niet_prefix_klapt_de_kwantor_om_in_plaats_van_het_verdict`,
   `beide_takken_spelen_de_vier_corpusgoldens` (ploegpremie FAIL) en
   `de_drl_markertekst_scheidt_de_redenen`. De gemelde reden is telkens
   "beide takken horen dezelfde cel te lezen — exec=…, drl-tekst=…".
2. Drempelvergelijking af-bij-één (`TEN_MINSTE` → `"> "`) — **rood**:
   6 checks falen, waaronder
   `de_drempelvergelijking_ligt_op_de_grens_en_niet_ernaast`,
   `de_tellende_kwantoren_lezen_de_telling_aan_beide_zijden`,
   `de_niet_prefix_landt_onder_een_drempel_op_de_telling`,
   `het_filter_kiest_de_getelde_leden` en
   `beide_takken_spelen_de_vier_corpusgoldens` (reddingsvloot FAIL).

Beide mutanten teruggedraaid; `mvn -o -f backends/drools/pom.xml package`
daarna weer `Tests run: 408, Failures: 0, Errors: 0, Skipped: 0`.

**Fixronde na de lens (2026-09-18).** Vier dingen erbij, alle vier
gemeten.

*De weigeringsassen hadden geen getuige* — terwijl de weesclausule juist
de gemeten faalmodus is. Er staan er nu drie, alledrie op de GEËMITTEERDE
DRL-TEKST en niet alleen op de status, want een gemarkeerde regel
compileert vaak gewoon en dan zegt alleen de tekst of er iets is
achtergebleven:
`een_weigeringsas_schrijft_geen_clausule_voor_hij_afketst` (een
matrixgrens op het eigen attribuut van het element, mét ledenfilter: de
arm is al door join en subselectie heen als hij afkeurt, en de tekst
bevat geen `accumulate(`),
`een_filter_dat_hijst_wordt_teruggedraaid_en_bij_naam_geweigerd` (een
die/dat-subselectie die zelf een vouw is — die hijst in de `Acc` en geeft
een lege inline zonder markering terug, dus alleen de clausuleteller
vangt haar) en
`een_vouw_onder_een_niet_conjunctieve_kwantor_weigert_bij_naam`. Beide
takken weigeren in alle drie de rijen op DEZELFDE as met dezelfde reden.
Letterlijk, met `--drl`:

```
EXCLUDE drl-transpiler MARKER: regel: Kenmerktoekenning zeewaardig — TODO §8.2 kwantorvouw (ALLE over Schuit) — een niet-scalaire matrixgrens (de eval na de accumulate bindt geen $subject): de wachtwaarde van alle schepen van de Vloot gedurende het gehele jaar groter is dan de grens
EXCLUDE drl-transpiler MARKER: regel: Kenmerktoekenning zeewaardig — TODO §8.2 kwantorvouw (ALLE over Schuit) — een die/dat-subselectie die deze tak niet IN het elementpatroon kan schrijven (Kwantorvouw): de wachtwaarde van alle schepen van de Vloot gedurende het gehele jaar groter is dan de grens
```

*De disjunctievorm brak stuk.* Een vouw als kind van een
niet-conjunctieve `Samengesteld` ("ten minste één van: alle schepen halen
de matrix; de Vloot is vlaggend") antwoordt met een eigen when-clausule
en een lege inline; de `||`-vouw schreef die lege inline als operand en
het pakket stierf. Vóór, op HEAD van de eerste commit:

```
heelperiodematrix    --drl : EXCLUDE drl compile: [Message [id=1, … [ERR 101] Line 12:25 no viable alternative at input ')' in rule "Kenmerktoekenning zeewaardig"] …
niet-temporele matrix --drl: EXCLUDE drl compile: [Message [id=1, … [ERR 101] Line 10:25 no viable alternative at input ')' in rule "Kenmerktoekenning zeewaardig"] …
```

Op `main` gemeten ter vergelijking: de heelperiodevorm markeerde daar nog
netjes (`TODO voorwaarde: ten minste één van de volgende voorwaarden`) —
zij kwam nooit tot schrijven — maar de NIET-temporele vorm brak daar al
stuk, dus het defect is niet door deze kaart ontstaan, het is er alleen
doorheen gegroeid. Ná, beide vormen:

```
EXCLUDE drl-transpiler MARKER: regel: Kenmerktoekenning zeewaardig — TODO kwantorvouw (ALLE over Schuit) onder een niet-conjunctieve kwantor: haar clausule is een eigen when-clausule en geen patroonconstraint, dus zij kan niet in een ||/!-vouw staan: ten minste één van de volgende voorwaarden
```

De exec-tak weigert dezelfde documenten met haar bestaande
`voorwaarde buiten Phase-B scope`, en `ref` is de arbiter ernaast: beide
documenten zijn `verifier: PASS` en `ref: oracle: PASS (2 expectations)`.
Het blijft dus een benoemde KANAALweigering op een welgevormd document,
geen semantiekwijziging. De vangst is één mechanisme voor beide routes:
het hijsverbod reist mét de recursie mee naar beneden (sticky, want een
binnenconjunctie binnen een disjunctie heft hem niet op), zodat de vouw
weigert vóór zij iets schrijft en met haar eigen as in de reden; de
clausuleteller op de `Samengesteld` blijft ernaast staan als net voor een
kanaal dat een volgende knoop niet leest.

*Mutatieproeven, opnieuw feitelijk uitgevoerd en hersteld.*
`kvTerug` kortgesloten tot een kale markering (geen `aggTerug`) →
**rood**, 1 check:
`een_filter_dat_hijst_wordt_teruggedraaid_en_bij_naam_geweigerd` op "en
geen weesclausule van het gehesen filter". Hijsvangst uit (vlag én net)
→ **rood**, 1 check:
`een_vouw_onder_een_niet_conjunctieve_kwantor_weigert_bij_naam` op
"heelperiodematrix: een parsefout is geen benoemde weigering: drl
compile: … [ERR 101] …". Beide hersteld; suite daarna weer
`Tests run: 408, Failures: 0, Errors: 0, Skipped: 0`.

*Onware javadoc weggewerkt.* `RegelSpraakRuntime.rsGeheelPeriodeGeen`
zei "the DRL branch does not import it (it markers the node)". De
geëmitteerde DRL van `ploegpremie` importeert en roept hem aan; de zin
noemt nu beide takken en de datum waarop zij onwaar werd.

*Corpusbrede `--drl`-sweep na de fixronde: byte-identiek aan die ervóór*
— 105 PLAYED, geen enkele reden verschoven. De disjunctievangst raakt
geen corpusdocument.

**Gate-12-kanalen ongewijzigd.** `check-drools: PASS — played 148`,
`channel floor 50 non-PLAYED goldens pinned by name; played matches the
pinned 148`, `JUnit 408 tests, floor 239`. `EXPECTED_PLAYED` blijft dus
148 en `scripts/check-drools.sh` hoeft niet te wijzigen: poort 12 speelt
de EXEC-tak, en die speelde deze vier al.

**Gate-13-takteller vóór en ná: 21 en 21.** Ongewijzigd, en de reden is
gemeten, niet aangenomen: alle vier de goldens dragen uitsluitend
`"eenheid": null`, dus poort 13 verdraait er geen annotatie in en ze
komen in haar twist-set niet voor. De regel na afloop luidt
`check-reftype-derivation: PASS (108 unit annotations twisted over 22
goldens, floor 74; 100 executor replays reproduce the untouched outcome,
21 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)`.
Geen scriptwijziging nodig, dus ook geen pinvoorstel voor
`scripts/check-reftype-derivation.sh`.

**Alle veertien poorten groen** (`eval $(opam env)`, exitcode per poort):

```
self-contained: 0     catala: 0
ir-schema: 0          regelspraak: 0
verifier: 0           toka-parity: 0
case-schema: 0        drools: 0
oracle: 0             reftype-derivation: 0
clingo: 0             flogic: 0
noop: 0
rood: 0
```

`git diff --name-only main...HEAD`:

```
backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java
backends/drools/src/main/java/io/github/yhavinga/regelspraak/runtime/RegelSpraakRuntime.java
backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java
backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/HeelperiodeKwantorvouwWitnessTest.java
backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/NietTemporeleKwantorvouwWitnessTest.java
backlog/done/20260917-drl-heelperiode-arm.md
backlog/new/20260917-drl-heelperiode-arm.md
```

Binnen "May touch"; geen `scripts/`, geen `spec/`, geen `corpus/`. Van
`runtime/` wijzigde alleen commentaar: de drie helpers die deze kaart
nodig had bestonden al en zijn hergebruikt zoals ze zijn, maar de javadoc
van `rsGeheelPeriodeGeen` beweerde nog dat de DRL-tak hem niet importeert
— waar tot deze kaart, aantoonbaar onwaar erna.

**Bewuste restscope.** De carried-set van de twee takken is op deze knoop
nog steeds niet identiek, en dat is per as gemeten en benoemd gebleven,
niet stilgelegd: de exec-tak declineert een `Samengesteld` matrix en een
matrixgrens op een parameter die de DRL-tekst-tak op de NIET-temporele
route wél draagt; een geneste vouw als matrix weigeren beide; de
`bronRol`-terugval voor het zelfgetypeerde bereik en de
`voorwaarts`-opslagrichting die de exec-tak kent heeft deze tak niet (zij
joint op het veld aan de elementzijde, zoals haar aggregaties). De
niet-scalaire matrixgrens weigert deze arm symmetrisch met de exec-tak,
om dezelfde structurele reden. En één eigenschap van het
`Samengesteld`-arm is vastgelegd in plaats van verbreed: de markering van
een samengestelde voorwaarde VERVANGT de tekst van het gemarkeerde kind,
dus de as van dat kind bereikt het rapport alleen als hij expliciet wordt
doorgegeven — wat deze kaart voor de vouw doet (`Acc.hijsReden`) en voor
geen enkele andere knoop; elke andere gemarkeerde kindvorm blijft daar
"voorwaarde" heten. De doorgifte reikt bovendien één niveau diep
(slotlens-meting): een vouw in een conjunctie bínnen een disjunctie
weigert correct en luid, maar de tussenliggende conjunctie consumeert de
as in haar herstel, zodat de buitenste markering de generieke tekst
leest (`TODO voorwaarde: …`). Meegenomen opruiming, door de slotlens
gemeten en hier genoteerd: het terugrolnet werkt nu ook op het
conjunctieve markerpad, waar main op een verder gemarkeerde regel nog
een weesclausule (`not Schuit( … )`) liet staan — veilig per
constructie en corpussweep-byte-identiek. Buiten bereik gebleven zoals de kaart
voorschreef: `bronPad`, geneste vouw, niet-temporele vouwproductie in de
frontend, local bindings, standing-value writes, prioritized defaults.

---

**Registervoorstel (NIET geland — voor de hoofdsessie).**

`spec/CONSTRUCTS.md` rij 40 draagt de kwantorvouw en beweert op drie
plaatsen dat de DRL-tekst-tak de heelperiodematrix markeert. Voorstel,
letterlijk:

1. In het `diedat-filter`-blok, de zin

   > The DRL-text branch markers this golden for its WHOLE-PERIOD matrix,
   > which is the standing channel split and unchanged

   vervangen door

   > The DRL-text branch markered this golden for its WHOLE-PERIOD matrix
   > until 2026-09-18 (kaart drl-heelperiode-arm); it plays it now, filter
   > included — the filter is that branch's accumulate SOURCE, the same
   > element pattern its non-temporal witness route uses

2. In het `tellende-kwantoren`-blok, de zin

   > The DRL-text branch still markers the WHOLE-PERIOD matrix, counting
   > quantifiers included — the standing channel split, unmoved.

   vervangen door

   > **Landed (2026-09-18, kaart drl-heelperiode-arm) — the STANDING
   > CHANNEL SPLIT of this row is CLOSED:** where that clause says the
   > DRL-text branch still markers the whole-period matrix, counting
   > quantifiers included, that branch now lowers it, for all five
   > quantifiers, with the die/dat filter and the NIET prefix. The road is
   > the one the exec branch and the production toolchain already take —
   > a `collectList` accumulate over the contributors plus one result-var
   > `eval` — because a DENIED WITNESS pattern cannot ask ch. 3.9's
   > per-bucket question of a `List<Segment>` member without putting that
   > list in a scalar constraint. What the emitted text carries is a call
   > and a comparison: the per-contributor verdict stays
   > `rsGeheelPeriode`, read through the three shared folds
   > (`rsGeheelPeriodeAlle` / `-Geen` / `-Telling`), so no whole-period
   > table is restated as MVEL and the two branches cannot drift on the
   > matrix itself. The NIET prefix lands exactly where ch. 3.9a puts it:
   > it flips the quantifier under ALLE/GEEN and lands on the COUNT under
   > a threshold, with the contributor count read off the collected list's
   > own size — which is why the member with NO timeline counts, and
   > satisfies vacuously. Measured: the four goldens
   > (`jutterswet-ploegpremie`, `-keurploeg`, `-reddingsvloot`,
   > `-sleepvloot`) go from a named marker to PLAYED in that branch with
   > the expectations gates 5 and 6 pin unchanged, 101 → 105 DRL-played
   > goldens corpus-wide with nothing else moving, and gate 12's
   > `EXPECTED_PLAYED` unmoved at 148 (that gate plays the exec branch).
   > The backend's own suite pins the four dual-branch replays
   > permanently, plus the matrix flanks no single golden separates.

3. In de laatste kolom de zinsnede

   > the DRL-text Drools branch markers the node by name (the
   > prioritizedDefaults asymmetry, second instance)

   schrappen, en in dezelfde kolom

   > the DRL-text branch declines the WHOLE-PERIOD matrix, which the exec
   > branch carries

   vervangen door

   > the DRL-text branch carries the WHOLE-PERIOD matrix since
   > 2026-09-18; what is left of the asymmetry runs the other way (the
   > exec branch declines a `Samengesteld` matrix and a PARAMETER bound
   > the DRL-text branch carries on its non-temporal route), plus the
   > `bronRol` fallback and the storage-direction split the DRL-text
   > branch's range does not have

Verouderd "standing channel split"-proza buiten rij 40, dat dezelfde
correctie vraagt maar in beschermde of buiten-bereikzones ligt:

- `spec/RUNTIME-SEMANTICS.md` §3.12, de `Quantifier fold`-rij van de
  samenvattende tabel, slotzin: "the WHOLE-PERIOD matrix remains a named
  marker in the Drools DRL-text branch alone, counting quantifiers
  included". Voorstel: vervangen door "all matrix routes of this node are
  executed by all four executors since 2026-09-18; what stays bounded is
  which matrix FORMS each executor's condition translation carries, per
  form and by name". Statusproza, geen normatieve clausule — de
  semantiek zelf is ongewijzigd en had geen kaartmandaat nodig.
- `corpus/golden/jutterswet-sleepvloot/NOTE.md` r. 77-79: "De
  DRL-tekst-tak markeert dit document voor zijn heelperiodematrix, zoals
  hij elke heelperiodevouw markeert; dat is de staande kanaalsplitsing en
  zij beweegt hier niet." Voorstel: "De DRL-tekst-tak leest de ontkende
  telling sinds 2026-09-18 zelf, langs dezelfde gedeelde
  heelperiodehelpers en met de prefix op dezelfde telling
  (`$kvtlN.size() - rsGeheelPeriodeTelling(...)`); beide Drools-takken
  spelen dit document, en de kanaalsplitsing die hier stond is dicht."
- `corpus/golden/jutterswet-keurploeg/NOTE.md` r. 103-105: "The DRL-text
  branch markers this document by name — it lowers the non-temporal
  matrix but not the §8.2 whole-period one — which is the standing
  channel split, unchanged here." Voorstel: "The DRL-text branch plays
  this document since 2026-09-18: it writes the filter into the
  accumulate SOURCE of its own collectList fold, which is the same
  element pattern its non-temporal witness route constrains — so the
  filter has one reading in both Drools branches and not two."
- `corpus/golden/jutterswet-vuurtoren/NOTE.md` r. 37-44, de hele
  "What it covers that the other three do not"-alinea. Beide helften zijn
  onwaar geworden: de DRL-tekst-tak markeert de heelperiodematrix niet
  meer en ziet `jutterswet-reddingsvloot` dus wél, en daarmee is die
  golden niet langer "zonder corpusgetuige". Omdat dit de
  bestaansreden-alinea van deze golden is, hoort er een inhoudelijk
  kloppende vervanging te staan in plaats van een schrapping — voorstel:

  > Both executor branches that carry a fold have TWO matrix routes, and
  > the counting arms are separate code in each of them. This golden is
  > the only corpus document that reaches the NON-TEMPORAL one: the
  > Drools exec branch counts over a `collectList` accumulate of the
  > CONTRIBUTORS instead of over their timelines, and the DRL-text branch
  > writes a `Number( intValue <op> N ) from accumulate( … count(…) )`
  > over the element pattern — a count over a pattern, where its
  > whole-period sibling folds a collected list through
  > `rsGeheelPeriodeTelling`. `jutterswet-reddingsvloot` pins the other
  > route in both branches (it has since 2026-09-18, when the DRL-text
  > branch gained the whole-period fold); without THIS golden the
  > non-temporal counting route has no corpus witness in either of them,
  > and the two routes could drift apart unseen.

Deze vier liggen in `spec/` respectievelijk `corpus/`, buiten de
May-touch van deze kaart, en staan hier daarom als voorstel.
