# C2 — the articulated eerste/laatste fold, ORDERED as v0 computes it

Goal: stress-test instance-fold sub-form C2: an articulated value fold
(EERSTE/LAATSTE over ≥2 own scalar attributes) carrying a vestigial
elementObjecttype falls into ref's "unsupported instance-fold source
shape" arm (eval.ml:824-828 demands element_objecttype = None). AND the
exploration found a latent silent divergence waiting behind it: ref's
Arith.agg_eerste/agg_laatste (arith.ml:433-434) are POSITIONAL (first
filled in list order) where v0 production is ORDERING ("<"/">" — "de
eerste van 5 en 3" is 3, not 5). Mirror-then-diverge decides this
without a new maintainer question: v0's ordering semantics is the
mirror. Deliver: (1) fix agg_eerste/agg_laatste to the ordering fold
(empty operands skipped; all-empty → empty — verify the exact v0
empty rule in the runtime and pin it); FIRST verify no existing corpus
case pins the positional behaviour (if one does, STOP and report — that
is a divergence discovery, not a rewrite); (2) widen the articulated
arm to match on argument SHAPE instead of element absence; (3) the
matching clingo translation (min/max over the argument terms with the
empty-skip discipline); (4) RUNTIME-SEMANTICS worked example (mixed
values, an empty operand, the all-empty corner); (5) golden with
hand-computed cases (a date pair and a numeric pair; the all-empty
corner), through both executors.

Acceptance:
- Red-first: the verifier-clean articulated repro faults in today's
  ref (quoted); after the change both executors replay the golden and
  agree (gates 5–6); the ordering semantics is pinned by a case where
  positional and ordering answers DIFFER.
- All nine gates green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md` (worked example),
`ref/` (arith.ml, eval.ml), `backends/clingo/`,
`spec/ASP-TRANSLATION.md`, `corpus/golden/jutterswet-*`.
Out of scope: sub-form C1 (lands earlier in this chain), new WF rules
(the shape is already schema-legal; add one only if the verifier must
reject a genuinely meaningless combination — justify it),
`spec/CONSTRUCTS.md` (serial at merge).

Result: landed whole. Red-first on both legs: the verifier-clean
articulated repro faulted in ref ("EVALUATION FAULT: unsupported
instance-fold source shape") and the element-null variant showed the
positional answers (peildatum = 2026-06-21 where ordering pins
2026-05-01; laagloon = 5 where ordering pins 3). Verified first that
nothing pinned the positional reading: no EERSTE/LAATSTE anywhere in
the corpus wire, no agg_eerste/agg_laatste ref test — so the ordering
fold is the mirror (v0's exec-model and DRL paths both fold '<'/'>',
null skipped, all-null -> null; ties take the later operand, which is
unobservable after unit restating). agg_eerste/agg_laatste are now the
ordering fold (= agg_min/agg_max); eval dispatches the listed fold on
argument shape (>= 2 operands), the vestigial elementObjecttype
ignored; clingo folds #min/#max over a per-site helper predicate with
the empty-skip join and a definedness atom (all-empty stays leeg as
absence), fenced: one comparable family, identical-or-adopting units,
datum-tijd embargoed, symbolic operands faulted. RUNTIME-SEMANTICS
ch. 1.2e names the ordering fold (worked examples verbatim in
ref/test); ASP-TRANSLATION S8 + S19 updated; golden
jutterswet-peildatum (date pair + numeric pair; case1 the
ordering-vs-positional DIFFER pin, case2 empty-skip, case3 all-empty)
replays through both executors and refutes --noop. Nine gates green.
Deliberately left: listed MINIMUM/MAXIMUM keep their loud clingo
fault (same encoding would serve, no corpus case — recorded in S8/S19);
no new WF rule (the shape is schema-legal and now executes; nothing
meaningless to reject); the mixed-unit result-unit nuance of v0's
Eenheid1 restating is unobservable in ref's value model and fenced
identical-units in clingo.

Review fix (2026-08-25): two corrections. (1) The numeric half is NOT
a v0 mirror but a v1 typing widening: v0's `Typering.datumAggregatie`
(ExpressieBouwer EERSTE/LAATSTE, Severity.ERROR) admits only date
expressions — "de eerste van 5 en 3" never compiles in v0 production
(engine refuses ERROR models); only Onbekend-typed operands reach the
type-generic runtime fold. RS ch. 1.2e now names this a tracked
deviation, ratification pending; the CONSTRUCTS delta (`FunctieKind`
aggregations row) is the serial merge update — maintainer decides:
ratify the widening or cut the numeric rules/expectations from
`jutterswet-peildatum`. (2) The shape-dispatch arm in ref matched ALL
fold kinds: a multi-operand SOM/TOTAAL/AANTAL with a stamped element
silently computed a list fold v0 never exhibits (v0's accumulate
consumes the element, reads only argument 0). Red-first: the SOM probe
(laagloon rule, kind SOM, bergloon=5/meldloon=3, expected 8) passed
silently pre-fix ("oracle: PASS (2 expectations)"); the arm is now
narrowed to v0's listed family Eerste/Laatste/Minimum/Maximum
(isGelisteFold) and the probe faults loudly again ("unsupported
instance-fold source shape"). Also mirrored the listed-fold tie rule
exactly (ties take the later operand — value-equal, unobservable
today).
