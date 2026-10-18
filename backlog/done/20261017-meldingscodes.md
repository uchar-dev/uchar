# meldingscodes: een melding krijgt een code, los van haar tekst

*Op verzoek van de eigenaar (2026-10-17), bij de vraag naar meertalige foutmeldingen. Raakt `spec/`, het
case-schema en `scripts/`: de branch blijft een voorstel tot de eigenaar merget. Niet parallel met de stappen 3 en 5
van het programma "Engelse namen" (dezelfde bestanden); vóór de catalogi van stap 7.*

Goal: een melding is nu alleen tekst. De frontend heeft `Diagnostic(severity, message, line)`, de Drools-backend vrije
strings, en de gates pinnen fragmenten van die tekst: `expectedFault {ref, clingo, drools}` in 50 rode cases, een
armfragment in 354 `expect.txt`-bestanden, de redenen in de lijsten van gate 12 en de 36 bodemzinnen van gate 8.
Een tweede taal vraagt dan een tweede set pins, of gates die maar één taal toetsen. En omdat elke executor zijn eigen
fragment heeft, is "ref, clingo en Drools falen op dezelfde fout" handwerk. Deze kaart legt het ontwerp voor codes
vast en voert de normatieve plak uit: de foutsoorten bij evaluatie.

Modellen: de bouwer is Opus. Het ontwerp in `spec/proposals/MESSAGE-CODES.md` krijgt vóór de bouw van de plak één
review van Fable, op architectuur; de hoofdsessie vraagt die aan.

1. Ontwerp, in `spec/proposals/MESSAGE-CODES.md`, per soort melding de drager van de code en wie hem leest:
   - fouten bij evaluatie (het §9.5-kanaal en de andere foutkanalen van RUNTIME-SEMANTICS): normatief, een code per
     foutsoort met haar parameters;
   - afwijzingen door de verifier: WF-### blijft de code, en de arm binnen een regel krijgt een vaste sleutel van de
     verifier die `expect.txt` pint in plaats van een fragment;
   - weigeringen (een executor draagt een vorm niet: de redenen van gate 12, de bodemzinnen van ref en clingo):
     niet-normatief, een code per executor; de bodemzinlijst van gate 8 wordt een lijst van terugvalcodes die geen
     pin mag noemen;
   - diagnostiek van de frontend op een `.rgs`-bron: niet-normatief; `Diagnostic` krijgt `code` en `params`;
   - het rapportformaat: `CODE: tekst`, de code eerst, zodat een gate de code leest en een mens de tekst;
   - catalogi: Engels als referentie, Nederlands erbij voor de frontend en de Drools-backend, de onderdelen die de
     distributie levert; ref, verifier en clingo houden Engelse tekst bij hun codes; de distributie kiest de taal
     (een vlag in de CLI, `Accept-Language` in de dienst). Gedeclareerde namen gaan als parameter onvertaald in de
     tekst;
   - de volgorde van de vervolgkaarten.
2. De normatieve plak:
   - RUNTIME-SEMANTICS: een tabel van de foutsoorten met code, kanaal, parameters en per code een getuige in
     `corpus/rood`;
   - het case-schema: `expectedFault.code`, één code voor alle executors, additief;
   - ref, clingo en Drools zetten de code vooraan in hun rapport bij elke fout die een rode case pint;
   - gate 8 toetst de code in elk rapport. De fragmenten blijven staan tot elke rode case een code draagt; daarna
     verdwijnen ze, met de decodering door `ref --bodemzin`, in een vervolgkaart.

Acceptance:
- (a) `spec/proposals/MESSAGE-CODES.md` met de punten van 1, en een gemotiveerd voorstel voor de vorm van de codes
  (prefix, nummering, parameters) naast WF-###.
- (b) De tabel in RUNTIME-SEMANTICS; elke code heeft een getuige in `corpus/rood` (een zin zonder machinaal
  toetsbaar artefact is niet af).
- (c) `spec/schema/uchar-case-v0.schema.json` met `expectedFault.code`; gate 4 groen.
- (d) Alle 50 rode cases dragen `code`. Gate 8 faalt als een executor een andere code meldt dan de case noemt, eerst
  rood tegen een mutatie en dan groen; vloer `EXPECTED_MIN_RED_CODES`, gemeten.
- (e) Geen tekst van een melding verandert: elke bestaande fragmentpin blijft groen.
- (f) Alle gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `spec/proposals/MESSAGE-CODES.md` (nieuw), `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/uchar-case-v0.schema.json`, `corpus/rood/`, het foutrapport van `ref/`, `backends/clingo/` en
`backends/drools/`, `scripts/check-rood.sh`, `scripts/check-case-schema.sh` als het schema dat vraagt, deze kaart.

Out of scope:
- De catalogi en elke vertaling: vervolgkaart.
- De armsleutels van de verifier, de codes van gate 12 en die van de frontend: vervolgkaarten volgens het ontwerp.
- De distributie: haar taalkeuze, en haar eigen JSON-sleutels `fout`, `schendingen`, `veld` en `soort`.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Ontwerp (stand)

- `spec/proposals/MESSAGE-CODES.md` (9894e6d2): `EF-###` per foutsoort in een tabel van RUNTIME-SEMANTICS met kanaal (case input, fault, declined, §9.5), `WF-###.<slug>` als armsleutel, `REF-`/`CLINGO-`/`DROOLS-###` voor weigeringen (soort `refusal`, `limit`, `contract`, `fallback`), `RGS-###` voor de frontend; parameters benoemd, waarden als lexeem.
- Rapport `CODE: tekst`; de code reist als veld naast de tekst en staat vooraan het rapport, nooit in de zin, zodat geen fragmentpin breekt (twee gemeten gevallen: de regelprefix van Drools, de passagezin van ref).
- Gemeten: 25 foutsoorten achter de 50 rode cases (4 case input, 17 fault, 4 declined), 6 cases waar clingo om een eigen reden faalt (divergentielijst in gate 8), 354 `expect.txt`, 48 regels van gate 12 (alleen het kanaal vergeleken), 36 bodemzinnen, 248 `Diagnostic`-plekken.
- Volgorde: plak (Opus), dan armsleutels, gate-12-codes en frontendcodes parallel (Sonnet + Haiku), dan het opruimen van de fragmenten, dan de catalogi; een zevende kaart voor §9.5-codes, omdat een §9.5-feit geen rode getuige kan hebben (acceptance (b)).
- Voor Fable (§8): één `EF`-naamruimte met kanaalkolom; divergentielijst in het script tegenover velden per executor in de case; §9.5 buiten de plak; code als veld en `expectedFault.params` vóór het opruimen; declined vormen als `EF`; armsleutels als slug.

## Open punt voor de eigenaar: de standaardtaal van de Drools-backend (kaart 6)

Niet beslist; vóór kaart 6 nodig. Afweging van Fable (§2 (e)): de distributie levert de Drools-backend, en haar
gebruikers zien nu Nederlandse runtimezinnen. Na kaart 6 is de standaardtekst Engels, tenzij de distributie een
locale doorgeeft. Te beslissen: is de standaardlocale een instelling die de distributie zet (dan moet haar pin-bump
`nl` zetten), en dat besluit hoort in kaart 6; anders landt de omschakeling bij de pin-bump als een stille
gedragswijziging. Ook vastgelegd in `spec/proposals/MESSAGE-CODES.md` §6.

## Result (normatieve plak, 2026-10-19)

Commits op `card/meldingscodes`:
- `ebd42705` — ontwerp bijgewerkt op de review van Fable, vóór de bouw: per vraag besluit en reden in één zin
  (§8); EF-019/020 samengevoegd tot één code met parameter `position` (24 codes); EF-005 als besloten verhuizing
  naar §9.5; divergentielijst met categorie en reden, zonder codes en zonder `CLINGO`-register; drager in de
  OCaml-exceptie; `Diagnostic` zonder `code` op de draad, kaarten 3 en 4 na elkaar; code on demand in kaart 5
  (~100 plekken); `params` verplicht vóór het opruimen; standaardtaal Drools als open vraag (§6).
- `c6635535` — de code naast de tekst in ref, clingo en Drools, met de testpatronen.
- `254a825c` — tabel in RUNTIME-SEMANTICS ch. 5.7b, `expectedFault.code` in het schema, 50 rode cases, gate 4
  en gate 8.
- de commit met dit Result.

Tabel (acceptance (b)): 24 codes, EF-001..EF-024, in de nieuwe ch. 5.7b: 4 `case input` (EF-001..004), 16
`fault` (EF-005..020), 4 `declined` (EF-021..024); de kanaalcel van EF-005 noemt de besloten verhuizing naar §9.5
(ch. 1.4 punt 4, die nu EF-005 noemt). Drie thuiszinnen voor EF-001..003 onder ch. 5.7 stap 3. Elke code heeft
minstens één rode getuige; de getuigenkolom laat ook `corpus/golden/<dir>` toe (de controle zoekt daar een
`inconsistent`-verwachting met die code; kaart 7). De parameterkolom staat er en wordt pas vanaf kaart 5
getoetst; de preambule zegt dat. Alle 50 rode cases dragen `code` (24 verschillende).

Executors (acceptance (e): geen meldingstekst veranderd, elke fragmentpin groen):
- ref: `Value.Fault` en `Case.Error` dragen `{ code : string option; text : string }`; `faultc`/`failc` op 26
  plekken; `Value.report` zet de code vooraan (`EVALUATION FAULT: EF-005: …`). Op het rode pad drukt de runner
  `CODE <code>` en `CODE-PINNED <code van de case>`, door de eigen case-lezer gedecodeerd.
- clingo: 12 `faultc`-plekken, EF-004 bij de schaal van case-invoer, en `Translate.viol_code` voor de redenen
  van 8 `viol/2`-wachters; de runner drukt dezelfde twee regels en zet de codes vooraan het UNSAT-rapport.
- Drools: classificatie bij het vangen in `CaseRunner.faultCode` (`Breuk.DelingDoorNul` → EF-005; `code()` op
  `ComputeFault`, `WritersConflict` en de nieuwe `CaseRunner.CaseDateFault`; per fout binnen
  `EvaluationFaults`); `RedResult` kreeg de component `code`; `DroolsBackend.Firing.code()` geeft EF-015 bij het
  creatiebudget; statusregel `FAULT EF-005: …`. Alle 50 statusregels zijn gelijk aan de nulmeting, op de codekop
  na en op één punt: de twee datumcases tonen `CaseDateFault:` waar `IllegalArgumentException:` stond (eigen
  type, zodat de soort benoembaar is; de meldingstekst is gelijk).
- Afwijking van het ontwerp: `Firing` kreeg geen vierde component, want `SessionDumpWitnessTest` construeert hem
  met drie; `code()` leidt het budget af uit de telling (geen fixpunt onder het plafond).

Testpatronen (mandaat 2026-10-18), mechanisch, geen verwachting veranderd: `ref/test/test_ref.ml` 33 plekken
(27 `Value.Fault m` → `Value.Fault { text = m; _ }`, 6 `Case.Error m`), `backends/clingo/test/test_clingo.ml` 75
plekken (70 `Value.Fault`, 3 `Case.Error`, 2 constructies `Value.Fault { code = None; text = … }`). Fable telde
37 + 81; gemeten zijn 33 + 75 plekken die een lading binden of bouwen.

Gate 8 (acceptance (d)): per case de code van ref en clingo, en van Drools waar een `drools`-fragment staat;
divergentielijst van **7** clingo-cases (2 `REFUSAL`, 5 `LIMIT`), stale in beide richtingen. Het ontwerp
noemde 6: het telde de vijf bereikgevallen als vier (gecorrigeerd in §1). De tabel wordt streng gelezen (rij
buiten ch. 5.7b of onleesbaar: FAIL; getuige bestaat en pint de code; elke gepinde code is een rij). Vloeren
`EXPECTED_MIN_RED_CODES=93` (ref 50 + clingo 43) en `EXPECTED_MIN_EF_CODES=24`, gemeten; de Drools-codetoetsen
(26) krijgen geen eigen vloer, met dezelfde reden als zijn foutentelling. Gate 4: het nieuwe patroon kreeg 9
zelftestrijen.

Mutatie: ref meldt EF-006 voor deling door nul (`ref/lib/arith.ml`) en Drools EF-017 voor twee schrijvers
(`WritersConflict.code()`). Gate 8 rood met 11 FAIL-regels: de 8 EF-005-cases op ref, de vloer (85 < 93) en de
2 cases van `jutterswet-dubbele-melding` op Drools. Teruggezet; daarna groen.

Gates op `254a825c`, één volledige run (2026-10-19 00:15–00:34): alle 16 groen. Gate 8: 50 cases, 93
codetoetsen, 7 divergenties, 24 foutsoorten, Drools 26 van 26 met de code; gate 4 873 cases, 101
zelftestrijen; gate 5 1286 voorbeelden; gate 6 2198 checks; gate 12 JUnit 1619.

Vervolgkaarten, volgorde volgens het bijgewerkte ontwerp §7: 1 → 2 ∥ (3 → 4) → 5 → 6; 7 na 1, serieel met 5.
- 2 armsleutels van de verifier (sleutel plus `name=value`-regels in `expect.txt`): Haiku benoemt, Sonnet
  bouwt; mag als laatste.
- 3 codes van gate 12 (`DROOLS`): Haiku kent toe, Sonnet bouwt.
- 4 codes van de frontend (`RGS`, `Diagnostic` niet op de draad, vier Drools-plekken): Haiku en Sonnet; na 3.
- 5 opruimen van de fragmenten (`params` verplicht, code on demand, `CLINGO`-kolom in de divergentielijst):
  Haiku codeert, Sonnet bouwt.
- 6 catalogi NL/EN: eerst het open punt hierboven; Haiku vertaalt, Sonnet bouwt.
- 7 §9.5-codes, met de verhuizing van EF-005: Opus.

Voor de hoofdsessie:
- Geen `CONSTRUCTS.md`-rij (Fable Q3).
- Voorstel voor de alinea van gate 8 in `CLAUDE.md` (beschermd), toe te voegen na de bodemzinnen: "Since
  2026-10-19 (card `meldingscodes`) every red case pins `expectedFault.code`, the EF code of its fault kind
  (RUNTIME-SEMANTICS ch. 5.7b), and ref, clingo, and Drools where a `drools` fragment is pinned, must report it
  — the runners print `CODE`/`CODE-PINNED`, Drools heads its status line with it — unless the executor stands in
  the script's reason-carrying divergence list (category REFUSAL or LIMIT, stale both ways); the gate parses the
  ch. 5.7b table strictly, holds every witness to its row's code and every pinned code to a row; floors
  `EXPECTED_MIN_RED_CODES=93` and `EXPECTED_MIN_EF_CODES=24` (measured 2026-10-18)."
