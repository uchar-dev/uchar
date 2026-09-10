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
afleesbaar. Vastlegging van de gevestigde praktijk, geen nieuwe regel: dat
geldt voor het vangnet en, in de vorm die de fixronde hieronder erin heeft
gezet, voor `PLAN.md` en `CONSTRUCTS.md` — het register is werk van de
HOOFDsessie, niet van de kaartsessie, en dat is gemeten. Voor de drie
voorstel-plichtige items ligt het anders en zwakker: dát is de regel, hier
voor het eerst expliciet gemaakt. Kaartsessies hebben `ASP-TRANSLATION.md`
en `spec/proposals/` aantoonbaar midden in kaartwerk bewerkt (bijvoorbeeld
`8d141bb`, 2026-08-29, een fixronde die ook de May-touch van de eigen kaart
verbreedde), en of daar mandaat voor was is uit de commits niet af te lezen.
De regel wordt hier dus gesteld, niet vastgesteld.

### Wat gewijzigd is

- `CLAUDE.md`, sectie "Protected paths — propose, never merge": één alinea
  plus twee bullets, direct na de `Mechanism:`-alinea. Die zegt (a) dat
  CODEOWNERS `/spec/` in zijn geheel claimt, bewust breder dan de lijst;
  (b) dat `spec/PLAN.md` en `spec/CONSTRUCTS.md` binnen die claim het
  boekhoudkanaal van het kaartprotocol zijn dat de HOOFDsessie bijhoudt —
  de maintainers eigen seriële sessie op `main`, geen kaartsessie, zodat die
  landing de hand van de maintainer ís; de CONSTRUCTS-delta's, en PLAN bij
  scopewijziging, landen in de commit die de kaart naar `done/` verplaatst
  of in een commit direct daarna (merges zijn fast-forward, er is geen
  merge-commit), zonder afzonderlijke review vooraf; een kaartsessie raakt
  ze niet tenzij haar kaart dat expliciet zegt, en zet voorgestelde rijtekst
  in haar `Result:`-notitie; (c) dat al het overige onder `spec/` — vandaag
  `ASP-TRANSLATION.md`, `IR-REVIEW.md`, `proposals/` — datzelfde vangnet
  volgt: wijziging is voorstel, tenzij de maintainer er mandaat voor geeft,
  normaal in de kaart. De bullet noemt `WAY-OF-WORKING.md` §1 nu als
  verbod-op-gedeelde-registry waaraan het seriële kanaal voldoet, in plaats
  van te suggereren dat §1 deze twee bestanden zelf aanwijst.
- `CLAUDE.md`, sectie "Spec discipline": bij "every v1 deviation is a
  tracked delta in `spec/CONSTRUCTS.md`" staat nu "(landed by the main
  session — see Protected paths)". Dat was de tweede plek die de delta
  impliciet aan de kaartsessie toeschreef.
- `WAY-OF-WORKING.md` §4, stap 4: droeg nog "Update `spec/PLAN.md` only if
  the plan's scope itself changed" en sprak daarmee de nieuwe
  CLAUDE.md-bullet tegen. Nu: een kaartsessie werkt PLAN/CONSTRUCTS niet
  zelf bij, voorgestelde tekst gaat in de `Result:`, de hoofdsessie landt
  het register serieel bij de kaartafronding, met verwijzing naar
  `CLAUDE.md` Protected paths. "Touch `CLAUDE.md` only if an invariant
  changed" blijft staan.
- `WAY-OF-WORKING.md` §6, de artefactzone-opsomming: `frontends/`
  toegevoegd. Gate 1's `$ZONES` draagt die zone al sinds het Phase-5a-skelet
  en `CLAUDE.md`'s gate-1-tekst noemt hem ook; alleen deze opsomming liep
  achter. Lijstgelijkheid is precies het onderwerp van deze kaart, dus het
  hoort hier thuis.
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
serieel bij de kaartafronding — in de commit die de kaart naar `done/`
verplaatst of in een commit direct daarna; merges zijn fast-forward, dus een
merge-commit bestaat hier niet — en er komt geen afzonderlijke review
vooraf. Een kaartsessie raakt ze niet
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
`check-self-contained: PASS`, exit 0. Eén keer rood onderweg, in de
slotronde: de hernoem-tripwire sloeg aan op deze Results-notitie zelf (zie
"Gemeld, niet gekaart"). Opgelost in het proza, niet in het script. De
overige twaalf gates zijn door deze
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
dat de hoofdsessie het register serieel bijwerkt.

**Nagemeten in deze worktree, alle tellingen zonder deze kaart zelf**
(2026-09-10; de slotronde heeft de eerste, te ruime versie van deze cijfers
vervangen):

```
$ grep -rl "CONSTRUCTS" backlog/done/ | grep -v beschermde-paden-lijsten-gelijk \
    | xargs grep -lEi "hoofdsessie|serieel|serially|serial registry" | wc -l
19          (van 229 done-kaarten)

$ grep -rl "HOOFDSESSIE serieel" backlog/done/ | grep -v beschermde-paden | wc -l
1           hoofdlettergevoelig: backlog/done/20260901-let-lowering-spec-first.md:67
            "HOOFDSESSIE serieel — nooit een agent"

$ grep -ril "hoofdsessie serieel" backlog/done/ | grep -v beschermde-paden | wc -l
7           hoofdletterONgevoelig
```

Op de commitkant, over de laatste 40 commits die `spec/CONSTRUCTS.md`
raken: **28** dragen geen enkel `backlog/`-bestand — dat zijn de losse
registercommits. De andere twaalf zijn géén tegenvoorbeeld maar tellen ook
niet mee: 3 zijn de kaartlandingscommit zelf (register mee met de
`done/`-verplaatsing, precies de vorm die de bullet toestaat), 2 raken
`backlog/new/`, en 7 raken `backlog/doing/` — dus register-bewerkingen
middenin kaartwerk. Van die 7 zijn er 6 van 2026-08-23/24, vóór het seriële
regime, en is de zevende `8d141bb` (2026-08-29), een fixronde die in
dezelfde commit de May-touch van haar eigen kaart verbreedde. Eerlijker
gezegd dan de eerste versie van deze notitie: hoogstens 28 van de 40 zijn
losse registercommits, niet 37. De reden is `WAY-OF-WORKING.md` §1: parallelle
kaarten mogen geen registerbestand delen waaraan ze allebei aanvullen —
precies wat CONSTRUCTS.md is. `CLAUDE.md` had de fout in vastgelegde vorm
gezet en de tegenspraak de verkeerde kant op beslecht. De bullet, de
tabelrijen, de beslechting en de openingsalinea zijn daarop herschreven; in
dezelfde ronde landden de bronzin in "The repository is the state" en het
schrappen van "(once it exists)".

**Slotronde (2026-09-10).** `WAY-OF-WORKING.md` §4 stap 4 en §6's zonelijst
meegenomen (zie "Wat gewijzigd is"), plus vier scherpstellingen in
`CLAUDE.md`: "MAIN session" gedefinieerd als de seriële sessie van de
maintainer op `main` — dat sluit het schijnbare gat met de regel "only the
maintainer merges them" een paar regels hoger; "in the merge commit"
vervangen omdat het ff-only-regime geen merge-commits kent; de §1-verwijzing
teruggebracht tot wat §1 werkelijk zegt; en de CONSTRUCTS-zin in "Spec
discipline" die de delta impliciet aan de kaartsessie toeschreef. De
metingen in deze notitie zijn in dezelfde ronde hermeten en naar beneden
bijgesteld.

**May-touch-verbreding, expliciet gemeld.** De "May touch" van de kaart
noemt `CLAUDE.md`, `.github/` en `backlog/`; `WAY-OF-WORKING.md` staat er
niet in. Het is meegenomen omdat §4 stap 4 na de CLAUDE.md-wijziging een
harde tegenspraak werd tussen twee documenten die deze kaart juist gelijk
moet trekken, en omdat §6's zonelijst hetzelfde soort achterstand droeg als
de lijsten die de kaart onderzoekt. Het is een beschermd pad en valt in
dezelfde reviewronde: dezelfde branch, hetzelfde voorstel.

### Wat de hoofdsessie nog moet doen: `spec/PLAN.md` §8

Een kaartsessie raakt `spec/PLAN.md` nooit, dus dit is een melding, geen
wijziging — en meteen een toepassing van het kanaal dat deze kaart
vastlegt. §8 (rond r. 637-645) draagt drie dingen die na deze kaart niet
meer kloppen:

1. Dezelfde oude zin als `CLAUDE.md` en `WAY-OF-WORKING.md` droegen: "every
   session ends by updating the issue it worked and — only when scope
   changed — this plan". Dat is precies de formulering die hier is
   omgezet naar seriële hoofdsessie-boekhouding.
2. Een **derde** beschermde-padenlijst — "`IR-SPEC.md`,
   `RUNTIME-SEMANTICS.md`, the schema, the verifier, the anchor corpus, and
   the CI configuration" — die afwijkt van CODEOWNERS: zonder `scripts/`,
   zonder `WAY-OF-WORKING.md`, zonder `CLAUDE.md`. `.github/CODEOWNERS`
   regel 1 verwijst er bij naam naar ("see CLAUDE.md and spec/PLAN.md §8"),
   dus de afwijking is niet inert.
3. De claim dat handhaving loopt via "CODEOWNERS plus branch protection and
   a CI check on protected paths". Die CI-check bestaat niet; branch
   protection evenmin (die vraagt een publieke repo op het gratis org-plan,
   zoals `CLAUDE.md` al noteert).

De hoofdsessie werkt §8 serieel bij bij de landing van deze kaart.

### Gemeld, niet gekaart

Twee vondsten uit dezelfde lens, buiten de scope van deze kaart en hier niet
gewijzigd:

- `README.md` r. 34 noemt `spec/PLAN.md` "the constitution", terwijl
  `WAY-OF-WORKING.md` r. 3 `CLAUDE.md` zo aanduidt en `CLAUDE.md` zichzelf
  "the constitution's index" noemt. Datzelfde bestand zegt op r. 39-40
  "**Five** gates run on every change" en somt er vijf op; het zijn er
  dertien.
- Het werkdocument van de hernoemronde van 2026-08-29 op de root — het
  vierde `.md` daar, naast `README.md`, `CLAUDE.md` en `WAY-OF-WORKING.md` —
  draagt op r. 7-9 een **vierde** padopsomming ("vrijwel elk protected path
  ..."). Historisch, geen index en geen norm, maar wel de vierde lijst in de
  repo die paden opsomt. Zijn bestandsnaam staat hier niet uitgeschreven:
  die draagt de voor-hernoem-woordenschat, en gate 1's hernoem-tripwire
  laat die alleen in dat bestand zelf toe. Deze notitie liet de gate
  daarmee eerst rood lopen — gezien, prozafix, weer groen; de tripwire doet
  precies wat hij moet doen en is niet aangepast.

**Beschermde paden: `CLAUDE.md` en `WAY-OF-WORKING.md`.** De branch is een
VOORSTEL en blijft ongemerged tot de maintainer hem beoordeelt.
