# Fase 5c, kaart 2 — geneste ladder-groepen

Goal: PHASE5C-TUTORIAL §4 end-to-end (optie i geratificeerd; vraag 5:
A — sub-groep-conflict propageert naar de root, pad-gesleuteld
§9.5-feit, het rij-8-deviatierecord verruimd): `Rung.excepties`
{ongeordend, leden} incl. de groep-rung-vorm (excepties zonder eigen
consequentie — draagt meervoudige basisgroepen); RUNTIME-SEMANTICS
ch. 7 gegeneraliseerd; WF-050-recursie en dep_forms-uitbreiding
(descendant-reads als negatieve randen naar het root-doel; WF-051
ongewijzigd); ref-recursie in member_decision; het clingo-ontwerp
(per-pad-predicaatfamilies, waardeprioriteit sub-groep vs eigen
consequentie) — echt ontwerpwerk, geen toepassing; het
spurious-conflict-ankergezin als bindende toets (only-y → y consistent;
both-differing → root-no-write + pad-feit; both-equal; neither; base;
in beide doelsoorten, incl. een meervoudige basisgroep); plugin: de
recursieve boomwandeling op de just-true/false-discriminant, de
sub-root-weigering versmald.

Acceptance: alle negen gates groen; rood-eerst; herpin; twee
opus-lenzen met bevindingen gefixt vóór de merge.

May touch: als kaart 1. Out of scope: structs/subscopes/Let (andere
kaarten); de gedeelde niet-triviale basis onder alle leden (blijft
geweigerd, recorded).

## Review-fix (twee opus-lenzen, 2026-08-23)

Twee lenzen vonden één BLOCKING-divergentie plus bijvangst; alle vier
verwerkt, alle negen gates groen (73 ir.json; 132 negatief; 189 cases;
oracle ≡ clingo = 173; survivors ongewijzigd 22).

- FIX 1 (BLOCKING, ref ≢ clingo): het clingo `inconsistent/2`-deposit van
  een sub-groep-conflict vuurde op het kale `diff`-atoom, zonder de
  verdringingswacht die het keten-`cfl` wél droeg. `translate_group`/
  `translate_chain` dragen nu een geaccumuleerde `~outer_displaced`
  (`not stlM` van alle strikt-hogere rungs plus de omhullende); het
  deposit draagt die conjunctie. De root-kale-sleutel-deposit blijft
  ongewijzigd (root-leden verdringen elkaar niet). Rood-eerst bewezen op
  het nieuwe ankercase `nestuitzondering/case9-verdrongen-conflict`
  (X+B1+B2: clingo gaf 40.00 ÉN `inconsistent("jutter.heffing/1")` —
  incoherent — nu 40.00 consistent). Drie lens-gevallen dicht: (a) case9;
  (b) two-level inner-verdringing; (c) X+Y+Z+B1+B2 → alleen /0.
- FIX 2: rood-eerst-getuige + nieuw anker `anchor/jutterswet-nestdiepte`
  (drielaags, drie cases tegen ref gepind); ch. 7.9-normzin over
  verdringing van de sub-groep van een lagere rung + nestdiepte-tabel.
- FIX 3: de S17→S18-wezen (ASP-TRANSLATION, CONSTRUCTS rij 10, PLAN,
  twee rood-NOTEs); nul resterende wezen repo-breed (de nieuwe S17
  "Nested exception groups" en zijn backref blijven staan).
- FIX 4: WF-075-clausule + verifier-check — een groep-rung met een
  niet-null voorwaarde is nu onwelgevormd (fail-fast-gat gedicht);
  negatief `wf075-grouprung-with-justification`; IR-SPEC bijgewerkt.

Result: geneste ladder-groepen zijn kernel, end-to-end, onder de
geratificeerde optie (i) en het vraag-5-A-besluit. Spec-first: het
additieve Rung.excepties-veld met SubLadder (roosboom; de twee
rung-vormen incl. de groep-rung voor meervoudige basisgroepen; init-
en input-rung root-only), IR-SPEC ch. 6.1/WF-075, RUNTIME-SEMANTICS
ch. 7.9 (recursieve lezing; conflict-propagatie naar de root met
pad-gesleutelde §9.5-feiten; het rij-8-deviatierecord verruimd). De
keten met rood-eerst per stap: verifier met zeven wf075/wf050/wf051-
negatiefs (dep_forms vouwt descendant-reads in de root-randen —
WF-051 ongewijzigd, kruisladder-cycli gedekt), de ankers
jutterswet-nestuitzondering (negen cases: de spurious-conflict-toets
alleen-y → 10.00 als bindend paper-antwoord; sub-conflict → root-
no-write + "Jutter.heffing/0"; basisgroep-conflict → "/1";
equal-decisions op sub-niveau; verdrongen-conflict) en
jutterswet-nestkenmerk (het kenmerk-besluit-false-verdringen), ref
(recursieve member_decision met pad-opbouw en recursieve
speculate-flush), clingo (recursieve translate_chain/translate_group
met per-pad-predicaatfamilies, NAF-vrij per constructie, één stabiel
model; ASP-TRANSLATION S17, inventaris → S18), en de plugin (de twee
platte parsers → één recursieve boomwandeling op de
just-discriminant; de artikel-3-boomvorm als positieve E2E-fixture
mét de spurious-conflict-toets frontend-side; de sub-root-weigering
versmald tot de gedeelde-basis-vorm; 91 checks). Lensronde (2 opus):
één BLOCKING, door beide lenzen onafhankelijk en empirisch bewezen op
het gescheepte anker — clingo deponeerde het pad-feit van een
VERDRONGEN sub-groep (het inconsistent-hoofd miste de
verdringingswacht die het keten-conflictatoom wél droeg): ref 40.00 +
consistent, clingo 40.00 ÉN het feit — een incoherent model. Fix: de
geaccumuleerde not-stl-wachten doorgeregen tot in het deposit
(root-kale-sleutel onaangeroerd), rood-eerst vastgelegd op het nieuwe
case9-verdrongen-conflict, en de verdringingsnorm expliciet in
ch. 7.9. Plus: het diepte-2-getuigengat gedicht met het anker
jutterswet-nestdiepte (drie niveaus, ref als scheidsrechter — het
diepste conflict sleutelt op /0/0), twaalf-plus S17→S18-wezen
hersteld (repo-breed nul resterend), en het fail-fast-gat gedicht
(groep-rung met loze voorwaarde nu WF-075-onwelgevormd, met
negatief). Alle negen gates groen: 73 ir.json round-trip, 132
negatiefs, 189 cases, 173 dubbel (ref ≡ clingo), 151 no-op-refuters +
22 survivors, 16 rood, 91 plugin-checks, 4 Catala-goldens
byte-identiek. Bewust gelaten (luid): de gedeelde niet-triviale basis
onder alle leden blijft geweigerd (de PHASE4-§7-randbeperking);
ref/test/test_ref.ml gewijzigd (kaart-gesanctioneerd, WV-7.9-blokken,
hier gemeld). Herpin volgt direct na de merge.
