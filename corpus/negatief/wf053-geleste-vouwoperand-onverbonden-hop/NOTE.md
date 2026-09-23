Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) in the arm for an unbound read
(D10): the second operand of a listed `MAXIMUM` reads `bakenloon` on `Baken`
from the subject `Vondst` and carries no navigation bindings. A read without
bindings is the subject's own read, so its `objecttype` is the subject type;
`Baken` names a type only a navigation reaches, and no fact type relates
`Vondst` to `Baken` at all.

    WF-053 FAIL /rules/resolvedRegels/0/waarde/argumenten/1: an attribute
    reference without navigation bindings is the subject's own read, so its
    objecttype is the subject type 'Vondst' — 'Baken' names another type,
    which only a navigation reaches

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the read
was spelled without a written hop and the verifier derived its route from
the type pair, finding none ("bindingless navigation: no fact type relates
'Vondst' to 'Baken'"). Since L2 a navigation carries its bindings and
nothing derives them (B11, D10); the migration refused the document for the
same missing relation. Written by hand: the read keeps its foreign
`objecttype` and carries no binding, which is the D10 arm. The position — a
listed value fold's operand, an ordinary read — is unchanged. `expect.txt`
pins the arm (U1).
