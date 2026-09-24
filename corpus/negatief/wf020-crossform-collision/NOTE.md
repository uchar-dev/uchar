Violates WF-020 (`spec/IR-SPEC.md` ch. 3.1): one name used by a rule and by
an allocation (unique across all NAMED forms — widened 2026-09-12 from the
ch. 3 rule forms alone to the ch. 6.1 defaults groups as well; this case is
the arm between two ch. 3 forms).

    WF-020 FAIL /rules/rules/0: name 'Kenmerktoekenning jutter
    penninggerechtigd' collides with 'Kenmerktoekenning jutter
    penninggerechtigd' in allocations (unique across all named forms)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** The allocation
carried no source type and no recipient role. Since L2 the recipient role is
required (B12) and the migration derives it from the pair of source and
target type, so it refused a document without one. Written by hand: the
model gains the fact type `strandgoedverdeling` (a `verdeler` and an
`ontvanger`, both `Jutter`), the allocation names `Jutter` as its source
type and binds the `ontvanger` role. Nothing of the name collision moved;
the WF-034 lines beside it are the allocation's other empty fields, as
before.

Since L4 the colliding name is the `name` of the form (delta B19), written
once for all its versions, and the rule compares forms: a report on a named
form cites the form. `expect.txt` pins the arm.
