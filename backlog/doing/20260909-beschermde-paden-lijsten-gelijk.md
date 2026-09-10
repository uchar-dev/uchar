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
