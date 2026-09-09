# Operand-evaluatievolgorde normatief spellen (Begrensd, TIJDSDUUR)

Goal: RUNTIME-SEMANTICS legt de evaluatievolgorde van multi-operand-
knopen normatief vast waar die volgorde executor-zichtbaar is via het
§9.5-kanaal. Vandaag is de volgorde bij twee knopen alleen af te lezen
uit de ref-binary: `Begrensd` evalueert basis→maximum→minimum en
TIJDSDUUR/ABSOLUTE_TIJDSDUUR evalueert "tot" vóór "van", maar beide
volgen uit OCamls ONgespecificeerde argumentvolgorde bij gelabelde
aanroepen (`Arith.clamp`; gemeten op ocaml-base-compiler 5.4.1,
native én bytecode gelijk — een flambda-build zou kunnen flippen).
De toolchain-Let-port (20260901-toolchain-let-port, Result) heeft die
volgorde in haar statische spoorwandeling gepind op "wat de
referentie-binary doet"; dat hoort andersom: de spec spelt de
volgorde, ref bewijst haar.

Aanleiding: de eindverificatie van de Let-port (2026-09-09) mat dat de
volgorde het §9.5-kanaal raakt — een companion-dragende operand die
vóór dan wel ná een barrière-knallende lezing evalueert, deponeert wel
of niet. Zolang de spec zwijgt is ref's toevallige volgorde de facto
norm.

Acceptance:
- RUNTIME-SEMANTICS draagt per multi-operand-knoop met kanaal-zichtbare
  volgorde (minimaal Begrensd en de tijdsduurfuncties; sweep de andere
  knopen en verklaar per knoop volgorde-zichtbaar of -onzichtbaar) de
  normatieve volgorde met een gewerkt voorbeeld.
- ref maakt de volgorde expliciet (lets in plaats van gelabelde
  argumentvolgorde) zodat de binary niet langer de drager is; gedrag
  byte-identiek op het corpus (0 pin-bewegingen verwacht).
- Een getuige pint de volgorde observeerbaar (kanaalfeit-telling) in
  ref én clingo; alle dertien gates groen.

May touch: spec/RUNTIME-SEMANTICS.md, ref/**, backends/clingo/**
(indien de vertaling de volgorde spelt), corpus/golden/** (getuige),
backlog/.

Out of scope: toolchain-wijzigingen (de spoorwandeling daar volgt
vanzelf bij de eerstvolgende conformance-cyclus); Drools-backend-
wijzigingen tenzij de getuige een divergentie blootlegt.

---

## Result (2026-09-09)

**De ontwerpkeuze: wire order, links naar rechts — NIET de feitelijke
volgorde.** De kaart beval aan de huidige feitelijke volgorde te
normaliseren tenzij de sweep een knoop vond waar die aantoonbaar
inconsistent is met een verwante knoop. Die is gevonden, twee keer:

1. **Binnen `Begrensd` zelf.** Ch. 2.7 spelt al "minimum applied first",
   en `Arith.clamp` past de grenzen ook in die volgorde toe. De
   *evaluatie* liep precies andersom (maximum eerst). Eén knoop met twee
   tegengestelde richtingen erin.
2. **De clingo-vertaler liep al in wire order.** Zijn tijdsduur-arm doet
   `operand_day a; operand_day b;` en daarna `expr_term … a` vóór
   `expr_term … b` — van vóór tot. ref deed het omgekeerd. Een norm op de
   feitelijke ref-volgorde had de vertaler moeten omgooien; wire order
   kost hem niets.

Daar komt bij dat wire order de ENIGE volgorde is die het document al
spelt — §5.10 ("left to right in wire order"), de fold (ch. 10.2),
`links`/`rechts`, WF-073's velddeclaratievolgorde. De twee toevallige
volgordes waren de enige twee rechts-naar-links-plekken in het systeem,
en ze waren toevallig: gemeten op deze build evalueert OCaml de
argumenten van een applicatie van rechts naar links, en `List.map`
documenteert zijn resultaat maar niet zijn toepassingsvolgorde (alleen
`List.iter`/`fold_left` leggen die vast). De spec spelt nu; ref bewijst.

**Kosten, expliciet.** De statische spoorwandeling in de toolchain heeft
de oude feitelijke volgorde gepind; die moet bij de eerstvolgende
conformance-cyclus mee (buiten scope per de kaart). Op het corpus kost
de keuze niets — zie de meting.

### De sweep (RUNTIME-SEMANTICS ch. 5.11)

Elke expressie- en conditieknoop met een oordeel. **Zichtbaar** =
minstens twee operandposities kunnen elk falen, een §9.5-feit
deponeren of een ch.-10.6-need forceren. **Gedwongen** = de datastroom
legt de volgorde al vast. **Onzichtbaar** = hooguit één operandpositie
evalueert ooit.

| knoop | oordeel | norm |
|---|---|---|
| `Binair`, `Vergelijking` | zichtbaar | `links`, dan `rechts` |
| `Begrensd` | zichtbaar | `basis`, `minimum`, `maximum` |
| tijdsduur / absolute tijdsduur | zichtbaar | `van` (arg 0), dan `tot` (arg 1) |
| aggregatie (som/totaal/aantal/min/max/eerste/laatste) | zichtbaar | de bijdragers in wire order (instantiefold: het filter per lid, leden in foldvolgorde), dan de `leegwaarde` |
| `concatenatie`, `TekstReeks` | zichtbaar | deelvolgorde |
| `ListLiteral`, `StructLiteral` | zichtbaar | element- resp. gedeclareerde veldvolgorde (WF-073) |
| `Invoke` | zichtbaar | argumentvolgorde, dan het lichaam |
| `Fold` | zichtbaar | `list`, `init`, dan de elementen in lijstvolgorde |
| `MapList`, `FilterList` | zichtbaar | `list`, dan elementvolgorde |
| `Samengesteld` | zichtbaar | ch. 5.10 (wire order + kortsluiting) |
| `Let` | gedwongen | het lichaam stuurt; de `value` draait op de EERSTE lezing, op de plek van die lezing |
| `Match`, `IfThenElse` | gedwongen | scrutinee/conditie kiest de arm; de niet-gekozen tak evalueert niets |
| `Afgerond`, wortel, absolute waarde, jaar/maand/dag uit, eerste paasdag, `ListLength`, `FieldAccess`, `Construct`, `DimensieCel` | onzichtbaar | één operand |
| `TemporalRead` | onzichtbaar | alleen `datum`; `basis` NOEMT de tijdlijn en evalueert nooit (ch. 3.3) |
| `Predicaat` (leeg/gevuld) | onzichtbaar | één operand |
| `Literal`, `AttribuutRef`, `ParameterRef`, `VariabeleRef`, `Constante`, `BinderRef`, `KenmerkCheck`, `Regelversie`, `Rollidmaatschap`, `Onopgelost` | onzichtbaar | geen operanden |

Regel- en vormniveau (welke regel eerst vuurt, welk lid van een
defaults-groep) valt hier NIET onder: ch. 5.2 maakt het fixpunt
volgorde-onafhankelijk en ch. 7.1 legt de defaults-ketting op prioriteit
vast. Zo staat het ook in de sectie.

### Wat ref en clingo nu dragen

- **ref** sequenceert elke multi-operandknoop met `let`s en loopt elke
  operandlijst door twee expliciet gesequenceerde wandelingen
  (`map_in_order` / `filter_in_order`) — twaalf plekken. De binary is
  niet langer de drager.
- **De som's `leegwaarde`** stond vóór de bijdragers (buiten wire
  order); nu erna. WF-037 beperkt hem tot de som, dus dit is de enige arm
  die er ooit een evalueert — voor elke andere soort verwerpt de
  verifier hem (`wf037-fallback-on-minimum`), zodat de verplaatsing niets
  anders raakt.
- **backends/clingo** had dezelfde onbepaaldheid in zijn Begrensd-arm
  (`let lo = … and hi = …`, met een effectvolle rechterkant die faalt,
  emit en `ctx.fresh` muteert). Nu twee `let`s, minimum eerst.

### De meting: nul pinbewegingen

Vóór de ref-wijziging: gate 5 = 425 cases exact, gate 6 = 425 replays
ref ≡ clingo. Ná: **425 / 425**, identiek. Ná de clingo-wijziging
nogmaals gate 6: **425**. De volgordekeuze is op het corpus van vandaag
onzichtbaar — precies waarom er getuigen bij moesten.

### De getuigen (rood-eerst, handberekend, met mutant gemeten)

| getuige | zone | pint | mutant |
|---|---|---|---|
| `synth3-operandvolgorde` | golden | `Binair` links vóór rechts, via de KANAALFEIT-TELLING; case1 pint `consistent` (0 feiten — de afwezigheid IS de pin), case2 is de controle die bewijst dat de deling wél deponeert zodra ze bereikt wordt | rechts-vóór-links: `MISMATCH: expected consistent, but the channel carries: vangstsom van een vangst × V1` |
| `begrensd-reads-bounds-in-order` | rood | `Begrensd` minimum vóór maximum, in BEIDE executors: ref door WELKE fout valt (`division by numeric zero` vóór de barrière in het maximum), clingo door WELKE grens de vertaler als eerste weigert | ref omgekeerd: `EXPECTED FAULT DID NOT OCCUR`. clingo omgekeerd: rapport zegt `a Begrensd maximum bound …`, gepinde fragment afwezig |
| `tijdsduur-reads-van-before-tot` | rood | `van` vóór `tot` in ref, via de Rekendatum-fout vóór de barrière | ref omgekeerd: `EXPECTED FAULT DID NOT OCCUR`. clingo: ONVERANDERD |

**Eerlijk over de clingo-kant.** Het golden-getuige draait in ref én
clingo (en, zo bleek, ook in Drools — de volle differentiaalvierkant).
Maar de ASP-codering sequenceert niet: haar enige volgorde-afgeleide
constructie is de §5.10-prefixbewaking, die alleen werkt omdat die
triggers monotoon zijn in de prefix. Voor Begrensd is haar volgorde een
VERTAALvolgorde (welke grens ze eerst weigert), niet een
evaluatievolgorde — genoeg om een omkering te vangen, en zo opgeschreven.
Voor tijdsduur pint ze niets: ze draagt de vorm en faalt, maar in beide
richtingen hetzelfde. Dat staat met zoveel woorden in de NOTE en in
ch. 5.11, in plaats van als sterk gepresenteerd te worden.

**Waarom Begrensd geen golden kon worden:** de ASP-backend weigert een
`Begrensd`-grens die geen grond-literal is (een lege grens is ref's GEEN
grens, wat de kale join niet kan uitdrukken). Zowel een deling als een
binderlezing draagt atomen. Voor tijdsduur sluit een andere deur: de
enige expressie-deponeerder die beide executors dragen is een deling
door een lege deler, en die moet een tijdsduur-operand via `datum + duur`
bereiken — waarop clingo een EENHEIDdragende multiplicatieve operand
weigert. Vandaar rood in plaats van goud, met de reden in de NOTE.

### Blootgelegde divergentie (gemeld, niet gerepareerd — buiten scope)

**De Drools-backend evalueert een `Begrensd` MAXIMUM vóór MINIMUM**, de
omgekeerde ch.-5.11-volgorde. In beide richtingen gemeten op precies deze
vorm:

| deling staat in | ref | Drools |
|---|---|---|
| het `minimum` (het rode geval) | **faalt** | draait door naar een model |
| het `maximum` | schrijft 100, geen fout | **faalt**, `Breuk deling door 0` |

Spiegelbeelden — dus een VOLGORDE-divergentie, geen ontbrekende controle:
Drools evalueert de deling wél, alleen van de andere kant. Met die reden
in de uitsluitingslijst van `check-rood.sh`. Voor `Binair` houdt Drools
ch. 5.11 wél — vandaar dat de golden spelend binnenkwam.

Twee dingen die de maintainer kan willen naplannen (buiten may-touch
gelaten, dus NIET aangeraakt): (a) `ASP-TRANSLATION.md` S18 kan een
notitie gebruiken dat ref's positionele barrière en clingo's
lichaamsbrede bewaking uiteenlopen zodra een deponering vóór een
barrière staat — ch. 5.11 legt die grens nu vast, S18 zwijgt erover;
(b) de Drools-Begrensd-volgorde.

### Gatestand op de kop (alle DERTIEN groen)

Vanuit de worktree, ná commit; `sh`/`bash` per shebang; `eval $(opam
env)`; gates 8/12/13 hebben de jar herbouwd.

1 PASS · 2 = **191** · 3 = **191 golden / 339 negatief** · 4 = **452** ·
5 = **427** (2 zonder case: `jutterswet-seizoenvondst`,
`jutterswet-vormenkabinet`) · 6 = **427** · 7 = **391** / 36 survivors
(GEEN nieuwe: beide golden-cases pinnen een positieve waarde, dus een
no-op executor faalt ze) · 8 = **25** rode gevallen (Drools-leg 6
fault-pinned / 19 met reden) · 9 = 201 checks / 12 catala-goldens ·
10 = 173 tests / 38 rgs-goldens · 11 = 45 / 2 holdouts / 27 / 29 —
**onbewogen** · 12 = played **119** / channel floor 47 /
value-divergence 1 / no-case 2 / JUnit **223** · 13 = **74** twists /
15 goldens / 0 survivors. Daarnaast `test_ref` **462** gewerkte
voorbeelden groen.

Stand t.o.v. aanvang: golden+rood 188 → 191, negatief 339 → 339, cases
448 → 452, replays 425 → 427, played 118 → 119, JUnit 223 → 223, twists
74 → 74.

`git status --short` leeg; de branch blijft **ONGEMERGED** (beschermd
pad: `spec/RUNTIME-SEMANTICS.md`, plus voorgestelde pins in `scripts/`).
