# jutterswet-keurlast — a member the subselectie DROPS is not the fold's business

`corpus/golden/jutterswet-tempovouw` folds a monthly rate over a jutter's
vondsten; `corpus/golden/jutterswet-muntbewaring` narrows that fold's members
by `elementObjecttype`. This golden narrows them the third way the wire admits:
the §5.6 die/dat SUBSELECTION.

```
De keurlast van een Jutter moet berekend worden als het totaal van het
keurtarief van alle vondsten van de Jutter DIE GEKEURD ZIJN.
```

What it pins is not the arithmetic but a TRANSLATE-TIME fence.
`RUNTIME-SEMANTICS.md` ch. 3.6/§7.1: a set fold over a temporal attribute
integrates each member's own timeline, so every member must bring its own
horizon, and a member carrying an `asOfDate`-BUCKET scalar instead — a value
with no period of its own — is refused by name rather than silently bucketed at
the session date. The fence therefore has to walk the members, and the members
are the reach narrowed by the element type AND by the subselectie: the
contributors are settled in full, filter and all, before the integration runs
(ch. 3.4a). A fence that walked the unfiltered reach points at an instance the
fold never folds.

That is exactly the shape here. `V2` and `V3` are NOT `gekeurd` and carry a
bare `keurtarief` scalar with no timeline. They are not contributors, so their
missing timelines are none of this fold's business. Measured on this document
before the fence was filter-aware: `TRANSLATION FAULT: the per-element temporal
fold of "keurtarief" over "V3": the case supplies an asOfDate-bucket scalar` —
a verifier-clean document that `ref` and the Drools backend both answer,
refused by the ASP backend alone. This was the residue
`backlog/done/20260912-typerange-sluiting.md` parked as "the filter edge";
this golden is its witness.

The fence's FIRING direction is untouched: a member INSIDE the selection
carrying a bucket scalar is still refused by name, and keeps its own witness in
the ASP backend's unit suite.

## Case derivation (hand-computed, absolute, before any executor ran)

`totaal` (§4.20, Tabel 30) integrates a rate over time: each segment
contributes `value × the WHOLE grid units of its own span`, and the `€/mnd`
numerator survives while the `/mnd` denominator is consumed by the month grid —
so the result is `€`. Contributors are the vondsten that are `gekeurd`.

| jutter | vondsten (gekeurd?, keurtarief) | bijdragers | keurlast |
|---|---|---|---|
| J1 | V1 (ja, 12 €/mnd op [2026-01-01, 2026-04-01)), V2 (**nee**, emmerwaarde 99) | {V1} | **36 €** |
| J2 | V3 (**nee**, emmerwaarde 50) | {} | **0 €** |
| J3 | V4 (ja, 5 €/mnd op [2026-01-01, 2026-03-01)), V5 (ja, 3 €/mnd op [2026-03-01, 2026-04-01)) | {V4,V5} | **13 €** |

- **J1 = 36.** `[2026-01-01, 2026-04-01)` is jan+feb+mrt = 3 whole months, so
  `12 €/mnd × 3 mnd = 36 €`. V2 contributes nothing — and, the point of the
  cell, it does not make the fold unanswerable either: it carries a value with
  no period, which is precisely what the fence refuses for a CONTRIBUTOR.
- **J2 = 0.** The selection comes out EMPTY while the range does not: one
  vondst, not gekeurd. An empty `totaal` is 0 (not leeg), and the one member
  present is again a bucket scalar — so this cell separates "the subselectie
  emptied the contributor set" from "the relation delivered nothing".
- **J3 = 13.** Two contributors with adjacent spans:
  `5 €/mnd × 2 mnd + 3 €/mnd × 1 mnd = 10 + 3 = 13 €`. The control: the
  arithmetic of the fold itself is unchanged by the presence of a filter.

`asOfDate` is 2026-03-15 and is irrelevant to every number — deliberately: the
integration takes its buckets from each member's own timeline, and the whole
reason a bucket scalar is refused for a contributor is that it would have to
borrow the session date instead.
