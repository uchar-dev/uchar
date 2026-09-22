# tijdlijn-startpunt-emissie: "startend op" valt stil van de wire

Gemeten door de datumranden-kaart (2026-09-22, zusterranden-meting):
bij `voor elke week startend op <datum>` (§13.3.6.3) emitteert de
frontend alleen `WEEK`; het startpunt valt volledig weg, met of zonder
tijdcomponent. Stil informatieverlies — de verboden klasse — tenzij de
wire de vorm helemaal niet kent.

Bij oppakken EERST meten: draagt de v0-wire een startpuntveld (de
knoop `ResolvedStartpunt` bestaat — de naamtabel-kaart gaf hem het
naamvoorstel StartPointDetermination), en wat doen ref/clingo/spec met
een startpunt op een tijdlijn? Drie uitkomsten mogelijk: (1) de wire
draagt het veld en de emitter vult het niet → emitterfix + getuigen;
(2) de wire kent de vorm niet → frontend weigert de zin benoemd tot
een wire-kaart hem draagt (fail-fast boven stil verlies); (3) de spec
verklaart het startpunt betekenisloos voor de gedragen semantiek →
vastleggen met bron en een emittergetuige die het bewust weglaten
pint. De meting beslist; stil blijven mag niet.

## Acceptatie (aangescherpt bij oppakken, 2026-09-22)

- (a) Het dossier hieronder legt de meting vast — de v0-wire (schema,
  emitter), de spec (`IR-SPEC.md` ch. 2.8/3.8, `RUNTIME-SEMANTICS.md`
  ch. 3.2) en de uitvoerders (`ref`, clingo) — over ALLE vier de
  vaste-startpuntproducties van de grammatica (§13.3.6.3–.7: de datum-,
  maand-, kwartaal- en jaarvorm, inline op attribuut/kenmerk/parameter
  en in een benoemde Tijdlijn-definitie), niet alleen de weekvorm:
  het is één positie en één verliesmechanisme. De route volgt uit de
  meting, met motivering.
- (b) Rood-eerst: elke nieuwe frontendgetuige (JUnit onder
  `frontends/regelspraak/src/test`) faalt eerst tegen de oude code,
  letterlijke faaluitvoer in dit dossier; dan de fix; dan groen. Na de
  fix emitteert geen vast startpunt dat de wire niet kan dragen nog
  stil: `Resolver --ir` schrijft niets, exit 1, met een ERROR-diagnostic
  die de zin en de sectie noemt; `IrEmitter.emit` weigert dezelfde
  modellen. Waar het weglaten verliesvrij is (een startpunt dat samenvalt
  met de kalenderperiode), pint een emittergetuige het bewuste weglaten
  byte-voor-byte.
- (c) Mutatieproef: de fix teruggedraaid ⇒ de getuigen rood ⇒ fix terug
  ⇒ groen, in één keten uitgevoerd, uitvoer in dit dossier.
- (d) Alle veertien gates groen, elk als losse aanroep
  `bash scripts/check-<naam>.sh` na `eval $(opam env)`; gate 11 blijft
  groen zonder `toka/` te wijzigen.

May touch: `frontends/regelspraak/**`, `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (**voorstel**), `corpus/**`,
kaartverplaatsing.

## Dossier

### Meting (2026-09-22, op basis 788d355b)

**Wire.** `spec/schema/regelspraak-ir-v0.schema.json` kent geen veld voor
een vast startpunt. `Tijdlijn` heeft precies `node`, `naam`,
`variabelStartpunt` (boolean), `granulariteit`, `aantal`
(`additionalProperties: false`). De attribuut-, kenmerk- en
parameterzijde dragen alleen `attribuutGranulariteit`/`kenmerkGranulariteit`/
`granulariteit` plus `...TijdlijnAantal` en `attribuutTijdlijnNaam`.
`ResolvedStartpunt` heeft `tijdlijn`, `objecttype`, `lid` — het is de
ankermetadata van het VARIABELE startpunt (§9.9, IR-SPEC ch. 3.8: "the
anchor metadata for a variable-start timeline"), zonder datum; WF-032
verbiedt hem op een tijdlijn met vast startpunt. Het naamvoorstel
StartPointDetermination uit `spec/proposals/v1-naamtabel.json` hoort bij
diezelfde knoop en verandert daar niets aan.

**Frontend.** De grammatica kent vier vaste-startpuntproducties
(`vastStartpuntDatum` §13.3.6.3, `-Maand` .4, `-Kwartaal` .5, `-Jaar` .6;
de datumvorm ook in `meervoudigePeriode` §13.3.6.7), inline op attribuut,
kenmerk en parameter én in `tijdlijnDefinitie`. `DefinitionResolver` las
alleen granulariteit en aantal; geen van de vier werd ergens bewaard (de
modelcommentaar zei het zelf: "only the INLINE fixed startpunt … is still
dropped here" — en ook de definitievorm viel weg). Gemeten met `make ir`
op de oude code, alle exit 0, `"diagnostics": []`:

```
week.rgs      voor elke week startend op 5-1-2026                 -> "weekwaarde": "WEEK", geen spoor van de datum
weektijd.rgs  voor elke week startend op 5-1-2026 12:00:00.000    -> idem
deftl.rgs     Tijdlijn de schoft ... een week startend op 5-1-2026 -> Tijdlijn {variabelStartpunt:false, WEEK, 1}
jaar1510.rgs  voor elk jaar startend op dag 15 van maand 10       -> byte-identiek aan 'voor elk jaar':
OUD: 'startend op dag 15 van maand 10' == 'voor elk jaar' byte-identiek
```

**Spec.** `RUNTIME-SEMANTICS.md` ch. 3.2: bucketing gebruikt
kalendergebonden wanden (maand: de 1e tot de 1e; jaar: 1 januari tot
1 januari); dag-, week- en kwartaalbucketing is niet gedefinieerd; het
enige andere rooster is het verankerde rooster van het VARIABELE
startpunt. `IR-SPEC.md` ch. 2.8 (Tijdlijn-velden) en ch. 3.8
(start-point determination) noemen geen vast startpunt. De spec verklaart
het vaste startpunt dus nergens betekenisloos; zij kent het niet.

**Uitvoerders.** `ref` (en clingo, dat `Uchar_ref.Timeline` deelt) legt een
tijdlijn zonder anker op het kalenderrooster en controleert
case-invoer daarop (`ref/lib/case.ml`, `aligned` via `Timeline.bucket`;
WEEK faalt daar met "bucketing is defined for month and year only").
Gemeten met de oude emissie van "voor elk jaar startend op dag 15 van
maand 10" en een invoertijdlijn met één segment:

```
== ref, segment 2026-10-15 .. 2027-10-15
CASE ERROR: V1.jaarwaarde: segment boundaries must sit on the declared granularity grid
== ref, segment 2026-01-01 .. 2027-01-01
CASE ERROR: empty expectation list          (de roostertoets is gepasseerd)
```

De periode die de auteur declareerde wordt als buiten het rooster
geweigerd; de kalenderperiode die hij niet declareerde wordt aanvaard.
Het startpunt is dus betekenisdragend.

### Routekeuze

**Route 2 — de frontend weigert benoemd — voor elk vast startpunt dat
afwijkt van de kalenderperiode; route 3 — bewust weglaten, gepind — voor
het kalenderstartpunt.**

- Route 1 valt af: de wire heeft geen veld; er valt niets te vullen.
- Route 3 in het algemeen valt af: de meting hierboven laat zien dat de
  uitvoerders een ander rooster hanteren dan de auteur declareerde.
- Route 3 geldt wél voor het startpunt dat SAMENVALT met de
  kalenderperiode: dag 1 van de maand (§13.3.6.4), dag 1 van maand
  1, 4, 7 en 10 (§13.3.6.5), dag 1 van maand 1 (§13.3.6.6). De
  brontaal zegt dat een tijdlijn zonder startpunt de kalenderperioden
  volgt, en dat is precies het rooster van RUNTIME-SEMANTICS ch. 3.2;
  weglaten verliest daar niets. Dat is ook de vorm van de enige
  corpusgebruiker, `corpus/golden/jutterswet-penningjaar` (gate 10 houdt
  zijn emissie byte-identiek).
- De datumvorm (§13.3.6.3) is nooit het kalenderstartpunt: een week heeft
  geen kalenderrooster (de grammatica eist daar een startpunt) en een
  meervoudige periode wordt VANAF de datum gelegd. Een datum die toevallig
  op een kalendergrens valt ("elke 3 maanden startend op 1-1-2026") wordt
  daarom ook geweigerd; dat is bewust naar de luide kant, gedocumenteerd.
  Evenzo telt elke spelling anders dan het letterlijke "1" als afwijkend.

**Waarom op de emissiegrens en niet in de resolver.** Het model lost het
vaste startpunt correct op (granulariteit en aantal kloppen); het is de
wire die het niet kan dragen. De TOKA-suite onder `toka/` (read-only,
gate 11) bevestigt die modelzijde: twee tests resolven "voor elke 5
maanden startend op …" en eisen `m.errors().isEmpty()`. Een ERROR in de
resolver zou die terecht laten falen. Daarom:
`RegelSpraakModel.vasteStartpunten` bewaart de afwijkende startpunten
(sectie, bronzin, regel); `IrEmitter.wireWeigeringen(m)` maakt er
ERROR-diagnostics van; `Resolver --ir` voegt ze toe vóór de bestaande
`isOk`-poort, zodat het niets schrijft en exit 1 geeft; en
`IrEmitter.emit` weigert hetzelfde model met een
`IllegalStateException`, zodat ook de programmatische route niet stil
emitteert. Eén functie, twee afnemers.

Na de fix, `make ir` (make's eigen rc 2 omhult de java-exit 1):

```
== week.rgs
  ERROR (line 3): tijdlijn: het vaste startpunt 'startend op 5-1-2026' (§13.3.6.3) wijkt af van de kalenderperiode en de v0-wire heeft er geen veld voor — de zin wordt geweigerd in plaats van het startpunt stil te laten vallen
rc=2 geschreven=nee
== weektijd.rgs
  ERROR (line 3): tijdlijn: het vaste startpunt 'startend op 5-1-2026 12:00:00.000' (§13.3.6.3) ...
rc=2 geschreven=nee
== deftl.rgs
  ERROR (line 1): tijdlijn: het vaste startpunt 'startend op 5-1-2026' (§13.3.6.3) ...
rc=2 geschreven=nee
== jaarkaal.rgs
ir: objecttypen=1 feittypen=0 resolvedRegels=0 geschreven naar /tmp/tsp/jaarkaal.json
rc=0 geschreven=ja
```

### Getuigen, rood eerst

`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/VastStartpuntTest.java`,
elf tests. Tien weigeringsgetuigen, elk over BEIDE routes (model lost op;
`Resolver.irModus` exit 1, niets geschreven, ERROR met sectie en bronzin;
`IrEmitter.emit` gooit met sectie en bronzin): week met datum, week met
tijdcomponent, `elke 5 maanden` met datum, maand dag 15, kwartaal
2/5/8/11, kwartaal dag 10, jaar dag 15 van maand 10, kenmerk, parameter,
drie benoemde definities (week-datum, jaar dag 1 van maand 7,
5 maanden-datum). Eén route-3-pin: maand/kwartaal/jaar met
kalenderstartpunt emitteren byte-identiek aan de vorm zonder startpunt.

Eerste rode run tegen de oude code (de pin is per constructie groen: hij
pint bestaand gedrag, zijn roodheid volgt uit M3 hieronder):

```
[        11 tests found           ]
[         1 tests successful      ]
[        10 tests failed          ]
    => org.opentest4j.AssertionFailedError: --ir moet weigeren (exit 1), kreeg 0:
 ==> expected: <1> but was: <0>
```

Na de fix de eerste keer 10/11: de benoemde-tijdlijngetuige schreef
"voor elk boekjaar", wat de grammatica niet kent (`ELKE tijdlijnNaam`);
de getuige is naar "voor elke boekjaar" gezet (zoals
`jutterswet-penningjaar`). Daarna zijn twee voorbeeldwaarden die uit de
brontaalspecificatie stamden (een jaar vanaf 12 februari, een blok vanaf
9 maart 2025) vervangen door eigen waarden (15 oktober, 1-4-2026), omdat
artefactzones geen spec-tekst dragen. M1 hieronder is met de definitieve
getuigentekst opnieuw gedraaid.

### Mutatieproeven (werkelijk uitgevoerd, `/tmp/tsp/run.sh` bouwt schoon en draait de klasse)

M1 — de hele fix teruggedraaid (`git checkout 788d355b --` op de vier
bronbestanden), definitieve getuigentekst:

```
build-rc=0
testbuild-rc=0
│     ├─ kenmerk_met_een_vast_startpunt_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ kwartaal_op_een_andere_dag_dan_1_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ week_startend_op_een_datum_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ jaar_startend_buiten_1_januari_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ parameter_met_een_vast_startpunt_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ maand_startend_op_een_andere_dag_dan_1_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ kwartaal_buiten_de_kalenderkwartalen_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ meervoudige_periode_startend_op_een_datum_wordt_geweigerd() ✘ --ir moet weigeren
│     ├─ benoemde_tijdlijn_met_een_vast_startpunt_wordt_geweigerd() ✘ --ir moet weigeren
│     └─ week_startend_op_een_datum_met_tijdcomponent_wordt_geweigerd() ✘ --ir moet weigeren
[        11 tests found           ]
[         1 tests successful      ]
[        10 tests failed          ]
hersteld: DefinitionResolver.java
hersteld: IrEmitter.java
hersteld: Resolver.java
hersteld: RegelSpraakModel.java
=== na herstel
[        11 tests found           ]
[        11 tests successful      ]
[         0 tests failed          ]
```

M2 — alleen de weigering in `IrEmitter.emit` weggehaald (de `--ir`-route
intact):

```
│     ├─ kenmerk_met_een_vast_startpunt_wordt_geweigerd() ✘ IrEmitter.emit mag het startpunt niet stil laten vallen ==> Expected java.lang.IllegalStateException to be thrown, but nothing was thrown.
  ... (idem voor alle tien weigeringsgetuigen)
[        11 tests found           ]
[         1 tests successful      ]
[        10 tests failed          ]
hersteld: IrEmitter.java
```

M3 — de kalenderclassificatie te breed (maand weigert ook dag 1):

```
│     ├─ het_kalenderstartpunt_valt_bewust_en_verliesvrij_weg() ✘ het kalenderstartpunt emitteert ==> expected: <0> but was: <1>
[        11 tests found           ]
[        10 tests successful      ]
[         1 tests failed          ]
hersteld: DefinitionResolver.java
=== na herstel
[        11 tests found           ]
[        11 tests successful      ]
[         0 tests failed          ]
```

Elk herstel is met `cmp` tegen de fixkopie bevestigd (de regels
"hersteld: …" worden alleen bij `cmp`-succes geprint).

## Result

**Route 2 (weigeren) voor het afwijkende vaste startpunt, route 3
(bewust weglaten, gepind) voor het kalenderstartpunt.** De v0-wire heeft
geen veld voor een vast startpunt (`ResolvedStartpunt` is het variabele
anker), en `ref`/clingo leggen een ankerloze tijdlijn op het
kalenderrooster: gemeten weigert `ref` de periode die de auteur
declareerde als buiten het rooster en aanvaardt de kalenderperiode. Het
verlies was dus niet neutraal, en het trof alle vier de
vaste-startpuntproducties (§13.3.6.3–.7), inline én in een benoemde
definitie, niet alleen de weekvorm.

- (a) Gehaald: meting over wire, spec en uitvoerders en de routekeuze staan
  hierboven, met de letterlijke uitvoer.
- (b) Gehaald: `VastStartpuntTest` (11 tests), tien rood tegen de oude
  code, daarna groen. De weigering zit op de emissiegrens
  (`Resolver --ir` en `IrEmitter.emit`), niet in de resolver, omdat de
  modelzijde klopt en de TOKA-suite die modelzijde eist (gate 11 blijft
  groen zonder `toka/` te raken). Er emitteert geen vast startpunt meer
  stil: het afwijkende wordt benoemd geweigerd, het kalenderstartpunt
  byte-voor-byte gepind.
- (c) Gehaald: M1, M2 en M3 hierboven, elk met herstel en `cmp`.
- (d) **Niet volledig gehaald: gate 1 is rood, en was dat al op de
  basis.** Dertien van de veertien gates zijn groen; gate 1 faalt op een
  regel die niet van deze kaart is:

  ```
  backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/BonenveldWitnessTest.java:117:        // side the frontend picks for an objectcreatie-placed link; the only other witness is the toka zone, which
  FAIL: TOKA material in artifact zone 'backends' (rights policy, corpus/README.md; the sole exception is the isolated toka/ zone)
  ```

  Op een schone checkout van 788d355b in een tijdelijke worktree geeft
  gate 1 dezelfde fout (rc=1). De regel komt uit commit 58d32a6c ("De
  hop-weigerpoort krijgt weer een getuige") op main. Het bestand ligt
  buiten deze kaart (backends/drools-broncode, waar P3 parallel werkt),
  dus hier niet gerepareerd. De lus van gate 1 veegt alle zones zonder
  vroeg te stoppen; dit is de enige treffer, dus de wijzigingen van deze
  kaart zijn schoon. Dit had bij het begin gemeten moeten worden
  (WAY-OF-WORKING §4.1); dat gebeurde pas bij de gateronde.

Gate-uitslagen, elk als losse aanroep na `eval $(opam env)`:

```
 1 check-self-contained   rc=1  FAIL: TOKA material in artifact zone 'backends' (bestaand op 788d355b, zie boven)
 2 check-ir-schema        PASS (schema valid; 24 pattern self-test row(s); 273 file(s) validate and round-trip, floor 192)
 3 check-verifier         PASS (273 golden clean; 431 negatief rejected for their declared rule, floor 350)
 4 check-case-schema      PASS (schema valid; 629 case file(s) validate)
 5 check-oracle           PASS (ref suite 775 worked examples, floor 489; 588 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
 6 check-clingo           PASS (backend suite 479 checks, floor 83; 588 cases replay exactly through clingo — ref = clingo over the corpus)
 7 check-noop             PASS (552 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
 8 check-rood             PASS (41 red cases fault observably in ref and clingo, floor 26; 82 pinned fragments cite none of the 36 listed bottom sentences); Drools leg PASS (9 fault-pinned, 32 not-carried with reason)
 9 check-catala           PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
10 check-regelspraak      PASS — JUnit suite green (391 tests, floor 173); 70 rgs golden(s) re-emit byte-identically (floor 38)
11 check-toka-parity      PASS — 45 .rgs re-emitted byte-identical, floor 45; TOKA suite 27 tests, floor 27; ref 29 / clingo 29 / Drools 29 PLAYED
12 check-drools           PASS — played matches the pinned 179; DRL-text branch played 156 (pin 156); JUnit 564 tests, floor 239
13 check-reftype-derivation PASS (131 unit annotations twisted over 27 goldens, floor 74; 128 executor replays reproduce the untouched outcome, 25 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
14 check-flogic           PASS — frontend suite green (130 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
```

Geen nieuwe golden, dus `scripts/check-drools.sh` is niet aangeraakt
(179/156 onveranderd). Geen corpuswijziging: een weigering in de
producent heeft geen `ir.json` om in `negatief/` of `rood/` te leggen; de
getuigen horen in de frontendsuite (gate 10).

**Voorstel voor een beschermd pad** (commit 60bc9659, alleen
`spec/RUNTIME-SEMANTICS.md`): ch. 3.2 krijgt een alinea **Fixed starting
points** (de wire heeft geen veld; het kalenderstartpunt valt verliesvrij
weg; elk ander zou het rooster verschuiven, dus weigert de producent;
een worked example met de `ref`-meting; OPEN: een v1-wirekaart draagt
het, met het verankerde rooster en een constant anker, of houdt de
weigering). De ch. 3.12-tabel krijgt een rij "Fixed start point off the
calendar grid". Er staat bewust geen MUST in: een producentplicht is op
de wire niet te zien en dus niet door de verifier te controleren. De
getuige is `VastStartpuntTest`. `spec/IR-SPEC.md` is niet gewijzigd.

**Voorgestelde registerrij voor `spec/CONSTRUCTS.md`** (serieel door de
hoofdsessie). Het gaat om §2.4 Model layer, de `Tijdlijn`-rij, waarvan de
laatste kolom nu `—` is. Voorgestelde tekst voor die kolom:

> **Fixed start point (2026-09-22, card `tijdlijn-startpunt-emissie`):**
> the wire carries no fixed start point (§13.3.6.3–.7) — `Tijdlijn`
> has `variableStart` only and `ResolvedStartpunt` is the variable
> anchor. Measured before the card: the RegelSpraak frontend dropped all
> four fixed-start productions silently (inline and in a named
> definition; "voor elk jaar startend op dag 15 van maand 10" emitted
> byte-identically to "voor elk jaar"), and `ref` then refused the
> author's own period as off-grid input. Now a start point off the
> calendar grid is refused by name at the emission boundary
> (`Resolver --ir` writes nothing, `IrEmitter.emit` throws; the model
> still resolves, as the toka suite requires), and the calendar start
> point (day 1 of the month / of months 1-4-7-10 / of month 1) is
> omitted losslessly, pinned byte for byte. Witness
> `frontends/regelspraak` `VastStartpuntTest` (gate 10);
> `RUNTIME-SEMANTICS.md` ch. 3.2/3.12. OPEN: a v1 wire card carries the
> fixed start point (anchored buckets with a constant anchor) or keeps
> the refusal.

In de `Geldigheid`-rij (§2.3) noemt de producentenalinea deze kaart als
eigen kaart; die verwijzing kan bij de landing "(landed 2026-09-22)"
krijgen.

**Open:** (1) gate 1 is rood op main (zie (d)); dat hoort bij een eigen
reparatie in `backends/drools`, buiten deze kaart. (2) De wirebeslissing
zelf — het vaste startpunt dragen of blijvend weigeren — staat OPEN in
ch. 3.2/3.12 en vraagt een wirekaart. (3) De datumvorm op een
kalendergrens (bijv. "elke 3 maanden startend op 1-1-2026") wordt
geweigerd hoewel hij misschien verliesvrij is; bewust, zodat de
producent niet zelf roosters gelijkstelt. Een wirekaart heft dat vanzelf
op.
