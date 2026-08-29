# jutterswet-bergingsdrempel — ordered kenmerk exception (Uitzondering op)

Exercises the uchar-dialect **ordered exception** form (LADDER-EVOLUTIE §4.2):
a base rule with one `Uitzondering op` block, lowered by the RegelSpraak
frontend to ONE `DefaultLadder` on the characteristic `bergingsbevoegd`.
Own Jutterswet-flavoured domain (berging of wrecks), own numbers.

## Source (`bron.rgs`)

- Base rule **bergingsbevoegd bij quotum**: a Jutter is `bergingsbevoegd`
  when `aantal geborgen wrakken >= 20`.
- Exception **bergingsbevoegd bij noodweer** (`Uitzondering op bergingsbevoegd
  bij quotum`): lowers the threshold to 8 when the emergency ground
  `noodweer verklaard` holds.

## Lowering

One `DefaultLadder` (kenmerk `bergingsbevoegd`, onderwerp `Jutter`), rungs in
descending priority:

1. **exception rung** — `besluit true`, justified by `noodweer verklaard AND
   aantal geborgen wrakken >= 8`.
2. **base rung** — `besluit true`, justified by `aantal geborgen wrakken >= 20`.

Both the base rule and the exception leave `resolvedRegels` (WF-050
single-writer: their semantics live in the ladder); each keeps a `Regel` stub
in `regels`. The base rule's `geldig altijd` is carried onto the ladder as
`geldigheid {vanaf:null, tot:null}`.

## Case derivations (hand-computed, absolute)

Walk top-down; highest deciding rung wins. Both rungs decide `true`, so the
ladder attributes `bergingsbevoegd` iff the exception OR the base is justified.

| case | wrakken | noodweer verklaard | exception (>=8 & noodweer) | base (>=20) | bergingsbevoegd |
|------|---------|--------------------|----------------------------|-------------|-----------------|
| case1-quotum        | 20 | no  | no (no noodweer) | **yes** | **present** |
| case2-noodweer      | 8  | yes | **yes**          | no      | **present** |
| case3-onder-quotum  | 12 | no  | no (no noodweer) | no (12<20) | **absent** |
| case4-onder-noodweer| 5  | yes | no (5<8)         | no      | **absent** |

All four are `consistent` — a single characteristic ladder never conflicts.
case2 shows the exception lowering the threshold; case3 shows the exception is
inert without the ground; case4 shows the lowered threshold still has a floor.
