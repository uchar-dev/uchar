# jutterswet-aandelensom — a set sum over a percentage attribute

Art. 9 Jutterswet divides the proceeds of a joint salvage over the bergers
in proportion to each one's share in the salvaged weight. Here each `Berger`
carries that share as a percentage, `gewichtsaandeel` (`Percentage (geheel
getal)`), and a `Berging` sums the shares of its bergers into its `verdeeld
aandeel`: the part of the weight that is assigned, 100 when the shares are
complete.

## What the golden pins: the operand of a sum may be a percentage

WF-091's numeric-operand arm (`spec/IR-SPEC.md` ch. 4.1) demands that every
operand of a `sum` or a `total`, and the base of the pro-rata part, be
declared a `Number`, "a `%` number included". This golden is the witness of
that inclusion: the one fold in the corpus whose operand is a percentage.
Since L3 a `Number` whose unit is `%` is a percentage, and the `sum` row of
the derived-type table passes operand 0's type through, so the fold's own
`type` is a `Number` with the unit `%`, written without the `percentage`
presentation, which belongs to declarations only (IR-SPEC ch. 2.4).

Mutation, measured 2026-09-25: with the arm's type test narrowed to a plain
number (`when not (match d with Numeriek _ -> true | _ -> false)`), this
document is refused ("the sum of 'gewichtsaandeel' on 'Berger' folds Number
values …") and gate 3 fails on this golden alone.

## Hand computation (absolute)

A sum adds the magnitudes (RUNTIME-SEMANTICS ch. 2.2): a percentage standing
alone denotes its per-hundred number, and no operator here scales it. Every
share is filled, so the empty rules of ch. 1.2 do not come into play.

| case | berging | bergers (gewichtsaandeel) | verdeeld aandeel |
|------|---------|---------------------------|------------------|
| case1-twee-bergers | B1 | G1 45, G2 30 | **75** |
| case2-twee-bergingen | B1 | G1 50, G2 30, G3 20 | **100** |
| case2-twee-bergingen | B2 | G4 60 | **60** |

case2 folds two bergingen in one world: each sum ranges over its own
berging's bergers only. No consistency rule exists, so both cases end
`consistent`.

## Measured (2026-09-25)

The verifier passes the document; `ref` and clingo pass both cases (2 and 3
expectations); the Drools backend plays both cases in its exec-model branch
(PLAYED 2) and in its DRL-text branch (PLAYED 2).

## noop refutation (gate 7)

`verdeeld aandeel` is absent from the input world, so the `--noop` run fails
on every `attributeValue` expectation. Not a survivor.
