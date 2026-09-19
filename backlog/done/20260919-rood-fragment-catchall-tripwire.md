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

Het hekje staat in gate 8, de inventaris telt zesendertig zinnen, en de
vergelijking draait in OCaml omdat ze in de shell omzeilbaar bleek. Deze
tekst is de tweede doorgang: de lensronde vond twee blokkerende gebreken
in de eerste, en beide zijn verholpen in plaats van weggeschreven.

### (a) De bodemzinnen: zesendertig

De kaart gaf "vier bekende" mee, de eerste doorgang vond er dertien, en die
dertien misten een hele familie. De sweep is overgedaan over elke
wildcard-/restarm in `ref/lib` en `backends/clingo/lib` die een weigering
produceert; de uitkomst is zesendertig zinnen, elk met bestand, functie en
reden in `scripts/check-rood.sh`.

Wat de eerste doorgang miste was niet een enkele arm maar een hele
formulering. Naast de armen die zeggen "onbekende soort" ("expression kind
not translated", "unsupported rule target") staat een tweede familie die
opsomt wat de uitvoerder WEL draagt: "a list source here is an own
list-typed attribute, a list parameter, a MapList or a FilterList (no
corpus case for more)", "a temporal target takes only de ch. 3.7 pro-rata
of ch. 3.8 day-count shape", "a Construct payload other than a literal",
"Carried: …". Informatief is dat hetzelfde gebrek: de opsomming is de stand
van de implementatie en beweegt met elke kaart mee, dus de zin zegt niets
stabiels over de vorm van de getuige die hem uitlokte. De
gedurende-arm in beide uitvoerders (`ref/lib/eval.ml` r. 1310,
`backends/clingo/lib/translate.ml` r. 2662) en de temporele-doelarm aan
beide kanten (r. 1984 en r. 6250) horen tot die familie en stonden
onterecht buiten de lijst.

Daarom is het criterium herschreven en staat het nu voluit in het script:
een bodemzin is de weigering van een doorvalarm die INVARIANT is over alles
wat die arm vangt — ze beschrijft de positie en wat daar wél gedragen wordt,
nooit het geweigerde construct. Daartegenover blijft vrij pinbaar elke
weigering die het geweigerde construct wél aanwijst, of ze het nu noemt
("a ListLiteral list source is not translated", "allocation validity
windows are not translated"), interpoleert ("the fold source %S is not a
list-typed own attribute") of de ene eigenschap noemt die het definieert
("non-numeric arithmetic operand", "negative exponent", "som fallback other
than 0"). Welgevormdheidsmeldingen ("tijdsduur expects two arguments")
vallen buiten de vraag: die noemen een geschonden verwachting en hun arm
vangt één ding.

De verdeling: twaalf zinnen in `ref/lib/eval.ml` (`literal_value`,
`eval_expr` ×2, `eval_functie` ×3, `eval_cond` ×3, `run_pro_rata` ×3,
`run_rule`) en vierentwintig in `backends/clingo/lib/translate.ml`
(`symbolic_term`, `expr_term` ×4, `dt_term`, `whole_period_fails`,
`cond_atoms`, `construct_ground_term`, `sum_term`, `struct_base`,
`scrutinee_pattern`, `list_source`, `body_condition`, `closed_term` ×2,
`sum_operand`, `filter_condition`, `closed_division`, `value_condition`,
`translate_regel` ×3, `bool_atoms`). Alle zesendertig zijn woordelijk
tegen hun bronbestand gecontroleerd, met de OCaml-regelvoortzettingen en de
`\x`-escapes gedecodeerd, dus de lijst draagt de UITGESTOTEN tekst en niet
de broncode-spelling. Twee entries dragen het interpolatievrije deel
(`literal_value` en `run_rule`): hun staart is een lexeem of een regelnaam
en kan door geen pin geraakt worden.

### (b) De plek: gate 8; de lezing: `ref --bodemzin`

De plek blijft `scripts/check-rood.sh` (beschermd pad, dus VOORSTEL), om de
reden van de eerste doorgang: de zinnen van `ref` en die van clingo liggen
alleen hier in één blikveld, een backendsuite ziet de bodemarmen van de
ander nooit, en gate 8 bezit al de bestandenlijst waar het over gaat.

De LEZING is wel verhuisd, en dat was blokkerend. De eerste doorgang las de
fragmenten met een `awk` over het `expectedFault`-blok, en dat is
omzeilbaar: een schema-geldige pin mag elk teken als JSON-escape spellen.
Nagemeten op de echte rode casus — `"expression kind not translated"`
decodeert naar precies de bodemzin, een tekstzoektocht in het bestand
vindt nul treffers, en het oude hekje liet de pin dus door. Bovendien gaf
een `expectedFault`-object op één regel een lege extractie, wat op een
geldige casus vals alarm zou zijn; de bewering in het oude commentaar dat
gate 4 een sleutel-per-regel-layout vastpint was gewoon onwaar — die gate
valideert structuur, geen opmaak.

Gekozen route, de smalste van de twee die de lens aanbood: een eigen vlag
op de BESTAANDE rode runner, `ref/bin/runner.ml --bodemzin <zinnen>
<case.json>`. Geen nieuw bestand, geen nieuwe dune-stanza, geen nieuwe
bibliotheek — de mode hergebruikt `Case.expected_fault_of_file`, precies de
lezer waarmee het rode pad de pin al vóór de wereld uitleest, en
`Case.contains`, precies de substringtest waarmee de runner een
faultrapport tegen die pin houdt. Het hekje toetst daarmee dezelfde relatie
die de pin bij replay heeft, en niet een shell-benadering ervan. De mode
raakt het bestaande argumentenpad niet: `--noop` en de drie-argumentvorm
staan ongewijzigd, en de vlag is vier argumenten lang.

Uitvoer is een contract: `PINS <n>`, daarna per citaat `HIT <key> <regel>
<fragment>`, het fragment `%S`-geschreven zodat geen stuurteken de regel
kan breken die de gate terugleest. Een casus zonder leesbare
`expectedFault` eindigt op exit 2 en laat de gate falen; de lijst blijft in
het script, want daar hoort de reden-dragende data.

Over de afhankelijkheden nu de waarheid in plaats van de eerste, te
stellige claim: de JSON-decodering draait in `ref`, dat deze gate toch al
bouwt en draait, en de shellkant gebruikt alleen POSIX-tekstverwerking (een
leeslus en één `awk`-regelopzoeking). Er komt geen bouw- of
installatievereiste bij.

### (c) Proeven: nulmeting, drie mutaties, de historische treffer

NULMETING: `check-rood: PASS (29 red cases fault observably in ref and
clingo, floor 26; 58 pinned fragments cite none of the 36 listed bottom
sentences)`. Nul treffers.

MUTATIE A — de historische pin letterlijk terug
(`som-fallback-always-evaluated`, clingo weer `"expression kind not
translated"`). Gate 8 faalt bij naam en citeert regel 17 van de lijst met
bron en reden. Teruggedraaid.

MUTATIE A2 — dezelfde pin, maar met de eerste letter als JSON-escape
(`"\u0065xpression kind not translated"`, schema-geldig). Dit is de
mutatie die het oude awk-hekje liet passeren; nu vuurt hij, met dezelfde
melding. Naast elkaar gemeten: `json.load` leest hem als de bodemzin,
`grep -c` in hetzelfde bestand telt nul. Teruggedraaid.

MUTATIE B — de andere richting (`division-by-numeric-zero`, `ref` :=
`rule "vindersloon": unsupported target shape at ch. 5.2`, een fragment dat
een bodemzin VOLLEDIG bevat). Vuurt, citeert regel 12. Teruggedraaid.
`git status` op `corpus/` daarna schoon.

VALS-ALARMPROEF: een `expectedFault` als object op één regel — de vorm
waarop de oude extractor leeg terugkwam — leest nu gewoon (`PINS 2`, geen
HIT, exit 0).

BLIJVENDE ZELFPROEF in het script: een synthetisch paar (één zin, één pin
die haar `\u`-gespeld citeert) moet precies één HIT op `ref` geven. Zo niet,
dan faalt gate 8 met de mededeling dat het hekje escapes vergelijkt in
plaats van tekens en dat elke pin die het vrijpleit om de verkeerde reden
is vrijgepleit. Het paar is zelfstandig: het toetst het mechanisme, niet de
inventaris, dus het herformuleren van een echte bodemzin raakt het niet.

HISTORISCHE TREFFER, nagemeten met de volledige lijst van zesendertig. Op
`f4e8e6f4^` bestaan alle zesendertig zinnen woordelijk (36 van 36, nul
afwezig), en dezelfde runner over de rode zone van die commit geeft: 29
rode cases, 58 fragmenten, ÉÉN citaat —
`som-fallback-always-evaluated`, regel 17. Daar waren de ref/clingo-benen
groen, dus het hekje zou de enige melder geweest zijn. De "precies één"
uit de kaart is daarmee een meting, en hij houdt stand nu de lijst bijna
drie keer zo lang is.

### Korte generieke pins: bewust ook geweerd

De richting fragment-binnen-zin vuurt ook op een pin die zo kort is dat hij
toevallig in een bodemzin past — `"not translated"` — ook al beschrijft die
pin bij replay een weigering die de vorm wél noemt. Dat is gekozen en staat
als zodanig in het commentaar: het is een eis aan de pin, geen ongeluk. Een
fragment van die lengte wijst zijn eigen getuige evenmin aan, en het rode
contract is nu juist dat een pin noemt waarvan hij getuigt. De smallere
variant (alleen signatuur-binnen-fragment) zou die pins doorlaten en
daarmee de tweede helft van het defect openlaten.

### Bekende zwakte, en wat er bewust niet gebouwd is

Een bodemzin die in `ref` of clingo wordt herschreven, verplaatst of
toegevoegd zonder dat de lijst meebeweegt, laat het hekje blind voor
precies die zin. Dat staat in één zin in het commentaar; er is geen tweede
mechaniek omheen gebouwd, conform de opdracht. Voor de maintainer, als hij
het later toch wil: een aanwezigheidscontrole van elke lijstregel tegen
zijn bronbestand sluit de helft ervan (herschrijven en verplaatsen wel, een
nieuwe bodemarm niet) en kost vier regels — met de kanttekening dat zes
zinnen over meerdere bronregels lopen, dus zo'n controle moet de
OCaml-regelvoortzettingen eerst weggommen, precies zoals de verificatie in
deze kaart deed.

### Gates

Alle veertien los gedraaid ná de wijziging aan `ref/bin/runner.ml` (die
raakt gate 5, 7, 8 en 13), alle veertien groen: 1 PASS; 2 PASS (239
bestanden, vloer 192); 3 PASS (239 golden schoon, 398 negatief, vloer
350); 4 PASS (572 casusbestanden); 5 PASS (ref-suite 643, vloer 489; 543
cases, vloer 427); 6 PASS (backendsuite 423, vloer 83; 543 cases); 7 PASS
(507 cases, 36 overlevers); 8 PASS (29 rode cases, vloer 26; 58 fragmenten
tegen 36 zinnen; Drools-been 6 fault-pinned, 23 met reden); 9 PASS (201
checks, 12 goldens); 10 PASS (322 tests, 58 goldens); 11 PASS (ref 29 /
clingo 29 / Drools 29 PLAYED); 12 PASS (exec 159, DRL-tekst 132, JUnit
478); 13 PASS (126 twists over 23 goldens, 104 replays); 14 PASS (130
checks, 2 goldens).

### Voorstel voor de hoofdsessie

`scripts/check-rood.sh` is een beschermd pad: die diff is een VOORSTEL.
`ref/bin/runner.ml` is dat niet, maar de wijziging staat hier apart genoemd
omdat ze buiten de May-touch van de kaart valt: de lensronde wees de route
aan en de kaart kende geen andere manier om het escape-gat te dichten.

De gate-8-beschrijving in `CLAUDE.md` mist het nieuwe been (deze kaart
raakt dat bestand niet). Voorgestelde toevoeging aan het eind van punt 8,
na "…never silently green":

> ; and no pinned fragment cites a BOTTOM SENTENCE of `ref` or clingo — a
> fall-through arm's refusal, which is the same sentence for every form
> that arm catches because it describes the position and what is carried
> there rather than the construct rejected. Such a pin witnesses nothing
> of its own, and where the sentence claims no corpus case reaches it, it
> makes that claim untrue. The thirty-six sentences are listed in the
> script with source, function and reason; the fragments are decoded and
> compared by `ref --bodemzin`, the red path's own reader and matcher, so
> a JSON-escaped pin cannot slip past, and a standing self-test in the
> gate fails if that decoding is ever replaced by a text scrape. The cure
> is a named arm for the form, never a reworded bottom.
