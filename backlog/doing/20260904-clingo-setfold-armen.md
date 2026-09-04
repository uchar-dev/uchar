# De clingo-vouwarmen vernauwen: één opruiming voor het verkeerd-object-lek

Goal: De filterloze temporele `Totaal`-arm van
`backends/clingo/lib/translate.ml` (~:1682) matcht te ruim. Hij eist
**niet** `navigatie_rol = None`, **niet** `navigatie_pad = []`, en
**niet** de ci-controle `norm aot = norm subject_ot` die zijn zusterarm
met de `gedurende`-clip (~:1473) wél draagt — terwijl hij vervolgens
`val(<subject>, …)` emitteert. Op een **genavigeerde** temporele totaal
leest hij daardoor het attribuut van het VERKEERDE object. Gemeten
(2026-09-04, eigen bron in `/tmp`, jaargranulariteit; `Vondst.bewaarlast
= het totaal van het bewaartarief van de bewarende jutter`, J1 = 10,
V1 = 99):

```
met naamdeling (Jutter én Vondst dragen 'bewaartarief'):
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast = [99], expected 10      <- het VERKEERDE object

zonder naamdeling (Vondst draagt 'vondsttarief'):
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast is underived (leeg), expected 10
```

Twee stille antwoorden waar de andere executor luid weigert: het ene
fout, het andere leeg. Pre-existent — ouder dan de
`ref-tijdtotaal`-kaart, die de arm alleen een `set`-guard gaf.

**Rood-eerst, gereproduceerd bij de claim van deze kaart** (eigen bron
`/tmp/setfold/{a-naamdeling,b-geen-naamdeling}.rgs`, door gate 10's
frontend naar IR geëmitteerd; één Jutter J1 met `bewaartarief` = 10 op
het penningjaar, één Vondst V1 met 99, `Eén bewarende jutter bewaart
meerdere vondsten`, verwachting `V1.bewaarlast = 10`):

```
variant a (Jutter EN Vondst dragen 'bewaartarief')
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast = [99], expected 10
variant b (Vondst draagt 'vondsttarief')
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast is underived (leeg), expected 10
```

Woord voor woord de meting uit de kaartkop — de vorm is stabiel en de
wire-vorm die hem draagt is `Functie{kind: TOTAAL}` met een operand
`AttribuutRef{objecttype: "Jutter", navigatieRol: "bewarende jutter",
set: false}` onder subject `Vondst`.

Dezelfde klasse zit op de armen die de fixronde van die kaart nu
temporeel bewaakt: de `Som`-arm (~:1293) en de `Minimum`/`Maximum`-arm
(~:1358) matchen `navigatie_rol = None, navigatie_pad = []` wél, maar
leiden hun rol af uit `subject_ot`/`el` zonder de ci-controle die de
`gedurende`-arm draagt. Eén kaart, één vernauwingspatroon, alle armen
langs.

Acceptance:
- Rood eerst: de twee metingen hierboven staan als **vóór**-regel in de
  kaart; ná de vernauwing weigert clingo beide vormen luid met een
  reden-dragende melding, of vertaalt ze correct (10). Nooit meer stil
  fout, nooit meer stil leeg.
- De `Totaal`-arm draagt `navigatie_rol = None`, `navigatie_pad = []` en
  `norm aot = norm subject_ot` — letterlijk de vernauwing van de
  `gedurende`-zusterarm, zodat de twee armen op dezelfde vorm matchen.
- Elke vouwarm met een `set`- of objecttype-dragend operand is
  nagelopen op dezelfde drie punten; wat niet vernauwd wordt draagt een
  reden in commentaar.
- Getuige: een golden of, waar poort 6 geen weigerkanaal heeft, een
  `ref/test`- of `backends/clingo/test`-pin die de vorm vastlegt.
- Alle dertien gates groen; geen bestaande pin beweegt (gemeten
  verwachting: 0 — geen corpus- of toka-golden draagt een genavigeerde
  temporele totaal).

May touch: `backends/clingo/**`, `corpus/golden/**` (nieuwe getuige),
`backlog/`.

Out of scope: de §7.1 set-vouw écht vertalen (`Totaal`, `Som`,
`Minimum`/`Maximum` over een temporeel attribuut) — dat is een eigen
kaart en de armen weigeren die vorm sinds 2026-09-04 luid; `ref/`;
`toka/`; de frontends.
