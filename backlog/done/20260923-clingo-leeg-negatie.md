# clingo-leeg-negatie: de ASP-vertaling kent de leeg-test en de negaties niet

Gemeten door de drools-leeg-negatie-kaart (2026-09-23):
`backends/clingo/lib/translate.ml` vertaalt in een regelconditie alleen
de ongeneerde GEVULD-vorm; LEEG, LEEG¬ en GEVULD¬ geven elk een
`TRANSLATION FAULT: condition kind not translated (no corpus case
exercises it)`. De Drools-takken dragen alle vier de vormen sinds die
kaart (loader-wissel naar de zustertest; heel-tijdvak benoemd
geweigerd); ref draagt ze al. Zolang clingo de vormen niet kent, kan de
getuige-fixture geen golden worden — gate 6 heeft geen weigerlijst en
zou rood staan.

## Acceptatie (bij oppakken aanscherpen)

- (a) De vertaler krijgt een regelconditie-arm voor LEEG (helper plus
  `not`) en voor de genegeerde vormen (zusterwissel zoals de
  Drools-loader, of rechtstreeks; heel-tijdvak volgt ref of weigert
  benoemd — meet ref eerst). Rood-eerst getuigen in de clingo-suite.
- (b) De fixture uit `backends/drools/src/test/resources/leeg-negatie/`
  verhuist als golden naar `corpus/golden/jutterswet-waardemelding`
  (NOTE met de handberekening uit `LeegNegatieWitnessTest`), zodat
  gates 5/6/12 de vorm blijvend bewaken over het hele vierkant.
- (c) Gate-12-pins bewegen +1 exec / +1 DRL-tekst met redenregel —
  benoemd beschermd-pad-voorstel.
- (d) Corpussweep: geen bestaand document beweegt; alle veertien gates
  groen.

May touch: `backends/clingo/**`, `corpus/golden/**` (de nieuwe golden),
`backends/drools/src/test/**` (de fixture-verhuizing, gemeld),
`scripts/check-drools.sh` (**voorstel**, alleen de pins),
kaartverplaatsing. NB: serieel met andere Drools/clingo-kaarten.

## Result (2026-09-23)

**Gedragen.** Clingo draagt alle vier de vormen in de gedeelde
conditievertaling. De getuige is golden `jutterswet-waardemelding`.
Alle veertien gates groen. Commits `0aeae01e` (arm + suite),
`94e377f5` (golden + getuige), `4b376c00` (gate-12-pins, **voorstel**).

### (a) De arm

Vooraf gemeten op ceb72045: de fixture gaf `TRANSLATION FAULT:
condition kind not translated (no corpus case exercises it)` (ref:
`oracle: PASS (13 expectations)`). Heel-tijdvak: alle vier de vormen
(LEEG/GEVULD × negatie) gaven in clingo al `TRANSLATION FAULT:
whole-period leeg/gevuld test is ch. 3.9 scope, not executed here` en in
ref dezelfde zin als `EVALUATION FAULT`. De benoemde arm stond al vóór
de restarm en ving elke `periode = Some _`. Daar veranderde niets.

`cond_atoms` heeft nu één arm `Predicaat { soort = Leeg | Gevuld;
negatie; dagsoort = None; periode = None }`. Gevuld blijft de
positieve join van de operand, precies de oude code. De test is gevuld
als `(soort = Gevuld) <> negatie` en anders leeg. Leeg is het complement
van die join: een helper `gvN(S) :- isa(S,ot), ⟨join⟩.` en de conditie
`not gvN(S)`. Dat is dezelfde reïficatie als de ALLE-matrix van de
kwantorvouw. De variabelen van de join (ook het doel van een
navigatiestap) blijven lokaal in de helper, en dat is wat "geen binding
bestaat" vraagt. Dit volgt ref exact: leeg is `v = Empty`, gevuld het
complement, `negatie` de puntsgewijze omkering. De vertaling van de
ongenegeerde gevuld-test is ongewijzigd (geen extra `fresh`-tik).

Suite (`test_clingo.ml`, gemeld mandaat), +20 checks: zeven frames
tegen ref én clingo met handberekende rijen (LEEG, LEEG¬, GEVULD
(controle) en GEVULD¬ op het eigen attribuut; LEEG, LEEG¬ en GEVULD¬
over één gebonden stap), twee kwantorvouwen (ALLE over een LEEG-matrix:
dubbele negatie `not kvs(X)` over `not gv(X)`; GEEN over een
LEEG¬-matrix), en twee `faults_with`-pins op de weigering van LEEG¬ en
GEVULD¬ onder heel-tijdvak. De twee weigeringspins waren al vóór de fix
groen. Het zijn staande pins en geen rood-eerst-getuigen.

Rood eerst (vóór de arm):
```
FAIL LEEG, own: clingo pins the values (fault: condition kind not translated (no corpus case exercises it))
FAIL LEEG negated, own: … (idem)
FAIL GEVULD negated, own: … (idem)
FAIL LEEG, navigated: … (idem)
FAIL LEEG negated, navigated: … (idem)
FAIL GEVULD negated, navigated: … (idem)
FAIL ALLE over a LEEG matrix: … (idem)
FAIL GEEN over a negated LEEG matrix: … (idem)
test_clingo: 504 checks, 8 FAILURES
```
Alle ref-checks en de GEVULD-controle waren groen. Na de arm:
`test_clingo: 504 checks, 0 failures` (basis 484).

Mutatieproef. Elke mutatie werd gebouwd en gedraaid en daarna
teruggezet. Na afloop `cmp` tegen de bewaarde kopie: identiek.
- M1, negatie niet gelezen (`<> (negatie && false)`): `504 checks, 5
  FAILURES`, namelijk LEEG negated own/navigated, GEVULD negated
  own/navigated en GEEN over a negated LEEG matrix. Voorbeeld:
  `S1.meldpremie is underived (leeg), expected 40; S2.meldpremie = [40],
  expected leeg; …`.
- M2, leeg als positieve join (`|| true`): `5 FAILURES`, namelijk LEEG
  own/navigated, GEVULD negated own/navigated en ALLE over a LEEG
  matrix.
- M3, helper zonder `not`: dezelfde vijf `FAILURES`.

### (b) De golden

`corpus/golden/jutterswet-waardemelding/{ir,case}.json` is de fixture
byte voor byte (git ziet een rename). `NOTE.md` bevat de handberekening
uit de javadoc van `LeegNegatieWitnessTest`. Die is nagerekend en klopt
rij voor rij met `case.json`: meldpremie 40 op S1 en S3; ongemeten op
S2 en S4; werfpremie 25 op S1 en S2 (S3 zonder werf leest leeg, W2 van
S4 heeft geen tarief). Samen 12 rijen plus `consistent` = 13.
Conformiteit: gate 2 round-trip byte-identiek, gate 3 verifier-schoon,
gate 4 schema-valide, ref en clingo `PASS (13 expectations)`, gate 7
weerlegt de noop (geen survivor).

Drools-testresources: **verwijzen naar de golden**, de kopie is weg.
`LeegNegatieWitnessTest` (gemeld mandaat) kreeg drie wijzigingen, verder
niets: `FIXTURE = Path.of("..", "..", "corpus", "golden",
"jutterswet-waardemelding")` (het patroon van
`DrlEenheidsplekWitnessTest`), de javadoc-alinea "Why the fixture is
here and not in corpus/golden" vervangen door wat nu geldt, en de
bestandsnaam in de handberekening. De vier tests en hun asserties zijn
onaangeroerd. Een kopie was één bestand minder geweest om te wijzigen,
maar had twee documenten laten uiteenlopen, en de alinea was dan onwaar
geworden. `mvn -o test -Dtest=LeegNegatieWitnessTest`: `Tests run: 4,
Failures: 0, Errors: 0, Skipped: 0`.

### (c) Gate-12-pins — BESCHERMD-PAD-VOORSTEL

`scripts/check-drools.sh`, alleen de twee pins met een redenblok in de
huisstijl: `EXPECTED_PLAYED` 180 → 181 en `EXPECTED_DRL_PLAYED` 159 → 160
(de DRL-pin stond op main al op 159, niet 157). Gemeten: `played 181 …
played matches the pinned 181`, `DRL-text branch played 160 … pin 160`.
Commit `4b376c00` staat apart, zodat de maintainer hem los kan nemen.

### (d) Sweep en gates

Clingo-runner vóór (ceb72045) en ná de arm, over alle 636
golden+rood-cases (vóór de golden erbij kwam): `cmp` identiek. Dat zijn
594 `PASS` en 42 verwachte rood-fouten. Geen corpusdocument droeg LEEG,
LEEG¬ of GEVULD¬, dus geen vertaling beweegt. In de Drools-backend
veranderde geen broncode. De naamlijsten van gate 12 bleven groen.

Elk los na `eval $(opam env)`:
1 self-contained PASS · 2 ir-schema PASS (277 bestanden, +1; vloer 192) ·
3 verifier PASS (277 golden schoon, +1; 440 negatief, vloer 350) ·
4 case-schema PASS (637, +1) · 5 oracle PASS (ref-suite 788; 595 cases,
+1; vloer 427) · 6 clingo PASS (suite 504, +20 t.o.v. 484; 595 cases,
+1) · 7 noop PASS (559, +1; 36 survivors) · 8 rood PASS (42; Drools-leg
9/33) · 9 catala PASS (201; 12 goldens) · 10 regelspraak PASS (404
tests; 70 goldens) · 11 toka-parity PASS (29/29/29) · 12 drools PASS
(played 181 = pin; DRL-tekst 160 = pin; JUnit 604, vloer 239) ·
13 reftype-derivation PASS (131 twists, 128 replays, 25 DRL) ·
14 flogic PASS (130 checks; 2 goldens).

### Voorstel registerrij (hoofdsessie) — beperkt in de fixronde

`spec/CONSTRUCTS.md`, rij `— Predicaatsoort`: vervang het slot vanaf
"no corpus golden yet, because …" tot het einde van de cel door:

"golden `jutterswet-waardemelding` (LEEG¬ and GEVULD¬ on an own
attribute, LEEG¬ over one bound navigation step) replays across the
whole square since card `clingo-leeg-negatie` (2026-09-23): the ASP
backend's shared condition translation (`cond_atoms` — rule condition,
fold filter, quantifier-fold matrix, assertion) now carries all four
forms. Gevuld is the operand's positive join. Leeg is its complement,
reified in a helper and read under `not`. `negatie` selects the sister
test pointwise. That matches ref only in a document WITHOUT a recursive
creation group. WF-046 counts no leeg test as a negative edge, so a
cycle WF-045 admits can run through the helper's negation (measured:
UNSAT, and a wrong model chosen among five). The complement route
therefore refuses by name in any document with a recursive creation
group — a conservative, document-wide first cut (maintainer decision
2026-09-23). The root is the norm card
`20260923-verifier-leegtest-negafhankelijkheid`, and the same gap in the
ALLE-matrix reification is `20260923-clingo-alle-matrix-stratificatie`.
An unbound scalar parameter in the operand faults in ref's words. Under
a whole-period qualifier the ASP backend refuses every leeg/gevuld test
in ref's own words, as before. Red twin
`corpus/rood/leegnegatie-heel-tijdvak` pins that refusal in all three
executors."

Open: niets binnen de kaart. De speczin voor RUNTIME-SEMANTICS ch. 1.2
uit drools-leeg-negatie (drempel onder heel-tijdvak open) blijft een
normpunt van de maintainer, en deze kaart heeft hem niet aangeraakt.

## Fixronde (2026-09-23, na de lens)

Commits `a7f6d056` (arm + suite), `77a2768f` (rood + NOTE),
`31433903` (gate-8-uitsluitingsregel en gate-12-redenregel,
**voorstel**, beschermd pad).

**B1: recursieve creatiegroep.** Gemeten op de branchtip 2bebdce2 met de
lensproeven. R1 (LEEG in de conditie van een recursieve creatie) gaf
`clingo: UNSATISFIABLE — a fail-fast guard fired: no viol/2 witness (the
input is not stratified?)`. R3 (GEVULD¬ in de creatie, LEEG in de regel)
gaf `MISMATCH: W1.merk is underived (leeg), expected 1`. ref: `oracle:
PASS (2 expectations)` op beide. De R2-controle (GEVULD¬ in de creatie)
was ook UNSAT, want ook die vorm neemt de helperroute. Detectie: eerste
snede documentbreed, met het bestaande predicaat
`has_recursive_creation` (een recursieve regelgroep met een
objectcreatie als lid). "Bereikt de operand" is nog niet verfijnd, en
dat is zo benoemd in het commentaar en in de weigerzin. Kosten in het
corpus: nul, want geen document draagt zowel een recursieve
creatiegroep als een helpervorm. De weigerzin, letterlijk, alleen op de
complementroute (LEEG en GEVULD¬):

> a leeg test (or a negated gevuld test) is not translated in a document
> with a recursive creation group: this backend reads it as the negation
> of a helper, and WF-046 counts no leeg test as a negative edge, so a
> creation cycle through it need not be stratified. Declined in
> translation, conservatively for the whole document

Het commentaar "Stratified by construction" is vervangen door wat
werkelijk geldt, met het maintainerbesluit en de twee vervolgkaarten.

**B2: ongebonden parameter.** Probe E_param op de tip: clingo `PASS (3
expectations)` tegen ref `EVALUATION FAULT: parameter "drempel" is
unbound in this case (binding is fail-fast, ch. 5.7)`. De arm loopt nu
vóór beide routes over de operand (`unbound_param_in`: ook genestelde
condities, vouwfuncties en de body van een aangeroepen functie; een
lijstparameter is vrijgesteld, want ref leest zijn afwezigheid als leeg)
en weigert in refs zin. Ook de ongenegeerde GEVULD krijgt die weigering,
omdat de arm gedeeld is. E2 (ref faalt) faalt nu in beide. Het bredere
gat, een gewone waardelezing van een ongebonden parameter (probe E3:
clingo PASS, ref faalt), blijft staan voor
`20260923-clingo-parameter-failfast`.

**Suite** 504 → 518 (+14): assertie LEEG¬ en LEEG (A1-G), vouwfilter
plus telmatrix (A1-H, de H-proef), LEEG en LEEG¬ op een gebonden
parameter, LEEG en LEEG¬ op een ongebonden parameter (B2), R1 en R3
(B1). Rood eerst, met de translator van de tip teruggezet en de nieuwe
suite gedraaid:
```
FAIL LEEG on an unbound parameter: expected a loud fault, got a model
FAIL LEEG negated on an unbound parameter: expected a loud fault, got a model
FAIL R1: LEEG in a recursive creation's condition: expected a loud fault, got a model
FAIL R3: GEVULD negated in a recursive creation, LEEG in the rule: expected a loud fault, got a model
test_clingo: 518 checks, 4 FAILURES
```
Na de fix: `518 checks, 0 failures`. De G-, H- en gebonden-parameterpins
waren op de tip al groen. Het zijn staande pins.

**A2: isa-getuige.** De lensmutant L2 (helper zonder `isa(S,ot)`) geeft
nu `FAIL LEEG on a bound parameter: clingo pins the values (clingo
exited 65)`, `518 checks, 1 FAILURES`. De atomen van een parameter binden
S niet. Opnieuw gedraaid op de nieuwe suite: M1 8, M2 10 en M3 8
FAILURES. Na elke mutatie `cmp` identiek.

**A3: rood.** `corpus/rood/leegnegatie-heel-tijdvak`: de eerste regel
van de golden, lengte MAAND, GeheelPeriode (jaar, niet=false), invoer
als tijdlijn. ref en clingo: `expected fault observed (whole-period
leeg/gevuld test is ch. 3.9 scope, not executed here)`. De zin komt uit
een benoemde arm in beide executors en staat niet in de lijst van 36
bodemzinnen (gate 8 bevestigt: 86 fragmenten, geen HIT). Drools:
`EXCLUDE loader: form not carried by the v0 Drools backend:
Predicaat.negatie (…the threshold of the negated test is not normed)`.
Daarom een regel in de Drools-uitsluitingslijst van `check-rood.sh`
(**voorstel**). Zonder die regel is gate 8 rood: `FAIL:
leegnegatie-heel-tijdvak carries no drools fragment and is not in the
Drools exclusion list`.

**A4.** Het redenblok van gate 12 zegt nu dat geen bronbestand van de
backend veranderde en dat de getuige nu de golden leest.

**Gates** (elk los, na `eval $(opam env)`): clingo-suite 518/0 ·
3 verifier PASS (278 schoon, +1 rood; 440 negatief) · 6 clingo PASS
(suite 518; 595 cases) · 8 rood PASS (43 rood, +1; 86 fragmenten;
Drools-leg 9 gepind / 34 met reden). Extra: 1, 2 (278), 4 (638), 5 (595)
en 7 (559) PASS. Gate 12 is niet opnieuw gedraaid: `backends/drools` is
in deze ronde niet geraakt. De clingo-runnersweep over de 636 bestaande
golden- en rood-cases is `cmp`-identiek aan die van vóór de fixronde.
