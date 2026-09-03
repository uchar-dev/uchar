Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) on the READ side through a
`ListType` element: the `MapList` source reads `Jutter.dossiers`, declared
`list of dossier`, but its `type` is `list of logboek`; the map's lambda
parameter and the `BinderRef` in its body agree with that claim, so the
document is internally consistent everywhere except at the declaration
boundary, and WF-078's soort cross (struct against struct) sees nothing.

    WF-090 FAIL /resolvedRegels/1/waarde/list/list/type: attribute
    reference 'dossiers' is typed with struct domain 'logboek', but the
    declaration it reads from names struct domain 'dossier'

Why it has to exist beside `wf090-attribuutref`: it is what pins the
look-through, and it carries the worst executor outcome of the set. Ref
faulted at the field read, but clingo produced a **wrong answer** —
`J1.totaalwaarde = [0]` where 12.50 was expected — the struct twin of the
list-element flank `wf089-attribuutref-lijst-sum` records for sum types.
A scalar case cannot pin the `List_type` line of `struct_domain`.

Falsifiable, arm-exact: drop the `List_type` recursion from
`struct_domain` and this case alone turns clean — every other WF-090 case
crosses a scalar struct type and stays red.
