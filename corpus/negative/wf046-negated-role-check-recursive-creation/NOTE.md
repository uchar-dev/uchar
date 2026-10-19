# wf046-negated-role-check-recursive-creation

Violates **WF-046** (`spec/IR-SPEC.md` ch. 5.2 and 5.3,
`RUNTIME-SEMANTICS.md` ch. 6.1 and 6.3): a recursive creation guarded by a
negated role check on the relation it creates itself.

    Regelgroep estafette is recursief
    Regel werf de opvolger
        geldig altijd
            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
            indien hij aan alle volgende voorwaarden voldoet:
            - hij is geen voorganger
            - zijn rang is kleiner dan 3.

"hij is geen voorganger" is a `RoleMembership` on `opvolging` with
`negated: true`. The creation writes `opvolging` (its back-link, the
`connectingRole` opvolger), and the role check reads it. Since 2026-10-06 a
role check reads the link set of the relation it names, and negated it is a
negative edge: it holds on absence, and the link the creation lays makes it
false. So the creation reads negatively what it writes itself, on a cycle
that WF-045 admits because it passes through a recursive creation:

    WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge
    on a cycle: werf de opvolger reads what werf de opvolger produces while
    werf de opvolger is reachable from it (no strata exist)

`expect.txt` pins the arm of that line.

Measured on 2026-10-06 with one Wachter W1 of rang 1. Before the reading the
role check read nothing, the relation was input to the analysis, and the
verifier said `verifier: PASS`. The reference interpreter answered the case
without a fault, in the order of its pass; the ASP backend found no stable
model (`clingo: UNSATISFIABLE — a fail-fast guard fired: no viol/2 witness
(the input is not stratified?)`): W1 gets a successor only while it is no
voorganger, and the successor makes it one. Both Drools branches play the
case. With the reading, the reference interpreter refuses to evaluate under
WF-046.

The RegelSpraak frontend writes this document: its cycle check repeats
WF-046 only on a cycle that WF-045 refuses.

Falsifiable in one edit: drop "hij is geen voorganger" and the document is
the clean shape of `corpus/golden/anchor/jutterswet-wachtersketen`.
