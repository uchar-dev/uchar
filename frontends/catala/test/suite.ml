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
         [Rung.excepties] sub-group. The binding spurious-conflict test of
         PHASE4-DEFAULTS §7 pinned frontend-side: only-one-sub-member (n=100)
         decides that member ($20 — flattening would fabricate a $20-vs-$10
         conflict), the form-a own consequence fires when the sub-group is
         empty (n=50 -> $10), the base carries the fall-through (n=5 -> $40),
         and two differing sub-members (n=200) propagate to the root — no
         write, one path-keyed §9.5 fact "Nestgroep.belasting/0" — through ref
         AND clingo *)
      ("nested_group.catala_en",
       [ "cases/nested_100.json"; "cases/nested_50.json";
         "cases/nested_5.json"; "cases/nested_200.json" ]);
      (* row-5c §6: the tutorial's rule-level let-if over money — nested strict
         Let nodes whose body is an IfThenElse (WF-069 widens into Let bodies)
         with a binder-only condition (WF-077). Both branches through the full
         differential *)
      ("let_if.catala_en", [ "cases/letif_60.json"; "cases/letif_0.json" ]);
      (* row-5c §5: Catala decimals at the pinned scale S = 4 — a percentage
         literal, the rational operators (Add/Sub/Mult/Div_rat_rat, unary
         Minus_rat) and the ToRat cast, plus a money × rate product that Catala
         rounds to the cent (Afgerond{REKENKUNDIG,2}) on tutorial magnitude,
         which clingo encodes SPLIT so the scale-6 product never wraps int31 *)
      ("decimals.catala_en", [ "cases/dec_drie.json"; "cases/dec_vijf.json" ]) ]
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
  (* refusal programs: source -> expected reason fragment. Besluit 4's four
     forms plus the empty-operand and literal-false refusals; the subscope
     fence carries the boundary proof that the remaining tutorial construct
     (the subscope call) still falls outside the fragment — decimals, let-in
     and branching exceptions now land (row 5c). *)
  let refusals =
    [ ("refuse_context.catala_en", "context");
      ("refuse_subscope.catala_en", "subscope");
      (* a shared non-trivial base under all members of a root-level unordered
         group stays refused (PHASE4-DEFAULTS §7; nested groups carry
         below-root branching, not this) *)
      ("refuse_sharedbase.catala_en", "shared non-trivial base");
      ("refuse_bool_value.catala_en", "value position");
      ("refuse_empty_arith.catala_en", "operand");
      (* row-36 fences: a partial match (WF-063 totality) and a tuple payload
         (an n-ary constructor payload — single-payload constructors only)
         each refuse loudly *)
      ("refuse_partialmatch.catala_en", "total");
      ("refuse_tuple.catala_en", "tuple");
      (* row-5c §5: a decimal literal that does not terminate at the pinned
         scale S = 4 is refused loudly at the source (the leeg-barrier for
         precision) *)
      ("refuse_nonterminating.catala_en", "terminate");
      (* row-5c §6: WF-077, the condition-operand discipline — an if-then-else
         condition that reads an attribute directly (not through a strict let)
         is refused with the hoist instruction *)
      ("refuse_condition_read.catala_en", "WF-077");
      (* an if-then-else in a BARE rule expression (not under a let) stays
         refused (WF-069: value conditionals live only inside lambda, function
         or strict Let bodies) *)
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
  Printf.printf "\n%d checks, %d failures\n%!" !ntests !failures;
  if !failures > 0 then exit 1
