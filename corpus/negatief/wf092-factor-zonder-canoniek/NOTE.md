Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **factor-without-canonical**
half: the `Afstand` unit `m` carries `factorNaarCanoniek: 1` and
`canoniek: null` — a factor with nothing to convert into. Everything else is
the unit-normalisation golden unchanged. One field, at the five positions
that same unit record occupies (the system declaration, both attribute
declarations, the rule target's type, the attribute reference's type).

    WF-092 FAIL /eenheidstelsels/Afstand/eenheden/m: unit 'm' carries
    conversion factor 1 but names no canonical unit to convert into: a
    half-filled conversion pair is not a unit (both fields, or neither)
    WF-092 FAIL /objecttypen/Sleepbaan/attributen/aanloop/eenheid: unit 'm' …
    WF-092 FAIL /objecttypen/Sleepbaan/attributen/sleeplengte/eenheid: …
    WF-092 FAIL /resolvedRegels/0/doel/type/eenheid: unit 'm' …
    WF-092 FAIL /resolvedRegels/0/waarde/rechts/type/eenheid: unit 'm' …

The unit is deliberately the **self-canonical** one. That is what makes the
case reach anything: the convertibility predicate falls back to the
ABBREVIATION when `canoniek` is null, and `m`'s abbreviation happens to be
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
and restate correctly; the third executor reads `canoniek` and finds nothing
to match `km` against, drops the conversion, and adds `2,5 + 300 = 302,5`
straight into an `m` target. A wrong value, silently, from a document every
rule admitted.

Falsifiable, branch-exact and position-exact: neuter the
factor-without-canonical branch of `check_eenheid_form` in
`spec/verifier/lib/checks.ml` and this case turns clean, alone, while
`wf092-canoniek-zonder-factor` and `wf092-noemer-zonder-factor` stay red on
the mirror branch. Drop the model-layer datatype walk from `wf092` and it
loses exactly its two attribute-declaration citations, keeping the system
declaration, the rule target and the expression type; drop the
`eenheidstelsels` walk and it keeps the other four. It is refused five times
over by design: the rule is a shape law on every unit in the document, not a
cross between two of them.
