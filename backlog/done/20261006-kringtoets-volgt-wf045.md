# kringtoets-volgt-wf045: check weigert ook een kring op het eigen onderwerp (WF-045, WF-046); een kring door een niet-recursieve objectcreatie blijft doorgelaten

*Kleine fix zonder kaart, opdracht van de hoofdsessie van 2026-10-06; gebouwd
vanaf uchar `b3861896` op de tak `fix/kring-zelflezing`. Eerst gemeten, daarna
op besluit van de eigenaar doorgezet voor open punt 3(a) van
`20261002-ronde-klein-2.md`. Open punt 3(b) is meegenomen, gemeten en op
besluit van de eigenaar weer van de tak gehaald (zie Open punten). Commits:
`ee4d790c` (getuigen, rood), `a7210c19` (de fix), `64d3d18b` (negatief),
`ce23142b` (IR-SPEC), `da4979c1` (DIVERGENCES: de grens voor creatiekringen).
De meetbestanden staan onder `/tmp/kringmeting/` (`units.txt`,
`meting-voor.txt`, `meting-na.txt`, `meting-na3b.txt`, `meting-na2.txt`, het
meetprogramma onder `harness/` en `pariteit/`). Dit record legt de fix vast,
zodat de eigenaar er zijn Keuring bij kan zetten.*

## Aanleiding

Twee kenmerkregels die elkaars ontkenning lezen:

    Regel uitsluiting
        geldig altijd
            Een Jutter is uitgesloten indien hij niet penninggerechtigd is.

    Regel penningrecht
        geldig altijd
            Een Jutter is penninggerechtigd indien hij niet uitgesloten is.

Op `b3861896` gaf `check` hier alleen de WARNING `kringverwijzing tussen
berekeningen: Jutter.uitgesloten -> Jutter.penninggerechtigd ->
Jutter.uitgesloten — herberekening bereikt mogelijk geen fixpunt (de engine
begrenst het aantal activaties)` en exit 0. De verifier weigert het document:
`WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through no recursive
creation: uitsluiting -> penningrecht -> uitsluiting` en tweemaal WF-046
(`negative dependency edge on a cycle: …`). Met één Jutter zonder invoer weigert
ref onder WF-045; het ASP-programma heeft twee stabiele modellen (één met
`penninggerechtigd`, één met `uitgesloten`) en de replay neemt het eerste; beide
Drools-takken vuren de regel die eerst staat, en met de regels omgedraaid de
andere.

De oorzaak was de uitzondering `alGewaarschuwd` in
`RuleChecker.keurAfhankelijkheidskringen` (ingevoerd met `1dbeacc7`): een kring
die de eigen WARNING al noemde, kreeg geen ERROR.

## Meting vooraf

Het meetprogramma lost elke eenheid op met de frontend van `b3861896` en met de
fix, drukt de kring-diagnoses af en, bij een model zonder ERROR, de
WF-045-kringen van `Afhankelijkheden` met de randsoorten langs de kring; bij een
model met een ERROR is het document zonder die ERRORs geschreven om verborgen
kringen te zien. 462 eenheden: 121 goldens `bron.rgs`, 28 met `scenario.rgs`,
47 overige `.rgs` van de repo (`toka/casus` per bestand) en de casus als geheel;
van de distributie `examples/beslistabel` (8), `examples/toka` per bestand (47)
en als geheel (twee varianten) en de 32 regels van het toka-manifest; van de
werkbank v1 `basis`, `dagloon-ww`, `herleving-ww`, `verrekening-ww`, v2
`fundament` en `herleving-ww`, elk in de bronvolgorde van zijn Makefile, met de
140 scenario's van de v1-manifesten en de 8 van v2; de 19 eenheden van
`presentaties/demo-govtech4all`; en drie proefbronnen. De casus is alleen
gemeten met toka-23 en toka-45 erin; dat model draagt al ERRORs, dus de
kringtoets draaide er niet op (zie Open punten).

| Bron | Kring | WARNING vóór | ERROR na |
|---|---|---|---|
| goldens, `toka/`, voorbeelden van de distributie | geen | nee | nee |
| werkbank v1 en v2 behalve dagloon-ww, met scenario's | geen | nee | nee |
| werkbank v1 dagloon-ww en zijn 89 scenario's | twee positieve WF-045-kringen over rollen (de garantieketen, vier regels), niet op het eigen onderwerp, achter twee bestaande ERRORs (`1/dg`) | nee | nee (de kringtoets draait pas op een model zonder ERROR; gevolgtrekking: zonder die ERRORs weigerde `b3861896` ze al, want zij geven geen WARNING en `alGewaarschuwd` sloeg ze dus niet over) |
| demo-govtech4all | geen | nee | nee |
| proefbronnen met de twee kenmerkregels, beide volgordes | kring met ontkenning | ja | ja |

Tests die met de fix faalden:

- frontend: 16 rijen `ZelflezingKringTest`, 2 `RecursieTest` en 1
  `AfhankelijkheidskringTest` (eigen onderwerp) legden precies dit gedrag vast;
  de rij door een afwijking van `AfhankelijkheidskringTest` noemt nu ook WF-046;
  twee rijen `IrTest` lazen hun eigen doel in de voorwaarde terwijl ze de vorm
  van die voorwaarde toetsen.
- distributie: `RegelSpraakEngineTest.kringverwijzing_wordt_begrensd_niet_gehangen`
  legde de WARNING en de begrensde run vast; `ScenarioOrakelTest` met de regels
  `verhoog` (totaalprijs := totaalprijs + grens indien totaalprijs < grens) en
  `tel` (duur := duur + looptijd) las het eigen doel. De rest van de distributie
  bleef gelijk: service- en Quarkus-tests, `check-tweelingen`,
  `check-dienst-replay` (212 goldens), het toka-manifest (29 ok, 3 uitgesloten,
  0 mismatch).
- backend, graaf en `toka/`: geen.

De meting stopte eerst vóór het pushen, omdat de twee orakeltests van de
distributie een uitkomst van een positieve kring vastlegden; de eigenaar besliste
door te zetten.

## Wat landde

- `RuleChecker.keurAfhankelijkheidskringen`: de uitzondering voor een kring die
  de WARNING noemt is weg, en ook de functie `alGewaarschuwd` en het record
  `Afhankelijkheden.Doel`, dat alleen haar voedde. Een WF-045-kring met een
  negatieve rand tussen twee van haar vormen noemt WF-046 erbij
  (`Afhankelijkheden.draagtNegatieveRand`). De WARNING van
  `keurKringverwijzingen` blijft naast de ERROR staan: zij noemt de kring in de
  leden die de auteur schreef en is de enige melding van de kring in een model
  dat een andere ERROR draagt. De commentaren van beide passen zeggen dat. De
  uitzondering voor een kring door een objectcreatie buiten een recursieve groep
  blijft.
- De melding, letterlijk:

      regel 'uitsluiting' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een recursieve creatie (WF-045), en een kring draagt geen negatieve rand, zoals een ontkenning (WF-046): dependency cycle through no recursive creation: uitsluiting -> penningrecht -> uitsluiting

  Zonder negatieve rand blijft de bestaande zin, zonder het deel over WF-046.
- Tests: de genoemde rijen leggen de weigering vast naast de WARNING; `IrTest`
  schrijft een ander attribuut.
- `corpus/negatief/wf046-kenmerken-lezen-elkaars-ontkenning`: de twee
  kenmerkregels op Jutterswet-materiaal, het document zoals de frontend het vóór
  de fix schreef; `expect.txt` pint de arm van de eerste WF-046-regel.
- `grammar/DIVERGENCES.md` (§7 en "Kringen: WF-045 en WF-051 in check", met de
  reden waarom de grens voor creatiekringen blijft) en IR-SPEC ch. 5.5 (de
  Recorded-alinea van ronde klein 2).
- `20261002-ronde-klein-2.md`: open punten 3(a) en 6 opgelost, 3(b) open met de
  vondst.
- In de distributie volgen de drie herschreven tests en de pin, in de werkbank
  de pin.

## Metingen na

- De 462 eenheden op `a7210c19` (`meting-na.txt`): alleen de twee proefbronnen
  met de kenmerkregels veranderen, van exit 0 met de WARNING naar de WARNING en
  de ERROR hierboven.
- WF-046 in de melding tegen de verifier: op de 868 documenten van de
  corpuszones en de parity-zone (met het nieuwe negatief) liggen elf
  WF-045-kringen, vijf met een negatieve rand; bij alle elf noemt de frontend
  WF-046 precies waar de verifier een WF-046-regel geeft tussen twee vormen van
  de kring.
- Rood vóór: de getuigen 20 tests rood op `b3861896`.
- Suites: frontend 1158 tests, 0 failures; backend 1094, 0 failures.
- Poorten: de zestien in de volgorde van `gates.yml`, `dune build` vooraf, op
  `da4979c1` (de laatste commit buiten `backlog/`; de commit met dit record
  raakt alleen `backlog/`), zeventien keer `exit 0` en `DONE 13:47:47`:

       1. check-self-contained: PASS
       2. check-ir-schema: PASS (… 334 file(s) validate and round-trip, floor 192)
       3. check-verifier: PASS (334 golden clean; 504 negatief rejected …; 347 on their declared arm …)
       4. check-case-schema: PASS (… 842 case file(s) validate)
       5. check-oracle: PASS (ref suite 1152 worked examples, floor 489; 800 cases replayed exactly, floor 427 …)
       6. check-clingo: PASS (backend suite 1988 checks, floor 83; 800 cases replay exactly through clingo …)
       7. check-noop: PASS (756 cases refute a do-nothing executor; 44 pinned survivors …)
       8. check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26 …)
       9. check-catala: PASS (241 checks, floor 201; 12 catala golden(s) …)
      10. check-regelspraak: PASS (JUnit 1158 tests, floor 173; 121 rgs golden(s) re-emit byte-identically, floor 80)
      11. check-toka-parity: PASS (1 casus dump, 45 .rgs re-emitted byte-identical, floor 45; ref en clingo 29 PASS)
      12. check-drools: PASS (played 239, pin 239; DRL-text branch played 193, pin 193; JUnit 1094 tests, floor 239)
      13. check-reftype-derivation: PASS (43 unit annotations twisted over 24 goldens, floor 38; 122 executor replays …)
      14. check-flogic: PASS (150 checks, floor 81 …)
      15. check-graaf: PASS
      16. check-scenario: PASS (28 scenario golden(s) regenerate 99 case file(s) …)

  Gate 3 telt één negatief meer dan op `b3861896` (504, waarvan 347 op hun arm),
  het nieuwe document.

## Voorgestelde registertekst (CONSTRUCTS rij 6)

In de Landed-alinea van 2026-10-02 (ronde klein 2) de bijzin "except a cycle its
own warning names on a rule's own subject and a cycle through an object creation
outside a recursive group (both pinned by existing frontend tests)" vervangen
door: "except a cycle through an object creation outside a recursive group
(pinned by existing frontend tests; the casus of the parity zone carries one);
since 2026-10-06 a cycle on a rule's own subject is refused as well, naming
WF-046 too where a negative edge lies between two forms of the cycle (negatief
`wf046-kenmerken-lezen-elkaars-ontkenning`)". Vervangen door het voorstel in
`20261006-feitcreatie-zonder-populatie.md`; besluit gevraagd in kaart
`20261006-besluit-registertekst-constructs-rij-6.md`.

## Open punten

1. **3(b), de kring door een niet-recursieve objectcreatie.** Gemeten op de tak
   met de uitzondering weg (de commits zijn weer van de tak gehaald): de 462
   eenheden veranderen niet, drie frontend-tests (twee in `CreatieRolTest`, één
   in `SetlezingZonderVouwTest`) moeten de weigering vastleggen, en de
   verifier geeft op die drie vormen dezelfde regels als de melding. Maar de
   TOKA-casus (zonder toka-23 en toka-45) bevat een kring door een
   niet-recursieve objectcreatie. Weigeren breekt poort 11 (de casus is dan niet
   opnieuw te schrijven) en de dienst van de distributie, die die casus
   serveert (17 servicetests). Daarom is het een vraag aan de spec: is WF-045 op
   dit punt strenger dan RegelSpraak bedoelt?
2. Open punt 4 van ronde klein 2 blijft: een negatieve rand op een kring door
   een recursieve creatie (WF-045 laat die kring toe, WF-046 niet) gaat door
   `check`. Kaart: `20261006-check-wf046-door-recursieve-creatie.md`.
3. De distributie noemt in de begrenzing van `RegelSpraakEngine.Session.fire`
   en in `Uitvoerder` nog "de resolver-WARNING" als bron van een kring zonder
   fixpunt. Een bron met een kring op het eigen onderwerp bereikt de engine
   niet meer; de tekst is niet aangepast. Kaart:
   `20261006-bronvolgorde-restpunten.md`, punt (e).
4. v1 `dagloon-ww` draagt twee positieve WF-045-kringen achter de twee ERRORs
   `1/dg`; wie die ERRORs oplost, krijgt de kringen als ERROR.
