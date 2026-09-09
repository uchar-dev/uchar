# Opruiming: het Onopgelost-drager-zijkanaal in backends/drools

Goal: uchar's Drools-port (`backends/drools`) draagt de Let nog via de
Onopgelost-drager plus het identity-zijkanaal (de `LetVorm`-registry
gesleuteld op object-identiteit) — een constructie uit de tijd dat de
geconsumeerde IR geen echte Let/BinderRef-records had. Sinds de
Let-port in regelspraak-java (20260901-toolchain-let-port, geland
2026-09-09) bestaat het bewijs dat dezelfde transpiler-structuur met
echte `Expr.Let`/`BinderRef`-records eenvoudiger is (geen zijkanaal,
geen identity-sleutels). Deze kaart ruimt het zijkanaal in de
uchar-port op door de loader echte Let-knopen te laten bouwen en het
Let-cluster daarop te laten werken.

Acceptance:
- De identity-gesleutelde registry en de Onopgelost-drager voor Lets
  zijn verwijderd; de loader bouwt de Let-vorm direct.
- Gate 12 (played-set, kanaalvloer, JUnit incl. de
  CompanionPadWitnessTest-flanken) en gate 13 byte-gelijk aan de
  huidige stand: 0 gedragsverschillen, geen pin beweegt.
- Alle dertien gates groen.

May touch: backends/drools/**, backlog/.

Out of scope: gedragswijzigingen (dit is een pure
constructie-opruiming); spec-tekst; de andere executors;
regelspraak-java.
