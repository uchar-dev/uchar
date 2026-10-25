# drl-function-names-from-rule: een functie van de DRL-tekst heet naar haar regel en haar soort

*Voorstel van de werkbank (2026-10-25), op besluit van de eigenaar ("optie 3"). Niet gecommit tot de bouw. Begint na
de landing van `20261025-drl-no-passthrough-functions`: beide kaarten raken `DrlRendering.java`
(`WAY-OF-WORKING.md` §1). Vervolg in de toolchain: `backlog/new/20261025-drl-readable-functions-pin.md`.*

## Aanleiding

Een functie van de DRL-tekst heet `r<n>_<soort><plaats>`: `n` is de plaats van haar regel in het plan, `plaats` haar
plaats onder de functies van die regel (`DrlRendering.name`, r. 449; javadoc r. 74: "one plan gives one text"). Wie de
ruwe tekst leest, ziet in `eval( r122_toets0() )` niet van welke regel de functie is; hij moet naar beneden zoeken tot
`rule "…"`. Sinds `drl-rule-functions` zegt `drlPieces()` per regel welke functies de zijne zijn, dus de werkbank
heeft de namen niet nodig. Een Java-engineer die de tekst leest, of een stacktrace van een sessie, heeft ze wel nodig.

Goal: de naam van elke functie zegt van welke regel zij is en welke soort zij is. De naam volgt uit de naam van de
regel en de soort, is deterministisch (één plan geeft één tekst), uniek in de tekst, en een geldige naam voor DRL en
voor het Java-dialect.

Vorm (besluit van de eigenaar, 2026-10-25, gekozen uit `<regel>_<soort>`, `<soort>_<regel>` en een korte vorm
`r<n>_<soort>` met de regelnaam als commentaarkop): `<soort>_<regel>`, met `<soort>` het woord van nu (`toets`,
`samengesteld`, `afloop`, `waarde`, `pad`, `fout`, …) en `<regel>` = `Namen.ident(plan.name(), false)`. Heeft een
regel meer dan één functie van een soort, dan krijgen ze allemaal een volgnummer direct achter de soort, vanaf 1 in de
volgorde van het plan (`pad1_<regel>`, `pad2_<regel>`); een functie die de enige van haar soort is in haar regel,
krijgt geen nummer (`toets_<regel>`). Ook dat besliste de eigenaar op 2026-10-25. Het `_` komt in een `ident` niet voor, want `Namen.ident` splitst op elk teken dat
geen letter of cijfer is. Daardoor is een functienaam nooit gelijk aan een beanlid, een parameter-global of een local
van een gevolg (`v`, `f`, `k`, `sonde`). Zo'n gelijke naam is wat Fable-advies 2 §2.1 als gevaar mat: een functie of
hulp die heet als een veld, verbergt dat veld zonder melding.

Meting op v2/herleving-ww (regels `ece012e`, uchar `229c5141`): 209 regels hebben functies. Het regeldeel van de naam
is mediaan 71 en ten hoogste 119 tekens, en geen twee regels krijgen hetzelfde `ident`. De eigenaar aanvaardt die
lengte. Voorbeelden: `toets_kalendermaandsystematiekArt20Lid1OnderCEnD`,
`afloop_defaultsAEindigingsgrondHerlevingsdatumTerugval2`. Een aanroep leest dan als
`eval( toets_defaultsAEindigingsgrondHerlevingsdatumTerugval2($einddatumVanEenEindigingsgrond, …) )` en
`Object v = afloop_defaultsAEindigingsgrondHerlevingsdatumTerugval2($einddatumVanEenEindigingsgrond);`. Een parameter
als `$codeRedenBeëindigingRechtOpUitkeringWw` compileert nu al, dus een `ë` in een naam neemt Drools in elk geval in
die plaats aan.

Acceptance:
- (a) Een getuige over het corpus: elke functienaam in de tekst van elke gedragen golden volgt de vorm, is uniek, en
  is niet gelijk aan een global (ook niet aan een dagsoort-global `rsDagsoort_<Naam>`, die wel een `_` bevat), een
  beanlid dat een patroon leest, een local van een gevolg, of een woord van DRL of MVEL.
- (b) Rood eerst, gericht: een regel met twee functies van één soort (`pad1_…`, `pad2_…`) naast een regel met één
  (`pad_…`), een regelnaam die met een cijfer begint (een `ident` dat geen Java-identifier is), een
  regelnaam waarvan `ident` leeg is (`x`), twee regelnamen met hetzelfde `ident` (zoals `a-b` en `a b`, met een
  deterministische onderscheiding), een naam met `ë` en een naam met een niet-Latijnse letter (de documenten van
  `İş` en `日` uit stap 1e en 2a van `drl-teksttak-op-truth-maintenance`). Neemt Drools 8.44 een naam niet aan, dan
  geeft de vorm een naam die het wel aanneemt; geen nieuwe weigering.
- (c) Eén plan geeft één tekst: twee weergaven van hetzelfde plan zijn bytegelijk.
- (d) Geen gedragsverandering: `BranchDumpsCorpusWitnessTest` ongewijzigd groen, gate 12 met dezelfde statusregel per
  golden, geen golden die nu geweigerd wordt.
- (e) De verwachte namen in `DrlPiecesWitnessTest`, `FormsPlan` en de lijst `Unreached` van
  `DrlPiecesCorpusWitnessTest` (`r2_binder4`) veranderen, omdat deze kaart de namen verandert; het `Result:` noemt elke
  aangepaste verwachting.
- (f) De javadoc van `DrlRendering` (r. 74) en de sectie "The DRL-text branch" van `backends/drools/README.md` zeggen
  de nieuwe vorm.
- (g) In het `Result:` het stuk van de regel van het voorbeeld (`default 1` van `datum aanvang eindigingsgrond recht op
  uitkering`) na de wijziging, en de lengtes van de namen op v2.
- (h) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/DrlRendering.java`; de DRL-getuigen in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/` en `.../transpiler/tms/`; de sectie "The DRL-text branch"
van `backends/drools/README.md`; deze kaart.

Out of scope:
- De soortwoorden zelf (`afloop`, `samengesteld`, …) en de namen van de slots (`$…`, stap 1e).
- De namen van de regels; `rule "…"` blijft de naam van het plan.
- Minder functies (kaart `20261025-drl-no-passthrough-functions`).
- `Namen.ident` zelf: de fact-model-generator leest het ook.

## Result (2026-10-25)

Tak `card/drl-function-names-from-rule`, vanaf main `88ab04b0`, de kaart naar `doing/` in `f867513d`.

- `7a25a21d` de weergave (`DrlRendering`), de README-sectie "The DRL-text branch", de corpusgetuige uitgebreid, de
  gerichte getuige `DrlFunctionNamesWitnessTest` nieuw, (c) in `DrlRenderingWitnessTest`, en de verwachte namen in zes
  andere DRL-getuigen.
- dit Result, in het commit daarna.

May touch gevolgd: `git diff f867513d -- backends/drools/src/main` raakt alleen `DrlRendering.java`; verder
`backends/drools/README.md` (alleen de sectie "The DRL-text branch"), DRL-getuigen in `.../cases/` en
`.../transpiler/tms/`, en deze kaart. Geen beschermd pad, geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: de
draad, de spec, het plan en het exec-model veranderen niet. `Namen.ident`, de soortwoorden, de namen van de slots en de
namen van de regels veranderen niet.

### De vorm

Een functie heet `<soort>_<regeldeel>`:

- `<soort>` het woord van nu (`toets`, `samengesteld`, `afloop`, `waarde`, `pad`, `fout`, …, 21 woorden);
- staat de soort meer dan eens in de regel, dan krijgen al die functies een nummer direct achter de soort, vanaf 1 in
  de volgorde van het plan (`pad1_kadegeld`, `pad2_kadegeld`); de enige van haar soort krijgt er geen
  (`pad_havengeld`);
- `<regeldeel>` is `Namen.ident(plan.name(), false)` zonder de code points die Drools 8.44 niet in een naam leest, en
  `x` als er niets overblijft (zie randgeval 6);
- hebben meer regels met functies hetzelfde regeldeel, dan krijgen al die regels `_1`, `_2`, … achter het regeldeel,
  in de volgorde van het plan (randgeval 3).

Een regeldeel bevat geen `_` (`Namen.ident` splitst op elk teken dat geen letter of cijfer is) en een soortwoord bevat
geen cijfer, dus het deel vóór de eerste `_` (soort en nummer) en het deel erna (regeldeel en regelnummer) bepalen de
naam elk eenduidig: twee functies krijgen nooit dezelfde naam. Een global is een ident, `rekendatum` of
`rsDagsoort_<Naam>`; geen soortwoord is `rsDagsoort`, dus een functienaam is nooit een global. `render` faalt
bovendien met een `IllegalStateException` als twee functies dezelfde naam krijgen of een functie heet als een global
(niet bereikbaar zonder fout in de weergave; mutatie M2 en M4 tonen de eerste).

Hoe: pas de hele regel zegt of een soort een nummer krijgt, en pas het hele plan of een regeldeel er een krijgt.
`render` rendert daarom elke regel eerst één keer om de soorten van zijn functies in de volgorde van hun plaatsen te
krijgen (`Rule.kinds()`; de tekst van die weergave gaat nergens heen), maakt daaruit alle namen
(`functionNames`), en rendert dan elke regel opnieuw met die namen. De tweede weergave faalt als haar soorten niet die
van de eerste zijn. De imports en dagsoorten die de eerste weergave noteert, noteert de tweede opnieuw met hetzelfde
resultaat. Een weigering valt in de eerste weergave, op dezelfde regel als voorheen.

Het stuk van `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1` op
v2/herleving-ww na de wijziging (vóór: `r122_afloop0`):

```java
// zijn berekende datum aanvang eindigingsgrond recht op uitkering
function Object afloop_defaultsAEindigingsgrondDatumAanvangEindigingsgrondRechtOpUitkeringDefault1(Object $berekendeDatumAanvangEindigingsgrondRechtOpUitkering) {
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
    Object v = afloop_defaultsAEindigingsgrondDatumAanvangEindigingsgrondRechtOpUitkeringDefault1($berekendeDatumAanvangEindigingsgrondRechtOpUitkering);
    if (ComputeHelpers.isEvaluationFault(v)) {
        insertLogical(new Fault($onderwerp, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering", ComputeHelpers.designate(v, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering")));
    } else if (v != null) {
        insertLogical(new Value($onderwerp, ElementKey.of("eindigingsgrond", "datum aanvang eindigingsgrond recht op uitkering", false), v, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering"));
        insertLogical(new Decides($onderwerp, "defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering", 1));
    }
end
```

### De randgevallen van (b)

1. Een `ident` dat met een cijfer begint (`2 jarige termijn`): het regeldeel blijft `2JarigeTermijn`, de naam is
   `toets_2JarigeTermijn`. De soort staat vooraan, dus de naam begint altijd met een kleine ASCII-letter en is een
   naam voor DRL en Java, ook waar het `ident` dat niet is. Drools neemt het aan (de gerichte test speelt op beide
   takken).
2. Een regelnaam zonder letter of cijfer (`— § —`): `Namen.ident` geeft `x`, de naam is `toets_x`. Geen eigen vorm:
   `x` is een gewoon regeldeel, en heeft een andere regel met functies ook regeldeel `x` (een regel die `x` heet, of
   een tweede zonder letter of cijfer), dan geldt randgeval 3.
3. Twee regels met hetzelfde `ident` (`a-b` en `a b`): `toets_aB_1` en `toets_aB_2`, het nummer na een `_` achter het
   regeldeel, in de volgorde van het plan; een regel zonder functies met hetzelfde `ident` (`a/b`, ertussen) telt niet
   mee. Het `_` is nodig omdat een regeldeel op een cijfer kan eindigen (`aB2` is het regeldeel van `a b 2`); een
   regeldeel bevat zelf geen `_`, dus `toets_aB_2` botst met geen ander regeldeel. Zie voorlopig besluit 2.
4. `ë` (`beëindiging`): blijft, `toets_beëindiging`. De DRL-lexer leest de letters van Latin-1 in een naam; op v2
   staan 25 namen met `ë` of `ó` (zoals `afloop_defaultsAEindigingsgrondBeëindigingsdatumDefault0` en
   `toets_uitkeringsduurBovenTienKalenderjarenMetMinderDanTienVóór2016Art42Lid2`), en die tekst compileert.
5. Een niet-Latijnse letter: de documenten `property/turkish-member` (lid `İş`) en `property/awkward-members` (leden
   `日 X` en `İç`) van stap 1e en 2a van `drl-teksttak-op-truth-maintenance`, met de regel hernoemd in de test tot
   `İş van een jutter` en `日 X naar İç`: `waarde_işVanEenJutter` (`Namen.ident` schrijft `İ` vooraan klein, als `i`)
   en `waarde_日XNaarİç`. Beide blijven zoals `Namen.ident` ze geeft; Drools leest `日`, `İ`, `ş` en `ç` in een naam, en
   de cases van beide documenten spelen op beide takken.
6. Niet in de kaart, wel nodig: een code point dat Drools 8.44 niet in een naam leest, valt weg uit het regeldeel. De
   DRL6-lexer van Drools 8.44 (`DRL6Lexer.g`, `IdentifierStart`/`IdentifierPart`) heeft de tabel van Java 5: Unicode
   4.0, niets buiten het Basic Multilingual Plane. Gemeten (probe in `werk/drl-function-names-from-rule/scratch/probe/`,
   niet in de repository): van de 142 939 code points in `\p{L}\p{N}` leest de lexer na `f` er 46 109 als deel van één
   naam en neemt Java er 142 024 in een identifier; 46 100 nemen beide. Elk van die 46 100, in namen van 150 code
   points, compileert en vuurt in een declaratie, een `eval`, een binding en een MVEL-gevolg (308 regels, alle 308
   gevuurd). Elk ander code point dat ik probeerde, geeft `[ERR 101] no viable alternative`: `𝔘` (U+1D518, buiten het
   BMP), `ȷ` (U+0237, Unicode 4.1), `²` (U+00B2, `\p{No}`), `½`, `ꓐ` (U+A4D0), `龦` (U+9FA6). Dat raakte een bestaande
   test: `DrlPiecesWitnessTest.a_place_counts_utf16_code_units` heeft een regel `penninggerechtigd 𝔘` met een functie,
   en met het ongefilterde `ident` compileert die tekst niet meer (mutatie M3). Nu: `toets_penninggerechtigd`, en
   `artikel 2² en ȷ` geeft `toets_artikel2En`. `DrlRendering.namePart` vraagt het per code point aan de lexer zelf
   (`f` gevolgd door het code point is één `ID`) en aan `Character.isJavaIdentifierPart`, en onthoudt het antwoord. Zie
   voorlopig besluit 3.

### Getuigen

(a) `DrlPiecesCorpusWitnessTest`, uitgebreid (`names`): voor elke functie van elk stuk, op elke gedragen golden, zonder
en met lezingen: de naam is `<soort><nummer>_<regeldeel>`, de soort een van de 21 woorden (`KINDS`), het nummer
precies daar waar het stuk meer dan één functie van die soort heeft, van 1 in de volgorde van de tekst, het regeldeel
dat van een eigen orakel (`rulePart`: `Namen.ident` gefilterd met de DRL-lexer en Java) met `_<k>` waar meer stukken met
functies het hebben; de naam is geen global van de kop (ook geen `rsDagsoort_…`), geen woord van DRL (de velden van
`DroolsSoftKeywords`), van MVEL (`AbstractParser.OPERATORS` en `LITERALS`) of Java (`SourceVersion.isKeyword`), 180
woorden van DRL en MVEL; en buiten commentaarregels en stringliterals staat de naam in de tekst alleen als declaratie
of aanroep (direct gevolgd door `(`), dus geen beanlid dat een patroon leest, geen local, geen parameter en geen global
heet zo. De orakels `DECLARATION`, `CALL` en `RETURNED_CALL` lezen nu een naam met letters en cijfers buiten ASCII
(`\p{L}`, `\p{N}`). Nieuwe vloeren, gemeten 2026-10-25: `functienamen met een nummer van hun soort` 261, met lezingen
265. Op het corpus: 2201 namen zonder en 3158 met lezingen, geen naam buiten ASCII, geen gedeeld regeldeel, geen
weggelaten code point; de randgevallen staan daarom in (b).

- Rood op `f867513d` (de getuigen van de tip in een worktree op `f867513d`, log
  `werk/drl-function-names-from-rule/logs/red-1.log`): "functienamen buiten de vorm: 5359" (2201 + 3158, elke naam
  `r<n>_<soort><plaats>`).
- Groen op `7a25a21d`.

(b) `DrlFunctionNamesWitnessTest`, nieuw, vier tests, per regel de tabel van zijn stuk (`DrlPiecesWitnessTest.table`):

1. `a_kind_of_which_a_rule_has_more_than_one_function_is_numbered`: de golden `jutterswet-havengeld` heeft het:
   `havengeld` met `pad_havengeld` en `waarde_havengeld`, `kadegeld` met `pad1_kadegeld`, `pad2_kadegeld` en
   `waarde_kadegeld`.
2. `the_rule_part_is_the_ident_of_the_rule_with_what_drools_reads_in_a_name`: een document in de test met acht regels
   "Een Jutter is doelN indien hij lid is", de randgevallen 1, 2, 3, 4 en 6 (`2 jarige termijn`, `— § —`, `a-b`, `a/b`
   zonder voorwaarde en zonder functies, `a b`, `beëindiging`, `penninggerechtigd 𝔘`, `artikel 2² en ȷ`); de case met
   een Jutter met en een zonder `lid` speelt op beide takken.
3. `a_rule_name_with_letters_outside_ascii_names_its_functions_with_them`: randgeval 5; de drie cases van de twee
   documenten (`case.json`, `case-input.json`, `case.json`) spelen op beide takken.
4. `one_plan_gives_one_text`: (c), hieronder.

Rood op `f867513d` (`red-1.log`): 1, 2 en 3, elk op de tabel met `r<n>_<soort><plaats>`; 4 is daar groen (één plan gaf
ook toen één tekst).

(c) Eén plan geeft één tekst. `DrlRenderingWitnessTest.the_text_of_every_golden_compiles_or_the_rendering_refuses_a_kind`
rendert het plan van elke golden (279) nu twee keer en eist dezelfde tekst en dezelfde stukken; de vormenplannen deden
dat al (`assertEquals(text, DrlRendering.text(plan))`). `DrlFunctionNamesWitnessTest.one_plan_gives_one_text` bouwt
het document van test 2 twee keer op de tak `DRL_TEXT` en één keer op `EXEC_MODEL` (drie analyses, drie weergaven):
dezelfde tekst en dezelfde stukken. Beide zijn groen op `f867513d`; mutaties M6 en M7 maken ze rood.

Mutaties van de wijziging in `DrlRendering.java`, in een worktree op `7a25a21d` (niet gecommit; na elke mutatie
`git checkout`, daarna de worktree verwijderd; recept `werk/drl-function-names-from-rule/mutations/run.py` met de
vervangen tekst per mutatie, logs `logs/mutation-M*.log`). Gedraaid: de vijf DRL-getuigen hieronder. Geen mutatie gaf
een compileerfout:

| mutatie | corpus (a) | (b) 1 | (b) 2 | (b) 3 | (b) 4 / (c) | (c) corpus | andere |
|---|---|---|---|---|---|---|---|
| M1 elke soort een nummer, ook de enige | rood: 4833 buiten de vorm | rood | rood | rood | groen | groen | 7 van `DrlConditionFunctionsWitnessTest`, 2 van `DrlPiecesWitnessTest`, 5 vormenplannen |
| M2 geen soort een nummer | fout: twee functies heten `beta_deelbaarTariefFilterfoutInVouw6` | fout | groen | groen | groen | fout | 3 van `DrlConditionFunctionsWitnessTest`, 2 vormenplannen en `the_carried_rules_of_every_golden_compile` fout |
| M3 het regeldeel is `Namen.ident` ongefilterd | groen | groen | fout: tekst compileert niet | groen | fout | groen | `DrlPiecesWitnessTest.a_place_counts_utf16_code_units` fout |
| M4 geen nummer achter een gedeeld regeldeel | groen | groen | fout: twee functies heten `toets_aB` | groen | fout | groen | |
| M5 een regel zonder functies telt mee | groen | groen | rood: `toets_aB_3` | groen | groen | groen | |
| M6 het regelnummer loopt door over weergaven | groen | groen | rood: `toets_aB_5` | groen | rood | groen | |
| M7 het soortnummer loopt door over regels en weergaven | rood: 526 | rood | groen | groen | groen | rood: elke golden met een genummerde soort | 2 vormenplannen ("één plan geeft één tekst"), 1 van `DrlConditionFunctionsWitnessTest` |
| M8 alleen ASCII blijft van het regeldeel | groen | groen | rood: `toets_beindiging` | rood: `waarde_iVanEenJutter` | groen | groen | `toets_nLid0` in `DrlConditionFunctionsWitnessTest` |

Zonder mutatie is alles groen.

(d) Geen gedragsverandering.

- `git diff f867513d -- backends/drools/src/main` raakt alleen `DrlRendering.java`; het plan en het exec-model zijn
  dezelfde. `BranchDumpsCorpusWitnessTest` is ongewijzigd en groen in de suite.
- Alleen de namen veranderen: met een probe-testklasse (niet gecommit, dezelfde als in `drl-no-passthrough-functions`)
  de tekst en de stukken van elke golden op de tak `DRL_TEXT`, zonder en met lezingen, op `7a25a21d`, naast die van
  `1ba36560` (main code gelijk aan `f867513d`) uit `werk/drl-no-passthrough-functions/probe/after-corpus`. Per tekst
  dezelfde regels met evenveel functies; de oude tekst met elke oude naam als heel woord vervangen door de nieuwe, in
  de volgorde van de stukken, is bytegelijk aan de nieuwe tekst: 564 van 564 teksten; de 94 weigeringen zijn
  bytegelijk. Op v2 hetzelfde (1 van 1). Script `werk/drl-function-names-from-rule/probe/renamed.py`.
- Gate 12: zie hieronder.

(e) Aangepaste verwachtingen, oud → nieuw:

- `DrlPiecesCorpusWitnessTest.EXPECTED_UNREACHED`: `r2_binder2` → `binder_defaultsAJutterBergingsforfaitDefault0`
  (`jutterswet-bergingstoeslag`, `defaults A:Jutter.bergingsforfait: default 0`). `EXPECTED_MIN` krijgt de twee vloeren
  van (a); de andere vloeren blijven.
- `DrlPiecesWitnessTest`: in `a_composite_condition_is_the_body_of_its_test` `r1_toets0` →
  `toets_defaultsKJutterUitgeslotenDefault000` en `r1_afloop1` → `afloop_defaultsKJutterUitgeslotenDefault000`; in
  `the_fall_back_of_a_default_carries_the_functions_of_its_test_and_its_value` `r3_toets0` →
  `toets_defaultsKJutterUitgeslotenTerugval0` en `r3_afloop1` → `afloop_defaultsKJutterUitgeslotenTerugval0`; het
  orakel `CALL` las alleen `r<n>_<soort><plaats>` en leest nu `<soort><nummer>_<regeldeel>[_<k>]`. De vrije labels
  `r1_toets0` en `r2_afloop1` van de recordtest `places_that_do_not_add_up_are_a_bookkeeping_fault_of_the_rendering`
  zijn geen namen van de weergave en bleven.
- `FormsPlan`: in `FORMS` de fragmenten `_pad`, `_beta`, `_samengesteld`, `_zonderfout`, `_vouwlezing`,
  `_kwantorlezing`, `_let`, `_binder`, `_lijst`, `_deponering`, `_lezing` → `pad1_alleTermen(`, `beta_alleTermen(`,
  `samengesteld_alleTermen(`, `zonderfout1_alleTermen(`, `vouwlezing1_alleTermen(`, `kwantorlezing_alleTermen(`,
  `let1_alleTermen(`, `binder1_alleTermen(`, `lijst_schrijvers(`, `deponering_deponeringVanEenToets(`,
  `lezing_lezing(`; in `FOLD_FORMS` `_matrix`, `_lidtoets`, `_plaats` → `matrix_oordeel(`, `lidtoets_lidtoets(`,
  `plaats_lidtoets(`; in `CHANNEL_FORMS` `_componenten` → `componenten_sleutel(`; in `DEFAULTS_FORMS` `r0_afloop0`,
  `r1_afloop0`, `r2_afloop0`, `r4_conflict0` → `afloop_default0`, `afloop_default1`, `afloop_default2`,
  `conflict_verschil0`; in `CREATION_FORMS` `r0_creatie0`, `r1_creatie0` → `creatie_creatieMetInitialisaties`,
  `creatie_creatieZonderInitialisaties`. Het `_`-fragment las "een functie van deze soort"; het nieuwe fragment noemt
  de functie zelf, want `_lezing` zou nu ook in `vouwlezing_…` staan.
- `DrlConditionFunctionsWitnessTest` (niet in de lijst van de opdracht, wel een DRL-getuige in `cases/` die namen
  pint): elke `r<n>_<soort><plaats>` naar de nieuwe naam; de regels van de golden (`toets_drukte`,
  `vouwlezing_drukte`, `samengesteld1_drukteFoutInDeVoorwaarde` … `vouwlezing2_drukteDeponeringInDeVoorwaarde`,
  `toets_eenmaligKaveletiketSleutelVanKavel1`, `componenten_…`, `afloop_defaultsAJutterBergingstoeslagDefault1`,
  `let_`/`binder_defaultsAJutterBergingsforfaitDefault0`, `toets_eenmaligVondstnummerSleutelVanStrandvondst1`,
  `toets_zwareVondstenBijdrageAanVouw1`, `beta_`/`toets_`/`fout_`/`plaats_zwareVondstenFilterfoutInVouw1`,
  `toets_ongemerktAantalBijdrageAanVouw0Selectie0`) en van de documenten in de test (`"r" + i + "_toets0"` →
  `"toets_éénLid" + i`, `r1_toets0` → `toets_geenLidOnderTenMinsteEen`, `r0..r3_toets0` → `toets_genest`,
  `toets_omhuld`, `toets_leeg`, `toets_gelijk`). In `drukte: fout in de voorwaarde` waren het `r3_samengesteld1` en
  `r3_samengesteld4`, nu `samengesteld1_…` en `samengesteld2_…`: het nummer telt binnen de soort, niet de plaats.
- Drie getuigen die de oude vorm met een patroon lazen, rood in de eerste suite-run (`logs/suite-1.log`):
  `DrlTextWitnessTest` (`PATH_IN_BINDING`, `PATH_IN_FIELD_TEST`: `r\d+_pad\d+` → `pad[0-9]*_[\p{L}\p{N}_]+`, met
  ervoor geen letter, cijfer of `_` in het tweede), `WholePeriodCountFilterWitnessTest` (`WHOLE_PERIOD`: `r\d+_toets\d+`
  → `toets[0-9]*_[\p{L}\p{N}_]+`), en `KeyConventionWitnessTest.the_drl_text_also_normalises_the_union_key`: die eist
  dat de geschreven spelling `Vondstnummer` van een sleutel de DRL-tekst niet haalt, en de tekst bevat nu
  `toets_eenmaligVondstnummerSleutelVanStrandvondst1`, de camel case van de regel "eenmalig vondstnummer: sleutel van
  Strandvondst 1", geen sleutelplaats. De helper `drl` van die klasse haalt nu de namen van de functies (uit
  `drlPieces()`) uit de tekst voordat de twee tests van sectie 5 zoeken; de rest van de tekst wordt gezocht als
  voorheen. Zie voorlopig besluit 4.

(f) De javadoc van `DrlRendering` (de bullet over de namen, en `namePart`, `rulePart`, `functionNames`) en de
README-sectie "The DRL-text branch" zeggen de vorm. In de bullet stond `{@code depositFlag}` als local van een gevolg;
de local heet `sonde` (`DEPOSIT_FLAG`), en zo staat het er nu, zoals de kaart hem noemt.

(g) De meting op v2/herleving-ww (regels `ece012e`; bronnen in resolve-volgorde, `fundament/rules/*.rgs` gesorteerd,
dan `herleving-ww/rules/*.rgs` gesorteerd; het IR gemaakt met het frontend van deze worktree, `--ir`, bytegelijk aan
`werk/drl-no-passthrough-functions/v2/ir.json`; de tekst met de probe op `7a25a21d`, zonder lezingen): 339 regels, 416
functies in 209 regels. Het regeldeel: mediaan 71, ten hoogste 119 tekens (per regel met functies). De hele naam:
mediaan 80, ten hoogste 132 tekens, ten minste 21 (per functie). 89 namen hebben een nummer van hun soort, 25 een
teken buiten ASCII (`ë`, `ó`), geen twee regels met functies delen een regeldeel en geen code point valt weg. Het stuk
van `default 1` staat hierboven.

### Voorlopige besluiten

1. Twee weergaven per regel. Een naam staat in de tekst op het moment dat de functie geschreven wordt, en pas het eind
   van de regel zegt of haar soort een nummer krijgt. Een plaatsvervanger in de tekst die na de regel vervangen wordt,
   kan in een zin of een stringliteral ook staan; een tweede weergave raakt geen tekst die niet van de weergave is.
   De kosten: de weergave rendert elke regel twee keer (de compilatie van Drools blijft één keer). Dezelfde getuigen op
   `f867513d` en op `7a25a21d`: `DrlPiecesCorpusWitnessTest` 14 s en 13 s, `DrlRenderingWitnessTest` 29 s en 27 s.
2. Een gedeeld regeldeel krijgt bij alle regels een nummer, zoals de eigenaar het voor een soort besliste, en alleen
   regels met functies tellen, want alleen die dragen een naam; een regel zonder functies verschuift de nummers van de
   andere dan niet. Het nummer staat na een `_` achter het regeldeel, omdat een regeldeel op een cijfer kan eindigen.
   Op het corpus en op v2 komt het niet voor.
3. Een code point dat Drools niet in een naam leest, valt weg, in plaats van een escape (`u1D518`) of een weigering.
   Weglaten houdt de naam leesbaar en de nummering van randgeval 3 houdt hem uniek; een weigering zou een document
   weigeren dat vóór de kaart speelde (`penninggerechtigd 𝔘`). Wat Drools leest, vraagt `namePart` aan de DRL6-lexer van
   Drools 8.44 zelf (`org.drools.drl.parser.lang.DRL6Lexer`, uit `drools-drl-parser`, al op het klassenpad van de
   backend) in plaats van een gekopieerde tabel: een andere versie van Drools met een andere lexer geeft dan zijn eigen
   antwoord, en een lexer die er niet meer is, geeft een compileerfout. Een parameter-global of een bindingsvariabele
   (`$…`) uit een naam met zo'n teken controleert de weergave niet; dat is buiten deze kaart.
4. `KeyConventionWitnessTest`: de check "de geschreven spelling haalt de tekst niet" zocht in de hele tekst en vond
   nu de camel case van een regelnaam in een functienaam. De namen van de functies gaan eruit voordat de test zoekt;
   een sleutelplaats, een stringliteral, een veld en een getter worden gezocht als voorheen. Dat is een verandering
   van een test die een gedrag van deze kaart raakt (de namen), geen verzwakking van wat hij toetst.
5. De twee controles in `render` (twee functies van één naam, een functie die heet als een global) zijn fail-fast
   voor een fout in de weergave; de vorm sluit beide uit.

### Open punt (door de hoofdsessie afgehandeld)

De javadoc van `TmsTranspiler.drlPieces` (r. 905, `TmsTranspiler.java`) zei nog "the rendering names a function after
its rule's place in the plan", en sinds `drl-no-passthrough-functions` ook niet meer precies dat elke toets een functie
is. De hoofdsessie heeft die javadoc in een eigen commit op de kaarttak bijgewerkt ("after its kind and its rule", en
"every test and every value … that tests or computes something"): alleen commentaar, geen code. Daarmee raakt de diff
van `src/main` naast `DrlRendering.java` ook `TmsTranspiler.java`, en verbreedt de may-touch zich met die javadoc;
acceptatie (d) geldt voor de code.

### Voor de toolchain-kaart `drl-readable-functions-pin`

`PlaygroundDrlRulesTest` (r. 732–750) pint `r11_toets0` en `r11_samengesteld1`; na beide kaarten heet een functie
`<soort>_<regeldeel>` naar de regel van het stuk. `docs/ENGINE.md` r. 318 (`r118_afloop2`) wordt
`afloop_<regeldeel>` van die regel. De velden van `drlRules` veranderen niet.

### Suite en gate 12

- De JUnit-suite van `backends/drools` (`mvn -B -o test`) op de stand van `7a25a21d`: 1702 tests, 0 fouten, 0
  overgeslagen; de 1698 van `f867513d` plus vier in `DrlFunctionNamesWitnessTest`. `BranchDumpsCorpusWitnessTest`
  ongewijzigd en groen. Log `werk/drl-function-names-from-rule/logs/suite-2.log`. De eerste run (`suite-1.log`, vóór
  de drie getuigen met een patroon van (e)) had 3 rode tests van 1702.
- Gate 12 (`scripts/check-drools.sh`) op `7a25a21d`: PASS. 279 goldens gespeeld op de exec-model-tak, gelijk aan
  `EXPECTED_PLAYED=279`; kanaalvloer 50 niet-gespeelde goldens bij naam; de DRL-teksttak speelt 279, elke statusregel
  gelijk aan die van de exec-model-tak (sweep 225 s); de suite van de gate 1702 tests (vloer 1573), nul
  overgeslagen; `check-drools-codes` PASS (294 codes). Het rapport (31 regels) is regel voor regel gelijk aan dat van
  gate 12 op `c786cc5f` (`werk/drl-no-passthrough-functions/logs/gate12-2.log`), op de wandkloktijd van de sweep (222 s
  daar) en het aantal tests (1698 daar) na: dezelfde weigeringen met dezelfde codes per vorm, dezelfde aantallen. Log
  `werk/drl-function-names-from-rule/logs/gate12-1.log`.
- De volledige gate-set draaide hier niet; die draait de hoofdsessie vóór de landing (acceptatie (h)). Gate 13 en 17
  gebruiken de DRL-teksttak ook; gate 13 vergelijkt statusregels, en de lezingen van gate 17 komen uit het plan
  (`Consequence.Reading`), niet uit de namen van de functies.
- De commits na `7a25a21d` veranderen alleen de kaart.
