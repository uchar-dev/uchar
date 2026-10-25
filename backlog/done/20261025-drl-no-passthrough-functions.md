# drl-no-passthrough-functions: de DRL-tekst maakt geen functie die alleen doorgeeft of niets toetst

*Voorstel van de werkbank (2026-10-25), op besluit van de eigenaar ("optie 1"). Niet gecommit tot de bouw. Zelfde
bestand als `20261025-drl-function-names-from-rule`; die kaart komt na deze (`WAY-OF-WORKING.md` §1). Vervolg in de
toolchain: `backlog/new/20261025-drl-readable-functions-pin.md`.*

## Aanleiding

Volgens de eigenaar is de DRL-tekst bedoeld als vorm van de verlaging die een Java-engineer kan lezen. Sinds kaart
`drl-teksttak-op-truth-maintenance` (`927e3821`, 2026-10-13) is elke toets en elke waarde van het plan een functie in
het Java-dialect met de RegelSpraak-zin erboven. Dat volgt uit twee besluiten van de eigenaar: besluit 5
(2026-10-12: runtime-aanroepen met de bronzin, geen MVEL-operatoren) en normvraag 1 van Fable-advies 2 (2026-10-13:
"altijd een functie", omdat MVEL geen `try`/`throw` kent). Het voorbeeld bij die vraag had één functie per
RegelSpraak-zin. De weergave maakt daarnaast een functie voor elke knoop met eigen statements (`DrlRendering`, javadoc
r. 54 e.v.), ook waar die functie alleen doorgeeft of niets toetst.

Meting op v2/herleving-ww (regels `ece012e`, uchar `229c5141`, toolchain `ec2b6bb0`): 339 regels en 562 functies.

- 115 functies `rN_toets…` doen alleen `return rN_samengesteld…(…);`: de toets van een regel waarvan de voorwaarde
  samengesteld is (`test`, r. 678, rol `CONDITION`/`VORM` op r. 686, en `compound`, r. 819). Beide dragen dezelfde zin.
- 19 samengestelde voorwaarden hebben nul leden, kwantor `ALLE`, alle in defaults-varianten `default N`; commentaar
  `// altijd`. Zij geven `ComputeHelpers.endVerdict(ALLE, 0, 0, 0)`, en dat is `true`; de regel roept ze aan als
  `eval( rN_toets0() )`. Dat zijn 38 functies (toets plus samengesteld).
- 17 samengestelde voorwaarden hebben één lid met kwantor `DE`.
- Bij nul leden declareert `compound` `Boolean vast` zonder het te gebruiken, en `raak` alleen om de vaste 0 door te
  geven.

Voorbeeld, `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1`:

```java
// altijd
function boolean r122_toets0() {
    return r122_samengesteld1();
}

// altijd
function boolean r122_samengesteld1() {
    int raak = 0;
    Boolean vast;
    return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 0);
}
```

Goal: de DRL-weergave schrijft een functie alleen waar die iets toetst of berekent; dezelfde termboom, dezelfde
semantiek, minder functies.

- Een toets waarvan de voorwaarde samengesteld is, is één functie: de toets draagt het lichaam van de samengestelde
  voorwaarde. Bij de rollen met een `try` (`WITHOUT_FAULT`, `FAULT`, `DEPOSIT`) komt dat lichaam binnen de `try`, of het
  `Result:` zegt waarom daar twee functies blijven.
- Een samengestelde voorwaarde zonder leden waarvan het oordeel `true` is (`ALLE`, `DE`): geen functie en geen `eval`.
  Bij een andere kwantor zonder leden kiest de bouwer de vorm en zegt waarom.
- Een samengestelde voorwaarde met één lid wordt dat lid, waar het oordeel van de kwantor over één lid gelijk is aan
  dat van het lid (volgens `established` en `endVerdict`: `ALLE`, `DE`, `TEN_MINSTE 1`; de andere per kwantor
  nagaan). Staat zij in een andere samengestelde voorwaarde, dan verdwijnt haar aanroep uit het lichaam van de ouder.
- Een local (`raak`, `vast`) staat alleen waar hij gelezen wordt.
- Er gaat geen RegelSpraak-zin verloren: heeft een functie die opgaat in een andere een andere zin dan die andere, dan
  blijft haar zin als commentaarregel op haar plaats in het lichaam.

Acceptance:
- (a) Een getuige over het corpus, voor elke golden die de backend als DRL-tekst draagt: geen functie waarvan het
  lichaam alleen de aanroep van een andere functie van dezelfde regel teruggeeft, en geen functie voor een
  samengestelde voorwaarde zonder leden met oordeel `true`.
- (b) Rood eerst, gericht: een toets over een samengestelde voorwaarde in elke rol (`CONDITION`, `VORM`,
  `WITHOUT_FAULT`, `FAULT`, `DEPOSIT`); een default zonder voorwaarde; een samengestelde voorwaarde met één lid voor
  elke kwantor; zo'n voorwaarde genest in een andere; een zin die als commentaarregel blijft staan.
- (c) Geen gedragsverandering: `BranchDumpsCorpusWitnessTest` ongewijzigd groen, `planvorm` en `modelvorm` gelijk aan
  hun nulmeting (het plan verandert niet), gate 12 met dezelfde statusregel per golden en dezelfde `EXPECTED_PLAYED`.
- (d) In het `Result:` de meting op v2/herleving-ww vóór en na: het aantal functies (verwacht rond 562 − 115 − 19), de
  tellingen van (a), en het stuk van `r122` na de wijziging.
- (e) De vloeren van `DrlPiecesCorpusWitnessTest` (functies, commentaar, aanroepen, gemeten 2026-10-24) en de
  verwachte functies in `DrlPiecesWitnessTest` en `FormsPlan` veranderen, omdat deze kaart het aantal functies
  verandert; het `Result:` noemt elke aangepaste verwachting.
- (f) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/DrlRendering.java`; de DRL-getuigen in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/` en `.../transpiler/tms/`; de sectie "The DRL-text branch"
van `backends/drools/README.md`; deze kaart.

Out of scope:
- De namen van de functies (kaart `20261025-drl-function-names-from-rule`).
- Een term die niet kan falen inline zetten ("optie 2", op 2026-10-13 afgewezen), zoals een constante
  (`return Boolean.TRUE;`) of één `collapse` van een gelezen waarde in een `try` (31 op v2).
- De analyse en het plan (`TmsTranspiler`, `plan/`), de exec-modelweergave, `ComputeHelpers`.
- MVEL-operatoren; besluit 5 blijft staan.

## Result (2026-10-25)

Tak `card/drl-no-passthrough-functions`, vanaf main `a9784027`, de kaart naar `doing/` in `ce2566f3`.

- `1ba36560` de weergave (`DrlRendering`), de README-sectie "The DRL-text branch", de corpusgetuige uitgebreid, de
  gerichte getuige `DrlConditionFunctionsWitnessTest` nieuw, de verwachtingen van `DrlPiecesWitnessTest` aangepast.
- `c786cc5f` een zevende gerichte test (een samengestelde voorwaarde zonder leden onder `GEEN` en `TEN_MINSTE 1`).
- dit Result, in het commit daarna.

May touch gevolgd: de diff van de tak raakt `DrlRendering.java`, `backends/drools/README.md` (alleen de sectie "The
DRL-text branch"), drie DRL-getuigen in `backends/drools/src/test/java/dev/uchar/regelspraak/cases/` en deze kaart. Geen
beschermd pad, geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: de draad, de spec, het plan en het exec-model
veranderen niet.

### Wat de weergave nu schrijft

Alleen in `DrlRendering.Rule`:

- `test`: een toets die het oordeel van zijn voorwaarde vraagt (rollen `CONDITION`, `VORM`, `WITHOUT_FAULT`) draagt de
  statements van een samengestelde voorwaarde zelf (`returning`, `compoundBody`); bij `WITHOUT_FAULT` staan ze binnen de
  `try`, waar elke `return` van het oordeel het antwoord van de toets is, zoals voorheen de `return` van de aanroep. De
  rollen `FAULT` en `DEPOSIT` houden de functie van de samengestelde voorwaarde (zie voorlopig besluit 1).
- de matrix van een kwantorvouw (`contribution`, `Verdict`) draagt haar samengestelde voorwaarde ook: haar functie gaf
  anders alleen de aanroep terug, wat acceptatie (a) verbiedt (4 per bouw in `jutterswet-redderkeur`).
- `compound`: een samengestelde voorwaarde over één lid waarvan het oordeel dat van het lid is, is dat lid (`member`,
  `unwrapped`), ook als lid van een andere; welke dat zijn, vraagt `givesMemberVerdict` bij het renderen aan
  `ComputeHelpers.established(q, t, 0, 0, 1)` en `endVerdict(q, t, 0, 1)` en `endVerdict(q, t, 1, 1)`, de functies die
  de samengestelde voorwaarde zelf aanroept: geen stopteken vóór het lid, oordeel false zonder en true met het lid. Dat
  geldt voor `ALLE`, `DE`, `TEN_MINSTE 1` en `PRECIES 1`, en voor geen andere kwantor of drempel (`GEEN` over één lid is
  de ontkenning, `TEN_HOOGSTE 1` en `TEN_MINSTE 0` staan vast zonder het lid te lezen, `TEN_MINSTE 2` en `PRECIES 2`
  staan vast op false, `TEN_HOOGSTE 0` en `PRECIES 0` zijn de ontkenning).
- een samengestelde voorwaarde over geen lid is haar oordeel waar zij staat: `ComputeHelpers.endVerdict(<kwantor>,
  <drempel>, 0, 0)`, zonder functie en zonder locals (voorlopig besluit 2). Een toets in een van de drie rollen
  hierboven waarvan de voorwaarde zo'n samengestelde voorwaarde met oordeel true is (`holdsWhatever`), is geen toets: de
  regel krijgt geen `eval` en geen functie voor haar. Dat zijn de defaults zonder voorwaarde (`ALLE` over geen lid,
  commentaar `// altijd`).
- `raak` en `vast` staan alleen nog in de statements van een samengestelde voorwaarde over een lid of meer, waar beide
  gelezen worden.
- de zin: elke samengestelde voorwaarde die opgaat in een andere functie, zet haar zin in `absorbed`; `reserveer`
  noteert per functie hoe lang die lijst was, en de zinnen erna gaan als commentaarregel in die functie (`sentences`):
  boven de `if` van het lid dat zij werd, in de statements van een samengestelde voorwaarde, of bovenaan het lichaam als
  geen statement ze plaatste (de samengestelde voorwaarde die een toets draagt, of een in een waarde). Een zin die
  gelijk is aan die van de functie of samengestelde voorwaarde waarin zij opgaat, of aan de regel erboven, komt er niet.
  `renderRule` faalt met een `IllegalStateException` als een zin in geen functie terechtkwam. `let` schrijft de binder
  nu vóór het lichaam uit, zodat de zinnen van de waarde in de binder komen en die van het lichaam in de `Let`.
- de javadoc van de klasse (een bullet) en van `test`, `compound`, `returning`, `compoundBody`, `member`,
  `holdsWhatever`, `unwrapped`, `givesMemberVerdict`, `sentences` zeggen wat nu een eigen functie krijgt; zo ook de
  README-sectie "The DRL-text branch".

Het stuk van `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1` (`r122`) op
v2/herleving-ww, na de wijziging (vóór: `r122_toets0`, `r122_samengesteld1` en `r122_afloop2`, zoals in de Aanleiding):

```java
// zijn berekende datum aanvang eindigingsgrond recht op uitkering
function Object r122_afloop0(Object $berekendeDatumAanvangEindigingsgrondRechtOpUitkering) {
    try {
        return ComputeHelpers.collapse($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
    } catch (RuntimeException e) {
        return ComputeHelpers.caught(e);
    }
}

rule "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1"
    dialect "mvel"
when
    $onderwerp : dev.uchar.regelspraak.facts.Eindigingsgrond( ComputeHelpers.classIs(this, "dev.uchar.regelspraak.facts.Eindigingsgrond") )
    $stand_0 : State( attribute == ElementKey.of("eindigingsgrond", "berekende datum aanvang eindigingsgrond recht op uitkering", false), subject == $onderwerp, $berekendeDatumAanvangEindigingsgrondRechtOpUitkering : value )
    not( $hoger_1250 : Decides( stack == "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering" && index < 1, subject == $onderwerp ) )
then
    Object v = r122_afloop0($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
    if (ComputeHelpers.isEvaluationFault(v)) {
        insertLogical(new Fault($onderwerp, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering", ComputeHelpers.designate(v, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering")));
    } else if (v != null) {
        insertLogical(new Value($onderwerp, ElementKey.of("eindigingsgrond", "datum aanvang eindigingsgrond recht op uitkering", false), v, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering"));
        insertLogical(new Decides($onderwerp, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering", 1));
    }
end
```

### Wat er verdween

Gemeten met een probe-testklasse in de werkmap (niet gecommit, daarna verwijderd; dezelfde opzet als in
`drl-rule-functions`), die de DRL-tekst van de tak `DRL_TEXT` met de stukken naar bestanden schrijft, op de code van
`ce2566f3` en op die van `1ba36560`, en een script dat per regel de functies van vóór en na vergelijkt
(`werk/drl-no-passthrough-functions/probe/`, `vanished.py`, `ownwork.py`). Er kwam geen functie bij.

Corpus, 282 gedragen goldens, per bouw (zonder lezingen; met lezingen dezelfde verschillen): 2499 functies vóór, 2201
na. Weg: 240 functies `samengesteld` en 58 functies `toets`, in 225 regels:

| vorm | samengesteld | toets |
|---|---|---|
| toets `CONDITION`/`VORM` over een samengestelde voorwaarde van meer leden (3 met een geneste) | 86 | |
| toets `CONDITION`/`VORM` over één lid (15 `ALLE`, 13 `DE`, en 4 `GEEN`, die de toets draagt) | 32 | |
| toets `CONDITION` over geen lid (`ALLE`, oordeel true: defaults zonder voorwaarde) | 58 | 58 |
| toets `WITHOUT_FAULT` (19 over meer leden, 11 over één lid `DE`) | 30 | |
| toets `FAULT` en functie `fout` over één lid `DE` (11 regels, 2 per regel) | 22 | |
| toets `DEPOSIT` en functie `deponering` over één lid `DE` (4 regels, 2 per regel) | 8 | |
| matrix van een kwantorvouw over meer leden | 4 | |

De weigeringen (94 teksten van de 47 niet gedragen goldens) zijn bytegelijk; 174 van de 564 teksten veranderen.

v2/herleving-ww (regels `ece012e`, bronnen in resolve-volgorde: `fundament/rules/*.rgs` gesorteerd, dan
`herleving-ww/rules/*.rgs` gesorteerd; het IR gemaakt met het frontend van deze worktree, `--ir`, zonder diagnostics),
zonder lezingen: vóór 339 regels en 562 functies, zoals de kaart verwacht; na 339 regels en 416 functies. Weg: 127
functies `samengesteld` en 19 functies `toets`:

- 115 toetsen `CONDITION`/`VORM` die alleen hun `samengesteld` aanriepen, zoals de kaart telt: 79 over meer leden (13
  met een geneste), 17 over één lid `DE` en 19 over geen lid (`ALLE`, `// altijd`), van die laatste ook de toets;
- 12 toetsen `WITHOUT_FAULT` over meer leden (3 met een geneste), die de kaart niet meetelde: hun `try` deed iets, maar
  het lichaam van de samengestelde voorwaarde past erin zonder herschrijving.

Dus 562 − 115 − 19 − 12 = 416, en niet de 428 (562 − 115 − 19) van de kaart. De 12 toetsen `FAULT` en de 12 functies
`fout` over dezelfde voorwaarden houden hun `samengesteld` (voorlopig besluit 1).

De tellingen van (a) op v2, met dezelfde toetsen als de corpusgetuige (in Python nagebouwd, `ownwork.py`; op het corpus
geeft het dezelfde getallen als de Java-getuige): vóór 115 functies die alleen een functie van hun stuk aanroepen, 19
functies van een samengestelde voorwaarde zonder leden met oordeel true, 17 functies van een samengestelde voorwaarde
met één lid dat haar oordeel geeft, en 19 functies met een local (`vast`) die niet gelezen wordt; na: 0, 0, 0, 0.

### Getuigen

(a) `DrlPiecesCorpusWitnessTest`, uitgebreid (`ownWork`): voor elke functie van elk stuk, op elke gedragen golden,
zonder en met lezingen, gelezen uit de tekst: haar lichaam (zonder commentaarregels) is niet alleen `return <functie van
het stuk>(…);`; zij is niet de functie van een samengestelde voorwaarde zonder leden met oordeel true (het lichaam is,
op de locals en de regels van een `try` na, `return ComputeHelpers.endVerdict(<q>, <t>, …, 0);` en `endVerdict(q, t, 0,
0)` is true); niet die van een samengestelde voorwaarde met één lid dat haar oordeel geeft (`endVerdict(<q>, <t>, raak,
1)` waar `established` en `endVerdict` het lid als oordeel geven); en een gedeclareerde `raak` of `vast` wordt gelezen.
De oordelen komen van `ComputeHelpers` zelf. Elke afwijking komt in een eigen lijst met golden, regel en functie; de
test faalt met hun aantal en de eerste tien.

- Rood op `ce2566f3` (de getuigen van de tip in een worktree op `ce2566f3`, log
  `werk/drl-no-passthrough-functions/logs/red-2.log`): "functies zonder eigen werk: 730", per bouw 180 functies die
  alleen een functie van hun stuk aanroepen (176 toetsen, 4 matrices), 58 functies van een samengestelde voorwaarde
  zonder leden met oordeel true, 69 met één lid dat haar oordeel geeft (15 `ALLE`, 54 `DE`), en 58 met een `vast` die
  niet gelezen wordt.
- Groen op `1ba36560`: 0 van elk.

(b) `DrlConditionFunctionsWitnessTest`, nieuw, zeven tests, per regel de tabel van zijn stuk
(`DrlPiecesWitnessTest.table`, nu package-zichtbaar, net als `piece`) en waar de vorm van een lichaam de zaak is de
functie letterlijk:

1. `a_test_over_a_composite_is_one_function_where_its_role_asks_the_verdict`: `CONDITION` op
   `jutterswet-uitsluiting-rgs` (`default 0.0.0`, drie leden, de toets letterlijk), `VORM` op
   `anchor/jutterswet-kustregister` (`eenmalig kaveletiket: sleutel van Kavel 1`, twee leden, zonder zin) en
   `WITHOUT_FAULT` op `fold-filter-divides-by-zero` (`drukte`, twee leden, de toets letterlijk met de statements binnen
   de `try`; de toets roept nog de functie van de vouwlezing aan).
2. `a_test_that_asks_for_a_fault_or_a_deposit_keeps_the_function_of_its_composite`: `FAULT` en `DEPOSIT` op
   `fold-filter-divides-by-zero` (`drukte: fout in de voorwaarde`, `drukte: deponering in de voorwaarde`): toets,
   `samengesteld`, vouwlezing, `fout`/`deponering`, `samengesteld`, vouwlezing. Deze test is ook op `ce2566f3` groen:
   hij pint de vorm die blijft.
3. `a_default_without_a_condition_has_no_test`: `jutterswet-bergingstoeslag`, `defaults A:Jutter.bergingstoeslag:
   default 1` alleen `r1_afloop0` en geen `eval`; `default 0` van `bergingsforfait` de waarde met `Let` en binder.
4. `a_composite_of_one_member_is_that_member_in_the_goldens`: `ALLE` in een `VORM`-toets
   (`anchor/jutterswet-kustregister`, `return ComputeHelpers.emptyTest($vondstnummer, Boolean.FALSE);`), `DE` in de
   toetsen `WITHOUT_FAULT`, `FAULT` en `DEPOSIT` en in de functies `fout` en `deponering`
   (`fold-filter-divides-by-zero`), en `GEEN` over één lid (`jutterswet-uniekwantor`), dat de toets als samengestelde
   voorwaarde draagt.
5. `a_composite_of_one_member_is_that_member_where_its_quantifier_gives_the_members_verdict`: een document in de test
   (`TmsDocuments`) met negen regels "Een Jutter is doelN indien <kwantor> over één lid": `the 0`, `all 0`, `atLeast 1`,
   `exactly 1` geven `return ComputeHelpers.characteristic($lid, Boolean.TRUE);`; `none 0`, `atLeast 2`, `atMost 0`,
   `atMost 1`, `exactly 0` dragen de samengestelde voorwaarde. Een case met een Jutter met en een zonder het lid speelt
   op de tak `EXEC_MODEL` en op `DRL_TEXT`, met de verwachtingen met de hand uit RUNTIME-SEMANTICS ch. 5.10.
6. `a_composite_of_one_member_in_another_is_its_member_with_its_sentence_as_a_comment_line`: een document met vier
   regels: een lid van één lid met een eigen zin (`    // aan de voorwaarde b voldoet` boven de `if` van dat lid), een
   toets over één lid dat een samengestelde voorwaarde van twee is (de zin van de binnenste bovenaan het lichaam), een
   lid over geen lid (`    // altijd` boven `if (ComputeHelpers.endVerdict(…ALLE, 0, 0, 0))`), en een lid met dezelfde
   zin als de samengestelde voorwaarde waarin het staat (geen commentaarregel). Elke regel heeft één functie. De case
   met de vier combinaties van `a` en `b` speelt op beide takken.
7. `a_composite_over_no_member_is_its_verdict`: een document met `GEEN` over geen lid (oordeel true: geen toets, geen
   `eval`) en `TEN_MINSTE 1` over geen lid (oordeel false: de toets geeft `ComputeHelpers.endVerdict(…TEN_MINSTE, 1, 0,
   0)` terug, zonder functie van de samengestelde voorwaarde). De case speelt op beide takken.

Rood op `ce2566f3` (`werk/drl-no-passthrough-functions/logs/red-2.log`): 6 van de 7 (alle behalve test 2), elk op de
tabel of de tekst met een functie `samengesteld` of een toets die alleen haar aanroept; de documenten laadden en bouwden
daar ook (`atLeast 2` over één lid en een samengestelde voorwaarde zonder leden in een voorwaarde neemt de loader aan).

Mutaties van de wijziging in `DrlRendering.java`, elk in een eigen worktree (niet gecommit, daarna teruggezet en de
worktree verwijderd; recept `werk/drl-no-passthrough-functions/mutations/run.py`, met de vervangen tekst per mutatie,
logs `werk/drl-no-passthrough-functions/logs/mutation-M*.log`). M1 tot en met M8 liepen op `1ba36560` (zes gerichte
tests), M4 nogmaals en M9 op `c786cc5f` (zeven); de tabel noemt voor M4 die laatste run. Geen mutatie gaf een
compileerfout; elke rode test faalt op zijn tabel of zijn tekst:

| mutatie | corpusgetuige | gerichte tests die rood worden |
|---|---|---|
| M1 `returning` draagt nooit een samengestelde voorwaarde | rood: 94 per bouw die alleen een aanroep teruggeven (de toetsen en matrices over meer leden en `GEEN`) | 1, 4, 5, 6 |
| M2 `WITHOUT_FAULT` roept in de `try` de functie aan, zoals vóór de kaart | groen (een `try` is geen doorgeven) | 1 |
| M3 `FAULT` draagt de statements van de samengestelde voorwaarde | niet gedraaid | 2, 4 |
| M4 `holdsWhatever` geeft altijd false | rood: 58 per bouw functies van een samengestelde voorwaarde zonder leden met oordeel true (de toets `return ComputeHelpers.endVerdict(…ALLE, 0, 0, 0)`) | 3, 7 |
| M5 `givesMemberVerdict` geeft altijd false | rood: 69 per bouw met één lid dat haar oordeel geeft | 4, 5, 6 |
| M6 `givesMemberVerdict` neemt elk lid over één lid als lid aan, ongevraagd | groen | 4 (`GEEN`), 5 (`none 0`) |
| M7 `sentences` laat elke zin vallen | groen (het corpus heeft geen zin die opgaat in een functie met een andere zin) | 6 |
| M8 een samengestelde voorwaarde over geen lid heeft haar functie van vóór de kaart, en de toets blijft | rood: per bouw 58 alleen een aanroep, 58 zonder leden met oordeel true, 58 met een `vast` die niet gelezen wordt | 3, 6 |
| M9 een samengestelde voorwaarde over geen lid krijgt haar functie van vóór de kaart waar zij geen toets wegneemt | groen (op het corpus staat zo'n voorwaarde alleen als toets van een default) | 6, 7 |

Zonder mutatie is alles groen.

(c) Geen gedragsverandering. `BranchDumpsCorpusWitnessTest` is ongewijzigd en groen in de suite (2 tests). De dumps
"planvorm" en "modelvorm" van de kaart bestaan niet meer als gereedschap; in plaats daarvan: `git diff ce2566f3 --
backends/drools/src/main` raakt alleen `DrlRendering.java`. Het plan en het exec-model renderen dezelfde analyse, die
niet verandert, dus het plan en het exec-model zijn dezelfde. De gerichte tests 5, 6 en 7 spelen hun cases op beide
takken. Gate 12: zie hieronder.

(e) Aangepaste verwachtingen:

- `DrlPiecesCorpusWitnessTest.EXPECTED_MIN`, gemeten 2026-10-25: `functies` 2499 → 2201, `functies met commentaar` 1944
  → 1680, `functies die een functie van hun stuk aanroepen` 481 → 241, `functies met lezingen` 3456 → 3158. Ongewijzigd:
  `stukken` 2124, `stukken zonder functies` 763 (elke default zonder voorwaarde heeft nog zijn waarde), `stukken met
  lezingen` 3046, de goldens 282 en de teksten buiten ASCII 49.
- `DrlPiecesCorpusWitnessTest.EXPECTED_UNREACHED`: `r2_binder4` → `r2_binder2`, dezelfde binder in
  `jutterswet-bergingstoeslag`, `defaults A:Jutter.bergingsforfait: default 0`; alleen de plaats verschuift, omdat de
  toets en de samengestelde voorwaarde van die default weg zijn.
- `DrlPiecesWitnessTest.a_composite_condition_is_a_function_its_test_calls` heet nu
  `a_composite_condition_is_the_body_of_its_test`; de tabel van `defaults K:Jutter.uitgesloten: default 0.0.0` was
  `r1_toets0` (roept `r1_samengesteld1`), `r1_samengesteld1`, `r1_afloop2`, regel roept `r1_toets0`, `r1_afloop2`, en is
  `r1_toets0` (roept niets), `r1_afloop1`, regel roept `r1_toets0`, `r1_afloop1`.
- `DrlPiecesWitnessTest.the_fall_back_of_a_default_carries_the_functions_of_its_test_and_its_value`: de tabel van
  `terugval 0` was `r3_toets0` (roept `r3_samengesteld1`), `r3_samengesteld1`, `r3_afloop2`, en is `r3_toets0` (roept
  niets), `r3_afloop1`; de letterlijke functie `r3_afloop2` heet `r3_afloop1` (de plaats verschuift) en is de tweede in
  plaats van de derde van het stuk.
- Beide tests van `DrlPiecesWitnessTest` zijn rood op `ce2566f3` met de nieuwe verwachtingen (`red-2.log`), op de tabel
  met `r1_samengesteld1` en `r3_samengesteld1`.
- De javadoc van `DrlPiecesWitnessTest` zegt dat de functie die een andere functie van dezelfde regel aanroept, die
  `drl-rule-functions` (b) vroeg, sindsdien in `DrlConditionFunctionsWitnessTest` staat (de toets die de vouwlezing
  aanroept, de toets van een fout die zijn `samengesteld` aanroept).
- `FormsPlan` en `DrlRenderingWitnessTest` veranderen niet: de enige samengestelde voorwaarde van de vormenplannen staat
  in een toets `DEPOSIT` ("alle termen", vijf leden), die zijn functie houdt, dus `_samengesteld` staat nog in de
  vormentekst, en geen ander fragment of naam van de plannen verschuift.

### Voorlopige besluiten

1. `FAULT` en `DEPOSIT` houden twee functies. De toets vraagt daar niet het oordeel maar of de evaluatie faalt of
   deponeert. De statements van een samengestelde voorwaarde eindigen in een `return` van het oordeel bij elk stopteken
   en aan het eind; binnen de `try` van die rollen moest elke `return` een andere afloop worden (`return false;` bij
   `FAULT`, uit de `try` vallen naar `return ComputeHelpers.reported(sonde);` bij `DEPOSIT`). Dan leest dezelfde
   samengestelde voorwaarde per rol anders, en een vergeten `return` gaf het oordeel als antwoord op "faalt zij", een
   stil fout antwoord. Als eigen functie leest zij in elke rol gelijk, en gelijk aan de functies `fout` en `deponering`
   van het gevolg, die haar ook in hun `try` aanroepen. Bij `WITHOUT_FAULT` is de `return` van het oordeel het antwoord,
   dus daar gaan de statements ongewijzigd binnen de `try`. Op v2: 12 toetsen `FAULT` en 12 functies `fout` houden hun
   `samengesteld`; op het corpus per bouw 19 toetsen `FAULT` en 15 `DEPOSIT` over meer leden, en evenveel functies
   `fout` en `deponering`.
2. Een samengestelde voorwaarde over geen lid onder een andere kwantor of met oordeel false: haar oordeel
   `ComputeHelpers.endVerdict(<q>, <t>, 0, 0)` staat waar zij stond, zonder functie. Zij toetst niets en berekent alleen
   dat oordeel, met dezelfde functie als voorheen (één semantiek, het oordeel wordt niet bij het renderen ingevuld, wat
   "optie 2" zou zijn). Een toets met zo'n voorwaarde en oordeel false blijft een functie die dat oordeel teruggeeft (de
   regel vuurt nooit, zoals voorheen); een toets `FAULT` of `DEPOSIT` erover (komt niet voor) evalueert haar in zijn
   `try`. Op het corpus en op v2 komt geen samengestelde voorwaarde zonder leden voor met een andere kwantor dan `ALLE`;
   test 6 en 7 tonen de vorm.
3. Een samengestelde voorwaarde over één lid waarvan het oordeel afwijkt, blijft een samengestelde voorwaarde (de
   statements in de toets die haar vraagt, of haar eigen functie). `GEEN` en `TEN_HOOGSTE 0` en `PRECIES 0` over één lid
   zijn de ontkenning van het lid; een vorm `!lid` zou een nieuwe vorm zijn naast de kwantoren, en `TEN_MINSTE 0`,
   `TEN_HOOGSTE 1`, `TEN_MINSTE 2` staan vast zonder het lid te lezen, wat als constante "optie 2" zou zijn. Op het
   corpus per bouw 4 keer `GEEN` over één lid (`jutterswet-uniekwantor`), door de toets gedragen.
4. De zin "altijd" van een default zonder voorwaarde verdwijnt met zijn toets. Die toets gaat niet op in een andere
   functie; de regel van de kaart over zinnen gaat over een functie die opgaat in een andere. "altijd" is ook niet de
   zin van een voorwaarde uit de bron maar het snippet dat het frontend aan de lege rechtvaardiging van zo'n default
   geeft (`"justification": {"node": "Compound", "parts": [], "source": {"snippet": "altijd"}}`); dat de regel geen
   voorwaarde heeft, staat in de tekst als het ontbreken van een `eval`.
5. De zin van een samengestelde voorwaarde die in een waarde opgaat (de voorwaarde van een keuze, een samengestelde
   voorwaarde als argument), staat bovenaan het lichaam van de functie waarin zij opgaat, niet boven de statement waarin
   de waarde staat: alleen de statements van een samengestelde voorwaarde plaatsen een zin precies. Het komt op het
   corpus en op v2 niet voor (M7 laat de corpusgetuige groen).
6. Een commentaarregel in een lichaam gaat naar de Java-compiler, die een backslash gevolgd door `u` ook in commentaar
   als tekenescape leest (een regeleinde daarin breekt de functie, een `u` zonder vier hexcijfers is een compileerfout);
   `sentences` schrijft daar `\ u`. Boven een functie is het commentaar van DRL en blijft het zoals het was.
7. De functie `zonderfout` van een voorwaarde zonder haar fout (`Term.WithoutFault`) roept de functie van haar
   samengestelde voorwaarde in haar `try` aan, zoals voorheen; zij geeft niet alleen door en de kaart noemt haar niet.
   Op het corpus en op v2 roept geen `zonderfout` een `samengesteld` aan.
8. De local `uitkomst` in de `try` van `FAULT`, `DEPOSIT`, `fout` en `deponering` wordt niet gelezen; hij staat er omdat
   Java een uitdrukking als `((Boolean) x)` niet als statement neemt. De kaart noemt alleen `raak` en `vast`; hij
   blijft.

### Voor de toolchain-kaart `drl-readable-functions-pin`

`PlaygroundDrlRulesTest` (r. 732–750) pint `r11_toets0 | … | roept [r11_samengesteld1]` en `r11_samengesteld1`; na deze
kaart is dat één functie `r11_toets0` (als de voorwaarde van `r11` een samengestelde voorwaarde over een lid of meer is
in de rol `CONDITION`, `VORM` of `WITHOUT_FAULT`), en de functies erna schuiven één plaats op. Een default zonder
voorwaarde heeft geen toets meer; zijn waarde heet `r<n>_afloop0`.

### Suite en gate 12

- De JUnit-suite van `backends/drools` (`mvn -B -o test`) op `1ba36560`: 1697 tests, 0 fouten, 0 overgeslagen; de 1691
  van `ce2566f3` plus zes in `DrlConditionFunctionsWitnessTest`. `BranchDumpsCorpusWitnessTest` (2),
  `DrlRenderingWitnessTest` (9), `ReadingRulesFixpointWitnessTest` en `ReadingsCorpusWitnessTest` groen. Log
  `werk/drl-no-passthrough-functions/logs/suite-1.log`.
- Gate 12 (`scripts/check-drools.sh`) op `1ba36560` en opnieuw op `c786cc5f`: PASS, beide keren; 279 goldens gespeeld op
  de exec-model-tak, gelijk aan `EXPECTED_PLAYED=279`; kanaalvloer 50 niet-gespeelde goldens bij naam; de DRL-teksttak
  speelt 279, elke statusregel gelijk aan die van de exec-model-tak (sweep 221 s en 222 s); de suite van de gate 1697
  en 1698 tests (vloer 1573), nul overgeslagen; `check-drools-codes` PASS (294 codes). Het rapport van de gate is regel
  voor regel gelijk aan dat van gate 12 in de gate-set van `drl-rule-functions` (`werk/drl-rule-functions/gates-2/
  12-drools.log`, op `210dfed2`), op de wandkloktijd van de sweep en het aantal tests na: dezelfde weigeringen met
  dezelfde codes per vorm, dezelfde aantallen. Logs `werk/drl-no-passthrough-functions/logs/gate12-1.log` en
  `gate12-2.log`.
- De volledige gate-set draaide hier niet; die draait de hoofdsessie vóór de landing (acceptatie (f)). Gate 13 en 17
  gebruiken de DRL-teksttak ook; de lezingen komen uit het plan (`Consequence.Reading`), niet uit de functies van een
  toets, en `ReadingsCorpusWitnessTest` en `BranchDumpsCorpusWitnessTest` (met de sectie `reading`) zijn groen.
- De commits na `c786cc5f` veranderen alleen de kaart.
- De volledige gate-set (`werk/gates.sh`, hoofdsessie) op `bb2d9f2a`: 17 van 17 groen in 29 minuten (12 732 s,
  13 201 s, 17 390 s); samenvatting `werk/drl-no-passthrough-functions/gates-1.summary.txt`. Geland op `main`
  fast-forward, met deze kaart naar `done/`.
