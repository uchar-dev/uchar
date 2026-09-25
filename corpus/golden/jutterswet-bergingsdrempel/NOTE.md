# jutterswet-bergingsdrempel — ordered kenmerk exception (Uitzondering op)

Exercises the uchar-dialect **ordered exception** form
(PRIORITIZED-DEFAULTS-EVOLUTIE §4.2): a base rule with one `Uitzondering
op` block, lowered by the RegelSpraak frontend to ONE `PrioritizedDefaults`
on the characteristic `bergingsbevoegd`. Own Jutterswet-flavoured domain
(berging of wrecks), own numbers.

## Source (`bron.rgs`)

- Base rule **bergingsbevoegd bij quotum**: a Jutter is `bergingsbevoegd`
  when `aantal geborgen wrakken >= 20`.
- Exception **bergingsbevoegd bij noodweer** (`Uitzondering op bergingsbevoegd
  bij quotum`): lowers the threshold to 8 when the emergency ground
  `noodweer verklaard` holds.

## Lowering

One `PrioritizedDefaults` (characteristic `bergingsbevoegd`, subject `Jutter`),
defaults in descending priority:

1. **exception default** — `decision true`, justified by `noodweer verklaard
   AND aantal geborgen wrakken >= 8`.
2. **base default** — `decision true`, justified by
   `aantal geborgen wrakken >= 20`.

Both the base rule and the exception leave `rules` (WF-050
single-writer: their semantics live in the defaults). Until L1 each also kept
a `Regel` stub in the `regels` register, which left the wire with the M1
card (B3). The base rule's `geldig altijd` is carried onto the defaults as
a `validity` with neither `from` nor `to`.

## Case derivations (hand-computed, absolute)

Walk top-down; the highest deciding default wins. Both defaults decide
`true`, so the defaults attribute `bergingsbevoegd` iff the exception OR the
base is justified.

| case | wrakken | noodweer verklaard | exception (>=8 & noodweer) | base (>=20) | bergingsbevoegd |
|------|---------|--------------------|----------------------------|-------------|-----------------|
| case1-quotum        | 20 | no  | no (no noodweer) | **yes** | **present** |
| case2-noodweer      | 8  | yes | **yes**          | no      | **present** |
| case3-onder-quotum  | 12 | no  | no (no noodweer) | no (12<20) | **absent** |
| case4-onder-noodweer| 5  | yes | no (5<8)         | no      | **absent** |

All four are `consistent` — a single set of characteristic defaults never
conflicts.
case2 shows the exception lowering the threshold; case3 shows the exception is
inert without the ground; case4 shows the lowered threshold still has a floor.
