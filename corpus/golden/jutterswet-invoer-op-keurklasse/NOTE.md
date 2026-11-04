# jutterswet-invoer-op-keurklasse — case input on a computed enumeration and text target

"De staat van een Vondst moet gesteld worden op zijn herkeuring indien hij gemeld is" writes an enumeration
(Keurklasse), "De opmerking van een Vondst moet gesteld worden op zijn notitie indien hij gemeld is" a text. The case
input of each target stands in the stack of its writers as a numeric input does (RUNTIME-SEMANTICS ch. 7.8): a rule
displaces it where it decides, and a rule that fires with a leeg source decides nothing (ch. 3.11).

| instance | input | `staat` | `opmerking` |
|---|---|---|---|
| V1 gemeld, herkeuring 'beschadigd', notitie "nieuw" | staat 'gaaf', opmerking "oud" | **'beschadigd'** | **"nieuw"** |
| V2 gemeld | staat 'gaaf', opmerking "oud" | **'gaaf'** | **"oud"** |
| V3 not gemeld, herkeuring 'beschadigd', notitie "nieuw" | staat 'gaaf', opmerking "oud" | **'gaaf'** | **"oud"** |
| V4 gemeld, herkeuring 'beschadigd', notitie "nieuw" | — | **'beschadigd'** | **"nieuw"** |
