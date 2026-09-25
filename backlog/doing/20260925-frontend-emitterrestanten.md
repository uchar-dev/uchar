# frontend-emitterrestanten: de laatste emissies die de wire niet draagt, vóór de pin-bump

Restpunten uit het Result van de L5-kaart (open punt 6) en van de kaart
rgs-getuigen-bronloze-constructen (open punt O1), gebundeld omdat elke fix
de emissie van de frontend verandert en dus goldens raakt: na de pin-bump
zou dat een tweede bump betekenen. Maintainersbesluit 2026-09-25: deze
kaart hoort bij de laatste vóór de bump, samen met
dialect-opgave-boven-berekening en verifier-allocatie-keten.

Gemeten (2026-09-25):

- De frontend schrijft `type: null` op de builtin-aanroepen `dayCount` en
  `concatenation`; WF-034 weigert zo'n document (pre-L5 gedrag, nooit in
  een golden omdat de vorm er niet voorkomt).
- De emitter kan geen `DimensionSelection` schrijven: hij weigert de vouw
  met een dimensieselectie.
- De frontend schrijft nooit `connectingRole` op een objectcreatie, ook
  niet als de zin de rol noemt (IR-SPEC ch. 3.3 laat het veld weg en RS
  ch. 5.5 leidt de relatie af; bij een zelfgetypeerde creatie is de
  terugverwijzing niet afleidbaar, dus die vorm heeft geen
  RegelSpraak-route: de ankers wachtersketen en budgetgrens-onder en de
  golden opvolgerquorum zijn handgeschreven).
- De Java-emitter weigert een lege brontekst met een Nederlandse melding
  waar de OCaml-lader Engels spreekt (L5 open punt 2); geen wire-gevolg.

## Acceptatie (bij oppakken aanscherpen)

- (a) Per restant een rood-eerst JUnit-getuige; `type` op `dayCount` en
  `concatenation` afgeleid volgens RS ch. 2.10 (een geheel getal dagen,
  een tekst); een `DimensionSelection` geëmitteerd waar de zin er een
  schrijft, met een golden (handgerekend, ref ≡ clingo ≡ Drools waar
  gedragen); `connectingRole` geschreven waar de zin de rol noemt, met een
  golden die de zelfgetypeerde vorm uit de bron haalt — meet welke
  bestaande goldens daardoor anders emitteren (`jutterswet-wrakboeking-rgs`,
  `jutterswet-wrakboekwaarde`) en her-emitteer ze met byte-identieke cases.
- (b) De meldingstaal van de lege brontekst gelijk aan de OCaml-lader
  (Engels; gelijke zin, U10 van L5).
- (c) Gate 10 byte-identiek over alle rgs-goldens; gate 11 byte-identiek
  (de toka-casus schrijft geen van deze vormen: meten, alleen tellen).
- (d) Gate-12-pins (**voorstel**) voor nieuwe goldens; alle veertien gates
  groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/**` (nieuwe goldens
en her-emissies met byte-gelijke cases), `scripts/check-drools.sh` en
`scripts/check-noop.sh` (**voorstel**), kaartverplaatsing. Niet in deze
kaart: de spec en de verifier.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Typen: lees IR-SPEC ch. 4.2 (de `BuiltinCall`-tabel: het resultaattype van `dayCount` en `concatenation`) en RUNTIME-SEMANTICS ch. 2.10; meet eerst op een proefdocument wat de frontend nu schrijft (`type: null`) en wat de verifier zegt (WF-034); de emitter schrijft daarna precies het type dat de spec afleidt; de verifier is de arbiter (schoon). Rood-eerst JUnit per vorm.
- U2 `DimensionSelection`: `IrEmitter.java` r. ~805 weigert een dimensieselectie op een vouw (`facetWeigering`); emitteer haar als `Aggregation.dimensionSelection` (`DimensionSelection {dimension, labels}`, schema r. ~2017); golden met `bron.rgs` en handgerekende cases (ref ≡ clingo); Drools weigert gedimensioneerde invoer per vorm (bekende auto-refusal) → `EXPECTED_CHANNEL`-regel met reden (**voorstel**), meten.
- U3 `connectingRole`: de allocatie schrijft hem al (r. ~1391–1410, `verbindendeRolBinding`); de objectcreatie schrijft hem waar de zin de rol noemt (lees hoe de resolver de rol van een objectcreatie kent; is die binding afleidbaar uit het typenpaar, schrijf hem dan ook — de wire laat het veld toe en een lezer die hem heeft hoeft niets af te leiden). Her-emitteer `jutterswet-wrakboeking-rgs` en `jutterswet-wrakboekwaarde` (cases byte-gelijk; hun NOTE-diff tegen de drager krimpt — NOTE bijwerken); een golden voor de zelfgetypeerde creatie uit de bron: een tweeling van `jutterswet-opvolgerquorum` (of het kleinste anker `jutterswet-wachtersketen`/`synth2-budgetgrens-onder`; ankers zijn beschermd, de tweeling komt in `corpus/golden/`), cases byte-gelijk, ref ≡ clingo; Drools meten (de exec-tak speelt een creatie met binding).
- U4 Meldingstaal: `IrEmitter.java` r. ~1150 (lege brontekst): Engels, letterlijk de zin van de OCaml-lader (zoek haar in `spec/verifier/lib/` op "snippet"); de WF-113 (b)-melding r. ~1132 idem als die afwijkt.
- U5 Parallel bouwt de kaart dialect-opgave-boven-berekening aan de grammatica en de resolver (de lowering van de opgave-markering); jij raakt `IrEmitter.java` en alleen de resolverplek van de objectcreatierol. De hoofdsessie rebaset serieel.
- U6 Gates 1–8, 10, 11, 12, 13; pins als voorstel.
