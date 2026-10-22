# jutterswet-termijnoverdracht — een satelliet leest haar eigenaar ontkennend, op de kring over de vorige vergunning

Een Vergunning heeft een Termijn (de satelliet, over het feittype
`vergunningstermijn`) en kan een vorige vergunning hebben. Het overschot
van een Termijn (de dagen van de stopdag tot de basiseinddag van haar
houder) gaat over naar de volgende vergunning: die is dan verlengd, en
haar verlengde einddag is haar basiseinddag plus dat overschot. Een
verlenging gaat niet twee keer over: een Termijn is alleen overdraagbaar
als haar houder zelf niet verlengd is, en alleen een overdraagbare Termijn
heeft een overschot. Langs een keten wisselt het dus: de eerste geeft over,
de tweede is verlengd en geeft niets over, de derde is niet verlengd en
geeft weer over.

Per type is dat een kring (verlengd → overdraagbaar → overschot →
verlengd); per object een keten die eindigt bij een vergunning zonder
voorganger. Het document is de getuige van de diepte van een satelliet
(`spec/RUNTIME-SEMANTICS.md` ch. 6.2: een satelliet staat op de diepte van
haar eigenaar; `spec/IR-SPEC.md` ch. 5.3: het huishouden van een object).

## De graaf

- "overdraagbaar" leest het kenmerk `verlengd` van de houder ontkend: een
  negatieve lokale rand, over de stap van de satelliet naar haar eigenaar.
- "overschot" leest het eigen kenmerk `overdraagbaar`: een positieve lokale
  rand.
- "verlengd" toetst of het overschot van de termijn van de vorige
  vergunning gevuld is: een negatieve verschoven rand (één stap naar de
  voorganger, één stap naar diens termijn).
- "verlengde einddag" leest hetzelfde overschot verschoven en positief; die
  vorm staat buiten de kring.

Zonder de verschoven rand blijft verlengd → overdraagbaar → overschot over,
kringvrij. Niveaus: verlengd 0, overdraagbaar 1 (de negatieve rand telt
één), overschot 1.

## Waarom de diepte van de satelliet beslist

De leden van de kring rekenen per object in de volgorde van de keten: eerst
de objecten op diepte 0, per diepte niveau na niveau. Staat een Termijn op
de diepte van haar houder, dan leest "overdraagbaar" het kenmerk van een
houder die op die diepte al op niveau 0 klaar is. Staat een Termijn op
diepte 0 terwijl haar houder dieper staat, dan leest "overdraagbaar" de
houder voordat diens `verlengd` bepaald is: de ontkenning slaagt, het
kenmerk staat, en een referentie-evaluatie zonder intrekking (ch. 6.1)
houdt het. Gemeten op 2026-10-22 met de satellietstap uit de dieptetoets
van `ref/` gehaald (de eigenaars van het geordende deel leeg, alleen als
meting): case 1 en case 2 falen (`T2` en `termijn tweede` overdraagbaar
met een overschot van 91; in case 2 daardoor `derde` verlengd, met
verlengde einddag 2027-08-31), case 3 speelt (daar is de houder van `T2`
niet verlengd, dus geeft de vroege lezing hetzelfde antwoord). Met de code
zoals ze is spelen alle drie.

## De handberekening

De duur in hele dagen tussen twee datums is hun verschil in kalenderdagen;
een datum plus een aantal dagen is een kalenderstap (ch. 3.8). Een leeg
overschot in de som telt als 0 dagen (ch. 1.2).

- 2026-03-01 tot 2026-06-01: maart 31, april 30, mei 31 → **92**.
- 2026-12-01 plus 92 dagen: december 30 (tot 31-12), januari 31, februari 28
  (2027 is geen schrikkeljaar) is 89, plus 3 → **2027-03-03**.
- 2026-09-01 tot 2026-12-01: september 30, oktober 31, november 30 → **91**.
- 2027-04-01 tot 2027-06-01: april 30, mei 31 → **61**.

| case | invoer | verlengd | verlengde einddag | overdraagbaar | overschot |
|---|---|---|---|---|---|
| `case1-keten-van-twee` | V1: stopdag 2026-03-01, basiseinddag 2026-06-01, termijn T1; V2: stopdag 2026-09-01, basiseinddag 2026-12-01, vorige V1, termijn T2 | V1 nee, V2 **ja** | V1 **2026-06-01**, V2 **2027-03-03** | T1 ja, T2 **nee** | T1 **92**, T2 **leeg** |
| `case2-keten-van-drie` | eerste als V1, tweede als V2 (vorige eerste), derde: stopdag 2027-04-01, basiseinddag 2027-06-01, vorige tweede; elk met een eigen termijn | eerste nee, tweede **ja**, derde **nee** | eerste **2026-06-01**, tweede **2027-03-03**, derde **2027-06-01** | termijn eerste ja, termijn tweede **nee**, termijn derde ja | termijn eerste **92**, termijn tweede **leeg**, termijn derde **61** |
| `case3-zonder-stopdag` | V1 zonder stopdag, verder als in case 1; V2 als in case 1 | V1 nee, V2 nee | V1 **2026-06-01**, V2 **2026-12-01** | T1 ja, T2 ja | T1 **leeg**, T2 **91** |

Zonder voorganger is een vergunning niet verlengd: het overschot van de
termijn van de vorige vergunning is leeg, want er is geen vorige (ch. 5.8).
In case 2 is de tweede verlengd, dus geeft haar termijn niets over, en is
de derde niet verlengd: haar verlengde einddag blijft de basiseinddag, en
haar eigen termijn is weer overdraagbaar. In case 3 heeft V1 geen stopdag,
dus geen overschot; V2 is niet verlengd en haar termijn geeft 91 dagen
over. De labels van case 2 zijn zo gekozen dat noch de volgorde in het
bestand (tweede, derde, eerste) noch de alfabetische (derde, eerste,
tweede) de volgorde van de keten is. Elke case pint ook dat het kanaal leeg
is.

## De bron

De bron staat in `source.rgs`; de frontend geeft dit `ir.json`
byte-identiek uit (gate 10).

## Herkomst

Eigen werk (2026-10-22, de open vondst 4 van de kaart
`instance-cycle-household-reads`: geen golden maakte de diepte van een
satelliet beslissend), naar de vorm van `jutterswet-vergunningketen`.
