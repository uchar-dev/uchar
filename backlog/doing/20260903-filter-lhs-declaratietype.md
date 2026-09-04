# De ene niet-declaratie-getrouwe emitterplek: de filter-LHS

Goal: gemeten (meetfase 2026-09-03): de RegelSpraak-frontend schrijft
referentietypen per constructie als het declaratieobject, op één plek
na — `ExpressieBouwer.java:1058` geeft de filter-LHS het aggregaattype
(`agg.type()`) in plaats van een herlookup van de declaratie. Vandaag
drift-vrij op het corpus (nul afwijkingen over ruim duizend
referenties), maar het is het enige kanaal waardoor de eigen frontend
een WF-091-strijdige kopie zou kunnen emitteren. Maak de plek
declaratie-getrouw, of verklaar hem aantoonbaar equivalent (met de
meting erbij).

Acceptance:
- De plek herschreven of gemotiveerd equivalent verklaard.
- Gate 10 byte-identiek over alle rgs-goldens — beweegt er een byte,
  dan is de wijziging semantisch en stopt de kaart voor een vraag.
- Alle twaalf gates groen; herpin (frontends geraakt).

May touch: `frontends/regelspraak/src/**`, `backlog/`. Nieuwe
frontend-tests alleen na de testbestanden-vraag aan de maintainer.

Out of scope: verifier, spec, corpus.

## Meting (2026-09-04)

**De premisse van de kaart klopt niet: `agg` is de OPERAND, niet de
aggregatie.** In `elementVergelijking` is `agg` de enige operand
(`argumenten.get(0)`, gebonden onder `argumenten.size() != 1`), dus
`agg.type()` is het OPERANDtype. Het aggregaattype is een ander object —
`functieType(kind, args)`, dat op de `Functie`-knoop landt. Precies daar
zitten de gevreesde afleidingen (§4.20 noemerstrip, min/max-decimalen);
die raken deze plek niet.

**De wire-plek.** Wél geserialiseerd: `IrEmitter` schrijft de filter als
`Conditie.Vergelijking`, dus het pad is
`…/waarde/filter/links/type`. De verifier kruist het daar ook al: de
`Vergelijking`-tak van `walk_cond` (`checks.ml:3724`) loopt `walk_expr`
op `links`, en de `Attribuut_ref`-tak roept `use_site_identity`
(WF-091, `checks.ml:2960`).

**Het beslissende flankgeval.** `/tmp`-casus met een tempo-eenheid,
`het totaal van het tarief van alle vondsten … die groter zijn dan
5 €/mnd`, waar aggregaattype en declaratie aantoonbaar verschillen:

```
functie.type      : Numeriek … eenheid €          (noemer gestript, §4.20)
argumenten[0].type: Numeriek … eenheid €/mnd
filter.links.type : Numeriek … eenheid €/mnd      ← byte-gelijk aan
Vondst.attributen.tarief : Numeriek … eenheid €/mnd   de declaratie
```

De plek neemt dus het declaratietype, niet het aggregaattype.

**Corpusbrede meting** (tijdelijke sonde op de plek, daarna verwijderd;
73 `.rgs`-bronnen over `corpus/golden` (26), `corpus/rood` (0) en de
pariteitszone (47)): TWEE filter-LHS-plekken in totaal, beide
`Vondst.gewicht` (`jutterswet-strandgoed`, `jutterswet-drempelvangst`),
nul afwijkingen — en sterker dan gelijkheid: `agg.type() ==
elem.attribuut(agg.attribuut())` is OBJECT-identiteit, hetzelfde
`Datatype`-exemplaar dat de declaratiemap draagt. Idem op vier
`/tmp`-flanken (eenheid+decimalen, `€/mnd`-tempo, de `" in
<eenheid>"`-strip, de meervoudsherbinding).

**Codebewijs dat ze niet KUNnen verschillen.** De twee wachters op
`:1038-1039` pinnen `agg.objecttype() == elem` (referentiegelijkheid) en
`agg.navigatieRol() == null`. Elke `Expr.AttribuutRef` die daar langs
komt, is getypeerd door een `ot.attribuut(…)`-declaratielookup:
`attribuutRef:1550/1571/1584`, `onderwerpAlsWaarde:1677`,
`possessief:1703/1731`. De bouwers die een type NIET uit de declaratie
halen, dragen alle een `navigatieRol` — `possessief:1715/1726/1745`
(rolnavigatie, type `onbekend()`) en de ketenbouwer `:1634` (invariante
niet-null `navigatieRol`) — en vallen dus buiten de wachters. Een
`DimensieCel` is geen `AttribuutRef` en faalt de `instanceof`. Blijft
over: het `onbekend()`-vangnet op `:1642`. Gemeten dat het de plek niet
haalt — een niet-declaratie-dragende foldbron wordt al bij de
set-wachter in `bouwFunctie:474-482` de hele aggregatie `Onopgelost`
(flank t5: `'lading' is geen … gedeclareerd attribuut van Vondst`, geen
filterknoop geëmitteerd).

## Keuze (2026-09-04): gemotiveerde equivalentieverklaring, geen herschrijving

Uitkomst (a) van de kaart. De plek (nu `ExpressieBouwer:1071`)
SYNTHETISEERT geen referentie, hij KOPIEERT er een: de filter-LHS is de
foldbron, herankerd op het element
(alleen `onderwerpBron`/`bron` gereset), met de drieslag
`(objecttype, attribuut, type)` letterlijk overgenomen. De verifier pint
diezelfde declaratie al op de foldbron zelf — de fold-source-arm op
`argumenten/0/type` (`checks.ml:2769`), met dezelfde
`resolved_attr_datatype` en dus dezelfde declaratie. Een kopie kan geen
afwijking introduceren die de bron niet al heeft, en die bron is
gekruist. De plek is geen extra WF-091-kanaal.

Een herlookup zou bovendien niet veiliger zijn maar minder: hij zou dit
filterterm en de foldbron van de accumulate uit elkaar kunnen laten
lopen (declaratietype hier, sentinel daar), terwijl beide back-ends
juist eisen dat het ÉÉN lezing van ÉÉN attribuut is. Geland is daarom
een commentaar op de plek zelf met het waarom, de wachterketen en de
meting.

## Gates (2026-09-04)

Gate 10 PASS — JUnit 159 tests, 26 rgs-goldens byte-identiek (nul
bewegende bytes; de wijziging is commentaar-only). Gate 11 PASS — 45
`.rgs` byte-identiek (twee bekende holdouts), suite 27 tests, 29
replays door ref/clingo/Drools zonder divergentie. Gate 2 PASS — 172
bestanden valide + round-trip. Gate 1 PASS.

Eén afwijking onderweg: gate 1 struikelde eerst over het woord voor de
pariteitszone in het nieuwe commentaar (de rechtentripwire vuurt op elke
artefactzone buiten die zone zelf); het commentaar noemt nu de gates in
plaats van de zone.

**Testvraag aan de maintainer.** Als een nieuw frontend-testbestand
mocht: één resolvertest op de `totaal van <€/mnd> … die …`-vorm die pint
dat `filter.links.type` de eenheid MET noemer draagt (`€/mnd`) en niet
het aggregaattype (`€`). Dat is de enige vorm waarin de twee kandidaten
uit elkaar liggen; het corpus draagt hem niet (beide bestaande
filter-LHS-getuigen zijn eenheidloze gehele getallen), dus de
equivalentieverklaring is nu door commentaar en meting gedekt, niet door
een artefact.
