# Wat er van de periodeconditie nog OPEN staat na ch. 3.10

Goal: de twee resterende helften van `RUNTIME-SEMANTICS.md` ch. 3.10 —
de grens-EXPRESSIE-vorm en de clip op een tijdlijndoel — uitvoerbaar maken
of als spec-besluit afsluiten. De scalaire poort zelf is KLAAR: ch. 3.10
normeerde haar al in zes clausules met worked example, en sinds 2026-09-20
voeren alle vier de uitvoerders haar uit (kaart `ref-periodeconditie-arm`,
golden `jutterswet-premievenster`).

**Hersneden 2026-09-20.** Deze kaart heette eerst "de TimeWindow-conditie
normeren" en stelde vier beslispunten. Drie daarvan waren al beslist en de
kaart berustte op een verkeerde lezing: ch. 8.2's "the ch. 3.10 period
condition" wijst naar ch. 3.10 van ditzelfde document (het document citeert
zijn eigen hoofdstukken als `ch. N`), niet naar IR-SPEC ch. 3.10. De
grenzen, de tijdvrije sessie en het temporele doel staan daar alle drie —
inclusief, exclusief, "tot en met" inclusief, een afwezige grens open, een
ongezette evaluatiedatum matcht nooit, een lege grens matcht nooit, en de
clip als het doel een tijdlijn is. Wat overblijft is hieronder.

## (1) De grens-EXPRESSIE

ch. 3.10's v0-mirror-note begrenst wat een grens mag ZIJN: een
datumliteral of een eigen scalair datumattribuut van het onderwerp. WF-042
laat elke datum-expressie toe. Alle vier de uitvoerders weigeren de rest —
`ref` en clingo bij naam met de fase in de zin, de exec-tak met een
transpiler-SKIP, de DRL-tak met een marker die de grens zelf noemt. Rode
getuige: `corpus/rood/periodegrens-parameterdatum` (een datumparameter).

Te beslissen: draagt v1 de volle expressie (dan vervalt de mirror-note en
wordt die rode map een golden), of legt de spec de tweevorm vast als de
norm (dan wordt het een WF-regel met een negatief in plaats van vier
weigeringen)? De eerste vraagt vertaalwerk in clingo, waar een
niet-onderwerpgebonden grens geen val-atoom heeft om aan te hangen.

## (2) De clip op een tijdlijndoel

ch. 3.10's tweede geval is **gespecificeerd en niet gebouwd**: bij een
tijdsafhankelijk doel snijdt het venster de geschreven tijdlijn in plaats
van de regel te poorten. Het hoofdstuk zegt "`ref/` is to implement them
first". Vandaag weigeren beide OCaml-uitvoerders de vorm bij naam, en met
opzet BIJ DE REGEL — de gevalsonderscheiding hangt aan het doel en een
conditie ziet er geen — zodat de scalaire poort nooit per ongeluk op een
tijdlijn wordt toegepast. Elke uitvoerder pint die weigering in zijn eigen
suite.

Te bouwen: `written(tl, window) = tl restricted to [vanaf, tot)` in `ref`,
daarna clingo, met een golden. Let op de aanpalende OPEN beslissing die
ch. 3.12 registreert: een geldigheidsvenster op een temporele schrijving
mengt de versie-as met de tijdlijn-as, en dat is de één-as-beslissing van
ch. 8 (PLAN §9, besluit 2). De clip zelf hangt daar niet aan, maar wie
beide tegelijk aanraakt wel.

## Samenhang

`backlog/new/20260920-drl-periode-zink.md` gaat over `GeheelPeriode` — de
§8.2-heelperiode-KWALIFICATIE op een feitcreatie-guard, waar de DRL-tak op
een compile-fout zinkt. Dat is een andere wireknoop en een andere positie
dan deze conditie; de kaarten delen alleen het woord "periode". Ze mogen
gebundeld worden, maar dan als bewuste keuze.

De statustabel van ch. 3.12 draagt sinds 2026-09-20 drie rijen voor deze
familie: de scalaire poort (uitgevoerd), de clip (gespecificeerd, niet
uitgevoerd) en de grensvorm (OPEN, backend-rijpheid).

## Acceptance (bij oppakken aanscherpen)

- (a) Per helft een besluit met reden, en de spec-tekst die het draagt
  (**voorstel**).
- (b) Wat uitvoerbaar wordt, wordt uitgevoerd in `ref` én clingo, met een
  golden met handberekende absoluten; wat geweigerd blijft, houdt zijn
  benoemde arm mét fase-staart en zijn getuige.
- (c) De lijsten bewegen mee: gate 8's Drools-uitsluitingsregel, de
  ch. 3.12-rijen en de gate-12-pins bij een kanaalwissel.
- (d) Alle veertien gates groen.

May touch: `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md`
(**voorstel**), `ref/**`, `backends/clingo/**`, `corpus/**`,
`scripts/check-rood.sh` en `scripts/check-drools.sh` (**voorstel**, de
lijsten en de pins).
