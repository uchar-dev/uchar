# jutterswet-uniekwantor — dezelfde orde, andere spelling van de negatie

De zusterdocument van `jutterswet-uniestratum`, en het verschil is één ding:
elk takfilter dat daar `niet gemerkt` schrijft, schrijft hier

    aan GEEN van de volgende voorwaarden voldoen: gemerkt zijn

Dezelfde zin, dezelfde uitkomst, een andere wire-vorm — een `Compound(none,
[CharacteristicCheck gemerkt, negated=false])` in plaats van een
`CharacteristicCheck(gemerkt, negated=true)`. Model, case en alle elf
verwachtingen zijn letterlijk die van `jutterswet-uniestratum`; wie de twee
`ir.json`'s naast elkaar legt ziet alleen de filters verschillen.

## Waarom de spelling ertoe doet

Een samengestelde met `none`, `atMost` of `exactly` is NEGATIE-ACHTIG:
`RUNTIME-SEMANTICS.md` ch. 6.1 zegt het en de verifier voert het uit (haar
`dep_cond` noemt het "negation-like: conservative"). De verdictwissel hangt
daar op leden die NIET gelden, dus een kenmerklezing eronder beslist zoals een
genegeerde lezing dat doet, en de regel hoort een band te zakken.

De Drools-stratificatie las de polariteit tot 2026-09-22 uitsluitend uit
`Kenmerk.negatie()` en liep een samengestelde binnen zonder negatiecontext.
Gemeten op dit document, vóór die reparatie:

    ref     11 verwachtingen PASS
    clingo  11 verwachtingen PASS
    Drools exec-model   FAIL  J1: kenmerk alarmerend verwacht afwezig,
                              werkelijk aanwezig
    Drools DRL-tekst    FAIL  idem

Dezelfde ordefoutklasse als bij de zuster, langs een spelling die de reparatie
van die zuster níet dekte. De wandeling draagt nu een conservatieve
negatiecontext mee, precies zoals de verifier hem doorgeeft: eenmaal negatief,
behoudend negatief — een tweede negatie heft de eerste niet op. `all`, `the` en
`atLeast` geven de context dóór in plaats van hem om te zetten, zodat een
positieve lezing eronder positief blijft en geen regel voor niets zakt; die
helft staat gepind in `VouwfilterStratumAnalyseTest`.

## De handberekening

Ongewijzigd ten opzichte van `jutterswet-uniestratum` — `V1`, `V5` en `V7`
wegen boven de drempel 5 en zijn gemerkt, `V4` en `V6` niet:

| jutter | aangemeld | geborgen | ongemerkt per tak | unie | alarmerend |
|---|---|---|---|---|---|
| `J1` | {V1} | {} | {} en {} | **0** | nee |
| `J2` | {V4} | {} | {V4} en {} | **1** | ja |
| `J3` | {V5, V6} | {V6, V7} | {V6} en {V6} | **1** | ja |

`J1` is opnieuw de ordegetuige, `J3` opnieuw de dedupgetuige erbij (`V6` in
beide takken: unie 1, som der delen 2).

Handgeschreven, net als de zuster: de frontend lost geen van beide
negatiespellingen op naar een kenmerk van het element en weigert de zin bij
naam, dus er is geen `bron.rgs` en geen producentroute.
