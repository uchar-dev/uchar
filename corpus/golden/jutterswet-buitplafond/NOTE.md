# jutterswet-buitplafond

The **exact naar-rato-with-maximum** witness (ASP-TRANSLATION S12, the
proportional-with-cap admission narrowed 2026-08-28). One allocation
(`NAAR_RATO`, RUNTIME-SEMANTICS ch. 4.2) distributes a strandbuit's
`buitopbrengst` over its `buitjutters` in proportion to each jutter's
`aandeelgewicht`, capped per jutter by `buitplafond`, writing each share
into `buitdeel` and the undivided remainder into `buitrest` (ch. 4.3/4.4).

The allocation is **exact**: it carries no `roundingDecimals`, so `floor_d`
is the identity (ch. 4.2) and each share is `max(0, min(raw, cap))`. Every
attribute is a whole number (0 decimals), so the cap scale equals the
target scale (cdec = tdec = 0): a clipped write is an exact integer at the
target scale and always fits (S12). The raw proportional shares are
themselves exact integers here, so clingo's exactness viol — which fires on
any inexact RAW share independent of the cap — does not fire; that stricter
corner (an inexact raw clipped to a fitting integer, where clingo refuses
loudly though ref writes the cap) is deliberately NOT exercised.

## Hand-computed absolutes (RUNTIME-SEMANTICS ch. 4.2 / 4.3 / 4.4)

### case.json — the cap clips (a), one recipient stays under (b)

`buitopbrengst = 120`; weights `aandeelgewicht = {J1:1, J2:2, J3:3}`,
`Σ weight = 6`. Raw proportional shares (exact, ch. 4.2):

| jutter | weight | raw = 120·w/6 | cap | share = min(raw, cap) | reason |
|--------|--------|---------------|-----|-----------------------|--------|
| J1     | 1      | **20**        | 30  | **20**                | raw < cap → stays under (b) |
| J2     | 2      | **40**        | 25  | **25**                | raw > cap → clipped (a) |
| J3     | 3      | **60**        | 50  | **50**                | raw > cap → clipped (a) |

All raws are exact integers (`120·w mod 6 = 0` for w ∈ {1,2,3}), so no
exactness viol. `Σ share = 20 + 25 + 50 = 95`; undivided rest (ch. 4.4)
`buitrest = 120 − 95 = 25` — the capped surplus falls into the rest, not
redistributed (ch. 4.3). Every weight and cap is filled and non-negative,
so the §9.5 channel is empty → `consistent`.

### case2-onbeperkt.json — the cap clips nothing (pure proportional)

`buitopbrengst = 90`; same weights, `Σ weight = 6`; every `buitplafond =
100`. Raw shares 90·w/6 → **15, 30, 45** (all exact integers, all below
100), so no cap clips: shares = raws. `Σ share = 90`; rest `= 90 − 90 =
0`. The channel is empty → `consistent`. This second case strengthens the
pin: the same exact-with-cap form where the cap is inert reduces to plain
proportional distribution, remainder 0.

## noop refutation

Neither case supplies the allocation target `buitdeel` (that layer's
witness is `jutterswet-veilinginvoer`), and `buitrest` is written from
absence. Every expected `buitdeel`/`buitrest` value therefore differs from
the raw input (absent), so `runner --noop` fails on both cases (gate 7):
the golden is no survivor — it pins the allocation, not a standing input.
