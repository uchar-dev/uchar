# Drools: de scalaire ch.-7.8-standing-norm (waardestelling uit de divergentielijst)

Goal: De scalaire tegenhanger van de temporele standing-arm (kaart
20260830-drools-fixpunt-en-ch78): op een STAAND scalair doel beslist een
lege opbrengst niets en blijft de case-geleverde waarde staan. Besloten
spec — RUNTIME-SEMANTICS ch. 7.8 voert `anchor/jutterswet-waardestelling`
verbatim op als gewerkt voorbeeld (voorwaarde geldt, taxatiewaarde leeg →
prijs blijft 40, de doorval); ref implementeert het via
`Desugar.standing_targets` (~temporal:false) mét de ch.-7.7-`desugared_targets`
ernaast (ref raadpleegt in de scalaire tak beide — poort ze allebei) en de
`Value.Empty`-arm in `ref/lib/eval.ml` (rond r1348-1361); clingo draagt
het ook. `backends/drools` schrijft daar null over de voorgevulde waarde
heen — de gelijste `EXCLUDE_KNOWN`-entry noemt het nog "v0-divergence …
v1-delta candidate", maar het is klasse (d): implementatiegap tegen
besloten spec (bouwrondebevinding 2026-08-30, door lens 2 van de
fixpunt-kaart onderschreven). Onderzoek daarbij of
`anchor/jutterswet-waardering-v0vorm` (de leeg-bron-oscillatie tegen
INITIALISATIE) door dezelfde poort convergeert — zo ja, dan verlaten
BEIDE entries de lijst en resteert alleen kustwacht (de
negatie-stratificatie, een echte v0-divergentie).

Acceptance:
- Rood-eerst: de huidige FAIL-uitkomsten van waardestelling (en de
  geen-fixpunt van waardering-v0vorm) vóór de fix getoond.
- `bash scripts/check-drools.sh` → PASS; waardestelling verlaat
  `EXCLUDE_KNOWN` en speelt tegen zijn ongewijzigde pins (het
  stale-regime dwingt de verwijdering af); value-divergence 2 (of 1 als
  waardering-v0vorm meegaat — dan óók die entry weg, met de geratificeerde
  Q11-notitie bijgewerkt in de entrytekst van kustwacht’s buurcommentaar,
  nooit een verwachting bewogen).
- De poort volgt ref clausule-voor-clausule (standing_targets ~temporal:false
  én desugared_targets; zelfde schrijverstelling, zelfde other_writer-kanalen)
  met witness-tests voor beide kanten (leeg → doorval; gevuld → overschrijven)
  en voor de niet-staande flank.
- Alle twaalf gates groen; geen verwachtingswaarde, pin of WF-nummer
  gewijzigd; geen wijziging aan ref/clingo/verifier/spec.

May touch: `backends/drools/src/main/java/**` (ExecModelTranspiler,
runtime-helpers), `backends/drools/src/test/**` (nieuwe testbestanden),
`scripts/check-drools.sh` (uitsluitend het verwijderen/bijwerken van de
entries), `backlog/`. *(Uitzondering, hoofdsessiebesluit bij de landing
2026-08-30: één methode in het bestaande
`TemporeelFixpuntWitnessTest.java` — de bewaker die pinde dat de
waardering-v0vorm-oscillatie observeerbaar bleef — vervalt met zijn
object: deze kaart heft die toestand per besloten spec op, en dezelfde
dwang die de gate-12-entry verwijdert (het stale-regime) geldt voor de
witness die dezelfde toestand bewaakte. Aparte commit, door lens 1
getoetst en akkoord bevonden.)*

Out of scope: kustwacht (echte v0-divergentie, blijft), de
CaseRunner-taxonomie-herziening als zodanig, temporele vormen (gedaan),
objectcreatie, transpiler-scope-regels, ref/clingo/spec/corpus.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. De scalaire ch.-7.8-arm
staat: op een staand scalair doel beslist een lege (null-)opbrengst niets
en blijft de case-waarde staan; het domein is ref's
`standing_targets_gen ~temporal:false` clausule-voor-clausule geport
(`staandeScalaireDoelen`: is_computing, executable vacuüm waar buiten het
temporele pad, count==1 via de gedeelde schrijverstelling, wanted_shape
omgekeerd — géén granulariteit, other_writer), mét de
ch.-7.7-`desugared_targets` ernaast (`ontsuikerdeDoelen`: puur
regelsamenstelling, bewust zonder vorm- of other_writer-clausule —
ref-getrouw, de asymmetrie is gedocumenteerd, niet gladgestreken). Beide
lenzen legden ref er regel voor regel naast; alle acht witness-verwachtingen
zijn vóór het pinnen door ref gehaald, en de niet-staande flank
(twee schrijvers → de v0-wis geldt onverkort) is in beide executors
gemeten.

Rood-eerst: waardestelling FAIL "verwacht 40.00, werkelijk leeg"
(case3-leeg-valt-door); waardering-v0vorm 2× "geen fixpunt"; nulmeting
gate 12 = 84/50/3/1, JUnit 45. Eindstand: **86 played / 50 auto-excluded /
1 value-divergence / 1 no-case, JUnit 52** — beide anchors spelen tegen
hun ongewijzigde pins, en de enige overgebleven divergentie is kustwacht,
de échte v0-negatie-stratificatie-divergentie: de value-divergence-lijst
draagt weer uitsluitend wat zijn banner belooft. De
waardering-v0vorm-oscillatie bleek een gevolg van dezelfde gap (de
leeg-write en de INITIALISATIE wisselden elkaar af; met de write onderdrukt
beslist de init één keer — het doel zit via ch. 7.7 in het ontsuikerde
domein, niet in het staande: de INITIALISATIE telt als schrijver maar is
nooit domeinlid, exact als ref). De geratificeerde Q11-notitie is daarmee
door constructie afgedaan, niet door de CaseRunner-taxonomie-herziening
waarop zij wachtte — die herziening zelf blijft open.

May-touch-uitzondering (hoofdsessiebesluit, door lens 1 getoetst): één
methode in het bestaande `TemporeelFixpuntWitnessTest` — de bewaker die
pinde dat de oscillatie observeerbaar bleef — verviel met zijn object, in
een eigen commit; dezelfde stale-dwang die de gate-12-entry verwijderde.
Verder gewijzigd: `scripts/check-drools.sh` (beschermd pad; twee entries
weg, het (d')-blok legt eerlijk vast dat waardestelling altijd al klasse
(d) was — RUNTIME-SEMANTICS ch. 7.8 voert hem verbatim als gewerkt
voorbeeld) en het nieuwe `StaandeScalaireNormWitnessTest` (8 witnesses).

Eerlijk vastgelegd, uit de lensrondes: (1) de other_writer-witness rust op
een WF-050-ongeldig model (een regel naast een defaults-doel — de verifier
verwerpt het); dat is precies waaróm de pd-tak van other_writer op geldige
IR onbereikbaar is — de witness pint de clausule-mechaniek op de enige
vorm die haar observeerbaar maakt (ref divergeert op datzelfde model per
ch. 5.7; zonder de clausule beslist de groep 12.00 ongehinderd en splitsen
de executors stil); de verdeling-/objectcreatie-takken zijn legaal maar
corpusloos. (2) `is_plain_init` en de `desugarable`-lengtetelling zijn op
lezing geport en corpus-onbereikbaar (geen frontendbron produceert de
vormen); de temporele other_writer-tweeling deelt dit patroon. (3) De
2-arg `ExecModelTranspiler.transpile` krijgt geen defaults mee en is op de
pd-tak blind — het gatepad gebruikt de 3-arg build en is dat niet. (4)
Gates 8/11 hergebruiken een aanwezige jar en zijn deze kaart alleen met
handmatig ververste jars byte-identiek aan main bevestigd — gecard als
`backlog/new/20260830-gates-8-11-verse-jar.md`. Alle twaalf gates
integraal groen op de eindstand; gates 8 en 11 gemeten identiek aan main.
