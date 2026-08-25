# De LegalRuleML-expressiviteitsaudit

Goal: de staande toets uit PLAN §3 ("can our IR express what
LegalRuleML can?") als grondig, read-only audit-document
spec/proposals/LEGALRULEML-AUDIT.md: per requirement-cluster van de
OASIS-standaard (defeasibility met overrides en prioriteiten;
meervoudige tijdassen; jurisdictie; deontische begrippen —
verplichting/toestemming/verbod; bronnen/autoriteit; alternatieven)
de IR-dekking met concrete verwijzingen (WF-regels, hoofdstukken,
corpus-getuigen), en eerlijk wat NIET gedekt is met het gevolg:
gedocumenteerd non-goal per het PLAN (bijv. de enkele tijdas als
besluit 2), tracked delta, of kaartkandidaat. Geen frontend-bouw;
geen normatieve wijziging.

Acceptance: het document staat, is zelf-consistent met PLAN en
CONSTRUCTS, en elke dekkingsclaim draagt een concrete verwijzing die
een lens kan natrekken; gates groen; twee opus-lenzen verwerkt.

May touch: spec/proposals/LEGALRULEML-AUDIT.md (nieuw), backlog/.
Out of scope: al het overige; spec/CONSTRUCTS.md niet aanraken.

## Verricht (2026-08-25)

`spec/proposals/LEGALRULEML-AUDIT.md` staat: de OASIS LegalRuleML Core
Specification v1.0 (OASIS Standard, 30 augustus 2021) opgehaald via curl,
exacte versie/URL genoteerd; per requirement-cluster (defeasibility, tijd,
jurisdictie, deontisch, bronnen/autoriteit, alternatives/context) een
GEDEKT/DEELS/NIET-oordeel met concrete verwijzing (WF-regel, IR-SPEC/RS-
hoofdstuk, corpus-getuige) die een lens kan natrekken. Adversarieel-eerlijk:
niet naar dekking toe geredeneerd — override en strength-tags zijn DEELS/NIET,
de meervoudige tijdassen NIET (besluit 2), deontisch/jurisdictie/alternatives
NIET (non-goals rows 13/32/33), bronnen DEELS. Zelf-consistent met
`spec/CONSTRUCTS.md` rows 3/6/8/9/12/13/32/33 (niet aangeraakt) en PLAN §3/§9.

Sluit af met het §3-antwoord in één alinea + kandidatenlijst:
- **C1** — statuut-annotaties op de uchar-wire getuigen: `bronAnnotaties`
  bestaat upstream (Phase 0) maar 0 corpus-goldens dragen het (geverifieerd);
  PLAN §4-belofte nog ongetuigd.
- **C2** — de override/strength-vraag beslissen: uchar heeft alleen
  intra-target ladder-prioriteit, geen vrije superioriteitsrelatie of
  strict/defeasible/defeater-tags (CONSTRUCTS row 8 "refinements").

Alle negen gates groen (slotregels):
1. check-self-contained: PASS
2. check-ir-schema: PASS (schema valid; 104 file(s) validate and round-trip)
3. check-verifier: PASS (104 golden clean; 142 negatief rejected)
4. check-case-schema: PASS (schema valid; 279 case file(s) validate)
5. check-oracle: PASS (261 cases replayed exactly)
6. check-clingo: PASS (261 cases replay exactly through clingo)
7. check-noop: PASS (228 refute a do-nothing executor; 33 pinned survivors)
8. check-rood: PASS (18 red cases fault observably in both executors)
9. check-catala: PASS (frontend suite green, 169 checks; 11 catala goldens
   regenerate byte-identically)

Geen normatieve wijziging; `spec/CONSTRUCTS.md` niet aangeraakt. Resterend
per acceptance: twee opus-lenzen verwerken (orchestratie-vervolg).

## Lensronde 1 verwerkt (2026-08-25)

Negen bevindingen (2 MAJOR, 7 MINOR), alle negen zelf geverifieerd tegen
repo en OASIS-brontekst (opnieuw opgehaald) — geen enkele weerlegd; alle
negen verwerkt in commit 59b33a7:

- **MAJOR (1+6, zelfde defect):** applicability-gating-rij cluster 2 was
  GEDEKT op getuige `jutterswet-vormenkabinet`, die per eigen NOTE nooit
  kan replayen en het enige document met begrensd geldigheid-venster is
  (scan over alle 104 ir.json's bevestigd; geen case*.json aanwezig).
  Rij naar DEELS (WF-021 verifier-getuigd + negatief
  `wf021-version-overlap`; RS ch. 8.2-runtime-gating door nul cases
  geoefend); clusterverdict, §7-antwoord en ledger aangepast; nieuwe
  kandidaat **C3** (replayende twee-versies-golden, asOfDate per
  venster); "exactly two live loose ends" → three.
- **MINOR (3+7, zelfde defect):** afleidbare Override
  (`body => (r1 > r2)`) is Antoniou-literatuur in OASIS §4.2.1, geen
  standaard-machinerie (16 Override-voorkomens gescand; Context §3.6
  heeft appliesStrength, geen appliesOverride — herbevestigd tegen de
  opgehaalde bron). §1-intro, Override-rij en C2 versmald.
- **MINOR 2:** "PLAN §5 (Phase 5)" was tweemaal mis (§5 = Where things
  live; de voorspelling op r. 481 gaat over Catala) — nu §4.3.5's eigen
  uitsluiting als drager, PLAN §6 (Phase 5) als Catala-parallel.
- **MINOR 4:** statuutomvang "art. 1–5" → "art. 1–5 in v0 growing to
  art. 1–9 in v4" (grep '^\*\*Art' over wet-v*.md bevestigd).
- **MINOR 5:** OASIS-citaat §4.3.5 miste "or desired" — hersteld
  woordelijk (tegen de bron gecontroleerd).
- **MINOR 8:** "six conflict-path cases" → zes cases waarvan drie het
  conflictpad pinnen, case4 de gelijke-gronden-geen-conflict-flank
  (grep inconsistent over de zes case-files bevestigd: 3/6).
- **MINOR 9:** Compliance (NIET, row 32, bij de Violation-rij),
  Paraphrase (NIET, bron-metadata-familie, nieuwe rij cluster 5) en
  FactualStatement (triviaal GEDEKT, case-dossier/gate 4) elk één zin
  gegeven (grep bevestigde afwezigheid van alle drie).

Gates na verwerking: check-self-contained PASS, check-ir-schema PASS
(104 files validate and round-trip). Resterend: tweede opus-lens.
