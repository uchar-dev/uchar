# Minimale F-logic-frontend: subtypen uit een tweede taal, met Flora-2 als externe maat

Goal: `frontends/flogic` met de minimale F-logic-snede uit `spec/proposals/SUBTYPING.md` vraag 6: subklasse-declaratie (`c::d`), lidmaatschap (`o:c`), signatures (`c[attr=>type]`), attribuutwaarden (`o[attr->v]`) en Horn-regels, gelowered naar dezelfde kernel-subtypering als het RegelSpraak-dialect; alles buiten de snede (HiLog, meta-programmeren, defeasible inheritance, negatievormen, meervoudige ouders) weigert luid met verwijzing naar het opdrachtdocument van 2026-09-10 (opgegaan in `spec/proposals/SUBTYPING.md`). De use case van opdrachtdeel 1.5 wordt in F-logic-bron toegevoegd en per frontend-diff (de penningrecht-methode van Phase-5d) naast de dialectvorm gelegd: elk verschil wordt gecategoriseerd en alleen administratieve of getrackte verschillen zijn toelaatbaar. Als de onderzoekskaart Flora-2 bruikbaar verklaarde: draai de use case ook door Flora-2 en leg de uitkomstvergelijking vast als externe-executor-notitie (geen gate, wel bewijsstuk dat onze inheritance-semantiek de JACM-lijn niet stilzwijgend tegenspreekt).

Acceptance: de F-logic-golden speelt groen door frontend → verifier → executorreeks en is via de frontend-diff tegen de dialect-golden verantwoord (geen categorie-(iii)-verschillen); de weigeringslijst heeft per punt een negatieve fixture; de volledige gate-reeks is groen; de eventuele Flora-2-vergelijking staat als notitie in het proposal of de kaartafronding.

May touch: `frontends/flogic/` (nieuw), `corpus/` (de F-logic-golden + fixtures), `spec/proposals/SUBTYPING.md` (alleen de Flora-2-notitie), en de eigen kaart.

Out of scope: kernel- en executor-wijzigingen; ErgoAI-integratie of licentieonderzoek daarvan; Flora-2's overriding-syntax in de frontend-snede (de sémantische vergelijking met F-logic's niet-monotone overerving hoort bij de onderzoekskaart, want besluit 3 maakt onze groepserving zelf een vorm daarvan); volledige F-logic-dekking — dit is nadrukkelijk een minimale snede met luide randen.

## Aangescherpt door de onderzoekskaart (2026-09-11)

`spec/proposals/SUBTYPING.md` §8 is bindend; hieronder de uitvoerbare
neerslag.

**Gemeten bevinding: de snede zoals de opdracht haar vastlegt kan haar
eigen use case niet uitdrukken.** In Flora-2 2.2 is de érfbare
klasseniveau-waarde `c[|attr -> v|]` — mét de balken — en niet
`c[attr -> v]`; de JACM-vorm `*->` is Flora-1-syntax en wordt door de
2.2-parser geweigerd (gemeten). De opdrachtsnede noemt alleen
`c[attr=>type]` en `o[attr->v]` en kan daarmee geen enkele érfbare
default schrijven, dus ook de pinguïn-toets niet.

*Aanbevolen*, niet beslist: verbreed met **`c[|attr -> v|]`** (lowert
naar een uchar defaults-**basis** op het doel van de klasse — precies
de vorm van besluit 3) en **`c[|attr => type|]`** (de
instantiesignatuur), en maak de ongebalkte klasseniveau-waarde
`c[attr -> v]` een **weigering**, want uchar kent geen
klasseniveau-attribuutwaarde en stille lowering naar een
instantiedefault zou de betekenis veranderen. De opdracht heeft de
snede vastgelegd, dus verbreden is een **opdrachtamendement en een
maintainerbesluit** — SUBTYPING §11.4. Begin deze kaart niet voordat
dat punt beantwoord is; als de maintainer de verbreding afwijst, moet
de use case van deze kaart een andere zijn dan die van §7.

**Weigeringslijst.** Meervoudige ouders (twee `::`-feiten met
onvergelijkbare ouders, met `RechtOpUitkeringWazo` en
`RechtUitkeringZw` bij naam) — geweigerd onder **WF-106** (besluit
§11.1, 2026-09-12: "een objecttype declareert hoogstens één direct
supertype"). Deze fixture is de scherpste getuige die die regel heeft:
op de wire is een tweede ouder onschrijfbaar (`supertype` is één
string), dus er is geen verifier-check en geen `corpus/negatief`,
terwijl F-logic met twee `::`-feiten wél twee ouders kán schrijven —
hier wordt de weigering dus echt uitgeoefend en niet alleen
geconstrueerd (SUBTYPING §3.1, §9.3). Verder: HiLog en
meta-programmeren; defeasible
reasoning (argumentatietheorieën, `\overrides`, `\neg`); negatievormen
(`\naf`, `\neg`); **`setsemantics{inheritance=monotonic}`** — uchar's
overerving is niet-monotoon per besluit 3 en de twee modi zijn niet
uitwisselbaar; de ongebalkte `c[attr->v]`; en al het overige buiten de
verbrede snede. Elke weigering verwijst naar
het opdrachtdocument van 2026-09-10 (opgegaan in `spec/proposals/SUBTYPING.md`).

**Golden.** `corpus/golden/jutterswet-wrakrecht-flogic` (het
`-flogic`-achtervoegsel volgt de bestaande `-catala`-conventie), naast
de dialectvorm `corpus/golden/jutterswet-wrakrecht` in de
frontend-diff; beide drukken de kernel-anchor
`anchor/jutterswet-strandrechtketen` uit.

**Flora-2: bruikbaar verklaard, maar begrensd.** Installeerbaar en
geïnstalleerd (buiten de repo): XSB 5.0 uit broncode, daarna de
Makeself-`.run` van Flora-2 2.2 beta uitgepakt met
`--noexec --keep --nox11 --target` en het gedocumenteerde
`floraconfig`-pad gedraaid; de installer ondersteunt Darwin expliciet.
Wat zij kan: de pinguïn-toets en de vierceltabel exact reproduceren —
gemeten, en cel voor cel gelijk aan besluit 3. Wat zij niet kan: het
§9.5-conflictgeval überhaupt uitdrukken (geen conflictkanaal, en
kardinaliteitsbeperkingen worden nooit afgedwongen — zij geeft beide
botsende waarden stil terug), en geen eenheden, decimalen, tijdlijnen
of verdeling dragen. **Dus: leg de vergelijking vast als notitie over
het overervingsskelet alleen — nooit een gate, nooit een vierde been
van de differential.** De letterlijke uitvoer van de vier proeven staat
in SUBTYPING §8.3 en hoeft niet opnieuw gedraaid te worden.

## Result (2026-09-12)

**Geland.** `frontends/flogic` — lexer, recursief-dalende parser, verlaging
en een eigen serialiser (~1200 regels OCaml), gebouwd op `uchar-ir-v0`;
`corpus/golden/jutterswet-wrakrecht-flogic` met `bron.flogic`, de door de
frontend geëmitteerde `ir.json`, de vier byte-identiek hergebruikte
casusbestanden en een NOTE.md die de frontend-diff en de
Flora-2-vergelijking draagt; `scripts/check-flogic.sh` (gate 14) en de
EXPECTED_PLAYED-gelijkheid van gate 12 op 132 -> 133.

**Bouwvorm: OCaml + dune, niet de jar-vorm van gate 10.** De werkruimte
bouwt verifier, `ref`, de clingo-backend en de Catala-frontend al met dune;
`uchar-ir-v0` levert de getypeerde AST, zodat schemaconformiteit
structureel is in plaats van stringly-typed (het Catala-precedent). Dat
scheelt de gitignored jars van gate 10 en — belangrijker — geeft gate 14
een **onvoorwaardelijke** CI-poot waar gate 9 er een voorwaardelijke heeft:
er hangt geen externe compiler aan. Byte-identiteit loopt via gate 2's
eigen serialiser, uitgevoerd vanuit dat script, zoals gate 9 het doet: één
canonieke vorm in de repo erbij zou er een derde zijn.

**Bronextensie `.flogic`.** Het precedent (`bron.rgs`, `bron.catala_en`) is
"de eigen extensie van de brontaal", en die zou `.flr` zijn — maar dat is
Flora-2's extensie en de snede is Flora-2 niet. `.flogic` noemt de taal en
loopt gelijk met `frontends/flogic` en het `-flogic`-achtervoegsel.

### Punten die noch proposal noch kaart dekte

**(1) Het feittype heeft geen F-logic-vorm.** F-logic kent objectattributen,
geen benoemde binaire relatie met twee benoemde roluiteinden. Gekozen: de
relatie wordt gereïficeerd als klasse, gemarkeerd met het gereserveerde
lidmaatschap `x : feittype`, met haar twee rolsignaturen erop. Dat gebruikt
alleen snedevormen, noemt élke naam expliciet (zodat feittypenaam en
`bronRol.feittype` gelijk blijven aan de dialectvorm) en is de gangbare
associatieklasse-idiomatiek. Het alternatief — een verwijzingsattribuut op
de houder — laat de feittypenaam en de tegenrol synthetiseren, dus raden.

**(2) `=>>` bestaat niet in Flora-2 2.2** (gemeten: parsefout). De
meervoudigheid van een rol staat in de kardinaliteitsbeugels,
`{0..*}` tegen `{1..1}` — dus die beugels zitten in de snede, hoewel de
kaart alleen `c[attr=>type]` noemt.

**(3) Grondfeiten worden geweigerd.** De kaart noemt `o:c` en `o[attr->v]`
de kanalen voor "case-invoer/instantie", maar de IR hééft geen
instantiekanaal: instanties horen in een `uchar-case-v0`-document, en de
vier hergebruikte casusbestanden zijn dat kanaal. Op programmaniveau
weigeren beide vormen dus luid met een verwijzing naar het casuskanaal; het
enige toegelaten grondlidmaatschap is de gereserveerde marker uit (1).

**(4) Prioriteit komt uit de vorm van de regel.** F-logics eigen middel om
twee definities van één doel te ordenen is defeasible reasoning, en dat is
weigering 4. Gekozen: een onvoorwaardelijke definitie is de basis van haar
doel, een voorwaardelijke een uitzondering erboven (broncodevolgorde), en
een doel met alléén een onvoorwaardelijke definitie blijft een kale regel —
de ch.-7.8-norm — in plaats van een defaultsgroep van één. De gebalkte
`c[|attr -> v|]` is altijd een basis en nooit een kale regel: een erfbare
default *is* een default. Twee onvoorwaardelijke definities weigeren.

**(5) Getal en bezieldheid blijven leeg.** F-logic declareert geen
grammaticaal getal en geen bezieldheid, dus `meervoud: null` en
`bezield: false`, en een meervoudige rol draagt één label. Niet verzonnen,
en niet betekenisdragend: geen enkele executor leest die velden (gemeten;
Drools laadt ze en gebruikt ze niet), de casussleutel valt op `Rol.naam`
in plaats van `Rol.meervoud` (`ref/lib/case.ml:259` matcht op beide), en de
vorm "meervoudig zonder meervoud" staat al in `jutterswet-kaveltelling`.
Gate 12 bevestigt het: de golden speelt.

**(6) `leegwaarde` is gemeten, niet verzonnen.** Flora-2 antwoordt 0 op een
aggregaat over een lege verzameling (gemeten op 2.2 beta), dus de SOM
krijgt die 0 op de gedeclareerde schaal van het opgetelde attribuut. De
fixture `aggregatie.flogic` met een lege rol pint het in ref én clingo.

**(7) De typewoordenschat.** `numeriek(teken, schaal)` en
`kenmerk(soort)` als samengestelde termen (parseren in Flora-2, gemeten);
`\integer`, `\boolean`, `\string`, `\date` als exacte aliassen; `\decimal`
weigert, want het noemt geen decimaalschaal waar uchars `Numeriek` er een
declareert — stil raden zou erger zijn dan weigeren.

### De frontend-diff

Vier administratieve categorieën, geen vijfde: (i) bronteksten (`bron`,
`onderwerpBron` — door geen executor gelezen), (ii) literaalspelling met
haar annotatie (`5,00` tegen `5.00`; het anchor spelt het al als de
F-logic-vorm, en gate 13's wet zegt dat consumenten uit declaraties
afleiden), (iii) gedeclareerde oppervlaktenamen waar F-logic geen
grammatica voor heeft (punt 5), (iv) de volgorde van de regelindex. Alle
objecttypen, datatypes, het `supertype`, het feittype, de drie
`ResolvedRegel`s, de twee `PrioritizedDefaults`-groepen met dezelfde leden
in dezelfde volgorde en beide aggregaties zijn byte-identiek.

### Flora-2 op het overervingsskelet

Gemeten op 2.2 beta (rev. 1203) over XSB 5.0, met `grondslag` op 10 zodat
de twee bases de klasseniveau-constanten worden die Flora wél kan dragen.
Cellen 1-5: 10 / 99 / 20 / 99 / 55, exact gelijk aan uchar. Cel 6: Flora
geeft {55, 99} zonder te kiezen, uchar antwoordt 55 — dat ligt ín Flora's
antwoordverzameling en is de waarde die Flora's eigen principe 1 kiest
wáár het kiest; het verschil is determinisme, niet richting. Vastgelegd in
de NOTE.md van de golden (het corpus kent één notitiebestand per golden;
een tweede bestandssoort zou het precedent breken). Bewijsstuk, nooit een
gate.

### Bewust niet gedaan

- `spec/proposals/SUBTYPING.md` is niet aangeraakt (de kaart stond het toe
  voor de Flora-2-notitie, maar dat is een beschermd pad en de notitie hoort
  bij de golden waar zij over gaat).
- `min`/`max`/`avg` parseren en weigeren met naam: ze zijn goedkoop te
  ondersteunen maar zouden een ongeteste verlaging opleveren.
- Geen `corpus/negatief`-ingang: WF-106 begrenst de taal *boven* de wire,
  dus er is geen wire-document dat haar schendt — de fixture is de getuige,
  precies zoals SUBTYPING §3.1 vastlegt.


### Naronde (zelfde kaart)

Een lensronde over de veldgebruikers leverde één echt gat op, hier meteen
gedicht: de frontend liet een regel zónder `@!{}`-id toe, en die bereikt de
wire als `naam: null`. WF-020 filtert lege namen weg vóór de
uniciteitsvergelijking en kijkt sowieso nooit naar de naam van een
`prioritizedDefaults`-groep, dus noch een lege noch een dubbele regelnaam
wordt stroomafwaarts gevangen — en de DRL-tekstbranche van de
Drools-backend (het tweede been van gate 13) kan een lege naam niet
renderen. De rule-id is daarom verplicht en moet uniek zijn; twee
weigeringen met fixture erbij, suitevloer 60 -> 66. De golden verandert er
niet van: gate 14 emitteert haar byte-identiek, wat het bewijs is dat de
regel alleen een deur sluit die niemand hoorde te gebruiken.

Wat diezelfde ronde bevestigde en dus géén wijziging vroeg: `meervoud` en
`bezield` worden door geen enkele check of executor semantisch gelezen
(alleen WF-057 toetst de tekstveiligheid van een aanwezige meervoudsvorm);
clingo valt voor de canonieke rolnaam terug op het enkelvoud als het
meervoud ontbreekt en Drools matcht op naam óf meervoud, wat precies is
waarom de hergebruikte casusbestanden hier op `Rol.naam` vallen; een lege
`regels`-index is toegestaan (geen enkele check leest hem — de
Catala-frontend emitteert hem altijd leeg), dus het vullen ervan is winst
en geen plicht; en gate 13 pikt een nieuwe golden vanzelf op, zonder
registratie.

### Tweede naronde: de gebalkte basis draagt nu ook een naam

De eerste naronde sloot alleen de Horn-regel. De gebalkte
klassewaarde bleef een stil kanaal: `c[|attr -> v|]` levert een
`PrioritizedDefaults`-groep, en een id kón er niet op — de parser weigerde
"a rule id on Vogel, which is not a rule". Gemeten in de eigen fixture
`overerving.flogic`: een groep `Pinguin.hoogte` met `naam: null`, en met
twee zulke bases een lege `regels`-index. Woordelijk het kanaal dat de
README en de eerste naronde claimden te sluiten.

Gekozen route: de meer-taal-route. `@!{'…'}` mag nu op de gebalkte
klassewaarde en is daar **verplicht**, met één uniforme regel — *elke vorm
die een naam op de wire zet draagt er een, en alleen die vormen mogen er
een*. Een gebalkte *signatuur* declareert een lid en noemt niets, dus een
id daarop weigert, net als op `::`, op `:` en op het ongebalkte frame.
Omdat Flora-2 geen id per item kent, draagt een benoemde gebalkte
uitspraak precies haar ene definitie. De uniciteitscontrole loopt over
beide soorten samen, en de `regels`-index is nu de volledige namenlijst in
broncodevolgorde. De golden verandert er niet van (benoemde Horn-bases);
gate 14 emitteert haar byte-identiek. Suitevloer 66 -> 81.

Meegenomen in dezelfde beweging: een fixture voor de
argumentatietheorie-descriptor `@{…}` (de README belooft er één per
weigering en die ontbrak); weigering van twee declaraties van één lid op
één klasse (voorheen exit 0 met een dubbele JSON-sleutel, RFC
8259-ongedefinieerd) en van een rol met een ongedeclareerde filler
(voorheen pas WF-003, terwijl de verlaging de filler leest om een
aggregatie te typeren); `Emit.to_file` serialiseert nu volledig vóór het
openen van het bestand, zodat een fail-fast-arm geen half document
achterlaat, en de CLI vangt de resterende fouten met een eerlijk
`FATAL`-kanaal (exit 3) naast `REFUSED` (exit 2); de twee `Option.get`'s
zijn weg; en de WF-106-naald in de suite is onderscheidend gemaakt —
`RechtUitkeringZw` is een prefix van `RechtUitkeringZwWazo`, dus de naald
draagt nu het paar én zijn ouders.

### Openstaand: een spec-gat, voor de maintainer

**Geen enkele WF-regel kruist een regelnaam op de wire.** WF-020 houdt
namen uniek binnen en over de regelvormige arrays, maar filtert lege namen
weg vóór de vergelijking, en `prioritizedDefaults` staat niet in
`form_arrays` — dus een lege naam, een duplicaat binnen de groepen én een
kruisbotsing tussen een groep en een regel passeren alle drie de verifier.
Eén Drools-branche (de DRL-tekstbranche, het tweede been van gate 13) kan
een lege naam bovendien niet renderen. De weigering in deze frontend is
vandaag het enige vangnet, en die geldt per definitie alleen voor
documenten die uit deze frontend komen. Spec-vraag: WF-020 verbreden tot
`prioritizedDefaults` en tot een non-null-eis, of een eigen nummer. Naast
de al genoteerde open flanken van rij 39.

### Voorstel (a) — CLAUDE.md, gateslijst, letterlijke tekst

```
14. `scripts/check-flogic.sh` — the F-logic frontend (`frontends/flogic`,
    plain OCaml in the workspace's dune build, so no fetched jars and no
    external compiler) builds and its own suite passes with a non-zero
    check count: the suite emits every test program, holds it
    verifier-clean, replays its cases through `ref` AND clingo, and drives
    one fixture per refusal, checking both the reason and the citation of
    the mandate document the refusal list belongs to. Every corpus golden
    carrying a `bron.flogic` re-emits through the frontend
    byte-identically after canonicalisation (gate 2's serialiser shape,
    executed from that script — one source of truth, as in gate 9). An
    empty flogic-golden set, a suite that ran zero checks, or a missing
    dune is a failure, never silently green. Counting floors in the
    script: `EXPECTED_MIN_CHECKS=81` and `EXPECTED_MIN_GOLDENS=1`, both
    measured 2026-09-12. CI leg unconditional and blocking — unlike gate
    9 this frontend links no external toolchain, so every runner can run
    it.
```

### Voorstel (b) — CONSTRUCTS rij 39, deltazin

> A SECOND frontend landed the same day (2026-09-12, card
> flogic-frontend): `frontends/flogic` lowers a minimal F-logic slice —
> syntactically a subset of Flora-2 2.2 — to the same `supertype` field
> and the same inherited defaults stack, with
> `corpus/golden/jutterswet-wrakrecht-flogic` as the anchor's SECOND
> source twin (the same four case files byte-identically; gate 14's
> byte-identical re-emission, gate 12's played channel 132 -> 133). That
> is what makes the field LANGUAGE-neutral rather than dialect-borne: two
> source languages sharing no grammar and no code reach one IR, and the
> frontend diff against `jutterswet-wrakrecht` closes at **52 differing
> lines in four ADMINISTRATIVE categories with no fifth** — 27 of source
> provenance (`bron`/`onderwerpBron`, read by no executor), 12 of literal
> spelling with its own annotation (the hand anchor already spells the
> F-logic form, and §2.10's effective-type law is why all four executors
> pin the same values across it), 8 of declared surface names F-logic has
> no grammar for (`meervoud` null, `bezield` false, one label on a
> to-many role, so the shared case key falls on `Rol.naam` — a shape
> `jutterswet-kaveltelling` already carries), and 5 of `regels` index
> order. WF-106's witness is doubled and, for the first time, EXERCISED
> rather than constructed: F-logic can WRITE two `::` facts, so the
> refusal fires on a real second parent instead of on a spelling the
> dialect has to be handed. Three lowerings the mandate's slice did not
> fix, taken on the generic reading and argued on the card: a fact type is
> a class marked `x : feittype` carrying its two role signatures (F-logic
> has no named binary relation, and Flora-2 2.2 has no `=>>` — a role's
> arity is a cardinality brace, measured); priority comes from the SHAPE
> of a definition (unconditional = base, conditional = exception, a target
> with only an unconditional rule stays the ch. 7.8 standing norm, and a
> barred class-level value is always a base) because F-logic's own
> ordering device is defeasible reasoning, which the slice refuses; and a
> ground instance fact refuses with a pointer to the `uchar-case-v0`
> document, since the IR has no instance channel. The frontend also
> requires an id on every form that puts a name on the wire and refuses a
> duplicate across both kinds, because nothing downstream does: WF-020
> filters null names out and never reaches a `prioritizedDefaults` name
> (open spec question, recorded on the card). Flora-2 2.2 was run over the
> use case's inheritance skeleton as a documented external comparison
> (never a gate, never a fourth leg): under the child-wins reading of
> §11.2 five of the six cells agree exactly — 10 / 99 / 20 / 99 / 55 —
> and on the sixth uchar's single 55 lies inside Flora's unchosen
> {55, 99}, so what divides them there is determinism, not direction.

### Voorstel (c) — `.github/workflows/gates.yml`, job

```yaml
  flogic:
    # Gate 14 — the F-logic frontend (card flogic-frontend, 2026-09-12): a
    # second source language reaching the same kernel subtyping. Needs
    # OCaml (the frontend, the verifier and ref) and gringo (the suite
    # replays every case through the ASP backend too), and nothing else —
    # this frontend links no external toolchain, so unlike gate 9 the leg
    # is unconditional and blocking. Invoked with BASH, like gates 10-13:
    # the script uses process substitution and is a syntax error under sh.
    name: "Gate 14: F-logic frontend"
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ocaml/setup-ocaml@v3
        with:
          ocaml-compiler: "5"
      - name: install yojson + zarith
        run: opam install -y yojson zarith
      - name: install clingo
        # Ubuntu ships the clingo binary in the gringo package
        run: sudo apt-get update && sudo apt-get install -y gringo && clingo --version
      - name: check-flogic
        run: opam exec -- bash scripts/check-flogic.sh
```
