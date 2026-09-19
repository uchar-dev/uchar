# clingo-som-catchall: de bodemzin is live onwaar op de expressiepositie

Lensvondst (clingo-onopgelost-lens, 2026-09-19, pre-existent): de
expressie-catch-all in `backends/clingo/lib/translate.ml` (~r. 3561)
luidt `expression kind not translated (no corpus case exercises it)` —
en `corpus/rood/som-fallback-always-evaluated/case1.json` pint exact
dat bericht als zijn clingo-fragment. Er ís dus een corpusgeval dat de
zin uitlokt: precies de onwaarheid waarvoor de conditiepositie zojuist
is behoed, live op de expressiepositie. De NOTE van die rode case
noemt het gedrag zelf "an honest refusal"; alleen de zín liegt.

Twee routes, te wegen bij oppakken: (a) de eenregelige
eerlijkheidscorrectie — de bodemzin herformuleren tot een weigering
zonder de valse geen-corpusgeval-claim (raakt het gepinde fragment van
de rode case: die pin beweegt mee, gemandateerd binnen de kaart); of
(b) de listed-operand-som een eigen arm geven zodat de bodem weer
onbereikt is. (a) is een uur; (b) is de structurele sluiting.

## Acceptatie (bij oppakken aanscherpen)

- (a)/(b) gekozen met reden; rood-eerst; bij (a) beweegt het gepinde
  fragment van `som-fallback-always-evaluated` mee in dezelfde commit;
  suite-getuige + mutatieproef; alle veertien gates groen.

May touch: `backends/clingo/**`, `corpus/rood/som-fallback-always-
evaluated/**` (fragment-pin), kaartverplaatsing.

Result: geland via route (b), de structurele sluiting. De gelijste som
heeft een eigen arm in `backends/clingo/lib/translate.ml`, direct boven
de expressiebodem, met een weigering die de vorm noemt: `a som over
listed operands is not translated: every som this backend carries
ranges a #sum over an instance source, and a listed fold has none; and
where it carries a ch. 5.11 leegwaarde, the addition chain that would
carry it must evaluate that stand-in unconditionally. No corpus case
pins a value for the form`. De bodemzin zelf is ONGEWIJZIGD gebleven,
en dat is de winst van (b) boven (a): niet de zin verzwakken maar haar
weer waar maken.

Waarom (b) en niet (a): de onwaarheid zat niet in de formulering maar
in de plaats. De gelijste som is een echte vorm met een echte grens —
`ref` draagt hem (zijn bijdragenarm leest de operandenlijst zodra
`elementObjecttype` ontbreekt), deze backend niet, omdat elke somarm
hier een `#sum` over een instantiebron laat lopen die een gelijste vouw
nooit levert — en zo'n grens hoort in een arm die haar noemt. Route (a)
had de bodem eerlijk gemaakt en de vorm naamloos gelaten. De
rood-discipline hield: de rode case faalt nog steeds observeerbaar in
beide uitvoerders, nu op een vormnoemend fragment.

Meting vooraf: van de 29 rode cases pinde er precies één de bodemzin
(`som-fallback-always-evaluated`). Precies gezegd, want een eerdere
formulering hiervan was te sterk: geen enkele corpus-CASE bereikt de
bodem nog — dat is wat de gates zien en wat de zin beweert — maar
case-lóze corpusdocumenten kunnen dat buiten de gates om wél. Gemeten
2026-09-19: `jutterswet-vormenkabinet` en `jutterswet-seizoenvondst`,
de twee goldens zonder casebestand, vallen alle twee op de bodem zodra
je ze met een triviale case door de runner haalt. De bodemzin spreekt
over CASES en is als zodanig weer waar; over documenten zou ze onwaar
zijn.

Rood-eerst en bewijs. Vóór: `TRANSLATION FAULT: expression kind not
translated (no corpus case exercises it)`. Na: het nieuwe fragment, met
de pin in `case1.json` (`expectedFault.clingo`) meebewogen in dezelfde
commit — de enige gemandateerde wijziging aan een bestaande
verwachting — en de NOTE bijgewerkt: die citeerde het oude fragment en
legt nu ook uit waarom het verhuisde. De ref-kant is onaangeroerd
(`division by numeric zero`).

De wacht leest TWEE wirevelden, niet één, en dat is de correctie van de
lensronde. Een afwezig `elementObjecttype` sluit de instantievouwen uit
— elke dragende somarm eist het stempel aanwezig — maar het betekent
"gelijst ÓF dimensiecel", want WF-037 geeft de dimensiesom een
uitdrukkelijke arity-uitzondering: één operand, een `dimensieSelectie`,
geen stempel. Gemeten 2026-09-19 op een verifier-SCHOON frame van die
vorm: vóór de correctie kreeg hij `a som over listed operands …` te
horen, terwijl `ref` hem eerder en op eigen grond weigert (`dimension
selection not implemented in this card`). Dat is dezelfde soort
onwaarheid als de bodemzin, één laag dieper. De arm eist nu ook een
afwezige `dimensieSelectie` en de dimensiesom houdt de bodem, waar die
zin voor hém waar is: geen enkel document in corpus of pariteitszone
draagt een `dimensieSelectie` op een `Functie` (gemeten: nul).

Het weigerfragment claimde de ch.-5.11-onvoorwaardelijkheid bovendien
onvoorwaardelijk, terwijl een gelijste som zonder `leegwaarde` geen
invaller heeft om onvoorwaardelijk te evalueren — de suite-fixture zelf
is zo'n som. De bijzin staat nu onder die voorwaarde. Gecontroleerd dat
de kop niet meebeweegt: de pin in `case1.json` en de needle van
suite-check 1 citeren alleen `a som over listed operands is not
translated`, en beide bleven ongewijzigd.

Suite-getuige: vier checks in `backends/clingo/test/test_clingo.ml`,
419 → 423 (gate-6-vloer 83 ongewijzigd; groei vraagt geen bump). Eén
pint de vorm op de nieuwe arm; drie pinnen de ARMGRENS, één per as van
de wacht — een gelijst `MINIMUM` (ander kind), een som mét
`elementObjecttype` (wél een bronstempel) en de WF-037-dimensiesom
(stempel afwezig, `dimensieSelectie` gezet) houden alle drie de bodem.
Die grens kan het corpus niet dragen, en niet bij toeval: een rode
getuige voor een buurvorm zou de bodem opnieuw met een corpusgeval
uitlokken en de zin meteen weer falsifiëren. De suite is geen
corpuszone, dus daar kan het wel.

Mutatieproef, vier mutanten, elk in één `&&`-keten met gecontroleerde
build-rc: de arm weg → suite-check 1 rood én de rode case faalt op de
fragment-pin (gate 8 rood); de wacht verbreed naar `Som | Minimum` →
grenscheck KIND rood; `element_objecttype = None` weggelaten →
grenscheck ELEMENT rood; `dimensie_selectie = None` weggelaten →
grenscheck DIMENSIE rood. Elke mutant doodt precies één benoemde check.

Voorstel beschermd pad (`spec/ASP-TRANSLATION.md`, niet gemerged):
(1) de catch-all-inventaris noteert dat de gelijste som de bodem
verliet ZONDER vertaald te worden, met de reden en met alle drie de
grenzen; (2) een naburige zin stond te breed. Hij zei dat een gelijste
som/totaal/aantal in BEIDE uitvoerders luid faalt; gemeten geldt dat
alleen mét gestempeld `elementObjecttype`. Zonder stempel draagt `ref`
de vorm, wat de rode getuige aantoont door pas op de deling te
struikelen. Die correctie hoort in hetzelfde voorstel, omdat de nieuwe
arm er anders mee in tegenspraak staat. De inperking blijft binnen de
eerste/laatste-parenthese; het verhaal over de afwezige stempel kreeg
een eigen alinea erna, omdat het de parenthese anders openbreekt.

Niet gedaan, met reden: de vorm DRAGEN is buiten scope — dan wordt de
rode case een golden, een andere kaart. `TOTAAL` en `AANTAL` houden de
bodem: voor `TOTAAL` is een afwezig `elementObjecttype` geen
gelijstheidstest (zijn temporele eigen-subjectarmen dragen er ook een),
en `AANTAL`'s gelijste vorm is geen som. De dimensiesom houdt hem ook,
en dragen zou daar een dimensie-encoding vragen die deze backend
nergens heeft. Voor alle drie is de bodemzin waar.

Gemeld, niet gekaart. De lensronde stelde een tripwire tegen recidive
voor en die is hier bewust niet gebouwd. De vorm van de fout was beide
keren dezelfde — eerst op de conditiepositie, toen hier: een
`expectedFault`-fragment in `corpus/rood/**/case*.json` dat een
substring is van een catch-all-zin die zelf beweert geen corpusgeval te
hebben. Dat is mechanisch te controleren. Gemeten op de huidige stand
met een wegwerpscan over de vier bekende bodemzinnen van `ref` en
clingo: nul treffers — de tripwire zou nu groen zijn en vóór deze kaart
rood. Waar hij hoort (een lijst in een beschermd gate-script, of een
markering in de vertalerbron die de scan automatisch vult) en wat hij
kost is een eigen besluit, en daarom een eigen kaartje.

Gates. Eerste ronde: veertien groen, elk apart aangeroepen met eigen
exitcode — 1 PASS; 2 PASS (238 bestanden); 3 PASS (238 golden clean,
398 negatief); 4 PASS (568 cases); 5 PASS (ref-suite 643, 539 cases);
6 PASS; 7 PASS (503 weerleggingen, 36 survivors); 8 PASS (29 rode
cases, vloer 26); 9 PASS (201 checks, 12 goldens); 10 PASS (322 tests,
58 goldens); 11 PASS; 12 PASS (159 PLAYED, DRL-tekst 132, JUnit 478);
13 PASS (126 twists over 23 goldens); 14 PASS (130 checks, 2 goldens).
Na de lensronde opnieuw gedraaid, want daar raakte de wijziging aan:
1 PASS, 6 PASS (backendsuite 423 checks, 539 cases), 8 PASS (29 rode
cases), plus 11 PASS en 13 PASS omdat die twee clingo óók uitvoeren.
De overige negen zijn onbewogen en dat is niet alleen geredeneerd maar
begrensd: de tweede ronde raakte uitsluitend `backends/clingo/**` en
`spec/ASP-TRANSLATION.md`. Gates 2, 3, 4, 5 en 7 lezen schema,
verifier, casebestanden en `ref` — geen daarvan veranderde; gates 9, 10
en 14 zijn frontends en 12 is Drools, geen van vieren voert clingo uit.

Gewijzigde bestanden: `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml`,
`corpus/rood/som-fallback-always-evaluated/case1.json` en `NOTE.md`,
`spec/ASP-TRANSLATION.md` (voorstel), deze kaart.
