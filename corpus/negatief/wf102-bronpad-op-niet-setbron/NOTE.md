# wf102-bronpad-op-niet-setbron

Violates **WF-102 (c)** (`spec/IR-SPEC.md` ch. 4.3): a `sourcePath` on a source
that is not a value fold's SET source.

    WF-102 FAIL /rules/resolvedRegels/0/versions/0/value/sourcePath: a
    sourcePath is admissible only as the range of a single-argument VALUE fold's
    set source — a count's range is one step, spelled sourceRole

The document turns the golden's minimum fold into a count: an `AANTAL` whose
source names a ROLE (`geborgen vondst`), anchored on the fold subject as
WF-083 (c) demands of a count source, with the two-step path left in place
as its range. A count's range is one step, spelled `sourceRole`: its source
names the one relation whose role it counts, and a multi-step path is
admissible only as the range of a value fold's set source.

The executors refuse the shape by name, with this rule's sentence, rather
than counting along the path and ignoring the count source, or the other way
round: a range dropped in silence is the misbinding this whole field exists
to close. The history of that refusal: until L2 `ref`, the ASP backend and
the Drools loader all refused the shape; on the L2 union (`bd2c48de`) all
three counted along the path instead (measured by the L2 lens), while this
rule still refused the document; the L2 fix round (2026-09-24) restored the
refusal in all three with the verifier's sentence, and left clause (c)
itself unchanged.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`). Until L2 the
count source was spelled with the unknown-type sentinel on its own `type`;
that annotation left the wire (B13), and the count source is recognised at the
source itself (D9). The clause's wording moved with it: the position it admits
is the set source of a VALUE fold, since under L2 the range stands on every
instance fold (D8). `expect.txt` was added in the L2 fix round (the message
changed at L2, so the witness belongs to the stamp list).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
