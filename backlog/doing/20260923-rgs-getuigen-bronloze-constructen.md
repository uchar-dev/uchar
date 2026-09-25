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

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Tellingen vandaag: 73 goldens met `bron.rgs` (was 70); twaalf goldens met
  een niet-lege `rules.objectCreations` (de elf van de kaart plus
  `jutterswet-opvolgerquorum`); geen van de twaalf draagt `bron.rgs`; de
  ankermap draagt geen enkele `bron.rgs`.
- Wire-namen: `objectCreaties` → `rules.objectCreations[]` (`ObjectCreation
  {name, versions[{createdType, subject, …}]}`); de macht is een `Binary`
  met de L5-operatorspelling (lees `spec/verifier/lib/spelling.ml`, familie
  `binary_op`); de afronding is `Rounding {mode, digits}` met de L5-modi
  (familie `rounding_mode`); de verdeling op volgorde is een `Allocation`
  (IR-SPEC ch. 3.2, lees de veldnamen daar); een `Beslistabel` heeft geen
  eigen knoop: de frontend desugart elke rij tot een gewone `Rule`
  (`BeslistabelDesugar`), dus de getuige is een golden waarvan `bron.rgs` een
  beslistabel schrijft en `ir.json` de gedesugarde regels draagt.
- Elke door de frontend geëmitteerde knoop draagt `source {snippet, line?}`;
  een handgeschreven golden draagt dat niet of anders — daarom zal de
  emissie zelden byte-gelijk zijn aan een handgeschreven `ir.json`. De route
  is de tweeling (U1), niet het herschrijven van de handgeschreven IR.
- Gate 10: `make ir FILE=…` in `frontends/regelspraak` emitteert; de vloer
  `EXPECTED_MIN_GOLDENS` in `scripts/check-regelspraak.sh` lees je in het
  script (de kaart noemt 38).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Route: tweelingen.** Per construct een golden
  `corpus/golden/<drager>-rgs/` met `bron.rgs`, de frontend-emissie als
  `ir.json` (byte-identiek onder gate 10, round-trip onder gate 2), de cases
  van de handgeschreven drager byte-gelijk gekopieerd (zelfde absolute
  verwachtingen) en een `NOTE.md` met de structurele diff tegen de
  handgeschreven `ir.json` (elke `source` gestript, sleutels gesorteerd) en
  per verschil de triage: (i) dialect- of emissieverschil, (ii)
  IR-ontwerpfout, (iii) frontendfout — een (ii) of (iii) stopt dat construct
  en gaat als genummerd open punt naar de hoofdsessie (geen frontend- of
  spec-wijziging in deze kaart). Handgeschreven goldens en ankers blijven
  byte-ongewijzigd; geen `bron.rgs` in `anchor/`.
- U2 **Dragers kiezen door te meten:** een sweep over `corpus/golden/**/ir.json`
  op de macht-operator, de afrondingsmodi anders dan naar beneden, een
  `Allocation` met volgorde-criterium en de objectcreaties; per construct de
  kleinste handgeschreven niet-anker-drager (objectcreatie bijv.
  `jutterswet-wrakboeking` of `synth3-kamerheffing`). Bestaat er voor een vorm
  geen handgeschreven drager, dan een nieuwe Jutterswet-golden (handgerekend,
  via `scripts/admit.sh`: ref ≡ clingo, no-op weerlegd).
- U3 **Drools.** Een tweeling met objectcreatie wordt door de exec-tak
  geweigerd (de bekende marker): een `EXPECTED_CHANNEL`-regel met reden en
  een `DRL_CHANNEL`-regel (**voorstel**, `scripts/check-drools.sh`); de
  spelende tweelingen verhogen `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED`
  (**voorstel**, met de gemeten getallen in het Result). Gate 7: staat de
  drager op de overleverslijst, dan de tweeling met dezelfde reden
  (**voorstel**, `scripts/check-noop.sh`).
- U4 Het Result noemt per construct de rgs-getuige (acceptatie (d)) en per
  tweeling de diff-triage.
- U5 **Gates** voor deze bouwer: 1, 2, 3, 4, 5, 6, 7, 10, 12 (13 als een
  tweeling een type-annotatie op een afgeleide positie draagt); de unie
  draait alle veertien.
