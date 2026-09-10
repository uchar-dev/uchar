# Een kruis-stelsel-eenheid met gelijke canonieke naam splitst de uitvoerders — op ELKE eenheidspositie

*(Bestandsnaam draagt nog "kruis-stelsel-eenheid-divergentie"; de kaart is
gemeten breder dan haar naam suggereert. Dit is een **eenheden-kaart**, geen
klem-kaart — zie §"Reikwijdte".)*

Goal: beslissen — en als spec-delta of als expliciet niet-doen vastleggen — wat
er gebeurt met een eenheid die convertibel is op **canonieke naam** maar in een
**ander gedeclareerd eenheidstelsel** zit dan de eenheid waartegen ze wordt
gekruist. Vandaag laat de verifier zo'n document door, rekenen `ref` en Drools
het uit, en weigert de ASP-backend het te vertalen. Dat is een levende
divergentie op een **verifier-schoon** document, en ze zit **niet** op één
positie maar op alle vier de eenheidsposities die het IR kent.

## De vorm en de meting (2026-09-10)

Twee stelsels, allebei volledig gedeclareerd in `eenheidstelsels`:

- `Gewicht` met `kg` (canoniek `kg`, factor 1);
- `Massa` met `kg` (canoniek `kg`, factor 1) en `t` (canoniek `kg`, factor 1000).

Een `Begrensd` met de basis in `Gewicht`-`kg` en de grenzen in `Massa`-`t`
(`minimaal 10 t`, `maximaal 100 t` over een `kg`-basis):

| consument | uitkomst |
|---|---|
| verifier | **PASS** — de canonieke namen zijn gelijk, dus WF-099 en `units_convertible` laten het door |
| `ref` | herschaalt: 10 t = **10000** kg |
| Drools, beide takken | **10000** |
| ASP-backend (clingo) | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |

**"Twee tegen één" is te grof gezegd, en de nuance telt.** `ref` deelt het
convertibiliteitspredicaat **letterlijk** met de verifier: `ref/lib/arith.ml`
regel 80 is `let convertible a b = Ir_v0.Checks.units_convertible a b`. Dat
`ref` de verifier volgt is dus geen onafhankelijke bevestiging maar dezelfde
code die tweemaal spreekt. De onafhankelijke stem is **Drools** — een eigen
implementatie (`Eenheidsconflict.eisHerschaalbaar`) die op de canonieke naam
kijkt en het stelsel negeert. De stand is dus preciezer: `ref` (zelfde
predicaatcode als de verifier) **plus** Drools (onafhankelijk) tegenover de
ASP-backend.

## Reikwijdte: dit is een eenheden-kaart, niet een klem-kaart

Nagemeten 2026-09-10 op alle vier de posities waar een van de drie eenheden
kruist. Dezelfde kruis-stelsel-vorm splitst **identiek** op elke positie:

| positie | verifier | `ref` | ASP-backend |
|---|---|---|---|
| WF-099 klem-grens | PASS | 10000 | `… under a Begrensd bound …` |
| WF-035 additieve join (`kg`-attribuut plus `10 t`) | PASS | **10005** | `… under an additive join …` |
| WF-036 vergelijking (`kg`-attribuut groter dan `10 t`) | PASS | evalueert (leeg) | `… under a comparison …` |
| WF-023 toekenning (doel in `Massa`-`kg`, waarde uit `Gewicht`-`kg`) | PASS | 5 | `… under the write to "bandbreedte" …` |

**En het splitst zelfs ZONDER omrekenfactor.** `Massa`-`kg` (factor 1, canoniek
`kg`) als grens op een `Gewicht`-`kg`-basis: verifier PASS, `ref` `oracle: PASS
(5 expectations)` — dus `ref` ziet de twee eenheden als identiek en herschaalt
niets — terwijl de ASP-backend nog steeds `TRANSLATION FAULT` geeft. De
divergentie hangt dus **niet** aan een factor maar aan het stelsel-veld zelf.

Daarmee is acceptance-punt 2 hieronder ("meet eerst of de zusters dezelfde
scheefstand hebben") **al beantwoord**: dat doen ze, alle drie, met hun eigen
foutboodschap. De bouwkaart hoeft die meting niet over te doen, alleen te
bevestigen op de dan geldende HEAD.

**Oorzaakduiding, verfijnd.** Twee mechanismen samen, en de verfijning verklaart
precies waarom ook de factorloze vorm splitst:

1. `units_identical_ok` in de ASP-backend vergelijkt **hele eenheid-records
   structureel** (`Some a, Some b when a <> b`), en `stelsel` is gewoon één van
   de velden van dat record. Twee eenheden met dezelfde afkorting, dezelfde
   canonieke naam en dezelfde factor zijn dus **ongelijk** zodra hun stelsel
   verschilt.
2. `Normalise.document`, dat vóór de vertaling draait, herschaalt alleen binnen
   het stelsel van het **schrijfdoel**. Een kruis-stelsel-literal wordt daardoor
   nooit aangeraakt en houdt zijn eigen stelselveld.

Binnen één stelsel maakt (2) de records gelijk, en daar valt de identiteitstoets
samen met convertibiliteit — precies waarom WF-099 op convertibiliteit kon
landen. Stelsel-overschrijdend doet (2) niets, dus (1) vuurt, met of zonder
omrekenfactor. `ref` (`units_convertible`) en Drools
(`Eenheidsconflict.eisHerschaalbaar`) kijken allebei alleen naar de **canonieke
naam** en negeren het stelsel, dus die twee rekenen door.

## De vraag die deze kaart moet beslissen

Twee uitwegen, en het kader is de zuster-WF's:

1. **De wire sluiten** — een verifier-regel (of een arm van een bestaande
   eenhedenregel) die kruist-stelsel-paren afwijst, zodat de ASP-backend nooit
   een document ziet dat ze zou weigeren. Dan wordt de vraag: is "verenigbaar"
   canonieke naam **plus** stelsel? Dat is een echte verscherping van
   `units_convertible`, en ze raakt WF-023/035/036/099 tegelijk — die roepen
   allemaal hetzelfde predicaat aan. Kosten meten vóór het besluit.
2. **De vertaling verruimen** — de ASP-backend een exacte omrekenfactor laten
   dragen over stelselgrenzen heen, zodat hij `ref` en Drools volgt. Let op wat
   de foutboodschap zelf zegt over de reden dat hij dat niet doet: "ref restates
   with an exact conversion factor this integer encoding does not carry" — dus
   dit is werk aan de integer-codering/normalisatie, geen vlag.

Wat **niet** mag: het laten staan als stilzwijgende divergentie. Twee
uitvoerders die hetzelfde antwoord geven en een derde die weigert, op een
document dat alle gates passeert, is precies wat het differentiële vierkant
hoort te vangen.

## Prioriteitssignaal — levend, maar (voor zover gemeten) alleen handgebouwd

De divergentie is echt en reproduceerbaar, maar de **voorwaarde** ervoor komt in
geen enkel bestaand document voor. Gemeten 2026-09-10 over corpus én
pariteitszone: **67** documenten declareren twee of meer eenheidstelsels, en in
**0** daarvan delen twee stelsels een canonieke eenheidsnaam. Er is dus vandaag
geen gegatet document dat de vorm draagt, en geen bekende producentenroute.

Voorzichtig geformuleerd, want dit is een **niet-uitputtende** meting: er is
niet ge-audit of de RegelSpraak-frontend twee stelsels met een gedeelde
canonieke naam *kan* emitteren — alleen dat geen bestaand document het doet. Een
korte frontend-meting hoort bij stap 1 van deze kaart en kan het signaal beide
kanten op bijstellen.

## Acceptance

- de driehoeksmeting hierboven is gereproduceerd op de dan geldende HEAD, met
  echte uitvoer van alle drie de uitvoerders;
- de reikwijdtemeting is **bevestigd** op de dan geldende HEAD in plaats van
  overgedaan: dezelfde scheefstand staat al gemeten op WF-035, WF-036 en WF-023
  (zie "Reikwijdte"), dus wat de bouwkaart moet doen is nagaan dat die vier
  splitsingen er nog zijn — niet ze opnieuw ontdekken;
- kort gemeten is of de frontend de vorm kan produceren;
- de uitkomst is één van twee, expliciet: een verifier-regel met de volledige
  uitrusting (ID, verifier-check, geslaagde golden, reden-dragende
  `corpus/negatief/<wf###-slug>/`) — of een verruimde ASP-vertaling met een
  golden die de kruis-stelsel-vorm in het volle vierkant speelt;
- in beide gevallen wordt de "open"-passage in WF-099's clausule én registerrij
  bijgewerkt, want die verwijst nu naar deze kaart;
- gates 1, 2, 3, 5, 6 groen, en 12 als de gekozen weg Drools raakt.

May touch: `spec/IR-SPEC.md` en `spec/verifier/` (voorstel, beschermd),
`corpus/negatief/`, `corpus/golden/`, en bij uitweg 2 `backends/clingo/`.
`backlog/`.

Out of scope:
- WF-099 zelf: de regel staat, en haar keuze voor convertibiliteit is los van
  deze vraag gemeten en verdedigd.
- De eenzijdige-eenheidsparen-klasse (ch. 4.1) — andere open klasse, één besluit
  voor vier regels samen.
- De niet-gedeclareerd-stelsel-vorm: die is al dicht, WF-034 weigert hem
  (`unit 't' declares unit system 'Massa', which no declaration carries`).
