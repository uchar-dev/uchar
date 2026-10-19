Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5) at the **denominator** of a
composite rate: the `EUR/mnd` unit's numerator is untouched, and its
denominator `mnd` names its canonical unit `mnd` while carrying no
`factorToCanonical`. One field, at the eleven positions that same
denominator record occupies. Everything else is the temporal-total golden
unchanged.

    WF-092 FAIL /model/objectTypes/0/attributes/0/datatype/unit/denominator:
    unit 'mnd' names canonical unit 'mnd' but carries no conversion factor
    into it: a half-filled conversion pair is not a unit (both fields, or
    neither)
    WF-092 FAIL /model/objectTypes/0/attributes/1/datatype/unit/denominator: …
    WF-092 FAIL /model/objectTypes/0/attributes/2/datatype/unit/denominator: …
    WF-092 FAIL /model/objectTypes/1/attributes/0/datatype/unit/denominator: …
    WF-092 FAIL /rules/rules/0/versions/0/value/type/unit/denominator:
    …
    WF-092 FAIL /rules/rules/1/versions/0/value/type/unit/denominator:
    …

Reading convention (house-wide): a citation path is the verifier's LOGICAL
position — the walk that reached the node, spelled with declared names like
`bewaarvergoeding per maand` — not a JSON Pointer into the wire document,
which would escape those names and index the maps differently. Paths locate,
they do not address.

The eleven split 4 / 2 / 2 / 3 over four call sites, each mutant-measured by
the citations it loses: four model-layer attribute declarations, two rule
targets, two walked expression `type`s and three fold sources. None of them
comes from the unit-system walk, because the declared systems spell
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
plain-unit sibling `wf092-canonical-without-factor`, whose branch this case
repeats one level down: the missing factor is read as 1 by two executors and
refused by name by the third. Stating that plainly rather than implying a
measurement that was not made: this case pins a REFUSAL and nothing else.

Falsifiable, branch-exact and position-exact: remove the denominator descent
from `check_eenheid_form` in `spec/verifier/lib/checks.ml` and this case turns
clean, alone, while both plain-unit cases stay red at every one of their
positions. Neuter the canonical-without-factor branch instead and this case
turns clean together with `wf092-canonical-without-factor`, the branch they
share, while `wf092-factor-without-canonical` stays red.

*Mutating that descent takes TWO edits, not one:* dropping the recursive call
also makes the `rec` flag unused, which this build treats as an error
(warning 39). Anyone who removes only the call gets a failed build and a
sweep that quietly re-runs the previous binary — a green that means nothing.
Drop the `rec` in the same edit.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the rule
targets and the fold sources lost their own `type` (B13), and with them five
of the eleven positions — six remain, four declarations and the two
aggregations' derived types; the split and the mutant counts above record
the positions as they stood before L2.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The document is the
translation of `spec/proposals/v1-migratie/m3.py`: a unit's denominator is
spelled `denominator`, and the factor the witness leaves out is absent where
it was null (delta B17 (i) of `spec/proposals/V1-WIREMIGRATIE.md`). The report
is unchanged but for its paths. `expect.txt` pins the arm.
