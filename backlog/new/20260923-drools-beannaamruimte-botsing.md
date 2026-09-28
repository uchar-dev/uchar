# drools-beannaamruimte-botsing: attribuut en kenmerk delen één getterruimte

Gemeten door de naamnorm-lens (2026-09-23, bestond al): een attribuut
`Gehesen` naast een kenmerk `gehesen` op één objecttype is
verifier-schoon en ref speelt het, maar de Drools-beanmodel-generatie
geeft beide dezelfde getter — exec faalt met `ERROR … setter
setGehesen(List)` en de DRL-tak weigert het hele bestand bij het
compileren. Luid, geen stil fout antwoord, maar een pakketbrede of
kale fout in plaats van een benoemde per-vorm-weigering. De
naamnorm-kaart maakte het geval bereikbaar via afwijkende spelling;
de botsing zelf is een naamruimtekwestie van het beanmodel.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; meet welke declaratieparen botsen (attribuut ×
  kenmerk, en zijn er meer ruimtes — rollen?).
- (b) Benoemde weigering bij het laden (huispatroon: loader-Refusal
  die beide declaraties noemt) óf gescheiden naamruimtes in het
  beanmodel; kies de kleinste en motiveer; rood-eerst beide takken.
- (c) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

## Aanvulling (hoofdsessie, 2026-09-28): twee typenamen op één Java-klasse

Dezelfde naamruimtekwestie, nu tussen objecttypen. Gemeten op uchar
`ea6695a2` via de distributie (`bin/regelspraak check`), reproductie op
Jutterswet-materiaal; `/` scheidt regels.

    R1  Objecttype het RechtOpPremie (mv: RechtenOpPremie)
            de grondslag Numeriek (geheel getal);
        Objecttype het Recht op premie is een RechtOpPremie (mv: Rechten op premie)
            de premie Numeriek (geheel getal);
        Regel premie / geldig altijd /
        De premie van een Recht op premie moet berekend worden als de grondslag van het Recht op premie plus 1.
        nu: het frontend onderscheidt de twee typen (objecttypen=2, resolved=1); de
        beancompiler schrijft 'Recht op premie' en 'RechtOpPremie' naar één klassenaam
        en stopt met `java.lang.IllegalStateException: gegenereerde fact-beans
        compileren niet: …/facts/RechtOpPremie.java:4: error: cyclic inheritance
        involving dev.uchar.regelspraak.facts.RechtOpPremie`
        (`BeanCompiler.compileer` ← `DroolsBackend.build`); exit 1 met stacktrace,
        geen melding die de twee declaraties noemt.
    R2  R1 zonder `is een` en zonder de regel (twee losse typen).
        nu: exit 3, `contract: RechtOpPremie.grondslag: getter getGrondslag()
        ontbreekt op dev.uchar.regelspraak.facts.RechtOpPremie` — de tweede klasse
        overschrijft de eerste; de melding noemt het gevolg, niet de botsing.

Acceptatie erbij:

- (d) R1 en R2 geven één benoemde weigering die beide objecttypen en de
  gedeelde klassenaam noemt (het huispatroon van (b)), óf botsingsvrije
  klassenamen zodat beide typen spelen. De bouwer kiest de kleinste vorm die
  (b) en (d) samen dekt en motiveert; botsingsvrije namen alleen als elke
  bestaande golden zijn beans byte-gelijk houdt. Meet welke naamparen botsen:
  objecttype × objecttype (met en zonder `is een`), attribuut × kenmerk (de
  aanleiding), rollen, feittypen.
- (e) Een getuige per gekozen vorm: een negatief- of rood-getuige voor een
  weigering; bij botsingsvrije namen een golden met beide typen, in ref ≡
  clingo ≡ Drools-exec.
- (f) Alle zestien gates groen (de scripts met bash waar de shebang dat
  zegt).
