Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **object creation** (ch. 5.5)
whose `subject` — the object type the creation runs over — names a type *no
declaration carries*.

    WF-034 FAIL /rules/objectCreaties/1/subject: object creation's subject type
    'Bestaatniet' is not declared: the creation resolves against no
    declaration

**What the field is.** `subject` names the population the creation iterates:
one new instance per subject instance the condition admits, and every member
value in `toekenningen` evaluates **on the subject**, never on the created
type (ch. 5.5). It is a reference like any other, spelled in one field.

**What crossed it before: nothing that is about it.** WF-027 crosses the
CREATED type (`nieuwType`) and stops there. The subject was reached only by
rules that read *through* it — WF-053 when a member value navigates, WF-047
when an explicit `verbindendeRol` names the back-link — and those are
incidental cover, not coverage: they stand down the moment a creation has
neither.

**Measured**, `subject` pointed at an undeclared name on each of the 11 golden
object creations in turn:

    10 of 11   already refused — all ten by WF-053 (the navigation inside a
               member value loses its scope), nine of those by WF-047 as well
     1 of 11   verifier-CLEAN — the only creation with no `toekenningen` at
               all: `anchor/jutterswet-penninguitgifte`, creation 1

That one is this document. And there the executors do not fault either — they
**agree on a wrong answer**:

    ref    : MISMATCH: J1.aantal dossiers = 0, expected 1
    clingo : MISMATCH: J1.aantal dossiers = [0], expected 1

The creation runs over an empty population, no `Dossier` is made, and both
executors report the same 0 where 1 is pinned. The bean-model executor has no
say here: it skips this golden for an unrelated reason (a directed count over
a role pair the v0 bean model cannot express).

So this is a row where the differential square is **blind**: backend agreement
proves nothing, and only the hand-pinned expectation of an **anchor** golden
stands between the corpus and the defect. That is precisely the situation the
anchor subset exists for, and precisely the situation a well-formedness rule
is supposed to remove.

**What this arm does not close**, measured the same day and stated so the
silence is not mistaken for coverage: `subject: null` on the same document is
equally verifier-clean, and there both executors are loud (`ref` "missing
ObjectCreatie.subject in IR node", clingo "missing ObjectCreatie.subject").
That is the empty-reference class of the earlier waves — a different question
from resolution — and it has no witness yet.

*Measured breadth:* zero documents lose anything. Measured at the pre-card
state, all 28 object creations in golden, rood, negatief and the parity zone
named a declared subject; of the 30 those zones hold since, 29 do and the
thirtieth is this document. The four-zone sweep moves no other verdict and no
other report.

A hand-broken variant of `corpus/golden/anchor/jutterswet-penninguitgifte/ir.json`
— one field repointed, a one-line diff; schema-valid, since
`ResolvedObjectCreatie.subject` is `["string","null"]` on the v0 wire.

Falsifiable, clause-exact: make `wf034_objectcreatie_subject`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones,
turns clean — 503 documents compared, one verdict moved, no report elsewhere.
