# De zeventien grove inconsistent-cases verscherpen met rule/label-pins

Besluit maintainer 2026-08-21: gekozen in het werkmenu (veegbundel).

Goal: elke bestaande grove {kind: inconsistent}-verwachting krijgt de
rule- (en waar zinvol label-)pin die het kanaalpin-werk mogelijk
maakte, zodat een verkeerde-regel-fout niet langer vals-groen kan
zijn. Routinewerk, geen ontwerp; per case de juiste vormnaam uit het
document.

Acceptance: geen grove inconsistent-verwachting meer zonder pin (of
een beredeneerde uitzondering in het Result); alle acht gates groen;
twee lenzen (steekproefsgewijs op de pin-juistheid).

May touch: `corpus/golden/`, `corpus/golden/anchor/` (verwachting-
verscherping is additief beleid), `backlog/`.

Out of scope: schema- of checkerwijzigingen.
