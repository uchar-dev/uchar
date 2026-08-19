# Aggregatie-filters: de die/dat-subselectie executeert

Goal: de gefilterde fold — "de som/het aantal van ... die <conditie>" —
wordt uitvoerbaar in beide executors en correct geverifieerd. De
filterconditie evalueert PER ELEMENT (het contributor-subject, niet het
buitensubject): ref filtert de contributor-lijst met eval_cond op de
contributor-instantie; clingo voegt de filter-atomen over de
contributor-variabele toe binnen de aggregaat-conditie (een leeg
filter-operand sluit de contributor uit via de unbound join — ch. 1.2d
gratis); de verifier-wandeling en dep_cond nemen het elementtype als
subject voor het filter (de vastgelegde buitensubject-kwestie uit de
pin-bump-verificatie). Een anchor pint gefilterde som en aantal
absoluut, inclusief de leeg-uitsluiting en een kenmerk-filter; de
emitter produceert filters al (de gefixte element-subject-emissie van
pin fe46e49 krijgt zo zijn uitvoerende getuige).

Acceptance: alle acht gates groen; de anchor draait gefilterde folds
met absolute waarden door ref én clingo; de verifier accepteert
element-eigen filterlezingen (geen onterechte WF-053-to-many) en blijft
genavigeerde/foute filtervormen afwijzen; CONSTRUCTS-notitie
bijgewerkt.

May touch: spec/verifier/lib/checks.ml, ref/lib/eval.ml,
backends/clingo/lib/translate.ml, spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md,
corpus/golden/ (anchor of emitter-golden), corpus/negatief/ (indien een
nieuwe clausule dat vraagt), backlog/.

Out of scope: temporele (gedurende-)filters — v0-marker, blijft fault;
dimensieSelectie; filters op verdeling/creatie-vormen buiten wat v0
emit.
