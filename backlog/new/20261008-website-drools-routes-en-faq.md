# website-drools-routes-en-faq: de routes naar Drools en een FAQ op "Onder de motorkap"

*Vastgelegd door de hoofdsessie op 2026-10-08 op verzoek van de eigenaar.
Het werk zelf zit in de website-repo (`uchar-dev/website`); de kaart staat
hier omdat de backlog van uchar de kaarten voor werk over repo's heen bijhoudt.
Stand van de website: `a677b24`, `python3 _research/check.py` groen.*

## Wacht op

Deze kaart begint pas als `20261008-tms-omschakeling` en
`20261008-drl-teksttak-op-truth-maintenance` geland zijn (keuze van de
eigenaar, 2026-10-08). `20261007-drools-logische-afleiding` is geland, en de
eigenaar besloot daarop de truth-maintenance-tak uit te bouwen tot vervanging
van de exec-modeltak en de DRL-teksttak ook op truth maintenance te zetten
(PLAN §9, 2026-10-08); de kaart `20261006-drl-defaults-lowering` ging op in de
tweede kaart hierboven.

De diagrammen en de antwoorden beschrijven de stand ná die twee kaarten: twee
Drools-takken met truth maintenance, een in PatternDSL en een in DRL-tekst,
zonder derde tak.

Goal: `ontwikkelaar.html` ("Onder de motorkap") krijgt een sectie over de
routes van regels naar de Drools-engine, met twee diagrammen, en een FAQ. Beide
in het Nederlands en het Engels, volgens `_research/COMPONENTS.md`. Een nieuw
feitenblad onderbouwt elke bewering.

## Diagram 1: de routes in Drools, met de grenzen

Inhoud, als ASCII vastgelegd op 2026-10-08. Op de site wordt het een
`<figure class="diagram">` met een SVG in de stijl van
`_research/snippet-dsq.html`. Een diagram staat er één keer, taalneutraal
(COMPONENTS.md, "Tweetaligheid"): labels zijn namen van artefacten en API's,
en de uitleg van de grenzen staat in beide talen eronder.

```
GRENS                           ROUTE 1             ROUTE 2 en 3           ROUTE 4
                                klassieke DRL       executable model       executable model
                                                    uit DRL                direct, vanaf [C]
═══════════════════════════════════════════════════════════════════════════════════════════
[A] regeltekst                  .drl, .xls          .drl, .xls             (geen)
    publiek · tekst              │                   │
                                 │ DrlParser         │ DrlParser
─────────────────────────────────┼───────────────────┼─────────────────────────────────────
[B] PackageDescr (DRL-AST)       ▼                   ▼
    half-publiek · Java         PackageDescr        PackageDescr
                                 │                   │ drools-model-codegen
─────────────────────────────────┼───────────────────┼─────────────────────────────────────
[B2] gegenereerde Java-bron      │                   ▼
     tekst · alleen route 2–3    │                  Java-bron met
                                 │                  PatternDSL-aanroepen
                                 │                  en lambda-klassen
                                 │                   │ javac: in de Maven-
                                 │                   │ build (2) of in het
                                 │                   │ geheugen (3)
─────────────────────────────────┼───────────────────┼─────────────────────────────────────
[C] org.drools.model.Model       │                   ▼                      Model, opgebouwd
    publiek · Java-objecten      │                  Model                   door eigen code die
    drools-canonical-model       │                   │                      PatternDSL aanroept
                                 │ KnowledgeBuilder  │ drools-model-         │ KieBaseBuilder.
                                 │ + drools-mvel     │ compiler              │ createKieBase-
                                 │                   │                       │ FromModel
─────────────────────────────────┼───────────────────┼───────────────────────┼──────────────
[D] RuleImpl, KiePackage         ▼                   ▼                       ▼
    intern; hier komen alle      └───────────────────┼───────────────────────┘
    routes samen                                     │ Phreak-netwerk
─────────────────────────────────────────────────────┼─────────────────────────────────────
[E] KieBase → KieSession                             ▼
    publiek · org.kie.api       insert · update · delete · fireAllRules · getObjects
═══════════════════════════════════════════════════════════════════════════════════════════
```

De uitleg eronder:
- [A] tekst die mensen schrijven;
- [B] de syntaxboom van DRL (`drools-drl-ast`), die je ook in code kunt
  opbouwen, maar dat gebeurt zelden;
- [B2] alleen in route 2 en 3: de Java-code die Drools zelf genereert;
- [C] het canonical model, een publieke Java-API en het enige instappunt zonder
  tekst;
- [D] de interne regelvorm, geen interface voor gebruikers;
- [E] de runtime-API, voor alle routes gelijk: een afnemer ziet niet langs welke
  route de KieBase gebouwd is.

Voorwaarden: in route 1 via MVEL, in routes 2–4 als gecompileerde
Java-lambda's. Feitklassen moeten als Java-klasse bestaan vóór de stap naar
[D]; in route 2 al tijdens de Maven-build.

## Diagram 2: dezelfde lagen met uchar erin

Stand 2026-10-08. Werk het bij naar de stand na de twee kaarten van "Wacht
op": beide takken met truth maintenance, de defaults in beide met logische
stuurfeiten, en geen derde tak.

```
uchar, boven Drools
═══════════════════════════════════════════════════════════════════════════════════════════
    RegelSpraak (.rgs)                                          tekst
      │ frontend: parser, resolver, typen en eenheden
      ▼
    getypeerde IR: ir.json (uchar-ir-v1)                        JSON, schema in spec/
      │ ──► ref (OCaml) en clingo (ASP) lezen dezelfde IR, zonder Drools
      │ IrLoader
      ▼
    RegelSpraakModel en opgeloste regels                        Java, intern
      │ ──► FactModelGenerator → Java-bron → BeanCompiler (javac in het
      │     geheugen) → feitklassen in een ClassLoader, voor beide takken
      └──────────────────────────┬──────────────────────────────────────────┐
                                 │ DrlTranspiler                            │ ExecModelTranspiler
                                 │ DRL-tak: controle in gate 12,            │ exec-model-tak:
                                 │ weergave in playground en                │ productie
                                 │ regelspraak drl                          │
══ GRENS NAAR DROOLS ════════════╪══════════════════════════════════════════╪══════════════
[A] regeltekst                   ▼ DRL-tekst          route 2 en 3:          │
                                 │ KieHelper          niet gebruikt;         │
─────────────────────────────────┼──────────────────────────────────────────┼──────────────
[B] PackageDescr                 ▼ PackageDescr       uchar genereert        │
                                 │                    nergens Java-bron      │
                                 │                    voor regels            │
─────────────────────────────────┼──────────────────────────────────────────┼──────────────
[B2] gegenereerde Java-bron      │                    (leeg)                 │
─────────────────────────────────┼──────────────────────────────────────────┼──────────────
[C] org.drools.model.Model       │ KnowledgeBuilder                          │ PatternDSL-aanroepen
                                 │ + drools-mvel                             │ tijdens het draaien;
                                 │                                           │ lambda's roepen
                                 │                                           │ RegelSpraakRuntime aan
                                 │                                           ▼
                                 │                                          Model, alleen in
                                 │                                          het geheugen
                                 │                                           │ KieBaseBuilder.
                                 │                                           │ createKieBaseFromModel
─────────────────────────────────┼──────────────────────────────────────────┼──────────────
[D] RuleImpl, KiePackage         ▼ RuleImpl …                                ▼ RuleImpl …
─────────────────────────────────┼──────────────────────────────────────────┼──────────────
[E] KieBase → KieSession         ▼ KieBase van de DRL-tak                    ▼ KieBase van de
                                                                               exec-model-tak
═══════════════════════════════════════════════════════════════════════════════════════════
    gate 12 (CaseRunner): beide KieBases, per case een verse sessie, vergeleken
      met de vastgelegde uitkomsten
    regelspraak-toolchain (beslisservice, scenariorunner): alleen de exec-model-tak;
      insert bonen · fireAllRules(100_000) · lees bonen en Inconsistentie-feiten
```

Bronnen op 2026-10-08: `DroolsBackend.java` (`createKieBaseFromModel` regel 200,
`KieHelper` met het mvel-dialect regel 247), `DrlTranspiler.java` regel 88–89
(`import function` voor de hulpen van `RegelSpraakRuntime`), `BeanCompiler.java`
regel 76, en in de toolchain `RegelSpraakEngine.java` regel 57 (bouwt met
`EXEC_MODEL`).

## FAQ

Vragen zoals ze in de FAQ komen, met de kern van het antwoord. Elk antwoord
wordt opnieuw vastgesteld tegen de repo's op de landingscommits van beide
kaarten. Geen verwijzing naar de video, geen meningen, alleen feiten uit het
feitenblad.

1. **Het executable model van Drools wordt toch als Java-code gegenereerd?
   Waarom is er van de productietak geen tekst te lezen?** In Drools' eigen
   route (2 en 3) vertaalt Drools DRL naar Java-bron. uchar volgt route 4: de
   transpiler roept de PatternDSL-API tijdens het draaien zelf aan en bouwt het
   `Model` in het geheugen; er wordt geen DRL en geen Java-bron voor regels
   geschreven. Leesbaar zijn de Dekking per vorm en de DRL-tekst van de
   DRL-teksttak.
2. **Wat is de PatternDSL?** De Java-API van Drools (`drools-canonical-model`,
   pakket `org.drools.model`) om een regel op te bouwen uit patronen,
   voorwaarden en een consequentie, met lambda's. Met één regel als voorbeeld,
   in DRL en in PatternDSL.
3. **Hoe kunnen regels draaien zonder dat er per regelset Java wordt
   gecompileerd?** Een lambda is gecompileerde code plus de waarden die hij bij
   het aanmaken meekrijgt. De vormen zitten gecompileerd in de backend; per
   regelset ontstaan exemplaren met waarden uit de IR. Kanttekening: de
   feitklassen worden wel gegenereerd en in het geheugen gecompileerd.
4. **Waarom bouwt de backend het model rechtstreeks, en genereert hij geen DRL
   of Java?** Voorwaarden als gecompileerde lambda's in plaats van MVEL; de
   rekenhulpen staan op één plek in `RegelSpraakRuntime`; een regelset komt pas
   tijdens het draaien binnen, dus vooraf bronnen genereren levert niets op;
   getypeerde accessors geven directe aanroepen. De DRL-teksttak blijft als
   tweede lowering in gate 12 en als leesbare weergave.
5. **Waarom Drools?** De werkende compiler van RegelSpraak naar Drools bestond
   al; uchar begon bij dat werkende paar (README van uchar, "Why these
   languages and engines"). Drools is een volwassen productieregel-engine op de
   JVM. `ref` en clingo toetsen dat de IR geen aannames van Drools overneemt.
6. **Waarom heeft de Drools-backend geen NO_LOOP en geen salience, en wel een
   vuurplafond?** Hoe de backend met truth maintenance afleidt, waarom de
   maatregelen tegen destructief schrijven daarmee wegvielen, en waarom het
   vuurplafond blijft. Ook (aanvulling 2026-10-09, na
   `20261008-tms-aggregaties-creatie-en-fouten`): creaties vuren in fasen per
   stratum, via eigen agendagroepen die de driver van de backend één voor één
   de focus geeft, met een budget per stratum zoals de referentie-interpreter;
   en het plafond groeit met de invoer mee, als `20261008-tms-omschakeling`
   dat zo oplevert (haar punt 10).
7. **Hoe weet je dat een tak die je niet kunt lezen, klopt?** `ref`, clingo en
   beide Drools-takken worden tegen dezelfde vastgelegde uitkomsten afgespeeld,
   en die uitkomsten zijn vastgezet op handgerekende ankers, zodat een fout die
   in meer backends zit niet verdwijnt doordat ze het met elkaar eens zijn.
8. **Gebruikt de backend truth maintenance?** Ja, in beide takken (PLAN §9,
   2026-10-08), met de metingen uit de `Result:`-notities van
   `20261007-drools-logische-afleiding` en `20261008-tms-omschakeling`. Met
   één uitzondering die het antwoord moet noemen (aanvulling 2026-10-09):
   een creatie is een gesteld feit, geen logisch feit; ze vuurt alleen aan een
   fixpunt van de regellaag en blijft staan (besluit van de eigenaar
   2026-10-08, PLAN §9; RUNTIME-SEMANTICS ch. 5.5).
9. **Waarom drie backends?** Kort, met een link naar de bestaande tekst op de
   site.

## Acceptatie

- (a) Een nieuwe sectie in `ontwikkelaar.html` met een taalneutraal anker op
  het omhullende element (bijvoorbeeld `id="drools-routes"` en `id="faq"`),
  na de sectie `#gates`: beide diagrammen als SVG en de FAQ, Nederlands en
  Engels volgens COMPONENTS.md.
- (b) Een feitenblad `_research/H-drools-routes.md` met bij elke bewering een
  bron (repo, commit, bestand en regel), zoals A–G.
- (c) `python3 _research/check.py` eindigt met exit 0.
- (d) `grep -ci video ontwikkelaar.html` geeft 0.
- (e) Geen privé-repopaden, geen lokale machinepaden, en geen termen uit
  `FORBIDDEN` in `check.py`.

May touch: in de website-repo `ontwikkelaar.html`,
`_research/H-drools-routes.md`, en `assets/` als de diagrammen een stijlregel
nodig hebben; in uchar alleen de kaartverplaatsing.

Out of scope: andere pagina's (behalve één link vanaf de Drools-alinea van
`index.html`, als die nodig is), uchar-code, en deployen (`deploy.sh` is van
de eigenaar).

## Na `20261008-tms-omschakeling` (2026-10-11)

Sinds `20261008-tms-omschakeling` (2026-10-10) heet de verlaging van de exec-model-tak `TmsTranspiler`
(`transpiler/tms/`, met truth maintenance); `ExecModelTranspiler` in het schema bestaat niet meer.
