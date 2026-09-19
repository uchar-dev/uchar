Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (t)): a **far-side role check**
(ch. 5.8a read from the other end) whose `backref` names a real role on the
*wrong side* — the role the FILLER fills, not the one the subject fills — so
the two fields resolve against no single declaration.

    WF-034 FAIL /resolvedRegels/0/voorwaarde/backref: no declared fact type
    holds the subject 'Wachtpost' in a role named 'wachtjutter' with 'Jutter'
    at the other end: the far-side role check's filler and back-reference
    resolve against no single declaration

**Why the rule is a crossing and not two lookups.** A `Rollidmaatschap` spells
the near-side check with a `rol` field beside `anderTeFiller`, and (o) can say
that the first is presentation and the second is the whole reference. A
`VerreRollidmaatschap` has **no written role name at all**: `filler` and
`backref` are the only two fields it carries, and each of them resolves here.
`Jutter` is a declared object type. `wachtjutter` is a declared role of the
declared fact type `wachtindeling`, which does relate `Wachtpost` to `Jutter`.
What fails is the *pair*: the check asks for a fact type that holds the
**subject** in the role `backref` names, with `filler` at the other end, and
`wachtjutter` is filled by the Jutter, not by the Wachtpost. The exists-join
that reads this node therefore ranges over an empty relation while every
individual name looks up fine. Swapping `backref` to `toegewezen wachtpost`
turns the document into `corpus/golden/jutterswet-postbezetting`.

**The cross.** One, against the role fillers, mirroring (o): the
(subject role, far role) pairs on one declared fact type
(`far_role_candidates`). An **empty** list is this arm's report. Several
entries stay WF-048's ambiguity question; a subject the expression walk does
not know keeps the arm silent. The two absent spellings — no `filler`, no
`backref` — fold into the same arm on the ch. 4.6 (s) precedent: with either
gone the node carries no reference whatever, each is refused by its own
message, and each is one edit from this document.

**Arity is deliberately uncrossed.** A producer writes the far form where the
named role is to-many from the subject, and a bean-model executor needs the
`backref` role to be to-one because it stores one reference field per to-one
role. Neither property is crossed here: `RUNTIME-SEMANTICS.md` ch. 5.7a makes
a relation symmetric, so neither OCaml executor reads a direction, and a MUST
on arity would refuse documents two of the four executors execute correctly.
The bean-model executor refuses what it cannot store per form and with its own
reason, which is measured on this very document (below).

**Measured 2026-09-20**, before the arm existed, on this document:

    verifier            PASS  — the hole this case closes
    ref                 EVALUATION FAULT: condition kind not implemented in
                        this card
    clingo              TRANSLATION FAULT: condition kind not translated (no
                        corpus case exercises it)
    Drools exec-model   EXCLUDE transpiler SKIP: verre rollidmaatschapscheck:
                        het beanmodel leidt voor de rol 'wachtjutter' op
                        Jutter geen referentieveld naar Wachtpost af
    Drools DRL-tekst    EXCLUDE drl-transpiler MARKER: the same sentence

Four executors, four treatments, and only one of them names the defect. The
two OCaml sentences are **bottom sentences** — both stand verbatim in the
forbidden list of `scripts/check-rood.sh` — so they were never a refusal of
*this* form at all; they were the residual arm over condition kinds reporting
its own position. That is why this case could not have been a red one, and
why the same card gave both executors a named arm for the form.

*Measured breadth:* zero documents lose anything. Over golden, rood, negatief
and the parity zone — **690** documents at the branch tip on 2026-09-20
(217 golden + 34 rood + 409 negatief + 30 parity) — **zero** carried a
`VerreRollidmaatschap` before this card and **two** carry one after it, both
the card's own: this document, with two of them, and
`corpus/golden/jutterswet-postbezetting`, with four. So every other document
in every zone is a non-over-rejection witness by construction, and the
before/after sweep moves exactly one verdict and one report line: this
document's. Nothing else changes.

A hand-written document in the `jutterswet-wachtindeling` world — feittype
`wachtindeling`, the to-many role `wachtjutter` against the to-one role
`toegewezen wachtpost` — with the subject on the post's side; schema-valid,
since both fields are typed `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: neuter the guard of the `Verre_rollidmaatschap` arm of
`walk_cond` (`spec/verifier/lib/checks.ml`) and this case, alone in all four
zones, turns clean — 690 documents compared, one verdict moved, no report
elsewhere.
