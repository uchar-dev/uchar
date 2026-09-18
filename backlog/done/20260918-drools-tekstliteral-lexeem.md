# drools-tekstliteral-lexeem: beide takken vergelijken v0-tekst verbatim

Goal: Beide Drools-takken geven een v0 `Literal` van type `Tekst`
dezelfde lexicale waarde: `waarde` wordt verbatim vergeleken en alleen
bij DRL-emissie ge-escaped. Classic DRL mag geen aanhalingstekens uit
de semantische waarde verwijderen; de exec-model-tak gebruikt niet
langer een afwijkende private tekstlezer. Dit herstelt zowel
beslistabelcellen uit de oorspronkelijke toolchain als canonieke
uchar-tekst met ingebedde quotes.

Evidence:

- IR-SPEC ch. 4.1 zegt dat v0 de onontlede bronstring in `value`
  draagt. Ref maakt daarvan verbatim `Value.Text`; case-invoer maakt
  van `{"text": ...}` eveneens verbatim tekst.
- Classic DRL roept `Literalen.tekstOfEnumLabel` aan, dat nu alle
  `"`-tekens met `replace` verwijdert. Exec-model heeft juist een
  private verbatimlezing. Het commentaar dat er één gedeelde
  interpretatie is, is daardoor aantoonbaar onwaar.
- `jutterswet-keurmerk` bewijst de ingebedde-quoteflank:
  exec speelt, classic DRL vergelijkt `merk "AA"` als `merk AA` en
  mist ook de geschreven waarde `veiling "Zee"`.
- De drie geïsoleerde `scenario-toka-46-woonregio-factor-*`-replays
  bewijzen de oude bronlexeemflank: zowel literal als casewaarde dragen
  `"Friesland"`-achtige buitenste quote-tekens; exec speelt en classic
  DRL faalt. De oorspronkelijke
  `ExecModelDifferentialTest.BESLISTABEL`-fixture speelde in beide
  oorspronkelijke armen. Een tegenprobe met beide waarden rauw keerde
  de huidige takuitkomst om; de branches interpreteren dus dezelfde
  wire aantoonbaar verschillend.

Acceptance:

- Rood-eerst: leg de classic-DRL-fout van
  `corpus/golden/jutterswet-keurmerk` en van alle drie
  `toka/cases/scenario-toka-46-woonregio-factor-*`-gevallen vast.
- `Literalen.tekstOfEnumLabel` (of een duidelijker hernoemde gedeelde
  helper) retourneert voor `Tekst` exact `lit.waarde()`. Alleen
  enumeratiewaarden verwijderen hun gepaarde enkel-quote-delimiters.
  `DrlTranspiler` gebruikt `drlString` uitsluitend om die waarde veilig
  in DRL te renderen; escaping verandert de waarde niet.
- Verwijder de private semantische tekstinterpretatie uit
  `ExecModelTranspiler`; alle tekstliteralvergelijkingen en
  tekstliteralwrites in beide branches gebruiken dezelfde helper.
- Unit- en dual-branchgetuigen pinnen afzonderlijk:
  1. rauw `Flevoland` == rauw `Flevoland`;
  2. quote-dragend `"Flevoland"` == exact dezelfde quote-dragende
     casewaarde;
  3. `merk "AA"` behoudt de ingebedde quotes;
  4. rauw en quote-dragend zijn onderling ongelijk (geen impliciete
     case-normalisatie);
  5. GELIJK en ONGELIJK, een `Samengesteld`/beslistabel-`of`-rij en een
     tekstwaarde in de consequentie.
- `jutterswet-keurmerk` en de drie geïsoleerde TOKA-replays eindigen
  zonder vlag én met `--drl` in `PLAYED`, zonder wijziging van IR,
  cases of verwachtingen. De backend-JUnit-getuige voor de
  buitenste-quotevorm is eigen geschreven en bevat geen TOKA-herkomst
  of private verwijzing.
- De v0-lexeemkeuze wordt niet stil omgezet in de toekomstige v1
  typed-value-delta. Noteer een eventueel gewenst canoniek
  migratiebesluit als vervolgkaart; wijzig deze kaart niet onderweg in
  een wiremigratie.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-regelspraak.sh`,
  `bash scripts/check-toka-parity.sh`,
  `bash scripts/check-drools.sh` en alle veertien gates zijn groen.

May touch: `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/kern/Literalen.java`,
`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/kern/LiteralenTest.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Bestaande corpus- en TOKA-bestanden zijn
alleen leesbare acceptatiegetuigen.

Out of scope: case-inputnormalisatie, het herschrijven van oude IR,
v1 typed literals, tekstinterpolatie (`TekstReeks`), verruiming van de
RegelSpraak-stringgrammatica, enumdomeinwerk en iedere kopie van
TOKA-materiaal buiten `toka/`. De exec gate-12-kanaaltelling wijzigt
niet. Deze kaart deelt beide transpilers met andere pariteitskaarten en
wordt serieel gepland.

Result: geland. De gedeelde lezer `Literalen.tekstOfEnumLabel` geeft een
`Tekst`-literal nu `lit.waarde()` verbatim terug; alleen
enumeratiewaarden raken hun gepaarde enkel-quote-delimiters kwijt (via
`enumLabel`, ongewijzigd). De private tekstlezing in `ExecModelTranspiler`
is verwijderd: de zes vergelijkings- en schendingsplekken roepen de
gedeelde lezer aan, en de waarde-plek (`scalarWaarde`) doet dat achter een
`instanceof Expr.Literal`-poort, omdat zij als enige een willekeurige
`Expr` aanbiedt. De diff aan `DrlTranspiler` raakt alleen commentaar: de
methode `literal` haalde de waarde al uit de gedeelde lezer en rendert
haar met `drlString`, dat escapeert en niets verwijdert — juist daardoor
verandert het gedrag van de classic-DRL-tak mee met de lezer. De helper is NIET hernoemd: de naam
staat op drie plekken en de javadoc draagt de betekenis nu expliciet;
hernoemen zou een grotere diff zijn zonder gedragswinst.

ROOD-EERST (jar van `main`, vóór enige code-edit).

    $ java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
        --drl corpus/golden/jutterswet-keurmerk/ir.json \
              corpus/golden/jutterswet-keurmerk/case*.json
    FAIL case.json: kenmerk gekeurd: verwacht aanwezig, werkelijk afwezig;
                    attribuut afslag: verwacht veiling "Zee", werkelijk leeg
    rc=1

    $ java -jar … (dezelfde aanroep ZONDER --drl)
    PLAYED 1
    rc=0

    scenario-toka-46-woonregio-factor-1  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-1  --drl      : rc=1  FAIL case.json
    scenario-toka-46-woonregio-factor-2  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-2  --drl      : rc=1  FAIL case.json
    scenario-toka-46-woonregio-factor-3  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-3  --drl      : rc=1  FAIL case.json

De drie FAIL-regels zijn hier ingekort tot hun status: de volledige regel
citeert een attribuutnaam en een verwachte waarde uit het
©-Belastingdienst-casusmateriaal, en die hoort nergens buiten `toka/`
(CLAUDE.md, Corpus rules). De vorm is telkens dezelfde als bij keurmerk —
de classic-DRL-tak vond het doelattribuut leeg waar het gevuld hoorde te
zijn, omdat de literal zijn buitenste aanhalingstekens verloor en de
casewaarde die hield. Beide takken draaien op ONGEWIJZIGDE IR, cases en
verwachtingen; er is geen corpus- of `toka/`-bestand aangeraakt.

GROEN (jar ná de wijziging, dezelfde aanroepen, dezelfde bestanden).

    jutterswet-keurmerk                  zonder vlag: rc=0  PLAYED 1
    jutterswet-keurmerk                  --drl      : rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-1  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-1  --drl      : rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-2  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-2  --drl      : rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-3  zonder vlag: rc=0  PLAYED 1
    scenario-toka-46-woonregio-factor-3  --drl      : rc=0  PLAYED 1

GETUIGEN. Unit, in `frontends/regelspraak/src/test/java/io/github/
yhavinga/regelspraak/kern/LiteralenTest.java`:
`tekstOfEnumLabel_leest_tekst_verbatim` (rauw lexeem, quote-dragend
lexeem, ingebed aanhalingsteken),
`tekstOfEnumLabel_normaliseert_niet_stilletjes` (rauw =/= quote-dragend)
en `tekstOfEnumLabel_stript_alleen_de_enumdelimiters` — de laatste
vervangt het oude `tekstOfEnumLabel_dekt_tekst_en_enumeratiewaarde`, dat
het strippen juist vastlegde en dus mee moest.

Dual-branch, nieuw bestand `backends/drools/src/test/java/io/github/
yhavinga/regelspraak/cases/TekstLexeemWitnessTest.java`, acht getuigen die
elk BEIDE takken op één document bevragen (`viaExecModel` naast
`viaDrlTekst`): `rauwe_tekst_vergelijkt_gelijk_in_beide_takken`,
`quotedragende_tekst_vergelijkt_gelijk_in_beide_takken`,
`ingebedde_quotes_overleven_in_beide_takken` (de ongewijzigde golden
zelf), `het_gevolg_schrijft_het_hele_lexeem_in_beide_takken`,
`een_quotedragend_literal_raakt_een_rauwe_casewaarde_niet`,
`een_rauw_literal_raakt_een_quotedragende_casewaarde_niet`,
`ongelijk_spiegelt_dezelfde_lexeemgelijkheid_in_beide_takken` en
`de_of_rij_kiest_op_het_hele_lexeem_in_beide_takken` (een `Samengesteld`
TEN_MINSTE-1 met drie beslistabelcellen). De modellen zijn synthetisch,
in geheugen gebouwd uit `corpus/golden/jutterswet-keurmerk/ir.json`, met
zelfgeschreven provincienamen; geen TOKA-herkomst, geen private pad.

Elk synthetisch model is door de arbiters gehaald voordat het werd
gepind: verifier PASS op elk model, en per paar `oracle: PASS (3
expectations)` en `clingo: PASS (3 expectations)` — behalve bij het
gevolg-getuige, waar ref `MISMATCH: V1.afslag = "\"Noordzee\"", expected
"Noordzee"` geeft en clingo hetzelfde verschil meldt; dat is precies wat
die getuige in beide Drools-takken als FAIL vastlegt.

MUTATIEPROEF, feitelijk uitgevoerd, niet beredeneerd. Twee success-shaped
no-ops:
(A) de oude toestand terug — gedeelde lezer stript, exec houdt zijn eigen
verbatimlezer: 6 van de 8 dual-branchgetuigen rood;
(B) de "fix" die alleen de private kopie weghaalt — gedeelde lezer stript,
beide takken erop: eveneens 6 van de 8 rood, plus 2 van de 3
unitgetuigen (`…leest_tekst_verbatim`, `…normaliseert_niet_stilletjes`).
Groen blijven in beide proeven alleen de quotevrije flank en de
spiegelrichting (rauw literal vs quote-dragende casewaarde). Die twee
vangen de andere kant: een lezer die in plaats van de literal juist de
casewaarde of de rauwe tekst zou gaan normaliseren, breekt op hen. Het
staat zo ook in de klassedoc van het getuigebestand.

GERICHTE CHECKS. `mvn -o -f backends/drools/pom.xml package` (mét
testrun): BUILD SUCCESS, `Tests run: 369, Failures: 0, Errors: 0,
Skipped: 0` (vloer 239). `check-regelspraak: PASS — JUnit suite green (280
tests, floor 173); 53 rgs golden(s) re-emit byte-identically (floor 38)`.
Let wel: geen van de 53 rgs-goldens draagt een `Tekst`-literal, dus deze
gate kan de tekstflank van de emissie niet zien. Het bewijs dat de
emissie ongemoeid blijft is dat `tekstOfEnumLabel` in de frontend nergens
buiten zijn eigen definitie voorkomt, plus gate 11: de byte-identiek
her-geëmitteerde `toka/casus/ir.json` draagt wél quote-omsloten
tekstliteralen. `check-toka-parity: PASS` (29 cases,
ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED, 0 divergenties).
`check-drools: PASS`.

ALLE VEERTIEN GATES, exitcode per gate, los uitgevoerd:

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

KANAALTELLING ONGEWIJZIGD. Gate 12 rapporteert `channel floor 50
non-PLAYED goldens pinned by name; played matches the pinned 148`:
`EXPECTED_PLAYED` staat nog op 148 en is niet aangeraakt, net zomin als de
kanaallijst, de divergentielijst (`value-divergence 1`) of enige
tellingsvloer. Gate 13 blijft groen met 19 door de DRL-tekst-tak gespeelde
replays (vloer 13). Er is geen script, geen spec-bestand, geen `ir.json`
en geen `case*.json` gewijzigd; `git diff --name-status main...HEAD` geeft
precies de vijf May-touch-bestanden plus de kaartverplaatsing.

BUITEN SCOPE GEHOUDEN, bewust. Case-inputnormalisatie: een casewaarde
komt verbatim binnen en blijft dat; de twee ongelijkheidsgetuigen pinnen
dat er ook aan die kant niets stilletjes wordt afgepeld. Het herschrijven
van oude IR naar één canonieke tekstvorm: geen enkel `waarde`-veld is
aangeraakt, de wire is precies gebleven wat hij was. Verder ongemoeid:
v1 typed literals, `TekstReeks`-interpolatie (die heeft in
`DrlTranspiler.tekstReeks` zijn eigen, al escapende lezing), verruiming
van de stringgrammatica, enumdomeinwerk, en het hernoemen van de helper.

VERVOLGKAART, voorstel (niet aangemaakt, deze kaart mag er niet in
overgaan): *v0-tekstlexeem naar één canonieke vorm* — v0 draagt nu twee
lexeemfamilies voor dezelfde tekstwaarde, een rauwe en een met eigen
buitenste aanhalingstekens, en beide zijn geldig omdat `waarde` per
IR-SPEC ch. 4.1 de onontlede bronstring is. De splitsing is
emittergedrag, geen corpus-toeval: `ExpressieBouwer.java:381` schrijft de
bronsnippet inclusief buitenste aanhalingstekens, waar handgeschreven
goldens rauwe lexemen dragen. Dat is verdedigbaar zolang
consumenten verbatim vergelijken (wat ze na deze kaart alle vier doen),
maar het betekent dat twee documenten die dezelfde bedoeling opschrijven
niet gelijk zijn. De keuze — canonicaliseren bij emissie, of de dubbelheid
expliciet in de spec vastleggen — hoort bij de v1-typed-value-delta en
vraagt een eigen, gemeten kaart met een `spec/CONSTRUCTS.md`-regel.

REGISTERVOORSTEL: geen. Deze kaart verandert geen IR-vorm, geen
schema, geen WF-regel en geen semantiekclausule — zij maakt een
bestaande spec-zin (IR-SPEC ch. 4.1, `value` draagt de onontlede
bronstring) waar in de tak die hem niet nakwam. `spec/PLAN.md` en
`spec/CONSTRUCTS.md` zijn dan ook niet aangeraakt.
