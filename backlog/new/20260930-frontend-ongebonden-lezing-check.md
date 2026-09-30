# frontend-ongebonden-lezing-check: een lezing die niet bindt passeert `check` als ongedeclareerd attribuut

*Vondst van een regelsetbouwer, 2026-09-30 (skelet-vervolg, waarneming G2
in zijn Result), bevestigd door de hoofdsessie op uchar `b0aab696` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/check-ongebonden/` (a-subtype-lid.rgs, b-ongedeclareerd.rgs,
scenario.rgs, cases-*/; alleen lezen). Stil in de frontend (`check` exit 0,
IR met lege diagnostics), luid pas in de uitvoerders: de exec-tak crasht
met "geen getter", ref en clingo falen benoemd. Op verzoek van de eigenaar
geschreven (2026-09-30).*

## Aanleiding

Een genavigeerde lezing waarvan het lid niet bestaat op de rolvuller — "de
toeslag van de jutter van de Vondst", waar `toeslag` alleen op het subtype
Strandjutter staat, of "de rang van de jutter van de Vondst", waar `rang`
nergens staat — wordt door de resolver niet als fout gezien maar herlezen
als een attribuut met de naam `toeslag van de jutter` van de Vondst zelf.
Dat attribuut is nergens gedeclareerd; de frontend schrijft het toch in het
IR, zonder WARNING of ERROR. `check` zegt "resolved=1" en zwijgt. Ref en
clingo weigeren het document dan benoemd ("'toeslag van de jutter' is not a
declared attribute of 'Vondst': a value position reads an attribute …"); de
Drools-exec-tak crasht ("java.lang.IllegalStateException: geen getter voor
'toeslag van de jutter' op … Vondst"). DIVERGENCES ("An undeclared
attribute read — refused for one shape") laat zo'n lezing bewust aan de
verifier; maar de poort `check` van de distributie draait de verifier niet,
en een regelsetbouwer die op `check` vertrouwt, ziet de fout pas bij het
scenario, met een crash in plaats van een zin. Namen met spaties en
koppelwoorden ("maxdatumverschuiving van de lopende periode") zijn in het
dialect gewoon, dus de herlezing "toeslag van de jutter" is een echte
dubbelzinnigheid van de grammatica, niet een tikfout die de lexer al vangt.

## Minimale reproductie

`b0aab696`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de premie Numeriek (geheel getal);
    Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
        de toeslag Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        de bonus Numeriek (geheel getal);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel bonus
        geldig altijd
            De bonus van een Vondst moet berekend worden als de toeslag van de jutter van de Vondst plus 1.

    Scenario: Jutter J (premie 5) met Vondst V; verwacht: de bonus van V is leeg (de verwachting dient
    alleen om de uitvoerders te laten spreken).

    R1  a-subtype-lid.rgs, zoals hierboven (het lid bestaat op een subtype van de rolvuller).
        nu: check exit 0, geen ERROR of WARNING, "regels=1 resolved=1"; IR: `"attribute": "toeslag van
        de jutter"` op Vondst, diagnostics leeg; scenario FAIL met "java.lang.IllegalStateException:
        geen getter voor 'toeslag van de jutter' op dev.uchar.regelspraak.facts.Vondst"; ref
        "EVALUATION FAULT: \\"toeslag van de jutter\\" is not a declared attribute of \\"Vondst\\": a
        value position reads an attribute …"; clingo "TRANSLATION FAULT: …" met dezelfde zin.
    R2  b-ongedeclareerd.rgs: `de rang van de jutter van de Vondst`, `rang` bestaat nergens.
        nu: identiek: check exit 0, IR met `"attribute": "rang van de jutter"`, scenario "geen getter",
        ref en clingo benoemd.

## Acceptatie

- (a) Meet de vormruimte: het lid op een subtype van de rolvuller, nergens,
  en op een ander objecttype dan de rolvuller; een verkeerde rolnaam; de
  lezing in een waarde, in een voorwaarde, in een rechtvaardiging van een
  default en in de subselectie van een vouw; één en twee rollen ver; en de
  bindende lezing als de vorm die moet blijven spelen.
- (b) De resolver weigert bij `check` benoemd, in de woorden van de auteur:
  welk lid niet bestaat op welke rolvuller, en waar het wel bestaat (het
  subtype, WF-053) als dat zo is; geen herlezing als attribuut van het
  onderwerp tenzij dat attribuut gedeclareerd is. Rood-eerst JUnit-getuige
  met R1 en R2. Blijft de herlezing ergens nodig (een gedeclareerd attribuut
  met een koppelwoord in zijn naam), dan wint de declaratie en zegt het
  rapport dat.
- (c) Negatief-cases in het corpus (werknaam `jutterswet-ongebonden-lezing`)
  voor R1 en R2 met `expect.txt`, zodat gate 4 de weigering pint; de pins
  van gate 4 en 10 als **voorstel** waar zij verschuiven.
- (d) Corpussweep ongewijzigd; alle zestien gates groen (bash waar de
  shebang dat zegt). DIVERGENCES: de sectie over de ongedeclareerde lezing
  zegt wat de frontend nu weigert en wat aan de verifier blijft. Noot voor de
  distributie (buiten deze kaart): `bin/regelspraak check` zou als tweede
  lijn de verifier kunnen draaien.

May touch: `frontends/regelspraak/**`, `corpus/negatief/<nieuw>/`,
`scripts/check-*.sh` (pins, **voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/**`, `spec/`. NB: niet nodig vóór de regelset verder gaat (alle
regels van de regelset binden); wel vóór een volgende bouwer erop vertrouwt
dat `check` een lezing zonder lid vangt.
