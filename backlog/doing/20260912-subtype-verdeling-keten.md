# subtype-verdeling-keten — de ketensluiting van de verdelings-tak

Goal: een reparatiekaart uit de dialect-lensronde. Die ronde mat op
`/tmp/lens-cases/p18-verdeling-erfattribuut.rgs` een WF-034 FAIL op
`/verdelingen/0/doelAttribuut` waar het doeltype een subtype is en het
doelattribuut op een voorouder staat, en las die als een achterblijver van de
subtype-kernel: de verdelings-tak zou nog de rauwe declaratieset lezen terwijl
ch. 2.2's effectieve-ledenverzameling het attribuut aan het subtype toekent.
Deze kaart meet dat na over ELKE verdelings-zijdige en aanverwante
declaratie-lookup in `spec/verifier` (verdeling, feitcreatie, objectcreatie,
uniciteit, consistentie), repareert wat achterblijver is, documenteert per
plek die rauw BLIJFT waarom dat daar klopt, en meet daarna de hele route: kan
zo'n verdeling ook worden UITGEVOERD in ref, clingo en Drools? Spelen alle
drie, dan een kleine golden met handberekende pins als getuige.

Acceptance:
- de p18-vorm is eerst een falende getuige, daarna groen — of, als de
  weigering geen achterblijver blijkt, is de werkelijke oorzaak gemeten,
  benoemd en met een probe vastgelegd;
- elke rauw gelaten declaratie-lookup draagt zijn reden;
- alle dertien gates groen (`bash scripts/check-*.sh`);
- de getuige-golden speelt in ref, clingo en Drools met dezelfde
  handberekende pins.

May touch: `spec/verifier/`, `ref/`, `backends/`, `corpus/` (de nieuwe
golden), `scripts/check-drools.sh` (alleen de `EXPECTED_PLAYED`-gelijkheid),
`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (alleen zinnen die door deze
reparatie onwaar of onvolledig worden), dit kaartbestand.

Out of scope: `frontends/regelspraak` (net geland; een gat daar wordt gemeld,
niet gerepareerd), `spec/CONSTRUCTS.md` en `spec/PLAN.md` (registervoorstel
als letterlijke tekst in het Result), grote spec-herschrijvingen.

Result:

**Het gemeten uitgangspunt hield geen stand, en de werkelijke oorzaak van
p18 ligt in de frontend.** `wf034_verdeling_naamresolutie` leest de
ketengesloten view (`objecttype ctx`) al sinds de kernel; met
`doelAttribuut: "bergingsaandeel"` op `doelType: "Hoofdberger"` — het
attribuut staat op `Berger` — geeft de verifier PASS. p18 faalt omdat de
frontend de MEERVOUDSVORM `bergingsaandelen` op de wire zet:
`MeervoudMatcher.vindAttribuut` leest `ot.attributen` en
`ot.attribuutMeervoud` van het doeltype ZELF, niet van `ot.keten()`, dus
op een subtype bindt geen enkele geschreven vorm van een geërfd attribuut
en blijft de geschreven meervoudsvorm staan. `jutterswet-berging` schrijft
exact dezelfde zin en emitteert wél het enkelvoud, want daar staat het
attribuut op het doeltype. De verifier weigert dus terecht; de frontend
levert een naam die geen declaratie draagt. Buiten may-touch, dus gemeld
en niet gerepareerd — de fix is `keten()` in `MeervoudMatcher`
(`vindAttribuut`, en `vindObjecttype` heeft dezelfde vraag niet). Zolang
dat gat er is kan een verdeling naar een subtype met een geërfd
doelattribuut niet uit een `bron.rgs` komen; de getuige-golden is daarom
handgeschreven, zonder brontweeling.

**Punt 1 — de veeg.** Elke naam-gesleutelde lookup in `spec/verifier` gaat
al door `objecttype ctx`: de zes verdelingsvelden (WF-034
naamresolutie), de objectcreatie (WF-027 `nieuwType`, WF-034 `subject`),
de uniciteit en haar unie (WF-030), de consistentie (WF-029) en elke
verwijzingspositie in de expressiewandeling. Rauw BLIJVEN, elk met reden
(nu opgeschreven bij `declared_objecttype`): WF-002, WF-105 en WF-034's
zijtabelkruising (die moeten eigen en geërfd uit elkaar houden), WF-103
(een supertype-verwijzing moet een GEDECLAREERD type noemen — via de
keten resolveren zou circulair zijn), en de zeventien checks die geen
lookup doen maar over de declaratietabel ITEREREN (WF-001, WF-094 beide
armen, WF-003/004/005/007/092, WF-057, WF-059, WF-061, WF-065, WF-072,
WF-088, WF-104): hun onderwerp is de declaratie op dat JSON-pad, en een
ketengesloten view zou één geërfd lid één keer per afstammeling melden,
op paden waar het document niets zegt.

Wél achterblijvers, en de kaart vond ze bij het meten: **de GEBONDEN tak
van elke join vergeleek twee typenamen letterlijk** terwijl haar
bindingloze tweeling (`nav_role_by_name` → `fills`) door ⊑ resolveert.
Hetzelfde document werd zo toegelaten zonder rolbinding en geweigerd mét
— en ch. 2.6 raadt de geweigerde spelling juist aan zodra er een subtype
in het spel is. **TIEN vergelijkplekken**, elk met een probe gemeten, en
ze krijgen bewust niet alle hetzelfde antwoord. De scheidslijn: `fills` /
eenzijdig `is_sub` waar een positie moet KUNNEN HOUDEN wat haar bereikt,
`type_compatible` alleen waar er dóórheen wordt GELEZEN — wat WF-047 met
"agree" al bedoelde.

| # | plek | helper na de kaart | richting |
|---|---|---|---|
| 1 | WF-053 navigatieketen, nabije zijde | `fills` | eenzijdig |
| 2 | WF-053 kenmerkcontrole, nabije zijde | `fills` | eenzijdig |
| 3 | WF-102 `bronPad`-stap, nabije zijde | `fills` | eenzijdig |
| 4 | WF-028 creatieketen-hop, nabije zijde | `fills` | eenzijdig |
| 5 | WF-054 uniciteitsscope, nabije zijde | `fills` | eenzijdig |
| 6 | WF-053 endpoint | `is_sub ~sub:bereikt ~super:endpoint` | eenzijdig (lens-vangst 2) |
| 7 | WF-028 `end_check` (ankerRol/doelRol) | `is_sub ~sub:node ~super:uitvuller` | eenzijdig (lens-punt 3) |
| 8 | WF-034 feitcreatie-beanveld | `is_sub ~sub:uitvuller ~super:beantype` | eenzijdig (lens-punt 3) |
| 9 | WF-052 aangemaakt-zijdige rol | `connecting_role_candidates` | geen vergelijking meer (lens-vangst 1) |
| 10 | WF-102 landing op `elementObjecttype` | `type_compatible` | tweezijdig, op de WF-047-lijn |

Rij 10 blijft tweezijdig omdat de gewone vouwbron dezelfde vraag al door
`wf047_binding ~bound_filler:(Some el)` stelt, die de kernel tweezijdig
maakte: één arm smaller maken zou de twee spellingen van één vouw weer
laten verschillen — precies de splitsing die deze kaart wegneemt. De reden
staat bij de regel. WF-028's KETENEINDPUNT (`the join chain reaches …`)
blijft LETTERLIJK, met reden ter plekke: de onsound richting weigert het
al, en de verbreding hangt aan hetzelfde open punt als hieronder (geen
uitvoerder filtert een bereikte verzameling op een gedeclareerd type).

Geen enkele negatief-getuige komt vrij (gate 3: 378 verworpen,
ongewijzigd).

**Twee regressies die de lens ving, eerlijk als lens-vangst.** Beide zaten
IN de verbredingen hierboven en waren van mij, niet pre-existent:

1. **WF-052 bond de verkeerde rol, volgorde-afhankelijk.** Mijn eerste
   versie gebruikte `List.find_opt … fills` over `ft.rollen`. Onder ⊑ vult
   een subtype-instantie óók de voorouder-rol, dus in een feittype met rol
   0 = ouder (naar-veel) en rol 1 = subtype (naar-één) pakte de check de
   OUDERrol en las daar `rol_meervoudig`. Gemeten op
   `p1d-wf052-bounded.json`: verifier-PASS, waarna **ref omvalt**
   (`to-one role "bijvondsten" on "W1" already links oc0::W1; a second
   filler is ill-typed (ch. 5.5)`) terwijl **clingo PASS** geeft en beide
   links stil vasthoudt — exact de divergentie die WF-052's eigen
   commentaar statisch zegt te sluiten. Met de twee rollen omgedraaid in
   de JSON (`p1b`) weigerde dezelfde kop wél: het antwoord hing van
   array-volgorde af. Nu de kandidaat-afleiding die ref spiegelt
   (`connecting_role_candidates`); p1, p1b, p1d én p1e weigeren, p1c (één
   creatie) blijft PASS.
2. **De WF-053-endpointverbreding liet de richting toe die ref weigert.**
   Ik gebruikte het tweezijdige `type_compatible`, maar
   `ref/lib/eval.ml:521-526` weigert uitdrukkelijk de onsound richting (een
   supertype-instantie tegen een subtype-endpoint). Gemeten op
   `p2-wf053-endpoint.json`: verifier-PASS, **ref EVALUATION FAULT** (`the
   navigation reached "Werf", not the declared endpoint "Bedrijfswerf"`),
   **clingo PASS met getallen** — ref ≢ clingo op een verifier-schoon
   document. Nu eenzijdig; p6a, p7 en de golden blijven PASS, p2 weigert.

Beide zijn dezelfde les als de ontvangers-correctie zelf: een verbreding
is pas af als ze de richting kiest die de uitvoerders wáármaken.

**Punt 2 — de route, per uitvoerder gemeten.** De kaartvorm (doeltype =
subtype, doelattribuut op de voorouder, relatie op het SUBTYPE) speelt
ongewijzigd in ref, clingo en Drools. De CGM-vorm (relatie op de OUDER)
legde een stille drieweg-divergentie bloot op een verifier-schoon
document: 1001 over twee `Hoofdberger`s naast één gewone `Berger` aan één
`Berger`-rol gaf **Drools 500/500/leeg rest 1** en **ref = clingo
333/333/333 rest 2**. `RUNTIME-SEMANTICS` ch. 4.1 zegt letterlijk "every
`targetType` instance related to it through the connecting relation", dus
Drools had gelijk. Ref filtert nu op de extensie van `doelType`; clingo
leidt de doorsnede af in één `vdN_ontv/2` dat elk ontvangerslichaam leest
(18 plekken, wrapgrenzen en tellingen inbegrepen). Identiteit waar de
uitvuller `doelType` of lager is, dus het hele corpus staat stil (460
cases ongewijzigd in gate 5 en 6). Geen terugvalregel nodig: alle drie
spelen.

**Punt 3 — de getuige.** `corpus/golden/jutterswet-hoofdberging`, één
case, handberekende pins, gespeeld door alle drie. Draagt drie dingen
tegelijk die geen van alle uitdrukbaar zijn zonder supertype: het
doelattribuut op de voorouder, de ontvangersfilter, en een scoped
uniciteit die haar scope-hop expliciet bindt met een subtype als
onderwerp (de WF-054-getuige, rood-dan-groen aangetoond). Geen
`bron.rgs`: zie het frontend-gat hierboven.
`scripts/check-drools.sh` **EXPECTED_PLAYED 130 -> 131**, met reden en
datum in het script.

**Punt 4 — spec.** Drie gerichte zinnen. `IR-SPEC` ch. 2.2's extensionele
lijst telde zes posities en miste de zevende, de `targetType` van een
verdeling (nu genoemd, met deze golden als getuige). ch. 3.2's zin "the
bound role's filler IS `targetType`" is sinds de WF-047-verbreding onwaar
en zegt nu ⊑-verenigbaar; de `targetType`-rij zegt erbij dat het de
populatie is en dat `targetAttribute` op de effectieve ledenverzameling
resolveert. `RUNTIME-SEMANTICS` ch. 4.1 krijgt de consequentie met haar
worked example (500/500/leeg, rest 1).

De ch. 2.2-zin beloofde eerst dat alle ZEVEN posities een getuige dragen;
de lens mat dat positie één, de `elementType` van de instancevouw, alleen
de INSLUITINGSHELFT draagt. De uitsluitingsrichting — een `elementType`
strikt ONDER wat het bronpad bereikt — bestaat in **geen enkele
uitvoerder**: gemeten op `p3-wf102-landing.json` telt een mee-gelinkte
gewone `Vondst` mee in een `Munt`-vouw, in ref én clingo, met dezelfde
uitkomst. Spec-versus-uitvoerders, geen differentiële divergentie. De zin
is getemperd en zegt dat nu uit; het punt staat hieronder bij vervolgwerk.

**Registervoorstel** (letterlijke tekst, hoofdsessie landt het),
`spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:

> WF-034's verbreding over de ketengesloten verzameling gold vanaf de
> kernel ook al voor de verdelings-tak; wat achterbleef was de GEBONDEN
> tak van de join-controles (WF-028, WF-053, WF-054, WF-102 en WF-034's
> feitcreatie-beanveld), die twee typenamen letterlijk vergeleek waar de
> bindingloze tweeling door ⊑ resolveerde — hersteld 2026-09-12. Bij
> dezelfde meting bleek de ontvangersverzameling van een verdeling in
> ref en clingo de rol te volgen in plaats van `doelType`, tegen
> RUNTIME-SEMANTICS ch. 4.1 in en afwijkend van Drools; ook hersteld,
> getuige `golden/jutterswet-hoofdberging`.

**Vervolgwerk — gemeten, niet in deze ronde gerepareerd.**

*A. De frontend-ketenblindheid (eigen ronde direct na deze merge).* De
lens vond dezelfde ketenblinde lezing op vijf plekken in
`frontends/regelspraak`, waar een lid van een VOOROUDER niet bindt omdat
de resolver alleen het eigen type leest:

1. `MeervoudMatcher.vindAttribuut` — leest `ot.attributen` /
   `ot.attribuutMeervoud`, niet `ot.keten()`. Dit is de oorzaak van de
   p18-weigering waarmee deze kaart begon; zolang hij er is kan een
   verdeling naar een subtype met een geërfd doelattribuut niet uit een
   `bron.rgs` komen.
2. `KenmerkMatcher` — dezelfde vraag voor kenmerken.
3. De objectcreatie-kenmerktak — een geërfd kenmerk op het nieuwe type.
4. De temporaliteitsfamilie — granulariteit/tijdlijn van een geërfd
   attribuut.
5. `dimensieCel` — de dimensies van een geërfd attribuut.

`RegelSpraakModel.Objecttype` heeft de ketenbewuste accessors al
(`keten`, `attribuut`, `dimensiesVan`, `heeftLid`, `kenmerkSoort`,
`declarantVan`); de vijf plekken gebruiken ze niet.

*B. De uitsluitingsfilter van een type-rangende positie.* Geen uitvoerder
filtert een BEREIKTE verzameling op een gedeclareerd type. Gemeten voor
de instancevouw (`p3`, hierboven); het is dezelfde familie als de
verdeling die deze kaart wél sloot, maar op de vouw en op WF-028's
keteneindpunt. Pre-existent (de kernel maakte `wf047_binding` tweezijdig),
niet van deze kaart; het is één wijziging op beide vouwarmen plus de
uitvoerders, met een eigen meting.

*C. WF-045's populatieknoop sleutelt op de letterlijke typenaam.*
`n_pop`/`n_attr` bouwen hun knoop uit de GESCHREVEN typenaam, dus een
objectcreatie van een subtype laat de populatie van de OUDER ongezien
groeien en een cyclus die door beide loopt blijft buiten de graaf.
Pre-existent en LUID in ref (de fixpuntlus merkt de nieuwe instantie
gewoon), dus geen stille fout — maar de stratificatiegarantie is er
smaller dan haar tekst. Vraagt een eigen kaart; `declaring_type` bestaat
al als de normalisatie die dit zou gebruiken.

**Dertien gates groen** (slotregels in het sessieverslag). **Niet
gemerged**: de kaart raakt beschermde paden (`spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `spec/verifier/`, `scripts/`) en blijft dus
een voorstel op `card/subtype-verdeling-keten`.

**Eerlijke afwijkingen.** (a) De kaart vroeg de verdelings-tak van WF-034
ketengesloten te maken; die was het al, dus de reparatie landde op de
plek die het meten wél rood maakte. (b) De tien join-plekken zijn breder
dan de vijf paden die de kaart noemt: WF-053 en WF-102 zitten in de
expressielaag. Ze zijn meegenomen omdat het één en dezelfde regel is —
gebonden tak leest namen, bindingloze tak leest ⊑ — en de helft
repareren zou het bestand in een vreemdere toestand achterlaten dan het
nu is. (c) De ref/clingo-correctie van de ontvangersverzameling is
functioneel gedrag, geen verifier-werk; may-touch dekt het ("alleen
indien de meting het vergt") en de meting vergde het. (d) Mijn eerste
ronde verbreedde twee van die tien in de verkeerde richting; de lens ving
ze en ze staan hierboven bij naam. Ze waren geen van beide pre-existent:
ik heb ze in deze kaart geïntroduceerd en in deze kaart gesloten.
