# wf102-bronpad-leeg

Violates **WF-102 (b)** (`spec/IR-SPEC.md` ch. 4.3): the range is present
and empty.

    WF-102 FAIL /rules/resolvedRegels/0/waarde/bronPad: a bronPad carries at least
    TWO steps, found 0 — the one-step range is spelled bronRol

The empty list is not "no range": it is a stated range that reaches nothing,
and a consumer reading it as "walk no steps" would fold the SUBJECT itself as
its own element. The field is loaded verbatim — empty included — precisely so
this is a violation the rule can cite rather than a load failure with no rule
to name.
