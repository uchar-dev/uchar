Violates WF-037 (`spec/IR-SPEC.md` ch. 4.6; the `BuiltinCall` table of
ch. 4.3): a bucketing builtin without its bucket — a `timeProportionalPart`
call that carries no `granularity`.

    WF-037 FAIL /rules/rules/0/versions/0/value: bucketing builtin
    without a granularity

**Why the facet is required.** `timeProportionalPart` and `dayCount`
divide a timeline into buckets and compute per bucket
(`RUNTIME-SEMANTICS.md` ch. 3.7 and 3.8). The call names the bucket, and
ch. 4.3 requires the facet on exactly these two kinds; without it the node
does not say which division it performs. No witness
pinned this arm before the L4 fix round: no corpus file in the repository's
history carries its sentence.

**No executor names the defect.** Measured 2026-09-24 against the case of the
source golden: the reference interpreter and the ASP backend both refuse the
rule, but with their sentence for a temporal target of another shape (`a
temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape (a
gedurende condition over an own temporal characteristic); no corpus case for
more`), and the Drools backend excludes the rule at transpilation
(`transpiler SKIP`). This rule is the one place that reports the missing
bucket.

**The document.** `corpus/golden/jutterswet-bakenmeester/ir.json` with one field
removed from the value of its rule: `"granularity": "month"`.

Falsifiable, arm-exact: emptying the arm `None, (Tijdsevenredig_deel |
Aantal_dagen_in)` of the granularity match in `walk_expr`
(`spec/verifier/lib/checks.ml`) turns this case clean and nothing else — 703
documents compared over golden, rood and negatief, one verdict moved.
