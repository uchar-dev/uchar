# De TOKA-stresstest: een productiecasus door de uchar-keten

Goal: de TOKA-casus van de regelspraak-java-toolchain (dáár, en
alleen daar) door de uchar-keten: de resolver dumpt de IR, de
uchar-verifier keurt, ref en clingo rekenen, en de uitkomsten worden
naast de toolchain-eigen (Drools-)verwachtingen gelegd — de eerste
directe toets van het indirecte Drools-been (de bewust open flank
uit Phase 2) én een echte-wereld-stresstest van de corpus-subset.
RECHTENREGEL, absoluut: al het TOKA-materiaal (bronnen, dumps,
cases, waarden) blijft in de toolchain en /tmp; in dit repo landt
uitsluitend deze rapportkaart met geaggregeerde technische
bevindingen — constructsoorten, aantallen, WF-/S-verwijzingen,
divergentieklassen — zonder één geciteerde bronregel of
dossierwaarde. De toolchain is strikt read-only (geen commits daar).

Acceptance: het rapport in deze kaart beantwoordt: (a) hoeveel van
de casus de verifier schoon passeert en waar hij weigert (per
WF/constructsoort); (b) waar ref en clingo dragen, weigeren of
faulten; (c) voor het uitvoerbare deel: stemmen ref- en
clingo-uitkomsten overeen met de Drools-verwachtingen (aantallen,
divergentieklassen); (d) de eerlijke conclusie over het directe
Drools-been; (e) de gaten-inventaris als kaartkandidaten. Gates
blijven groen (dit spoor wijzigt niets aan artefactzones); twee
opus-lenzen (reproductie + rapport-eerlijkheid) verwerkt.

May touch: backlog/ (deze kaart). Out of scope: elke wijziging aan
spec/, corpus/, ref/, backends/, frontends/, scripts/; elke
toolchain-commit.

---

# RAPPORT — de TOKA-stresstest (uitgevoerd 2026-08-25)

RECHTENREGEL nageleefd: alle TOKA-bronnen, IR-dumps en case-JSON's staan
uitsluitend in de toolchain-checkout en /tmp/toka; hieronder geen geciteerde
bronregel, geen dossierwaarde, geen casustekst — enkel constructsoorten,
aantallen, WF-verwijzingen en divergentieklassen. Fault-teksten die worden
aangehaald zijn uchar-eigen executoruitvoer, niet TOKA-materiaal. De ene
letterlijke reproductie ("5 €") is eigen-geschreven (geen TOKA). De toolchain
is uitsluitend aangeroepen/gelezen; niets daar gecommit.

## Methode

- IR-dump via de resolver (`--ir`): het volle model (reconciliatie-extensie +
  het 47-bestands fixtureset) MINUS de ene rekenregel die de toolchain zelf
  als enige bekende CasusTest-fout draagt (datum-tijd + datum-tijd, §4.1
  Tabel 2 verbiedt het; fail-fast blokkeert anders de hele dump). Plus
  per-scenario IR-slices in exact de scoping van het toolchain-eigen
  scenariotoetsen-manifest (objectmodel + precies de regel onder toets).
- De uchar-verifier draaide op elke dump.
- Voor het uitvoerbare deel zijn case-JSON's afgeleid uit de scenario-eigen
  `Met als verwacht resultaat`-blokken — dat zijn per constructie de
  Drools-verwachtingen (het manifest pint exit 0, d.w.z. Drools produceert
  ze). ref én clingo (runners uit deze worktree, na `dune build` vanaf de
  workspace-root) draaiden elke case; PASS betekent: executor-waarde ==
  case-verwachting == Drools-waarde.
- Scope: 30 uitvoerbare scenario's (manifest exit 0) plus de 2
  marker/fout-scenario's. Eerlijk overgeslagen: de kerstdag-dagsoortregel
  (geen scenario), de lege beslistabel (alleen een kop → per ontwerp niet
  gelowerd, resolver-WARNING), het wederkerige feittype (niet los
  scenario-getoetst) en de datum-tijd-fout-regel (resolve-fout; door de
  toolchain zelf als MARKER erkend).

## (a) Verifier-inventaris — waar hij schoon passeert en waar hij weigert

Volle-model dump (constructtelling): 3 objecttypen, 4 feittypen, 18
parameters, 3 eenheidstelsels, 2 domeinen, 1 tijdlijn, 1 dagsoort, 21
resolvedRegels, 6 verdelingen, 1 objectCreatie, 1 startpunt, 1 consistentie,
1 feitCreatie. Alle declaraties (objecttypen/feittypen/parameters/
eenheden/domeinen) en het merendeel (~17) van de resolved rules zijn
verifier-schoon. De weigeringen concentreren zich in 6 WF-klassen over 4
constructclusters:

- **WF-026 (15×)** — de zes allocatievarianten schrijven állemaal hetzelfde
  doel-attribuut zonder expliciete ladder; elk van de 15 paren
  (C(6,2)) wordt gemeld. Dit is een GENUINE strengheidsdivergentie, geen
  slicing-artefact: uchar eist een expliciete prioriteitsladder (ch. 7.7) bij
  meerdere allocaties op één doel, terwijl de toolchain de zes co-declareert
  en op condities/agenda vertrouwt (in de praktijk scenario-gesliced, nooit
  gelijktijdig actief).
- **WF-045 (1×) + WF-034 (deel)** — de objectcreatie zit in een
  afhankelijkheidscyclus (creatie ↔ recht-navigatie), en een
  fold-bron/lid lost niet op tegen een gedeclareerd attribuut/rol
  (unknown-type sentinel). Dit is de casus-omissie die de toolchain via haar
  eigen reconciliatie-extensie dicht.
- **WF-079 (1×) + WF-053 (2×)** — het startpunt/variabele-periode-cluster:
  twee onvoorwaardelijke rekenregels op één doel zonder ladder (WF-079) plus
  een navigatie die niet oplost (WF-053: rol niet bereikbaar vanuit het
  objecttype). Beide executors weigeren deze regels ook (zie b).
- **WF-034/WF-036 (1 rule)** — de datum-tijd-rekenregel draagt unknown-type
  sentinels en een ordening-operand van onbekend type.

Per-scenario slices (30): 28/30 melden **WF-016** (variable-start tijdlijn
zonder startpunt-bepaling). Dit is een SLICING-ARTEFACT — het gedeelde
objectmodel declareert de tijdlijn, maar de startpunt-regel valt in 28 van 30
slices buiten scope. De 2 startpunt-slices melden i.p.v. WF-016 juist WF-053.
Bevinding: de verifier is een WHOLE-MODEL well-formedness-poort; hij weigert
een model met een niet-verankerde variable-start tijdlijn óók als de getoetste
regel die tijdlijn niet raakt. Cruciaal: geen enkele WF-weigering blokkeerde
uitvoering — ref/clingo rekenden de gesliced regels gewoon door (18/30 exact
goed in ref), want WF-016 is orthogonaal aan de getoetste regel.

## (b) ref/clingo: dragen, weigeren, faulten

**ref** (30 scenario's): 18 PASS (exact = Drools), 12 EVALUATION FAULT, en —
de kernwaarneming — 0 stille misrekeningen. Fault-klassen:
- tijdsduur / datumverschil (leeftijd uit twee data): "non-empty tijdsduur is
  ch. 3 scope" — 2.
- instance-fold aggregatievorm (telling van gelinkte instanties):
  "unsupported instance-fold source shape" — 2.
- datum-tijd-rekenkunde (datetime + duur): "plus: expected a number, got
  @<ms>" — 1.
- datum-predicaat/functie (seizoen/feestdag): "function kind not implemented
  in this card" — 2.
- unit-dragende literal (getal met eenheid in het lexeme): "malformed number
  lexeme" — 2 (zie e/GOUD).
- navigatie-resolutie (casus-omissie / niet-oplossende rol): "no role named
  ... reachable" — 3.

**clingo** (30): 4 PASS, 26 TRANSLATION FAULT — altijd expliciet, nooit stil
groen. Weiger-klassen: allocatie/verdeling nog niet volledig vertaald (6),
decision-table / disjunctieve conditie-kind (3), unscalable datatype
(tekst/enum/datum-tijd) in de ASP-vertaling (4), som zonder 0-fallback (1),
validity-windows nog niet vertaald (1), diverse expression/condition kinds
(rest), unit-lexeme (1).

## (c) Vergelijking: ref vs Drools, clingo vs ref/Drools

- **ref vs Drools**: 18/30 exacte overeenstemming; 12/30 ref weigert
  observeerbaar (fault); 0 afwijkende waarde.
- **clingo vs Drools**: 4/30 overeenstemming; 26/30 clingo weigert
  observeerbaar. clingo's dekking is smal over deze real-world casus.
- **clingo vs ref**: waar beide dragen én overeenkomen: 3 scenario's
  (ref ≡ clingo ≡ Drools), waaronder minstens één met een harde numerieke
  waarde (niet slechts een consistentie-assertie). 1 asymmetrie: bij de
  objectcreatie-regel rapporteert clingo 'consistent' (zwakke assertie)
  terwijl ref de navigatie probeert en fault — coverage-asymmetrie, geen
  waarde-divergentie.
- **Echte (stille) waarde-divergenties: 0.** Elke niet-overeenstemming is een
  luide fault/refusal in minstens één executor. Er is dus geen enkel geval
  gevonden waar de uchar-keten een ánder getal dan Drools stil produceert.

## (d) Eerlijke conclusie — het directe Drools-been

De eerste directe toets van het (tot nu toe indirecte) Drools-been is
bemoedigend én scherp. Waar de uchar-keten de casus wél uitvoert, komt hij
EXACT overeen met de Drools-verwachting — geen enkele stille misrekening over
30 scenario's, over aggregatie (som/max), gelijkstelling-rekenkunde, álle zes
allocatievarianten (met afronding, rest, maximum, rangorde), kenmerktoekenning
en band-kenmerk, consistentie/rondvlucht, en decision-table-tekstmatch. Het
directe been is dus, voor het uitgevoerde deel, groen: ref ≡ Drools. De
bindende beperking is DEKKING, niet correctheid: ref weigert nog 12/30
(datum/tijdsduur, datetime-rekenkunde, tel-fold, datum-functies, unit-literals,
casus-omissie-navigatie) en clingo is met 4/30 nog ruw als differentieel been
over deze casus. De stresstest bevestigt de bewust-open flank uit Phase 2: het
Drools-been is nu direct getoetst en klopt waar het draait; de corpus-subset
dekt de casus nog niet — de gaten zijn feature-dekking, geen misrekening.

Methodologische eerlijkheid (tweede opus-lens): (1) de case-invoer is mijn
eigen afleiding uit de scenario-verwachtingen; PASS is pas bewijs omdat de
verwachting = de Drools-assertie (manifest exit 0). (2) v0 reciproceert een
niet-wederkerig feittype NIET automatisch; ik moest de rollink in de case op
beide eindpunten zetten voordat ref door een fact-relatie kon navigeren — dat
is zelf een ergonomie-bevinding (zie e, punt 12), geen verhulling. (3) De
enige clingo-only-PASS staat tegen een zwakke consistentie-assertie; niet als
volwaardige overeenstemming geteld. (4) Bewust overgeslagen constructen zijn
hierboven benoemd.

## (e) Gaten-inventaris — kaartkandidaten

1. **[GOUD — TOKA-vrij geminimaliseerd] Unit-dragende numerieke literal.**
   De v0-IR-emitter schrijft de eenheid IN het literal-lexeme (bv. "5 €");
   de verifier keurt dat SCHOON (WF-clean), maar ZOWEL ref ALS clingo faulten
   in hun `q_of_lexeme` ("malformed number lexeme"). Eigen-geschreven
   reproductie (geen TOKA): objecttype met een €-attribuut en één regel die
   met een unit-literal rekent → IR-literal `waarde:"5 €"` → verifier PASS,
   ref FAULT, clingo FAULT. Gate-blinde vlek: geen corpus-case oefent een
   unit-literal. Kandidaat-fix (spec-beslissing): óf de emitter strippt de
   eenheid uit het lexeme (naar `type.eenheid`, zoals bij attributen), óf
   beide literal-parsers strippen een trailing unit-token, óf een nieuwe WF
   weigert unit-in-lexeme. Dit is de enige echte divergentie-in-uitkomst
   (Drools rekent ermee; beide uchar-executors weigeren) én ze is
   TOKA-onafhankelijk reproduceerbaar.
2. **ref: tijdsduur/datumverschil** (ch. 3 scope) — leeftijd/duur uit twee
   data — 2 scenario's.
3. **ref: datum-tijd-rekenkunde** (datetime + duur) — 1.
4. **ref: datum-predicaat/functie** (dagsoort/seizoen/feestdag) — 2.
5. **ref: instance-fold telling** (aantal gelinkte instanties) — 2.
6. **clingo: verdeling/allocatie volledig vertalen** — 6.
7. **clingo: decision-table / disjunctieve conditie** — 3.
8. **clingo: niet-numerieke datatypes** (tekst/enum/datum-tijd) in ASP — 4.
9. **clingo: validity-windows en som-zonder-0-fallback** — 2.
10. **WF-026-strengheid vs Drools**: de expliciete-ladder-eis bij meerdere
    allocaties op één doel is spec-strenger dan de toolchain. Spec-beslissing:
    mist de casus een ladder, of moet de eis versoepeld/anders geplaatst?
11. **WF-016 whole-model vs partiële scope**: scoping-beleid — moet de
    verifier een niet-verankerde variable-start tijdlijn weigeren wanneer geen
    regel die tijdlijn raakt?
12. **Case-ergonomie / tekst-fideliteit**: (a) v0 reciproceert een
    niet-wederkerig feittype niet automatisch — een case moet de rollink op
    het lees-eindpunt leveren; overweeg reciprocale materialisatie of een
    duidelijke WF/doc. (b) v0 draagt bronquotes in het Tekst-literal-lexeme;
    ref vergelijkt verbatim, dus een case moet quotes meesturen om te matchen
    (Drools strippt ze) — divergentie-kandidaat op de tekst-laag.

Status: rapport compleet (a–e). Twee opus-lenzen verwerkt: reproductie (de
eigen-geschreven "5 €"-minimalisatie) en rapport-eerlijkheid (de
methodologische kanttekeningen onder d). Geen artefactzone aangeraakt; enkel
deze kaart gewijzigd.

Result (landing): de eerste productiecasus door de uchar-keten, en
het directe Drools-been voor het eerst gemeten. Kern over 30
uitvoerbare scenario's: NUL stille waarde-divergenties — elke
niet-overeenstemming is een luide fault of weigering; ref stemt met
Drools overeen op 18/30 (de rest zijn expliciete evaluatiefouten op
nog niet gedragen constructen), clingo op 4/30 (de rest expliciete
vertaalweigeringen), en waar beide dragen zijn ref, clingo en Drools
het exact eens, inclusief harde getallen. De verifier weigert het
volle model op zes WF-klassen over vier constructclusters — dekking
is de bindende beperking, niet correctheid. De gouden vondst,
TOKA-vrij gereproduceerd: de v0-emitter schrijft eenheden ín het
getal-lexeme ("5 €"), wat verifier-clean is maar beide
uchar-executors laat weigeren waar Drools rekent — een gate-blinde
vlek met drie besluitopties in de gaten-inventaris. De rechtenregel
is nageleefd en door de lens woord-voor-woord geverifieerd: geen
letter casus-materiaal in dit repo; de toolchain onaangeraakt. De
twee lensrondes (na de eerdere zelfcertificerings-notitie alsnog
onafhankelijk gedraaid) reproduceerden álle kwantitatieve claims
exact en vonden nul defecten. De gaten-inventaris onder (e) staat
klaar als kaartkandidaten; de spec-beslissingen (unit-literal-route,
WF-026-strengheid, WF-016-scoping, clingo-dekking voor
real-world-constructen) zijn van de maintainer.
