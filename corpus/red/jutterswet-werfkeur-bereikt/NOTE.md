# jutterswet-werfkeur-bereikt — the member order of a to-many role reaches a fault

The document of the golden `corpus/golden/jutterswet-werfkeur` (see its NOTE for the rules), with a case in which the
member order of `RUNTIME-SEMANTICS.md` ch. 5.7a makes the quantifier fold reach a member that faults.

W1 is not vlaggerechtigd, so it gets no werfvlag and no kotter; it gets a sloep (lengte 2). The case writes no link on
W1's side: S1 (lengte 8), S3 (6) and S4 (7) name W1 as their werf, and S2 (3) names W1 as its bouwwerf. No evaluation
date.

## Computed by hand

The members of W1's schuiten: the loader materialises S1, S3, S4; the fact creation links S2; the sloep follows. In
instance order: S1, S2, S3, S4, sloep.

| member | lengte > 5 | keurdatum after the rekendatum | verdict |
|---|---|---|---|
| S1 | 8, true | not read | true (1 of 3) |
| S2 | 3, false | keurdatum leeg is read, then the rekendatum: no evaluation date | **fault EF-011** |

The rule `keur van een werf` fails observably on W1 with EF-011 (`params` empty: the evaluation date is the context
value, ch. 5.7b). Read in the order the links were laid — S1, S3, S4 at load, S2 by the fact creation — the fold would
count S1, S3 and S4 and stop with keur 1, without a fault.

## Executors

`ref/` faults EF-011. The ASP backend refuses with CLINGO-035 (gate 8's divergence list) and the Drools backend with
DROOLS-259 (gate 8's exclusion list): neither carries the member order of a relation a creation writes.
