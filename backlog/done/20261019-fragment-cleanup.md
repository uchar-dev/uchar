# fragment-cleanup: de fragmenten van de rode cases verdwijnen, code en parameters komen ervoor in de plaats

*Vervolgkaart 5 van `spec/proposals/MESSAGE-CODES.md` (§7), stap 7 van het programma "Engelse namen". Raakt `spec/`,
het case-schema en `scripts/`: de branch blijft een voorstel tot de eigenaar merget. Landt ná kaart 3
(`drools-refusal-codes`, op main) en vóór kaart 6 (catalogi: de Nederlandse `drools`-fragmenten zouden na de omschakeling
niet meer passen, ontwerp §6). Serieel met kaart 7 (`channel-fact-codes`, §9.5-codes): beide veranderen het case-schema,
gate 8 en de tabel van RUNTIME-SEMANTICS ch. 5.7b; de bouwer herbaseert na kaart 7.*

Goal: een rode case pint vandaag drie stukken meldingstekst (`expectedFault.ref`, `.clingo`, `.drools`) naast de
`EF`-code, en gate 8 houdt twee dingen in stand om die tekst te dragen: de bodemzinlijst met haar fence (36 zinnen) en
`ref --bottom-sentence`, dat de fragmenten leest omdat een JSON-string elk teken als escape kan schrijven. Een code
zonder parameters kan twee cases van één map niet meer onderscheiden (`aanvoergeld-parameter-ongebonden`: de parameter;
`jutterswet-dubbele-melding`: het label), en 19 van de 50 pins citeren een gedeclareerde naam. Deze kaart laat de drie
executors de parameters van een fout uitzenden en laat de case ze pinnen (ontwerp §3.3, verplicht vóór een fragment
verdwijnt); codeert `ref` en clingo op aanvraag (§2.3, §7 rij 5); en haalt dan de fragmenten, de bodemzinlijst en de
decodering door `ref --bottom-sentence` weg. Na deze kaart leest geen gate meer een stuk meldingstekst van `ref` of
clingo, en elke rode pin is een code met parameters.

Modellen (ontwerp §7): Haiku kent de sleutels toe, Sonnet bouwt. De voorbereiding (het script dat de plekken vond, de
partij voor Haiku, de opdracht en het bouwplan) staat in de werkmap van de hoofdsessie, buiten de repository.

## Wat er staat (gemeten op `745ed4336`, 2026-10-19)

- 725 plekken in `ref/lib` (inclusief de caselezer) en `backends/clingo/lib` waar een fout wordt opgeworpen
  (`fault`, `faultc`, `fail`, `failc`, `failwith`, `invalid_arg`). Daarvan dragen er 39 al een `EF`-code (kaart 1: 26 in
  `ref`, 13 in clingo; daarnaast kent `Translate.viol_code` 8 redenen van `viol/2`-wachters), 36 zijn een bodemzin van gate 8 (12 in
  `ref/lib/eval.ml`, 24 in `backends/clingo/lib/translate.ml`), 3 staan voor de 7 regels van de divergentielijst van
  gate 8 (de Clamp-grens, de som over opgesomde operanden, het geheelgetalbereik van `scaled_of_value`), en 647 zijn
  geen van die drie.
- **39 plekken krijgen een nieuwe code** (12 `REF`, 27 `CLINGO`): het ontwerp raamde "~100", omdat het de `contract`-klasse
  en de plekken buiten pin en lijst nog niet had afgebakend. Alle 647 overige blijven zonder code: geen pin en geen
  lijst noemt ze, en een code die niemand leest dient niets (§2.3).
- 50 rode cases dragen `expectedFault {code, ref, clingo, drools?}`: 50 `ref`-pins (34 verschillende fragmenten), 50
  `clingo`-pins (35), 26 `drools`-pins (20). De bodemzinlijst heeft 36 zinnen, de divergentielijst 7 clingo-regels.
- De tabel van RUNTIME-SEMANTICS ch. 5.7b noemt de parameters per code, maar geen executor zendt ze uit en geen gate
  toetst ze ("the one column of this table without a machine check").

## Wat blijft staan

- Geen tekst van een melding verandert. De rapporten van de 50 cases en de statusregels van Drools zijn op de kop na
  (code en parameters) gelijk aan die van `745ed4336`; de bouwer legt de vergelijking vast in het Result.
- De `EF`-codes, de tabel in ch. 5.7b (op de parameterkolom en de preambule na), `expectedFault.code`, de `CODE`- en
  `CODE-PINNED`-regels van de runners (zij krijgen de parameters erbij) en de decodering van de gepinde code door de
  eigen caselezer van de runner (het precedent van `--bottom-sentence`: een JSON-string kan elk teken als escape
  schrijven, dus nooit een shell-scrape).
- De `DROOLS`-codes van kaart 3 en hun gates (`scripts/check-drools-codes.sh`, `EXPECTED_CHANNEL`, de Drools-lijst
  van gate 8), de `NOFAULT`- en `NOCASE`-regels, en de vloeren van andere gates.
- Een `EF`-code blijft een foutsoort van de semantiek; een `REF`- of `CLINGO`-code is gedrag van één executor en staat
  nooit in de case (ontwerp §3.5).

## Acceptance

- (a) **Parameters uitgezonden.** `ref`, clingo (ook voor de `viol/2`-wachters) en Drools zenden de parameters van de
  fout uit op dezelfde machineregel als de code (`CODE EF-005 {"rule": "…"}`, ontwerp §3.3: een JSON-object van
  strings, waarden als lexeem, gedeclareerde namen onvertaald), naast de tekst en nooit erin. De drager is de
  uitzondering: `Value.Fault` en `Case.Error` krijgen een veld `params`; `faultc` en `failc` nemen ze mee; Drools
  geeft ze via `code()` en `RedResult` door. Een code zonder parameters in de tabel zendt `{}` uit.
- (b) **Parameters gepind.** `expectedFault.params` (een object van strings) in het case-schema, op alle 50 rode cases
  (leeg waar de tabel geen parameters kent), door de eigen caselezer van de runner gedecodeerd en als `CODE-PINNED`-regel
  met de gepinde parameters gedrukt. Gate 8 eist van elke executor die de case controleert dat de uitgezonden parameters
  van de gepinde code de gepinde bevatten (deelverzameling), naast de code zelf. Een parameter die niet elke
  controlerende executor uitzendt, pint de case niet; wat een executor niet uitzendt staat dan in de parameterkolom
  van de tabel als `—` voor die code of in de divergentielijst, nooit als stille uitzondering.
- (c) **Onderscheid en kolom.** De twee paren cases die vandaag alleen in een parameter van hun fragment verschillen
  (`aanvoergeld-parameter-ongebonden`: de parameter; `jutterswet-dubbele-melding`: het label) staan in een lijst van
  het script, met hun reden; gate 8 faalt als de twee cases van een paar dezelfde `params` pinnen, en als een
  vermeld paar niet meer bestaat (verouderd in beide richtingen). Een code alleen onderscheidt ze niet. De
  parameterkolom van ch. 5.7b wordt machinaal gelezen: elke gepinde en elke uitgezonden sleutel staat in de kolom van
  zijn code; een uitgezonden sleutel buiten de kolom faalt met de code, de executor en de sleutel. De preambule zegt
  niet meer dat de kolom ongetoetst is.
- (d) **Codes op aanvraag voor `ref` en clingo.** De 36 bodemzinnen (12 `REF`, 24 `CLINGO`) krijgen een code van de
  soort `fallback`, de 3 plekken van de divergentielijst een `CLINGO`-code van de soort `refusal` (de Clamp-grens, de
  som over opgesomde operanden) of `limit` (het geheelgetalbereik). Geen `contract`-code en geen code voor de 647
  overige plekken. Per executor één register, een tabelbestand (`code`, soort, sleutel, parameters, functie die hem
  opwerpt), in de component zelf (ontwerp §2.3). Gemeten bij de voorbereiding: 12 + 27 codes. Vloeren
  `EXPECTED_MIN_REF_CODES` en `EXPECTED_MIN_CLINGO_CODES`, gemeten bij de bouw, met reden en datum.
- (e) **Carriercontrole in twee richtingen**, in een script dat gate 8 aanroept (`scripts/check-exec-codes.sh`, naar het
  voorbeeld van `check-drools-codes.sh`): elke geregistreerde code wordt in de bron van zijn component opgeworpen, elke
  opgeworpen `REF`-, `CLINGO`- of `EF`-code staat in zijn register respectievelijk in de tabel van ch. 5.7b, nummers
  lopen op zonder gat of hergebruik, sleutels zijn per register uniek, elke soort is `refusal`, `limit` of `fallback`
  en elke `ef`-verwijzing is een rij van de tabel. Het script leest de commentaarloze, aaneengevoegde bron (de
  normalisatie van de bodemzinfence, die dan vervalt) zodat een in commentaar geciteerde code niet telt.
- (f) **Divergentielijst met codekolom.** `<case>|<executor>|<CODE>|<CATEGORY> <reden>`: de lijst van gate 8 noemt voor
  elk van de 7 regels de `CLINGO`-code die clingo meldt, en de gate vergelijkt die met de `CODE`-regel van de runner;
  de categorie (`REFUSAL`, `LIMIT`) moet gelijk zijn aan de soort van de code in het register. Verouderd in beide
  richtingen: een regel waarvan de executor de code van de case of een andere code meldt faalt, en een controle van
  een uitvoering zonder code of met een andere code dan de regel noemt faalt.
- (g) **De fence op codes.** Geen pin en geen lijstregel noemt een `fallback`-code als wat een getuige bewijst:
  `expectedFault.code` is altijd een `EF`-code (patroon), de divergentielijst en de lijsten van de Drools-poot en van
  gate 12 mogen geen `fallback`-code noemen (hun soort volgt uit het register, respectievelijk `DroolsCode`), en
  een fault die in een rode case alleen een `fallback`-code meldt faalt als niet-gepind. De bodemzinlijst, de
  carrierfence die haar bewaakte, de `\u`-zelftest en `ref --bottom-sentence` zijn weg, met de redenen van de oude
  fence in het commentaar van de nieuwe (de remedie blijft: een benoemde tak voor de vorm, nooit een andere formulering
  van de bodem).
- (h) **De fragmenten weg.** `expectedFault.ref`, `.clingo` en `.drools` ontbreken in alle 50 cases en in het schema
  (`expectedFault` krijgt `required: ["code", "params"]` en houdt `additionalProperties: false`, zodat een fragment
  afgewezen wordt); `Case.expected_fault` en `--bottom-sentence` verliezen de fragmentvelden; de runners vergelijken
  geen tekst meer; de gate controleert niet meer op een stuk meldingstekst van `ref` of clingo. Gate 4 krijgt
  zelftestrijen voor het nieuwe patroon van `params` en voor een afgewezen fragmentsleutel. Een grep naar
  `"ref":` en `"clingo":` onder `expectedFault` in `corpus/rood/` levert niets.
- (i) **Drools.** De statusregel draagt de uitgezonden parameters bij de code (`FAULT EF-005 {…}: …`, of een eigen regel
  die de gate leest, een keuze van de bouwer die de bestaande vergelijking van de twee takken in gate 12 niet
  breekt); de 26 cases die een `drools`-fragment pinden pinnen nu code en parameters op Drools mee; de 24 zonder
  (EXCLUDE en NOFAULT) blijven in hun lijst met hun `DROOLS`-code. De Drools-poot van gate 8 verliest haar
  fragmentcontrole en de regel "een `drools`-fragment citeert de eigen zin van de backend".
- (j) **Mutaties**, elk één keer, eerst rood en dan teruggezet en groen, in het Result vastgelegd: een parameterwaarde
  veranderd in één executor (gate 8 rood met case, executor en beide waarden); een `REF`-code uit de bron gehaald (de
  carriercontrole rood met de code); een divergentieregel met een andere `CLINGO`-code (rood met beide codes); een
  `fallback`-code in een divergentieregel (de fence rood).
- (k) **Vloeren**, gemeten bij de bouw met reden en datum: `EXPECTED_MIN_RED_PARAMS` (de gepinde parameterparen die
  elke controlerende executor bevestigde, plus de controles van lege `params`), `EXPECTED_MIN_REF_CODES`,
  `EXPECTED_MIN_CLINGO_CODES`. De bestaande vloeren van gate 8 (`EXPECTED_MIN_RED_CASES=26`, `EXPECTED_MIN_RED_CODES=93`,
  `EXPECTED_MIN_EF_CODES=24`, `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19`) blijven staan of worden herberekend en gemotiveerd
  als de bouw er een raakt.
- (l) **Tests.** De suites van `ref/test/` en `backends/clingo/test/`, en de Drools-suite met nul overgeslagen, blijven
  groen. Een fragment dat verdwijnt is ook een invoer van testbestanden; de aanpassing is mechanisch (de verwachting
  verandert niet: wat een fragment toetste, toetst nu code en parameters) en vraagt de toestemming van de eigenaar, die de
  hoofdsessie vóór de bouw bevestigt: `ref/test/test_ref.ml` (drie verwachtingen van de caselezer rond regels 8089 tot
  8096, "missing field clingo" en "expectedFault.ref: expected a string"), `backends/clingo/test/test_clingo.ml` (zes
  ingebedde `expectedFault`-objecten met `ref` en `clingo`, en twee constructies `Value.Fault { code = None; text = … }`
  voor het nieuwe veld `params`), en drie Drools-tests die een pin uit het corpus lezen
  (`DateStepRangeBranchesWitnessTest` regel 77, `TmsWritersWitnessTest` regel 354, `TmsCreationWitnessTest` regel 742)
  plus twee ingebedde caseteksten (`FoldPlanWitnessTest` regel 632, `OverlappingWritersWitnessTest` regel 87).
- (m) Alle 16 gates van `CLAUDE.md`, in één volledige run op de eindstand van de branch.

## May touch

`ref/lib/` (de dragers `value.ml` en `case.ml`, de opwerpplekken die een `EF`-, `REF`- of `CLINGO`-code krijgen of
parameters uitzenden), `ref/bin/runner.ml`, het registerbestand van `ref/`; `backends/clingo/lib/` en
`backends/clingo/bin/` (dezelfde plekken, `Translate.viol_code`, de runner), het registerbestand van clingo;
`backends/drools/src/main` voor de uitzending van parameters (`CaseRunner`, `DroolsRedMain`, `ComputeFault`,
`WritersConflict`, `DroolsBackend.Firing`) en de replay-runner die de statusregel drukt; `corpus/rood/` (de 50
`expectedFault`-objecten, verder niets); `spec/schema/uchar-case-v0.schema.json`; `spec/RUNTIME-SEMANTICS.md` ch. 5.7b
(parameterkolom en preambule); de verwijzingen naar de fragmenten in `spec/IR-SPEC.md` (de WF-044-rij: "`expectedFault.{ref,
clingo}`"), `spec/ASP-TRANSLATION.md`, `corpus/README.md`, `ref/README.md` en `backends/clingo/README.md`; `scripts/check-rood.sh`, `scripts/check-case-schema.sh` (zelftestrijen), het nieuwe
`scripts/check-exec-codes.sh`; `spec/proposals/MESSAGE-CODES.md` (de stand van §2.3, §3.3, §5, §7 rij 5); deze kaart.
Testbestanden alleen mechanisch en na bevestiging van de toestemming door de hoofdsessie (zie (l)).

## Out of scope

- De catalogi en elke vertaling van een melding (kaart 6); het Drools-register en de frontendcodes (kaarten 3 en 4, op main).
- De §9.5-codes, `inconsistent.code` en het verhuizen van EF-005 naar §9.5 (kaart 7).
- Codes voor de overige 647 plekken, `contract`-codes en elke wijziging van een meldingstekst.
- De armsleutels van de verifier (kaart 2, op main) en de `CLAUDE.md`-alinea van gate 8 (de hoofdsessie, beschermd pad;
  de bouwer legt een voorstel in het Result).

## Gate set

Alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch, in één volledige run (geen dubbele run: de jar één
keer, `scripts/check-drools.sh --fast` om pins af te stellen).

## Result

Gebouwd op main `108a2d71b` (kaart 7 geland); herbaseerd zonder conflict. Vier commits op `card/fragment-cleanup`: ref en
clingo (`a55cf6411`), de Drools-backend (`ad833c9d8`), corpus, schema en gate-scripts (`d65d0e223`) en de documenten van
`spec/` (daarna); met deze Result en de alinea van `MESSAGE-CODES.md` (§7) een vijfde. Niet gepusht.

**Aantallen.** 725 opwerpplekken (onveranderd na kaart 7: dezelfde 725, dezelfde sjablonen). 39 plekken kregen een nieuwe
code (12 `REF`, 27 `CLINGO`; Haiku-antwoorden zonder fout of botsing, 39 sleutels, de steekproef van de drie
divergentieplekken, `bound`, `rules` en `value` klopte); 48 plekken zenden parameters uit, 72 door `apply_codes.py`
gewijzigd, de rest met de hand (zes in `case.ml`/`eval.ml`/`translate.ml`, plus `Translate.viol_code`, dat de parameters van
de acht `viol/2`-redenen uit de reden en het onderwerp leest). Kaart 7 bracht geen opwerpplekken en geen EF-codes met
parameters bij die de partij vergrootten; de rijen EF-025 tot en met EF-031 hebben de parameters `rule` en `label` en worden
door geen executor als fout uitgezonden (het zijn kanaalfeiten). Floors: `EXPECTED_MIN_REF_CODES=12`,
`EXPECTED_MIN_CLINGO_CODES=27` (in `check-exec-codes.sh`), `EXPECTED_MIN_RED_PARAMS=131` (ref 69 + clingo 62),
`EXPECTED_MIN_DROOLS_PARAMS=33` (gemeten op de Drools-poot, apart omdat die poot overgeslagen kan worden);
`EXPECTED_MIN_RED_CODES=93`, `EXPECTED_MIN_EF_CODES=31` en `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19` blijven gelijk (gemeten
93, 31, 19). Gate 8 telt nu de Drools-codechecks niet meer in `RED_CODES` (ze telden daar niet in).

**Wat de cases pinnen** (de doorsnede van wat elke controlerende executor uitzendt; 69 paren over 50 cases): EF-001, 002,
003, 013, 014, 018, 020, 021, 023 hun volledige parameters; EF-004 `value` en `decimals` (clingo noemt `place` niet);
EF-009 `reach`; EF-008 `count`; EF-016 `rules`, `attribute`, `label`, `objectType` (clingo noemt `values` niet); EF-017
`role`, `label` (clingo noemt `filler` niet); EF-003 van `inputdatum-kalender` alleen `lexeme` (Drools noemt het veld
zonder de instantie); lege `params` bij EF-005, 006, 007, 010, 011, 012, 015, 019, 022, 024 (geen gemeenschappelijke
parameter). EF-005 `rule` komt alleen van Drools; `dividend`, `contextValue`, `position` en `key` van geen executor. De
twee paren verschillen: `parameter` (aanvoertarief, aanvoerdrempel) en `label` (V, V2).

**Geen tekst verandert.** `compare_reports.py` (baseline na de rebase, eigen meting vóór de eerste wijziging):
0 verschillen in 50 cases voor ref en clingo (volledige rapporten) en 0 verschillen in 50 cases voor de statusregel van
Drools, op de code en de parameters aan de kop na. Zes clingo-rapporten verschilden van de baseline van vóór kaart 7 (de
regelpositie in het tijdelijke programma, `#defined inconsistent_code/3` van kaart 7); daarom is de baseline opnieuw
gemeten op de rebase-stand.

**Mutaties** (elk één keer, rood, teruggezet, groen):
1. `ref`'s `label` van EF-016 met `x` achter de waarde: gate 8 rood met `FAIL: jutterswet-dubbele-melding/case1-…: ref emits
   {…"label": "Vx"…} for EF-016 with another value, but the case pins label "V"`, en hetzelfde voor `case2` (`V2x`/`V2`);
   daarbij de vloeren (`91 < 93` codechecks, `123 < 131` parameters).
2. `REF-002` uit `eval.ml` (de aanroep wordt `Value.fault`): `check-exec-codes: FAIL — REF-002 is registered in
   ref/codes.tsv but no place of ref/lib raises it`.
3. Divergentieregel van `begrensd-reads-bounds-in-order` met `CLINGO-007` waar clingo `CLINGO-005` meldt: `clingo is
   listed as reporting CLINGO-007, but reports CLINGO-005`, plus de verouderd-melding van de regel.
4. Dezelfde regel met `CLINGO-004` (een `fallback`-code): `the divergence entry names CLINGO-004, a fallback code … never what
   a witness rests on`.
Voor de nieuwe toetsen zelf, elk eenmaal: (5) `label` van `case2` van `jutterswet-dubbele-melding` gelijk aan dat van
`case1`: `… pin the same params {…}: the pair differs in a parameter only`; (6) een `EXCLUDE DROOLS-044` (een
`fallback`-code van het Drools-register) in de lijst van de Drools-poot: `names DROOLS-044, a fallback code of the Drools
register`; (7) `REF-001` met de extra parameter `extra` in het register: `ref/lib/eval.ml:754 passes the parameters
[lexeme] on REF-001, but its line names [lexeme,extra]`; (8) `ref` zendt de sleutel `objecttype` uit op EF-016: `ref emits
the parameter objecttype on EF-016, which is no key of the parameter column of that code`; (9) `required: ["code"]` in
het schema: gate 4 rood op de zelftestrij `{'code': 'EF-005'}: schema accepts it, expected refuse`.

**Gates.** Eén volledige run van alle 16 op de eindstand (`bd32e3f45` met daarna alleen deze tekst), 0 mislukt, in
18 minuten; gate 12 604 s (jar één keer, 1619 JUnit-tests, 255 gespeeld op beide takken), gate 6 189 s (2198 checks), gate 5
(1286 voorbeelden), gate 8 (131 en 33 parameterchecks, 93 codechecks). Gate 1 groen op de commit na de Result.

**Testbestanden aangepast** (besluit van de hoofdsessie onder het mandaat voor stap 7; alleen vervanging, geen verzwakking):
- `ref/test/test_ref.ml:8091` en `:8094` (twee verwachtingen van de caselezer): `missing field "clingo"` is nu `missing field
  "params"` voor `{ "code": "EF-005" }`, en `expectedFault.ref: expected a string, got 1` is nu
  `expectedFault.params.rule: expected a string, got 1`; dezelfde twee gedragingen (een verplicht veld, het type van een waarde).
- `backends/clingo/test/test_clingo.ml`: 7 ingebedde `expectedFault`-objecten (regels 5042, 5113, 5285, 5300, 5452, 5610, 5836)
  van `{ "ref": …, "clingo": … }` naar `{ "code": "EF-005", "params": {} }`; de naald van `both_break` (ref-tekst) blijft; de
  twee constructies `Value.Fault { code = None; text }` (regels 40 en 3487) kregen `params = []`.
- Drools: `DateStepRangeBranchesWitnessTest.fails` (:77 tot :88; de zin van de `drools`-pin is de code `EF-007` onder de
  soorten van de fout in de oorzaakketen), `TmsWritersWitnessTest` (:354 tot :362; de pin is `EF-016` met de gepinde
  parameters als deelverzameling van die van `WritersConflict`, de zin met "twee schrijvers van één doel" blijft getoetst),
  `TmsCreationWitnessTest` (:735 tot :743; de twee fragmentverwachtingen op ref en clingo zijn de code `EF-015` en de parameters
  `creation`, `stratum` en `depth` die de Drools-run zelf uitzendt, wat de bewering "ref noemt dezelfde creatie, stratum en diepte" sterker
  dekt), `FoldPlanWitnessTest:632` en `OverlappingWritersWitnessTest:88` (ingebedde casetekst).

**Wat open blijft.** (1) De `inconsistent`-verwachtingen en kanaalfeiten zijn ongewijzigd (kaart 7). (2) `dividend`,
`contextValue`, `position` en `key` zendt geen executor uit; `rule` van EF-005 alleen Drools: de kolom zegt het
(ch. 5.7b, "Parameters"), geen case pint ze. (3) De `DROOLS`-codes zelf dragen nog geen parameters op hun rapport (kaart 6,
catalogi). (4) `scripts/check-drools.sh` is niet aangeraakt (niet in "May touch"): de fence op `fallback`-codes in
`EXPECTED_CHANNEL` zit in gate 8 (de Drools-poot leest `EXPECTED_CHANNEL` en het register). (5) De regel in IR-SPEC bij
WF-044 zei dat de Drools-poot `Unresolved` lijst; dat was al achterhaald (beide `wf044`-cases FAULTen op Drools) en is nu "de
Drools-poot houdt haar aan dezelfde pin".

**Voorstel voor de alinea van gate 8 in `CLAUDE.md`** (beschermd; de hoofdsessie zet haar):

> 8. `scripts/check-rood.sh` — the red corpus: every `corpus/rood/**/case*.json` declares `expectedFault {code, params}`
>    (the EF code of the fault kind, RUNTIME-SEMANTICS ch. 5.7b, and its parameters as an object of strings; the schema
>    refuses every other key, so no piece of message text is pinned) and MUST fail observably — at load or at evaluation —
>    in **both** executors as that kind: `ref` and clingo print one `CODE <code> <json>` line per observed code and the
>    case's own `CODE-PINNED <code> <json>`, decoded by the runner's case reader (never a shell scrape: a JSON string may
>    spell any character as an escape), and the gate demands the pinned code with the pinned parameters among the emitted
>    ones (a subset; the Drools leg reads the same from its status line `FAULT EF-005 {…}: …`, for every case not in its
>    exclusion list), the unsolved-state guard among them (`Unresolved` reached in evaluation faults with EF-019,
>    WF-044, never vanishes). An executor that fails a case for a reason of its own stands in the divergence list
>    `<case>|<executor>|<CODE>|<CATEGORY> <reason>`, with the `REF-###` or `CLINGO-###` code it reports and a category
>    equal to the kind of that code in its register (`REFUSAL` ↔ `refusal`, `LIMIT` ↔ `limit`); the list is stale-guarded in
>    both directions. The registers of the executors (`ref/codes.tsv`, `backends/clingo/codes.tsv`) are held to their
>    sources in both directions, parameters included, by `scripts/check-exec-codes.sh`, which gate 8 calls (floors
>    `EXPECTED_MIN_REF_CODES=12`, `EXPECTED_MIN_CLINGO_CODES=27`). The fence on codes replaces the bottom-sentence list: a
>    `fallback` code — the bottom arm of a dispatch — is named by no pin (`expectedFault.code` is an `EF` code), no
>    divergence entry, no exclusion entry of the Drools leg and no `EXPECTED_CHANNEL` entry of gate 12, and a fault that
>    reports only a `fallback` code is a code other than the pinned one. Every parameter an executor emits is a key of the
>    parameter column of ch. 5.7b, and the two pairs of cases that differ in a parameter only (`aanvoergeld-parameter-ongebonden`,
>    `jutterswet-dubbele-melding`) are told apart by their pinned `params` (stale-guarded). An empty red set is a failure,
>    never silently green. Floors: `EXPECTED_MIN_RED_CASES=26`, `EXPECTED_MIN_RED_CODES=93`, `EXPECTED_MIN_RED_PARAMS=131`,
>    `EXPECTED_MIN_EF_CODES=31`, `EXPECTED_MIN_GOLDEN_CODES=73`, `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19`,
>    `EXPECTED_MIN_DROOLS_PARAMS=33` (the last two on the Drools leg).
> Gate 4 carries self-test rows for the shape of `expectedFault` (a `params` object of strings, no fragment key).

