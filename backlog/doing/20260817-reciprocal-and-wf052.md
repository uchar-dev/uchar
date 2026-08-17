# Close the recorded edges: reciprocal fact types bind, and to-one connecting relations get a single writer (WF-052)

Goal: the two edges earlier verification rounds recorded, closed. (1)
**Reciprocal fact types** (wederkerig, one role, WF-011/013): folds and
role bindings over them are today unverifiable on every route — the
binding resolver and the connecting-relation derivations only match
two-role fact types. All five derivation sites learn the single-role
shape (the bound and opposite role are the same role): the verifier's
`resolve_binding` and `connecting_candidates`, and the executors'
`connecting_role`, `connecting_pair` and `role_by_name` — clingo
inherits them all. Witnessed by a new anchor
(`jutterswet-maatschap`): a bound AANTAL fold and a bindingless SOM
fold over a reciprocal `maatschap`, both executors. (2) **WF-052** (the
WF-026/WF-050 single-writer family): a connecting relation with a
to-one role MUST have at most one creating writer (fact creations
naming it; object creations whose unique back-link it is) — closing
statically the recorded divergence where two 1:1 object creations made
`ref/` fault while clingo silently held both links.

Acceptance: all eight gates green; the maatschap anchor replays in both
executors; WF-052 has verifier check + passing golden
(`jutterswet-penninguitgifte`) + failing negatief; the WF-013 row gets
its first reciprocal golden; new negatief for the reciprocal binding
path (wrong filler).

May touch: `spec/verifier/lib/checks.ml`, `ref/lib/eval.ml`,
`ref/lib/case.ml` (resolve_role), `corpus/golden/anchor/jutterswet-maatschap/`
(new), `corpus/golden/anchor/README.md`, `corpus/negatief/` (two new),
`spec/IR-SPEC.md` (WF-052 text + index rows 013/047/052),
`spec/RUNTIME-SEMANTICS.md` (one clause), `backends/clingo/README.md`
(limitation update), `spec/CONSTRUCTS.md` (if a row carries the
reciprocal note), `backlog/`.

Out of scope: reciprocal creations' own witness (the derivations now
find the relation, recorded untested); symmetric-closure semantics for
reciprocal links (links are read as stored, the existing convention);
recursive rule groups.
