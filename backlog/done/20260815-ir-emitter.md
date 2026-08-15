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

Result: implemented in `regelspraak-java` on branch `ir-emitter` (one
commit, parent = the pinned `b85dff3`; main untouched; no coordination
artifacts placed there). `Resolver --ir <out.json> <file.rgs...>` plus a
`make ir` target: a faithful v0 JSON dump (`"formaat":
"regelspraak-ir-v0"`, Dutch field names verbatim, two-space indent,
byte-stable across runs) of the complete resolved model — all declaration
registries incl. the Objecttype side-maps and full unit records, every
`Resolved*` list, full `Expr`/`Conditie` trees via exhaustive sealed
switches (a new subtype is a compile error), diagnostics. Hand-rolled JSON
writer, zero new dependencies; purely additive (~780 LOC new, ~30 LOC CLI
dispatch mirroring `--facts`/`--graaf`, incl. the disk discipline: an
unresolvable model writes nothing, exits 1 — demonstrated). Gates on the
branch, real results: `make test` 536/536 (1 assumption-skip); `mvn -o
clean verify` exit 0 (the full differential suite); `make scenario-test`
30/30 PASS + marker-exit-3; `make facts` PASS; `make
verify-negatief-corpus` all verdicts match. `make verify-corpus` is red —
identically red on main: the non-redistributed specification corpus is
absent on this machine (environmental, no regression). Not run: `make
engine` and the service-module suites (they consume the installed jar via
a separate build; the root-module compilation they depend on is covered by
the green `mvn verify`) — run them at merge if desired. Deliberate
limitations recorded in the emitter javadoc: declared-type references
emitted by name (not inlined); scenario IR is engine-layer and not on the
model, so not emitted. The smoke slice exercised the model layer,
ResolvedRegel/Verdeling and the main Expr kinds; the remaining node kinds
are compiler-enforced but not runtime-exercised yet — the golden corpus
will cover them. The `ir-emitter` branch awaits the maintainer's merge in
regelspraak-java; after that merge the pin can move and the JSON-schema
card here is unblocked.
