# jutterswet-diepwrakbasis — two bare rules on one chain, a group between them

Three levels: `Diepwrak` is a `Wrakrecht`, and a `Wrakrecht` is a
`Strandrecht`. Two unconditional computing rules write `uitkering`: one on
the highest type (`De uitkering van een Strandrecht` = its `grondslag`) and
one on the lowest (`De uitkering van een Diepwrak` = its `grondslag` maal 3).
Between them `Wrakrecht` declares a defaults group on `uitkering`: 99,00 when
the Wrakrecht is `betwist`, else its `grondslag` maal 2.

## What the golden pins: the gate half of WF-079's chain arm

Through a type chain WF-079 pairs a version on a type with one on an
ancestor only outside the gate of `RUNTIME-SEMANTICS.md` ch. 7.10: where the
lower type's chain declares no defaults group on the target (`IR-SPEC.md`
ch. 3.1). The `Diepwrak`'s chain declares one, on the middle type, so the
fold makes the two rules one writer per concrete type and the verifier
passes the pair. `corpus/negatief/wf079-keten-kale-schrijvers` pins the
other half: a chain without a group, where the pair is refused.

No other document in the corpus exercises this gate: in
`jutterswet-wrakbasis`, `jutterswet-wrakerfbasis` and
`anchor/jutterswet-strandrechtketen` each chain carries at most one bare
writer, so the arm forms no pair there. Measured 2026-09-25: with the gate
condition deleted from the chain arm (the conjunct `not (List.mem ("A:" ^
norm a) (chain_group_keys ctx.doc low))` in `wf079`,
`spec/verifier/lib/checks.ml`), this golden is refused,

    WF-079 FAIL /rules/rules/1/versions/0: two unconditional computing rules
    write target (Diepwrak, uitkering) through its type chain — one declared
    on Strandrecht, one on Diepwrak — and no defaults group folds them into
    one stack, so both fire on a Diepwrak instance; priority is explicit (a
    defaults group, whose subtype base replaces the inherited base, ch. 7.10),
    never implicit agenda order (ch. 7.7)

and none of the other 754 documents in golden, rood, negatief and the
parity zone changes its report.

## The fold per concrete type (ch. 7.10, `spec/proposals/SUBTYPING.md` §4.1)

- **Strandrecht.** Its chain is the type itself and declares no group: the
  rule on `Strandrecht` enters no stack and fires as the ch. 7.8 standing
  norm.
- **Wrakrecht.** Its chain declares the group. The `Strandrecht` rule becomes
  the inherited base (rule 4), and the group's own base, `grondslag` maal 2,
  replaces it; the `betwist` exception sits above that base.
- **Diepwrak.** Its chain declares the group, on `Wrakrecht`. The rule on
  `Diepwrak` is the base of its own level (rule 1, the bare-rule branch) and
  replaces the inherited base, `grondslag` maal 2; the inherited exception
  stays above it.

## Hand computation (absolute)

`grondslag` is 10,00 on every instance.

| case | instance | betwist | uitkering | what it shows |
|------|----------|---------|-----------|---------------|
| case1-basis-per-niveau | S1 Strandrecht | no | **10,00** | the `Strandrecht` rule, outside the gate |
| case1-basis-per-niveau | W1 Wrakrecht | no | **20,00** | the group's own base replaces the inherited rule |
| case1-basis-per-niveau | D1 Diepwrak | no | **30,00** | the lower type's rule replaces the inherited base |
| case2-groep-beslist | S1 Strandrecht | yes | **10,00** | no group in this chain: `betwist` changes nothing |
| case2-groep-beslist | W1 Wrakrecht | yes | **99,00** | the group's exception decides |
| case2-groep-beslist | D1 Diepwrak | yes | **99,00** | the inherited exception stays above the `Diepwrak` base |

Both cases end `consistent`. D1 is the row the golden exists for: 30,00
without `betwist` and 99,00 with it is the effective stack the fold builds
for a `Diepwrak` — the inherited exception, then its own base.

## Measured (2026-09-25)

The verifier passes the document; `ref` and clingo pass both cases (4
expectations each); the Drools backend's exec-model branch plays both cases
(PLAYED 2), and its DRL-text branch refuses the document on its defaults
sentence (`ch. 7 prioritizedDefaults — the DRL-text branch has no defaults
lowering`), class (A) of `DRL_CHANNEL` in `scripts/check-drools.sh`.

## noop refutation (gate 7)

`uitkering` is absent from the input world, so the `--noop` run fails on
every `attributeValue` expectation. Not a survivor.
