# Fase-5-voorstel: Catala als frontend

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 2a); eerste
grote vervolg na de pin-bump.

Goal: spec/proposals/PHASE5-CATALA.md naar het model van
PHASE4-DEFAULTS — het aanhechtpunt in de Catala-compiler (het
default-calculus-niveau: sugar weg, typen bekend, exception-bomen
genormaliseerd), de mapping van dat niveau naar de kern-IR (scopes
met in/uit-variabelen tegen objecttypen/creaties/navigatie;
ConflictError op het §9.5-kanaal; geld/rationalen/datums op de
exacte rekenkunde; folds en function values als geplande
kernuitbreidingen, CONSTRUCTS rijen 10/36), de tutorial-case als
eerste doel door verifier én beide executors, de frictiepunten
eerlijk (tijd: onze as is rijker; evaluatiemodel: zwak-naar-sterk),
en een gefaseerd migratiepad met besluiten voor de maintainer. Nog
geen code.

Acceptance: het voorstel staat compleet met een besluitenlijst;
lenzen checken tegen het Catala-paper (ICFP 2021), CONSTRUCTS.md en
de spec; alle acht gates groen (document-only).

May touch: `spec/proposals/`, `backlog/`; CONSTRUCTS.md alleen als
het voorstel rijen aanscherpt.

Out of scope: de plugin zelf; IR-wijzigingen.

Result (2026-08-21): spec/proposals/PHASE5-CATALA.md staat compleet —
aanhechtpunt (de scopelang→dcalc-grens, met het verificatievoorbehoud
voor 5a), de term-voor-term-mapping op de gelande kern, de
rechtenschone corpusroute (tutorial frontend-side; het
Jutterswet-in-Catala-artikel als corpuscase: hetzelfde artikel door
twee frontends, IR-diff met de bestaande ankers als scheidsrechter),
vijf eerlijke fricties met hun route, het gefaseerde pad 5a–5d, en
ZES genummerde besluiten in de opties-plus-aanbeveling-vorm van het
PHASE4-precedent. De twee lenzen vonden onafhankelijk dezelfde
BLOCKING in het concept: de C-Def-mappingrij conflateerde de twee
polariteiten die PHASE4-DEFAULTS §2.2 uitdrukkelijk scheidt — C-Def
is Reading B (de caller-waarde als hóógste exceptie, verslaat de
berekening), de gelande input-rung is Reading A (berekening verslaat
input). Gefixt vóór de merge: de rij mapt een context-override nu op
een top-rung (het §4.1-hardheidspatroon als Reading-B-precedent), de
input/context-variabelen zijn gesplitst, en de reconciliatie is het
nieuwe genummerde besluit 6 — open voor de maintainer, niet langer
een voldongen feit. Ook gefixt: de EmptyError-karakterisering
(∅-fall-through is het alomtegenwoordige kernmechanisme; alleen de
cross-scope-hoek blijft te beslechten in 5a) en de
D-Label-plaatsing gereconcilieerd met de companion. De zes besluiten
wachten op een maintainersronde; er is geen code gestart.
