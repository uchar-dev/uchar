# duration-reads-van-before-tot

The **tijdsduur operand-order** witness of `RUNTIME-SEMANTICS.md`
ch. 5.11: argument 0 (`van`) is evaluated before argument 1 (`tot`).

## Why a RED case, not a golden

Same reason as `clamp-reads-bounds-in-order`, by a different route.
A green witness would need the later operand to deposit a §9.5 channel
fact, and the only expression-level producer both executors carry is a
division by an empty divisor. But a `duration` operand must be a
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

* `van` = the `evaluationDate` context value — with no evaluation date in
  the session this **faults** (ch. 8.2: the context value is the only
  expression-level access to the evaluation date, and there is none).
* `tot` = a read of `vertrekdatum`, whose need is leeg — the ch. 10.6
  barrier, which would end the whole `Let` with no fault.

## What each executor pins

| executor | pinned fragment | does it pin the order? |
|----------|-----------------|------------------------|
| ref | `the evaluationDate context value needs an evaluation date` | **yes** — `van` is evaluated before `tot`, so the context value is reached before the barrier |
| clingo | `the evaluationDate context value needs an evaluation date` | **no** — the ASP translation has no operand evaluation order at all; it refuses the dateless context value whichever operand it visits first |

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

Carried since the card `tms-omschakeling` (2026-10-10): the red run
drives the exec-model branch, which derives with truth maintenance, and
it faults as ref and clingo do: `fout in de regel "vaartduur van een
vaart": the evaluationDate context value needs an evaluation date; the
case carries asOfDate null`, pinned up to `an evaluation date`. Until
that card the red run drove the destructive lowering, which enforced no
van/tot **order** on a tijdsduur, so the ch. 10.6 barrier this witness
plants never became a fault and the session reached a model — `NOFAULT`,
exactly as its `clamp-reads-bounds-in-order` twin then; it was listed
with that reason in `scripts/check-red.sh`.

Until 2026-09-09 this paragraph read the other way — "the reaching rule
does not lower, a transpiler SKIP on an unregistered `rekendatum`
global, so fire is never reached" — and that was true of the backend of
the day for a reason that has nothing to do with the order: the
context value above sits **inside a `Let` body**, and while the Drools
loader parsed a `Let` into an opaque `Onopgelost` carrier, the generic
`Expr.bevat` scan read that carrier as a leaf, never saw the constant,
and left the §4.2 `rekendatum` global unregistered — so the rule was
skipped whole. The ch. 4.10 side-channel cleanup made `Let` an ordinary
union node; the scan now descends into value and body, the global is
registered, and the rule lowers and fires. What stays not-carried is the
order itself, which is a narrower and more honest verdict than the old
one — and it leaves the two pinned legs untouched: this case still pins
`expectedFault {ref, clingo}`, and both still fault.
