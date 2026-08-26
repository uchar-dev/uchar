# D2 — the type-pair fallback for the fold-source role name (narrowed)

Goal: 1 stress-test scenario: an instance-fold source names the role
with a synthesised plural while the fact type declares no (mv:); v0
matches best-effort or falls back to the type pair (scope(),
DrlTranspiler.java:2893); uchar refuses. Maintainer decision
(2026-08-25): mirror the TYPE-PAIR fallback only, narrowed —
name resolution first (declared name or declared plural, ci); if the
name does not resolve AND exactly ONE fact type connects the subject
type to the element type, that role is the source (deterministic, no
spelling guess); zero or multiple connecting fact types → the loud
fault stays. The +s/+en pluralisation is NOT mirrored (stays a tracked
delta). Deliver: (1) IR-SPEC: the resolution rule stated normatively in
the aggregation-source paragraph (closing the OPEN morphology decision
ch. 2.6:597-602 in this narrowed form), with the WF-034 clause
adjusted; negatief `wf034-ambigu-typenpaar` (two fact types connect the
same pair, unresolved name → rejected). (2) RUNTIME-SEMANTICS: one
sentence + example. (3) verifier: aggregation_source_check learns the
fallback (checks.ml:1181-1227). (4) ref: the fold-arm resolution
(eval.ml:810-813) via connecting_role. (5) clingo: the same, AND the
unconditional fold-arm hardening the exploration found
(translate.ml:873 uses the raw lexeme as the rol/3 key — resolve-or-
fault instead; this also fixes the latent silent-0 when a case names
the singular of a to-many role whose declared plural is the key).
(6) Witness: golden where the written plural is undeclared but the type
pair is unique, through both executors.

Acceptance:
- Red-first: the repro faults in today's ref (quoted, "no role named
  ... reachable"); after the change both executors replay and agree;
  the ambiguity negatief is rejected citing WF-034.
- The clingo hardening is demonstrated: the singular-key form that
  yesterday produced a silent 0 now resolves (or faults loudly) —
  show it.
- All nine gates green with counts.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/verifier/`, `ref/`, `backends/clingo/`,
`corpus/golden/jutterswet-*`, `corpus/negatief/wf034-*`,
`spec/ASP-TRANSLATION.md` (hardening note).
Out of scope: pluralisation heuristics (stays a delta), navigated reads
(D1), `spec/CONSTRUCTS.md` (serial at merge).

Result: landed whole (2026-08-26). The narrowed type-pair fallback is
normative (IR-SPEC ch. 2.6 decision closing the OPEN morphology point,
ch. 4.3 resolution paragraph, WF-034 clause + index row) and executes in
all three implementations: verifier aggregation_source_check, ref
fold-arm (connecting_role), clingo count arm. The clingo arm is
additionally hardened to resolve-or-fault and to key rol/3 by the
connecting role's canonical name, never the raw written lexeme — the
latent silent-0 (written singular of a to-many role whose declared
plural keys the facts) is closed and pinned (ASP-TRANSLATION S8).
Red-first: ref faulted 'no role named "duinkavels" reachable from
"Jutter"'; clingo counted 0 where 3 and 2 were expected. Witness golden
jutterswet-kaveltelling (both executors, 5 expectations, noop-refuting);
negatief wf034-ambigu-typenpaar (rejected citing WF-034, WF-048
incidental); RUNTIME-SEMANTICS ch. 3.4a worked example verbatim as
WV-3.4a-typenpaar (5 ref-test assertions, add-only). Nine gates green:
138 ir round-trip, 138/158 verifier, 360 case-schema, 340 oracle, 340
clingo, 305+35 noop, 20 rood, 169 catala checks + 11 byte-identical.
Deliberately left: the +s/+en pluralisation stays an unmirrored tracked
delta (per the card); the CONSTRUCTS.md delta-row note is serial at
merge (out of scope here); the WF-047 written-name-denotes-bound-role
rule is untouched — with an explicit binding a divergent written form
stays ill-formed.

Review fix (2026-08-26). The MAJOR: the fallback counted distinct fact
types where v0 counts (fact type, role) candidates — a self-typed
two-role fact type (both roles the same object type) yields ONE
connecting fact type but TWO candidate roles, so ref/clingo silently
picked one role where v0 refuses. Fixed by counting candidate ROLES in
all three implementations: ref `connecting_role` (List.concat_map over
both role sides — clingo shares it), and a new verifier
`connecting_role_candidates` for the WF-034 aggregation fallback (its
`connecting_candidates`, which counts fact types, stays for WF-048 and
the ch. 5.5 creation back-link, both of which special-case the
self-typed pair at their call sites). The normative text now reads
"unique connecting role" with the self-typed block spelled out
(IR-SPEC ch. 4.3 + WF-034 clause + index row; RUNTIME-SEMANTICS
ch. 3.4a). Red-first: ref silently returned 1 before, faults
`ambiguous (2 candidate roles)` after; verifier passed the self-typed
document before, rejects WF-034 after. New negatief
`wf034-zelftype-typenpaar` — the clean WF-034-only witness (WF-048 does
not fire for a lone self-typed pair, so it uniquely exercises the
role-count branch). Also pinned the NOTE'd ambiguous-role-name branch
(reachable_roles_by_name ≥ 2 in ref eval.ml:981 and clingo
translate.ml:1040): new rood `ambiguous-fold-role-name` — two same-named
roles on distinct far types, verifier-clean, both executors fault
`ambiguous role name`. Nine gates green: 139 ir round-trip, 139/159
verifier, 361 case-schema, 340 oracle, 340 clingo, 305+35 noop, 21 rood,
169 catala + 11 byte-identical; ref 346 worked examples pass.
Deliberately left (motivated): (a) the verifier's global `is_role_name`
name gate vs ref's reachable-from-subject `reachable_roles_by_name` (the
MINOR/NOTE) — a distinct axis (name-resolution SCOPE, not the counting)
that the card left underdetermined; aligning either side is a v0-scope
decision needing the source, not a mechanical mirror. (b) the
member-attribute-vs-fallback spec-precision MINOR — the executors
already agree (member reads precede the sentinel-typed fold-source arm);
no normative claim added on the protected doc. (c) the zero-connector
WF-034 branch NOTE — co-guarded by WF-048 ("no fact type relates"), so
not cleanly pinnable in isolation without removing WF-048 (out of
scope); the self-typed negatief is the clean witness.
