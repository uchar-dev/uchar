# drools-heeltijdvak-leegtest: Drools speelt waar ref en clingo weigeren

Gemeten door de leeg-negatie-lens (2026-09-23, eigen proef-IR's): een
ONgenegeerde leeg/gevuld-test onder een heel-tijdvak-kwalificatie
speelt in beide Drools-takken, terwijl ref er een fout geeft
("whole-period leeg/gevuld test is ch. 3.9 scope, not executed here")
en clingo ook weigert. Drools geeft dus een antwoord op een vorm
waarvan de norm (de ch.-3.9-drempel voor een leeg-test) niet is
vastgelegd — de divergentieklasse: niet stil fout tegenover een
uitvoerder die speelt, maar wel een antwoord waar de andere twee
uitvoerders benoemd weigeren. Geen corpusdrager. De GEnegeerde variant
weigert de loader sinds de drools-leeg-negatie-kaart benoemd; de
ongenegeerde bleef buiten die kaart.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer met een proef-IR; meet alle vier de uitvoerders.
- (b) Beide Drools-takken volgen ref: benoemde weigering van de
  heel-tijdvak-leeg/gevuld-test (zelfde plek als de negatieweigering,
  IrLoader.predicaat, ligt voor de hand) — óf de norm wordt eerst
  vastgelegd en alle uitvoerders dragen haar; weigeren is de kleinste
  stap. Rood-eerst getuige beide takken.
- (c) Corpussweep: geen bestaand document beweegt; alle veertien gates
  groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
