# wf083-vouwbron-anker-op-operand

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), the **self-denotation** clause,
in the one shape where the anchor of that clause is observable: the written
role is tested against the type the executors read it on — the fold
**subject** — and not against the operand's own `objecttype`.

    WF-083 FAIL /resolvedRegels/0/waarde/argumenten/0: navigatieRol 'vondst'
    on a fold source does not denote the fold subject: no role named so
    (declared name or plural, ci) is filled by the fold subject 'Jutter'
    itself — a fold source never navigates

The document. A Jutter-subject count of the finds a Jutter reports: the
row-1 wart source (`attribuut: "vondsten"`, the declared plural of the role
`vondst`; the unknown sentinel on its own `type`) over
`elementObjecttype: "Vondst"`, carrying `navigatieRol: "vondst"` and
`set: true`. The operand's `objecttype` names `Vondst` — which for a set
source IS the anchor, so the ANCHOR CROSS of the same rule is satisfied and
this clause stands alone. `Vondst` fills a role named `vondst`; the fold
subject `Jutter` does not, and the written form "het aantal vondsten van de
vondst" therefore names no role the subject wears.

Why the shape matters. Anchoring the test on the operand's `objecttype` made
the verdict turn on a field neither OCaml executor reads in this position.
Measured on the same fact type, on the set fold of the temporal-total golden
with `navigatieRol: "vondst"` added and nothing else changed:

    objecttype "Vondst" : verifier PASS
    objecttype "Jutter" : WF-083 FAIL  (plus WF-034, since Jutter declares
                                        no such attribute)
    ref                 : EVALUATION FAULT: unsupported instance-fold source
                          shape  — in BOTH spellings
    ASP backend         : TRANSLATION FAULT: expression kind not translated
                          — in BOTH spellings

One wire field flipped the verdict; no executor moved. The field the
executors DO read — the walking type — did not appear in the rule at all.
`ref` guards its instance-fold source arm with the self-denotation test on
`self.otype`, the ASP backend its count arm on the subject type; both
therefore refuse the document above, and the verifier now refuses it for the
same reason they do.

Executors, on THIS document (case: two finds reported by J1, none by J2, the
count pinned at 2 and 0):

    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated
    Drools      : PLAYED — it fires and answers the pinned 2 and 0

Three roads, and the Drools reading is the sharpest argument that the shape
must not be well-formed: the bean-model executor silently ignores the written
role and counts the link set anyway, so a document that faults twice in the
OCaml executors quietly answers in the third. Refusing it at the wire is what
keeps the three from being asked.

Hand-authored: the counting golden's IR with the source turned into a set
read carrying the written role. Schema-valid — `set` is a boolean on every
`AttribuutRef` and the unknown sentinel is an admissible `type`.

Falsifiable, arm-exact: in `aggregation_source_check`, anchor the
self-denotation test back on the operand (`self_denoting_role ctx
~otype:otn nr` instead of `~otype:st`). Measured over golden + rood +
negatief + parity — 508 documents — exactly one report changes and this
case, alone, turns clean; the sibling `wf083-fold-source-navigates-away`
(whose operand objecttype IS the subject) stays rejected, and so do the
three other WF-083 cases.
