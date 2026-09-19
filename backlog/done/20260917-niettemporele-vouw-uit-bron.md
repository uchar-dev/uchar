# niettemporele-vouw-uit-bron: de ongekwalificeerde kwantorvouw bereikt de frontend

Aanleiding: de RegelSpraak-frontend lowert een §8.2-zin alleen naar een
Kwantorvouw wanneer de conditie een heelperiodekwalificatie draagt
("gedurende het gehele jaar") — `RuleChecker.kwantorvouw` opent met
`if (periodeVan(c) == null) return c;`. De NIET-temporele vouw ("alle X
die Y <vergelijking>", zonder kwalificatie) speelt sinds de
asymmetrie-kaart drieweg, maar alleen vanuit handgeschreven IR; de
goldens `jutterswet-schouwploeg` en `-vuurtoren` hebben daarom geen
`bron.rgs`, en de ongekwalificeerde spelling valt vandaag op WF-096
(vergelijkingsvorm) of `Onopgelost` (kenmerkvorm). Maintainerslezing
2026-09-17: de ongekwalificeerde vorm is ook valide RegelSpraak; de
frontend moet hem dragen. Dit is een echte frontenduitbreiding — rij 40
noemt hem als "widening that is a frontend card of its own".

Eerste stap van de kaart: de spec-grond vaststellen. Toets tegen de
RegelSpraak-specificatie v2.3.0 (read-only; de ANTLR-mapping ligt in de
toolchain-repo onder `specification/`) welke productie de
ongekwalificeerde kwantificatie draagt en met welke semantiek, en leg
die vindplaats in het Result vast — géén spec-tekst citeren in
corpusmateriaal.

## Acceptatie (bij oppakken aanscherpen)

- (a) De spec-grond (v2.3.0-vindplaats + de gedragen spellingen) in het
  Result; elke spelling die v2.3.0 niet draagt blijft luid geweigerd.
- (b) De frontend lowert de gedragen ongekwalificeerde vormen naar de
  Kwantorvouw met niet-temporele matrix (alle vijf kwantoren, filter
  incluis), met JUnit-getuigen en rood-eerst-metingen van de huidige
  WF-096/Onopgelost-uitkomsten.
- (c) `jutterswet-schouwploeg` en `-vuurtoren` krijgen hun `bron.rgs`
  (byte-identieke heremissie door gate 10) — of, waar de zinsvorm net
  anders uitpakt, een nieuwe golden mét bron ernaast; niets bestaands
  wijzigt zonder gemelde reden.
- (d) De rij 40-clausules over de §8.2-poort en de handgeschreven
  getuigen versmallen — registervoorstel in het Result. De
  getalpredicaat-weigering van de diedat-kaart wordt herzien waar de
  matrix niet meer per constructie temporeel is (de reden vervalt op
  deze positie — meet en beslis).
- (e) Alle veertien gates groen; toolchain-naschok bij de herpin
  gemeten (de vendored resolver gaat de vorm loweren; de
  toolchain-armen dragen de niet-temporele matrix nog niet — luide
  weigering verwacht, meten).

## Result (2026-09-19)

De poort is weg, beide getuigen komen uit bron, en er ging geen enkel
bestaand document of script aan mee.

### (a) De spec-grond — v2.3.0, vindplaats per productie

Kwantificatie en heelperiodecheck zijn in v2.3.0 ONAFHANKELIJKE
producties; geen van beide noemt de ander.

- De kwantor staat op **§13.4.2.9** (`attribuutVanOnderwerp`:
  `kwantificatie? attribuutMetLidwoord VAN onderwerpKeten`) en op
  **§13.4.17.5** (`objectExpressie`: `kwantificatie? onderwerpKeten`).
  Zijn vocabulaire is **§13.4.14.5** (`kwantificatie`): `de | alle | al |
  geen van de | (ten minste|ten hoogste|precies) <telwoord> van de`;
  de bulletvariant ernaast is **§13.4.14.4**.
- De heelperiodecheck is **§13.4.15.70** (`gehelePeriodevergelijking`:
  `NIET? GEDURENDE (HET_GEHELE JAAR | DE_GEHELE MAAND)`) en staat als
  **optionele prefix** — `gehelePeriodevergelijking?` — in ELKE
  vergelijkingsoperator: de verb-split (vragende) reeks
  **§13.4.15.20–.48** en de verb-leading (stellende) reeks
  **§13.4.15.72–.100**.

De optionaliteit daar IS de spec-grond: de ongekwalificeerde spelling
van een gekwantificeerde conditie is precies zo goed RegelSpraak als
haar gekwalificeerde zuster. De poort
`if (periodeVan(c) == null) return c;` zette dus een FRONTENDgrens waar
de taal er geen heeft. De semantiek van de matrix volgt uit
`RUNTIME-SEMANTICS.md` ch. 3.9a, die de vouwformule over een
WILLEKEURIGE `test` stelt — er beweegt niets aan de kwantor, alleen het
`periode`-veld van de matrix gaat leeg.

Gedragen, en dus gelowerd: de vergelijkingsvorm (§13.4.2.9 +
§13.4.15.24/.23) en de lidmaatschapsvorm (§13.4.2.9 + §13.4.15.44/.45,
"… gediplomeerd zijn"), elk onder alle vijf kwantoren van §13.4.14.5 en
met de §5.6-deelselectie.

Wat op de lidmaatschapsroute geweigerd blijft is ZEVEN vormen, en niet
alle zeven even hard. **Drie luid** (gemeten):

- een telwoord onder 1 (WF-041) — WAARSCHUWING die de as noemt, marker;
- de §8.2-GEKWALIFICEERDE lidmaatschapsmatrix — ERROR "de check op
  volledige periode … kan alleen op een tijdsafhankelijk onderwerp
  worden toegepast (§8.2)", `make ir` exit 2, niets geschreven;
- een niet-bindende die/dat — ERROR uit `keurDeelselecties`, exit 2,
  niets geschreven. Die was in de eerste ronde van deze kaart stuk; zie
  "f-1" hieronder.

**Vier stil**: `DE` (één lid, geen kwantificatie geschreven), een §8.1.7
ROLCHECK van het element, een meerstapsbereik, en een onbindbaar
bereik. Gemeten geeft elk een document met een `Onopgelost`-voorwaarde
en NUL diagnoses. Dat is bestaand ontwerpgedrag — `Onopgelost` is de
eerlijke marker van deze frontend voor elke onopgeloste conditie, en op
main markeerde de HELE lidmaatschapsvorm zo — dus deze kaart voegt geen
stilte toe, zij haalt er alleen voor de gedragen vormen een stuk vanaf.
Het blijft niettemin een stilte over een zin die de lezer voor een
kwantificatie aanziet. **Voorstel: een eigen kaartje** dat meet of een
`Onopgelost` op een GEKWANTIFICEERDE conditiepositie een luide weigering
hoort te zijn (de posture van `keurDeelselecties`), of dat de marker daar
de juiste is. Buiten deze kaart gehouden omdat het een besluit over de
markerdiscipline van de hele frontend is, niet over deze vouw.

### (b) Rood-eerst, letterlijk

Gemeten op de twee bronbestanden vóór enige codewijziging, met
`make -C frontends/regelspraak ir FILE=… IR_OUT=…`:

`jutterswet-schouwploeg` — de vergelijkingsvorm faalt, `make` exit 1,
er wordt NIETS geschreven:

```
  ERROR (line 19): setlezing buiten een vouwbron: 'de dienstjaren van alle wachten van de Schouwploeg' noemt de waarde van elk lid van de verzameling — alleen toegestaan als bron van één vouw (som, totaal, minimum, maximum, eerste, laatste, aantal); hier wordt één waarde verwacht (WF-096, IR-SPEC ch. 4.3)
  ERROR (line 23): setlezing buiten een vouwbron: 'de dienstjaren van geen van de wachten van de Schouwploeg' …
make: *** [ir] Error 1
```

De kenmerkvorm daarentegen emitteert STIL — losgetrokken in een eigen
bestand, zonder één diagnose:

```
bevoegd bij diploma -> {"node": "Onopgelost", "bron": "alle wachten van de Schouwploeg gediplomeerd zijn"}
sluitend bij diploma -> {"node": "Onopgelost", "bron": "geen van de wachten van de Schouwploeg niet gediplomeerd is"}
```

`jutterswet-vuurtoren` — vier WF-096-fouten (regels 19, 23, 27, 31: ten
minste 2 / ten hoogste 1 / precies 1 / het gefilterde ten minste 2) plus
op regel 31 de die/dat-weigering ("de 'die/dat'-deelselectie 'gekeurd
zijn' wordt niet omgezet en zou stil wegvallen"); `make` exit 1.

### (c) Wat er gebouwd is

`RuleChecker.kwantorvouw` verliest zijn openingsregel. Dat alleen al
draagt de vergelijkingsvorm, want de kwantor stond altijd al op de
knoop (`geschrevenKwantor`/`geschrevenDrempel`) en elke weigering van de
vouw hangt aan het BEREIK, niet aan de periode.

De kenmerkvorm is een route van zichzelf, en de meting legde uit waarom:
"alle wachten van de Schouwploeg" parseert als §13.4.2.9
`kwantificatie? attribuutMetLidwoord VAN onderwerpKeten` — de kwantor
zit op de attribuutVanOnderwerp ZELF, niet op een binnenste onderwerp,
dus de operand lost op als "de wachten van de Schouwploeg" met
`set: false` en `kwantor: null`. Er was daar dus geen setlezing om te
hekken (vandaar de stilte) en geen operand om te vouwen. Nieuw:
`ExpressieBouwer.gekwantificeerdOnderwerp` leest kwantor, telwoord,
elementtype en geschreven bron van de parse-boom, en
`RuleChecker.kwantorKenmerk` bindt het kenmerk op het ELEMENTtype
(waarom `kenmerkPredicaat` het niet kon: "gediplomeerd" is geen kenmerk
van de Schouwploeg) en bouwt de vouw. De door de gulzige parse
GEABSORBEERDE "niet" — in "geen van de wachten van de Schouwploeg NIET
gediplomeerd is" belandt hij in de kaleNaam van het anker
("Schouwploeg niet") — wordt teruggezet vóór de kenmerknaam, zodat de
gewone verbatim-first ontkenning van §8.1.8.1 hem afhandelt.

De §13.4.8 consistentieregelASSERTIE wordt EXPLICIET buiten de vouw
gehouden, en op de gronden van haar eigen drager: `transpileerbaar`
kent geen schendingsregel voor een `Kwantorvouw`, dus verlagen zou daar
de WF-096-FOUT ruilen voor de "nog niet omgezet"-WAARSCHUWING en de
controle van de wire laten vallen. De bulletcriteria hebben zo'n hek
niet nodig: een samengestelde consistentieregel zet geen regelsubject,
dus het bereik bindt er nooit.

### (c) De goldens — beide uit bron, geen nieuwe golden nodig

De zinsvorm pakte voor GEEN van beide anders uit, dus er is geen nieuwe
golden en geen kanaalgevolg:

- `corpus/golden/jutterswet-schouwploeg/bron.rgs` — nieuw;
  `make ir` levert het BESTAANDE `ir.json` byte voor byte
  (`diff` leeg). Vijf regels: ALLE/GEEN over een vergelijking,
  ALLE/GEEN over een kenmerkcheck (de tweede met de ontkende matrix),
  plus de instantietelling.
- `corpus/golden/jutterswet-vuurtoren/bron.rgs` — nieuw; idem
  byte-identiek. Vijf regels: de drie tellende kwantoren, het
  gefilterde `ten minste 2 … die gekeurd zijn`, plus de telling.

Beide `ir.json` zijn ONAANGEROERD. Gewijzigd met gemelde reden: de
`## Why this ir.json is hand-written and carries no bron.rgs`-alinea van
beide NOTE.md — die beschreef de weigering als blijvend en is dat niet
meer. De hand-uitgerekende cellen eronder staan onaangeraakt.

### De vier uitvoerders

Ongewijzigd, en dat is het punt: de wire-documenten zijn identiek, dus
ref ≡ clingo ≡ Drools-exec ≡ Drools-DRL-tekst antwoorden precies wat ze
antwoordden. Gate 5 (516 cases exact), gate 6 (516 cases door clingo),
gate 12 (played 151, kanaalvloer 49 niet-gespeelde goldens bij naam,
`EXPECTED_PLAYED` ONVERANDERD) en gate 13 (100 replays, 21 door de
DRL-tekst-tak) bevestigen het. De tien casebestanden van de twee
goldens blijven hun hand-uitgerekende verwachtingen pinnen; wat erbij
kwam is dat die verwachtingen nu aan een BRON hangen.

### Sweep-effect

Nul. Alle 56 rgs-goldens (was 54) re-emitteren byte-identiek, alle 45
TOKA-`.rgs` ook (gate 11's her-emissieleg; de 29 die dezelfde gate noemt
zijn zijn replay-slices, een ander getal over dezelfde zone), en de 12
catala- en 2 flogic-goldens eveneens. Dat is verwacht en het is een
echte meting: een golden is verifier-schoon, dus kon geen enkele
bestaande bron de geweigerde vorm dragen.

In de JUnit-suite verschoven precies DRIE rijen, alle drie pinnen van de
poort die verdwijnt:

- `KwantorvouwTest.een_ongekwalificeerde_setlezing_in_een_voorwaarde_houdt_de_wf096_fout`
  → `…_wordt_ook_een_kwantorvouw`;
- `SetlezingZonderVouwTest.setlezing_in_een_voorwaarde_wordt_benoemd`
  → `…_krijgt_een_kwantor_als_thuis`;
- `SetlezingZonderVouwTest.setlezing_in_de_voorwaarde_van_een_objectcreatie_wordt_benoemd`
  → `de_voorwaarde_van_een_objectcreatie_vouwt_mee`.

De consistentieregelrij van dat bestand en alle VIER waardeposities
(regelwaarde, samengestelde operand, gelijkstelling/initialisatie,
objectcreatie-toekenning) meten onveranderd de WF-096-FOUT.

### Getuigen en mutatieproeven

Nieuw: `frontends/regelspraak/src/test/java/…/NietTemporeleVouwTest.java`
— vijftien rijen, met de spec-grond hierboven als klassecommentaar. Vijf
kwantoren over de vergelijking, vijf over de kenmerkvorm, de lege
periode op de matrix, de eigen elementlezing, de deelselectie op beide
routes, het getalpredicaat op een niet-temporeel attribuut, de
stellende bulletvorm, en VIJF weigeringen (subjectkenmerk op het
element, telwoord onder 1, de gekwalificeerde kenmerkvorm, het
meerstapsbereik, en — sinds de fixronde — de niet-bindende die/dat, die
op ERROR gepind staat en niet op WAARSCHUWING). Suite: 283 → 298 tests,
alle groen.

Vier mutatieproeven, elk gedood:

1. **De poort weer dicht** (`if (periodeVan(c) == null) return c;`
   terug): `make ir` op `jutterswet-schouwploeg/bron.rgs` faalt met de
   twee WF-096-fouten op regels 19 en 23 en schrijft niets; op
   `jutterswet-vuurtoren/bron.rgs` met vier WF-096-fouten plus de
   die/dat-weigering. De nieuwe emissie is dus echt van deze wijziging
   afhankelijk.
2. **De kenmerkroute uit** (`elementKenmerk` → `null`): schouwploeg
   emitteert wél, maar `bevoegd`/`sluitend` komen terug als
   `{"node": "Onopgelost"}` en de diff met het gecommitte `ir.json`
   faalt.
3. **De geabsorbeerde "niet" laten vallen** (`absorbedNegator(lhs)` →
   `false`): de diff faalt op precies één regel —
   `"negatie": false` waar `true` hoort. De ontkenning zit dus
   aantoonbaar op de MATRIX en niet op de kwantor.
4. **De teruggave van de deelselectie uit** (`geefDeelselectieTerug`
   weggehaald): de f-1-zin emitteert weer stil (exit 0, document op
   schijf) en de nieuwe JUnit-rij valt op "expected: <1> but was: <0>" —
   de blokkerende fout is dus echt aan die ene regel opgehangen.

### f-1 — de stilte die de fixronde vond, vóór en ná

De lens vond één blokkerende regressie: een die/dat die niet tegen het
element bindt, viel op de lidmaatschapsroute van een blokkerende fout
terug naar een waarschuwing die de CLI niet eens print. Gemeten op

```
Een Schouwploeg is bevoegd indien alle wachten van de Schouwploeg die gekeurd zijn gediplomeerd zijn.
```

(`gekeurd` is een kenmerk van de Schouwploeg, niet van de Strandwacht,
dus de deelselectie bindt niets).

**Op het aftakpunt (`bdea4ff4`, de resolverbestanden teruggezet en
opnieuw gebouwd):**

```
  ERROR (line 15): de 'die/dat'-deelselectie 'gekeurd zijn' wordt niet omgezet en zou stil wegvallen: …
make: *** [ir] Error 1     exit=2     bestand geschreven: NEE
```

**Op de eerste ronde van deze kaart:** exit 0, bestand GESCHREVEN, met
`{"node": "Onopgelost", …}` als voorwaarde en alleen een WARNING in
`diagnostics` — precies de ruil die het commentaar van
`keurDeelselecties` verbiedt ("ERROR, not WARNING, and the difference is
the point").

**Oorzaak.** `ExpressieBouwer.deelselectie` boekt het predicaat
ONVOORWAARDELIJK als verbruikt vóórdat het weet of de vouw het aanneemt.
Op de VERGELIJKINGSroute is dat juist: een geweigerde vouw laat de
setlezing staan, WF-096 blokkeert het document, en de deelselectie wordt
mét dat document geweigerd. Op de LIDMAATSCHAPSroute is er geen
setlezing en dus geen WF-096 — `keurDeelselecties` was het enige slot, en
dat zweeg omdat het predicaat als verbruikt geboekt stond.

**Fix** (`66761ff6`): `ExpressieBouwer.geefDeelselectieTerug`, aangeroepen
in de weigeringstak van `RuleChecker.kwantorKenmerk`. Ná:

```
  WARNING (line 15): de 'die/dat'-deelselectie in 'alle wachten van de Schouwploeg die gekeurd zijn' wordt niet omgezet naar een kwantorvouw-filter: zij bindt geen kenmerk of getalpredicaat van Strandwacht (§5.6, IR-SPEC ch. 4.5); …
  ERROR (line 15): de 'die/dat'-deelselectie 'gekeurd zijn' wordt niet omgezet en zou stil wegvallen: …
make: *** [ir] Error 1     exit=2     bestand geschreven: NEE
```

De fout is terug, en de waarschuwing staat er nu NAAST in plaats van in
haar plaats: zij noemt de as waarop de binding faalde, wat de
blokkerende fout niet kan. De vergelijkingsroute is onaangeraakt —
`KwantorvouwFilterTest` meet daar onveranderd één WF-096-fout plus één
waarschuwing.

### Gemeten restvorm — de GENESTE kwantorvouw

Met de poort weg is er een producentroute bij die niemand vroeg: een
`Kwantorvouw` IN het filter van een `Kwantorvouw`. Hij loopt via de
§13.4.13.4 samengestelde deelselectie, waarvan de bullets door
`bouwElementair` gaan en dus nu ook vouwen. Gemeten met een eigen
probe-document plus casebestand (`het wachttal van alle ploegen van de
Haven die aan alle volgende voorwaarden voldoen: - hij keurbaar is -
alle wachten van de Schouwploeg gediplomeerd zijn groter is dan 2`):
**verifier PASS**, **ref PASS**, **clingo PASS**, en BEIDE Drools-takken
weigeren luid per vorm (`EXCLUDE transpiler SKIP … een die/dat-subselectie
die deze tak niet op de source kan binden` resp. `EXCLUDE drl-transpiler
MARKER … niet IN het elementpatroon kan schrijven (Samengesteld)`).

Benoemd en niet gebouwd: er is geen getuige en geen gate voor deze vorm,
en drie-van-vier is geen vierkant. De weigeringen zijn luid en per vorm,
dus de vorm kan niet stil verkeerd landen — maar wie hem wil pinnen moet
er een golden voor schrijven, en dat verschuift gate-12-kanalen. Eigen
kaart.

### (d) Registervoorstellen — NIET geland, exact geformuleerd

Rij 40, laatste kolom. **Voorstel 1** — de clausule "the frontend
lowers only the whole-period-qualified condition — a NINTH, narrower
shape — so ALL EIGHT measured WF-096 producer routes are untouched"
vervangen door: *"the frontend lowers a quantified CONDITION, qualified
or not, so the EIGHT measured WF-096 producer routes are SIX: BOTH
CONDITION positions left that fence — the rule's own voorwaarde and the
objectcreatie's, which are one and the same code path (`bouwVoorwaarde`,
where every resultaatdeel kind's voorwaarde passes through) — because a
condition has a quantifier to give the collection a home. What remains
fenced is the four VALUE positions (rule value, composite operand,
gelijkstelling/initialisatie value, objectcreatie toekenning), which take
one value and name no quantifier, plus the §13.4.8 consistentieregel
ASSERTION, fenced on its carrier's own terms."*

Gemeten, niet geredeneerd: op het aftakpunt gaf de
objectcreatie-voorwaarde de WF-096-FOUT, op de branch een `Kwantorvouw`.
`SetlezingZonderVouwTest` meet beide posities nu als verlaging en de
vier waardeposities plus de consistentieregel onveranderd als FOUT.

**Voorstel 2** — VIER stale clausules vervangen door één
**Landed**-alinea. Het zijn: (1) "the frontend does NOT produce this
shape — measured, both spellings … widening that is a frontend card of
its own, and the witness is hand-written IR without a `bron.rgs`"; (2)
"the §8.2 gate is unmoved: a fold whose matrix carries no whole-period
qualifier still does not come from source"; (3) "The §8.2 gate is again
unmoved — … so the non-temporal witness is hand-written IR and gate 10
does not see it" (tellende-kwantoren-kaart); en (4) de
NARROWS-passage van de bronpad-kaart, "`RuleChecker.kwantorvouw` still
lowers the §8.2-QUALIFIED condition and nothing wider — the non-temporal
matrix is still hand-written IR, and widening that is still a frontend
card of its own". De alinea:

> **Landed (2026-09-19, kaart niettemporele-vouw-uit-bron) — de
> §8.2-POORT van dit knooppunt is WEG, en dat sluit de laatste
> brongrens van deze rij.** Waar vier clausules hierboven zeggen dat de
> frontend deze vorm niet produceert en dat de poort onbewogen blijft:
> hij is verdwenen, en de grond was spec-grond. v2.3.0 schrijft de
> kwantificatie (§13.4.2.9, §13.4.17.5, vocabulaire §13.4.14.5) en de
> heelperiodecheck (§13.4.15.70) als ONAFHANKELIJKE producties, en die
> tweede staat als OPTIONELE prefix in elke vergelijkingsoperator van
> §13.4.15.20–.48 en .72–.100. De ongekwalificeerde spelling is dus even
> goed RegelSpraak als haar zuster, en `RuleChecker.kwantorvouw` opende
> niettemin op een kwalificatietest — een frontendgrens waar de taal er
> geen heeft (maintainerslezing 2026-09-17). Twee spellingen landen. De
> VERGELIJKINGSvorm viel op WF-096 en volgt nu de bestaande verlaging
> met een `periode: null`-matrix — ch. 3.9a stelt de formule over een
> willekeurige `test`, dus er beweegt niets aan de kwantor. De
> LIDMAATSCHAPSvorm ("alle wachten van de Schouwploeg gediplomeerd
> zijn") is een route van zichzelf en was STIL: daar zit de kwantor op
> de §13.4.2.9 attribuutVanOnderwerp zelf, dus de operand kwam terug met
> `set: false` en `kwantor: null` — géén setlezing om te hekken, geen
> operand om te vouwen, en de zin markerde zonder één diagnose. Zij
> bindt het kenmerk nu op het ELEMENTtype, met de door de gulzige parse
> geabsorbeerde "niet" terug op de matrix. Alle vijf kwantoren en de
> §5.6-deelselectie doen beide routes; wat blijft weigeren doet dat met
> zijn eigen reden, en op de lidmaatschapsroute zijn dat er zeven —
> DRIE luid (een telwoord onder 1, de §8.2-GEKWALIFICEERDE matrix en een
> niet-bindende die/dat, die laatste via `keurDeelselecties`' ERROR, dus
> blokkerend) en VIER als `Onopgelost`-marker (`DE`, een §8.1.7 rolcheck
> van het element, een meerstapsbereik en een onbindbaar bereik) —
> bestaand markergedrag, niet door deze kaart toegevoegd, en een eigen
> kaartje waard. **En dit HAALT de woorden "hand-written IR" weg bij
> beide niet-temporele getuigen**: `corpus/golden/jutterswet-schouwploeg`
> en `-vuurtoren` hebben hun `bron.rgs` en re-emitteren hun BESTAANDE
> `ir.json` byte voor byte — de documenten zijn onaangeroerd, gate 10
> telt 56 rgs-goldens en ziet ze nu. Sweep: nul — alle 56 rgs-goldens,
> alle 45 TOKA-`.rgs` (gate 11's her-emissie; de 29 zijn zijn
> replay-slices), en de 12 catala- en 2 flogic-goldens re-emitteren
> identiek, en gate 12's `EXPECTED_PLAYED` blijft staan (geen nieuwe
> golden). Drie JUnit-rijen draaiden om, alle drie pinnen van de
> verdwenen poort; getuige `NietTemporeleVouwTest` (vijftien rijen). Eén
> restvorm is GEMETEN en niet gebouwd: een `Kwantorvouw` in het FILTER
> van een `Kwantorvouw`, via de §13.4.13.4 samengestelde deelselectie —
> verifier, ref en clingo dragen hem, beide Drools-takken weigeren luid
> per vorm, en er is geen getuige en geen gate.

**Voorstel 3 (de gemeten herziening van (d))** — de getalpredicaat-
clausule "but on THIS position that operand is temporal by construction,
and a timeline in a scalar slot has no reading (ch. 3.3), so every such
filter would emit a document that falls over by name in all three
executors; the NODE carries the shape …, this sentence position cannot
produce it" vervangen door: *"but when that operand is TIJDSAFHANKELIJK
a timeline in a scalar slot has no reading (ch. 3.3), so the filter is
refused by name. On the §8.2 position the operand was temporal BY
CONSTRUCTION, which is why the diedat card recorded this as a refusal of
the POSITION; since the unqualified fold landed (2026-09-19) the matrix
is an ordinary condition and the operand is usually scalar. The check
was always keyed to the ATTRIBUTE's granularity and not to the position,
so it needed NO code change: over a non-temporal attribute the same
sentence binds, measured in `NietTemporeleVouwTest`. Only the reason and
the diagnostic stopped claiming the position."*

Dat derde punt IS de meting die de kaart vroeg: de weigering hoefde niet
opgeheven te worden, want zij stond al op de juiste as. De
frontendcommentaren en de melding zijn in deze kaart bijgetrokken (dat
zijn geen registerbestanden); `NietTemporeleVouwTest
.een_getalpredicaat_bindt_op_een_niettemporeel_attribuut` en
`KwantorvouwFilterTest
.een_getalpredicaat_deelselectie_valt_luid_op_een_temporeel_attribuut`
meten de twee kanten.

Rij 40 kolom 6 (v1-delta) hoeft NIET bijgetrokken: die beschrijft de
wire, en de wire bewoog niet.

### (e) De veertien gates, exitcodes letterlijk

Hermeten na de fixronde (de eerste ronde gaf dezelfde codes met 297 in
plaats van 298 tests):

```
check-self-contained      exit=0
check-ir-schema           exit=0   226 files validate + round-trip (floor 192)
check-verifier            exit=0   226 golden clean; 395 negatief rejected (floor 350)
check-case-schema         exit=0   542 case files validate
check-oracle              exit=0   ref suite 643 (floor 489); 516 cases exact (floor 427)
check-clingo              exit=0   backend suite 362 (floor 83); 516 cases replay
check-noop                exit=0   480 cases refute a do-nothing executor; 36 survivors
check-rood                exit=0   26 red cases fault in ref and clingo (floor 26)
check-catala              exit=0   201 checks (floor 201); 12 catala goldens
check-regelspraak         exit=0   298 tests (floor 173); 56 rgs goldens (floor 38)
check-toka-parity         exit=0   45 .rgs byte-identiek her-emitteerd (floor 45);
                                   29 replay-slices: ref 29 / clingo 29 / Drools 29 PLAYED
check-drools              exit=0   played 151; channel floor 49; JUnit 447 (floor 239)
check-reftype-derivation  exit=0   108 annotations / 22 goldens; 100 replays, 21 DRL-text
check-flogic              exit=0   130 checks (floor 81); 2 flogic goldens
```

### Scriptdiff

Leeg. `git diff -- scripts/` over de hele kaart geeft niets: er kwam
geen nieuwe golden bij, dus `scripts/check-drools.sh` hoefde niet
aangeraakt — de parallelle kaart die datzelfde script wél raakt, kan
schoon rebasen.

### May touch, geverifieerd

`git diff --name-only bdea4ff4..HEAD` geeft exact twaalf paden: de kaart
(twee, de verplaatsing), de twee genoemde goldens (bron.rgs + NOTE.md,
vier) en zes bestanden onder `frontends/regelspraak/**` (twee resolvers,
vier testbestanden waarvan één nieuw). Geen `scripts/`, geen `spec/`,
geen backends-code, geen ander corpus.

### Open — de toolchain-naschok, te meten bij de eerstvolgende herpin

Niet gemeten in deze kaart, met opzet: de gevendorde resolver in de
toolchain-repo gaat deze vorm straks óók loweren, en of de armen van die
toolchain de NIET-temporele matrix dragen is daar een open vraag — een
luide weigering is het te verwachten antwoord, maar dat is een meting
aan de andere kant van de pin. De hoofdsessie meet hem bij de
eerstvolgende `SPEC_REV`-herpin. Binnen dit repo staat de vraag niet
open: gate 11 her-emitteert alle 45 TOKA-`.rgs` byte-identiek door uchars
eigen frontend, dus onze kant van de grens is gemeten.
