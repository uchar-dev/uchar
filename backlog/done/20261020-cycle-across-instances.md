# cycle-across-instances: een waarde die dezelfde waarde van een ander object van hetzelfde type leest, weigert `check` als kring, ook als de keten in de casus eindig is

*Grens gevonden door een regelsetbouwer, 2026-10-02 (brug K12 van de regelset herleving-WW v2, clusterdocument G7/G9,
V1), nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a`
(`bin/regelspraak check|scenario|cases`) en ref en clingo uit `_build`. Luid: `check` exit 1, geen fout antwoord. Een
keuze van de eigenaar; geen bouwdatum.*

## Aanleiding

De afhankelijkheidsgraaf heeft per objecttype en attribuut één knoop (RUNTIME-SEMANTICS ch. 6.1: "**attribute** — object
type × attribute"), en WF-045 laat een kring alleen toe door een recursieve creatie (ch. 6.2). Een regel die een waarde van
een object afleidt uit dezelfde waarde van een ander object van hetzelfde type, via een feittype met twee rollen op dat
type, sluit daardoor een kring, ook als de schakels in de casus een eindige keten vormen. De WW kent zo'n norm: de duur van
een nieuw recht wordt verlengd met het restant van het eerdere recht (art. 42b WW), en dat restant hangt af van de
maxdatum van het eerdere recht. De regelset zet de maxdatum na de verlenging daarom in een eigen lid naast de maxdatum
(brug K12): de maxdatum van het recht draagt de verlenging niet, en een volgende casus moet de verlengde maxdatum als
opgave meegeven.

Getuige:

    Eenheidsysteem Tijd
        de dag dg

    Objecttype de Vergunning (mv: Vergunningen)
        de stopdag Datum in dagen;
        de basiseinddag Datum in dagen;
        de einddag Datum in dagen;
        het restant Numeriek (geheel getal) met eenheid dg;

    Feittype vorige vergunning
        de vergunning Vergunning
        de vorige vergunning Vergunning
    Eén vergunning heeft één vorige vergunning

    Regel restant
        geldig altijd
            Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen
            indien hij aan alle volgende voorwaarden voldoet:
            - zijn stopdag is gevuld
            - zijn einddag is gevuld
            - zijn stopdag is eerder dan zijn einddag.

    Regel einddag
        geldig altijd
            De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning
            indien het restant van de vorige vergunning van de Vergunning gevuld is.

    Regel einddag zonder restant
        geldig altijd
            De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
            indien het restant van de vorige vergunning van de Vergunning leeg is.

    Casus: V1 (stopdag 01-03-2026, basiseinddag 01-06-2026, geen vorige), V2 (basiseinddag 01-12-2026, vorige vergunning
    V1). Per object: einddag V1 = 01-06-2026, restant V1 = 92 dg, einddag V2 = 03-03-2027.

    R1  zoals hierboven.
        nu: check exit 1, twee ERRORs: "ERROR (line 15): regel 'restant' hangt via een kring van zichzelf af; de
        afhankelijkheden zijn kringvrij buiten een recursieve creatie (WF-045), en een kring draagt geen negatieve rand,
        zoals een ontkenning (WF-046): dependency cycle through no recursive creation: restant -> einddag -> restant", en
        dezelfde met "restant -> einddag zonder restant -> restant". Zonder de tweede regel (geen leeg-toets) blijft de
        eerste ERROR.
    R2  de brug: "De einddag van een Vergunning moet gesteld worden op zijn basiseinddag", en een lid `verlengde einddag`
        met de eerste regel van R1.
        nu: check exit 0; scenario PASS (restant V1 92 dg, verlengde einddag V2 03-03-2027); ref en clingo PASS.

## Doel

De eigenaar kiest één van twee wegen.
- (a) De grens blijft. RUNTIME-SEMANTICS ch. 6.2 noemt dit geval als voorbeeld, en de weigering in `check` noemt het
  feittype dat twee objecten van één type verbindt, zodat de auteur ziet waarom de kring er is en dat een eigen lid de
  uitweg is.
- (b) Een stratificatie per object langs zo'n relatie: WF-045 laat een kring toe die alleen via een relatie tussen objecten
  van één type naar hetzelfde attribuut terugkeert, zoals hij nu een kring door een recursieve creatie toelaat, mits de
  schakels van die relatie in de casus geen kring vormen; een kring in de schakels (V1 heeft V2 als vorige en V2 heeft V1)
  is een benoemde fout bij het laden of de evaluatie. Eerst een spec-voorstel (ch. 6.1–6.3, de verifier), dan ref, clingo
  en beide Drools-takken. Rood-eerst met R1: `check` exit 0 en einddag V2 03-03-2027 in ref ≡ clingo ≡ Drools; de casus met
  een kring in de schakels geeft in alle drie dezelfde benoemde fout; alle gates groen.

## Voorlopig besluit (2026-10-22, naar het Fable-advies): weg (b), scherp afgebakend

Bron: `uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 1 (B1.1–B1.5), met metingen in `proef/k12/`. De
eigenaar kan het besluit omdraaien; het is een normwijziging van ch. 5.2 en ch. 6.

- **Afbakening.** Een *schakelrelatie* R is een feittype waarvan beide rollen door vergelijkbare typen gevuld worden,
  waarvan de gelezen rol to-one is en dat geen feitcreatie schrijft. Een *satellietstap* gaat over een feittype waarvan
  de rol terug naar het type van de kring to-one is. Een *verschoven lezing* is een genavigeerde lezing (attribuut,
  kenmerk, of het bereik van een vouw) waarvan het pad begint met precies één R-stap en verder alleen satellietstappen
  draagt. Een *verschoven rand* p → c is een rand waarvan elke leespositie in c van de knoop die p schrijft een
  verschoven lezing is.
- **WF-045′/046′/051′.** De graaf is kringvrij na het weglaten van de creatievormen van recursieve groepen en van de
  verschoven randen; negatieve verschoven randen (een leeg-toets, een ontkende kenmerkcheck op de voorganger) zijn op zo'n
  kring toegelaten; hetzelfde voor de defaults-vouw (WF-051).
- **Dynamisch:** de schakelgraaf van R over de casusinstanties is kringvrij; een kring daarin is een fout bij het laden,
  vóór elke evaluatie: nieuw EF-032 (of het volgende vrije nummer, als een eerdere landing het nam), parameters
  `factType` en `cycle`, in ref, clingo en Drools. Clingo rekent de keten zonder wijziging aan de vertaling (gemeten
  PASS, met en zonder leeg-toets), maar het gronden van een schakelkring eindigt niet: de runner toetst de schakelgraaf
  vóór het gronden.
- **Evaluatie:** de vormen van zo'n kring rekenen per object in dieptevolgorde (ref: substrata (k, d); Drools: een
  verschoven rand telt in `Cycles.strata` als positieve kringrand, zoals een creatiekring, en truth maintenance trekt een
  voorlopige waarde in zodra de voorganger staat; blijft een vorm volgordeafhankelijk, dan weigert de backend haar bij
  naam). De lezingen van ch. 5.12 en gate 17 veranderen niet.
- **Geen nieuw wire-veld** (gate 2); alle bestaande WF-045/046/051-getuigen blijven rood (gate 3); de goldens spelen
  byte-identiek; de randen van gate 15 veranderen niet. Ch. 5.2 en ch. 6.2 noemen twee gecontroleerde uitzonderingen in
  plaats van één.

**Snede van de hoofdsessie (afwijkend van het advies, dat vijf kaarten noemt):** deze kaart draagt spec, verifier, ref,
clingo en Drools op één tak en landt in één keer, omdat een golden bij zijn landing in gate 5, 6 en 12 moet spelen. De
frontend (`check` laat de vorm toe, en de betere weigering voor wat buiten de afbakening valt) is de kaart
`check-admits-instance-cycle`, na deze. Tot die landt, is de golden handgeschreven (geen `source.rgs`), zoals het IR in
`proef/k12/` van het advies.

Rood eerst: golden `jutterswet-vergunningketen` (getuige R1, drie casussen: zonder voorganger, keten van twee, keten van
drie; met de leeg-toets en in de positieve vorm), gate 3 rood vóór en groen erna; de vijf negatieven uit het advies (R-stap
to-many, R door een feitcreatie geschreven, gedeelde satelliet, dezelfde knoop ook rechtstreeks gelezen, R-stap gevolgd
door een to-many stap); een rood geval met een schakelkring (`expectedFault` EF-032) in ref, clingo en Drools; het
gewerkte voorbeeld van ch. 6.4 als ref-test; `EXPECTED_PLAYED` plus één. De CONSTRUCTS-rij als tekst in de Result.
Start pas als `date-with-in-ref-and-clingo` geland is (beide raken `spec/RUNTIME-SEMANTICS.md`, ref en clingo).

## Voorlopige besluiten van de bouwer (2026-10-22)

*Normkeuzes die het advies niet beslist en die de bouw nodig had; de eigenaar kan ze omdraaien. Elk staat in IR-SPEC
ch. 5.3 met reden en getuige.*

1. **Een stap reikt naar één.** Een satellietstap is een stap over een satellietrelatie (eigenaarsrol to-one, zoals het
   advies) én bereikt een to-one rol; dat geldt voor elke stap van een verschoven of lokale lezing. Anders is de
   vijfde negatieve ("R-stap gevolgd door een to-many stap") niet te onderscheiden van een toegelaten vorm. Een vouw over
   de satellieten van de voorganger is gezond (zij hebben één diepte) en blijft een latere uitbreiding, zoals een
   verzameling voorgangers.
2. **Lokaal is smal, en verplicht.** Het advies laat elke gewone rand binnen de kring toe zolang de graaf zonder de
   verschoven randen kringvrij is. Dat is niet gezond: een regel in de kring die de einddag van álle Vergunningen leest
   (een vouw over de populatie) sluit per object een echte kring (restant(x) ← einddag(y) ← restant(x) met x de vorige
   van y) die de statische toets dan niet ziet. Daarom moet elke lezing achter een interne rand verschoven of lokaal
   zijn: lokaal is geen stap, of één stap over een satellietrelatie, heen of terug. Twee stappen (naar een satelliet en
   terug), een vouw over de eigen satellieten en elke andere relatie maken het deel ongeordend. Getuige
   `wf045-instance-cycle-non-local-read`.
3. **Eén schakelrelatie, één rol per deel.** Alle verschoven lezingen van een deel lopen over dezelfde relatie naar
   dezelfde rol. Een deel dat de voorganger én de opvolger leest (dezelfde relatie, de andere rol) sluit per object een
   echte kring; het advies sluit dat niet uit. Getuige `wf045-instance-cycle-predecessor-and-successor`.
4. **Eén satellietrelatie per satelliettype in een deel**, anders kan een satelliet twee eigenaars van verschillende
   diepte hebben. Getuige `wf045-instance-cycle-two-satellite-relations`.
5. **Alleen regels en defaults-groepen**, en geen deel met een creatie van een recursieve groep (de twee uitzonderingen
   disjunct, zoals het advies schrijft). Een reciprook feittype is geen schakelrelatie (zijn schakels lopen beide kanten
   op).
6. **EF-033** staat op het kanaal `case input` (de schakels zijn invoer; geen vorm schrijft ze). `factType` is de
   gedeclareerde naam, `cycle` de labels van de kring vanaf het kleinste label (bytevolgorde) langs de rol naar de
   voorganger, gescheiden door `, `; bij meer kringen die met het kleinste label. In ref en clingo staat de toets in de
   case-lezer die zij delen, in Drools in `CaseInput.of`.
7. **Diepte en niveau.** Een satelliet staat op de diepte van haar eigenaar; een gemaakt object (creatie) op 0. In ref
   rekent een deel binnen zijn stratum per diepte en per diepte per niveau (de langste weg over de niet-verschoven
   interne randen, een strikte of negatieve rand telt één); Drools ordent niet en laat truth maintenance terugnemen
   (gemeten, zie Result).

## Result (2026-10-22)

Tak `card/cycle-across-instances`, vanaf `f4a55f8a`. Geraakt: `spec/verifier/lib/checks.ml`, `ref/lib/{case,eval}.ml`,
`ref/test/test_ref.ml`, `backends/drools/` (`Cycles`, `TmsTranspiler`, `CaseInput`, `CaseRunner`, beide catalogi,
`TmsCycleVerifierWitnessTest`), `scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), als voorstel `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md`, het corpus (één golden, acht negatieven, één rood geval) en deze kaart. Niet geraakt:
`frontends/`, het IR-schema, `backends/clingo/` (de vertaling niet en de runner niet: de toets zit in de case-lezer die
clingo met ref deelt), `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`, `toka/`.

- `1fc679b3b` de kaart naar `doing/`.
- `4a7d8c4e4` verifier: het pad per lezing, `instance_ordering`, WF-045/046/051 zonder verschoven randen; de golden en
  de vijf negatieven van het advies.
- `ae5093047` ref: de dieptevolgorde in `evaluate`, EF-033 in de gedeelde case-lezer, WV-6.4-i/j/k; het rode geval.
- `051d4b2d3` Drools: `Cycles.orders`, EF-033 in `CaseInput`, de catalogi, `EXPECTED_PLAYED` 268, de witnesstest.
- `2cf11ed01` spec-voorstel: IR-SPEC ch. 4.6, 5.2, 5.3 en de index; RUNTIME-SEMANTICS ch. 5.2, 5.7b, 6.1–6.4.
- `e437b74a1` WV-6.4-l (defaults-groep op de kring), de indexrijen van WF-046/051, de alinea van ch. 5.5, de voorlopige
  besluiten in deze kaart.
- `420c44981` drie negatieven voor de clausules die ik aan het advies toevoegde (na gates-1, als fix).

### Wat er is

**Afbakening zoals gebouwd** (IR-SPEC ch. 5.3; de voorlopige besluiten hierboven). Een *schakelrelatie* R: twee rollen
met vergelijkbare vullers, de rol die een stap bereikt (de voorganger) to-one, geen vorm die haar schakels schrijft,
niet reciprook. Een *satellietrelatie* van T: een ander feittype met twee rollen, zonder schrijver, eigenaarsrol to-one
en gevuld door een type vergelijkbaar met T, de andere rol door een type dat dat niet is. Het *pad* van een lezing: de
bereiken van de vouwen waar zij in staat, dan haar eigen stappen. *Verschoven*: één stap over R naar de voorganger, dan
hooguit één stap naar de satelliet, elke stap naar een to-one rol. *Lokaal*: geen stap, of één stap over een
satellietrelatie heen of terug, naar een to-one rol. Een *instantiegeordend deel*: een sterk samenhangend deel van de
hele graaf, zonder creatie van een recursieve groep, alleen regels en defaults-groepen, met ten minste één verschoven
rand, alle verschoven lezingen over één R naar één rol, elke lezing achter een interne rand verschoven of lokaal, één
satellietrelatie per satelliettype, en kringvrij zonder zijn verschoven randen. Die randen laten WF-045, WF-046 en
WF-051 weg; `--dependency-edges` (gate 15) is ongewijzigd.

**De acht negatieven**, elk WF-045 met `expect.txt` (`WF-045.dependency-cycle` plus `cycle=`). De vijf uit het advies
zijn (op de vierde na) schoon met alleen die clausule versoepeld in het eigen document; de drie voor de eigen clausules
(voorlopige besluiten 2–4) zijn schoon met alleen die clausule versoepeld in de verifier (alles gemeten):

| negatief | clausule | schoon met |
|---|---|---|
| `wf045-instance-cycle-to-many-predecessor` | de rol naar de voorganger to-one (een som over alle vorige vergunningen) | `many: false` op de rol |
| `wf045-instance-cycle-created-relation` | geen schrijver van R (een feitcreatie legt de schakels) | de feitcreatie weg |
| `wf045-instance-cycle-shared-satellite` | eigenaarsrol to-one (een Termijn met meer houders) | `houder` to-one |
| `wf045-instance-cycle-direct-and-shifted-read` | elke lezing van de knoop verschoven (de einddag leest ook het eigen restant) | — (de rand verschoven nemen laat een kring op één object door) |
| `wf045-instance-cycle-to-many-satellite-step` | de stap na R to-one (een som over de termijnen van de voorganger) | `termijn` to-one |
| `wf045-instance-cycle-non-local-read` | elke lezing achter een interne rand verschoven of lokaal (een som over alle Vergunningen van hetzelfde register) | die lezing als lokaal geteld |
| `wf045-instance-cycle-predecessor-and-successor` | één rol (het saldo leest de voorganger, het overschot de opvolger) | elke stap over R verschoven, welke rol ook |
| `wf045-instance-cycle-two-satellite-relations` | één satellietrelatie per satelliettype (een Termijn als lopende termijn van de een en reserve van de ander) | de toets uit |

**De golden** `jutterswet-vergunningketen` (handgeschreven IR, gemaakt met de frontend via hulpattributen; `NOTE.md`
met de bron en de handberekening): de einddag met de leeg-toets (R1) en de verlengde einddag in de positieve vorm over
een satelliet Termijn; drie casussen, zonder voorganger, keten van twee (de casus van de kaart: einddag V2 03-03-2027),
keten van drie in een volgorde die noch die van het bestand noch de alfabetische is (einddag derde 01-12-2027; de
tweede zonder termijn, dus verlengde einddag derde 01-06-2027). Gate 3 rood ervoor (`g3-red.log`: WF-045 en WF-046 op
de golden), groen erna.

**ref.** De leden van een deel delen een stratum (randen op een kring tellen positief, zoals al); binnen dat stratum
rekent ref per diepte d = 0, 1, … en per diepte per niveau l, de leden van niveau ≤ l op de objecten van diepte d en
alle leden op de lagere dieptes, ieder tot hun fixpunt, en daarna het hele stratum nog eens. Diepte: 0 zonder
voorganger, 1 + die van de voorganger, een satelliet op die van haar eigenaar, een gemaakt object 0; uit de
invoerwereld. Zonder die volgorde gaf ref op een kenmerk op de kring ("zonder verlenging indien het restant van de
vorige leeg is") EF-016 (gemeten met de volgorde uitgezet); met haar het antwoord van clingo. Ref-tests WV-6.4-i (het
gewerkte voorbeeld van ch. 6.4, vier volgordes van regels en objecten, en zonder voorganger), -j (het kenmerk, twee
volgordes), -k (EF-033 bij het laden), -l (een defaults-groep op de kring: "mag opgegeven worden").

**clingo.** De vertaling verandert niet: de golden, -j en -l spelen ongewijzigd. De toets op de schakels zit in de
case-lezer die ref en clingo delen (`Case.check_link_cycles`), dus vóór het gronden; gemeten zonder die toets: clingo na
30 s nog aan het gronden, 2,2 GB, met de hand gestopt.

**Drools.** `Cycles` draagt per lezing het pad en vindt de delen zoals de verifier (`orders`); de verschoven randen
tellen niet mee in `cycles45` en `negations46`; de strata (alleen voor creaties) veranderen niet. Geen dieptevolgorde:
truth maintenance trekt een waarde op een lege voorganger in zodra de voorganger staat. `CaseInput.of` weigert een
kring in de schakels met `LinkCycleFault` (EF-033, `factType` en `cycle`), na de rollen, vóór de sessie; de red-run
geeft `FAULT EF-033 {...}`; de catalogi hebben `EF-033` in beide talen. De golden speelt op beide takken PLAYED 3.

**Volgorde in Drools, gemeten.** Alle 120 volgordes van de vijf regels van de golden en alle 24 van de vier regels van
de kenmerk-probe (WV-6.4-j): beide takken PLAYED, ref en clingo PASS (`werk/bruggen/k12/order.log`); de probe ook met de
objecten omgekeerd. `BranchDumpsCorpusWitnessTest` groen (beide rijen, met de golden en het rode geval). Geen vorm
bleef volgordeafhankelijk, dus geen weigering bij naam nodig.

**EF-033** in ch. 5.7b: kanaal `case input`, parameters `factType`, `cycle`, getuige `corpus/red/instance-order-link-cycle`
(drie casussen: twee vergunningen, drie in een ongesorteerde kring, een eigen voorganger), in ref, clingo en Drools.

### Gates

De volledige set (`werk/gates.sh`), twee keer: `gates-1` op `e437b74a1`, EXIT 0; na de fix (drie negatieven) `gates-2`
op `420c44981`, **EXIT 0** (2026-10-22 12:52–13:27), alle zeventien rc=0. Uit `gates-2`: gate 3 361 goldens en rode
documenten schoon, 522 negatieven geweigerd op hun regel, 365 op hun arm; gate 5 1361 gewerkte voorbeelden (was 1343,
plus WV-6.4-i/j/k/l), 859 casussen exact; gate 6 859 casussen ref ≡ clingo; gate 8 57 rode gevallen in ref en clingo, 34
op de Drools-poot met hun code (EF-033 erbij); gate 12 PLAYED 268 op beide takken, de suite groen; gate 15 de randen
ongewijzigd; gate 17 268 goldens op beide takken, 0 afwijkingen. Tussendoor los: gate 3 rood vóór de verifierwijziging
(`g3-red.log`), `BranchDumpsCorpusWitnessTest` en `TmsCycleVerifierWitnessTest` alleen.

### Meting 5 (geen bestand in de repository)

De v2-regelset herleving-WW (fundament plus regels, 40 regels en 19 defaults-groepen), geëmitteerd met de frontend van
deze tak: verifier PASS. Met de verlenging in de maxdatum van het recht (de berekende maxdatum plus de duurverlenging,
de regel en het lid `verlengde maxdatum` weg, IR met de hand) weigert de verifier: WF-045 en WF-046. De kring is één
deel van 17 vormen (maxdatum, beëindigingsdatum, herleving, herlevingstermijn, duurverlenging, resterende
uitkeringsduur en hun groepen), de schakelrelatie `eerdere recht` wordt herkend en vier lezingen van de duurverlenging
over het eerdere recht zijn verschoven, maar vier lezingen achter interne randen zijn noch verschoven noch lokaal:

1. de herlevingsdatum en `vrijstelling op grond-van-leeftijd` van de eindigingsgronden van het recht (een vouw over een
   to-many satellietstap, in "herlevingsdatum van het recht", "herleving" en "herlevingstermijn buiten toepassing");
2. de beëindigingsdatum van het recht gelezen vanaf een inkomen in een kalendermaand (twee stappen: naar het inkomen en
   terug naar het recht, in "eerste kalendermaand zonder overschrijding …");
3. de beëindigingsdatum van een ander recht via de samenloop (twee stappen naar een ander object van hetzelfde type, in
   "herleving").

ref komt er dus niet aan toe (het herhaalt WF-045 bij het laden). Stap 6 van het advies (K12 en N13 vervallen) kan na
deze kaart nog niet: daarvoor moet de afbakening ten minste vouwen over to-many satellieten (1) en de stap heen en terug
naar dezelfde eigenaar (2) als lokaal tellen, en voor (3) is een normkeuze nodig (een lezing over de samenloop leest een
object van een andere keten, dat de diepte niet ordent). Diagnose in `werk/bruggen/k12/m5/diag.txt`.

### Vondsten

1. **Drie gaten in de afbakening van het advies**, gedicht in deze kaart (voorlopige besluiten 2–4), elk met een
   negatief: zonder de eis dat elke interne lezing verschoven of lokaal is laat de statische toets een echte kring per
   object door (`wf045-instance-cycle-non-local-read`: een som over het register, dat ook de opvolger bevat); zonder
   "één rol" ook (`…-predecessor-and-successor`); zonder "één satellietrelatie per satelliettype" kan een satelliet twee
   eigenaars van verschillende diepte hebben (`…-two-satellite-relations`). Eerst na gates-1 gezien; daarom gates-2.
2. **Bestaande test gewijzigd, met reden:** `TmsCycleVerifierWitnessTest.each_negative_document_of_wf045_wf046_and_wf051_is_refused`
   telde 22 negatieven met een gelijkheid; het zijn er nu 30 (de acht nieuwe, alle geweigerd). Een test erbij:
   `the_instance_ordered_cycle_is_admitted`.
3. Geen bug buiten de kaart gevonden in spec, verifier, ref, clingo of Drools.

### Wat niet gedaan, met reden

- De frontend (`check` laat de kring toe, de golden een `source.rgs`): de kaart `check-admits-instance-cycle`.
- Een golden voor WF-051 op de kring: de opdracht vraagt één golden; de defaults-groep staat als ref-test WV-6.4-l en
  is in alle vier uitvoerders gemeten.
- Een verzameling voorgangers, vouwen over satellieten, meer satellietstappen achter elkaar: latere uitbreidingen
  (ch. 5.3), nodig voor meting 5 (zie boven).
- Drools ordent niet naar diepte; truth maintenance volstaat op alle gemeten volgordes.

### Voorgestelde CONSTRUCTS-rij (voor de hoofdsessie)

| 45 | A value of an object that reads the same value of its predecessor — the instance-ordered cycle | statute rules that extend a right by what remains of the earlier one (art. 42b WW: the duration of a new right is extended by the remaining duration of the ended right); in RegelSpraak a rule over a fact type with two roles on one type, "het restant van de vorige vergunning van de Vergunning" | core | kernel: a second controlled exception to WF-045, beside recursive creation | v0 warns on a circular reference at build time and fires up to a cap; until this row v1 refused every such cycle (WF-045, and WF-046 where a leeg-test closes it). **Landed (2026-10-22, card `cycle-across-instances`; voorlopig besluit):** IR-SPEC ch. 5.3 defines the switch relation (two roles on comparable types, the predecessor's role to-one, no writer), the satellite relation, the path of a read, shifted and local reads and the instance-ordered part; WF-045, WF-046 and WF-051 walk the graph without its shifted edges, `--dependency-edges` unchanged. RUNTIME-SEMANTICS ch. 6.2: the members evaluate per object by depth in the chain and per depth by level; a cycle in the links is EF-033 at case input, in ref, clingo and Drools. Witnesses: golden `jutterswet-vergunningketen` (gates 3, 5, 6, 12, 17), eight `wf045-instance-cycle-*` negatives, red `instance-order-link-cycle`, `ref/` tests WV-6.4-i to -l | out: a set of predecessors, folds over satellites, two-step local reads and reads over another relation inside the cycle; the RegelSpraak frontend still refuses the form (card `check-admits-instance-cycle`) |

### Voor de kaart `check-admits-instance-cycle`

- De afbakening staat op één plek per uitvoerder: `instance_ordering` in `spec/verifier/lib/checks.ml` en `Cycles.orders`
  in de Drools-backend. `Dependencies.java` moet precies dezelfde set toelaten: het pad per lezing (vouwbereik plus eigen
  stappen), de klassen verschoven en lokaal, de zeven voorwaarden van een deel. De acht negatieven zijn de toets dat
  `check` niets meer toelaat.
- De golden is geëmitteerd met deze frontend en daarna met de hand gepatcht (hulpattributen weg, snippets aangepast,
  `NOTE.md`); haar bron in `NOTE.md` hoort bytegelijk te emitteren zodra `check` de kring toelaat, maar gemeten is dat
  niet.
- Acceptatie (c) van die kaart (de v2-regelset met de verlenging in de maxdatum geeft `check` exit 0) haalt de
  frontend niet zolang de verifier dat document weigert: meting 5 hierboven. Eerst een kaart die de afbakening
  verruimt (punten 1 en 2), en een normkeuze voor 3.
- De melding buiten de afbakening kan de reden noemen die de verifier nu niet noemt: welke lezing achter een interne
  rand noch verschoven noch lokaal is (de diagnose van meting 5 laat zien dat dat de bruikbare informatie is).

### Landing (hoofdsessie, 2026-10-22)

Gerebased op main `0a02aa1b` (K13 geland); het enige conflict was `EXPECTED_PLAYED`, 268 + 1 = 269. `gates-3` op
`22744f78`: alle zeventien groen, EXIT 0 (13:29–14:05). Fable-advies 2 (`research/bruggen-2026-10-22/fable-advies-2.md`)
adviseerde de kern nu te landen; de verruiming tot het huishouden is de volgende kaart
(`instance-cycle-household-reads`), vóór `check-admits-instance-cycle`. CONSTRUCTS-rij 45 uit de Result, met de
kolom "out" naar dat advies; PLAN §9 "A cycle over a switch relation is ordered by depth".
