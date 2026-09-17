# smt-backend: een verificatiebackend naast het differentiaalvierkant (optie)

Aanleiding: het oorspronkelijke ontwerpantwoord (calculemus-sessie
2026-08-03, elf dagen vóór de uchar-opdracht) was een tweeluik —
"ASP (clingo) als primaire extra target, met SMT als reken-coprocessor,
beide via een default-logic-kern". De ASP-helft is gebouwd; de SMT-helft
niet. Maintainersbesluit 2026-09-17: een SMT-backend is nuttig, als
optie vastgelegd. Doel-solvers: Z3 of cvc5, via SMT-LIB.

Wat het is: een vierde backend in de zandloper (IR → SMT-LIB), maar met
een ander doel dan de drie executors. SMT voert geen casus uit; het
beantwoordt vragen over ALLE mogelijke casussen tegelijk — SAT levert
een concreet tegenvoorbeeld, UNSAT is een bewijs over de hele
inputruimte (beslisbaar voor het lineaire-rekenkundefragment dat
wetgeving overwegend nodig heeft).

Twee rollen:

1. **Verificatiepoort naast het vierkant.** Eerste vraagvorm:
   botsingsvrijheid — kunnen twee regels ooit tegenstrijdige waarden
   aan één doel toekennen, over alle inputs? Latere vraagvormen:
   dekking (bestaat een input die geen regel raakt), bereik (kan een
   bedrag negatief worden), equivalentie van herschrijvingen. Een
   SAT-uitkomst levert het tegenvoorbeeld machinaal — kandidaat voor
   `rood`/`negatief`-materiaal. Afnemer buiten de gates: de werkbank
   ("welke input leidt tot deze uitkomst" is dezelfde
   model-generatie).
2. **Celgenerator voor de kaartdiscipline.** De discriminerende cellen
   die nu met de hand worden verzonnen (precies-op-de-drempel, de
   GEEN≠!ALLE-cel) kan de solver produceren: vraag een dossier waarop
   twee lezingen verschillend antwoorden. De hand blijft de controle;
   de solver wordt de vinder.

Wat het aan de IR-ontwikkeling toetst (de vierde onafhankelijke
lezing): de vertaling is denotationeel — elke regel wordt een
beperking, geen uitvoeringsstap — dus al het stiekem operationele in
de IR vertaalt niet en valt op; partialiteit (leeg) moet expliciet als
optietype; en de vertaler classificeert onvermijdelijk welk
IR-fragment in beslisbare theorieën valt en waar het knelt (verdeling
naar rato is vermenigvuldiging met een variabele; afronding vergt
codeerwerk). Die beslisbaarheidskaart bestaat nu nergens en is zelf
spec-informatie.

Randvoorwaarde uit het oorspronkelijke ontwerp: de niet-monotone kern
(de defaults-voorrangsstapel) gaat NIET rechtstreeks naar de solver —
eerst volledig reduceren door de ch. 7-semantiek tot expliciete
gevalsanalyse, anders herintroduceert de vertaling de instorting die
het tweeluik juist vermeed. De grens blijft: dit bewijst interne
eigenschappen van de regelset, nooit dat de vertaling de bedoeling van
de wet raakt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een `backends/smt/` die voor een gemeten IR-fragment (scalaire
  regels, vergelijkingen, lineaire rekenkunde, defaults ná reductie)
  SMT-LIB emitteert; vormen buiten het fragment weigeren bij naam.
- (b) De eerste vraagvorm (botsingsvrijheid per doel) draait over het
  bestaande corpus; elke gevonden botsing is óf een echte specfout
  (adversariële triage) óf een gepind, gemotiveerd tegenvoorbeeld.
- (c) Een demonstratie van rol 2: minstens één bestaande golden-cel
  machinaal gereproduceerd als solver-antwoord, naast de handberekening.
- (d) Gate-vorm naar het huispatroon: nooit stil groen, lege sets
  falen, tellingen met vloeren; solver-aanwezigheid gedraagt zich als
  gate 9's catala-conditie (luide skip waar de solver ontbreekt) of
  onvoorwaardelijk — meten wat runners dragen.
- (e) Alle bestaande gates blijven groen; registervoorstel in het
  Result.
