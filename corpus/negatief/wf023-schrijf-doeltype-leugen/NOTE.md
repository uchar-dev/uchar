Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on its domain clause through
the one position WF-088 deliberately leaves outside the expression walk —
the rule TARGET's serialized `type`. The target `vaarwater` is declared
`Vaarwater`, the value is a `Literal` `Noord` of domain `Kust`, and the
target's own `type` ALSO says `Kust`: target type and value type agree with
each other and both lie about the declaration. The cross is against the
target's DECLARED datatype (`resolved_attr_datatype`), so the lie does not
help.

    WF-023 FAIL /rules/resolvedRegels/0: a value of the enumeration domain
    'Kust' is assigned to a target of the enumeration domain 'Vaarwater'

Why it has to exist: the previous WF-023 compared the SERIALIZED target
type with the value type — a matching pair of lies passed it (verifier
PASS), ref faulted `match has no arm for enumeration value "Noord"` at the
faithful reader and clingo left `B.tarief` underived. WF-088's index row
records the target-type drift as "measured harmless" because both
executors write through the DECLARED datatype — which is exactly why the
declared datatype, not the serialized one, is the side to cross here.

Falsifiable: make position 1 of `wf023` take `typ_of_expr doel` instead of
preferring `resolved_attr_datatype ctx doel` and this case passes while
every other `wf023-*` case (whose target types are truthful) stays red.
