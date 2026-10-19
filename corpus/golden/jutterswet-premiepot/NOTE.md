# jutterswet-premiepot — a fact creation links the members of a role to a created object

A Haven has havenjutters (`HavenHeeftJutter`). The object creation
`premiepot` gives every Haven one Premiepot (`HavenHeeftPremiepot`) whose pot
is the number of havenjutters of the Haven times the parameter `de premie per
havenjutter`. The fact creation `premiejutters` makes every havenjutter of the
premiehaven of a Premiepot a premiejutter of that Premiepot
(`PremiepotVoorJutters`): it walks from the created Premiepot back to its
Haven and on to the Jutters that exist, and creates no Jutter. Three rules
read the result: the jutteraantal of a Premiepot counts its premiejutters,
the premietotaal and the premietal of a Haven read the pot and the
jutteraantal of its premiepot, and the characteristic `premiegerechtigd` holds
for a Jutter that is a premiejutter (a role check on the created relation).

## The dependency graph

The object creation reads the population of Jutter and the relation
`HavenHeeftJutter` (the count over the role), and writes the population of
Premiepot, the relation `HavenHeeftPremiepot` (its back-link) and the pot. The
fact creation reads `HavenHeeftPremiepot` and `HavenHeeftJutter` (its hop and
its join) and writes `PremiepotVoorJutters` and nothing else
(RUNTIME-SEMANTICS ch. 6.1). The role check of `premiegerechtigd` reads
`PremiepotVoorJutters`, stratum-strict, because a characteristic attribution
writes irrevocably. There is no cycle: the creation is stratum 0, the fact
creation stratum 1, `premiegerechtigd` stratum 2.

Until 2026-10-06 a fact creation also wrote the population of the filler of
its target role, here Jutter, as a proxy. The count of the object creation
reads that population, so the graph had the edge premiejutters → premiepot
beside the real edge premiepot → premiejutters, and the verifier refused the
document under WF-045 (`dependency cycle through no recursive creation:
premiepot -> premiejutters -> premiepot`); `ref/` refused it on the same
ground, while the ASP backend and both Drools branches answered the values
below. The casus of the parity zone has the same shape in its rules
`vastgestelde contingent treinmiles` and `passagier met recht op treinmiles`.
The role check read nothing until the same date; the edge to
`premiegerechtigd` is the one it now draws.

The order of the two creations in `source.rgs` does not matter: with the fact
creation written above the object creation the frontend writes the same
document byte for byte. Until 2026-10-06 it dropped the fact creation then,
with the WARNING "referentieveld-zijde voor tussenstap 'premiehaven' niet af
te leiden", because it only counted an object creation that stood earlier in
the source (`SourceOrderTest`).

## Hand computation

`case1-twee-havens.json`: de premie per havenjutter is 50. J1 and J2 are
havenjutters of H1, J3 of H2; H3 has none and J4 has no haven.

- H1: 2 havenjutters, pot 2 × 50 = **100**; its premiepot gets J1 and J2 as
  premiejutters, jutteraantal 2, premietal **2**.
- H2: 1 havenjutter, pot **50**; premiejutter J3, premietal **1**.
- H3: 0 havenjutters, pot 0 × 50 = **0**; the creation has no condition, so
  H3 gets a premiepot as well, with no premiejutters: premietal **0**.
- J1, J2 and J3 are premiejutters: **premiegerechtigd**. J4 is no
  premiejutter of any premiepot: **niet premiegerechtigd**.

## Executors

ref and clingo replay the case; both Drools branches play it. The case refutes
the do-nothing executor.
