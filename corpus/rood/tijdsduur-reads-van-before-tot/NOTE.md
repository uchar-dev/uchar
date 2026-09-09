# tijdsduur-reads-van-before-tot

The **tijdsduur operand-order** witness of `RUNTIME-SEMANTICS.md`
ch. 5.11: argument 0 (`van`) is evaluated before argument 1 (`tot`).

## Why a RED case, not a golden

Same reason as `begrensd-reads-bounds-in-order`, by a different route.
A green witness would need the later operand to deposit a §9.5 channel
fact, and the only expression-level producer both executors carry is a
division by an empty divisor. But a `tijdsduur` operand must be a
**date**, so the division would have to reach it through
`datum + duur` — and the ASP backend refuses a **unit-carrying
multiplicative operand**, which a duration necessarily is. The green
form does not translate. A fault does.

## The shape

```
vaartduur := Let vertrekdatum = zijn vertrek
             in de tijdsduur van de rekendatum tot de vertrekdatum in hele dagen
```

with `vertrek` **absent** and the case carrying `asOfDate: null`.

* `van` = the `Rekendatum` constant — with no evaluation date in the
  session this **faults** (ch. 8.2: the constant is the only
  expression-level access to the evaluation date, and there is none).
* `tot` = a read of `vertrekdatum`, whose need is leeg — the ch. 10.6
  barrier, which would end the whole `Let` with no fault.

## What each executor pins

| executor | pinned fragment | does it pin the order? |
|----------|-----------------|------------------------|
| ref | `the Rekendatum constant needs an evaluation date` | **yes** — `van` is evaluated before `tot`, so the constant is reached before the barrier |
| clingo | `the Rekendatum constant needs an evaluation date` | **no** — the ASP translation has no operand evaluation order at all; it refuses the dateless constant whichever operand it visits first |

Measured: with ref's two `let`s swapped the case reports `EXPECTED FAULT
DID NOT OCCUR: evaluation succeeded`, because the barrier fires first and
the `Let` is leeg. clingo's report is unchanged by the same swap.

This is stated plainly rather than dressed up: the case is a **two-executor
red case** (gate 8 needs both to fail observably, and both do), but only
the ref leg is an order witness. The ASP encoding derives; it does not
sequence. The one order-derived construct it has — the ch. 5.10 prefix
guard — exists because that short circuit's triggers are monotone in the
prefix, and a tijdsduur's two operands have no such structure.

## Drools

Not carried: the reaching rule does not lower — a transpiler SKIP, the
`THEN` value referring to an unregistered `rekendatum` global — so fire
is never reached and the van/tot order is not Drools-observable here.
Listed with that reason in `scripts/check-rood.sh`.
