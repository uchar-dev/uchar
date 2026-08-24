# De logica-rekenaar-rest: magnitudegrens en struct-invoerwaarden

Goal: twee gedocumenteerde clingo-flanken dichten. (1) De
magnitudegrens van de gesplitste geldvermenigvuldiging: het
intermediair op schaal 10^8 overschrijdt int31 bij legale invoer
(tutorial: kids >= 6) terwijl ref exact doorrekent en het
EINDresultaat representeerbaar is — verruim de encodering (bijv. een
verdere splitsing of een kleinere intermediaire schaal met bewezen
afronding, in de stijl van het bestaande
gesplitste-vermenigvuldigingsbewijs incl. ties) zodat de tutorialvorm
ruim voorbij kids=6 draagt; de hek-conditie blijft luid voor wat
werkelijk niet past. (2) De struct-VALUE-vorm: samengestelde
(geneste) structs als case-invoer/verwachting faulten in
struct_value_term — maak die recursief, consistent met de
struct_base-landing. Voor beide: rood-eerst, eigen corpus-getuigen
met absolute verwachtingen door beide executors, ASP-TRANSLATION
bijgewerkt (S18 resp. S16; S19-schrappingen waar gedicht; S19 blijft
de hoogste sectie), en de kids<=5-notities in de
tutorialfixture-header en S18 geactualiseerd.

Acceptance: alle negen gates groen; rood-eerst per flank; ref ≡
clingo over de volle corpus; twee opus-lenzen verwerkt vóór de merge.

May touch: backends/clingo/, corpus/golden/ (nieuwe getuigen),
corpus/rood/ (indien een hek-getuige past), spec/ASP-TRANSLATION.md,
frontends/catala/test/tutorial_end_2_3.catala_en (alleen de
grens-notitie), scripts/check-noop.sh (alleen indien nodig),
backlog/. GEEN spec/CONSTRUCTS.md (registry-conflictles — de
rij-notitie doet de maintainer-loop bij de landing). Out of scope:
ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/verifier/,
frontends-code.
