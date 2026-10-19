# jutterswet-penningbesluit — an object creation whose back-link follows from the type pair

A hand-computed Jutterswet golden written in RegelSpraak, on art. 3 jo. 8:
the strandvoogd stelt voor iedere jutter die over een penningjaar recht op
de penning heeft een penningbesluit vast, and the besluit vermeldt het
penningjaar en het penningbedrag. The object creation that makes the
penningbesluit carries **no `connectingRole` binding**: its sentence names
the created TYPE, not a role, so the back-link is the uniquely-derivable
connecting relation of `RUNTIME-SEMANTICS.md` ch. 5.5 (`IR-SPEC.md` ch. 3.3).
Rules read the made object back over that relation in both directions. Gate
10 re-emits `ir.json` from `source.rgs` byte for byte.

The parameters follow art. 7 and the overgangsrecht for the penningjaar
2025 in `corpus/jutterswet/wet-v4.md`: a vondstdrempel of 12 and a
penningbedrag of 250 euro.

## Source (`source.rgs`)

- `Objecttype de Jutter` with the characteristic `penninggerechtigd` and
  the attributes `aantal aangemelde vondsten` and `penningjaar` (input),
  `uitkering` (€), `uitkeringsjaar` and `aantal penningbesluiten` (derived).
- `Objecttype het Penningbesluit` with `besluitjaar` and `besluitbedrag` (€).
- `Feittype penningtoekenning` with the roles `begunstigde` (a `Jutter`) and
  `toegekend besluit` (a `Penningbesluit`, plural `toegekende besluiten`),
  one to one. Neither role carries the name of its filler type.
- Rule `penningrecht bij drempel` (art. 3): a Jutter is penninggerechtigd if
  his `aantal aangemelde vondsten` is at least the vondstdrempel.
- Object creation `stel het penningbesluit vast` (art. 8):
  `Een Jutter heeft een Penningbesluit met besluitbedrag gelijk aan het
  penningbedrag indien hij penninggerechtigd is.`
- Rule `besluitjaar van het penningbesluit`: the `besluitjaar` of a
  Penningbesluit is the `penningjaar` of zijn begunstigde — a read from the
  made object back to its subject.
- Rules `uitkering volgens het penningbesluit` and `uitkeringsjaar volgens
  het penningbesluit`: the `uitkering` and the `uitkeringsjaar` of a Jutter
  are the `besluitbedrag` and the `besluitjaar` of zijn toegekend besluit — a
  read from the subject to the made object.
- Rule `tel de penningbesluiten`: the `aantal penningbesluiten` of a Jutter
  is the count of his toegekende besluiten, 0 or 1 over the to-one role.

**Why there is no binding.** The frontend writes a `connectingRole` where the
creation's noun is the declared name or plural of a role the created type
fills in a fact type that also relates the subject
(`RegelSpraakModel.namedRoleBinding`). Here the noun is the type name
`Penningbesluit`, and no role is declared under that name, so the frontend
writes none: `/rules/objectCreations/0/versions/0` has no `connectingRole`.
The back-link is then derived from the type pair: `penningtoekenning` is the
only fact type that relates `Jutter` and `Penningbesluit`, the made
Penningbesluit fills `toegekend besluit` and the subject fills
`begunstigde`. Both roles are to-one, so the creation has the 1:1 shape: an
instance already linked toward the subject in the input world blocks it
(ch. 5.5).

## Hand computation

Every case supplies the vondstdrempel 12 and the penningbedrag 250. The
document has no consistency rule, so every case also expects `consistent`.

### Case 1 — two penninggerechtigde jutters (`case1-twee-besluiten.json`)

Input: **J1** with 14 aangemelde vondsten and penningjaar 2025; **J2** with
12 aangemelde vondsten and penningjaar 2024.

1. J1: 14 ≥ 12, so J1 is **penninggerechtigd**. J2: 12 ≥ 12 (the bound is
   inclusive, "groter of gelijk"), so J2 is **penninggerechtigd** too.
2. The creation makes one Penningbesluit per jutter, with besluitbedrag =
   penningbedrag = 250, and links it over `penningtoekenning`: the made
   besluit in `toegekend besluit` of its jutter, the jutter in `begunstigde`
   of the besluit. Neither jutter has a besluit in the input, so neither
   creation is blocked.
3. `besluitjaar` of each made besluit = the `penningjaar` of its begunstigde:
   2025 for J1's, 2024 for J2's.
4. `uitkering` of J1 and of J2 = the besluitbedrag of their own besluit =
   **250** each.
5. `uitkeringsjaar` of J1 = **2025**, of J2 = **2024**: each value went from
   the jutter to its besluit and back, so each besluit is linked to its own
   jutter in both directions.
6. `aantal penningbesluiten` = **1** for each jutter.

### Case 2 — below the vondstdrempel (`case2-onder-de-drempel.json`)

Input: **J3** with 11 aangemelde vondsten and penningjaar 2025.

1. 11 < 12, so J3 is **not** penninggerechtigd.
2. The condition of the creation does not hold: nothing is made and nothing
   is linked.
3. `uitkering` and `uitkeringsjaar` of J3 read zijn toegekend besluit, and
   there is none: both stay **leeg**.
4. `aantal penningbesluiten` = **0**: a count over an empty set is 0, never
   leeg (ch. 1.2e).

### Case 3 — a besluit that already exists (`case3-eerder-besluit.json`)

Input: **J4** with 13 aangemelde vondsten, penningjaar 2025 and
`toegekend besluit: [B1]`; **B1**, a Penningbesluit with besluitbedrag 200
and no `besluitjaar`.

1. The supplied link establishes the relation itself (ch. 5.7a): J4's
   toegekend besluit is B1, and B1's begunstigde is J4.
2. 13 ≥ 12, so J4 is **penninggerechtigd**.
3. The creation is **blocked**: the created-side role `toegekend besluit` is
   to-one and B1 fills it toward J4 in the input world. Nothing is made.
4. `besluitjaar` of B1 = the penningjaar of its begunstigde J4 = **2025**,
   read over the reverse leg of the supplied link.
5. `uitkering` of J4 = the besluitbedrag of B1 = **200**, not the
   penningbedrag 250.
6. `uitkeringsjaar` of J4 = the besluitjaar of B1 = **2025**.
7. `aantal penningbesluiten` = **1**.

A creation that ignored the guard would give J4 a second filler of the
to-one role `toegekend besluit`, which ch. 5.5 makes a fault, so the case
would not pass.

## What the cases observe

Case 1 depends on the creation. Measured 2026-10-11 with
`rules.objectCreations` emptied (the document stays verifier-clean): ref and
clingo leave `uitkering` and `uitkeringsjaar` leeg and count 0 for both
jutters, and case 1 fails. Cases 2 and 3 stay green under that removal: in
case 2 the condition does not hold and in case 3 the guard blocks, so the
creation makes nothing in either.

Every case pins a value its input does not carry (`uitkering` 250 and 200,
`aantal penningbesluiten` 0), so a no-op run fails each of them and none
needs a survivor entry.

`uchar-case-v0` cannot address a made instance; the cases read it through
its jutter. In case 3 the input besluit B1 has a label, so its derived
`besluitjaar` is pinned directly.

## Executors

ref and clingo replay the three cases with the pinned values. The Drools
backend plays all three in both branches, exec-model and DRL text,
`PLAYED 3` each (2026-10-11). The DRL-text branch already did at this
golden's admission. The exec-model branch then refused the document with
`transpiler SKIP: objectcreatie: stel het penningbesluit vast — een
objectcreatie zonder connectingRole-binding, waarvan de terugkoppeling uit
het typenpaar volgt: deze tak draagt deze vorm nog niet`, and its manifest
lowered the other five forms and refused nothing else. The card
objectcreatie-zonder-connectingrole-binding, which adds this golden, carries
the form in that branch: it takes the back-link the shared decision derives
from the type pair, as the DRL-text branch does.
