# Fase 5a, kaart 2: de Jutterswet in Catala — het corpusgezicht

Na kaart 1 (het skelet). Besloten 2026-08-21 (besluit 2A).

Goal: Jutterswet art. 3/5 (penningrecht met het hardheidspatroon)
zelf geschreven in Catala, door de plugin naar IR, als golden in het
corpus met cases door de volledige gate-set — en de frontend-diff:
hetzelfde artikel door de RegelSpraak-toolchain en door de plugin,
de IR-diff getriageerd (dialectverschil / IR-ontwerpfout →
spec-delta / frontend-bug), gedocumenteerd als de eerste 5d-oefening.

Acceptance: de golden draait door alle acht gates; het diff-verslag
staat in het Result of een spec-notitie; twee lenzen.

May touch: `frontends/catala/`, `corpus/`, `spec/` (alleen
diff-bevindingen als delta), `backlog/`.

Out of scope: constructen buiten het 5a-fragment.

Gereedschapsnotitie (2026-08-21, uit de installatiesessie): naast
catala en clerk staan lokaal catala-format 1.2.0 (topiary; `-i`
in-place, idempotentie-check standaard, locale via bestandsextensie)
en catala-lsp/catala-dap. Voor deze kaart: (a) de eigen
.catala_en-bronnen door catala-format halen vóór ze het corpus in
gaan (stijldiscipline zoals de canonieke JSON-ser); (b) bij de
frontend-diff dient `catala interpret` als DERDE oordeel naast ref
en clingo — de Catala-native uitkomst op hetzelfde programma maakt
de triage (dialectverschil / IR-fout / frontend-bug) beslisbaar in
plaats van beredeneerd, met catala-dap als stapdebugger wanneer de
uitkomsten uiteenlopen.

Besluit-6-notitie voor 5c (2026-08-21, uit kaart 1): het skelet
weigert context-variabelen — ch. 7.8's input-default is Reading A en een
enkel-scope-programma heeft geen caller, dus de door de maintainer
gekozen top-default-mapping (optie A) wordt pas realiseerbaar wanneer
subscope-calls vertalen: de caller-waarde wordt dan een GEGENEREERDE
top-default in de defaults-groep van de aangeroepen scope, geen
standing-machinerie. Wie 5c oppakt begint daar.

Frontend-diff-triage (2026-08-21, de eerste 5d-oefening; hetzelfde
artikel door twee frontends, structureel vergeleken):

1. KERNUITKOMST: de defeasibiliteitsstructuur is isomorf — beide
   frontends leveren een kenmerk-defaults-groep met de art. 5-default
   (ALLE[hardheidsgrond, ≥6]) bóven de art. 3-default (≥12), zelfde
   besluiten, zelfde volgorde. De waist draagt twee bronnen.
2. Dialectverschillen (geen actie): naamstijl (underscores vs
   spaties — identifier-regime van de brontaal); objecttype
   Penningrecht (Catala's scope als berekeningseenheid) vs Jutter
   (RegelSpraak's rechtssubject) — de scope-mapping abstraheert het
   subject; bron-strings kaal vs verhalend.
3. Structurele verschillen zonder gedragsverschil (lens-getoetst):
   (a) de Catala-render draagt een derde, onvoorwaardelijke
   besluit-FALSE-basisdefault (uit `equals false`) waar het anker
   stilte impliciet laat — de lens BEWEES dat die default gedragsmatig
   inert is: kenmerkattributie is monotoon (Char false schrijft
   niets, identiek aan geen besluit; ref/lib/eval.ml), en de
   input-kenmerk-proef laat de input op BEIDE IRs staan in beide
   executors — mijn eerdere lezing dat de default input zou verdringen
   was onjuist en is hier gecorrigeerd; (b) het tekenrooster:
   Catala's `integer` is voortekend, dus de vondstentelling draagt
   teken=ONBEPAALD waar het anker NIET_NEGATIEF authort — een echt
   type-roosterverschil zonder gedragsgevolg op deze cases; (c)
   meervoud=null en bezield=true op de scope-als-objecttype — de
   scope is een berekeningseenheid, geen rechtssubject; de
   bezield-vlag is een mapping-detail dat 5c mag aanscherpen.
4. Veldnotitie (PHASE4 §2.2): catala 1.2.1 dedupliceert gelijke
   exceptiewaarden (ongelijke conflicteren nog) — de
   equal-decisions-deviatie is tegen de shipping compiler versmald
   tot precies onze lezing.

May-touch-verruiming (2026-08-21, expliciet per huisprotocol):
scripts/check-noop.sh is geraakt voor de twee reden-dragende
survivorpins — gate-onderhoud dat de nieuwe golden afdwingt (het
patroon van elke eerdere anchor-kaart met stiltehoeken), gedekt door
het staande mergemandaat voor beschermde paden; geen ontwerpbesluit.
Ook de brontekst draagt nu de canonieke art. 3/5-formulering uit
corpus/jutterswet/wet-v2.md met een expliciete modelleringsnotitie
(de eerdere kop herformuleerde de artikelen stil — lens-vondst).

Result (2026-08-21): de Jutterswet spreekt Catala. De golden
corpus/golden/jutterswet-penningrecht-catala/ draagt art. 3/5 in de
ketenvorm (artikel 5 als gelabelde exceptie óp artikel 3 — de lex
specialis als structuur), met de canonieke wetstekst uit wet-v2.md en
een expliciete modelleringsnotitie; de IR is door de plugin
gegenereerd en door gate 2's eigen serializer gecanoniseerd; de vijf
ankergevallen zijn drievoudig geverifieerd — catala interpret (het
derde oordeel), ref en clingo geven op alle vijf exact het antwoord
dat het handgeschreven hardheidsanker pint — en de twee stiltehoeken
staan als reden-dragende no-op-survivors gepind (elk met zijn eigen
precieze reden, naar het ankermodel). De frontend-diff-triage staat
hierboven op de kaart: de defeasibiliteitsstructuur is isomorf tussen
de twee frontends (de eerste echte 5d-validatie van de waist), de
naam-/subjectverschillen zijn dialect, en de besluit-false-basisdefault
bleek na lens-toetsing gedragsmatig inert — mijn eerdere
verdringings-lezing was onjuist en is gecorrigeerd; het
tekenroosterverschil (voortekend integer vs niet-negatief geauthord)
en meervoud/bezield zijn als mapping-details genoteerd voor 5c. De
lensronde leverde verder: de veldnotitie dat catala 1.2.1 gelijke
exceptiewaarden dedupliceert (PHASE4 §2.2 — de
equal-decisions-deviatie is tegen de shipping compiler versmald tot
onze lezing), de canonieke-tekst-correctie, en het procedurele punt
dat de scripts/check-noop.sh-verruiming expliciet op de kaart moest
(genoteerd; gedekt door het staande mandaat — survivorpins zijn
gate-onderhoud, geen ontwerpbesluit). Rood-eerst-notitie, eerlijk:
de verwachte weigering bleef uit — het skelet bleek de boolean-keten
al te dragen (base-false → besluit-false-default); het "rood" van deze
kaart was gate-niveau: check-ir-schema (stijl) en check-noop
(ongepinde survivors) faalden eerst en zijn met de canonicalisatie en
de pins groen gemaakt. Bewust gelaten: de kenmerk-input-hoek heeft
geen eigen case (gedragsidentiek bewezen, dus niets te pinnen); de
tutorial blijft wachten op 5b; datum-vergelijkingen blijven ref-only.
