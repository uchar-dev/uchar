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

Meetpunten uit golf C (de frontend lowert het variabelendeel, 2026-09-01):
- **De union groeide met TWEE nodes, niet drie.** `Expr` draagt nu
  `Let(binder, value, body, type, bron)` en `BinderRef(naam, type, bron)`;
  `IfThenElse` NIET (die blijft emitter-vrij). Rimpel: alleen
  `IrEmitter.expr` viel om (de enige exhaustieve switch op `Expr` in de
  hele boom); `Expr.bevat` kreeg met de hand een `Let`-arm (value én body
  — elke aanroeper stelt een STRUCTURELE vraag). In `backends/drools`
  brak niets: elke walk daar is een instanceof-cascade met een
  fall-through. Dat is precies het gevaar (een onbekende node is er een
  opake LEAF, dus stille onderrapportage), dus
  `ExecModelTranspiler.geenUnieLet` faalt nu hard op een union-Let/
  BinderRef, aangeroepen vanuit de vier plekken die `letVormen`
  raadplegen (`scalarWaarde`, `letKinderen`, `letKinderenZeker`,
  `leestAltijd`). De loader blijft de wire-Let naar drager+zijkanaal
  parsen: één vorm voor het hele ch.-4.10-cluster in plaats van een
  half-union/half-kanaal (reden bijgewerkt op `LetVorm`/`IrLoader`).
- **Resolverkeuze 1 — voorwaarde-variabelen blijven substitutie.** De
  wire heeft twee velden (`voorwaarde`: Conditie, `waarde`: Expr) en geen
  Expr-node overspant ze. Een variabele die alleen de voorwaarde leest
  houdt dus de v0-route. Eerlijk gezegd: dat kost SHARING (een variabele
  die tweemaal in één voorwaarde gelezen wordt, wordt tweemaal
  uitgerekend), geen antwoord — een voorwaarde-lezing evalueert sowieso
  één keer, en de leeg-barrière op de leesplek valt daar samen met de
  §5-leeg-operanddiscipline die de vergelijking al toepast.
- **Resolverkeuze 2 — GEEN dode Lets, ook niet bij nul lezingen.** Het
  besluit-5-woord "élke variabele wordt een Let, ook bij nul lezingen"
  is één stap verder gegeneraliseerd: een Let wordt geëmitteerd voor
  elke variabele die het resultaat leest (direct of via de value van een
  latere Let — de keten wordt van binnen naar buiten gebouwd, dus dat is
  transitief gratis), en voor géén andere. Reden: de kaart geeft zelf de
  motivatie ("vervuilt de wire") voor de voorwaarde-only-variabele, en
  die geldt woordelijk net zo hard voor de nergens-gelezen variabele;
  één uniforme regel is eerlijker uit te leggen dan een uitzondering op
  een uitzondering. **Afwijking van de letterlijke besluittekst,
  expliciet gemeld.**
- **BEVINDING: toka-18 wordt GEEN derde holdout.** Gemeten: toka-18's
  `X` wordt UITSLUITEND in de voorwaarde gelezen en zijn resultaatdeel
  is een kenmerktoekenning (waarde = de synthetische `Literal "waar"`),
  dus onder keuze 1+2 verandert er niets aan zijn wire-vorm. De hele
  casusdump re-emitteert byte-identiek (45 fragmenten, gate 11 groen met
  de bestaande TWEE holdouts). Besluit 5's verwachting berustte op de
  "volle lowering"-lezing zonder de voorwaarde-carve-out. Geen andere
  toka-slice draagt een variabelendeel (18/23/45 zijn de enige drie).
- **Scope-grens, bewust:** de Let-keten omhult de `waarde`-slot van een
  ResolvedRegel (gelijkstelling/berekening/initialisatie + het
  startpunt-anker). Verdeling-bedragen, objectcreatie-toekenningen,
  feitcreatie en de consistentie-assertie houden de inline-route — een
  assertie is een Conditie (kan geen Expr-Let dragen) en de andere
  dragen meerdere onafhankelijke waardeslots die één keten niet
  overspant. Geen corpus- of pariteitsmateriaal raakt ze.
- **Getuigen:** `corpus/golden/jutterswet-lichterloon` (sharing: één Let,
  twee BinderRefs), `corpus/golden/jutterswet-vondstrecht` (de
  leeg-barrière — gemeten tegen de ingelijnde variant van dezelfde IR:
  30 vs. leeg), `corpus/golden/synth3-takbarriere` (probe B1,
  handgeschreven: ongelezen binder in de niet-genomen tak → doel
  geschreven).
- **Tellingen (alle twaalf groen):** 2 = 164 (was 161); 3 = 164/178;
  4 = 402 casebestanden; 5/6 = 380 (was 377); 7 = 344 weerleggend / 36
  survivors (ONGEWIJZIGD — alle drie nieuwe cases weerleggen de no-op);
  8 = 22 rood; 9 = 201/12; 10 = 152 tests (was 144) / 25 rgs-goldens
  (was 23, de 23 oude byte-identiek); 11 = 45 fragmenten / 2 holdouts /
  27 tests / 29 replays; 12 = played 101 (was 98 — alle drie de nieuwe
  goldens spelen) / 39 auto-excluded / 1 value-divergence / JUnit 125.

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

Result (aanzet) — lens 1 + fixronde (2026-09-01):
- **De DERDE lowering-grens, gevonden als échte regressie.** Pass 2's
  BinderRef is een opake blad, dus elke positie waar de resolver op de
  VORM van de deelboom beslist viel om: het rechterlid van een
  §5.6-subselectievergelijking, de aggregatie-setbepaling, de
  temporele-operandvorm en de bijbehorende instanceof-sites. Gemeten:
  een som met "die groter zijn dan de drempel" (drempel uit
  `Daarbij geldt:`) rekende op main 19 en degradeerde op de branch naar
  `Conditie.Onopgelost` → WF-044-fault. Fix: `bouwFunctie` bouwt een
  AGGREGATIE (de zeven instantie-aggregaties + de twee §7-temporele
  operatoren) in `ingelijnd()`-scope — daarbinnen substitueert `naamRef`
  weer, precies zoals v0. Pass 2 kijkt daarbij naar een groeiend PREFIX
  van de ingelijnde definities, zodat een voorwaartse verwijzing in
  beide passes identiek onopgelost blijft. Normatief vastgelegd als
  grens (c) in IR-SPEC ch. 4.10 + RS ch. 10.6 tracked delta 1; getuige
  `corpus/golden/jutterswet-drempelvangst` (beide kanten in één golden).
- **De sharing van `jutterswet-lichterloon` is niet waarde-observeerbaar.**
  Eén Let met twee BinderRefs rekent hetzelfde getal als twee
  gesubstitueerde deelbomen; gates 5/6/12 kunnen het verschil dus niet
  zien. De dekking van die getuige is daarom *volledig* gate 10
  (byte-identieke re-emissie van `bron.rgs`) plus de frontend-JUnit
  (`VariabeledeelLoweringTest`, die op de wire-vorm asserteert) — niet
  de differentiële vierkant. Dat is geen gat, maar het moet gezegd zijn:
  wie de emitter terugdraait naar inlining ziet de goldens groen blijven
  en alleen gate 10 en de JUnit rood worden.
- **Twee flanken zijn PRE-EXISTENT, niet door deze kaart geopend.**
  (a) De §6.5/§6.6-companion ontbreekt onder een drager — de
  Drools-voorwaarde-companion; opgeschreven in de golf-B-restpost
  hierboven en opgelost door `20260901-companion-onder-drager`.
  (b) De WF-076-shadowingweigering: de shadowing-grens rond een binder
  die een gelijknamige buitenbinder overschaduwt. Beide bestonden vóór
  de lowering en bewegen er niet door; ze zijn hier genoteerd zodat de
  fixronde ze niet als eigen schade opvoert.
- **Bewust ongemoeid:** `spec/verifier/**` (kaartrandvoorwaarde) draagt
  nog de strikte L-terminologie ("the strict let", "Strict: a leeg …" in
  `lib/ast.ml` en `lib/checks.ml`) — waar, maar verouderd sinds besluit
  O3. Maintainerwerk op een beschermd pad.
