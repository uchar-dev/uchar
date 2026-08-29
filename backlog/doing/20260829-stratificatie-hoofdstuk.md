# Stratificatie-hoofdstuk: IR-SPEC §5 + de dagsoort-WF

Goal: het normatieve stratificatiehoofdstuk van LADDER-EVOLUTIE §7 /
Q7+Q8 (geratificeerd 2026-08-29). Een nieuw IR-SPEC §5 "Stratification"
(het nummeringsgat §4→§6) tilt de afhankelijkheidsgraaf- en
kantsoort-definities uit RUNTIME-SEMANTICS ch. 6 naar normatief niveau
(knopen; positieve/stratum-strikte/negatieve kanten; de WF-051-laddervouw;
de illegale-cyclusregel), waarna WF-045/046/051 náár §5 citeren en ch. 6
het operationele gezelschapsstuk blijft. Inclusief de
executor-neutraliteitsclausule: een backend realiseert óf een correcte
verfijning van de graafstrata, óf een smallere stratifier mét luide
weigering van de ongedekte vormen (de geratificeerde, begrensde
Drools-houding). Plus de dagsoort-beslissing (Q8, optie A met
stratum-0-framing): WF-031 krijgt de puurheidsclausule (een
dagsoort-voorwaarde leest uitsluitend datums, datumcomponenten, eerste
paasdag, literalen, parameters en dagsoort-verwijzingen — nooit attributen
of kenmerken) en een nieuwe acycliciteitscheck op de
dagsoort-verwijzingsgraaf (luid, waar de toolchain stil false-in-cyclus
is); dagsoorten blijven buiten de afhankelijkheidsgraaf. De v0-divergentie
(v0 checkt noch documenteert dit) wordt een CONSTRUCTS-delta (hoofdsessie
serieel).

Acceptance: gate 3 groen mét de nieuwe getuigen — rood-eerst: negatiefs
voor een dagsoort die een kenmerk leest en voor een verwijzingscyclus
(beide eerst rood getoond tegen de oude verifier), en één golden met een
acyclische dagsoort→dagsoort-verwijzing; alle overige gates groen; de
verifier-implementatie in spec/verifier/lib/checks.ml sluit aan op de
§5-tekst (geen tweede graafnotie).

May touch: spec/IR-SPEC.md (§5 nieuw + WF-031/045/046/051-verwijzingen),
spec/RUNTIME-SEMANTICS.md (ch.-6-aanhef die naar §5 wijst),
spec/verifier/ (de twee nieuwe checks), corpus/negatief/ (twee nieuwe),
corpus/golden/ (één nieuwe), backlog/. Beschermde paden: onder het
mergemandaat na gates+lenzen.

Out of scope: dagsoort-evaluatie in ref/clingo bouwen (de predicaten
blijven geweigerd); Drools-stratifier verbreden (de clausule legitimeert de
smalle vorm); CONSTRUCTS/PLAN (hoofdsessie serieel).
