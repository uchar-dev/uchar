# open-values-in-ref: ref rekent een document dat uit leeg niets afleidt

*Opgesteld door de hoofdsessie op 2026-11-21, na het besluit van de eigenaar van die dag voor de lezing met drie
standen. Het ontwerp per vorm, met uitgewerkte voorbeelden en de metingen, staat in `spec/proposals/STRICT-EMPTY.md`;
de schakelaar staat op `main` (`inferFromEmpty: false` op de wortel, `Afleiden uit leeg: nee` in de bron, en ref,
clingo en Drools weigeren zo'n document met REF-014, CLINGO-049 en DROOLS-370).*

## Aanleiding

Een regelset kan kiezen dat uit een lege waarde niets volgt. De eigenaar koos de lezing waarin een waarde gevuld,
vaststaand leeg of open is:

- **open**: het staat niet vast. Een invoergegeven dat de case niet geeft en dat de regelset niet als volledig
  behandelt; en elk lid dat een vorm zou schrijven of toekennen terwijl haar voorwaarde onbepaald is of haar waarde open.
- **vaststaand leeg**: er staat vast dat er geen waarde is. Een afgeleid attribuut waarvoor geen schrijver geldt, een
  lezing over een link die er niet is, een opgave die niet gegeven is.
- Wat vaststaat, rekent zoals RUNTIME-SEMANTICS ch. 1.2–1.4 het nu vastlegt. Wat een open waarde leest, is zelf open;
  een voorwaarde die een open waarde leest is onbepaald, tenzij een ander deel haar beslist (de sterke tabel van
  Kleene).

Een voorbeeld uit de Jutterswet: "Een vondst is kostbaar indien zijn taxatie groter is dan 1000", "Het vindloon van een
vondst is 10 indien hij kostbaar is", "De uitbetaling is zijn vindloon plus zijn bergloon". Taxatie 50: niet kostbaar,
het vindloon is vaststaand leeg, de uitbetaling is het bergloon. Taxatie leeg: kostbaar is onbepaald, vindloon en
uitbetaling zijn open, en een regel "indien de vondst niet kostbaar is" vuurt niet.

De lezing is gehouden aan twee eisen: **monotonie** (een gegeven invullen kan een uitkomst toevoegen, maar nooit een
uitkomst veranderen die er stond; de sluitingen die een auteur schrijft zijn de benoemde uitzondering) en **behoud bij
volledige kennis** (is niets open, dan is de uitkomst die van de gewone lezing).

## Goal

De referentie-interpreter rekent een document met `inferFromEmpty: false` volgens deze lezing, RUNTIME-SEMANTICS
beschrijft haar met uitgewerkte voorbeelden die de tests van ref zijn, het case-formaat kan "staat vast dat niet" en
"open" zeggen, en strikte goldens met handberekende verwachtingen spelen in ref. clingo en Drools weigeren die goldens
bij naam tot hun eigen kaart.

## De architectuur (bindend)

**Eén lezing, één evaluator.** De gewone lezing is het geval waarin niets open is: behoud bij volledige kennis is hier
geen toets achteraf maar de opbouw. `inferFromEmpty: false` verandert alleen welke afwezige invoer open is. Daaruit
volgt:

- Geen tweede evaluator, geen kopie van `eval_expr` of `eval_cond`, geen strikte varianten van operatoren of vouwen. Een
  uitdrukking waarvan alle lezingen vaststaan, wordt door de bestaande code gerekend.
- `Uchar_ref.Value.t` krijgt geen constructor erbij (clingo leest dat type).
- Een document zonder de eigenschap geeft byte voor byte dezelfde uitkomsten, lezingen en eindstand als nu: gate 5, 7,
  8, 13 en 17 blijven groen zonder dat een verwachting, pin of lijst verandert.
- De eigenschap wordt op één plaats gelezen (welke afwezige invoer is open); nergens anders staat een tak op de
  eigenschap.

**Aanbevolen opzet** (afwijken mag, met de reden in het Result, zolang het bovenstaande blijft staan):

1. *Open als uitkomst van een vorm.* De wereld houdt per vorm en onderwerp al een uitkomst bij (de deponeringen, of de
   fout). Daar komt bij welke doelen die evaluatie open liet. Een lid is open als de case het niet geeft en het open
   invoer is, of als een uitkomst die staat het open liet. Een lezing zonder waarde en zonder merkteken is vaststaand
   leeg.
2. *Een open lezing breekt de evaluatie af.* Een lezing van een open lid werpt een eigen exceptie, die wordt gevangen
   op de grens van wat samen één oordeel of één waarde is: een atomaire voorwaarde (dan onbepaald), de waarde van een
   vorm (dan is het doel open), de voorwaarde van een `IfThenElse`, het filter of de waarde van een lid van een vouw.
   Wat een afgebroken evaluatie tot dan toe deponeerde, vervalt. Zo raakt de lezing geen operator.
3. *Het oordeel van een voorwaarde is een paar*: wat voor de vorm zelf vaststaat, en wat voor een andere vorm vaststaat.
   Ze verschillen op één plaats: een toets die zegt dat een open X leeg is (direct, of als de ontkenning van "is
   gevuld") is voor de vorm zelf waar (de sluiting die de auteur schreef) en voor een ander onbepaald. De samengestelde
   voorwaarde leest haar leden van links naar rechts tot haar oordeel vaststaat.
4. *Een vorm* beslist als haar voorwaarde voor haarzelf waar is en haar waarde zonder afbreken rekent. Beslist zij niet,
   dan is haar doel open, tenzij haar voorwaarde voor een ander onwaar is (dan staat vast dat zij niets vult). "Staat
   vast dat niet" van een kenmerk en "vaststaand leeg" van een attribuut zijn dus: geen waarde en geen merkteken, nadat
   het stratum van het lid af is.

Bestudeer vóór de bouw hoe ref een lezing behandelt van een lid dat later in hetzelfde stratum geschreven wordt
(RUNTIME-SEMANTICS ch. 5 en 6, de schrijfacties aan het fixpunt): een merkteken "open" moet voor die regels gelden als
een schrijfactie op het doel.

## Bouwdelen

Elk deel een verse bouwer; elk deel eindigt groen en met een overdracht.

1. **De kern** (deel 1a): de tekst in RUNTIME-SEMANTICS voor de standen, de open lezing, het oordeel van een
   voorwaarde, de toets op leeg en gevuld, de regel, het kenmerk en de consistentieregel, met uitgewerkte voorbeelden;
   ref rekent die vormen; elk ander soort vorm in een strikt document weigert ref met REF-014 en de vorm als parameter.
2. **Het case-formaat, de golden en de gates** (deel 1b): de velden voor "staat vast dat niet" en "open", de cases van
   `jutterswet-afleiden-uit-leeg`, de kanaallijsten van clingo en Drools, de poot voor behoud bij volledige kennis.
3. **De stapel, de vouwen en de creaties** (deel 2): defaults met doorval, initialisatie en opgave, de vouwen en
   kwantorvouwen over een verzameling, object- en feitcreatie; de getuigen van de proef als goldens.
4. **Lezingen, eindstand, wat nog geweigerd wordt** (deel 3): ch. 5.12 en 5.12a voor een onbepaalde voorwaarde en een
   open lid, de WF-regel voor vormen zonder gedefinieerde lezing, de poot voor monotonie, het Result.

## Acceptance (deel 1a; de volgende delen krijgen de hunne in hun opdracht)

Rood eerst per voorbeeld.

- **RUNTIME-SEMANTICS** krijgt een paragraaf in ch. 1 (voorstel: 1.5) die de lezing beschrijft in de termen van de
  executie, niet van een omzetting: de drie standen en waar "open" vandaan komt; dat een document zonder de eigenschap
  geen open waarde kent; een evaluatie die een open waarde leest; het oordeel van een voorwaarde met de tabellen en de
  leesvolgorde; de toets op leeg en gevuld met haar sluiting; wanneer een regel beslist, vaststaand niets vult, of haar
  doel open laat; de drie standen van een kenmerk; de consistentieregel; de twee eisen. Elke zin die iets vastlegt
  heeft een uitgewerkt voorbeeld uit de Jutterswet met de verwachte uitkomst, en elk voorbeeld is een test in
  `ref/test/` met zijn naam (`WV-1.5-…`). Huidige stand, geen geschiedenis, geen status van een executor, geen naam van
  de proef of haar gegenereerde kenmerken. Als voorstel; de hoofdsessie redigeert bij de landing.
- **ref** rekent in een strikt document: de regel (gelijkstelling, berekening, kenmerktoekenning, initialisatieregel),
  elke voorwaarde zonder vouw (vergelijking, toets op leeg en gevuld ook van een berekende waarde, cijfertoets,
  kenmerkcheck ook genavigeerd, rolcheck, identiteitscheck, toets op een marker, de samengestelde voorwaarde met haar
  vijf kwantoren), elke uitdrukking zonder vouw of tijdlijn (door de opzet zonder wijziging), de lezing over een pad met
  een afwezige link, `Let`, en de consistentieregel. De getuigen van de proef binnen dat bereik zijn de voorbeelden
  (`arithmetic`, `comparison`, `filled-test`, `compound-all`, `compound-any`, `compound-none`, `compound-counting`,
  `characteristic`, `marker`, `absent-link`, `absent-link-path`, `navigated-read`, `consistency`, `exact-digits`,
  `settled-empty`, `settled-empty-date`, `let`, `closed-input`, en de delen zonder stapel of vouw van
  `filled-test-not-closure`, `composite-filled-test` en `empty-by-fault`), met hun handberekende uitkomsten uit het
  voorstel; de gewone documenten staan onder `spec/verifier/test/strict-empty/` op de branch
  `card/strict-empty-lowering` (worktree `/Users/yeb/Developer/uchar-dev/wt/strict-empty-lowering`, alleen lezen). Hun
  cases noemen gegenereerde namen van de proef (`K (niet)`, `A (leeg)`); vertaal die naar de standen: `K (niet)` is
  "staat vast dat niet", geen van beide is "open".
- **Gesloten invoer** is in dit deel een instelling van de sessie voor de tests (een lijst leden, en "alles"): er is
  nog geen schrijfwijze in de bron. Een opgave is gesloten door haar declaratie.
- **Wat ref nog niet strikt rekent, weigert hij bij naam**: een strikt document met een defaults-groep, een vouw of
  kwantorvouw over een verzameling, een creatie, een tijdlijnvorm, een verdeling, een dagsoort, een uniciteit, een cel
  van een gedimensioneerd attribuut, of een vorm van ch. 9–11 buiten de marker, geeft REF-014 met de vorm als parameter
  (`form`), vóór er iets gerekend wordt. Eén functie somt ze op.
- **Een gewoon document verandert niet**: de suite van ref en gate 5, 7, 8, 13 en 17 groen, zonder gewijzigde
  verwachting.
- **Behoud bij volledige kennis, gemeten** met een script in de werkmap (de gate-poot komt in deel 1b): elke golden van
  het corpus waarvan ref de vormen strikt draagt, met de eigenschap erbij gezet en alle invoer gesloten, speelt haar
  eigen verwachtingen. Rapporteer goldens, cases, gelijk, niet gelijk met de oorzaak; elke afwijking is een fout.
- **De naad, gemeten**: in het Result per bestand van `ref/lib` de toegevoegde en gewijzigde regels en de functies die
  geraakt zijn, en of de vier punten van "De architectuur" gehouden zijn.
- Los groen: gate 1, 3, 5, 6, 7, 8, 13, 17 (clingo en de suites van de OCaml-frontends bouwen mee met de bibliotheek
  van ref). `make gates` draait de hoofdsessie.

## May touch

- `ref/` (bibliotheek, runner, tests, fixtures, `codes.tsv`);
- `spec/RUNTIME-SEMANTICS.md` (de paragraaf, als voorstel) en in `spec/IR-SPEC.md` alleen de verwijzing van de paragraaf
  over `inferFromEmpty` naar de nieuwe tekst;
- wat in `backends/clingo/`, `frontends/catala/` of `frontends/flogic/` moet meebewegen omdat een signatuur van de
  bibliotheek van ref verandert (meld elke plek);
- een gate-script alleen waar een floor of lijst het vraagt, in een eigen commit;
- deze kaart.

Mandaat voor de beschermde paden: de opdracht van de eigenaar van 2026-11-21 en zijn keuze voor deze lezing. De
hoofdsessie leest na en landt.

## Out of scope (deel 1a)

Het case-schema en de casebestanden, de golden, de kanaallijsten (deel 1b); de stapel, de vouwen, de creaties (deel 2);
lezingen en eindstand (deel 3); `backends/drools/`, de vertaling van clingo, de frontend van RegelSpraak, de verifier
buiten wat een signatuur vraagt, `spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor), de toolchain, tijdlijnen,
de schrijfwijze van sluiten per gegeven.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de lezing met drie standen; de sterke tabel van Kleene, ook na een onbepaald lid; een
initialisatie is een sluiting over haar stapel; de wire draagt het document van de auteur en elke executor draagt de
lezing zelf; voorbeelden uit de Jutterswet.

Van de hoofdsessie (voorlopig, ter bevestiging): een lezing over een afwezige link staat vast; "X is leeg" over een
open X is alleen voor de vorm die het schreef een sluiting; een opgave is gesloten door haar declaratie; een
invoerkenmerk dat de case niet noemt is open; een afgeleid lid dat de case geen waarde geeft volgt zijn schrijvers; een
evaluatie die een open waarde leest geeft geen waarde, geen oordeel en geen deponering; de invoer naast een schrijver
die niet beslist blijft staan (ch. 7.8).

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.

## Result (deel 1a)

*Bouwer deel 1a, 2026-11-21. Branch `card/open-values-in-ref`, op `cdc4addc1`.*

### Wat er is

| commit | wat |
|---|---|
| `5814d89d1` | ref: de kern (World: drie standen, uitkomst die een lid open laat; Eval: open lezing, oordeel als paar, regel, consistentie, open invoer, weigering per vorm; runner `--closed-inputs`), de tests per principe (WV-1.5-*), de aangepaste test WV-2-infer-from-empty |
| `d788c07d6` | RUNTIME-SEMANTICS ch. 1.5 (voorstel) en de verwijzing ernaar in IR-SPEC ch. 2 (`inferFromEmpty`) |
| `52a76c937` | negentien getuigen van de proef als fixtures (`ref/test/fixtures/strict-empty/`, WV-1.5-witness-*), de toets op leeg en gevuld over een berekende waarde |
| `9db3d29e6` | de grens `held` laat een andere exceptie dan een open lezing door met haar deposito's (WV-1.5-boundary-keeps-a-barrier); de rolcheck in WV-1.5-link-* en de zin erover in ch. 1.5 |
| (slotcommit) | dit Result |

Gewijzigd gedrag van een bestaande test (de taak verandert het): WV-2-infer-from-empty verwachtte dat ref het fixture
`ref/test/fixtures/infer-from-empty` bij elke ingang weigert (REF-014, `form` `document`). ref draagt dat document nu: de
replay speelt de case (V3 zonder vondstwaarde: meldplichtig niet toegekend, beide open), `--trace` weigert met REF-014
`form` `reading`. De fixture en zijn case zijn ongewijzigd; clingo weigert hem nog met CLINGO-049 (gate 6).

### Principes, zinnen en tests

Rood eerst: de testfile van `52a76c937` gedraaid op de code van `cdc4addc1` met stubs voor de nieuwe API die "niets is
open" zeggen (`World.is_open` altijd false, `Eval.closed` zonder werking, `Eval.refuse_uncarried` = de oude weigering van
het hele document), in een losse worktree (`git worktree add --detach`): 241 FAIL
(`logs/red-suite-02.log` in de werkmap). De rolcheck en WV-1.5-boundary-keeps-a-barrier kwamen daarna: beide groen op
`cdc4addc1` (de rolcheck staat vast, de barrière is de gewone lezing). "nee" hieronder: de test toetst juist dat wat vaststaat rekent zoals ch. 1.2;
elk principe heeft er een rode naast zich.

| principe | zin in ch. 1.5 | test(s) | rood eerst |
|---|---|---|---|
| P1 standen | "The three states" | WV-1.5-open-input (vindloon, drijfhout), -no-open-without-the-property, -input-per-object | ja (open-input 2, input-per-object 1); no-open-without-the-property nee (de gewone lezing) |
| P2 wat vaststaat rekent als nu | "What stands reads as before" | WV-1.5-settled-operand (plus, ≠, en het contrast zonder taxatie) | ja (het contrast); plus en ≠ nee |
| P3 open plant zich voort | "An open read" | WV-1.5-open-passes-on (plus; deling door een open deler zonder EF-031; zonder de eigenschap wel EF-031) | ja (3) |
| P4 oordeel van een voorwaarde | "The judgement of a condition", met de tabel | WV-1.5-atom; WV-1.5-table-and/-or/-none (elk 9 rijen), -not (3), -at-least-2 (5), -at-most-1 (5), -exactly-1 (6) | ja (atom 1; table 15 rijen rood, elk deel ≥ 1) |
| P5 de regel | "For the form itself and for another", "A rule", "An initialisation" | WV-1.5-rule-decides/-fills-nothing/-open-condition/-open-value; WV-1.5-initialisation (4); WV-1.5-consistency (2) | ja (rule-open-condition, rule-open-value, initialisation 1, consistency 1); decides en fills-nothing nee |
| P6 de afgeleide stand | "A derived member" | WV-1.5-derived-attribute (3), -derived-characteristic (3), -derived-chain (5, met beide volgordes en een lezer die niets schrijft) | ja (6) |
| P7 de ontkenning | "A negated characteristic check" | WV-1.5-negation (5) | ja (2) |
| P8 leeg en gevuld | "The test on leeg and gevuld", met de tabel | WV-1.5-filled-test-two-valued, -open, -closure, -computed (8), -consistency; WV-1.5-filled-guard (4) | ja (open 1, computed 3, filled-guard 2); two-valued, closure en consistency nee (de sluiting is de gewone uitkomst) |
| P9 lezing over een link | "A read over a link" | WV-1.5-link-absent, -present-open, -present-filled (met rolcheck) | ja (present-open 2) |
| P10 gesloten invoer | "Complete input" | WV-1.5-closed-member (met contrast), -closed-every-input, -closed-characteristic | ja (het contrast); de sluitingen zelf nee |
| P11 monotonie | "The two requirements" | WV-1.5-monotone: elke geregistreerde scenario × elke invoer die het geeft weggelaten, plus een check per benoemde uitzondering (10 sluitingen) | ja (123 FAIL op de oude code) |
| P12 behoud | "The two requirements" | WV-1.5-preserved: elk scenario met alle invoer gesloten tegen hetzelfde document zonder de eigenschap | nee (op de oude code is alles de gewone lezing) |
| gewoon document | (ch. 1.2–1.4, 10.6) | WV-1.5-no-open-without-the-property, WV-1.5-boundary-keeps-a-barrier | nee (de gewone lezing; de tweede is rood op een tussenstand die deposito's bij een barrière liet vallen) |
| weigering | (IR-SPEC ch. 2) | WV-1.5-refused-set-fold, -if-then-else, -not-walked-without-the-property, -before-evaluation | ja (3) |
| getuigen | slotzin van ch. 1.5 | WV-1.5-witness-* (19 getuigen, 77 cases, met hun standen) | ja (elke getuige ≥ 2 FAIL) |

### Per eis van "Acceptance (deel 1a)"

- **RUNTIME-SEMANTICS**: ch. 1.5 (`d788c07d6`, aangevuld in `52a76c937` en de laatste commit). Elke zin met een voorbeeld
  uit de Jutterswet en de naam van zijn test; geen proeftaal, geen executorstatus. Gate 5 draait de tests.
- **ref rekent**: regel (gelijkstelling, berekening, kenmerktoekenning: WV-1.5-rule-*, -derived-*; initialisatieregel:
  WV-1.5-initialisation, witness `initialisation`), vergelijking (WV-1.5-atom, -table-*, witness `comparison`), toets op
  leeg en gevuld ook van een berekende waarde (WV-1.5-filled-test-*, witness `filled-test`), cijfertoets (witness
  `exact-digits`), kenmerkcheck ook genavigeerd (WV-1.5-link-*, -negation, witness `characteristic`, `absent-link`),
  rolcheck (WV-1.5-link-*), toets op een marker (witness `marker`), samengestelde voorwaarde met de vijf kwantoren
  (WV-1.5-table-*, witnesses `compound-*`), uitdrukkingen zonder vouw of tijdlijn (witness `arithmetic`,
  `settled-empty-date`), lezing over een pad met afwezige link (WV-1.5-link-*, witnesses `absent-link`,
  `absent-link-path`, `navigated-read`), `Let` (witness `let`), consistentieregel (WV-1.5-consistency,
  -filled-test-consistency, witness `consistency`). De identiteitscheck staat alleen in het filter of de matrix van een
  vouw (WF-122) en komt in een strikt document dus niet voor zonder een vouw, die geweigerd wordt; hij is tweewaardig
  en vaststaand gezet (`established`). Gate 5.
- **Getuigen binnen het bereik**: 19 van de 21 genoemd plus `initialisation`: alle cases van `arithmetic`, `comparison`,
  `filled-test`, `compound-all`, `compound-any`, `compound-none`, `compound-counting`, `characteristic`, `marker`,
  `absent-link`, `absent-link-path`, `navigated-read`, `consistency`, `exact-digits`, `settled-empty`,
  `settled-empty-date`, `let`, `closed-input`, `initialisation` spelen met hun handberekende uitkomsten. Niet
  overgenomen: de delen zonder stapel of vouw van `filled-test-not-closure`, `composite-filled-test` en `empty-by-fault`
  (hun documenten houden een stapel of vouw, ref weigert ze als geheel; het principe van een berekende waarde staat in
  WV-1.5-filled-test-computed, de getuigentoeslag en schadetoeslag van `composite-filled-test` letterlijk).
- **Gesloten invoer**: `Eval.closed = Every_input | Members of (object type, member) list`, als `?closed` van
  `Eval.evaluate`; een lid sluit op elk object van dat type of een subtype. Runnervlag `--closed-inputs` (alle
  invoer). Een opgave is in dit deel niet te maken: zij staat in een defaults-groep, die geweigerd wordt.
- **Weigering bij naam**: `Eval.refuse_uncarried` loopt het document af, alleen als het de eigenschap draagt, en weigert
  met REF-014, `form` = de slug van de eerste vorm in vaste volgorde: `defaults-group`, `allocation`,
  `object-creation`, `fact-creation`, `uniqueness`, `start-point`, `day-kind`, `function-def`, `timeline-member`, dan
  per regel en consistentie in documentvolgorde `dimension-cell`, `set-fold`, `temporal-total`,
  `time-proportional-part`, `dimension-fold`, `builtin-filter`, `variable-reference`, `unresolved`, `temporal-read`,
  `match`, `record`, `list-family`, `if-then-else`, `invoke`, `whole-period`, `quantifier-fold`, `time-window`,
  `rule-status-check`; met `~reading:true` (de runner bij `--trace`) eerst `reading`. De runner roept haar aan bij elke
  ingang, `Eval.evaluate` zelf ook. `ref/codes.tsv`: de functie van REF-014 is `refuse_uncarried` (gate 8,
  check-exec-codes groen).
- **Een gewoon document verandert niet**: gate 5, 7, 8, 13, 17 groen zonder gewijzigde verwachting, pin of lijst (zie
  onder).
- **Behoud bij volledige kennis, gemeten** (`preservation.py` in de werkmap; per golden een kopie met de eigenschap
  direct na `format`, `runner --closed-inputs` op de eigen cases): 432 goldens, waarvan 1 zonder case
  (`jutterswet-afleiden-uit-leeg`). **134 goldens gedragen, 339 cases, 339 gelijk, 0 niet gelijk.** 297 goldens
  geweigerd, geteld naar de eerste vorm die de weigering noemt: defaults-group 55, set-fold 48, timeline-member 48,
  object-creation 27, allocation 21, list-family 16, if-then-else 11, fact-creation 10, quantifier-fold 10, day-kind 9,
  match 8, record 7, uniqueness 7, dimension-cell 6, temporal-total 6, function-def 3, en 1 elk dimension-fold,
  rule-status-check, start-point, time-window, unresolved. Ter vergelijking zonder iets te sluiten: 289 van de 339
  cases houden de verwachting van de gewone lezing; de 50 andere rusten op een open waarde (één daarvan is de fout
  EF-011 van een evaluatiejaar zonder datum achter een onbepaald lid, de corpuscase van STRICT-EMPTY §12.1).
  `jutterswet-afleiden-uit-leeg` draagt ref (geen geweigerde vorm).

### De naad (ref/lib, `git diff cdc4addc1`)

| bestand | regels | functies |
|---|---|---|
| `world.ml` | +77 −16 | nieuw: `exception Open_read`, `is_open`, `read_attr`, `read_char`, `leave_open`, `opened_of`, `opened_by`; velden: `outcome.opened`, `t.open_input`, `t.opening`, `t.opened`; gewijzigd: `empty`, `set_outcome` (`~opened`), `close_evaluation`, `fault_evaluation` |
| `eval.ml` | +357 −117 (42 hunks) | nieuw: `type truth`, `type judgement`, `established`, `undetermined`, `count_verdict`, `held`, `two_valued`, `judge_cond`, `atomic`, `type closed`, `open_inputs`; `refuse_infer_from_empty` vervangen door `refuse_uncarried` (de grootste toevoeging, de opsomming van de vormen); `eval_cond_node` heet `judge_cond_node` en geeft een oordeel; gewijzigd: `eval_cond` (omhulsel: `two_valued` van `judge_cond`), `eval_expr_node` (één regel: de attribuutlezing via `World.read_attr`), de armen van `judge_cond_node` (vergelijking, cijfertoets en kenmerkcheck via `atomic`; leeg en gevuld samen in één arm met de sluiting; samengestelde voorwaarde via `count_verdict`; de overige armen `established`), `cond_periods` (één aanroep), `over_subjects` (een veranderde open-markering telt als verandering), `run_rule` (oordeel, `undecided`, de waarde als één grens), `run_consistency`, `evaluate` (`?closed`, weigering, open invoer) |
| `bin/runner.ml` | +18 −12 | `load_document ?reading`, `--closed-inputs` |

De vier punten van de architectuur:
1. **Eén evaluator**: geen kopie van `eval_expr` of `eval_cond`; geen strikte operator of vouw. `eval_cond` is een
   omhulsel van `judge_cond`, de samengestelde voorwaarde heeft één leeslus (`count_verdict` vervangt de oude `verdict`
   en `settled` en geeft op een document zonder open leden dezelfde stoppunten; afgeleid in het commentaar).
2. **`Value.t` ongewijzigd** (`value.ml` niet geraakt; clingo, catala en flogic bouwen zonder wijziging mee).
3. **Een gewoon document byte voor byte gelijk**: gate 5, 7, 8, 13, 17 groen zonder gewijzigde verwachting. Onder een
   observer (ch. 5.12) is een oordeel tweewaardig, zodat `reading.ml` niets merkt. De grens `held` laat elke andere
   exceptie dan een open lezing door met de deposito's van daarvoor, zoals zonder grens: getuige
   WV-1.5-boundary-keeps-a-barrier (de barrière van een Let in een voorwaarde na een deling door 0 houdt haar EF-005;
   rood op een tussenstand van deze branch die ze liet vallen, groen op `cdc4addc1`).
4. **De eigenschap op één plaats**: `grep infer_from_empty ref/` geeft `refuse_uncarried` (de weigering) en
   `open_inputs` (welke afwezige invoer open is), verder niets; de lader en de schrijver staan in `spec/verifier`.

### Voorlopige besluiten

1. **Het paar in één evaluatie.** Het oordeel is `{own; other}`; de samengestelde voorwaarde leest door tot beide
   vaststaan. Op een document zonder open leden zijn ze gelijk, dus de leesvolgorde is die van ch. 5.10.
2. **De grens van een open lezing**: een atomaire voorwaarde en de waarde van een regel; de deposito's binnen die grens
   worden vastgehouden en pas na afloop doorgegeven (`held`), zodat een afgebroken evaluatie niets deponeert.
3. **Invoer per object**: een lid is invoer op een object als geen regel het op het type van het object schrijft
   (`isa`); een regel buiten zijn geldigheidsvenster telt als schrijver en vult daar vaststaand niets.
4. **Een initialisatieregel sluit** over haar doel (ook boven een schrijver die het doel open laat), met haar eigen
   voorwaarde en waarde als die van een regel.
5. **De open-markering is een schrijfactie** voor de fixpuntlus: een veranderde `opened` van een uitkomst telt als
   verandering (getuige WV-1.5-derived-chain "reader first, nothing written"; gemeten dat de test rood wordt zonder).
6. **`--trace` op een strikt document** weigert met REF-014 `form` `reading`; onder een observer is een oordeel
   tweewaardig (een onbepaald oordeel daar is een fout van ref, `two_valued`).
7. **De weigering noemt de eerste vorm**; een gedeclareerd lid met een tijdlijn en een gedeclareerde dagsoort weigeren
   het document ook als niets ze leest (veilig, ruimer dan nodig).
8. **Standen van de getuigen** staan in `states.json` naast hun cases, tot het case-formaat ze draagt (deel 1b); een
   invoerkenmerk "K (niet)" van de proef is een gesloten K op het type van het object.
9. **Monotonie-test**: een link wordt niet weggelaten (een relatie is volledig, ch. 1.5).

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: na "…`form`, here `document`)." vervangen "The reading itself is open." door: "ref
carries the reading of RUNTIME-SEMANTICS ch. 1.5 for a rule, a consistency rule, a condition without a fold and an
expression without a fold or a timeline, and refuses a document with another form by name: `REF-014` with the slug of
the form as `form` (`reading` for `--trace`). The reading of the other forms is open." Getuigenkolom erbij:
"WV-1.5-* and WV-1.5-witness-* (`ref/test/fixtures/strict-empty/`) in the suite of gate 5".

### Open punten

1. **Deel 1b, case-formaat**: de velden "staat vast dat niet" en "open"; daarna kunnen de `states.json` van de getuigen
   in de casebestanden zelf, en kan een case per object sluiten (`quantifier-fold` case7 van de proef sluit een kenmerk
   op één van twee objecten van één type: met de sluiting per type niet te schrijven).
2. **Gate 17 en de strikte golden**: zodra `jutterswet-afleiden-uit-leeg` cases heeft, weigert `runner --trace` hem
   (REF-014 `reading`); gate 17 telt dat als ERROR. Deel 1b of 3 beslist (READINGS_ERROR_KNOWN of de lezing zelf).
3. **Deel 2**: de opgave (gesloten door haar declaratie) en rij 3 van de tabel in §5.4 van het voorstel (een uitzondering
   boven een basis) vragen de defaults-groep; de getuigen met stapel, vouw of creatie, en de drie gedeeltelijke getuigen.
4. **De weigering noemt één vorm**; een telling van alle vormen per golden vraagt een lijst in de foutparameter.
5. Bijvangst, geen fout: de aanroepers van `eval_cond` (IfThenElse, lijstfilter, vouwfilters en -matrix, creaties,
   defaults, dagsoort) lezen tweewaardig; elk van die vormen wordt in een strikt document geweigerd. Deel 2 vervangt ze
   per vorm door `judge_cond`.

### Gates, los gedraaid

Op de eindstand `9db3d29e6` (gate 3 op `d788c07d6`; de verifier is daarna niet geraakt), elk los, logs `final2-gate*.log` in de werkmap:

```
check-self-contained: PASS
check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 508 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 508 documents: 834 closure places, floor 827, 383 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1461 pointers resolve)
check-oracle: PASS (ref suite 3374 worked examples, floor 489; 1199 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-afleiden-uit-leeg)
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1157 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; 179 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 297 pinned parameter checks confirmed, floor 120; 46 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-readings: PASS (ref traced 1199 golden case files; on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 87 s)
```

### Landing van deel 1a (hoofdsessie, 2026-11-21)

Nagelezen: de hele wijziging in `ref/lib/world.ml`, `ref/lib/eval.ml` en `ref/bin/runner.ml`, de namen van de tests per
principe en de paragraaf 1.5. De architectuur van de kaart is gehouden: `Open_read` en `held` dragen de lezing, geen
operator is geraakt, het oordeel van de samengestelde voorwaarde verving de tweewaardige telling, en de eigenschap wordt
voor het rekenen alleen in `open_inputs` gelezen. De paragraaf landt als geldende tekst (de statusregel "PROPOSAL" is
eruit); rij 48 van `spec/CONSTRUCTS.md` is bijgewerkt. `make gates` groen op `1f8711886` (654 s, 17 gates, red=0).
De kaart blijft in `backlog/doing/`: deel 1b, 2 en 3 volgen op dezelfde branch.
