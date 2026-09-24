# ambiguous-fold-role-name

*Until L2 a red case, since L2 a golden* (`spec/proposals/V1-WIREMIGRATIE.md`
B12; the L2 card, `backlog/done/20260923-l2-referentielaag.md`). The count
"het aantal vriend van de Jutter" writes the role name `vriend`, and `vriend`
is declared on TWO fact types the Jutter reaches — `vondstband` (to `Vondst`)
and `bakenband` (to `Baken`). While the fold's range was derived from that
written name, the name resolved to two roles and both executors refused to
pick one. Since L2 the range is written: the count carries its `bronRol`
(`vondstband` / `vriend`), the only relation that reaches the element type
`Vondst`, so the ambiguity of the name is never asked. The written name is
presentation, and the count reads the binding (IR-SPEC ch. 4.3).

The two roles also declare distinct plurals, `vondstvrienden` and
`bakenvrienden`, so a case can key a link on either relation.

**Hand computation.** The count folds the instances its range reaches from
the subject (RUNTIME-SEMANTICS ch. 3.4a) and is never leeg (ch. 1.2e): the
count over the empty set is 0.

- `case1`: J1 has no link at all, so the range reaches nothing and
  `aantal vrienden` of J1 is **0**.
- `case2` tells the two bindings apart. J1 is linked over `vondstband` to V1
  and V2, and J2 only over `bakenband` to B1. The range `vondstband` /
  `vriend` reaches {V1, V2} from J1, so J1 counts **2**; from J2 it reaches
  nothing, so J2 counts **0**. A range bound to `bakenband` would reach
  nothing from J1, and from J2 only B1, which is no `Vondst` (ch. 2.2), so
  it would count 0 for both.

Measured 2026-09-24: ref and clingo pass both cases. On the mutant
`bronRol := bakenband / vriend` with `case2`, ref reports
`MISMATCH: J1.aantal vrienden = 0, expected 2`, clingo reports the same
mismatch, and the verifier refuses the mutant with WF-047 (`the bound role
'vriend' fills 'Baken', but the node names 'Vondst'`).

The singular key `vriend` stays ambiguous from `Jutter` in a case. The case
loader of ref and clingo refuses it at load, `role key "vriend" is
ambiguous from "Jutter"` (measured 2026-09-24), which is the refusal
`corpus/rood/ambiguous-case-role-key` pins; that refusal belongs to the
OCaml case loader, not to the document.
