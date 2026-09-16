# jutterswet-muntweging — a write whose TARGET slot is declared on the ancestor

`corpus/golden/jutterswet-vangstklem` pins unit restating (`kg` against its
sister `t`, one system, factor 1000) on a document with no inheritance in it.
This golden is that arithmetic with a subtype under it, and it moves the
inheritance to the one position the other subtype goldens leave alone: not the
fold's element set and not the rule's SUBJECT, but the attribute the rule
WRITES. `Munt ⊑ Vondst`, `geborgen gewicht` is declared on `Vondst`, and the
rule that computes it has `Munt` as its subject — so the write reaches a slot
the subject's own declaration does not carry.

`IR-SPEC` ch. 2.2 makes that an ordinary write: the effective declaration of a
type is its own members closed over the supertype chain, so `Munt` has a
`geborgen gewicht` in `kg` and it is the same slot `Vondst` declares. The two
rules are here to say that in one document: `aangegeven gewicht` is written by
a rule whose subject is `Vondst`, which fires for the munten as well (ch. 2.2,
the inclusion direction), and it does the SAME arithmetic. On a `Munt` the two
attributes must therefore hold the same number, reached through a target
declared on the instance's own effective type in both cases but through a
RULE SUBJECT that is the ancestor in one and the subtype in the other.

What the document catches is a unit question rather than a value one. Before a
unit-carrying additive join is translated to ASP, a normalisation pass restates
every literal of the target's unit system into that system's finest base unit,
so the write fence sees one identical unit record instead of two convertible
ones. That pass looks up the TARGET's declared unit, and until 2026-09-16 it
looked it up in the raw declaration map: for `Munt` the slot was simply absent,
the pass answered "no unit" and left the rule untouched, and the fence then
refused. Measured on this document before the correction: `ref` 1002,00 and
the Drools backend the same, and clingo `TRANSLATION FAULT: non-identical
units under an additive join are not translated`. It is the same defect class
as the temporality lookup `jutterswet-muntbewaring` exposed, one file over.

## Case derivation (hand-computed, absolute)

`1 t = 1000 kg` by the declared factor, and the target is `kg`, which is the
finest base unit of the `Gewicht` system — so every sum below is done in `kg`
after restating the literal exactly once.

| instantie | type | brutogewicht | `aangegeven gewicht` (regel op Vondst) | `geborgen gewicht` (regel op Munt) |
|---|---|---|---|---|
| V1 | Vondst | 3,00 kg | 3,00 + 1000,00 = **1003,00** | **leeg** |
| M1 | Munt | 2,00 kg | 2,00 + 1000,00 = **1002,00** | 2,00 + 1000,00 = **1002,00** |
| M2 | Munt | 0,50 kg | 0,50 + 1000,00 = **1000,50** | 0,50 + 1000,00 = **1000,50** |

V1 carries the inclusion direction and its counterpart on one row: the
supertype rule fires for it, and the SUBTYPE rule does not, so `geborgen
gewicht` stays empty on a plain vondst — a rule subject is not widened
upwards.

M1 and M2 are the cross-check. The two columns hold the same number by
construction, so a restating that fires on one path and not on the other shows
up as a disagreement inside one instance rather than as a number nobody can
check. M2's 0,50 keeps the second decimal load-bearing: an executor that
restated `1 t` to a whole 1000 and then dropped the operand's decimals would
answer 1000,00 there and still be right on M1.
