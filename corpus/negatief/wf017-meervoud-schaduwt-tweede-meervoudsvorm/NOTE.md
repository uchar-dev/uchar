# wf017-meervoud-schaduwt-tweede-meervoudsvorm

Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a day kind's `plural`
that is one of the thirty spellings of the normative calendar table
(`RUNTIME-SEMANTICS.md` ch. 3.8) — here `secondes`, which is neither an
abbreviation nor the `-en` plural but a SECOND PLURAL FORM of the second,
standing beside `seconden`.

    WF-017 FAIL /model/dayKinds/1/plural: the plural 'secondes' of day kind
    'stormvloeddag' shadows a built-in calendar unit

The class is named by a witness of its own because a reader who sorts the
thirty spellings into singulars, plurals and abbreviations loses exactly the
two second plurals, `secondes` and `millisecondes`. Until L3 the class was
pinned as a key of the separate spelling registry, which left the wire with L3
(`corpus/negatief/README.md`, L3); since then a day kind's further spelling is
its `plural`, and the class is pinned there. The declared-name position keeps
its abbreviation witness, `wf017-dagsoortnaam-schaduwt-kalenderafkorting`.

The document is `corpus/golden/jutterswet-schouwrooster` with the plural of
`stormvloeddag` — a spelling no expression of the host uses — respelled
`secondes`. `expect.txt` pins the arm.
