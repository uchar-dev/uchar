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
niemand gebruikte. Bij landing staat de teller op 624 documenten en 1104
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

- **populatie**: alleen omhoog. Een creatie van T laat T's populatie en die
  van elke voorouder groeien; niet die van een afstammeling, want creatie is
  niet polymorf. Een feitcreatie maakt geen instantie — haar
  populatieschrijving is ch. 6.1's proxy — en blijft letterlijk.
- **lid**: beide richtingen, over de ⊑-**vergelijkbare** typen die het lid
  dragen. Een schrijving op eigenaar T raakt elke T-instantie, en de
  instantieverzamelingen van T en S overlappen precies wanneer ze
  vergelijkbaar zijn.

Lezen blijft letterlijk: elke geldige leesspelling wordt vanaf de
schrijfkant bereikt, dus beide sluiten zou alleen edges verzinnen.

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

**Kosten: geen, en niet leeg.** De sluiting verbreedt een schrijving in **9**
corpusdocumenten (`strandrechtketen`, `wrakrecht`, `wrakrecht-flogic`,
`wrakbasis`, `wrakerfbasis`, `wrakconflict`, `bergingsdeling`,
`hoofdberging`, `zustervondst`) en geen ervan krijgt een cyclus.
`zustervondst` is de zusterstructuur zelf: een schrijving op de ouder bereikt
twee onvergelijkbare kinderen en die bereiken elkaar nog steeds niet.

**Getuigen.** `wf045-subtypecreatie-cyclus` (populatie-arm) en
`wf045-erfattribuut-spelling-cyclus` (lid-arm), naast de bestaande
`wf045-cyclic-attribute-dependency`. De drie vallen onafhankelijk: een graaf
op de geschreven naam vangt alleen de eerste.

### De hele voor/na-veeg

Over **624 documenten** in vier zones bewegen **8** rapportregels en geen
enkele andere: de zeven nieuwe getuigen, plus `wf020-crossform-collision`
waarvan het *oordeel* gelijk blijft en alleen de boodschap herformuleert
("rule name" → "name", "all rule forms" → "all named forms"). Gate 3 telt
209 golden schoon (ongewijzigd) en 385 negatief (378 → 385).

### Veertien gates groen — slotregels

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 209 file(s) validate and round-trip, floor 192)
check-verifier: PASS (209 golden clean; 385 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 492 case file(s) validate)
check-oracle: PASS (ref suite 592 worked examples, floor 489; 466 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 125 checks, floor 83; 466 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (430 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (245 tests, floor 173); 45 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS   (ref 29 / clingo 29 / Drools 29 PLAYED)
check-drools: PASS   (played 134, channel floor 49 pinned by name, JUnit 335 tests floor 239)
check-reftype-derivation: PASS (93 unit annotations twisted over 18 goldens, floor 74; 84 executor replays reproduce the untouched outcome, 15 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
check-flogic: PASS — frontend suite green (81 checks, floor 81); 1 flogic golden(s) re-emit byte-identically (floor 1)
```

Gate 12 en 13 staan stil op hun uitgangswaarden (played 134; 93/84/15) — er
is geen uitvoerder aangeraakt.

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
> zodat één botsing niet twee nummers kan citeren. Kosten nul: 1104 benoemde
> vormen over 624 documenten droegen al een naam.

**(b) `spec/CONSTRUCTS.md` rij 39, aan de bestaande cel toe te voegen:**

> De subtype-consequentie voor de STRATIFICATIE was de laatste die nog op de
> letterlijke typenaam stond, hersteld 2026-09-12: de knoop van de
> ch. 5.1-afhankelijkheidsgraaf is nu ketengesloten aan de SCHRIJFkant, en
> de twee knoopsoorten sluiten verschillend — een populatieschrijving alleen
> OMHOOG (creatie is niet polymorf, en een feitcreatie maakt geen instantie,
> dus haar populatieschrijving blijft de letterlijke proxy), een
> lidschrijving over de ⊑-VERGELIJKBARE typen die het lid dragen, in beide
> richtingen; lezen blijft letterlijk. Twee cycli die er tot dan buiten
> vielen zijn nu statisch geweigerd, allebei verifier-schoon en allebei
> divergerend in `ref` met `no fixpoint within 100 passes`: een objectcreatie
> waarvan het nieuwe type een subtype van haar eigen subject is
> (`negatief/wf045-subtypecreatie-cyclus`) en twee regels op één geërfd
> attribuutpaar met verschillend gespelde eigenaars
> (`negatief/wf045-erfattribuut-spelling-cyclus`). Bewust NIET de kortere
> route van `declaring_type` aan beide kanten: die voegt twee
> onvergelijkbare zustersubtypen samen tot één knoop en weigert een document
> dat `ref` schoon uitrekent (gemeten dezelfde dag). De sluiting verbreedt
> een schrijving in 9 corpusdocumenten zonder er één een cyclus te geven.

### Eerlijke afwijkingen

- **De kaart vroeg één WF-045-getuige, er zijn er twee.** De lidknoop-arm
  kwam uit het meten van gat 2 en is dezelfde familie met dezelfde
  consequentie; hem laten liggen zou WF-045's belofte half waar laten. Hij
  staat apart in de spec, met eigen negatief en eigen grensmeting.
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
