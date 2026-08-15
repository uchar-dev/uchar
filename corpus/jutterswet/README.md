# De Jutterswet — het casusmateriaal van het uchar-corpus

Op het eiland Scholde geldt de Wet op de jutterspenning: de **Jutterswet**.
Scholde en zijn inwoners zijn verzonnen — eigen werk van de maintainer,
afkomstig uit het *calculemus*-cursusmateriaal — en de casus is daarmee
rechtenvrij per constructie. De valkuilen zijn dat niet: de Jutterswet
spiegelt artikel voor artikel het British Nationality Act-experiment (Sergot,
Sadri, Kowalski, Kriwaczek, Hammond & Cory, "The British Nationality Act as a
Logic Program", *CACM* 29(5), 1986). Die afstamming geeft het corpus
vergelijkingsmateriaal in vier decennia logisch-programmeren- en
ASP-literatuur — precies wat de differential square van Phase 3 nodig heeft.

## De wet (v0)

1. **Art. 1 (verkrijging).** Erkend jutter is degene die na inwerkingtreding
   van deze wet op Scholde geboren is uit ten minste één ouder die op dat
   tijdstip erkend jutter of vast ingezetene van Scholde is.
2. **Art. 2 (strandvondeling).** Wie als pasgeborene aangetroffen wordt op
   het strand van Scholde, wordt voor de toepassing van artikel 1 geacht
   daaraan te voldoen, zolang het tegendeel niet blijkt.
3. **Art. 3 (penningrecht).** Recht op de jutterspenning heeft de erkende
   jutter van onbesproken juttersgedrag die in het penningjaar ten minste
   twaalf vondsten bij de strandvoogd heeft aangemeld.
4. **Art. 4 (uitsluiting).** Geen recht op de penning heeft degene aan wie
   krachtens de Strandvonderijverordening 1837 een raapvergunning is
   geweigerd.
5. **Art. 5 (hardheid).** De strandvoogd kan in bijzondere gevallen
   artikel 3 toepassen als ware "twaalf" vervangen door "zes".

**De dossiers** (de kiem van de scenariotoetsen):

- **Ade** — geboren op Scholde na inwerkingtreding; moeder vast ingezetene.
- **Brecht** — dertien vondsten in het penningjaar aangemeld; één anonieme
  klacht over wrakroof.
- **Norre** — als pasgeborene aangetroffen op het strand; ouders onbekend.

De cursus laat de wet evolueren: v1 (vondeling-reparatie), v2 (gelaagde
uitzonderingen rond art. 4, in gecompileerde én gelaagde representatie), v3
(temporalisering — verwerft/bezit/verliest — plus een herzieningsartikel).
Die versiegeschiedenis is corpusmateriaal voor regelversies, geldigheid en
amendementen.

## Semantische dekking

Wat de wet v0–v3 al van nature toetst: defeasible defaults (art. 2 "zolang
het tegendeel niet blijkt"), uitzonderingsladders (art. 4/5), de twee soorten
negatie, discretionaire begrippen als eerlijke `Onopgelost`-grens
("onbesproken juttersgedrag", "bijzondere gevallen"), verwijzing naar een
andere regeling (de Strandvonderijverordening 1837), en tijd (v3).

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

## Tweede casus: een echte regeling

Naast de Jutterswet komt één op werkelijke wetgeving gebaseerde casus als
realismetoets (wettekst is auteursrechtvrij, art. 11 Aw); kandidaat: de
zorgtoeslag — compact, rekenintensief, met echte jaarlijkse
parameterwijzigingen voor de vintage-machinerie. Eigen map zodra het
Phase-1-corpuswerk start.
