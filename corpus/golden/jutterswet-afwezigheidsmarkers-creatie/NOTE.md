# jutterswet-afwezigheidsmarkers-creatie

The absence markers of the RegelSpraak dialect (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Afwezigheidsmarkeringen"; `spec/RUNTIME-SEMANTICS.md` ch. 9.5) in an object creation, and a number written into an
attribute with markers. A Vondst declares

    de taxatie Numeriek (getal met 2 decimalen) (of onbekend);

and `ir.json` is the frontend's emission of `source.rgs` (gate 10). A Jutter that is actief makes a vondst "met taxatie
gelijk aan onbekend", the `Construct onbekend` as the member's value, and one that is ervaren a vondst "met taxatie
gelijk aan 12,50", the `Construct waarde(12.50)`; a drijfhout Vondst has its taxatie written by "… moet gesteld worden
op 5,00", the `Construct waarde(5.00)`. The two literals are typed by their values, 5,00 as a whole number and 12,50
with one decimal, and are arguments of the payload of 2 decimals: WF-062 reads the argument of a `Construct` as a
write into the payload (`spec/IR-SPEC.md` ch. 4.7). A Vondst whose taxatie is onbekend is ongetaxeerd; the Jutter counts
its ongetaxeerde vondsten and sums the taxatie of its vondsten that hold a value.

The case files are the lowering of `scenario.rgs` (gate 16), one scenario each. With ref, clingo and both branches of
the Drools backend:

| case | input | taxatie | aantal ongetaxeerde vondsten of J1 | getaxeerd totaal of J1 |
|---|---|---|---|---|
| 1 | J1 actief | the made vondst `onbekend` | 1 | leeg: no vondst holds a value |
| 2 | J1 ervaren | the made vondst `waarde(12.50)` | 0 | 12,50 |
| 3 | J1 actief and ervaren, V1 drijfhout of J1 | V1 `waarde(5.00)`, the made vondsten `onbekend` and `waarde(12.50)` | 1 | 17,50 |
| 4 | V1 drijfhout and V2, both of J1 with the taxatie `onbekend` | V1 `waarde(5.00)` (the rule displaces the input), V2 `onbekend` | 1 | 5,00 |
