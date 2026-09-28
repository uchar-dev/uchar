# drools-exec-voorwaarde-genavigeerde-afleiding: een voorwaarde op een afgeleide waarde van een genavigeerd object vuurt nooit

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie (`bin/regelspraak check|scenario|cases`)
en de eigen uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/k4/` (alleen lezen). Een stil fout antwoord: gate 12 heeft
geen golden van deze vorm.*

## Aanleiding

Een gewone regel (gelijkstelling of berekening) op een object, met een
voorwaarde die via een to-one rol een AFGELEIDE waarde van het genavigeerde
object leest, vuurt in de Drools-exectak nooit: het doel blijft leeg en de
dekking zegt `NIET_GERAAKT`, terwijl `check` exit 0 geeft en ref en clingo de
verwachting halen. Zonder de voorwaarde vuurt de regel; met dezelfde lezing
in de WAARDE van de regel ook; met een voorwaarde op een INVOERwaarde van het
genavigeerde object ook (meting van de bouwer). De vorm is gewoon in een
regelset: een periode of grond die iets van "zijn recht" leest nadat een
andere regel dat op het recht heeft afgeleid.

## Minimale reproductie

`056df376`, distributie; `scenario` speelt de exectak.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de meldingsdatum Datum in dagen;
        de afgeleide datum Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de uitkomst Datum in dagen;
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel afgeleid
        geldig altijd
            De afgeleide datum van een Jutter moet gesteld worden op de meldingsdatum van de Jutter.

    Regel uitkomst
        geldig altijd
            De uitkomst van een Vondst moet gesteld worden op de meldingsdatum van de jutter van de Vondst
            indien de afgeleide datum van de jutter van de Vondst gevuld is.

    Scenario navigatie
    Gegeven is de volgende situatie:
    een Jutter J
    - de meldingsdatum is 01-03-2026
    een Vondst V
    - de jutter is J
    Met als verwacht resultaat:
    - de afgeleide datum van J is 01-03-2026
    - de uitkomst van V is 01-03-2026

    nu: check exit 0; scenario exit 1, "afgeleid : GEVUURD, uitkomst : NIET_GERAAKT", uitkomst leeg;
        cases + ref: `oracle: PASS (2 expectations)`; clingo: `clingo: PASS (2 expectations)`;
        zonder de indien-regel: scenario PASS.

## Acceptatie

- (a) Meet de vormruimte vóór de fix: voorwaarde op een afgeleid attribuut
  van een to-one genavigeerd object (deze reproductie); op een afgeleid
  kenmerk; twee rollen diep; via een to-many rol (kwantor of vouw); de
  afleiding uit een defaultsgroep in plaats van een gewone regel; en de
  DRL-teksttak op dezelfde documenten. Per vorm: ref, clingo, exec, DRL.
- (b) De oorzaak in de exec-tak (vermoedelijk: de voorwaarde op het
  gejoinde object wordt niet opnieuw geëvalueerd wanneer de afgeleide
  waarde later binnenkomt, of de join leest een kopie) en de fix, zodat de
  regel vuurt zodra de afleiding er is; rood-eerst JUnit-getuige met deze
  reproductie.
- (c) Een golden (werknaam `jutterswet-meldingsketen`) met deze vorm en de
  gemeten varianten die de fix dekt, handgerekend, in ref ≡ clingo ≡
  Drools-exec; `EXPECTED_PLAYED` +1 in `scripts/check-drools.sh` als
  **voorstel**. Een vorm die na de fix nog niet speelt, staat luid in het
  kanaal (EXCLUDE met reden), nooit als stil fout antwoord.
- (d) Corpussweep: alle goldens houden hun uitkomst (gate 12 played-telling
  en kanaal), beans byte-gelijk waar de fix de beangeneratie niet raakt;
  alle zestien gates groen (scripts met `#!/usr/bin/env bash` onder bash).
- (e) Een regel in `backends/drools/README.md` of de plek waar de exectak
  zijn beperkingen noemt, als er een beperking overblijft.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: parallel met de kaart
drools-subtype-in-supertype-rol (zelfde backend, andere bestanden: die raakt
het beanmodel en de rollen, deze de regelevaluatie); de hoofdsessie landt
serieel en rebaset.

## Result (2026-09-28, bouwer)

Branch `card/drools-exec-voorwaarde-genavigeerde-afleiding` in de worktree van de kaart, gebouwd op `396097de`
en daarna op verzoek van de hoofdsessie gerebased op main `08f55ce9` (frontend-extensie-herdeclaratie en
drools-subtype-in-supertype-rol geland); zie "Rebase" onder.
Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, de Drools-jar (`java -jar <jar> ir.json case…`,
`--drl` voor de teksttak) en de frontend (`Resolver --ir … --cases …`). "Vóór" is de jar van de claim
(backend gelijk aan `396097de`), "na" de jar met de fix; de hashes vóór de rebase staan tussen haken bij de
commits onder.

### Oorzaak en fix

**Oorzaak.** `ExecModelTranspiler.voorwaarde` zet elke toets van een voorwaarde op het patroon van het
onderwerp. Drools draait zo'n toets alleen wanneer het onderwerp wordt ingevoegd of bijgewerkt. Een lezing via
een to-one rol leest een ander feit; een regel die daar later een waarde afleidt, werkt dat feit bij en niet het
onderwerp, dus de toets draait niet opnieuw. Een positieve toets (gevuld, een vergelijking, een kenmerk) blijft
dan onwaar en de regel vuurt nooit; een ontkende toets blijft waar en de regel vuurt op de stand bij invoegen. De
waarde van een regel had al een join op het bereikte feit (`navReactiviteit`); de voorwaarde niet. Het vermoeden
van de kaart klopt dus in de eerste helft: de voorwaarde wordt niet opnieuw geëvalueerd. Er wordt geen kopie
gelezen. Alleen een join toevoegen helpt niet: het onderwerp valt al vóór de join uit het alfanetwerk.

**Fix** (`4f3c09dd`, vóór de rebase `2535a8f9`). `regelVoorwaarde` geeft een voorwaarde die via een to-one rol leest (een attribuutlezing
in een operand of de hop van een genavigeerde kenmerkcheck) dezelfde join per navigatie als de waarde heeft
(`navJoins`, gedeeld met `navReactiviteit`; een navigatie die de waarde ook leest krijgt geen tweede join), en
zet de toetsen van de voorwaarde op het onderwerp dat ná die joins opnieuw wordt gebonden met
`from $subject`. Dat patroon draait opnieuw bij elke wijziging van de tupel ervoor, het bereikte feit
inbegrepen; het null-alternatief van de join (geen relatie) blijft staan. Het onderwerppatroon krijgt de
ondoorzichtige ware expr, zodat het klassereactief blijft zoals de toetsen het maakten. Een voorwaarde zonder
zo'n lezing blijft ongewijzigd op het onderwerppatroon. De lowering loopt vóór de joins, zodat haar eigen
weigerreden blijft staan (`RolveldlezerWitnessTest` eist de lezer "genavigeerde kenmerktijdreeks"; een eerste
versie met de joins vóór de lowering gaf daar "genavigeerde kenmerkcheck" en is zo teruggedraaid). Dezelfde gate
gaat naar de begeleidende regels die de voorwaarde van hun gastregel herhalen (deling door leeg, wortel, leeg
startpunt, gevuurd) en naar objectcreatie en feitcreatie: elk zet dezelfde toets op zijn onderwerp en elk bleek
gemeten dezelfde fout te geven (tabel, rijen g4, h1b, h2b, h3). `ExecModelTranspiler.java`: 77 regels erbij, 10
eraf; `FactModelGenerator`, de loader en de rolafleiding zijn niet aangeraakt.

### (a) Vormruimte, vóór en na

Elke vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs, door de frontend naar ir.json en cases),
in `/tmp/v2/k4-bouw/vorm/`. ref en clingo: PASS op elke case, behalve waar genoemd. De DRL-teksttak is vóór en
na gelijk (DrlTranspiler is niet aangeraakt). "zijn jutter" is de navigatie Vondst → jutter (to-one).

| vorm | voorwaarde (in de bron staat de afleiding vóór de lezer, tenzij anders) | exec vóór | exec na | DRL (vóór = na) |
|---|---|---|---|---|
| c1 controle | geen voorwaarde | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| c2 controle | afgeleide lezing in de WAARDE | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| c3 controle | gevuld op een INVOERwaarde van de jutter | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| a1 reproductie | afgeleide datum van de jutter gevuld | FAIL uitkomst leeg | PLAYED 1 | FAIL uitkomst leeg |
| a2 | afgeleide datum van de jutter leeg (afleiding eerst) | FAIL werkelijk 2026-01-01 | PLAYED 2 | FAIL |
| a2b | idem, lezer eerst | FAIL werkelijk 2026-01-01 | **FAIL** (open punt 2) | FAIL |
| o1 | eigen afgeleide datum leeg (afleiding eerst) | PLAYED 2 | PLAYED 2 | PLAYED 2 |
| o2 | idem, lezer eerst | FAIL werkelijk 2026-01-01 | FAIL (open punt 2) | FAIL |
| a3 | later dan een datumliteraal | FAIL | PLAYED 1 | FAIL |
| a4 | later dan een parameter | FAIL | PLAYED 1 | FAIL |
| a5 | groter dan 5 (numeriek, afgeleid) | FAIL resultaat leeg | PLAYED 1 | FAIL |
| a6 | later dan de eigen vinddag | FAIL | PLAYED 1 | FAIL |
| a7 | groter dan een parameter | FAIL | PLAYED 1 | FAIL |
| a8 | (afgeleid plus 1) groter dan 7 | FAIL | PLAYED 1 | FAIL |
| a9 | alle: genavigeerd gevuld + eigen gevuld | FAIL | PLAYED 1 | FAIL |
| a10 | ten minste één: genavigeerd gevuld / eigen leeg | FAIL | PLAYED 1 | FAIL |
| b1 | zijn jutter melder (afgeleid kenmerk) | FAIL | PLAYED 1 | FAIL |
| b2 | zijn jutter niet melder (afleiding eerst) | FAIL werkelijk 2026-01-01 | PLAYED 2 | FAIL |
| b2b | idem, lezer eerst | FAIL | PLAYED 2 | FAIL |
| k1 twee rollen diep | afgeleide datum van de jutter van de gemelde vondst | EXCLUDE voorwaarde buiten Phase-B scope | idem | EXCLUDE MARKER TODO voorwaarde |
| d1 to-many | kwantor: ten minste 1 van de vondsten, afgeleid gewicht | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| d2 to-many | som van het afgeleide gewicht van alle vondsten | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| d3 to-many | aantal vondsten die (afgeleid) zwaar zijn | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| e1 defaultsgroep | afgeleide datum uit `Uitzondering op` | FAIL (2 cases) | PLAYED 2 | EXCLUDE defaultszin |
| e2 defaultsgroep | genavigeerde voorwaarde in een uitzondering | PLAYED 1 | PLAYED 1 | EXCLUDE defaultszin |
| f1 kenmerktoekenning | gevuld, doel een kenmerk | FAIL | PLAYED 1 | FAIL |
| f2 initialisatie | gevuld, `geïnitialiseerd` | FAIL | PLAYED 1 | FAIL |
| g1 consistentie | vinddag eerder dan de meldingsdatum van de jutter | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| g4 §6.5 onder de voorwaarde | eigen deeltal / eigen deler, deler leeg | FAIL consistent + 5.00 leeg | PLAYED 2 (met alleen de gastregel: case 1 FAIL) | FAIL |
| g2 §6.5 onder de voorwaarde | 10 gedeeld door de deler, deler leeg | FAIL consistent + 5.00 leeg | FAIL consistent (open punt 3) | FAIL |
| g3 §6.5 zonder voorwaarde | 10 gedeeld door de deler | FAIL consistent | FAIL consistent (open punt 3) | FAIL |
| h1 objectcreatie | afleiding eerst | PLAYED 1 (toeval, zie onder) | PLAYED 1 | FAIL meldingaantal 0 |
| h1b objectcreatie | afleiding laatst | FAIL meldingaantal 0 | PLAYED 1 | FAIL |
| h2 gevuurd | "regelversie uitkomst is gevuurd" over a1 | FAIL uitkomst leeg, afgehandeld afwezig | PLAYED 1 | FAIL |
| h2b gevuurd | idem, de gastregel schrijft niets nieuws | FAIL afgehandeld afwezig (ook met alleen de gastregel) | PLAYED 1 | FAIL |
| h3 feitcreatie | afgeleide aanwijzing van de strandvoogd (afleiding laatst) | FAIL meldingaantal 0 | PLAYED 1 | FAIL |
| h3c feitcreatie | idem op een invoerwaarde | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| h4 verdeling | genavigeerd in de voorwaarde | EXCLUDE | EXCLUDE | EXCLUDE (open punt 5) |

Uitzonderingen op "ref en clingo PASS": h2/h2b (`is gevuurd`: ref `EVALUATION FAULT: 'is gevuurd' is not
implemented`, clingo `TRANSLATION FAULT`), h4 (de frontend lost de navigatie in een verdelingsvoorwaarde niet op:
verifier `WF-034 FAIL … 'afgeleide aanwijzing van de strandvoogd' is neither an attribute nor a characteristic of
'Berging'`). h1 speelde vóór de fix alleen doordat de telregel de Vondst bijwerkte nadat de afleiding er was.

Conclusie: vóór de fix faalde elke voorwaarde die via een to-one rol een afgeleide waarde leest, in elke
lowering die haar als toets op het onderwerp zette (regel, kenmerktoekenning, initialisatie, begeleider,
objectcreatie, feitcreatie), tenzij een toevallige bijwerking van het onderwerp de toets opnieuw liet draaien.
Via een to-many rol (kwantor, som, aantal) en in een defaultsgroep of consistentieregel speelde het al: daar
reageert de accumulate op het elementfeit, of beslist de consequentie bij het vuren.

### (b) Getuigen

`GenavigeerdeVoorwaardeWitnessTest` (exectak, acht rijen, documenten in de klasse op Jutterswet-materiaal;
het anker kustwacht en de basis synth-navigatiepremie uit het corpus, alleen gelezen). Rood op de claim
(`35b666f8`, vóór de rebase `ef91ae4c`, de eerste zes rijen):

    Tests run: 6, Failures: 5, Errors: 0, Skipped: 0
    de_deling_door_leeg_meldt_zich_onder_dezelfde_voorwaarde:71 … <case: verwacht inconsistent, werkelijk consistent>
    de_regel_vuurt_zodra_de_afleiding_op_het_genavigeerde_object_er_is:41 … <case: attribuut uitkomst: verwacht 2026-03-01, werkelijk leeg>
    een_feitcreatie_ziet_de_afgeleide_waarde:146 … <case: attribuut meldingaantal: verwacht 1, werkelijk 0>
    een_objectcreatie_ziet_de_afgeleide_waarde:103 … <case: attribuut meldingaantal: verwacht 1, werkelijk 0>
    het_anker_kustwacht_speelt:156 jutterswet-kustwacht case1.json … <case: kenmerk rustwacht: verwacht afwezig, werkelijk aanwezig>

De zesde rij (`zonder_relatie_en_zonder_afleiding_beslist_de_lege_lezing`) is een flank, groen vóór en na: zij
houdt het null-alternatief van de join vast. Twee rijen kwamen na de fix, elk gemeten op de hele klasse tegen de
transpiler van de claim (acht rijen: zeven rood, de flank groen):
`de_gevuurd_begeleider_volgt_de_voorwaarde_van_zijn_gastregel` (`df1d7707`: `kenmerk afgehandeld: verwacht
aanwezig, werkelijk afwezig`, ook rood met alleen de gastregel hersteld, net als de delingsrij) en
`een_genavigeerde_kenmerkcheck_over_een_verdrongen_rolveld_weigert` (`02366f30`: `Unexpected exception type
thrown, expected: <…CaseRunner.Excluded> but was: <org.drools.modelcompiler.constraints.ConstraintEvaluationException>`).
Na de fix: de klasse 8 tests, 0 failures; de suite 870 tests, 0 failures, 0 skipped (862 op de claim; vloer
239); na de rebase 881 (main bracht er 11 bij), 0 failures, 0 skipped.

### (c) Golden `jutterswet-meldingsketen`

Vijf regels op de Vondst lezen in hun voorwaarde een afgeleide waarde van hun vinder (feittype
`vondstmelding`): aanmelddatum (gevuld), controledatum (alle: eigen gewicht > 0 en genavigeerd later dan de
eigen vinddag), tijdig aangemeld (eerder dan de parameter meldtermijngrens), meldingsgewicht (gewicht gedeeld
door de weegfactor, onder "zijn vinder erkend is", met de §6.5-fout) en ongemeld ("zijn vinder niet erkend
is"). De erkende meldingsdatum komt uit een defaultsgroep (`Uitzondering op erkende meldingsdatum: herstelde
melding`), het kenmerk erkend daaruit. In bron.rgs staan de lezers eerst. Handgerekend in NOTE.md; toegelaten
via `scripts/admit.sh` (`admit: ADMITTED corpus/golden/jutterswet-meldingsketen (3 case(s); verifier-clean,
ref = clingo, no-op refuted, gates green)`, met het gate-12-voorstel in de werkboom).

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-erkende-melding | 13 | PASS | PASS | FAIL ongemeld aanwezig (V1) | PASS | EXCLUDE defaultszin | MISMATCH V1.meldingsgewicht |
| case2-herstelde-melding | 13 | PASS | PASS | FAIL ongemeld aanwezig (V3, V4) | PASS | EXCLUDE | MISMATCH inconsistent verwacht |
| case3-geen-melding | 8 | PASS | PASS | PASS | PASS | EXCLUDE | MISMATCH V5 mist ongemeld |

Replay per uitvoerder: `oracle: PASS (13 expectations)`, `(13)`, `(8)`; `clingo: PASS` idem; exectak
`PLAYED 3`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`.
Vóór de fix faalden alleen de ongemeld-verwachtingen; de vier andere lezers slaagden daar door toeval: de foute
ongemeld-schrijving werkte de Vondst bij na de afleidingen. Zonder de ongemeld-regel antwoordde de exectak vóór
de fix in case 1 de aanmelddatum, controledatum en meldingsgewicht van V1 leeg en tijdig aangemeld afwezig, en
in case 2 beide aanmelddatums, de controledatum van V3 en het meldingsgewicht van V4 leeg en de case consistent.
Na de fix speelt de golden ook met de afleidingen vóór de lezers (gemeten). Niet in de golden, met reden: de
leeg-toets op een afgeleide waarde (a2/a2b: hangt van de regelvolgorde af, open punt 2) en de ketting van twee
rollen (k1: geweigerd). De ontkende genavigeerde kenmerkcheck staat er wel in (ongemeld) en ook in het anker
kustwacht.

### Gate-12-voorstel (`f76ae08f`, `scripts/check-drools.sh`, beschermd pad)

- `EXCLUDE_KNOWN`: de regel `anchor/jutterswet-kustwacht|v0-divergence … NEGATION-as-failure is not stratified …`
  vervalt; een commentaarblok zegt waarom (het stratum zette rustwacht al na risicopost; de genavigeerde
  kenmerkcheck werd niet opnieuw getoetst). De lijst is leeg.
- `known_reason` en de stale-lus: `for e in "${EXCLUDE_KNOWN[@]:-}"; do [ -n "$e" ] || continue; …` (leest een
  lege lijst ook onder bash 3.2, de vorm die het script elders al gebruikt).
- `EXPECTED_CHANNEL`: de regel `anchor/jutterswet-kustwacht|DIVERGE|…` vervalt.
- `EXPECTED_PLAYED`: +2 (kustwacht en jutterswet-meldingsketen), met een redenblok. Op de claim was dat 219 →
  221; na de rebase 220 → 222, bovenop de 220 van main (jutterswet-wrakmelding), het redenblok na dat van
  wrakmelding.
- `DRL_CHANNEL`: `"jutterswet-meldingsketen|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this
  branch"` in klasse (A); de kustwacht-regel (FAIL) krijgt de oorzaak als reden: "a navigated characteristic
  check in a condition sits in the Wachter's pattern and is not tested again when the post becomes a risicopost:
  rustwacht stays present and gevarenwacht absent in both cases, …"; kopteksten: klasse (A) 35 regels, gat 33
  (222 − 189 na de rebase), zes klassekoppen A 35, B 37, C 10, D 2, E 3, F 3 = 90, "eighty-five of the ninety".
- `EXPECTED_DRL_PLAYED` blijft staan (188 op de claim, 189 na de rebase); `EXPECTED_MIN_TESTS` blijft 239.

Wat deze kaart in het script verandert, voor een volgende rebase: `EXPECTED_PLAYED` +2, `EXCLUDE_KNOWN` leeg,
kustwacht uit `EXPECTED_CHANNEL`, één regel erbij in `DRL_CHANNEL` (A) met de kopteksten van (A), en de
kustwacht-regel in (D) herschreven.

### (d) Sweep

Over alle 277 goldens van de claim, elke golden met al zijn cases, statusregel per golden vergeleken:
- exectak: precies één regel verschuift, `anchor/jutterswet-kustwacht` van `FAIL case1.json: kenmerk rustwacht:
  verwacht afwezig, werkelijk aanwezig | case2.json: …` naar `PLAYED 2`; alle andere 276 regels (ook elke
  EXCLUDE-reden) byte-gelijk; PLAYED 219 → 220.
- DRL-teksttak: alle 277 regels byte-gelijk, PLAYED 188.
- beans: een SHA-256 over de gesorteerde gegenereerde beanbronnen (`FactModelGenerator.genereer`) per golden,
  jar vóór en jar na: byte-gelijk voor alle 240 goldens die de loader leest (37 loader-weigeringen, gelijk).
Met de nieuwe golden, op de claim: gate 12 `played 221`, `DRL-text branch played 188`.

### Rebase

`git rebase main` naar `08f55ce9`. Eén conflict, in `scripts/check-drools.sh` bij `EXPECTED_PLAYED`: opgelost
door de tellingen op te tellen (220 van main + 2 = 222) met het redenblok van wrakmelding eerst en dat van deze
kaart erna ("220 -> 222"); de kopregel van klasse (A) telt nu tegen 222 − 189. `EXPECTED_DRL_PLAYED` blijft 189
(de nieuwe golden speelt niet in de teksttak, kustwacht ook niet). De commitboodschap van het voorstel noemde
219 → 221 en 188; zij is zonder interactieve rebase herschreven (cherry-pick op dezelfde boom) naar 220 → 222 en
189. Na de rebase opnieuw gemeten:
- de suite: 881 tests, 0 failures, 0 skipped;
- sweep over alle 279 goldens, jar van main tegen jar van de kop: exectak precies twee regels anders,
  `anchor/jutterswet-kustwacht` (FAIL → `PLAYED 2`) en `jutterswet-meldingsketen` (FAIL `kenmerk ongemeld …` →
  `PLAYED 3`), PLAYED 220 → 222; DRL-tak alle 279 regels gelijk, PLAYED 189; beans byte-gelijk voor alle 242
  geladen goldens (37 loader-weigeringen);
- de 38 vormen van (a): exectak en DRL-tak per vorm gelijk aan de meting vóór de rebase.

### Gates

Alle zestien in de volgorde van `gates.yml`, op de gerebasede kop `02366f30` (bash voor de zes `#!/usr/bin/env bash`-scripts, sh
voor de rest, `eval $(opam env)` vooraf, catala aanwezig, plus `dune build @ref/runtest`). Vóór het werk, op de
claim, waren ze ook alle zestien groen (gate 12: played 219, DRL-text 188, JUnit 862).

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 318 file(s) validate and round-trip, floor 192; 41720 object(s) in 819 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 791 case file(s) validate)
    check-verifier: PASS (318 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 751 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    (dune build @ref/runtest: exit 0)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 751 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (707 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (990 tests, floor 173); 106 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27
    check-drools: PASS
      played 222  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 54  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      value-divergence 0  (listed with reason — the exec-model differs from ref/clingo)
      channel floor 57 non-PLAYED goldens pinned by name; played matches the pinned 222
      DRL-text branch played 189  (its OWN count — the narrower branch, not the square; pin 189)
      DRL-text floor 90 non-PLAYED goldens pinned by name; sweep cost 152s wall clock
      JUnit 881 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 242 of 279 goldens compared, 137 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: PASS
    check-scenario: PASS — 13 scenario golden(s) (floor 7) regenerate 50 case file(s) (floor 31) and their ir.json byte-identically

### (e) README

`backends/drools/README.md` (`46dbbb78`), sectie "Recorded: a condition that reads through a role": wat de
exectak nu doet en de drie beperkingen die blijven (de DRL-tak; de leeg-toets op een afgeleide waarde met de
lezer vóór de schrijver, eigen en genavigeerd gelijk; twee of meer hops, behalve de leeg- en gevuld-toets over de
hele periode).

### Afwijkingen van de kaart

1. De fix gaat verder dan "een gewone regel": ook de begeleiders (deling, wortel, leeg startpunt, gevuurd), de
   objectcreatie en de feitcreatie. Reden: ze zetten dezelfde toets op hun onderwerp en gaven gemeten dezelfde
   fout (g4, h1b, h2b, h3); met alleen de gastregel hersteld bleef de §6.5-fout stil terwijl de gastregel vuurde
   (g4 case 1, `verwacht inconsistent, werkelijk consistent`).
2. `EXPECTED_PLAYED` gaat +2 in plaats van +1: het anker kustwacht speelt nu. Zijn vermelding als "genuine v0
   divergence: negation stratification" noemde het mechanisme verkeerd; `EXCLUDE_KNOWN` wordt leeg.
3. De DRL-teksttak is niet hersteld (de kaart vraagt de exectak). De golden komt daar in klasse (A) terecht omdat
   hij een defaultsgroep draagt; de fout van de DRL-tak blijft in het corpus zichtbaar via kustwacht (FAIL,
   reden herschreven).
4. Een genavigeerde kenmerkcheck waarvan de rol geen eigen beanveld heeft, weigert nu met `genavigeerde
   kenmerkcheck: … (§13.3.9 opslagrichting)`; eerder las hij het verdringende attribuut als relatie, met een
   `ConstraintEvaluationException` bij het vuren zodra dat attribuut gevuld is. Getuige in de klasse; geen golden
   heeft de vorm (sweep).
5. De leeg-toets op een afgeleide waarde zit niet in de golden (open punt 2), de ketting van twee rollen ook
   niet (geweigerd, niet gebouwd).

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/CONSTRUCTS.md` rij 6, de "Open conformance flank (2026-08-29 …)": vervangen door "Closed in the
  exec-model branch (2026-09-28): the kustwacht read fired pre-fixpoint because a navigated characteristic check
  in a condition was tested on the subject's pattern and not again when the reached post became a risicopost; the
  derived stratum was already correct. The exec branch tests such a condition after a far-side join (golden
  `jutterswet-meldingsketen`, anchor `jutterswet-kustwacht` plays). The DRL-text branch keeps the form." En in
  "Recorded open …": "a navigated read in a condition does not react to a later far-side write in either order"
  wordt "… in the DRL-text branch".
- `spec/IR-SPEC.md` ch. 5.5, "Recorded flank (measured 2026-09-23 …)", tweede bullet: aanvullen met "Closed in
  the exec-model branch 2026-09-28: a condition that reads through a to-one navigation is tested after a far-side
  join on the reached object; the DRL-text branch keeps the form." De eerste bullet (de volgordeafhankelijke vorm)
  blijft open.
- `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §9 Q11 punt 3 noemt kustwacht een stratificatiegat; de
  gemeten oorzaak is de niet herhaalde toets.

### Open punten

1. De DRL-teksttak heeft dezelfde fout voor elke genavigeerde vorm uit de tabel (FAIL op a1–a10, a2b, b1, b2,
   b2b, f1, f2, g4, h1, h1b, h2, h2b, h3; kustwacht FAIL; e1 en e2 weigert zij op de defaultsgroep). Een herstel zou dezelfde vorm in DRL-tekst zijn (join per navigatie, toetsen op
   `Vondst(…) from $subject`); dat raakt `DrlTranspiler` en is een eigen kaart.
2. Een toets die op een lege waarde waar is (leeg-toets op een afgeleid attribuut), met de lezer vóór de schrijver:
   de exectak vuurt op de tijdelijke leegte en de schrijving blijft (a2b, o2; eigen en genavigeerd gelijk). Dit is
   de eerste bullet van de IR-SPEC-flank; de fix daarvoor is een salience onder de schrijvers van de operand.
3. Buiten deze kaart gevonden: een deling waarvan het deeltal een literal is ("10 gedeeld door de deler van de
   Vondst") krijgt in geen van beide Drools-takken een §6.5-begeleider; ref en clingo antwoorden inconsistent, de
   Drools-takken consistent (g3, zonder voorwaarde en zonder navigatie). `kwalificeertOperand` laat alleen een
   eigen of genavigeerd attribuut toe.
4. Een voorwaarde over twee of meer rollen wordt geweigerd (`voorwaarde buiten Phase-B scope`), behalve de leeg-
   en gevuld-toets over de hele periode (k1).
5. Een genavigeerde lezing in de voorwaarde van een verdeling lost de frontend niet op: de ir.json draagt de hele
   frase als attribuutnaam en de verifier weigert met WF-034 (h4). Niet in Drools te meten.
6. De begeleider voor het leeg startpunt (§9.9) gaat door dezelfde `regelVoorwaarde` maar heeft geen eigen
   getuige; de deling- en gevuurd-begeleider hebben er een.
7. De distributie-CLI draait op de oude pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

Na de rebase op `08f55ce9` (tussen haken de hash vóór de rebase):

    cf3f978d (227905f3) claim: drools-exec-voorwaarde-genavigeerde-afleiding
    35b666f8 (ef91ae4c) GenavigeerdeVoorwaardeWitnessTest: voorwaarde over een rol, rood
    4f3c09dd (2535a8f9) Exectak: voorwaarde over een rol getoetst na de join op het doel
    3b25a10f (b9ede5b2) golden jutterswet-meldingsketen: voorwaarde op een afgeleide waarde van de vinder
    f76ae08f (15b3f37a) voorstel gate 12: kustwacht speelt, meldingsketen erbij
    df1d7707 (b8458248) GenavigeerdeVoorwaardeWitnessTest: de gevuurd-begeleider onder de voorwaarde
    46dbbb78 (91c0e817) backends/drools/README: wat een voorwaarde over een rol nog niet draagt
    02366f30 (eab71d20) GenavigeerdeVoorwaardeWitnessTest: kenmerkcheck over een verdrongen rolveld

en de commit die dit Result in de kaart zet. Niet gepusht, niet gemerged; de kaart blijft in `backlog/doing/`.
