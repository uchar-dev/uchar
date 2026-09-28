# drools-exec-datumvouw-over-rol: een vouw over datums langs een rol speelt niet in de exec-tak

*Vondst van een regelsetbouwer, 2026-09-28 (brug K1 in zijn README), bevestigd
door de hoofdsessie op uchar `056df376` via de distributie; reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/k1k3/` (k1a.rgs; alleen lezen). Luid
(exit 3), geen fout antwoord; wel een gat dat een regelset dwingt tot
dagtellingen als hulpattributen.*

## Aanleiding

Een regel die de vroegste of laatste datum van een verzameling langs een
rol neemt ("de eerste van de vinddag van alle vondsten van de Jutter")
resolvet en speelt in ref en clingo, maar de exec-tak markeert hem: "regel
(waarde buiten scope)". Een regelset die per grond een einddatum kent en op
het recht de laatste (of eerste) daarvan nodig heeft, schrijft daarom per
object een aantal dagen vanaf een ankerdatum en neemt daar het maximum van
(een getalvouw speelt wel; gemeten: `het aantal vondsten van de Jutter plus
1` in een berekening geeft exit 0). Dezelfde bouwer meldt dat ook een vouw
in een berekening ("aggregatie-in-arithmetiek buiten scope") in zijn model
markeerde; meet welke vorm dat was.

## Minimale reproductie

`056df376`, distributie `bin/regelspraak check`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de eerste vinddag Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel eerste vinddag
        geldig altijd
            De eerste vinddag van een Jutter moet berekend worden als de eerste van de vinddag van alle vondsten van de Jutter.

    nu: resolved=1; "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: regel: eerste vinddag —
        regel (waarde buiten scope): eerste vinddag".

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  eerste en de laatste datum over een to-many rol; dezelfde vouw met een
  voorwaarde op de elementen; een datumvouw als operand in een berekening
  (plus een duur); de getalvouwen (aantal, som, minimum, maximum) als operand
  in een berekening; twee rollen diep.
- (b) De exec-tak speelt de datumvouw over een rol (eerste/laatste), en de
  vormen uit (a) die met dezelfde lowering meegaan; wat niet meegaat blijft
  een benoemde SKIP met de vorm in de zin. Rood-eerst JUnit-getuigen.
- (c) Golden (werknaam `jutterswet-vroegste-vondst`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar eigen kanaal.
- (d) Corpussweep: played-telling en kanaal van gate 12 ongewijzigd voor de
  bestaande goldens; alle zestien gates groen (bash waar de shebang dat zegt).

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaarten
drools-exec-voorwaarde-genavigeerde-afleiding en drools-subtype-in-supertype-rol.
