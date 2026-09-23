# wf102-bronpad-op-niet-setbron

Violates **WF-102 (c)** (`spec/IR-SPEC.md` ch. 4.3): a `bronPad` on a source
that is not a value fold's SET source.

    WF-102 FAIL /rules/resolvedRegels/0/waarde/bronPad: a bronPad is
    admissible only as the range of a single-argument VALUE fold's set source
    — a count's range is one step, spelled bronRol

The document turns the golden's minimum fold into a count: an `AANTAL` whose
source names a ROLE (`geborgen vondst`), anchored on the fold subject as
WF-083 (c) demands of a count source, with the two-step path left in place
as its range. A count's range is one step, spelled `bronRol`: its source
names the one relation whose role it counts, and a multi-step path is
admissible only as the range of a value fold's set source.

`ref` and the ASP backend fault the shape by name rather than folding the
path and ignoring the count source, or the other way round: a range dropped
in silence is the misbinding this whole field exists to close.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`). Until L2 the
count source was spelled with the unknown-type sentinel on its own `type`;
that annotation left the wire (B13), and the count source is recognised at
the source itself (D9). The clause's wording moved with it: the position it
admits is the set source of a VALUE fold, since under L2 the range stands on
every instance fold (D8).
