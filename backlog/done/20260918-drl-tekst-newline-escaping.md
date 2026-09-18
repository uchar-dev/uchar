# drl-tekst-newline-escaping: regeleinden overleven de DRL-tekst-tak niet

Vondst van de tekstlexeem-lensronde (2026-09-18), pre-existent en door
die kaart niet geraakt: een `Tekst`-waarde met een regeleinde speelt in
exec-model maar faalt in de classic-DRL-tak — `Namen.drlString`
escapeert `\` en `"`, maar geen CR/LF, dus het regeleinde bereikt de
gegenereerde DRL als letterlijke regelbreuk en de waarde vermindert
stil (`verwacht uit\nvoer, werkelijk uitvoer`; ref ≢ DRL). Een enkele
backslash als hele waarde geeft in de DRL-tak een luide
compile-weigering (`EXCLUDE drl compile`), geen fout antwoord — dat is
de eerlijke vorm. Lege string, kale `"`, `a\"b` en `pad\naar` (met
echte backslash) spelen in beide takken.

Geen corpusgetuige draagt deze vorm; de flank is met losse synthetische
modellen gemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) `drlString` escapeert ook regeleinden (en te bepalen: overige
  controltekens) zodat een tekstwaarde met newline in de DRL-tekst-tak
  dezelfde uitkomst geeft als exec, ref en clingo; dual-branchgetuige
  erbij.
- (b) De losse-backslashvorm wordt óf gedragen, óf blijft een luide
  per-vormweigering met reden — nooit een stil fout antwoord.
- (c) Alle veertien gates groen; kanaaltellingen ongewijzigd tenzij een
  nieuwe golden deze flank in het corpus pint (dan het gebruikelijke
  beschermd-padvoorstel voor de telling).

## Result

`Namen.drlString` escapeert nu ook CR en LF; de getuigenklasse
`TekstLexeemWitnessTest` kreeg een zesde sectie. Geen corpusbestand, geen
script, geen spec.

**Rood eerst, letterlijk gemeten (2026-09-18, jar vóór de fix).** Vijf
synthetische documenten op Jutterswet-vocabulaire, elk gebouwd uit
`corpus/golden/jutterswet-keurmerk` (één Vondst, `keurmerk` vergeleken,
`afslag` geschreven). Alle vijf zijn verifier-clean en ref speelt ze:
`verifier: PASS` en `oracle: PASS (3 expectations)` voor elk.

| vorm | exec-model | DRL-tekst (vóór) |
| --- | --- | --- |
| literal én casewaarde `uit\nvoer`, gevolg `afslag\nnoord` | `PLAYED 1` | `FAIL case.json: attribuut afslag: verwacht afslag\nnoord, werkelijk afslagnoord` |
| literal `uit\nvoer`, casewaarde `uitvoer` (moet missen) | `PLAYED 1` | `PLAYED 1` |
| literal én casewaarde `uit\rvoer` | `PLAYED 1` | `FAIL case.json: attribuut afslag: verwacht uit\rvoer, werkelijk uitvoer` |
| losse backslash als hele waarde | `PLAYED 1` | `EXCLUDE drl compile: … Unable to build expression for 'consequence' …` |
| tab `in\tslag` | `PLAYED 1` | `PLAYED 1` |

Dat scherpt de vondst: de constraint-kant hield het regeleinde wél vast (de
tweede rij mist terecht, en in de eerste rij was alleen `afslag` fout, niet
`gekeurd`), de consequence-voorbewerking gooide het weg. De verminking zit
dus uitsluitend op de SCHRIJFkant.

Clingo is op deze flank geen arbiter: `TRANSLATION FAULT: name or label
"uit\nvoer" carries a character the ASP string encoding cannot quote` — een
luide weigering voor CR, LF én tab. Ref is de arbiter, zoals de kaart
voorzag.

**De controltekenvraag, gemeten in plaats van aangenomen.** Naast tab zijn
form feed, backspace, verticale tab en NUL door beide takken gehaald: alle
vier `PLAYED 1` / `PLAYED 1`, vóór de fix. Alleen CR en LF breken. Daarom
escapeert de fix precies die twee en geen teken meer — escapen zonder
getuige zou scope zonder grond zijn, en de motivering staat in de javadoc.

**Groen ná.** Dezelfde vijf documenten plus de vier extra controltekens op de
gebouwde jar: alles `PLAYED 1` in beide takken, met één uitzondering (de
backslashvorm, hieronder). Ook de mengvorm — een echte backslash gevolgd
door een regeleinde, in één waarde — speelt nu in beide takken; vóór de fix
verloor die op precies dezelfde manier haar regeleinde op de schrijfkant.

**De backslash-uitkomst, expliciet.** Ongewijzigd door deze fix, want de
backslash werd al geëscapet: een waarde die OP een backslash EINDIGT blijft
in de DRL-tekst-tak een luide per-vormweigering, `EXCLUDE drl compile: …
Unable to build expression for 'consequence' …`. Nooit een stil fout
antwoord, dus acceptatie (b) staat — als weigering, niet als dracht. De
vorm is scherper dan de kaart hem stelde: niet "een losse backslash" maar
"eindigt op een backslash" (`pad\` weigert identiek), terwijl een backslash
in het MIDDEN (`pad\naar`, echte backslash) in beide takken speelt. De
oorzaak is dat de verdubbelde backslash de consequence-voorbewerking de
sluitquote als geëscapet laat lezen; dat rechtzetten zou moeten veranderen
hoe de consequence wordt opgebouwd, niet hoe een waarde wordt gespeld — dus
buiten het may-touch-bereik van deze kaart, en niet gedaan.

**Getuigen, met feitelijk uitgevoerde mutatieproef.** Drie tests in
`TekstLexeemWitnessTest` (8 → 11 tests), elk op ÉÉN document met BEIDE
takken:

- `een_regeleinde_overleeft_vergelijking_en_gevolg_in_beide_takken` —
  vergelijking én gevolg dragen het regeleinde, en de test doet dit voor LF
  én CR, omdat het twee losse substituties zijn en een getuige op één de
  ander vrijlaat.
- `een_regeleindedragend_literal_raakt_de_platgeslagen_casewaarde_niet` —
  de spiegelrichting, gepind tegen een toekomstige "fix" die regeleinden
  NORMALISEERT in plaats van escapet; zo'n helper zou de eerste getuige
  halen (beide takken verliezen hetzelfde teken en zijn het eens) en hier
  een regel vuren die de wire niet vergunt. Speelde vóór de fix ook al.
- `een_waarde_die_op_een_backslash_eindigt_weigert_luid_in_de_tekst_tak` —
  exec `PLAYED`, DRL-tekst `EXCLUDE` met `reason` beginnend op
  `drl compile: `. De niet-gedragen vorm staat daarmee gepind als LUID in
  plaats van onuitgesproken.

Mutatieproef, uitgevoerd en hersteld, twee keer:

1. beide nieuwe escapes weg → `Tests run: 11, Failures: 1`,
   `een_regeleinde_overleeft…` rood op
   `attribuut afslag: verwacht afslag\nnoord, werkelijk afslagnoord`;
2. alleen de CR-escape weg (LF blijft) → opnieuw `Tests run: 11,
   Failures: 1`, dezelfde getuige rood op
   `verwacht afslag\rnoord, werkelijk afslagnoord`.

Beide keren hersteld; daarna `Tests run: 400, Failures: 0, Errors: 0,
Skipped: 0` en `BUILD SUCCESS`.

**Gates (alle veertien, exitcode per gate).**

```
self-contained: 0        rood: 0
ir-schema: 0             catala: 0
verifier: 0              regelspraak: 0
case-schema: 0           toka-parity: 0
oracle: 0                drools: 0
clingo: 0                reftype-derivation: 0
noop: 0                  flogic: 0
```

Gate 12: `played 148`, `auto-excluded 47`, `value-divergence 1`,
`no-case 2`, `channel floor 50 non-PLAYED goldens pinned by name; played
matches the pinned 148`, `JUnit 400 tests, floor 239`. Gate 13:
`108 unit annotations twisted over 22 goldens, floor 74; 100 executor
replays …, 21 of them PLAYED by the DRL-text branch, floor 13; 0 pinned
survivors`.

**Pins onaangeroerd.** `EXPECTED_PLAYED` blijft 148, de kanaalvloer blijft
50 met naam, geen enkele `EXPECTED_MIN_*` is aangeraakt en geen
survivor-/divergentielijst is verschoven: `git diff --name-only main...HEAD`
noemt geen enkel bestand onder `scripts/`. Alleen de JUnit-telling groeit
(397 → 400), wat een vloer per definitie toestaat.

**May touch, geverifieerd.** `git diff --name-only main...HEAD`:
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/Namen.java`,
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/TekstLexeemWitnessTest.java`,
`backlog/doing→done/20260918-drl-tekst-newline-escaping.md`. Geen
`DrlTranspiler.java`, geen `ExecModelTranspiler.java`, geen `scripts/`,
`spec/` of `corpus/`.

**Registervoorstel: geen.** Dit is takconformiteit — de DRL-tekst-tak gaat
doen wat exec-model, ref en de wire al deden. Geen enkel construct, geen
spec-zin en geen v1-afwijking verandert, dus `spec/CONSTRUCTS.md` noch
`spec/PLAN.md` heeft een rij nodig.

**Bewust niet gedaan.** Geen corpusgolden voor deze flank: clingo weigert
regeleinden luid in de ASP-stringcodering, dus een golden met deze vorm zou
gate 6 breken. De flank is daarom in de getuigenklasse gepind en niet in het
corpus — wat de kaart als mogelijkheid openliet en hier meetbaar gesloten
wordt.
