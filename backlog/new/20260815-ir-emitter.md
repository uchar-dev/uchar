# The `--ir` emitter (cross-repo: lands in regelspraak-java)

Goal: `Resolver --ir out.json` in `regelspraak-java`: a faithful JSON dump
of the v0 in-memory IR — the model-layer declarations, every `Resolved*`
form, and the full `Expr`/`Conditie` trees — warts included, Dutch v0
names as-is (the mirror; renaming is v1's job). This is the artifact the
whole Phase-1 chain hangs on: it unblocks the JSON schema, the verifier,
and the golden corpus.

Acceptance: the emitter runs on at least one repo-example source and
produces well-formed JSON covering every node kind that source exercises;
`regelspraak-java`'s own gate set stays green (its Makefile targets);
the emitter is purely additive — no behaviour change to resolver,
transpilers, or runtime.

May touch (in regelspraak-java, on a branch there): a new emitter
class/package + CLI wiring; nothing in this repo except `backlog/`. The
card and its Result live here; no coordination artifacts land in
regelspraak-java.

Out of scope: the JSON schema (own card here, follows the dump shape);
v1 renames or structural deltas; emitting corpus files into this repo.
