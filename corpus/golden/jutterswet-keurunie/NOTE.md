# jutterswet-keurunie — de unie over ÉÉN relatie, en de kale selectie ernaast

De tweede getuige van WF-110 en `RUNTIME-SEMANTICS.md` ch. 3.4d, en de vorm die
§5.7 in haar eigen uitgewerkte voorbeeld spelt: meerdere selecties over dezelfde
relatie, elk met haar eigen kenmerk-deelselectie. `jutterswet-vondstunie` pint
de unie over verschillende relaties; dit document pint de twee dingen die daar
niet te zien zijn.

## Regel 1 — overlappende filters

    Het aantal keurstukken van een Jutter moet berekend worden als het aantal
    aangemelde vondsten van de Jutter die gemerkt zijn en aangemelde vondsten
    van de Jutter die gekeurd zijn.

`J1` meldt `V1` (gemerkt), `V2` (gekeurd) en `V3` (beide) aan. De selecties zijn
{V1, V3} en {V2, V3}; `V3` voldoet aan allebei en telt één keer, dus **3** waar
de som der delen 4 is. `J2` meldt één vondst aan die geen van beide is: beide
selecties leeg, **0**. `J3` meldt niets aan: **0**, nooit leeg.

## Regel 2 — een KALE selectie naast een gefilterde

    Het aantal gemelde stukken van een Jutter moet berekend worden als het
    aantal aangemelde vondsten van de Jutter die gemerkt zijn en aangemelde
    vondsten van de Jutter.

Hier wijkt uchar gemotiveerd af van de v0-toolchain, die deze vorm als marker
laat liggen. Haar reden is mechanisch en staat in haar eigen test: zij lowert
een concatenatie door de filtertermen van de operanden te OR-en, en een operand
zonder filter levert geen term op. Onze wire draagt de SELECTIES zelf, dus
`filter: null` is hier de gewone vorm en geen onuitdrukbare.

`J1`: {V1, V3} ∪ {V1, V2, V3} = **3** — de kale selectie slokt de gefilterde op,
wat precies de eigenschap is die een stilzwijgend weggelaten filter onzichtbaar
zou maken; daarom weigert de frontend een deelselectie die niet oplost, in
plaats van hem te laten vallen. `J2`: {} ∪ {V7} = **1**. `J3`: **0**.

## Rood-eerst

Gemeten op de frontend van vóór deze kaart (5b42e5b9): beide zinnen werden
geweigerd met de WF-037-ariteitsfout en het document bereikte de schijf niet.
Sinds de kaart emitteert `source.rgs` byte-identiek aan de gecommitte `ir.json`
(poort 10), is het document verifier-schoon (poort 3) en antwoorden `ref` en
clingo dezelfde zes absolute getallen (poorten 5 en 6).
