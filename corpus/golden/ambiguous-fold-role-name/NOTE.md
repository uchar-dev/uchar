# ambiguous-fold-role-name

*Until L2 a red case, since L2 a golden* (card `l2-referentielaag`, D7/D8).
The count "het aantal vriend van de Jutter" writes the role name `vriend`,
and `vriend` is declared on TWO fact types the Jutter reaches —
`vondstband` (to `Vondst`) and `bakenband` (to `Baken`). While the fold's
range was derived from that written name, the name resolved to two roles
and both executors refused to pick one. Since L2 the range is written: the
count carries its `bronRol` (`vondstband` / `vriend`), the only relation
that reaches the element type `Vondst`, so the name ambiguity has nothing
left to decide. The written name is presentation and the count reads the
binding (IR-SPEC ch. 4.3, D8 (ii)).

**Hand computation.** The count folds the instances its range reaches from
the subject (RUNTIME-SEMANTICS ch. 3.4a) and is never leeg (ch. 1.2e): the
count over the empty set is 0. In `case1` J1 has no link at all, so the
range reaches nothing and `aantal vrienden` of J1 is **0**.

Why only that case: a case can supply no link to J1 over either `vriend`
role. The case key `vriend` is ambiguous from `Jutter`, and so is the
reverse leg the case loader derives from a link written on the far side
(ch. 5.7a): measured, both executors refuse such a case at load with
`role key "vriend" is ambiguous from "Jutter"`. The golden therefore pins
the one outcome this model can carry: the document evaluates, and the
bound count answers 0 in both executors instead of a fault.
