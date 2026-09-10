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
afleesbaar. Dit is vastlegging van de gevestigde praktijk, geen nieuwe regel.

### Wat gewijzigd is

- `CLAUDE.md`, sectie "Protected paths — propose, never merge": één alinea
  plus twee bullets, direct na de `Mechanism:`-alinea. De bestaande lijst
  zelf is ONGEWIJZIGD. De toevoeging zegt (a) dat CODEOWNERS `/spec/` in
  zijn geheel claimt, bewust breder dan de lijst; (b) dat `spec/PLAN.md` en
  `spec/CONSTRUCTS.md` binnen die claim het boekhoudkanaal van het
  kaartprotocol zijn — sessies werken ze bij als onderdeel van het afronden
  van een kaart, en die update landt met de kaartmerge onder het mandaat dat
  de maintainer de sessie gaf, zonder afzonderlijke review vooraf; (c) dat
  de overige `spec/`-items buiten de lijst het vangnet volgen: wijziging is
  voorstel, tenzij de kaart er expliciet mandaat voor geeft.
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

| `spec/`-item | in prozalijst | mag een sessie het mergen? |
| --- | --- | --- |
| `IR-SPEC.md` | ja | nee — voorstel |
| `RUNTIME-SEMANTICS.md` | ja | nee — voorstel |
| `schema/` | ja | nee — voorstel |
| `verifier/` | ja | nee — voorstel |
| `PLAN.md` | nee | ja — sessiewerk binnen het kaartprotocol (alleen bij scopewijziging) |
| `CONSTRUCTS.md` | nee | ja — sessiewerk binnen het kaartprotocol (tracked delta's) |
| `ASP-TRANSLATION.md` | nee | nee — voorstel via het vangnet |
| `IR-REVIEW.md` | nee | nee — voorstel via het vangnet |
| `proposals/` (9 documenten) | nee | nee — voorstel via het vangnet |

Vier in de prozalijst, vijf erbuiten maar binnen de `/spec/`-claim —
onveranderd t.o.v. 2026-09-09. Van die vijf gaan er twee naar het
sessiekanaal en drie naar het vangnet.

### De beslechting van de tegenspraak

`spec/PLAN.md` en `spec/CONSTRUCTS.md` zijn **sessiewerk binnen het
kaartprotocol**. `CLAUDE.md` droeg sessies al op ze bij te werken bij het
afsluiten van een kaart; die opdracht is het mandaat, en de kaartmerge is
het reviewmoment. Er komt dus geen afzonderlijke review vooraf, en de
CODEOWNERS-claim blijft er als vangnet overheen liggen voor het geval
branch protection ooit aangaat — dan is een expliciet mandaat in de kaart
wat de sessie ervoor nodig heeft. De rest van `spec/` buiten de vier
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

### Buiten de scope gebleven

De inhoud van de beschermde documenten zelf, en branch protection /
PLAN §9 beslissing 10 — beide expliciet out of scope. Ook niet aangeraakt:
`spec/PLAN.md` en `spec/CONSTRUCTS.md` zelf; deze kaart beschrijft alleen
onder welk regime ze vallen.

**Beschermd pad: `CLAUDE.md`.** De branch is een VOORSTEL en blijft
ongemerged tot de maintainer hem beoordeelt.
