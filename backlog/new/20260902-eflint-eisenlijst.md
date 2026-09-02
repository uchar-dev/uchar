# eFLINT als tweede staande eisenlijst (R0 uit de flint-verkenning)

Goal: De flint-verkenning (2026-09-02, RAPPORT.md in de verkenningsmap
naast de repo) conserveren in de spec op de manier waarop PLAN §3
LegalRuleML draagt: eFLINT als tweede staande eisenlijst, met de vier
bevindingen die de spec raken en anders met de sessie verdwijnen:
(1) schending is in elke uitvoerbare deontische taal een AFGELEID,
observeerbaar predicaat op een toestand — geen modaliteit (bevestigt de
§9.5-kanaaldiscipline als de juiste vorm); (2) bevoegdheid is
act-enabledness, geen eigen positiesoort — twee onafhankelijke
implementaties kozen zo (versmalt rij 32: de op te slaan soorten zijn
plicht en handeling); (3) de leidende deontische normtaal heeft GEEN
regelvoorrang — uchar's prioritizedDefaults is daar strikt sterker, en
dat verschil is een waarschuwing voor elke toekomstige frontend die
overlappende afleidingsregels stilzwijgend op prioritizedDefaults afbeeldt
(betekenisverandering, gemeten); (4) eFLINT-plichten zijn routinematig
ternair (houder, wederpartij, betrekking) waar WF-047 het feittype op
exact twee rollen pint — de ariteitsvraag hoort bij elk rij-32-besluit.

Acceptance:
- Eén prozawijziging in `spec/PLAN.md` §3 (een eFLINT-rij in de
  borrow-tabel, naast LegalRuleML, met de staande toets) en één
  aanvulling op `spec/CONSTRUCTS.md` rij 32 (de vier bevindingen als
  genummerde ontwerprandvoorwaarden, elk één zin).
- Geen normatieve wijziging, geen WF-regel, geen corpuswijziging; gates
  onaangeroerd groen (gate 1 blijft de enige die deze bestanden leest).
- Beide bestanden zijn protected paths en hoofdsessie-serieel: de kaart
  wordt door de hoofdsessie zelf uitgevoerd (geen agent) en de branch
  blijft zonodig als proposal staan.

May touch: `spec/PLAN.md` (proposal), `spec/CONSTRUCTS.md` (proposal,
hoofdsessie), `backlog/`.

Out of scope: elke IR-/schema-/verifier-wijziging; rij 32 openen (dat is
R2 en wacht op het meldplichtspoor-bewijs); alles uit RAPPORT.md dat de
spec niet raakt.
