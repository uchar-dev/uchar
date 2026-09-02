# Niveau 0: de meldplicht als gevouwen positiewaarde (nul IR-wijziging)

Goal: Eén nieuwe golden, `corpus/golden/jutterswet-meldplichtspoor/`, die
met UITSLUITEND gelande constructen een normatieve positie draagt en over
een gebeurtenisreeks bijwerkt: een keuzetype `meldstatus` (`Openstaand`
met de resterende termijn als getallading, `Vervuld`, `Geschonden`) als
accumulator van een `Fold` over een lijstinvoer van gebeurteniswaarden, met
`Match` op de accumulator en `IfThenElse`/`Let` in het lichaam, geschreven
naar een somtype-getypeerd attribuut op de Jutter. Het doel is BEWIJS, niet
vorm: `TOEKOMST_hohfeld_deontiek_20260822.md` §10 stelt niveau 0 voor als
stresstest die met bewijsmateriaal — niet met architectuurintuïtie —
bepaalt welke knopen van §5 hun plaats verdienen, en deze kaart maakt dat
uitvoerbaar zonder één IR-wijziging. Twee dingen komen er hard uit. (a) Of
de gelande som-, record- en foldkern een positie werkelijk draagt door BEIDE
executors: de samengestelde vorm — een `Fold` met SOMTYPE-accumulator over
een LIJSTINVOER — draait vandaag NERGENS in het corpus, terwijl WF-065 een
gedeclareerd `SumType` als lijstelement toelaat en WF-070 een
`SumType`-accumulator uitdrukkelijk toelaatbaar verklaart; de twee helften
draaien alleen los (somtype-accumulator over een objectattribuut in
`jutterswet-penningstaffel-catala` en `jutterswet-kamerhuishouden-catala`;
numerieke accumulator over een lijstparameter in `jutterswet-kinderdeler`
en `anchor/jutterswet-staffelvergoeding`). Die lacune sluiten heeft
corpuswaarde los van de deontische vraag. (b) WAAR de vorm breekt: de
verwachting is dat hij breekt op IDENTITEIT, want WF-060 laat als
somtype-lading alleen een getal, een dagpreciezige datum of een
gedeclareerd somtype toe (tekst is al negatief-gepind door
`corpus/negatief/wf060-text-payload`) en WF-071 kent voor structvelden
dezelfde beperking plus structs — houder en wederpartij kunnen dus niet ín
de waarde zitten. De houder is daarom het regelsubject en de wederpartij
per regel vast. Die grens wordt in de NOTE vastgelegd als het scherpste
argument dat een latere kernuitbreiding moet beantwoorden. De kaart
levert géén IR-uitbreiding, géén WF-nummer en géén spec-tekst; zij levert
een golden, hand-gerekende absolute verwachtingen, en een eerlijk
opgetekende grens.

Acceptance:
- De golden bestaat als `corpus/golden/jutterswet-meldplichtspoor/` met
  `ir.json`, ten minste drie `case*.json` (op tijd gemeld → `Vervuld`;
  te laat of niet gemeld → `Geschonden`; lege gebeurtenisreeks →
  `Openstaand` met de volle termijn) en een `NOTE.md` in de vorm van
  `corpus/golden/jutterswet-kinderdeler/NOTE.md`: hand-gerekende absolute
  waarden per casus, de gate-verwijzingen, en de opgetekende grens uit (b).
  De NOTE draagt GEEN herkomst — geen externe taal- of projectnamen, geen
  paden buiten de repo (gate 1; corpusregel "code en data dragen geen
  herkomst, git-historie en het plan doen dat").
- Elke casus pint de einduitkomst ABSOLUUT als
  `attributeValue` met een `constructor`/`payload`-waarde — de vorm die
  `anchor/jutterswet-beschikkingsom` al door beide executors speelt — plus
  `consistent`.
- ROOD-EERST waar van toepassing: als de samengestelde vorm in `ref` of
  clingo op een bestaande weigering stuit (de S19-inventaris, bijvoorbeeld
  de gesloten-lichaam-grammatica of een lijstbron), toon die weigering
  eerst, en laat haar daarna groen worden of — als dragen buiten deze kaart
  valt — laat de golden NIET landen en lever in plaats daarvan de
  opgetekende weigering plus een vervolgkaart. Een golden die maar door één
  executor speelt, landt niet: dat is de gate-regel, geen onderhandeling.
- `scripts/check-oracle.sh` (gate 5) en `scripts/check-clingo.sh` (gate 6)
  spelen alle nieuwe casussen met exact deze verwachtingen — ref ≡ clingo.
- `scripts/check-noop.sh` (gate 7) faalt op elke nieuwe casus (de
  verwachtingen gelden niet op de rauwe invoer). Als een casus de no-op
  overleeft, is de casus verkeerd gesneden — herzien, NIET aan de
  survivorlijst toevoegen (`scripts/` is protected).
- `scripts/check-drools.sh` (gate 12) blijft groen: een lijstinvoer valt
  onder de bestaande per-vorm-autoweigering, die haar reden gegroepeerd in
  het gate-rapport afdrukt. Toon dat rapport; voeg NIETS toe aan de
  waarde-divergentielijst in het script.
- Alle twaalf gates groen, met getoonde uitvoer:
  `scripts/check-self-contained.sh`, `check-ir-schema.sh`,
  `check-verifier.sh`, `check-case-schema.sh`, `check-oracle.sh`,
  `check-clingo.sh`, `check-noop.sh`, `check-rood.sh`, `check-catala.sh`,
  `check-regelspraak.sh`, `check-toka-parity.sh`, `check-drools.sh`.
  Gates 9 en 10 zijn per constructie ongemoeid: de golden draagt geen
  `bron.catala_en` en geen `bron.rgs`.
- Het `Result:`-briefje bij het verplaatsen naar `done/` benoemt expliciet:
  welke vorm speelde, welke grens is geraakt, en welke van de vier posten
  uit `TOEKOMST_hohfeld_deontiek_20260822.md` §5 door dit bewijs
  waarschijnlijker respectievelijk onwaarschijnlijker is geworden.

May touch: `corpus/golden/jutterswet-meldplichtspoor/**` (nieuw),
`backlog/`.

Out of scope: elke IR-, schema-, verifier- of spec-wijziging (`spec/**` is
protected en er is hier geen nieuwe WF-regel nodig); `corpus/golden/anchor/`
(protected — promotie tot anchor is een maintainersbesluit, niet deze
kaart); `corpus/negatief/` en `corpus/rood/` (de identiteitsgrens is al
gepind door `wf060-text-payload`; nieuwe negatieven horen bij een nieuwe
WF-regel); `scripts/**` (protected — geen survivor- of exclusielijst
aanpassen); elke handelings- of positieknoop, elk beëindigingseffect, elke
frameregel en elke spoorindex in het casusformaat (dat is rij 32/37-werk en
vraagt een fase, geen kaart); en elke verwijzing naar externe normtalen in
corpusdata.
