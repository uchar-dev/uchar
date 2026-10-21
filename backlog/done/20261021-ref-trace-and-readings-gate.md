# ref-trace-and-readings-gate: ref als getuige van de lezingen, en de leesclausule

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U3 uit het Fable-advies over de waarden in de uitleg
(`research/uitleg-lezingen-2026-10-21/fable-advies-2026-10-21.md` in `uchar-dev/research`, §3.5, §5, §6). Besluit van de
eigenaar 2026-10-21: de lezing krijgt een plaats in de spec, met ref als tweede uitvoerder en een gate. Volgt op
`fixpoint-readings`.*

## Doel

`ref --trace <ir> <case>` print per (vormpointer, onderwerplabel, knoopnummer) hetzelfde record als de lezingen van
de Drools-backend, in canonieke tekst (getallen als exacte breuk of als decimaal zoals de case-vergelijking ze
canoniseert, datums als in de cases), op de eindwereld. RUNTIME-SEMANTICS ch. 5 krijgt een paragraaf "The reading of a
condition at the fixpoint" (per vorm met een voorwaarde en per onderwerp: de evaluatie op de fixpunt-wereld in de
volgorde van ch. 5.10 en 5.11, per knoop bereikt of niet, oordeel of waarde, fout; een vorm buiten haar geldigheid en
een verdrongen default hebben geen lezing; een lezing deponeert niets), met een uitgewerkt voorbeeld (een golden met een
samenstelling die stopt) dat een ref-test letterlijk toetst. `scripts/check-readings.sh` (gate 17) vergelijkt per
golden-case-bestand de trace van ref met de lezingen van Drools (beide takken), met `EXPECTED_MIN_*`-vloeren en een
redendragende afwijkingenlijst die leeg begint en in beide richtingen op veroudering faalt.

## Acceptatie

- (a) Rood met een mutant op beide kanten (een `Compound` die niet stopt, of een lid dat wordt bereikt terwijl de
  stopregel al vaststond), elk één keer.
- (b) Gate 17 groen over het corpus, met vloeren; gate 1 groen (geen deling van code tussen `ref/` en
  `backends/drools`).
- (c) Alle gates groen; voorgestelde `CLAUDE.md`-tekst voor gate 17 in de Result.

May touch: `ref/`, `spec/RUNTIME-SEMANTICS.md`, `scripts/check-readings.sh`, `.github/workflows/gates.yml` (de gate in
de volgorde), `spec/verifier/` alleen als de gedeelde lader iets nodig heeft; beschermde paden als voorstel, de eigenaar
merget; de kaart.

## Result (2026-10-21)

Tak `card/ref-trace-and-readings-gate`, vanaf `15f39170`. Geraakt: `ref/`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-readings.sh` en `.github/workflows/gates.yml` (voorstel), de kaart. Niet geraakt: `backends/drools`,
`spec/verifier/` (de gedeelde lader hoefde niets), `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`.

- `2c152791` de kaart naar `doing/`.
- `1e0b4c4c` ref leest de voorwaarden aan het fixpunt: `runner --trace` (stap 1).
- `006a8e53` RUNTIME-SEMANTICS 5.12: de lezing van een voorwaarde aan het fixpunt (stap 2).
- `3ec8f13c` gate 17: `scripts/check-readings.sh` en de job `readings` in `gates.yml` (stap 3).
- `fe033f71` opruiming in `reading.ml`: de wandeling over een defaultsketen geeft een bool (dezelfde trace over het
  corpus).

### Wat er is

**ref, de waarnemer.** `ref/lib/eval.ml` krijgt een waarnemer (`Eval.observer`, standaard `None`): `eval_expr` en
`eval_cond` geven elke knoop die zij binnengaan met de evaluatie ervan als closure aan de waarnemer, en zonder
waarnemer is de evaluatie die van vóór de kaart (gate 5 ongewijzigd groen, 835 cases). Drie plaatsen melden wat niet
door `eval_expr` gaat: de bijdragers van een vouw over een verzameling (`contribute`: een telling per geteld object,
een waardevouw per lid met zijn bijdrage, een lege waarde draagt niets bij; een kwantorvouw per lid het oordeel van de
matrix, de fout, of na de stop `Member_unreached`), een operand die de evaluatie als tijdlijn heel leest
(`read_whole`: geheel-periode, temporeel totaal, basis van een tijdsevenredig deel) en een voorwaarde die zij over een
tijdlijn leest (`held_over`: de klem van een temporeel totaal, het filter van een tijdsevenredig deel of een
dagtelling). Twee stukken van `run_object_creation` zijn helpers geworden (`creation_pair`, `creation_blocked`), zodat
de lezing de 1:1-wacht van de creatie zelf leest in plaats van haar na te bouwen.

**ref, de lezing.** `ref/lib/reading.ml`: per vorm met een voorwaarde (regelversie, consistentie, verdeling, object-
en feitcreatie, default) en per object waarop zij aan het fixpunt loopt, de evaluatie van de voorwaarde op de
fixpunt-wereld met de waarnemer aan, en van de waarde waar zij geldt; elke deponering valt weg, elke wereld die de
evaluatie maakt wordt weggegooid. Het bereik en de beurt komen uit de sessie: `of_type` met de typen waarop ch. 7.10
een regel in een stapel opneemt uitgesloten; de stapels uit `Eval.effective_defaults`, per keten gelopen zoals
`run_defaults_group` ze loopt (een default onder de beslisser en alles daarbinnen `DISPLACED`, net als een default
waarvan een uitzonderingsgroep besliste); een initialisatie met een invoerwaarde of een andere schrijver `DISPLACED`
(`World.init_may_write`); een 1:1-creatie met een gekoppeld onderwerp in de invoer `DISPLACED`; buiten het
geldigheidsvenster `OUTSIDE_VALIDITY`, zonder datum bij een gedateerd venster `FAULTS` zonder knopen. De nummering is
een wandeling over de AST in draadvolgorde en wordt per vorm getoetst aan de JSON zelf (de soorten van de knopen onder
de pointer, nummer voor nummer); wijkt die af, dan faalt de lezing met de vorm in de zin. Een regel op een
tijdlijndoel leest zijn waarde via `run_pro_rata` (de geschreven tijdlijn), en een periodevenster als hele voorwaarde
(de klem van ch. 3.10) als venster.

**`runner --trace <ir> <case>`** print de lezingen van een golden-case in de canonieke tekst van
`DroolsBackendMain --readings`, elke regel na `reading `, gesorteerd; exit 1 bij een evaluatiefout, 2 bij een
laadfout of een case met `expectedFault`. Ref is snel: alle 835 golden-cases in 5 s.

**De canonieke tekst, gelijk aan die van Drools.** De vier punten van de voorganger: (1) een getal als zijn exacte
decimaal waar het afbreekt, anders het quotiënt op 16 significante cijfers half-naar-even (IEEE 754 decimal64), zonder
nullen achteraan: ref doet hetzelfde (`Reading.decimal`), dus de gate vergelijkt letterlijk (3/7 → `0.4285714285714286`
op beide kanten); (2) `unordered`: de vergelijkingsregel van de gate (onder); (3) een gemaakt object heet in ref
`«"<creatie>"»(<onderwerp>)#1`, uit het label `oc<i>::<onderwerp>` en de naam van creatie i, recursief; `#1` omdat een
creatie één object per onderwerp maakt (ch. 5.5); (4) een fout als `fault <EF-code>` of `fault -`. Daarbij: de eenheid
van een leeg operand of een fout komt uit de declaratie of de annotatie van de knoop (`Eval.static_type`), zoals Drools
de eenheid van de effectieve-typewet ook bij `empty` schrijft.

**RUNTIME-SEMANTICS 5.12** (voorstel). Na 5.11, vóór hoofdstuk 6: de lezing als verslag, geen evaluatieregel; welke
vormen en objecten; de statustabel; de evaluatie (de status is die van de voorwaarde, een fout in de waarde laat haar
`HOLDS`); "deponeert niets, schrijft niets"; de nummering; de vier uitkomsten per knoop; tijdlijnen; de bijdragers; de
canonieke tekst; het uitgewerkte voorbeeld `jutterswet-ploegaandeel` (de golden van het eerste voorbeeld van 5.10) met
de tabel van de drie Jutters en de canonieke tekst van J1; de artefacten. De ref-test `WV-5.12-ploegaandeel` toetst de
tabel letterlijk (alle 36 regels, uit de golden zelf gelezen; `ref/test/dune` heeft de twee bestanden als deps, zodat
de test ook onder `dune build @ref/runtest` draait). `WV-5.12-fout-in-een-lezing` toetst een fout binnen een lezing,
die geen golden haalt (aan een fixpunt dat ref bereikte faalde geen gelezen vorm): de EF-019 van een `Unresolved`-lid
op dat lid en op de samenstelling, de status `FAULTS`, en op het andere object de stop ervoor. Suite 1304 → 1306.

**Gate 17, `scripts/check-readings.sh`.** (1) ref bouwt en `runner --trace` leest elk golden-case-bestand, ook van een
golden die Drools niet draagt; (2) de jar vers of herbouwd (`scripts/lib/drools-jar.sh`); per golden op beide takken
`--readings [--drl]` eindigt op `READINGS <n>` met n het aantal case-bestanden, of op `EXCLUDE`, op beide takken
dezelfde regel; een golden die de lezingen uitsluiten sluit de replay uit met dezelfde regel (de gewone replay wordt
gevraagd), zodat geen golden die het vierkant speelt aan de vergelijking ontsnapt; een `ERROR` faalt, tenzij de golden
in `READINGS_ERROR_KNOWN` staat en de replay hem uitsluit; (3) per case-bestand de regels van ref en van elke tak gelijk
volgens de vergelijkingsregel, een verschil faalt tenzij `DEVIATIONS` het per case en tak noemt, beide lijsten falen op
veroudering in beide richtingen; (4) tien rijen van de vergelijkingsregel zelf; (5) vloeren.

De vergelijkingsregel: elke regel letterlijk, behalve de bijdragers van een kwantorvouw, per lid vergeleken: het lid
altijd, `reached`/`unreached` waar Drools er een schrijft, het oordeel waar ref het lid bereikte. Nodig omdat Drools de
matrix voor elk lid in een eigen regel evalueert en de stopregel over de oordelen naspeelt (dus ook een lid na de stop
een oordeel geeft, waar 5.12 er geen geeft: `- unreached`), en waar de matrix niet kan falen of deponeren geen volgorde
heeft (`unordered`).

### Gemeten

Over het corpus, 2026-10-21: ref traceert 835 golden-case-bestanden (307 goldens met cases; drie goldens zonder case).
Op beide takken leest Drools 261 goldens met 706 case-bestanden; 42 sluit hij uit (`EXCLUDE`, dezelfde regel als de
replay), 4 eindigen in `ERROR` (onder). Op alle 706 case-bestanden zijn de trace van ref en de lezingen van beide takken
gelijk; de uitvoer van de exec-model- en de DRL-teksttak was bytegelijk. `DEVIATIONS` is leeg. Per tak vergeleken: 3826
lezingen (statusregels), 13971 knoopregels, waarvan 3118 `unreached`, 314 met bijdragers en 76 met een tijdlijn.

De eerste vergelijking (prototype in `werk/defaults-decider/u3/scripts/compare.py`) gaf 685 van 706 gelijk; de 21
verschillen waren drie dingen die ref niet door `eval_expr` leest: de tijdlijn-operand van een geheel-periodetoets en
van een temporeel totaal (`jutterswet-gehele-periode-leeg`(-`rgs`), `jutterswet-vast-startpunt`, `jutterswet-klipwacht`),
de klem van een temporeel totaal (`klipwacht`), de waarde van een tijdsevenredig deel (`synth2-prorata-breed`), en de
leden met een lege waarde in een waardevouw, die Drools geen bijdrager noemt (`jutterswet-sluitdag-gemeld`). Alle
drie zijn opgelost in ref, met de lezing die 5.12 nu beschrijft; geen ervan is een afwijking van Drools gebleven.

Vloeren in het script, gemeten 2026-10-21: `EXPECTED_MIN_TRACED_CASES=835`, `EXPECTED_MIN_COMPARED_GOLDENS=261`,
`EXPECTED_MIN_COMPARED_CASES=706`, `EXPECTED_MIN_READINGS=3826`, `EXPECTED_MIN_NODE_LINES=13971`,
`EXPECTED_MIN_UNREACHED=3118`, `EXPECTED_MIN_CONTRIBUTOR_LINES=314`, `EXPECTED_MIN_TIMELINE_LINES=76`.

### Gate of leg: een eigen gate 17

Gemeten looptijden, sequentieel zoals elke gate: ref over alle golden-cases 5 s; Drools `--readings` exec-model 152 s,
DRL-tekst 269 s (één JVM per golden); de hele gate 401 s, 449 s en 374 s in drie runs (`u3/logs/gate17-1.log`, `-2.log`, `-3.log`),
waarvan de replay van de 46 niet-gelezen goldens een halve minuut, en geen jar-bouw in een volledige run (gate 12 bouwt hem, `ensure_drools_jar` vindt hem vers). Als leg van
gate 12 zou dat zeven minuten toevoegen aan een gate van twaalf (696 s bij `fixpoint-readings`), met een ander
onderwerp (ref tegen de lezingen van Drools, niet Drools tegen de pins) en eigen lijsten en vloeren. Als eigen gate
draait hij los in zeven minuten, valt hij rood met zijn eigen naam, en kost hij in de volledige set niets dubbel.

### Rood met een mutant, op beide kanten (niet gecommit)

Beide met de volledige gate, elk één keer.

**ref**: de samenstelling van `eval_cond` leest na de stop door (`| Some v -> List.iter (fun c -> ignore (eval_cond env
world self ~err c)) parts; v`), het oordeel blijft. `u3/logs/gate17-mutant-ref.log`, exit 1, 224 (case, tak)-paren
verschillen (112 case-bestanden op beide takken), het eerste:

```
check-readings: FAIL — jutterswet-ploegaandeel/case.json (exec): the trace and the readings differ:
      ref     /rules/rules/0/versions/0 "J1" 4 Comparison false
      ref     /rules/rules/0/versions/0 "J1" 5 Rounding empty
      ref     /rules/rules/0/versions/0 "J1" 6 Binary empty
      ref     /rules/rules/0/versions/0 "J1" 7 AttributeReference 100
      ref     /rules/rules/0/versions/0 "J1" 8 AttributeReference 0
      ref     /rules/rules/0/versions/0 "J1" 9 Literal 0
      backend /rules/rules/0/versions/0 "J1" 4 Comparison unreached
      backend /rules/rules/0/versions/0 "J1" 5 Rounding unreached
```

Dezelfde mutant maakt de ref-suite rood: `WV-5.12-ploegaandeel`, `WV-5.12-fout-in-een-lezing` en tien andere,
onder meer de stoptests van ch. 5.10 (12 failures).

**Drools**: `RecordingRendering.compound` leest na de stop door (`if (fixed != null) { for (int j = i; j < n; j++)
test(s.members().get(j), w); return fixed; }`); `ensure_drools_jar` herbouwde de jar (7 s).
`u3/logs/gate17-mutant-drools.log`, exit 1, 224 paren, het spiegelbeeld:

```
check-readings: FAIL — jutterswet-ploegaandeel/case.json (exec): the trace and the readings differ:
      ref     /rules/rules/0/versions/0 "J1" 4 Comparison unreached
      ref     /rules/rules/0/versions/0 "J1" 5 Rounding unreached
      ref     /rules/rules/0/versions/0 "J1" 6 Binary unreached
      ref     /rules/rules/0/versions/0 "J1" 7 AttributeReference unreached
      ref     /rules/rules/0/versions/0 "J1" 8 AttributeReference unreached
      ref     /rules/rules/0/versions/0 "J1" 9 Literal unreached
      backend /rules/rules/0/versions/0 "J1" 4 Comparison false
      backend /rules/rules/0/versions/0 "J1" 5 Rounding empty
```

Beide mutanten teruggezet (`git checkout`); de slotrun herbouwde de jar uit de herstelde bron.

Gebouwd door een Opus-bouwer; de hoofdsessie draaide de volledige set, landde de `CLAUDE.md`-tekst hieronder en de
README-telling, en schreef dit Result af uit zijn concept.

### Gates

Zelf gedraaid: de ref-suite (`dune build @ref/runtest` en vanaf de root: 1306 worked examples), gate 1 (PASS), gate 5
(PASS, 835 cases, suite 1306), gate 17 (PASS, 449 s op `3ec8f13c`, `u3/logs/gate17-2.log`; slotrun op `fe033f71`, met de
jar herbouwd uit de herstelde bron: PASS in 374 s, `u3/logs/gate17-3.log`). De volledige set, door de hoofdsessie op `fe033f71` (`werk/defaults-decider/u3/gates-1`, met gate 17 toegevoegd aan
het draaiscript): alle zeventien `rc=0`, `EXIT 0` (gate 12 700 s, gate 13 186 s, gate 17 356 s).

### Vondsten buiten de kaart

- **De build met lezingen weigert vier goldens met een `IllegalStateException`** waar de replay ze uitsluit:
  `anchor/jutterswet-bootberging`, `jutterswet-ketenkwantor`, `jutterswet-muntketen` (DROOLS-067) en
  `jutterswet-havenrooster` (DROOLS-179) geven onder `--readings` (beide takken)
  `ERROR IllegalStateException: de lezing van een vorm begint terwijl die van een andere loopt`. Oorzaak:
  `TmsTranspiler` opent de lezing van een vorm (`beginReading`) vóór de verlaging en sluit haar na (`endReading`);
  weigert de verlaging de vorm halverwege, dan vangt `meet` de `Refusal` en staat `noted` nog open, zodat de volgende
  vorm bij `beginReading` weigert. Voorstel voor een kaart in `backends/drools`: `meet` sluit bij een `Refusal` een open
  lezing af (een `abandonReading()` die `noted` en `notedUnits` op null zet), en een getuige dat de build met lezingen
  elke golden die de replay uitsluit met dezelfde regel uitsluit. In gate 17 staan de vier in `READINGS_ERROR_KNOWN`,
  met reden; die lijst faalt op veroudering zodra de fout weg is. De vloer `EXPECTED_MIN_READINGS=3635` van
  `ReadingRulesFixpointWitnessTest` zag dit niet, omdat die alleen gespeelde goldens bouwt. Kaart:
  `backlog/new/20261021-reading-after-refusal.md` (hoofdsessie, in de landingscommit).
- **De eenheid van een lege waarde** komt in ref uit `Eval.static_type`, voor een samengestelde knoop dus uit zijn
  annotatie; gate 13 draait de trace niet. In het corpus staan 15 lege waarden met een eenheid, alle op een
  `AttributeReference` (eenheid uit de declaratie). Geen fout, wel een plaats waar de effectieve-typewet in ref nog op
  de annotatie leunt.

### Voorgestelde tekst voor `CLAUDE.md` (gate 17, na gate 16)

```
17. `scripts/check-readings.sh` — the reading of a condition at the
    fixpoint (RUNTIME-SEMANTICS ch. 5.12; card
    `ref-trace-and-readings-gate`, 2026-10-21). The reference
    interpreter's trace (`runner --trace`, `ref/lib/reading.ml`: per form
    with a condition and object it runs on, the evaluation of the
    condition on the fixpoint world through an observer on ref's own
    evaluation, recorded per node) and the Drools backend's readings
    (`DroolsBackendMain --readings`, card `fixpoint-readings`) are held
    equal per golden case file, in the canonical text of ch. 5.12, on
    both branches of the backend; the two share no code (gate 1). The
    trace must read every golden case file, also of a golden the backend
    does not carry; per golden both branches must end in `READINGS <n>`,
    n its case files, or in the same `EXCLUDE` line the plain replay
    prints, so no golden the square plays escapes the comparison; an
    `ERROR` fails unless the golden stands in the reason-carrying
    `READINGS_ERROR_KNOWN` (four since the gate began: the build with
    readings refuses after a form the transpiler refused part-way).
    Every line is compared verbatim except the contributors of a
    quantifier fold, compared per member: the member always,
    `reached`/`unreached` where the backend writes one (`unordered`
    where its session keeps no order), the verdict where ref reached the
    member. A difference fails unless the reason-carrying `DEVIATIONS`
    names the case and the branch (empty since the gate began); both
    lists fail when stale, in both directions, and the comparison rule
    passes ten rows of its own. Floors `EXPECTED_MIN_TRACED_CASES=835`,
    `EXPECTED_MIN_COMPARED_GOLDENS=261`, `EXPECTED_MIN_COMPARED_CASES=706`,
    `EXPECTED_MIN_READINGS=3826`, `EXPECTED_MIN_NODE_LINES=13971`,
    `EXPECTED_MIN_UNREACHED=3118`, `EXPECTED_MIN_CONTRIBUTOR_LINES=314`
    and `EXPECTED_MIN_TIMELINE_LINES=76` (measured 2026-10-21). The jar
    is taken fresh or rebuilt (`scripts/lib/drools-jar.sh`). An empty
    comparison is a failure, never silently green. CI leg unconditional
    and blocking.
```

En in gate 5, na de zin over `ref/test/`: "Since 2026-10-21 that suite carries the ch. 5.12 worked example, read off
the golden `jutterswet-ploegaandeel` itself." (optioneel; de vloer `EXPECTED_MIN_EXAMPLES=489` blijft.)

Voor `spec/CONSTRUCTS.md`: geen rij; 5.12 is geen nieuwe vorm en geen afwijking van v0, maar een clausule over wat
uitvoerders lezen. Voor `spec/PLAN.md`: de kaart is in het advies (§6 U3) gepland; geen scopewijziging.
