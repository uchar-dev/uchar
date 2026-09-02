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

Maintainer-aanwijzing (2026-09-02): de eisenlijst blijft een EIGEN,
bronvermeldend document — geen anonieme versmelting met bestaande
spec-tekst, zodat de herkomst van elk idee traceerbaar blijft. En elke
eFLINT-les wordt vóór opname gekruist met twee onafhankelijke kaders:
Hohfeld (het bestaande TOEKOMST-verkenningsdocument in de repo-root) en
de Wetsanalyse-methode (Ausems/Bulles/Lokin 2021 — het boek is ©; het
lokale cursusmateriaal in de wetsanalyse-course-repo is read-only
leesbron; uitsluitend eigen parafrase met bronvermelding, nooit
excerpten in deze repo). Waar de drie kaders elkaar tegenspreken is dát
de bevinding — noteer de spanning, kies niet stilzwijgend.

Acceptance:
- Eén nieuw document `spec/proposals/DEONTIEK-EISENLIJST.md`: per eis
  één regel met een BRONKOLOM (eFLINT-meting / Hohfeld / LegalRuleML /
  Wetsanalyse) en per eis de kruistoets tegen de twee andere kaders
  (bevestigt / spreekt tegen / zwijgt), in eigen woorden.
- `spec/PLAN.md` §3 krijgt hooguit ÉÉN verwijzingsregel naar dat
  document (proposal); `spec/CONSTRUCTS.md` rij 32 blijft ONGEMOEID tot
  het meldplichtspoor-bewijs er ligt (dan pas is er meetmateriaal om de
  randvoorwaarden aan te ijken).
- Geen normatieve wijziging, geen WF-regel, geen corpuswijziging; gate 1
  groen (de vocabulaire-tripwire leest ook proposals en backlog).
- Protected paths en hoofdsessie-serieel: de kaart wordt door de
  hoofdsessie zelf uitgevoerd (geen agent); de branch blijft zonodig als
  proposal staan.

May touch: `spec/proposals/DEONTIEK-EISENLIJST.md` (nieuw),
`spec/PLAN.md` (proposal, één verwijzingsregel), `backlog/`.

Out of scope: `spec/CONSTRUCTS.md` (wacht op meldplichtspoor-bewijs);
elke IR-/schema-/verifier-wijziging; rij 32 openen (dat is R2); alles
uit het verkenningsrapport dat de spec niet raakt; excerpten uit het
Wetsanalyse-boek of uit eFLINT-repos.
