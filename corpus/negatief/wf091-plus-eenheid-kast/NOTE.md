# wf091-plus-eenheid-kast

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, "Unit identity is structural,
never convertible") at a **composite** position, `Binary`/PLUS arm, unit
facet, with a lie in CASE alone. Both operands read `Jutter.gewicht`,
declared in `kg` of system `Gewicht` (canonical `kg`, factor 1), so ch. 2.6
derives `kg` of `Gewicht` for the sum. The node's own `type` carries `KG` of
system `GEWICHT` (canonical `KG`, factor 1): the same unit under WF-001's
case-insensitive name resolution, a different spelling of its three names.

    WF-091 FAIL /rules/resolvedRegels/0/versions/0/value/type: unit mismatch:
    the PLUS composite is typed 'KG' of system 'GEWICHT' (factor 1 to 'KG'), but
    the type derived from its operands is 'kg' of system 'Gewicht' (factor 1 to
    'kg')

The rule compares the abbreviation, the system and the canonical unit byte
for byte: an annotation repeats the unit it derives from literally, case
included, because a consumer that reads those names literally to pick a
conversion would otherwise diverge on a spelling that differs in case alone.
Name resolution stays case-insensitive (WF-001); copy fidelity is the
question here.

Why it has to exist. Until L2 the byte-exact comparison was pinned on a
reference's own type, `wf091-attribuutref-eenheid-kast`: a `KG`/`GEWICHT`/`KG`
copy of a `kg`/`Gewicht`/`kg` declaration. L2 (B13) took the reference's own
type off the wire and that witness left with it, so from the L2 union
(`bd2c48de`) on nothing pinned the comparison any more. Since L2 a unit name
is still written on a derived position, the annotation of a composite, and
this witness puts the same lie there.

Falsifiable, facet-exact: let `eenheid_identical` in
`spec/verifier/lib/checks.ml` compare the three names in their WF-001 form
(`norm`) instead of byte for byte, and this case turns clean. Measured in the
L2 fix round (2026-09-24) over golden, rood, negatief and the parity zone (715
documents): that mutation changes exactly one report, this one;
`wf091-binair-eenheid`, whose lie is a convertible unit (`t`), stays rejected.

The document is `wf091-binair-eenheid` with the annotation's unit changed from
`t` (factor 1000) to `KG`/`GEWICHT`/`KG` (factor 1). `expect.txt` pins the arm.
