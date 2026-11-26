# jutterswet-invoer-rol — non-reciprocal to-one roles stated empty in an ordinary document

The rules of `corpus/golden/jutterswet-invoer-rol-strikt` (art. 3m) in an ordinary document, without the line
`Afleiden uit leeg: nee`. Own Jutterswet-flavoured casus, rights-free.

The document reads an unlinked role as RUNTIME-SEMANTICS ch. 1.2 and ch. 5.8 lay down: a role on which the case gives
no link is absent, and `(volledig gegeven)` on `keurmeester` changes no outcome either. A case may still name a
non-reciprocal `to-one` role in `rolesEmpty` (the scenario's `- de|het <rol> is leeg`); in this document that changes
no outcome (ch. 1.5), and the frontend warns with RGS-480 at each bullet that does not retract an inherited link from an
earlier situation block (`case2-als-leeg-gegeven`), while staying silent where `- de vinder is leeg` retracts the link
inherited from `J1`'s `- de vondsten zijn V1` in `case3-tegenkoppeling-vervangen-door-leeg`.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the three case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand.

## Cases (hand-computed, absolute)

| case | input | vindpremie | ketenpremie | keurtoeslag | toegewezen | verweesd | gekeurd |
|---|---|---|---|---|---|---|---|
| case1-niets-genoemd | `V1` (`waarde = 100`) | 100 | 100 | 100 | absent | holds | absent |
| case2-als-leeg-gegeven | `V1` (`waarde = 100`; `vinder`, `bewaarplaats` in `rolesEmpty`) | 100 | 100 | 100 | absent | holds | absent |
| case3-tegenkoppeling-vervangen-door-leeg | based on `met tegenkoppeling` (`J1.vondsten = V1`, `gekeurde vondsten = V1`), overridden on `V1` with `vinder is leeg` | 100 | 100 | 120 | absent | holds | holds |

All three cases produce identical outcomes across `ref`, `clingo` and both branches of the Drools backend.
