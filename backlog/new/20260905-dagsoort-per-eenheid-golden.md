# Een publieke golden voor `dagsoortPerEenheid`

Goal: één publieke golden die de vorm `dagsoortPerEenheid` draagt — een
gedeclareerde dagsoort die als **eenheidsspelling** in een tijdsduur of een
datumrekening gebruikt wordt — met een case-bestand en gepinde verwachtingen,
zodat de zesde WF-034-golf haar kruisingsarm alsnog kan krijgen.

## Waarom deze kaart bestaat

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) heeft
`dagsoortPerEenheid` **gemeten en bewust open gelaten**, en dat is genoteerd in
de open-lijst van `spec/IR-SPEC.md` ch. 4.6. De meting, 2026-09-05 over de vier
zones (155 golden + 22 rood + 303 negatief + 30 pariteit):

    dagsoortPerEenheid   60 ingangen — ALLE 60 in de pariteitszone,
                          0 in golden, 0 in rood, 0 in negatief

De map koppelt een eenheidsspelling aan een gedeclareerde dagsoort; de
verifier leest hem (`day_kind_unit`, de dagsoort-eenheid van WF-035/WF-038) en
kruist de **waarde** nergens tegen `dagsoorten`. Een kruisingsarm zou dus een
echte lek dichten — maar hij zou geen enkele publieke getuige hebben, en de
kaarttekst van de zesde golf verbiedt met zoveel woorden wat dan overblijft:

> Een negatief bouwen op een vorm die geen enkele golden draagt is de derde weg
> en is verboden: hij zou een clausule geven die geen enkel echt document ooit
> passeert, en de sweep zou dat niet zien.

De pariteitszone kan die getuige niet leveren: geen gate draait de verifier
eroverheen (gate 3 loopt de drie corpuszones), de zone is tijdelijk, en haar
inhoud mag het publieke corpus niet in.

## Wat deze kaart moet opleveren

- Eén golden onder `corpus/golden/` (Jutterswet-materiaal, eigen auteurschap)
  die `dagsoorten` declareert **en** `dagsoortPerEenheid` vult — een
  eenheidsspelling die naar zo'n dagsoort verwijst, gebruikt in een
  `TIJDSDUUR` of een datum ± duur, zodat de vorm ook echt uitgevoerd wordt.
- Een `case*.json` met met de hand gepinde verwachtingen; gates 5 en 6 moeten
  hem replayen (ref ≡ clingo), en gate 12 hem spelen of met reden weigeren.
- Bij voorkeur ook een `bron.rgs`, zodat gate 10 de vorm door de eigen frontend
  heen byte-identiek terugkrijgt.

Pas daarna is de kruisingsarm te schrijven: *de waarde van elke
`dagsoortPerEenheid`-ingang MOET een gedeclareerde dagsoort noemen* — met een
negatief dat de golden één veld verdraait, en de arm-exacte mutant erbij.

## Acceptance

- De nieuwe golden is verifier-schoon en replayt exact in ref én clingo.
- `dagsoortPerEenheid` telt daarna ≥ 1 buiten de pariteitszone; die telling
  wordt in de open-lijst van ch. 4.6 bijgewerkt.
- Alle dertien gates groen.

May touch: `corpus/golden/**`, `backlog/`.

## Out of scope

- De kruisingsarm zelf en zijn negatief — die horen bij een opvolgkaart, zodra
  de getuige bestaat. Deze kaart levert alleen het corpusmateriaal.
- `VerreRollidmaatschap`'s eigen vuller en backref: die vorm draagt **geen
  enkele** zone, ook de pariteitszone niet, dus daar helpt een golden pas als
  de frontend hem überhaupt kan emitteren. Eigen vraag, eigen kaart.
