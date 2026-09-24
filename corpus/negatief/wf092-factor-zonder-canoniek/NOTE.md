Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **factor-without-canonical**
half: the `Afstand` unit `m` carries `factorToCanonical: {"num": 1, "den": 1}`
and no `canonical` — a factor with nothing to convert into. Everything else is
the unit-normalisation golden unchanged. One field, at the three positions
that same unit record occupies since L2 (the system declaration and both
attribute declarations; the rule target and the attribute reference carry no
type of their own since L2, B13).

    WF-092 FAIL /model/unitSystems/0/units/0: unit 'm' carries
    conversion factor 1 but names no canonical unit to convert into: a
    half-filled conversion pair is not a unit (both fields, or neither)
    WF-092 FAIL /model/objectTypes/0/attributes/0/datatype/unit: unit 'm' …
    WF-092 FAIL /model/objectTypes/0/attributes/1/datatype/unit: …

The unit is deliberately the **self-canonical** one. That is what makes the
case reach anything: the convertibility predicate falls back to the
ABBREVIATION when `canonical` is absent, and `m`'s abbreviation happens to be
the canonical name `km` points at, so the pair stays compatible and WF-035
and WF-023 both pass. Do the same to `km` instead — whose abbreviation is not
its canonical name — and the fallback makes the two units incompatible, so
WF-035 and WF-023 refuse the document for a reason that is not this one
(measured: `WF-023 assignment across incompatible units` plus `WF-035
additive operands have incompatible units`). The half-form is therefore
either masked by a neighbour or invisible to every neighbour, and this case
pins the second, sharper window.

**The measured executor asymmetry** (the same case input the source golden
pins, `aanloop = 300 m`, expecting `sleeplengte = 2800 m`), against the
verifier that did not yet carry this rule — it reported `verifier: PASS`:

    ref     oracle: PASS (2 expectations)      → 2800
    clingo  clingo: PASS (2 expectations)      → 2800
    Drools  FAIL: attribuut sleeplengte: verwacht 2800, werkelijk 302.5

This is the sharper of the two halves. Nothing faults anywhere: three
executors, two different answers, no refusal. `ref` and clingo read the
missing canonical name as the abbreviation, find `m` and `km` convertible,
and restate correctly; the third executor reads the canonical name and finds nothing
to match `km` against, drops the conversion, and adds `2,5 + 300 = 302,5`
straight into an `m` target. A wrong value, silently, from a document every
rule admitted.

Falsifiable, branch-exact and position-exact: neuter the
factor-without-canonical branch of `check_eenheid_form` in
`spec/verifier/lib/checks.ml` and this case turns clean, alone, while
`wf092-canoniek-zonder-factor` and `wf092-noemer-zonder-factor` stay red on
the mirror branch. Drop the model-layer datatype walk from `wf092` and it
loses exactly its two attribute-declaration citations, keeping the system
declaration, the rule target and the expression type; drop the unit-system
walk and it keeps the other four. It is refused five times
over by design: the rule is a shape law on every unit in the document, not a
cross between two of them.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the rule
target and the attribute reference lost their own `type` (B13), and with
them two of the five positions; the paragraph on mutants above counts the
positions as they stood before L2.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The document is the
translation of `spec/proposals/v1-migratie/m3.py`: the factor is the exact
rational `{num, den}`, and the canonical name the witness leaves out is absent
where it was null (delta B17 (i) of `spec/proposals/V1-WIREMIGRATIE.md`). The
report is unchanged but for its paths. `expect.txt` pins the arm.
