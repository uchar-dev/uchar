# frontend-regelgroep-sluit-bij-uitzondering: een `Regelgroep` eindigt stil bij de eerste `Uitzondering op`

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting, gemeten
in het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(M5, kaartbranch): een `Regelgroep berging` over een basisregel, haar
`Uitzondering op` en een derde regel zendt `leden: ["bergingsbevoegd bij
quotum"]` uit met `diagnostics: []`. De uitzondering en de regel erna
vallen buiten de groep, want `regelGroep` omvat alleen opeenvolgende
`regel`-constructen (`RegelSpraak.g4`, het `Regelgroep`-patroon), en omdat
de basis in een defaults-groep wordt gelift weigert de verifier het document
daarna met `WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd bij
quotum' does not resolve to a rule form`. De auteur krijgt de fout op de
verkeerde plaats en zonder oorzaak.

## Te beslissen bij oppakken

- (a) Weigeren: een `Uitzondering op` (of `Uitzonderingen ongeordend`,
  `Afwijking van`) binnen de reikwijdte van een `Regelgroep` is in het
  dialect niet toegestaan; luide melding op de kop van het blok, met de
  verwijzing naar §9.10 (de groep bestaat alleen voor recursie).
- (b) Opnemen: de defaults-groep (WF-107 geeft haar een naam) wordt lid van
  de regelgroep. Dat vergt dat een groepslid naar een defaults-groep mag
  verwijzen — IR-SPEC (WF-033) en verifier (**voorstel**) — en een
  semantiek voor een defaults-groep in een recursieve groep
  (RUNTIME-SEMANTICS ch. 5.5/9.10, **voorstel**).

Aanbeveling: (a), tenzij een werkbank-regelset de combinatie nodig heeft;
(b) is een spec-vraag met drie uitvoerders erachter.

## Acceptatie (bij oppakken aanscherpen)

- Rood-eerst JUnit-getuige met het M5-document; de gekozen uitkomst
  (melding op de juiste regel, of opname met verifier-dekking).
- `DIVERGENCES.md` §4 en het `Regelgroep`-deel beschrijven de interactie.
- Alle veertien gates groen.

May touch: `frontends/regelspraak/**`; bij (b) `spec/IR-SPEC.md`,
`spec/verifier/**`, `corpus/negatief/**` (**voorstel**); kaartverplaatsing.

Niet in deze kaart: de dialectvormen zelf (eigen kaarten).
