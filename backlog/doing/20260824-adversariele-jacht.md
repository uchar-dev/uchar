# Adversariële jacht, eerste sessie

Goal: de staande PLAN §8-jacht, eerste uitvoering: vind programma's
waarop het differentieel-vierkant divergeert (ref ≢ clingo) of de
verifier ten onrechte accepteert, gericht op de jonge constructen
(sumtypes, folds, structs, geneste groepen, Let/decimals,
subscope-lowering, kenmerk-init-expressies) en de klassieke flanken
(leeg, randwaarden, schalen, diepe nesting, grote getallen,
tijdvensters). Hand-gerichte constructie, geen blinde fuzzing; elk
vermoeden empirisch door beide executors. Elke vondst wordt
GEMINIMALISEERD en dan gerouteerd: een verifier-vals-accept wordt een
corpus/negatief-voorstel mét rapport; een echte, kleine executor-bug
mag binnen het mandaat gefixt (rood-eerst, smalste wijziging, beide
executors); een grote of ontwerp-rakende vondst blijft een
kaartvoorstel in het rapport. KRITIEK: een onopgeloste
ref↔clingo-divergentie kan NIET als corpus-case worden ingecheckt
(gate 6 zou rood) — die blijft geminimaliseerd in het rapport.

Acceptance: alle negen gates groen op de branch (wat is ingecheckt is
af); het jachtrapport in de kaart somt élk onderzocht spoor op —
ook de sporen die niets vonden (negatieve resultaten zijn data);
twee opus-lenzen verwerkt over de ingecheckte wijzigingen.

May touch: corpus/negatief/, corpus/rood/, ref/ en backends/clingo/
(UITSLUITEND voor kleine mandaat-fixes met rood-eerst; meld elk),
scripts/check-noop.sh (alleen indien nodig), backlog/. Out of scope:
spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/ (een verifier-gat is een negatief-voorstel + rapport,
geen eigen check-wijziging), frontends/, spec/CONSTRUCTS.md.
