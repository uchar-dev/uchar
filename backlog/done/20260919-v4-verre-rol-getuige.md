# v4-verre-rol-getuige: de verre rollidmaatschapsvorm krijgt zijn getuige

Uit `spec/proposals/V1-WIREMIGRATIE.md` V4, **BESLIST (2026-09-19,
maintainer): route (b)** — eerst de getuige, dán (bij de v1-klap, niet
in deze kaart) het samenvoegen van de twee rollidmaatschapsknopen.

De stand (V4, hermeten bij oppakken): `VerreRollidmaatschap` heeft nul
dragers in het hele corpus, en `spec/IR-SPEC.md` ch. 4.5 zegt dat
`filler` en `backref` daarom ongekruist blijven. De samensmelting die
v1 wil is daarmee een speczin zonder machine-controleerbaar artefact —
precies wat de repo-discipline verbiedt. De getuige gaat dat kruisen.
Let op: de V4-tekst meldt dat `ref` de verre vorm bij evaluatie
weigert; wat de drie uitvoerders vandaag werkelijk doen is dus de
eerste meting, niet een aanname.

## Acceptatie (aangescherpt bij oppakken, 2026-09-20)

- (a) **Hermeting eerst.** Eén minimaal handgeschreven wire-document
  met de verre vorm gaat door alle vijf de lezers — verifier, `ref`,
  clingo, de Drools exec-model-tak en de Drools DRL-teksttak — en het
  gemeten gedrag per lezer staat in dit dossier vóór er één regel code
  wijzigt. De V4-tekst *beweert* dat `ref` bij evaluatie weigert; deze
  kaart meet dat en neemt het niet aan.
- (b) **Eén handgeschreven golden** onder `corpus/golden/` die de verre
  vorm draagt, zonder `bron.rgs` (geen producentroute nodig), met de
  verwachte waarden HANDBEREKEND en in dit dossier genoteerd vóór de
  eerste run; gespeeld door `ref` én clingo met exact die waarden.
  Draagt een van beide de vorm vandaag niet, dan is dragend maken
  onderdeel van deze kaart: rood-eerst, arbiter is de spec-tekst van
  ch. 4.5 plus `RUNTIME-SEMANTICS.md` ch. 5.8a, en `ref` krijgt een
  BENOEMDE arm — nooit een stille terugval. Drools speelt mee of krijgt
  een benoemde refusal met een regel in de kanaallijst.
- (c) **Eén negatief** dat de filler/backref-kruising schendt en dat de
  verifier weigert onder de WF-regel die de mapnaam draagt. De WF-zin
  wordt OPGESCHREVEN voordat hij geïmplementeerd wordt; bestaat er een
  WF-regel die de kruising al kan dragen, dan die, anders een nieuw
  nummer met eigen verifier-check. Beide zijn beschermd-padvoorstellen:
  de branch draagt ze, de hoofdsessie merget.
- (d) **Alle veertien gates groen**, elk los gedraaid; geen wijziging
  aan de nabije vorm (`Rollidmaatschap`) en aan geen enkel bestaand
  golden/negatief.

**Stopregel.** Blijkt de verre vorm principieel onuitvoerbaar in `ref`
of clingo, dan stopt de kaart bij (b): de hermeting en het negatief
worden afgerond en de bevinding gaat als genummerde vraag met opties en
aanbeveling terug naar de maintainer (optie (c) uit V4 — schrappen —
komt dan opnieuw op tafel).

## Hermeting (2026-09-20, vóór elke wijziging)

Het meetdocument is met de hand geschreven naar het model van
`corpus/golden/jutterswet-wachtindeling` — feittype `wachtindeling`, rol
`wachtjutter` (Jutter, meervoudig) tegenover rol `toegewezen wachtpost`
(Wachtpost, enkelvoudig) — maar met het ONDERWERP aan de andere kant:
een Wachtpost is `bemand` indien hij een wachtjutter heeft, en
`onbemand` indien hij er geen heeft. De voorwaarde is dus tweemaal
`{"node":"VerreRollidmaatschap","filler":"Jutter","backref":"toegewezen
wachtpost"}`, eenmaal positief en eenmaal genegeerd. Invoer: W1 en W2
zijn Wachtposten, J1 is een Jutter met `toegewezen wachtpost = W1`.
Handberekend: W1 bemand en niet onbemand, W2 onbemand en niet bemand.

| lezer | uitkomst | exit |
|---|---|---|
| verifier | `verifier: PASS` — geen enkele kruising op `filler`/`backref` | 0 |
| `ref` | `EVALUATION FAULT: condition kind not implemented in this card` | 1 |
| clingo | `TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)` | 1 |
| Drools exec-model | `PLAYED 1` — exact de vier handberekende waarden | 0 |
| Drools DRL-tekst | `PLAYED 1` — exact de vier handberekende waarden | 0 |

Twee dingen die de meting toevoegt aan wat V4 aannam. Ten eerste
weigeren `ref` én clingo, maar allebei met een **bodemzin**: beide
sentences staan woordelijk in de verbodslijst van
`scripts/check-rood.sh` (`ref/lib/eval.ml eval_cond` en
`backends/clingo/lib/translate.ml cond_atoms`). Zo'n weigering zegt
niets over de verre vorm — ze zegt alleen dat de arm die haar ving geen
naam heeft voor wat hij ving. De remedie die het gate-stelsel daarvoor
voorschrijft is precies deze kaart: een benoemde arm, geen herformuleerde
bodem. Ten tweede is de Drools-uitkomst tweezijdig gemeten: met één
verwachting omgedraaid meldt elke tak `FAIL … kenmerk bemand: verwacht
afwezig, werkelijk aanwezig`, dus beide takken vergelijken werkelijk en
bevestigen de handberekening.

De kruisingsschending, op hetzelfde document gemeten met
`backref: "wachtjutter"` (de rol van de VULLER in plaats van die van het
onderwerp — de fout die een producent maakt):

| lezer | uitkomst |
|---|---|
| verifier | `verifier: PASS` — het gat dat (c) dicht |
| `ref` / clingo | dezelfde twee bodemzinnen; ze kijken niet naar de velden |
| Drools exec-model | `EXCLUDE transpiler SKIP: … verre rollidmaatschapscheck: het beanmodel leidt voor de rol 'wachtjutter' op Jutter geen referentieveld naar Wachtpost af` |
| Drools DRL-tekst | `EXCLUDE drl-transpiler MARKER:` met dezelfde zin |

Conclusie voor (b): de stopregel treedt NIET in werking. De vorm is in
beide OCaml-uitvoerders gewoon niet geschreven; hij is niet
onuitvoerbaar. `RUNTIME-SEMANTICS.md` ch. 5.7a maakt hem in `ref` en
clingo zelfs goedkoop: een aangeleverde rolschakel vestigt de relatie
zelf en de lader materialiseert het omgekeerde been, dus het onderwerp
draagt de schakel naar de vuller al — de verre check is de
bestaansvraag over die schakel, niet een tweede, eigen leesroute.

## Handberekening vóór de eerste run (2026-09-20)

De golden is `corpus/golden/jutterswet-postbezetting`. Vier regels op de
Wachtpost, allemaal de verre vorm, in twee paren die alleen in hun
vuller verschillen: `bemand`/`onbemand` met vuller Jutter en
`strandbemand`/`strandvrij` met vuller Strandjutter, een strikt subtype
van Jutter. De verwachtingen zijn uitgerekend vóór ref of clingo het
document ooit zag:

| geval | invoer | W1 | W2 |
|---|---|---|---|
| `case1-bezette-post` | J1, een gewone Jutter, draagt `toegewezen wachtpost = W1`; W2 staat leeg | bemand, niet onbemand, **niet** strandbemand, strandvrij | niet bemand, onbemand, niet strandbemand, strandvrij |
| `case2-dubbele-wacht` | W1 draagt `wachtjutters = [J1, J2]` met J2 een Strandjutter; J3 heeft geen post; W2 staat leeg | bemand, niet onbemand, strandbemand, niet strandvrij | niet bemand, onbemand, niet strandbemand, strandvrij |
| `case3-strandwacht` | J1 (Jutter) draagt W1, S1 (Strandjutter) draagt W2 | bemand, niet onbemand, **niet** strandbemand, strandvrij | bemand, niet onbemand, **strandbemand**, niet strandvrij |

Elk geval draagt iets dat de andere niet dragen. Geval 1 spelt de
relatie aan de JUTTERSKANT, dus W1's oordeel hangt aan een been dat geen
enkele caseregel voor W1 schrijft — het been dat ch. 5.7a laat
materialiseren — en zijn `strandvrij` is de klassetoets op zichzelf: de
relatie is er, de klasse niet. Geval 2 spelt hem aan de POSTKANT, zet er
twee vullers in plus een jutter zonder post (twee vullers geven
hetzelfde antwoord als één, en een losse jutter bemant niets) en pint
met zijn Strandjutter de opwaartse helft van de klassetoets. Geval 3 is
de onderscheider: twee posten, elk één vuller, die alleen in KLASSE
verschillen — `bemand` geldt van allebei en `strandbemand` van precies
één, dus de rolbrede lezing en de vullerlezing lopen binnen één wereld
uiteen.

**Waarom de derde regel er is** (fixronde 2026-09-20, lensvondst). De
eerste versie van deze getuige spelde de vuller precies als het
gedeclareerde roltype. Daardoor vielen de twee lezingen van `filler`
samen en kon geen enkel corpusdocument een vullerblinde lezer aanwijzen
— en dat waren `ref` en clingo allebei: ze losten het paar op en lieten
het veld daarna vallen. Gemeten op een verifier-schoon document met een
Strandjutter onder de Jutter: beide OCaml-uitvoerders meldden een post
bemand door een strandjutter die hij niet had, terwijl beide
Drools-takken correct speelden. Het vierkant spleet twee-twee.

## Open punten, geregistreerd en niet gerepareerd

1. **Het ambigue paar.** Twee feittypen die hetzelfde typenpaar door
   gelijknamige onderwerprollen verbinden, maken de verre verwijzing
   dubbelzinnig. WF-034 (t) zwijgt daar met opzet — de lege
   kandidatenlijst is de arm, meerdere entries blijven "WF-048's
   ambiguïteitsvraag", precies de constructie van arm (o) — maar WF-048
   dekt deze knoop feitelijk niet: zijn tekst is gescopeerd op een
   bindingloze verdeling of instantievouw. Het gevolg is een
   verifier-schoon document dat alle vier de uitvoerders LUID weigeren.
   Dat is een bestaand, symmetrisch gat (arm (o) heeft het net zo goed),
   geen regressie van deze kaart, en het verdient een eigen meting over
   beide armen tegelijk. `ref` heeft er een getuige voor
   (`WV-5.8a-ver-ambigu-paar`).
2. **De rolnaamnormalisatie is losser dan het boonmodel.** (t) en beide
   OCaml-uitvoerders vergelijken `backref` onder `norm` (spaties
   samengevouwen, kleine letters), zoals elke andere rolopzoeking in de
   spec. Het boonmodel leidt zijn veldnaam af uit de geschreven rolnaam,
   dus een `backref` die alleen ónder normalisatie klopt wordt door de
   verifier en `ref` gedragen en door de exec-tak uitgesloten met een
   benoemde reden. Dat is geen stil verschil: het kanaalregime van gate
   12 vangt zo'n wissel bij naam. Ook hier: bestaand gedrag van de
   normalisatie, geen vondst van deze vorm.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`corpus/negatief/**`, `ref/**` en `backends/**` uitsluitend waar (b)
dragend maken vergt, `spec/verifier/**` en `spec/IR-SPEC.md` ch. 4.5
(voorstel), `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), kaartverplaatsing. Out of scope: het samenvoegen zelf
(dat is de v1-klap), `frontends/**` (geen producentroute nodig voor
een handgeschreven getuige).

Result: de verre rollidmaatschapsvorm heeft zijn getuige, en de
stopregel trad niet in werking. De hermeting (hierboven) liet zien dat
de aanname waarop V4 stond half waar was: `ref` weigert de vorm
inderdaad, maar met een BODEMZIN — en clingo ook, en beide zinnen staan
woordelijk in de verbodslijst van gate 8. De vorm was dus niet geweigerd
als vorm; hij was ongeschreven. Beide Drools-takken speelden hem al, en
tweezijdig gemeten (met één verwachting omgedraaid melden ze `FAIL`).

Wat er staat. `corpus/golden/jutterswet-postbezetting` is de eerste
drager van `VerreRollidmaatschap` in welke zone dan ook: artikel 12 van
de Jutterswet, vier regels op de Wachtpost, geen `bron.rgs`, drie
gevallen met handberekende absoluten die vóór de eerste run in dit
dossier stonden. Alle vier de uitvoerders spelen ze — `ref` en clingo 8
verwachtingen per geval, beide Drools-takken `PLAYED 3` — dus het
vierkant sluit op de verre vorm. `ref` en de ASP-backend kregen daarvoor
elk een BENOEMDE arm, geen herformuleerde bodem, met één gedeelde
afleiding van het paar (`Eval.far_side_role`) zodat de twee de knoop
niet verschillend kunnen lezen. `RUNTIME-SEMANTICS.md` ch. 5.8a draagt de
verre helft met vijf regels en dit document als bewerkt voorbeeld; de
voorbeelden staan verbatim in `ref/test/test_ref.ml` (702 voorbeelden,
vloer 489).

De kruising. WF-034 krijgt arm (t) en geen nieuw nummer: WF-034 IS de
resolutieregel en de spec noemde deze twee velden zelf al "de verre
tweelingzus van (o)". De zin is opgeschreven vóór hij geïmplementeerd
werd. Hij kruist het PAAR — `filler` en `backref` moeten samen op één
gedeclareerd feittype uitkomen, met `backref` als de rol die het
onderwerp vervult — omdat de knoop geen geschreven rolnaam draagt en het
paar dus de hele verwijzing is. Negatief:
`corpus/negatief/wf034-verre-rolcheck-verkeerde-zijde`, met `backref`
aan de kant van de vuller: beide namen lossen op, het paar niet.
Rood-eerst: vóór de arm ging dat document schoon door de verifier. De
veegslag over 690 documenten (217 golden + 34 rood + 409 negatief + 30
pariteit) verplaatst precies één oordeel en één rapportregel, en de arm
is armexact falsifieerbaar — neutraliseer hem en alleen dat document
wordt schoon.

**Fixronde 2026-09-20 (lensvondst, blokkerend).** De eerste getuige
spelde de vuller precies als het gedeclareerde roltype, en daardoor
viel niet te zien dat `ref` en clingo het veld na de resolutie lieten
vallen: ze beantwoordden de hele rolpopulatie in plaats van de genoemde
klasse. Gemeten op een verifier-schoon document met een `Strandjutter`
onder de `Jutter` spleet het vierkant twee-twee — beide Drools-takken
filteren op de vullerklasse en speelden correct, beide OCaml-uitvoerders
meldden een post bemand door een strandjutter die hij niet had. Beide
zijn gerepareerd met de extensionele klassetoets (`ref` filtert de
bereikte objecten op `⊑ filler`, de ASP-vertaling hangt een `isa/2`
aan de join; de genegeerde vorm rijdt op dezelfde gefilterde lijst mee,
zodat positief en negatief niet over verschillende objecten kunnen
gaan). De getuige is uitgebreid zodat het corpus het verschil kan zien:
een `Strandjutter`-subtype, twee extra regels met die vuller, en een
derde geval waarin `bemand` van twee posten geldt en `strandbemand` van
één. Rood-eerst gedaan en gecommit: commit `96d31df3` zet gate 5 en 6
rood, `824303e6` haalt ze groen. Eén eigen misser is er ook geweest en
staat in `203fd7d4`: een `git checkout` tijdens een mutantproefje
zette de nog ongecommitte filterlezing in `ref` terug, waarna het
vierkant even drie-een stond; gate 5 betrapte het op de eigen suite.

Wat de kaart met opzet NIET doet. De kardinaliteit blijft ongekruist:
`RUNTIME-SEMANTICS.md` ch. 5.7a maakt de relatie symmetrisch, dus geen
van beide OCaml-uitvoerders leest een richting, en een MUST daar zou
documenten weigeren die twee van de vier uitvoerders correct uitvoeren.
Het boonmodel weigert zijn eigen grens al per vorm met een eigen reden.
De vuller BOVEN het roltype wordt wél geweigerd — (t) gebruikt de
eenzijdige `fills`-toets, net als arm (o) — en dat is als bewuste keuze
in de spec-tekst vastgelegd, niet als vondst. De twee lege spellingen
(`filler` weg, `backref` weg) vouwen op het (s)-precedent in dezelfde
arm in plaats van elk een eigen negatief te krijgen; ze zijn buiten het
corpus gemeten en ze zijn NIET hetzelfde defect — een lege `backref` is
overal luid, een lege `filler` laat beide Drools-takken ongeclassificeerd
omvallen met een `NullPointerException`. De twee open punten hierboven
(het ambigue paar; de normalisatie tegenover het boonmodel) zijn
geregistreerd en niet gerepareerd: allebei bestaand en symmetrisch met
arm (o), en allebei een eigen meting waard. Het samenvoegen van de twee
knopen blijft de v1-klap, en `frontends/**` is niet aangeraakt.

Gates, elk los gedraaid na de fixronde, alle veertien groen: 1 PASS; 2
PASS (251 documenten, vloer 192); 3 PASS (251 golden schoon, 409
negatief geweigerd voor hun eigen regel, vloer 350); 4 PASS (589
casebestanden); 5 PASS (702 bewerkte voorbeelden, vloer 489; 555 cases
exact, vloer 427); 6 PASS (452 suitechecks, vloer 83; 555 cases); 7 PASS
(519 cases weerleggen de nietsdoener, 36 overlevers); 8 PASS (34 rode
cases, vloer 26; 68 fragmenten citeren geen van de 36 bodemzinnen); 9
PASS (201 checks, 12 catala-goldens); 10 PASS (322 tests, 63
rgs-goldens); 11 PASS (29/29 in ref, clingo en Drools); 12 PASS (played
165, DRL-tekst 142, JUnit 502 tests, vloer 239); 13 PASS (129
verdraaiingen over 25 goldens, vloer 74; 23 gespeeld door de
DRL-teksttak, vloer 13); 14 PASS (130 checks, 2 flogic-goldens).

Buiten de kaart gemeld. (1) `scripts/check-drools.sh`: alleen de twee
gelijkheidspinnen, `EXPECTED_PLAYED` 164 → 165 en `EXPECTED_DRL_PLAYED`
141 → 142, elk met een redenblok erboven; geen enkele kanaallijstregel
erbij of eraf, want de golden komt als SPELER binnen, en de fixronde
verschoof ze niet verder omdat de getuige is UITGEBREID en niet
verdubbeld. (2) `scripts/check-rood.sh`: één redenkolom herschreven —
die van `ref`'s conditie-bodemzin, die na deze kaart geen restarm meer
beschrijft maar één vorm (`Periode_conditie`). Tekstueel, geen telling
beweegt, geen zin uit de lijst verwijderd. (3)
`spec/RUNTIME-SEMANTICS.md` stond niet in "May touch" en is toch
aangeraakt, tweemaal en minimaal: de verre helft van ch. 5.8a is
toegevoegd (een semantiekarm zonder tekst zou een arm zonder uitspraak
zijn) en in de operandvolgorde-tabel is de regel
"*(ref declines `VerreRollidmaatschap`)*" geschrapt, die door deze kaart
onwaar werd. (4) De IR-SPEC-diff raakt ch. 4.1 (de open lijst van het
naamgevingsplan), ch. 4.5 (de `RoleMembership`-rij), ch. 4.6 (arm (t))
en de WF-index — alle vier gemandateerd door acceptatiepunt (c), dat de
WF-regel met verifier-check als beschermd-padvoorstel vroeg. Alles
onder `spec/` in deze branch is een voorstel; de hoofdsessie merget. (5)
`ref/lib/eval.ml`: de restarm die de verre vorm ving draagt nu alleen
nog `Periode_conditie`, met ongewijzigde bodemzin. De cure is een
benoemde arm voor de periodeconditie en die raakt de lijst in
`scripts/check-rood.sh`; eigen kaartje.
