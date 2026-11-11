# jutterswet-wrakbasis-vervangen-basis-groep — the exceptions of a replaced base stay, own base as a group

The document of `jutterswet-wrakbasis-vervangen-basis-regel`, with the own base of Wrakrecht written as a defaults
group "uitkering van een wrakrecht" of one default (always: the grondslag times 2) instead of an ordinary rule.

The defaults of "uitkering van een strandrecht" are one form-a default: the uitkering is the grondslag (always), with a
nested exception group "99,00 if betwist". The subtype's own base replaces the inherited base (RUNTIME-SEMANTICS ch.
7.10, block 3); the replaced form-a base loses only its own justification and its own consequence, and its exception
groups stay as an inherited group-default at the bottom of block 2, above the subtype's own base. An own base written
as a defaults group composes into the same stack as one written as a rule.

## The hand calculation

Grondslag 10,00 for every object.

| object | type | betwist | deciding default | uitkering |
|---|---|---|---|---|
| S1 | Strandrecht | no | the base: grondslag | **10,00** |
| S2 | Strandrecht | yes | the nested exception | **99,00** |
| W1 | Wrakrecht | no | the own base: grondslag × 2 | **20,00** |
| W2 | Wrakrecht | yes | the inherited nested exception, above the own base | **99,00** |

An executor that drops the exceptions together with the replaced base writes 20,00 for W2.

## Provenance

Own work, 2026-11-11. Before the executors kept the exception groups of a replaced form-a base, ref, the ASP backend
and both branches of the Drools backend wrote 20,00 for W2. Measured at admission: verifier PASS; ref, the ASP backend
and both branches of the Drools backend play the case.
