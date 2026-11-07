Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (r)): a **unit** whose `system` —
the unit system it belongs to — names a system *no declaration carries*.

    WF-034.undeclared-unit-system FAIL /model/objectTypes/0/attributes/0/datatype/unit/0/system:
    unit 'm' declares unit system 'Afstandt', which no declaration carries: the
    unit resolves against no conversion table

(four times, once per unit record in the document that names the system: two
attribute declarations and two expression types.)

**What the field is.** A unit record on the wire carries the name of the
declared `UnitSystem` it belongs to (absent for the built-in `%` and the bare
calendar units). That is the unit's one link back to the conversion table that
gives it meaning. WF-034 (r) holds that a present `system` names a declared
system. Where it does, WF-009 holds that the system declares the unit's
abbreviation (`wf009-undeclared-abbreviation`); where it does not, as here,
that question does not arise and WF-034 alone reports.

**Who reads it.** The ASP backend's normalisation pass reads it as a gate: a
numeric literal is restated into the finest base unit of the target
attribute's system only when the literal's own `system` matches that system
by name and the system resolves among the declared unit systems. The verifier's
identity family (WF-089/WF-090/WF-091) reads it as part of a unit's structural
identity — two units are the same unit only if they name the same system.

**The harm.** Replaying this document against its host golden's own case
(measured 2026-11-07; on the untouched golden all three pass):

    ref     oracle: PASS (2 expectations)
    clingo  TRANSLATION FAULT: non-identical units under the write to
            "sleeplengte" are not translated (ref restates with an exact
            conversion factor this integer encoding does not carry)
    drools  PLAYED 1 (both branches)

The dangling system name switches the ASP restating pass off: the literal keeps
its coarser unit, the write no longer sees identical units, and the backend
declines the whole document, while the other two executors answer it.

**Measured breadth.** Every unit record of the four zones that names a system
names a declared one, except the four of this document; the percentage unit
and the bare calendar units name none, and this arm passes over them by
construction. Every use of one declared system renamed consistently, one
(golden, system) pair at a time, is refused (21 of 21 pairs when the arm was
written); the instrument has to be consistent, since renaming a single
occurrence breaks unit identity and WF-091 reports that instead.

The document is `corpus/golden/jutterswet-sleepbaan/ir.json` with the system
name misspelt at every use outside the declaration; schema-valid, since
`Unit.system` is any string. `expect.txt` pins the arm.

Falsifiable, arm-exact: make the `system` arm of `check_unit_form`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones,
turns clean.
