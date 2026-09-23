Violates WF-004 (`spec/IR-SPEC.md` ch. 2.2): an attribute whose `dimensions`
list is present and empty.

    WF-004 FAIL /objecttypen/Jutter/attribuutDimensies/aantal aangemelde vondsten: dimensioned attribute with an empty dimension list

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`:
the golden unchanged, except that its one attribute, `aantal aangemelde
vondsten`, carries `"dimensions": []`. An absent list means a scalar attribute;
an empty one would be a second spelling of the same thing, so a present list
must be non-empty. `spec/schema/uchar-ir-v1.schema.json` refuses the empty list
(`minItems: 1`) and `spec/proposals/v1-migratie/m1.py` refuses to migrate a v0
document that carries one instead of dropping it, which would have turned this
violation into a clean scalar attribute. The negatief zone is not schema-gated,
so the verifier's own arm is what this witness holds in place.
