# Let-lowering spec-first: het variabelendeel op de wire (fase 2 van het Let-besluit)

Goal: De RegelSpraak-let ("Daarbij geldt:", officiële spec §11/§13.4.2 —
read-only referentie, §-verwijzingen mogen, excerpten nooit) wordt een
échte wire-vorm in plaats van weggewerkt te worden. Nu inlinet de eigen
frontend het variabelendeel (ExpressieBouwer substitueert de geresolveerde
subtree per gebruiksplek; het blok wordt nooit geserialiseerd; sharing
gaat verloren en `VariabeleRef` overleeft alleen als residu-marker). Na
deze kaart lowert de frontend het variabelendeel naar de `Let`-node —
CONSTRUCTS rij 24 ("Local variables") en rij 10 blijken twee namen voor
één construct en worden samengevoegd.

Maintainerbesluiten 2026-09-01 (geratificeerd): (1) de route is gefaseerd
— fase 1 (20260901-drools-let-node) leert Drools de node eerst; (2) de
evaluatiesemantiek is normatief OP GEBRUIKSPLEK: een gebonden expressie
wordt alleen geëvalueerd waar de naam werkelijk gelezen wordt
(§11.1-conform — geen onnodige berekening; het deling-door-nul-voorbeeld
uit de officiële spec blijft werken; gedragsbehoudend t.o.v. de huidige
inlining). ref en clingo worden hierop getoetst en zo nodig bijgesteld —
een strikte Let die het §11.1-voorbeeld zou laten faulten is fout.

Dit is SPEC-FIRST werk: (a) RUNTIME-SEMANTICS krijgt de normatieve
gebruiksplek-semantiek met een gewerkt voorbeeld (de deling-door-nul-
casus, eigen formulering); (b) IR-SPEC/CONSTRUCTS: rij 24+10 samengevoegd,
de v0→v1-delta gedocumenteerd (v0-wire kent geen variabelendeel — de
emitter-wijziging is de delta); (c) de emitter in frontends/regelspraak
lowert het variabelendeel naar Let (raakt de vendored slice → re-vendor +
herpin SPEC_REV + push door de hoofdsessie, plus de toolchain-conformance
en gate 10-byte-identiteit: bestaande rgs-goldens ZONDER variabelendeel
blijven byte-identiek; nieuwe corpus-goldens MET variabelendeel worden de
getuigen); (d) alle executors dragen de vorm (Drools via fase 1); (e)
minstens één golden met een gedeeld tussenresultaat pint dat sharing
terug is en één pint het gebruiksplek-gedrag (voorwaarde-eerst).

Acceptance:
- Rood-eerst per werkstuk; elke verwachting eerst door ref (na de
  ref-bijstelling: ref is dan de belichaming van het besluit).
- Alle twaalf gates groen; gate 10 byte-identiek op de bestaande
  rgs-goldens; toolchain-conformance na de herpin (make uchar-conformance
  op de nieuwe pin).
- Beschermde paden (spec/, scripts/) via het volle regime: twee lenzen +
  fixronde vóór de merge; échte ontwerpafwijkingen als genummerde vraag.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md
(HOOFDSESSIE serieel — nooit een agent), spec/schema/** (indien de
Let-wire-vorm velden mist), frontends/regelspraak/** (de emitter),
ref/**, backends/clingo/**, backends/drools/** (bijstelling op het
besluit), corpus/golden/** (NIEUWE getuigen; bestaande pins onaantastbaar),
corpus/negatief/** (nieuwe WF-gevallen), scripts/, backlog/. Toolchain:
re-vendor + herpin door de hoofdsessie.

Out of scope: de overige v1-vormen; de keuzetype-familie (eigen kaart);
elke wijziging aan bestaande golden-pins of toka/.
