# jutterswet-opgave-afwezigheidsmarkers — the opgave marker beside the absence markers

Exercises the RegelSpraak dialect's **opgave marker** (`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on four
attributes that carry **absence markers** as well (DIVERGENCES, "Afwezigheidsmarkeringen";
`spec/RUNTIME-SEMANTICS.md` ch. 9.5), the absence marking first:

    de teruggavedatum Datum in dagen (of onbekend of niet van toepassing) (mag opgegeven worden);
    de afhaaldatum Datum in dagen (of niet van toepassing) (mag opgegeven worden);
    het bergloon Numeriek (geheel getal) (of onbekend) (mag opgegeven worden);
    de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend) (mag opgegeven worden);

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3h (teruggave, afhaling, bergloon en opslag).** 1. De teruggavedatum van een vondst valt een
> teruggavetermijn na de dag waarop de eigenaar haar opeist. 2. Voor drijfhout is geen teruggavedatum van toepassing.
> 3. Is het etiket van een vondst onleesbaar, dan is de teruggavedatum onbekend. 4. De afhaaldatum van een vondst valt
> zeven dagen na de claimdatum; zonder claimdatum is zij 1 januari 2026. 5. Het bergloon van een vondst is haar
> gewicht verminderd met tweemaal haar schade; zonder gewicht is het onbekend. 6. De opslagkosten van een vondst zijn
> in januari en februari 2025 onbekend en bedragen van maart tot en met juni 2025 10 per maand. 7. De strandvoogd kan
> de teruggavedatum, de afhaaldatum, het bergloon en de opslagkosten per maand opgeven, ook als een van de markeringen
> die het attribuut draagt; een opgave gaat voor de berekening en voor de waarde zonder claimdatum of gewicht.
> 8. Wijkt een opgave af van een berekening, dan wordt dat gemeld.

## Source (`source.rgs`)

- The parameter `teruggavetermijn`, a whole number of days (lid 1); the scenario binds it to 14 dg in every case.
- `teruggavedatum`, a `Datum in dagen` with both markers (lid 1 to 3, 7); `afhaaldatum`, a `Datum in dagen` with the
  marker niet van toepassing (lid 4, 7); `bergloon`, a whole number with the marker onbekend (lid 5, 7), computed from
  the whole numbers `gewicht` and `schade`; `opslagkosten`, a whole number for every month with the marker onbekend
  (lid 6, 7). Each carries the opgave marker.
- **teruggave na de claimdatum** (lid 1): the teruggavedatum is zijn claimdatum plus de teruggavetermijn, a date plus
  a parameter as its duration.
- **geen teruggave van drijfhout** (lid 2): niet van toepassing indien hij drijfhout is.
- **teruggave bij een onleesbaar etiket** (lid 3): onbekend indien hij onleesbaar is.
- **afhaal na de claimdatum** (lid 4): the afhaaldatum is zijn claimdatum plus 7 dagen, a date plus a literal
  duration; **afhaal zonder claim** (lid 4): the afhaaldatum is initialised on 01-01-2026.
- **bergloon naar gewicht en schade** (lid 5): the bergloon is zijn gewicht verminderd met 2 maal zijn schade;
  **bergloon zonder gewicht** (lid 5): the bergloon is initialised on onbekend.
- **opslag onbekend in de winter** and **opslag in het voorjaar** (lid 6): onbekend on [2025-01-01, 2025-03-01),
  10 on [2025-03-01, 2025-07-01).

A drijfhout or onleesbare vondst with a claimdatum would have two writers with different values (EF-016,
RUNTIME-SEMANTICS ch. 5.7); no case gives one.

## Lowering (the frontend's)

| Piece | Wire |
|---|---|
| domains | one domain per marked attribute: `teruggavedatum van een Vondst` (`waarde` with a `Date` of precision `day`, `onbekend`, `niet van toepassing`), `afhaaldatum van een Vondst` (`waarde` with a `Date`, `niet van toepassing`), `bergloon van een Vondst` (`waarde` with a whole number, `onbekend`) and `opslagkosten van een Vondst` (`waarde` with a whole number, `onbekend`) |
| 1, 2 | `opgegeven X` and `berekende X` carry the sum type of their marked attribute X, over its domain (the opslagkosten with its timeline) |
| moved writers | the computing rules write `berekende X`: `Construct waarde(zijn claimdatum plus de teruggavetermijn)`, `Construct niet van toepassing`, `Construct onbekend` for the teruggavedatum; `Construct waarde(zijn claimdatum plus 7 dagen)` for the afhaaldatum; `Construct waarde(zijn gewicht verminderd met 2 maal zijn schade)` for the bergloon; the two rules of lid 6 for the opslagkosten |
| 3 | `opgave boven berekening X`: the opgave default and the computation default, each with the plain reference to the member as its consequence, so the group passes a value or a marker on whole; on the afhaaldatum and the bergloon the initialisation is the lowest default, `Construct waarde(2026-01-01)` and `Construct onbekend` |
| 4 | `afwijking opgave X` attributes `afwijkende opgave X` under an `all` of: `opgegeven X` filled, `berekende X` filled, `opgegeven X` notEqual `berekende X` — the two compared as values of one sum type (RUNTIME-SEMANTICS ch. 9.3) |
| 5 | `controle opgave X`, the negated check of the characteristic; its source is the marked declaration |

On the opslagkosten, a timeline, pieces 4 and 5 are not generated (DIVERGENCES §7).

## Cases (hand-computed, absolute)

Per case the highest default that decides wins: the opgave, else the computation, else the initialisation, else what
the input holds (ch. 7.1, 7.8). A marker is a filled value: it decides, and it is filled for the deviation. The
deviation holds where both members are filled and their values differ, tag or payload (ch. 9.3); the consistency
deposits one fact `controle opgave X` × V exactly where it holds. A computed payload is leeg where its operator reads
it leeg (ch. 9.1, 1.2a): a date plus a duration propagates a leeg date, verminderd met propagates a leeg left operand.
The teruggavetermijn is 14 dg in every case, so a claimdatum of 10-03-2026 gives the computed teruggavedatum
24-03-2026 and the computed afhaaldatum 17-03-2026.

| Case | Input | opgave | computation | teruggavedatum | deviation |
|---|---|---|---|---|---|
| case1-berekende-marker | drijfhout | leeg | niet van toepassing | **niet van toepassing** | no |
| case2-opgegeven-datum-verdringt-berekende-marker | drijfhout, opgave 12-03-2026 | 12-03-2026 | niet van toepassing | **12-03-2026** | yes |
| case3-opgegeven-marker-verdringt-berekende-datum | claimdatum 10-03-2026, opgave niet van toepassing | niet van toepassing | 24-03-2026 | **niet van toepassing** | yes |
| case4-dezelfde-marker | drijfhout, opgave niet van toepassing | niet van toepassing | niet van toepassing | **niet van toepassing** | no |
| case5-andere-marker | onleesbaar, opgave niet van toepassing | niet van toepassing | onbekend | **niet van toepassing** | yes |
| case6-opgegeven-marker-zonder-berekening | opgave onbekend | onbekend | leeg | **onbekend** | no |
| case7-opgegeven-datum-verdringt-berekende-datum | claimdatum 10-03-2026, opgave 12-03-2026 | 12-03-2026 | 24-03-2026 | **12-03-2026** | yes |
| case8-opgegeven-datum-gelijk-aan-berekende-datum | claimdatum 10-03-2026, opgave 24-03-2026 | 24-03-2026 | 24-03-2026 | **24-03-2026** | no |
| case9-niets | — | leeg | leeg | **leeg** | no |

The opslagkosten, per day (ch. 7.11): without an opgave (case9) **onbekend** on [2025-01-01, 2025-03-01) and **10**
on [2025-03-01, 2025-07-01), leeg elsewhere. In case10-opgave-per-maand the opgave 4 on [2025-02-01, 2025-05-01) and
onbekend on [2025-06-01, 2025-08-01) decides the days on which it holds a value or a marker and the computation the
others: **onbekend** on [2025-01-01, 2025-02-01), **4** on [2025-02-01, 2025-05-01), **10** on [2025-05-01,
2025-06-01), **onbekend** on [2025-06-01, 2025-08-01), leeg elsewhere.

The afhaaldatum and the bergloon, each consistent:

| Case | Input | opgave | computation | initialisation | result |
|---|---|---|---|---|---|
| case11-berekende-afhaaldatum | claimdatum 10-03-2026 | leeg | 17-03-2026 | 01-01-2026 | afhaaldatum **17-03-2026** |
| case12-afhaaldatum-zonder-claim | — | leeg | leeg | 01-01-2026 | afhaaldatum **01-01-2026** |
| case13-opgegeven-marker-boven-de-initialisatie | opgave afhaaldatum niet van toepassing | niet van toepassing | leeg | 01-01-2026 | afhaaldatum **niet van toepassing**, no deviation |
| case14-berekend-bergloon | gewicht 30, schade 4 | leeg | 30 − 2 × 4 = 22 | onbekend | bergloon **22** |
| case15-bergloon-zonder-gewicht | schade 4 | leeg | leeg (the left operand of verminderd met) | onbekend | bergloon **onbekend** |

Every case plays in ref, in the ASP backend and on both branches of the Drools backend.
