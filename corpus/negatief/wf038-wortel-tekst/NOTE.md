Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the builtin operand typing): a
`squareRoot` takes one number, and this one takes the text literal
`'vier'`. Broken from `corpus/golden/synth-grensrand`: the value of
`grensproduct van een meting` is replaced by the root, under the rounding a
root always carries (0 decimals, `halfAwayFromZero`).

    WF-038 FAIL /rules/rules/0/versions/0/value/base: squareRoot operand is
    text, must be a number

Rood-eerst: until 2026-09-25 the verifier let the document pass
(`verifier: PASS`). With the golden's case, `ref` faulted at evaluation
(`squareRoot: expected a number, got "vier"`) and the ASP backend refused the
rounded root outright (`rounded root is not translated (no corpus case)`).
Since then both refuse the form in the verifier's sentence first
(`EVALUATION FAULT: squareRoot operand is text, must be a number`,
`TRANSLATION FAULT: squareRoot operand is text, must be a number`).
`expect.txt` pins the arm.
