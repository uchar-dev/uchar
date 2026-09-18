# v1-wiremigratie-inventaris: de brekende lijst compleet en besloten

Maintainersbesluit 2026-09-18: de brekende v0→v1-wiremigratie wordt
voorbereid met één inventarisatiekaart, uit te voeren na het
zes-kaarten-pariteitsprogramma en de tekstlexeem-canonicalisatie. De
versieklap zelf is een eigen vervolgprogramma; deze kaart maakt hem
beslisbaar. Additieve delta's (nieuwe knopen naast bestaande vormen)
vallen erbuiten — die breken niets en landen gewoon door.

Aanleiding: v1 groeit al maanden additief (prioritizedDefaults,
Kwantorvouw, bronPad, Let/BinderRef, hiërarchie, vouwen), maar de
brekende kern-herspelling is opgespaard. Eén klap wil je één keer doen:
elke brekende delta dwingt alle emitters (RegelSpraak, Catala, F-logic),
alle consumenten (ref, clingo, Drools-loader, verifier), het schema, de
corpusbestanden en de toolchain-pin tot meebewegen. Nu is er één afnemer
op de pin; wachten maakt de migratie alleen duurder.

## Wat de kaart oplevert

Een proposal-document (`spec/proposals/`, beschermd-padvoorstel) met de
complete brekende lijst, per delta: de wire-wijziging, de geraakte
partijen, de migratienota, en een besluit-klare genummerde vraag waar
een keuze open is. Bekende kandidaten om te verifiëren en aan te vullen:

- de inerte bean-spiegelbagage eruit (`zetVeld`/`joinVeld` en verwanten;
  de symbolische opvolgers zijn al de enige gelezen vorm — IR-SPEC
  ch. 3.4-migratienota, CONSTRUCTS rij 5);
- `bron`-string naar het gestructureerde `Bron`-record (rij 3);
- typed values (de tekstlexeem-canonicalisatie,
  `20260918-tekstlexeem-canonicalisatie.md`, is de voorbode);
- alles wat een mechanische sweep over "v0", "mirror", "inert",
  "baggage", "tracked delta" in `spec/` en `CONSTRUCTS.md` verder
  aanwijst als brekend-maar-onbesloten.

Plus het volgordebesluit als expliciete maintainersvraag: de
`toka/`-parityzone verliest bij de klap zijn directe byte-anker tegen de
toolchain — zone opruimen vóór de migratie (met de
geschiedenis-herschrijving die de publieke flip toch al eist), of
mee-regenereren en pas bij de flip opruimen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De lijst is aantoonbaar compleet: registervergelijking tegen
  `spec/CONSTRUCTS.md` (elke rij geclassificeerd
  additief/brekend/gedaan) plus de mechanische sweep, beide verantwoord
  in het document.
- (b) Elke brekende delta draagt wire-diff-schets, geraakte partijen,
  migratienota en waar nodig een genummerde beslisvraag; geen enkele
  staat als "nog uitzoeken".
- (c) Deze kaart wijzigt geen code, schema, corpus of gates; alle
  veertien gates blijven onaangeroerd groen. Het document is een
  proposal; besluiten neemt de maintainer op de genummerde vragen.
- (d) De uitkomst benoemt het vervolgprogramma in kaartvorm (schatting
  van de kaartenreeks), inclusief herpin- en
  emitter-golden-consequenties voor de toolchain.
