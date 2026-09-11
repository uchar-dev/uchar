# RegelSpraak-dialectextensie: `is een` — objecttype-overerving in de kopvorm

Goal: de kopvorm `Objecttype de RechtOpUitkeringWw is een RechtOpUitkering (mv: …)` als uchar-dialectextensie in `frontends/regelspraak`, naar het stramien van de prioritized-defaults-extensie: strikt additief (alle officiële documenten en het hele bestaande corpus parsen en emitten byte-identiek), lexicaal minimaal, gelowered naar de kernel-subtypering van `20260910-subtype-kernel`, en geregistreerd in `grammar/DIVERGENCES.md` als tracked divergence van de v2.3.0-arbiter. Onderzoek en besluit onderweg: de interactie van `is een` met bestaande kopannotaties (bezield, mv), het gedrag van kwantificatie- en navigatievormen zodra subtypen bestaan, en de frontend-weigeringslijst (tweede ouder, zelfverwijzing, onbekend supertype) — elk met een negatieve getuige; een target dat op twee niveaus regels draagt is géén weigering maar volgt de groepserving van opdrachtbesluit 3, met de kernel-getuigen als semantische maat. Neem in de kaartafronding de notitie op dat de werkbank-generator (`wetsanalyse/herleving-ww/scripts/bouw-gegevensspraak.py`, buiten deze repo) de eerste afnemer is: die emit na landing de 17 gepinde CGM-overervingsparen als `is een` in plaats van platslaan.

Acceptance: de use-case-golden bestaat in dialectvorm en speelt groen door de frontend → verifier → executorreeks; de negatieve getuigen weigeren luid; `grammar/DIVERGENCES.md` draagt de registratie; byte-identieke heremissie van het bestaande corpus is aangetoond; de volledige gate-reeks is groen.

May touch: `frontends/regelspraak/` (grammatica, lowering, fixtures), `grammar/DIVERGENCES.md`, `corpus/` (de dialect-golden), en de eigen kaart.

Out of scope: kernel- of executor-wijzigingen (voorganger-kaart); GegevensSpraak-spec-wijzigingen richting de officiële arbiter (wij registreren divergentie, wij herschrijven de arbiter niet); de F-logic-frontend; meervoudige overerving.

## Aangescherpt door de onderzoekskaart (2026-09-11)

`spec/proposals/SUBTYPING.md` §9.2 is bindend; hieronder de
uitvoerbare neerslag.

**Kopvorm, positie beslist.** De productie luidt vandaag
(`frontends/regelspraak/grammar/RegelSpraak.g4:311`):

```
objectTypeDefinition
    : OBJECTTYPE bepaaldLidwoord? objectTypeNaam
      ( MV_START meervoudsvorm RPAREN )? BEZIELD?
      objectTypeMember*
```

De onderlinge volgorde van de annotaties **ligt dus al vast in de
grammatica** — de `mv:`-groep gaat vóór `(bezield)`, er is geen
alternatief — en `Objecttype de Jutter (mv: Jutters) (bezield)` is die
volgorde, geen vrije keuze. De extensie voegt één optioneel element toe
ná `objectTypeNaam` en vóór de `mv:`-groep:

```
Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
```

**Géén nieuw lexertoken.** Een heelfrase-token `IS_EEN : 'is' WSP
'een'` is hier niet veilig, en het `IS_VAN_HET_TYPE`-precedent gaat
niet op. Lexen is niet kop-lokaal: onder maximal munch vuurt zo'n token
overal waar de frase staat, en die staat al in **regeltekst** —
`corpus/golden/jutterswet-schouwrooster/bron.rgs:15` en `:22` ("Een dag
is een schouwdag"), `corpus/golden/jutterswet-postwacht/bron.rgs:66`,
plus zeven `toka/`-bronnen. Dat zou gate 10's byte-identieke
heremissie breken op documenten die niets met subtypering te maken
hebben. `IS_VAN_HET_TYPE` (`RegelSpraakLexer.g4:187`) is alleen veilig
omdat die frase nergens anders voorkomt. De oplossing zit op
**parserniveau**: de bestaande tokens `IS` (`RegelSpraakLexer.g4:284`)
en `EEN` (`:378`) achter elkaar op de kopplek, waar de context
disambigueert. Registratie in
`frontends/regelspraak/grammar/DIVERGENCES.md` naast de
prioritized-defaults-extensie, onder hetzelfde
inert-op-officiële-documenten-argument.

**Golden.** `corpus/golden/jutterswet-wrakrecht` — dezelfde use case
als de kernel-anchor `anchor/jutterswet-strandrechtketen`, nu met
`bron.rgs`, dus onder gate 10's byte-identieke heremissie. Samen met
`corpus/golden/jutterswet-wrakrecht-flogic` (F-logic-kaart) vormt dit
het frontend-diff-paar.

**Weigeringslijst, elk met een fixture.** (1) twee `is een`-clausules op
één kop, of `is een A en B` — de tweede ouder, met de twee CGM-gevallen
`RechtOpUitkeringWazo` en `RechtUitkeringZw` bij naam in de melding;
(2) zelfverwijzing `X is een X`; (3) onbekend supertype — resolutie is
documentbreed en volgordevrij, dus een voorwaartse verwijzing is géén
fout, alleen een ongedeclareerde naam is dat; (4) `is een` dat iets
benoemt dat geen objecttype is (een domein, een parameter); (5) een
geshadowd lid — de frontend weigert vóór de verifier, want zij heeft de
keten bij de hand (kernel: WF-105).

**Expliciete niet-weigering.** Een doel dat op twee niveaus regels
draagt is geen fout: opdrachtbesluit 3 heeft het eerdere blanket-verbod
ingetrokken, en de kernelgetuigen zijn de semantische maat.

**Twee gevolgen die de kaart moet meenemen.**

- **Emitteer rolbindingen zodra je een subtype emitteert.** WF-048's
  uniciteit wordt over de ketengesloten typeparen berekend, dus een
  bindingloze navigatie die vandaag welgevormd is kan door het
  toevoegen van een subtype dubbel verbonden raken en luid omvallen.
  `verbindendeRol` / `bronRol` meegeven neemt dat weg.
- **De laaggrensordening heeft een oppervlak nodig — mits zij er komt.**
  De kernel stelt `geerfdeUitzonderingen` ∈ `boven` | `onder` |
  `ongeordend` (WF-106) op de defaults-groep voor; de dialect-extensie
  zou de eerste producent ervan zijn en moet er dan een schrijfwijze
  voor kiezen in het `Uitzondering op` / `Uitzonderingen
  ongeordend`-blok. **Het veld is een aanbeveling, geen geratificeerd
  besluit:** SUBTYPING §11.2 legt de kleinere variant — laaggrens
  altijd ongeordend — aan de maintainer voor. Wacht dat punt af voor je
  oppervlak ontwerpt.
