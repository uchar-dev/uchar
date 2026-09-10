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

## Result (2026-09-10) — GESLOTEN met clausule (d), plus de loader-terugval

Maintainerbesluit 2026-09-09 (genummerde vraag 6, bindend): **CONSISTENT +
LOADER-TERUGVAL** — clausule (d) bij WF-094 op alle vijf tabellen, ÉN route 2
erbij. Beide geland.

### Hermeten op de branch-kop (2026-09-10, de kaartmeting is van 2026-09-06)

| grootheid | kaart (2026-09-06) | nu gemeten |
| --- | --- | --- |
| documenten over de vier zones | 533 | **572** vóór, **577** ná de getuigen |
| gesleutelde declaraties in de vijf tabellen | 1937 | **2000** vóór, **2016** ná |
| per tabel (vóór) | — | `objecttypen` 798, `parameters` 657, `eenheidstelsels` 222, `feittypen` 311, `dimensies` 12 |
| entries met `naam: null` | 0 | **0** |
| sleutels met interne witruimte | 603 | **603**, en nul zelfnamen die alleen in witruimte afwijken |

De prijs van sluiten is dus nog steeds nul, en de vijf getuigen zijn de enige
documenten waar dan ook die de vorm dragen.

### Rood-eerst, per arm, met de echte output

Pre-clausule-binary **md5 `83ce827aba9fab4e369debe8f5f1dd93`** (na de sweep
bit-exact opnieuw gebouwd, zelfde md5): alle vijf getuigen `verifier: PASS`.
Post-clausule **md5 `f55fdba98ccd5ac68bb99492519aa580`** (de binary van de
branch-kop; de eerste meting draaide op `8ef3920f82ca129e2a8c384f6218318e`, de
binary vóór de prozafixronde van de slotronde — die ronde wijzigde alléén
commentaar in `checks.ml`, wat de hash meebeweegt, en alle drie de metingen
hieronder zijn op de kop-binary opnieuw gedaan met bit-identieke uitkomst):
elk faalt met precies
één regel die de positie noemt, bijvoorbeeld

    WF-094 FAIL /parameters/vondstdrempel/naam: parameter under the key
    'vondstdrempel' carries no self-name (naam is null): a keyed declaration
    carries its own name, and an absent name is not one

Edit-recept per getuige: kopie van de golden, precies één regel —
`"naam": "<sleutel>",` wordt `"naam": null,`; elk ander byte onaangeroerd
(één-regel-diff, geverifieerd met `diff`).

| negatief | bron-golden | tabel/sleutel |
| --- | --- | --- |
| `wf094-zelfnaam-null-objecttype` | `anchor/jutterswet-maatschap` | `objecttypen/Jutter` |
| `wf094-zelfnaam-null-parameter` | `jutterswet-penningdrempel` | `parameters/vondstdrempel` |
| `wf094-zelfnaam-null-eenheidstelsel` | `jutterswet-berging` | `eenheidstelsels/Munt` |
| `wf094-zelfnaam-null-feittype` | `jutterswet-vondsttelling` | `feittypen/vondstaanmelding` |
| `wf094-zelfnaam-null-dimensie` | `jutterswet-seizoenvondst` | `dimensies/seizoen` |

**Nul over-afwijzing, direct gemeten:** beide binaries over **577** documenten
van golden + rood + negatief + pariteit — precies vijf rapporten bewegen
(exit 0 → 1), geen enkel elders, ook geen rapporttekst.

**Arm-exact, vijf mutanten, vijf verschillende binaries** (md5's verschillen,
dus elke mutant is echt gebouwd en gemeten): elke mutant maakt precies zijn
eigen getuige schoon en beweegt niets anders over dezelfde 577. De
`objecttypen`-arm is de kop van de `@`-keten; de mutant laat de volgende arm
zijn leidende `@` verliezen, anders bouwt hij niet.

Vorm in de verifier: **eigen functie `wf094_null` naast `wf094`** (huisstijl
`wf034_*`), zodat de tien armen elk op hun eigen getuige falsifieerbaar zijn —
een geschrapte null-arm maakt zijn eigen document schoon en niet zijn
niet-null-zuster.

### De grond per arm, eerlijk

Twee van de vijf armen dragen **gemeten schade** (`parameters`, `dimensies` —
de twee loader-plekken zonder terugval), drie rusten op het **construct**.
Dat staat per arm in de NOTE, zoals WF-094 het al voor zijn eigen armen doet.

### Route 2 — de loader-terugval, met vóór/ná-meting

`IrLoader.parameter(...)` en `IrLoader.dimensie(...)` kregen de sleutel mee en
dezelfde `!= null ? … : sleutel`-vorm als hun drie zusters.

| arm | ongewijzigd | `naam: null` vóór de fix | `naam: null` ná de fix |
| --- | --- | --- | --- |
| `objecttypen` | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| `parameters` | PLAYED 1 | **ERROR** `NullPointerException: Cannot invoke "String.trim()" because "naam" is null` | **PLAYED 1** |
| `eenheidstelsels` | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| `feittypen` | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| `dimensies` | *(geen case — niet speelbaar)* | — | — |

`ref` en clingo bewegen op geen van de vier speelbare armen, elk met zijn eigen
aantal verwachtingen vóór en ná: `objecttypen` 7, `parameters` 3,
`eenheidstelsels` 5, `feittypen` 7.

**De dimensie-arm heeft geen speelbare getuige** — de enige twee goldens met
een `dimensies`-tabel (`jutterswet-seizoenvondst`, `jutterswet-vormenkabinet`)
dragen geen `case*.json`, hertelde stand 2026-09-10. Verifier-only, precies
zoals de kaart het zegt. Wat wél te pinnen valt is het loaderniveau, en dat is
gebeurd: de additieve `ZelfnaamTerugvalWitnessTest` (5 tests) pint de terugval
op **alle vijf** tabellen; tegen de loader van vóór de fix faalt hij op precies
de twee armen zonder terugval (`expected: <vondstdrempel> but was: <null>`,
`expected: <seizoen> but was: <null>`) en slaagt op de drie zusters.

### Spec (voorstel, beschermd pad)

`spec/IR-SPEC.md` ch. 2.1: clausule (d) bij WF-094, met de armen, de grond per
arm, de metingen, v0-mirror-noot, delta en migratie. De lege helft verlaat de
open lijst van ch. 4.6 zoals `Eenheidstelsel.naam` en `dagsoortPerEenheid` dat
deden; de niet-gesloten-lijst van WF-094 gaat van drie naar twee posten;
registerrij bijgewerkt met clausule (d) en de vijf nieuwe negatiefs; de
witruimte-scherpingsnoot kreeg zijn hertelling (1937 → 2016, zelfde lezing).

### Alle dertien gates groen, na de wijziging

    gate 1  exit 0   check-self-contained: PASS
    gate 2  exit 0   192 file(s) validate and round-trip, floor 192
    gate 3  exit 0   192 golden clean; 355 negatief (was 350), floor 350
    gate 4  exit 0   453 case file(s) validate
    gate 5  exit 0   ref suite 489; 427 cases replayed exactly
    gate 6  exit 0   backend suite 83; 427 cases replay exactly
    gate 7  exit 0   391 cases refute a do-nothing executor; 36 survivors
    gate 8  exit 0   26 red cases fault observably, floor 26
    gate 9  exit 0   frontend suite 201; 12 catala goldens byte-identical
    gate 10 exit 0   JUnit 173; 38 rgs goldens byte-identical
    gate 11 exit 0   check-toka-parity: PASS
    gate 12 exit 0   played matches the pinned 119; JUnit 244 tests, floor 239
    gate 13 exit 0   74 twists, floor 74; 0 pinned survivors

Gate 3 gaat van 350 naar 355 negatiefs — boven zijn vloer, zonder
scriptwijziging, zoals het vloerenregime bedoelt. Gate 12's JUnit gaat van 239
naar 244 door de vijf additieve tests; `EXPECTED_PLAYED` (119) onaangeroerd, en
de negatiefs spelen niet.

### Wat bewust bleef liggen

- De `eenheden`/`aliassen`-klasse binnen een eenheidstelsel: eigen vraag over
  EENHEIDidentiteit, eigen kaart (out of scope van deze kaart, en clausule (d)
  raakt hem niet).
- WF-009's eenhedenvraag: onaangeroerd.
- De gevulde helft van WF-094: niet heropend.
- Een speelbare dimensie-getuige: bestaat niet zolang geen uitvoerder een
  dimensiecel leest; de arm rust op de wire-clausule plus de loader-pin.

### Slotronde vóór de merge (2026-09-10, lens 1) — vijf prozafixes

Lens 1 oordeelde mergen na twee prozafixes; vijf punten geland, alle vijf
tekst, geen enkele regelwijziging:

1. **De census in de getuige-NOTEs was verkeerd gescoopt.** Alle vijf droegen
   "over the four zones (577 documents) ... 2000 keyed declarations ... not one
   carries `naam: null`" — maar 2000 is de census van vóór de getuigen
   (572 documenten), en op 577 dragen precies de vijf getuigen — het eigen
   document van elke NOTE incluis — wél null. Nu in alle vijf gescoopt naar
   "as they stood before these witnesses (572 documents) ... held 2000 ... not
   one carried", met de slotzin die zegt wat er ná staat (2016 over 577).
   De dimensie-NOTE's "12 dimension declarations" is op dezelfde manier
   gedateerd (13 mét de getuige).
2. **De 58/639-zin brak op mijn eigen artefact.** De eenheidstelsel-getuige is
   een kopie van `jutterswet-berging` en draagt het `€`/`EUR`-paar mee, dus de
   `eenheden`-telling in WF-094's niet-gesloten-lijst werd onwaar. Herteld en
   gedateerd: **59 van 654 over 59 documenten, 10 golden** (vóór de getuige
   58 van 648 over 58 — de noemer 639 was al vóór deze kaart verlopen), met de
   reden erbij dat een regel over ZELFnamen een telling over EENHEIDnamen
   verschoof. De alias-tellers waren ook verlopen: **710 van 740** (was 682 van
   712), met dezelfde 30 die overeenkomen bij elke hertelling.
3. **"All three OCaml loaders" suggereerde drie onafhankelijke bevestigingen.**
   Nagemeten: er is precies één `loader.ml` (bibliotheek `uchar-ir-v0`), en
   verifier, `ref` én de clingo-backend hangen er alle drie aan — dus één
   weigering die drie keer bereikt wordt. Herformuleerd op alle zeven plekken
   die mijn diff toevoegde (spec-clausule, verifier-commentaar, vijf NOTEs);
   pre-bestaande formuleringen elders in de spec zijn niet aangeraakt.
4. **"(3, 7, 5 en 7 verwachtingen)" stond onder een tabel in andere volgorde.**
   Nu met tabelnaam: `objecttypen` 7, `parameters` 3, `eenheidstelsels` 5,
   `feittypen` 7.
5. **De registerrij droeg de kale "Clause (d)".** De lettering-toelichting die
   ch. 2.1 al had (het label is de nummering van het besluit; het is de tweede
   clausule van de regel) staat nu ook daar.

**Gevolg voor de binary-hash, en de hermeting.** Punt 3 raakt commentaar in
`spec/verifier/lib/checks.ml`, en dat beweegt de hash mee: de post-clausule
binary is nu `f55fdba98ccd5ac68bb99492519aa580`. Alle drie de metingen zijn op
die kop-binary opnieuw gedaan en reproduceren bit-identiek: vijf getuigen falen
met precies één WF-094-regel; over-afwijzing 577 vergeleken, 5 bewogen; vijf
arm-exacte mutanten met vijf verschillende hashes
(`6486cb79…`, `fb3f21ec…`, `189db2be…`, `6af05bb6…`, `8cdb8fce…`), elk precies
zijn eigen getuige schoon en niets anders.
