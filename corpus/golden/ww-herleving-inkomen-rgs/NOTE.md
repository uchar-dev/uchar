# ww-herleving-inkomen-rgs — de RegelSpraak-tweeling van `anchor/ww-herleving-inkomen`

Dezelfde wet als het handanker `anchor/ww-herleving-inkomen`, nu geschreven
in het RegelSpraak-dialect en door het frontend (`frontends/regelspraak`)
uitgezonden: het inkomenspad van de WW, art. 15 → art. 20 lid 1 onderdeel c
→ art. 21 lid 1 → art. 21 lid 2 onderdeel a, als één defaults-groep op het
kenmerk `recht op uitkering`, plus drie rekenregels. De tien `case*.json`
zijn byte-gelijk aan die van het anker: één set verwachtingen, afgespeeld
tegen een handgeschreven en een uitgezonden document. Het is de eerste
golden waarin een RegelSpraak-bron drie `Uitzondering op`-blokken schrijft
die elk de vorige regel als drager noemen, zodat twee niveaus in elkaar
nesten (vorm 2 van het dialect, `frontends/regelspraak/grammar/DIVERGENCES.md`
§4).

## De wetteksten

Werkloosheidswet (BWBR0004045, toestand 2026-07-01): art. 15, art. 16
lid 1, art. 20 lid 1 onderdeel c, art. 21 lid 1 en lid 2 en art. 43 lid 1,
verbatim in `corpus/golden/anchor/ww-herleving-inkomen/NOTE.md`.

## De bron (`source.rgs`)

De bron van `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §8.2,
letterlijk: het objecttype `Werknemer`, een basisregel, drie rekenregels en
drie uitzonderingsblokken. Elk blok schrijft alleen zijn eigen voorwaarde:

- basisregel `toekenning art 15`: `Een Werknemer heeft recht op uitkering
  indien hij werkloos is`;
- `Uitzondering op toekenning art 15: einde art 20 lid 1 c`: `heeft geen
  recht op uitkering` indien zijn maandinkomen groter is dan zijn
  inkomensgrens;
- `Uitzondering op einde art 20 lid 1 c: herleving art 21 lid 1`: `heeft
  recht op uitkering` indien hij omstandigheid vervallen is — het blok
  noemt een uitzondering als drager;
- `Uitzondering op herleving art 21 lid 1: blokkade art 21 lid 2 a`: `heeft
  geen recht op uitkering` indien hij nieuw recht ontstaan is en zijn nieuw
  maandloon groter is dan zijn inkomensgrens;
- de rekenregels `inkomensgrens van een werknemer` (maandloon maal 0,8750,
  rekenkundig afgerond op 2 decimalen), `verstreken periode bij herleving`
  (de tijdsduur van eindigingsdatum tot vervaldatum in hele dagen) en
  `nieuwe maxdatum bij herleving` (maxdatum plus verstreken periode).

`Uitzondering op X` is beperkend: het frontend zet de rechtvaardiging van X,
zelf al geconjungeerd, vóór de eigen voorwaarde, in één platte `Compound`
met kwantor `all`. Zo draagt de blokkade alle vijf conjuncten, zoals in het
anker. Elk blok en de basisregel dragen een `// Bron:`-annotatie met hun
WW-artikel.

## De lowering

Eén `PrioritizedDefaults` op het kenmerk `recht op uitkering`, genoemd naar
de basisregel. Het eerste niveau is een ketenpositie boven de basis; elk
dieper niveau nest in de default van zijn drager. Met W = `werkloos`,
T = `maandinkomen > inkomensgrens`, V = `omstandigheid vervallen` en
N = `nieuw recht ontstaan` en `nieuw maandloon > inkomensgrens`:

```
Default 0  (besluit onwaar, als: all[W, T])                -- einde art 20 lid 1 c
  exceptions, geordend, één groep:
    Default (besluit waar, als: all[W, T, V])              -- herleving art 21 lid 1
      exceptions, geordend, één groep:
        Default (besluit onwaar, als: all[W, T, V, N])     -- blokkade art 21 lid 2 a
Default 1  (besluit waar,   als: W)                        -- toekenning art 15
```

## De structurele diff tegen het anker

Gemeten op beide `ir.json` met elk `source`-record recursief weggelaten en
de sleutels gesorteerd (de methode van `spec/proposals/PHASE5D-DIFF.md`
§2–3): 140 regels van de genormaliseerde diff (`diff`, de regels met `<` of
`>`), in elf blokken. Het objecttype, de rekenregels, de besluiten en de
rechtvaardigingen zijn gelijk, op de vier verschillen hieronder na.

| verschil | anker | tweeling | triage |
|---|---|---|---|
| vorm van de groep | default 0 = toekenning, met einde genest in haar `exceptions` | default 0 = einde, met herleving en blokkade genest als in het anker; default 1 = toekenning | (i) |
| groepsnaam | `recht op uitkering bij herleving` | `toekenning art 15` | (i) |
| literal `0,8750` | type 4 decimalen, product 6 | type 3 decimalen, product 5 | (i) |
| eenheid `dag`, op `verstreken periode` en op het type van de `duration`-aanroep | alleen `abbreviation` | ook `canonical: "dag"` en `factorToCanonical: {num: 1, den: 1}` | (i) |

**De vorm van de groep.** Het anker nest de hele keten in de toekenning.
Het dialect houdt het eerste niveau buiten de basis, als ketenpositie
erboven (lowering van `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§5.2), zodat een eigen basis van een subtype de uitzonderingen niet
meeneemt wanneer zij de geërfde basis vervangt (RUNTIME-SEMANTICS
ch. 7.10). De volgorde van beslissen is gelijk: in het anker beslissen de
uitzonderingsgroepen van de toekenning eerst en daarna haar eigen besluit
(ch. 7.9); in de tweeling beslist default 0 met zijn uitzonderingsgroepen
eerst en valt de keten daarna door naar de toekenning. Herleving en
blokkade staan in beide documenten op dezelfde plaats onder einde, dus in
beide beslissen blokkade, herleving, einde en toekenning in die volgorde.
Met groepen van één lid beslissen beide vormen op elke invoer hetzelfde;
zij verschillen alleen in het indexpad waarop een genest conflict zou
worden gesleuteld, en een groep van één lid kent geen conflict. De vier
rechtvaardigingen zijn gelijk, conjunct voor conjunct en in dezelfde
volgorde.

**De groepsnaam.** Het frontend noemt een defaults-groep naar haar
basisregel; het anker draagt een eigen omschrijving. `name` is geen
`source`, dus het verschil blijft na het weglaten van de `source`-records
staan. Geen case leest de naam.

**De literal.** Het frontend typeert een numerieke literal op zijn kleinste
schaal (`ExpressionBuilder.literalDecimals`): `0,8750` op drie decimalen,
het product op vijf; het lexeem blijft `0.8750`. RUNTIME-SEMANTICS ch. 2.10
maakt het type van een `Literal` de bron. De waarde is dezelfde, en de
afgeronde inkomensgrens ook (2100,00). clingo's gesplitste geld-×-
tariefcodering (ASP-TRANSLATION S18(c)) koos tot de kaart
clingo-tarief-decimalen haar route op dat type en nam alleen een tarief op
vier decimalen; het product viel daardoor op het gewone productpad, waar het
operandhek bij een maandloon van 2400,00 vuurt, en clingo was UNSAT op alle
tien cases. Sinds die kaart kiest de codering haar route op de decimalen die
de termen dragen: het lexeem `0.8750` draagt er vier, dus neemt het product
de splitsing met de tariefterm zoals hij is (k = 1), zonder herschaling. De
herschaling naar schaal 10^4 is voor een tarief dat op één tot drie
decimalen geschreven of gedeclareerd is; deze tweeling gebruikt haar niet.

**De eenheid.** Het frontend schrijft de eenheid `dag` met het
conversiepaar naar zichzelf; het anker schrijft de kale vorm. Beide
spellingen voldoen aan WF-092: het paar staat heel of helemaal niet.

Geen verschil van klasse (ii) of (iii).

De `source`-records vallen buiten deze diff. Het anker draagt
omschrijvingen met artikelnummers op de groep, op elke default, op elke
rechtvaardiging en op elke uitzonderingsgroep. De tweeling draagt op elke
default de `// Bron:`-annotatie van haar blok (`WW art. 20 lid 1 onderdeel
c` enzovoort), op de groep die van de basisregel (`WW art. 15`), op elke
samengestelde rechtvaardiging de teksten van haar conjuncten met " en "
ertussen, en op de twee geneste uitzonderingsgroepen de kop die ze maakt
(`Uitzondering op einde art 20 lid 1 c`, `Uitzondering op herleving art 21
lid 1`): een kop en geen regel maakt een uitzonderingsgroep, dus er staat
geen annotatie boven.

## Cases

De tien cases van het anker, byte-gelijk. Gemeten op dit `ir.json`: ref en
clingo `PASS` op alle tien (case7 met 2 expectations, de andere met 5), de
Drools exec-tak `PLAYED 10`; de DRL-teksttak weigert het document op zijn
defaults-zin (`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch
has no defaults lowering`). Negen cases weerleggen een uitvoerder die niets
doet (gate 7). `case7-invoer-staat-tegen-blokkade` houdt stand onder zo'n
uitvoerder, zoals in het anker, en staat met de reden van het anker op de
overleverslijst van gate 7 (`scripts/check-noop.sh`): een `onwaar`-besluit
neemt geen kenmerk weg dat de invoer zet (RUNTIME-SEMANTICS ch. 7.1 en
7.8).
