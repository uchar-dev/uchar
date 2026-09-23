Violates WF-022 (`spec/IR-SPEC.md`), its second clause: a characteristic
target's value must be the boolean constant true. The flat rule
`geen herleving` attributes the characteristic `herleefd` with the literal
`onwaar`.

The document is the one proposed in
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §7.3: one object type
`Recht` with the characteristics `herleefd` and `nieuw recht ontstaan`, and
one rule whose condition is `nieuw recht ontstaan`. The verifier rejects it
on this clause and on nothing else:

    WF-022 FAIL /rules/resolvedRegels/0/waarde: characteristic target's value must be the boolean constant true

The older negatief `wf022-target-unresolvable` violates the first clause
only (the target does not resolve), so until this case nothing failed when
the value clause went missing. Measured 2026-09-23 on a verifier with the
value clause switched off: gate 3 failed on this case and on no other.

What the executors do with the document when it is forced through anyway,
measured 2026-09-23 with a case outside the corpus (R1 has `nieuw recht
ontstaan`, R2 has not):

- `ref` runs the rule and leaves `herleefd` absent on R1 and R2.
- The ASP backend stops at translation: `TRANSLATION FAULT: characteristic
  attribution with a non-waar value`.
- The Drools exec branch sets `herleefd` present on R1 (`FAIL case1.json:
  kenmerk herleefd: verwacht afwezig, werkelijk aanwezig`).
- The Drools DRL-text branch leaves it absent on both (`PLAYED 1`).

One document, three answers and a refusal. On this wire a negative decision
on a characteristic exists only inside a defaults group, where it keeps the
group from attributing the characteristic; a flat rule with `onwaar` would
be a write, and WF-022 refuses it.
