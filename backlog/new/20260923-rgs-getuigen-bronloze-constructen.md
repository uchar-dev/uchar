# rgs-getuigen-bronloze-constructen: objectcreatie, macht, afronding, op volgorde van en beslistabel krijgen een RegelSpraak-bron

Vijf constructen die de frontend gewoon parseert (`objectCreatie`,
`TOT_DE_MACHT`, de afrondingsrichtingen naast NAAR_BENEDEN, verdeling
`OP_VOLGORDE_VAN`, `beslistabel`) hebben in het corpus geen enkele golden
met `bron.rgs`: van de 70 rgs-goldens bevat er geen een, en de elf goldens
met `objectCreaties` (vier Catala-goldens, `jutterswet-wrakboeking`,
`synth2-budgetgrens-onder`, `synth3-kamerheffing` en de vier anchor-goldens
`jutterswet-penninguitgifte`, `jutterswet-rekenkamer`,
`jutterswet-rekenkamer-overzee`, `jutterswet-wachtersketen`) zijn
handgeschreven IR of Catala-emissie. Gate 10 (byte-identieke her-emissie
door de frontend) bewijst voor deze vijf vormen dus niets; gemeten
2026-09-23 bij het schrijven van de website, die dit gat als
"zonder RegelSpraak-bron in het corpus" moest melden. Vermoedelijke
oorzaak: de goldens dateren van vóór de komst van de frontend in de
monorepo (2026-08-27) en zijn nooit van een bron voorzien.

## Acceptatie (bij oppakken aanscherpen)

- (a) Per construct ten minste één golden met `bron.rgs` die gate 10
  byte-identiek her-emitteert. Voorkeur: een bestaande handgeschreven
  golden een bron geven waar de frontend precies de ingecheckte `ir.json`
  emitteert; waar de emissie afwijkt van de handgeschreven IR, de
  afwijking benoemen in de `Result:`-noot en NIET de handgeschreven IR
  stil herschrijven — een nieuwe Jutterswet-golden (handgerekend, via
  `scripts/admit.sh`) is dan de route. Anchor-goldens zijn beschermd pad:
  een bron erbij is een voorstel.
- (b) Het vierkant blijft dicht: ref ≡ clingo over elke nieuwe of
  gewijzigde golden; wat Drools van de nieuwe vormen niet speelt, komt bij
  naam met reden in de gate-12-lijsten (de objectcreatie-marker op een
  onoplosbare terugverwijzing is bekend).
- (c) Alle veertien gates groen. `EXPECTED_MIN_GOLDENS` in
  `scripts/check-regelspraak.sh` (38, gemeten 2026-09-10) is een
  ondergrens en hoeft niet omhoog; een bump is een voorstel op een
  gelegenheid, conform het telvloerregime.
- (d) `Result:` noemt per construct de golden die nu als rgs-getuige
  dient, zodat de website-passage "zonder RegelSpraak-bron in het corpus"
  kan vervallen.

May touch: `corpus/golden/**` (nieuwe goldens; `anchor/` als voorstel),
`scripts/check-regelspraak.sh` (**voorstel**, alleen als een vloer bewust
omhoog gaat), kaartverplaatsing.

Out of scope: frontend- of spec-wijzigingen. Blijkt een handgeschreven IR
niet door de frontend te emitteren, dan is dat een eigen kaart (frontend
of registerrij), geen reden om de IR hier aan te passen.
