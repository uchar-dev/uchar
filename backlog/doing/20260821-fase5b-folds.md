# Fase 5b, ontwerp 2 — folds en function values (rij 10)

Goal: `spec/proposals/PHASE5B-KERNEL.md` §3 end-to-end, onder de
geratificeerde besluiten (vraag 2: de gesloten kern mét positiegebonden
`IfThenElse` en sum-getypeerde accumulatoren), voortbouwend op het
`BinderRef`/`locals`-substraat van ontwerp 1. De zeven kernvormen van
§3.1: `ListType`, `ListLiteral`, `FunctionDef` (gesloten body's,
acyclische call-graph — DMN's BKM), `Invoke`, `Lambda`
(positiegebonden), `IfThenElse` (alleen in body's; conditie uit de
bestaande conditie-unie), `Fold`/`MapList`/`FilterList`/`ListLength`
(accumulator eerste-orde-niet-lijst). WF-regels per §3.4 met
negatiefs; RUNTIME-SEMANTICS ch. 10 met worked examples (fold
links-vouwend, `[]` → init, leeg propageert; de `Reduce`-lowering als
voorbeeld) als `WV-10.*`-blokken; case-formaat `{"list": […]}`;
beide executors — ref per §3.3 en clingo met `elem/3`-reïficatie,
indexrecursie mét `viol/2`-bereikwachten op accumulator en
`MapList`-uitvoer, `IfThenElse` per-branch met builtin-complementen,
`Invoke` geïnlined of als helperpredicaat-familie; nieuwe
ASP-TRANSLATION-sectie(s) met Witnesses (inventaris terminaal);
Jutterswet-ankers per §3.6 (staffel-fold met cap, map/filter over
sum-waarden, max-via-`Reduce`; lege-lijst/leeg-lijst-hoeken als
reden-dragende survivors); CONSTRUCTS rij-10-deltastempel; en daarná
de plugin-vertaling per §3.5 (Fold/Map/Filter/Length direct; `Reduce`
gelowered naar Fold met `Optional`-accumulator; topdef-functies →
`FunctionDef`; scope-vangende body's, Sort/Find/Map2/argmin en
plain-value topdefs weigeren luid met rij-10-teksten) met
suite-fixtures.

Acceptance: alle negen gates groen met de nieuwe ankers, negatiefs en
fixtures erin; rood-eerst getoond voor elke nieuwe WF-check en gate-
uitbreiding; daarna herpin in de toolchain (SPEC_REV-bump,
`make uchar-conformance UCHAR_DIR=…` 11/11 byte-identiek); twee
opus-lenzen met bevindingen gefixt vóór de merge.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
spec/ASP-TRANSLATION.md, spec/CONSTRUCTS.md, spec/schema/,
spec/verifier/, ref/ (incl. test — melden), backends/clingo/,
frontends/catala/, corpus/ (golden incl. anchor, negatief, rood,
jutterswet-bronmateriaal), scripts/check-noop.sh (survivors),
backlog/. Toolchain: uitsluitend de herpin-commit.

Out of scope: structs/subscopes en de `no_stdlib`-flip met
`Integer.sum`-toelating (5c-route); attribuut-/scope-lezende body's;
eerste-klasse functiewaarden; lijst-accumulatoren; hereniging van de
`Functie`-aggregatiecatalogus met de fold-familie; `IfThenElse` buiten
body's.
