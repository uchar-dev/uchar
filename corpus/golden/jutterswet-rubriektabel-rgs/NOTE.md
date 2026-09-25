# jutterswet-rubriektabel-rgs — a decision table written in RegelSpraak

The RegelSpraak-source twin of `jutterswet-rubriektabel`: an `Aanspoelsel`
gets the `rubriek` "organisch" when its `soort` is "zeewier" or "wrakhout",
and "overig" when it is neither; it is `weegplichtig` when at least two of
three grounds hold. The two `rubriek` rules are written as one `Beslistabel`
in `bron.rgs` (§12). A decision table has no node of its own: the
frontend's `BeslistabelDesugar` turns every data row into an ordinary rule,
so `ir.json` carries two plain rules for the table. Gate 10 re-emits the
document from the source byte for byte, so the desugaring of a table with
rows is pinned for the first time; `jutterswet-kaletabel` carries a table
header without rows, which the frontend reports and does not lower. The
three case files are the carrier's, copied byte for byte, with the same
absolute expectations.

## Source (`bron.rgs`)

```
Beslistabel rubriek van een aanspoelsel
    geldig altijd
| | de rubriek van een Aanspoelsel moet gesteld worden op | indien zijn soort gelijk is aan | indien zijn soort ongelijk is aan |
|---|---|---|---|
| 1 | "organisch" | "zeewier" of "wrakhout" | n.v.t. |
| 2 | "overig" | n.v.t. | "zeewier" en "wrakhout" |
```

One conclusion column, two condition columns, and `n.v.t.` where a row sets
no condition. Row 1's cell under the equality header is a value series with
`of`, which the desugaring reads as a disjunction over that one column:
`Compound {atLeast, 1}` of two `equal` comparisons. Row 2's cell under the
inequality header is a series with `en`, which it reads as a conjunction of
inequalities: `Compound {all}` of two `notEqual` comparisons. Each row
becomes one rule named `rubriek van een aanspoelsel <row>`, with
`presentation: "equation"` (the header's `moet gesteld worden op`).

The third rule stays a plain `Regel`: `Een Aanspoelsel is weegplichtig
indien hij aan ten minste twee van de volgende voorwaarden voldoet`, with
the grounds `gekeurd`, `geborgen` and `stuks` ≥ 10. A table row's
conditions are a conjunction, so a counting quantifier has no row form.

## Structural diff against `jutterswet-rubriektabel/ir.json`

Read with every `source` stripped and the keys sorted, and with the rules
paired by target and value, the documents differ in the two row names and
in the order of `rules.rules`:

| path (hand-written order) | hand-written | emitted | triage |
|---|---|---|---|
| `/rules/rules/0/name` | `rubriek bij organische soort` | `rubriek van een aanspoelsel 1` | (i) naming |
| `/rules/rules/1/name` | `rubriek bij overige soort` | `rubriek van een aanspoelsel 2` | (i) naming |
| order of `/rules/rules` | the two `rubriek` rules, then `weegplicht bij dubbele borging` | `weegplicht bij dubbele borging` first, then the two rows | (i) serialisation order |

(i) naming: the desugaring names a row after its table and its row number,
and the case files pin no rule name. (i) order: the frontend writes a
document's plain rules before the rows of its tables; `rules.rules` is a set
of rules evaluated to a fixpoint, so its order carries no meaning. Every
condition, comparison, literal, target and presentation is equal.

At the byte level the `source` records differ in wording on the same nodes:
the hand-written document writes its text values in single quotes
(`'zeewier'`) and short summaries on the compound conditions (`ten minste
één van: soort 'zeewier', soort 'wrakhout'`), where the frontend writes the
sentence each row desugars to, with the double quotes of a RegelSpraak text
value.

## Hand computation

| case | soort | stuks | kenmerken (input) | rubriek | grounds held | weegplichtig |
|---|---|---|---|---|---|---|
| case1-zeewier | zeewier | 12 | gekeurd, geborgen | **organisch** (row 1) | 3 of 3 | **present** |
| case2-drijfhout | drijfhout | 4 | gekeurd | **overig** (row 2) | 1 of 3 | **absent** |
| case3-wrakhout-grens | wrakhout | 10 | geborgen | **organisch** (row 1) | 2 of 3 (geborgen, 10 ≥ 10) | **present** |

case3 puts the `stuks` ground on its boundary and reaches the threshold of
two exactly. Every case expects `consistent`. The input carries no `rubriek`,
so a no-op run fails every case and none needs a survivor entry.

## Executors

ref and clingo replay all three cases with the pinned values; the Drools
backend plays them in both branches, exec-model and DRL text, `PLAYED 3`
each, as it does the carrier (measured 2026-09-25).
