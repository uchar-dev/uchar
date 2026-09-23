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
