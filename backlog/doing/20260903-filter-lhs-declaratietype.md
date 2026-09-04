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

**Het beslissende flankgeval.** Wegwerpcasus buiten de repo met een
tempo-eenheid, `het totaal van het tarief van alle vondsten … die groter
zijn dan 5 €/mnd`, waar aggregaattype en declaratie aantoonbaar
verschillen:

```
functie.type      : Numeriek … eenheid €          (noemer gestript, §4.20)
argumenten[0].type: Numeriek … eenheid €/mnd
filter.links.type : Numeriek … eenheid €/mnd      ← byte-gelijk aan
Vondst.attributen.tarief : Numeriek … eenheid €/mnd   de declaratie
```

De plek neemt dus het declaratietype, niet het aggregaattype.

**Meting op DEZE bouwpositie** (tijdelijke sonde op de plek, daarna
verwijderd; 73 `.rgs`-bronnen over `corpus/golden` (26), `corpus/rood`
(0) en de pariteitszone (47)): TWEE filter-LHS-plekken uit deze
bouwpositie, beide `Vondst.gewicht` (`jutterswet-strandgoed`,
`jutterswet-drempelvangst`), nul afwijkingen — en sterker dan
gelijkheid: `agg.type() == elem.attribuut(agg.attribuut())` is
OBJECT-identiteit, hetzelfde `Datatype`-exemplaar dat de declaratiemap
draagt. Idem op vier wegwerpflanken buiten de repo (eenheid+decimalen,
`€/mnd`-tempo, de `" in <eenheid>"`-strip, de meervoudsherbinding).

**Die twee zijn NIET de telling van het wire-pad.** `…/waarde/filter/links`
heeft corpusbreed VIER bezetters (`corpus/golden` + `corpus/rood` +
pariteitszone; de laatste twee dragen er geen):

| golden | bouwer | `links.onderwerpBron` | `bron.rgs` |
| --- | --- | --- | --- |
| `jutterswet-drempelvangst` | deze plek (`SOM`-filter) | `""` (kopie) | ja |
| `jutterswet-strandgoed` | deze plek (`SOM`-filter) | `""` (kopie) | ja |
| `jutterswet-getijdenwacht` | `gedurendeVergelijking:764-772` | `"zijn"` | ja |
| `synth2-clip-cond-over-prorata` | `gedurendeVergelijking:764-772` | `"zijn"` | nee |

De twee `gedurende`-clips zijn `TOTAAL`-knopen waarvan de LHS gewoon door
`bouw()` wordt opgelost — geen kopie, geen synthese; de laatste is een
handgeschreven golden zonder `bron.rgs` en komt dus nooit langs gate 10.
"Twee" is dus de telling van deze bouwpositie, niet van het pad.

**Ze KUNNEN wél verschillen — twee ontsnappingsroutes.** De eerste versie
van deze notitie en van het gelande commentaar beweerden dat élke
`AttribuutRef` die de plek haalt getypeerd is door een
`ot.attribuut(…)`-declaratielookup, en dat een onoplosbare foldbron al bij
de set-wachter `Onopgelost` wordt. Beide beweringen zijn onwaar. De twee
wachters op `:1038-1039` (`agg.objecttype() == elem`,
`agg.navigatieRol() == null`) sluiten inderdaad de navigatie-, keten- en
rolbouwers uit, maar het `onbekend()`-vangnet op `:1642` haalt de plek
lángs twee routes wél:

*Route 1 — een KENMERKnaam glipt door de set-wachter.* De wachter in
`bouwFunctie:474-482` slaat alleen aan bij
`!sa.objecttype().heeftLid(sa.attribuut())`, en `heeftLid`
(`RegelSpraakModel.java:620`) is `attribuut(naam) != null ||
kenmerken.anyMatch(…)`. Een kenmerknaam IS dus een lid: de wachter zwijgt,
`ot.attribuut(naam)` geeft `null`, en `:1642` synthetiseert een VERS
`onbekend()`. Gereproduceerd op "de som van het waardevol van alle
vondsten van de Jutter die groter zijn dan 5", met `waardevol` als
kenmerk (niet attribuut) van `Vondst`: de emitter schrijft nul
diagnostics en zet op `…/waarde/filter/links` een `AttribuutRef` met
`"type": {"node": "Onbekend", "reden": "niet opgelost"}` — een filter-LHS
zonder declaratie, uit deze plek. De verifier verwerpt hem:

```
WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type sentinel inside a structured node
WF-036 FAIL /resolvedRegels/0/waarde/filter/links: ordering operand is unknown
WF-034 FAIL /resolvedRegels/0/waarde/filter/links/type: unknown-type sentinel inside a structured node
WF-080 FAIL /resolvedRegels/0/waarde/filter/links: 'waardevol' is a characteristic of 'Vondst', not an attribute — a characteristic endpoint is ill-formed in a value position
WF-080 FAIL /resolvedRegels/0/waarde/argumenten/0: 'waardevol' is a characteristic of 'Vondst', not an attribute — a characteristic endpoint is ill-formed in an aggregation source position
```

*Route 2 — bij `AANTAL` is `set == false`, dus vuurt de set-wachter
helemaal niet.* De wachter eist `sa.set()`; een telling over een
bare-definite onderwerp levert een operand met `set: false`, en dan is de
hele wachter buiten beeld. Gereproduceerd op "het aantal strandmaten van
de Jutter die groter zijn dan 5" over een wederkerige zelfrelatie
(`strandmaten` is een ROLNAAM, geen attribuut van `Jutter`): opnieuw nul
diagnostics en een `Onbekend`-dragende filter-LHS. De verifier verwerpt
hem, hier op het `filter/links`-pad zelf:

```
WF-036 FAIL /resolvedRegels/0/waarde/filter/links: ordering operand is unknown
WF-034 FAIL /resolvedRegels/0/waarde/filter/links/type: unknown-type sentinel inside a structured node
WF-034 FAIL /resolvedRegels/0/waarde/filter/links: 'strandmaten' is neither an attribute nor a characteristic of 'Jutter'
```

**Wat dit wel en niet betekent.** De verifier (gate 3) vangt beide routes
hard, dus geen golden kan de vorm dragen: er staat geen onwaar artefact in
de repo, en de gelande keuze zelf is niet geraakt — de kopie neemt het
type van de bron over, en als die bron typeloos is, is dat de FOUT VAN DE
BRON; de verifier benoemt hem op de kopie (route 2: alléén daar) en waar
hij kan ook op de bron zelf (route 1). De schade was een onwaar
commentaar. Dat is nu verwijderd: het commentaar claimt niet langer een
wachterketen die dit uitsluit, maar alleen wat waar is — de plek kopieert
en voegt niets toe.

## Keuze (2026-09-04): gemotiveerde equivalentieverklaring, geen herschrijving

Uitkomst (a) van de kaart. De plek (nu `ExpressieBouwer:1066`)
SYNTHETISEERT geen referentie, hij KOPIEERT er een: de filter-LHS is de
foldbron, herankerd op het element
(alleen `onderwerpBron`/`bron` gereset), met de drieslag
`(objecttype, attribuut, type)` letterlijk overgenomen. Een kopie kan
geen afwijking introduceren die de bron niet al heeft, en de verifier
kruist hem op zijn eigen gebruiksplek als elke andere referentie.

**De kopie is het STRENGERE kanaal, niet een overbodig extra.** De
foldbron-arm (`checks.ml:2769`) en de `filter/links`-arm van `walk_expr`
draaien allebei `use_site_identity` met dezelfde
`resolved_attr_datatype` — maar `use_site_identity` DEFEREERT
(`checks.ml:1225`: `None, _ | _, None | Some (Datatype_onbekend _), _ |
_, Some (Datatype_onbekend _) -> []`) zodra de declaratie onoplosbaar is
of een sentinel in het spel is. Bij een ROLNAAM-foldbron is dat precies
wat er gebeurt: de foldbron-arm zwijgt volledig, en de kopie is het
ENIGE kanaal dat nog vuurt — route 2 hierboven laat het zien, waar
`…/argumenten/0` géén bevinding oplevert en `…/filter/links` er drie
draagt (WF-036 + tweemaal WF-034). De eerdere formulering ("de plek is
geen extra WF-091-kanaal, de bron is al gekruist") had het dus omgedraaid:
de bron is NIET altijd gekruist, en juist daarom pleit de kopie-keuze
voor zichzelf — ze zet één extra, onafhankelijk gekruiste getuige van
dezelfde lezing op de wire.

Een herlookup zou bovendien niet veiliger zijn maar minder: hij zou dit
filterterm en de foldbron van de accumulate uit elkaar kunnen laten
lopen (declaratietype hier, sentinel daar), terwijl beide back-ends
juist eisen dat het ÉÉN lezing van ÉÉN attribuut is. Geland is daarom
een commentaar op de plek zelf met alleen het waarom: dat de plek
kopieert en niets toevoegt, en waarom niet het aggregaattype.

## Gates (2026-09-04)

Gate 10 PASS — JUnit 159 tests, 26 rgs-goldens byte-identiek (nul
bewegende bytes; de wijziging is commentaar-only). Gate 11 PASS — 45
`.rgs` byte-identiek (twee bekende holdouts), suite 27 tests, 29
replays door ref/clingo/Drools zonder divergentie. Gate 2 PASS — 172
bestanden valide + round-trip. Gate 1 PASS.

Na de fixronde (commentaar herschreven, notitie gecorrigeerd) opnieuw:
gate 10 PASS (159 tests, 26 rgs-goldens byte-identiek), gate 2 PASS (172
bestanden), gate 1 PASS. De diff over `frontends/regelspraak/src` is nog
steeds commentaar-only: de diff zonder `//`-regels is leeg.

Eén afwijking onderweg: gate 1 struikelde eerst over het woord voor de
pariteitszone in het nieuwe commentaar (de rechtentripwire vuurt op elke
artefactzone buiten die zone zelf); dat noopte tot een omweg via de
gatenamen, en na de fixronde noemt het commentaar noch de zone noch de
gates — het draagt geen meting meer.

**Testvraag aan de maintainer.** Als een nieuw frontend-testbestand
mocht: één resolvertest op de `totaal van <€/mnd> … die …`-vorm die pint
dat `filter.links.type` de eenheid MET noemer draagt (`€/mnd`) en niet
het aggregaattype (`€`). Dat is de enige vorm waarin de twee kandidaten
uit elkaar liggen; het corpus draagt hem niet (beide bestaande
filter-LHS-getuigen zijn eenheidloze gehele getallen), dus de
equivalentieverklaring is nu door commentaar en meting gedekt, niet door
een artefact.
