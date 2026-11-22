# clingo-helper-scope: een hulppredicaat dat een variabele van buiten zijn sleutel leest, wordt luid geweigerd

*Opgesteld door de hoofdsessie op 2026-11-22. Gevonden met willekeurige documenten bij de landing van
`open-values-in-clingo`; nagedraaid op `main`. Een onafhankelijke tweede lezer (Google Neon, alleen lezen) adviseerde
dezelfde snit: klein, los van het programma "Afleiden uit leeg", luid weigeren in plaats van stil fout.*

## Aanleiding

In een gewoon document leest clingo een lokale naam van een `Let` ("daarbij geldt") onder een operator (plus, min, verminderd
met, maal, afronding, grens) in het filter van een vouw zonder melding voor het verkeerde vormonderwerp. Een hulppredicaat
(`coalesce0`, en de hulppredicaten van een tellende samenstelling) heeft alleen het lid als sleutel; de binder hoort bij het
vormonderwerp, en twee onderwerpen delen dan een hulpfeit. Gemeten: "het grenstotaal van een Jutter is, daarbij geldt: de grens
is zijn drempel, de grens plus de som van het gewicht van zijn vondsten waarvan het gewicht plus de grens groter is dan 12",
twee jutters met drempel 10 en 1: ref 35 en 1, clingo 35 en 16. Zonder operator (gewicht groter dan de grens) klopt het. Een
verwant geval: een `Let` in het filter van een vouw waarvan een dieper filter de binder met `=` of `≠` leest, geeft een ander
bedrag dan ref (60 in plaats van 50).

Drools en ref zijn het eens; alleen deze toets-uitvoerder geeft stil een fout antwoord.

## Wat er komt

1. **De vorm luid geweigerd.** Een hulppredicaat waarvan de body een variabele leest die niet in zijn sleutel staat, is een
   vorm die deze codering niet draagt: de vertaling weigert met een code uit het register (`backends/clingo/codes.tsv`, de
   volgende vrije, soort refusal of limit), met de naam van de plek. Beide gevallen hierboven weigeren, en elk ander
   hulppredicaat dat op dezelfde manier leest; zoek ze (`coalesce0`, `eq`, tellende samenstelling, `helper_key`). Gewone
   documenten die de vorm niet hebben, houden hun programma byte voor byte (1300 programma's van het corpus, `dump_programs.py`
   van de werkmap).
2. **Een test voor deze soort fouten**: de isolatie-eigenschap. Voor een kleine matrix van `Let` × operator × vouw draait de
   suite elke case twee keer: elk onderwerp alleen, en alle onderwerpen samen; de uitkomst van elk onderwerp is gelijk. Dezelfde
   matrix laat zien dat wat gedragen wordt klopt, en dat de vorm hierboven weigert.

## Niet in deze kaart

De reparatie (de sleutel op het paar vormonderwerp en lid in elk document): zij geeft een grondingsfout in een tellende
samenstelling, en geen golden heeft de vorm. Niets aan de lezing van open waarden.

## May touch

`backends/clingo/`, `scripts/check-exec-codes.sh` alleen als het register een regel mist, `backends/clingo/codes.tsv`. `spec/`
niet; de kaart `clingo-suite-shards` (zelfde branch) raakt `scripts/check-clingo.sh`.

## Acceptance

- De twee gemeten gevallen weigeren met de code; rood op de basis (clingo 16 en 60 in plaats van de weigering).
- Isolatietest groen op de basis voor wat gedragen wordt, en met een mutant (`coalesce0` met alleen het lid als sleutel) rood.
- Suite van clingo groen, gate 6 groen, gate 8 (been clingo: `check-exec-codes`), 1, 3, 5, 7, 14.

## Result

Commits op `card/clingo-helper-scope`: `0ab55b84c` (weigering, rijen, isolatietest, README, `codes.tsv`) en `c2f64261b` (de isolatierij
faalt bij naam). `translate.ml` +211/−94, waarvan het grootste deel de herschreven fence van het anker en haar commentaar.

### Wat er is

- **CLINGO-051** (`helper-reads-outside-its-key`, refusal, parameters `form` en `read`), opgeworpen door `refuse_unkeyed`.
  De controle staat op één plaats, `unkeyed_read` in `emit_rule`, waar elke regel van het programma langskomt. Zij is de
  bestaande fence van het anker, die nu ook de binder van een `Let` en de payload van een match houdt; de weigering van het
  anker had geen code en heeft die nu (de bestaande rij op haar tekst is ongewijzigd, een rij op de code erbij).
- `around` en `members` houden per lid de leden eromheen bij (`with_member_reach`), `note_outer_read` noteert bij elke lezing
  van een binder op welke objecten zijn waarde staat. Geen enkele regel die geschreven wordt, verandert.
- README, "Recorded limitations". `CLAUDE.md` gate 6 en `spec/CONSTRUCTS.md`: geen zin en geen rij verandert.

### Per leesroute: wat de codering zonder de weigering antwoordt

Gemeten met een mutant per route (de route uitgezet), `ref` ernaast; op `main` is alleen het anker geweigerd. Vondsten: J1 heeft
gewicht 20 en 5, J2 gewicht 5 en 10, tenzij anders gezegd.

| Route | Document | ref | zonder de weigering |
|---|---|---|---|
| anker (bestond) | som waarvan gewicht plus de drempel van de jutter (anker) > 12; drempel 5 en 10 | 20, 15 | J1 25 |
| anker (bestond) | som waarvan geen van [gewicht = drempel (anker), gewicht > 100] | 20, 5 | J2 leeg |
| binder, `coalesce0` | grens plus som waarvan gewicht plus grens > 12; drempel 10 en 1 (kaart) | 35, 1 | J2 16 |
| binder, telling | som waarvan geen van [gewicht > grens, gewicht > 100]; drempel 5 en 10 | 5, 15 | J2 5 |
| binder van een Let in een filter | som waarvan (maat = gewicht in maat plus som van de medevondsten met gewicht = maat) > 35; 10, 20, 30 (kaart) | 50 | 60 |
| binder, gelezen in de guard | als grens > 3 dan som waarvan gewicht plus grens > 12 anders grens; drempel 5 en 10 | 20, 15 | J1 25 |
| payload | Erkend(b): som waarvan gewicht = b; b 7 en 1 | leeg, leeg | 25, 15 |
| payload, telling | Erkend(b): som waarvan geen van [gewicht = b, gewicht > 100] | 25, 15 | leeg, leeg |
| onderwerp van de match | Vast(t): som waarvan afgerond(bedrag maal t) > 3,00, één Vondst bij twee jutters; t 0,5 en 0,1 | 20, leeg | J2 20 |

Twee controles zonder zo'n document zijn eruit: de binder van een lid dat een geankerde knoop verlaat (de andere routes weigeren
elk zulk document al), en een regel die zo'n variabele noemt zonder atoom dat haar bindt (clingo grondt haar niet: open punt 2).

### Goldens en rode cases

Van spelen naar weigeren: **0**. De programma's van alle 1313 paren van `corpus/golden` en `corpus/red` (1300 zonder de
eigenschap) zijn byte-gelijk aan `29f68f07e`, met status en stderr (`dump_programs.py`); ook met de weigering zonder de
uitzondering hieronder (0 van 1313).

### De uitzondering voor één object (voorlopig besluit 1)

De binder wordt alleen geweigerd als de sessie meer dan één object kan bevatten waarvoor zijn variabele staat:
`objects_at_most` telt de objecten van het type in de case (met subtypen) en telt per creatie die er een maakt één per subject,
onbegrensd op een kring. Met één object klopt het antwoord: de body van de hulpregel kan alleen dat object binden, en de lezer van
het hulpfeit staat op datzelfde object. De weigering hangt dus van de case af. Een golden met een case van één en een case van twee
objecten speelt niet half: gate 6 faalt erop bij naam ("its cases play and refuse … a line of EXPECTED_CHANNEL covers every case"),
en geen regel in de lijst maakt hem groen; zo'n golden moet gesplitst worden of op de reparatie van de sleutel wachten. Nu heeft
geen golden de vorm. Waarom niet statisch: dan kantelen 35 bestaande controles van de suite van spelen (of CLINGO-035) naar deze
weigering, de rijen van de Let-barrières per lid, en zijn README [N2], [N3], [L3] en [V3] niet meer waar. Statisch maken is één
uitdrukking (`or_single = false` in `note_outer_read`). Het anker en de payload zelf blijven statisch.

### Acceptance

| Eis | Getuige | Rode stand |
|---|---|---|
| De twee gemeten gevallen weigeren | suite `CLINGO-051: …` (gate 6) | op `29f68f07e`: 16 in plaats van 1, 60 in plaats van 50; 42 van de 324 nieuwe controles rood |
| Isolatie: alleen gelijk aan samen | suite `isolation (…)`: 15 operatoren × 3 vouwen × gewoon/`inferFromEmpty: false` (gate 6) | mutant `coalesce0` op het lid alleen: 24 rood; met de fence uit: 59 |
| Wat gedragen wordt, klopt | dezelfde matrix: 18 gewone en 45 strikte cellen gedragen, elk ook tegen `ref` | fence uit: 42 rood; per route uit: payload 8, onderwerp van de match 1, waarde in de kop 1 |
| Gewone programma's byte-gelijk | `dump_programs.py`, 1313/0 | — |

Buiten de repository, tegen `ref`: een rooster van documenten (Let × plaats × lezing, een vondst bij één en bij meer jutters) gaf op
de basis 1242 stille fouten en 172 grondingsfouten op 5328, en geeft nu op 6508 geen stille fout van deze soort (8 cellen van open
punt 1, 8 van open punt 2); 403 willekeurige documenten met Lets: 0 stille fouten, 3 van open punt 2; het rooster met
`inferFromEmpty: false` (5328): elke uitkomst als op de basis.

### Voorlopige besluiten

1. De uitzondering voor één object, hierboven.
2. Eén code voor de drie routes; het anker krijgt haar mee.
3. Een `viol/2`-fence over een lid blijft vrij van de controle, zoals zij was: zij kan te vaak vuren, en dat is luid.

### Open punten

1. Een deponering vóór de eerste lezing van een binder die leeg is, ontbreekt in clingo (ook op `main`, gewone documenten): "daarbij
   geldt: de grens is zijn drempel, de som van het gewicht van zijn vondsten waarvan gewicht gedeeld door schade groter is dan de
   grens", drempel leeg, schade 0: `ref` deponeert EF-005 voor de jutter, clingo niets; de waarde is in beide leeg. Met de
   eigenschap weigert `let-binder-not-first`.
2. Grondingsfouten zonder code (luid, ook op `main`): een fence over een lid die het vormonderwerp alleen onder een negatie leest
   (geankerde ≠ of leeg-test in een vouw in een filter), en met één jutter een tellend lid `not eq(S,X)`.
3. Opgelost op deze branch: een deponering in de voorwaarde van een feitcreatie droeg in clingo de naam van de vorige vorm;
   `translate_fact_creation` zet nu `ctx.rule_name`. Suite `the form of a §9.5 fact: …`: regel, feitcreatie, objectcreatie,
   verdeling, default en consistentie, met `ref` ernaast; op `abaabb3d7` is alleen de feitcreatie rood. Een uniciteit leest geen
   expressie. De 1313 programma's van het corpus zijn byte-gelijk (`dump_programs.py`).

### Gates (los)

- 1 `check-self-contained: PASS`
- 3 `check-verifier: PASS` (510 goldens schoon, 657 negatieven afgewezen)
- 5 `check-oracle: PASS` (4239 voorbeelden, 1211 cases)
- 6 `check-clingo: PASS` (suite 4779 controles, 1181 cases spelen, 10 goldens en 30 cases in de weigerlijst)
- 7 `check-noop: PASS` (1169 cases)
- 8 `check-exec-codes: PASS` (14 REF- en 51 CLINGO-codes); `check-red`: alleen de regel over het Drools-been (geen `mvn` op `PATH`)
- 14 `check-flogic: PASS` (164 controles)

Niet gedraaid: gates 12, 13, 17 en `make gates`; het been Drools van gate 8.
