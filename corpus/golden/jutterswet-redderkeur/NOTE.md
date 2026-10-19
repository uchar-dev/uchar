# jutterswet-redderkeur — a COMPOUND matrix under the quantifier fold

The fifth of the quantifier-fold family. `jutterswet-schouwploeg` runs a single
leaf condition as the matrix under `all` and `none`, `jutterswet-vuurtoren` runs
a single leaf under the three counting quantifiers. This one puts a `Compound`
there — the §13.4.14 composite, with its own quantifier and its own threshold —
and nests it three levels deep, so the fold's matrix is a composition of leaves
rather than one leaf.

```
Een Reddingspost is paraat      indien alle redders van de Reddingspost aan
                                alle volgende voorwaarden voldoen:
                                - zijn dienstjaren groter zijn dan 5
                                - hij gediplomeerd is.
Een Reddingspost is geoefend    indien alle redders van de Reddingspost aan
                                ten minste 1 van de volgende voorwaarden
                                voldoen:
                                - zijn conditie groter is dan 7
                                - hij aan alle volgende voorwaarden voldoet:
                                  - hij gediplomeerd is
                                  - zijn dienstjaren groter zijn dan 2.
Een Reddingspost is stil        indien geen van de redders van de Reddingspost
                                aan alle volgende voorwaarden voldoet:
                                - zijn dienstjaren groter zijn dan 5
                                - hij gediplomeerd is.
Een Reddingspost is wisselend   indien alle redders van de Reddingspost aan ten
                                hoogste 1 van de volgende voorwaarden voldoen:
                                - hij gediplomeerd is
                                - zijn conditie groter is dan 7
                                - zijn dienstjaren groter zijn dan 5.
Het redderstal van een Reddingspost moet berekend worden als het aantal redders
                                van de Reddingspost.
```

`IR-SPEC.md` ch. 4.5 (`QuantifierFold`, WF-108) over ch. 2.2's `Compound`;
semantics `RUNTIME-SEMANTICS.md` ch. 3.9a, whose `test` is an ORDINARY condition
evaluated per contributor with the element as subject — a composite is one.

## Why this ir.json is hand-written and carries no source.rgs

When it was written, for the reason its two siblings were:
`RuleChecker.quantifierFold` lowered only the §8.2-QUALIFIED condition, so an
unqualified comparison was refused (`setlezing buiten een vouwbron`, a message
that cited WF-096 until L2) and the characteristic spelling resolved to
`Onopgelost`. Measured again at L2 (2026-09-24) on the frontend of this
repository: both of those spellings now lower to a `QuantifierFold` without a
diagnostic, but this document's own form, a composite matrix under the
quantifier, still does not. Each bullet is resolved against the rule subject,
the Reddingspost, which declares neither `dienstjaren`, `conditie` nor
`gediplomeerd`, so every bullet becomes an `Unresolved` marker with a `warning`
(`onopgelost: de naam 'zijn dienstjaren' is geen gedeclareerde parameter, …`
and `onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het
onderwerp …`); no set reading is written, so no setlezing error arises. Widening
that is a frontend card of its own; this document is written by hand against
the schema, and gate 10 does not see it.

## What it covers that the other four do not

The matrix is a `Compound`, at three depths: `paraat` and `stil` carry a
one-level `all` composite, `wisselend` a one-level `atMost`, and `geoefend` an
`atLeast` whose SECOND bullet is itself an `all` composite — a composite inside
a composite inside a fold. Four of the five quantifier vocabulary entries appear
in this document — three at the composite position (`all`, `atLeast`, `atMost`)
and two at the fold position (`all`, `none`), so the two levels cannot be
confused for one another: a reader that folded the inner quantifier into the
outer would answer `geoefend` and `wisselend` wrong in opposite directions.

`stil` is `paraat`'s matrix under the other fold quantifier, which is what makes
the pair a `none ≠ ¬all` witness: they are both false in case 1 and both true in
case 3, and case 2 separates them.

## The cells, computed by hand before any executor ran

`dj` is dienstjaren, `c` is conditie, `g` is gediplomeerd. All comparisons are
STRICT (`groter dan`).

| case | post | de redders van de post (dj, c, g) | redderstal | paraat | geoefend | stil | wisselend |
|---|---|---|---|---|---|---|---|
| 1 | P1 | R1(8, 9, ja), R2(6, 3, ja) | 2 | **ja** | **ja** | nee | nee |
| 2 | P1 | R1(4, 3, ja), R2(1, 9, nee) | 2 | nee | **ja** | **ja** | **ja** |
| 3 | P1 | — | 0 | **ja** | **ja** | **ja** | **ja** |
| 4 | P1 | R1(9, 8, ja) | 1 | **ja** | **ja** | nee | nee |
| 4 | P2 | R2(2, 1, nee) | 1 | nee | nee | **ja** | **ja** |
| 5 | P1 | R1(leeg, 9, nee), R2(6, 3, nee) | 2 | nee | nee | **ja** | **ja** |

Reading the harder columns:

- **case 1** — R1 holds all three `wisselend` bullets, so its count is 3 and the
  `atMost 1` composite fails for that one member, which is enough to fail the
  `all` fold. `paraat` and `geoefend` hold for both members.
- **case 2 is the cell the NESTING decides.** Neither member satisfies
  `geoefend`'s FIRST bullet on its own merit alone: R1 fails `c > 7` (3) and is
  carried by the nested `all` composite (gediplomeerd ∧ 4 > 2), while R2 fails
  that nested composite (not gediplomeerd) and is carried by `c > 7` (9). One
  member takes each road, so flattening the nesting — or reading the inner
  quantifier as the outer — fails this cell. `paraat` is false (R1 has 4 ≤ 5)
  while `stil` is true (nobody has BOTH), which is the `none ≠ ¬all` pair.
- **case 3** is the vacuum: an empty contributor set satisfies `all` and `none`
  alike (ch. 3.9a), and `redderstal` says the range really is empty.
- **case 4 pins the RANGE**: two posts in one run reading opposite on every
  column. A reader quantifying over every `Redder` instead of over the redders
  of the post answers them the same.
- **case 5 is the leeg member.** R1 has no `dienstjaren` at all. An ordering
  comparison over leeg is onwaar (ch. 1.2d), so `dj > 5` fails for R1 and its
  `wisselend` count is 1 (gediplomeerd nee, 9 > 7 ja, leeg > 5 nee) rather than
  2 — the cell flips to **ja** exactly because the leeg bullet does not count.
  That is a THRESHOLD reading of the same leeg member `jutterswet-vuurtoren`
  case 5 reads arithmetically, one level further in.

`asOfDate` is null in every case: nothing in this document is time-dependent,
which is what makes the matrix non-temporal.
