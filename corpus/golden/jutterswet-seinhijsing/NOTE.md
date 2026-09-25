# jutterswet-seinhijsing

The **host** witness (WF-109, `spec/IR-SPEC.md` ch. 4.6 (q2);
`spec/RUNTIME-SEMANTICS.md` ch. 5.5 and ch. 5.8b worked example B). It is the
corpus's first golden carrying the shape the RegelSpraak frontend actually
emits a `host` on: the guard of a §13.4.7 fact creation that names a
characteristic of the DOEL. Until 2026-09-19 that shape could not be a golden
at all — the verifier admitted it, both Drools branches played it, and `ref`
and the ASP backend refused it — so the only owner shape that replayed
everywhere (`jutterswet-keurhouder`, owner equal to the subject's own type) was
one no producer writes. This document closes that gap: source, wire and all
four executors, one answer.

The file is authored, not patched: `bron.rgs` goes through the frontend and
`ir.json` is its byte-identical emission (gate 10).

## What makes the reading identifiable

`gehesen` is declared on **all three** types the creation touches — the anker
`Kustvak`, the intermediate step `Post`, and the doel `Sein` — and the case
gives them values that separate the three readings by NUMBER and not merely by
name:

| carried by | `gehesen` |
|---|---|
| V1 (Kustvak, the anker) | yes |
| P1 (Post, the intermediate step) | no |
| S1 (Sein) | yes |
| S2 (Sein) | no |

So a guard read on the anker admits every sein it reaches (2), a guard read on
the intermediate step admits none (0), and the normative reading — the doel,
because the host field says `Sein` — admits S1 alone (1). An answer of 1 is
therefore not agreement with the doel reading, it IS the doel reading.

The second creation is the same law under the opposite polarity: "indien het
sein **niet** gehesen is", hosted on `Sein` as well, over a distinct relation.
It admits S2 alone. The two together account for every sein exactly once, which
is the property a mis-read guard cannot fake.

## Hand-computed absolutes

The world: one `Kustvak` V1 (gehesen) with one `Post` P1 (not gehesen) carrying
two `Sein`en, S1 (gehesen) and S2 (not).

- **`seinaantal` of P1** — the count of `seinen` of the post, a plain §5.8 fold
  over an input relation: S1 and S2, so **2**. It pins the population the
  creation draws from, independently of what the creation does.
- **`vakseinaantal` of V1** — the count of `vakseinen`, the relation the first
  creation makes. The chain runs V1 → posten → P1 → seinen → {S1, S2}; the
  guard is hosted on `Sein`, so it is asked of S1 (gehesen, admitted) and of S2
  (not, dropped). One link is created, so **1**.
- **`duisteraantal` of V1** — the count of `duisterseinen`, the relation the
  second creation makes, over the same chain with the guard negated: S1 is
  dropped, S2 is admitted. So **1**.
- **consistent** — no consistency rule and no second filler on a to-one role;
  the two created relations are distinct and both to-many on the sein side.

The two counts sum to `seinaantal`, 1 + 1 = 2, which is the partition the two
polarities state.

## What breaks it

Move the first creation's host field and the document stops being this one:
to `Post` it names an object no pattern carries
(`corpus/negatief/wf109-gastheer-derde-type`), to `Kustvak` it reads the anker
and `vakseinaantal` becomes 2, and emptied it reads the anker too. Nest the
guard in a composite, or make it navigate, and WF-109's two scope fences refuse
it (`wf109-doelgastheer-samengesteld`, `wf109-doelgastheer-genavigeerd`).
