# Uniciteit-unies en scoped uniqueness uitvoerbaar; de WF-index-OPENs opgeruimd

Goal: de laatste twee vormen waarop beide executors botweg weigeren worden
uitvoerbaar, exact naar de gepinde v0-semantiek: (1) de uniciteit-unie —
één gedeeld uniciteitsdomein over alle bronnen, positionele sleuteltuples,
rijen met een lege sleutelcomponent doen niet mee, één §9.5-kanaalfeit per
(unie-naam × instantie) over de bronnen heen; (2) scoped uniqueness —
sleutels uniek binnen de deelpopulatie die dezelfde scope-ouder deelt,
scope-loze instanties doen niet mee (de stille v0-keuze, gespiegeld en
vastgelegd), met een symbolische `scopeRol`-RolBinding-extensie naast het
bean-veld `scopeVeld` (WF-054, naar het WF-047/053-model: resolutie,
near-side = subject, geschreven naam denoteert de bound-rol, to-one).
Scoped + unie tegelijk is in v0 niet representeerbaar en blijft dat.
Daarnaast de WF-index-opruiming richting Phase-1-exit: elke OPEN krijgt
een artefact of een expliciete motivering — twee bestaande goldens vullen
WF-035/038; een verifier-only getuigedocument dekt de
constructen zonder corpusgetuige (dimensies WF-004/014/040, enum
WF-006/039, variabel-start WF-016/032, dagsoort WF-017/031,
versievensters WF-021, tijdvenster WF-042, geheel-periode WF-043,
telkwantor WF-041); de ondiepe WF-023-slice (doel-type vs waarde-type:
familie, eenheden, teken) wordt geïmplementeerd met negatief;
WF-008/010/019 en de precisiepoort van WF-023 blijven OPEN met de reden
in de rij.

Acceptance: alle acht gates groen. Een nieuwe anchor draait de unie en
beide scoped-varianten met absolute verwachtingen door ref én clingo
(duplicaat over twee bronsoorten heen; zelfde sleutel in verschillende
scopes consistent; scope-loze duplicaten uitgesloten; lege sleutels doen
niet mee); wf054-negatiefs geweigerd met WF-054; het getuigedocument is
verifier-clean en de WF-index bevat geen enkele ongemotiveerde OPEN meer.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md,
spec/schema/regelspraak-ir-v0.schema.json, spec/verifier/lib/,
ref/lib/, backends/clingo/, corpus/golden/ (nieuwe documenten + anchor),
corpus/negatief/wf054-*, corpus/negatief/wf023-*, backlog/.

Out of scope: emitterwerk (scopeRol-emissie is pin-bump-territorium);
scoped unies (v0-onrepresenteerbaar); de WF-023-precisiepoort
(expressievorm-analyse); temporele sleutels in unies (v0 skipt ze —
vastleggen, niet bouwen); het leeg-coalescing-gat onder operatoren
(aparte kaart).
