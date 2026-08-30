# Drools: wederkerig self-relation (gate-12- en gate-11-dekking, familie 2)

Goal: `FactModelGenerator` leidt voor een wederkerig feittype (§13.3.9.2,
hetzelfde objecttype aan beide zijden) een referentieveld voor de
self-pair-rol af, zodat een regel die die rol leest een getter heeft;
`CaseRunner` zet die rol uit case-input en de weigering in
`CaseRunner.build` (`model.feittypen … wederkerig`) vervalt. Daarmee spelen
`anchor/jutterswet-maatschap` en `jutterswet-aanmelderkrediet` (gate 12 → +2
played) en komt gate 11's Drools-been tot leven: nu 0 PLAYED / 29 loud
EXCLUDE, waarvan 28 op precies deze vorm. Onderzoekshint: de
productietoolchain draait dezelfde casus wél — daar komen beanvelden uit de
gedeclareerde schema-velden (de inerte v0-mirror-bagage `zetVeld`/`joinVeld`),
terwijl uchar's `FactModelGenerator` ze symbolisch afleidt; de fix hoort in
de symbolische afleiding, nooit in het opnieuw lezen van beanveld-bagage.

Acceptance:
- Rood-eerst: de twee gate-12-EXCLUDE-regels en de gate-11-telling
  (0 PLAYED / 29 EXCLUDE) vóór de fix getoond; erna PLAYED.
- `bash scripts/check-drools.sh` → PASS, played +2 en auto-excluded −2 ten
  opzichte van de stand na de temporele-inputpoortkaart; geen
  verwachtingswaarde gewijzigd.
- `bash scripts/check-toka-parity.sh` → PASS; `toka/cases/parity.manifest`
  herpind: elke slice die na de fix tegen zijn gepinde verwachtingen speelt
  gaat van `EXCLUDE|fragment` naar `PLAYED` (verwacht ~28; 1 blijft EXCLUDE
  op objectcreatie); wat niet speelt blijft een luide EXCLUDE met fragment of
  wordt een genummerde maintainersvraag. Toolchain-gepinde
  `toka/**/ir.json`'s blijven byte-identiek.
- Alle twaalf gates groen. De kaart-Result noemt uitsluitend geaggregeerde
  tellingen en klassenamen — geen toka-scenario-ID's (het manifest en toka/
  zelf mogen ze wél dragen).

May touch:
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java`,
`.../schema/ContractValidator.java` (indien het contract het nieuwe veld
moet kennen), `.../cases/CaseRunner.java`, `backends/drools/src/test/**`,
`toka/cases/parity.manifest`, `scripts/check-drools.sh` (uitsluitend een
eventuele `EXCLUDE_KNOWN`-entry), `backlog/`.

Out of scope: nieuwe transpiler-lowering-vormen, de twee overige
relatie-excludes (het rolreferentieveld-geval en de
kenmerk-naamdeling-bean-limiet), objectcreatie, wijziging van
toka-ir.json's of van ref/clingo/verifier/spec/corpusverwachtingen.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. `FactModelGenerator.zelfpaarRol`
leidt het §13.3.9.2-self-pair-referentieveld symbolisch af (wederkerig-guard,
`verbindendeRol(ot,ot)`, veldnaam = de enkelvoudige rolnaam, zelf-getypeerd
scalar; de afleiding leest nooit de inerte `zetVeld`/`joinVeld`-wire-bagage)
en `CaseRunner` spiegelt de omgekeerde poot in een aparte pas ná alle
rolzetting — conform ref's `materialise_reciprocal` en RUNTIME-SEMANTICS
§4.1, orde-onafhankelijk (door lens 1 over alle zes instantievolgordes
bewezen; twee ketenwitnesses maken het falsifieerbaar), idempotent, met
WF-013-weigering van een botsende poot. Lens 1 bevestigde ref ≡ Drools op
vijf zelfgebouwde vormen (eenzijdige levering, beide poten, zelfkoppeling,
botsende keten in twee volgordes, meervoudssleutel).

Gate 12: 79→81 played / 52→50 auto-excluded (anchor/jutterswet-maatschap en
jutterswet-aanmelderkrediet spelen tegen hun ongewijzigde pins);
value-divergence 6 en no-case 1 onveranderd. Gate 11: het Drools-been van
0 PLAYED / 29 EXCLUDE naar 28 PLAYED / 1 EXCLUDE (objectcreatie) / 0
FAIL-or-ERROR — 28 manifestregels herpind, nul case- of ir-bytes bewogen.
KANTTEKENING, in de manifest-header vastgelegd en door beide lenzen
mechanisch herbevestigd: die 28 zijn een ZWAKKE getuige — het wederkerige
feittype wordt in alle toka-ir's gedeclareerd maar door geen case gelinkt en
door geen regel genavigeerd, dus zij bewijzen alleen dat het afgeleide veld
inert is waar ongebruikt; de geoefende semantiek zit in de twee
corpus-goldens en de witness-suite. Geen verwachting, pin of WF-nummer
bewogen; corpus, spec, ref, clingo, scripts/, ContractValidator, toolchain
en toka-ir's onaangeroerd.

De lensrondes brachten drie luide-weigering-hardings, elk rood/groen
bewezen: (1) de naamdelings-bean-limiet — een gedeclareerd lid dat de
rolnaam deelt verdringt het veld via `leden()`'s naam-dedup; nu een
per-vorm-Excluded i.p.v. een ongeclassificeerde ERROR (NB: de kaart-Out-of-
scope noemde de bestaande kenmerk-naamdeling-limiet als niet-aan-te-raken;
deze guard raakt die exclude-klasse niet maar bewaakt het NIEUW afgeleide
veld); (2) de ambigue/to-many-zelfpaarvorm (twee wederkerige feittypen op
één objecttype) — `noemtWederkerig` splitst vorm van afleidbaarheid, de
onafleidbare helft weigert luid; (3) de voorwaartse to-one-poot —
`zetToOne` weigert een tweede, ander doel (ref weigert die case bij het
laden), waar last-wins een kapotte case stil groen zou spelen. De
`invoerBeans`-seam zet nu ook rollen (replay-volgorde), zodat de
spiegelvulling bean-voor-bean asserteerbaar is; JUnit-backend 20→32/0.

Bewust gelaten: de wederkerig-guard in `zelfpaarRol` is aantoonbaar
belt-and-braces (javadoc herschreven — de eerdere niet-redundant-claim was
onwaar); de to-many wederkerige rol is door de verifier onbereikbaar
(negatief-corpus) en alleen door lezing gedekt; reflexieve zelfkoppeling
(A→A) is in beide executors consistent één relatie, ongepind. Alle twaalf
gates integraal groen op de eindstand (160/168, 396, 374, 374, 338+36, 22,
169+11, 144+23, toka 28/1/0, drools 81/50/6/1; JUnit 32/0).
