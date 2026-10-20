# bronvolgorde-restpunten: wat na de bronvolgorde-ronde nog van de volgorde afhangt, en de tekst "de resolver-WARNING"

*Uitgestelde punten, vastgelegd door de hoofdsessie op 2026-10-06 op verzoek
van de eigenaar, zodat ze vindbaar blijven; geen bouwdatum. Bron: open punten 2
en 3 van `backlog/done/20261006-feitcreatie-zonder-populatie.md`, open punt 3
van `backlog/done/20261006-kringtoets-volgt-wf045.md`, en de sectie
"Bronvolgorde" van `frontends/regelspraak/grammar/DIVERGENCES.md` (regel
3010–3043). Regelnummers op uchar `0a378588` en regelspraak-toolchain
`f9fc44c`.*

## Aanleiding

De ronde feitcreatie-zonder-populatie maakte het document dat de frontend
schrijft onafhankelijk van de volgorde van regels en bestanden, op de volgorde
binnen een lijst na (gemeten op 368 bronnen en bronketens). Vier dingen hangen
nog aan de volgorde: twee zijn alleen tekst, één is bewust, één is niet
gemeten. Daarnaast verwijzen twee meldingen in de toolchain nog naar een
WARNING als bron van een kring, terwijl `check` zo'n kring sinds 2026-10-06 met
een ERROR weigert. De punten zijn klein; het onderscheid dat telt, is dat (a),
(b) en (e) tekst zijn en (d) eerst gemeten moet worden.

## Per punt

**(a) Welke vorm of declaratie een weigering als eerste noemt. Tekst.**
De WF-045-wandeling noemt de eerste kring die zij tegenkomt, in de volgorde van
de vormen (`Dependencies.deep`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Dependencies.java`
regel 89; de verifier doet hetzelfde in `wf045_046`,
`spec/verifier/lib/checks.ml` regel 7866–7887). Gemeten bij de bron
`jutterkring`: de melding begint met "Jutter.uitgesloten -> …" of met
"Jutter.penninggerechtigd -> …", naar de volgorde. Zo ook bij een botsing van
twee dagsoortdefinities of twee regelnamen (gemeld bij de tweede) en bij de
keuze van de eigen basis in de hersteladviezen van de opgavelowering. De
weigering zelf staat er in elke volgorde; exitcode en document zijn gelijk.
Mogelijk werk: de wandeling laten beginnen bij een vaste vorm (bijvoorbeeld de
kleinste naam), in de frontend en in de verifier, zodat de zin niet van de
volgorde afhangt. Dat raakt de zin van de verifier (beschermd pad) en de
`expect.txt` van de negatieven met een kring. Anders: vastleggen als aanvaard.

**(b) De `source` van de regel van een gegenereerd lid. Tekst.**
Een vouw over een verzameling met to-one stappen erna genereert een lid "<lid>
via <rol 1>, … en <rol n>" en een regel die het zet (DIVERGENCES, "A fold over
a set and to-one steps after it"). `ExpressionBuilder.helperMember`
(`resolver/ExpressionBuilder.java` regel 888–922) geeft die regel de brontekst
van de vouw die het lid als eerste genereert (regel 918–920); een tweede vouw
over dezelfde stappen vindt het bestaande lid (regel 894) en verandert niets.
Bij twee vouwen met een anders geschreven lezing verschilt tussen twee
volgordes alleen de `source` van die regel; waarden en afhankelijkheden zijn
gelijk.
Mogelijk werk: een keuze die niet van de volgorde afhangt (bijvoorbeeld de
kleinste brontekst), of vastleggen als aanvaard.

**(c) De rangorde van `Uitzondering op` volgt de bron. Bewust; geen werk.**
Van twee `Uitzondering op`-blokken staat het later geschreven blok hoger
(DIVERGENCES, sectie "Bronvolgorde", met verwijzing naar §4). De volgorde is
hier de inhoud. Genoteerd zodat een volgende meting over permutaties dit niet
als afwijking telt.

**(d) `ruleSubjectOf` vóór `setVariables`. Niet gemeten.**
Bij elke regelversie zet `RuleChecker` eerst het onderwerp en daarna de
variabelen (`resolver/RuleChecker.java` regel 3290–3291):

    bouwer.setSubject(subjectVan(rsr.resultaatDeel()));
    bouwer.zetVariabelen(rsr.variabeleDeel());

Voor een enkelvoudige consistentieregel bepaalt `ruleSubjectOf` (regel 4643) het
onderwerp door de uitdrukking te bouwen (`bouwer.bouw(e.expressie())`, regel
4658–4660). Dat gebeurt voordat `setVariables` de variabelen van de vorige
regel wist en voordat het nieuwe onderwerp gezet is, dus met het onderwerp en
de variabelen van de regel die in de bron ervoor staat. Het record vond geen
geaccepteerde zin die daardoor een ander document krijgt, maar heeft dat niet
gemeten.
Te meten: (1) of `bouw` op die plek iets aan het model toevoegt (een diagnose,
een gegenereerd lid, een binding) of alleen een type teruggeeft; (2) een
enkelvoudige consistentieregel die een naam leest die in de regel ervoor een
variabele is ("Daarbij geldt"), en een die "zijn …" leest, telkens na een regel
met een ander onderwerp, in beide volgordes; (3) of het teruggegeven objecttype
kan verschillen. Als het iets uitmaakt: de variabelen wissen en het onderwerp
leeg zetten vóór `ruleSubjectOf`, met een rood-eerst getuige in
`SourceOrderTest`.

**(e) "de resolver-WARNING". Tekst.**
De begrenzing op 100000 activaties noemt de WARNING van de frontend als bron
van een kring zonder fixpunt:

- regelspraak-toolchain
  `toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/RegelSpraakEngine.java`
  regel 181–182, in `Session.fire`: "… — vermoedelijk een kringverwijzing
  tussen berekeningen (zie de resolver-WARNING)", en de javadoc op regel
  168–170 ("resolver-diagnosed as a WARNING");
- regelspraak-toolchain
  `service/src/main/java/dev/uchar/regelspraak/toolchain/service/Executor.java`
  regel 71–72, dezelfde zin;
- in uchar het commentaar in
  `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
  regel 548: "a read-write CYCLE (resolver-WARNed) hits the engine's fire cap".

Sinds 2026-10-06 weigert `check` elke kring die WF-045 weigert met een ERROR, en
de WARNING staat ernaast (DIVERGENCES, "Kringen: WF-045 en WF-051 in check").
Een bron met zo'n kring bereikt de engine via `check`, de CLI en de dienst niet
meer. Wat de grens nog kan bereiken, is een handgeschreven document of een kring
die WF-045 toelaat, door een recursieve creatie (zie kaart
`20261006-check-wf046-door-recursieve-creatie.md`); dat is een gevolgtrekking,
niet gemeten. De tekst moet zeggen wat de grens nu nog bereikt, in plaats van
naar de WARNING te verwijzen. Geen test in de toolchain pint de zin (gezocht op
"resolver-WARNING" en "geen fixpunt binnen").

## Acceptatie (bij oppakken aanscherpen)

- (d) gemeten, met de uitkomst in het `Result:`; bij een verschil een rood-eerst
  getuige en de fix.
- (e) de meldingen en commentaren in de toolchain en in uchar zeggen wat de
  grens nu nog bereikt; de toolchain-tests groen.
- (a) en (b): per punt de keuze (wijzigen of aanvaard) in het `Result:`, en de
  sectie "Bronvolgorde" van DIVERGENCES daarop bijgewerkt.
- De permutatiemeting van de ronde opnieuw: geen verschil buiten de volgorde
  binnen een lijst en de punten die aanvaard zijn.
- Alle zestien gates groen.

## Omvang

Klein. (e) is twee meldingen, één javadoc en één commentaar. (a) en (b) zijn
elk een keuze tussen een kleine wijziging en vastleggen als aanvaard; bij (a)
raakt een wijziging ook de verifier. (c) is geen werk. (d) is eerst een meting
van enkele uren; als een fix nodig is, is dat een paar regels in `RuleChecker`
met een getuige.

May touch: `frontends/regelspraak/**`, het commentaar op regel 548 van
`ExecModelTranspiler.java`, in regelspraak-toolchain de twee meldingen en de
javadoc; bij (a) met wijziging ook `spec/verifier/` (**voorstel**, beschermd
pad) en de betrokken `expect.txt`; kaartverplaatsing.
Niet in deze kaart: de volgorde binnen een lijst; (c).

## Na `20261008-tms-omschakeling` (2026-10-11)

Bij (e): het commentaar op r. 548 van `ExecModelTranspiler.java` is met dat bestand weg (`20261008-tms-omschakeling`,
2026-10-10); de exec-model-tak weigert een kring die WF-045/046 verbieden bij de bouw, en de twee meldingen in de
toolchain verdwijnen met haar kaart voor de omschakeling.
