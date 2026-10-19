# jutterswet-bergingstoeslag

The **Let-in-a-defaults-consequence witness**: a hand-written `ir.json`
with no `source.rgs`, whose job is to bring one corpus-uncovered path under
the gates — a ch. 4.10 `Let` sitting in the **consequence** of a ch. 7
`prioritizedDefaults` default. Every other `Let` in the corpus lives in a
rule value, and every other defaults consequence is `Let`-free, so no
golden made the two meet: the executors' defaults lowering and their
value-level binding were each covered, their composition was not.

## The shape

One `Jutter` with four attributes — `bergingsgrondslag` (2 dec, input),
`aantal geborgen wrakken` (whole, input) and two derived targets, each
the target of its own `prioritizedDefaults` group with two defaults in
descending priority:

```
bergingstoeslag:
  1. if zijn aantal geborgen wrakken >= 2 then
         let bergingsgrond = zijn bergingsgrondslag in bergingsgrond + 100,00
  2. always                                 -> 0,00   (het bodembedrag)

bergingsforfait:
  1. always -> let bergingsgrond = zijn bergingsgrondslag in 42,00
  2. always                                 -> 0,00   (het bodembedrag)
```

`bergingstoeslag`'s default 1 carries exactly the shape an emitter writes
for a variable part: `Let b = <read> in <arithmetic over b>`, the binder
read as a `BinderRef` in the body and typed as the bound value is — which
is what the emitter does and what every other `Let` in the corpus shows.
Default 2 is unconditional (a `Compound` under `all` over zero parts), so
the group always decides and there is a member for a fall-through to land
on — without it the interesting cases would end in "nothing written"
rather than in a decision.

`bergingsforfait`'s default 1 is the same shape with the binder **never
read**: the second axis, below.

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

What `bergingstoeslag` alone does **not** separate is call-by-need from
the strict binding this chapter stated before the L-unification: on all
three cases the two agree, because the body reads the binder and the two
semantics differ only in **where** the barrier sits. `bergingsforfait` is
that second axis, over the same composition:

- its default 1 binds `bergingsgrondslag` and **never reads the binder**,
  so ch. 10.6 clause 3 evaluates the value not at all and the `Let`'s
  outcome IS the body's — 42,00, on every case, *including* case3 where
  the bound attribute is absent;
- under the strict binding the same default would evaluate leeg at the
  **bind** place, not decide, and the group would fall through to the
  floor — 0,00. Measured, not assumed: the same group with the body
  changed to read the binder (`bergingsgrond plus 0,00 maal
  bergingsgrond`, which forces the need without moving the arithmetic)
  gives `ref` **0,00** on case3 and 200,00 on case1.

  Honest about what this axis does **not** pin: under call-by-need
  `bergingsforfait`'s default 1 decides on every case, so its floor
  (default 2) is reachable on **no** pinned input — it is there purely so
  the counterfactual strict executor lands on a DECISION instead of on
  nothing-written. Measured: with that member deleted all three cases
  still pass through `ref`. Contrast `bergingstoeslag`, whose floor **is**
  pinned — delete it and case2 and case3 both mismatch (`leeg` where 0,00
  is expected). So a change that damages or drops `bergingsforfait`'s
  floor will not be caught by a gate; a change to its default 1 will.

So 42,00 versus 0,00 on case3 is the L-versus-S discriminant inside the
ch. 7 fall-through, and 0,00 versus 100,00 on `bergingstoeslag` is the
binding-versus-substitution one. Both need the defaults composition to
be observable at all.

## Hand-computed absolutes (each measured through `ref/` before pinning)

| case | grondslag | wrakken | toeslag default 1 | bergingstoeslag | bergingsforfait |
|------|-----------|---------|-------------------|-----------------|-----------------|
| case1-grond-gelezen   | 200,00   | 3 | decides: 200,00 + 100,00 | **300,00** | **42,00** |
| case2-onder-de-drempel| 200,00   | 1 | unjustified (1 < 2)      | **0,00** (floor) | **42,00** |
| case3-lege-grondslag  | *absent* | 3 | justified, but leeg — the barrier at the read | **0,00** (floor) | **42,00** (clause 3: unread) |

All three are `consistent`: a leeg consequence is a fall-through, not a
fault, and the floor never conflicts with anything.

## The differential square

`ref` ≡ `clingo` over all nine expectations (gate 5 ≡ gate 6), anchored to
these hand-written absolutes; the Drools backend PLAYS the golden (gate
12), so ref ≡ clingo ≡ Drools holds on this shape too. NOOP-REFUTING:
both targets are derived and absent from the raw input in every case,
so a do-nothing run mismatches all three (gate 7) — including case3,
whose 0,00 is a decision by the floor, never a silence.
