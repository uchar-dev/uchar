# clingo crasht op een symbolische term in de schaal-deling — nette fault

Goal: de gelede EERSTE/LAATSTE over een SumType-element laat clingo
hard crashen: `Fatal error: exception Division_by_zero`, exitcode 2,
zonder file/line — geen fault, een crash. Gemeten (meetfase
doorgeeffold, 2026-09-04): `backends/clingo/lib/translate.ml` deelt in
de gelede-fold-arm (~r. 1485) `m / s` waar `s = 0`, de schaal die
`expr_term` (~r. 114-120) aan een constructorterm geeft; de
`is_symbolic_operand`-fence op dezelfde arm (~r. 1456) dekt Tekst,
Booleaan en Enumeratie maar niet SumType. Fail-fast betekent een
reden-dragende `TRANSLATION FAULT`, nooit een ongevangen exceptie: de
fence leert SumType kennen (of de schaal-nul wordt zelf de luide
weigering), en de kaart loopt na of méér plekken een symbolische term
door een numerieke schaal-deling laten lopen (grep op de
schaal-delingen in translate.ml; elke vondst zelfde behandeling of een
reden in commentaar).

Onder het orderbaarheidsbesluit van 2026-09-04
(`20260902-doorgeeffold-orderbaarheid.md`) wordt de dragende vorm
ill-formed, maar een crash-pad hoort ook onbereikbaar-net te zijn: de
translator mag op geen enkele invoer een ongevangen exceptie gooien.

Acceptance:
- Rood-eerst: de crash gereproduceerd (letterlijke uitvoer in de
  kaart); ná de fix een reden-dragende TRANSLATION FAULT op dezelfde
  invoer.
- De sweep over de schaal-delingen met per plek fence of reden.
- Alle dertien gates groen; geen bestaande pin beweegt (gemeten
  verwachting: geen corpusdocument raakt de arm).

May touch: `backends/clingo/lib/**`, `backlog/`.

Out of scope: de orderbaarheidsclausule zelf (eigen kaart), ref,
frontends, corpus.
