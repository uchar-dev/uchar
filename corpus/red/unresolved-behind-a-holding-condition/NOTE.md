# unresolved-behind-a-holding-condition

The document of `corpus/golden/unresolved-behind-a-failing-condition`, with V1
actief. The value of "waardering van een vondst" is reached, and so are the
second members of the conditions of "bijzondere vondst" and "zware vondst": an
`Unresolved` expression, an `Unresolved` condition and an `Unresolved` operand
of a comparison. Each is a fault, EF-019 (WF-044, `spec/RUNTIME-SEMANTICS.md`
ch. 5.7b); the case pins no parameter, since the `position` of the row is
emitted by no executor.

Every executor faults where it evaluates the node: `ref` in its evaluation,
the ASP backend at solving (a `viol/2` witness whose body is the reach of the
node), and the Drools backend in its red run.
