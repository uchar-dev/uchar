# Relations first-class: explicit role bindings in the IR (rows 4/17/22)

Goal: land the Phase-3 exit criterion "role bindings replace bean-field
names in the IR" as a tracked, additive v1-delta on the wire, and lift
the shared connecting-role derivation out of the executors. First
decision inside this card (shared with the temporal-read card): how a
v1 construct enters the wire. **Decided: additive extension of
`regelspraak-ir-v0`** — extension fields are *optional* in the schema,
the v0 emitter never produces them at the pinned revision, every
extension carries a delta reference in `spec/CONSTRUCTS.md`, and the
eventual `regelspraak-ir-v1` format bump is reserved for non-additive
cleanup (renames; dropping `zetVeld`/`joinVeld`). Recorded normatively
in `spec/IR-SPEC.md` (method) and `spec/schema/README.md`.

Then, on that mechanism:

- A `RolBinding{feittype, rol}` node (IR-SPEC ch. 2.6): a symbolic
  reference to one declared role of one declared fact type.
- `ResolvedVerdeling` gains optional `verbindendeRol` (the recipient
  role of the connecting relation, ch. 4.1's wart); `Functie` gains
  optional `bronRol` (the role whose filler instances form the folded
  set). WF-047: a binding resolves and its filler types agree with the
  node's declared source/target types (non-reciprocal binary fact type).
  WF-048: a *bindingless* allocation or instance fold is well-formed
  only if the connecting relation is uniquely derivable from the type
  pair — the former runtime fault becomes well-formedness.
- `ResolvedFeitCreatie` gains the optional symbolic creation fields
  (`feittype`, `ankerRol`, `doelRol`; `Tussenstap` gains an optional
  role binding), and WF-028 — OPEN until now, blocked on this delta —
  gets its verifier check over the symbolic form plus a negatief case.
  Creation *execution* stays out of scope; WF-028's executable golden
  witness lands with the future creation-semantics card, recorded
  honestly.
- `ref/` and `backends/clingo` consume the explicit binding when
  present and fall back to the derivation for bindingless v0 dumps;
  the shared `Eval.connecting_role` becomes fallback-only.
- The witness is the **first hand-written anchor case**
  (`corpus/golden/anchor/`): two fact types over the same object-type
  pair — the exact shape the derivation cannot resolve — with an
  allocation and an instance fold carrying explicit bindings, and
  absolute expected values replayed by gates 5 and 6.

Acceptance: gates 1–6 green; the anchor case replays through ref and
clingo with exact expectation match; `corpus/negatief/wf047-*`,
`wf048-*`, `wf028-*` are rejected citing their rules; existing goldens
byte-unchanged; `scripts/check-self-contained.sh` covers the new
anchor zone.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/CONSTRUCTS.md`, `spec/ASP-TRANSLATION.md`, `spec/schema/**`,
`spec/verifier/**`, `corpus/golden/anchor/**` (new),
`corpus/negatief/**`, `ref/**`, `backends/clingo/**`, `backlog/**`.

Out of scope: the emitter (pin-bump territory; the resolver's
`verbindendeRol` makes emitting the binding cheap later); creation
execution semantics (ch. 5.5) and its executable witness; the
navigation-path unification of row 4 (`navigatieRol`/`navigatiePad` →
role-step path) — the RolBinding node is its building block; rows 5
and 20 (role-membership conditions, uniqueness scope) ride the same
discipline later; reciprocal fact types in bindings (recorded).
