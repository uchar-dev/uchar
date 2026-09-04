# Set-fold "totaal van" over een tempo-eenheid: geldig, gepind, of uitgesloten?

Goal: lens 1 op de filter-LHS-kaart (2026-09-04) vond dat de enige vorm
waarin het aggregaattype en de foldbron-declaratie uiteenlopen — een
set-fold `het totaal van <€/mnd>` met §4.20-noemerstrip — nergens gepind
is, én dat de reference-interpreter die vorm niet kan evalueren
(`EVALUATION FAULT: incompatible units: EUR vs EUR`, ook zonder filter):
de frontend typeert een vorm die de oracle niet draagt, en geen gate
raakt het gat omdat het corpus de vorm niet kent. Voorvraag aan de
maintainer (genummerd, vóór het bouwen): is een set-fold `totaal van`
over een tempo-eenheid überhaupt een geldige vorm, of is `som van` de
set-fold en `totaal van` uitsluitend de temporele integratie? De
grammatica laat hem toe en de frontend typeert hem volwaardig
(noemerstrip onvoorwaardelijk).

Aanpak per antwoord: (geldig) → ref leert de vorm dragen (de
noemerstrip-integratie voor set-folds), daarna een caseloze golden
`jutterswet-tempototaal` (bron.rgs + ir.json, geen case zolang niet alle
executors dragen) die op de wire pint dat het functie-type de noemer
strippt terwijl de filter-LHS hem houdt — gate 10 wordt dan de wacht op
de emitterkeuze; (ongeldig) → de frontend weigert de vorm expliciet en
een negatief pint de weigering, waarmee de flank onbereikbaar wordt in
geldige bron.

Acceptance:
- De genummerde voorvraag gesteld en beantwoord.
- Het gekozen spoor gebouwd, rood-eerst waar een pin bijkomt.
- Alle gates groen; herpin (frontend of ref geraakt).

May touch: afhankelijk van het antwoord — `ref/**` óf
`frontends/regelspraak/src/**`, plus `corpus/golden/**` of
`corpus/negatief/**`, `backlog/`; de keuze legt de kaartwerker vast bij
de claim, na de voorvraag.

Out of scope: de Drools- en clingo-kant (volgen de bestaande gates), de
verifier.
