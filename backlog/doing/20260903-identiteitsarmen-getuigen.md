# De getuigeloze identiteitsarmen en de ongepinde deferral-kanalen

Goal: de WF-089/090/091-familie tweezijdig pinnen waar dat nu niet kan.
Twee armen hebben geen enkele golden-getuige met een eenheiddragende of
domein-getypeerde gebruikspositie — `ParameterRef` en `DimensieCel` —
zodat over-afwijzing daar ongemeten is (de eerlijkheidsnotities in de
WF-089/090/091-indexrijen zeggen dat expliciet; de pariteitszone draagt
twaalf eenheiddragende `ParameterRef`-posities, voor `DimensieCel`
bestaat nergens een eenheiddragend voorbeeld). Daarnaast zijn twee
deferral-kanalen ongepind (kaartbijvangst 2026-09-03): een
`AttribuutRef` met `objecttype: null` defereert de motor én ontloopt de
resolutiecheck — ref faalt er hard op, nul corpusgebruik; en het
Onbekend-sentinel mag alleen op het regeldoel staan.

Aanpak: eerst meten of een eenheiddragende `ParameterRef`- en
`DimensieCel`-golden door alle drie executors gedragen wordt; dan
goldens toevoegen en de eerlijkheidsnotities inkorten; dan per
deferral-kanaal beslissen — een WF-regel (bijv. `objecttype` verplicht
op een `AttribuutRef`) of een gedocumenteerde deferral met negatief —
als genummerde vraag aan de maintainer.

Acceptance:
- Per arm een golden-getuige of een gemotiveerde
  onmogelijkheidsnotitie in de indexrij.
- Per deferral-kanaal een dichting of een expliciet gedocumenteerde
  deferral, elk met artefact.
- Alle twaalf gates groen; herpin bij normatieve wijziging.

May touch: `corpus/golden/**`, `corpus/negatief/**`, `spec/IR-SPEC.md`
(voorstel), `spec/verifier/**` (voorstel), `backlog/`;
`backends/clingo/**` alleen als dragerverbreding nodig blijkt — dan
eerst de vraag stellen.

Out of scope: Drools, frontends, bestaande corpuspins.

## Draagbaarheidsmeting (2026-09-04), vóór de bouw

Vier vormen, elk als proefpaar in /tmp gebouwd met de eigen frontend en
door alle vier de poorten gehaald. Letterlijk, per executor:

| vorm | verifier | ref | clingo | Drools |
|---|---|---|---|---|
| eenheiddragende parameter (`€`, `kg`), gelezen in een vergelijking én in een optelling | `verifier: PASS` | `oracle: PASS` | `clingo: PASS` | `PLAYED 1` |
| eenheiddragend, gedimensioneerd attribuut, gelezen per dimensiecel | `verifier: PASS` | `EVALUATION FAULT: expression kind not implemented in this card` | `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)` | `EXCLUDE dimensioned attribute input 'vangst' (§3.6, a Map bean member) is not carried by the v0 backend` |
| `t`-gedeclareerde parameter vergeleken met een `kg`-attribuut (beide zijden waar) | `verifier: PASS` | `oracle: PASS` | `TRANSLATION FAULT: non-identical units under a comparison are not translated (ref restates with an exact conversion factor this integer encoding does not carry)` | `PLAYED 1` |
| samengestelde eenheid `€/kg` maal een `kg`-attribuut (de noemer valt weg) | `verifier: PASS` | `oracle: PASS` | `TRANSLATION FAULT: non-identical units under the write to "baakgeld" are not translated (…)` | `PLAYED 1` |

Gevolg, zonder onderhandeling: alleen vorm 1 kan een spelende golden
worden. Vorm 2 kan geen case dragen — daar komt bij dat `uchar-case-v0`
geen celinvoer kent (de waardevarianten zijn number/date/dateTime/
boolean/text/enumeration/constructor/list/struct) — dus wordt het een
tweede verifier-only getuige naast het vormenkabinet. Vorm 3 en 4 zijn
niet ingebouwd: een golden moet door ALLE poorten, en die twee maken
poort 6 rood. Ze staan als gemeten grens in de NOTE van de golden.

## De twee goldens en hun handberekening

**`jutterswet-aanvoergeld`** (speelt, `case.json`, 7 verwachtingen).
Parameters `aanvoerdrempel` = 500,00 kg en `aanvoertarief` = 12,50 €;
beide vergelijkingsoperanden in `kg`, beide opteloperanden in `EUR`, dus
nergens een herformulering, en de sommen zijn exact op twee decimalen
tegen een tweedecimalig doel — de ch. 2.4-toekenningspoort rondt niets.

| Jutter | gewicht | ≥ 500,00? | aanvoerplichtig | waarde | aanvoergeld |
|---|---|---|---|---|---|
| J1 | 640,25 kg | ja | aanwezig | 310,00 € | 310,00 + 12,50 = **322,50** |
| J2 | 499,99 kg | nee | afwezig | 87,40 € | 87,40 + 12,50 = **99,90** |
| J3 | 500,00 kg | ja (de grens) | aanwezig | 0,00 € | 0,00 + 12,50 = **12,50** |

Met de hand berekend vóór de eerste run; ref bevestigt ze
(`oracle: PASS (7 expectations)`), clingo en Drools spelen dezelfde pin.

**`jutterswet-seizoenvondst`** (verifier-only, geen case). Geen pin, dus
geen berekening: de getuige is de vorm, niet de waarde. De NOTE draagt
de drie executorweigeringen letterlijk, zodat "niet afgespeeld" een
gemeten uitspraak is en geen stilte.

## Het tweezijdige pin-bewijs, per arm

**Kant 1 — de golden is schoon en de arm vuurt er wél op.** Motor met
een arm-mutant die op ELKE eenheiddragende gebruikspositie van één arm
vuurt (de meetmethode van eerlijkheidsnotitie 3), over golden + rood:

    ParameterRef  arm-mutant -> RED: corpus/golden/jutterswet-aanvoergeld/ir.json   (1 document)
    DimensieCel   arm-mutant -> RED: corpus/golden/jutterswet-seizoenvondst/ir.json (1 document)

Vóór deze kaart: nul documenten, beide armen. Pariteitszone read-only,
zelfde mutant: 6 documenten op de `ParameterRef`-arm, 0 op de cel-arm.

**Kant 2 — de leugen wordt geweigerd, arm-exact.** Een `t`-voor-`kg`-kopie
(één canonieke eenheid, factor 1000: de converteerbare klasse die
WF-035/WF-023 per constructie doorlaten) op precies die arm, in een
/tmp-KOPIE van de nieuwe golden, door de ONgewijzigde verifier:

    WF-091 FAIL /resolvedRegels/0/voorwaarde/rechts/type: unit mismatch: parameter reference 'aanvoerdrempel' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'), but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1 to 'kg')
    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: dimension-cell read of 'vondstgewicht' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'), but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1 to 'kg')

**Controle — de armen zijn dragend.** Met beide callsites gestubd (`[]`)
blijven alle goldens en rood-documenten schoon (stubben kan geen
overtreding maken) en houden precies zes negatiefs op geweigerd te
worden: `wf089-parameterref-enum`, `wf089-dimensiecel-enum`,
`wf090-parameterref`, `wf090-dimensiecel`, `wf091-parameterref-eenheid`,
`wf091-dimensiecel-eenheid`. Beide richtingen zijn nu falsifieerbaar.

## De indexrij-getallen, nagemeten

Met een tijdelijke per-arm-sonde in een /tmp-kopie van de motor, die de
telling van de rij eerst REPRODUCEERT op de 172 documenten van de vorige
meting (826 golden + 95 rood = 921 uitgevoerd, 38 eenheiddragend, 71
deferrals, 14 aggregatievergelijkingen — exact). Daarna met de twee
nieuwe goldens erbij: **929** uitgevoerde vergelijkingen over **174**
documenten (834 golden + 95 rood), **46** eenheiddragend, **72**
deferrals; het aggregatiegetal (14) verandert niet. De rij dateert haar
telling niet als momentopname maar noemt documentaantallen, dus is
hermeten de juiste keuze; de pariteitszin (147/30/30) is niet van deze
kaart en blijft staan.

Eén afwijking, eerlijk gemeld en NIET aangepast: dezelfde sonde telt in
de pariteitszone **6** aggregatievergelijkingen waar de rij er 5 noemt
(golden: 14 = 14, exact). Beide tellingen zijn "uitgevoerde
vergelijkingen" — geen enkele aggregatie-invocatie in die zone defereert
— dus het lijkt een telfout van de vorige meting. Buiten scope van deze
kaart; ter beoordeling van de maintainer.

## Poorten

Gates 1–8 en 10–12 gedraaid, alle groen:

    1  check-self-contained: PASS
    2  check-ir-schema: PASS (schema valid; 174 file(s) validate and round-trip)
    3  check-verifier: PASS (174 golden clean; 234 negatief rejected for their declared rule)
    4  check-case-schema: PASS (schema valid; 425 case file(s) validate)
    5  check-oracle: PASS (403 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
    6  check-clingo: PASS (403 cases replay exactly through clingo — ref = clingo over the corpus)
    7  check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    8  check-rood: PASS (22 red cases fault observably in ref and clingo); Drools leg PASS (5 fault-pinned, 17 not-carried with reason)
    10 check-regelspraak: PASS — JUnit suite green (159 tests); 28 rgs golden(s) re-emit byte-identically
    11 check-toka-parity: PASS (45 fragmenten byte-identiek, 2 holdouts; 29 replay-slices door ref/clingo/Drools, nul stille divergenties)
    12 check-drools: PASS — played 106, auto-excluded 43, value-divergence 1, no-case 2, JUnit 178 tests

Gate 12-telling: +1 played (`jutterswet-aanvoergeld`) en +1 no-case
(`jutterswet-seizoenvondst`); het gate-rapport noemt de laatste met reden
(`no-case  jutterswet-seizoenvondst (a golden without a case file — not
playable)`). Gate 9 (catala) is op deze machine NIET gedraaid: `catala`
en `clerk` staan niet op PATH — de reviewersplicht op een
catala-dragende machine, conform CLAUDE.md.

## Deel 2 — de twee deferral-kanalen: alleen gemeten

Niets gedicht; geen regel toegevoegd; `spec/verifier/**` onaangeraakt.
Alle reproducties zijn /tmp-mutaties van een corpus-golden.

### Kanaal (a): `AttribuutRef` met `objecttype: null`

**Gemeten dracht: nul.** Geen enkel document draagt de vorm — 152 golden
+ 22 rood + 234 negatief + 30 pariteit, alle nul.

**Wat de vorm doet.** `resolved_attr_datatype` levert `None` zodra
`objecttype` afwezig is, en de WF-034/WF-053/WF-080-tak van de
`AttribuutRef`-arm staat zelf in een `match otn with Some … | None -> []`.
Eén afwezig veld ontwapent dus tegelijk de naamresolutie, de
navigatiecheck, de kenmerk-in-waardepositie-check én de HELE
identiteitsfamilie (WF-089/090/091 defereren op `decl = None`). Het is
geen deferral van één regel maar een generieke vrijstelling.

**Reproductie** (`jutterswet-aanvoergeld` met `objecttype` op één
referentie op null, schema-geldig gecontroleerd):

    verifier : verifier: PASS                                        (ook mét de t-voor-kg-leugen erbij)
    ref      : EVALUATION FAULT: missing AttribuutRef.objecttype in IR node
    clingo   : TRANSLATION FAULT: missing objecttype
    Drools   : PLAYED 1                                              (waarheidsgetrouwe eenheid: alle 7 verwachtingen kloppen)
    Drools   : FAIL case.json: kenmerk aanvoerplichtig: verwacht afwezig, werkelijk aanwezig   (mét de t-voor-kg-leugen)

Controle: dezelfde eenheidsleugen MÉT `objecttype` gevuld wordt wel
geweigerd (`WF-091 FAIL … attribute reference 'aangevoerd gewicht' is
typed 't' …`). De leugen is dus niet onschuldig — hij is onzichtbaar
gemaakt door het lege veld.

**Dat is geen theoretische deferral maar een gemeten verkeerd-antwoordklasse:**
verifier stil, twee executors weigeren het document om een ANDERE reden,
en de derde rekent door en geeft een ander antwoord.

**Frontend-bereikbaarheid, gemeten.** De eigen frontend produceert de
vorm wel (`het aangevoerd gewicht van de Spookjutter` →
`objecttype: null`), maar altijd SAMEN met een `Onbekend` knooptype, en
dat vangt WF-034 al (`unknown-type sentinel inside a structured node`).
De gevaarlijke variant — leeg `objecttype` met een echt, eenheiddragend
type — is met deze frontend niet te maken; ze is wel schema-geldig en
staat elke andere conforme emitter vrij. Dat is precies het publiek van
een engine-neutrale IR-spec.

**Opties, met exacte consequentie.**

* **(A) Nieuwe regel — eerstvolgende vrije nummer is `WF-092`** (de index
  loopt door tot WF-091): "een `AttributeReference` noemt het objecttype
  waarop zijn waarde leeft". Kosten: één clausule in de motor (de
  bestaande `| None -> []`-tak wordt een melding), één negatief
  (`wf092-attribuutref-zonder-objecttype`, rood-eerst), nul corpuswijziging
  (dracht is nul → geen herpin, geen poortverschuiving). Spec: een rij in
  de WF-index en een zin bij het `AttributeReference`-knooppunt in ch. 4.1.
  Schema: NIET nodig — `objecttype` is daar `["string","null"]` net als
  bijna elk ander referentieveld; één veld verscherpen breekt met de
  huisstijl "de wire is toegeeflijk, de verifier is normatief". Wie het
  tóch wil, verandert precies die ene `anyOf` in `$defs/AttribuutRef`.
* **(A′) Geen nieuw nummer — WF-034 verbreden.** De zin van WF-034 is nu
  al "every expression/condition reference resolves"; een referentie die
  geen objecttype noemt, resolveert nergens. Kosten: één clausule, één
  negatief (`wf034-attribuutref-zonder-objecttype`), één regel erbij in
  de gate-kolom van de WF-034-rij; geen nieuw nummer.
* **(B) Gedocumenteerde deferral.** Een zin in ch. 4.1 die vastlegt dat
  de identiteitsfamilie defereert zonder `objecttype`, met de ref-fout
  als vangnet. Consequentie, gemeten: dat vangnet BESTAAT NIET — Drools
  weigert niet, hij antwoordt (en met de leugen erbij: verkeerd). Een
  rood-document zou ref+clingo kunnen pinnen (beide falen observeerbaar,
  de twee fragmenten hierboven) maar de derde executor zou als
  "not-carried" of als divergentie in het rapport staan terwijl hij in
  werkelijkheid gewoon doorrekent. B legt dus een onwaarheid vast.

**Technische voorkeur: A′, met A als alternatief.** Argument: (i) de
dracht is nul, dus over-afwijzing is per meting onmogelijk; (ii) de
kosten zijn één clausule en één negatief; (iii) het gaat niet om een
gemiste optimalisatie maar om een gemeten verkeerd antwoord in een
executor; (iv) A′ vraagt geen nieuw regelnummer omdat WF-034's bestaande
zin de eis al uitspreekt — het huis heeft bovendien precedent voor
"X zonder Y"-meldingen binnen de eigen regel van het construct (WF-027
"object creation without a created type", WF-030 "uniqueness without a
subject type", WF-035 "clamp without any bound"). Wie de dichting los
citeerbaar wil hebben, kiest A.

### Kanaal (b): het `Onbekend`-sentinel op het regeldoel-type

**Gemeten dracht: nul** in alle vier de zones (zelfde telling als boven).

**Reproductie** (`jutterswet-aanvoergeld`, `resolvedRegels/1/doel/type`
vervangen door `{"node":"Onbekend","reden":"niet opgelost"}`,
schema-geldig gecontroleerd):

    verifier : verifier: PASS
    ref      : oracle: PASS (7 expectations)
    clingo   : clingo: PASS (7 expectations)
    Drools   : PLAYED 1

Controles, die laten zien dat dit de ENIGE positie is waar het sentinel
overleeft: hetzelfde sentinel op een gewandeld expressietype geeft
`WF-034 FAIL … unknown-type sentinel inside a structured node`, en op een
attribuutdeclaratie `WF-003 FAIL … unresolved datatype (v0 'Onbekend'
sentinel: niet opgelost)`.

**Wat het kost.** Geen verkeerd antwoord vandaag: geen enkele executor
leest het doel-type (ze lezen alle drie de GEDECLAREERDE datatype, wat de
WF-023-rij ook al zegt). Wat het wél doet: het is de uitknop van de
WF-091-regeldoelarm. Die arm kruist het doeltype tegen de declaratie en
defereert op het sentinel — een emitter die `Onbekend` schrijft, schakelt
de eenheidscontrole op zijn schrijfdoelen uit en blijft verifier-schoon.

**Frontend-bereikbaarheid, gemeten.** De frontend zet `Onbekend` op een
doel alleen wanneer het doelattribuut niet is gedeclareerd — en dan vangt
WF-022 het document al (`target 'spookbedrag' is neither an attribute nor
a characteristic of 'Jutter'`). In een WF-022-schoon document is de vorm
met deze frontend niet te maken; met een andere emitter wel.

**Opties.**

* **(A) WF-034 verbreden tot het doel-type.** De zin van WF-034 is precies
  "no unknown-type sentinel" en zijn gate-kolom zegt al "sentinel types";
  het doel-type is de ene positie die de wandeling niet raakt (de lvalue
  is WF-022's). Kosten: één clausule op de bestaande
  regeldoel-registratie, één negatief (`wf034-doeltype-onbekend`), nul
  corpuswijziging (over-afwijzing per meting onmogelijk: dracht nul).
* **(B) Expliciet toestaan + documenteren.** Een zin die vastlegt dat het
  doel-type adviserend is en elke consument de DECLARATIE leest. Kosten:
  goedkoop, maar het pint niets — een niet-weigering is met een negatief
  niet te pinnen, en een golden die het sentinel draagt zou een leugen op
  de wire zegenen. Bovendien botst het met de WF-091-regeldoelarm, die
  het doel-type juist wél normatief kruist: dan zijn er twee zinnen over
  hetzelfde veld, waarvan één zegt "adviserend" en de andere "identiek".

**Technische voorkeur: A.** Het sentinel wordt op ELKE andere positie
geweigerd; het doel is een inconsistentie, geen ontwerp. De prijs is één
clausule en één negatief, de gemeten over-afwijzing is nul, en het haalt
een zelfbedieningsvrijstelling weg uit de enige identiteitsarm die
vandaag nog uitgezet kan worden.

### De twee kanalen samen, voor de maintainer

Ze zijn hetzelfde ding op twee plekken: **een veld leegmaken of van een
sentinel voorzien is vandaag een zelfbediende vrijstelling van de
identiteitsfamilie**, schema-geldig en verifier-schoon. Kanaal (a) is
bovendien gemeten antwoordveranderend in één van de drie executors.
Beide dichtingen kosten samen twee clausules en twee negatiefs, en per
meting nul corpuswijziging.
