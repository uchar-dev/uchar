Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its third write position —
a prioritized-defaults consequence: the `PrioritizedDefaults` on
`Berging.vaarwater` (declared `Vaarwater`) carries one default whose
consequence is a `Literal` `Noord` of domain `Kust` (`initialisation:
true`). A rule reads `vaarwater` faithfully.

    WF-023 FAIL /rules/prioritizedDefaults/0/defaults/0/consequence: a value of
    the enumeration domain 'Kust' is assigned to a target of the
    enumeration domain 'Vaarwater'

Why it has to exist: the defaults consequence had no typing at all — the
ch. 7 rules (WF-050 and kin) shape the group, none types the written
value. Verifier-clean before; ref faulted `match has no arm for
enumeration value "Noord"` at the reader; clingo faulted the translation
(`unscalable datatype`, the pre-existing fence on enumeration defaults —
the faithful twin fails clingo identically). The walk recurses into
exception groups, which inherit the root target.

Falsifiable: drop the defaults position from `wf023` (make the consequence
arm of position 3 return `[]`) and this case passes together with
`wf023-defaults-soort`, while the rule-target and object-creation cases
stay red.
