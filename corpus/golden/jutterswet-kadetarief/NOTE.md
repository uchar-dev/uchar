# jutterswet-kadetarief — a fold over months with more than one negative factor

The fold over a timeline (`spec/RUNTIME-SEMANTICS.md` ch. 2.6, rule 8; ch. 3.6) multiplies by the grid unit, and the
negative factor of the class of that unit rises one power. A harbour charges a tariff per metre of quay per month, so an
attribute in `€/m/mnd` on a grid of months totals to `€/m`: the month goes, the metre stays. Which of the two factors goes
is for the grid to say; the unit alone does not tell. Both routes of `het totaal van` are here: the subject's own timeline
(`havengeld per meter`) and the per-element fold over a set (`kadeopbrengst per meter`).

Hand computation, `case.json`, `asOfDate` 2026-03-15:

| fold | input | calculation | result |
|---|---|---|---|
| `havengeld per meter` of H1 | `havengeldtarief` 10 at the date, the bucket of March | 10 × 1 month | 10 `€/m` |
| `havengeld per meter` of H2 | no tariff | an absent timeline totals 0 | 0 `€/m` |
| `kadeopbrengst per meter` of H1, S1 | 12 from January to April | 12 × 3 months = 36 | |
| `kadeopbrengst per meter` of H1, S2 | 7 in January, 5 in February | 7 + 5 = 12 | |
| `kadeopbrengst per meter` of H1 | S1 and S2 | 36 + 12 | 48 `€/m` |
| `kadeopbrengst per meter` of H2 | no schuiten | an empty set totals 0 | 0 `€/m` |

Every grid factor is 1, which the integer encoding of the ASP backend needs. The flank without a factor of the grid
(`€/kg/m` over months) is no document that plays: `ref`, clingo and Drools refuse the fold by name, witnessed in
`ref/test` (`WV-2.6-rule-8-two-negative-factors-none-of-the-grid`), `backends/clingo/test` and
`TmsTimelineRefusalWitnessTest` (`twee-noemers-geen-van-het-raster`). It carries no EF code of ch. 5.7b, so it has no case in
`corpus/red`.
