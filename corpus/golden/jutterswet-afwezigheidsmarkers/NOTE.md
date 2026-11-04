# jutterswet-afwezigheidsmarkers

The absence markers of the RegelSpraak dialect (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Afwezigheidsmarkeringen"; `spec/RUNTIME-SEMANTICS.md` ch. 9.5) on a scalar attribute and on a timeline. A Vondst
declares

    de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
    de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend of niet van toepassing);

and `ir.json` is the frontend's emission of the source below, which the frontend's suite holds byte for byte
(`AbsenceMarkerTest.the_golden_is_the_emission_of_its_source`). The source is not `source.rgs` here: the route leg
of gate 15 reads every `source.rgs` back through the Drools loader, which refuses this document (below); it becomes
`source.rgs` with the Drools carriage of sum types. Each of the two attributes is a sum type over a domain of
its own, `taxatie van een Vondst` and `opslagkosten van een Vondst`, with the constructors `waarde` (the payload is
the declared datatype), `onbekend` and `niet van toepassing`. A marker written is a `Construct` of the marker, a
number written a `Construct waarde(...)`; a marker tested is an equality with the `Construct`; "een waarde heeft" is a
gevuld test of a `PayloadAccess`, and every read of the value is a `PayloadAccess` under that test (WF-127).

The cases, with ref and clingo:

| case | input | taxatie | vindloon | other |
|---|---|---|---|---|
| 1 | schatting 300,00 | `waarde(300.00)` | 30,00 | not kostbaar |
| 2 | onleesbaar, taxatie `waarde(40.00)` | `onbekend` (the marker displaces the input) | 25,00 | |
| 3 | drijfhout | `niet van toepassing` | 0,00 | |
| 4 | V1 nothing, V2 schatting 100,00 | V1 leeg, V2 `waarde(100.00)` | V1 leeg, V2 10,00 | |
| 5 | taxatie `onbekend` | `onbekend` (the input stands; the rule from the schatting fires with leeg) | 25,00 | |
| 6 | schatting 2000,00 | `waarde(2000.00)` | 200,00 | kostbaar |
| 7 | a Jutter with V1 (schatting 300,00), V2 (onleesbaar), V3 (schatting 100,00) | V2 `onbekend` | | getaxeerd totaal 400,00: the subselection "die een waarde hebben" leaves V2 out |
| 8 | a Jutter with V2 (onleesbaar) only | V2 `onbekend` | | getaxeerd totaal leeg: no member holds a value |
| 9 | opslagkosten `waarde(10)` on 2025 | | | opslagkosten `onbekend` in January and February, `waarde(10)` from March; opslagtoeslag 7 in January and February (a comparison with the marker, read per period) |
| 10 | as 9, verkocht from 2025-06-01 | | | opslagkosten `onbekend`, `waarde(10)` from March to May, `niet van toepassing` from June; opslagtoeslag as in 9 |

A marker that meets a number, and two markers that meet on one day, are the fault of two writers (EF-016): the red
twin `corpus/red/jutterswet-afwezigheidsmarkers-botsing` carries this document with those cases.

The Drools backend refuses the document at load (`EXCLUDE DROOLS-010`, `Datatype:SumType`), so the golden is a
ref = clingo pair until the Drools carriage of sum types.

The source:

    Objecttype de Jutter (mv: Jutters)
        het getaxeerd totaal Numeriek (getal met 2 decimalen);

    Objecttype de Vondst (mv: Vondsten)
        de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
        de schatting Numeriek (getal met 2 decimalen);
        het vindloon Numeriek (getal met 2 decimalen);
        de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend of niet van toepassing);
        de opslagtoeslag Numeriek (geheel getal) voor elke maand;
        is onleesbaar kenmerk (bijvoeglijk);
        is drijfhout kenmerk (bijvoeglijk);
        is kostbaar kenmerk (bijvoeglijk);
        is verkocht kenmerk (bijvoeglijk) voor elke maand;

    Feittype vangst
        de jutter Jutter
        de vondst Vondst
        één jutter heeft meerdere vondsten

    Regel taxatie uit schatting
        geldig altijd
            De taxatie van een Vondst moet gesteld worden op zijn schatting.

    Regel taxatie onbekend
        geldig altijd
            De taxatie van een Vondst moet gesteld worden op onbekend
            indien hij onleesbaar is.

    Regel taxatie niet van toepassing
        geldig altijd
            De taxatie van een Vondst moet gesteld worden op niet van toepassing
            indien hij drijfhout is.

    Regel vindloon naar taxatie
        geldig altijd
            Het vindloon van een Vondst moet berekend worden als zijn taxatie gedeeld door 10 rekenkundig afgerond op 2 decimalen
            indien zijn taxatie een waarde heeft.

    Regel forfaitair vindloon
        geldig altijd
            Het vindloon van een Vondst moet gesteld worden op 25,00
            indien zijn taxatie onbekend is.

    Regel geen vindloon
        geldig altijd
            Het vindloon van een Vondst moet gesteld worden op 0,00
            indien zijn taxatie niet van toepassing is.

    Regel kostbare vondst
        geldig altijd
            Een Vondst is kostbaar
            indien hij aan alle volgende voorwaarden voldoet:
            - zijn taxatie heeft een waarde
            - zijn taxatie is groter dan 1000,00.

    Regel getaxeerd totaal
        geldig altijd
            Het getaxeerd totaal van een Jutter moet berekend worden als de som van de taxatie van alle vondsten van de Jutter die een waarde hebben.

    Regel opslag onbekend in de winter
        geldig altijd
            De opslagkosten van een Vondst moet gesteld worden op onbekend van dd. 01-01-2025 tot dd. 01-03-2025.

    Regel opslag niet van toepassing na verkoop
        geldig altijd
            De opslagkosten van een Vondst moet gesteld worden op niet van toepassing gedurende de tijd dat hij verkocht is.

    Regel opslagtoeslag
        geldig altijd
            De opslagtoeslag van een Vondst moet gesteld worden op 7 gedurende de tijd dat zijn opslagkosten onbekend zijn.
