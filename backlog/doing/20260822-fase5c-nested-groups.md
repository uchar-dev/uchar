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
