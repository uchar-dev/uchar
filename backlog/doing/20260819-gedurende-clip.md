# De gedurende-tijdlijnclip: het temporele filter knipt de tijdlijn

Goal: de §7.1-vorm — een filter op de temporele totaal ("het totaal van
... gedurende de tijd dat <conditie>") — wordt uitvoerbaar als
tijdlijnbewerking: de waardetijdlijn wordt geknipt op de periodes
waarin de conditie houdt, vóór het totaal, exact naar de gepinde
v0-semantiek (rsTijdKnip vóór rsTijdTotaal). Nooit een
instantie-subselectie. Beide executors dragen de clip (ref op de
Timeline-module; clingo over tl/5-segmentintersectie); de
conditietijdlijn (een temporeel kenmerk of een temporele vergelijking)
krijgt zijn case-vorm waar die nog ontbreekt; de verifier dekt de
clipvorm; een anchor pint de knip absoluut, en de vormen die v0 als
marker laat vallen blijven luide faults.

Acceptance: alle acht gates groen; de anchor draait de clip met
absolute waarden door ref én clingo (inclusief een gat in de
conditietijdlijn en een volledig weggeknipte waardetijdlijn); de
niet-clipbare vormen faulten in beide executors; CONSTRUCTS/IR-SPEC/
RUNTIME-SEMANTICS dragen de Landed-notities.

May touch: ref/lib/ (eval, timeline, case, world), backends/clingo/,
spec/verifier/lib/, spec/schema/uchar-case-v0.schema.json (indien de
conditietijdlijn een case-vorm vergt), spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md, corpus/golden/,
corpus/negatief/, corpus/rood/, backlog/.

Out of scope: TIJDSEVENREDIG_DEEL (eigen kaart als een golden hem
vraagt); temporele doelen (schrijven naar tijdlijnen); de
dimensieSelectie.
