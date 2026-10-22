# jutterswet-premietabel-twee-ofcellen — a decision-table row with two or-cells

A Vondst has a soort and the strook where it was found. A decision table sets
its vondstpremie: row 1 gives 10 when the soort is "touw" or "glas" and the
strook "noord" or "zuid"; row 2 gives 3 for "hout" on "noord". Row 1 carries two
or-cells. The frontend writes a decision table as one rule per row; a row with
two or-cells it skipped with a WARNING (RGS-300) until the fix round of the card
third-condition-level, because without the depth of a bullet the second nested
group would have taken the values of the first. It now writes the row as an
'alle' list of two 'ten minste één' groups, each a `•` header with `••` values
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "§13.4.14.6–7 The depth of a
bullet"). Own Jutterswet-flavoured casus, own values, rights-free.

## Casus (own text)

> **Art. 9c (vondstpremie).** 1. Een vondst van touw of glas op de noord- of
> de zuidstrook geeft een premie van 10. 2. Een vondst van hout op de
> noordstrook geeft een premie van 3.

## Source (`source.rgs`)

- **vondstpremie** (art. 9c): a decision table with two condition columns,
  "indien zijn soort gelijk is aan" and "indien zijn strook gelijk is aan".

The IR carries row 1 as `Compound all [Compound atLeast 1 [soort = "touw",
soort = "glas"], Compound atLeast 1 [strook = "noord", strook = "zuid"]]` and
row 2 as `Compound all [soort = "hout", strook = "noord"]`.

## Hand computation

Each case has two Vondsten; V2 always gets a premie, so no case survives a run
that evaluates nothing (gate 7).

| case | V1 soort, strook | V1 premie | V2 soort, strook | V2 premie |
|---|---|---|---|---|
| `case1-touw-noord.json` | touw, noord | 10 (row 1) | glas, zuid | 10 (row 1) |
| `case2-glas-noord.json` | glas, noord | 10 (row 1) | touw, zuid | 10 (row 1) |
| `case3-touw-west.json` | touw, west | leeg: the strook is in neither group's values | hout, noord | 3 (row 2) |
| `case4-kurk-zuid.json` | kurk, zuid | leeg: the soort is in neither | glas, zuid | 10 (row 1) |
| `case5-kurk-west.json` | kurk, west | leeg: both fail | touw, noord | 10 (row 1) |

The four combinations of row 1 (touw and glas, noord and zuid) each give 10. A
reading of row 1 as one 'ten minste één' list of all four values gives V1 of
cases 3 and 4 a premie of 10; a reading as one 'alle' list of all four gives no
Vondst a premie from row 1.

## Executors

ref, clingo and both Drools branches replay every case (PLAYED 5).
