# wf102-bronpad-op-niet-setbron

Violates **WF-102 (c)** (`spec/IR-SPEC.md` ch. 4.3): a `bronPad` on a source
that is not the SET source.

    WF-102 FAIL /resolvedRegels/0/waarde/bronPad: a bronPad is admissible
    only on the SET source of a single-argument instance fold — the position
    bronRol stands at

The document turns the golden's minimum fold into the row-1 wart: an `AANTAL`
whose source names a ROLE (`geborgen vondst`) and carries the unknown sentinel
on its own `type`, anchored on the fold subject as WF-083 (c) demands of a
non-set source, with the two-step path left in place. The wart hands the fold
the subject's LINKED INSTANCES rather than a value per contributor (WF-093),
so there is nothing for a range to range over — the count is subject-anchored
by construction, and the field would be read by no consumer.

`ref` and the ASP backend fault the shape by name rather than folding the path
and ignoring the wart, or the other way round: a range dropped in silence is
the misbinding this whole field exists to close.
