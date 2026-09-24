# wf046-kwantorvouw-bereik-ander-type

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3), through the RANGE of an ALLE quantifier fold
whose recursive creation mints an instance of ANOTHER type than its subject.

The recursive creation "richt een post in" (group "estafette") mints a Post
for a Wachter while "de rang van de Wachter is kleiner dan 5 en alle posten
van de Wachter hebben een nummer groter dan 5". The fold's range is bound:
`bronRol` `bemanning`/`post`, the relation that connects Wachter to Post. The
creation writes that relation through its back-link and gives the new Post
nummer 1. The Wachter without posten satisfies the ALLE fold vacuously, and
the Post it mints falsifies it.

    WF-046 FAIL /rules/objectCreaties/0/versions/0: negative dependency edge on
    a cycle: richt een post in reads what richt een post in produces while richt
    een post in is reachable from it (no strata exist)

Under ALLE the matrix is monotone and reads positively, so the refusal comes
from the range: the fold reads the relation the creation writes, and more
posten can only falsify it. `expect.txt` pins this arm.

What sets it apart. Every other WF-046 quantifier-fold witness
(`wf046-kwantorvouw-alle-bereik` and its siblings) mints a successor of the
subject's own type, a Wachter for a Wachter. This one mints a Post, so the
range edge runs through a relation between two different types.

History. Until L2 this was `wf046-kwantorvouw-bindingloos-bereik`: the fold
named no role, and its range was the one connecting relation WF-048 derived
from the type pair (Wachter, Post). Until 2026-09-23 the dependency walk drew
no range edge at all for such a bindingless fold, and the document was
verifier-clean; measured then, with one input Wachter W1 of rang 1, the
reference interpreter minted one Post with nummer 1, justified by a vacuous
ALLE that this Post falsifies, and the ASP backend found no stable model.

L2 (B12) makes the range required on every quantifier fold: the schema and
every loader refuse a quantifier fold without `bronRol` or `bronPad`. The L2
union first removed the witness as a duplicate of `wf046-kwantorvouw-alle-bereik`.
That did not meet the removal criterion of the L2 stamp list (the migration
refuses the document, or its translation is verifier-clean): measured in the
L2 fix round (2026-09-24), `spec/proposals/v1-migratie/m2.py` on the L1 form
of the document derives the range `bemanning`/`post` (one quantifier binding,
two reference types removed), and the verifier rejects the translation with
the message above. The document here is that translation, byte for byte;
stamp: herschreven (m), under a new name because the range is no longer
bindingless.

Minimal own-authored document on the shape of
`corpus/golden/anchor/jutterswet-wachtersketen`, with a second type Post.
