Violates WF-037 (`spec/IR-SPEC.md` ch. 4.6; the `arguments` row of the
`Aggregation` table in ch. 4.3): a value fold with one operand — a listed
`first` without `elementType` whose second operand is gone.

    WF-037 FAIL /rules/rules/0/versions/0/value: value fold with
    fewer than two operands

**Why one operand is no fold.** An aggregation folds either the instances its
range reaches — an instance fold, which carries `elementType` and a range —
or the operands it lists, and the listed form carries two or more (ch. 4.3;
WF-037: "a value fold MUST carry ≥ 2 arguments"). This node carries no element
type and one operand, so it is neither form. The two counts stand outside this
clause and carry arity arms of their own. No negatief document reported this
arm before the L4 fix round.

**One executor answers, two refuse.** Measured 2026-09-24 against the three
cases of the source golden: the reference interpreter answers every case and
matches the expectations pinned for the golden (3, 3 and 5) — in each case the
morning high water is the earlier or the only one, so the one operand left
carries the pinned answer; the ASP backend refuses at translation
(`TRANSLATION FAULT: datum-tijd here is not the pair term's arithmetic. …`),
and the Drools backend excludes the rule at transpilation
(`transpiler SKIP`).

**The document.** `corpus/golden/jutterswet-hoogwaterpaar/ir.json` with the
second operand (`zijn avondhoogwater`) of the `first` in its first rule's
value removed.

Falsifiable, arm-exact: emptying the value-fold arity clause in `walk_expr`
(`spec/verifier/lib/checks.ml`) turns this case clean and nothing else — 706
documents compared over golden, rood and negatief, one verdict moved.
