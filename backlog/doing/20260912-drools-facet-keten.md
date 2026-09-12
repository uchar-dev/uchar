# drools-facet-keten — de ketensluiting van de Drools-backend

Goal: de reparatiekaart die vervolgpunt A van
`backlog/done/20260912-subtype-frontend-keten.md` uitvoert, met maintainersmandaat
voor route 1 (mechanisch). `backends/drools` leest de facet-ZIJKAARTEN van leden
(granulariteit, tijdlijnaantal, dimensies) op 122 gemeten plekken buiten de loader (121 regels)
rechtstreeks van het gedeclareerde/concrete type-object in plaats van via de
`⊑`-keten. Bij een subtype met een GEËRFD temporeel of gedimensioneerd lid
antwoordt zo'n lezing "niet temporeel" — geen ontbrekende binding maar een
VERKEERD oordeel (een tijdlijn die als scalair wordt gelowerd). Het vangnet
daartegen is een luide per-vorm-weigering in `IrLoader.bindSupertypes`, die de
combinatie subtype × temporeel/gedimensioneerd in zijn geheel afwijst; het
commentaar zegt letterlijk "until those reads are chain-aware". Gevolg: die
documentklasse werkt in ref en clingo maar speelt niet in Drools — een
gedocumenteerd gat in het differentiaalvierkant.

Deze kaart leidt de lezingen door de ketenbewuste accessors op
`RegelSpraakModel.Objecttype`, heft de weigering op, en landt de golden die de
vorige kaart bouwde, mat en weer moest verwijderen: `jutterswet-erfwacht`, de
subtype-tweeling van `jutterswet-getijdenwacht`.

Acceptance:
- rood-eerst: de golden eerst, gemeten op de basis (Drools EXCLUDEt hem per vorm),
  daarna de lezingen, daarna PLAYED;
- elke van de 122 plekken gemeten en geclassificeerd: LEDENLOOKUP (→ accessor) of
  ITERATIE over een eigen-ledenkaart (→ rauw, met de reden ter plekke — een
  iteratie per declarant bezoekt elk lid precies één keer, ketensluiting zou
  dubbeltellen);
- `corpus/golden/jutterswet-erfwacht`: ref = clingo exact (6 × 10 + 1 × 11 = 71),
  no-op weerlegd, PLAYED in beide transpilertakken voor zover die de vormen dragen;
- het vangnet: basis-jar en kop-jar over ALLE goldens-met-cases in BEIDE takken,
  regel voor regel gedift — alleen `jutterswet-erfwacht` mag verschijnen;
- alle veertien gates groen, gate 10 met byte-identieke re-emissie van élke
  rgs-golden (de model-accessors compileren mee in de frontend).

May touch: `backends/drools/`,
`frontends/regelspraak/src/main/java/.../model/RegelSpraakModel.java` en
`.../kern/Tijdrekening.java` (alleen accessor-toevoegingen in de bestaande vorm),
`corpus/golden/jutterswet-erfwacht` (nieuw), `scripts/check-drools.sh`
(`EXPECTED_PLAYED` + de kanaalvloerlijst, elk met reden), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (alleen de gerichte weigering-zinnen), dit
kaartbestand.

Out of scope: `spec/CONSTRUCTS.md` en `spec/PLAN.md` (registervoorstel als
letterlijke tekst in het Result), `CLAUDE.md`, `.github/`, `spec/verifier/`,
`ref/`, `backends/clingo`, de resolver/emitter van `frontends/regelspraak`,
`frontends/flogic`, `frontends/catala`, bestaande corpusbestanden.

Result:

**De meting: 122 voorkomens op 121 regels.** De vorige kaart telde 122, ik telde
121, en het raadsel is door de lens opgelost: `ContractValidator.java:58` draagt
twee lezingen op één regel (`!ot.attribuutGranulariteit.isEmpty() ||
!ot.kenmerkGranulariteit.isEmpty()`). Beide kaarten maten dezelfde commit; de
ene telde voorkomens, de andere regels. Per bestand, in regels:
ExecModelTranspiler 67, DrlTranspiler 43, CaseRunner 5, FactModelGenerator 4,
EffectieveEenheid 1, ContractValidator 1; de loader telt er zelf 12, allemaal de
schrijfkant van de declaratie.

| klasse | n | wat ermee gebeurt |
|---|---|---|
| ledenlookup, één naam direct (`get`/`containsKey`) | 85 | door de ketengesloten kaart |
| ledenlookup, hele kaart aan een naamcontract (`meerPeriodiek` 24, `bevatNaam` 5, `gedeclareerdeNaam` 2) | 31 | door de ketengesloten kaart |
| ITERATIE over een eigen-ledenkaart | 2 | **rauw**, met reden |
| lookup gesleuteld op een naam ÚIT die iteratie | 2 | **rauw**, met reden |
| disjunctie over ALLE objecttypen (2 termen op 1 regel) | 2 | **rauw**, met reden |

116 geleid, 6 rauw, samen 122.

De drie rauwe redenen staan ter plekke. De twee iteraties zijn de
ledenafleiding van de beangenerator: de gegenereerde subklasse `extends` haar
ouder en `leden()` trekt het geërfde er per eigenschapsnaam weer af, dus een
ketengesloten iteratie zou elk geërfd lid één keer per afstammeling bezoeken om
het daarna weer te laten weghalen. De twee opzoekingen erbinnen zijn gesleuteld
op een naam die die iteratie zelf opleverde, dus de dichtstbijzijnde declarant
ís het type zelf (WF-105 weigert schaduwing). De disjunctie van de
contractvalidator loopt over élk objecttype in het model, dus een declarerende
voorouder komt op zijn eigen beurt langs.

**Kaartvorm, niet de waarde-accessors, en dat was een keuze.** De backend
vergelijkt lidnamen met ref's `norm` (lowercase-ASCII plus samengevouwen
witruimte) en het model met `equalsIgnoreCase`; die twee zijn onvergelijkbaar —
`norm` vouwt witruimte, `equalsIgnoreCase` vouwt niet-ASCII kapitaal. Alle 122
lezingen door `opKeten` leiden zou de keten sluiten en in dezelfde bewerking een
naamvergelijkingsverschil openen, precies het soort dat `Namen.norm` in zijn
eigen javadoc als bron van eerdere ellende beschrijft. Dus: één generieke
`opKetenKaart` op `Objecttype` plus vijf dunne varianten
(`attribuutLeden`, `granulariteitLeden`, `tijdlijnAantalLeden`,
`kenmerkGranulariteitLeden`, `kenmerkTijdlijnAantalLeden`), in de vorm die
`gedimensioneerdeLeden` al had — die is nu zelf een variant op de generieke.
Wat elke plek van zijn kaart vráágt is onveranderd; alleen de kaart is
ketengesloten.

**Rood-eerst, in drie gemeten stappen.**

1. De golden gebouwd, dan de basis-jar (main 8bd98e4) erop:
   `EXCLUDE loader: … Objecttype.supertype (a supertype chain carrying a
   temporal or dimensioned member (the facet side maps are read per declared
   type))` — in BEIDE takken.
2. Op de basis de weigering weggehaald en opnieuw gemeten, om te zien wat de
   ketenblindheid werkelijk doet: `EXCLUDE transpiler SKIP: … (aggregatie
   buiten Phase-C scope)` in de exec-tak en `EXCLUDE drl-transpiler MARKER:
   TODO aggregatie TOTAAL` in de DRL-tak. Voor déze golden is de blindheid dus
   LUID — de vouwpaden zijn geklemd op "is temporeel" en zonder die klem
   weigeren ze — niet stil. Dat is de correctie die de vorige kaart ook op
   zichzelf moest maken, en ze geldt hier weer.
3. Na het werk: `PLAYED 1` in beide takken.

**Waar de blindheid wél stil was: één echt gat plus één gedegenereerde
randvorm.** Mijn eerste versie van dit verslag schreef dat BEIDE §9.9-vormen op
de basis stil meespeelden. Nagemeten op verifier-schone invoer klopt dat niet,
en het onderscheid doet ertoe. WF-005 (`checks.ml`) eist dat de
granulariteitsadoptie van een benoemde tijdlijn TOTAAL is: een niet-lege
`attribuutTijdlijnNaam` impliceert een `attribuutGranulariteit` gelijk aan die
van de declaratie. Dus ving de brede weigering — die naar granulariteit keek —
de gewone benoemde-tijdlijnvorm gewoon af. De enige ontsnapping is de
gedegenereerde declaratie met `granulariteit: null`, schemageldig, waarbij
WF-005 None met None vergelijkt en het attribuut geen facet nodig heeft.

Vijf verifier-schone fixtures, elk gemeten op de basis-jar (main 8bd98e4) en op
de kop — dezelfde documenten die de JUnit-getuigen bouwen:

| fixture | verifier | BASIS | KOP | lezing |
|---|---|---|---|---|
| geërfd attribuut, benoemde tijdlijn, **variabel** startpunt | PASS | EXCLUDE (breed) | EXCLUDE (a) | **residu**, niet een nieuw dicht gat |
| idem, gedegenereerd `granulariteit: null` | PASS | **PLAYED 1** | EXCLUDE (a) | **gedegenereerde randvorm**, stil, nu dicht |
| geërfd attribuut, benoemde tijdlijn, **vast** startpunt | PASS | EXCLUDE (breed) | **PLAYED 1** | door de versmalling **vrijgegeven** |
| startpuntbepaling op een **voorouder** | PASS | **PLAYED 1** | EXCLUDE (b) | **het echte stille gat**, nu dicht |
| startpuntbepaling **naast** de keten | PASS | PLAYED 1 | PLAYED 1 | terecht ongemoeid |

De weigering is dus op twee assen versmald en op één as verruimd. Ze weigert
alleen nog waar `heeftVariabelStartpunt` werkelijk blind is: op een
VAST-startpunt-tijdlijn geeft dat predicaat vals uit de eigen kaart én uit de
keten, daar bestaat geen blindheid, en de gedrukte reden ("reads such an
attribute as unanchored") zou er domweg wáár zijn. Die versmalling vergt de
tijdlijnlijst, dus de wacht is verhuisd naar `weigerGeerfdAnker`, naast de
startpuntwacht en ná het lezen van zowel `tijdlijnen` als `resolvedStartpunten`
— `bindSupertypes` draait vóór beide, en dat is precies waarom de grove vorm
ontstond. De klasse is levend: `jutterswet-penningjaar` draagt een benoemde
tijdlijn met `variabelStartpunt: false`.

WF-016 knoopt de twee plekken aan elkaar zonder ze samen te voegen: een
variabel-startpunt-tijdlijn MOET een bepaling hebben, maar niets dwingt die
bepaling het type te noemen dat het attribuut declareert. Vandaar dat (a) alleen
valt als de bepaling elders staat, en (b) alleen als geen attribuut op de
tijdlijn rijdt.

De familie als geheel verbreden blijft wat de vorige kaart erover schreef: geen
ledenlookup maar een protocol, waarvan één van de vier plekken door de keten
leiden de naad alleen verschuift. Dat is vervolgwerk B, nu met vijf gemeten
getuigen erbij.

**De golden `corpus/golden/jutterswet-erfwacht`.** Exact de bron en de
caseafleiding die de vorige kaart bewaarde: `Klipjutter is een Jutter`, het
maandattribuut `wachtvergoeding per maand` en het maandkenmerk `wachtplichtig`
op de VOOROUDER, `wachtsom` op het subtype, en één regel die vanaf een
Klipjutter-subject het geërfde attribuut vouwt over het geërfde kenmerk.
Handberekend: 10 €/maand over 2023, 11 over het eerste halfjaar 2024,
wachtplichtig 2023-07-01 t/m 2024-02-01 = 6 × 10 + 1 × 11 = **71**; `K2` is
nooit wachtplichtig en krijgt **0**. Gemeten: verifier PASS, ref PASS (3
verwachtingen), clingo PASS (3), no-op weerlegd op beide pins, en PLAYED 1 in
béide Drools-takken. De golden draagt geen defaults, dus het ontbreken van de
defaults-lowering in de DRL-tak raakt hem niet — het kanaal is daar hetzelfde
als in de exec-tak.

**Het vangnet.** Basis-jar (main 8bd98e4) en kop-jar over ÉLKE golden, in BEIDE
transpilertakken, op DEZELFDE corpusinhoud (366 regels elk, dus de nieuwe golden
staat aan beide kanten en het rood-eerst zit in dezelfde diff), regel voor regel:

```
113,114c113,114
< jutterswet-erfwacht|exec|EXCLUDE loader: … (a supertype chain carrying a temporal or dimensioned member …)
< jutterswet-erfwacht|drl |EXCLUDE loader: … (a supertype chain carrying a temporal or dimensioned member …)
---
> jutterswet-erfwacht|exec|PLAYED 1
> jutterswet-erfwacht|drl |PLAYED 1
```

Twee regels, allebei de nieuwe golden, en geen enkele andere rapportregel
beweegt. exec-tak PLAYED 133 -> 134, DRL-tak 91 -> 92. De sweep is na élke
loaderwijziging opnieuw gedraaid — ook na de versmalling van de §9.9-wacht, die
na de eerste ronde kwam.

**Gate-bookkeeping.** `EXPECTED_PLAYED` **133 -> 134** in
`scripts/check-drools.sh`, met reden en datum in het script. De kanaalvloerlijst
beweegt NIET, en dat is gemeten in plaats van aangenomen: het
weigerkanaal van de teruggetrokken vorm was **vacuüs** over het corpus — geen
enkele golden rapporteerde "supertype chain" in de basis-sweep — dus er
vertrekt geen entry en er komt er geen bij (49 gepinde niet-spelers, ongewijzigd).

**De JUnit-getuige, expliciet.** `OverervingWeigeringWitnessTest` pinde de
teruggetrokken weigering met twee tests. Dat is een wijziging van een BESTAANDE
test, normaal vraagplichtig; hier is ze de kern van het gemandateerde werk — de
weigering verdwijnt bewust, dus haar getuige moet mee omkeren of hij pint een
zin die niet meer waar is. Beide zijn waarde-getuigen geworden die het FACET op
het subtype uitlezen (`granulariteitVan`/`granulariteitLeden` en
`dimensiesVan`/`gedimensioneerdeLeden`), plus een assertie dat de EIGEN kaart
eigen blijft — die heeft de beangenerator nodig. Er kwamen vijf tests bij voor
de §9.9-familie: twee weigeringen, hun gedegenereerde randvorm en twee grenzen
die moeten LADEN. Klasse 9 -> 14 tests, suite **330 -> 335** (vloer 239, bewust
niet verhoogd).

Eén fout in die fixtures gerepareerd voordat ze landden: mijn eerste versies
bouwden documenten die de verifier zelf al afwijst (een
`attribuutTijdlijnNaam`-ingang zonder tijdlijndeclaratie en zonder passende
granulariteit is WF-003 plus WF-005). Een weigering gepind op een document dat
via geen enkele gate een uitvoerder bereikt, pint niets. Alle vijf de fixtures
zijn nu gemeten verifier-schoon, en de klasse zegt in haar commentaar waarom
dat de eis is.

Eerlijk erbij: de gedimensioneerde getuige is op de basis rood om ÉÉN reden, de
loader. De accessorhelft slaagde daar al, want `dimensiesVan` loopt sinds de
kernel de keten. De gedimensioneerde helft van de brede weigering was dus al
te breed toen ze geschreven werd — een STATISCH argument (nul rauwe
`attribuutDimensies`-lezingen buiten de loader), geen uitvoeringsgetuige: geen
van de 183 goldens draagt een gedimensioneerd attribuut mét cases, dus dat de
intrekking van die helft geen enkele waarde verschuift is geredeneerd en niet
gedraaid. Genoteerd als open punt, niet als meting.

**Spec-zinnen: er waren er geen.** Ik heb `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` doorzocht op de weigering (op "temporal or
dimensioned", "facet side map", "per declared type", "Drools loader", en op
elke regel die "Drools" met "chain"/"inherit"/"subtype"/"supertype" combineert).
Geen enkele normatieve zin noemde haar: de weigering stond in de loader, in de
JUnit-javadoc en in `spec/CONSTRUCTS.md` rij 39 — het register dat ik niet
aanraak. Er viel dus niets bij te werken; er viel iets bij te ZETTEN. Eén
bullet toegevoegd in IR-SPEC ch. 2.2, bij de andere uitvoerderstatusnoten van
de keten: dat een geërfd lid zijn facetten meedraagt (getuige
`golden/jutterswet-erfwacht`), en precies wat de backend nog wél weigert — het
geërfde attribuut op een benoemde tijdlijn **met een variabel startpunt**, en
het voorouderlijke startpuntanker — met de zin die de grens trekt: op een
vast-startpunt-tijdlijn antwoordt het predicaat vanaf het subtype hetzelfde als
vanaf de declarerende voorouder, dus daar valt niets blind te zijn en laadt de
vorm gewoon. IR-SPEC is een beschermd pad, dus dat is een VOORSTEL, geen
landing.

**Veertien gates groen** (slotregels in het sessieverslag).

**Registervoorstel** (letterlijke tekst, hoofdsessie landt het),
`spec/CONSTRUCTS.md` rij 39. **Twee plekken**, want de cel noemt de ingetrokken
weigering TWEEMAAL, en na de landing zijn de drie deelclaims van de tweede
allemaal onwaar.

*Plek 1.* De bestaande zin

> The Drools loader additionally refuses a chain carrying a temporal or
> dimensioned member: its facet side maps are read per declared type at some
> forty places, and an inherited timeline member would lower as a scalar.

te VERVANGEN door:

> The Drools loader refused a chain carrying a temporal or dimensioned member
> until 2026-09-12, because its facet side maps were read per declared type;
> that refusal is WITHDRAWN. All 116 member lookups among the 122 measured
> reads outside the loader (on 121 lines — `ContractValidator` carries two on
> one) now go through chain-closed map accessors on `Objecttype` — one generic
> `opKetenKaart` and five thin variants on it, the shape `gedimensioneerdeLeden`
> already had — in MAP form rather than through the existing one-name
> accessors, because the backend matches member names by ref's `norm` and the
> model by `equalsIgnoreCase`, and routing them through `opKeten` would have
> closed the chain and opened a name-matching divergence in the same edit. The
> six remaining raw reads each carry their reason: the bean generator's two
> ITERATIONS over its own declaration maps (the generated subclass `extends`
> its parent and `leden()` subtracts what the parent already declares, so a
> chain-closed iteration would visit each inherited member once per
> descendant), the two lookups inside them (keyed by a name that iteration
> produced, so the nearest declarant is the type itself) and the contract
> validator's two-term disjunction over EVERY object type. Witness
> `golden/jutterswet-erfwacht`, the subtype twin of
> `golden/jutterswet-getijdenwacht`: an inherited monthly attribute folded over
> an inherited monthly characteristic from a subtype subject, 6 × 10 + 1 × 11 =
> 71, ref = clingo = Drools in both transpiler branches (played channel
> 133 -> 134). The dimensioned half of the withdrawn refusal was OVER-BROAD
> even when written — every dimension read already went through the chain-aware
> `dimensiesVan`. That half is a STATIC argument and has no execution witness:
> no golden carries a dimensioned attribute together with case files, so the
> backend refuses every such member per form anyway and nothing measures the
> withdrawal. What replaces the refusal is the §9.9 startpunt-ANCHOR family,
> refused at two independently falling sites: an inherited attribute riding a
> named tijdlijn WITH A VARIABEL STARTPUNT, and a startpuntbepaling declared on
> an ancestor. The exchange is narrower on two axes and wider on one, all five
> shapes measured verifier-clean against the merge base: the fixed-startpunt
> named tijdlijn is FREED (base EXCLUDE, head PLAYED — there the predicate
> answers the same from the subtype as from the ancestor, so there is nothing
> to be blind about), while a determination on an ancestor (base PLAYED, the
> one non-degenerate hole) and the degenerate `granulariteit: null` tijdlijn
> (base PLAYED — WF-005 compares None with None, so no granularity facet is
> required and the broad refusal saw nothing) are both CAUGHT. On ordinary
> verifier-clean input the plain named-tijdlijn shape was never silent: WF-005
> forces the granularity facet to match the declaration, and the broad refusal
> tested exactly that. The family stays open with its reason: it is not a
> member lookup but a protocol keyed on the declared subject name, so it is
> widened whole or not at all.

*Plek 2.* In de "Open with reasons:"-staart van dezelfde cel de derde
open-flank te SCHRAPPEN. De staart luidt nu

> Open with reasons: the uncrossed AttribuutRef name itself (a spec question —
> widen WF-034 or a number of its own), the §9.9 anchor family keyed on the
> declared subject name (widened whole or not at all), and the Drools backend,
> which reads its facet maps per declared type at 122 sites and therefore
> loudly refuses subtype x temporal/dimensioned in the loader — repair material
> (`jutterswet-erfwacht`, emitted, verifier-clean, ref = clingo exact)
> preserved verbatim in the repair card.

en moet worden

> Open with reasons: the uncrossed AttribuutRef name itself (a spec question —
> widen WF-034 or a number of its own), and the §9.9 anchor family keyed on the
> declared subject name (widened whole or not at all) — narrowed 2026-09-12 to
> the two sites where that key really blinds a subtype, and refused there by
> name in the Drools loader.

Alle drie de deelclaims van de geschrapte flank zijn na de landing onwaar: de
backend leest zijn facetkaarten niet meer per gedeclareerd type, hij weigert
subtype × temporeel/gedimensioneerd niet meer, en het reparatiemateriaal is
geen materiaal meer maar een gelande golden.

**Eerlijke afwijkingen.** (a) Ik telde eerst 121 waar de opdracht 122 zei en
meldde het verschil als onverklaard; de lens wees het aan —
`ContractValidator.java:58` draagt twee lezingen op één regel. Beide getallen
klopten, van verschillende dingen. (b) De opdracht vroeg de
weigering te VERWIJDEREN omdat de reden weg is; ik verwijder de brede en zet er
een smallere voor in de plaats, omdat de meting een restklasse aanwees die de
brede weigering niet eens dekte. Dat is meer dan gevraagd en het is de reden
waarom: zonder die twee weigeringen zou deze kaart twee stille gaten laten
liggen die ze zelf heeft blootgelegd. (c) De spec-zinnen bestonden niet; ik heb
er één bullet bijgezet in plaats van er drie bijgewerkt, en dat is een voorstel
op een beschermd pad. (d) `kern/Tijdrekening.java` stond in may-touch maar
hoefde niet: `periodeGrensLowerbaar` liep al door `granulariteitVan`. (e) Voor
de eigenlijke documentklasse van deze kaart is de ketenblindheid LUID gebleken
(stap 2 hierboven), niet stil; de stille gevallen die ik vond zitten in de
§9.9-familie, en die repareert deze kaart niet maar weigert ze.
