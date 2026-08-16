# CONSTRUCTS — the scope matrix

*Status: Phase-0 inventory complete (2026-08-15). Sources mined:
`regelspraak-java` at the pinned commit `b85dff3` — the `ir/` node set
(`Expr`, `Conditie`, `Resolved*`), the model layer, the coverage-manifest
mechanism, and the `Onopgelost` production sites — plus the public
requirements lists of LegalRuleML (OASIS), DMN/FEEL (OMG), Catala (ICFP
2021), and OpenFisca. Verdicts are session proposals; the maintainer
confirms or overrules them when the corresponding spec text lands. Spec
references (`§…`) are section numbers of the RegelSpraak language
specification — numbers only, no text from it is reproduced here.*

Columns:

- **Verdict (v1)** — `core` | `dialect` (RegelSpraak-specific, lowers to
  core) | `sugar` (desugars in the frontend) | `later(…)` | `non-goal`.
- **Kernel or sugar?** — the GHC-Core question: does the construct need its
  own kernel node, or is it expressible in existing ones?
- **v0→v1 delta** — what changes relative to the faithful dump of today's
  `regelspraak-java` IR; "—" = mirrored as-is.

Composite verdicts appear where one v0 node divides across categories, and
are always explained in place: `split` (the delta names the parts and their
verdicts), qualifications like `core (reshaped)` (core concept, v0 shape
replaced) or `core (metadata)`, and `data-plane` (semantics specified in
RUNTIME-SEMANTICS, not a language-IR node).

A global delta applies to every row (PLAN §9, decision 6): v0 node names are
Dutch (`ResolvedRegel`, `Vergelijking`, …); the v1 metamodel renames them to
language-neutral English. The inventory below uses the v0 names, because they
identify what is being mirrored.

## 1. The scope matrix

| # | Construct | Origin | Verdict (v1) | Kernel or sugar? | v0→v1 delta | Notes |
|---|-----------|--------|--------------|------------------|-------------|-------|
| 1 | Typed expression trees (arithmetic, comparison, functions, aggregation) | RegelSpraak §13.4.16/.17 | core | kernel | split the overloaded `Functie` node into aggregation-with-source (element type, filter, empty-value fallback, dimension selection) vs plain builtin call | v0 conflates them in one node; see §2.1 |
| 2 | Assignment rules incl. versions with validity windows | §13.4.4/.9, §13.4.2.5 | core | kernel | merge BEREKENING/GELIJKSTELLING into one node + presentation tag; version selection becomes explicit IR structure, not a rule-name suffix | PLAN §9 (4); `INITIALISATIE` goes to row 7, `KENMERKTOEKENNING` stays as a target-kind flag; Catala's labeled variable states are a later sugar over chained assignment. Candidate WF (RUNTIME-SEMANTICS ch. 2.4): a possibly non-terminating value must pass explicit rounding before a bounded write |
| 3 | Source references | — | core | kernel | `String bron` → `Bron(file, line, column, snippet)`; `// Bron:` statute annotations pass the lexer | Phase 0 |
| 4 | Fact types and role navigation | §13.3.9, §13.4.1 | core | kernel | bean-field names (`zetVeld`/`joinVeld`/`scopeVeld`/`backref`) → symbolic `RolBinding(feittype, rol)`; navigation as role traversal with declared cardinality | Phase 3 forcing move. The model layer is already symbolic (`Feittype`/`Rol` are clean); the leak lives in the navigation contract and the `ir/` encoding. The scenario layer's label-based role values show the neutral form to generalise. Confirmed by the ASP hand-translation (`ASP-TRANSLATION.md`): a fact type is a plain predicate, navigation is variable sharing |
| 5 | Role membership (near-side and far-side exists) | §8.1.7 | core | kernel | drop the top-level-only restriction (a Drools pattern artefact) and the `backref` field; merge near-side and far-side checks into one existential role construct | Phase 3; confirmed by the ASP hand-translation: the existential is a body variable at any nesting depth — the restriction has no translation target |
| 6 | Negation over derived characteristics | §8.1.8 | core | kernel | salience −1 → declared stratification requirement (WF rule) | Phases 3–4. ASP hand exercise 2 (`ASP-TRANSLATION.md` ch. 6): the art. 4 ladder is a four-stratum negative chain over *derived* predicates — the WF condition is acyclicity of the negative dependency graph, and the statute's own "geldt niet / geldt weer niet" nesting supplies the strata |
| 7 | Initialisation-only-if-empty | §9.6 | core | kernel | leeg-guard → explicit defeasible default | Phase 4; OpenFisca's per-variable `default_value` is the same idea — a declared default must never mask `Onopgelost` |
| 8 | Exception trees with priorities + conflict detection | Catala default calculus | later(Phase 4) | kernel | new: adopted default model; conflicts → the §9.5 inconsistency channel | the hard dependency for Catala interop. Catala distinguishes under-determination (EmptyError) from over-determination (ConflictError) — v1 must keep those outcomes distinct. LegalRuleML adds strict-vs-defeasible tagging and cross-rule override as refinements. ASP hand-translation: the single-layer art. 2 default is one NAF over a declared contrary; no priority mechanism appears. Exercise 2 (`ASP-TRANSLATION.md` ch. 6): exception-on-exception = layered NAF over derived exception predicates — the Phase-4 model needs a priority *chain*, not pairwise defaults; parallel "evenmin" grounds compose as a conjunction of NAFs outside the ladder; a linear ladder never conflicts, so the §9.5 channel is exercised only by *unordered* simultaneous exceptions; keep layering first-class — the compiled DNF erases exactly the structure amendments edit |
| 9 | Scopes with in/out variables, rebound subscope inputs | Catala | later(Phase 5) | sugar (over object types + creation + navigation) | — | real design work; Catala's subscope call (instantiate, bind inputs, read outputs) is the composition mechanism to cover |
| 10 | General folds / function values | Catala | non-goal (v1) | — | — | subset stance; DMN's Business Knowledge Model + invocation (named reusable logic, bind-by-name) falls under the same stance |
| 11 | Timelines (granularity, variable starting points) + parameter vintages | §13.3.6, §9.9 | core | kernel | temporality into the type system (timeline-of-element), out of the side tables; the startpunt registration (`ResolvedStartpunt`) becomes explicit anchor metadata, the write stays an ordinary rule | review improvement 5. Parameters are a distinct leaf category (fact / derived / parameter) with their own vintage semantics — OpenFisca confirms the design. `Parameterset` value data is data-plane, specified in RUNTIME-SEMANTICS ch. 8, not a language-IR node. Automatic granularity conversion (OpenFisca periodisation) is later. First corpus-driven demand: art. 1's "op dat tijdstip" qualification (recorded gap in the ASP hand-translation) |
| 12 | Second time axis (knowledge time / retroactivity) | LegalRuleML; review | later(v2) | kernel | v1 names the single axis and the extension point | PLAN §9 (2); LegalRuleML distinguishes in-force / efficacy / application — the extension point must not hard-code "exactly two axes" |
| 13 | Jurisdiction | LegalRuleML | non-goal (v1) | — | — | standing LegalRuleML test |
| 14 | Rule groups / recursion marker | §9.10 (documented extension) | dialect | — | stays out of core | PLAN §9 (5). v0 defines `recursief` only as Drools fixpoint-within-one-`fireAllRules`; a v1 lowering needs the neutral fixpoint semantics of RUNTIME-SEMANTICS ch. 5, or the marker is a non-goal |
| 15 | Decision tables | §12 | sugar | — | already desugared by the frontend | keep. The desugaring spec must state its hit policy explicitly; DMN's hit-policy vocabulary (U/A/P/F/R/C/O) is the reference. DMN's unary-test cell language (ranges, value lists, "any") is frontend sugar under the same rule |
| 16 | `Onopgelost` (honest inexpressibility) | `regelspraak-java` | core | kernel | specified as a permanent part of the contract; v1 adds a structured reason (taxonomy) instead of a bare source slice | PLAN §9 (3); v0 has ~24 production sites — see §4. Distinct from FEEL-null (absent value) and from Catala ConflictError (over-determined): three different things that must not collapse |
| 17 | Allocation (equal / pro-rata / ordered + maximum, rounding, remainder) | §9.7, §13.4.10 | core | kernel | semantics as recurrence equations (`RUNTIME-SEMANTICS.md` ch. 4), not helper-defined | v0's `Methode.COMPLEX` (unhandled forms) must become a verifier error, never an IR value; two allocations writing the same target are agenda-order-dependent in v0 (principled skip) — in v1 a WF error |
| 18 | Empty-value discipline | §6 / divergences | core | — | reopened as a design decision before freezing | PLAN §9 (7); `VERMINDERD_MET`'s left-operand exception is part of the case list; FEEL's null propagation is the reference three-valued model to compare against; the open-world/closed-world boundary surfaced concretely in the ASP hand-translation (open world facts vs closed-world program inputs) |
| 19 | Consistency rules | §13.4.8, §9.5 | core | kernel | specify declaratively (the assertion must hold; violation is observable on the §9.5 channel), not as "fire on the negation" | v0 leaves compound criteria "not yet lifted" — v1 spec must cover them |
| 20 | Uniqueness, incl. union form (`verenigd met`) | §8.1.6 | core | kernel | unify `ResolvedUniciteit` + `ResolvedUniciteitUnie` into one construct (single type = one source); `scopeVeld` → symbolic scope navigation | key arity and temporal keys are open cases the v1 spec must decide |
| 21 | Object creation | §13.4.6 | core | kernel | — | attribute and kenmerk assignments on the new object mirror as-is |
| 22 | Fact creation (relation instance) | §13.4.7, §9.4 | core | kernel | drop the reference-field encoding (`zetVeld`/`joinVeld`, `Tussenstap.veld`/`viaOuderVeld`); a creation names a feittype and binds each role symbolically; path steps become {role, direction} | the heaviest single Drools leak in v0; same family as rows 4–5 |
| 23 | Day-kind definitions (dagsoorten) | §9.8, §3.12 | core | kernel | — | a named pure predicate over dates is engine-neutral and small; the unit-spelling recognition ("5 werkdagen") stays dialect |
| 24 | Local variables (let-bindings, "Daarbij geldt") | §13.4.2.10 | core | kernel | — | DMN's boxed context is the same construct; scoping rules become WF conditions |
| 25 | Evaluation-context globals (rekendatum/rekenjaar) | §4.2, §5.3 | core | kernel | split v0's `Constante` grab-bag: the evaluation-date globals are a kernel context reference; subject/pronoun referents ("hij", "de dag") are surface forms the frontend resolves away | the single-axis decision (row 12 / PLAN §9 (2)) names exactly this axis |
| 26 | Units and unit algebra | §13.3.5 | core | kernel | factors become true rationals; only the exact num/den restatement form is specified | composite/rate units, conversion factors, and unit systems are engine-neutral in v0; keep the algebra, drop the pre-rounded single-ratio form |
| 27 | Datatype refinements | §13.3.3/.4 | core | kernel | `Percentage` = `Numeriek` with an implicit `%` unit (dialect tag); `DatumInDagen`/`DatumTijd` = one date type parameterised by granularity; `Datatype.Onbekend` leaves the v1 sum (fail-fast — `Onopgelost` is the honest marker, not an "unknown type"); literals become typed values, not raw strings | FEEL's two duration kinds (calendar-sensitive months vs exact days) inform the date-arithmetic chapter; ambiguous calendar arithmetic fails fast (Catala) rather than guessing |
| 28 | Dimensioned attributes + dimension selection | §13.3.7, §3.6 | split | kernel (in dialect) | keep pre-expanded label lists (good, engine-neutral); drop the map-get lowering from the node contract | the `Dimensie` declaration itself (an ordered label domain) is core, like `Enumeratie`; the dialect part is its *use* on attributes — cell read (`DimensieCel`, every dimension pinned = WF condition) and aggregation selection |
| 29 | Whole-period qualifier ("gedurende het gehele jaar") | §8.2 | dialect | lowers to timeline algebra | — | specified as a temporal operator lowering onto RUNTIME-SEMANTICS ch. 3, not per-backend markers |
| 30 | Rule-execution reflection ("is gevuurd" / "is inconsistent") | §8.1.9 | split | — | — | "is gevuurd": non-goal — v0's own backends both decline it (no observable runtime record). "is inconsistent": later — it reads the §9.5 channel and waits for that channel's spec (rows 8/19) |
| 31 | Text interpolation | §5.4 | sugar | — | desugars to concatenation of coerced parts | v0's `TekstReeks` |
| 32 | Deontic layer (obligation/permission/prohibition, violation → reparation, alternatives) | LegalRuleML | non-goal (v1) | — | — | uchar v1 is a constitutive calculation IR: rules define what counts as what, not who must do what. This row is the explicit boundary of the standing LegalRuleML test (with rows 12–13) |
| 33 | Norm authority/agent attribution | LegalRuleML | later(v2) | sugar (metadata on row 3) | — | provenance metadata on source references; cheap, not evaluation-relevant |
| 34 | Money discipline | Catala; OpenFisca | core (via rows 26/27 + rounding) | no new kernel node | — | decision: no dedicated money type; a unit-typed exact numeric plus mandated rounding (`Afgerond`, §6.1.2 assignment gate) covers it. Rounding mode is legal semantics, never display |
| 35 | Parameter scales / brackets (marginal rates) | OpenFisca | later(v2) | kernel if adopted | — | central to tax law but absent from RegelSpraak; expressible today as rules/decision tables. A first-class scale node waits until a frontend demands it — adopt, don't invent |
| 36 | Sum types with exhaustive match | Catala | later(Phase 5) | kernel if adopted | — | enumeration domains (no payload) are already core via row 27; payload-carrying variants + compile-time-total match come with the Catala frontend |
| 37 | Scenario / conformance-test format | `regelspraak-java` extension | non-goal (for the IR) | — | — | v0's `ResolvedScenario` tree is a test-harness IR, not executable law. It is the donor for the corpus case schema — delivered as `uchar-case-v0` (`spec/schema/uchar-case-v0.schema.json`, gate 4): inputs, expected outcomes, the explicit-empty value slot, label-based role values (which also feed row 4) |

## 2. Node inventory (v0, `regelspraak-java` @ `b85dff3`)

Every permitted subtype and top-level form, with its verdict and the matrix
row it belongs to. Discriminator enums are listed under their node.

### 2.1 Expression layer — `Expr`, 12 permitted subtypes

| Node | Meaning (own words) | Spec § | Verdict (v1) | Row | Delta / notes |
|---|---|---|---|---|---|
| `Literal` | constant value with resolved datatype | §13.2 | core | 1 | v1: typed value, not a raw source string (row 27) |
| `TekstReeks` (+ `Deel`: `Letterlijk`, `Ingevoegd`) | text interpolation | §5.4 | sugar | 31 | desugars to concatenation |
| `Binair` | binary arithmetic | §13.4.16 | core | 1 | see `BinOp` |
| — `BinOp` | PLUS, MIN, MAAL, GEDEELD_DOOR, TOT_DE_MACHT | §13.4.16 | core | 1 | kernel operator set |
| — `BinOp` (variants) | VERMINDERD_MET, GEDEELD_DOOR_ABS, PERCENTAGE_VAN | §13.4.16 | dialect/sugar | 1, 18 | VERMINDERD_MET differs only in empty-handling — its lowering depends on the row-18 decision; GEDEELD_DOOR_ABS is dialect (a defined collapse point, RUNTIME-SEMANTICS ch. 2.2); PERCENTAGE_VAN desugars (×1/100) |
| `Functie` | one overloaded node: aggregations, scalar builtins, date/time functions | §13.4.17 | core | 1 | v1 splits aggregation-with-source vs builtin call; drop the Drools accumulate framing |
| — `FunctieKind` (aggregations) | SOM, TOTAAL, AANTAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE | §13.4.17 | core | 1 | TOTAAL is **not** sugar for SOM: same fold, observably different empty-source identity (som = empty unless fallback; totaal = 0 unconditionally, §4.20 — RUNTIME-SEMANTICS ch. 1.2) |
| — `FunctieKind` (scalar/text) | WORTEL, ABSOLUTE_WAARDE, CONCATENATIE | §13.4.17 | core | 1 | builtin calls |
| — `FunctieKind` (date/time) | TIJDSDUUR, ABSOLUTE_TIJDSDUUR, TIJDSEVENREDIG_DEEL, AANTAL_DAGEN_IN, EERSTE_PAASDAG, DATUM_MET, DATUMTIJD_MET, JAAR/MAAND/DAG/UUR/MINUUT/SECONDE/MILLISECONDE_UIT | §13.4.17 | dialect | 1, 27 | builtin library; semantics land in RUNTIME-SEMANTICS ch. 2–3 |
| — `DimensieSelectie` | pre-expanded label selection for aggregation over a dimensioned attribute | §13.4.17.51–.54 | dialect | 28 | pre-expansion is good and stays |
| `Afgerond` (+ `AfrondModus`: NAAR_BENEDEN, NAAR_BOVEN, REKENKUNDIG, RICHTING_NUL, WEG_VAN_NUL) | explicit rounding to N decimals | §13.4.17.25/.26, §6.1.2 | core | 34 | load-bearing for assignment precision |
| `Begrensd` | clamp to min/max | §13.4.17.27–.30 | sugar | 1 | min(max(x, lo), hi) |
| `AttribuutRef` | attribute read, possibly via navigation | §13.4.2.9 | core | 4 | v1: one explicit navigation path (role steps + cardinality) instead of `navigatieRol` + `navigatiePad` + `set` flag; strip transpiler framing |
| `DimensieCel` | single-cell read of a dimensioned attribute | §3.6 | dialect | 28 | every dimension pinned = WF condition; no map-get in the contract |
| `ParameterRef` | parameter reference | §13.3.8 | core | 11 | v1: symbolic parameter id, not an embedded model object |
| `VariabeleRef` | local let-binding reference | §13.4.2.10 | core | 24 | — |
| `Constante` | grab-bag: rekendatum globals + pronoun referents | §4.2, §5.3 | split | 25 | evaluation-date globals kernel; pronoun referents resolved away by the frontend |
| `Onopgelost` (Expr) | honest unresolved marker, typed `Onbekend` | — | core | 16 | permanent contract; v1 adds a structured reason |

### 2.2 Condition layer — `Conditie`, 9 permitted subtypes

| Node | Meaning (own words) | Spec § | Verdict (v1) | Row | Delta / notes |
|---|---|---|---|---|---|
| `Vergelijking` | binary comparison | §13.4.13–.15 | core | 1 | — |
| — `Operator` | KLEINER(_OF_GELIJK), GROTER(_OF_GELIJK), GELIJK, ONGELIJK, EERDER(_OF_GELIJK), LATER(_OF_GELIJK) | §5.1–5.10 | core | 1 | EERDER/LATER are date-typed ordering — v1 may unify under typed ordering |
| `Predicaat` | unary test on one operand | §5.11–5.16 | split | 1 | see `Predicaatsoort` |
| — `Predicaatsoort` | LEEG, GEVULD | §5.13/.14 | core | 18 | kernel emptiness tests, tied to the row-18 decision |
| — `Predicaatsoort` (variants) | ELFPROEF, NUMERIEK_MET_EXACT, DAGSOORT | §5.15/.16, §3.12 | dialect | 1, 23 | builtin predicates |
| `Kenmerk` | characteristic membership test, own or via to-one navigation | §8.1.8, §13.4.7 | core | 1, 6 | a row-1 condition primitive; row 6 governs its negation. Drop the `eigenaar` pattern-steering field (Drools leak) |
| `Rollidmaatschap` | near-side role check | §8.1.7 | core | 5 | merge with far-side into one existential role construct |
| `VerreRollidmaatschap` | far-side role check (exists a filler) | §8.1.7 | core (reshaped) | 5 | drop `backref` and the top-level-only restriction — the two named v0 leaks |
| `Periode` | temporal validity window on a condition | §8.4.3 | core | 11, 25 | rekendatum-gating becomes explicit semantics, not a lowering note |
| `Samengesteld` | quantified compound over sub-conditions | §13.4.14 | core | 1 | — |
| — `Kwantor` | ALLE, GEEN, DE | §13.4.14 | core | 1 | kernel quantifiers |
| — `Kwantor` (counting) | TEN_MINSTE, TEN_HOOGSTE, PRECIES | §13.4.14.4 | sugar | 1 | count-and-compare |
| `Regelversie` | reflection on another rule's execution | §8.1.9 | split | 30 | "is gevuurd" non-goal; "is inconsistent" later |
| `Onopgelost` (Conditie) | honest unresolved condition | — | core | 16 | permanent contract |
| — `GeheelPeriode` | whole-period qualifier on Vergelijking/Predicaat/Kenmerk | §8.2 | dialect | 29 | a temporal operator, not a backend marker |

### 2.3 Top-level forms — `ir/Resolved*`, `Geldigheid`

| Node | Meaning (own words) | Spec § | Verdict (v1) | Row | Delta / notes |
|---|---|---|---|---|---|
| `ResolvedRegel` | assignment/derivation rule: target ← expression, guarded, versioned | §13.4.4/.9 | core | 2 | version selection explicit, no name mangling; drop then/when framing |
| — `Soort` | GELIJKSTELLING, BEREKENING, INITIALISATIE, KENMERKTOEKENNING | §13.4.4/.9 | split | 2, 7 | first two merge (presentation tag); INITIALISATIE → defeasible default; KENMERKTOEKENNING → the target's member kind (attribute vs characteristic reference — IR-SPEC ch. 3.1), no separate flag |
| `ResolvedVerdeling` | allocation over recipients (method, cap, rounding, remainder) | §13.4.10, §9.7 | core | 17 | recurrence-equation semantics; ordering keys are domain, not engine |
| — `Methode` | GELIJKE_DELEN, NAAR_RATO, OP_VOLGORDE | §9.7.2 | core | 17 | — |
| — `Methode.COMPLEX` | donor placeholder for unhandled forms | — | non-goal | 17 | verifier error in v1, never an IR value |
| `ResolvedObjectCreatie` (+ `Toekenning`) | create object, assign attributes/kenmerken, link via feittype | §13.4.6 | core | 21 | — |
| `ResolvedFeitCreatie` (+ `Tussenstap`) | create relation instance along a navigation path | §13.4.7, §9.4 | core (reshaped) | 22 | drop `zetVeld`/`joinVeld`/`veld`/`viaOuderVeld`; symbolic role bindings + {role, direction} steps |
| `ResolvedConsistentie` | assertion that must hold; violation observable | §13.4.8, §9.5 | core | 19 | declarative, not fire-on-negation; compound criteria must be covered |
| `ResolvedUniciteit` | key unique across a type, optionally scoped | §8.1.6 | core | 20 | `scopeVeld` → symbolic scope navigation |
| `ResolvedUniciteitUnie` (+ `Bron`) | key unique across a union of sources | §8.1.6 | core | 20 | unify with the single-type form |
| `ResolvedDagsoort` | day-kind as predicate over a date | §9.8 | core | 23 | — |
| `ResolvedStartpunt` | registers the anchor attribute of a variable-start timeline | §9.9 | core (metadata) | 11 | explicit anchor metadata; the write is an ordinary rule |
| `ResolvedRegelGroep` | rule group + recursion marker | §9.10 (extension) | dialect | 14 | recursion marker needs neutral fixpoint semantics or is non-goal |
| `ResolvedScenario` (nested: `Scope` + `ScopeSoort` {ENKELE_REGEL, MEERDERE_REGELS, REGELGROEP}, `ScopeRegel`, `Situatie`, `Instantie`, `AttribuutWaarde`, `RolWaarde`, `KenmerkWaarde`, `ParameterWaarde`, `Verwachting` + `VerwachtingSoort` {ATTRIBUUT_WAARDE, ATTRIBUUT_LEEG, KENMERK_AANWEZIG, KENMERK_AFWEZIG, CONSISTENT, INCONSISTENT}, `Waarde`) | test scenario: rules under test, input world, expectations | extension | non-goal (IR) | 37 | corpus-schema donor: label-based roles, unit-normalised values, explicit-empty slot |
| `Geldigheid` | validity window [vanaf, tot], reused for vintages | §13.4.2.5, §4.2 | core | 2, 11 | — |

### 2.4 Model layer

| Node | Meaning (own words) | Spec § | Verdict (v1) | Row | Delta / notes |
|---|---|---|---|---|---|
| `RegelSpraakModel` | container: declarations + diagnostics + transpiler input | — | split | — | normative declarations stay; the `ir.Resolved*` lists and backend helpers (rule-fired bookkeeping facts, inconsistency-flag insertion, engine loop-guards, rule-name prefixing) leave the IR type |
| `Objecttype` | entity type: attributes, kenmerken, temporal/dimension facets | §13.3.3/.6/.7 | core | 11 | reify `Attribuut` and `Kenmerk` as first-class nodes; the parallel per-name side-maps are the v0 shape row 11's "out of the side tables" delta targets |
| — `KenmerkSoort` | BIJVOEGLIJK, BEZITTELIJK, ZELFSTANDIG | §3.5 | dialect (presentation) | 2 | same boolean membership; phrasing tag only — the row-2 pattern again |
| `Parameter` | global typed value, possibly a timeline | §13.3.8 | core | 11 | v1 parameters carry the full timeline facet (granularity + count + name); v0 records only a bare granularity |
| `Feittype` / `Rol` | relation type with symbolic roles + cardinality | §13.3.9 | core | 4 | data already symbolic; fix the navigation contract (reference-field assumption) |
| `Granulariteit` | DAG, WEEK, MAAND, KWARTAAL, JAAR | §13.3.6 | core | 11 | — |
| `Tijdlijn` | named timeline: granularity, count, variable start | §13.3.6.10, §9.9 | core | 11 | — |
| `Dimensie` | dimension with ordered labels | §13.3.7 | core | 28 | the declaration side of row 28's split; `voorzetsel` is presentational |
| `Regel` (shell) | parse-stage rule stub (name + soort string) | §13.4 | non-goal | 2 | superseded by `ResolvedRegel`; collapses away |
| `KetenHop` | one resolved navigation hop (role → target type) | §13.4.1 | core | 4 | describe as symbolic traversal, not getter emission |
| `Datatype` (sealed sum) | the type system | §13.3.3 | core | 27 | closed sum stays |
| — `Numeriek` (+ `Teken`: ONBEPAALD, NEGATIEF, NIET_NEGATIEF, POSITIEF) | number: integer/decimal, precision, sign, unit | §13.3.3.2 | core | 27 | — |
| — `Percentage` | number with implicit `%` unit | §13.3.3.3 | dialect (tag) | 27 | = Numeriek + `%` |
| — `Tekst`, `Booleaan` | primitives | §13.3.3.4/.5 | core | 27 | — |
| — `DatumInDagen`, `DatumTijd` | date at day vs millisecond granularity | §13.3.3.6 | core | 27 | unify: one date type parameterised by granularity |
| — `Enumeratie` | enumeration domain (name + values) | §13.3.4.2 | core | 27, 36 | domain identity a real reference, not a name string |
| — `Onbekend` | resolver error-recovery type | — | non-goal | 27 | leaves the v1 sum; fail-fast |
| `Eenheid` / `Eenheidstelsel` | units, rate/product units, conversion; unit systems | §13.3.5 | core | 26 | conversion factors become true rationals; only the exact num/den restatement form is specified (v0's pre-rounded DECIMAL64 single ratio is dropped) |
| `Parameterset` (+ `WaardeInterval`) | deployed vintage values, peildatum-selected, fail-fast | — | data-plane | 11 | specified in RUNTIME-SEMANTICS ch. 8, not a language-IR node |
| `Rationaal` | numeric-literal parse helper | §13.2.5 | non-goal | — | frontend utility, not IR |
| `Severity` (ERROR, WARNING, INFO) / `Diagnostic` | tooling diagnostics | — | non-goal | — | build-time tooling, not IR |
| dagsoort registry | day-kinds usable as units | §3.12, §9.8 | core / dialect | 23 | predicate core; unit-spelling recognition dialect |

## 3. The coverage manifest (v0 mechanism)

The manifest is not a static construct→verdict table. Both backends wrap the
same seven construct families — regel, objectcreatie, verdeling,
consistentie, uniciteit, uniciteit-unie, feitcreatie — in a `meet()` step
that records one verdict per model instance, in model order, so the two
manifests are positionally comparable. The default is LOWERED. The DRL
backend derives MARKER by scanning its own emitted output for a
TODO/onopgelost sentinel (per sub-expression: the rule is still emitted, with
an inert term); the exec-model backend derives SKIP from its skip list (per
whole construct: no rule emitted). The differential test reconciles that
granularity asymmetry via an explicit allowlist.

Two consequences for v1. First, "coverage" in v0 is a property of the
*output*, not of the IR — the honest verdict machinery itself is
backend-local. In v1 coverage becomes an IR-level fact: a construct either
has specified semantics or is `Onopgelost` with a reason. Second, the
recurring marker/skip families are the v0 boundary map; the recurring themes
and where they land:

| Marker/skip family | What it tells v1 | Row |
|---|---|---|
| Multi-period temporal writes; temporal kenmerktoekenning with indeterminate write-producer; variable startpunt without startpuntbepaling | genuinely under-specified in v0 — the timeline-write semantics are a spec obligation (RUNTIME-SEMANTICS ch. 3/5), not a backend gap | 11 |
| Whole-period comparisons (§8.2 families), period-comparison clipping (§8.4.3) | the whole-period operator needs real timeline-algebra semantics | 29 |
| Date arithmetic, tijdsduur, dagsoort-as-unit families | the date/duration chapter must fix operand-precision and unit rules | 27, 23 |
| Undimensioned read of a dimensioned attribute; dimensiecel limits | WF conditions on dimension pinning | 28 |
| To-many navigation, unresolved navigation | cardinality-typed navigation (rows 4/5) removes the ambiguity | 4, 5 |
| Aggregation/value scope limits (exec phases; bound-variable limits) | backend maturity limits, not IR gaps — no v1 obligation beyond row 1 | 1 |
| Two allocations on one target (agenda-order-dependent, principled skip); `Methode.COMPLEX` | promote to WF errors | 17 |
| Compound consistency criteria, uniqueness union arity/temporal keys | v1 spec must cover what v0 declined | 19, 20 |
| Rule-execution reflection with unknown reference | see the split verdict | 30 |
| Non-rescalable composite-unit conversion in arithmetic conditions | unit-algebra edge the spec must decide | 26 |

## 4. The `Onopgelost` production inventory (v0)

Roughly 24 production sites, all funnelling into `Expr.Onopgelost` (typed
`Onbekend`) or `Conditie.Onopgelost`; every site pairs the node with a
warning or error, or relies on the downstream marker/skip warning. Grouped by
what they mean for v1:

- **Structural catch-alls** — unrecognised expression/condition shapes in
  the resolver's builders. Frontend concern; the IR only needs the marker
  node itself (row 16).
- **Declared-names-only refusals** — aggregation over an undeclared member,
  unknown names, unresolvable deep navigation chains. Correct fail-fast
  behaviour; v1 keeps the refusal and the WF rules make it checkable.
- **Genuine spec under-determination** — ordinal references ("de tweede …")
  over two-plus same-named bindings; the whole-period qualifier attached to
  a role check; a whole-period qualifier over a non-temporal subject. These
  are places where the source language is under-specified: v1 must either
  specify the semantics or declare the form inexpressible — honestly, per
  row 16.
- **Safety refusals** — a dangling negation that would silently invert a
  check; mis-parsed bullet nesting under counting quantifiers; union counts
  over mixed elements; a day-kind test over a non-date operand. The v0
  discipline "never a silent wrong answer" is exactly the row-16 contract
  and carries into v1 verbatim.

The v1 delta recorded in row 16 follows from this inventory: `Onopgelost`
gets a structured reason (which of these categories, plus the source slice),
so consumers and the verifier can distinguish "frontend could not parse"
from "language under-specified" from "refused for safety".

## 5. Remaining gaps

- Per-builtin semantics (each `FunctieKind`, each date/duration edge) are
  inventoried but not yet individually specified — that is Phase-1
  IR-SPEC/RUNTIME-SEMANTICS work, steered by the verdicts above.
- The scenario tree's mapping onto the corpus case schema (row 37) is noted
  but not yet designed; it belongs to the Phase-1 corpus workstream.
- DMN was mined at requirements level (hit policies, unary tests, null
  semantics, BKM, DRG); its FEEL function library was not exhaustively
  enumerated — deliberately, since row 15 keeps tables as frontend sugar.
