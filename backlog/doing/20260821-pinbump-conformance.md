# Pin-bump: de toolchain conformeert aan de huidige spec

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 4): toolchain
vrijgegeven voor deze kaart; gaat vóór het grote vervolg.

Goal: regelspraak-java consumeert deze repo weer op de actuele pin —
SPEC_REV bump naar uchar-main HEAD, de conformance-/gate-suite van de
toolchain volledig groen (mvn -B test plus de eigen gates), en elke
afwijking die de bump blootlegt spec-first teruggemeld (delta hier,
dan pas de toolchain-fix).

Acceptance: SPEC_REV staat op de gepinde nieuwe rev; volle suite
groen in de toolchain; een Result dat elke gevonden afwijking en zijn
route (spec-delta of toolchain-fix) benoemt; uchar-gates onaangeroerd
groen.

May touch: regelspraak-java (vrijgegeven voor deze kaart);
`backlog/` hier. Uchar-spec/corpus alleen via een expliciete
spec-first-delta als de bump er een eist.

Out of scope: nieuwe constructen; de bronverankering (eigen kaart).
