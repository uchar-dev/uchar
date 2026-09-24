# wf017-meervoud-schaduwt-kalenderafkorting

Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a day kind's `plural`
that is one of the thirty spellings of the normative calendar table
(`RUNTIME-SEMANTICS.md` ch. 3.8) — here `wk`, the abbreviation of the week.

    WF-017 FAIL /model/dayKinds/1/plural: the plural 'wk' of day kind
    'stormvloeddag' shadows a built-in calendar unit

The calendar table carries three classes of spelling: the singular, the
abbreviation and the plural. `wf017-meervoud-schaduwt-kalendereenheid` pins the
plural class (`dagen`) and `wf017-meervoud-schaduwt-tweede-meervoudsvorm` the
second plurals (`secondes`); this witness pins the abbreviation class on the
`plural`, which until L3 the registry-key witness
`wf017-eenheidsleutel-schaduwt-kalenderafkorting` held and which left the
corpus with the registry (`corpus/negatief/README.md`, L3; card
`20260924-l3-typesysteem`).

The document is `corpus/golden/jutterswet-schouwrooster` with the plural of
`stormvloeddag` — a spelling no expression of the host uses — respelled `wk`.
`expect.txt` pins the arm.
