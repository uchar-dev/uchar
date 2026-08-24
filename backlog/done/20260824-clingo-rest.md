# De logica-rekenaar-rest: magnitudegrens en struct-invoerwaarden

Goal: twee gedocumenteerde clingo-flanken dichten. (1) De
magnitudegrens van de gesplitste geldvermenigvuldiging: het
intermediair op schaal 10^8 overschrijdt int31 bij legale invoer
(tutorial: kids >= 6) terwijl ref exact doorrekent en het
EINDresultaat representeerbaar is — verruim de encodering (bijv. een
verdere splitsing of een kleinere intermediaire schaal met bewezen
afronding, in de stijl van het bestaande
gesplitste-vermenigvuldigingsbewijs incl. ties) zodat de tutorialvorm
ruim voorbij kids=6 draagt; de hek-conditie blijft luid voor wat
werkelijk niet past. (2) De struct-VALUE-vorm: samengestelde
(geneste) structs als case-invoer/verwachting faulten in
struct_value_term — maak die recursief, consistent met de
struct_base-landing. Voor beide: rood-eerst, eigen corpus-getuigen
met absolute verwachtingen door beide executors, ASP-TRANSLATION
bijgewerkt (S18 resp. S16; S19-schrappingen waar gedicht; S19 blijft
de hoogste sectie), en de kids<=5-notities in de
tutorialfixture-header en S18 geactualiseerd.

Acceptance: alle negen gates groen; rood-eerst per flank; ref ≡
clingo over de volle corpus; twee opus-lenzen verwerkt vóór de merge.

May touch: backends/clingo/, corpus/golden/ (nieuwe getuigen),
corpus/rood/ (indien een hek-getuige past), spec/ASP-TRANSLATION.md,
frontends/catala/test/tutorial_end_2_3.catala_en (alleen de
grens-notitie), scripts/check-noop.sh (alleen indien nodig),
backlog/. GEEN spec/CONSTRUCTS.md (registry-conflictles — de
rij-notitie doet de maintainer-loop bij de landing). Out of scope:
ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/verifier/,
frontends-code.

## Voortgang 2026-08-24

Beide flanken gedicht in `backends/clingo/lib/translate.ml`; alle negen
gates groen in de worktree; ref ≡ clingo over de volle corpus (207 cases).

### Flank 1 — magnitudegrens gesplitste geldvermenigvuldiging

De bindende hek was niet de gesplitste geldproduct-partial maar het
scale-10^8 intermediair van de EXACTE deling die de 4-decimale rate
berekent (`1 + kindertal / 2.0000`). De exacte geschaalde quotient
`L·(s_R·m)/(R·s_L)` prescaleert de teller met de delerschaal `s_R = 10^4`,
dus `n·10^8` — hek `|n| > 5`.

Ontwerp: een **constante deler** `c` (hier `2.0000` → `c = 20000`) laat de
volledige constante breuk `(s_R·m)/(c·s_L)` door zijn **volle gcd** reduceren
vóór grounden, zodat de delerschaal uit de teller valt: `n·5000 / 1`, hek
`|n| > 107374`. Bewezen exact in ASP-TRANSLATION S18(c): gereduceerde en
ongereduceerde vorm duiden dezelfde rationale; clingo's `/` is de floor en
`\ == 0` de deelbaarheid, beide invarianten van de rationale — waarde,
niet-exacte `viol/2` en nul-hek onveranderd. De cent-afronding (incl. ties,
half-up) van het geldproduct is NIET aangeraakt; die staat als de bindende
hek en vuurt luid bij `n ≥ 41` voor `$10.000 · (1 + n/2)` (draagt `n ≤ 40`).
Een RUNTIME 4-decimale deler houdt de tweefactorvorm (geen corpus-case).

Rood-eerst (huidige viol-uitvoer, vóór de fix), getuige `jutterswet-kinderweging`
(nieuw golden, tutorialvorm, `n = 20` ruim voorbij 6, plus `n = 6` de oude
grens):
`clingo: UNSATISFIABLE — a fail-fast guard fired: viol("arithmetic range exceeded (clingo grounds in machine integers)","J1")`
Na de fix: `clingo: PASS`; ref: `oracle: PASS`.

Kids<=5-notities geactualiseerd: fixture-header `tutorial_end_2_3.catala_en`
(alleen de grens-notitie → `n ≤ 40`, hek bij `n ≥ 41`) en S18(c).

### Flank 2 — struct_value_term recursief

`struct_value_term` is nu `let rec`: een struct-getypeerd veld recurseert
(één geneste functor per niveau), consistent met de recursieve `struct_base`
(veldtoegang). Sum-/list-getypeerde veldwaarden faulten nog (nieuwe reden
`a nested sum/list field value has no integer encoding in the ASP struct
term (no corpus case)`). De check-zijde (`model.ml`) deelt de emissie + de al
recursieve termparser — geen wijziging daar nodig. S16 (nieuw getuige-alinea)
en S19 (schrapping "nested struct field" uit de niet-gedragen lijst) bij.

Rood-eerst (vóór de fix): `TRANSLATION FAULT: a nested struct/sum/list field
value has no integer encoding in the ASP struct term (no corpus case)`.
Getuige `jutterswet-waarderingsinvoer` (nieuw golden): geneste
`waardering{toeslag, kern{eenheden, grondslag}}` als case-INVOER én
-VERWACHTING, plus `grondslagkopie := waardering.kern.grondslag` (struct_base
over de invoer). Na de fix: `clingo: PASS (4)`, ref: `oracle: PASS (4)`.

### check-noop.sh

Niet gewijzigd — beide nieuwe goldens hebben berekende verwachtingen en
weerleggen de no-op vanzelf (geen survivor-toevoeging nodig).

### Onopgelost / maintainer-loop

- `spec/CONSTRUCTS.md` rij 38 zegt nog "a nested struct/sum field *value*
  (input/expectation) has no flat-term encoding (S19)" — nu deels achterhaald
  (nested struct value landt; sum/list value nog niet). NIET aangeraakt per
  kaart (registry-conflictles); rij-notitie bij de maintainer-landing.
- Twee opus-lenzen vóór de merge staan nog open (review-stap van de
  orchestrator/maintainer).

## Lensronde 2026-08-24

Twee lens-bevindingen (MAJOR/CONFIRMED), beide dezelfde wortel op
`backends/clingo/lib/translate.ml:1348` — de constante-deler-fold liet de
hek op de **noemerzijde** vallen. Zelf geverifieerd vóór de fix (hand-IR
buiten de corpus, `q := Afgerond(x / c, MODUS, dec)`):

- Bevinding 1 (`c = 2000000001`, REKENKUNDIG, 2 dec, x=1): ref `oracle:
  PASS` (0.00); clingo `MISMATCH: R1.q = [-6]` — een STABIEL model (SAT,
  geen viol) met een fout antwoord −0.06. NAAR_BOVEN/WEG_VAN_NUL (x=2000000):
  ref 0.01, clingo `[-1]` (−0.01). Stil ref≠clingo, de kardinale defectklasse.
- Bevinding 2 (`c = 214748.3647`, geschaald 2³¹−1, copriem met 10,
  REKENKUNDIG, 4 dec, x=1): ref `PASS` (0.0000); clingo `MISMATCH: R1.q =
  [973741824]` — grof fout, SAT, geen viol.

Oorzaak: de fold reduceert de constante breuk en emitteert de **gereduceerde
noemer** `kden_c = c·s_L/k` als grondconstante die de afrondingsformules
ongehekt in `2·dent` resp. `2·numt+dent` voeren. `numt` is gehekt tot 2²⁹,
maar `kden_c` niet — voorbij 2²⁹ verlaat `2·dent` int31 en **wrapt stil**
(clingo 5.8.2: `2·2000000001` grondt naar een negatief getal). Een grondhek op
de al-gewrapte literal kan niet vuren, dus dit is niet met een grond-`range_guard`
te dichten.

Fix (smalste): de fold geldt alleen zolang `kden_c ≤ 536870912` (dan blijven
`2·dent`, `numt+dent−1`, `2·numt+dent` binnen 2³¹). Voorbij die grens valt de
tak terug op de **tweefactorvorm**, wier `(t_R, 536870912/kden)` `range_guard`
de delerterm hekt en de luide `arithmetic range exceeded` UNSAT vuurt — precies
het gedrag dat elke constante deler had vóór de fold bestond. Na de fix vuren
alle vier repro-gevallen die UNSAT; de kleine-deler-fold (`/3`, `/2.0000`) blijft
byte-identiek en rekent exact door (ref ≡ clingo `PASS`).

Geen corpus-getuige mogelijk: ref rekent zo'n quotiënt exact (geen fault), dus
noch een golden (ref-PASS vs clingo-UNSAT divergeert) noch een rood-getuige
(rood eist fault in BEIDE executors, ref faalt niet) past. Delen door >10⁹ is
exotisch in recht; het punt is de luide hek, niet een verbrede encodering.

ASP-TRANSLATION S18 bijgewerkt met de noemerhek-alinea (de fold geldt alleen
binnen de int31-hek; erbuiten vuurt de tweefactor-`range_guard`). `spec/CONSTRUCTS.md`
NIET aangeraakt (registry-conflictles). `scripts/check-noop.sh` NIET gewijzigd.
Alle negen gates opnieuw groen (slotregels):

- `check-self-contained: PASS`
- `check-ir-schema: PASS (schema valid; 86 file(s) validate and round-trip)`
- `check-verifier: PASS (86 golden clean; 138 negatief rejected for their declared rule)`
- `check-case-schema: PASS (schema valid; 224 case file(s) validate)`
- `check-oracle: PASS (207 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
- `check-clingo: PASS (207 cases replay exactly through clingo — ref = clingo over the corpus)`
- `check-noop: PASS (183 cases refute a do-nothing executor; 24 pinned survivors pin defeasible standing)`
- `check-rood: PASS (17 red cases fault observably in both executors)`
- `check-catala: PASS — frontend suite green (128 checks); 7 catala golden(s) regenerate byte-identically`

Result: beide flanken gedicht. (1) De magnitudegrens zat niet in het
geldproduct maar in de exacte deling met constante deler: de
delerschaal (10^4) prescaleerde de teller naar 10^8. De constante
breuk reduceert nu door zijn volle gcd vóór het gronden, bewezen in
ASP-TRANSLATION S18(c) (gereduceerd en ongereduceerd duiden dezelfde
rationale; waarde, niet-exact-viol en nul-hek onveranderd); de
tutorialvorm draagt nu tot n=40 kinderen (cent-afrondingshek), was
n≤5 — getuige jutterswet-kinderweging (n=20 en n=6). De runtime-
variabele deler houdt de oude tweefactorvorm (geen corpus-case). (2)
struct_value_term is recursief: geneste structs als case-invoer en
-verwachting dragen door beide executors; sum-/list-veldwaarden
faulten nog, met nieuwe reden in S19; CONSTRUCTS rij 38 is bij de
landing serieel geactualiseerd. De lensronde ving een echte MAJOR
(tweemaal onafhankelijk): de gereduceerde const-divisor-vorm liet de
deler-zijdige range-hek vallen zodat clingo stil kon overlopen waar
ref exact rekent — hersteld met het hek op de gereduceerde factoren,
door beide lenzen nagerekend. Rood-eerst per flank met de letterlijke
oude viol- en fault-uitvoer.
