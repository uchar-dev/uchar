# Subtypering: ontwerponderzoek — semantiek, use case en het defaults×hiërarchie-besluit

Goal: het beslisdocument `spec/proposals/SUBTYPING.md` dat de kernelkaart kan uitvoeren zonder open ontwerpvragen. Het beantwoordt genummerd: (1) de IR-vorm van de supertype-verwijzing en de naamresolutie door de keten, inclusief het sibling-botsingsbesluit (het CGM heeft echte gevallen, zie de opdracht); (2) de precieze semantiek van opdrachtbesluit 3 — overerving als defaults-groep-erving met basis-override — met de WF-nummers die daarvoor worden gereserveerd, en met antwoord op de randen: wat als het supertype voor een target alleen uitzonderingen zonder basis draagt; mag een subtype een geërfde uitzondering verfijnen of vervangen en hoe wordt die dan geadresseerd; hoe voegen geërfde en subtype-eigen uitzonderingen zich in één stapel (ongeordend tenzij expliciet geordend, botsing als §9.5-conflictfeit) en hoe verhoudt dat zich tot de geneste groepen (`Default.excepties`) en de ch. 7.8-standing norm; (3) de Drools-loweringskeuze — native `declare … extends` tegenover vlakke emissie met discriminator — beargumenteerd vanuit de differential, niet vanuit smaak; (4) de extensionele kwantificatie- en rolcompatibiliteitssemantiek; (5) het ontwerp van de minimale jutterswet-use-case uit opdrachtdeel 1.5 (supertype-regel vuurt voor subtype-instantie; kwantificatie over het supertype telt beide); (6) de F-logic-semantiekstudie, die door besluit 3 aan gewicht wint: onze groepserving-met-basis-override ís een vorm van niet-monotone overerving, dus vergelijk haar expliciet met de inheritance-semantiek van Kifer/Lausen/Wu (JACM 1995) en met Flora-2's gedrag op de pinguïn-toets en op de botsende-uitzonderingen-rand (waar wij het §9.5-conflictfeit kiezen), en beoordeel of Flora-2 lokaal installeerbaar en bruikbaar is als externe vergelijkingsexecutor; (7) de weigeringslijsten voor kernel, dialect en F-logic-frontend, inclusief de twee met naam genoemde CGM-MI-gevallen. Werk de bestaande literatuur- en repo-feiten uit het opdrachtdocument van 2026-09-10 (opgegaan in `spec/proposals/SUBTYPING.md`) in; verzin geen nieuwe feiten over het CGM — de telling van 2026-09-10 is de bron.

Acceptance: `spec/proposals/SUBTYPING.md` bestaat met alle zeven vragen beantwoord in Decided-vorm (het stramien van de bestaande proposals); de drie vervolgkaarten zijn waar nodig aangescherpt met concrete WF-nummers en golden-namen; geen enkel ander bestand gewijzigd; een leesronde door de maintainer is de poort.

May touch: `spec/proposals/SUBTYPING.md` (nieuw), de drie kaarten `backlog/new/20260910-subtype-kernel.md`, `backlog/new/20260910-subtype-regelspraak-dialect.md`, `backlog/new/20260910-flogic-frontend-minimaal.md` (alleen aanscherping), en een eventuele Flora-2-installatieproef buiten de repo.

Out of scope: elke wijziging aan spec/IR-SPEC.md, verifier, executors, frontends of corpus; meervoudige overerving (gedocumenteerde weigering, geen ontwerp); specificiteit-als-prioriteitsas (later eigen proposal, zie opdrachtbesluit 3).

## Result

Geland als `spec/proposals/SUBTYPING.md`: de zeven vragen in
Decided-vorm, met §11 als lijst van zes punten die alleen de
maintainer kon beslissen en §12 als samenvatting "proposed, not
ratified". De drie vervolgkaarten zijn aangescherpt met de
gereserveerde WF-nummers, de golden- en casusnamen, de gemeten
loweringsplekken en de weigeringslijsten. Flora-2 2.2 beta is
buiten de repo geïnstalleerd en gemeten; de transcripten staan in §8.3.
Twee lens- plus fixrondes hebben het document nagemeten (de
enkelschrijver-premisse bleek omgekeerd te staan en is herschreven; de
naadpunten van de tweede ronde zijn in dezelfde branch verwerkt).

**De maintainer besliste op 2026-09-12 drie van de zes §11-punten, en
het document is er volledig op doorgetrokken.** Voor deze kaart telt
vooral dat besluit §11.2 de route vervangt die de Goal hierboven nog
citeert: geërfde en subtype-eigen uitzonderingen vormen géén
ongeordende groep met een §9.5-conflictfeit bij botsing, maar staan in
een vaste blokorde met de subtype-eigen uitzonderingen bovenaan — een
definitie op het kind wint altijd van een op de parent, deterministisch
en zonder conflictfeit. Het voorgestelde declaratieveld op de laaggrens
verviel daarmee, en het vrijgekomen WF-106 is herbestemd naar de
weigering "een objecttype declareert hoogstens één direct supertype"
(§11.1; wirevorm blijft één string, dus geen verifier-check en geen
negatief — de getuigen wonen bij de twee frontends). §11.3 legt de
clingo-sluiting definitief in de ASP. De Goal-tekst blijft staan zoals
zij gesteld was: dat is de vraag, niet het antwoord.

Open, en de poort voor het geheel: §11.4 (mag de F-logic-snede
verbreed worden met de gebalkte klasseniveau-vormen), §11.5 (ketendiepte
onbegrensd, alleen vastgelegd) en §11.6 (de opdrachtkopie kan niet
getrackt worden zolang gate 1's hernoem-tripwire hem rood maakt). De
leesronde door de maintainer blijft de poort voor het document als
geheel; een beslist punt ratificeert de sectie eromheen niet.
