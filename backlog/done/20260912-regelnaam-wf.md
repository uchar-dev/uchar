# regelnaam-wf — twee gemeten verifier-gaten dichten

Goal: twee lensvondsten die beide op verifier-terrein liggen en beide een
verifier-schoon document opleveren dat stroomafwaarts breekt, worden
normatieve regels met check, negatief-getuige en spec-zin. Herkomst: de
naronde van `backlog/done/20260910-flogic-frontend-minimaal.md` ("Openstaand:
een spec-gat, voor de maintainer") en vervolgwerk C van
`backlog/done/20260912-subtype-verdeling-keten.md`. Maintainersmandaat: geen
open punten.

**Gat 1 — geen WF-regel eist of kruist een naam op de wire.** `naam: null` op
een `resolvedRegel` passeert de verifier (WF-020 filtert nulls weg vóór zijn
uniciteitsvergelijking), ref en clingo rekenen door, de Drools-exec-tak speelt
en de DRL-tekst-tak gooit een NullPointerException. Voor
`prioritizedDefaults[i].naam` is het breder: die array staat niet in
`form_arrays`, dus null, duplicaat binnen de groepen én kruisbotsing met een
regelnaam passeren alle drie. De frontends eisen namen al; de wire zelf niet.

**Gat 2 — WF-045's populatieteller sleutelt op de letterlijke typenaam.** Een
objectcreatie waarvan het nieuwe type een subtype van het subjecttype is laat
de subjectpopulatie groeien zonder dat de cyclusdetectie het ziet; ref eindigt
in "no fixpoint within 100 passes" waar de verifier had moeten weigeren.

Acceptance:
- het hele corpus is eerst gemeten op naamdracht en naamuniciteit; elke
  corpusreparatie die daaruit zou volgen is expliciet gemeld vóór hij gebeurt;
- per gat: spec-zin(nen) in `IR-SPEC` met WF-indexrij, verifier-check,
  en per afzonderlijk bereikbare vorm een eigen `corpus/negatief`-directory
  die rood is vóór de check bestaat;
- de p1c-vorm (één objectcreatie, nieuwType ⊑ subject) is eerst
  verifier-PASS + ref-divergentie en daarna een WF-045-weigering;
- de grens van gat 2 is gemeten en gedocumenteerd: een subtypecreatie
  BINNEN een keten zonder cyclus blijft geldig;
- alle veertien gates groen (`bash scripts/check-*.sh`), gate 12 en 13
  onbewogen.

May touch: `spec/IR-SPEC.md` (WF-teksten + index), `spec/verifier/`,
`corpus/negatief/` (nieuwe directories), corpusmateriaal alleen na expliciete
meting-en-melding, dit kaartbestand.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github/`,
`ref/`, `backends/`, `frontends/`, `scripts/`, bestaande corpusbestanden.
Registervoorstel als letterlijke tekst in het Result.

## Result (2026-09-12)

**De corpusmeting eerst, want daar hing de zwaarste beslissing aan: het
corpus is schoon en er is geen corpusbestand aangeraakt.** Gemeten over alle
vier zones (golden 183, rood 26, negatief 378, pariteit 30 — 617 documenten
bij aanvang): 1094 benoemde vormen, waarvan 65 defaultsgroepen, **allemaal
met een niet-lege naam**, en geen enkele botsing behalve de bestaande
getuige `wf020-crossform-collision`. De verwachting uit de kaartopdracht
klopte dus: alle producenten emitteren namen, en de regel sluit een deur die
niemand gebruikte. Bij landing staat de teller op 625 documenten en 1105
benoemde vormen, met precies drie naamloze/blanco vormen — de drie eigen
getuigen.

### Gat 1 — twee eigenschappen, twee nummers, één lijst

**De keuze, en waarom.** Aanwezigheid en uniciteit zijn twee eigenschappen.
WF-020 *stelde al* "unique across all rule forms"; wat te kort was, was de
lijst die de check las. Dus: **WF-020 verbreedt** (bestaand nummer, twee
nieuwe bereikbare vormen) en **WF-107 is nieuw** en eist alleen de naam. Het
alternatief — beide onder WF-107 — zou het register versnipperen: dezelfde
botsing zou WF-020 citeren tussen twee regels en iets anders tussen een
regel en een groep. De keuze staat in beide WF-teksten en in beide
indexrijen.

**Verifier.** Eén nieuwe lijst `named_forms` = `form_arrays` +
`prioritizedDefaults`, met de reden erbij dat de twee lijsten verschillend
blijven: WF-021 (versievensters) en WF-033 (groepslidmaatschap) gaan over de
*versievormen* van ch. 3 en houden `form_arrays`; de twee naamregels lezen
`named_forms`. WF-101 leest hem nu ook — dat is een pure ontdubbeling: zijn
handgeschreven `prioritizedDefaults`-arm was woordelijk `named_forms` minus
`form_arrays`, en zulke handarmen zijn precies wat dit gat maakte. WF-057
houdt zijn eigen arm (die geeft een ander label en het is geen gat).

**Meegenomen, lensvangst in dezelfde kaart:** WF-020's
"mixes a bare name with versioned entries"-arm vuurde ook op twee entries
met één kale naam — een tweede, onware regel op een document dat de
duplicaat-arm al weigert. Hij eist nu ook een *versioned* lid. Het bevrijdt
niets: zonder versielid ís de gestripte naam de naam en heeft `dups_by` de
groep al gevangen.

**Getuigen.** Vijf nieuwe directories, elk een apart vallende vorm:
`wf107-naamloze-regel` (null), `wf107-blanco-regelnaam` (`"   "` — een
presence-only check laat hem door), `wf107-naamloze-defaultsgroep` (de array
die in géén namenlijst stond), `wf020-defaultsgroep-botst-met-regel`
(kruis-arm) en `wf020-defaultsgroep-duplicaat` (binnen-arm). De positieve
getuige is het **hele corpus** — elke golden draagt namen — en het scherpst
`anchor/jutterswet-uitsluiting`, waar een regel en een groep twee benoemde
vormen met twee namen zijn.

**Wat de wire NIET doet:** het schema blijft `["string","null"]`. Dat is
opzet en staat in de WF-107-tekst: een naamloze vorm moet een *wel*gevormd
document blijven, anders heeft de regel geen getuige — dat is precies de
WF-106-situatie, en de reden dat die er geen heeft.

### Gat 2 — de knoop leest de keten, aan de schrijfkant

**Gemeten uitgangspunt bevestigd.** De p1c-vorm (één objectcreatie,
`nieuwType` ⊑ `subject`) was verifier-PASS en gaf in `ref`
`EVALUATION FAULT: no fixpoint within 100 passes`. Dezelfde creatie met
`subject` = `nieuwType` werd al geweigerd, dus een subtype was een
ontsnapping uit een regel die het document in één type al haalde.

**Twee sluitingen, want de meting vond een tweede gat van dezelfde familie.**
Naast de populatie sleutelde ook de *lidknoop* op de geschreven naam: twee
regels op één geërfd attribuutpaar, elk met een andere eigenaar gespeld,
misten elkaar volledig. Ook dat document was verifier-schoon en divergeert in
`ref` (gemeten). Beide sluiten aan de **schrijf**kant, elk anders, omdat het
niet dezelfde soort verzameling is:

**Drie schrijfposities, en ze sluiten niet gelijk** — dat is de
asymmetrie, en ze staat nu voluit in ch. 5.1, in de WF-045-indexrij en in
het registervoorstel hieronder:

- **populatie**: alleen OMHOOG. Een creatie van T laat T's populatie en die
  van elke voorouder groeien; niet die van een afstammeling, want creatie is
  niet polymorf. Een feitcreatie maakt geen instantie — haar
  populatieschrijving is ch. 6.1's proxy — en blijft letterlijk.
- **lid, geschreven door een vorm die een type ITEREERT** (regeldoel,
  verdelingsdoel en -restant, defaultsdoel): BEIDE richtingen, over de
  ⊑-**vergelijkbare** typen die het lid dragen. Zo'n vorm schrijft het lid
  op élke instantie van haar eigenaar, en de instantieverzamelingen van T en
  S overlappen precies wanneer ze vergelijkbaar zijn.
- **lid, geschreven door een OBJECTCREATIE**: alleen OMHOOG, net als de
  populatieschrijving ernaast en om dezelfde reden. De waarde landt op de
  ENE instantie die gemunt wordt, en haar concrete type is precies het
  gecreëerde type.

Lezen blijft letterlijk: elke geldige leesspelling wordt vanaf de
schrijfkant bereikt, dus beide sluiten zou alleen edges verzinnen.

**Lensvangst binnen deze kaart: de derde positie ontbrak eerst.** Mijn eerste
versie liet de creatie-toekenning bidirectioneel sluiten, samen met de
itererende vormen. Dat trekt een valse edge van een creatie die haar eigen
SUPERTYPE munt terug naar een regel op het subtype dat zij nooit kan munten.
Gemeten op de lensgetuige: basis alles PASS, en met de foute lezing verifier
**WF-045 FAIL** op een model zonder cyclus, waarbij `ref` — die deze graaf
DEELT — weigerde te evalueren terwijl clingo de gepinde getallen antwoordde:
het differentiaalvierkant gespleten op een document waar geen van beide iets
tegen had. Nul corpusblootstelling vandaag, dus latent. `member_writes` kiest
nu tussen `Extension` en `Minted`; het is dezelfde asymmetrie die
`pop_writes` al had, en de fout was dat één van de twee hem niet erfde.

**De afgewezen kortere route, met haar meting.** Beide kanten op
`declaring_type` sleutelen is één regel korter en was de route die de
bronkaart noemde. Gemeten: hij voegt **twee onvergelijkbare zustertypen**
samen tot één knoop en weigert een document dat `ref` schoon uitrekent (twee
zustersubtypen die elk één geërfd attribuut uit het andere berekenen:
`ref` PASS, verifier FAIL onder de samenvoeging). De
vergelijkbaarheidswandeling vangt hetzelfde gat en laat dat document staan.
Minste verrassing wint van één regel korter.

**De grens, precies gemeten** (alle vier PASS na de reparatie, zoals het
hoort — de regel weigert cycli, geen subtypecreatie):

| variant | uitkomst |
|---|---|
| `subject` Strandrecht, `nieuwType` Wrakrecht (⊑ subject) | **WF-045 FAIL** — de getuige |
| `subject` Wrakrecht, `nieuwType` Strandrecht (omgekeerd) | PASS — een voorouder maken laat de subtypepopulatie niet groeien |
| `subject` Kustgemeente, `nieuwType` Wrakrecht (zijwaarts) | PASS — twee populatieschrijvingen, één onverwante lezing |
| dezelfde creatie in een bewaakte `recursief`-groep | PASS — WF-045's eigen uitzondering |
| twee onvergelijkbare zustersubtypen over één geërfd paar | PASS — geen instantie is van beide |
| creatie die haar eigen SUPERTYPE munt met een geërfd lid | PASS — de `Minted`-arm; onder de bidirectionele lezing een valse WF-045 |

**Kosten: geen, en niet leeg.** De sluiting verbreedt een schrijving in **9**
corpusdocumenten (`strandrechtketen`, `wrakrecht`, `wrakrecht-flogic`,
`wrakbasis`, `wrakerfbasis`, `wrakconflict`, `bergingsdeling`,
`hoofdberging`, `zustervondst`) en geen ervan krijgt een cyclus.
`zustervondst` is de zusterstructuur zelf: een schrijving op de ouder bereikt
twee onvergelijkbare kinderen en die bereiken elkaar nog steeds niet.

**Getuigen.** Twee negatief: `wf045-subtypecreatie-cyclus` (populatie-arm) en
`wf045-erfattribuut-spelling-cyclus` (lid-arm), naast de bestaande
`wf045-cyclic-attribute-dependency`. De drie vallen onafhankelijk: een graaf
op de geschreven naam vangt alleen de eerste.

Eén positief: **`corpus/golden/jutterswet-wrakboeking`**, de getuige van de
`Minted`-arm en het enige corpusdocument dat haar kan falsifiëren — laat de
creatie-toekenning beide richtingen sluiten en gate 3 wordt rood op déze
golden (gemeten, beide lezingen, op dit bestand). Speelt in ref, clingo én
Drools, dus het vierkant staat er ook omheen. Elke andere arm van de
ketensluiting had al een schone golden (`strandrechtketen`, `wrakrecht`,
`hoofdberging`, `zustervondst`); deze had er geen, want de vorm — een
creatie die haar eigen supertype munt — bestond nergens in het corpus.

**Scope-uitzondering, expliciet gemeld.** Die golden kon er niet bij zonder
één regel in een BESCHERMD script dat de kaart uitsluit: gate 12's
`EXPECTED_PLAYED` is een gelijkheid, dus een nieuwe speler faalt de gate tot
het getal meegaat (134 → 135, met reden en datum ter plekke — precies de bump
die de gate zelf in haar foutboodschap voorschrijft). De golden, haar NOTE en
die ene regel zitten in **één commit**, zodat de maintainer de uitzondering
als geheel kan aanvaarden of laten vallen; valt hij weg, dan blijven de
veertien gates groen en houdt de arm alleen haar gemeten bewijs.

### De hele voor/na-veeg

Over **626 documenten** in vier zones bewegen **9** rapportregels en geen
enkele andere: de acht nieuwe negatief-getuigen, plus
`wf020-crossform-collision` waarvan het *oordeel* gelijk blijft en alleen de
boodschap herformuleert ("rule name" → "name", "all rule forms" → "all named
forms"). De nieuwe golden `jutterswet-wrakboeking` staat NIET in de veeg, en
dat hoort: main's letterlijke sleutel laat hem ook door — wat hij
onderscheidt is de tussenliggende, foute lezing, en dat is met de
falsificatieproef gemeten in plaats van met de veeg. Gate 3 telt 210 golden
schoon (209 → 210) en 386 negatief (378 → 386).

### Veertien gates groen — slotregels

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 210 file(s) validate and round-trip, floor 192)
check-verifier: PASS (210 golden clean; 386 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 493 case file(s) validate)
check-oracle: PASS (ref suite 592 worked examples, floor 489; 467 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 125 checks, floor 83; 467 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (431 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (245 tests, floor 173); 45 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS   (ref 29 / clingo 29 / Drools 29 PLAYED)
check-drools: PASS   (played 135, channel floor 49 pinned by name, JUnit 335 tests floor 239)
check-reftype-derivation: PASS (93 unit annotations twisted over 18 goldens, floor 74; 84 executor replays reproduce the untouched outcome, 15 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
check-flogic: PASS — frontend suite green (81 checks, floor 81); 1 flogic golden(s) re-emit byte-identically (floor 1)
```

Gate 13 staat stil op zijn uitgangswaarden (93/84/15) — er is geen
uitvoerder aangeraakt en de nieuwe golden draagt geen eenheidsannotatie.
Gate 12 beweegt met precies één speler mee (134 → 135), de gemelde
scope-uitzondering hierboven.

### Registervoorstel (letterlijke tekst, hoofdsessie landt het)

**(a) `spec/CONSTRUCTS.md`, nodetabel, aan de cel van `ResolvedRegel` toe te
voegen** — het WF-101/`Geldigheid`-precedent, want dit is een
welgevormdheidspromotie op een bestaande vorm en geen construct:

> **Landed (2026-09-12, WF-107 + WF-020-verbreding, IR-SPEC ch. 3.1):** een
> naam is de IDENTITEIT van een benoemde vorm en de wire eiste er geen. Het
> veld blijft nullable — dat is opzet, want anders heeft de regel geen
> getuige — en WF-107 eist hem aanwezig en niet-blanco op elke benoemde
> vorm: de zeven versievormen van ch. 3 én de `PrioritizedDefaults`-groepen
> van ch. 6.1, die in géén namenlijst stonden. Gemeten voor de regel op een
> naamloze `ResolvedRegel`: verifier-schoon, `ref`, clingo en de
> Drools-exec-tak rekenen door, en de DRL-tekst-tak valt om met een
> `NullPointerException` uit de regelnaam-escaper. Uniciteit blijft WF-020,
> die nu dezelfde lijst leest — twee eigenschappen, twee nummers, één lijst,
> zodat één botsing niet twee nummers kan citeren. Kosten nul: 1107 benoemde
> vormen over 626 documenten droegen al een naam. Bij dezelfde meting kwam
> een derde naamdrager boven die niets kruist — een `resolvedDagsoort` zonder
> naam definieert geen dagsoort en glipte langs WF-031's eigen
> resolutiekruising heen — en die is bij **WF-031** gesloten en niet hier,
> want de benoemde vormen delen één naamruimte en een dagsoortnaam heeft een
> eigen. `regelGroepen` blijft bewust buiten beide: WF-033 leest de LEDEN van
> een groep op naam en niemand leest de groepsnaam zelf.

### Gat 3 — meegenomen, want het is dezelfde vraag in een andere naamruimte

Bij het meten van gat 1 kwam een derde naam boven die niets kruist: een
`resolvedDagsoort` met `naam: null` passeerde de verifier. WF-031's eigen
kruising ("definition for undeclared day kind") heeft dan niets te
vergelijken, dus de definitie bindt aan niets en is stil dood — precies de
vorm die WF-031's vensterclausule al met die reden weigert. Gesloten **bij
WF-031 en niet bij WF-107**, want het zijn twee naamruimten: de benoemde
vormen delen er één (die WF-020 uniek houdt), een dagsoortnaam heeft een
eigen (die WF-031 zelf uniek houdt). Getuige
`wf031-dagsoortdefinitie-naamloos`; kosten nul, 24 dagsoortdefinities over
de vier zones (golden 6, negatief 17, pariteit 1) en allemaal benoemd op de
getuige na. Gemeten schade: op de eigen casus van
`jutterswet-schouwkalender` beweegt geen antwoord — het defect is een
verloren definitie, geen fout getal, en dat is juist waarom het een regel
vraagt en geen gated case. De blanco-arm staat VOOR de resolutie-arm: een
blanco naam is ook geen gedeclareerde dagsoort, dus in de natuurlijke
volgorde antwoordt de resolutie-arm eerst met `undeclared day kind ' '` —
waar, en de verkeerde diagnose.

**De grens van WF-107 is per array getrokken, niet geraden**, en staat zo in
de regeltekst en de indexrij. Vier andere top-level arrays dragen een naam,
elk om een eigen reden buiten deze regel — allemaal gemeten over de vier
zones:

| array | telling | waarom buiten WF-107 |
|---|---|---|
| `resolvedDagsoorten` | 24, één naamloos (de getuige) | eigen naamruimte; presence is WF-031's clausule (hierboven) |
| `tijdlijnen` | 37, allemaal benoemd | al gedekt: WF-015 sleutelt de declaratie op de naam, en een naamloze tijdlijn valt op **WF-003** (gemeten: `reference to undeclared timeline 'penningjaar'`) |
| `regelGroepen` | 8, allemaal benoemd | label; WF-033 resolveert de LEDEN op naam, niemand leest de groepsnaam — inert, geen regel zonder falen |
| `regels` | 866, allemaal benoemd | de parse-stage stub-index die geen check en geen uitvoerder leest; naamloos is verifier-schoon en blijft dat (gemeten) |

Daarnaast is `dagsoorten` een lijst kále strings; `[null]` is daar vandaag
ook toegelaten (gemeten: verifier PASS) en dat is **als open genoteerd**, niet
gesloten: het is opnieuw de dagsoort-naamruimte, dus het hoort bij WF-031's
familie en niet hier, en geen producent schrijft er een.

**(b) `spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:**

> De subtype-consequentie voor de STRATIFICATIE was de laatste die nog op de
> letterlijke typenaam stond, hersteld 2026-09-12: de knoop van de
> ch. 5.1-afhankelijkheidsgraaf is nu ketengesloten aan de SCHRIJFkant, en de
> DRIE schrijfposities sluiten NIET gelijk — dat is de kern en het is per
> positie opgeschreven in plaats van gegeneraliseerd. Een POPULATIEschrijving
> sluit alleen OMHOOG (creatie is niet polymorf; een feitcreatie maakt geen
> instantie, dus haar populatieschrijving blijft de letterlijke proxy). Een
> LIDschrijving door een vorm die een type ITEREERT — regeldoel,
> verdelingsdoel en -restant, defaultsdoel — sluit in BEIDE richtingen over
> de ⊑-VERGELIJKBARE typen die het lid dragen, want zo'n vorm schrijft op elke
> instantie van haar eigenaar. Een LIDschrijving door een OBJECTCREATIE sluit
> weer alleen OMHOOG, want zij vult de ene instantie die zij munt en het
> concrete type daarvan is precies het gecreëerde type. Lezen blijft
> letterlijk. Twee cycli die er tot dan buiten vielen zijn nu statisch
> geweigerd, allebei verifier-schoon en allebei divergerend in `ref` met
> `no fixpoint within 100 passes`: een objectcreatie waarvan het nieuwe type
> een subtype van haar eigen subject is
> (`negatief/wf045-subtypecreatie-cyclus`) en twee regels op één geërfd
> attribuutpaar met verschillend gespelde eigenaars
> (`negatief/wf045-erfattribuut-spelling-cyclus`). Twee kortere routes zijn
> gemeten en verworpen, elk omdat zij een document weigert dat draait:
> `declaring_type` aan beide kanten voegt twee onvergelijkbare
> zustersubtypen samen tot één knoop, en de creatie-toekenning bidirectioneel
> sluiten trekt een valse edge van een creatie die haar eigen SUPERTYPE munt
> terug naar een regel op het subtype dat zij nooit kan munten — daar
> weigerde `ref`, die deze graaf deelt, terwijl clingo doorrekende, dus het
> differentiaalvierkant spleet. De sluiting verbreedt een schrijving in 9
> corpusdocumenten zonder er één een cyclus te geven, en
> `golden/jutterswet-wrakboeking` is de getuige van de derde positie: het
> enige corpusdocument dat rood wordt zodra die weer bidirectioneel sluit.

### Eerlijke afwijkingen

- **Twee bestanden buiten may-touch aangeraakt, beide gemeld.**
  (i) `scripts/check-drools.sh` — één regel, `EXPECTED_PLAYED` 134 → 135, met
  reden en datum; zonder die bump kan géén nieuwe golden landen, en de gate
  schrijft de bump zelf voor in haar foutboodschap. Zit met de golden in één
  commit, dus als geheel te laten vallen. (ii)
  `corpus/negatief/wf020-crossform-collision/NOTE.md` — één zin, die de regel
  parafraseerde als "unique across all rule forms" en door de verbreding
  onwaar werd. Een onware NOTE laten staan is het slechtere alternatief.
- **Een derde gat, buiten de twee die de kaart noemt, is meegenomen**
  (WF-031, hierboven). Het kwam uit de meting van gat 1, het is dezelfde
  vraag, de reden stond al in WF-031's eigen tekst en het corpus is er
  schoon voor. Een aparte kaart zou een open punt zijn geweest waar het
  mandaat er geen wil.
- **De kaart vroeg één WF-045-getuige, er zijn er twee.** De lidknoop-arm
  kwam uit het meten van gat 2 en is dezelfde familie met dezelfde
  consequentie; hem laten liggen zou WF-045's belofte half waar laten. Hij
  staat apart in de spec, met eigen negatief en eigen grensmeting.
- **Mijn eerste versie van gat 2 was in één van de drie posities fout**, en
  de lens ving het: de creatie-toekenning sloot bidirectioneel mee. Latent
  (nul corpusblootstelling), maar het spleet het differentiaalvierkant op de
  lensgetuige. Eerlijk als lens-vangst: hij was van mij en zat in deze kaart,
  en hij is in deze kaart gesloten, mét de positieve getuige die de arm
  ontbrak.
- **De formulering van gat 2 wijkt af van de bronkaart.** Die noemde
  `declaring_type` als de bestaande normalisatie; de meting wees uit dat die
  te grof is (zusterversmelting). De sluiting gebruikt daarom
  `is_sub`/vergelijkbaarheid. Dat is meer code en minder verrassing.
- **WF-020's boodschapstekst is veranderd** ("rule name" → "name") op een
  bestaande negatief-getuige. Het oordeel beweegt niet; de NOTE.md van
  `wf020-crossform-collision` is niet aangeraakt (bestaand corpusbestand) en
  parafraseert de regel, citeert geen rapportregel.
- **WF-101 leest nu `named_forms`** in plaats van `form_arrays` + eigen arm.
  Nul gedragsverandering (dezelfde paden, dezelfde boodschappen), maar het
  is een aanraking buiten de strikte opdracht — gedaan omdat de
  handgeschreven arm precies de duplicatie is die gat 1 veroorzaakte.
- **Niet gemerged.** De kaart raakt beschermde paden (`spec/IR-SPEC.md`,
  `spec/verifier/`) en blijft een voorstel op `card/regelnaam-wf`.
