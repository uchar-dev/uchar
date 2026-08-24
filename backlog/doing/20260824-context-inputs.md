# Besluit-6-context-inputs: de gegenereerde top-rung

Goal: het geratificeerde-maar-uitgestelde besluit over Catala's
context-inputs uitvoeren, nu de subscope-machinerie er is. Lees
EERST spec/proposals/PHASE5-CATALA.md (het besluit over de
context-override-polariteit als gegenereerde top-rung, Reading B als
rung-volgorde) en de PHASE5C-TUTORIAL §3-randvoorwaarde: een
caller-waarde op een GELADDERD callee-doel gaat als rung, nooit als
MemberInit (de WF-050-randvoorwaarde). De plugin lowert een
context-input op een subscope naar de geratificeerde vorm: de
caller-waarde wordt een gegenereerde top-rung boven de callee-ladder
(de bestaande refuse_context versmalt of klapt om); ladderloze
callee-doelen blijven de MemberInit-route. E2E: fixture(s) plus een
eigen catala-golden door plugin → verifier → ref → clingo. Wijk je
ergens van het geratificeerde ontwerp af of blijkt een verifier-
aanscherping (de randvoorwaarde als check) nodig die het proposal
niet expliciet dekt: BLOKKADE, geen eigen keuze.

Acceptance: rood-eerst (de huidige refusal letterlijk getoond); alle
negen gates groen; de refuse-fixtureset dekt exact wat blijft
weigeren; twee opus-lenzen verwerkt vóór de merge.

May touch: frontends/catala/, corpus/golden/ (de nieuwe
catala-golden), spec/CONSTRUCTS.md (UITSLUITEND de rij die
context-inputs draagt), spec/proposals/PHASE5-CATALA.md (de
Executed-notitie), scripts/check-noop.sh (alleen indien nodig),
backlog/. Out of scope: ref/, backends/, spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/verifier/ (een noodzakelijk
gebleken verifier-check = blokkade, eerst voorleggen).
