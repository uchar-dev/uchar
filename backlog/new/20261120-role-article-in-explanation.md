# role-article-in-explanation: de uitleg schrijft "de recht" voor een rol die als "het recht" is gedeclareerd

*Opgesteld door een werkbanksessie op 2026-11-20, op verzoek van de eigenaar, als vondst uit de ronde van de marker
`niet van toepassing` op de herlevingsdatum (regelset v2/herleving-ww). Verwant aan de toolchain-kaart
`initialisation-reason-reads-zijn` (2026-11-20), die "omdat de bergloon …" in "omdat zijn bergloon …" veranderde.*

## Aanleiding

De regelset v2 declareert de rol in `v2/fundament/rules/fnd-06-feittypen.rgs` (regelspraak-rules, main 229d744):

    het recht (mv: rechten) RechtOpUitkering

Een voorwaarde over die rol, in `v2/herleving-ww/rules/hww-07-herlevingsdatum.rgs`:

    - het recht van de RechtOpUitkeringPeriode heeft de indicatie herleving recht op uitkering

De uitleg (toolchain 17739877, uchar 2c883ab74; `bin/regelspraak scenario --explanation`) maakt van de niet-vervulde
voorwaarde:

    P1 heeft geen berekende herlevingsdatum, want geen regel gold; de regel 'herlevingsdatum art. 21 lid 1' gold niet,
    want niet geldt: de recht de indicatie herleving recht op uitkering heeft

"de recht" is fout: de declaratie zegt "het recht". Gemeten 2026-11-20 op main van de regelset: 23 keer in de uitleg
van een inventaris van scenario's (werkbank, `werknotities/20261120-herleving-marker/meting/meet-gronden-basis.log`,
o.a. r. 44, 89, 134); het komt in elke uitleg van een kenmerkcontrole via die rol. De uitleg is invoer voor een
taalmodel en voor de werkbank, die haar letterlijk toont.

De zin ontstaat in de toolchain (`toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/Verbaliser.java`).
Niet vastgesteld: of de IR van uchar het gedeclareerde lidwoord van een rol meegeeft (dan is het een toolchainfout),
of niet (dan moet de IR het eerst dragen).

## Goal

Een zin van de uitleg die een rol noemt, gebruikt een lidwoord dat met de declaratie klopt, of een vorm zonder
lidwoord zoals de initialisatie ("zijn recht"). De bouwer meet waar het lidwoord verloren gaat, stelt de vorm voor
(gedeclareerd lidwoord, of "zijn"/"haar" zoals `initialisation-reason-reads-zijn`), de hoofdsessie besluit.

## Acceptance

- Een golden of negatieve proef met een rol die als het-woord is gedeclareerd, en een kenmerkcontrole via die rol in
  een voorwaarde die niet geldt: de uitleg schrijft geen "de <het-woord>".
- Het geval hierboven, op de regelset v2 van regelspraak-rules: de 23 zinnen zijn goed; geen andere zin van de uitleg
  verandert, behalve dezelfde fout elders (die noemt de Result).
- Gates groen; WERKBANK.md voor de werkbank met de nieuwe zin, zodat de werkbank haar verwachtingen en opnames kan
  volgen.

## Omvang

Klein: één vorm in de verbalisering, eventueel een veld in de IR. Geen gedrag van de regels.
