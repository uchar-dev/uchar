# WF-083 — the self-denoting instance-fold source counts in both executors

Goal: the stress-test instance-fold class, sub-form C1: "het aantal
<rol> van de <onderwerp>" where the written source names the fold
subject by its OWN role name. The v0 wire carries `navigatieRol` on the
fold-source argument; production ignores the field (the form denotes the
subject, no navigation happens), but ref's contributors() arm demands
`navigatie_rol = None` (ref/lib/eval.ml:801-815) and faults "unsupported
instance-fold source shape"; the verifier passes it silently. Close it
spec-first (the exploration design, verified file:line): (1) IR-SPEC
ch. 4.1a/4.3, one paragraph: a fold-source argument may carry
`navigatieRol` iff it names a role (ci, name or declared plural) that
the argument's object type — the fold subject — itself fills; the field
is presentation of the written form, not navigation; the fold stays
subject-anchored. New **WF-083** enforces exactly that: a fold source
with a non-empty navigatiePad or an away-pointing navigatieRol is
ill-formed. (2) RUNTIME-SEMANTICS worked example beside ch. 3.4a/1.2e:
two linked elements count 2 (instances, not values — a leeg element
attribute still counts), zero links count 0. (3) verifier: the
aggregation_source_check arm (checks.ml ~1207) + the WF-083 rejection
replacing the silent fall-through. (4) ref: widen arm 1 with the
self-denotation check (existential: ≥1 fact-type role, ci over name and
declared plural, filled by self.otype — the safe mirror of "production
ignores the field"); keep the loud fault otherwise. (5) clingo:
translate.ml:862 same widening. (6) The dep-walker gap: the bindingless
fold source gets the relation edge beside the population edge in the
shared dep_forms (checks.ml:3122-3133) — cheap, closes a latent
mis-stratification. (7) Witness: golden with the role-name-denoting
count through both executors; negatief `wf083-fold-source-navigates-away`
(navigatieRol naming a role filled by a non-subject type).

Acceptance:
- Red-first: the verifier-clean repro faults in today's ref with the
  quoted fault; after the change both executors replay the golden and
  the negatief is rejected citing WF-083.
- All nine gates green with counts.

May touch: `spec/IR-SPEC.md` (paragraph + WF-083 index row),
`spec/RUNTIME-SEMANTICS.md` (worked example), `spec/verifier/`, `ref/`,
`backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*`, `corpus/negatief/wf083-*`.
Out of scope: sub-form C2 (the ordered eerste/laatste articulated fold
— own card, carries a semantics decision), the optional emitter
pin-bump giving the self-denoting source a bronRol (toolchain-side
follow-up), best-effort pluralisation (stays a tracked delta),
`spec/CONSTRUCTS.md` (serial at merge).
