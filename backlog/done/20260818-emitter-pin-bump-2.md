# De tweede emitter-pin-bump: navigatie, scope en de symbolische feitcreatievelden

Goal: de drie anchor-pioneered extensiefamilies worden emitter-gedragen.
In regelspraak-java leert de emitter: (1) `navigatieBindingen` op de
genavigeerde scalar-AttribuutRef (by-name, keten, by-pair) en
`navigatieBinding` op de genavigeerde kenmerkcheck — uitsluitend waar de
binding uniek afleidbaar is uit gedeclareerde namen, en NOOIT op een
fold-bron (het `bronRolBinding`-model; de fold-bron-uitzondering staat
sinds deze kaart normatief in IR-SPEC ch. 4.1a); (2) `scopeRol` op de
parent-scoped uniciteit; (3) de symbolische feitcreatievelden
(`feittype`/`ankerRol`/`doelRol`/`joinRol`/`tussenstappen[].rolBinding`)
afgeleid uit de bean-velden. Absent waar niet uniek — nooit null, nooit
een geraden vorm. Daarna in uchar: pin-bump naar de nieuwe
toolchain-commit, her-emit van de emitter-goldens (byte-diff puur
additief), minstens één nieuwe emitter-golden die de nieuwe emissie
draagt, pin-notitie in § Method en de CONSTRUCTS-rijen.

Acceptance: alle acht uchar-gates groen; de toolchain-gates groen; de
her-geëmitte goldens verschillen uitsluitend additief (de nieuwe
extensievelden); de nieuwe emitter-golden draait door beide executors
waar het construct uitvoerbaar is.

May touch: spec/IR-SPEC.md (§ Method-pin-notitie, ch. 4.1a
emitter-discipline), spec/CONSTRUCTS.md, corpus/golden/ (her-emits +
nieuwe emitter-golden), backlog/; en in regelspraak-java: de
emitter/resolver plus zijn eigen tests, volgens de gate-set daar.

Out of scope: v1-formaatwijzigingen; verifier/executor-wijzigingen in
uchar (de acceptatiekant ligt er al); TemporalRead/defaultLadders-emissie
(geen oppervlaktesyntax in de toolchain).

Result: geland zoals gesneden. Werkpin nu toolchain fe46e49: de emitter
produceert de vier symbolische families — navigatieBindingen op de
scalar genavigeerde lezing (nooit op fold-bronnen, per de ch. 4.1a
emitter-discipline die deze kaart normatief vastlegde),
navigatieBinding op de genavigeerde kenmerkcheck, scopeRol op de
parent-scoped uniciteit, en het heel-of-niet feitcreatieblok — elk
alleen waar uniek afleidbaar uit gedeclareerde namen. De vijf bestaande
emitter-goldens her-emitten byte-identiek (de bump is puur additief);
de nieuwe emitter-golden jutterswet-postwacht draagt alle vier families
en draait door beide executors (navigatie 15/7, de genavigeerde check,
folds 2/10, de gecreëerde vakbeseining-relatie teruggeteld als 2).

Verificatievangsten (2 opus-lenzen), gefixt vóór de merge: (1) een
value-comparison-fold-filter kreeg een spurious by-pair-binding op de
element-eigen lezing (huidigSubject bleef het buitensubject) — het
filter emit nu met het elementtype als subject; inert in het corpus
(ref weigert filters; de verifier wees de vorm onafhankelijk af) maar
latent bij toekomstige filter-implementatie; (2) de vier families
hadden nul emitter-outputtest in de toolchain — IrEmitterTest (6 tests,
incl. het fold-bron-verbod en het gefixte filter) dekt de wire-vormen
nu regressievast; toolchain-gates 943/0/0 groen. Gerecord voor
vervolg: de huidigSubject-threading in creatie-contexten laat bronRol
nu ook dáár emitten (semantisch correct, door de verifier PASS
bevonden, maar zonder corpus-golden met een fold in een
creatie-toekenning — kandidaat bij de volgende corpus-kaart); de
verifier-wandeling van fold-filters vanaf het buitensubject (WF-053
to-many-melding op element-eigen filterlezingen) is een bestaande,
aparte kwestie die met de filter-implementatiekaart moet landen.
TemporalRead en de ladders blijven anchor-pioneered (geen
oppervlaktesyntax). De toolchain-commits (144d517, 31f630a, 1f0d6ef,
fe46e49) staan lokaal op main daar; pushen is aan de maintainer.
