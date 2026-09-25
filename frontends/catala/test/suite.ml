(* End-to-end plugin suite. Driven by dune (runtest): the four executable
   paths arrive as argv — the plugin, the verifier, the reference runner,
   the clingo runner. For each passing Catala source: translate to IR,
   assert verifier-clean, replay every case on ref AND clingo. For each
   refusal source: assert the plugin exits non-zero with the expected
   reason fragment. No silent skips — an empty program list is a failure. *)

let read_all ic =
  let buf = Buffer.create 4096 in
  let chunk = Bytes.create 4096 in
  let rec loop () =
    let n = input ic chunk 0 4096 in
    if n > 0 then (Buffer.add_subbytes buf chunk 0 n; loop ())
  in
  loop ();
  Buffer.contents buf

(* run a command, return (exit code, combined stdout+stderr) *)
let run argv =
  let cmd = String.concat " " (List.map Filename.quote argv) ^ " 2>&1" in
  let ic = Unix.open_process_in cmd in
  let out = read_all ic in
  let code = match Unix.close_process_in ic with
    | Unix.WEXITED c -> c
    | Unix.WSIGNALED s | Unix.WSTOPPED s -> 128 + s
  in
  (code, out)

let contains ~needle haystack =
  let nl = String.length needle and hl = String.length haystack in
  if nl = 0 then true
  else
    let rec at i =
      if i + nl > hl then false
      else if String.sub haystack i nl = needle then true
      else at (i + 1)
    in
    at 0

let failures = ref 0
let ntests = ref 0

let check name ok detail =
  incr ntests;
  if ok then Printf.printf "  ok   %s\n%!" name
  else (incr failures; Printf.printf "  FAIL %s -- %s\n%!" name detail)

let () =
  let plugin, verifier, refx, clingo =
    match Sys.argv with
    | [| _; a; b; c; d |] -> (a, b, c, d)
    | _ ->
        prerr_endline "usage: suite <plugin> <verifier> <ref> <clingo>";
        exit 2
  in
  (* a passing program run: translate, assert verifier-clean, replay every
     case on ref, and (unless [ref_only]) on clingo too *)
  let run_passing ~ref_only src cases =
    Printf.printf "[passing%s] %s\n%!" (if ref_only then " ref-only" else "") src;
    let out = Filename.temp_file "catfe" ".ir.json" in
    let code, o = run [ plugin; src; out ] in
    check (src ^ " : translate") (code = 0) (Printf.sprintf "exit %d\n%s" code o);
    if code = 0 then begin
      let vc, vo = run [ verifier; out ] in
      check (src ^ " : verifier-clean") (vc = 0 && contains ~needle:"PASS" vo) vo;
      List.iter
        (fun cf ->
          let rc, ro = run [ refx; out; cf ] in
          check (cf ^ " : ref") (rc = 0) ro;
          if not ref_only then begin
            let gc, go = run [ clingo; out; cf ] in
            check (cf ^ " : clingo") (gc = 0) go
          end)
        cases
    end
  in
  (* ref + clingo passing programs: source -> its case files *)
  let passing =
    [ ("proef.catala_en",
       [ "cases/proef_12.json"; "cases/proef_6.json"; "cases/proef_25.json" ]);
      ("root_unordered.catala_en",
       [ "cases/root_12.json"; "cases/root_3.json"; "cases/root_7.json" ]);
      (* overlapping unordered grounds -> the §9.5 conflict deviation *)
      ("root_conflict.catala_en", [ "cases/conflict_12.json" ]);
      ("geld.catala_en",
       [ "cases/geld_1.json"; "cases/geld_2.json"; "cases/geld_3.json" ]);
      (* row-36 sum types: a payload enum with a total match (Construct,
         Match, BinderRef, nullary arms and sum-equality) through the full
         differential *)
      ("enum.catala_en",
       [ "cases/enum_gehonoreerd.json"; "cases/enum_afgewezen.json";
         "cases/enum_aangehouden.json" ]);
      (* a wildcard match: the desugarer makes it total before scopelang, so
         the plugin never sees a wildcard *)
      ("enum_wildcard.catala_en",
       [ "cases/wild_een.json"; "cases/wild_twee.json" ]);
      (* WF-087: an ALL-NULLARY enum normalises to the Enumeration domain — the
         value Literal, the enumeration-scrutinee Match and the ordinary enum
         equality through the full differential. The wire SHAPE is asserted
         separately below (an E2E run is blind to which variant carried it) *)
      ("enum_nullair.catala_en",
       [ "cases/getij_eb.json"; "cases/getij_vloed.json";
         "cases/getij_doodtij.json" ]);
      (* WF-087's LOWER boundary on the plugin side: ONE nullary constructor.
         A unit type is a nullary keuzetype too, so it lands on the Enumeration
         domain as well — the shape a [List.length > 1] weakening of
         [keuzetype_of] would let escape into a SumType (its verifier twin is
         negatief `wf087-unit-sumtype`). The wire assertion below is what
         makes that weakening red *)
      ("enum_unit.catala_en", [ "cases/meld_gemeld.json" ]);
      (* row-10 fold family: a map + explicit numeric fold (MapList as a fold
         source, the aanspraaktelling pattern) *)
      ("lijst.catala_en", [ "cases/lijst_twee.json"; "cases/lijst_leeg.json" ]);
      (* row-10 + row-36: maximum … or if list empty then … — the §3.5 reduce
         lowering (Optional-som, IfThenElse reducer, Fold, extraction Match)
         through the full differential, including the empty-list default *)
      ("maximum.catala_en", [ "cases/max_drie.json"; "cases/max_leeg.json" ]);
      (* row-5c structs: a record input, a field projection as an arithmetic
         operand, and a list of records with map-over-field + fold — the
         tutorial's struct shape without a subscope, through the full
         differential (StructType as a list element is WF-065) *)
      ("struct.catala_en", [ "cases/struct_gevuld.json"; "cases/struct_leeg.json" ]);
      (* row-5c/ch. 7.9 nested exception groups: the tutorial article-3 tree
         (a base with an exception carrying two unordered exceptions) rides one
         [Default.exceptions] slot. The binding spurious-conflict test of
         PHASE4-DEFAULTS §7 pinned frontend-side: only-one-sub-member (n=100)
         decides that member ($20 — flattening would fabricate a $20-vs-$10
         conflict), the form-a own consequence fires when the exception groups decide
         nothing (n=50 -> $10), the base carries the fall-through (n=5 -> $40),
         and two differing sub-members (n=200) propagate to the root — no
         write, one path-keyed §9.5 fact "Nestgroep.belasting/0" — through ref
         AND clingo *)
      ("nested_group.catala_en",
       [ "cases/nested_100.json"; "cases/nested_50.json";
         "cases/nested_5.json"; "cases/nested_200.json" ]);
      (* row-5c §6: the tutorial's rule-level let-if over money — nested
         [Let] nodes whose body is an IfThenElse (WF-069 widens into Let bodies)
         with a binder-only condition (WF-077). Both branches through the full
         differential *)
      ("let_if.catala_en", [ "cases/letif_60.json"; "cases/letif_0.json" ]);
      (* row-5c §5: Catala decimals at the pinned scale S = 4 — a percentage
         literal, the rational operators (Add/Sub/Mult/Div_rat_rat, unary
         Minus_rat) and the ToRat cast, plus a money × rate product that Catala
         rounds to the cent (Rounding{halfAwayFromZero,2}) on tutorial magnitude,
         which clingo encodes SPLIT so the scale-6 product never wraps int31 *)
      ("decimals.catala_en", [ "cases/dec_drie.json"; "cases/dec_vijf.json" ]);
      (* PHASE5C-TUTORIAL §3: a declared subscope lowered onto existing kernel —
         one ObjectCreation of the callee (the call's args as MemberInits on
         its inputs, plus the connectingRole back-link) and the output read as
         a navigated AttributeReference over that back-link; the callee's rule runs in
         the caller's fixpoint on the minted instance. Through the full
         differential (ref AND clingo) *)
      ("subscope.catala_en", [ "cases/subscope_70.json"; "cases/subscope_160.json" ]);
      (* IR-SPEC ch. 4.1a/5, WF-053: a boolean OUTPUT of a declared subscope (a
         callee characteristic) read as a CONDITION lowers to a navigated
         CharacteristicCheck over the synthesised back-link — the condition sibling
         of the value-position navigated AttributeReference, no new node. The exception
         default's guard reads `overzees` of the minted eigenaar over the back-link;
         overseas doubles the heffing (registratiezone 200 → $6000), binnengaats
         the base decides (zone 50 → $3000), both pinned against `catala
         interpret`. Through the full differential (ref AND clingo) *)
      ("overzeelezing.catala_en",
       [ "cases/overzee_hoog.json"; "cases/binnengaats.json" ]);
      (* besluit 6: a context (reentrant) subscope input lowered to a generated
         highest default above the callee's own defaults group (PHASE5-CATALA
         decision 6, Reading B). The caller's override rides above the callee's
         own default `drempel = 10`; the highest default's guard (`pas_toe`) and
         value (`eigen_drempel`) read the caller navigated over the back-link. Both
         polarities: pas_toe = true → the highest default decides (100 − 30 = 70),
         and pas_toe = false → it falls through and the callee base decides
         (100 − 10 = 90), pinned against `catala interpret`. Through the full
         differential (ref AND clingo) *)
      ("context.catala_en", [ "cases/context_wel.json"; "cases/context_niet.json" ]);
      (* PHASE5C-TUTORIAL §3 exit witness: the Catala book's tutorial_end_2_3
         through the plugin — structs, decimals, a branching exception tree with
         a total date-complementary base group, the money × computed-rate product
         (split in clingo), a declared subscope whose inputs include a struct
         (Individual) and a BOOLEAN (overseas_territories, the WF-027-relaxed
         kenmerk-init carrying a caller boolean-attribute expression), and the
         let-if household tax reading the subscope's income_tax output over the
         back-link. Three cases: the low-income binnengaats flank ($7,000), and
         the overseas-true vs overseas-false high-income flanks ($9,999.75 vs
         $4,999.70 — article_6's 25% against article_5's 30%), all pinned against
         `catala interpret`. Through the full differential (ref AND clingo) *)
      ("tutorial_end_2_3.catala_en",
       [ "cases/tutorial_binnengaats_laag.json"; "cases/tutorial_overzee_hoog.json";
         "cases/tutorial_binnengaats_hoog.json" ]);
      (* CONSTRUCTS row 2: labeled variable states as sugar over chained
         assignment. A three-step chain (bruto → na_aftrek → definitief), each
         step an ordinary internal attribute reading the previous, with an
         exception on the last step (its own defaults group). Two cases pin both
         the plain-chain values and the exception branch (verhoogd true → 95 via
         the highest default, false → 180 via the base), calibrated against
         `catala interpret`. Through the full differential (ref AND clingo) *)
      ("states.catala_en",
       [ "cases/states_normaal.json"; "cases/states_verhoogd.json" ]);
      (* CONSTRUCTS row 2 / WF-020 guard: two scopes declare the SAME
         state-bearing output variable `heffing`. The cross-scope ambiguity
         detector mangles the `#` exactly as the rule labeller does, so the
         recurring step rules are qualified by subject (`berekening Eerste
         heffing netto` vs `berekening Tweede heffing netto`) and stay globally
         unique — without the mangling they collide on WF-020 (the lens flank).
         One case pins both scopes' chains ($100 → 90 for Eerste, → 80 for
         Tweede), calibrated against `catala interpret`. Full differential *)
      ("states_twoscope.catala_en", [ "cases/states_twoscope.json" ]);
      (* besluit 1-A (2026-08-24): the per-element subscope call's functie-route.
         The tutorial-2-4 shape [map each g among xs to (S of {…}).f] over a
         PURE-FUNCTION callee lowers S to a FunctionDef (row 10) and the map body
         to [MapList(xs, λg. Invoke(S, g))] — no instance, no creation key (the
         per-element identity is the list position, clingo's lelem index). Here
         `Kamer` is pure (one output, plain total base, an ADDITIVE body), and
         the caller folds the mapped list to a SCALAR — the covered, triple-green
         form. grondslagen [1;2;3] -> each +100 -> sum 306, through the full
         differential (ref AND clingo) *)
      ("subscope_perelement.catala_en", [ "cases/perelement_drie.json" ]);
      (* PHASE5C-TUTORIAL §3a (the fusie-lowering): the Catala book's tutorial 2-4
         through the plugin — a whole-struct per-element map landing in an INTERNAL
         [list of HouseholdTaxIndividualComputation], consumed only per field and
         FUSED to per-field [MapList(individuals, λi. Invoke(<callee> <field>, i))];
         MULTI-OUTPUT (household_tax + deduction → two FunctionDefs); a NESTED
         Invoke (the callee's own IncomeTaxComputation subscope); the money ×
         computed-rate product ($10,000·(1+n/2)) over the widened closed-body
         fence; and the deduction cap on the STATES (total_deduction base→capped,
         household_tax base→deduction). Three cases pinned against `catala
         interpret`: a two-person household ($21,750; total_deduction $8,250), the
         cap flank ($11,500; total_deduction capped at $8,500), and the exempt-rate
         edge ($25,000; $0). Through the full differential (ref AND clingo) *)
      ("tutorial_end_2_4.catala_en",
       [ "cases/tutorial24_meerpersoons.json"; "cases/tutorial24_capflank.json";
         "cases/tutorial24_randgeval.json" ]);
      (* PHASE5C-TUTORIAL §3a MULTI-OUTPUT, the direct-map-body sibling: a
         per-element callee with two outputs, of which only `.bedrag` is read in
         the map body, synthesises exactly `KamerMulti bedrag` (grondslag + 100)
         and folds to a scalar. grondslagen [1;2;3] -> 306, ref AND clingo *)
      ("subscope_perelement_multi.catala_en", [ "cases/perelement_drie.json" ]) ]
  in
  List.iter (fun (src, cases) -> run_passing ~ref_only:false src cases) passing;
  (* ref-only passing programs. datum: clingo cannot translate a date literal
     in a term position (dates are civil day numbers from case input, not IR
     literals). functie: clingo translates an Invoke inside a fold/map body
     (the aanspraaktelling anchor) but not yet a rule-level Invoke — a recorded
     backend limit, not a plugin bug. *)
  let passing_ref_only =
    [ ("datum.catala_en", [ "cases/datum_najaar.json"; "cases/datum_voorjaar.json" ]);
      ("functie.catala_en", [ "cases/functie_tien.json" ]) ]
  in
  List.iter (fun (src, cases) -> run_passing ~ref_only:true src cases) passing_ref_only;
  (* Wire-shape assertions for the keuzetype normalisation (WF-087, maintainer
     decision 2026-09-01). Which of the two wire variants a Catala enum lands
     on is decided by its payloads, and an E2E replay cannot see that choice —
     both programs below evaluate identically whichever form carried them. So
     the emitted JSON is asserted directly, in both directions: an all-nullary
     enum is the Enumeration domain and carries NO SumType/Construct anywhere,
     a payload-carrying enum keeps the row-36 SumType and never becomes an
     Enumeration. Since M1 the declaration carries its name once, as the
     Domain wrapper's "name", and "domain" is left only at the reference
     positions, so both are asserted. *)
  let wire =
    [ ("enum_nullair.catala_en",
       [ "\"node\": \"Enumeration\""; "\"name\": \"Getijstand\"";
         "\"domain\": \"Getijstand\""; "\"constructor\": \"Doodtij\""; "\"binder\": null" ],
       [ "SumType"; "Construct" ]);
      (* the same assertion at the rule's LOWER boundary — one constructor is
         still all-nullary, so still the Enumeration domain and never a SumType *)
      ("enum_unit.catala_en",
       [ "\"node\": \"Enumeration\""; "\"name\": \"Meldstatus\"";
         "\"domain\": \"Meldstatus\""; "\"constructor\": \"Gemeld\""; "\"binder\": null" ],
       [ "SumType"; "Construct" ]);
      ("enum.catala_en",
       [ "\"node\": \"SumType\""; "\"node\": \"Construct\"" ],
       [ "Enumeration" ]) ]
  in
  List.iter
    (fun (src, present, absent) ->
      Printf.printf "[wire] %s\n%!" src;
      let out = Filename.temp_file "catfe" ".ir.json" in
      let code, o = run [ plugin; src; out ] in
      check (src ^ " : translate (wire)") (code = 0) (Printf.sprintf "exit %d\n%s" code o);
      if code = 0 then begin
        let ic = open_in_bin out in
        let ir = read_all ic in
        close_in ic;
        List.iter
          (fun needle ->
            check
              (Printf.sprintf "%s : wire carries %s" src needle)
              (contains ~needle ir) "absent from the emitted IR")
          present;
        List.iter
          (fun needle ->
            check
              (Printf.sprintf "%s : wire free of %s" src needle)
              (not (contains ~needle ir)) "present in the emitted IR")
          absent
      end)
    wire;
  (* refusal programs: source -> expected reason fragment. Besluit 4's four
     forms plus the empty-operand and literal-false refusals; the subscope
     fence carries the boundary proof that the remaining tutorial construct
     (the subscope call) still falls outside the fragment — decimals, let-in
     and branching exceptions now land (row 5c). *)
  let refusals =
    [ (* besluit 6 landed the attribute-target highest default; three narrowed
         remnants of the old blanket context refusal each stay recorded flanks, one
         fixture per flank (CONSTRUCTS row 9): a context override on a CHARACTERISTIC
         (boolean) callee target — a decided attribution, not a value; a caller
         override that BRANCHES BELOW THE ROOT (a nested exception tree, not a
         plain-default chain); and a callee OWN DEFAULT that is itself ROOT-UNORDERED
         (no single base for a highest default to sit above) *)
      ("refuse_context.catala_en", "characteristic");
      ("refuse_context_branching.catala_en", "branching tree");
      ("refuse_context_owndefault.catala_en", "root-level unordered own default");
      (* PHASE5C-TUTORIAL §3 subscope flanks — declared subscopes lower
         (subscope.catala_en), and the per-element PURE-FUNCTION call now lowers
         too (subscope_perelement.catala_en, besluit 1-A), but these shapes stay
         refused, each loudly: the inline/anonymous call (2-1), a whole-struct
         output read, and a subscope reading a callee's multi-state output (a
         standalone state chain now lands as sugar — states.catala_en — only the
         bare cross-scope output read has no image) *)
      ("refuse_subscope.catala_en", "subscope");
      ("refuse_subscope_wholestruct.catala_en", "whole struct");
      ("refuse_subscope_multistate.catala_en", "multi-state");
      (* besluit 1-A weiger-grens (refined 2026-08-25, PHASE5C-TUTORIAL §3a) —
         the functie-route lowers a PURE callee; multi-output and a nested
         pure-function subscope now LAND (subscope_perelement_multi.catala_en,
         tutorial_end_2_4.catala_en), but these impurities stay refused, each
         loudly: a top-level non-input read (an internal/output variable in the
         body); a top-level unordered/conflict-group output (no single closed
         body); a NESTED sub-callee needing a real instance — a conflict channel
         (refuse_perelement_creation: échte creatie, Direction B); a NESTED
         sub-callee with a non-input read (niet-zuivere sub-callee); and a
         whole-struct per-element map declared as an OUTPUT (a list that must
         exist on the wire, no clingo image) *)
      ("refuse_perelement_noninput.catala_en", "non-input read");
      ("refuse_perelement_conflict.catala_en", "unordered/conflict exception groups");
      ("refuse_perelement_creation.catala_en", "unordered/conflict exception groups");
      ("refuse_perelement_nested_noninput.catala_en", "non-input read");
      ("refuse_perelement_wholestruct_output.catala_en", "OUTPUT");
      (* the fusie-lowering lens flank (2026-08-25): a fused whole-struct
         per-element map INTERNAL is excluded from [vars], so any consumption
         OTHER than a bare per-field map — a list-length (`number of shares`) or
         a whole read into an output (`picked equals shares`, Direction B) —
         must refuse loudly, not raise [Not_found] on the absent [vars] entry
         (exit 2, an uncaught internal error). Both scalar_read and kenmerk_read
         now mirror the sibling [is_subscope] guard *)
      ("refuse_perelement_wholestruct_read.catala_en", "read other than per field");
      ("refuse_perelement_wholestruct_whole.catala_en", "read other than per field");
      (* besluit 1-A lens round — two ref-only lowerings the map-body/functie
         route let slip past: (1) the same inline call written directly in a
         FOLD reducer body (not the map body) — its Invoke would land as a
         closed_term the clingo backend cannot translate; the gate is the DIRECT
         map body ([in_map_body]), not the broader [in_body]; (2) a callee with a
         binary-subtraction body — [Verminderd_met] is outside clingo's closed-body
         additive fence (Plus/Min; card clingo-bodymult widened it to ×, the
         money×rate cent product and a constant-divisor division, but NOT binary
         subtraction). Each refuses loudly per element, not ref-only IR *)
      ("refuse_perelement_foldcall.catala_en", "non-map closed body");
      ("refuse_perelement_subtraction.catala_en", "non-carried binary operator");
      (* a shared non-trivial base under all members of a root-level unordered
         group stays refused (PHASE4-DEFAULTS §7; nested groups carry
         below-root branching, not this) *)
      ("refuse_sharedbase.catala_en", "shared non-trivial base");
      ("refuse_bool_value.catala_en", "value position");
      (* besluit 4-A, the navigated sibling: a boolean OUTPUT of a declared
         subscope (a callee characteristic) read in a VALUE position — here an
         equality operand — refuses loudly, mirroring the own-boolean value
         guard. Read as a CONDITION it lowers (overzeelezing.catala_en); the
         value position has no image and would emit a dangling AttributeReference the
         verifier admits but ref/clingo split on *)
      ("refuse_subscope_bool_value.catala_en", "value position");
      ("refuse_empty_arith.catala_en", "operand");
      (* row-36 fences: a partial match (WF-063 totality) and a tuple payload
         (an n-ary constructor payload — single-payload constructors only)
         each refuse loudly *)
      ("refuse_partialmatch.catala_en", "total");
      ("refuse_tuple.catala_en", "tuple");
      (* the two honest narrowings of the WF-087 normalisation (maintainer
         decision 2026-09-01): an all-nullary opsomming is an Enumeration, not
         a SumType, and WF-065/WF-071 admit no enumeration as a list element
         or a struct field. Both carriers demand the payload form; before the
         normalisation both of these lowered. The spec speaks, not the
         plugin — each refusal cites its rule *)
      ("refuse_list_nullary_enum.catala_en", "WF-065");
      ("refuse_struct_nullary_enum.catala_en", "WF-071");
      (* row-5c §5: a decimal literal that does not terminate at the pinned
         scale S = 4 is refused loudly at the source (the leeg-barrier for
         precision) *)
      ("refuse_nonterminating.catala_en", "terminate");
      (* row-5c §6: WF-077, the condition-operand discipline — an if-then-else
         condition that reads an attribute directly (not through a `let … in`)
         is refused with the hoist instruction *)
      ("refuse_condition_read.catala_en", "WF-077");
      (* an if-then-else in a BARE rule expression (not under a let) stays
         refused (WF-069: value conditionals live only inside lambda, function
         or [Let] bodies) *)
      ("refuse_ifthenelse.catala_en", "if-then-else");
      ("refuse_boolfalse.catala_en", "false");
      (* row-10 fences (§4): list sorting, a plain-value topdef, and a
         scope-capturing fold body (the closed-body fence, WF-067) *)
      ("refuse_sort.catala_en", "Sort");
      ("refuse_topdef_value.catala_en", "plain-value");
      ("refuse_capture.catala_en", "WF-067") ]
  in
  List.iter
    (fun (src, needle) ->
      Printf.printf "[refuse] %s\n%!" src;
      let out = Filename.temp_file "catfe" ".ir.json" in
      let code, o = run [ plugin; src; out ] in
      check
        (src ^ " : refuses with reason")
        (code <> 0 && contains ~needle o)
        (Printf.sprintf "exit %d, wanted %S in:\n%s" code needle o))
    refusals;
  (* The serialiser's own refusals, which no Catala source reaches: the
     translator builds no Unresolved and fills none of the lists the document
     writes empty, so an E2E run never meets these arms. A loaded document
     drives them instead. A marker written without its warning fails WF-113
     (b) at the verifier, and a filled list written as [] drops what it held
     without a word, so each has to refuse at the serialiser. *)
  let m1_doc ?(parameters = "") ?(regels = "") ?(diagnostics = "") () =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": { "objectTypes": [], "factTypes": [], "parameters": [%s], "domains": [],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": { "rules": [%s], "dayKindDefinitions": [], "objectCreations": [],
    "allocations": [], "consistencies": [], "uniquenesses": [],
    "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [%s] }|}
      parameters regels diagnostics
  in
  (* one rule version with an attribute target, whose value and condition
     each test fills: the wire carries no rule without a kind, a target or a
     value since the L4 landing *)
  let regel ~waarde ~voorwaarde =
    Printf.sprintf
      {|{ "node": "Rule", "name": "r", "versions": [ {
  "validity": { "node": "Validity" },
  "target": { "node": "AttributeReference", "attribute": "a", "subjectType": "T", "source": { "snippet": "de a" } },
  "value": %s,%s "initialisation": false, "presentation": "calculation" } ] }|}
      waarde
      (match voorwaarde with None -> "" | Some c -> Printf.sprintf {| "condition": %s,|} c)
  in
  let getal =
    {|{ "node": "Literal", "value": { "number": "1" },
  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "1" } }|}
  in
  let emit_refusals =
    [ ("an expression Unresolved",
       m1_doc
         ~regels:
           (regel ~voorwaarde:None
              ~waarde:{|{ "node": "Unresolved", "source": { "snippet": "de onbekende waarde" } }|})
         (),
       "Unresolved");
      ("a condition Unresolved",
       m1_doc
         ~regels:
           (regel ~waarde:getal
              ~voorwaarde:(Some {|{ "node": "Unresolved", "source": { "snippet": "de onbekende voorwaarde" } }|}))
         (),
       "Unresolved");
      ("a filled diagnostics list",
       m1_doc
         ~diagnostics:
           {|{ "node": "Diagnostic", "severity": "warning", "message": "een melding", "line": 1 }|}
         (),
       "fills diagnostics");
      ("a filled parameters list",
       m1_doc
         ~parameters:
           {|{ "node": "Parameter", "name": "drempel",
  "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } }|}
         (),
       "fills parameters");
      (* the three forms the translator never builds and no reader would take
         from this plugin: a parameter reference resolves against no declaration
         (the plugin writes no parameters, WF-034), a variable read has no
         executor that carries it, and a whole-period qualifier has no
         time-dependent operand to qualify (WF-043) *)
      ("a ParameterReference",
       m1_doc
         ~regels:
           (regel ~voorwaarde:None
              ~waarde:{|{ "node": "ParameterReference", "parameter": "drempel", "source": { "snippet": "de drempel" } }|})
         (),
       "node not produced by the skeleton: ParameterReference");
      ("a VariableReference",
       m1_doc
         ~regels:
           (regel ~voorwaarde:None
              ~waarde:{|{ "node": "VariableReference", "name": "drempel", "source": { "snippet": "de drempel" } }|})
         (),
       "node not produced by the skeleton: VariableReference");
      ("a wholePeriod qualifier",
       m1_doc
         ~regels:
           (regel ~waarde:getal
              ~voorwaarde:
                (Some
                   {|{ "node": "Comparison", "op": "greater",
  "left": { "node": "AttributeReference", "attribute": "a", "subjectType": "T", "source": { "snippet": "de a" } },
  "right": { "node": "Literal", "value": { "number": "0" },
    "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "0" } },
  "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": false },
  "source": { "snippet": "de a groter dan 0 gedurende het hele jaar" } }|}))
         (),
       "condition shape not produced by the skeleton: a wholePeriod qualifier");
      ("a wholePeriod qualifier on a characteristic check",
       m1_doc
         ~regels:
           (regel ~waarde:getal
              ~voorwaarde:
                (Some
                   {|{ "node": "CharacteristicCheck", "characteristic": "k", "negated": false,
  "wholePeriod": { "node": "WholePeriod", "granularity": "month", "negated": true },
  "source": { "snippet": "niet de hele maand k" } }|}))
         (),
       "condition shape not produced by the skeleton: a wholePeriod qualifier") ]
  in
  List.iter
    (fun (what, text, needle) ->
      Printf.printf "[emit-refuse] %s\n%!" what;
      let name = what ^ " : the serialiser refuses naming " ^ needle in
      (* the verifier's stance loads every form: an executor refuses a
         VariableReference at load (WF-034), and the serialiser's own refusal
         is the subject here *)
      match
        Catala_frontend_lib.Emit.to_string
          (Ir_v0.Loader.document ~reader:Ir_v0.Loader.Verifier (Yojson.Safe.from_string text))
      with
      | (_ : string) -> check name false "the serialiser wrote a document"
      | exception Failure m -> check name (contains ~needle m) m)
    emit_refusals;
  (* The serialiser's arms no Catala source reaches, driven by one hand-built
     document (the emitter-form proof of the L4 landing, widened at the L5
     landing): the two context nodes — the translator builds neither a
     ContextValue nor a BoundDay — the text and dateTime value variants, the
     rule kinds and the rule condition the translator never combines, and the
     L5 forms no Catala source writes: a characteristic check naming its host,
     a validity with both bounds, a role with its plural, the three
     characteristic kinds and an initialisation default. What the serialiser
     writes must load in every reader and be verifier-clean, exactly as an
     E2E emission is, and the loader must read each of those forms back into
     the value it was written from. *)
  let module A = Ir_v0.Ast in
  let num0 : A.datatype = Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = None } in
  let zaak : A.objecttype =
    { ot_naam = Some "Zaak"; ot_supertype = None;
      attributen =
        [ ("peildatum", Some A.Datum_in_dagen); ("dagdatum", Some A.Datum_in_dagen);
          ("bedrag", Some num0); ("omschrijving", Some A.Tekst); ("moment", Some A.Datum_tijd);
          ("toeslag", Some num0); ("tarief", Some num0) ];
      attribuut_meervoud = []; attribuut_dimensies = []; attribuut_granulariteit = [];
      attribuut_tijdlijn_aantal = []; attribuut_tijdlijn_naam = [];
      kenmerken = [ Some "spoedeisend"; Some "voorrang"; Some "hoofdzaak" ];
      kenmerk_soorten = [ ("spoedeisend", Some A.Bijvoeglijk); ("voorrang", Some A.Bezittelijk) ];
      kenmerk_granulariteit = []; kenmerk_tijdlijn_aantal = [];
      bezield = false; meervoud = Some "Zaken" }
  in
  let aref a : A.expr =
    Attribuut_ref { attribuut = Some a; objecttype = Some "Zaak"; bron = Some a; navigatie_bindingen = None }
  in
  let lit v t : A.expr =
    Literal { variant = v; typ = Some t; bron = Some "proef" }
  in
  let regel naam soort doel waarde cond : A.resolved_regel =
    { rr_naam = Some naam; rr_soort = Some soort; doel = Some (aref doel); waarde = Some waarde;
      rr_voorwaarde = cond; rr_geldigheid = None; rr_bron_annotaties = None; rr_pointer = "" }
  in
  let groter a n : A.conditie =
    Vergelijking
      { op = Groter; links = Some (aref a); rechts = Some (lit (Lv_number n) num0); periode = None;
        bron = Some "groter" }
  in
  (* the subject's own type as the host: the reading an absent host has *)
  let spoedeisend : A.conditie =
    Kenmerk_check
      { kenmerk = Some "spoedeisend"; negatie = false; eigenaar = Some "Zaak"; periode = None;
        bron = Some "zij is spoedeisend"; navigatie_binding = None }
  in
  let jaar2026 : A.geldigheid = { geld_vanaf = Some "2026-01-01"; geld_tot = Some "2027-01-01" } in
  let koppeling : A.feittype =
    { ft_naam = Some "Zaakkoppeling"; wederkerig = false;
      rollen =
        [ { rol_naam = Some "moederzaak"; rol_objecttype_naam = Some "Zaak"; rol_meervoud = None;
            rol_meervoudig = false };
          { rol_naam = Some "deelzaak"; rol_objecttype_naam = Some "Zaak";
            rol_meervoud = Some "deelzaken"; rol_meervoudig = true } ] }
  in
  let tarief : A.prioritized_defaults =
    { pd_naam = Some "tarief van een zaak"; pd_onderwerp = "Zaak"; pd_kenmerk = None;
      pd_attribuut = Some "tarief";
      defaults =
        [ { default_besluit = None; default_voorwaarde = Some (groter "bedrag" "100");
            default_consequentie = Some (lit (Lv_number "7") num0); default_initialisatie = false;
            default_excepties = None; default_bron = Some "zeven bij een groot bedrag" };
          { default_besluit = None; default_voorwaarde = None;
            default_consequentie = Some (lit (Lv_number "0") num0); default_initialisatie = true;
            default_excepties = None; default_bron = Some "anders nul, tenzij al gevuld" } ];
      pd_ongeordend = false; pd_bron = Some "het tarief";
      pd_geldigheid = Some { geld_vanaf = None; geld_tot = None } }
  in
  let proef : A.document =
    { objecttypen = [ ("Zaak", zaak) ]; domeinen = []; parameters = []; eenheidstelsels = [];
      feittypen = [ ("Zaakkoppeling", koppeling) ]; dimensies = []; tijdlijnen = []; dagsoorten = [];
      resolved_regels =
        [ regel "peildatum van een zaak" Gelijkstelling "peildatum"
            (Constante
               { naam = Some Ir_v0.Spelling.evaluation_date; typ = Some A.Datum_in_dagen;
                 bron = Some "de rekendatum" })
            None;
          regel "dagdatum van een zaak" Berekening "dagdatum"
            (Bound_day { typ = Some A.Datum_in_dagen; bron = Some "de dag" }) None;
          regel "bedrag van een zaak" Initialisatie "bedrag" (lit (Lv_number "5") num0) None;
          regel "omschrijving van een zaak" Berekening "omschrijving" (lit (Lv_text "proef") A.Tekst)
            (Some (groter "bedrag" "0"));
          regel "moment van een zaak" Gelijkstelling "moment"
            (lit (Lv_date_time "2026-03-15T12:00:00") A.Datum_tijd) None;
          { (regel "toeslag van een zaak" Berekening "toeslag" (lit (Lv_number "3") num0)
               (Some spoedeisend))
            with rr_geldigheid = Some jaar2026 } ];
      resolved_dagsoorten = []; object_creaties = []; verdelingen = []; consistenties = [];
      uniciteiten = []; uniciteit_unies = []; feit_creaties = []; resolved_startpunten = [];
      regel_groepen = []; diagnostics = []; prioritized_defaults = [ tarief ]; function_defs = [];
      presentatie_annotaties = [] }
  in
  Printf.printf "[emit-proef] de vormen die geen Catala-bron bereikt\n%!";
  let text = Catala_frontend_lib.Emit.to_string proef in
  let out = Filename.temp_file "catfe-proef" ".ir.json" in
  let oc = open_out out in
  output_string oc text;
  close_out oc;
  let vc, vo = run [ verifier; out ] in
  check "emit-proef : verifier-clean" (vc = 0 && contains ~needle:"PASS" vo) vo;
  (match Ir_v0.Loader.document (Yojson.Safe.from_string text) with
   | back ->
       check "emit-proef : loads for an executor" true "";
       let toeslag =
         List.find (fun (r : A.resolved_regel) -> r.rr_naam = Some "toeslag van een zaak")
           back.resolved_regels
       in
       check "emit-proef : reads the host back" (toeslag.rr_voorwaarde = Some spoedeisend) text;
       check "emit-proef : reads both validity bounds back" (toeslag.rr_geldigheid = Some jaar2026) text;
       check "emit-proef : reads the role plural back"
         ((List.assoc "Zaakkoppeling" back.feittypen).rollen = koppeling.rollen) text;
       check "emit-proef : reads the initialisation default back"
         (List.map (fun (d : A.default) -> (d.default_initialisatie, d.default_voorwaarde = None))
            (List.hd back.prioritized_defaults).defaults
          = [ (false, false); (true, true) ])
         text
   | exception Ir_v0.Loader.Error (p, m) -> check "emit-proef : loads for an executor" false (p ^ ": " ^ m));
  List.iter
    (fun needle -> check ("emit-proef : writes " ^ needle) (contains ~needle text) text)
    [ {|"node": "ContextValue"|}; {|"name": "evaluationDate"|}; {|"node": "BoundDay"|};
      {|"text": "proef"|}; {|"dateTime": "2026-03-15T12:00:00"|}; {|"presentation": "equation"|};
      {|"initialisation": true|}; {|"condition": {|}; {|"host": "Zaak"|};
      {|"from": "2026-01-01"|}; {|"to": "2027-01-01"|}; {|"plural": "deelzaken"|};
      {|"kind": "adjectival"|}; {|"kind": "possessive"|}; {|"kind": "nominal"|} ];
  Printf.printf "\n%d checks, %d failures\n%!" !ntests !failures;
  if !failures > 0 then exit 1
