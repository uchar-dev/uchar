# wf034-neerwaartse-lezing

Violates the chain-aware **WF-034** (`spec/IR-SPEC.md` ch. 2.2/4.6 (p)):
`bergingsdiepte` is declared on the strict subtype `Wrakrecht` and read
through a `Strandrecht`-typed role.

    WF-034 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/0: 'bergingsdiepte'
    is not a declared attribute of 'Strandrecht': a value fold's source reads an
    attribute of each contributor

The rule's text is unchanged; what subtyping adds is the reachability. Member
resolution walks the chain UPWARD ONLY, because not every instance of the
supertype has a member its subtype declares — here the fold ranges over every
`Strandrecht` the `Kustgemeente` registers, and a plain one has no
`bergingsdiepte` at all. The role's declared filler type is an upper bound,
so reading through it resolves upward and never downward.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the fold
source lost its own `type` and set reading (B11, B13), and the fold carries
its range; the source still reads, on the supertype `Strandrecht`, an
attribute declared on a subtype. The rule still fires on the same arm, and
`expect.txt` pins it.
