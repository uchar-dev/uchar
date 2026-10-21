# fixpoint-readings: de motor legt de lezing van elke voorwaarde vast

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U2 uit het Fable-advies over de waarden in de uitleg
(`research/uitleg-lezingen-2026-10-21/fable-advies-2026-10-21.md` in `uchar-dev/research`, §3–§7). Besluit van de
eigenaar 2026-10-21: weg C (de motor levert de waarden) met ref als getuige (kaart `ref-trace-and-readings-gate`).
Volgt op `undecided-stack-record`; de toolchain-kaart `explanation-from-readings` volgt deze kaart met een pin-bump.*

## Aanleiding

De Verbalisator van de toolchain rekent voorwaarden zelf na en wijkt daarbij op zes plekken van de Drools-backend af
(leeg in `+ − ×`, vergelijking met een lege operand, eenheden, tekst- en enumeratiegelijkheid, de notatie van een
getalliteral, datums; advies §1.1). Wat hij niet kent, heet "niet vast te stellen": 80 keer in de vijf
service-smoke-verzoeken van de regelset herleving-WW v2. De TMS-lowering heeft alles wat een lezing nodig heeft (per
regel een `ReadTable` en een `TestItem` met een `Term`; aan het fixpunt één standingsfeit per onderwerp en lezing), maar
geen lezer.

## Doel

In een build met uitleg (een optie op `DroolsBackend.build`; de productiebuild blijft zonder) krijgt elke `RulePlan`
met een `TestItem` een zusterregel met dezelfde linkerkant zonder de toets, die logisch een `Reading`-feit met de
waardenrij inzet. uchar evalueert de `Term` over die rij opnemend (een derde weergave naast `ClosureRendering`, met
dezelfde functies, dezelfde stopregel en dezelfde volgorde van ch. 5.10 en 5.11), en geeft
`DroolsBackend.readingsOf(Build, KieSession)` naast `fixpointOf` en `decisionsOf`: per vorm (de pointer van
`defaults-decider`) en onderwerp de status (geldt, faalt, fout, verdrongen, buiten geldigheid) en per knoop (pre-orde in
de draadvolgorde) bereikt of niet, oordeel of waarde met eenheid, fout, en de leden van een kwantorvouw. Een lezing
deponeert niets en schrijft niets. Elke vorm met een voorwaarde krijgt een lezing (besluit: ook berekeningen,
consistenties en creaties). De technische keuzes van advies §5 gelden, tenzij de bouwer meet dat het anders moet.

## Aanvullingen van de hoofdsessie (2026-10-21, na vragen van de eigenaar)

- **Naam.** De zusterregel heet in de code een *reading rule*: een `RulePlan` met dezelfde `readings` en `leftSide`
  zonder het `TestItem`, met `Consequence.Reading` als gevolg. Nodig omdat het `TestItem` in de linkerkant staat: een
  regel waarvan de voorwaarde faalt, matcht niet en legt dus niets vast, terwijl de uitleg ook zegt waarom iets niet
  gold. Het alternatief, de toets van elke regel naar haar gevolg verplaatsen, verandert het antwoordpad in elke regel
  en laat elke regel voor elk onderwerp vuren; niet doen.
- **Twee builds (besluit van de eigenaar, 2026-10-21).** De productiebuild blijft zonder reading rules; alleen de
  uitlegbuild (de optie op `DroolsBackend.build`) heeft ze. Getuige (a) "met en zonder gelijk" blijft daarom nodig.
  Reading rules tellen niet mee voor `Observer`, `coverage` en de trace; de fire-cap van de uitlegbuild houdt er
  rekening mee. Meet en noteer in de Result wat ze kosten (regels, vuringen, feiten, tijd van een vuur) over het corpus
  en op de regelset herleving-WW v2.
- **De replay geeft zijn sessie door, alleen als dat de code niet ingewikkelder maakt (eigenaar, 2026-10-21).** uchars
  replay (`CaseRunner`) kan de sessie of het fixpunt met de lezingen doorgeven aan een afnemer, zodat de scenario-runner
  van de toolchain een case niet een tweede keer speelt (`CaseSession.play`, nu bewaakt door een vergelijking van de
  vuringen). Vraagt dat een tweede pad of een uitzondering in `CaseRunner`, dan niet doen en de reden in de Result
  zetten.

## Acceptatie

- (a) Eerst gemeten: met de optie blijven de productiefeiten aan het fixpunt gelijk aan die zonder (dumpvergelijking
  over het corpus, beide takken), vóór iets anders gebouwd wordt.
- (b) Rood eerst met een klein eigen model (Jutterswet-namen) per vormfamilie, op beide takken: een samenstelling
  "alle" die bij het tweede lid stopt (het derde niet bereikt), een "ten minste één"-blok, een telling met filter, een
  kwantorvouw over een rol met twee leden, een vergelijking met een lege operand, een product met een lege operand (0),
  een herschaalde vergelijking, een tijdsduur, een kenmerkcheck over een rol, een default-keten met een beslisser in
  het midden (erboven faalt, eronder verdrongen), een attribuutdefault die geen waarde gaf, een vorm buiten haar
  geldigheid, een initialisatie.
- (c) Over het corpus, op beide takken: de lezing geldt ⇔ de vorm vuurde; elke operand die een lid leest heeft de waarde
  die `fixpointOf` dat lid geeft; een verdrongen default en een vorm buiten geldigheid hebben geen lezing; het stoppunt
  van een samenstelling volgt uit de ledenoordelen; met vloeren op (case, vorm, onderwerp) en op knopen. De
  `Reading`-feiten in `SessionDump`, zodat `BranchDumpsCorpusWitnessTest` de takken ook daarop gelijk houdt. Een vlag
  op `DroolsBackendMain` die per case de lezingen in canonieke tekst print (voor `ref-trace-and-readings-gate`).
- (d) Een zusterregel telt niet mee voor `Observer` en `coverage`. Gate 12 `EXPECTED_PLAYED` en lijsten ongewijzigd;
  gates 5, 6, 12, 13 dezelfde uitkomsten; alle zestien gates groen.
- (e) Result met de gemeten aantallen en de voorgestelde `CLAUDE.md`-tekst (gate 12: de dumpsectie).

May touch: `backends/drools/**` behalve de productieruntime; `scripts/check-drools.sh` alleen als voorstel; de kaart.

Out of scope: de uitleg zelf (toolchain `explanation-from-readings`); de ref-trace en de leesclausule in de spec
(`ref-trace-and-readings-gate`).
