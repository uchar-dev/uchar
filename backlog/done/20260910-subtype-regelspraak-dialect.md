# RegelSpraak-dialectextensie: `is een` — objecttype-overerving in de kopvorm

Goal: de kopvorm `Objecttype de RechtOpUitkeringWw is een RechtOpUitkering (mv: …)` als uchar-dialectextensie in `frontends/regelspraak`, naar het stramien van de prioritized-defaults-extensie: strikt additief (alle officiële documenten en het hele bestaande corpus parsen en emitten byte-identiek), lexicaal minimaal, gelowered naar de kernel-subtypering van `20260910-subtype-kernel`, en geregistreerd in `grammar/DIVERGENCES.md` als tracked divergence van de v2.3.0-arbiter. Onderzoek en besluit onderweg: de interactie van `is een` met bestaande kopannotaties (bezield, mv), het gedrag van kwantificatie- en navigatievormen zodra subtypen bestaan, en de frontend-weigeringslijst (tweede ouder, zelfverwijzing, onbekend supertype) — elk met een negatieve getuige; een target dat op twee niveaus regels draagt is géén weigering maar volgt de groepserving van opdrachtbesluit 3, met de kernel-getuigen als semantische maat. Neem in de kaartafronding de notitie op dat de werkbank-generator (`wetsanalyse/herleving-ww/scripts/bouw-gegevensspraak.py`, buiten deze repo) de eerste afnemer is: die emit na landing de 17 gepinde CGM-overervingsparen als `is een` in plaats van platslaan.

Acceptance: de use-case-golden bestaat in dialectvorm en speelt groen door de frontend → verifier → executorreeks; de negatieve getuigen weigeren luid; `grammar/DIVERGENCES.md` draagt de registratie; byte-identieke heremissie van het bestaande corpus is aangetoond; de volledige gate-reeks is groen.

May touch: `frontends/regelspraak/` (grammatica, lowering, fixtures), `grammar/DIVERGENCES.md`, `corpus/` (de dialect-golden), en de eigen kaart.

Out of scope: kernel- of executor-wijzigingen (voorganger-kaart); GegevensSpraak-spec-wijzigingen richting de officiële arbiter (wij registreren divergentie, wij herschrijven de arbiter niet); de F-logic-frontend; meervoudige overerving.

## Aangescherpt door de onderzoekskaart (2026-09-11)

`spec/proposals/SUBTYPING.md` §9.2 is bindend; hieronder de
uitvoerbare neerslag.

**Kopvorm, positie beslist.** De productie luidt vandaag
(`frontends/regelspraak/grammar/RegelSpraak.g4:311`):

```
objectTypeDefinition
    : OBJECTTYPE bepaaldLidwoord? objectTypeNaam
      ( MV_START meervoudsvorm RPAREN )? BEZIELD?
      objectTypeMember*
```

De onderlinge volgorde van de annotaties **ligt dus al vast in de
grammatica** — de `mv:`-groep gaat vóór `(bezield)`, er is geen
alternatief — en `Objecttype de Jutter (mv: Jutters) (bezield)` is die
volgorde, geen vrije keuze. De extensie voegt één optioneel element toe
ná `objectTypeNaam` en vóór de `mv:`-groep:

```
Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
```

**Géén nieuw lexertoken.** Een heelfrase-token `IS_EEN : 'is' WSP
'een'` is hier niet veilig, en het `IS_VAN_HET_TYPE`-precedent gaat
niet op. Lexen is niet kop-lokaal: onder maximal munch vuurt zo'n token
overal waar de frase staat, en die staat al in **regeltekst** —
`corpus/golden/jutterswet-schouwrooster/bron.rgs:15` en `:22` ("Een dag
is een schouwdag"), `corpus/golden/jutterswet-postwacht/bron.rgs:66`,
plus zeven `toka/`-bronnen. Dat zou gate 10's byte-identieke
heremissie breken op documenten die niets met subtypering te maken
hebben. `IS_VAN_HET_TYPE` (`RegelSpraakLexer.g4:187`) is alleen veilig
omdat die frase nergens anders voorkomt. De oplossing zit op
**parserniveau**: de bestaande tokens `IS` (`RegelSpraakLexer.g4:284`)
en `EEN` (`:378`) achter elkaar op de kopplek, waar de context
disambigueert. Registratie in
`frontends/regelspraak/grammar/DIVERGENCES.md` naast de
prioritized-defaults-extensie, onder hetzelfde
inert-op-officiële-documenten-argument.

**Golden.** `corpus/golden/jutterswet-wrakrecht` — dezelfde use case
als de kernel-anchor `anchor/jutterswet-strandrechtketen`, nu met
`bron.rgs`, dus onder gate 10's byte-identieke heremissie. Samen met
`corpus/golden/jutterswet-wrakrecht-flogic` (F-logic-kaart) vormt dit
het frontend-diff-paar.

**Weigeringslijst, elk met een fixture.** (1) twee `is een`-clausules op
één kop, of `is een A en B` — de tweede ouder, geweigerd onder
**WF-106** (besluit §11.1, 2026-09-12: het nummer benoemt "een
objecttype declareert hoogstens één direct supertype"), met de twee
CGM-gevallen `RechtOpUitkeringWazo` en `RechtUitkeringZw` bij naam in
de melding. Deze fixture is één van de twéé getuigen die die regel
heeft: op de wire is een tweede ouder onschrijfbaar, dus er is geen
verifier-check en geen `corpus/negatief` — de weigering begrenst de
taal bóven de wire en wordt hier en bij de F-logic-frontend bewezen
(SUBTYPING §3.1);
(2) zelfverwijzing `X is een X`; (3) onbekend supertype — resolutie is
documentbreed en volgordevrij, dus een voorwaartse verwijzing is géén
fout, alleen een ongedeclareerde naam is dat; (4) `is een` dat iets
benoemt dat geen objecttype is (een domein, een parameter); (5) een
geshadowd lid — de frontend weigert vóór de verifier, want zij heeft de
keten bij de hand (kernel: WF-105).

**Expliciete niet-weigering.** Een doel dat op twee niveaus regels
draagt is geen fout: opdrachtbesluit 3 heeft het eerdere blanket-verbod
ingetrokken, en de kernelgetuigen zijn de semantische maat.

**Twee gevolgen die de kaart moet meenemen.**

- **Emitteer rolbindingen zodra je een subtype emitteert.** WF-048's
  uniciteit wordt over de ketengesloten typeparen berekend, dus een
  bindingloze navigatie die vandaag welgevormd is kan door het
  toevoegen van een subtype dubbel verbonden raken en luid omvallen.
  `verbindendeRol` / `bronRol` meegeven neemt dat weg.
- **De laaggrensordening vraagt geen oppervlak.** Besluit §11.2
  (2026-09-12) schrapte het voorgestelde `geerfdeUitzonderingen`-veld:
  de laaggrens is vast geordend — de subtype-eigen uitzonderingen staan
  boven de geërfde — dus er valt niets te declareren en de extensie
  hoeft er niets voor te emitteren. Het nummer WF-106 is herbestemd
  naar de meervoudige-ouderweigering uit de weigeringslijst hierboven.

## Result (2026-09-12)

**Geland: de kopvorm, de vijf weigeringen, de golden.** De frontend leest
`Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)`, bindt het
documentbreed en volgordevrij, en emitteert het gelande kernelveld
`Objecttype.supertype` direct na `naam`. Twee commits: `d8ce276` (dialect) en
`cba015c` (golden + gate-12-pin).

### Wat er is gebouwd

- **Grammatica** (`grammar/RegelSpraak.g4`). Eén optioneel element in
  `objectTypeDefinition`, na `objectTypeNaam` en vóór de `mv:`-groep, plus de
  nieuwe productie `objectTypeSupertype : IS onbepaaldLidwoord objectTypeNaam
  ( EN onbepaaldLidwoord? objectTypeNaam )*`. **Géén nieuw lexertoken** — de
  bestaande `IS` en `EEN` in reeks; het lidwoord is de eigen
  `onbepaaldLidwoord`-productie van de taal, dus `is <Naam>` zonder lidwoord
  bindt niet. De extensie reserveert daardoor géén enkel naamwoord (anders dan
  de defaults-extensie, die `Uitzonderingen`/`ongeordend` inneemt). De
  productie ADMITTEERT met opzet wat WF-106 verbiedt, zodat de resolver de
  ouders bij naam kan weigeren.
- **Resolutie** (`resolver/DefinitionResolver.java`). De clausules worden
  opgeslagen en pas ná pass 2 gebonden — een voorwaartse verwijzing is geen
  fout. Daarna de cyclusdetectie (kleur-DFS met genoemd pad, de vorm van de
  verifier; de terugtak wordt na melding doorgeknipt) en de shadowcontrole.
- **Emissie** (`emit/IrEmitter.java`). `supertype` alleen wanneer aanwezig, op
  de schemapositie; een worteltype emitteert niets, dus de byte-identiteit van
  alle 42 bestaande rgs-goldens blijft staan (gemeten).
- **DIVERGENCES.md §5** — registratie naast de prioritized-defaults-extensie,
  met het inert-op-officiële-documenten-argument, de lowering, de
  weigeringslijst en de scopegrenzen.
- **Golden `corpus/golden/jutterswet-wrakrecht`** — `bron.rgs`, een door de
  frontend geëmitte `ir.json`, `NOTE.md`, en vier casebestanden die
  **byte-identiek** zijn aan die van `anchor/jutterswet-strandrechtketen`. Eén
  stel verwachtingen, afgespeeld tegen een handgeschreven én een geëmitteerd
  document. Speelt in alle drie de executors (gate 12: EXPECTED_PLAYED 129 →
  130, met reden en datum in het script).

### De weigeringen (elk met een fixture in `SubtypeDialectTest.java`)

| # | vorm | regel | melding bevat |
|---|---|---|---|
| 1 | `is een A is een B` en `is een A en B` | WF-106 | beide oudernamen + `RechtOpUitkeringWazo`, `RechtUitkeringZw` |
| 2 | `X is een X` | WF-104 | het objecttype |
| 3 | cyclus van twee stappen | WF-104 | het genoemde pad |
| 4 | onbekend supertype | WF-103 | de onbekende naam |
| 5 | supertype dat een domein/parameter/… noemt | WF-103 | de soort van de naam |
| 6 | geshadowd attribuut / kenmerk | WF-105 | lid + declarant |

Expliciete niet-weigeringen, ook met getuige: een voorwaartse verwijzing, en
een doel dat op twee niveaus regels draagt.

### Rood-eerst

`SubtypeDialectTest.java` is geschreven en gedraaid vóór één regel grammatica:
**223 tests, 14 rood** — elke weigering, elke kopvormtest en de wiretest
faalden, en de twee die groen waren (`een_worteltype_emitteert_geen_supertype_sleutel`,
`het_lidwoord_voor_de_supertypenaam_hoort_bij_de_frase`) waren precies de twee
die de afwezigheid van de extensie pinnen. Na grammatica + resolver + emitter:
223 groen. De rolbindingtest was daarna nog rood op een verkeerde premisse van
mijzelf (het dubbel verbonden paar is Kustgemeente × **Wrakrecht**, niet ×
Strandrecht — een rol is een BOVENgrens, dus alleen het subtype vult beide
rollen); de test is op de juiste premisse gezet, niet de code. De
`altijd`-rechtvaardiging ging óók rood-eerst, maar via de gate: gate 3 wees de
golden af met tweemaal `WF-050 … a non-initialisation default carries a
justification`, en pas daarna is de lowering geschreven en de unittest
toegevoegd (224 groen).

De lensronde bracht er drie bij (227 groen). De misbindingsgetuige ging óók
rood-eerst: `p11`/`p15` geëmit, `bronRol {strandbeheer, wrakrechten}` gemeten,
verifier PASS — het stille geval — daarna de identiteitseis, daarna dezelfde
twee documenten bindingloos met een luide WF-048, met de positieve controle
`p16` onveranderd.

### De dertien slotregels (letterlijk)

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 205 file(s) validate and round-trip, floor 192)
check-verifier: PASS (205 golden clean; 378 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 485 case file(s) validate)
check-oracle: PASS (ref suite 592 worked examples, floor 489; 459 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 125 checks, floor 83; 459 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (423 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (227 tests, floor 173); 43 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS
check-drools: PASS
check-reftype-derivation: PASS (92 unit annotations twisted over 17 goldens, floor 74; 80 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
```

Gate 12's eigen tellingen: `channel floor 49 non-PLAYED goldens pinned by
name; played matches the pinned 130` en `JUnit 330 tests, floor 239`.

### Twee beslissingen die proposal noch kaart dekte

**1. Een onvoorwaardelijke basisregel krijgt de vacuüm-ware rechtvaardiging.**
De §7.2-usecase vraagt een basis `= de grondslag` zónder `indien`, en dat had
de defaults-extensie nog nooit gelowerd: al haar bestaande fixtures en beide
bestaande defaults-goldens dragen een conditie. WF-050 eist een
rechtvaardiging op élke niet-initialisatiedefault, dus `voorwaarde: null` is
verifier-afgewezen. Gekozen: de lowering synthetiseert
`Samengesteld{ALLE, drempel 0, onderdelen []}` met bron `"altijd"` — precies
hoe de handgeschreven anchors het spellen, en semantisch wat een kale regel
altijd al was (de ch.-7.8-staande norm, die geldt zodra niets erboven beslist).
Eén plek (`RuleChecker.defaultVan`), dus geordend én ongeordend krijgen hem.
Alternatief was een taalvorm voor "indien altijd" — meer oppervlak voor niets.

**2. De vouwbron valt terug op de GESCHREVEN rol, en eist dan identiteit.** De
kaart vraagt de frontend WF-048's ketensluiting vóór te zijn. Gemeten: de
paarzoektocht van de frontend is al ketengesloten (`rolHeeftObjecttype` gebruikt
`isEen`), dus zij zwijgt precies wanneer WF-048 weigert — en dan ging de
vouwbron bindingloos de wire op. `IrEmitter.bronRolBinding` probeert nu, waar
het paar dubbelzinnig is, de rol die de zin schreef (`elementRolBinding` over de
naamkandidaten van `onderwerpBron`) — dezelfde declared-name-zoektocht die de
Onbekend-tak al deed.

De eerste versie van die terugval was FOUT en is door de lensronde gemeten
(`p11`, `p15`): de naamkandidaten ontstaan door leidende woorden weg te laten,
dus "alle geborgen wrakrechten" wordt óók als "wrakrechten" geprobeerd, en dat
trof een gelijknamige rol in een ANDER feittype. De bovengrenstoets die overal
elders juist is liet dat door — `Wrakrecht.isEen(Strandrecht)` — en de vouwbron
ging gebonden aan een relatie die de zin nooit noemde de wire op: verifier
PASS, `ref` rekende 100 waar het document 10 verwacht. Bindingloos was daar
juist de goede uitkomst geweest (luide WF-048). De terugval eist nu dat de
bereikte rol het elementtype ZELF als vuller declareert — identiteit, niet ⊑.
Gemeten na de fix: `p11` en `p15` gaan bindingloos en krijgen
`WF-048 FAIL … 2 fact types relate 'Kustgemeente' to 'Wrakrecht'`, de positieve
controle `p16` bindt onveranderd op `wrakberging/geborgen wrakrecht` en blijft
verifier-schoon, en de golden heremitteert byte-identiek. Twee getuigen:
`een_subtype_dat_het_typepaar_dubbel_verbindt_houdt_zijn_bronRol` (bindt) en
`een_naam_die_een_rol_in_een_ander_feittype_treft_bindt_niet` (bindt niet).
Codecommentaar, DIVERGENCES §5 en deze notitie zeggen nu alle drie hetzelfde.

### Kleinere keuzes, expliciet gemaakt

- **WF-106 telt onderscheiden namen (ci) over het hele document.** Eén ouder
  die twee keer geschreven wordt is overtollig, geen tweede ouder; twee
  verschillende namen weigeren, hoe ze ook gespeld zijn.
- **`Extensie van objecttype` (§13.3.1.2) draagt de clausule niet.** Een
  tweede declaratieplek voor één relatie heropent de tweede-ouder-vraag over
  constructies heen. Genoteerd in DIVERGENCES §5 als scopegrens.
- **`verdeling`'s `verbindendeRol` kreeg géén naam-terugval.** De
  `ResolvedVerdeling` draagt geen geschreven rolnaam, dus er is niets om op
  terug te vallen; een subtype dat het bron/doel-paar dubbel verbindt laat die
  binding dus wél wegvallen en WF-048 weigert luid. Geen corpusgetuige, geen
  stille fout — maar het is een openstaande flank, niet iets wat deze kaart
  heeft dichtgezet.
- **Kernelgat, buiten deze may-touch: de verdelingstak van WF-034 sluit de
  keten niet.** Gemeten met de lenscasus `p18`: `bergingsaandeel` staat op
  `Berger`, `Hoofdberger is een Berger`, en een verdeling naar de hoofdbergers
  valt op
  `WF-034 FAIL /verdelingen/0/doelAttribuut: 'bergingsaandelen' is not a
  declared attribute of the target type 'Hoofdberger'`. Luid, dus geen
  correctheidsfout — maar het weigert een document dat ch. 2.2 toestaat. De
  verifier ligt buiten deze kaart; de hoofdsessie pakt het direct na de merge
  op.
- **Twee auteursfouten krijgen een gerichte melding náást de ANTLR-fout.**
  Kaal `is Strandrecht` (lidwoord vergeten) en `(mv: …) is een …` (verkeerde
  annotatievolgorde) leverden alleen "no viable alternative", wat de auteur naar
  de verkeerde plek stuurt. `Resolver.kopvormHints` leest ze van de tokenstroom
  af, alleen náást een echte syntaxfout en alleen over de KOP — de wandeling
  stopt bij het eerste token dat geen kop kan bevatten, zodat de "is een" van
  gewone regeltekst nooit voor een van beide wordt aangezien. Drie getuigen,
  waarvan één negatief (`een_correcte_kop_krijgt_geen_kopvormhint`).
- **WF-104 en WF-105 citeren nu een regel.** De cyclusmelding wijst de clausule
  aan die de cyclus sluit; de shadowmelding wijst de HERDECLARATIE aan, want
  daar zit de reparatie. Gemeten: regel 4 respectievelijk regel 6 op de twee
  fixturevormen, waar beide eerder 0 droegen.
- **De `altijd`-unittest staat in `SubtypeDialectTest`, niet in
  `DefaultsExtensieTest`,** waar hij inhoudelijk hoort. Bestaande testbestanden
  zijn niet aangeraakt; een latere kaart mag hem verhuizen.

### Registervoorstel (letterlijke tekst, `spec/CONSTRUCTS.md` rij 39)

In de IR-deltakolom, vervang

> `new kernel (extension per the § Method, not emitter-produced yet — the dialect card makes it emitter-borne, the route `Let`/`BinderRef` took):`

door

> `new kernel (extension per the § Method; EMITTER-BORNE since 2026-09-12, the route `Let`/`BinderRef` took — the RegelSpraak dialect writes it from the head form `Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)`, a parser-position extension over the existing `IS`/`EEN` tokens and never a new lexer token, registered in `frontends/regelspraak/grammar/DIVERGENCES.md` §5):`

en voeg aan het eind van de **Landed (2026-09-12)**-zin toe, ná "and five negatiefs":

> `. The dialect frontend landed the same day: `jutterswet-wrakrecht` is the anchor's SOURCE twin — the same three types and the same four cases, byte-identical, but with an `ir.json` the emitter wrote from a `bron.rgs`, so the field is producer-backed and not only hand-written (gate 10's byte-identical re-emission, gate 12's played channel 129 -> 130). WF-106's promised witness is now real: the frontend refuses both spellings of a second parent (two `is een` clauses, and `is een A en B`) naming the two canonical-data-model cases in the message, and refuses WF-103/104/105 ahead of the verifier because it has the chain at hand. Two consequences the syntax did not carry: an UNCONDITIONAL base rule now lowers to the vacuously true justification WF-050 requires (an `ALLE`-group over no parts — how the hand anchors spell "altijd"), which no defaults golden had needed before; and where the chain closure makes a type pair ambiguous the fold's `bronRol` falls back to the role the sentence WROTE — demanding that role declare the element type ITSELF, since the by-name candidates are truncations and an upper-bound test there would bind a same-named role in another fact type, which is verifier-clean and silently folds the wrong set where a bindingless fold would get the loud WF-048`

`spec/PLAN.md` heeft geen wijziging nodig: de scope van de kaart is niet
veranderd.

### Niet aangeraakt

`spec/CONSTRUCTS.md`, `spec/PLAN.md`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, de verifier, `ref/`, `backends/`, andere
corpuszones, `.github/`, `WAY-OF-WORKING.md`. Van `scripts/` alleen
`check-drools.sh`, en alleen de `EXPECTED_PLAYED`-gelijkheid (129 → 130) met
reden en datum. Niets in de kernelspec wringt met wat deze kaart bouwde: de
schema-sleutelvolgorde, ch. 2.2's "whole or not at all" en de WF-103/104/105/106
teksten sloten precies aan.
