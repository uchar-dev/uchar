# drools-heeltijdvak-leegtest: Drools speelt waar ref en clingo weigeren

Gemeten door de leeg-negatie-lens (2026-09-23, eigen proef-IR's): een
ONgenegeerde leeg/gevuld-test onder een heel-tijdvak-kwalificatie
speelt in beide Drools-takken, terwijl ref er een fout geeft
("whole-period leeg/gevuld test is ch. 3.9 scope, not executed here")
en clingo ook weigert. Drools geeft dus een antwoord op een vorm
waarvan de norm (de ch.-3.9-drempel voor een leeg-test) niet is
vastgelegd — de divergentieklasse: niet stil fout tegenover een
uitvoerder die speelt, maar wel een antwoord waar de andere twee
uitvoerders benoemd weigeren. Geen corpusdrager. De GEnegeerde variant
weigert de loader sinds de drools-leeg-negatie-kaart benoemd; de
ongenegeerde bleef buiten die kaart.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer met een proef-IR; meet alle vier de uitvoerders.
- (b) Beide Drools-takken volgen ref: benoemde weigering van de
  heel-tijdvak-leeg/gevuld-test (zelfde plek als de negatieweigering,
  IrLoader.predicaat, ligt voor de hand) — óf de norm wordt eerst
  vastgelegd en alle uitvoerders dragen haar; weigeren is de kleinste
  stap. Rood-eerst getuige beide takken.
- (c) Corpussweep: geen bestaand document beweegt; alle veertien gates
  groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

- (a) Proef-IR's uit `corpus/rood/leegnegatie-heel-tijdvak`: LEEG en
  GEVULD × `negatie` × `GeheelPeriode.niet` (acht varianten), elk
  gemeten in verifier, ref, clingo, exec-tak en DRL-tekst-tak; de
  letterlijke uitvoer in het Result. Dragers in corpus/ en toka/
  hertellen.
- (b) Eén loader-weigering in `IrLoader.predicaat` voor de ONgenegeerde
  vorm, met een eigen sleutel en een eigen zin (grond: ref voert de
  ch.-3.9-vouw voor leeg/gevuld niet uit; geen bodemzin). De genegeerde
  vorm houdt haar bestaande weigering (`Predicaat.negatie`), zodat de
  check-rood-regel van `leegnegatie-heel-tijdvak` waar blijft. Rood-eerst
  een getuige voor beide takken in `LeegNegatieWitnessTest` (nieuwe
  test; bestaande tests onaangeroerd). Mutatieproef met `cmp`.
- (c) Sweep beide takken over alle golden+rood-documenten vóór/na:
  `cmp` identiek. Alle veertien gates groen, elk los; pins 181/160
  bewegen niet.

## Result (2026-09-23)

**Geweigerd, benoemd, in beide takken.** Commits `38bd97f0` (loader +
getuige). Geen beschermd pad geraakt: de check-rood-regel van
`leegnegatie-heel-tijdvak` blijft letterlijk waar, dus **geen voorstel**.

### (a) Meting op main 8b685402, vóór de weigering

Acht proef-IR's uit `corpus/rood/leegnegatie-heel-tijdvak` (lengte
MAAND, tijdlijninvoer S1 = 8 over 2025, S2 zonder lengte): soort
LEEG/GEVULD × `negatie` × `GeheelPeriode.niet`. De case draagt de met de
hand berekende ch.-3.9-waarden (LEEG: S1 onwaar, S2 vacuüm waar; GEVULD:
beide waar; `niet` keert om). Alle acht `verifier: PASS`.

| variant | ref | clingo | exec | DRL-tekst |
|---|---|---|---|---|
| LEEG, GEVULD ongenegeerd, niet=false/true (4) | EVALUATION FAULT | TRANSLATION FAULT | PLAYED 1 | PLAYED 1 |
| LEEG¬, GEVULD¬, niet=false/true (4) | EVALUATION FAULT | TRANSLATION FAULT | EXCLUDE `Predicaat.negatie` | idem |

ref en clingo letterlijk, in alle acht: `whole-period leeg/gevuld test
is ch. 3.9 scope, not executed here`. De kaarttekst klopt en geldt ook
voor GEVULD en voor de `niet gedurende`-vorm. Terzijde: de vier
ongenegeerde varianten speelden tegen de handwaarden van ch. 3.9. Drools
lijkt de vouw dus te dragen. Controleren kan niemand, want ref voert hem
niet uit. Dragers hergeteld over 718 `ir.json` in corpus/ en 30 in
toka/: alleen `corpus/rood/leegnegatie-heel-tijdvak` (LEEG, negatie
true). Geen enkel document draagt de ongenegeerde vorm.

De exec-crash uit 20260923-ref-klipvouw-nul (ClassCastException) kwam in
geen enkele proef voor: geen ERROR-regel.

### (b) De weigering

`IrLoader.predicaat`: vóór de negatietak een eigen weigering voor
LEEG/GEVULD met `periode ≠ null` en `negatie = false`. Letterlijk:

> `Refusal("Predicaat.periode", "a leeg/gevuld test under a whole-period
> qualifier (ch. 3.9): ref executes neither the leeg fold (touched days)
> nor the gevuld fold (days(B)), so this backend does not answer the form
> either")`

De genegeerde vorm houdt `Predicaat.negatie` met de drempelzin. Die
grond is een andere: daar is de norm open, hier is hij er wel, maar voert
ref hem niet uit. Na de fix geven de vier ongenegeerde proeven in beide
takken `EXCLUDE loader: form not carried by the v0 Drools backend:
Predicaat.periode (…)`. De vier genegeerde geven ongewijzigd
`Predicaat.negatie (…)`.

Getuige: een nieuwe test in `LeegNegatieWitnessTest`,
`de_ongenegeerde_test_onder_geheel_periode_weigert_in_beide_takken`:
LEEG en GEVULD × niet=false/true, beide takken, pint sleutel en zin. De
bestaande vier tests zijn onaangeroerd; alleen de klassejavadoc noemt nu
twee weigeringen. Rood eerst:
```
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
org.opentest4j.AssertionFailedError: exec-model refuses whole-period LEEG ==> Expected io.github.yhavinga.regelspraak.loader.IrLoader.Refusal to be thrown, but nothing was thrown.
```
Na de fix: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`.

Mutatieproef in één keten (IrLoader op HEAD gezet, getuige, fix terug,
`cmp`, getuige):
```
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
org.opentest4j.AssertionFailedError: exec-model refuses whole-period LEEG ==> Expected … Refusal to be thrown, but nothing was thrown.
cmp: identiek
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```
De getuige stopt bij de eerste falende assertie (exec, LEEG). Dat ook de
DRL-tekst-tak en GEVULD rood waren, blijkt uit de CLI-meting onder (a).

### (c) Sweep en gates

Beide takken over alle 276 golden+rood-documenten met case-bestanden,
jar vóór (8b685402) en jar ná: 552 regels, `cmp` identiek. exec 203
PLAYED / 72 EXCLUDE / 1 FAIL, DRL-tekst 177 / 98 / 1. De FAIL is de
bestaande `anchor/jutterswet-kustwacht`, in beide jars gelijk.

Elk los na `eval $(opam env)`: 1 self-contained PASS · 2 ir-schema PASS
(278, vloer 192) · 3 verifier PASS (278 schoon; 440 negatief) ·
4 case-schema PASS (638) · 5 oracle PASS (ref-suite 788; 595 cases) ·
6 clingo PASS (suite 518; 595 cases) · 7 noop PASS (559; 36 survivors) ·
8 rood PASS (43; Drools-leg 9 gepind / 34 met reden; 86 fragmenten) ·
9 catala PASS (201; 12 goldens) · 10 regelspraak PASS (404 tests; 70
goldens) · 11 toka-parity PASS (29/29/29) · 12 drools PASS (played 181
= pin; DRL-tekst 160 = pin; JUnit 605, +1) · 13 reftype-derivation PASS
(131 twists, 128 replays, 25 DRL) · 14 flogic PASS (130; 2 goldens).

### Voorstel registerrij (hoofdsessie)

`spec/CONSTRUCTS.md`, rij `— Predicaatsoort`: vervang de laatste zin
"Red twin `corpus/rood/leegnegatie-heel-tijdvak` pins that refusal in all
three executors." door:

"Red twin `corpus/rood/leegnegatie-heel-tijdvak` pins that refusal in
all three executors. Since card `drools-heeltijdvak-leegtest`
(2026-09-23) the Drools loader also refuses the UNNEGATED whole-period
leeg/gevuld test by name (`Predicaat.periode`: ref executes neither the
leeg fold nor the gevuld fold), where both branches used to play it.
The form is normed in ch. 3.9 but executed by no executor, and no corpus
document carries it."

Open: niets binnen de kaart. Een rode tweeling voor de ongenegeerde vorm
(zelfde document, `negatie: false`) zou de weigering in alle drie de
uitvoerders blijvend bewaken. Buiten may-touch, dus alleen een voorstel.
Het vergt ook een check-rood-regel.
