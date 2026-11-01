# jutterswet-deling-in-afwijking-omgekeerd — a division in a default of a declared group

RUNTIME-SEMANTICS ch. 7.1: a default deposits only where it decides, and ch.
5.7: a default that is not reached is not evaluated. Five groups on a Vondst.
The rules stand in the reverse order of `jutterswet-deling-in-afwijking`, with the same expectations.

- `verhouding`: base 1,00; deviation gewogen gewicht gedeeld door geschatte
  gewicht, where the gewogen gewicht is gevuld.
- `toeslag`: base 1,00; deviation (toeslaggrond gedeeld door geschatte gewicht)
  plus 5,00, where the toeslaggrond is gevuld; below it the exception 2,00 where
  the Vondst is gemerkt.
- `korting`: base 1,00; deviation 7,00 where kortinggrond gedeeld door geschatte
  gewicht leeg is.
- `verlaging`: base 3,00 where toeslaggrond gedeeld door geschatte gewicht leeg
  is; deviation 4,00 where the Vondst is gemerkt.
- `premie`: base 1,00; deviation 9,00 where premiegrond gedeeld door geschatte
  gewicht groter is dan 1.

| case | input | outcome (the rest as the bases) | channel |
|---|---|---|---|
| case1-afwijking-beslist | V1 geschat 320, gewogen 240, toeslaggrond 240, kortinggrond 240, premiegrond 640 | verhouding **0,75**, toeslag **5,75**, premie **9,00**, verlaging **leeg** | consistent |
| case2-nul-valt-door | V2 geschat 0, gewogen 60 | verhouding **1,00** | consistent |
| case3-leeg-valt-door | V3 gewogen 60 | verhouding **1,00** | consistent |
| case4-beslist-met-nul | V4 geschat 0, toeslaggrond 60 | toeslag **5,00**, verlaging **3,00** | (Vondst.toeslag × V4) and (Vondst.verlaging × V4), EF-005 |
| case5-hogere-verdringt | V5 gemerkt, geschat 0, toeslaggrond 60 | toeslag **2,00**, verlaging **4,00** | consistent |
| case6-niet-exact-onder-hogere | V6 gemerkt, geschat 30, toeslaggrond 100 | toeslag **2,00**, verlaging **4,00** | consistent |
| case7-voorwaarde-met-nul | V7 geschat 0, kortinggrond 60 | korting **7,00** | (Vondst.korting × V7), EF-005 |
| case8-voorwaarde-met-leeg | V8 kortinggrond 60 | korting **7,00** | (Vondst.korting × V8), EF-031 |
| case9-onwaar-met-nul | V9 geschat 0, premiegrond 60 | premie **1,00** | consistent |
| case10-niets-te-delen | V10 geschat 20 | the bases | consistent |

Over a zero or a leeg divisor the consequence of `verhouding` is leeg, so the
deviation falls through and deposits nothing (case 2, 3). The consequence of
`toeslag` is filled there, so it decides with its deposit (case 4). The
justification of `korting` holds on the leeg quotient, so it decides with its
deposit (case 7, 8); that of `premie` does not hold, so it deposits nothing (case
9). A default that a higher one displaces is not evaluated, its division or its
100/30 included (case 5, 6). Where 100/30 plus 5,00 stands, the session faults:
`corpus/red/jutterswet-afwijking-past-niet`.
