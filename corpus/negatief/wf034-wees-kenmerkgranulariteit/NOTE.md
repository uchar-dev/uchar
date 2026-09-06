Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/kenmerkGranulariteit/verjaart: kenmerkGranulariteit key 'verjaart' names no
    declared characteristic of 'Vondst': the side table resolves against no declaration

**What the table is.** An object type declares its members in two places —
`attributen` (name to datatype) and `kenmerken` (a name list) — and hangs
eight per-member FACET maps beside them, each keyed by a member name. Nothing
required those keys to name a declared member. Every key is a reference, and it
is more than that: it is the v0 DENORMALISATION of a facet that v1 carries on
the member node itself (`spec/CONSTRUCTS.md` row 11 — "each v0 map entry folds
into the corresponding member node"), so the key IS that member's identity,
written out beside the declaration instead of on it. A key naming no member
resolves against no declaration; and because an identity is carried VERBATIM
(ch. 2.1's carriage rule, not its matching rule), the key must be the member's
declared spelling, case and whitespace included.

**The harm is a silent semantic change, not a missing value.** A key spelled `verjaart` where the declared characteristic is `verjaard` says
"this characteristic is time-dependent" about a characteristic that does not
exist, and leaves the declared one silently NON-temporal — the characteristic
half of the sharpest case, and the one the sixth wave was argued from.

**Who reads this table** (measured 2026-09-05 across the four consumers):
In the verifier: WF-043's whole-period qualifier in both `walk_expr` and
`walk_cond`, the temporal prologue of `expression_layer`, and WF-086's
defaults-target rule. In `ref`: the case loader and the evaluator. In the ASP
backend: the translator, twice. In the Drools backend: both transpiler branches,
the contract validator, the fact-model generator and the case runner.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now. The
renaming instrument does not apply cleanly here and is recorded as such: all
**9** live golden keys are already refused when renamed (0 of 9 clean), because
the characteristic's own `gedurende` reads lose their temporal ground and
WF-043 reports them. The ADDED key is the shape no rule caught.

**Executor observability.** Renaming each live key and replaying every case of its golden gave **27**
measurements. **26** are loud in both OCaml executors (`CASE ERROR: … a
characteristic timeline on a non-temporal characteristic`, or a fault on
`gedurende`). The twenty-seventh **DIVERGES, on an anchor golden**: on
`corpus/golden/anchor/jutterswet-dubbelwacht` case3, renaming `Jutter.aangesteld`
makes `ref` fault (`EVALUATION FAULT: gedurende over "aangesteld", which is not a
temporal characteristic (WF-055)`) while the ASP backend replays the case to its
pinned expectations. One document, two executors disagreeing about whether it
can be run at all.

*With one caveat, recorded 2026-09-06 rather than left implied:* the carrying
document is ill-formed independently of this clause, and already was before the
card — WF-055 refuses a `gedurende` over a non-temporal characteristic, which is
exactly why this table's rename instrument reads 0 of 9. So the divergence sits
behind a door the verifier already holds shut: it is a second-line finding, not
this arm's justification. The asymmetry underneath it — the ASP translator's
ch. 3.6 clip arm lacking the temporality gate its pro-rata and day-count sisters
carry — is its own card,
`backlog/new/20260906-clingo-gedurende-spiegelpoort.md`.

*Measured breadth:* zero documents lose anything. All 24 `kenmerkGranulariteit`
keys the four zones held before this card name a declared characteristic.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "kenmerkGranulariteit" ...` line
from `wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case
turns clean — 523 documents compared, no report elsewhere. Since 2026-09-06
this arm carries **two** witnesses and the mutant frees both: this one, the
orphan that names nothing, and
`corpus/negatief/wf034-zijtabel-kapitalisatiewees`, the key that names the
right characteristic in the wrong spelling. They separate under the narrower
mutant: give this one call site its case-insensitive predicate back
(`has_kenmerk ot`) and the capitalisation witness turns clean **alone**, this
one staying red — 523 documents compared, one verdict moved.

**Why the spelling and not just the name** (measured 2026-09-06). The consumers
do not agree on how they look this facet up: the verifier, `ref` and the ASP
backend resolve the key case-insensitively, the bean-model executor does not —
it reaches this table through raw case-sensitive map lookups at **20** sites.
So a key differing from its declaration in capitalisation alone is found by
three consumers and missed by the fourth, which drops the document out of the
differential without a word. The witness for that flank is
`corpus/negatief/wf034-zijtabel-kapitalisatiewees`, on this table.
