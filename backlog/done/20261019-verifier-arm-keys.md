# verifier-arm-keys: een afwijzing van de verifier krijgt een armsleutel, los van haar tekst

*Kaart 2 van §7 van `spec/proposals/MESSAGE-CODES.md`, vervolg op de normatieve plak (`meldingscodes`, op main
`719a2377`). Programma "Engelse namen", stap 7. Raakt `spec/verifier/` en `scripts/`: de branch blijft een voorstel
tot de eigenaar merget. Mandaat voor stap 7: lokaal werken, ff-merge door de hoofdsessie, niet pushen.*

Goal: een afwijzing van de verifier is nu alleen tekst, en `expect.txt` pint een fragment van die tekst: 354
bestanden, 327 verschillende fragmenten, 192 ervan met een aangehaalde naam. Een herformulering van een melding
breekt dan de pin, en een fragment pint de arm niet (§2.2: het pint ook de zin). Deze kaart geeft elke arm van een
regel een vaste sleutel `WF-###.<slug>` (ontwerp §2.2): de verifier meldt hem op elke schending, en `expect.txt`
pint de sleutel plus de benoemde parameters in plaats van een fragment. `WF-###` blijft de code van de regel.
De kaart mag als laatste van de reeks; haar opbrengst is dat een melding herschreven kan worden zonder een pin
te breken.

Het werk is bulk: Haiku benoemt de armen (een slug en de parameters per plek, partijen in
`werk/engelse-namen/stap7/k2/partijen/`), Sonnet bouwt, een script herschrijft de 354 `expect.txt`.

Modellen: de bouwer is Sonnet, het benoemen Haiku, per bouwstap een verse bouwer die het werk helemaal afmaakt.

Gemeten (2026-10-19, `main` `719a2377`, gereedschap in `werk/engelse-namen/stap7/k2/tools/`):
- 458 plekken in `spec/verifier/lib/checks.ml` die een schending maken (het ontwerp noemde er 441): 435 aanroepen
  `v "WF-###" …`, 17 aanroepen van een lokale `v` die één regel vastlegt (WF-116, -117, -118, -121), 2 directe
  records (WF-119, -120) en 4 plekken waar de regel een waarde is (`Closed_pos (wf, _)`: WF-067/069; `keyed_wf`:
  WF-034/100; de granulariteit: WF-056/058). 111 regels (WF-100 alleen via die plek); 453 verschillende aanroeptekst; 42 plekken bouwen hun zin in een helper.
- 511 negatiefmappen, 354 met `expect.txt` (alle één regel, geen meerregelig), 327 verschillende fragmenten, 192
  met een aangehaalde naam, 71 regels met een pin, 157 mappen zonder pin.
- Gate 3 op main: 348 golden schoon, 511 afgewezen, 354 op hun arm, 619 geciteerde pointers (vloeren 350, 260, 547).
- Meer plekken die het rapportformaat lezen: `scripts/check-verifier.sh`, `scripts/check-toka-parity.sh` (de regel
  `^\(WF-[0-9][0-9]*\) FAIL`, die de sleutel niet zou lezen), en de testsuites van catala en flogic (zoeken
  `WF-###` als deelstring; blijven werken).

1. Ontwerp van de vorm:
   - de schending (`Checks.violation`) krijgt `key` (`WF-###.<slug>`) en `params` (benoemde `name=value`-paren, de
     namen Engels, de waarden als in het document); de constructor `v` neemt de sleutel als verplicht argument,
     zodat een plek zonder sleutel niet compileert;
   - de rapportregel wordt `WF-###.<slug> FAIL <pointer>: <tekst>`; de tekst blijft ongewijzigd;
   - één sleutel per meldingssjabloon binnen een regel, nooit één sleutel voor twee sjablonen, en twee plekken met
     hetzelfde sjabloon delen de sleutel; de slug is Engels kebab-case, hoogstens drie woorden, uniek binnen zijn
     regel (ontwerp §2.2);
   - de parameters volgen de benamingen van ontwerp §3.3 (`attribute`, `objectType`, `label`, `value`, `lexeme`,
     `field`, `role`, `date`, `parameter`, `rule`) waar het woord past; een nieuwe naam is Engels en gebruikt het
     woord van de draad waar de draad er een heeft.
2. Benoemen: Haiku geeft elke plek een slug en de parameters (`werk/engelse-namen/stap7/k2/prompt-haiku.md`); een
   script leest de antwoorden en past `checks.ml` toe (`PLAN.md` in die map); de bouwer controleert op compileren
   en op sleutel-sjabloon-bijectie.
3. `expect.txt` van fragment naar sleutel: de eerste regel is de sleutel, de volgende regels zijn `name=value`; de
   verifier-melding moet de sleutel dragen en elke `name=value` moet in de parameters van die schending staan
   (deelverzameling). Een script herschrijft de 354 bestanden: het draait de verifier, neemt de sleutel en de
   parameters van de regel waarvan de tekst het oude fragment bevat, en faalt bij nul of meer dan één sleutel; elke
   aangehaalde naam van het oude fragment komt als `name=value` terug.
4. Gate 3: leest de sleutel (het patroon `^WF-[0-9]+ FAIL ` wordt `^WF-[0-9]+(\.[a-z0-9-]+)? FAIL `, ook in het
   pointerpatroon); toetst de sleutel en de `name=value`-regels; faalt op een `expect.txt` met de oude vorm (een
   fragment); faalt wanneer binnen een rapport één sleutel twee sjablonen heeft (het sjabloon is de tekst met de
   parameterwaarden vervangen door `{name}`); een lege of meerregelige pin faalt zoals nu, behalve dat meerdere
   regels nu `name=value` zijn.

Wat blijft staan:
- De tekst van elke melding (acceptance (e)), de pointers, de regelnummers `WF-###` en de naam van elke
  negatiefmap (`wf###-<slug>`: het script leest de regel daaruit, de sleutel komt erbij).
- De `LOAD ERROR`-regels van de loader (geen WF-regel; buiten het ontwerp §2.2).
- `--dependency-edges` en het golden-pad van gate 3 (schoon blijft schoon).
- De regel `WF-###` als code in IR-SPEC en de WF-index.

Acceptance (machinaal toetsbaar):
- (a) Elke schending draagt `key` en `params`: `Checks.v` kent geen aanroep zonder sleutel (compileert niet), en
  de 458 plekken zijn er allemaal aan toegekend (een script telt de plekken in `checks.ml` en vergelijkt met de
  toekenning; getal en vloer gemeten bij de bouw).
- (b) Binnen een regel verschillende sjablonen hebben verschillende sleutels en hetzelfde sjabloon heeft één
  sleutel: een statische controle over de plekken (script in `scripts/` of testgeval van de verifier, vloer
  `EXPECTED_MIN_ARMS` gemeten) en gate 3 over de corpusrapporten.
- (c) Alle 354 `expect.txt` dragen de nieuwe vorm: sleutel plus nul of meer `name=value`; geen bestand verloren
  (354 blijft 354, `EXPECTED_MIN_EXPECT` op de gemeten stand), en elke aangehaalde naam van het oude fragment staat
  als parameterregel in het nieuwe. Gate 3 groen vóór de herschrijving (fragment) en erna (sleutel).
- (d) Gate 3 rood tegen drie mutaties, elk gemeten en teruggezet: (1) een andere slug in `checks.ml` voor een
  gepinde arm; (2) een `name=value` met een verkeerde waarde in één `expect.txt`; (3) een sleutel voor twee
  sjablonen; en groen tegen een herformulering: de tekst van één gepinde arm gewijzigd, sleutel gelijk, gate 3
  blijft groen (de opbrengst van de kaart; daarna teruggezet, want geen meldingstekst verandert).
- (e) Geen meldingstekst verandert: het verschil van de volledige verifier-uitvoer over `corpus/negatief`,
  `corpus/golden` en `corpus/rood`, vóór en na, is alleen de sleutel op de kop van elke FAIL-regel
  (`WF-### FAIL` wordt `WF-###.<slug> FAIL`; een script vergelijkt de twee uitvoeren regel voor regel).
- (f) Vloeren van gate 3 re-gemeten en bijgewerkt waar iets kan krimpen: `EXPECTED_MIN_EXPECT`,
  `EXPECTED_MIN_POINTERS` (619 gemeten), nieuw `EXPECTED_MIN_ARMS` (het aantal verschillende sleutels dat de pins
  noemen) en `EXPECTED_MIN_PARAM_LINES` (het aantal `name=value`-regels), elk met reden en datum.
- (g) `scripts/check-toka-parity.sh` leest de regel `WF-###` uit een sleutelregel (de holdout-pin van de
  well-formedness-lus blijft `WF-### <pointer>`); gate 11 groen.
- (h) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `spec/verifier/lib/checks.ml` (de plekken en `violation`), `spec/verifier/bin/verifier.ml`,
`spec/verifier/README.md`, `corpus/negatief/*/expect.txt` (niet `ir.json`, niet `NOTE.md`) en de tabelregels van
`corpus/negatief/README.md` die de vorm van `expect.txt` noemen, `scripts/check-verifier.sh`,
`scripts/check-toka-parity.sh` (het ene patroon), `spec/proposals/MESSAGE-CODES.md` (§2.2 en §7 bijgewerkt op de
stand), deze kaart.

Out of scope:
- Het hernoemen van een melding, het samenvoegen van twee sjablonen of het splitsen van één: een arm die twee
  sjablonen heeft krijgt twee sleutels, de tekst blijft.
- Een sleutel voor de `LOAD ERROR`-regels van de loader en voor de afwijzingen van `ref`, clingo of Drools
  (kaarten 3 en 5).
- Catalogi en vertalingen (kaart 6); de sleutel is geen vertaalsleutel van de verifier, die geen tweede taal heeft.
- Een nieuw `expect.txt` voor de 157 negatiefmappen zonder pin; een nieuwe WF-regel; een wijziging van `ir.json`
  of van de spec.
- De tekst van `IR-SPEC.md` (de WF-index noemt `expect.txt`; zijn zin blijft waar) en `CLAUDE.md` (de gate-3-alinea
  krijgt een voorstel in het Result, zoals in kaart 1).

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch: 1 self-contained, 2 ir-schema, 3
verifier, 4 case-schema, 5 oracle, 6 clingo, 7 no-op, 8 rood, 9 catala, 10 regelspraak, 11 toka-parity, 12 drools,
13 reftype-derivation, 14 flogic, 15 graaf, 16 scenario. Gate 3 in volle omvang (`scripts/check-verifier.sh`);
de rest één keer op de eindstand.

## Result (2026-10-19)

Commits op `card/verifier-arm-keys`: `e4666945` (vorm, toekenning, 354 `expect.txt`, gate 3, documentatie),
`a73d64bb` (hulpfuncties houden hun oude vorm; ref, clingo en hun tests bouwen weer). Alleen lokaal; niet gepusht.

Vorm. `Checks.violation` draagt `key` en `params` (de velden staan achteraan: WF-046 sorteert de schendingen met
de polymorfe `compare` en de rapporten houden zo hun volgorde). `v ?params key at reason` leest de regel uit de
sleutel (`rule_of`, `invalid_arg` zonder `WF-###.<slug>`). Rapportregel `WF-###.<slug> FAIL <pointer>: <tekst>`;
`verifier --arms <ir.json>` drukt `key<TAB>pointer<TAB>{json}` af.

Afwijking van acceptance (a): een kale `v "WF-###"` compileert nog wel (de sleutel is een `string`); hij faalt bij
het bouwen en gate 3 weigert hem in de bron (regex over `checks.ml`, na het weghalen van commentaar). Een
eigen sleuteltype zou 458 plekken en alle lokale `v`'s veranderen zonder meer te toetsen.

Aantallen. 458 plekken, allemaal toegekend: 421 sleutelliteralen in `checks.ml` (426 verschillende
`WF-###.<slug>`-tekens, 437 voorkomens), 9 dynamische `wf ^ ".slug"`/`rule ^ "." ^ slug`-plekken, de overige via
hulpfuncties die slug en parameters met de zin teruggeven. 318 verschillende sleutels over de negatiefrapporten,
213 gepind door 354 `expect.txt` (354 blijft 354), 572 `name=value`-regels. 111 regels.
Haiku: 4 partijen, 458 antwoorden. `check-answer.py` meldde 17 slugs van vier woorden (hernoemd naar drie
woorden in `apply-keys.py`, bijv. `fact-type-role-count` naar `role-count`); `template-check.py` meldde
3 schijnbare botsingen die bij nazien één sjabloon bleken (parameterwaarde in de zin). 427 plekken script,
31 met de hand (26 met `arms` of helper-parameters, 4 dynamisch, 2 records). Slugs die de gate afdwong te
splitsen omdat de zin per arm verschilt: WF-083 (3: `count-source-anchor`, `own-attribute-anchor`,
`set-source-anchor`), WF-118 (`shared-effective-name` en `shared-scoped-name`), WF-098 (`context-date-type`,
`context-year-type`), WF-039 (6 tekenarmen: `sign-*` en `target-*`), WF-047/053/054/028 (de vier armen van
`resolve_binding`, WF-028 met `binding-`-voorvoegsel en `created-role-*` voor `pick`), WF-053 (hop-armen),
WF-109 (4), WF-115 (7: dag-, datum-, dag-maand-, kalender-, kanoniek-, variabel-, adoptie), WF-079 (4:
`computing-*`, `initialisation-*`), WF-113 (3 ernsten). Eén parameter van Haiku liet ik vallen
(`reading` van WF-083, een enum; de arm draagt hem nu in de sleutel); `function` van de WF-038-zinnen is
`Spelling.encode Spelling.builtin_kind f.kind`.

Rewriting van `expect.txt`: `convert-expect.py` (buiten de repo, `werk/.../tools/`): sleutel van de regel waarvan
de tekst het oude fragment bevat (nul of meer dan een sleutel faalt), daarna elke parameter die het fragment
aanhaalt of waarvan de waarde (>= 3 tekens) erin voorkomt; elke aangehaalde naam heeft een parameterregel (bij
`Type.attr` de twee delen; bij samengestelde waarden de hele parameter, bijv. WF-091 `type=`/`sourceType=`).
Eerste run 19 fouten (apostrofs, samengestelde waarden), alle in het script opgelost; geen `expect.txt` met de hand.

(e) Verschil van de volledige verifieruitvoer over `corpus/negatief`, `golden` en `rood` voor/na: 859
documenten, 0 verschillen na het weghalen van `.slug` uit de kop van elke FAIL-regel, uitgangscodes gelijk
(`diff-reports.sh`, de oude uitvoer van een bouw van `main` `719a2377`).

Gate 3. Leest de sleutel (pointerpatroon `^WF-[0-9]+(\.[a-z0-9-]+)? FAIL `); toetst per `expect.txt` de
sleutelregel (de regel van de mapnaam), de `name=value`-regels (geen lege regel, geen dubbele, oude vorm faalt) en
de deelverzameling tegen `--arms`; één sleutel is één sjabloon over alle negatiefrapporten (twee
normalisaties, de waarden vervangen door `{}` en elk getal door `#`; falen als beide meer dan een sjabloon
geven); en in `checks.ml` geen kale regel en slugs van hoogstens drie woorden. Vloeren, alle gemeten 2026-10-19:
`EXPECTED_MIN_EXPECT=354`, `EXPECTED_MIN_ARMS=213`, `EXPECTED_MIN_PARAM_LINES=572`,
`EXPECTED_MIN_REPORT_KEYS=318`, `EXPECTED_MIN_ARM_SITES=421`, `EXPECTED_MIN_POINTERS=619` (was 547). Gate 11:
het sed-patroon in `check-toka-parity.sh` leest `WF-###` met optioneel `.slug`; de holdout-pin blijft
`WF-099 <pointer>`.

Mutaties (d), elk tegen `scripts/check-verifier.sh`, daarna teruggezet met `git checkout`:
1. een andere slug (`WF-005.count-below-one` naar `count-low`): rood, 1 FAIL-regel
   (`wf005-count-zero ... key WF-005.count-below-one ... the WF-005 keys reported are ['WF-005.count-low']`);
2. een verkeerde waarde (`anchor=Jutter` naar `anchor=Jutterx` in `wf083-vouwbron-anker-op-operand`): rood, 1
   FAIL-regel met de gemelde parameters;
3. een sleutel voor twee sjablonen (`own-attribute-anchor` naar `count-source-anchor`): rood, 3 FAIL-regels: de
   pin van `wf083-vouwbron-vreemd-objecttype`, "key WF-083.count-source-anchor stands for 2 message
   templates" en de vloer van gemelde sleutels (317 < 318);
4. een herformulering (`duplicate dimension 'x'` naar `dimension 'x' occurs twice`, sleutel gelijk): groen,
   exit 0, 354 pins; teruggezet, want geen meldingstekst verandert.

Testbestanden en andere bestanden buiten "May touch", mechanisch omdat `violation` twee velden meer draagt
(geen verwachting over gedrag veranderd):
- `ref/lib/eval.ml` regel 3772: recordpatroon `{ wf; at; reason }` kreeg `; _` (waarschuwing 9);
- `ref/test/test_ref.ml` 3 plekken (regels 304, 8598 en 8610) en `backends/clingo/test/test_clingo.ml` 4
  plekken (12444 e.v., 12584, 12639): een record-vergelijking werd een vergelijking van `(wf, at, reason)`;
- `checks.ml` houdt de oude vorm van `resolve_binding`, `wf109_host_sentence`, `fixed_start_point_refusal`,
  `fixed_start_point_variable_refusal`, `fixed_start_point_adoption_refusal`, `literal_variant_refusal`,
  `wf115_findings` en `origin_refusal` (ref, clingo en de loader lezen ze als tekst); de sleutelvarianten
  heten `*_arm`, `*_keyed` of `literal_variant_mismatch`.

Gates op `a73d64bb`, een volledige run (2026-10-19 01:04-01:23): 14 van 16 groen; 08 en 09 eindigden met
rc=143 (SIGTERM van buiten, 13 s en 1 s na hun start, in de `drools-jar`-regel) en zijn los opnieuw gedraaid op
dezelfde stand: gate 8 PASS (50 rode cases, 93 codetoetsen), gate 9 PASS (241 checks, 12 goldens). Gate 3: 348
golden schoon, 511 afgewezen, 354 op hun arm; gate 12 JUnit-run 612 s groen.

Voor de hoofdsessie, voorstel voor de gate-3-alinea van `CLAUDE.md` (beschermd): "Since 2026-10-19 (card
`verifier-arm-keys`) every violation carries a key `WF-###.<slug>` (one per message template within a rule) and
named parameters, the report line is `WF-###.<slug> FAIL <pointer>: <message>`, and `verifier --arms` prints key,
pointer and parameters; `expect.txt` is a key line of the declared rule plus `name=value` lines, matched as a
subset against the parameters of one violation, so a rewording of a message breaks no pin; gate 3 also refuses a
key with two message templates over the negatief reports and a violation built without a key in `checks.ml`;
floors `EXPECTED_MIN_ARMS=213`, `EXPECTED_MIN_PARAM_LINES=572`, `EXPECTED_MIN_REPORT_KEYS=318` and
`EXPECTED_MIN_ARM_SITES=421` (measured 2026-10-19), `EXPECTED_MIN_EXPECT` raised to 354 and
`EXPECTED_MIN_POINTERS` to 619."

Open / vervolg:
- `corpus/negatief/README.md`: de rijen van oudere kaarten spreken nog van "fragment"; alleen de kop is bijgewerkt.
- `spec/proposals/MESSAGE-CODES.md` §7 rij 2 niet als gedaan gemarkeerd (de hoofdsessie doet de kaartverplaatsing);
  §2.2 draagt de stand.
- Een sleutel heeft geen vertaalsleutel van de verifier (kaart 6); parameterwaarden als `scope= of the object
  type 'Jutter'` (WF-118) dragen de zinsfragmenten van de helper; een schonere parameter is vervolgwerk.
- De sjabloontoets (twee normalisaties) kan een sleutel met twee zinnen missen als parameterwaarden in beide
  normalisaties toevallig samenvallen met zinsdelen; de mutatie (3) laat zien dat een echte splitsing wel faalt.
