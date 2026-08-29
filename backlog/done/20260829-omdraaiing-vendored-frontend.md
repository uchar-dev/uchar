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

Result: geland in de toolchain als main dcd2167 (vier commits, ff). De slice
frontends/regelspraak@39e303e is byte-identiek gevendord onder
vendor/uchar-frontend/ (46 bestanden, driftgate-geverifieerd); de eigen
frontend-kopie (35 bestanden: grammar, resolver, ir, model, emit, dubbele
kern-klassen) is verwijderd; de nieuwe cli.RegelSpraakCli levert
--drl/--facts/--graaf/--graaf-mmd rond de gevendorde resolver, make ir
blijft op de gevendorde Resolver --ir; scenariotoetsen bleven
toolchain-local (split package). SPEC_REV = 39e303e (enige pin);
uchar-conformance is nu de driftgate: byte-identiteit gevendorde kopie ≡
gepinde bron + bestandsset exact + de 20 emitter-goldens. Suites: jar-only
549/550 (1 assumeTrue-env-skip), drools-it 959/0/0, engine 115, scenario
30/30, conformance PASS. Lenzen (2 opus, adversarieel): geen
CRITICAL/MAJOR; regressielens bewees byte-identieke IR/DRL/facts/graaf
branch↔main incl. exit-codes, en beide driftrichtingen boobytrapped.
Verwerkte MINORs: doc-sweep (Resolver→RegelSpraakCli; grammar-paden),
grammar-section-command herschreven (grammar is vendored/read-only), de
package-private koppeling ScenarioResolver→RuleChecker/KenmerkMatcher
gedocumenteerd (vendor-README + NB-comment). Eerlijk gemeld: (1)
FactModelCliTest.java (testbestand) is door de bouwagent hergericht op de
wrapper — noodzakelijk voor compilatie na de CLI-verhuizing, maar buiten de
nooit-testbestanden-zonder-vraag-regel om; de resterende verouderde
javadoc-@link in datzelfde bestand staat nog open in afwachting van
akkoord. (2) De toolchain-eigen frontend-tests (TOKA-gekleurd) leven
voort naast de slapende gevendorde tests — bewust, gedocumenteerd in de
vendor-README; geen gate houdt ze synchroon met uchar's variant. (3) De
toolchain heeft geen CI; de driftgate is een make-doel (pre-existent).
