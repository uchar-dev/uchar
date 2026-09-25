# regelspraak-java-dialect-pinbump: de SPEC_REV-bump na de dialectkaarten

Besluit 8 van de onderzoekskaart dialect-defaults-niet-en-nesting
(maintainersbesluit 2026-09-23): het volgwerk in regelspraak-java krijgt
eigen kaarten, per regime opgesteld in `uchar/backlog`. Deze kaart is de
gewone spec-first pin-bump na de landing van dialect-niet-in-uitzondering
en dialect-uitzondering-op-uitzondering; de tekst is overgenomen van de
conceptkaart van het voorstel (commit `381dad80`, vervallen in `89e63594`)
en bijgesteld op de kaartnamen van de maintainer.

Doel: de gevendorde frontendslice (`vendor/uchar-frontend/`) brengt de
nieuwe grammatica en RuleChecker mee op de nieuwe `SPEC_REV`. De vuurroute
hoeft niets nieuws te leren: de exec-model-vertaling daar lowert `besluit`
en geneste `excepties` al (`ExecModelTranspiler.compileDefault`), en een
`false`-beslissing schrijft niets, zoals in uchar. De DRL-teksttak
behoudt haar luide weigering van defaults-documenten.

## Acceptatie (bij oppakken aanscherpen)

- (a) De conformantiecontrole (gevendorde kopie ≡ gepinde bron) groen op
  de nieuwe pin.
- (b) De drie tweelingen uit dialect-uitzondering-op-uitzondering
  (`ww-herleving-inkomen-rgs`, `jutterswet-uitsluiting-rgs`,
  `jutterswet-nestkenmerk-rgs`) vuren in de toolchain met de verwachtingen
  van hun corpuscases, via de exec-model-tak.
- (c) De bestaande regressie van regelspraak-java groen.

May touch (in regelspraak-java): `SPEC_REV`, `vendor/uchar-frontend/**`,
eigen tests; hier alleen kaartverplaatsing.

Niet in deze kaart: het scenario-harnas, de graaf, de uitleg en de dekking
(kaart regelspraak-java-defaults-in-scenario-en-werkbank).

## Vervallen (2026-09-25, hoofdsessie)

Deze kaart gaat op in de ene pin-bump na de v1-klap (maintainersbesluit 2026-09-24: geen tussentijdse herpins; één bump in de nieuwe regelspraak-java onder uchar zodra de zes spec/golden-kaarten van 2026-09-25 geland zijn). Acceptatie (b) — de drie tweelingen van dialect-uitzondering-op-uitzondering vuren in de toolchain — hoort bij die bump.
