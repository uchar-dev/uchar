# De recursieve-feitcreatie-getuige: transitieve sluiting door beide executors

Goal: het vastgelegde maar ongetuigde WF-033-pad — een feitcreatie als lid
van een recursieve regelgroep, guard-vrij omdat relatiecreatie idempotent
is — krijgt zijn anchor: een transitieve sluiting (de feitcreatie leest
haar eigen gecreëerde relatie als tussenstap en breidt haar uit over de
invoerketen) die in ref (cyclus-tolerante strata, idempotente
add_link-fixpoint) en clingo (recursieve rol/3-regels, gewone stabiele
modellen — geen generatiediepte nodig: er ontstaan geen nieuwe objecten)
tot dezelfde sluiting komt. Bijvangst die de getuige blootlegt: wf033
eiste een OBJECTcreatie-lid en wees een groep met alleen een
feitcreatie-lid af, in tegenspraak met dep_forms/WF-045 — de check
verruimt naar creatie-lid van beide soorten, met de guard-eis alleen op
objectcreaties.

Acceptance: alle acht gates groen; anchor jutterswet-erflijn draait de
sluiting met absolute foldwaarden door beide executors; wf033's
verruiming laat het bestaande negatief intact; WF-033-indexrij en
CONSTRUCTS-notitie bijgewerkt.

May touch: spec/verifier/lib/checks.ml (wf033), spec/IR-SPEC.md (WF-033),
spec/CONSTRUCTS.md, corpus/golden/anchor/jutterswet-erflijn/,
corpus/golden/anchor/README.md, backlog/.

Out of scope: nieuwe WF-nummers; wijzigingen aan ref/clingo (de
machinerie bestaat); recursieve feitcreaties die objecten minten (dat is
objectcreatie-territorium).
