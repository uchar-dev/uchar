# zero-division-channel: delen door een numerieke nul geeft leeg plus een §9.5-feit

*Uitvoering van een besluit dat al in de spec staat (RUNTIME-SEMANTICS ch. 1.4 punt 4: "Decided: v1 routes
numeric-zero division into the same inconsistency channel"). Gemeten bij kaart `channel-fact-codes` (2026-10-19),
die de verhuizing bewust niet deed. De eigenaar besloot 2026-10-19: een eigen kaart, later uit te voeren; tot dan
blijft delen door nul een fout met code EF-005.*

Goal: een deling van een gevuld deeltal door een numerieke nul geeft leeg plus een §9.5-feit met code EF-005, in
plaats van een harde fout; zo gedraagt delen door leeg en de wortel uit een negatief getal zich al. De code blijft;
het kanaal van EF-005 in de tabel van ch. 5.7b wordt §9.5, en de getuigen gaan van `corpus/red` naar golden
`inconsistent`-verwachtingen. De v0-fout en de weigering van een letterlijke `1/0` bij het ontleden blijven het
spiegelbeeld van de productieruntime.

Wat het raakt (gemeten op main 0ba90bff):
- ref: `Arith.div_gen`; `ref/test/test_ref.ml` 6 regels (onder meer `expect_fault "ch2.2-div-zero"`, de Let-rijen
  i/ii van ch. 10.6).
- clingo: de `viol/2`-wachters van de deling in `translate.ml`; `backends/clingo/test/test_clingo.ml` 29 regels.
- Drools: `ComputeHelpers.divided`/`dividedAbs` en de klassificatie in `CaseRunner.faultCode`; 23 testbestanden,
  72 regels (onder meer `ZeroDivisorWitnessTest`, `ZeroDivisorConditionWitnessTest`,
  `ZeroDivisorConsumerWitnessTest`, `TmsFaultWitnessTest`).
- RUNTIME-SEMANTICS: de uitgewerkte voorbeelden die de fout pinnen (ch. 3.4c besluit G1, ch. 5.10, ch. 5.11
  `begrenswaarde`, ch. 10.6 rij ii) en ch. 2.2, ch. 1.4 punt 4, ch. 5.7b.
- Corpus: 8 rode getuigen van EF-005. Zes kan clingo als golden spelen; twee niet:
  `clamp-reads-bounds-in-order` (clingo weigert de Clamp met een niet-grondig minimum) en
  `sum-fallback-always-evaluated` (clingo vertaalt een som over losse operanden niet). Gate 6 eist dat clingo elke
  golden speelt.

Eerst te beslissen door de eigenaar: de twee getuigen die clingo niet draagt (schrappen met een verlaagde vloer, of
eerst een clingo-kaart voor die vormen); en de testverwachtingen in de drie suites en de uitgewerkte voorbeelden
mogen veranderen (gedragswijziging). Gevolg voor de distributie: haar gebruikers krijgen bij delen door nul een leeg
resultaat met een melding in plaats van een fout; de pin-bump noemt dat.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.
