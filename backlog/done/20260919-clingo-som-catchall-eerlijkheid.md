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
ranges a #sum over an instance source, and a listed fold has none; the
addition chain that would carry it must also evaluate the ch. 5.11
stand-in unconditionally, and no corpus case pins a value for it`. De
bodemzin zelf is ONGEWIJZIGD gebleven, en dat is de winst van (b) boven
(a): niet de zin verzwakken maar haar weer waar maken.

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
(`som-fallback-always-evaluated`); een golden kan de bodem niet raken,
want gate 6 gaat op de vertaalfout af. Na de arm raakt geen corpusgeval
de bodem meer, dus "(no corpus case exercises it)" is weer waar.

Rood-eerst en bewijs. Vóór: `TRANSLATION FAULT: expression kind not
translated (no corpus case exercises it)`. Na: het nieuwe fragment, met
de pin in `case1.json` (`expectedFault.clingo`) meebewogen in dezelfde
commit — de enige gemandateerde wijziging aan een bestaande
verwachting — en de NOTE bijgewerkt: die citeerde het oude fragment en
legt nu ook uit waarom het verhuisde. De ref-kant is onaangeroerd
(`division by numeric zero`).

Suite-getuige: drie checks in `backends/clingo/test/test_clingo.ml`,
419 → 422 (gate-6-vloer 83 ongewijzigd; groei vraagt geen bump). Eén
pint de vorm op de nieuwe arm; twee pinnen de ARMGRENS langs beide
assen van de wacht — een gelijst `MINIMUM` (ander kind, zelfde vorm) en
een som mét `elementObjecttype` (zelfde kind, wél een bronstempel)
houden de bodem. Die grens kan het corpus niet dragen, en niet bij
toeval: een rode getuige voor een buurvorm zou de bodem opnieuw met een
corpusgeval uitlokken en de zin meteen weer falsifiëren. De suite is
geen corpuszone, dus daar kan het wel.

Mutatieproef, drie mutanten, elk in één `&&`-keten met gecontroleerde
build-rc: de arm weg → suite-check 1 rood én de rode case faalt op de
fragment-pin (gate 8 rood); de wacht verbreed naar `Som | Minimum` →
grenscheck 1 rood; `element_objecttype = None` weggelaten →
grenscheck 2 rood. Elke mutant doodt precies één benoemde check.

Voorstel beschermd pad (aparte commit, `spec/ASP-TRANSLATION.md`, niet
gemerged): (1) de catch-all-inventaris noteert het vertrek van de
gelijste som — het tweede vertrek dat géén vertaling is — met de reden
en met beide grenzen; (2) een naburige zin stond te breed. Hij zei dat
een gelijste som/totaal/aantal in BEIDE uitvoerders luid faalt;
gemeten geldt dat alleen mét gestempeld `elementObjecttype`. Zonder
stempel draagt `ref` de vorm, wat de rode getuige aantoont door pas op
de deling te struikelen. Die correctie hoort in hetzelfde voorstel,
omdat de nieuwe arm er anders mee in tegenspraak staat.

Niet gedaan, met reden: de vorm DRAGEN is buiten scope — dan wordt de
rode case een golden, een andere kaart. `TOTAAL` en `AANTAL` houden de
bodem: voor `TOTAAL` is een afwezig `elementObjecttype` geen
gelijstheidstest (zijn temporele eigen-subjectarmen dragen er ook een),
en `AANTAL`'s gelijste vorm is geen som. Voor beide is de bodemzin
waar.

Veertien gates groen, elk apart aangeroepen met eigen exitcode: 1 PASS;
2 PASS (238 bestanden); 3 PASS (238 golden clean, 398 negatief);
4 PASS (568 cases); 5 PASS (ref-suite 643, 539 cases); 6 PASS
(backendsuite 422 checks, 539 cases); 7 PASS (503 weerleggingen, 36
survivors); 8 PASS (29 rode cases, vloer 26); 9 PASS (201 checks, 12
goldens); 10 PASS (322 tests, 58 goldens); 11 PASS (29/29 in drie
uitvoerders); 12 PASS (159 PLAYED, DRL-tekst 132, JUnit 478); 13 PASS
(126 twists over 23 goldens); 14 PASS (130 checks, 2 goldens).

Gewijzigde bestanden: `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml`,
`corpus/rood/som-fallback-always-evaluated/case1.json` en `NOTE.md`,
`spec/ASP-TRANSLATION.md` (voorstel), deze kaart.
