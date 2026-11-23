# jutterswet-opgave-teruggavetermijn — the opgave marker on a date computed with a parameter as its duration

Exercises the RegelSpraak dialect's **opgave marker** (`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on a
`Datum in dagen` computed as a date plus a parameter: the duration is the parameter `teruggavetermijn`, a whole number
with the unit dg (RUNTIME-SEMANTICS ch. 2.2 and 3.8, the calendar arithmetic of a date and a duration). `ir.json` is
the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs` (gate 16),
whose expectations are written by hand. Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3h (teruggave).** 1. De teruggavedatum van een vondst valt een teruggavetermijn na de claimdatum. 2. De
> strandvoogd kan de teruggavedatum opgeven; een opgave gaat voor de berekening. 3. Wijkt een opgave af van de
> berekening, dan wordt dat gemeld.

## Source (`source.rgs`)

- The parameter `teruggavetermijn`, a whole number of days (lid 1); the scenario binds it to 14 dg in every case.
- `teruggavedatum`, a `Datum in dagen` with the opgave marker (lid 2, 3).
- **teruggave na de claimdatum** (lid 1): the teruggavedatum is zijn claimdatum plus de teruggavetermijn.

## Lowering (the frontend's)

| Piece | Wire |
|---|---|
| 1, 2 | `opgegeven teruggavedatum` (input) and `berekende teruggavedatum`, both `Date` of precision `day` |
| moved writer | the rule writes `berekende teruggavedatum`: the date `zijn claimdatum` plus the parameter `de teruggavetermijn` |
| 3 | `opgave boven berekening teruggavedatum`: the opgave default above the computation default |
| 4 | `afwijking opgave teruggavedatum` attributes `afwijkende opgave teruggavedatum` under an `all` of: `opgegeven teruggavedatum` filled, `berekende teruggavedatum` filled, the two notEqual |
| 5 | `controle opgave teruggavedatum`, the negated check of the characteristic |

## Cases (hand-computed, absolute)

The teruggavetermijn is 14 dg, so a claimdatum of 10-03-2026 gives the computed teruggavedatum 24-03-2026. A date
plus a duration is leeg where the date is leeg (ch. 1.2a). The opgave decides where it holds a value, the computation
elsewhere (ch. 7.1); the deviation holds where both hold a value and differ, and the consistency deposits one fact
`controle opgave teruggavedatum` × V exactly where it holds.

| Case | Input | opgave | computation | teruggavedatum | deviation |
|---|---|---|---|---|---|
| case1-berekende-teruggavedatum | claimdatum 10-03-2026 | leeg | 24-03-2026 | **24-03-2026** | no |
| case2-opgegeven-datum-verdringt-berekende-datum | claimdatum 10-03-2026, opgave 12-03-2026 | 12-03-2026 | 24-03-2026 | **12-03-2026** | yes |
| case3-opgegeven-datum-gelijk-aan-berekende-datum | claimdatum 10-03-2026, opgave 24-03-2026 | 24-03-2026 | 24-03-2026 | **24-03-2026** | no |
| case4-opgegeven-datum-zonder-claimdatum | opgave 12-03-2026 | 12-03-2026 | leeg | **12-03-2026** | no |

Every case plays in ref, in the ASP backend and on both branches of the Drools backend.
