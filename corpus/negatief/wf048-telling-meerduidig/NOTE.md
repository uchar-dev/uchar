Violates WF-048 (`spec/IR-SPEC.md` ch. 4.3): the count carries no
`bronRol`, and **two** fact types (`muntvondst` and `muntbewaring`)
relate `Jutter` to `Munt` — the connecting relation is not uniquely
derivable from the type pair, so the bindingless instance fold is
ill-formed (the lifted ch. 4.1 runtime fault of
`spec/RUNTIME-SEMANTICS.md`).

The sibling `wf048-ambiguous-connecting-relation` pins the same rule on
an ALLOCATION; this one pins its `site:"instance fold"` arm, which no
negatief case reached before.

The same document is **also** WF-034-ill-formed, and that is the point
of writing it this way: the element word `Munten` names the object
type's own declared plural, so it resolves to no declared role and the
ch. 4.3 type-pair fallback is what would have to stand in — and with
two candidates it cannot. The two rules fire on one sentence, one at
the source argument and one at the fold. The WF-034 citation is the
sibling `wf034-ambigu-typenpaar`'s; this directory declares WF-048.

The accepting twin is `corpus/golden/jutterswet-muntpeiling`, whose
`het aantal Munten van de Jutter` is the same sentence over a uniquely
connected pair: there the fold goes out bindingless and verifier-clean
and all three executors derive the one connecting role. The document
here is that golden's source with a second `Jutter`–`Munt` fact type
added, emitted by the RegelSpraak frontend — so the refusal stands on
what the frontend really produces, not on a hand-built shape.
