# wf034-neerwaartse-lezing

Violates the chain-aware **WF-034** (`spec/IR-SPEC.md` ch. 2.2/4.6 (p)):
`bergingsdiepte` is declared on the strict subtype `Wrakrecht` and read
through a `Strandrecht`-typed role.

    WF-034 FAIL /resolvedRegels/0/waarde/argumenten/0: 'bergingsdiepte' is
    not a declared attribute of 'Strandrecht': outside the v0 instance-fold
    source (the unknown-typed row-1 wart) a fold-source argument is an
    ordinary attribute reference — the ch. 4.3 role-name exception and its
    type-pair fallback do not reach here

The rule's text is unchanged; what subtyping adds is the reachability. Member
resolution walks the chain UPWARD ONLY, because not every instance of the
supertype has a member its subtype declares — here the fold ranges over every
`Strandrecht` the `Kustgemeente` registers, and a plain one has no
`bergingsdiepte` at all. The role's declared filler type is an upper bound,
so reading through it resolves upward and never downward.
