# Fase 5d — de frontend-diff-oefening

Goal: hetzelfde stuk Jutterswet-recht door beide frontends —
RegelSpraak via de toolchain-resolver (--ir-dump op de gepinde
SPEC_REV), Catala via de plugin — en beide IR-dumps structureel
gediffed en geclassificeerd: (i) administratief (namen, bronnen,
volgorde), (ii) getrackte v0↔v1-delta (gewild, CONSTRUCTS), (iii)
echte ontwerpfout — dezelfde semantiek die zonder reden twee vormen
krijgt. Gedocumenteerd met de resulterende spec-gevolgen; daarmee
valt het tweede Phase-5-exitcriterium (PLAN §6: "the frontend-diff
exercise is documented with resulting spec changes").

Acceptance: het diff-document in spec/proposals/; elke
categorie-(iii)-vondst draagt een spec-gevolg (besluit-vrije delta,
of een genummerde maintainersvraag vóór de merge); alle negen gates
groen; PLAN §6 Phase-5-markering bijgewerkt; twee opus-lenzen
verwerkt vóór de merge.

May touch: spec/proposals/, spec/CONSTRUCTS.md, spec/PLAN.md,
corpus/golden/ (een nieuw paar-fragment indien nodig),
frontends/catala/ (fixture indien nodig), backlog/, en — correctie
bij uitvoering, expliciet genoteerd — scripts/check-noop.sh: de
stilte-cases van het nieuwe paar-fragment zijn no-op-survivors en
moeten reden-dragend in de survivor-lijst (gate 7 eist dat); een
beschermd pad, onder het lens-plus-mandaat-regime.

Out of scope: normatieve IR-SPEC/RUNTIME-SEMANTICS-wijzigingen
zonder genummerde vraag; elke toolchain-wijziging (read-only, de
dump-aanroep uitgezonderd).

## Voortgang — het paar-fragment (2026-08-24)

Gekozen rechtsinhoud: **Jutterswet art. 3 jo. 5 (penningrecht /
hardheid)** — de defeasibility-ladder (basis: geen recht; art. 3:
drempel van twaalf; art. 5: hardheid leest twaalf als zes, een
uitzondering óp art. 3). Route (b): de bestaande Catala-golden
`jutterswet-penningrecht-catala` (plugin) krijgt zijn
RegelSpraak-tegenhanger, geschreven en door de toolchain-resolver
gedumpt.

Waarom dit fragment: het draagt de ladder + een voorwaarde (>=12/>=6,
met een samengestelde hardheid-EN-drempel in art. 5), en het is
**bewijsbaar semantisch gelijk op élke invoer** — beide ladder-sporten
beslissen dezelfde waarde (`waar`), dus de geordende ladder valt samen
met een gewone disjunctie en er is geen sport-conflict bereikbaar. De
toolchain kent geen ladder en rendert dezelfde wet als twee platte
`KENMERKTOEKENNING`-regels; disjunctie ≡ ladder hier. Het paar isoleert
zo een **zuivere vorm-diff**. `tariefrecht-catala` (met berekening) is
bewust afgewezen: zijn twee ongeordende uitzonderingen geven op de
overlap-invoer een Catala `ConflictError` (uchar §9.5), die platte,
elkaar uitsluitende RegelSpraak-regels niet reproduceren — dat zou de
vorm-diff met een echte semantische afwijking vervuilen.

Beide dumps reproduceerbaar geproduceerd:
- RegelSpraak-kant (nieuw): `corpus/golden/jutterswet-penningrecht/`
  (bron.rgs, ir.json, vijf cases). Recept: vanuit de regelspraak-java
  checkout `make ir FILE=<...>/jutterswet-penningrecht/bron.rgs
  OUT=<pad>` — byte-identiek aan de ingecheckte `ir.json` (geverifieerd
  tegen een verse resolve). Revisies: uchar-branchcommit `3285c58` (na rebase ververst),
  toolchain git HEAD `332db4c`, toolchain SPEC_REV `81e198e`.
- Catala-kant (bestaand): `jutterswet-penningrecht-catala/ir.json`,
  door de plugin (`_build/default/frontends/catala/bin/catala_frontend.exe`
  na `dune build`, gecanoniseerd met de gate-2-slice — gate 9
  regenereert byte-identiek).

Kern-vondst voor de analyse-stap: **plugin → `defaultLadders` (één
entry, twee rungs); toolchain → twee `resolvedRegels`
(`KENMERKTOEKENNING`) en géén `defaultLadders`-veld (de v0-emitter laat
het weg, waar de Catala-dump het gevuld draagt).** Plus category-i-ruis
(objecttypenaam Jutter vs Penningrecht, attribuutnaam met spatie vs
underscore, `bron`-strings).

Geparkeerd voor de analyse-stap onder `/tmp/fase5d/`: `RECIPE.md`
(keuze + recept + revisies), `rgs-penningrecht-ir.json` en
`catala-penningrecht-ir.json` (rauwe dumps, canonieke gate-2-vorm,
niets gestript), en `rgs-penningrecht-norm.json` /
`catala-penningrecht-norm.json` (genormaliseerd: alle `bron`-velden
recursief gestript én sleutels gesorteerd — alleen op de `-norm`-kopieën
toegepast, genoteerd, nooit op de ingecheckte dumps).

Protected-path voorstel (maintainer merget): `scripts/check-noop.sh`
krijgt twee survivor-pins met reden —
`jutterswet-penningrecht/case3-stilte.json` en `case4-hardheidsgrens.json`
(stilte zonder grond is de bedoeling), naast hun anker- en
Catala-tweelingen.

Status: alle negen gates groen (gate 9 lokaal, catala 1.2.1). De
analyse (het diff-document in `spec/proposals/`, de
categorie-classificatie met spec-gevolgen, PLAN §6-markering, twee
opus-lenzen) is de vervolgstap die deze geparkeerde artefacten leest.

## De analyse — het diff-document (2026-08-24)

`spec/proposals/PHASE5D-DIFF.md` geschreven: het gekozen fragment en
waarom, het reproduceerbare dump-recept (revisies gepind), de
gelijkheidstoets (vijf cases, dezelfde absolute uitkomsten door ref én
clingo op beide dumps), en de volledige boom-classificatie.

Uitkomst van de classificatie:
- **(i) administratief:** objecttypenaam (Jutter/Penningrecht),
  member-namen (spatie vs underscore), `bron`/`onderwerpBron`-strings,
  `kenmerken`-volgorde, `meervoud`, en `teken` (NIET_NEGATIEF vs
  ONBEPAALD — een auteursverschil in de twee bronteksten, benigne op dit
  corpus).
- **(ii) getrackte v0↔v1-delta (bevestiging, geen vondst):** de kern —
  `defaultLadder` (Catala) vs twee platte `KENMERKTOEKENNING`-regels
  (toolchain), getrackt in CONSTRUCTS rij 8/2/7 + IR-SPEC ch. 6.1 +
  RUNTIME-SEMANTICS ch. 7/7.7; de parse-stage `Regel`-shells (§2.3
  non-goal); het optionele `defaultLadders`-veld aanwezig vs
  nooit-geproduceerd (schema/Method wire-discipline). Onder de container
  zijn de conditie-bomen vormidentiek — de hele diff zit in de wrapper.
- **(iii) echte ontwerpfout: GEEN.** Elk verschil valt in (i) of een
  getrackte (ii); geen verwacht-maar-ongetrackt verschil.

Eén beslissingsvrije documentaire aanbeveling (§5): een verhelderende
zin in ch. 7.7 / CONSTRUCTS rij 8 dat meervoudige gelijk-waardige
kenmerktoekenningen op één doel native blijven (idempotent, ordevrij),
extensioneel de disjunctieve één-sport-ladder. Géén genummerde
maintainersvraag; `besluit_plichtig` leeg.

Eerlijke grenzen (§6): v0-spiegel vs v1-vormen; één fragment ≠
volledigheid; het divergente geval (ongeordende, verschillend-beslissende
uitzonderingen — `tariefrecht`) is bewust buiten scope, dáár kan een
echte vorm-fout nog schuilen; de twee bronnen zijn geen teken-identieke
declaraties (`teken`, meervoud).

Gates 1–2 groen na het toevoegen van het document (raakt geen
artefactzone). Vervolg (card-close): PLAN §6-markering flippen, twee
opus-lenzen.

## Afronding — documentatie geland (2026-08-24)

De besluit-vrije uitkomst van de diff is vastgelegd en de
Phase-5-markering geflipt (geen semantiekwijziging):

- `spec/CONSTRUCTS.md` rij 8 draagt nu de getrackte Phase-5d-notitie: het
  penningrecht-paar (toolchain twee platte `KENMERKTOEKENNING` vs Catala's
  ene `defaultLadder`) diffde tot **geen categorie-(iii)-ontwerpfout**; de
  ene besluit-vrije observatie — meervoudige gelijk-waardige
  kenmerktoekenningen op één doel blijven native, niet gedesugared
  (ordevrij/conflictvrij per monotone idempotentie ch. 5.3, extensioneel de
  disjunctieve één-sport-equivalent) — staat er verwijzend naar
  `spec/proposals/PHASE5D-DIFF.md`. Geen genummerde maintainersvraag.
- `spec/PLAN.md` §6 Phase 5: **"Met in part" → "Met" (2026-08-24)**, met
  beide getuigen — `spec/proposals/PHASE5C-TUTORIAL.md` (het eerste
  criterium, tutorial-round-trip) en `spec/proposals/PHASE5D-DIFF.md` (het
  frontend-diff-criterium, het paar-fragment door de volle gate-set). Beide
  exitcriteria staan; de hourglass-claim is nu waar (twee frontends,
  twee-plus backends, 2×2 door één waist). Verder niets geherformuleerd.

Ter controle gedraaid: gate 1 (`check-self-contained.sh`) en gate 2
(`check-ir-schema.sh`) groen — PLAN/CONSTRUCTS/backlog zijn working prose,
geen artefactzone. Gate 9 (`check-catala.sh`) groen (lokaal, catala 1.2.1):
de plugin-suite + de byte-identieke corpus-regeneratie ongewijzigd, want de
afronding raakt `frontends/catala/` noch `corpus/golden/` niet.

Open vóór de merge: twee opus-lenzen op het diff-document.

Result: de frontend-diff-oefening is uitgevoerd en gedocumenteerd
(spec/proposals/PHASE5D-DIFF.md); daarmee staat PLAN §6 Phase 5 op
"Met" met beide getuigen. Het paar: Jutterswet art. 3 jo. 5
(penningrecht/hardheid) — de toolchain-kant nieuw geschreven als
emitter-golden corpus/golden/jutterswet-penningrecht (bron.rgs +
ir.json byte-identiek reproduceerbaar, vijf cases die de
catala-golden één-op-één spiegelen, twee reden-dragende
no-op-survivor-pins), naast de bestaande
jutterswet-penningrecht-catala. De vorm-diff: de toolchain emitteert
twee platte KENMERKTOEKENNING-regels zonder defaultLadders-sleutel;
de plugin één ladder met drie sporten — en de structurele
classificatie leverde géén categorie-(iii)-ontwerpfout: elk verschil
is administratief of een getrackte v0↔v1-delta (CONSTRUCTS rijen
8/2/7). Eén besluit-vrije observatie is op rij 8 vastgelegd
(meervoudige gelijk-waardige kenmerktoekenningen blijven native —
ordevrij en conflictvrij door monotone idempotentie). De lensronde
(twee opus-lenzen, empirisch) bevestigde de gelijkheidsclaim op tien
extra flanken (leeg, negatief, grenzen, de overlap) over beide
vormen × beide executors, en de teken-benignclaim op negatieve
invoer; twee documentaire MINORs zijn vóór de merge verwerkt (de te
sterke wrapper-only-formulering; verweesde pre-rebase-hashes in het
recept). Bleef bewust liggen, met naam genoemd in het document: het
werkelijk divergente geval — ongeordende, verschillend besluitende
uitzonderingen (tariefrecht's ConflictError) — is buiten dit paar
gehouden om de vorm-diff niet met een semantische divergentie te
vervuilen; dáár kan een echte vormfout nog schuilen. Eén fragment
bewijst geen volledigheid; de oefening is een steekproef op de
kern-defeasibility, geen dekkingsclaim.
