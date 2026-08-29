# Hernoeming: ladder → prioritized defaults (wire-epoch v0.10)

Goal: de volledige, in één beweging uitgevoerde hernoeming van het
gemunte "ladder/rung/sub-ladder"-vocabulaire naar prioritized
defaults / default / exception group, conform
HERNOEM_LADDER_20260829.md (geratificeerd 2026-08-29, V1–V8; archief
inbegrepen per V4-B): spec (IR-SPEC, RUNTIME-SEMANTICS, CONSTRUCTS,
ASP-TRANSLATION, PLAN integraal), schema + wire (vijf spellingen,
epoch v0.10, geen alias), verifier, ref, clingo, drools, beide
frontends incl. tests (V7), corpus (golden-regeneratie byte-identiek;
twaalf negatief-dirs via git mv; NOTE's incl. de NL-metafoor "sport"),
scripts, CLAUDE.md, de vier open kaarten, én het archief —
spec/proposals/ en backlog/done/ inhoudelijk plus de zes git mv's van
§2 — met de formerly-noot en de naamsverantwoordingsalinea (bijlage A)
als brug naar de git-geschiedenis.

Acceptance: alle twaalf gates groen met output; de §6-grep levert
exact de reason-carrying restlijst (formerly-noten + eventuele
vals-positieven; rood getoond bij aanvang); geen verwachtingswaarde
gewijzigd.

May touch: spec/ integraal (IR-REVIEW.md: nulmeting), corpus/golden,
corpus/negatief, corpus/jutterswet (NL-casusprose — valt onder de
§6-grep), ref/, backends/, frontends/, scripts/, CLAUDE.md,
backlog/new/*.md, backlog/done/**, spec/proposals/**, backlog/
(kaartverplaatsing), HERNOEM_LADDER_20260829.md.

Out of scope: toka/, corpus/rood (nulmeting), git-geschiedenis en
commitboodschappen, de bestandsnaam HERNOEM_LADDER_20260829.md,
regelspraak-java (volgt als pin-bump-kaart), calculemus-cursus.

Claim-notitie (2026-08-29, nulmeting op main = 8c6a992): de §6-grep
telt 2525 treffers — rood getoond vóór aanvang, conform fase A. Twee
afwijkingen van de §3-inventaris (gemeten op 55a7e1b, vóór de
typerfix-ronde): (1) de negatief-mv-lijst van fase E telt VEERTIEN
dirs, niet twaalf — `wf086-temporele-laddertarget` en
`wf086-temporele-ladderkenmerk` (typerfix-ronde) komen erbij als
`wf086-temporele-defaultstarget` / `wf086-temporele-defaultskenmerk`
(stamregel §1.2); (2) backlog/new/ is leeg — de "vier open kaarten"
van fase J zijn geland en vallen als done-kaarten onder de
V4-B-archiefbehandeling (V8's letter ingehaald, zoals bij ratificatie
voorzien). Extra binnen het contract: corpus/jutterswet/-prose (4
NL-treffers) valt onder de §6-grep en gaat mee.

Result: GELAND (2026-08-29). De volledige hernoeming in één beweging:
wire-epoch v0.10 (vijf spellingen, geen alias — een oud bestand faalt
luid in gate 2 én gate 3, boobytrap-bewezen), spec (IR-SPEC ch. 6.1 e.o.,
RS ch. 6/7/8.2/9.5, CONSTRUCTS, ASP-TRANSLATION, PLAN integraal), schema
(defs + velden + epoch-noot), verifier, ref, clingo, drools, beide
frontends incl. tests (V7), corpus (42 golden/negatief-ir.json's
gesubstitueerd; regeneratie byte-identiek via gates 2/9/10), scripts,
CLAUDE.md, én het archief per V4-B (spec/proposals 8 bestanden,
backlog/done 61 kaarten). Mv's: VEERTIEN negatief-dirs (12 uit het plan
+ wf086-temporele-defaultstarget/-defaultskenmerk, na de §3-inventaris
ontstaan), 6 archiefbestanden, 4 codebestanden, en — buiten het plan om,
zelfde V6-lijn — het casusbestand case6-rung-overschrijft.json →
case6-default-overschrijft.json (3 verwijzingen mee). Alle twaalf gates
groen met exact de pre-hernoemingsgetallen: 160/168 (gate 3), 396, 374,
374, 338+36, 22, 169+11 byte-identiek (gate 9), 144+23 byte-identiek
(gate 10), 29 slices/45 rgs/holdouts {toka-23,toka-45} (gate 11),
71/63/3/1 (gate 12). Geen verwachtingswaarde, pin of WF-nummer gewijzigd
(tokendiff over alle ~212 bestanden, twee lenzen onafhankelijk);
corpus/rood en toka/ byte-identiek.

Twee adversariële lensrondes, elk met fixronde vóór de merge:
- Lens 1 (2 MAJOR): (a) "exception group" kreeg twee referenten →
  opgelost met de TWEE-TERMENLIJN, een post-ratificatie-precisering van
  §1.3: "exception group" is uitsluitend het LID (de wire-node, één
  default-keten in `leden`); de container heet "the `excepties` field"
  of het meervoud; "sub-group" is volledig uitgefaseerd (57 tokens
  spec + schema + verifier + NOTE's + comments). (b) het §6-contract
  beloofde faalt-wanneer-verouderd maar niets handhaafde het → de
  RENAME-TRIPWIRE in scripts/check-self-contained.sh: elke oude-termregel
  moet een reden-dragende allowance matchen én elke allowance moet nog
  bestaan; rood-rood-groen getoond; omzeilpogingen (runGolden-strip,
  kaart-hernoeming) dichtgetest door lens 2.
- Lens 2 (3 MAJOR, geveegd): NL-cognaat "sub-groep" (21 treffers, 12
  bestanden, incl. identifier bevatMeervoudigeSubgroep →
  bevatMeervoudigeExcepties); ~26 enkelvoud-containerlezingen in
  code-comments naar de twee-termenlijn; categoriefouten in drie
  negatief-NOTE's. Plus: de tripwire dekt nu ook PADNAMEN
  (git ls-files, rood-groen getoond).

De §6-acceptatiegrep levert exact deze reden-dragende restlijst
(de tripwire handhaaft haar mechanisch; het tripwire-script excludeert
zichzelf — het spelt de patronen die het jaagt):
1. spec/IR-SPEC.md:2069 + spec/RUNTIME-SEMANTICS.md:2339 — de twee
   formerly-noten (de brug naar de git-geschiedenis);
2. spec/RUNTIME-SEMANTICS.md:2343 en :2363 — de bijlage-A-naams-
   verantwoording (opent met de oude naam; sluit met "ladder logic
   (IEC 61131-3)" als disambiguatie);
3. deze kaart zelf (titel, Goal, claim-notitie — een hernoemingskaart
   citeert haar onderwerp);
4. drie runGolden-vals-positieven (CaseRunner.java:73,
   DroolsBackendMain.java:32, backlog/done/20260827-drools-backend.md:145
   — substring, geen laddervocabulaire; de allowance hergrept de regel
   met dat token gestript).

Bewuste keuzes en aanvaarde restpunten, eerlijk:
- check-noop.sh-survivorreden "kenmerktoekenning rungs, not a ladder"
  werd "rules, not a defaults group" — redactioneel (feitelijk juister:
  die golden draagt geen prioritizedDefaults), geen stamsubstitutie.
- Twee NL-maintainerscitaten in backlog/done/20260828-clingo-factor1-
  eenheid.md gingen per V4-B inhoudelijk mee ("het ladder ontwerp" →
  "het defaults-ontwerp"); het citaat leest daardoor anders dan de
  oorspronkelijke uitspraak — de git-geschiedenis draagt de originele
  vorm.
- NL-containervariatie blijft: "defaults-groep" / "defaults-bundel" /
  "defaultsgroep" naast elkaar in prose (lens-2-MINOR; één referent,
  geen dubbelzinnigheid — normaliseren zou de diff ver voorbij pure
  substitutie trekken).
- Regelbreedte: substitutie maakte ~83 prosaregels langer dan de
  wrap-conventie; bewust niet hergewrapt (diff-hygiëne).
- `exceptions` als IR-SPEC-metamodelnaam (Engels) met v0-wire-spelling
  `excepties` is pre-existente huisconventie, ongewijzigd.
- toka/ onaangeraakt (original form, nulmeting 0 treffers); de
  gate-11-holdoutbestandsnamen (toka-23/toka-45-*.rgs) vallen onder de
  toka-exclusie van tripwire én §6-grep.
