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
