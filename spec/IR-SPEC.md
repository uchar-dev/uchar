# uchar IR — Specification

*Status: v0.0 — skeleton only. **Nothing in this file is normative yet.**
Normative content lands in Phase 1 (see `PLAN.md` §6). Authority today lives
in `regelspraak-java`: the spec-§-citing javadoc of `ir/` and `model/`, and
the decision records in `docs/grammar-divergences.md`.*

## Method: mirror, then diverge

- **v0** is the faithful JSON dump of `regelspraak-java`'s in-memory IR
  (`Resolver --ir`, to be built) at a pinned commit — warts included.
- **v1** is what this document specifies: it deviates from v0 deliberately
  where the founding review (`IR-REVIEW.md`) flagged Drools leakage
  (bean-field names → role bindings, salience → declared stratification,
  `bron` string → structured source references, …).
- Every deviation is a tracked delta with a migration note, indexed in the
  delta column of `CONSTRUCTS.md` — never a silent rewrite. The delta list
  doubles as the Phase 0–4 roadmap.

## Conventions (binding once content lands)

- Normative keywords per RFC 2119 (MUST/SHOULD/MAY).
- Every MUST carries an ID `WF-###` and comes with: a check in
  `spec/verifier/`, at least one passing case in `corpus/golden/`, and one
  deliberately failing case in `corpus/negatief/`.
- Every semantics clause carries a worked example with concrete expected
  output; those examples are the reference interpreter's test suite (`ref/`),
  verbatim.
- Every node is marked `layer: core` or `layer: dialect(regelspraak)`; the
  dialect→core lowering is itself specified and tested.
- **A spec sentence without a machine-checkable artifact is not done.**
- Versioning: semantic-versioned from the first normative release; v0.x while
  the mirror-then-diverge phase runs.

## Planned structure

1. **Scope and conformance** — what a conforming producer (frontend) and
   consumer (backend/interpreter) must do; the `Onopgelost` contract
   (honest inexpressibility is a first-class, permanent part of the
   interface).
2. **The model layer** — object types, characteristics (kenmerken), fact
   types and roles (symbolic role bindings), parameters, domains, datatypes
   and units, dimensions, timelines.
3. **The rule layer** — assignment (one node; presentation tag for
   berekening/gelijkstelling verbalisation), characteristic attribution,
   object creation, fact creation, allocation (verdeling), consistency,
   uniqueness (incl. union form), day-kind definitions, starting-point
   determinations.
4. **The expression and condition layer** — typed expression trees,
   comparison/predicate forms, quantified compounds, aggregation with
   filters and dimension selections.
5. **Time** — rule-version validity, timelines in the type system, parameter
   vintages; the single-axis decision (valuation = calculation = vintage
   selection) stated explicitly, with the named extension point for a
   knowledge-time axis.
6. **Defaults and defeasibility** (Phase 4) — the adopted default calculus:
   prioritised defaults, conflict detection, the inconsistency channel.
7. **Serialization** — the JSON form; canonicalization; round-trip
   requirements. Schema in `schema/`.
8. **Well-formedness rules** — the WF index (each rule: statement, verifier
   check, positive and negative corpus case).
9. **The RegelSpraak dialect** — dialect-only constructs (e.g. the §9.10
   rule group) and the specified lowering to core.
10. **Versioning policy.**

## WF index

*(empty — first entries land with the v0 mirror)*
