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
   terug), een vouw over de eigen satellieten en elke andere relatie maken het deel ongeordend.
3. **Eén schakelrelatie, één rol per deel.** Alle verschoven lezingen van een deel lopen over dezelfde relatie naar
   dezelfde rol. Een deel dat de voorganger én de opvolger leest (dezelfde relatie, de andere rol) sluit per object een
   echte kring; het advies sluit dat niet uit.
4. **Eén satellietrelatie per satelliettype in een deel**, anders kan een satelliet twee eigenaars van verschillende
   diepte hebben.
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
