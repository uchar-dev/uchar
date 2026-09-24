# wf031-dagsoortdefinitie-naamloos

Violates **WF-031** (`spec/IR-SPEC.md` ch. 3.7): the first day-kind
definition carries `dayKind: null`, so it defines no day kind at all — while
`schouwdag` is still declared and now has no definition.

    WF-031 FAIL /rules/dayKindDefinitions/0: day-kind definition without a name;
    it defines no declared day kind and is silently dead

Same clause and same reason as the rule's bounded-window arm: a day-kind
definition nothing consults is dead weight the document does not say is dead.
The name is not decoration here — it IS which day kind is being defined, and
WF-031's own cross ("definition for undeclared day kind") is written against
it, so a null slipped through the very comparison meant to catch a definition
that binds to nothing.

It is deliberately NOT a WF-107 case. That rule holds of the NAMED FORMS,
whose names share one namespace WF-020 keeps unique; a day kind's name lives
in its own namespace, which WF-031 keeps unique itself. Two namespaces, two
rules.

Measured before the arm (2026-09-12): verifier PASS. No answer moved on this
golden's own case — the two day kinds are not decisive there — so this is a
document that says nothing wrong out loud and simply loses a definition; the
sister arm above it exists for exactly that shape.

Falsifiable in one edit: put `schouwdag` back in the name.

Minimal hand-broken variant of `corpus/golden/jutterswet-schouwkalender`.
