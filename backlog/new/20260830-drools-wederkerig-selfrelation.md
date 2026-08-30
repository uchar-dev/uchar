# Drools: wederkerig self-relation (gate-12- en gate-11-dekking, familie 2)

Goal: `FactModelGenerator` leidt voor een wederkerig feittype (§13.3.9.2,
hetzelfde objecttype aan beide zijden) een referentieveld voor de
self-pair-rol af, zodat een regel die die rol leest een getter heeft;
`CaseRunner` zet die rol uit case-input en de weigering in
`CaseRunner.build` (`model.feittypen … wederkerig`) vervalt. Daarmee spelen
`anchor/jutterswet-maatschap` en `jutterswet-aanmelderkrediet` (gate 12 → +2
played) en komt gate 11's Drools-been tot leven: nu 0 PLAYED / 29 loud
EXCLUDE, waarvan 28 op precies deze vorm. Onderzoekshint: de
productietoolchain draait dezelfde casus wél — daar komen beanvelden uit de
gedeclareerde schema-velden (de inerte v0-mirror-bagage `zetVeld`/`joinVeld`),
terwijl uchar's `FactModelGenerator` ze symbolisch afleidt; de fix hoort in
de symbolische afleiding, nooit in het opnieuw lezen van beanveld-bagage.

Acceptance:
- Rood-eerst: de twee gate-12-EXCLUDE-regels en de gate-11-telling
  (0 PLAYED / 29 EXCLUDE) vóór de fix getoond; erna PLAYED.
- `bash scripts/check-drools.sh` → PASS, played +2 en auto-excluded −2 ten
  opzichte van de stand na de temporele-inputpoortkaart; geen
  verwachtingswaarde gewijzigd.
- `bash scripts/check-toka-parity.sh` → PASS; `toka/cases/parity.manifest`
  herpind: elke slice die na de fix tegen zijn gepinde verwachtingen speelt
  gaat van `EXCLUDE|fragment` naar `PLAYED` (verwacht ~28; 1 blijft EXCLUDE
  op objectcreatie); wat niet speelt blijft een luide EXCLUDE met fragment of
  wordt een genummerde maintainersvraag. Toolchain-gepinde
  `toka/**/ir.json`'s blijven byte-identiek.
- Alle twaalf gates groen. De kaart-Result noemt uitsluitend geaggregeerde
  tellingen en klassenamen — geen toka-scenario-ID's (het manifest en toka/
  zelf mogen ze wél dragen).

May touch:
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java`,
`.../schema/ContractValidator.java` (indien het contract het nieuwe veld
moet kennen), `.../cases/CaseRunner.java`, `backends/drools/src/test/**`,
`toka/cases/parity.manifest`, `scripts/check-drools.sh` (uitsluitend een
eventuele `EXCLUDE_KNOWN`-entry), `backlog/`.

Out of scope: nieuwe transpiler-lowering-vormen, de twee overige
relatie-excludes (het rolreferentieveld-geval en de
kenmerk-naamdeling-bean-limiet), objectcreatie, wijziging van
toka-ir.json's of van ref/clingo/verifier/spec/corpusverwachtingen.
