# wf037-count-cell-source

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm: a
`count` over the element type `Jutter` with the range
`ploeglidmaatschap`/`lid`, whose one argument is a `DimensionCellRead`, the
cell `voorjaar` of `vondstgewicht` of each member. A cell of each member is
the source of a value fold and of the filled count; a count folds the
instances its range reaches and reads no cell, so the cell is no source of
instances.

    WF-037.aggregation-non-source FAIL /rules/rules/0/versions/0/value: a
    count aggregation carries elementType "Jutter" and a DimensionCellRead
    operand: the source of an instance fold is an AttributeReference

The document is `corpus/golden/jutterswet-ploegseizoen` with the one rule
`aantal voorjaarsleden van een ploeg`, its kind `filledCount` replaced by
`count`. Outside the source position the reference under the cell is an
ordinary read, so WF-053 names its `subjectType` as well
(`WF-053.read-other-type`); the pin is the WF-037 arm.
