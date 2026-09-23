# drools-consistentie-label: de runner keurt inconsistent zonder labelvergelijking

Gemeten door de leeg-negatie-lens (2026-09-23): bij een casus die
`inconsistent` verwacht met een REGELNAAM als sleutel vergelijkt
`CaseRunner.consistentie` in de Drools-backend het label niet — elke
gemelde inconsistentie telt als de verwachte. Een casus kan daardoor
groen lijken op de verkeerde inconsistentie (de lens betrapte het:
een onderscheidende consistentieproef speelde met én zonder de
leeg-negatie-fix). Verboden klasse: een verwachting die niet werkelijk
wordt getoetst. Meet bij oppakken hoe ref de sleutel vergelijkt en of
corpuscasussen met regelnaam-sleutels bestaan die vandaag op deze
zwakte leunen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: een getuige met twee consistentieregels waarvan de
  verkeerde vuurt, die vandaag ten onrechte groen speelt.
- (b) De runner vergelijkt het label zoals ref dat doet; een casus met
  een niet-passend label faalt benoemd.
- (c) Corpussweep beide takken: geen kanaal- of waardewissel; alle
  veertien gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Aangescherpt bij de claim (2026-09-23)

- (a0) Meet eerst hoe ref sleutel en label vergelijkt
  (`ref/lib/case.ml`) en inventariseer de corpuscasussen met een
  regelnaam-sleutel plus label. Leunt er een op de zwakte (faalt hij
  onder de strikte vergelijking), dan is dat een corpusvondst: benoemen
  en stoppen, maintainerbesluit.
- (a) Rood-eerst in de JUnit-suite: een getuige met twee
  consistentieregels waarvan de verkeerde (op een ander onderwerp)
  vuurt, eerst groen-ten-onrechte in beide takken op de basis gezien.
- (b) Label vergeleken voor elke sleutel zoals ref (subject-label
  exact); de mismatch noemt het gepinde label en wat het kanaal draagt.
- (c) Sweep beide takken voor/na cmp-identiek; veertien gates groen,
  elk los aangeroepen; pins 181/160 onbewogen. Mutatieproef: vergelijking
  weer uit ⇒ getuige groen-ten-onrechte ⇒ herstel.

## Result (2026-09-23)

Af. De runner toetst het label van elke `inconsistent`-pin zoals ref,
en beide Drools-takken leggen de §9.5-verdelingsfout nu op de
ontvanger. Eerst liep de kaart vast op een corpusvondst
(rangmaximering). Die is per maintainerbesluit in deze kaart opgelost.

**Maintainerbesluit (verbatim).** "De §9.5-verdelingsfout (leeg
soortgelijk gewicht; leeg of negatief maximum) landt in beide
Drools-takken voortaan op elke ONTVANGER (rule × subject, ch. 5.6),
zoals ref, clingo en RUNTIME-SEMANTICS ch. 4.2/4.3 al doen — de
bronobject-plaatsing was de afwijking die de labelzwakte maskeerde.
Geen uitzondering in de vergelijking, geen pinbeweging: rangmaximering
speelt daarna met het juiste label (W5)."

**ref-meting, vergelijking.** `ref/lib/case.ml`, arm `Inconsistent`:
een kanaalfeit past als de regelnaam past (beide kanten `norm` +
`strip_version`) EN, bij een label-pin, `String.equal f.subject l`
(exact). Een label-pin zonder regel-pin en een onbekend label weigert
ref al bij het laden. Drools vergeleek de regel exact en het label
alleen bij een defaults-sleutel (`Onderwerp.doel[/pad]`).

**ref-meting, plaatsing.** `ref/lib/eval.ml` `run_verdeling` +
`ref/lib/alloc.ml`: de ontvangers zijn de doeltype-instanties die de
verbindende rol bereikt. `Alloc.allocate` roept `err ~id:r.id` per
ontvanger aan en `World.insert_error ~rule:name ~subject:id` legt het
feit op die ontvanger. Wanneer precies:
- leeg gewicht: naar rato, elke ontvanger zonder gewicht; op volgorde
  met rato-gewicht, elke ontvanger met een leeg tiegewicht;
- leeg of negatief maximum (`cap_floor`): gelijke delen en op volgorde,
  elke ontvanger; naar rato alleen voor gewogen ontvangers en alleen
  als de gewichtsom ≠ 0 (anders `Nothing`, geen maximum gelezen);
- lege pot: op volgorde `Nothing` (geen fout), gelijke delen en naar
  rato een fault in ref, dus nooit een kanaalfeit;
- zonder ontvangers of met een onwaar voorwaarde geen fout.
Beide takken volgen dat nu: bron-patroon `bronAttr != null`
(+ voorwaarde, geldigheid), `$subject`/`$d` is de ontvanger via de
verdelingsjoin (`FactModelGenerator.verdelingsjoin`, al hergebruikt),
de naar-rato-maximumregel draagt `gewicht != null` plus een
accumulate-som ≠ 0, en de idempotentie-guard keyt op regel × ontvanger.
`DrlTranspiler` "(leeg gewicht)"/"(leeg of negatief maximum)" en
`ExecModelTranspiler.existsFoutRegel` → `ontvangerFoutRegel`.

**Corpusinventaris.** 62 `inconsistent`-verwachtingen in
`corpus/golden`, op één na allemaal met regel plus label (wachtersketen
case1 pint alleen de regel). Eén leunde op de zwakte:
`jutterswet-rangmaximering` case2-nulgroep (`verdeel het penningbudget
van een penningkas @ W5`).

**Sweep beide takken** (233 goldens met casussen, per golden de
exec- en `--drl`-regel):
- basis db390c28: exec PLAYED 181, drl PLAYED 160; rangmaximering
  `PLAYED 2 | PLAYED 2` (vals groen, op de sleutel alleen);
- na de labelfix (1ecef001): precies één regel anders, beide takken
  `FAIL case2-nulgroep.json: verwacht inconsistent (verdeel het
  penningbudget van een penningkas @ W5), niet gevonden onder verdeel
  het penningbudget van een penningkas @ K1`;
- na de plaatsingsfix: `PLAYED 2 | PLAYED 2` (echt groen), de hele
  sweep `cmp`-identiek aan de basis. Alleen de rangmaximering-regel
  bewoog, en die staat weer op de basiswaarde. De andere 61
  verwachtingen bewogen niet. Pins 181/160 onbewogen.

**Rood-eerst, label** (`ConsistentieLabelWitnessTest`, fixture
`jutterswet-keurspelling`, V1 wrakhout/wrakhout, V2 gaaf/beschadigd,
labels verwisseld). Op de basis: exec `PLAYED 1`, drl `PLAYED 1`;
JUnit: `exec-model — the rule fires, but on V1, not on the pinned V2
==> expected: not <null>`. ref: `MISMATCH: expected inconsistent (a
channel fact from rule "geen wrakhout ter keuring" on "V2"), but the
channel carries: geen wrakhout ter keuring × V1; herkeuring volgt de
staat × V2`. Na de fix beide takken: `FAIL …: verwacht inconsistent
(geen wrakhout ter keuring @ V2), niet gevonden onder geen wrakhout ter
keuring @ V1, herkeuring volgt de staat @ V2; verwacht inconsistent
(herkeuring volgt de staat @ V1), …`. De lensmutatie wachtersketen
W3→W2 faalt nu in beide takken: `verwacht inconsistent (rangdetector @
W2), niet gevonden onder rangdetector @ W3, rangdetector @ null`.
Mutatieproef: vergelijking uit (`if (true) return null;`) ⇒ getuige
weer groen-ten-onrechte (`Tests run: 2, Failures: 1 … expected: not
<null>`) ⇒ herstel, `cmp` identiek.

**Rood-eerst, plaatsing** (`VerdelingsfoutOntvangerWitnessTest`, vier
rijen: rangmaximering case2 W5 plus de K1-pin rood; berging met H
zonder gewicht en D maximum −5 ⇒ feit op H en D, niet op W of B1;
naar rato met gewichtsom 0 en leeg maximum ⇒ consistent; op volgorde
met lege pot ⇒ consistent). Elke rij eerst door ref en clingo gehaald:
dezelfde uitslag. Met de plaatsing terug op de bron (mutant = HEAD
1ecef001 van beide transpilers): `Tests run: 4, Failures: 4`, o.a.
`het_leeg_gewicht_landt_op_de_ontvanger:42 exec-model — … but was:
<case: verwacht inconsistent (verdeel het penningbudget van een
penningkas @ W5), niet gevonden onder verdeel het penningbudget van een
penningkas @ K1>` en `een_lege_pot_meldt_niets:80 … werkelijk
inconsistent (verdeel het penningbudget van een penningkas @ K1)`.
Herstel, `cmp` identiek. Gerichte mutanten: DRL zonder pot-voorwaarde
⇒ `een_lege_pot_meldt_niets:80 drl-tekst — … werkelijk inconsistent
(verdeel het penningbudget van een penningkas @ W5)`; exec zonder
gewichtsom-poort ⇒ `een_nulgewichtsom_leest_geen_maximum:72 exec-model
— … werkelijk inconsistent (verdeel de opbrengst van een berging @
W)`. Beide hersteld, `cmp` identiek.

**Onbekend label.** Een pin op een label zonder instantie faalt nu
benoemd in beide takken (rij `een_onbekend_label_faalt_in_beide_takken`,
toegevoegd aan de labelgetuige; de bestaande rijen zijn ongewijzigd).
ref weigert dezelfde casus al bij het láden (`inconsistent expectation
references unknown label`). Die strengere lijn blijft een normverschil
met de Drools-runner.

**Regelnaam-norm.** De runner vergelijkt de regelnaam byte-exact, ref
na `norm` + `strip_version`. Geen corpuscasus raakt dat vandaag. Het
hoort bij dezelfde familie als kaart 20260923-drools-temporele-naamnorm
en wordt daar opgepakt.

**Gates** (elk los, na `eval $(opam env)`, alle exit 0):
1 `check-self-contained: PASS`;
2 `check-ir-schema: PASS (schema valid; 24 pattern self-test row(s);
278 file(s) validate and round-trip, floor 192)`;
3 `check-verifier: PASS (278 golden clean; 440 negatief rejected for
their declared rule, floor 350)`;
4 `check-case-schema: PASS (schema valid; 638 case file(s) validate)`;
5 `check-oracle: PASS (ref suite 788 worked examples, floor 489; 595
cases replayed exactly, floor 427; …)`;
6 `check-clingo: PASS (backend suite 518 checks, floor 83; 595 cases
replay exactly through clingo …)`;
7 `check-noop: PASS (559 cases refute a do-nothing executor; 36 pinned
survivors …)`;
8 `check-rood: PASS (43 red cases fault observably in ref and clingo,
floor 26; 86 pinned fragments cite none of the 36 listed bottom
sentences)`, Drools leg `9 fault-pinned, 34 not-carried`;
9 `check-catala: PASS — frontend suite green (201 checks, floor 201);
12 catala golden(s) …`;
10 `check-regelspraak: PASS — JUnit suite green (404 tests, floor 173);
70 rgs golden(s) …`;
11 `check-toka-parity: PASS` (ref 29 / clingo 29 / Drools 29 PLAYED);
12 `check-drools: PASS`, `played 181`, `DRL-text branch played 160`,
`JUnit 612 tests, floor 239` (607 + 5 nieuwe);
13 `check-reftype-derivation: PASS (131 unit annotations twisted over
27 goldens, floor 74; 128 executor replays …, 25 of them PLAYED by the
DRL-text branch, floor 13; 0 pinned survivors)`;
14 `check-flogic: PASS — frontend suite green (130 checks, floor 81); 2
flogic golden(s) …`.
Na de gates is alleen de Javadoc van `CaseRunner.consistentie`
bijgewerkt, zonder gedragswijziging.

**Registerzin** voor `spec/CONSTRUCTS.md` rij 19 "Consistency rules"
(§13.4.8, §9.5; de noderij `ResolvedConsistentie` verwijst ernaar):
"Drools backend (2026-09-23): the case runner checks an `inconsistent`
expectation's `label` against the channel fact's subject for every key
kind, as ref does (before, only default-group keys were, and a
rule-name pin passed on any fact from its rule); and both transpiler
branches now deposit the §9.5 allocation error (empty weight; empty or
negative maximum) on each offending recipient, rule × subject per
ch. 5.6 and ch. 4.2/4.3, where they had laid it on the source —
the divergence the unchecked label had masked."
