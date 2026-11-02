# wf023-decimal-nonterminating

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by the literal 3,0000, written into a target of 4 decimals.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

At run time the executors fault on this write with EF-006, the evaluation fault that stands behind the gate (ch. 2.4 point 1); what follows describes the document and that runtime fault.

The pinned Catala-decimal scale is S = 4 (RUNTIME-SEMANTICS ch. 2.9,
Phase-5c vraag 4-A): a decimal-typed target holds exactly four decimals.
Here `quotient` (4 dec) is fed `1,0000 / 3,0000` = 1/3, which does **not**
terminate at four decimals. Both executors must fault loudly and
consistently — the differential holds where it runs:

- **ref** stays exact-rational: `dividedBy` yields 1/3 and the
  bounded-write gate (ch. 2.4) refuses the write —
  *"...does not fit the declared 4 decimals — an explicit rounding is
  mandatory (ch. 2.4)"*.
- **clingo** scales by the declared decimals: the division encoding
  derives the `viol/2` witness *"non-exact division (an explicit rounding
  is mandated before precision is lost)"*.

The case deliberately uses **no `Let`** — a bare `Binary` `dividedBy`
suffices — so it exercises only nodes ref and clingo already carry. The
`expectedFault` fragments are pinned from the runners' current source
text (this card's opdracht 2 lands the corpus only); the executor cards
(opdracht 3 for ref, opdracht 4 for clingo) run both runners and confirm
the fragments live, alongside making the `jutterswet-tariefstelling`
anchor's `Let` node executable.
