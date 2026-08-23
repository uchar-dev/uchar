# Fase 5c, kaart 3 — Let, de rule-level-conditional en decimals

Goal: PHASE5C-TUTORIAL §5–6 onder de geratificeerde besluiten (vraag
2: A — strikte Let-knoop met conditie-operand-discipline; vraag 4: A —
gepinde decimaalschaal S): de `Let`-knoop (strikt: leeg-waarde → leeg;
binders via BinderRef, WF-064-verbreding); WF-069-herbereik
(IfThenElse ook in Let-body's) plus de conditie-operand-WF (alleen
binders/parameters/literals; directe attribuut-guards geweigerd,
recorded); het clingo-guarded-consequence-ontwerp (meervoudige
bewaakte kopregels op consequentiepositie); decimals: TRat →
Numeriek{geheel=false, decimalen=S} (S tegen de tutorial gekozen en in
het voorstel vastgelegd), LRat-literals, de rationale operatoren en
casts, luide consistente faults op niet-terminerend-op-S; het
Afgerond{REKENKUNDIG,2}-omhulsel op Mult_mon_rat/Mult_mon_int
(byte-neutraal voor het bestaande corpus); de
deprecation-warning-tolerantie; ankers en fixtures door alle gates.

Acceptance: alle negen gates groen; rood-eerst; herpin; twee
opus-lenzen met bevindingen gefixt vóór de merge.

May touch: als kaart 1. Out of scope: stdlib-modules (blijven
geweigerd — de som-vormen zijn 5b-gedekt); subscopes.

Result: de strikte Let, de rule-level-conditional en decimals zijn
kernel, end-to-end, onder de geratificeerde besluiten (vraag 2: A;
vraag 4: A met S = 4 als implementatiekeuze, vastgelegd in RS ch. 2.9).
Spec-first: IR-SPEC ch. 4.10 (de strikte Let met de leeg-barrière;
WF-076) en WF-077 (de conditie-operand-discipline als
LEAF-READ-lezing: rekenkundige combinatoren over binders/parameters/
literals toegestaan, elke attribuut-/veld-/kenmerklezing verboden —
de letterlijk-atomaire lezing zou het gescheepte staffelvergoeding-
anker rood hebben gemaakt; de spec-agent legde die keuze open op
tafel), het WF-069-herbereik (IfThenElse ook in Let-body's) en de
WF-064-verruiming; RUNTIME-SEMANTICS ch. 10.6 (de strikte Let mét het
normatieve ch. 1.2-contrast: kale maal coalesct, de Let propageert —
precies waarom de discipline bestaat) en ch. 2.9 (de decimals-pin,
de niet-termineren-fault in beide executors, de
geldproduct-afronding). De keten met rood-eerst: vier negatiefs, het
anker jutterswet-tariefstelling (drie cases op tutorial-magnitude:
€20.000 × 21% = €4.200 → aanslag 3.700,00; de dan-tak; de dubbele
leeg-barrière — mét het bewijs dat zonder Let maal zou coalescen), de
rood-getuige decimal-nonterminating (1/3 op 4 decimalen, beide benen),
ref (strikte Let; 245 WV-checks incl. shadowing), clingo (de Let als
gebonden intermediair; guarded consequences per tak, NAF-vrij; de
GESPLITSTE geldvermenigvuldiging — de rat in twee schaal-2-helften,
wiskundig herbewezen door de lens incl. ties, elk deelproduct binnen
de hekken; ASP-TRANSLATION S18, inventaris → S19), en de plugin
(let-in → Let; decimals S=4 met luide weigering van
niet-terminerende literals; de rat-operatoren en casts;
Mult_mon_rat/int → Afgerond{REKENKUNDIG,2}, byte-neutraal voor het
bestaande corpus; vijfde Catala-golden jutterswet-tariefrecht-catala
— de vertakkende tariefboom uit kaart 2 gecomponeerd met de
let-if-vorm; 103 checks). Lensronde (2 opus): geen blocking, nergens
stille divergentie; één MAJOR — clingo's waarde-positie-conditie
accepteerde alleen kale operanden waar WF-077 combinatoren toelaat,
en S19 noemde die backstop onterecht "unreachable" (empirisch
weerlegd) → operand_ok verbreed naar de bound_only-vorm (ref ≡ clingo
op de toets) en S19 gecorrigeerd; plus de twee luide
niet-vertalingen (Let in gesloten body's; Let→IfThenElse buiten de
kale regelwaarde-positie) eerlijk geïnventariseerd met de ch.
4.10-executornotitie, de shadowing-getuigen, de
WF-076-diagnostiek met decimalen, en twee formuleringspunten. Alle
negen gates groen: 76 ir.json round-trip, 136 negatiefs, 196 cases,
179 dubbel (ref ≡ clingo), 157 no-op-refuters + 22 survivors, 17
rood, 103 plugin-checks, 5 Catala-goldens byte-identiek. Bewust
gelaten (luid, S19): Let in gesloten body's en Let→IfThenElse op
ladder-rung-/creatie-/sum-/struct-posities (geen corpus-case);
negatieve geldproducten en de magnitude-hekken faulten luid;
ref/test/test_ref.ml gewijzigd (kaart-gesanctioneerd, gemeld). Herpin
volgt direct na de merge.
