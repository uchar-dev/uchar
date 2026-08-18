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
