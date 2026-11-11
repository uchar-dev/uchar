# jutterswet-wrakbasis-vervangen-basis-regel — the exceptions of a replaced base stay

A Strandrecht has a grondslag and an uitkering; Wrakrecht is a subtype of Strandrecht. The defaults of "uitkering van
een strandrecht" are one form-a default: the uitkering is the grondslag (always), with a nested exception group "99,00
if betwist". Wrakrecht has its own base, an ordinary rule: the uitkering of a Wrakrecht is its grondslag times 2.

The subtype's own base replaces the inherited base (RUNTIME-SEMANTICS ch. 7.10, block 3). A replaced form-a base loses
only its own justification and its own consequence: its exception groups stay, as inherited exceptions, an inherited
group-default at the bottom of block 2, above the subtype's own base. So a betwist Wrakrecht still gets 99,00.

## The hand calculation

Grondslag 10,00 for every object.

| object | type | betwist | deciding default | uitkering |
|---|---|---|---|---|
| S1 | Strandrecht | no | the base: grondslag | **10,00** |
| S2 | Strandrecht | yes | the nested exception | **99,00** |
| W1 | Wrakrecht | no | the own base: grondslag × 2 | **20,00** |
| W2 | Wrakrecht | yes | the inherited nested exception, above the own base | **99,00** |

An executor that drops the exceptions together with the replaced base writes 20,00 for W2.
`jutterswet-wrakbasis-vervangen-basis-groep` holds the same with the own base written as a defaults group.

## Provenance

Own work, 2026-11-11. Before the executors kept the exception groups of a replaced form-a base, ref, the ASP backend
and both branches of the Drools backend wrote 20,00 for W2. Measured at admission: verifier PASS; ref, the ASP backend
and both branches of the Drools backend play the case.
