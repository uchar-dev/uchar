# drools-bereikafleiding-deelt: één afleiding, één weigeringszin

Rest van de som-over-bronrol-kaart (2026-09-19), door de lens gewogen:

1. **`gebondenRolpaar` staat in twee exemplaren** — regel voor regel
   identiek in `DrlTranspiler` en `ExecModelTranspiler`. De twee takken
   houden elkaars bereikafleiding nu alleen via de dual-branchgetuigen
   bij elkaar. Eén gedeelde afleiding hoort in `FactModelGenerator`
   (het patroon dat de weigeringszinnen al volgen via
   `geenJoinveldReden`).
2. **De weigering bij twee meervoudige rollen blijft generiek** in de
   DRL-tak (`TODO aggregatie SOM`) tegenover exec's benoemde arm
   (`aggregatie buiten Phase-C scope`) — per construct, maar niet in
   dezelfde woorden; het delen van de afleiding is de plek om ook de
   zin te delen.
3. **De temporele set-vouwen** (`tijdreeksSomAlleSet`,
   `temporeelSetAccumulate`) lezen de wire-binding nog niet; geen
   corpusgolden combineert ze met een herhaald paar en de vorm valt
   door naar een marker, niet naar een verkeerde verzameling —
   meenemen in dezelfde consolidatie.

## Acceptatie (bij oppakken aanscherpen)

- (a) De afleiding verhuist naar één plek; beide transpilers lezen
  haar; de dual-branchgetuigen blijven groen en de mutatieproef op de
  gedeelde plek raakt beide takken (het StaandeDoelen-patroon).
- (b) De meervoudige-rollenweigering draagt in beide takken dezelfde
  zin; getuige erbij.
- (c) De temporele set-vouwen lezen de binding of weigeren benoemd;
  meting eerst.
- (d) Corpussweep beide takken: nul kanaal- en redenwijzigingen
  (behalve waar (b)/(c) een diepere reden geven — bij naam verantwoord);
  alle veertien gates groen.

May touch: `backends/drools/**` (transpilers, `FactModelGenerator`,
tests), kaartverplaatsing. Geen scripts, corpus of frontend.
