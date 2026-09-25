Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the builtin operand typing): an
`absoluteValue` takes one number, and this one takes the text literal
`'vier'`. Broken from `corpus/golden/synth-grensrand`: the value of
`grensproduct van een meting` is replaced by that builtin call.

    WF-038 FAIL /rules/rules/0/versions/0/value: absoluteValue operand is
    text, must be a number

Rood-eerst: until 2026-09-25 the verifier let the document pass
(`verifier: PASS`), no clause of WF-038 reading the operand of the two
numeric builtins. With the golden's case, `ref` faulted at evaluation
(`absoluteValue: expected a number, got "vier"`) and the ASP backend in its
bottom sentence (`expression kind not translated (no corpus case exercises
it)`). Since then both refuse the form in the verifier's sentence, read off
the operand's static type before its value (`EVALUATION FAULT: absoluteValue
operand is text, must be a number`, `TRANSLATION FAULT: absoluteValue operand
is text, must be a number`); the sentence is spelled once, in
`numeric_builtin_operand_refusal` of `spec/verifier/lib/checks.ml`.
`expect.txt` pins the arm.
