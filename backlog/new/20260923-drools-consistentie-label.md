# drools-consistentie-label: de runner keurt inconsistent zonder labelvergelijking

Gemeten door de leeg-negatie-lens (2026-09-23): bij een casus die
`inconsistent` verwacht met een REGELNAAM als sleutel vergelijkt
`CaseRunner.consistentie` in de Drools-backend het label niet — elke
gemelde inconsistentie telt als de verwachte. Een casus kan daardoor
groen lijken op de verkeerde inconsistentie (de lens betrapte het:
een onderscheidende consistentieproef speelde met én zonder de
leeg-negatie-fix). Verboden klasse: een verwachting die niet werkelijk
wordt getoetst. Meet bij oppakken hoe ref de sleutel vergelijkt en of
corpuscasussen met regelnaam-sleutels bestaan die vandaag op deze
zwakte leunen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: een getuige met twee consistentieregels waarvan de
  verkeerde vuurt, die vandaag ten onrechte groen speelt.
- (b) De runner vergelijkt het label zoals ref dat doet; een casus met
  een niet-passend label faalt benoemd.
- (c) Corpussweep beide takken: geen kanaal- of waardewissel; alle
  veertien gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
