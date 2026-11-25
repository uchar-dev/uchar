# jutterswet-invoer-leeg — scalar attributes stated to have no value in an ordinary document

The rules of `corpus/golden/jutterswet-invoer-leeg-strikt` (art. 3k) in an ordinary document, without the line
`Afleiden uit leeg: nee`. Own Jutterswet-flavoured casus, rights-free.

The document reads a missing value as RUNTIME-SEMANTICS ch. 1.2 lays down: an attribute the case gives no value is
`leeg`. A case may still name an attribute in `attributesEmpty` (the scenario's `- de|het <attr> is leeg`); in this
document that changes no outcome (ch. 1.5), and the frontend warns with RGS-473 at each bullet that does not replace an
inherited filled value from an earlier situation block (`case2-als-leeg-gegeven`), while staying silent where `- de
gewenste afhaal-dag is leeg` and `- het strandloon is leeg` retract inherited filled values in
`case3-waarde-vervangen-door-leeg`.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the three case files are the lowering of
`scenario.rgs` (gate 16), whose expectations are computed by hand.

## Cases (hand-computed, absolute)

| case | input | gewenste afhaal-dag | opslagtermijn | direct vrijgegeven | vindloon | bergpremie | strandloon |
|---|---|---|---|---|---|---|---|
| case1-niets-genoemd | `taxatie = 50` | `leeg` | 30 | holds | `leeg` | 4 | 5 |
| case2-als-leeg-gegeven | `taxatie = 50`; `gewenste afhaal-dag`, `vindloon`, `bergpremie`, `strandloon` in `attributesEmpty` | `leeg` | 30 | holds | `leeg` | 4 | 5 |
| case3-waarde-vervangen-door-leeg | based on `met opgave` (`14`, `8`), overridden with `is leeg` | `leeg` | 30 | holds | `leeg` | 4 | 5 |

All three cases produce identical outcomes across `ref`, `clingo` and both branches of the Drools backend.
