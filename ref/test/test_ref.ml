(* The worked examples of RUNTIME-SEMANTICS ch. 1–2, verbatim, plus the
   ch. 4.6 allocation examples against the pure recurrence. Test names
   carry the WV numbering of the chapters (WV-<section>-<label>).

   Not covered here, deliberately: WV-1.2f (timeline rules — ch. 3, next
   card), WV-2.3 (static result-precision typing — a static property of
   the emitter/verifier, not of evaluation; its one example with a value
   side, the product that is never whole, is WV-2.3-maal-niet-heel), and
   the ch. 5 examples that
   need full IR documents — the corpus cases exercise those (the
   penningdrempel case is the 5.7 trace verbatim). *)

open Uchar_ref

let failures = ref 0
let total = ref 0

(* The last worked example that REPORTED. A fault raised while computing the
   next example's value escapes before its own [check] ever runs, so the binary
   used to die on a bare "Fatal error: exception Value.Fault" with no clue which
   example was in flight — measured on the mutant harness, which killed two
   mutants with zero report lines. The handler below turns that into a named
   report. Nothing here changes an assertion or its verdict: [last_item] is a
   write-only breadcrumb and the handler runs only on the way out. *)
let last_item = ref "<no example reported yet>"

let check name ok =
  incr total;
  last_item := name;
  if not ok then begin
    incr failures;
    Printf.printf "FAIL %s\n" name
  end

let () =
  Printexc.set_uncaught_exception_handler (fun e bt ->
      (match e with
      | Value.Fault { text = m; _ } ->
          Printf.printf
            "FAIL <uncaught fault> after the last reported example %S\n  fault: %s\n"
            !last_item m
      | e ->
          Printf.printf "FAIL <uncaught exception> after the last reported example %S\n  %s\n"
            !last_item (Printexc.to_string e));
      Printexc.print_raw_backtrace stdout bt;
      flush stdout)

let q = Q.of_string
let n s = Value.Num (Value.q_of_lexeme s, Value.Unitless)
let empty_value = Value.Empty

let expect_num name v s =
  match v with
  | Value.Num (got, _) -> check name (Q.equal got (Value.q_of_lexeme s))
  | other -> check (name ^ " (got " ^ Value.to_string other ^ ")") false

let expect_empty name v = check name (v = Value.Empty)

let no_err = fun (_ : Value.deposit) -> ()

let with_errs f =
  let errs = ref [] in
  let r = f (fun m -> errs := m :: !errs) in
  (r, List.length !errs)

let expect_fault name f =
  match f () with
  | exception Value.Fault { text = _; _ } -> check name true
  | _ -> check name false

(* A fault is only a witness if it says WHAT it refuses. [expect_fault]
   pins that something went wrong; this pins which sentence came out, so a
   refusal whose text drifts into another meaning fails here instead of
   passing silently. Used wherever the message is a shared contract — a
   refusal the other executors must speak with one voice. *)
let substring hay needle =
  let nh = String.length hay and nn = String.length needle in
  nn = 0
  ||
  let rec go i = i + nn <= nh && (String.sub hay i nn = needle || go (i + 1)) in
  go 0

let expect_fault_msg name f frag =
  match f () with
  | exception Value.Fault { text = m; _ } -> check (name ^ " (got: " ^ m ^ ")") (substring m frag)
  | _ -> check (name ^ " (no fault at all)") false

(* --- ch. 1.2a: arithmetic operators over empty ------------------------- *)

let () =
  expect_num "WV-1.2a-plus-1" (Arith.add (n "5") empty_value) "5";
  expect_num "WV-1.2a-plus-2" (Arith.add empty_value empty_value) "0";
  expect_num "WV-1.2a-min-1" (Arith.sub_min empty_value (n "5")) "-5";
  expect_num "WV-1.2a-min-2" (Arith.sub_min (n "5") empty_value) "5";
  expect_num "WV-1.2a-min-3" (Arith.sub_min empty_value empty_value) "0";
  expect_empty "WV-1.2a-vm-1" (Arith.sub_reduced empty_value (n "5"));
  expect_num "WV-1.2a-vm-2" (Arith.sub_reduced (n "5") empty_value) "5";
  expect_empty "WV-1.2a-vm-3" (Arith.sub_reduced empty_value empty_value);
  expect_num "WV-1.2a-maal-1" (Arith.mul (n "5") empty_value) "0";
  expect_num "WV-1.2a-maal-2" (Arith.mul empty_value empty_value) "0";
  expect_num "WV-1.2a-pct-1" (Arith.pct_of empty_value (n "600")) "0";
  expect_num "WV-1.2a-pct-2" (Arith.pct_of (n "10") empty_value) "0";
  expect_num "WV-1.2a-deel-1" (Arith.div_ ~err:no_err empty_value (n "5")) "0";
  expect_num "WV-1.2a-deel-2" (Arith.div_ ~err:no_err empty_value empty_value) "0";
  (let r, errs = with_errs (fun err -> Arith.div_ ~err (n "5") empty_value) in
   expect_empty "WV-1.2a-deel-3" r;
   check "WV-1.2a-deel-3 (§9.5 error)" (errs = 1));
  expect_empty "WV-1.2a-macht-1" (Arith.pow_ empty_value (n "2"));
  expect_empty "WV-1.2a-macht-2" (Arith.pow_ (n "4") empty_value);
  expect_empty "WV-1.2a-wortel-1" (Arith.sqrt_ ~err:no_err empty_value);
  (let r, errs = with_errs (fun err -> Arith.sqrt_ ~err (n "-4")) in
   expect_empty "WV-1.2a-wortel-2" r;
   check "WV-1.2a-wortel-2 (§9.5 error)" (errs = 1));
  expect_empty "WV-1.2a-abs-1" (Arith.abs_ empty_value);
  expect_empty "WV-1.2a-afr-1" (Arith.round_v Half_away_from_zero 2 empty_value);
  expect_empty "WV-1.2a-begr-1" (Arith.clamp ~min_:(n "0") ~max_:(n "100") empty_value);
  expect_num "WV-1.2a-begr-2" (Arith.clamp ~min_:empty_value ~max_:(n "100") (n "150")) "100";
  (* the subtraction pair, §6.3 *)
  expect_num "WV-1.2-sub-1" (Arith.sub_min empty_value (n "4")) "-4";
  expect_empty "WV-1.2-sub-2" (Arith.sub_reduced empty_value (n "4"))

(* --- ch. 1.2b: dates and durations over empty -------------------------- *)

let () =
  expect_empty "WV-1.2b-datum-1" (Arith.date_plus_duration empty_value (n "5"));
  (let d = Value.date_of_iso "2025-03-01" in
   check "WV-1.2b-datum-2" (Arith.date_plus_duration d empty_value = d));
  expect_empty "WV-1.2b-datum-3" (Arith.year_of empty_value);
  expect_empty "WV-1.2b-datum-4" (Arith.duration empty_value (Value.date_of_iso "2025-03-01"))

(* --- ch. 1.2c: text coercion of empty ---------------------------------- *)

let () =
  check "WV-1.2c-text-1"
    (Arith.concat [ Value.Text "a"; empty_value; Value.Text "b" ] = Value.Text "ab")

(* --- ch. 1.2d: comparisons (three-valued at the edges) ------------------ *)

let () =
  check "WV-1.2d-ord-1" (Arith.compare_op Less empty_value (n "5") = false);
  check "WV-1.2d-ord-2" (Arith.compare_op Less empty_value empty_value = false);
  check "WV-1.2d-eq-1" (Arith.compare_op Equal empty_value empty_value = true);
  check "WV-1.2d-eq-2" (Arith.compare_op Equal empty_value (n "5") = false);
  check "WV-1.2d-neq-1" (Arith.compare_op Not_equal empty_value (n "5") = true);
  check "WV-1.2d-neq-2" (Arith.compare_op Not_equal empty_value empty_value = false);
  check "WV-1.2d-neq-3" (Arith.compare_op Not_equal (n "5") (n "3") = true);
  (* non-numeric equality: two empties are NOT equal (§5 Tabel 38) *)
  check "WV-1.2d-teq-1" (Arith.eq_non_numeric empty_value empty_value = false);
  (* the date row (v0's Tabel-38 guard on the date shape): gelijk-aan
     over dates is onwaar on ANY empty operand — in particular
     both-empty, where the numeric rule above reads waar — while
     ongelijk-aan keeps the numeric cells (exactly one empty → waar,
     both empty → onwaar) *)
  (let d = Value.date_of_iso "2026-06-21" in
   let cmp op a b = Arith.compare_op ~static_numeric:true ~static_date:true op a b in
   check "WV-1.2d-date-eq-1" (cmp Equal empty_value empty_value = false);
   check "WV-1.2d-date-eq-2" (cmp Equal empty_value d = false);
   check "WV-1.2d-date-eq-3" (cmp Equal d d = true);
   check "WV-1.2d-date-neq-1" (cmp Not_equal empty_value d = true);
   check "WV-1.2d-date-neq-2" (cmp Not_equal empty_value empty_value = false));
  (* Tabel 44 (§5.6): inequality is uniform over EVERY datatype — exactly
     one empty operand → waar, both empty → onwaar. Text, enum and boolean
     each mirror the numeric one-empty cell (WV-1.2d-neq-1 above); equality
     stays any-empty → onwaar (§5 Tabel 38). static_numeric is false so a
     value-carrying pair takes the non-numeric route; two empties are
     typeless at runtime, so the shared both-empty cell is checked once. *)
  let nn op a b = Arith.compare_op ~static_numeric:false op a b in
  check "WV-1.2d-txt-neq-one-l" (nn Not_equal empty_value (Value.Text "zeevondst") = true);
  check "WV-1.2d-txt-neq-one-r" (nn Not_equal (Value.Text "zeevondst") empty_value = true);
  check "WV-1.2d-txt-neq-val" (nn Not_equal (Value.Text "a") (Value.Text "b") = true);
  check "WV-1.2d-txt-neq-eqval" (nn Not_equal (Value.Text "a") (Value.Text "a") = false);
  check "WV-1.2d-enum-neq-one-l" (nn Not_equal empty_value (Value.Enum "gaaf") = true);
  check "WV-1.2d-enum-neq-val" (nn Not_equal (Value.Enum "gaaf") (Value.Enum "wrakhout") = true);
  check "WV-1.2d-bool-neq-one-l" (nn Not_equal empty_value (Value.Bool true) = true);
  check "WV-1.2d-bool-neq-one-r" (nn Not_equal (Value.Bool true) empty_value = true);
  check "WV-1.2d-nn-neq-both" (nn Not_equal empty_value empty_value = false);
  (* equality is untouched: any empty operand → onwaar for the same operands *)
  check "WV-1.2d-txt-eq-one" (nn Equal empty_value (Value.Text "zeevondst") = false);
  check "WV-1.2d-enum-eq-one" (nn Equal empty_value (Value.Enum "gaaf") = false);
  check "WV-1.2d-bool-eq-one" (nn Equal empty_value (Value.Bool true) = false)

(* --- ch. 1.2e: aggregations (the §5.8.2/§4.20 split) -------------------- *)

let () =
  expect_num "WV-1.2e-som-1" (Arith.agg_sum ~fallback:None [ n "5"; empty_value; n "3" ]) "8";
  expect_empty "WV-1.2e-som-2" (Arith.agg_sum ~fallback:None [ empty_value; empty_value ]);
  expect_num "WV-1.2e-som-3" (Arith.agg_sum ~fallback:(Some (n "0")) [ empty_value; empty_value ]) "0";
  expect_num "WV-1.2e-totaal-1" (Arith.agg_total []) "0";
  expect_num "WV-1.2e-aantal-1" (Arith.agg_count []) "0";
  expect_num "WV-1.2e-min-1" (Arith.agg_min [ n "5"; empty_value; n "3" ]) "3";
  expect_empty "WV-1.2e-min-2" (Arith.agg_min [ empty_value; empty_value ]);
  (* eerste/laatste are ORDERING folds (ch. 1.2e): least/earliest and
     greatest/latest over the operand values, never a positional pick *)
  expect_num "WV-1.2e-eerste-1" (Arith.agg_first [ n "5"; empty_value; n "3" ]) "3";
  expect_num "WV-1.2e-laatste-1" (Arith.agg_last [ n "5"; empty_value; n "3" ]) "5";
  check "WV-1.2e-eerste-2"
    (Arith.agg_first [ Value.date_of_iso "2026-06-21"; Value.date_of_iso "2026-05-01" ]
    = Value.date_of_iso "2026-05-01");
  check "WV-1.2e-laatste-2"
    (Arith.agg_last [ Value.date_of_iso "2026-06-21"; empty_value ]
    = Value.date_of_iso "2026-06-21");
  expect_empty "WV-1.2e-eerste-3" (Arith.agg_first [ empty_value; empty_value ]);
  (* dimension folds (§5.8.5): empty cells skipped; all-empty = empty *)
  expect_num "WV-1.2e-dim-1" (Arith.agg_sum ~fallback:None [ n "3"; empty_value; n "2" ]) "5";
  expect_empty "WV-1.2e-dim-2" (Arith.agg_sum ~fallback:None [ empty_value ])

(* --- ch. 1.2g: assignment (via the write boundary) ---------------------- *)

let decl2 : Ir_v0.Ast.datatype option =
  Some (Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None })

let whole : Ir_v0.Ast.datatype option =
  Some (Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = None })

let mini_world v =
  World.add_instance World.empty
    {
      label = "x";
      otype = "T";
      attrs = (match v with None -> World.SMap.empty | Some v -> World.SMap.singleton "a" v);
      chars = [];
      left_side = World.SMap.empty;
      tls = World.SMap.empty; ctls = World.SMap.empty;
    }

let () =
  (* target holds 5, computed empty → the write happens, target becomes empty *)
  let w = mini_world (Some (n "5")) in
  let w', changed = World.write_attr w ~label:"x" ~attr:"a" ~decl:decl2 empty_value in
  check "WV-1.2g-assign-1" (changed && World.attr (World.get w' "x") "a" = Value.Empty);
  (* target empty, computed empty → no-op *)
  let w = mini_world None in
  let _, changed = World.write_attr w ~label:"x" ~attr:"a" ~decl:decl2 empty_value in
  check "WV-1.2g-assign-2" (not changed)

(* --- ch. 2.2: operators, exact ------------------------------------------ *)

let () =
  expect_num "WV-2.2-a" (Arith.mul (n "9") (n "1/3")) "3";
  expect_num "WV-2.2-b" (Arith.add (n "0.1") (n "0.2")) "0.3";
  expect_num "WV-2.2-c" (Arith.sub_reduced (n "5") (n "3")) "2";
  expect_num "WV-2.2-d" (Arith.div_ ~err:no_err (n "10") (n "4")) "5/2";
  expect_num "WV-2.2-e" (Arith.div_ ~err:no_err (n "2") (n "3")) "2/3";
  expect_num "WV-2.2-f" (Arith.div_abs ~err:no_err (n "10") (n "3")) "3.33333";
  expect_num "WV-2.2-g" (Arith.div_abs ~err:no_err (n "-10") (n "3")) "-3.33333";
  expect_num "WV-2.2-h" (Arith.pow_ (n "7") (n "19")) "11398895185373143";
  expect_num "WV-2.2-i" (Arith.pow_ (n "1/3") (n "-1")) "3";
  expect_num "WV-2.2-j" (Arith.pow_ (n "4") (n "0.5")) "2";
  expect_num "WV-2.2-k" (Arith.pct_of (n "1/3") (n "600")) "2";
  expect_num "WV-2.2-l" (Arith.pct_of (n "50") (n "4.00")) "2";
  expect_num "WV-2.2-m" (Arith.sqrt_ ~err:no_err (n "9")) "3";
  expect_num "WV-2.2-n" (Arith.sqrt_rounded ~err:no_err Half_away_from_zero 2 (n "2")) "1.41";
  expect_num "WV-2.2-o" (Arith.abs_ (n "-3/7")) "3/7";
  (* division by numeric zero: leeg plus one §9.5 deposit of the kind EF-005
     (ch. 1.4 point 4; the v0 mirror faults) *)
  (let codes = ref [] in
   let r = Arith.div_ ~err:(fun (d : Value.deposit) -> codes := d.code :: !codes) (n "5") (n "0") in
   expect_empty "ch2.2-div-zero" r;
   check "ch2.2-div-zero (§9.5 EF-005)" (!codes = [ Some "EF-005" ]));
  (* the dividend rule of ch. 1.2 comes first: 0 ÷ 0 has a filled dividend and
     deposits, leeg ÷ 0 is 0 without a deposit *)
  (let codes = ref [] in
   let r = Arith.div_ ~err:(fun (d : Value.deposit) -> codes := d.code :: !codes) (n "0") (n "0") in
   expect_empty "ch2.2-div-zero-by-zero" r;
   check "ch2.2-div-zero-by-zero (§9.5 EF-005)" (!codes = [ Some "EF-005" ]));
  (let r, deposits = with_errs (fun err -> Arith.div_ ~err empty_value (n "0")) in
   expect_num "ch2.2-div-empty-by-zero" r "0";
   check "ch2.2-div-empty-by-zero (no §9.5 deposit)" (deposits = 0))

(* --- ch. 2.3: the wholeness of a product ---------------------------------- *)

(* WV-2.3-maal-niet-heel: the ch. 2.3 example "3 × 4, two whole operands,
   types at 0 decimals and fractional although its value is 12". The rest of
   ch. 2.3 stays outside this suite (see the header); this one example has a
   reader on both sides. The VALUE is ref's own: the product evaluates to the
   exact 12. The TYPE is static, and ref reads it through the checks library
   it links, the verifier's WF-091 derivation: annotated whole, the product
   draws exactly the wholeness finding; annotated fractional, nothing. *)
let () =
  let doc entire =
    Printf.sprintf
      {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"uitkomst","datatype":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"uitkomst van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"uitkomst","subjectType":"Jutter","source":{"snippet":"De uitkomst van een Jutter"}},"value":{"node":"Binary","op":"times","left":{"node":"Literal","value":{"number":"3"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"3"}},"right":{"node":"Literal","value":{"number":"4"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"4"}},"type":{"node":"Number","whole":%b,"decimals":0,"sign":"unspecified"},"source":{"snippet":"3 maal 4"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
      entire
  in
  let case_data =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter", "attributes": {},
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "uitkomst",
      "value": { "number": "12" } } ] }|}
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv23" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" (doc false) and whole_f = write ".ir.json" (doc true) in
  let casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; whole_f; casef ])
    (fun () ->
      let d = Ir_v0.Loader.load_file irf in
      let c = Case.load_file d casef in
      let final =
        Eval.evaluate
          { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
            locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
          c.world
      in
      check "WV-2.3-maal-niet-heel-waarde-12" (Case.check c final = []);
      let findings f =
        List.map
          (fun (x : Ir_v0.Checks.violation) -> (x.wf, x.at, x.reason))
          (Ir_v0.Checks.all (Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier f))
      in
      check "WV-2.3-maal-niet-heel-fractioneel-schoon" (findings irf = []);
      check "WV-2.3-maal-niet-heel-heel-geweigerd"
        (findings whole_f
        = [ ( "WF-091", "/rules/rules/0/versions/0/value/type",
              "wholeness mismatch: the times composite is typed whole, but the type derived from \
               its operands is fractional" ) ]))

(* --- ch. 2.4: the assignment gate --------------------------------------- *)

let () =
  (* 1,5 into a whole-number attribute: error; rounded first: writes 2 *)
  let w = mini_world None in
  expect_fault "WV-2.4-a" (fun () -> World.write_attr w ~label:"x" ~attr:"a" ~decl:whole (n "1.5"));
  let w', _ = World.write_attr w ~label:"x" ~attr:"a" ~decl:whole (Arith.round_v Half_away_from_zero 0 (n "1.5")) in
  expect_num "WV-2.4-b" (World.attr (World.get w' "x") "a") "2";
  (* the rounding applies to the true rational *)
  expect_num "WV-2.4-c" (Arith.round_v Half_away_from_zero 2 (n "2/3")) "0.67";
  expect_num "WV-2.4-d" (Arith.round_v Floor 2 (n "2/3")) "0.66"

(* ch. 2.4 point 3: the unbounded form (IR-SPEC ch. 2.4 (c): whole false, 0
   decimals, a "getal" declared without decimals) has no ceiling, so the gate
   does not apply to it: the exact value lands, a non-terminating one
   included, where the same 3/2 into the whole form is the error of
   WV-2.4-a. The case input fence reads the same form: an input 1/6 on an
   unbounded attribute is carried exactly, and the rules compute on it. *)
let unbounded : Ir_v0.Ast.datatype option =
  Some (Number { whole_part = false; decimal_places = 0; sign_char = None; measure_unit = None })

let () =
  let w = mini_world None in
  let w', _ = World.write_attr w ~label:"x" ~attr:"a" ~decl:unbounded (n "3/2") in
  expect_num "WV-2.4-e" (World.attr (World.get w' "x") "a") "3/2";
  let w'', _ = World.write_attr w ~label:"x" ~attr:"a" ~decl:unbounded (n "1/3") in
  expect_num "WV-2.4-f" (World.attr (World.get w'' "x") "a") "1/3";
  let doc = {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Vondst","animate":false,"plural":"Vondsten","attributes":[{"node":"Attribute","name":"sleepafstand","datatype":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"dubbele sleepafstand","datatype":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"gedeelde sleepafstand","datatype":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"dubbele sleepafstand","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"dubbele sleepafstand","subjectType":"Vondst","source":{"snippet":"De dubbele sleepafstand van een Vondst"}},"value":{"node":"Binary","op":"times","left":{"node":"AttributeReference","attribute":"sleepafstand","subjectType":"Vondst","source":{"snippet":"zijn sleepafstand"}},"right":{"node":"Literal","value":{"number":"2"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"2"}},"type":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"},"source":{"snippet":"zijn sleepafstand maal 2"}},"initialisation":false,"presentation":"calculation"}]},{"node":"Rule","name":"gedeelde sleepafstand","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"gedeelde sleepafstand","subjectType":"Vondst","source":{"snippet":"De gedeelde sleepafstand van een Vondst"}},"value":{"node":"Binary","op":"dividedBy","left":{"node":"AttributeReference","attribute":"sleepafstand","subjectType":"Vondst","source":{"snippet":"zijn sleepafstand"}},"right":{"node":"Literal","value":{"number":"4"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"4"}},"type":{"node":"Number","whole":false,"decimals":0,"sign":"unspecified"},"source":{"snippet":"zijn sleepafstand gedeeld door 4"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|} in
  let case_data input_data =
    Printf.sprintf
      {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "V1", "objectType": "Vondst",
      "attributes": { "sleepafstand": { "number": "%s" } }, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
      input_data
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv24" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" doc in
  let compute input_data =
    let casef = write ".case.json" (case_data input_data) in
    Fun.protect
      ~finally:(fun () -> Sys.remove casef)
      (fun () ->
        let d = Ir_v0.Loader.load_file irf in
        let c = Case.load_file d casef in
        let final =
          Eval.evaluate
            { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
              locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
            c.world
        in
        let v1 = World.get final "V1" in
        (World.attr v1 "sleepafstand", World.attr v1 "dubbele sleepafstand",
         World.attr v1 "gedeelde sleepafstand"))
  in
  Fun.protect
    ~finally:(fun () -> Sys.remove irf)
    (fun () ->
      let input_data, duplicate, shared = compute "1/6" in
      expect_num "WV-2.4-g-invoer" input_data "1/6";
      expect_num "WV-2.4-g-maal" duplicate "1/3";
      expect_num "WV-2.4-g-gedeeld" shared "1/24";
      let input_data, duplicate, shared = compute "1.5" in
      expect_num "WV-2.4-h-invoer" input_data "3/2";
      expect_num "WV-2.4-h-maal" duplicate "3";
      expect_num "WV-2.4-h-gedeeld" shared "3/8")

(* --- ch. 2.5: the two rounding tables, cell by cell --------------------- *)

let () =
  let row name d value cells =
    List.iter2
      (fun (mode, label) expected ->
        check
          (Printf.sprintf "WV-2.5-%s-%s" name label)
          (Q.equal (Arith.round_q mode d (Value.q_of_lexeme value)) (q expected)))
      [
        (Ir_v0.Ast.Floor, "beneden");
        (Ceiling, "boven");
        (Toward_zero, "nul");
        (Away_from_zero, "weg");
        (Half_away_from_zero, "rekenkundig");
      ]
      cells
  in
  (* to 2 decimals *)
  row "2dec-2345" 2 "2.345" [ "234/100"; "235/100"; "234/100"; "235/100"; "235/100" ];
  row "2dec-n2345" 2 "-2.345" [ "-235/100"; "-234/100"; "-234/100"; "-235/100"; "-235/100" ];
  row "2dec-2355" 2 "2.355" [ "235/100"; "236/100"; "235/100"; "236/100"; "236/100" ];
  row "2dec-n2355" 2 "-2.355" [ "-236/100"; "-235/100"; "-235/100"; "-236/100"; "-236/100" ];
  (* to 0 decimals *)
  row "0dec-25" 0 "2.5" [ "2"; "3"; "2"; "3"; "3" ];
  row "0dec-n25" 0 "-2.5" [ "-3"; "-2"; "-2"; "-3"; "-3" ];
  row "0dec-2345" 0 "2.345" [ "2"; "3"; "2"; "3"; "2" ];
  row "0dec-n2345" 0 "-2.345" [ "-3"; "-2"; "-2"; "-3"; "-2" ]

(* --- ch. 2.6: unit algebra ---------------------------------------------- *)

let unit_ ?denominator ?(factor = (1, 1)) ~canonical_form afk : Ir_v0.Ast.measure_unit =
  {
    abbr_text = Some afk;
    system = Some "test";
    canonical_form = Some canonical_form;
    factor_to_canonical = Some { num = fst factor; den = snd factor };
    denominator;
  }

let mnd = unit_ ~canonical_form:"mnd" "mnd"
let jr = unit_ ~canonical_form:"mnd" ~factor:(12, 1) "jr"
let kw = unit_ ~canonical_form:"mnd" ~factor:(3, 1) "kw"

(* a declared month-sized unit that is NOT spelled as a calendar unit: the
   ch. 3.6 integration cancels it through the declared factor, never
   through the spelling table *)
let bm = unit_ ~canonical_form:"mnd" "bm"

(* a day unit in its OWN canonical chain: the declarations connect it to
   nothing month-sized, so nothing may convert it *)
let dg = unit_ ~canonical_form:"dg" "dg"
let kg = unit_ ~canonical_form:"kg" "kg"
let eur = unit_ ~canonical_form:"EUR" "€"
let cent = unit_ ~canonical_form:"EUR" ~factor:(1, 100) "ct"
let meter = unit_ ~canonical_form:"m" "m"
let eur_per_jr = { eur with denominator = Some jr }
let num_with_unit u s = Value.Num (Value.q_of_lexeme s, Value.Unit u)

let () =
  check "WV-2.6-a" (Arith.compare_op Equal (num_with_unit mnd "24") (num_with_unit jr "2") = true);
  (match Arith.mul (num_with_unit eur_per_jr "100") (num_with_unit jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "WV-2.6-b" (Q.equal v (q "200") && u.denominator = None && u.abbr_text = Some "€")
  | _ -> check "WV-2.6-b" false);
  (match Arith.mul (num_with_unit eur "3") (num_with_unit meter "2") with
  | Value.Num (v, Value.Product _) -> check "WV-2.6-c" (Q.equal v (q "6"))
  | _ -> check "WV-2.6-c" false);
  (match Arith.div_ ~err:no_err (num_with_unit eur "6") (num_with_unit eur "3") with
  | Value.Num (v, Value.Unitless) -> check "WV-2.6-d" (Q.equal v (q "2"))
  | _ -> check "WV-2.6-d" false);
  (match Arith.div_ ~err:no_err (num_with_unit eur "600") (num_with_unit jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "WV-2.6-e" (Q.equal v (q "300") && u.abbr_text = Some "€" && u.denominator = Some jr)
  | _ -> check "WV-2.6-e" false);
  (* plus restates the right operand into the left unit: 2 jr + 12 mnd = 3 jr *)
  (match Arith.add (num_with_unit jr "2") (num_with_unit mnd "12") with
  | Value.Num (v, Value.Unit u) -> check "ch2.6-add-restate" (Q.equal v (q "3") && u.abbr_text = Some "jr")
  | _ -> check "ch2.6-add-restate" false);
  (* a unit fault must NAME the two units apart: a rate and its numerator
     both printed as the numerator's abbreviation, so every mismatch
     between them read "€ vs €" and said nothing *)
  check "verify-show-unit-denominator"
    (Arith.show_unit (Value.Unit eur_per_jr) = "€/jr" && Arith.show_unit (Value.Unit eur) = "€");
  (match Arith.restate_into (Value.Unit eur) (q "1") (Value.Unit eur_per_jr) with
  | exception Value.Fault { text = m; _ } ->
      check "verify-show-unit-in-fault" (m = "incompatible units: € vs €/jr")
  | _ -> check "verify-show-unit-in-fault" false)

(* IR-SPEC ch. 2.5, the conversion algebra's worked examples verbatim: the
   factor is the exact rational the wire writes, a restatement multiplies by
   factor(u)/factor(v) as a rational, and nothing is rounded on the way —
   `18 mnd` is `3/2 jr`, not 1.4999… or 1.5000…1 *)
let () =
  let restate ~into v from = Arith.restate_into (Value.Unit into) (q v) (Value.Unit from) in
  check "WV-2.5-3jr-is-36mnd" (Q.equal (restate ~into:mnd "3" jr) (q "36"));
  check "WV-2.5-18mnd-is-3/2jr" (Q.equal (restate ~into:jr "18" mnd) (q "3/2"));
  check "WV-2.5-factor-mnd-jr-1/12" (Q.equal (Arith.conv_factor mnd jr) (q "1/12"));
  check "WV-2.5-100eur/mnd-is-1200eur/jr"
    (Q.equal (restate ~into:eur_per_jr "100" { eur with denominator = Some mnd }) (q "1200"))

(* ...and the factor that is not a whole number, the case the exact rational
   exists for: an hour declared as {1, 24} of a day. 36 uur restated in dg is
   3/2 exactly, and 3/2 dg is 36 uur again. The JSON number a v0 wire carried
   for 1/24 is the double 0.041666666666666664…, not 1/24: taken as the exact
   value of that double, 36 of them are 1.4999…, and the short decimal reading
   this interpreter used to attempt finds none and faults. So these three
   hold only when the factor is read as the rational the wire writes. *)
let () =
  let hour = unit_ ~canonical_form:"dg" ~factor:(1, 24) "uur" in
  let restate ~into v from = Arith.restate_into (Value.Unit into) (q v) (Value.Unit from) in
  check "WV-2.5-factor-uur-dg-1/24" (Q.equal (Arith.conv_factor hour dg) (q "1/24"));
  check "WV-2.5-36uur-is-3/2dg" (Q.equal (restate ~into:dg "36" hour) (q "3/2"));
  check "WV-2.5-3/2dg-is-36uur" (Q.equal (restate ~into:hour "3/2" dg) (q "36"))

(* WF-092, the executor's half: the canonical unit and the factor are one
   fact in two fields, and the factor question refuses to guess.

   The BARE form — both null — keeps its meaning and its old behaviour:
   factor 1. It is NOT a unit that converts into itself alone, though:
   convertibility reads a missing canonical as the ABBREVIATION, so a bare
   unit is a canonical TARGET for every declared unit whose canonical
   string spells that abbreviation, and the crossing quantity restates on
   THAT unit's factor — the `kaal-is-canoniek-doel` witness below pins that
   reading (2 jr into a bare `mnd` is 24, not 2). The branch is live, not
   decorative: making it fault turns 9 corpus cases over 3 goldens red (the
   duration parameters and duration operands).

   Either HALF-FILLED form is what WF-092 refuses, and both used to read
   silently. (a) A canonical target with no factor: measured on a corpus
   copy with a canonical-`m` `km` stripped of its factor, 2,5 km + 300 m
   came out 302,5 instead of 2,8 km, and surfaced only as a DECIMALS
   complaint about a number whose unit was already wrong. (b) A factor with
   no canonical target: measured on the wf092-factor-without-canonical
   negative, ref and the ASP backend both replayed a clean 2800 while a
   third executor computed 302.5 — nothing refused. Both now fault by name,
   so no executor computes quietly on a shape the verifier rejects. *)
let mnd_bare : Ir_v0.Ast.measure_unit =
  { abbr_text = Some "mnd"; system = Some "test"; canonical_form = None;
    factor_to_canonical = None; denominator = None }

let jr_half : Ir_v0.Ast.measure_unit = { jr with factor_to_canonical = None }
let mnd_half_b : Ir_v0.Ast.measure_unit = { mnd with canonical_form = None }

let () =
  (* the bare form: unchanged, factor 1 *)
  check "verify-wf092-bare-is-factor-1" (Q.equal (Arith.factor mnd_bare) Q.one);
  (match Arith.add (num_with_unit mnd_bare "2") (num_with_unit mnd_bare "12") with
  | Value.Num (v, Value.Unit u) ->
      check "verify-wf092-bare-adds-up" (Q.equal v (q "14") && u.abbr_text = Some "mnd")
  | _ -> check "verify-wf092-bare-adds-up" false);
  (* …and it is a canonical TARGET, not an island: `jr` declares canonical
     "mnd", the bare unit spells "mnd" as its abbreviation, so they are
     convertible and 2 jr restates on jr's own factor 12 *)
  (match Arith.add (num_with_unit mnd_bare "3") (num_with_unit jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "verify-wf092-bare-is-canonical-target"
        (Q.equal v (q "27") && u.abbr_text = Some "mnd")
  | _ -> check "verify-wf092-bare-is-canonical-target" false);
  (* half-filled (a): a loud fault naming the unit and its target *)
  (match Arith.factor jr_half with
  | exception Value.Fault { text = m; _ } ->
      check "verify-wf092-half-form-fails-loudly"
        (m = "unit jr names canonical unit \"mnd\" but carries no conversion factor into it")
  | _ -> check "verify-wf092-half-form-fails-loudly" false);
  (* and it faults where it would otherwise have lied: the restatement
     that used to read the missing factor as 1 (2 jr + 12 mnd = 3 jr) *)
  expect_fault "verify-wf092-half-form-fails-in-conversion" (fun () ->
      Arith.add (num_with_unit jr_half "2") (num_with_unit mnd "12"));
  (* half-filled (b), the mirror: a factor with nothing to convert into.
     Symmetric to (a) and equally loud — the arm that used to answer the
     factor question with the stray factor itself, unasked what it converts
     into *)
  (match Arith.factor mnd_half_b with
  | exception Value.Fault { text = m; _ } ->
      check "verify-wf092-half-form-b-fails-loudly"
        (m
       = "unit mnd carries conversion factor 1 but names no canonical unit to convert into")
  | _ -> check "verify-wf092-half-form-b-fails-loudly" false);
  expect_fault "verify-wf092-half-form-b-fails-in-conversion" (fun () ->
      Arith.add (num_with_unit mnd_half_b "12") (num_with_unit jr "2"))

(* --- ch. 2.7: clamping -------------------------------------------------- *)

let () =
  expect_num "WV-2.7-a" (Arith.clamp ~min_:(n "0") ~max_:(n "100") (n "150")) "100";
  expect_num "WV-2.7-b" (Arith.clamp ~min_:(n "0") ~max_:(n "100") (n "-5")) "0";
  expect_num "WV-2.7-c"
    (Arith.round_v Half_away_from_zero 2 (Arith.clamp ~min_:(n "0") ~max_:(n "1") (n "2/3")))
    "0.67"

(* --- ch. 2.8: tijdsevenredig deel (the arithmetic core) ------------------ *)

let () =
  let v = Q.mul (q "1200") (q "90/365") in
  check "WV-2.8-a-exact" (Q.equal v (q "21600/73"));
  check "WV-2.8-a-rounded" (Q.equal (Arith.round_q Half_away_from_zero 2 v) (q "29589/100"))

(* --- ch. 4.6: the allocation recurrence, verbatim ------------------------ *)

let rcp ?weight ?cap ?key id = { Alloc.id; weight; cap; key }

let shares_of = function
  | Alloc.Distributed (shares, rest) ->
      ( List.map
          (fun (id, s) ->
            (id, match s with Alloc.Assigned q -> Some q | Alloc.Excluded -> None))
          shares,
        Some rest )
  | Alloc.Nothing -> ([], None)

let no_alloc_err = fun ~id:_ (_ : Value.deposit) -> ()

let rest_is r because = match r with Some x -> Q.equal x (q because) | None -> false

let alloc ?(err = no_alloc_err) method_name ?(descending = false) ?decimals pot recs =
  shares_of (Alloc.allocate method_name ~descending ~decimals ~pot:(n pot) ~err recs)

let assigned name shares expected =
  check name
    (List.for_all2
       (fun (id, s) (eid, es) ->
         String.equal id eid
         && match (s, es) with
            | Some got, Some because -> Q.equal got (q because)
            | None, None -> true
            | _ -> false)
       shares expected)

let () =
  (* 4.6-a equal shares *)
  let s, r = alloc Equal_shares "90" [ rcp "a"; rcp "b"; rcp "c" ] in
  assigned "WV-4.6-a1" s [ ("a", Some "30"); ("b", Some "30"); ("c", Some "30") ];
  check "WV-4.6-a1-rest" (rest_is r "0");
  let s, _ = alloc Equal_shares "1000" [ rcp "a"; rcp "b" ] in
  assigned "WV-4.6-a2" s [ ("a", Some "500"); ("b", Some "500") ];
  (* 4.6-b proportional; a weightless fourth stays leeg *)
  let errs = ref 0 in
  let s, _ =
    alloc ~err:(fun ~id:_ _ -> incr errs) Proportional "120"
      [ rcp ~weight:(n "1") "a"; rcp ~weight:(n "2") "b"; rcp ~weight:(n "3") "c"; rcp ~weight:empty_value "d" ]
  in
  assigned "WV-4.6-b" s [ ("a", Some "20"); ("b", Some "40"); ("c", Some "60"); ("d", None) ];
  check "WV-4.6-b-err" (!errs = 1);
  (* 4.6-c proportional + floor + rest: 1001 over 3:2, whole euros *)
  let s, r =
    alloc Proportional ~decimals:0 "1001" [ rcp ~weight:(n "3") "a"; rcp ~weight:(n "2") "b" ]
  in
  assigned "WV-4.6-c" s [ ("a", Some "600"); ("b", Some "400") ];
  check "WV-4.6-c-rest" (rest_is r "1");
  (* 4.6-d proportional + cap + rest (the berging shape) *)
  let s, r =
    alloc Proportional ~decimals:0 "1200"
      [
        rcp ~weight:(n "3") ~cap:(n "300") "a";
        rcp ~weight:(n "1") ~cap:(n "300") "b";
        rcp ~weight:(n "2") ~cap:(n "300") "c";
      ]
  in
  assigned "WV-4.6-d" s [ ("a", Some "300"); ("b", Some "200"); ("c", Some "300") ];
  check "WV-4.6-d-rest" (rest_is r "400");
  (* 4.6-e cap degrade: empty and negative caps degrade to uncapped,
     with two §9.5 errors — one per degraded cap (the decided ch. 4.6
     erratum; ch. 4.3 + ch. 5.6's rule × subject discipline). *)
  let errs = ref 0 in
  let s, _ =
    alloc ~err:(fun ~id:_ _ -> incr errs) Proportional "1200"
      [
        rcp ~weight:(n "3") ~cap:empty_value "a";
        rcp ~weight:(n "2") ~cap:(n "-5") "b";
        rcp ~weight:(n "1") ~cap:(n "300") "c";
      ]
  in
  assigned "WV-4.6-e" s [ ("a", Some "600"); ("b", Some "400"); ("c", Some "200") ];
  check "WV-4.6-e-err" (!errs = 2);
  (* 4.6-f ordered depletion, ascending then descending *)
  let recs () =
    [
      rcp ~weight:(n "3") ~key:(n "20") "a";
      rcp ~weight:(n "1") ~key:(n "20") "b";
      rcp ~weight:(n "1") ~key:(n "22") "c";
    ]
  in
  let s, r = alloc Ordered "1200" (recs ()) in
  assigned "WV-4.6-f-asc" s [ ("a", Some "900"); ("b", Some "300"); ("c", Some "0") ];
  check "WV-4.6-f-asc-rest" (rest_is r "0");
  let s, _ = alloc Ordered ~descending:true "1200" (recs ()) in
  assigned "WV-4.6-f-desc" s [ ("c", Some "1200"); ("a", Some "0"); ("b", Some "0") ];
  (* 4.6-g the full recurrence: cap + floor + depletion over 1800 *)
  let s, r =
    alloc Ordered ~decimals:0 "1800"
      [
        rcp ~weight:(n "3") ~cap:(n "500") ~key:(n "22") "p22";
        rcp ~weight:(n "2") ~cap:(n "500") ~key:(n "39") "p39a";
        rcp ~weight:(n "1") ~cap:(n "500") ~key:(n "39") "p39b";
        rcp ~weight:(n "3") ~cap:(n "250") ~key:(n "66") "p66a";
        rcp ~weight:(n "1") ~cap:(n "250") ~key:(n "66") "p66b";
      ]
  in
  assigned "WV-4.6-g" s
    [
      ("p22", Some "500");
      ("p39a", Some "500");
      ("p39b", Some "433");
      ("p66a", Some "250");
      ("p66b", Some "91");
    ];
  check "WV-4.6-g-rest" (rest_is r "26");
  (* 4.6-h edge rows *)
  check "WV-4.6-h-empty" (alloc Proportional "100" [] = ([], None));
  let s, _ =
    alloc Ordered "1200" [ rcp ~weight:(n "3") ~key:(n "1") "a"; rcp ~weight:(n "-1") ~key:(n "1") "b" ]
  in
  assigned "WV-4.6-h-neg" s [ ("a", Some "1200"); ("b", Some "0") ];
  let s, _ =
    alloc Ordered "1200"
      [
        rcp ~weight:(n "0") ~key:(n "1") "a";
        rcp ~weight:(n "0") ~key:(n "1") "b";
        rcp ~weight:(n "1") ~key:(n "2") "c";
      ]
  in
  assigned "WV-4.6-h-zero" s [ ("a", Some "0"); ("b", Some "0"); ("c", Some "1200") ];
  let s, _ =
    alloc Ordered "300"
      [
        rcp ~key:(n "2020") "a";
        rcp ~key:empty_value "b";
        rcp ~key:(n "2021") "c";
      ]
  in
  (* evaluation order 2020, 2021, leeg (the empty key last) *)
  check "WV-4.6-h-leeg-laatst" (List.map fst s = [ "a"; "c"; "b" ])

(* --- verification-round regressions ------------------------------------- *)

let () =
  (* ch. 1.2d asymmetry for two empties routes on the static kind:
     numeric → waar, non-numeric (§5 Tabel 38) → onwaar *)
  check "verify-eq-static-numeric" (Arith.compare_op ~static_numeric:true Equal empty_value empty_value = true);
  check "verify-eq-static-nonnum" (Arith.compare_op ~static_numeric:false Equal empty_value empty_value = false);
  check "verify-neq-static-nonnum"
    (Arith.compare_op ~static_numeric:false Not_equal empty_value empty_value = false);
  (* strict lexeme validation: no silent +inf or 0 *)
  expect_fault "verify-lexeme-zero-den" (fun () -> Value.q_of_lexeme "1/0");
  expect_fault "verify-lexeme-empty" (fun () -> Value.q_of_lexeme "");
  expect_fault "verify-lexeme-junk" (fun () -> Value.q_of_lexeme "abc");
  (* one number grammar since the L4 landing (WF-081): the case format's, so
     a decimal comma, a bare leading point and a denominator with a leading
     0 are malformed, never read as the number they resemble *)
  expect_fault_msg "verify-lexeme-comma" (fun () -> Value.q_of_lexeme "12,50")
    {|malformed number lexeme "12,50"|};
  expect_fault_msg "verify-lexeme-kale-punt" (fun () -> Value.q_of_lexeme ".5")
    {|malformed number lexeme ".5"|};
  expect_fault_msg "verify-lexeme-negative-bare-point" (fun () -> Value.q_of_lexeme "-.5")
    {|malformed number lexeme "-.5"|};
  expect_fault_msg "verify-lexeme-denominator-leading-zero" (fun () -> Value.q_of_lexeme "3/04")
    {|malformed number lexeme "3/04"|};
  check "verify-lexeme-neg-dec" (Q.equal (Value.q_of_lexeme "-0.5") (q "-1/2"));
  (* a unit factor reads as the exact rational the wire writes, never
     through a double: {1, 100} is 1/100 exactly and {1000, 1} is 1000 *)
  check "verify-wire-factor-cent" (Q.equal (Arith.factor cent) (q "1/100"));
  check "verify-wire-factor-int"
    (Q.equal (Arith.factor (unit_ ~canonical_form:"g" ~factor:(1000, 1) "kg")) (q "1000"));
  (* an astronomic exponent faults instead of escaping as Z.Overflow *)
  expect_fault "verify-pow-overflow" (fun () ->
      Arith.pow_ (n "2") (Value.Num (Q.of_string "100000000000000000000", Value.Unitless)))

(* --- ch. 6.4: evaluation by strata (worked examples, hand-built docs) --- *)

let empty_doc : Ir_v0.Ast.document =
  {
    object_types = [];
    domain_specs = [];
    parameters = [];
    unit_systems = [];
    fact_types = [];
    dimensions = [];
    timelines = [];
    day_kinds = [];
    resolved_rules = [];
    resolved_day_kind_definitions = [];
    object_creations = [];
    allocations = [];
    consistencies = [];
    uniquenesses = [];
    uniqueness_unions = [];
    fact_creations = [];
    resolved_start_point_determinations = [];
    rule_groups = [];
    diagnostics = [];
    prioritized_defaults = [];
    function_defs = [];
    presentation_annotations = [];
  }

let ot name attrs characteristics : string * Ir_v0.Ast.object_type =
  ( name,
    {
      ot_ident = Some name;
      ot_supertype = None;
      attributes = attrs;
      attribute_plural = [];
      attribute_dimensions = [];
      attribute_granularity = [];
      attribute_timeline_count = [];
      attribute_timeline_names = [];
      characteristics = List.map (fun k -> Some k) characteristics;
      characteristic_kinds = [];
      characteristic_granularity = [];
      characteristic_timeline_count = [];
      animate = false;
      plural = None;
    } )

let num0 : Ir_v0.Ast.datatype = Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = None }

(* the subject's own read: no navigation bindings, and no type of its own
   (B13) — the declaration is the one source *)
let aref attr otn : Ir_v0.Ast.expr =
  Attribute_reference { attribute_ref = Some attr; object_type = Some otn; source_name = None; navigation_bindings = None }

(* a literal as the loader reads it off the wire (Ir_v0.Loader): the value
   variant its type names *)
let litv lex typ : Ir_v0.Ast.expr =
  let variant : Ir_v0.Ast.literal_variant =
    match typ with
    | Ir_v0.Ast.Number _ | Percentage _ -> Lv_number lex
    | Boolean -> (
        match lex with
        | "waar" -> Lv_boolean true
        | "onwaar" -> Lv_boolean false
        | _ -> invalid_arg ("litv: a boolean literal is waar or onwaar, not " ^ lex))
    | Text_type -> Lv_text lex
    | Date_in_days -> Lv_date lex
    | Date_time_type -> Lv_date_time lex
    | Enumeration _ -> Lv_enumeration lex
    | Sum_type _ -> Lv_constructor lex
    | List_type _ | Struct_type _ -> invalid_arg "litv: a list or struct type names no literal variant"
  in
  Literal { variant; typ = Some typ; source_name = None }

let rr name variety objective value_node cond : Ir_v0.Ast.resolved_rule =
  { rr_name = Some name; rr_presentation = Some variety; objective; value_node; rr_condition = cond; rr_validity = None;
    rr_source_annotations = None; rr_pointer = "" }

let inst label otype attrs : World.instance =
  {
    label;
    otype;
    attrs = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty attrs;
    chars = [];
    left_side = World.SMap.empty;
    tls = World.SMap.empty; ctls = World.SMap.empty;
  }

(* Every version record carries the wire pointer the loader sets on it
   (`/rules/<list>/<f>/versions/<v>`), and ref finds a form's stratum by that
   pointer. A hand-built document leaves the pointers empty; [env_of] fills
   each empty one the way the loader numbers a form with one version, the
   uniqueness unions after the single-source ones in the wire's one list. *)
let with_pointers (doc : Ir_v0.Ast.document) : Ir_v0.Ast.document =
  let at list i p = if p = "" then Printf.sprintf "/rules/%s/%d/versions/0" list i else p in
  let n_uni = List.length doc.uniquenesses in
  {
    doc with
    resolved_rules =
      List.mapi
        (fun i (r : Ir_v0.Ast.resolved_rule) ->
          { r with rr_pointer = at "rules" i r.rr_pointer })
        doc.resolved_rules;
    object_creations =
      List.mapi
        (fun i (o : Ir_v0.Ast.resolved_object_creation) ->
          { o with oc_pointer = at "objectCreations" i o.oc_pointer })
        doc.object_creations;
    allocations =
      List.mapi
        (fun i (v : Ir_v0.Ast.resolved_allocation) ->
          { v with vd_pointer = at "allocations" i v.vd_pointer })
        doc.allocations;
    consistencies =
      List.mapi
        (fun i (c : Ir_v0.Ast.resolved_consistency) ->
          { c with cons_pointer = at "consistencies" i c.cons_pointer })
        doc.consistencies;
    uniquenesses =
      List.mapi
        (fun i (u : Ir_v0.Ast.resolved_uniqueness) ->
          { u with uni_pointer = at "uniquenesses" i u.uni_pointer })
        doc.uniquenesses;
    uniqueness_unions =
      List.mapi
        (fun i (u : Ir_v0.Ast.resolved_uniqueness_union) ->
          { u with uu_pointer = at "uniquenesses" (n_uni + i) u.uu_pointer })
        doc.uniqueness_unions;
    fact_creations =
      List.mapi
        (fun i (f : Ir_v0.Ast.resolved_fact_creation) ->
          { f with fc_pointer = at "factCreations" i f.fc_pointer })
        doc.fact_creations;
  }

let env_of doc : Eval.env =
  { doc = with_pointers doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None;
    locals = Eval.SMap.empty; btypes = Eval.SMap.empty; subj = None }

(* WV-6.4-a (= the ch. 5.3 pair): producer/consumer across one negative
   edge — {waar, onwaar} at inkomen 50, {onwaar, waar} at 200, never the
   transient {waar, waar} *)
let () =
  let doc =
    {
      empty_doc with
      object_types = [ ot "Jutter" [ ("inkomen", Some num0) ] [ "vrijgesteld"; "bijdrageplichtig" ] ];
      resolved_rules =
        [
          rr "vrijstelling" Characteristic_attribution
            (Some (aref "vrijgesteld" "Jutter"))
            (Some (litv "waar" Boolean))
            (Some
               (Comparison
                  {
                    op = Less;
                    left_side = Some (aref "inkomen" "Jutter");
                    right_side = Some (litv "100" num0);
                    time_period = None;
                    source_name = None;
                  }));
          rr "bijdrageplicht" Characteristic_attribution
            (Some (aref "bijdrageplichtig" "Jutter"))
            (Some (litv "waar" Boolean))
            (Some
               (Characteristic_check
                  {
                    characteristic = Some "vrijgesteld";
                    negation = true;
                    holder = Some "Jutter";
                    time_period = None;
                    source_name = None;
                    navigation_binding = None;
                  }));
        ];
    }
  in
  let run inkomen =
    let w = World.add_instance World.empty (inst "j" "Jutter" [ ("inkomen", n inkomen) ]) in
    let final = Eval.evaluate (env_of doc) w in
    let j = World.get final "j" in
    (World.has_char j "vrijgesteld", World.has_char j "bijdrageplichtig")
  in
  check "WV-6.4-a-50" (run "50" = (true, false));
  check "WV-6.4-a-200" (run "200" = (false, true))

(* WV-5.5-charinit: a characteristic initialisation in an ObjectCreatie may
   carry a boolean-TYPED expression, evaluated on the creation subject exactly
   as an attribute initialisation is (the WF-027 relaxation, maintainer
   decision 2026-08-24, besluit 1). The caller Jutter carries the boolean; the
   minted Rekenkamer's kenmerk is set iff the value evaluates true, left unset
   when it evaluates false (closed-world). The boolean constant stays the v0
   form; a value that evaluates non-boolean faults fast. *)
let () =
  let doc_of value_node : Ir_v0.Ast.document =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [ ("overzees", Some Boolean) ] []; ot "Rekenkamer" [] [ "overzees" ] ];
      object_creations =
        [
          {
            oc_name = Some "richt de rekenkamer op";
            created_type = Some "Rekenkamer";
            oc_subject = Some "Jutter";
            assignments =
              [ { to_attribute = Some "overzees"; to_value = Some value_node; to_characteristic = true } ];
            oc_condition = None;
            oc_validity = None;
            oc_connecting_role = None;
            oc_pointer = "";
          };
        ];
    }
  in
  let mint value_node overzees_in =
    let doc = doc_of value_node in
    let w = World.add_instance World.empty (inst "j" "Jutter" [ ("overzees", overzees_in) ]) in
    let final = Eval.evaluate (env_of doc) w in
    World.has_char (World.get final "oc0::j") "overzees"
  in
  let read_overzees = aref "overzees" "Jutter" in
  check "WV-5.5-charinit-expr-true" (mint read_overzees (Value.Bool true) = true);
  check "WV-5.5-charinit-expr-false" (mint read_overzees (Value.Bool false) = false);
  (* the empty flank (a missing caller attribute reads leeg) joins false:
     the mark is left unset, no fault — the attribute-init parallel and the
     closed-world reading, identical to the ASP backend's absent atom *)
  check "WV-5.5-charinit-expr-empty" (mint read_overzees Value.Empty = false);
  (* the preserved v0 constant form is unconditional — set on waar regardless
     of the caller, unset on onwaar *)
  check "WV-5.5-charinit-const-waar" (mint (litv "waar" Boolean) (Value.Bool false) = true);
  check "WV-5.5-charinit-const-onwaar" (mint (litv "onwaar" Boolean) (Value.Bool true) = false);
  (* a value that evaluates to a non-boolean faults fast (the runtime
     bounded-write guard behind the verifier's static boolean-type check) *)
  expect_fault "WV-5.5-charinit-nonbool-fault" (fun () ->
      ignore (mint (litv "5" num0) (Value.Bool true)))

(* WV-6.4-b (= the ch. 5.6 channel read): one error fact, both dossiers
   flagged — the reading rule sits strictly above the consistency *)
let () =
  let doc =
    {
      empty_doc with
      object_types =
        [
          ot "Rekeninghouder" [ ("saldo", Some num0) ] [];
          ot "Dossier" [] [ "onder verscherpt toezicht" ];
        ];
      consistencies =
        [
          {
            cons_name = Some "controle saldo";
            cons_subject = Some "Rekeninghouder";
            assertion =
              Some
                (Comparison
                   {
                     op = Greater_or_equal;
                     left_side = Some (aref "saldo" "Rekeninghouder");
                     right_side = Some (litv "0" num0);
                     time_period = None;
                     source_name = None;
                   });
            cons_source = None;
            cons_line = 0;
            cons_validity = None;
            cons_pointer = "";
          };
        ];
      resolved_rules =
        [
          rr "verscherpt toezicht" Characteristic_attribution
            (Some (aref "onder verscherpt toezicht" "Dossier"))
            (Some (litv "waar" Boolean))
            (Some (Rule_status_check { rule_def = Some "controle saldo"; inconsistent = true; source_name = None }));
        ];
    }
  in
  let w =
    List.fold_left
      (fun w i -> World.add_instance w i)
      World.empty
      [
        inst "h1" "Rekeninghouder" [ ("saldo", n "-5") ];
        inst "h2" "Rekeninghouder" [ ("saldo", n "10") ];
        inst "d1" "Dossier" [];
        inst "d2" "Dossier" [];
      ]
  in
  let final = Eval.evaluate (env_of doc) w in
  check "WV-6.4-b-one-fact" (List.length final.channel = 1);
  check "WV-6.4-b-d1" (World.has_char (World.get final "d1") "onder verscherpt toezicht");
  check "WV-6.4-b-d2" (World.has_char (World.get final "d2") "onder verscherpt toezicht")

(* WV-6.4-d: the four-deep negation chain — strata 0..3; x=0 yields
   {onwaar, waar, onwaar, waar}, x=1 the complement. Rules listed in
   deliberately wrong order to prove the layering, not the listing,
   decides. *)
let () =
  let kc name = 
    Ir_v0.Ast.Characteristic_check
      { characteristic = Some name; negation = true; holder = Some "Aanvraag"; time_period = None; source_name = None; navigation_binding = None }
  in
  let kt name char cond =
    rr name Characteristic_attribution
      (Some (aref char "Aanvraag"))
      (Some (litv "waar" Boolean))
      (Some cond)
  in
  let doc =
    {
      empty_doc with
      object_types = [ ot "Aanvraag" [ ("x", Some num0) ] [ "k1"; "k2"; "k3"; "k4" ] ];
      resolved_rules =
        [
          kt "trap4" "k4" (kc "k3");
          kt "trap3" "k3" (kc "k2");
          kt "trap2" "k2" (kc "k1");
          kt "trap1" "k1"
            (Comparison
               {
                 op = Greater_or_equal;
                 left_side = Some (aref "x" "Aanvraag");
                 right_side = Some (litv "1" num0);
                 time_period = None;
                 source_name = None;
               });
        ];
    }
  in
  let run x =
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "a" "Aanvraag" [ ("x", n x) ])) in
    let a = World.get final "a" in
    (World.has_char a "k1", World.has_char a "k2", World.has_char a "k3", World.has_char a "k4")
  in
  check "WV-6.4-d-x0" (run "0" = (false, true, false, true));
  check "WV-6.4-d-x1" (run "1" = (true, false, true, false))

(* WV-6.4-e: an emptiness test on a derived attribute by a characteristic
   rule is a negative edge (stratum-strict until 2026-09-23, when every
   read under a leeg/gevuld test became negative) — either way the reader
   sits above the writer and the outcome is rule-order-independent (the
   verification round's counterexample, locked) *)
let () =
  let writer =
    rr "zet a" Equation (Some (aref "a" "Ding")) (Some (litv "5" num0)) None
  in
  let reader =
    rr "markeer" Characteristic_attribution
      (Some (aref "gemarkeerd" "Ding"))
      (Some (litv "waar" Boolean))
      (Some
         (Predicate
            {
              variety = Empty_predicate;
              operand = Some (aref "a" "Ding");
              day_kind = None;
              negation = false;
              digit_chars = 0;
              time_period = None;
              source_name = None;
            }))
  in
  let run rules =
    let doc =
      { empty_doc with object_types = [ ot "Ding" [ ("a", Some num0) ] [ "gemarkeerd" ] ]; resolved_rules = rules }
    in
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "x" "Ding" [])) in
    World.has_char (World.get final "x") "gemarkeerd"
  in
  check "WV-6.4-e-writer-first" (run [ writer; reader ] = false);
  check "WV-6.4-e-reader-first" (run [ reader; writer ] = false)

(* WV-1.2-neg: the predicate's negated field is the inner, per-value
   negation and complements the leeg/gevuld test — negated leeg is
   gevuld, negated gevuld is leeg (ch. 1.2, row leeg/gevuld) *)
let () =
  let pred variety negation : Ir_v0.Ast.condition_expr =
    Predicate
      {
        variety;
        operand = Some (aref "a" "Ding");
        day_kind = None;
        negation;
        digit_chars = 0;
        time_period = None;
        source_name = None;
      }
  in
  let run variety negation attrs =
    let reader =
      rr "markeer" Characteristic_attribution
        (Some (aref "gemarkeerd" "Ding"))
        (Some (litv "waar" Boolean))
        (Some (pred variety negation))
    in
    let doc =
      { empty_doc with object_types = [ ot "Ding" [ ("a", Some num0) ] [ "gemarkeerd" ] ]; resolved_rules = [ reader ] }
    in
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "x" "Ding" attrs)) in
    World.has_char (World.get final "x") "gemarkeerd"
  in
  check "WV-1.2-neg-leeg-op-5" (run Empty_predicate true [ ("a", n "5") ] = true);
  check "WV-1.2-neg-leeg-op-leeg" (run Empty_predicate true [] = false);
  check "WV-1.2-neg-gevuld-op-5" (run Filled true [ ("a", n "5") ] = false);
  check "WV-1.2-neg-gevuld-op-leeg" (run Filled true [] = true)

(* WV-6.4-c: a dependency cycle reaching ref/ is a fault (the verifier
   rejects it as WF-045; ref/ fails fast rather than diverging) *)
let () =
  let plus l r : Ir_v0.Ast.expr =
    Binary { op = Plus; left_side = Some l; right_side = Some r; typ = Some num0; source_name = None }
  in
  let doc =
    {
      empty_doc with
      object_types = [ ot "Ding" [ ("a", Some num0); ("b", Some num0) ] [] ];
      resolved_rules =
        [
          rr "bereken a" Calculation
            (Some (aref "a" "Ding"))
            (Some (plus (aref "b" "Ding") (litv "1" num0)))
            None;
          rr "bereken b" Calculation
            (Some (aref "b" "Ding"))
            (Some (plus (aref "a" "Ding") (litv "1" num0)))
            None;
        ];
    }
  in
  let w = World.add_instance World.empty (inst "x" "Ding" []) in
  expect_fault "WV-6.4-c-cycle-fault" (fun () -> Eval.evaluate (env_of doc) w)

(* WV-6.4-f: a leeg test in an ATTRIBUTE rule on a derived attribute is a
   negative edge (maintainer decision 2026-09-23), so the reader sits
   strictly above the writer and the rule order no longer matters. It was a
   positive edge until then: both rules shared a stratum, and a reader
   listed first acted on the transient leeg and kept its write, because
   nothing retracts it (ch. 6.1). One verifier-clean document gave b = 1
   with the reader first and leeg with the writer first. The ASP backend
   answered leeg in both orders. WV-6.4-e is the same pair with a
   characteristic reader, which was already stratum-strict. *)
let () =
  let writer = rr "zet a" Equation (Some (aref "a" "Ding")) (Some (litv "5" num0)) None in
  let reader =
    rr "zet b" Calculation
      (Some (aref "b" "Ding"))
      (Some (litv "1" num0))
      (Some
         (Predicate
            {
              variety = Empty_predicate;
              operand = Some (aref "a" "Ding");
              day_kind = None;
              negation = false;
              digit_chars = 0;
              time_period = None;
              source_name = None;
            }))
  in
  let run rules =
    let doc =
      {
        empty_doc with
        object_types = [ ot "Ding" [ ("a", Some num0); ("b", Some num0) ] [] ];
        resolved_rules = rules;
      }
    in
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "x" "Ding" [])) in
    World.attr (World.get final "x") "b"
  in
  expect_empty "WV-6.4-f-writer-first" (run [ writer; reader ]);
  expect_empty "WV-6.4-f-reader-first" (run [ reader; writer ])

(* WV-6.4-g: a leeg/gevuld test on a WF-045-admissible creation cycle is a
   WF-046 fault in ref/, in both polarities. The recursive creation "werf de
   opvolger" mints a successor Wachter; "merk van een wachter" writes
   merk = 1 from the successor's rang. The R3 shape tests "het merk is niet
   gevuld" in the creation and "de rang van de opvolger is niet gevuld" in
   the rule (negatie true in both); the R1 shape tests "het merk is leeg"
   and "de rang van de opvolger is gevuld" (negatie false in both). Both
   were verifier-clean until the arm. On R3 ref/ answered merk(W1) = 1,
   which is what its pass order produces (rules before creations): the
   opposite order mints W2 and leaves merk(W1) leeg, and the ASP reading
   has five stable models, one per chain length. On R1 ref/ minted a chain
   of five while the ASP reading has no stable model. Eval.evaluate re-runs
   the shared WF-045/046 check before it computes strata, so both
   documents are now refused, never answered. *)
let () =
  let mark = aref "merk" "Wachter" and ranking = aref "rang" "Wachter" in
  let successor : Ir_v0.Ast.role_binding = { rb_fact_type = "opvolging"; rb_role = "opvolger" } in
  let successor_rank : Ir_v0.Ast.expr =
    Attribute_reference
      {
        attribute_ref = Some "rang";
        object_type = Some "Wachter";
        source_name = None;
        navigation_bindings = Some [ successor ];
      }
  in
  let test variety negation operand : Ir_v0.Ast.condition_expr =
    Predicate
      { variety; operand = Some operand; day_kind = None; negation; digit_chars = 0; time_period = None; source_name = None }
  in
  let role_ref ident plural : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some "Wachter"; role_plural = plural; role_many = false }
  in
  let doc ~in_creation ~in_rule =
    {
      empty_doc with
      object_types = [ ot "Wachter" [ ("rang", Some num0); ("merk", Some num0) ] [] ];
      fact_types =
        [ ( "opvolging",
            { ft_name = Some "opvolging"; reciprocal = false;
              role_names = [ role_ref "voorganger" None; role_ref "opvolger" (Some "opvolgers") ] } ) ];
      resolved_rules =
        [ rr "merk van een wachter" Calculation (Some mark) (Some (litv "1" num0)) (Some in_rule) ];
      object_creations =
        [
          {
            oc_name = Some "werf de opvolger";
            created_type = Some "Wachter";
            oc_subject = Some "Wachter";
            assignments =
              [
                {
                  to_attribute = Some "rang";
                  to_value =
                    Some
                      (Binary
                         { op = Plus; left_side = Some ranking; right_side = Some (litv "1" num0); typ = Some num0;
                           source_name = None });
                  to_characteristic = false;
                };
              ];
            oc_condition =
              Some
                (Compound
                   {
                     quantifier_kind = All;
                     cutoff = 0;
                     components =
                       [
                         Comparison
                           { op = Less; left_side = Some ranking; right_side = Some (litv "5" num0);
                             time_period = None; source_name = None };
                         in_creation;
                       ];
                     source_name = None;
                   });
            oc_validity = None;
            oc_connecting_role = Some successor;
            oc_pointer = "";
          };
        ];
      rule_groups =
        [ { rgr_name = Some "estafette"; recursive = true; member_list = [ Some "werf de opvolger" ];
            rgr_source = None; rgr_line = 1 } ];
    }
  in
  let run d =
    Eval.evaluate (env_of d) (World.add_instance World.empty (inst "W1" "Wachter" [ ("rang", n "1") ]))
  in
  expect_fault_msg "WV-6.4-g-r3-negatie-waar"
    (fun () -> run (doc ~in_creation:(test Filled true mark) ~in_rule:(test Filled true successor_rank)))
    "WF-046 at /rules/objectCreations/0/versions/0: negative dependency edge on a cycle";
  expect_fault_msg "WV-6.4-g-r1-negatie-onwaar"
    (fun () -> run (doc ~in_creation:(test Empty_predicate false mark) ~in_rule:(test Filled false successor_rank)))
    "WF-046 at /rules/objectCreations/0/versions/0: negative dependency edge on a cycle"

(* WV-6.4-h (ch. 6.1, since 2026-10-06): a fact creation writes the relation
   it creates and no population, and a role check reads the relation it
   names. The object creation "premiepot" gives every Haven a Premiepot whose
   pot counts the havenjutters of the Haven; the fact creation "premiejutters"
   links those Jutters, which exist, to the Premiepot the creation made;
   "premiegerechtigd" holds for a Jutter that fills the role the fact creation
   creates. Strata: the creation 0, the fact creation 1 (it reads the
   creation's back-link stratum-strict), premiegerechtigd 2 (it reads the
   created relation stratum-strict). H1 with havenjutters J1 and J2, J3
   without a haven: J1 and J2 premiegerechtigd, J3 not. Until that date the
   fact creation wrote population(Jutter) as a proxy, the count read it, and
   the verifier and ref/ refused the document under WF-045 (premiepot ->
   premiejutters -> premiepot), while the ASP backend answered these values;
   the role check read nothing. The shape of
   corpus/golden/jutterswet-premiepot, without its parameter, its readers of
   the pot and the source snippets. *)
let () =
  let ir =
    {|
    {
      "format": "uchar-ir-v1",
      "model": {
        "objectTypes": [
          {
            "node": "ObjectType",
            "name": "Haven",
            "animate": false,
            "plural": "Havens",
            "attributes": [],
            "characteristics": []
          },
          {
            "node": "ObjectType",
            "name": "Jutter",
            "animate": true,
            "plural": "Jutters",
            "attributes": [],
            "characteristics": [
              {"node": "Characteristic", "name": "premiegerechtigd", "kind": "adjectival"}
            ]
          },
          {
            "node": "ObjectType",
            "name": "Premiepot",
            "animate": false,
            "plural": "Premiepotten",
            "attributes": [
              {
                "node": "Attribute",
                "name": "pot",
                "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}
              }
            ],
            "characteristics": []
          }
        ],
        "factTypes": [
          {
            "node": "FactType",
            "name": "HavenHeeftJutter",
            "reciprocal": false,
            "roles": [
              {"node": "Role", "name": "haven", "objectType": "Haven", "many": false},
              {
                "node": "Role",
                "name": "havenjutter",
                "objectType": "Jutter",
                "plural": "havenjutters",
                "many": true
              }
            ]
          },
          {
            "node": "FactType",
            "name": "HavenHeeftPremiepot",
            "reciprocal": false,
            "roles": [
              {"node": "Role", "name": "premiehaven", "objectType": "Haven", "many": false},
              {"node": "Role", "name": "premiepot", "objectType": "Premiepot", "many": false}
            ]
          },
          {
            "node": "FactType",
            "name": "PremiepotVoorJutters",
            "reciprocal": false,
            "roles": [
              {
                "node": "Role",
                "name": "verdeelde premiepot",
                "objectType": "Premiepot",
                "many": false
              },
              {
                "node": "Role",
                "name": "premiejutter",
                "objectType": "Jutter",
                "plural": "premiejutters",
                "many": true
              }
            ]
          }
        ],
        "parameters": [],
        "domains": [],
        "unitSystems": [],
        "dimensions": [],
        "timelines": [],
        "dayKinds": []
      },
      "rules": {
        "rules": [
          {
            "node": "Rule",
            "name": "premiegerechtigd",
            "versions": [
              {
                "validity": {"node": "Validity"},
                "target": {
                  "node": "AttributeReference",
                  "attribute": "premiegerechtigd",
                  "subjectType": "Jutter"
                },
                "value": {
                  "node": "Literal",
                  "value": {"boolean": true},
                  "type": {"node": "Boolean"}
                },
                "condition": {
                  "node": "RoleMembership",
                  "factType": "PremiepotVoorJutters",
                  "role": "premiejutter",
                  "negated": false
                },
                "initialisation": false
              }
            ]
          }
        ],
        "dayKindDefinitions": [],
        "objectCreations": [
          {
            "node": "ObjectCreation",
            "name": "premiepot",
            "versions": [
              {
                "createdType": "Premiepot",
                "subject": "Haven",
                "initialisations": [
                  {
                    "node": "MemberInit",
                    "member": "pot",
                    "value": {
                      "node": "Aggregation",
                      "kind": "count",
                      "arguments": [
                        {
                          "node": "AttributeReference",
                          "attribute": "havenjutters",
                          "subjectType": "Haven"
                        }
                      ],
                      "elementType": "Jutter",
                      "sourceRole": {
                        "node": "RoleBinding",
                        "factType": "HavenHeeftJutter",
                        "role": "havenjutter"
                      },
                      "type": {
                        "node": "Number",
                        "whole": true,
                        "decimals": 0,
                        "sign": "nonNegative"
                      }
                    },
                    "characteristic": false
                  }
                ],
                "validity": {"node": "Validity"},
                "connectingRole": {
                  "node": "RoleBinding",
                  "factType": "HavenHeeftPremiepot",
                  "role": "premiepot"
                }
              }
            ]
          }
        ],
        "allocations": [],
        "consistencies": [],
        "uniquenesses": [],
        "factCreations": [
          {
            "node": "FactCreation",
            "name": "premiejutters",
            "versions": [
              {
                "factType": "PremiepotVoorJutters",
                "anchorRole": "verdeelde premiepot",
                "targetRole": "premiejutter",
                "anchor": "Premiepot",
                "path": [
                  {"node": "RoleBinding", "factType": "HavenHeeftPremiepot", "role": "premiehaven"},
                  {"node": "RoleBinding", "factType": "HavenHeeftJutter", "role": "havenjutter"}
                ],
                "validity": {"node": "Validity"}
              }
            ]
          }
        ],
        "startPointDeterminations": [],
        "ruleGroups": []
      },
      "diagnostics": []
    }
|}
  and case_data =
    {|
    {
      "format": "uchar-case-v0",
      "asOfDate": null,
      "input": {
        "instances": [
          {
            "label": "H1",
            "objectType": "Haven",
            "attributes": {},
            "roles": {},
            "characteristics": []
          },
          {
            "label": "J1",
            "objectType": "Jutter",
            "attributes": {},
            "roles": {"haven": ["H1"]},
            "characteristics": []
          },
          {
            "label": "J2",
            "objectType": "Jutter",
            "attributes": {},
            "roles": {"haven": ["H1"]},
            "characteristics": []
          },
          {
            "label": "J3",
            "objectType": "Jutter",
            "attributes": {},
            "roles": {},
            "characteristics": []
          }
        ],
        "parameters": {}
      },
      "expected": [
        {"kind": "characteristicPresent", "label": "J1", "characteristic": "premiegerechtigd"},
        {"kind": "characteristicPresent", "label": "J2", "characteristic": "premiegerechtigd"},
        {"kind": "characteristicAbsent", "label": "J3", "characteristic": "premiegerechtigd"}
      ]
    }
|}
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv64h" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" ir and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      check "WV-6.4-h-verifier-schoon"
        (Ir_v0.Checks.all (Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier irf) = []);
      let d = Ir_v0.Loader.load_file irf in
      let c = Case.load_file d casef in
      let final =
        Eval.evaluate
          { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
            locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
          c.world
      in
      check "WV-6.4-h-rolcheck-na-de-feitcreatie" (Case.check c final = []))

(* --- ch. 5.5: generations and the generation budget ---------------------- *)

(* The document and the case as files, loaded the way the runner loads them.
   [f] gets the verifier's findings, the case and the evaluation, deferred so
   that a fault can be caught where it is pinned. [world_state] changes the input
   world before the evaluation, for a state no case can write. *)
let with_files ?(world_state = Fun.id) ~ir ~case_data f =
  let write suffix body_text =
    let file = Filename.temp_file "ref-wv55" suffix in
    let oc = open_out file in
    output_string oc body_text;
    close_out oc;
    file
  in
  let irf = write ".ir.json" ir and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      let findings = Ir_v0.Checks.all (Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier irf) in
      let d = Ir_v0.Loader.load_file irf in
      let c = Case.load_file d casef in
      f findings c (fun () ->
          Eval.evaluate
            { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
              locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
            (world_state c.world)))

(* verifier-clean, evaluated without a fault, every expectation of the case
   met, then [further] on the end state. A fault or a mismatch is carried in
   the example's name, so a red run says what came out. *)
let plays ~ident ~ir ~case_data further =
  with_files ~ir ~case_data (fun findings c evalueer ->
      check (ident ^ ": verifier-clean") (findings = []);
      match evalueer () with
      | exception Value.Fault { text = m; _ } -> check (ident ^ " (fault: " ^ m ^ ")") false
      | final ->
          let mismatches = Case.check c final in
          check
            (ident ^ match mismatches with [] -> "" | f -> " (" ^ String.concat "; " f ^ ")")
            (mismatches = []);
          further final)

(* every order of a list of named rules *)
let rec orders = function
  | [] -> [ [] ]
  | rule_defs ->
      List.concat_map
        (fun (ident, rule_def) ->
          List.map
            (fun rest -> (ident, rule_def) :: rest)
            (orders (List.filter (fun (n, _) -> n <> ident) rule_defs)))
        rule_defs

let attribute_of (final : World.t) label attr =
  match World.SMap.find_opt label final.instances with
  | Some i -> World.attr i attr
  | None -> Value.Text ("<no instance " ^ label ^ ">")

let number_json = {|{"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}|}

(* WV-5.5-generaties (ch. 5.5, since 2026-10-09): a creation reads the
   settled world of the generation before its own, so the value its
   initialisation takes at the moment of making does not depend on where the
   document writes the rules. "werf de opvolger" (recursive group
   "estafette") gives a Wachter of rang below 4 an opvolger with rang plus 1
   and beginstand equal to its stand; "stand van een wachter" counts its
   opvolgers; "opvolgerbegin" reads the beginstand of its opvolger. The copy
   variant adds "De standkopie van een Wachter moet gesteld worden op zijn
   stand" and initialises the beginstand from the standkopie instead. W1
   (rang 1) makes W2, W2 makes W3, W3 makes W4. When W1 makes its opvolger it
   has none, so its stand is 0; afterwards it is 1. In every order of the
   rules: stand(W1) = 1, opvolgerbegin(W1) = 0, each minted Wachter starts at
   beginstand 0, and the chain has four Wachters. Until that date a creation
   read the running world of a pass, so the moment of making was a position
   in the pass: with the copy rule written before the count, the
   initialisation read a copy the pass had not yet computed and
   opvolgerbegin(W1) was leeg. *)
let () =
  let owned ident =
    Printf.sprintf
      {|{"node": "AttributeReference", "attribute": "%s", "subjectType": "Wachter", "source": {"snippet": "zijn %s"}}|}
      ident ident
  in
  let rule_def ident objective presentation_kind value_node =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "Wachter", "source": {"snippet": "De %s van een Wachter"}}, "value": %s, "initialisation": false, "presentation": "%s"}]}|}
        ident objective objective value_node presentation_kind )
  in
  let state =
    rule_def "stand van een wachter" "stand" "calculation"
      {|{"node": "Aggregation", "kind": "count", "arguments": [{"node": "AttributeReference", "attribute": "opvolgers", "subjectType": "Wachter", "source": {"snippet": "opvolgers van de Wachter"}}], "elementType": "Wachter", "sourceRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative"}, "source": {"snippet": "het aantal opvolgers van de Wachter"}}|}
  in
  let opvolgerbegin =
    rule_def "opvolgerbegin" "opvolgerbegin" "equation"
      {|{"node": "AttributeReference", "attribute": "beginstand", "subjectType": "Wachter", "source": {"snippet": "de beginstand van zijn opvolger"}, "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}]}|}
  in
  let state_copy = rule_def "standkopie van een wachter" "standkopie" "equation" (owned "stand") in
  let doc ~was_read rule_defs =
    let member_list = [ "rang"; "stand"; "beginstand"; "opvolgerbegin" ] @ if was_read = "stand" then [] else [ was_read ] in
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [%s], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "plural": "opvolgers", "many": false}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "werf de opvolger", "versions": [{"createdType": "Wachter", "subject": "Wachter", "initialisations": [{"node": "MemberInit", "member": "rang", "value": {"node": "Binary", "op": "plus", "left": %s, "right": {"node": "Literal", "value": {"number": "1"}, "type": %s, "source": {"snippet": "1"}}, "type": %s, "source": {"snippet": "plus"}}, "characteristic": false}, {"node": "MemberInit", "member": "beginstand", "value": %s, "characteristic": false}], "condition": {"node": "Comparison", "op": "less", "left": %s, "right": {"node": "Literal", "value": {"number": "4"}, "type": %s, "source": {"snippet": "4"}}, "source": {"snippet": "less"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}}]}], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "estafette", "recursive": true, "members": ["werf de opvolger"], "source": {"snippet": "estafette"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
      (String.concat ", "
         (List.map
            (fun one_member -> Printf.sprintf {|{"node": "Attribute", "name": "%s", "datatype": %s}|} one_member number_json)
            member_list))
      (String.concat ", " (List.map snd rule_defs))
      (owned "rang") number_json number_json (owned was_read) (owned "rang") number_json
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W1", "objectType": "Wachter", "attributes": {"rang": {"number": "1"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "stand", "value": {"number": "1"}}, {"kind": "attributeValue", "label": "W1", "attribute": "opvolgerbegin", "value": {"number": "0"}}]}|}
  in
  List.iter
    (fun (variant, was_read, rule_defs) ->
      List.iter
        (fun ranking_order ->
          let ident =
            Printf.sprintf "WV-5.5-generaties-%s [%s]" variant (String.concat "; " (List.map fst ranking_order))
          in
          plays ~ident ~ir:(doc ~was_read ranking_order) ~case_data (fun final ->
              check (ident ^ ": four Wachters") (World.SMap.cardinal final.instances = 4);
              List.iter
                (fun label ->
                  expect_num
                    (Printf.sprintf "%s: beginstand(%s) = 0" ident label)
                    (attribute_of final label "beginstand") "0")
                [ "oc0::W1"; "oc0::oc0::W1"; "oc0::oc0::oc0::W1" ]))
        (orders rule_defs))
    [ ("keten", "stand", [ state; opvolgerbegin ]);
      ("kopie", "standkopie", [ state_copy; state; opvolgerbegin ]) ]

(* WV-5.5-generaties-feitcreatie (ch. 5.5, since 2026-10-09): a fact
   creation belongs to the generation like an object creation, and no
   creation reads what another creation of its own generation links. "maak
   maten" (in group "estafette" beside "werf de opvolger") links a Wachter as
   maatgever to its opvolger as maat; "werf de opvolger" gives a Wachter of
   rang below 4 an opvolger with rang plus 1 and beginstand equal to the
   number of its maatgevers; "opvolgerbegin" reads the beginstand of the
   opvolger and "verderbegin" the opvolgerbegin of the opvolger. W1 (rang 1)
   makes W2 in generation 1. In generation 2 "maak maten" links W1 to W2 and
   W2 makes W3, both against the world of generation 1, where W2 has no
   maatgever: W3 starts at beginstand 0, and so does W4 in generation 3. In
   both orders of the rules: opvolgerbegin(W1) = 0, verderbegin(W1) = 0 and
   each minted Wachter at beginstand 0. Until that date a pass ran the fact
   creations before the object creations, so the initialisation counted the
   link of its own pass: beginstand 1 for W3 and W4, verderbegin(W1) = 1. *)
let () =
  let rule_def ident objective value_node =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "Wachter", "source": {"snippet": "De %s van een Wachter"}}, "value": {"node": "AttributeReference", "attribute": "%s", "subjectType": "Wachter", "source": {"snippet": "de %s van zijn opvolger"}, "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}]}, "initialisation": false, "presentation": "equation"}]}|}
        ident objective objective value_node value_node )
  in
  let rule_defs =
    [ rule_def "opvolgerbegin" "opvolgerbegin" "beginstand"; rule_def "verderbegin" "verderbegin" "opvolgerbegin" ]
  in
  let doc rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [%s], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "plural": "opvolgers", "many": false}]}, {"node": "FactType", "name": "maatschap", "reciprocal": false, "roles": [{"node": "Role", "name": "maatgever", "objectType": "Wachter", "plural": "maatgevers", "many": true}, {"node": "Role", "name": "maat", "objectType": "Wachter", "plural": "maten", "many": true}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "werf de opvolger", "versions": [{"createdType": "Wachter", "subject": "Wachter", "initialisations": [{"node": "MemberInit", "member": "rang", "value": {"node": "Binary", "op": "plus", "left": {"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}, "right": {"node": "Literal", "value": {"number": "1"}, "type": %s, "source": {"snippet": "1"}}, "type": %s, "source": {"snippet": "plus"}}, "characteristic": false}, {"node": "MemberInit", "member": "beginstand", "value": {"node": "Aggregation", "kind": "count", "arguments": [{"node": "AttributeReference", "attribute": "maatgevers", "subjectType": "Wachter", "source": {"snippet": "maatgevers van de Wachter"}}], "elementType": "Wachter", "sourceRole": {"node": "RoleBinding", "factType": "maatschap", "role": "maatgever"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative"}, "source": {"snippet": "het aantal maatgevers van de Wachter"}}, "characteristic": false}], "condition": {"node": "Comparison", "op": "less", "left": {"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}, "right": {"node": "Literal", "value": {"number": "4"}, "type": %s, "source": {"snippet": "4"}}, "source": {"snippet": "less"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}}]}], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [{"node": "FactCreation", "name": "maak maten", "versions": [{"factType": "maatschap", "anchorRole": "maatgever", "targetRole": "maat", "anchor": "Wachter", "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}], "validity": {"node": "Validity"}}]}], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "estafette", "recursive": true, "members": ["werf de opvolger", "maak maten"], "source": {"snippet": "estafette"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
      (String.concat ", "
         (List.map
            (fun one_member -> Printf.sprintf {|{"node": "Attribute", "name": "%s", "datatype": %s}|} one_member number_json)
            [ "rang"; "beginstand"; "opvolgerbegin"; "verderbegin" ]))
      (String.concat ", " (List.map snd rule_defs))
      number_json number_json number_json
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W1", "objectType": "Wachter", "attributes": {"rang": {"number": "1"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "opvolgerbegin", "value": {"number": "0"}}, {"kind": "attributeValue", "label": "W1", "attribute": "verderbegin", "value": {"number": "0"}}]}|}
  in
  List.iter
    (fun ranking_order ->
      let ident =
        Printf.sprintf "WV-5.5-generaties-feitcreatie [%s]" (String.concat "; " (List.map fst ranking_order))
      in
      plays ~ident ~ir:(doc ranking_order) ~case_data (fun final ->
          List.iter
            (fun label ->
              expect_num
                (Printf.sprintf "%s: beginstand(%s) = 0" ident label)
                (attribute_of final label "beginstand") "0")
            [ "oc0::W1"; "oc0::oc0::W1"; "oc0::oc0::oc0::W1" ]))
    (orders rule_defs)

(* WV-5.5-budget-grens (ch. 5.5, since 2026-10-09): the creation budget —
   a mint at creation depth 101 in its stratum is the fault — counts the
   chain of mints, not passes, so the boundary does not depend on the order
   of the rules that make nothing. "werf de opvolger" (group "estafette")
   gives Wachters of rang below 52 an opvolger with rang plus 1; "leg de
   volgende boei" (group "boeienlijn") gives a Boei whose nummer is below the
   bound a volgende boei with nummer plus 1 and a wachterrang read from the
   rang of its wachter, which puts that chain a stratum higher. "kopie van
   een boei" copies the nummer, "kopie2 van een boei" the kopie. W1 (rang 1)
   guards B1 (nummer 1). Bound 100: B100 at depth 99; bound 101: B101 at
   depth 100, the last the budget admits; bound 102: B102 would be the mint
   at depth 101, the fault. Every generation of the Boeien mints one Boei on
   the last, so a Boei's depth is the generation that minted it, and the
   bound sits where the generation budget of the same day put it. In both
   orders of the two copy rules: 100 and 101 Boeien, kopie2 of the last Boei
   equal to its nummer, and the fault "creation budget exhausted". Until
   that date the budget was 100 passes of the whole stratum, and the copy
   rules settle in one pass more when the reader of kopie is written first:
   with that order bound 100 faulted (no fixpoint within 100 passes) and the
   other order played. *)
let () =
  let boei ident =
    Printf.sprintf
      {|{"node": "AttributeReference", "attribute": "%s", "subjectType": "Boei", "source": {"snippet": "zijn %s"}}|}
      ident ident
  in
  let copy_rule ident objective source_name =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "Boei", "source": {"snippet": "De %s van een Boei"}}, "value": %s, "initialisation": false, "presentation": "equation"}]}|}
        ident objective objective (boei source_name) )
  in
  let copy = copy_rule "kopie van een boei" "kopie" "nummer"
  and copy_2 = copy_rule "kopie2 van een boei" "kopie2" "kopie" in
  let literal n =
    Printf.sprintf {|{"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}|} n
      number_json n
  in
  let one_member ident = Printf.sprintf {|{"node": "Attribute", "name": "%s", "datatype": %s}|} ident number_json in
  let wachter_rank =
    {|{"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}|}
  in
  let doc ~boundary rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [%s], "characteristics": []}, {"node": "ObjectType", "name": "Boei", "animate": true, "plural": "Boeis", "attributes": [%s], "characteristics": [{"node": "Characteristic", "name": "genummerd", "kind": "adjectival"}]}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "many": false}]}, {"node": "FactType", "name": "boeiketen", "reciprocal": false, "roles": [{"node": "Role", "name": "vorige boei", "objectType": "Boei", "many": false}, {"node": "Role", "name": "volgende boei", "objectType": "Boei", "many": false}]}, {"node": "FactType", "name": "bewaking", "reciprocal": false, "roles": [{"node": "Role", "name": "wachter", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "boei", "objectType": "Boei", "many": false}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "werf de opvolger", "versions": [{"createdType": "Wachter", "subject": "Wachter", "initialisations": [{"node": "MemberInit", "member": "rang", "value": {"node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s, "source": {"snippet": "plus"}}, "characteristic": false}], "condition": {"node": "Comparison", "op": "less", "left": %s, "right": %s, "source": {"snippet": "less"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}}]}, {"node": "ObjectCreation", "name": "leg de volgende boei", "versions": [{"createdType": "Boei", "subject": "Boei", "initialisations": [{"node": "MemberInit", "member": "nummer", "value": {"node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s, "source": {"snippet": "plus"}}, "characteristic": false}, {"node": "MemberInit", "member": "wachterrang", "value": {"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "de rang van zijn wachter"}, "path": [{"node": "RoleBinding", "factType": "bewaking", "role": "wachter"}]}, "characteristic": false}], "condition": {"node": "Comparison", "op": "less", "left": %s, "right": %s, "source": {"snippet": "less"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "boeiketen", "role": "volgende boei"}}]}], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "estafette", "recursive": true, "members": ["werf de opvolger"], "source": {"snippet": "estafette"}}, {"node": "RuleGroup", "name": "boeienlijn", "recursive": true, "members": ["leg de volgende boei"], "source": {"snippet": "boeienlijn"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
      (one_member "rang")
      (String.concat ", " (List.map one_member [ "nummer"; "wachterrang"; "kopie"; "kopie2" ]))
      (String.concat ", " (List.map snd rule_defs))
      wachter_rank (literal 1) number_json wachter_rank (literal 52)
      (boei "nummer") (literal 1) number_json (boei "nummer") (literal boundary)
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W1", "objectType": "Wachter", "attributes": {"rang": {"number": "1"}}, "roles": {"boei": ["B1"]}, "characteristics": []}, {"label": "B1", "objectType": "Boei", "attributes": {"nummer": {"number": "1"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "rang", "value": {"number": "1"}}, {"kind": "attributeValue", "label": "B1", "attribute": "kopie", "value": {"number": "1"}}, {"kind": "attributeValue", "label": "B1", "attribute": "kopie2", "value": {"number": "1"}}]}|}
  in
  List.iter
    (fun boundary ->
      List.iter
        (fun ranking_order ->
          let ident =
            Printf.sprintf "WV-5.5-budget-grens-%d-generaties [%s]" (boundary - 1)
              (String.concat "; " (List.map fst ranking_order))
          in
          let ir = doc ~boundary ranking_order in
          if boundary - 1 <= 100 then
            plays ~ident ~ir ~case_data (fun final ->
                let boeien =
                  World.SMap.fold
                    (fun _ (i : World.instance) n -> if i.otype = "Boei" then n + 1 else n)
                    final.instances 0
                in
                check (Printf.sprintf "%s: %d Boeien (got %d)" ident boundary boeien) (boeien = boundary);
                let last_item = String.concat "" (List.init (boundary - 1) (fun _ -> "oc1::")) ^ "B1" in
                expect_num (ident ^ ": copy2 of the last Boei") (attribute_of final last_item "kopie2")
                  (string_of_int boundary))
          else
            with_files ~ir ~case_data (fun findings _ evalueer ->
                check (ident ^ ": verifier-clean") (findings = []);
                expect_fault_msg ident evalueer
                  "creation budget exhausted: object creation \"leg de volgende boei\" of stratum 1 \
                   would mint at creation depth 101"))
        (orders [ copy_2; copy ]))
    [ 100; 101; 102 ]

(* WV-5.5-budget-erflijn (ch. 5.5, since 2026-10-09): a fact creation
   carries no creation budget. It mints nothing, and over the finite
   universe a stratum's mints leave the links are finite, so a closure of
   any length completes. "erf de directe opvolger" links every Wachter as
   erflater to its opvolger; "erf de lijn door" (recursive group
   "erfestafette") links an erflater to the opvolger of each of its
   erfvolgers, a stratum higher, since a creation reads the erflijn
   strictly. Over a chain of 103 Wachters W1 → … → W103 the link W1 → W103
   is 102 hops long and comes in the 101st generation of that stratum:
   erfvolgeraantal(W1) = 102 and the channel stays empty. Until that date a
   stratum admitted 100 generations that mint or link, and this document
   faulted ("no fixpoint within 100 generations — generation 101 still
   mints or links (fact creation "erf de lijn door")"), where the ASP
   backend played it. The anchor jutterswet-erflijn is the same document
   over four Wachters. *)
let () =
  let ir =
    {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [{"node": "Attribute", "name": "erfvolgeraantal", "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}}], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "plural": "voorgangers", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "plural": "opvolgers", "many": false}]}, {"node": "FactType", "name": "erflijn", "reciprocal": false, "roles": [{"node": "Role", "name": "erflater", "objectType": "Wachter", "plural": "erflaters", "many": true}, {"node": "Role", "name": "erfvolger", "objectType": "Wachter", "plural": "erfvolgers", "many": true}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [{"node": "Rule", "name": "erfvolgeraantal van een wachter", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "erfvolgeraantal", "subjectType": "Wachter", "source": {"snippet": "Het erfvolgeraantal van een Wachter"}}, "value": {"node": "Aggregation", "kind": "count", "arguments": [{"node": "AttributeReference", "attribute": "erfvolgers", "subjectType": "Wachter", "source": {"snippet": "alle erfvolgers van de Wachter"}}], "elementType": "Wachter", "sourceRole": {"node": "RoleBinding", "factType": "erflijn", "role": "erfvolger"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}, "source": {"snippet": "het aantal erfvolgers van de Wachter"}}, "initialisation": false, "presentation": "equation"}]}], "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [{"node": "FactCreation", "name": "erf de directe opvolger", "versions": [{"factType": "erflijn", "anchorRole": "erflater", "targetRole": "erfvolger", "anchor": "Wachter", "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}], "validity": {"node": "Validity"}}]}, {"node": "FactCreation", "name": "erf de lijn door", "versions": [{"factType": "erflijn", "anchorRole": "erflater", "targetRole": "erfvolger", "anchor": "Wachter", "path": [{"node": "RoleBinding", "factType": "erflijn", "role": "erfvolger"}, {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}], "validity": {"node": "Validity"}}]}], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "erfestafette", "recursive": true, "members": ["erf de lijn door"]}]}, "diagnostics": []}|}
  in
  let n = 103 in
  let wachter i =
    Printf.sprintf
      {|{"label": "W%d", "objectType": "Wachter", "attributes": {}, "roles": {%s}, "characteristics": []}|}
      i
      (if i < n then Printf.sprintf {|"opvolger": ["W%d"]|} (i + 1) else "")
  in
  let case_data =
    Printf.sprintf
      {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [%s], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "erfvolgeraantal", "value": {"number": "%d"}}, {"kind": "consistent"}]}|}
      (String.concat ", " (List.init n (fun i -> wachter (i + 1))))
      (n - 1)
  in
  plays ~ident:"WV-5.5-budget-erflijn-102-hops" ~ir ~case_data (fun _ -> ())

(* WV-5.5-budget-wacht-op-link (ch. 5.5, since 2026-10-09): the budget
   counts the depth of a chain of mints, not the generations it takes; a
   creation whose condition waits for a link keeps its subject's depth.
   "werf de opvolger" gives a Wachter whose rang is below N and who has a
   maatgever an opvolger with rang plus 1; "maak maten" links every Wachter
   as maatgever to its opvolger; both are members of the recursive group
   "estafette", so they share one stratum. The consistency "rangdetector"
   holds a rang below N. W1 (rang 1) has the maatgever W0 (rang 0) in the
   input and mints W2 in generation 1; W2 gets its maatgever W1 in
   generation 2 and mints W3 in generation 3, so W(k + 1) comes in
   generation 2k - 1 at creation depth k. N = 101: W101 at depth 100, in
   generation 199 — the chain plays, 102 Wachters, the last of rang 101, and
   the rangdetector meets W101 alone. N = 102: W102 would be the mint at
   depth 101, the fault. Until that date the budget counted 100 generations
   per stratum, and this chain faulted from N = 52 on (W52 in generation
   101, at depth 51), where the ASP backend, which counted the depth,
   played it to N = 101. *)
let () =
  let number_value n =
    Printf.sprintf {|{"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}|} n
      number_json n
  in
  let ranking = {|{"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}|} in
  let doc boundary =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [{"node": "Attribute", "name": "rang", "datatype": %s}], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "plural": "opvolgers", "many": false}]}, {"node": "FactType", "name": "maatschap", "reciprocal": false, "roles": [{"node": "Role", "name": "maatgever", "objectType": "Wachter", "plural": "maatgevers", "many": true}, {"node": "Role", "name": "maat", "objectType": "Wachter", "plural": "maten", "many": true}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "werf de opvolger", "versions": [{"createdType": "Wachter", "subject": "Wachter", "initialisations": [{"node": "MemberInit", "member": "rang", "value": {"node": "Binary", "op": "plus", "left": %s, "right": %s, "type": %s, "source": {"snippet": "plus"}}, "characteristic": false}], "condition": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [{"node": "Comparison", "op": "less", "left": %s, "right": %s, "source": {"snippet": "less"}}, {"node": "Comparison", "op": "greater", "left": {"node": "Aggregation", "kind": "count", "arguments": [{"node": "AttributeReference", "attribute": "maatgevers", "subjectType": "Wachter", "source": {"snippet": "maatgevers van de Wachter"}}], "elementType": "Wachter", "sourceRole": {"node": "RoleBinding", "factType": "maatschap", "role": "maatgever"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative"}, "source": {"snippet": "het aantal maatgevers van de Wachter"}}, "right": %s, "source": {"snippet": "greater"}}], "source": {"snippet": "aan alle volgende voorwaarden"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}}]}], "allocations": [], "consistencies": [{"node": "Consistency", "name": "rangdetector", "versions": [{"subject": "Wachter", "assertion": {"node": "Comparison", "op": "less", "left": %s, "right": %s, "source": {"snippet": "de rang van de Wachter is kleiner dan %d"}}, "validity": {"node": "Validity"}, "source": {"snippet": "rangdetector"}}]}], "uniquenesses": [], "factCreations": [{"node": "FactCreation", "name": "maak maten", "versions": [{"factType": "maatschap", "anchorRole": "maatgever", "targetRole": "maat", "anchor": "Wachter", "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}], "validity": {"node": "Validity"}}]}], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "estafette", "recursive": true, "members": ["werf de opvolger", "maak maten"], "source": {"snippet": "estafette"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
      number_json ranking (number_value 1) number_json ranking (number_value boundary) (number_value 0) ranking (number_value boundary) boundary
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W0", "objectType": "Wachter", "attributes": {"rang": {"number": "0"}}, "roles": {}, "characteristics": []}, {"label": "W1", "objectType": "Wachter", "attributes": {"rang": {"number": "1"}}, "roles": {"maatgever": ["W0"]}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "rang", "value": {"number": "1"}}, {"kind": "inconsistent", "rule": "rangdetector"}]}|}
  in
  plays ~ident:"WV-5.5-budget-wacht-op-link-101" ~ir:(doc 101) ~case_data (fun final ->
      check "WV-5.5-budget-wacht-op-link-101: 102 Wachters" (World.SMap.cardinal final.instances = 102);
      let last_item = String.concat "" (List.init 100 (fun _ -> "oc0::")) ^ "W1" in
      expect_num "WV-5.5-budget-wacht-op-link-101: the rang of the Wachter at depth 100"
        (attribute_of final last_item "rang") "101";
      check "WV-5.5-budget-wacht-op-link-101: the rangdetector meets the last Wachter alone"
        (List.map (fun (f : World.error_fact) -> (f.rule, f.subject)) final.channel = [ ("rangdetector", last_item) ]));
  with_files ~ir:(doc 102) ~case_data (fun findings _ evalueer ->
      check "WV-5.5-budget-wacht-op-link-102: verifier-clean" (findings = []);
      expect_fault_msg "WV-5.5-budget-wacht-op-link-102" evalueer
        "creation budget exhausted: object creation \"werf de opvolger\" of stratum 0 would mint at \
         creation depth 101")

(* The invariant beside the creation budget (since 2026-10-09): a creation
   never meets a subject that a higher stratum minted. A creation reads its
   subject's population strictly (ch. 6.1), so it sits at or above every
   creation that makes its subjects, and a stratum is complete before a
   higher one starts (ch. 6.3). ref/ checks it on every subject a creation
   ranges over and fails loudly with the file's name, rather than count a
   depth from a stratum that has not run. The world below gives W1 a forged
   mint of stratum 1 and meets "werf de lange keten", the only creation, of
   stratum 0. *)
let () =
  let ir =
    {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [{"node": "Attribute", "name": "rang", "datatype": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}}], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "many": false}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "werf de lange keten", "versions": [{"createdType": "Wachter", "subject": "Wachter", "initialisations": [{"node": "MemberInit", "member": "rang", "value": {"node": "Binary", "op": "plus", "left": {"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}, "right": {"node": "Literal", "value": {"number": "1"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}, "source": {"snippet": "1"}}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}, "source": {"snippet": "plus"}}, "characteristic": false}], "condition": {"node": "Comparison", "op": "less", "left": {"node": "AttributeReference", "attribute": "rang", "subjectType": "Wachter", "source": {"snippet": "zijn rang"}}, "right": {"node": "Literal", "value": {"number": "5"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"}, "source": {"snippet": "5"}}, "source": {"snippet": "less"}}, "validity": {"node": "Validity"}, "connectingRole": {"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}}]}], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "lange estafette", "recursive": true, "members": ["werf de lange keten"], "source": {"snippet": "lange estafette"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "W1", "objectType": "Wachter", "attributes": {"rang": {"number": "1"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "W1", "attribute": "rang", "value": {"number": "1"}}]}|}
  in
  let ident = "creation budget invariant: a subject a higher stratum minted" in
  with_files
    ~world_state:(fun w -> { w with World.mints = World.SMap.add "W1" (1, 1) w.World.mints })
    ~ir ~case_data
    (fun findings _ evalueer ->
      check (ident ^ ": verifier-clean") (findings = []);
      match evalueer () with
      | exception Failure m ->
          check (ident ^ " (" ^ m ^ ")") (substring m "ref/lib/eval.ml, run_object_creation")
      | exception Value.Fault { text = m; _ } -> check (ident ^ " (an evaluation fault: " ^ m ^ ")") false
      | _ -> check (ident ^ " (no failure)") false)

(* WV-5.5-budget-regelketen (since 2026-10-09): the forms that make nothing
   carry no budget of their own. Without creations their graph is acyclic, so
   unless two writers of one target keep overwriting each other they settle
   within as many passes as there are of them, and ref bounds their loop at the
   larger of 100 and that number plus one, which only turns those two writers
   (ch. 5.7) into a fault. A Ding with an input a0 = 0 and 105 rules "a<i> van
   een ding": a<i> = a<i-1> + 1. Written from a105 down to a1, each pass
   settles one more link of the chain, and the 105th pass the last; written
   from a1 up, one pass settles all. a105 = 105 in both orders. Until that
   date the bound was the budget of 100 passes, and the first order faulted
   (no fixpoint within 100 passes). *)
let () =
  let n = 105 in
  let one_member i = Printf.sprintf {|{"node": "Attribute", "name": "a%d", "datatype": %s}|} i number_json in
  let rule_def i =
    Printf.sprintf
      {|{"node": "Rule", "name": "a%d van een ding", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "a%d", "subjectType": "Ding", "source": {"snippet": "De a%d van een Ding"}}, "value": {"node": "Binary", "op": "plus", "left": {"node": "AttributeReference", "attribute": "a%d", "subjectType": "Ding", "source": {"snippet": "zijn a%d"}}, "right": {"node": "Literal", "value": {"number": "1"}, "type": %s, "source": {"snippet": "1"}}, "type": %s, "source": {"snippet": "plus"}}, "initialisation": false, "presentation": "calculation"}]}|}
      i i i (i - 1) (i - 1) number_json number_json
  in
  let doc ranking_order =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Ding", "animate": false, "plural": "Dingen", "attributes": [%s], "characteristics": []}], "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [], "prioritizedDefaults": []}, "diagnostics": []}|}
      (String.concat ", " (List.init (n + 1) one_member))
      (String.concat ", " (List.map rule_def ranking_order))
  in
  let case_data =
    Printf.sprintf
      {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "D", "objectType": "Ding", "attributes": {"a0": {"number": "0"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "D", "attribute": "a%d", "value": {"number": "%d"}}]}|}
      n n
  in
  let op = List.init n (fun i -> i + 1) in
  List.iter
    (fun (direction, ranking_order) ->
      plays ~ident:(Printf.sprintf "WV-5.5-budget-regelketen-%d [%s]" n direction) ~ir:(doc ranking_order) ~case_data
        (fun _ -> ()))
    [ ("van a105 naar a1", List.rev op); ("van a1 naar a105", op) ]

(* WV-6.3-na-een-creatie (ch. 6.3, since 2026-10-09): the forms of stratum
   <= k reach their fixpoint before any form of stratum k + 1 fires, and that
   holds again on what a creation makes. "post voor een open haven" gives a
   Haven that is open a Post; "y van een haven" writes y = 1 and "open haven"
   marks a Haven whose y is groter dan 0; "a van een post" writes a = 5;
   "gemarkeerde post" marks a Post whose a is ongelijk aan 5, a read the
   stratification puts above the writer of a; "bewaakte haven" marks a Haven
   whose post is gemarkeerd. H1 without input. In both orders of the reader
   of a and its writer: H1 open, one Post with a = 5 that is not gemarkeerd,
   and H1 not bewaakt. Until that date the forms of every stratum up to the
   creation's ran together in one pass in document order, so with the reader
   first "gemarkeerde post" read a on the new Post before "a van een post"
   wrote it, took "leeg ongelijk aan 5" for true (ch. 1.2) and marked it for
   good: H1 bewaakt. *)
let () =
  let characteristic_rule ident objective ot precondition =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": {"snippet": "%s"}}, "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": {"snippet": "waar"}}, "condition": %s, "initialisation": false}]}|}
        ident objective ot objective precondition )
  in
  let value_rule ident objective ot n =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": {"snippet": "%s"}}, "value": {"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}, "initialisation": false, "presentation": "equation"}]}|}
        ident objective ot objective n number_json n )
  in
  let comparison op attr ot n =
    Printf.sprintf
      {|{"node": "Comparison", "op": "%s", "left": {"node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": {"snippet": "zijn %s"}}, "right": {"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}, "source": {"snippet": "%s %s %d"}}|}
      op attr ot attr n number_json n attr op n
  in
  let marked = characteristic_rule "gemarkeerde post" "gemarkeerd" "Post" (comparison "notEqual" "a" "Post" 5)
  and y = value_rule "y van een haven" "y" "Haven" 1
  and open_ = characteristic_rule "open haven" "open" "Haven" (comparison "greater" "y" "Haven" 0)
  and a = value_rule "a van een post" "a" "Post" 5
  and bewaakt =
    characteristic_rule "bewaakte haven" "bewaakt" "Haven"
      {|{"node": "CharacteristicCheck", "characteristic": "gemarkeerd", "negated": false, "source": {"snippet": "zijn post gemarkeerd is"}, "navigation": {"node": "RoleBinding", "factType": "havenpost", "role": "post"}}|}
  in
  let doc rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Haven", "animate": false, "plural": "Havens", "attributes": [{"node": "Attribute", "name": "y", "datatype": %s}], "characteristics": [{"node": "Characteristic", "name": "open", "kind": "adjectival"}, {"node": "Characteristic", "name": "bewaakt", "kind": "adjectival"}]}, {"node": "ObjectType", "name": "Post", "animate": false, "plural": "Posten", "attributes": [{"node": "Attribute", "name": "a", "datatype": %s}], "characteristics": [{"node": "Characteristic", "name": "gemarkeerd", "kind": "adjectival"}]}], "factTypes": [{"node": "FactType", "name": "havenpost", "reciprocal": false, "roles": [{"node": "Role", "name": "haven", "objectType": "Haven", "many": false}, {"node": "Role", "name": "post", "objectType": "Post", "many": false}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "post voor een open haven", "versions": [{"createdType": "Post", "subject": "Haven", "initialisations": [], "condition": {"node": "CharacteristicCheck", "characteristic": "open", "negated": false, "source": {"snippet": "de Haven is open"}}, "validity": {"node": "Validity"}}]}], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [], "prioritizedDefaults": []}, "diagnostics": []}|}
      number_json number_json
      (String.concat ", " (List.map snd rule_defs))
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "H1", "objectType": "Haven", "attributes": {}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "characteristicPresent", "label": "H1", "characteristic": "open"}, {"kind": "characteristicAbsent", "label": "H1", "characteristic": "bewaakt"}]}|}
  in
  List.iter
    (fun (ranking_order, rule_defs) ->
      let ident = Printf.sprintf "WV-6.3-na-een-creatie [%s]" ranking_order in
      plays ~ident ~ir:(doc rule_defs) ~case_data (fun final ->
          let posten =
            World.SMap.fold
              (fun _ (i : World.instance) acc -> if i.otype = "Post" then i :: acc else acc)
              final.instances []
          in
          check (ident ^ ": one Post") (List.length posten = 1);
          expect_num (ident ^ ": a of the Post = 5") (attribute_of final "oc0::H1" "a") "5";
          check (ident ^ ": the Post is not marked")
            (List.for_all (fun i -> not (World.has_char i "gemarkeerd")) posten)))
    [ ("gemarkeerde post eerst", [ marked; y; open_; a; bewaakt ]);
      ("a van een post eerst", [ a; y; open_; marked; bewaakt ]) ]

(* WV-6.3-consistentie-na-een-creatie (ch. 6.3, since 2026-10-09): a
   consistency fires at the fixpoint of its stratum, and again after every
   later generation, on what a creation of a higher stratum makes. "y van een
   haven" writes y = 1 and "a van een post" a = 5 (stratum 0); "gesloten
   haven" marks a Haven whose y is groter dan 1 (stratum 1); "open haven"
   marks a Haven that is not gesloten (stratum 2, a negated read); "post voor
   een open haven" gives an open Haven a Post (stratum 2); the consistency
   "controle post" asserts that the a of a Post is kleiner dan 5 (stratum 1,
   a strict read of a). H1 without input. In both orders of the rules: H1
   open, one Post with a = 5, and one channel fact, "controle post" on that
   Post. Until that date the consistency fired once, at the fixpoint of
   stratum 1, before the creation of stratum 2 made the Post, and the Post
   was never checked: the channel stayed empty. The same holds for a
   uniqueness: with "eenmalige a" (the a of every Post is unique, stratum 1)
   in place of the consistency and the Havens H1 and H2, each open Haven gets
   a Post with a = 5, and both Posts carry a channel fact of "eenmalige a";
   until that date the channel stayed empty there as well. *)
let () =
  let rule_def ident objective ot value_node precondition presentation_kind =
    ( ident,
      Printf.sprintf
        {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "%s", "source": {"snippet": "%s"}}, "value": %s%s, "initialisation": false%s}]}|}
        ident objective ot objective value_node
        (match precondition with Some c -> {|, "condition": |} ^ c | None -> "")
        presentation_kind )
  in
  let number_value n =
    Printf.sprintf {|{"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}|} n
      number_json n
  in
  let truth = {|{"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": {"snippet": "waar"}}|} in
  let presentation_kind = {|, "presentation": "equation"|} in
  let y = rule_def "y van een haven" "y" "Haven" (number_value 1) None presentation_kind
  and a = rule_def "a van een post" "a" "Post" (number_value 5) None presentation_kind
  and sealed =
    rule_def "gesloten haven" "gesloten" "Haven" truth
      (Some
         (Printf.sprintf
            {|{"node": "Comparison", "op": "greater", "left": {"node": "AttributeReference", "attribute": "y", "subjectType": "Haven", "source": {"snippet": "zijn y"}}, "right": %s, "source": {"snippet": "y > 1"}}|}
            (number_value 1)))
      ""
  and open_ =
    rule_def "open haven" "open" "Haven" truth
      (Some
         {|{"node": "CharacteristicCheck", "characteristic": "gesloten", "negated": true, "source": {"snippet": "hij is niet gesloten"}}|})
      ""
  in
  let verification =
    Printf.sprintf
      {|{"node": "Consistency", "name": "controle post", "versions": [{"subject": "Post", "assertion": {"node": "Comparison", "op": "less", "left": {"node": "AttributeReference", "attribute": "a", "subjectType": "Post", "source": {"snippet": "zijn a"}}, "right": %s, "source": {"snippet": "zijn a is kleiner dan 5"}}, "validity": {"node": "Validity"}}]}|}
      (number_value 5)
  and eenmalig =
    {|{"node": "Uniqueness", "name": "eenmalige a", "versions": [{"sources": [{"node": "Source", "objectType": "Post", "keys": ["a"]}], "validity": {"node": "Validity"}}]}|}
  in
  let doc ?(consistencies = [ verification ]) ?(uniquenesses = []) rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Haven", "animate": false, "plural": "Havens", "attributes": [{"node": "Attribute", "name": "y", "datatype": %s}], "characteristics": [{"node": "Characteristic", "name": "gesloten", "kind": "adjectival"}, {"node": "Characteristic", "name": "open", "kind": "adjectival"}]}, {"node": "ObjectType", "name": "Post", "animate": false, "plural": "Posten", "attributes": [{"node": "Attribute", "name": "a", "datatype": %s}], "characteristics": []}], "factTypes": [{"node": "FactType", "name": "havenpost", "reciprocal": false, "roles": [{"node": "Role", "name": "haven", "objectType": "Haven", "many": false}, {"node": "Role", "name": "post", "objectType": "Post", "many": false}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [{"node": "ObjectCreation", "name": "post voor een open haven", "versions": [{"createdType": "Post", "subject": "Haven", "initialisations": [], "condition": {"node": "CharacteristicCheck", "characteristic": "open", "negated": false, "source": {"snippet": "de Haven is open"}}, "validity": {"node": "Validity"}}]}], "allocations": [], "consistencies": [%s], "uniquenesses": [%s], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [], "prioritizedDefaults": []}, "diagnostics": []}|}
      number_json number_json
      (String.concat ", " (List.map snd rule_defs))
      (String.concat ", " consistencies) (String.concat ", " uniquenesses)
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "H1", "objectType": "Haven", "attributes": {}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "characteristicPresent", "label": "H1", "characteristic": "open"}, {"kind": "inconsistent", "rule": "controle post"}]}|}
  in
  List.iter
    (fun (ranking_order, rule_defs) ->
      let ident = Printf.sprintf "WV-6.3-consistentie-na-een-creatie [%s]" ranking_order in
      plays ~ident ~ir:(doc rule_defs) ~case_data (fun final ->
          let posten =
            World.SMap.fold
              (fun _ (i : World.instance) acc -> if i.otype = "Post" then i.label :: acc else acc)
              final.instances []
          in
          check (ident ^ ": one Post") (posten = [ "oc0::H1" ]);
          expect_num (ident ^ ": a of the Post = 5") (attribute_of final "oc0::H1" "a") "5";
          check (ident ^ ": the channel carries check post on the Post, once")
            (List.map (fun (f : World.error_fact) -> (f.rule, f.subject)) final.channel
            = [ ("controle post", "oc0::H1") ])))
    [ ("y, a, gesloten, open", [ y; a; sealed; open_ ]);
      ("open, gesloten, a, y", [ open_; sealed; a; y ]) ];
  let case_2 =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "H1", "objectType": "Haven", "attributes": {}, "roles": {}, "characteristics": []}, {"label": "H2", "objectType": "Haven", "attributes": {}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "characteristicPresent", "label": "H1", "characteristic": "open"}, {"kind": "characteristicPresent", "label": "H2", "characteristic": "open"}, {"kind": "inconsistent", "rule": "eenmalige a"}]}|}
  in
  List.iter
    (fun (ranking_order, rule_defs) ->
      let ident = Printf.sprintf "WV-6.3-uniciteit-na-een-creatie [%s]" ranking_order in
      plays ~ident
        ~ir:(doc ~consistencies:[] ~uniquenesses:[ eenmalig ] rule_defs)
        ~case_data:case_2
        (fun final ->
          check (ident ^ ": both Posts carry eenmalige a")
            (List.sort compare
               (List.map (fun (f : World.error_fact) -> (f.rule, f.subject)) final.channel)
            = [ ("eenmalige a", "oc0::H1"); ("eenmalige a", "oc0::H2") ])))
    [ ("y, a, gesloten, open", [ y; a; sealed; open_ ]);
      ("open, gesloten, a, y", [ open_; sealed; a; y ]) ]

(* WV-5.5-consistentie-op-een-groeiend-kenmerk (ch. 5.5, since 2026-10-09):
   the consistency and uniqueness forms of a stratum fire once the
   generations of that stratum are done. A consistency reads a positive
   characteristic non-strictly (ch. 6.1), so it can share a stratum with a
   cycle that still makes that characteristic grow, and a channel fact is
   never withdrawn (ch. 5.6). "ervaren wachter" marks a Wachter that has a
   maatgever; "geef door" (recursive group "doorgeven") links an ervaren
   Wachter as maatgever to its opvolger, the leerling; the consistency "elke
   wachter ervaren" asserts that a Wachter is ervaren. All three are in
   stratum 0. W1 → W2 → W3 over opvolging, W1 ervaren in the input: W2 is
   ervaren after generation 1, W3 after generation 2, and the channel stays
   empty. Fired on the world of generation 0, where only W1 is ervaren, the
   consistency would leave a fact on W2 and one on W3.

   WV-5.5-uniciteit-op-een-groeiende-sleutel: a uniqueness reads its keys
   strictly (ch. 6.1), so it sits a stratum above every writer of a key and
   never shares a stratum with a cycle that still changes one. The document
   above without the consistency, with "volgnummer van een wachter" (the
   number of its maatgevers plus its basis, stratum 0 beside the cycle) and
   the uniqueness "uniek volgnummer" (stratum 1). W1 and W2 with basis 5, W3
   with basis 0: on the world of generation 0, W1 and W2 share volgnummer 5;
   at the end the volgnummers are 5, 6 and 1. In both orders of the two
   rules the channel stays empty. *)
let () =
  let number_value n =
    Printf.sprintf {|{"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}|} n
      number_json n
  in
  let maatgevers =
    {|{"node": "Aggregation", "kind": "count", "arguments": [{"node": "AttributeReference", "attribute": "maatgevers", "subjectType": "Wachter", "source": {"snippet": "alle maatgevers van de Wachter"}}], "elementType": "Wachter", "sourceRole": {"node": "RoleBinding", "factType": "maatschap", "role": "maatgever"}, "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative"}, "source": {"snippet": "het aantal maatgevers van de Wachter"}}|}
  in
  let ervaren =
    ( "ervaren wachter",
      Printf.sprintf
        {|{"node": "Rule", "name": "ervaren wachter", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "ervaren", "subjectType": "Wachter", "source": {"snippet": "Een Wachter is ervaren"}}, "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": {"snippet": "waar"}}, "condition": {"node": "Comparison", "op": "greater", "left": %s, "right": %s, "source": {"snippet": "het aantal maatgevers van de Wachter is groter dan 0"}}, "initialisation": false}]}|}
        maatgevers (number_value 0) )
  and sequence_number =
    ( "volgnummer van een wachter",
      Printf.sprintf
        {|{"node": "Rule", "name": "volgnummer van een wachter", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "volgnummer", "subjectType": "Wachter", "source": {"snippet": "Het volgnummer van een Wachter"}}, "value": {"node": "Binary", "op": "plus", "left": %s, "right": {"node": "AttributeReference", "attribute": "basis", "subjectType": "Wachter", "source": {"snippet": "zijn basis"}}, "type": %s, "source": {"snippet": "het aantal maatgevers van de Wachter plus zijn basis"}}, "initialisation": false, "presentation": "calculation"}]}|}
        maatgevers number_json )
  in
  let doc ?(attributes = []) ?(consistencies = "") ?(uniquenesses = "") rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Wachter", "animate": true, "plural": "Wachters", "attributes": [%s], "characteristics": [{"node": "Characteristic", "name": "ervaren", "kind": "adjectival"}]}], "factTypes": [{"node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [{"node": "Role", "name": "voorganger", "objectType": "Wachter", "plural": "voorgangers", "many": false}, {"node": "Role", "name": "opvolger", "objectType": "Wachter", "plural": "opvolgers", "many": false}]}, {"node": "FactType", "name": "maatschap", "reciprocal": false, "roles": [{"node": "Role", "name": "maatgever", "objectType": "Wachter", "plural": "maatgevers", "many": true}, {"node": "Role", "name": "leerling", "objectType": "Wachter", "plural": "leerlingen", "many": true}]}], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [%s], "uniquenesses": [%s], "factCreations": [{"node": "FactCreation", "name": "geef door", "versions": [{"factType": "maatschap", "anchorRole": "maatgever", "targetRole": "leerling", "anchor": "Wachter", "path": [{"node": "RoleBinding", "factType": "opvolging", "role": "opvolger"}], "condition": {"node": "CharacteristicCheck", "characteristic": "ervaren", "negated": false, "source": {"snippet": "hij is ervaren"}}, "validity": {"node": "Validity"}}]}], "startPointDeterminations": [], "ruleGroups": [{"node": "RuleGroup", "name": "doorgeven", "recursive": true, "members": ["geef door"], "source": {"snippet": "doorgeven"}}], "prioritizedDefaults": []}, "diagnostics": []}|}
      (String.concat ", "
         (List.map
            (fun one_member -> Printf.sprintf {|{"node": "Attribute", "name": "%s", "datatype": %s}|} one_member number_json)
            attributes))
      (String.concat ", " (List.map snd rule_defs))
      consistencies uniquenesses
  in
  (* W1 → W2 → W3 over opvolging, W1 ervaren; [base_expr] gives each its basis *)
  let case_data ?base_expr expected_value =
    let wachter label successor characteristic b =
      Printf.sprintf
        {|{"label": "%s", "objectType": "Wachter", "attributes": {%s}, "roles": {%s}, "characteristics": [%s]}|}
        label
        (match b with Some b -> Printf.sprintf {|"basis": {"number": "%d"}|} b | None -> "")
        (match successor with Some o -> Printf.sprintf {|"opvolger": ["%s"]|} o | None -> "")
        (if characteristic then {|"ervaren"|} else "")
    in
    let b i = Option.map (fun l -> List.nth l i) base_expr in
    Printf.sprintf
      {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [%s, %s, %s], "parameters": {}}, "expected": [%s]}|}
      (wachter "W1" (Some "W2") true (b 0))
      (wachter "W2" (Some "W3") false (b 1))
      (wachter "W3" None false (b 2))
      expected_value
  in
  let every_wachter_ervaren =
    {|{"node": "Consistency", "name": "elke wachter ervaren", "versions": [{"subject": "Wachter", "assertion": {"node": "CharacteristicCheck", "characteristic": "ervaren", "negated": false, "source": {"snippet": "hij is ervaren"}}, "validity": {"node": "Validity"}}]}|}
  in
  plays ~ident:"WV-5.5-consistentie-op-een-groeiend-kenmerk"
    ~ir:(doc ~consistencies:every_wachter_ervaren [ ervaren ])
    ~case_data:
      (case_data
         {|{"kind": "characteristicPresent", "label": "W2", "characteristic": "ervaren"}, {"kind": "characteristicPresent", "label": "W3", "characteristic": "ervaren"}, {"kind": "consistent"}|})
    (fun _ -> ());
  let unique_sequence_number =
    {|{"node": "Uniqueness", "name": "uniek volgnummer", "versions": [{"sources": [{"node": "Source", "objectType": "Wachter", "keys": ["volgnummer"]}], "validity": {"node": "Validity"}}]}|}
  in
  List.iter
    (fun ranking_order ->
      let ident =
        Printf.sprintf "WV-5.5-uniciteit-op-een-groeiende-sleutel [%s]"
          (String.concat "; " (List.map fst ranking_order))
      in
      plays ~ident
        ~ir:(doc ~attributes:[ "basis"; "volgnummer" ] ~uniquenesses:unique_sequence_number ranking_order)
        ~case_data:
          (case_data ~base_expr:[ 5; 5; 0 ]
             {|{"kind": "attributeValue", "label": "W1", "attribute": "volgnummer", "value": {"number": "5"}}, {"kind": "attributeValue", "label": "W2", "attribute": "volgnummer", "value": {"number": "6"}}, {"kind": "attributeValue", "label": "W3", "attribute": "volgnummer", "value": {"number": "1"}}, {"kind": "consistent"}|})
        (fun _ -> ()))
    (orders [ ervaren; sequence_number ])

(* WV-5.7-twee-schrijvers-twee-strata (since 2026-10-09): the forms that make
   nothing come to rest stratum by stratum, and each stratum's fixpoint runs
   the forms of every lower stratum again, so two writers of one target in
   different strata still overwrite each other there and fault (ch. 5.7). "a
   een" writes a = 1 when x is groter dan 0 (stratum 0); "m" marks a Ding whose
   x is groter dan 9 (stratum 0); "a twee" writes a = 2 when the Ding is not m,
   a negated read that puts it in stratum 1. D with x = 5: both conditions
   hold, no fixpoint, in both orders. A fixpoint of stratum 1 that ran only
   the forms of stratum 1 would leave a = 2 without a word. *)
let () =
  let rule_def ident objective value_node precondition presentation_kind =
    Printf.sprintf
      {|{"node": "Rule", "name": "%s", "versions": [{"validity": {"node": "Validity"}, "target": {"node": "AttributeReference", "attribute": "%s", "subjectType": "Ding", "source": {"snippet": "%s"}}, "value": %s, "condition": %s, "initialisation": false%s}]}|}
      ident objective objective value_node precondition presentation_kind
  in
  let greater_than n =
    Printf.sprintf
      {|{"node": "Comparison", "op": "greater", "left": {"node": "AttributeReference", "attribute": "x", "subjectType": "Ding", "source": {"snippet": "zijn x"}}, "right": {"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}, "source": {"snippet": "x > %d"}}|}
      n number_json n n
  in
  let number_value n =
    Printf.sprintf {|{"node": "Literal", "value": {"number": "%d"}, "type": %s, "source": {"snippet": "%d"}}|} n
      number_json n
  in
  let single = rule_def "a een" "a" (number_value 1) (greater_than 0) {|, "presentation": "equation"|}
  and m =
    rule_def "m" "m" {|{"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": {"snippet": "waar"}}|}
      (greater_than 9) ""
  and two =
    rule_def "a twee" "a" (number_value 2)
      {|{"node": "CharacteristicCheck", "characteristic": "m", "negated": true, "source": {"snippet": "niet m"}}|}
      {|, "presentation": "equation"|}
  in
  let doc rule_defs =
    Printf.sprintf
      {|{"format": "uchar-ir-v1", "model": {"objectTypes": [{"node": "ObjectType", "name": "Ding", "animate": false, "plural": "Dingen", "attributes": [{"node": "Attribute", "name": "x", "datatype": %s}, {"node": "Attribute", "name": "a", "datatype": %s}], "characteristics": [{"node": "Characteristic", "name": "m", "kind": "adjectival"}]}], "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []}, "rules": {"rules": [%s], "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [], "prioritizedDefaults": []}, "diagnostics": []}|}
      number_json number_json (String.concat ", " rule_defs)
  in
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "D", "objectType": "Ding", "attributes": {"x": {"number": "5"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "consistent"}]}|}
  in
  List.iter
    (fun (ranking_order, rule_defs, sentence_text) ->
      let ident = Printf.sprintf "WV-5.7-twee-schrijvers-twee-strata [%s]" ranking_order in
      with_files ~ir:(doc rule_defs) ~case_data (fun findings _ evalueer ->
          check (ident ^ ": verifier-clean") (findings = []);
          expect_fault_msg ident evalueer sentence_text))
    [ ( "a een eerst",
        [ single; m; two ],
        {|two writers of one target disagree: rule "a een" writes 1 and rule "a twee" writes 2 to the attribute a of D, a Ding|}
      );
      ( "a twee eerst",
        [ two; m; single ],
        {|two writers of one target disagree: rule "a twee" writes 2 and rule "a een" writes 1 to the attribute a of D, a Ding|}
      ) ]

(* WV-7.9: nested exception groups (ch. 7.9) — the PHASE4-DEFAULTS §7
   spurious-conflict test at the unit level (b <- x, x <- {y, z}, hand-built
   attribute defaults). Only y applicable and x justified must yield y, never a
   manufactured root conflict; y and z deciding differently propagates the
   conflict to the root (no write, path-keyed Jutter.heffing/0); neither
   applicable falls back to the default's own consequence. The corpus anchors
   jutterswet-nestuitzondering/nestkenmerk carry the full family; this isolates
   the reconciliation invariant in the fast suite. *)
let () =
  let inst_c label otype chars : World.instance =
    {
      label; otype;
      attrs = World.SMap.empty;
      chars = List.map Ir_v0.Checks.norm chars;
      left_side = World.SMap.empty;
      tls = World.SMap.empty; ctls = World.SMap.empty;
    }
  in
  let kc name : Ir_v0.Ast.condition_expr =
    Characteristic_check
      { characteristic = Some name; negation = false; holder = Some "Jutter";
        time_period = None; source_name = None; navigation_binding = None }
  in
  let leaf name lex : Ir_v0.Ast.default =
    { default_decision = None; default_justification = Some (kc name);
      default_consequence = Some (litv lex num0); default_initialisation = false;
      default_exceptions = None; default_source = None }
  in
  let sub name lex : Ir_v0.Ast.exception_group = { eg_defaults = [ leaf name lex ]; eg_source = None } in
  let defaults : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some "heffing"; pd_subject = "Jutter"; pd_characteristic = None;
      pd_attribute = Some "heffing";
      defaults =
        [ { default_decision = None; default_justification = Some (kc "grondX");
            default_consequence = Some (litv "40" num0); default_initialisation = false;
            default_exceptions =
              Some { egs_unordered = true; egs_members = [ sub "grondY" "10"; sub "grondZ" "20" ] };
            default_source = None } ];
      pd_unordered = false; pd_source = None; pd_validity = None }
  in
  let doc =
    { empty_doc with
      object_types = [ ot "Jutter" [ ("heffing", Some num0) ] [ "grondX"; "grondY"; "grondZ" ] ];
      prioritized_defaults = [ defaults ] }
  in
  let run chars =
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst_c "j" "Jutter" chars)) in
    let j = World.get final "j" in
    (World.attr j "heffing", List.map (fun (f : World.error_fact) -> f.rule) final.channel)
  in
  let v1, c1 = run [ "grondX"; "grondY" ] in
  check "WV-7.9-spurious-y" (Arith.equal v1 (n "10") && c1 = []);
  let v2, c2 = run [ "grondX"; "grondY"; "grondZ" ] in
  check "WV-7.9-subconflict" (v2 = Value.Empty && c2 = [ "Jutter.heffing/0" ]);
  let v3, c3 = run [ "grondX" ] in
  check "WV-7.9-fallback" (Arith.equal v3 (n "40") && c3 = [])

(* --- ch. 3: timeline algebra (worked examples verbatim) ----------------- *)

let day s = match Value.date_of_iso s with Value.Date z -> z | _ -> assert false
let seg van up_to v : Timeline.segment = { s_from = day van; s_to = day up_to; s_value = v }
let seg_empty van up_to : Timeline.segment = { s_from = day van; s_to = day up_to; s_value = empty_value }

let tl_shape name (tl : Timeline.t) expected =
  check name
    (List.length tl = List.length expected
    && List.for_all2
         (fun (s : Timeline.segment) (van, up_to, v) ->
           s.s_from = day van && s.s_to = day up_to
           &&
           match (s.s_value, v) with
           | Value.Empty, None -> true
           | actual, Some lex -> Arith.equal actual (n lex)
           | _ -> false)
         tl expected)

(* WV-1.2f: the two timeline empty rules side by side *)
let () =
  let a = [ seg_empty "2024-01-01" "2024-02-01" ] in
  let b = [ seg "2024-01-01" "2024-02-01" (n "4") ] in
  tl_shape "WV-1.2f-tl-1" (Timeline.plus a b) [ ("2024-01-01", "2024-02-01", Some "4") ];
  tl_shape "WV-1.2f-tl-2" (Timeline.summed [ a; b ]) [ ("2024-01-01", "2024-02-01", Some "4") ];
  let bb = [ seg_empty "2024-01-01" "2024-02-01" ] in
  tl_shape "WV-1.2f-tl-3" (Timeline.plus a bb) [ ("2024-01-01", "2024-02-01", Some "0") ];
  tl_shape "WV-1.2f-tl-4" (Timeline.summed [ a; bb ]) [ ("2024-01-01", "2024-02-01", None) ];
  tl_shape "WV-1.2f-tl-4-fallback"
    (Timeline.summed ~fallback:(n "0") [ a; bb ])
    [ ("2024-01-01", "2024-02-01", Some "0") ]

(* WV-3.4: cut-merge — the period-coalescing rule *)
let () =
  let a = [ seg "2024-01-01" "2024-07-01" (n "100") ] in
  let b = [ seg "2024-04-01" "2024-10-01" (n "30") ] in
  tl_shape "WV-3.4-a" (Timeline.plus a b)
    [
      ("2024-01-01", "2024-04-01", Some "100");
      ("2024-04-01", "2024-07-01", Some "130");
      ("2024-07-01", "2024-10-01", Some "30");
    ];
  let a = [ seg "2024-01-01" "2024-02-01" (n "1") ] in
  let b = [ seg "2024-03-01" "2024-04-01" (n "2") ] in
  tl_shape "WV-3.4-b" (Timeline.plus a b)
    [
      ("2024-01-01", "2024-02-01", Some "1");
      ("2024-02-01", "2024-03-01", Some "0");
      ("2024-03-01", "2024-04-01", Some "2");
    ]

(* WV-3.5: temporal summation — the contributor-skipping rule *)
let () =
  let p1 = [ seg "2024-01-01" "2024-02-01" (n "50"); seg "2024-02-01" "2024-03-01" (n "60") ] in
  let p2 = [ seg "2024-02-01" "2024-03-01" (n "10"); seg "2024-03-01" "2024-04-01" (n "10") ] in
  tl_shape "WV-3.5-a" (Timeline.summed [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "50");
      ("2024-02-01", "2024-03-01", Some "70");
      ("2024-03-01", "2024-04-01", Some "10");
    ];
  let p1 = [ seg "2024-01-01" "2024-02-01" (n "40") ] in
  let p2 = [ seg "2024-03-01" "2024-04-01" (n "20") ] in
  tl_shape "WV-3.5-b" (Timeline.summed [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "40");
      ("2024-02-01", "2024-03-01", None);
      ("2024-03-01", "2024-04-01", Some "20");
    ];
  tl_shape "WV-3.5-b-fallback"
    (Timeline.summed ~fallback:(n "0") [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "40");
      ("2024-02-01", "2024-03-01", Some "0");
      ("2024-03-01", "2024-04-01", Some "20");
    ];
  check "WV-3.5-c-empty-set" (Timeline.summed ~fallback:(n "0") [] = []);
  (* cross-unit: €/mnd contributors into a €/jr target — restated before
     the fold: 50×12 + 10×12 = 720 *)
  let eur_mnd = { eur with denominator = Some mnd } in
  let eur_jr = { eur with denominator = Some jr } in
  let q1 = [ seg "2024-01-01" "2024-02-01" (num_with_unit eur_mnd "50") ] in
  let q2 = [ seg "2024-01-01" "2024-02-01" (num_with_unit eur_mnd "10") ] in
  (match Timeline.summed ~target_unit:(Value.Unit eur_jr) [ q1; q2 ] with
  | [ { s_value = Value.Num (v, Value.Unit u); _ } ] ->
      check "WV-3.5-d-720" (Q.equal v (q "720") && u.denominator = Some jr)
  | _ -> check "WV-3.5-d-720" false)

(* WV-3.6: the temporal total *)
let () =
  let month = Ir_v0.Ast.Month in
  let tl =
    [ seg "2023-01-01" "2024-01-01" (n "10.00"); seg "2024-01-01" "2024-07-01" (n "11.00") ]
  in
  expect_num "WV-3.6-a" (Timeline.grand_total month tl) "186";
  let degenerate =
    [ seg "2024-01-01" "2024-01-01" (n "99"); { s_from = day "2024-06-01"; s_to = day "2024-05-01"; s_value = n "7" }; seg "2024-01-01" "2024-07-01" (n "10") ]
  in
  expect_num "WV-3.6-b" (Timeline.grand_total month degenerate) "60";
  (* gedurende "> 1000" clips to the 1200 months *)
  let vals = [ seg "2024-01-01" "2024-04-01" (n "1200"); seg "2024-04-01" "2024-06-01" (n "800") ] in
  let span = List.map (fun (s : Timeline.segment) -> (s.s_from, s.s_to)) vals in
  let tp = Timeline.true_periods_of vals ~span (fun v -> Arith.compare_op Greater v (n "1000")) in
  expect_num "WV-3.6-c" (Timeline.grand_total month (Timeline.clip vals tp)) "3600";
  (* gedurende a characteristic, and its negated complement *)
  let vals = [ seg "2023-01-01" "2024-01-01" (n "10"); seg "2024-01-01" "2024-07-01" (n "11") ] in
  let span = [ (day "2023-01-01", day "2024-07-01") ] in
  let char_true = [ (day "2023-07-01", day "2024-02-01") ] in
  expect_num "WV-3.6-d" (Timeline.grand_total month (Timeline.clip vals char_true)) "71";
  expect_num "WV-3.6-e" (Timeline.grand_total month (Timeline.clip vals (Timeline.complement char_true ~span))) "115";
  (* gedurende "≠ 1000": leeg satisfies ≠ against a filled bound *)
  let vals = [ seg "2024-01-01" "2024-07-01" (n "100") ] in
  let tested = [ seg "2024-01-01" "2024-02-01" (n "1000") ] in
  let span = [ (day "2024-01-01", day "2024-07-01") ] in
  let tp = Timeline.true_periods_of tested ~span (fun v -> Arith.compare_op Not_equal v (n "1000")) in
  expect_num "WV-3.6-f" (Timeline.grand_total month (Timeline.clip vals tp)) "500";
  (* the explicit rate operand: 10 €/mnd across 2024 integrates to 120 €,
     denominator cancelled against the grid's own unit *)
  let eur_per_mnd = { eur with denominator = Some mnd } in
  let year_2024 v = [ seg "2024-01-01" "2025-01-01" v ] in
  let grid u = Ok u in
  (match Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit eur_per_mnd "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g" (Q.equal v (q "120") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-g (got " ^ Value.to_string other ^ ")") false);
  (* a plain € value on the same per-month timeline keeps the plain rule:
     the same 120, and no denominator to cancel *)
  (match Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit eur "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-plain-unit"
        (Q.equal v (q "120") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-g-plain-unit (got " ^ Value.to_string other ^ ")") false);
  (* the three readings of the SAME grid: unitless, plain €, and the €/mnd
     rate. The plain rule and the €/mnd integration agree on the NUMBER
     (12×10); the claim only holds where the denominator matches the grid
     one-for-one — WV-3.6-g-vreemde-noemer below is the counter-example. *)
  check "WV-3.6-g-getallen-identiek"
    (List.for_all
       (fun v ->
         match Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 v) with
         | Value.Num (got, _) -> Q.equal got (q "120")
         | _ -> false)
       [ n "10"; num_with_unit eur "10"; num_with_unit eur_per_mnd "10" ]);
  (* the FOREIGN but CONVERTIBLE denominator integrates EXACTLY through the
     declared factor: 12 months IS one year, so 10 €/jr over 2024 is 10 €,
     not 120 (maintainer decision 2026-09-04) *)
  (match Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit eur_per_jr "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-jaarnoemer-exact"
        (Q.equal v (q "10") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-g-jaarnoemer-exact (got " ^ Value.to_string other ^ ")") false);
  (* a month-sized denominator that is NOT a calendar spelling (factor 1 to
     the same canonical month): the declared factor carries it, the
     spelling table never enters *)
  let eur_per_bm = { eur with denominator = Some bm } in
  expect_num "WV-3.6-g-bm-jaar"
    (Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit eur_per_bm "10")))
    "120";
  expect_num "WV-3.6-g-bm-bucket"
    (Timeline.grand_total ~grid_unit:(grid mnd)
       month
       [ seg "2024-01-01" "2024-02-01" (num_with_unit eur_per_bm "10") ])
    "10";
  (* a COARSER grid: 10 €/mnd over 2024 on a per-quarter grid is 4 quarters
     × 3 months × 10 = 120 €, exactly the per-month reading *)
  (match Timeline.grand_total ~grid_unit:(grid kw) Quarter (year_2024 (num_with_unit eur_per_mnd "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-kwartaalrooster"
        (Q.equal v (q "120") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-g-kwartaalrooster (got " ^ Value.to_string other ^ ")") false);
  (* a denominator the declarations connect to nothing month-sized is
     refused loudly, and the refusal NAMES BOTH units — the value's and
     the grid's *)
  let eur_per_dg = { eur with denominator = Some dg } in
  (match Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit eur_per_dg "10")) with
  | exception Value.Fault { text = m; _ } ->
      check "WV-3.6-g-vreemde-noemer"
        (m
        = "the temporal total of a €/dg value cannot integrate over a mnd grid: the \
           declarations connect no conversion between its denominator and the grid unit \
           (ch. 3.6)")
  | _ -> check "WV-3.6-g-vreemde-noemer" false);
  (* a denominator that names no time unit at all is refused the same way *)
  expect_fault "WV-3.6-g-noemer-zonder-tijdseenheid" (fun () ->
      Timeline.grand_total ~grid_unit:(grid mnd) month (year_2024 (num_with_unit { eur with denominator = Some kg } "10")));
  (* no grid unit to integrate against: the refusal carries the lookup's
     own reason, and it bites the RATE operand only — the plain and
     unitless readings above total in a document that declares no unit *)
  expect_fault "WV-3.6-g-geen-roostereenheid" (fun () ->
      Timeline.grand_total ~grid_unit:(Error "the document declares no mnd unit") month
        (year_2024 (num_with_unit eur_per_mnd "10")));
  expect_num "WV-3.6-g-geen-roostereenheid-plain"
    (Timeline.grand_total ~grid_unit:(Error "the document declares no mnd unit") month
       (year_2024 (n "10")))
    "120";
  (* segments of ONE timeline carrying DIFFERENT units: the collapse sums
     the per-segment products with the ordinary ch. 2.6 addition, so a
     convertible sister unit is restated EXACTLY (120 € + 500 ct = 125 €)
     and a foreign one is refused loudly; only the unitless side adopts
     silently. The case loader attaches the declaration's ONE unit to
     every segment, so no case can reach these shapes. *)
  let ct_per_mnd = { cent with denominator = Some mnd } in
  expect_num "WV-3.6-g-segmentmenging-convertibel"
    (Timeline.grand_total ~grid_unit:(grid mnd) month
       [ seg "2024-01-01" "2025-01-01" (num_with_unit eur_per_mnd "10");
         seg "2025-01-01" "2025-02-01" (num_with_unit ct_per_mnd "500") ])
    "125";
  expect_fault "WV-3.6-g-segmentmenging-niet-convertibel" (fun () ->
      Timeline.grand_total ~grid_unit:(grid mnd) month
        [ seg "2024-01-01" "2024-02-01" (num_with_unit eur "10");
          seg "2024-02-01" "2024-03-01" (num_with_unit meter "3") ]);
  expect_num "WV-3.6-g-segmentmenging-unitless-adopteert"
    (Timeline.grand_total ~grid_unit:(grid mnd) month
       [ seg "2024-01-01" "2024-02-01" (n "10");
         seg "2024-02-01" "2024-03-01" (num_with_unit eur "5") ])
    "15"

(* the grid unit is looked up in the DOCUMENT's declared unit systems
   (ch. 3.6 / ch. 3.8 spelling table), once per fold: present, absent, and
   ambiguous each have their own answer — never a first-match guess *)
let () =
  let system name unit_list : string * Ir_v0.Ast.unit_system =
    ( name,
      { es_name = Some name;
        unit_list = List.map (fun e -> { Ir_v0.Ast.ge_unit = e; ge_spellings = [] }) unit_list } )
  in
  let doc_of systems = { empty_doc with unit_systems = systems } in
  (match Timeline.grid_unit (doc_of [ system "Munt" [ eur ]; system "Tijd" [ mnd; jr ] ]) Month with
  | Ok u -> check "WV-3.6-rooster-lookup" (u.abbr_text = Some "mnd")
  | Error _ -> check "WV-3.6-rooster-lookup" false);
  (match Timeline.grid_unit (doc_of [ system "Munt" [ eur ]; system "Tijd" [ mnd; jr ] ]) Year with
  | Ok u -> check "WV-3.6-rooster-lookup-jaar" (u.abbr_text = Some "jr")
  | Error _ -> check "WV-3.6-rooster-lookup-jaar" false);
  (match Timeline.grid_unit (doc_of [ system "Munt" [ eur ] ]) Month with
  | Error _ -> check "WV-3.6-rooster-ongedeclareerd" true
  | Ok _ -> check "WV-3.6-rooster-ongedeclareerd" false);
  (* two systems spelling the same grid unit with different canonical
     chains: no single grid, so the lookup refuses instead of picking *)
  let mnd_in_days = unit_ ~canonical_form:"dg" ~factor:(30, 1) "mnd" in
  (match Timeline.grid_unit (doc_of [ system "A" [ mnd ]; system "B" [ mnd_in_days ] ]) Month with
  | Error _ -> check "WV-3.6-rooster-ambigu" true
  | Ok _ -> check "WV-3.6-rooster-ambigu" false)

(* WV-3.6-h: the §7.1 per-element fold over a set — each member's own
   timeline collapses over time FIRST, then the scalars fold over the
   set. Three set members: 10 €/mnd across 2024 → 120 €, 5 €/mnd over
   the first half → 30 €, one without a timeline → contributes nothing;
   total 150 €. A ranking or counting fold over the same shape is
   refused, and so is a member that brings no horizon of its own
   (maintainer decision 2026-09-04, the convention of this fold). *)
let () =
  let eur_per_mnd = { eur with denominator = Some mnd } in
  let num_eur_mnd : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Some eur_per_mnd }
  in
  let num_eur : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Some eur }
  in
  let vondst =
    let nm, o = ot "Vondst" [ ("bewaartarief", Some num_eur_mnd) ] [] in
    (nm, { o with attribute_granularity = [ ("bewaartarief", Some Ir_v0.Ast.Month) ] })
  in
  (* a SAME-NAMED temporal attribute at a DIFFERENT granularity elsewhere:
     the fold takes the grid off the ELEMENT type's declaration, never off
     the name *)
  let kist =
    let nm, o = ot "Kist" [ ("bewaartarief", Some num_eur_mnd) ] [] in
    (nm, { o with attribute_granularity = [ ("bewaartarief", Some Ir_v0.Ast.Year) ] })
  in
  let systems : (string * Ir_v0.Ast.unit_system) list =
    [ ("Munt", { es_name = Some "Munt"; unit_list = [ { ge_unit = eur; ge_spellings = [] } ] });
      ("Tijd", { es_name = Some "Tijd"; unit_list = [ { ge_unit = mnd; ge_spellings = [] } ] }) ]
  in
  let doc =
    {
      empty_doc with
      unit_systems = systems;
      object_types = [ ot "Jutter" [ ("bewaarlast", Some num_eur) ] []; vondst; kist ];
      fact_types =
        [ ( "bewaring",
            {
              ft_name = Some "bewaring";
              reciprocal = false;
              role_names =
                [
                  { role_identifier = Some "bewarende jutter"; role_object_type_name = Some "Jutter";
                    role_plural = None; role_many = false };
                  { role_identifier = Some "vondst"; role_object_type_name = Some "Vondst";
                    role_plural = Some "vondsten"; role_many = true };
                ];
            } ) ];
    }
  in
  let fold kind : Ir_v0.Ast.expr =
    Function_call
      { kind;
        call_arguments =
          [ Attribute_reference
              { attribute_ref = Some "bewaartarief"; object_type = Some "Vondst";
                source_name = None; navigation_bindings = None } ];
        element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num_eur; source_name = None; grain = None;
        source_role = Some { rb_fact_type = "bewaring"; rb_role = "vondst" }; source_path = None }
  in
  let tl_of segs label = { (inst label "Vondst" []) with
                           tls = World.SMap.add "bewaartarief" segs World.SMap.empty } in
  let jutter labels =
    { (inst "j1" "Jutter" []) with
      left_side = World.SMap.add "vondsten" labels World.SMap.empty }
  in
  let world_of j members = List.fold_left World.add_instance (World.add_instance World.empty j) members in
  let v1 = tl_of [ seg "2024-01-01" "2025-01-01" (num_with_unit eur_per_mnd "10") ] "v1" in
  let v2 = tl_of [ seg "2024-01-01" "2024-07-01" (num_with_unit eur_per_mnd "5") ] "v2" in
  let v3 = inst "v3" "Vondst" [] in
  let w = world_of (jutter [ "v1"; "v2"; "v3" ]) [ v1; v2; v3 ] in
  let env = env_of doc in
  let run w kind = Eval.eval_expr env w (World.get w "j1") ~err:no_err (fold kind) in
  (match run w Total with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-h" (Q.equal v (q "150") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-h (got " ^ Value.to_string other ^ ")") false);
  (* de som van over the same shape folds identically inside *)
  (match run w Sum_function with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-h-som" (Q.equal v (q "150") && u.abbr_text = Some "€" && u.denominator = None)
  | other -> check ("WV-3.6-h-som (got " ^ Value.to_string other ^ ")") false);
  (* every ranking or counting fold over the same shape is refused: there
     is no defined moment of the timeline to rank or count at *)
  List.iter
    (fun (label, kind) -> expect_fault ("WV-3.6-h-rangschikking-" ^ label) (fun () -> run w kind))
    [ ("minimum", Ir_v0.Ast.Minimum); ("maximum", Maximum); ("eerste", First);
      ("laatste", Last); ("aantal", Count) ];
  (* the convention of this fold: a member carrying the asOfDate-bucket
     SCALAR brings no horizon of its own, so the integration cannot run —
     refused loudly, NAMING the member (ch. 8.4's "needs the one, finds
     the other"), never silently bucketed at the session date *)
  let bucket_member label =
    { (inst label "Vondst" []) with
      attrs = World.SMap.add "bewaartarief" (num_with_unit eur_per_mnd "10") World.SMap.empty }
  in
  let w_bucket = world_of (jutter [ "v9" ]) [ bucket_member "v9" ] in
  (match run w_bucket Total with
  | exception Value.Fault { text = m; _ } ->
      check "WV-3.6-h-bucketlid"
        (m
        = "the per-element temporal total of \"bewaartarief\" over \"v9\": the case supplies an \
           asOfDate-bucket scalar, which carries no period of its own; a set fold needs each \
           member's own timeline (ch. 3.6/§7.1, ch. 8.4)")
  | _ -> check "WV-3.6-h-bucketlid" false);
  (* mixing a full timeline with a horizonless bucket member fails on the
     bucket member alone — timelines stay admissible beside it *)
  let w_menging = world_of (jutter [ "v1"; "v9" ]) [ v1; bucket_member "v9" ] in
  expect_fault "WV-3.6-h-menging" (fun () -> run w_menging Total);
  (* an ABSENT member contributes nothing: all-absent totals 0 (the §4.20
     unconditional 0), while de som van keeps its leeg rule outside *)
  let w_absent = world_of (jutter [ "v3"; "v4" ]) [ v3; inst "v4" "Vondst" [] ] in
  expect_num "WV-3.6-h-afwezig-totaal" (run w_absent Total) "0";
  expect_empty "WV-3.6-h-afwezig-som" (run w_absent Sum_function);
  (* the empty set: the same two answers *)
  let w_empty = world_of (jutter []) [] in
  expect_num "WV-3.6-h-lege-set-totaal" (run w_empty Total) "0";
  expect_empty "WV-3.6-h-lege-set-som" (run w_empty Sum_function);
  (* the same attribute NAME at a different granularity on another object
     type leaves this fold untouched: 150 stays *)
  expect_num "WV-3.6-h-eigen-granulariteit" (run w Total) "150"

(* verification-round regression: a true-period boundary that merely
   splits a bucket must not make the total undercount — clip coalesces
   adjacent equal fragments *)
let () =
  (* the clip floors whole units PER RAW FRAGMENT (the pinned runtime's
     rsTijdKnip × rsTijdTotaal, mirrored by the ASP encoding): a
     condition boundary that splits a bucket genuinely loses the partial
     units on both sides — two adjacent half-month fragments each floor
     to 0 whole months. The earlier coalescing expectation (100)
     contradicted the runtime and was corrected with the clip card. *)
  let vals = [ seg "2024-01-01" "2024-02-01" (n "100") ] in
  let tp = [ (day "2024-01-01", day "2024-01-15"); (day "2024-01-15", day "2024-02-01") ] in
  expect_num "verify-clip-per-fragment-floor" (Timeline.grand_total Month (Timeline.clip vals tp)) "0"

(* WV-3.2: anchored month walls clamp from the anchor, never from the
   previous wall *)
let () =
  let anchor = day "2024-01-31" in
  let wall k = Timeline.anchored_wall Month ~anchor k in
  check "WV-3.2-anchored"
    (wall 0 = day "2024-01-31" && wall 1 = day "2024-02-29" && wall 2 = day "2024-03-31"
    && wall 3 = day "2024-04-30")

(* WV-3.2-weging: the whole units the temporal total weighs a span by
   (RUNTIME-SEMANTICS ch. 3.2, job 1), the chapter's worked examples
   verbatim: the greatest k with van + k months <= tot, the month step
   clamping, so a period between clamped walls is one unit — where the
   duration of ch. 3.8 counts 0 whole months between the same two dates. *)
let () =
  let wu g van up_to = Timeline.whole_units g (day van) (day up_to) in
  check "WV-3.2-weging-maand-klem" (wu Month "2024-01-31" "2024-02-29" = 1);
  check "WV-3.2-weging-niet-de-tijdsduur"
    (Arith.whole_units_between Tu_month (day "2024-01-31") (day "2024-02-29") = 0);
  check "WV-3.2-weging-kwartaal-klem" (wu Quarter "2024-11-30" "2025-02-28" = 1);
  check "WV-3.2-weging-jaar-klem" (wu Year "2024-02-29" "2025-02-28" = 1);
  check "WV-3.2-weging-dag-te-kort" (wu Month "2024-01-31" "2024-02-28" = 0)

(* WV-3.2-vast: the grid a fixed start point lays (RUNTIME-SEMANTICS ch. 3.2,
   "Fixed starting points"), the chapter's worked examples verbatim. A wall
   is a day whose period starts on it; consecutive walls bound one period. *)
let () =
  let grid gran s = Timeline.grid_of ~gran ~count:1 ~fixed:(Some s) ~variable:false in
  let time_period gr d = Timeline.grid_bucket gr (day d) in
  let is (a, b) (x, y) = a = day x && b = day y in
  let walls gr ws =
    let rec consecutive = function
      | a :: (b :: _ as rest) -> time_period gr a = (day a, day b) && consecutive rest
      | [ a ] -> fst (time_period gr a) = day a
      | [] -> true
    in
    consecutive ws
  in
  let year = grid Year "--10-01" in
  check "WV-3.2-vast-jaar-feb" (is (time_period year "2025-02-01") ("2024-10-01", "2025-10-01"));
  check "WV-3.2-vast-jaar-sep" (is (time_period year "2024-09-30") ("2023-10-01", "2024-10-01"));
  check "WV-3.2-vast-maand-klem"
    (walls (grid Month "--01-31") [ "2024-01-31"; "2024-02-29"; "2024-03-31"; "2024-04-30" ]);
  check "WV-3.2-vast-kwartaal"
    (walls (grid Quarter "--05-31")
       [ "2024-02-29"; "2024-05-31"; "2024-08-31"; "2024-11-30"; "2025-02-28" ]);
  check "WV-3.2-vast-schrikkeldag"
    (walls (grid Year "--02-29")
       [ "2024-02-29"; "2025-02-28"; "2026-02-28"; "2027-02-28"; "2028-02-29" ]);
  let week = grid Week "2026-01-05" in
  check "WV-3.2-vast-week-ervoor" (is (time_period week "2026-01-04") ("2025-12-29", "2026-01-05"));
  check "WV-3.2-vast-week-erop" (is (time_period week "2026-01-05") ("2026-01-05", "2026-01-12"));
  (* the day count into a target on the --10-01 year grid, against the same
     count on the calendar grid *)
  let tp = [ (day "2024-09-28", day "2024-10-03") ] in
  (match Timeline.count_days_in_grid year tp with
  | [ a; b ] ->
      check "WV-3.2-vast-dagtelling-grenzen"
        (is (a.s_from, a.s_to) ("2023-10-01", "2024-10-01")
        && is (b.s_from, b.s_to) ("2024-10-01", "2025-10-01"));
      expect_num "WV-3.2-vast-dagtelling-3" a.s_value "3";
      expect_num "WV-3.2-vast-dagtelling-2" b.s_value "2"
  | _ -> check "WV-3.2-vast-dagtelling (two periods)" false);
  (match Timeline.count_days_in Year tp with
  | [ k ] ->
      check "WV-3.2-vast-dagtelling-kalender" (is (k.s_from, k.s_to) ("2024-01-01", "2025-01-01"));
      expect_num "WV-3.2-vast-dagtelling-kalender-5" k.s_value "5"
  | _ -> check "WV-3.2-vast-dagtelling-kalender (one period)" false);
  (* a case input on the --10-15 year grid: 15 October to 15 October sits on
     it, the calendar year is refused as off it *)
  let input_data van up_to =
    Yojson.Safe.from_string
      (Printf.sprintf {|[{"from":"%s","to":"%s","value":{"number":"1"}}]|} van up_to)
  in
  let load_doc van up_to =
    Case.timeline_of_json ~at:"J1.stormtoelage" ~grid:(grid Year "--10-15") (Some num0) (input_data van up_to)
  in
  check "WV-3.2-vast-invoer-op-het-raster"
    (match load_doc "2024-10-15" "2025-10-15" with [ _ ] -> true | _ -> false);
  check "WV-3.2-vast-invoer-naast-het-raster"
    (match load_doc "2025-01-01" "2026-01-01" with
    | exception Case.Error { text = m; _ } -> substring m "segment boundaries must sit on the declared granularity grid"
    | _ -> false)

(* WV-3.2-vast-peildatum: the subject's own total over the asOfDate bucket
   reads the period of the member's grid that holds the evaluation date
   (RUNTIME-SEMANTICS ch. 3.2, 3.6), on a week and a quarter grid as on a month
   and a year grid; the calendar's week grid lays no period. The chapter's
   worked example verbatim. *)
let () =
  let as_of_date = day "2025-02-15" in
  let grand_total gran fixed value_node =
    let gr = Timeline.grid_of ~gran ~count:1 ~fixed ~variable:false in
    let van, up_to = Timeline.grid_bucket gr as_of_date in
    ((van, up_to), Timeline.grand_total gran [ { Timeline.s_from = van; s_to = up_to; s_value = n value_node } ])
  in
  let (wv, wt), w = grand_total Week (Some "2024-09-30") "7" in
  check "WV-3.2-vast-peildatum-week" (wv = day "2025-02-10" && wt = day "2025-02-17");
  expect_num "WV-3.2-vast-peildatum-week-7" w "7";
  let (kv, kt), k = grand_total Quarter (Some "--05-31") "30" in
  check "WV-3.2-vast-peildatum-kwartaal" (kv = day "2024-11-30" && kt = day "2025-02-28");
  expect_num "WV-3.2-vast-peildatum-kwartaal-30" k "30";
  expect_fault_msg "WV-3.2-vast-peildatum-kalenderweek"
    (fun () -> grand_total Week None "7")
    "bucketing is defined for month and year only (ch. 3.2)"

(* WV-3.2-vast, what a grid refuses: several units carry no operation, a
   variable start is laid per object from an anchor ref does not execute, and
   the pro-rata over a fixed start point is fenced — each loud, in its own
   sentence, where the calendar grid used to answer. *)
let () =
  let plural_flag = Timeline.grid_of ~gran:Month ~count:5 ~fixed:(Some "2026-04-01") ~variable:false in
  expect_fault_msg "WV-3.2-vast-meervoudig"
    (fun () -> Timeline.grid_bucket plural_flag (day "2026-05-01"))
    "a multi-period grid (5 month periods) carries no operation";
  let varying = Timeline.grid_of ~gran:Year ~count:1 ~fixed:None ~variable:true in
  expect_fault_msg "WV-3.2-variabel"
    (fun () -> Timeline.grid_bucket varying (day "2026-05-01"))
    "a variable-start grid is laid per object";
  check "WV-3.2-meervoudig-invoer"
    (match
       Case.timeline_of_json ~at:"J1.blok" ~grid:plural_flag (Some num0)
         (Yojson.Safe.from_string {|[{"from":"2026-04-01","to":"2026-09-01","value":{"number":"1"}}]|})
     with
    | exception Case.Error { text = m; _ } -> substring m "a multi-period grid, which carries no operation"
    | _ -> false)

(* WV-3.2-zinnen: the sentences in which a grid without an operation is
   refused, whole and word for word, because the ASP backend and the Drools
   backend speak them too: at load for a case timeline, with its position,
   and at evaluation for the asOfDate bucket of a scalar case value. *)
let () =
  let load_doc ~at gr =
    match
      Case.timeline_of_json ~at ~grid:gr (Some num0)
        (Yojson.Safe.from_string {|[{"from":"2024-01-01","to":"2024-03-01","value":{"number":"1"}}]|})
    with
    | exception Case.Error { text = m; _ } -> m
    | _ -> "(geladen)"
  in
  let at_the_as_of_date gr =
    match Timeline.grid_bucket gr (day "2024-02-15") with
    | exception Value.Fault { text = m; _ } -> m
    | _ -> "(een periode)"
  in
  let plural_flag = Timeline.grid_of ~gran:Month ~count:2 ~fixed:(Some "2024-01-01") ~variable:false in
  let varying = Timeline.grid_of ~gran:Year ~count:1 ~fixed:None ~variable:true in
  check "WV-3.2-zinnen-meervoudig-invoer"
    (load_doc ~at:"J1.m2" plural_flag
    = "J1.m2: the declared grid is 2 month units, a multi-period grid, which carries no operation, a \
       case input included (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not \
       as a defect of the case");
  check "WV-3.2-zinnen-variabel-invoer"
    (load_doc ~at:"S1.waterstand" varying
    = "S1.waterstand: the declared grid is laid per object from the anchor of a variable start, which \
       this loader does not read (RUNTIME-SEMANTICS ch. 3.2): refused as a v0 limit, not as a defect of \
       the case");
  check "WV-3.2-zinnen-meervoudig-peildatum"
    (at_the_as_of_date plural_flag
    = "a multi-period grid (2 month periods) carries no operation (RUNTIME-SEMANTICS ch. 3.2, OPEN in \
       ch. 3.12)");
  check "WV-3.2-zinnen-variabel-peildatum"
    (at_the_as_of_date varying
    = "a variable-start grid is laid per object from the anchor its start-point determination binds, \
       which this interpreter does not execute (ch. 3.2)");
  check "WV-3.2-zinnen-kalenderweek-peildatum"
    (at_the_as_of_date (Timeline.calendar_grid Week) = "bucketing is defined for month and year only (ch. 3.2)")

(* WV-3.2-vast-prorata: the pro-rata part onto a target on a fixed start
   point, or from a base on one, is fenced (RUNTIME-SEMANTICS ch. 3.2): the
   base is constant across a bucket because its grid nests in the target's,
   and a start point off the calendar breaks that nesting. The calendar
   document beside them evaluates, so the fault is the fence's alone. The ASP
   backend's suite holds the same three documents. *)
let () =
  let facet gran frozen =
    Printf.sprintf {|{"node":"TimelineFacet","granularity":"%s","count":1%s}|} gran
      (match frozen with Some v -> Printf.sprintf {|,"fixedStart":"%s"|} v | None -> "")
  in
  let doc ~objective ~base_expr =
    Printf.sprintf
      {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"borgtarief","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"timeline":%s},{"node":"Attribute","name":"borgdeel","datatype":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"timeline":%s}],"characteristics":[{"node":"Characteristic","name":"borgplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"borgdeel van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"borgdeel","subjectType":"Jutter","source":{"snippet":"Het borgdeel van een Jutter"}},"value":{"node":"BuiltinCall","kind":"timeProportionalPart","arguments":[{"node":"AttributeReference","attribute":"borgtarief","subjectType":"Jutter","source":{"snippet":"zijn borgtarief"}}],"filter":{"node":"CharacteristicCheck","characteristic":"borgplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij borgplichtig is"}},"granularity":"month","type":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"source":{"snippet":"het tijdsevenredig deel per maand van zijn borgtarief gedurende de tijd dat hij borgplichtig is"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
      (facet "year" base_expr) (facet "month" objective)
  in
  let case_data van up_to =
    Printf.sprintf
      {|{"format":"uchar-case-v0","asOfDate":null,"input":{"instances":[{"label":"J1","objectType":"Jutter","attributes":{"borgtarief":{"timeline":[{"from":"%s","to":"%s","value":{"number":"12"}}]}},"roles":{},"characteristics":[],"characteristicTimelines":{"borgplichtig":[{"from":"2024-02-01","to":"2024-03-01"}]}}],"parameters":{}},"expected":[{"kind":"consistent"}]}|}
      van up_to
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-vast" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let evalueer ir case_text () =
    let irf = write ".ir.json" ir and casef = write ".case.json" case_text in
    Fun.protect
      ~finally:(fun () -> Sys.remove irf; Sys.remove casef)
      (fun () ->
        let doc = Ir_v0.Loader.load_file irf in
        let c = Case.load_file doc casef in
        ignore
          (Eval.evaluate
             { Eval.doc; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
               locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
             c.world))
  in
  let enclosed = "pro-rata onto or from a grid that does not lie on the calendar is fenced" in
  check "WV-3.2-vast-prorata-kalender"
    (match evalueer (doc ~objective:None ~base_expr:None) (case_data "2024-01-01" "2025-01-01") () with
    | () -> true
    | exception Value.Fault { text = _; _ } -> false);
  expect_fault_msg "WV-3.2-vast-prorata-doel"
    (evalueer (doc ~objective:(Some "--01-15") ~base_expr:None) (case_data "2024-01-01" "2025-01-01")) enclosed;
  expect_fault_msg "WV-3.2-vast-prorata-basis"
    (evalueer (doc ~objective:None ~base_expr:(Some "--10-01")) (case_data "2023-10-01" "2024-10-01")) enclosed

(* WF-115's sentences, which this interpreter's loader speaks when it refuses a
   fixed start point ([Loader.reader]): one per reason of arm (b), and arms (a)
   and (c). The golden and the three negative witnesses pin the verifier's
   side; these pin every reason of (b) the witnesses do not reach. *)
let () =
  let rejects g n s frag =
    match Ir_v0.Checks.fixed_start_point_refusal g n s with
    | Some m -> substring m frag
    | None -> false
  in
  let fits_in g n s = Ir_v0.Checks.fixed_start_point_refusal g n s = None in
  check "WF-115-b-dag"
    (rejects Day 1 "2026-01-05"
       "fixed start point '2026-01-05' does not fit a day grid of count 1: a day grid has a wall at \
        every day boundary and takes no start point");
  check "WF-115-b-datum-gevraagd"
    (rejects Week 1 "--01-05" "does not fit a week grid of count 1: a week grid and a grid of several units start on a date"
    && rejects Month 5 "--01-15" "does not fit a month grid of count 5"
    && rejects Week 1 "2026-02-30" "the ISO form of a day of the calendar");
  check "WF-115-b-dag-maand-gevraagd"
    (rejects Year 1 "2024-10-01" "starts on a day-month, written --mm-dd, a day of month mm in a leap year"
    && rejects Year 1 "--02-30" "a day of month mm in a leap year"
    && rejects Month 1 "--13-01" "a day of month mm in a leap year");
  check "WF-115-b-kalender"
    (rejects Year 1 "--01-01" "it lays the calendar's own grid, which is written without a fixedStart"
    && rejects Month 1 "--02-01" "it lays the calendar's own grid"
    && rejects Quarter 1 "--04-01" "it lays the calendar's own grid");
  check "WF-115-b-canoniek"
    (rejects Month 1 "--03-15" "its canonical spelling is --01-15"
    && rejects Quarter 1 "--04-15" "its canonical spelling is --01-15"
    && rejects Quarter 1 "--02-31" "a day of month mm in a leap year"
    && rejects Quarter 1 "--08-31" "its canonical spelling is --05-31");
  check "WF-115-b-past"
    (fits_in Year 1 "--10-01" && fits_in Year 1 "--02-29" && fits_in Month 1 "--01-31" && fits_in Quarter 1 "--05-31"
    && fits_in Quarter 1 "--02-15" && fits_in Week 1 "2026-01-05" && fits_in Month 5 "2026-04-01"
    && fits_in Day 5 "2026-04-01");
  check "WF-115-a"
    (Ir_v0.Checks.fixed_start_point_variable_refusal "stormjaar" "--10-01"
    = "timeline 'stormjaar' has a variable start and a fixedStart '--10-01': its periods run from the \
       anchor its start-point determination binds per object, and a timeline has one start");
  check "WF-115-c"
    (Ir_v0.Checks.fixed_start_point_adoption_refusal ~facet:None ~timeline_state:"stormjaar"
       ~from_timeline:(Some "--10-01")
    = "named-timeline adoption is partial: fixed start point none on the facet, '--10-01' on timeline \
       'stormjaar'")

(* WF-091's second line in this evaluator (ch. 3.6, 3.7, 1.2e). Since the
   card ref-klipvouw-nul, WF-091 (IR-SPEC ch. 4.1) demands a Number as the
   operand of `sum` and `total` and as the pro-rata base; the five documents
   that pinned these refusals as red cases are therefore negative witnesses
   without a case file, and a negative runs through no executor. The refusals
   stay as a second line with their sentences unchanged: the pro-rata fold
   names its base, and a total or sum over a Date faults on the
   multiplication or the addition itself. The corpus pins the verifier's
   refusal; these rows pin this evaluator's. Each row loads the negative's
   document (minified, equal as JSON) and the input of its former red case,
   and evaluates them as the runner does. *)
let () =
  let write suffix body_text =
    let f = Filename.temp_file "ref-wf091" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let evalueer ir case_text () =
    let irf = write ".ir.json" ir and casef = write ".case.json" case_text in
    Fun.protect
      ~finally:(fun () -> Sys.remove irf; Sys.remove casef)
      (fun () ->
        let doc = Ir_v0.Loader.load_file irf in
        let c = Case.load_file doc casef in
        ignore
          (Eval.evaluate
             { Eval.doc; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
               locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
             c.world))
  in
  let timeline_total =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"bewaarvergoeding per maand","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Attribute","name":"bewaargeld","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Munt","units":[{"node":"Unit","abbreviation":"EUR","system":"Munt","canonical":"EUR","factorToCanonical":{"num":1,"den":1},"spellings":["\u20ac","euro","euros"]}]},{"node":"UnitSystem","name":"Bewaartijd","units":[{"node":"Unit","abbreviation":"mnd","system":"Bewaartijd","canonical":"mnd","factorToCanonical":{"num":1,"den":1},"spellings":["maand"]}]}],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"bewaargeld van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"bewaargeld","subjectType":"Jutter","source":{"snippet":"Het bewaargeld van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"bewaarvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn bewaarvergoeding per maand"}}],"elementType":"Jutter","type":{"node":"Date","precision":"day"},"source":{"snippet":"het totaal van zijn bewaarvergoeding per maand"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let clip_fold =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"wachtvergoeding per maand","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}}],"characteristics":[{"node":"Characteristic","name":"wachtplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]},{"node":"ObjectType","name":"Klipjutter","supertype":"Jutter","animate":true,"plural":"Klipjutters","attributes":[{"node":"Attribute","name":"wachtsom","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"wachtsom van een klipjutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"wachtsom","subjectType":"Klipjutter","source":{"snippet":"De wachtsom van een Klipjutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Klipjutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Klipjutter","filter":{"node":"CharacteristicCheck","characteristic":"wachtplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij wachtplichtig is"}},"type":{"node":"Date","precision":"day"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is"}},"initialisation":false,"presentation":"equation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let prorata =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"borgtarief","datatype":{"node":"Date","precision":"day"},"timeline":{"node":"TimelineFacet","granularity":"year","count":1}},{"node":"Attribute","name":"borgdeel","datatype":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}}],"characteristics":[{"node":"Characteristic","name":"borgplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"borgdeel van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"borgdeel","subjectType":"Jutter","source":{"snippet":"Het borgdeel van een Jutter"}},"value":{"node":"BuiltinCall","kind":"timeProportionalPart","arguments":[{"node":"AttributeReference","attribute":"borgtarief","subjectType":"Jutter","source":{"snippet":"zijn borgtarief"}}],"filter":{"node":"CharacteristicCheck","characteristic":"borgplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij borgplichtig is"}},"granularity":"month","type":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"},"source":{"snippet":"het tijdsevenredig deel per maand van zijn borgtarief gedurende de tijd dat hij borgplichtig is"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let set_source_sum =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"vondstdagensom","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]},{"node":"ObjectType","name":"Vondst","animate":false,"plural":"Vondsten","attributes":[{"node":"Attribute","name":"vondstdatum","datatype":{"node":"Date","precision":"day"},"plural":"vondstdata"}],"characteristics":[]}],"factTypes":[{"node":"FactType","name":"vondstaanmelding","reciprocal":false,"roles":[{"node":"Role","name":"aanmelder","objectType":"Jutter","many":false},{"node":"Role","name":"vondst","objectType":"Vondst","plural":"vondsten","many":true}]}],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"vondstdagensom van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"vondstdagensom","subjectType":"Jutter","source":{"snippet":"De vondstdagensom van een Jutter"}},"value":{"node":"Aggregation","kind":"sum","arguments":[{"node":"AttributeReference","attribute":"vondstdatum","subjectType":"Vondst","source":{"snippet":"de vondstdatum van alle vondsten van de Jutter"}}],"elementType":"Vondst","sourceRole":{"node":"RoleBinding","factType":"vondstaanmelding","role":"vondst"},"type":{"node":"Date","precision":"day"},"source":{"snippet":"de som van de vondstdatum van alle vondsten van de Jutter"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let set_source_total =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"vondstdagentotaal","datatype":{"node":"Date","precision":"day"}}],"characteristics":[]},{"node":"ObjectType","name":"Vondst","animate":false,"plural":"Vondsten","attributes":[{"node":"Attribute","name":"vondstdatum","datatype":{"node":"Date","precision":"day"},"plural":"vondstdata"}],"characteristics":[]}],"factTypes":[{"node":"FactType","name":"vondstaanmelding","reciprocal":false,"roles":[{"node":"Role","name":"aanmelder","objectType":"Jutter","many":false},{"node":"Role","name":"vondst","objectType":"Vondst","plural":"vondsten","many":true}]}],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"vondstdagentotaal van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"vondstdagentotaal","subjectType":"Jutter","source":{"snippet":"Het vondstdagentotaal van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"vondstdatum","subjectType":"Vondst","source":{"snippet":"de vondstdatum van alle vondsten van de Jutter"}}],"elementType":"Vondst","sourceRole":{"node":"RoleBinding","factType":"vondstaanmelding","role":"vondst"},"type":{"node":"Date","precision":"day"},"source":{"snippet":"het totaal van de vondstdatum van alle vondsten van de Jutter"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let set_source_case =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
      { "label": "J1", "objectType": "Jutter", "attributes": {},
        "roles": { "vondsten": [ "V1", "V2" ] }, "characteristics": [] },
      { "label": "V1", "objectType": "Vondst", "attributes": { "vondstdatum": { "date": "2024-01-15" } },
        "roles": {}, "characteristics": [] },
      { "label": "V2", "objectType": "Vondst", "attributes": { "vondstdatum": { "date": "2024-01-15" } },
        "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
  in
  expect_fault_msg "WF-091-tweede-linie-tijdlijntotaal-datum"
    (evalueer timeline_total
       {|{ "format": "uchar-case-v0", "asOfDate": "2026-03-15",
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
      "attributes": { "bewaarvergoeding per maand": { "date": "2024-01-15" } },
      "roles": {}, "characteristics": [] } ], "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|})
    "maal: expected a number, got 2024-01-15";
  expect_fault_msg "WF-091-tweede-linie-klipvouw-datum"
    (evalueer clip_fold
       {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "K1", "objectType": "Klipjutter",
      "attributes": { "wachtvergoeding per maand": { "timeline": [
        { "from": "2023-07-01", "to": "2023-09-01", "value": { "date": "2024-01-15" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "wachtplichtig": [ { "from": "2023-07-01", "to": "2023-08-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|})
    "maal: expected a number, got 2024-01-15";
  expect_fault_msg "WF-091-tweede-linie-prorata-datumbasis"
    (evalueer prorata
       {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
      "attributes": { "borgtarief": { "timeline": [
        { "from": "2024-01-01", "to": "2025-01-01", "value": { "date": "2024-01-18" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "borgplichtig": [ { "from": "2024-01-01", "to": "2024-02-01" } ] } } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|})
    "pro-rata base is not numeric: 2024-01-18";
  expect_fault_msg "WF-091-tweede-linie-setbron-som-datum" (evalueer set_source_sum set_source_case)
    "plus: expected a number, got 2024-01-15";
  expect_fault_msg "WF-091-tweede-linie-setbron-totaal-datum" (evalueer set_source_total set_source_case)
    "plus: expected a number, got 2024-01-15"

(* --- ch. 9: sum types and match (RUNTIME-SEMANTICS ch. 9) ---------------
   Operator-level worked examples. The 9.1 unary Construct build with its
   leeg-argument propagation, and the 9.2 whole-match evaluation, need a
   document; the anchor jutterswet-beschikkingsom pins those through gates
   5-6 (uitkering := match beschikking: case1-3 the filled arms, case4 the
   leeg-scrutinee corner; herbeschikking := Toegekend(borg): case1 the unary
   build Toegekend(300.00), cases 2-4 the leeg-argument corner). Here: the
   value level — the 9.1 rendering and the 9.3 equality corners, verbatim on
   the anchor's beschikking = Toegekend(bedrag) | Afgewezen | Aangehouden. *)
let ctor tag p = Value.Ctor (tag, p)

let () =
  let toegekend = ctor "Toegekend" (Some (n "1250.50")) in
  let toegekend' = ctor "Toegekend" (Some (n "1250.50")) in
  let toegekend_other = ctor "Toegekend" (Some (n "1000.00")) in
  let afgewezen = ctor "Afgewezen" None in
  let aangehouden = ctor "Aangehouden" None in
  (* 9.1: the deterministic constructor rendering — the payload uses the
     canonical decimal lexeme (lexeme_of_q, trailing zero dropped), the
     same form used everywhere a value is rendered *)
  check "WV-9.1-render-unary" (Value.to_string toegekend = "Toegekend(1250.5)");
  check "WV-9.1-render-nullary" (Value.to_string afgewezen = "Afgewezen");
  (* 9.3: structural equality — equal tag AND equal payload *)
  check "WV-9.3-struct-eq" (Arith.eq_non_numeric toegekend toegekend' = true);
  (* 9.3: same tag, unequal payload → not equal *)
  check "WV-9.3-struct-neq-payload" (Arith.eq_non_numeric toegekend toegekend_other = false);
  (* 9.3: unequal tags → not equal (unary vs nullary, and nullary vs nullary) *)
  check "WV-9.3-neq-tag-unary-nullary" (Arith.eq_non_numeric toegekend afgewezen = false);
  check "WV-9.3-neq-tag-nullary" (Arith.eq_non_numeric afgewezen aangehouden = false);
  (* the idempotent-write equality agrees (a payload is never leeg) *)
  check "WV-9.3-equal-struct-eq" (Arith.equal toegekend toegekend' = true);
  check "WV-9.3-equal-struct-neq" (Arith.equal toegekend toegekend_other = false);
  (* 9.3 through the comparison operator (sum types are non-numeric): the
     anchor's rule 2 (= Aangehouden) and rule 3 (≠ Aangehouden) *)
  check "WV-9.3-eq-same-tag"
    (Arith.compare_op ~static_numeric:false Equal aangehouden aangehouden = true);
  check "WV-9.3-neq-same-tag"
    (Arith.compare_op ~static_numeric:false Not_equal aangehouden aangehouden = false);
  check "WV-9.3-eq-diff-tag"
    (Arith.compare_op ~static_numeric:false Equal toegekend aangehouden = false);
  check "WV-9.3-neq-diff-tag"
    (Arith.compare_op ~static_numeric:false Not_equal toegekend aangehouden = true);
  (* 9.3: a leeg operand makes `=` false (Tabel 38, equality untouched) but
     `≠` WAAR (Tabel 44, uniform inequality — the ratified 2026-08-28 change;
     the anchor's case 4 now fires afgehandeld), in both operand orders *)
  check "WV-9.3-eq-empty-l"
    (Arith.compare_op ~static_numeric:false Equal empty_value aangehouden = false);
  check "WV-9.3-neq-empty-l"
    (Arith.compare_op ~static_numeric:false Not_equal empty_value aangehouden = true);
  check "WV-9.3-eq-empty-r"
    (Arith.compare_op ~static_numeric:false Equal aangehouden empty_value = false);
  check "WV-9.3-neq-empty-r"
    (Arith.compare_op ~static_numeric:false Not_equal aangehouden empty_value = true);
  (* 9.3: both operands leeg → `≠` onwaar (Tabel 44 both-empty cell) *)
  check "WV-9.3-neq-both-empty"
    (Arith.compare_op ~static_numeric:false Not_equal empty_value empty_value = false);
  (* 9.3: eq_non_numeric with an empty operand is false directly *)
  check "WV-9.3-teq-empty" (Arith.eq_non_numeric empty_value aangehouden = false);
  (* 9.3: sum values are not orderable — scalar_cmp faults *)
  expect_fault "WV-9.3-order-fault"
    (fun () -> Arith.compare_op ~static_numeric:false Less toegekend aangehouden)

(* --- ch. 9.2 (enumeration scrutinee): the keuzetype family's other wire
   variant (IR-SPEC ch. 2.4). The chapter's worked example verbatim —
   `waarde` := match `staat`: gaaf -> 10.00, beschadigd -> 4.00,
   wrakhout -> 1.00 over the three-value `keurklasse` domain — evaluated
   against a document, because the leeg corner is the point: an
   enumeration-typed slot CAN simply hold leeg (a case that does not supply
   the attribute), where a sum-typed slot only reaches leeg by propagation.
   Nothing is written there — not the last arm, not 0.00. Card-sanctioned
   test touch (20260901-keuzetype-familie). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let keurklasse : Ir_v0.Ast.datatype =
    Enumeration
      { domain_spec = Some "keurklasse"; label_list = [ Some "gaaf"; Some "beschadigd"; Some "wrakhout" ] }
  in
  let enum_match : Ir_v0.Ast.expr =
    Match
      { typ = Some num2;
        scrutinee = aref "staat" "Vondst";
        arms =
          [ ("gaaf", None, litv "10.00" num2);
            ("beschadigd", None, litv "4.00" num2);
            ("wrakhout", None, litv "1.00" num2) ];
        source_name = None }
  in
  let doc =
    { empty_doc with
      domain_specs = [ ("keurklasse", Some keurklasse) ];
      object_types =
        [ ot "Vondst" [ ("staat", Some keurklasse); ("waarde", Some num2) ] [] ] }
  in
  let env = env_of doc in
  let eval status =
    let attrs = match status with None -> [] | Some s -> [ ("staat", Value.Enum s) ] in
    let w = World.add_instance World.empty (inst "V1" "Vondst" attrs) in
    Eval.eval_expr env w (World.get w "V1") ~err:no_err enum_match
  in
  expect_num "WV-9.2-enum-gaaf" (eval (Some "gaaf")) "10.00";
  expect_num "WV-9.2-enum-beschadigd" (eval (Some "beschadigd")) "4.00";
  expect_num "WV-9.2-enum-wrakhout" (eval (Some "wrakhout")) "1.00";
  (* the label match is ci — the norm WF-063 checked the arms under *)
  expect_num "WV-9.2-enum-ci" (eval (Some "GAAF")) "10.00";
  (* step 1: leeg in, leeg out — no implicit leeg-arm, no coalesce *)
  expect_empty "WV-9.2-enum-leeg" (eval None)

(* --- ch. 10: folds and function values (RUNTIME-SEMANTICS ch. 10) -------
   Value-level worked examples: list rendering (10.1) and the list-equality
   corners — elementwise and IN ORDER, with the Tabel 38 Empty corners ([] is
   a value, leeg is its absence). The evaluation of the fold family over
   documents is pinned absolutely by the jutterswet-staffelvergoeding and
   jutterswet-aanspraaktelling anchors through gates 5-6 (case1 the deciding
   fold, case2 the empty-list value writing 0.00/0, case3 the leeg-propagation
   survivor writing nothing). *)
let element_list xs = Value.List_value xs

let () =
  let l1 = element_list [ n "12.50"; n "30.00"; n "7.25" ] in
  let l1' = element_list [ n "12.50"; n "30.00"; n "7.25" ] in
  let l_reordered = element_list [ n "30.00"; n "12.50"; n "7.25" ] in
  let l_short = element_list [ n "12.50"; n "30.00" ] in
  (* 10.1: the deterministic list rendering (canonical trailing-zero-dropped
     lexemes, the same form used everywhere a value renders) *)
  check "WV-10.1-render" (Value.to_string l1 = "[12.5, 30, 7.25]");
  check "WV-10.1-render-empty" (Value.to_string (element_list []) = "[]");
  (* 10.1: equality is elementwise AND in order — the load-bearing distinction
     the staffelvergoeding anchor pins (reordered would fold to 37.25) *)
  check "WV-10.1-eq" (Arith.equal l1 l1' = true);
  check "WV-10.1-eq-order" (Arith.equal l1 l_reordered = false);
  check "WV-10.1-eq-length" (Arith.equal l1 l_short = false);
  check "WV-10.1-eq-empty-list" (Arith.equal (element_list []) (element_list []) = true);
  (* 10.1: the non-numeric equality path agrees (a position is never leeg) *)
  check "WV-10.1-teq" (Arith.eq_non_numeric l1 l1' = true);
  check "WV-10.1-teq-order" (Arith.eq_non_numeric l1 l_reordered = false);
  (* Tabel 38: a leeg operand makes eq_non_numeric false in BOTH directions;
     and the empty list [] (a value) is never equal to leeg (its absence) *)
  check "WV-10.1-teq-empty-l" (Arith.eq_non_numeric empty_value l1 = false);
  check "WV-10.1-teq-empty-r" (Arith.eq_non_numeric l1 empty_value = false);
  check "WV-10.1-eq-empty-vs-nil" (Arith.equal empty_value (element_list []) = false);
  (* composing ch. 9: a list of constructor values (the aanspraaktelling
     shape) — equal to itself, order-sensitive, never equal reordered *)
  let a1 =
    element_list [ ctor "Erkend" (Some (n "10.00")); ctor "Vervallen" None; ctor "Erkend" (Some (n "2.50")) ]
  in
  let a1' =
    element_list [ ctor "Erkend" (Some (n "10.00")); ctor "Vervallen" None; ctor "Erkend" (Some (n "2.50")) ]
  in
  let a_reordered =
    element_list [ ctor "Vervallen" None; ctor "Erkend" (Some (n "10.00")); ctor "Erkend" (Some (n "2.50")) ]
  in
  check "WV-10.1-ctor-eq" (Arith.equal a1 a1' = true);
  check "WV-10.1-ctor-eq-order" (Arith.equal a1 a_reordered = false);
  check "WV-10.1-ctor-render" (Value.to_string a1 = "[Erkend(10), Vervallen, Erkend(2.5)]")

(* --- ch. 10.1/10.3/10.5: absent-parameter binding at eval level ---------
   The closed-body leeg-free invariant hinges on where an absence enters: a
   declared-but-absent SCALAR parameter FAULTS (fail-fast, ch. 5.7), while
   only an absent LIST parameter reads leeg (source position — the
   staffelvergoeding case3 propagation the Fold needs). Narrowing leeg-on-
   absent to lists is what keeps ref and clingo in step: a scalar absence
   read leeg would fire ref's fold-body else-branch where clingo derives
   nothing (the empirically observed divergence this test pins shut). *)
let () =
  let param name dt : Ir_v0.Ast.parameter =
    { par_name = Some name; par_datatype = Some dt; par_granularity = None; par_timeline_count = None;
      par_fixed_start_point = None }
  in
  let doc =
    {
      empty_doc with
      object_types = [ ot "J" [] [] ];
      parameters =
        [ ("p", param "p" num0); ("lst", param "lst" (List_type { element = num0 })) ];
    }
  in
  (* env_of leaves params empty, so both declared parameters are absent *)
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "j" "J" []) in
  let self = World.get w "j" in
  let pref name : Ir_v0.Ast.expr = Parameter_ref { parameter = Some name; source_name = None } in
  expect_fault "WV-5.7-scalar-absent-fault"
    (fun () -> Eval.eval_expr env w self ~err:no_err (pref "p"));
  expect_empty "WV-10.3-list-absent-leeg" (Eval.eval_expr env w self ~err:no_err (pref "lst"))

(* --- ch. 11: record values (RUNTIME-SEMANTICS ch. 11) -------------------
   Value-level worked examples on the anchor's dossier struct (fields
   waarde, telling): the 11.1 rendering and the 11.2 equality — structural
   AND nominal. ch. 11.2 fixes the comparison to be POSITIONAL over the
   shared declaration order with field names matched ci; it is NOT
   field-order-independent (the "veldvolgorde-onafhankelijkheid? nee"
   point). The Tabel 38 Empty corners: a leeg operand makes both = and ≠
   false. The evaluation over documents — StructLiteral no-hole,
   FieldAccess-on-leeg, the nominal GELIJK/ONGELIJK rules — is pinned
   absolutely by the jutterswet-vondstdossier anchor through gates 5-6
   (case1/2 dubbel, case3 the both-directions-false corner and the
   build-side no-hole leeg). *)
let record fs = Value.Record fs

let () =
  let d1 = record [ ("waarde", n "120.50"); ("telling", n "3") ] in
  let d1' = record [ ("waarde", n "120.50"); ("telling", n "3") ] in
  (* 11.1: the deterministic record rendering — canonical trailing-zero-
     dropped lexemes, fields in declaration order *)
  check "WV-11.1-render"
    (Value.to_string (record [ ("waarde", n "125.50"); ("telling", n "2") ])
     = "{waarde: 125.5, telling: 2}");
  (* 11.2: structural equality — fieldwise, every field equal *)
  check "WV-11.2-eq" (Arith.equal d1 d1' = true);
  (* 11.2: a differing field value → not equal *)
  let d_val = record [ ("waarde", n "125.50"); ("telling", n "3") ] in
  check "WV-11.2-neq-value" (Arith.equal d1 d_val = false);
  (* 11.2: field names are matched ci by declaration — same order, ci-variant
     names, equal values → equal (nominal names, not case-sensitive) *)
  let d_ci = record [ ("Waarde", n "120.50"); ("Telling", n "3") ] in
  check "WV-11.2-eq-ci-names" (Arith.equal d1 d_ci = true);
  (* 11.2: the comparison is POSITIONAL over the shared declaration order —
     NOT field-order-independent: a record whose fields sit in a different
     order is a different construction and compares unequal (ch. 11.2 fixes
     the order; the loader always builds in declaration order) *)
  let d_reordered = record [ ("telling", n "3"); ("waarde", n "120.50") ] in
  check "WV-11.2-neq-reordered" (Arith.equal d1 d_reordered = false);
  (* 11.2: the non-numeric equality path agrees (a field is never leeg) *)
  check "WV-11.2-teq" (Arith.eq_non_numeric d1 d1' = true);
  check "WV-11.2-teq-neq" (Arith.eq_non_numeric d1 d_val = false);
  (* 11.2 through the comparison operator (records are non-numeric): the
     anchor's dubbel (= dossier) and afwijkend (≠ dossier) rules *)
  check "WV-11.2-eq-op" (Arith.compare_op ~static_numeric:false Equal d1 d1' = true);
  check "WV-11.2-neq-op" (Arith.compare_op ~static_numeric:false Not_equal d1 d1' = false);
  check "WV-11.2-eq-op-diff" (Arith.compare_op ~static_numeric:false Equal d1 d_val = false);
  check "WV-11.2-neq-op-diff" (Arith.compare_op ~static_numeric:false Not_equal d1 d_val = true);
  (* 11.2: a leeg operand makes `=` false (Tabel 38, equality untouched) but
     `≠` WAAR (Tabel 44, uniform inequality — the ratified 2026-08-28 change;
     the anchor's case3 keeps dubbel absent yet now fires afwijkend), in both
     operand orders; both operands leeg → `≠` onwaar *)
  check "WV-11.2-eq-empty-l" (Arith.compare_op ~static_numeric:false Equal empty_value d1 = false);
  check "WV-11.2-neq-empty-l" (Arith.compare_op ~static_numeric:false Not_equal empty_value d1 = true);
  check "WV-11.2-eq-empty-r" (Arith.compare_op ~static_numeric:false Equal d1 empty_value = false);
  check "WV-11.2-neq-empty-r" (Arith.compare_op ~static_numeric:false Not_equal d1 empty_value = true);
  check "WV-11.2-neq-both-empty" (Arith.compare_op ~static_numeric:false Not_equal empty_value empty_value = false);
  check "WV-11.2-teq-empty" (Arith.eq_non_numeric empty_value d1 = false);
  (* 11.2: records are not orderable — scalar_cmp faults *)
  expect_fault "WV-11.2-order-fault"
    (fun () -> Arith.compare_op ~static_numeric:false Less d1 d1');
  (* composing ch. 10: a list of records (the anchor's dossiers input) —
     equal to itself, order-sensitive, never equal reordered *)
  let ds =
    element_list
      [ record [ ("waarde", n "10.00"); ("telling", n "1") ];
        record [ ("waarde", n "2.50"); ("telling", n "2") ] ]
  in
  let ds' =
    element_list
      [ record [ ("waarde", n "10.00"); ("telling", n "1") ];
        record [ ("waarde", n "2.50"); ("telling", n "2") ] ]
  in
  let ds_reordered =
    element_list
      [ record [ ("waarde", n "2.50"); ("telling", n "2") ];
        record [ ("waarde", n "10.00"); ("telling", n "1") ] ]
  in
  check "WV-11.2-list-eq" (Arith.equal ds ds' = true);
  check "WV-11.2-list-eq-order" (Arith.equal ds ds_reordered = false);
  check "WV-11.2-list-render"
    (Value.to_string ds = "[{waarde: 10, telling: 1}, {waarde: 2.5, telling: 2}]")

(* --- ch. 10.6: the call-by-need Let (RUNTIME-SEMANTICS ch. 10.6; PHASE5C §6,
   the L unification ratified 2026-09-01) ---------------------------------
   The worked examples pinning the discipline the jutterswet-tariefstelling
   anchor rests on:
   (1) binder-lookup — a bound value is read through BinderRef in the body;
   (2) the clause-2/clause-3 PAIR that distinguishes call-by-need from the
       strict binding this rule used to state: a leeg value under a READ
       binder empties the whole Let (the barrier, at the read), while a leeg
       value under an UNREAD binder is never evaluated at all and the body's
       outcome is the Let's;
   (3) the maal-coalesce CONTRAST — a bare AttribuutRef reading leeg times a
       literal coalesces to 0 (the ch. 1.2 coalesce table's maal row),
       whereas the SAME read routed through a Let binding propagates
       leeg: the very reason the discipline exists (anchor case3);
   (4) shadowing — a nested Let reusing a binder name: the inner binding
       wins inside its body and the outer is restored once that body ends;
   (5) shadowing across a fold parameter — a Let in the lambda body reusing
       the fold's element binder wins over it (ref semantics; the ASP side
       carries a Let only in the bare rule value, ASP-TRANSLATION S19);
   (i)–(iv) the four L witnesses, each replayed differentially against clingo:
       a depositing value under an unread binder deposits nothing, the same
       value under a read binder deposits once and ends the Let, a leeg value
       read only in the branch the
       condition does NOT choose leaves the chosen branch standing, and the
       need is evaluated exactly ONCE however often it is read. *)
let () =
  let binder_ref name : Ir_v0.Ast.expr = Binder_ref { ident = name; source_name = None } in
  let letexpr binder value body typ : Ir_v0.Ast.expr =
    Let { typ = Some typ; binder; value; body; source_name = None }
  in
  let times l r : Ir_v0.Ast.expr =
    Binary { op = Times; left_side = Some l; right_side = Some r; typ = Some num0; source_name = None }
  in
  let doc =
    { empty_doc with
      object_types = [ ot "J" [ ("a", Some num0); ("g", Some num0); ("d", Some num0) ] [] ] }
  in
  let env = env_of doc in
  (* the instance leaves "a" and "d" absent (a bare read is leeg) and supplies
     "g" = 100 — the filled dividend witness (iv)'s channel deposit needs *)
  let w = World.add_instance World.empty (inst "j" "J" [ ("g", n "100") ]) in
  let self = World.get w "j" in
  let a_empty = aref "a" "J" in
  (* (1) let x = 7 in x -> 7 *)
  expect_num "WV-10.6-binder-lookup"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" (litv "7" num0) (binder_ref "x") num0))
    "7";
  (* (2a) clause 2 — a leeg value under a READ binder: the barrier fires at the
     read and the WHOLE Let is leeg (so a body read never sees leeg) *)
  expect_empty "WV-10.6-leeg-value-gelezen-binder"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" a_empty (binder_ref "x") num0));
  (* (2b) clause 3 — the SAME leeg value under an UNREAD binder: nothing is
     evaluated and the body's outcome IS the Let's. Under the strict binding
     this rule stated before the L unification, this was leeg. *)
  expect_num "WV-10.6-leeg-value-ongelezen-binder"
    (Eval.eval_expr env w self ~err:no_err (letexpr "x" a_empty (litv "42" num0) num0))
    "42";
  (* (3a) the bare maal coalesces the leeg read to 0 (ch. 1.2a) ... *)
  expect_num "WV-10.6-maal-coalesce-bare"
    (Eval.eval_expr env w self ~err:no_err (times a_empty (litv "5" num0)))
    "0";
  (* (3b) ... but the SAME read through a Let binding propagates leeg — the
     barrier at the READ (ch. 10.6 clause 2) *)
  expect_empty "WV-10.6-let-barrier"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" a_empty (times (binder_ref "x") (litv "5" num0)) num0));
  (* (4) shadowing: let x = 2 in (let x = 3 in x) maal x  ==  3 × 2 = 6
     (an inner leak would give 3×3 = 9; no shadowing would give 2×2 = 4) *)
  expect_num "WV-10.6-shadow-inner-wins"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" (litv "2" num0)
          (times (letexpr "x" (litv "3" num0) (binder_ref "x") num0) (binder_ref "x"))
          num0))
    "6";
  (* (5) shadowing across a fold parameter: fold(+, 0, [1;2;3]) with body
     `let x = 100 in acc + x` totals 0+100+100+100 = 300 — the Let's x wins
     over the fold's element binder x (were it not to, the total would be 6) *)
  let plus l r : Ir_v0.Ast.expr =
    Binary { op = Plus; left_side = Some l; right_side = Some r; typ = Some num0; source_name = None }
  in
  let shadow_fold : Ir_v0.Ast.expr =
    Fold
      { typ = Some num0;
        list =
          List_literal
            { typ = Some (List_type { element = num0 });
              elements = [ litv "1" num0; litv "2" num0; litv "3" num0 ];
              source_name = None };
        function_name =
          Fn_lambda
            { parameters = [ ("acc", num0); ("x", num0) ];
              body =
                letexpr "x" (litv "100" num0)
                  (plus (binder_ref "acc") (binder_ref "x"))
                  num0;
              source_name = None };
        init = litv "0" num0;
        source_name = None }
  in
  expect_num "WV-10.6-shadow-fold-parameter"
    (Eval.eval_expr env w self ~err:no_err shadow_fold)
    "300";
  (* --- the four L witnesses (ref ≡ clingo differentially on each) --------- *)
  let divide_by_zero : Ir_v0.Ast.expr =
    Binary
      { op = Divided_by; left_side = Some (litv "100" num0); right_side = Some (litv "0" num0);
        typ = Some num0; source_name = None }
  in
  (* (i) clause 3 with a DEPOSITING value: `let q = 100 / 0 in 42` never
     evaluates the division, so it yields 42 and deposits no §9.5 reason.
     Until v1 routed the numeric zero into the channel (ch. 1.4 point 4) the
     value faulted, and this row pinned that the fault did not happen *)
  (let r, deposits =
     with_errs (fun err ->
         Eval.eval_expr env w self ~err (letexpr "q" divide_by_zero (litv "42" num0) num0))
   in
   expect_num "WV-10.6-ongelezen-faultende-value" r "42";
   check "WV-10.6-ongelezen-faultende-value (no §9.5 reason)" (deposits = 0));
  (* (ii) the same value READ: the division runs and deposits its one reason,
     EF-005, and its need is leeg, so the read ends the Let (clause 2) *)
  (let codes = ref [] in
   let r =
     Eval.eval_expr env w self
       ~err:(fun (d : Value.deposit) -> codes := d.code :: !codes)
       (letexpr "q" divide_by_zero (binder_ref "q") num0)
   in
   expect_empty "WV-10.6-gelezen-faultende-value" r;
   check "WV-10.6-gelezen-faultende-value (§9.5 EF-005)" (!codes = [ Some "EF-005" ]));
  (* (iii) a leeg value whose binder is read ONLY in the branch the condition
     does not choose: `let k = 2 in let x = a in if k > 1 then 7 else x maal 2`
     -> 7. The condition is read on EVERY branch (k), the else-branch operand on
     none — under the strict binding the whole Let was leeg here. *)
  let greater l r : Ir_v0.Ast.condition_expr =
    Comparison { op = Greater; left_side = Some l; right_side = Some r; time_period = None; source_name = None }
  in
  let probe : Ir_v0.Ast.expr =
    letexpr "k" (litv "2" num0)
      (letexpr "x" a_empty
         (If_then_else
            { typ = Some num0;
              condition = greater (binder_ref "k") (litv "1" num0);
              then_ = litv "7" num0;
              else_ = times (binder_ref "x") (litv "2" num0);
              source_name = None })
         num0)
      num0
  in
  expect_num "WV-10.6-ongelezen-in-niet-gekozen-tak"
    (Eval.eval_expr env w self ~err:no_err probe)
    "7";
  (* (iv) call-by-need: the need is evaluated ONCE however often it is read.
     The need must be GEVULD for the question to arise at all. A LEEG need
     ends the Let at the FIRST read (clause 2), so a second read never happens
     and one evaluation is forced by the barrier, not by the memo — such a
     probe witnesses nothing. So: a filled dividend over an EMPTY divisor
     (ch. 5.6/§9.5 — it deposits a channel reason and yields no value, which
     makes the deposit COUNT the evaluation count), and a `plus 0` that
     COALESCES that leeg to a filled 0 (the ch. 1.2 plus row). The read then
     hands back 0, the barrier stays silent, and the SECOND read really
     happens — yet exactly ONE reason is deposited. Drop the memo and this
     very expression deposits TWO. *)
  let division_empty_divisor_coalesced : Ir_v0.Ast.expr =
    plus
      (Binary
         { op = Divided_by; left_side = Some (aref "g" "J"); right_side = Some (aref "d" "J");
           typ = Some num0; source_name = None })
      (litv "0" num0)
  in
  let twice, n_twice =
    with_errs (fun err ->
        Eval.eval_expr env w self ~err
          (letexpr "q" division_empty_divisor_coalesced
             (plus (binder_ref "q") (binder_ref "q")) num0))
  in
  let once, n_once =
    with_errs (fun err ->
        Eval.eval_expr env w self ~err
          (letexpr "q" division_empty_divisor_coalesced (binder_ref "q") num0))
  in
  (* both reads landed: 0 + 0 = 0, not the leeg a barrier would have produced *)
  expect_num "WV-10.6-need-kanaalfeit-tweemaal-gelezen" twice "0";
  check "WV-10.6-need-eenmaal-geevalueerd" (n_twice = 1 && n_once = 1 && twice = once)

(* --- ch. 2.9: the afrondingsnorm — exact to the rounding point
   (RUNTIME-SEMANTICS ch. 2.9; maintainer decision 2026-08-24, vraag 3-A).
   Intermediate values are exact-rational (ch. 2.1); a declared scale binds a
   declaration and the write boundary it guards (ch. 2.4), never an
   intermediate step; the sole collapse in this witness is its endpoint
   Afgerond node (ch. 2.5; ch. 2.1 also admits the ch. 2.2 abs-division,
   not exercised here). Witness: the non-terminating quotient 100 ÷ 3 = 33,333…, bound
   in a Let whose value is DECLARED at two decimals, read at the
   endpoint through a single Rounding. Rounded to 2 the endpoint is 33,33;
   rounded to 4 it is 33,3333 — not 33,3300, the value a scale that bound the
   intermediate step would have forced (ref ignores the binder's declared
   scale and carries the exact rational to the endpoint). clingo materialises
   the binder at that declared scale and refuses this very form loudly
   (ASP-TRANSLATION S19): a documented backend limit, conform to the norm,
   never a wrong value. Card-sanctioned test touch (20260824-afrondingsnorm). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let binder_ref name : Ir_v0.Ast.expr = Binder_ref { ident = name; source_name = None } in
  let portion : Ir_v0.Ast.expr =
    Binary
      { op = Divided_by;
        left_side = Some (litv "100" num0);
        right_side = Some (litv "3" num0);
        typ = Some num2;
        source_name = None }
  in
  let let_round dec : Ir_v0.Ast.expr =
    Let
      { typ = Some num2;
        binder = "deel";
        value = portion;
        body =
          Rounding
            { base_expr = Some (binder_ref "deel");
              mode_kind = Half_away_from_zero;
              decimal_places = dec;
              typ = Some num2;
              source_name = None };
        source_name = None }
  in
  let doc = { empty_doc with object_types = [ ot "J" [ ("a", Some num0) ] [] ] } in
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "j" "J" []) in
  let self = World.get w "j" in
  (* endpoint rounded to 2 decimals: the exact 100/3 collapses once → 33,33 *)
  expect_num "WV-2.9-let-endpoint-round"
    (Eval.eval_expr env w self ~err:no_err (let_round 2))
    "33.33";
  (* endpoint rounded to 4: 33,3333 — the exact rational survived the strict
     Let; a scale binding the intermediate step would have pre-collapsed it to
     33,33 so this endpoint would read 33,3300 (== 33,33) *)
  expect_num "WV-2.9-let-exact-intermediate"
    (Eval.eval_expr env w self ~err:no_err (let_round 4))
    "33.3333"

(* --- ch. 2.9 × ch. 9.2: the afrondingsnorm across a Match-arm result.
   The ch. 2.9 norm names the Match-arm result (ch. 9.2) as a second carrier
   of an exact, non-terminating intermediate: the strict-Let witness above
   pins the Let carrier, this one pins the Match arm. Witness: 100 ÷ 3 =
   33,333… is the payload of a Toegekend construct; the Match binds it to the
   arm binder [portion] — DECLARED at two decimals, as the Let binder was — and
   reads it at the endpoint through a single Rounding. The binder's declared
   scale never collapses the intermediate: the exact rational crosses the arm
   (ch. 9.2 binds the payload value verbatim) and the sole endpoint Rounding
   is the one collapse → 33,33; had the arm binding pre-rounded to two
   decimals this would still read 33,33, but the ch. 2.9 norm forbids that
   intermediate collapse and the exact carry is what this pins. Card-
   sanctioned test touch (20260824-afrondingsnorm). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let binder_ref name : Ir_v0.Ast.expr = Binder_ref { ident = name; source_name = None } in
  let beschikking : Ir_v0.Ast.datatype =
    Sum_type
      { domain_spec = "beschikking";
        constructor_list = [ ("Toegekend", Some num2); ("Afgewezen", None) ] }
  in
  let portion : Ir_v0.Ast.expr =
    Binary
      { op = Divided_by;
        left_side = Some (litv "100" num0);
        right_side = Some (litv "3" num0);
        typ = Some num2;
        source_name = None }
  in
  (* scrutinee: Toegekend(100 ÷ 3) — the non-terminating quotient is the payload *)
  let scrutinee : Ir_v0.Ast.expr =
    Construct
      { typ = Some beschikking;
        constructor_ = "Toegekend";
        argument = Some portion;
        source_name = None }
  in
  let match_round : Ir_v0.Ast.expr =
    Match
      { typ = Some num2;
        scrutinee;
        arms =
          [ ( "Toegekend",
              Some "deel",
              Rounding
                { base_expr = Some (binder_ref "deel");
                  mode_kind = Half_away_from_zero;
                  decimal_places = 2;
                  typ = Some num2;
                  source_name = None } );
            ("Afgewezen", None, litv "0" num2) ];
        source_name = None }
  in
  let doc = { empty_doc with object_types = [ ot "J" [ ("a", Some num0) ] [] ] } in
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "j" "J" []) in
  let self = World.get w "j" in
  (* the exact 100/3 crosses the Match arm and collapses once at the endpoint *)
  expect_num "WV-2.9-match-arm-round"
    (Eval.eval_expr env w self ~err:no_err match_round)
    "33.33"

(* --- ch. 3.8: the calendar kernel (worked examples verbatim) ------------ *)

let () =
  let d = Value.date_of_iso in
  let td u a b = Arith.duration ~unit_:u (d a) (d b) in
  let plus u date lex = Arith.date_plus_duration ~unit_:u (d date) (n lex) in
  let min_ u date lex = Arith.date_plus_duration ~minus:true ~unit_:u (d date) (n lex) in
  let open Ir_v0.Checks in
  (* the leap child: 29-02-2004 → 28-02-2026 = 21 whole years; one day
     later = 22 *)
  expect_num "WV-3.8-leap-21" (td Tu_year "2004-02-29" "2026-02-28") "21";
  expect_num "WV-3.8-leap-22" (td Tu_year "2004-02-29" "2026-03-01") "22";
  expect_num "WV-3.8-leap-mnd-263" (td Tu_month "2004-02-29" "2026-02-28") "263";
  expect_num "WV-3.8-leap-mnd-264" (td Tu_month "2004-02-29" "2026-03-01") "264";
  (* clamp, not inverse: 31-01-2024 + 1 maand = 29-02-2024, yet the
     tijdsduur between them is 0 whole months *)
  check "WV-3.8-clamp-leap" (plus Tu_month "2024-01-31" "1" = d "2024-02-29");
  expect_num "WV-3.8-clamp-not-inverse" (td Tu_month "2024-01-31" "2024-02-29") "0";
  (* signed truncation toward zero: −7 months is −2 whole quarters, 0 years *)
  expect_num "WV-3.8-neg-mnd" (td Tu_month "2025-03-10" "2024-08-10") "-7";
  expect_num "WV-3.8-neg-kw" (td Tu_quarter "2025-03-10" "2024-08-10") "-2";
  expect_num "WV-3.8-neg-jr" (td Tu_year "2025-03-10" "2024-08-10") "0";
  expect_num "WV-3.8-neg-abs"
    (Arith.duration ~absolute:true ~unit_:Tu_month (d "2025-03-10") (d "2024-08-10"))
    "7";
  (* date ± whole calendar units: the month-end clamp, quarter = 3 months,
     year = 12 months, day/week linear *)
  check "WV-3.8-clamp-nonleap" (plus Tu_month "2025-01-31" "1" = d "2025-02-28");
  check "WV-3.8-kwartaal" (plus Tu_quarter "2024-01-01" "1" = d "2024-04-01");
  check "WV-3.8-jaar-leap" (plus Tu_year "2024-02-29" "1" = d "2025-02-28");
  check "WV-3.8-weken" (plus Tu_week "2024-01-31" "2" = d "2024-02-14");
  check "WV-3.8-dagen" (plus Tu_day "2024-12-30" "3" = d "2025-01-02");
  (* a fractional count truncates toward zero before the calendar step *)
  check "WV-3.8-frac-plus" (plus Tu_month "2024-01-31" "1.9" = d "2024-02-29");
  check "WV-3.8-frac-min" (min_ Tu_month "2024-01-31" "1.9" = d "2023-12-31");
  (* min: the date left, the duration subtracted *)
  check "WV-3.8-min-week" (min_ Tu_week "2024-01-31" "1" = d "2024-01-24");
  (* empty rules (ch. 1.2): leeg date propagates; leeg duration counts 0;
     leeg tijdsduur operand propagates *)
  expect_empty "WV-3.8-leeg-datum" (Arith.date_plus_duration ~unit_:Tu_day empty_value (n "5"));
  check "WV-3.8-leeg-duur"
    (Arith.date_plus_duration ~unit_:Tu_month (d "2024-01-31") empty_value = d "2024-01-31");
  expect_empty "WV-3.8-leeg-tijdsduur" (Arith.duration ~unit_:Tu_year empty_value (d "2025-03-01"));
  (* the spelling table: nine units, keyed on spelling, case-insensitive;
     plural and abbreviation land on the same unit; alien spellings miss *)
  check "WV-3.8-spelling-jr"
    (time_unit_of_spelling "jaren" = Some Tu_year
    && time_unit_of_spelling "JR" = Some Tu_year
    && time_unit_of_spelling "jaar" = Some Tu_year);
  check "WV-3.8-spelling-negen"
    (time_unit_of_spelling "maanden" = Some Tu_month
    && time_unit_of_spelling "kwartalen" = Some Tu_quarter
    && time_unit_of_spelling "weken" = Some Tu_week
    && time_unit_of_spelling "dg" = Some Tu_day
    && time_unit_of_spelling "uren" = Some Tu_hour
    && time_unit_of_spelling "min" = Some Tu_minute
    && time_unit_of_spelling "secondes" = Some Tu_second
    && time_unit_of_spelling "ms" = Some Tu_millisecond);
  check "WV-3.8-spelling-alien" (time_unit_of_spelling "werkdag" = None);
  (* sub-day units demand datum-tijd operands — loud, never silent *)
  expect_fault "WV-3.8-subday-tijdsduur" (fun () -> td Tu_hour "2024-01-01" "2024-01-02");
  expect_fault "WV-3.8-subday-plus" (fun () -> plus Tu_hour "2024-01-01" "3")

(* --- ch. 3.8: the day-kind duration forms (worked examples verbatim) -----
   The kernel level: the predicate arrives as a function over the civil day
   number, so these pin the COUNTING and the WALK. The Eval block below pins
   the other half — resolving a unit spelling to a day kind and binding
   `de dag`. The day kind of the chapter's own examples is 25/26 December. *)

let () =
  let d = Value.date_of_iso in
  let day = function Value.Date z -> z | _ -> failwith "date" in
  let kerst z =
    let _, m, dd = Value.civil_from_days z in
    m = 12 && (dd = 25 || dd = 26)
  in
  let never _ = false in
  let td ?(absolute = false) ?(matches = kerst) a b =
    Arith.duration_day_kind ~absolute ~matches (d a) (d b)
  in
  let plus ?(matches = kerst) date lex = Arith.date_plus_day_kind ~matches (d date) (n lex) in
  let min_ ?(matches = kerst) date lex =
    Arith.date_plus_day_kind ~minus:true ~matches (d date) (n lex)
  in
  (* §6.10, the count over (A, B]: start exclusive, end inclusive *)
  expect_num "WV-3.8-dk-telling-2" (td "2024-12-20" "2024-12-31") "2";
  expect_num "WV-3.8-dk-telling-start-uit" (td "2024-12-25" "2024-12-26") "1";
  expect_num "WV-3.8-dk-telling-omgekeerd" (td "2024-12-31" "2024-12-20") "-2";
  expect_num "WV-3.8-dk-telling-absoluut" (td ~absolute:true "2024-12-31" "2024-12-20") "2";
  expect_num "WV-3.8-dk-telling-gelijk" (td "2024-12-25" "2024-12-25") "0";
  expect_empty "WV-3.8-dk-telling-leeg"
    (Arith.duration_day_kind ~matches:kerst empty_value (d "2024-12-31"));
  (* an unsatisfiable day kind COUNTS 0 — an answer, where a step is not *)
  expect_num "WV-3.8-dk-telling-onvervulbaar" (td ~matches:never "2024-12-20" "2024-12-31") "0";
  (* §6.11, the walk: the Nth day of the kind, the start never counting *)
  check "WV-3.8-dk-stap-2" (plus "2024-12-20" "2" = d "2024-12-26");
  check "WV-3.8-dk-stap-start-uit" (plus "2024-12-25" "1" = d "2024-12-26");
  check "WV-3.8-dk-stap-jaargrens" (plus "2024-12-26" "1" = d "2025-12-25");
  check "WV-3.8-dk-stap-nul" (plus "2024-12-20" "0" = d "2024-12-20");
  check "WV-3.8-dk-stap-leeg-telling"
    (Arith.date_plus_day_kind ~matches:kerst (d "2024-12-20") empty_value = d "2024-12-20");
  expect_empty "WV-3.8-dk-stap-leeg-datum"
    (Arith.date_plus_day_kind ~matches:kerst empty_value (n "2"));
  check "WV-3.8-dk-stap-terug" (min_ "2024-12-27" "1" = d "2024-12-26");
  check "WV-3.8-dk-stap-terug-jaargrens" (min_ "2024-12-25" "1" = d "2023-12-26");
  (* a negative count flips the direction, a fractional one truncates
     toward zero before the walk (the calendar-unit rule, unchanged) *)
  check "WV-3.8-dk-stap-negatief" (plus "2024-12-27" "-1" = d "2024-12-26");
  check "WV-3.8-dk-stap-breuk" (plus "2024-12-20" "2.9" = d "2024-12-26");
  (* the REACH fence: a day kind no calendar day satisfies FAULTS, never an
     endless walk. What the fence bounds is the GAP, and the two kinds it
     must NOT refuse are pinned beside it — the century-skipped leap day
     (2 921 days, the widest Gregorian recurrence) and a YEAR-BOUND kind
     decades out, which is the case the reach was raised for *)
  expect_fault "WV-3.8-dk-stap-onvervulbaar" (fun () -> plus ~matches:never "2024-12-20" "1");
  let leap_day z =
    let _, m, dd = Value.civil_from_days z in
    m = 2 && dd = 29
  in
  check "WV-3.8-dk-stap-eeuwsprong"
    (plus ~matches:leap_day "2096-02-29" "1" = d "2104-02-29");
  check "WV-3.8-dk-reikwijdte-2921"
    (day (plus ~matches:leap_day "2096-02-29" "1") - day (d "2096-02-29") = 2921);
  let in2040 z =
    let y, _, dd = Value.civil_from_days z in
    y = 2040 && (dd = 1 || dd = 15)
  in
  check "WV-3.8-dk-stap-jaargebonden" (plus ~matches:in2040 "2026-03-10" "1" = d "2040-01-01");
  (* §6.11 ON A DATUM-TIJD OPERAND (maintainer decision 2026-09-19): the day
     kind is judged on the CIVIL DAY PART and the clock time rides through
     unchanged; zero, leeg and negative follow the day-precision rules *)
  let dtk s = Value.Date_time (Case.ms_of_datetime s) in
  let plus_dt from lex = Arith.date_plus_day_kind ~matches:kerst (dtk from) (n lex) in
  let min_dt from lex = Arith.date_plus_day_kind ~minus:true ~matches:kerst (dtk from) (n lex) in
  check "WV-3.8-dk-dt-stap" (plus_dt "2024-12-20T09:15:30" "2" = dtk "2024-12-26T09:15:30");
  check "WV-3.8-dk-dt-stap-start-uit"
    (plus_dt "2024-12-25T23:59:59" "1" = dtk "2024-12-26T23:59:59");
  check "WV-3.8-dk-dt-stap-terug" (min_dt "2024-12-26T09:15:30" "1" = dtk "2024-12-25T09:15:30");
  check "WV-3.8-dk-dt-stap-nul" (plus_dt "2024-12-20T09:15:30" "0" = dtk "2024-12-20T09:15:30");
  check "WV-3.8-dk-dt-stap-leeg-telling"
    (Arith.date_plus_day_kind ~matches:kerst (dtk "2024-12-20T09:15:30") empty_value
    = dtk "2024-12-20T09:15:30");
  expect_empty "WV-3.8-dk-dt-stap-leeg-datum"
    (Arith.date_plus_day_kind ~matches:kerst empty_value (n "2"));
  (* §6.10 over datum-tijd operands stays REFUSED by name — and the precision
     test runs BEFORE the leeg arms, so a leeg operand gets the refusal that
     names the form instead of the leeg rule of a form that is not carried *)
  expect_fault "WV-3.8-dk-dt-telling" (fun () ->
      Arith.duration_day_kind ~matches:kerst (dtk "2024-12-20T10:00:00")
        (dtk "2024-12-31T10:00:00"));
  expect_fault "WV-3.8-dk-dt-telling-leeg-links" (fun () ->
      Arith.duration_day_kind ~matches:kerst empty_value (dtk "2024-12-31T10:00:00"));
  expect_fault "WV-3.8-dk-dt-telling-leeg-rechts" (fun () ->
      Arith.duration_day_kind ~matches:kerst (dtk "2024-12-20T10:00:00") empty_value)

(* --- ch. 3.8 through Eval: resolving a day kind and binding `de dag` -----
   The half the kernel block cannot see: a unit SPELLING resolved through a
   day kind's name or plural (trimmed, case-insensitive), the definition
   found by name, and the voorwaarde evaluated with `de dag` bound to the
   walked day.
   The `de dag` context value outside that binding faults by name. *)

let () =
  let christmas_days = unit_ ~canonical_form:"kerstdagen" "kerstdagen" in
  let num_kd : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Some christmas_days }
  in
  let the_day : Ir_v0.Ast.expr =
    Bound_day { typ = Some Date_in_days; source_name = None }
  in
  let portion kind : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments = [ the_day ]; element_object_type = None; empty_fallback = None;
        filter = None; dimension_selection = None;
        typ = Some (Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = None });
        source_name = None; grain = None; source_role = None; source_path = None }
  in
  let eq (l : Ir_v0.Ast.expr) lex : Ir_v0.Ast.condition_expr =
    Comparison
      { op = Equal; left_side = Some l;
        right_side = Some (litv lex (Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = None }));
        time_period = None; source_name = None }
  in
  let kerstdag : Ir_v0.Ast.resolved_day_kind =
    { rd_name = Some "kerstdag";
      rd_condition =
        Some
          (Compound
             { quantifier_kind = All; cutoff = 0;
               components =
                 [ eq (portion Month_of) "12";
                   Compound
                     { quantifier_kind = At_least; cutoff = 1;
                       components = [ eq (portion Day_of) "25"; eq (portion Day_of) "26" ];
                       source_name = None } ];
               source_name = None });
      rd_validity = None }
  in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Post" [ ("aanvang", Some Date_in_days); ("sluiting", Some Date_in_days) ] [] ];
      (* the plural is the day kind's duration-unit SPELLING; the lookup is
         trimmed and case-insensitive, which the odd plural here exercises *)
      day_kinds = [ { dk_name = "kerstdag"; dk_plural = Some "  KERSTDAGEN  " } ];
      resolved_day_kind_definitions = [ kerstdag ];
    }
  in
  let env = env_of doc in
  let w =
    World.add_instance World.empty
      (inst "p" "Post"
         [ ("aanvang", Value.date_of_iso "2024-12-20");
           ("sluiting", Value.date_of_iso "2024-12-31") ])
  in
  let self = World.get w "p" in
  let ev = Eval.eval_expr env w self ~err:no_err in
  let step_item lex : Ir_v0.Ast.expr =
    Binary
      { op = Plus; left_side = Some (aref "aanvang" "Post");
        right_side = Some (litv lex num_kd); typ = Some Date_in_days; source_name = None }
  in
  let counting : Ir_v0.Ast.expr =
    Function_call
      { kind = Duration;
        call_arguments =
          [ aref "aanvang" "Post"; aref "sluiting" "Post" ];
        element_object_type = None; empty_fallback = None; filter = None; dimension_selection = None;
        typ = Some num_kd; source_name = None; grain = None; source_role = None; source_path = None }
  in
  check "WV-3.8-dk-eval-stap" (ev (step_item "2") = Value.date_of_iso "2024-12-26");
  expect_num "WV-3.8-dk-eval-telling" (ev counting) "2";
  (* `de dag` has no value outside a day-kind evaluation — a fault that
     names the form, never a fall-through and never leeg *)
  expect_fault "WV-3.8-dk-eval-de-dag-buiten" (fun () -> ev the_day);
  (* a spelling no day kind carries is neither a calendar unit nor a day kind *)
  let doc_empty = { doc with day_kinds = [ { dk_name = "kerstdag"; dk_plural = None } ] } in
  expect_fault "WV-3.8-dk-eval-onbekende-eenheid" (fun () ->
      Eval.eval_expr (env_of doc_empty) w self ~err:no_err (step_item "2"));
  (* a day kind without a definition faults at resolution *)
  let doc_find = { doc with resolved_day_kind_definitions = [] } in
  expect_fault "WV-3.8-dk-eval-geen-definitie" (fun () ->
      Eval.eval_expr (env_of doc_find) w self ~err:no_err (step_item "2"));
  (* a self-referential definition has no fixpoint (WF-085): ref faults
     rather than looping, even though the verifier refuses it first *)
  let cyclic : Ir_v0.Ast.resolved_day_kind =
    { rd_name = Some "kerstdag";
      rd_condition =
        Some
          (Predicate
             { variety = Day_kind_predicate; operand = None; day_kind = Some "kerstdag"; negation = false;
               digit_chars = 0; time_period = None; source_name = None });
      rd_validity = None }
  in
  expect_fault "WV-3.8-dk-eval-cyclus" (fun () ->
      Eval.eval_expr
        (env_of { doc with resolved_day_kind_definitions = [ cyclic ] })
        w self ~err:no_err (step_item "1"));
  (* the §9.8 composition is carried ON THE BOUND DAY ONLY: a nested test on
     some other date is refused, in the ASP backend's words (the narrow route
     of 2026-09-19 — ref and the Drools compiler answered it while the ASP
     backend refused, and one executor computing what another refuses is the
     asymmetry this card ends) *)
  let another : Ir_v0.Ast.resolved_day_kind =
    { rd_name = Some "kerstdag";
      rd_condition =
        Some
          (Predicate
             { variety = Day_kind_predicate; day_kind = Some "kerstdag";
               operand = Some (litv "2024-12-25" Date_in_days); negation = false; digit_chars = 0;
               time_period = None; source_name = None });
      rd_validity = None }
  in
  expect_fault "WV-3.8-dk-eval-geneste-andere-dag" (fun () ->
      Eval.eval_expr
        (env_of { doc with resolved_day_kind_definitions = [ another ] })
        w self ~err:no_err (step_item "1"))

(* --- ch. 3.8: date-part extraction and the Easter builtin (worked
   examples verbatim) ----------------------------------------------------- *)

let () =
  let d = Value.date_of_iso in
  (* civil components of a day-precision date, unitless whole numbers *)
  expect_num "WV-3.8-jaar-uit" (Arith.year_of (d "2026-11-03")) "2026";
  expect_num "WV-3.8-maand-uit" (Arith.month_of (d "2026-11-03")) "11";
  expect_num "WV-3.8-dag-uit" (Arith.day_from (d "2026-11-03")) "3";
  (* the four pinned Easter Sundays: 2024 (the standing pin), 2026, the
     March Easter 2027, the late-April Easter 2038 *)
  check "WV-3.8-paasdag-2024" (Arith.easter_sunday (n "2024") = d "2024-03-31");
  check "WV-3.8-paasdag-2026" (Arith.easter_sunday (n "2026") = d "2026-04-05");
  check "WV-3.8-paasdag-2027" (Arith.easter_sunday (n "2027") = d "2027-03-28");
  check "WV-3.8-paasdag-2038" (Arith.easter_sunday (n "2038") = d "2038-04-25");
  (* leeg propagates (ch. 1.2b) *)
  expect_empty "WV-3.8-maand-uit-leeg" (Arith.month_of empty_value);
  expect_empty "WV-3.8-dag-uit-leeg" (Arith.day_from empty_value);
  expect_empty "WV-3.8-paasdag-leeg" (Arith.easter_sunday empty_value);
  (* a non-whole or unit-carrying year faults (never a silent truncation);
     a non-date extractor operand faults *)
  expect_fault "WV-3.8-paasdag-frac" (fun () -> Arith.easter_sunday (n "2024.5"));
  expect_fault "WV-3.8-paasdag-eenheid" (fun () -> Arith.easter_sunday (num_with_unit jr "2024"));
  (* a whole year outside the machine-integer range faults, never wraps:
     10^17 fits an OCaml int, its civil day number does not *)
  expect_fault "WV-3.8-paasdag-jaar-buiten-bereik" (fun () ->
      Arith.easter_sunday (n "100000000000000000"));
  expect_num "WV-3.8-paasdag-groot-jaar" (Arith.year_of (Arith.easter_sunday (n "1000000000000000")))
    "1000000000000000";
  expect_fault "WV-3.8-maand-uit-tekst" (fun () -> Arith.month_of (Value.Text "x"))

(* --- ch. 3.8: the date construction (worked examples verbatim) ---------- *)

let () =
  let d = Value.date_of_iso in
  let date_with parts = Arith.date_construction ~time:false (List.map n parts) in
  let date_time_with parts = Arith.date_construction ~time:true (List.map n parts) in
  check "WV-3.8-datummet" (date_with [ "2026"; "3"; "1" ] = d "2026-03-01");
  check "WV-3.8-datummet-schrikkeldag" (date_with [ "2024"; "2"; "29" ] = d "2024-02-29");
  check "WV-3.8-datummet-eeuwjaar" (date_with [ "2000"; "2"; "29" ] = d "2000-02-29");
  (* parts that name no calendar day fault: never a roll-over into the next
     month, never the month-end clamp of date + duration *)
  expect_fault "WV-3.8-datummet-geen-schrikkeljaar" (fun () -> date_with [ "2026"; "2"; "29" ]);
  expect_fault "WV-3.8-datummet-eeuw-geen-schrikkeljaar" (fun () -> date_with [ "1900"; "2"; "29" ]);
  expect_fault "WV-3.8-datummet-31-april" (fun () -> date_with [ "2026"; "4"; "31" ]);
  expect_fault "WV-3.8-datummet-maand-13" (fun () -> date_with [ "2026"; "13"; "1" ]);
  expect_fault "WV-3.8-datummet-maand-0" (fun () -> date_with [ "2026"; "0"; "1" ]);
  expect_fault "WV-3.8-datummet-dag-0" (fun () -> date_with [ "2026"; "3"; "0" ]);
  (* a part is a whole unitless number, as the Easter year is *)
  expect_fault "WV-3.8-datummet-breuk" (fun () -> date_with [ "2026"; "2.5"; "1" ]);
  expect_fault "WV-3.8-datummet-eenheid" (fun () ->
      Arith.date_construction ~time:false [ num_with_unit jr "2026"; n "3"; n "1" ]);
  (* a leeg part makes the date leeg, also beside a part that names no day *)
  expect_empty "WV-3.8-datummet-leeg"
    (Arith.date_construction ~time:false [ empty_value; n "3"; n "1" ]);
  expect_empty "WV-3.8-datummet-leeg-voor-de-geldigheid"
    (Arith.date_construction ~time:false [ n "2026"; n "13"; empty_value ]);
  check "WV-3.8-datumtijdmet"
    (date_time_with [ "2026"; "3"; "1"; "6"; "30"; "0"; "0" ]
     = Value.Date_time (Result.get_ok (Value.read_iso_datetime "2026-03-01T06:30:00")));
  check "WV-3.8-datumtijdmet-laatste-milliseconde"
    (date_time_with [ "2026"; "12"; "31"; "23"; "59"; "59"; "999" ]
     = Value.Date_time (Result.get_ok (Value.read_iso_datetime "2026-12-31T23:59:59.999")));
  expect_fault "WV-3.8-datumtijdmet-uur-24" (fun () ->
      date_time_with [ "2026"; "3"; "1"; "24"; "0"; "0"; "0" ]);
  expect_fault "WV-3.8-datumtijdmet-seconde-60" (fun () ->
      date_time_with [ "2026"; "3"; "1"; "0"; "0"; "60"; "0" ]);
  expect_fault "WV-3.8-datumtijdmet-milliseconde-1000" (fun () ->
      date_time_with [ "2026"; "3"; "1"; "0"; "0"; "0"; "1000" ]);
  expect_fault "WV-3.8-datumtijdmet-31-april" (fun () ->
      date_time_with [ "2026"; "4"; "31"; "0"; "0"; "0"; "0" ]);
  (* the instant lies in the representable millisecond range: year
     146140482 begins inside it, year 200000000 lies beyond it *)
  check "WV-3.8-datumtijdmet-bereik"
    (date_time_with [ "146140482"; "1"; "1"; "0"; "0"; "0"; "0" ]
     = Value.Date_time (Value.days_from_civil ~y:146140482 ~m:1 ~d:1 * Value.ms_per_day));
  expect_fault "WV-3.8-datumtijdmet-buiten-bereik" (fun () ->
      date_time_with [ "200000000"; "3"; "1"; "0"; "0"; "0"; "0" ]);
  (* the two ends of the range, to the millisecond: 2^62 - 1 and -2^62 *)
  check "WV-3.8-datumtijdmet-bovengrens"
    (date_time_with [ "146140482"; "4"; "24"; "15"; "36"; "27"; "903" ] = Value.Date_time max_int);
  expect_fault "WV-3.8-datumtijdmet-bovengrens-plus-een" (fun () ->
      date_time_with [ "146140482"; "4"; "24"; "15"; "36"; "27"; "904" ]);
  check "WV-3.8-datumtijdmet-ondergrens"
    (date_time_with [ "-146136543"; "9"; "8"; "8"; "23"; "32"; "96" ] = Value.Date_time min_int);
  expect_fault "WV-3.8-datumtijdmet-ondergrens-min-een" (fun () ->
      date_time_with [ "-146136543"; "9"; "8"; "8"; "23"; "32"; "95" ]);
  (* ch. 5.12: a year the case format cannot write, with its sign and at
     least four digits (ISO 8601's expanded form) *)
  check "WV-5.12-jaar-boven-9999" (Value.iso_of_date (Value.days_from_civil ~y:12026 ~m:3 ~d:1) = "+12026-03-01");
  check "WV-5.12-jaar-onder-0000" (Value.iso_of_date (Value.days_from_civil ~y:(-5) ~m:3 ~d:1) = "-0005-03-01");
  check "WV-5.12-jaar-0000" (Value.iso_of_date (Value.days_from_civil ~y:0 ~m:3 ~d:1) = "0000-03-01")

(* --- ch. 3.8: the datum-tijd millisecond layer (worked examples
   verbatim) --------------------------------------------------------------- *)

let () =
  let dt s = Value.Date_time (Case.ms_of_datetime s) in
  let plus u date lex = Arith.date_plus_duration ~unit_:u (dt date) (n lex) in
  let min_ u date lex = Arith.date_plus_duration ~minus:true ~unit_:u (dt date) (n lex) in
  let td u a b = Arith.duration ~unit_:u (dt a) (dt b) in
  let open Ir_v0.Checks in
  (* sub-day linearity, across the leap day *)
  check "WV-3.8-dt-uur" (plus Tu_hour "2024-02-28T23:30:00" "26" = dt "2024-03-01T01:30:00");
  check "WV-3.8-dt-minuut"
    (min_ Tu_minute "2024-03-01T00:15:00" "30" = dt "2024-02-29T23:45:00");
  check "WV-3.8-dt-ms"
    (plus Tu_millisecond "2024-01-01T00:00:00" "1500" = dt "2024-01-01T00:00:01.500");
  (* calendar units act on the civil day part, preserving the time-of-day *)
  check "WV-3.8-dt-klem" (plus Tu_month "2024-01-31T09:30:00" "1" = dt "2024-02-29T09:30:00");
  check "WV-3.8-dt-maand" (plus Tu_month "2024-02-28T23:30:00" "1" = dt "2024-03-28T23:30:00");
  check "WV-3.8-dt-weken" (plus Tu_week "2024-02-28T23:30:00" "2" = dt "2024-03-13T23:30:00");
  (* the fractional count truncates toward zero before the step, as at day
     precision; the pre-epoch split keeps the time-of-day non-negative *)
  check "WV-3.8-dt-frac" (plus Tu_hour "2024-02-28T23:30:00" "1.9" = dt "2024-02-29T00:30:00");
  check "WV-3.8-dt-pre-epoch"
    (plus Tu_day "1969-12-31T23:30:00" "1" = dt "1970-01-01T23:30:00");
  (* the borrow corner: the time of day counts back before the whole-unit
     count — hours stay linear, days/months borrow *)
  expect_num "WV-3.8-dt-borrow-uren" (td Tu_hour "2024-02-28T23:30:00" "2024-03-01T10:15:00") "34";
  expect_num "WV-3.8-dt-borrow-dagen" (td Tu_day "2024-02-28T23:30:00" "2024-03-01T10:15:00") "1";
  expect_num "WV-3.8-dt-borrow-maand"
    (td Tu_month "2024-01-31T10:00:00" "2024-03-31T09:59:00") "1";
  expect_num "WV-3.8-dt-geen-borrow"
    (td Tu_month "2024-01-31T10:00:00" "2024-03-31T10:00:00") "2";
  (* a negative span; the absolute variant drops the sign *)
  expect_num "WV-3.8-dt-neg-uren" (td Tu_hour "2024-03-01T10:15:00" "2024-02-28T23:30:00") "-34";
  expect_num "WV-3.8-dt-neg-dagen" (td Tu_day "2024-03-01T10:15:00" "2024-02-28T23:30:00") "-1";
  expect_num "WV-3.8-dt-abs-uren"
    (Arith.duration ~absolute:true ~unit_:Tu_hour
       (dt "2024-03-01T10:15:00") (dt "2024-02-28T23:30:00"))
    "34";
  expect_num "WV-3.8-dt-abs-dagen"
    (Arith.duration ~absolute:true ~unit_:Tu_day
       (dt "2024-03-01T10:15:00") (dt "2024-02-28T23:30:00"))
    "1";
  (* empty rules (ch. 1.2): leeg datetime propagates; leeg duration counts
     0; leeg tijdsduur operand propagates *)
  expect_empty "WV-3.8-dt-leeg-datum" (Arith.date_plus_duration ~unit_:Tu_hour empty_value (n "26"));
  check "WV-3.8-dt-leeg-duur"
    (Arith.date_plus_duration ~unit_:Tu_hour (dt "2024-02-28T23:30:00") empty_value
    = dt "2024-02-28T23:30:00");
  expect_empty "WV-3.8-dt-leeg-tijdsduur"
    (Arith.duration ~unit_:Tu_hour empty_value (dt "2024-03-01T10:15:00"));
  (* mixed precision is ill-formed (WF-038) — fail fast behind the verifier *)
  expect_fault "WV-3.8-dt-mix" (fun () ->
      Arith.duration ~unit_:Tu_day (Value.date_of_iso "2024-02-28") (dt "2024-03-01T10:15:00"));
  (* a step that leaves the representable ms range [-2^62, 2^62 - 1] faults,
     never a silent wrap-around *)
  expect_fault "WV-3.8-dt-bereik-uur" (fun () ->
      plus Tu_hour "2024-01-01T00:00:00" "2000000000000");
  expect_fault "WV-3.8-dt-bereik-neg" (fun () ->
      min_ Tu_hour "2024-01-01T00:00:00" "2000000000000");
  expect_fault "WV-3.8-dt-bereik-dag" (fun () ->
      plus Tu_day "2024-01-01T00:00:00" "60000000000");
  (* the range paragraph's worked example: the last whole hour before
     2^62 - 1 ms is 146140482-04-24T15:00:00, and the count 2^64 + 1 is out
     of range itself, never wrapped to 1 *)
  check "WV-3.8-dt-bereik-grens"
    (plus Tu_hour "2024-01-01T00:00:00" "1281023420655"
     = Value.Date_time 4611686018425200000);
  expect_fault "WV-3.8-dt-bereik-grens-plus-een" (fun () ->
      plus Tu_hour "2024-01-01T00:00:00" "1281023420656");
  expect_fault "WV-3.8-dt-bereik-telling" (fun () ->
      plus Tu_day "2024-01-01T00:00:00" "18446744073709551617")

(* --- ch. 3.8 through Eval: the datum-tijd routing ------------------------
   The widened WF-035/WF-038 fences in eval.ml admit a Date_time_type operand
   with a sub-day unit into the millisecond layer — and still refuse that
   unit on a day-precision date. The Arith checks above cannot see this
   routing (they call the layer directly), and the datum-tijd embargo
   admits no corpus golden, so these IR-shaped expressions through
   Eval.eval_expr are the only executable pin on the eval wiring
   ("Executed in ref only", RUNTIME-SEMANTICS ch. 3.8). *)
let () =
  let hour = unit_ ~canonical_form:"uur" "u" in
  let num_hour : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Some hour }
  in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Vondst"
            [ ("melding", Some Date_time_type); ("berging", Some Date_time_type);
              ("dagtekening", Some Date_in_days) ]
            [] ];
    }
  in
  let env = env_of doc in
  let dt s = Value.Date_time (Case.ms_of_datetime s) in
  let w =
    World.add_instance World.empty
      (inst "v" "Vondst"
         [ ("melding", dt "2024-02-28T23:30:00");
           ("berging", dt "2024-03-01T10:15:00");
           ("dagtekening", Value.date_of_iso "2024-02-28") ])
  in
  let self = World.get w "v" in
  let ev = Eval.eval_expr env w self ~err:no_err in
  let plus_hours attr typ lex : Ir_v0.Ast.expr =
    Binary
      { op = Plus; left_side = Some (aref attr "Vondst");
        right_side = Some (litv lex num_hour); typ = Some typ; source_name = None }
  in
  let duration_hours a b : Ir_v0.Ast.expr =
    Function_call
      { kind = Duration;
        call_arguments = [ aref a "Vondst"; aref b "Vondst" ];
        element_object_type = None; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num_hour; source_name = None;
        grain = None; source_role = None; source_path = None }
  in
  (* the widened WF-035 fence: a datum-tijd operand rides its sub-day unit
     into the ms layer (WV-3.8-dt-uur, through the evaluator) *)
  check "WV-3.8-dt-eval-plus-uur"
    (ev (plus_hours "melding" Date_time_type "26") = dt "2024-03-01T01:30:00");
  (* ... and still stands for a day-precision date (WF-035) *)
  expect_fault "WV-3.8-dt-eval-wf035" (fun () ->
      ev (plus_hours "dagtekening" Date_in_days "26"));
  (* the widened WF-038 fence: sub-day tijdsduur over two datetimes reaches
     the linear hour count (WV-3.8-dt-borrow-uren, through the evaluator) *)
  expect_num "WV-3.8-dt-eval-tijdsduur"
    (ev (duration_hours "melding" "berging"))
    "34";
  (* ... and still refuses a day-precision operand (WF-038) *)
  expect_fault "WV-3.8-dt-eval-wf038" (fun () ->
      ev (duration_hours "dagtekening" "berging"))

(* --- ch. 3.4a: the count source (D8 (ii)) -------------------------------- *)

(* WV-3.4a-telbron: "het aantal vondsten van de aanmelder" — the single
   argument of a count that carries a range names the counted role, and the
   count folds the instances the range reaches. A Jutter linked to two
   Vondsten — one with gewicht 2,50, one whose gewicht is leeg — counts 2
   (instances, not values: the ch. 1.2 aggregation row, a count never
   empty); a Jutter with zero links counts 0, never leeg. A count without
   its range faults: the relation is written, never derived (B12). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [ ("aantal eigen vondsten", Some num0) ] [];
          ot "Vondst" [ ("gewicht", Some num2) ] [] ];
      fact_types =
        [ ( "vondstaanmelding",
            {
              ft_name = Some "vondstaanmelding";
              reciprocal = false;
              role_names =
                [
                  { role_identifier = Some "aanmelder"; role_object_type_name = Some "Jutter";
                    role_plural = None; role_many = false };
                  { role_identifier = Some "vondst"; role_object_type_name = Some "Vondst";
                    role_plural = Some "vondsten"; role_many = true };
                ];
            } ) ];
    }
  in
  let count_source : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some "vondsten"; object_type = Some "Jutter"; source_name = None;
        navigation_bindings = None }
  in
  let quantity source_role : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments = [ count_source ];
        element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role; source_path = None }
  in
  let vondst = Some { Ir_v0.Ast.rb_fact_type = "vondstaanmelding"; rb_role = "vondst" } in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance
            (World.add_instance World.empty
               { (inst "j1" "Jutter" []) with
                 left_side = World.SMap.add "vondsten" [ "v1"; "v2" ] World.SMap.empty })
            (inst "j2" "Jutter" []))
         (inst "v1" "Vondst" [ ("gewicht", n "2.50") ]))
      (inst "v2" "Vondst" [])
  in
  let ev label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  expect_num "WV-3.4a-telbron-twee" (ev "j1" (quantity vondst)) "2";
  expect_num "WV-3.4a-telbron-nul" (ev "j2" (quantity vondst)) "0";
  expect_fault "WV-3.4a-telbron-zonder-bereik" (fun () -> ev "j1" (quantity None))

(* --- ch. 3.4b: the multi-step fold source, the flat union --------------- *)

(* WV-3.4b: the `anchor/jutterswet-bootberging` worked example, verbatim
   (both cases, replayed by gates 5 and 6). Jutter -(bemanning/gevoerde
   boot)-> Boot -(berging/geborgen vondst)-> Vondst.gewicht, plus one
   step deeper -(inpakking/ingepakte kist)-> Kist.inhoudsgewicht.

   The union spread over TWO intermediate instances is what makes the
   answer a proof: J1 sails B1 and B2, the minimum 3,00 sits on B1 while
   B2 contributes 5,00, so an executor that walked only one boat — or
   that dropped the outer step and folded a direct relation — cannot
   produce it. The empty corners answer alike at the last step (J3, whose
   boat salvages nothing) and at the first (J4, who sails nothing): the
   minimum is leeg because min has no fallback, the sum is the fallback
   fired ONCE. And the leeg element attribute drops out of a value fold
   without dropping its instance from the range.

   Case 2 is the UNION pin: V1 salvaged by BOTH boats contributes once,
   so the sum is 15,00 and not 18,00. The minimum is 3,00 either way,
   which is why the sum carries it. *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let role_ref ident otn mv plural_flag : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some otn; role_plural = Some mv;
      role_many = plural_flag }
  in
  let ft ident role_names = (ident, { Ir_v0.Ast.ft_name = Some ident; reciprocal = false; role_names }) in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [] []; ot "Boot" [] [];
          ot "Vondst" [ ("gewicht", Some num2) ] [ "gekeurd" ];
          ot "Kist" [ ("inhoudsgewicht", Some num2) ] [] ];
      fact_types =
        [ ft "bemanning"
            [ role_ref "schipper" "Jutter" "schippers" false;
              role_ref "gevoerde boot" "Boot" "gevoerde boten" true ];
          ft "berging"
            [ role_ref "bergende boot" "Boot" "bergende boten" true;
              role_ref "geborgen vondst" "Vondst" "geborgen vondsten" true ];
          ft "inpakking"
            [ role_ref "verpakte vondst" "Vondst" "verpakte vondsten" true;
              role_ref "ingepakte kist" "Kist" "ingepakte kisten" true ] ];
    }
  in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let path2 = [ rb "bemanning" "gevoerde boot"; rb "berging" "geborgen vondst" ] in
  let path3 = path2 @ [ rb "inpakking" "ingepakte kist" ] in
  let source_name attr otn : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some attr; object_type = Some otn; source_name = None; navigation_bindings = None }
  in
  let fold_node kind attr el trail empty_fallback : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments = [ source_name attr el ]; element_object_type = Some el; empty_fallback; filter = None;
        dimension_selection = None; typ = Some num2; source_name = None; grain = None;
        source_role = None; source_path = Some trail }
  in
  let minimum = fold_node Minimum "gewicht" "Vondst" path2 None in
  let summed =
    fold_node Sum_function "gewicht" "Vondst" path2
      (Some (litv "0" num0))
  in
  let deep = fold_node Minimum "inhoudsgewicht" "Kist" path3 None in
  let env = env_of doc in
  let left_side l = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty l in
  let i label otn proportion l =
    { (inst label otn (match proportion with None -> [] | Some (a, g) -> [ (a, n g) ])) with
      World.left_side = left_side l }
  in
  let world_state insts = List.fold_left World.add_instance World.empty insts in
  let ev w label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  (* case 1 — the range spread over two intermediates, and the three empty corners *)
  let w1 =
    world_state
      [ i "J1" "Jutter" None [ ("gevoerde boot", [ "B1"; "B2" ]) ];
        i "J2" "Jutter" None [ ("gevoerde boot", [ "B3" ]) ];
        i "J3" "Jutter" None [ ("gevoerde boot", [ "B4" ]) ];
        i "J4" "Jutter" None [];
        i "B1" "Boot" None [ ("geborgen vondst", [ "V1"; "V2" ]) ];
        i "B2" "Boot" None [ ("geborgen vondst", [ "V3" ]) ];
        i "B3" "Boot" None [ ("geborgen vondst", [ "V4"; "V5" ]) ];
        i "B4" "Boot" None [];
        i "V1" "Vondst" (Some ("gewicht", "3.00")) [ ("ingepakte kist", [ "K1" ]) ];
        i "V2" "Vondst" (Some ("gewicht", "7.00")) [ ("ingepakte kist", [ "K2"; "K3" ]) ];
        i "V3" "Vondst" (Some ("gewicht", "5.00")) [];
        i "V4" "Vondst" (Some ("gewicht", "9.00")) [ ("ingepakte kist", [ "K4" ]) ];
        i "V5" "Vondst" None [];
        i "K1" "Kist" (Some ("inhoudsgewicht", "2.50")) [];
        i "K2" "Kist" (Some ("inhoudsgewicht", "1.25")) [];
        i "K3" "Kist" (Some ("inhoudsgewicht", "4.00")) [];
        i "K4" "Kist" (Some ("inhoudsgewicht", "6.00")) [] ]
  in
  expect_num "WV-3.4b-vlakke-unie-minimum" (ev w1 "J1" minimum) "3.00";
  expect_num "WV-3.4b-vlakke-unie-som" (ev w1 "J1" summed) "15.00";
  expect_num "WV-3.4b-vlakke-unie-driestaps" (ev w1 "J1" deep) "1.25";
  expect_num "WV-3.4b-leeg-element-minimum" (ev w1 "J2" minimum) "9.00";
  expect_num "WV-3.4b-leeg-element-som" (ev w1 "J2" summed) "9.00";
  expect_num "WV-3.4b-leeg-element-driestaps" (ev w1 "J2" deep) "6.00";
  expect_empty "WV-3.4b-leeg-bij-de-laatste-stap-minimum" (ev w1 "J3" minimum);
  expect_num "WV-3.4b-leeg-bij-de-laatste-stap-som" (ev w1 "J3" summed) "0";
  expect_empty "WV-3.4b-leeg-bij-de-eerste-stap-minimum" (ev w1 "J4" minimum);
  expect_num "WV-3.4b-leeg-bij-de-eerste-stap-som" (ev w1 "J4" summed) "0";
  (* case 2 — the shared element contributes ONCE: 15,00, never 18,00 *)
  let w2 =
    world_state
      [ i "J1" "Jutter" None [ ("gevoerde boot", [ "B1"; "B2" ]) ];
        i "B1" "Boot" None [ ("geborgen vondst", [ "V1"; "V2" ]) ];
        i "B2" "Boot" None [ ("geborgen vondst", [ "V1"; "V3" ]) ];
        i "V1" "Vondst" (Some ("gewicht", "3.00")) [ ("ingepakte kist", [ "K1" ]) ];
        i "V2" "Vondst" (Some ("gewicht", "7.00")) [ ("ingepakte kist", [ "K2" ]) ];
        i "V3" "Vondst" (Some ("gewicht", "5.00")) [ ("ingepakte kist", [ "K1" ]) ];
        i "K1" "Kist" (Some ("inhoudsgewicht", "2.50")) [];
        i "K2" "Kist" (Some ("inhoudsgewicht", "1.25")) [] ]
  in
  expect_num "WV-3.4b-gedeeld-som" (ev w2 "J1" summed) "15.00";
  expect_num "WV-3.4b-gedeeld-minimum" (ev w2 "J1" minimum) "3.00";
  expect_num "WV-3.4b-gedeeld-driestaps" (ev w2 "J1" deep) "1.25";
  (* the two shape arms of WF-102, mirrored in the executor: a document that
     reaches ref without passing the verifier must fault by NAME rather than
     be read as the one-step form with the range dropped. Two spellings of one
     range are refused in the shared loader's sentence *)
  expect_fault_msg "WV-3.4b-pad-naast-bronrol-faalt"
    (fun () ->
      ev w1 "J1"
        (Function_call
           { kind = Minimum; call_arguments = [ source_name "gewicht" "Vondst" ];
             element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
             dimension_selection = None; typ = Some num2; source_name = None; grain = None;
             source_role = Some (rb "berging" "geborgen vondst"); source_path = Some path2 }))
    "sourcePath beside sourceRole: a fold carries one range, never two";
  expect_fault "WV-3.4b-eenstaps-pad-faalt"
    (fun () -> ev w1 "J1" (fold_node Minimum "gewicht" "Vondst" [ rb "berging" "geborgen vondst" ] None));
  (* a listed fold has no set source (D8 (i): the source is a SINGLE
     argument), so a range beside two operands has nothing to range *)
  expect_fault "WV-3.4b-pad-op-een-niet-setbron-faalt" (fun () ->
      ev w1 "J1"
        (Function_call
           { kind = Minimum; call_arguments = [ source_name "gewicht" "Vondst"; source_name "gewicht" "Vondst" ];
             element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
             dimension_selection = None; typ = Some num2; source_name = None; grain = None;
             source_role = None; source_path = Some path2 }));
  (* WF-102 (c), the verifier's sentence: a count's range is one step,
     spelled sourceRole, so a path under `count` is refused by name, with and
     without a die/dat subselection. Until this refusal ref counted the
     instances the path reached while the verifier refused the document *)
  let counting filter : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments = [ source_name "geborgen vondsten" "Jutter" ];
        element_object_type = Some "Vondst"; empty_fallback = None; filter;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = None; source_path = Some path2 }
  in
  let zwaar : Ir_v0.Ast.condition_expr =
    Comparison
      { op = Greater; left_side = Some (source_name "gewicht" "Vondst");
        right_side = Some (litv "4.00" num2);
        time_period = None; source_name = None }
  in
  expect_fault_msg "WV-3.4b-telling-over-een-pad-faalt" (fun () -> ev w1 "J1" (counting None))
    "a sourcePath is admissible only as the range of a single-argument VALUE fold's set source \
     — a count's range is one step, spelled sourceRole";
  expect_fault_msg "WV-3.4b-telling-over-een-pad-met-filter-faalt"
    (fun () -> ev w1 "J1" (counting (Some zwaar)))
    "a count's range is one step, spelled sourceRole";
  (* WF-093 (b) before WF-102, as the verifier and the Drools loader judge
     it: a count whose source names a MEMBER of the element type is refused
     for that, over a path of two steps and of one step alike. A count whose
     source names a role over one step is refused by WF-102 (c), judged
     before the arity *)
  let count_over source_label source_ot trail : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments = [ source_name source_label source_ot ]; element_object_type = Some "Vondst";
        empty_fallback = None; filter = None; dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role = None; source_path = Some trail }
  in
  let one_step = [ rb "berging" "geborgen vondst" ] in
  expect_fault_msg "WV-3.4b-telling-over-een-lid-met-pad-faalt"
    (fun () -> ev w1 "J1" (count_over "gewicht" "Vondst" path2))
    "reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  expect_fault_msg "WV-3.4b-telling-over-een-lid-met-eenstapspad-faalt"
    (fun () -> ev w1 "J1" (count_over "gewicht" "Vondst" one_step))
    "reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  expect_fault_msg "WV-3.4b-roltelling-over-een-eenstapspad-faalt"
    (fun () -> ev w1 "J1" (count_over "geborgen vondsten" "Jutter" one_step))
    "a count's range is one step, spelled sourceRole";
  (* WF-080: a characteristic is no attribute, so a value fold's set source
     never names one — the verifier's aggregation-source sentence *)
  expect_fault_msg "WV-3.4b-kenmerk-als-setbron-faalt"
    (fun () -> ev w1 "J1" (fold_node Sum_function "gekeurd" "Vondst" path2 None))
    "'gekeurd' is a characteristic of 'Vondst', not an attribute — a characteristic endpoint \
     is ill-formed in an aggregation source position"

(* WV-3.4b-toone: the ch. 3.4b clause "a step through a to-ONE role is an
   ordinary step". Multiplicity is a declaration fact and does not enter
   the reading — a to-one role contributes its zero-or-one instance to
   the union like any other — and this is the ONE clause of that chapter
   no corpus document exercises: every sourcePath step in the corpus is
   to-many, so without this check the sentence has no artefact.

   The model makes the answer discriminating rather than merely defined:
   Jutter -(bemanning/boot, BOTH roles to-one)-> Boot -(bootvangst/vondst,
   to-many)-> Vondst, beside a DIRECT jutterbezit relation. J1 sails B1,
   which catches V1 (3,00) and V2 (7,00), and J1 also owns V9 (1,00)
   through the direct relation. The union over the path is {V1, V2}, so
   the minimum is 3,00 — an executor that dropped the to-one step and
   folded the direct pair would answer 1,00. J2 sails nothing and owns
   nothing: the range is empty at the FIRST step, and a minimum has no
   fallback, so it reads leeg. *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let role_ref ident otn mv plural_flag : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some otn; role_plural = mv;
      role_many = plural_flag }
  in
  let ft ident role_names = (ident, { Ir_v0.Ast.ft_name = Some ident; reciprocal = false; role_names }) in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [] []; ot "Boot" [] []; ot "Vondst" [ ("gewicht", Some num2) ] [] ];
      fact_types =
        [ ft "bemanning"
            [ role_ref "schipper" "Jutter" None false; role_ref "boot" "Boot" None false ];
          ft "bootvangst"
            [ role_ref "vangende boot" "Boot" None false;
              role_ref "vondst" "Vondst" (Some "vondsten") true ];
          ft "jutterbezit"
            [ role_ref "bezitter" "Jutter" None false;
              role_ref "bezeten vondst" "Vondst" (Some "bezeten vondsten") true ] ];
    }
  in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let minimum : Ir_v0.Ast.expr =
    Function_call
      { kind = Minimum;
        call_arguments =
          [ Attribute_reference
              { attribute_ref = Some "gewicht"; object_type = Some "Vondst"; source_name = None; navigation_bindings = None } ];
        element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num2; source_name = None; grain = None;
        source_role = None;
        source_path = Some [ rb "bemanning" "boot"; rb "bootvangst" "vondst" ] }
  in
  let env = env_of doc in
  let left_side l = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty l in
  let i label otn proportion l =
    { (inst label otn (match proportion with None -> [] | Some g -> [ ("gewicht", n g) ])) with
      World.left_side = left_side l }
  in
  let w =
    List.fold_left World.add_instance World.empty
      [ i "J1" "Jutter" None [ ("boot", [ "B1" ]); ("bezeten vondst", [ "V9" ]) ];
        i "J2" "Jutter" None [];
        i "B1" "Boot" None [ ("vondst", [ "V1"; "V2" ]) ];
        i "V1" "Vondst" (Some "3.00") []; i "V2" "Vondst" (Some "7.00") [];
        i "V9" "Vondst" (Some "1.00") [] ]
  in
  let ev label = Eval.eval_expr env w (World.get w label) ~err:no_err minimum in
  expect_num "WV-3.4b-toone-stap" (ev "J1") "3.00";
  expect_empty "WV-3.4b-toone-leeg-bereik" (ev "J2")

(* --- ch. 3.4c: the filled-value count ---------------------------------- *)

(* WV-3.4c: the ch. 3.4c worked examples, verbatim. Four groups, and none
   of them is spare.

   (1) The source shapes, each beside the INSTANCE count over the same
   population, because the new kind is only discriminating next to the old
   one: a member with no value is a member (3 against 2, and 1 against 0),
   while an empty source answers 0 from both and leeg from neither.

   (2) The typelessness of the test (decision G8): the same three
   contributors folded once per declared type. The boolean row carries the
   sharpest half — `onwaar` is a VALUE and counts as filled; leeg is the
   absence of one.

   (3) The filter narrows the contributors BEFORE the presence test
   (ch. 3.4a): the leeg member fails the comparison and drops out, which is
   why the filtered counts agree at 1 where the unfiltered ones part.

   (4) The multi-step range counts ELEMENTS (ch. 3.4b): an element reachable
   along two intermediate instances contributes once. That sentence has
   waited for a carrier since the count's set source left the wire, and this
   kind is it.

   Plus the loud refusal: a source that names a role is a count source,
   which this kind cannot read. The verifier fences it on the wire (WF-093);
   the fault stays as the second line of defence. *)
let () =
  let num2kg : Ir_v0.Ast.datatype =
    Number
      { whole_part = false; decimal_places = 2; sign_char = None;
        measure_unit =
          Some { abbr_text = Some "kg"; system = Some "Gewicht"; canonical_form = Some "kg";
                 factor_to_canonical = Some { num = 1; den = 1 }; denominator = None } }
  in
  let kustvak : Ir_v0.Ast.datatype =
    Enumeration { domain_spec = Some "kustvakken"; label_list = [ Some "noord"; Some "zuid" ] }
  in
  let role_ref ident otn mv plural_flag : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some otn; role_plural = mv;
      role_many = plural_flag }
  in
  let ft ident role_names = (ident, { Ir_v0.Ast.ft_name = Some ident; reciprocal = false; role_names }) in
  let doc =
    {
      empty_doc with
      domain_specs = [ ("kustvakken", Some kustvak) ];
      object_types =
        [ ot "Jutter" [] []; ot "Boot" [] [];
          ot "Vondst"
            [ ("gewicht", Some num2kg); ("vindplaats", Some Text_type);
              ("vondstdag", Some Date_in_days); ("kustvak", Some kustvak);
              ("geborgen", Some Boolean) ]
            [ "gekeurd" ] ];
      fact_types =
        [ ft "vondstaanmelding"
            [ role_ref "aanmelder" "Jutter" None false; role_ref "vondst" "Vondst" (Some "vondsten") true ];
          ft "bemanning"
            [ role_ref "schipper" "Jutter" None false;
              role_ref "gevoerde boot" "Boot" (Some "gevoerde boten") true ];
          ft "berging"
            [ role_ref "bergende boot" "Boot" (Some "bergende boten") true;
              role_ref "geborgen vondst" "Vondst" (Some "geborgen vondsten") true ] ];
    }
  in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let set_source attr : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some attr; object_type = Some "Vondst"; source_name = None; navigation_bindings = None }
  in
  (* the count source: the counted role's name, read on the fold subject (D8 (ii)) *)
  let tally_source : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some "vondsten"; object_type = Some "Jutter"; source_name = None;
        navigation_bindings = None }
  in
  let fold_node kind call_arguments filter trail : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments; element_object_type = Some "Vondst"; empty_fallback = None; filter;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = (if trail = None then Some (rb "vondstaanmelding" "vondst") else None);
        source_path = trail }
  in
  let populated ?filter attr = fold_node Filled_count [ set_source attr ] filter None in
  let quantity ?filter () = fold_node Count [ tally_source ] filter None in
  let heftier lex : Ir_v0.Ast.condition_expr =
    Comparison
      { op = Greater; left_side = Some (aref "gewicht" "Vondst");
        right_side = Some (litv lex num2kg); time_period = None; source_name = None }
  in
  let env = env_of doc in
  let left_side l = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty l in
  let jutter label vondsten =
    { (inst label "Jutter" []) with World.left_side = left_side [ ("vondsten", vondsten) ] }
  in
  let world_state insts = List.fold_left World.add_instance World.empty insts in
  let w =
    world_state
      [ jutter "J1" [ "V1"; "V2"; "V3" ]; jutter "J2" []; jutter "J3" [ "V4" ];
        jutter "J4" [ "V5"; "V6" ]; jutter "J5" [ "V7"; "V8" ];
        inst "V1" "Vondst"
          [ ("gewicht", n "2.50"); ("vindplaats", Value.Text "Noorderstrand");
            ("vondstdag", Value.date_of_iso "2026-05-01"); ("kustvak", Value.Enum "noord");
            ("geborgen", Value.Bool true) ];
        inst "V2" "Vondst"
          [ ("gewicht", n "1.25"); ("vindplaats", Value.Text "Zuiderhoofd");
            ("vondstdag", Value.date_of_iso "2026-06-21"); ("kustvak", Value.Enum "zuid");
            ("geborgen", Value.Bool false) ];
        inst "V3" "Vondst" []; inst "V4" "Vondst" [];
        inst "V5" "Vondst" [ ("gewicht", n "5.00") ];
        inst "V6" "Vondst" [ ("gewicht", n "7.00") ];
        inst "V7" "Vondst" []; inst "V8" "Vondst" [] ]
  in
  let ev w label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  (* (1) the source shapes, the filled count beside the instance count *)
  expect_num "WV-3.4c-gemengd-aantal" (ev w "J1" (quantity ())) "3";
  expect_num "WV-3.4c-gemengd-gevuld" (ev w "J1" (populated "gewicht")) "2";
  expect_num "WV-3.4c-alles-gevuld-aantal" (ev w "J4" (quantity ())) "2";
  expect_num "WV-3.4c-alles-gevuld-gevuld" (ev w "J4" (populated "gewicht")) "2";
  expect_num "WV-3.4c-alles-leeg-aantal" (ev w "J5" (quantity ())) "2";
  expect_num "WV-3.4c-alles-leeg-gevuld" (ev w "J5" (populated "gewicht")) "0";
  expect_num "WV-3.4c-enig-lid-leeg-aantal" (ev w "J3" (quantity ())) "1";
  expect_num "WV-3.4c-enig-lid-leeg-gevuld" (ev w "J3" (populated "gewicht")) "0";
  expect_num "WV-3.4c-lege-bron-aantal" (ev w "J2" (quantity ())) "0";
  expect_num "WV-3.4c-lege-bron-gevuld" (ev w "J2" (populated "gewicht")) "0";
  (* (2) every value type counts; the test is presence, not number *)
  expect_num "WV-3.4c-type-numeriek" (ev w "J1" (populated "gewicht")) "2";
  expect_num "WV-3.4c-type-tekst" (ev w "J1" (populated "vindplaats")) "2";
  expect_num "WV-3.4c-type-datum" (ev w "J1" (populated "vondstdag")) "2";
  expect_num "WV-3.4c-type-enumeratie" (ev w "J1" (populated "kustvak")) "2";
  (* onwaar is a VALUE: V2's `geborgen` is false and counts as filled *)
  expect_num "WV-3.4c-type-booleaan" (ev w "J1" (populated "geborgen")) "2";
  (* (3) the filter narrows the contributors before the presence test *)
  expect_num "WV-3.4c-filter-aantal" (ev w "J1" (quantity ~filter:(heftier "2.00") ())) "1";
  expect_num "WV-3.4c-filter-gevuld"
    (ev w "J1" (populated ~filter:(heftier "2.00") "gewicht"))
    "1";
  expect_num "WV-3.4c-filter-alles-weg-aantal"
    (ev w "J1" (quantity ~filter:(heftier "10.00") ()))
    "0";
  expect_num "WV-3.4c-filter-alles-weg-gevuld"
    (ev w "J1" (populated ~filter:(heftier "10.00") "gewicht"))
    "0";
  (* (4) the multi-step range counts ELEMENTS, and a shared one counts once *)
  let trail = Some [ rb "bemanning" "gevoerde boot"; rb "berging" "geborgen vondst" ] in
  let over_path = fold_node Filled_count [ set_source "gewicht" ] None trail in
  let boot label vondsten =
    { (inst label "Boot" []) with
      World.left_side = left_side [ ("geborgen vondsten", vondsten) ] }
  in
  let schipper label boten =
    { (inst label "Jutter" []) with World.left_side = left_side [ ("gevoerde boten", boten) ] }
  in
  let vondsten =
    [ inst "V1" "Vondst" [ ("gewicht", n "3.00") ]; inst "V2" "Vondst" [];
      inst "V3" "Vondst" [ ("gewicht", n "5.00") ] ]
  in
  let w2 =
    world_state
      ([ schipper "J1" [ "B1"; "B2" ]; boot "B1" [ "V1"; "V2" ]; boot "B2" [ "V3" ] ] @ vondsten)
  in
  let w3 =
    world_state
      ([ schipper "J1" [ "B1"; "B2" ]; boot "B1" [ "V1"; "V2" ]; boot "B2" [ "V1"; "V3" ] ]
      @ vondsten)
  in
  expect_num "WV-3.4c-bronpad-vlakke-unie" (ev w2 "J1" over_path) "2";
  expect_num "WV-3.4c-bronpad-gedeeld-element" (ev w3 "J1" over_path) "2";
  (* the second line of defence: a source that names a role and no
     attribute of the element type is a count source, which this kind
     cannot read — refused by name instead of counting leeg members *)
  expect_fault_msg "WV-3.4c-kale-rolbron-faalt"
    (fun () -> ev w "J1" (fold_node Filled_count [ tally_source ] None None))
    "a role name is a count source only a count folds";
  (* WF-093 (a) and WF-034, split as the verifier's source reading and the
     Drools loader split them: with the fold subject known, a role reachable
     from it is a count source (WF-093 (a)), and any other name declares
     nothing on the element type (WF-034) — a role of another type's
     relation included ("geborgen vondsten" is Boot's, not the Jutter's) *)
  let ev_jutter e =
    Eval.eval_expr { env with Eval.subj = Some "Jutter" } w (World.get w "J1") ~err:no_err e
  in
  expect_fault_msg "WV-3.4c-bereikbare-rolbron-faalt"
    (fun () -> ev_jutter (fold_node Filled_count [ tally_source ] None None))
    "a role name is a count source only a count folds";
  expect_fault_msg "WV-3.4c-onbereikbare-rolbron-faalt"
    (fun () -> ev_jutter (fold_node Filled_count [ set_source "geborgen vondsten" ] None None))
    "'geborgen vondsten' is not a declared attribute of 'Vondst': a value fold's source reads \
     an attribute of each contributor";
  expect_fault_msg "WV-3.4c-onbekende-bron-faalt"
    (fun () -> ev_jutter (fold_node Filled_count [ set_source "onbekend" ] None None))
    "'onbekend' is not a declared attribute of 'Vondst': a value fold's source reads an \
     attribute of each contributor";
  (* D9's other side, WF-093 (b): a count over a name that IS a member of the
     element type reads values, which a count does not fold — refused by name,
     for an attribute and for a characteristic alike *)
  expect_fault_msg "WV-3.4c-telling-over-attribuut-faalt"
    (fun () -> ev w "J1" (fold_node Count [ set_source "gewicht" ] None None))
    "reads 'gewicht', a member of the element type 'Vondst': a count folds INSTANCES";
  expect_fault_msg "WV-3.4c-telling-over-kenmerk-faalt"
    (fun () -> ev w "J1" (fold_node Count [ set_source "gekeurd" ] None None))
    "reads 'gekeurd', a member of the element type 'Vondst': a count folds INSTANCES";
  (* ch. 3.4c's ONE exclusion, and it is on the temporal axis rather than on
     a value type: a set read of a TIME-DEPENDENT attribute folds twice, and
     only som and totaal define the first collapse (ch. 3.6, §7.1). A count
     has no defined MOMENT at which to ask a timeline whether it is filled,
     so it must fail NAMING the attribute instead of reading a value the
     timeline does not have at an instant. Until 2026-09-20 this behaviour
     was correct and undocumented — the chapter said "whatever its type" and
     the exclusion lived only in a code comment, which carries no norm. *)
  let doc_t =
    let ident, o = ot "Vondst" [ ("vergoeding", Some num2kg) ] [] in
    { doc with
      object_types =
        [ ot "Jutter" [] []; (ident, { o with attribute_granularity = [ ("vergoeding", Some Day) ] }) ] }
  in
  let env_t = env_of doc_t in
  (* the needle NAMES THE FOLD. Until 2026-09-20 it read "has no defined moment
     to fold", which is the shared fall-through of every kind that reaches this
     position — so the check passed identically for minimum, maximum, eerste and
     laatste, and the worked example proved nothing about the filled count. The
     chapter's own sentence is the one pinned here now, and the red case
     `filled-count-temporal-set-source` pins the same words. *)
  expect_fault_msg "WV-3.4c-temporeel-faalt"
    (fun () ->
      Eval.eval_expr env_t w (World.get w "J1") ~err:no_err
        (populated "vergoeding"))
    "no moment at which a filled count can ask a timeline whether it is filled";
  (* the sister measurement, and it is what makes the line above a witness: the
     SAME document under a ranking fold must NOT reach that sentence *)
  expect_fault_msg "WV-3.4c-temporeel-rangschikking-eigen-zin"
    (fun () ->
      Eval.eval_expr env_t w (World.get w "J1") ~err:no_err
        (fold_node Minimum [ set_source "vergoeding" ] None None))
    "has no defined moment to fold"

(* WV-3.4d: the INSTANCE-SET UNION — the §5.7 concatenation of subject
   expressions as the source of an instance count (ch. 3.4d, wire form and
   well-formedness IR-SPEC ch. 4.3 / WF-110, maintainer decision 2026-09-20).

   What these lines pin is the OPERATION: the members are the union of the
   selections' contributors, DEDUPLICATED ON INSTANCE IDENTITY, so an
   instance two selections both select contributes once. Every check below
   is chosen so that the sum of the parts is a DIFFERENT number from the
   union — otherwise the witness would pass under an implementation that
   simply added the selections' counts. *)
let () =
  let role_ref ident otn mv plural_flag : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some otn; role_plural = Some mv;
      role_many = plural_flag }
  in
  let ft ident role_names = (ident, { Ir_v0.Ast.ft_name = Some ident; reciprocal = false; role_names }) in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [ ("aantal strandvondsten", Some num0) ] [];
          ot "Vondst" [] [ "gemerkt"; "gekeurd" ] ];
      fact_types =
        [ ft "vondstaanmelding"
            [ role_ref "aanmelder" "Jutter" "aanmelders" false;
              role_ref "aangemelde vondst" "Vondst" "aangemelde vondsten" true ];
          ft "vondstberging"
            [ role_ref "berger" "Jutter" "bergers" false;
              role_ref "geborgen vondst" "Vondst" "geborgen vondsten" true ] ];
    }
  in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let branch_node ?filter ft r : Ir_v0.Ast.instance_set_branch =
    { isb_element_type = Some "Vondst"; isb_source_role = rb ft r;
      isb_filter = filter; isb_source = None }
  in
  let characteristic k : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some k; navigation_binding = None; negation = false;
                    holder = None; time_period = None; source_name = None }
  in
  let union branch_list : Ir_v0.Ast.expr = Instance_set_union { branches = branch_list; source_name = None } in
  let tally branch_list : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments = [ union branch_list ]; element_object_type = None; empty_fallback = None;
        filter = None; dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role = None; source_path = None }
  in
  let over_relations =
    tally [ branch_node "vondstaanmelding" "aangemelde vondst"; branch_node "vondstberging" "geborgen vondst" ]
  in
  let over_characteristics =
    tally
      [ branch_node ~filter:(characteristic "gemerkt") "vondstaanmelding" "aangemelde vondst";
        branch_node ~filter:(characteristic "gekeurd") "vondstaanmelding" "aangemelde vondst" ]
  in
  let filtered_beside_bare =
    tally
      [ branch_node ~filter:(characteristic "gemerkt") "vondstaanmelding" "aangemelde vondst";
        branch_node "vondstaanmelding" "aangemelde vondst" ]
  in
  let env = env_of doc in
  let left_side l = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty l in
  let jutter label l = { (inst label "Jutter" []) with World.left_side = left_side l } in
  let vondst label characteristics = { (inst label "Vondst" []) with World.chars = characteristics } in
  let world_state insts = List.fold_left World.add_instance World.empty insts in
  let ev w label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  (* selections over DIFFERENT relations — the shared element is what makes
     this a dedup witness and not an addition witness *)
  let w1 =
    world_state
      [ jutter "J1" [ ("aangemelde vondst", [ "V1"; "V2"; "V3" ]);
                      ("geborgen vondst", [ "V3"; "V4" ]) ];
        jutter "J2" [ ("aangemelde vondst", [ "V5" ]); ("geborgen vondst", [ "V5" ]) ];
        jutter "J3" [ ("geborgen vondst", [ "V6" ]) ];
        jutter "J4" [];
        vondst "V1" []; vondst "V2" []; vondst "V3" []; vondst "V4" [];
        vondst "V5" []; vondst "V6" [] ]
  in
  (* {V1,V2,V3} ∪ {V3,V4} = 4, where the sum of the parts is 5 *)
  expect_num "WV-3.4d-gedeeld-element" (ev w1 "J1" over_relations) "4";
  (* both selections are {V5}: 1, where the sum of the parts is 2 *)
  expect_num "WV-3.4d-volledig-gedeeld" (ev w1 "J2" over_relations) "1";
  (* one empty selection contributes nothing rather than emptying the union *)
  expect_num "WV-3.4d-lege-tak" (ev w1 "J3" over_relations) "1";
  (* the empty union is 0, never leeg (ch. 1.2e) *)
  expect_num "WV-3.4d-lege-unie" (ev w1 "J4" over_relations) "0";
  (* selections over ONE relation, narrowed by their own filters per
     contributor: V3 satisfies both and counts once *)
  let w2 =
    world_state
      [ jutter "J1" [ ("aangemelde vondst", [ "V1"; "V2"; "V3" ]) ];
        jutter "J2" [ ("aangemelde vondst", [ "V7" ]) ];
        vondst "V1" [ "gemerkt" ]; vondst "V2" [ "gekeurd" ];
        vondst "V3" [ "gemerkt"; "gekeurd" ]; vondst "V7" [] ]
  in
  (* {V1,V3} ∪ {V2,V3} = 3, where the sum of the parts is 4 *)
  expect_num "WV-3.4d-overlappende-filters" (ev w2 "J1" over_characteristics) "3";
  expect_num "WV-3.4d-filters-sluiten-alles-uit" (ev w2 "J2" over_characteristics) "0";
  (* the toolchain leaves an unfiltered selection beside a filtered one
     unlowered — a property of its OR-filter mechanism, not of the operation.
     uchar carries it: the union absorbs the filtered selection. *)
  expect_num "WV-3.4d-kale-tak-naast-gefilterde" (ev w2 "J1" filtered_beside_bare) "3";
  expect_num "WV-3.4d-kale-tak-draagt-alleen" (ev w2 "J2" filtered_beside_bare) "1";
  (* two selections describing the SAME thing count that selection once *)
  expect_num "WV-3.4d-dubbele-tak"
    (ev w1 "J1"
       (tally [ branch_node "vondstaanmelding" "aangemelde vondst";
              branch_node "vondstaanmelding" "aangemelde vondst" ]))
    "3";
  (* the node is not a value: standing where one is read it faults by name,
     never silently. WF-110 (a) is what keeps this out of reach on the wire;
     the arm is the second line of defence, on the house pattern. *)
  expect_fault_msg "WV-3.4d-unie-als-waarde-faalt"
    (fun () -> ev w1 "J1" (union [ branch_node "vondstaanmelding" "aangemelde vondst" ]))
    "an instance-set union stands where a value is read";
  (* the SECOND LINE for clause (d): one population for the whole union. The
     wire refuses the shape, so this arm is out of reach through a well-formed
     document — and without it this evaluator ANSWERED it (measured 2026-09-22
     on `wf110-union-unequal-element-types`: a silent 3, while both Drools
     branches refused by name). ch. 4.3 says the rule refuses instead of
     picking an answer by side effect; an executor that answers anyway makes
     that sentence untrue exactly where it matters. *)
  expect_fault_msg "WV-3.4d-twee-populaties-faalt"
    (fun () ->
      ev w1 "J1"
        (tally [ branch_node "vondstaanmelding" "aangemelde vondst";
               { isb_element_type = Some "Kavel";
                 isb_source_role = rb "vondstberging" "geborgen vondst";
                 isb_filter = None; isb_source = None } ]))
    "ranges over TWO populations";
  (* WF-110 (c), second line: a count over a union names no source of its own.
     The wire refuses an elementType, a range or a filter on that node beside
     the union (IR-SPEC ch. 4.3). Until the card aantal-bronarm-en-drools-lader
     this evaluator counted the union with a range alone as if the range were
     not written, refused the count with an elementType in its residual
     sentence and the filter in a sentence of its own. Each line pins the
     verifier's sentence with the fields it names, in the verifier's order. *)
  let with_source ?el ?role_ref ?filter () : Ir_v0.Ast.expr =
    Function_call
      { kind = Count;
        call_arguments =
          [ union [ branch_node "vondstaanmelding" "aangemelde vondst"; branch_node "vondstberging" "geborgen vondst" ] ];
        element_object_type = el; empty_fallback = None; filter; dimension_selection = None;
        typ = Some num0; source_name = None; grain = None; source_role = role_ref; source_path = None }
  in
  let sentence_text field_list =
    Printf.sprintf
      "a count over an instance-set union names no source of its own, and this node carries %s: \
       the union's selections carry the population, the relation and the subselection, one set \
       per selection, so a source here is a second claim on one node (ch. 4.3)"
      field_list
  in
  let value_range = rb "vondstaanmelding" "aangemelde vondst" in
  expect_fault_msg "WF-110c-elementtype-en-bronrol"
    (fun () -> ev w1 "J1" (with_source ~el:"Vondst" ~role_ref:value_range ()))
    (sentence_text "elementType + sourceRole");
  expect_fault_msg "WF-110c-alleen-bronrol" (fun () -> ev w1 "J1" (with_source ~role_ref:value_range ()))
    (sentence_text "sourceRole");
  expect_fault_msg "WF-110c-alleen-filter"
    (fun () -> ev w1 "J1" (with_source ~filter:(characteristic "gemerkt") ()))
    (sentence_text "filter")

(* WV-3.4a-opgesomd: a count handed VALUES instead of instances, and why this
   evaluator now refuses it by name.

   Every shape that reaches the value arm under `Count` is fenced on the
   wire: the listed operand list of two or more by WF-037 (2026-09-20) and
   the count of ONE operand or none without an element type by WF-037
   (2026-09-23); a ranged single source counts instances since L2. Until 2026-09-23
   the arm answered — the count of the FILLED operands, 1 on one filled own
   read, 0 on none — and these six lines pinned that answer, the last three
   as the open flank a well-formed document still reached. The flank is
   closed now, so the arm is out of reach through any well-formed document,
   and a second line that answers a form nobody may write answers silently.
   It refuses instead, and these lines pin the refusal at every arity the
   wire refuses: two operands, one, none. The fragment is the construct
   named, not the position, so it witnesses this arm and no other.

   The operands are the SUBJECT's own reads, which is the whole of the form:
   V5 carries a gewicht and no vindplaats, V6 carries both, V7 neither — the
   three subjects that told the filled-count answer apart, kept so that a
   return to it fails here on the first line. *)
let () =
  let num2kg : Ir_v0.Ast.datatype =
    Number
      { whole_part = false; decimal_places = 2; sign_char = None;
        measure_unit =
          Some { abbr_text = Some "kg"; system = Some "Gewicht"; canonical_form = Some "kg";
                 factor_to_canonical = Some { num = 1; den = 1 }; denominator = None } }
  in
  let doc =
    { empty_doc with
      object_types = [ ot "Vondst" [ ("gewicht", Some num2kg); ("vindplaats", Some Text_type) ] [] ] }
  in
  let env = env_of doc in
  let w =
    List.fold_left World.add_instance World.empty
      [ inst "V5" "Vondst" [ ("gewicht", n "5.00") ];
        inst "V6" "Vondst" [ ("gewicht", n "7.00"); ("vindplaats", Value.Text "Zuiderhoofd") ];
        inst "V7" "Vondst" [] ]
  in
  let enumerated call_arguments : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments; element_object_type = None; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = None; source_path = None }
  in
  let proportion = aref "gewicht" "Vondst" in
  let vindplaats = aref "vindplaats" "Vondst" in
  let ev label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  let hint = "a count folds INSTANCES and this node hands it" in
  (* the listed form, two operands (WF-037, 2026-09-20) *)
  expect_fault_msg "WV-3.4a-opgesomd-tweede-linie"
    (fun () -> ev "V5" (enumerated [ proportion; vindplaats ])) hint;
  expect_fault_msg "WV-3.4a-opgesomd-alles-gevuld"
    (fun () -> ev "V6" (enumerated [ proportion; vindplaats ])) hint;
  expect_fault_msg "WV-3.4a-opgesomd-alles-leeg"
    (fun () -> ev "V7" (enumerated [ proportion; vindplaats ])) hint;
  (* the count without a source, one operand or none (WF-037, 2026-09-23) —
     the flank these three pinned open until that date *)
  expect_fault_msg "WV-3.4a-eenoperand-gevuld" (fun () -> ev "V5" (enumerated [ proportion ])) hint;
  expect_fault_msg "WV-3.4a-eenoperand-leeg" (fun () -> ev "V7" (enumerated [ proportion ])) hint;
  expect_fault_msg "WV-3.4a-geen-operand" (fun () -> ev "V5" (enumerated [])) hint;
  (* the spelling that SHOWED the defect: one list literal. Until 2026-09-23
     both answered 1 — the operand counted, never its contents *)
  let element_list : Ir_v0.Ast.datatype = List_type { element = num0 } in
  let list_lit elements : Ir_v0.Ast.expr = List_literal { typ = Some element_list; elements; source_name = None } in
  expect_fault_msg "WV-3.4a-lijstliteraal-twee"
    (fun () -> ev "V5" (enumerated [ list_lit [ litv "1" num0; litv "2" num0 ] ])) hint;
  expect_fault_msg "WV-3.4a-lijstliteraal-leeg" (fun () -> ev "V5" (enumerated [ list_lit [] ])) hint

(* WF-037, second line: a fold that names its population and hands no source.

   The wire refuses both shapes (IR-SPEC ch. 4.3), so no well-formed document
   reaches these arms. Until the card aantal-bronargument-welgevormd this
   evaluator refused them in its residual sentence, `unsupported instance-fold
   source shape`, which names the position and not the construct. Each line
   pins the verifier's sentence for one kind and one shape: no argument, under
   every aggregation kind and without a range (the shape that was
   verifier-clean), and one ListLiteral under the two counts, with the range
   the loader demands of a single-argument fold. The last two lines pin the
   boundary: a ListLiteral under a value fold keeps the refusal it had, and a
   Literal under a count takes the source arm's sentence (the next block), not
   this arm's. *)
let () =
  let doc = { empty_doc with object_types = [ ot "Vondst" [ ("gewicht", Some num0) ] [] ] } in
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "V1" "Vondst" [ ("gewicht", n "2") ]) in
  let ev e = Eval.eval_expr env w (World.get w "V1") ~err:no_err e in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let fold_node ?source_role kind call_arguments : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments; element_object_type = Some "Vondst"; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None; source_role;
        source_path = None }
  in
  let element_list : Ir_v0.Ast.expr =
    List_literal
      { typ = Some (List_type { element = num0 }); elements = [ litv "1" num0; litv "2" num0 ];
        source_name = None }
  in
  let value_range = rb "vondstaanmelding" "vondst" in
  List.iter
    (fun (kind, ident) ->
      expect_fault_msg
        ("WF-037-elementtype-zonder-operand-" ^ ident)
        (fun () -> ev (fold_node kind []))
        (Printf.sprintf "a %s aggregation carries elementType \"Vondst\" but no source operand"
           ident))
    [ (Ir_v0.Ast.Count, "count"); (Filled_count, "filledCount"); (Sum_function, "sum");
      (Total, "total"); (Minimum, "minimum"); (Maximum, "maximum"); (First, "first");
      (Last, "last") ];
  expect_fault_msg "WF-037-elementtype-lijstliteraal-count"
    (fun () -> ev (fold_node ~source_role:value_range Count [ element_list ]))
    "a count aggregation carries elementType \"Vondst\" and a ListLiteral operand";
  expect_fault_msg "WF-037-elementtype-lijstliteraal-filledCount"
    (fun () -> ev (fold_node ~source_role:value_range Filled_count [ element_list ]))
    "a filledCount aggregation carries elementType \"Vondst\" and a ListLiteral operand";
  let not_this_arm name e =
    match ev e with
    | exception Value.Fault { text = m; _ } ->
        check (name ^ " (got: " ^ m ^ ")")
          (not (substring m "no source operand" || substring m "no source of instances"))
    | _ -> check (name ^ " (no fault at all)") false
  in
  not_this_arm "WF-037-elementtype-lijstliteraal-onder-sum-niet-deze-arm"
    (fold_node ~source_role:value_range Sum_function [ element_list ]);
  not_this_arm "WF-037-elementtype-literal-onder-count-niet-deze-arm"
    (fold_node ~source_role:value_range Count [ litv "2" num0 ])

(* WF-037, second line: the fold-source arm.

   The one argument of an aggregation that carries an elementType is an
   AttributeReference, its fold source (IR-SPEC ch. 4.3); the wire refuses
   every other node there and names it, so no well-formed document reaches
   this arm. Until the card aantal-bronarm-en-drools-lader this evaluator
   refused them in its residual sentence, `unsupported instance-fold source
   shape`. Each line pins the verifier's sentence for one kind and one node:
   seven nodes under the count, two under the filled count (one of them the
   union, which there is a non-source like any other), a literal under each
   value fold, a parameter under the sum, and a literal as the argument of a
   total without a range. The last line pins the boundary: a union under a
   count that carries its own source is WF-110 (c)'s, and does not take this
   arm's sentence. *)
let () =
  let doc = { empty_doc with object_types = [ ot "Vondst" [ ("gewicht", Some num0) ] [] ] } in
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "V1" "Vondst" [ ("gewicht", n "2") ]) in
  let ev e = Eval.eval_expr env w (World.get w "V1") ~err:no_err e in
  let value_range : Ir_v0.Ast.role_binding = { rb_fact_type = "vondstaanmelding"; rb_role = "vondst" } in
  let fold_node ?(element = Some "Vondst") kind call_arguments : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments; element_object_type = element; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = Some value_range; source_path = None }
  in
  let two = litv "2" num0 in
  let union : Ir_v0.Ast.expr =
    let branch_node : Ir_v0.Ast.instance_set_branch =
      { isb_element_type = Some "Vondst"; isb_source_role = value_range; isb_filter = None;
        isb_source = None }
    in
    Instance_set_union { branches = [ branch_node; branch_node ]; source_name = None }
  in
  let form_list : (string * Ir_v0.Ast.expr) list =
    [ ("a Literal", two);
      ("a Binary", Binary { op = Plus; left_side = Some two; right_side = Some two; typ = Some num0; source_name = None });
      ("a ParameterReference", Parameter_ref { parameter = Some "drempel"; source_name = None });
      ("a VariableReference", Variable_reference { ident = Some "x"; source_name = None });
      ("a BuiltinCall",
        Function_call
          { kind = Absolute_value; call_arguments = [ two ]; element_object_type = None;
            empty_fallback = None; filter = None; dimension_selection = None; typ = Some num0;
            source_name = None; grain = None; source_role = None; source_path = None });
      ("an Unresolved", Expr_unresolved { typ = Some num0; source_name = Some "iets onbekends" });
      ("an Aggregation", fold_node Count [ aref "vondsten" "Vondst" ]) ]
  in
  List.iter
    (fun (graph_node, arg) ->
      expect_fault_msg
        ("WF-037-bronarm-count-" ^ graph_node)
        (fun () -> ev (fold_node Count [ arg ]))
        (Printf.sprintf
           "a count aggregation carries elementType \"Vondst\" and %s operand: the source of an \
            instance fold is an AttributeReference"
           graph_node))
    form_list;
  (* the value folds, whose fold source is an AttributeReference too: with a range, and the total
     without one, whose argument is the subject's own read (the subject here is a Vondst) *)
  List.iter
    (fun (kind, ident) ->
      expect_fault_msg
        ("WF-037-bronarm-" ^ ident ^ "-a Literal")
        (fun () -> ev (fold_node kind [ two ]))
        (Printf.sprintf "a %s aggregation carries elementType \"Vondst\" and a Literal operand" ident))
    [ (Ir_v0.Ast.Sum_function, "sum"); (Total, "total"); (Minimum, "minimum"); (Maximum, "maximum");
      (First, "first"); (Last, "last") ];
  expect_fault_msg "WF-037-bronarm-sum-a ParameterReference"
    (fun () -> ev (fold_node Sum_function [ Parameter_ref { parameter = Some "drempel"; source_name = None } ]))
    "a sum aggregation carries elementType \"Vondst\" and a ParameterReference operand";
  expect_fault_msg "WF-037-bronarm-total-zonder-bereik-a Literal"
    (fun () ->
      ev
        (Function_call
           { kind = Total; call_arguments = [ two ]; element_object_type = Some "Vondst"; empty_fallback = None;
             filter = None; dimension_selection = None; typ = Some num0; source_name = None; grain = None;
             source_role = None; source_path = None }))
    "a total aggregation carries elementType \"Vondst\" and a Literal operand";
  expect_fault_msg "WF-037-bronarm-filledCount-a Literal"
    (fun () -> ev (fold_node Filled_count [ two ]))
    "a filledCount aggregation carries elementType \"Vondst\" and a Literal operand";
  expect_fault_msg "WF-037-bronarm-filledCount-an InstanceSetUnion"
    (fun () -> ev (fold_node Filled_count [ union ]))
    "a filledCount aggregation carries elementType \"Vondst\" and an InstanceSetUnion operand";
  match ev (fold_node Count [ union ]) with
  | exception Value.Fault { text = m; _ } ->
      check ("WF-037-bronarm-unie-onder-count-niet-deze-arm (got: " ^ m ^ ")")
        (not (substring m "the source of an instance fold is an AttributeReference"))
  | _ -> check "WF-037-bronarm-unie-onder-count-niet-deze-arm (no fault at all)" false

(* --- ch. 3.10: the period condition's evaluation-date gate -------------- *)

(* WV-3.10: the ch. 3.10 worked example, verbatim — window
   [2024-01-01, 2025-01-01), evaluation date 2024-12-31 applies,
   2025-01-01 does not, and the "tot en met 2024-12-31" form still applies
   on 2024-12-31 — plus the three clauses the example does not spell out:
   an absent bound is open, an unset evaluation date never matches, a leeg
   bound never matches.

   The corpus witness (`corpus/golden/jutterswet-premievenster`) pins the
   same six clauses through all four executors; these run the chapter's own
   sentences at the level the chapter states them, and they carry the two
   refusals no corpus golden can hold: the bound shape the mirror note
   defers (red-witnessed as well) and ch. 3.10's OTHER case, the clip on a
   temporal target, which is decided at the rule and never in the
   condition. *)
let () =
  let date_value : Ir_v0.Ast.datatype = Date_in_days in
  let window ?starting_at ?up_to ~incl () : Ir_v0.Ast.condition_expr =
    Time_window
      { starting_at = Option.map (fun d -> litv d date_value) starting_at;
        up_to = Option.map (fun d -> litv d date_value) up_to;
        to_inclusive = incl; source_name = None }
  in
  let doc = { empty_doc with object_types = [ ot "Vondst" [ ("meldatum", Some date_value) ] [] ] } in
  let w = World.add_instance World.empty (inst "V1" "Vondst" [ ("meldatum", Value.Date 19783) ]) in
  let empty_w = World.add_instance World.empty (inst "V2" "Vondst" []) in
  let gate ?(world = w) ?(label = "V1") as_of c =
    Eval.eval_cond { (env_of doc) with as_of } world (World.get world label) ~err:no_err c
  in
  let halfopen = window ~starting_at:"2024-01-01" ~up_to:"2025-01-01" ~incl:false () in
  let inclusive = window ~starting_at:"2024-01-01" ~up_to:"2024-12-31" ~incl:true () in
  (* the worked example, sentence by sentence *)
  check "WV-3.10-voorbeeld-2024-12-31-geldt" (gate (Some "2024-12-31") halfopen = true);
  check "WV-3.10-voorbeeld-2025-01-01-geldt-niet" (gate (Some "2025-01-01") halfopen = false);
  check "WV-3.10-voorbeeld-totenmet-2024-12-31" (gate (Some "2024-12-31") inclusive = true);
  (* vanaf inclusive, and the day before it *)
  check "WV-3.10-vanaf-inclusief" (gate (Some "2024-01-01") halfopen = true);
  check "WV-3.10-voor-vanaf" (gate (Some "2023-12-31") halfopen = false);
  (* tot en met is exactly one day wider than tot *)
  check "WV-3.10-totenmet-een-dag-breder" (gate (Some "2024-12-31") inclusive = true);
  check "WV-3.10-totenmet-dag-erna" (gate (Some "2025-01-01") inclusive = false);
  (* an absent bound is open, in both directions *)
  check "WV-3.10-open-boven"
    (gate (Some "2999-12-31") (window ~starting_at:"2024-01-01" ~incl:false ()) = true);
  check "WV-3.10-open-onder"
    (gate (Some "1900-01-01") (window ~up_to:"2024-01-01" ~incl:false ()) = true);
  check "WV-3.10-open-onder-op-de-grens"
    (gate (Some "2024-01-01") (window ~up_to:"2024-01-01" ~incl:false ()) = false);
  (* an unset evaluation date never matches — not a fault, an answer *)
  check "WV-3.10-tijdvrij-matcht-nooit" (gate None halfopen = false);
  check "WV-3.10-tijdvrij-ook-open-venster"
    (gate None (window ~starting_at:"1900-01-01" ~incl:false ()) = false);
  (* a leeg bound never matches: the same window, one subject carrying the
     bound attribute and one not *)
  let own_bound : Ir_v0.Ast.condition_expr =
    Time_window
      { starting_at = Some (aref "meldatum" "Vondst"); up_to = None; to_inclusive = false;
        source_name = None }
  in
  check "WV-3.10-eigen-grens-na" (gate (Some "2024-06-15") own_bound = true);
  check "WV-3.10-eigen-grens-voor" (gate (Some "2024-01-01") own_bound = false);
  check "WV-3.10-lege-grens-matcht-nooit"
    (gate ~world:empty_w ~label:"V2" (Some "2024-06-15") own_bound = false);
  (* the bound shape ch. 3.10's mirror note defers, refused by name and with
     the PHASE tail the ASP backend's twin sentence differs in *)
  expect_fault_msg "WV-3.10-grens-buiten-de-mirror-note"
    (fun () ->
      gate (Some "2024-06-15")
        (Time_window
           { starting_at = Some (Parameter_ref { parameter = Some "peildatum"; source_name = None });
             up_to = None; to_inclusive = false; source_name = None }))
    "neither a date literal nor an own scalar date attribute of the subject";
  expect_fault_msg "WV-3.10-grens-fasestaart"
    (fun () ->
      gate (Some "2024-06-15")
        (Time_window
           { starting_at = Some (Parameter_ref { parameter = Some "peildatum"; source_name = None });
             up_to = None; to_inclusive = false; source_name = None }))
    "Declined at evaluation";
  (* WF-042's bound-less window has no gate to compute *)
  expect_fault_msg "WV-3.10-venster-zonder-grens"
    (fun () -> gate (Some "2024-06-15") (window ~incl:false ()))
    "without any bound has no window to gate on";
  (* the mirror note's SECOND bound: the window under a quantifier that counts
     or complements its verdict. The boundary is the one measured on the other
     three executors — `all` and `the` carried, the rest refused — and it is pinned
     from BOTH sides here, because a refusal that quietly widened would look
     exactly like a refusal that did not. *)
  let compound quantifier_kind cutoff : Ir_v0.Ast.condition_expr =
    Compound { quantifier_kind; cutoff; components = [ halfopen ]; source_name = None }
  in
  List.iter
    (fun (ident, quantifier_kind, cutoff) ->
      expect_fault_msg ("WV-3.10-telkwantor-" ^ ident)
        (fun () -> gate (Some "2024-06-15") (compound quantifier_kind cutoff))
        "under none or a counting quantifier")
    [ ("geen", Ir_v0.Ast.None_of, 0); ("tenminste", At_least, 1); ("tenhoogste", At_most, 1);
      ("precies", Exactly, 1) ];
  check "WV-3.10-telkwantor-fasestaart"
    (match gate (Some "2024-06-15") (compound None_of 0) with
     | (_ : bool) -> false
     | exception Value.Fault { text = m; _ } -> substring m "Declined at evaluation");
  check "WV-3.10-alle-blijft-gedragen" (gate (Some "2024-06-15") (compound All 0) = true);
  check "WV-3.10-de-blijft-gedragen" (gate (Some "2024-06-15") (compound The 0) = true);
  check "WV-3.10-alle-poort-werkt-nog" (gate (Some "2025-01-01") (compound All 0) = false)

(* WV-3.10-clip: ch. 3.10's SECOND case, built 2026-09-23. On a temporal
   target the window CLIPS the written timeline instead of gating the rule:
   the worked example verbatim ({2024: 10, 2025: 12} yearly, window
   [2025-01-01, open) -> {2025: 12}); the evaluation date plays no part; and
   the clause the corpus cannot hold — a bound inside a bucket cuts the
   segment and keeps its value — because the case format pins every expected
   timeline to the declared grid. The corpus witness is
   `corpus/golden/jutterswet-pachtvenster`.

   Until that day this block pinned the REFUSAL of the attribute-target
   clip; building the clip turned those two lines into the lines below. The
   refusals that remain are decided at the rule, before a subject is taken:
   the window as a composite MEMBER, and a CHARACTERISTIC target. *)
let () =
  let date_value : Ir_v0.Ast.datatype = Date_in_days in
  let day_number iso = match Value.date_of_iso iso with Value.Date d -> d | _ -> assert false in
  let year = Some Ir_v0.Ast.Year in
  let vondst =
    let n, o = ot "Vondst" [ ("vergoeding", Some num0); ("tarief", Some num0) ] [ "actief" ] in
    ( n,
      { o with
        attribute_granularity = [ ("vergoeding", year); ("tarief", year) ];
        characteristic_granularity = [ ("actief", year) ] } )
  in
  let prorata : Ir_v0.Ast.expr =
    Function_call
      { kind = Time_proportional_part; call_arguments = [ aref "tarief" "Vondst" ];
        element_object_type = None; empty_fallback = None;
        filter =
          Some
            (Characteristic_check
               { characteristic = Some "actief"; negation = false; holder = None;
                 time_period = None; source_name = None; navigation_binding = None });
        dimension_selection = None; typ = Some num0; source_name = None; grain = year;
        source_role = None; source_path = None }
  in
  let window ?starting_at ?up_to ?(incl = false) () : Ir_v0.Ast.condition_expr =
    Time_window
      { starting_at = Option.map (fun d -> litv d date_value) starting_at;
        up_to = Option.map (fun d -> litv d date_value) up_to; to_inclusive = incl; source_name = None }
  in
  let doc cond =
    { empty_doc with
      object_types = [ vondst ];
      resolved_rules =
        [ rr "vergoeding van een vondst" Calculation
            (Some (aref "vergoeding" "Vondst"))
            (Some prorata) (Some cond) ] }
  in
  let w =
    World.add_instance World.empty
      { (inst "V1" "Vondst" []) with
        tls =
          World.SMap.add "tarief"
            [ { Timeline.s_from = day_number "2024-01-01"; s_to = day_number "2025-01-01"; s_value = n "10" };
              { s_from = day_number "2025-01-01"; s_to = day_number "2026-01-01"; s_value = n "12" } ]
            World.SMap.empty;
        ctls = World.SMap.add "actief" [ (day_number "2024-01-01", day_number "2026-01-01") ] World.SMap.empty }
  in
  let written_value ?as_of cond =
    let d = doc cond in
    let w', _ = Eval.run_rule { (env_of d) with as_of } w (List.hd d.resolved_rules) in
    List.map
      (fun (s : Timeline.segment) -> (s.s_from, s.s_to, Value.to_string s.s_value))
      (Option.value (World.timeline (World.get w' "V1") "vergoeding") ~default:[])
  in
  let seg van up_to v = (day_number van, day_number up_to, Value.to_string (n v)) in
  (* the worked example, verbatim — and with no evaluation date at all *)
  check "WV-3.10-clip-voorbeeld"
    (written_value (window ~starting_at:"2025-01-01" ()) = [ seg "2025-01-01" "2026-01-01" "12" ]);
  (* the evaluation date plays no part: one outside the window writes the
     same timeline, where the scalar gate would have written nothing *)
  check "WV-3.10-clip-evaluatiedatum-speelt-geen-rol"
    (written_value ~as_of:"2023-06-01" (window ~starting_at:"2025-01-01" ())
     = [ seg "2025-01-01" "2026-01-01" "12" ]);
  (* the restriction cuts SEGMENTS and keeps VALUES: a window from mid-March
     to the end of June ("tot en met", so the window ends 1 July) keeps
     12 on the part of the 2025 bucket inside it, not 12 x 108/365 *)
  check "WV-3.10-clip-knipt-segment-houdt-waarde"
    (written_value (window ~starting_at:"2025-03-15" ~up_to:"2025-06-30" ~incl:true ())
     = [ seg "2025-03-15" "2025-07-01" "12" ]);
  (* two cuts in one segment and one across a bucket wall *)
  check "WV-3.10-clip-over-de-bucketwand"
    (written_value (window ~starting_at:"2024-07-01" ~up_to:"2025-04-01" ())
     = [ seg "2024-07-01" "2025-01-01" "10"; seg "2025-01-01" "2025-04-01" "12" ]);
  (* a window that misses the produced timeline leaves nothing of it *)
  check "WV-3.10-clip-gemist" (written_value (window ~up_to:"2024-01-01" ()) = []);
  (* the window as a MEMBER of a composite on a temporal target stays
     refused by name, with the phase tail the ASP backend's twin differs in *)
  let one_member =
    Ir_v0.Ast.Compound
      { quantifier_kind = All; cutoff = 0; components = [ window ~starting_at:"2025-01-01" () ]; source_name = None }
  in
  expect_fault_msg "WV-3.10-clip-als-lid-geweigerd"
    (fun () -> written_value one_member)
    "inside a composite on the temporal target";
  expect_fault_msg "WV-3.10-clip-als-lid-fasestaart" (fun () -> written_value one_member)
    "Declined at evaluation";
  (* the CHARACTERISTIC target, which declares its timeline in a different
     table (kenmerkGranulariteit). Reading the attribute table alone let this
     document through as a scalar target — measured 2026-09-20 in the ASP
     backend, which then PLAYED the gate on a timeline. Pinned here so the two
     tables stay asked apart; the clip is refused on it because no temporal
     characteristic is written (ch. 3.11). *)
  let kdoc =
    { empty_doc with
      object_types =
        [ (let n, o = ot "Vondst" [] [ "geborgen" ] in
           (n, { o with characteristic_granularity = [ ("geborgen", Some Ir_v0.Ast.Year) ] })) ];
      resolved_rules =
        [ rr "berging van een vondst" Characteristic_attribution
            (Some (aref "geborgen" "Vondst"))
            (Some (litv "waar" Boolean))
            (Some (window ~starting_at:"2024-01-01" ())) ] }
  in
  let kw = World.add_instance World.empty (inst "V1" "Vondst" []) in
  let kenv = { (env_of kdoc) with as_of = Some "2024-06-15" } in
  expect_fault_msg "WV-3.10-tijdlijnKENMERKdoel-is-de-clip"
    (fun () -> Eval.run_rule kenv kw (List.hd kdoc.resolved_rules))
    "is ch. 3.10's CLIP over a characteristic timeline"

(* WV-3.10-clip-dagtelling: ch. 3.10's sentence for the segment-cutting
   clause, verbatim — "A monthly day count of 31 cut at the fifth of the
   month still reads 31 on the days that remain." The corpus cannot hold it
   (the case format pins expected timelines to the grid), so it is pinned
   here, at the level the chapter states it. *)
let () =
  let day_number iso = match Value.date_of_iso iso with Value.Date d -> d | _ -> assert false in
  let month_kind = Some Ir_v0.Ast.Month in
  let vondst =
    let n, o = ot "Vondst" [ ("zomerdagen", Some num0) ] [ "aanwezig" ] in
    ( n,
      { o with
        attribute_granularity = [ ("zomerdagen", month_kind) ];
        characteristic_granularity = [ ("aanwezig", month_kind) ] } )
  in
  let day_count : Ir_v0.Ast.expr =
    Function_call
      { kind = Day_count; call_arguments = []; element_object_type = None; empty_fallback = None;
        filter =
          Some
            (Characteristic_check
               { characteristic = Some "aanwezig"; negation = false; holder = None;
                 time_period = None; source_name = None; navigation_binding = None });
        dimension_selection = None; typ = Some num0; source_name = None; grain = month_kind;
        source_role = None; source_path = None }
  in
  let window : Ir_v0.Ast.condition_expr =
    Time_window
      { starting_at = Some (litv "2025-07-05" Date_in_days); up_to = None; to_inclusive = false;
        source_name = None }
  in
  let doc =
    { empty_doc with
      object_types = [ vondst ];
      resolved_rules =
        [ rr "zomerdagen van een vondst" Equation
            (Some (aref "zomerdagen" "Vondst"))
            (Some day_count) (Some window) ] }
  in
  let w =
    World.add_instance World.empty
      { (inst "V1" "Vondst" []) with
        ctls = World.SMap.add "aanwezig" [ (day_number "2025-07-01", day_number "2025-08-01") ] World.SMap.empty }
  in
  let w', _ = Eval.run_rule (env_of doc) w (List.hd doc.resolved_rules) in
  let tl =
    List.map
      (fun (s : Timeline.segment) -> (s.s_from, s.s_to, Value.to_string s.s_value))
      (Option.value (World.timeline (World.get w' "V1") "zomerdagen") ~default:[])
  in
  check "WV-3.10-clip-dagtelling-31-gesneden-op-de-vijfde"
    (tl = [ (day_number "2025-07-05", day_number "2025-08-01", Value.to_string (n "31")) ])

(* --- ch. 5.7a: what a supplied role link establishes -------------------- *)

(* WV-5.7a: the `jutterswet-ketenband` worked example, verbatim (the
   golden's cases 1-3, replayed by gates 5 and 6). The fact type
   `ketenband` relates Wachter to Wachter through two distinct roles:
   `voorligger` (to-one) and `opvolger` (plural `opvolgers`, to-many).
   Three Wachters with rangnummer 10, 20, 30 and ONE relation — W1 is
   the voorligger of both W2 and W3 — spelled three ways: both legs, the
   to-many leg only, the to-one legs only. All three read identically,
   and that identity IS the law: a supplied link establishes the
   relation instance, and both roles read it.

   The values naming the UNSUPPLIED direction carry the weight — row 2
   reads W2.voorliggerrang = 10 though no case line writes anything on
   W2, row 3 reads W1.opvolgeraantal = 2 though no case line writes
   anything on W1. W1's own voorliggerrang stays leeg in every row: no
   one fills `voorligger` toward W1, and ch. 5.8 rule 1 reads leeg
   there — establishing a relation in both directions is not the same
   as inventing one. *)
let () =
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Wachter"
            [ ("rangnummer", Some num0); ("voorliggerrang", Some num0);
              ("opvolgeraantal", Some num0) ]
            [] ];
      fact_types =
        [ ( "ketenband",
            {
              ft_name = Some "ketenband";
              reciprocal = false;
              role_names =
                [
                  { role_identifier = Some "voorligger"; role_object_type_name = Some "Wachter";
                    role_plural = Some "voorliggers"; role_many = false };
                  { role_identifier = Some "opvolger"; role_object_type_name = Some "Wachter";
                    role_plural = Some "opvolgers"; role_many = true };
                ];
            } ) ];
    }
  in
  let voorliggerrang : Ir_v0.Ast.expr =
    Attribute_reference { attribute_ref = Some "rangnummer"; object_type = Some "Wachter"; source_name = None; navigation_bindings = Some [ { rb_fact_type = "ketenband"; rb_role = "voorligger" } ] }
  in
  let opvolgeraantal : Ir_v0.Ast.expr =
    Function_call
      {
        kind = Count;
        call_arguments =
          [ Attribute_reference { attribute_ref = Some "opvolgers"; object_type = Some "Wachter"; source_name = None; navigation_bindings = None } ];
        element_object_type = Some "Wachter"; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = Some { rb_fact_type = "ketenband"; rb_role = "opvolger" };
        source_path = None;
      }
  in
  let env = env_of doc in
  let wachter label ranking left_side =
    { (inst label "Wachter" [ ("rangnummer", n ranking) ]) with
      World.left_side =
        List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty left_side }
  in
  (* the loader's own pass, on the links a case supplies *)
  let supplied supplies =
    Case.materialise_relations doc (List.fold_left World.add_instance World.empty supplies)
  in
  let both_sides =
    supplied
      [ wachter "W1" "10" [ ("opvolgers", [ "W2"; "W3" ]) ];
        wachter "W2" "20" [ ("voorligger", [ "W1" ]) ];
        wachter "W3" "30" [ ("voorligger", [ "W1" ]) ] ]
  in
  let to_many =
    supplied
      [ wachter "W1" "10" [ ("opvolgers", [ "W2"; "W3" ]) ];
        wachter "W2" "20" []; wachter "W3" "30" [] ]
  in
  let to_one =
    supplied
      [ wachter "W1" "10" [];
        wachter "W2" "20" [ ("voorligger", [ "W1" ]) ];
        wachter "W3" "30" [ ("voorligger", [ "W1" ]) ] ]
  in
  let ev w label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  List.iter
    (fun (table_row, w) ->
      expect_empty (table_row ^ "-voorliggerrang-W1") (ev w "W1" voorliggerrang);
      expect_num (table_row ^ "-voorliggerrang-W2") (ev w "W2" voorliggerrang) "10";
      expect_num (table_row ^ "-voorliggerrang-W3") (ev w "W3" voorliggerrang) "10";
      expect_num (table_row ^ "-opvolgeraantal-W1") (ev w "W1" opvolgeraantal) "2";
      expect_num (table_row ^ "-opvolgeraantal-W2") (ev w "W2" opvolgeraantal) "0";
      expect_num (table_row ^ "-opvolgeraantal-W3") (ev w "W3" opvolgeraantal) "0")
    [ ("WV-5.7a-beide", both_sides); ("WV-5.7a-naar-veel", to_many);
      ("WV-5.7a-naar-een", to_one) ];
  (* the WF-013 flank: a reverse leg that collides with a supplied to-one
     link is a load error, never a silent overwrite *)
  check "WV-5.7a-botsende-terugpoot"
    (try
       ignore
         (supplied
            [ wachter "W1" "10" [ ("opvolgers", [ "W2" ]) ];
              wachter "W2" "20" [ ("voorligger", [ "W3" ]) ];
              wachter "W3" "30" [] ]);
       false
     with Case.Error { text = m; _ } -> Case.contains ~needle:"the reverse leg of" m)

(* --- ch. 5.8 rule 4: the zero-hop self-reference (WF-053, D10) ----------- *)

(* WV-5.8-4: the `jutterswet-aanmelderkrediet` worked example. Jutters
   fill the role aanmelder (vondstaanmelding) and the reciprocal role
   strandmaat (strandmaatschap). "het strandtegoed van de aanmelder
   plus 2" denotes the subject itself — on the wire the subject's OWN read,
   a reference without navigation bindings (D10), unconditionally (J3
   carries no links at all and still reads 9). "het strandtegoed van zijn
   strandmaat" is the one hop its binding names over the reciprocal role
   (never a self-read): J1 reads J2's 25, J3 reads leeg (rule 1, absent
   link). A binding over the to-many role vondst faults in this scalar
   slot — never a silent first pick. *)
let () =
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter"
            [ ("strandtegoed", Some num0); ("meldkrediet", Some num0);
              ("maatdeling", Some num0) ]
            [];
          ot "Vondst" [ ("gewicht", Some num0) ] [] ];
      fact_types =
        [ ( "vondstaanmelding",
            {
              ft_name = Some "vondstaanmelding";
              reciprocal = false;
              role_names =
                [
                  { role_identifier = Some "aanmelder"; role_object_type_name = Some "Jutter";
                    role_plural = None; role_many = false };
                  { role_identifier = Some "vondst"; role_object_type_name = Some "Vondst";
                    role_plural = Some "vondsten"; role_many = true };
                ];
            } );
          ( "strandmaatschap",
            {
              ft_name = Some "strandmaatschap";
              reciprocal = true;
              role_names =
                [
                  { role_identifier = Some "strandmaat"; role_object_type_name = Some "Jutter";
                    role_plural = Some "strandmaten"; role_many = false };
                ];
            } ) ];
    }
  in
  let nav_read ft role_ref : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some "strandtegoed"; object_type = Some "Jutter"; source_name = None;
        navigation_bindings = Some [ { rb_fact_type = ft; rb_role = role_ref } ] }
  in
  let plus2 : Ir_v0.Ast.expr =
    Binary
      { op = Plus; left_side = Some (aref "strandtegoed" "Jutter"); right_side = Some (litv "2" num0);
        typ = Some num0; source_name = None }
  in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance World.empty
            { (inst "j1" "Jutter" [ ("strandtegoed", n "10") ]) with
              left_side = World.SMap.add "strandmaat" [ "j2" ] World.SMap.empty })
         { (inst "j2" "Jutter" [ ("strandtegoed", n "25") ]) with
           left_side = World.SMap.add "strandmaat" [ "j1" ] World.SMap.empty })
      (inst "j3" "Jutter" [ ("strandtegoed", n "7") ])
  in
  let ev label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  expect_num "WV-5.8-4-zelf-j1" (ev "j1" plus2) "12";
  expect_num "WV-5.8-4-zelf-j2" (ev "j2" plus2) "27";
  expect_num "WV-5.8-4-zelf-j3-onvoorwaardelijk" (ev "j3" plus2) "9";
  let maat = nav_read "strandmaatschap" "strandmaat" in
  expect_num "WV-5.8-4-maat-j1" (ev "j1" maat) "25";
  expect_num "WV-5.8-4-maat-j2" (ev "j2" maat) "10";
  expect_empty "WV-5.8-4-maat-j3-leeg" (ev "j3" maat);
  expect_fault "WV-5.8-4-vreemde-rol" (fun () -> ev "j1" (nav_read "vondstaanmelding" "vondst"))

(* --- ch. 3.4a: the count source's range is written (B12) ---------------- *)

(* WV-3.4a-typenpaar: the `jutterswet-kaveltelling` worked example in its
   L2 form. The role duinkavel (kaveltoewijzing) declares no plural; the
   count names it and binds it, and j1 counts its three Duinkavels. The
   singular "vat" names the role vat of vatberging (declared plural
   "vaten"): j1 counts its two Vaten. A Jutter with no links counts 0 and
   0. With a second fact type connecting Jutter to Duinkavel the count
   still reads the relation its binding names — nothing is left to guess
   between two relations — and a count without a range is refused. *)
let () =
  let base_ft : (string * Ir_v0.Ast.fact_type_name) list =
    [ ( "kaveltoewijzing",
        {
          ft_name = Some "kaveltoewijzing";
          reciprocal = false;
          role_names =
            [
              { role_identifier = Some "pachter"; role_object_type_name = Some "Jutter";
                role_plural = None; role_many = false };
              { role_identifier = Some "duinkavel"; role_object_type_name = Some "Duinkavel";
                role_plural = None; role_many = true };
            ];
        } );
      ( "vatberging",
        {
          ft_name = Some "vatberging";
          reciprocal = false;
          role_names =
            [
              { role_identifier = Some "berger"; role_object_type_name = Some "Jutter";
                role_plural = None; role_many = false };
              { role_identifier = Some "vat"; role_object_type_name = Some "Vat";
                role_plural = Some "vaten"; role_many = true };
            ];
        } ) ]
  in
  let doc =
    {
      empty_doc with
      object_types =
        [ ot "Jutter" [ ("aantal duinkavels", Some num0); ("aantal geborgen vaten", Some num0) ] [];
          ot "Duinkavel" [] []; ot "Vat" [] [] ];
      fact_types = base_ft;
    }
  in
  let count_source name : Ir_v0.Ast.expr =
    Attribute_reference { attribute_ref = Some name; object_type = Some "Jutter"; source_name = None; navigation_bindings = None }
  in
  let quantity ?source_role name el : Ir_v0.Ast.expr =
    Function_call
      { kind = Count; call_arguments = [ count_source name ];
        element_object_type = Some el; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role; source_path = None }
  in
  let kavel = { Ir_v0.Ast.rb_fact_type = "kaveltoewijzing"; rb_role = "duinkavel" } in
  let vat = { Ir_v0.Ast.rb_fact_type = "vatberging"; rb_role = "vat" } in
  let w =
    List.fold_left World.add_instance World.empty
      [ { (inst "j1" "Jutter" []) with
          left_side =
            World.SMap.add "duinkavel" [ "k1"; "k2"; "k3" ]
              (World.SMap.add "vaten" [ "t1"; "t2" ] World.SMap.empty) };
        inst "j2" "Jutter" [];
        inst "k1" "Duinkavel" []; inst "k2" "Duinkavel" []; inst "k3" "Duinkavel" [];
        inst "t1" "Vat" []; inst "t2" "Vat" [] ]
  in
  let ev doc label e = Eval.eval_expr (env_of doc) w (World.get w label) ~err:no_err e in
  expect_num "WV-3.4a-typenpaar-kavels"
    (ev doc "j1" (quantity ~source_role:kavel "duinkavel" "Duinkavel")) "3";
  expect_num "WV-3.4a-typenpaar-vat-enkelvoud" (ev doc "j1" (quantity ~source_role:vat "vat" "Vat")) "2";
  expect_num "WV-3.4a-typenpaar-kavels-nul"
    (ev doc "j2" (quantity ~source_role:kavel "duinkavel" "Duinkavel")) "0";
  expect_num "WV-3.4a-typenpaar-vat-nul" (ev doc "j2" (quantity ~source_role:vat "vat" "Vat")) "0";
  let doc2 =
    { doc with
      fact_types =
        base_ft
        @ [ ( "kavelpacht",
              {
                ft_name = Some "kavelpacht";
                reciprocal = false;
                role_names =
                  [
                    { role_identifier = Some "pachtheer"; role_object_type_name = Some "Jutter";
                      role_plural = None; role_many = false };
                    { role_identifier = Some "pachtkavel"; role_object_type_name = Some "Duinkavel";
                      role_plural = None; role_many = true };
                  ];
              } ) ] }
  in
  expect_num "WV-3.4a-typenpaar-tweede-feittype-bindt"
    (ev doc2 "j1" (quantity ~source_role:kavel "duinkavel" "Duinkavel")) "3";
  expect_fault "WV-3.4a-typenpaar-zonder-bereik" (fun () ->
      ev doc2 "j1" (quantity "duinkavel" "Duinkavel"))

(* --- ch. 5.8a: the near-side rolcheck ---------------------------------- *)

(* WV-5.8a: the `jutterswet-wachtindeling` worked example. Feittype
   wachtindeling relates the to-many role wachtjutter (Jutter) to the
   to-one role toegewezen wachtpost (Wachtpost). The role check holds iff
   a link of wachtindeling exists in which the subject fills wachtjutter
   (D11). J1 is assigned post W1, J2 to none: the positive check holds for
   J1, not J2; the "is geen" negation flips it (unlike the navigated
   characteristic check of ch. 5.8 rule 3, which is asymmetric). With the
   far role to-many the check is still an existence question: the number
   of links is no part of it, so it answers and never refuses. *)
let () =
  let ft_guard right_many : string * Ir_v0.Ast.fact_type_name =
    ( "wachtindeling",
      {
        ft_name = Some "wachtindeling";
        reciprocal = false;
        role_names =
          [
            { role_identifier = Some "wachtjutter"; role_object_type_name = Some "Jutter";
              role_plural = Some "wachtjutters"; role_many = true };
            { role_identifier = Some "toegewezen wachtpost"; role_object_type_name = Some "Wachtpost";
              role_plural = (if right_many then Some "wachtposten" else None);
              role_many = right_many };
          ];
      } )
  in
  let doc =
    { empty_doc with
      object_types = [ ot "Jutter" [] [ "wachtdoend"; "vrijgesteld" ]; ot "Wachtpost" [] [] ];
      fact_types = [ ft_guard false ] }
  in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance World.empty (inst "w1" "Wachtpost" []))
         { (inst "j1" "Jutter" []) with
           left_side = World.SMap.add "toegewezen wachtpost" [ "w1" ] World.SMap.empty })
      (inst "j2" "Jutter" [])
  in
  let role_check negated : Ir_v0.Ast.condition_expr =
    Role_membership
      { fact_type = "wachtindeling"; role = "wachtjutter"; counterpart_type = None; negated;
        source_name = None }
  in
  let ec label c = Eval.eval_cond env w (World.get w label) ~err:no_err c in
  check "WV-5.8a-vervult-j1" (ec "j1" (role_check false));
  check "WV-5.8a-vervult-j2-niet" (not (ec "j2" (role_check false)));
  check "WV-5.8a-geen-j1-niet" (not (ec "j1" (role_check true)));
  check "WV-5.8a-geen-j2" (ec "j2" (role_check true));
  let doc_mm = { doc with fact_types = [ ft_guard true ] } in
  check "WV-5.8a-naar-veel-bestaat"
    (Eval.eval_cond (env_of doc_mm) w (World.get w "j1") ~err:no_err (role_check false));
  check "WV-5.8a-naar-veel-j2-niet"
    (not (Eval.eval_cond (env_of doc_mm) w (World.get w "j2") ~err:no_err (role_check false)));

  (* WV-5.8a-ver: the `jutterswet-postbezetting` worked example — the same
     sentence read from the OTHER end. The Wachtpost fills `toegewezen
     wachtpost`, so "heeft een wachtjutter" is the role check on THAT role:
     a link of wachtindeling in which the post fills it (D11). The worlds
     below carry BOTH legs of every relation, which is what a loaded case
     carries (ch. 5.7a: the loader materialises the leg the case did not
     spell) and what every runtime link write produces (both legs, always).

     Case 1 pins the two corners and their negations. Case 2 separates
     existence from counting (two fillers on one post read the same as one)
     and from sheer presence (a Jutter in no relation leaves every post
     alone). Case 3 has two posts with one filler each, differing only in
     CLASS. The strand checks carry the counterpart (U12): the filler of the
     other role must be a Strandjutter, so a plain Jutter's link does not
     count for them while it counts for `bemand`, and a Strandjutter's link
     counts for both (the subtype satisfies the supertype filler). A check
     naming the role of the OTHER side faults: the subject does not fill it;
     so does a counterpart outside the other role's declared filler. *)
  let doc_sj =
    { doc with
      object_types =
        doc.object_types
        @ [ (let n, o = ot "Strandjutter" [] [] in
             (n, { o with ot_supertype = Some "Jutter" })) ] }
  in
  let env = env_of doc_sj in
  let post label wachters =
    { (inst label "Wachtpost" []) with
      left_side = World.SMap.add "wachtjutters" wachters World.SMap.empty }
  in
  let jutter_op label otype post =
    { (inst label otype []) with
      left_side = World.SMap.add "toegewezen wachtpost" [ post ] World.SMap.empty }
  in
  let world_state insts = List.fold_left World.add_instance World.empty insts in
  let distant ?counterpart_type role negated : Ir_v0.Ast.condition_expr =
    Role_membership { fact_type = "wachtindeling"; role; counterpart_type; negated; source_name = None }
  in
  let bemand = distant "toegewezen wachtpost" in
  let strandbemand = distant ~counterpart_type:"Strandjutter" "toegewezen wachtpost" in
  let w1 =
    world_state [ post "w1" [ "j1" ]; inst "w2" "Wachtpost" []; jutter_op "j1" "Jutter" "w1" ]
  in
  let ev1 label c = Eval.eval_cond env w1 (World.get w1 label) ~err:no_err c in
  check "WV-5.8a-ver-w1-bemand" (ev1 "w1" (bemand false));
  check "WV-5.8a-ver-w1-niet-onbemand" (not (ev1 "w1" (bemand true)));
  check "WV-5.8a-ver-w2-niet-bemand" (not (ev1 "w2" (bemand false)));
  check "WV-5.8a-ver-w2-onbemand" (ev1 "w2" (bemand true));
  check "WV-5.8a-ver-w1-niet-strandbemand" (not (ev1 "w1" (strandbemand false)));
  check "WV-5.8a-ver-w1-strandvrij" (ev1 "w1" (strandbemand true));
  let w2 =
    world_state
      [ post "w1" [ "j1"; "j2" ]; inst "w2" "Wachtpost" []; jutter_op "j1" "Jutter" "w1";
        jutter_op "j2" "Strandjutter" "w1"; inst "j3" "Jutter" [] ]
  in
  let ev2 label c = Eval.eval_cond env w2 (World.get w2 label) ~err:no_err c in
  check "WV-5.8a-ver-twee-vullers-telt-niet" (ev2 "w1" (bemand false));
  check "WV-5.8a-ver-twee-vullers-niet-onbemand" (not (ev2 "w1" (bemand true)));
  check "WV-5.8a-ver-subtype-vult-supertype" (ev2 "w1" (strandbemand false));
  check "WV-5.8a-ver-losse-jutter-bemant-niets" (ev2 "w2" (bemand true));
  let w3 =
    world_state
      [ post "w1" [ "j1" ]; post "w2" [ "s1" ]; jutter_op "j1" "Jutter" "w1";
        jutter_op "s1" "Strandjutter" "w2" ]
  in
  let ev3 label c = Eval.eval_cond env w3 (World.get w3 label) ~err:no_err c in
  check "WV-5.8a-ver-klasse-splitst-w1-bemand" (ev3 "w1" (bemand false));
  check "WV-5.8a-ver-klasse-splitst-w1-strandvrij" (ev3 "w1" (strandbemand true));
  check "WV-5.8a-ver-klasse-splitst-w2-bemand" (ev3 "w2" (bemand false));
  check "WV-5.8a-ver-klasse-splitst-w2-strandbemand" (ev3 "w2" (strandbemand false));
  expect_fault "WV-5.8a-ver-verkeerde-zijde" (fun () -> ev1 "w1" (distant "wachtjutter" false));
  expect_fault "WV-5.8a-ver-vreemde-tegenpartij" (fun () ->
      ev1 "w1" (distant ~counterpart_type:"Wachtpost" "toegewezen wachtpost" false))

(* --- ch. 5.8b: the characteristic check's owner (WF-100) ----------------
   The RUNTIME-SEMANTICS ch. 5.8b worked example, verbatim. `Vondst` and
   `Jutter` both declare a characteristic named `gekeurd`; V1 carries it and
   J1 does not, so the two readings are distinguishable by value and not only
   by name. Three owners over the same subject:

     host absent    -> reads V1's own `gekeurd`  = true
     host "Vondst"  -> the same, the field naming the subject's own type
     host "Jutter"  -> FAULT, never J1's value and never V1's

   The third line is the decision (2026-09-10): the field is authoritative, so
   an owner that is not the subject's type is not a decoration to be read past.
   Measured before the rule, on this document: the check answered `true` — V1's
   value under J1's name. Since WF-109 (2026-09-19) such a document is refused
   by the VERIFIER, so the fault below is this reader's fail-fast on a
   non-conforming input and not the answer to a conforming one; the corpus
   cannot hold the witness for exactly that reason.

   Six owners in the block below, not three: each of the three is also read from
   the OTHER subject, and the last pair is the NAVIGATED form. The navigated
   check is where the two halves of the field could be mistaken for each other:
   WF-100's declaration cross does NOT reach it (the characteristic lives on the
   type the hop reaches, which WF-053 crosses there), while the HOSTING refusal
   does — the owner is a statement about the pattern the constraint hangs on,
   and that is the subject's on a navigated check exactly as on a scalar one.
   The refusal therefore fires BEFORE the hop is resolved, which is why the
   message and not merely the fault is asserted here: a bare `expect_fault`
   would also pass on an unresolvable role. Measured on the postwacht shape with
   the reached type in the field (`risicopost` via `post`, owner `Post`, subject
   `Wachter`): verifier PASS before AND after WF-100 — the declaration cross is
   correctly out of scope — `ref` and the ASP backend PASS (10) before and
   faulting after, and both Drools branches refusing all along (a transpiler
   SKIP on one branch, a `TODO kenmerk 'risicopost' hoort bij Post` marker on
   the other). That form had NO answering executor at all, deliberately, and
   WF-109 has since taken it off the wire (a navigating check's host must be a
   type the subject satisfies, `wf109-target-host-navigated`). This block
   stays the machine-checked witness that the reader does not soften the fence
   into a fallback. *)

let () =
  let doc =
    { empty_doc with
      object_types = [ ot "Vondst" [] [ "gekeurd" ]; ot "Jutter" [] [ "gekeurd" ] ] }
  in
  let w =
    List.fold_left World.add_instance World.empty
      [ { (inst "v1" "Vondst" []) with chars = [ "gekeurd" ] }; inst "j1" "Jutter" [] ]
  in
  let kc holder : Ir_v0.Ast.condition_expr =
    Characteristic_check
      { characteristic = Some "gekeurd"; negation = false; holder; time_period = None;
        source_name = None; navigation_binding = None }
  in
  let ec label c = Eval.eval_cond (env_of doc) w (World.get w label) ~err:no_err c in
  check "WV-5.8b-eigenaar-afwezig" (ec "v1" (kc None));
  check "WV-5.8b-eigenaar-is-onderwerptype" (ec "v1" (kc (Some "Vondst")));
  check "WV-5.8b-eigenaar-kapitalisatie" (ec "v1" (kc (Some "vondst")));
  check "WV-5.8b-eigenaar-afwezig-j1-onwaar" (not (ec "j1" (kc None)));
  expect_fault "WV-5.8b-vreemde-eigenaar-weigert" (fun () -> ec "v1" (kc (Some "Jutter")));
  expect_fault "WV-5.8b-vreemde-eigenaar-weigert-ook-omgekeerd" (fun () ->
      ec "j1" (kc (Some "Vondst")));
  (* the navigated form: the hosting refusal reaches it too, and precedes the
     hop — so the SENTENCE is asserted, not merely the fault *)
  let kc_nav holder : Ir_v0.Ast.condition_expr =
    Characteristic_check
      { characteristic = Some "gekeurd"; negation = false; holder; time_period = None; source_name = None;
        navigation_binding = Some { rb_fact_type = "vondstvinding"; rb_role = "vinder" } }
  in
  match ec "v1" (kc_nav (Some "Jutter")) with
  | exception Value.Fault { text = m; _ } ->
      check ("WV-5.8b-genavigeerde-vreemde-eigenaar-weigert (got: " ^ m ^ ")")
        (Case.contains ~needle:"is hosted on \"Jutter\" while the subject is \"Vondst\"" m)
  | _ -> check "WV-5.8b-genavigeerde-vreemde-eigenaar-weigert (no fault at all)" false

(* --- ch. 5.8b worked example B: the two-object position (WF-109) --------
   The RUNTIME-SEMANTICS ch. 5.8b second worked example, verbatim, and the
   ch. 5.5 clause it belongs to. A fact creation over `Kustvak` -> `Post` ->
   `Sein` whose guard is the bare characteristic `gehesen`; all three types
   declare that characteristic, so the reading is identified by its VALUE and
   not merely by its name:

     host "Sein"    -> the DOEL reading, the guard filters the targets  = 1
     host "Kustvak" -> the ANKER reading, the guard gates the anker     = 2
     host "Post"    -> the intermediate step, which no pattern hosts    = refused

   V1 (a Kustvak that IS gehesen) reaches P1 (a Post that is NOT) carrying S1
   (gehesen) and S2 (not), so the three readings give 1, 2 and 0-by-refusal.
   The goldens `jutterswet-seinhijsing` and `jutterswet-mastsein` replay the
   same law through all four executors; this block is the interpreter's own
   witness, and it is where the ANKER reading is pinned on the SAME document —
   a golden can only carry one host per creation.

   The third row is WF-109's zero flank: `Post` is an object the chain walks
   through, not one the guard is about, and the verifier refuses that document
   before any executor sees it. The fault here is this reader's fail-fast on a
   non-conforming input, and it is asserted by SENTENCE because a bare fault
   would also pass on an unresolvable role. *)

let () =
  let role_ref ident otn mv plural_flag : Ir_v0.Ast.role_ref =
    { role_identifier = Some ident; role_object_type_name = Some otn; role_plural = Some mv;
      role_many = plural_flag }
  in
  let ft ident role_names = (ident, { Ir_v0.Ast.ft_name = Some ident; reciprocal = false; role_names }) in
  let rb f r : Ir_v0.Ast.role_binding = { rb_fact_type = f; rb_role = r } in
  let vaksein_role = role_ref "vaksein" "Sein" "vakseinen" true in
  let doc_of holder =
    {
      empty_doc with
      object_types =
        [ ot "Kustvak" [] [ "gehesen" ]; ot "Post" [] [ "gehesen" ]; ot "Sein" [] [ "gehesen" ] ];
      fact_types =
        [ ft "kustligging"
            [ role_ref "kustvak" "Kustvak" "kustvakken" false; role_ref "post" "Post" "posten" true ];
          ft "seinvoering"
            [ role_ref "post" "Post" "posten" false; role_ref "sein" "Sein" "seinen" true ];
          ft "vakbeseining"
            [ role_ref "seinvak" "Kustvak" "seinvakken" false; vaksein_role ] ];
      fact_creations =
        [ { fc_name = Some "vakbeseining door het kustvak";
            anchor_point = Some "Kustvak"; fc_target = Some "Sein";
            intermediates = [ { ts_role = rb "kustligging" "post" } ];
            fc_condition =
              Some
                (Characteristic_check
                   { characteristic = Some "gehesen"; negation = false; holder;
                     time_period = None; source_name = None; navigation_binding = None });
            fc_validity = None; fc_fact_type = "vakbeseining";
            anchor_role = "seinvak"; objective_role = "vaksein";
            join_role = rb "seinvoering" "sein"; fc_pointer = "" } ];
    }
  in
  let left_side l = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty l in
  let w =
    List.fold_left World.add_instance World.empty
      [ { (inst "v1" "Kustvak" []) with chars = [ "gehesen" ];
          left_side = left_side [ ("posten", [ "p1" ]) ] };
        { (inst "p1" "Post" []) with
          left_side = left_side [ ("kustvak", [ "v1" ]); ("seinen", [ "s1"; "s2" ]) ] };
        { (inst "s1" "Sein" []) with chars = [ "gehesen" ] };
        inst "s2" "Sein" [] ]
  in
  let vakseinen holder =
    let final = Eval.evaluate (env_of (doc_of holder)) w in
    List.length (Eval.role_linked_instances final (World.get final "v1") vaksein_role)
  in
  check "WV-5.8b-B-doelgastheer-filtert-de-doelen" (vakseinen (Some "Sein") = 1);
  check "WV-5.8b-B-ankergastheer-poort-de-anker" (vakseinen (Some "Kustvak") = 2);
  check "WV-5.8b-B-gastheerloze-guard-poort-de-anker" (vakseinen None = 2);
  (match vakseinen (Some "Post") with
  | exception Value.Fault { text = m; _ } ->
      check ("WV-5.8b-B-tussenstap-gastheer-weigert (got: " ^ m ^ ")")
        (Case.contains ~needle:"is hosted on \"Post\" while the subject is \"Kustvak\"" m)
  | _ -> check "WV-5.8b-B-tussenstap-gastheer-weigert (no fault at all)" false);
  (* --- the EXTENSIONAL half of the host (ch. 5.8b rule 2, WF-109) ---
     `T ⊑ O`, not `T = O`. A host naming a SUPERTYPE of the doel is satisfied
     through inheritance and reads the doel; the golden `jutterswet-bakensein`
     replays the same law through all four executors, and this is the
     interpreter's own row. Measured 2026-09-19: both Drools branches refused
     this form until the same day, asking the question by name identity. *)
  let super_doc host =
    let d = doc_of host in
    let baken = snd (ot "Baken" [] [ "gehesen" ]) in
    let sein = snd (ot "Sein" [] []) in
    { d with
      object_types =
        ("Baken", baken)
        :: List.map
             (fun (n, o) -> if n = "Sein" then (n, { sein with ot_supertype = Some "Baken" }) else (n, o))
             d.object_types }
  in
  let super_vakseinen host =
    let final = Eval.evaluate (env_of (super_doc host)) w in
    List.length (Eval.role_linked_instances final (World.get final "v1") vaksein_role)
  in
  check "WV-5.8b-B-supertype-gastheer-leest-het-doel" (super_vakseinen (Some "Baken") = 1);
  (* --- the two named roles resolve as WF-028 resolves them ---
     A creation whose anchorRole or targetRole names no role of the created
     fact type, names one of two roles that share it, or whose fact type is
     undeclared or has not exactly two roles, is refused in WF-028's own
     sentence, never read as the role the name resembles. The verifier
     refuses such a document first; this is what a node built past it meets. *)
  let with_creation f =
    let d = doc_of None in
    { d with fact_creations = List.map f d.fact_creations }
  in
  let with_roles role_names =
    let d = doc_of None in
    { d with
      fact_types =
        List.map
          (fun (n, (x : Ir_v0.Ast.fact_type_name)) -> if n = "vakbeseining" then (n, { x with role_names }) else (n, x))
          d.fact_types }
  in
  let rejects ident doc sentence_text = expect_fault_msg ident (fun () -> Eval.evaluate (env_of doc) w) sentence_text in
  rejects "WF-028-onbekende-doelrol-weigert"
    (with_creation (fun fc -> { fc with objective_role = "onbekend" }))
    "'onbekend' is not a role of 'vakbeseining'";
  rejects "WF-028-onbekende-ankerrol-weigert"
    (with_creation (fun fc -> { fc with anchor_role = "onbekend" }))
    "'onbekend' is not a role of 'vakbeseining'";
  rejects "WF-028-onbekend-feittype-weigert"
    (with_creation (fun fc -> { fc with fc_fact_type = "onbekendfeit" }))
    "created fact type 'onbekendfeit' is not declared";
  rejects "WF-028-gedeelde-rolnaam-weigert"
    (with_roles [ role_ref "seinvak" "Kustvak" "seinvakken" false; role_ref "seinvak" "Sein" "vakseinen" true ])
    "role 'seinvak' is ambiguous within 'vakbeseining'";
  rejects "WF-028-feittype-zonder-twee-rollen-weigert" (with_roles [ vaksein_role ])
    "created fact type 'vakbeseining' does not have exactly two roles";
  (* --- the AMBIGUOUS host faults, and says which two objects it reached ---
     Populations are extensional: with `Sein ⊑ Kustvak` a concrete Sein fills an
     anker declared Kustvak, so a `Sein` host designates the anker AND the doel.
     ch. 5.8b requires a fault there; until 2026-09-19 this reader silently
     answered from the anker instead, which is the reading the decision rejects.
     WF-109 makes the document unreachable through the verifier, so this is
     fail-fast on a non-conforming input — and a corpus zone cannot hold it: a
     golden must replay and a red case must be verifier-clean. *)
  let overlap_doc =
    let d = doc_of (Some "Sein") in
    let sein = snd (ot "Sein" [] []) in
    { d with
      object_types =
        List.map
          (fun (n, o) -> if n = "Sein" then (n, { sein with ot_supertype = Some "Kustvak" }) else (n, o))
          d.object_types }
  in
  match Eval.evaluate (env_of overlap_doc) w with
  | exception Value.Fault { text = m; _ } ->
      check ("WV-5.8b-B-ambigue-gastheer-weigert (got: " ^ m ^ ")")
        (Case.contains ~needle:"is hosted on 'Sein', which the target 'Sein' is" m
        && Case.contains ~needle:"but so can the anchor 'Kustvak' be" m)
  | _ -> check "WV-5.8b-B-ambigue-gastheer-weigert (no fault at all)" false

(* --- ch. 3.9: the whole-period qualifier over a characteristic ----------
   The RUNTIME-SEMANTICS ch. 3.9 worked examples, verbatim: the four rows of
   the table (a failed to-one hop, a linked target with no periods, a full
   year, a half year), then the qualifier's own negation, the inner
   negation, and the month grid beside the year grid.

   These are the four cells the card measured on 2026-09-11, before any
   change, in three executors: `ref` answered a SILENT false on row a (the
   unqualified navigated check's absent-link asymmetry, applied under a
   qualifier that lifts it) and a LOUD ch. 3.3 refusal on b/c/d ("temporal
   characteristic in a scalar condition"), while ch. 3.3 names ch. 3.9 as
   one of its own two exits; the ASP backend dropped the qualifier and read
   a has/2 fact a temporal characteristic can never have, answering leeg
   on a/b/c without a word. The Drools backend answered all four rows
   correctly all along, so the corpus golden `synth-gehelejaarkenmerk`
   closes the square on exactly these values.

   The fixture below carries NO `characteristic_timeline_count` entry, which is what
   a frontend-produced document looks like: the emitter writes that key only
   for a count OTHER than 1. Until 2026-09-11 that absence was load-bearing —
   a hand-built document that did carry it at 1 flipped both Drools branches
   into a refusal (their gate tested the key's PRESENCE, not "two or more")
   and made the form look unsupported when it is not. That is precisely the
   mismeasurement the ch. 3.9 card first reported and then withdrew; the gate
   itself was corrected in both branches by
   `backlog/done/20260911-kenmerktijdlijnaantal-poort.md`.

   Row a is the one that cannot be reached through the timeline fold alone —
   it is about the NAVIGATION, not about any timeline — which is why these
   run through [Eval.eval_cond] over a document and a world rather than over
   [Timeline.entire_period_characteristic] directly. *)

let () =
  let ligplaats : string * Ir_v0.Ast.fact_type_name =
    ( "ligplaats",
      {
        ft_name = Some "ligplaats";
        reciprocal = false;
        role_names =
          [
            { role_identifier = Some "werf"; role_object_type_name = Some "Werf"; role_plural = None;
              role_many = false };
            { role_identifier = Some "schuit"; role_object_type_name = Some "Schuit";
              role_plural = Some "schuiten"; role_many = true };
          ];
      } )
  in
  let werf = ot "Werf" [] [ "bedrijvig" ] in
  let werf =
    ( fst werf, { (snd werf) with characteristic_granularity = [ ("bedrijvig", Some Month) ] } )
  in
  let doc =
    { empty_doc with object_types = [ werf; ot "Schuit" [] [] ]; fact_types = [ ligplaats ] }
  in
  let day_number iso = match Value.date_of_iso iso with Value.Date d -> d | _ -> assert false in
  let time_period van up_to = (day_number van, day_number up_to) in
  (* one world per row: the Werf's true-periods, and whether S1 links to it *)
  let world_state ~link period_list =
    let w1 =
      { (inst "W1" "Werf" []) with
        ctls =
          (match period_list with
          | [] -> World.SMap.empty
          | ps -> World.SMap.add "bedrijvig" ps World.SMap.empty) }
    in
    let s1 =
      { (inst "S1" "Schuit" []) with
        left_side = (if link then World.SMap.add "werf" [ "W1" ] World.SMap.empty else World.SMap.empty) }
    in
    List.fold_left World.add_instance World.empty [ w1; s1 ]
  in
  let cond ?(year = true) ?(not_flag = false) ?(negation = false) () : Ir_v0.Ast.condition_expr =
    Characteristic_check
      { characteristic = Some "bedrijvig"; negation; holder = None;
        time_period = Some { gp_granularity = year; gp_negated = not_flag }; source_name = None;
        navigation_binding = Some { rb_fact_type = "ligplaats"; rb_role = "werf" } }
  in
  let ec ~link period_list c =
    let w = world_state ~link period_list in
    Eval.eval_cond (env_of doc) w (World.get w "S1") ~err:no_err c
  in
  let whole_2026 = [ time_period "2026-01-01" "2027-01-01" ] in
  let jan_jul = [ time_period "2026-01-01" "2026-07-01" ] in
  let half_jan = [ time_period "2026-01-15" "2026-03-01" ] in
  (* the table: a null timeline and a segmentless one both touch no bucket *)
  check "WV-3.9-kenmerk-a-geen-koppeling" (ec ~link:false [] (cond ()));
  check "WV-3.9-kenmerk-b-geen-tijdlijn" (ec ~link:true [] (cond ()));
  check "WV-3.9-kenmerk-c-heel-jaar" (ec ~link:true whole_2026 (cond ()));
  check "WV-3.9-kenmerk-d-half-jaar" (not (ec ~link:true jan_jul (cond ())));
  (* the QUALIFIER's negation complements each verdict — the clause's
     "the qualifier's negation then fails" on the two vacuous rows *)
  check "WV-3.9-kenmerk-niet-a" (not (ec ~link:false [] (cond ~not_flag:true ())));
  check "WV-3.9-kenmerk-niet-b" (not (ec ~link:true [] (cond ~not_flag:true ())));
  check "WV-3.9-kenmerk-niet-c" (not (ec ~link:true whole_2026 (cond ~not_flag:true ())));
  check "WV-3.9-kenmerk-niet-d" (ec ~link:true jan_jul (cond ~not_flag:true ()));
  (* the INNER negation: same touched buckets, complementary satisfied days *)
  check "WV-3.9-kenmerk-neg-a" (ec ~link:false [] (cond ~negation:true ()));
  check "WV-3.9-kenmerk-neg-b" (ec ~link:true [] (cond ~negation:true ()));
  check "WV-3.9-kenmerk-neg-c" (not (ec ~link:true whole_2026 (cond ~negation:true ())));
  check "WV-3.9-kenmerk-neg-d" (not (ec ~link:true jan_jul (cond ~negation:true ())));
  (* the MONTH grid: the qualifier's own, never the characteristic's
     declared granularity — six full months pass where the year bucket failed *)
  check "WV-3.9-kenmerk-maand-d" (ec ~link:true jan_jul (cond ~year:false ()));
  check "WV-3.9-kenmerk-maand-deelmaand"
    (not (ec ~link:true half_jan (cond ~year:false ())));
  check "WV-3.9-kenmerk-jaar-deelmaand" (not (ec ~link:true half_jan (cond ())));
  (* the OWN form — the same qualifier without a hop, read on the Werf
     itself. Every row above navigates, and so does the corpus golden, so
     without this row the "own and navigated" of the ch. 3.12 delta line and
     the CONSTRUCTS register would be a word with no artefact. Both
     directions in one check: the full year passes, the half year does not,
     which is what separates the own arm from a constant. *)
  let own_cond : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some "bedrijvig"; negation = false; holder = None; time_period = Some { gp_granularity = true; gp_negated = false }; source_name = None; navigation_binding = None }
  in
  let ec_own period_list =
    let w = world_state ~link:false period_list in
    Eval.eval_cond (env_of doc) w (World.get w "W1") ~err:no_err own_cond
  in
  check "WV-3.9-kenmerk-eigen-vorm"
    (ec_own whole_2026 && (not (ec_own jan_jul)) && ec_own []);
  (* the fence WF-043 does not reach on the navigated form: a non-temporal
     characteristic would fold over an always-empty period set and pass *)
  let doc_non_temporal =
    { doc with object_types = [ ot "Werf" [] [ "bedrijvig" ]; ot "Schuit" [] [] ] }
  in
  (match
     let w = world_state ~link:true whole_2026 in
     Eval.eval_cond (env_of doc_non_temporal) w (World.get w "S1") ~err:no_err (cond ())
   with
  | exception Value.Fault { text = m; _ } ->
      check ("WV-3.9-kenmerk-niet-tijdsafhankelijk (got: " ^ m ^ ")")
        (Case.contains ~needle:"is not a time-dependent characteristic of \"Werf\"" m)
  | _ -> check "WV-3.9-kenmerk-niet-tijdsafhankelijk (no fault at all)" false);
  (* the fold itself, with the chapter's own day counts *)
  check "WV-3.9-fold-leeg" (Timeline.entire_period_characteristic Year ~negation:false []);
  check "WV-3.9-fold-vol" (Timeline.entire_period_characteristic Year ~negation:false whole_2026);
  check "WV-3.9-fold-deel" (not (Timeline.entire_period_characteristic Year ~negation:false jan_jul))

(* --- WF-095: the datum-tijd lexeme (IR-SPEC ch. 4.1) --------------------
   The corpus golden pins what the READER answers; these pin the reader's
   own fence, which no corpus document can reach — a case file that carries
   an ill-formed lexeme cannot exist inside gate 4, and an ill-formed
   LITERAL cannot exist inside gate 3. The witness for the fence therefore
   lives here. *)

let dt_ms name lex because =
  match Value.datetime_of_lexeme lex with
  | Value.Date_time got -> check (name ^ " (got " ^ string_of_int got ^ ")") (got = because)
  | other -> check (name ^ " (got " ^ Value.to_string other ^ ")") false

let () =
  (* the hand-computed anchor: 2026-03-15 is civil day 20 527, noon is
     43 200 000 ms into it (jutterswet-meldstip's NOTE.md carries the
     derivation) *)
  let noon = (20527 * 86_400_000) + 43_200_000 in
  check "WF-095-anchor-is-1773576000000" (noon = 1_773_576_000_000);
  (* ONE instant, ONE spelling: the canonical ISO lexeme, with or without
     its fraction *)
  dt_ms "WF-095-canoniek-met-fractie" "2026-03-15T12:00:00.000" noon;
  dt_ms "WF-095-canoniek-zonder-fractie" "2026-03-15T12:00:00" noon;
  (* The day-first source snippet is REFUSED, by name. Until 2026-09-23
     these four were dt_ms pins that the reader answered noon (and
     2026-03-05 noon for the narrow fields): the snippet was a second
     admitted spelling while the RegelSpraak emitter still wrote it into
     `waarde`. The emitter writes ISO since card p5-datumtijd-lexeem and
     WF-095 admits ISO alone, so a snippet in `waarde` has no producer and no
     verifier-clean document; reading it anyway would answer a document
     nobody may write (card datumtijd-lezers-versmalling). The refusal
     quotes the lexeme and names the one form it expected. *)
  let refuses_snippet name lex =
    expect_fault_msg name
      (fun () -> Value.datetime_of_lexeme lex)
      (Printf.sprintf "malformed datum-tijd lexeme %S: not the canonical ISO form" lex)
  in
  refuses_snippet "WF-095-weigert-snippet" "15-03-2026 12:00:00.000";
  refuses_snippet "WF-095-weigert-snippet-zonder-fractie" "15-03-2026 12:00:00";
  refuses_snippet "WF-095-weigert-snippet-dedato-prefix" "dd. 15-03-2026 12:00:00.000";
  refuses_snippet "WF-095-weigert-snippet-smalle-velden" "5-3-2026 12:00:00";
  (* the de-dato prefix is not a spelling of its own either: in front of the
     ISO lexeme it is refused like the snippet it came from *)
  refuses_snippet "WF-095-weigert-dedato-prefix-voor-iso" "dd. 2026-03-15T12:00:00";
  (* the fraction is WHOLE MILLISECONDS, never a positional fraction *)
  dt_ms "WF-095-fractie-is-milliseconden" "2026-03-15T12:00:00.500" (noon + 500);
  dt_ms "WF-095-fractie-001" "2026-03-15T12:00:00.001" (noon + 1);
  (* and everything else outside the one spelling is refused, per field class *)
  let refuses name lex = expect_fault name (fun () -> Value.datetime_of_lexeme lex) in
  refuses "WF-095-weigert-zone" "2026-03-15T12:00:00Z";
  refuses "WF-095-weigert-offset" "2026-03-15T12:00:00+01:00";
  refuses "WF-095-weigert-spatie-scheider" "2026-03-15 12:00:00";
  refuses "WF-095-weigert-eencijferige-maand" "2026-3-15T12:00:00";
  refuses "WF-095-weigert-eencijferig-uur" "2026-03-15T9:00:00";
  refuses "WF-095-weigert-fractie-1" "2026-03-15T12:00:00.1";
  refuses "WF-095-weigert-fractie-4" "2026-03-15T12:00:00.1234";
  refuses "WF-095-weigert-fractie-6" "2026-03-15T12:00:00.100000";
  refuses "WF-095-weigert-ontbrekende-seconden" "2026-03-15T12:00";
  refuses "WF-095-weigert-omringende-tekst" "rommel 2026-03-15T12:00:00 x";
  refuses "WF-095-weigert-lege-tekst" "";
  refuses "WF-095-weigert-naijlende-spatie" "2026-03-15T12:00:00 ";
  (* the calendar is checked, not only the shape *)
  refuses "WF-095-weigert-29-februari-buiten-schrikkeljaar" "2026-02-29T12:00:00";
  dt_ms "WF-095-laat-29-februari-in-schrikkeljaar-toe" "2024-02-29T00:00:00"
    (19782 * 86_400_000);
  refuses "WF-095-weigert-uur-24" "2026-03-15T24:00:00";
  refuses "WF-095-weigert-minuut-60" "2026-03-15T12:60:00";
  (* the CASE wire and the literal wire carry the same single spelling since
     2026-09-23; the case loader calls the ISO reader directly (its schema
     pattern is exactly that form), the literal reader goes through
     [datetime_of_lexeme] — both refuse the snippet *)
  check "WF-095-casewire-neemt-canoniek"
    (Value.read_iso_datetime "2026-03-15T12:00:00.000" = Ok noon);
  check "WF-095-casewire-weigert-snippet"
    (match Value.read_iso_datetime "15-03-2026 12:00:00.000" with
     | Error _ -> true
     | Ok _ -> false);
  (* a refusal names the POSITION at which the grammar diverges, so a
     producer can act on it; a bare "malformed" could not *)
  check "WF-095-weigering-noemt-de-positie"
    (match Value.read_iso_datetime "2026-03-15T12:00:00.1" with
     | Error why -> why = "at offset 20: expected exactly three fraction digits"
     | Ok _ -> false)

(* WV-5.10: the composite condition short-circuits. The members are read left to
   right in WIRE order and evaluation stops at the first member after which the
   outcome stands, so a member the outcome no longer reads deposits NO §9.5
   fact. Both ch. 5.10 tables verbatim (corpus goldens jutterswet-ploegaandeel
   and jutterswet-wachtdeling): member 1 tests the divisor, member 2 divides by
   it. A division by the numeric zero deposits EF-005 since v1 routed it into
   the channel (ch. 1.4 point 4); until then it faulted, and these examples
   pinned the fault's absence where they now pin the channel's. *)
let () =
  let division : Ir_v0.Ast.expr =
    Rounding
      { base_expr =
          Some
            (Binary
               { op = Divided_by;
                 left_side = Some (aref "vangstwaarde" "Wacht");
                 right_side = Some (aref "ploegomvang" "Wacht");
                 typ = Some num0; source_name = None });
        mode_kind = Floor; decimal_places = 0; typ = Some num0; source_name = None }
  in
  let cmp op left_side right_side : Ir_v0.Ast.condition_expr =
    Comparison { op; left_side = Some left_side; right_side = Some right_side; time_period = None; source_name = None }
  in
  let member1 op = cmp op (aref "ploegomvang" "Wacht") (litv "0" num0) in
  let member2 = cmp Greater division (litv "0" num0) in
  let doc_of quantifier_kind cutoff op1 : Ir_v0.Ast.document =
    { empty_doc with
      object_types =
        [ ot "Wacht"
            [ ("ploegomvang", Some num0); ("vangstwaarde", Some num0); ("merk", Some num0) ] [] ];
      resolved_rules =
        [ rr "merk" Equation
            (Some (aref "merk" "Wacht"))
            (Some (litv "1" num0))
            (Some (Compound { quantifier_kind; cutoff; components = [ member1 op1; member2 ]; source_name = None }));
        ];
    }
  in
  let final doc ploeg vangst =
    let w =
      World.add_instance World.empty
        (inst "w" "Wacht" [ ("ploegomvang", n ploeg); ("vangstwaarde", n vangst) ])
    in
    Eval.evaluate (env_of doc) w
  in
  let run doc ploeg vangst = World.attr (World.get (final doc ploeg vangst) "w") "merk" in
  (* the channel after the run: (rule, subject, codes) per fact *)
  let channel doc ploeg vangst =
    List.map (fun (f : World.error_fact) -> (f.rule, f.subject, f.codes)) (final doc ploeg vangst).channel
  in
  let unread name doc ploeg vangst = check (name ^ " (no §9.5 fact)") (channel doc ploeg vangst = []) in
  (* table 1 — `all`, member 1 "ploegomvang ongelijk aan 0" *)
  let every = doc_of All 0 Not_equal in
  expect_empty "WV-5.10-alle-deler-0-geen-fault" (run every "0" "100");
  unread "WV-5.10-alle-deler-0-geen-fault" every "0" "100";
  expect_num "WV-5.10-alle-deler-4" (run every "4" "100") "1";
  expect_empty "WV-5.10-alle-deler-7-lid2-beslist" (run every "7" "3");
  (* table 2 — the SAME two members, member 1 "ploegomvang gelijk aan 0",
     under `none` (the stop fixes the outcome false) and `atLeast` 1 (the same
     stop fixes it true): one trigger, opposite verdicts *)
  let none_value = doc_of None_of 0 Equal and least = doc_of At_least 1 Equal in
  expect_empty "WV-5.10-geen-deler-0-geen-fault" (run none_value "0" "100");
  unread "WV-5.10-geen-deler-0-geen-fault" none_value "0" "100";
  expect_empty "WV-5.10-geen-deler-4" (run none_value "4" "100");
  expect_num "WV-5.10-geen-deler-7" (run none_value "7" "3") "1";
  expect_num "WV-5.10-tenminste-deler-0-geen-fault" (run least "0" "100") "1";
  unread "WV-5.10-tenminste-deler-0-geen-fault" least "0" "100";
  expect_num "WV-5.10-tenminste-deler-4" (run least "4" "100") "1";
  expect_empty "WV-5.10-tenminste-deler-7" (run least "7" "3");
  (* the clause is load-bearing: WITHOUT the stop, member 2 is read on a zero
     divisor and deposits its §9.5 fact. The one-member composite isolates that
     member: no prefix can settle the outcome, so it IS read, and the fact is
     the pre-clause behaviour of every table row above. *)
  let naked : Ir_v0.Ast.document =
    { (doc_of All 0 Not_equal) with
      resolved_rules =
        [ rr "merk" Equation
            (Some (aref "merk" "Wacht"))
            (Some (litv "1" num0))
            (Some (Compound { quantifier_kind = All; cutoff = 0; components = [ member2 ]; source_name = None }));
        ];
    }
  in
  expect_empty "WV-5.10-ongepoort-lid-fault-wel" (run naked "0" "100");
  check "WV-5.10-ongepoort-lid-fault-wel (§9.5 EF-005)"
    (channel naked "0" "100" = [ ("merk", "w", [ "EF-005" ]) ]);
  (* the OUT-OF-REACH trigger stops too: TEN MINSTE 2 over two members whose
     first does not hold can no longer reach 2, so member 2 stays unread even
     though no hit has been counted *)
  let least2 = doc_of At_least 2 Not_equal in
  expect_empty "WV-5.10-tenminste-2-onhaalbaar-geen-fault" (run least2 "0" "100");
  unread "WV-5.10-tenminste-2-onhaalbaar-geen-fault" least2 "0" "100";
  (* ---- one example per trigger row, each pinning the §9.5 fact's absence
     (until v1: the fault's) rather than only the verdict. Without these the trigger table is
     spec text with no artifact: an executor may drop, add or defer a row and
     every gate still passes. Each row below was measured against an arm-exact
     ref mutant that changes exactly that row. ---- *)
  let doc_members quantifier_kind cutoff components : Ir_v0.Ast.document =
    { empty_doc with
      object_types =
        [ ot "Wacht"
            [ ("ploegomvang", Some num0); ("vangstwaarde", Some num0); ("merk", Some num0) ] [] ];
      resolved_rules =
        [ rr "merk" Equation
            (Some (aref "merk" "Wacht"))
            (Some (litv "1" num0))
            (Some (Compound { quantifier_kind; cutoff; components; source_name = None }));
        ];
    }
  in
  let true_ploeg0 = member1 Equal in
  let true_vangst = cmp Greater (aref "vangstwaarde" "Wacht") (litv "0" num0) in
  let faultend = member2 in
  (* THE STOP IS CHECKED BEFORE THE MEMBER, NOT AFTER. At TEN MINSTE with a
     threshold ABOVE the member count the out-of-reach trigger fires on the
     EMPTY prefix, so NOTHING is read — a checker that evaluates first and asks
     afterwards reads the depositing member 1 and deposits. *)
  expect_empty "WV-5.10-drempel-boven-ledental-leest-niets"
    (run (doc_members At_least 3 [ faultend; true_ploeg0 ]) "0" "100");
  unread "WV-5.10-drempel-boven-ledental-leest-niets" (doc_members At_least 3 [ faultend; true_ploeg0 ]) "0" "100";
  (* AT-MOST'S TRUE DIRECTION IS A TRIGGER (maintainer decision 2026-09-07;
     this replaces the earlier boundary that excluded it). With d >= n the
     outcome can no longer become false, so it STANDS on the empty prefix and
     NOTHING is read — the depositing member 1 included. Vacuously true. *)
  expect_num "WV-5.10-atmost-vacuum-waar-leest-niets"
    (run (doc_members At_most 2 [ faultend; true_ploeg0 ]) "0" "100") "1";
  unread "WV-5.10-atmost-vacuum-waar-leest-niets" (doc_members At_most 2 [ faultend; true_ploeg0 ]) "0" "100";
  (* the same trigger below the member count: after two non-holding members of
     four, two remain and the threshold is 2, so hits can no longer EXCEED it
     and the outcome stands true — the depositing third member stays unread *)
  expect_num "WV-5.10-atmost-waar-stop-onder-ledental"
    (run (doc_members At_most 2 [ member1 Greater; member1 Greater; faultend; true_ploeg0 ]) "0" "100")
    "1";
  unread "WV-5.10-atmost-waar-stop-onder-ledental"
    (doc_members At_most 2 [ member1 Greater; member1 Greater; faultend; true_ploeg0 ]) "0" "100";
  (* AT-MOST'S FALSE DIRECTION IS a trigger: two hits past a threshold of 1
     settle the outcome, and the depositing third member is never read. *)
  expect_empty "WV-5.10-atmost-overschrijding-stopt"
    (run (doc_members At_most 1 [ true_ploeg0; true_vangst; faultend ]) "0" "100");
  unread "WV-5.10-atmost-overschrijding-stopt" (doc_members At_most 1 [ true_ploeg0; true_vangst; faultend ]) "0" "100";
  (* `exactly`'s OVER-THRESHOLD TRIGGER, likewise: hits 2 against a threshold of 1
     settle it false and the depositing third member stays unread. *)
  expect_empty "WV-5.10-precies-overschrijding-stopt"
    (run (doc_members Exactly 1 [ true_ploeg0; true_vangst; faultend ]) "0" "100");
  unread "WV-5.10-precies-overschrijding-stopt" (doc_members Exactly 1 [ true_ploeg0; true_vangst; faultend ]) "0" "100";
  (* `exactly`'s OUT-OF-REACH TRIGGER: a threshold above the member count settles
     it false on the empty prefix, so nothing is read. *)
  expect_empty "WV-5.10-precies-onhaalbaar-leest-niets"
    (run (doc_members Exactly 3 [ faultend; true_ploeg0 ]) "0" "100");
  unread "WV-5.10-precies-onhaalbaar-leest-niets" (doc_members Exactly 3 [ faultend; true_ploeg0 ]) "0" "100"

(* --- ch. 5.11: operand order — one pinned pair per visible sweep row ----
   Each row below is TWO worked examples over one shape: two operand
   positions, each carrying an expression that FAULTS with a message of its
   own, swapped between the pair. Which message surfaces is decided by the
   evaluation order and by nothing else — both positions are reachable under
   either order, both faults are available, and no value is ever produced. An
   executor that walks the node's operands the other way fails BOTH examples
   of the row, which is what the corpus witnesses alone did not give: the
   golden and the two red cases pin `Binary`, `Clamp` and tijdsduur, and
   every other visible row of ch. 5.11's sweep table was spec text with no
   artifact until this battery (measured: reversing both ordered walks in
   Eval left every gate green).

   The order is pinned by FAULT here, not by §9.5 count, and deliberately: a
   channel insert is idempotent per (rule × subject), so two deposits from one
   node collapse into one fact and cannot tell the two positions apart. The
   corpus witnesses can use the channel because there a ch. 10.6 barrier makes
   the second deposit not happen at all. A barrier needs a binder in scope,
   which a closed fold body (WF-069) does not have — the fault device reaches
   every row uniformly, including those. *)
let () =
  let contains hay needle =
    let nl = String.length needle and hl = String.length hay in
    let rec go i = i + nl <= hl && (String.sub hay i nl = needle || go (i + 1)) in
    nl = 0 || go 0
  in
  let expect_msg name f frag =
    match f () with
    | exception Value.Fault { text = m; _ } -> check (name ^ " (got: " ^ m ^ ")") (contains m frag)
    | _ -> check (name ^ " (no fault at all)") false
  in
  (* the two distinguishable faults. The first was the division by a numeric
     zero until that went to the §9.5 channel (ch. 1.4 point 4, card
     zero-division-channel); the evaluation year read in a session without an
     evaluation date faults at its read as the division did (ch. 8.2) *)
  let tree : Ir_v0.Ast.expr =
    Context_value { ident = Some Ir_v0.Spelling.evaluation_year; typ = Some num0; source_name = None }
  in
  let pit : Ir_v0.Ast.expr = Expr_unresolved { typ = Some num0; source_name = None } in
  let bm = "the evaluationYear context value needs an evaluation date"
  and pm = "Unresolved reached in evaluation" in
  let element_list : Ir_v0.Ast.datatype = List_type { element = num0 } in
  let pair_domain : Ir_v0.Ast.datatype =
    Struct_type { domain_spec = "paar"; field_list = [ ("een", num0); ("twee", num0) ] }
  in
  let doc_of fds cond value_node : Ir_v0.Ast.document =
    { empty_doc with
      object_types = [ ot "Vangst" [ ("teller", Some num0); ("uitkomst", Some num0) ] [] ];
      function_defs = fds;
      resolved_rules =
        [ rr "uitkomst van een vangst" Equation
            (Some (aref "uitkomst" "Vangst"))
            (Some value_node) cond ];
    }
  in
  let world_state () = World.add_instance World.empty (inst "v" "Vangst" [ ("teller", n "4") ]) in
  let run_v value_node () = ignore (Eval.evaluate (env_of (doc_of [] None value_node)) (world_state ())) in
  let run_f fds value_node () = ignore (Eval.evaluate (env_of (doc_of fds None value_node)) (world_state ())) in
  let run_c cond () =
    ignore (Eval.evaluate (env_of (doc_of [] (Some cond) (litv "1" num0))) (world_state ()))
  in
  (* row: Vergelijking — links before rechts *)
  let cmp op l r : Ir_v0.Ast.condition_expr =
    Comparison { op; left_side = Some l; right_side = Some r; time_period = None; source_name = None }
  in
  expect_msg "WV-5.11-vergelijking-links-eerst" (run_c (cmp Equal tree pit)) bm;
  expect_msg "WV-5.11-vergelijking-rechts-later" (run_c (cmp Equal pit tree)) pm;
  (* row: aggregation — the contributors in wire order, and BEFORE the
     empty_fallback. The stand-in is the LAST operand of the node, not the first. *)
  let summed args empty_value : Ir_v0.Ast.expr =
    Function_call
      { kind = Sum_function; call_arguments = args; element_object_type = None; empty_fallback = empty_value;
        filter = None; dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role = None; source_path = None }
  in
  expect_msg "WV-5.11-som-bijdrager0-eerst" (run_v (summed [ tree; pit ] None)) bm;
  expect_msg "WV-5.11-som-bijdrager1-later" (run_v (summed [ pit; tree ] None)) pm;
  expect_msg "WV-5.11-som-bijdragers-voor-leegwaarde" (run_v (summed [ tree ] (Some pit))) bm;
  expect_msg "WV-5.11-som-leegwaarde-na-bijdragers" (run_v (summed [ pit ] (Some tree))) pm;
  (* and the leegwaarde is evaluated UNCONDITIONALLY: a filled contributor
     leaves the stand-in unused, never unevaluated. Its faults and its §9.5
     deposits happen whether or not its value is read (ch. 5.11). *)
  expect_msg "WV-5.11-som-leegwaarde-onvoorwaardelijk"
    (run_v (summed [ litv "5" num0 ] (Some tree))) bm;
  (* row: concatenatie and TekstReeks — part order *)
  let concat args : Ir_v0.Ast.expr =
    Function_call
      { kind = Concatenation; call_arguments = args; element_object_type = None; empty_fallback = None;
        filter = None; dimension_selection = None; typ = Some Text_type; source_name = None;
        grain = None; source_role = None; source_path = None }
  in
  expect_msg "WV-5.11-concat-deel0-eerst" (run_v (concat [ tree; pit ])) bm;
  expect_msg "WV-5.11-concat-deel1-later" (run_v (concat [ pit; tree ])) pm;
  let sequence a b : Ir_v0.Ast.expr =
    Text_composition
      { pieces = [ Embedded_expression { expr = Some a }; Embedded_expression { expr = Some b } ];
        typ = Some Text_type; source_name = None }
  in
  expect_msg "WV-5.11-tekstreeks-deel0-eerst" (run_v (sequence tree pit)) bm;
  expect_msg "WV-5.11-tekstreeks-deel1-later" (run_v (sequence pit tree)) pm;
  (* row: dateWith / dateTimeWith — the parts in argument order *)
  let date_with args : Ir_v0.Ast.expr =
    Function_call
      { kind = Date_with; call_arguments = args @ [ litv "1" num0 ]; element_object_type = None;
        empty_fallback = None; filter = None; dimension_selection = None; typ = Some Date_in_days;
        source_name = None; grain = None; source_role = None; source_path = None }
  in
  expect_msg "WV-5.11-datummet-deel0-eerst" (run_v (date_with [ tree; pit ])) bm;
  expect_msg "WV-5.11-datummet-deel1-later" (run_v (date_with [ pit; tree ])) pm;
  (* row: ListLiteral — element order *)
  let list_lit a b : Ir_v0.Ast.expr =
    List_literal { typ = Some element_list; elements = [ a; b ]; source_name = None }
  in
  expect_msg "WV-5.11-listliteral-element0-eerst" (run_v (list_lit tree pit)) bm;
  expect_msg "WV-5.11-listliteral-element1-later" (run_v (list_lit pit tree)) pm;
  (* row: StructLiteral — declared field order (WF-073) *)
  let structlit a b : Ir_v0.Ast.expr =
    Struct_literal { typ = Some pair_domain; field_list = [ ("een", a); ("twee", b) ]; source_name = None }
  in
  expect_msg "WV-5.11-structliteral-veld0-eerst" (run_v (structlit tree pit)) bm;
  expect_msg "WV-5.11-structliteral-veld1-later" (run_v (structlit pit tree)) pm;
  (* row: Invoke — argument order (the body runs only after all of them) *)
  let fds =
    [ ({ fd_name = "kies"; fd_parameters = [ ("een", num0); ("twee", num0) ]; fd_result = num0;
         fd_body = litv "1" num0; fd_source = None } : Ir_v0.Ast.function_def) ]
  in
  let call a b : Ir_v0.Ast.expr =
    Invoke { typ = Some num0; function_name = "kies"; arguments = [ a; b ]; source_name = None }
  in
  expect_msg "WV-5.11-invoke-arg0-eerst" (run_f fds (call tree pit)) bm;
  expect_msg "WV-5.11-invoke-arg1-later" (run_f fds (call pit tree)) pm;
  (* row: Fold — the `list` operand before the `init` operand *)
  let fold_node lst ini : Ir_v0.Ast.expr =
    Fold
      { typ = Some num0; list = lst; init = ini;
        function_name =
          Fn_lambda
            { parameters = [ ("acc", num0); ("x", num0) ];
              body = Binder_ref { ident = "acc"; source_name = None }; source_name = None };
        source_name = None }
  in
  expect_msg "WV-5.11-fold-list-voor-init" (run_v (fold_node tree pit)) bm;
  expect_msg "WV-5.11-fold-init-na-list" (run_v (fold_node pit tree)) pm;
  (* row: MapList and FilterList — the elements in list order. A fold body is
     CLOSED (WF-069), so no binder from an enclosing Let is in scope and the
     barrier device is unavailable; the body branches on the element instead,
     so the two elements fault differently and the surviving message names the
     element that ran first. *)
  let branch_node : Ir_v0.Ast.expr =
    If_then_else
      { typ = Some num0;
        condition =
          cmp Greater (Binder_ref { ident = "x"; source_name = None }) (litv "0" num0);
        then_ = tree; else_ = pit; source_name = None }
  in
  let couple a b : Ir_v0.Ast.expr =
    List_literal { typ = Some element_list; elements = [ litv a num0; litv b num0 ]; source_name = None }
  in
  let map_node l : Ir_v0.Ast.expr =
    Map_list
      { typ = Some element_list; list = l;
        function_name = Fn_lambda { parameters = [ ("x", num0) ]; body = branch_node; source_name = None };
        source_name = None }
  in
  expect_msg "WV-5.11-maplist-element0-eerst" (run_v (map_node (couple "1" "0"))) bm;
  expect_msg "WV-5.11-maplist-element1-later" (run_v (map_node (couple "0" "1"))) pm;
  let sieve l : Ir_v0.Ast.expr =
    Filter_list
      { typ = Some element_list; list = l; binder = "x";
        condition = cmp Greater branch_node (litv "0" num0); source_name = None }
  in
  expect_msg "WV-5.11-filterlist-element0-eerst" (run_v (sieve (couple "1" "0"))) bm;
  expect_msg "WV-5.11-filterlist-element1-later" (run_v (sieve (couple "0" "1"))) pm;
  (* row: the aggregation's INSTANCE fold — the filter per member, members in
     fold order. The listed-`call_arguments` branch above cannot reach this: an
     instance fold takes its contributors from a role, and its `filter` runs
     once per MEMBER with that member as the condition's subject. So the
     branching trick moves into the filter and the members carry the switch —
     `merk` = 1 selects the division, `merk` = 0 the marker, and which member
     runs first decides which fault survives. Both filtered walks are pinned:
     the `Instances` one an `aantal` takes, and the `Values` one a `som` over
     a set-read takes. Measured: without this pair, reversing BOTH member
     walks passes every gate and every other example here. *)
  let ftn : (string * Ir_v0.Ast.fact_type_name) list =
    [ ( "deelname",
        { ft_name = Some "deelname"; reciprocal = false;
          role_names =
            [ { role_identifier = Some "vangst"; role_object_type_name = Some "Vangst";
                role_plural = None; role_many = false };
              { role_identifier = Some "deelvangst"; role_object_type_name = Some "Deelvangst";
                role_plural = Some "deelvangsten"; role_many = true } ]; } ) ]
  in
  let members_doc : Ir_v0.Ast.document =
    { empty_doc with
      object_types =
        [ ot "Vangst" [ ("teller", Some num0); ("uitkomst", Some num0) ] [];
          ot "Deelvangst" [ ("merk", Some num0); ("bedrag", Some num0) ] [] ];
      fact_types = ftn }
  in
  (* the filter faults one way on a member with merk > 0 and the other way on
     a member with merk = 0 *)
  let member_filter : Ir_v0.Ast.condition_expr =
    cmp Greater
      (If_then_else
         { typ = Some num0;
           condition = cmp Greater (aref "merk" "Deelvangst") (litv "0" num0);
           then_ = tree; else_ = pit; source_name = None })
      (litv "0" num0)
  in
  let members_world m1 m2 =
    List.fold_left World.add_instance World.empty
      [ { (inst "v" "Vangst" []) with
          left_side = World.SMap.add "deelvangsten" [ "d1"; "d2" ] World.SMap.empty };
        inst "d1" "Deelvangst" [ ("merk", n m1); ("bedrag", n "1") ];
        inst "d2" "Deelvangst" [ ("merk", n m2); ("bedrag", n "1") ] ]
  in
  let run_members e m1 m2 () =
    let w = members_world m1 m2 in
    ignore (Eval.eval_expr (env_of members_doc) w (World.get w "v") ~err:no_err e)
  in
  (* the `Instances` walk: an aantal over the bare role *)
  let counting : Ir_v0.Ast.expr =
    Function_call
      { kind = Count;
        call_arguments =
          [ Attribute_reference { attribute_ref = Some "deelvangsten"; object_type = Some "Vangst"; source_name = None; navigation_bindings = None } ];
        element_object_type = Some "Deelvangst"; empty_fallback = None; filter = Some member_filter;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = Some { rb_fact_type = "deelname"; rb_role = "deelvangst" }; source_path = None }
  in
  expect_msg "WV-5.11-instantiefold-lid0-eerst" (run_members counting "1" "0") bm;
  expect_msg "WV-5.11-instantiefold-lid1-later" (run_members counting "0" "1") pm;
  (* the `Values` walk: a som over the members' set-read attribute *)
  let members_sum : Ir_v0.Ast.expr =
    Function_call
      { kind = Sum_function;
        call_arguments =
          [ Attribute_reference
              { attribute_ref = Some "bedrag"; object_type = Some "Deelvangst"; source_name = None; navigation_bindings = None } ];
        element_object_type = Some "Deelvangst"; empty_fallback = None; filter = Some member_filter;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None;
        source_role = Some { rb_fact_type = "deelname"; rb_role = "deelvangst" }; source_path = None }
  in
  expect_msg "WV-5.11-instantiesom-lid0-eerst" (run_members members_sum "1" "0") bm;
  expect_msg "WV-5.11-instantiesom-lid1-later" (run_members members_sum "0" "1") pm

(* --- ch. 7.10: inheritance, the effective stack per type ---------------- *)

(* WV-7.10-a: the six cells of ch. 7.10.1 on the smallest model that
   separates them. `Wrakrecht is een Strandrecht`; the supertype's group is
   base = grondslag with a 99-exception on `betwist`, the subtype's is
   base = grondslag x 2 with a 55-exception on bergingsdiepte > 20. The
   effective Wrakrecht stack is the own 55, the inherited 99, the own base
   20 — so the pinned pair (row 4 = 99, row 3 = 20) is exactly what a flat
   lowering cannot produce at once, and row 6 = 55 separates child-wins
   from parent-wins and from an unordered no-write. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let ot_sub name super attrs characteristics : string * Ir_v0.Ast.object_type =
    let n, o = ot name attrs characteristics in
    (n, { o with ot_supertype = Some super })
  in
  let always : Ir_v0.Ast.condition_expr =
    Compound { quantifier_kind = All; cutoff = 0; components = []; source_name = None }
  in
  let d precondition consequence : Ir_v0.Ast.default =
    { default_decision = None; default_justification = Some precondition;
      default_consequence = Some consequence; default_initialisation = false;
      default_exceptions = None; default_source = None }
  in
  let cluster subject_ref defaults : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some ("uitkering van een " ^ subject_ref); pd_subject = subject_ref;
      pd_characteristic = None; pd_attribute = Some "uitkering"; defaults; pd_unordered = false;
      pd_source = None; pd_validity = None }
  in
  let betwist : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some "betwist"; negation = false;
                    holder = None; time_period = None; source_name = None; navigation_binding = None }
  in
  let deep : Ir_v0.Ast.condition_expr =
    Comparison { op = Greater; left_side = Some (aref "bergingsdiepte" "Wrakrecht");
                   right_side = Some (litv "20" num0); time_period = None; source_name = None }
  in
  let doc =
    { empty_doc with
      object_types =
        [ ot "Strandrecht" [ ("grondslag", Some amount); ("uitkering", Some amount);
                             ("registratiekosten", Some amount) ] [ "betwist" ];
          ot_sub "Wrakrecht" "Strandrecht" [ ("bergingsdiepte", Some num0) ] [] ];
      (* the bare rule on the SUPERTYPE: extensional quantification makes it
         fire for a subtype instance too (ch. 2.2) *)
      resolved_rules =
        [ rr "registratiekosten van een strandrecht" Calculation
            (Some (aref "registratiekosten" "Strandrecht"))
            (Some (litv "5.00" amount)) None ];
      prioritized_defaults =
        [ cluster "Strandrecht" [ d betwist (litv "99.00" amount);
                                d always (aref "grondslag" "Strandrecht") ];
          cluster "Wrakrecht"
            [ d deep (litv "55.00" amount);
              d always
                (Binary { op = Times; left_side = Some (aref "grondslag" "Wrakrecht");
                          right_side = Some (litv "2" num0); typ = Some amount; source_name = None }) ] ];
    }
  in
  let run otype ~betwist:b ~nesting_depth =
    let i = inst "x" otype
        ([ ("grondslag", n "10.00") ]
         @ match nesting_depth with Some dp -> [ ("bergingsdiepte", n dp) ] | None -> [])
    in
    let i = if b then { i with World.chars = [ "betwist" ] } else i in
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty i) in
    (World.attr (World.get final "x") "uitkering",
     World.attr (World.get final "x") "registratiekosten",
     (World.get final "x").World.otype, final.World.channel)
  in
  let uitkering (u, _, _, _) = u and kosten (_, k, _, _) = k
  and error_channel (_, _, _, c) = c in
  let r1 = run "Strandrecht" ~betwist:false ~nesting_depth:None in
  let r2 = run "Strandrecht" ~betwist:true ~nesting_depth:None in
  let r3 = run "Wrakrecht" ~betwist:false ~nesting_depth:(Some "5") in
  let r4 = run "Wrakrecht" ~betwist:true ~nesting_depth:(Some "5") in
  let r5 = run "Wrakrecht" ~betwist:false ~nesting_depth:(Some "25") in
  let r6 = run "Wrakrecht" ~betwist:true ~nesting_depth:(Some "25") in
  expect_num "WV-7.10-a-rij1-supertype-basis" (uitkering r1) "10.00";
  expect_num "WV-7.10-a-rij2-supertype-uitzondering" (uitkering r2) "99.00";
  expect_num "WV-7.10-a-rij3-basisvervanging" (uitkering r3) "20.00";
  expect_num "WV-7.10-a-rij4-geerfde-uitzondering-boven-eigen-basis" (uitkering r4) "99.00";
  expect_num "WV-7.10-a-rij5-eigen-uitzondering" (uitkering r5) "55.00";
  expect_num "WV-7.10-a-rij6-kind-wint" (uitkering r6) "55.00";
  (* row 6 is an ORDINARY write: the layer boundary deposits no conflict *)
  check "WV-7.10-a-rij6-geen-conflictfeit" (error_channel r6 = []);
  (* the supertype's bare rule fires for the subtype instance *)
  expect_num "WV-7.10-a-supertyperegel-op-subtype" (kosten r3) "5.00";
  expect_num "WV-7.10-a-supertyperegel-op-supertype" (kosten r1) "5.00"

(* WV-7.10-b: the ch. 7.2 conflict key names the type that DECLARES the
   attribute, never the instance's concrete type. Two declared-unordered
   groups on `Strandrecht.uitkering` both decide, differently, on a
   `Wrakrecht` instance — the one conflict shape inheritance leaves: a clash
   INSIDE an inherited internally-unordered block, surfacing for the first
   time on the subtype. One target reports under one name. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let ot_sub name super attrs : string * Ir_v0.Ast.object_type =
    let n, o = ot name attrs [] in
    (n, { o with ot_supertype = Some super })
  in
  let kc k : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some k; negation = false; holder = None;
                    time_period = None; source_name = None; navigation_binding = None }
  in
  let cluster k value_node : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some ("uitkering bij " ^ k); pd_subject = "Strandrecht"; pd_characteristic = None;
      pd_attribute = Some "uitkering";
      defaults =
        [ { default_decision = None; default_justification = Some (kc k);
            default_consequence = Some (litv value_node amount); default_initialisation = false;
            default_exceptions = None; default_source = None } ];
      pd_unordered = true; pd_source = None; pd_validity = None }
  in
  let doc =
    { empty_doc with
      object_types =
        [ ot "Strandrecht" [ ("uitkering", Some amount) ] [ "betwist"; "geborgen" ];
          ot_sub "Wrakrecht" "Strandrecht" [] ];
      prioritized_defaults = [ cluster "betwist" "99.00"; cluster "geborgen" "44.00" ] }
  in
  let i = { (inst "w" "Wrakrecht" []) with World.chars = [ "betwist"; "geborgen" ] } in
  let final = Eval.evaluate (env_of doc) (World.add_instance World.empty i) in
  expect_empty "WV-7.10-b-botsing-schrijft-niet" (World.attr (World.get final "w") "uitkering");
  check "WV-7.10-b-sleutel-noemt-de-declarant"
    (List.map (fun (f : World.error_fact) -> (f.rule, f.subject)) final.World.channel
     = [ ("Strandrecht.uitkering", "w") ])

(* WV-7.10-c: the two corners ch. 7.10 refuses rather than lowers. Both are
   NAMED refusals before any value exists, so all three executors stop on the
   same document instead of each answering its own way. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let ot_sub name super attrs : string * Ir_v0.Ast.object_type =
    let n, o = ot name attrs [] in
    (n, { o with ot_supertype = Some super })
  in
  let kc k : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some k; negation = false; holder = None;
                    time_period = None; source_name = None; navigation_binding = None }
  in
  let cluster subject_ref k value_node unordered : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some (subject_ref ^ " bij " ^ k); pd_subject = subject_ref; pd_characteristic = None;
      pd_attribute = Some "uitkering";
      defaults =
        [ { default_decision = None; default_justification = Some (kc k);
            default_consequence = Some (litv value_node amount); default_initialisation = false;
            default_exceptions = None; default_source = None } ];
      pd_unordered = unordered; pd_source = None; pd_validity = None }
  in
  let types =
    [ ot "Strandrecht" [ ("uitkering", Some amount) ] [ "betwist"; "geborgen" ];
      ot_sub "Wrakrecht" "Strandrecht" [] ]
  in
  let w = World.add_instance World.empty (inst "w" "Wrakrecht" []) in
  (* (1) a declared-unordered SET on one level and an inherited stack on the
     other: the block order composes one stack over one stack *)
  let doc_set =
    { empty_doc with object_types = types;
      prioritized_defaults =
        [ cluster "Strandrecht" "betwist" "99.00" true; cluster "Strandrecht" "geborgen" "44.00" true;
          cluster "Wrakrecht" "betwist" "55.00" false ] }
  in
  (* (1a) ONE own group over an inherited declared-unordered SET. §4.1 rule 3
     DEFINES this — the inherited block moves whole, above the base and below
     the own exceptions — so the refusal names it as not lowered rather than
     as undefined. The reason is a property of the wire: that reading is a
     three-layer priority, and a group's members reconcile as EQUALS. *)
  expect_fault_msg "WV-7.10-c-geerfde-ongeordende-set-regel-3"
    (fun () -> Eval.evaluate (env_of doc_set) w)
    "SUBTYPING §4.1 rule 3 defines it";
  (* (1b) a declared-unordered set on the OWN level too. Here the design
     itself is silent — §4.1 rule 3 speaks of an unordered PARENT group and
     no rule places a subtype's own set against an inherited stack — so this
     one is genuinely undefined, not merely un-lowered. *)
  let doc_both =
    { empty_doc with object_types = types;
      prioritized_defaults =
        [ cluster "Strandrecht" "betwist" "99.00" true; cluster "Strandrecht" "geborgen" "44.00" true;
          cluster "Wrakrecht" "betwist" "55.00" true; cluster "Wrakrecht" "geborgen" "66.00" true ] }
  in
  expect_fault_msg "WV-7.10-c-eigen-ongeordende-set-ongedefinieerd"
    (fun () -> Eval.evaluate (env_of doc_both) w)
    "has no defined reading";
  ()

(* WV-7.10-e/f: an ORDINARY rule as the base of its level — SUBTYPING §4.1
   rules 1 and 4, LOWERED. ch. 7.8 already reads a bare computing rule as a
   default; inheritance takes that at its word, so the rule is the one-member
   base of the level that declares it. The two shapes and their boundary. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let ot_sub name super : string * Ir_v0.Ast.object_type =
    let n, o = ot name [] [] in
    (n, { o with ot_supertype = Some super })
  in
  let types =
    [ ot "Strandrecht" [ ("uitkering", Some amount) ] [ "betwist" ]; ot_sub "Wrakrecht" "Strandrecht" ]
  in
  let always : Ir_v0.Ast.condition_expr =
    Compound { quantifier_kind = All; cutoff = 0; components = []; source_name = None }
  in
  let kc k : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some k; negation = false; holder = None;
                    time_period = None; source_name = None; navigation_binding = None }
  in
  let d precondition value_node : Ir_v0.Ast.default =
    { default_decision = None; default_justification = Some precondition;
      default_consequence = Some (litv value_node amount); default_initialisation = false;
      default_exceptions = None; default_source = None }
  in
  let cluster subject_ref defaults : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some ("uitkering van een " ^ subject_ref); pd_subject = subject_ref;
      pd_characteristic = None; pd_attribute = Some "uitkering"; defaults; pd_unordered = false;
      pd_source = None; pd_validity = None }
  in
  let run doc otype chars =
    let i = { (inst "x" otype []) with World.chars = List.map Ir_v0.Checks.norm chars } in
    World.attr (World.get (Eval.evaluate (env_of doc) (World.add_instance World.empty i)) "x")
      "uitkering"
  in
  (* (e) §4.1 rule 1, the BARE-RULE branch: the subtype's own rule IS its own
     base and replaces the inherited one, while the inherited exception stays
     above it. This is the penguin row with a RULE in the base position. *)
  let doc_e =
    { empty_doc with object_types = types;
      resolved_rules =
        [ rr "uitkering van een wrakrecht" Calculation
            (Some (aref "uitkering" "Wrakrecht")) (Some (litv "7.00" amount)) None ];
      prioritized_defaults =
        [ cluster "Strandrecht" [ d (kc "betwist") "99.00"; d always "30.00" ] ] }
  in
  expect_num "WV-7.10-e-supertype-basis" (run doc_e "Strandrecht" []) "30.00";
  expect_num "WV-7.10-e-supertype-uitzondering" (run doc_e "Strandrecht" [ "betwist" ]) "99.00";
  expect_num "WV-7.10-e-eigen-regel-vervangt-geerfde-basis" (run doc_e "Wrakrecht" []) "7.00";
  expect_num "WV-7.10-e-geerfde-uitzondering-boven-de-regelbasis"
    (run doc_e "Wrakrecht" [ "betwist" ]) "99.00";
  (* (f) §4.1 rule 4: the ANCESTOR's bare rule is the descendant's inherited
     base, and the descendant's own base replaces it. The same rule keeps
     governing the ancestor's own instances through the ordinary ch. 7.8
     path, because that chain declares no group — the boundary that keeps
     every flat model where it was. *)
  let doc_f =
    { empty_doc with object_types = types;
      resolved_rules =
        [ rr "uitkering van een strandrecht" Calculation
            (Some (aref "uitkering" "Strandrecht")) (Some (litv "30.00" amount)) None ];
      prioritized_defaults =
        [ cluster "Wrakrecht" [ d (kc "betwist") "99.00"; d always "20.00" ] ] }
  in
  expect_num "WV-7.10-f-voorouderregel-op-de-voorouder" (run doc_f "Strandrecht" []) "30.00";
  expect_num "WV-7.10-f-eigen-basis-vervangt-voorouderregel" (run doc_f "Wrakrecht" []) "20.00";
  expect_num "WV-7.10-f-eigen-uitzondering-boven-alles" (run doc_f "Wrakrecht" [ "betwist" ]) "99.00";
  (* the boundary: a bare rule with NO defaults group anywhere on its chain
     is the ordinary ch. 7.8 standing norm and enters no stack at all — which
     is every ordinary rule in a flat model, and the reason none of them move *)
  let doc_bare =
    { empty_doc with object_types = types;
      resolved_rules =
        [ rr "uitkering van een strandrecht" Calculation
            (Some (aref "uitkering" "Strandrecht")) (Some (litv "30.00" amount)) None ] }
  in
  expect_num "WV-7.10-f-kale-regel-blijft-de-staande-norm" (run doc_bare "Wrakrecht" []) "30.00";
  (* NOT lowered, and refused by name: a rule with a VALIDITY WINDOW beside an
     inherited stack. A windowed base would put version selection inside the
     stack, which ch. 7.4 keeps apart from defeasibility on purpose. The
     refusal has to be LOUD because absorption takes the rule out of the rule
     circuit: a fold that merely skipped the window would silence the writer
     without ever making it a base — measured, on both directions of the
     shape, as a silently empty target and as an active rule answering from
     the inherited base instead of its own value. *)
  let doc_window =
    { empty_doc with object_types = types;
      resolved_rules =
        [ { (rr "uitkering van een wrakrecht" Calculation
               (Some (aref "uitkering" "Wrakrecht")) (Some (litv "7.00" amount)) None)
            with rr_validity = Some { valid_from = Some "2020-01-01"; valid_to = None } } ];
      prioritized_defaults =
        [ cluster "Strandrecht" [ d (kc "betwist") "99.00"; d always "30.00" ] ] }
  in
  expect_fault_msg "WV-7.10-f-venster-in-de-basis-weigert"
    (fun () ->
      Eval.evaluate (env_of doc_window)
        (World.add_instance World.empty (inst "x" "Wrakrecht" [])))
    "a rule with a VALIDITY WINDOW beside an inherited defaults stack"

(* WV-7.10-d: the THREE-LEVEL claim of ch. 7.10 — "the grandchild's own
   exceptions above the child's, above the grandparent's, with the base
   contributed by the lowest declarant that has one". No rule per level is
   needed for that, because the fold reads the parent's EFFECTIVE stack and
   not its declarations; this pins the difference. Aa <- Bb <- Cc, each
   level declaring one exception and one base (11/30, 22/60, 99/90):

     effective Cc = [k3 -> 99]  own exception
                    [k2 -> 22]  the child's, inherited
                    [k1 -> 11]  the GRANDPARENT's, inherited THROUGH the child
                    [      90]  the own base, replacing both inherited ones

   A fold that read the parent's DECLARATIONS would lose the grandparent
   entirely and answer 90 on a k1-only instance; a fold that let the
   grandparent's base win would answer 30 where 99 is pinned. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let ot_sub name super : string * Ir_v0.Ast.object_type =
    let n, o = ot name [] [] in
    (n, { o with ot_supertype = Some super })
  in
  let always : Ir_v0.Ast.condition_expr =
    Compound { quantifier_kind = All; cutoff = 0; components = []; source_name = None }
  in
  let kc k : Ir_v0.Ast.condition_expr =
    Characteristic_check { characteristic = Some k; negation = false; holder = None;
                    time_period = None; source_name = None; navigation_binding = None }
  in
  let d precondition value_node : Ir_v0.Ast.default =
    { default_decision = None; default_justification = Some precondition;
      default_consequence = Some (litv value_node amount); default_initialisation = false;
      default_exceptions = None; default_source = None }
  in
  let cluster subject_ref k exemption base_expr : Ir_v0.Ast.prioritized_defaults =
    { pd_name = Some ("uitkering van een " ^ subject_ref); pd_subject = subject_ref;
      pd_characteristic = None; pd_attribute = Some "uitkering";
      defaults = [ d (kc k) exemption; d always base_expr ];
      pd_unordered = false; pd_source = None; pd_validity = None }
  in
  let doc =
    { empty_doc with
      object_types =
        [ ot "Aa" [ ("uitkering", Some amount) ] [ "k1"; "k2"; "k3" ];
          ot_sub "Bb" "Aa"; ot_sub "Cc" "Bb" ];
      prioritized_defaults =
        [ cluster "Aa" "k1" "11.00" "30.00"; cluster "Bb" "k2" "22.00" "60.00";
          cluster "Cc" "k3" "99.00" "90.00" ] }
  in
  let run otype chars =
    let i = { (inst "x" otype []) with World.chars = List.map Ir_v0.Checks.norm chars } in
    World.attr (World.get (Eval.evaluate (env_of doc) (World.add_instance World.empty i)) "x")
      "uitkering"
  in
  (* the three levels each answer from their OWN effective stack *)
  expect_num "WV-7.10-d-aa-basis" (run "Aa" []) "30.00";
  expect_num "WV-7.10-d-bb-basis" (run "Bb" []) "60.00";
  expect_num "WV-7.10-d-cc-basis" (run "Cc" []) "90.00";
  (* the grandparent's exception reaches the grandchild THROUGH the child *)
  expect_num "WV-7.10-d-cc-grootouderuitzondering" (run "Cc" [ "k1" ]) "11.00";
  expect_num "WV-7.10-d-cc-ouderuitzondering" (run "Cc" [ "k2" ]) "22.00";
  expect_num "WV-7.10-d-cc-eigen-uitzondering" (run "Cc" [ "k3" ]) "99.00";
  (* the ORDER inside the inherited block: the child's above the grandparent's *)
  expect_num "WV-7.10-d-cc-ouder-boven-grootouder" (run "Cc" [ "k1"; "k2" ]) "22.00";
  (* and the grandchild's own above both — 99, never the grandparent's base 30 *)
  expect_num "WV-7.10-d-cc-eigen-boven-alles" (run "Cc" [ "k1"; "k2"; "k3" ]) "99.00";
  (* the child still reads its own two, unaffected by the grandchild's level *)
  expect_num "WV-7.10-d-bb-grootouderuitzondering" (run "Bb" [ "k1" ]) "11.00";
  expect_num "WV-7.10-d-bb-eigen-boven-grootouder" (run "Bb" [ "k1"; "k2" ]) "22.00"

(* WV-IR2.2-a/b/c: the type-ranging positions of ch. 2.2 that the anchor does
   NOT reach. Quantification is extensional at EVERY such position, and the
   six-item list in the spec is only a list until each item has a witness:
   the anchor covers the rule subject, the instance fold's elementType and
   the defaults subject, and these three cover the rest — the uniqueness
   scope, the consistency subject, and a characteristic check's owner. *)
let () =
  let ot_sub name super attrs characteristics : string * Ir_v0.Ast.object_type =
    let n, o = ot name attrs characteristics in
    (n, { o with ot_supertype = Some super })
  in
  let types =
    [ ot "Vondst" [ ("nummer", Some num0); ("boete", Some num0) ] [ "gemeld" ];
      ot_sub "Wrakvondst" "Vondst" [] [] ]
  in
  (* (a) UNIQUENESS ranges over the extension: two instances of DIFFERENT
     types on one chain sharing a key collide, exactly as two of one type do.
     A literal-type reading would let a subtype instance duplicate a
     supertype instance's number in silence. *)
  let uni_doc =
    { empty_doc with
      object_types = types;
      uniquenesses =
        [ { uni_name = Some "nummer is uniek"; uni_subject = Some "Vondst";
            key_list = [ Some "nummer" ]; uni_source = None;
            uni_validity = None; scope_role = None; uni_pointer = "" } ] }
  in
  let uni_run a b =
    let w =
      World.add_instance
        (World.add_instance World.empty (inst "v" "Vondst" [ ("nummer", n a) ]))
        (inst "w" "Wrakvondst" [ ("nummer", n b) ])
    in
    (Eval.evaluate (env_of uni_doc) w).World.channel
  in
  check "WV-IR2.2-a-uniciteit-botst-over-de-keten" (uni_run "1" "1" <> []);
  check "WV-IR2.2-a-uniciteit-laat-verschillende-sleutels-staan" (uni_run "1" "2" = []);
  (* (b) a CONSISTENCY rule declared on the supertype is asserted of subtype
     instances too — the same extension the rule subject ranges over. *)
  let cons_doc =
    { empty_doc with
      object_types = types;
      consistencies =
        [ { cons_name = Some "boete blijft onder de grens"; cons_subject = Some "Vondst";
            assertion =
              Some
                (Comparison
                   { op = Less; left_side = Some (aref "boete" "Vondst");
                     right_side = Some (litv "100" num0); time_period = None; source_name = None });
            cons_source = None; cons_line = 0; cons_validity = None; cons_pointer = "" } ] }
  in
  let cons_run otype boete =
    let w = World.add_instance World.empty (inst "x" otype [ ("boete", n boete) ]) in
    (Eval.evaluate (env_of cons_doc) w).World.channel
  in
  check "WV-IR2.2-b-consistentie-op-supertype" (cons_run "Vondst" "150" <> []);
  check "WV-IR2.2-b-consistentie-vuurt-op-subtype" (cons_run "Wrakvondst" "150" <> []);
  check "WV-IR2.2-b-consistentie-zwijgt-binnen-de-grens" (cons_run "Wrakvondst" "50" = []);
  (* (c) a characteristic check's OWNER is satisfied extensionally: a check
     hosted on the supertype reads a subtype subject, because a Wrakvondst
     IS a Vondst. Under a literal reading WF-100's hosting refusal would
     fire on the legitimate form and the rule would fault instead of firing
     (ch. 5.8b). *)
  let owner_doc =
    { empty_doc with
      object_types = types;
      resolved_rules =
        [ rr "boete van een wrakvondst" Calculation
            (Some (aref "boete" "Wrakvondst"))
            (Some (litv "7" num0))
            (Some
               (Characteristic_check
                  { characteristic = Some "gemeld"; negation = false;
                    holder = Some "Vondst"; time_period = None; source_name = None;
                    navigation_binding = None })) ] }
  in
  let owner_run chars =
    let i = { (inst "x" "Wrakvondst" []) with World.chars = List.map Ir_v0.Checks.norm chars } in
    World.attr (World.get (Eval.evaluate (env_of owner_doc) (World.add_instance World.empty i)) "x")
      "boete"
  in
  expect_num "WV-IR2.2-c-kenmerkeigenaar-op-voorouder" (owner_run [ "gemeld" ]) "7";
  expect_empty "WV-IR2.2-c-kenmerkeigenaar-onvervuld" (owner_run [])

(* WV-5.6-enum-sleutel: RUNTIME-SEMANTICS ch. 5.6, a uniqueness key is compared
   under the ch. 1.2d value equality, so an ENUMERATION component collides up
   to the ci label identity — "Beschadigd" and "beschadigd" are one key. The
   verbatim form of golden jutterswet-keurspelling case 4 (V1 and V2 both
   marked); two different labels stay two keys. Card-sanctioned test touch
   (20260920-drools-enumnaam-identiteit), additive. *)
let () =
  let keurklasse : Ir_v0.Ast.datatype =
    Enumeration
      { domain_spec = Some "Keurklasse"; label_list = [ Some "gaaf"; Some "beschadigd"; Some "wrakhout" ] }
  in
  let doc =
    { empty_doc with
      domain_specs = [ ("Keurklasse", Some keurklasse) ];
      object_types = [ ot "Vondst" [ ("staat", Some keurklasse) ] [] ];
      uniquenesses =
        [ { uni_name = Some "eenmalige staat"; uni_subject = Some "Vondst";
            key_list = [ Some "staat" ]; uni_source = None;
            uni_validity = None; scope_role = None; uni_pointer = "" } ] }
  in
  let run a b =
    let w =
      World.add_instance
        (World.add_instance World.empty (inst "V1" "Vondst" [ ("staat", Value.Enum a) ]))
        (inst "V2" "Vondst" [ ("staat", Value.Enum b) ])
    in
    List.length (Eval.evaluate (env_of doc) w).World.channel
  in
  check "WV-5.6-enum-sleutel-twee-spellingen-botsen" (run "Beschadigd" "beschadigd" = 2);
  check "WV-5.6-enum-sleutel-verschillende-labels-niet" (run "Beschadigd" "gaaf" = 0)

(* --- ch. 3.9 / 3.9a: the whole-period COMPARISON and the QUANTIFIER FOLD --
   The five pinned rows of the §8.2 form
     "… indien de <attribuut> van ALLE <leden> van de <onderwerp>
        gedurende het gehele jaar groter is dan <grens>"
   plus the scalar comparison the matrix rests on, and the `none` twin. *)

let dagnr iso = match Value.date_of_iso iso with Value.Date d -> d | _ -> assert false

let seg van up_to value_node : Timeline.segment =
  { s_from = dagnr van; s_to = dagnr up_to; s_value = value_node }

(* Berger.dagvergoeding is month-granular; the qualifier's own grid is the
   YEAR, which is the ch. 3.9 independence the two must not conflate. *)
let kv_berger : string * Ir_v0.Ast.object_type =
  let base = ot "Berger" [ ("dagvergoeding", Some num0) ] [] in
  (fst base, { (snd base) with attribute_granularity = [ ("dagvergoeding", Some Month) ] })

let kv_ploegindeling : string * Ir_v0.Ast.fact_type_name =
  ( "ploegindeling",
    {
      ft_name = Some "ploegindeling";
      reciprocal = false;
      role_names =
        [
          { role_identifier = Some "ploeg"; role_object_type_name = Some "Bergingsploeg";
            role_plural = None; role_many = false };
          { role_identifier = Some "berger"; role_object_type_name = Some "Berger";
            role_plural = Some "bergers"; role_many = true };
        ];
    } )

let kv_doc =
  { empty_doc with
    object_types = [ ot "Bergingsploeg" [] []; kv_berger ];
    fact_types = [ kv_ploegindeling ] }

(* the matrix: "zijn dagvergoeding gedurende het gehele jaar groter is dan 40",
   read on the ELEMENT — a set:false own read, the qualifier riding it *)
let kv_test ?(op : Ir_v0.Ast.compare_op = Greater) ?(boundary = "40") ?(not_flag = false) () :
    Ir_v0.Ast.condition_expr =
  Comparison
    {
      op;
      left_side = Some (aref "dagvergoeding" "Berger");
      right_side = Some (litv boundary num0);
      time_period = Some { gp_granularity = true; gp_negated = not_flag };
      source_name = None;
    }

let kv_cond ?(quantifier_kind : Ir_v0.Ast.quantifier_kind = All) ?cutoff ?filter ?test () : Ir_v0.Ast.condition_expr
    =
  Quantifier_fold
    {
      kv_quantifier = quantifier_kind;
      kv_threshold = cutoff;
      kv_element_object_type = Some "Berger";
      kv_filter = filter;
      kv_test = Some (match test with Some t -> t | None -> kv_test ());
      kv_source = None;
      kv_source_role = Some { rb_fact_type = "ploegindeling"; rb_role = "berger" };
      kv_source_path = None;
    }

(* one world per row: the ploeg P1 and its bergers, each with its own
   dagvergoeding timeline *)
let kv_world member_list =
  let bergers =
    List.map
      (fun (label, tl) ->
        { (inst label "Berger" []) with
          World.tls = World.SMap.add "dagvergoeding" tl World.SMap.empty })
      member_list
  in
  let p1 =
    { (inst "P1" "Bergingsploeg" []) with
      World.left_side =
        (match member_list with
        | [] -> World.SMap.empty
        | _ -> World.SMap.add "bergers" (List.map fst member_list) World.SMap.empty) }
  in
  List.fold_left World.add_instance World.empty (p1 :: bergers)

let kv_eval ?quantifier_kind ?cutoff ?filter ?test member_list =
  let w = kv_world member_list in
  Eval.eval_cond (env_of kv_doc) w (World.get w "P1") ~err:no_err
    (kv_cond ?quantifier_kind ?cutoff ?filter ?test ())

let whole_year v = [ seg "2026-01-01" "2027-01-01" (n v) ]
let half_year v = [ seg "2026-01-01" "2026-07-01" (n v) ]

let () =
  (* THE FIVE PINS (the toolchain's own expectations for the §8.2 form) *)
  check "WV-3.9a-pin1-allen-heel-jaar-boven-de-grens"
    (kv_eval [ ("B1", whole_year "50"); ("B2", whole_year "60") ]);
  check "WV-3.9a-pin2-een-lid-onder-de-grens"
    (not (kv_eval [ ("B1", whole_year "50"); ("B2", whole_year "30") ]));
  check "WV-3.9a-pin3-een-lid-dekt-maar-een-half-jaar"
    (not (kv_eval [ ("B1", whole_year "50"); ("B2", half_year "60") ]));
  check "WV-3.9a-pin4-lege-set-is-vacuous-waar" (kv_eval []);
  check "WV-3.9a-pin5-precies-op-de-grens-is-niet-strikt-groter"
    (not (kv_eval [ ("B1", whole_year "40") ]));

  (* the `none` twin: the verdict is the complement PER MEMBER, never of the
     fold — a member with a gap does NOT have the value whole-period, so it
     does not block `none` either *)
  check "WV-3.9a-geen-allen-onder-de-grens"
    (kv_eval ~quantifier_kind:None_of [ ("B1", whole_year "30"); ("B2", whole_year "20") ]);
  check "WV-3.9a-geen-een-lid-erboven"
    (not (kv_eval ~quantifier_kind:None_of [ ("B1", whole_year "30"); ("B2", whole_year "50") ]));
  check "WV-3.9a-geen-lege-set-is-vacuous-waar" (kv_eval ~quantifier_kind:None_of []);
  check "WV-3.9a-geen-half-dekkend-lid-blokkeert-niet"
    (kv_eval ~quantifier_kind:None_of [ ("B1", half_year "50") ]);

  (* a member with NO timeline at all touches no bucket and passes
     vacuously — ch. 3.9's own rule, lifted unchanged; it is NOT the same
     thing as partial coverage, and the difference is pinned here so an
     executor that collapses the two fails by name *)
  check "WV-3.9a-lid-zonder-tijdlijn-is-vacuous-waar"
    (kv_eval [ ("B1", whole_year "50"); ("B2", []) ]);
  (* and the cell where that vacuum CHANGES SIGN: the very same member
     SATISFIES its matrix vacuously, which is what `none` denies. Pinned on
     its OWN set, because beside a member at 50 the fold is already decided
     and the empty one is never reached — the two vacua (at the member, at
     the set) part company only here *)
  check "WV-3.9a-geen-enig-lid-zonder-tijdlijn-is-onwaar"
    (not (kv_eval ~quantifier_kind:None_of [ ("B1", []) ]));
  check "WV-3.9a-alle-enig-lid-zonder-tijdlijn-is-waar"
    (kv_eval [ ("B1", []) ]);

  (* the die/dat subselection selects WHICH members are quantified over; the
     excluded member's failing value never reaches the verdict *)
  let filter_above_10 : Ir_v0.Ast.condition_expr =
    Comparison
      { op = Greater; left_side = Some (aref "dagvergoeding" "Berger");
        right_side = Some (litv "10" num0); time_period = Some { gp_granularity = true; gp_negated = false };
        source_name = None }
  in
  check "WV-3.9a-filter-sluit-het-falende-lid-uit"
    (kv_eval ~filter:filter_above_10 [ ("B1", whole_year "50"); ("B2", whole_year "5") ]);
  check "WV-3.9a-filter-dat-alles-uitsluit-is-vacuous-waar"
    (kv_eval ~filter:filter_above_10 [ ("B1", whole_year "5") ]);

  (* the COUNTING quantifiers (ch. 3.9a): the verdict COUNTS the
     contributors whose matrix holds and compares that number with the
     threshold. `all` is [hits = n] and `none` is [hits = 0] written the other
     way round, so one comparison covers all five — and the empty set gets
     no vacuum rule of its own, it simply counts 0. Matrix throughout: the
     §8.2 test "dagvergoeding > 40 gedurende het gehele jaar". *)
  let tally k d member_list = kv_eval ~quantifier_kind:k ~cutoff:d member_list in

  (* each threshold from BOTH sides, on ONE pair of member sets: a count of
     2 sits exactly at at-least 2 and exceeds at-most 1 by exactly one; a
     count of 1 turns both over and satisfies exactly 1 *)
  let two_above = [ ("B1", whole_year "50"); ("B2", whole_year "60") ] in
  let one_above = [ ("B1", whole_year "50"); ("B2", whole_year "30") ] in
  check "WV-3.9a-tel-minstens2-telling2-op-de-drempel" (tally At_least 2 two_above);
  check "WV-3.9a-tel-minstens2-telling1-onder-de-drempel" (not (tally At_least 2 one_above));
  check "WV-3.9a-tel-hoogstens1-telling1-op-de-drempel" (tally At_most 1 one_above);
  check "WV-3.9a-tel-hoogstens1-telling2-boven-de-drempel" (not (tally At_most 1 two_above));
  check "WV-3.9a-tel-precies1-telling1" (tally Exactly 1 one_above);
  check "WV-3.9a-tel-precies1-telling2-is-onwaar" (not (tally Exactly 1 two_above));
  check "WV-3.9a-tel-precies1-telling0-is-onwaar"
    (not (tally Exactly 1 [ ("B1", whole_year "30"); ("B2", whole_year "20") ]));

  (* the EMPTY contributor set, per quantifier: it is the count, not a
     principle. `all` and `none` read waar there (pinned above); these three
     part company on the same set *)
  check "WV-3.9a-tel-lege-set-minstens1-is-onwaar" (not (tally At_least 1 []));
  check "WV-3.9a-tel-lege-set-hoogstens1-is-waar" (tally At_most 1 []);
  check "WV-3.9a-tel-lege-set-precies1-is-onwaar" (not (tally Exactly 1 []));

  (* ch. 5.10's EMPTY-PREFIX triggers, over a set instead of a written list:
     at-least above the contributor count is out of reach before the matrix
     is read once, and at-most at or above it can no longer be exceeded —
     both answer having read no member at all *)
  check "WV-3.9a-tel-minstens2-over-een-lid-is-buiten-bereik"
    (not (tally At_least 2 [ ("B1", whole_year "50") ]));
  check "WV-3.9a-tel-hoogstens2-over-een-lid-is-vacuous-waar"
    (tally At_most 2 [ ("B1", whole_year "50") ]);

  (* ch. 3.9's gap, unchanged under a counting quantifier: a member whose
     timeline does not cover the period fails the matrix, so it does not
     COUNT — the half-year member is the difference between 2 and 1 here *)
  check "WV-3.9a-tel-half-dekkend-lid-telt-niet-mee"
    (not (tally At_least 2 [ ("B1", whole_year "50"); ("B2", half_year "60") ]));

  (* filter and threshold together: the subselection decides WHICH members
     are counted, the threshold what the count must be *)
  check "WV-3.9a-tel-filter-en-drempel-telling2"
    (kv_eval ~quantifier_kind:At_least ~cutoff:2 ~filter:filter_above_10
       [ ("B1", whole_year "50"); ("B2", whole_year "60"); ("B3", whole_year "5") ]);
  check "WV-3.9a-tel-filter-laat-de-telling-onder-de-drempel-zakken"
    (not
       (kv_eval ~quantifier_kind:At_least ~cutoff:2 ~filter:filter_above_10
          [ ("B1", whole_year "50"); ("B2", whole_year "5"); ("B3", whole_year "5") ]));

  (* the NIET prefix under a counting quantifier. On the whole-period matrix
     the "niet" qualifies the OUTER comparison, and under `all`/`none` an
     executor may fold it by flipping the quantifier — a THRESHOLD has no
     quantifier to flip into, so the negation is read per contributor and
     lands on the COUNT: count(not P) = n - count(P), exact because the
     whole-period predicate is total. Each row is chosen so that counting P
     instead of not-P gives the other verdict. *)
  let denied = kv_test ~not_flag:true () in
  let not_count k d member_list = kv_eval ~quantifier_kind:k ~cutoff:d ~test:denied member_list in
  check "WV-3.9a-niet-minstens2-geen-lid-haalt-de-matrix"
    (not_count At_least 2 [ ("B1", whole_year "30"); ("B2", whole_year "20") ]);
  check "WV-3.9a-niet-hoogstens1-elk-lid-haalt-de-matrix"
    (not_count At_most 1 [ ("B1", whole_year "50"); ("B2", whole_year "60") ]);
  check "WV-3.9a-niet-precies1-twee-halen-hem-een-niet"
    (not_count Exactly 1
       [ ("B1", whole_year "50"); ("B2", whole_year "60"); ("B3", whole_year "30") ]);
  (* ch. 3.9's gap counts on the DENIED side; a member with NO timeline passes
     the matrix vacuously and therefore counts on the POSITIVE one *)
  check "WV-3.9a-niet-minstens1-het-half-dekkende-lid-telt-ontkend"
    (not_count At_least 1 [ ("B1", half_year "50") ]);
  check "WV-3.9a-niet-minstens1-lid-zonder-tijdlijn-telt-niet-ontkend"
    (not (not_count At_least 1 [ ("B1", []) ]));
  check "WV-3.9a-niet-minstens1-lege-set-blijft-nul-min-nul"
    (not (not_count At_least 1 []));

  (* a counting fold with no threshold has nothing to compare the count
     with — WF-108 (e)'s business, and reaching it in evaluation is a
     fault, never a verdict. `the` is arm (a)'s and faults the same way *)
  expect_fault_msg "WV-3.9a-tel-kwantorvouw-zonder-drempel-faalt"
    (fun () ->
      Value.Bool (kv_eval ~quantifier_kind:At_least [ ("B1", whole_year "50") ]))
    "counting quantifier fold without a threshold: the verdict compares the number of \
     contributors satisfying the matrix with a threshold, and there is none to compare it with";
  expect_fault_msg "WV-3.9a-kwantorvouw-met-kwantor-DE-faalt"
    (fun () -> Value.Bool (kv_eval ~quantifier_kind:The [ ("B1", whole_year "50") ]))
    "quantifier fold with quantifier the: the bare definite is a description of ONE member, not \
     a quantification over the set — a fold over it has no reading";

  (* a matrix that is absent is WF-108's business, and reaching it in
     evaluation is a fault, never a verdict *)
  expect_fault_msg "WV-3.9a-matrixloze-kwantorvouw-faalt"
    (fun () ->
      let w = kv_world [ ("B1", whole_year "50") ] in
      Value.Bool
        (Eval.eval_cond (env_of kv_doc) w (World.get w "P1") ~err:no_err
           (Quantifier_fold
              { kv_quantifier = All; kv_threshold = None; kv_element_object_type = Some "Berger";
                kv_filter = None;
                kv_test = None; kv_source = None;
                kv_source_role = Some { rb_fact_type = "ploegindeling"; rb_role = "berger" };
                kv_source_path = None })))
    "quantifier fold without a test: the matrix is what every contributor must satisfy, and a \
     quantifier over no condition has no verdict";
  (* WF-108 (c), the range's shape: one spelling and a path of at least two
     steps. The loader and the verifier refuse the wire forms first; a node
     built past them is refused in their sentences *)
  let rb : Ir_v0.Ast.role_binding = { rb_fact_type = "ploegindeling"; rb_role = "berger" } in
  let range_fails ident role_ref trail sentence_text =
    expect_fault_msg ident
      (fun () ->
        let w = kv_world [ ("B1", whole_year "50") ] in
        Value.Bool
          (Eval.eval_cond (env_of kv_doc) w (World.get w "P1") ~err:no_err
             (match kv_cond () with
             | Quantifier_fold k -> Quantifier_fold { k with kv_source_role = role_ref; kv_source_path = trail }
             | c -> c)))
      sentence_text
  in
  range_fails "WV-3.9a-kwantorvouw-met-twee-bereiken-faalt" (Some rb) (Some [ rb; rb ])
    "sourcePath beside sourceRole: a quantifier fold carries one range, never two";
  range_fails "WV-3.9a-kwantorvouw-zonder-bereik-faalt" None None
    "a quantifier fold carries its range (sourceRole or sourcePath): the range is written, never \
     derived";
  range_fails "WV-3.9a-kwantorvouw-met-een-pad-van-een-stap-faalt" None (Some [ rb ])
    "a quantifier fold's sourcePath carries at least TWO steps, found 1 — the one-step range is \
     spelled sourceRole"

(* The SCALAR whole-period comparison the matrix rests on (ch. 3.9): the
   same four rows the characteristic form already pins, over a VALUE
   timeline. Until this card both OCaml executors refused the form by name
   while ch. 3.9 specified it — the gap ch. 3.12 recorded. *)
let () =
  let scalar_doc =
    { empty_doc with object_types = [ (fst kv_berger, snd kv_berger) ] }
  in
  let ev ?(op : Ir_v0.Ast.compare_op = Greater) ?(not_flag = false) ?(year = true) ?(boundary = "40") tl =
    let i =
      { (inst "B1" "Berger" []) with
        World.tls = World.SMap.add "dagvergoeding" tl World.SMap.empty }
    in
    let w = World.add_instance World.empty i in
    Eval.eval_cond (env_of scalar_doc) w (World.get w "B1") ~err:no_err
      (Comparison
         { op; left_side = Some (aref "dagvergoeding" "Berger");
           right_side = Some (litv boundary num0);
           time_period = Some { gp_granularity = year; gp_negated = not_flag }; source_name = None })
  in
  check "WV-3.9-vgl-a-geen-tijdlijn-raakt-geen-emmer" (ev []);
  check "WV-3.9-vgl-b-heel-jaar-boven-de-grens" (ev (whole_year "50"));
  check "WV-3.9-vgl-c-heel-jaar-onder-de-grens" (not (ev (whole_year "30")));
  check "WV-3.9-vgl-d-half-jaar-boven-de-grens" (not (ev (half_year "50")));
  check "WV-3.9-vgl-e-precies-op-de-grens" (not (ev (whole_year "40")));
  check "WV-3.9-vgl-f-groter-of-gelijk-op-de-grens"
    (ev ~op:Greater_or_equal (whole_year "40"));
  (* the QUALIFIER's own negation complements the verdict, the vacuous rows
     included — exactly as on the characteristic form *)
  check "WV-3.9-vgl-niet-a" (not (ev ~not_flag:true []));
  check "WV-3.9-vgl-niet-b" (not (ev ~not_flag:true (whole_year "50")));
  check "WV-3.9-vgl-niet-d" (ev ~not_flag:true (half_year "50"));
  (* the grid is the QUALIFIER's own: six whole months pass where the year
     bucket failed *)
  check "WV-3.9-vgl-maand-d" (ev ~year:false (half_year "50"));
  check "WV-3.9-vgl-maand-deelmaand"
    (not (ev ~year:false [ seg "2026-01-15" "2026-03-01" (n "50") ]));
  (* a leeg segment inside the period is a day that does not satisfy the
     test — false, never a fault (ch. 3.9) *)
  check "WV-3.9-vgl-leeg-segment-faalt"
    (not (ev [ seg "2026-01-01" "2026-07-01" (n "50"); seg "2026-07-01" "2027-01-01" empty_value ]));
  (* an EXPLICIT leeg segment reads as a gap (ch. 3.1) for a comparison too,
     as for the leeg and gevuld tests: it touches no bucket, and a day under
     it satisfies no test, the inequality included (ch. 3.9, decided
     2026-09-26). Between two filled years it leaves only those years
     touched; after a half year of 50 it leaves the second half unsatisfied
     for "≠ 40" as a gap does; alone it touches nothing. *)
  let between = [ seg "2025-01-01" "2026-01-01" (n "50"); seg "2026-01-01" "2027-01-01" empty_value;
                 seg "2027-01-01" "2028-01-01" (n "50") ] in
  check "WV-3.9-vgl-leeg-segment-raakt-geen-emmer" (ev between);
  check "WV-3.9-vgl-leeg-segment-raakt-geen-emmer-ongelijk" (ev ~op:Not_equal between);
  check "WV-3.9-vgl-leeg-segment-ongelijk-is-een-gat"
    (not (ev ~op:Not_equal [ seg "2026-01-01" "2026-07-01" (n "50"); seg "2026-07-01" "2027-01-01" empty_value ]));
  check "WV-3.9-vgl-leeg-segment-alleen" (ev [ seg "2026-01-01" "2027-01-01" empty_value ]);
  (* a non-time-dependent operand keeps WF-043's refusal *)
  expect_fault_msg "WV-3.9-vgl-niet-tijdsafhankelijk"
    (fun () ->
      let flat = ot "Berger" [ ("dagvergoeding", Some num0) ] [] in
      let d = { empty_doc with object_types = [ flat ] } in
      let w = World.add_instance World.empty (inst "B1" "Berger" [ ("dagvergoeding", n "50") ]) in
      Value.Bool
        (Eval.eval_cond (env_of d) w (World.get w "B1") ~err:no_err
           (Comparison
              { op = Greater; left_side = Some (aref "dagvergoeding" "Berger");
                right_side = Some (litv "40" num0);
                time_period = Some { gp_granularity = true; gp_negated = false }; source_name = None })))
    "whole-period qualifier on a comparison whose operands are not a time-dependent attribute"

(* ch. 3.9, the LEEG and GEVULD tests under the whole-period qualifier
   (decided 2026-09-26), the chapter's worked-example table verbatim: one
   threshold, days(B), for both tests; a day that reads leeg satisfies the
   leeg test and fails the gevuld test; the inner negation per day, so the
   negated leeg test IS the gevuld test; the qualifier's own negation on
   the folded verdict. Schuit.bemanning and Werf.havengeld are
   month-granular, and the qualifier's grid is the year unless a row says
   month. Until that date this arm faulted by name on every form below. *)
let () =
  let ligplaats : string * Ir_v0.Ast.fact_type_name =
    ( "ligplaats",
      {
        ft_name = Some "ligplaats";
        reciprocal = false;
        role_names =
          [
            { role_identifier = Some "werf"; role_object_type_name = Some "Werf"; role_plural = None;
              role_many = false };
            { role_identifier = Some "schuit"; role_object_type_name = Some "Schuit";
              role_plural = Some "schuiten"; role_many = true };
          ];
      } )
  in
  let monthly ident (otn, (o : Ir_v0.Ast.object_type)) =
    (otn, { o with attribute_granularity = [ (ident, Some Ir_v0.Ast.Month) ] })
  in
  let doc =
    { empty_doc with
      object_types =
        [ monthly "bemanning" (ot "Schuit" [ ("bemanning", Some num0) ] []);
          monthly "havengeld" (ot "Werf" [ ("havengeld", Some num0) ] []) ];
      fact_types = [ ligplaats ] }
  in
  let test ?(variety : Ir_v0.Ast.predicate_kind_type = Empty_predicate) ?(negation = false) ?(year = true)
      ?(not_flag = false) operand : Ir_v0.Ast.condition_expr =
    Predicate
      { variety; operand = Some operand; day_kind = None; negation; digit_chars = 0;
        time_period = Some { gp_granularity = year; gp_negated = not_flag }; source_name = None }
  in
  let with_timeline ident tl (i : World.instance) =
    match tl with [] -> i | _ -> { i with World.tls = World.SMap.add ident tl World.SMap.empty }
  in
  (* the own operand: S1's bemanning *)
  let ev ?variety ?negation ?year ?not_flag tl =
    let w = World.add_instance World.empty (with_timeline "bemanning" tl (inst "S1" "Schuit" [])) in
    Eval.eval_cond (env_of doc) w (World.get w "S1") ~err:no_err
      (test ?variety ?negation ?year ?not_flag (aref "bemanning" "Schuit"))
  in
  let four = n "4" in
  let row_b = [ seg "2025-01-01" "2026-01-01" four ] in
  let row_c = [ seg "2025-01-01" "2025-07-01" four ] in
  let row_d = [ seg "2025-01-01" "2025-07-01" four; seg "2025-07-01" "2027-01-01" (n "5") ] in
  (* the table: columns leeg, gevuld, niet leeg, niet gevuld, niet gedurende … leeg *)
  let table_row ident tl (empty_, populated, not_empty, not_filled, not_during) =
    check ("WV-3.9-leeg-" ^ ident ^ "-leeg") (ev tl = empty_);
    check ("WV-3.9-leeg-" ^ ident ^ "-gevuld") (ev ~variety:Filled tl = populated);
    check ("WV-3.9-leeg-" ^ ident ^ "-niet-leeg") (ev ~negation:true tl = not_empty);
    check ("WV-3.9-leeg-" ^ ident ^ "-niet-gevuld") (ev ~variety:Filled ~negation:true tl = not_filled);
    check ("WV-3.9-leeg-" ^ ident ^ "-niet-gedurende") (ev ~not_flag:true tl = not_during)
  in
  table_row "a-geen-segmenten" [] (true, true, true, true, false);
  table_row "b-heel-jaar" row_b (false, true, true, false, true);
  table_row "c-half-jaar" row_c (false, false, false, false, true);
  table_row "d-twee-segmenten-twee-jaar" row_d (false, true, true, false, true);
  (* the MONTH grid: the qualifier's own, never the declared granularity *)
  check "WV-3.9-leeg-maand-c-gevuld" (ev ~variety:Filled ~year:false row_c);
  let over_year_boundary = [ seg "2025-07-01" "2026-07-01" four ] in
  check "WV-3.9-leeg-jaargrens-jaar-gevuld" (not (ev ~variety:Filled over_year_boundary));
  check "WV-3.9-leeg-jaargrens-maand-gevuld" (ev ~variety:Filled ~year:false over_year_boundary);
  let half_january = [ seg "2025-01-15" "2025-03-01" four ] in
  check "WV-3.9-leeg-deelmaand-jaar-gevuld" (not (ev ~variety:Filled half_january));
  check "WV-3.9-leeg-deelmaand-maand-gevuld" (not (ev ~variety:Filled ~year:false half_january));
  check "WV-3.9-leeg-deelmaand-jaar-leeg" (not (ev half_january));
  check "WV-3.9-leeg-deelmaand-maand-leeg" (not (ev ~year:false half_january));
  (* an EXPLICIT leeg segment reads as a gap (ch. 3.1) and touches no bucket:
     alone it reads as row a, after a half year of 4 as row c *)
  let only_empty = [ seg "2025-01-01" "2026-01-01" empty_value ] in
  check "WV-3.9-leeg-expliciet-alleen-leeg" (ev only_empty);
  check "WV-3.9-leeg-expliciet-alleen-gevuld" (ev ~variety:Filled only_empty);
  let half_and_empty = [ seg "2025-01-01" "2025-07-01" four; seg "2025-07-01" "2026-01-01" empty_value ] in
  check "WV-3.9-leeg-expliciet-half-leeg" (not (ev half_and_empty));
  check "WV-3.9-leeg-expliciet-half-gevuld" (not (ev ~variety:Filled half_and_empty));
  (* the NAVIGATED operand, one to-one hop: a hop that reaches nothing gives
     the null timeline, which passes both tests *)
  let havengeld_werf : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some "havengeld"; object_type = Some "Werf"; source_name = None;
        navigation_bindings = Some [ { rb_fact_type = "ligplaats"; rb_role = "werf" } ] }
  in
  let ev_nav ?variety ?not_flag ~link tl =
    let w1 = with_timeline "havengeld" tl (inst "W1" "Werf" []) in
    let s1 =
      { (inst "S1" "Schuit" []) with
        left_side = (if link then World.SMap.add "werf" [ "W1" ] World.SMap.empty else World.SMap.empty) }
    in
    let w = List.fold_left World.add_instance World.empty [ w1; s1 ] in
    Eval.eval_cond (env_of doc) w (World.get w "S1") ~err:no_err
      (test ?variety ?not_flag havengeld_werf)
  in
  check "WV-3.9-leeg-nav-geen-koppeling-gevuld" (ev_nav ~variety:Filled ~link:false row_b);
  check "WV-3.9-leeg-nav-geen-koppeling-leeg" (ev_nav ~link:false row_b);
  check "WV-3.9-leeg-nav-geen-koppeling-niet-gedurende"
    (not (ev_nav ~variety:Filled ~not_flag:true ~link:false row_b));
  check "WV-3.9-leeg-nav-heel-jaar-gevuld" (ev_nav ~variety:Filled ~link:true row_b);
  check "WV-3.9-leeg-nav-heel-jaar-leeg" (not (ev_nav ~link:true row_b));
  check "WV-3.9-leeg-nav-half-jaar-gevuld" (not (ev_nav ~variety:Filled ~link:true row_c));
  (* a CHAIN of two to-one hops (WF-053 admits one): Schuit -> Werf over
     ligplaats, Werf -> Haven over havenligging. An absent link at either hop
     gives the null timeline. *)
  let havenligging : string * Ir_v0.Ast.fact_type_name =
    ( "havenligging",
      {
        ft_name = Some "havenligging";
        reciprocal = false;
        role_names =
          [
            { role_identifier = Some "thuishaven"; role_object_type_name = Some "Haven"; role_plural = None;
              role_many = false };
            { role_identifier = Some "aangesloten werf"; role_object_type_name = Some "Werf";
              role_plural = Some "aangesloten werven"; role_many = true };
          ];
      } )
  in
  let chain_doc =
    { doc with
      object_types = doc.object_types @ [ monthly "havengeld" (ot "Haven" [ ("havengeld", Some num0) ] []) ];
      fact_types = [ ligplaats; havenligging ] }
  in
  let havengeld_thuishaven : Ir_v0.Ast.expr =
    Attribute_reference
      { attribute_ref = Some "havengeld"; object_type = Some "Haven"; source_name = None;
        navigation_bindings =
          Some
            [ { rb_fact_type = "ligplaats"; rb_role = "werf" };
              { rb_fact_type = "havenligging"; rb_role = "thuishaven" } ] }
  in
  let ev_chain ?variety ~haven tl =
    let h1 = with_timeline "havengeld" tl (inst "H1" "Haven" []) in
    let w1 =
      { (inst "W1" "Werf" []) with
        left_side =
          (if haven then World.SMap.add "thuishaven" [ "H1" ] World.SMap.empty else World.SMap.empty) }
    in
    let s1 = { (inst "S1" "Schuit" []) with left_side = World.SMap.add "werf" [ "W1" ] World.SMap.empty } in
    let w = List.fold_left World.add_instance World.empty [ h1; w1; s1 ] in
    Eval.eval_cond (env_of chain_doc) w (World.get w "S1") ~err:no_err
      (test ?variety havengeld_thuishaven)
  in
  check "WV-3.9-leeg-keten-heel-jaar-gevuld" (ev_chain ~variety:Filled ~haven:true row_b);
  check "WV-3.9-leeg-keten-heel-jaar-leeg" (not (ev_chain ~haven:true row_b));
  check "WV-3.9-leeg-keten-half-jaar-gevuld" (not (ev_chain ~variety:Filled ~haven:true row_c));
  check "WV-3.9-leeg-keten-geen-haven-gevuld" (ev_chain ~variety:Filled ~haven:false row_c);
  (* the operands no executor folds, refused by name: a parameter (WF-043
     admits a time-dependent one), and a non-temporal attribute (WF-043's
     own refusal, kept as a belt in the verifier's sentence) *)
  expect_fault_msg "WV-3.9-leeg-parameter-operand"
    (fun () ->
      let w = World.add_instance World.empty (inst "S1" "Schuit" []) in
      Value.Bool
        (Eval.eval_cond (env_of doc) w (World.get w "S1") ~err:no_err
           (test (Parameter_ref { parameter = Some "havengeldtarief"; source_name = None }))))
    "a whole-period leeg/gevuld test over the parameter 'havengeldtarief' is not executed: the fold \
     reads the timeline of an attribute";
  expect_fault_msg "WV-3.9-leeg-niet-tijdsafhankelijk"
    (fun () ->
      let simple = { empty_doc with object_types = [ ot "Schuit" [ ("bemanning", Some num0) ] [] ] } in
      let w = World.add_instance World.empty (inst "S1" "Schuit" [ ("bemanning", n "4") ]) in
      Value.Bool
        (Eval.eval_cond (env_of simple) w (World.get w "S1") ~err:no_err
           (test ~variety:Filled (aref "bemanning" "Schuit"))))
    "wholePeriod qualifier on a non-time-dependent operand"

(* WV-3.10-geldigheid-datumlezing: the validity gate reads its three dates —
   the case's asOfDate and the form's validity.from / .to — through the
   strict date reader, and refuses a non-date by position. Until 2026-09-23
   it compared the raw strings: asOfDate "01-06-2026" sorted before every
   20xx bound and a day-first bound moved its window, each answering
   silently (jutterswet-vensterheffing: overgangsheffing 140 where leeg is
   right). No corpus document can pin this: WF-101 refuses a malformed bound
   at gate 3, and the case schema's pattern refuses a day-first asOfDate at
   gate 4 — only a calendar-invalid asOfDate like "2026-13-01" passes the
   schema, and that one is pinned here at the loader. *)
let () =
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let doc starting_at up_to =
    { empty_doc with
      object_types = [ ot "Melding" [ ("heffing", Some amount) ] [] ];
      resolved_rules =
        [ { (rr "heffing in 2026" Calculation (Some (aref "heffing" "Melding"))
               (Some (litv "125" amount)) None)
            with rr_validity = Some { valid_from = starting_at; valid_to = up_to } } ] }
  in
  let run ?(as_of = "2026-06-01") starting_at up_to =
    let w =
      Eval.evaluate
        { (env_of (doc starting_at up_to)) with as_of = Some as_of }
        (World.add_instance World.empty (inst "m" "Melding" []))
    in
    World.attr (World.get w "m") "heffing"
  in
  let year = (Some "2026-01-01", Some "2026-12-31") in
  (* the canonical window still decides by calendar, both bounds inclusive *)
  expect_num "WV-3.10-geldigheid-binnen-venster" (run (fst year) (snd year)) "125";
  expect_empty "WV-3.10-geldigheid-voor-venster" (run ~as_of:"2025-06-01" (fst year) (snd year));
  expect_num "WV-3.10-geldigheid-vanaf-inclusief" (run ~as_of:"2026-01-01" (fst year) (snd year)) "125";
  expect_empty "WV-3.10-geldigheid-dag-voor-vanaf" (run ~as_of:"2025-12-31" (fst year) (snd year));
  expect_num "WV-3.10-geldigheid-tot-inclusief" (run ~as_of:"2026-12-31" (fst year) (snd year)) "125";
  expect_empty "WV-3.10-geldigheid-na-venster" (run ~as_of:"2027-01-01" (fst year) (snd year));
  (* The WHOLE refusal sentence is pinned, reader's reason included: the
     clingo suite pins the same sentences, which is what makes "one refusal
     for both executors" a witnessed claim rather than a shared prefix. *)
  let sentence_text what lex why =
    Printf.sprintf "validity gate: %s %S is not the ISO date lexeme yyyy-mm-dd (%s)" what lex why
  in
  let form_kind lex = Printf.sprintf "malformed date lexeme %S (expected ISO yyyy-mm-dd)" lex in
  let month_kind lex = Printf.sprintf "month out of range in date lexeme %S" lex in
  let day_number lex = Printf.sprintf "day out of range in date lexeme %S" lex in
  let rejects ident f expected_value =
    match f () with
    | exception Value.Fault { text = m; _ } -> check (ident ^ " (got: " ^ m ^ ")") (m = expected_value)
    | _ -> check (ident ^ " (no fault at all)") false
  in
  (* position vanaf: day-first sorted before "2026-06-01", so the window
     opened; month 13 sorted after, so it stayed shut *)
  rejects "WV-3.10-geldigheid-vanaf-dag-eerst-weigert"
    (fun () -> run (Some "01-01-2026") (snd year))
    (sentence_text "validity.from" "01-01-2026" (form_kind "01-01-2026"));
  rejects "WV-3.10-geldigheid-vanaf-kalender-weigert"
    (fun () -> run (Some "2026-13-01") None)
    (sentence_text "validity.from" "2026-13-01" (month_kind "2026-13-01"));
  (* the month-length half of the calendar: 30 February has the shape and
     even sorts correctly, and only the reader's month length refuses it *)
  rejects "WV-3.10-geldigheid-vanaf-maandlengte-weigert"
    (fun () -> run (Some "2026-02-30") None)
    (sentence_text "validity.from" "2026-02-30" (day_number "2026-02-30"));
  (* position tot: "31-12-2025" sorted before "2026-06-01", so the window
     closed; a day-first tot that sorts AFTER the date kept it open *)
  rejects "WV-3.10-geldigheid-tot-dag-eerst-weigert"
    (fun () -> run (fst year) (Some "31-12-2026"))
    (sentence_text "validity.to" "31-12-2026" (form_kind "31-12-2026"));
  rejects "WV-3.10-geldigheid-tot-kalender-weigert"
    (fun () -> run None (Some "2026-12-32"))
    (sentence_text "validity.to" "2026-12-32" (day_number "2026-12-32"));
  (* a malformed bound is refused even where the other bound already
     excludes the date, in both directions: neither reading sits behind
     the other bound's short-circuit *)
  rejects "WV-3.10-geldigheid-tot-weigert-ook-buiten-vanaf"
    (fun () -> run ~as_of:"2025-06-01" (fst year) (Some "31-12-2026"))
    (sentence_text "validity.to" "31-12-2026" (form_kind "31-12-2026"));
  rejects "WV-3.10-geldigheid-vanaf-weigert-ook-buiten-tot"
    (fun () -> run ~as_of:"2027-06-01" (Some "01-01-2026") (snd year))
    (sentence_text "validity.from" "01-01-2026" (form_kind "01-01-2026"));
  (* position asOfDate, at the gate: an env built without the case loader *)
  rejects "WV-3.10-geldigheid-asofdate-dag-eerst-weigert"
    (fun () -> run ~as_of:"01-06-2026" (fst year) (snd year))
    (sentence_text "asOfDate" "01-06-2026" (form_kind "01-06-2026"));
  (* position asOfDate, at the case loader — the one ref and clingo share,
     so the refusal comes before either executor runs *)
  let load as_of =
    let f = Filename.temp_file "ref-asofdate" ".case.json" in
    let oc = open_out f in
    Printf.fprintf oc
      {|{ "format": "uchar-case-v0", "asOfDate": "%s",
          "input": { "instances": [], "parameters": {} },
          "expected": [ { "kind": "consistent" } ] }|}
      as_of;
    close_out oc;
    let r = try Ok (Case.load_file empty_doc f) with Case.Error { text = m; _ } -> Error m in
    Sys.remove f;
    r
  in
  check "WV-3.10-caselader-neemt-canonieke-asofdate"
    (match load "2026-06-01" with Ok c -> c.as_of = Some "2026-06-01" | Error _ -> false);
  List.iter
    (fun (ident, lex, why) ->
      let expected_value =
        Printf.sprintf "asOfDate %S is not the uchar-case-v0 date lexeme yyyy-mm-dd (%s)" lex why
      in
      match load lex with
      | Error m -> check (ident ^ " (got: " ^ m ^ ")") (m = expected_value)
      | Ok _ -> check (ident ^ " (loaded)") false)
    [ ("WV-3.10-caselader-weigert-dag-eerst-asofdate", "01-06-2026", form_kind "01-06-2026");
      ("WV-3.10-caselader-weigert-maand-13-asofdate", "2026-13-01", month_kind "2026-13-01");
      (* the pattern-admitted calendar hole: 29 February outside a leap year *)
      ("WV-3.10-caselader-weigert-29-februari-asofdate", "2025-02-29", day_number "2025-02-29") ]

(* The case loader's lexeme refusals: every date, dateTime and number lexeme
   a case carries is read AT LOAD, and a lexeme the reader refuses is a
   Case.Error naming the position, the lexeme and the reason — the runners
   print it as CASE ERROR, exit 2, before either executor runs. Until
   2026-09-23 the date reader's Value.Fault passed the loader unconverted
   (the runners called a day-first input date an EVALUATION FAULT, clingo a
   TRANSLATION FAULT, with no field in it — measured on
   jutterswet-bergingsklok), a dateTime refusal named no field, and a number
   went through the IR literal grammar (WF-081) instead of the case wire's:
   "1,000" loaded as 1 without a word. Gate 4 checks the lexical pattern
   only: the calendar half passes it ("2026-02-30" the date pattern, an hour
   24 the dateTime one), and a validator that reads `$` the Python way also
   lets a trailing newline through. So the positions are pinned here,
   through the loader both executors share. The WHOLE sentence is pinned per
   position. The asOfDate sentence these share their shape with is pinned
   above, by the WV-3.10-caselader examples. *)
let () =
  let date_value : Ir_v0.Ast.datatype = Date_in_days in
  let amount : Ir_v0.Ast.datatype =
    Number { whole_part = false; decimal_places = 2; sign_char = None; measure_unit = None }
  in
  let vondst =
    let ident, o =
      ot "Vondst"
        [ ("vondstdag", Some date_value); ("meldstip", Some Ir_v0.Ast.Date_time_type);
          ("vangstdagen", Some (Ir_v0.Ast.List_type { element = date_value }));
          ("dossier", Some (Ir_v0.Ast.Struct_type { domain_spec = "Dossier"; field_list = [ ("peildag", date_value) ] }));
          ( "status",
            Some
              (Ir_v0.Ast.Sum_type
                 { domain_spec = "Vondststatus"; constructor_list = [ ("Gepland", Some date_value); ("Open", None) ] }) );
          ("gewicht", Some amount); ("tarief", Some amount); ("ijkdag", Some date_value);
          ("gemeld", Some Ir_v0.Ast.Boolean) ]
        [ "dienstdoend" ]
    in
    ( ident,
      { o with
        attribute_granularity = [ ("tarief", Some Ir_v0.Ast.Month); ("ijkdag", Some Ir_v0.Ast.Month) ];
        characteristic_granularity = [ ("dienstdoend", Some Ir_v0.Ast.Day) ] } )
  in
  let par ident dt gran =
    (ident, { Ir_v0.Ast.par_name = Some ident; par_datatype = Some dt; par_granularity = gran;
             par_timeline_count = Option.map (fun _ -> 1) gran; par_fixed_start_point = None })
  in
  let doc =
    { empty_doc with
      object_types = [ vondst ];
      parameters =
        [ par "peildatum" date_value None; par "peilstip" Ir_v0.Ast.Date_time_type None; par "heffing" amount None;
          par "tariefreeks" amount (Some Ir_v0.Ast.Year); par "ijkreeks" date_value (Some Ir_v0.Ast.Year) ] }
  in
  (* one lexeme per {slot}; every position a lexeme can sit in, once *)
  let template =
    {|{ "format": "uchar-case-v0", "asOfDate": "2026-06-01",
  "input": {
    "instances": [ { "label": "V1", "objectType": "Vondst",
      "attributes": {
        "vondstdag": { "date": "{vondstdag}" },
        "meldstip": { "dateTime": "{meldstip}" },
        "vangstdagen": { "list": [ { "date": "2026-03-09" }, { "date": "{vangstdag}" } ] },
        "dossier": { "struct": { "peildag": { "date": "{peildag}" } } },
        "status": { "constructor": "Gepland", "payload": { "date": "{gepland}" } },
        "gewicht": { "number": "{gewicht}" },
        "tarief": { "timeline": [
          { "from": "{tarief-van}", "to": "2026-02-01", "value": { "number": "{tarief-waarde}" } },
          { "from": "2026-02-01", "to": "{tarief-tot}", "value": { "number": "6" } } ] },
        "ijkdag": { "timeline": [
          { "from": "2026-01-01", "to": "2026-02-01", "value": { "date": "{ijkdag-waarde}" } } ] } },
      "roles": {}, "characteristics": [],
      "characteristicTimelines": { "dienstdoend": [
        { "from": "2026-01-01", "to": "2026-02-01" },
        { "from": "{dienst-van}", "to": "{dienst-tot}" } ] } } ],
    "parameters": {
      "peildatum": { "date": "{peildatum}" },
      "peilstip": { "dateTime": "{peilstip}" },
      "heffing": { "number": "{heffing}" },
      "tariefreeks": { "timeline": [
        { "from": "{reeks-van}", "to": "{reeks-tot}", "value": { "number": "7" } } ] },
      "ijkreeks": { "timeline": [
        { "from": "2026-01-01", "to": "2027-01-01", "value": { "date": "{ijkreeks-waarde}" } } ] } } },
  "expected": [
    { "kind": "attributeValue", "label": "V1", "attribute": "vondstdag",
      "value": { "date": "{verwacht-vondstdag}" } },
    { "kind": "attributeValue", "label": "V1", "attribute": "meldstip",
      "value": { "dateTime": "{verwacht-meldstip}" } },
    { "kind": "attributeValue", "label": "V1", "attribute": "gewicht",
      "value": { "number": "{verwacht-gewicht}" } },
    { "kind": "attributeTimeline", "label": "V1", "attribute": "tarief", "timeline": [
      { "from": "{verwacht-van}", "to": "{verwacht-tot}", "value": { "number": "5" } } ] },
    { "kind": "consistent" } ] }|}
  in
  let canonical_form =
    [ ("vondstdag", "2026-03-10"); ("meldstip", "2026-03-10T09:00:00"); ("vangstdag", "2026-03-11");
      ("peildag", "2026-03-10"); ("gepland", "2026-03-10"); ("gewicht", "-12.50");
      ("tarief-van", "2026-01-01"); ("tarief-waarde", "5"); ("tarief-tot", "2026-03-01");
      ("ijkdag-waarde", "2026-01-15"); ("dienst-van", "2026-03-01"); ("dienst-tot", "2026-04-16");
      ("peildatum", "2026-03-10"); ("peilstip", "2026-03-10T09:00:00.250"); ("heffing", "3/4");
      ("reeks-van", "2026-01-01"); ("reeks-tot", "2027-01-01"); ("ijkreeks-waarde", "2026-01-15");
      ("verwacht-vondstdag", "2026-03-10"); ("verwacht-meldstip", "2026-03-10T09:00:00");
      ("verwacht-gewicht", "12.50"); ("verwacht-van", "2026-01-01"); ("verwacht-tot", "2026-02-01") ]
  in
  (* A slot that does not occur fails: the text would pass through unchanged
     and the witness would pin a different case than its name says. *)
  let substitute ~sub ~via s =
    let b = Buffer.create (String.length s) and n = String.length sub and hits_list = ref 0 in
    let rec go i =
      if i > String.length s - n then Buffer.add_string b (String.sub s i (String.length s - i))
      else if String.sub s i n = sub then (Buffer.add_string b via; incr hits_list; go (i + n))
      else (Buffer.add_char b s.[i]; go (i + 1))
    in
    go 0;
    if !hits_list = 0 then failwith (Printf.sprintf "vervang: %S does not occur" sub);
    Buffer.contents b
  in
  check "replace fails on a slot that does not occur"
    (match substitute ~sub:"{geen-slot}" ~via:"x" template with
    | (_ : string) -> false
    | exception Failure _ -> true);
  let case_with slot lex =
    if slot <> "" && not (List.mem_assoc slot canonical_form) then invalid_arg ("unknown slot " ^ slot);
    List.fold_left
      (fun t (k, v) -> substitute ~sub:("{" ^ k ^ "}") ~via:(if k = slot then lex else v) t)
      template canonical_form
  in
  let load_in d body_text =
    let f = Filename.temp_file "ref-caselexeem" ".case.json" in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    let r =
      match Case.load_file d f with
      | c -> `Loaded c
      | exception Case.Error { text = m; _ } -> `Refused m
      | exception Value.Fault { text = m; _ } -> `Fault m
    in
    Sys.remove f;
    r
  in
  let load_doc = load_in doc in
  let refuses_text ?(d = doc) ident body_text expected_value =
    match load_in d body_text with
    | `Refused m -> check (ident ^ " (got: " ^ m ^ ")") (m = expected_value)
    | `Fault m -> check (ident ^ " (escaped the loader as Value.Fault: " ^ m ^ ")") false
    | `Loaded _ -> check (ident ^ " (loaded)") false
  in
  let rejects ident slot lex expected_value = refuses_text ident (case_with slot lex) expected_value in
  (* the over-rejection guard: every canonical form loads and reads as written *)
  (match load_doc (case_with "" "") with
  | `Loaded c ->
      let v1 = World.get c.world "V1" in
      check "case-loader-lexeme-canonical-loads" true;
      check "case-loader-lexeme-canonical-date"
        (World.attr v1 "vondstdag" = Value.date_of_iso "2026-03-10");
      check "case-loader-lexeme-canonical-negative-decimal"
        (match World.attr v1 "gewicht" with Value.Num (x, _) -> Q.equal x (q "-25/2") | _ -> false);
      check "case-loader-lexeme-canonical-rational"
        (match World.SMap.find_opt "heffing" c.params with
         | Some (Value.Num (x, _)) -> Q.equal x (q "3/4")
         | _ -> false);
      check "case-loader-lexeme-canonical-date-time"
        (World.SMap.find_opt "peilstip" c.params
        = Some (Value.Date_time (Case.ms_of_datetime "2026-03-10T09:00:00.250")))
  | `Refused m -> check ("case-loader-lexeme-canonical-loads (refused: " ^ m ^ ")") false
  | `Fault m -> check ("case-loader-lexeme-canonical-loads (fault: " ^ m ^ ")") false);
  let d at lex why =
    Printf.sprintf "%s %S is not the uchar-case-v0 date lexeme yyyy-mm-dd (%s)" at lex why
  in
  let form_kind lex = Printf.sprintf "malformed date lexeme %S (expected ISO yyyy-mm-dd)" lex in
  let month_kind lex = Printf.sprintf "month out of range in date lexeme %S" lex in
  let day_number lex = Printf.sprintf "day out of range in date lexeme %S" lex in
  let dm = "10-03-2026" in
  (* date: every position, day-first *)
  List.iter
    (fun (ident, slot, at) -> rejects ("case-loader-lexeme-" ^ ident) slot dm (d at dm (form_kind dm)))
    [ ("attribuut-datum", "vondstdag", "V1.vondstdag");
      ("lijstelement-datum", "vangstdag", "V1.vangstdagen[1]");
      ("structveld-datum", "peildag", "V1.dossier.peildag");
      ("constructorpayload-datum", "gepland", "V1.status.payload");
      ("attribuuttijdlijn-van", "tarief-van", "V1.tarief.timeline[0].from");
      ("attribuuttijdlijn-tot", "tarief-tot", "V1.tarief.timeline[1].to");
      ("attribuuttijdlijn-waarde-datum", "ijkdag-waarde", "V1.ijkdag.timeline[0].value");
      ("kenmerktijdlijn-van", "dienst-van", "V1.dienstdoend[1].from");
      ("kenmerktijdlijn-tot", "dienst-tot", "V1.dienstdoend[1].to");
      ("parameter-datum", "peildatum", "parameter peildatum");
      ("parametertijdlijn-van", "reeks-van", "parameter tariefreeks.timeline[0].from");
      ("parametertijdlijn-tot", "reeks-tot", "parameter tariefreeks.timeline[0].to");
      ("parametertijdlijn-waarde-datum", "ijkreeks-waarde", "parameter ijkreeks.timeline[0].value");
      ("verwachte-waarde-datum", "verwacht-vondstdag", "expected V1.vondstdag");
      ("verwachte-tijdlijn-van", "verwacht-van", "expected V1.tarief.timeline[0].from");
      ("verwachte-tijdlijn-tot", "verwacht-tot", "expected V1.tarief.timeline[0].to") ];
  (* the calendar half, which the case schema's pattern lets through: the
     reader's own reason rides along whatever it is *)
  rejects "case-loader-lexeme-attribute-date-30-february" "vondstdag" "2026-02-30"
    (d "V1.vondstdag" "2026-02-30" (day_number "2026-02-30"));
  rejects "case-loader-lexeme-attribute-timeline-from-month-13" "tarief-van" "2026-13-01"
    (d "V1.tarief.timeline[0].from" "2026-13-01" (month_kind "2026-13-01"));
  (* dateTime: the reader's refusal names the grammar position, the loader
     adds the case position *)
  let dt at lex =
    Printf.sprintf
      "%s %S is not the uchar-case-v0 dateTime lexeme yyyy-mm-ddThh:mm:ss[.fff] (malformed dateTime \
       %S (at offset 10: expected 'T'))"
      at lex lex
  in
  let space = "2026-03-10 09:00:00" in
  List.iter
    (fun (ident, slot, at) -> rejects ("case-loader-lexeme-" ^ ident) slot space (dt at space))
    [ ("attribuut-datumtijd", "meldstip", "V1.meldstip");
      ("parameter-datumtijd", "peilstip", "parameter peilstip");
      ("verwachte-waarde-datumtijd", "verwacht-meldstip", "expected V1.meldstip") ];
  (* the clock half, which the dateTime pattern lets through *)
  rejects "case-loader-lexeme-attribute-date-time-hour-24" "meldstip" "2026-03-10T24:00:00"
    (Printf.sprintf
       "V1.meldstip %S is not the uchar-case-v0 dateTime lexeme yyyy-mm-ddThh:mm:ss[.fff] (malformed \
        dateTime %S (hour 24 is out of range))"
       "2026-03-10T24:00:00" "2026-03-10T24:00:00");
  (* number: the case wire's own grammar, which the IR literal's number
     variant shares since the L4 landing. Before 2026-09-23 "1,000" read as
     1 (WF-081's decimal comma of the day) and ".5" as 0.5, both silently;
     "1/0" and "abc" escaped the loader as evaluation faults *)
  let g at lex =
    Printf.sprintf
      "%s %S is not the uchar-case-v0 number lexeme [-]digits[.digits] or [-]digits/digits, the \
       denominator without a leading 0"
      at lex
  in
  List.iter
    (fun (ident, slot, lex, at) -> rejects ("case-loader-lexeme-" ^ ident) slot lex (g at lex))
    [ ("attribuut-getal-komma", "gewicht", "1,000", "V1.gewicht");
      ("attribuut-getal-kale-punt", "gewicht", ".5", "V1.gewicht");
      ("attribuut-getal-exponent", "gewicht", "1E3", "V1.gewicht");
      ("attribuut-getal-plusteken", "gewicht", "+5", "V1.gewicht");
      ("attribuut-getal-tekst", "gewicht", "abc", "V1.gewicht");
      ("attribuut-getal-noemer-nul", "gewicht", "1/0", "V1.gewicht");
      (* the numerator half of the rational branch: without its own check a
         decimal or empty numerator passed the fence and escaped the loader
         as an evaluation fault (lens mutant, 2026-09-23) *)
      ("attribuut-getal-teller-decimaal", "gewicht", "1.5/2", "V1.gewicht");
      ("attribuut-getal-teller-leeg", "gewicht", "-/3", "V1.gewicht");
      ("attribuuttijdlijn-waarde-getal-komma", "tarief-waarde", "12,5", "V1.tarief.timeline[0].value");
      ("parameter-getal-noemer-voorloopnul", "heffing", "3/04", "parameter heffing");
      ("verwachte-waarde-getal-exponent", "verwacht-gewicht", "1.25E1", "expected V1.gewicht") ];
  (* The two other load-time refusals that left the loader as a Value.Fault
     (measured 2026-09-23, both runners exit 1): label uniqueness, which the
     loader's header claims as its own cross-check but left to the world's
     evaluation-time guard, and the grid check of a timeline on a declared
     grid other than month or year, which runs through the bucketing (ch.
     3.2 defines it for those two only). Both are load refusals, so both
     are case errors, the second by position. *)
  let two_refusals =
    (* the canonical case with its one instance written twice: the array
       after "instances", closed by the bracket that matches it (no string
       in the template carries a bracket) *)
    let t = case_with "" "" in
    let sort_key = {|"instances": [|} in
    let rec find i = if String.sub t i (String.length sort_key) = sort_key then i else find (i + 1) in
    let open_ = find 0 + String.length sort_key - 1 in
    let rec closes i nesting_depth =
      match t.[i] with
      | '[' | '{' -> closes (i + 1) (nesting_depth + 1)
      | ']' | '}' -> if nesting_depth = 1 then i else closes (i + 1) (nesting_depth - 1)
      | _ -> closes (i + 1) nesting_depth
    in
    let finish = closes open_ 0 in
    let v1 = String.sub t (open_ + 1) (finish - open_ - 1) in
    String.sub t 0 (open_ + 1) ^ v1 ^ ", " ^ v1 ^ String.sub t finish (String.length t - finish)
  in
  refuses_text "caselader-dubbel-label" two_refusals "duplicate instance label \"V1\"";
  let doc_day =
    { doc with
      object_types =
        [ (let ident, o = vondst in
           ( ident,
             { o with
               attribute_granularity =
                 [ ("tarief", Some Ir_v0.Ast.Day); ("ijkdag", Some Ir_v0.Ast.Month) ] } )) ] }
  in
  refuses_text ~d:doc_day "caselader-tijdlijn-dagrooster" (case_with "" "")
    "V1.tarief: the declared grid is day, which every day boundary sits on, yet this loader \
     checks a timeline's grid through the calendar bucketing and v0 defines buckets for month \
     and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as \
     a defect of the case";
  let doc_grid g =
    { doc with
      object_types =
        [ (let ident, o = vondst in
           ( ident,
             { o with
               attribute_granularity = [ ("tarief", Some g); ("ijkdag", Some Ir_v0.Ast.Month) ] }
           )) ] }
  in
  List.iter
    (fun (ident, g, grid) ->
      refuses_text ~d:(doc_grid g) ident (case_with "" "")
        (Printf.sprintf
           "V1.tarief: the declared grid is %s, and v0 defines calendar buckets for month and \
            year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12), so this loader has no %s \
            grid to check the segment boundaries against: refused as a v0 limit, not as a defect \
            of the case"
           grid grid))
    [ ("caselader-tijdlijn-weekrooster", Ir_v0.Ast.Week, "week");
      ("caselader-tijdlijn-kwartaalrooster", Ir_v0.Ast.Quarter, "quarter") ];
  (* the per-segment refusals beside the lexeme ones name their segment the
     same way; the two that compare segments with each other keep the
     timeline's own position *)
  rejects "case-loader-segment-to-not-after-from" "tarief-tot" "2026-02-01"
    "V1.tarief.timeline[1]: segment 'to' must lie after 'from'";
  rejects "case-loader-segment-outside-grid" "tarief-van" "2026-01-15"
    "V1.tarief.timeline[0]: segment boundaries must sit on the declared granularity grid";
  rejects "case-loader-period-to-not-after-from" "dienst-tot" "2026-03-01"
    "V1.dienstdoend[1]: period 'to' must lie after 'from'";
  (* The JSON-shape refusals: a value of the wrong JSON kind, or a missing
     field, named by the same positions as the lexeme refusals above, with
     what was found there. Until 2026-09-23 they read "expected an object",
     "expected a string" and "missing field" with no position at all, a
     non-string asOfDate read "malformed asOfDate", and the value forms each
     had a wording of their own ("malformed value", "malformed boolean", "a
     list value's "list" field is not an array", "malformed timeline
     value"). Gate 4's schema keeps every one of them out of the corpus
     files, so they are pinned here, one per call site of the loader. *)
  let canon = case_with "" "" in
  let tally sub s =
    let n = String.length sub in
    let rec go i k =
      if i > String.length s - n then k
      else if String.sub s i n = sub then go (i + n) (k + 1)
      else go (i + 1) k
    in
    go 0 0
  in
  (* one edit of the canonical case; the anchor must occur exactly once, or
     the witness would pin a different edit than its name says *)
  let edited ~sub ~via =
    let k = tally sub canon in
    if k <> 1 then invalid_arg (Printf.sprintf "witness anchor %S occurs %d times" sub k);
    substitute ~sub ~via canon
  in
  let form_kind ?d ident ~sub ~via expected_value = refuses_text ?d ident (edited ~sub ~via) expected_value in
  let bare ?(asof = "null") ?(input = {|{ "instances": [], "parameters": {} }|})
      ?(rest = {|, "expected": [ { "kind": "consistent" } ]|}) () =
    Printf.sprintf {|{ "format": "uchar-case-v0", "asOfDate": %s, "input": %s%s }|} asof input rest
  in
  let with_instance inst =
    bare ~input:(Printf.sprintf {|{ "instances": [ %s ], "parameters": {} }|} inst) ()
  in
  let v1 rest = Printf.sprintf {|{ "label": "V1", "objectType": "Vondst"%s }|} rest in
  let doc_role =
    { doc with
      object_types = doc.object_types @ [ ot "Melder" [] [] ];
      fact_types =
        [ ( "vondstmelding",
            { Ir_v0.Ast.ft_name = Some "vondstmelding";
              reciprocal = false;
              role_names =
                [ { Ir_v0.Ast.role_identifier = Some "melder"; role_object_type_name = Some "Melder";
                    role_plural = Some "melders"; role_many = true };
                  { Ir_v0.Ast.role_identifier = Some "vondst"; role_object_type_name = Some "Vondst";
                    role_plural = None; role_many = false } ] } ) ] }
  in
  (* the root and the input *)
  refuses_text "caselader-vorm-wortel-geen-object" "[]" "<root>: expected an object, got []";
  form_kind "caselader-vorm-format-ontbreekt" ~sub:{|"format": "uchar-case-v0", |} ~via:""
    {|<root>: missing field "format"|};
  form_kind "caselader-vorm-format-geen-tekst" ~sub:{|"format": "uchar-case-v0"|} ~via:{|"format": 0|}
    "format: expected a string, got 0";
  form_kind "caselader-vorm-asofdate-ontbreekt" ~sub:{|"asOfDate": "2026-06-01",|} ~via:""
    {|<root>: missing field "asOfDate"|};
  form_kind "caselader-vorm-asofdate-geen-tekst" ~sub:{|"asOfDate": "2026-06-01"|}
    ~via:{|"asOfDate": 20260601|} "asOfDate: expected a string or null, got 20260601";
  refuses_text "caselader-vorm-input-geen-object" (bare ~input:"[]" ())
    "input: expected an object, got []";
  refuses_text "caselader-vorm-input-ontbreekt"
    {|{ "format": "uchar-case-v0", "asOfDate": null, "expected": [ { "kind": "consistent" } ] }|}
    {|<root>: missing field "input"|};
  refuses_text "caselader-vorm-instances-geen-lijst"
    (bare ~input:{|{ "instances": {}, "parameters": {} }|} ())
    "input.instances: expected a list, got {}";
  refuses_text "caselader-vorm-instances-ontbreekt" (bare ~input:{|{ "parameters": {} }|} ())
    {|input: missing field "instances"|};
  refuses_text "caselader-vorm-parameters-geen-object"
    (bare ~input:{|{ "instances": [], "parameters": [] }|} ())
    "input.parameters: expected an object, got []";
  refuses_text "caselader-vorm-parameters-ontbreekt" (bare ~input:{|{ "instances": [] }|} ())
    {|input: missing field "parameters"|};
  refuses_text "caselader-vorm-expected-geen-lijst" (bare ~rest:{|, "expected": {}|} ())
    "expected: expected a list, got {}";
  refuses_text "caselader-vorm-expected-ontbreekt" (bare ~rest:"" ())
    {|<root>: missing field "expected"|};
  refuses_text "caselader-vorm-expectedfault-geen-object"
    (bare ~rest:{|, "expectedFault": []|} ())
    "expectedFault: expected an object, got []";
  refuses_text "caselader-vorm-expectedfault-veld-ontbreekt"
    (bare ~rest:{|, "expectedFault": { "code": "EF-005" }|} ())
    {|expectedFault: missing field "params"|};
  refuses_text "caselader-vorm-expectedfault-geen-tekst"
    (bare ~rest:{|, "expectedFault": { "code": "EF-005", "params": { "rule": 1 } }|} ())
    "expectedFault.params.rule: expected a string, got 1";
  (* an instance: by its array position until its label is read, by the
     label from then on *)
  refuses_text "caselader-vorm-instantie-geen-object" (with_instance "5")
    "input.instances[0]: expected an object, got 5";
  form_kind "caselader-vorm-label-ontbreekt" ~sub:{|{ "label": "V1", "objectType": "Vondst",|}
    ~via:{|{ "objectType": "Vondst",|} {|input.instances[0]: missing field "label"|};
  form_kind "caselader-vorm-label-geen-tekst" ~sub:{|"label": "V1", "objectType"|}
    ~via:{|"label": 7, "objectType"|} "input.instances[0].label: expected a string, got 7";
  (* an empty label: the schema asks minLength 1, and every later position of
     the instance is written after its label, so an empty one used to load and
     send those positions to the root ("objectType: …", ".vondstdag.date: …") *)
  refuses_text "caselader-vorm-label-leeg"
    (with_instance
       {|{ "label": "", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }|})
    {|input.instances[0].label: expected a non-empty string, got ""|};
  form_kind "caselader-vorm-objecttype-geen-tekst" ~sub:{|"objectType": "Vondst"|}
    ~via:{|"objectType": 1|} "V1.objectType: expected a string, got 1";
  List.iter
    (fun (ident, rest, expected_value) ->
      refuses_text ~d:doc_role ("caselader-vorm-" ^ ident) (with_instance (v1 rest)) expected_value)
    [ ("attributes-ontbreekt", {|, "roles": {}, "characteristics": []|},
       {|V1: missing field "attributes"|});
      ("attributes-geen-object", {|, "attributes": [], "roles": {}, "characteristics": []|},
       "V1.attributes: expected an object, got []");
      ("characteristics-ontbreekt", {|, "attributes": {}, "roles": {}|},
       {|V1: missing field "characteristics"|});
      ("characteristics-geen-lijst", {|, "attributes": {}, "roles": {}, "characteristics": {}|},
       "V1.characteristics: expected a list, got {}");
      ("kenmerk-geen-tekst", {|, "attributes": {}, "roles": {}, "characteristics": [ 3 ]|},
       "V1.characteristics[0]: expected a string, got 3");
      ("roles-ontbreekt", {|, "attributes": {}, "characteristics": []|},
       {|V1: missing field "roles"|});
      ("roles-geen-object", {|, "attributes": {}, "roles": [], "characteristics": []|},
       "V1.roles: expected an object, got []");
      ("rol-geen-lijst", {|, "attributes": {}, "roles": { "melders": "M1" }, "characteristics": []|},
       {|V1.roles.melders: expected a list, got "M1"|});
      ("rollabel-geen-tekst",
       {|, "attributes": {}, "roles": { "melders": [ 5 ] }, "characteristics": []|},
       "V1.roles.melders[0]: expected a string, got 5");
      ("kenmerktijdlijnen-geen-object",
       {|, "attributes": {}, "roles": {}, "characteristics": [], "characteristicTimelines": []|},
       "V1.characteristicTimelines: expected an object, got []");
      ("perioden-geen-lijst",
       {|, "attributes": {}, "roles": {}, "characteristics": [],
          "characteristicTimelines": { "dienstdoend": {} }|},
       "V1.dienstdoend: expected a list, got {}");
      ("periode-geen-object",
       {|, "attributes": {}, "roles": {}, "characteristics": [],
          "characteristicTimelines": { "dienstdoend": [ 5 ] }|},
       "V1.dienstdoend[0]: expected an object, got 5");
      ("periodegrens-ontbreekt",
       {|, "attributes": {}, "roles": {}, "characteristics": [],
          "characteristicTimelines": { "dienstdoend": [ { "from": "2026-01-01" } ] }|},
       {|V1.dienstdoend[0]: missing field "to"|});
      ("periodegrens-geen-tekst",
       {|, "attributes": {}, "roles": {}, "characteristics": [],
          "characteristicTimelines": { "dienstdoend": [ { "from": 20260101, "to": "2026-02-01" } ] }|},
       "V1.dienstdoend[0].from: expected a string, got 20260101") ];
  (* values, timeline segments and parameters *)
  form_kind "caselader-vorm-waarde-geen-object" ~sub:{|"vondstdag": { "date": "2026-03-10" }|}
    ~via:{|"vondstdag": "2026-03-10"|} {|V1.vondstdag: expected an object, got "2026-03-10"|};
  form_kind "caselader-vorm-datum-geen-tekst" ~sub:{|"vondstdag": { "date": "2026-03-10" }|}
    ~via:{|"vondstdag": { "date": 20260310 }|} "V1.vondstdag.date: expected a string, got 20260310";
  form_kind "caselader-vorm-getal-geen-tekst" ~sub:{|"gewicht": { "number": "-12.50" }|}
    ~via:{|"gewicht": { "number": -12.5 }|} "V1.gewicht.number: expected a string, got -12.5";
  form_kind "caselader-vorm-datumtijd-geen-tekst"
    ~sub:{|"meldstip": { "dateTime": "2026-03-10T09:00:00" }|}
    ~via:{|"meldstip": { "dateTime": 1 }|} "V1.meldstip.dateTime: expected a string, got 1";
  form_kind "caselader-vorm-waarde-meerdere-varianten" ~sub:{|"vondstdag": { "date": "2026-03-10" }|}
    ~via:{|"vondstdag": { "date": "2026-03-10", "number": "1" }|}
    {|V1.vondstdag: expected one value variant, got {"date":"2026-03-10","number":"1"}|};
  refuses_text "caselader-vorm-boolean-geen-boolean"
    (with_instance
       (v1 {|, "attributes": { "gemeld": { "boolean": "waar" } }, "roles": {}, "characteristics": []|}))
    {|V1.gemeld.boolean: expected true or false, got "waar"|};
  form_kind "caselader-vorm-lijst-geen-lijst"
    ~sub:{|"vangstdagen": { "list": [ { "date": "2026-03-09" }, { "date": "2026-03-11" } ] }|}
    ~via:{|"vangstdagen": { "list": {} }|} "V1.vangstdagen.list: expected a list, got {}";
  form_kind "caselader-vorm-struct-geen-object"
    ~sub:{|"dossier": { "struct": { "peildag": { "date": "2026-03-10" } } }|}
    ~via:{|"dossier": { "struct": [] }|} "V1.dossier.struct: expected an object, got []";
  form_kind "caselader-vorm-payload-geen-object" ~sub:{|"payload": { "date": "2026-03-10" }|}
    ~via:{|"payload": 5|} "V1.status.payload: expected an object, got 5";
  refuses_text "caselader-vorm-tijdlijn-geen-lijst"
    (with_instance
       (v1 {|, "attributes": { "tarief": { "timeline": {} } }, "roles": {}, "characteristics": []|}))
    "V1.tarief.timeline: expected a list, got {}";
  form_kind "caselader-vorm-constructor-geen-tekst" ~sub:{|"constructor": "Gepland"|}
    ~via:{|"constructor": 1|} "V1.status.constructor: expected a string, got 1";
  form_kind "caselader-vorm-lijstelement-geen-tekst" ~sub:{|{ "date": "2026-03-09" }|}
    ~via:{|{ "date": 9 }|} "V1.vangstdagen[0].date: expected a string, got 9";
  form_kind "caselader-vorm-structveld-geen-tekst" ~sub:{|"peildag": { "date": "2026-03-10" }|}
    ~via:{|"peildag": { "date": 10 }|} "V1.dossier.peildag.date: expected a string, got 10";
  form_kind "caselader-vorm-segment-geen-object"
    ~sub:{|{ "from": "2026-02-01", "to": "2026-03-01", "value": { "number": "6" } }|} ~via:"5"
    "V1.tarief.timeline[1]: expected an object, got 5";
  form_kind "caselader-vorm-segmentwaarde-ontbreekt"
    ~sub:{|{ "from": "2026-02-01", "to": "2026-03-01", "value": { "number": "6" } }|}
    ~via:{|{ "from": "2026-02-01", "to": "2026-03-01" }|}
    {|V1.tarief.timeline[1]: missing field "value"|};
  form_kind "caselader-vorm-segmentgrens-geen-tekst" ~sub:{|{ "from": "2026-02-01", "to": "2026-03-01"|}
    ~via:{|{ "from": 20260201, "to": "2026-03-01"|}
    "V1.tarief.timeline[1].from: expected a string, got 20260201";
  form_kind "caselader-vorm-parameterwaarde-geen-object" ~sub:{|"heffing": { "number": "3/4" }|}
    ~via:{|"heffing": 34|} "parameter heffing: expected an object, got 34";
  form_kind "caselader-vorm-parameterdatum-geen-tekst" ~sub:{|"peildatum": { "date": "2026-03-10" }|}
    ~via:{|"peildatum": { "date": 1 }|} "parameter peildatum.date: expected a string, got 1";
  (* expectations: by their array position, their value by label and attribute *)
  form_kind "caselader-vorm-verwachting-geen-object" ~sub:{|{ "kind": "consistent" }|} ~via:"5"
    "expected[4]: expected an object, got 5";
  form_kind "caselader-vorm-kind-ontbreekt" ~sub:{|{ "kind": "consistent" }|} ~via:"{ }"
    {|expected[4]: missing field "kind"|};
  form_kind "caselader-vorm-kind-geen-tekst" ~sub:{|{ "kind": "consistent" }|} ~via:{|{ "kind": 1 }|}
    "expected[4].kind: expected a string, got 1";
  form_kind "caselader-vorm-verwacht-label-ontbreekt" ~sub:{|"label": "V1", "attribute": "vondstdag",|}
    ~via:{|"attribute": "vondstdag",|} {|expected[0]: missing field "label"|};
  form_kind "caselader-vorm-verwacht-label-geen-tekst" ~sub:{|"label": "V1", "attribute": "vondstdag"|}
    ~via:{|"label": 1, "attribute": "vondstdag"|} "expected[0].label: expected a string, got 1";
  form_kind "caselader-vorm-verwacht-attribuut-geen-tekst" ~sub:{|"attribute": "vondstdag"|}
    ~via:{|"attribute": 3|} "expected[0].attribute: expected a string, got 3";
  form_kind "caselader-vorm-verwachte-waarde-ontbreekt"
    ~sub:"\"attribute\": \"vondstdag\",\n      \"value\": { \"date\": \"2026-03-10\" } }"
    ~via:{|"attribute": "vondstdag" }|} {|expected[0]: missing field "value"|};
  form_kind "caselader-vorm-verwachte-waarde-geen-object"
    ~sub:{|"value": { "date": "2026-03-10" }|} ~via:{|"value": "2026-03-10"|}
    {|expected V1.vondstdag: expected an object, got "2026-03-10"|};
  let expected_timeline =
    {|"timeline": [|} ^ "\n      "
    ^ {|{ "from": "2026-01-01", "to": "2026-02-01", "value": { "number": "5" } } ]|}
  in
  form_kind "caselader-vorm-verwachte-tijdlijn-ontbreekt"
    ~sub:({|"attribute": "tarief", |} ^ expected_timeline ^ " }")
    ~via:{|"attribute": "tarief" }|} {|expected[3]: missing field "timeline"|};
  form_kind "caselader-vorm-verwachte-tijdlijn-geen-lijst" ~sub:expected_timeline
    ~via:{|"timeline": {}|} "expected V1.tarief.timeline: expected a list, got {}";
  form_kind "caselader-vorm-kenmerk-ontbreekt" ~sub:{|{ "kind": "consistent" }|}
    ~via:{|{ "kind": "characteristicPresent", "label": "V1" }|}
    {|expected[4]: missing field "characteristic"|};
  form_kind "caselader-vorm-regel-geen-tekst" ~sub:{|{ "kind": "consistent" }|}
    ~via:{|{ "kind": "inconsistent", "rule": 5 }|} "expected[4].rule: expected a string, got 5";
  (* what was found is shown up to 80 bytes and cut on a character boundary:
     a whole wrong subtree used to be printed in full on one line (a root
     given as a list: 2 154 characters, measured by the lens 2026-09-23) *)
  refuses_text "caselader-vorm-vondst-afgekapt"
    ("[ " ^ String.concat ", " (List.init 40 (fun i -> string_of_int (i + 1))) ^ " ]")
    "<root>: expected an object, got \
     [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,3\xe2\x80\xa6";
  refuses_text "caselader-vorm-vondst-afgekapt-op-tekengrens"
    (bare
       ~input:
         (Printf.sprintf {|{ "instances": [ "%s" ], "parameters": {} }|}
            (String.concat "" (List.init 60 (fun _ -> "\xc3\xa9"))))
       ())
    ("input.instances[0]: expected an object, got \""
    ^ String.concat "" (List.init 39 (fun _ -> "\xc3\xa9"))
    ^ "\xe2\x80\xa6");
  (* three cross-checks of the same functions that named no position either:
     with two to-one links or two inconsistent pins in one case the author
     could not tell which one was meant *)
  form_kind "caselader-positie-onbekende-soort" ~sub:{|{ "kind": "consistent" }|}
    ~via:{|{ "kind": "consistentt" }|} {|expected[4].kind: unknown expectation kind "consistentt"|};
  form_kind "caselader-positie-labelpin-zonder-regel" ~sub:{|{ "kind": "consistent" }|}
    ~via:{|{ "kind": "inconsistent", "label": "V1" }|}
    "expected[4]: an inconsistent label pin requires a rule pin";
  refuses_text ~d:doc_role "caselader-positie-naar-een-rol-twee-labels"
    (bare
       ~input:
         {|{ "instances": [
            { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
            { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
            { "label": "M1", "objectType": "Melder", "attributes": {},
              "roles": { "vondst": [ "V1", "V2" ] }, "characteristics": [] } ],
          "parameters": {} }|}
       ())
    {|M1.roles.vondst: to-one role "vondst" carries 2 labels|}

(* WF-022's value clause, held for every KENMERKTOEKENNING: the rule writes
   the boolean constant waar and nothing else. The document below is
   corpus/negative/wf022-characteristic-false rebuilt by hand (rule "geen herleving"
   attributes "herleefd" with onwaar when "nieuw recht ontstaan" holds). Forced
   past the verifier, ref used to run the rule and leave the characteristic
   absent without a word, where the ASP backend refuses the document at
   translation; since 2026-09-23 ref refuses it too, by name. A computed value
   is refused the same way even when it would evaluate to true: the clause is
   about the constant, not about what the value happens to be. The verifier
   keys the clause on a characteristic TARGET and ref on the SOORT, so ref
   also refuses a KENMERKTOEKENNING whose target is an attribute, which the
   verifier admits (the lens's measurement V7 on jutterswet-penningdrempel,
   rebuilt below). *)
let () =
  let recht =
    ot "Recht" [ ("actief", Some Ir_v0.Ast.Boolean) ] [ "herleefd"; "nieuw recht ontstaan" ]
  in
  let characteristic k : Ir_v0.Ast.condition_expr =
    Characteristic_check
      { characteristic = Some k; navigation_binding = None; negation = false;
        holder = None; time_period = None; source_name = None }
  in
  let doc value_node =
    { empty_doc with
      object_types = [ recht ];
      resolved_rules =
        [ rr "geen herleving" Characteristic_attribution
            (Some (aref "herleefd" "Recht"))
            (Some value_node) (Some (characteristic "nieuw recht ontstaan")) ] }
  in
  let world_state =
    List.fold_left World.add_instance World.empty
      [ { (inst "R1" "Recht" [ ("actief", Value.Bool true) ]) with chars = [ "nieuw recht ontstaan" ] };
        inst "R2" "Recht" [ ("actief", Value.Bool true) ] ]
  in
  let sentence_text ?(rule_def = "geen herleving") ?(objective = "herleefd") ?(ot = "Recht") value_node =
    Printf.sprintf
      "rule %S writes %S of %S as a characteristic, with %s: a version without a presentation and \
       without initialisation writes the boolean constant true and nothing else. Declined at \
       evaluation"
      rule_def objective ot value_node
  in
  let rejects ident value_node expected_value =
    match Eval.evaluate (env_of (doc value_node)) world_state with
    | exception Value.Fault { text = m; _ } -> check (ident ^ " (got: " ^ m ^ ")") (m = expected_value)
    | w ->
        check
          (Printf.sprintf "%s (no fault at all; herleefd on R1: %b, on R2: %b)" ident
             (World.has_char (World.get w "R1") "herleefd")
             (World.has_char (World.get w "R2") "herleefd"))
          false
  in
  rejects "WF-022-kenmerk-onwaar-weigert" (litv "onwaar" Ir_v0.Ast.Boolean)
    (sentence_text {|the literal {"boolean": false} typed Boolean|});
  rejects "WF-022-kenmerk-berekend-weigert"
    (aref "actief" "Recht")
    (sentence_text "a computed value");
  (* both halves of the constant, each on its own (lens mutants MW3 and MW4
     dropped one half each and the suite stayed green): the text waar, and
     the boolean variant true under a type other than Boolean. Since the
     L4 landing a boolean literal is a JSON boolean, so a capitalised "Waar"
     is no spelling of one any more and the type half is probed by the true
     variant under Text_type — a node only a hand builds, since the loader
     refuses a variant its type does not name (WF-114) *)
  rejects "WF-022-kenmerk-waar-als-tekst-weigert" (litv "waar" Ir_v0.Ast.Text_type)
    (sentence_text {|the literal {"text": "waar"} typed Text|});
  rejects "WF-022-kenmerk-booleaanse-variant-als-tekst-weigert"
    (Literal { variant = Lv_boolean true; typ = Some Ir_v0.Ast.Text_type; source_name = None })
    (sentence_text {|the literal {"boolean": true} typed Text|});
  (* the control: the constant itself still attributes, on the condition *)
  let w = Eval.evaluate (env_of (doc (litv "waar" Ir_v0.Ast.Boolean))) world_state in
  check "WF-022-kenmerk-waar-kent-toe"
    (World.has_char (World.get w "R1") "herleefd" && not (World.has_char (World.get w "R2") "herleefd"));
  (* V7: a KENMERKTOEKENNING whose target is an ATTRIBUTE, typed as that
     attribute, with onwaar — verifier PASS, main ref PASS without a word *)
  let quantity : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = None }
  in
  let jutter = ot "Jutter" [ ("aantal aangemelde vondsten", Some quantity) ] [ "penninggerechtigd" ] in
  let doc_v7 =
    { empty_doc with
      object_types = [ jutter ];
      resolved_rules =
        [ rr "aantal als kenmerk" Characteristic_attribution
            (Some (aref "aantal aangemelde vondsten" "Jutter"))
            (Some (litv "onwaar" Ir_v0.Ast.Boolean)) None ] }
  in
  (match
     Eval.evaluate (env_of doc_v7)
       (World.add_instance World.empty (inst "J1" "Jutter" [ ("aantal aangemelde vondsten", n "14") ]))
   with
  | exception Value.Fault { text = m; _ } ->
      let expected_value =
        sentence_text ~rule_def:"aantal als kenmerk" ~objective:"aantal aangemelde vondsten" ~ot:"Jutter"
          {|the literal {"boolean": false} typed Boolean|}
      in
      check ("WF-022-kenmerktoekenning-op-attribuut-weigert (got: " ^ m ^ ")") (m = expected_value)
  | _ -> check "WF-022-kenmerktoekenning-op-attribuut-weigert (no fault at all)" false)

(* WV-4.1a-eigen-lezing (WF-053, IR-SPEC ch. 4.1a): an own read names the
   subject type itself. Jutter ⊑ Persoon inherits leeftijd; written on the
   subject type the read reaches the inherited member through the chain and
   answers 40; written on the supertype it names another type, which only a
   navigation reaches, and ref refuses it in the verifier's sentence.
   WV-2.10-kenmerk (WF-080): a characteristic is no attribute, so its name
   has no static type and a value position never reads it *)
let () =
  let persoon = ot "Persoon" [ ("leeftijd", Some num0) ] [] in
  let jutter =
    let ident, o = ot "Jutter" [ ("tweede leeftijd", Some num0) ] [ "vrijgesteld" ] in
    (ident, { o with ot_supertype = Some "Persoon" })
  in
  let doc value_node : Ir_v0.Ast.document =
    {
      empty_doc with
      object_types = [ persoon; jutter ];
      resolved_rules =
        [ rr "tweede leeftijd" Calculation (Some (aref "tweede leeftijd" "Jutter")) (Some value_node) None ];
    }
  in
  let run value_node =
    let w = World.add_instance World.empty (inst "j" "Jutter" [ ("leeftijd", n "40") ]) in
    World.attr (World.get (Eval.evaluate (env_of (doc value_node)) w) "j") "tweede leeftijd"
  in
  expect_num "WV-4.1a-eigen-lezing-op-het-subjecttype" (run (aref "leeftijd" "Jutter")) "40";
  expect_fault_msg "WV-4.1a-eigen-lezing-op-een-supertype-faalt"
    (fun () -> run (aref "leeftijd" "Persoon"))
    "an attribute reference without a path is the subject's own read, so its subjectType is the \
     subject type 'Jutter' — 'Persoon' names another type, which only a navigation reaches";
  expect_fault_msg "WV-4.1-kenmerk-op-een-waardepositie-faalt"
    (fun () -> run (aref "vrijgesteld" "Jutter"))
    "'vrijgesteld' is a characteristic of 'Jutter', not an attribute — a characteristic \
     endpoint is ill-formed in a value position";
  check "WV-2.10-kenmerk-heeft-geen-statisch-type"
    (Eval.static_type (env_of (doc (aref "leeftijd" "Jutter"))) (aref "vrijgesteld" "Jutter") = None);
  (* WF-083 (c): a total without a range is the subject's own read, so its
     elementType is the subject type; the supertype Persoon there is
     refused in the verifier's sentence, where the time-total arm used to
     let it through [isa] *)
  let grand_total el : Ir_v0.Ast.expr =
    Function_call
      { kind = Total; call_arguments = [ aref "leeftijd" "Jutter" ]; element_object_type = Some el;
        empty_fallback = None; filter = None; dimension_selection = None; typ = Some num0; source_name = None;
        grain = None; source_role = None; source_path = None }
  in
  expect_fault_msg "WV-4.3-totaal-zonder-bereik-over-een-supertype-faalt"
    (fun () -> run (grand_total "Persoon"))
    "the elementType 'Persoon' of a total without a range is not the fold subject \
     'Jutter': without a range the fold is the subject's own read, so its elementType must \
     name the subject type"

(* WF-037's granularity facet (IR-SPEC ch. 4.3): since the L4 landing only a
   BuiltinCall carries one, and only the two bucketing builtins read it, as
   the whole value of a rule on a temporal target (ch. 3.7, 3.8). On any
   other kind the facet is refused in the verifier's sentence, and a
   bucketing builtin anywhere else is refused by name: neither is evaluated
   with its facet dropped. The verifier refuses the first document first *)
let () =
  let doc value_node : Ir_v0.Ast.document =
    { empty_doc with
      object_types = [ ot "Jutter" [ ("jaar", Some num0); ("basis", Some num0) ] [] ];
      resolved_rules = [ rr "jaar" Calculation (Some (aref "jaar" "Jutter")) (Some value_node) None ] }
  in
  let run value_node =
    Eval.evaluate (env_of (doc value_node)) (World.add_instance World.empty (inst "J1" "Jutter" []))
  in
  let with_granularity kind call_arguments : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments; element_object_type = None; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = Some Month;
        source_role = None; source_path = None }
  in
  expect_fault_msg "WF-037-granulariteit-op-een-niet-emmerende-builtin-faalt"
    (fun () -> run (with_granularity Year_of [ litv "2026-03-15" Ir_v0.Ast.Date_in_days ]))
    "granularity facet on a non-bucketing kind";
  expect_fault_msg "WF-037-granulariteit-op-een-aggregatiesoort-faalt"
    (fun () -> run (with_granularity Sum_function [ litv "1" num0; litv "2" num0 ]))
    "granularity facet on a non-bucketing kind";
  expect_fault_msg "WV-3.7-emmerende-builtin-buiten-een-tijdsafhankelijk-doel-faalt"
    (fun () -> run (with_granularity Time_proportional_part [ aref "basis" "Jutter" ]))
    "a granularity-bucketed builtin call is executed only as the whole value of a rule on a \
     temporal target (ch. 3.7, 3.8)"

(* WV-4.1-literaal (IR-SPEC ch. 4.1): a literal is read from its value
   variant under its type. A variant the type does not name is refused in
   the loader's sentence (WF-114), never read as the type's value — a node
   only a hand builds, since an executor's loader refuses it at load. A
   number variant is read through the one number grammar, so a decimal comma
   is malformed rather than read as the number it resembles — on a node
   built by hand as well, since the loader refuses such a number too, in
   WF-081's sentence. *)
let () =
  let doc value_node : Ir_v0.Ast.document =
    { empty_doc with
      object_types = [ ot "Jutter" [ ("aantal", Some num0) ] [] ];
      resolved_rules = [ rr "aantal" Calculation (Some (aref "aantal" "Jutter")) (Some value_node) None ] }
  in
  let run value_node =
    Eval.evaluate (env_of (doc value_node)) (World.add_instance World.empty (inst "J1" "Jutter" []))
  in
  expect_fault_msg "WV-4.1-literaalvariant-tegen-type-faalt"
    (fun () ->
      run (Literal { variant = Lv_text "5"; typ = Some num0; source_name = None }))
    "literal value is a text value but its type is a Number: the variant is the one the type names";
  expect_fault_msg "WV-4.1-getal-met-komma-faalt"
    (fun () -> run (litv "12,5" num0))
    {|malformed number lexeme "12,5"|};
  expect_num "WV-4.1-getal-met-punt"
    (World.attr (World.get (run (litv "12" num0)) "J1") "aantal")
    "12";
  (* WV-4.1-contextwaarde (WF-098): a context value's name is one of two,
     spelled byte for byte. The bound day of a day-kind definition is no
     context value but a BoundDay, so a context value named after it is
     refused in the loader's sentence, never read as the bound day; and a
     recapitalised session value is no session value *)
  let context_value ident : Ir_v0.Ast.expr =
    Context_value { ident = Some ident; typ = Some Date_in_days; source_name = None }
  in
  let doc_date value_node : Ir_v0.Ast.document =
    { empty_doc with
      object_types = [ ot "Jutter" [ ("peildatum", Some Date_in_days) ] [] ];
      resolved_rules =
        [ rr "peildatum" Equation (Some (aref "peildatum" "Jutter")) (Some value_node) None ] }
  in
  let run_date value_node =
    Eval.evaluate
      { (env_of (doc_date value_node)) with as_of = Some "2026-03-15" }
      (World.add_instance World.empty (inst "J1" "Jutter" []))
  in
  expect_fault_msg "WV-4.1-de-dag-als-contextwaarde-faalt"
    (fun () -> run_date (context_value "de dag"))
    "context value 'de dag' is not one of the two admissible names evaluationDate, \
     evaluationYear: the context-value name is a closed set, spelled byte for byte";
  expect_fault_msg "WV-4.1-contextwaarde-andere-spelling-faalt"
    (fun () -> run_date (context_value "rekendatum"))
    "context value 'rekendatum' is not one of the two admissible names evaluationDate, \
     evaluationYear";
  (* the v0 spelling of the session value is outside the set since L5, like
     any other name (the ref side of corpus/negative/wf098-v0-name-evaluation-date) *)
  expect_fault_msg "WV-4.1-contextwaarde-v0-spelling-faalt"
    (fun () -> run_date (context_value "Rekendatum"))
    "context value 'Rekendatum' is not one of the two admissible names evaluationDate, \
     evaluationYear";
  check "WV-4.1-rekendatum-leest-de-evaluatiedatum"
    (World.attr (World.get (run_date (context_value Ir_v0.Spelling.evaluation_date)) "J1") "peildatum"
    = Value.date_of_iso "2026-03-15")

(* --- ch. 8.2: the execution year (worked example verbatim) -------------- *)

(* WV-8.2-rekenjaar: evaluationYear is the calendar year of the evaluation
   date, a whole unitless number, the value yearOf gives that date. It is
   derived from the session's one date and never supplied beside it, so a
   session one day later across a year boundary answers one year more; a
   session without a date faults in the sibling of evaluationDate's
   sentence *)
let () =
  let assessment_year : Ir_v0.Ast.expr =
    Context_value { ident = Some Ir_v0.Spelling.evaluation_year; typ = Some num0; source_name = None }
  in
  let assessment_date : Ir_v0.Ast.expr =
    Context_value { ident = Some Ir_v0.Spelling.evaluation_date; typ = Some Date_in_days; source_name = None }
  in
  let doc : Ir_v0.Ast.document =
    { empty_doc with
      object_types =
        [ ot "Vondst"
            [ ("vondstjaar", Some num0); ("meldjaar", Some num0);
              ("aantal verstreken penningjaren", Some num0) ]
            [] ];
      resolved_rules =
        [ rr "meldjaar" Equation (Some (aref "meldjaar" "Vondst")) (Some assessment_year) None;
          rr "aantal verstreken penningjaren" Calculation
            (Some (aref "aantal verstreken penningjaren" "Vondst"))
            (Some
               (Binary
                  { op = Min; left_side = Some assessment_year; right_side = Some (aref "vondstjaar" "Vondst");
                    typ = Some num0; source_name = None }))
            None ] }
  in
  let run as_of =
    let w =
      Eval.evaluate { (env_of doc) with as_of }
        (World.add_instance World.empty (inst "V1" "Vondst" [ ("vondstjaar", n "2025") ]))
    in
    let v1 = World.get w "V1" in
    (World.attr v1 "meldjaar", World.attr v1 "aantal verstreken penningjaren")
  in
  let meldjaar, verstreken = run (Some "2026-12-31") in
  expect_num "WV-8.2-rekenjaar-oudjaarsdag-meldjaar" meldjaar "2026";
  expect_num "WV-8.2-rekenjaar-oudjaarsdag-verstreken" verstreken "1";
  let meldjaar, verstreken = run (Some "2027-01-01") in
  expect_num "WV-8.2-rekenjaar-nieuwjaarsdag-meldjaar" meldjaar "2027";
  expect_num "WV-8.2-rekenjaar-nieuwjaarsdag-verstreken" verstreken "2";
  expect_fault_msg "WV-8.2-rekenjaar-zonder-rekendatum-faalt"
    (fun () -> run None)
    "the evaluationYear context value needs an evaluation date; the case carries asOfDate null";
  (* the value itself: the number 2026 without a unit, equal to what yearOf
     gives the evaluation date in the same session *)
  let env = { (env_of doc) with as_of = Some "2026-12-31" } in
  let w = World.add_instance World.empty (inst "V1" "Vondst" []) in
  let fetch e = Eval.eval_expr env w (World.get w "V1") ~err:no_err e in
  (match fetch assessment_year with
  | Value.Num (q, Value.Unitless) ->
      check "WV-8.2-rekenjaar-eenheidsloos" (Q.equal q (Q.of_int 2026))
  | v -> check ("WV-8.2-rekenjaar-eenheidsloos (got " ^ Value.to_string v ^ ")") false);
  match (fetch assessment_year, Arith.year_of (fetch assessment_date)) with
  | Value.Num (a, ua), Value.Num (b, ub) ->
      check "WV-8.2-rekenjaar-is-jaar-uit-rekendatum" (Q.equal a b && ua = ub)
  | a, b ->
      check
        ("WV-8.2-rekenjaar-is-jaar-uit-rekendatum (got " ^ Value.to_string a ^ " and "
       ^ Value.to_string b ^ ")")
        false

(* WV-4.1-variabele (WF-034): a VariableReference is the frontend's marker
   for a variable its resolver could not bind, and the wire serializes no
   binding site, so it resolves against nothing. ref loads the wire as an
   executor and refuses it there, at the verifier's pointer and in the
   verifier's sentence (the ref side of
   corpus/negative/wf034-variable-without-binding) *)
let () =
  let irf = Filename.temp_file "ref-wf034" ".ir.json" in
  let oc = open_out irf in
  output_string oc
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"heffing","datatype":{"node":"Number","whole":false,"decimals":2,"sign":"unspecified"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"heffing van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"heffing","subjectType":"Jutter","source":{"snippet":"De heffing"}},"value":{"node":"VariableReference","name":"x","source":{"snippet":"x"}},"initialisation":false,"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|};
  close_out oc;
  let finding =
    List.map
      (fun (x : Ir_v0.Checks.violation) -> (x.wf, x.at, x.reason))
      (List.filter
         (fun (x : Ir_v0.Checks.violation) -> x.wf = "WF-034")
         (Ir_v0.Checks.all (Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier irf)))
  in
  (match Ir_v0.Loader.load_file irf with
  | (_ : Ir_v0.Ast.document) -> check "WV-4.1-variabele-weigert-bij-laden (the document loads)" false
  | exception Ir_v0.Loader.Error (at, m) ->
      check
        ("WV-4.1-variabele-weigert-bij-laden (" ^ at ^ ": " ^ m ^ ")")
        (m = Ir_v0.Checks.variable_reference_refusal (Some "x")
        && finding = [ ("WF-034", at, m) ]));
  Sys.remove irf

(* WV-4.3-numerieke-builtin (WF-038): a square root and an absolute value take
   one number; a text operand is refused before it is read, in the verifier's
   sentence (the ref side of corpus/negative/wf038-absolute-value-text and
   corpus/negative/wf038-square-root-text) *)
let () =
  let body_text : Ir_v0.Ast.expr = Literal { variant = Lv_text "vier"; typ = Some Text_type; source_name = None } in
  let builtin kind : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments = [ body_text ]; element_object_type = None; empty_fallback = None; filter = None;
        dimension_selection = None; typ = Some num0; source_name = None; grain = None; source_role = None;
        source_path = None }
  in
  let run value_node =
    let doc =
      { empty_doc with
        object_types = [ ot "Jutter" [ ("aantal", Some num0) ] [] ];
        resolved_rules = [ rr "aantal" Calculation (Some (aref "aantal" "Jutter")) (Some value_node) None ] }
    in
    Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "J1" "Jutter" []))
  in
  expect_fault_msg "WV-4.3-absolute-waarde-tekst-faalt"
    (fun () -> run (builtin Absolute_value))
    "absoluteValue operand is text, must be a number";
  expect_fault_msg "WV-4.3-wortel-tekst-faalt"
    (fun () ->
      run
        (Rounding
           { base_expr = Some (builtin Square_root); mode_kind = Half_away_from_zero; decimal_places = 0; typ = Some num0;
             source_name = None }))
    "squareRoot operand is text, must be a number"

(* WV-3.8-duur-onbekende-eenheid (WF-038): the counted unit of a duration is
   a time unit or a declared day kind; any other is refused in the verifier's
   sentence, which names the builtin's own kind (the ref side of
   corpus/negative/wf038-duration-unknown-unit and
   wf038-absolute-duration-unknown-unit) *)
let () =
  let vadem : Ir_v0.Ast.datatype =
    Number
      { whole_part = true; decimal_places = 0; sign_char = None;
        measure_unit =
          Some { abbr_text = Some "vadem"; system = None; canonical_form = None; factor_to_canonical = None;
                 denominator = None } }
  in
  let extent kind : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments = [ aref "van" "Jutter"; aref "tot" "Jutter" ];
        element_object_type = None; empty_fallback = None; filter = None; dimension_selection = None;
        typ = Some vadem; source_name = None; grain = None; source_role = None; source_path = None }
  in
  let doc kind =
    { empty_doc with
      object_types =
        [ ot "Jutter" [ ("van", Some Date_in_days); ("tot", Some Date_in_days); ("spanne", Some vadem) ] [] ];
      resolved_rules = [ rr "spanne" Calculation (Some (aref "spanne" "Jutter")) (Some (extent kind)) None ] }
  in
  List.iter
    (fun (name, kind) ->
      match
        Eval.evaluate (env_of (doc kind))
          (World.add_instance World.empty
             (inst "J1" "Jutter"
                [ ("van", Value.date_of_iso "2024-01-01"); ("tot", Value.date_of_iso "2025-01-01") ]))
      with
      | exception Value.Fault { text = m; _ } ->
          check (name ^ " (got: " ^ m ^ ")") (m = Ir_v0.Checks.duration_unknown_unit kind "vadem")
      | _ -> check (name ^ " (no fault at all)") false)
    [ ("WV-3.8-duur-onbekende-eenheid-faalt", Ir_v0.Ast.Duration);
      ("WV-3.8-absolute-duur-onbekende-eenheid-faalt", Ir_v0.Ast.Absolute_duration) ]

(* WV-3.8-duurvorm (WF-038): the duration clause's other forms — two dates
   of one precision, a node type, a counted unit on it, a sub-day unit only
   over millisecond-precision operands — fault in the verifier's order and in
   its sentences from [Ir_v0.Checks], read off the static types before any
   value; each names the builtin's own kind. The ASP backend's suite holds the
   same rows. *)
let () =
  let unit_ a : Ir_v0.Ast.measure_unit =
    { abbr_text = Some a; system = None; canonical_form = None; factor_to_canonical = None; denominator = None }
  in
  let counted ?u () : Ir_v0.Ast.datatype =
    Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Option.map unit_ u }
  in
  let extent ?(kind = Ir_v0.Ast.Duration) typ a b : Ir_v0.Ast.expr =
    Function_call
      { kind; call_arguments = [ a; b ]; element_object_type = None; empty_fallback = None; filter = None;
        dimension_selection = None; typ; source_name = None; grain = None; source_role = None; source_path = None }
  in
  let van = aref "van" "Jutter" and up_to = aref "tot" "Jutter" and moment = aref "moment" "Jutter" in
  let run value_node =
    let doc =
      { empty_doc with
        object_types =
          [ ot "Jutter"
              [ ("van", Some Date_in_days); ("tot", Some Date_in_days); ("moment", Some Date_time_type);
                ("spanne", Some (counted ~u:"jr" ())) ]
              [] ];
        resolved_rules = [ rr "spanne" Calculation (Some (aref "spanne" "Jutter")) (Some value_node) None ] }
    in
    Eval.evaluate (env_of doc)
      (World.add_instance World.empty
         (inst "J1" "Jutter"
            [ ("van", Value.date_of_iso "2024-01-01"); ("tot", Value.date_of_iso "2025-01-01");
              ("moment", Value.Date_time 0) ]))
  in
  let open Ir_v0.Checks in
  List.iter
    (fun (name, value_node, sentence) ->
      match run value_node with
      | exception Value.Fault { text = m; _ } -> check (name ^ " (got: " ^ m ^ ")") (m = sentence)
      | _ -> check (name ^ " (no fault at all)") false)
    [ ( "WV-3.8-duurvorm-tekstoperand", extent (Some (counted ~u:"jr" ())) (litv "gisteren" Text_type) up_to,
        duration_operand_refusal Duration Text_type );
      ( "WV-3.8-duurvorm-absolute-tekstoperand",
        extent ~kind:Absolute_duration (Some (counted ~u:"jr" ())) van (litv "morgen" Text_type),
        duration_operand_refusal Absolute_duration Text_type );
      ( "WV-3.8-duurvorm-gemengde-precisie", extent (Some (counted ~u:"jr" ())) van moment,
        duration_mixed_precision Duration );
      ("WV-3.8-duurvorm-zonder-knooptype", extent None van up_to, duration_without_node_type Duration);
      ("WV-3.8-duurvorm-zonder-tijdseenheid", extent (Some (counted ())) van up_to, duration_without_time_unit Duration);
      ( "WV-3.8-duurvorm-uur-op-dagdatums", extent (Some (counted ~u:"uur" ())) van up_to,
        duration_sub_day_on_day_dates Duration ) ]

(* WV-3.8-datumvorm (WF-035): the form of date ± x is decided on the date
   type of either precision — two dates, a duration on the left, or a date on
   the left and the step, whose unit the duration's static type carries — and
   each form faults in its one sentence from [Ir_v0.Checks], in day,
   millisecond and mixed precision. The ASP backend's suite holds the same
   rows, so the two executors and the verifier speak one sentence per form. *)
let () =
  let unit_ a : Ir_v0.Ast.measure_unit =
    { abbr_text = Some a; system = None; canonical_form = None; factor_to_canonical = None; denominator = None }
  in
  let extent ?u n =
    litv n (Number { whole_part = true; decimal_places = 0; sign_char = None; measure_unit = Option.map unit_ u })
  in
  let bin ?(minus = false) typ l r : Ir_v0.Ast.expr =
    Binary { op = (if minus then Min else Plus); left_side = Some l; right_side = Some r; typ = Some typ; source_name = None }
  in
  let day_number = aref "dag" "Getij" and moment = aref "moment" "Getij" in
  let d = Ir_v0.Ast.Date_in_days and m = Ir_v0.Ast.Date_time_type in
  let run objective value_node =
    let doc =
      { empty_doc with
        object_types =
          [ ot "Getij" [ ("dag", Some Date_in_days); ("moment", Some Date_time_type); ("doel", Some objective) ] [] ];
        resolved_rules = [ rr "doel" Calculation (Some (aref "doel" "Getij")) (Some value_node) None ] }
    in
    Eval.evaluate (env_of doc)
      (World.add_instance World.empty
         (inst "G1" "Getij" [ ("dag", Value.date_of_iso "2026-03-15"); ("moment", Value.Date_time 0) ]))
  in
  let open Ir_v0.Checks in
  List.iter
    (fun (name, objective, value_node, sentence) ->
      match run objective value_node with
      | exception Value.Fault { text = msg; _ } -> check (name ^ " (got: " ^ msg ^ ")") (msg = sentence)
      | _ -> check (name ^ " (no fault at all)") false)
    [ ("WV-3.8-datumvorm-dag-plus-dag", d, bin d day_number day_number, date_plus_date_refusal);
      ("WV-3.8-datumvorm-ms-min-ms", m, bin ~minus:true m moment moment, date_plus_date_refusal);
      ("WV-3.8-datumvorm-dag-plus-ms-in-dag", d, bin d day_number moment, date_plus_date_refusal);
      ("WV-3.8-datumvorm-ms-min-dag-in-ms", m, bin ~minus:true m moment day_number, date_plus_date_refusal);
      ( "WV-3.8-datumvorm-duur-min-dag", d, bin ~minus:true d (extent ~u:"wk" "2") day_number,
        duration_before_date_refusal ~minus:true );
      ( "WV-3.8-datumvorm-duur-plus-ms", m, bin m (extent ~u:"uur" "26") moment,
        duration_before_date_refusal ~minus:false );
      ( "WV-3.8-datumvorm-duur-plus-dag-in-ms", m, bin m (extent ~u:"dg" "2") day_number,
        duration_before_date_refusal ~minus:false );
      ("WV-3.8-datumvorm-dag-plus-vadem", d, bin d day_number (extent ~u:"vadem" "2"), date_duration_unknown_unit "vadem");
      ( "WV-3.8-datumvorm-ms-plus-vadem", m, bin m moment (extent ~u:"vadem" "26"),
        date_duration_unknown_unit "vadem" );
      ( "WV-3.8-datumvorm-dag-plus-vadem-in-ms", m, bin m day_number (extent ~u:"vadem" "26"),
        date_duration_unknown_unit "vadem" );
      ("WV-3.8-datumvorm-dag-plus-kaal", d, bin d day_number (extent "2"), date_duration_without_time_unit);
      ("WV-3.8-datumvorm-ms-plus-kaal", m, bin m moment (extent "26"), date_duration_without_time_unit);
      ("WV-3.8-datumvorm-dag-plus-kaal-in-ms", m, bin m day_number (extent "2"), date_duration_without_time_unit);
      ("WV-3.8-datumvorm-dag-plus-uur", d, bin d day_number (extent ~u:"uur" "2"), sub_day_unit_on_day_date);
      ("WV-3.8-datumvorm-dag-plus-tekst", d, bin d day_number (litv "twee" Text_type), date_duration_operand_refusal Text_type);
      ("WV-3.8-datumvorm-ms-plus-tekst", m, bin m moment (litv "twee" Text_type), date_duration_operand_refusal Text_type);
      ( "WV-3.8-datumvorm-dag-plus-tekst-in-ms", m, bin m day_number (litv "twee" Text_type),
        date_duration_operand_refusal Text_type );
      ("WV-3.8-datumvorm-dag-plus-uur-in-ms", m, bin m day_number (extent ~u:"uur" "26"), sub_day_unit_on_day_date) ]

(* --- ch. 5.12: the reading of a condition at the fixpoint ------------------ *)

(* The readings of a case at its fixpoint in the canonical text, as
   [runner --trace] prints them without the "reading " before each line. *)
let readings_of ?(evaluate = true) irf casef =
  let d = Ir_v0.Loader.load_file irf in
  let c = Case.load_file d casef in
  let env =
    { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
      locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
  in
  let final = if evaluate then Eval.evaluate env c.world else c.world in
  Reading.lines env ~world:final
    (Reading.readings (Yojson.Safe.from_file irf) env ~world0:c.world ~world:final)

(* a file of the corpus, from the workspace root (gate 5 runs the suite there)
   or from the test's own build directory (dune runtest) *)
let corpus_file rel = if Sys.file_exists rel then rel else Filename.concat "../.." rel

(* WV-5.12-ploegaandeel: the chapter's worked example verbatim, the golden of
   ch. 5.10's first example. The composite stops after member 1 on J1, reads
   both members on J2 and J3, and the value 1 is reached where it holds. *)
let () =
  let golden = "corpus/golden/jutterswet-ploegaandeel/" in
  let got = readings_of (corpus_file (golden ^ "ir.json")) (corpus_file (golden ^ "case.json")) in
  let p = "/rules/rules/0/versions/0" in
  let row subject cells = List.map (fun c -> Printf.sprintf "%s \"%s\" %s" p subject c) cells in
  let expected =
    List.sort compare
      (row "J1"
         [ "FAILS"; "0 Compound false"; "1 Comparison false"; "2 AttributeReference 0"; "3 Literal 0";
           "4 Comparison unreached"; "5 Rounding unreached"; "6 Binary unreached";
           "7 AttributeReference unreached"; "8 AttributeReference unreached"; "9 Literal unreached";
           "10 Literal unreached" ]
      @ row "J2"
          [ "HOLDS"; "0 Compound true"; "1 Comparison true"; "2 AttributeReference 4"; "3 Literal 0";
            "4 Comparison true"; "5 Rounding 25"; "6 Binary 25"; "7 AttributeReference 100";
            "8 AttributeReference 4"; "9 Literal 0"; "10 Literal 1" ]
      @ row "J3"
          [ "FAILS"; "0 Compound false"; "1 Comparison true"; "2 AttributeReference 7"; "3 Literal 0";
            "4 Comparison false"; "5 Rounding 0"; "6 Binary 0.4285714285714286"; "7 AttributeReference 3";
            "8 AttributeReference 7"; "9 Literal 0"; "10 Literal unreached" ])
  in
  check "WV-5.12-ploegaandeel" (got = expected);
  if got <> expected then List.iter (Printf.printf "  got: %s\n") got

(* WV-5.12-fout-in-een-lezing: a node that faults carries the EF code of its
   fault, and so does every node the fault passes on its way out; the status
   is FAULTS. No golden reaches it — at a fixpoint the reference interpreter
   reached, the forms it read did not fault — so the readings are taken on
   the input world of a document whose second member is an Unresolved
   condition (EF-019): on J1 member 1 holds and member 2 faults, on J2 the
   composite stops before it. *)
let () =
  let doc =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"ploegomvang","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"bergingsaandeel","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}],"characteristics":[]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"bergingsaandeel van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"bergingsaandeel","subjectType":"Jutter","source":{"snippet":"Het bergingsaandeel van een Jutter"}},"value":{"node":"Literal","value":{"number":"1"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"1"}},"condition":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"Comparison","op":"notEqual","left":{"node":"AttributeReference","attribute":"ploegomvang","subjectType":"Jutter","source":{"snippet":"zijn ploegomvang"}},"right":{"node":"Literal","value":{"number":"0"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"0"}},"source":{"snippet":"zijn ploegomvang is ongelijk aan 0"}},{"node":"Unresolved","source":{"snippet":"hij is onvindbaar"}}],"source":{"snippet":"hij aan alle volgende voorwaarden voldoet"}},"initialisation":false,"presentation":"equation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let case_data =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "J1", "objectType": "Jutter", "attributes": { "ploegomvang": { "number": "4" } }, "roles": {}, "characteristics": [] },
    { "label": "J2", "objectType": "Jutter", "attributes": { "ploegomvang": { "number": "0" } }, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv512" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" doc and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      let got = readings_of ~evaluate:false irf casef in
      let p = "/rules/rules/0/versions/0" in
      let row subject cells = List.map (fun c -> Printf.sprintf "%s \"%s\" %s" p subject c) cells in
      let expected =
        List.sort compare
          (row "J1"
             [ "FAULTS"; "0 Compound fault EF-019"; "1 Comparison true"; "2 AttributeReference 4";
               "3 Literal 0"; "4 Unresolved fault EF-019"; "5 Literal unreached" ]
          @ row "J2"
              [ "FAILS"; "0 Compound false"; "1 Comparison false"; "2 AttributeReference 0";
                "3 Literal 0"; "4 Unresolved unreached"; "5 Literal unreached" ])
      in
      check "WV-5.12-fout-in-een-lezing" (got = expected);
      if got <> expected then List.iter (Printf.printf "  got: %s\n") got)

(* WV-5.12-keurgemiddelde: the chapter's second worked example verbatim, the
   golden jutterswet-keurgemiddelde. The node of a quantifier fold carries the
   table of ch. 3.9a over its contributors — the matrix's verdict on each, on
   the fixpoint world, depositing nothing — and the position: the members
   after the stop are not reached, and have their verdict all the same. On
   case 1 and case 3 the member after the stop divides by zero; the session
   never read it, so its channel is consistent, and the reading's verdict
   rests on the deposit it dropped. On case 4 the member is reached and the
   session deposits (the golden pins EF-005); the reading drops that deposit
   too. The golden's second rule, the wachttal, a count without a condition,
   reads its value: it holds, and the count carries each Strandwacht it
   counts (card readings-of-unconditional-forms). *)
let () =
  let golden = "corpus/golden/jutterswet-keurgemiddelde/" in
  let p = "/rules/rules/0/versions/0 \"P1\" " and q = "/rules/rules/1/versions/0 \"P1\" " in
  List.iter
    (fun (file, status, contributors, counted) ->
      let got = readings_of (corpus_file (golden ^ "ir.json")) (corpus_file (golden ^ file)) in
      let expected =
        List.sort compare
          [ p ^ status;
            p ^ "0 QuantifierFold " ^ string_of_bool (status = "HOLDS")
            ^ " contributors [" ^ contributors ^ "]";
            q ^ "HOLDS";
            q ^ "0 Aggregation " ^ string_of_int (List.length counted) ^ " contributors ["
            ^ String.concat "; " (List.map (fun w -> "\"" ^ w ^ "\" empty reached") counted) ^ "]" ]
      in
      check ("WV-5.12-keurgemiddelde " ^ file) (got = expected);
      if got <> expected then List.iter (Printf.printf "  got: %s\n") got)
    [ ("case1-stop-voor-de-deling.json", "FAILS", {|"W1" false reached; "W2" false unreached|}, [ "W1"; "W2" ]);
      ("case2-beide-leden-gelezen.json", "HOLDS", {|"W1" true reached; "W2" true reached|}, [ "W1"; "W2" ]);
      ( "case3-het-tweede-lid-beslist.json", "FAILS",
        {|"W1" true reached; "W2" false reached; "W3" false unreached|}, [ "W1"; "W2"; "W3" ] );
      ("case4-de-deling-bereikt.json", "FAILS", {|"W1" true reached; "W2" false reached|}, [ "W1"; "W2" ]) ]

(* WV-5.12-fout-in-een-kwantorvouw: the fault arms of the table, which no
   golden reaches (a member after the stop that would fault is a fault the
   ASP backend raises while it translates the document, or not at all where
   only its parameter guard is lazy, and the Drools backend reads an unbound
   parameter as leeg), so the readings are taken on the input world of a
   document whose matrix ends in an Unresolved condition (EF-019). On P1 the
   fold stops at W1 and the matrix faults on W2, which the session did not
   reach: the fold has its verdict, the reading its status, and W2 carries
   the fault, not reached. On P2 the matrix faults on W3, which the session
   reached: the fold faults, and W4 after it has its verdict, not reached.
   The second rule's filter reaches the Unresolved on a member that is not
   gekeurd (W1, W4) and faults: its contributors are not settled, and the
   node carries none. *)
let () =
  let doc =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Ploeg","animate":false,"plural":"Ploegen","attributes":[],"characteristics":[{"node":"Characteristic","name":"inzetbaar","kind":"adjectival"},{"node":"Characteristic","name":"bemand","kind":"adjectival"}]},{"node":"ObjectType","name":"Strandwacht","animate":true,"plural":"Strandwachten","attributes":[],"characteristics":[{"node":"Characteristic","name":"gekeurd","kind":"adjectival"}]}],"factTypes":[{"node":"FactType","name":"ploegindeling","reciprocal":false,"roles":[{"node":"Role","name":"ploeg","objectType":"Ploeg","many":false},{"node":"Role","name":"wacht","objectType":"Strandwacht","plural":"wachten","many":true}]}],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"inzetbaar","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"inzetbaar","subjectType":"Ploeg","source":{"snippet":"Een Ploeg is inzetbaar"}},"value":{"node":"Literal","value":{"boolean":true},"type":{"node":"Boolean"},"source":{"snippet":"waar"}},"condition":{"node":"QuantifierFold","quantifier":"all","elementType":"Strandwacht","test":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"CharacteristicCheck","characteristic":"gekeurd","negated":false,"source":{"snippet":"hij is gekeurd"}},{"node":"Unresolved","source":{"snippet":"zijn keuring is onleesbaar"}}],"source":{"snippet":"hij aan alle volgende voorwaarden voldoet"}},"source":{"snippet":"alle wachten van de Ploeg aan alle volgende voorwaarden voldoen"},"sourceRole":{"node":"RoleBinding","factType":"ploegindeling","role":"wacht"}},"initialisation":false}]},{"node":"Rule","name":"bemand","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"bemand","subjectType":"Ploeg","source":{"snippet":"Een Ploeg is bemand"}},"value":{"node":"Literal","value":{"boolean":true},"type":{"node":"Boolean"},"source":{"snippet":"waar"}},"condition":{"node":"QuantifierFold","quantifier":"all","elementType":"Strandwacht","filter":{"node":"Compound","quantifier":"atLeast","threshold":1,"parts":[{"node":"CharacteristicCheck","characteristic":"gekeurd","negated":false,"source":{"snippet":"die gekeurd zijn"}},{"node":"Unresolved","source":{"snippet":"wier keuring onleesbaar is"}}],"source":{"snippet":"die aan ten minste 1 van de volgende voorwaarden voldoen"}},"test":{"node":"CharacteristicCheck","characteristic":"gekeurd","negated":false,"source":{"snippet":"gekeurd zijn"}},"source":{"snippet":"alle wachten van de Ploeg die gekeurd of onleesbaar zijn gekeurd zijn"},"sourceRole":{"node":"RoleBinding","factType":"ploegindeling","role":"wacht"}},"initialisation":false}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let case_data =
    {|{ "format": "uchar-case-v0", "asOfDate": null,
  "input": { "instances": [
    { "label": "P1", "objectType": "Ploeg", "attributes": {}, "roles": { "wachten": [ "W1", "W2" ] }, "characteristics": [] },
    { "label": "P2", "objectType": "Ploeg", "attributes": {}, "roles": { "wachten": [ "W3", "W4" ] }, "characteristics": [] },
    { "label": "W1", "objectType": "Strandwacht", "attributes": {}, "roles": {}, "characteristics": [] },
    { "label": "W2", "objectType": "Strandwacht", "attributes": {}, "roles": {}, "characteristics": [ "gekeurd" ] },
    { "label": "W3", "objectType": "Strandwacht", "attributes": {}, "roles": {}, "characteristics": [ "gekeurd" ] },
    { "label": "W4", "objectType": "Strandwacht", "attributes": {}, "roles": {}, "characteristics": [] } ],
    "parameters": {} },
  "expected": [ { "kind": "consistent" } ] }|}
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv512" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" doc and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      let got = readings_of ~evaluate:false irf casef in
      let line form subject cell = Printf.sprintf "/rules/rules/%d/versions/0 \"%s\" %s" form subject cell in
      let expected =
        List.sort compare
          [ line 0 "P1" "FAILS";
            line 0 "P1" {|0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]|};
            line 0 "P2" "FAULTS";
            line 0 "P2"
              {|0 QuantifierFold fault EF-019 contributors ["W3" fault EF-019 reached; "W4" false unreached]|};
            line 1 "P1" "FAULTS";
            line 1 "P1" "0 QuantifierFold fault EF-019";
            line 1 "P2" "FAULTS";
            line 1 "P2" "0 QuantifierFold fault EF-019" ]
      in
      check "WV-5.12-fout-in-een-kwantorvouw" (got = expected);
      if got <> expected then List.iter (Printf.printf "  got: %s\n") got)

(* WV-5.12-opgave-bedrag: the chapter's worked example of a form without a
   condition, verbatim, the golden jutterswet-opgave-bedrag. The rule
   verzekerde waarde has no condition: it holds on every Vondst, with the
   nodes of its value from 0. The initialisation default of the opgave group
   holds where it is at its turn (case 5: nothing above it decides, the input
   leaves the waarde empty) and is displaced where a higher default decided
   (case 1) or the input fills the target (case 7). The lines of the two forms
   in each case's canonical text (card readings-of-unconditional-forms). *)
let () =
  let golden = "corpus/golden/jutterswet-opgave-bedrag/" in
  let rule = "/rules/rules/2/versions/0 \"V\" " and init = "/rules/prioritizedDefaults/0/defaults/2 \"V\" " in
  let of_forms lines =
    List.filter
      (fun l -> String.starts_with ~prefix:rule l || String.starts_with ~prefix:init l)
      lines
  in
  List.iter
    (fun (file, default_lines, insured, waarde) ->
      let got = of_forms (readings_of (corpus_file (golden ^ "ir.json")) (corpus_file (golden ^ file))) in
      let expected =
        List.sort compare
          (List.map (fun c -> init ^ c) default_lines
          @ List.map (fun c -> rule ^ c)
              [ "HOLDS"; "0 Binary " ^ insured ^ " \"EUR\""; "1 AttributeReference " ^ waarde ^ " \"EUR\"";
                "2 Literal 2.5 \"EUR\"" ])
      in
      check ("WV-5.12-opgave-bedrag " ^ file) (got = expected);
      if got <> expected then List.iter (Printf.printf "  got: %s\n") got)
    [ ("case1-geen-opgave.json", [ "DISPLACED" ], "112.5", "110");
      ("case5-niets.json", [ "HOLDS"; "0 Literal 25 \"EUR\"" ], "27.5", "25");
      ("case7-doel-zelf-zonder-feiten.json", [ "DISPLACED" ], "82.5", "80") ]

(* WV-5.12-gedurende-clip: the gedurende clause of a temporal total (ch. 3.6)
   is read as the part of the timeline in which it holds: the true periods of
   a characteristic, also beyond the totalled timeline; negated, the periods
   outside them within each segment of that timeline; a composite, the
   intersection of its members' periods, each member read on its own; each a
   period without a value; and a comparison of another timeline, the
   segments of that timeline where it holds, with their values, cut where the
   totalled timeline's segments are cut. Four rules without a condition on two
   Jutters, J2 without a wachtvergoeding. The same document is the Drools
   backend's ReadingsWitnessTest
   (src/test/resources/readings/temporal-total-clip), each line written by
   hand from ch. 3.6. *)
let () =
  let doc =
    {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Jutter","animate":true,"plural":"Jutters","attributes":[{"node":"Attribute","name":"wachtvergoeding per maand","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Attribute","name":"borgvergoeding per maand","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Attribute","name":"wachtsom","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"stilsom","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"tegensom","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}},{"node":"Attribute","name":"borgsom","datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"}}],"characteristics":[{"node":"Characteristic","name":"wachtplichtig","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}},{"node":"Characteristic","name":"aangesteld","kind":"adjectival","timeline":{"node":"TimelineFacet","granularity":"month","count":1}}]}],"factTypes":[],"parameters":[],"domains":[],"unitSystems":[],"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"wachtsom van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"wachtsom","subjectType":"Jutter","source":{"snippet":"De wachtsom van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Jutter","filter":{"node":"CharacteristicCheck","characteristic":"wachtplichtig","negated":false,"source":{"snippet":"gedurende de tijd dat hij wachtplichtig is"}},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is"}},"initialisation":false,"presentation":"equation"}]},{"node":"Rule","name":"stilsom van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"stilsom","subjectType":"Jutter","source":{"snippet":"De stilsom van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Jutter","filter":{"node":"CharacteristicCheck","characteristic":"aangesteld","negated":true,"source":{"snippet":"gedurende de tijd dat hij niet aangesteld is"}},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij niet aangesteld is"}},"initialisation":false,"presentation":"equation"}]},{"node":"Rule","name":"tegensom van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"tegensom","subjectType":"Jutter","source":{"snippet":"De tegensom van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Jutter","filter":{"node":"Compound","quantifier":"all","threshold":1,"parts":[{"node":"CharacteristicCheck","characteristic":"wachtplichtig","negated":false,"source":{"snippet":"hij wachtplichtig is"}},{"node":"CharacteristicCheck","characteristic":"aangesteld","negated":true,"source":{"snippet":"hij niet aangesteld is"}}],"source":{"snippet":"gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - hij niet aangesteld is"}},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - hij niet aangesteld is"}},"initialisation":false,"presentation":"equation"}]},{"node":"Rule","name":"borgsom van een jutter","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference","attribute":"borgsom","subjectType":"Jutter","source":{"snippet":"De borgsom van een Jutter"}},"value":{"node":"Aggregation","kind":"total","arguments":[{"node":"AttributeReference","attribute":"wachtvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn wachtvergoeding per maand"}}],"elementType":"Jutter","filter":{"node":"Comparison","op":"greater","left":{"node":"AttributeReference","attribute":"borgvergoeding per maand","subjectType":"Jutter","source":{"snippet":"zijn borgvergoeding per maand"}},"right":{"node":"Literal","value":{"number":"10"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"10"}},"source":{"snippet":"gedurende de tijd dat zijn borgvergoeding per maand groter is dan 10"}},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified"},"source":{"snippet":"het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn borgvergoeding per maand groter is dan 10"}},"initialisation":false,"presentation":"equation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}
  in
  let case_data =
    {|{"format":"uchar-case-v0","asOfDate":null,"input":{"instances":[{"label":"J1","objectType":"Jutter","attributes":{"wachtvergoeding per maand":{"timeline":[{"from":"2024-01-01","to":"2024-07-01","value":{"number":"10"}},{"from":"2024-07-01","to":"2025-01-01","value":{"number":"20"}}]},"borgvergoeding per maand":{"timeline":[{"from":"2023-07-01","to":"2024-04-01","value":{"number":"15"}},{"from":"2024-04-01","to":"2025-03-01","value":{"number":"5"}}]}},"roles":{},"characteristics":[],"characteristicTimelines":{"wachtplichtig":[{"from":"2024-03-01","to":"2025-03-01"}],"aangesteld":[{"from":"2024-05-01","to":"2025-01-01"}]}},{"label":"J2","objectType":"Jutter","attributes":{"borgvergoeding per maand":{"timeline":[{"from":"2024-01-01","to":"2024-07-01","value":{"number":"12"}}]}},"roles":{},"characteristics":[],"characteristicTimelines":{"wachtplichtig":[{"from":"2024-01-01","to":"2024-04-01"}]}}],"parameters":{}},"expected":[{"kind":"consistent"}]}|}
  in
  let write suffix body_text =
    let f = Filename.temp_file "ref-wv512" suffix in
    let oc = open_out f in
    output_string oc body_text;
    close_out oc;
    f
  in
  let irf = write ".ir.json" doc and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      let got = readings_of irf casef in
      let line form subject cell = Printf.sprintf "/rules/rules/%d/versions/0 \"%s\" %s" form subject cell in
      let whole = "1 AttributeReference [[2024-01-01 2024-07-01 10], [2024-07-01 2025-01-01 20]]" in
      let expected =
        List.sort compare
          [ line 0 "J1" "0 Aggregation 160"; line 0 "J1" whole;
            line 0 "J1" "2 CharacteristicCheck [[2024-03-01 2025-03-01 empty]]"; line 0 "J1" "HOLDS";
            line 0 "J2" "0 Aggregation 0"; line 0 "J2" "1 AttributeReference empty";
            line 0 "J2" "2 CharacteristicCheck [[2024-01-01 2024-04-01 empty]]"; line 0 "J2" "HOLDS";
            line 1 "J1" "0 Aggregation 40"; line 1 "J1" whole;
            line 1 "J1" "2 CharacteristicCheck [[2024-01-01 2024-05-01 empty]]"; line 1 "J1" "HOLDS";
            line 1 "J2" "0 Aggregation 0"; line 1 "J2" "1 AttributeReference empty";
            line 1 "J2" "2 CharacteristicCheck []"; line 1 "J2" "HOLDS";
            line 2 "J1" "0 Aggregation 20"; line 2 "J1" whole;
            line 2 "J1" "2 Compound [[2024-03-01 2024-05-01 empty]]";
            line 2 "J1" "3 CharacteristicCheck [[2024-03-01 2025-03-01 empty]]";
            line 2 "J1" "4 CharacteristicCheck [[2024-01-01 2024-05-01 empty]]"; line 2 "J1" "HOLDS";
            line 2 "J2" "0 Aggregation 0"; line 2 "J2" "1 AttributeReference empty"; line 2 "J2" "2 Compound []";
            line 2 "J2" "3 CharacteristicCheck [[2024-01-01 2024-04-01 empty]]";
            line 2 "J2" "4 CharacteristicCheck []"; line 2 "J2" "HOLDS";
            line 3 "J1" "0 Aggregation 30"; line 3 "J1" whole;
            line 3 "J1" "2 Comparison [[2023-07-01 2024-01-01 15], [2024-01-01 2024-04-01 15]]";
            line 3 "J1" "3 AttributeReference [[2023-07-01 2024-04-01 15], [2024-04-01 2025-03-01 5]]";
            line 3 "J1" "4 Literal 10"; line 3 "J1" "HOLDS";
            line 3 "J2" "0 Aggregation 0"; line 3 "J2" "1 AttributeReference empty";
            line 3 "J2" "2 Comparison [[2024-01-01 2024-07-01 12]]";
            line 3 "J2" "3 AttributeReference [[2024-01-01 2024-07-01 12]]"; line 3 "J2" "4 Literal 10";
            line 3 "J2" "HOLDS" ]
      in
      check "WV-5.12-gedurende-clip" (got = expected);
      if got <> expected then List.iter (Printf.printf "  got: %s\n") got)

(* --- ch. 6.2/6.4: the instance-ordered cycle -------------------------------- *)

(* The document and the case as files, loaded the way the runner loads them,
   the rules of the document in the order [order] gives (indices into its
   rules); [f] gets the loaded case and the evaluation, deferred so that a
   fault at load or at evaluation is caught where it is pinned. *)
let with_instance_case ir ~order case_data f =
  let doc =
    match Yojson.Safe.from_string ir with
    | `Assoc kvs ->
        `Assoc
          (List.map
             (fun (k, v) ->
               match (k, v) with
               | "rules", `Assoc rkvs ->
                   ( k,
                     `Assoc
                       (List.map
                          (fun (rk, rv) ->
                            match (rk, rv) with
                            | "rules", `List rs -> (rk, `List (List.map (List.nth rs) order))
                            | _ -> (rk, rv))
                          rkvs) )
               | _ -> (k, v))
             kvs)
    | _ -> invalid_arg "with_instance_case: the document is no object"
  in
  let write suffix text =
    let file = Filename.temp_file "ref-wv64-instance" suffix in
    let oc = open_out file in
    output_string oc text;
    close_out oc;
    file
  in
  let irf = write ".ir.json" (Yojson.Safe.to_string doc) and casef = write ".case.json" case_data in
  Fun.protect
    ~finally:(fun () -> List.iter Sys.remove [ irf; casef ])
    (fun () ->
      let d = Ir_v0.Loader.load_file irf in
      f
        (Ir_v0.Checks.all (Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier irf))
        (fun () -> Case.load_file d casef)
        (fun (c : Case.t) ->
          Eval.evaluate
            { Eval.doc = d; params = c.params; param_tls = c.param_tls; as_of = c.as_of;
              locals = World.SMap.empty; btypes = World.SMap.empty; subj = None }
            c.world))

(* the rules of R1 below: "restant", "einddag", "einddag zonder restant" *)
let wv64_r1 =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Vergunning",
"animate":false,"plural":"Vergunningen","attributes":[{"node":"Attribute","name":"stopdag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"basiseinddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"einddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"restant",
"datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}}],
"characteristics":[]}],"factTypes":[{"node":"FactType","name":"vorige vergunning",
"reciprocal":false,"roles":[{"node":"Role","name":"vergunning","objectType":"Vergunning",
"many":false},{"node":"Role","name":"vorige vergunning","objectType":"Vergunning","many":false}]}],
"parameters":[],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Tijd",
"units":[{"node":"Unit","abbreviation":"dg","system":"Tijd","canonical":"dg",
"factorToCanonical":{"num":1,"den":1},"spellings":["dag"]}]}],"dimensions":[],"timelines":[],
"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"restant",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning"},"value":{"node":"BuiltinCall","kind":"duration",
"arguments":[{"node":"AttributeReference","attribute":"stopdag","subjectType":"Vergunning"},
{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"}],
"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}},
"condition":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"Predicate",
"kind":"filled","operand":{"node":"AttributeReference","attribute":"stopdag",
"subjectType":"Vergunning"},"negated":false,"digits":0},{"node":"Predicate","kind":"filled",
"operand":{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"},
"negated":false,"digits":0},{"node":"Comparison","op":"earlier","left":{"node":"AttributeReference",
"attribute":"stopdag","subjectType":"Vergunning"},"right":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"}}]},"initialisation":false,
"presentation":"calculation"}]},{"node":"Rule","name":"einddag",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"},"value":{"node":"Binary","op":"plus",
"left":{"node":"AttributeReference","attribute":"basiseinddag","subjectType":"Vergunning"},
"right":{"node":"AttributeReference","attribute":"restant","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vorige vergunning","role":"vorige vergunning"}]},
"type":{"node":"Date","precision":"day"}},"condition":{"node":"Predicate","kind":"filled",
"operand":{"node":"AttributeReference","attribute":"restant","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vorige vergunning","role":"vorige vergunning"}]},
"negated":false,"digits":0},"initialisation":false,"presentation":"calculation"}]},{"node":"Rule",
"name":"einddag zonder restant","versions":[{"validity":{"node":"Validity"},
"target":{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"},
"value":{"node":"AttributeReference","attribute":"basiseinddag","subjectType":"Vergunning"},
"condition":{"node":"Predicate","kind":"empty","operand":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning","path":[{"node":"RoleBinding",
"factType":"vorige vergunning","role":"vorige vergunning"}]},"negated":false,"digits":0},
"initialisation":false,"presentation":"equation"}]}],"dayKindDefinitions":[],"objectCreations":[],
"allocations":[],"consistencies":[],"uniquenesses":[],"factCreations":[],
"startPointDeterminations":[],"ruleGroups":[]},"diagnostics":[]}|}

(* The ch. 6.4 worked example of the instance-ordered cycle (WV-6.4-i). The
   einddag of a Vergunning is its basiseinddag plus the restant of its vorige
   vergunning when that is filled, else its basiseinddag; its restant is the
   days from its stopdag to its einddag when both are filled and the stopdag
   is earlier. V1 (stopdag 01-03-2026, basiseinddag 01-06-2026, no
   predecessor) and V2 (basiseinddag 01-12-2026, vorige vergunning V1): einddag
   V1 01-06-2026 (the basiseinddag: no predecessor), restant V1 92, einddag V2
   03-03-2027. The verifier admits the cycle (restant -> einddag closes it only
   over the predecessor), and the outcome is the same in every order of the
   three rules and of the two objects in the case. *)
let () =
  let case_data ~v2_first =
    let v1 =
      {|{"label": "V1", "objectType": "Vergunning", "attributes": {"stopdag": {"date": "2026-03-01"}, "basiseinddag": {"date": "2026-06-01"}}, "roles": {}, "characteristics": []}|}
    and v2 =
      {|{"label": "V2", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-12-01"}}, "roles": {"vorige vergunning": ["V1"]}, "characteristics": []}|}
    in
    Printf.sprintf
      {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [%s], "parameters": {}}, "expected": [
         {"kind": "attributeValue", "label": "V1", "attribute": "einddag", "value": {"date": "2026-06-01"}},
         {"kind": "attributeValue", "label": "V1", "attribute": "restant", "value": {"number": "92"}},
         {"kind": "attributeValue", "label": "V2", "attribute": "einddag", "value": {"date": "2027-03-03"}},
         {"kind": "attributeEmpty", "label": "V2", "attribute": "restant"},
         {"kind": "consistent"}]}|}
      (if v2_first then v2 ^ ", " ^ v1 else v1 ^ ", " ^ v2)
  in
  List.iter
    (fun (order, v2_first) ->
      with_instance_case wv64_r1 ~order (case_data ~v2_first) (fun violations load evaluate ->
          let tag =
            Printf.sprintf "WV-6.4-i-%s%s" (String.concat "" (List.map string_of_int order))
              (if v2_first then "-v2-eerst" else "")
          in
          check (tag ^ "-verifier-schoon") (violations = []);
          let c = load () in
          check tag (Case.check c (evaluate c) = [])))
    [ ([ 0; 1; 2 ], false); ([ 2; 1; 0 ], false); ([ 1; 0; 2 ], true); ([ 2; 0; 1 ], true) ]

(* WV-6.4-i without a predecessor: the einddag is the basiseinddag *)
let () =
  with_instance_case wv64_r1 ~order:[ 0; 1; 2 ]
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
       {"label": "V1", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-06-01"}}, "roles": {}, "characteristics": []}],
       "parameters": {}}, "expected": [
       {"kind": "attributeValue", "label": "V1", "attribute": "einddag", "value": {"date": "2026-06-01"}},
       {"kind": "attributeEmpty", "label": "V1", "attribute": "restant"}]}|}
    (fun _ load evaluate ->
      let c = load () in
      check "WV-6.4-i-zonder-voorganger" (Case.check c (evaluate c) = []))

(* WV-6.4-j: a characteristic on the instance-ordered cycle. "zonder
   verlenging" holds for a Vergunning whose vorige vergunning has a leeg
   restant (a negative shifted edge), "einddag zonder restant" writes the
   basiseinddag on a Vergunning zonder verlenging, and einddag and restant
   are R1's. A characteristic is never withdrawn (ch. 5.3): evaluated on V2
   before the restant of V1 is computed, it would hold for V2 for good, and
   two writers of the einddag of V2 would disagree. Measured 2026-10-22 with
   the depth order switched off: EF-016, "einddag zonder restant" writes
   2026-12-01 and "einddag" 2027-03-03. With it, in either order of the four
   rules: V1 zonder verlenging, V2 not, einddag V2 03-03-2027, the answer of
   the ASP backend as well. *)
let wv64_kenmerk =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Vergunning",
"animate":false,"plural":"Vergunningen","attributes":[{"node":"Attribute","name":"stopdag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"basiseinddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"einddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"restant",
"datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}}],
"characteristics":[{"node":"Characteristic","name":"zonder verlenging","kind":"adjectival"}]}],
"factTypes":[{"node":"FactType","name":"vorige vergunning","reciprocal":false,
"roles":[{"node":"Role","name":"vergunning","objectType":"Vergunning","many":false},{"node":"Role",
"name":"vorige vergunning","objectType":"Vergunning","many":false}]}],"parameters":[],"domains":[],
"unitSystems":[{"node":"UnitSystem","name":"Tijd","units":[{"node":"Unit","abbreviation":"dg",
"system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1},"spellings":["dag"]}]}],
"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule",
"name":"zonder verlenging","versions":[{"validity":{"node":"Validity"},
"target":{"node":"AttributeReference","attribute":"zonder verlenging","subjectType":"Vergunning"},
"value":{"node":"Literal","value":{"boolean":true},"type":{"node":"Boolean"}},
"condition":{"node":"Predicate","kind":"empty","operand":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning","path":[{"node":"RoleBinding",
"factType":"vorige vergunning","role":"vorige vergunning"}]},"negated":false,"digits":0},
"initialisation":false}]},{"node":"Rule","name":"einddag zonder restant",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"},"value":{"node":"AttributeReference",
"attribute":"basiseinddag","subjectType":"Vergunning"},"condition":{"node":"CharacteristicCheck",
"characteristic":"zonder verlenging","negated":false},"initialisation":false,
"presentation":"equation"}]},{"node":"Rule","name":"einddag",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"},"value":{"node":"Binary","op":"plus",
"left":{"node":"AttributeReference","attribute":"basiseinddag","subjectType":"Vergunning"},
"right":{"node":"AttributeReference","attribute":"restant","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vorige vergunning","role":"vorige vergunning"}]},
"type":{"node":"Date","precision":"day"}},"condition":{"node":"Predicate","kind":"filled",
"operand":{"node":"AttributeReference","attribute":"restant","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vorige vergunning","role":"vorige vergunning"}]},
"negated":false,"digits":0},"initialisation":false,"presentation":"calculation"}]},{"node":"Rule",
"name":"restant","versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning"},"value":{"node":"BuiltinCall","kind":"duration",
"arguments":[{"node":"AttributeReference","attribute":"stopdag","subjectType":"Vergunning"},
{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"}],
"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}},
"condition":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"Predicate",
"kind":"filled","operand":{"node":"AttributeReference","attribute":"stopdag",
"subjectType":"Vergunning"},"negated":false,"digits":0},{"node":"Predicate","kind":"filled",
"operand":{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"},
"negated":false,"digits":0},{"node":"Comparison","op":"earlier","left":{"node":"AttributeReference",
"attribute":"stopdag","subjectType":"Vergunning"},"right":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"}}]},"initialisation":false,
"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],
"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],
"ruleGroups":[]},"diagnostics":[]}|}

let () =
  let case_data =
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
       {"label": "V2", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-12-01"}}, "roles": {"vorige vergunning": ["V1"]}, "characteristics": []},
       {"label": "V1", "objectType": "Vergunning", "attributes": {"stopdag": {"date": "2026-03-01"}, "basiseinddag": {"date": "2026-06-01"}}, "roles": {}, "characteristics": []}],
       "parameters": {}}, "expected": [
       {"kind": "characteristicPresent", "label": "V1", "characteristic": "zonder verlenging"},
       {"kind": "characteristicAbsent", "label": "V2", "characteristic": "zonder verlenging"},
       {"kind": "attributeValue", "label": "V1", "attribute": "einddag", "value": {"date": "2026-06-01"}},
       {"kind": "attributeValue", "label": "V1", "attribute": "restant", "value": {"number": "92"}},
       {"kind": "attributeValue", "label": "V2", "attribute": "einddag", "value": {"date": "2027-03-03"}},
       {"kind": "consistent"}]}|}
  in
  List.iter
    (fun order ->
      with_instance_case wv64_kenmerk ~order case_data (fun violations load evaluate ->
          let tag = "WV-6.4-j-" ^ String.concat "" (List.map string_of_int order) in
          check (tag ^ "-verifier-schoon") (violations = []);
          let c = load () in
          check tag (Case.check c (evaluate c) = [])))
    [ [ 0; 1; 2; 3 ]; [ 3; 2; 1; 0 ] ]

(* WV-6.4-k: a cycle in the links of the switch relation is refused while the
   case is read, before any rule fires (EF-033, ch. 5.7b): V1's vorige
   vergunning is V2 and V2's is V1. The cycle is listed from its smallest
   label along the predecessor's role. *)
let () =
  with_instance_case wv64_r1 ~order:[ 0; 1; 2 ]
    {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
       {"label": "V2", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-12-01"}}, "roles": {"vorige vergunning": ["V1"]}, "characteristics": []},
       {"label": "V1", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-06-01"}}, "roles": {"vorige vergunning": ["V2"]}, "characteristics": []}],
       "parameters": {}}, "expected": [{"kind": "consistent"}]}|}
    (fun _ load _ ->
      match load () with
      | (_ : Case.t) -> check "WV-6.4-k-schakelkring" false
      | exception Case.Error { code; params; _ } ->
          check "WV-6.4-k-schakelkring"
            (code = Some "EF-033"
            && params = [ ("factType", "vorige vergunning"); ("cycle", "V1, V2") ]))

(* WV-6.4-l: a defaults group on the instance-ordered cycle (WF-051 over the
   graph without the shifted edges). The einddag of a Vergunning may be given
   ("mag opgegeven worden"): the group "opgave boven berekening einddag" puts
   the opgegeven einddag above the berekende einddag, which is the basiseinddag
   plus the restant of the vorige vergunning; the restant reads the einddag.
   The group's reads are negative edges, local ones here. V1 (no predecessor),
   V2 with an opgegeven einddag 01-01-2027 and V3: berekende einddag V2
   03-03-2027 but einddag V2 01-01-2027, restant V2 122 dagen (from 01-09-2026),
   einddag V3 01-06-2027 + 122 dagen = 01-10-2027; the consistency the frontend
   adds for a given value that differs from the computed one holds for V2. The
   ASP backend and both Drools branches answer the same (measured 2026-10-22). *)
let wv64_opgave =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Vergunning",
"animate":false,"plural":"Vergunningen","attributes":[{"node":"Attribute","name":"stopdag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"basiseinddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"einddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"opgegeven einddag",
"datatype":{"node":"Date","precision":"day"},"origin":{"form":"inputAboveComputation",
"from":{"kind":"attribute","owner":"Vergunning","name":"einddag"}}},{"node":"Attribute",
"name":"berekende einddag","datatype":{"node":"Date","precision":"day"},
"origin":{"form":"inputAboveComputation","from":{"kind":"attribute","owner":"Vergunning",
"name":"einddag"}}},{"node":"Attribute","name":"restant","datatype":{"node":"Number","whole":true,
"decimals":0,"sign":"unspecified","unit":{"node":"Unit","abbreviation":"dg","system":"Tijd",
"canonical":"dg","factorToCanonical":{"num":1,"den":1}}}}],
"characteristics":[{"node":"Characteristic","name":"afwijkende opgave einddag","kind":"possessive",
"origin":{"form":"inputAboveComputation","from":{"kind":"attribute","owner":"Vergunning",
"name":"einddag"}}}]}],"factTypes":[{"node":"FactType","name":"vorige vergunning",
"reciprocal":false,"roles":[{"node":"Role","name":"vergunning","objectType":"Vergunning",
"many":false},{"node":"Role","name":"vorige vergunning","objectType":"Vergunning","many":false}]}],
"parameters":[],"domains":[],"unitSystems":[{"node":"UnitSystem","name":"Tijd",
"units":[{"node":"Unit","abbreviation":"dg","system":"Tijd","canonical":"dg",
"factorToCanonical":{"num":1,"den":1},"spellings":["dag"]}]}],"dimensions":[],"timelines":[],
"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"restant",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning"},"value":{"node":"BuiltinCall","kind":"duration",
"arguments":[{"node":"AttributeReference","attribute":"stopdag","subjectType":"Vergunning"},
{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"}],
"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}},
"condition":{"node":"Compound","quantifier":"all","threshold":0,"parts":[{"node":"Predicate",
"kind":"filled","operand":{"node":"AttributeReference","attribute":"stopdag",
"subjectType":"Vergunning"},"negated":false,"digits":0},{"node":"Predicate","kind":"filled",
"operand":{"node":"AttributeReference","attribute":"einddag","subjectType":"Vergunning"},
"negated":false,"digits":0},{"node":"Comparison","op":"earlier","left":{"node":"AttributeReference",
"attribute":"stopdag","subjectType":"Vergunning"},"right":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Vergunning"}}]},"initialisation":false,
"presentation":"calculation"}]},{"node":"Rule","name":"einddag",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"berekende einddag","subjectType":"Vergunning"},"value":{"node":"Binary","op":"plus",
"left":{"node":"AttributeReference","attribute":"basiseinddag","subjectType":"Vergunning"},
"right":{"node":"AttributeReference","attribute":"restant","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vorige vergunning","role":"vorige vergunning"}]},
"type":{"node":"Date","precision":"day"}},"initialisation":false,"presentation":"calculation"}]},
{"node":"Rule","name":"afwijking opgave einddag","versions":[{"validity":{"node":"Validity"},
"target":{"node":"AttributeReference","attribute":"afwijkende opgave einddag",
"subjectType":"Vergunning"},"value":{"node":"Literal","value":{"boolean":true},
"type":{"node":"Boolean"}},"condition":{"node":"Compound","quantifier":"all","threshold":0,
"parts":[{"node":"Predicate","kind":"filled","operand":{"node":"AttributeReference",
"attribute":"opgegeven einddag","subjectType":"Vergunning"},"negated":false,"digits":0},
{"node":"Predicate","kind":"filled","operand":{"node":"AttributeReference",
"attribute":"berekende einddag","subjectType":"Vergunning"},"negated":false,"digits":0},
{"node":"Comparison","op":"notEqual","left":{"node":"AttributeReference",
"attribute":"opgegeven einddag","subjectType":"Vergunning"},"right":{"node":"AttributeReference",
"attribute":"berekende einddag","subjectType":"Vergunning"}}]},"initialisation":false}],
"origin":{"form":"inputAboveComputation","from":{"kind":"attribute","owner":"Vergunning",
"name":"einddag"}}}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],
"consistencies":[{"node":"Consistency","name":"controle opgave einddag",
"versions":[{"subject":"Vergunning","assertion":{"node":"CharacteristicCheck",
"characteristic":"afwijkende opgave einddag","negated":true},"validity":{"node":"Validity"}}],
"origin":{"form":"inputAboveComputation","from":{"kind":"attribute","owner":"Vergunning",
"name":"einddag"}}}],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],
"ruleGroups":[],"prioritizedDefaults":[{"node":"PrioritizedDefaults",
"name":"opgave boven berekening einddag","subject":"Vergunning","attribute":"einddag",
"defaults":[{"node":"Default","justification":{"node":"Compound","quantifier":"all","threshold":0,
"parts":[]},"consequence":{"node":"AttributeReference","attribute":"opgegeven einddag",
"subjectType":"Vergunning"}},{"node":"Default","justification":{"node":"Compound",
"quantifier":"all","threshold":0,"parts":[]},"consequence":{"node":"AttributeReference",
"attribute":"berekende einddag","subjectType":"Vergunning"}}],"validity":{"node":"Validity"},
"origin":{"form":"inputAboveComputation","from":{"kind":"attribute","owner":"Vergunning",
"name":"einddag"}}}]},"diagnostics":[]}|}

let () =
  List.iter
    (fun order ->
      with_instance_case wv64_opgave ~order {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [{"label": "V3", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2027-06-01"}}, "roles": {"vorige vergunning": ["V2"]}, "characteristics": []}, {"label": "V2", "objectType": "Vergunning", "attributes": {"stopdag": {"date": "2026-09-01"}, "basiseinddag": {"date": "2026-12-01"}, "opgegeven einddag": {"date": "2027-01-01"}}, "roles": {"vorige vergunning": ["V1"]}, "characteristics": []}, {"label": "V1", "objectType": "Vergunning", "attributes": {"stopdag": {"date": "2026-03-01"}, "basiseinddag": {"date": "2026-06-01"}}, "roles": {}, "characteristics": []}], "parameters": {}}, "expected": [{"kind": "attributeValue", "label": "V1", "attribute": "einddag", "value": {"date": "2026-06-01"}}, {"kind": "attributeValue", "label": "V1", "attribute": "restant", "value": {"number": "92"}}, {"kind": "attributeValue", "label": "V2", "attribute": "berekende einddag", "value": {"date": "2027-03-03"}}, {"kind": "attributeValue", "label": "V2", "attribute": "einddag", "value": {"date": "2027-01-01"}}, {"kind": "attributeValue", "label": "V2", "attribute": "restant", "value": {"number": "122"}}, {"kind": "attributeValue", "label": "V3", "attribute": "einddag", "value": {"date": "2027-10-01"}}, {"kind": "inconsistent", "rule": "controle opgave einddag", "label": "V2"}]}|} (fun violations load evaluate ->
          let tag = "WV-6.4-l-" ^ String.concat "" (List.map string_of_int order) in
          check (tag ^ "-verifier-schoon") (violations = []);
          let c = load () in
          check tag (Case.check c (evaluate c) = [])))
    [ [ 0; 1; 2 ]; [ 2; 1; 0 ] ]

(* WV-6.4-m: a fold over the own satellites on the instance-ordered cycle (IR-SPEC
   ch. 5.3, the household of an object). A Vergunning has several termijnen; the
   overschot of a Termijn is the days from the verlengde einddag of its houder to
   its einddag when that is later, the restant of a Vergunning is the sum of the
   overschotten of its termijnen (a local read: every termijn stands at the depth
   of its houder, however many there are), and the verlengde einddag is the
   basiseinddag plus the restant of the vorige vergunning (a shifted read). V1
   (basiseinddag 01-06-2026, termijnen T1 to 11-06-2026 and T2 to 21-06-2026, no
   predecessor) and V2 (basiseinddag 01-12-2026, vorige vergunning V1, termijn T3
   to 10-01-2027): verlengde einddag V1 01-06-2026, overschot T1 10 and T2 20,
   restant V1 30, verlengde einddag V2 31-12-2026, overschot T3 10, restant V2
   10, in every order of the three rules tried and of the objects in the case.
   Until the card instance-cycle-household-reads the verifier refused the
   document: the fold reached a to-many role. *)
let wv64_household =
  {|{"format":"uchar-ir-v1","model":{"objectTypes":[{"node":"ObjectType","name":"Vergunning",
"animate":false,"plural":"Vergunningen","attributes":[{"node":"Attribute","name":"basiseinddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"verlengde einddag",
"datatype":{"node":"Date","precision":"day"}},{"node":"Attribute","name":"restant",
"datatype":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit",
"abbreviation":"dg","system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}}],
"characteristics":[]},{"node":"ObjectType","name":"Termijn","animate":false,"plural":"Termijnen",
"attributes":[{"node":"Attribute","name":"einddag","datatype":{"node":"Date","precision":"day"}},
{"node":"Attribute","name":"overschot","datatype":{"node":"Number","whole":true,"decimals":0,
"sign":"unspecified","unit":{"node":"Unit","abbreviation":"dg","system":"Tijd","canonical":"dg",
"factorToCanonical":{"num":1,"den":1}}},"plural":"overschotten"}],"characteristics":[]}],
"factTypes":[{"node":"FactType","name":"vorige vergunning","reciprocal":false,
"roles":[{"node":"Role","name":"vergunning","objectType":"Vergunning","many":false},{"node":"Role",
"name":"vorige vergunning","objectType":"Vergunning","many":false}]},{"node":"FactType",
"name":"vergunningstermijn","reciprocal":false,"roles":[{"node":"Role","name":"houder",
"objectType":"Vergunning","many":false},{"node":"Role","name":"termijn","objectType":"Termijn",
"plural":"termijnen","many":true}]}],"parameters":[],"domains":[],
"unitSystems":[{"node":"UnitSystem","name":"Tijd","units":[{"node":"Unit","abbreviation":"dg",
"system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1},"spellings":["dag"]}]}],
"dimensions":[],"timelines":[],"dayKinds":[]},"rules":{"rules":[{"node":"Rule","name":"overschot",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"overschot","subjectType":"Termijn"},"value":{"node":"BuiltinCall","kind":"duration",
"arguments":[{"node":"AttributeReference","attribute":"verlengde einddag","subjectType":"Vergunning",
"path":[{"node":"RoleBinding","factType":"vergunningstermijn","role":"houder"}]},
{"node":"AttributeReference","attribute":"einddag","subjectType":"Termijn"}],"type":{"node":"Number",
"whole":true,"decimals":0,"sign":"unspecified","unit":{"node":"Unit","abbreviation":"dg",
"system":"Tijd","canonical":"dg","factorToCanonical":{"num":1,"den":1}}}},
"condition":{"node":"Comparison","op":"earlier","left":{"node":"AttributeReference",
"attribute":"verlengde einddag","subjectType":"Vergunning","path":[{"node":"RoleBinding",
"factType":"vergunningstermijn","role":"houder"}]},"right":{"node":"AttributeReference",
"attribute":"einddag","subjectType":"Termijn"}},"initialisation":false,
"presentation":"calculation"}]},{"node":"Rule","name":"restant",
"versions":[{"validity":{"node":"Validity"},"target":{"node":"AttributeReference",
"attribute":"restant","subjectType":"Vergunning"},"value":{"node":"Aggregation","kind":"sum",
"arguments":[{"node":"AttributeReference","attribute":"overschot","subjectType":"Termijn"}],
"elementType":"Termijn","sourceRole":{"node":"RoleBinding","factType":"vergunningstermijn",
"role":"termijn"},"type":{"node":"Number","whole":true,"decimals":0,"sign":"unspecified",
"unit":{"node":"Unit","abbreviation":"dg","system":"Tijd","canonical":"dg",
"factorToCanonical":{"num":1,"den":1}}}},"initialisation":false,"presentation":"calculation"}]},
{"node":"Rule","name":"verlengde einddag","versions":[{"validity":{"node":"Validity"},
"target":{"node":"AttributeReference","attribute":"verlengde einddag","subjectType":"Vergunning"},
"value":{"node":"Binary","op":"plus","left":{"node":"AttributeReference","attribute":"basiseinddag",
"subjectType":"Vergunning"},"right":{"node":"AttributeReference","attribute":"restant",
"subjectType":"Vergunning","path":[{"node":"RoleBinding","factType":"vorige vergunning",
"role":"vorige vergunning"}]},"type":{"node":"Date","precision":"day"}},"initialisation":false,
"presentation":"calculation"}]}],"dayKindDefinitions":[],"objectCreations":[],"allocations":[],
"consistencies":[],"uniquenesses":[],"factCreations":[],"startPointDeterminations":[],
"ruleGroups":[]},"diagnostics":[]}|}

let () =
  let case_data ~reversed =
    let instances =
      [
        {|{"label": "V1", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-06-01"}}, "roles": {}, "characteristics": []}|};
        {|{"label": "T1", "objectType": "Termijn", "attributes": {"einddag": {"date": "2026-06-11"}}, "roles": {"houder": ["V1"]}, "characteristics": []}|};
        {|{"label": "T2", "objectType": "Termijn", "attributes": {"einddag": {"date": "2026-06-21"}}, "roles": {"houder": ["V1"]}, "characteristics": []}|};
        {|{"label": "V2", "objectType": "Vergunning", "attributes": {"basiseinddag": {"date": "2026-12-01"}}, "roles": {"vorige vergunning": ["V1"]}, "characteristics": []}|};
        {|{"label": "T3", "objectType": "Termijn", "attributes": {"einddag": {"date": "2027-01-10"}}, "roles": {"houder": ["V2"]}, "characteristics": []}|};
      ]
    in
    Printf.sprintf
      {|{"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [%s], "parameters": {}}, "expected": [
         {"kind": "attributeValue", "label": "V1", "attribute": "verlengde einddag", "value": {"date": "2026-06-01"}},
         {"kind": "attributeValue", "label": "T1", "attribute": "overschot", "value": {"number": "10"}},
         {"kind": "attributeValue", "label": "T2", "attribute": "overschot", "value": {"number": "20"}},
         {"kind": "attributeValue", "label": "V1", "attribute": "restant", "value": {"number": "30"}},
         {"kind": "attributeValue", "label": "V2", "attribute": "verlengde einddag", "value": {"date": "2026-12-31"}},
         {"kind": "attributeValue", "label": "T3", "attribute": "overschot", "value": {"number": "10"}},
         {"kind": "attributeValue", "label": "V2", "attribute": "restant", "value": {"number": "10"}},
         {"kind": "consistent"}]}|}
      (String.concat ", " (if reversed then List.rev instances else instances))
  in
  List.iter
    (fun (order, reversed) ->
      with_instance_case wv64_household ~order (case_data ~reversed) (fun violations load evaluate ->
          let tag =
            Printf.sprintf "WV-6.4-m-%s%s" (String.concat "" (List.map string_of_int order))
              (if reversed then "-reversed" else "")
          in
          check (tag ^ "-verifier-clean") (violations = []);
          let c = load () in
          check tag (Case.check c (evaluate c) = [])))
    [ ([ 0; 1; 2 ], false); ([ 2; 1; 0 ], false); ([ 1; 2; 0 ], true); ([ 2; 0; 1 ], true) ]

let () =
  if !failures > 0 then begin
    Printf.printf "%d failure(s)\n" !failures;
    exit 1
  end
  else Printf.printf "test_ref: all %d worked examples pass\n" !total
