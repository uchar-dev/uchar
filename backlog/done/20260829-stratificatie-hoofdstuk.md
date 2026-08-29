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

Result: IR-SPEC ch. 5 "Stratification" vult het §4→§6-nummeringsgat —
normatieve graafdefinities (vijf knoopsoorten; drie kantsoorten incl. de
WF-051-laddervouw; WF-045/046 als cyclusregels), dagsoorten expliciet als
stratum 0 buiten de graaf, en de executor-neutraliteitsclausule (smallere
stratifier mét luide per-vorm-weigering is conform; Drools als de
geratificeerde begrensde instantie — de lens toetste dat de formulering
géén stille miswaarde legitimeert). WF-031 draagt de puurheidsclausule
(gesloten toelatingslijst; de recursie-OPEN gesloten, LADDER-EVOLUTIE
Q8), WF-085 (nieuw) eist acycliciteit van de dagsoort-verwijzingsgraaf —
luid waar de toolchain stil false-in-cyclus is. Getuigen rood-eerst tegen
de oude verifier: wf031-dagsoort-leest-kenmerk,
wf085-dagsoort-verwijzingscyclus, en golden jutterswet-schouwkalender
(acyclische verwijzing, mét case die door het volle vierkant speelt —
gate 12 played 67). No-drift dubbel bewezen: additive diff én
byte-identieke verifier-uitvoer over alle 319 pre-existente documenten.
Lensvangsten verwerkt vóór de merge: (1) het proza beloofde een gesloten
lijst maar de check vlagde alleen attribuut/kenmerk-reads —
Rollidmaatschap, Verre_rollidmaatschap en Regelversie nu ook geweigerd,
met rood-eerst-negatief wf031-dagsoort-leest-rolstaat (gate 3 faalde
aantoonbaar zonder de fix; 166 negatiefs na); (2) twee pre-existente
ongekwalificeerde ch.-5-terugverwijzingen die met het nieuwe hoofdstuk
gingen botsen zijn gekwalificeerd naar RUNTIME-SEMANTICS. Bewust
gelaten: dagsoort-predicaten blijven onuitvoerbaar in elke executor;
roadmap-hernummering (Time 5→7) mechanisch gevolg van ch. 5. CONSTRUCTS-
delta (diverge-from-v0: v0 checkt noch documenteert) maintainer-serieel.
