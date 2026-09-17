# drl-heelperiode-arm: de DRL-tekst-tak lowert ook de heelperiodematrix

Aanleiding: de DRL-tekst-tak van `backends/drools` MARKEERT de Kwantorvouw
met een §8.2-heelperiodematrix bij naam, waar de exec-model-tak hem speelt
— tot nu toe het "staande kanaalbesluit" (zichtbaar op de goldens
`jutterswet-ploegpremie`, `-keurploeg`, `-reddingsvloot`,
`-sleepvloot`). Maintainersbesluit 2026-09-17: dit takverschil is
ongewenst. De twee takken hebben elk hun rol — de DRL-tekst-tak om snel
mee te ontwikkelen en te testen, de exec-model-tak om services mee te
compileren — en een vorm die maar in één tak speelt breekt die
werkverdeling. De asymmetrie-kaart bewees al dat de NIET-temporele
matrix in beide takken kan; de heelperiodevorm hoort te volgen.

Richting: de runtime-helpers bestaan (`rsGeheelPeriodeAlle`,
`rsGeheelPeriodeGeen`, `rsGeheelPeriodeTelling` — beide takken delen de
runtime al); het patroon collectList-accumulate + result-var-eval staat
als voorbeeld in de exec-tak én in de toolchain-DRL-tak
(regelspraak-java, de Kwantorvouw-naport). De vijf kwantoren en de
NIET-omklap/telling-landing zijn in de exec-tak al gepind — de DRL-arm
moet exact diezelfde tabellen reproduceren.

## Acceptatie (bij oppakken aanscherpen)

- (a) De DRL-tekst-tak lowert de heelperiodematrix onder alle vijf
  kwantoren, incl. filter en de NIET-prefix-semantiek (omklap bij
  ALLE/GEEN, telling-landing bij de drempelkwantoren), met dezelfde
  weigeringsgrenzen als de exec-tak — elke resterende weigering
  benoemd, nooit stil.
- (b) De bestaande heelperiode-goldens verhuizen in gate 12/13 van
  MARKER naar PLAYED in de DRL-tekst-tak; de kanaallijsten en pinnen
  bewegen mee met gedateerde stappen.
- (c) Rood-eerst: de huidige markerteksten gemeten vóór de bouw; daarna
  DRL ≡ exec ≡ ref ≡ clingo op alle betrokken cellen.
- (d) De "standing channel split"-formuleringen (rij 40, ch. 3.12,
  NOTE's, testjavadocs) bewegen mee — registervoorstel in het Result;
  RUNTIME-SEMANTICS-wijziging alleen met kaartmandaat.
- (e) Alle veertien gates groen.
