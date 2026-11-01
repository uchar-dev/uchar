Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), unit clause: the rule `teruggerekende breedte` writes the read of `oppervlakte`,
declared in `m^2`, into an attribute in `m`. The dimensions differ, so there is no restatement
(`RUNTIME-SEMANTICS.md` ch. 2.6, rules 1 and 6).

Falsifiable: divide the read by `lengte`, as `jutterswet-oppervlak` does, and the document is verifier-clean.
