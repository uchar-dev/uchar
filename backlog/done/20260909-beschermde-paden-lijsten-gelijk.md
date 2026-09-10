# De twee beschermde-padenlijsten lopen uiteen

Goal: `CLAUDE.md` beschrijft de beschermde paden in proza en
`.github/CODEOWNERS` handhaaft ze mechanisch, maar de twee dekken niet
hetzelfde. CODEOWNERS claimt `/spec/` in zijn geheel; de prozalijst noemt
binnen `spec/` alleen `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/` en `spec/verifier/`. Deze kaart trekt de twee gelijk, of legt
het verschil expliciet vast als bedoeld — met de reden erbij, zodat een
sessie aan één lijst genoeg heeft om te weten wat zij niet mag mergen.

## De meting (2026-09-09, tijdens `20260906-gedurende-complementwandeling`)

`spec/` draagt op topniveau negen items. Vier staan in de prozalijst
(`IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, `schema/`, `verifier/`). **Vijf
vallen buiten de prozalijst maar binnen de `/spec/`-claim van CODEOWNERS:**
`ASP-TRANSLATION.md`, `CONSTRUCTS.md`, `IR-REVIEW.md`, `PLAN.md` en
`proposals/`.

Het scherpst is de tegenspraak bij twee daarvan:

- `spec/PLAN.md` — `CLAUDE.md` draagt sessies op de kaart af te sluiten en
  "**only if scope changed** — `spec/PLAN.md`" bij te werken. Dat is een
  instructie om het bestand te wijzigen; CODEOWNERS eist er
  maintainer-review voor.
- `spec/CONSTRUCTS.md` — `CLAUDE.md` beschrijft het als de plek waar elke
  v1-afwijking als "tracked delta" landt, opnieuw sessiewerk; CODEOWNERS
  eist ook hier review.

`spec/ASP-TRANSLATION.md` is de aanleiding: de complementwandeling-kaart
moest de ASP-vertaalvorm daar opschrijven, en kon uit `CLAUDE.md` niet
afleiden of dat een gewone wijziging of een voorstel was. Die kaart heeft
het uiteindelijk als voorstel gemeld.

## Wat de kaart moet opleveren

Eén van twee, met de reden genoteerd:

- **Gelijktrekken.** De prozalijst neemt de `/spec/`-claim over (of
  CODEOWNERS wordt versmald tot wat het proza noemt), en de PLAN/CONSTRUCTS-
  tegenspraak wordt opgelost — die twee zijn óf sessiewerk óf beschermd,
  niet allebei.
- **Verschil motiveren.** Als de ruimere CODEOWNERS-claim bedoeld is (een
  vangnet: liever te veel review dan te weinig), dan zegt `CLAUDE.md` dat
  met zoveel woorden, inclusief hoe een sessie zich dan tot PLAN.md en
  CONSTRUCTS.md verhoudt.

## Acceptance

- De meting hierboven opnieuw uitgevoerd en in het `Result:` genoteerd (de
  lijst kan intussen gegroeid zijn — opnieuw tellen, niet overschrijven).
- Voor elk `spec/`-item op topniveau is uit **één** document af te lezen of
  een sessie het mag mergen.
- De PLAN.md- en CONSTRUCTS.md-tegenspraak is expliciet beslecht.
- Alle dertien gates groen (de kaart is tekst; gate 1 draait over
  `CLAUDE.md` en `WAY-OF-WORKING.md`).

May touch: `CLAUDE.md` en `.github/` — **beide beschermd, dus uitsluitend
als voorstel**: de branch blijft ongemerged tot de maintainer hem beoordeelt.
`backlog/`.

## Out of scope

- De inhoud van de beschermde documenten zelf; deze kaart gaat alleen over
  welke lijst welk pad claimt.
- Branch protection aan- of uitzetten (dat vraagt een publieke repo op het
  gratis org-plan — `CLAUDE.md` noteert dat al) en PLAN §9 beslissing 10,
  de publieke flip.

---

## Result (2026-09-10)

**Gekozen: "verschil motiveren", niet gelijktrekken.** De ruimere
`/spec/`-claim van `.github/CODEOWNERS` is bedoeld als vangnet — liever te
veel review dan te weinig — en blijft daarom staan. `CLAUDE.md` zegt dat nu
met zoveel woorden en maakt de status per `spec/`-item uit dat ene document
afleesbaar. Vastlegging van de gevestigde praktijk, geen nieuwe regel — dat
geldt voor het vangnet en voor de drie voorstel-plichtige items. Voor
`PLAN.md` en `CONSTRUCTS.md` geldt het pas in de vorm die de fixronde
hieronder erin heeft gezet: het register is werk van de HOOFDsessie, niet
van de kaartsessie.

### Wat gewijzigd is

- `CLAUDE.md`, sectie "Protected paths — propose, never merge": één alinea
  plus twee bullets, direct na de `Mechanism:`-alinea. Die zegt (a) dat
  CODEOWNERS `/spec/` in zijn geheel claimt, bewust breder dan de lijst;
  (b) dat `spec/PLAN.md` en `spec/CONSTRUCTS.md` binnen die claim het
  boekhoudkanaal van het kaartprotocol zijn dat de HOOFDsessie bijhoudt — de
  CONSTRUCTS-delta's, en PLAN bij scopewijziging, landen serieel bij de
  kaartafronding (met de merge of in een commit direct erna), zonder
  afzonderlijke review vooraf; een kaartsessie raakt ze niet tenzij haar
  kaart dat expliciet zegt, en zet voorgestelde rijtekst in haar
  `Result:`-notitie; (c) dat al het overige onder `spec/` — vandaag
  `ASP-TRANSLATION.md`, `IR-REVIEW.md`, `proposals/` — datzelfde vangnet
  volgt: wijziging is voorstel, tenzij de maintainer er mandaat voor geeft,
  normaal in de kaart.
- `CLAUDE.md`, de opsomming van beschermde paden: "(once it exists)" achter
  `spec/verifier/` geschrapt. De verifier bestaat en gate 3 bouwt hem; dit
  is de kaart die over de juistheid van die lijst gaat. Verder is aan de
  opsomming niets gewijzigd.
- `CLAUDE.md`, sectie "The repository is the state": de bronzin van de
  tegenspraak bijgewerkt. Een sessie sluit haar taak af, en een
  scopewijziging bereikt `spec/PLAN.md` via de seriële boekhouding van de
  hoofdsessie. Die zin droeg sessies eerder op PLAN zelf bij te werken en
  was daarmee de scherpste helft van de tegenspraak die deze kaart aanwees.
- `.github/CODEOWNERS`: **bewust onaangeraakt.** Versmallen tot de vier
  prozalijst-items zou de dekking verkleinen op het moment dat handhaving
  nog niet eens actief is (branch protection vraagt een publieke repo op het
  gratis org-plan). Het vangnet kost niets zolang het niet handhaaft, en
  dekt bij het aanzetten meer dan het proza opsomt.
- `backlog/`: deze kaart, claim en afronding.

### De hermeting (2026-09-10)

`ls -1a spec/` en `git ls-files spec/ | cut -d/ -f2 | sort -u` geven
hetzelfde antwoord: **negen items op topniveau**, precies dezelfde negen als
bij de meting van 2026-09-09. De lijst is dus niet gegroeid. Geen verborgen
bestanden, geen ongetrackte items.

| `spec/`-item | in prozalijst | wie wijzigt het, en hoe |
| --- | --- | --- |
| `IR-SPEC.md` | ja | kaartsessie stelt voor; de maintainer merget |
| `RUNTIME-SEMANTICS.md` | ja | kaartsessie stelt voor; de maintainer merget |
| `schema/` | ja | kaartsessie stelt voor; de maintainer merget |
| `verifier/` | ja | kaartsessie stelt voor; de maintainer merget |
| `PLAN.md` | nee | HOOFDsessie, serieel bij de kaartafronding, alleen bij scopewijziging; de kaartsessie raakt het niet |
| `CONSTRUCTS.md` | nee | HOOFDsessie, serieel bij de kaartafronding, de tracked delta's; de kaartsessie raakt het niet |
| `ASP-TRANSLATION.md` | nee | voorstel via het vangnet, tenzij de kaart mandaat geeft |
| `IR-REVIEW.md` | nee | voorstel via het vangnet, tenzij de kaart mandaat geeft |
| `proposals/` (9 documenten) | nee | voorstel via het vangnet, tenzij de kaart mandaat geeft |

Vier in de prozalijst, vijf erbuiten maar binnen de `/spec/`-claim —
onveranderd t.o.v. 2026-09-09. Van die vijf gaan er twee naar het
registerkanaal van de hoofdsessie en drie naar het vangnet.

### De beslechting van de tegenspraak

`spec/PLAN.md` en `spec/CONSTRUCTS.md` zijn **het register, en de hoofdsessie
houdt het bij**. De CONSTRUCTS-delta's en, bij scopewijziging, PLAN landen
serieel bij de kaartafronding — met de merge of in een commit direct erna —
en er komt geen afzonderlijke review vooraf. Een kaartsessie raakt ze niet
tenzij haar kaart dat expliciet zegt; zij levert voorgestelde rijtekst in
haar `Result:`. Dat is precies wat `WAY-OF-WORKING.md` §1 vraagt: parallelle
kaarten mogen geen registerbestand delen waaraan ze allebei aanvullen. De
CODEOWNERS-claim blijft er als vangnet overheen liggen voor het geval branch
protection aangaat; de seriële hoofdsessie-boekhouding is dan het kanaal
waarlangs die twee bestanden landen. De rest van `spec/` buiten de vier
genoemde items (`ASP-TRANSLATION.md`, `IR-REVIEW.md`, `proposals/`) is
voorstel-plichtig; de aanleiding van deze kaart — de complementwandeling die
niet kon afleiden onder welk regime `ASP-TRANSLATION.md` viel, en het
uiteindelijk als voorstel meldde — is daarmee beantwoord: die melding was
juist.

### Gates

Gate 1 (`bash scripts/check-self-contained.sh`) op de kop van deze branch:
`check-self-contained: PASS`, exit 0. De overige twaalf gates zijn door deze
sessie **niet** gedraaid; de wijziging is tekst in `CLAUDE.md` en `backlog/`
en raakt geen zone die zij lezen. De hoofdsessie draait de volledige dertien
vóór de merge — dat is waar de acceptance van deze kaart op afgetekend
wordt, niet hier.

**De acceptance-aanname van de kaart klopt niet, en dat is nagemeten.** De
kaarttekst schrijft "gate 1 draait over `CLAUDE.md` en `WAY-OF-WORKING.md`".
Dat is onwaar: `$ZONES` in `scripts/check-self-contained.sh` is
`corpus/golden corpus/negatief corpus/rood spec/schema spec/verifier ref
backends frontends spec/IR-SPEC.md spec/RUNTIME-SEMANTICS.md` — `CLAUDE.md`
staat er niet in, en `WAY-OF-WORKING.md` §6 rekent beide juist tot de
werkproza die de privérepo's mág noemen. De PASS hierboven zegt dus alleen
dat elders niets geregresseerd is; hij toetst geen letter van de nieuwe
alinea. Er is geen gate die dat doet — deze wijziging steunt volledig op de
review van de maintainer.

### Buiten de scope gebleven

De inhoud van de beschermde documenten zelf, en branch protection /
PLAN §9 beslissing 10 — beide expliciet out of scope. Ook niet aangeraakt:
`spec/PLAN.md` en `spec/CONSTRUCTS.md` zelf; deze kaart beschrijft alleen
onder welk regime ze vallen.

### Fixronde (2026-09-10)

De eerste versie van de `CLAUDE.md`-bullet en van deze Results schreef de
PLAN/CONSTRUCTS-update aan de **kaartsessie** toe: "sessies werken ze bij als
onderdeel van het afronden van een kaart", en de tabel zei bij beide items
"ja — sessiewerk". Dat was ONWAAR als beschrijving van de gevestigde
praktijk, en het kwam uit de bouwopdracht, niet uit de repo. De praktijk is
dat de hoofdsessie het register serieel bijwerkt. Nagemeten in deze
worktree: 20 van de 230 done-kaarten noemen CONSTRUCTS samen met de
hoofdsessie of het seriële regime, waarvan 7 met de letterlijke frase
"HOOFDSESSIE serieel" (`backlog/done/20260901-let-lowering-spec-first.md`
regel 67: "HOOFDSESSIE serieel — nooit een agent"); van de laatste 40
commits op `spec/CONSTRUCTS.md` raken er 3 óók `backlog/done/`, dus 37 zijn
losse registercommits naast de kaartlanding, verscheidene met
"hoofdsessie-serieel", "maintainer-serieel" of "serial registry step at
landing" in de boodschap zelf. De reden is `WAY-OF-WORKING.md` §1: parallelle
kaarten mogen geen registerbestand delen waaraan ze allebei aanvullen —
precies wat CONSTRUCTS.md is. `CLAUDE.md` had de fout in vastgelegde vorm
gezet en de tegenspraak de verkeerde kant op beslecht. De bullet, de
tabelrijen, de beslechting en de openingsalinea zijn daarop herschreven; in
dezelfde ronde landden de bronzin in "The repository is the state" en het
schrappen van "(once it exists)".

**Beschermd pad: `CLAUDE.md`.** De branch is een VOORSTEL en blijft
ongemerged tot de maintainer hem beoordeelt.
