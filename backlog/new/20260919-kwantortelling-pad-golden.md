# kwantortelling-pad-golden: de telling over een meerstapsbereik krijgt haar getuige

Gemeten (aantal-setbron-lens, 2026-09-19): een tellende kwantor
(`TEN_MINSTE n`) over een tweestaps `bronPad` (Jutter→Boot→Vondst) is
verifier-SCHOON — WF-108 (c) laat `bronPad` op de kwantorvouw toe en
`RUNTIME-SEMANTICS.md` ch. 3.9a deelt daar het bereik van ch. 3.4a/3.4b
— en wordt door `ref` én clingo beantwoord. Het lensdocument scheidde
bovendien de VLAKKE UNIE van de geneste lezing: met één via twee
tussenexemplaren bereikbaar element telt vlak 3 distinct (kenmerk
afwezig bij drempel 4) waar genest 4 voorkomens zou tellen (aanwezig).
Beide OCaml-uitvoerders antwoordden vlak — maar niets pint dat: er is
geen corpus-golden met een tellende kwantor over een meerstapsbereik,
en de versmalde WF-102-COUNT-passage in `spec/IR-SPEC.md` ("niet
uitdrukbaar als AANTAL-vouw"; de kwantorroute is de knoop die wél telt)
staat daarmee zonder positieve getuige — precies wat de huisregel
verbiedt zodra iemand de positieve claim opschrijft.

## Acceptatie (bij oppakken aanscherpen)

- (a) Eén handgeschreven golden (of frontend-geëmitteerd, als de
  producentroute bestaat — meten): een tellende kwantor over een
  tweestaps `bronPad` met een gedeeld element, handberekende
  verwachtingen die de vlakke unie pinnen (het gedeelde element telt
  één keer), casus met beide uitkomsten van de drempel.
- (b) Gespeeld door ref en clingo; Drools gespeeld of benoemde EXCLUDE
  in BEIDE kanaallijsten (exec én DRL — beschermd-padvoorstel).
- (c) Daarna mag de positieve zin in de WF-102-COUNT-passage
  (voorstel): de kwantorvouw telt over een meerstapsbereik, vlak, met
  deze golden als getuige.
- (d) Alle veertien gates groen.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`scripts/check-drools.sh` uitsluitend de kanaallijsten (voorstel),
`spec/IR-SPEC.md` (voorstel), kaartverplaatsing. NB: serieel met
DrlTranspiler/check-drools-kaarten (de kanaallijsten bewegen mee).
