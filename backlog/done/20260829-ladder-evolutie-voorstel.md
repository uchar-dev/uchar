# Ladder-ontwerpronde — het voorstel LADDER-EVOLUTIE

Goal: één ontwerpvoorstel `spec/proposals/LADDER-EVOLUTIE.md` dat de volle
ladder-agenda dekt en eindigt in genummerde ratificatievragen aan de
maintainer — er wordt in deze ronde NIET gebouwd. De agenda, volledig:
(1) de ladder-producerende frontend-evolutie (RegelSpraak-bron →
defaultLadders, WF-050/051), inclusief de "tenzij"-taalvraag en de
verhouding tot v0's vlakke KENMERKTOEKENNING-vorm; (2) volledige
ladder-service in de Drools-backend (het open Fase-4-punt: lowering naar het
exec-model); (3) de afhankelijkheids-omdraaiing — de toolchain gaat
`frontends/regelspraak` consumeren (PLAN §5); (4) het
stratificatie-hoofdstuk, mét de dagsoort-recursievraag die WF-031 expliciet
open laat; (5) de taille-vragen: universele eenheid-conversie in de IR
(canonieke kern, dimensietags) én CONSTRUCTS rij 27 (Percentage lost op in
Number + %-eenheid + dialecttag; herspelling van het vindersloon-anker rijdt
mee); (6) de v1-delta-kandidaten uit de gate-12-triage als geformaliseerde
delta's (leeg-bron-toewijzing, leeg-operand-vergelijking, stratificatie van
genegeerde kenmerk-reads, de keurmerk-vondst); (7) het flip-checklist-punt
dat de Onbekend-sentinel zijn enige machine-getuige verliest bij de
publiek-flip; (8) herleving-WW als ladder-toets: de uitzonderingsstructuur
van WW art. 15–21 gedestilleerd tot laddereisen waaraan elk voorgesteld
patroon expliciet getoetst wordt.

Acceptance: `spec/proposals/LADDER-EVOLUTIE.md` bestaat, dekt alle acht
agendapunten, toetst elk ladderpatroon aan de herleving-WW-artikelen, en
sluit af met genummerde ratificatievragen (opties + aanbeveling per vraag);
twee adversariële lensrondes zijn gedraaid en verwerkt; alle twaalf gates
groen (`bash scripts/check-*.sh` — documentwijziging, geen code); geen
toka-scenario-ID's in het voorstel (alleen geaggregeerde tellingen en
klassenamen); handboek-citaten blijven buiten het document (wettekst mag,
art. 11 Auteurswet). Bouwen gebeurt pas ná ratificatie, op aparte kaarten.

May touch: spec/proposals/LADDER-EVOLUTIE.md (nieuw), backlog/.

Out of scope: elke bouwactie (frontend, backends, ref, verifier, schema);
spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md,
spec/PLAN.md (registrynotities en plan-zinnen doet de hoofdsessie serieel,
pas bij ratificatie-uitkomsten); de toolchain-checkout.

Result: spec/proposals/LADDER-EVOLUTIE.md geland — alle acht agendapunten
gedekt, elk ladderpatroon getoetst aan WW art. 15-21 (R1-R5 + de
atemporele grens), veertien genummerde ratificatievragen met opties en
aanbeveling. Verkenningsgolf: acht read-only opus-sporen. Lensronde: twee
adversariële opus-lenzen; lens 1 (feiten/citaten/rechten): mergeable,
vier MINORs — alle verwerkt (tellingen 6/11/15/20 ontward, tenzij-claim
verzacht, Onbekend-negatief-telling gecorrigeerd, het
RegelSpraak-spec-citaatvoorbeeld verwijderd); lens 2 (ontwerp): twee
MAJORs, beide in §6 — (1) de scenariotoetsen-laag van de toolchain zou
stilzwijgend sneuvelen bij de omdraaiing → expliciet belegd
(toolchain-local, re-home als alternatief, in Q5); (2) "één pin" klopte
niet onder een submodule → mechanisme herschreven naar vendored subtree
met byte-identiteitscheck tegen de gepinde bron (uchar-conformance wordt
precies die check), losse split-repo alleen nog als publicatievehikel
(Q6). Plus vier MINORs verwerkt (grammar-divergences-log bestaat alleen
in de toolchain; WW-eerste-case expliciet op scalaire invoerfeiten;
telling-consistentie; §3.4/Q14-voorbehoud). Bewust gelaten: Q2 en Q11
blijven als expliciete bevestigingsvragen in de set (lens-2 suggereerde
ze naar §11 te verplaatsen) — goedkoop voor de maintainer en de
Q1-context raakt ze echt. NIET gebouwd, per kaart: bouwkaarten worden pas
gesneden na ratificatie van §12.
