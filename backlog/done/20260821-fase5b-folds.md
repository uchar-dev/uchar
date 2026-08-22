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

Result: rij 10 is kernel, end-to-end, op het rij-36-substraat. Spec-first:
IR-SPEC ch. 2.4 (ListType, WF-065), ch. 2.11 (functionDefs — DMN's
BKM, WF-067: gesloten body's, acyclische invocatiegraaf) en ch. 4.8
(ListLiteral/Lambda/IfThenElse/Invoke/Fold/MapList/FilterList/
ListLength, WF-066/068/069/070; FilterList als binder-plus-conditie
naar het aggregatie-filter-precedent — nergens boolean-getypeerde
expressies; IfThenElse positiegebonden aan body's), RUNTIME-SEMANTICS
ch. 10 (lijstwaarden met de []-vs-leeg-scheiding; linksvouw met
normatieve volgorde; de gesloten-body-leeg-vrij-invariant). Daarna de
keten met rood-eerst per stap: schema's additief, verifier met twaalf
wf06[5-9]/wf070-negatiefs, de ankers jutterswet-staffelvergoeding
(begrensde vouw 19.75 — volgorde-dragend: herordend gaf 37.25; [] →
0.00; afwezig → leeg als survivor) en jutterswet-aanspraaktelling
(ch. 9-compositie: MapList+Invoke+Fold → 12.50, FilterList+Ongelijk+
ListLength → 2), ref (Lijst-waarde, apply_fold_fn, WV-10-blokken —
testbestand gewijzigd, gesanctioneerd en gemeld), clingo
(lpres/lelem-reïficatie, acc-indexrecursie mét viol/2-hekken,
IfThenElse per-tak met builtin-complementen zonder NAF, Invoke
geïnlined; ASP-TRANSLATION S15 nieuw, inventaris → S16 terminaal), en
de plugin (Map/Filter/Fold/Length direct; de Reduce→Fold-lowering met
concrete Optional-SumType-accumulator; topdefs → FunctionDef;
scope-vangende body's en Sort/argmin/plain-value-topdefs weigeren
luid; derde Catala-golden jutterswet-penningstaffel-catala met
maximum-of-lijst — de lowering, IfThenElse en Fold op de wire,
byte-identiek geregenereerd; 76 checks). Lensronde (2 opus): één
BLOCKING, onafhankelijk door beide lenzen empirisch bewezen — de
implementatie-agent had leeg-bij-afwezige-parameter ONVOORWAARDELIJK
gemaakt, waardoor een afwezige scalaire parameter ref (0.00 via de
else-tak) en clingo (onafgeleid) stil splitste; versmald naar
uitsluitend lijst-parameters, de ch. 5.7-fault hersteld en de
invariant met twee WV-tests gepind. Verder: Fold-over-gefilterde-lijst
rekende in clingo stil mis (indexgaten; verifier-clean) → nu een
luide gerichte fault in de S16-inventaris; Fn_name als fold-operand
(getuigenloos ref-accepteert/clingo-weigert) → per WF-069 versmald
met eigen negatief; de rij-10-stempel ontdaan van een teltfout en een
executor-overclaim. Alle negen gates groen: 66 ir.json round-trip,
115 negatiefs, 165 cases, 151 dubbel (ref ≡ clingo), 132
no-op-refuters + 19 survivors, 14 rood, 76 plugin-checks, 3
Catala-goldens byte-identiek; @ref/runtest 216. Bewust gelaten (luid,
geïnventariseerd in S16): ListLiteral als fold-bron, Fold over een
gefilterde keten (herindexering), rule-level Invoke in clingo,
lijst-verwachtingen in model.ml, en de niet-lijst-vormen van de
weigerlijst; de Catala-tutorial wacht nog op structs+subscopes (5c).
Herpin volgt direct na de merge; de pin-SHA-stempels van rijen 36 en
10 in CONSTRUCTS volgen met de eerstvolgende kaart.
