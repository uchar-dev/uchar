# wf040-fold-source-cell-label

Violates WF-040 (`spec/IR-SPEC.md` ch. 4.3): the source of a filled count
over the members of `ploeglidmaatschap`/`lid` is a cell of each member, and
its label `winter` is no label of the dimension `seizoen` of `vondstgewicht`.
The labels of a cell as a fold source are judged as those of every cell.

    WF-040.misplaced-label FAIL /rules/rules/0/versions/0/value/arguments/0:
    label 'winter' at position 1 is not a label of dimension 'seizoen', the
    dimension declared at that position

The document is `corpus/golden/jutterswet-ploegseizoen` with the one rule
`aantal voorjaarsleden van een ploeg`, the label `voorjaar` of its source
replaced by `winter`.
