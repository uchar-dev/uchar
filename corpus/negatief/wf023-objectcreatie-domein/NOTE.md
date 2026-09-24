Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its second write position —
an object creation's attribute member: the `ResolvedObjectCreatie` creates
a `Berging` for each `Jutter` and initialises `vaarwater` (declared
`Vaarwater`) with a `Literal` `Noord` of domain `Kust`. A rule reads the
created object's `vaarwater` faithfully.

    WF-023 FAIL /rules/objectCreaties/0/versions/0/initialisations/0: a value of
    the enumeration domain 'Kust' is assigned to a target of the enumeration
    domain 'Vaarwater'

Why it has to exist: WF-027 checks that the member EXISTS and types only
the characteristic value; no rule typed an attribute member's value at all
(see `wf023-objectcreatie-soort` for the soort layer). Verifier-clean
before; ref faulted `match has no arm for enumeration value "Noord"` at
the reader; clingo faulted the translation (`unscalable datatype` — the
pre-existing carrier fence on an enumeration object-creation member, so
clingo carries neither this document nor its faithful twin).

Falsifiable: drop the object-creation position from `wf023` (make the
`Some (Some dt), Some wt` arm of position 2 return `[]`) and this case
passes together with `wf023-objectcreatie-soort`, while the rule-target
and defaults cases stay red.
