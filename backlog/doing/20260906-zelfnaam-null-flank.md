# De lege helft van de zelfnaamklasse — `naam: null` op een gesleutelde declaratie

Goal: beslissen wat de v0-wire doet met een gesleutelde declaratie die geen
zelfnaam draagt — `"naam": null` in plaats van een naam die van zijn sleutel
afwijkt — en dat sluiten of met een gemeten argument open verklaren. Dit is de
LEGE helft van de klasse die WF-094 (`spec/IR-SPEC.md` ch. 2.1, 2026-09-06)
voor de gevulde helft sloot: dezelfde vijf map-vormige declaratietabellen
(`objecttypen`, `parameters`, `eenheidstelsels`, `feittypen`, `dimensies`),
hetzelfde veld, de andere waarde. Het is de (n)/(o)-vorm van de zesde golf van
ch. 4.6 één tabel verderop: een naam die iets ANDERS zegt dan hij moet, tegen
een naam die er niet is.

## De meting ligt er al (2026-09-06, fixronde van de WF-094-kaart)

Deze kaart begint niet blanco. De volgende cijfers zijn gemeten op de
branch-kop van `card/zelfnaam-mapsleutel`, over de vier zones (155 golden +
22 rood + 325 negatief + 30 pariteit; na de rebase op de dagsoortlanding
156 golden, 533 documenten en 1937 gesleutelde declaraties in de vijf tabellen
samen) en met een handgebroken kopie van
`corpus/golden/jutterswet-penningdrempel` via `git archive`.

**1. Voorkomens: nul.** Geen van de 1937 entries draagt `naam: null`. De prijs
van sluiten is dus nul, en de open vraag is de VORM van de clausule, niet zijn
kosten. (Een getuige is één bewerking ver — precies zoals WF-094's vijf
getuigen handgebroken kopieën van goldens zijn. "Geen getuige mogelijk" is
uitdrukkelijk NIET de grond waarop deze vraag open staat; die grond stond
eerder in de open lijst van ch. 4.6 en is in dezelfde fixronde vervangen.)

**2. De WEGLAATflank is dubbel dicht.** `naam` staat in `required` in
`spec/schema/regelspraak-ir-v0.schema.json`, voor alle vijf de tabellen. Een
document zonder het veld faalt gate 2 (`'naam' is a required property`) én
alle drie de OCaml-loaders weigeren het met een luide laadfout die het pad
noemt (`LOAD ERROR at /parameters/vondstdrempel: missing field 'naam'`,
identiek in de verifier, in `ref` en in de ASP-backend). Daar valt niets te
sluiten.

**3. De NULflank staat open, en splitst de uitvoerders.** Het veld is getypeerd
`["string", "null"]`, dus `naam: null` is schema-valide en rondreist
byte-identiek (gate 2 PASS). Gemeten op één golden met één parameter:

| consument | `naam: null` op de parameter |
| --- | --- |
| gate 2 (schema + round-trip) | **PASS** |
| verifier | **PASS** |
| `ref` (`case.json`) | **PASS**, 3 verwachtingen |
| ASP-backend (`case.json`) | **PASS**, 3 verwachtingen |
| bean-modeluitvoerder | **ERROR** `NullPointerException: Cannot invoke "String.trim()" because "naam" is null` |

De crash zit in het identifier-contract
(`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/Namen.java:22`,
de `naam.trim()` in `ident`) — **hetzelfde contract dat het schadekanaal van
WF-094 is**. Daar liep de schade via een naam die de verkeerde identifier
opleverde; hier via een naam die er niet is.

**4. De terugvalgrens verklaart welke tabellen crashen.** De IR-loader van die
uitvoerder neemt de ZELFNAAM als de naam van de declaratie en valt op drie
tabellen terug op de sleutel wanneer het veld leeg is
(`.../loader/IrLoader.java:233` objecttype, `:255` eenheidstelsel, `:268`
feittype: `text(n, "naam") != null ? text(n, "naam") : naam`), maar op twee
tabellen niet (`:250` parameter, `:276` dimensie: `new Parameter(text(n,
"naam"), …)`). Tegenmeting op dezelfde golden: `objecttypen[...].naam = null`
geeft **PLAYED**, ongewijzigd — de terugval doet precies wat hij belooft. De
grens is dus waarneembaar en niet beredeneerd.

**5. Wat de dimensie-arm niet kan tonen.** De twee enige goldens met een
`dimensies`-tabel zijn verifier-only getuigen zonder `case*.json`, dus de
tweede terugvalloze tabel heeft vandaag geen speelbare getuige. Dat is
dezelfde structurele nul die WF-094's dimensie-arm draagt en het is een
argument over de VORM van de clausule, niet over zijn geldigheid.

## Waarom dit scherper is dan het (n)/(o)-paar waar het op lijkt

Ch. 4.6 (n)/(o) splitste "een naam die nergens naar wijst" van "een naam die er
niet is" omdat de twee ANDERS worden waargenomen. Daar waren beide
OCaml-uitvoerders LUID op de lege vorm (`missing ObjectCreatie.subject`). Hier
is het andersom en dat is de scherpere stand: twee consumenten zijn **stil en
correct** — ze sleutelen alles op de mapsleutel, dus voor hen mist er niets —
en de derde **sterft**. Een document dat de verifier, `ref` en clingo
probleemloos passeert en de vierde consument laat crashen is precies de vorm
die de differentiële ruit stilzwijgend tot een driehoek maakt.

## Wat deze kaart moet opleveren

Eén beslissing, rood-eerst uitgevoerd, uit twee routes met hun prijs erbij.

**Route 1 — de wire weigert het (spec + verifier).** Een clausule (d) bij
WF-094 die `naam: null` op een gesleutelde declaratie ill-formed maakt. Twee
snitten, elk met een eigen prijs:

- *smal:* alleen op de twee tabellen zonder terugval (`parameters`,
  `dimensies`), want daar is de schade gemeten. Prijs: de regel zegt dat de
  zelfnaamklasse per tabel iets anders betekent — precies wat WF-094's
  vijf-armen-motivering ontkent — en hij verandert mee wanneer de loader
  verandert, terwijl de wire niet van één backend hoort af te hangen.
- *consistent:* op alle vijf, want de klasse is één klasse en het veld is
  `required` juist omdat de declaratie zijn naam draagt. Prijs: drie van de
  vijf armen rusten dan op het CONSTRUCT en niet op gemeten schade — dezelfde
  eerlijke grond die WF-094 al voor drie van zijn vijf armen opschrijft, en die
  per arm in de NOTE moet staan.

In beide snitten is de prijs in documenten **nul**: 0 voorkomens over 1937
entries, dus geen golden, rood, negatief of pariteitsdocument beweegt.

**Route 2 — de loader leert de terugval (backend).** `:250` en `:276` krijgen
dezelfde `!= null ? … : sleutel`-vorm als hun drie zusters. Dat haalt de crash
weg en maakt de uitvoerders het weer eens. **Route 2 vervangt route 1 niet:**
daarna zegt de spec nog steeds niets over de lege zelfnaam, elke nieuwe
consument mag opnieuw kiezen wat hij ermee doet, en het verschil is dan
volledig onzichtbaar geworden in plaats van luid — de slechtste stand van de
drie. Route 2 is hoogstens een aanvulling op route 1, nooit een vervanging.

## Acceptance

- **Rood-eerst zichtbaar:** de verifier van vóór de regel geeft op elk nieuw
  negatief `verifier: PASS`, met md5 van de binary vastgelegd; daarna faalt elk
  met de nieuwe clausule en citeert alleen die.
- **Arm-exact:** per gesloten tabel één negatief; een mutant die één arm
  schrapt maakt precies dat ene document schoon en geen ander. (Let op de vorm
  van `wf094`: de eerste arm is de kop van de `@`-keten, dus die schrappen
  vereist dat de volgende regel zijn leidende `@` verliest, anders bouwt de
  mutant niet en draait de sweep de vorige binary.)
- **Nul over-afwijzing, direct gemeten:** beide binaries over alle documenten
  van de vier zones; alleen de nieuwe getuigen bewegen, geen enkel rapport
  elders.
- **Alle dertien gates groen**, elk met zijn echte exitcode.
- Bij route 2 erbij: de bean-modeluitvoerder speelt de nieuwe getuigen niet
  meer stuk, en de JUnit-suite van de backend draait met een niet-nul,
  nul-geskipt testaantal.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad), `spec/verifier/**`
(voorstel), `corpus/negatief/**`, `backlog/`. Bij route 2 ook
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader/IrLoader.java`.

## Out of scope

- **WF-009's eenhedenvraag** (of een `afkorting` een eenheid is van het stelsel
  dat hij noemt): eigen regel, eigen kaart, zoals ch. 4.6 (r) al vastlegt.
- **De `eenheden`/`aliassen`-klasse.** De twee maps BINNEN een eenheidstelsel
  lijken op deze klasse en zijn het niet: hun sleutel is niet de eigen naam van
  de waarde. Gemeten over de vier zones: 682 van 712 aliasentries wijken af van
  de `afkorting` die ze dragen (bij ontwerp — de sleutel is daar een alias) en
  58 van 639 eenhedenentries, alle 58 dezelfde vorm (sleutel `€` tegen
  afkorting `EUR`), over 58 documenten waarvan 10 golden. Dat is een vraag over
  EENHEIDidentiteit, naast die van WF-009, en hoort op een eigen kaart.
- De gevulde helft zelf: WF-094 is gesloten en wordt hier niet heropend.
