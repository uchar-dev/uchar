# check-wf046-door-recursieve-creatie: een negatieve rand op een kring door een recursieve creatie gaat door `check`

*Uitgesteld punt, vastgelegd door de hoofdsessie op 2026-10-06 op verzoek van
de eigenaar, zodat het vindbaar blijft; geen bouwdatum. Bron: open punt 4 van
`backlog/done/20261002-ronde-klein-2.md` en open punt 1 van
`backlog/done/20261006-feitcreatie-zonder-populatie.md`. Gemeten op uchar
`0a378588`: de frontend opnieuw gebouwd (`make -C frontends/regelspraak
compile`), `dune build`, de shaded jar van `backends/drools` van na de laatste
backendcommit `fdae9fc1`. De proefbestanden stonden onder `/tmp/kaart1-probe/`
(`bron.rgs`, `bron-obs.rgs`, `case-*.json`, `meting.txt`,
`meting-leegtest.txt`); wat nodig is om de meting te herhalen, staat hieronder.*

## Aanleiding

WF-045 laat een kring toe die door een creatie in een recursieve regelgroep
loopt. WF-046 verbiedt op elke kring een negatieve rand, ook op zo'n kring
(`spec/IR-SPEC.md` regel 8054–8062; ch. 5.2 en 5.3). De verifier toetst WF-046
los van WF-045, over de volledige graaf: per negatieve rand p → c of p vanuit c
bereikbaar is (`spec/verifier/lib/checks.ml` regel 7888–7914, in `wf045_046`,
regel 7854).

`check` herhaalt WF-046 alleen binnen een kring die WF-045 weigert.
`RuleChecker.keurAfhankelijkheidskringen`
(`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/RuleChecker.java`
regel 155–180) meldt de kringen van `Afhankelijkheden.kringen` (regel 43), en
dat zijn alleen WF-045- en WF-051-schendingen. De WF-045-wandeling slaat een
vorm van een recursieve creatie over (`Afhankelijkheden.java` regel 91), en
`draagtNegatieveRand` (regel 67) kijkt alleen tussen de vormen van een
WF-045-kring. `grammar/DIVERGENCES.md` regel 3005–3006 legt de grens vast: "A
negative edge on a cycle WF-045 admits, through a recursive creation, still
passes `check`."

Sinds 2026-10-06 leest een rolcheck de relatie die hij noemt, en ontkend is dat
een negatieve rand (record feitcreatie-zonder-populatie, "Stap 1"). Daardoor
valt ook een ontkende rolcheck op de relatie die een recursieve creatie zelf
legt onder deze grens. Het negatief
`corpus/negatief/wf046-ontkende-rolcheck-recursieve-creatie` is zo'n document;
zijn `NOTE.md` geeft de regel:

    Regelgroep estafette is recursief
    Regel werf de opvolger
        geldig altijd
            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
            indien hij aan alle volgende voorwaarden voldoet:
            - hij is geen voorganger
            - zijn rang is kleiner dan 3.

## Gemeten (2026-10-06)

`bron.rgs` is deze regel met de declaraties van
`corpus/golden/jutterswet-wachtersketen-rgs/bron.rgs` (de Wachter met zijn
rang, het feittype opvolging met voorganger en opvolger). De case: één Wachter
W1 met rang 1.

| Uitvoerder | Uitkomst |
|---|---|
| frontend (`Resolver --ir`) | exit 0, geen diagnose; het document is, zonder regelnummers, gelijk aan het `ir.json` van het negatief |
| verifier | `WF-046 FAIL /rules/objectCreations/0/versions/0: negative dependency edge on a cycle: werf de opvolger reads what werf de opvolger produces while werf de opvolger is reachable from it (no strata exist)`, exit 1 |
| ref | `EVALUATION FAULT: WF-046 at /rules/objectCreations/0/versions/0: … — the dependency graph is ill-formed; ref/ refuses to evaluate` |
| clingo | `UNSATISFIABLE — a fail-fast guard fired: no viol/2 witness (the input is not stratified?)` |
| Drools, exec-modeltak | `PLAYED 1` |
| Drools, DRL-teksttak (`--drl`) | `PLAYED 1` |

**Wat de Drools-takken uitrekenen.** Het negatief heeft geen lid waaraan de
uitkomst te zien is. Daarom is ook gemeten op `bron-obs.rgs`: dezelfde bron
met twee lezende regels uit `jutterswet-wachtersketen-rgs` erbij:

    Regel opvolgerrang van een wachter
        geldig altijd
            De opvolgerrang van een Wachter moet berekend worden als de som van de rang van alle opvolgers van de Wachter of 0 als die er niet zijn.

    Regel rangdetector
        geldig altijd
            De rang van een Wachter moet kleiner zijn dan 3.

De verifier geeft op dat document dezelfde ene WF-046-regel; ref en clingo
antwoorden als in de tabel. Beide Drools-takken spelen de case met de
verwachtingen "W1 heeft opvolgerrang 2" en "`rangdetector` geschonden"
(`PLAYED 1`). Een mutant die opvolgerrang 99 en `consistent` verwacht, geeft in
beide takken dezelfde regel:

    FAIL case-obs-mutant.json: attribuut opvolgerrang: verwacht 99, werkelijk 2; verwacht consistent, werkelijk inconsistent (rangdetector @ null)

Gevolgtrekking uit die uitkomst: beide takken maken de keten W1 (rang 1) → W2
(rang 2) → W3 (rang 3). W1 heeft precies één opvolger, met rang 2 (de som is
2), en één gemaakte Wachter heeft rang 3 of meer; met de voorwaarde "zijn rang
is kleiner dan 3" kan dat alleen W3 zijn. In die eindtoestand zijn W1 en W2
voorganger, dus de voorwaarde "hij is geen voorganger" waaronder hun opvolger
gemaakt werd, is voor beide onwaar. Drools toetst de voorwaarde van een creatie
op het moment dat zij vuurt, en neemt een gemaakt object niet terug als een
latere koppeling die voorwaarde onwaar maakt. Het antwoord is dus het resultaat
van één keer voorwaarts vuren, waar de verifier en ref zeggen dat er geen strata
bestaan en clingo geen stabiel model vindt. Geen van beide takken meldt iets.

**Ter vergelijking: de vier andere WF-046-negatieven door een recursieve
creatie**, met dezelfde case (W1, rang 1; de case verwacht alleen rang 1):

| Negatief | exec-modeltak | DRL-teksttak |
|---|---|---|
| `wf046-leegtest-recursieve-creatie` | `EXCLUDE transpiler SKIP: regel: merk van een wachter — regel (een leeg- of gevuld-toets op rang van Wachter, dat de objectcreatie werf de opvolger, die op deze regel wacht, schrijft: een kring zonder volgorde (WF-046)): merk van een wachter` | `PLAYED 1` |
| `wf046-leegtest-negatie-recursieve-creatie` | dezelfde `EXCLUDE` | `PLAYED 1` |
| `wf046-leegtest-aanwezigheid-recursieve-creatie` | `PLAYED 1` | `PLAYED 1` |
| `wf046-leegtest-kwantorvouw-recursieve-creatie` | `PLAYED 1` | `PLAYED 1` |

De exec-modeltak weigert dus alleen een leeg- of gevuld-toets in een gewone
regel op zo'n kring met naam en reden (`kringWeigering`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
regel 136–139, afgeleid in `groepVolgorde`, regel 9040; de javadoc op regel
9028–9029 zegt dat een stratum-strikte lezing op een kring door een recursieve
creatie haar plaats houdt). Een creatie die negatief leest wat op haar eigen
kring geschreven wordt, speelt in beide takken; de DRL-teksttak speelt alle
vijf documenten. Of de frontend de vier leeg-toetsnegatieven ook zonder melding
schrijft, is niet gemeten: naast die documenten staat geen bron.

## Acceptatie (bij oppakken aanscherpen)

- (a) `check` weigert elke negatieve rand op een kring, ook op een kring die
  WF-045 toelaat, met een ERROR in de zin van de verifier (WF-046) op de kop
  van de vorm die de verifier noemt. `Afhankelijkheden` krijgt de
  bereikbaarheidstoets van `wf045_046` over de volledige graaf, zonder vormen
  over te slaan; `keurAfhankelijkheidskringen` krijgt een arm per
  WF-046-regel. Een WF-045-kring houdt zijn melding met de bijzin over WF-046;
  een WF-046-regel die daarin al genoemd is, staat niet nog eens apart.
- (b) Rood-eerst getuigen in de frontend: de bron van het negatief, en een
  leeg-toets in een gewone regel op een kring door een recursieve creatie.
  Flank: de bron van `jutterswet-wachtersketen-rgs` (dezelfde kring, zonder
  ontkenning) blijft zonder melding.
- (c) Pariteit met de verifier: de WF-046-regels van `check` zijn gelijk aan
  die van de verifier op de documenten van de corpuszones en de parity-zone,
  zoals de meting van 2026-10-06 voor WF-045 (868 documenten). Inertie: een
  eenhedenlijst als die van de vorige rondes (goldens, `toka/`, de voorbeelden
  van de distributie, de werkbank) geeft vóór en na dezelfde uitvoer, behalve
  nieuwe weigeringen, die elk benoemd worden.
- (d) Drools: een besluit van de eigenaar of de exec-modeltak een creatie met
  een negatieve lezing op zo'n kring met naam en reden weigert, zoals
  `kringWeigering` dat voor een leeg-toets in een gewone regel doet. Zonder
  besluit blijft het zoals hierboven gemeten. Na (a) bereikt een bron via de
  CLI of de dienst de takken niet meer met zo'n kring; een handgeschreven
  document wel.
- (e) `grammar/DIVERGENCES.md` (de zin op regel 3005–3006) bijgewerkt;
  registertekst voor CONSTRUCTS rij 6 als voorstel in het `Result:`; alle
  zestien gates groen.

## Omvang

Klein tot middel. De wijziging in de frontend is één functie in
`Afhankelijkheden` (de toets in de verifier is ongeveer 25 regels OCaml), één arm in
`keurAfhankelijkheidskringen`, getuigen en een zin in DIVERGENCES. De meting
kost meer dan de wijziging; de meetopzet van
`backlog/done/20261006-kringtoets-volgt-wf045.md` is te hergebruiken. Gevolgen
voor bestaande bronnen: in de 1171 documenten van de meting van 2026-10-06
droeg geen enkel document een rolcheck op een relatie die een creatie schrijft
(record feitcreatie-zonder-populatie, "Stap 1"); voor leeg-toetsen en
kwantorvouwen op een kring door een recursieve creatie is dat niet gemeten.
Punt (d) is apart werk in `backends/drools`, alleen na een besluit. De
distributie en de werkbank volgen met de pin.

May touch: `frontends/regelspraak/**` (resolver, tests,
`grammar/DIVERGENCES.md`), kaartverplaatsing; bij (d) ook
`backends/drools/**`.
Niet in deze kaart: de spec, de verifier en ref (die weigeren al); het plaatsen
van de registertekst (hoofdsessie, serieel).

## Na `20261008-tms-omschakeling` (2026-10-11)

Na `20261008-tms-omschakeling` (2026-10-10) bestaan `ExecModelTranspiler`, `kringWeigering` en `groepVolgorde` niet
meer; de exec-model-tak weigert bij de bouw wat WF-045/046 weigeren (`Kringen`, 'exec-model: kring in de
afhankelijkheden …'). De kolom 'exec-modeltak' van de tabel beschrijft de verwijderde verlaging; wat de huidige
exec-model-tak op de vijf negatieven doet, is niet nagemeten.
