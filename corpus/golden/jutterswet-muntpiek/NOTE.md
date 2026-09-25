# jutterswet-muntpiek — the EXTREME folds narrow their element set too

The min/max sibling of `corpus/golden/jutterswet-munttelling`. That golden
pins the exclusion direction of `IR-SPEC.md` ch. 2.2 on the count and on the
sum; this one pins it on `minimum` and `maximum`, the two arms that aggregate
by comparing rather than by accumulating and that no golden narrowed before.

The arrangement is the same and deliberately so: `vondstaanmelding` relates
`Jutter` to `Vondst`, both folds declare `elementType: Munt`, and
`Munt ⊑ Vondst` is a strict subtype — the role supplies the REACH, the
declared element type supplies the POPULATION.

Why the arm deserves its own witness rather than riding along on the sum: an
extreme is decided by ONE contributor, so a wrongly included instance does not
shift the answer a little, it replaces it. Measured on this document with the
element filter removed from both OCaml executors: J1 answers 1.00 and 9.00
instead of 5.00 and 7.00 — neither extreme is a `Munt` — and J2 answers 4.00
twice where the fold has no member at all. The empty-set rule is the other
half: min/max carry no `emptyFallback`, so an empty fold is empty, and an
executor that folded the reach instead of the population would have written a
number there.

## Case derivation (hand-computed, absolute)

Four jutters, so the element set is emptied four different ways.

| jutter | linked | of those, `Munt` | contributed `gewicht` | `laagste` | `hoogste` |
|---|---|---|---|---|---|
| J1 | V1 (1.00), M1 (7.00), M2 (5.00), V2 (9.00) | M1, M2 | 7.00, 5.00 | **5.00** | **7.00** |
| J2 | V3 (4.00) | none | — | **leeg** | **leeg** |
| J3 | M3 (no gewicht) | M3 | — | **leeg** | **leeg** |
| J4 | M4 (3.00) | M4 | 3.00 | **3.00** | **3.00** |

J1 is the exclusion proper, and it brackets the members on BOTH sides: the
lightest and the heaviest link are the two plain vondsten, so the filter has
to remove them or each extreme lands on one of them.

J2 and J3 are the two ways an extreme reaches empty and they must not be
conflated: J2's role reaches an instance and the type filter removes it, J3's
member passes the filter and carries no value. Both are empty here, and the
count in `jutterswet-munttelling` is what tells the two apart — an extreme
cannot.

J4 is the singleton: with one member the minimum and the maximum are the same
number, so a swapped aggregate would still look right on the other rows and is
caught here only by J1.
