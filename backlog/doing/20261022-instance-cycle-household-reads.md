# instance-cycle-household-reads: een lezing binnen het huishouden telt als lokaal of verschoven, en de weigering noemt de lezing

*Uit Fable-advies 2 van 2026-10-22 (`uchar-dev/research/bruggen-2026-10-22/fable-advies-2.md`, vraag 1, stap 1 van het
stappenplan, beslispunten B2.1–B2.3). Voorlopige besluiten van de hoofdsessie naar dat advies; de eigenaar kan elk
besluit omdraaien. Vervolg op `cycle-across-instances` (in `backlog/done/`).*

## Aanleiding

De afbakening van `cycle-across-instances` (IR-SPEC ch. 5.3) telt een lezing achter een interne rand van een
instantiegeordend deel alleen als lokaal of verschoven wanneer elke stap naar een to-one rol gaat. Meting 5 van die kaart
(`werk/bruggen/k12/m5/diag.txt`) laat in de v2-regelset herleving-WW vijf lezingen buiten die afbakening zien. Vier
ervan lezen binnen het eigen huishouden van het recht:

1. `herlevingsdatum van het recht art. 21 lid 1` vouwt over `eindigingsgrond.herlevingsdatum` (to-many satellieten);
2. `herlevingstermijn buiten toepassing art. 21 lid 5` toetst een kenmerk van de eindigingsgronden (een kwantor);
3. `herleving art. 21 lid 1` vouwt over `eindigingsgrond.herlevingsdatum`;
5. `eerste kalendermaand zonder overschrijding vanaf de eindmaand art. 21 lid 1` vouwt over de inkomens van het recht,
   met in het filter de beëindigingsdatum van het recht van het inkomen (de stap naar een satelliet en terug).

De vijfde, `herleving art. 21 lid 1` die de beëindigingsdatum van de rechten in de samenloop leest, is een lezing over
een hub; die blijft buiten de afbakening (B2.2, hieronder).

Verder zegt de verifier bij zo'n weigering alleen `WF-045 … <kring>`: welke lezing buiten de afbakening valt, moet de
lezer zelf zoeken. Dat is precies wat de werkbank nodig heeft.

## Voorlopige besluiten (2026-10-22, naar Fable-advies 2)

- **B2.1 Het huishouden.** Het *huishouden* van een object X van T is X met zijn satellieten over de satellietrelaties
  van T. Elk lid heeft de diepte van X (ch. 6.2). Een *huishoudstap* is een stap over een satellietrelatie van T: naar de
  satelliet (to-one of to-many; een to-many stap alleen als bereik van een vouw of kwantor, WF-053 weigert haar
  scalair) of terug naar de eigenaar (to-one).
  - **Lokaal:** nul of meer huishoudstappen vanaf het onderwerp (T of een satelliet).
  - **Verschoven:** eventueel één eigenaarsstap (als het onderwerp een satelliet is), dan één R-stap naar de
    voorganger, dan nul of meer huishoudstappen in het huishouden van de voorganger.
  - **Onveranderd blijft:** de schakelrelatie, één R en één rol per deel, elke lezing achter een interne rand
    verschoven of lokaal, één satellietrelatie per satelliettype, alleen regels en defaults-groepen, geen creatie van
    een recursieve groep, kringvrij zonder de verschoven randen, de niveaus, en EF-033.
  - **Het negatief `wf045-instance-cycle-to-many-satellite-step` vervalt.** Fable trekt het in: de kardinaliteit aan
    de satellietkant doet voor de dieptevolgorde niet ter zake, want alle satellieten van X hebben diepte(X). Het
    negatief gaat weg met reden, in de bestaande rijvorm van `corpus/negative/README.md`.
- **B2.2 Buiten de afbakening blijven,** met reden in ch. 5.3:
  - een hub, dat wil zeggen een relatie met aan de T-kant een to-many rol (`Samenloop`, `Register`); gemeten geeft die
    twee stabiele modellen (`proef/k12-2/hub/`);
  - een satelliet van een satelliet;
  - twee R-stappen;
  - een verzameling voorgangers.
  
  Er komt geen grondgraaftoets per casus.
- **B2.3 De weigering noemt de lezing.** Valt een sterk samenhangend deel met een schakelrelatie op een clausule van
  de afbakening, dan geeft de verifier naast de kring een eigen arm `WF-045.instance-cycle-read-outside`. Die heeft de
  parameters `form`, `node`, `path` en `clause`. Drools geeft dezelfde weigering in dezelfde zin.

## Voorlopige besluiten van de bouwer (2026-10-22)

*Keuzes die B2.1–B2.3 niet beslissen en die de bouw nodig had; de eigenaar kan ze omdraaien. Elk staat in IR-SPEC
ch. 5.3.*

1. **De arm heet `WF-045.instance-read-outside`.** B2.3 noemt `instance-cycle-read-outside`; gate 3 (check 5, kaart
   `verifier-arm-keys`) laat een slug van ten hoogste drie woorden toe.
2. **De arm vuurt per lezing, op een deel met een schakelrelatie.** Een deel met een
   schakelrelatie is een sterk samenhangend deel van regels en defaults-groepen zonder recursieve creatie waarvan ten
   minste één lezing achter een interne rand met een stap over een schakelrelatie begint (de eerste stap, of de
   tweede na een eigenaarsstap). De arm noemt elke lezing die noch verschoven noch lokaal is, en de clausule waarop
   zij valt; de clausules over het deel als geheel (ten minste één verschoven rand, kringvrij zonder de verschoven
   randen, alleen regels en defaults-groepen, geen recursieve creatie) noemen geen lezing en geven geen arm. Een deel
   zonder schakelrelatie (de voorganger to-many, de relatie door een feitcreatie geschreven) geeft geen arm.
3. **De clausulesleutels:** `hub`, `satellite-of-satellite`, `two-switch-steps`, `set-of-predecessors` (de vier van
   B2.2), `one-walk` (één schakelrelatie, één rol), `one-satellite-relation` (één satellietrelatie per satelliettype)
   en `shifted-or-local` (elke andere stap: een relatie met een schrijver, een reciproke relatie, een tweede relatie
   tussen vergelijkbare typen, een R-stap na een huishoudstap die geen eigenaarsstap vooraf is, een lezing zonder
   geschreven pad). Een gedeelde satelliet is een hub: de rol aan de T-kant is to-many.

## Doel

- Verifier `instance_ordering`:
  - `satellite_hop` laat de eis "to-one" vallen voor de stap naar de satelliet, en houdt hem voor de eigenaarsrol;
  - `classify` loopt over het pad, houdt het huidige type bij (T of een satelliettype) en aanvaardt de drie rijen van
    B2.1;
  - elke gebruikte satellietrelatie gaat in `sats`.
  
  De paden zijn er al (`df_paths`).
- Drools `Cycles.satelliteHop` en `classify` spiegelen de verifier. ref verandert niet, want `instance_orders` is
  gedeeld en de diepte van een satelliet loopt via `io_owners`; zie het of dat klopt. clingo verandert niet.
- Spec als voorstel:
  - IR-SPEC ch. 5.3 krijgt de drie definities en de redenen van B2.2. De zin "Without the to-one hops of a shifted read
    the read lands on a set" vervalt.
  - RUNTIME-SEMANTICS ch. 6.4 krijgt een voorbeeld met een vouw over de eigen satellieten.
  - De index krijgt de nieuwe arm.

## Acceptatie

- (a) **Rood eerst: een golden.** `jutterswet-vergunningtermijnen` is handgeschreven IR, met een `NOTE.md` met de
  handberekening. Het gaat om een `Vergunning` met to-many `Termijnen` en de volgende vormen:
  - `restant` als som over de eigen termijnen;
  - `verlengde einddag` als basiseinddag plus de som over de termijnen van de vorige vergunning;
  - een voorwaarde met alle termijnen;
  - een vouw met in het filter een lezing van de einddag van de houder.
  
  De casussen hebben ketens van één tot drie vergunningen en nul tot drie termijnen. De golden faalt gate 3 (WF-045)
  vóór de wijziging. Erna speelt hij in gates 3, 5, 6, 12 (beide takken) en 17. De volgordes worden gemeten met
  `werk/bruggen/k12/scripts/order_run.sh`. `EXPECTED_PLAYED` gaat plus één.
- (b) **Negatieven.**
  - `wf045-instance-cycle-to-many-satellite-step` gaat weg met reden.
  - Rood erbij komen `wf045-instance-cycle-satellite-of-satellite` en `wf045-instance-cycle-hub-negation`. Het laatste
    is de miniatuur van `proef/k12-2/hub/`, met de twee modellen in haar `NOTE.md`.
  - De nieuwe arm krijgt een `expect.txt` op elk bestaand negatief waar hij past.
  - Gate 3 blijft groen. De vloeren zijn ondergrenzen.
- (c) **Meting 6, zonder bestand in deze repository.** Het m5-document gaat door de verifier van de tak. Verwacht wordt
  dat precies één lezing buiten de afbakening overblijft, de samenlooplezing, en dat de nieuwe arm haar noemt.
- (d) De zeventien gates groen. De Result noemt de CONSTRUCTS-rij (rij 45 bijgewerkt) als voorgestelde tekst.

May touch: `spec/verifier/**`, `backends/drools/**` buiten de productieruntime, `ref/` (alleen als de gedeelde module het
vraagt), `corpus/golden/jutterswet-vergunningtermijnen/`, `corpus/negative/` (de genoemde gevallen en `README.md`),
`scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), `spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` als voorstel,
deze kaart. Niet: `frontends/` (dat is `check-admits-instance-cycle`, na deze kaart), `backends/clingo/`.

## Open voor de werkbank (B2.5, normvraag)

De samenlooplezing verdwijnt alleen als de werkbank de uitzondering van art. 21 lid 2 onder a herschrijft, zodat het
nieuwe recht niet wordt herkend aan een afgeleide beëindigingsdatum. Er zijn twee wegen:

- (c2) een invoerkenmerk op het recht in de samenloop (aanbevolen);
- (c1) de opvolger over `eerdere recht`.

Daarna vervallen K12, N13 en afwijking 21 in v2.

## Result (2026-10-22)

Tak `card/instance-cycle-household-reads`, vanaf main `018be1df`, gerebased op main `4970cceb` (third-condition-level
geland; het enige conflict was `EXPECTED_PLAYED`, 271 + 1 = 272) en na de gates-run op main `c98b690d`
(percentage-of-snippets: `ExpressionBuilder` en de snippets van één golden, geen conflict, geen gedeeld bestand; daarop
gates 2 en 3 opnieuw groen, de volledige set niet opnieuw). Geraakt: `spec/verifier/lib/checks.ml`,
`backends/drools/` (`Cycles`, `DroolsCode`, beide catalogi, `TmsCycleVerifierWitnessTest`), `scripts/check-drools.sh`
(alleen `EXPECTED_PLAYED`), `ref/test/test_ref.ml` (WV-6.4-m), als voorstel `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md`, het corpus (één golden, twee negatieven erbij, één weg met reden, vier `expect.txt` naar de
nieuwe arm, `corpus/negative/README.md`) en deze kaart. Niet geraakt: `ref/lib/`, `backends/clingo/`, `frontends/`, het
IR-schema, `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`, `toka/`.

- `1430e999c` de kaart naar `doing/`.
- `9b67734fd` verifier: het huishouden en de arm; de golden, de twee negatieven, het negatief weg met reden.
- `3d3d4c88a` Drools: `Cycles` spiegelt de verifier, DROOLS-292, de catalogi, `EXPECTED_PLAYED`, de witnesstest.
- `013f7fc9e` spec-voorstel: IR-SPEC ch. 5.3 en de index, RUNTIME-SEMANTICS ch. 6.2 en 6.4; ref-test WV-6.4-m.
- `8edd1f683` spec-voorstel: de kosten gemeten.
- de commit van deze Result: twee zinnen van ch. 5.3 nagelezen, de Result.

### Wat er is

**Verifier** (`instance_ordering`). `satellite_hop` geeft de richting mee en eist to-one alleen nog voor de
eigenaarsrol; de stap naar de satelliet mag een verzameling bereiken. `classify` is een loop over het pad met vier
toestanden (onderwerp; na één eigenaarsstap als eerste stap; in het huishouden; na de R-stap): lokaal is nul of meer
huishoudstappen, verschoven is hooguit één eigenaarsstap, één R-stap naar de rol van de voorganger en dan
huishoudstappen. Een pad daarbuiten geeft de clausule van de eerste stap die er niet in past (`clause_of_hop`). De
schakelstap van een deel is de eerste stap van een pad, of de tweede na een stap die er geen is (de eigenaarsstap). Elke
gebruikte satellietrelatie gaat in `sats`, dus in `io_owners` en in de ene-relatie-clausule. `instance_ordering` geeft
naast de delen en de verschoven randen de lezingen buiten de afbakening terug; `wf045_046` maakt er per lezing de arm
van, na de kringen.

**De arm** `WF-045.instance-read-outside` (voorlopig besluit 1 hierboven), aan de vorm die leest, met `form` (de naam
van de vorm), `node` (de knoop van ch. 5.1, bv. `attr:vergunning:verlengde einddag`), `path` (`factType/role`,
gescheiden door `; `) en `clause`. Melding: `read outside the instance-ordered cycle (ch. 5.3): <form> reads <node>
over [<path>], which falls on the clause <clause>`. De clausulesleutels staan in de tabel van IR-SPEC ch. 5.3: `hub`,
`satellite-of-satellite`, `two-switch-steps`, `set-of-predecessors`, `one-walk`, `one-satellite-relation`,
`shifted-or-local`.

**Drools** (`Cycles`). `satelliteHop`, `clauseOfHop`, `walkOf` en `classify` spiegelen de verifier; `orders` geeft een
`Ordering` met de delen en de lezingen buiten. `refusal` weigert een deel met zulke lezingen met de nieuwe code
DROOLS-292 (`instance-read-outside`, CONTRACT, parameters `cycles` en `reads`): de kring (de delen van DROOLS-092) en
elke lezing als `PART.cycle.read-outside`, bv. `exec-model: cycle in the dependencies across the members
Vergunning.blokkade, Vergunning.einddag and Vergunning.openstand via …, and outside the scope of the instance-ordered
cycle: the rule "blokkade" reads Vergunning.openstand over [registratie/register; registratie/geregistreerde
vergunning] (clause hub) — …`. Zonder lezing buiten blijft het DROOLS-092. Beide catalogi; `check-drools-codes.sh`
groen met 292 codes. `TmsCycleVerifierWitnessTest` houdt de genoemde lezing (pad en clausule) tegen de `expect.txt` van
elk `wf045-instance-cycle-*`-negatief en laat de golden toe.

**ref** blijft ongewijzigd in `ref/lib/` (gemeten, `werk/bruggen/household/ref-no-depth.txt`): de golden en
WV-6.4-m spelen met de gedeelde `instance_orders`. Met de dieptevolgorde tijdelijk uit faalt case 2 van de golden
(`T3.overschot = 40` en `V2.restant = 40`, verwacht leeg: de verlengde einddag van V2 werd eerst als basiseinddag
gelezen); met haar PASS. Zonder `io_owners` (een termijn op diepte 0) speelt alles nog: in deze golden beslist de
diepte van de eigenaars, niet die van de satellieten, omdat de leden van een lagere diepte in elke latere fase
opnieuw rekenen (zie vondst 4).

**clingo** blijft ongewijzigd: de golden en WV-6.4-m spelen. Zie vondst 1.

**De golden** `jutterswet-vergunningtermijnen` (handgeschreven IR, met de frontend van de tak geëmitteerd met één
hulpattribuut en teruggezet; `NOTE.md` met de bron en de handberekening). Vijf regels: overschot (Termijn, leest de
verlengde einddag van zijn houder); restant (som over de eigen termijnen, met in het filter de verlengde einddag van de
houder van de termijn); afgewikkeld (kwantor `alle` over de eigen termijnen, de toets leest hetzelfde); verlengde
einddag (basiseinddag plus de som over de termijnen van de vorige vergunning, indien het restant van de vorige gevuld
is en de vorige niet afgewikkeld is); verlengde einddag zonder restant. Drie casussen: één vergunning met één termijn;
een keten van twee met twee en één termijnen (V2 wordt afgewikkeld dankzij de verlenging); een keten van drie met drie,
nul en één termijnen, in een bestandsvolgorde die noch die van de keten noch de alfabetische is. Rood vóór
(`werk/bruggen/household/g3-red.log`: vijf WF-045- en drie WF-046-regels op de golden), groen erna.

**Negatieven** (gate 3: 523 geweigerd op hun regel, 366 op hun arm).
- Weg met reden: `wf045-instance-cycle-to-many-satellite-step`, in de nieuwe sectie "Vervallen bij een normwijziging"
  van `corpus/negative/README.md`, in de rijvorm van de v1-migratie; gemeten schoon op de verifier van de tak.
- Erbij: `wf045-instance-cycle-hub-negation` (de miniatuur; `NOTE.md` met de twee stabiele modellen, opnieuw gemeten met
  de clingo-runner van de tak en `clingo -n 0`; schoon met de rol `geregistreerde vergunning` to-one) en
  `wf045-instance-cycle-satellite-of-satellite` (Vergunning → Termijn → Deeltermijn; schoon met zo'n stap in de verifier
  als huishoudstap geteld).
- De arm per negatief:

| negatief | `expect.txt` | keuze |
|---|---|---|
| `…-to-many-predecessor` | `WF-045.dependency-cycle` | de arm vuurt niet: geen schakelrelatie in het deel (de rol naar de voorganger is to-many) |
| `…-created-relation` | `WF-045.dependency-cycle` | de arm vuurt niet: de relatie heeft een schrijver, dus geen schakelrelatie |
| `…-direct-and-shifted-read` | `WF-045.dependency-cycle` | de arm vuurt niet: elke lezing is verschoven of lokaal, het deel valt op "kringvrij zonder de verschoven randen" |
| `…-shared-satellite` | `instance-read-outside`, `hub`, form `verlengde einddag` | twee lezingen genoemd; de pin is die naast de voorganger, die de NOTE als gedeeld object noemt |
| `…-non-local-read` | `instance-read-outside`, `hub`, form `overschot` | de enige lezing buiten |
| `…-predecessor-and-successor` | `instance-read-outside`, `one-walk`, form `overschot` | beide lezingen genoemd; de pin is de lezing van de opvolger |
| `…-two-satellite-relations` | `instance-read-outside`, `one-satellite-relation`, form `saldo` | beide genoemd; de pin is de lezing naast de voorganger |
| `…-hub-negation` (nieuw) | `instance-read-outside`, `hub`, form `blokkade` | de enige lezing buiten |
| `…-satellite-of-satellite` (nieuw) | `instance-read-outside`, `satellite-of-satellite`, form `verlengde einddag` | beide genoemd; de pin is de lezing naast de voorganger |

**Volgordes**, gemeten met `werk/bruggen/household/scripts/order_run.sh` (naar dat van de vorige bouwer;
`werk/bruggen/household/order.log`): alle 120 volgordes van de vijf regels van de golden, elk met de casussen zoals ze
zijn en met de objecten van elke casus omgekeerd, 240 runs: exec-model en DRL-tekst elk PLAYED 3, ref PASS, clingo PASS,
in alle 240. Geen vorm bleef volgordeafhankelijk. WV-6.4-m speelt in ref, clingo en beide Drools-takken
(`werk/bruggen/household/wv/wv-m-four.txt`).

**Kosten** (`werk/bruggen/household/verifier-{before,after}.txt`): over de 916 documenten van het corpus en de
pariteitszone zijn de randen (`--dependency-edges`) gelijk aan die van main; de rapporten verschillen in zeven
documenten, de golden en de zes negatieven met de nieuwe arm.

### Gates

De volledige set (`werk/gates.sh`), één keer: `gates-1` op `271368ffb` (de tak op main `4970cceb`), **EXIT 0**
(2026-10-22 15:00–15:38), alle zeventien rc=0. Uit `gates-1`: gate 3 365 goldens en rode documenten schoon, 523
negatieven geweigerd op hun regel, 366 op hun arm, 215 arm-sleutels; gate 5 1369 gewerkte voorbeelden (was 1361, plus
WV-6.4-m), 875 casussen exact; gate 6 875 casussen ref ≡ clingo; gate 12 PLAYED 272 op beide takken, de suite 1652
tests groen; gate 13 44 eenheidsannotaties getwist over 25 goldens; gate 15 groen; gate 17 272 goldens en 746
casussen op beide takken, 0 afwijkingen. Na de run veranderden alleen twee zinnen van IR-SPEC ch. 5.3 (de niveaus
binnen een diepte, de groepering van de getuigen van de arm) en deze kaart; gate 1 daarop opnieuw groen.
Tussendoor los op de tak vóór de rebase: gates 3, 5, 6, 12 `--fast` en 17 groen (`werk/bruggen/household/build-gates-2.txt`),
gate 15 groen (`g15-1.log`), gate 3 rood vóór de verifierwijziging (`g3-red.log`).

### Meting 6 (geen bestand in de repository)

Het m5-document van de vorige bouwer (`werk/bruggen/k12/m5/v2-k12.ir.json`, de v2-regelset met de verlenging in de
maxdatum) door de verifier van de tak (`werk/bruggen/household/meting6.txt`): tien `WF-045.dependency-cycle`, 29
WF-046, tien WF-051 en precies één `WF-045.instance-read-outside`:

    WF-045.instance-read-outside	/rules/prioritizedDefaults/4	{"form":"herleving art. 21 lid 1","node":"attr:rechtopuitkering:beëindigingsdatum","path":"SamenloopRechtenOpUitkeringBetreftRechtOpUitkering/samenloop; SamenloopRechtenOpUitkeringBetreftRechtOpUitkering/recht","clause":"hub"}

Dat is de samenlooplezing (lezing 4). De vier andere van meting 5 (de vouwen over de eindigingsgronden in
"herlevingsdatum van het recht", "herleving" en "herlevingstermijn buiten toepassing", en de stap naar het inkomen en
terug in "eerste kalendermaand zonder overschrijding …") zijn lokaal: vóór de wijziging gaf een tijdelijk
geïnstrumenteerde verifier van main er vijf (`werk/bruggen/household/m0-reads.txt`, gelijk aan
`werk/bruggen/k12/m5/diag.txt`), erna één. Het document zonder de K12-wijziging (`v2.ir.json`) blijft verifier-schoon.

### Vondsten

1. **clingo, buiten mijn paden: een vouw met een datumvergelijking in het filter grondt niet onder een ontkende
   kenmerkvoorwaarde.** De bereikbewaking van een vergelijking in het filter (`translate.ml`, de `viol("arithmetic
   range exceeded …")`-regels) neemt de voorwaarde van de regel over, `not has(S,"afgewikkeld")`, zonder `S` te binden:
   `error: unsafe variables … 'S' is unsafe`, grounding stopt. Getuige zonder kring en verifier-schoon:
   `werk/bruggen/household/clingo-guard/hulp-v1.ir.json` (de eerste vorm van de golden, met een hulpattribuut) met
   `corpus/golden/jutterswet-vergunningtermijnen/case1-een-vergunning.json`; ref rekent het. De golden heeft daarom de
   kenmerkvoorwaarde op de verlengde einddag (verschoven, op de voorganger) in plaats van op het restant.
2. **Gate 3 laat een slug van ten hoogste drie woorden toe**, dus heet de arm `WF-045.instance-read-outside` in plaats
   van `instance-cycle-read-outside` (voorlopig besluit 1). De kaart `backlog/new/20261022-check-admits-instance-cycle.md`
   noemt nog de oude naam; die kaart valt buiten mijn paden.
3. **Bestaande test gewijzigd, met reden:** `TmsCycleVerifierWitnessTest.each_negative_document_of_wf045_wf046_and_wf051_is_refused`
   telde 30 negatieven met een gelijkheid; het zijn er nu 31 (één weg, twee erbij, alle geweigerd). Erbij:
   `the_refusal_names_the_read_the_verifier_names` en `the_reads_in_the_household_are_admitted`.
4. **Geen getuige maakt de diepte van een satelliet beslissend.** Met `io_owners` leeg spelen de golden en WV-6.4-m in
   ref nog: een satelliet op diepte 0 rekent opnieuw zodra de diepte van haar eigenaar aan de beurt is. Een vorm waarin
   het verschil zichtbaar wordt, is een kenmerk op een satelliet dat haar eigenaar ontkennend leest; de kaart vroeg er
   niet om.
5. Zonder negatief zijn de clausules `two-switch-steps`, `set-of-predecessors` (met een schakelrelatie in hetzelfde
   deel) en `shifted-or-local`; ze staan in de tabel en in de code van beide uitvoerders.
6. Geen bug gevonden in de verifier of in Drools `Cycles` buiten de kaart.

### Voorgestelde CONSTRUCTS-rij (voor de hoofdsessie)

| 45 | A value of an object that reads the same value of its predecessor — the instance-ordered cycle | statute rules that extend a right by what remains of the earlier one (art. 42b WW: the duration of a new right is extended by the remaining duration of the ended right); in RegelSpraak a rule over a fact type with two roles on one type, "het restant van de vorige vergunning van de Vergunning" | core | kernel: a second controlled exception to WF-045, beside recursive creation | v0 warns on a circular reference at build time and fires up to a cap; until this row v1 refused every such cycle (WF-045, and WF-046 where a leeg-test closes it). **Landed (2026-10-22, card `cycle-across-instances`; voorlopig besluit):** IR-SPEC ch. 5.3 defines the switch relation (two roles on comparable types, the predecessor's role to-one, no writer), the satellite relation, the path of a read, shifted and local reads and the instance-ordered part; WF-045, WF-046 and WF-051 walk the graph without its shifted edges, `--dependency-edges` unchanged. RUNTIME-SEMANTICS ch. 6.2: the members evaluate per object by depth in the chain and per depth by level; a cycle in the links is EF-033 at case input, in ref, clingo and Drools. **Widened (2026-10-22, card `instance-cycle-household-reads`; voorlopig besluit B2.1–B2.3):** a read counts as local or shifted within the household of an object (the object and its satellites, all at its depth): folds and quantifiers over to-many satellites of the object or of its predecessor, and the hop to a satellite and back; the owner's role stays to-one. A part over a switch relation that falls on a clause gets beside its cycle `WF-045.instance-read-outside` (`form`, `node`, `path`, `clause`), and the Drools backend refuses it with the same reads (DROOLS-292); ref and clingo unchanged. Witnesses: goldens `jutterswet-vergunningketen` and `jutterswet-vergunningtermijnen` (gates 3, 5, 6, 12, 17), nine `wf045-instance-cycle-*` negatives (`to-many-satellite-step` removed with reason, `hub-negation` and `satellite-of-satellite` added), red `instance-order-link-cycle`, `ref/` tests WV-6.4-i to -m | out, each with its reason in IR-SPEC ch. 5.3: a read over a hub (a relation whose role on the side of the cycle's type is to-many; refused statically, a miniature gives two stable models), a satellite of a satellite, two switch steps, a set of predecessors; the RegelSpraak frontend still refuses the form (card `check-admits-instance-cycle`) |

### Voor de kaart `check-admits-instance-cycle`

- De afbakening staat op één plek per uitvoerder: `instance_ordering` in `spec/verifier/lib/checks.ml` en `Cycles.orders`
  in de Drools-backend. `Dependencies.java` moet dezelfde set toelaten: de huishoudstap (`satellite_hop`: eigenaarsrol
  to-one, de stap naar de satelliet mag een verzameling bereiken), de loop over het pad met vier toestanden (`classify`),
  de schakelstap op de eerste of de tweede stap van een pad (`walk_of`), en per lezing buiten de clausule
  (`clause_of_hop`, plus `one-walk` voor elke lezing over een schakelrelatie als het deel er meer dan één paar heeft, en
  `one-satellite-relation` per lezing over een gedeeld satelliettype).
- De melding naast de kring: arm `WF-045.instance-read-outside`, parameters `form`, `node` (`attr:<type>:<lid>`
  genormaliseerd), `path` (`factType/role; …`, gespeld zoals in het document) en `clause`, een van de zeven sleutels van
  IR-SPEC ch. 5.3. Drools zegt hetzelfde als DROOLS-292 (`<vorm> leest <Type.lid> over [<pad>] (clausule <sleutel>)`).
- De negatieven `wf045-instance-cycle-*` zijn de toets: negen, zes met de arm in `expect.txt`. Meting 6: het m5-document
  geeft precies één lezing buiten, de samenlooplezing (`hub`). Acceptatie (c) van die kaart (de v2-regelset met de
  verlenging in de maxdatum geeft `check` exit 0) haalt de frontend dus pas na de herschrijving van hww-06 in de
  werkbank (B2.5).
