# Het leeg-coalescing-gat onder rekenoperatoren in de ASP-vertaling

Goal: de laatste bekende klasse stille ref≠clingo-divergentie dicht. De
ch. 1.2a-whitelist (leeg leest als 0 onder plus/min/maal;
verminderd-met: links propageert, rechts coalescet) krijgt zijn
ASP-encodering: elk coalescerend operand-slot dat leeg kán zijn wordt
een default-0-hulppredicaat (waarde wanneer de lezing bindt, 0 via
negation-as-failure met een definedness-helper); een propagerend slot
(verminderd-met links) blijft de kale unbound join — leeg laat de regel
niet vuren, exact refs Empty-propagatie. Min en Verminderd-met treden
toe tot het vertaalde fragment (zelfde schaalharmonisatie als Plus);
een anchor pint leeg onder elke vorm absoluut in beide executors.

Acceptance: alle acht gates groen — inclusief de bestaande anchors met
Plus op creatie-cycli (wachtersketen, erflijn: de NAF-encodering mag de
stabiele modellen daar niet verstoren); de nieuwe anchor pint
leeg-als-0 onder Plus en Min, en de verminderd-met-asymmetrie
(links-leeg → doel leeg via attributeEmpty; rechts-leeg → coalescet).

May touch: backends/clingo/lib/translate.ml, spec/ASP-TRANSLATION.md,
spec/RUNTIME-SEMANTICS.md (alleen indien nodig), spec/IR-SPEC.md
(alleen indien nodig), corpus/golden/anchor/jutterswet-leegrekening/,
corpus/golden/anchor/README.md, backlog/.

Out of scope: gedeeld-door/macht/percentage-van in de vertaling (blijven
luide faults); unit-conversie binnen operatoren in clingo (bestaande
flank, genoteerd); ref-wijzigingen (ref is de mirror en al correct).

Result: geland zoals gesneden. De ch. 1.2a-whitelist heeft zijn
ASP-encodering (coalesce0: waarde bij binding, 0 via
negation-as-failure met definedness-helper; propagerende posities
blijven de kale unbound join), Min en Verminderd-met traden toe tot het
vertaalde fragment, en de anchor jutterswet-leegrekening pint alle
whitelist-cellen absoluut in beide executors — inclusief de
verminderd-met-asymmetrie (links-leeg propageert naar attributeEmpty,
rechts-leeg coalescet). Alle bestaande anchors, ook de recursieve met
Plus op creatie-cycli, bleven stabiel onder de encodering. De
encoderings-lens weerlegde niets; de dekkings-lens ving één MINOR
(plus-rechts-leeg met attribuut-operand ongetuigd) — gedicht met de
som-regel (5 plus korting: 5+0=5) vóór de merge. Encodering
gedocumenteerd in ASP-TRANSLATION.md ch. 6a. Buiten scope gebleven,
luide faults: gedeeld-door, macht, maal en percentage-van in de
vertaling; de unit-conversie-flank binnen operatoren is genoteerd als
bestaande, aparte kwestie.
