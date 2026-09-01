# Drools: de Let-node dragen (fase 1 van het Let-besluit)

Goal: De Drools-backend leert de `Let`-expressienode, zodat de
Let-dragende goldens spelen. Achtergrond (onderzoek 2026-09-01, officiële
RegelSpraak-spec v2.3.0 — read-only referentie op het
uwv/regelspraak-werkbank-pad; §-verwijzingen mogen, excerpten nooit): de
Let is GEEN Catala-luxe — RegelSpraak §11 (Variabelendeel, "Daarbij
geldt:") is er de directe tegenhanger van, en onze eigen
`spec/CONSTRUCTS.md` rij 24 draagt hem als core met Origin §13.4.2.10.
Dat de backend hem nooit ziet komt doordat de eigen frontend het
variabelendeel weg-inlinet (ExpressieBouwer, substitutie per
gebruiksplek) — dát wordt fase 2 (de spec-first-kaart
20260901-let-lowering-spec-first). Deze kaart is puur backend-dekking
van de bestaande wire-vorm.

Maintainerbesluit 2026-09-01 (geratificeerd): de Let-route is gefaseerd —
eerst deze backend-kaart, daarna de frontend-lowering spec-first; de
evaluatiesemantiek wordt normatief "op gebruiksplek" (§11.1-conform), te
verankeren in fase 2. Voor DEZE kaart geldt: Drools ≡ ref/clingo over de
bestaande Let-goldens — ref is de scheidsrechter over wat de huidige
pins eisen; elke gepinde verwachting eerst door ref.

LET OP de weigeringsattributie: de loader stopt bij de eerste onbekende
vorm, dus de "4 Let-goldens" van de gate-telling zijn een ondergrens —
na deze kaart kunnen goldens die Let mét een andere v1-vorm dragen naar
de vólgende weigering doorschuiven (synth3-letbeslag → SumType,
jutterswet-kinderlet → ListType). Rapporteer de verschuivingen expliciet.

Acceptance:
- Rood-eerst: nulmeting (gate 12 = played 94 / auto-excluded 42 /
  value-divergence 1 / no-case 1, JUnit 99) en de Let-weigeringen
  getoond; erna de nieuwe telling met de verschuivingen benoemd.
- `bash scripts/check-drools.sh` → PASS; elke Let-dragende golden speelt
  tegen ongewijzigde pins óf staat reden-dragend gelijst (een échte
  divergentie → EXCLUDE_KNOWN of genummerde vraag).
- Nieuwe witnesses (nieuwe testbestanden; bestaande niet wijzigen; de
  publieke replay-API-naam niet noemen) pinnen: de binding zelf, herbruik
  van de gebonden naam, en het gedrag dat ref op de bestaande goldens
  toont — geen eigen semantiekkeuzes vooruitlopend op fase 2.
- Alle twaalf gates groen; gates 5/6 ongewijzigd.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `scripts/check-drools.sh` (uitsluitend
entries/lijsten), `backlog/`.

Out of scope: de frontend-lowering en de emitter (fase 2), elke
spec-tekst, ref/clingo/verifier, corpusverwachtingen en -pins, de overige
v1-vormen (ListType/StructType/SumType/functionDefs/TemporalRead).

## Resultaat (bouwgolf 2026-09-01)

Gate 12: **played 94 → 98, auto-excluded 42 → 38, JUnit 99 → 111**;
value-divergence 1 (kustwacht) en no-case 1 ongewijzigd, de overige
weigeringsregels letterlijk gelijk. De regel `4x … Expr:Let` is weg.

GEEN verschuivingen: de vier andere Let-dragende goldens weigeren
onveranderd op hun eigen eerste vorm (letbeslag → `Datatype:SumType`,
kinderlet/postenheffing → `Datatype:ListType`, kamerhuishouden-catala →
`functionDefs`) — de datatype-/sectieweigering valt in het
declaratiedeel, dus vóór `resolvedRegels`. Zo blijven ListType 13,
StructType 6, SumType 5 en functionDefs 3 exact staan.

Vorm: `Let`/`BinderRef`/`IfThenElse` laden naar een `Expr.Onopgelost`
DRAGER plus een identity-zijkanaal (`IrLoader.Loaded.letVormen`,
`loader/LetVorm.java`) — hetzelfde patroon als `rolBindingen`, want het
`Expr`-record is sealed en gevendord. De binder-omgeving rijdt mee in de
globals-map die elke `WaardeFn` al draadt: per evaluatie een KOPIE met
één extra sleuf (`"binder <naam>"`, met spatie — `Namen.ident` kan die
nooit produceren), dus zuiver functioneel, geen cel en geen ThreadLocal.
Daardoor erft de Let-body de bestaande leegwaarde-tabellen, de §4
eenheidsherwaardering en de Breuk-bewuste `rs*`-helpers ongewijzigd, en
profiteert het defaults-pad automatisch mee.

Vier generieke Expr-wandelingen leren door de drager heen kijken
(`letKinderen`): de freshness-watch (`eigenReads`), de
verre-zijde-reactiviteit (`navRefs`), de defaults-salience
(`exprLeesnamen`) en het defaults-delingshek (`bevatDeling`). Alleen de
eerste is corpus-observeerbaar gemaakt — met een eigen witness waarin
stratificatie de producent ná de consument laat vuren.

## Fixronde na lens 1 (2026-09-01)

Gate 12 blijft **played 98 / auto-excluded 38 / value-divergence 1 /
no-case 1**; JUnit **111 → 118**, nul skipped. Gates 1 t/m 11 groen.

Drie wandelingen bleken nog drager-blind, en elk gat is met een
ref-differentieel bewezen vóór de fix: `verzamelDelingen` (§6.5) en
`verzamelWortels` (§6.6) verloren de fout-companion achter een Let —
Drools zei stil *consistent* waar ref *inconsistent* zegt — en
`kanBreukOpleveren` liet een Breuk achter een Let op de harde
`(BigDecimal)`-cast crashen. Alle drie dalen nu als eerste stap via
`letKinderen` af.

`startpuntCompanions` (§6.8) daalt bewust **niet** af. Gemeten op een
twee-flank-paar (leeg startpunt-anker, kaal en achter een Let): ref zegt
op beide kanten *consistent* — ref kent de leeg-anker-fout helemaal
niet, die is productie-RegelSpraak-semantiek die deze port draagt. De
kale flank divergeert dus al (Drools inconsistent), en juist de
drager-blindheid laat de omhulde flank mét ref meelopen. Afdalen zou een
ref-divergentie tóevoegen. De §6.8-vs-ref-vraag is een spec-kwestie,
buiten deze kaart; geen enkele corpus-golden schrijft een startpunt-lid
per regel.

`letKinderen` is losgekoppeld van `letConditie` (besluit hoofdsessie):
een nieuwe `conditieOperanden` daalt af in de Expr-operanden van álle
conditievormen. Dat is veilig omdat een conditievorm die `letConditie`
weigert de hele regel lúid laat vallen — niet omdat "een extra fout
onschuldig" zou zijn.

Nieuw `LetDragerWandelingWitnessTest` (7 witnesses) pint wat de drager
*verbergt* — de twee construeerbare hekken als luide weigering, de twee
al gebouwde wandelingen, en de drie nieuwe fixes naast hun kale
tweelingen. Elk model is verifier-clean en elke gepinde waarde is eerst
door ref gedraaid; elke witness is mutant-rood bewezen in een
/tmp-kopie.

## Fixronde na lens 2 (2026-09-01)

Gate 12 blijft **played 98 / auto-excluded 38 / value-divergence 1 /
no-case 1**; JUnit **118 → 120**, nul skipped. Alle twaalf gates groen.

Lens 1's fix over-corrigeerde. `verzamelDelingen` en `verzamelWortels`
daalden af via het volle `letKinderen`, dat voor een Keuze BEIDE takken
en voor een Binding óók de body levert — paden die per ch. 10.2 / ch.
10.6 niet altijd draaien. Zij bouwen §6.5/§6.6-**fout-companions**, en
een companion op een pad dat nooit draait is INCONSISTENT waar ref
consistent zegt: een fout antwoord, geen behoedzame weigering. Nieuw
`letKinderenZeker` levert alleen de zeker geëvalueerde kinderen (Binding
→ value; Keuze → de conditie-operanden; Lezing → niets) en exact die
twee wandelingen gebruiken het. De vier andere blijven op het volle
`letKinderen`: hun uitkomst is een weigering of een registratie, en daar
is meer-zien nog steeds een correct antwoord. Twee nieuwe witnesses
(beide ref-bevestigd, beide mutant-rood met de strikte accessor
teruggezet) pinnen het.

**De restpost, gemeten en bewust geaccepteerd.** Een companion is een
losse statische regel op de leegte van de operanden; hij kan niet testen
wélke tak draaide. Dus krijgt een §6.5/§6.6-fout in een tak die WEL
genomen wordt, of in een body waarvan de value WEL gevuld is, evenmin
een companion: op `Let d = deler in inkomen gedeeld door tweede` met
`deler` = 2,00 en `tweede` leeg zegt ref *inconsistent* en Drools
*consistent* (vóór deze fix zei Drools daar toevallig goed). Beide
richtingen zijn fout op een andere vorm; nooit-verzinnen is de gekozen
kant. Een companion die ook de barrière/tak-conditie meedraagt is de
echte oplossing — eigen kaart, geen corpus-golden draagt de vorm.

De opsomming op `LetVorm` is herschreven in drie gecheckte categorieën
(via `letKinderen` / via `letKinderenZeker` / gemotiveerd niet), met
`heeftTijdgebonden`, `parityOnveilig`, `containsAggregatie`,
`isDatumRekenExpr`, `lowerVal`, `companionOperand` en
`Stratificatie.verzamelReads` erbij naast `startpuntCompanions`. De
loader controleert nu de verplichte velden van Let/BinderRef/IfThenElse
(luide Refusal met vorm + veld, in plaats van een naamloze NPE of — bij
een BinderRef zonder type — een stil doorspelende drager), en de twee
aanroeperloze `transpile`-overloads zijn weg.

## Afronding (hoofdsessie, 2026-09-01)

Eindstand na bouwgolf + twee lensrondes + twee fixrondes: **gate 12
played 98 / auto-excluded 38 / value-divergence 1 (kustwacht) / no-case 1,
JUnit 120, nul skipped; alle twaalf gates groen.** De vier nieuw spelende
goldens: `synth3-drempelheffing`, `jutterswet-tariefrecht-catala`,
`jutterswet-kinderweging`, `anchor/jutterswet-tariefstelling`.

Twee kaartaannames bleken onwaar en zijn hier gecorrigeerd:
(1) de voorspelde verschuivingen (letbeslag → SumType, kinderlet →
ListType) waren al de **nulmeting** — objecttypes parseren vóór
`resolvedRegels`, dus die goldens weigerden nooit op `Expr:Let`;
(2) `jutterswet-tariefrecht-catala` draagt zijn Lets in
`resolvedRegels[0].waarde`, niet in `prioritizedDefaults`-waarden — dat
het defaults-pad meelift is met een zelfgebouwd witness-drietal bewezen,
niet met een corpus-golden.

Bekende grenzen, expliciet gelaten (elk luid of ongepind, nooit stil
gepind-fout): een Let in conditie-/patroonpositie en een Let over een
aggregatie-/temporele waarde weigeren luid ("waarde buiten scope" —
relevant voor fase 2, zie de bijgewerkte lowering-kaart); de
§6.5/§6.6-companion kent geen tak-/barrièreconditie (restpost hierboven —
vervolgkaart `20260901-companion-onder-drager`); `startpuntCompanions`
blijft bewust drager-blind (§6.8 is productie-semantiek die ref niet
kent — de kale flank divergeert vandaag al); de pre-existente
eenheden-ongecontroleerde referentie (lens-1-flank, ref 550 vs Drools
500.05 op een verifier-clean model) kreeg vervolgkaart
`20260901-eenheden-op-refs`.
