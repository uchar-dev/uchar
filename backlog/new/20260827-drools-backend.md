# backends/drools — the Drools backend consumes the uchar IR (loader + gate 12)

Goal: maintainer decisions 2026-08-27 (exploration wf_2857a7bd-f2e,
with the maintainer's architectural correction on question 1): the
Drools backend is built ON THE UCHAR IR — it loads `ir.json` like ref
and clingo do, never the RegelSpraak source (the source path would
bypass the waist and test the old chain instead of the IR). Deliver:

1. **The copy** (read-only from
   /Users/yeb/Developer/yhavinga/regelspraak-java, main ≥ b4ba108):
   `backends/drools` gets transpiler/ (DrlTranspiler+Stratificatie),
   transpiler/model/ (ExecModelTranspiler), runtime/ (the five
   pure-Java classes), graaf/, schema/, the five backend-only kern
   classes (Dagsoorten, Dekking, Namen, Operatoren, PeriodeDekking),
   and the fire layer (BeanCompiler, RegelSpraakEngine,
   ParametersetLader, ScenarioResultaat, CompileException, plus the
   reusable comparison core of ScenarioRunner.Loop). NOT: Verbalisator,
   ScenarioRunnerCli, ResolvedScenario/ScenarioResolver, the two giant
   src/it differential tests. Rewire the transpiler.kern
   {Literalen,Tijdrekening} imports to the frontend's kern package —
   no second copy in the repo.
2. **The ir.json→model loader — the heart of this card**: the inverse
   of IrEmitter, parsing the wire (jackson; the record types live in
   frontends/regelspraak/ir, the normative shape in spec/schema +
   IR-SPEC) into RegelSpraakModel + Resolved* forms. Discipline: every
   wire form the v0 transpiler does not carry (defaultLadders,
   functionDefs/folds, struct types/literals, Let, MapList/Invoke, …)
   is a LOUD per-form refusal ("form not carried by the v0 Drools
   backend"), never a silent skip; purely additive provenance fields
   (bronAnnotaties) may be ignored with a documented note; where both a
   bean field and its symbolic extension ride the wire, the v0 field is
   authoritative and a mismatch is a loud fault. Round-trip witness:
   loader(emit(model)) reaches an equivalent transpiler input — pin it
   on at least three structurally different goldens.
3. **The case runner**: uchar `case*.json` (instances, roles,
   parameters, asOfDate, expected) → fact beans via the existing
   Loop.beanVoor/zetAttributen/zetRollen machinery + FactModelGenerator
   beans → fresh KieSession per case → compare with the existing
   compareTo==0 discipline (scale-safe); attributeTimeline input and
   read-back is new — build it (the runtime carries segments).
4. **Build** (decision V2-B): Maven builds one SHADED Drools-runtime
   uber-jar once; the gate itself runs jar-only like gate 10
   (setup-java + cached jars). Document the shade-rebuild step.
5. **Gate 12** (`scripts/check-drools.sh`): the differential square —
   every corpus golden case whose forms the loader+transpiler carry
   MUST replay through Drools with exactly the same pinned expectations
   gates 5–6 verify for ref and clingo; the not-carried forms land in a
   reason-carrying exclusion list IN THE SCRIPT that fails when stale
   (the check-noop survivor pattern); an empty played set is a failure.
   Report counts: played, excluded per reason. Red-first shown. CI: an
   unconditional blocking job (gate-10 pattern).
6. **No-share (decision V3-a)**: CLAUDE.md restates the invariant
   co-located (the production runtime lives ONLY under
   backends/drools/runtime/; ref/ shares no code with it) and gate 1
   gains the tripwire: grep ref/ and spec/verifier/ for
   RegelSpraakRuntime/org.drools/runtime method tokens (rsPlus,
   rsVerdeelOpVolgorde, Breuk) — loud failure on a transliteration.

Acceptance:
- Gate 12 red-first then green with counts (played N, excluded per
  reason); gates 1–11 stay green (gate 1 now includes the tripwire —
  show it trip on a probe).
- The loader round-trip witness on three goldens.
- All Drools-played cases agree with the SAME expectations ref and
  clingo replay (the square, corpus-scoped, anchored to hand-written
  values).

May touch: `backends/drools/**` (new), `scripts/check-drools.sh` (new),
`scripts/check-self-contained.sh` (tripwire), `.github/workflows/
gates.yml`, `CLAUDE.md` (gate 12 + no-share restate).
Out of scope: the red-path schema change (next card), the toka leg
(third card), ladder-lowering (waits for the ladder-frontend
evolution), any toolchain commit, `spec/**` (schema untouched — the
loader READS the existing wire), `spec/CONSTRUCTS.md` (serial at
merge).
