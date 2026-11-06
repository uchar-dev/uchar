# jutterswet-lege-pot-en-negatief-gewicht — an empty pot and a negative weight in an allocation

Two allocations (`RUNTIME-SEMANTICS.md` ch. 4) share one source and one set of recipients: the `opbrengst` of a
Berging goes through the fact type `bergingsdeelname` to its Bergers, once in equal shares (`gelijke aandeel`, rest in
`onverdeelde rest`) and once in proportion to the `geborgen gewicht` (`rato-aandeel`, rest in `ratorest`), both floored
at 0 decimals. The golden pins the two v1 rows of ch. 4.4 that differ from the v0 mirror:

- **an empty pot is no distribution, in every method**: no share and no rest is written, and nothing surfaces on the
  §9.5 channel, also not for an empty weight beside it;
- **a negative weight in the proportional method counts as 0**: its recipient stays in the split and is assigned a
  real 0, and the weight surfaces as a §9.5 error (EF-038, ch. 5.7b).

## The hand calculation

| case | instance | input | gelijke aandeel | rato-aandeel |
|---|---|---|---|---|
| case1-lege-pot | G1, no opbrengst | — | rest **leeg** | rest **leeg** |
| | B1 of G1 | gewicht 3, gelijke aandeel 7 | **7** (the input stands) | **leeg** |
| | B2 of G1 | no gewicht | **leeg** | **leeg**, no error |
| | G2, opbrengst 10 | — | rest **0** | rest **0** |
| | B3 of G2 | gewicht 1 | **10** | **10** |
| case2-negatief-gewicht | G1, opbrengst 100 | — | rest 100 − 99 = **1** | rest 100 − 100 − 0 = **0** |
| | B1 of G1 | gewicht 3 | ⌊100 / 3⌋ = **33** | 100 × 3 / 3 = **100** |
| | B2 of G1 | gewicht −1 | **33** | weight 0: **0**, and EF-038 |
| | B3 of G1 | no gewicht | **33** | excluded: **leeg**, and EF-028 |

In case 2 the eligible weights of the proportional split are 3 and 0 (the −1 counted as 0), so the sum is 3. Read
unclamped, the v0 hole, the sum would be 2 and the shares 150 and −50. G2 in case 1 lets the no-op guard (gate 7) see
a written value beside the empty pot.

## Provenance

Own work (card `verdeling-lege-pot-en-negatief-gewicht`, 2026-11-06), the edge rows of part D of the witness
`TmsAllocationWitnessTest` of the Drools backend, written as a RegelSpraak source. Measured at admission: verifier
PASS; before the repair the reference interpreter faulted on case 1 ("empty source amount in equal-shares allocation
(v0 mirror hole, ch. 4.4)") and wrote 150 and −50 in case 2 without a channel fact, and the ASP runner was
unsatisfiable on its guards `viol("allocation: empty pot","G1")` and `viol("allocation: negative weight","B2")`;
after it, ref, clingo and both branches of the Drools backend play both cases (11 and 10 expectations).
