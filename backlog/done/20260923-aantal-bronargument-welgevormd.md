# aantal-bronargument-welgevormd: mét elementtype, zonder bron, niemand voert uit

Gemeten door de aantal-zonder-bron-kaart (2026-09-23, open punt 1):
een `AANTAL` MÉT elementtype maar ZONDER bron-argument (ariteit 0, of
één `ListLiteral` als argument) is verifier-schoon, en alle vier de
uitvoerders weigeren hem — ref met een bodemzin. Er is geen kloof
tussen de uitvoerders, maar wel een welgevormd document dat niemand
kan uitvoeren: een groene deelverzameling die geen groen is. De
WF-037-arm van die kaart dekt alleen de vorm ZONDER elementtype; deze
tweeling (elementtype gezet, bron ontbreekt) viel buiten het besluit.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet de vormen (ariteit 0 met elementtype; ListLiteral met
  elementtype) op alle vier; leg de bodemzin-route vast.
- (b) Normkeuze: de verifier eist een bron-argument bij een
  elementtype (WF-uitbreiding, negatief per spelling) — de lijn van
  de zusterbesluiten — of de vorm krijgt een lezing. Weigering ligt
  in lijn; motiveer verbatim.
- (c) Rood-eerst per negatief; mutatieproef; refs bodemzin wordt een
  benoemde arm of een invariantnet (fail-fast).
- (d) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**`, kaartverplaatsing.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Wire-namen: `AANTAL` → `Aggregation {kind: "count" | "filledCount",
  arguments, elementType?, filter?, sourceRole?, sourcePath?}`;
  `elementObjecttype` → `elementType`; `ListLiteral` en `InstanceSetUnion`
  heten zo. WF-037 staat in IR-SPEC ch. 4.3 (de Aggregation-tabel, cel
  `arguments`, r. 4169, en de regeltekst); de zusterbesluiten: opgesomd ≥ 2
  is de waardevouw (2026-09-20), ariteit 0/1 zonder elementtype (kaart
  aantal-zonder-bron, 2026-09-23; Result §11 punt 1 is deze kaart).
- De bodemzin van `ref`: `unsupported instance-fold source shape`; de
  Drools-lader weigert `ListLiteral` als niet-gedragen knoop (`Refusal`),
  ariteit 0 als SKIP/MARKER; clingo: TRANSLATION FAULT (meet of dat een
  benoemde zin of een bodemzin uit de lijst van 36 is).
- De frontend emitteert de vorm sinds aantal-zonder-bron niet meer;
  DIVERGENCES.md §13.4.17.47 "A type as the operand: no count over all
  instances (2026-09-23)".

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: weigering.** Geen lezing "alle instanties van het elementtype"
  (die normvraag is open punt 2 van aantal-zonder-bron en blijft open). Nieuwe
  arm van WF-037, geen nieuw nummer: een telling (`count` of `filledCount`)
  die `elementType` draagt, draagt PRECIES ÉÉN argument en dat argument is
  een instantiebron (de vormen die WF-096/WF-102 als setbron kennen);
  ariteit 0 en een `ListLiteral` als argument zijn ill-formed. Sluit de arm
  precies op de gemeten tweeling (elementtype met ariteit 0; elementtype met
  één `ListLiteral`); blijkt bij de meting een derde niet-bronvorm
  verifier-schoon, dan neem je haar mee en meld je het.
- U2 **Zinnen** (voorstel; de bouwer stelt vast en meldt letterlijk): `a
  count carries elementType "Vondst" but no source operand: an instance fold
  ranges over a written source (WF-037)` en `a count carries elementType
  "Vondst" and a ListLiteral operand: a listed value is no source of
  instances (WF-037)`; voor `filledCount` dezelfde zinnen met "a filled-value
  count". Zonder regelnummerstaart. `ref` krijgt voor beide vormen een
  benoemde arm met dezelfde zin (de bodemzin vervalt voor deze vormen);
  clingo idem als zijn zin een bodemzin is; Drools blijft zoals hij is
  (benoemde `Refusal`/SKIP), gemeld in het Result.
- U3 **Getuigen.** Negatiefs `wf037-aantal-elementtype-zonder-operand`,
  `wf037-aantal-elementtype-lijstliteraal`,
  `wf037-gevuldetelling-elementtype-zonder-operand` met `expect.txt` en
  NOTE (echte verifieruitvoer); rood-eerst (verifier PASS vóór de arm,
  geciteerd); mutatieproef; een ref-suitegetuige per benoemde arm.
- U4 **Meting (acceptatie (a)):** de tabel vóór en na op verifier, `ref`,
  clingo, Drools exec en DRL-tak.
- U5 **Spec.** IR-SPEC: de WF-037-regeltekst en de cel `arguments`; geen
  RS-wijziging. Geen D-/U-nummers, geen procestaal. Naamtabel-toets rc 0.
- U6 Frontend ongemoeid; gate 10 blijft groen (meten).
- U7 **Gates** voor deze bouwer: 1, 2, 3, 5, 6 (als clingo wijzigt), 8; de
  unie draait alle veertien.

## Result (2026-09-25, bouwer)

## Result aantal-bronargument-welgevormd (2026-09-25)

### Stand

Branch `card/aantal-bronargument-welgevormd`, basis main `2f47d80e`, claim
`12dc945f`, kop `5667c890`. Zeven commits, niets gepusht of gemerged; de
kaart, `spec/CONSTRUCTS.md`, `spec/PLAN.md` en `scripts/` niet aangeraakt.

    cae9c12a negatief: vijf getuigen voor de vouw met elementtype zonder bron   (rood-eerst)
    df3dadba verifier: WF-037-arm ... (voorstel)
    d8880032 ref: de vouw zonder bron weigert benoemd, in de zin van de verifier
    877c0bee clingo: de vouw zonder bron weigert benoemd, in de zin van de verifier
    714ff6bd spec: een elementType komt met zijn bron (WF-037, voorstel)
    99e4a681 clingo: ook de getypeerde ingangen noemen de vouw zonder bron
    5667c890 spec: de omgekeerde arm van WF-037 zegt precies wat hij weigert

### Wat gebouwd is

Norm U1 (weigering), nieuwe arm van WF-037, geen nieuw nummer. Twee vormen:

- (a) een aggregatie met `elementType` en GEEN argument, onder elke soort
  (acht; zie "niet klopte" 3 voor de verbreding voorbij de tellingen);
- (b) een `count` of `filledCount` met `elementType` en als enig argument
  een `ListLiteral`.

Plaatsen:
- `spec/verifier/lib/checks.ml` (voorstel): vormherkenning `sourceless`
  naast `source_position`; de arm direct na de bronarm van de telling; de
  armen van de gevulde telling (`|| sourceless <> None`) en de bereikarm van
  WF-047 (`None, Some _ when sourceless <> None -> []`) treden op de vorm
  terug, zodat één gebrek één regel houdt.
- `ref/lib/eval.ml`, `contributors ()`: twee benoemde armen direct vóór
  `Some _, _ -> "unsupported instance-fold source shape"`; die bodemzin blijft
  voor de andere vormen (grenstoets in de suite).
- `backends/clingo/lib/translate.ml`: hulpfunctie `sourceless_fold` (vóór
  `symbolic_term`), aangeroepen uit `expr_term` (beide vormen),
  `symbolic_term` en `dt_term` (vorm a). Reden voor de twee laatste: een
  tekst-, enumeratie- of datetime-getypeerde first/last/minimum/maximum komt
  niet bij `expr_term` en viel op hun bodemzinnen (gemeten, zie tabel).
- Drools: ongewijzigd (U2).
- `spec/IR-SPEC.md` (voorstel): cel `arguments` (ch. 4.3), de WF-037-
  registertekst (samenvattende zin + alinea "The converse arm"), de WF-037-
  rij van de regeltabel (tekst + vijf getuigen). Geen RS-wijziging.
  `python3 spec/proposals/v1-naamtabel-toets.py`: rc 0 (`v1-naamtabel-toets: OK`).

### De definitieve weigerzinnen (letterlijk gelijk in verifier, ref en clingo)

    a %s aggregation carries elementType "%s" but no source operand: an instance fold ranges over a written source
    a %s aggregation carries elementType "%s" and a ListLiteral operand: a listed value is no source of instances

Eerste `%s`: de soort in L5-spelling (`Spelling.encode
Spelling.aggregation_kind`): count, filledCount, sum, total, minimum,
maximum, first, last. Tweede `%s`: de elementType-naam, tussen letterlijke
dubbele aanhalingstekens (bewust geen OCaml-`%S`, dat niet-ASCII-namen zou
escapen). Geen regelnummerstaart. Voorbeelden zoals de verifier ze meldt:

    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation carries elementType "Vondst" but no source operand: an instance fold ranges over a written source
    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation carries elementType "Vondst" and a ListLiteral operand: a listed value is no source of instances
    WF-037 FAIL /rules/rules/0/versions/0/value: a filledCount aggregation carries elementType "Vondst" but no source operand: an instance fold ranges over a written source
    WF-037 FAIL /rules/rules/0/versions/0/value: a filledCount aggregation carries elementType "Vondst" and a ListLiteral operand: a listed value is no source of instances
    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries elementType "Vondst" but no source operand: an instance fold ranges over a written source

Afwijking van de U2-voorstellen: "a count" / "a filled-value count" werd
"a count aggregation" / "a filledCount aggregation" (de arm draagt acht
soorten, en een melding die een enumwaarde noemt gebruikt de L5-spelling;
WF-091 schrijft al "the sum aggregation"); de staart "(WF-037)" vervalt.
Gelijkheid machinaal getoetst op 18 documenten (verifierbericht = ref-fout
= clingo-fout, letterlijk).

### Meting vóór en na

Documenten in (buiten de repo) (generators maak*.py; model golden
jutterswet-vondsttelling; case J1 {V1,V2,V3}, J2 {}, J3 {V4}). Logs
(buiten de repo) en meet-*na.log.

    vorm (elem = elementType "Vondst")        verifier vóór        verifier na   ref vóór/na      clingo vóór/na   Drools exec / DRL (ongewijzigd)
    count  elem, geen arg, geen bereik        PASS                 WF-037 (a)    bodem / arm      bodem / arm      SKIP / MARKER
    count  elem, geen arg, sourceRole         WF-047 binding       WF-037 (a)    bodem / arm      bodem / arm      PLAYED 1 / PLAYED 1 (!)
    count  elem, geen arg, sourcePath         WF-102 (c)+(d)       WF-037 (a) + WF-102   WF-102 / WF-102   WF-102 / WF-102   loader WF-102
    count  elem, [ListLiteral 2|0], sourceRole WF-047 binding      WF-037 (b)    bodem / arm      bodem / arm      loader ListLiteral (beide)
    count  elem, [ListLiteral], geen bereik   LOAD ERROR (bereik)  idem          idem             idem             loader ListLiteral
    count  elem, [ListLiteral], sourcePath    WF-102 (c)+(d)       WF-037 (b) + WF-102   WF-102 / WF-102   WF-102 / WF-102   loader ListLiteral
    filled elem, geen arg, geen bereik        WF-037 ariteitsarm   WF-037 (a)    bodem / arm      bodem / arm      SKIP / MARKER
    filled elem, geen arg, sourceRole         WF-037 ariteit+WF-047 WF-037 (a)   bodem / arm      bodem / arm      SKIP / PLAYED 1 (!)
    filled elem, [ListLiteral], sourceRole    WF-037 setbronarm    WF-037 (b)    bodem / arm      bodem / arm      loader ListLiteral (beide)
    sum/total/min/max/first/last elem, geen arg PASS (alle zes)   WF-037 (a)    bodem / arm      bodem / arm      SKIP / MARKER
    first/last/min/max, tekst/datetime-type   (niet gemeten vóór)  WF-037 (a)    - / arm          symbolic_term- resp. dt_term-bodem / arm (na 99e4a681)
    first/last/min/max, boolean-type          -                    WF-037 (a)    - / arm          clingo weigert het booleaanse DOEL (benoemd, geen bodemzin)
    count  elem, [Literal] of [Binary], sourceRole  WF-047 binding  ongewijzigd  bodem / bodem    bodem / bodem    PLAYED 1 / PLAYED 1 (!)

bodem ref = `unsupported instance-fold source shape`; bodem clingo =
`expression kind not translated (no corpus case exercises it)`; beide in de
lijst van 36 van scripts/check-rood.sh. WF-047 binding = `a fold-source role
binding on a node that is not an instance fold over a relation`. PLAYED 1 =
de Drools-tak speelt de case en haalt 3/0/1.

Sweep vóór de arm over 737 documenten (golden 242, negatief 418, rood 47,
toka 30): nul aggregaties met elementType naast geen argument of naast één
ListLiteral, onder elke soort.

### Rood-eerst en mutatieproef

Verifier vóór: checks.ml md5 `2e9dd78483864a00da56bd327406f635`, binary
`e0bafac419b97e989317b13194932efe`. Na: `389773713ef066c7ccbf5ba414db0758`,
binary `6f7eebf248dbdefcc0ed66c291ecf56f`.

Vóór de arm: `wf037-aantal-elementtype-zonder-operand` en
`wf037-som-elementtype-zonder-operand` `verifier: PASS` rc 0;
`wf037-aantal-elementtype-lijstliteraal` alleen WF-047;
`wf037-gevuldetelling-elementtype-zonder-operand` WF-037-ariteitsarm;
`wf037-gevuldetelling-elementtype-lijstliteraal` WF-037-setbronarm. Poort 3
rood op precies deze vijf (8 FAIL-regels, geen andere), door PASS, door
"rejected, but not for WF-037", of door de expect.txt-pin. Poort 2 expliciet
over de vijf: `5 file(s) validate and round-trip`.

Sweep over 742 corpusdocumenten + 18 proefdocumenten
((buiten de repo)):
- oud → nieuw: precies de vijf getuigen, de andere 737 byte-gelijk;
- mA vormherkenning uit (`cfac5e95…`): gelijk aan oud op alle 760;
- mB arm stil, terugtreden aan (`10f3ecc1…`): alle vijf getuigen PASS;
- mC WF-047 treedt niet terug (`894a0be5…`): alleen aantal-lijstliteraal
  krijgt de WF-047-regel erbij (geen verdictwissel);
- mD gevulde armen treden niet terug (`d7f95850…`): alleen de twee
  gevuldetelling-getuigen krijgen hun oude regel erbij (geen verdictwissel).

ref-suite rood-eerst: 12 nieuwe regels (`WF-037-elementtype-*`), tegen de
oude eval.ml 10 FAIL, alle "(got: unsupported instance-fold source shape)";
de twee grensregels (ListLiteral onder sum; Literal onder count: niet deze
arm) slaagden al en slagen nog.

### Tellingen

negatief 418 → 423; expect.txt-pinnen 260 → 265; ref-suite 977 → 989;
clingo-suite 1726 (ongewijzigd); golden + rood schoon 289; cases 619.
Geen vloer aangeraakt.

### Poorten (elk los na `eval $(opam env)`, eindstand 5667c890; 5/6/8/10 na hun laatste relevante wijziging)

     1 check-self-contained  PASS
     2 check-ir-schema       PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 289 file(s) validate and round-trip, floor 192)
     3 check-verifier        PASS (289 golden clean; 423 negatief rejected for their declared rule, floor 350; 265 on their declared arm (expect.txt), floor 260)
     5 check-oracle          PASS (ref suite 989 worked examples, floor 489; 619 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
     6 check-clingo          PASS (backend suite 1726 checks, floor 83; 619 cases replay exactly through clingo — ref = clingo over the corpus)
     8 check-rood            PASS (48 red cases fault observably in ref and clingo, floor 26; 96 pinned fragments cite none of the 36 listed bottom sentences); Drools leg PASS (11 fault-pinned, 37 not-carried with reason)
    10 check-regelspraak     PASS — JUnit suite green (577 tests, floor 173); 73 rgs golden(s) re-emit byte-identically (floor 38)

Gewijzigde scriptregels: geen. Opmerking: check-regelspraak.sh heeft een
bash-shebang; `sh` faalt erop (procesvervanging), de gate draaide met bash.

### Wat aan de kaarttekst niet klopte (gemeten)

1. De kaart is op de L1-wire gemeten. Op L5 is van de tweeling alleen
   ariteit 0 zonder bereik verifier-schoon. De ListLiteral-vorm laadt niet
   zonder bereik (B12), krijgt met sourceRole WF-047 (een zin over de
   binding), met sourcePath WF-102. De arm neemt haar toch (U1/U3) en WF-047
   treedt terug; rood-eerst is voor deze getuige "poort 3 rood, niet voor
   WF-037", niet "verifier PASS".
2. filledCount + elementType + ariteit 0 werd al geweigerd (ariteitsarm,
   zin met de grond van de opgesomde vorm), de ListLiteral onder filledCount
   al door de setbronarm. Beide krijgen nu de zinnen van de arm (U2: "voor
   filledCount dezelfde zinnen"); rood-eerst via de expect.txt-pin.
3. Derde vorm (U1): onder de tellingen geen (Literal en Binary met bereik:
   WF-047). WEL verifier-schoon: dezelfde ariteit-0-vorm onder alle zes
   waardevouwen, door iedere uitvoerder geweigerd (ref/clingo met
   bodemzinnen). Meegenomen onder de clausule "blijkt bij de meting een
   derde niet-bronvorm verifier-schoon, dan neem je haar mee en meld je
   het": vorm (a) geldt onder elke soort. Vorm (b) blijft bij de tellingen:
   onder een waardevouw weigerden WF-091 (+ WF-038 of WF-047) haar al.
4. U3 noemt drie negatiefs; gebouwd zijn er vijf: plus
   `wf037-gevuldetelling-elementtype-lijstliteraal` (vorm b onder filledCount)
   en `wf037-som-elementtype-zonder-operand` (vorm a onder de waardevouwen,
   één soort voor zes, zoals `wf037-eerste-een-operand`).
5. "Drools weigert benoemd (Refusal/SKIP)" geldt voor de getuigen zelf, maar
   niet voor de varianten met bereik: zie open punt 1.
6. De clingo-zin was de bodemzin van expr_term; voor tekst/enumeratie/
   datetime-getypeerde ordenende vouwen waren het de bodemzinnen van
   symbolic_term en dt_term. Alle drie benoemd.

### Registervoorstel spec/CONSTRUCTS.md rij 1

Na de alinea "**Landed (2026-09-23, card `aantal-zonder-bron`) ...**" (eindigt
"Cost: zero existing documents change verdict."), vóór "**Landed
(2026-09-23, card `temporele-datumvouw`):**":

> **Landed (2026-09-25, card `aantal-bronargument-welgevormd`): the converse
> arm — an `elementType` needs an argument.** An aggregation that carries
> `elementType` and no argument, under every kind, and a `count` or
> `filledCount` whose one argument is a `ListLiteral`, are ill-formed under
> WF-037; no new number. Measured on the L5 wire before the arm: without an
> argument the fold was verifier-clean under every kind but `filledCount`
> (whose arity arm refused it on the listed form's ground) and every
> executor refused it, `ref` and clingo in bottom sentences; the
> `ListLiteral` under a count was refused on WF-047 alone, a sentence about
> the binding. The arm speaks two sentences, the kind in its wire spelling;
> the filled count's arity and set-source arms and WF-047 step aside on the
> shape; `ref` and clingo refuse it in the same two sentences from named
> arms (clingo in `expr_term`, `symbolic_term` and `dt_term`). Five
> negatiefs; the shape-off mutant equals the old verifier over 742
> documents. Cost: zero existing documents.

### Open punten

1. **Drools beantwoordt ill-formed invoer** (gemeten, beide takken, jar van
   2f47d80e-bron): `count` + elementType + sourceRole met geen argument, een
   Literal of een Binary: PLAYED 1 (hij telt het bereik, 3/0/1, en negeert
   het argument); `filledCount` + elementType + geen argument + sourceRole:
   DRL-tak PLAYED 1 (telde instanties als een gewone telling). De verifier
   weigert alle (WF-037 resp. WF-047). backends/drools valt buiten deze
   kaart (U2); voorstel: de Drools-lader weigert elementType zonder argument
   en een niet-referentie-argument onder een telling, in de zinnen van de
   verifier.
2. **Andere niet-bronargumenten onder een telling met elementType en
   bereik** (Literal, Binary, ParameterReference, ...) houden WF-047's
   bindingszin en de bodemzinnen van ref en clingo. Buiten U1 ("precies op
   de tweeling"). Voorstel: een bronarm van de telling onder WF-037, gespiegeld
   aan de setbronarm van de gevulde telling, met WF-047 terugtredend.
3. **De clingo-armen hebben geen suitegetuige**: test_clingo.ml niet
   aangeraakt (testbestand buiten het mandaat). Voorstel: `faults_with` naast
   de WF-110-getuigen voor telling zonder argument, telling met ListLiteral,
   een tekst- en een datetime-getypeerde first zonder argument.
4. **De verbreding naar de waardevouwen** (niet klopte 3) is een
   bouwerskeuze; terugdraaien naar alleen de tellingen: de eerste case van
   `sourceless` (verifier), de ref-arm en de clingo-aanroepen beperken tot
   count/filledCount, `wf037-som-elementtype-zonder-operand` en zes
   suiteregels schrappen.
5. Met sourcePath meldt de verifier WF-037 naast WF-102 (c) (en (d)); ref en
   clingo stoppen bij de WF-102-zin, die zij eerder toetsen. Twee gebreken,
   twee regels; geen drager in het corpus.
6. Het één-melding-kenmerk (mutanten mC, mD) ziet poort 3 niet: extra
   meldingen zijn daar toegestaan. Alleen in de NOTE's vastgelegd.
7. Producent (U6): frontend ongemoeid, poort 10 groen; de producentroute
   niet opnieuw gemeten (de zusterkaart mat dat de grammatica een operand
   eist na "het aantal").

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens A (spec/verifier-kwartet, export van de unie d43eb002; oordeel EERST FIXEN).** Voor deze kaart: B3 — de benoemde clingo-armen hadden geen getuige (mutant met alle vier `sourceless_fold`-aanroepen uit: gate 6 groen); A5 — "the only clauses … that demand a form" niet meer waar; A6 — de WF-037-indexrij noemde alleen WF-047 waar onder `filledCount` de setbronarm weigerde; A15 — Drools speelt ongeldige vormen met bereik (telling + elementType + sourceRole zonder argument, met Literal of Binary: PLAYED in beide takken); A16 — de vijf NOTE's herhalen hetzelfde mutatieblok; A17.2 — een bronarm voor de telling met bereik en een niet-bronargument (nu WF-047-bindingszin en bodemzinnen). Bevestigd: de zinnen letterlijk gelijk in verifier, ref en clingo; de verbreding naar de zes waardevouwen terecht.

**Besluiten van de hoofdsessie.** B3: vier `faults_with`-checks in de clingo-suite (mandaat verleend); A5, A6, A16 gefixt; A15 en A17.2 samen in de kaart `aantal-bronarm-en-drools-lader` (backlog/new).

**Fixronde (fixer A-exec).** B3: het frame `bronloze_vouw_doc` en vier `faults_with`-checks in de clingo-suite met de WF-037-armzin als naald (rood-eerst met alle vier `sourceless_fold`-aanroepen uit: 4 failures, elk terug op een bodemzin; per aanroepplaats valt precies de eigen check om).

**Fixronde (fixer A-spec).** A5 ("the only clauses") en A6 (de WF-037-indexrij noemt WF-047 onder `count` en de setbronarm onder `filledCount`) gefixt; A16: het mutatieblok staat alleen nog in `wf037-aantal-elementtype-zonder-operand/NOTE.md`, de vier andere NOTE's verwijzen ernaar (de vijfregelige alinea over de bodemzinnen staat nog in alle vier: open punt).
