# Eenheden op referenties: de verifier kruist soort, geen eenheid

Goal: Een verifier-clean model kan op een `AttribuutRef` (en sinds de
Let-kaart ook op een `BinderRef`) een eenheid claimen die niet bij de
declaratie past — WF-078/WF-023 kruisen alleen de datatype-soort, niet
de eenheid. Lens-1-meting (2026-09-01, flankenpaar buiten het corpus):
zo'n eenheidsleugen geeft ref 550 waar Drools 500.05 zegt — een stille
executor-divergentie op een model dat gate 3 goedkeurt. De vorm is
pre-existent (de kale AttribuutRef-flank divergeert identiek, zonder
Let). Onderzoek en besluit: hoort een eenheids-consistentiecheck op
referentietypen in de WF-regels (verifier, rood-eerst met
negatief-geval), of is de declaratie-zijde de enige waarheid en moeten
de executors het ref-type negeren? Ref is scheidsrechter; de spec
beslist.

Acceptance:
- De divergentie gereproduceerd als gedocumenteerd flankenpaar
  (rood-eerst).
- Een voorstel-diff voor de WF-regel (protected path — als proposal
  achterlaten, genummerde vraag aan de maintainer) met minstens één
  passend negatief-geval; óf een gemotiveerd besluit dat de
  executor-kant moet veranderen, dan de backend-fix met witnesses.
- Alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `corpus/negatief/**`,
`backends/drools/src/**`, `backlog/`, `spec/IR-SPEC.md` (proposal;
maintainerbesluit 2026-09-03 — de facetmotor-clausule en de
WF-091-indexrij horen bij de regel).

Out of scope: de v1-vormen, corpuspins van bestaande goldens.

## Maintainerbesluit 2026-09-03 (na de meetfase)

De meetfase (flankenpaar gereproduceerd: 550,00 in ref/clingo tegen
500,05/0,55/50.500/550.000 in Drools op vier verifier-schone
typeposities; nul over-afwijzing over 1.023 referenties in
golden+rood+toka; WF-035 vangt structureel de onschadelijke,
niet-converteerbare helft) leidde tot een structuurkeuze in plaats van
een losse eenheidsregel:

1. **Facetmotor + WF-091-paraplu.** Eén vergelijking van het volledige
   geserialiseerde referentietype tegen de declaratie, op de negen
   armen (zes lezend via de `use_site_identity`-plekken, drie
   schrijvend via de WF-023-machinerie). Het eerste afwijkende facet
   kiest het nummer: domein (ci) → WF-089/WF-090, binder-soort →
   WF-078, elk overig facet (soort op niet-binderreferenties, eenheid,
   decimalen, geheel, teken, rest) → **WF-091** met de facetnaam in de
   melding. Een volgend facet kost geen nieuwe kaart meer. Alle 220
   bestaande negatiefs behouden hun nummer.
2. **Geen herschrijvende canonicaliser** (silent-fallbackbezwaar;
   frontend-drift moet de verifier melden, niet gate 2 helen). In
   plaats daarvan: de v1-delta — het referentietype verdwijnt van de
   wire — als gevolgde afwijking in CONSTRUCTS (hoofdsessie), en een
   eigen kleine kaart voor de ene niet-declaratie-getrouwe emitterplek.
3. **Drools blijft in deze kaart onaangeraakt**; de consumentenclausule
   (RUNTIME-SEMANTICS), het verdraai-harnas en de herschaal-werklijst
   worden een eigen kaart, net als het pinnen van de getuigeloze armen
   en het WF-007-doelgat uit de bijvangst.

## Bouwnotitie (2026-09-03)

Geland: de facetmotor in `use_site_identity`, twaalf wf091-negatiefs, de
IR-SPEC-clausule + WF-091-indexrij + WF-078-herformulering (voorstel).
Alle twaalf gates groen; gate 3 172 golden / 232 negatief.

Drie afwijkingen van de opdracht, met reden:

1. **Zeven armen, geen negen.** Van de drie schrijfposities draagt
   alleen het REGELDOEL een eigen geserialiseerd type (`doel` is een
   expr). Een objectcreatie-lid (`Toekenning.attribuut`) en een
   defaults-consequentie (`PrioritizedDefaults.attribuut`) NOEMEN hun
   doel — er is daar geen referentietype om tegen de declaratie te
   leggen. Wat op die twee posities kruist is de TOEWIJZING (waarde
   tegen declaratie) onder WF-023/WF-090, en dat is compatibiliteit, geen
   identiteit: identiteit eisen zou elk literal-in-attribuut afwijzen. De
   motor is dus geregistreerd op zes leesarmen + het regeldoel; de
   `wf090-schrijf-*`-negatiefs blijven onaangeraakt bij hun eigen wet.
   *Bijgesteld in de fixronde: vijf leesarmen + het regeldoel — de
   doorgeef-aggregatie is eruit, zie onder.*
2. **Twee extra negatiefs** buiten de opgedragen lijst: `-teken` en
   `-reststructuur`. Zonder die twee zou de clausule zeven facetten
   noemen waarvan er twee door geen artefact falsifieerbaar zijn.
3. **De facetvolgorde pint het NUMMER, niet de weigering.** Gemeten: de
   reststructuurfacet is een vangnet dat elk verschil ziet, dus het
   uitschakelen van een genoemd facet degradeert alleen de MELDING
   (`residual structure mismatch`) — het document blijft geweigerd.
   Alleen de reststructuurfacet uitschakelen laat een document passeren.
   Het nummerkeuze-deel is wél falsifieerbaar (soortfacet uit →
   `wf078-binderref-type-lie` wordt WF-091; domeinfacet uit → alle 21
   wf089/wf090-gevallen worden WF-091). Zo staat het in de indexrij.
   *Bijgesteld in de fixronde: 18 van de 24, niet 21 — de drie
   doorgeefluik-gevallen en de drie `wf090-schrijf-*`-gevallen lopen niet
   door de motor.*

Extra incidentele meldingen op BESTAANDE negatiefs, nagemeten tegen de
verifier van vóór de motor (`b3648f8`): ACHT bij de landing, niet zes —
zeven met een extra WF-091 (`wf007-whole-with-decimals`,
`wf027-lying-attribuutref-type`, `wf035-power-with-unit`,
`wf036-order-number-vs-text`, `wf037-fallback-on-minimum`,
`wf040-unpinned-cell`, `wf072-expressietype-volgorde`) en
`wf023-schrijf-doeltype-leugen` met een extra WF-089 via de regeldoelarm.
Na de fixronde zijn het er ZEVEN: de extra op `wf037-fallback-on-minimum`
liep via de doorgeef-aggregatiearm op de tekenfacet en vervalt met F1.
Elk van de zeven behoudt zijn eigen nummer; gate 3 staat extra meldingen
naast de gedeclareerde regel toe.

Bijvangst voor een eigen kaart: (a) de armen zonder positieve
golden-getuige voor de eenheidsfacet — `ParameterRef` en `DimensieCel`
(gemeten nul; de pariteitszone heeft twaalf eenheiddragende
`ParameterRef`-posities); (b) WF-078 defereerde tot nu toe NIET op een
binder met het onbekend-sentinel terwijl ch. 4.7 zegt dat het defereert —
de motor volgt de spec en defereert nu wel; (c) **twee ongepinde
deferral-kanalen** (gevonden in de leesronde, hier bewust NIET gedicht —
eigen kaart). Ten eerste: een `AttribuutRef` met `objecttype: null`
defereert de motor (de declaratie is niet op te zoeken) én ontloopt de
resolutiecheck, dus een `t`-voor-`kg`-eenheidsleugen op zo'n referentie
is verifier-schoon (gemeten); geen enkel golden-, rood- of
pariteitsdocument gebruikt de vorm (gemeten nul), en ref faalt er hard
op. Ten tweede: het regeldoel is de enige positie waar het
v0-Onbekend-sentinel op een TYPE mag staan — dezelfde sentinel op een
declaratie wordt door WF-003 geweigerd (`unresolved datatype`), op een
`doel/type` passeert hij (gemeten).

## Fixronde (2026-09-03)

1. **De doorgeef-aggregatie terug naar het domeinfacet.** De motor
   kruiste daar élk facet tegen élke operand, wat structureel
   onvervulbaar is: een foldtype is een AFGELEIDE over een operandenlijst
   (de eigen frontend: min-decimalen, AND-geheel, teken en eenheid van
   operand 0; EERSTE/LAATSTE nemen operand 0 geheel), geen kopie van één
   declaratie. Gemeten over-afwijzing op de door ch. 4.6 gezegende vorm
   "het minimum van het gewicht en 0". De arm roept nu `use_site_domain`
   aan; de drie doorgeefluik-negatiefs blijven rood op hun eigen nummer
   en er bestond geen wf091-negatief op deze arm.
2. **Eenheidsnamen vouwen ci** in `eenheid_identical` én in `dt_ci` —
   beide beloofd door de clausule, geen van beide waargemaakt. Factor en
   noemerstructuur blijven exact, dus kg/t blijft een leugen.
3. **Zeven NOTE-correcties.** Zes NOTE's beweerden "drop the facet and
   this case alone turns clean"; gemeten is dat het vangnet elk verschil
   ziet, dus de melding degradeert naar `residual structure mismatch` en
   het geval blijft WF-091. Plus een verouderd meldcitaat.
4. **Indexrij-getallen zelf nagemeten** (per-arm-teller in de motor,
   over beide corpuszones en de pariteitszone). De 1.047 uit de rij is
   door geen definitie te reproduceren; de rij draagt nu 921 uitgevoerde
   vergelijkingen (826 golden + 95 rood, 38 eenheiddragend) + 147 in de
   pariteitszone (30 eenheiddragend), met erbij wat de telling uitsluit.

## Fixronde 2 (2026-09-03)

Twee kanalen die fixronde 1 openliet, elk gemeten.

1. **Het kastkanaal is dicht.** Punt 2 hierboven is TERUGGEDRAAID: de drie
   eenheidsnamen worden weer byte voor byte vergeleken, in
   `eenheid_identical` én in de reststructuurvorm (`eenheid_ci` vervalt).
   Naamresolutie is ci — WF-001's domein — maar WF-091 gaat over
   kopie-getrouwheid, en dat is een andere vraag: een kopie die alleen in
   kast liegt (`KG`/`GEWICHT`/`KG` tegen `kg`/`Gewicht`/`kg`, factor 1 aan
   beide kanten) was na fixronde 1 verifier-schoon terwijl ref 3,75
   antwoordt en het Drools-pad 3.750,00 — de eenheidslogica daar vergelijkt
   case-sensitief en valt bij mismatch stil terug op factor één. Het
   domeinfacet blijft ci, gepind door `jutterswet-dossierspelling` en
   `jutterswet-vaarwaterspelling` (opnieuw nagemeten: precies die twee).
2. **De foldresultaatklasse is heropend en eerlijk gecard.** De clausule
   schreef de liegende NUMERIEKE facetten van een foldwortel toe aan
   WF-035/WF-038; dat is meetbaar onwaar (die kruisen convertibiliteit, en
   kg/t zijn convertibel). Op beide plekken — clausule en indexrij — staat
   nu een ongedekt-notitie: een resultaattype is een afgeleide zonder één
   declaratie tegenover zich, de klasse is gemeten divergent (3,75 in ref
   tegen 3.750,00 in het Drools-pad op een eenheidsleugen op een
   waardefoldwortel) en het sluiten ervan vraagt een AFLEIDINGSwet voor
   resultaattypen. Vierde honesty-notitie erbij: het domein-alleen-besluit
   op de doorgeef-aggregatiearm is door geen artefact gepind — een golden
   kan de tegenvorm niet dragen (de ASP-vertaling weigert de multi-operand
   `MINIMUM`/`MAXIMUM` vóór vertaling) en een negatief pint geen
   niet-weigering.

Twee nieuwe negatiefs (232 → 234): `wf091-attribuutref-eenheid-kast` (de
kastleugen; vouw de namen weer ci en dit ene geval passeert — gate 3 rood,
gemeten) en `wf091-dimensiecel-eenheid` (de celLEZING met een trouwe
binnenreferentie), waarmee elke vol-kruisende arm een eenheidsgeval heeft.
Alle 232 bestaande negatief-meldingen letterlijk nagemeten vóór en ná de
byte-vergelijking: geen enkele verandert.

Voor de vervolgkaart, naast de bijvangst hierboven: (a) de
foldresultaatklasse — de afleidingswet voor resultaattypen; (b) de stille
factor-één-terugval in de Drools-eenheidslogica, die van een
naamverschil een verkeerd antwoord maakt in plaats van een fout; (c) het
verdraai-harnas.
