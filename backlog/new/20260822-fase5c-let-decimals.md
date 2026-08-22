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
