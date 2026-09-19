# drl-objectcreatie-defect: de creatie-lowering van de DRL-tekst-tak

Gemeten spanning (drl-takteller, 2026-09-19): vier goldens met precies één
`objectCreatie` spelen wel in het exec-kanaal en niet in de DRL-tekst-tak,
zonder dat die tak ze bij naam weigert. `anchor/jutterswet-rekenkamer` en
`jutterswet-rekenkamerheffing-catala` antwoorden leeg waar ref, clingo en
exec 70 / 160 / -15 / 60 geven — de toekenningen van de creatie landen
niet. `anchor/jutterswet-rekenkamer-overzee` en `synth3-kamerheffing`
vallen bij het vuren om met een reflectieve `NullPointerException` in de
consequence van de creatieregel. Sinds de takteller staan ze gepind in
`DRL_CHANNEL` (klasse D, kanalen `FAIL` en `ERROR`), dus repareren beweegt
die lijst en `EXPECTED_DRL_PLAYED` met vier omhoog.

De luide helft van hetzelfde gat staat in klasse C: `anchor/jutterswet-wachtersketen`
en `synth2-budgetgrens-onder` dragen een objectcreatie-MARKER op een
onopgeloste terugkoppeling naar Wachter, en worden door exec gewoon
gespeeld (PLAYED 2 en PLAYED 1). Repareren van die kant hoort bij de
koppelloos-kaart hieronder, niet bij deze.

NB serieel met `20260919-drl-creatie-koppelloos.md` (beide raken
`DrlTranspiler.objectCreatie`). Niet samenvoegen: het zijn disjuncte
codepaden, disjuncte goldens en disjuncte symptomen. De vier goldens van
deze kaart nemen de `rolNaarSubject == null`-tak niet — hun terugkoppeling
lóst op en het misgaan zit bij het vuren; de goldens die die tak wél nemen
staan in klasse C als `EXCLUDE`.

## Acceptatie (bij oppakken aanscherpen)

- (a) De `NullPointerException` bij het vuren is weg, of is een luide
  weigering per vorm.
- (b) De twee leeg-antwoordende goldens spelen met de gepinde waarden, of
  hun afwijking draagt een reden die zegt waarom dit v0-semantiek is.
- (c) `DRL_CHANNEL` en `EXPECTED_DRL_PLAYED` bewegen mee; de exec-pins
  bewegen niet; alle veertien poorten groen.

May touch: `backends/drools/` (`DrlTranspiler` en zijn eigen suite),
`scripts/check-drools.sh` (voorstel — beschermd pad), kaartverplaatsing.

Result: geland. Alle vier de goldens spelen, met exact de waarden die
gate 5 en gate 6 pinnen; `DRL_CHANNEL` verliest klasse D in zijn geheel
(78 -> 74 regels, en de laatste `ERROR`-regel gaat mee) en
`EXPECTED_DRL_PLAYED` gaat 132 -> 136. De exec-pins staan onveranderd:
`EXPECTED_PLAYED` 159, `EXPECTED_CHANNEL` 51 regels, het exec-rapport
regel voor regel hetzelfde.

## Het defect: twee defecten, niet één verschijning van één

De kaart vermoedde één defect met twee verschijningen. Gemeten zijn het
er twee, in verschillende zinnen van de lowering, en de kaarttekst had de
symptoomverdeling half mis — wat de lens hier corrigeert:

**(1) De koppeling wordt maar aan één kant geschreven.** `DrlTranspiler.
objectCreatie` zet `$new.set<rol>( $subject )` — de terugverwijzing op
het gecreëerde object — en verder niets. Het VOORWAARTSE veld op het
subject blijft leeg, dus `de heffing van haar rekenkamer` navigeert naar
`null` en het attribuut van het subject blijft ongeschreven. Dit raakt
ALLE VIER de goldens, op elke case met een bepaalde mark, en niet alleen
de twee die de kaart als leeg-antwoorders noemt:

    anchor/jutterswet-rekenkamer          case1 70, case2 160/-15, case3 60  -> alle leeg
    jutterswet-rekenkamerheffing-catala   case1 70, case2 160, case3 -15     -> alle leeg
    anchor/jutterswet-rekenkamer-overzee  case1 6000, case2 5000             -> beide leeg
    synth3-kamerheffing                   case1 200, case2 30                -> beide leeg

Case4 van `jutterswet-rekenkamer` (`koppeling-op-de-gemunte-zijde`) sloeg
NIET om: daar levert de case de rekenkamer zelf en schrijft de loader
beide benen (ch. 5.7a), dus het voorwaartse veld stond al gevuld.

**(2) Een lege kenmerkwaarde wordt door het primitieve veld geperst.**
Een §13.4.6.4 kenmerkwaardetoekenning lowerde naar een kale
`$new.set<k>( <waarde> )`. Het beanveld van een scalair kenmerk is een
PRIMITIEVE `boolean` (`FactModelGenerator.leden`, `isVorm`), dus een lege
waarde unboxt `null`:

    java.lang.NullPointerException: Cannot invoke "java.lang.Number.intValue()"
      because the return value of
      "sun.invoke.util.ValueConversions.primitiveConversion(…)" is null
      at java.base/sun.invoke.util.ValueConversions.unboxBoolean(…:108)

Dat treft precies de twee cases waarin het subject geen mark draagt:
`rekenkamer-overzee/case3-onbepaald` en `kamerheffing/case3-leeg-mark`.
De kaart noemde een "reflectieve NullPointerException", en dat klopt
alleen voor de VOLLE goldenrun: draait de golden al zijn cases in één
JVM, dan heeft mvel's accessor-cache de plek al geoptimaliseerd en komt
de kop eruit als `Cannot invoke "java.lang.reflect.Method.
getDeclaringClass()" because "method" is null`. Eén case tegelijk geeft
de `intValue()`-kop hierboven. Zelfde oorzaak, twee koppen, afhankelijk
van de volgorde — vandaar het vermoeden van één defect.

De rest van de aanvankelijke hypothese is gemeten en VERWORPEN: dat
`$subject.getOverzees()` de verkeerde accessorvorm zou zijn. `overzees`
is op Jutter een Booleaans ATTRIBUUT (`getOverzees()` levert `Boolean`);
alleen op Rekenkamer is het een kenmerk. De lezing was nooit het
probleem, de SCHRIJVING wel.

## De fix: ch. 5.5 in de tweede tak

Beide reparaties staan letterlijk in `RUNTIME-SEMANTICS.md` ch. 5.5, en
de exec-tak implementeert ze al — met in `ExecModelTranspiler.
objectCreatie` benoemd als "deliberate departures from `DrlTranspiler.
objectCreatie` (the ported production semantics)". Deze kaart haalt die
twee naar de DRL-tekst-tak; de spec is het product, dus een tweede
uitvoerder van het uchar-IR hoort hem uit te voeren en niet de
productie-eigenaardigheid te bewaren.

    // ch. 5.5 "both ends"
    modify($subject) {
        setRekenkamer( $new )
    }

    // ch. 5.5 "false OR empty leaves it unset"
    if (java.util.Objects.equals( $subject.getOverzees(), true )) {
        $new.setOverzees( true );
    }

Het voorwaartse schrijven staat op de 1:1-arm (`eenOpEen`), want alleen
een naar-één-rol leidt een beanveld af; op de naar-veel-arm ligt de
koppeling per constructie aan de andere kant en die werd al geschreven.
Het veld wordt bovendien alleen geschreven als het bean het ook
werkelijk DRAAGT (`FactModelGenerator.heeftReferentieVeld`) — een
gedeclareerd lid van dezelfde naam verdringt een referentie, en dan zou
de schrijving een fact in een `BigDecimal`-veld duwen. Dezelfde vraag,
in dezelfde woorden, die de exec-tak stelt.

**De kaartgrens is gerespecteerd.** De `rolNaarSubject == null`-tak (de
koppelloos-route, `// onopgelost terugkoppeling naar …`) is niet
aangeraakt: `eenOpEen` is daar per definitie onwaar, dus het voorwaartse
schrijven komt er niet. Die route markeert de hele constructie en speelt
dus nooit; de twee goldens die hem nemen
(`anchor/jutterswet-wachtersketen`, `synth2-budgetgrens-onder`) staan
onveranderd in klasse (C) en zijn de volgende kaart. Ook NIET
overgenomen, en bewust: route 1 van de exec-tak, de symbolische
`verbindendeRol`-binding. Deze tak leidt de terugkoppeling nog altijd af
uit het TYPENPAAR. Op deze vier goldens lost dat op — `overzee` en
`kamerheffing` dragen wél een binding op de draad en de paarafleiding
geeft dezelfde rol — maar een zelfgetypeerd paar lost er niet mee op, en
dat is precies wat de koppelloos-kaart bekijkt.

## Voor en na, per golden

    golden                                 voor            na
    anchor/jutterswet-rekenkamer           FAIL (3 cases)  PLAYED 4
    anchor/jutterswet-rekenkamer-overzee   ERROR at fire   PLAYED 3
    jutterswet-rekenkamerheffing-catala    FAIL (3 cases)  PLAYED 3
    synth3-kamerheffing                    ERROR at fire   PLAYED 3

Handverificatie vóór de sweep, op `anchor/jutterswet-rekenkamer`
case2-andere-waarden, tegen de case- en IR-bestanden: het feittype
`taxatieopdracht` heeft twee naar-één-rollen (`opdrachtgever` op Jutter,
`rekenkamer` op Rekenkamer), dus beide beans dragen een referentieveld en
de 1:1-arm geldt. J2 (grondslag 250, aftrek 90) munt een Rekenkamer met
diezelfde twee waarden; `heffing van een rekenkamer` geeft 250 - 90 =
**160**; `heffing van een jutter` leest dat over het voorwaartse veld.
J3 (40, 55) geeft 40 - 55 = **-15**. Dat zijn exact de twee gepinde
waarden van die case.

## De emissie bewoog ook buiten de vier

Elf goldens dragen samen twaalf `objectCreatie`s. Acht creaties kregen
het voorwaartse schrijven erbij, waaronder in goldens die al speelden
(`jutterswet-wrakboeking`, "boek een strandrecht bij elk wrakrecht") of
die om een andere reden weigeren (`penninguitgifte`, `hardheidsaftrek-
catala`, `overzeelezing-catala`). Vier kregen het niet: de twee
koppelloze hierboven, de naar-veel-penning van `penninguitgifte` (de
CreationMarker-arm), en `kamerhuishouden-catala` (loader-weigering).
Geen enkele golden verschoof van kanaal — gate 12 meldde bij de eerste
draai exact vier drifts, alle vier van `FAIL`/`ERROR` naar `PLAYED`, en
verder niets, in beide takken.

## De getuigen, en de mutatieproef

Nieuw bestand `backends/drools/src/test/java/…/cases/
DrlCreatieTakWitnessTest.java`, zeven rijen, allemaal door
`BeideTakken` — één geschreven document, dezelfde uitspraak geëist van
BEIDE takken. Geen bestaand testbestand gewijzigd. De vijf
casedocumenten zijn eerst door `ref/` gedraaid (`oracle: PASS`) en pas
daarna geasserteerd.

ROOD AANGETOOND vóór de fix: 7 tests, 3 failures en 1 error — één per
symptoomklasse plus de twee extra markwaarden:

    beide_takken_bereiken_de_mint_vanaf_het_subject
        drl-tekst — verwacht 150, werkelijk leeg
    een_ware_kenmerkwaarde_zet_de_mark_in_beide_takken
        drl-tekst — verwacht 6000, werkelijk leeg
    een_onware_kenmerkwaarde_laat_de_mark_… — verwacht 5000, werkelijk leeg
    een_lege_kenmerkwaarde_laat_de_mark_…  — ConsequenceException at fire

MUTATIEPROEF, beide kernen apart teruggedraaid en daarna samen:

    kern 1 weg (voorwaarts schrijven)  -> 7 tests, 4 failures, 0 errors
    kern 2 weg (TRUE-garde)            -> 7 tests, 0 failures, 1 error
    beide weg, jar herbouwd            -> de vier goldens terug op
                                          FAIL / ERROR / FAIL / ERROR

Die laatste is de gatekant: tegen de nieuwe pins meldt de kanaalcheck
vier drifts bij naam en leest `EXPECTED_DRL_PLAYED` 132 tegen een pin van
136 — beide richtingen van het hek vuren. Daarna hersteld en herbouwd;
suite weer 485 groen.

## De scriptwijziging (gemandateerd voorstel, beschermd pad)

`scripts/check-drools.sh`, uitsluitend de DRL-lijst, de DRL-pin en hun
redenblokken:

- Klasse (D), de vier objectcreatie-regels: VERWIJDERD. Daarmee is de
  klasse leeg en zijn (E) en (F) doorgeschoven naar (D) en (E), zoals de
  letterconventie van dat blok voorschrijft.
- Het nieuwe blok (D) — de kustwacht-divergentie — draagt één zin die
  zegt dat dit nu de ENIGE verkeerde waarde van deze tak is en dat de
  §13.4.6-klasse gerepareerd is in plaats van gepind.
- Blok (C) verwijst niet langer naar "class D below": de luide helft is
  wat er van het §13.4.6-gat over is, en dat staat er nu zo.
- Blok (A): "27 below the exec one" -> 23, "30 exec-only players" -> 26.
- Het openingsblok: "seventy-one of the seventy-eight entries" ->
  "seventy-one of the seventy-four" (A 24 + B 35 + C 12 = 71 van 74).
  NB de 71 stond er al goed sinds de vorige kaart hem rechttrok.
- `EXPECTED_DRL_PLAYED` 132 -> 136, met het bewegingsblok eronder in de
  vorm die daar staat: de datum, de kaart, de reden, en de nieuwe
  verdeling (136 gespeeld, 70 vormweigeringen, 1 verkeerde waarde, geen
  fouten bij het vuren, 2 zonder case).

Niet aangeraakt: `EXPECTED_PLAYED`, `EXPECTED_CHANNEL`, `EXCLUDE_KNOWN`,
`EXPECTED_MIN_TESTS` (de suite groeide 478 -> 485, een vloer hoeft niet
mee omhoog), en het kopblok over de listbaarheid van `ERROR` — dat
beschrijft de regel, niet de bezetting, en blijft dus waar.

## Alle veertien gates, ieder los gedraaid, exitcode per gate

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Slotstanden waar deze kaart aan raakt: gate 12 `played 159`,
`DRL-text branch played 136` (pin 136), `DRL-text floor 74`,
`JUnit 485 tests` (vloer 239), DRL-sweep 104s. Gate 13 speelt nu 22 van
zijn twistbare goldens in de DRL-tekst-tak (vloer 13, was 13 bij de
meting van 2026-09-10); geen pin daar is een gelijkheid, dus die groei
vraagt geen bump. Gate 5 543 cases / 643 worked examples, gate 6 543
cases / 423 suite-checks, gate 10 322 tests / 58 rgs-goldens, gate 14
130 checks / 2 flogic-goldens — geen van alle bewogen door deze kaart.

## Registervoorstellen (niet geland — hoofdsessie)

Geen. Deze kaart voegt geen construct toe en verplaatst geen grens: ze
laat een tweede uitvoerder doen wat ch. 5.5 al voorschrijft en wat de
eerste al deed. `spec/CONSTRUCTS.md` rij over §13.4.6 zou hoogstens de
zin "the exec branch implements ch. 5.5 where production does not"
kunnen verliezen als "beide Drools-takken" — dat is een
formuleringskwestie voor de hoofdsessie, geen delta.

## Wat open blijft

De koppelloos-route (`rolNaarSubject == null`) en de symbolische
`verbindendeRol`-binding: `20260919-drl-creatie-koppelloos.md`, serieel
hierna. Tweede punt, eerlijk: de TRUE-garde evalueert de kenmerkwaarde
tweemaal (garde en setter), net als de idempotentiegarde in `regel()`,
en leunt op dezelfde aanname dat waarde-expressies puur zijn. En anders
dan de exec-tak werpt deze tak GEEN luide fout bij een niet-booleaanse
kenmerkwaarde: mvel draagt geen `throw`, dus een niet-booleaanse waarde
blijft hier stil ongezet. WF-027 en de verifier vangen die vorm vóór
deze tak hem ziet, en de rode zone bereikt de DRL-tak helemaal niet
(gate 8 draait alleen de exec-tak) — maar het is een asymmetrie tussen
de twee takken en ze staat hier opgeschreven in plaats van weggelaten.
