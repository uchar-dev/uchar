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
