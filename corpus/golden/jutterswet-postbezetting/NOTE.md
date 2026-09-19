# jutterswet-postbezetting — the far-side role check gets its witness

*Jutterswet, artikel 12 (bezetting van de wachtposten).* Een wachtpost is
bemand indien hij een wachtjutter heeft. Een wachtpost is onbemand indien
hij geen wachtjutter heeft.

The `VerreRollidmaatschap` node's first carrier in any corpus zone. Until
2026-09-20 the form had **zero** documents across golden, rood, negatief and
the parity zone, which is why `IR-SPEC.md` ch. 4.5 recorded its two fields as
uncrossed: a clause over them would have had no witness. This document is that
witness, hand-written, with its expectations computed by hand before any
executor saw it.

**What the form is.** The same §8.1.7 sentence as the near-side rolcheck
(`jutterswet-wachtindeling`, the same feittype and the same two roles), read
from the other end. The subject is the **Wachtpost**, which does not fill
`wachtjutter`; it is the relation's other end. So the check is not a null-test
on a reference the subject holds but an **existence** question over the
relation: does any Jutter stand in `wachtindeling` to this post. The node
carries no written role name at all — `filler` names the type at the far end
(`Jutter`) and `backref` the role the subject itself fills
(`toegewezen wachtpost`) — and the two must meet on one declared fact type
(WF-034 (t), ch. 4.6). `RUNTIME-SEMANTICS.md` ch. 5.8a's far half carries the
semantics and this document's worked example.

**Hand-computed expectations, written before the first run.**

| case | input | W1 | W2 |
|---|---|---|---|
| `case1-bezette-post` | J1 holds `toegewezen wachtpost = W1`; W2 empty | bemand, not onbemand | not bemand, onbemand |
| `case2-dubbele-wacht` | W1 holds `wachtjutters = [J1, J2]`; J3 has no post; W2 empty | bemand, not onbemand | not bemand, onbemand |

The two cases carry the same verdicts on purpose, for two different reasons.

*Case 1 spells the relation from the JUTTER's side*, so W1's verdict hangs on
a leg no case line names for W1 at all — the leg `RUNTIME-SEMANTICS.md`
ch. 5.7a has the loader materialise. It is the ch. 5.7a law applied to this
node: how a case spells a relation does not change what the model computes
from it.

*Case 2 spells it from the POST's side* and adds two separations. Two fillers
on one post read exactly as one does: the check is existence, never a count
and never a pick. And J3, a Jutter in no relation at all, leaves W2 onbemand —
a filler that exists is not a filler that is linked.

**All four executors, all four verdicts.** `ref` and clingo both PASS 4
expectations per case, and both Drools branches — the exec-model one and the
DRL-text one — play both cases against these same pins. So the far form
closes the differential square (ref ≡ clingo ≡ Drools) on the values above,
anchored to hand-written numbers rather than to parity.

*Why it has no `bron.rgs`.* The witness is a wire document, and it needs no
producer route to be one: the form's producer half is a frontend question and
this card is about what the IR means and what its readers do with it. The
directory therefore carries no source, and gates 9, 10 and 14 skip it by
construction.

*Its twin in the negatief zone* is
`corpus/negatief/wf034-verre-rolcheck-verkeerde-zijde`: this document with
`backref` repointed at `wachtjutter`, the filler's own role instead of the
subject's — one field, one edit, and the crossing fails while both names still
resolve.
