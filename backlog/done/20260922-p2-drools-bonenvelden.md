# p2-drools-bonenvelden: feitcreatie en uniciteitsscope uit de rolbindingen

V1-preconditie P2 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B7/B8). Bij feitcreatie (`zetVeld`/`joinVeld`/
`Tussenstap.*`) en de uniciteitsscope (`scopeVeld`) leest
`backends/drools` de symbolische rolvelden (`feittype`/`ankerRol`/
`doelRol`/`joinRol`, `scopeRol`, tussenstap-`rolBinding`) wel, maar
het BONENveld beslist de uitvoering. B7/B8 halen de bonenvelden van de
wire; zonder deze port is dat een amputatie van uitgevoerde
semantiek. De port: leid het bonenveld af uit de rolbinding
(feittype + rol → veldnaam), zodat de symbolische velden de
beslissende bron worden.

## Acceptatie

- (a) Beide Drools-takken beslissen op de uit de rolbindingen
  afgeleide velden; de wire-bonenvelden verliezen hun beslissende
  rol (kruiscontrole die hardop faalt bij verschil mag — kies en
  motiveer).
- (b) De leesproef rood-eerst per positie (zetVeld, joinVeld,
  tussenstap, scopeVeld): een IR-mutatieproef die het BONENveld
  twist en aantoont dat het gedrag ná de port de rolbinding volgt
  waar het vóór de port de boon volgde. Uitgevoerd en teruggedraaid
  met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P1, P3, unie-telling); grootste van de drie
ports — vier posities.

Result: (a) gehaald. `FactModelGenerator.creatievelden` en
`FactModelGenerator.scopeVeld` zijn de ene afleiding waarop beide takken
beslissen (exec `feitCreatie`/`uniciteit`, DRL-tekst idem). De regel is
die van `leden()`: een beanveld heet naar de rol die de andere kant
speelt. `zetVeld` = `ankerRol` (in `feittype`), `joinVeld` = de zusterrol
van `joinRol`, een hop: gebonden rol enkelvoudig → veld op de ouder met
de naam van die rol, anders → veld op het kind met de naam van de
zusterrol, type = de vuller van de gebonden rol; `scopeVeld` = de rol die
`scopeRol` bindt. Bij 1:1 wordt de ouderzijde genomen: case-invoer schrijft
beide velden (ch. 5.7a) en het is de zijde die de frontend kiest bij een
door objectcreatie gelegde koppeling (de enige 1:1-hop in de corpora, in
toka, speelt zo ongewijzigd). Zonder symbolische vorm (WF-028 verwerpt
dat, ref en clingo falen erop) weigert de creatie als `feitcreatie
(onopgelost)`; een scopebinding die niet oplost weigert als `uniciteit
(scopebinding onopgelost)`. Alleen de bindingloze scope (WF-054 laat hem
toe, ref leest dan ook de geschreven naam) leest nog `scopeVeld`.

Gekozen: GEEN kruiscontrole. Gemeten, per positie. ref beslist alle vier
symbolisch: `run_feit_creatie` leest alleen `feittype`/`ankerRol`/
`doelRol`/`joinRol`/`rolBinding` en faalt op een creatie met alleen
bonenvelden; `run_uniciteit` neemt `scopeRol` en valt alleen zonder
binding terug op `scopeVeld`. De verifier kruist maar gedeeltelijk: WF-054
eist dat `scopeVeld` de gebonden rol aanduidt (hoofdletterongevoelig, ook
het meervoud); WF-034 (m) houdt `zetVeld`/`joinVeld`/hopveld tegen de
enkelvoudige rol van het feittype, niet tegen `ankerRol` of de zuster van
`joinRol`, en zwijgt bij gelijke kardinaliteit; het hoptype wordt eenzijdig
gekruist; `viaOuderVeld` kruist geen enkele regel. Het P1-argument
("de verifier dekt het") gaat hier dus maar half op. Het draagt toch,
om de andere helft: na de port leest Drools wat ref leest, dus elk
verschil tussen boon en binding komt uit als ref ≡ Drools, en een
kruiscontrole zou een veld dat bij B7/B8 verdwijnt een veto geven over
een document dat ref speelt. Bewijs dat de gaten echt zijn: de twist
`viaOuderVeld: true` op de hop van `jutterswet-postwacht` is
verifier-schoon, ref speelt hem, en vóór de port weigerden beide takken
hem.

Meting vooraf. Leesplekken vóór de port (accessor-aanroepen buiten
commentaar), DRL-tekst / exec: `zetVeld` 4/2, `joinVeld` 3/2,
tussenstap `veld` 4/1, `viaOuderVeld` 3/1, `type` 5/4, `scopeVeld` 2/3;
de symbolische velden werden geladen maar in geen van beide takken
gelezen. Dragers: golden 10 creaties in 7 documenten (8 hops in 6), 5
gescopete uniciteiten in 4 documenten (1 bindingloos:
`anchor/jutterswet-kustregister`, tweede); rood 2 creaties (2 hops) en 2
scopes; negatief 19 creaties en 12 scopes; toka 1 creatie met 1 hop.
Afleiding tegen wire over alle vier zones: golden wijkt af in
`anchor/jutterswet-erflijn` (zetVeld, joinVeld, hop) en
`anchor/jutterswet-gemeenteberging` (joinVeld, hopzijde), rood in
`to-one-second-filler` (dezelfde vorm als gemeenteberging), toka nergens,
negatief in de bonenveld-getuigen zelf (WF-034, WF-028, WF-054, WF-109).
De twee goldens weigeren in beide takken al eerder (de aggregatiejoin),
dus spelen ze niet mee. Opvallend in erflijn: de wire-`joinVeld`
`opvolger` wijst over de zelfgetypeerde 1:1 `opvolging` de verkeerde
richting op (doel.opvolger == anker); de afleiding geeft `voorganger`,
zoals ref loopt. WF-034 arm B staat daar af (gelijke kardinaliteit).

(b) gehaald, rood eerst. `BonenveldWitnessTest`, 13 rijen via de
replay-API in beide takken. Per positie een bonentwist die niets meer
mag veranderen en een bindingtwist die het antwoord moet verplaatsen:
zetVeld (`jutterswet-dubbelverbinding`, winterberging → keurhaven;
binding naar keurberging), joinVeld (→ vlaghaven; binding naar
vlagvoering.vlaggenschip), tussenstap (postwacht met een tweede
Kustvak-Post-relatie `reserveligging`: veld → reservevak; binding naar
reserveligging; plus zijde en type los), scopeVeld (kustregister:
→ sectiecode; binding naar een tweede relatie `wijkindeling`). Daarbij:
het postwacht-document zonder één bonenveld (de wire na B7/B8) speelt, de
creatie zonder symbolische vorm en de onoplosbare scopebinding worden
geweigerd. Matrix per rij, jar van main 58ae7a4c tegen jar met port, elk
in beide takken, plus ref: vóór de port volgde elke bonentwist het
getwiste veld (waardefout, of voor zijde en type een weigering op een
veld dat de boon niet draagt) en speelde elke bindingtwist het
ongetwiste antwoord; na de port geeft elke rij in beide takken exact het
antwoord van ref (bonentwist: ref PASS, bindingtwist: dezelfde
MISMATCH-cellen). Uitgevoerd en teruggedraaid in één keten (stash van de
drie hoofdbestanden, test, pop, test): zonder port test-rc=1, 13/13 rood
(10 failures, 3 errors); met port test-rc=0, 13/13 groen.

(c) gehaald. Sweep over alle 232 goldens en 41 rood-documenten, beide
takken, volledige stdout per document: vóór en na byte-identiek (`diff`
leeg). Exec 179, DRL-tekst 156; gate 12 meldt 179/156, vloeren 53/76,
dus de pins bewegen niet.

(d) NIET gehaald zoals gecommit. Twee bestaande getuigen pinnen de oude
lezing, en daardoor faalt de JUnit-stap in gates 8, 11 en 12:
`HerhaaldTypepaarWitnessTest.de_aangemaakte_koppeling_wordt_aan_beide_kanten_gelezen`
(de mutatieflank twist `zetVeld` en verwacht dat het antwoord meegaat,
precies de omgekeerde van de nieuwe rij) en
`RolveldlezerWitnessTest.de_feitcreatie_weigert_een_tussenstap_zonder_veld`
(pint de weigerreden van de hop op de wire-ouderzijde van
gemeenteberging; na de port ligt de hop aan de kindzijde, waar Jutter het
veld draagt, en weigert de creatie één positie verder, op de join over de
N:N-relatie `aanmelding`: "het beanmodel leidt voor de rol 'aanmelder' op
Vondst geen referentieveld naar Jutter af"). Een conversie van beide
(flank twist de binding naar keurberging; de rij pint de nieuwe
weigerreden) is gemeten maar NIET gecommit, omdat bestaande tests
wijzigen toestemming vraagt. Met die conversie zijn alle veertien gates
groen: 1 PASS, 2 PASS (273 bestanden), 3 PASS (273 golden schoon, 431
negatief verworpen), 4 PASS (629 cases), 5 PASS (775 voorbeelden, 588
cases), 6 PASS (479 checks, 588 cases), 7 PASS (552 cases, 36
overlevers), 8 PASS (41 rood; Drools-been 9 met pin, 32 met reden), 9
PASS (201 checks, 12 goldens), 10 PASS (374 tests, 70 goldens), 11 PASS
(29 toka-cases in ref, clingo en Drools), 12 PASS (179/156, JUnit 562),
13 PASS (131 twists, 128 replays), 14 PASS (130 checks). Wire, schema,
corpus, frontends, toka en scripts zijn niet gewijzigd.

Voorgestelde registerbijwerking (spec/CONSTRUCTS.md, hoofdsessie). Rij
4, "Measured authority relation": "the Drools loader's v0 bean fields are
AUTHORITATIVE" krijgt "— except, since 2026-09-22 (P2), for fact creation
and the uniqueness scope"; "holds for ref and clingo on fact creation and
nowhere else" wordt "holds for ref, clingo and, since 2026-09-22 (P2),
both Drools branches on fact creation". Daarachter: "**Landed
(2026-09-22, kaart `p2-drools-bonenvelden`):** both Drools branches
execute a fact creation and a scoped uniqueness on bean fields derived
from the role bindings (`FactModelGenerator.creatievelden` /
`scopeVeld`): the written field is `ankerRol`, the join field `joinRol`'s
sibling, a hop's field, side and type follow from its binding and the
roles' cardinality, the scope field is `scopeRol`'s role; no cross-check
against the wire; a bean-only creation is refused, as in ref. Corpus
sweep byte-identical in both branches over golden and rood (179/156);
witness `BonenveldWitnessTest`. Residue for B8: a bindingless scope still
reads the written `scopeVeld`." Rij 20 en rij 22, en de knooprijen
`ResolvedFeitCreatie` en `ResolvedUniciteit`, krijgen elk: "executed by
both Drools branches off the bindings since 2026-09-22 (P2)". PLAN.md:
geen scopewijziging.
