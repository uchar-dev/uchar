# Aggregatie-filters: de die/dat-subselectie executeert

Goal: de gefilterde fold — "de som/het aantal van ... die <conditie>" —
wordt uitvoerbaar in beide executors en correct geverifieerd. De
filterconditie evalueert PER ELEMENT (het contributor-subject, niet het
buitensubject): ref filtert de contributor-lijst met eval_cond op de
contributor-instantie; clingo voegt de filter-atomen over de
contributor-variabele toe binnen de aggregaat-conditie (een leeg
filter-operand sluit de contributor uit via de unbound join — ch. 1.2d
gratis); de verifier-wandeling en dep_cond nemen het elementtype als
subject voor het filter (de vastgelegde buitensubject-kwestie uit de
pin-bump-verificatie). Een anchor pint gefilterde som en aantal
absoluut, inclusief de leeg-uitsluiting en een kenmerk-filter; de
emitter produceert filters al (de gefixte element-subject-emissie van
pin fe46e49 krijgt zo zijn uitvoerende getuige).

Acceptance: alle acht gates groen; de anchor draait gefilterde folds
met absolute waarden door ref én clingo; de verifier accepteert
element-eigen filterlezingen (geen onterechte WF-053-to-many) en blijft
genavigeerde/foute filtervormen afwijzen; CONSTRUCTS-notitie
bijgewerkt.

May touch: spec/verifier/lib/checks.ml, ref/lib/eval.ml,
backends/clingo/lib/translate.ml, spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/CONSTRUCTS.md,
corpus/golden/ (anchor of emitter-golden), corpus/negatief/ (indien een
nieuwe clausule dat vraagt), backlog/.

Out of scope: temporele (gedurende-)filters — v0-marker, blijft fault;
dimensieSelectie; filters op verdeling/creatie-vormen buiten wat v0
emit.

Result: geland zoals gesneden. De die/dat-subselectie evalueert per
contributor in beide executors (ref: eval_cond op de element-instantie,
in de recursieve keten gevoegd; clingo: filteratomen over de
element-variabele in #count/#sum — de leeg-uitsluiting gratis via de
join); de verifier en de dep-graaf wandelen het filter met het
elementtype als subject (de vastgelegde buitensubject-kwestie
gedicht — een filter over een afgeleid kenmerk ordent boven zijn
schrijver, door de anchor getuigd); de gedurende-tijdlijnclip die
hetzelfde wire-veld deelt wordt in beide executors geweigerd, nooit
als subselectie uitgevoerd. Emitter-golden jutterswet-strandgoed
(bron.rgs via pin fe46e49): vergelijkingsfilter 19 (7+12; leeg en
klein vallen uit), afgeleid-kenmerk-telling 1, en de
alles-uitsluitende-filter-getuige (J3: fallback 0 en telling 0 door
filteruitsluiting, naast J2's lege-rol-route).

Verificatievangsten, verwerkt vóór de merge: (1) MAJOR — het
CONSTRUCTS-acceptatiecriterium was niet geleverd — rij 1 draagt nu de
Landed-notitie en RUNTIME-SEMANTICS ch. 3.4a de normatieve clausule
met het worked example; (2) de alles-uitsluitende filter deelde zijn
getuige met de lege-rol-route — J3 pint hem onderscheidend; (3) de
IR-SPEC-zin nuanceert de executor-asymmetrie: ref draagt de volledige
conditieset, clingo's corpus-fragment fault luid op de rest (bestaand
beleid — de referentie-interpreter is de executable spec en wordt niet
tot clingo's fragment verminkt); (4) de gedurende-weigering is
gerecord-unwitnessed tot de clip-kaart (rood-hek daar). Eén lens
rapporteerde tijdelijke debug-scaffolding in de worktree tijdens de
review (door reviewer-experimenten; teruggedraaid) — de merge-staat is
geverifieerd schoon met de subject-fix intact.
