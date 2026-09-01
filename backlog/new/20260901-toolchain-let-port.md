# Toolchain draagt L echt (route R1: de Let-port in regelspraak-java)

Goal: Sinds de let-lowering-kaart (2026-09-01) draagt de wire het
variabelendeel als call-by-need-`Let` (semantiek L), maar de toolchain
(`regelspraak-java`) blijft op de v0-kant via haar lokale, post-resolve
`LetInliner` (route R3, gemeten groen: 549/549 + 959/959 + scorebord
30/31 zonder testwijzigingen) — toolchain-productie en de
uchar-executors divergeren daardoor op precies één klasse: een GELEZEN
binder wiens value leeg is (v0 coalesceert, v1 barrièrt; tracked delta
in CONSTRUCTS rij 24). Deze kaart convergeert: de toolchain-backends
voeren de bewaarde Let zélf met L-semantiek uit en de `LetInliner`
verdwijnt.

Meetgegevens uit de verkenning (2026-09-01, read-only): de
toolchain-`ExecModelTranspiler` is 97,7% regel-verwant aan uchar's
`backends/drools`-port — het Let-cluster daar (~180 kernregels:
binderSleutel/letWaarde/letConditie/letKinderen/letKinderenZeker/
conditieOperanden) is goedkoop tilwerk, en wordt SIMPELER omdat de
toolchain na de slice-groei echte `Expr.Let`/`BinderRef`-records heeft
(geen Onopgelost-drager + identity-zijkanaal nodig). De blokkade is de
DRL-tekst-backend: een luie Let is in een `when`-patrooncconstraint
principieel niet uitdrukbaar (expressie, geen statement), en het
differentieel (`manifestDiff` + `runDifferential` die bij lege
overgeslagen-set beide vuurt) maakt "exec lowert, DRL markert" een
harde DIVERGE — DRL moet meeleren (waardekant: MVEL-statementblok kán
een lokale binding; de duplicatie idempotentiewacht/setter verdwijnt er
zelfs mee; voorwaardekant blijft substitutie, zoals uchar's grens (a))
óf beide moeten per vorm weigeren. Drie testbestanden pinnen de
inlining-vorm en vergen maintainertoestemming bij herziening:
`ExecModelDifferentialTest.PHASE_11`/`PHASE_11_ABS` en
`DrlTest.variabele_met_lidwoord_en_shadowing_van_parameter`.

Acceptance:
- Rood-eerst: de leeg-barrière-klasse (het vondstrecht-patroon) door de
  toolchain-pipeline getoond als v0-coalesce vóór de port, L-barrière
  ná; de uchar-goldens met Lets replayen door de toolchain-backends met
  de uchar-pins.
- `make test`, `mvn -B test`, het TOKA-scorebord en
  `make uchar-conformance` groen; de `LetInliner` en zijn
  post-normalisatie-assertie verwijderd.
- Testherzieningen (PHASE_11/ABS, DrlTest-shadowing) expliciet
  voorgelegd vóór de wijziging.

May touch (regelspraak-java): src/main/**, src/test/** en src/it/**
(na toestemming), vendor/ (re-vendor), SPEC_REV. In uchar: backlog/
(deze kaart), en het opruimkaartje voor het dan overbodige
Onopgelost-drager-zijkanaal in backends/drools mag hier genoemd maar
niet uitgevoerd.

Out of scope: nieuwe wire-vormen; de §11.1-kortsluiting
(20260901-samengesteld-kortsluiting); uchar-executor-wijzigingen.
