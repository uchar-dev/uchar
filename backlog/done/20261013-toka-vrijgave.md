# toka-vrijgave: de besluiten van 2026-10-13 over TOKA en de publieke repositories

*Kaart van de hoofdsessie, 2026-10-13. Grondslag: het besluit van de eigenaar van 2026-10-13 ("Herkomst en status")
en de technische gevolgen die de hoofdsessie daaruit trok. De inventaris en het bronnenonderzoek van dezelfde dag staan
buiten de repository; de labels hieronder (C1-b, C2-a, C7-a enz.) komen uit die inventaris en staan hier met hun
inhoud.*

## Besluiten

Van de eigenaar (2026-10-13):

1. **Herkomst en status.** Weg gaan de zinnen over privé blijven zolang de zone bestaat, over nooit citeren, over
   "not redistributed", en over de publieke flip die zoneverwijdering en een history rewrite vereist. Voor TOKA komt
   overal één vaste tekst (bron, rechtenvermelding van de bron, status van de vrijgave, licenties), inhoudelijk gelijk
   in `toka/README.md`, `README.md` en `CLAUDE.md`, met per repository de lijst van letterlijk overgenomen en van
   afgeleide of eigen bestanden.
2. **Publiek.** De repositories uchar en regelspraak-toolchain zijn publiek sinds 2026-10-12. Er komt geen history
   rewrite: de zone en haar geschiedenis blijven.

Van de hoofdsessie (technische gevolgen):

3. **C1-b.** De TOKA-tripwire van gate 1 houdt hetzelfde gedrag. De reden wordt de herkomst: het corpus is eigen werk,
   en TOKA is materiaal van derden waarvan de vrijgave nog niet schriftelijk bevestigd is.
4. **C2-a.** Gate 11 wordt een gewone, blijvende gate. "TEMPORARY" en de uitzondering op monotone groei verdwijnen.
5. **C7-a.** `uchar-dev/regelspraak-toolchain` gaat uit het PRIVATE-patroon van gate 1; de uitslag van gate 1
   verandert daardoor niet.
6. De regel "geen RegelSpraak-specificatiecitaten" blijft. De reden mag zeggen dat de specificatie "Alle rechten
   voorbehouden" draagt; de ©-taal over TOKA verdwijnt.
7. Het gedrag van geen enkele gate verandert, behalve C7-a: alleen commentaar, meldingen en tekst.

Goal: de levende teksten, scripts en commentaren van uchar volgen besluiten 1 tot en met 7. De vaste TOKA-tekst staat
gelijk op de drie plekken; de beschermde paden dragen de nieuwe redenen (C1-b), gate 11 als gewone gate (C2-a) en het
PRIVATE-patroon zonder de toolchain (C7-a); de voorgestelde tekst voor `spec/PLAN.md` en `spec/CONSTRUCTS.md` staat in
het Result, voor de hoofdsessie.

Acceptance:

- alle 16 gates van `CLAUDE.md` groen, lokaal gedraaid op de laatste inhoudelijke commit van de branch;
- de vaste TOKA-tekst is gelijk op de drie plekken: mechanisch vergeleken na het weghalen van het citaatteken (`> `)
  en de inspringing;
- een grep op de levende bestanden (zonder `backlog/done/`, `spec/proposals/` en de originele tests in `toka/src/`) naar
  `stay private|must stay private|public flip|history rewrite|filter-repo|not redistributed|TEMPORARY` geeft alleen
  bedoelde resten, elk met reden in het Result;
- C7-a: het oude en het nieuwe PRIVATE-patroon geven in de artefactzones van gate 1 hetzelfde aantal treffers, en gate 1
  geeft vóór en na de wijziging PASS.

May touch: `CLAUDE.md` (corpusregels, gate 1, gate 11, het mechanisme bij Protected paths, de repo-relaties),
`WAY-OF-WORKING.md` (§5–6), `scripts/check-self-contained.sh` (commentaar, meldingen, het PRIVATE-patroon),
`scripts/check-toka-parity.sh` (kop en commentaar), `scripts/admit.sh` (de melding), `.github/CODEOWNERS` (de kop),
`.github/workflows/gates.yml` (de job van gate 11), `README.md`, `corpus/README.md`, `toka/README.md`,
`toka/cases/parity.manifest` (alleen commentaar) en deze kaart. De eerste zeven zijn beschermde paden: de branch blijft
een voorstel en de hoofdsessie merget.

Out of scope: `spec/PLAN.md` en `spec/CONSTRUCTS.md` (voorgestelde tekst in het Result; de hoofdsessie landt die);
branch protection (C4); de licentie van `backlog/` en van de documenten in de wortel (L2/L3); het schema-`$id` onder
uchar.dev (D); privéverwijzingen in gearchiveerd proza (P); de website; de toolchain; elke gedragswijziging van een gate
buiten C7-a.

## Result (2026-10-13)

### Wat de kaart opleverde

- **Beschermde paden** (`3685cca3`; voorstel, de hoofdsessie merget):
  - `CLAUDE.md`:
    - het mechanisme bij Protected paths: de repository is publiek sinds 2026-10-12, branch protection staat niet aan
      (2026-10-13), dus het mandaat-en-reviewprotocol van `WAY-OF-WORKING.md` draagt de review;
    - de corpusregels: de regel "geen RegelSpraak-specificatiecitaten" blijft, met als reden dat de specificatie "Alle
      rechten voorbehouden" draagt. De ene uitzondering is de TOKA-casus (Bijlage 1 van de specificatie) in de zone. Die
      uitzondering is een eigen keuze: zonder haar spreekt de regel de zone tegen. Ter controle;
    - de zone-alinea zonder privé, flip en history rewrite, met de herkomst als reden van de tripwire en met de vaste
      TOKA-tekst;
    - gate 1: de reden van de tripwire, "temporary" weg;
    - gate 11: "TEMPORARY" en de uitzondering op monotone groei weg, met "a regular gate of the set since 2026-10-13";
    - de repo-relaties: de toolchain is publiek sinds 2026-10-12.
  - `WAY-OF-WORKING.md`: §5 noemt de opvolger publiek; in §6 heet de regel "TOKA tripwire", met de reden.
  - `scripts/check-self-contained.sh`: C7-a (het patroon, met een regel commentaar over het waarom), het commentaar van
    de tripwire en de FAIL-melding "(corpus provenance, corpus/README.md; the sole exception is the toka/ parity
    zone)".
  - `scripts/check-toka-parity.sh`:
    - de kop zonder TEMPORARY en zonder de uitzondering op monotone groei;
    - de ©-taal weg;
    - de zin dat de vloeren met de gate de set verlaten weg;
    - "the public corpus" werd "the corpus" (r. 33 en 40). Het bijvoeglijk naamwoord zette het corpus af tegen een
      zone die niet publiek zou worden.
  - `scripts/admit.sh`: de melding "(corpus provenance, corpus/README.md)".
  - `.github/CODEOWNERS`: de kop zegt dat branch protection niet aanstaat en dat het protocol de review draagt.
  - `.github/workflows/gates.yml`: commentaar en naam van de job van gate 11 zonder "temporary".
- **Niet beschermd** (`140c1033`):
  - `README.md`: de rij `toka/`, de status, de eerste afnemer (publiek), en de licentiesectie met "Not covered" en de
    vaste tekst;
  - `corpus/README.md`: twee regels, één over specificatiecitaten (reden: "Alle rechten voorbehouden") en één over
    TOKA (reden: herkomst);
  - `toka/README.md`: de titel, de inleiding, een nieuwe sectie "Provenance and status" met de vaste tekst, de zin over
    "not redistributed" weg, "Rights and lifecycle" werd "Lifecycle";
  - `toka/cases/parity.manifest`: alleen commentaar. Gate 11 leest het manifest zoals voorheen (29 gevallen).
- Het gedrag van geen enkele gate veranderde, behalve door C7-a; de uitslag van gate 1 bleef daarbij gelijk (zie
  Acceptatie).

### Commits

`453312f5` kaart · `3685cca3` beschermde paden · `140c1033` README's en manifest · de commit van dit Result.

### Acceptatie

**Gates.** Alle 16 groen op `140c1033`, lokaal gedraaid op 2026-10-13 van 09:55 tot 10:09 (`gates.sh`, verse
uitvoermap). De commit van dit Result raakt alleen deze kaart; gate 1 is daarop opnieuw gedraaid: PASS.

| Gate | Uitslag |
|---|---|
| 1 self-contained | PASS |
| 2 ir-schema | PASS: 342 bestanden valideren en round-trippen (vloer 192); 44712 objecten in 847 documenten in de sleutelvolgorde |
| 3 verifier | PASS: 342 golden schoon; 505 negatief op hun regel afgewezen (vloer 350), 348 op hun arm (vloer 260); 611 pointers (vloer 547) |
| 4 case-schema | PASS: 857 casebestanden |
| 5 oracle | PASS: ref-suite 1286 (vloer 489); 808 cases (vloer 427) |
| 6 clingo | PASS: suite 2198 (vloer 83); 808 cases, ref = clingo |
| 7 noop | PASS: 764 cases; 44 survivors |
| 8 rood | PASS: 49 rode cases (vloer 26); 98 fragmenten, geen bodemzin |
| 9 catala | PASS: 241 checks (vloer 201); 12 goldens (vloer 12) |
| 10 regelspraak | PASS: 1165 tests (vloer 173); 125 rgs-goldens (vloer 80) |
| 11 toka-parity | PASS: 45 fragmenten byte-identiek (vloer 45); 27 tests (vloer 27); 29 slices schema en round-trip; 28 verifier-schoon (vloer 28; holdout toka-24 WF-099); 29 cases (vloer 29): ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED |
| 12 drools | PASS: played 250 (pin 250); DRL-teksttak 197 (pin 197); JUnit 1443 (vloer 239) |
| 13 reftype-derivation | PASS: 43 twists over 24 goldens (vloer 38); 122 replays, 22 PLAYED in de DRL-teksttak (vloer 20) |
| 14 flogic | PASS: 150 checks (vloer 81); 2 goldens (vloer 1) |
| 15 graaf | PASS: 261 van 298 goldens vergeleken, 207 afhankelijkheden (vloeren 234 en 116); routebeen 125 goldens (vloer 98); 68 unittests (vloer 23) |
| 16 scenario | PASS: 30 goldens (vloer 7), 102 casebestanden (vloer 31) |

**De vaste TOKA-tekst is gelijk op de drie plekken.**

- Vergelijking: de blockquote-regels van `CLAUDE.md`, `README.md` en `toka/README.md`, na het weghalen van `> ` en de
  inspringing. Uitkomst: drie keer 23 regels, sha256
  `1d03660aa8e419dcb52102ce21cba7ddccd5d310ede55784fc96d3f842ae30d0`, `diff` leeg.
- De eerste alinea is, na het samenvoegen van witruimte, byte-gelijk aan de vaste tekst van het besluit (817 bytes; de
  tekst is buiten de repository aangeleverd).
- De tweede alinea is de bestandslijst van uchar:
  - letterlijk overgenomen: 47 fragmenten;
  - afgeleid of eigen werk: de dump, 29 slices, 29 casebestanden, het manifest, 2 tests en de README.
- Bij het opstellen gemeten, en daarom in de lijst opgenomen:
  - de dump en 22 van de 29 slices dragen tekst van de fragmenten in `source.snippet`;
  - `ModelTest` citeert regels van de fragmenten letterlijk.

**Grep** op `140c1033`:
`git grep -nE 'stay private|must stay private|public flip|history rewrite|filter-repo|not redistributed|TEMPORARY' -- . ':(exclude)backlog/done' ':(exclude)spec/proposals' ':(exclude)toka/src'`

```
backlog/doing/20261013-toka-vrijgave.md:13:   "not redistributed", en over de publieke flip die zoneverwijdering en een history rewrite vereist. Voor TOKA komt
backlog/doing/20261013-toka-vrijgave.md:24:4. **C2-a.** Gate 11 wordt een gewone, blijvende gate. "TEMPORARY" en de uitzondering op monotone groei verdwijnen.
backlog/doing/20261013-toka-vrijgave.md:42:  `stay private|must stay private|public flip|history rewrite|filter-repo|not redistributed|TEMPORARY` geeft alleen
spec/PLAN.md:359:public flip is now unblocked and awaits the maintainer.
spec/PLAN.md:713:and rewriting history (filter-repo or a fresh public copy) — recorded in
spec/PLAN.md:733:and P11 (zone removal plus history rewrite) executes at the public flip.
spec/PLAN.md:788:new toolchain under uchar; the `toka/` zone stays until the public flip.
spec/RUNTIME-SEMANTICS.md:3187:  The capped surplus is **not redistributed and not iterated** — it falls
```

Resten, met reden:

- `backlog/doing/20261013-toka-vrijgave.md`: deze kaart citeert de besluiten en het patroon. Na de commit van dit
  Result komen de regels van dit Result erbij, om dezelfde reden.
- `spec/PLAN.md:359`, `:713`, `:733` en `:788`: gedateerde verslagregels. Het zijn:
  - de Met-markering van fase 1 (2026-08-19);
  - de voorwaarde van 2026-08-27 bij §9 (10);
  - P11 in §9 (11);
  - de L5-landing (2026-09-25).

  Op hun datum waren ze waar. PLAN is van de hoofdsessie. De voorgestelde aanvulling bij (10) hieronder trekt de
  voorwaarde in en laat P11 vervallen.
- `spec/RUNTIME-SEMANTICS.md:3187`: "not redistributed" gaat over het afgetopte overschot van een verdeling; geen
  verband met TOKA.

Dezelfde grep zonder hoofdlettergevoeligheid vindt daarnaast:

- "temporary" in een andere betekenis: tijdelijke bestanden, probes en hekken;
- acht regels in artefactzones die de zone tijdelijk noemen. Die staan bij de open punten (punt 6).

**C7-a.** Het oude en het nieuwe PRIVATE-patroon geven in alle tien zones van gate 1 elk 0 treffers, op `fcb225db` en
op `140c1033`. `uchar-dev/regelspraak-toolchain` komt in geen zone voor. Gate 1 gaf PASS op `fcb225db`, op `140c1033`
en op de commit van dit Result.

### Voorgestelde tekst voor de hoofdsessie

**`spec/PLAN.md` §4 (r. 156–161).** De alinea "The published corpus is rights-free by construction." wordt:

> **The published corpus is rights-free by construction.** The corpus ships with the spec and inherits its
> obligations. Own-authored casus material (the Jutterswet) and statute text (copyright-free under art. 11
> Auteurswet) only; no RegelSpraak specification excerpts enter the uchar repos (the specification carries "Alle
> rechten voorbehouden"), with one exception: the TOKA casus, which is not corpus material and lives in the `toka/`
> parity zone, outside `corpus/` (§9 (10); `CLAUDE.md`, Corpus rules). A test case that cannot be published is not a
> test case for a public spec.

**`spec/PLAN.md` fase 1 (r. 337).** "this repo is TOKA-free from its first commit." wordt:

> the corpus is TOKA-free from its first commit, and the TOKA casus has its own parity zone, `toka/`, since
> 2026-08-27 (§9 (10)).

**`spec/PLAN.md` §8 (r. 643–646).** "enforced by CODEOWNERS plus branch protection once the repository is public;
until then the mandate-and-review protocol of `WAY-OF-WORKING.md` carries it, and no separate CI check exists
(recorded 2026-09-10)." wordt:

> the mandate-and-review protocol of `WAY-OF-WORKING.md` carries it, and no separate CI check exists (recorded
> 2026-09-10). The repository is public since 2026-10-12, so the free org plan allows branch protection; it is not
> enabled (2026-10-13), so GitHub does not enforce the CODEOWNERS review, and whether to enable it is open.

**`spec/PLAN.md` §9 (8)** (r. 698–699), aan te vullen na "no ©-Belastingdienst material enters the uchar repos.":

> **Amended (2026-10-13):** the clause no longer covers the TOKA casus, Bijlage 1 of the RegelSpraak specification:
> it stays outside the corpus, in the `toka/` parity zone (see (10)). Other specification excerpts still do not
> enter the uchar repos; the specification carries "Alle rechten voorbehouden".

**`spec/PLAN.md` §9 (10)** (r. 703–714), aan te vullen na "recorded in `CLAUDE.md` with the zone's charter.":

> **Taken (2026-10-12):** `uchar` and `regelspraak-toolchain` are public. **Amended (2026-10-13, maintainer):** the
> precondition of 2026-08-27 is withdrawn. The `toka/` zone stays, its git history is not rewritten, and P11 of
> decision (11) lapses; gate 11 is a regular gate of the set, so the set has no documented exception to monotone
> growth any more. The provenance and status of the TOKA casus stand in one fixed text in `toka/README.md`,
> `README.md` and `CLAUDE.md`: the Belastingdienst indicated to the maintainer on 2026-10-13 that TOKA is meant to be
> releasable; written confirmation is not yet received. Branch protection is available on the public repository and
> not enabled (2026-10-13); whether to enable it is open.

**`spec/CONSTRUCTS.md` r. 63 (rij 27).** "— waarbij gate 11 tijdelijk is en de set met de toka-zone verlaat, waarna de
gate-10-emittergetuige de blijvende machinale borging is." wordt:

> ; beide zijn blijvende machinale borging (gate 11 is sinds 2026-10-13 een gewone gate van de set).

De zin luidt dan: "… machinaal geborgd door de emittergetuige (…, gate 10) en door gate 11's byte-identieke
her-emissie; beide zijn blijvende machinale borging (gate 11 is sinds 2026-10-13 een gewone gate van de set)."

### Open punten

1. **C4, branch protection.** Staat niet aan (2026-10-13). `CLAUDE.md`, `.github/CODEOWNERS` en de PLAN-voorstellen
   bij §8 en (10) stellen dat vast zonder te kiezen. Na een besluit volgen die plekken en de website.
2. **L2/L3, licenties.** De licentiesectie van `README.md` noemt `backlog/`, `README.md`, `CLAUDE.md`,
   `WAY-OF-WORKING.md`, `.github/` en `dune-workspace` niet. Voor `toka/` zegt zij nu dat geen van beide licenties
   geldt, met een verwijzing naar `toka/README.md`.
3. **D, schema-`$id`.** De `$id` onder `https://uchar.dev/schema/` geeft 404 (inventaris, 2026-10-13). PLAN §5
   (r. 232–234) zegt dat uchar.dev `spec/` gaat serveren.
4. **P, privéverwijzingen in gearchiveerd proza.** Het gaat om `backlog/done/` en `spec/proposals/`: `/Users/`-paden
   in 15 kaarten, en `nluwv` of `regelspraak-werkbank` in PLAN, voorstellen en kaarten. Zonder history rewrite blijven
   ze ook in de geschiedenis. Niet aangeraakt.
5. **De website.** Het gaat om de statustekst ("nog privé"), de voetteksten, gate 11 als tijdelijk en `FORBIDDEN` in
   `_research/check.py`. Die staan niet in deze repository.
6. **De zone als "temporary" in artefactzones.** Nieuw gevonden; de inventaris noemt deze plekken niet. Acht regels
   noemen de zone tijdelijk. De grep van de acceptatie vindt ze niet, omdat zij hoofdlettergevoelig is. De tripwire
   vindt ze ook niet, omdat het woord "toka" er niet in staat. De regels:
   - `frontends/regelspraak/grammar/DIVERGENCES.md` r. 856, 915, 1546, 1723, 2134 en 2362;
   - `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/DefinitionResolver.java` r. 383 (javadoc);
   - `backends/drools/src/test/java/dev/uchar/regelspraak/cases/BonenveldWitnessTest.java` r. 97 (commentaar in een
     test).

   Voorstel: in alle acht "temporary" schrappen ("the repository's parity zone"). Dat is alleen proza en commentaar.
   Ze vallen buiten de May touch van deze kaart, om twee redenen. Een testbestand wijzigen vraagt eerst toestemming.
   En in `frontends/regelspraak` en `backends/drools` werken parallelle kaarten.
7. **De gedateerde regels in PLAN.** Het gaat om r. 359, 710–713, 733 en 788 (zie de grep). Ze blijven als verslag
   staan. De hoofdsessie beslist of er een verwijzing naar de aanvulling bij (10) bij komt.
8. **De toolchain.** Het gaat om `NOTICE`, `README.md`, de zoneregel in `CLAUDE.md` en `examples/toka/README.md`. De
   vaste tekst hoort daar ook, met de bestandslijst van de toolchain. Dat wordt een eigen kaart in die repository,
   samen met de besluiten over de zoneregel (C6) en de specificatienotities (S).
9. **Waarneming, niet gewijzigd.** `scripts/admit.sh` noemt zijn patronen "the gate-1 patterns", maar wijkt van gate
   1 af: het patroon bevat wel `regelspraakruntime` en geen `main-spec`. Dat stond er al vóór deze kaart. Met C7-a
   heeft het niets te maken, want de toolchain stond niet in het patroon van `admit.sh`.
