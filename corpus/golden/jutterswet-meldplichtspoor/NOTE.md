# jutterswet-meldplichtspoor

The **folded-position witness**: a `Fold` whose ACCUMULATOR is a declared
`SumType` over a case-supplied LIST PARAMETER whose ELEMENTS are a declared
`SumType`. Both halves ran before, but only apart — a sum-typed accumulator
over an object attribute of numeric elements
(`jutterswet-penningstaffel-catala`), and a numeric accumulator over a list
parameter (`jutterswet-kinderdeler`). This golden is the composition, and what
it carries is a normative position that a sequence of events updates: a
reporting duty that starts open with a term, is discharged by a report in time,
and is breached when the term runs out first.

## What it declares

| Thing | Form |
|---|---|
| domain `meldstatus` | `SumType`: `Openstaand(dagen)`, `Vervuld`, `Geschonden` |
| domain `gebeurtenis` | `SumType`: `Dagverloop(dagen)`, `Melding` |
| parameter `meldreeks` | `ListType` of `gebeurtenis` — the event register, case input |
| attribute `Jutter.meldpositie` | `meldstatus` — the rule target |

Both domains satisfy WF-087 (each carries one payload-bearing constructor, so
neither normalises to an enumeration) and WF-060 (whole-number payloads).
WF-065 admits the declared `SumType` as a list element; WF-070 admits the
`SumType` accumulator.

The step is a closed lambda body (WF-069), a `Match` on the accumulator with a
nested `Match` on the element and an `IfThenElse` under it:

```
init = Openstaand(30)

λ (stand, gebeurtenis).
  match stand with
  | Openstaand resterend ->
      (match gebeurtenis with
       | Dagverloop dagen -> if dagen <= resterend
                             then Openstaand (resterend - dagen)
                             else Geschonden
       | Melding          -> Vervuld)
  | Vervuld    -> Vervuld
  | Geschonden -> Geschonden
```

`Vervuld` and `Geschonden` are **absorbing**: once the position has settled, no
later event moves it — which is what makes a breach survive a late report
instead of being repaired by it. The `IfThenElse` condition reads only the two
binders, so WF-077 holds and the branch complement stays exact.

## Hand-computed absolutes (term = 30 days, `init = Openstaand(30)`)

**case1 — `[Dagverloop(12), Melding]`** (reported in time)

| step | accumulator in | event | accumulator out |
|---|---|---|---|
| 0 | — | — | `Openstaand(30)` |
| 1 | `Openstaand(30)` | `Dagverloop(12)` | 12 ≤ 30 → `Openstaand(18)` |
| 2 | `Openstaand(18)` | `Melding` | **`Vervuld`** |

**case2 — `[Dagverloop(20), Dagverloop(14), Melding]`** (reported too late)

| step | accumulator in | event | accumulator out |
|---|---|---|---|
| 0 | — | — | `Openstaand(30)` |
| 1 | `Openstaand(30)` | `Dagverloop(20)` | 20 ≤ 30 → `Openstaand(10)` |
| 2 | `Openstaand(10)` | `Dagverloop(14)` | 14 ≤ 10 is false → `Geschonden` |
| 3 | `Geschonden` | `Melding` | **`Geschonden`** (absorbing) |

**case3 — `[]`** (nothing happened yet): the list is present with no elements,
so the fold base fires and the result is the init — **`Openstaand(30)`**, the
full term.

**case4 — `[Dagverloop(31)]`** (never reported): 31 ≤ 30 is false →
**`Geschonden`**.

Every case pins its outcome absolutely as an `attributeValue` with a
`constructor`/`payload` value, plus `consistent`.

## Gates

Gate 5 (`ref`) and gate 6 (clingo) both replay all four cases against exactly
these hand-written values — ref ≡ clingo, never parity alone. Gate 3 passes the
document verifier-clean; gate 2 validates and round-trips the dump; gate 4
validates the four case files. Gate 7 (the no-op guard) fails on all four: the
target is absent on the raw input, so no expectation survives a do-nothing
executor and nothing is added to the survivor list. Gate 12 (Drools)
auto-refuses this golden per form — `Datatype:SumType (unknown datatype node)`
— which the gate report prints grouped with its reason; the value-divergence
list is untouched.

On the clingo side the fold is index recursion over the reified list, the
nested match on a bound binder is functor unification (no negation as failure),
and the constructor term rides straight into the write of the sum-typed target.
An absent register derives no list-presence fact and the position stays empty in
both executors, which is why no case pins that corner: it would survive the
no-op guard.

## The boundary this golden records

The sum-type payload fence admits a number, a day-precision date or another
declared sum type, and the record-field fence admits that set plus records.
Neither admits a reference to an object. **So the parties a duty runs between
cannot sit inside the position value.** The value can carry the term and the
outcome; it cannot carry who owes and to whom.

What this golden does instead is what the landed kernel allows: the **holder is
the rule subject** — the Jutter the attribute hangs on — and the
**counterparty is fixed by the rule itself**, nowhere named in the value. The
same fence shows a second time here from the other side: the event register is a
document-level list, so the position is indexed per subject while the evidence
that moves it is not indexed at all.

That is the sharpest argument a later kernel extension has to answer. A position
that is genuinely relational — one duty per pair of parties, several such duties
on one holder at once, each with its own term — is not expressible today, and
not because the fold, the match or the sum type fall short: they carry the whole
state machine above without a single extension. It falls short on **identity**.
The residual question is therefore narrow and testable: what has to become a
payload (or a field) before a position can name its own parties, and what does
that cost the acyclicity that bounds term depth for the ASP encoding. The text
half of the same fence is already pinned negatively by
`corpus/negatief/wf060-text-payload`.
