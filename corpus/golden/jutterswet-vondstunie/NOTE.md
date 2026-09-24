# jutterswet-vondstunie — de unie over TWEE relaties, en de dedup die daar echt werk is

De getuige van WF-110 en `RUNTIME-SEMANTICS.md` ch. 3.4d: de §5.7-concatenatie
van onderwerpexpressies als bron van een instantietelling. De zin —

    Het aantal strandvondsten van een Jutter moet berekend worden als het
    aantal aangemelde vondsten van de Jutter en geborgen vondsten van de
    Jutter.

— telt over de unie van twee selecties die over VERSCHILLENDE relaties lopen
(`vondstaanmelding` en `vondstberging`) maar over dezelfde populatie
(`Vondst`). Die vorm heeft geen OR-filterspelling: één telling met een
samengestelde filter kan alleen selecties over dezelfde bronrange samenvatten.
Daarom is dit het document waarop deduplicatie ophoudt vanzelf te gebeuren en
een operatie wordt die elke uitvoerder afzonderlijk goed moet doen.

## De handberekening

| jutter | aangemeld | geborgen | unie | som der delen |
|---|---|---|---|---|
| `J1` | {V1, V2, V3} | {V3, V4} | **4** | 5 |
| `J2` | {V5} | {V5} | **1** | 2 |
| `J3` | {} | {V6} | **1** | 1 |
| `J4` | {} | {} | **0** | 0 |

`J1` is de scherpe: `V3` zit in beide selecties en telt één keer, dus 4 en niet
5. `J2` is de scherpste: beide selecties zijn dezelfde eenling, dus 1 en niet 2
— een uitvoerder die de deeltellingen optelt valt hier om, ook zonder dat er
iets "gedeeld" oogt. `J3` pint dat een LEGE selectie niets bijdraagt in plaats
van de unie leeg te maken, en `J4` dat de lege unie **0** is en nooit leeg
(ch. 1.2e — een telling is een kardinaliteit).

## Wat er op de wire staat

Een `Aggregation` met `kind: "AANTAL"`, ÉÉN argument — een `InstanceSetUnion`
met twee `InstanceSetBranch`-selecties — en zonder `elementType`, `filter` en
`sourceRole`: de telling noemt geen eigen bron, want de selecties dragen
populatie, relatie en deelselectie elk voor zich (WF-110 (c)).
De unie draagt geen `type`: zij duidt instanties aan, geen waarde, en de
typeproloog van WF-034 zondert haar daarom uit zoals zij een Lambda uitzondert.

De ariteit is ÉÉN. Dat is niet cosmetisch: het is precies wat deze vorm op de
wire onderscheidbaar houdt van de opgesomde WAARDEtelling die WF-037 sinds
2026-09-20 weigert, en waarom die weigering letterlijk kon blijven staan.

## Het tweede geval: hetzelfde relatiefeit tweemaal aangeleverd

Het caseformaat laat een rollenlijst met een herhaald doel toe (geen
`uniqueItems` op de array), en dat is een GELDIGE case: een relatiefeit is een
feit, geen teller. `case2-dubbel-feit.json` pint dat alle vier de uitvoerders
het zo lezen. `J1` krijgt `aangemelde vondsten: [V1, V1, V2]` en `geborgen
vondsten: [V2, V2]` — de selecties zijn {V1, V2} en {V2}, de unie is
{V1, V2} en het antwoord **2**, niet 5. `J2` krijgt `[V3, V3]` en telt **1**,
niet 2.

Het onderscheidt zich van `WV-3.4d-dubbele-tak` in `ref/test/`: die herhaalt
de SELECTIE, deze herhaalt het FEIT. De eerste is een vraag over de unie, de
tweede over de invoerlezing, en pas samen dekken zij "een instantie telt één
keer" aan beide kanten.

## Rood-eerst

Gemeten op de frontend van vóór deze kaart (5b42e5b9): deze `bron.rgs` werd
geweigerd met de WF-037-ariteitsfout en bereikte de schijf niet. Sinds de kaart
emitteert zij byte-identiek aan de gecommitte `ir.json` (poort 10), is het
document verifier-schoon (poort 3) en antwoorden `ref` en clingo dezelfde vier
absolute getallen (poorten 5 en 6).
