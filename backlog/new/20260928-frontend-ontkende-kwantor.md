# frontend-ontkende-kwantor: 'alle … geen X hebben' wordt als de positieve controle gelowerd

*Bijvangst van de kaart frontend-extensie-herdeclaratie (bouwer, 2026-09-28),
bevestigd door de hoofdsessie op uchar `056df376` via de distributie;
reproductie op Jutterswet-materiaal in `/tmp/v2/probe/kwantor/` (alleen
lezen). Een stil verkeerde lezing: geen diagnose, een fout antwoord.*

## Aanleiding

Een kwantorvouw over een rol met een ontkend bezittelijk kenmerk, `alle
vondsten van de Jutter geen drijfvermogen hebben`, lowert het frontend tot
dezelfde voorwaarde als de positieve vorm `alle vondsten van de Jutter
drijfvermogen hebben`: de IR-condities zijn gelijk (`negated: false`),
`diagnostics: []`. Een scenario waarin geen enkele vondst drijfvermogen
heeft, verwacht het kenmerk en krijgt het niet. De ontkende vorm met een
bijvoeglijk kenmerk (`… niet waardevol zijn`) lowert wel goed (meting van
de bouwer).

## Minimale reproductie

`056df376`, distributie `bin/regelspraak ir` en `scenario`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        is zinkend kenmerk (bijvoeglijk);
    Objecttype de Vondst (mv: Vondsten)
        het drijfvermogen kenmerk (bezittelijk);
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel zinkend
        geldig altijd
            Een Jutter is zinkend
            indien alle vondsten van de Jutter geen drijfvermogen hebben.

    Scenario alle zonder drijfvermogen: een Jutter J met vondsten V1 en V2 zonder drijfvermogen;
    verwacht: J is zinkend. Scenario een met drijfvermogen: K met W1 (heeft drijfvermogen) en W2;
    verwacht: K is niet zinkend.

    nu: check exit 0, diagnostics []; de conditie in de IR is gelijk aan die van de positieve
        vorm; scenario 1 FAIL ("characteristicPresent J zinkend (verwacht=aanwezig,
        werkelijk=afwezig)"), scenario 2 PASS.

## Acceptatie

- (a) Meet de vormruimte: `alle`/`geen van`/`ten minste één`/`er is een` ×
  bezittelijk en bijvoeglijk kenmerk × ontkend en bevestigd × `hebben`/`zijn`
  (Tabel 16), plus de ontkenning van een rolcontrole; per vorm de
  IR-conditie (`negated`) en een scenario in ref en clingo.
- (b) Het frontend lowert de ontkende bezittelijke vorm tot de ontkende
  controle (`negated: true`), gelijk aan de bijvoeglijke; rood-eerst
  JUnit-getuige met deze reproductie; de andere gemeten vormen kloppen of
  weigeren luid.
- (c) Golden (werknaam `jutterswet-zinkende-jutter`) met de vormen,
  handgerekend, in ref ≡ clingo ≡ Drools-exec (of luid in het kanaal);
  gate-12-voorstel als de exec-tak speelt.
- (d) Inertie met de methode van DIVERGENCES §6: geen bestaande bron
  verandert van IR (verwacht: geen bron gebruikt de ontkende bezittelijke
  vorm; tel het); alle zestien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**, alleen als de golden in de
exec-tak speelt), kaartverplaatsing. Niet: `backends/`, `ref/`, `spec/`.
