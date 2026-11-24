# same-conclusion-open-variant: een open variant die alleen hetzelfde kan beslissen, houdt de uitkomst niet open

*Voorstel vanuit de werkbank, 2026-11-24, bij het strikt maken van een regelset buiten deze repository. Vóór de bouw
gelezen door een extern model (xhigh, `werk/neon-review/antwoorden/xhigh-mm-same-conclusion-card.md`); de eigenaar liet
dat model de open vragen beantwoorden en gaf daarna het sein om te bouwen ("begin met bouwen"), met mandaat voor de
beschermde paden die de kaart raakt (ch. 1.5 en 5.12, de gate-scripts, het corpus). De besluiten en eisen staan onderaan.*

## Aanleiding

In een document met `Afleiden uit leeg: nee` leest een beslisvolgorde van hoog naar laag, en bij de eerste variant die
open is, is het doel open (RUNTIME-SEMANTICS ch. 1.5, "A stack"); in een uitzonderingsgroep en een ongeordende groep houdt
een open lid de groep open (ch. 1.5, "Exception groups and a declared-unordered group"; in de Drools-backend de rol
`reconcile-open`). Dat geldt ook als de open variant **dezelfde conclusie** heeft als een lagere variant of een ander lid
dat wel vaststaat. Dan kan de open variant de uitkomst niet veranderen: geldt haar voorwaarde later, dan beslist zij
hetzelfde; geldt zij niet, dan beslist de lagere. De uitkomst staat dus vast, maar de lezing laat haar open.

Schematisch, in de woorden van de Jutterswet (geen golden):

```
Een Vondst is beloonbaar indien zijn gewicht groter is dan 2.
  Uitzondering: een Vondst is niet beloonbaar indien hij betwist is.        (hoger)
  Uitzondering: een Vondst is niet beloonbaar indien hij drijfhout is.      (lager)
```

Bij gewicht 5, drijfhout vast geldend en betwist niet genoemd is beloonbaar nu open; bij elke invulling van betwist
staat het vast niet te gelden. Hetzelfde bij een attribuut met een constante: "het vindloon op 0 indien hij betwist is"
boven "op 0 indien hij beschadigd is", met beschadigd vast geldend en betwist open, geeft nu een open vindloon en bij elke
invulling 0. En in een ongeordende groep van die twee: het lid dat 0 beslist, kan geen conflict krijgen van een open lid
dat alleen 0 kan beslissen.

In de regelset waarin het opviel, bleef zo in 34 situaties de uitkomst open waar zij bij elke invulling dezelfde was: een
open uitsluiting boven een andere uitsluiting met dezelfde conclusie die wel vaststond. De eigenaar van die regelset liet daar als tussenstand de uitsluitingen met
dezelfde conclusie samenvoegen tot één variant met "ten minste één van de volgende voorwaarden", zodat de Kleene-
disjunctie beslist. Dat kost de uitleg haar aparte varianten, elk met hun eigen grondslag. Met deze kaart kan dat weer
uit elkaar.

## Voorstel

1. **Ch. 1.5, "A stack":** een default die open is, houdt het doel niet open als hij alleen kan beslissen wat een lagere
   default beslist die vaststaat, en geen default ertussen anders kan beslissen. "Alleen kan beslissen": zijn beslissing
   is een constante (een waarde zonder lezing, of bij een kenmerk "wel" of "niet") die gelijk is aan die van de lagere.
   Dan beslist het doel die waarde. Hetzelfde voor de initialisatie als laagste default.
2. **Ch. 1.5, "Exception groups and a declared-unordered group":** een lid dat open is, houdt de groep niet open als het
   alleen kan beslissen wat een lid beslist dat vaststaat; een conflict kan het dan niet maken.
3. **Monotonie** (de eis van ch. 1.5 dat wat vaststaat niet verandert als de casus meer geeft): de nieuwe uitkomst is
   die van elke invulling, dus de eis geldt; dat is de getuige die gate en executors moeten dragen, ook voor het kanaal
   van fouten en inconsistenties (een open voorwaarde mag niet alsnog een fout of deponering kunnen geven die de uitkomst
   verandert; meet of dat kan).
4. **De executors:** `ref`, clingo en Drools (in `TmsTranspiler`: de wacht `noOutcome` boven een default, en de
   afstemming met `reconcile-open`), met dezelfde uitkomst; goldens met de drie vormen hierboven (stapel op een kenmerk,
   stapel met een constante, ongeordende groep), met en zonder de open invoer, en een rode casus waar de open variant
   iets anders kan beslissen (een andere constante, of een berekende waarde) en de uitkomst open moet blijven.

## Besluiten voor de eigenaar

- **De beslisser in de uitleg.** De uitkomst staat vast, maar wie beslist niet: bij de ene invulling de open variant, bij
  de andere de lagere. Wat noemt de uitleg (de lagere als beslisser met de open variant als "staat niet vast, verandert
  de uitkomst niet"; of beide; of geen beslisser), en wat staat er in de grondslag (`GROUND`, de Bron-regels)? Wat zegt
  `coverage`/`trace` over de lagere variant?
- **Gelijke berekende waarden.** Mag het ook bij een berekende beslissing waarvan de waarde vaststaat en gelijk is
  (dezelfde uitdrukking, of een andere met dezelfde waarde)? Voorstel: nee, alleen constanten; een berekende waarde
  staat apart, want zij kan zelf open worden.
- **Een open variant met een andere conclusie ertussen.** Voorstel: dan blijft het open, zoals nu.
- **`openInputs`.** Een invoerlid dat alleen zo'n open variant openhield, vraagt de dienst dan niet meer; dat volgt,
  maar zeg het in de toolchain-kaart.

## Daarna

Een toolchain-pin met de uitleg zoals de eigenaar beslist, en in de werkbank een eigen ronde om de samengevoegde
varianten van die regelset weer te scheiden (met gelijke uitkomsten als acceptatie).

## Besluiten (2026-11-24, uit de lezing vóór de bouw, nagerekend door de hoofdsessie)

1. **De lezing heeft vijf uitkomsten** per default, keten en (uitzonderings)groep: `Decided v`, `Conflict`, `Undecided`,
   `Open_only v` (open, en bij elke invulling alleen `Decided v` of `Undecided`, nooit een andere waarde en nooit een
   conflict) en `Open_any`.
   - Een open default is `Open_only v` als zijn beslissing een constante is, anders `Open_any`.
   - Keten (hoger boven lager, ook een uitzonderingsgroep boven haar terugval): `Decided`, `Conflict` en `Open_any` boven
     iets geven zichzelf; `Undecided` boven `b` geeft `b`; `Open_only v` boven `b` geeft `Decided v` als `b = Decided v`,
     `Open_only v` als `b` `Open_only v` of `Undecided` is, en anders `Open_any`.
   - Een groep (uitzonderingsgroepen, een ongeordende groep): de bestaande voorrang van een conflict blijft; twee
     verschillende beslissingen zijn een conflict; is er een `Open_any`, dan `Open_any`; anders, met een `Decided v`:
     `Decided v` als elk open lid `Open_only v` is met dezelfde `v`, anders `Open_any`; zonder beslissing: `Undecided` als
     elk lid `Undecided` is, `Open_only v` als elk open lid `Open_only v` is met dezelfde `v`, anders `Open_any`. Een open
     lid telt met zijn hele keten (tegenvoorbeeld: een lid `[0 indien betwist; 5 altijd]` naast een vaststaand 0 is
     `Open_any`).
   - Alleen aan de wortel van de groep van een **kenmerk** sluit `Open_only (niet)` tot `Undecided` (niet toekennen):
     K3 geeft ¬c ∧ R = onwaar als R onwaar is. In een subgroep of een lid van een ongeordende groep niet: daar verschilt
     `Undecided` van een beslissing.
2. **Alleen constanten:** bij een kenmerk wel of niet, bij een attribuut een `Literal` zonder lezing. Geen parameter en
   geen berekening: die vragen een voorlopige evaluatie van willekeurige uitdrukkingen. Logisch zou een berekende waarde
   die vaststaat ook kunnen (de parallelle als-dan-anders); dat is een bewuste afbakening voor de eenvoud.
3. **Voorlopig lezen onder een open default.** Wat onder een `Open_only`-default staat, leest voorlopig: een deponering
   (§9.5) wordt niet geschreven, een fout maakt het `Open_any`, zoals ch. 1.5 dat doet voor de leden na een onbepaald lid
   van een samengestelde voorwaarde. Anders breekt de monotonie (tegenvoorbeeld: `2 indien betwist` boven `2 indien (10
   gedeeld door 0) leeg is`, dat 2 beslist met EF-005; met betwist waar verdringt de hogere de lagere en verdwijnt
   EF-005).
4. **Een default ertussen** met een andere conclusie houdt de uitkomst open als zij open is of anders beslist; geeft zij
   vaststaand op, dan staat zij niet in de weg.
5. **De lezing van ch. 5.12 en de beslisser:** de open variant leest `OPEN`, de lagere die beslist `HOLDS` (zij is de
   beslisser, met haar grondslag), wat eronder staat `DISPLACED`. De uitleg in de toolchain (de open variant als "staat
   niet vast, maar kan alleen hetzelfde beslissen") en `openInputs` (een invoerlid dat alleen zo'n variant openhield,
   valt weg; let daar op `OpenReads.writers`, die de `OPEN`-lezing niet als schrijver mag tellen) zijn een
   toolchain-kaart na deze.
6. **Afbakening:** `IfThenElse` in een uitdrukking blijft de gewone (sequentiële) als-dan-anders van ch. 1.5;
   `verifier --empty-closures` verandert niet. Een gewone regel met een onvoorwaardelijke initialisatie zonder
   `prioritizedDefaults` op de wire (ch. 7.7/7.8): ref, clingo en Drools lezen haar gelijk; meet vooraf hoe elk haar nu
   leest.

## Acceptatie (aangevuld)

- Ch. 1.5 ("A stack", "Exception groups and a declared-unordered group", "An initialisation" waar nodig) en ch. 5.12 (de
  statussen) beschrijven de lezing van besluit 1–5, met uitgewerkte voorbeelden (`WV-1.5-…`, `WV-5.12-…`) die de tests van
  ref zijn.
- Goldens met handberekening, elk met de open invoer weggelaten, gevuld als waar en als onwaar (of met een waarde), zodat
  gate 5 de monotonie toetst: een kenmerk-stapel (ook de wortelsluiting), een attribuut-stapel met een constante, een
  ongeordende groep, geneste uitzonderingsgroepen; rood (blijft open): een andere constante, een berekende waarde, een
  open default ertussen met een andere conclusie, de keten `[0 indien betwist; 5 altijd]` naast 0, drie leden `{open 0,
  vast 0, open 5}`; groen: een default ertussen die vaststaand opgeeft; een deponering en een fout onder een open
  constante (voorlopig gelezen); een initialisatie-default zonder en met afwijkende invoer.
- ref, clingo en Drools (beide takken) spelen ze gelijk; gate 17 vergelijkt de lezingen; gate 7 en `EXPECTED_PLAYED`
  volgen; `make gates` groen.

## Result stap 1 (spec en ref, met de goldens; 2026-11-24)

Commits (branch `card/same-conclusion-open-variant`, op main `f46796f52`):

- `88e677695`: de kaart geclaimd;
- `09f323a13`: spec ch. 1.5 en 5.12, ref `eval.ml` en `reading.ml`, de uitgewerkte voorbeelden als tests;
- `067fa324d`: zeven goldens, een rode casus, de lezingen van ch. 5.12 op twee goldens als tests;
- dit Result.

**Spec.** Ch. 1.5: in "A stack" beslist een lagere default ook waar elke hogere die open is alleen haar waarde kan
beslissen; een nieuwe alinea **An open default with a constant** met de vijf uitkomsten (beslist v, conflict, geeft op,
open met v, open), de tabel van de keten, een default met uitzonderingsgroepen als twee plaatsen in zijn keten, het
voorlopig lezen onder een default open met v (geen deponering, een fout laat het open), en de theorie: op een kenmerk
Kleene's sterke tabel (de stapel als formule c ∨ R, ¬c ∧ R, bodem onwaar), op een attribuut de parallelle als-dan-anders
van Plotkin (1977), met de constante als bewuste afbakening en `IfThenElse` sequentieel. "The initialisation of a stack"
(ook een voorbeeld met afwijkende invoer), "A stack on a characteristic" (de sluiting aan de wortel, alleen daar),
"Exception groups and a declared-unordered group" (de regel van de groep, een lid telt met zijn hele keten), "An
initialisation" (besluit 6: naast een regel geen stapel), en in "The two requirements" één zinsdeel (ook een default
van een stapel wordt voorlopig gelezen). Ch. 5.12: de rij `DISPLACED`, de alinea **Open values** (onder een default open
met v zijn de lagere aan de beurt, voorlopig, met hun eigen status tot de default die de uitkomst vastlegt; die
eronder `DISPLACED`; een voorlopige fout `OPEN` met de fout aan de knopen), het voorbeeld van case 5 van
`jutterswet-afleiden-uit-leeg` ("10" nu `HOLDS`), en `WV-5.12-same-conclusion` en `-fault`. Buiten de genoemde
hoofdstukken één zin in ch. 7.3 (de initialisatie-default "below a default that is open"), die anders de nieuwe lezing
tegensprak.

Een keuze die de kaart niet noemde, gemaakt bij het schrijven van ch. 5.12: onder een default open met v krijgt een
lagere default die een *andere* waarde beslist ook `HOLDS` (niet meer `DISPLACED`), omdat de sessie haar nu leest; de
lezing volgt wat de sessie evalueert. Dat verandert de lezingen van bestaande goldens (gate 17, hieronder).

**ref.** `eval.ml`: `chain_result` met `Open_only` en `Open_any`; `in_chain` (de tabel), `reconcile` (de groep),
`own_outcome ~provisional` (deponeringen niet doorgegeven, `Value.Fault` → `Open_any`), `constant` (kenmerkbesluit of
`Literal`), aan de wortel `Open_only (Char_dec false)` als `Undecided`. `reading.ml`: dezelfde loop voor de statussen
(`read_on ?faulted`, zodat een voorlopige fout `OPEN` wordt en een binnen een samengestelde voorwaarde opgevangen fout
niet).

**Tests van ref.** Nieuw: `WV-1.5-stack-same-conclusion-*` (waarde, andere constante, berekend, ertussen, voorlopig
met deponering en fout, initialisatie, kenmerk met wortel, ongeordend met keten en drie leden, genest met terugval),
`WV-1.5-initialisation-beside-the-same-value`, `WV-5.12-same-conclusion` en `-fault`; de scenario's lopen mee in
`WV-1.5-monotone` en `WV-1.5-preserved`. Veranderd door de nieuwe lezing (besluit 5 en de keuze hierboven), met reden:
`WV-5.12-afleiden-uit-leeg (betwist open)`: "10" `DISPLACED` → `HOLDS`; `WV-5.12-open-stack (open default)`:
`defaults/2` `DISPLACED` → `HOLDS`; `(two initialisations below an open default)`: `defaults/1` `DISPLACED` → `HOLDS`,
`rules/0` `DISPLACED` → `FAILS`. Elk heeft een open default met een constante boven een lagere die een andere waarde
beslist; de uitkomst blijft open.

**Meting** (`werk/same-conclusion-open-variant/METING.md`). Op een meetdocument met de drie vormen van de kaart en een
regel met initialisatie lazen ref, clingo en Drools (beide takken) vooraf alle vier open. Een gewone regel met een
onvoorwaardelijke initialisatie lezen alle drie natief (clingo past de desugaring van ch. 7.7 in een strikt document
niet toe); dat blijft (besluit 6). Geen golden van het corpus verandert van uitkomst (gate 5 groen); de lezingen van
twee wel: `jutterswet-afleiden-uit-leeg` (case 1–5, de premie "10" `HOLDS`) en `jutterswet-heffing-strikt` (case 2 en
3, de stapelheffing "25" `HOLDS`).

**Goldens** (Jutterswet, handberekening in `NOTE.md`; "open" = `attributeOpen`/`characteristicOpen`):

| golden | vorm | casussen | bron |
|---|---|---|---|
| `jutterswet-zelfde-conclusie-kenmerk` | kenmerk-stapel, wortelsluiting, open met "wel" | 9: betwist open/waar/niet bij gewicht 5 en 1, drijfhout niet, gemeld waar/niet | `source.rgs`, `scenario.rgs` |
| `jutterswet-zelfde-conclusie-waarde` | attribuut-stapel; andere constante; berekend; 7 ertussen | 8: betwist open/waar/niet, beschadigd niet, gemeld open/waar | idem |
| `jutterswet-zelfde-conclusie-ongeordend` | ongeordend {0,0}, {0,5}, {0,0,5} | 6, met twee conflicten | idem |
| `jutterswet-zelfde-conclusie-genest` | uitzonderingsgroepen {0,0}; open 0 boven terugval 0; keten [0 indien betwist; 5] naast 0 | 5, met een conflict `Vondst.bergloon/0` | idem |
| `jutterswet-zelfde-conclusie-deponering` | EF-005 onder een open 2 | 3 (open: 2 zonder feit; waar: 2; niet: 2 met feit) | idem |
| `jutterswet-zelfde-conclusie-fout` | EF-011 onder een open 2 | 2 (open: open, geen fout; waar: 2) | `source.rgs`, casussen met de hand (de scenariolaag eist een peildatum) |
| `jutterswet-zelfde-conclusie-initialisatie` | initialisatie-default onder open 5, zonder en met invoer 7 en 5; regel + initialisatie (besluit 6) | 7 | `ir.json` met de hand (de frontend schrijft een initialisatie-default alleen onder de opgavemarker) |

Rood: `corpus/red/jutterswet-zelfde-conclusie-fout` (betwist niet: de sessie faalt met EF-011). Elke golden heeft
casussen met de open invoer weggelaten en gegeven; gate 5 vergelijkt de runs zonder die invoer (monotonie).

**Getallen** (gate 5; geen aparte vóór-run, oud afgeleid): ref-suite 4232 → 4473 werkvoorbeelden (vloer 489);
gereplayde casussen 1201 → 1241 (vloer 427); goldens met de eigenschap 7 → 14; monotonie 2789 runs zonder één invoer
(vloer 2652), 856 casussen. Geen vloer veranderd. Gate 2: 521 bestanden; gate 3: 521 golden schoon, 646 negatief; gate
4: 1343 casusbestanden; gate 7: 1196 casussen, 45 survivors; gate 10: 233 rgs-goldens; gate 16: 59 scenario-goldens, 209
casusbestanden.

**Gates** (elk apart in de worktree):

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 563 shape self-test row(s); 521 file(s) validate and round-trip, floor 192; 66197 object(s) in 1167 document(s) in the schema's key order, 2 in no subschema)
check-verifier: PASS (…; 521 golden clean; 646 negative rejected for their declared rule, floor 568; …; empty-closures inventory over 521 documents: 839 closure places, floor 823, 428 negation, floor 383, 209 absent-instance, floor 205, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1514 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1343 case file(s) validate)
check-oracle: PASS (ref suite 4473 worked examples, floor 489; 1241 cases replayed exactly, floor 427; preservation with complete knowledge: 281 goldens and 787 cases play, floors 279 and 782; …; 14 carrying the property already, …; monotonicity: 856 cases, 2789 runs without one input compared, floor 2652, 83 of a member not open without its value; 0 listed gap(s) of ch. 1.5; …)
check-noop: PASS (1196 cases refute a do-nothing executor; 45 pinned survivors pin defeasible standing)
check-regelspraak: PASS — JUnit suite green (1716 tests, floor 173); 233 rgs golden(s) re-emit byte-identically (floor 80)
check-scenario: PASS — 59 scenario golden(s) (floor 7) regenerate 209 case file(s) (floor 31) and their ir.json byte-identically
```

Ook gedraaid, voor de rode casus: `check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; …)`,
Drools-poot `PASS (83 fault-pinned, …)`.

Rood, zoals verwacht, en alleen op deze goldens:

- gate 6 (clingo): 14 casussen van `-kenmerk` (5), `-waarde` (3), `-ongeordend` (3), `-genest`, `-deponering`,
  `-initialisatie` (elk 1); `-fout` speelt al;
- gate 12 (Drools): de JUnit-suite, 4 van 2073 tests: `TmsCorpusWitnessTest` en `DrlTextCorpusWitnessTest` (de zes
  goldens hierboven rekenen verkeerd), `RuleOriginCorpusWitnessTest` (`-genest` en `-ongeordend`: de regel "open …" met
  rol `RECONCILE_OPEN` wordt door de test niet aan zijn naam herkend; de eerste goldens in het corpus met een
  verzoening die open kan zijn), `DrlTextPinWitnessTest` (de zeven nieuwe goldens hebben nog geen pin); de replay en
  `EXPECTED_PLAYED` liep daardoor niet;
- gate 13: alleen de clingo-poot, 28 regels op dezelfde zes goldens (presentatie- en naampoot: clingo mist de uitkomst
  al op de onaangeraakte golden);
- gate 17: de lezingen op `jutterswet-afleiden-uit-leeg` (case 1–5) en `jutterswet-heffing-strikt` (case 2, 3), beide
  takken, en op de zeven nieuwe goldens (lezingen, en waar de uitkomst verandert de eindtoestand).

**Herstel stap 1** (na de review `werk/neon-review/antwoorden/xhigh-nn-same-conclusion-s1.md`, § 1, 3 en 4;
2026-11-24). Commits:

- `240d8ba61`: beslissingen van een stapel vergeleken zoals de schrijfstap ze schrijft (ref en ch. 1.5), en ch. 5.12;
- `6a2e11b90`: ch. 5.12, een voorlopig gelezen default die faalt is `OPEN`, ook in de opsomming van de statussen;
- `fea88448a`: de goldens `-kenmerk`, `-genest` en `-fout` uitgebreid (en de rode `ir.json` van `-fout` mee);
- deze bijwerking van het Result.

*Monotonie bij eenheden* (review § 1). `constant` en `equal_dec` in `ref/lib/eval.ml`, en hun spiegel in
`ref/lib/reading.ml`, vergeleken een `Literal` vóór de omrekening naar het doel: een eenheidloze 1 was gelijk aan 1 ct
op een doel in €. Nu: `Eval.as_written` (de waarde zoals `World.collapse` haar schrijft, of geen waarde waar het
schrijven faalt) en `Eval.same_as_written` (gelijk in de eenheid en met de decimalen van het doel; een waarde waarop het
schrijven faalt is niet gelijk aan een die geschreven wordt; twee waarop het faalt worden vergeleken zoals berekend,
zodat in een gewoon document een conflict vóór de schrijffout blijft gaan en twee gelijke waarden beslissen en bij het
schrijven falen). Een `Literal` die de schrijfstap niet kan schrijven (andere eenheid, schrijfgrens) is geen constante: de default
is open. Ch. 1.5 ("An open default with a constant") zegt het, met het voorbeeld `WV-1.5-stack-same-conclusion-unit-*`.
De rode test (`ref/test/test_ref.ml`, die rijen, het vindloon in € met twee decimalen) vóór de fix:

```
FAIL WV-1.5-stack-same-conclusion-unit-cent (got 0.01, want open)
FAIL WV-1.5-stack-same-conclusion-unit-hundred-cent (got open, want 1)
FAIL WV-1.5-stack-same-conclusion-unit-another-unit (got 1, want open)
FAIL WV-1.5-stack-same-conclusion-unit-write-boundary (fault: V1.vindloon: value 1/200 does not fit the declared 2 decimals — an explicit rounding is mandatory (ch. 2.4))
FAIL WV-1.5-monotone WV-1.5-stack-same-conclusion-unit-cent-betwist-holds without V1 betwist (adds V1.vindloon 0.01)
5 failure(s)
```

en erna `test_ref: all 4500 worked examples pass` (4473 → 4500). Geen golden verandert erdoor van uitkomst (gate 5).

*Spec* (review § 3). Ch. 5.12: de status van een voorlopig gelezen default naar voorwaarde en waarde (`HOLDS` waar de
rechtvaardiging geldt en de waarde vaststaat, ook een vaststaand lege waarde waarop hij opgeeft; `FAILS` waar zij niet
geldt; `OPEN` waar zij onbepaald is, de waarde open of de evaluatie faalt). `WV-5.12-same-conclusion-fault` noemt nu
het rekenloon, als de golden.

*Goldens* (review § 4), met handberekening in `NOTE.md`, gelezen door ref:

- `jutterswet-zelfde-conclusie-kenmerk`, twee kenmerken erbij, elk in de negen casussen: `bergbaar`, een groep-default
  met de uitzonderingsgroepen "niet bergbaar indien hij betwist is" en "… drijfhout is" boven de basis "bergbaar indien
  zijn gewicht groter is dan 2"; `keurbaar`, een ongeordende groep van "keurbaar indien zijn gewicht groter is dan 2" en
  "niet keurbaar indien hij betwist is". Case4 (bergbaar) en case1, case4 (keurbaar): open — een groep of lid open met
  niet toekennen sluit niet, alleen de wortel; zou het sluiten, dan hield het kenmerk. Case5 (keurbaar): de groep is
  open met niet toekennen, en aan de wortel staat keurbaar niet. Case2: het conflict `Vondst.keurbaar`;
- `jutterswet-zelfde-conclusie-genest`, het havenloon: "op 0 indien hij betwist is" boven "op 7 indien hij beschadigd
  is" met de uitzonderingsgroepen "op 0 indien zijn gewicht groter is dan 2" en "op 5 indien … groter is dan 10", boven
  "zijn gewicht maal 2". Case1 (beschadigd, betwist open): open, geen §9.5-feit (het geneste conflict onder de open 0
  wordt voorlopig gelezen); case2: 0; case3: leeg met `Vondst.havenloon/1`; case4: open; case5: 100;
- `jutterswet-zelfde-conclusie-fout`, het werkloon: "op 2 indien hij betwist is" boven "het Rekenjaar min 2023", zonder
  peildatum: een fout in de waarde van de lagere default. Case1 (betwist open): open, geen fout; case2: 2; de rode
  casus (betwist niet) faalt met EF-011 als voorheen.

Nieuwe of veranderde verwachtingen: `-kenmerk` 9 casussen (elk twee kenmerken erbij, case2 ook een inconsistentie),
`-genest` 5 (het havenloon, case3 ook een tweede inconsistentie), `-fout` 2 (het werkloon). Geen nieuwe casusbestanden.

Gates in de worktree, elk apart, na elkaar:

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 563 shape self-test row(s); 521 file(s) validate and round-trip, floor 192; 66376 object(s) in 1167 document(s) in the schema's key order, 2 in no subschema)
check-verifier: PASS (…; 521 golden clean; 646 negative rejected for their declared rule, floor 568; …; empty-closures inventory over 521 documents: 840 closure places, floor 823, 435 negation, floor 383, 209 absent-instance, floor 205, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1522 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1343 case file(s) validate)
check-oracle: PASS (ref suite 4500 worked examples, floor 489; 1241 cases replayed exactly, floor 427; preservation with complete knowledge: 281 goldens and 787 cases play, floors 279 and 782; …; 14 carrying the property already, …; monotonicity: 856 cases, 2789 runs without one input compared, floor 2652, 83 of a member not open without its value; 0 listed gap(s) of ch. 1.5; …)
check-noop: PASS (1196 cases refute a do-nothing executor; 45 pinned survivors pin defeasible standing)
check-regelspraak: PASS — JUnit suite green (1716 tests, floor 173); 233 rgs golden(s) re-emit byte-identically (floor 80)
check-scenario: PASS — 59 scenario golden(s) (floor 7) regenerate 209 case file(s) (floor 31) and their ir.json byte-identically
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; …); Drools leg PASS (83 fault-pinned, …)
```

Rood, alleen op de goldens van deze kaart en de twee met veranderde lezingen (logs in
`werk/same-conclusion-open-variant/logs/herstel/`):

- gate 6 (clingo): 14 casussen, `-kenmerk` case1–5, `-waarde` (3), `-ongeordend` (3), `-genest`, `-deponering`,
  `-initialisatie` (elk 1); `-fout` speelt, ook het werkloon; in `-kenmerk` zijn bergbaar (case1) en keurbaar (case5)
  bij de mismatches;
- gate 12 (Drools): `Tests run: 2073, Failures: 4`: `TmsCorpusWitnessTest` en `DrlTextCorpusWitnessTest` (dezelfde zes
  goldens), `RuleOriginCorpusWitnessTest` (`-genest`, `-ongeordend`, en nu ook `-kenmerk`: de verzoening van keurbaar
  heeft de rol `RECONCILE_OPEN`), `DrlTextPinWitnessTest` (zeven goldens zonder pin);
- gate 13: 28 regels, alleen de clingo-poot (presentatie- en naampoot), op dezelfde zes goldens;
- gate 17: 74 regels, `jutterswet-afleiden-uit-leeg` case1–5 en `jutterswet-heffing-strikt` case2, 3 op beide takken,
  en de zeven goldens van deze kaart.

**Stap 2, clingo** (`backends/clingo/lib/translate.ml`, strikte tak):

- `translate_chain` (rond 14166–14398): naast `out`/`gw`/`st` een atoom "open met v" per default en per keten; een
  default is open met v waar hij bereikt is, zijn rechtvaardiging niet vaststaat en zijn besluit een constante is
  (kenmerkbesluit, `Literal`); bereikbaarheid onder een hogere "open met v" (de tabel van ch. 1.5): de lagere wordt
  gelezen, beslist v → de keten beslist v, geeft op of is open met v → door, anders open;
- voorlopig lezen: wat alleen onder zo'n default bereikt wordt, deponeert niets (`deposit_guard`, `inconsistent/2`) en
  een fout (`viol/2`) maakt de keten open in plaats van de sessie te laten falen;
- `translate_group` en `stands_beside` (rond 14400–14488): de regel van de groep (een lid open met v naast een lid dat v
  beslist laat de groep v beslissen; geen beslissing en alle open leden open met v → de groep open met v);
- `translate_defaults_group` (rond 14757–14907): aan de wortel van een kenmerk sluit "open met niet" tot `nil`; op een
  attribuut blijft "open met v" open (ook met invoer v); `val`/`has` waar de groep beslist;
- de regel naast een initialisatie blijft natief (geen desugaring in een strikt document).

Uit review § 5 (`werk/neon-review/antwoorden/xhigh-nn-same-conclusion-s1.md`), nog niet hierboven:

- `stack_atoms` (rond 14159): de waarde van "open met v" (`s_oo(S,V)`) door dezelfde omrekening naar eenheid en schaal
  van het doel als `s_out` (`fitted`, rond 14312–14318), zodat "open met v" en een beslissing in de eenheid van het doel
  vergeleken worden (zoals ref sinds de herstelronde, ch. 1.5); een `Literal` die de schrijfgrens of de eenheid van het
  doel niet haalt, geeft geen `s_oo` (de default is open);
- bereikbaarheid in twee soorten: echt (`rch`, alle hogere geven op) en voorlopig (onder een hogere "open met v"),
  allebei meegegeven aan geneste uitzonderingsgroepen (`translate_group`); `ctx.rule_guard` en `ctx.param_reach` op de
  vereniging, `ctx.member_gate` en `ctx.deposit_guard` op de echte, zodat `emit_viol` (rond 1310–1314) een EF-fout onder
  voorlopige bereikbaarheid onderdrukt en open maakt, terwijl een limiet via `ctx.limit_gate` zichtbaar blijft;
- de deponering van EF-027 in `translate_group` (rond 14485) staat buiten `ctx.boundary`: zij moet expliciet op de
  echte bereikbaarheid van de groep wachten, anders deponeert een conflict van uitzonderingsgroepen onder een hogere
  "open met v" toch (golden `-genest`, havenloon case1: geen feit);
- een default met uitzonderingsgroepen en een eigen inhoud (rond 14350–14372): de keten-tabel ook tussen de verzoende
  groepen en de terugval (de terugval voorlopig bereikt waar de groepen open met v zijn);
- `stands_beside` (rond 14164): `m.s_st(S)` vervangen door een hulpatoom "staat of is open met v";
- aan de wortel van een kenmerk leidt alleen de groep "open met niet" `nil` af, niet één lid naast een ander open lid;
  in een uitzonderingsgroep en een lid van een ongeordende groep sluit niets (golden `-kenmerk`, bergbaar en keurbaar);
  op een attribuut leidt "open met v" geen `nil` af, zodat ook `inputval` + `nil` (rond 14904–14906) open blijft.

**Stap 3, Drools** (`TmsTranspiler`, `Outcome`, `SessionReaders`, beide renderings):

- `simpleForm` (rond 4135): een open default met een constante verdringt de lagere niet; òf zulke stapels naar
  `generalForm`, òf `simpleForm` krijgt de tabel;
- `generalForm`/`chainRules` (rond 4170) en `Outcome.Kind`: een uitkomst "open met v"; `noOutcome` laat een hogere
  "open met v" door; de keten-tabel; onder zo'n uitkomst geen deponering (`depositRule`, ook die van een
  rechtvaardiging) en een fout (`conditionFaultRule`, een fout in de consequentie) als open;
- `reconciliation` (rond 4251): `RECONCILE_AGREEMENT` beslist ook naast leden open met dezelfde v; `RECONCILE_OPEN`
  alleen bij een open lid of een lid open met een andere v; een groep zonder beslissing met alle open leden open met v
  is open met v; aan de wortel van een kenmerk sluit "open met niet";
- de lezingen (`SessionReaders` rond 532–619, `readingRule`): onder een default open met v de lagere aan de beurt met
  hun eigen status (`HOLDS`, `FAILS`, `OPEN`, een voorlopige fout `OPEN`), `DISPLACED` onder de default die de uitkomst
  vastlegt; dat verandert ook de lezingen van `jutterswet-afleiden-uit-leeg` en `jutterswet-heffing-strikt`;
- `RuleOriginCorpusWitnessTest`: de rol `RECONCILE_OPEN` bij de naam "open …"; DRL-pins voor de zeven nieuwe goldens
  (`-Duchar.drlpin`); `EXPECTED_PLAYED` in `scripts/check-drools.sh` +7.

Uit review § 5, nog niet hierboven:

- de keuze van de vorm (rond 3978): elke strikte stapel met een default die open met v kan zijn naar `generalForm`,
  ook een vlakke kenmerk-stapel met een "niet"-default (`simpleForm` zet bij een open default direct `LeftOpen`, rond
  4153); de DRL-pins van `jutterswet-afleiden-uit-leeg` en `jutterswet-heffing-strikt` en de vloeren van
  `RuleOriginCorpusWitnessTest` schuiven dan mee;
- de uitkomst per positie tegenover die van de keten: laat `noOutcome` een hogere "open met v" door, dan staan er meer
  `Outcome`-feiten in één keten (positie 0 open met 0, positie 1 beslist 100); `reconciliation` (rond 4257–4334) en
  `SessionReaders.settledInGeneralForm` (rond 557–570, dat nu een `IllegalStateException` gooit bij twee uitkomsten
  per keten) mogen de lagere beslissing niet als die van de keten lezen; `Outcome` (rond 42–51), `Consequence` (rond
  789), `StaticFunction` (de vergelijking met "open met v", in de eenheid en met de decimalen van het doel) en beide
  renderings (`PatternDslRendering`, `DrlRendering`) daarop aanpassen;
- voorlopig lezen onder een hogere "open met v" in de eigen keten of een voorouderketen, op alle plekken: `depositRule`
  (rechtvaardiging), `Consequence.Deposit` (consequentie, rond 4479), `conditionFaultRule` (een fout als open, geen
  `Fault`), `ComputeFault` in `Consequence.Decision` (idem; golden `-fout`, werkloon), en de fouten en deponeringen van
  een vouw in een voorlopige default (`FOLD_FILTER_FAULT`, `FOLD_MEMBER_FAULT`, `FOLD_FILTER_DEPOSIT`); een conflict
  van uitzonderingsgroepen onder zo'n default deponeert geen EF-027 (golden `-genest`, havenloon);
- `SessionReaders.reading(...)` (rond 1154–1164) geeft nu bij een fout in de voorwaarde `FAULTS` en bij een fout in de
  waarde `HOLDS`; een voorlopig gelezen default moet daar `OPEN` krijgen, met de fout aan de knopen
  (`WV-5.12-same-conclusion-fault`, en de werkloon van `-fout`);
- aan de wortel van een kenmerk mag "open met niet" geen `LeftOpen` invoegen, anders zet `openValues` (rond 857–861)
  het kenmerk alsnog open;
- `EXPECTED_PLAYED` in `scripts/check-drools.sh` (rond 1667): 389 → 396.

**Open punten.**

- De zin in ch. 7.3 viel buiten de genoemde hoofdstukken van het mandaat; zonder haar spreekt de spec zichzelf tegen.
- Op een attribuut blijft een stapel open met v open, ook als de casus het doel v geeft (besluit 1 letterlijk: alleen
  de wortel van een kenmerk sluit); de initialisatie-golden pint het (case 7).
- Voorstel voor `spec/CONSTRUCTS.md` rij 93 (`inferFromEmpty`), bij de landing: "**Landed (2026-11-24, card
  `same-conclusion-open-variant`):** a stack reads an open default whose decision is a constant as open with that value
  (RUNTIME-SEMANTICS ch. 1.5, 'An open default with a constant'): Kleene's table on a characteristic, the parallel
  choice on an attribute; what stands below it is read provisionally. Goldens `jutterswet-zelfde-conclusie-*`."
- Toolchain-kaart na deze (besluit 5): de uitleg van de open variant ("staat niet vast, maar kan alleen hetzelfde
  beslissen") en `openInputs` (`OpenReads.writers` mag de `OPEN`-lezing van zo'n variant niet als schrijver tellen);
  de `Verbaliser` mag een lagere default die `HOLDS` leest onder een `OPEN` default, waar de uitkomst open blijft, niet
  als de beslisser tonen (review § 2).
