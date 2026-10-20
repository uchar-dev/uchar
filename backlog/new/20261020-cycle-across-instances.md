# cycle-across-instances: een waarde die dezelfde waarde van een ander object van hetzelfde type leest, weigert `check` als kring, ook als de keten in de casus eindig is

*Grens gevonden door een regelsetbouwer, 2026-10-02 (brug K12 van de regelset herleving-WW v2, clusterdocument G7/G9,
V1), nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a`
(`bin/regelspraak check|scenario|cases`) en ref en clingo uit `_build`. Luid: `check` exit 1, geen fout antwoord. Een
keuze van de eigenaar; geen bouwdatum.*

## Aanleiding

De afhankelijkheidsgraaf heeft per objecttype en attribuut één knoop (RUNTIME-SEMANTICS ch. 6.1: "**attribute** — object
type × attribute"), en WF-045 laat een kring alleen toe door een recursieve creatie (ch. 6.2). Een regel die een waarde van
een object afleidt uit dezelfde waarde van een ander object van hetzelfde type, via een feittype met twee rollen op dat
type, sluit daardoor een kring, ook als de schakels in de casus een eindige keten vormen. De WW kent zo'n norm: de duur van
een nieuw recht wordt verlengd met het restant van het eerdere recht (art. 42b WW), en dat restant hangt af van de
maxdatum van het eerdere recht. De regelset zet de maxdatum na de verlenging daarom in een eigen lid naast de maxdatum
(brug K12): de maxdatum van het recht draagt de verlenging niet, en een volgende casus moet de verlengde maxdatum als
opgave meegeven.

Getuige:

    Eenheidsysteem Tijd
        de dag dg

    Objecttype de Vergunning (mv: Vergunningen)
        de stopdag Datum in dagen;
        de basiseinddag Datum in dagen;
        de einddag Datum in dagen;
        het restant Numeriek (geheel getal) met eenheid dg;

    Feittype vorige vergunning
        de vergunning Vergunning
        de vorige vergunning Vergunning
    Eén vergunning heeft één vorige vergunning

    Regel restant
        geldig altijd
            Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen
            indien hij aan alle volgende voorwaarden voldoet:
            - zijn stopdag is gevuld
            - zijn einddag is gevuld
            - zijn stopdag is eerder dan zijn einddag.

    Regel einddag
        geldig altijd
            De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning
            indien het restant van de vorige vergunning van de Vergunning gevuld is.

    Regel einddag zonder restant
        geldig altijd
            De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
            indien het restant van de vorige vergunning van de Vergunning leeg is.

    Casus: V1 (stopdag 01-03-2026, basiseinddag 01-06-2026, geen vorige), V2 (basiseinddag 01-12-2026, vorige vergunning
    V1). Per object: einddag V1 = 01-06-2026, restant V1 = 92 dg, einddag V2 = 03-03-2027.

    R1  zoals hierboven.
        nu: check exit 1, twee ERRORs: "ERROR (line 15): regel 'restant' hangt via een kring van zichzelf af; de
        afhankelijkheden zijn kringvrij buiten een recursieve creatie (WF-045), en een kring draagt geen negatieve rand,
        zoals een ontkenning (WF-046): dependency cycle through no recursive creation: restant -> einddag -> restant", en
        dezelfde met "restant -> einddag zonder restant -> restant". Zonder de tweede regel (geen leeg-toets) blijft de
        eerste ERROR.
    R2  de brug: "De einddag van een Vergunning moet gesteld worden op zijn basiseinddag", en een lid `verlengde einddag`
        met de eerste regel van R1.
        nu: check exit 0; scenario PASS (restant V1 92 dg, verlengde einddag V2 03-03-2027); ref en clingo PASS.

## Doel

De eigenaar kiest één van twee wegen.
- (a) De grens blijft. RUNTIME-SEMANTICS ch. 6.2 noemt dit geval als voorbeeld, en de weigering in `check` noemt het
  feittype dat twee objecten van één type verbindt, zodat de auteur ziet waarom de kring er is en dat een eigen lid de
  uitweg is.
- (b) Een stratificatie per object langs zo'n relatie: WF-045 laat een kring toe die alleen via een relatie tussen objecten
  van één type naar hetzelfde attribuut terugkeert, zoals hij nu een kring door een recursieve creatie toelaat, mits de
  schakels van die relatie in de casus geen kring vormen; een kring in de schakels (V1 heeft V2 als vorige en V2 heeft V1)
  is een benoemde fout bij het laden of de evaluatie. Eerst een spec-voorstel (ch. 6.1–6.3, de verifier), dan ref, clingo
  en beide Drools-takken. Rood-eerst met R1: `check` exit 0 en einddag V2 03-03-2027 in ref ≡ clingo ≡ Drools; de casus met
  een kring in de schakels geeft in alle drie dezelfde benoemde fout; alle gates groen.
