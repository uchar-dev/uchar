# Source anchoring (cross-repo: lands in regelspraak-java)

Ontparkeerd 2026-08-21 (besluitenronde 2, punt 2b): tweede grote
vervolg, na het fase-5-voorstel; de toolchain is ervoor vrijgegeven.

Goal: the Phase-0 job from `spec/PLAN.md` §6 that lands in
`regelspraak-java`: replace the bare `String bron` with
`Bron(file, line, column, snippet)` on `Expr`, `Conditie`, and every
`Resolved*` form, and pass `// Bron:` statute annotations through the
lexer on a hidden channel so explanations can cite articles of law. Small,
mechanical, touches no semantics.

Acceptance: every IR node carries file/line/column; a statute annotation
written above a rule is retrievable from the resolved form;
`regelspraak-java`'s own gate set stays green.

May touch (in regelspraak-java, on a branch there): `ir/`, lexer/parser
plumbing, resolver construction sites; nothing here except `backlog/`.

Out of scope: the v1 `Bron` structure in IR-SPEC (row 3 delta — specified
here, separately); verbalization/explanation output changes.

Result (2026-08-21): geland in regelspraak-java (68fec24 + 1ce8ca3,
lensfixes a0ba971, herpin 44d0d1b; alle gepusht). Het Bron-record
(file, line, column, snippet; toString = snippet) vervangt de kale
String op Expr, Conditie en de vier Resolved*-vormen die ooit een
bron-anker droegen (Consistentie, RegelGroep, Uniciteit,
UniciteitUnie — de overige Resolved*-vormen hadden er nooit een; de
kaartfrase "every Resolved* form" was in zoverre aspirationeel);
~56 resolver-constructiesites dragen echte coördinaten, één site is
synthetisch (het "waar"-literal van de kenmerkwaardetoekenning).
// Bron:-annotaties gaan via het hidden channel naar
ResolvedRegel.bronAnnotaties. Rood-eerst: BronTest compileerde rood
(5 fouten) vóór de implementatie. De wire is byte-identiek bewezen:
11/11 emitter-goldens, en make uchar-conformance is groen op de
nieuwe pin d79999e (de herpin die het pin-regime eiste nadat de
normatieve paden deze sessie bewogen — het regime werkte precies
zoals ontworpen). Volle suite 955 tests groen. De lenzen vingen één
echte MUST_FIX: de engine-module brak op drie Verbalisator-sites die
bron() als String consumeerden (de toString-vangnet dekte
concatenaties maar geen String-aanroepen) — gefixt met .snippet(),
engine-build groen; plus de completering dat de CLI de bestandsnaam
nu doorgeeft waar de invoer één bestand is (facts altijd; --ir alleen
enkelbestand — een keten blijft bewust naamloos omdat
per-bestand-offsets zouden liegen). Precisie-punten uit de lenzen,
recorded: beslistabel-gedesugarde regels dragen coördinaten in de
synthetische herparse-stream (wire onaangetast; de
annotatie-guard levert daar correct leeg); de annotatie-associatie
overbrugt blanco regels en tussenliggend commentaar (breder dan
"direct erboven"; gedrag goed gedefinieerd, alleen het
enkelvoudige geval regressie-getest). Bewust gelaten:
ResolvedScenario.Verwachting.bron blijft String (test-only-extensie,
consumptie in de aparte engine-module); bronAnnotaties alleen op
ResolvedRegel; de v1-Bron-wire-structuur (rij 3-delta) blijft de
aparte spec-kaart. Notitie voor de maintainer: hiermee lijken de twee
resterende fase-0-exitcriteria (file/line/column op elke node;
citeerbare statuutannotaties) materieel vervuld — de
Met-markering in PLAN §6 fase 0 verdient een eigen verificatiekaart,
niet een stilzwijgende bijschrijving hier.
