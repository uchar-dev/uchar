# HERNOEM_LADDER_20260829 — hernoemingsplan: ladder → prioritized defaults

*Status: GERATIFICEERD · 2026-08-29 · werkprosa (geen gate-1-zone; mag
privérepo's noemen). De acht vragen van §9 zijn door de maintainer
beantwoord: V1–V3 en V5–V8 conform aanbeveling, **V4 afwijkend — het
archief gaat mee**. De uitvoering
raakt vrijwel elk protected path (`spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `spec/schema/`, `spec/verifier/`,
`corpus/golden/anchor/`, `scripts/`, `CLAUDE.md`), dus de kaartbranch is
per definitie een voorstel dat de maintainer merget. Dit document is de
ontwerpverantwoording; de overneembare kaarttekst staat in bijlage B.*

## 0. Waarom, en waarheen

"Ladder" is op 2026-08-15/16 in dit project gemunt (herkomst: de
m09-lestekst van de calculemus-cursus → `corpus/jutterswet/README.md`
"uitzonderingsladders" → ASP-oefening 2 → RS ch. 7) en is **geen vakterm**:
de term komt in geen van de geciteerde bronnen voor (Kowalski, BNA-paper,
Catala-paper, Lawsky) en niet in de rechtsinformatica (Prakken/Sartor,
Hage, Governatori, ABGM-defeasible logic). De spec belijdt de juiste
familie overigens al: `IR-SPEC.md:1892` — "prioritised defaults per target
evaluating top-down"; `RUNTIME-SEMANTICS.md:2332` — "Catala's default
calculus — prioritised defaults with explicit conflict detection".

De hernoeming trekt de naamgeving gelijk met die belijdenis. Doelvocabulaire:
**prioritized defaults** (de bundel per doel), **default** (het element,
in de zin van Horty/Lawsky/Catala: justification + consequence, prioriteit
doet het verweer), **exception group** (de geneste subgroep, aansluitend
bij het bestaande wire-veld `excepties` en Catala's exceptions).

Wat de hernoeming NIET verandert: semantiek, WF-nummers (WF-050/051/075),
de wire-velden `excepties`/`besluit`/`consequentie`/`initialisatie`/
`ongeordend`, de begrippen fall-through, displacement en decides (geen
laddermetafoor), en de gate-set.

## 1. Doelterminologie

### 1.1 Wire en typenamen (de vijf spellingen op de wire)

| nu (wire/code) | wordt | waar |
|---|---|---|
| `defaultLadders` (veld) | `prioritizedDefaults` | schema, alle `ir.json`, emitters, loaders |
| `"DefaultLadder"` (typestring) | `"PrioritizedDefaults"` | idem |
| `rungs` (veld) | `defaults` | idem |
| `"Rung"` (typestring) | `"Default"` | idem |
| `"SubLadder"` (typestring) | `"ExceptionGroup"` | idem |
| `DefaultLadder.java` / OCaml-typen | `PrioritizedDefaults` | frontends `ir/`, drools-loader, ref, verifier, clingo |
| `Rung.java` (incl. `Rung.Excepties`) | `Default.java` (`Default.Excepties`) | idem |
| `SubLadder.java` | `ExceptionGroup.java` | idem |

Let op Java: `default` is een keyword — het **veld** `defaults` en de
**klasse** `Default` zijn geldig, maar lokale variabelen mogen niet
`default` heten (kies `dft`, `d`, of het meervoud). OCaml en Python kennen
geen `default`-keyword.

### 1.2 Samengestelde identifiers (stamregel)

Stam-substitutie, werktuiglijk toe te passen: stam `ladder` → `defaults`,
stam `Rung` → `Default`. Gemeten voorbeelden uit `backends/drools` en
`frontends/regelspraak`:

| nu | wordt |
|---|---|
| `run_ladder_group` | `run_defaults_group` |
| `LadderRun`, `LadderOnvertaalbaar`, `LadderGlobals`, `LadderLeesnamen` | `DefaultsRun`, `DefaultsOnvertaalbaar`, … |
| `ladderGroep(en)`, `ladderSalience`, `ladderReden`, `ladderSleutel`, `ladderNorm`, `ladderConstructNaam` | `defaultsGroep(en)`, `defaultsSalience`, … |
| `GecompileerdeRung` | `GecompileerdeDefault` |
| `RuleChecker$LadderDoel` | `RuleChecker$DefaultsDoel` |
| `LadderExtensieTest.java` | `DefaultsExtensieTest.java` |

### 1.3 Engelse spec-prose

| nu | wordt |
|---|---|
| ladder / default ladder | prioritized defaults; "the defaults on `T`" |
| rung | default |
| sub-ladder / sub-group onder een rung | exception group |
| attribute / characteristic ladder | attribute / characteristic defaults |
| initialisation rung; virtual input rung | initialisation default; virtual input default |
| top / highest / base rung | highest(-priority) / base default |
| declared-unordered ladder group | declared-unordered default group |
| ladder service (Drools) | defaults service |
| ladder fold ("laddervouw", WF-051) | defaults fold |
| ladder conflict (§9.5-sleutelprose) | default-group conflict |

### 1.4 Nederlandse prose (kaarten, NOTE's)

Conventie blijft: infrastructuurtermen Engels. Dus: "ladder" →
"(geprioriteerde) defaults" of "defaults-bundel"; **"sport" → "default"**
(de NL-metafoor leeft o.a. in `corpus/golden/anchor/ww-herleving-inkomen/NOTE.md`,
inclusief samenstellingen als "herleving-sport" → "herleving-default" en
"form-a-sport" → "form-a-default"); "laddervouw" → "defaultsvouw".

### 1.5 Wat blijft

WF-050/051/075 (nummers én betekenis); `excepties`, `besluit`,
`consequentie`, `initialisatie`, `ongeordend` (wire, deels NL, geen
laddermetafoor); fall-through, displacement, decides, speculative
evaluation; de §9.5-kanaalvorm en padsleutels; alle golden-dirnamen
(geen enkele draagt ladder/rung); `toka/` integraal (zie §2).

## 2. Reikwijdte en historische grens (geratificeerd: archief inbegrepen)

**Mee: alles wat de repository tracked, levend én archief.** Levend:
`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`spec/ASP-TRANSLATION.md`, `spec/PLAN.md` integraal (ook gedateerde
logregels en rondenamen), `spec/schema/`, `spec/verifier/`, `ref/`,
`backends/clingo`, `backends/drools`, `frontends/` (catala én
regelspraak, incl. testbestanden — V7), `corpus/golden/` en
`corpus/negatief/` (ir.json, NOTE.md, dirnamen — V6), `scripts/`,
`CLAUDE.md`, en de **vier open kaarten** in `backlog/new/`
(frontend-uitzondering en stratificatie-hoofdstuk zwaar; triage-sluiting
7 treffers; eenheden-normalisatiepas alleen de proposalverwijzing).

Archief (maintainersbesluit 2026-08-29, **V4-B, afwijkend van de
aanbeveling**): `spec/proposals/**` (371 treffers) en `backlog/done/**`
(335 treffers in 58 kaarten) worden inhoudelijk mee omgezet, inclusief
zes bestandsnamen via `git mv`:

| nu | wordt |
|---|---|
| `spec/proposals/LADDER-EVOLUTIE.md` | `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` |
| `backlog/done/20260815-asp-ladder.md` | `…/20260815-asp-defaults.md` |
| `backlog/done/20260817-art4-ladder.md` | `…/20260817-art4-defaults.md` |
| `backlog/done/20260817-attribute-ladder.md` | `…/20260817-attribute-defaults.md` |
| `backlog/done/20260829-drools-ladderservice.md` | `…/20260829-drools-defaultsservice.md` |
| `backlog/done/20260829-ladder-evolutie-voorstel.md` | `…/20260829-defaults-evolutie-voorstel.md` |

Elke verwijzing naar die bestandsnamen of naar rondenamen
("`LADDER-EVOLUTIE.md` Q9", "de ladder-ronde") wordt repo-breed in de
nieuwe spelling bijgewerkt.

**Niet mee:**

1. `toka/` — de integratietests zijn er "in original form" (gate 11);
   gemeten: nul treffers, dus er valt ook niets te doen.
2. Git-geschiedenis en commitboodschappen — onveranderbaar zonder
   history rewrite, die hier niet aan de orde is. **Aanvaard gevolg van
   V4-B:** het herschreven archief spreekt de nieuwe taal terwijl zijn
   eigen commits de oude spreken; de formerly-noot hieronder is de brug.
3. Bouwartefacten (`_build/`, `out/`, `target/`).
4. Dit plandocument zelf — de bestandsnaam `HERNOEM_LADDER_20260829.md`
   is door de maintainer bepaald en draagt de oude term als onderwerp;
   in de acceptatie-grep uitgesloten met deze reden.
5. De cursus-repo (`calculemus-cursus`) — andere repository, eigen
   didactische keuzes; buiten dit plan.

**De brug tussen geschiedenis en heden** is één "formerly"-noot op de
twee normatieve plekken (IR-SPEC ch. 6.1-kop en RS ch. 7.1-kop):

> *Renamed 2026-08-XX from "ladder"/"rung"/"sub-ladder" (coined here
> 2026-08-15/16; no term of art). Git history — commit messages and
> pre-rename revisions — uses the old names throughout.*

plus de naamsverantwoordingsalinea van bijlage A in RS ch. 7.1.

## 3. Inventaris (gemeten 2026-08-29, `main` = 55a7e1b)

| component | treffers `ladder\|rung` (ci) |
|---|---|
| `spec/IR-SPEC.md` / `RUNTIME-SEMANTICS.md` | 83 / 171 |
| `spec/schema/` | 28 |
| `spec/verifier/` | 209 |
| `ref/` (lib 63, test 15) | 78 |
| `backends/clingo` / `backends/drools` | 77 / 170 |
| `frontends/catala` / `regelspraak` (main 128, test 31) | 202 / 159+ |
| `corpus/golden` (json + NOTE's) / `corpus/negatief` | 224 / 196 |
| `corpus/rood` / `toka/` | 0 / 0 |
| `scripts/` | 11 (o.a. `check-noop.sh`; inventarisstap A verifieert `check-drools.sh`) |
| `CLAUDE.md` / `spec/PLAN.md` | 2 / 8 |
| `spec/proposals/` (V4-B; zwaarst: PHASE4-DEFAULTS 105, LADDER-EVOLUTIE 87) | 371 |
| `backlog/done/` (V4-B; 58 kaarten, 5 bestandsnamen) | 335 |
| wire-strings in golden-ir.json | `defaultLadders` 23 · `rungs` 44 · `"DefaultLadder"` 25 · `"Rung"` 61 · `"SubLadder"` 19 |
| negatief-dirs met oude naam | 12 (lijst in fase E) |

Bouwartefacten (`_build/`, `out/`, `target/`, `lib/`-jars) tellen niet:
die verdwijnen bij herbouw.

## 4. Uitvoeringsvolgorde

Eén kaart, één branch (`card/hernoem-prioritized-defaults`), strikt
serieel; geen andere kaart loopt zolang deze open staat (het raakoppervlak
is de hele repo — WAY-OF-WORKING §1). Tussencommits hoeven niet gate-groen
(een wire-epoch kán niet per stap groen), het eindresultaat wel: alle
twaalf gates.

- **A. Claim + nulmeting.** Kaart naar `doing/`. De acceptatie-grep van §6
  éérst rood tonen (huidige stand ± 2300 treffers, archief meegeteld —
  V4-B) — het rood-eerst-idioom, hier op de hernoeming zelf.
- **B. Spec.** IR-SPEC ch. 6.1 e.o.; RS ch. 6 (WF-051-prose), ch. 7
  integraal, ch. 8.2/9.5-passages; CONSTRUCTS (registerrijen volledig,
  gedateerde vermeldingen inbegrepen — de formerly-noot maakt ze leesbaar);
  ASP-TRANSLATION (S10/S17 e.o.). Formerly-noot + bijlage-A-alinea erin.
- **C. Schema.** `regelspraak-ir-v0.schema.json`: definities en
  veldnamen; versienoot bump (v0.10, "the rename epoch"); bestandsnaam
  blijft. Canonicaliser/serialiser (gate-2-vorm) mee.
- **D. Verifier.** `spec/verifier/` (209): typen, checks,
  meldingsteksten — WF-nummers blijven in elke melding (gate-3-contract).
- **E. Corpus.** (1) Twaalf `git mv`'s in `corpus/negatief/`:
  `wf050-allocation-beside-ladder→wf050-allocation-beside-defaults`,
  `wf050-creation-beside-ladder→…-beside-defaults`,
  `wf050-empty-ladder→wf050-empty-defaults`,
  `wf050-ladder-beside-rule→wf050-defaults-beside-rule`,
  `wf050-rule-beside-attribute-ladder→…-attribute-defaults`,
  `wf050-subrung-wrong-shape→wf050-subdefault-wrong-shape`,
  `wf050-two-ladders-one-target→wf050-two-defaults-one-target`,
  `wf051-cycle-through-subrung→…-through-subdefault`,
  `wf051-ladder-reads-own-target→wf051-defaults-read-own-target`,
  `wf075-contentless-rung→wf075-contentless-default`,
  `wf075-grouprung-with-justification→wf075-groupdefault-with-justification`,
  `wf075-init-in-subrung→wf075-init-in-subdefault` — plus hun ir.json
  en NOTE.md. (2) Golden-ir.json: waar `bron.catala_en`/`bron.rgs`
  bestaat regenereren via de plugin resp. `make ir FILE=…`
  (byte-identiek per gates 9/10); handgeschreven anchors gericht
  substitueren en met de gate-2-round-trip bewijzen. (3) NOTE.md's,
  incl. de sport-metafoor in `ww-herleving-inkomen`. Inventarisstap:
  bevestig dat `case*.json` nergens ladder/rung draagt.
- **F. ref/.** `lib/` (eval.ml, case.ml) en `test/test_ref.ml`
  (testbestand — valt onder de V7-toestemming).
- **G. clingo.** `backends/clingo/lib` + tests; interne functienamen en
  eventueel gegenereerde ASP-commentaarregels.
- **H. drools.** Loader (`IrLoader`), ir-records, transpiler
  (`LadderRun`, `GecompileerdeRung`, salience-namen), CaseRunner-prose
  (§9.5-conflictsleutels), DRL-sjablonen.
- **I. frontends.** Catala-plugin (202); regelspraak: `ir/`-typen,
  resolver (`LadderDoel`), emitter, tests (`LadderExtensieTest`).
  Daarna regeneratiebewijs: gates 9 en 10 integraal.
- **J. Scripts + levende prose.** `check-noop.sh`-survivorredenen,
  `check-drools.sh`-reden-/divergentielijsten; `CLAUDE.md` (gate-12-zin:
  "defaultLadder lowering" → "prioritizedDefaults lowering";
  kaartnaamverwijzingen volgen de `git mv`'s van fase K); `spec/PLAN.md`
  integraal — ook logregels en rondenamen (V4-B); de vier open kaarten
  in `backlog/new/` herschreven in het doelvocabulaire.
- **K. Archief (V4-B).** `spec/proposals/**` (371) en `backlog/done/**`
  (335 in 58 kaarten) inhoudelijk omgezet — pure substitutie, geen
  redactie; de zes `git mv`'s uit §2; daarna repo-breed elke verwijzing
  naar de oude bestandsnamen bijgewerkt (PLAN, CLAUDE.md, open kaarten;
  dit plandocument uitgezonderd).
- **L. Bewijs + afronding.** Alle twaalf gates groen met output; de
  acceptatie-grep van §6 levert exact de verwachte restlijst; kaart naar
  `done/` met `Result:`; maintainer merget (protected paths). Direct
  daarna het nawerk van §7 inplannen.

## 5. Byte-identiteit en de wire-epoch

De hernoeming is een brekende wire-wijziging; drie onafhankelijke
regeneratieroutes moeten dezelfde nieuwe spelling produceren:

1. **gate 2** — canonicaliser-round-trip over elke `ir.json`;
2. **gate 9** — catala-goldens regenereren byte-identiek door de plugin;
3. **gate 10** — rgs-goldens re-emitten byte-identiek door de frontend
   (`make ir FILE=…`, vanuit de hoofdcheckout).

Volgorde is dwingend: eerst schema + canonicaliser (C), dan pas corpus
(E), dan de emitters (I) vóór het gate-9/10-bewijs. Geen
overgangsalias in het schema: één spelling is geldig, de oude faalt
luid (fail-fast; zie V5).

## 6. Acceptatie (het nul-grep-contract)

```sh
git grep -inE 'ladder|rung' -- . \
  ':(exclude)toka' ':(exclude)HERNOEM_LADDER_20260829.md'
```

moet na afloop **uitsluitend** de reason-carrying restlijst opleveren
(het gate-7/12-idioom: expliciet, met reden, faalt-wanneer-verouderd —
de lijst wordt letterlijk in de kaart-Result opgenomen). Door V4-B is
die lijst kort:

1. de twee formerly-noten en de bijlage-A-alinea (citeren de oude naam,
   met reden — de brug naar de git-geschiedenis);
2. eventuele vals-positieven van de substring-grep (in te perken met
   `\b`; "sprung"/"wrung"-achtige woorden komen in de repo nu niet voor).

Daarnaast: alle twaalf gates groen met getoonde output, en `git status`
schoon op de branch na de laatste commit.

## 7. Nawerk buiten deze repo (aparte kaart/sessie)

- **regelspraak-java**: vendored-subtree-sync van de hernoemde frontend +
  `SPEC_REV`-pin-bump als conformance-PR (spec-first, CLAUDE.md
  "Repo relationships"). Het CLI-wrapper-commentaar dat naar
  "LADDER-EVOLUTIE §6" verwijst gaat in diezelfde PR mee naar de nieuwe
  proposalnaam — de V4-B-lijn doorgetrokken.
- Geen actie in `calculemus-cursus` (buiten scope, eigen register).

## 8. Valstrikken

1. **Java-keyword `default`** — nooit als lokale variabele/veldnaam
   enkelvoud; meervoud `defaults` en klasse `Default` zijn veilig.
2. **Substring-grep** — "sport" matcht "transport": NL-substituties met
   woordgrenzen uitvoeren en handmatig nalopen.
3. **Citaat vs. terminologie** — door V4-B is vrijwel alles
   terminologie: ook gedateerde besluitzinnen, rondenamen en
   archiefteksten gaan om; alleen de git-geschiedenis blijft oud. Elke
   verwijzing naar een hernoemd bestand (de proposal, de vijf
   done-kaarten) wordt bijgewerkt — een achterblijvende oude
   bestandsnaam is een gebroken pad, geen citaat.
4. **`toka/` niet aanraken** — original form is een gate-11-eigenschap;
   er is bovendien niets te vervangen (nulmeting).
5. **Testbestanden** — wijzigen vereist per huisregel toestemming; deze
   kaart ís dat verzoek (V7). Migratiebestanden zijn niet in het geding.
6. **De symlink `regelspraak-java/`** in de root niet meenemen in greps
   of substituties (andere repository; volgt via §7).
7. **Bouwartefacten** (`out/`, `target/`, `_build/`) niet editen —
   herbouwen.
8. **Anchor-goldens zijn protected** — de waarden zijn permanent gepind;
   de hernoeming raakt spelling, nooit een verwachtingswaarde. Elke
   inhoudelijke afwijking in gate 5/6/12-uitvoer is een stopteken.

## 9. Besluiten (geratificeerd door de maintainer, 2026-08-29)

- **V1 — bundelnaam: A.** `prioritizedDefaults`/`PrioritizedDefaults` —
  de familienaam die de spec-prose al gebruikt; claimt geen identiteit
  met Catala of ABGM. (Verworpen: `defaultTree(s)`, de nuloptie.)
- **V2 — elementnaam: A.** `Default`/`defaults` — Horty's default,
  exact het paar justification+consequence.
- **V3 — subgroepnaam: A.** `ExceptionGroup` — matcht het wire-veld
  `excepties` en Catala's exceptions.
- **V4 — historische grens: B, afwijkend van de aanbeveling.** Het
  archief gaat mee: `spec/proposals/**` en `backlog/done/**` worden
  inhoudelijk omgezet, inclusief de zes bestandsnamen (§2). Aanvaard
  gevolg: het archief spreekt de nieuwe taal, zijn commits de oude; de
  formerly-noot overbrugt naar de git-geschiedenis.
- **V5 — schema-epoch: A.** Harde hernoeming, v0.10, geen
  dubbelspelling; een oud bestand faalt luid.
- **V6 — negatief-dirnamen: A.** De twaalf `git mv`'s van fase E; de
  dirnaam is levend gate-3-contract.
- **V7 — testbestanden: toestemming verleend.** Deze ratificatie is het
  door de huisregel vereiste akkoord om `LadderExtensieTest.java`,
  `ref/test/test_ref.ml` en verwante testbestanden te hernoemen/aan te
  passen — uitsluitend naamsubstitutie, verwachtingswaarden onaangetast;
  `toka/`-tests blijven hoe dan ook buiten scope.
- **V8 — volgorde: A.** De hernoeming landt vóór de vier open kaarten;
  die worden in fase J herschreven en bouwen daarna in de nieuwe taal.

## Bijlage A — naamsverantwoording (concept, landt in RS ch. 7.1)

> *Naming.* "Prioritized defaults" replaces "ladder", coined in this
> project (2026-08-15/16) and no term of art. The family is prioritized
> default logic (Reiter; Brewka; Horty, *Reasons as Defaults*; Lawsky,
> *A Logic for Statutes*, 21 Fla. Tax Rev. 60 (2017)). A default here is
> a Horty-style pair — justification plus consequence — with priority
> doing the work of Reiter's consistency justifications: the lowering
> generates `not decides_m` for every higher default. The nearest named
> structure is Catala's default tree (Merigoux, Chataing & Protzenko,
> ICFP 2021), from which this deviates exactly on the tracked
> `CONSTRUCTS.md` row-8 deltas (observable non-fatal conflicts,
> equal-decision reconciliation, leeg fall-through across compositions).
> It is deliberately *not* a defeasible theory in the
> Antoniou–Billington–Governatori–Maher sense: priority is a total,
> structural, per-target order — never a free `r1 > r2` relation
> (`spec/proposals/PRIORITY-RELATION.md`, C2) — there is no
> strict/defeasible/defeater trichotomy, and a pure blocker on an
> attribute target is inexpressible by design (a defaults group never
> writes leeg). Operational kin, for the integrator: DMN hit policies
> FIRST (the ordered group) and ANY (the declared-unordered group), and
> XACML's `first-applicable` / `only-one-applicable` with
> `Indeterminate`-as-value. Unrelated to ladder logic (IEC 61131-3).

## Bijlage B — overneembare kaarttekst

```markdown
# Hernoeming: ladder → prioritized defaults (wire-epoch v0.10)

Goal: de volledige, in één beweging uitgevoerde hernoeming van het
gemunte "ladder/rung/sub-ladder"-vocabulaire naar prioritized
defaults / default / exception group, conform
HERNOEM_LADDER_20260829.md (geratificeerd 2026-08-29, V1–V8; archief
inbegrepen per V4-B): spec (IR-SPEC, RUNTIME-SEMANTICS, CONSTRUCTS,
ASP-TRANSLATION, PLAN integraal), schema + wire (vijf spellingen,
epoch v0.10, geen alias), verifier, ref, clingo, drools, beide
frontends incl. tests (V7), corpus (golden-regeneratie byte-identiek;
twaalf negatief-dirs via git mv; NOTE's incl. de NL-metafoor "sport"),
scripts, CLAUDE.md, de vier open kaarten, én het archief —
spec/proposals/ en backlog/done/ inhoudelijk plus de zes git mv's van
§2 — met de formerly-noot en de naamsverantwoordingsalinea (bijlage A)
als brug naar de git-geschiedenis.

Acceptance: alle twaalf gates groen met output; de §6-grep levert
exact de reason-carrying restlijst (formerly-noten + eventuele
vals-positieven; rood getoond bij aanvang); geen verwachtingswaarde
gewijzigd.

May touch: spec/ integraal (IR-REVIEW.md: nulmeting), corpus/golden,
corpus/negatief, ref/, backends/, frontends/, scripts/, CLAUDE.md,
backlog/new/*.md, backlog/done/**, spec/proposals/**, backlog/
(kaartverplaatsing), HERNOEM_LADDER_20260829.md.

Out of scope: toka/, corpus/rood (nulmeting), git-geschiedenis en
commitboodschappen, de bestandsnaam HERNOEM_LADDER_20260829.md,
regelspraak-java (volgt als pin-bump-kaart), calculemus-cursus.
```
