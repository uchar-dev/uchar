# jutterswet-muntbewaring — the per-element temporal fold, narrowed

`corpus/golden/jutterswet-tempovouw` folds a monthly `bewaartarief` over a
jutter's vondsten and answers 48. This golden is that document with a subtype
between the relation and the fold: `Munt ⊑ Vondst`, the fold declares
`elementObjecttype: Munt`, and the same 48 has to come out — the numbers are
deliberately identical so the difference between the two goldens is the
narrowing and nothing else.

What that narrowing reaches here is a TRANSLATE-TIME fence, not a value.
`RUNTIME-SEMANTICS` ch. 3.6/§7.1: a set fold over a temporal attribute
integrates each member's own timeline first, so every member must bring its
own horizon, and a member carrying an asOfDate-BUCKET scalar instead —
a value with no period of its own — is refused by name rather than silently
bucketed at the session date. The fence has to walk the members, and the
members are the reach narrowed by `elementObjecttype` (ch. 2.2). A fence that
walked the raw reach would point at an instance the fold never folds.

That is exactly the shape here: `V1` and `V2` are plain `Vondst` instances
carrying a plain `bewaartarief` scalar (99 and 50, no timeline), linked beside
the munten. They are not members, so their missing timelines are not this
fold's business. Measured on this document with the fence's member walk
reverted to the raw reach: `TRANSLATION FAULT: the per-element temporal fold
of "bewaartarief" over "V2": the case supplies an asOfDate-bucket scalar` —
a verifier-clean document that ref and the Drools backend both answer, refused
by the ASP backend alone. The firing direction of the fence is untouched and
keeps its own witness in that backend's unit suite; this golden pins the
scope.

A second, independent consequence of the narrowing landed with this golden and
is worth naming, because it is the reason the document did not translate at
first: the §4.20 noemer strip (`€/mnd` integrated over a month grid is `€`)
hangs on a temporality test, and that test read the RAW declaration instead of
the effective one. Anchored at `Munt`, which declares no timeline of its own,
it answered "not temporal", the strip did not fire, and the write fence then
refused `€/mnd` into a `€` target. An inherited temporal member is temporal;
the lookup now closes over the chain like every other declaration lookup in
that translator.

## Case derivation (hand-computed, absolute)

Session date 2026-03-15. A segment contributes its value times the WHOLE
months of its own span; the member totals then add over the set.

| jutter | linked | of those, `Munt` | contribution |
|---|---|---|---|
| J1 | M1, M2, V1 (scalar 99) | M1, M2 | M1: 12 €/mnd × 3 mnd (jan, feb, mrt) = 36 € · M2: 7 × 1 + 5 × 1 = 12 € |
| J2 | V2 (scalar 50) | none | — |
| J3 | M3 (no bewaartarief at all) | M3 | — |

- **J1 = 36 + 12 = 48 €.** V1 contributes nothing and, more to the point,
  refuses nothing.
- **J2 = 0 €.** `TOTAAL` over an empty set is 0 unconditionally (§4.20), and
  the set is empty because the type filter removed the one instance the role
  reaches. An executor reading the links alone would have V2's bucket scalar
  in hand and no period to integrate it over.
- **J3 = 0 €.** The flank that keeps the fence honest in the other direction:
  the member passes the filter and is simply ABSENT — no timeline and no
  scalar — which contributes nothing and must never be refused.
