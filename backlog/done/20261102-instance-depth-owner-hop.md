# instance-depth-owner-hop: ref rekent de diepte van een instantiegeordend deel zonder kring

*Gevonden op 2026-11-01 bij het onderzoek van `20261009-drools-schrijfgrens` (ref haalde op 18 van de 175 v2-gevallen de
meetgrens niet) en op 2026-11-02 uitgezocht. Rapport, commando's en logs: `/Users/yeb/Developer/uchar-dev/research/ref-tijd-2026-11-02/`.*

Goal: ref eindigt op elke verifier-schone invoer met een instantiegeordend deel. De stap naar de eigenaar in `depth` geldt
alleen een satelliet, en een kring in die rekening is een fout met pad en labels, geen Stack overflow na 80 minuten.

## Aanleiding

Werkbank-regels `2d0aae3`, scenario `toets-g7-g9-restant-en-wekeneis`, IR en cases van uchar `3d4676b88`: 18 van de 28
gevallen (allemaal met een `eerdere recht`-link) halen in ref geen uitkomst. Drools speelt ze alle 28 in < 1 s, en clingo
speelt het verkleinde corpusgeval. Gemeten: geval 10 na 1800 s niet klaar, RSS 1,2 GB. Met `OCAMLRUNPARAM=l=8M` geeft het
`Stack overflow` na 24,5 s. Profiel: 90 % in de minor GC die de stack afloopt.

`ref/lib/eval.ml:4261-4262` neemt de eigenaarsstap ook vanaf het eerste recht. De rolnaam `recht` vindt daar via
`World.left_side` (op rolnaam, zonder feittype) de teruglink van `eerdere recht` die `ref/lib/case.ml:603` neerzette, en
`depth` (`eval.ml:4250-4266`, memo pas na de berekening) loopt R2 → R1 → R2 rond.

Sinds `9c6a5c792` (2026-10-22); gemeten op de golden `jutterswet-vergunningketen` met de eigenaarsrol hernoemd tot
`vergunning`: `cbab1246e` PASS, `9c6a5c792` tot en met `3d4676b88` Stack overflow. Met een typebewaking in een scratchkopie
halen alle 28 gevallen PASS in 0,2-1,25 s.

De beslisservice is niet geraakt: zij draait op Drools en roept ref niet aan, en de Drools-tijd per geval is tussen de
toolchain-pins `57ae7d515` en `f6082c2a` gelijk (mediaan 0,919 en 0,927 s, verschil binnen de ruis).

## Richting

In `depth` de eigenaarsstap alleen nemen vanaf een object waarvan het type de satellietkant van dat feittype vult (de
voorwaarde van `satellite_hop`, `spec/verifier/lib/checks.ml:8382-8395`). Daarnaast een markering "in bewerking" in de memo,
zodat een kring faalt met `ref/lib/eval.ml, instance order: …` en de labels, in de stijl van de `failwith` in hetzelfde blok.

Acceptance:
- (a) Rood eerst: een golden (Jutterswet) met een vergunningketen waarin de eigenaarsrol van de satellietrelatie dezelfde naam
  draagt als de zusterrol van de schakelrelatie, met een keten van twee en van drie. ref ≡ clingo ≡ beide Drools-branches op
  absolute verwachtingen, en ref binnen een seconde.
- (b) Een worked example in `ref/test/` (WV-6.4-…) voor de diepte van een object van het ketentype zonder voorganger, met een
  teruglink onder de naam van de eigenaarsrol: diepte 0.
- (c) De 28 gevallen van `toets-g7-g9-restant-en-wekeneis` halen in ref `oracle: PASS`; de meting in het Result.
- (d) `make gates` groen.

May touch: `ref/lib/eval.ml`, `ref/test/test_ref.ml`, `corpus/golden/<nieuw>/`, kaartverplaatsing. De spec niet:
ch. 6.2 (`RUNTIME-SEMANTICS.md:6293-6296`) zet een object zonder voorganger al op diepte 0.

Out of scope: dat `World.left_side` op rolnaam zonder feittype indexeert (`ref/lib/world.ml:20`). Hier is dat alleen de route
waarlangs de verkeerde stap iets vindt; of andere lezingen erdoor geraakt kunnen worden, is niet onderzocht.

## Result

Landt op `card/instance-depth-owner-hop` (voorstel; de hoofdsessie landt, ook vanwege het beschermde
`scripts/check-drools.sh`). Logs: `/Users/yeb/Developer/uchar-dev/werk/instance-depth-owner-hop/logs/`; mutatie- en
proefmateriaal: `/private/tmp/claude-501/-Users-yeb-Developer-uchar-dev-uchar/0f057885-693c-417d-8351-b7bfac514f31/scratchpad/`
(`ding/` de drie proefdocumenten, `mut/` `mutate.py`, `run.sh`, `measure.py`).

**Commits.** `c151bb53f` kaart naar doing; `e3248176e` ref: de reparatie en de tests; `12aee2179` de golden en
`EXPECTED_PLAYED`; daarna deze Result.

**De reparatie** (`ref/lib/eval.ml`). De diepte staat nu in een eigen functie `Eval.instance_depth env world0 o`
(boven `evaluate`, dat haar per deel aanroept), zodat het worked example de diepte rechtstreeks leest. Twee
wijzigingen:
- Een stap geldt alleen vanaf een object waarvan het type de rol vult die de stap verlaat (`leaves_from`: de
  rol tegenover de rol van de binding, `isa` van het objecttype op haar vuller). Dat geldt voor de stap naar de
  eigenaar (de satellietkant, de voorwaarde van `satellite_hop`) en ook voor de stap naar de voorganger (zie
  voorlopig besluit 1). De voorwaarde staat in ref zelf; ref deelt met de verifier alleen de ketendefinitie
  (`Ir_v0.Checks.subtype_of`, via `isa`), zoals elders in `eval.ml`.
- De memo heeft een markering "in bewerking" (`None`). Een object dat in bewerking opnieuw gevraagd wordt, faalt
  met `ref/lib/eval.ml, instance order: the depth of "<label>" over the switch relation "<R>" depends on itself:
  <label> -(<feittype>: <rol>)-> … -> <label>`. Een binding die geen rol van een tweerollig feittype vindt, faalt in
  dezelfde stijl (komt op verifier-schone invoer niet voor: de verifier leidt de bindingen af).

**Golden (a).** `corpus/golden/jutterswet-vergunningketen-eigenaarsrol`: de regels van `jutterswet-vergunningketen`
met de eigenaarsrol van `vergunningstermijn` genaamd `vergunning`, de naam van de zusterrol van `vorige
vergunning`; `source.rgs` (gate 10 byte-identiek), `NOTE.md` met de handberekening uit ch. 6.2, ch. 3.8 en ch. 1.2.
Twee cases met nieuwe datums: `case1-keten-van-twee` (9 verwachtingen; satellieten op diepte 0 en 1) en
`case2-keten-van-drie` (12 verwachtingen; satellieten op diepte 0 en 2, de middelste zonder termijn, zodat einddag
en verlengde einddag van de jongste verschillen). Ref `oracle: PASS` in 0,16 s en < 0,01 s; clingo `PASS (9)` en
`PASS (12)`; Drools exec-model en DRL-tekst elk `PLAYED 2`.

**Worked examples (b) en de markering** (`ref/test/test_ref.ml`, één blok tussen WV-6.4-m en WV-5.7-fixpoint, met
een eigen helper `with_depth_case`; 18 checks, de suite telt 1794):
- `WV-6.4-n`: het document van WV-6.4-m met `houder` hernoemd tot `vergunning`. Pint de teruglink op V1 onder
  `vergunning` (= `["V2"]`), de diepten V1 0, T1 0, T2 0, V2 1, T3 1, en de uitkomst van WV-6.4-m.
- `WV-6.4-o`: alleen een link van de schakelrelatie maakt een voorganger. Een Ding d met onder `vorige vergunning`
  een link van de relatie `verwijzing` (rol naar een Bijlage met die naam), en Bijlage i met eigenaar d: diepten
  d 0, i 0, V1 0; overschot i 92, verlengde einddag V1 2026-06-01; clingo `PASS (3)`.
- `instance-depth-owner-cycle`: twee Bijlagen die elkaars eigenaar zijn, terwijl Bijlage ⊑ Ding en de eigenaarsrol
  door Ding gevuld wordt. Verifier-schoon; de diepte van b1, b2 en `evaluate` falen met het pad
  `b1 -(bijlagerelatie: eigenaar)-> b2 -(bijlagerelatie: eigenaar)-> b1` (resp. vanaf b2); clingo antwoordt
  `PASS (1)` (`consistent`). Sinds de fix hieronder: diepte 0 en `consistent`.

**Rode stand.**
- Golden, op de code van `a0e0fa0ee` (de `_build` van de worktree vóór de wijziging),
  `OCAMLRUNPARAM=l=1M timeout 60 runner.exe`: beide cases `Fatal error: exception Stack overflow`, rc 2, na 0,42 s
  en 0,26 s (`logs/red-golden.log`).
- Worked example: het nieuwe blok compileert niet tegen de oude code (geen `instance_depth`). Daarom in een
  `--detach`-kopie van `e3248176e` met `instance_depth` vervangen door het oude lichaam letterlijk (geen bewaking,
  memo pas na de berekening): suite met `l=1M` rc 2, `FAIL <uncaught exception> after the last reported example
  "WV-6.4-n-back-link-under-the-owner-role"`, `Stack overflow`; de golden met de standaard-stack > 60 s (tijdgrens);
  de proeven A1 en B1 `Stack overflow` (`logs/mut-old.log`).

**Mutaties** (niet gecommit; `mut/run.sh <naam>` in een `git worktree add --detach`-kopie van `e3248176e`, daarna
verwijderd; suite met `OCAMLRUNPARAM=l=1M`; ongemuteerd daar: 1794 groen, `logs/mut-none.log`):
- eigenaarsbewaking weg (`if not (leaves_from i owner) then None` → `if false then None`): suite rc 1, 6 × FAIL
  (`WV-6.4-n-depth-V1` … `-T3`, `WV-6.4-n`) met de melding van de markering. De golden met de standaard-stack faalt
  in 0,14 s met `V1 -(vergunningstermijn: vergunning)-> V2 -(vorige vergunning: vorige vergunning)-> V1`, in plaats
  van een Stack overflow na ongeveer 80 minuten (`logs/mut-owner.log`).
- voorgangersbewaking weg: suite rc 1, 3 × FAIL (`WV-6.4-o-depth-d`, `-i`, `WV-6.4-o`) met
  `d -(vorige vergunning: vorige vergunning)-> i -(bijlagerelatie: eigenaar)-> d` (`logs/mut-pred.log`).
- markering weg (`Hashtbl.replace memo label None;` geschrapt): suite rc 2, `FAIL <uncaught exception> after the
  last reported example "instance-depth-owner-cycle-depth-V1"`, `Stack overflow`; proef B1 `Stack overflow` na
  0,85 s (`logs/mut-marker.log`).

**Is de kring na de bewaking nog te bereiken? Ja.** Redenering: de case reader eist per (objecttype, rolsleutel)
één relatie (EF-001 in `check_role_targets`, ook over de teruggezette links), dus vanaf een object dat de rol vult
die de stap verlaat, horen alle links onder die sleutel bij de relatie van de stap. Een voorgangersstap landt dan op
het ketentype, en een object van het ketentype neemt nooit een eigenaarsstap (het satelliettype is niet compatibel
met het ketentype, en een type heeft één supertype-keten). Een kring van alleen voorgangersstappen weigert EF-033
(`check_link_cycles` leest dezelfde sleutels, op elk object). Over blijft een kring van alleen eigenaarsstappen: een
satelliet die zelf eigenaar is, wat kan als het satelliettype onder de vuller van een eigenaarsrol ligt, en die vuller
is dan een strikt supertype van het ketentype. `instance-depth-owner-cycle` is zo'n document; de verifier laat het
toe. Daar meldde de markering de kring in < 0,1 s; sinds de fix hieronder staan die satellieten op 0.

**Voorlopige besluiten.**
1. Ook de stap naar de voorganger bewaakt, niet alleen die naar de eigenaar. Afweging: de kaart noemt alleen de
   eigenaarsstap, maar de voorgangersstap leest langs dezelfde weg (rolnaam zonder feittype, vanaf elk object). Op
   WV-6.4-o (verifier-schoon) gaf ref met alleen de eigenaarsbewaking de markeringsfout, terwijl ch. 6.2 d op 0 zet
   ("an object without a link of the switch relation") en clingo het geval speelt. Met beide bewakingen is het
   één regel: een stap alleen vanaf een object dat de verlaten rol vult.
2. De diepte als `Eval.instance_depth` naar het hoofdniveau, zodat (b) de diepte zelf pint in plaats van alleen de
   uitkomst. Het lichaam is per regel gelijk aan het oude, plus de bewaking en de markering.
3. Een satelliet waarvan de eigenaar geen object van de keten is (een eigenaar buiten de keten, een satelliet
   daaronder) staat op diepte 0: optie (c), een definitie van de diepte, uit het advies
   `/Users/yeb/Developer/uchar-dev/werk/instance-depth-owner-hop/fable/advies.md` (punt 2). Een kring van
   eigenaarslinks (`instance-depth-owner-cycle`) is dan geen interpreterfout meer: b1 en b2 staan op 0 en ref
   antwoordt `consistent`, zoals clingo. Het advies wees af: (a) een statische weigering (een arm van
   `WF-045.instance-read-outside`), die een document met goede regels weigert om een geval dat misschien nooit
   komt, en (b) een EF-033-weigering bij het lezen van de case, die een geval met één model weigert en `case.ml`,
   `CaseInput.java`, ch. 5.7b en een rode case kost. De spec (ch. 6.2 en ch. 5.3) is bijgewerkt als voorstel op
   deze branch.

**Pinnen en vloeren.** `scripts/check-drools.sh` `EXPECTED_PLAYED` 299 → 300 (beschermd pad, voorstel). Geen vloer
veranderd. Gemeten tellingen in `make gates`: ref-suite 1794 (+18), gate 5 972 cases, gate 12 played 300 op beide
branches.

**Meting (c).** `toets-g7-g9-restant-en-wekeneis` (IR en 28 gevallen van de frontend op `3d4676b88`, verifier
`PASS`), `_build/default/ref/bin/runner.exe <ir.json> <geval>`, standaard-stack, tijdgrens 60 s per geval, wandtijd
per proces (`mut/measure.py`, `logs/werkbank-28.tsv`): 28 × `oracle: PASS`, min 0,185 s, mediaan 0,645 s, max 1,207 s.

| geval | s | verw. | geval | s | verw. |
|---|---|---|---|---|---|
| case1 | 0,198 | 2 | case15 | 0,759 | 8 |
| case2 | 0,199 | 1 | case16 | 0,705 | 4 |
| case3 | 0,206 | 1 | case17 | 0,648 | 5 |
| case4 | 0,185 | 1 | case18 | 0,771 | 8 |
| case5 | 0,196 | 1 | case19 | 0,799 | 10 |
| case6 | 0,405 | 5 | case20 | 0,919 | 4 |
| case7 | 0,418 | 5 | case21 | 0,690 | 8 |
| case8 | 0,405 | 5 | case22 | 0,747 | 5 |
| case9 | 0,399 | 5 | case23 | 0,646 | 4 |
| case10 | 0,645 | 11 | case24 | 0,247 | 8 |
| case11 | 0,617 | 9 | case25 | 0,780 | 8 |
| case12 | 0,695 | 3 | case26 | 0,611 | 5 |
| case13 | 0,669 | 6 | case27 | 1,207 | 10 |
| case14 | 0,685 | 4 | case28 | 0,625 | 6 |

**Voorstel voor `spec/CONSTRUCTS.md`, rij 45** (achter "both goldens carry a `source.rgs` (gate 10)", in de cel
v0→v1 delta): "**Depth over the own relation (2026-11-02, card `instance-depth-owner-hop`; voorlopig besluit):** ref
takes a hop of the depth only from an object whose type fills the role the hop leaves — to the predecessor from an
object of the chain, to the owner from a satellite — since the case input keys a link by role name alone. A
satellite stands at the depth of its owner where the owner is an object of the chain and at 0 where it is not (an
owner outside the chain, a satellite among them; RUNTIME-SEMANTICS ch. 6.2, IR-SPEC ch. 5.3): a cycle of owner links
among satellites whose type lies below the filler of the owner's role, which EF-033 does not refuse, puts them at 0,
and ref answers the case as the ASP backend does. Witnesses: golden
`jutterswet-vergunningketen-eigenaarsrol` (gates 3, 5, 6, 10, 12, 17), `ref/` tests WV-6.4-n, WV-6.4-o and
`instance-depth-owner-cycle`."

**`make gates`** op `12aee2179` (`logs/gates-2.log`, `out/gates/20261102-111221/summary.txt`):

```
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 217 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 531 source files)
01-self-contained rc=0 1s
02-ir-schema rc=0 127s
04-case-schema rc=0 6s
03-verifier rc=0 35s
05-oracle rc=0 19s
06-clingo rc=0 271s
07-noop rc=0 12s
08-red rc=0 146s
09-catala rc=0 4s
10-regelspraak rc=0 225s
11-toka-parity rc=0 225s
12-drools rc=0 321s
13-reftype rc=0 264s
14-flogic rc=0 1s
15-graph rc=0 44s
16-scenario rc=0 36s
17-readings rc=0 245s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 11:17:42 321s red=0
```

Een eerste `make gates` (`logs/gates-1.log`) heb ik na ongeveer een minuut zelf gestopt (op procesgroep): bij de
start liep de suite van `drools-schrijfgrens` (`mvn -o test`), die de `pgrep`-controle vond. Daarna gewacht tot er
niets meer liep. Vóór de volle set liepen apart groen: gates 1, 2, 3, 4, 5, 6, 7, 10, 12, 13, 15, 17 en de ref-suite.

**Wat bleef liggen.**
- Besluit 3 is voorlopig (optie (c)); bevestigen of herzien is aan de maintainer.
- `World.left_side` bleef zoals het is. Wat ik zag over andere lezingen: een lezing langs een pad is veilig zolang
  het object de rol vult die de stap verlaat (de EF-001-eis hierboven). Voor de lezingen van een vorm bewaakt de
  verifier dat: een eerdere versie van proef C weigerde hij met `WF-053.hop-near-side` ("the near side fills
  'Vergunning', but the walk stands at 'Ding'"). Of elke lezing in ref onder die bewaking valt, heb ik niet
  nagelopen. Buiten die voorwaarde leest nog `Case.check_link_cycles`
  (`ref/lib/case.ml`, EF-033): de rol van de voorganger op elk object, van elk type. Een link van een andere relatie
  met die rolnaam doet dan mee in de wandeling; daardoor kan EF-033 een kring melden die geen kring van de
  schakelrelatie is (te streng, nooit te los). Niet gemeten, buiten de "May touch" van deze kaart.

**Fix na het Fable-advies** (opdracht `/Users/yeb/Developer/uchar-dev/werk/instance-depth-owner-hop/fix-prompt.md`,
advies `…/fable/advies.md`, logs `logs/fix-*`). Commit `d614df120`, gerebased op main `a6574fd4d` (de landing van
`drools-schrijfgrens`; vóór de rebase `ea20461eb` op `b3e49849a`, zonder conflict). Ref (`instance_depth`): na de
eigenaarsstap de diepte van de eigenaar alleen als `leaves_from q pred` (de eigenaar is een object van de keten),
anders 0; de recursie loopt zo alleen over de schakelrelatie, die EF-033 kringvrij houdt, en de markering blijft als
bewaking. Spec (voorstel): ch. 6.2 "Depth" met de nieuwe clausule en haar reden, ch. 6.4 een uitgewerkt voorbeeld "A
satellite outside the chain" (diepte V1, b1, b2 0, `consistent`, letterlijk de test), IR-SPEC ch. 5.3 "a satellite of a
satellite: it stands at 0". De test `instance-depth-owner-cycle` verandert met opzet: oud pinden `-b1`, `-b2` en
`-evaluate` de markeringsfout `… depends on itself: b1 -(bijlagerelatie: eigenaar)-> b2 -(bijlagerelatie: eigenaar)->
b1` (resp. vanaf b2); nieuw `-depth-b1` 0, `-depth-b2` 0 en `-evaluate` `consistent`, zoals clingo (los gedraaid:
verifier `PASS`, ref `oracle: PASS (1 expectations)` in 0,14 s, clingo `PASS (1 expectations)`). Vijf checks als
eerder, suite 1794 vóór de rebase. WV-6.4-n en WV-6.4-o ongewijzigd. Rood eerst: de nieuwe test op `b3e49849a` (`git worktree add
--detach`, testdiff toegepast) gaf rc 1, `3 failure(s)`, de drie checks hierboven met de markeringsfout
(`logs/fix-red-suite.log`). Mutatie: in die kopie op `ea20461eb` de nieuwe voorwaarde weg (`if leaves_from q pred then
depth … else 0` → `depth …`): dezelfde drie FAIL, rc 1 (`logs/fix-mut-suite.log`); kopie verwijderd. Gemeten bij de
reden in ch. 6.2: het advies laat zo'n satelliet zijn eigenaar lokaal lezen, maar ligt het satelliettype onder de
vuller van de eigenaarsrol, dan heeft de satellietrelatie de vorm van een schakelrelatie, en de verifier weigert een
lezing van een satelliet naar zijn eigenaar als `one-walk` (proeven D en D2: het document van
`instance-depth-owner-cycle` met een Bijlage die met een leeg-toets de markdag, resp. de grensdag, van haar eigenaar
leest; `/private/tmp/claude-501/-Users-yeb-Developer-uchar-dev-uchar/0f057885-693c-417d-8351-b7bfac514f31/scratchpad/depth-c/`,
`gen_d.py`, D2 is D zonder markdag).
Zonder die weigering had diepte 0 op een keten b1 → b2 → V2 (V2 op diepte 1) een leeg-toets van b1 op b2 vóór b2
laten lezen. `make gates` op `d614df120` (`logs/fix-gates-1.log`, `out/gates/20261102-115406/`; ref-suite 1807 na de
rebase, gate 5 972 cases, gate 12 played 300 op beide branches):

```
ocaml rc=0 3s ocaml-build: rebuilt (6 source path(s) newer than the stamp, e.g. backends/clingo/lib; 3s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 6s drools-jar: rebuilt (50 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/cases; 6s)
01-self-contained rc=0 2s
02-ir-schema rc=0 138s
04-case-schema rc=0 7s
03-verifier rc=0 37s
05-oracle rc=0 22s
06-clingo rc=0 277s
07-noop rc=0 13s
08-red rc=0 159s
09-catala rc=0 5s
10-regelspraak rc=0 228s
11-toka-parity rc=0 232s
12-drools rc=0 327s
13-reftype rc=0 269s
14-flogic rc=0 2s
15-graph rc=0 47s
16-scenario rc=0 38s
17-readings rc=0 249s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 11:59:39 333s red=0
```

**Landing (hoofdsessie, 2026-11-02).** Zelf nagemeten op de eerste stand: beide cases van de golden geven op ref van `3d4676b88` `Stack overflow` (`l=1M`) en op `e3248176e` PASS (9 en 12 verwachtingen); de 28 werkbank-gevallen alle `oracle: PASS`. Normvraag 3 op Fable-advies (`werk/instance-depth-owner-hop/fable/advies.md`) beslist als (c), in dezelfde landing. `make gates` op `d614df120` (basis `a6574fd4d`, met `drools-schrijfgrens`) groen; daarna alleen gerebased op `aa7fb75e1`, dat alleen backlog en `spec/CONSTRUCTS.md` raakt. CONSTRUCTS rij 45 bijgewerkt.
