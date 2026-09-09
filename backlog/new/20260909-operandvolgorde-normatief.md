# Operand-evaluatievolgorde normatief spellen (Begrensd, TIJDSDUUR)

Goal: RUNTIME-SEMANTICS legt de evaluatievolgorde van multi-operand-
knopen normatief vast waar die volgorde executor-zichtbaar is via het
§9.5-kanaal. Vandaag is de volgorde bij twee knopen alleen af te lezen
uit de ref-binary: `Begrensd` evalueert basis→maximum→minimum en
TIJDSDUUR/ABSOLUTE_TIJDSDUUR evalueert "tot" vóór "van", maar beide
volgen uit OCamls ONgespecificeerde argumentvolgorde bij gelabelde
aanroepen (`Arith.clamp`; gemeten op ocaml-base-compiler 5.4.1,
native én bytecode gelijk — een flambda-build zou kunnen flippen).
De toolchain-Let-port (20260901-toolchain-let-port, Result) heeft die
volgorde in haar statische spoorwandeling gepind op "wat de
referentie-binary doet"; dat hoort andersom: de spec spelt de
volgorde, ref bewijst haar.

Aanleiding: de eindverificatie van de Let-port (2026-09-09) mat dat de
volgorde het §9.5-kanaal raakt — een companion-dragende operand die
vóór dan wel ná een barrière-knallende lezing evalueert, deponeert wel
of niet. Zolang de spec zwijgt is ref's toevallige volgorde de facto
norm.

Acceptance:
- RUNTIME-SEMANTICS draagt per multi-operand-knoop met kanaal-zichtbare
  volgorde (minimaal Begrensd en de tijdsduurfuncties; sweep de andere
  knopen en verklaar per knoop volgorde-zichtbaar of -onzichtbaar) de
  normatieve volgorde met een gewerkt voorbeeld.
- ref maakt de volgorde expliciet (lets in plaats van gelabelde
  argumentvolgorde) zodat de binary niet langer de drager is; gedrag
  byte-identiek op het corpus (0 pin-bewegingen verwacht).
- Een getuige pint de volgorde observeerbaar (kanaalfeit-telling) in
  ref én clingo; alle dertien gates groen.

May touch: spec/RUNTIME-SEMANTICS.md, ref/**, backends/clingo/**
(indien de vertaling de volgorde spelt), corpus/golden/** (getuige),
backlog/.

Out of scope: toolchain-wijzigingen (de spoorwandeling daar volgt
vanzelf bij de eerstvolgende conformance-cyclus); Drools-backend-
wijzigingen tenzij de getuige een divergentie blootlegt.
