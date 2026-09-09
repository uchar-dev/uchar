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

Result (2026-09-09): geland in regelspraak-java, ff-merge
dfbf87f→56cb6d6 (zes commits). Beide back-ends (exec-model én
DRL-tekst) voeren de wire-Let zelf uit met de ch. 10.6-semantiek
(call-by-need, leeg-barrière, once-ness); de `LetInliner` en zijn
post-normalisatie-assertie zijn verwijderd. Rood-eerst gedemonstreerd:
het vondstrecht-patroon toonde v0-coalesce (30) vóór en de L-barrière
(leeg) ná de port. Nieuw replay-harnas (`UcharLetReplayTest`, draait
onder `-Duchar.dir`): de 3 rgs-dragende Let-goldens spelen met
11 verwachtingen ×2 back-ends; de overige 12 Let-goldens zijn zonder
IR-lezer structureel onbereikbaar (eerlijk gemeld, geen dekking
geclaimd). Vier adversariële lens-/fixrondes joegen de port naar
ref-getrouwheid: de §6.5/§6.6/§6.8-companions volgen nu het
evaluatiePAD via een statische spoorwandeling met compositionele
barrière-precondities (uchar's CompanionPadWitnessTest als arbiter,
alle vier flanken exact op de ref-pins; fabricage én weglating beide
gedicht, inclusief de datum-overload — een lege datum propageert per
Tabel 15 waar de duur coalesceert); de DRL-waardekant volgt de
leesvolgorde met op het spoor gehesen fault-sites (een fout links van
de barrière-lezing deponeert weer, zoals main); de spoorwandeling is
per knooptype tegen ref/lib/eval.ml gesweept (drie correcties:
Begrensd basis→maximum→minimum, Functie-leegwaarde vóór de argumenten,
TIJDSDUUR tot-vóór-van — empirisch gemeten op de ref-binary); en het
temporele doel met variabelendeel lowert weer (was door de port stil
marker geworden). Testherzieningen binnen het mandaat van 2026-09-06:
DrlTest-shadowing-vormasserties herschreven en strikter,
PHASE_11/PHASE_11_ABS gedrags- en absolute pins byte-identiek (alleen
kopcommentaar/naam). Eindstand: make test 549 geslaagd + 1
pre-existente omgevings-abort; mvn -B test 969/0/3 (met -Duchar.dir
969/0/2, replay groen); TOKA-scorebord 29/29; make uchar-conformance
PASS op de onaangeroerde pin a0f2970 — geen herpin nodig (deze kaart
raakt de normatieve set niet). Bewust gebleven en benoemd: de
companions zijn statisch met een expliciete restklasse die weigert
(deling/macht/tekstreeks/dimensiecel/aggregatie/navigated read als
binder-definitie — weigering kost alleen het companion áchter zo'n
binder, nooit een waarde); de ref-operandvolgorde bij
Begrensd/TIJDSDUUR hangt aan ongespecificeerd OCaml-gedrag — dat is
een uchar-spec-punt, als kaart aangemaakt
(20260909-operandvolgorde-normatief). Het door deze kaart genoemde
opruimkaartje voor het Onopgelost-drager-zijkanaal in uchar's
backends/drools is eveneens aangemaakt
(20260909-onopgelost-zijkanaal-opruiming).
