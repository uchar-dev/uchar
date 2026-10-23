(* End-to-end suite for the F-logic frontend. Driven by dune (runtest):
   the four executable paths arrive as argv — the frontend, the verifier,
   the reference runner, the clingo runner.

   For each passing source: emit the IR, assert verifier-clean, replay
   every case on ref AND clingo. For each refusal source: assert the
   frontend exits non-zero, names its reason, and cites the mandate
   document that fixed the slice — the citation is a card obligation, so
   it is checked mechanically rather than trusted. No silent skips: an
   empty program list is a failure. *)

let read_all ic =
  let buf = Buffer.create 4096 in
  let chunk = Bytes.create 4096 in
  let rec loop () =
    let n = input ic chunk 0 4096 in
    if n > 0 then (Buffer.add_subbytes buf chunk 0 n; loop ())
  in
  loop ();
  Buffer.contents buf

let run argv =
  let cmd = String.concat " " (List.map Filename.quote argv) ^ " 2>&1" in
  let ic = Unix.open_process_in cmd in
  let out = read_all ic in
  let code =
    match Unix.close_process_in ic with
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

let mandate = "spec/proposals/SUBTYPING.md"

let () =
  let frontend, verifier, refx, clingo =
    match Sys.argv with
    | [| _; a; b; c; d |] -> (a, b, c, d)
    | _ ->
        prerr_endline "usage: suite <frontend> <verifier> <ref> <clingo>";
        exit 2
  in
  let passing =
    [ ("inheritance.flogic",
       [ "cases/inheritance_unhurt.json"; "cases/inheritance_wounded.json" ]);
      ("aggregation.flogic",
       [ "cases/aggregation_two.json"; "cases/aggregation_empty.json" ]);
      (* the five quantifiers of the fold, as counts: the empty case is the
         vacuum cell (ALLE and GEEN both hold), the mixed one separates GEEN
         from not-ALLE, and two cells are decided by the filter alone *)
      ("quantifier_fold.flogic",
       [ "cases/quantifier_fold_two.json"; "cases/quantifier_fold_mixed.json";
         "cases/quantifier_fold_empty.json"; "cases/quantifier_fold_filter_decides.json" ]) ]
  in
  List.iter
    (fun (src, cases) ->
      Printf.printf "[passing] %s\n%!" src;
      let out = Filename.temp_file "flogic" ".ir.json" in
      let code, o = run [ frontend; src; out ] in
      check (src ^ " : emit") (code = 0) (Printf.sprintf "exit %d\n%s" code o);
      if code = 0 then begin
        let vc, vo = run [ verifier; out ] in
        check (src ^ " : verifier-clean") (vc = 0 && contains ~needle:"PASS" vo) vo;
        List.iter
          (fun cf ->
            let rc, ro = run [ refx; out; cf ] in
            check (cf ^ " : ref") (rc = 0) ro;
            let gc, go = run [ clingo; out; cf ] in
            check (cf ^ " : clingo") (gc = 0) go)
          cases
      end)
    passing;
  if passing = [] then (incr failures; print_endline "  FAIL no passing programs");

  (* One fixture per refusal on the F-logic list of the subtyping design
     (§9.3), plus the slice edges this frontend adds. *)
  let refusals =
    [ (* the sharp witness of WF-106: two :: facts, which no wire document
         and no other frontend source form can even write *)
      ("refuse_second_parent.flogic", "WF-106");
      ("refuse_second_parent.flogic", "RechtOpUitkeringWazo and RechtUitkeringZw,");
      (* the needle carries the pair AND its parents: a bare
         "RechtUitkeringZw" is a prefix of "RechtUitkeringZwWazo" and would
         pass on the parent name alone *)
      ("refuse_second_parent.flogic", "RechtOpUitkering and RechtUitkeringZwWazo");
      ("refuse_unbarred.flogic", "unbarred class-level frame");
      ("refuse_hilog.flogic", "HiLog and meta-programming");
      ("refuse_defeasible.flogic", "\\overrides");
      ("refuse_naf.flogic", "\\naf");
      ("refuse_neg.flogic", "\\neg");
      ("refuse_monotonic.flogic", "setsemantics");
      ("refuse_instance.flogic", "uchar-case-v0");
      ("refuse_predicate.flogic", "no predicate channel");
      ("refuse_flora1.flogic", "*->");
      ("refuse_decimal.flogic", "\\decimal");
      ("refuse_second_base.flogic", "two unconditional definitions");
      ("refuse_aggregate.flogic", "sum and count only");
      ("refuse_role_signature.flogic", "declare a fact type");
      (* a rule reaches the wire by name: WF-020 filters nulls out and never
         looks at a defaults group's name, so both of these have to refuse
         at the frontend or not at all *)
      ("refuse_unnamed.flogic", "a rule with no id");
      ("refuse_unnamed_base.flogic", "has no id");
      ("refuse_id_without_definition.flogic", "which is a subclass fact");
      ("refuse_duplicate_name.flogic", "is already used at line");
      ("refuse_argumentation_theory.flogic", "argumentation-theory");
      ("refuse_duplicate_member.flogic", "declares the member hoogte twice");
      ("refuse_unknown_filler.flogic", "not a declared object type");
      (* the quantifier fold's own edges: the threshold the source wrote is
         the threshold on the wire, the count stands on the left, and a
         conditioned comprehension is a quantification and not a value *)
      ("refuse_count_threshold_strict.flogic", "'>= 2'");
      ("refuse_count_threshold_zero.flogic", "WF-041");
      ("refuse_count_not_equal.flogic", "no direct spelling to rewrite this into");
      (* the hop is the rule's subject here exactly as it is in a value
         position — same rule, same message *)
      ("refuse_count_foreign_hop.flogic", "does not start at the rule's subject ?S");
      ("refuse_count_right.flogic", "on the LEFT");
      ("refuse_count_threshold_no_number.flogic", "a whole, non-negative number");
      ("refuse_all_twin.flogic", "the spelling of the quantifier all repeats the comprehension");
      ("refuse_filtered_count_in_value.flogic", "is a QUANTIFICATION");
      ("refuse_filtered_sum.flogic", "sum{...} carries no subselection");
      ("refuse_count_second_hop.flogic", "ONE role hop");
      ("refuse_count_foreign_variable.flogic", "speaks of ?S");
      ("refuse_characteristic_head_unknown.flogic", "declares no characteristic zeewaardig");
      ("refuse_characteristic_second_definition.flogic", "?X : sterk is defined twice") ]
  in
  List.iter
    (fun (src, needle) ->
      Printf.printf "[refuse] %s (%s)\n%!" src needle;
      let out = Filename.temp_file "flogic" ".ir.json" in
      let code, o = run [ frontend; src; out ] in
      check
        (src ^ " : refuses naming " ^ needle)
        (code <> 0 && contains ~needle o)
        (Printf.sprintf "exit %d, wanted %S in:\n%s" code needle o);
      check (src ^ " : cites the mandate") (contains ~needle:mandate o) o;
      (* a refusal writes no document: a half-lowered IR would be worse
         than none *)
      check (src ^ " : leaves no output")
        (not (contains ~needle:"uchar-ir-v1" (try
                let ic = open_in out in
                let s = read_all ic in
                close_in ic; s
              with Sys_error _ -> "")))
        "the frontend wrote an IR document for a refused source")
    refusals;
  (* The serialiser's own refusals, which no F-logic source reaches: the
     lowering builds no Unresolved and fills none of the lists the document
     writes empty, so an E2E run never meets these arms. A loaded document
     drives them instead. A marker written without its WARNING fails WF-113
     (b) at the verifier, and a filled list written as [] drops what it held
     without a word, so each has to refuse at the serialiser. *)
  let m1_doc ?(parameters = "") ?(domains = "") ?(rules = "") ?(diagnostics = "") () =
    Printf.sprintf
      {|{ "format": "uchar-ir-v1",
  "model": { "objectTypes": [], "factTypes": [], "parameters": [%s], "domains": [%s],
    "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": [] },
  "rules": { "rules": [%s], "dayKindDefinitions": [], "objectCreations": [],
    "allocations": [], "consistencies": [], "uniquenesses": [],
    "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
  "diagnostics": [%s] }|}
      parameters domains rules diagnostics
  in
  (* one rule version with an attribute target, whose value and condition
     each test fills: the wire carries no rule without a kind, a target or a
     value since the L4 landing *)
  let rule_def ~value_node ~precondition =
    Printf.sprintf
      {|{ "node": "Rule", "name": "r", "versions": [ {
  "validity": { "node": "Validity" },
  "target": { "node": "AttributeReference", "attribute": "a", "subjectType": "T", "source": { "snippet": "de a" } },
  "value": %s,%s "initialisation": false, "presentation": "calculation" } ] }|}
      value_node
      (match precondition with None -> "" | Some c -> Printf.sprintf {| "condition": %s,|} c)
  in
  let number =
    {|{ "node": "Literal", "value": { "number": "1" },
  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "1" } }|}
  in
  let emit_refusals =
    [ ("an expression Unresolved",
       m1_doc
         ~rules:
           (rule_def ~precondition:None
              ~value_node:{|{ "node": "Unresolved", "source": { "snippet": "de onbekende waarde" } }|})
         (),
       "expression node not produced by the F-logic slice");
      ("a condition Unresolved",
       m1_doc
         ~rules:
           (rule_def ~value_node:number
              ~precondition:(Some {|{ "node": "Unresolved", "source": { "snippet": "de onbekende voorwaarde" } }|}))
         (),
       "condition shape not produced by the F-logic slice");
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
      ("a filled domains list",
       m1_doc
         ~domains:
           {|{ "node": "Domain", "name": "Getijstand",
  "datatype": { "node": "Enumeration", "values": [ "Eb", "Vloed" ] } }|}
         (),
       "fills domains") ]
  in
  List.iter
    (fun (what, text, needle) ->
      Printf.printf "[emit-refuse] %s\n%!" what;
      let name = what ^ " : the serialiser refuses naming " ^ needle in
      match Flogic_frontend_lib.Emit.to_string (Ir_v0.Loader.document (Yojson.Safe.from_string text)) with
      | (_ : string) -> check name false "the serialiser wrote a document"
      | exception Failure m -> check name (contains ~needle m) m)
    emit_refusals;
  (* The serialiser's arms no F-logic source reaches, driven by one hand-built
     document (the emitter-form proof of the L5 landing): a characteristic
     check naming its host, a validity with both bounds, a role with its
     plural, a characteristic defaults group whose default carries a
     decision, the Text and Date datatypes and the presentation equation.
     What the serialiser writes must load in every reader and be
     verifier-clean, exactly as an E2E emission is, and the loader must read
     each of those forms back into the value it was written from. *)
  let module A = Ir_v0.Ast in
  let num0 : A.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = Some Unspecified; measure_unit = None }
  in
  let sein : A.object_type =
    { ot_ident = Some "Sein"; ot_supertype = None;
      attributes =
        [ ("kleur", Some A.Text_type); ("hijsdatum", Some A.Date_in_days); ("hoogte", Some num0);
          ("breedte", Some num0) ];
      attribute_plural = []; attribute_dimensions = []; attribute_granularity = [];
      attribute_timeline_count = []; attribute_timeline_names = [];
      characteristics = [ Some "gehesen"; Some "zichtbaar" ];
      characteristic_kinds = [ ("gehesen", Some A.Adjectival); ("zichtbaar", Some A.Adjectival) ];
      characteristic_granularity = []; characteristic_timeline_count = []; animate = false; plural = None }
  in
  let aref a : A.expr =
    Attribute_reference { attribute_ref = Some a; object_type = Some "Sein"; source_name = Some a; navigation_bindings = None; anchor = None }
  in
  let number n : A.expr = Literal { variant = Lv_number n; typ = Some num0; source_name = Some n } in
  (* the subject's own type as the host: the reading an absent host has *)
  let gehesen : A.condition_expr =
    Characteristic_check
      { characteristic = Some "gehesen"; negation = false; holder = Some "Sein"; time_period = None;
        source_name = Some "?S : gehesen"; navigation_binding = None; anchor = None }
  in
  let seizoen : A.validity_window = { valid_from = Some "2026-04-01"; valid_to = Some "2026-10-01" } in
  let sein_pair : A.fact_type_name =
    { ft_name = Some "seinpaar"; reciprocal = false;
      role_names =
        [ { role_identifier = Some "hoofdsein"; role_object_type_name = Some "Sein"; role_plural = None;
            role_many = false };
          { role_identifier = Some "bijsein"; role_object_type_name = Some "Sein";
            role_plural = Some "bijseinen"; role_many = true } ] }
  in
  let visible : A.prioritized_defaults =
    { pd_name = Some "zichtbaar sein"; pd_subject = "Sein"; pd_characteristic = Some "zichtbaar";
      pd_attribute = None;
      defaults =
        [ { default_decision = Some true;
            default_justification =
              Some
                (Comparison
                   { op = Greater; left_side = Some (aref "hoogte"); right_side = Some (number "10");
                     time_period = None; source_name = Some "?S.hoogte > 10" });
            default_consequence = None; default_initialisation = false; default_exceptions = None;
            default_source = None } ];
      pd_unordered = false; pd_source = None; pd_validity = Some { valid_from = None; valid_to = None } }
  in
  let rule_def ident variety objective value_node cond validity_window : A.resolved_rule =
    { rr_name = Some ident; rr_presentation = Some variety; objective = Some (aref objective); value_node = Some value_node;
      rr_condition = cond; rr_validity = validity_window; rr_source_annotations = None; rr_pointer = "" }
  in
  let trial : A.document =
    { object_types = [ ("Sein", sein) ]; domain_specs = []; parameters = []; unit_systems = [];
      fact_types = [ ("seinpaar", sein_pair) ]; dimensions = []; timelines = []; day_kinds = [];
      resolved_rules =
        [ rule_def "hoogte van een sein" Equation "hoogte" (number "12") None None;
          rule_def "breedte van een sein" Calculation "breedte" (number "3") (Some gehesen) (Some seizoen) ];
      resolved_day_kind_definitions = []; object_creations = []; allocations = []; consistencies = [];
      uniquenesses = []; uniqueness_unions = []; fact_creations = []; resolved_start_point_determinations = [];
      rule_groups = []; diagnostics = []; prioritized_defaults = [ visible ]; function_defs = [];
      presentation_annotations = [] }
  in
  Printf.printf "[emit-trial] the forms that no F-logic source reaches\n%!";
  let text = Flogic_frontend_lib.Emit.to_string trial in
  let out = Filename.temp_file "flogic-proef" ".ir.json" in
  let oc = open_out out in
  output_string oc text;
  close_out oc;
  let vc, vo = run [ verifier; out ] in
  check "emit-trial : verifier-clean" (vc = 0 && contains ~needle:"PASS" vo) vo;
  (match Ir_v0.Loader.document (Yojson.Safe.from_string text) with
   | back ->
       check "emit-trial : loads for an executor" true "";
       let breedte =
         List.find (fun (r : A.resolved_rule) -> r.rr_name = Some "breedte van een sein")
           back.resolved_rules
       in
       check "emit-trial : reads the host back" (breedte.rr_condition = Some gehesen) text;
       check "emit-trial : reads both validity bounds back" (breedte.rr_validity = Some seizoen) text;
       check "emit-trial : reads the role plural back"
         ((List.assoc "seinpaar" back.fact_types).role_names = sein_pair.role_names) text;
       check "emit-trial : reads the characteristic group and its decision back"
         (match back.prioritized_defaults with
          | [ pd ] ->
              pd.pd_characteristic = Some "zichtbaar" && pd.pd_attribute = None
              && List.map (fun (d : A.default) -> d.default_decision) pd.defaults = [ Some true ]
          | _ -> false)
         text
   | exception Ir_v0.Loader.Error (p, m) -> check "emit-trial : loads for an executor" false (p ^ ": " ^ m));
  List.iter
    (fun needle -> check ("emit-trial : writes " ^ needle) (contains ~needle text) text)
    [ {|"host": "Sein"|}; {|"from": "2026-04-01"|}; {|"to": "2026-10-01"|};
      {|"plural": "bijseinen"|}; {|"characteristic": "zichtbaar"|}; {|"decision": true|};
      {|"node": "Text"|}; {|"precision": "day"|}; {|"presentation": "equation"|} ];
  Printf.printf "\n%d checks, %d failures\n%!" !ntests !failures;
  if !failures > 0 then exit 1
