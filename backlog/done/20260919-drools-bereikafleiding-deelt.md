# drools-bereikafleiding-deelt: één afleiding, één weigeringszin

Rest van de som-over-bronrol-kaart (2026-09-19), door de lens gewogen:

1. **`gebondenRolpaar` staat in twee exemplaren** — regel voor regel
   identiek in `DrlTranspiler` en `ExecModelTranspiler`. De twee takken
   houden elkaars bereikafleiding nu alleen via de dual-branchgetuigen
   bij elkaar. Eén gedeelde afleiding hoort in `FactModelGenerator`
   (het patroon dat de weigeringszinnen al volgen via
   `geenJoinveldReden`).
2. **De weigering bij twee meervoudige rollen blijft generiek** in de
   DRL-tak (`TODO aggregatie SOM`) tegenover exec's benoemde arm
   (`aggregatie buiten Phase-C scope`) — per construct, maar niet in
   dezelfde woorden; het delen van de afleiding is de plek om ook de
   zin te delen.
3. **De temporele set-vouwen** (`tijdreeksSomAlleSet`,
   `temporeelSetAccumulate`) lezen de wire-binding nog niet; geen
   corpusgolden combineert ze met een herhaald paar en de vorm valt
   door naar een marker, niet naar een verkeerde verzameling —
   meenemen in dezelfde consolidatie.

## Acceptatie (bij oppakken aanscherpen)

- (a) De afleiding verhuist naar één plek; beide transpilers lezen
  haar; de dual-branchgetuigen blijven groen en de mutatieproef op de
  gedeelde plek raakt beide takken (het StaandeDoelen-patroon).
- (b) De meervoudige-rollenweigering draagt in beide takken dezelfde
  zin; getuige erbij.
- (c) De temporele set-vouwen lezen de binding of weigeren benoemd;
  meting eerst.
- (d) Corpussweep beide takken: nul kanaal- en redenwijzigingen
  (behalve waar (b)/(c) een diepere reden geven — bij naam verantwoord);
  alle veertien gates groen.

May touch: `backends/drools/**` (transpilers, `FactModelGenerator`,
tests), kaartverplaatsing. Geen scripts, corpus of frontend.

---

Result:

**(a) De afleiding staat op één plek.** `gebondenRolpaar` is
`FactModelGenerator.gebondenRolpaar(model, rb)` geworden — publiek,
statisch, met de samengevoegde documentatie van de twee privé-exemplaren
(de WF-047-arbeidsverdeling van de exec-kant plus de "resolutie gaat via
het feittype dat de wire NOEMT"-alinea van de DRL-kant). Beide privé-kopieën
zijn weg. Zeven aanroepplekken lezen nu die ene — drie rechtstreeks
(`ExecModelTranspiler.aggregatie` voor `elemRol`, exec's ch.-3.9a
kwantorvouw, `ExecModelTranspiler.objectCreatie`) en vier via de naad
`bereikjoin` hieronder (`DrlTranspiler.scope`,
`ExecModelTranspiler.aggregatie`, en `tijdreeksSomAlleSet` in béíde
takken).

**(b) Eén weigeringszin, en de meting die zegt waarom dat mocht.** De
kaart nam aan dat exec hier een benoemde arm had; dat is niet zo. Beide
takken waren generiek: exec viel terug op `aggregatie buiten Phase-C scope`
(de bodem als `aggregatieReden` leeg blijft) en de DRL-tak op de kale
`TODO aggregatie <KIND>`. Er was dus geen bestaande zin om naar te
convergeren.

Eerst gemeten wélke goldens de te wijzigen reden dragen, corpusbreed,
beide takken: **geen enkele**. De arm vraagt dat de geschreven naam én de
paarzoektocht falen én dat de binding oplost naar een meervoudige
zusterrol; `erflijn`, `gemeenteberging` en `penninguitgifte` weigeren
allemaal een laag verderop (bij `geenJoinveldReden` respectievelijk de
conservatieve exec-arm) en raken de bindingsarm nooit. Gate 12's
`EXPECTED_CHANNEL` beweegt daarom niet, `EXPECTED_PLAYED` evenmin, en
er is geen gedwongen scriptdiff — het voorstel dat de opdracht
reserveerde is niet nodig.

De gedeelde zin komt uit `FactModelGenerator.bereikjoin`, dat de hele
naad draagt (rol óf reden, nooit allebei):

```
aggregatie-bereik: de wire-binding wijst de rol 'ligger' aan, wiens
zusterrol 'thuishaven' MEERVOUDIG is — die naam zou het referentieveld op
Schuit zijn, maar de opslag van een meervoudige rol ligt aan de overzijde,
dus dat veld bestaat niet (§13.3.9 opslagrichting)
```

Woordelijk hetzelfde in beide takken (exec zet hem op `aggregatieReden`,
de DRL-tak op `navReden`); gepind door
`BronRolVouwWitnessTest.de_meervoudige_zusterrol_weigert_in_beide_takken_met_dezelfde_zin`
via `BeideTakken.geweigerd`, die het fragment van béíde takken eist.

**(c) De temporele set-vouw leest de binding nu; haar zustervorm kán het
niet.** Rood eerst, op een eigen document: `jutterswet-tempovouw` met een
MAAND-temporeel doel, vouwsoort `TOTAAL` → `SOM` (alleen SOM blijft
temporeel) en één eenheid aan beide kanten, plus een tweede feittype over
hetzelfde (Jutter, Vondst)-paar. Gemeten op de jar van vóór de wijziging:

| document | verifier | ref | clingo | exec | drl |
|---|---|---|---|---|---|
| uniek paar | PASS | FAULT | FAULT | PLAYED 1 | PLAYED 1 |
| herhaald paar, mét `bronRol` | PASS | FAULT | FAULT | SKIP | MARKER |
| herhaald paar, binding gestript | **WF-048 FAIL** | FAULT | FAULT | SKIP | MARKER |

De twee weigeringen luidden `regel (temporele toekenning buiten Phase-F
scope)` en `TODO temporele toekenning: de som van het bewaartarief van
alle vondsten van de Jutter` — en ze waren **woordelijk gelijk voor de
tweede en de derde rij**: de takken konden een document dat zijn bereik
NOEMT niet onderscheiden van een document dat dat niet doet, terwijl die
laatste per WF-048 niet eens welgevormd is. Ná de wijziging speelt de
tweede rij in beide takken (`PLAYED 1`, dezelfde handberekende tijdlijn
19 / 17 / 12) en blijft de derde in beide takken geweigerd.

Ref en clingo weigeren álle drie ("temporal writes are not implemented" /
"a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count
shape"), en dát is waarom geen corpusgolden deze vorm kan dragen: de
verwachting zou door gate 5 en 6 vallen. De tijdlijn staat daarom als
handberekening in de getuige, in de enige twee uitvoerders die de vorm
dragen.

De tweede vorm uit kaartpunt 3 — `temporeelSetAccumulate` (DRL) en haar
exec-tweeling `geheelPeriodeAlleVoorwaarde` — is **niet** aangepast, en
de reden is structureel, niet praktisch: die lezen een kale
`AttribuutRef` onder een `GeheelPeriode`, en het schema kent `bronRol`
alleen op `Functie` en `Kwantorvouw`. Er is aan die knoopsoorten dus
geen binding om te lezen; de paarzoektocht ÍS het hele aanbod van de
wire. Bovendien staat in beide takken al gemeten (2026-09-11) dat
WF-096 een set-lezing op een gewone leespositie weigert, zodat de vorm
door een welgevormd document niet bereikbaar is. Beide weigeren daar al
benoemd (`§8.2 gehele-periode over onbereikbare verzameling` /
`geen verbindende rol tussen …`); er is niets gedeeld te maken.

**Mutatieproeven, feitelijk gedraaid en teruggedraaid.** Beide op de
GEDEELDE plek, beide gemeten in béíde takken.

- *Mutatie a — `gebondenRolpaar` geeft `[zuster, gebonden]` terug.* Suite:
  **463 tests, 4 failures, 26 errors**. Direct op de wire gemeten, dus
  niet alleen op exec (de `BeideTakken`-rijen vragen exec eerst en stoppen
  daar):

  ```
  som-vouw            exec: SKIP  aggregatie-bereik: … rol 'thuishaven' … zusterrol 'ligger' MEERVOUDIG
                      drl : MARKER aggregatie-bereik: … rol 'thuishaven' … zusterrol 'ligger' MEERVOUDIG
  temporele set-vouw  exec: SKIP  aggregatie-bereik: … rol 'bewarende jutter' … zusterrol 'vondst' MEERVOUDIG
                      drl : MARKER aggregatie-bereik: … rol 'bewarende jutter' … zusterrol 'vondst' MEERVOUDIG
  ```

  Eén mutatie, vier rode uitkomsten over twee constructen en twee takken —
  het StaandeDoelen-patroon, letterlijk. `ObjectCreatieWitnessTest` valt
  mee om (21 rijen: 2 failures + 19 errors), wat de creatie-aanroepplek
  meeneemt.
- *Mutatie b — de kardinaliteitsweigering in `bereikjoin` weggehaald.*
  Suite: **463 tests, 1 failure**, precies de nieuwe (b)-rij. Beide takken
  verliezen de bindingsdiagnose en vallen terug op de laag eronder
  (`geenJoinveldReden`), in exec én in de DRL-tak. Dat die terugval zélf al
  gedeeld is, is de eerlijke nuance: de rij vangt het verlies van de
  bindingsdiagnose, niet een uiteenlopen van de takken.

Na beide mutaties was de werkboom weer schoon (`git status` leeg) en de
suite groen.

**(d) Corpussweep, beide takken, vóór en ná.** Alle 203 `ir.json` onder
`corpus/golden`, elk in exec en in DRL-tekst, 406 statusregels:

```
diff sweep-voor.txt sweep-na.txt  ->  leeg
exec-gespeeld 154 -> 154      DRL-tekst-gespeeld 127 -> 127
kanaalwisselingen: 0          redenwijzigingen: 0
```

Regel voor regel identiek. De diepere redenen die (b) en (c) toevoegen
zijn corpus-onbereikbaar — dat is gemeten, niet aangenomen — dus er is
geen enkele golden bij naam te verantwoorden.

**`main` bewoog tijdens de kaart.** De tak vertrok van `508bba31`;
`main` liep door naar `35f05a66` (de kruis-stelsel-kaart: een
clingo-wijziging, een nieuwe golden `jutterswet-stelselweging` en een
`EXPECTED_PLAYED` 153 → 154) en daarna naar `9cd7017d` (alleen een nieuw
kaartbestand). Er is geen bestandsoverlap, beide rebases liepen schoon,
en de hele meetronde is op `35f05a66` OVERGEDAAN: de
vóór-sweep is opnieuw gedraaid uit een aparte worktree op `35f05a66`, de
ná-sweep op de tak, en alle veertien gates opnieuw. De getallen hierboven
en hieronder zijn die van de tweede ronde. (Eerste ronde, op de oude
basis: 202 goldens, 404 regels, exec 153 → 153, DRL 126 → 126, óók
regel voor regel identiek.)

**Veertien gates, elk apart gedraaid** (`eval $(opam env)`; exitcodes):

```
self-contained: 0      rood: 0
ir-schema: 0           catala: 0
verifier: 0            regelspraak: 0
case-schema: 0         toka-parity: 0
oracle: 0              drools: 0
clingo: 0              reftype-derivation: 0
noop: 0                flogic: 0
```

Gate 12: `played 154`, kanaalvloer 49 niet-PLAYED goldens bij naam,
JUnit **463 tests, 0 skips** (was 447; vloer 239). Gate 13: 126
eenheidsannotaties over 23 goldens (vloer 74), 104 replays, 22
DRL-gespeeld (vloer 13) — beide taktellingen gelijk aan wat de basis
zonder deze wijziging meet. Gate 10: 298 tests, 57 rgs-goldens. Gate 11:
29 toka-cases, nul divergenties.

**Getuigen.** Eén nieuwe rij in `BronRolVouwWitnessTest` en een nieuwe
klasse `TemporeleSetvouwBereikWitnessTest` (3 rijen: het unieke
paar als regressiespiegel, het herhaalde paar als de rij zelf, en het
gestripte bereik als de wacht tegen verbreding-tot-gokken). Beide
speelrijen lopen via `BeideTakken`, dus elke rij eist hetzelfde van béíde
takken.

**Geen bestaande testcheck gewijzigd.** De bestaande rij
`een_meervoudige_zusterrol_levert_geen_scalair_joinveld` is met opzet
ongemoeid gelaten: zij pint dát beide takken weigeren, de nieuwe rij pint
dat ze het in dezelfde woorden doen. Dat kost vier regels fixture-dubbeling
en spaart een wijziging aan een staande check.

**May touch, geverifieerd** (`git diff --name-only main...HEAD`): zeven
paden, alle binnen de lijst — `FactModelGenerator.java`,
`DrlTranspiler.java`, `ExecModelTranspiler.java`, twee testbestanden onder
`backends/drools/src/test/`, en de kaartverplaatsing (twee paden, één
bestand). `scripts/`, corpus,
frontends en `spec/` zijn ongemoeid; `spec/CONSTRUCTS.md` en
`spec/PLAN.md` zijn niet aangeraakt.

**Registervoorstel** voor `spec/CONSTRUCTS.md` rij 4 (*Fact types and role
navigation*) — niet zelf geland:

> **Landed (2026-09-19):** the ch. 4.3 range derivation is now ONE
> function, `FactModelGenerator.gebondenRolpaar`, read by both Drools
> branches at every seam that resolves a fold's set — the instance fold,
> the §5.8.2 TEMPORAL set fold, the ch. 3.9a quantifier fold and the §13.4.6
> creation's back-link. The two branches carried it privately, line for
> line identical, from 2026-09-18 to 2026-09-19. A binding that resolves
> to a TO-MANY sibling now refuses in ONE sentence in both branches
> (`FactModelGenerator.bereikjoin`), where each branch used to fall back
> to its own generic scope note. NEW behaviour, with its red measurement:
> the temporal set fold (`de som van <temporeel attribuut> van alle …`
> into a temporal target) read the by-pair search alone, so on a REPEATED
> type pair it refused a document whose wire named the relation exactly —
> word for word the same refusal it gives a document with NO range, which
> WF-048 rejects outright. It now plays in both branches. Corpus-wide,
> both branches, byte-identical: exec 154 → 154, DRL-text 127 → 127, no
> channel and no reason changes; every new diagnosis is corpus-unreachable
> and lives in witnesses. The §8.2 whole-period SET forms are deliberately
> untouched: `bronRol` exists on `Functie` and `Kwantorvouw` only, so a
> bare `AttribuutRef` under a `GeheelPeriode` has no binding to read, and
> WF-096 refuses a set reading at an ordinary read position anyway.

**Open bevinding — een VIERDE bereikafleiding, buiten deze kaart.** De
ch.-3.9a kwantorvouw leidt haar bereik óók zelf af, en dáár loopt de
takgelijkheid nog steeds uiteen: `ExecModelTranspiler` leest
`kv.bronRol()` (en gebruikt sinds deze kaart de gedeelde
`gebondenRolpaar`), `DrlTranspiler.kwantorvouw` leest de binding
helemaal niet. Gemeten, op `jutterswet-keurploeg` met één extra feittype
over (Keurploeg, Keurmeester) — verder letter voor letter gelijk:

```
uniek paar     exec PLAYED 7   drl PLAYED 7
herhaald paar  exec PLAYED 7   drl EXCLUDE MARKER: regel: premiewaardig bij
                               keurvergoeding — TODO §8.2 kwantorvouw (ALLE over
                               Keurmeester) — geen verbindende rol tussen
                               Keurploeg en Keurmeester
```

Het corpus draagt 32 kwantorvouwknopen over negen goldens, alle met een
`bronRol` en alle over een uniek paar, dus de split is vandaag
corpus-onzichtbaar. Dit is dezelfde takongelijkheid die de
som-over-bronrol-kaart voor de instantievouw sloot — een ONTBREKENDE arm,
geen dubbeling, en daarmee een eigen kaart met eigen rood-eerst en eigen
getuigen. Bewust NIET hier gedaan: het is gedragswijziging op een
construct dat deze kaart niet noemt, en acceptatie (d) laat alleen
(b)/(c) als uitzondering toe. Exec's kwantorvouwnaad draagt trouwens nóg
een inline kopie van de meervoudige-zusterrolcheck
(`backref == null && paar != null && !paar[1].meervoudig()`), die in
dezelfde kaart naar `bereikjoin` hoort.

Voorstel voor `backlog/new/`: **`drl-kwantorvouw-bereikbinding`** — de
DRL-tak leest `kv.bronRol()` langs `FactModelGenerator.bereikjoin`, exec's
inline kardinaliteitscheck gaat naar diezelfde naad, met de meting
hierboven als rood, een dual-branchgetuige en een corpussweep.
