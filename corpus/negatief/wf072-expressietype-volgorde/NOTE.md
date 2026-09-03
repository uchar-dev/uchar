Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the `AttribuutRef` reading
`dossier` carries as its `type` an inline copy of domain `dossier` with the
declared fields in REVERSED order (`telling`, `waarde` where the declaration
says `waarde`, `telling`). Same domain name, same field names, same datatypes
— only the order drifted, and WF-006 order is normative.

The arm this case pins: the EXPRESSION layer. `wf072` walked the model layer
only, so a drifted copy on a node's `type` was verifier-clean — while both
executors read that copy: ref resolves a field by NAME and clingo encodes the
struct term POSITIONALLY out of the inline list. Measured on an anchor golden
with exactly this one node changed: `verifier: PASS`, `oracle: PASS (8
expectations)`, and clingo `MISMATCH: J1.samenvatting = [dossier(503,2)],
expected dossier(12550,2)` — a **wrong answer** on a verifier-clean,
schema-valid document, of the same class WF-089/WF-090 close for domain names.

The attribute declaration repeats the declaration faithfully, so the model
layer stays silent: only the expression position fires.

A minimal hand-built document (the `wf074-unknown-field` skeleton with a
declared field and a reordered use-site copy).
