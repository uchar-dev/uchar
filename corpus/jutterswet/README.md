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
| `versiegeschiedenis.md` | De versies v0–v3 met data en leeswijzers — waaronder v1, de vondeling-lezing die de wetstekst niet wijzigt |
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

Wat het eiland erbij krijgt om de volledige IR-oppervlakte te dekken
(Phase-1-corpuswerk):

| Te dekken constructie | Jutterswet-uitbreiding |
|---|---|
| Parameters + vintages | penningbedrag en vondstdrempel, jaarlijks herzien door de eilandraad |
| Aggregaties, feittypen/rollen | vondsten aangemeld bij de strandvoogd; "het aantal vondsten van de jutter in het penningjaar" |
| Verdeling (alle modaliteiten) | opbrengst van een gezamenlijke berging verdeeld over de bergers — naar rato, met maximum, afronding en onverdeelde rest |
| Eenheden en dimensies | vondstgewicht in kg, opbrengst in €, vondsten per kwartaal of vondstsoort |
| Tijdlijnen + variabel startpunt | vast ingezetenschap als tijdlijn; erkenningsdatum als startpunt |
| Dagsoorten | stormvloeddag (vondsten op een stormvloeddag tellen dubbel) |
| Uniciteit, consistentie | vondstnummers uniek per penningjaar; aanmelddatum binnen het penningjaar |
| Object-/feitcreatie | het penningbesluit als gecreëerd object; de aanmelding als feitcreatie |
| Beslistabellen | de hardheidstoets van art. 5 als beslistabel |
| Terugwerkende kracht / kennistijd | een later raadsbesluit dat terugwerkt tot het begin van het lopende penningjaar |

## Tweede casus: een echte regeling

Naast de Jutterswet komt één op werkelijke wetgeving gebaseerde casus als
realismetoets (wettekst is auteursrechtvrij, art. 11 Aw); kandidaat: de
zorgtoeslag — compact, rekenintensief, met echte jaarlijkse
parameterwijzigingen voor de vintage-machinerie. Eigen map zodra het
Phase-1-corpuswerk start.
