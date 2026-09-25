# jutterswet-schouwkalender — an acyclic day-kind reference chain

The positive witness for the day-kind stratum-0 answer (`spec/IR-SPEC.md`
par. 5 / ch. 3.7): a day-kind definition **may** reference another day
kind, as long as the reference graph stays acyclic.

## The day kinds

- `schouwdag` — a pure date condition (in the inspection season, a
  comparison of two date literals), the shape of the one pre-existing
  day-kind witness (`jutterswet-vormenkabinet`);
- `hoogschouwdag` — its condition **references** `schouwdag` (a
  `Predicate` of kind `dayKind`). The reference graph is the single edge
  `hoogschouwdag -> schouwdag`; acyclic, so WF-085 is satisfied, and both
  conditions are pure, so WF-031 is satisfied.

## Why the replay case touches only the ordinary rule

Day-kind predicates are **not evaluated** by any executor — ref and clingo
refuse the predicate kind (a day-kind is stratum-0 input, defined outside
the dependency graph, `spec/RUNTIME-SEMANTICS.md` ch. 6). The day-kind
definitions here are therefore verifier-only content. The replay case
exercises the golden's ordinary rule — the `penninggerechtigd`
characteristic on the `vondstdrempel` — exactly as
`jutterswet-penningdrempel` does; it never mentions a day kind, so ref and
clingo replay it to the pinned expectations without ever touching the
day-kind conditions. This keeps the golden a full member of gates 5, 6 and
12 while still witnessing the acyclic day-kind reference.
