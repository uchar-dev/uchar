Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the WRITE-TARGET arm, unit
facet: the rule target reads `Jutter.kopie`, declared in `kg`, but its
serialized `type` says `t`. The value written is a faithful reference to
`Jutter.gewicht` (`kg`), so WF-023's assignment cross is satisfied — it
crosses the value against the target's DECLARED datatype and never looks
at the target's own annotation.

    WF-091 FAIL /resolvedRegels/0/doel/type: unit mismatch: rule target
    'kopie' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'), but
    the attribute declaration it writes is 'kg' of system 'Gewicht'
    (factor 1 to 'kg')

Why it has to exist: the target lvalue is WF-022's and the expression walk
deliberately never enters it, which left the target `type` the one
reference type in a document that nothing crossed. WF-088's index row
records that omission as measured harmless for a drifted enumeration copy
— both executors write through the DECLARED datatype. It is not harmless
for a unit: a consumer that honours the annotation rescales by the factor
and deposits a value a thousand times off, which is what the card measured
on this shape.

Falsifiable, arm-exact: delete position 0 from `wf023` and this case alone
turns clean; every read-arm case stays red, and so does
`wf023-schrijf-doeltype-leugen`, which loses only its incidental second
message.
