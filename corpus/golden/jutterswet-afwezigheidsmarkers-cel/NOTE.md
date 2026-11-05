# jutterswet-afwezigheidsmarkers-cel

The absence markers of the RegelSpraak dialect (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Afwezigheidsmarkeringen"; `spec/RUNTIME-SEMANTICS.md` ch. 9.5) on a cell of a dimensioned attribute (ch. 5.8c). A
Vondst declares

    het gewicht Numeriek (getal met 2 decimalen) gedimensioneerd met seizoen (of onbekend);

over the dimension `seizoen` (voorjaar, najaar), and `ir.json` is the frontend's emission of `source.rgs` (gate 10).
The attribute is of the sum type over the domain `gewicht van een Vondst`, with the constructors `waarde` (payload: the
declared number) and `onbekend`, per cell. A rule writes the number of the schatting into the cell in najaar
(`Construct waarde(...)`) and the marker onbekend into the cell in voorjaar of an onleesbare Vondst (`Construct
onbekend`); "zijn gewicht in najaar een waarde heeft" is a gevuld test of the `PayloadAccess` of that cell, under which
the najaarsloon reads it, and "zijn gewicht in voorjaar onbekend is" an equality of that cell with `Construct
onbekend`. The test of a cell guards the read of the same cell (WF-127).

The case files are the lowering of `scenario.rgs` (gate 16), one scenario each. With ref, clingo and both branches of
the Drools backend:

| case | input | gewicht in najaar | gewicht in voorjaar | other |
|---|---|---|---|---|
| 1 | schatting 60,00 | `waarde(60.00)` | leeg | najaarsloon 120,00, voorjaarstoeslag leeg, not zwaar |
| 2 | onleesbaar, gewicht in voorjaar `waarde(10.00)` | | `onbekend` (the marker displaces the input) | voorjaarstoeslag 7,00, najaarsloon leeg |
| 3 | V1 schatting 150,00, V2 nothing | V1 `waarde(150.00)`, V2 leeg | | V1 najaarsloon 300,00 and zwaar; V2 najaarsloon leeg, not zwaar |
| 4 | gewicht in najaar and in voorjaar `onbekend` | `onbekend` (the input stands; the rule from the schatting fires with leeg) | `onbekend` | najaarsloon leeg, not zwaar, voorjaarstoeslag 7,00 |
