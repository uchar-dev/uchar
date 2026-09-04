# De DRL-teksttak meetbaar: de tweede transpiler onder het harnas

Goal: gate 13 (het annotatie-onverschilligheidsharnas) speelt
uitsluitend de exec-model-tak — de replay-runner bouwt alleen die — en
de ~27 EffectieveEenheid-omzettingen in `DrlTranspiler` zijn daardoor
enkel gecompileerd, gedragen door drie JUnit-getuigen die geen eenheden
raken (B-lens-2-meting 2026-09-04: op het corpus wijken vier call-sites
ooit van de annotatie af, alle vier exec-model). Maak de DRL-tak
meetbaar (maintaineropdracht 2026-09-04, tweede golf).

Aanpak: EERST meten. (a) Kan de replay-runner een DRL-leg krijgen — de
teksttranspiler bestaat; wat kost het om een case door de
gegenereerde DRL te spelen (KieBase uit DRL-tekst i.p.v. exec-model),
en spelen de 106 goldens daar überhaupt (de tekst-tak kan eigen
vorm-grenzen hebben — inventariseer de weigeringen)? (b) Zo niet of te
duur: mutatie-gemeten JUnit-getuigen per omgezette DRL-plek (het
EffectieveEenheidWitnessTest-patroon: per plek een getuige die op de
annotatie-terugval-mutant rood is). Kies op de meting, motiveer in de
kaart, en bouw het gekozen spoor. Een harnas-/gate-13-uitbreiding gaat
als voorstel (scripts/ is beschermd) en de gate mag alleen groeien als
hij groen is.

Acceptance:
- De meting gedocumenteerd (DRL-speelbaarheid per golden of de
  onhaalbaarheidsmotivering).
- Elke omgezette DRL-plek aantoonbaar gedekt: harnas-leg met
  mutant-demonstratie, óf getuigen die op de plekmutant rood zijn.
- Alle dertien gates groen; gate-13-wijzigingen als voorstel, alleen
  groen toegevoegd.

May touch: `backends/drools/src/**` (incl. het kaart-eigen
testbestand), `scripts/check-reftype-derivation.sh` (voorstel),
`backlog/`.

Out of scope: frontends, verifier, spec/IR-SPEC.md; RUNTIME-SEMANTICS
alleen als de gate-vorm normatief verandert — dan eerst de vraag.
