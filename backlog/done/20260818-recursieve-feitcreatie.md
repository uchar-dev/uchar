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

Result: geland zoals gesneden. De anchor jutterswet-erflijn draait de
transitieve sluiting door beide executors (fold 3/2/1/0 over de
vier-wachterketen); wf033 verruimd — de recursiedrager is een
creatie-lid van beide soorten, de guard-eis blijft op objectcreaties.
Verificatievangsten (2 opus-lenzen), verwerkt vóór de merge: (1) de
"termination is structural"-claim spande met refs vaste pass-budget —
de WF-033-tekst legt de budgetgrens nu eerlijk vast (een sluiting van
~100+ hops fault in ref waar clingo haar grondt; zelfde familie als de
objectcreatie-generatiegrens; een rood-hek voor die grens vergt een
100+-instantieketen en is een vastgelegd toekomstpunt); (2) de
verruimde containment-tak was negatief-loos — wf033-no-creation-member
pint hem (recursieve groep met alleen een gewone regel).
