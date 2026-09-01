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

Maintainerbesluiten (herzien en geratificeerd 2026-09-01, na de gemeten
verkenning — de eerdere formulering "gedragsbehoudend t.o.v. de huidige
inlining" bleek onhaalbaar: inlining is coalesce-zonder-barrière (N) en
N beweegt drie pins waaronder een anchor):
(1) de route is gefaseerd — fase 1 (20260901-drools-let-node, done)
leerde Drools de strikte node eerst;
(2) O3: de Let-semantiek UNIFICEERT naar L — lui met de leeg-barrière
op de LEESPLEK: een value wordt pas geëvalueerd (éénmalig, call-by-need)
bij de eerste lezing van de binder; een gelezen binder wiens value leeg
is maakt de hele Let leeg (de barrière verhuist van bind- naar
leesplek); een ongelezen binder evalueert niets. Gemeten pin-neutraal
(399/399 corpus-cases identiek aan strikt; WF-077 en de NAF-vrije
complement-eigenschap blijven — probe G). Het variabelendeel krijgt
daarmee de leeg-barrière: een bewuste v1-divergentie t.o.v. de
v0-inlining-coalesce, als tracked delta in CONSTRUCTS. Catala's
call-by-value-let wordt eveneens een tracked getrouwheidsdelta;
(3) het §11.1-deling-door-nul-voorbeeld wordt NIET door deze kaart
geleverd — dat is kortsluiting van Samengesteld ALLE (eigen kaart
20260901-samengesteld-kortsluiting); deze kaart levert §11.1's
"niet onnodig berekenen";
(4) ref-unittest WV-10.6-leeg-value-no-body wordt geherformuleerd tot
de L-getuige (toestemming maintainer): lege value + gelezen binder →
leeg; lege value + ongelezen binder → body-uitkomst;
(5) de emitter lowert VOL (elke bronvariabele → Let); toka-18 wordt de
derde reden-dragende gate-11-holdout (de toolchain-gepinde
toka/casus/ir.json draagt X ingelijnd en kan nooit een Let dragen).
De clingo-kant krijgt de per-tak-leesanalyse en een nieuwe
synth-getuige-golden sluit de false-green-window (de probe-B1-vorm:
lege value, binder alleen in de niet-gekozen tak gelezen — onder L
schrijft het doel, onder strikt niet).

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

Meetpunten uit fase 1 (Let-node-kaart, done 2026-09-01):
- SPANNING die deze kaart moet beslechten: de bestaande wire-Let is
  STRIKT (leeg-barrière, corpus-gepind in de Catala-goldens en nu in
  drie executors identiek), het geratificeerde besluit voor het
  variabelendeel is GEBRUIKSPLEK. Eén node met twee semantieken kan
  niet; de kaart kiest expliciet (bv. een tweede vorm/vlag, of de
  lowering bewaakt gedragsbehoud per gebruiksplek) — bestaande
  strikte-barrière-pins zijn onaantastbaar. Genummerde vraag als het
  besluit spec-tekst raakt.
- Gate-12-druk, gemeten (lens-1-injectiesweep fase 1): een Let om een
  aggregatie-/temporele waarde of in een conditie-/patroonpositie
  weigert in Drools luid ("waarde buiten scope"; letConditie draagt
  alleen de kale numerieke Vergelijking) — ~20 van de 98 spelende
  goldens vallen bij zo'n omhulling uit. Een emitter die
  variabelendelen breed naar Let lowert drukt die goldens uit de
  played-set; meet en rapporteer de verschuiving expliciet, of verbreed
  eerst de Drools-dekking.
- De §6.5/§6.6-companions kennen geen tak-/barrièreconditie
  (vervolgkaart 20260901-companion-onder-drager) — nieuwe goldens met
  delingen ín Let-bodies raken die grens.

Meetpunten uit golf B (Drools van strikt naar L, 2026-09-01):
- **De spiegelrestpost, herschreven en GROTER.** `letKinderenZeker`
  levert onder L een Binding-value alleen nog als de body de binder op
  ELK pad leest (`leestAltijd`, conservatief: bij twijfel niet-zeker).
  Drie vormen krijgen daardoor géén §6.5/§6.6-companion waar ref de
  fout wél stelt: (a) een fout in de tak die ch. 10.2 WEL neemt
  (ongewijzigd); (b) een fout in een Let-BODY — onder L draait de body
  altijd, dus dit is nu een misser op élke deling/wortel in een body,
  waar het onder de strikte binding alleen een misser was als de value
  gevuld was; (c) een fout in een value wiens binder op sommige maar
  niet alle paden gelezen wordt (of wiens lezing `leestAltijd` niet kan
  certificeren — een lezing van een ANDERE binder ervóór kan het pad via
  diens barrière afbreken). Nooit-verzinnen blijft de gekozen kant; de
  echte oplossing blijft `20260901-companion-onder-drager`.
- **BEVINDING (maintainer): een fase-1-witness pint nu een
  ref-divergentie.** `LetDragerWandelingWitnessTest.
  een_deling_achter_een_nooit_gepasseerde_leeg_barriere_levert_geen_fout`
  speelt `Let d = deler in (inkomen gedeeld door tweede)` met `deler` én
  `tweede` leeg. De binder wordt NIET gelezen, dus onder L draait de
  body wél: ref geeft daar nu **leeg + INCONSISTENT** (gemeten met de
  L-ref). De Drools-pin (leeg + CONSISTENT) blijft gróén — maar via
  restpost (b), niet meer via overeenstemming met ref. De assertie is
  dus niet omgevallen; de reden erboven is verouderd. Testbestand niet
  aangeraakt (kaartrandvoorwaarde); een herformulering is
  maintainerwerk, bij voorkeur samen met
  `20260901-companion-onder-drager`, die de vorm sowieso oplost.
