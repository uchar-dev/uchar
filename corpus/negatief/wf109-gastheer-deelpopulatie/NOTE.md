Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) on its **partial-population
flank**: the host names a STRICT SUBTYPE of the only object the position binds,
so the reading would hold for some objects and fault for the rest.

    WF-109 FAIL /rules/rules/0/versions/0/condition/host: the
    characteristic check is hosted on 'Strandvondst', which no object bound here
    IS: only SOME objects of the subject 'Vondst' are one, so the host would
    hold for some of them and fault for the rest

Broken from `corpus/golden/jutterswet-keurhouder` by adding one declaration and
moving one field: a `Strandvondst` subtype of `Vondst`, and the first rule's
host from `Vondst` to `Strandvondst`. `gekeurd` is inherited, so WF-100 is
satisfied and this rule is the only one speaking.

Why this arm exists, and why it is a FAIL and not a pass. WF-109 asks two
questions of a binding: does it SATISFY the host (`T ⊑ O` — every object it can
hold is one) and does it OVERLAP the host (some object could be both). The
overlap question is what catches `wf109-subtype-overlap`; the satisfaction
question is what catches this one. Counting by overlap ALONE would admit this
document, and then `ref` would answer for a `Strandvondst` and fault for a plain
`Vondst` — a verdict that depends on the case rather than on the wire, which is
precisely the state WF-109 exists to end. So the rule requires exactly one
SATISFYING binding and no second OVERLAPPING one, and a host that only
sometimes applies has zero of the first.

*What is NOT decided here.* Whether a partially-applicable host should read
**false** for the objects it does not cover, rather than being refused, is a
normative question nobody has answered; refusing it leaves that open, and
answering it would settle it by accident.

Falsifiable in three directions; **two turn this case clean and the third
moves it to another rule**:

- put `Vondst` back in the field, or empty it — clean, the golden;
- declare the subject `Strandvondst` instead — clean: host and subject then
  coincide;
- drop the `Strandvondst` declaration — NOT clean. The host then resolves to
  nothing and the document is **WF-034 (q)** instead, a different rule and a
  different report; WF-109 steps aside for an undeclared host.
