# jutterswet-mastsein

The **host over a multi-step chain** (WF-109, `spec/IR-SPEC.md` ch. 4.6 (q2);
`spec/RUNTIME-SEMANTICS.md` ch. 5.5 and ch. 5.8b worked example B). Its sister
`jutterswet-seinhijsing` pins the doel reading over ONE intermediate step; this
document pins that the reading is about the chain's END and not about how far
along it happens to stop, by putting TWO steps between the anker and the doel.

The question is real and not decorative: a guard has exactly two patterns to
hang on, so an executor that mistook "the object the chain reaches last" for
"the object the chain reaches next" would answer a different number here and
the same number there. The file is authored, not patched — `bron.rgs` through
the frontend, `ir.json` its byte-identical emission (gate 10).

## What makes the reading identifiable

`gehesen` is declared on **all four** types the creation touches, and the case
gives each a value that separates the four candidate readings:

| carried by | `gehesen` |
|---|---|
| V1 (Kustvak, the anker) | yes |
| P1 (Post, the FIRST intermediate step) | no |
| M1 (Mast, the SECOND intermediate step) | yes |
| S1, S3 (Sein) | yes |
| S2 (Sein) | no |

Read on the anker, the guard admits every sein reached (3). Read on the first
step, none (0). Read on the second step, every sein again (3). Read on the
doel — which is what the host field `Sein` says — S1 and S3 (2). Only the doel
reading answers 2.

## Hand-computed absolutes

The world: V1 (Kustvak, gehesen) → P1 (Post, not) → M1 (Mast, gehesen) → S1
(gehesen), S2 (not), S3 (gehesen).

- **`seinaantal` of M1** — the count of `seinen` of the mast, a plain §5.8 fold
  over an input relation: S1, S2 and S3, so **3**. It pins the population the
  creation draws from.
- **`vakseinaantal` of V1** — the count of `vakseinen`, the relation the
  creation makes. The chain runs V1 → posten → P1 → masten → M1 → seinen →
  {S1, S2, S3}; the guard, hosted on `Sein`, admits S1 and S3 and drops S2, so
  two links are created: **2**.
- **consistent** — no consistency rule, and the created relation is to-many on
  the sein side, so no second-filler fault can arise.

## What breaks it

The same three moves as its sister, with one extra: hosting the guard on
`Mast` or on `Post` names an intermediate step, which is an object the chain
walks THROUGH and no pattern carries — WF-109's zero flank, witnessed at
`corpus/negatief/wf109-gastheer-derde-type` over the one-step chain.
