# gates: de Drools-jar één keer per run bouwen (gates 8, 11, 13 lezen versheid, gate 12 bouwt)

Goal: een volledige gate-run bouwt de Drools shaded jar nu vier keer
(gates 8 en 11: `mvn -o package` mét de 653 JUnit-tests; gate 12: idem,
by design de suite plus de jar; gate 13: `package -DskipTests`). Drie
van die vier builds zijn dubbel werk zonder extra zekerheid. Maintainer-
sein 2026-09-23 ("laten we efficient werken"). Na deze kaart bouwt
alleen gate 12 onvoorwaardelijk (zijn ontwerp: één `mvn package` draait
de suite én ververst de jar); gates 8, 11 en 13 eisen een VERSE jar en
bouwen alleen als hij ontbreekt of ouder is dan zijn bronnen — de
lens-bewezen stale-jar-flank ("a build-only-when-absent jar can silently
predate the source") blijft gesloten, want de toets is "ouder dan de
bron", niet "afwezig".

Acceptatie:
- (a) `scripts/lib/drools-jar.sh` met één functie `ensure_drools_jar`:
  eist `mvn`; eist precies één `backends/drools/target/uchar-drools-backend-*-shaded.jar`
  na afloop; bouwt met `mvn -o -q -f backends/drools/pom.xml package -DskipTests`
  als de jar ontbreekt of ouder is dan ENIG bestand onder
  `backends/drools/src/**`, `backends/drools/pom.xml` of
  `frontends/regelspraak/src/main/**` (de backend compileert de
  recordtypen van de frontend mee); drukt één regel af: `drools-jar:
  rebuilt (…)` of `drools-jar: fresh (jar newer than N source files)`;
  faalt luid met de mvn-staart als de build rood is. Na afloop een
  interne assertie: jar-mtime ≥ jongste bron-mtime, anders FAIL.
- (b) Gates 8, 11 en 13 roepen de functie aan in plaats van hun eigen
  `mvn package`; hun kopcommentaar en meldingen zeggen "fresh or rebuilt"
  in plaats van "unconditionally". Gate 12 ongewijzigd.
- (c) Zelftest in het script (rood-eerst getoond in het Result): een
  jar ouder maken dan een bron (`touch -t` op de jar) → `rebuilt`; direct
  daarna opnieuw → `fresh`; een bron aanraken → `rebuilt`.
- (d) Meting in het Result: wandkloktijd van een volledige run van gates
  8, 11, 12, 13 (na elkaar) vóór en na, op dezelfde boom.
- (e) Alle veertien gates groen; geen pin, vloer of lijst beweegt.

May touch: `scripts/lib/drools-jar.sh` (nieuw), `scripts/check-rood.sh`,
`scripts/check-toka-parity.sh`, `scripts/check-reftype-derivation.sh`,
kaartverplaatsing. NIET: `scripts/check-drools.sh`, `CLAUDE.md` (de
gate-beschrijvingen 8/11/13 zeggen "rebuilt unconditionally" — de
hoofdsessie werkt CLAUDE.md bij de landing bij), `.github/**`,
`spec/CONSTRUCTS.md`, `spec/PLAN.md`.

Out of scope: `dune build` (incrementeel, goedkoop); de JUnit-suite van
gate 12; CI-configuratie.
