Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the duration clause): both
operands of a `duration` are dates; here the first is the text literal
`'gisteren'`. Broken from `corpus/golden/anchor/jutterswet-jutleeftijd`: the
rule `Berekening jutleeftijd` counts from `'gisteren'` instead of from the
Jutter's `geboortedatum`.

    WF-038 FAIL /rules/rules/0/versions/0/value/arguments/0: duration operand
    is text, must be a date

The sentence is spelled once, `duration_operand_refusal` in
`spec/verifier/lib/checks.ml`, and names the builtin's own kind; `ref` and
the ASP backend read it off the operand's static type before any value —
measured 2026-09-25 on this document: `EVALUATION FAULT: duration operand is
text, must be a date` and `TRANSLATION FAULT:` with the same sentence. Until
then `ref` faulted at evaluation (`duration: expected two dates, got
"gisteren" and 2026-02-28`) and the ASP backend in its own words (`duration
operand is not statically a day-precision date`). `expect.txt` pins the arm.
