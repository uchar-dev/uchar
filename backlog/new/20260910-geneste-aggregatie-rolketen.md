# De geneste to-many-aggregatiebron: de wire draagt maar één rolstap

Goal: uchar's vouwbron beschrijft precies ÉÉN relatie — `elementObjecttype`
plus één `bronRol` (`spec/IR-SPEC.md` ch. 4.3). Een aggregatie waarvan de
bron TWEE to-many stappen stapelt ("het minimum van X van alle A van alle B
van het onderwerp") is op die wire niet uitdrukbaar. De RegelSpraak-frontend
accepteert de zin toch, zonder één diagnostiek, en gooit de buitenste stap
weg. Waar toevallig een DIRECTE relatie tussen onderwerp en elementtype
bestaat, vult de emitter die in — en dan is het document verifier-schoon en
antwoorden `ref`, de ASP-backend én Drools eensluidend over de VERKEERDE
verzameling. Deze kaart beslist de wire-vorm van een meerstaps
aggregatiebereik, spec-first, en sluit tegelijk de stille misbinding af.

Deze kaart is GROOT. Ze raakt spec, schema, verifier, negatief-zone, corpus,
frontend en alle drie de uitvoerders (Drools in twee takken). Zie
"Voorgestelde aanpak" voor de voorgestelde splitsing in twee kaarten; de
eerste helft is klein en sluit de acute flank alleen.

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
guard `&& !isSetOnderwerp(onderwerp)` op **regel 1622** sluit een set-onderwerp
uit voordat de wandeling begint. Dat is de plek waar de acute-flankfix hieronder
moet landen. Bovendien kan uchar's WIRE de keten helemaal niet noteren, ook niet
als de frontend hem zou oplossen.
De `Functie`-knoop draagt één `elementObjecttype` (één typenaam) en één
optionele `bronRol` (één `RolBinding`). Er is geen ketenveld, en de twee
velden die een pad zouden kunnen dragen zijn op een vouwbron expliciet
verboden: WF-083 (a) verklaart een niet-lege `navigatiePad` op een vouwbron
ill-formed, en de emitter-discipline bij WF-053 verklaart
`navigatieBindingen` op een vouwbron ill-formed. Dit is dus een spec-first
kaart, niet een implementatiekaart.

## Meting (2026-09-10)

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
oorzaak staat op één plek: `emit/IrEmitter.java:767` leidt de `bronRol` van
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
   KANDIDAAT-nummer voor de padregel is WF-101 (bijgewerkt 2026-09-10:
   WF-098 is inmiddels bezet door de contextwaarde-namenregel, ch. 4.1, kaart
   `20260909-constante-naam-gesloten-verzameling`, WF-099 door de
   begrenzing-grenseenheidsregel, ch. 4.6, kaart
   `20260909-begrensd-grens-verifier-omheining`, en WF-100 door de
   kenmerkeigenaar-regel, ch. 4.6 (q1), kaart
   `20260909-eigenaar-verklaart-kenmerk`); niet reserveren voordat de
   vorm vaststaat. **Let op de dubbeling:** `20260910-reverse-rol-via-naam.md`
   noemt WF-101 óók als kandidaat, voor haar eenzijdige-leveringsregel. Wie het
   eerst landt gebruikt het nummer, de ander schuift op.
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

**De acute flank kan en moet EERDER landen, als aparte kleine kaart.**
Onafhankelijk van de wire-verruiming moet de frontend ophouden een
tweestaps-zin op een éénstaps-relatie af te beelden. De kleinste vorm: waar
het set-onderwerp zelf weer een set-stap nest, weigert de resolver met een
gerichte diagnostiek en emitteert de regel niet — dezelfde tweesprong die
`20260909-frontend-alle-zonder-vouw.md` voor een andere vorm weegt
(diagnostiek versus andere emissie), en die kaart moet hier meegelezen
worden. Overweeg daarnaast, in dezelfde kleine kaart, of de emitter de
type-paar-afleiding van `bronRol` mag doen wanneer de geschreven rolnaam
GEEN van de rollen van het gevonden feittype is: dat is de tweede helft van
de stille misbinding en ze staat los van de ketenvraag.

## Acceptance

- Eén besluit over de wire-vorm van een meerstaps aggregatiebereik, met de
  vlakke-unie-lezing normatief vastgelegd en het verschil met de geneste
  lezing expliciet benoemd bij MIN/MAX, AANTAL en SOM-met-`leegwaarde`.
- De diepte-vraag beslecht met argument (twee stappen als de werkbank, of
  ongelimiteerd), niet als aanname.
- Elke nieuwe normatieve MUST draagt een ID, een verifier-check, minstens één
  slagende golden en één negatief-getuige (`CLAUDE.md`, Spec discipline).
- Document B hierboven is een verplichte getuige: NA de kaart mag het niet
  meer verifier-schoon-met-fout-antwoord zijn. Óf de frontend weigert de
  bron, óf de wire draagt de keten en alle drie de uitvoerders antwoorden
  3.00. Een derde uitkomst — nog steeds stil 5.00 — is een gefaalde kaart.
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
besluit. Gemengde ketens waarin een to-one stap voorkomt: benoemen als open
vraag, niet in deze kaart beslechten tenzij de vlakke-unie-lezing het
gratis meebrengt. De geneste TELVORM (het aantal-over-een-keten), waarvan de
elementnaam in de attribuutpositie staat: een andere zinsvorm, eigen kaart.
