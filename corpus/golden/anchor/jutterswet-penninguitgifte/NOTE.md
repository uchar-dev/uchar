# anchor/jutterswet-penninguitgifte — the hand computation

Object-creation execution (WF-027; `RUNTIME-SEMANTICS.md` ch. 5.5). This note
carries the arithmetic behind the three case files, and in particular the
recomputation the **ch. 5.7a case-input decision** (2026-09-10) forced on
case 2.

## The model

| | |
|---|---|
| `penningbezit` | `houder` (Jutter, **to-one**) / `penning` (Penning, plural `penningen`, **to-many**) → **1:N** |
| `dossiervoering` | `dossierhouder` (Jutter, to-one) / `dossier` (Dossier, plural `dossiers`, **to-one**) → **1:1** |
| creation 1 | per penninggerechtigde Jutter a Penning: `bedrag := vondstwaarde`, characteristic `uitgereikt` set; back-link over `penningbezit` |
| creation 2 | per penninggerechtigde Jutter a Dossier; back-link over `dossiervoering`, and **blocked** when the subject already fills the to-one `dossier` role in the input world |
| rules | `aantal penningen` = AANTAL(penningen); `penningtotaal` = SOM(bedrag of all penningen) or 0; `aantal dossiers` = AANTAL(dossiers) |
| consistenties | `penningcontrole`: every Penning is `uitgereikt`. `houderscontrole`: `aantal houder` of the Penning ≥ 1 |

## Case 2 — both creation guards observable, and the ch. 5.7a recomputation

Input: **J3** (vondstwaarde 7, penninggerechtigd) with `penningen: [P1]` and
`dossier: [D1]`; **P1** (bedrag 5, *not* uitgereikt); **P2** (bedrag 3,
uitgereikt); **D1**.

At load, ch. 5.7a mirrors each supplied link onto its **sister role**:

    J3.penningen = [P1]   ->   P1.houder        = [J3]
    J3.dossier   = [D1]   ->   D1.dossierhouder = [J3]

Evaluation:

1. Creation 1 fires on J3 (penninggerechtigd). The created-side role `penning`
   is to-many, so the linked-instance guard does not apply: a Penning is
   minted with `bedrag = 7`, `uitgereikt` set, and `houder = J3`.
2. Creation 2 is **blocked**: the created-side role `dossier` is to-one and J3
   already fills it (D1) in the input world.
3. `aantal penningen` of J3 = |{P1, mint}| = **2** — input and mint mixing in
   one fold.
4. `penningtotaal` of J3 = 5 + 7 = **12**.
5. `aantal dossiers` of J3 = |{D1}| = **1** — the blocked creation, observable.
6. `penningcontrole`: P1 is not uitgereikt → **fires on P1**. P2 and the mint
   are uitgereikt → nothing.
7. `houderscontrole`: P1.houder = [J3] through the **mirrored leg** → 1 ≥ 1,
   nothing. The mint's houder is J3 → nothing. P2 is named by no jutter and
   supplies no role of its own, so its houder count is 0 → **fires on P2**.

End-state channel, exactly: `penningcontrole × P1; houderscontrole × P2` —
two facts for two pins, no third fact riding along unpinned under the
existential match of an `inconsistent` expectation.

**What the decision changed here.** Before ch. 5.7a the case carried no P2 and
pinned `houderscontrole × P1`: P1's houder read *empty* although J3 named P1
among its penningen, and that emptiness was the only reason the rule fired.
The symmetric reading removes it — measured, the one corpus failure the
decision caused:

    MISMATCH: expected inconsistent (a channel fact from rule "houderscontrole"
    on "P1"), but the channel carries: penningcontrole × P1

P2 restores the rule's firing witness on an instance whose houder is genuinely
absent under **either** reading, which makes case 2 decision-INDEPENDENT: it
replays `oracle: PASS (5 expectations)` with the mirror and without it. The
decision itself is pinned by case 3.

## Case 3 — the ch. 5.7a pin

Input: **J1** (vondstwaarde 4, penninggerechtigd) with `penningen: [P1]`;
**P1** (bedrag 6, uitgereikt), no roles of its own.

Mirror: `J1.penningen = [P1]` → `P1.houder = [J1]`.

1. Creation 1 mints a Penning: `bedrag = 4`, uitgereikt, `houder = J1`.
2. Creation 2 is **not** blocked (J1 fills no `dossier` role) → a Dossier is
   minted and linked.
3. `aantal penningen` = |{P1, mint}| = **2**.
4. `penningtotaal` = 6 + 4 = **10**.
5. `aantal dossiers` = **1**.
6. `penningcontrole`: P1 and the mint are both uitgereikt → nothing.
7. `houderscontrole`: P1.houder = [J1] **through the reverse leg the case
   never wrote**; the mint's houder is J1 → nothing.

Channel empty → **`consistent`**. That is the only expectation form in which
the *absence* of a channel fact is assertable, and it is why this case exists:
under the pre-decision directed reading it fails, by name —

    MISMATCH: expected consistent, but the channel carries: houderscontrole × P1

so case 3 is the decision's regression detector.

## Case 1

J1 (vondstwaarde 12, penninggerechtigd) and J2 (99, not entitled), no links.
One mint for J1: 1 penning totalling 12, 1 dossier; J2 reads 0/0/0. The 1 pins
the deterministic mint's idempotence across fixpoint passes. Channel empty →
`consistent`.
