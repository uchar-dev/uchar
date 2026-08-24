# Fase 5d — de frontend-diff-oefening

Goal: hetzelfde stuk Jutterswet-recht door beide frontends —
RegelSpraak via de toolchain-resolver (--ir-dump op de gepinde
SPEC_REV), Catala via de plugin — en beide IR-dumps structureel
gediffed en geclassificeerd: (i) administratief (namen, bronnen,
volgorde), (ii) getrackte v0↔v1-delta (gewild, CONSTRUCTS), (iii)
echte ontwerpfout — dezelfde semantiek die zonder reden twee vormen
krijgt. Gedocumenteerd met de resulterende spec-gevolgen; daarmee
valt het tweede Phase-5-exitcriterium (PLAN §6: "the frontend-diff
exercise is documented with resulting spec changes").

Acceptance: het diff-document in spec/proposals/; elke
categorie-(iii)-vondst draagt een spec-gevolg (besluit-vrije delta,
of een genummerde maintainersvraag vóór de merge); alle negen gates
groen; PLAN §6 Phase-5-markering bijgewerkt; twee opus-lenzen
verwerkt vóór de merge.

May touch: spec/proposals/, spec/CONSTRUCTS.md, spec/PLAN.md,
corpus/golden/ (een nieuw paar-fragment indien nodig),
frontends/catala/ (fixture indien nodig), backlog/.

Out of scope: normatieve IR-SPEC/RUNTIME-SEMANTICS-wijzigingen
zonder genummerde vraag; elke toolchain-wijziging (read-only, de
dump-aanroep uitgezonderd).
