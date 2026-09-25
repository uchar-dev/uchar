# jutterswet-nestkenmerk-rgs — de RegelSpraak-tweeling van `anchor/jutterswet-nestkenmerk`

Dezelfde defaults als het handanker `anchor/jutterswet-nestkenmerk`, nu
geschreven in het RegelSpraak-dialect en door het frontend
(`frontends/regelspraak`) uitgezonden: een grond `nx` die `vrijgesteld`
toekent, met twee ongeordende uitzonderingen, de ontkennende `ny` en de
toekennende `nz`. De vier `case*.json` zijn byte-gelijk aan die van het
anker. Het is de eerste golden met een `Uitzonderingen ongeordend op`-blok
(vorm 2 van het dialect, `frontends/regelspraak/grammar/DIVERGENCES.md` §4)
en de eerste waarin het kenmerkconflict `Jutter.vrijgesteld/0` uit een
RegelSpraak-bron komt. `nx`, `ny` en `nz` zijn abstracte gronden, zoals in
het anker: de golden pint de uitvoeringssemantiek, geen lezing van een
wettekst.

## De bron (`bron.rgs`)

- basisregel `vrijstelling van een jutter`: `Een Jutter is vrijgesteld
  indien hij nx is`;
- het blok `Uitzonderingen ongeordend op vrijstelling van een jutter:` met
  twee leden: `vrijstelling vervalt bij ny` (`Een Jutter is niet vrijgesteld
  indien hij ny is`) en `vrijstelling bij nz` (`Een Jutter is vrijgesteld
  indien hij nz is`).

De leden van een `Uitzonderingen ongeordend op X`-blok erven de
rechtvaardiging van X, net als een enkele `Uitzondering op X`: het frontend
zet `nx` vóór de eigen voorwaarde van elk lid, in één platte `Compound` met
kwantor `all`. Elk blok draagt een `// Bron:`-annotatie met de tekst die
het anker op de overeenkomstige default heeft.

## De lowering

Eén `PrioritizedDefaults` op het kenmerk `vrijgesteld`, genoemd naar de
basisregel. Een ongeordend blok onder een basisregel wordt een
groepsdefault direct boven de basis: een default zonder eigen besluit of
rechtvaardiging, met één uitzonderingsgroep per lid, gemarkeerd
`unordered`:

```
Default 0  (groepsdefault)
  exceptions, ongeordend:
    groep 0 -> Default (besluit onwaar, als: all[nx, ny])   -- vrijstelling vervalt bij ny
    groep 1 -> Default (besluit waar,   als: all[nx, nz])   -- vrijstelling bij nz
Default 1  (besluit waar, als: nx)                           -- de basis
```

## De structurele diff tegen het anker

Gemeten op beide `ir.json` met elk `source`-record recursief weggelaten en
de sleutels gesorteerd (de methode van `spec/proposals/PHASE5D-DIFF.md`
§2–3). Het model, de groepsnaam, de uitzonderingsgroepen met hun
rechtvaardigingen en besluiten, en het geldigheidsvenster zijn gelijk. Er
is één verschil (5 regels van de genormaliseerde diff): in het anker draagt
default 0 zelf het besluit `waar` op `nx` naast zijn uitzonderingsgroepen
(een form-a-default); in de tweeling is default 0 een groepsdefault en
staan besluit en rechtvaardiging van de basis in een eigen default 1.

Triage: **(i) dialectverschil**. Het dialect houdt het eerste niveau onder
een basisregel buiten de basis, als ketenpositie erboven (lowering van
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §5.2), zodat de eigen
basis van een subtype de uitzonderingen niet meeneemt wanneer zij de
geërfde basis vervangt (RUNTIME-SEMANTICS ch. 7.10). De uitkomsten zijn
gelijk: de groepsdefault beslist wat zijn uitzonderingsgroepen beslissen,
en beslissen die niets, dan valt de keten door naar de basis, zoals de
form-a-default van het anker terugvalt op zijn eigen besluit (ch. 7.9).
Het conflictfeit houdt dezelfde sleutel `Jutter.vrijgesteld/0`: de
uitzonderingsgroepen hangen in beide documenten aan default 0 van de
wortelketen. Geen verschil van klasse (ii) of (iii).

De `source`-records vallen buiten deze diff; elk knooppunt van de
defaults-laag draagt er in beide documenten een. Het anker draagt
omschrijvingen: `art. 7: de vrijstelling met een geneste uitzondering` op
de groep, `uitzondering ny: niet vrijgesteld` en `uitzondering nz:
vrijgesteld` op de uitzonderingsgroepen. De tweeling draagt op elke default
de `// Bron:`-annotatie van zijn blok, op de groep die van de basisregel,
en op de groepsdefault en beide uitzonderingsgroepen de kop die ze maakt,
`Uitzonderingen ongeordend op vrijstelling van een jutter`: een kop en geen
regel maakt die knopen, dus er staat geen annotatie boven.

## Cases

De vier cases van het anker, byte-gelijk. Gemeten op dit `ir.json`: ref en
clingo `PASS` op alle vier (in case2 het feit `Jutter.vrijgesteld/0`), de
Drools exec-tak `PLAYED 4`; de DRL-teksttak weigert het document op zijn
defaults-zin (`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch
has no defaults lowering`). `case1-y-wint` houdt stand onder een uitvoerder
die niets doet, zoals in het anker, en staat met de reden van het anker op
de overleverslijst van gate 7 (`scripts/check-noop.sh`); de andere drie
weerleggen hem.
