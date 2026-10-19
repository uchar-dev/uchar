Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (r)): a **unit** whose `system` —
the unit system it belongs to — names a system *no declaration carries*.

    WF-034 FAIL /model/objectTypes/0/attributes/0/datatype/unit/system: unit
    'm' declares unit system 'Afstandt', which no declaration carries: the unit
    resolves against no conversion table

(four times, once per unit record in the document that claims the system: two
attribute declarations and two expression types. Until L2 it was six times,
with four expression types; the rule target and the right operand, both
references, lost their own `type` with B13, and with it their unit record.)

**What the field is.** A unit record on the wire carries the name of the
declared `UnitSystem` it belongs to (absent for the built-in `%` and the bare
calendar units). That is the unit's one link back to
the conversion table that gives it meaning — WF-009's declared chain of
canonical units and factors. Nothing crossed it: WF-009 checks the table's
INTERNAL consistency, WF-092 checks the record's own conversion pair, and
neither asks whether a unit occurrence points into a declared table at all.

**Who reads it.** The ASP backend's normalisation pass reads it as a gate: a
numeric literal is restated into the finest base unit of the target
attribute's system only when the literal's own `system` matches that system
by name and the system resolves among the declared unit systems. The verifier's identity
family (WF-089/WF-090/WF-091) reads it as part of a unit's structural identity
— two units are the same unit only if they name the same system. `ref` does
not read it; the Drools backend does not read it.

**The harm is a measured ref / clingo divergence.** Replaying this document
against its host golden's own case, with the verifier of the day before this
card calling it well-formed:

    ref     oracle: PASS (2 expectations)
    clingo  TRANSLATION FAULT: non-identical units under the write to
            "sleeplengte" are not translated (ref restates with an exact
            conversion factor this integer encoding does not carry)
    drools  PLAYED 1

against, on the untouched golden, `PASS` in all three. The dangling system
name switches the ASP restating pass off silently — the literal keeps its
coarser unit, the write no longer sees identical units, and the backend
declines the whole document. A verifier-clean document that one executor
answers, a second declines, and a third answers again is exactly the class an
engine-neutral IR must not admit.

**Measured.** Every use of one declared system renamed consistently, one
(golden, system) pair at a time — the instrument has to be consistent, since
renaming a single occurrence breaks unit identity and WF-091 reports that
instead: **21 of 21** pairs stayed verifier-clean under the verifier of the
day before this card, and none does now.

**Scope.** This arm crosses the `system` back-reference and nothing else.
Whether the unit's `abbreviation` is a unit that system declares is WF-009's
question and stays out of scope (ch. 4.6, and the open list says so).

*Measured breadth:* zero documents lose anything. Of the **2377** unit records
the four zones held (2026-09-06), **2244** named a system and every one of
them resolved; the remaining 133 named none — the percentage unit and the bare
calendar units, which belong to no declared system and which this arm passes
over by construction. The four-zone sweep moves no other
verdict and no other report. (*Recounted 2026-09-06.* The figures first
recorded here were **2442** and **2309**: the instrument counted the **73**
denominator units of composite rates twice — once in their own right and once
under the enclosing record — and 2369 + 73 = 2442, 2236 + 73 = 2309. The count
above is the corrected total over the four zones as they stood then. A
denominator is a unit record and is walked as one; anything that counts unit
records must descend into `denominator` exactly once.)

A hand-broken variant of `corpus/golden/jutterswet-sleepbaan/ir.json` — the
system name misspelt at every use OUTSIDE the declaration, a six-line diff;
schema-valid, since `Unit.system` is any string. Since L3 the field is spelled
`system` (card `20260924-l3-typesysteem`); the defect and the report are the
same, only the key and the path moved. `expect.txt` pins the arm.

Falsifiable, arm-exact: make the `system` arm of `check_eenheid_form`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones,
turns clean — 522 documents compared, one verdict moved, no report elsewhere.
