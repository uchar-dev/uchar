Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at the **characteristic
check's host**: the check is hosted on `Jutter` while the only object the
position binds is the subject, a `Vondst`. The host names the object the check
lives on, and here it names none.

    WF-109 FAIL /rules/resolvedRegels/0/versions/0/condition/eigenaar: the
    characteristic check is hosted on 'Jutter', which no object bound here can
    be: this position binds the subject 'Vondst'

Broken from `corpus/golden/jutterswet-keurhouder` by moving one field and
adding one declaration: the first rule's `eigenaar` from `Vondst` to `Jutter`,
and `gekeurd` onto `Jutter` as well. The second declaration is the point of the
case. Without it the document is also WF-100 — the host declaring no such
characteristic — and the two rules would report together; with it WF-100 is
satisfied and **this rule is the only one speaking**, which is what makes the
witness about the object half and not the declaration half. Everything else is
that golden verbatim.

Why it has to exist. Until 2026-09-19 a document of exactly this shape was
verifier-clean and answered by nobody: `ref` and the ASP backend faulted on a
host they had no object for (the superseded hosting refusal of
`spec/RUNTIME-SEMANTICS.md` ch. 5.8b), and the Drools backend dropped the whole
rule as a transpiler skip. One verifier-clean document, three executors, no
answer. WF-109 moves that verdict to the verifier, which is what lets the forms
that DO name a bound object be carried by all four.

Falsifiable in three directions, each turning this case clean:

- put `Vondst` back in the field, or empty it — the golden, where host and
  subject are one object;
- take `gekeurd` off `Jutter` again — the document is then WF-100, a different
  rule and a different report (`wf100-eigenaar-verklaart-kenmerk-niet`);
- give `Vondst` a supertype that `Jutter` also has, and put THAT type in the
  field — the subject then satisfies the host extensionally (ch. 2.2) and the
  check reads the subject, as an absent host does.

Arm-exact: this case exercises the ZERO flank at a ONE-object position. The
TWO flank is `wf109-gastheer-ambigu-zelfgetypeerd`, the two-object position is
`wf109-gastheer-derde-type`, and the two scope fences are
`wf109-doelgastheer-samengesteld` and `wf109-doelgastheer-genavigeerd`.
