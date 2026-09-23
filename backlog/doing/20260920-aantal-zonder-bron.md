# De telling zonder bron — één arm of twee? (normvraag)

Goal: beslissen wat `AANTAL` betekent wanneer hij GEEN instantiebron
draagt en ook geen opgesomde vorm is — ariteit 1 zonder
`elementObjecttype`, en ariteit 0 — en die beslissing op de wire zetten.
Vandaag zijn beide welgevormd, antwoordt precies één uitvoerder, en
weigeren de andere drie. Gevonden bij de meting van
`backlog/done/20260920-aantal-opgesomde-telling.md`, die de opgesomde
vorm (ariteit ≥ 2) van de wire haalde en deze rand BENOEMD open liet
omdat het maintainersbesluit van die dag over "twee of meer" ging.

## De meting (2026-09-20, met de hand nagerekend)

Vier probes tegen de verifier van die kaart, elk met ref, clingo en
beide Drools-takken erachter. De eerste twee rijen zijn geland; de
laatste twee zijn deze kaart.

    elementtype  ariteit  verifier  ref                     clingo/Drools
    gezet        1        PASS      telt instanties         vertaald/gespeeld
    null/gezet   >=2      WF-037    —                       —
    null         1        PASS      gevuldtelling: 0 of 1   weigeren
    null         0        PASS      0                       weigeren

`ref` bereikt die twee via `| None, args -> ` `Values`` in
`eval_functie`'s `contributors ()`, waarna de `Aantal`-arm
`agg_aantal (filled vs)` doet — de gevuldtelling, precies de betekenis
die het setbron-besluit van 2026-09-19 en de opgesomde-kaart van
2026-09-20 allebei van deze soort weg hebben genomen. Zij is hier nog
bereikbaar, en `ref/test/` pint dat expliciet
(`WV-3.4a-eenoperand-gevuld`, `-eenoperand-leeg`, `-geen-operand`).

## De scherpste spelling: de telling van een LIJSTLITERAAL

Ariteit 1 zonder elementtype heeft een spelling die het gebrek laat zien
in plaats van het te beschrijven: één `ListLiteral`-argument
(`IR-SPEC.md` ch. 4.8, `RUNTIME-SEMANTICS.md` ch. 10.1). Gemeten
2026-09-20:

    verifier : PASS
    ref      : 1     bij een lijst van TWEE elementen
    ref      : 1     bij een LEGE lijst
    clingo   : TRANSLATION FAULT: expression kind not translated
    Drools   : EXCLUDE loader: form not carried by the v0 Drools backend:
               Expr:ListLiteral (unknown expression node)

De telling van een lijst antwoordt dus ÉÉN, ongeacht hoeveel de lijst
draagt — zij telt het OPERAND, niet de inhoud. Dat is geen randgeval van
de vraag maar de vraag zelf: een telling zonder instantiebron telt iets
wat niemand bedoelt, en zij doet het stil.

## De normvraag (maintainer)

- **(a) Eén arm: een telling MOET een elementtype dragen.** Dan vallen
  ariteit 0 en ariteit 1-zonder-elementtype samen onder één zin, en die
  zin is LETTERLIJK de tweede arm die `AANTAL_GEVULDE` sinds 2026-09-20
  draagt ("the filled count is an INSTANCE fold and MUST carry an
  element type"). De twee tellingen zouden dan dezelfde wet hebben op
  dezelfde positie, wat het sterkste argument voor deze route is.
  Kosten: nul documenten (gemeten — alle 109 tellingen in de vier zones
  dragen een elementtype).
- **(b) Twee armen.** Ariteit 0 is een vouw zonder enig argument en
  ariteit 1-zonder-elementtype is een vouw met een argument dat geen
  bron is; wie die als twee gebreken leest, wil twee zinnen en twee
  getuigen.
- **(c) Openlaten en `ref` laten weigeren.** Dan verhuist de grens naar
  de uitvoerder in plaats van naar de wire, tegen het huispatroon in
  (de wire beslist, de uitvoerder is tweede verdedigingslinie).

Aanbeveling van de meting: **(a)**. Eén zin, één arm, letterlijk
gespiegeld aan de zustersoort, nul bestaande documenten geraakt.

## Acceptatie (bij oppakken aanscherpen)

- Rood-eerst: een negatief per gekozen arm, eerst falend gemeten met de
  verifier-md5 ervóór en erna, plus een arm-exacte mutant over golden +
  rood + negatief + pariteit.
- Minstens drie spellingen wegen: ariteit 0; ariteit 1 met een gewone
  eigen lezing; ariteit 1 met een `ListLiteral`. De laatste is de
  getuige die het gebrek toont.
- `ref`'s bereikbare arm: na de beslissing is `agg_aantal (filled vs)`
  onder `Aantal` volledig onbereikbaar. Dan pas geldt wat de
  opgesomde-kaart over die arm NIET kon zeggen; de comment daar en de
  zes `WV-3.4a-opgesomd`-getuigen moeten mee bewegen, want drie ervan
  pinnen juist de bereikbaarheid.
- De producentkant MEETEN, niet aannemen. De opgesomde kaart vond een
  lek in de frontendgrammatica waar de brontaal niets spelt; dezelfde
  vraag hoort hier gesteld: kan `ExpressieBouwer` een telling zonder
  elementtype emitteren, en langs welke zin?
- Alle veertien poorten groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL,
`spec/RUNTIME-SEMANTICS.md` als VOORSTEL, `corpus/negatief/**`,
`ref/lib/eval.ml` + `ref/test/` (de comment en de getuigen van de
opgesomde kaart), en `frontends/regelspraak/**` als de meting daar een
route aanwijst.

Out of scope: de opgesomde vorm zelf — geland 2026-09-20. De
lijstwereld van ch. 4.8 als geheel; deze kaart raakt hem alleen als
ARGUMENT van een telling.

## Maintainersbesluit (2026-09-23)

OPTIE (a) — één arm: een `AANTAL`-telling MOET een elementtype dragen;
ariteit 0 en ariteit 1 zonder elementtype vallen onder één zin, letterlijk
gespiegeld aan de tweede arm die `AANTAL_GEVULDE` sinds 2026-09-20 draagt
('the filled count is an INSTANCE fold and MUST carry an element type').
Motivering: de gevuldtelling-betekenis is per de besluiten van 2026-09-19
(setbron) en 2026-09-20 (opgesomde vorm) al van `AANTAL` weggenomen en
heeft met `AANTAL_GEVULDE` een eigen soort; een bronloze telling telt het
OPERAND en niet de inhoud (de lijstliteraal-meting: antwoord 1 ongeacht de
lijstinhoud, stil) — precies de verboden klasse; de kosten zijn nul
documenten (alle 109 tellingen in de vier zones dragen een elementtype); en
beide tellingsoorten krijgen zo dezelfde wet op dezelfde positie.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/aantal-zonder-bron`, basis `main` `91880111`.

1. Hermeting van de vier-probe-tabel op de huidige stand, en de
   AANTAL-sweep over golden + rood + negatief + pariteit opnieuw — er is
   sinds 2026-09-20 veel geland (WF-110 de instance-set-unie onder meer).
2. Spec-first: een WF-037-arm "een telling MOET een elementtype dragen"
   (IR-SPEC-tekst + verifier-arm, beide VOORSTEL); het nummer motiveren.
3. Rood-eerst, drie negatiefs: ariteit 0, ariteit 1 met een eigen lezing,
   ariteit 1 met een `ListLiteral`; elk PASS onder de oude verifier (rc 0),
   FAIL onder de nieuwe, met verifier-md5 vóór en na; een arm-exacte mutant
   over golden + rood + negatief + pariteit.
4. `ref`: de waardearm onder `Aantal` wordt onbereikbaar en weigert
   benoemd (fail-fast, tweede verdedigingslinie); de zes
   `WV-3.4a`-getuigen kantelen naar die weigering, rood-eerst gemeten
   tegen de oude `eval.ml`.
5. Clingo en beide Drools-takken ná de arm gemeten; `backends/**` niet
   aangeraakt.
6. De producentkant gemeten: kan `ExpressieBouwer` een telling zonder
   elementtype emitteren, en langs welke zin — zo ja, dichten.
7. Alle veertien poorten groen, elk als losse aanroep, tellingbewegingen
   benoemd.
