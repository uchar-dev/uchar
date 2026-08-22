# Fase 5b — het kernontwerpvoorstel: sum types (rij 36) en folds/function values (rij 10)

Goal: één ontwerpdocument `spec/proposals/PHASE5B-KERNEL.md` dat beide
kernel-uitbreidingen spec-first uitwerkt — (1) sum types met payload en
compile-time-totale match, (2) general folds en function values (DMN's
BKM+invocation onder hetzelfde ontwerp) — elk met: de nodevormen en hun
plaats in IR-SPEC, de WF-regels (nieuwe nummers vanaf WF-060) met hun
negatiefs, de RUNTIME-SEMANTICS-normtekst in schets met worked examples,
de executor-strategie voor ref én clingo (luide clingo-faults toegestaan
mits als S14-item benoemd), de case-formaat-uitbreiding, de
CONSTRUCTS-delta's voor rijen 10/36, en de plugin-mapping vanaf
scopelang. De échte kernkeuzes gaan als genummerde vragen naar de
maintainer vóór enig spec-werk merget (het WF-059-precedent); de genomen
besluiten worden in het document als Decided vastgelegd.

Acceptance: `spec/proposals/PHASE5B-KERNEL.md` bestaat en is door twee
opus-lenzen gehaald met bevindingen gefixt; elke kernkeuze draagt een
maintainer-antwoord (Decided, met datum); de implementatiekaarten
`20260821-fase5b-sumtypes.md` en `20260821-fase5b-folds.md` staan in
`backlog/new/` met acceptance in gate-termen; gates 1–9 blijven groen
(dit voorstel raakt geen artefactzones).

May touch: spec/proposals/PHASE5B-KERNEL.md (nieuw), backlog/.

Out of scope: elke wijziging aan IR-SPEC.md, RUNTIME-SEMANTICS.md,
spec/schema/, spec/verifier/, ref/, backends/, frontends/, corpus/ —
die volgen in de implementatiekaarten na ratificatie.

Result: `spec/proposals/PHASE5B-KERNEL.md` staat, gelensd en
geratificeerd (2026-08-22, twee genummerde vragen, beide A): rij 36 als
`SumType` met één payload per constructor (gehost door `domeinen`;
`Construct`/`Match`/`BinderRef`; totaliteit als WF; leeg propageert),
rij 10 als de gesloten kern mét positiegebonden `IfThenElse` en
sum-getypeerde accumulatoren (volledig ref≡clingo-differentiaal; de
Reduce→Fold-lowering uitgespeld). De lensronde (2 opus-lenzen) ving
één blocking en vijf majors, alle verwerkt: de verborgen
waarde-conditional-keuze (max/min desugaren naar Reduce met
IfThenElse-reducer) werd het hart van vraag 2; de
VariabeleRef-herbestemming bleek niet-additief → nieuwe knoop
`BinderRef`, row 24 blijft onaangeroerd OPEN; alle Catala-enums →
`SumType` (de alle-nullair→Enumeratie-mapping kon het differentiaal
nooit bereiken); clingo-`Ongelijk` positief encoderen
(beide-gebonden-én-verschillend, Tabel 38); `viol/2`-bereikwachten op
de fold-recurrentie; `model.ml`-werk eerlijk benoemd (recursieve
termparser, payload-geschaald naar de payload-declaratie); de
no-op-leeg-hoeken als reden-dragende survivors; de
WF-008-"sluiting" teruggenomen (eigen WF-nummer, de Enumeration-OPEN
blijft open). Implementatiekaarten `20260821-fase5b-sumtypes.md` en
`20260821-fase5b-folds.md` gesneden in backlog/new. Alle negen gates
groen (61 golden, 89 negatief, 135 cases dubbel geverifieerd, 44
plugin-checks). Bewust blijven liggen: de definitieve
WF-nummertoewijzing (bij implementatie), de Enumeration-WF-008-getuige,
en alles in voorstel-§4 (structs/5c, scope-vangende body's,
Sort/Find/Map2/argmin, plain-value topdefs, Enumeratie-encodering in
clingo).
