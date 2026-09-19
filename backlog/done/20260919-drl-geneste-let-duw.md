# drl-geneste-let-duw: de ch.-10.6-herschrijving voor de DRL-tekst-tak

Gemeten (asp-ch102-kaart, 2026-09-19): de DRL-tekst-tak plaatst een
TWEEDE geneste `Let` tussen behoefte en takknoop nog eager, met een
verkeerde wáárde — `verwacht 1.00, werkelijk leeg` — en een tot de arm
verkleinde barrière — `verwacht 6.00, werkelijk 5.00` (op dezelfde
case antwoorden ref, clingo en exec 6,00). Dit is de
ch.-10.6-herschrijving (til-, distributie- en wisselroutes plus de
gemeten capture-guard) die de ASP-tak over twee kaarten heeft
gekregen, nu voor de DRL-tekst-tak. `synth3-armwissel` weigert daar nu
luid bij naam (`TODO takgarde van een voorwaardelijk gelezen behoefte
(ch. 10.2)`); armverdeling/armvangst falen op waarde.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de huidige foute waarden en de luide weigering
  gepind per vorm.
- (b) De herschrijving in de DRL-tak, met dezelfde reikwijdte-pins als
  de ASP-kant (één-arm-lezing gemeten, niet syntactisch afgeleid);
  suite-getuigen + mutatieproeven.
- (c) Corpussweep beide takken; kanaaltellings-gevolg als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
`scripts/check-drools.sh` uitsluitend kanaaltelling (voorstel),
kaartverplaatsing. NB: serieel met drl-let-meldplek (zelfde bestand;
die kaart eerst — vier goldens spelen pas als beide er zijn).

## Aangrenzende lensvondst (2026-09-19, kaart drl-let-meldplek)

Een behoefte die zélf een binder is mist haar companion in de
DRL-tekst-tak: `Let k = inkomen in (Let m = k in (m gedeeld door
restpost))` is verifier-clean, ref/clingo/exec melden `inconsistent`,
de DRL-tak niet (pre-existent). Oorzaak gemeten: de behoeftelus roept
`companionDeel` aan vóórdat de binderomgeving is opgezet; de naïeve
herordening zou de zelf-resolutiebescherming breken, dus dit is een
besluit binnen deze kaart, geen mechanische fix.

Result: geland. De DRL-tekst-tak speelt nu ALLE VIJF `synth3-*`-goldens (130 ->
132; `synth3-armverdeling` van FAIL naar PLAYED, `synth3-armwissel` van EXCLUDE
naar PLAYED). De tak had geen knoopverplaatsende herschrijving nodig — zij lowert
de binding als control flow — maar wél een juist forceerhek: wat tussen een
diepere behoefte en de consequentie staat is de BARRIÈRE van de omsluitende
`Let`, niet de leegte van die behoefte. Wat control flow niet kan uitdrukken is
VOLGORDE, en daar doet deze tak dezelfde WISSEL als de ASP-kant. De
lensvondst is met de herordening-met-bescherming gedicht, en een derde,
pre-existente rand kwam er tijdens het werk uit: een takgarde wier binder
tussendoor herbonden wordt liet deze tak een `inconsistent` VERZINNEN; die vorm
wordt nu bij naam geweigerd. Exec beweegt niet (159, byte-identieke sweep), dus
`EXPECTED_PLAYED` blijft staan en geen beschermd script is aangeraakt.

## Rood-eerst: vier vormen, letterlijk vóór en ná

Elke vorm op een eigen minimale fixture (met de hand op de wire, buiten de repo
geschreven — deze kaart mag het corpus niet raken), plus de twee goldens zelf.
`uit` is het enige doel; `drempel` is een literal 10,00. Alle vijf modellen zijn
`verifier: PASS`, en elke gepinde waarde is EERST door `ref` geproduceerd en
daarna door clingo herhaald.

    D1  Let k = inkomen in (Let z = k gedeeld door restpost in
                            (Let w = 1,00 in (als k > 10,00 dan w anders z)))
    D2  ... in ((Let w = 1,00 in (als k > 10,00 dan w anders z)) plus 5,00)
    D3  ... (Let w = k in (als w > 10,00 dan 0,00 anders z))
    D4  Let k = inkomen in (Let m = k in (m gedeeld door restpost))
    D5  ... (Let w = 1,00 in (als k > 10,00 dan (w plus z) anders z))

                            ref/clingo/exec        --drl VÓÓR                --drl NÁ
    D1 inkomen 40,00        uit 1,00 consistent    FAIL verwacht 1.00,       PLAYED
       restpost leeg                                    werkelijk leeg
    D1 inkomen 5,00         uit leeg inconsistent  PLAYED                    PLAYED
    D1 inkomen 5,00/rp 2,00 uit 2,50 consistent    PLAYED                    PLAYED
    D2 inkomen 40,00        uit 6,00 consistent    FAIL verwacht 6.00,       PLAYED
       restpost leeg                                    werkelijk 5.00
    D2 inkomen 5,00         uit leeg inconsistent  PLAYED                    PLAYED
    D3 alle drie flanken    0,00 / leeg / 2,50     EXCLUDE MARKER: TODO      PLAYED
                                                   takgarde van een voor-
                                                   waardelijk gelezen
                                                   behoefte (ch. 10.2)
    D4 inkomen 10,00        uit leeg inconsistent  FAIL verwacht incon-      PLAYED
       restpost leeg                                    sistent, werkelijk
                                                        consistent
    D4 inkomen 10,00/rp2,00 uit 5,00 consistent    PLAYED                    PLAYED
    D5 beide flanken        leeg + inconsistent    PLAYED                    PLAYED

De goldens, DRL-tak vóór en ná:

    synth3-armverdeling  FAIL case1: heffing verwacht 1.00 werkelijk leeg;
                              wikkelheffing verwacht 6.00 werkelijk 5.00   -> PLAYED 2
    synth3-armwissel     EXCLUDE drl-transpiler MARKER: TODO takgarde …    -> PLAYED 2

De emitted DRL bevestigde de oorzaak vóór er een regel veranderde. D1's prelude
luidde `$behoefte3 = ($behoefte1 != null && $behoefte2 != null &&
rsAggVergelijk($behoefte1, 10.00, ">")) ? 1.00 : null;` — en `$behoefte2` (de
geneste `Let z`) is op de dan-arm null omdat zij dáár nooit geforceerd is, niet
omdat haar barrière knipte. `w` werd dus ook niet geforceerd en haar eigen
barrière, die de ANDERE armgarde draagt, leegde haar `Let`.

## De correctie: geen knoopverplaatsing, wel een juist hek — plus één wissel

De ASP-tak MOET de `Let` verplaatsen: elke lowering daar vertaalt de waarde van
een `Let` vóórdat zij de takken van de body kent, dus de armgarde bestaat nog
niet als de hekken en §9.5-deposities van die waarde worden uitgezonden. De
DRL-tekst-tak heeft dat probleem niet: zij lowert de binding als CONTROL FLOW —
één preluderegel per behoefte, geforceerd onder een hek, plus de leeg-barrière
op de `Let`-knoop zelf — en de armgarde staat gewoon in dat hek. Er is dus geen
til-, distributie- of capture-route nodig; dat is de architectuurreden dat deze
kaart 15 regels vertaler kost waar de ASP-kant twee kaarten nodig had.

Drie correcties, elk apart gemeten:

1. **Het forceerhek van een diepere behoefte is de BARRIÈRE** (`letWaarde`).
   `acc.letBewaking` droeg `var != null`; dat is de juiste bewering alleen als de
   `Let` geen armgarde heeft. Mét armgarde `G` knipt de barrière pas bij
   `G && var == null`, dus het hek is `!(G && var == null)`. De ongegarde spelling
   blijft letterlijk `var != null` — dezelfde bewering, en zo houdt elk document
   zonder voorwaardelijk gelezen behoefte zijn DRL byte voor byte.
2. **De WISSEL** (`letWissel`, aangeroepen vanuit `letWaarde` én `letDescent`).
   Twee aangrenzende `Let`s commuteren: ch. 10.6 evalueert een behoefte bij haar
   eerste LEZING, nooit bij haar binding, dus de nestvolgorde van een paar ordent
   hun evaluaties niet, en de buitenste van het paar staat hoe dan ook op dezelfde
   positie. De wissel schuift de `Let` één stap naar de takknoop toe en herhaalt
   zich door de hele keten. Drie scope-condities, elk een correctheidseis: de twee
   binders verschillen; de geneste waarde leest de buitenste binder niet (zij zou
   diens scope verlaten); de buitenste waarde leest de NAAM van de geneste binder
   niet (zij zou diens scope binnengaan). Die derde is gemeten dragend — zie M5.
3. **De companion bouwt hekken en omgeving in ÉÉN wandeling** (`plekGuards`).
   Dat is de lensvondst, en hieronder staat waarom de bescherming intact blijft.

## De lensvondst: herordening MÉT bescherming, niet weigeren

De keuze was: de omgeving eerst opzetten (en de zelf-resolutiebescherming
riskeren) of de vorm benoemd weigeren. Het werd de eerste, omdat de bescherming
niet in de VOLGORDE VAN DE TWEE LUSSEN zit maar in de volgorde BINNEN de
wandeling — en die blijft staan als je de twee lussen tot één maakt.

`plekGuards` las dezelfde ketting twee keer: eerst een lus over de behoeften (het
`vol`-hek van elk), dan een lus over de binders (de omgeving). Een behoefte die
zélf een binder is werd dus gedecomponeerd tegen een nog lege omgeving en gaf
`null`, wat de companion laat vallen. De naïeve fix — de omgeving eerst volledig
opbouwen — zou een behoefte tegen haar EIGEN binding kunnen oplossen.

De gelande vorm is één lus over een geordende lijst `Behoefte(sleutel, waarde)`:
elke behoefte wordt gedecomponeerd tegen de bindingen die er STRIKT BUITEN staan
en pas daarna zelf in scope gezet. Dat is precies dezelfde bescherming als
voorheen (een behoefte lost nooit door zichzelf op, dus de omgeving kan geen
cykel sluiten), maar nu ook toegepast op de behoeften — die hem daarvoor niet
kregen omdat ze een aparte lus hadden. `Poort.binders` (een op NAAM
samengevouwen map) verdwijnt daarmee; de geordende lijst drukt schaduwing beter
uit, want een schaduwende binnenbehoefte leest de OMSLUITENDE binding van die
naam, wat WF-076 zegt.

Dat laatste is een redenering en geen getuige, en dat is gemeten: de vorm die hem
zou testen — `Let a = inkomen in (Let a = a plus 1,00 in (a gedeeld door
restpost))` — is niet verifier-clean (`WF-076 FAIL: the let binder 'a' is
referenced in its own value`). Zij kan dus niet van de wire komen, en de
verbetering is daar strikt theoretisch.

Structureel bewijs dat de twee helften bij elkaar horen: de WISSEL maakt de
behoefte van de verplaatste `Let` zélf een binder (`Let w = k`), dus zonder de
lensfix speelt `synth3-armwissel` niet. M3 hieronder laat precies dat zien.

## De derde rand, tijdens het werk gemeten: een verzonnen melding

Een ch.-10.2-takgarde wordt gesteld bij de `Let` die zij hekt, terwijl de
takknoop lager staat. Herbindt er iets tussenin een naam die de conditie leest,
dan betekent die naam op de twee plekken niet hetzelfde — en deze tak loste hem
op tegen de binding die toevallig hier in scope stond. Gemeten op een eigen
verifier-clean fixture, en PRE-EXISTENT (identiek op de jar van vóór deze kaart):

    uit := Let w = saldo in
             ((Let z = (w gedeeld door restpost) in
                 (Let w = inkomen in (als w > 10,00 dan 0,00 anders z))) plus w)

    saldo 5,00 / inkomen 40,00 / restpost leeg
      ref    uit 5,00 CONSISTENT        exec   PLAYED
      clingo TRANSLATION FAULT (luid)   --drl  FAIL: verwacht consistent,
                                               werkelijk inconsistent

Een VERZONNEN fout is het slechtste antwoord dat er is, dus deze vorm wordt nu
bij naam geweigerd (`gardeLeestBinnenBinder`). De toets over-approximeert
bewust — een `Let` in een zusteroperand of binnen een arm laat hem ook afgaan,
hoewel geen van beide de conditie kan schaduwen — omdat de exacte toets het pad
naar de takknoop zou moeten bijhouden voor een vorm die geen enkele golden heeft.
De over-approximatie kost weigeringen, nooit verkeerde antwoorden, en de sweep
meet dat zij er vandaag nul kost. Eén zustervorm die vóór deze kaart TOEVALLIG
goed antwoordde (de twee bindingen vielen daar samen) wordt nu ook geweigerd; het
toeval was geen reden om hem te houden.

## Reikwijdte, in beide richtingen gemeten

- **Een behoefte die BEIDE armen lezen blijft eager, en dat is exact** (D5). De
  guard is een METING van waar de binder gelezen wordt — `leesGarde` daalt ook
  door de waarde van een geneste `Let` af (`enOok`: de behoefte moet geforceerd
  worden ÉN haar waarde moet lezen), wat precies de capture-verfijning is die de
  ASP-kant apart moest bouwen. Hier mat zij "altijd", en dan is er geen arm om de
  behoefte in te beperken.
- **Twee vormen worden LUID geweigerd**, beide op een eigen fixture gemeten, en
  beide dezelfde grens als aan de ASP-kant:
  - een rekenwikkel TUSSEN de twee `Let`s met een conditielezende tussenbinder —
    de wissel kan niet over de wikkel stappen en er staat op `z`'s eigen positie
    geen takknoop om in te distribueren. `ref` en exec spelen hem (5,00);
    clingo weigert hem met zijn eigen luide TRANSLATION FAULT, deze tak met de
    ch.-10.2-marker. Exact dezelfde vorm, exact dezelfde grens, twee takken.
  - de herbonden takgarde hierboven.
- **Ongewijzigd geweigerd** blijft `Onbepaald` (een lezing onder twee
  verschillende takgardes) en de `letVoorspel == null`-plekken.

## De getuige

`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/GenesteLetDuwWitnessTest.java`,
zeven rijen. Vijf gaan door `BeideTakken` (exec-model én DRL-tekst, dezelfde
verwachting van allebei geëist); twee zijn uitgeschreven omdat `BeideTakken`
met opzet geen vorm heeft voor "de ene tak speelt, de andere weigert" — en dat
staat er als comment bij. Elke waarde is eerst door `ref` geproduceerd en door
clingo herhaald.

    de_geneste_let_tussen_behoefte_en_takknoop_volgt_de_gelezen_arm
      D1, drie flanken: 1,00 consistent / leeg inconsistent / 2,50 consistent
    de_barriere_van_de_geneste_behoefte_leegt_het_geheel_en_niet_de_arm
      D2, de REIKWIJDTE-PIN: 6,00 op de niet-lezende arm, leeg (NIET 5,00) op de
      lezende — beide flanken vallen om op 5,00 als de barrière tot de arm krimpt
    de_takgarde_leest_haar_binder_doordat_de_twee_lets_wisselen
      D3, drie flanken: 0,00 / leeg+melding / 2,50
    een_behoefte_die_zelf_een_binder_is_meldt_op_haar_eigen_plek
      D4, met de fabricage-flank ernaast (gevulde deler: 5,00 + consistent)
    een_behoefte_die_beide_armen_lezen_blijft_eager_en_dat_is_exact
      D5, drie flanken, waaronder 21,00 waar beide lezingen rekenen
    een_wikkel_tussen_de_twee_lets_wordt_luid_geweigerd_in_de_drl_tak
      de GRENS: exec speelt 5,00, de DRL-tak weigert bij naam
    een_takgarde_wier_binder_tussendoor_herbonden_wordt_wordt_luid_geweigerd
      de derde rand, met het getal dat de twee `w`'s scheidt: ref 7,50, de
      verwarde lezing 9,00

Suite: 471 -> 478 tests (+7), 0 failures, 0 skipped.

## Mutatieproef, feitelijk uitgevoerd en hersteld

Vijf mutaties, elk apart gebouwd, gedraaid en teruggedraaid; werkboom daarna
schoon en de suite terug op 478/0.

    M1  het forceerhek terug op "var != null"                        2 FAILURES
        drl-tekst — de dan-arm leest de geneste behoefte niet ==>
          expected: <null> but was: <case: attribuut uit:
          verwacht 1.00, werkelijk leeg>
        drl-tekst — de niet-lezende arm telt gewoon op ==>
          expected: <null> but was: <case: attribuut uit:
          verwacht 6.00, werkelijk 5.00>
    M2  de wissel uit (letWissel geeft altijd b terug)               1 ERROR
        de_takgarde_leest_haar_binder… » Excluded drl-transpiler MARKER:
          TODO takgarde van een voorwaardelijk gelezen behoefte (ch. 10.2)
    M3  de companion weer in twee lussen                             2 FAILURES
        drl-tekst — de anders-arm leest haar en meldt ==> expected: <null>
          but was: <case: verwacht inconsistent, werkelijk consistent>
        drl-tekst — de behoefte-als-binder meldt ==> expected: <null>
          but was: <case: verwacht inconsistent, werkelijk consistent>
    M4  de weigering van de herbonden takgarde weg                   1 FAILURE
        Expected CaseRunner.Excluded to be thrown, but nothing was thrown
        (en de fixture antwoordt dan weer "werkelijk inconsistent")
    M5  de derde scope-conditie van de wissel weg                    1 FAILURE
        idem, en apart gemeten op de fixture: FAIL attribuut uit:
        verwacht 7.50, werkelijk 9.00 — de wissel leest dan de BINNENSTE w

M1 valt precies op de twee waardenrijen en met de OUDE getallen. M3 valt óók op
de wisselrij, wat de structurele koppeling tussen de wissel en de lensfix meet.
M5 legt een verkeerde WAARDE bloot en niet alleen een ontbrekende weigering, dus
de scope-conditie is dragend en geen decoratie.

## Corpussweep over ALLE goldens in BEIDE takken, vóór en ná

Eén regel per golden (209 goldens), met zelfgebouwde shaded jars van vóór en van
ná. Exec: PLAYED 159 -> 159, het hele rapport BYTE-IDENTIEK — geen enkele regel
beweegt. DRL-tekst: PLAYED 130 -> 132, met precies TWEE gewijzigde regels:

    synth3-armverdeling  FAIL (case1 waarden)   -> PLAYED 2
    synth3-armwissel     EXCLUDE (ch.-10.2-marker) -> PLAYED 2

Geen andere golden verandert van kanaal, telling of reden, in geen van beide
takken. De narrowing van de derde rand kost dus niets: de sweep ná de weigering
is byte-identiek aan de sweep ervóór.

Een tweede, strengere meting: de EMITTED DRL van alle goldens, gedumpt met beide
jars en byte-vergeleken. Vier bestanden verschillen, twee meer dan er regels
bewegen:

    synth3-armverdeling   de vier gewijzigde preluderegels + de wissel
    synth3-armwissel      idem
    synth3-armvangst      één preluderegel, waardebehoudend
    synth3-drempelheffing één preluderegel, waardebehoudend

De laatste twee verdienen hun eigen zin. Daar is de armgarde van de geneste `Let`
DEZELFDE als die van de omsluitende, dus `!(G && v == null)` en `v != null`
vallen onder `G` samen; de tekst wordt langer, de betekenis niet, en de replay
bevestigt dat (beide blijven PLAYED met identieke status). Alle 205 andere
goldens zijn byte-identiek.

## Alle veertien gates, los gedraaid, exitcode per gate

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Tellingen: gate 2 237 documenten (vloer 192) + 24 patroon-zelftestrijen, gate 3
237 golden clean + 395 negatief (vloer 350), gate 4 567 casebestanden, gate 5 643
worked examples (vloer 489) + 539 cases (vloer 427), gate 6 539 cases + 418
suite-checks (vloer 83), gate 7 503 cases weerleggen een niets-doener + 36
survivors, gate 8 28 rode cases (vloer 26), gate 9 201 checks (vloer 201) + 12
goldens (vloer 12), gate 10 298 tests (vloer 173) + 58 goldens (vloer 38), gate
11 45 rgs byte-identiek (vloer 45) + 27 TOKA-tests + 29/29/29, gate 12 played 159
/ kanaalvloer 50 / JUnit 478 (vloer 239), gate 13 126 twists over 23 goldens
(vloer 74) / 104 replays / 22 DRL-tekst-PLAYED (vloer 13), gate 14 130 checks
(vloer 81) + 2 goldens (vloer 1).

Gate 12's exec-telling beweegt niet: die gate draait alleen de exec-tak en die
tak was al compleet. Gate 13's DRL-PLAYED-telling beweegt ook niet (22 vóór én
ná): geen van de vijf `synth3-*`-goldens draagt een eenheidsdragende annotatie,
dus geen van hen zit in de twist-set.

## Registervoorstellen (niet geland — hoofdsessie)

**`spec/CONSTRUCTS.md` rij 24.** VERVANG de zin

    since 2026-09-19 (drl-let-meldplek card) the DRL-text branch plays THREE of
    them — `synth3-takhek`, `synth3-operatortak` and `synth3-armvangst` —
    because its §6.5/§6.6 companion now follows the BINDING: a fault site whose
    operand is a binder stands for its need's own re-readable read, so a §9.5
    place inside a `Let` body deposits exactly as the un-bound spelling does.
    The two it does not play are not that gap: `synth3-armverdeling` still
    answers a wrong VALUE on case 1 (a second nested `Let` between the need and
    the branch node is placed eagerly there) and `synth3-armwissel` is refused
    loudly by name; both reasons are recorded at gate 12's channel count.

DOOR

    since 2026-09-19 the DRL-text branch plays ALL FIVE, over two cards. Its
    §6.5/§6.6 companion follows the BINDING (drl-let-meldplek): a fault site
    whose operand is a binder stands for its need's own re-readable read, so a
    §9.5 place inside a `Let` body deposits exactly as the un-bound spelling
    does — and since drl-geneste-let-duw a need that is ITSELF a binder
    resolves through that same environment, which the companion now builds in
    ONE walk with its need guards instead of two, each need decomposed against
    the bindings strictly outside it. That branch needs no node-MOVING rewrite
    of its own, because it lowers the binding as control flow rather than
    translating a value before it knows the body's branches: what the ch. 10.6
    arm discipline asks of it is a correct forcing GATE — what stands between a
    deeper need and the consequence is the enclosing `Let`'s BARRIER, not that
    need's emptiness, and under a branch guard the two differ on exactly the
    arm that does not read it. The one thing control flow cannot express is
    ORDER, and there it performs the same EXCHANGE: two adjacent `Let`s
    commute, since ch. 10.6 evaluates a need at its first READ and never at its
    binding, so a branch guard reading a binder bound deeper stops being a
    forward reference. Its scope limits are measured rather than assumed: a
    need BOTH arms read stays eager, which is what ch. 10.6 asks for there, and
    two shapes are refused LOUDLY — an arithmetic wrapper standing between the
    two `Let`s, the same shape neither ASP route reaches, and a branch guard
    whose own binder is REBOUND between the `Let` and the branch node, where
    resolving that name against whatever stood in scope higher up had made this
    branch FABRICATE an Inconsistentie on a document ref and the exec-model
    branch call consistent — clingo refuses it with its own loud TRANSLATION
    FAULT (measured 2026-09-19, pre-existing).

**`scripts/check-drools.sh`, het redenblok bij `EXPECTED_PLAYED`** (de alinea die
met "The DRL-TEXT branch plays THREE of the five" begint). Zij beschrijft (b) en
(c) als actueel en dat is door deze kaart achterhaald; `EXPECTED_PLAYED` zelf
blijft 159. Voorstel, als vervanging van die alinea:

    # The DRL-TEXT branch plays ALL FIVE since 2026-09-19 (cards
    # drl-let-meldplek and drl-geneste-let-duw). That is recorded here rather
    # than left to be rediscovered, because this gate does not measure that
    # branch and nothing else would say it. All three reasons measured when
    # these goldens landed are now CLOSED. (a) The branch deposited no §9.5
    # channel reason for a §6.5 place inside a `Let` BODY — the same document
    # without the `Let` played, so the gap was the binding and not the branch —
    # and its companion follows the binding now (DRL-text 127 -> 130). (b) A
    # second nested `Let` between the need and the branch node was placed
    # eagerly, so armverdeling's case 1 answered leeg where ref, clingo and the
    # exec branch answer 1,00, and its wrapper rule a coalesced 5,00 where all
    # three answer 6,00. The cause was the forcing GATE and not the placement:
    # what stands between a deeper need and the consequence is the enclosing
    # Let's BARRIER, not that need's emptiness, and under a branch guard those
    # differ on the arm that does not read it. (c) armwissel was refused LOUDLY
    # by name (`TODO takgarde van een voorwaardelijk gelezen behoefte
    # (ch. 10.2)`); two adjacent Lets commute, so the exchange gives that guard
    # its binder. DRL-text 130 -> 132, measured over all goldens in both
    # branches; this gate's exec count is unmoved at 159 and its whole exec
    # report byte-identical, so EXPECTED_PLAYED does not move. What that branch
    # still refuses by name, each measured on its own minimal input and each
    # the same boundary the ASP side has: an arithmetic wrapper standing
    # between the two Lets, and a branch guard whose own binder is rebound
    # between the Let and the branch node (that one used to answer, and what it
    # answered was a fabricated Inconsistentie).

**Voor de kaart `gate12-drl-takteller`** (maintainersbesluit 2026-09-19, de volle
vorm): de DRL-PLAYED-stand die daar gepind moet worden is **132**, niet de 127
die in die kaarttekst staat — deze kaart heeft hem met twee opgehoogd.

## Wat NIET af is, en wat gemeten tegenviel

**De behoefte-hekken van de companion zijn onder een armgarde te sterk.**
`letDescent` geeft de body `achterBehoefte`, dus elke §6.5-plek in de body zit
achter `behoefte != null` — ook als die behoefte alleen op de ANDERE arm gelezen
wordt en haar barrière daar dus nooit knipt. Dat is een GEMISTE melding, nooit
een verzonnen, en geen enkele golden of fixture van deze kaart raakt hem; ik heb
hem niet aangeraakt omdat hij buiten de gemeten vormen valt.

**De wissel bereikt geen rekenwikkel.** Zie de reikwijdtesectie: dezelfde grens
als aan de ASP-kant, daar en hier bij naam geweigerd. Geen van beide takken
draagt hem, en er is geen corpusgolden die hem heeft.

**De extra hekterm is soms logisch overbodig.** Waar de armgarde van de geneste
`Let` samenvalt met die van de omsluitende (armvangst, drempelheffing) staat er
nu `!(G && v == null) && G` waar `v != null && G` hetzelfde zegt. Ontdubbelen
vraagt een termvereenvoudiger; de DRL is er langer door en verder niets.

## Open twijfel

De over-approximatie van `gardeLeestBinnenBinder` is de enige plek waar deze
kaart bewust meer weigert dan zij moet: een `Let` die ergens in de body staat
maar de conditie onmogelijk kan schaduwen — in een zusteroperand, of binnen een
arm — laat de toets afgaan. Vandaag kost dat nul goldens (de sweep meet het),
maar het is een meting over het huidige corpus en geen eigenschap van de toets.
De exacte vorm zou het pad van de `Let` naar de takknoop moeten bijhouden, wat
`Leesgarde` vandaag niet doet; ik vond dat te veel machinerie voor een vorm die
nergens voorkomt, en noteer liever dat de keuze een afweging was.

Tweede twijfel: de wissel is gemeten voor één en voor twee lagen en volgt
structureel voor meer, maar er is geen getuige voor drie. Dat is exact dezelfde
open twijfel als aan de ASP-kant, en om dezelfde reden: elke laag neemt hetzelfde
recursieve pad. Wie dat te dun vindt heeft gelijk dat het een redenering is.

Derde twijfel: de eenlus-companion maakt een geschaduwde binnenbehoefte
oplosbaar tegen de OMSLUITENDE binding van die naam, wat WF-076 zegt — maar de
vorm die dat zou testen is door WF-076 zelf verboden en kan niet van de wire
komen. Die verbetering is dus onfalsifieerbaar, en ik heb haar niet weggehaald
omdat het alternatief (de map weer samenvouwen) meer code is om minder te doen.

## Lensronde (2026-09-19, naschrift hoofdsessie)

Alle tien lenspunten CONFIRMED — de lens draaide alle vijf mutaties
(niet drie), bevestigde dat de over-approximatie in elke eigen probe
foute antwoorden in weigeringen omzet, en bouwde zelf de ontbrekende
drie-lagen-getuige, die houdt (EXCLUDE→PLAYED met 1,00 en 3,50, ref
en clingo eens). Advies FIX-dan-MERGE: twee éénregel-correcties in de
registervoorstellen, door de hoofdsessie bij het landen doorgevoerd —
clingo weigert het fabricagedocument luid (het stond ten onrechte bij
de consistent-zeggers) en het stale takteller-getal was 127, niet
130. Twee observaties, vastgelegd zonder kaart: (1) een derde
geweigerde vorm — de wissel herbekijkt zijn eigen teruggegeven
buitenknoop niet, dus een arm-gebonden tussenlaag tript de weigering;
de gemeten éénregel-fixpoint-variant degradeert één flank tot een
gemiste melding, dus de huidige luide weigering is het betere
antwoord; (2) de openingszin "geen knoopverplaatsende herschrijving"
is aangezet — de wissel ís er één, zoals zijn eigen javadoc zegt, en
de kwalificatie volgt pas in de zin erna.
