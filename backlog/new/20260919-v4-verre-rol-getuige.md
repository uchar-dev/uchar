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

## Acceptatie (bij oppakken aanscherpen)

- (a) Hermeting: één minimaal handgeschreven wire-document met de
  verre vorm door verifier, ref, clingo en beide Drools-takken — het
  gedrag per uitvoerder vastgelegd vóór er iets wijzigt.
- (b) Eén handgeschreven golden die de verre vorm draagt, met
  handberekende verwachtingen, gespeeld door ref en clingo; Drools
  gespeeld of benoemde EXCLUDE (kanaallijst-voorstel). Als een
  uitvoerder de vorm vandaag niet kan dragen, is dat dragend maken
  onderdeel van deze kaart — rood-eerst, arbiter is de spec-tekst van
  ch. 4.5; blijkt de vorm principieel onuitvoerbaar, dan stopt de
  kaart en gaat de bevinding als genummerde vraag terug naar de
  maintainer (optie (c) — schrappen — komt dan opnieuw op tafel).
- (c) Eén negatief dat de filler/backref-kruising schendt, geweigerd
  met de WF-regel die de mapnaam draagt (bestaande WF of voorstel voor
  een nieuwe, met verifier-check — beschermd-padvoorstel).
- (d) Alle veertien gates groen; geen wijziging aan de nabije vorm.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`corpus/negatief/**`, `ref/**` en `backends/**` uitsluitend waar (b)
dragend maken vergt, `spec/verifier/**` en `spec/IR-SPEC.md` ch. 4.5
(voorstel), `scripts/check-drools.sh` uitsluitend de kanaallijst
(voorstel), kaartverplaatsing. Out of scope: het samenvoegen zelf
(dat is de v1-klap), `frontends/**` (geen producentroute nodig voor
een handgeschreven getuige).
