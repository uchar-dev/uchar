# jutterswet-invoer-rol-strikt — non-reciprocal to-one roles open, declared complete, or stated empty (`rolesEmpty`)

A Jutterswet casus under `Afleiden uit leeg: nee` (`inferFromEmpty: false`, RUNTIME-SEMANTICS ch. 1.5, "Roles and
links", ch. 5.7, ch. 5.12, ch. 5.12a; IR-SPEC ch. 2.6, WF-130), covering an unlinked `to-one` input role (`vinder` on
`Vondst`), a `to-one` role declared complete with `(volledig gegeven)` (`keurmeester` on `Vondst`), a two-step
navigation (`Vondst -> vinder -> ploeg`), role checks in both polarities (`toegewezen`, `verweesd`, `gekeurd`), a
two-step `Aggregation` and `QuantifierFold` whose second hop crosses a `to-one` role with a declared plural
(`Jutter -> vondsten -> bewaarplaatsen`), a self-relation with two `to-one` roles (`Vergunning` on `opvolging`:
`vorige vergunning` and `volgende vergunning`), and bidirectional `gebaseerd op` overrides in `scenario.rgs` (including
retracting an inherited counterpart `to-many` link via `- de vinder is leeg`, and retracting an inherited `rolesEmpty`
entry from the counterpart via `- de vondsten zijn V1`). Its twin `corpus/golden/jutterswet-invoer-rol` carries the same
rules in an ordinary document. Own Jutterswet-flavoured casus, rights-free.

## Casus (own text)

> **Art. 3m (vindersregistratie, keuring, ploegketen, bewaring en opvolging).** De vindpremie van een vondst bedraagt de
> waarde vermeerderd met de bonus van haar vinder; de ketenpremie bedraagt de waarde vermeerderd met de ploegtoelage van
> de ploeg van haar vinder. De keurmeester van een vondst wordt volledig opgegeven; de keurtoeslag bedraagt de waarde
> vermeerderd met de bonus van die keurmeester, en een vondst geldt als gekeurd indien zij een gekeurde vondst is. Een
> vondst is toegewezen indien zij een vondst van een vinder is en verweesd indien zij dat niet is. De loodscapaciteit
> van een jutter is de som van de capaciteit van alle bewaarplaatsen van zijn vondsten; hij geldt als ruim bewaard
> indien de capaciteit van elk van die bewaarplaatsen groter is dan 5. Bij vergunningopvolging wordt de vorige
> vergoeding gesteld op de vergoeding van de vorige vergunning; een vergunning is opvolgend indien zij een volgende
> vergunning is en opgevolgd indien zij een vorige vergunning is.

## Source (`source.rgs`) and scenarios (`scenario.rgs`)

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the six case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand.

## Cases (hand-computed, absolute)

| case | setup | key outcomes |
|---|---|---|
| case1-rol-weggelaten | `V1` (`waarde = 100`); `vinder`, `keurmeester` (`complete: true`) and `bewaarplaats` omitted | `vindpremie`, `ketenpremie`, `toegewezen`, `verweesd` open; `keurtoeslag = 100`, `gekeurd` absent |
| case2-rol-vaststaand-leeg | `J1` (`bonus = 20`, `ploeg` empty), `V1` (`waarde = 100`, `vinder` and `bewaarplaats` in `rolesEmpty`) | `vindpremie = 100`, `ketenpremie = 100`, `keurtoeslag = 100`, `toegewezen` absent, `verweesd` present, `loodscapaciteit` empty, `ruim bewaard` present |
| case3-tweede-stap-en-vouwbereik-open | `J1` (`bonus = 20`, `vondsten = [V1, V2]`, `ploeg` omitted), `V1` (`bewaarplaats = L1`), `V2` (`bewaarplaats` omitted) | `V1.vindpremie = 120`, `V1.ketenpremie` open (hop 2 `ploeg` open), `J1.loodscapaciteit` and `J1.ruim bewaard` open (`V2.bewaarplaats` open) |
| case4-leeg-vervangen-vanuit-tegenobject | based on `zonder vinder`, overridden from `J1` (`vondsten = V1`, `gekeurde vondsten = V1`, `ploeg = P1`) and `V1` (`bewaarplaats = L1`) | `vindpremie = 120`, `ketenpremie = 115`, `keurtoeslag = 120`, `toegewezen` & `gekeurd` present, `loodscapaciteit = 12`, `ruim bewaard` present |
| case5-tegenkoppeling-vervangen-door-leeg | based on `met tegenkoppeling` (`J1.vondsten = V1`), overridden on `V1` with `- de vinder is leeg` | `J1.vondsten` cleared and `"vinder"` in `V1.rolesEmpty`: `vindpremie = 100`, `ketenpremie = 100`, `keurtoeslag = 120`, `verweesd` & `gekeurd` present, `loodscapaciteit` empty |
| case6-zelfrelatie-vergunningen | `G1` (`vergoeding = 10`, `vorige vergunning` in `rolesEmpty`), `G2` (`vergoeding = 20`, `vorige vergunning = G1`, `volgende vergunning` omitted) | `G1.vorige vergoeding` empty, `G1` not `opvolgend`, `G1` `opgevolgd`; `G2.vorige vergoeding = 10`, `G2` `opvolgend`, `G2.opgevolgd` open |

`ref`, `clingo` and both branches of the Drools backend (`exec-model` and `DRL-text`) play all six cases and agree on
their readings and end states (gate 17).
