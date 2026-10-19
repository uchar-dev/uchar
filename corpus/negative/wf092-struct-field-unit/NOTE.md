Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **canonical-without-factor**
half, at a **struct field's datatype**: the unit `km` of the field `afstand`
names `canonical: "m"` and carries no `factorToCanonical`. Broken from
`corpus/golden/jutterswet-dossierspelling` by one move: a struct domain
`meetstaat` is added, with one numeric field `afstand` in that unit. No rule
reads the domain, so no use site copies the unit and WF-072 (struct identity)
and WF-073/WF-074 (field datatypes) have nothing to compare; the half-filled
pair is the document's only defect. Put on the golden's own `dossier`
struct instead (the domain and the two attribute copies), the rules' struct
literal and field accesses, which do not carry the unit, are reported under
WF-072, WF-073 and WF-074 (measured).

    WF-092 FAIL /model/domains/1/datatype/fields/0/datatype/unit: unit 'km'
    names canonical unit 'm' but carries no conversion factor into it: a
    half-filled conversion pair is not a unit (both fields, or neither)

The position is the struct arm of the datatype walk (`check_datatype_units`
in `spec/verifier/lib/checks.ml`). Falsifiable: with that arm returning no
violation this case turns clean, and it is the only one of the 438 negative
cases that does (measured 2026-09-25): no other witness has a unit inside a
struct field.

The pointer is recorded as measured, because gate 3 compares the rule and the
arm, not the pointer. Until 2026-09-25 the verifier cited this position as
`/model/domains/1/datatype/velden/afstand/unit` — the field by its name under
a segment the document does not have; the cited path now resolves in the
document. `expect.txt` pins the arm.
