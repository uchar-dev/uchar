# wf017-plural-shadows-calendar-unit

Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a day kind's `plural`
that is one of the thirty spellings of the normative calendar table
(`RUNTIME-SEMANTICS.md` ch. 3.8) — here `dagen`, the plural of the day.

    WF-017 FAIL /model/dayKinds/1/plural: the plural 'dagen' of day kind
    'stormvloeddag' shadows a built-in calendar unit

A duration's unit is recognised by spelling through the calendar table first,
and "a spelling outside the table is a declared day kind". A day-kind plural
inside the table states the opposite for the same spelling, so the document
contradicts itself whichever reading a consumer takes. Since L3 a day kind
carries its spellings on its own node, `name` and `plural` (card
`20260924-l3-typesysteem`); until then this arm read the keys of a separate
spelling registry, and its three witnesses left the corpus with that registry
(`corpus/negative/README.md`, L3). This is the plain class of the three: the
`-en` plural of a calendar unit.

The document is `corpus/golden/jutterswet-schouwrooster` with the plural of
`stormvloeddag` — a spelling no expression of the host uses — respelled
`dagen`. `expect.txt` pins the arm.
