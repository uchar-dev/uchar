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

Result: geland zoals gesneden, plus de verificatie-opbrengst. De drie
navigatievormen (by-name, keten, by-pair) en de genavigeerde kenmerkcheck
draaien in beide executors; WF-053 dekt resolutie, ketentypering,
endpoint, geschreven-naam-denotatie en hop-cardinaliteit, met vijf
negatiefs; de anchor jutterswet-kustwacht pint de absolute waarden, de
leeg-navigatie op hop 1 én hop 2 (attributeEmpty), en de
negatie-asymmetrie (W3 zonder link krijgt géén rustwacht, W4 mét link
wel). RUNTIME-SEMANTICS ch. 5.8 + ch. 6.1-navigatie-edges, IR-SPEC
ch. 4.1a + WF-053-indexrij, CONSTRUCTS rij 4 Landed.

Verificatievangsten (2 opus-lenzen), gefixt vóór de merge: (1) de
bindingless by-name-resolutie miste v0's endpoint-type-disambiguatie
(verbindendeRolViaNaam filtert op naam én doeltype) — endpoint-filter
toegevoegd op de laatste hop in verifier, ref en clingo; v0's
best-effort-meervoud is bewust NIET gespiegeld (luide weigering boven
heuristische naamvorming) en als tracked delta vastgelegd; (2) het
ambiguïteits-negatief raakte alleen de by-name-route — herontworpen
zodat by-name- én by-pair-ambiguïteit vuren; (3) spurious
navigatieBindingen op een fold-bron gleden stil door — WF-053-handhaving
in aggregation_source_check.

Bewuste keuzes: de verifier eist count-overeenstemming tussen de
geschreven vorm en de bindingen (bindingen-only multi-hop is ill-formed;
de executors zijn ruimer — de veilige richting). dep_expr/dep_cond zijn
ctx-bewust geworden; een genavigeerde kenmerkcheck registreert zijn
kenmerk-edge nu op het BEREIKTE type (was: subject — latente
mis-stratificatie, door de anchor-rangorde getuigd), en
expression_layer wandelt objectcreatie-toekenningen met subject
oc_subject (was: nieuw_type — latent fout voor navigatie/folds in
toekenningen). test_ref.ml: uitsluitend mechanische
recordveld-toevoegingen (navigatie_* = None), geen verwachting
gewijzigd.

Bevindingen buiten scope, vastgelegd voor vervolg: (a) het
leeg-coalescing-gat onder rekenoperatoren — ref coalescet leeg→0 bij
Plus/Min/Maal (ch. 1.2a) waar clingo's unbound join de regel niet laat
vuren; bestond al vóór deze kaart (elke mogelijk-lege operand), door
geen corpus-case geraakt; de anchor plaatst genavigeerde lezingen
daarom alleen in kale toewijzing/vergelijking/leeg-gevuld —
kandidaat-kaart: leeg-coalescing in de ASP-vertaling; (b) genavigeerde
fold-bronnen en set=true-vormen blijven overal luide faults
(v0-territorium voor een latere kaart); (c) de emitter-kant van
navigatieBindingen/navigatieBinding is pin-bump-territorium.
