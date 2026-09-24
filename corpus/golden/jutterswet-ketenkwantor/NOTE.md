# jutterswet-ketenkwantor

A COUNTING quantifier over a multi-step range: the quantifier fold
(`IR-SPEC.md` ch. 4.5, WF-108 (c)) with a two-step `bronPad`, read as the
FLAT UNION of `RUNTIME-SEMANTICS.md` ch. 3.4b (ch. 3.9a shares the range of
ch. 3.4a/3.4b). A Jutter voert Boten, a Boot bergt Vondsten, and one Vondst
may be geborgen by more than one Boot (the `berging` relation is
many-to-many, which is what lets an element be shared).

```
Een Jutter is vondstrijk indien het gewicht van ten minste 4 van de
geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00.
Een Jutter is vondstarm indien het gewicht van ten hoogste 3 van de
geborgen vondsten van alle gevoerde boten van de Jutter groter is dan 1,00.
```

Both rules read ONE count, from either side: `vondstarm` is the
complement of `vondstrijk` on every cell, so each case pins the count
twice. The second rule is also what makes every case refute a
do-nothing executor (gate 7): a case whose only verdict is an absent
kenmerk is satisfied by the raw input.

Under `AANTAL` a count over this range is ill-formed: a count's range is one
step, spelled `sourceRole` (WF-102 (c)), and the verifier, ref and the ASP
backend refuse it in one sentence (`corpus/negatief/wf102-bronpad-op-niet-setbron`).
Until L2 WF-096 fenced the same node (`wf096-aantal-setbron-bronpad`, removed
with WF-096 at L2). The quantifier fold is a different node
under its own rule, and this document is the witness that it counts over a
multi-step range, flat.

## Producer route, measured 2026-09-22

No `bron.rgs`. Each sentence above, fed through the RegelSpraak frontend
(`make -C frontends/regelspraak ir FILE=…`), is refused with the error
"setlezing buiten een vouwbron", which cited WF-096 until L2 and cites WF-053
since (measured again 2026-09-24, on both sentences: `… hier wordt één waarde
verwacht, en een meervoudige stap in een lezing vraagt een aggregatie (WF-053,
IR-SPEC ch. 4.1a)`): the frontend's quantifier lowering
declines a written range that resolved to a multi-step path rather than
bind its first step (the `bereikPad` guard in `RuleChecker.kwantorvouw`),
and the same holds with "van de gevoerde boten" instead of "van alle".
The document is therefore hand-written: the one-step sentences "… van de
geborgen vondsten van de Boot …" were emitted by the frontend and their
`Kwantorvouw`s carried over with the subject moved to the Jutter and
`bronRol` replaced by the two-step `bronPad` (bemanning/gevoerde boot,
berging/geborgen vondst).

## The cells, computed by hand before any executor ran

Every Vondst weighs more than 1,00 unless stated, so the matrix holds of
every reached element except where the table says otherwise. `distinct` is
the number of elements in the flat union that satisfy the matrix — the
pinned reading. The table sets it against the two alternative readings,
in the terms of `IR-SPEC.md` ch. 4.3's COUNT bullet: `voorkomens` counts
join tuples (boot, vondst), each element once per intermediate that
reaches it; `tussen` is the nested reading's count of counts, which
counts INTERMEDIATE INSTANCES (here the boten whose inner count is
non-empty). A fourth reading, the RANGE-FREE one that ignores `bronPad`
and counts every Vondst in the world that satisfies the matrix, is
`populatie`; case 4 exists for it.

| case | subject: boten → vondsten | flat union | distinct | voorkomens | tussen | populatie | vondstrijk (≥ 4) | vondstarm (≤ 3) |
|---|---|---|---|---|---|---|---|---|
| `case1-gedeelde-vondst-onder-de-drempel` | J1: B1→{V1,V2}, B2→{V2,V3} | {V1,V2,V3} | 3 | 4 | 2 | 3 | **afwezig** | **aanwezig** |
| `case2-gedeelde-vondst-op-de-drempel` | J1: B1→{V1,V2}, B2→{V2,V3,V4} | {V1,V2,V3,V4} | 4 | 5 | 2 | 4 | **aanwezig** | **afwezig** |
| `case3-toets-beslist-mee` | J1: B1→{V1,V2}, B2→{V2,V3,V4}; V4 weighs 0,50 | {V1,V2,V3,V4} | 3 | 4 | 2 | 3 | **afwezig** | **aanwezig** |
| `case4-bereik-en-tweede-jutter` | J1: B1→{V1,V2}, B2→{V2,V3} | {V1,V2,V3} | 3 | 4 | 2 | 7 | **afwezig** | **aanwezig** |
| (same case) | J2: B3→{V2,V4,V5,V6} | {V2,V4,V5,V6} | 4 | 4 | 1 | 7 | **aanwezig** | **afwezig** |

Case 1 is the separating cell: V2 is reached along B1 and along B2 and
counts once, so the count is 3: the threshold of 4 is not met and the
ceiling of 3 is. An executor that counted occurrences would answer 4 and
turn BOTH verdicts over.

Case 2 is the other side of the threshold with the same shared element:
the flat count reaches exactly 4. Case 3 keeps case 2's graph and makes
the matrix fail on V4 — the count is over the elements that SATISFY the
test, so it drops to 3 (occurrences would still reach 4, again turning both
verdicts over).

Case 2 is also where the INTERMEDIATE-INSTANCE reading is ruled out: J1
has two boten, so a count of counts gives 2 and would leave `vondstrijk`
absent and set `vondstarm`. In cases 1 and 3 that reading agrees with the
flat one by accident (2 and 3 sit on the same side of both rules); case 2 is
the cell that separates it, as cases 1 and 3 separate the occurrence
count.

Case 4 pins the RANGE and the per-subject separation. It carries case 1's
chain for J1 unchanged, a second Jutter J2 whose one boot B3 bergt V2 (the
element J1 reaches as well), V4, V5 and V6, and a Vondst V8 of 9,00 that no
boot bergt. Per subject the flat union gives J1 3 and J2 4, so the two
jutters land on opposite sides of both rules in one world — so ANY count
that does not depend on the subject fails this case, whatever it counts.
A range-free
count sees all seven Vondsten (V1–V6, V8) for both and turns J1's two
verdicts over; a count that leaks one subject's range into the other's
gives J1 {V1,…,V6} = 6 and turns them over as well. V2 being shared across
the two jutters is what makes the separation a claim about the RANGE and
not about disjoint data. Cases 1–3 cannot pin this: in them every Vondst
is reached by J1 and every boot is J1's, so a range-free count gives the
same number there.
