# jutterswet-keurhouder

The **characteristic-check owner** witness (WF-100, `spec/IR-SPEC.md` ch. 4.6
(q1); `spec/RUNTIME-SEMANTICS.md` ch. 5.8b). It is the corpus's first golden
carrying a `host` on a characteristic check at all: before this document
the field appeared in the negative zone only, so no gated, replayed case pinned
what a reader does with it.

**The delimitation, stated because it matters more than the "first".** The
shape this golden carries — owner equal to the subject's own type — is one the
v0 producer never writes: the frontend sets the field only on a fact-creation
guard where the doel differs from the anker. *Until 2026-09-19 the shape the
producer DOES write could not become a golden either, because two of the three
executors faulted on it* — so the only owner shape that replayed everywhere had
no producer, and the producer's shape had no replay. WF-109 (ch. 4.6 (q2))
closed that, and `jutterswet-seinhijsing` is the producer's own shape replaying
in all four executors. What this document pins is the other half, and it is
still its own: that an owner naming the subject's type loads, lowers and
replays identically everywhere, which is the reading an ABSENT owner has.
What pins the DECISION is the negative witness (the verifier half) and the two
executor suites (the executor half).

Two `Vondst` rules read the same characteristic `gekeurd` under both
polarities, each naming `Vondst` — the subject's own type — as the check's
owner. A second object type, `Jutter`, is declared beside it and carries a
characteristic of its own; it is what makes the owner a CHOICE rather than the
only name available, and it is the type the negative witness
`wf100-host-declares-characteristic-negated` puts in the field to break this
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

- `host` -> `"Jutter"` (a declared type that does not declare `gekeurd`):
  the verifier refuses it, WF-100. That is the negative witness.
- `host` -> a type that DOES declare `gekeurd` but is not the subject's:
  **WF-109 refuses it** (ch. 4.6 (q2), 2026-09-19) — the host names no object
  this position binds. *Until that rule the same twist was verifier-CLEAN and
  refused by both OCaml executors at run time, the reading that used to answer
  from the subject; the verdict moved to the verifier so that what stays on the
  wire is carried by all four.* The negative witness is
  `corpus/negative/wf109-host-outside-range`, which is exactly this twist.
  The executors still fault on the document, fail-fast, and their own suites
  keep asserting the sentence: `ref/test/test_ref.ml` (ch. 5.8b) and
  `backends/clingo/test/test_clingo.ml`.
- `host` -> a SUPERTYPE of `Vondst` that declares `gekeurd`: clean, and the
  answers do not move — satisfaction is extensional (`T ⊑ O`). Pointed at a
  strict SUBTYPE instead it is WF-109 again, on the partial-population flank
  (`corpus/negative/wf109-host-partial-population`): only some `Vondst`en are
  one.
- `host` removed from both rules: the answers do not move. An absent host
  and a host naming the subject's own type are the same reading, which is
  why every other golden in the corpus is unaffected by the rule.
