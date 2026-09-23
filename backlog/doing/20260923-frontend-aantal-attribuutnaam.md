# frontend-aantal-attribuutnaam: de foutzin mist het "aantal …"-attribuut

Gemeten door de aantal-zonder-bron-lens (2026-09-23, advies 5): "het
aantal gewogen posten van de Jutter" met een gedeclareerd ATTRIBUUT
"aantal gewogen posten" krijgt sinds de `tellingBronFout`-ERROR de
zin "telling zonder bron" — de terugval op een gedeclareerde naam in
`ExpressieBouwer.bouwFunctie` vangt alleen parameters, geen attribuut
dat "aantal …" heet en met lidwoord wordt teruggelezen. Vóór de kaart
emitteerde de frontend hier stil een wrattelling die WF-034 al
weigerde, dus de fout is nu luider en geen regressie — maar de zin
noemt de echte oorzaak niet, en zulke attribuutnamen zijn in
RegelSpraak-modellen gangbaar.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer het geval; meet of de attribuutterugval veilig kan
  (wanneer is "het aantal X van Y" een attribuutlezing en wanneer een
  telling? — de brontaal beslist; sectieverwijzingen, niet citeren).
- (b) Óf de terugval leert het attribuut lezen (rood-eerst getuige,
  emissie als AttribuutRef), óf de foutzin benoemt de botsing ("de
  naam is een gedeclareerd attribuut; een telling vergt een
  instantiebron") — geen stille of misleidende fout.
- (c) Alle veertien gates groen; herpin bij frontend-beweging.

May touch: `frontends/regelspraak/**`, kaartverplaatsing.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-aantal-attribuutnaam`, basis `main` `582e8f7e`.

1. Reproductie met `make ir` op "het aantal gewogen posten van de
   Jutter" (attribuut `aantal gewogen posten` gedeclareerd).
2. Normmeting: de brontaal (spec v2.3.0, sectieverwijzingen) én de
   toolchain (dezelfde zinnen door zijn resolver; het vendored model in
   `toka/` op attribuutlezingen van een "aantal …"-naam). Het
   gedeclareerde attribuut mag onder "het aantal" alleen winnen waar de
   toolchain dat ook doet, niets breder; anders benoemt de foutzin de
   botsing en de spelling die het attribuut wél leest.
3. Rood-eerst getuige per spelling, een tegenproef dat de aangeraden
   spelling het attribuut schoon leest, een tegenproef zonder
   "aantal …"-attribuut; mutatieproef per voorwaarde.
4. Gates 10 en 11 byte-identiek (verwacht: geen corpusdrager); alle
   veertien gates groen, elk als losse aanroep; pins 181/160 bewegen niet.
