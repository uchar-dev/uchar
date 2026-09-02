# jutterswet-bergingstoeslag

The **Let-in-a-defaults-consequence witness**: a hand-written `ir.json`
with no `bron.rgs`, whose job is to bring one corpus-uncovered path under
the gates — a ch. 4.10 `Let` sitting in the **consequence** of a ch. 7
`prioritizedDefaults` default. Every other `Let` in the corpus lives in a
rule value, and every other defaults consequence is `Let`-free, so no
golden made the two meet: the executors' defaults lowering and their
value-level binding were each covered, their composition was not.

## The shape

One `Jutter` with three attributes — `bergingsgrondslag` (2 dec, input),
`aantal geborgen wrakken` (whole, input) and `bergingstoeslag` (2 dec),
the target of one `prioritizedDefaults` group with two defaults in
descending priority:

```
bergingstoeslag:
  1. if zijn aantal geborgen wrakken >= 2 then
         let bergingsgrond = zijn bergingsgrondslag in bergingsgrond + 100,00
  2. always                                 -> 0,00   (het bodembedrag)
```

Default 1 carries exactly the shape an emitter writes for a variable
part: `Let b = <read> in <arithmetic over b>`, the binder read as a
`BinderRef` in the body. Default 2 is unconditional (a `Samengesteld
ALLE` over zero parts), so the group always decides and there is a
member for a fall-through to land on — without it the interesting cases
would end in "nothing written" rather than in a decision.

## Why this discriminates

The three cases separate the three ways the group can end:

- **case1** — the justification holds and the binder's value is filled,
  so the `Let` computes and the highest default wins;
- **case2** — the justification fails, so default 1 never decides and
  the floor writes 0,00. The `Let` is not the decision here, which is
  what keeps case1 from passing on a group that ignores its condition;
- **case3** — the justification holds but `bergingsgrondslag` is
  **absent**. The body reads `bergingsgrond`, so ch. 10.6 clause 2 fires
  the leeg-barrier **at that read** and the whole `Let` is leeg; a
  leeg-evaluating consequence does not decide (ch. 7), so the group
  falls through to the floor.

Case3 is the load-bearing one and its value is not free-standing: the
same case over the same group with the `Let` **removed** — the read
inlined into the `plus`, which is what an inlining lowering produces —
was measured through `ref/` as **100,00**, because a bare `plus`
coalesces the absent read to 0. Barrier 0,00 versus coalesce 100,00: an
executor that treats the binding as substitution answers this case
wrong, it does not merely miss a subtlety.

## Hand-computed absolutes (each measured through `ref/` before pinning)

| case | grondslag | wrakken | default 1 | bergingstoeslag |
|------|-----------|---------|-----------|-----------------|
| case1-grond-gelezen   | 200,00   | 3 | decides: 200,00 + 100,00 | **300,00** |
| case2-onder-de-drempel| 200,00   | 1 | unjustified (1 < 2)      | **0,00** (floor) |
| case3-lege-grondslag  | *absent* | 3 | justified, but leeg — the barrier at the read | **0,00** (floor) |

All three are `consistent`: a leeg consequence is a fall-through, not a
fault, and the floor never conflicts with anything.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
these hand-written absolutes; the Drools backend PLAYS the golden (gate
12), so ref ≡ clingo ≡ Drools holds on this shape too. NOOP-REFUTING:
`bergingstoeslag` is derived and absent from the raw input in every case,
so a do-nothing run mismatches all three (gate 7) — including case3,
whose 0,00 is a decision by the floor, never a silence.
