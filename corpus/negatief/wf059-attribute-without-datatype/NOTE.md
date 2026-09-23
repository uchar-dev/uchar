Violates WF-059 (`spec/IR-SPEC.md` ch. 2.2): a declared attribute carries no
datatype — the Attribute node `vrij` of object type `Register` has
`"datatype": null`.

    WF-059 FAIL /model/objectTypes/0/attributes/2/datatype: declared attribute carries no datatype

A minimal hand-broken variant of the model of
`corpus/golden/anchor/jutterswet-sluitregister/ir.json` (the object type only;
no rules).

**Rewritten at M1 (2026-09-23), stamp: herschreven.** Until M1 this witness was
a v0 document whose `attributen` map held `"vrij": null`. The migration script
(`spec/proposals/v1-migratie/m1.py`) refuses a null datatype on purpose, so the
M1 form was written by hand: the same object type with its members as
Attribute and Characteristic nodes, and the one field that makes it a witness,
`datatype`, still null. `spec/schema/uchar-ir-v1.schema.json` admits null
there (`Attribute.datatype`), so the document loads — and validates — and the
verifier refuses it on WF-059 arm 1, not with a load error. That is the point
of keeping it: a datatype-less attribute is a semantic defect in a loadable
document. Arms 2 and 3 of the rule (a time-dependent member without a
granularity) left the wire at M1 together with their witnesses; see
`corpus/negatief/README.md`, "Vervallen bij de v1-migratie".
