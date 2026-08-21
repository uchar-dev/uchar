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
weigert context-variabelen — ch. 7.8's input-rung is Reading A en een
enkel-scope-programma heeft geen caller, dus de door de maintainer
gekozen top-rung-mapping (optie A) wordt pas realiseerbaar wanneer
subscope-calls vertalen: de caller-waarde wordt dan een GEGENEREERDE
top-rung in de ladder van de aangeroepen scope, geen
standing-machinerie. Wie 5c oppakt begint daar.

Frontend-diff-triage (2026-08-21, de eerste 5d-oefening; hetzelfde
artikel door twee frontends, structureel vergeleken):

1. KERNUITKOMST: de defeasibiliteitsstructuur is isomorf — beide
   frontends leveren een kenmerk-ladder met de art. 5-rung
   (ALLE[hardheidsgrond, ≥6]) bóven de art. 3-rung (≥12), zelfde
   besluiten, zelfde volgorde. De waist draagt twee bronnen.
2. Dialectverschillen (geen actie): naamstijl (underscores vs
   spaties — identifier-regime van de brontaal); objecttype
   Penningrecht (Catala's scope als berekeningseenheid) vs Jutter
   (RegelSpraak's rechtssubject) — de scope-mapping abstraheert het
   subject; bron-strings kaal vs verhalend.
3. Eén écht bronsemantiekverschil, IR-uitdrukbaar aan beide kanten
   (dus bewijs van expressiviteit, geen IR-ontwerpfout): de
   Catala-render draagt een derde, onvoorwaardelijke
   besluit-FALSE-basisrung (uit `equals false`); het anker laat
   stilte impliciet. Catala's false ís een beslissing, RegelSpraak's
   stilte niet — waarneembaar pas bij case-input op het kenmerk (de
   besliste ladder verdringt input; het anker laat hem staan).
   Onze cases raken die hoek niet; genoteerd als flank.
4. Veldnotitie (PHASE4 §2.2): catala 1.2.1 dedupliceert gelijke
   exceptiewaarden (ongelijke conflicteren nog) — de
   equal-decisions-deviatie is tegen de shipping compiler versmald
   tot precies onze lezing.
