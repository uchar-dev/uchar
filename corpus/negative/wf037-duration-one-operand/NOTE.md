Violates WF-037 (`spec/IR-SPEC.md` ch. 4.6; the `BuiltinCall` table of
ch. 4.3): a builtin of a two-date kind with one operand — a `duration` that
keeps `zijn aankomst` and has lost `zijn vertrek`.

    WF-037 FAIL /rules/rules/0/versions/0/value: builtin arity 1,
    expected 2

**Why the count is fixed.** `duration` and `absoluteDuration` count whole units
between two dates, from the first operand to the second (`RUNTIME-SEMANTICS.md`
ch. 3.8; WF-037: "two dates for `duration` and `absoluteDuration`"). With one
date there is no span to count. No witness pinned this arm before the L4 fix
round: no corpus file in the repository's history carries its sentence.

**The executors refuse it.** Measured 2026-09-24 against the two cases of the
source golden: the reference interpreter faults at evaluation and the ASP
backend at translation, both with `duration expects two arguments`
(re-measured 2026-09-25; until L5 both named the kind `tijdsduur`), and the
Drools backend excludes the rule at transpilation (`transpiler SKIP`).

**The document.** `corpus/golden/jutterswet-getijdenpaar/ir.json` with the
second argument of the `duration` in its first rule's value removed; its
`source.snippet` still reads "de tijdsduur van zijn aankomst tot zijn vertrek in
hele uren".

Falsifiable, arm-exact: emptying the two-date branch of the builtin-arity
clause in `walk_expr` (`spec/verifier/lib/checks.ml`) turns this case clean
and nothing else — 706 documents compared over golden, red and negative, one
verdict moved.
