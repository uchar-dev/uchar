# wf083-fold-source-navigates-away

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3): the fold-source argument of the
count carries `navigatieRol: "vondst"` — a role filled by `Vondst`, not by the
fold subject `Jutter`. An away-pointing
`navigatieRol` on a fold source claims a navigation that no executor performs:
a fold source never navigates; its relation is the fold's `bronRol`.

The legal sibling is the **self-denoting** source (`jutterswet-aanmeldertelling`):
"het aantal vondsten van de aanmelder" names the fold subject by its OWN role
name — `navigatieRol: "aanmelder"`, a role `Jutter` itself fills — and the field
is presentation of the written form, not navigation. Here the written form
"het aantal vondsten van de vondst" points the role at the far side of the
relation instead; admitting it would let a document claim a hop the evaluation
silently never takes (the v0 production runtime ignores the field outright).

Both executors refuse the shape: `ref` faults `EVALUATION FAULT: unsupported
instance-fold source shape`; the ASP backend faults `TRANSLATION FAULT:
expression kind not translated`.

Old verifier: `PASS` (the fold-source arm's silent fall-through admitted any
`navigatieRol`). New verifier: `WF-083 FAIL /resolvedRegels/0/waarde/argumenten/0:
navigatieRol 'vondst' on a fold source does not denote the fold subject: no
role named so (declared name or plural, ci) is filled by the fold subject
'Jutter' itself — a fold source never navigates`.

The verdict is anchor-independent here and the wording is not: this document's
operand spells `objecttype: "Jutter"`, which for a non-set source IS the fold
subject, so the self-denotation test reads the same type either way. The
sentence names the subject explicitly since 2026-09-05, when the test moved
from the operand's own `objecttype` to the fold subject — the type both
executors anchor it at. The case that separates the two anchors is the sibling
`wf083-vouwbron-anker-op-operand`.
