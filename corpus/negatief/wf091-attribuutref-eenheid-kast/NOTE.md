Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, unit facet: the reference reads `Jutter.gewicht`, declared with the
unit `kg` of system `Gewicht` (canonical `kg`), but its serialized `type`
spells all three unit NAMES in another case — `KG` of system `GEWICHT`,
canonical `KG`. The conversion FACTOR is 1 on both sides, and every other
facet agrees, so this document differs from its declaration in **case
alone**.

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: attribute
    reference 'gewicht' is typed 'KG' of system 'GEWICHT' (factor 1 to
    'KG'), but the declaration it reads from is 'kg' of system 'Gewicht'
    (factor 1 to 'kg')

Why it has to exist: it pins the BYTE comparison of the three unit names
mechanically. Fold those names case-insensitively — as every other declared
name in the verifier is folded — and this case, and only this case,
passes. That is not a harmless liberty. The target `Jutter.kopie` is
declared in `t`, so the value is CONVERTED on the way in, and a consumer
picks the conversion by reading the reference's own unit names: `ref` and
clingo read the DECLARATION and answer `3.75` for an input of `3750.00`
kg, while the Drools path compares abbreviation and canonical name
case-sensitively, finds no match and falls back to factor one — `3750.00`.
Verifier-clean and ref ≢ Drools, measured on exactly this shape. Name
RESOLUTION is case-insensitive (WF-001's domain, and the DOMAIN facet of
this very rule stays ci, pinned by `jutterswet-dossierspelling` and
`jutterswet-vaarwaterspelling`); copy FIDELITY is a different question, and
this is the case that keeps the two apart.

Falsifiable, facet-exact and arm-exact: neuter the unit facet in the
identity engine (`use_site_identity`) and this case degrades to `residual
structure mismatch` — still WF-091, still red, because the backstop makes
the same split — while disabling the `Attribuut_ref` arm turns it clean
together with the other `wf091-attribuutref-*` cases and leaves the binder,
parameter, cell-read, fold-source and write-target cases red.
