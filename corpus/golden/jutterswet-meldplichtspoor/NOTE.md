# jutterswet-meldplichtspoor

The **folded-position witness**: a `Fold` whose ACCUMULATOR is a declared
`SumType` over a case-supplied LIST PARAMETER whose ELEMENTS are a declared
`SumType`. Both halves ran before, but only apart — a sum-typed accumulator
over an object attribute of numeric elements, and a numeric accumulator over a
list parameter. This golden is the composition, and what it carries is a
normative position that a sequence of events updates: a reporting duty that
starts open with a term, is discharged by a report in time, and is breached by
an event that overruns what is left of the term.

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
later event moves it. Both halves are pinned, one case each: a breach survives
a late report (case2), and a discharge survives the term running out afterwards
(case8) — the non-re-arming that keeps a duty already met from lapsing. The
`IfThenElse` condition reads only the two binders, so WF-077 holds and the
branch complement stays exact.

The comparison is `dagen <= resterend`, so the LAST day of the term still
counts as inside it: a report on day 30 discharges (case6). And exhausting the
term does not breach by itself — the position stays `Openstaand(0)`, open with
no days left (case5); only an event that would overrun the remainder moves it
to `Geschonden`.

The fold is a LEFT fold, and two cases pin that direction absolutely: reversing
case2's sequence yields `Vervuld` where the case pins `Geschonden`, and
reversing case8's yields `Geschonden` where the case pins `Vervuld`.

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

**case5 — `[Dagverloop(30)]`** (the term exactly used up): 30 ≤ 30 is true →
**`Openstaand(0)`** — still open, with no days left.

**case6 — `[Dagverloop(30), Melding]`** (reported on the last day):
`Openstaand(30)` → 30 ≤ 30 → `Openstaand(0)` → `Melding` → **`Vervuld`**.

**case7 — `[Dagverloop(12)]`** (part of the term used): 12 ≤ 30 →
**`Openstaand(18)`** — the only case that pins a counted-down term rather than
the init.

**case8 — `[Melding, Dagverloop(31)]`** (discharged, then the term runs out):
`Openstaand(30)` → `Melding` → `Vervuld` → the outer `Vervuld` arm →
**`Vervuld`**.

Every case pins its outcome absolutely as an `attributeValue` with a
`constructor`/`payload` value, plus `consistent`.

Cases 5–8 exist because cases 1–4 leave three semantic choices unpinned, each
measured: with `<=` weakened to `<` all of cases 1–4 still pass in both
executors (cases 5 and 6 refute it); with the subtraction's operands swapped
all of cases 1–4 still pass (case7 refutes it — cases 1–4 pin no `Openstaand`
other than the init); and with the outer `Vervuld` arm rewritten to
`Geschonden` all of cases 1–4 still pass (case8 refutes it — that arm is never
entered by cases 1–4).

## Gates

Gate 5 (`ref`) and gate 6 (clingo) both replay all eight cases against exactly
these hand-written values — ref ≡ clingo, never parity alone. Gate 3 passes the
document verifier-clean; gate 2 validates and round-trips the dump; gate 4
validates the eight case files. Gate 7 (the no-op guard) fails on all eight:
the target is absent on the raw input, so no expectation survives a do-nothing
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
Neither admits a reference to an object — but that is the smaller half of the
observation, and stating it alone mis-locates the obstacle. The datatype
universe of the IR has eleven members (number, percentage, text, boolean,
day-date, datetime, enumeration, unknown, sum, list, record) and an object
reference is not among them anywhere: not as an attribute type, not as a
parameter type, not as a list element. **So the parties a duty runs between
cannot sit inside the position value** — and the reason is not that a fence was
drawn narrowly, it is that there is no kind of value to widen the fence
towards. The value can carry the term and the outcome; it cannot carry who owes
and to whom.

What this golden does instead is what the landed kernel allows: the **holder is
the rule subject** — the Jutter the attribute hangs on — and the
**counterparty is fixed by the rule itself**, nowhere named in the value. The
same limit shows a second time here from the other side: the event register is a
document-level list, so the position is indexed per subject while the evidence
that moves it is not indexed at all.

That is the sharpest argument a later kernel extension has to answer. A position
that is genuinely relational — one duty per pair of parties, several such duties
on one holder at once, each with its own term — is not expressible today, and
not because the fold, the match or the sum type fall short: they carry the whole
state machine above without a single extension. It falls short on **identity**.
The residual question is therefore narrow and testable, and it is not a fence
widening: a NEW kind of datatype — an object reference — has to enter the
universe before a position can name its own parties; only then does admitting
it as a payload or a field become a question at all, and only then does the
acyclicity that bounds constructor- and record-term depth for the ASP encoding
have to be re-argued over a graph that now reaches instances. The text half of
the existing payload fence is already pinned negatively by
`corpus/negatief/wf060-text-payload`.
