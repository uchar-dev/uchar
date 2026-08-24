# WF-voorstel: kruiscontrole van BinderRef.type

Herkomst: de eerste adversariële jachtsessie
(`backlog/doing/20260824-adversariele-jacht.md`, spoor 7b). Dit is een
KAARTVOORSTEL, geen ingecheckte wijziging — `spec/verifier/` is een
protected path, dus de jacht raakt de check niet zelf aan.

## De vondst

De verifier kruiscontroleert het `type` van een `AttribuutRef` tegen de
gedeclareerde attribuut-datatype (WF-023; het lieg-geval heeft een
negatief: `corpus/negatief/wf027-lying-attribuutref-type/`). Voor een
`BinderRef` bestaat die kruiscontrole NIET. WF-064 controleert alleen
(a) dat elke BinderRef lexicaal naar een omsluitende binder resolvet en
(b) dat de arm-lichamen van een Match op één resultaat-datatype
overeenkomen (`spec/verifier/lib/checks.ml:2218`, `:2244`). Het `type`
dat het BinderRef-blad zelf draagt — schema-verplicht en non-null
(`spec/schema/regelspraak-ir-v0.schema.json:1352`) — wordt nergens tegen
de gedeclareerde datatype van de binder gehouden.

Gevolg: een IR waarin een genest BinderRef-blad een `type` draagt dat
zijn binder tegenspreekt, is verifier-schoon, maar splitst het
differentieel-vierkant:

- decimalen-leugen (2→4 op `x` in een fold-lichaam `acc + x`): INERT —
  beide executors negeren de annotatie (clingo leest de binder-schaal uit
  `ctx.binders`, ref de runtime-waarde). verifier PASS, ref PASS, clingo
  PASS, dezelfde waarde. Geen splitsing, wél een geaccepteerde
  inconsistentie.
- soort-leugen (`Numeriek` → `Tekst` of `Booleaan` op `x`/`acc`):
  verifier PASS + ref PASS (annotatie genegeerd, correcte waarde) +
  clingo TRANSLATION FAULT `"non-numeric arithmetic operand is not
  translated (no corpus case)"`. De clingo-fout komt uit `numeric_ok`
  (`backends/clingo/lib/translate.ml:383`), dat `typ_of_expr` van de
  operand leest en op `Tekst`/`Booleaan` weigert.

De splitsing is LUID (clingo weigert, ref rekent) — nooit stil, dus geen
corpus-case en geen zwijgende divergentie. Maar het is precies de klasse
die de verifier hoort te vangen: een zelf-tegensprekende IR die geen enkel
frontend hoort te emitteren en die de WF-023-broer voor een AttribuutRef
al zou afkeuren. De verifier is de poortwachter van de differentiële
vierkant-invariant; deze leugen glipt erdoor.

## Minimale reproductie

Basis: `corpus/golden/anchor/jutterswet-aanspraaktelling/` (fold over een
sumtype-lijst met een `acc + x`-lambda-lichaam). Mutatie: zet op de
`rechts`-operand (BinderRef `x`) van het fold-lichaam
`type = {"node":"Tekst"}`. Draai verifier/ref/clingo met
`case1-gemengd.json`:

    verifier: PASS
    oracle:   PASS (3 expectations)        # ref: erkende som 12.50, aantal 2 — correct
    clingo:   TRANSLATION FAULT: non-numeric arithmetic operand ...

Dezelfde leugen op de decimalen (2→4) levert driemaal PASS — inert.

## Voorstel

Breid WF-064 (of een nieuwe WF-nabuur) uit met: het `type` van elke
`BinderRef` moet de gedeclareerde datatype van zijn resolvende binder
zijn (match-arm binder-payload, lambda/functie-parameter, of Let-binder),
onder dezelfde ci/decimalen/eenheid-vergelijking die WF-023 voor
AttribuutRef gebruikt; de `Datatype_onbekend`-sentinel defereert naar de
runtime fail-fast, net als WF-023/WF-027. Bij landing hoort een negatief
`corpus/negatief/wf064-lying-binderref-type/` (soort-leugen), broer van
`wf027-lying-attribuutref-type`.

Merk op: het decimalen-lieg-geval is INERT (geen gedrag), dus of de check
ook decimalen-consistentie afdwingt is een spec-keuze; de soort-leugen is
de gedrag-rakende splitsing en is het minimale doel.

## Reikwijdte / risico

Klein en mechanisch (één statische typvergelijking, model WF-023). Raakt
`spec/verifier/` + `spec/IR-SPEC.md` (WF-index) + een negatief-case —
alle protected, dus maintainer-werk. Geen executor-wijziging nodig: ref
en clingo gedragen zich al correct (ref negeert, clingo weigert luid); de
check haalt de inconsistente IR gewoon weg vóór de executors.
