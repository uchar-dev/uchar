# De geneste to-many-aggregatiebron: de wire draagt maar één rolstap

Goal: uchar's vouwbron beschrijft precies ÉÉN relatie — `elementObjecttype`
plus één `bronRol` (`spec/IR-SPEC.md` ch. 4.3). Een aggregatie waarvan de
bron TWEE to-many stappen stapelt ("het minimum van X van alle A van alle B
van het onderwerp") is op die wire niet uitdrukbaar. Tot 2026-09-11 accepteerde
de RegelSpraak-frontend de zin toch, zonder één diagnostiek, en gooide ze de
buitenste stap weg; waar toevallig een DIRECTE relatie tussen onderwerp en
elementtype bestond vulde de emitter die in, en dan was het document
verifier-schoon en antwoordden `ref`, de ASP-backend én Drools eensluidend over
de VERKEERDE verzameling. **Die stille misbinding is dicht** — de
acute-flankkaart weigert de vorm nu in de resolver
(`backlog/done/20260911-geneste-aggregatie-acute-flank.md`), zodat er een
welgevormd document overblijft dat luid faalt (WF-044) in plaats van een fout
antwoord. Wat deze kaart nog beslist is de wire-vorm van een meerstaps
aggregatiebereik, spec-first: de zin UITDRUKBAAR maken in plaats van
weigeren.

Deze kaart is GROOT. Ze raakt spec, schema, verifier, negatief-zone, corpus,
frontend en alle drie de uitvoerders (Drools in twee takken). Ze is gesplitst:
de eerste, kleine helft — de acute flank — is op 2026-09-11 geland als
`backlog/done/20260911-geneste-aggregatie-acute-flank.md`; wat hier ligt is de
wire-helft.

## Probleem

De UWV-werkbank-toolchain (`regelspraak-werkbank`, commit `2cead54`) heeft
dit gat aan haar kant gedicht, en de commit-boodschap benoemt precies de
plek waar het zit: de parse-boom draagt de hele geschreven keten, maar de
resolver reduceerde hem tot "elementtype = binnenste zelfstandig naamwoord".
Vanaf dat moment zijn een éénstaps- en een tweestaps-bereik bit voor bit
gelijk in de IR, en geen enkele slimmigheid stroomafwaarts kan informatie
terughalen die de IR al heeft weggegooid. Het is daarom eerst een
IR-wijziging en pas daarna een codegeneratie-wijziging.

Wat de werkbank structureel gedaan heeft, in eigen woorden:

- De IR-knoop voor een attribuutverwijzing kreeg één veld erbij: een LIJST
  van opgeloste to-many stappen, onderwerp-zijde eerst. Elke stap draagt twee
  dingen: het objecttype waar die stap op uitkomt, en de naam van de
  TERUGVERWIJZENDE rol — de rol die vanaf dat type terug naar zijn houder
  wijst. Dat is de join-voorwaarde. Het is de to-many spiegel van de al
  bestaande to-one getter-keten.
- De resolver loopt de geschreven keten af en zet elke geschreven rolnaam om
  in zo'n stap. De GESCHREVEN namen zijn gezaghebbend. Een naam die nul of
  meer dan één rol raakt, een to-one stap, een wederkerig feittype, een keten
  dieper dan twee stappen, of een anker dat het objecttype van het onderwerp
  niet letterlijk noemt: elk daarvan wijst de HELE keten af, en de expressie
  houdt haar bestaande zichtbare "niet geïmplementeerd"-markering. De
  beslissing valt één keer, in de resolver, zodat beide backends per
  constructie symmetrisch blijven: ze consumeren dezelfde keten of geen.
- Beide backends verwerken de keten mechanisch: per tussenliggende stap één
  extra join-patroon vóór het elementpatroon binnen de accumulate-bron, en
  het element joint zijn DIRECTE houder in plaats van het onderwerp. De
  bestaande lege-verzameling-machinerie blijft ongemoeid op het binnenste
  patroon staan.
- Twee grenzen zijn bewust getrokken. Het anker moet het objecttype van het
  onderwerp letterlijk noemen, nooit via een rolnaam oplossen: een rol
  waarvan de vuller toevallig het eigen type van het onderwerp is, duidt een
  ANDER exemplaar aan, en dat accepteren zou stil over de eigen keten van het
  onderwerp aggregeren — een fout antwoord, strikt slechter dan de eerlijke
  markering. En de geneste TELVORM blijft een markering: haar elementnaam
  staat in de attribuutpositie, een andere zinsvorm.
- Eén Drools-detail dat overgenomen is: bij een samengestelde accumulate-bron
  lokaliseert de model-compiler het bronpatroon via de bronvariabele van de
  EERSTE accumulate-functie. Bronloze count-functies zijn legaal boven een
  gewoon patroon maar laten boven een subnetwerk elke binding onopgelost, met
  een ClassCastException tijdens het vuren. De counts dragen nu de
  elementvariabele.

De testvorm die het pint: onderwerp → tussentype → elementtype, met het
MINIMUM op het ENE tussenexemplaar en de
rest op het andere, zodat het antwoord bewijst dat beide tussenexemplaren
door de tussenstap heen bijdragen; plus een tweede onderwerp dat alleen zijn
EIGEN keten vouwt, een element met een lege attribuutwaarde dat niet meetelt,
en een onderwerp wiens tussenexemplaren geen elementen hebben (leeg bereik).

Aan uchar's kant is de situatie ANDERS en scherper. uchar's frontend heeft
wél een ketenwandeling: `resolver/ExpressieBouwer.java` regel ~1610-1661 loopt
de §13.4.1 multi-hop keten af ("de X van de Y van de Z"), lost elke stap op als
een to-ONE hop en levert het resultaat als `navigatiePad` op de `AttribuutRef`
(regel 1664). Wat ontbreekt is dus niet ketenresolutie op zich, maar (a) een
NAAR-VEEL-keten — de wandeling verwerpt elke to-many hop en daarmee de hele
keten — en (b) de wandeling wordt voor een VOUWBRON helemaal overgeslagen: de
guard `&& !isSetOnderwerp(onderwerp)` sluit een set-onderwerp uit voordat de
wandeling begint (regelnummers in deze alinea zijn van vóór de
acute-flanklanding; de guard staat op de gelande kop op r. 1737 en is door die
landing niet aangeraakt — de flankfix landde stroomopwaarts, in
`keurVouwbron`, r. 485/596). Bovendien kan uchar's WIRE de keten helemaal niet noteren, ook niet
als de frontend hem zou oplossen.
De `Functie`-knoop draagt één `elementObjecttype` (één typenaam) en één
optionele `bronRol` (één `RolBinding`). Er is geen ketenveld, en de twee
velden die een pad zouden kunnen dragen zijn op een vouwbron expliciet
verboden: WF-083 (a) verklaart een niet-lege `navigatiePad` op een vouwbron
ill-formed, en de emitter-discipline bij WF-053 verklaart
`navigatieBindingen` op een vouwbron ill-formed. Dit is dus een spec-first
kaart, niet een implementatiekaart.

## Meting (2026-09-10)

*Deze meting is van VÓÓR de acute-flanklanding van 2026-09-11: de
frontend-kolommen ("de frontend accepteert de bron met `diagnostics: []`") en
de verifier/ref/ASP/Drools-rijen van A en B beschrijven de toenmalige kop. De
huidige uitkomst van A en B staat in
`backlog/done/20260911-geneste-aggregatie-acute-flank.md`; wat hieronder
onveranderd geldt is de WIRE-analyse — welke velden de `Functie` draagt, en
waarom de omweg niet draagt.*

Twee handgeschreven bronnen in Jutterswet-stijl, twee gemeten documenten en
één handgebouwde omweg. Keten: `Jutter` -(feittype *bemanning*, rol
*gevoerde boot*, to-many)→ `Boot` -(feittype *berging*, rol *geborgen
vondst*, to-many)→ `Vondst.gewicht`.

De regel, letterlijk:

    Het laagste vondstgewicht van een Jutter moet berekend worden als de
    minimale waarde van het gewicht van alle geborgen vondsten van alle
    gevoerde boten van de Jutter.

De casus: `J1` voert `B1` en `B2`; `B1` bergt `V1` (3.00 kg) en `V2` (7.00
kg); `B2` bergt `V3` (5.00 kg). Handverwachting uit de ZIN: **3.00**.

**Document A — geen directe relatie tussen onderwerp en elementtype.**
De frontend accepteert de bron met `diagnostics: []` en emitteert:
`elementObjecttype: "Vondst"`, argument met `set: true`, `navigatieRol:
null`, `navigatiePad: []`, en GEEN `bronRol` — het veld ontbreekt op de
wire. De hele geschreven keten overleeft alleen in de presentatievelden, die
geen enkele consument leest: `bron` draagt "het gewicht van alle geborgen
vondsten van alle gevoerde boten van de Jutter", `onderwerpBron` draagt "alle
geborgen vondsten van alle gevoerde boten van de Jutter". De buitenste stap is
weg.

| lens | uitkomst |
|---|---|
| verifier | `WF-048 FAIL /resolvedRegels/1/waarde: bindingless instance fold: no fact type relates 'Jutter' to 'Vondst'` |
| `ref` | `EVALUATION FAULT: no fact type relates "Jutter" to "Vondst"` |
| ASP-backend | `TRANSLATION FAULT: no fact type relates "Jutter" to "Vondst"` |
| Drools | `EXCLUDE transpiler SKIP: regel: laagste vondstgewicht — regel (aggregatie buiten Phase-C scope)` |

Vier keer luid. De melding noemt alleen niet de werkelijke oorzaak: het
probleem is niet dat er geen feittype `Jutter`→`Vondst` is, het is dat de zin
er ook nooit één beweerde.

**Document B — dezelfde bron, plus één DIRECT feittype `Jutter` ↔ `Vondst`**
(*vondstaanmelding*, rol *aangemelde vondst*), waarbij `J1` alleen `V3` heeft
aangemeld. Dit is de acute flank. De frontend emitteert opnieuw met
`diagnostics: []`, en nu MET een `bronRol`:

    "bronRol": { "node": "RolBinding", "feittype": "vondstaanmelding",
                 "rol": "aangemelde vondst" }

De zin schrijft "geborgen vondsten van alle gevoerde boten"; de wire zegt
"aangemelde vondsten". De geschreven woorden zijn nooit geraadpleegd. De
oorzaak staat op één plek: `emit/IrEmitter.java` r. 776 (767 is de Javadoc,
768 de methodekop) leidt de `bronRol` van
een ledenvouw af met een zuivere TYPE-PAAR-zoektocht van het regelonderwerp
naar het elementtype (`verbindendeRolBinding(huidigSubject,
elementObjecttype)`).

| lens | uitkomst |
|---|---|
| verifier | `verifier: PASS` |
| `ref` (verwachting 3.00) | `MISMATCH: J1.laagste vondstgewicht = 5, expected 3` |
| ASP-backend (verwachting 3.00) | `MISMATCH: J1.laagste vondstgewicht = [500], expected 300` |
| Drools (verwachting 3.00) | `FAIL … attribuut laagste vondstgewicht: verwacht 3.00, werkelijk 5.00` |
| `ref` (verwachting 5.00) | `oracle: PASS (1 expectations)` |
| ASP-backend (verwachting 5.00) | `clingo: PASS (1 expectations)` |
| Drools (verwachting 5.00) | gespeeld, geslaagd |

Dat is de kern: **verifier-schoon, en het differentiële vierkant is het
eens — over de verkeerde verzameling.** Geen enkele gate ziet dit. Gate 10
niet (de frontend regenereert dit document byte-identiek), gate 2/3 niet, en
gates 5/6/12 juist niet omdat ze het EENS zijn. Alleen de ankerdiscipline
vangt het: een mens die de verwachting uit de ZIN schrijft in plaats van uit
de uitvoer.

Waarom de verifier het laat passeren, precies: de `fold_source`-herkenner in
`spec/verifier/lib/checks.ml` (rond regel 4650) kruist de GESCHREVEN naam
alleen bij de `Onbekend`-getypeerde rolnaam-wrat (de row-1-wart). Bij een
ledenvouw (`set: true`) is de geschreven vorm van de bron per constructie
niet gekruist — er IS geen veld waarin ze staat, alleen de presentatiestring.
WF-047 controleert dan enkel of de gebonden rol een `Vondst` vult en de
tegenoverliggende rol een `Jutter`, en dat klopt allebei.

**Omweg — geneste vouw-in-vouw, handgebouwd binnen het huidige schema.**
Buitenste `MINIMUM` met `elementObjecttype: "Boot"` en `bronRol`
*bemanning*/*gevoerde boot*; als enig argument een binnenste `MINIMUM` met
`elementObjecttype: "Vondst"` en `bronRol` *berging*/*geborgen vondst*. Het
schema laat dit toe (`argumenten` is een lijst van `expr`, en `Functie` zit
in de `expr`-unie), maar:

| lens | uitkomst |
|---|---|
| verifier | `WF-047 FAIL /resolvedRegels/1/waarde/bronRol: a fold-source role binding on a node that is not an instance fold over a relation` en `WF-047 FAIL /resolvedRegels/1/waarde/argumenten/0/bronRol: the opposite role 'bergende boot' fills 'Boot', but the node names 'Jutter'` |
| `ref` | `EVALUATION FAULT: unsupported instance-fold source shape` |
| ASP-backend | `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)` |
| Drools | transpiler-SKIP |

De omweg draagt vandaag NIET, in geen enkele lens. De tweede verifier-regel
noemt de structurele reden: de verifier kruist de `bronRol` van een vouw
tegen het REGELONDERWERP, vast bedraad. Een vouw kan geen onderwerp zijn van
een andere vouw; er is in de v0-wire geen binder die het buitenste element
als onderwerp aan de binnenste vouw doorgeeft. Hetzelfde geldt in `ref`: de
vouwbron-arm matcht uitsluitend op een `AttribuutRef`, alles anders valt door
naar de luide fout hierboven.

**Corpus.** Geen enkel document in `corpus/` en geen fixture in de
`toka/`-zone draagt vandaag een gestapelde keten. Er is dus geen bestaande
groene golden die stiekem fout is; het risico zit in de KLASSE documenten die
morgen binnenkomt.

## Voorgestelde aanpak

**Kernel of sugar.** Dit is KERNEL, en wel door verruiming van een bestaand
veld, niet door een nieuwe knoop. Het GHC-Core-criterium (`spec/PLAN.md` §3)
vraagt of de vorm naar de kernel desuikert; de meting hierboven laat zien dat
de enige kandidaat-desuikering — vouw-in-vouw — geen geldige v0-vorm HEEFT,
en bovendien semantisch iets anders zegt (zie hieronder). Wat werkelijk
kernel is, is niet de vouw maar het BEREIK: de vouw beschrijft vandaag haar
bron met één relatie, en één relatie is de lengte-1 gevallen van een pad. De
kleinste eerlijke wijziging is dus: generaliseer `bronRol` (één `RolBinding`)
naar een PAD van `RolBinding`s, onderwerp-uitwaarts, elke binding met de
bereikte rol gebonden — exact de oriëntatie en discipline van
`navigatieBindingen` in ch. 4.1a, zodat WF-047's resolutie letterlijk
hergebruikt wordt. Alle zeven vouwsoorten krijgen de vorm dan tegelijk, met
één vlakke-unie-semantiek, zonder een tweede knoop en zonder een tweede
regelset.

**Is de werkbank-representatie de goede vorm voor uchar?** Niet
letterlijk. De werkbank noteert per stap (doeltype, naam van de
terugverwijzende rol) — een vorm die op bean-veldnamen leunt, want de
terugrol IS daar het join-veld. uchar's symbolische equivalent is de
`RolBinding` (feittype + rol), die geen bean-naam noemt en wiens
resolutieregels al bestaan. De INHOUD van het besluit is wel dezelfde en
verdient overname: de geschreven namen zijn gezaghebbend, en één
onoplosbare, ambigue, wederkerige of to-one stap wijst het HELE pad af in
plaats van gedeeltelijk te binden.

**De leeg-semantiek MOET expliciet, want de twee lezingen verschillen.**
Vlakke unie: het minimum over alle `C` die vanaf `P` via welke `A` dan ook
bereikbaar zijn. Genest: het minimum over de per-`A` minima. Voor MIN/MAX
vallen die samen zolang elke binnenverzameling niet-leeg is — minimum is
associatief over een unie. Ze lopen uiteen zodra één `A` geen `C` heeft: de
vlakke lezing negeert die `A` volledig, de geneste lezing produceert per lege
`A` een LEEG binnenresultaat, en wat er dan gebeurt hangt af van de
leeg-regel van de buitenste vouw (MIN/MAX/EERSTE/LAATSTE hebben geen
`leegwaarde`, `RUNTIME-SEMANTICS.md` ch. 1.2). Voor AANTAL vallen ze
helemaal niet samen: een telling over de vlakke unie telt `C`'s, een telling
van tellingen telt `A`'s. En voor SOM met een `leegwaarde`-clausule ("of 0
als die er niet zijn") vuurt de standin in de vlakke lezing één keer, voor
een `P` zonder enige bereikbare `C`, en in de geneste lezing per lege `A`.
Het voorstel is daarom de VLAKKE UNIE als normatieve lezing — dat is wat de
zin zegt en wat de werkbank-join oplevert — en de geneste lezing NIET als
desuikering te presenteren.

**Volgorde, spec-first.**

1. `spec/IR-SPEC.md` ch. 4.3: het bereikpad als veld op de `Functie`
   (voorstel `bronPad`, v1 `sourcePath`), met `bronRol` als de lengte-1
   vorm; de wederzijdse uitsluiting van beide velden; de vlakke-unie-lezing;
   de diepte (start bij 2 stappen, zoals de werkbank, of ongelimiteerd —
   te beslissen, met argument). Plus de aanpassing van WF-083's
   ankerregel (c): het anker van de bron is het elementtype, maar het
   TUSSENTYPE is nu ook een positie die de regel moet kennen. Een
   KANDIDAAT-nummer voor de padregel is WF-102 (bijgewerkt 2026-09-11:
   WF-098 is inmiddels bezet door de contextwaarde-namenregel, ch. 4.1, kaart
   `20260909-constante-naam-gesloten-verzameling`, WF-099 door de
   begrenzing-grenseenheidsregel, ch. 4.6, kaart
   `20260909-begrensd-grens-verifier-omheining`, WF-100 door de
   kenmerkeigenaar-regel, ch. 4.6 (q1), kaart
   `20260909-eigenaar-verklaart-kenmerk`, en WF-101 door de
   geldigheidsgrens-lexeemregel, ch. 4.1, kaart
   `20260911-geldigheidsgrens-verifierhek`); niet reserveren voordat de
   vorm vaststaat. De dubbeling met `20260910-reverse-rol-via-naam.md` is
   weg: die kaart is geland met antwoord (ii) en heeft haar WF-kandidaat
   laten vervallen, dus deze kaart is de enige wachter op het volgende
   nummer.
2. `spec/RUNTIME-SEMANTICS.md`: de gewerkte voorbeelden, inclusief de drie
   leeg-hoeken (leeg binnenbereik, leeg buitenbereik, leeg elementattribuut)
   en de `leegwaarde`-vraag bij SOM.
3. `spec/schema/regelspraak-ir-v0.schema.json`: het extensieveld.
4. `spec/verifier/`: de padregel plus de aanpassing van WF-047/WF-048 (de
   bindingloze afleidbaarheid moet nu over een PAD spreken, niet over één
   paar).
5. `corpus/negatief/`: minstens de getuigen voor een niet-oplossende stap,
   een to-one stap in een bereik, een pad wiens laatste vuller ≠
   `elementObjecttype`, een leeg pad, en `bronRol` + `bronPad` samen.
6. `corpus/golden/`: één ankergolden met de tweestaps-vorm en HANDGESCHREVEN
   absolute verwachtingen — inclusief de vorm waarin het minimum op het ENE
   tussenexemplaar zit en de rest op het andere, zodat het antwoord bewijst
   dat beide tussenexemplaren bijdragen.
7. Frontend, dan `ref`, dan de ASP-backend, dan Drools (beide takken; gate 13
   twist-tabel nalopen op de nieuwe positie).

**De acute flank is GELAND (2026-09-11), als aparte kleine kaart:**
`backlog/done/20260911-geneste-aggregatie-acute-flank.md`. De frontend beeldt
een meerstaps-zin niet meer stil af op een éénstaps-relatie, en de
type-paar-afleiding van `bronRol` bindt niet meer tegen de geschreven rolnaam
in: de resolver markeert de ledenvouw als `Onopgelost` met een gerichte WARNING
langs drie routes — het set-onderwerp nest zelf een set-stap; er zit een to-één
tussenstap onder de set-stap, zodat het anker niet het regelonderwerp is; of de
geschreven elementnaam noemt de rol die het type-paar zou binden niet. De
markering draagt het EIGEN resultaattype van de aggregatie, niet het
onbekend-sentinel, dus het geweigerde document is WELGEVORMD (`verifier: PASS`)
en faalt luid onder WF-044 in `ref` en de ASP-backend, met een Drools-weigering
ernaast — de rode-zonevorm. Document B hieronder is daarmee niet meer
verifier-schoon-met-fout-antwoord; er is überhaupt geen antwoord meer. De
emitter-regel zelf is bewust NIET gewijzigd: gemeten is dat een bindingloze
vouw over een uniek verbonden paar verifier-schoon blijft en elke uitvoerder
dezelfde verkeerde relatie opnieuw afleidt, dus de binding weghalen maakt de
fout onzichtbaar in plaats van weg.

Wat deze kaart nog draagt is de WIRE-vraag hieronder — de zin uitdrukbaar maken
in plaats van weigeren — plus één deelvraag die de landing expliciet heeft
laten liggen omdat ze hier thuishoort: het ANKER dat geen objecttype noemt maar
een ROLNAAM ("alle vondsten van de aanmelder", 2 van de 56 gemeten
ledenvouwen). De frontend laat die vorm ongewijzigd binden, omdat de ankerregel
hierboven — "het anker moet het objecttype van het onderwerp letterlijk noemen,
nooit via een rolnaam oplossen" — een besluit van DEZE kaart is en niet
vooruitgelopen mag worden.

## Acceptance

- Eén besluit over de wire-vorm van een meerstaps aggregatiebereik, met de
  vlakke-unie-lezing normatief vastgelegd en het verschil met de geneste
  lezing expliciet benoemd bij MIN/MAX, AANTAL en SOM-met-`leegwaarde`.
- De diepte-vraag beslecht met argument (twee stappen als de werkbank, of
  ongelimiteerd), niet als aanname.
- Elke nieuwe normatieve MUST draagt een ID, een verifier-check, minstens één
  slagende golden en één negatief-getuige (`CLAUDE.md`, Spec discipline).
- Document B hierboven is een verplichte getuige. De eerste helft daarvan is
  op 2026-09-11 afgedaan door de acute-flankkaart: de frontend weigert de
  bron, dus stil 5.00 bestaat niet meer. Wat deze kaart nog moet halen is de
  tweede: de wire draagt de keten en alle drie de uitvoerders antwoorden 3.00.
- De ankergolden draagt handgeschreven absolute verwachtingen, niet
  overgenomen uit uitvoer, en de minimum-op-het-ene-tussenexemplaar-vorm.
- `ref` ≡ ASP-backend ≡ Drools over de nieuwe golden (het volle vierkant);
  Drools speelt hem of weigert hem met een benoemde reden in de
  kanaal-vloer van gate 12.
- Gate 13 nagemeten op de nieuwe positie: draagt een unit-dragende
  annotatie op het pad, of hoort het pad in de klem?
- Alle dertien gates groen; gate 11 (`toka/`) vooraf gemeten, niet achteraf.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
`spec/verifier/`, `corpus/golden/anchor/**` (de ankergolden van stap 6),
`scripts/check-drools.sh` (de kanaal-vloer van gate 12),
`scripts/check-reftype-derivation.sh` (de twisttabel en de klem van gate 13) —
**alle zeven voorstel, beschermd pad: niet mergen**;
`corpus/negatief/`, `corpus/golden/`, `frontends/regelspraak/src/**`,
`ref/`, `backends/clingo/`, `backends/drools/`, `backlog/`.

De drie laatste beschermde paden staan er omdat de eigen aanpak en Acceptance
ze al vergen: stap 6 vraagt een ANKERgolden met handgeschreven waarden, de
Acceptance vraagt dat Drools de nieuwe golden speelt of met een benoemde reden
in de kanaal-vloer van gate 12 staat, en ze vraagt gate 13 na te meten op de
nieuwe positie — een unit-dragende annotatie op het pad valt in de twisttabel,
en valt ze er buiten, dan slaat de bookkeeping-klem aan.

## Out of scope

De EERSTE/LAATSTE/AANTAL-setvouwkloof
(`20260909-eerste-laatste-setvouw-kloof.md`) — die kaart weegt welke
vouwSOORT welke uitvoerder draagt bij een ÉÉNstaps bron; deze kaart gaat over
het BEREIK en laat de soortenvraag ongemoeid. De setlezing BUITEN een vouw
(`20260909-frontend-alle-zonder-vouw.md`, WF-096) — een andere positie, ander
besluit. Gemengde ketens waarin een to-one stap voorkomt: de frontend WEIGERT
ze sinds 2026-09-11 (route 2 van de acute-flankkaart), dus de stille variant
bestaat niet meer; wat hier open blijft is of de wire zo'n stap moet KUNNEN
dragen — als de vlakke-unie-lezing het gratis meebrengt is dat hier beslist,
anders blijft het een eigen vraag. Eén deelvraag ligt hoe dan ook hier: het
ANKER dat geen objecttype noemt maar een rolnaam ("alle vondsten van de
aanmelder"), waar de frontend met opzet stil blijft omdat de ankerregel
hierboven — het anker moet het objecttype letterlijk noemen — deze kaart
toebehoort. De geneste TELVORM (het aantal-over-een-keten), waarvan de
elementnaam in de attribuutpositie staat: een andere zinsvorm, eigen kaart.

## Result (2026-09-11)

De wire-helft is af. `bronPad` staat in de spec, het schema, de verifier,
beide OCaml-uitvoerders en de RegelSpraak-frontend; Drools weigert de vorm bij
naam. Het gemeten document B is niet langer geweigerd-of-fout: de frontend
emitteert de keten en `ref` en de ASP-backend antwoorden 3.00.

### De vier besluiten, elk met zijn argument

**1. De wire-vorm: `bronPad`, een lijst `RolBinding`s, onderwerp-uitwaarts,
met `bronRol` als de LENGTE-1-VORM — en dus met minstens TWEE stappen in
`bronPad` zelf.** De kaart stelde "`bronRol` als lengte-1-vorm" voor; dat is
letterlijk genomen. Een `bronPad` van lengte 1 zou hetzelfde bereik in twee
vormen op de wire zetten, precies wat WF-087 voor het nullaire keuzetype
weigert en wat WF-089/WF-090 voor domein-identiteit waarmaken. Eén betekenis,
één spelling: dat maakt arm (a) (wederzijdse uitsluiting) en arm (b) (minstens
twee stappen) tot echte MUSTs met een eigen, geïsoleerde getuige, en het laat
alle bestaande documenten onaangeraakt.

**2. De lezing: de VLAKKE UNIE, normatief, en de geneste lezing uitdrukkelijk
GEEN desuikering.** Het gevouwen bereik is de VERZAMELING
`elementObjecttype`-exemplaren die vanaf het onderwerp langs het hele pad
bereikbaar zijn; een element dat via twee tussenexemplaren bereikbaar is
draagt ÉÉN keer bij. De drie verschillen met de geneste lezing staan
normatief uitgeschreven in `RUNTIME-SEMANTICS.md` ch. 3.4b:

- **MIN/MAX/EERSTE/LAATSTE.** Ze vallen samen zolang elk binnenbereik
  niet-leeg is en lopen uiteen zodra één tussenexemplaar niets bereikt: vlak
  negeert dat exemplaar, genest produceert een LEEG binnenresultaat dat de
  buitenste vouw moet absorberen — en juist deze vier hebben geen
  `leegwaarde`. De vlakke lezing stelt de vraag niet.
- **AANTAL.** Ze vallen helemaal niet samen: vlak telt ELEMENTEN, genest telt
  TUSSENEXEMPLAREN (op de ankergolden: drie vondsten tegen twee boten).
- **SOM met `leegwaarde`.** Vlak vuurt de standin HOOGSTENS ÉÉN keer, voor
  een onderwerp wiens hele bereik leeg is; genest per leeg tussenexemplaar.

Eerlijk erbij, want het is een gat dat ik benoem in plaats van verberg: bij
een NUL-leegwaarde vallen de twee sommen numeriek samen, en beide uitvoerders
dragen vandaag alleen de nul-leegwaarde (`backends/clingo`: "som fallback
other than 0 is not translated yet"). Het SOM-verschil is dus BENOEMD en
NERGENS GEPIND. Wat wél gepind is, is de UNIE zelf: `case2-gedeelde-vondst`
zet één vondst onder beide boten en de som is **15,00 en niet 18,00** (het
minimum is 3,00 in beide lezingen, dus de som is hier de enige getuige).

**3. Diepte: ONBEGRENSD (twee of meer stappen).** Argumenten, in volgorde van
gewicht. (i) Een grens is een constante in een normatieve regel zonder
semantiek erachter: de unie is per inductie op de lijst gedefinieerd, elke
consument loopt de lijst als een fold af, en geen clausule degradeert bij drie
stappen. (ii) Een grens kan alleen VERSOEPELEN — hem later verhogen maakt
ill-formed documenten welgevormd, en dat is een wire-format-gebeurtenis; een
niet-genomen grens kost niets, een genomen grens kost dat. (iii) De werkbank
begrenst op twee omdat zij één join-PATROON per stap emitteert en de
blast-radius van haar codegeneratie wilde begrenzen; dat is een eigenschap van
een codegenerator, niet van het bereik, en uchar consumeert de lijst
symbolisch. Terugdraaibaar in één arm: een `List.length pad > N`-arm naast (b)
in `spec/verifier/lib/checks.ml`, plus dezelfde klem in `ref` en
`backends/clingo`. **Niet als aanname maar als artefact:** de ankergolden
draagt een DRIESTAPS-regel naast de tweestaps (`laagste kistgewicht`), en de
frontend-getuige `een_driestaps_keten_draagt_drie_stappen` emitteert drie
bindingen uit één zin.

**4. Het ROLNAAM-ANKER: toegelaten, en wel precies onder de orde die ch. 4.1a
al vastlegt.** De werkbank weigert ELK rolanker, met het argument dat een rol
wiens vuller toevallig het eigen type van het onderwerp is een ANDER exemplaar
aanwijst. Dat argument is juist — en het is exact wat "verre rol eerst" al
vangt. uchar heeft die toets al, op twee plaatsen (WF-083 (b) en de
nulstaps-zelfverwijzing van ch. 4.1a / WF-053), inclusief de normatieve
VOLGORDE: verre-rolresolutie EERST, en de zelfdenoterende lezing alleen bij
NUL verre treffers. Toegepast op het anker van een vouwbron:

- een verre rol ("de strandmaat", bereikbaar VANAF het onderwerp) is een HOP
  en hoort als STAP op het pad; staat er maar één geschreven stap, dan is er
  geen pad om hem op te zetten en WEIGERT de resolver — met een eigen
  diagnose ("rolanker in een vouwbron", getuige
  `een_verre_rol_als_anker_wordt_geweigerd`);
- een zelfdenoterende naam ("de aanmelder": nul verre treffers, en het
  onderwerptype vult zelf een rol van die naam) IS het onderwerp, de keten
  begint daar, en de wire-vorm blijft de onveranderde `bronRol`.

Zo weigert uchar dezelfde vorm als de werkbank zonder de vorm ernaast te
weigeren. **Gemeten kosten: nul.** Over de vier zones: 56 ledenvouwen, waarvan
**zes** een anker dragen dat geen objecttype noemt — twee in de negatief-zone
(`wf093-wratvouw-set-som`, `wf093-wratvouw-set-maximum`, allebei al afgewezen
op hun soort) en vier in de pariteitszone — en alle zes zijn zelfdenoterend en
eenstaps. Gate 10 (42 byte-identiek) en gate 11 (45 byte-identiek) zijn de
harde nulmeting; de tegenproef van de zusterkaart is omgezet naar
`een_zelfdenoterend_rolanker_blijft_een_eenstaps_binding` en pint dat de
uitkomst byte-voor-byte dezelfde blijft.

**Bonus, en hier beslecht omdat de vlakke-unie-lezing hem gratis meebrengt
(de kaart stond dat toe): een TO-ÉÉN stap is een gewone stap.** Multipliciteit
is een declaratie-eigenschap en komt in de lezing niet voor: een to-één rol
draagt haar nul-of-één exemplaar bij als elke andere. Hem weigeren zou een
nieuwe arm op `meervoudig` vergen — een veld dat geen consument in deze
positie leest — om een vorm te weigeren wiens semantiek al vastligt. Document
F van de zusterkaart ("alle vondsten van de boot van de Jutter") is daarmee
een tweestaps `bronPad` in plaats van een weigering.

**5. Het Drools-kanaal: WEIGEREN, met een benoemde reden in de kanaalvloer.**
De kaart stond beide toe. Doorslaggevend is dat de WEIGERING hoe dan ook
gebouwd moest worden, en dat zij de correctheidskritische helft is: `IrLoader`
controleert geen onbekende sleutels BINNEN een knoop, dus zonder expliciete
`Refusal` valt `bronPad` geruisloos weg en leidt de transpiler de join weer
per TYPEPAAR af — precies de stille misbinding die dit veld sluit, nu in één
uitvoerder in plaats van in alle drie. **Gemeten, op het geëmitteerde document
`jutterswet-vondstketen` met `bronPad` met de hand verwijderd:**

    verifier: PASS
    ref    : MISMATCH: J1.laagste vondstgewicht = 5, expected 3
    clingo : MISMATCH: J1.laagste vondstgewicht = [500], expected 300
    Drools : FAIL case.json: attribuut laagste vondstgewicht: verwacht 3.00, werkelijk 5.00

Spelen vergt bovendien een SAMENGESTELDE accumulate-bron in beide takken plus
de §13.3.9-opslagrichtingstoets op elke tussenstap (`heeftReferentieVeld`) —
een gemeten wijziging met eigen gate-12-kanaalgevolgen, precies het soort werk
dat de kanaalvloer een eigen kaart laat zijn. Het vierkant blijft in de
richting die ertoe doet: ref ≡ clingo op de nieuwe goldens, met Drools luid
buiten in plaats van stil verkeerd.

**6. De exemptiegrens van `RuleChecker.keurSetlezing`: ONGEWIJZIGD, en dat is
gemeten, niet aangenomen.** De exemptie is gesleuteld op de VORM van de vouw
(`elementObjecttype != null && argumenten.size() == 1 && VOUWSOORTEN`), niet
op de aanwezigheid van een `bronRol`. Een padvorm is nog altijd de enige
setlezing van een enkelargument-ledenvouw en valt er dus zonder wijziging
binnen — aan beide kanten, want de verifier trekt de grens (WF-096) op
dezelfde drie eigenschappen. De suite bevestigt het: `keurSetlezing` staat op
`Severity.ERROR` en geen enkele padgolden wordt geblokkeerd (gate 10 groen,
207 tests).

### Wat er per laag gebouwd is

**`spec/IR-SPEC.md` ch. 4.3** — de veldrij `sourcePath`; een normatief blok
"The fold source's RANGE is a PATH (WF-102)" met de wire-vorm, de
vlakke-unie-lezing, de drie divergenties, het diepte-argument, het
to-één-besluit en het rolanker-besluit; WF-102 met vier armen; een
WF-index-rij; `bronPad` in de extensieveldenlijst van de Method-sectie. Plus
de aanpassing die de kaart vroeg: **WF-083 (c)** benoemt nu het TUSSENTYPE als
derde klasse type in die positie — het anker verschuift er niet door (elk lid
is nog altijd een `elementObjecttype`-exemplaar), maar de regel moest hem
GEZIEN hebben, want de vorm waarin een plausibel ogende verkeerde naam
beschikbaar is bestond niet vóór het pad.

**`spec/RUNTIME-SEMANTICS.md` ch. 3.4b** — de vlakke unie als algoritme, de
drie gevolgen, de to-één-clausule, en het gewerkte voorbeeld met absolute
waarden (beide cases van de ankergolden, handberekend vóór ze gedraaid zijn).

**`spec/schema/`** — `bronPad` als optioneel extensieveld (array van
`RolBinding`). BEWUST GEEN `minItems: 2`: dat zou een tweede, ongetuigde laag
zijn — gate 2 valideert alleen `corpus/golden` en `corpus/rood`, dus een
document dat de arity schendt kan daar niet staan. De ariteit is WF-102 (b),
met twee getuigen in `corpus/negatief`.

**`spec/verifier/`** — `bron_pad` in de AST; geladen VERBATIM (lege lijst
incluis), zodat een defect pad een CITEERBARE overtreding is en geen
laadfout zonder regelnaam. De check staat vóór de eenstaps-logica en keert
terug in plaats van door te vallen, zodat precies één van de twee vormen
beoordeeld wordt. Er is uitdrukkelijk GEEN bindingloze terugval voor een pad:
WF-048 leidt af uit een type-PAAR, en de tussentypen van een pad zijn een
eigen bewering, geen gevolg van zijn eindpunten. De afhankelijkheidsgraaf
(ch. 6.1) krijgt een kant per stap, zodat een feitcreatie die een
TUSSENrelatie vult nog altijd vóór de vouw geordend wordt.

**`corpus/negatief/` — negen getuigen, elk geïsoleerd:**

| getuige | arm | valt om |
|---|---|---|
| `wf102-bronpad-naast-bronrol` | (a) | beide velden aanwezig; beide resolveren, de laatste stap ÍS de binding |
| `wf102-bronpad-leeg` | (b) | een gesteld bereik dat niets bereikt |
| `wf102-bronpad-een-stap` | (b) | een GELDIG eenstaps bereik (het document krijgt er een direct feittype voor), dus de ariteit is het enige defect |
| `wf102-bronpad-op-niet-setbron` | (c) | het pad op de rij-1-wrat, correct op het onderwerp verankerd |
| `wf102-bronpad-start-niet-op-onderwerp` | (d) | de eerste join; de wire-vorm van precies de degradatie die dit veld vervangt |
| `wf102-bronpad-onverbonden-stap` | (d) | een middenjoin, op een document dat een eindpunt-only-regel zou doorlaten |
| `wf102-bronpad-landt-naast-elementtype` | (d) | de landing, één stap te ver; alle stappen resolveren én joinen |
| `wf047-bronpad-stap-onopgeloste-rol` | WF-047 | een stap die het gedeclareerde MEERVOUD spelt |
| `wf083-vouwbron-anker-op-tussentype` | WF-083 (c) | het anker op `Boot`, een type dat de vouw aantoonbaar bewandelt |

(De laatste draagt één mee-vurende WF-034-regel: `Boot` declareert geen
`gewicht`. Dat is de VORM van het defect, niet een tweede defect, en staat in
de NOTE.)

**`corpus/golden/anchor/jutterswet-bootberging`** — handgeschreven IR met
handberekende absolute verwachtingen. Drie regels op één document: het minimum
over een TWEESTAPS bereik, de som daarvan met een `of 0`-terugval, en het
minimum over een DRIESTAPS bereik. Case 1 zet het bereik over twee
tussenexemplaren — `J1` voert `B1` en `B2`, het minimum 3,00 zit op `B1` en
`B2` draagt 5,00 bij, dus het antwoord bewijst dat beide bewandeld worden — en
daarnaast de drie lege hoeken: een leeg elementattribuut valt uit beide
waardevouwen (9,00 / 9,00), en een leeg bereik bij de LAATSTE stap en bij de
EERSTE antwoorden gelijk (minimum leeg, som 0 — de terugval één keer). Case 2
is de unie-pin. Het model is EENHEIDSDRAGEND gemaakt (kg, met `t` als
omrekenbare zusterzus), zodat gate 13 de vouwbron ONDER een pad echt twist.

**`corpus/golden/jutterswet-vondstketen`** — de geëmitteerde getuige
(document B): het model met de DIRECTE `vondstaanmelding` ernaast, waar
`J1` alleen `V3` heeft aangemeld. De frontend emitteert het pad, ref en clingo
antwoorden 3,00, gate 10 regenereert het byte-identiek.

**`ref/`** — `linked_path`: de vlakke unie over het pad, met een expliciete
dedup op instantie-LABEL in eerste-bereik-volgorde. Plus de DRIE vormarmen van
WF-102 als luide fout, want een document dat een uitvoerder bereikt zonder
langs de verifier te komen moet BIJ NAAM falen in plaats van als de
eenstapsvorm met het bereik weggelaten gelezen te worden. 18 nieuwe gewerkte
voorbeelden (535 → 553).

**`backends/clingo`** — `source_join`: één `rol/3`-atoom per stap, met verse
tussenvariabelen. De dedup is hier GRATIS en dat is een eigenschap van de
gekozen aggregaat-TERM, niet van de join: de tussenvariabelen zijn
existentieel gekwantificeerd BINNEN het aggregaat en elke term noemt alleen
het element (en zijn waarde), dus een element dat via twee tussenexemplaren
bereikbaar is levert één tupel. De termen zijn daarom letterlijk gelaten zoals
de eenstaps-armen ze schreven, met een comment dat zegt waaróm. Vijf nieuwe
checks (120 → 125): vier voor de weigerende kant, die geen corpusdocument kan
dragen, en één voor de to-één-stap, die geen corpusdocument dráágt.

**`backends/drools`** — één `Refusal("Functie.bronPad", …)` in `IrLoader`,
plus `BronPadWeigeringWitnessTest` (3 tests, 318 → 321): de weigering is
per-vorm, zij keert NIET om wanneer een toevallig direct feittype beschikbaar
is, en zonder het veld laadt hetzelfde document gewoon.

**`frontends/regelspraak`** — `RegelSpraakModel.bereikHop` (één geschreven
stap → `(RolBinding, doeltype)`, via de bestaande `elementBinding`, dus
gedeclareerde naam of gedeclareerd meervoud en nooit het best-effort meervoud
— exact WF-047's "the plural does not bind"), `rolTreffersNaarSubject` (de
TELLING achter de verre-rol-eerst-orde: `rolDoeltypeNaarSubject` stort nul en
meerdere allebei in null, en een AMBIGUE verre treffer mag niet doorvallen
naar de zelfdenoterende lezing) en `vultZelfDeRol`. `ExpressieBouwer.bereikPad`
loopt de geschreven segmenten buiten-in af — zowel de geneste
set-`onderwerp`s als de gulzige `kaleNaam` die een to-één stap opslokt — en
wijst het HELE pad af bij een onoplosbare stap, een anker dat het onderwerp
niet is, of een wandeling die niet op het elementtype landt. `IrEmitter.bronPad`
tilt het van de operand (waar de geschreven keten is) naar de vouw (waar de
wire het draagt) en schrijft het IN PLAATS VAN `bronRol`.

### Gates

**Gate 11 vooraf gemeten, vóór er iets bewoog** (de kaart eiste het):

    check-toka-parity: PASS   [samengevat, zelfde rechtenreden als hieronder]
      (45 .rgs byte-identiek, vloer 45; 27 tests, vloer 27;
       29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)

**Alle dertien op de eindkop** — letterlijke slotregels:

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 199 file(s) validate and round-trip, floor 192)
    check-verifier: PASS (199 golden clean; 373 negatief rejected for their declared rule, floor 350)
    check-case-schema: PASS (schema valid; 470 case file(s) validate)
    check-oracle: PASS (ref suite 553 worked examples, floor 489; 444 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
    check-clingo: PASS (backend suite 125 checks, floor 83; 444 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (408 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
    check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (207 tests, floor 173); 42 rgs golden(s) re-emit byte-identically (floor 38)
    check-toka-parity: PASS
      [SAMENGEVAT, niet letterlijk — de letterlijke regel noemt de twee holdouts
       bij bestandsnaam, en die namen mogen dit Result niet in (rechtenregel).
       De getallen zijn wel letterlijk:]
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45
                   (holdouts: 2); TOKA suite 27 tests, floor 27
      schema: 29 replay slice(s) metaschema-valid + byte-round-trip
      differential replay: 29 cases (floor 29) through ref, clingo, Drools,
                           ZERO silent divergences
        ref 29 PASS / 0 diverge; clingo 29 PASS / 0 fault / 0 diverge;
        Drools 29 PLAYED / 0 EXCLUDE / 0 FAIL-or-ERROR
    check-drools: PASS
      played 124  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 46  (per-form refusals: loader / transpiler marker / contract / unsupported input)
        2x  loader: form not carried by the v0 Drools backend: Functie.bronPad (a multi-step fold-source range (WF-102); the accumulate source carries one join role)
      value-divergence 1  (listed with reason — the exec-model differs from ref/clingo)
      no-case 2  (goldens without a case file)
      channel floor 49 non-PLAYED goldens pinned by name; played matches the pinned 124
      JUnit 321 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (92 unit annotations twisted over 17 goldens, floor 74; 80 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
      corpus-wide accounting: 128 unit-bearing annotations = 95 twistable + 27 exempt (the annotation IS the source) + 6 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table

Bewegingen, alle groei en geen krimp: gate 2 197 → 199 documenten; gate 3
golden 197 → 199 en negatief 364 → 373 (+9); gate 4 467 → 470 casebestanden; gate 5
ref-suite 535 → 553 en 441 → 444 replays; gate 6 backend-suite 120 → 125 en
441 → 444 replays; gate 7 405 → 408; gate 10 suite 205 → 207 en 41 → 42
byte-identieke re-emissies; gate 12 JUnit 318 → 321 en de kanaalvloer 47 → 49;
gate 13 83 → 92 getwiste annotaties over 16 → 17 goldens. Geen enkele vloer is
aangeraakt, gate 11 is ongewijzigd op elk getal, en gate 12's
`EXPECTED_PLAYED` blijft 124 (twee NIEUWE goldens die niet spelen zijn nooit
meegeteld).

**Gate 13, nagemeten op de nieuwe positie zoals de kaart vroeg.** Het antwoord
is: het pad voegt GEEN twistpositie toe, want een `RolBinding` draagt geen
type-annotatie — hij is twee namen. De klem bevestigt het in haar eigen
boekhoudregel: "0 outside the twist table". Wat wél moest gebeuren is dat de
vouwbron ONDER een pad in de twistset komt, en daarvoor is de ankergolden
eenheidsdragend gemaakt: 83 → 92 annotaties over 16 → 17 goldens, en ref en
clingo reproduceren de ongetwiste uitkomsten.

### Fixronde (2026-09-11, na lens 1)

Lens 1 hermat alles — dertien gates, alle 17 ankerverwachtingen zelf
nagerekend en gefalsifieerd, arm-exactheid 6/6 zonder collateral over 373
negatiefs — en vond zes punten. Wat er inhoudelijk uit kwam:

**`ref` droeg WF-102 arm (a) niet.** Gemeten op
`corpus/negatief/wf102-bronpad-naast-bronrol`: `ref` gaf
`oracle: PASS (2 expectations)` waar de ASP-backend
`TRANSLATION FAULT: a bronPad beside a bronRol` gaf. Met beide velden
aanwezig won de padarm stil en werd de binding nooit gelezen — een
twee-uitvoerder-verschil onder een document dat geen regel in dat bestand
weigerde, en onzichtbaar voor elke gate, want een negatief wordt nooit
afgespeeld. Mijn eigen code-comment vroeg erom ("must fault by NAME rather
than be read as the one-step form with the range dropped"). De arm staat er
nu, VÓÓR de andere twee zodat een document dat beide spellingen draagt
daarvoor benoemd wordt en niet voor wat de overlevende spelling toevallig
nog schendt. Rood-eerst gemeten: met de arm weggehaald faalt precies één
check (`WV-3.4b-pad-naast-bronrol-faalt`) en geen andere. De NOTE van het
negatief zegt nu ook WAAROM die zin één ronde onwaar was. De (d)-armen zijn
nagemeten en divergeren niet — beide vouwen leeg — en zijn niet aangeraakt.

**De to-één-clausule had geen uitvoerdersartefact.** Gemeten: geen enkele
`bronPad`-stap in het hele repo is to-één (alle 13 dragers volledig
meervoudig), dus de normatieve zin in ch. 3.4b leunde alleen op een
frontend-emissietest. Beide uitvoerders krijgen nu een check op hetzelfde
document, en het is DISCRIMINEREND gekozen in plaats van slechts
gedefinieerd: Jutter -(`bemanning`/`boot`, BEIDE rollen to-één)→ Boot
-(`bootvangst`/`vondst`, to-many)→ Vondst, naast een DIRECTE
`jutterbezit`-relatie. `J1` vaart `B1` (met `V1` 3,00 en `V2` 7,00) en bezit
daarnaast `V9` (1,00). De unie over het pad is {V1, V2} → **3,00**; wie de
to-één stap laat vallen en het directe paar vouwt antwoordt **1,00**.
Gemeten op precies die twist: zonder `bronPad` blijft hetzelfde document
`verifier: PASS` en antwoorden ref én clingo 1 waar 3 gepind staat. `J2`
vaart niets: leeg bereik bij de EERSTE stap, minimum leeg.

**De AANTAL-divergentie is nu net zo gemarkeerd als die van SOM** — in
IR-SPEC ch. 4.3 en in RUNTIME-SEMANTICS ch. 3.4b — met de scherpere reden:
geen enkele `bronPad`-drager is een AANTAL, en een AANTAL over een SET-bron
vertaalt de ASP-backend überhaupt niet (de setvouwkloof-kaart). De lezing is
hier beslist; haar differentiële getuige wacht op die kaart. Er is bewust
GEEN getuige gebouwd.

**Twee telfouten** in dit Result rechtgezet: "acht getuigen" boven een
negatief-tabel van negen, en "negatief 365 → 373" waar `git ls-tree` op
`acc5a4e` **364** telt (dus +9).

**Twee typo's in beschermde bestanden**, op de branch hersteld: een
verdubbelde "v0" in `spec/IR-SPEC.md` ch. 4.3, en de schema-`description`,
waar mijn em-dash-uitweiding de komma-lijst van extensievelden brak — de
lijst is hersteld en de `bronPad`-toelichting staat nu áchter de lijst.

**Twee eerlijkheden in dit Result:** de gate-11-blokken zijn expliciet als
SAMENGEVAT gemarkeerd met hun reden (de letterlijke regel noemt de twee
holdouts bij bestandsnaam, en die namen mogen hier niet in), en de gate-13-
regels staan nu letterlijk. De pre-existente stilte die lens 1 op probe (d)
vond — een geschreven anker dat NIETS oplost — staat bij "Wat bewust niet
bewoog".

### Gewijzigde beschermde bestanden (voorstel — voor de landing)

- `spec/IR-SPEC.md` — ch. 4.3 (`sourcePath`-rij, het WF-102-blok, WF-083 (c)),
  de WF-index-rij, de extensieveldenlijst.
- `spec/RUNTIME-SEMANTICS.md` — ch. 3.4b (nieuw).
- `spec/schema/regelspraak-ir-v0.schema.json` — `bronPad` op `Functie`, plus de
  description.
- `spec/verifier/lib/{ast,loader,checks}.ml` — `bron_pad` en WF-102.
- `corpus/golden/anchor/jutterswet-bootberging/**` + een rij in
  `corpus/golden/anchor/README.md`.
- `scripts/check-drools.sh` — TWEE regels, allebei kanaalvloer-pins met reden
  (`anchor/jutterswet-bootberging` en `jutterswet-vondstketen`, beide
  `EXCLUDE|loader Refusal: Functie.bronPad`). `EXPECTED_PLAYED` is NIET
  aangeraakt: het blijft 124, want een nieuw toegevoegde golden die niet
  speelt werd nooit meegeteld — precies wat de vloer zelf voorschrijft.
- `scripts/check-reftype-derivation.sh` — GEEN LETTER. De klem en de
  twisttabel hadden geen wijziging nodig; dat is de meting, niet een omissie.

### Gewijzigde bestaande tests, met reden per naam

`frontends/regelspraak/.../GenesteVouwbronTest.java` (van de zusterkaart, één
dag oud). ACHT getuigen pinden de WEIGERING van vormen die deze kaart
uitdrukbaar maakt en stonden dus rood op mijn kop; ze zijn omgezet, niet
geschrapt. De tabel telt NEGEN namen in acht rijen: één rij voegt twee
getuigen samen, en de rolanker-rij is de negende — die stond vóór én ná
groen en is alleen hernoemd, omdat de deelvraag die zij openhield nu
beslecht is.

| was | is | reden |
|---|---|---|
| `geneste_ketenzin_zonder_directe_relatie_wordt_benoemd` | `..._wordt_een_bronpad` | document A resolveert nu; pint het pad in plaats van de weigering |
| `geneste_ketenzin_met_een_direct_feittype_wordt_ook_benoemd` | `..._wordt_ook_een_bronpad` | document B idem, plus: geen `bronRol`, geen `aangemelde vondst` op de wire |
| `de_geneste_vouwbron_wordt_niet_gevouwen` | `een_onoplosbare_keten_blijft_een_markering` | dezelfde asserties, verplaatst naar de gemengde keten, die nog altijd niet oplost |
| `een_tussenstap_in_een_vouwbron_wordt_benoemd` + `de_tussenstap_vouwbron_wordt_niet_gevouwen` | `een_toone_tussenstap_wordt_een_stap_op_het_pad` | document F is nu een tweestaps pad (besluit 4-bonus); de twee zijn samengevoegd en er is een NIEUWE route-(b)-getuige naast gezet, `een_anker_dat_het_onderwerp_niet_is_blijft_geweigerd` |
| `een_rolnaam_als_anker_blijft_buiten_de_weigering` | `een_zelfdenoterend_rolanker_blijft_een_eenstaps_binding` | de open deelvraag is beslecht; dezelfde uitkomst, nu met de reden erbij, plus de nieuwe tegenhanger `een_verre_rol_als_anker_wordt_geweigerd` |
| `de_markering_draagt_het_eigen_resultaattype` | idem | de lijst van drie bronnen verhuisd naar drie vormen die nog steeds weigeren |
| `de_diagnose_is_een_waarschuwing_en_geen_fout` | idem | zelfde reden |
| `de_emissie_draagt_de_diagnose_op_de_wire` | idem | zelfde reden |

Eén helper is uitgebreid: `vouwbronDiagnoses` filtert er de nieuwe
`rolanker in een vouwbron`-prefix bij. Verder is er ÉÉN bestaand testbestand
mechanisch aangeraakt: `ref/test/test_ref.ml` krijgt `bron_pad = None` op negen
`Functie`-constructies — een verplicht recordveld, geen gedragswijziging.
Buiten deze twee bestanden is geen bestaande test gewijzigd.

### Voor de hoofdsessie: re-vendor

Deze landing raakt `frontends/regelspraak`. Gewijzigde bestanden onder die map,
en alleen deze vijf:

- `src/main/java/io/github/yhavinga/regelspraak/ir/Expr.java` (het veld
  `bereikPad` op `AttribuutRef`, plus een delegerende constructor)
- `src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java`
  (`BereikHop`, `bereikHop`, `rolTreffersNaarSubject`, `vultZelfDeRol`)
- `src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java`
  (`bereikPad`, `ankertHetOnderwerp`, de padguard en de rolanker-arm in
  `keurVouwbron`)
- `src/main/java/io/github/yhavinga/regelspraak/emit/IrEmitter.java`
  (`bronPad`, geschreven in plaats van `bronRol`)
- `src/test/java/io/github/yhavinga/regelspraak/GenesteVouwbronTest.java`
  (omgezet, zie hierboven)

### Registerwerk voor de hoofdsessie (serieel, beschermde paden)

- **`spec/CONSTRUCTS.md` rij 1** (het `Functie`-knooppunt). Als ononderbroken
  tekst aan het eind van de cel `v0→v1 delta`:

  **Landed (2026-09-11):** de vouwbron beschrijft haar BEREIK niet langer met
  één relatie maar met een PAD — `bronPad` (v1 `sourcePath`), een lijst
  `RolBinding`s onderwerp-uitwaarts, met `bronRol` als de lengte-1-vorm en de
  twee velden wederzijds uitsluitend (WF-102). Dit is KERNEL door verruiming
  van een bestaand veld en niet door een nieuwe knoop: het GHC-Core-criterium
  vraagt of de vorm naar de kernel desuikert, en de enige kandidaat —
  vouw-in-vouw — HEEFT geen geldige v0-vorm (de verifier kruist de `bronRol`
  van een vouw vast bedraad tegen het regelonderwerp, en er is geen binder die
  het buitenste element als onderwerp doorgeeft) én zegt semantisch iets
  anders. De lezing is de VLAKKE UNIE en is normatief: de verzameling
  `elementObjecttype`-exemplaren die vanaf het onderwerp langs het hele pad
  bereikbaar zijn, als VERZAMELING — een element dat via twee tussenexemplaren
  bereikbaar is draagt één keer bij. De geneste lezing is uitdrukkelijk GEEN
  desuikering: bij MIN/MAX/EERSTE/LAATSTE moet zij beslissen wat een leeg
  binnenresultaat met de buitenste vouw doet en die vier hebben geen
  `leegwaarde`; bij AANTAL telt vlak ELEMENTEN waar genest TUSSENEXEMPLAREN
  telt; en bij SOM vuurt de `leegwaarde` vlak hoogstens één keer waar genest
  hem per leeg tussenexemplaar vuurt (bij een nul-terugval vallen die twee
  numeriek samen, en beide uitvoerders dragen vandaag alleen de nul — benoemd
  en nergens gepind). Diepte is ONBEGRENSD, beslist en niet aangenomen: een
  grens is een constante zonder semantiek die alleen maar kan versoepelen, en
  de v0-toolchain begrenst op twee omdat zij één join-PATROON per stap
  emitteert — een eigenschap van een codegenerator, niet van het bereik. Een
  TO-ÉÉN stap is een gewone stap (multipliciteit komt in de lezing niet voor),
  wat de to-één-tussenstap van de acute flank uitdrukbaar maakt. Gemeten over
  de vier zones: 56 ledenvouwen, NUL met een meerstaps bereik, dus geen enkel
  bestaand document beweegt (gate 10 42 en gate 11 45 byte-identiek). `ref`
  dedupliceert de unie op instantielabel; de ASP-backend krijgt de dedup
  gratis omdat haar aggregaatterm alleen het element noemt en de
  tussenvariabelen existentieel binnen het aggregaat staan; de Drools-backend
  WEIGERT de vorm bij naam (`Functie.bronPad`) in de kanaalvloer van gate 12 —
  expliciet en niet geërfd, want haar loader controleert geen onbekende
  sleutels binnen een knoop en een weggevallen veld laat de type-paar-zoektocht
  een toevallig direct feittype binden: gemeten op de geëmitteerde golden geeft
  hetzelfde document zonder `bronPad` `verifier: PASS` met 5,00 in ref, clingo
  én Drools waar de zin 3,00 zegt.

- **`spec/CONSTRUCTS.md` rij 4** ("Fact types and role navigation"). Als
  ononderbroken tekst aan het eind van de cel `v0→v1 delta`:

  **Landed (2026-09-11):** het ANKER van een vouwbron mag een ROLNAAM zijn, en
  de toets is de VOLGORDE die ch. 4.1a al vastlegt in plaats van een nieuwe
  regel: verre-rolresolutie EERST — een rol die VANAF het onderwerp bereikbaar
  is wijst een ander exemplaar aan en hoort dus als STAP op het `bronPad` —
  en de zelfdenoterende lezing (WF-083 (b): het onderwerptype vult zelf een rol
  van die naam, ci, gedeclareerde naam of gedeclareerd meervoud) alleen bij NUL
  verre treffers, waarna het pad gewoon bij het onderwerp begint. De
  v0-toolchain weigert elk rolanker met het argument dat een rol wiens vuller
  toevallig het eigen type is een ANDER exemplaar aanwijst; dat argument is
  juist en is precies wat verre-rol-eerst vangt, dus uchar weigert dezelfde
  vorm zonder de vorm ernaast te weigeren. De frontend weigert een verre-rol-
  anker nu met een eigen diagnose ("rolanker in een vouwbron") waar er geen pad
  is om de hop op te zetten. Gemeten over de vier zones: van 56 ledenvouwen
  dragen er ZES een anker dat geen objecttype noemt — twee in de negatief-zone
  (`wf093-wratvouw-set-*`, al afgewezen op hun soort) en vier in de
  pariteitszone — en alle zes zijn zelfdenoterend en eenstaps, dus het besluit
  beweegt geen document (gate 10 42 en gate 11 45 byte-identiek). De open
  deelvraag die `20260911-geneste-aggregatie-acute-flank.md` hier liet liggen is
  daarmee beslecht.

- **`spec/PLAN.md`**: geen voorgestelde wijziging. De scope van deze kaart is
  niet veranderd en er is geen besluit bijgekomen dat het plan draagt.

### Wat bewust niet bewoog, en wat open blijft

- **De geneste TELVORM** (het aantal over een keten, elementnaam in de
  attribuutpositie): out of scope, en nu ook mechanisch afgegrendeld — WF-102
  (c) laat een `bronPad` alleen op de SET-bron toe, dus de wrat kan er per
  constructie geen dragen. De tegenproef `de_telvorm_blijft_via_de_geschreven_
  elementnaam_binden` staat er nog.
- **De setlezing BUITEN een vouw** (WF-096): ongemoeid, met tegenproef.
- **De EERSTE/LAATSTE/AANTAL-setvouwkloof**: ongemoeid. Gevolg dat ik hier
  meld in plaats van verberg: een AANTAL over een SET-bron wordt vandaag door
  `backends/clingo` niet vertaald (het enige voorkomen in de vier zones is een
  negatief), dus het AANTAL-verschil tussen de vlakke en de geneste lezing is
  BENOEMD en niet differentieel gepind. Dat is die kaart, niet deze.
- **Drools speelt het pad niet.** Een samengestelde accumulate-bron in beide
  takken plus de opslagrichtingstoets per tussenstap is een eigen gemeten
  kaart met kanaalgevolgen; de kaart stond de benoemde weigering uitdrukkelijk
  toe.
- **Een SOM-leegwaarde die niet nul is** draagt geen uitvoerder, dus de derde
  divergentie met de geneste lezing staat benoemd en ongepind.
- **Een WEDERKERIG feittype als stap** wijst `bereikHop` af (haar
  `elementBinding` eist twee onderscheiden rollen), terwijl WF-047 een
  wederkerige binding wél kent. Conservatief: de wire kan hem dragen, de
  frontend produceert hem niet, en geen zin in het corpus vraagt erom.
- **Een geschreven anker dat NIETS oplost blijft stil — pre-existent gedrag
  van vóór deze kaart, hier benoemd omdat het besluit erlangs loopt.** De
  rolanker-arm in `ExpressieBouwer.keurVouwbron` kent drie uitkomsten: een
  objecttype ≠ onderwerp weigert, een VERRE rol weigert (nieuw), en een naam
  met NUL verre treffers valt door — maar die laatste groep bevat twee dingen:
  de zelfdenoterende naam (terecht, die IS het onderwerp) én een naam die
  helemaal niets denoteert. Gemeten op een eigen probe: "… van alle
  aangemelde vondsten van **de zeemeermin**", waarbij `zeemeermin` geen
  objecttype en geen rol is, geeft `diagnostics: []` en bindt eenstaps via het
  typepaar (`vondstaanmelding`/`aangemelde vondst`). Dat was vóór deze kaart
  al zo — de `continue` in die lus stond er als "a role anchor: the wire
  question, left alone" en dekte beide gevallen — en het staat in de
  code-comment naast de arm, maar het stond niet in dit Result. Het is
  strikt genomen geen rolanker-vraag maar de vraag wat een ONBEKEND anker
  betekent, en die valt onder WF-034's naamresolutie, niet onder ch. 4.3.
