Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1), its type arm: a
`ContextValue` named `evaluationDate` whose `type` annotation is a
millisecond-precision `Date`, where the name fixes a day-precision `Date`.

    WF-049 FAIL /rules/rules/0/versions/0/value/date: the read's date
    operand is date-ms, must be a day-precision date
    WF-098 FAIL /rules/rules/0/versions/0/value/date/type: context value
    'evaluationDate' is annotated with a type other than the one its name
    fixes: a day-precision Date

**Two rules, two readings.** WF-049 reads the annotation where the
vintage read uses its date operand; WF-098's type arm reads it at the node,
against the type the name fixes. Every position a date is used in has a
rule of its own that reads the operand's type (measured 2026-09-26: the same
twist inside a duration builtin draws WF-038, `duration operands mix date
precisions`), so for `evaluationDate` the arm mostly repeats a finding at
the node itself. It is kept because the type is fixed by the name in every
position, not only in those a position rule covers, and because this
witness is what holds the date half of the arm: without it, relaxing that
half would move no verdict in the corpus.

**Executors.** None reads the annotation. Measured 2026-09-26 with the
source golden's `case.json`: `ref` `oracle: PASS (2 expectations)`, the ASP
backend `clingo: PASS (2 expectations)`; the Drools loader stops earlier, on
the temporal read (`EXCLUDE loader: form not carried by the v0 Drools
backend: Expr:TemporalRead (unknown expression node)`).

**The document.** `corpus/golden/anchor/jutterswet-penningvintage/ir.json`
with the `precision` of its one `ContextValue`'s type changed from `day`
to `millisecond`.

Falsifiable, arm-exact (measured 2026-09-26): let the `evaluationDate` half
of `context_value_type_refusal` in `spec/verifier/lib/checks.ml` accept a
millisecond-precision `Date` and this case is refused for WF-049 alone, which
gate 3 reports as a rejection not for WF-098; the sister
`wf098-evaluation-year-with-decimals` stays refused. `expect.txt` pins the arm.
