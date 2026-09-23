Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4): the sum-type twin of
`wf089-attribuutref-enum`. The `Match` scrutinee reads `beschikking` of
`Jutter`, declared with the sum domain `beschikking` (`Toegekend(bedrag)`,
`Afgewezen`, `Aangehouden`), but its `type` names the second declared sum
domain `oordeel` (`Goedgekeurd(bedrag)`, `Verworpen`, `Uitgesteld`) and
the arms cover `oordeel` exactly.

    WF-089 FAIL /rules/resolvedRegels/0/waarde/scrutinee/type: attribute reference
    'beschikking' is typed with keuzetype domain 'oordeel', but the
    declaration it reads from names domain 'beschikking'

Why it has to exist beside the enumeration case: the sum side is
pre-existent rather than new — WF-061 pins a `SumType` use site against
its declaration only at attribute and parameter DATATYPES, never at an
expression `type`, and WF-063 reads the domain the scrutinee names. So the
document was verifier-clean; ref faulted (`match has no arm for
constructor "Toegekend"`), clingo left `uitkering` underived where 1275.50
was expected, and Drools auto-excluded the `SumType` datatype.

Falsifiable through the same `Attribuut_ref` arm as the enumeration case;
`keuzetype_domain` reads the `SumType`'s `domain` where the enumeration
case reads the `Enumeratie`'s `domein`, so a mutant that drops the
`Sum_type` line of that helper passes this case and keeps the enumeration
one red.
