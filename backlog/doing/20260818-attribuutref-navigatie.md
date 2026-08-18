# AttribuutRef-navigatie uitvoerbaar (rij 4-slotstap)

Goal: de genavigeerde attribuutlezing en de genavigeerde kenmerkcheck worden
uitvoerbaar in beide executors en verifieerbaar in gate 3, met een symbolische
RolBinding-navigatie-extensie op de v0-wire (§ Method-discipline). De
v0-semantiek wordt exact gespiegeld: een afwezige to-one link leest als leeg
(per-hop short-circuit door de keten); een to-many hop in een scalar-slot is
statisch verboden (WF-053), nooit een stille eerste keuze; een genavigeerde
kenmerkcheck op een afwezige relatie is onwaar, óók ontkend (de bewuste
v0-asymmetrie met vergelijking en leeg/gevuld, die de afwezige relatie als
leegwaarde behandelen). Drie navigatievormen: by-name single-hop
(`navigatieRol`), keten (`navigatiePad`), en by-pair (`objecttype` direct
benoemd, geen rol). WF-053 dekt resolutie, filler-/ketentypering, endpoint-
attribuut, geschreven-naam-denotatie en hop-cardinaliteit; bindingless volgt
het WF-048-model (unieke afleidbaarheid). `dep_forms` registreert per hop een
`n_rel`-read zodat stratificatie navigatie-na-creatie ordent.

Acceptance: alle acht gates groen. Een nieuwe anchor-case draait met absolute
verwachtingen door ref én clingo (single-hop, keten, by-pair, leeg-navigatie,
genavigeerde kenmerkcheck incl. de negatie-asymmetrie-getuige); de
wf053-negatiefs worden geweigerd met WF-053 in de melding; CONSTRUCTS rij 4
draagt de Landed-notitie; de WF-index krijgt de WF-053-rij.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md,
spec/schema/regelspraak-ir-v0.schema.json, spec/verifier/lib/,
ref/lib/, ref/bin/, backends/clingo/, corpus/golden/anchor/ (nieuwe case +
README), corpus/negatief/wf053-*, corpus/rood/ (alleen indien een echt
runtime-only faultpad bestaat), backlog/.

Out of scope: emitterwerk in de toolchain (de extensie blijft
anchor-pioneered tot een pin-bump-kaart); multi-hop genavigeerde
kenmerkchecks (bestaan niet in v0 — Kenmerk draagt één rolnaam); de
v1-formaat-bump; temporeel genavigeerde kenmerken (v0-marker, blijft marker).
