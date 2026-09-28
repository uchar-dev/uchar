# drools-exec-defaults-justificatie-vouw: de rechtvaardiging van een default telt niet en leest niet verder dan één rol

*Vondst van een regelsetbouwer, 2026-09-28 (brug K3 in zijn README), bevestigd
door de hoofdsessie op uchar `056df376` via de distributie; reproducties op
Jutterswet-materiaal in `/tmp/v2/probe/k1k3/` (k3a.rgs, k3b.rgs; alleen
lezen). Luid (exit 3), geen fout antwoord.*

## Aanleiding

De exec-tak vertaalt een defaultsgroep (DIVERGENCES §4) alleen als elke
rechtvaardiging reduceerbaar is: lokaal of hooguit één rol ver, zonder
aggregatie. Een `Uitzondering op` met een voorwaarde die telt ("het aantal
vondsten van de Jutter groter is dan 2") of twee rollen ver leest ("de premie
van de jutter van de vondst van de Melding") markeert de hele groep:
"defaults (…): niet-reduceerbare justificatie (far-side/aggregatie/global)".
Dezelfde voorwaarde in een gewone regel speelt (gemeten: exit 0), en één rol
ver in een default speelt ook (exit 0). Een regelset die uitzonderingen op
een recht schrijft vanuit wat zijn gronden of zijn aanvraag zeggen, moet
daardoor hulpkenmerken en dagtellingen op het recht zetten die een gewone
regel afleidt.

## Minimale reproductie

`056df376`, distributie `bin/regelspraak check`; basis B:

    Objecttype de Jutter (mv: Jutters) (bezield)
        de premie Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        het gewicht Numeriek (geheel getal);
        de bonus Numeriek (geheel getal);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    R1  B + Regel premie / geldig altijd / De premie van een Jutter moet berekend worden als 10.
        Uitzondering op premie: premie bij veel vondsten / geldig altijd /
        De premie van een Jutter moet berekend worden als 20
        indien het aantal vondsten van de Jutter groter is dan 2.
        nu: exit 3, "transpiler SKIP: defaults: defaults A:Jutter.premie — defaults (Jutter.premie):
        niet-reduceerbare justificatie (far-side/aggregatie/global): <default>".
    R2  B + een derde objecttype Melding (de gemelde premie) met Feittype VondstHeeftMelding
        (de vondst Vondst; de melding (mv: meldingen) Melding; Eén vondst heeft meerdere meldingen);
        Regel gemelde premie / … als 10; Uitzondering op gemelde premie: … als 20
        indien de premie van de jutter van de vondst van de Melding groter is dan 5.
        nu: exit 3, dezelfde zin voor Melding.gemelde premie.
    R3  B + Regel bonus / … als 1; Uitzondering op bonus: bonus bij rijke jutter / … als 2
        indien de premie van de jutter van de Vondst groter is dan 5.
        nu: exit 0 (één rol ver speelt).
    R4  R1 als gewone regel (zonder Uitzondering op, met de voorwaarde op de regel zelf).
        nu: exit 0.

## Acceptatie

- (a) Meet de vormruimte: aggregatie in de rechtvaardiging (aantal, som,
  minimum, maximum, met en zonder filter), twee en drie rollen ver, een
  parameter (global) in de rechtvaardiging, en combinaties; per vorm ref,
  clingo, exec, DRL-tekst.
- (b) De exec-tak speelt een rechtvaardiging met een aggregatie en met een
  lezing twee rollen ver, met dezelfde uitkomst als ref en clingo (de
  rechtvaardiging beslist per ch. 7 op het moment dat haar operanden er zijn;
  let op de herevaluatie als een geaggregeerde waarde later verandert, dezelfde
  familie als de kaart drools-exec-voorwaarde-genavigeerde-afleiding). Wat na
  de kaart niet meegaat, blijft een benoemde SKIP die de vorm noemt. Rood-eerst
  JUnit-getuigen.
- (c) Golden (werknaam `jutterswet-premiestaffel`) met R1 en R2 en de
  gemeten vormen, handgerekend, ref ≡ clingo ≡ Drools-exec; gate-12-voorstel;
  DRL-teksttak volgens haar kanaal (zij heeft geen defaults-lowering).
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (zelfde regelevaluatie).
