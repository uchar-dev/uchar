Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on the one-sided arm of its
domain clause within ONE family: the attribute `aanspraken` of `Jutter` is
declared `list of beschikking` (a sum type), and the rule writes a
`ListLiteral` typed `list of number` (`10.00`, `25.00`). The family clause
is silent (list = list); only the target names a keuzetype domain.

    WF-023 FAIL /rules/rules/0/versions/0: a list value naming no
    keuzetype domain is assigned to a target of the sum-type domain
    'beschikking'

Why it has to exist: the family check stops at the list wrapper, so a
number list enters a sum-typed list attribute verifier-clean without this
arm; ref wrote it and faulted `match scrutinee is 10, not a constructor
value` in the faithful reader's lambda; clingo faulted the translation
(`unscalable datatype`, the list-write fence). Across families (a number
into an enumeration attribute) the family message already names the
defect, so the one-sided arms are restricted to `same_family`.

Falsifiable: make the two one-sided arms of `wf023_assignment` (`Some a,
None when same_family` and `None, Some b when same_family`) return `[]`
and this case passes together with `wf023-schrijf-lijst-geen-domein-doel`.
