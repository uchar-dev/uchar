# jutterswet-bakensein

The **supertype host** (WF-109, `spec/IR-SPEC.md` ch. 4.6 (q2);
`spec/RUNTIME-SEMANTICS.md` ch. 5.8b). The spec says an object of type `T`
satisfies host `O` iff `T ⊑ O`, so a host naming a SUPERTYPE of the doel is
legal and must be carried. This golden is that sentence's only corpus witness,
and it exists because the sentence was false in two of the four executors when
it was written.

**Hand-written IR, deliberately: there is no `bron.rgs`.** The RegelSpraak
frontend tags a fact-creation guard with the doel's own type and never with an
ancestor, so no source can produce this document. Writing one would mean
changing the frontend, which would mean a toolchain re-pin, for a form the
producer has no sentence for — so the wire carries it directly instead. Goldens
without a source are the corpus's normal way of pinning a form no frontend emits
(`corpus/golden/README.md`).

## What makes the reading identifiable

`Sein` is a subtype of `Baken`, which declares `gehesen`; the guard is hosted on
**`Baken`**, so it is satisfied by the doel through inheritance and by nothing
else. `Kustvak` (the anker) and `Post` (the intermediate step) each declare a
`gehesen` of their own, on chains of their own, so the three readings again give
three different numbers:

| carried by | `gehesen` | via |
|---|---|---|
| V1 (Kustvak, the anker) | yes | its own declaration |
| P1 (Post, the intermediate step) | no | its own declaration |
| S1 (Sein) | yes | inherited from `Baken` |
| S2 (Sein) | no | inherited from `Baken` |

Anker reading 2, intermediate reading 0, **doel reading 1**. And the host cannot
reach the anker: no declared type is below both `Kustvak` and `Baken`, so the
populations do not overlap and WF-109's ambiguity flank stays silent — which is
exactly what makes this document legal where
`corpus/negatief/wf109-gastheer-ambigu-supertype` (a common supertype over BOTH)
is not.

## Hand-computed absolutes

The world: V1 (Kustvak, gehesen) → P1 (Post, not gehesen) → S1 (Sein, gehesen),
S2 (Sein, not).

- **`seinaantal` of P1** — the count of `seinen` of the post: S1 and S2, so
  **2**. It pins the population the creation draws from.
- **`vakseinaantal` of V1** — the count of `vakseinen`. The chain runs V1 →
  posten → P1 → seinen → {S1, S2}; the guard, hosted on `Baken`, is asked of S1
  (a `Baken` by inheritance, carrying `gehesen` → admitted) and of S2 (dropped).
  One link: **1**.
- **consistent** — no consistency rule; the created relation is to-many on the
  sein side.

## What it was built to catch

Measured 2026-09-19 before the fix: verifier **PASS**, `ref` **PASS** (1), ASP
backend **PASS** (1), and **both** Drools branches refusing the whole creation
by name (`voorwaarde niet naar anker of doel te lowereren`). Both branches asked
"is the host the same object as the pattern's type?" by identity, where the spec
asks "is the pattern's type a subtype of the host?". Two executors answering and
two refusing one verifier-clean document is the split WF-109 exists to close, so
the fix and this golden landed together.
