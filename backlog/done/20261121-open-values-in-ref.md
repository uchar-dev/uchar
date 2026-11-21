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

## Result (deel 1b)

*Bouwer deel 1b, 2026-11-21. Branch `card/open-values-in-ref`, op `e1c24bc6f`.*

### Wat er is

| commit | wat |
|---|---|
| `fcf76af0b` | case-schema: `characteristicsAbsent` in de invoer (na `characteristics`), de verwachtingen `attributeOpen` en `characteristicOpen` (achteraan in de `oneOf`, zodat de patroonposities van gate 4 blijven), beschrijvingen van `attributeEmpty` en `characteristicAbsent` als vaststaand |
| `40d2e9fdd` | gate 4: twaalf zelftestrijen voor de standen (`STATE_ROWS`) |
| `5b5e36111` | ref: caselezer en controle (`Case`, `World.stated_absent`, `World.is_open`), tests WV-1.5-case-* en drie rijen WV-5.7-case-reader-refuses-…; de fixtures van de getuigen zonder `states.json`; clingo's modelcontrole kent de twee open soorten |
| `486be6dd5` | golden `jutterswet-afleiden-uit-leeg`: drie regels erbij, vier cases, `NOTE.md`; de zin in `DIVERGENCES.md` over de golden |
| `a8621336d` | RUNTIME-SEMANTICS (voorstel): ch. 1.5 "The case", ch. 5.7 de weigeringen, ch. 5.7b rij EF-047; rode getuige `corpus/red/jutterswet-kenmerk-wel-en-niet` |
| `e19320f49` | gate 8: Drools-poot `jutterswet-kenmerk-wel-en-niet|EXCLUDE DROOLS-370` |
| `b7e0604ad` | gate 6: `jutterswet-afleiden-uit-leeg|CLINGO-049` in `EXPECTED_CHANNEL` |
| `337bd7b37` | gate 12: de `NOCASE`-regel wordt `EXCLUDE DROOLS-370` |
| `413206b96` | gate 17: lijst `TRACE_REFUSED` (golden, REF-code, reden) |
| `95a08baa4` | gate 5: check 5, behoud bij volledige kennis, met floors en een rode rij |
| (slotcommit) | dit Result |

`backends/drools/` is niet geraakt: de build weigert de golden (DROOLS-370) voordat er een plan is, dus
`DrlTextPinWitnessTest` vraagt geen DRL-pin.

### Het case-formaat

- **Invoer** `characteristicsAbsent` (lijst namen, optioneel): de case zegt dat het kenmerk vaststaand niet geldt. De
  lezer zet het in `World.stated_absent` (per label en kenmerk); `World.is_open` geeft voor zo'n lid nooit open. Dat is
  data, geen tak op de eigenschap: in een gewoon document is niets open en verandert het veld niets. Fouten: in
  `characteristics` en in `characteristicsAbsent` → EF-047 (`label`, `characteristic`); niet gedeclareerd → EF-040
  (`label`, `name`); tijdsafhankelijk → fout van de case zonder code, zoals bij `characteristics`.
- **Verwachtingen**: `attributeEmpty` houdt waar het attribuut geen waarde heeft en niet open is; `characteristicAbsent`
  waar het kenmerk niet geldt en niet open is; `attributeOpen` en `characteristicOpen` waar het lid open is. Op een
  tijdsafhankelijk lid weigert de lezer de open soorten zoals de andere scalaire soorten. clingo, dat geen strikt
  document vertaalt, vindt een open verwachting nooit vervuld (`backends/clingo/lib/model.ml`, twee armen).
- **No-op** (gate 7): de ruwe invoer kent geen open lid, dus een `…Open`-verwachting houdt daar nooit en
  `attributeEmpty`/`characteristicAbsent` lezen de ruwe invoer als voorheen (WV-1.5-case-raw-input).

### Per eis van de opdracht

| eis | getuige | gate | rood eerst |
|---|---|---|---|
| schema: twee soorten en het veld | `STATE_ROWS` (12 rijen) | 4 | 4 rijen rood op het schema van `e1c24bc6f` (`logs/1b-red-schema-01.log`) |
| caselezer: het veld | WV-1.5-case-characteristics-absent (7), -absent-refused (2), WV-5.7-case-reader-refuses-characteristicsAbsent-on-time-dependent | 5 | rood op `e1c24bc6f`: stated, stated-not-open, not-named-open, beide weigeringen, de tijdsafhankelijke rij (het veld werd genegeerd) |
| caselezer: `attributeOpen` | WV-1.5-case-attribute-open (7), WV-1.5-case-raw-input, WV-5.7-…-attributeOpen-on-time-dependent | 5 | rood: elke rij met de soort ("unknown expectation kind") en "open, attributeEmpty" (de oude controle zag open als leeg) |
| caselezer: `characteristicOpen` | WV-1.5-case-characteristic-open (7), WV-1.5-case-raw-input, WV-5.7-…-characteristicOpen-on-time-dependent | 5 | rood als hierboven |
| rood eerst, samen | — | — | 20 FAIL op `e1c24bc6f` met alleen de nieuwe tests (`logs/1b-red-suite-01.log`; 3 andere FAIL daar zijn de runner-checks van WV-2, die in die boom geen runner hadden) |
| fixtures: standen in de casebestanden | WV-1.5-witness-* (19 getuigen, 77 cases) leest nu alleen de casebestanden en `closed-input/closed.json` | 5 | met de strengere controle faalden 28 cases van de oude fixtures (100 verwachtingen op een open lid) en de case van WV-2-infer-from-empty (`logs/1b-suite-01.log`) |
| golden met handberekende cases | `corpus/golden/jutterswet-afleiden-uit-leeg` (4 cases, 32 verwachtingen) | 5, 7 | elke case faalt in `--noop` |
| clingo weigert bij naam | `EXPECTED_CHANNEL` regel | 6 | — |
| Drools weigert bij naam | `EXPECTED_CHANNEL` `EXCLUDE DROOLS-370` | 12 | de oude `NOCASE`-regel zou nu falen (de golden heeft cases) |
| lezingen bij naam geweigerd | `TRACE_REFUSED` | 17 | zonder de regel faalt check 1 op "ref does not trace" |
| behoud bij volledige kennis | check 5 van gate 5, rode rij `PRESERVATION_MUTANT` | 5 | de rode rij: zonder `--closed-inputs` mist `jutterswet-leeg-deeltal` 1 van 3 cases |

**De fixtures.** Per getuige: een invoerkenmerk "K (niet)" van de proef, eerder een sluiting per type in `states.json`,
is nu `characteristicsAbsent` op het object (`characteristic` case2 en case4, `initialisation` case2 en case6); de
standen "not", "leeg" en "open" staan als `characteristicAbsent`, `attributeEmpty`, `attributeOpen`/`characteristicOpen`
in de case. Alleen `closed-input` houdt een `closed.json` (schade en beschadigd per type gesloten, de sluiting die de
proef bij de omzetting maakte). De strengere controle vond 100 verwachtingen "geen waarde" op een lid dat open is; 35 noemde `states.json` als open,
de 65 andere niet (de toeslagen achter een open kenmerk, de berekeningen over een open operand); elk is met
de hand nagerekend op de regels van de getuige en omgezet naar de open soort. Het script en de gecontroleerde lijst
staan in de werkmap (`draft/convert_fixtures.py`, `draft/open_mismatches.py`).

**De golden.** Bron: de taxatie, het gewicht, het vindloon en kostbaar, plus de bewakingstoeslag ("5 indien hij niet
kostbaar is"), ongetaxeerd ("indien zijn taxatie leeg is") en het meldloon ("3 indien hij gemeld is", gemeld een
invoerkenmerk). Cases:

- case1-alles-gegeven: V1 taxatie 2000, gewicht 5, gemeld → kostbaar, vindloon 15, bewakingstoeslag vaststaand leeg,
  ongetaxeerd staat vast niet, meldloon 3; V2 taxatie 50, gewicht 0, gemeld afwezig → kostbaar niet, vindloon 10,
  bewakingstoeslag 5, ongetaxeerd niet, gemeld niet, meldloon vaststaand leeg; consistent.
- case2-taxatie-open: gewicht 5, gemeld → kostbaar open, vindloon 15, bewakingstoeslag open, ongetaxeerd geldt
  (sluiting), meldloon 3.
- case3-gewicht-open: taxatie 50, gemeld afwezig → kostbaar niet, vindloon open, bewakingstoeslag 5, ongetaxeerd niet,
  gemeld niet, meldloon vaststaand leeg.
- case4-beide-open: niets → kostbaar, vindloon, bewakingstoeslag, gemeld en meldloon open, ongetaxeerd geldt.

ref speelt de vier (gate 5); clingo weigert elke case met CLINGO-049 (gate 6); beide Drools-branches eindigen in
`EXCLUDE DROOLS-370`, ook `--readings` (gate 12, 17); `runner --trace` weigert met REF-014 `form` `reading` (gate 17).
Gate 10 herschrijft `ir.json` byte-identiek.

**Gate 5, check 5.** Per golden met cases zonder de eigenschap: een kopie met `"inferFromEmpty": false` direct na
`format`, `runner --closed-inputs` per case, één runner per core (`scripts/lib/parallel.sh`), in corpusvolgorde
beoordeeld. Gemeten: **134 goldens, 339 cases, alle gelijk** (floors `EXPECTED_MIN_PRESERVED_GOLDENS=134`,
`EXPECTED_MIN_PRESERVED_CASES=339`, 2026-11-21); 297 goldens geweigerd, per eerste vorm (defaults-group 55, set-fold 48,
timeline-member 48, object-creation 27, allocation 21, list-family 16, if-then-else 11, fact-creation 10,
quantifier-fold 10, day-kind 9, match 8, record 7, uniqueness 7, dimension-cell 6, temporal-total 6, function-def 3, en
1 elk dimension-fold, rule-status-check, start-point, time-window, unresolved); 1 golden draagt de eigenschap al
(`jutterswet-afleiden-uit-leeg`, gespeeld door check 3). Rode rij: dezelfde kopie van `jutterswet-leeg-deeltal` zonder
`--closed-inputs` mist case1 ("V1.loon = leeg, expected 0"); speelt zij al haar cases, dan faalt de gate.

**Gate 7, 13 en 17, gemeten.** Gate 7: de vier cases falen in `--noop` (elke case heeft een verwachting die de ruwe
invoer niet geeft); geen survivor nodig, het script is niet geraakt. Gate 13: zie de samenvattingsregel hieronder.
Gate 17: zonder lijst faalt check 1 op de geweigerde sporen; `TRACE_REFUSED` is daarom gebouwd zoals besluit 5 vroeg,
met als extra eis dat de backend een genoemde golden uitsluit (anders zijn er lezingen die niemand vergelijkt).

### Gewijzigde bestaande tests (de taak verandert hun gedrag)

- WV-2-infer-from-empty: V3 van het fixture is open, dus `ref/test/fixtures/infer-from-empty/case.json` verwacht nu
  `characteristicOpen` voor V3's meldplichtig; de gewone golden `jutterswet-meldplicht-flogic` speelt dezelfde invoer
  met `case-ordinary.json` (V3 niet meldplichtig). Zo ook de clingo-test "inferFromEmpty absent: the runner plays the
  golden" (`backends/clingo/test/test_clingo.ml`).
- WV-1.5-witness-*: leest de casebestanden en `closed.json` in plaats van `states.json`. De suite telt nu 3328 in plaats
  van 3374 voorbeelden: de losse standcontroles van `states.json` zijn verwachtingen binnen de controle van hun case
  geworden (één controle per case), niets is weggelaten.
- De weigerzin voor een open soort op een tijdsafhankelijk kenmerk noemt de soort (`characteristicOpen`); de zin voor
  `characteristicPresent` en `characteristicAbsent` is ongewijzigd.

### Voorlopige besluiten

1. **Een kenmerk in `characteristicsAbsent` is nooit open**, ook naast een schrijver die niets beslist (zoals een
   invoerwaarde blijft staan, ch. 7.8); een schrijver die beslist dat het geldt, wint (WV-1.5-case-characteristics-absent,
   "beside a writer that decides"). Gebouwd in `World.is_open`, niet in `open_inputs`: één plek voor invoer en afgeleid lid.
2. **EF-047** voor een kenmerk in beide lijsten, met `label` en `characteristic`; de rode getuige draagt het strikte
   document (daar beslist het veld), dus Drools weigert eerst met DROOLS-370 en staat in de Drools-poot van gate 8.
3. **De golden groeit met drie regels**: het ontkende kenmerk en de toets op leeg (de twee die de opdracht noemde) en een
   invoerkenmerk met het meldloon, omdat `characteristicsAbsent` anders in geen golden staat en gate 4 en 5 het veld dan
   alleen via zelftest en unit tests zien.
4. **Fixtures**: "K (niet)" is `characteristicsAbsent` per object (de proef bedoelde een object), niet langer een
   sluiting per type; `closed.json` alleen voor de sluiting van de sessie zelf.
5. **Gate 5, check 5**: een golden die de eigenschap al draagt, doet niet mee; de vorm van een weigering wordt uit de
   eerste regel van het rapport gelezen (`… for the <slug>: it is refused`), een onbekend rapport faalt bij naam.
6. **Gate 17**: een genoemde golden moet ook door de backend worden uitgesloten.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: na "…The reading of the other forms is open." toevoegen: "The case format says the
states of ch. 1.5: `characteristicsAbsent` in the input (a characteristic named there and in `characteristics` is
EF-047), `attributeOpen` and `characteristicOpen` beside `attributeEmpty` and `characteristicAbsent`, which expect a
settled member. The golden `jutterswet-afleiden-uit-leeg` plays in ref and is refused by name by clingo (gate 6) and
both Drools branches (gate 12), its readings by ref (gate 17); gate 5 holds preservation with complete knowledge over
the corpus." Getuigenkolom: "the golden `jutterswet-afleiden-uit-leeg`, without a case file" vervangen door "the golden
`jutterswet-afleiden-uit-leeg` with four cases; `corpus/red/jutterswet-kenmerk-wel-en-niet`; WV-1.5-case-*; check 5 of
gate 5".

### Voorstel voor `CLAUDE.md`

- Gate 4, laatste zin: "Self-test rows hold the shape of `expectedFault` (a `params` object of strings, no fragment
  key), of a cell, and of the states of RUNTIME-SEMANTICS ch. 1.5 (`characteristicsAbsent`, `attributeOpen`,
  `characteristicOpen`)."
- Gate 5, vóór "Floors:": "A preservation leg (RUNTIME-SEMANTICS ch. 1.5): every golden whose document does not carry
  `inferFromEmpty`, copied with `inferFromEmpty: false` and replayed with `runner --closed-inputs`, plays its own
  cases; a golden whose copy ref refuses (REF-014) is counted per form, any other difference fails by name, and the
  copy of `PRESERVATION_MUTANT` without `--closed-inputs` must miss a case (its red row)." Floors erbij:
  `EXPECTED_MIN_PRESERVED_GOLDENS`, `EXPECTED_MIN_PRESERVED_CASES`.
- Gate 6 en 12: geen tekstwijziging (alleen een regel in hun lijst).
- Gate 17: "The trace reads every golden case file; …" wordt "The trace reads every golden case file except those of a
  golden `TRACE_REFUSED` names with the code of its refusal (REF-014, form `reading`, a document with
  `inferFromEmpty: false`), which the backend must exclude as well; …", en "The three lists are reason-carrying" wordt
  "The four lists are reason-carrying".

### Open punten

1. `InferFromEmptyRefusalWitnessTest` (javadoc) zegt dat de golden geen casebestand heeft; dat klopt niet meer.
   `backends/drools/` viel buiten dit deel; een regel tekst voor de hoofdsessie of de kaart van de Drools-lezing.
2. De caselezer van Drools kent `characteristicsAbsent` en de open soorten niet: het veld wordt genegeerd, een open
   soort is "unknown expectation kind". Alleen bereikbaar op een document dat de build niet weigert, dus een gewoon
   document met een `…Open`-verwachting: ref en clingo geven een mismatch, Drools een fout. Van de kaart van de
   Drools-lezing.
3. De scenariolaag van de RegelSpraak-frontend (`--cases`) schrijft `characteristicsAbsent` en de open soorten nog niet;
   een schrijfwijze in `scenario.rgs` is niet gekozen.
4. Een cel van een gedimensioneerd attribuut heeft geen open soort (`cellEmpty` blijft zoals het was); in een strikt
   document weigert ref de lezing van een cel (`dimension-cell`).
5. De gemeten paragraaf "Parameters" van ch. 5.7b (2026-10-19) noemt EF-047 niet; ref en clingo geven beide
   parameters.
6. Deel 3 maakt `TRACE_REFUSED` stale zodra `--trace` een strikt document leest.

### Gates, los gedraaid

Elk los, één zware tegelijk, logs `logs/1b-gate*.log` in de werkmap. Op de eindstand `95a08baa4`: gate 1–7, 8 (tweede
run), 10, 12, 13, 16, en extra gate 15 (de bron van de golden groeide). Gate 17 draaide op de stand van `413206b96`
voordat de tekst van zijn PASS-regel en zijn kopcommentaar veranderden (de regel hieronder heeft nog de oude woorden);
de nieuwe PASS-regel is los met de echte argumenten geformatteerd.

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 552 shape self-test row(s); 509 file(s) validate and round-trip, floor 192; 66926 object(s) in 1166 document(s) in the schema's key order, 2 in no subschema)
check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 509 documents: 837 closure places, floor 827, 385 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1466 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1305 case file(s) validate)
check-oracle: PASS (ref suite 3328 worked examples, floor 489; 1203 cases replayed exactly, floor 427; preservation with complete knowledge: 134 goldens and 339 cases play, floors 134 and 339; 297 goldens refused per form (defaults-group 55, set-fold 48, timeline-member 48, object-creation 27, allocation 21, list-family 16, if-then-else 11, fact-creation 10, quantifier-fold 10, day-kind 9, match 8, record 7, uniqueness 7, dimension-cell 6, temporal-total 6, function-def 3, dimension-fold 1, rule-status-check 1, start-point 1, time-window 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs)
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 34 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1161 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-regelspraak: PASS — JUnit suite green (1704 tests, floor 173); 222 rgs golden(s) re-emit byte-identically (floor 80)
check-drools: PASS (played 384; auto-excluded 48; no-case 0; channel floor 48 non-PLAYED goldens pinned by name, played matches the pinned 384; DRL-text branch played 384; JUnit 1976 tests, floor 1573)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-graph: PASS (extra; route leg: the graph of the wire equals the graph of the source on all 222 goldens with a source.rgs, floor 98; 83 unit tests green, floor 23)
check-scenario: PASS — 48 scenario golden(s) (floor 7) regenerate 176 case file(s) (floor 31) and their ir.json byte-identically
check-readings: PASS (ref traced 1199 golden case files, and refused the trace of 4 of 1 golden(s) by name (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 89 s)
```

### Landing van deel 1b (hoofdsessie, 2026-11-21)

Nagelezen: de caselezer en de controle (`ref/lib/case.ml`), `stated_absent` in de wereld, de poot in gate 5, en de vier
cases van de golden, met de hand nagerekend. De alinea's van gate 4, 5 en 17 in `CLAUDE.md` en rij 48 van
`spec/CONSTRUCTS.md` zijn gezet; de javadoc van `InferFromEmptyRefusalWitnessTest` zegt niet meer dat de golden geen
casebestand heeft (open punt 1). `make gates` groen op `266b24d0b` (684 s, 17 gates, red=0; de poot voor behoud: 134
goldens en 339 cases). De open punten 2 en 3 (de caselezer van Drools, de scenariolaag) horen bij de kaart waarin
Drools de lezing draagt en bij een kaart voor de scenariotaal.

## Result (deel 2a)

*Bouwer deel 2a, 2026-11-21. Branch `card/open-values-in-ref`, op `247a61084`. Opdracht: de voorwaarden 1–4 van het
onafhankelijke oordeel over deel 1a (A–D), daarna de vouwen (E).*

### Wat er is

| commit | wat |
|---|---|
| `cc20991ce` | A: ch. 1.5 zegt de operationele keuzes (wat een evaluatie bereikt, binnen één grens, de leesregel van de samengestelde voorwaarde, de eigen ontkenning, een regel buiten haar venster, een instance-ordered deel), met tests; de invariant van het paar in tekst en commentaar; stopconditie en `run_rule` korter |
| `037aca65e` | B: een waarde die geen schrijver besliste, is open zolang een schrijver het lid open laat; `World.attr_open`/`char_open` als de ene plaats van de stand; eindcontrole van een case via die plaats; monotonie-test over invoerleden |
| `d3ca0b4ce` | C: elke lezing tijdens een evaluatie via een lezer met de stand (`read_attr`, `read_char`, nieuw `read_timeline`, `read_char_periods`); de lijst rechtstreekse lezingen als test; de grens heet `open_read_boundary` |
| `83ce7a370` | D: het sluitingssignaal van de grens; de toets op leeg en gevuld als bijzonder geval; een regel met een lege waarde na een sluiting laat haar doel open |
| `3714a6fad` | E: de kwantorvouw en de vouwen over een verzameling strikt, de telling van gevulde waarden en een filter op gevuld via het signaal; `refuse_uncarried` laat ze los; vijf getuigen als fixtures; ch. 1.5 |
| `aba670949` | gate 5: floors van de poot voor behoud 197 goldens, 480 cases (eigen commit) |
| (slotcommit) | dit Result |

### A. De norm zegt wat de code doet

Ch. 1.5 "An open read" zegt nu de regel: een evaluatie leest wat de gewone lezing bereikt (ch. 5.11), en wat zij leest
dat open is, breekt haar af; binnen één grens vervalt een deponering vóór de open lezing, blijft een fout ervoor een
fout, en ontstaat een fout erna niet. "For the form itself and for another" zegt de invariant (het oordeel voor een
ander staat nooit vast waar het eigen oordeel dat niet doet, en dan met dezelfde waarde) en de leesregel (de
samengestelde voorwaarde leest tot haar oordeel voor een ander vaststaat; een lid daarna deponeert en faalt als elk
gelezen lid). "The test on leeg and gevuld": de eigen ontkenning van een gevuld-toets sluit. "The three states": een
regel buiten haar venster schrijft haar doel en vult daar niets. "A derived member": een instance-ordered deel komt per
object tot zijn fixpunt. Code: de stopconditie is `other = Established _`; `run_rule` heeft drie armen (`other`
onwaar → vult niets; eigen niet waar → open; eigen waar → beslist).

| keuze | test | rood eerst |
|---|---|---|
| wat bereikt wordt | WV-1.5-reached (leeg deeltal, binder niet gelezen, en de tegenhangers) | nee op `247a61084` (gedrag bestond); mutant "deler altijd lezen": 2 FAIL |
| leesregel samengestelde voorwaarde | WV-1.5-read-for-another (vrijstelling 1 met EF-031; volledig: zonder feit) | nee; mutant "stop op het eigen oordeel": 1 FAIL, door geen bestaande test gevangen |
| deponering vóór een open lezing | WV-1.5-boundary-deposit-before-an-open-read (4) | nee; mutant "deposito's doorlaten": 1 FAIL |
| fout vóór / na een open lezing | WV-1.5-boundary-fault-before-an-open-read, -after-an-open-read (EF-014) | nee (vastgelegd gedrag) |
| eigen ontkenning | WV-1.5-own-negation (2) | nee; mutant "geen sluiting": FAIL |
| buiten het venster | WV-1.5-outside-the-window (3) | nee (vastgelegd gedrag) |
| instance-ordered deel | WV-1.5-instance-ordered (2 cases × 2 volgordes, de documenten van WV-6.4-i met de eigenschap) | nee; mutant "geen sluiting": 2 FAIL |

Logs: `logs/2a-mutants-A-01.log`, `-02.log` in de werkmap. Het instance-ordered deel wordt gedragen, niet geweigerd.

### B. Monotonie

**Voorlopig besluit (hoofdsessie, ter bevestiging):** een lid met een waarde die geen schrijver besliste (de case, of
een creatie) is open zolang een staande uitkomst het open laat; de waarde staat pas vast als van elke schrijver
vaststaat dat hij niets vult (ch. 7.8). Gebouwd op de ene plaats van de stand, `World.attr_open`: zonder waarde open
als open invoer of door een uitkomst opengelaten; met een waarde die een schrijver besliste (`owners`) gevuld; met een
andere waarde open zolang een uitkomst het lid open laat.

Wat het verder raakte, en waarom:
- **de eindcontrole van een case** (`Case.check`): `attributeValue`, `attributeEmpty`, `attributeOpen`, de cel- en
  tijdlijnverwachtingen en `characteristicAbsent`/`characteristicOpen` vragen eerst de stand; anders zou een case 7
  verwachten van een lid dat open is.
- **`characteristicsAbsent`**: het voorlopige besluit 1 van deel 1b ("nooit open, ook naast een schrijver die niets
  beslist") rustte op het herziene besluit en volgt het: naast een schrijver die het kenmerk open laat, is het open;
  naast een schrijver die niets vult, staat het (`World.char_open`). Een kenmerk dat de case noemt, blijft gelden: een
  schrijver kan het alleen toekennen. De test WV-1.5-case-characteristics-absent verwacht daarom op de rij "beside a
  writer that decides nothing" nu `characteristicOpen` (gewijzigde verwachting, door de taak), met een nieuwe rij
  "beside a writer that fills nothing".
- **de initialisatie die bij een invoerwaarde uit staat** (`World.init_may_write`): ongewijzigd en juist: met een
  invoerwaarde schrijft de initialisatie in geen enkele aanvulling.
- **de monotonie-test** laat alleen invoerleden weg: een waarde die de case een afgeleid lid geeft, is geen invoer en
  staat waar elke schrijver niets vult in plaats van vaststaand leeg (een zin in ch. 1.5).

Test: WV-1.5-input-beside-a-writer (vindloon 7 naast "10 indien taxatie > 1000": open zonder taxatie, ook de
uitbetaling; 10 bij 2000; 7 bij 50), drie geregistreerde scenario's in WV-1.5-monotone. Rood eerst op `cc20991ce` met
stubs die de nieuwe API de oude lezing geven: 4 FAIL (beide rijen open, de case-rij, de monotonie bij taxatie 2000;
`logs/2a-red-B-01.log`).

### C. De open lezing is niet te omzeilen

Elke lezing van een lid tijdens een evaluatie gaat via `World.read_attr`, `read_char`, `read_timeline` of
`read_char_periods` (de laatste twee nieuw, voor een tijdlijn en de waarperiodes van een kenmerk); een cel leest via
`read_attr`. Ook in de vormen die een strikt document nog weigert (tijdlijnen, verdeling, uniciteit, dimensievouw),
zodat een vorm die later vrijkomt de stand al vraagt. De test **WV-1.5-direct-reads** leest de code van `ref/lib` (zonder
`world.ml`) en `ref/bin`, commentaar en strings eruit, per top-level functie, op `World.attr`, `has_char`, `timeline`,
`char_periods` en de velden `.attrs`, `.tls`, `.ctls`, `.chars`, en eist gelijkheid met de lijst; een mutant die één
lezing terugzet op `World.attr` faalt bij naam (`logs/2a-mutants-C-01.log`). De lijst: **18 plaatsen, 26 lezingen**:

| plaats | lezingen | reden |
|---|---|---|
| `eval.ml` `open_inputs` | `World.attr`, `timeline`, `has_char`, `char_periods` | wat de case geeft, bepaalt welk invoerlid open is |
| `eval.ml` `validate_input_precision` | `.attrs`, `.tls` | toetst de invoer vóór de evaluatie (ch. 2.4) |
| `eval.ml` `input_empty_days` | `World.attr`, `timeline` | leest de invoerwereld |
| `eval.ml` `run_defaults_group` | `World.attr` | leest de invoerwereld |
| `eval.ml` `initialisation_days` | `World.attr` | de bewaking van een initialisatie leest haar eigen doel (ch. 5.4) |
| `case.ml` `check` | 5× `World.attr`, `timeline`, 3× `has_char` | de eindcontrole: eerst de stand, dan de opgeslagen waarde |
| `end_state.ml` `object_lines` | 3× `World.attr`, `timeline`, `has_char`, `.ctls` | de eindstand (ch. 5.12a); geweigerd voor een strikt document |
| `reading.ml` `defaults` | `World.attr` | leest de invoerwereld |

De grens heet `open_read_boundary`; `held` betekent in `ref/lib` nu alleen nog `World.held` (en `held_over`, de
observer). Bijvangst: `open_inputs` telt een kenmerk dat de case met waarperiodes geeft als gegeven (was open invoer;
onbereikbaar, want `timeline-member` wordt geweigerd).

### D. Een sluiting binnen een uitdrukking

Het signaal: een knoop die een sluiting op een open waarde toepast, roept `signal_closure` aan; dat zet een vlag van de
binnenste grens onder weg (een stapel `boundaries`), en `open_read_boundary` geeft `Some (waarde, gesloten)` terug.
`atomic` maakt van `gesloten` het oordeel `{ own = b; other = Undetermined }`; een regel met waarde en signaal beslist,
een regel met een lege waarde en signaal laat haar doel open (eigen: vult niets; voor een ander staat dat niet vast).
De toets op leeg en gevuld is het bijzondere geval: zij leest haar operand met `closure_over_open` (open → leeg voor de
vorm zelf, signaal) binnen een atomaire grens. Een filter in een waarde gaat via `value_verdict`: eigen onbepaald →
open lezing, eigen vast en voor een ander onbepaald → signaal. Eén evaluatie per uitdrukking; de evaluator geeft één
waarde; een signaal buiten een grens is een fout van ref (`failwith`).

Ch. 1.5 "A closure inside a value". Test: WV-1.5-closure-in-a-value (7: gevuld-telling 2 over 20, open, 30; "gelijk aan
3" open, "groter dan 1" geldt; som met filter op gevuld 50; over alleen open gewichten telling 0 en som **open**; zonder
vondsten som vaststaand leeg). Rood: op `83ce7a370` weigert ref de vouw (zie E); mutanten "gevuld-telling zonder
sluiting" 3 FAIL, "leeg na sluiting vult niets" 1 FAIL (`logs/2a-mutants-E-01.log`).

### E. De vouwen

- **De kwantorvouw**: eerst het filter van elk lid van het bereik (zoals een vouw haar bijdragers vaststelt), dan per
  lid waarvan het filter voor een ander niet onwaar is de matrix, beide als oordeel; een lid is lid van een
  samengestelde voorwaarde over het bereik (bij `all`/`the`: filter onwaar of matrix waar; anders: beide waar, Kleene),
  geteld met `count_verdict`, gelezen tot het oordeel voor een ander vaststaat. Waar elk filter vaststaat zijn de
  stoppunten die van ch. 3.9a (afgeleid in het commentaar; gate 17 ongewijzigd groen). Tests: WV-1.5-quantifier-fold-
  member (32 rijen over zes kwantoren, ook met het open lid voorop), -filter (2), -closed-set (2).
- **De vouwen over een verzameling** (som, minimum, maximum, eerste, laatste, telling, gevuld-telling, de telling van
  een vereniging, en opgesomde vormen erover): filter onwaar telt niet, vaststaand leeg wordt overgeslagen, onbepaald
  filter of open waarde maakt de vouw open (de telling alleen via haar filter), geen bijdrage is vaststaand leeg of de
  terugvalwaarde. Test: WV-1.5-set-fold (4 rijen × 6 doelen, plus vaststaand leeg overgeslagen en een open afgeleid lid).
- **Getuigen**: `quantifier-fold` (7 cases, case7 nu met `characteristicsAbsent` per object), `set-fold` (4),
  `settled-empty-fold` (4), `listed-fold` (5), `empty-by-fault` (4, nu geheel) in `ref/test/fixtures/strict-empty/`,
  omgezet met `draft2a/witness_cases.py` in de werkmap (het case-formaat van deel 1b). Dertien verwachtingen "geen
  waarde" van de proef bleken open, elk met de hand nagerekend en omgezet: `empty-by-fault` case4 (afwijkend, totaal:
  deling door een open aantal), `listed-fold` case3/4 (eerste en laatste meldingsdag: een vouw over een open vinddag),
  `set-fold` case2 (vijf vouwen over een open gewicht of een open zwaar), case4 (eerste en laatste vinddag: V1 zonder
  vinddag), `settled-empty-fold` case2 (jutterloon over een open totaal). `filled-test-not-closure` en
  `composite-filled-test` hebben een defaults-groep en wachten op 2b.
- **De weigering**: `refuse_uncarried` laat `set-fold`, `quantifier-fold` en de telling van een vereniging los en loopt
  hun filter, bron en terugvalwaarde af. WV-1.5-refused-set-fold is WV-1.5-refused-temporal-total geworden (de eigen
  totaalsom weigert nog) met WV-1.5-refused-not-the-set-fold (gewijzigd gedrag, door de taak).
- **Rood eerst**: op `83ce7a370` stopt de suite bij de eerste E-test met REF-014 `form` `quantifier-fold`
  (`logs/2a-red-E-01.log`); mutanten: "een onbepaald lid maakt de hele kwantorvouw onbepaald" 8 FAIL (de rijen met het
  open lid voorop), "onbepaald filter sluit uit" FAIL op WV-1.5-set-fold.
- **Poot voor behoud** (gate 5, check 5): **197 goldens, 480 cases, 0 afwijkingen** (was 134 en 339); floors naar
  197/480 in `aba670949`. Geweigerd per eerste vorm: defaults-group 55, timeline-member 48, object-creation 27,
  allocation 21, list-family 16, if-then-else 11, fact-creation 10, day-kind 9, match 8, record 7, uniqueness 7,
  dimension-cell 6, function-def 3, time-window 2, en 1 elk dimension-fold, rule-status-check, start-point, unresolved.
  De zes die eerder als `temporal-total` telden, waren tellingen van een vereniging en spelen nu.

### De naad (ref/lib tegenover `247a61084`)

| bestand | regels | wat |
|---|---|---|
| `eval.ml` | +250 −157 | `boundaries`, `signal_closure`, `closure_over_open`, `value_verdict`; `held` → `open_read_boundary` met signaal; `atomic`; de arm van leeg/gevuld; de arm van de kwantorvouw; `keep`/`bkeep` en de gevuld-telling in `eval_function_call`; `run_rule` (drie armen, lege waarde na sluiting); `refuse_uncarried`; elke lezing via een lezer (`whole_period_characteristic`, `qualifier_timeline`, `ever_tuples` met `world`); `open_inputs` |
| `world.ml` | +43 −16 | `is_open` → `attr_open`, `char_open`; `read_timeline`, `char_periods`, `read_char_periods`; `timeline` naar boven |
| `case.ml` | +44 −25 | `check` vraagt de stand eerst |

De architectuur: één evaluator (geen kopie; de kwantorvouw en de vouwen gebruiken hun bestaande arm), `Value.t`
ongewijzigd (`value.ml`, clingo en de frontends niet geraakt), de eigenschap op twee plaatsen (`refuse_uncarried`,
`open_inputs`), een gewoon document ongewijzigd (gate 5, 7, 8, 13, 17 groen zonder gewijzigde verwachting, zie onder).

### Voorlopige besluiten

1. Een waarde die geen schrijver besliste, is open zolang een schrijver het lid open laat; zo ook een kenmerk in
   `characteristicsAbsent`; een kenmerk dat de case noemt, blijft gelden (B).
2. Monotonie gaat over invoerleden; een waarde op een afgeleid lid staat waar elke schrijver niets vult (B).
3. Een instance-ordered deel wordt gedragen; het reopen-mechanisme herstelt een tijdelijk vaststaand lege lezing zoals
   elders in een stratum (A).
4. Een regel met een waarde na een sluiting beslist; met een lege waarde na een sluiting laat zij haar doel open; een
   initialisatie evenzo (D).
5. Een lid van een kwantorvouw is een lid van een samengestelde voorwaarde over het bereik (implicatie bij `all`/`the`,
   conjunctie anders); de filters van alle leden eerst, de matrix alleen waar het filter voor een ander niet onwaar is
   (E).
6. De telling van een vereniging (ch. 3.4d) wordt gedragen; elk takfilter via `value_verdict` (E).
7. Een onbepaald filter in een waarde werpt `World.Open_read` met als lid "the filter of a fold" (E).
8. De lijst rechtstreekse lezingen is een test van de suite, geen eigen poot in een gate-script (C).

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: "ref carries the reading of RUNTIME-SEMANTICS ch. 1.5 for a rule, a consistency rule, a
condition without a fold and an expression without a fold or a timeline" wordt "ref carries the reading of
RUNTIME-SEMANTICS ch. 1.5 for a rule, a consistency rule, and a condition or an expression without a timeline, the
quantifier fold, the folds over a set and the count of an instance-set union among them". Getuigenkolom erbij:
"WV-1.5-direct-reads (every read of a member past its state, listed with its reason)".

### Voorstel voor `CLAUDE.md`

Geen: de alinea van gate 5 noemt de floors bij naam, niet bij getal.

### Open punten

1. Deel 2b: de stapel (defaults met doorval, initialisatie, opgave) en de creaties; zie de overdracht.
2. `scripts/check-readings.sh` is niet uitvoerbaar maar vraagt `bash` (`declare -A`); de vuistregel "uitvoerbaar
   betekent bash" van de werkafspraken klopt daar niet (`make gates` roept het met `bash` aan). Geen fout van de gate.
3. Deel 3: wat de observer van een afgebroken evaluatie en van een sluiting ziet; `--trace` weigert een strikt document
   nog (REF-014 `reading`).

### Gates, los gedraaid

Elk los, één zware tegelijk, op `aba670949`; logs `logs/2a-gate*.log` in de werkmap (gate 17 met `bash`: `2a-gate17-02`).

```
check-self-contained: PASS
check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 509 documents: 837 closure places, floor 827, 385 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1466 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1305 case file(s) validate)
check-oracle: PASS (ref suite 3603 worked examples, floor 489; 1203 cases replayed exactly, floor 427; preservation with complete knowledge: 197 goldens and 480 cases play, floors 197 and 480; 234 goldens refused per form (defaults-group 55, timeline-member 48, object-creation 27, allocation 21, list-family 16, if-then-else 11, fact-creation 10, day-kind 9, match 8, record 7, uniqueness 7, dimension-cell 6, function-def 3, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs)
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 34 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1161 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-readings: PASS (ref traced 1199 golden case files, and refused by name the trace of 4 case file(s) of 1 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 87 s)
```

### Landing van deel 2a (hoofdsessie, 2026-11-21)

Aanleiding van dit deel: een onafhankelijke beoordelaar las deel 1a in plaats van de eigenaar en oordeelde "schoon,
overdraagbaar naar clingo en Drools, onder voorwaarden"; de vier voorwaarden voor ref zijn A tot en met D. Nagelezen:
de standen op één plaats (`World.attr_open`, `char_open`), de grens met haar signaal (`open_read_boundary`,
`signal_closure`, `closure_over_open`, `value_verdict`), de kwantorvouw als samengestelde voorwaarde over het bereik, en
de toevoegingen aan ch. 1.5. Bij de landing erbij: `scripts/check-readings.sh` stopt onder een bash ouder dan 4 (de
lijst `TRACE_REFUSED` van deel 1b is een associatieve array; onder bash 3.2 liep het script zonder de lijst door; de
gate-set draait met bash 5, dus de eerdere runs waren geldig), en rij 48 van `spec/CONSTRUCTS.md`. `make gates` groen
op `fdc742de0` (675 s, 17 gates, red=0; de poot voor behoud: 197 goldens en 480 cases).

## Result (deel 2b)

*Bouwer deel 2b, 2026-11-21. Branch `card/open-values-in-ref`, op `ddf33bac0`. Opdracht: de defaults-stapel (A), de
creaties (B), `IfThenElse` in een waarde (C), de getuigen, de golden en de poot voor behoud (D).*

### Wat er is

| commit | wat |
|---|---|
| `8883b8995` | A: de drie uitkomsten van een schrijver op één plaats (`outcome_of`, `value_outcome`), gelezen door `run_rule` en de defaults-groep; de stapel van hoog naar laag, de initialisatie als sluiting, uitzonderingsgroepen en de ongeordende groep met `Left_open`; `open_inputs` telt een defaults-groep als schrijver; ch. 1.5 "A stack" en verder; tests WV-1.5-stack-* |
| `843d60f88` | B: creaties met de drie uitkomsten; het merkteken per onderwerp en relatie (`World.read_links`, `links_open`, `leave_links_open`) en één lezer in `role_linked_instances`; de leden van een gemaakt object; weigering `creation-cycle`; ch. 1.5 "A creation", "An object made"; tests WV-1.5-creation-*, WV-1.5-fact-creation; de lijst rechtstreekse lezingen kent de relatie |
| `8fadebe23` | C: de voorwaarde van een `IfThenElse` via `value_verdict`; ch. 1.5 "An IfThenElse in a value"; test WV-1.5-if-then-else |
| `67584ada4` | D: negen getuigen als fixtures; de golden `jutterswet-afleiden-uit-leeg` met een uitzondering boven een basisregel en twee cases; de rode case draagt hetzelfde document |
| `4dc61bb7e` | gate 5: floors van de poot voor behoud 281 goldens, 787 cases (eigen commit) |
| (slotcommit) | dit Result |

### Per principe: de zin in ch. 1.5, de test, de rode stand

Rood eerst: de testfile van elk deel gedraaid op de commit ervoor, in een losse worktree
(`logs/2b-red-A`, opgeruimd). Daar weigert ref elk document van de nieuwe tests met REF-014 (`form` `defaults-group`,
`object-creation`, `fact-creation`, `if-then-else`): elke rij FAIL (`ov_row` vangt de fout per rij). Omdat de rode stand
een weigering is, toont per principe een mutant van de nieuwe code dat de test onderscheidt
(`mutants_2b.py` in de werkmap, logs `2b-mutants-*.log`).

| principe | zin | test(s) | rood eerst |
|---|---|---|---|
| A1 de stapel, de eerste die beslist; voorrang geven | "A stack" | WV-1.5-stack-first-decides (5), -gives-way (3) | REF-014 op alle 44 A-rijen (`2b-red-A-01.log`); mutant "open default geeft voorrang" 55 FAIL |
| A2 alles geeft voorrang, geen initialisatie: vaststaand leeg | "A stack" | WV-1.5-stack-settled-leeg (2) | REF-014 |
| A3 initialisatie sluit over de stapel | "The initialisation of a stack" | WV-1.5-stack-initialisation (8) | REF-014; mutant "geen sluiting" 6 FAIL |
| A4 de opgave | "An opgave" (zie voorlopig besluit 1) | WV-1.5-stack-opgave (4) | REF-014 |
| A5 stapel op een kenmerk | "A stack on a characteristic" | WV-1.5-stack-characteristic (5) | REF-014 |
| A6 uitzonderingsgroepen, ongeordende groep, typeketen | "Exception groups and a declared-unordered group" | WV-1.5-stack-exception-groups (5), -unordered (4), -type-chain (7) | REF-014; mutant "open lid valt weg" 44 FAIL, "groep open schrijft niets" 37 FAIL |
| A7 tijdlijndoel blijft geweigerd | "Exception groups …", slotzin | WV-1.5-stack-timeline-refused | rood: de oude code noemt `defaults-group` |
| B1 creatie maakt waar haar voorwaarde voor haarzelf waar is | "A creation" | WV-1.5-creation-condition (2), WV-1.5-fact-creation (links, links-nothing) | REF-014 `object-creation`/`fact-creation` (`2b-red-B-01.log`) |
| B2 onbepaalde voorwaarde laat open wat zij zou maken | "A creation" | WV-1.5-creation-open (2), WV-1.5-fact-creation (open) | REF-014; mutant "geen merkteken" 3 FAIL, "verre kant niet gemerkt" 1 FAIL |
| B3 leden van een gemaakt object | "An object made" | WV-1.5-creation-member (3) | REF-014; mutant "open lid wordt leeg" 3 FAIL, "merktekens niet bewaard" 3 FAIL |
| B4 `open_inputs` en de creatie | "An object made" (zie voorlopig besluit 2) | WV-1.5-creation-member (V9) | REF-014 |
| B5 creatie op een cyclus geweigerd | "An object made", slotzin | WV-1.5-creation-cycle-refused | rood: de oude code noemt `object-creation` |
| C `IfThenElse` | "An IfThenElse in a value" | WV-1.5-if-then-else (4), WV-1.5-refused-not-the-if-then-else | REF-014 `if-then-else` (`2b-red-C-01.log`); mutant "geen signaal" 4 FAIL |

Gewijzigde bestaande tests (de taak verandert hun gedrag): WV-1.5-refused-if-then-else is
WV-1.5-refused-not-the-if-then-else geworden, en WV-1.5-refused-before-evaluation gebruikt nu de eigen totaalsom
(`temporal-total`) als voorbeeld. De monotonie-test telt een defaults-groep als schrijver en noemt vijf sluitingen
erbij (de initialisatie van de stapel twee keer, de toets op leeg in een `IfThenElse` drie keer). WV-1.5-direct-reads
leest ook `World.links` en `.left_side`. De suite telt 3873 voorbeelden (was 3603).

### Wat gedragen wordt en wat geweigerd

Gedragen in een strikt document, bovenop deel 1a–2a: de defaults-groep op een doel zonder tijdlijn (uitzonderingsgroepen,
de ongeordende groep, de samengestelde stapel van ch. 7.10, de initialisatie en de opgave), de object- en de feitcreatie
buiten een cyclus, en `IfThenElse`. `refuse_uncarried` noemt nog, in deze volgorde: `reading` (alleen bij `--trace`),
`allocation`, `creation-cycle`, `uniqueness`, `start-point`, `day-kind`, `function-def`, `timeline-member`, en per regel,
consistentie, default en creatie `dimension-cell`, `temporal-total`, `time-proportional-part`, `dimension-fold`,
`builtin-filter`, `day-kind`, `variable-reference`, `unresolved`, `temporal-read`, `match`, `record`, `list-family`,
`invoke`, `whole-period`, `time-window`, `rule-status-check`.

### B.2: het merkteken van een relatie

Gebouwd zoals de opdracht vroeg, zonder de sluiting van het voorstel. Een creatie met een onbepaalde voorwaarde laat
het paar (onderwerp, sleutel van de relatie) open in haar uitkomst (`World.leave_links_open`); `World.opened` houdt het
naast de open leden (de sleutel bevat een teken dat geen lidnaam spelt). Elke lezing van een relatie tijdens een
evaluatie gaat via `role_linked_instances`, dat `World.read_links` vraagt; een open relatie werpt `World.Open_read`.
De navigatiecheck, de rolcheck en het bereik van een kwantorvouw lezen de relatie binnen een grens (onbepaald); een
vouw, een telling en een genavigeerde lezing in een waarde laten de waarde open. Een objectcreatie merkt de relatie
waarover zij haar object aan het onderwerp zou koppelen; een feitcreatie merkt beide kanten: het anker, en elk doel dat
de keten bereikt (gaat de keten zelf over een open relatie, elk object van het doeltype). De fixpuntlus ziet het
merkteken als een verandering (`over_subjects`, `reopened`), dus een lezer wordt opnieuw geëvalueerd. Verwachting van
de proef die verandert: `fact-creation` case3 (gemeld niet genoemd) was hulptal 0 en is open; `object-creation` case3
(ervaring leeg) was ontvangen taxatie leeg en is open.

### De getuigen

`defaults-stack` (8 cases), `defaults-characteristic` (6), `defaults-subtype` (7), `settled-empty-stack` (6),
`input-above-computation` (7, waarvan case7 eigen werk), `object-creation` (3), `fact-creation` (4),
`filled-test-not-closure` (4), `composite-filled-test` (5) in `ref/test/fixtures/strict-empty/`, omgezet met
`draft2a/witness_cases.py`. 24 verwachtingen "geen waarde" van de proef zijn open, elk met de hand nagerekend en omgezet
met `draft2b/open_by_hand.py` (de lijst met de reden per groep staat in dat script): de stapel open (betwist onbepaald,
een open waarde, de invoer naast een open stapel), de premies naast een open kenmerk, de eigen uitzondering van de
Strandjutter, de uitbetaling over een open bergloon, de toeslagen achter een gevuld-toets over een open waarde, de
berekende bergloon over een open schade, en de twee creaties hierboven. `input-above-computation` speelt met
`closed.json` (de opgave gesloten door de sessie; voorlopig besluit 1); case7 zonder die sluiting geeft 25 (de
initialisatie sluit over de open opgave).

### De golden

`jutterswet-afleiden-uit-leeg` krijgt de premie: "Regel premie: 10" met daarboven "Afwijking van premie: betwiste premie:
0 indien hij betwist is", `betwist` een invoerkenmerk. case5-uitzondering-open: betwist niet genoemd → premie open;
case6-uitzondering-staat-niet: V1 betwist in `characteristicsAbsent` → 10, V2 betwist → 0. `ir.json` via de frontend
(`make ir`), gate 10 herschrijft hem byte-identiek; de rode case `jutterswet-kenmerk-wel-en-niet` draagt dezelfde
`ir.json`. Elke case faalt in `--noop`. De pins van gate 6, 12 en 17 noemen de golden en blijven kloppen (zie onder).

### De poot voor behoud

Gate 5, check 5: **281 goldens, 787 cases, 0 afwijkingen** (was 197 en 480); floors naar 281/787 in `4dc61bb7e`.
Geweigerd per eerste vorm: timeline-member 57, allocation 21, list-family 16, day-kind 9, dimension-cell 8, match 8,
uniqueness 8, record 7, creation-cycle 6, function-def 4, time-window 2, en 1 elk dimension-fold, rule-status-check,
start-point, unresolved. Na A alleen: 242/704; na B: 269/761.

### De naad (ref/lib tegenover `ddf33bac0`)

| bestand | regels | wat |
|---|---|---|
| `eval.ml` | +312 −179 | nieuw: `writer_outcome`, `outcome_of`, `value_outcome`, `map_outcome`, `left_open`; `chain_result.Left_open`; gewijzigd: `value_verdict` (`?what`), `refuse_uncarried` (laat stapel, creaties en `IfThenElse` los, loopt hun voorwaarden en waarden af, weigert `creation-cycle`), `role_linked_instances` (via `World.read_links`), `eval_expr_node` (de arm van `IfThenElse`), `judge_cond_node` (navigatiecheck, rolcheck, bereik van de kwantorvouw binnen een grens), `run_rule` (de beslissing via `outcome_of`), `run_fact_creation`, `run_object_creation`, `run_defaults_group` (oordeel per default, `own_outcome`, `closing`, `reconcile`), `open_inputs` (defaults-groep als schrijver) |
| `world.ml` | +24 −0 | nieuw: `links_open`, `read_links`, `leave_links_open`, `leave_open_again` |

De architectuur: één evaluator (geen kopie van `eval_expr` of `eval_cond`; de stapel en de creaties gebruiken de
bestaande armen); `Value.t` ongewijzigd (`value.ml`, clingo en de frontends niet geraakt); de eigenschap op twee plaatsen
(`refuse_uncarried`, `open_inputs`); de drie uitkomsten van een default en van een regel op één plaats (`outcome_of`,
`value_outcome`); een gewoon document geeft dezelfde uitkomsten (gate 5, 7, 8, 13 en 17 groen zonder gewijzigde
verwachting of pin, zie onder).

### Voorlopige besluiten

1. **De opgave is niet gesloten door haar herkomst.** De wire draagt de opgave alleen in `origin` (`inputAboveComputation`),
   en IR-SPEC ch. 2.13 zegt: "An origin never resolves anything and decides nothing: no executor reads it for
   evaluation, and a document whose `origin` fields are removed gives every executor the same answer"; gate 13 houdt
   dat vast (de ORIGIN-poot); en volgens de aanwijzing van de eigenaar (2026-11-07) staat RegelSpraak in de IR-SPEC
   alleen als herkomst. ref leest de herkomst daarom niet: de opgave leest als elke default over een invoerlid (open waar de
   case haar niet geeft; een sessie die haar volledig noemt laat de berekening beslissen; een initialisatie sluit over
   de open opgave, WV-1.5-stack-opgave). Twee wegen voor de eigenaar: (a) een veld op de declaratie dat een invoerlid
   volledig noemt (STRICT-EMPTY §9, beslispunt 3: `(volledig)`), door de frontend geschreven voor `opgegeven X`, gelezen
   in `open_inputs` — een eigen kaart (schema, verifier, frontend, IR-SPEC); of (b) de herkomst lezen: de runner en de
   testhelper zetten de attributen met `origin.form = inputAboveComputation` (via `Ir_v0.Loader.vocabularies`) in
   `closed`, met een uitzonderingszin in IR-SPEC ch. 2.13 en een uitzondering voor strikte goldens in de ORIGIN-poot
   van gate 13 (ongeveer twintig regels in ref).
2. **Een creatie telt in `open_inputs` niet als schrijver.** Op een object dat de case geeft, blijft een lid dat een
   creatie op haar eigen objecten toekent een invoerlid (open zonder waarde), zoals ch. 1.5 een invoerlid definieert en
   ch. 7.8 de waarde van een creatie als invoer van het gemaakte object (rang 2) leest. Op een gemaakt object is de
   zorg van de opdracht er niet: het staat niet in de invoerwereld, dus wat niemand toekent staat vast leeg
   (WV-1.5-creation-member, V9 en de schade van het gemaakte object).
3. **Een open relatie is een open lezing van de hele vorm**: een telling, een kwantorvouw en een rolcheck over een
   relatie die open is, zijn open, ook waar een link er al staat (een rolcheck "heeft een vondst" met een gegeven vondst
   is open). Veilig; een fijnere lezing (het bereik als de staande leden plus één onbepaald lid) is mogelijk.
4. **Een feitcreatie** merkt beide kanten; gaat haar keten over een open relatie, dan elk object van het doeltype.
5. **Een objectcreatie zonder koppelrelatie** merkt niets: geen lezing bereikt haar object over een relatie; een vorm
   over de populatie (consistentie) voegt bij een latere invulling alleen uitkomsten toe.
6. **Wat een creatie op haar gemaakte object open liet, blijft open** bij latere evaluaties van de creatie
   (`World.leave_open_again`): de leden worden eenmaal, bij de creatie, geschreven.
7. **Een kenmerktoekenning van een creatie** met een open lezing, of onwaar/leeg na een sluiting, laat het kenmerk op
   het gemaakte object open; waar na een sluiting zet het.
8. **De verzoening van groepen**: eerst een conflict (twee leden die verschillend beslissen staan vast), dan een open lid
   (open), dan niets (voorrang), anders de beslissing. Onder een open default wordt alleen de initialisatie geëvalueerd.
9. **Een creatie op een cyclus** herkent ref aan het lidmaatschap van een recursieve groep (op naam), niet aan de graaf.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: "ref carries the reading of RUNTIME-SEMANTICS ch. 1.5 for a rule, a consistency rule,
and a condition or an expression without a timeline, the quantifier fold, the folds over a set and the count of an
instance-set union among them" wordt "ref carries the reading of RUNTIME-SEMANTICS ch. 1.5 for a rule, a defaults group
on a target without a timeline (its exception groups, a declared-unordered group and the effective stack of ch. 7.10
among them), an object creation and a fact creation off a cycle, a consistency rule, and a condition or an expression
without a timeline, the quantifier fold, the folds over a set, the count of an instance-set union and the IfThenElse
among them; a creation on a cycle is refused by name (`creation-cycle`)". Getuigenkolom erbij: "WV-1.5-stack-*,
WV-1.5-creation-*, WV-1.5-fact-creation, WV-1.5-if-then-else; the witnesses of the stack and the creations under
`ref/test/fixtures/strict-empty/`; the golden `jutterswet-afleiden-uit-leeg` with six cases".

### Voorstel voor `CLAUDE.md`

Geen: de alinea van gate 5 noemt de floors bij naam.

### Open punten

1. De opgave (voorlopig besluit 1): een keuze van de eigenaar tussen een veld voor volledige invoer en de herkomst.
2. `backends/clingo/README.md` r. 509–513 zegt dat ref een strikt document weigert (REF-014); dat klopt sinds deel 1a
   niet meer. Van de kaart waarin clingo de lezing draagt.
3. Deel 3: de lezingen (ch. 5.12) van een default die open is, van een creatie met een onbepaalde voorwaarde en van een
   `IfThenElse` met een onbepaalde voorwaarde; de eindstand (ch. 5.12a) van een open relatie (het merkteken staat in
   `World.opened` onder de sleutel van de relatie).
4. De kwantorvouw, de telling en de rolcheck over een open relatie zijn als geheel open (voorlopig besluit 3).

### Gates, los gedraaid

Elk los, één zware tegelijk, logs `logs/2b-gate*.log` in de werkmap. Gate 1, 3, 5, 6, 7, 8, 12, 13, 15 (extra: de bron
van de golden groeide met een defaults-groep), 16 en 17 op `4dc61bb7e`; gate 2, 4 en 10 op dezelfde inhoud vóór de
commit `67584ada4` (daarna veranderde alleen `scripts/check-oracle.sh`); gate 17 met `opam exec -- bash`.

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 552 shape self-test row(s); 509 file(s) validate and round-trip, floor 192; 66964 object(s) in 1166 document(s) in the schema's key order, 2 in no subschema)
check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 509 documents: 837 closure places, floor 827, 387 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1468 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1307 case file(s) validate)
check-oracle: PASS (ref suite 3873 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 281 goldens and 787 cases play, floors 281 and 787; 150 goldens refused per form (timeline-member 57, allocation 21, list-family 16, day-kind 9, dimension-cell 8, match 8, uniqueness 8, record 7, creation-cycle 6, function-def 4, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs)
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 36 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1163 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-regelspraak: PASS — JUnit suite green (1704 tests, floor 173); 222 rgs golden(s) re-emit byte-identically (floor 80)
check-drools: PASS (played 384; auto-excluded 48, among them 1x DROOLS-370; DRL-text branch played 384; JUnit 1976 tests, floor 1573)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-graph: PASS (extra; route leg: the graph of the wire equals the graph of the source on all 222 goldens with a source.rgs, floor 98; 84 unit tests green, floor 23)
check-scenario: PASS — 48 scenario golden(s) (floor 7) regenerate 176 case file(s) (floor 31) and their ir.json byte-identically
check-readings: PASS (ref traced 1199 golden case files, and refused by name the trace of 6 case file(s) of 1 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 89 s)
```

### Landing van deel 2b (hoofdsessie, 2026-11-21)

Nagelezen: de drie uitkomsten van een schrijver op één plaats (`writer_outcome`, `outcome_of`, `value_outcome`), de
lezer van een relatie met haar merkteken (`World.read_links`, `links_open`), en de twee nieuwe cases van de golden, met
de hand nagerekend. Twee afwijkingen van de opdracht, die de hoofdsessie overneemt:

- **De opgave is niet gesloten door haar declaratie.** Het voorlopige besluit van de kaart vroeg dat, herkend aan de
  herkomst van de groep; IR-SPEC ch. 2.13 zegt dat geen executor de herkomst leest en gate 13 houdt dat vast. Een
  opgave die de case niet geeft is dus open, en de berekening eronder beslist niet, tenzij een initialisatie sluit.
  Dat een niet-gegeven opgave "er is geen opgave" betekent, komt met de kaart voor een volledig gegeven: een
  uitdrukkelijk veld op de declaratie, dat de frontend voor een opgave zet. Het besluit "een opgave is gesloten door
  haar declaratie" onder "Besluiten vóór de bouw" geldt in die vorm niet meer.
- **Een creatie telt in `open_inputs` niet als schrijver**: een lid op een object dat de case geeft, blijft invoer.

De zin in de README en het commentaar van clingo dat ref een strikt document weigert, is bijgewerkt; rij 48 van
`spec/CONSTRUCTS.md` ook. `make gates` groen op `73d6f05e2` (671 s, 17 gates, red=0; de poot voor behoud: 281 goldens en
787 cases).

## Samenvatting van de kaart

*Voor wie de kaart in `backlog/done/` leest; de Results per deel staan erboven, dat van deel 3 eronder.*

**Wat ref rekent.** Een document met `inferFromEmpty: false` rekent ref met drie standen (gevuld, vaststaand leeg,
open), zoals RUNTIME-SEMANTICS ch. 1.5 het vastlegt: de regel, de defaults-groep op een doel zonder tijdlijn (met
uitzonderingsgroepen, de ongeordende groep en de stapel van ch. 7.10), object- en feitcreatie buiten een cyclus, de
consistentieregel, en elke voorwaarde en uitdrukking zonder tijdlijn, de kwantorvouw, de vouwen over een verzameling,
de telling van een vereniging en `IfThenElse` daaronder. Eén evaluator: een open lezing werpt `World.Open_read` en
breekt de evaluatie af tot de grens van één oordeel of één waarde (`open_read_boundary`); het oordeel van een
voorwaarde is een paar (voor de vorm zelf, voor een ander) met de open lezing die het onbepaald liet; een lid van een
samengestelde voorwaarde of een matrix van een kwantorvouw dat gelezen wordt na een lid of filter dat voor een ander
niet vaststaat, wordt voorlopig gelezen (`judge_cond ~past`): zijn oordeel telt, een deponering vervalt, een fout laat
het lid onbepaald, een barrière van een omsluitende `Let` is een open lezing, en de deponeringen van een binder die
het als eerste leest wachten op de eerste gewone lezing; een sluiting binnen een waarde meldt zich bij haar grens met de lezing waarover zij sloot
(`signal_closure`); een schrijver heeft drie uitkomsten (`outcome_of`, `value_outcome`). De lezing aan het fixpunt (ch. 5.12) en de eindstand (ch. 5.12a) zijn gedefinieerd voor open waarden;
`runner --trace` leest elk strikt document.

**Wat ref weigert.** REF-014 met `form` = de slug van de eerste vorm uit de lijst aan het eind van ch. 1.5 die het
document bevat: `allocation`, `creation-cycle`, `uniqueness`, `start-point`, `day-kind`, `function-def`,
`timeline-member`, `dimension-cell`, `dimension-fold`, `time-proportional-part`, `temporal-total`, `builtin-filter`,
`variable-reference`, `unresolved`, `temporal-read`, `match`, `record`, `list-family`, `invoke`, `whole-period`,
`time-window`, `rule-status-check` (`Eval.uncarried_forms`, gelijk gehouden door WV-1.5-uncarried-forms).

**De twee eisen en hun poten.** Behoud bij volledige kennis: gate 5 check 5, elke golden met de eigenschap in een
kopie en alle invoer gesloten speelt haar eigen cases (280 goldens, 784 cases), en WV-1.5-preserved. Monotonie: gate 5
check 6, per golden die ref strikt draagt, per case en per gegeven invoer houdt de eindstand zonder dat gegeven elke
uitkomst in de eindstand met het gegeven (790 cases, 2255 runs vergeleken; 443 met een sluiting apart, 71 waarin het
weggelaten lid geen invoerlid is; `MONOTONE_GAPS` leeg), en WV-1.5-monotone.

**De naad over alle delen** (`ref/` tegenover `fadbce3a3`, `git diff --numstat`):

| bestand | regels | wat |
|---|---|---|
| `eval.ml` | +1011 −394 | open lezing en grens, oordeel als paar met open lezing, `count_verdict`, sluitingssignaal met de gesloten lezing, drie uitkomsten van een schrijver; armen van de voorwaarden, de kwantorvouw, de vouwen, `IfThenElse`, de stapel, de creaties; het voorlopig lezen (`judge_cond ~past`); `open_inputs` (de ene lezer van de eigenschap) en `refuse_uncarried` met `uncarried_forms`; de teller van sluitingen per evaluatie |
| `world.ml` | +157 −19 | `Open_read`, de stand van een lid (`attr_open`, `char_open`, `links_open`) en de lezers erlangs; `opened`, `open_input`, `stated_absent`; de uitkomst van een evaluatie met `opened` en `closure` |
| `case.ml` | +111 −34 | `characteristicsAbsent`, de verwachtingen `attributeOpen`/`characteristicOpen`, de eindcontrole via de stand |
| `reading.ml` | +181 −78 | het oordeel per knoop, de open lezing, de status `OPEN`, de stapel onder een open default |
| `end_state.ml` | +17 −7 | open leden en open relaties |
| `bin/runner.ml` | +43 −19 | `--closed-inputs`, `--trace` voor een strikt document, `--end-state` |

`value.ml` is niet geraakt (`Value.t` zonder nieuwe constructor), en de eigenschap wordt gelezen in `refuse_uncarried`
en `open_inputs`, nergens anders. Een gewoon document gaf in elk deel byte voor byte dezelfde uitkomsten, lezingen en
eindstand (gate 5, 7, 8, 13 en 17 zonder gewijzigde verwachting; in deel 3 en 3b ook de 1199 sporen van
`runner --trace` over de gewone goldens, byte voor byte tegen de binary van `dbb53b038` en van `95be947d4`).

**Wat de kaarten van clingo en Drools moeten naspelen.** De leesregel van de samengestelde voorwaarde en de kwantorvouw
(lezen tot het oordeel voor een ander vaststaat, en voorlopig lezen voorbij een lid of filter dat voor een ander niet
vaststaat, ch. 1.5); de eindstand met `attribute … open`, `characteristic … open`
en de link met `open` (ch. 5.12a); de lezing met `undetermined open <object> "<lid>"`, `true closure`/`false closure`,
de status `OPEN` en de stapel onder een open default (ch. 5.12, "Open values"); de slugs en hun volgorde (ch. 1.5,
"Forms without the reading"). De overdracht (`werk/open-values-in-ref/overdracht.md`, sectie "voor de bouwers van
clingo en Drools") zegt waar het in ref staat.

## Result (deel 3)

*Bouwer deel 3, 2026-11-21. Branch `card/open-values-in-ref`, op `dbb53b038`. Opdracht: de eindstand (A), de lezing aan
het fixpunt (B), gate 17 (C), monotonie als poot in gate 5 (D), de lijst van vormen zonder lezing (E).*

### Wat er is

| commit | wat |
|---|---|
| `5b48f31bc` | A en B: de observer krijgt het oordeel; de lezing toont waarheid, `closure`, `undetermined open …`; status `OPEN`; de stapel onder een open default; de eindstand met open leden en relaties; `runner --trace` weigert geen strikt document; ch. 5.12 "Open values" en ch. 5.12a; tests WV-5.12-afleiden-uit-leeg, WV-5.12-open-*, WV-5.12a-afleiden-uit-leeg, WV-5.12a-open-link |
| `1289079bc` | C: gate 17, `TRACE_REFUSED` leeg (eigen commit) |
| `df9d9a17d` | D in ref: de uitkomst van een evaluatie onthoudt een sluiting; `runner --end-state`; een creatie met versies op een cyclus wordt geweigerd (bijvangst van de poot) met een testrij |
| `a79adc07e` | D in gate 5: check 6, monotonie; floors van check 5 naar 280/784 (eigen commit) |
| `01cd365f9` | E: de tabel in ch. 1.5, `Eval.uncarried_forms` met voorrang naar de lijst, WV-1.5-uncarried-forms; de verwijzing in IR-SPEC ch. 2 |
| (slotcommit) | deze samenvatting en dit Result |

Gewijzigd gedrag van bestaande tests (de taak verandert het): WV-2-infer-from-empty verwachtte dat `--trace` het fixture
weigert (REF-014 `reading`) en dat `refuse_uncarried ~reading:true` weigert; nu leest `--trace` het fixture
(`runner-trace-reads`: V3 `undetermined open "V3" "vondstwaarde"`, `OPEN`, de open leden in de eindstand) en bestaat de
parameter `~reading` niet meer. De reden bij `end_state.ml` in WV-1.5-direct-reads zegt nu dat de eindstand eerst de
stand vraagt.

### A. De eindstand

Regelvorm: een open lid staat met `open` op de plaats van zijn waarde, `attribute <object> "<lid>" open` en
`characteristic <object> "<kenmerk>" open`; een relatie die op een object open is als link met `open` op de plaats van
de objecten die een creatie er nog aan kan koppelen, de rollen in de volgorde van het feittype:
`link "<feittype>" "<rol>" <object> "<rol>" open`. Een attribuut zonder regel is vaststaand leeg, een kenmerk zonder
regel staat vast niet. De plaats in de volgorde is die van het sorteren, zoals elke regel. Een lid met een waarde die
geen schrijver besliste en dat open is (invoer naast een open schrijver) staat als `open`, niet met die waarde.

| eis | getuige | gate | rood eerst |
|---|---|---|---|
| open leden | WV-5.12a-afleiden-uit-leeg (case4 heel, case2 heel) | 5 | op `dbb53b038` weigert `--trace` de golden (REF-014 `reading`, `logs/3-red-trace-01.log`); mutant "geen open lid" 3 FAIL |
| open relatie | WV-5.12a-open-link (fixture `object-creation` case3) | 5 | mutant "geen open relatie" 1 FAIL |
| gewoon document ongewijzigd | 1199 sporen byte-gelijk; gate 17 | 17 | — |

### B. De lezing aan het fixpunt

Woorden: `true` en `false` blijven; `undetermined` erbij voor onbepaald, en `closure` na de waarheid voor de vorm zelf
waar die voor een ander onbepaald is. Na `undetermined` staat de open lezing: `open <object> "<lid>"` (een relatie bij
haar rol). Status `OPEN` erbij: de vorm laat haar doel open (regel, default, creatie), of de bewering van een
consistentieregel is onbepaald. `HOLDS` en `FAILS` volgen de drie uitkomsten: `HOLDS` waar de vorm beslist (ook met
een lege waarde), `FAILS` waar zij voor een ander vaststaand niets vult. Voorbeeld (golden, case2, regel 0):

```
/rules/rules/0/versions/0 "V1" 0 Comparison undetermined open "V1" "taxatie"
/rules/rules/0/versions/0 "V1" 1 AttributeReference undetermined open "V1" "taxatie"
/rules/rules/0/versions/0 "V1" OPEN
```

| punt | zin in ch. 5.12 | test | rood eerst |
|---|---|---|---|
| 1 oordeel voor de vorm zelf, `closure` | "Open values", eerste punt | WV-5.12-afleiden-uit-leeg (ongetaxeerd `true closure`), WV-5.12-open-composite | REF-014 op `dbb53b038`; mutant "closure niet getoond" 1 FAIL |
| 2 afgebroken knoop is onbepaald en noemt het lid | tweede en derde punt | WV-5.12-afleiden-uit-leeg, -open-path, -open-fold-over-a-set | mutant "afgebroken knoop als ended" 10 FAIL |
| welke open lezing (atomair, samengesteld: eerste onbepaalde lid, kwantorvouw: filter vóór matrix) | derde punt | WV-5.12-open-composite (eerste van twee), -open-quantifier-fold | mutant "laatste onbepaalde lid" 1 FAIL |
| 3 leden tot het oordeel voor een ander vaststaat | alinea na de punten | WV-5.12-open-composite ("opvallend": lid 1 onbepaald, lid 2 waar, lid 3 niet bereikt) | — (de lus van de sessie zelf) |
| 4 status `OPEN`; stapel onder een open default | alinea na de punten; tabel van de status | WV-5.12-open-stack, -open-creation, WV-5.12-open-status (over alle strikte fixtures en de golden: `OPEN` precies waar de sessie iets open liet) | mutanten "open default geeft voorrang" 3 FAIL, "open waarde houdt" 57 FAIL |
| 5 de observer geeft een oordeel; geen knoop met een uitkomst die er niet is | eerste zin van "Open values" | alle bovenstaande; geen tweede wandeling | — |
| 6 gewoon document byte-gelijk | slotzin | gate 17 (1205 sporen, 0 afwijkingen), 1199 sporen tegen de oude binary | — |

Logs: `logs/3-mutants-AB-01.log`, `-02.log` in de werkmap.

### C. Gate 17

`TRACE_REFUSED` is leeg (de lijst en haar controles blijven, zoals `READINGS_ERROR_KNOWN`); ref traceert 1205
case-bestanden, de zes van `jutterswet-afleiden-uit-leeg` erbij; de backend sluit de golden uit met `EXCLUDE
DROOLS-370`. De versiecontrole op bash staat er nog. Het script kent de status `OPEN` en de nieuwe woorden niet: alleen
de golden die de backend uitsluit draagt ze, en die wordt niet vergeleken (open punt 3).

### D. Monotonie als poot

`runner --end-state <ir> <case>`: de eindstand (regels na `state `) en per evaluatie aan het fixpunt die een sluiting
op een open waarde toepaste een regel `closure <vorm> <object>`; bij een fout van de evaluatie `closures <n>`, het
aantal sluitingen ervoor. Een sluiting is: het signaal van de grens (de toets op leeg en gevuld, de gevuld-telling, een
filter op gevuld, een `IfThenElse` die een sluiting besliste), een initialisatieregel aan de beurt naast een schrijver
die haar doel open laat (welke uitkomst zij ook heeft: ook haar deposito's rusten op de sluiting), en de initialisatie
van een stapel die onder een open default beslist. Gesloten invoer komt in de poot niet voor.

Check 6 van gate 5: per golden die check 5 draagt en per golden met de eigenschap, per case en per invoergegeven
(attribuutwaarde, kenmerk in `characteristics` of in `characteristicsAbsent`) de case zonder dat gegeven tegenover de
case. **789 cases; 2254 runs vergeleken (floor 2254); 443 met een sluiting apart (floor 443); 71 waarin het weggelaten
lid geen invoerlid is (een waarde op een afgeleid lid, de uitzondering van ch. 1.5), apart; 8 gaten van de norm bij
naam (`MONOTONE_GAPS`, stale-failing); 14 eigen rijen van de vergelijking; rode rij `MONOTONE_MUTANT`: de golden case1
zonder de taxatie van V1, vergeleken alsof er geen sluiting was, verliest 1 uitkomst (ongetaxeerd).** Mutant van ref
"het signaal telt niet": de poot faalt met 73 meldingen, elk bij naam (`logs/3-mutant-gate5-01.log`). De toewijzing is grof: een run
met een sluiting valt als geheel weg; van die 443 verschillen er 114 van de case (gemeten; de 25 die ik las rusten op een
gevuld-telling, een toets op leeg of een initialisatie); een fijnere toewijzing (welke leden rusten op de sluiting) vraagt
de afhankelijkheidsgraaf per document en is niet gebouwd.

Wat de poot vond:

1. **Fout in ref, hersteld**: een creatie met twee versies in een recursieve groep (`jutterswet-opvolgerversies`) werd
   niet als `creation-cycle` geweigerd: een versie heet `<naam> #<k>`, de groep noemt de vorm. De weigering vergelijkt
   nu de vorm (`Ir_v0.Checks.strip_version`). Test: WV-1.5-creation-cycle-refused, rij "a creation with two versions";
   rood met de oude vergelijking (`logs/3-red-cycle-01.log`). Gevolg: de poot voor behoud draagt 280 goldens en 784
   cases (was 281/787); de floors zijn in dezelfde commit verlaagd (een bewuste verwijdering, geen verzwakking).
2. **Gat in de norm** (8 runs, `MONOTONE_GAPS`): ch. 1.5 laat een samengestelde voorwaarde en een kwantorvouw voorbij een
   onbepaald lid lezen ("a fault or a §9.5 fact of that member can arise where ch. 5.10 would have stopped"), en "The two
   requirements" noemt een §9.5-feit een uitkomst zonder die uitzondering. Voorbeeld: `jutterswet-keurgemiddelde` case3
   zonder de keurpunten van W2: de vouw "inzetbaar" leest W3 voorbij de onbepaalde W2 en deelt door 0 keuringen, EF-005
   op P1; met de keurpunten (50) is W2 onwaar, stopt de vouw en is er geen feit. Hetzelfde met een fout: in
   `jutterswet-werfkeur` case1 zonder de lengte van S1 leest de matrix de rekendatum en faalt met EF-011; in
   `context-value-behind-a-failing-condition` case1 faalt de case zelf al zo. Twee wegen: de uitzondering in "The two
   requirements" benoemen (een fout of feit van een lid dat voorbij een onbepaald lid gelezen wordt), of een deposito en
   een fout na een onbepaald lid binnen dezelfde samengestelde voorwaarde niet laten staan. Een normkeuze; de poot houdt
   de acht bij naam tot die keuze er is.

### E. De vormen zonder lezing

De tabel staat aan het eind van ch. 1.5 met 22 slugs en hun vorm. Waar een document er meer bevat, noemt de executor de
eerste van de lijst (niet de eerste die zijn wandeling tegenkomt): één zin, voor drie executors na te spelen. ref:
`Eval.uncarried_forms`; `refuse_uncarried` verzamelt wat het document bevat en weigert met de eerste van de lijst; een
slug die de lijst niet kent is een fout van ref (`failwith`). Test WV-1.5-uncarried-forms: de tabel uit
`spec/RUNTIME-SEMANTICS.md` gelezen is de lijst, slug voor slug en in volgorde; de namen in `refuse_uncarried` zijn de
lijst als verzameling; een document met een periodevenster in regel 0 en een lijstliteraal in regel 1 geeft
`list-family`. Mutanten: een rij uit de tabel 1 FAIL, twee slugs omgewisseld 1 FAIL, "de eerste die de wandeling
tegenkomt" 1 FAIL (`logs/3-mutants-E-01.log`). Gevolg in gate 5: de telling per eerste vorm verschuift (day-kind 17,
timeline-member 49); het totaal geweigerd (151) niet.

### De naad van deel 3 (`ref/` tegenover `dbb53b038`)

| bestand | regels | wat |
|---|---|---|
| `eval.ml` | +148 −78 | `judgement` met `open_read` naar voren (vóór de observer); `undetermined` met de lezing; `judged`; `open_read_boundary` geeft `Ok`/`Error`; `value_verdict` werpt de echte open lezing; `judge_cond` geeft het oordeel aan de observer; de samengestelde voorwaarde en de kwantorvouw dragen hun eerste onbepaalde lid; `Member_verdict` met oordeel; `closures_applied` en de drie plaatsen die tellen; `over_subjects` geeft `closure` door; `refuse_uncarried` zonder `~reading`, met `uncarried_forms`, de cyclus per vorm |
| `reading.ml` | +181 −78 | `Judged`, `Broken`, `Open`; `read_on` via `outcome_of` en `value_outcome`; de stapel met drie uitkomsten en `closing`; de tekst van oordeel en open lezing |
| `world.ml` | +33 −14 | `outcome.closure`, `closures`, `open_beside_initialisation` |
| `end_state.ml` | +17 −7 | open leden en open relaties |
| `bin/runner.ml` | +33 −15 | `--trace` zonder weigering, `--end-state` |

De architectuur: één evaluator (de lezing is de evaluatie van de sessie door de observer; geen tweede wandeling: de
stapel in `reading.ml` volgde de sessie al en kreeg alleen de derde uitkomst); `Value.t` ongewijzigd; de eigenschap op
twee plaatsen (`refuse_uncarried`, `open_inputs`); een gewoon document byte-gelijk.

### Voorlopige besluiten

1. **Een onbepaalde knoop noemt één open lezing**: bij een atomaire voorwaarde en een afgebroken knoop het gelezen lid;
   bij een samengestelde voorwaarde die van haar eerste lid dat voor de vorm zelf onbepaald is; bij een kwantorvouw de
   relatie van het bereik, anders die van haar eerste onbepaalde lid (het filter waar dat onbepaald is, anders de
   matrix); een knoop die een onbepaalde voorwaarde in een waarde afbrak (`IfThenElse`, het filter van een vouw) die van
   die voorwaarde. Daarvoor draagt het oordeel de open lezing (`open_read`); `value_verdict` werpt haar in plaats van
   een omschrijving ("the filter of a fold").
2. **Een bijdrager van een kwantorvouw** schrijft alleen de waarheid voor de vorm zelf (`true`, `false`,
   `undetermined`), zonder open lezing en zonder `closure`: de regel blijft één token per waarde, zoals gate 17 hem leest.
3. **Een vouw die een open lezing afbrak heeft geen bijdragers**, zoals een vouw die faalde voordat ze vaststonden.
4. **De status volgt de uitkomst van de schrijver**, niet alleen de voorwaarde: een regel met een ware voorwaarde en een
   open waarde is `OPEN`. In een gewoon document is dat dezelfde status als voorheen.
5. **Onder een open default** is de initialisatie aan de beurt en is elke andere default `DISPLACED` (speculatief
   gelezen, zoals onder een default die beslist); zo ook de inhoud van een default wiens uitzonderingsgroepen open zijn.
6. **Een open lid met een waarde** (invoer naast een open schrijver) staat in de eindstand als `open`, zonder de waarde.
7. **`TRACE_REFUSED` blijft als lege lijst** met haar controles, zoals `READINGS_ERROR_KNOWN`; de bash-controle houdt
   daarmee haar reden.
8. **De poot voor monotonie is grof** (een run met een sluiting valt weg) en slaat een weggelaten lid dat zonder het
   gegeven niet open is over als geen invoerlid; een fout van de run zonder het gegeven telt als sluiting waar er een
   vóór de fout was.
9. **De voorrang van de slugs is de volgorde van de lijst**, niet de wandeling van de executor.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: "…, and refuses a document with another form by name: `REF-014` with the slug of the form
as `form` (`reading` for `--trace`). The reading of the other forms is open." wordt "…, and refuses a document with
another form by name: `REF-014` with the slug of the form as `form`, the first of the list of RUNTIME-SEMANTICS ch. 1.5
("Forms without the reading") the document holds. The reading at the fixpoint and the end state are defined for open
values (ch. 5.12 "Open values", ch. 5.12a), and `runner --trace` reads every document with the property. The reading of
the other forms is open." En "…; gate 5 holds preservation with complete knowledge over the corpus." wordt "…; gate 5
holds preservation with complete knowledge and monotonicity over the corpus." Getuigenkolom erbij: "WV-5.12-afleiden-
uit-leeg, WV-5.12-open-*, WV-5.12a-afleiden-uit-leeg, WV-5.12a-open-link, WV-1.5-uncarried-forms; the monotonicity leg
of gate 5".

### Voorstel voor `CLAUDE.md`

- Gate 5, na "(its red row).": "A monotonicity leg (the same section): per golden that leg carries or that carries the
  property, per case and per input the case gives, `runner --end-state` of the case without that input against the
  case: every outcome of the first stands in the second; a run in which a closure decided (a `closure` line, or a fault
  after one) and a run whose member left out is no input member are counted apart, every other difference fails by
  name unless `MONOTONE_GAPS` lists it (reason-carrying, stale-failing), the comparison passes rows of its own, and the
  run of `MONOTONE_MUTANT` compared all the same must lose an outcome." Floors erbij: `EXPECTED_MIN_MONOTONE_RUNS`,
  `EXPECTED_MIN_MONOTONE_CLOSURES`.
- Gate 17: "The trace reads every golden case file except those of a golden `TRACE_REFUSED` names with the code of its
  refusal (REF-014, form `reading`: a document with `inferFromEmpty: false`), which the backend must exclude as well;"
  wordt "The trace reads every golden case file, a document with `inferFromEmpty: false` included, except those of a
  golden `TRACE_REFUSED` names with the REF code of its refusal (empty), which the backend must exclude as well;".

### Open punten

1. **Het gat in de norm van D.2** (monotonie tegenover lezen voorbij een onbepaald lid): een keuze voor de eigenaar of
   Fable; tot dan staan de acht runs in `MONOTONE_GAPS`.
2. **De fijnere toewijzing van een sluiting** (welke leden erop rusten) is niet gebouwd; de poot laat 443 runs weg, waarvan
   114 van de case verschillen.
3. **Gate 17 leest de nieuwe woorden niet**: `displaced_nodes` en de telling van statusregels kennen `OPEN` niet, en een
   bijdrager met `undetermined` is één token (zo gekozen). Zodra Drools een strikt document leest, moet de rij-telling
   `OPEN` kennen; nu leest de gate de golden niet (`EXCLUDE DROOLS-370`).
4. **clingo en Drools** weigeren een strikt document nog als geheel (`form` `document`); de lijst van ch. 1.5 is voor
   hun kaarten.

### Gates, los gedraaid

Elk los, één zware tegelijk (6, 12, 13, 17, 8 achter elkaar in de achtergrond), op `01cd365f9`; daarna veranderde
alleen tekst (de rij van `OPEN` in ch. 5.12, deze kaart). Logs `logs/3-final-gate*.log` in de werkmap; gate 17 met
`opam exec -- bash`.

```
check-self-contained: PASS
check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 509 documents: 837 closure places, floor 827, 387 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1468 pointers resolve)
check-case-schema: PASS (schema valid; 146 self-test row(s); 1307 case file(s) validate)
check-oracle: PASS (ref suite 3896 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; 151 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cell 8, match 8, uniqueness 8, creation-cycle 7, record 7, function-def 4, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs; monotonicity: 789 cases, 2254 runs without one input compared, floor 2254, 443 with a closure apart, floor 443, 71 of no input member; 8 listed gap(s) of ch. 1.5; 14 comparison rows; red row: jutterswet-afleiden-uit-leeg/case1-alles-gegeven.json|attribute|V1|taxatie loses 1 outcome(s))
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 36 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1163 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-drools: PASS (played 384; auto-excluded 48, among them 1x DROOLS-370; DRL-text branch played 384; JUnit 1976 tests, floor 1573)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-readings: PASS (ref traced 1205 golden case files, and refused by name the trace of 0 case file(s) of 0 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 88 s)
```

## Result (deel 3b)

*Bouwer deel 3b, 2026-11-21. Branch `card/open-values-in-ref`, op `95be947d4`. Opdracht: wat een lid deponeert of aan
fout geeft dat voorbij een onbepaald lid gelezen wordt (open punt 1 van deel 3, D.2). Besluit van de hoofdsessie
(voorlopig, ter bevestiging door de eigenaar): zo'n lid is voorlopig.*

| commit | wat |
|---|---|
| `7e73ecd54` | ref: een lid voorbij een lid dat voor een ander niet vaststaat wordt voorlopig gelezen; een sluiting draagt de lezing waarover zij sloot; ch. 1.5 en 5.12; tests |
| `d7a2aa833` | gate 5: `MONOTONE_GAPS` leeg, `EXPECTED_MIN_MONOTONE_RUNS` 2254 → 2255 (eigen commit) |
| `22d56b297` | dit Result en de samenvatting |
| `350112c30` | ref: de barrière van een `Let` die uit een voorlopig lid komt, is een open lezing; ch. 1.5 en 5.12; WV-1.5-past-undetermined-barrier; in "A quantifier fold" de waarden van V1 in het voorbeeld van het filter |
| `498d8f329` | het Result bijgewerkt voor de barrière |
| `09c33442f` | ref: de deponeringen van de waarde van een binder ontstaan bij de eerste lezing die niet voorlopig is; ch. 1.5; WV-1.5-past-undetermined-binder |
| (slotcommit) | het Result bijgewerkt voor de binder |

### De regel (ch. 1.5, "The judgement of a condition" en "A quantifier fold")

"Whether ch. 5.10 would read a member that comes after one undetermined for another form (below) does not stand, so
that member is read provisionally: its judgement counts, what it deposits in the §9.5 channel is dropped, and a fault of
it does not end the evaluation but leaves the member undetermined, for the form itself and for another. A member after
members that all stand for another is read as ch. 5.10 reads it." Bij de kwantorvouw: "A matrix is read provisionally
[…] where a member before it is undetermined for another form, and also where a filter is: every filter is read before
every matrix, and whether ch. 3.9a reads a matrix depends on its own filter and, through the members it excludes, on
every other." In "For the form itself and for another" is de zin "A member read after the form's own judgement stands
deposits and faults as any member read" vervangen door: zo'n lid komt na een lid dat voor een ander onbepaald is en wordt
voorlopig gelezen. Bij de regel staat ook: "Such a member that reads a leeg binder of a Let around it (ch. 10.6)
makes an open read instead of the Let's barrier: whether the Let is leeg does not stand, so the value it stands in is
open." En: "A binder such a member reads first is computed there, once, and what the computation deposits arises at
the first read of the binder that is not provisional, and not at all where none comes; a fault of the computation
leaves the member undetermined and arises at that read, and an open read in it is an open read at every read of the
binder." "The two requirements" kreeg niets: de uitzondering bestaat niet.

In ref: `judge_cond ?past` (één plaats, binnen de observer): met `past` = de open lezing van het eerste eerdere lid
(of filter) dat voor een ander niet vaststaat, leest het lid met `~err:ignore` en maakt het van een `Value.Fault`
`undetermined past`, en van een `Let_barrier` die het lid verlaat `World.Open_read past`, die de dichtstbijzijnde grens
vangt. De barrière draagt daarvoor de identiteit van haar `Let`-evaluatie (`Let_barrier of int`) in plaats van een
lokale exceptie; een `Let` vangt alleen de hare. De `Let` rekent de waarde van haar binder één keer, met haar
deponeringen ernaast (`memo`: resultaat of exceptie, en de deponeringen); de eerste lezing die niet voorlopig is geeft
ze door aan de `err` van de `Let`, een bewaarde exceptie komt bij elke lezing terug. Of een lezing voorlopig is, draagt
de binding: `Later of { read : provisional:bool -> Value.t; provisional : bool }`, en `judge_cond ?past` zet
`provisional` op de binders in `env.locals` van het lid (langs de omgeving, geen globale toestand). De samengestelde
voorwaarde geeft `first_other` mee, de kwantorvouw het eerste filter dat voor een
ander niet vaststaat (`filter_past`), anders `first_other`. Geen tweede evaluatie: het lid wordt één keer gelezen.

### De tests en hun rode stand

Rood op `95be947d4` met de nieuwe testfile (losse worktree `logs/3b-base`, opgeruimd; de nieuwe blokken in een handler zodat een
fout van de sessie per test zichtbaar is; `logs/3b-red-01.log`, 10 FAIL):

| test | wat | rood eerst |
|---|---|---|
| WV-1.5-past-undetermined-deposit | samengestelde voorwaarde: de deponering (EF-005) van het tweede lid na een onbepaald gewicht vervalt; het lid beslist; bij gewicht 20 staat het feit, bij 5 stopt de lezing | rij zonder gewicht: `got vrijstelling V1 EF-005`; WV-1.5-monotone `past-undetermined-deposit-stops without V1.gewicht (adds channel vrijstelling V1 EF-005)` |
| WV-1.5-past-undetermined-fault | samengestelde voorwaarde: de fout (EF-011, het Rekenjaar zonder rekendatum) van het tweede lid laat het onbepaald; het derde lid beslist (taxatie 50: staat niet) of niet (2000: open); bij gewicht 20 faalt de sessie; kwantorvouw-rij: de matrix van V1 onbepaald door de fout, V2 beslist | de sessie faalt met EF-011 (beide blokken) |
| WV-1.5-past-undetermined-fold | kwantorvouw: de deponering van V2 na een onbepaalde V1 vervalt; V2 beslist | `got gelijkmatig J1 EF-005` |
| WV-1.5-past-undetermined-filter | kwantorvouw: een filter dat voor een ander niet vaststaat maakt elke matrix voorlopig, het eigen filter en een later filter | rijen `own-open` en `later-open`: `got gelijkmatig J1 EF-005` |
| WV-1.5-past-undetermined-ordinary | in een document zonder de eigenschap verandert niets | groen op de oude code (een bewaker, geen nieuwe eis) |
| WV-5.12-open-past-undetermined | de lezing: het lid toont `undetermined open "V1" "gewicht"`, de knoop die faalde `fault EF-011`; rij (barrier): de `Let` en elke knoop op weg naar de grens `undetermined open "V1" "gewicht"`, de lezing van de binder `empty`, `OPEN` | de sessie faalt met EF-011; rij (barrier) rood op `22d56b297` |
| WV-1.5-past-undetermined-binder | toeslag "taxatie gedeeld door 0 plus 3", taxatie 50, "alle van": zonder gewicht 0 zonder EF-005, bij 20 met, bij 5 zonder; met "anders de toeslag" 3 met het feit, één keer (ook bij gewicht 5); toeslag "het Rekenjaar" met een derde lid: "anders 0" geeft 0, "anders de toeslag" faalt met EF-011 bij de gewone lezing | op `498d8f329` (`logs/3b-binder-red-01.log`, 2 FAIL): `past-undetermined-binder-open, channel (got uitkering V1 EF-005)` en WV-1.5-monotone `past-undetermined-binder-stops without V1.gewicht (adds channel uitkering V1 EF-005)`; de andere rijen zijn bewakers (de oude code gaf het feit ook één keer en de fout ook opnieuw) |
| WV-1.5-past-undetermined-barrier | de uitkering "daarbij geldt: de toeslag is zijn vindloon, 10 als ten minste één van: gewicht > 10, toeslag > 5; anders 0" bij taxatie 50 (vindloon vaststaand leeg): zonder gewicht open, bij 20 → 10, bij 5 vaststaand leeg; zonder de eigenschap zonder gewicht leeg | op `22d56b297` (`logs/3b-barrier-red-01.log`, 3 FAIL): `(no gewicht) (got leeg, want open)`, de lezing, WV-1.5-monotone `past-undetermined-barrier-decided without V1.gewicht (adds V1.uitkering leeg)` |

Gewijzigde bestaande tests (de taak verandert hun gedrag):

- WV-1.5-read-for-another: de rij `WV-1.5-read-for-another (EF-031)` verwachtte `vrijstelling V1 EF-031`; nu heet zij
  `WV-1.5-read-for-another (no fact)` en verwacht zij geen feit (het tweede lid wordt gelezen na een sluiting die voor
  een ander niet vaststaat); rood op de oude code. Het voorbeeld in ch. 1.5 zegt het zo.
- WV-1.5-monotone: de lijst `closures` kreeg één rij, `("past-undetermined-filter-later-not", "V1 gemerkt") → V1 gemerkt
  not`: een nieuw scenario met gesloten invoer, de benoemde uitzondering.

### De acht runs

Met de wijziging in ref meldde gate 5 alle acht als verouderd ("keeps every outcome now", en voor
`context-value-behind-a-failing-condition` case1 "runs now"; `logs/3b-gate05-01.log`), en niets anders. De lijst is
leeg, met haar controles. Die case draait nu met al haar invoer (actief open: het tweede lid faalt voorlopig, gemeld is
open) en levert één vergeleken run meer: 790 cases, 2255 runs (floor 2255), 443 met een sluiting apart (floor 443), 71
geen invoerlid, 0 gaten. De poot vond niets nieuws.

Gewoon document: de 1199 sporen van `runner --trace` over de gewone goldens byte voor byte gelijk aan die van de
binary van `95be947d4`, en de zes van de strikte golden ook (`logs/3b-traces-01.log`).

### De naad (`95be947d4..09c33442f`, `git diff --numstat`)

| bestand | regels | wat |
|---|---|---|
| `ref/lib/eval.ml` | +154 −80 | `judge_cond ?past` (een fout, een barrière van een omsluitende `Let`, de binders van het lid gemarkeerd); `Let_barrier of int`; de binder met `provisional` en haar bewaarde deponeringen; `first_other` in de samengestelde voorwaarde en de kwantorvouw, `filter_past`; `judged ~first_other`; de grens geeft de lezing van de eerste sluiting terug (`signal_closure read`, `closed : (string * string) option`), `atomic` zet haar in `open_read` van een sluiting, `value_verdict` geeft haar door |
| `ref/test/test_ref.ml` | +316 −5 | de acht tests hierboven, de wijziging van WV-1.5-read-for-another, de rij in `closures` |
| `spec/RUNTIME-SEMANTICS.md` | +85 −15 | ch. 1.5 (drie alinea's), ch. 5.12 ("Open values": de open lezing van een lid dat een fout onbepaald liet of een barrière afbrak; voorbeeld) |
| `scripts/check-oracle.sh` | +4 −13 | `MONOTONE_GAPS` leeg, floor |

De architectuur: één evaluator; `Value.t` ongewijzigd; geen tak op de eigenschap (in een document zonder de eigenschap
is geen lid onbepaald en wordt `past` nooit gezet); een gewoon document byte-gelijk.

### Voorlopige besluiten

1. **De kwantorvouw ruimer dan "een eerder lid"**: elke matrix is voorlopig waar een filter van het bereik voor een
   ander niet vaststaat, ook een later filter. Reden: het filter van het lid zelf beslist of de matrix gelezen wordt, en
   een later filter dat onwaar blijkt telt vooraf als uitgesloten en kan de lezing vóór deze matrix laten stoppen
   (`atLeast`, `atMost`, `exactly`); de rij `later-open` van WV-1.5-past-undetermined-filter toont het gat dat er
   anders bleef. Voor `all`, `the`, `none` en `atLeast` 1 kan een later filter de lezing niet eerder laten stoppen; daar is de regel
   ruimer dan nodig (een fout die bij elke invulling zou ontstaan, is dan onbepaald). Zo ook de regel zelf (het besluit):
   "een eerder lid dat voor een ander niet vaststaat" is een voldoende voorwaarde, geen precieze.
2. **De open lezing van een lid dat een fout onbepaald liet** is die van het eerste eerdere lid (of filter) dat voor een
   ander niet vaststaat. Is dat een sluiting, dan de lezing waarover zij sloot: daarvoor draagt een sluiting nu haar
   lezing (`open_read` van een oordeel dat voor de vorm zelf vaststaat en voor een ander niet). Geen nieuw woord in de
   lezing; de knoop die faalde en de knopen op weg naar het lid tonen `fault` met de code, zoals bij elke fout.
3. **Een sluiting binnen een lid dat daarna voorlopig faalt** telt in `closures_applied` mee (grof, zoals de poot al is:
   de run valt dan apart).
4. **De barrière van een omsluitende `Let` uit een voorlopig lid is een open lezing** (besluit van de hoofdsessie,
   voorlopig): niet "het lid onbepaald" zoals bij een fout, want dan zou een later lid de voorwaarde kunnen beslissen en
   de `Let` een waarde geven waar de gewone lezing bij een ingevuld gegeven de lege binder bereikt en de `Let` leeg is.
   De open lezing is `past`; de waarde waarin de `Let` staat is open. Een `Let` binnen het lid verandert niet.
5. **De deponeringen van de waarde van een binder ontstaan bij de eerste lezing die niet voorlopig is** (besluit van de
   hoofdsessie, voorlopig): de waarde wordt één keer berekend, ook bij een voorlopige eerste lezing; de deponeringen
   wachten bij de waarde en gaan bij de eerste gewone lezing in hun volgorde naar de `err` van de `Let`, of vervallen.
   Een fout van de berekening laat het lezende voorlopige lid onbepaald en ontstaat bij de eerste gewone lezing (de
   bewaarde exceptie, ook een open lezing, komt bij elke lezing terug). Keuze: de mark zit op de binding in de
   omgeving, gezet door `judge_cond ?past`.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, kolom van de stand, na "… the quantifier fold, the folds over a set, the count of an instance-set union and the
IfThenElse among them;" (bovenop de rij van deel 3): "a member of a composite, and a matrix of a quantifier fold, read
after a member or a filter undetermined for another form is read provisionally: its judgement counts, its deposits are
dropped, a fault leaves it undetermined, a read of a leeg binder of a Let around it is an open read, and the
deposits of a binder it computes first arise at the first read that is not provisional (ch. 1.5);".
Getuigenkolom erbij: "WV-1.5-past-undetermined-*,
WV-5.12-open-past-undetermined".

### Voorstel voor `CLAUDE.md`

Geen wijziging bovenop het voorstel van deel 3: `MONOTONE_GAPS` blijft er (leeg, reason-carrying, stale-failing).

### Open punten

1. Open punt 1 van deel 3 (het gat van D.2) is met dit deel gesloten; de punten 2–4 van deel 3 staan. Ander effect
   van een voorlopig lid dan oordeel, deponering, fout, barrière of binderwaarde: niet gevonden (wat een conditie
   verder raakt, is de teller van sluitingen, voorlopig besluit 3, en wat de observer van de lezing ziet).

### Gates, los gedraaid

Elk los en één tegelijk in de achtergrond, op `d7a2aa833` (daarna veranderde alleen deze kaart). Logs
`logs/3b-final-gate*.log` in de werkmap; gate 17 met `opam exec -- bash`.

```
check-self-contained: PASS
check-oracle: PASS (ref suite 3993 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; 151 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cell 8, match 8, uniqueness 8, creation-cycle 7, record 7, function-def 4, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs; monotonicity: 790 cases, 2255 runs without one input compared, floor 2255, 443 with a closure apart, floor 443, 71 of no input member; 0 listed gap(s) of ch. 1.5; 14 comparison rows; red row: jutterswet-afleiden-uit-leeg/case1-alles-gegeven.json|attribute|V1|taxatie loses 1 outcome(s))
check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 36 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
check-noop: PASS (1163 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
check-readings: PASS (ref traced 1205 golden case files, and refused by name the trace of 0 case file(s) of 0 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 87 s)
```

Na de barrière (`350112c30`) opnieuw los: de suite van ref (in gate 5), gate 1, 5 en 17 (logs
`logs/3b-barrier-gate*.log`); de gates 6, 7, 8 en 13 hierboven draaiden op `d7a2aa833`.

```
check-self-contained: PASS
check-oracle: PASS (ref suite 4006 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; 151 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cell 8, match 8, uniqueness 8, creation-cycle 7, record 7, function-def 4, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs; monotonicity: 790 cases, 2255 runs without one input compared, floor 2255, 443 with a closure apart, floor 443, 71 of no input member; 0 listed gap(s) of ch. 1.5; 14 comparison rows; red row: jutterswet-afleiden-uit-leeg/case1-alles-gegeven.json|attribute|V1|taxatie loses 1 outcome(s))
check-readings: PASS (ref traced 1205 golden case files, and refused by name the trace of 0 case file(s) of 0 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 88 s)
```

Na de binder (`09c33442f`) opnieuw los: de suite van ref (in gate 5), gate 1, 5 en 17 (logs
`logs/3b-binder-gate*.log`), en de 1199 sporen van `runner --trace` over de gewone goldens byte voor byte gelijk aan
die van de binary van `95be947d4`, de zes van de strikte golden ook (`logs/3b-binder-traces-01.log`).

```
check-self-contained: PASS
check-oracle: PASS (ref suite 4033 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; 151 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cell 8, match 8, uniqueness 8, creation-cycle 7, record 7, function-def 4, time-window 2, dimension-fold 1, rule-status-check 1, start-point 1, unresolved 1); 1 carrying the property already, played by check 3 (jutterswet-afleiden-uit-leeg); red row: jutterswet-leeg-deeltal misses 1 of 3 cases without --closed-inputs; monotonicity: 790 cases, 2255 runs without one input compared, floor 2255, 443 with a closure apart, floor 443, 71 of no input member; 0 listed gap(s) of ch. 1.5; 14 comparison rows; red row: jutterswet-afleiden-uit-leeg/case1-alles-gegeven.json|attribute|V1|taxatie loses 1 outcome(s))
check-readings: PASS (ref traced 1205 golden case files, and refused by name the trace of 0 case file(s) of 0 golden(s) (TRACE_REFUSED); on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of which 4427 unreached, 1016 with contributors, 1317 with a timeline, 842 with the object of a read and 618 of a displaced reading, 147 contributors of ref not reached; end states of 3044 objects (479 made) and 8501 members; all at or above their floors; deviations 0, end-state deviations 0; 14 comparison rows, 3 tripwire rows, 10 end-state rows; 88 s)
```

**Landing van deel 3 en 3b (hoofdsessie, 2026-11-21).** Nagelezen: de eindstand en de runner (`end_state.ml`,
`runner.ml --end-state`), de lezing met het oordeel als paar (`reading.ml`: `Judged`, `Broken`, de status `OPEN`, de
stapel onder een open default), de lijst `uncarried_forms`, de poot voor monotonie van gate 5 met haar methode, en van
deel 3b de regel in `judge_cond ?past` met de barrière van een `Let` en de deponeringen van een binder; de voorbeelden
van ch. 1.5 voor het voorlopig lezen zijn met de hand nagerekend. De floors van de poot voor behoud gingen van 281/787
naar 280/784 omdat een creatie met twee versies op een cyclus nu als vorm geweigerd wordt. De acht gaten die de poot
voor monotonie vond, zijn met deel 3b gedicht; twee verwante gaten die de bouwer daarbij meldde (de barrière, de binder)
zijn in dezelfde landing hersteld. Rij 48 van `spec/CONSTRUCTS.md` en de alinea's van gate 5 en 17 in `CLAUDE.md` zijn
gezet. `make gates` groen op `8d5b9d9ec` (673 s, 17 gates, red=0). Hiermee rekent de referentie-interpreter de lezing
van ch. 1.5 voor elke vorm buiten de lijst "Forms without the reading"; de kaart is af.

Voorlopige besluiten van de hoofdsessie in deze kaart, ter bevestiging door de eigenaar: een invoerwaarde naast een
schrijver die zijn doel open laat is open; een creatie met een onbepaalde voorwaarde laat haar relatie open; een
opgave is open zonder waarde (zij is niet gesloten door haar herkomst; een declaratie die een lid volledig noemt is
een eigen kaart); een lid dat voorbij een onbepaald lid gelezen wordt, is voorlopig (zijn oordeel telt, een
deponering vervalt, een fout laat het onbepaald, een barrière is een open lezing, de deponeringen van een binder
wachten op een gewone lezing).
