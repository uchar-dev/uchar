# rood-fragment-catchall-tripwire: een pin mag geen bodemzin citeren

Gemeld door de som-catchall-kaart (2026-09-19, "Gemeld, niet gekaart"):
de kaart genas een rode case die de catch-all-bodemzin van de
ASP-vertaling als `expectedFault`-fragment pinde — waarmee de
geen-corpusgeval-claim in die zin live onwaar was. Niets verhindert de
recidive: een toekomstige rode case kan opnieuw een catch-all-zin
pinnen, en niemand merkt het tot een lens erover struikelt.

De meting die de kaart meegaf: een wegwerp-scan van alle 29 rode
fragmenten tegen de vier bekende catch-all-zinnen van `ref` en clingo
geeft op de huidige stand NUL treffers — en had er vóór die kaart
precies één gegeven. De tripwire is dus vandaag goedkoop en had het
defect gevangen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de catch-all-/bodemzinnen van beide uitvoerders
  (de zinnen die "niet vertaald/niet gedragen" zeggen zonder de vorm
  te noemen) en leg ze als lijst vast op één plek.
- (b) Een check — in `scripts/check-rood.sh` (voorstel, beschermd pad)
  of in de suite van de backend — die faalt wanneer een
  `expectedFault`-fragment een substring van zo'n bodemzin is, met de
  naam van de rode case en de zin in de foutmelding.
- (c) Mutatieproef: een tijdelijke rode pin op een bodemzin laat de
  check falen; teruggedraaid. Alle veertien gates groen.

May touch: `scripts/check-rood.sh` (voorstel) of `backends/*/test/**`,
kaartverplaatsing. NB: raakt geen kanaallijsten; parallelveilig naast
DrlTranspiler-kaarten zolang niemand check-rood.sh anders raakt.

## Result (2026-09-19, branch `card/catchall-tripwire`, van a0d5fe15)

Het hekje staat, het vuurt in beide richtingen, en de historische treffer
is nagemeten in plaats van overgenomen.

### (a) De bodemzinnen: dertien, niet vier

De kaart gaf "vier bekende" mee; de inventarisatie over `ref/lib` en
`backends/clingo/lib` vond er dertien. De lijst staat in
`scripts/check-rood.sh`, één regel per zin, met bron (bestand + functie)
en de reden waarom juist deze zin geen vorm noemt — zodat een volgende
zin bewust wordt toegevoegd en niet per ongeluk.

`ref/lib/eval.ml`: `literal of this datatype not implemented`
(`literal_value`, de restarm over datatypes — hij interpoleert het
lexeem, nooit welk datatype geen lezer heeft); `expression kind not
implemented in this card` en `unsupported temporal-read basis shape`
(beide `eval_expr`); `unsupported instance-fold source shape` en
`function kind not implemented in this card` (beide `eval_functie`);
`predicate kind not implemented in this card` en `condition kind not
implemented in this card` (beide `eval_cond`); `unsupported target
shape` (`run_rule`, de zin zet de REGELnaam ervoor, nooit de vorm).

`backends/clingo/lib/translate.ml`: `unsupported temporal-read basis
shape` en `expression kind not translated (no corpus case exercises
it)` (beide `expr_term` — de tweede is het defect van vandaag zelf);
`condition kind not translated (no corpus case exercises it)`
(`cond_atoms`); `rule kind not translated` en `unsupported rule target`
(beide `translate_regel`).

De afgrenzing is streng gehouden en dat is in het script vastgelegd. Een
weigering die de vorm WEL noemt is geen bodemzin en blijft vrij pinbaar:
`a filter on a listed-operand fold has no v0 form` en `unsupported
AttribuutRef shape (set/sentinel outside a fold)` noemen allebei wat ze
weigeren. De welgevormdheidsmeldingen (`tijdsduur expects two
arguments`) evenmin: die noemen een geschonden verwachting, geen
onbereikte capaciteit. `unsupported case format %S` in `ref/lib/case.ml`
is ook buiten de lijst gebleven — die noemt het aangetroffen formaat en
gaat niet over een IR-vorm.

De lijst draagt van elke zin het INTERPOLATIEVRIJE deel: meer kan een
gepind fragment nooit raken, want de staart (een regelnaam, een lexeem)
verschilt per document.

### (b) De plek: gate 8, met reden

`scripts/check-rood.sh`, en niet een suite van een backend. De reden is
blikveld: de zinnen van `ref` en die van clingo liggen alleen hier in één
blikveld. Een clingo-suite ziet de bodemarmen van `ref` nooit en
andersom, dus in een suite zou het hekje per uitvoerder half zijn. Gate 8
bezit bovendien precies de bestandenlijst waar het over gaat
(`corpus/rood/**/case*.json`, al opgebouwd) en las de fragmenten al voor
het Drools-been.

De fragmenten worden zonder JSON-parser gelezen, met een `awk` over het
`expectedFault`-blok. Dat is een besluit en staat als zodanig in het
commentaar: de blokkerende ref/clingo-benen hebben vandaag genoeg aan
`dune` en `clingo`, en een tekstcontrole hoort daar geen werktuig aan toe
te voegen (`python3` is in dit script tot nu toe alleen een
voorwaarde van het overslaanbare Drools-been). De extractie is
gecontroleerd tegen een echte JSON-parser: over alle 29 rode cases en
alle drie de sleutels — 87 lezingen, inclusief de fragmenten met
ge-escapete aanhalingstekens — leveren `awk` en `json.load` byte-voor-byte
hetzelfde. En een case waaruit `ref` of `clingo` niet leesbaar is, FAALT;
een stille lege extractie kan het hekje dus niet uithollen.

Beide richtingen tellen als treffer, zoals de opdracht vroeg: fragment
binnen zin (de vorm van het som-defect) én zin binnen fragment (een pin
die een bodemzin heel citeert met een staart eraan). De vergelijking
gebruikt `case ... in *"$frag"*` met het fragment tussen aanhalingstekens,
dus een `*` of `[` in een toekomstig fragment is letterlijk en geen glob.

Het bereik is `ref` + clingo. Het Drools-been noemt zijn niet-gedragen
vormen bij NAAM in zijn uitsluitingslijst in plaats van via een fragment,
dus daar valt geen bodemzin te citeren.

Geen nieuwe `EXPECTED_MIN`-vloer, zoals de kaart voorschreef; de PASS-regel
meldt wel beide getallen (58 fragmenten, 13 zinnen), dus krimp is zichtbaar
in de uitvoer.

### (c) Mutatieproef, nulmeting, historische treffer

NULMETING op de huidige stand: `check-rood: PASS (29 red cases fault
observably in ref and clingo, floor 26; 58 pinned fragments cite none of
the 13 listed bottom sentences)`. Nul treffers.

MUTATIEPROEF A — de historische pin teruggezet
(`som-fallback-always-evaluated`, clingo weer `"expression kind not
translated"`). Gate 8 faalt bij naam, met de zin erbij: *"som-fallback-
always-evaluated pins expectedFault.clingo = "expression kind not
translated", which cites the bottom sentence "expression kind not
translated (no corpus case exercises it)" (backends/clingo/lib/translate.ml
expr_term — …)"*. Het runnerbeen faalt daar vandaag naast, omdat de
som-kaart de live zin veranderd heeft; dat is het verschil met de
historische stand en het is hieronder apart gemeten. Teruggedraaid.

MUTATIEPROEF B — de andere richting
(`division-by-numeric-zero`, `ref` := `rule "vindersloon": unsupported
target shape at ch. 5.2`, een fragment dat een bodemzin VOLLEDIG bevat).
Het hekje vuurt en noemt `unsupported target shape` met `ref/lib/eval.ml
run_rule` erbij. Teruggedraaid. `git status` daarna schoon op `corpus/`.

HISTORISCHE TREFFER, nagemeten en niet overgenomen. Op `f4e8e6f4^` — de
stand vóór de som-kaart — bestaan alle dertien zinnen woordelijk (elk met
`grep -F` gecontroleerd tegen de bestanden van die commit), en over de rode
zone van diezelfde commit geeft dezelfde hekjeslogica: 29 rode cases, 58
fragmenten, ÉÉN treffer, `som-fallback-always-evaluated`. Daar waren de
ref/clingo-benen groen, dus het hekje zou er de enige melder geweest zijn.
De "precies één" uit de kaart klopt dus, en nu als meting.

### Bekende zwakte, en wat er bewust niet gebouwd is

Een bodemzin die in `ref` of clingo wordt herschreven, verplaatst of
toegevoegd zonder dat deze lijst meebeweegt, laat het hekje blind voor
precies die zin. Dat staat in één zin in het commentaar en er is geen
tweede mechaniek omheen gebouwd — conform de opdracht. Voor de
maintainer, als hij het later toch wil: een `grep -F -q` van elke
lijstregel tegen zijn bronbestand sluit de HELFT ervan (herschrijven en
verplaatsen wel, een nieuwe bodemarm niet) en kost vier regels. Ik heb het
niet gedaan omdat de opdracht het expliciet afwees.

### Gates

Alle veertien los gedraaid, alle veertien groen: 1 PASS; 2 PASS (239
bestanden, vloer 192); 3 PASS (239 golden schoon, 398 negatief, vloer
350); 4 PASS (572 casusbestanden); 5 PASS (ref-suite 643, vloer 489; 543
cases, vloer 427); 6 PASS (backendsuite 423, vloer 83; 543 cases); 7 PASS
(507 cases, 36 overlevers); 8 PASS (29 rode cases, vloer 26; 58 fragmenten
tegen 13 zinnen; Drools-been 6 fault-pinned, 23 met reden); 9 PASS (201
checks, 12 goldens); 10 PASS (322 tests, 58 goldens); 11 PASS (ref 29 /
clingo 29 / Drools 29 PLAYED); 12 PASS (exec 159, DRL-tekst 132, JUnit
478); 13 PASS (126 twists over 23 goldens, 104 replays); 14 PASS (130
checks, 2 goldens).

### Voorstel voor de hoofdsessie

`scripts/check-rood.sh` is een beschermd pad: de diff is een VOORSTEL.

De gate-8-beschrijving in `CLAUDE.md` mist het nieuwe been (deze kaart
raakt dat bestand niet). Voorgestelde toevoeging aan het eind van punt 8,
na "…never silently green":

> ; and no pinned fragment cites a BOTTOM SENTENCE of `ref` or clingo —
> the refusal a node-kind match falls through to, which names no form.
> Such a pin makes the sentence's own no-corpus-case claim untrue and
> witnesses nothing of its own, since any other uncarried form satisfies
> it; the thirteen sentences are listed in the script with source and
> reason, and the cure is a named arm for the form, never a reworded
> bottom.
