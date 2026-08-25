# D1 — the §5.5.3 self-reference navigates zero hops (mirrored, narrowed)

Goal: 2 stress-test scenarios: a rule denotes its own subject through
the role name the subject itself fills; production reads the own
attribute (zero hops), ref faults "no role named %S reachable from %S"
(eval.ml:221 via nav_target's bindingless one-step arm, 248-250), the
verifier rejects with WF-053 (checks.ml:1363). Maintainer decision
(2026-08-25): mirror, NARROWED. Deliver: (1) IR-SPEC ch. 4.1a, the
WF-053 bindingless paragraph: a single bindingless written step that
does not resolve as a far role, while the endpoint type equals the
subject type AND the name (ci, name or declared plural) names a
declared role the subject type itself fills, denotes the subject — the
walk is empty (zero hops). Resolution ORDER is normative: far-role
resolution first (the reciprocal partner case stays a real hop);
self-reference only at zero far hits; v0's broader fall-through (ANY
unresolved name on a same-typed reference reads the own attribute) is
NOT mirrored — a tracked delta in the safe direction; v0's
ambiguous-far-match fall-through also stays a loud fault (delta noted).
(2) RUNTIME-SEMANTICS ch. 5.8 rule 4 + worked example showing both the
unconditional own-read and the leeg-on-absent-link contrast of real
hops. (3) verifier: the same arm in nav_hops (checks.ml:1461-1487).
(4) ref: the nav_target arm. (5) clingo: share the derivation.
(6) The navigated characteristic check (eval.ml:897-923) carries the
same form — include it, or explicitly defer with a loud fault and say
so in the Result. (7) Witness: golden through both executors; negatief
`wf053-zelfverwijzing-vreemde-rol` (same-typed reference whose name is
NOT a declared own role — stays rejected, pinning the narrowing).

Acceptance:
- Red-first: the verifier-clean... — note the current form is verifier-
  RED (WF-053): show today's verifier rejection AND ref fault quoted,
  then both executors replaying the golden after the change; negatief
  rejected citing WF-053.
- All nine gates green with counts. Keep the golden timeline-free (the
  exploration's WF-016/WF-079 entanglement warning).

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/verifier/`, `ref/`, `backends/clingo/`, `spec/ASP-TRANSLATION.md`
(if the derivation needs a note), `corpus/golden/jutterswet-*`,
`corpus/negatief/wf053-*`.
Out of scope: the plural-morphology fallback (card D2), the fold-source
forms (cards C1/C2), `spec/CONSTRUCTS.md` (serial — row 4 delta note at
merge).
