# Drools: vormcontrole op meerdelige temporele uniciteitssleutels

Goal: De Drools-backend handhaaft de vormcontrole die ref en clingo al
dragen: een uniciteitssleutel (uniciteit én uniciteitUnie) met meer dan één
temporeel sleutelcomponent is als vorm ongeldig en moet observeerbaar falen
in plaats van stil tot een model te lopen. Nu lowert `ExecModelTranspiler`
per component een `rsOoitBotst`-predicaat zonder ariteitscontrole, waardoor
twee rood-cases als NOFAULT gelijst staan in de Drools-exclusielijst van
`scripts/check-rood.sh` ("the v0 backend enforces no ARITY restriction on
temporal key components"). Maintainerbesluit 2026-08-30: klein kaartje,
ingepland na de fixpunt/ch-7.8-kaart; niet urgent — de lacune is luid
gelijst en machinaal bewaakt.

Acceptance:
- Rood-eerst: de twee NOFAULT-uitkomsten vóór de fix getoond.
- `sh scripts/check-rood.sh` → PASS; de twee entries verlaten de
  exclusielijst en pinnen elk hun `expectedFault.drools`-fragment
  (ref/clingo-fragmenten onaangeroerd; het script-regime dwingt dit af:
  een gelijste case die nu FAULT is een stale entry).
- Alle twaalf gates groen; gate 12 ongewijzigd (geen golden draagt de
  vorm); geen verwachtingswaarde gewijzigd.

May touch: `backends/drools/src/main/java/**` (de uniciteits-lowering of
een load-tijd-vormcontrole — volg ref's plek en strekking),
`backends/drools/src/test/**`, `scripts/check-rood.sh` (uitsluitend de
twee entries), `corpus/rood/*/case*.json` uitsluitend voor het TOEVOEGEN
van `expectedFault.drools`, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen, de overige
rood-exclusies, gate-12-gedrag.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. De Drools-backend handhaaft
nu ref's ch.-5.9-ariteit — hoogstens één temporeel component per
uniciteitssleutel — als VUUR-TIJD-foutregel in beide vormen (uniciteit
vóór de Inconsistentie-check; uniciteitUnie per bron, ref's
ever_tuples-plaatsing), met de melding byte-gelijk aan ref en clingo. De
vuur-tijd-keuze is machinaal bewezen (lens 1, boobytrap: een
bouwtijd-worp landt als ERROR waar de rode fence een pinbare FAULT eist).
Gate 8: de twee rood-cases verlieten de exclusielijst en pinnen elk
"drools": "at most one temporal key component" — 1 → 3 fault-pinned / 19
not-carried, met het volledige rood-eerst-spoor inclusief de getoonde
stale-tussenstand. Enige corpuswijziging: die twee drools-velden;
ref/clingo-fragmenten, goldens en toka byte-onaangeroerd. Gates 11
(28/1/0) en 12 (86/50/1/1) ongewijzigd; JUnit 52 → 59 (7
witnesses: beide foutarmen op de VOLLE gedeelde zin — lens 2 bewees dat
een herschreven melding het korte fragment overleefde — de instantieloze
arm, twee vindt-de-botsing-nog-armen, en de venstertak voor beide vormen,
die géén gate dekte).

Eerlijk vastgelegde restlacunes, alle pre-existent en lens-bewezen: (1)
de naamnormalisatie-conventie — containsKey exact op alle vier
sleutelsites waar ref/clingo normaliseren; een anders gekapitaliseerde
sleutel passeert de verifier, ontwijkt de guard én mist botsingen op een
geldig model — plus (2) de kruis-bron gemengde unie (temporeel op bron A,
scalair op bron B) die Drools laat crashen waar ref ≡ clingo spelen:
samen gecard als backlog/new/20260830-drools-sleutelconventies.md. Verder
genoteerd: het venster-zonder-rekendatum-randgeval (ref fault, Drools
stil — pre-existent geldigGuard-gedrag); clingo werpt de vormfout op
VERTAALtijd, instantie-onafhankelijk — dit backend volgt ref's
evaluatiemoment, en de rode fence pint alleen instantie-dragende cases,
dus de splitsing is daar onobserveerbaar; de per-bron-telling van de
unie-guard is ref-getrouw maar op corpusvormen ononderscheidbaar van
per-constructie (WF-030 houdt de asymmetrische unie buiten de corpus).
Alle twaalf gates integraal groen op de eindstand.
