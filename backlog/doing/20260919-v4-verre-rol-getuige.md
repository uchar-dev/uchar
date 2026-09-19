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

May touch: nieuwe goldenmap onder `corpus/golden/`,
`corpus/negatief/**`, `ref/**` en `backends/**` uitsluitend waar (b)
dragend maken vergt, `spec/verifier/**` en `spec/IR-SPEC.md` ch. 4.5
(voorstel), `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), kaartverplaatsing. Out of scope: het samenvoegen zelf
(dat is de v1-klap), `frontends/**` (geen producentroute nodig voor
een handgeschreven getuige).
