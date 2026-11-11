# jutterswet-rolnaam-in-twee-feittypen — one role name in two fact types

Two fact types relate a Jutter to Vondsten, and in both the far role is called `vondst`: `vondstaanmelding`
(aanmelder, vondst) and `vondstberging` (berger, vondst). Two rules count per Jutter: `aangemeld`, the number of
vondsten over `vondstaanmelding`, and `geborgen`, the number over `vondstberging`; each names its relation in the
`sourceRole` of its count. An object creation "berg een vondst" makes one Vondst for every Jutter and links it over
`vondstberging`.

A link belongs to its fact type (RUNTIME-SEMANTICS ch. 5.7a), and a count reads the relation its `sourceRole` names
(ch. 3.4a). An executor that keys a link on the role name alone counts the links of both fact types in both rules.

## The hand calculation

| case | input links | made link | aangemeld | geborgen |
|---|---|---|---|---|
| case1-invoerlinks | V1 aangemeld by J1; V2 and V4 geborgen by J1; V3 both | one geborgen Vondst for J1 | {V1, V3} = **2** | {V2, V3, V4, made} = **4** |
| case2-gemaakte-link | none (J1 alone) | one geborgen Vondst for J1 | **0** | **1** |

## Provenance

Own work, 2026-11-11, derived from `jutterswet-vondstunie`. Before the ASP backend kept the fact type in its role
atoms, it counted the links of both fact types. Measured at admission: verifier PASS; ref and the ASP backend play both
cases. The Drools backend refuses the golden (`EXCLUDE DROOLS-233`: its bean model derives no reference field to
Vondst for the role `vondst` on Jutter), listed in gate 12's `EXPECTED_CHANNEL`.
