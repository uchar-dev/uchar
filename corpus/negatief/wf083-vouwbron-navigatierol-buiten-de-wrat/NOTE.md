# wf083-vouwbron-navigatierol-buiten-de-wrat

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), the **scope** half of the
self-denotation clause: a `navigatieRol` on a fold source that is NOT the v0
instance-fold source. The written role is that wart's presentation of the
fold subject and nothing else; on any other fold source it claims a
navigation a fold source never performs.

    WF-083 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: navigatieRol
    'bewarende jutter' on a fold source that is not the v0 instance-fold
    source (the unknown-typed row-1 wart): the written role is that wart's
    presentation of the fold subject and nothing else — on any other fold
    source a navigatieRol claims navigation, which a fold source never does
    (its relation is bronRol)

The document. The Jutter-subject set fold "het totaal van het bewaartarief
van alle vondsten van de Jutter" — a genuine attribute read: `set: true`,
`objecttype: "Vondst"` (the elementObjecttype, so the anchor cross is
satisfied), a real EUR/mnd datatype, no unknown sentinel — with
`navigatieRol: "bewarende jutter"` written onto it. That role IS filled by
the fold subject `Jutter`, so the self-denotation test passes: this document
is refused by the SCOPE, not by the name.

Why the scope half has to exist. It is what keeps the re-anchoring of the
self-denotation test (sibling `wf083-vouwbron-anker-op-operand`) from opening
a hole the rule did not have. Anchored on the operand, the test happened to
reject this document for the wrong reason — `Vondst` fills no role named
"bewarende jutter". Anchored on the subject, and with no scope, it would have
ADMITTED it, because the subject does fill that role. Both executors refuse
the shape outright: `ref`'s set-source arm and its temporal total each demand
`navigatieRol` absent, and the ASP backend's set-fold arms mirror them field
for field. Only the wart's arm tolerates a written role, and it tolerates it
under exactly one condition — that the name denotes the subject.

Executors, on this document (case: three finds worth 12, 7 and absent, the
total pinned at 19):

    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated
    Drools      : PLAYED — it fires and answers the pinned 19

The same three-road split as the sibling: the two OCaml executors fault, the
bean-model executor ignores the written role and answers. A document three
executors read three ways is not well-formed.

Hand-authored: the temporal-total golden's set fold with one field added.
Schema-valid — `navigatieRol` is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: in `aggregation_source_check`, let the non-wart arm
of the self-denotation clause return `[]` (`| Some nr when not wart -> []`).
Measured over golden + rood + negatief + parity — 508 documents — exactly one
report changes and this case, alone, turns clean; the wart-carrying siblings
`wf083-vouwbron-anker-op-operand` and `wf083-fold-source-navigates-away` stay
rejected, and so do the two anchor-cross cases.
