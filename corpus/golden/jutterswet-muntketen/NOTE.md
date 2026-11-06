# jutterswet-muntketen — a MULTI-STEP range, narrowed at its landing

`corpus/golden/jutterswet-vondstketen` and
`corpus/golden/anchor/jutterswet-bootberging` both fold over a two-step
`sourcePath` — Jutter → Boot → Vondst — and in both the fold's
`elementType` is exactly what the last step's role fills, so the ch. 2.2
population equals the reach and the narrowing is invisible. This golden is the
same chain with `Munt ⊑ Vondst` under it and `elementType: Munt`, which
is the one arrangement in which the two can differ.

The rule the document pins is `RUNTIME-SEMANTICS` ch. 3.4b: the instances the
LAST step reaches are the fold's reach, and `elementType` narrows that
reach to the contributors exactly as in the one-step form — the intermediate
steps are not narrowed, only the landing. In the ASP encoding the path is a
chain of `rol/3` atoms through fresh intermediates and the `isa/2` atom closes
it on the landing variable alone; in `ref` the path is walked whole and the
type filter applies to the flat union. Two spellings of one rule, and nothing
but a golden held them together.

`sum` and `minimum` sit side by side because the path feeds both arms and they
fail differently: a wrongly included instance shifts a sum and REPLACES an
extreme. Measured on this document with the element filter removed from both
OCaml executors, J1 answers 1.00 and 22.00 instead of 5.00 and 12.00.

The middle relation `berging` declares both roles to-many, so it has no one
side whose bean could hold it. The Drools backend reads such a relation over
its links, which the case input states as facts of their own, and the hop
Boot → Munt joins the two nodes by such a link. The golden plays in all three
executors (ref ≡ clingo ≡ Drools, both Drools branches), as does
`anchor/jutterswet-bootberging` over the same relation. The one-step
narrowing is pinned in all three as well, by `jutterswet-munttelling` and
`jutterswet-muntpiek`.

## Case derivation (hand-computed, absolute)

The reach is the flat union over the path, deduplicated by instance; the
contributors are the reached instances whose type is `Munt`-or-below.

| jutter | boten | reached | of those, `Munt` | `laagste` | `totaal` |
|---|---|---|---|---|---|
| J1 | B1, B2 | V1 (1.00), M1 (7.00), M2 (5.00), V2 (9.00) | M1, M2 | **5.00** | 7.00 + 5.00 = **12.00** |
| J2 | B3 | V3 (4.00) | none | **leeg** | **0** (emptyFallback) |
| J3 | B4, B5 | M3 (3.00), V4 (2.00) | M3 | **3.00** | **3.00** |

J1 brackets the members on both sides again: the lightest and the heaviest
thing the chain reaches are the two plain vondsten, so the minimum lands on a
non-member unless the landing is narrowed, and the sum is 22.00 instead of
12.00.

J2 is the empty population over a non-empty reach, and it separates the two
empty-set rules on one row: `minimum` has no fallback and is empty, `sum`
carries an `emptyFallback` of 0 and writes it.

J3 is the DIAMOND: both boats bergen the same `M3`, so the flat union has to
deduplicate before the fold or the sum reads 6.00. It also carries a second
plain vondst reached through one of the two boats, so the dedup and the type
filter compose on one jutter rather than being pinned apart.
