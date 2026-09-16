# typerange-sluiting — de twee open punten van de subtypering-reeks

Goal: de laatste reparatiekaart van de subtypering-reeks. Twee gemeten gaten
blijven over uit de twee lensrondes ervoor, en de maintainer vraagt "geen open
punten". Beide gaan over hetzelfde: een positie die een type NOEMT terwijl de
verzameling die haar bereikt BREDER is dan dat type.

**Gat 1 — de uitsluitingsfilter van een type-rangende positie**
(`backlog/done/20260912-subtype-verdeling-keten.md`, vervolgwerk B; daar
"bevinding 4" van de lens). `IR-SPEC` ch. 2.2 belooft extensionele
kwantificatie op elke typerange-positie. De INsluitingsrichting (een
subtype-instantie telt mee op een supertype-positie) is overal gewitnesst; de
UITsluitingsrichting niet. Gemeten op `p3-wf102-landing.json`: een
`Munt ⊑ Vondst`-vouw met `elementObjecttype: "Munt"` over een pad dat op een
`Vondst`-rol landt telt de mee-gelinkte gewone `Vondst` gewoon mee, en die
bepaalt zelfs het minimum — in ref én clingo dezelfde uitkomst, dus
spec-versus-uitvoerders en geen differentiële divergentie. De verdelings-tak
van dezelfde familie is een ronde eerder al gesloten
(`ref/lib/eval.ml`, ontvangersfilter: identiteit zodra de vuller
`doelType`-of-lager is, filter anders); dat is het model.

**Gat 2 — de frontend emitteert geen doelType op een vooroudersrelatie**
(`backlog/done/20260912-subtype-frontend-keten.md`, vervolgwerk C). Een
verdeling "over alle hoofdbergers" waarvan het feittype op `Berger` hangt
(`Hoofdberger ⊑ Berger`) emitteert een verdeling ZONDER `doelType` →
`WF-034 FAIL "allocation without a target object type"`. Daardoor is precies
het arrangement dat gat 1's zusterreparatie pint — ontvangersverzameling
smaller dan het rolbereik — niet uit een `bron.rgs` schrijfbaar, en is
`golden/jutterswet-hoofdberging` handgeschreven zonder brontweeling.

Acceptance:
- gat 1: de uitsluitingsfilter bestaat generiek op de vouw-elementverzameling
  in ref én clingo; Drools is gemeten en het antwoord (gefilterd of niet)
  gedocumenteerd; de norm is handmatig gerekend (de vouw telt precies de
  gelinkte instanties waarvan het concrete type ⊑ `elementObjecttype`);
- gat 1: een getuige-golden met handberekende pins die de vorm in alle drie
  speelt, met een mee-gelinkte niet-passende instantie die NIET meetelt;
- gat 1: de ch. 2.2-positielijst is systematisch doorzocht op posities met
  hetzelfde gat, per positie gerapporteerd als gedekt / gerepareerd / n.v.t.;
- gat 1: de spec-zin die de lacune nu eerlijk benoemt belooft weer vol, met
  de getuige erbij;
- gat 2: de onderwerp-/doelresolutie van de verdeling bindt een frase die een
  SUBTYPE benoemt op een rol waarvan de vuller een VOOROUDER is
  (⊑-verenigbaar) en emitteert dat subtype als `doelType` plus de rolbinding;
  een rol wiens vuller niet ⊑-verenigbaar is bindt nooit, en meerduidigheid
  weigert;
- gat 2: een `bron.rgs`-getuige die het hoofdberging-arrangement uit bron laat
  komen, byte-identiek re-emitterend (gate 10) en spelend in alle drie;
- alle veertien gates groen; de bestaande 45 rgs-goldens re-emitteren
  byte-identiek; `EXPECTED_PLAYED` per stap apart benoemd, met reden en datum.

May touch: `ref/`, `backends/clingo`, `backends/drools` (alleen indien de
meting het vergt), `frontends/regelspraak` (resolver/emitter voor gat 2),
`corpus/golden/` (de nieuwe getuigen), `scripts/check-drools.sh` (alleen de
`EXPECTED_PLAYED`-stappen, met reden), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (alleen de gerichte zinnen), dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
`spec/verifier/` (tenzij een WF-zin mee moet — dan gemeld), bestaande
corpusbestanden, andere frontends.

Result:

**Beide gaten zijn dichtgegaan, en ze bleken één vraag met twee gezichten:
een positie die een TYPE noemt terwijl de verzameling die haar bereikt
breder is.** Bij gat 1 leverde een relatie te veel instanties aan een vouw;
bij gat 2 kon de frontend het type dat de populatie benoemt niet eens
opzoeken. Alle veertien gates groen.

**Gat 1 — de uitsluitingsfilter.**

*Meting.* De lensmeting reproduceerde exact: `p3-wf102-landing.json` gaf in
ref `3` waar `5` hoort (de mee-gelinkte gewone `Vondst` bepaalde het
minimum). Ik heb er twee probes naast gezet, zodat alle drie de vouw-armen
gedekt zijn en niet alleen de padvorm: de éénstaps `bronRol`-waardevouw en
de `AANTAL`-instantievouw. **Rood op de basis in BEIDE uitvoerders, met
hetzelfde antwoord** — ref `3 / 3 / 3`, clingo `[300] / [300] / [3]` tegen
`5 / 5 / 2` — dus spec-versus-uitvoerders, geen onderlinge divergentie.

*Norm, met de hand.* De vouw telt precies de gelinkte instanties waarvan het
concrete type ⊑ `elementObjecttype`. De relatie levert het BEREIK, het
gedeclareerde elementtype de POPULATIE, en de bijdragers zijn de doorsnede.
Identiteit zodra de rolvuller `elementObjecttype`-of-lager is, dus elk
document van vóór de keten staat stil.

*Fix, generiek.* In ref één helper `fold_members` die beide vouw-armen én
beide bronvormen (`bronRol`, `bronPad`) deelt — precies het model van de
ontvangersfilter uit de vorige ronde. In clingo sluit `source_join` haar
join af met één `isa(X,el)`-atoom; en omdat de `AANTAL`-wart zijn eigen
kopie van de rolafleiding had, loopt die nu door `source_join` heen in
plaats van ernaast. Daarmee wordt de populatie op één plek beslist. Derde
plek meegenomen: de vertaaltijd-hek voor de per-element temporele vouw
scande óók de ruwe bereikverzameling en kon dus struikelen over de
ontbrekende tijdlijn van een instantie die niet eens meedoet.

*Drools: gemeten, en het was de enige die het al goed deed.* De
exec-model-tak matcht het elementtype als JAVA-KLASSEPATROON over beans die
met native `extends` zijn bedraad, dus een `Munt`-patroon zag de gewone
`Vondst` nooit. **Dat maakt dit een driewegdivergentie op een
verifier-schoon document, niet een tweewegs spec-gat** — en ref en clingo
zijn naar Drools toe gecorrigeerd, niet andersom. Geen regel in
`backends/drools` gewijzigd.

*Getuige.* `corpus/golden/jutterswet-munttelling`, handgeschreven IR (zie
"eerlijke afwijkingen" (b) voor waarom er geen bron bij kan). Beide vouwen
declareren `elementObjecttype: Munt` over een rol die `Vondst` vult. Drie
jutters legen de elementverzameling elk anders, en dat is de hele opzet:
J1 verliest één van drie links (2 en 12,00 tegen 3 en 15,00 ongefilterd);
J2's rol raakt wél iets maar de populatie is leeg (0 en de leegwaarde — een
uitvoerder die van de links uitgaat zegt hier 1 en 4,00); J3's enige lid
telt mee maar draagt geen waarde (1 en de leegwaarde). J2 naast J3 is wat
de lege populatie van de ontbrekende waarde scheidt. Rood op de basis in
ref en clingo, groen op de kop, en **gespeeld door alle drie met dezelfde
pins**. `EXPECTED_PLAYED` 135 -> 136, met reden en datum in het script.

*(d) De positietabel.* De ch. 2.2-lijst is niet homogeen, en dat is de
bevinding: de zeven posities verschillen in HOE hun verzameling ontstaat, en
alleen bij twee ervan kunnen de twee richtingen uit elkaar lopen.

| # | ch. 2.2-positie | hoe de verzameling ontstaat | uitsluitingsrichting |
|---|---|---|---|
| 1 | instance fold `elementType` (ch. 4.3) | RELATIE (`bronRol`/`bronPad`) | **gerepareerd**, getuige `golden/jutterswet-munttelling` |
| 2 | rule subject | populatiescan (`World.of_type ~isa`; clingo `isa(S,ot)`) | n.v.t. — de scan ÍS de kwantor |
| 3 | defaults `subject` (ch. 6.1) | populatiescan per concreet type over de effectieve stapel (ch. 7.10) | n.v.t. |
| 4 | kenmerkcheck-eigenaar (ch. 4.5) | geen verzameling: `owner_hosts` is een predicaat op één subject | n.v.t. — niets om uit weg te laten |
| 5 | uniciteitsscope (ch. 3.6) | populatiescan; de scope-hop is groepeersleutel op label | n.v.t. |
| 6 | consistentie-subject | populatiescan | n.v.t. |
| 7 | verdeling `targetType` (ch. 3.2) | RELATIE (`verbindendeRol`) | al gerepareerd 2026-09-12; nu óók uit bron gewitnesst (`bergersdeling`) |

Om zeker te zijn dat de lijst niets mist heb ik niet de ch. 2.2-tekst maar
de CODE opgesomd: élke consument van een gelinkte verzameling in
`ref/lib/eval.ml`. Dat zijn er acht, en de twee hierboven zijn de enige
twee met een tweede, smaller gedeclareerd type ernaast.

| plek | oordeel |
|---|---|
| vouw-elementverzameling | **gerepareerd in deze ronde** |
| verdelings-ontvangers | al gerepareerd (vorige ronde) |
| WF-053 genavigeerde scalaire lezing, eindpunt | **weigert i.p.v. filtert, met reden**: één scalaire sleuf heeft geen verzameling om uit weg te laten, dus de onsound richting faalt luid (ch. 5.8). Bewust ongelijk aan de vouw |
| WF-053 genavigeerde kenmerkcheck | n.v.t. — het bereikte type ÍS de roldeclaratie; er staat geen tweede type naast |
| near-side rolcheck (ch. 5.8a) | n.v.t. — `anderTeFiller` is relatieselector; de uitkomst is een boolean over het SUBJECT, niet over de bereikte instantie |
| WF-028 feitcreatie-ketenhop + eindpunt | **gedekt door weigering**: de verifier vergelijkt het keteneindpunt LETTERLIJK, dus de richting die een filter zou vergen bereikt geen uitvoerder. Zie het meldpunt hieronder |
| objectcreatie-idempotentiewacht | n.v.t. — vraagt of een naar-één rol al gevuld is; het nieuwe type wordt geslagen, niet geselecteerd |
| uniciteits-scopehop | n.v.t. — de scope-ouder is groepeersleutel op label; de wire declareert er geen type voor |

*(e) De spec-zin.* `IR-SPEC` ch. 2.2 belooft weer vol en zegt er nu de
structuur bij die de lacune verklaarbaar maakt: vier posities zijn een
populatiescan, één is een predicaat op één subject, en alleen de twee
relatie-gevoede posities zijn twee LOSSE beweringen — pas bereikbaar sinds
WF-047 op ⊑-verenigbaarheid staat. De drieweg-geschiedenis staat er
expliciet in (ref en clingo naar Drools toe gecorrigeerd). Verder:
`elementType` en `sourceRole` in de Aggregation-tabel zeggen nu bereik
versus populatie, en `RUNTIME-SEMANTICS` ch. 3.4a krijgt de definitie van
de bijdragers mét de drie jutters als worked example; ch. 3.4b zegt dat het
pad de LANDING narrowt en niet de tussenstappen.

**Gat 2 — de doelType-emissie.**

*Meting, en de oorzaak was een andere dan vermoed.* De kaart vermoedde "een
lookup die alleen exacte vullers accepteert". Dat klopt niet: de rolbinding
liep allang door ⊑ (`rolHeeftObjecttype` → `ot.isEen(filler)`), en de
resolutie kwam niet eens tot die vraag toe. Gemeten met drie varianten en
daarna met een debugafdruk in `attribuutRef`: `resolveObjecttype` kent een
objecttype alleen op zijn **enkelvoud** (`objecttypen.get`, ci) en leest de
naam anders als **rol** (naam of rolmeervoud). "Alle bergers" bond daardoor
via de RÓL `berger (mv: bergers)`; "alle hoofdbergers" bond nergens op,
want een populatie die een subtype benoemt heeft per constructie geen eigen
rol — de relatie is één keer gedeclareerd, op de voorouder. Dus
`ot == null`, dus `doelType: null`, dus `WF-034 FAIL /verdelingen/0/doelType`.

*Fix.* De ambiguïteitsveilige `MeervoudMatcher.vindObjecttype` — die de
uniciteitscontrole al gebruikt — beslist nu ook op de posities die een
POPULATIE benoemen: na de enkelvouds- en de rollezing, en vóór de oudere
diepe parse-tree-scan, zodat een geschreven meervoud dat een gedeclareerd
type benoemt hier wordt beantwoord in plaats van door de
diepste-match-gok van die scan. Elke enkelvoudsnaam en elke rol lost
precies zo op als vandaag, en na de lensronde is de matcher op ELKE pas
ambiguïteitsveilig: een gedeeld gedeclareerd (mv: …) bindt geen van
beide, waarna de ongebonden naam luid op WF-034 valt (gemeten: zonder de
guard passeerde het tweelingdocument verifier-schoon met een stil
gekozen type; met de guard weigert de verifier het). Twee aanroepplaatsen, dezelfde helper: het set-onderwerp
(`resolveOnderwerp`) en het elementwoord van een telling (`aantalElement`).
De rolbinding hoefde niet mee.

*Getuige.* `corpus/golden/jutterswet-bergersdeling`: de brontweeling die
`jutterswet-hoofdberging` miste, met diens case ongewijzigd en dus diens
drie handberekende getallen (500/500/leeg, rest 1). Rood op de basis —
`WF-034 FAIL /verdelingen/0/doelType: allocation without a target object
type` op de basis-emissie van exact deze `bron.rgs` — groen op de kop, en
gespeeld door alle drie. `EXPECTED_PLAYED` 136 -> 137. De bestaande 45
rgs-goldens re-emitteren byte-identiek (gate 10: 46 nu; gate 11 ©-zone
ongewijzigd, 45 byte-identiek).

*De vouw-onderwerp-variant, meegemeten en half geland.* Dezelfde blindheid
trof het vouw-onderwerp: "het aantal Munten van de Jutter" gaf
`elementObjecttype: null`. Dat deel is nu óók opgelost (`Munt` bindt), maar
de vouw komt er nog steeds niet uit — de vouwBRON weigert luid, met een
WARNING die zegt dat de zin een TYPE noemt terwijl het enige verbindende
feittype de rol `vondst` draagt, en dat een type-paar-afleiding een andere
verzameling zou vouwen dan de zin noemt. Die weigering is bewust en van een
eerdere ronde; ik heb haar NIET verzwakt. Zie vervolgwerk A.

**Registervoorstel** (letterlijke tekst, hoofdsessie landt het),
`spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:

> De extensionele belofte van ch. 2.2 gold tot 2026-09-16 maar half. Haar
> zeven posities verschillen in hoe hun verzameling ontstaat: vier zijn een
> populatiescan en één is een predicaat op één subject, en daar kunnen
> in- en uitsluiting niet uit elkaar lopen. Alleen de twee die een
> gedeclareerd type tegen een door een RELATIE geleverde verzameling zetten
> — de instantievouw en de verdeling — zijn twee losse beweringen, en pas
> sinds WF-047's "agree" ⊑-verenigbaarheid werd is de tweede bereikbaar.
> De verdeling is 2026-09-12 gesloten; de vouw hier. Ref en clingo vouwden
> de bereikte verzameling heel en telden een mee-gelinkte instantie mee die
> het elementtype niet draagt, terwijl de Drools-backend dat nooit deed —
> haar exec-model-tak matcht het elementtype als klassepatroon over beans
> met native `extends`. Dat was dus een DRIEWEGdivergentie op een
> verifier-schoon document, en de twee interpreters zijn naar de backend toe
> gecorrigeerd; getuige `golden/jutterswet-munttelling`. Bij dezelfde ronde
> bleek de dialect-frontend een populatie alleen te kennen op het
> ENKELVOUD van een objecttype of op een gelijknamige ROL, zodat een
> populatie die een SUBTYPE benoemt — dat per constructie geen eigen rol
> heeft, want de relatie staat op de voorouder — nergens op bond en de
> verdeling zonder doeltype de wire op ging (WF-034). Hersteld met de
> ambiguïteitsveilige meervoudsmatcher als laatste kandidaat; getuige
> `golden/jutterswet-bergersdeling`, de brontweeling die
> `golden/jutterswet-hoofdberging` miste.

**Twee stale plekken in beschermde bestanden, gemeld en niet aangeraakt.**
Beide zijn door DEZE reparatie onwaar geworden; de letterlijke
vervangtekst staat hieronder zodat de hoofdsessie ze kan overnemen.

1. `spec/verifier/lib/checks.ml`, het commentaar bij WF-028's
   keteneindpunt (r. ~7529) zegt: *"it sits on the open question that no
   executor filters a reached set by a declared type"*. Die open vraag is nu
   gesloten voor de vouw. Voorgestelde vervanging van die ene zin:

   > the sound direction (the chain reaches a strict SUBTYPE of the target)
   > would be a widening, and it is held back: the fold and the allocation
   > now both narrow a reached set to their declared type (ch. 2.2), so the
   > machinery exists, but a creation LINKS rather than folds — a narrowed
   > chain would silently create fewer relations instead of fewer
   > contributors — and that deserves its own measurement rather than
   > riding along here.

2. `spec/ASP-TRANSLATION.md` §S8 pint de vouwlichamen als
   `rol(S,role,X), ⟨filter atoms⟩` (count) en
   `rol(S,role,X), val(X,a,G), ⟨filter⟩` (sum). Beide dragen nu het
   elementtype-atoom. Voorgestelde toevoeging aan §S8, na de count-alinea:

   > Every fold join closes with `isa(X,el)` on the ELEMENT variable — the
   > ch. 2.2 population, joined against the reach the `rol/3` atoms
   > deliver. It is emitted unconditionally, including for a `bronPad`
   > (where it lands on the last step only, never on an intermediate), and
   > it is the same chain-closed `isa/2` the allocation's `vdN_ontv` joins
   > on. On a document without a supertype edge the atom is satisfied by
   > every instance the role yields, so the encoding is unchanged.

**Vervolgwerk — gemeten, niet in deze ronde gerepareerd.**

*A. De vouwbron weigert een geschreven TYPEnaam.* Met het elementtype nu
oplosbaar is "het aantal Munten van de Jutter" nog steeds niet schrijfbaar:
de vouwbron eist dat de geschreven naam een ROL noemt, en valt anders terug
op een luide weigering in plaats van op de type-paar-afleiding. Die
weigering is van de `bereikPad`-ronde en heeft een goede grond (de
by-pair-afleiding bindt een toevallige directe relatie en vouwt dan een
andere verzameling dan de zin noemt). De vraag of een naam die ondubbelzinnig
een POPULATIE benoemt — geen rol, wél een gedeclareerd type, precies één
verbindend feittype — die weigering verdient, is een taalontwerpvraag met een
eigen meting. Zolang die staat is `jutterswet-munttelling` handgeschreven.

*B. WF-028's keteneindpunt verbreden.* Zie het meldpunt hierboven: de
uitsluitingsmachinerie bestaat nu, maar een creatie LINKT in plaats van te
vouwen, dus de betekenis van een versmalde keten is een andere vraag
(minder relaties in plaats van minder bijdragers). Eigen kaart, eigen meting.

**Eerlijke afwijkingen.**

(a) De kaart schreef gat 2 toe aan "een lookup die alleen exacte vullers
accepteert". Nagemeten is dat onjuist: de rolbinding liep allang door ⊑, en
de resolutie strandde één stap eerder, op een objecttype-meervoud dat
nergens een sleutel was. De fix zit dus op een andere plek dan de kaart
aanwees, en de ⊑-kant hoefde niet te worden aangeraakt. Het onderscheid doet
ertoe: er was geen te enge ⊑-controle om te verbreden, dus er is ook geen
nieuwe manier bijgekomen waarop een rol verkeerd kan binden.

(b) Gat 1's getuige is handgeschreven IR en heeft geen brontweeling, terwijl
gat 2's getuige er juist één IS. De reden is vervolgwerk A en niet luiheid:
de frontend weigert de vorm luid en bewust, en die weigering verzwakken is
een eigen, gemeten kaart — niet iets om en passant te doen om een golden een
bron te geven.

(c) Geen JUnit-getuige toegevoegd voor de frontend-fix. De regel "geen
testbestanden aanraken zonder te vragen" staat boven de gewoonte van de
vorige ronde; de dekking loopt nu via `golden/jutterswet-bergersdeling`,
dat door gate 3, 5, 6, 10 en 12 gaat en op de basis rood is met een
benoemde WF-034-weigering. De ambiguïteitsarm van de nieuwe resolutie leunt
op de bestaande unittests van `MeervoudMatcher.vindObjecttype`, die ik niet
heb uitgebreid.

(d) De `AANTAL`-wart in clingo is geherstructureerd in plaats van alleen
aangevuld: hij had een eigen kopie van de rolafleiding naast `source_join`.
Gedragsbehoudend (beide armen kozen dezelfde rol; alleen het
ambiguïteitsoordeel is wart-eigen en staat waar het stond), en gedaan omdat
twee kopieën van de bronafleiding betekent dat de elementscope op twee
plekken moet worden bijgehouden.

(e) Één plek buiten de letterlijke opdracht meegenomen: het vertaaltijd-hek
voor de per-element temporele vouw in clingo scande de ONgefilterde
bereikverzameling en kon dus een instantie aanwijzen die niet meedoet. Zelfde
regel, zelfde ronde; het los laten staan zou de twee lezingen van één vouw
uit elkaar laten lopen.


*Lensronde-navondsten, vastgelegd als vervolgwerk:* (i) de AANTAL-arm komt
na deze kaart wél uit de frontend zonder bronRol (type-paar-afleiding;
de meerduidige variant valt luid op WF-034+WF-048) maar heeft geen golden
of JUnit-getuige; (ii) de bronPad-tak, clingo's MIN/MAX-arm en het
temporele vertaaltijd-hek zijn gerepareerd en per probe bevestigd, maar
geen corpus-golden dekt ze (Drools weigert bronPad per vorm, dus een
driewegspeler kan daar niet — gate 5+6-dekking kan wel); (iii) de
near-side-rolcheck is n.v.t. op grond van RUNTIME-SEMANTICS ch. 5.8a (de
geschreven rolnaam is presentatie; anderTeFiller is een relatieselector),
niet om de boolean-vorm van de uitkomst — de lens-getuige (een
Rollidmaatschap wiens anderTeFiller nergens door een instantie wordt
gedragen, antwoordt waar) hoort bij een eventuele herziening van die
clausule.
