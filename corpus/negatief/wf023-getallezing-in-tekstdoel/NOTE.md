Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `AttributeReference`
arm, SOORT facet: the reference reads `Jutter.gewicht`, a `Numeriek`
attribute, with a serialized `type {Tekst}`. It is written into
`Jutter.etiket`, declared `Tekst`, so WF-023's assignment cross sees text
into text and is satisfied — the target's declared datatype is what it
crosses, never the reference's annotation.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: soort mismatch: attribute
    reference 'gewicht' is typed text, but the declaration it reads from
    is number

Why it has to exist: the soort of a reference was crossed at exactly two
places before — at a `BinderRef` (WF-078) and at an object creation's
characteristic value (WF-027's resolved cross) — and nowhere else. On an
ordinary read the annotation could say anything the surrounding
expression tolerated. Neither side names a domain here, so the domain
facet is silent and soort carries the refusal under WF-091: it has no
number of its own at this arm, which is precisely the umbrella's job.

Falsifiable, facet-exact and arm-exact: neuter the soort facet and this
case degrades to `residual structure mismatch` — still WF-091, still red,
which is what makes the residual facet the backstop rather than a
leftover; disable the `Attribuut_ref` arm and it turns clean together
with the other `wf091-attribuutref-*` cases while the binder, parameter,
fold-source and write-target cases stay red.
