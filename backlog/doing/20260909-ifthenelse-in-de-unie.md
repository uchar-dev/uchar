# IfThenElse als echt record in de gedeelde Expr-unie

Goal: de laatste `Expr.Onopgelost`-drager plus identity-gesleutelde kaart uit
`backends/drools` laten vervallen door `IfThenElse` een echt record te geven
in de gedeelde `Expr`-unie (`frontends/regelspraak/.../ir/Expr.java`), zoals
`Let` en `BinderRef` dat sinds de let-lowering al hebben.

Achtergrond. De opruimkaart 20260909-onopgelost-zijkanaal-opruiming haalde de
drager en de registry weg voor `Let` en `BinderRef`: die twee zijn sinds
commit 6fba057 gewone unie-knopen, dus de loader bouwt ze rechtstreeks in de
boom en elke generieke wandeling daalt er vanzelf in af. Voor `IfThenElse`
kon dat niet. Die knoop heeft géén record in de unie — hij is *never
emitter-produced*, dus de RegelSpraak-emitter maakt er nooit een en de unie
groeide er nooit naartoe — en de unie woont in `frontends/regelspraak`, wat
buiten het may-touch-bereik van die kaart viel. Gevolg: `IrLoader` parseert
die ene wire-vorm nog naar een `Expr.Onopgelost` plus `IrLoader.Keuze` in een
`IdentityHashMap`, en `ExecModelTranspiler.letKinderen` moet daar apart
doorheen kijken omdat een drager voor elke generieke `Expr`-wandeling een
ondoorzichtig BLAD is. Dat is precies de constructie die de opruimkaart
elders wegnam; hij staat er nu nog voor één vorm.

De meting die dit kaartje rechtvaardigt (opruimkaart, 2026-09-09). De vorm is
NIET dood hout: twaalf goldens dragen een `IfThenElse` en vijf daarvan SPELEN
in gate 12 — `anchor/jutterswet-tariefstelling`,
`jutterswet-tariefrecht-catala`, `synth3-conditielezing`,
`synth3-drempelheffing`, `synth3-takbarriere`. De vorm weigeren was daarom
geen optie: dat had die vijf uit het speelkanaal geduwd en de kanaalvloer
laten vallen, oftewel een gedragswijziging in een gedragsneutrale kaart. De
overige zeven weigert de loader al eerder op een ListType/SumType/functionDefs
(`anchor/jutterswet-staffelvergoeding`, `jutterswet-kamerhuishouden-catala`,
`jutterswet-meldplichtspoor`, `jutterswet-meldtermijnen`,
`jutterswet-penningstaffel-catala`, `synth3-metingtal`,
`synth3-vangstsom`).

Waarom dit SPEC-FIRST is en geen backend-kaart. De unie is gedeeld: de
Drools-backend compileert `frontends/regelspraak/src/main/java` mee, de
frontend zelf bouwt eruit, en de toolchain consumeert deze repo op een
`SPEC_REV`-pin. Een unie-uitbreiding raakt dus de emitter-kant en gate 10
(byte-gelijke her-emissie van de rgs-goldens), en de volgorde is die van
CLAUDE.md: de delta landt hier (spec + schema + corpus), daarna volgt de
pin-bump aan de toolchain-kant. Merk op dat `IfThenElse` al IN het schema
staat (`regelspraak-ir-v0.schema.json`, als extensieknoop met de notitie
"never emitter-produced") en al normatieve semantiek heeft
(`RUNTIME-SEMANTICS.md` ch. 10.2, WF-069 positiebeperking): het ontbreekt
alleen als Java-record.

Acceptance:
- `Expr.IfThenElse` bestaat als record in de gedeelde unie, met een arm in
  `Expr.bevat` (afdalen in beide takken plus de conditie-operanden, zoals de
  `Let`-arm afdaalt) — een ontbrekende arm is daar een compileerfout, dus dat
  is geen keuze maar een verplichting.
- `IrLoader.Keuze`, `keuzeDrager` en de `keuzes`-kaart zijn WEG uit
  `IrLoader`, `ExecModelTranspiler` en `EffectieveEenheid`; `Loaded` draagt de
  kaart niet meer.
- Gate 10 byte-gelijk (de rgs-goldens her-emitteren ongewijzigd — de emitter
  produceert de vorm niet, dus er mag daar niets bewegen) en gate 12
  byte-gelijk per golden: dezelfde uitslagregel voor alle goldens, dezelfde
  kanaalvloer, dezelfde played-telling. Gate 13 onbewogen.
- Alle dertien gates groen; geen pin verplaatst.

Aandachtspunt bij de meting. Net als bij `Let` geldt: zodra de vorm een echte
unie-knoop is, is hij voor `Expr.bevat` géén blad meer, en dan zien
`heeftTijdgebonden` (§4.2/§5.3 Rekendatum), `PeriodeDekking.parameterNamen`
en `ProducentConsument` in beginsel méér dan voorheen. Bij de `Let` bleek dat
over de hele corpus nul uitslagen en nul bytes DRL te verschuiven; verwacht
hetzelfde, maar meet het, en behandel elk verschil als een bevinding in
plaats van als een verbetering die vanzelf spreekt.

May touch: frontends/regelspraak/src/main/java/**/ir/Expr.java,
backends/drools/**, en wat gate 10 nodig heeft aan de frontend-kant.

Out of scope: gedragswijzigingen; de wire-vorm zelf (schema en semantiek
staan er al); de andere executors (ref, clingo dragen de vorm langs hun eigen
weg); de toolchain-pin-bump, die als aparte PR volgt.
