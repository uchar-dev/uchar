# De stille factor-één-terugval in de eenheidslogica wordt een fout

Goal: `Eenheid.tellerConvertibel`/`tellerFactorNaar`
(frontends/regelspraak/.../model/Eenheid.java, ~r. 79-128) vallen bij
een naam-mismatch stil terug op factor één — gemeten de motor achter
het kastkanaal (vóór de byte-vergelijking van WF-091: ref 3,75 tegen
3.750,00 in het Drools-pad) en strijdig met de
fail-fast-grondregel. Maak de terugval luid (maintaineropdracht
2026-09-04, tweede golf).

Aanpak: EERST de aanroepersinventaris — wie roept de twee methodes (en
`omrekenfactorNaar`/`vereistOmrekening` als die dezelfde terugval
delen): de frontend-Typering, de DRL- en exec-model-transpilers via het
gedeelde model, en meet per aanroeper of een naam-mismatch daar ná de
EffectieveEenheid-kaart nog kán aankomen (verwachting: niet — alle
argumenten komen uit declaraties of de afleiding; de terugval is dan
dood pad). Dan de vervanging: een mismatch die om een factor wordt
gevraagd faalt hard met een dragende melding, of — als een aanroeper de
terugval legitiem nodig blijkt te hebben — die ene plek expliciet en
reden-dragend, nooit stil. Let op de convertibiliteits-poortwachters
(convertibleNaar) die vóór de factorvraag horen te staan: de fout hoort
onbereikbaar te zijn op elk geldig pad, en dát is precies wat de
fail-fast bewaakt.

Acceptance:
- De inventaris met per aanroeper de meting.
- De terugval vervangen door een luide fout (of de dode-pad-conclusie
  met een fail-fast-klem die hem dood hóúdt).
- Gate 10 én 11 byte-identiek (beweegt één golden-emissie: stoppen en
  de vraag stellen); beide JUnit-suites groen (frontend 159, backend
  198); alle dertien gates groen; herpin + re-vendor (frontends
  geraakt).

May touch: `frontends/regelspraak/src/**`, `backlog/`. Nieuwe
frontend-tests alleen na de testbestanden-vraag aan de maintainer.

Out of scope: backends (consumeren het model; hun JUnit bewaakt de
uitkomst), verifier, spec, corpus.

## Bouwnotitie (2026-09-04)

### 1. De aanroepersinventaris — wie vraagt om een factor, en met welke poortwacht

Vier methodes dragen (of droegen) de terugval: `omrekenfactorNaar` (bij
`target == null`), de private `tellerFactorNaar` (bij een naam-mismatch én bij
een onbekende factor) en `vereistOmrekening`, dat de terugval doorgaf als
"nee, geen omrekening nodig". `exacteHerschaalFactoren` hoort er níet bij: die
geeft `null` — een signaal dat elke aanroeper afhandelt, geen verzonnen getal.
`convertibleNaar`/`tellerConvertibel` blijven ongemoeid; die geven true/false
en dat is de publieke semantiek waar de rest op poortwacht.

Elke aanroeper in de repo (gemeten met een grep over `*.java`; buiten
`Eenheid` zelf zijn dit ze allemaal):

| # | plek | vraagt | poortwacht | kan een naam-mismatch aankomen? |
|---|---|---|---|---|
| 1 | `ExpressieBouwer.attribuutRef:1565` (frontend) | `vereistOmrekening` | JA — `convertibleNaar(named) &&` ervóór | ja, en dat is het punt: het achtervoegsel "in ⟨eenheid⟩" is brontekst ("in jaren" op een mnd-attribuut). De poortwacht vangt het: niet-inwisselbaar ⇒ geen naamstrip ⇒ marker |
| 2 | `Typering:176,202,330,332,344,394` (frontend) | ALLEEN `convertibleNaar` | n.v.t. | stelt de factorvraag nooit — gemeten: de enige frontend-factoraanroeper is rij 1 |
| 3 | `EffectieveEenheid.eenheid:201,202` (backend) | ALLEEN `convertibleNaar` | n.v.t. | idem: de MAAL-cancelclausule vraagt convertibiliteit, geen factor |
| 4 | `ExecModelTranspiler.valueFactor:4416` | `vereistOmrekening` + `omrekenfactorNaar` | JA, in dezelfde `||`-keten | nee — en gemeten dat hij vuurt: 92 asks in gate 12, alle op inwisselbare paren |
| 5 | `ExecModelTranspiler.exactHerschaalFactoren:4444` | `vereistOmrekening` + `exacteHerschaalFactoren` | JA | nee; 96 asks in gate 12 |
| 6 | `ExecModelTranspiler.restatement:4635` | `vereistOmrekening` + `omrekenfactorNaar` (noemer-tak) | JA — `convertibleNaar` staat vóór de `||` | nee. Dít is het pad dat de EffectieveEenheid-kaart (B3-2) mat: bij niet-inwisselbaar geeft het de IDENTITEIT `{1,1}` uit eigen hand, één regel vóór de factorvraag. Het is dus GEEN aanroeper van de terugval — de kortsluiting bereikt `tellerFactorNaar` daar nooit |
| 7 | `ExecModelTranspiler.eenheidExactHerschaalbaar:4426` | `convertibleNaar` + `exacteHerschaalFactoren` | JA | nee (en `exacteHerschaalFactoren` verzon sowieso niets) |
| 8 | `ExecModelTranspiler.tijdreeksSomAlleSet:1151`, `schaalTijd:2034` | `vereistOmrekening` | JA | nee |
| 9 | `DrlTranspiler.schaalbaarNaarDoel:2503`, `schaalTijd:2546`, `vergelijkingsOperanden:3781`, `herschaalExact:3808`, `omrekenFactor:3821`, `herschaalConditie:3856` | `vereistOmrekening`, `omrekenfactorNaar`, `exacteHerschaalFactoren` | JA, alle zes | nee |
| 10 | **ONGEPOORTWACHT**: `DrlTranspiler.vergelijkingTijdreeks:2273`, `geheelPeriode82:2704`, `geheelPeriode82Set:2758`; `ExecModelTranspiler.vergelijkingClip:1656`, `:2453`, `:2519`, `grensOnveilig:4436`, `parityOnveilig:4520` | `vereistOmrekening` kaal | NEE | **JA.** Dit zijn de acht plekken die de vraag stellen als *predikaat* — "is dit een cross-unit-plek?" — over twee operanden waarvan de convertibiliteit nog niet vaststaat |

Rij 10 is dus de aanroeper die de terugval legitiem nodig had. "De argumenten
komen na de EffectieveEenheid-kaart uit declaraties" is waar, maar het maakt
een mismatch niet onmogelijk: twee GEDECLAREERDE eenheden hoeven niet
inwisselbaar te zijn. Het corpus draagt de vormen die dat kunnen:

- negen eenheidsvormen met `canoniek: null` en `factorNaarCanoniek: null`
  (`dag`, `dagen`, `jaar`/`jr`, `kw`, `maand`, `mnd`, `uur`, `week`, `wk` — de
  WF-038-tijdsduurparameters en de WF-035-duuroperanden; 24 voorkomens);
- twee canonieken binnen één stelsel (`Tijd` heeft `mnd` én `dg` — precies de
  appel/peer-vorm die `tellerConvertibel`'s eigen javadoc benoemt);
- het opake product (`Eenheid.product`: geen canoniek, geen factor).

Dus: geen dood pad in de zin van "onbereikbaar bij constructie". Wel dood in
de zin van "vandaag niet geraakt" — dat is de volgende meting.

### 2. Wat de drie consumenten vandaag echt vragen (sonde, tijdelijk)

Tijdelijke, niet-gecommitte sonde in `vereistOmrekening`/`omrekenfactorNaar`
(teller per ⟨soort, paar, call-site⟩, weggeschreven bij JVM-afsluiting omdat
gate 12 de stderr van elke replay wegschrijft naar `/dev/null`; geverifieerd
dat `Eenheid$PROBE.class` in de shaded jar zat die de gate draaide, en dat de
sonde zelf treffers boekt). Daarna `git checkout` — de werkboom is schoon,
`grep PROBE` geeft nul.

| run | asks | call-sites die vroegen | mismatches |
|---|---|---|---|
| gate 10 (159 frontend-tests + 28 heruitgiften) | 0 | — | **0** |
| gate 11 (45 toka-heruitgiften, 27 TOKA-tests, 29 replays) | 33 | `ExpressieBouwer.attribuutRef` 14, `restatement` 7, `valueFactor` 6, `exactHerschaalFactoren` 6 | **0** |
| gate 12 (150 goldens: 106 gespeeld + 43 geweigerd, plus 198 JUnit) | 191 | `exactHerschaalFactoren` 96, `valueFactor` 92, `restatement` 3 | **0** |

Van die 224 vragen gaat een handvol over een ECHTE omrekening — de enige
niet-identieke paren die in beide runs voorkomen zijn `m ↔ km` (gate 11 en 12)
en `jaar ↔ maand` (gate 12); al het andere is de identiteit. De acht ongepoortwachte plekken van rij 10 vuurden in geen enkele
run — hun eerdere `el != null && er != null` zeeft ze vandaag weg. De
factorpaden zijn dus levende code, de terugval is een levend BEREIKBARE maar
vandaag ONGERAAKTE tak. Precies het profiel waar een fail-fast-klem thuishoort
en niets kost.

### 3. De vervanging

`frontends/regelspraak/.../model/Eenheid.java`, één commit:

- `omrekenfactorNaar` krijgt de poortwacht als PRECONDITIE:
  `if (!convertibleNaar(target)) throw new IllegalStateException(…)`, met beide
  eenheidsnamen (`weergave()`, dus inclusief noemer) in de melding. Dit dekt
  in één zin de vier gaten die 1 gaven: geen target, teller-mismatch,
  noemer-mismatch, en tarief-tegen-kale-eenheid.
- `tellerFactorNaar` (private, ook aangeroepen voor de NOEMER) weigert de twee
  resterende: een andere canoniek ("ze herleiden niet tot dezelfde canonieke
  eenheid") en een ontbrekende `factorNaarCanoniek` bij gelijke canoniek —
  een wirevorm die het schema toestaat (`canoniek` en `factorNaarCanoniek` zijn
  allebei nullable) en die de buitenste poortwacht juist doorlaat.
- `vereistOmrekening` — rij 10's legitieme aanroeper — doet de
  convertibiliteitscheck nu EXPLICIET en reden-dragend in plaats van hem uit de
  terugval af te lezen: `convertibleNaar(target) && omrekenfactor…`. Antwoord
  ongewijzigd (false, gemeten in de probe hieronder op alle zes vormen); wat
  verandert is dat het antwoord nu ergens op slaat.

`IllegalStateException` is de huisstijl van de gedeelde Java-kant (33
voorkomens in `backends/drools`, Nederlandse melding met de aanstootgevende
naam erin). De frontendbronnen zelf gooien nergens — daar is de
diagnostiek een `Resultaat`-kanaal — maar `Eenheid` is een waardetype zonder
zo'n kanaal, en de drie backends-consumenten lezen het als bibliotheek.

### 4. De probe — letterlijke uitvoer vóór en ná

`/tmp/eenheid-probe/EenheidProbe.java`, tweemaal gedraaid tegen dezelfde
gebouwde `out/`: één keer met `Eenheid.class` van HEAD ervóór op het
classpath, één keer met de werkboomversie.

**VÓÓR** (elke mismatch levert stil factor 1):

```
== MISMATCH: om een factor vragen tussen niet-inwisselbare eenheden ==
KG(kastleugen) -> t (de gemeten motor)     convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1
EUR -> kg          (twee stelsels)         convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1
jr(Tijd) -> dg(Tijd) (appel vs peer)       convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1
jaar(kaal) -> mnd  (corpusvorm, canoniek null) convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1
m*m(opaak) -> m    (B3-2's gemeten paar)   convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1
EUR/jr -> EUR/kg   (noemer-mismatch)       convertibel=false vereistOmrekening=false exact=null  omrekenfactor=1

== WIREVORM: canoniek gelijk, factor ontbreekt (schema laat het toe) ==
x(canoniek m, factor null) -> m            convertibel=true  vereistOmrekening=false exact=null  omrekenfactor=1

== GELDIG: inwisselbare paren, de factor moet blijven staan ==
jr -> mnd                                  convertibel=true  vereistOmrekening=true  exact={12,1} omrekenfactor=12
mnd -> jr                                  convertibel=true  vereistOmrekening=true  exact={1,12} omrekenfactor=0.08333333333333333
EUR -> EUR                                 convertibel=true  vereistOmrekening=false exact={1,1}  omrekenfactor=1
EUR/jr -> EUR/mnd                          convertibel=true  vereistOmrekening=true  exact={1,12} omrekenfactor=0.08333333333333333
m*m -> m*m         (identiek product)      convertibel=true  vereistOmrekening=false exact=null   omrekenfactor=1
```

**NÁ** (de mismatchblokken; het GELDIG-blok is byte voor byte gelijk aan
hierboven — zelfde factoren, zelfde `vereistOmrekening`, zelfde `exact`):

```
KG(kastleugen) -> t (de gemeten motor)  … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'KG' naar 't': niet inwisselbaar
EUR -> kg          (twee stelsels)      … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'EUR' naar 'kg': niet inwisselbaar
jr(Tijd) -> dg(Tijd) (appel vs peer)    … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'jr' naar 'dg': niet inwisselbaar
jaar(kaal) -> mnd  (corpusvorm)         … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'jaar' naar 'mnd': niet inwisselbaar
m*m(opaak) -> m    (B3-2's paar)        … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'm·m' naar 'm': niet inwisselbaar
EUR/jr -> EUR/kg   (noemer-mismatch)    … omrekenfactor=IllegalStateException: geen omrekenfactor van eenheid 'EUR/jr' naar 'EUR/kg': niet inwisselbaar

x(canoniek m, factor null) -> m   convertibel=true  vereistOmrekening=IllegalStateException: onbekende omrekenfactor van 'x' (null) naar 'm' (1): canoniek 'm' zonder gedeclareerde factorNaarCanoniek
```

In alle zes de mismatchrijen blijft `vereistOmrekening` **false** en
`convertibleNaar` **false** — de publieke antwoorden bewegen niet, alleen de
factorvraag zelf.

### 5. Wat dood pad bleek, en wat de fout nu bewaakt

- **Dood pad (bij constructie):** `omrekenfactorNaar(null)` — alle vier de
  aanroepers hebben een non-null target; en de restatement-identiteit van rij 6
  bereikt de terugval nooit, want `convertibleNaar` staat vóór de `||`.
- **Dood pad (gemeten, vandaag):** de mismatch zelf — 0 treffers over gate 10,
  11 en 12 samen (224 factorvragen, allemaal op inwisselbare paren).
- **Niet dood:** de wirevorm `canoniek` zonder `factorNaarCanoniek`. Het schema
  laat hem toe, geen enkele WF-regel verbiedt hem, en hij glipt door
  `convertibleNaar` heen. Vóór deze kaart gaf hij factor 1; nu noemt hij zich.
  Het corpus draagt hem niet (nagemeten over alle 204 `ir.json` in
  `corpus/golden`, `corpus/rood` en `toka/`: 32 eenheidsvormen, geen enkele met
  een canoniek zonder factor). Dit is de enige gedragswijziging die verder gaat
  dan "fout in plaats van 1": daar antwoordde `vereistOmrekening` false, nu
  gooit hij.

### 6. Suites en gates

Frontend `make -C frontends/regelspraak test`: **159/159**, 0 overgeslagen.
Backend `mvn -o -f backends/drools/pom.xml test`: **198/198**, 0 overgeslagen,
BUILD SUCCESS.

```
 1 check-self-contained: PASS
 2 check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
 3 check-verifier: PASS (174 golden clean; 238 negatief rejected for their declared rule)
 4 check-case-schema: PASS (schema valid; 425 case file(s) validate)
 5 check-oracle: PASS (403 cases replayed exactly; NOT replayed, no case file yet: jutterswet-seizoenvondst jutterswet-vormenkabinet)
 6 check-clingo: PASS (403 cases replay exactly through clingo — ref = clingo over the corpus)
 7 check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
 8 check-rood: PASS (22 red cases fault observably in ref and clingo; Drools leg 5 fault-pinned, 17 not-carried)
 9 check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically
10 check-regelspraak: PASS — JUnit suite green (159 tests); 28 rgs golden(s) re-emit byte-identically
11 check-toka-parity: PASS — 45 .rgs byte-identiek (2 bekende holdouts), 29 replays: ref 29/0, clingo 29/0, Drools 29 PLAYED / 0 EXCLUDE / 0 FAIL
12 check-drools: PASS — played 106, auto-excluded 43, value-divergence 1, no-case 2, JUnit 198 tests
13 check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 39 executor replays reproduce the untouched outcome; 0 pinned survivors)
```

**Er is geen byte bewogen.** Gate 10 (28 rgs-heruitgiften) en gate 11 (45
toka-heruitgiften) zijn byte-identiek; gate 12 staat op elk getal gelijk aan de
gedocumenteerde main-basislijn (`played 106 / auto-excluded 43 /
value-divergence 1 / no-case 2`, zie `20260903-wf007-schrijfdoel`), met dezelfde
43 weigergronden in dezelfde vormen. Gate 13's tellingen (52/11/39/84) zijn
structureel — het aantal verdraaide annotaties — en zijn ongevoelig voor deze
wijziging; wat zij bewaakt is de nul-divergentie, en die staat.

### 7. De test die ik zou toevoegen (testbestanden-vraag staat open)

Geen nieuw testbestand gemaakt, geen bestaand testbestand aangeraakt. Wat ik
zou toevoegen, als de maintainer het toestaat:
`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/EenheidFactorTest.java`,
~12 getuigen (159 → ~171):

1. zes weiger-getuigen, één per mismatchvorm uit §4 (kastleugen `KG`→`t`, twee
   stelsels, twee canonieken in één stelsel, de kale corpusvorm, het opake
   product, de noemer-mismatch), elk `assertThrows` + `assertTrue` dat BEIDE
   namen in de melding staan;
2. de wirevorm-getuige (canoniek zonder factor) met haar eigen melding —
   de klem die de dode tak dood houdt;
3. `omrekenfactorNaar(null)` gooit;
4. drie behoud-getuigen: `jr→mnd` = 12, `mnd→jr` = 1/12 op DECIMAL64,
   `€/jr→€/mnd` = 1/12 (de noemerinversie);
5. één predikaat-getuige: `vereistOmrekening` is false op alle zes de
   mismatchvormen — het contract waar rij 10 van de inventaris op leunt, en
   precies wat een latere "gooi ook daar maar" onbedoeld zou breken.

Mutatiemeting die erbij hoort: met `return BigDecimal.ONE` teruggezet vallen 1,
2 en 3 om (negen getuigen) en blijven 4 en 5 staan — de scheiding tussen wat
de kaart repareert en wat zij expliciet ongemoeid laat.

### 8. Eerlijk open

- **Eén verouderde zin in een bestand dat deze kaart niet mag aanraken.**
  `backends/drools/.../kern/EffectieveEenheid.java:165-167` citeert een meting:
  "for m·m against m, `convertibleNaar` is false, `vereistOmrekening` is false,
  `omrekenfactorNaar` is 1 and `exacteHerschaalFactoren` is null". Na deze kaart
  klopt de derde clausule niet meer: `omrekenfactorNaar` GOOIT daar nu. De
  eerste, tweede en vierde staan nog (probe §4, rij `m*m -> m`), en de conclusie
  die de alinea trekt — het exec-pad maakt een niet-converteerbare herschaling
  tot een no-op — is ongewijzigd, want dat pad komt uit `restatement`'s eigen
  identiteit (rij 6), niet uit de factorvraag. Eenregelige naregel voor de
  maintainer of een volgende backendkaart.
- **De ongepoortwachte rij-10-plekken blijven stil.** Zij geven bij een
  niet-inwisselbaar paar nog steeds "geen omrekening nodig" en vergelijken dan
  rauwe magnitudes — B3-2's gemeten stille ref/Drools-divergentie. Deze kaart
  maakt dat expliciet en reden-dragend, maar lost het niet op: dat is een
  backendbesluit (weigeren of markeren) op een vorm die het corpus niet draagt
  en die ref én clingo weigeren. Kaartmateriaal, geen sluipende uitbreiding.
- De sonde uit §2 meet ONVERSCHILLIGHEID van vandaag, niet onmogelijkheid van
  morgen: nul treffers zegt dat het corpus + de toka-zone de mismatch niet
  aanbieden, niet dat geen enkel document dat kan. Dat is precies waarom de
  terugval een fout werd in plaats van een aanname.
