Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), the **canonical-without-factor**
half: the `Afstand` unit `km` names its canonical unit `m` and carries no
`factorToCanonical`. Everything else is the unit-normalisation golden
unchanged — the `m` unit, both declared attributes, the rule, the literal
`2,5 km`. One field, at the three positions that same unit record occupies.

    WF-092 FAIL /model/unitSystems/0/units/1: unit 'km' names
    canonical unit 'm' but carries no conversion factor into it: a
    half-filled conversion pair is not a unit (both fields, or neither)
    WF-092 FAIL /rules/rules/0/versions/0/value/type/unit: unit 'km' …
    WF-092 FAIL /rules/rules/0/versions/0/value/left/type/unit: unit
    'km' …

Why it has to exist: no neighbouring rule can reach this shape. WF-009 walks
the declared systems, but it only follows a canonical chain it can FOLLOW (a
factor's positivity is its wire form since L3) — a missing factor is nothing
to look at, and the chain `km → m` still terminates. WF-035 and WF-023 cross units for
COMPATIBILITY through the convertibility predicate, which reads the canonical
names and never the factor, so `km` and `m` remain compatible. WF-091
compares two units BYTE for byte, and the half-form is copied consistently
across every arm it crosses, so every comparison agrees.

**The measured executor asymmetry** (the same case input the source golden
pins, `aanloop = 300 m`, expecting `sleeplengte = 2800 m`), against the
verifier that did not yet carry this rule — it reported `verifier: PASS`:

    ref     EVALUATION FAULT: S1.sleeplengte: value 605/2 does not fit the
            declared 0 decimals — an explicit rounding is mandatory (ch. 2.4)
    clingo  UNSATISFIABLE — a fail-fast guard fired:
            viol("result of sleeplengte does not fit its declared decimals","S1")
    Drools  IllegalStateException: onbekende omrekenfactor van 'm' (1) naar
            'km' (null): canoniek 'm' zonder gedeclareerde factorNaarCanoniek

Three executors, three reports, one shared misreading: `605/2` is `302,5`,
which is `2,5 + 300` — the missing factor silently read as 1, so `300 m` was
restated into `km` unchanged instead of becoming `0,3 km`. Two of the three
executors then trip over the DECIMALS of a number whose UNIT was already
wrong, and name the decimals in their report; only the third names the cause.
The refusal that arrives is not the refusal the document earns, and on a rule
whose target admitted one decimal no refusal would arrive at all.

Falsifiable, branch-exact and position-exact: neuter the
canonical-without-factor branch of `check_eenheid_form` in
`spec/verifier/lib/checks.ml` and this case turns clean — together with
`wf092-noemer-zonder-factor`, which is the same branch one level down — while
`wf092-factor-zonder-canoniek` stays red on the mirror branch. Drop the
unit-system walk from `wf092` instead and this case keeps its two
expression-type citations and stays red; drop the expression-walk call
instead and it keeps its declaration citation. It is refused three times over
by design: the rule is a shape law on every unit in the document, not a cross
between two of them.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The document is the
translation of `spec/proposals/v1-migratie/m3.py`: the conversion pair is
spelled `canonical` and `factorToCanonical`, and the half the witness leaves
out is absent where it was null (delta B17 (i) of
`spec/proposals/V1-WIREMIGRATIE.md`). The report is unchanged but for its
paths. `expect.txt` pins the arm.
