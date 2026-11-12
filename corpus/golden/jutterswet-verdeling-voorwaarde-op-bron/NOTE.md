# jutterswet-verdeling-voorwaarde-op-bron — the condition of an allocation reads its source as subject

An allocation with a condition: "De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers
van de berging, waarbij wordt verdeeld in gelijke delen indien zijn opbrengst groter is dan 10 €." The subject of the
condition ("zijn") is the holder of the amount, the Berging (IR-SPEC ch. 3.2: the allocation runs per holder of its
source; its condition is evaluated on that holder).

## The hand calculation

| Berging | opbrengst | condition 'opbrengst > 10 €' | bergers | bergingsaandeel per berger |
|---|---|---|---|---|
| B1 | 20 € | true | X1, X2 | 20 ÷ 2 = **10 €** each |
| B2 | 5 € | false | Y1 | not allocated: **leeg** |

No rule writes twice, so the case is consistent.
