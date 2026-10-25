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
