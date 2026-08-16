# De Jutterswet — het casusmateriaal van het uchar-corpus

Op het eiland Scholde geldt de Wet op de jutterspenning: de **Jutterswet**.
Scholde en zijn inwoners zijn verzonnen — eigen werk van de maintainer — en
de casus is daarmee rechtenvrij per constructie. De valkuilen zijn dat
niet: de Jutterswet spiegelt artikel voor artikel het British Nationality
Act-experiment (Sergot, Sadri, Kowalski, Kriwaczek, Hammond & Cory, "The
British Nationality Act as a Logic Program", *CACM* 29(5), 1986). Die
afstamming geeft het corpus vergelijkingsmateriaal in vier decennia
logisch-programmeren- en ASP-literatuur — precies wat de differential
square van Phase 3 nodig heeft.

## Bestanden

| Bestand | Inhoud |
|---|---|
| `wet-v0.md` | De oorspronkelijke wet (vijf artikelen; in werking 1 januari 2020) |
| `wet-v2.md` | Geconsolideerde tekst na de Eerste wijzigingsverordening (art. 4 gelaagd + art. 4bis; 1 januari 2022) |
| `wet-v3.md` | Geconsolideerde tekst na de Tweede wijzigingsverordening (art. 6 afstand; 1 januari 2024) |
| `wet-v4.md` | Geconsolideerde tekst na de Derde wijzigingsverordening (de kwantitatieve uitbreiding: art. 3a–3c, 7–9, vondstdrempel; 1 januari 2025) |
| `versiegeschiedenis.md` | De versies v0–v4 met data en leeswijzers — waaronder v1, de vondeling-lezing die de wetstekst niet wijzigt, en de vaststellingsbesluiten onder art. 7 |
| `dossiers.md` | Negen dossiers: feiten, rechtsvraag, bedoelde uitkomst per versie, valkuilen |

De versiegeschiedenis is zelf corpusmateriaal voor regelversies, geldigheid
en amendementen; de data zijn door dit corpus vastgelegd.

## Semantische dekking

Wat de wet v0–v3 al van nature toetst: defeasible defaults (art. 2 "zolang
het tegendeel niet blijkt"), uitzonderingsladders (art. 4 gelaagd, art.
4bis, art. 5), de twee soorten negatie en de open/gesloten-wereldkeuze per
predicaat (de v1-lezing), discretionaire en open begrippen als eerlijke
`Onopgelost`-grens ("onbesproken juttersgedrag", "bijzondere gevallen", in
drie gradaties geijkt door Feiko/Gesa/Okko), verwijzing naar een andere
regeling (de Strandvonderijverordening 1837), temporalisering met
verwervings- en verliesgronden, persistentie en peildatum-aannames (v3), en
een bewust opengelaten leemte (herziening van een art. 2-erkenning — het
dossier "Norre, twee jaar later").

De kwantitatieve uitbreiding is sinds v4 (`wet-v4.md`) wetstekst; de
routekaart hieronder wijst per constructie de dragende artikelen aan:

| Constructie | Jutterswet-artikel(en) |
|---|---|
| Parameters + vintages (fail-fast) | art. 7 (vondstdrempel en penningbedrag, jaarlijks; geen vervangende waarde) + de vaststellingsbesluiten in `versiegeschiedenis.md` |
| Aggregaties, feittypen/rollen | art. 3a–3b (aanmelding bij de strandvoogd; de telling over het penningjaar) |
| Verdeling (alle modaliteiten) | art. 9 (naar rato van gewicht, per-berger-maximum, afronding naar beneden, rest naar het eilandfonds; gelijke delen als terugval) |
| Eenheden | art. 3a (kilogrammen), art. 7–9 (euro's) |
| Tijdlijnen | art. 3b (het penningjaar als kalenderjaar); variabel startpunt (erkenningsdatum, vast ingezetenschap) blijft v3-lezingsmateriaal |
| Dagsoorten | art. 3b–3c (stormvloeddag telt dubbel) |
| Uniciteit, consistentie | art. 3a (vondstnummer eenmalig per penningjaar; aanmelding in het penningjaar van de vondstdag) |
| Object-/feitcreatie | art. 8 (het penningbesluit), art. 3a (de aanmelding) |
| Beslistabellen | art. 5 nieuwe stijl (de halve vondstdrempel) |
| Terugwerkende kracht / kennistijd | nog niet gedekt — een later raadsbesluit met terugwerking volgt met de kennistijd-machinerie |

## Tweede casus: een echte regeling

Naast de Jutterswet komt één op werkelijke wetgeving gebaseerde casus als
realismetoets (wettekst is auteursrechtvrij, art. 11 Aw); kandidaat: de
zorgtoeslag — compact, rekenintensief, met echte jaarlijkse
parameterwijzigingen voor de vintage-machinerie. Eigen map zodra het
Phase-1-corpuswerk start.
