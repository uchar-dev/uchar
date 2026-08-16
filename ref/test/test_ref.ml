(* The worked examples of RUNTIME-SEMANTICS ch. 1–2, verbatim, plus the
   ch. 4.6 allocation examples against the pure recurrence. Test names
   carry the WV numbering of the chapters (WV-<section>-<label>).

   Not covered here, deliberately: WV-1.2f (timeline rules — ch. 3, next
   card), WV-2.3 (static result-precision typing — a static property of
   the emitter/verifier, not of evaluation), and the ch. 5 examples that
   need full IR documents — the corpus cases exercise those (the
   penningdrempel case is the 5.7 trace verbatim). *)

open Uchar_ref

let failures = ref 0

let check name ok =
  if not ok then begin
    incr failures;
    Printf.printf "FAIL %s\n" name
  end

let q = Q.of_string
let n s = Value.Num (Value.q_of_lexeme s, Value.Unitless)
let leeg = Value.Empty

let expect_num name v s =
  match v with
  | Value.Num (got, _) -> check name (Q.equal got (Value.q_of_lexeme s))
  | other -> check (name ^ " (got " ^ Value.to_string other ^ ")") false

let expect_leeg name v = check name (v = Value.Empty)

let no_err = fun (_ : string) -> ()

let with_errs f =
  let errs = ref [] in
  let r = f (fun m -> errs := m :: !errs) in
  (r, List.length !errs)

let expect_fault name f =
  match f () with
  | exception Value.Fault _ -> check name true
  | _ -> check name false

(* --- ch. 1.2a: arithmetic operators over empty ------------------------- *)

let () =
  expect_num "WV-1.2a-plus-1" (Arith.add (n "5") leeg) "5";
  expect_num "WV-1.2a-plus-2" (Arith.add leeg leeg) "0";
  expect_num "WV-1.2a-min-1" (Arith.sub_min leeg (n "5")) "-5";
  expect_num "WV-1.2a-min-2" (Arith.sub_min (n "5") leeg) "5";
  expect_num "WV-1.2a-min-3" (Arith.sub_min leeg leeg) "0";
  expect_leeg "WV-1.2a-vm-1" (Arith.sub_verminderd leeg (n "5"));
  expect_num "WV-1.2a-vm-2" (Arith.sub_verminderd (n "5") leeg) "5";
  expect_leeg "WV-1.2a-vm-3" (Arith.sub_verminderd leeg leeg);
  expect_num "WV-1.2a-maal-1" (Arith.mul (n "5") leeg) "0";
  expect_num "WV-1.2a-maal-2" (Arith.mul leeg leeg) "0";
  expect_num "WV-1.2a-pct-1" (Arith.pct_of leeg (n "600")) "0";
  expect_num "WV-1.2a-pct-2" (Arith.pct_of (n "10") leeg) "0";
  expect_num "WV-1.2a-deel-1" (Arith.div_ ~err:no_err leeg (n "5")) "0";
  expect_num "WV-1.2a-deel-2" (Arith.div_ ~err:no_err leeg leeg) "0";
  (let r, errs = with_errs (fun err -> Arith.div_ ~err (n "5") leeg) in
   expect_leeg "WV-1.2a-deel-3" r;
   check "WV-1.2a-deel-3 (§9.5 error)" (errs = 1));
  expect_leeg "WV-1.2a-macht-1" (Arith.pow_ leeg (n "2"));
  expect_leeg "WV-1.2a-macht-2" (Arith.pow_ (n "4") leeg);
  expect_leeg "WV-1.2a-wortel-1" (Arith.sqrt_ ~err:no_err leeg);
  (let r, errs = with_errs (fun err -> Arith.sqrt_ ~err (n "-4")) in
   expect_leeg "WV-1.2a-wortel-2" r;
   check "WV-1.2a-wortel-2 (§9.5 error)" (errs = 1));
  expect_leeg "WV-1.2a-abs-1" (Arith.abs_ leeg);
  expect_leeg "WV-1.2a-afr-1" (Arith.round_v Rekenkundig 2 leeg);
  expect_leeg "WV-1.2a-begr-1" (Arith.clamp ~min_:(n "0") ~max_:(n "100") leeg);
  expect_num "WV-1.2a-begr-2" (Arith.clamp ~min_:leeg ~max_:(n "100") (n "150")) "100";
  (* the subtraction pair, §6.3 *)
  expect_num "WV-1.2-sub-1" (Arith.sub_min leeg (n "4")) "-4";
  expect_leeg "WV-1.2-sub-2" (Arith.sub_verminderd leeg (n "4"))

(* --- ch. 1.2b: dates and durations over empty -------------------------- *)

let () =
  expect_leeg "WV-1.2b-datum-1" (Arith.date_plus_duration leeg (n "5"));
  (let d = Value.date_of_iso "2025-03-01" in
   check "WV-1.2b-datum-2" (Arith.date_plus_duration d leeg = d));
  expect_leeg "WV-1.2b-datum-3" (Arith.jaar_uit leeg);
  expect_leeg "WV-1.2b-datum-4" (Arith.tijdsduur leeg (Value.date_of_iso "2025-03-01"))

(* --- ch. 1.2c: text coercion of empty ---------------------------------- *)

let () =
  check "WV-1.2c-text-1"
    (Arith.concat [ Value.Text "a"; leeg; Value.Text "b" ] = Value.Text "ab")

(* --- ch. 1.2d: comparisons (three-valued at the edges) ------------------ *)

let () =
  check "WV-1.2d-ord-1" (Arith.compare_op Kleiner leeg (n "5") = false);
  check "WV-1.2d-ord-2" (Arith.compare_op Kleiner leeg leeg = false);
  check "WV-1.2d-eq-1" (Arith.compare_op Gelijk leeg leeg = true);
  check "WV-1.2d-eq-2" (Arith.compare_op Gelijk leeg (n "5") = false);
  check "WV-1.2d-neq-1" (Arith.compare_op Ongelijk leeg (n "5") = true);
  check "WV-1.2d-neq-2" (Arith.compare_op Ongelijk leeg leeg = false);
  check "WV-1.2d-neq-3" (Arith.compare_op Ongelijk (n "5") (n "3") = true);
  (* non-numeric equality: two empties are NOT equal (§5 Tabel 38) *)
  check "WV-1.2d-teq-1" (Arith.eq_non_numeric leeg leeg = false)

(* --- ch. 1.2e: aggregations (the §5.8.2/§4.20 split) -------------------- *)

let () =
  expect_num "WV-1.2e-som-1" (Arith.agg_som ~fallback:None [ n "5"; leeg; n "3" ]) "8";
  expect_leeg "WV-1.2e-som-2" (Arith.agg_som ~fallback:None [ leeg; leeg ]);
  expect_num "WV-1.2e-som-3" (Arith.agg_som ~fallback:(Some (n "0")) [ leeg; leeg ]) "0";
  expect_num "WV-1.2e-totaal-1" (Arith.agg_totaal []) "0";
  expect_num "WV-1.2e-aantal-1" (Arith.agg_aantal []) "0";
  expect_num "WV-1.2e-min-1" (Arith.agg_min [ n "5"; leeg; n "3" ]) "3";
  expect_leeg "WV-1.2e-min-2" (Arith.agg_min [ leeg; leeg ]);
  (* dimension folds (§5.8.5): empty cells skipped; all-empty = empty *)
  expect_num "WV-1.2e-dim-1" (Arith.agg_som ~fallback:None [ n "3"; leeg; n "2" ]) "5";
  expect_leeg "WV-1.2e-dim-2" (Arith.agg_som ~fallback:None [ leeg ])

(* --- ch. 1.2g: assignment (via the write boundary) ---------------------- *)

let decl2 : Ir_v0.Ast.datatype option =
  Some (Numeriek { geheel = false; decimalen = 2; teken = None; eenheid = None })

let whole : Ir_v0.Ast.datatype option =
  Some (Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = None })

let mini_world v =
  World.add_instance World.empty
    {
      label = "x";
      otype = "T";
      attrs = (match v with None -> World.SMap.empty | Some v -> World.SMap.singleton "a" v);
      chars = [];
      links = World.SMap.empty;
    }

let () =
  (* target holds 5, computed empty → the write happens, target becomes empty *)
  let w = mini_world (Some (n "5")) in
  let w', changed = World.write_attr w ~label:"x" ~attr:"a" ~decl:decl2 leeg in
  check "WV-1.2g-assign-1" (changed && World.attr (World.get w' "x") "a" = Value.Empty);
  (* target empty, computed empty → no-op *)
  let w = mini_world None in
  let _, changed = World.write_attr w ~label:"x" ~attr:"a" ~decl:decl2 leeg in
  check "WV-1.2g-assign-2" (not changed)

(* --- ch. 2.2: operators, exact ------------------------------------------ *)

let () =
  expect_num "WV-2.2-a" (Arith.mul (n "9") (n "1/3")) "3";
  expect_num "WV-2.2-b" (Arith.add (n "0.1") (n "0.2")) "0.3";
  expect_num "WV-2.2-c" (Arith.sub_verminderd (n "5") (n "3")) "2";
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
  expect_num "WV-2.2-n" (Arith.sqrt_rounded ~err:no_err Rekenkundig 2 (n "2")) "1.41";
  expect_num "WV-2.2-o" (Arith.abs_ (n "-3/7")) "3/7";
  (* division by numeric zero is a hard fault; ch. 1.4 point 4 *)
  expect_fault "ch2.2-div-zero" (fun () -> Arith.div_ ~err:no_err (n "5") (n "0"))

(* --- ch. 2.4: the assignment gate --------------------------------------- *)

let () =
  (* 1,5 into a whole-number attribute: error; rounded first: writes 2 *)
  let w = mini_world None in
  expect_fault "WV-2.4-a" (fun () -> World.write_attr w ~label:"x" ~attr:"a" ~decl:whole (n "1.5"));
  let w', _ = World.write_attr w ~label:"x" ~attr:"a" ~decl:whole (Arith.round_v Rekenkundig 0 (n "1.5")) in
  expect_num "WV-2.4-b" (World.attr (World.get w' "x") "a") "2";
  (* the rounding applies to the true rational *)
  expect_num "WV-2.4-c" (Arith.round_v Rekenkundig 2 (n "2/3")) "0.67";
  expect_num "WV-2.4-d" (Arith.round_v Naar_beneden 2 (n "2/3")) "0.66"

(* --- ch. 2.5: the two rounding tables, cell by cell --------------------- *)

let () =
  let row name d value cells =
    List.iter2
      (fun (mode, label) expected ->
        check
          (Printf.sprintf "WV-2.5-%s-%s" name label)
          (Q.equal (Arith.round_q mode d (Value.q_of_lexeme value)) (q expected)))
      [
        (Ir_v0.Ast.Naar_beneden, "beneden");
        (Naar_boven, "boven");
        (Richting_nul, "nul");
        (Weg_van_nul, "weg");
        (Rekenkundig, "rekenkundig");
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

let unit_ ?noemer ?(factor = 1.0) ~canoniek afk : Ir_v0.Ast.eenheid =
  {
    afkorting = Some afk;
    stelsel = Some "test";
    canoniek = Some canoniek;
    factor_naar_canoniek = Some factor;
    noemer;
  }

let mnd = unit_ ~canoniek:"mnd" "mnd"
let jr = unit_ ~canoniek:"mnd" ~factor:12.0 "jr"
let eur = unit_ ~canoniek:"EUR" "€"
let meter = unit_ ~canoniek:"m" "m"
let eur_per_jr = { eur with noemer = Some jr }
let nu u s = Value.Num (Value.q_of_lexeme s, Value.Unit u)

let () =
  check "WV-2.6-a" (Arith.compare_op Gelijk (nu mnd "24") (nu jr "2") = true);
  (match Arith.mul (nu eur_per_jr "100") (nu jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "WV-2.6-b" (Q.equal v (q "200") && u.noemer = None && u.afkorting = Some "€")
  | _ -> check "WV-2.6-b" false);
  (match Arith.mul (nu eur "3") (nu meter "2") with
  | Value.Num (v, Value.Product _) -> check "WV-2.6-c" (Q.equal v (q "6"))
  | _ -> check "WV-2.6-c" false);
  (match Arith.div_ ~err:no_err (nu eur "6") (nu eur "3") with
  | Value.Num (v, Value.Unitless) -> check "WV-2.6-d" (Q.equal v (q "2"))
  | _ -> check "WV-2.6-d" false);
  (match Arith.div_ ~err:no_err (nu eur "600") (nu jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "WV-2.6-e" (Q.equal v (q "300") && u.afkorting = Some "€" && u.noemer = Some jr)
  | _ -> check "WV-2.6-e" false);
  (* plus restates the right operand into the left unit: 2 jr + 12 mnd = 3 jr *)
  (match Arith.add (nu jr "2") (nu mnd "12") with
  | Value.Num (v, Value.Unit u) -> check "ch2.6-add-restate" (Q.equal v (q "3") && u.afkorting = Some "jr")
  | _ -> check "ch2.6-add-restate" false)

(* --- ch. 2.7: clamping -------------------------------------------------- *)

let () =
  expect_num "WV-2.7-a" (Arith.clamp ~min_:(n "0") ~max_:(n "100") (n "150")) "100";
  expect_num "WV-2.7-b" (Arith.clamp ~min_:(n "0") ~max_:(n "100") (n "-5")) "0";
  expect_num "WV-2.7-c"
    (Arith.round_v Rekenkundig 2 (Arith.clamp ~min_:(n "0") ~max_:(n "1") (n "2/3")))
    "0.67"

(* --- ch. 2.8: tijdsevenredig deel (the arithmetic core) ------------------ *)

let () =
  let v = Q.mul (q "1200") (q "90/365") in
  check "WV-2.8-a-exact" (Q.equal v (q "21600/73"));
  check "WV-2.8-a-rounded" (Q.equal (Arith.round_q Rekenkundig 2 v) (q "29589/100"))

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

let no_alloc_err = fun ~id:_ (_ : string) -> ()

let alloc ?(err = no_alloc_err) methode ?(afnemend = false) ?decimals pot recs =
  shares_of (Alloc.allocate methode ~afnemend ~decimals ~pot:(n pot) ~err recs)

let assigned name shares expected =
  check name
    (List.for_all2
       (fun (id, s) (eid, es) ->
         String.equal id eid
         && match (s, es) with
            | Some got, Some want -> Q.equal got (q want)
            | None, None -> true
            | _ -> false)
       shares expected)

let () =
  (* 4.6-a equal shares *)
  let s, r = alloc Gelijke_delen "90" [ rcp "a"; rcp "b"; rcp "c" ] in
  assigned "WV-4.6-a1" s [ ("a", Some "30"); ("b", Some "30"); ("c", Some "30") ];
  check "WV-4.6-a1-rest" (r = Some Q.zero);
  let s, _ = alloc Gelijke_delen "1000" [ rcp "a"; rcp "b" ] in
  assigned "WV-4.6-a2" s [ ("a", Some "500"); ("b", Some "500") ];
  (* 4.6-b proportional; a weightless fourth stays leeg *)
  let errs = ref 0 in
  let s, _ =
    alloc ~err:(fun ~id:_ _ -> incr errs) Naar_rato "120"
      [ rcp ~weight:(n "1") "a"; rcp ~weight:(n "2") "b"; rcp ~weight:(n "3") "c"; rcp ~weight:leeg "d" ]
  in
  assigned "WV-4.6-b" s [ ("a", Some "20"); ("b", Some "40"); ("c", Some "60"); ("d", None) ];
  check "WV-4.6-b-err" (!errs = 1);
  (* 4.6-c proportional + floor + rest: 1001 over 3:2, whole euros *)
  let s, r =
    alloc Naar_rato ~decimals:0 "1001" [ rcp ~weight:(n "3") "a"; rcp ~weight:(n "2") "b" ]
  in
  assigned "WV-4.6-c" s [ ("a", Some "600"); ("b", Some "400") ];
  check "WV-4.6-c-rest" (r = Some Q.one);
  (* 4.6-d proportional + cap + rest (the berging shape) *)
  let s, r =
    alloc Naar_rato ~decimals:0 "1200"
      [
        rcp ~weight:(n "3") ~cap:(n "300") "a";
        rcp ~weight:(n "1") ~cap:(n "300") "b";
        rcp ~weight:(n "2") ~cap:(n "300") "c";
      ]
  in
  assigned "WV-4.6-d" s [ ("a", Some "300"); ("b", Some "200"); ("c", Some "300") ];
  check "WV-4.6-d-rest" (r = Some (q "400"));
  (* 4.6-e cap degrade: empty and negative caps degrade to uncapped.
     The prose says "one §9.5 error"; ch. 4.3 + ch. 5.6 (one fact per
     rule × subject) imply one per degraded cap, i.e. two — flagged as an
     erratum candidate; the test requires at least one. *)
  let errs = ref 0 in
  let s, _ =
    alloc ~err:(fun ~id:_ _ -> incr errs) Naar_rato "1200"
      [
        rcp ~weight:(n "3") ~cap:leeg "a";
        rcp ~weight:(n "2") ~cap:(n "-5") "b";
        rcp ~weight:(n "1") ~cap:(n "300") "c";
      ]
  in
  assigned "WV-4.6-e" s [ ("a", Some "600"); ("b", Some "400"); ("c", Some "200") ];
  check "WV-4.6-e-err" (!errs >= 1);
  (* 4.6-f ordered depletion, ascending then descending *)
  let recs () =
    [
      rcp ~weight:(n "3") ~key:(n "20") "a";
      rcp ~weight:(n "1") ~key:(n "20") "b";
      rcp ~weight:(n "1") ~key:(n "22") "c";
    ]
  in
  let s, r = alloc Op_volgorde "1200" (recs ()) in
  assigned "WV-4.6-f-asc" s [ ("a", Some "900"); ("b", Some "300"); ("c", Some "0") ];
  check "WV-4.6-f-asc-rest" (r = Some Q.zero);
  let s, _ = alloc Op_volgorde ~afnemend:true "1200" (recs ()) in
  assigned "WV-4.6-f-desc" s [ ("c", Some "1200"); ("a", Some "0"); ("b", Some "0") ];
  (* 4.6-g the full recurrence: cap + floor + depletion over 1800 *)
  let s, r =
    alloc Op_volgorde ~decimals:0 "1800"
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
  check "WV-4.6-g-rest" (r = Some (q "26"));
  (* 4.6-h edge rows *)
  check "WV-4.6-h-empty" (alloc Naar_rato "100" [] = ([], None));
  let s, _ =
    alloc Op_volgorde "1200" [ rcp ~weight:(n "3") ~key:(n "1") "a"; rcp ~weight:(n "-1") ~key:(n "1") "b" ]
  in
  assigned "WV-4.6-h-neg" s [ ("a", Some "1200"); ("b", Some "0") ];
  let s, _ =
    alloc Op_volgorde "1200"
      [
        rcp ~weight:(n "0") ~key:(n "1") "a";
        rcp ~weight:(n "0") ~key:(n "1") "b";
        rcp ~weight:(n "1") ~key:(n "2") "c";
      ]
  in
  assigned "WV-4.6-h-zero" s [ ("a", Some "0"); ("b", Some "0"); ("c", Some "1200") ];
  let s, _ =
    alloc Op_volgorde "300"
      [
        rcp ~key:(n "2020") "a";
        rcp ~key:leeg "b";
        rcp ~key:(n "2021") "c";
      ]
  in
  (* evaluation order 2020, 2021, leeg (the empty key last) *)
  check "WV-4.6-h-leeg-laatst" (List.map fst s = [ "a"; "c"; "b" ])

let () =
  if !failures > 0 then begin
    Printf.printf "%d failure(s)\n" !failures;
    exit 1
  end
  else print_endline "test_ref: all worked examples pass"
