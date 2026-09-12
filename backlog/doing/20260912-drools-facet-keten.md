# drools-facet-keten — de ketensluiting van de Drools-backend

Goal: de reparatiekaart die vervolgpunt A van
`backlog/done/20260912-subtype-frontend-keten.md` uitvoert, met maintainersmandaat
voor route 1 (mechanisch). `backends/drools` leest de facet-ZIJKAARTEN van leden
(granulariteit, tijdlijnaantal, dimensies) op 121 gemeten plekken buiten de loader
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
- elke van de 121 plekken gemeten en geclassificeerd: LEDENLOOKUP (→ accessor) of
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

**De meting: 121 lezingen buiten de loader, niet 122.** De vorige kaart telde
122; nagemeten over `backends/drools/src/main` op de tien facetkaartnamen zijn
het er 121 (ExecModelTranspiler 67, DrlTranspiler 43, CaseRunner 5,
FactModelGenerator 4, EffectieveEenheid 1, ContractValidator 1; de loader telt
er zelf 12, allemaal de schrijfkant van de declaratie). Ik heb geen indirecte
lezing gevonden die het verschil verklaart — geen enkele facetkaart wordt in een
lokale variabele gezet en later gelezen — dus ik meld het getal dat ik meet.

| klasse | n | wat ermee gebeurt |
|---|---|---|
| ledenlookup, één naam direct (`get`/`containsKey`) | 85 | door de ketengesloten kaart |
| ledenlookup, hele kaart aan een naamcontract (`meerPeriodiek` 24, `bevatNaam` 5, `gedeclareerdeNaam` 2) | 31 | door de ketengesloten kaart |
| ITERATIE over een eigen-ledenkaart | 2 | **rauw**, met reden |
| lookup gesleuteld op een naam ÚIT die iteratie | 2 | **rauw**, met reden |
| disjunctie over ALLE objecttypen | 1 | **rauw**, met reden |

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
`norm` vouwt witruimte, `equalsIgnoreCase` vouwt niet-ASCII kapitaal. Alle 121
plekken door `opKeten` leiden zou de keten sluiten en in dezelfde bewerking een
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

**Waar de blindheid wél stil was, en het is niet waar de weigering keek.** De
brede weigering testte granulariteit en dimensies. Gemeten op de basis-jar met
de kernel-anchor als proefstuk:

```
BASE, geërfd attribuut op een BENOEMDE tijdlijn (attribuutTijdlijnNaam)  -> PLAYED 1
BASE, startpuntbepaling op een VOOROUDER (resolvedStartpunten)           -> PLAYED 1
BASE, geërfd temporeel lid (attribuutGranulariteit)                      -> EXCLUDE loader
```

De eerste twee zijn de §9.9-ankerfamilie, en die glipte er stil doorheen: een
benoemde tijdlijn zonder granulariteitsregel draagt geen van de twee sleutels
waar de brede weigering naar keek. `heeftVariabelStartpunt` leest
`attribuutTijdlijnNaam` op het type dat de referentie noemt, `startpuntLid` en
`isStartpuntLid` sleutelen op de subjectnaam van de startpuntbepaling — dus
vanaf een subtype geldt een geërfd attribuut als niet-variabel, wordt de
startpuntbucketing overgeslagen en verdwijnt de §6.8-leegfoutcompagnon. In
beide takken. De vervangende weigering is dus **smaller in vorm maar ruimer in
dekking**: ze weigert twee vormen die de brede toeliet, en laat de vorm toe
waarvoor de brede bestond.

De familie als geheel verbreden blijft wat de vorige kaart erover schreef: geen
ledenlookup maar een protocol, waarvan één van de vier plekken door de keten
leiden de naad alleen verschuift. Dat is vervolgwerk B, nu met twee gemeten
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

**Het vangnet.** Basis-jar (main 8bd98e4) en kop-jar over ÉLKE golden met
cases, in BEIDE transpilertakken, regel voor regel gedift:

```
base 364 regels, head 366 regels
112a113,114
> jutterswet-erfwacht|exec|PLAYED 1
> jutterswet-erfwacht|drl|PLAYED 1
```

Twee regels, allebei de nieuwe golden, en geen enkele andere rapportregel
beweegt. exec-tak PLAYED 133 -> 134, DRL-tak 91 -> 92. De sweep is na de
loaderwijziging opnieuw gedraaid, want de eerste liep vóór de nieuwe
§9.9-weigering bestond.

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
eigen blijft — die heeft de beangenerator nodig. Er kwamen twee tests bij voor
de twee nieuwe §9.9-weigeringen. De klasse-javadoc is bijgewerkt: 11 tests waar
het er 9 waren, suite 332 (vloer 239).

Eerlijk erbij: de gedimensioneerde getuige is op de basis rood om ÉÉN reden, de
loader. De accessorhelft slaagde daar al, want `dimensiesVan` loopt sinds de
kernel de keten. De gedimensioneerde helft van de brede weigering was dus al
te breed toen ze geschreven werd.

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
geërfde attribuut op een benoemde tijdlijn en het voorouderlijke startpuntanker.
IR-SPEC is een beschermd pad, dus dat is een VOORSTEL, geen landing.

**Veertien gates groen** (slotregels in het sessieverslag).

**Registervoorstel** (letterlijke tekst, hoofdsessie landt het),
`spec/CONSTRUCTS.md` rij 39. De bestaande zin

> The Drools loader additionally refuses a chain carrying a temporal or
> dimensioned member: its facet side maps are read per declared type at some
> forty places, and an inherited timeline member would lower as a scalar.

te VERVANGEN door:

> The Drools loader refused a chain carrying a temporal or dimensioned member
> until 2026-09-12, because its facet side maps were read per declared type;
> that refusal is WITHDRAWN. All 116 member lookups among the 121 measured
> reads outside the loader now go through chain-closed map accessors on
> `Objecttype` — one generic `opKetenKaart` and five thin variants on it, the
> shape `gedimensioneerdeLeden` already had — in MAP form rather than through
> the existing one-name accessors, because the backend matches member names by
> ref's `norm` and the model by `equalsIgnoreCase`, and routing them through
> `opKeten` would have closed the chain and opened a name-matching divergence
> in the same edit. The five remaining raw reads each carry their reason: the
> bean generator's two ITERATIONS over its own declaration maps (the generated
> subclass `extends` its parent and `leden()` subtracts what the parent already
> declares, so a chain-closed iteration would visit each inherited member once
> per descendant), the two lookups inside them (keyed by a name that iteration
> produced, so the nearest declarant is the type itself) and the contract
> validator's disjunction over EVERY object type. Witness
> `golden/jutterswet-erfwacht`, the subtype twin of
> `golden/jutterswet-getijdenwacht`: an inherited monthly attribute folded over
> an inherited monthly characteristic from a subtype subject, 6 × 10 + 1 × 11 =
> 71, ref = clingo = Drools in both transpiler branches (played channel
> 133 -> 134). The dimensioned half of the withdrawn refusal was OVER-BROAD
> even when written — every dimension read already went through the
> chain-aware `dimensiesVan`. What replaces it is narrower in form and WIDER in
> reach: the §9.9 startpunt-ANCHOR family, refused at two independently falling
> sites — an inherited attribute riding a NAMED tijdlijn, and a
> startpuntbepaling declared on an ancestor. Measured on the merge base, BOTH
> of those shapes PLAYED silently, because the broad refusal tested granularity
> and dimensions and neither of them carries either key: from a subtype the
> anchor family reads the attribute as unanchored, skips the variabel-startpunt
> bucketing and drops the §6.8 leeg-fout companion, in both branches. That
> family stays open with its reason: it is not a member lookup but a protocol
> keyed on the declared subject name, so it is widened whole or not at all.

**Eerlijke afwijkingen.** (a) De opdracht schreef 122 lezingen; ik meet er 121
en meld het verschil zonder het te kunnen verklaren. (b) De opdracht vroeg de
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
