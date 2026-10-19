# synth3-takbarriere

The **unread-binder witness** (probe B1 of the let-lowering card,
2026-09-01): a hand-written `ir.json` with no `source.rgs`, like its
`synth3-*` siblings, whose only job is to separate **call-by-need** from
the strict binding at corpus level (`RUNTIME-SEMANTICS.md` ch. 10.6
clause 3).

## The shape

```
heffing := let g = zijn grondslag in
           let o = zijn opslag in
             if g > drempel then basisheffing
                            else basisheffing + o
```

`o`'s only read sits in the `else` branch. The condition reads `g` and a
parameter, so it satisfies WF-077 without touching `o`.

## Why this discriminates

Under **call-by-need** an unread binder evaluates *nothing*: when the
condition picks `then`, `o` is never read, its value (an absent
attribute) is never evaluated, and no barrier is raised — the target is
**written**. Under a **strict** binding the same `Let` would evaluate
`zijn opslag` at bind time, find it leeg, and empty the whole `Let` —
the target would **not** be written. J1 is exactly that case, so a
strict executor fails this golden.

J2 is the mirror image: the same absent `opslag`, but the `else` branch
*is* taken and it *does* read `o`, so the barrier fires and `heffing`
stays leeg. J1 and J2 differ only in `grondslag`, i.e. only in which
branch runs — which is what makes the pair a clean per-branch read
analysis, and closes the false-green window a translation that computes
branch reads too coarsely would leave open.

## Hand-computed absolutes

`drempel = 50,00`, `basisheffing = 10,00`.

| Jutter | grondslag | opslag   | branch | heffing                                   |
|--------|-----------|----------|--------|-------------------------------------------|
| J1     | 100,00    | *absent* | then   | **10,00** (`o` unread — no barrier)       |
| J2     | 20,00     | *absent* | else   | **leeg** (`o` read — the barrier fires)   |
| J3     | 20,00     | 5,00     | else   | 10,00 + 5,00 = **15,00**                  |

## The differential square

`ref` ≡ `clingo` over all four expectations (gate 5 ≡ gate 6), anchored
to these hand-written absolutes; the Drools backend PLAYS it (gate 12),
so ref ≡ clingo ≡ Drools. NOOP-REFUTING: J1 and J3 are computed and
absent from the raw input, so a do-nothing run mismatches them (gate 7)
— the case does not ride on J2's leeg alone.
