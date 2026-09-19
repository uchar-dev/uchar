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

De golden is `corpus/golden/jutterswet-postbezetting`. Twee regels op de
Wachtpost, allebei de verre vorm — `bemand` indien hij een wachtjutter
heeft, `onbemand` indien hij er geen heeft. De verwachtingen zijn hier
uitgerekend vóór ref of clingo het document ooit zag:

| geval | invoer | W1 | W2 |
|---|---|---|---|
| `case1-bezette-post` | J1 draagt `toegewezen wachtpost = W1`; W2 staat leeg | bemand, niet onbemand | niet bemand, onbemand |
| `case2-dubbele-wacht` | W1 draagt `wachtjutters = [J1, J2]`; J3 heeft geen post; W2 staat leeg | bemand, niet onbemand | niet bemand, onbemand |

De twee gevallen dragen met opzet dezelfde uitkomst en om twee
verschillende redenen. Geval 1 spelt de relatie aan de JUTTERSKANT, dus
W1's oordeel hangt aan een been dat geen enkele caseregel voor W1
schrijft — het been dat ch. 5.7a laat materialiseren. Geval 2 spelt hem
aan de POSTKANT en zet er twee vullers in plus een jutter zonder post:
twee vullers geven hetzelfde antwoord als één (bestaan is geen telling)
en een losse jutter bemant niets.

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
de Jutterswet, twee regels op de Wachtpost, geen `bron.rgs`, twee
gevallen met handberekende absoluten die vóór de eerste run in dit
dossier stonden. Alle vier de uitvoerders spelen ze — `ref` 4 en 4,
clingo 4 en 4, beide Drools-takken `PLAYED 2` — dus het vierkant sluit
op de verre vorm. `ref` en de ASP-backend kregen daarvoor elk een
BENOEMDE arm, geen herformuleerde bodem, met één gedeelde afleiding van
het paar (`Eval.far_side_role`) zodat de twee de knoop niet verschillend
kunnen lezen. `RUNTIME-SEMANTICS.md` ch. 5.8a draagt de verre helft met
vier regels en dit document als bewerkt voorbeeld; de voorbeelden staan
verbatim in `ref/test/test_ref.ml` (695 voorbeelden, was 694 vóór de
laatste toevoeging, vloer 489).

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

Wat de kaart met opzet NIET doet. De kardinaliteit blijft ongekruist:
`RUNTIME-SEMANTICS.md` ch. 5.7a maakt de relatie symmetrisch, dus geen
van beide OCaml-uitvoerders leest een richting, en een MUST daar zou
documenten weigeren die twee van de vier uitvoerders correct uitvoeren.
Het boonmodel weigert zijn eigen grens al per vorm met een eigen reden.
De twee lege spellingen (`filler` weg, `backref` weg) vouwen op het
(s)-precedent in dezelfde arm in plaats van elk een eigen negatief te
krijgen; ze zijn buiten het corpus gemeten en ze zijn NIET hetzelfde
defect — een lege `backref` is overal luid, een lege `filler` laat beide
Drools-takken ongeclassificeerd omvallen met een `NullPointerException`.
Het samenvoegen van de twee knopen blijft de v1-klap, en `frontends/**`
is niet aangeraakt.

Gates, elk los gedraaid, alle veertien groen: 1 PASS; 2 PASS (251
documenten, vloer 192); 3 PASS (251 golden schoon, 409 negatief
geweigerd voor hun eigen regel, vloer 350); 4 PASS (588 casebestanden);
5 PASS (695 bewerkte voorbeelden, vloer 489; 554 cases exact, vloer
427); 6 PASS (452 suitechecks, vloer 83; 554 cases); 7 PASS (518 cases
weerleggen de nietsdoener, 36 overlevers); 8 PASS (34 rode cases, vloer
26; 68 fragmenten citeren geen van de 36 bodemzinnen); 9 PASS (201
checks, 12 catala-goldens); 10 PASS (322 tests, 63 rgs-goldens); 11 PASS
(29/29 in ref, clingo en Drools); 12 PASS (played 165, DRL-tekst 142,
JUnit 502 tests, vloer 239); 13 PASS (129 verdraaiingen over 25 goldens,
vloer 74); 14 PASS (130 checks, 2 flogic-goldens).

Buiten de kaart gemeld. (1) `scripts/check-drools.sh`: alleen de twee
gelijkheidspinnen, `EXPECTED_PLAYED` 164 → 165 en `EXPECTED_DRL_PLAYED`
141 → 142, elk met een redenblok erboven; geen enkele kanaallijstregel
erbij of eraf, want de golden komt als SPELER binnen. (2)
`spec/RUNTIME-SEMANTICS.md` stond niet in "May touch" en is toch
aangeraakt, tweemaal en minimaal: de verre helft van ch. 5.8a is
toegevoegd (een semantiekclausule zonder tekst zou een arm zonder
uitspraak zijn) en in de operandvolgorde-tabel is de regel
"*(ref declines `VerreRollidmaatschap`)*" geschrapt, die door deze kaart
onwaar werd. Beide zijn beschermd-padvoorstellen, net als de
IR-SPEC-wijzigingen. (3) `ref/lib/eval.ml`: de restarm die de verre vorm
ving draagt nu alleen nog `Periode_conditie`, met ongewijzigde bodemzin.
Die zin beschrijft daarmee één bekende vorm in plaats van een restklasse
— een verbetering die een eigen kaartje verdient, want de cure is een
benoemde arm voor de periodeconditie en die raakt de lijst in
`scripts/check-rood.sh` (beschermd, en een parallelle kaart zit daar
ook).
