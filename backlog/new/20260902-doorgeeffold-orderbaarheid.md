# Doorgeeffolds over equality-only-typen: weigeren op de familie-lezing

Goal: EERSTE / LAATSTE / MINIMUM / MAXIMUM zijn ordenende folds; IR-SPEC
ch. 2.4 verklaart Enumeratie en SumType equality-only, en WF-038 draagt
de operandclausule al in tekst ("numeric folds' operands MUST be
numeric …; date folds' (first/last) operands MUST be dates") — maar de
verifier implementeert haar niet, terwijl de zusterroute (de
vergelijkingsoperator) onder WF-036 al dicht is. **Maintainerbesluit
2026-09-04: weigeren, op de familie-lezing** — alle operanden van een
ordenende fold komen uit één vergelijkbare familie (getal óf
datum/datumtijd), equality-only-elementtypen zijn ill-formed;
`Onbekend`-getypeerde en afwezige operanden deferren (één
parity-document hangt daaraan). De letterlijke date-only-tekst van
WF-038 wordt daarbij herschreven naar deze familie-lezing — dat
ratificeert de v1-verbreding die RUNTIME-SEMANTICS (rond r. 148-158) al
als tracked deviation draagt; zonder die herschrijving breekt
`jutterswet-peildatum` (numerieke EERSTE/LAATSTE, 4 knopen).

Meetfase (2026-09-04, volledig; 24 verifier-schone /tmp-documenten):

* **Vorm A, set-bron** (`elementObjecttype` + set): ref speelt één
  bijdrager stil goed en fault op twee-plus met `unordered comparison`
  — óók op drie keer dezelfde waarde (`Zuid vs Zuid`), en gemeten
  datumafhankelijk (een peildatum-filter bepaalt of het document speelt
  of fault). clingo weigert luid (enum-fence resp. `unscalable
  datatype`/`not translated`). Drools: EERSTE/LAATSTE transpiler-SKIP;
  MINIMUM/MAXIMUM over enum een **ERROR** (`IllegalStateException: kan
  accumulate-resultaat niet naar BigDecimal omzetten`, ook bij één
  bijdrager); SumType loader-EXCLUDE.
* **Vorm B, geleed** (twee-plus opgesomde operanden): ref fault; clingo
  weigert (enum), **crasht** op de gelede som-EERSTE/LAATSTE
  (`Division_by_zero` — eigen kaartje
  `20260904-clingo-symbolische-schaal.md`), of weigert (som-MIN/MAX);
  Drools **antwoordt een waarde** — hoofdlettergevoelig ASCII op het
  label, beide transpilertakken — waar ref fault: een ref ≢
  Drools-waardedivergentie, vandaag alleen onzichtbaar omdat geen
  corpusdocument de vorm draagt.
* **v0**: EERSTE/LAATSTE hebben geen instance-fold-vorm
  (`isAggregatieKind`); de frontend weigert equality-only-operanden al
  bij typering; waar het toch bij de runtime komt is de orde
  Java-`Comparable` — alfabetisch, per ongeluk, niemands ontwerp.
* **Corpus**: 0 dragers in golden/rood/parity; de enige vijf
  negatief-dragers zijn al ill-formed op WF-089/WF-090 en krijgen een
  toegestane incidentele regel.

Onder het besluit worden de drie gemeten breuken (Drools-ERROR,
clingo-crash, ref≢Drools-waarde) onbereikbaar via geldige documenten.

Acceptance:
- Rood-eerst per fold-soort en per vorm (set-bron én geleed); daarna de
  WF-038-clausule met de WF-036-machinerie (`is_number_like`/`is_date`,
  familie per fold, defer op `Onbekend`/afwezig), clausule-exact
  mutant-bewezen.
- Negatief per fold-soort; sweeps over de vier zones met nul
  over-afwijzing (0 golden/rood/parity — gemeten verwachting).
- De WF-038-tekst (IR-SPEC ~r. 2699-2701) en de tracked-deviation-
  passage (RUNTIME-SEMANTICS ~r. 148-158) herschreven naar de
  familie-lezing (voorstel); de WF-038-statuskolom ("fold
  unit-compatibility OPEN") en de meetzinnen in de WF-089/WF-090-
  passages (IR-SPEC ~r. 514-522, ~r. 767-775) plus de vier
  NOTE-falsifieerbaarheidsnotities van de doorgeefluik-negatiefs mee.
- Alle dertien gates groen; geen bestaande pin beweegt.

NB (meetfase, buiten dit besluit): de set-bron EERSTE/LAATSTE over wél
ordenbare typen (datums) is frontend-emitteerbaar en verifier-schoon
maar wordt alleen door ref gedragen — clingo en Drools weigeren luid.
Dat is de staande, gedocumenteerde executor-asymmetrie; een
clingo-arm-kaart is optioneel vervolgwerk. En een payload-loos SumType
bestaat niet (WF-087 normaliseert naar Enumeratie) — die cel vervalt.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/negatief/**`,
`backlog/`.

Out of scope: een orde definiëren (verworpen 2026-09-04: ref-signatuur
tot in `scalar_cmp`, ontbrekende clingo-armen, drie Drools-stukken op
twee takken, en een bewuste divergentie van de gemeten
productiesemantiek); de clingo-crash zelf (eigen kaartje); executors.
