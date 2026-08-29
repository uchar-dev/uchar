# Omdraaiing: de toolchain consumeert frontends/regelspraak (vendored subtree)

Goal: de afhankelijkheids-omdraaiing van LADDER-EVOLUTIE §6 / Q5 (geratificeerd
2026-08-29), uitgevoerd terwijl de frontend stil ligt (freeze-invert-evolve).
De toolchain (regelspraak-java) vendort de TOKA-schone slice
`frontends/regelspraak` van uchar in zijn eigen boom op de SPEC_REV-pin,
compileert die als source root (naar het patroon van uchar's
`backends/drools/pom.xml`) en verwijdert zijn eigen
`{grammar,resolver,ir,model,emit}`-kopie. Een toolchain-wrapper levert
`--drl/--facts/--graaf` opnieuw rond de geconsumeerde `--ir`-resolver; de
scenariotoetsen-laag (ScenarioResolver/ResolvedScenario/ScenarioRunner +
tests) blijft toolchain-local, hergehuisvest op de geconsumeerde typen. De
NORMATIVE-set van `uchar-conformance.sh` groeit met
`frontends/regelspraak/**`; het script zelf wordt de nieuwe drift-check:
byte-identiteit van de gevendorde kopie tegen de gepinde bron, plus een
build-en-emit-smoke over de bestaande fixtures. SPEC_REV is en blijft de
enige pin.

Acceptance: de volledige toolchain-gate-set groen (make-doelen incl.
scenario-test, drools-it/engine, en de hernieuwde uchar-conformance);
`make ir FILE=... OUT=...` en de wrapper-modi werken; de gevendorde kopie is
byte-identiek aan `frontends/regelspraak/**` op de pin; geen tweede
frontend-bron meer in de toolchain (grammar/resolver/ir/model/emit weg); in
uchar blijven alle twaalf gates groen (er verandert daar niets behalve de
latere herpin). Herpin (volle 40-hex SHA) + push doet de hoofdsessie bij de
landing.

May touch: de toolchain-checkout (onder dit kaartmandaat; werk op een
branch, de hoofdsessie merget en pusht); in uchar alleen backlog/ (deze
kaart).

Out of scope: elke wijziging aan uchar's `frontends/regelspraak` zelf (de
frontend ligt stil tot de uitzondering-kaart); ladder-emissie; de
Drools-ladderservice; publicatie van de slice (Q6: uitgesteld tot de flip).
