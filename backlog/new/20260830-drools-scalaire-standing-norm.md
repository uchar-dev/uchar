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
entries), `backlog/`.

Out of scope: kustwacht (echte v0-divergentie, blijft), de
CaseRunner-taxonomie-herziening als zodanig, temporele vormen (gedaan),
objectcreatie, transpiler-scope-regels, ref/clingo/spec/corpus.
