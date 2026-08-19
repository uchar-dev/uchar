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

Result: geland zoals gesneden. De clip draait in beide executors
(emitter-golden jutterswet-getijdenwacht: het v0-71-patroon verbatim,
de vergelijkingsclip 66, de drie nulvormen én de
split-floor-getuige); characteristicTimelines draagt de waar-periodes
van temporele kenmerken; WF-055 dekt de clipvorm met twee negatiefs;
clingo's kalender komt als data uit de gedeelde Timeline-module zodat
de logica in ASP blijft.

Verificatievangst (MAJOR, gefixt vóór de merge): refs Timeline.clip
coalescde aangrenzende knipfragmenten vóór het floorren, waar v0 én de
ASP-encodering per rauw fragment floorren — empirisch getoond (twee
aangrenzende conditieperiodes over één jaarsegment: ref 10, v0/clingo
0). De coalesce is verwijderd; ch. 3.6 legt de per-fragment-floor nu
expliciet vast en J4 pint hem differentieel. LET OP testwijziging: de
unit-test verify-clip-coalesce pinde de oude (anti-spiegel-)verwachting
100 en is gecorrigeerd naar verify-clip-per-fragment-floor met
verwachting 0 — een inhoudelijke testcorrectie, onderbouwd door de
v0-repro van de verificatieronde, geen afzwakking. Verder: de
clingo-clip-arm-guard is gelijkgetrokken met ref/verifier
(navigatievrij, set=false, eigen type); het navigated-negatief is
geïsoleerd tot WF-055; de Gelijk/Ongelijk-clip is als ref-only
vastgelegd naast de negatie-clip. Buiten scope gebleven:
TIJDSEVENREDIG_DEEL (eigen fold-machinerie); Samengesteld-clips
(ALLE-doorsnede/TEN_MINSTE-unie — v0 kan ze, luide faults bij ons).
