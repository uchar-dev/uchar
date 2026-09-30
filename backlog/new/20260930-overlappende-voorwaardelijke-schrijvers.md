# overlappende-voorwaardelijke-schrijvers: twee voorwaardelijke schrijvers van één doel die beide gelden, drie antwoorden

*Vondst van de bouwer van opgave-markering-schrijver-op-subtype (2026-09-30,
open punt 5, gemeten als schrijvers op een supertype en een subtype), door de
hoofdsessie teruggebracht tot één type en gemeten op uchar `6b7ab8a2` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen uitvoerders;
reproductie op Jutterswet-materiaal in `/tmp/v2/probe/erflijn-schrijvers/`
(bron-een-type.rgs, scenario-een-type.rgs, cases-een-type/; bron.rgs en
scenario.rgs voor de erflijnvorm; alleen lezen). De verifier zwijgt; ref
en de exec-tak falen luid, de exec-tak met een verkeerde naam; clingo geeft
stil twee waarden; de DRL-teksttak speelt. Op akkoord van de eigenaar
geschreven (2026-09-30).*

## Aanleiding

Twee gewone voorwaardelijke regels die hetzelfde attribuut schrijven en
waarvan de voorwaarden op één instantie beide waar zijn, passeren de
verifier: WF-079 ("A (type, attribute) target that carries no defaults MUST
NOT be written by two unconditional computing versions of different forms")
dekt alleen onvoorwaardelijke schrijvers, en niets anders bewaakt de
overlap. Het document is dus welgevormd en de uitvoerders moeten het
beantwoorden. Zij antwoorden verschillend:

- `ref/` faalt luid: "EVALUATION FAULT: no fixpoint within 100 passes —
  divergence is a fault (ch. 5.7)". De twee schrijvers wisselen elkaar
  per pas af.
- De Drools-exec-tak faalt ook luid, maar noemt een oorzaak die er niet
  is: "geen fixpunt (kringverwijzing?)".
- De ASP-backend antwoordt met twee waarden voor één attribuut en geen
  `viol/2`-getuige: "MISMATCH: V.meldingsdatum = [20513;20516], expected
  20513" komt alleen van de verwachting van de case; zonder verwachting
  is het een stil dubbel antwoord, waar `backends/clingo/README.md` juist
  belooft dat elke divergentie een benoemde `viol/2`-getuige krijgt en
  nooit een tweede model.
- De DRL-teksttak speelt het document en kiest stil één van beide
  (bouwmeting p05u van de kaart opgave-markering-schrijver-op-subtype:
  `drools-drl: exit 0 — PLAYED 2`).

De erflijn is niet de oorzaak: dezelfde twee regels op één type gedragen
zich precies zo. IR-SPEC ch. 6 zegt dat de enkele-schrijverregels op de
effectieve stapel per type worden beoordeeld en "must ask whether two
writers survive into one instance's effective stack"; voor voorwaardelijke
schrijvers is die vraag nog nergens gesteld, noch statisch, noch bij de
uitvoering.

## Minimale reproductie

`6b7ab8a2`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Vondst (mv: Vondsten)
        de vinddatum Datum in dagen;
        de aanspoeldatum Datum in dagen;
        de meldingsdatum Datum in dagen;

    Regel melding op vinddag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn vinddatum
            indien zijn vinddatum gevuld is.

    Regel melding op aanspoeldag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn aanspoeldatum
            indien zijn aanspoeldatum gevuld is.

    Scenario: een Vondst V met vinddatum 04-03-2026 en aanspoeldatum 01-03-2026; de
    verwachting (meldingsdatum 01-03-2026) dient alleen om de uitvoerders te laten spreken.

    R1  bron-een-type.rgs, zoals hierboven.
        nu: check exit 0, 0 ERROR, 0 WARNING (resolved=2); scenario (exec) FAIL "geen fixpunt
        (kringverwijzing?)"; ref "EVALUATION FAULT: no fixpoint within 100 passes — divergence is
        a fault (ch. 5.7)"; clingo "MISMATCH: V.meldingsdatum = [20513;20516], expected 20513".
    R2  bron.rgs: dezelfde twee regels, de tweede op een subtype Strandvondst (aanspoeldatum op het
        subtype), scenario op een Strandvondst met beide datums.
        nu: identiek aan R1 in check, exec, ref en clingo.
    R3  R2 met alleen de vinddatum, of alleen de aanspoeldatum, gevuld.
        nu: PASS in alle drie (bouwmeting p05u case1: ref en clingo PASS, exec speelt).
    R4  dezelfde twee regels als `Regel` en `Afwijking van` erop (één boom).
        nu: exit 0 en één antwoord (de vorm van DIVERGENCES §4; de kaart bouwt hier niets).

## Acceptatie

- (a) Meet de vormruimte in de vier uitvoerders (ref, clingo, Drools-exec,
  DRL-tekst): twee en drie voorwaardelijke schrijvers op één type; op
  supertype en subtype; een gelijkstelling naast een berekening; een
  kenmerktoekenning naast een tweede; met een derde regel die het doel
  leest; met de opgave-markering op het doel; en de disjuncte vorm (de
  voorwaarden sluiten elkaar uit) als de vorm die moet blijven spelen.
- (b) Eén gedrag, vastgelegd in de spec: de standaardkeuze is de luide
  fout van `ref/` (divergentie, ch. 5.7), met een melding die de twee
  schrijvers en het doel noemt en geen kring; de ASP-backend leidt voor
  dezelfde body een benoemde `viol/2`-getuige af en geeft nooit twee
  waarden; de DRL-teksttak volgens haar kanaal. Het andere antwoord, als
  normkeuze van de eigenaar: de later geschreven regel wint, zoals ch. 7.10
  dat voor defaults zegt — dan is het een spec-wijziging in
  `RUNTIME-SEMANTICS.md` ch. 5 en een regel in alle uitvoerders.
- (c) Rode-zone-case in `corpus/rood/` (werknaam `jutterswet-dubbele-melding`)
  die ref én clingo laat falen op de body, plus een groene tweeling met de
  disjuncte vorm als golden (`jutterswet-melding-naar-aard`); rood-eerst
  JUnit-getuige in de exec-tak voor de melding; de pins van gate 8
  (rood) en gate 12 (`EXPECTED_PLAYED` 226, `EXPECTED_DRL_PLAYED` 191) als
  **voorstel**.
- (d) Of, als de statische weg gekozen wordt: een WF-regel die overlap van
  voorwaardelijke schrijvers weigert waar de verifier de overlap kan zien
  (dezelfde voorwaarde, of geen voorwaarde tegenover een voorwaarde), met
  de dynamische fout als vangnet voor de rest; IR-SPEC ch. 6 bijwerken.
  Corpussweep ongewijzigd; alle zestien gates groen.

May touch: `ref/` (de melding), `backends/clingo/**`, `backends/drools/**`,
`corpus/rood/<nieuw>/`, `corpus/golden/<nieuw>/`, `scripts/check-*.sh`
(pins, **voorstel**), `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md` alleen
met de registertekst van de eigenaar, kaartverplaatsing. NB: serieel na de
kaarten drools-exec-datumvouw-over-rol en drools-exec-vouwfilter-latere-
afleiding; niet nodig vóór de v2-regelset verder gaat (die kent de vorm als
modelfout, en ref en de exec-tak vangen haar luid in de scenariotoetsen).
