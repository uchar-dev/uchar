# jutterswet-postbezetting — the far-side role check gets its witness

*Jutterswet, artikel 12 (bezetting van de wachtposten).* Een wachtpost is
bemand indien hij een wachtjutter heeft. Een wachtpost is onbemand indien
hij geen wachtjutter heeft. Een wachtpost is strandbemand indien hij een
strandjutter heeft. Een wachtpost is strandvrij indien hij geen
strandjutter heeft.

The first corpus carrier of the far-side role check, the form that reads the
relation from the end the subject does not hold. Until 2026-09-20 the form had
**zero** documents across golden, red, negative and the parity zone, which is
why `IR-SPEC.md` ch. 4.5 recorded it as uncrossed: a clause over it would have
had no witness. This document is that witness, hand-written, with its
expectations computed by hand before any executor saw it. Since L2 (2026-09-24)
both directions of the role check are one node, `RoleMembership {factType,
role, counterpartType?, negated}` (`spec/proposals/V1-WIREMIGRATIE.md` B9,
`spec/CONSTRUCTS.md` row 5; `counterpartType` in IR-SPEC ch. 4.5).

**What the form is.** The same §8.1.7 sentence as the near-side rolcheck
(`jutterswet-wachtindeling`, the same feittype and the same two roles), read
from the other end. The subject is the **Wachtpost**, which does not fill
`wachtjutter`; it is the relation's other end. So the check is not a null-test
on a reference the subject holds but an **existence** question over the
relation: does any object of the far end's class stand in `wachtindeling` to
this post. The node names the fact type, `factType: "wachtindeling"`, and the
role the SUBJECT fills, `role: "toegewezen wachtpost"`, by its declared name;
WF-034's role arm crosses the pair and the subject's filling (ch. 4.6 (o)).
`RUNTIME-SEMANTICS.md` ch. 5.8a's far half carries the semantics and this
document's worked example.

**Why a Strandjutter.** The class of the far end narrows what the relation
reached (ch. 5.8a rule 2 — the class test is extensional, `T ⊑` the class).
Where that class IS the other role's declared type the narrowing is empty, so
the first version of this witness — a class `Jutter` against a role declared
at Jutter — could not tell the two readings apart, and both OCaml executors
were measured answering the role's whole population while both Drools branches
filtered on the class. The document therefore declares `Strandjutter` under
`Jutter` and carries the same check twice over. Rules 1 and 2 ask the
role-wide question and write no `counterpartType`: the far end is the declared
filler of `wachtjutter`, `Jutter`. Rules 3 and 4 ask the narrowed one and write
`counterpartType: "Strandjutter"` (IR-SPEC ch. 4.5), over one and the same relation; that
field is what makes case 3 class-distinguishing.

**Hand-computed expectations, written before the first run.**

| case | input | W1 | W2 |
|---|---|---|---|
| `case1-bezette-post` | J1, a plain Jutter, holds `toegewezen wachtpost = W1`; W2 empty | bemand, not onbemand, **not strandbemand**, strandvrij | not bemand, onbemand, not strandbemand, strandvrij |
| `case2-dubbele-wacht` | W1 holds `wachtjutters = [J1, J2]` with J2 a Strandjutter; J3 has no post; W2 empty | bemand, not onbemand, strandbemand, not strandvrij | not bemand, onbemand, not strandbemand, strandvrij |
| `case3-strandwacht` | J1 (Jutter) holds W1, S1 (Strandjutter) holds W2 | bemand, not onbemand, **not strandbemand**, strandvrij | bemand, not onbemand, **strandbemand**, not strandvrij |

Each case carries something the others do not.

*Case 1 spells the relation from the JUTTER's side*, so W1's verdict hangs on
a leg no case line names for W1 at all — the leg `RUNTIME-SEMANTICS.md`
ch. 5.7a has the loader materialise. It is the ch. 5.7a law applied to this
node: how a case spells a relation does not change what the model computes
from it. And its *strandvrij* is the class test on its own: the relation is
there, the class is not.

*Case 2 spells it from the POST's side* and adds two separations. Two fillers
on one post read exactly as one does: the check is existence, never a count
and never a pick. And J3, a Jutter in no relation at all, leaves W2 onbemand —
a filler that exists is not a filler that is linked. Its Strandjutter also
pins the *upward* half of the class test: a subtype instance satisfies the
supertype filler.

*Case 3 is the discriminator.* Two posts, one filler each, differing only in
CLASS. `bemand` holds of both and `strandbemand` of exactly one, so the
role-wide reading and the class reading come apart inside one world. A reader
that ignores the far end's class answers `bemand` right everywhere and case
3's W1 wrong — which is exactly the state `ref` and the ASP backend were
measured in on 2026-09-20, before this card's fix round, and the state the
first L2 form of the node would have fixed in the wire, had it not been given
`counterpartType`.

**All four executors, all eight verdicts per case.** `ref` and clingo both
PASS 8 expectations per case, and both Drools branches — the exec-model one
and the DRL-text one — play all three cases against these same pins. So the
far form closes the differential square (ref ≡ clingo ≡ Drools) on the values
above, anchored to hand-written numbers rather than to parity.

*Why it has no `source.rgs`.* The witness is a wire document, and it needs no
producer route to be one: the form's producer half is a frontend question and
this card is about what the IR means and what its readers do with it. The
directory therefore carries no source, and gates 9, 10 and 14 skip it by
construction.

*Its twins in the negative zone* are
`corpus/negative/wf034-far-role-check-wrong-side`: this document's first
rule with `role` repointed at `wachtjutter`, the other end's role instead of
the subject's — one field, one edit, and the crossing fails while both names
still resolve; and `corpus/negative/wf034-role-membership-foreign-filler-class`,
whose `counterpartType` names `Wachtpost`, a class outside the other role's
filler chain.

*History until L2 (2026-09-24).* The far-side check had a node of its own,
which named the far end's class and the subject's role in two fields that
had to meet on one declared fact type (WF-034 (t)). L2 merged it with the
near-side check into `RoleMembership`; the migration derived `factType` and
`role` from exactly that meeting, and `counterpartType` (IR-SPEC ch. 4.5) carries the
class where it narrows the declared filler.
