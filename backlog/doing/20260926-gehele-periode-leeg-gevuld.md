# gehele-periode-leeg-gevuld: de leeg/gevuld-test onder "gedurende de gehele periode" krijgt haar semantiek en haar uitvoering

De wire draagt `wholePeriod?` op `Predicate` (IR-SPEC ch. 4.5; de frontend
emitteert het, `IrEmitter` r. 1222), maar geen uitvoerder voert de vorm
uit: de Drools-lader weigert haar onder de sleutel `Predicaat.periode`
("a leeg/gevuld test under a whole-period qualifier (ch. 3.9): ref
executes neither the leeg fold (touched days) nor the gevuld fold (days(B)),
so this backend does not answer the form either", `IrLoader` r. 2041) en de
ontkende vorm onder `Predicaat.negatie` ("the threshold of the negated test
is not normed"). Geen golden draagt de vorm. De oude toolchain vuurde haar
via de bronroute; de maintainer besloot op 2026-09-26 dat uchar haar nu
draagt.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Semantiek in RS ch. 3.9, in de lijn van de bestaande
  gehele-periode-clausules voor vergelijkingen en kenmerken: "gedurende de
  gehele periode leeg" is waar desda de tijdlijnwaarde op elke dag van de
  periode leeg is; "gevuld" desda op elke dag gevuld; de ontkenning is de
  gewone driewaardige ontkenning van die uitkomst (geen aparte drempel).
  De bouwer toetst dit aan de RegelSpraak-specificatie (read-only, §8.2)
  en legt een afwijking als dialectkeuze vast. Worked example → `ref/test`.
- U2 De drie uitvoerders voeren de vorm uit (ref, clingo, beide
  Drools-takken); beide lader-weigeringen verdwijnen; de sleutels
  `Predicaat.periode` en `Predicaat.negatie` gaan uit de redenlijsten van
  gate 12.
- U3 Golden `jutterswet-gehele-periode-leeg` (**voorstel**): een tijdlijn-
  attribuut, een regel met de positieve test, een met de ontkende test,
  cases met een geheel lege, geheel gevulde en gedeeltelijk gevulde
  periode; ref ≡ clingo ≡ Drools. Negatief alleen als een nieuwe MUST
  ontstaat (de bouwer meet; standaard geen nieuwe WF).
- U4 Pins: gate 12 +1 op beide takken, klassekop herrekend; gate 7 en 13
  per de vaste patronen.
- U5 Klaar: alle veertien gates groen; naamtabel-toets rc 0; gelijke
  zinnen in ref, clingo en de Drools-lader zonder regelnummerstaart.

## Acceptatie

- (a) U1–U5; (b) `git grep "Predicaat.periode"` leeg; (c) de drie
  uitkomstklassen (leeg, gevuld, gemengd) in de cases.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 3.9 (**voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `corpus/golden/
jutterswet-gehele-periode-leeg/`, gatepins (**voorstel**). Niet in deze
kaart: de frontend, het schema, de verifier (tenzij U3 een MUST vraagt).

## Result (2026-09-26, bouwer)

### Stand

- Basis: main `b88a8ccc`. Claim: `ef43161a`. Kop: de commit die dit
  Result in de kaart zet (de hash staat in het eindbericht aan de
  hoofdsessie). Werkboom schoon na die commit. Niet gepusht, niet gemerged.
- Branch `card/gehele-periode-leeg-gevuld`, worktree `wt/periode`.

### Commits (na de claim)

1. `6415d063` gehele periode: leeg/gevuld-toets krijgt haar semantiek
   (voorstel) — `spec/RUNTIME-SEMANTICS.md`: ch. 3.9 (formule, leeg-bullet,
   eerste worked example, nieuwe alinea met tabel), de leeg/gevuld-rij van
   ch. 1.2 en de leeg/gevuld-rij van ch. 3.12.
2. `d0a0f6de` gehele periode: ref, clingo en Drools voeren leeg/gevuld uit
   — `ref/`, `backends/clingo/`, `backends/drools/` (lader, runtime, twee
   commentaren in de transpilers, `LeegNegatieWitnessTest`), de golden,
   de vier rode gevallen weg, NOTE van `jutterswet-waardemelding`, pins in
   `scripts/check-drools.sh` en `scripts/check-rood.sh`.
3. `48b785b4` getuigen voor een keten van to-one-hops (ref- en
   clingo-suite).
4. `2cd2ea32` keten van hops in ch. 3.12 nauwkeurig (voorstel).
5. `1b855db6` NOTE van de golden zonder de vervallen weigersleutel.
6. `74ceffd9` gate 12: redenblok noemt ook de runtimeregel voor een leeg
   segment (alleen commentaar).
7. deze Result-commit.

### U1 — semantiek in RS ch. 3.9

Wat er staat. Eén drempel voor elke toets: `satisfied-days(B, toets) ≥
days(B)` (de aparte `touched-days(B)` van de leeg-toets is weg). Voor de
twee toetsen: `satisfied-days(B, gevuld)` = de dagen van B waarop de
tijdlijn een waarde leest, `satisfied-days(B, leeg)` = de dagen waarop ze
leeg leest, gatdagen inbegrepen. De geraakte emmers lezen deze twee
toetsen af van `read` (ch. 3.1): een expliciet leeg segment is een gat en
raakt geen emmer. Gevolg: de leeg-toets is "de tijdlijn leest op geen
enkele dag een waarde"; het grid verandert haar oordeel niet. Een
tijdlijn zonder waarde slaagt voor beide toetsen (gevuld vacuüm). De
binnenste ontkenning (`negated`) is per dag, dus ontkend leeg = gevuld en
ontkend gevuld = leeg, met dezelfde emmers en drempel; de negatie van de
kwalificatie (`WholePeriod.negated`) complementeert de gevouwen uitkomst.
Operand: eigen tijdsafhankelijk attribuut of via to-one-hops; een hop die
niets bereikt geeft de null-tijdlijn en slaagt. Een tijdsafhankelijke
parameter passeert WF-043 en weigeren alle drie bij naam. De ch. 1.2-rij
en de ch. 3.12-rij volgen.

Toets aan de RegelSpraak-specificatie v2.3.0 §8.2 (gelezen, niet
geciteerd; parafrase): de toevoeging levert alleen waar op als de
controle van het predicaat op elk moment van de opgegeven kalenderperiode
waar is; de voorbeelden omvatten de leeg-controle onder een
maandkwalificatie en de ontkennende vorm vóór de kwalificatie; de
predicatentabel van §8.1 kent voor lege waardes alleen leeg en gevuld,
zonder ontkende vorm. Uitkomst: **geen afwijking** voor de ongenegeerde
toetsen en de ontkenning van de kwalificatie. De binnenste ontkenning van
leeg/gevuld heeft in RegelSpraak geen spelling; haar lezing is die van
uchar en staat zo in de spec (bullet "Source"). Twee bestaande
uchar-lezingen blijven ongewijzigd staan (in de lijn van de bestaande
clausules): het vouwen tot één boolean over de geraakte emmers en de
vacuüm-regel; de productieruntime in `backends/drools` (`rsGeheelPeriode`)
geeft `true` voor een lege of null-tijdlijn, dus ook daar geen afwijking.

Gemeten.
- Worked example → ref/test: `WV-3.9-leeg-*`, 43 checks (tabelrijen a–d
  × vijf kolommen, maandgrid, jaargrens, deelmaand buiten het grid,
  expliciet leeg segment, één hop, keten van twee hops, parameter,
  niet-tijdsafhankelijk). `test_ref: all 1090 worked examples pass`
  (basis 1047).
- Rood eerst (ref): de nieuwe tests tegen de basis-`eval.ml`/`timeline.ml`:
  `FAIL <onafgevangen fault> … fault: whole-period leeg/gevuld test is
  ch. 3.9 scope, not executed here`, exit 2; terug met `cmp: identiek`,
  groen.
- Mutatieproef op de keuze binnen/buiten: ref met de binnenste negatie
  als negatie van de gevouwen uitkomst: golden case1 en case3 falen
  (`MISMATCH: S1.nietvultoeslag = leeg, expected 40`, …; `MISMATCH:
  S1.nietvultoeslag = 40, expected leeg`, …), case2 slaagt (op een vol
  jaar vallen beide lezingen samen); terug met `cmp: identiek`.

### U2 — de drie uitvoerders

- ref: `whole_period_operand` + arm in `eval_cond`; vouw
  `Timeline.geheel_periode_leeg_gevuld` (emmers uit de gevulde segmenten,
  drempel days(B)).
- clingo: `whole_period_leeg_gevuld_fails` (gevuld: de _t/_g-emmervouw
  zonder waardevoorwaarde; leeg: faalt zodra de subject een gevuld
  segment heeft; hops via `rol/3` in een hulppredicaat) + arm in
  `cond_atoms_raw`. Geen recursieve-creatiewacht zoals op de gewone
  leeg-arm: WF-046 telt elke lezing onder een leeg/gevuld-toets als
  negatieve kant, ook onder de kwalificatie (verifier `checks.ml`, arm
  `Predicaat { soort = Leeg | Gevuld; … }`), net als bij de
  gehele-periodevergelijking.
- Drools: beide laderweigeringen weg; de verlaging van de binnenste
  ontkenning naar de zustertoets geldt nu ook onder de kwalificatie; de
  runtime-vouw bestond al (toets 6/8). `rsGeheelPeriode` toets 6 slaat
  een leeg segment over (ch. 3.1); toets 8 was al equivalent.
  Nieuwe laderweigering alleen voor de parameter-operand, sleutel
  `Predicate.operand` (L5-spelling).
- `git grep "Predicaat.periode" -- ':!backlog'` → alleen
  `spec/CONSTRUCTS.md` r. 113 (register, voorstel hieronder); code,
  spec-hoofdstukken, corpus en scripts zijn leeg. `Predicaat.negatie`
  idem, plus een grammaticacommentaar in `frontends/regelspraak` dat het
  recordveld bedoelt (buiten de kaart).
- De redenlijsten van gate 12 bevatten de sleutels niet (geen golden
  droeg de vorm); gate 8's Drools-been had vier regels voor de rode
  gevallen — weg met de gevallen.
- Rood eerst (clingo): nieuwe frames tegen de basis-`translate.ml`:
  `test_clingo: 1946 checks, 36 FAILURES` (`… clingo pins the values
  (fault: whole-period leeg/gevuld test is ch. 3.9 scope, not executed
  here)`); terug met `cmp: identiek`, `0 failures`. Mutant "keten tot de
  eerste hop": `2 FAILURES`, precies de twee ketenframes.
- Rood eerst (Drools): `LeegNegatieWitnessTest` tegen de basis-lader en
  -runtime: `Tests run: 6, Failures: 1, Errors: 2` (`Refusal … Predicaat.
  negatie …`, `Refusal … Predicaat.periode …`, `gevuld: no bucket is
  touched ==> expected: <true> but was: <false>`); terug met `cmp:
  identiek`, `Tests run: 6, Failures: 0`. De parametertoets tegen de
  basis-lader: `Failures: 1` (de oude `Predicaat.periode`-melding); daarna
  7/7 groen.
- Golden door de basis-jar: exec en DRL-tekst `EXCLUDE loader: form not
  carried by the v0 Drools backend: …` (de oude weigering); door de
  basis-ref/-clingo: `EVALUATION FAULT` / `TRANSLATION FAULT` met de oude
  zin. Nu: `oracle: PASS (15 expectations)` en `clingo: PASS (15
  expectations)` per case, `PLAYED 3` in beide takken.

### U3 — golden `jutterswet-gehele-periode-leeg`

Handgeschreven (geen `bron.rgs`: twee regels dragen de binnenste
ontkenning). Schuit.bemanning en Werf.havengeld maandgranulair; zeven
regels (leeg, gevuld, niet leeg, niet gevuld, niet gedurende … leeg,
gevuld per maand, gevuld over de werf). Cases: `case1-geheel-leeg`,
`case2-geheel-gevuld`, `case3-gemengd` (15 verwachtingen elk);
handberekening in de NOTE. Verifier PASS (ook met de basis-verifier).
Negatief: **geen nieuwe WF** (standaard toegepast) — WF-043 laat alle
vormen al toe of weigert ze; de parameter-operand is een weigering van de
uitvoerders, geen welgevormdheidsregel. De vier rode gevallen
`corpus/rood/{leegtest,leegnegatie,gevuldtest,gevuldnegatie}-heel-tijdvak`
zijn verwijderd: hun weigering bestaat niet meer, en hun inhoud (vol jaar,
geen tijdlijn) zit in case2 en case1.

### U4 — pins

- Gate 12: `EXPECTED_PLAYED` 209 → 210, `EXPECTED_DRL_PLAYED` 179 → 180,
  elk met redenblok. Klassekoppen van `DRL_CHANNEL` ongewijzigd: de golden
  speelt in beide takken, dus geen klasse wijzigt; gemeten diff van het
  gaterapport basis ↔ nu: alleen +1 golden, played +1 in beide takken,
  JUnit 814 → 816; de 52 auto-exclusies letterlijk gelijk.
- Gate 7: geen survivor (elke case pint afgeleide waarden); 665 → 668
  weerleggend, 44 survivors ongewijzigd.
- Gate 13: geen pin; de golden draagt geen eenheid (41 twists over 23
  goldens, de golden komt in het rapport niet voor).
- Gate 8: vier regels uit de Drools-lijst; vloer 26 blijft (39 ≥ 26).

### U5 — klaar

Alle veertien gates, elk los na `eval $(opam env)`, op `d0a0f6de`; gates
5 en 6 opnieuw na de ketengetuigen (`48b785b4`), gate 1 opnieuw na de
NOTE-correctie (`1b855db6`), gate 12 opnieuw na de commentaarwijziging in
zijn script (`74ceffd9`), elk weer PASS met de getallen hieronder:

| gate | uitkomst |
|---|---|
| 1 self-contained | PASS |
| 2 ir-schema | PASS — 304 bestanden (basis 307: −4 rood, +1 golden), vloer 192; 37934 objecten in 785 documenten in schemavolgorde |
| 3 verifier | PASS — 304 golden/rood schoon; 481 negatief, vloer 350; 324 op arm, vloer 260; 584 pointers, vloer 547 |
| 4 case-schema | PASS — 751 (basis 752) |
| 5 oracle | PASS — ref-suite 1090 (basis 1047), vloer 489; 712 cases (basis 709), vloer 427 |
| 6 clingo | PASS — suite 1952 (basis 1878), vloer 83; 712 cases |
| 7 noop | PASS — 668 weerleggen, 44 survivors |
| 8 rood | PASS — 39 (basis 43), vloer 26; Drools-been 11 gepind / 28 met reden (basis 32); 78 fragmenten (basis 86), 36 bodemzinnen |
| 9 catala | PASS — 241 checks, vloer 201; 12 goldens |
| 10 regelspraak | PASS — 764 tests; 93 rgs-goldens, vloer 80 |
| 11 pariteitszone | PASS — 45 her-emissies, vloer 45; suite 27; 28 slices verifier-schoon; 29 cases door ref, clingo, Drools |
| 12 drools | PASS — played 210 = pin; DRL-tekst 180 = pin; JUnit 816, vloer 239 |
| 13 reftype-derivation | PASS — 41 twists over 23 goldens, vloer 38; 106 replays, 22 DRL, vloer 20 |
| 14 flogic | PASS — 150 checks, vloer 81; 2 goldens |

Naamtabel-toets: `python3 spec/proposals/v1-naamtabel-toets.py` →
`v1-naamtabel-toets: OK`, rc 0. Logs: `/tmp/golf/logs/periode/`.

### Zinnen letterlijk (geen regelnummerstaart)

- Parameter-operand, gelijk in ref (`EVALUATION FAULT: …`), clingo
  (`TRANSLATION FAULT: …`) en de Drools-lader (`EXCLUDE loader: form not
  carried by the v0 Drools backend: Predicate.operand (…)`, beide takken),
  gemeten op een proef-IR (verifier PASS):
  `a whole-period leeg/gevuld test over the parameter 'havenpeil' is not
  executed: the fold reads the timeline of an attribute`
- Niet-tijdsafhankelijke operand (WF-043-gordel), gelijk aan de
  verifierliteral, gemeten: verifier `WF-043 FAIL
  /rules/rules/0/versions/0/condition: wholePeriod qualifier on a
  non-time-dependent operand`, ref en clingo `wholePeriod qualifier on a
  non-time-dependent operand`.
- Hergebruikt in clingo (bestaand, ongewijzigd): de WF-053-zin van de
  eigen lezing en `navigation over the to-many role %S in a scalar slot is
  refused` (ref's `nav_target`).
- Verdwenen: `whole-period leeg/gevuld test is ch. 3.9 scope, not
  executed here` (ref, clingo) en de twee Drools-laderzinnen.
- Keten van twee hops, gemeten: verifier PASS, ref en clingo PASS, exec
  `EXCLUDE transpiler SKIP: … (voorwaarde buiten Phase-B scope) …`,
  DRL-tekst `EXCLUDE drl-transpiler MARKER: … TODO §8.2 gehele-periode …`
  — de takken verlagen één hop; zo vastgelegd in ch. 3.12.

### Buiten de letterlijke may-touch, met reden

- `corpus/rood/*-heel-tijdvak` (vier mappen weg) en de vier regels in
  `scripts/check-rood.sh`: de gevallen pinden precies de weigering die de
  kaart opheft; gate 8 faalt anders.
- `corpus/golden/jutterswet-waardemelding/NOTE.md`: verwees naar die
  rode gevallen als geldende weigering.
- RS ch. 1.2-rij en ch. 3.12-rij (naast ch. 3.9): beide beweerden dat alle
  drie weigeren en noemden de rode gevallen.
- Testbestanden: `LeegNegatieWitnessTest` (twee weigertoetsen werden
  speeltoetsen, twee toetsen erbij), `test_clingo.ml`, `test_ref.ml` — het
  mandaat van U2 maakt de oude weigerasserties onwaar.

### Voorgestelde registertekst

`spec/CONSTRUCTS.md`, rij 29, in de laatste cel direct na de zin die
begint met "**Alleen de leeg/gevuld-toetsen blijven
gespecificeerd-maar-door-geen-OCaml-uitvoerder-uitgevoerd:**" (en eindigt
op "een groene deelverzameling."):

"**Landed (2026-09-26, kaart gehele-periode-leeg-gevuld) — dit vervangt
de zin hierboven dat alleen de leeg/gevuld-toetsen door geen uitvoerder
worden uitgevoerd:** ze draaien nu in alle drie de uitvoerders, beide
Drools-takken inbegrepen, over een eigen operand en één to-one-hop, beide
grids en beide negaties. Eén drempel, days(B), voor elke toets; een lege
dag is een leeg-dag; de geraakte emmers van deze twee toetsen volgen uit
read (ch. 3.1), zodat een expliciet leeg segment geen emmer raakt. De
binnenste negatie is per dag: ontkend leeg is de gevuld-toets, ontkend
gevuld de leeg-toets. Getuige `corpus/golden/jutterswet-gehele-periode-leeg`
(geheel leeg, geheel gevuld, gemengd); de vier rode gevallen
`*-heel-tijdvak` zijn vervallen. Een keten van hops voeren ref en de
ASP-backend uit en weigeren beide Drools-takken bij naam; een
tijdsafhankelijke parameter als operand weigeren alle drie in één zin."

`spec/CONSTRUCTS.md`, rij 113 (`— Predicaatsoort`): vervang de zin
"Under a whole-period qualifier it refuses by name (`Predicaat.negatie`):
… ref faults on every whole-period leeg/gevuld test." door "Under a
whole-period qualifier the same lowering applies since 2026-09-26 (card
`gehele-periode-leeg-gevuld`): RUNTIME-SEMANTICS ch. 3.9 gives every test
the threshold days(B), so the negated leeg test is the gevuld test there
too." — en vervang het stuk van "Under a whole-period qualifier the ASP
backend refuses every leeg/gevuld test in ref's own words, as before."
tot en met "… so for that corner the case is the only guard on the Drools
refusal." door: "Under the whole-period qualifier all three executors
execute the tests since 2026-09-26 (golden
`jutterswet-gehele-periode-leeg`); the Drools loader's two refusals and
the four red twins `corpus/rood/*-heel-tijdvak` that held them are gone.
A time-dependent parameter as the operand is refused in one sentence by
ref, the ASP backend and the Drools loader (`Predicate.operand`)." Daarna
geeft `git grep "Predicaat.periode" -- ':!backlog'` niets meer.

`spec/PLAN.md`: geen wijziging nodig; de golfpassage (§9) noemt de kaart
al. `CLAUDE.md`: geen wijziging; er kwam geen gate bij en geen gate
verandert van omschrijving.

### Normvragen (standaardantwoord toegepast)

1. "De ontkenning is de gewone driewaardige ontkenning van die uitkomst"
   (U1): per dag of van de gevouwen uitkomst? Toegepast: **per dag** —
   ch. 1.2 normeert `negated` al als de binnenste, per-waarde-ontkenning,
   ch. 3.9 leest de binnenste ontkenning van de kenmerkcheck per dag, en
   §8.2 past de toevoeging toe op de controle die het predicaat
   beschrijft; de buitenste ontkenning is `WholePeriod.negated`. De golden
   onderscheidt de twee lezingen (mutatieproef hierboven).
2. "Gedurende het gehele jaar gevuld" op een tijdlijn zonder enige waarde:
   waar? Toegepast: **ja, vacuüm**, zoals ch. 3.9 het voor elke toets zegt
   en de productieruntime antwoordt; case1 pint het (`vultoeslag` 20 bij
   geen bemanning).

### Open punten

1. **Expliciet leeg segment onder een gehele-periodeVERGELIJKING.** Voor
   leeg/gevuld lezen nu alle drie de uitvoerders de geraakte emmers af van
   read. Voor de ordenings- en gelijkheidstoetsen niet: ref
   (`geheel_periode_waarde`) en de Drools-runtime (toets 0–5) tellen een
   expliciet leeg segment als rakend, de ASP-codering kan het niet
   schrijven (leeg = afwezigheid); daarnaast leest ref `≠` op zo'n dag als
   voldaan (Tabel 44) en de Drools-runtime niet. Gevonden door code te
   lezen, niet gemeten op een document: zo'n segment ontstaat alleen via
   de temporele som van ch. 3.5, en geen golden legt er een
   gehele-periodevergelijking op. Buiten deze kaart (die gaat over
   leeg/gevuld); vraagt een normbesluit voor ch. 3.9 en een getuige.
2. **Register.** CONSTRUCTS-rijen 29 en 113 (tekst hierboven); tot de
   landing staat `Predicaat.periode` nog in rij 113, dus acceptatie (b)
   geldt buiten het register.
3. **Bronroute.** De golden heeft geen `bron.rgs`, dus gate 10 toetst de
   emissie van de spelbare vormen (leeg, gevuld, "niet gedurende", het
   maandgrid, de genavigeerde operand) niet. Een `-rgs`-tweeling met
   alleen die vormen zou dat doen; niet gemeten of de frontend ze
   byte-gelijk aan deze IR emitteert.
