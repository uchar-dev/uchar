Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the FOLD-SOURCE arm, unit
facet: a `SOM` over `het gewicht van alle schuiten van de Berging` reads
`Schuit.gewicht`, declared in `kg`, through a fold source typed in `t`.
The aggregation's own node type is faithful (`kg`) and the target
`Berging.totaalgewicht` is declared in `kg`, so WF-023 is satisfied.

    WF-091 FAIL /resolvedRegels/0/waarde/argumenten/0/type: unit mismatch:
    fold-source attribute reference 'gewicht' is typed 't' of system
    'Gewicht' (factor 1000 to 'kg'), but the declaration it reads from is
    'kg' of system 'Gewicht' (factor 1 to 'kg')

Why it has to exist: the fold source is the one reference position the
ordinary expression walk never enters — the fold-source shape rules replace
it — so every identity that is stated once for references has to be applied
there a second time, by hand. WF-089 and WF-090 both carry a `foldbron`
case for that reason; this is the same arm for the unit facet. A computing
fold (`SOM`) was chosen deliberately: it is not an element-passing fold, so
the aggregation arm stays out of the way and the message is arm-exact.

Falsifiable, arm-exact: delete the `use_site_identity` call in the
fold-source branch of the aggregation walk and this case turns clean
together with `wf089-foldbron-enum` and `wf090-foldbron`, while every other
arm stays red.
