# meldingscodes: een melding krijgt een code, los van haar tekst

*Op verzoek van de eigenaar (2026-10-17), bij de vraag naar meertalige foutmeldingen. Raakt `spec/`, het
case-schema en `scripts/`: de branch blijft een voorstel tot de eigenaar merget. Niet parallel met de stappen 3 en 5
van het programma "Engelse namen" (dezelfde bestanden); vóór de catalogi van stap 7.*

Goal: een melding is nu alleen tekst. De frontend heeft `Diagnostic(severity, message, line)`, de Drools-backend vrije
strings, en de gates pinnen fragmenten van die tekst: `expectedFault {ref, clingo, drools}` in 50 rode cases, een
armfragment in 354 `expect.txt`-bestanden, de redenen in de lijsten van gate 12 en de 36 bodemzinnen van gate 8.
Een tweede taal vraagt dan een tweede set pins, of gates die maar één taal toetsen. En omdat elke executor zijn eigen
fragment heeft, is "ref, clingo en Drools falen op dezelfde fout" handwerk. Deze kaart legt het ontwerp voor codes
vast en voert de normatieve plak uit: de foutsoorten bij evaluatie.

Modellen: de bouwer is Opus. Het ontwerp in `spec/proposals/MESSAGE-CODES.md` krijgt vóór de bouw van de plak één
review van Fable, op architectuur; de hoofdsessie vraagt die aan.

1. Ontwerp, in `spec/proposals/MESSAGE-CODES.md`, per soort melding de drager van de code en wie hem leest:
   - fouten bij evaluatie (het §9.5-kanaal en de andere foutkanalen van RUNTIME-SEMANTICS): normatief, een code per
     foutsoort met haar parameters;
   - afwijzingen door de verifier: WF-### blijft de code, en de arm binnen een regel krijgt een vaste sleutel van de
     verifier die `expect.txt` pint in plaats van een fragment;
   - weigeringen (een executor draagt een vorm niet: de redenen van gate 12, de bodemzinnen van ref en clingo):
     niet-normatief, een code per executor; de bodemzinlijst van gate 8 wordt een lijst van terugvalcodes die geen
     pin mag noemen;
   - diagnostiek van de frontend op een `.rgs`-bron: niet-normatief; `Diagnostic` krijgt `code` en `params`;
   - het rapportformaat: `CODE: tekst`, de code eerst, zodat een gate de code leest en een mens de tekst;
   - catalogi: Engels als referentie, Nederlands erbij voor de frontend en de Drools-backend, de onderdelen die de
     distributie levert; ref, verifier en clingo houden Engelse tekst bij hun codes; de distributie kiest de taal
     (een vlag in de CLI, `Accept-Language` in de dienst). Gedeclareerde namen gaan als parameter onvertaald in de
     tekst;
   - de volgorde van de vervolgkaarten.
2. De normatieve plak:
   - RUNTIME-SEMANTICS: een tabel van de foutsoorten met code, kanaal, parameters en per code een getuige in
     `corpus/rood`;
   - het case-schema: `expectedFault.code`, één code voor alle executors, additief;
   - ref, clingo en Drools zetten de code vooraan in hun rapport bij elke fout die een rode case pint;
   - gate 8 toetst de code in elk rapport. De fragmenten blijven staan tot elke rode case een code draagt; daarna
     verdwijnen ze, met de decodering door `ref --bodemzin`, in een vervolgkaart.

Acceptance:
- (a) `spec/proposals/MESSAGE-CODES.md` met de punten van 1, en een gemotiveerd voorstel voor de vorm van de codes
  (prefix, nummering, parameters) naast WF-###.
- (b) De tabel in RUNTIME-SEMANTICS; elke code heeft een getuige in `corpus/rood` (een zin zonder machinaal
  toetsbaar artefact is niet af).
- (c) `spec/schema/uchar-case-v0.schema.json` met `expectedFault.code`; gate 4 groen.
- (d) Alle 50 rode cases dragen `code`. Gate 8 faalt als een executor een andere code meldt dan de case noemt, eerst
  rood tegen een mutatie en dan groen; vloer `EXPECTED_MIN_RED_CODES`, gemeten.
- (e) Geen tekst van een melding verandert: elke bestaande fragmentpin blijft groen.
- (f) Alle gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `spec/proposals/MESSAGE-CODES.md` (nieuw), `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/uchar-case-v0.schema.json`, `corpus/rood/`, het foutrapport van `ref/`, `backends/clingo/` en
`backends/drools/`, `scripts/check-rood.sh`, `scripts/check-case-schema.sh` als het schema dat vraagt, deze kaart.

Out of scope:
- De catalogi en elke vertaling: vervolgkaart.
- De armsleutels van de verifier, de codes van gate 12 en die van de frontend: vervolgkaarten volgens het ontwerp.
- De distributie: haar taalkeuze, en haar eigen JSON-sleutels `fout`, `schendingen`, `veld` en `soort`.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.
