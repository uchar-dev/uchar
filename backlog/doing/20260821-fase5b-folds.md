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

Voortgang:
- Opdracht 1 (schema + verifier + WF-065..070-negatiefs), 2 (de twee
  ankers), 3 (ref), 4 (clingo) gemerged.
- Opdracht 5 (2026-08-22): de Catala-plugin draagt de fold-familie —
  `TArray`→`ListType` (WF-065-weigering voor ontoelaatbare elementen),
  `EArray`→`ListLiteral`, `Map`/`Filter`/`Fold`/`Length` direct, `EAbs`
  in foldpositie→`Lambda`, `IfThenElse` alleen in body's (rule-niveau
  blijft geweigerd), topdefs→`FunctionDef` + `Invoke`, en de `Reduce`-
  lowering (`sum`/`maximum`/`minimum`) naar een `Fold` over een concrete
  `Optional`-som (het universele option_enum-payload omzeild) met een
  extractie-`Match`. Weigeringen: scope-vangende body's (WF-067),
  Sort/Find/Map2/argmin, plain-value topdefs. De clingo-backend is
  uitgebreid voor de reduce-lowering (sum-getypeerde foldaccumulator
  scale-0, `Construct` met berekende payload in body's, `Match` over een
  Fold-resultaat — ASP-TRANSLATION S15). Derde catala-golden
  `jutterswet-penningstaffel-catala` (maximum-met-default + numerieke
  fold; drie beslissende cases, alle drie falen --noop; drievoudig
  groen). Suite +17 checks (76 totaal; drie catala-goldens byte-
  identiek). Alle negen gates groen. Nog te doen vóór merge: de
  toolchain-herpin en de twee opus-lenzen.
