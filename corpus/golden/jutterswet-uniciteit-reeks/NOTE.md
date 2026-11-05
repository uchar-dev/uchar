# jutterswet-uniciteit-reeks — a uniqueness over a union with a text key

"De reeksen van alle Strandvondsten verenigd met de reeksen van alle
Wrakvondsten moeten uniek zijn." The key is the `reeks`, a text, and the two
object types form one key domain (`spec/RUNTIME-SEMANTICS.md` ch. 5.9, rule
1): a duplicate key can arise across the two types and within one. Every
object whose key occurs more than once gets the fault under the name of the
uniqueness, EF-026 on the §9.5 channel. A text key compares by its value,
as a number does; a union harmonises the decimals of a numeric key across
its sources, and a text has none. Own Jutterswet-flavoured casus,
rights-free.

## Casus (own text)

> **Art. 6c (reeks van een vondst).** De reeks van een strandvondst of een
> wrakvondst komt bij geen andere strandvondst of wrakvondst voor.

## Cases (hand-computed, absolute)

| case | Strandvondsten | Wrakvondsten | outcome |
|---|---|---|---|
| `case1-botsing` | S1 "A", S2 "B" | W1 "A" | S1 and W1 **inconsistent**; S2 not |
| `case2-leeg-type` | S1 "A", S2 "A" | — | S1 and S2 **inconsistent** |

In case 1 the key "A" occurs in both types. In case 2 the Wrakvondst has no
instances, and the duplicate lies within the Strandvondsten: the union over
a type without instances is the union of the other source alone.
