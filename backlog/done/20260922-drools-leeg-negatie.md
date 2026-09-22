# drools-leeg-negatie: NIET-LEEG-predicaat stil genegeerd in beide takken

Gemeten door de P1-nalens (2026-09-22, probe buiten het kaartobject):
een `Predicaat LEEG` met `negatie: true` op een verifier-schone
conditie wordt door BEIDE Drools-takken genegeerd — ref PASS, exec en
DRL-tekst spelen en geven het foute antwoord ("verwacht 40, werkelijk
leeg"). Dat is de verboden klasse: geen weigering maar een stil
verkeerd antwoord op een verifier-schoon document. Het gebeurt ook bij
een ongebonden geschreven enkele stap, dus het is ouder dan en staat
los van de P1-port. Corpusdragers: nul (alleen GEVULD met
`negatie: false`: 1× golden, 21× negatief, 2× rood — hermeten
2026-09-23 over corpus/ en toka/, de oorspronkelijke "23×" klopte
niet; LEEG komt nergens voor).

## Acceptatie (aangescherpt bij de claim, 2026-09-22)

- (a) Reproduceer de probe op main db224b04 (na P1–P3): een
  verifier-schone conditie met `Predicaat LEEG, negatie: true`, zowel
  op een eigen attribuut als over een enkele navigatiestap, en het
  zusje `GEVULD, negatie: true` (dezelfde veldnegeer-klasse). Meet
  verifier, ref, exec-tak en DRL-tekst-tak; letterlijke uitvoer in het
  Result. Leg vast welke speczin de negatie normeert (IR-SPEC ch. 4
  Predicate-rij, RUNTIME-SEMANTICS ch. 1.2) en wat ref ermee doet.
- (b) Beide Drools-takken dragen de negatie (voorkeur: de genegeerde
  lege-/gevuldtest is precies de complementaire test, dus mechanisch te
  lowern) of weigeren benoemd waar dat niet exact is (een negatie op
  een geheel-periode-kwalificatie). Rood-eerst een JUnit-getuige via de
  replay-API voor BEIDE takken; een hand-geschreven golden in
  `corpus/golden/` met handberekende verwachtingen in een NOTE.md,
  gate 2/3/4-conform (schema-valide, round-trip byte-identiek, case
  naast ir, verifier-schoon). Gate-12-pins `EXPECTED_PLAYED` en
  `EXPECTED_DRL_PLAYED` elk +1 met redenregel — beschermd-pad-voorstel.
  Negatief- of ref-getuige alleen als de spec er een regel voor draagt.
- (c) Corpussweep beide takken vóór/na over alle goldens en rood: geen
  bestaande golden wisselt van kanaal of waarde, runner-uitvoer
  byte-identiek op de nieuwe golden na.
- (d) Alle veertien gates groen, elk als losse aanroep. Mutatieproef:
  fix terugdraaien ⇒ getuige én golden rood in beide takken ⇒ fix
  terug, `cmp` op het bronbestand.

May touch: `backends/drools/**`, `corpus/**`, `scripts/check-drools.sh`
(**voorstel**), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

## Result (2026-09-23)

**Gedragen, niet geweigerd. De golden is NIET geleverd:** de ASP-backend
vertaalt drie van de vier vormen niet, en die backend viel buiten de
may-touch-set. De vorm staat daarom in de JUnit-suite en niet in het
corpus. Commit `5bc3a7ac`.

### (a) Probe op main db224b04, vóór de fix

Fixture `backends/drools/src/test/resources/leeg-negatie/{ir,case}.json`
(hand-geschreven IR, geen `bron.rgs`: RegelSpraak heeft geen "is niet
leeg", de frontend schrijft `negatie` op LEEG/GEVULD nooit). Twee Werven
(W1 tarief 312, W2 zonder), vier Schuiten. Drie regels:
meldpremie := 40 als lengte niet leeg is, kenmerk ongemeten als lengte
niet gevuld is, werfpremie := 25 als het tarief van de werf niet leeg is
(één gebonden stap, navigatieRol `werf`). Per regel apart gemeten,
positieve tweeling als controle:

| variant | verifier | ref | clingo | exec | DRL-tekst |
|---|---|---|---|---|---|
| eigen LEEG¬ | PASS | PASS (5) | TRANSLATION FAULT | FAIL | FAIL |
| eigen LEEG | PASS | PASS (5) | TRANSLATION FAULT | PLAYED 1 | PLAYED 1 |
| eigen GEVULD¬ | PASS | PASS (5) | TRANSLATION FAULT | FAIL | FAIL |
| eigen GEVULD | PASS | PASS (5) | PASS (5) | PLAYED 1 | PLAYED 1 |
| genavigeerd LEEG¬ (gebonden) | PASS | PASS (5) | TRANSLATION FAULT | FAIL | FAIL |
| genavigeerd LEEG | PASS | PASS (5) | TRANSLATION FAULT | PLAYED 1 | PLAYED 1 |
| genavigeerd LEEG¬ (ongebonden) | PASS | PASS (5) | TRANSLATION FAULT | FAIL | FAIL |

Letterlijk, beide takken gelijk:
- eigen LEEG¬: `FAIL case-own-leeg-neg.json: attribuut meldpremie: verwacht 40, werkelijk leeg; verwacht leeg, werkelijk 40; …`
- eigen GEVULD¬: `FAIL case-own-gevuld-neg.json: kenmerk ongemeten: verwacht afwezig, werkelijk aanwezig; kenmerk ongemeten: verwacht aanwezig, werkelijk afwezig; …`
- genavigeerd LEEG¬, gebonden en ongebonden: `FAIL …: attribuut werfpremie: verwacht 25, werkelijk leeg; …; verwacht leeg, werkelijk 25; …`
- clingo: `TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)`.

Het volledige document: verifier PASS, `oracle: PASS (13 expectations)`,
beide Drools-takken FAIL op alle twaalf waarderijen (elke rij
omgekeerd). De kaarttekst wordt bevestigd en uitgebreid: ook GEVULD¬
valt in dezelfde klasse, en de probe reproduceert ook na P1–P3, gebonden
én ongebonden. Nog een geval: het genegeerde LEEG onder een
geheel-periode-kwalificatie (lengte MAAND-granulair, tijdlijninvoer)
speelde in beide takken en gaf `verwacht 40, werkelijk leeg; verwacht
leeg, werkelijk 40`. ref weigert elke geheel-periode-leeg/gevuld-test
(`whole-period leeg/gevuld test is ch. 3.9 scope, not executed here`).

**Norm.** IR-SPEC ch. 4, rij Predicate: `negated` is een veld van elke
soort; de leeg/gevuld-tests zijn kernel, driewaardige regels in
RUNTIME-SEMANTICS ch. 1.2 ("absence … **is** leeg = waar"). ref
(`ref/lib/eval.ml`, arm `Predicaat { soort = Leeg; … }`) leest de negatie
als `not is_empty`, en voor GEVULD als `not (v <> Empty)`. De spec zegt
nergens expliciet wat een genegeerde leeg/gevuld-test betekent.
`Predicaat.negatie` is de BINNENSTE, puntsgewijze negatie. ch. 3.9's
"Negation complements the folded result" gaat over de eigen negatie van
de kwalificatie (`GeheelPeriode.niet`, "niet gedurende …"), niet over dit
veld; RUNTIME-SEMANTICS ch. 3.9 onderscheidt beide in de worked examples
en ref zegt het bij `whole_period_kenmerk`. *(Fixronde 2026-09-23: de
eerste versie van dit dossier koppelde die zin ten onrechte aan
`Predicaat.negatie`.)* Wat onder de kwalificatie open blijft is de
DREMPEL: ch. 3.9 geeft de leeg-test touched-days en elke andere test
days(B), en welke een genegeerde leeg-test krijgt staat nergens. Onder
days(B) zou de wissel naar gevuld exact zijn, onder touched-days niet.

### (b) Route: dragen

Mechanisch: niet-leeg is puntsgewijs precies gevuld, en omgekeerd. Beide
takken lezen de soort op een tiental plekken (eigen attribuut,
genavigeerd, kwantorvouw-toets, assertie, geheel-periode) en nergens de
negatie. Daarom is de lowering één keer in `IrLoader.predicaat`
geplaatst, op de draadgrens: bij `negatie: true` wordt LEEG GEVULD
(`negatie: false`) en GEVULD LEEG. Dat dekt alle leesplekken in beide
takken tegelijk. Met `periode ≠ null` hangt de betekenis af van een
drempelkeuze die de norm niet maakt (en ref faalt er zelf op), dus daar
staat een benoemde weigering: `Refusal("Predicaat.negatie", "a negated
leeg/gevuld test under a whole-period qualifier (ch. 3.9): the threshold
of the negated test is not normed")`. De getuige pint de sleutel én de
reden.

Getuige `LeegNegatieWitnessTest` (vier tests, beide takken via
`BeideTakken`/`CaseRunner.speelCase`): het hele document speelt; dat
geldt ook zonder `navigatieBindingen`; de ongenegeerde tweeling speelt
met elke verwachting gespiegeld (controle tegen een lowering die de
soort altijd omdraait); de geheel-periodevorm weigert in beide takken.
Rood eerst (vóór de fix): `Tests run: 4, Failures: 3` en de controle was
groen. De drie falende tests gaven onder meer `exec-model — niet leeg /
niet gevuld … expected: <null> but was: <case: attribuut meldpremie:
verwacht 40, werkelijk leeg; kenmerk ongemeten: verwacht afwezig,
werkelijk aanwezig; …>` en `Expected …IrLoader.Refusal to be thrown, but
nothing was thrown`. Na de fix: `Tests run: 4, Failures: 0`. Na de fix
speelt het fixture-document `PLAYED 1` in beide takken, net als alle
zeven varianten. De periodevariant geeft in beide `EXCLUDE loader: form
not carried by the v0 Drools backend: Predicaat.negatie (…)`.

**Golden: niet geleverd.** Gate 6 kent geen uitsluitingslijst. Een
golden met LEEG, LEEG¬ of GEVULD¬ in een regelconditie maakt gate 6 rood,
want clingo vertaalt in `backends/clingo/lib/translate.ml` in een
regelconditie alleen `Predicaat { soort = Gevuld; negatie = false; … }`.
Precedent is `DatumTijdLexeemWitnessTest`: de getuige draagt de vorm tot
de square hem draagt, daarna verhuist het fixture naar `corpus/golden`.
De verwachtingen zijn met de hand berekend in de javadoc van de getuige,
en ref bevestigt ze (13/13). Omdat er geen nieuwe golden is, bewegen de
gate-12-pins niet: `EXPECTED_PLAYED=180` en `EXPECTED_DRL_PLAYED=157`
blijven staan en `scripts/check-drools.sh` is niet aangeraakt. **Geen
beschermd-pad-voorstel.**

Negatief-/ref-getuige: niet toegevoegd. De verifier laat alle varianten
toe, ook negatie + periode, en geen WF-regel verbiedt ze. ref draagt de
vorm al. De spec draagt dus geen regel waar een negatief bij hoort.

### (c) Corpussweep

Alle 271 golden+rood-documenten met case-bestanden, door beide takken,
jar vóór (db224b04) en jar ná de fix: 542 regels, `cmp` identiek. Vóór
en na: exec 200 PLAYED / 70 EXCLUDE / 1 FAIL, DRL-tekst 172 PLAYED / 98
EXCLUDE / 1 FAIL. Rood zit in de sweep, dus die getallen zijn groter dan
de gate-12-tellingen. Geen document wisselt van kanaal of waarde.

### (d) Gates (elk los, na `eval $(opam env)`)

1 self-contained PASS · 2 ir-schema PASS (273 bestanden, vloer 192) ·
3 verifier PASS (273 golden schoon, 431 negatief, vloer 350) ·
4 case-schema PASS (629) · 5 oracle PASS (ref-suite 775, 588 cases) ·
6 clingo PASS (suite 479, 588 cases) · 7 noop PASS (552; 36 survivors) ·
8 rood PASS (41) · 9 catala PASS (201 checks; 12 goldens) ·
10 regelspraak PASS (392 tests; 70 goldens) · 11 toka-parity PASS
(29/29/29) · 12 drools PASS (played 180, pin 180; DRL-tekst 157, pin
157; JUnit 577, vloer 239) · 13 reftype-derivation PASS (131 twists, 128
replays) · 14 flogic PASS (130 checks; 2 goldens).

Mutatieproef, in één keten uitgevoerd: `IrLoader.java` teruggezet op
HEAD~ en de getuige gedraaid gaf `Tests run: 4, Failures: 3, Errors: 0`
(dezelfde drie als rood-eerst). Daarna de fix teruggezet, met `cmp`
tegen de bewaarde kopie: identiek. Opnieuw gedraaid: `Tests run: 4,
Failures: 0`. `BeideTakken.groen` stopt na de eerste tak die faalt. Dat
de DRL-tekst-tak ook rood was, blijkt daarom uit de CLI-meting onder (a)
met de jar van vóór de fix (`--drl`: FAIL op elke genegeerde variant).

### Voorstellen voor de hoofdsessie

- **Registerrij**, `spec/CONSTRUCTS.md`, bij de rij `— Predicaatsoort |
  LEEG, GEVULD | §5.13/.14 | core | 18` (regel 113), aan te vullen met:
  "**Negation carried (2026-09-23, card drools-leeg-negatie):** a
  `Predicaat` of soort LEEG or GEVULD with `negatie: true` was answered
  UNNEGATED by both Drools branches — every site read the soort and none
  the field, since the RegelSpraak frontend never writes it on these two
  soorten — a silent inversion on a verifier-clean document, own and
  navigated operand alike. `negatie` is the INNER, per-day negation
  (the qualifier's own is `GeheelPeriode.niet`). The backend's loader
  now lowers it to the sister soort unnegated (pointwise, not-leeg is
  gevuld). Under a whole-period qualifier it refuses by name
  (`Predicaat.negatie`): ch. 3.9 gives the leeg test the touched-days
  threshold and every other test days(B), and which of the two a negated
  leeg test takes is not normed (under days(B) the swap would be exact);
  ref faults on every whole-period leeg/gevuld test. Witness
  `LeegNegatieWitnessTest` in the backend suite; no corpus golden yet,
  because the ASP backend's shared condition translation (`cond_atoms`
  — rule condition, fold filter, quantifier-fold matrix, assertion)
  carries only the unnegated gevuld test and faults on LEEG, LEEG¬ and
  GEVULD¬; it reads the negated forms only inside day-kind definitions
  (`dk_cond`)."
- **Speczin** (RUNTIME-SEMANTICS ch. 1.2, rij leeg/gevuld, voorstel): een
  zin plus worked example "the predicate's `negated` field is the inner,
  per-value negation and complements the test: `leeg(5)` negated =
  waar, `leeg(leeg)` negated = onwaar, so negated leeg is gevuld and
  negated gevuld is leeg. Under the ch. 3.9 whole-period qualifier this
  inner negation is distinct from the qualifier's own `niet`, and its
  meaning there is an OPEN point of the norm: the qualifier gives the
  leeg test the touched-days threshold and every other test days(B), and
  which a negated leeg test takes is not chosen — `ref` and both Drools
  branches refuse the form by name until it is."
- **Vervolgkaart** (clingo-leeg-negatie; door de hoofdsessie aangemaakt als
  `backlog/new/20260923-clingo-leeg-negatie.md`): de ASP-backend geeft een
  regelconditie-arm voor LEEG (helper plus `not`) en voor de genegeerde
  vormen. Daarna verhuist het fixture naar
  `corpus/golden/jutterswet-waardemelding` (NOTE met de
  handberekening). Gate 12 beweegt dan +1 exec / +1 DRL-tekst, met een
  redenregel.

Open: de golden (hierboven). Verder niets.
