# jutterswet-opgave-afwezigheidsmarkers — the opgave marker beside the absence markers

Exercises the RegelSpraak dialect's **opgave marker** (`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on two
attributes that carry **absence markers** as well (DIVERGENCES, "Afwezigheidsmarkeringen";
`spec/RUNTIME-SEMANTICS.md` ch. 9.5), the absence marking first:

    de teruggavedatum Datum in dagen (of onbekend of niet van toepassing) (mag opgegeven worden);
    de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend) (mag opgegeven worden);

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand. Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3h (teruggave en opslag).** 1. De teruggavedatum van een vondst is de dag waarop de eigenaar haar opeist.
> 2. Voor drijfhout is geen teruggavedatum van toepassing. 3. Is het etiket van een vondst onleesbaar, dan is de
> teruggavedatum onbekend. 4. De opslagkosten van een vondst zijn in januari en februari 2025 onbekend en bedragen
> van maart tot en met juni 2025 10 per maand. 5. De strandvoogd kan de teruggavedatum en de opslagkosten per maand
> opgeven, ook als onbekend en, voor de teruggavedatum, als niet van toepassing; een opgave gaat voor de berekening.
> 6. Wijkt een opgegeven teruggavedatum af van een berekende, dan wordt dat gemeld.

## Source (`source.rgs`)

- `teruggavedatum`, a `Datum in dagen` with both markers and the opgave marker (lid 1 to 3, 5); `opslagkosten`, a
  whole number for every month with the marker onbekend and the opgave marker (lid 4, 5).
- **teruggave op de claimdatum** (lid 1): the teruggavedatum is the claimdatum.
- **geen teruggave van drijfhout** (lid 2): niet van toepassing indien hij drijfhout is.
- **teruggave bij een onleesbaar etiket** (lid 3): onbekend indien hij onleesbaar is.
- **opslag onbekend in de winter** and **opslag in het voorjaar** (lid 4): onbekend on [2025-01-01, 2025-03-01),
  10 on [2025-03-01, 2025-07-01).

A drijfhout or onleesbare vondst with a claimdatum would have two writers with different values (EF-016,
RUNTIME-SEMANTICS ch. 5.7); no case gives one.

## Lowering (the frontend's)

| Piece | Wire |
|---|---|
| domains | one domain per marked attribute, `teruggavedatum van een Vondst` (`waarde` with a `Date` of precision `day`, `onbekend`, `niet van toepassing`) and `opslagkosten van een Vondst` (`waarde` with a whole number, `onbekend`) |
| 1, 2 | `opgegeven teruggavedatum` and `berekende teruggavedatum` (and the same for the opslagkosten, with its timeline) carry the sum type of their marked attribute, over its domain |
| moved writers | the three rules of lid 1 to 3 write `berekende teruggavedatum`: `Construct waarde(zijn claimdatum)`, `Construct niet van toepassing`, `Construct onbekend`; the two rules of lid 4 write `berekende opslagkosten` |
| 3 | `opgave boven berekening teruggavedatum` and `opgave boven berekening opslagkosten`: the opgave default and the computation default, each with the plain reference to the member as its consequence, so the group passes a value or a marker on whole |
| 4 | `afwijking opgave teruggavedatum` attributes `afwijkende opgave teruggavedatum` under an `all` of: `opgegeven teruggavedatum` filled, `berekende teruggavedatum` filled, `opgegeven teruggavedatum` notEqual `berekende teruggavedatum` — the two compared as values of one sum type (RUNTIME-SEMANTICS ch. 9.3) |
| 5 | `controle opgave teruggavedatum`, the negated check of the characteristic; its source is the marked declaration |

On the opslagkosten, a timeline, pieces 4 and 5 are not generated (DIVERGENCES §7).

## Cases (hand-computed, absolute)

Per case the highest default that decides wins: the opgave, else the computation, else what the input holds
(ch. 7.1, 7.8). A marker is a filled value: it decides, and it is filled for the deviation. The deviation holds where
both members are filled and their values differ, tag or payload (ch. 9.3); the consistency deposits one fact
`controle opgave teruggavedatum` × V exactly where it holds.

| Case | Input | opgave | computation | teruggavedatum | deviation |
|---|---|---|---|---|---|
| case1-berekende-marker | drijfhout | leeg | niet van toepassing | **niet van toepassing** | no |
| case2-opgegeven-datum-verdringt-berekende-marker | drijfhout, opgave 12-03-2026 | 12-03-2026 | niet van toepassing | **12-03-2026** | yes |
| case3-opgegeven-marker-verdringt-berekende-datum | claimdatum 10-03-2026, opgave niet van toepassing | niet van toepassing | 10-03-2026 | **niet van toepassing** | yes |
| case4-dezelfde-marker | drijfhout, opgave niet van toepassing | niet van toepassing | niet van toepassing | **niet van toepassing** | no |
| case5-andere-marker | onleesbaar, opgave niet van toepassing | niet van toepassing | onbekend | **niet van toepassing** | yes |
| case6-opgegeven-marker-zonder-berekening | opgave onbekend | onbekend | leeg | **onbekend** | no |
| case7-opgegeven-datum-verdringt-berekende-datum | claimdatum 10-03-2026, opgave 12-03-2026 | 12-03-2026 | 10-03-2026 | **12-03-2026** | yes |
| case8-opgegeven-datum-gelijk-aan-berekende-datum | claimdatum 10-03-2026, opgave 10-03-2026 | 10-03-2026 | 10-03-2026 | **10-03-2026** | no |
| case9-niets | — | leeg | leeg | **leeg** | no |
| case10-opgave-per-maand | opgave opslagkosten 4 on [2025-02-01, 2025-05-01) and onbekend on [2025-06-01, 2025-08-01) | | | | |

The opslagkosten, per day (ch. 7.11): without an opgave (case9) **onbekend** on [2025-01-01, 2025-03-01) and **10**
on [2025-03-01, 2025-07-01), leeg elsewhere. In case10 the opgave decides the days on which it holds a value or a
marker and the computation the others: **onbekend** on [2025-01-01, 2025-02-01), **4** on [2025-02-01, 2025-05-01),
**10** on [2025-05-01, 2025-06-01), **onbekend** on [2025-06-01, 2025-08-01), leeg elsewhere.

Every case plays in ref, in the ASP backend and on both branches of the Drools backend.
