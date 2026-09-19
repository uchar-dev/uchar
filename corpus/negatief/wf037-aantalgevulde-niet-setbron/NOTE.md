# wf037-aantalgevulde-niet-setbron

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in the filled count's
SET-SOURCE arm: the single argument of an `AANTAL_GEVULDE` must be a set
reading, and this one is an ordinary read of the subject's own attribute.

    WF-037 FAIL /resolvedRegels/0/waarde: the filled count's single
    argument MUST be a set reading: this kind reads a VALUE per
    contributor, so its source is the set source (ch. 4.3, `set: true`) —
    a non-set source is the subject's own read, whose filled count is 0 or
    1, and a bare role source hands the fold instances instead of values

**The one WF-037 arm that demands a form instead of forbidding one.** It has
to: the non-set source position is real and occupied — the ch. 3.6 temporal
total reads the subject's own timeline there, and the row-1 wart hands the
fold the subject's linked instances — but neither reading has a filled count.
The own read has one value, so its filled count is 0 or 1: a question no
author asks and no executor should have to answer. `ref` already refuses the
bare role source at EVALUATION (`value aggregation over a bare role source`);
this arm puts the fence on the wire, where it can be checked before anything
runs.

The document. Jutter declares a kg attribute `vondstgewicht` of its own, and
the count's argument reads it with `set: false`, anchored on the fold
subject as WF-083 (c) demands of a non-set source. The `bronRol` is absent —
a role binding beside a non-set source is WF-047's defect and would have
masked this one. `elementObjecttype` stays `Vondst`. The reading is the only
defect and the verifier reports one fault.

Rood-eerst, measured on this document. With the wire enum landed and the
arms absent (verifier md5 `220741bf2cee253d71ca6b13b8ad1402`):
`verifier: PASS`. With the arms (md5 `d970a571d0ecbc88de4bea5b14da9fc7`):
the single rejection above.

Falsifiable, arm-exact: return `[]` from the arm's `Some _, _` case in
`spec/verifier/lib/checks.ml` and leave everything else untouched. Swept
over golden + rood + negatief + parity (695 documents) exactly this document
moves — it turns clean again — and no other document changes a verdict or a
report line.
