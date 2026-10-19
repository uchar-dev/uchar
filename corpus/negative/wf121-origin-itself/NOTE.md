# wf121-origin-itself

Violates WF-121 (c) (`spec/IR-SPEC.md` ch. 2.13), its first arm: an origin's
`from` does not name its own declaration.

    WF-121 FAIL
    /model/objectTypes/0/attributes/4/origin/from: the origin of the
    attribute 'berekende waarde' of 'Vondst' names that declaration itself

The document is `corpus/golden/jutterswet-opgave-bedrag` with the `from` of
the origin of the attribute `berekende waarde` of `Vondst` naming
`berekende waarde` of `Vondst`: a declaration that comes from itself, which
resolves under (b) and points a consumer nowhere. The verifier reports it in
one line, the arm `expect.txt` pins; the cycle arm of the same clause is
`wf121-origin-cycle`.
