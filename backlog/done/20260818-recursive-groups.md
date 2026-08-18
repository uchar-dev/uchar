# Recursive rule groups execute: the one-generation chain runs to its fixpoint in both executors

Goal: the `recursive` marker gets its neutral meaning (CONSTRUCTS row
14): membership of a recursive group is exactly what WF-045 already
admits — a creation whose output feeds its own condition — and both
executors run the chain to its fixpoint. `ref/` learns to assign
evaluation strata in the presence of the admissible cycle (edges inside
a dependency cycle count positive — ch. 6.3's discharge-by-generation
clause, implemented); its global pass budget is the ch. 5.5 fault. The
ASP backend bounds the otherwise-unbounded grounding with a
per-instance generation depth (`depth/2`, `#const oc_budget`) whose
exhaustion derives a `viol/2` witness — UNSAT exactly where ref/'s
budget faults. Because a self-typed chain cannot name its back-link
role by derivation (both roles fill the same type; the pinned runtime
returns null and drops the link), `ResolvedObjectCreatie` gains the
`verbindendeRol` extension field (wire-entry discipline): a RoleBinding
naming the role the CREATED object's far side... the created-side role
explicitly; the derivation remains the bindingless fallback, and the
self-typed two-role derivation now mirrors v0's null (no link, marker
idempotence) instead of silently picking a role.

Witness: `corpus/golden/anchor/jutterswet-wachtersketen` — a recursive
group whose creation chains Wachter rank n to n+1 while n < 5 over the
`opvolging` relation (bound via the new field); the direct-successor
fold pins generation 2 exactly, a detector consistency pins that
generation 5 is reached (inconsistent), and green-within-budget pins
termination. The red witness `corpus/rood/runaway-recursion` pins the
budget fault in both executors (ref: no fixpoint within the pass
budget; clingo: generation budget exhausted → UNSAT).

Acceptance: all eight gates green; WF-033's row carries the executable
golden; the ch. 5.5 recursive worked example is machine-checked in its
measurable form; the Method extension list and ch. 3.3/3.9 record the
new field and the interpreted marker.

May touch: `spec/verifier/lib/{ast,loader,checks}.ml`,
`spec/schema/regelspraak-ir-v0.schema.json`, `ref/lib/eval.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/README.md`,
`corpus/golden/anchor/jutterswet-wachtersketen/` (new),
`corpus/golden/anchor/README.md`, `corpus/rood/runaway-recursion/`
(new), `corpus/negatief/` (one new WF-047 creation-binding case),
`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`backlog/`.

Out of scope: recursive FACT-creation chains (idempotent, no guard
needed per WF-033 — recorded untested); emitting `verbindendeRol` on
creations from the toolchain (pin-bump territory); channel-content
expectations (the exact end-rank pin beyond the detector's granularity
is recorded as future case-schema work).

Result: landed in full; the recursive marker is interpreted, not
carried. Membership of a recursive group is the WF-045 cycle
admission, and the chain runs one generation per instance to its
fixpoint in both executors: ref/'s strata became cycle-tolerant
(reachability classifies edges on a dependency cycle; strictness there
is discharged by the generation progression, ch. 6.3 implemented), and
the ASP backend grounds recursive creations along a generation depth
whose exhaustion is the named viol/2 witness. ObjectCreatie gained the
verbindendeRol extension (schema/loader/WF-047 site/WF-052/dep_forms)
because a self-typed chain cannot name its back-link by derivation —
connecting_pair now mirrors v0's null there, with the v0-faithful
candidate counting (a two-role self shape blocks; any plurality on a
self-typed pair is null). Binair Plus landed in the ASP expr_term
(numeric operands only). The wachtersketen anchor pins the chain
measurably (fold 2/5/0; the rank detector inconsistent exactly because
generation five exists); runaway-recursion red-pins the budget fault
in both executors; wf047-creation-binding-wrong-filler pins the new
binding site.

The verification round (two opus lenses) forced four fixes before
merge: the budgets are now EQUAL (100 passes in ref, 100 generations
per chain in clingo — the round proved a 100..499-generation window
where clingo was UNSAT while ref answered, and the fix surfaced a real
clingo limitation: its term handling aborts near nesting depth ~200,
now a recorded README limitation the budget deliberately stays under);
ref re-runs WF-045/046 itself before evaluating (the cycle-tolerant
strata would otherwise silently answer on verifier-red input — fail
fast restored, demonstrated on stripped-group and negative-cycle
documents); Binair Plus refuses non-numeric operands (a Date would
have summed as day numbers where ref faults); and connecting_pair
counts self-typed candidates exactly as the pinned runtime does (the
reciprocal-beside-binary corner no longer minted a link ref-side
only). Notes recorded: non-recursive mints carry depth facts in
recursive documents so chains seeded from them stay bounded; the
ch. 3.3 table now lists the extension field.

Deliberately left: recursive FACT-creation chains (WF-033 admits them
guard-free; no witness yet); emitting verbindendeRol on creations from
the toolchain (pin-bump territory); exact end-rank pinning beyond the
detector's granularity (channel-content expectations are future
case-schema work).
