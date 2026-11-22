# let-binder-in-a-composite: een lokale naam in enig lid van een samenstelling, bij naam geweigerd

*Akkoord van de eigenaar op 2026-11-22 ("akkoord met jouw aanbevelingen"): het gat in ch. 1.5 dichten door de weigering `let-in-composite` uit te breiden, en de openingstekst en "The two requirements" van ch. 1.5 aanpassen. Mandaat
voor `spec/RUNTIME-SEMANTICS.md` ch. 1.5 (en een verwijzing elders die ernaar verwijst) en `ref/`.*

## Aanleiding

De vorm `let-in-composite` (de tabel aan het eind van ch. 1.5) weigert een lezing van de binder van een `Let` in een lid van een samenstelling **dat niet het eerste is**, en in het filter of de toets van een kwantorvouw. Een lezing in het
**eerste** lid heeft nu een lezing, en die is niet monotoon, zonder sluiting. Nagerekend met `ref`, document verifier-schoon: "daarbij geldt: t is zijn vindloon, de som van het gewicht van zijn vondsten waarvan ten minste één van: t groter is dan 5, zijn gewicht groter
is dan 10", het vindloon 10 indien taxatie groter dan 1000, één vondst van gewicht 20: zonder taxatie is de som 20 (t is open, lid 1 onbepaald, lid 2 waar), met taxatie 50 is zij vaststaand leeg (t is leeg, en de lezing van een lege binder beëindigt de
hele `Let`). Een uitkomst die stond, verandert. Drools en clingo weigeren dit document om andere redenen; ch. 1.5 en `ref` dragen het.

De rest van een `Let` lijdt er niet onder: een open binder buiten een samenstelling laat de waarde open, en een samenstelling buiten de romp van de `Let` leest haar uitkomst als één atoom.

## Goal

1. **De vorm breder.** `let-in-composite`: een `Let` waarvan de romp, op enige diepte, een lezing van haar binder heeft in een lid van een samenstelling met meer dan één lid, of in het filter of de toets van een kwantorvouw. Een samenstelling met één lid verandert niets. De tabelrij, de zinnen en de
   voorbeelden `WV-1.5-let-in-composite-*` in ch. 1.5 volgen; wat nu als "has a reading" staat met een lezing in het eerste lid, wordt een geweigerd voorbeeld. Dezelfde grens in `ref`, clingo (`refuse_uncarried`) en Drools (`UncarriedForms`), elk met getuigen, en dezelfde volgorde in hun lijsten.
2. **De tekst van ch. 1.5** (letterlijk):
   - de eerste zin van ch. 1.5 ("A document with … in three states instead of two.") wordt: "A document with `inferFromEmpty: false` (IR-SPEC ch. 2) reads a member of an object, an attribute or a characteristic, in three states instead of two: filled, settled leeg, or open, where neither stands. Where ch. 1.2–1.4 read an
     input member that the case leaves out as leeg, it is open here, unless its declaration says that its input is complete. A form that reads an open member draws nothing from it, unless its outcome stands without that read, or the form says in so many words what it does where the member is absent (a closure, below).";
     de zin "This section says what that changes; …" blijft erachter staan;
   - in "The two requirements", de opsomming van uitzonderingen: "a leeg test or a gevuld assertion over an open value (the form's own negation of a gevuld test among them), a filled-value count and a fold filter that tests on gevuld over an open value" wordt "a test on leeg or gevuld, or a gevuld assertion, over an open value
     (the form's own negation of a gevuld test among them), a filled-value count and a filter of a fold, over a set or with a quantifier, that tests on gevuld over an open value";
   - daarachter, na de zin over een afgeleid lid ("… in place of settled leeg."): "The requirement is over the input members of the objects of a case: its links are complete (but where a creation could still lay one), and adding a link or an object is not giving a member a value. It holds where both sessions
     end: an input given can make a session fault that ended without it, where an evaluation then reaches a fault, or a member that faults is no longer read provisionally."
   De zinnen die de eigenaar goedkeurde, staan hier; een regelafbreking van 72–80 kolommen zoals de rest van het hoofdstuk.

## Acceptance

- Rood eerst: het document hierboven is in `ref` vóór de wijziging speelbaar en niet monotoon (twee rode getuigen), en wordt erna geweigerd (REF-014, `let-in-composite`). Zo ook: een samenstelling in de voorwaarde van een `IfThenElse` met de lezing in het eerste lid, en genest in een samenstelling met meer leden. Een
  samenstelling met één lid, en een atoom als filter, spelen onveranderd.
- Zonder `inferFromEmpty: false` verandert geen document: de 1300+ programma's van clingo byte-gelijk, elke golden in `ref` en Drools gelijk (gates).
- Gate 5 met behoud en monotonie: tellingen gelijk aan main, of met de gemeten reden (een golden die de vorm draagt en nu REF-014 is). Een vloer die daalt: in dezelfde commit, met de reden, in `scripts/check-oracle.sh` en nergens anders; meld dat in het Result (beschermd).
- Gate 12 (Drools) groen; clingo's suite groen.

## May touch

`spec/RUNTIME-SEMANTICS.md` ch. 1.5 (en verwijzingen daarnaar), `ref/lib`, `ref/test`, `backends/clingo`, `backends/drools` (`UncarriedForms` en getuigen), hun README's; één vloer in `scripts/check-oracle.sh` als de meting dat vraagt. `spec/CONSTRUCTS.md` en `spec/PLAN.md`: voorstel in het Result.

## Result

Commits: `d5afa634f` (ch. 1.5 en `ref`), `0aa99e19f` (clingo), `1244c47a8` (Drools). Gebouwd en gemeten door bouwer 1, afgerond door bouwer 2.

De vorm `let-in-composite` weigert een lezing van de binder in enig lid van een samenstelling met meer dan één lid, en in het filter of de
toets van een kwantorvouw, in `ref` (`refuse_uncarried`), clingo (`refuse_uncarried`) en Drools (`UncarriedForms`), in de volgorde van de
tabel. De drie teksten van ch. 1.5 staan er letterlijk.

### Acceptance

- **Rood eerst.** `ref` op de basis: 7 rijen `WV-1.5-let-in-composite-*` "no refusal" (eerste lid, eerste lid van een eerste lid, één lid in
  een eerste lid, een keuze in een eerste lid, de waarde van een Let in een eerste lid, twee in een filter), en 3 tijdelijk geregistreerde
  scenario's rood in `WV-1.5-monotone`: het document van de kaart (zonder taxatie 20, met taxatie 50 vaststaand leeg, met 2000 weer 20;
  verifier-schoon), een samenstelling in de voorwaarde van een keuze, en een samenstelling in een lid van een andere. Drools op de basis:
  `a_let_read_in_a_member_of_a_composite_is_named` (gaf `if-then-else`) en `a_let_read_in_a_composite_filter_is_named` (speelde). Daarna
  REF-014, CLINGO-049 en DROOLS met `let-in-composite`. Een samenstelling met één lid en een atoom als filter spelen als daarvoor.
- **Zonder de eigenschap.** `dump_programs.py`: 1313 van 1313 programma's byte-gelijk na de drie commits. Goldens gelijk: gates hieronder.
- **Gate 5.** 279 goldens, 782 cases, 2226 runs, 401 sluitingen, gelijk aan main (op `131ba4cdb`, de oude hash van `1244c47a8`). Het enige
  document van de repository met de vorm is de golden `jutterswet-grens-per-jutter` (5 lezingen, alle in kwantorvouwen), die al geweigerd
  was. Geen vloer daalt. Suite van `ref` 4239 naar 4277.
- **Gate 12** PASS (2067 JUnit-tests, zelfde boom). De suite van clingo groen: 4797 naar 4877 na deze kaart.
- Twee mutanten op `ref` rood: "alleen de eerste lezing van een binder meldt de sluiting" en "de berekening van een binder zonder eigen
  grens".
- Vervallen getuigen (hun documenten hebben nu de vorm): in `ref` `let-in-composite-not-first-member-*` (4) en `-first-of-a-first*` (3), nu
  geweigerde rijen; `closure-of-a-binder-*` en `binder-broken-off-*` zijn herschreven op een document zonder de vorm; in clingo de rijen "the
  first member …" en "closure-of-a-binder (a deposit/fault …)".

### Voorlopige besluiten

1. De zin "each read tells its own boundary" blijft; waarneembaar is zij nu in de lezing van ch. 5.12 (fixture `let-closure`,
   `WV-5.12-closure-of-a-binder`), want een samenstelling die de naam leest, is geweigerd.
2. Twee voorbeelden van ch. 1.5 zijn vervangen, want hun documenten hebben nu de vorm (ch. 1.5, buiten de drie letterlijke teksten).

### Voorstel voor `spec/CONSTRUCTS.md` en `CLAUDE.md`

Rij 48: "a `Let` whose binder a member of a composite other than the first …" wordt "a `Let` whose binder a member of a composite with more
than one member, or the filter or the matrix of a quantifier fold, reads". `CLAUDE.md`: geen wijziging.

### Gates

Los gedraaid door de bouwers: gates 5 en 12 op `1244c47a8` (de boom van deze kaart), de clingo-suite groen. `make gates` door de hoofdsessie vóór de landing.
