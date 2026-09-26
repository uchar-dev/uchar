# graaf-samenvoeging-en-gate: één regelgraaf in uchar, met de subtypesleutel, en een gate die haar vasthoudt

De regelgraaf (`backends/drools/src/main/java/…/graaf/RegelGraaf.java`,
233 regels; `ProducentConsument.java`, 122) is een lezing van de IR: welke
regel produceert en consumeert welk (objecttype, attribuut of kenmerk),
door de subtypeketen heen. IR-SPEC ch. 5 definieert de
afhankelijkheidsgraaf en haar drie randsoorten voor de stratificatie; de
verifier rekent die uit. De maintainer besloot op 2026-09-26 dat de graaf
in uchar blijft ("eigenaarschap volgt de IR-semantiek, niet de gatedekking
van vandaag"). Vandaag zijn er twee lijnen: uchars versie draagt het
navigatiepredicaat uit L2 (`genavigeerd`), de toolchainversie
(uchar-dev/regelspraak-toolchain, commit c333bea5 "Een element sleutelt op
zijn declarant, niet op wie het aanraakt": `elementVan`/`declarant`, de
⊑-sleutel; `ScopeKetenTest` in de dienst, 232 regels) sleutelt een element
op zijn declarant. Gemeten: 91 diffregels in `ProducentConsument`, 10 in
`RegelGraaf`; in uchar geen test en geen gate die de graaf raakt (alleen
een javadocregel in `CaseRunner` r. 1022). De distributie roept
`RegelGraaf.bouw`, `ProducentConsument.elementVan` en `verbruikt` aan
(vier compilefouten tegen uchars huidige API — onderzoeksrapport §4.3).

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Eén graaf: de ⊑-sleutel uit de toolchain samengevoegd met uchars
  navigatiepredicaat; de API die de distributie aanroept (`bouw`,
  `elementVan`, `verbruikt` met predicaat) is de samengevoegde API; de
  bouwer legt de signaturen in het Result vast. De graaf leest de
  `RegelSpraakModel`-records zoals de lader ze uit de wire terugbouwt, dus
  ze werkt van `ir.json` uit (een consument zonder bronnen krijgt dezelfde
  graaf) — de bouwer bewijst dat met een test: graaf(bron-route) ≡
  graaf(wire-route) op elke golden met `bron.rgs`.
- U2 Tests: de `GraafTest` van de oude toolchain en het graafdeel van
  `ScopeKetenTest` geporteerd op Jutterswet-fixtures (de fixtures van de
  oude toolchain zijn ©-materiaal waar ze TOKA-domeinwoorden dragen; de
  bouwer schrijft ze over): de ⊑-keten (element op supertype, lezing op
  subtype en andersom), de genavigeerde lezing, de kenmerklezing, de
  vouwbron.
- U3 Gate (**voorstel**, `scripts/check-graaf.sh`, protected; de
  hoofdsessie landt haar): de graaf wordt over elke golden's `ir.json`
  gebouwd via de lader (nooit de bron) en vergeleken met wat de spec bezit:
  de verifier krijgt een uitvoermodus die de ch. 5-afhankelijkheidsranden
  van een document afdrukt (**voorstel**, `spec/verifier`), de graaf drukt
  dezelfde projectie af, en de gate eist gelijkheid per golden, met een
  redendragende lijst voor goldens waar de twee lezingen bewust verschillen
  en een floor op het aantal vergeleken goldens. Blijkt de projectie niet
  scherp te definiëren (randsoorten, granulariteit), dan pint de gate in
  plaats daarvan per golden de knoop- en randtellingen in een lijst in het
  script (het check-noop-patroon, stale-failing) — de bouwer meet, kiest
  en verantwoordt.
- U4 Geen spec-wijziging (de graaf is een lezing van ch. 5, geen nieuw
  normatief feit); `backends/drools/README.md` één alinea over de graaf en
  haar gate.
- U5 Klaar: alle gates groen inclusief de nieuwe; gate 12's pins
  ongewijzigd; naamtabel-toets rc 0; geen D-/U-nummers in scripts en
  code.

## Acceptatie

- (a) U1–U5; (b) de vier aanroepvormen van de distributie compileren tegen
  de samengevoegde API (bouwer toont het met een kleine proef buiten de
  repo of een test); (c) de gate faalt aantoonbaar op een mutant (één rand
  weggelaten).

May touch: `backends/drools/**`, `spec/verifier/**` (alleen de
uitvoermodus, **voorstel**), `scripts/check-graaf.sh` (**voorstel**). Niet in
deze kaart: de frontend, het corpus, `CLAUDE.md` (hoofdsessie bij de
landing).
