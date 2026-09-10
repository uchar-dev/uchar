# IfThenElse als echt record in de gedeelde Expr-unie

Goal: de laatste `Expr.Onopgelost`-drager plus identity-gesleutelde kaart uit
`backends/drools` laten vervallen door `IfThenElse` een echt record te geven
in de gedeelde `Expr`-unie (`frontends/regelspraak/.../ir/Expr.java`), zoals
`Let` en `BinderRef` dat sinds de let-lowering al hebben.

Achtergrond. De opruimkaart 20260909-onopgelost-zijkanaal-opruiming haalde de
drager en de registry weg voor `Let` en `BinderRef`: die twee zijn sinds
commit 6fba057 gewone unie-knopen, dus de loader bouwt ze rechtstreeks in de
boom en elke generieke wandeling daalt er vanzelf in af. Voor `IfThenElse`
kon dat niet. Die knoop heeft géén record in de unie — hij is *never
emitter-produced*, dus de RegelSpraak-emitter maakt er nooit een en de unie
groeide er nooit naartoe — en de unie woont in `frontends/regelspraak`, wat
buiten het may-touch-bereik van die kaart viel. Gevolg: `IrLoader` parseert
die ene wire-vorm nog naar een `Expr.Onopgelost` plus `IrLoader.Keuze` in een
`IdentityHashMap`, en `ExecModelTranspiler.letKinderen` moet daar apart
doorheen kijken omdat een drager voor elke generieke `Expr`-wandeling een
ondoorzichtig BLAD is. Dat is precies de constructie die de opruimkaart
elders wegnam; hij staat er nu nog voor één vorm.

De meting die dit kaartje rechtvaardigt (opruimkaart, 2026-09-09). De vorm is
NIET dood hout: twaalf goldens dragen een `IfThenElse` en vijf daarvan SPELEN
in gate 12 — `anchor/jutterswet-tariefstelling`,
`jutterswet-tariefrecht-catala`, `synth3-conditielezing`,
`synth3-drempelheffing`, `synth3-takbarriere`. De vorm weigeren was daarom
geen optie: dat had die vijf uit het speelkanaal geduwd en de kanaalvloer
laten vallen, oftewel een gedragswijziging in een gedragsneutrale kaart. De
overige zeven weigert de loader al eerder op een ListType/SumType/functionDefs
(`anchor/jutterswet-staffelvergoeding`, `jutterswet-kamerhuishouden-catala`,
`jutterswet-meldplichtspoor`, `jutterswet-meldtermijnen`,
`jutterswet-penningstaffel-catala`, `synth3-metingtal`,
`synth3-vangstsom`).

Waarom dit SPEC-FIRST is en geen backend-kaart. De unie is gedeeld: de
Drools-backend compileert `frontends/regelspraak/src/main/java` mee, de
frontend zelf bouwt eruit, en de toolchain consumeert deze repo op een
`SPEC_REV`-pin. Een unie-uitbreiding raakt dus de emitter-kant en gate 10
(byte-gelijke her-emissie van de rgs-goldens), en de volgorde is die van
CLAUDE.md: de delta landt hier (spec + schema + corpus), daarna volgt de
pin-bump aan de toolchain-kant. Merk op dat `IfThenElse` al IN het schema
staat (`regelspraak-ir-v0.schema.json`, als extensieknoop met de notitie
"never emitter-produced") en al normatieve semantiek heeft
(`RUNTIME-SEMANTICS.md` ch. 10.2, WF-069 positiebeperking): het ontbreekt
alleen als Java-record.

Acceptance:
- `Expr.IfThenElse` bestaat als record in de gedeelde unie, met een arm in
  `Expr.bevat` (afdalen in beide takken plus de conditie-operanden, zoals de
  `Let`-arm afdaalt) — een ontbrekende arm is daar een compileerfout, dus dat
  is geen keuze maar een verplichting.
- `IrLoader.Keuze`, `keuzeDrager` en de `keuzes`-kaart zijn WEG uit
  `IrLoader`, `ExecModelTranspiler` en `EffectieveEenheid`; `Loaded` draagt de
  kaart niet meer.
- Gate 10 byte-gelijk (de rgs-goldens her-emitteren ongewijzigd — de emitter
  produceert de vorm niet, dus er mag daar niets bewegen) en gate 12
  byte-gelijk per golden: dezelfde uitslagregel voor alle goldens, dezelfde
  kanaalvloer, dezelfde played-telling. Gate 13 onbewogen.
- Alle dertien gates groen; geen pin verplaatst.

Aandachtspunt bij de meting. Net als bij `Let` geldt: zodra de vorm een echte
unie-knoop is, is hij voor `Expr.bevat` géén blad meer, en dan zien
`heeftTijdgebonden` (§4.2/§5.3 Rekendatum), `PeriodeDekking.parameterNamen`
en `ProducentConsument` in beginsel méér dan voorheen. Bij de `Let` bleek dat
over de hele corpus nul uitslagen en nul bytes DRL te verschuiven; verwacht
hetzelfde, maar meet het, en behandel elk verschil als een bevinding in
plaats van als een verbetering die vanzelf spreekt.

May touch: frontends/regelspraak/src/main/java/**/ir/Expr.java,
backends/drools/**, en wat gate 10 nodig heeft aan de frontend-kant.

Out of scope: gedragswijzigingen; de wire-vorm zelf (schema en semantiek
staan er al); de andere executors (ref, clingo dragen de vorm langs hun eigen
weg); de toolchain-pin-bump, die als aparte PR volgt.

---

## Result (2026-09-10, branch `card/ifthenelse-in-de-unie`, basis `5661fde`)

`Expr.IfThenElse` is een record in de gedeelde unie
(`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/ir/Expr.java`),
met een arm in `Expr.bevat` die in beide takken afdaalt en de guard-operanden
via de gedeelde `Conditie.bevatExpr` meeneemt. Velden heten `conditie` / `dan`
/ `anders`: de wire spelt ze `condition` / `then` / `else` en twee daarvan zijn
Java-sleutelwoorden. Het bladental in `Expr.bevat` blijft zeven.

**De drie opruimplekken — `Keuze`, `keuzeDrager` en `keuzes` zijn weg.**

- `loader/IrLoader.java`: `case "IfThenElse"` bouwt nu `new Expr.IfThenElse(...)`
  met dezelfde vier `verplicht(...)`-controles; het record `Keuze`, de methode
  `keuzeDrager` en het veld `keuzes` zijn geschrapt. `Loaded` telt nog vier
  componenten (`model`, `prioritizedDefaults`, `rolBindingen`, `lets`).
- `transpiler/model/ExecModelTranspiler.java`: het veld `keuzes` en de
  `build`-parameter zijn weg (`build` heeft nu vijf argumenten);
  `letWaarde`, `letKinderen` en `isLetKnoop` matchen `Expr.IfThenElse` met
  `instanceof` in plaats van een identiteitsopzoeking.
- `transpiler/kern/EffectieveEenheid.java`: de constructor neemt alleen nog de
  Let-index; `eenheid`, `letEenheid` en `noteer` matchen de unie-knoop.

Twee aanroepregels moesten mee omdat de signaturen krompen:
`cases/CaseRunner.java` (één regel, het `loaded.keuzes()`-argument vervalt) en
`transpiler/DrlTranspiler.java` (de `EffectieveEenheid`-constructie plus haar
toelichting). **Melding:** `CaseRunner.java` staat op het may-touch-lijstje van
het parallelle spoor `card/reverse-rol-via-naam`. De wijziging hier is één
regel op regel 235 en dat spoor raakt regel ~495 en ~568, dus tekstueel ver
uiteen; er is geen andere route, want `Loaded` mag de kaart niet meer dragen.

`emit/IrEmitter.java` krijgt de arm die de verzegelde unie afdwingt, en die
arm **weigert luid**. Reden: de emitter produceert deze vorm niet, dus een
serialisatiepad dat niets uitvoert zou een verkeerde sleutelvolgorde of
veldnaam kunnen dragen zonder dat enige gate die bytes ooit leest. Een luide
weigering kan niet stil fout staan; een dode serialisator wel. Géén golden
draagt een `bron.rgs` met deze vorm (gecontroleerd over alle twaalf), dus
gate 10 bereikt de arm nooit.

**De hermeting op deze basis.** Twaalf goldens dragen een `IfThenElse` (samen
vijftien knopen); vijf daarvan spelen in gate 12 —
`anchor/jutterswet-tariefstelling`, `jutterswet-tariefrecht-catala`,
`synth3-conditielezing`, `synth3-drempelheffing`, `synth3-takbarriere`. De
overige zeven weigert de loader eerder op ListType/SumType/functionDefs.
Precies de telling van de kaart. Uitgangsstand: 120 PLAYED / 44 EXCLUDE /
1 FAIL (kustwacht, de gepinde value-divergence) / 2 NOCASE.

**De per-golden-bytevergelijking.** De uitslagregel die gate 12 leest
(`java -jar <shaded> <ir> <cases> | tail -1`) is voor en na afgenomen over
alle 167 goldens: **identiek, dezelfde MD5 over het hele bestand**
(`0302dacf9fa8a32cbab1f2838e159e0b`). Geen golden verandert van kanaal,
geen `PLAYED n` verandert van n, geen weigerreden verandert van tekst.

**Het aandachtspunt van de kaart — gemeten, en er is één verschuiving.**
Corpusbreed staat er onder een `IfThenElse` (guard plus beide takken):
51 `BinderRef`, 15 `Vergelijking`, 15 `Binair`, 12 `Literal`,
9 `ParameterRef`, 6 `Construct`, 6 `SumType`, 2 `Afgerond` — en
**nul `AttribuutRef`** en **nul `Rekendatum`/`Rekenjaar`**. Dus:

- `ProducentConsument` (zoekt `AttribuutRef`): nul knopen extra zichtbaar —
  nul verschuiving, zoals bij de `Let`.
- `heeftTijdgebonden` (zoekt de reken-constanten): nul knopen extra zichtbaar
  — nul verschuiving.
- `PeriodeDekking.parameterNamen` (zoekt `ParameterRef`): **negen knopen extra
  zichtbaar, zes daarvan in de vijf spelers** (`synth3-drempelheffing` 3,
  `synth3-takbarriere` 3). Dit is de bevinding. Zij is niet waarneembaar:
  `PeriodeDekking.controleer` heeft in deze repo **geen enkele aanroeper**
  (grep over alle bestandstypen: alleen het eigen bestand en backlog-proza), en
  haar eigen scoping meldt alleen over parameters die in geladen
  parametersets zitten, die het gate-pad niet laadt. Zij verschuift dus geen
  uitslag en geen byte, maar zij ziet wél meer — dat is gemeld, niet
  weggeredeneerd.

**Een tweede vlak dat de kaart niet noemde, ook gemeten.** De DRL-tekst-tak gaf
`EffectieveEenheid` vroeger een lege kaart, dus daar was de drager een
`Onopgelost`-blad en stond zijn eigen annotatie; nu daalt de afleiding ook op
die tak in de takken af. Corpusbreed draagt **geen enkele `IfThenElse` een
eenheid, en geen enkele knoop eronder draagt er een** — de afleiding antwoordt
dus hoe dan ook `null` en er kan geen byte DRL verschuiven. Gate 13 bevestigt
het: het rapport is woordelijk identiek aan de meting op de schone boom,
inclusief de DRL-tekst-telling 13.

**De rode corpus en de `toka/`-zone dragen geen enkele `IfThenElse`** — de
flank waar de zusterkaart haar bevinding vond, is hier leeg.

**De getuige** (additief, één nieuw bestand):
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/loader/IfThenElseUnieWitnessTest.java`,
drie checks over modellen die het bestand zelf schrijft (geen corpusbestand
gelezen of aangeraakt): de loader bouwt een unie-knoop in plaats van een
`Onopgelost`-drager en er blijft géén `Onopgelost` in de boom over; een
generieke `Expr.bevat`-wandeling daalt af in de dan-tak, de anders-tak én de
guard-operanden, met een niet-gelezen naam als controle; en dezelfde wandeling
bereikt de vorm ook onder een `Let`-body. Alle drie waren vóór deze kaart
structureel rood (de eerste assertie is dan een `Onopgelost`). Twee bestaande
getuigen volgden de signaturen: `EffectieveEenheidWitnessTest` (de
keuze-getuige bouwt nu `new Expr.IfThenElse(...)` en gebruikt `overLets()`;
verwachting `KG` en de boodschap ongewijzigd) en `EenheidsconflictWitnessTest`
(twee aanroepregels). Geen enkele assertie is gewijzigd.

**Gates — alle dertien groen, hier gedraaid.**

| | gate | uitkomst |
|---|---|---|
| 1 | check-self-contained | PASS |
| 2 | check-ir-schema | PASS (193 bestanden, vloer 192) |
| 3 | check-verifier | PASS (193 golden clean; 362 negatief, vloer 350) |
| 4 | check-case-schema | PASS (454 casebestanden) |
| 5 | check-oracle | PASS (496 worked examples, vloer 489; 428 cases, vloer 427) |
| 6 | check-clingo | PASS (92 checks, vloer 83; 428 cases) |
| 7 | check-noop | PASS (392 cases; 36 gepinde survivors) |
| 8 | check-rood | PASS (26 rode cases, vloer 26; Drools-leg 6 fault-pinned, 20 not-carried) |
| 9 | check-catala | PASS (201 checks, vloer 201; 12 catala-goldens, vloer 12) |
| 10 | check-regelspraak | PASS (173 tests, vloer 173; 38 rgs-goldens byte-identiek, vloer 38) |
| 11 | check-toka-parity | PASS (45 .rgs byte-identiek, vloer 45; 27 tests; 29 replays, nul divergenties) |
| 12 | check-drools | PASS (**played 120**, kanaalvloer 47; JUnit 269, vloer 239) |
| 13 | check-reftype-derivation | PASS (74 twists, vloer 74; 70 replays, 13 DRL-tekst, vloer 13; 0 survivors) |

Geen pin verplaatst. `EXPECTED_PLAYED` blijft 120, de kanaalvloer blijft 47
namen, `EXPECTED_MIN_TESTS` blijft 239 (de suite groeide van 266 naar 269 —
groei hoeft de vloer niet te verzetten). Gate 10's suite blijft 173 tests: er
is geen frontend-test bijgekomen.

**Wat bewust niet bewoog.** Het schema (`IfThenElse` stond er al, inclusief de
notitie "never emitter-produced"), `RUNTIME-SEMANTICS.md` ch. 10.2 en WF-069,
elke corpusbron en elk `ir.json`, `spec/CONSTRUCTS.md` en `spec/PLAN.md` (per
opdracht), en alle gate-scripts. De pre-existente eigenschap dat
`EffectieveEenheid.conditieOperanden` een operand én zijn afstammelingen
oplevert (`Conditie.bevatOperand` is een volledige pre-order-wandeling) is
gelaten zoals hij is — hij geldt sinds `Expr.bevat` zijn Let-arm kreeg, en hem
repareren zou een gedragswijziging zijn in een gedragsneutrale kaart; de
toelichting ter plaatse zegt nu wat er feitelijk gebeurt.

**Herpin-relevantie.** `frontends/regelspraak` beweegt: `ir/Expr.java` en
`emit/IrEmitter.java`. **`Expr.java` zit in de gevendorde slice**, dus de
toolchain-kant re-vendort deze twee bestanden bij de herpin — dat hoort bij de
herpin, niet bij deze kaart. De **uitvoer** van de emitter beweegt niet: de
resolver bouwt geen `IfThenElse`, gate 10 her-emitteert 38 rgs-goldens
byte-identiek en gate 11 45 `.rgs` uit de toka-zone, allebei ongewijzigd.

**Voorgestelde `spec/CONSTRUCTS.md`-zin (niet aangeraakt; rij 10 draagt hem).**
Rij 10 is de drager van `IfThenElse` ("de acht ch. 4.8-knopen … never
emitter-produced"); rij 24 legde de unie-landing van `Let` vast en rij 36 de
regel dat `BinderRef` sinds de let-lowering wél emitter-produced is. Er is dus
precedent voor het vastleggen van unie- en emitterstatus in deze rijen, en dit
is de laatste knoop van het cluster die hem miste. Voorstel, aan te hechten
aan rij 10:

> **Amended (2026-09-10, ifthenelse-in-de-unie card):** `IfThenElse` is sinds
> deze kaart een gewone knoop van de gedeelde `Expr`-unie — nog steeds *never
> emitter-produced* (de emitter weigert de vorm luid in plaats van een
> ongeverifieerde wire-vorm te schrijven), maar niet langer een
> `Onopgelost`-drager met een identiteits-gesleutelde kaart ernaast in de
> Drools-loader. Daarmee is de unie de inventaris van de IR en niet die van de
> emitter, en daalt elke generieke `Expr`-wandeling er per constructie in af in
> plaats van hem als ondoorzichtig blad te lezen. Gedragsneutraal gemeten: de
> gate-12-uitslagregel is byte-identiek per golden over alle 167, en gate 13
> woordelijk identiek. Eén gemeten verschuiving in wat een consument *ziet*:
> `PeriodeDekking.parameterNamen` bereikt negen `ParameterRef`s onder een
> `IfThenElse` die eerder onzichtbaar waren — zonder waarneembaar gevolg, want
> die analyse heeft in deze repo geen aanroeper.
