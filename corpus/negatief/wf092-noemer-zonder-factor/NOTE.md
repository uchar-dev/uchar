Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5) at the **denominator** of a
composite rate: the `EUR/mnd` unit's numerator is untouched, and its
denominator `mnd` names its canonical unit `mnd` while carrying
`factorNaarCanoniek: null`. One field, at the eleven positions that same
denominator record occupies. Everything else is the temporal-total golden
unchanged.

    WF-092 FAIL /objecttypen/Jutter/attributen/bewaarlast/eenheid/noemer:
    unit 'mnd' names canonical unit 'mnd' but carries no conversion factor
    into it: a half-filled conversion pair is not a unit (both fields, or
    neither)
    WF-092 FAIL /objecttypen/Jutter/attributen/bewaarsom/eenheid/noemer: …
    WF-092 FAIL /objecttypen/Jutter/attributen/bewaarvergoeding per maand/eenheid/noemer: …
    WF-092 FAIL /objecttypen/Vondst/attributen/bewaartarief/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/0/doel/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/0/waarde/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/0/waarde/argumenten/0/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/1/doel/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/1/waarde/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/1/waarde/argumenten/0/type/eenheid/noemer: …
    WF-092 FAIL /resolvedRegels/2/waarde/argumenten/0/type/eenheid/noemer: …

Reading convention (house-wide): a citation path is the verifier's LOGICAL
position — the walk that reached the node, spelled with declared names like
`bewaarvergoeding per maand` — not a JSON Pointer into the wire document,
which would escape those names and index the maps differently. Paths locate,
they do not address.

The eleven split 4 / 2 / 2 / 3 over four call sites, each mutant-measured by
the citations it loses: four model-layer attribute declarations, two rule
targets, two walked expression `type`s and three fold sources. None of them
comes from the `eenheidstelsels` walk, because the declared systems spell
`mnd` as a plain base unit and only the composite `EUR/mnd` carries it as a
denominator.

Why it has to exist: it is the only witness of the rule's **recursion**. The
two sibling cases both sit on a plain unit, so removing the denominator
descent from `check_eenheid_form` leaves each of them refused at exactly the
same positions with exactly the same messages — the recursion would be
unfalsifiable and effectively untested. This case is the branch's whole
evidence. It is also the only shape in which the rule's positions differ, as
the split above records.

**Verifier-only, and measured so.** No golden that carries a rate unit
carries a case file — the temporal-total golden this case copies is one of
the goldens gate 5 lists as not replayed — and it is the only golden in the
corpus with a denominator at all. So no executor measurement is possible on
this shape, and the executor asymmetry the rule answers to is measured on its
plain-unit sibling `wf092-canoniek-zonder-factor`, whose branch this case
repeats one level down: the missing factor is read as 1 by two executors and
refused by name by the third. Stating that plainly rather than implying a
measurement that was not made: this case pins a REFUSAL and nothing else.

Falsifiable, branch-exact and position-exact: remove the denominator descent
from `check_eenheid_form` in `spec/verifier/lib/checks.ml` and this case turns
clean, alone, while both plain-unit cases stay red at every one of their
positions. Neuter the canonical-without-factor branch instead and this case
turns clean together with `wf092-canoniek-zonder-factor`, the branch they
share, while `wf092-factor-zonder-canoniek` stays red.

*Mutating that descent takes TWO edits, not one:* dropping the recursive call
also makes the `rec` flag unused, which this build treats as an error
(warning 39). Anyone who removes only the call gets a failed build and a
sweep that quietly re-runs the previous binary — a green that means nothing.
Drop the `rec` in the same edit.
