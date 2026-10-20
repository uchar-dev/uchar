# impliciete-tijd: rekenen en voorwaarden per periode over tijdlijnen, naar het voorbeeld van ALEF (voorstelkaart)

*Opgesteld op 2026-10-10 door een verkenningssessie op verzoek van de eigenaar, na het lezen van de presentaties van
Rules as Code Europe 2026 (Den Haag, 10–11 maart 2026) in de openbare GitLab-groep `digicampus/gt4a` en van de
broncode van ALEF. Nog niet door de hoofdsessie ingepland. Regelnummers op uchar `a1c21727`. De presentatie
"Implicit Temporal Reasoning for Executable Legal Rules" van de Belastingdienst staat in de map `public/rac2026/` van
`https://gitlab.com/digicampus/gt4a/sga2-wp8` (commit `118317d`) en wordt aangehaald met dianummers. De broncode: de
EUPL-1.2-uitgave van ALEF door de Belastingdienst, `https://github.com/belastingdienst/ALEF`, op `872b8595`
(2026-07-02, branch `main`); paden hieronder relatief aan
`AlefJava/alefRuntime/src/main/java/nl/belastingdienst/alef_runtime/`, tenzij anders vermeld.*

## Aanleiding

In de presentatie "Implicit Temporal Reasoning for Executable Legal Rules" liet de Belastingdienst zien hoe ALEF, haar
RegelSpraak-omgeving, met tijd omgaat. De opsteller schrijft een regel zonder tijd, en ALEF leest die regel per
periode over de tijdsafhankelijke gegevens die hij leest: de operatoren en ook de voorwaarden. Daarnaast is er een kleine set expliciete tijdbewerkingen. De broncode van ALEF bevestigt dat en maakt
het op een aantal punten precies.

Dat raakt de open rijen van RUNTIME-SEMANTICS ch. 3.12 (r. 3054–3090): tijdlijnrekenen buiten plus en min ("per-period
lifting of ch. 1.2 rules is the candidate"), een scalair naast een tijdlijn, minimum en maximum over tijdlijnen, en het
onvoorwaardelijke tijdsafhankelijke kenmerk. Het raakt ook een gelande clausule: het totaal over een gedeeltelijke
eenheid (ch. 3.6). uchar spiegelt `regelspraak-java`; ALEF is de uitvoering van de eigenaar van de brontaal, en waar
die twee uiteenlopen, hoort uchar dat te weten en te besluiten.

Rechten. De presentatie draagt geen licentie; ALEF is EUPL-1.2, uchar Apache-2.0 en CC-BY-4.0. Deze kaart en het
voorstel beschrijven beide in eigen woorden en verwijzen naar dia, bestand en regel; er gaat geen tekst of code uit
over naar uchar, en het corpus volgt de corpusregels van `CLAUDE.md`.

Goal: een voorstel `spec/proposals/IMPLICIT-TIME.md` dat de lezing per periode voor uchar uitwerkt in genummerde
besluitpunten, elk met een uitgewerkt voorbeeld met absolute waarden; met een meting van wat elke uitvoerder vandaag
met de vormen uit de presentatie doet; en met de tekst van de bouwkaarten die erop volgen, in een volgorde die past bij
de twee TMS-kaarten. Deze kaart verandert geen spec, corpus of uitvoerder.

## Wat de presentatie laat zien

1. **Een tijdloze regel, per periode gelezen** (dia 17, 18, 20). "Betaalt belasting als hij meer dan 10 000 euro
   verdient" betekent per kalenderjaar, naar het inkomen van dat jaar. Een som van drie tijdsafhankelijke inkomens met
   verschillende periodegrenzen wordt per deelperiode tussen alle grenzen gerekend; een operand zonder waarde telt
   daar niet mee. Nagerekend: de elf uitkomsten van dia 20 kloppen met ch. 3.4 en ch. 3.5 (geen deelperiode heeft er
   alle operanden leeg, dus de twee leeg-regels geven hier hetzelfde).
2. **Een tijdsmodus per attribuut en feittype** (dia 19, 21): een eenheid (dag, week, maand, jaar) en een startdatum;
   een bedrag in €/jr kan "voor elke maand" gelden.
3. **Expliciete tijdbewerkingen** (dia 22–33, lijst op dia 28): een kenmerk van datum tot datum (dia 22); gehele maand
   en deel van de maand, elk als waarheidstijdlijn per maand (dia 29); verschuiven naar het begin van de maand of van
   de volgende maand (dia 30); eenmalig, waar vanaf het eerste moment dat de voorwaarde geldt en zonder einde (dia 31);
   het aantal dagen per maand, met 0 voor een maand zonder ware dag (dia 32); het tijdsevenredig deel (dia 33); het
   totaal over een periode (dia 27, 35).
4. **Voorwaarden per periode** (dia 23–25). Eén regel telt per maand de rentedragende dagen die geen schrikkeldag
   zijn, maar alleen in een maand die niet geheel rentedragend is; een tweede regel zet 30 dagen per maand in de
   maanden die dat wel zijn, februari inbegrepen. Samen vullen de twee regels één tijdlijn, elk in eigen maanden.
5. **Een rentevoorbeeld in twee formuleringen.** Bedrag 10 000 €, vervaldatum 2025-11-15, betaaldatum 2026-03-12,
   rentevoet 4 %/jr tot 2026-02-01 en 5 %/jr daarna, als parameter per dag. Rentedragend is de periode van de dag na de
   vervaldatum tot en met de betaaldatum (de dagtellingen van dia 24 en 35 tellen de betaaldatum mee).
   - **A** (dia 21–27), 30/360: rente per maand = rentevoet × bedrag × rentedagen / 360 dagen per jaar: 16 2/3,
     33 1/3, 33 1/3, 41 2/3 en 16 2/3 €; totaal **141 2/3 €**. Klopt.
   - **B** (dia 34–35), werkelijke dagen: de rentevoet op het bedrag gedurende de tijd dat de betaling rentedragend is,
     getotaliseerd: 77/365 × 400 € + 40/365 × 500 €. De dia geeft 84 134/365 € voor de eerste term en 138 59/365 € als
     totaal; juist is 84 28/73 € (84 140/365) en **139 13/73 €** (≈ 139,18 €). Ook de eigen termen van de dia tellen
     op tot 139 59/365 €, niet tot 138 59/365 €.
   - De presentatie zet B neer als de eenvoudiger schrijfwijze van A, maar het zijn verschillende regels: ze
     verschillen 545/219 € (≈ 2,49 €). Zo'n verschil en de rekenfout op dia 35 worden zichtbaar in een ankercasus met
     handgerekende absolute waarden; daarvoor bestaat die.
   - De eenheden van de presentatie sluiten niet: de rentedagen zijn een getal zonder eenheid (dia 21) en worden
     getoond als dagen per maand (dia 24–26); de rente is €/jr per maand (dia 21) en wordt getoond als € per maand
     (dia 26). De ankercasus legt de eenheden zelf vast.

## Wat de broncode van ALEF daaraan toevoegt

Gelezen, niet gedraaid, tenzij een test genoemd wordt.

- **Een tijdlijn bestrijkt alle tijd.** `time/ITimed.java` r. 15–20: de waarden liggen in aaneengesloten vakken van
  `OPEN_BEGIN` tot `OPEN_END`; leeg is een vak met waarde null. Een scalair wordt `Always.of(waarde)` (r. 165–170).
  Zo ontloopt ALEF het tijddomeinprobleem van ch. 3.4 en 3.11, en "eenmalig" en open perioden zijn gewone waarden
  (`TestTimeOperations.testRedistribute`, het tweede deel).
- **Lifting is puntsgewijs.** `time/Time.java` r. 271–308: per moment de doorsnede van de vakken van de operanden, de
  waarden versneden volgens hun `TimeMode`, en dan de scalaire functie op die waarden, null inbegrepen. De tests
  (`TestLifting`, `TestNullLifting`) eisen per moment `f(a(t), b(t))`. De leeg-regels zitten dus in de scalaire
  operator; `BigRational.add`, `subtract` en `multiply` tellen null als 0 (`BigRational.java` r. 124–141). Niet
  nagegaan: of de generator precies die operatoren voor tijdsafhankelijk rekenen gebruikt. Zo ja, dan geeft ALEF
  buiten de perioden van de operanden 0 waar ch. 3.4 een gat geeft.
- **Twee manieren om een waarde over een vak te lezen.** `time/TimeMode.java`: `CONSTANT` (de waarde geldt op elk
  moment) en `PROPORTIONAL`, `PROPORTIONAL_MONTHS` (een bedrag over het vak, dat bij versnijden naar rato meegaat).
  Dat is ALEF's antwoord op een fijnere operand in een grover doel.
- **Twee schrijvers per vak.** `ITimed.merge` (r. 99–106): een fout ("Meervoudige ongelijke waarde toekenning") alleen
  in een vak waar beide gevuld en ongelijk zijn.
- **Een voorwaarde per periode.** `ITimed.when(IValidity)` (r. 109–115) maakt de waarde leeg waar de voorwaarde niet
  geldt; `time/IValidity.java` heeft `and`, `or`, `negate`, `ifThen`, `iif` en de tellende vormen `atLeastNOf`,
  `atMostNOf`, `exactlyNOf`, alle per periode.
- **Gehele en gedeeltelijke periode per emmer.** `time/TimeLine.java` r. 222–251: `wholePeriods`, `partialPeriods`,
  `notWholePeriods` geven een waarheidstijdlijn op het raster, geen vouw. De vertaling van `HeleTijdvak` in
  `languages/regelspraak.tijd/models/regelspraak.tijd.translator.mps` gebruikt ze.
- **Dagentelling.** `TimeLine.getDuration` (r. 371–375) = de telling per periode, aangevuld met 0
  (`orElse(BigRational.ZERO)`). `TestTimeOperations.testDuration`: 0 voor elke maand zonder ware dag, vóór en na de
  ware periode. Het rentevoorbeeld staat zelf in de tests (`testDuration2`: rentedragend en geen schrikkeldag als
  samengestelde geldigheid, februari 2016 telt 4 dagen).
- **Integratie naar rato van de werkelijke lengte.** `TimeLine.timesDuration`, `lengthOf` en `partialLength`
  (r. 295–344): een gedeeltelijke eenheid telt als dagen ÷ dagen van de omsluitende kalendereenheid, dus 365 of 366
  per jaar. `testRedistribute`: 400 €/jr tot 2021-05-15 en 500 €/jr daarna, rentedragend op [2021-04-01, 2021-07-21),
  geeft precies 140 € (44 × 400/365 + 67 × 500/365). De vertaling van het totaal gebruikt `timesDuration` (gelezen in
  het vertaalmodel).
- **Verschuiven.** `TimeLine.round(valid, TimeRoundingMode)` (r. 95–107) rondt de grenzen van een geldigheid af naar
  het raster. In de taal `regelspraak.tijd` is dat het concept `TijdlijnConversie`, met `afronding` (`VANAF_BEGIN`,
  `VANAF_VOLGENDE`) en een vlag `naEerste` (betekenis niet nagegaan). Verschuiven is dus RegelSpraak zoals ALEF het
  spreekt, al heeft de grammatica van RegelSpraak v2.3.0 in `frontends/regelspraak` er geen vorm voor.

## Waar uchar staat (op `a1c21727`)

| Vorm | Dia | uchar |
|---|---|---|
| plus en min per periode | 20 | gespecificeerd, ch. 3.4 (r. 1001–1052); zie "Uitvoerders" |
| som per periode | 20 | gespecificeerd, ch. 3.5; zie "Uitvoerders" |
| maal, gedeeld door, percentage van, verminderd met | 26 | OPEN (ch. 3.12) |
| een scalair of een parameter naast een tijdlijn | 26 | OPEN: het tijddomein (ch. 3.4, 3.12); een tijdsafhankelijke parameter alleen via `TemporalRead` (ch. 3.3, 8.3) |
| een voorwaarde per periode op een tijdsafhankelijk doel | 23–25 | een voorwaarde is een poort voor de hele schrijfactie (ch. 8.2); een tijdsafhankelijke operand in een voorwaarde wordt buiten de collapses geweigerd (ch. 3.3); de periodisering komt van de producent, "never from evaluating a scalar rule 'once per period'" (ch. 3.11, r. 2993–2995) |
| gehele maand, deel van de maand | 29 | ch. 3.9 (r. 2363–2371): één vouw over alle aangeraakte emmers tot één waarheidswaarde |
| aantal dagen per maand | 24, 32 | ch. 3.8, uitgevoerd; de voorwaarde moet een eigen tijdsafhankelijk kenmerk zijn; een emmer zonder ware dag geeft niets |
| tijdsevenredig deel | 33 | ch. 3.7, uitgevoerd (met omheinde varianten) |
| totaal over een periode | 27, 35 | ch. 3.6, uitgevoerd, met integratie over een tarief en de knip met "gedurende"; per fragment alleen hele eenheden ("floor, never pro-rata") |
| van datum tot datum | 22 | ch. 3.10: de knip op een attribuutdoel; op een kenmerkdoel geweigerd (rood `periodevenster-tijdlijnkenmerk`); het onvoorwaardelijke kenmerk OPEN (ch. 3.11); een grens is een datumliteraal of een eigen scalair datumattribuut (WF-111), dus niet "de dag na de vervaldatum" |
| verschuiven, eenmalig | 30, 31 | geen tegenhanger |
| twee regels op één tijdlijndoel, elk in eigen perioden | 25 | ch. 5.7 (r. 3813): twee schrijvers wier voorwaarden beide gelden, met verschillende waarden, geven een fout; de tak met truth maintenance weigert een tweede schrijver van een tijdlijndoel |
| een rentevoet per dag in een rente per maand | 21, 26 | ch. 3.4 eist een operand die grover of even grof is als het doel; rij 11 van `spec/CONSTRUCTS.md` noemt automatische granulariteitsconversie "later" |
| 30 en 360 dagen | 25–26 | geen conventie nodig: de regels schrijven 30 dagen per maand en 360 dagen per jaar uit (ch. 3.2: "There is no 360-day convention anywhere") |
| tijdsafhankelijk feittype | 19 | geen: het schema geeft alleen `Attribute`, `Characteristic`, `Parameter` en `StartPointDetermination` een tijdlijn |

**Uitvoerders.** ch. 3.4 en 3.5 zijn gespecificeerd, maar geen golden en geen OCaml-uitvoerder schrijft ze via een
regel.
- `ref` heeft ze als bibliotheekfunctie (`ref/lib/timeline.ml` r. 408–419 `cut_merge`, `plus`, `min_`, en r. 427
  `som`), alleen bereikt vanuit de WV-3.4-tests; de evaluator weigert een tijdsafhankelijk attribuut op een
  waardepositie (`ref/lib/eval.ml` r. 890–899).
- clingo vertaalt drie lezers en twee producenten (ASP-TRANSLATION S9) en weigert een tijdsafhankelijk attribuut of
  een granulaire parameter op een scalaire positie (`backends/clingo/lib/translate.ml` r. 3723, 3767).
- Drools: de destructieve exec-modeltak en de DRL-teksttak verlagen plus en min (`rsTijdReken`,
  `RegelSpraakRuntime.java` r. 1206; `ExecModelTranspiler.java` r. 2726; `DrlTranspiler.java` r. 3752) en de som per
  periode (`rsTijdSomAlle`, r. 1237). De tak met truth maintenance schrijft een tijdlijn alleen met een tijdsevenredig
  deel of een dagentelling (`backends/drools/README.md` r. 500).
- Corpus, gemeten over `corpus/golden/**/ir.json`: van de 17 regelversies met een tijdsafhankelijk doel is de waarde
  10 keer een tijdsevenredig deel en 7 keer een dagentelling; 4 hebben een voorwaarde (3 keer een `TimeWindow` in
  `jutterswet-pachtvenster`, 1 keer een scalaire vergelijking in `synth2-prorata-breed`); geen enkele draagt een
  gehele-periodekwalificator.

## Het principe dat het voorstel moet vastleggen

**De lezing per dag.** Voor elke operator en voorwaarde zonder eigen tijdsemantiek is de betekenis over tijdlijnen de
scalaire betekenis per dag: ch. 1.2 per dag voor leeg, ch. 5.1 per dag voor de vergelijking die een regel is. ch. 3.4
is dat al voor plus en min: `cut_merge` past de scalaire operator per deelperiode toe, en de period-coalescing rule is
"leeg + leeg = 0" uit ch. 1.2, binnen de span. Maal, gedeeld door, percentage van en verminderd met volgen dan zonder
nieuwe leeg-regels; alleen deling door leeg vraagt een melding op het §9.5-kanaal per periode. De bewerkingen met een
eigen tijdsemantiek (gehele periode, dagentelling, tijdsevenredig deel, totaal, en eventueel verschuiven en eenmalig)
houden hun eigen definitie. ALEF werkt zo (`Time.lift`).

**Het tijddomein** is het obstakel dat ch. 3.4 en 3.11 noemen: per dag gelezen is 5 + leeg overal 5, ook buiten elke
periode. Twee kandidaten:
- *De span* (dicht bij uchar nu): een scalaire operand levert geen knippen; het domein is de span van de
  tijdlijnoperanden, zoals in ch. 3.4; een regel zonder tijdlijnoperand maar met een periodevenster (ch. 3.10) krijgt
  het venster als domein. Dat dekt "van datum tot datum" en het onvoorwaardelijke kenmerk binnen een venster; zonder
  een van beide blijft het OPEN, en eenmalig past er niet in.
- *Alle tijd* (ALEF): een tijdlijn bestrijkt alle tijd, leeg buiten haar perioden, met een segment zonder begin of
  einde als gewone waarde. Dat dekt broadcast, eenmalig en open perioden, maar raakt ch. 3.1 (gaten, geen open
  segmenten), het caseformaat en de ASP-codering, en mogelijk de gat-clausule van ch. 3.4.

## Besluitpunten (genummerd, voor het voorstel)

1. **Rekenen per periode.** Maal, gedeeld door (beide varianten), percentage van en verminderd met over tijdlijnen,
   per dag gelezen met de leeg-regels van ch. 1.2; eenheden per ch. 2.6, het resultaat eenmaal herrekend naar het
   doel. Daarbinnen open: welke periode noemt de §9.5-melding bij een deling door leeg?
2. **Het tijddomein**, met een scalair of een tijdsafhankelijke parameter als operand: de span of alle tijd (zie
   boven). Een tijdsafhankelijke parameter als tijdlijnoperand is nu alleen bereikbaar via `TemporalRead`; ref en
   clingo weigeren hem op een scalaire positie, de tak met truth maintenance weigert hem helemaal.
3. **Een voorwaarde op een regel met een tijdsafhankelijk doel**: per periode (de regel schrijft waar de voorwaarde
   geldt; een knip op haar waar-perioden en daarmee de veralgemening van de vensterknip van ch. 3.10; ALEF:
   `ITimed.when`) of, zoals nu, een poort voor de hele schrijfactie. Bij "per periode" wordt een tijdsafhankelijke
   operand in zo'n voorwaarde toelaatbaar; voor een scalair doel blijft ch. 3.3. Vast te stellen tegen de
   RegelSpraak-specificatie (§5.1.4, §8.2, §8.4; alleen paragraafnummers in de repo) en tegen ALEF.
4. **De gehele-periodekwalificator op zo'n regel**: per emmer, als waarheidstijdlijn op de emmers van de kwalificator
   (ALEF: `wholePeriods`, `notWholePeriods`), of één vouw (ch. 3.9). Gemeten: geen golden zet de kwalificator op een
   regel met een tijdsafhankelijk doel, dus de lezing per emmer verandert daar geen uitkomst. Voor een scalair doel
   is de vouw gepind (`synth-gehelejaarkenmerk`, `jutterswet-ploegpremie`) en blijft ze.
5. **Twee schrijvers van één tijdlijndoel**: ch. 5.7 per dag, met een fout alleen op dagen waar beide schrijven en de
   waarden verschillen (ALEF: `ITimed.merge`), of zoals nu.
6. **Van datum tot datum**: een tijdsafhankelijk kenmerk binnen een venster als schrijfvorm (de rode casus
   `periodevenster-tijdlijnkenmerk` gaat dan over in een golden), en of WF-111 een datum plus of min een duur als
   grens toelaat ("de dag na de vervaldatum"; ch. 3.12 noemt de algemene datumexpressie een OPEN wirebesluit).
7. **De dagentelling**: geeft een emmer zonder ware dag 0 (ALEF, `getDuration`) of niets (ch. 3.8)? Mag de voorwaarde
   een samenstelling zijn met een dagsoorttoets op de getelde dag ("geen schrikkeldag", ALEF `testDuration2`)? En
   welke eenheid draagt de telling: nu geen (ASP-TRANSLATION S9), terwijl de rentevorm dagen vraagt, zodat "360 dagen
   per jaar" de eenheden sluit.
8. **Een fijnere operand in een grover doel** (een rentevoet per dag in een rente per maand): weigeren (nu, ch. 3.4),
   constantheid per emmer eisen en anders luid falen, of naar rato versnijden zoals ALEF's `PROPORTIONAL`-modus.
9. **Minimum, maximum, eerste en laatste over tijdlijnen**: per periode over de operanden, of over de tijd van één
   tijdlijn (ch. 3.5 slot, ch. 3.12). Twee vormen, niet één dubbelzinnige.
10. **Verschuiven en eenmalig** (dia 30, 31): ALEF's dialect heeft verschuiven (`TijdlijnConversie`), de grammatica
    van uchar's frontend niet. Vastleggen, en pas aannemen als een brontaal van uchar erom vraagt (het beginsel van
    rij 35: "adopt, don't invent"). Eenmalig hangt aan besluitpunt 2.
11. **De dagtelconventie bij integratie.** ch. 3.6 laat de gedeclareerde factor beslissen, niet de kalender: met
    jr = 365 dg rekent formulering B "werkelijk/365". ALEF rekent met de werkelijke lengte van het kalenderjaar
    (`partialLength`). In 2025 en 2026 valt dat samen; in een schrikkeljaar niet (ankercasus, geval 2).
12. **Het totaal over een gedeeltelijke eenheid** (geland, ch. 3.6): uchar telt per geknipt fragment alleen hele
    eenheden, gepind door de split-floor-casus; ALEF telt een gedeeltelijke eenheid naar rato van haar werkelijke
    lengte (`timesDuration`, `testRedistribute` = 140 €, waar de vloer van ch. 3.6 bij een jaarrooster 0 zou geven).
    Dat is een verschil in gelande semantiek tussen de spiegel van `regelspraak-java` en de uitvoering van de
    eigenaar. Het voorstel stelt vast wat de RegelSpraak-specificatie zegt (§4.20, het totaal) en adviseert: houden,
    of als gevolgde afwijking (`spec/CONSTRUCTS.md`) bijstellen.
13. **Tijdsafhankelijke feittypen** (dia 19): vastleggen als later.

## De ankercasus

In eigen woorden in de Jutterswet, bijvoorbeeld verzuimrente over een te late afdracht (namen voorlopig), met de
getallen van de presentatie, zodat de uitkomst naast die van ALEF kan liggen. Rentedragend van de dag na de
vervaldatum tot en met de betaaldatum. Formulering A met eenheden die sluiten: rente per maand in €, rentedagen in
dagen, 360 dagen per jaar. Formulering B met een rente per dag in €/jr, een rooster per dag en jr = 365 dg; op een
jaarrooster gaf de vloer van ch. 3.6 voor beide gevallen 0.

| Geval | A (30/360), beide | B, uchar (jr = 365 dg) | B, ALEF (gelezen) |
|---|---|---|---|
| 1: 10 000 €, verval 2025-11-15, betaald 2026-03-12, 4 %/jr tot 2026-02-01, daarna 5 %/jr | nov 16 2/3, dec 33 1/3, jan 33 1/3, feb 41 2/3, mrt 16 2/3; totaal **141 2/3 €** | 400 × 77/365 + 500 × 40/365 = **139 13/73 €** | gelijk, **139 13/73 €** |
| 2: idem, verval 2028-02-10, betaald 2028-04-04, 5 %/jr | feb 18 rentedagen (19 min de schrikkeldag) → 25; mrt 30 → 41 2/3; apr 4 → 5 5/9; totaal **72 2/9 €** | 500 × 54/365 = **73 71/73 €** | 500 × 54/366 = **73 47/61 €** |

Geval 1 vraagt in A de besluitpunten 1–7 (en 8 met een rentevoet per dag), in B de punten 1, 2, 6 en 11; geval 2
pint de schrikkeldagtoets (7) en de conventie (11). Een lege betaaldatum is geen ankerwaarde maar de tijddomeinvraag
(2, 6).

## Verhouding tot de TMS-omschakeling

Niet in `20261008-tms-omschakeling` en ook niet ervoor; fase 0 kan nu al.

1. **Die kaart verandert geen uitkomst.** Haar acceptatie (a) en (d) eisen dezelfde goldens met dezelfde
   verwachtingen, en punt 1 stuurt elk verschil terug naar een bouwkaart. Nieuwe semantiek in dezelfde kaart haalt het
   uitgangspunt weg waarmee die doorloop meet.
2. **Spec eerst** (`CLAUDE.md`, "Repo relationships"; `spec/PLAN.md` §9): spec, corpus, ref en clingo eerst, Drools
   daarna.
3. **Vóór de omschakeling** zou het Drools-deel drie keer gebouwd moeten worden: in de destructieve verlaging die
   verdwijnt, in de DRL-teksttak en in de tak met truth maintenance. Na `20261008-drl-teksttak-op-truth-maintenance`
   is het één ontwerp in twee weergaven (PLAN §9, 2026-10-08).
4. **De vorm van de tak met truth maintenance past.** Een tijdlijn is daar een onveranderlijke `List<Period>` in een
   standfeit, berekend door zuivere functies in de runtime (`Allocation.compute` is het precedent). Rekenen per periode
   is dan een zuivere functie over segmentlijsten, een voorwaarde per periode een knip op waar-perioden, en een
   schrijversconflict per dag een logisch foutfeit zoals `WritersConflict`. De weigering in de README (r. 500),
   "any value of a timeline target other than a pro-rata part or a day count", is precies de lijst die fase 2 inkort.
5. **Volgorde van de bestanden.** Fase 1 voegt goldens toe, en gate 12 noemt elke niet-gespeelde golden bij naam in
   `scripts/check-drools.sh`, een bestand dat beide TMS-kaarten wijzigen. Fase 1 loopt daarom serieel na die kaarten,
   of ertussen naar keuze van de eigenaar. Fase 0 wijzigt alleen `spec/proposals/` en mag naast
   `tms-omschakeling` lopen (WAY-OF-WORKING §1, punt 2).
6. **Eén punt voor de TMS-kaarten.** De destructieve exec-modeltak en de DRL-teksttak verlagen plus, min en de som per
   periode over tijdlijnen (`rsTijdReken`, `rsTijdSomAlle`); de tak met truth maintenance weigert ze, en gate 12 ziet
   dat niet, omdat geen golden de vorm draagt. Na beide TMS-kaarten schrijft dus geen uitvoerder ch. 3.4 of 3.5 via
   een regel; ref en clingo weigeren het nu ook. Dat is consistent, maar `20261008-drl-teksttak-op-truth-maintenance`
   hoort de twee runtimehulpen niet als dode code te verwijderen zonder deze kaart te noemen: fase 2 gebruikt ze, of
   hun opvolger.

## Fasering

- **Fase 0, deze kaart**: het voorstel en de meting.
- **Fase 1, spec eerst, in drie kaarten**: (1a) ch. 3.4 en 3.5 via een regel in ref en clingo, met een golden: het
  gat dat er nu al is; (1b) de besluitpunten 1, 2 en 11 met formulering B als anker; (1c) de besluitpunten 3–7 met
  formulering A als anker. Elk met spec-tekst, WF-regels, verifier, golden, negatief en rood, ref, clingo en de
  RegelSpraak-frontend. Besluitpunt 12 krijgt een eigen kaart als het voorstel bijstellen adviseert.
- **Fase 2, Drools**: na `20261008-drl-teksttak-op-truth-maintenance`, in de verlaging met truth maintenance van beide
  takken.
- De besluitpunten 8, 9, 10 en 13 krijgen in het voorstel een besluit (aannemen, weigeren of vastleggen), maar geen
  bouwkaart zonder vraag uit een brontaal; het voorstel kijkt of de WW-ronde (art. 15–21 WW) zo'n vraag stelt.

## Acceptatie

- (a) `spec/proposals/IMPLICIT-TIME.md` behandelt de besluitpunten 1–13, elk met de vraag, de lezingen (uchar nu,
  ALEF, en waar ze bestaat de RegelSpraak-specificatie bij paragraafnummer), een aanbeveling met reden, en ten minste
  één uitgewerkt voorbeeld met absolute waarden dat ref letterlijk als test kan overnemen.
- (b) Een meting: de ankercasus in beide formuleringen als RegelSpraak-bron, buiten de repo, door de frontend
  (`--ir`), de verifier, ref, clingo en beide Drools-takken zoals ze op dat moment op main staan; per vorm de eerste
  weigering met haar zin, als tabel in het voorstel; de commando's in het `Result:`. Desgewenst een derde kolom met de
  uitkomst van de ALEF-runtime (`alefRuntime` als gewone Java-bibliotheek, buiten de repo, met de aanroepen die de
  vertaling van `regelspraak.tijd` gebruikt); de kolom "B, ALEF" in de ankertabel hierboven is uit de code gelezen,
  niet gedraaid.
- (c) De ankerwaarden van deze kaart opnieuw en onafhankelijk nagerekend in exacte breuken: 141 2/3, 139 13/73,
  72 2/9, 73 71/73 en 73 47/61 €. Wijkt een waarde af, dan zegt het `Result:` welke en waarom.
- (d) `scripts/check-self-contained.sh` groen; niets buiten "May touch" veranderd; geen tekst uit de presentatie, uit
  ALEF of uit de RegelSpraak-specificatie, alleen dia-, bestands-, regel- en paragraafnummers.
- (e) Het `Result:` geeft de kaartteksten van fase 1 en 2 (Goal, Acceptance, May touch, Out of scope), hun volgorde en
  hun plaats ten opzichte van de twee TMS-kaarten, en voorgestelde registertekst voor rij 11 en 29 van
  `spec/CONSTRUCTS.md` en voor ch. 3.12.

## Raakvlakken

- `20261008-tms-omschakeling` en `20261008-drl-teksttak-op-truth-maintenance`: zie "Verhouding tot de
  TMS-omschakeling".
- `20261009-runtime-tijdconditie-leeg`: de vergelijkingsknip van ch. 3.6 leest ch. 1.2 per periode; de lezing per dag
  van besluitpunt 3 moet daarmee samenvallen.
- `20261009-emmerscalair-in-een-lezing`: de twee invoervormen van ch. 8.4; een tijdlijnexpressie leest volledige
  tijdlijnen, de emmerscalair blijft voor het eigen totaal.
- Een eventuele deontische laag (rij 32; het lokale TOEKOMST-document over Hohfeld, §6): de inertieregel vraagt
  hetzelfde antwoord op het tijddomein als eenmalig. Dia 13 noemt plicht, bevoegdheid en toestemming als soorten
  regels in ALEF; die blijven buiten deze kaart.

## Review

Eén lens op het voorstel (sol6) en een herkeuring na de fixronde, met twee vragen: heeft elk besluitpunt een
uitgewerkt voorbeeld met absolute waarden dat ref letterlijk kan overnemen, en noemt de meting elke weigering met haar
zin? Een lensvondst telt alleen met een concreet tegenvoorbeeld.

## Omvang

Klein tot middel: een voorstel en een meting, geen bouw. De bouwfasen samen zijn groot; 1a is de kleinste (de
functies staan in ref, het werk zit in de evaluator, clingo en de golden).

May touch: `spec/proposals/IMPLICIT-TIME.md` (nieuw; onder het CODEOWNERS-net van `spec/` een voorstel),
kaartverplaatsing. De meting draait in een kladmap buiten de repo.

Out of scope: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/`, `corpus/`, `ref/`,
`backends/`, `frontends/`, `scripts/`; `spec/PLAN.md` en `spec/CONSTRUCTS.md` (registertekst in het `Result:`); de rest
van de presentatie: regels over de informatietoestand ("het is niet bekend of", dia 16; rij 18 houdt
leeg-als-onbekend buiten v1), projectioneel bewerken (dia 6) en de regels over handelingen, plichten en bevoegdheden
(dia 13; rij 32).
