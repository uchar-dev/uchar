# jutterswet-leeg-deeltal — an empty dividend does not read its divisor

One rule: the loon of a Vondst is its bonus divided by (its opbrengst divided by its aandeel). An empty dividend
decides the division before the divisor is read (RUNTIME-SEMANTICS ch. 1.4 point 4, ch. 2.2): leeg ÷ x is 0, and the
divisor is not evaluated, so a division by zero inside it deposits nothing. A filled dividend reads the divisor; a
divisor that divides by a numeric zero is leeg with a §9.5 deposit of kind EF-005, and a division by that leeg divisor
is leeg with one of kind EF-031 (ch. 1.4 point 4).

## The hand calculation

| case | bonus | opbrengst | aandeel | divisor | loon | §9.5 channel |
|---|---|---|---|---|---|---|
| case1-leeg-deeltal | leeg | 90 | 0 | not read | **0** | empty: consistent |
| case2-gevuld-deeltal | 60 | 90 | 3 | 90 ÷ 3 = 30 | 60 ÷ 30 = **2** | empty: consistent |
| case3-gevuld-deeltal-nuldeler | 60 | 90 | 0 | 90 ÷ 0 = leeg, EF-005 | 60 ÷ leeg = **leeg**, EF-031 | "loon van een vondst" × V1 with EF-005 and EF-031 |

Case 1 and case 3 differ in the dividend alone: the division by zero in the divisor surfaces only where the dividend
is filled. An executor that reads the divisor first deposits EF-005 in case 1 and fails it on `consistent`.

## Provenance

Own work, 2026-11-11, the worked example of ch. 2.2 without its rounding and with a third case. Before the executors
read the dividend first, ref and the ASP backend reported an inconsistency in case 1. Measured at admission: verifier
PASS; ref, the ASP backend and both branches of the Drools backend play the three cases.
