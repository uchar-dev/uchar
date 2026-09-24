Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (n), its empty half): an **object
creation** (ch. 5.5) with **no** `subject` at all.

    WF-034 FAIL /rules/objectCreations/1/versions/0/subject: object creation
    without a subject: the population it runs over, and the type its member
    values evaluate on, resolve against no declaration

**The other spelling of the same defect.** The fifth wave closed the FILLED
half — a subject naming a type no declaration carries — and measured this one
beside it, deliberately leaving it without a witness
(`wf034-objectcreatie-onbekend-subject` says so in as many words). This is
that witness. The two are separate arms because they are separate questions:
a name that resolves against nothing has a declaration table to be missing
from; an ABSENT name has nothing to look up at all.

**And they are observed differently**, which is why the split is not
bookkeeping. Pointed at an undeclared name on this very creation, both OCaml
executors agree on a WRONG ANSWER (0 dossiers where the anchor pins 1 — the
creation runs over an empty population and no object is made). Emptied
instead, both are LOUD, on both cases of the golden:

    ref     EVALUATION FAULT: missing ObjectCreatie.subject in IR node
    clingo  TRANSLATION FAULT: missing ObjectCreatie.subject

So the filled half is the dangerous one and this one is the tidy one — but the
verifier had nothing to say about either, and a document is not well-formed
because an engine happens to shout at it.

**Measured.** `subject` emptied on each of the **11** golden object creations
in turn: 11 of 11 stayed verifier-clean under the verifier of the day before
this card, and none does now.

*Measured breadth:* zero documents lose anything. Of the 30 object creations
the four zones held before this card, every one names a subject; the only
creation without one anywhere is this witness. The four-zone sweep moves no
other verdict and no other report.

A hand-broken variant of `corpus/golden/anchor/jutterswet-penninguitgifte/ir.json`
— one field emptied, a one-line diff; schema-valid, since the version's
`subject` is `["string","null"]` on the wire. It is deliberately the SAME
creation the filled-name witness breaks: the one with no `initialisations`,
the only one the fifth wave's measurement found unfenced, so the two
witnesses are the two spellings of one position.

Falsifiable, arm-exact: delete the `| None ->` branch of
`wf034_objectcreatie_subject` (`spec/verifier/lib/checks.ml`) and this case,
alone in all four zones, turns clean — 522 documents compared, one verdict
moved, no report elsewhere.

Since L4 an object creation is an `ObjectCreation` with its versions (delta
B19); the defect sits on the `ObjectCreationVersion`, whose fields are
`createdType`, `subject`, `initialisations` (each a `MemberInit`) and
`connectingRole`. `expect.txt` pins the arm.
