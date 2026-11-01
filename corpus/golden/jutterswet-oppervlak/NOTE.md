# jutterswet-oppervlak — a product of two lengths is a square

`met eenheid m^2` declares one factor `m` with exponent 2 (`spec/IR-SPEC.md` ch. 2.5, point 5). `lengte maal breedte` is
`m^2` by rule 2 of `spec/RUNTIME-SEMANTICS.md` ch. 2.6, `oppervlakte gedeeld door lengte` is `m` by rule 3, and the
threshold `100 m^2` is compared in the same unit. Every factor is 1, which the integer encoding of the ASP backend needs.
`scenario.rgs` is the source of the three case files (gate 16).

| scenario | lengte | breedte | oppervlakte | teruggerekende breedte, naar beneden | ruim (> 100 m^2) |
|---|---|---|---|---|---|
| ruim strandvak | 20 m | 8 m | 160 m^2 | 160 ÷ 20 = 8 m | ja |
| krap strandvak | 5 m | 6 m | 30 m^2 | 30 ÷ 5 = 6 m | nee |
| op de drempel | 25 m | 4 m | 100 m^2 | 100 ÷ 25 = 4 m | nee: 100 is niet groter dan 100 |
