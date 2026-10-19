Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Unresolved` marker whose
source snippet is blank — a single space — beside a second marker that the
document's one `warning` accounts for.

    WF-113 FAIL /rules/rules/0/versions/0/condition: Unresolved marker whose
    source snippet is blank: blank counts as absent, since nearly every message
    contains it and a message about another marker would account for this one

The document of `wf113-marker-source-null`, with `"source": {"snippet": " "}` on
the left marker where that document has no `source`. The containment test of
(b) is literal, and a blank snippet is contained in nearly every message, so
without this clause the `warning` about `'de verjaringsgrens'` would account
for this marker as well and the document would pass. Blank counts as absent,
as it does for a named form's name (WF-107): the verifier tests for a snippet
of only whitespace, and this witness pins that test.

**Rewritten by hand at L5 (2026-09-25), stamp: herschreven (i).** Until L5 the
marker carried `"bron": ""`, the empty string, and pinned the empty case of
the test. The source record (delta B26) has no empty snippet: the schema gives
`snippet` a minimum length of 1, and every reader, the verifier included,
refuses an empty snippet at load ("a source snippet is never empty: a node
without source text carries no source"), so the migration refused the
document. Written by hand with a snippet of one space, the witness keeps the
blank arm's defect; `expect.txt` pins the arm.
