Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) at the second write position —
an object creation's attribute member. An `ObjectCreation` creates a
`Bundel` per `Jutter` and initialises `kern`, declared with the struct
domain `dossier`, with a `StructLiteral` of the second declared struct
domain `logboek`. A rule on the created type reads `kern.waarde`
faithfully.

    WF-090 FAIL /rules/objectCreaties/0/versions/0/initialisations/0: a value of
    the struct domain 'logboek' is assigned to a target of the struct domain
    'dossier'

Why it has to exist beside `wf090-schrijf-regeldoel`: it is the position
where the split between the executors is widest and quietest. Ref faulted
(`field "waarde" is not present on the record value`) while clingo
reported `clingo: PASS (8 expectations)` — the created object's derived
attribute is outside every pinned expectation, so the ASP side stayed
green on a document ref cannot evaluate. The faithful twin passes both
(`oracle: PASS (8 expectations)`, `clingo: PASS (8 expectations)`).

Falsifiable, arm-exact: suppress WF-090 at write position 2 only (filter
`x.wf <> "WF-090"` off the `wf023_assignment` result in the object-creation
branch of `wf023`) and this case alone turns clean; the rule-target and
defaults cases stay red.
