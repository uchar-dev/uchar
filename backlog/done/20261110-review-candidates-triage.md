# Triage of the review candidates of 2026-11-10

Goal: every candidate below is either fixed with a witness or struck with the reason. The candidates come from a
read-only review of 2026-11-10 that was stopped half-way: they are suspicions from the reviewers' notes, not
findings. Only the first has a measurement behind it. Line numbers are those of `ccf6de21c`.

Per candidate: reproduce it first (a minimal document, case or source; the built binaries suffice). A reproduced
fault is fixed in this card with its witness — a negative case, a golden, a red case or a unit test, whichever the
zone uses — and a candidate that does not reproduce is struck in the Result with one sentence saying why.

Round A — a wrong value or a lost fault in an executor:

| # | Place | Suspicion |
|---|---|---|
| A1 | `backends/clingo/lib/translate.ml:10542-10546` | The range guard of the additive join in a closed body stands on the term after scaling, with bound 2^30/k; the sibling at `:7030-7032` guards the raw term. The product can wrap inside the guard. Measured at clingo level (5.8.2): `50000*50000 = -1794967296`, and `42949673*100` gives 4 while a guard of that form does not fire. Not yet shown through the runner: a fold with body `acc + x`, `acc` of two decimals, `x` a whole number of at least 42,949,673. |
| A2 | `backends/clingo/lib/translate.ml:3695` (`ctor_functor`) | A constructor or struct domain named as an ASP keyword (`not`) gives a clingo syntax error instead of a quoted functor or a refusal. |
| A3 | `backends/drools/.../transpiler/tms/TmsTranspiler.java:3778-3815` (`deposits`) | The walker has a default arm `false` and does not descend into `Construct.argument`, `PayloadAccess.base` or the bounds of a `TimeWindow`; `depositRule` then returns null and a fault inside such a part may be lost. `lowerOwnRule` (`:5094`) decides by this walker, `writeRule` (`:3382`) by the deposit flag. |
| A4 | `TmsTranspiler.java:2650` (`periodArithmeticTarget`) | Reads `isPeriodArithmetic(x.value())` without `letBound`, where `timelineRule` and `writeRule` use `letBound(r.value())`. |
| A5 | `ref/lib/timeline.ml:163` (`pro_rata`) | No `refuse_open_periods` before bucketing; an open bound may reach `bucket`. |
| A6 | `ref/lib/arith.ml:358` (`pow_rounded`) | A negative base with a whole exponent may fault as fractional. |

Round B — a document or a source accepted that should be refused, or the reverse:

| # | Place | Suspicion |
|---|---|---|
| B1 | `backends/drools/.../loader/IrLoader.java:2888-2911` | Lenient readers (`text`, `bool`, `intOf`, `stringList`, `elements`) beside the strict ones (`textAt`, `truth`, `whole`, `textList`, `list`): `intOf` wraps a value beyond int through `asInt` (a threshold of 4294967297), `elements` reads a non-array as empty. Sites at `:1307`, `:1569`, `:2283`, `:2315`, `:2371`. |
| B2 | `IrLoader.java:2230` | A `CharacteristicCheck` whose host is not declared reads as the subject. |
| B3 | `IrLoader.java:1596` | `DimensionCellRead.attribute` that is no `AttributeReference` ends in a ClassCastException. |
| B4 | `IrLoader.java:853-855` | A misindented `if`: the designations are read outside it. |
| B5 | `backends/drools/.../transpiler/core/Namen.java:133` (`escapeRuleName`) | Not injective (`a"b` and `a'b`, `a\b` and `a/b`): two rules may get one name. |
| B6 | `frontends/regelspraak/.../resolver/RuleChecker.java:1067` (`checkPaths`) | The subject is taken only where the target is an `AttributeReference`; a rule with a dimension cell as target gets subject null, and a navigated read or set fold in it may get RGS-119 or RGS-130 wrongly. Other sites of `target() instanceof Expr.AttributeReference` likewise. |
| B7 | `RuleChecker.java:1541` (`foldError`) | Casts the fold source to `AttributeReference`; a cell or a `PayloadAccess` there ends in a ClassCastException. |
| B8 | `RuleChecker.java:1014-1037` (`windowPositionIn`) | The default arm skips `Construct.argument` and `PayloadAccess.base`. |
| B9 | `ExpressionBuilder.java:3275`, `:3301`, `:3249` | `functionKind` falls back to `SOM`; `ordinalPosition` calls `parseInt` uncaught; `functionType` types a day count as unknown. |
| B10 | `RuleChecker.java:343` (`checkDependencyCycles`) | Catches `IllegalStateException` and `UnsupportedOperationException` and returns. |
| B11 | `spec/verifier/lib/checks.ml:6001`, `:6142`, `:6512`, `:6657` (`walk_expr`) | `~body` is not passed on into function arguments, `emptyFallback`, a temporal read and a cell read; an `IfThenElse` in a `Let` body below an aggregation argument may be judged at rule position (WF-069). |
| B12 | `checks.ml:7601` | WF-043 on a whole-period check is held only for a check that is not navigated. |
| B13 | `spec/verifier/lib/loader.ml:123` (`check_keys`) | A JSON object with a key twice is accepted, the first wins. |
| B14 | `frontends/flogic/lib/lower.ml:333`, `:699`, `:895`, `:69` | `?X = ?X + 1` recurses without end; of two bindings of one variable the second is dropped; the ids of exceptions do not reach `Default.name`; `sign_of` says "negative" where it checks `negatief`. |

Round C — gates and distribution:

| # | Place | Suspicion |
|---|---|---|
| C1 | `scripts/check-verifier.sh:237` | `grep -q "$expect"` finds the rule id anywhere in the report, also in a message, and not only as the key of a line. |
| C2 | `scripts/check-oracle.sh:120` | `for d in corpus/golden/*/` lists goldens without a case one level deep only. |
| C3 | `scripts/check-self-contained.sh:58` | An error status of `git grep` (above 1) counts as no hit. |
| C4 | `scripts/check-verifier.sh:421-428` | A branch that cannot be reached with the floor at 568. |
| C5 | toolchain `Verbaliser.java:2934`, `:2215`, `:2727` | `readRaw` turns a `ReflectiveOperationException` into null; a `Compound` with `GEEN` reads "A en niet B"; `nlNumber` writes "-0,00". These go to the toolchain's backlog once reproduced. |

Acceptance:
- The Result has one line per candidate: fixed with the name of its witness, or struck with the reason.
- Every fix has a witness that fails on `ccf6de21c` and passes on the landing.
- `make gates` green; no floor lowered, no list entry added without its reason.

May touch: the files named above, their tests, corpus witnesses under `corpus/negative`, `corpus/red` and
`corpus/golden` (not `corpus/golden/anchor`), this card. `spec/verifier/` and `scripts/` are protected paths: the
main session lands those fixes.

Out of scope: every simplification the review noted (cards `tms-session-readers-out`, `period-helpers-one`); the
zones the review did not reach (`toolchain/service`, most of the gates, the second half of `checks.ml` and
`eval.ml`); a spec change — where a candidate turns out to be a question about the norm, it goes to the Result as a
question.

Result (2026-11-11): 31 kandidaten (25 van de kaart, de vier B14-vermoedens apart geteld, plus R1); 22 gefixt met een
getuige die op de stand vóór de fix rood was, 6 geschrapt, 2 normvragen (beantwoord, zie onder), C5 niet hier.
Bouwers per zone (clingo/ref, Drools-loader, frontend, ref/F-logic, TMS) in eigen worktrees, de hoofdsessie voor de
verifier en de gatescripts; A3/A4 bovenop kaart `tms-session-readers-out`.

- A1 — fixed: `test_clingo.ml` "closed-body acc + x", "closed-body x > 5,00", "value-position k > 5,00". De guard
  stond op de opgeschaalde term; twee zusters schaalden zonder guard. 42.949.673 geeft nu CLINGO-034, 200.000 het
  goede getal.
- A2 — fixed: `test_clingo.ml` "a constructor named Not"; `not` is het enige kale woord dat clingo reserveert.
- A3 — fixed: `DepositInWrappedPlaceWitnessTest`; `deposits` daalt af in `Construct.argument`, `PayloadAccess.base`
  en de grenzen van een `TimeWindow` (drie verloren EF-005-deposits en een onterechte DROOLS-130).
- A4 — fixed: golden `let-bound-period-arithmetic-total`; dezelfde fout zat in ref's `period_arithmetic_target`
  (die clingo aanroept), dus `ref/lib/eval.ml` is in dezelfde commit meegenomen: één regel, `let_bound_value`.
- A5 — struck: een open grens bereikt `pro_rata` niet (de case eist `from` en `to`; een afgeleide open grens geeft
  EF-023 vóór het schrijven).
- A6 — struck: `pow_rounded` wordt alleen met een gebroken literal bereikt; een gehele exponent rekent via `pow_`.
- R1 (naast A6) — fixed: `test_ref.ml` "ch2.2-pow-runtime-exponent-rounded" en drie zusters; een gebroken exponent
  die geen literal is, rekent nu onder zijn afronding.
- B1 — fixed: `LenientReaderWitnessTest`; de vijf optionele lezers weigeren een verkeerd JSON-type, een getal buiten
  int is DROOLS-003; `Default.decision` hoorde bij dezelfde familie.
- B2 — fixed: `ReferenceLayerRefusalWitnessTest.a_characteristic_check_on_an_undeclared_host_is_refused`.
- B3 — fixed: `ReferenceLayerRefusalWitnessTest.a_cell_read_reads_an_attribute_reference` (ClassCastException bij
  het laden, NullPointerException bij het afspelen).
- B4 — fixed: `VocabularyWitnessTest.a_defaults_group_without_a_name_enters_no_designation`.
- B5 — struck: `Namen.escapeRuleName` heeft geen aanroeper meer; `RuleNameWitnessTest` pint al dat de namen
  verschillend blijven.
- B6 — fixed: `DimensionTargetTest` (drie tests); een regel met een celdoel leest vanaf het type onder de cel.
- B7 — fixed: `DimensionCellOverRoleTest.a_cell_fold_without_a_range_is_refused` (RGS-131 in plaats van een
  ClassCastException).
- B8 — struck: op het moment van de venstercontrole draagt een `Construct` geen argument en een `PayloadAccess`
  alleen een attribuut of cel als basis.
- B9 — fixed: `OrdinalPositionTest`, `BuiltinTypeTest.the_day_count_is_a_number_to_the_assignment_check`; RGS-393
  voor een rangtelwoord zonder plaats; de dagentelling toetst haar soort. De terugval op `SOM` is onbereikbaar en
  werpt nu een interne fout (geen getuige: geen bron bereikt hem).
- B10 — fixed: `DependencyCycleTest.a_model_the_emitter_refuses_carries_the_refusal`; RGS-394 draagt de weigering
  van de emitter in plaats van een stack trace bij `--ir`.
- B11 — fixed: golden `jutterswet-let-keuze-in-laatste`; zeven `walk_expr`-aanroepen geven de body-positie door.
- B12 — fixed: negatieve case `wf043-wholeperiod-navigated-nontemporal`.
- B13 — vraag, beantwoord: weigeren (vervolgkaart `review-findings-2026-11-11`).
- B14 — fixed, vier delen: `refuse_self_binding.flogic`, `refuse_cyclic_binding.flogic` (stack overflow),
  `refuse_second_binding.flogic` (tweede binding viel stil weg; voorlopig besluit: weigeren, mandaat SUBTYPING §9.3
  punt 7), de suite-check "every Default carries the id of its definition" met vier `name`-regels in
  `jutterswet-wrakrecht-flogic`, en `refuse_sign.flogic` (alleen de tekst).
- C1 — fixed: gate 3 zoekt de regel als sleutel van een rapportregel (rood aangetoond met een document dat `WF-050`
  als kenmerknaam draagt).
- C2 — fixed: gate 5 vindt goldens zonder case op elke diepte (de ankers).
- C3 — fixed: gate 1 faalt op een fout van `git grep` (rood aangetoond met een ongeldige regex).
- C4 — struck: dode tak zolang de vloer boven nul staat; geen fout.
- C5 — niet hier: toolchain; staat in de vervolgkaart voor de toolchainsessie.

Normbesluiten van de maintainer (2026-11-11), uit te voeren in `review-findings-2026-11-11`: verdeling rekent om
(F1/F2); dubbele JSON-sleutel wordt geweigerd (B13); een herhaald label in een rol-lijst is één link (F6);
EF-011/EF-014 voor de sessiestart (F5); leeg deeltal leest de deler niet (K-C); WF-126 dekt de initialisatie (V4);
de IfThenElse-guardpositie gaat uit WF-127 (V5); een negatief grondtal met een gebroken exponent is altijd een fault.

`EXPECTED_PLAYED` 365 → 367 (twee goldens). `make gates` groen bij de landing samen met `tms-session-readers-out`.
