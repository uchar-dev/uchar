# diedat-filter: de deelselectie op de kwantorvouw bereikt de frontend

Aanleiding: de forall-kaart (`backlog/done/20260916-heelperiode-forall.md`,
afwijking c) stelde vast: de die/dat-deelselectie op een Kwantorvouw
("… van alle bergers DIE AANGEMELD ZIJN van de …") is gespecificeerd
(IR-SPEC ch. 4.5: `filter`, per bijdrager met het element als subject) en
door `ref` gepind, maar de RegelSpraak-frontend produceert hem niet, dus
het corpus draagt hem niet. De asymmetrie-kaart mat intussen dat de
backends de filter al dragen (clingo `fatoms`; Drools in de nieuwe
niet-temporele armen, per lens-probe). De maintainer wil de rand gedicht:
de frontend produceert de filter, en het corpus krijgt zijn drieweg-getuige.

De typerange-kaart parkeerde daarnaast één residu als "filtervraag": het
temporele vertaaltijd-hek moet een door de filter WEGgevallen lid met een
asOfDate-emmerwaarde niet als fout aanmerken (het lid doet niet mee).
Die getuige hoort bij deze kaart.

## Acceptatie

- (a) De frontend (`frontends/regelspraak`) parseert de die/dat-deelselectie
  op de kwantorvouw-positie en emitteert `filter` op de wire, met
  JUnit-getuigen in de bestaande suite-stijl (de gedragen spelling(en),
  de wire-vorm, en minstens één geweigerde/onopgeloste randvorm die luid
  valt). Meet EERST wat de huidige stand is (welke spelling resolvet
  vandaag naar wat — Onopgelost, WF-096, of een fout antwoord) en zet
  die meting als rood-eerst in het Result.
- (b) Minstens één corpus-golden MET `bron.rgs` (Jutterswet-domein) die de
  filter door het vierkant speelt: een heelperiode-matrix met
  deelselectie (drieweg: ref, clingo, Drools-exec; de DRL-tekst-tak
  markeert de heelperiodevorm — kanaalconform), en cellen die de filter
  onderscheidend maken: een lid BUITEN de selectie met een waarde die
  het verdict zou omkeren als hij meetelde; een selectie die leeg
  uitvalt (vacuüm); en de gewone gemengde cel. Handberekend vóór het
  draaien, in NOTE.md en Result. Overweeg een tweede golden of extra
  cellen op de NIET-temporele matrix met filter (die speelt sinds de
  asymmetrie-kaart drieweg, ook in de DRL-tekst-tak).
- (c) Het typerange-residu krijgt zijn getuige: een weggefilterd lid met
  een asOfDate-emmerwaarde laat de temporele vouw ongemoeid (geen
  vertaaltijd-weigering, geen bijdrage) — in de zone die past (cel in
  de golden van (b) of een eigen golden), gemeten in ref én clingo.
- (d) Alle veertien gates groen; gate 12-bewegingen (EXPECTED_PLAYED,
  kanaallijst) met eigen gedateerde stappen — gemandateerd; andere
  scripts niet aanraken.
- (e) Bestaande goldens/negatiefs/tests ongewijzigd; anchor onaangeraakt;
  `spec/CONSTRUCTS.md`/`spec/PLAN.md` niet aanraken — registervoorstel
  (rij 40-aanvulling: de frontendgrens verschuift) als letterlijke
  tekst in het Result. Als IR-SPEC- of RUNTIME-SEMANTICS-tekst
  aantoonbaar moet meebewegen: NIET doen, maar het voorstel met
  letterlijke tekst in het Result zetten (geen mandaat in deze kaart).
- (f) Backends alleen aanraken als een meting een gat toont (de filter
  wordt geacht gedragen te zijn); elke backend-wijziging met de meting
  erbij.

## Result

(door de kaartronde in te vullen)
