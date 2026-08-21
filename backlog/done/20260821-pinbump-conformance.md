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

Result (2026-08-21): de kaart VESTIGT de pin — er bestond aan de
toolchain-kant nog geen SPEC_REV of conformance-mechanisme; het
PLAN-regime beschreef het, deze kaart materialiseert het (eerlijk
verschil met de kaarttekst, die van een bump sprak). Geland in
regelspraak-java (87391fb + lens-hardening 4c037f5, gepusht):
SPEC_REV op de repo-root (eerste pin f39d4a2, volle 40-hex SHA
verplicht — een branchnaam zou een bewegend doel pinnen),
scripts/uchar-conformance.sh en make uchar-conformance. De check:
elke uchar emitter-golden (bron.rgs naast ir.json, recursief
gevonden) moet byte-identiek gereproduceerd worden — byte-identiteit
impliceert schema-validiteit en verifier-cleanliness omdat uchar's
gates 2/3 die op de ingecheckte bestanden pinnen op de gegatede rev;
het script hérdraait die gates niet, het vertrouwt ze. Rood-eerst
getoond: ontbrekende pin, foute pin, niet-SHA-pin, dirty tree en
lege golden-set falen alle luid. Uitkomst: 11/11 goldens
byte-identiek op de pin; mvn -B test volledig groen (952 tests, de
losse script-edit erna raakt de suite aantoonbaar niet).
Pin-conventie, expliciet vastgelegd (de regime-lens vroeg een
besluit; de gekozen vorm is dwingend afleidbaar): de pin staat op de
inhoudsstabiele uchar-rev bij schrijftijd (f39d4a2 — één
backlog-only commit vóór de merge van deze kaart, dus zonder
inhoudsdrift); de check eist ancestor-schap plus ongewijzigde
normatieve paden (corpus/golden, spec/schema, IR-SPEC,
RUNTIME-SEMANTICS) tussen pin en HEAD — inhoudelijke drift dwingt
de spec-first-herpin, backlog-proza niet. Strikte HEAD==PIN zou op
elk backlog-commit falen en een na-merge-herpin is een oneindige
regressie; dit is de enige houdbare vorm. Recorded, bewust gelaten:
een multi-source emitter-golden (de --ir-keten kan meerdere bronnen
aan) heeft nog geen bron*.rgs-conventie — spec-first te regelen
zodra zo'n golden landt; de dekking is 11 emitter-goldens (anchors
en synths zonder bron.rgs vallen per constructie buiten
emitter-conformance).
