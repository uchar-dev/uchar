Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, RESIDUAL-STRUCTURE facet: `Jutter.aanspraken` is declared as a list
of the sum type `oordeel` and the reference that reads it is typed as the
ELEMENT — the same sum type, one `ListType` layer short. Both inline copies
repeat the domain declaration faithfully, so WF-061 has nothing to say.

    WF-091 FAIL /resolvedRegels/0/waarde/type: residual structure
    mismatch: attribute reference 'aanspraken' is typed sumtype 'oordeel'
    [Goedgekeurd of number(fractional, 2 decimals, NIET_NEGATIEF, no
    unit), Verworpen, Uitgesteld], but the declaration it reads from is
    list of sumtype 'oordeel' [Goedgekeurd of number(fractional, 2
    decimals, NIET_NEGATIEF, no unit), Verworpen, Uitgesteld]

Why it has to exist: it is the exact hole the facet order leaves open by
design. The soort facet stands aside wherever either side names a domain —
that discrepancy is WF-089/WF-090's, one namespace, one variant — and the
domain facet looks THROUGH a `ListType` element, so a list and its element
name the same keuzetype and both earlier facets fall silent. Without a
residual facet the engine would be a sieve with a list-shaped hole.

Falsifiable: drop the residual facet from the engine and this case alone
turns clean; the six named facets stay red on their own cases.
