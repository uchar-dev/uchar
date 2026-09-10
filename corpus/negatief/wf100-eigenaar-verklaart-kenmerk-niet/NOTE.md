Violates WF-100 (`spec/IR-SPEC.md` ch. 4.6 (q1)) at the **characteristic
check's owner**: the check names `gekeurd` and hosts itself on `Jutter`, a
declared object type that declares `penninggerechtigd` and no `gekeurd` at
all. The subject, `Vondst`, does declare it — which is exactly the shape the
rule is about: before 2026-09-10 that was enough to make the document clean,
because the declared-characteristic cross was keyed on the subject and nothing
looked at the field.

    WF-100 FAIL /resolvedRegels/0/voorwaarde/eigenaar: 'gekeurd' is not a
    declared characteristic of 'Jutter'

Broken from `corpus/golden/jutterswet-keurhouder` by moving one field: the
first rule's `eigenaar` from `Vondst` to `Jutter`. Everything else — the two
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

Falsifiable in two directions, each turning this case clean:

- give `Jutter` a `gekeurd` of its own — the owner then declares the
  characteristic and WF-100 has nothing to say (the document stays clean for
  the verifier and is refused by the executors instead, which is a different
  rule and a different report: `spec/RUNTIME-SEMANTICS.md` ch. 5.8b);
- put `Vondst` back in the field, or empty the field — the golden.

Arm-exact: this is the only document in the four zones whose verdict the rule
moves. Measured over the verifier-gated zones on the day it landed (golden
167, negatief 360, rood 26 — 553 documents), the before/after sweep moves this
verdict and not one other report line anywhere.
