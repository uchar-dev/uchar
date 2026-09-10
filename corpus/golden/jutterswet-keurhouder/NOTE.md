# jutterswet-keurhouder

The **characteristic-check owner** witness (WF-100, `spec/IR-SPEC.md` ch. 4.6
(q1); `spec/RUNTIME-SEMANTICS.md` ch. 5.8b). It is the corpus's first golden
carrying an `eigenaar` on a characteristic check at all: before this document
the field appeared in the negatief zone only, so no gated, replayed case pinned
what a reader does with it.

**The delimitation, stated because it matters more than the "first".** The
shape this golden carries — owner equal to the subject's own type — is one the
v0 producer never writes: the frontend sets the field only on a fact-creation
guard where the doel differs from the anker. And the shape the producer DOES
write cannot become a golden under this decision, because two of the three
executors fault on it (`RUNTIME-SEMANTICS.md` ch. 5.8b rule 2). So the only
owner shape that replays everywhere has no producer, and the producer's shape
has no replay: this document pins that the form loads, lowers and replays
identically in all three executors, not that the decision changed its answer.
What pins the DECISION is the negatief witness (the verifier half) and the two
executor suites (the executor half).

Two `Vondst` rules read the same characteristic `gekeurd` under both
polarities, each naming `Vondst` — the subject's own type — as the check's
owner. A second object type, `Jutter`, is declared beside it and carries a
characteristic of its own; it is what makes the owner a CHOICE rather than the
only name available, and it is the type the negatief witness
`wf100-eigenaar-verklaart-kenmerk-niet` puts in the field to break this
document.

## Hand-computed absolutes

| instance | type   | input characteristic | afslag       | keurstatus   |
|----------|--------|----------------------|--------------|--------------|
| V1       | Vondst | `gekeurd`            | `zeeveiling` | leeg         |
| V2       | Vondst | (none)               | leeg         | `onbekeurd`  |
| J1       | Jutter | `penninggerechtigd`  | n/a          | n/a          |

`afslag` is written exactly where `gekeurd` holds, `keurstatus` exactly where
it does not, so the two rules partition the two Vondsten and neither instance
gets both. `J1` is untouched by every rule: no rule has `Jutter` as its
subject, and `Jutter`'s own `penninggerechtigd` is expected back unchanged.
`gekeurd` is not declared time-dependent on either type, so both reads are
scalar.

ref = clingo over every expectation (gate 5 ≡ gate 6). NOOP-REFUTING: V1's
`afslag` and V2's `keurstatus` are computed and absent from the raw input, so a
do-nothing run mismatches both.

## What it pins about the owner

The field is authoritative (maintainer decision 2026-09-10). Two twists, each
measured on this document:

- `eigenaar` -> `"Jutter"` (a declared type that does not declare `gekeurd`):
  the verifier refuses it, WF-100. That is the negatief witness.
- `eigenaar` -> a type that DOES declare `gekeurd` but is not the subject's:
  verifier-clean, and both executors refuse it at run time — the reading that
  used to answer from the subject. The document that shows it cannot live in
  a corpus zone (golden replays must pass, red cases must fault in every
  executor that carries the form, and the Drools backend drops the rule as a
  transpiler skip rather than faulting), so its witnesses are the executors'
  own suites: `ref/test/test_ref.ml` (ch. 5.8b) and
  `backends/clingo/test/test_clingo.ml`.
- `eigenaar` -> `null` on both rules: the answers do not move. An absent owner
  and an owner naming the subject's own type are the same reading, which is
  why every other golden in the corpus is unaffected by the rule.
