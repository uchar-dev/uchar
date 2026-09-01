Violates WF-087 (`spec/IR-SPEC.md` ch. 2.4) at its lower boundary: the
`meldstatus` sum type declares exactly ONE constructor, `Gemeld`, with a
null payload. "All nullary" is a universal quantification, so it holds of
a one-alternative declaration too — a unit type is a nullary keuzetype
and therefore an `Enumeratie` domain with a single value, never a
`SumType`.

This case pins that boundary. Its sibling `wf087-all-nullary-sumtype`
carries two constructors, so a verifier weakened to
`List.length constructoren > 1` still rejected it and the mutant survived
gate 3; against this document that mutant turns the gate red.

A minimal hand-built document.
