Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that `count`
demands a count source (since the L2 landing): the count ranges over the
`Vondst`en that `vondstaanmelding` links to the subject `Jutter`, and its
source reads `gewicht`, an attribute of the element type. A count folds the
contributors themselves; a source that reads a value per contributor hands it
something it does not consume.

    WF-093 FAIL /rules/rules/0/versions/0/value/arguments/0: the source
    of this count aggregation reads 'gewicht', a member of the element type
    'Vondst': a count folds INSTANCES, so its source is a count source naming
    the relation's role, never a value read per contributor

**Successor at L2 (2026-09-24), stamp: vervangen, named after the rule it now
pins.** This directory replaces `wf096-aantal-setbron`, whose defect was the
set reading under `count` (WF-096). The set reading left the wire with L2
(B11), and the count's half of WF-096 went into WF-093, which since L2 keys on
the source itself: a count source names a role of the fold subject and no
attribute of the element type, and this source names an attribute. The
document is the migration's translation (`spec/proposals/v1-migratie/m2.py`).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
