# jutterswet-keurploeg — the quantifier fold with a die/dat SUBSELECTION

`corpus/golden/jutterswet-ploegpremie` folds a §8.2 whole-period matrix over
the members of a linked set. This golden is that document with one thing
added: the §5.6 subselectie that says WHICH members the sentence is about.

```
Een Keurploeg is premiewaardig indien de keurvergoeding van ALLE
keurmeesters van de Keurploeg DIE AANGEMELD ZIJN gedurende het gehele jaar
groter is dan 40.
Een Keurploeg is stilliggend indien de keurvergoeding van GEEN VAN DE
keurmeesters van de Keurploeg DIE AANGEMELD ZIJN gedurende het gehele jaar
groter is dan 40.
```

`IR-SPEC.md` ch. 4.5: `filter` is evaluated **per contributor with the element
as subject**, and it settles the contributor set BEFORE the matrix is asked
(`RUNTIME-SEMANTICS.md` ch. 3.9a / ch. 3.4a). It is the fold's range field, not
the quantifier's: both rules carry the same subselectie and quantify over the
same selected members.

The third rule is the control and it is not decoration:

```
Het keurtal van een Keurploeg moet berekend worden als het aantal
keurmeesters van de Keurploeg.
```

`keurtal` counts the WHOLE range — no subselectie — so every case states how
many members the relation delivers beside what the two folds quantify over.
Cases 1, 3 and 7 are exactly the cases where those two numbers differ, and a
reader that dropped the filter would have to answer them with the range.

## The seven cells, computed by hand before any executor ran

Bound 40, strictly `groter dan`, calendar 2026 (365 days), `asOfDate`
2026-03-15 in every case and irrelevant to every verdict — the whole-period
fold takes its buckets from the timeline, not from the evaluation date.
`keurvergoeding` is month-granular while the qualifier's grid is the YEAR;
that independence is ch. 3.9's and the casus carries it rather than hiding it
behind a matching pair.

A member is a CONTRIBUTOR when it is `aangemeld`. It satisfies the MATRIX when
its `keurvergoeding` timeline is above 40 over the whole of 2026. `ALLE` holds
when every contributor satisfies the matrix; `GEEN` when none does.

| case | the ploeg's keurmeesters (aangemeld?, keurvergoeding over 2026) | keurtal | bijdragers | premiewaardig | stilliggend |
|---|---|---|---|---|---|
| 1 | K1 (ja, 50 heel jaar), K2 (**nee**, 30 heel jaar) | 2 | {K1} | **ja** | nee |
| 2 | K1 (ja, 50 heel jaar), K2 (ja, 30 heel jaar) | 2 | {K1,K2} | nee | nee |
| 3 | K1 (**nee**, 50 heel jaar), K2 (**nee**, 30 heel jaar) | 2 | {} | **ja** | **ja** |
| 4 | K1 (ja, 40 heel jaar) | 1 | {K1} | nee | **ja** |
| 5 | K1 (ja, 50 heel jaar), K2 (ja, 60 op [jan, jul) ) | 2 | {K1,K2} | nee | nee |
| 6 | geen keurmeesters | 0 | {} | **ja** | **ja** |
| 7 | K1 (**nee**, 50 heel jaar), K2 (ja, 30 heel jaar) | 2 | {K2} | nee | **ja** |

Per cell, the derivation:

1. **The member outside the selection that would reverse the verdict.** K2 sits
   at 30 and is not aangemeld. Over the contributors {K1} every member is above
   40, so `ALLE` holds: **premiewaardig ja**. Over the RANGE {K1,K2} it does
   not, so an executor that ignored the filter answers **nee** here. `GEEN` has
   K1 as a witness either way, so `stilliggend` is **nee** and stays nee — the
   filter is decisive on one side only in this cell, which is why cell 7 exists.
2. **The ordinary mixed cell**, and the `GEEN ≠ ¬ALLE` cell: both are aangemeld,
   K1 is above the bound and K2 is not, so `ALLE` fails on K2 and `GEEN` fails
   on K1. Neither characteristic is attributed. This is the cell that would
   read `ja` under a reader that computed `GEEN` as `not ALLE`.
3. **The selection that comes out EMPTY while the range does not.** Two members,
   neither aangemeld: `keurtal` is 2 and the contributor set is `{}`. An empty
   conjunction holds under BOTH quantifiers (ch. 3.9a), so **both
   characteristics are attributed**. An executor that ignored the filter would
   answer exactly the opposite of both (`ALLE` broken by K2 at 30, `GEEN` broken
   by K1 at 50), which makes this the sharpest cell in the file.
4. **The strict bound**, inside the selection: 40 is not greater than 40, so
   `ALLE` fails and — the same fact read the other way — `GEEN` holds.
5. **Partial coverage inside the selection.** K2's timeline runs
   `[2026-01-01, 2026-07-01)`: it touches the 2026 bucket and covers 181 of its
   365 days, so it falls short of "gedurende het gehele jaar" (ch. 3.9) and
   fails the matrix. `ALLE` fails on K2; `GEEN` fails on K1, which does cover
   the year above the bound. Partial COVERAGE, not a hole punched in the middle.
6. **The empty RANGE**, beside cell 3's empty SELECTION. Both are vacuous and
   both read ja/ja, and they are two different vacua: here the relation
   delivers nothing (`keurtal` 0), there it delivers two members the
   subselectie drops (`keurtal` 2). A fold that reported the range size from
   its contributor list would read cell 3 as cell 6.
7. **The GEEN-side mirror of cell 1.** The member outside the selection (K1 at
   50) is the one that WOULD satisfy the matrix, so it is the one an unfiltered
   `GEEN` would take as its witness: ignoring the filter answers
   **stilliggend nee**, honouring it leaves `{K2}` at 30, which satisfies
   nothing, so **stilliggend ja**. `premiewaardig` is nee either way (K2 at 30
   breaks `ALLE`, and so would K1's presence not repair it).

## What each executor has to get right

The subselectie is a `Conditie` on the element, so each executor reads it with
its own machinery and none of them may special-case it: `ref` filters the
members `fold_members` delivers before quantifying; the ASP backend joins the
filter's atoms into the witness rule beside the range join, under BOTH
quantifiers (under `ALLE` the witness is a contributor that FAILS the reified
matrix, so the filter atoms must sit in the witness body and not in the
reification); the Drools exec branch constrains the accumulate SOURCE with it,
exactly as `Expr.Functie.filter` is constrained. The DRL-text branch plays
this document since 2026-09-18: it writes the filter into the accumulate
SOURCE of its own collectList fold, which is the same element pattern its non-
temporal witness route constrains — so the filter has one reading in both
Drools branches and not two.
