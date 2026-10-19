Violates WF-100 (`spec/IR-SPEC.md` ch. 4.6 (q1)) at the **characteristic
check's owner**: the check names `gekeurd` and hosts itself on `Jutter`, a
declared object type that declares `penninggerechtigd` and no `gekeurd` at
all. The subject, `Vondst`, does declare it — which is exactly the shape the
rule is about: before 2026-09-10 that was enough to make the document clean,
because the declared-characteristic cross was keyed on the subject and nothing
looked at the field.

    WF-100 FAIL /rules/rules/0/versions/0/condition/host: 'gekeurd'
    is not a declared characteristic of 'Jutter'

Broken from `corpus/golden/jutterswet-keurhouder` by moving one field: the
first rule's `host` from `Vondst` to `Jutter`. Everything else — the two
rules, both polarities, both object types — is that golden verbatim, so the
defect is the owner name alone.

Why it has to exist. WF-034 (q) fences the field's RESOLUTION (the name must
be a declared object type) and stopped there, saying so; which owner is
AUTHORITATIVE was left open and was decided on 2026-09-10 in the field's
favour. Under that decision the declaration cross is keyed on the owner, so an
owner that declares no such characteristic resolves the check against nothing
— and the third executor is the one that shows what that costs. Measured on
this exact document before the rule (2026-09-10): verifier **PASS**, `ref` and
the ASP backend both answered from the subject's own `gekeurd`, and the Drools
backend dropped the rule as a transpiler skip (`voorwaarde buiten Phase-B
scope`). Put the same name on a §13.4.7 fact-creation guard, where the bean
model does host the constraint on the named type's pattern, and the DRL branch
emits a property constraint against a bean that has no such field while the
exec-model branch reaches `RegelSpraakRuntime.get` on a getter that does not
exist. WF-100 keeps that document off the wire.

Falsifiable in two directions, one of which narrowed on 2026-09-19:

- give `Jutter` a `gekeurd` of its own — the owner then declares the
  characteristic and WF-100 has nothing to say. *Until WF-109 landed that left
  the document verifier-clean and refused by the executors instead; since then
  the object half is the verifier's too, so that direction lands on WF-109 and
  its own witness, `wf109-host-outside-range`.* Either way it is a
  different rule and a different report;
- put `Vondst` back in the field, or empty the field — the golden.

Arm-exact, as measured when WF-100 landed: this was the only document in the
four zones whose verdict that rule moved. Measured over the verifier-gated
zones on the day it landed (golden 167, negative 360, red 26 — 553
documents), the before/after sweep moved this verdict and not one other report
line anywhere. **Since 2026-09-19 this file reports TWO rules**, because the
host it names is also an object nothing here is — the second citation is
WF-109's, and it is the one rule this directory does not declare. The declared
rule still fires and still fires first-class; the document violates both
obligations at one field, which is what two independent crosses on one field
look like.

Since L5 the field is `host` (delta B10); the report cites `…/host`, and
`expect.txt` pins the arm.
