# jutterswet-afwezigheidsmarkers

The absence markers of the RegelSpraak dialect (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Afwezigheidsmarkeringen"; `spec/RUNTIME-SEMANTICS.md` ch. 9.5) on a scalar attribute and on a timeline. A Vondst
declares

    de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
    de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend of niet van toepassing);

and `ir.json` is the frontend's emission of `source.rgs` (gate 10). Each of the two attributes is a sum type over a
domain of
its own, `taxatie van een Vondst` and `opslagkosten van een Vondst`, with the constructors `waarde` (the payload is
the declared datatype), `onbekend` and `niet van toepassing`. A marker written is a `Construct` of the marker, a
number written a `Construct waarde(...)`; a marker tested is an equality with the `Construct`; "een waarde heeft" is a
gevuld test of a `PayloadAccess`, and every read of the value is a `PayloadAccess` under that test (WF-127).

The cases, with ref, clingo and both branches of the Drools backend:

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
