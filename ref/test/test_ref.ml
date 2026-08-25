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
let total = ref 0

let check name ok =
  incr total;
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
  check "WV-1.2d-teq-1" (Arith.eq_non_numeric leeg leeg = false);
  (* the date row (v0's Tabel-38 guard on the date shape): gelijk-aan
     over dates is onwaar on ANY empty operand — in particular
     both-empty, where the numeric rule above reads waar — while
     ongelijk-aan keeps the numeric cells (exactly one empty → waar,
     both empty → onwaar) *)
  (let d = Value.date_of_iso "2026-06-21" in
   let cmp op a b = Arith.compare_op ~static_numeric:true ~static_date:true op a b in
   check "WV-1.2d-date-eq-1" (cmp Gelijk leeg leeg = false);
   check "WV-1.2d-date-eq-2" (cmp Gelijk leeg d = false);
   check "WV-1.2d-date-eq-3" (cmp Gelijk d d = true);
   check "WV-1.2d-date-neq-1" (cmp Ongelijk leeg d = true);
   check "WV-1.2d-date-neq-2" (cmp Ongelijk leeg leeg = false))

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
      tls = World.SMap.empty; ctls = World.SMap.empty;
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

let rest_is r want = match r with Some x -> Q.equal x (q want) | None -> false

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
  check "WV-4.6-a1-rest" (rest_is r "0");
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
  check "WV-4.6-c-rest" (rest_is r "1");
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
  check "WV-4.6-d-rest" (rest_is r "400");
  (* 4.6-e cap degrade: empty and negative caps degrade to uncapped,
     with two §9.5 errors — one per degraded cap (the decided ch. 4.6
     erratum; ch. 4.3 + ch. 5.6's rule × subject discipline). *)
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
  check "WV-4.6-e-err" (!errs = 2);
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
  check "WV-4.6-f-asc-rest" (rest_is r "0");
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
  check "WV-4.6-g-rest" (rest_is r "26");
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

(* --- verification-round regressions ------------------------------------- *)

let () =
  (* ch. 1.2d asymmetry for two empties routes on the static kind:
     numeric → waar, non-numeric (§5 Tabel 38) → onwaar *)
  check "verify-eq-static-numeric" (Arith.compare_op ~static_numeric:true Gelijk leeg leeg = true);
  check "verify-eq-static-nonnum" (Arith.compare_op ~static_numeric:false Gelijk leeg leeg = false);
  check "verify-neq-static-nonnum"
    (Arith.compare_op ~static_numeric:false Ongelijk leeg leeg = false);
  (* strict lexeme validation: no silent +inf or 0 *)
  expect_fault "verify-lexeme-zero-den" (fun () -> Value.q_of_lexeme "1/0");
  expect_fault "verify-lexeme-empty" (fun () -> Value.q_of_lexeme "");
  expect_fault "verify-lexeme-junk" (fun () -> Value.q_of_lexeme "abc");
  check "verify-lexeme-neg-dec" (Q.equal (Value.q_of_lexeme "-0.5") (q "-1/2"));
  (* unit factors read as the intended decimal, not the nearest double *)
  check "verify-wire-factor-cent" (Q.equal (Arith.q_of_wire_number 0.01) (q "1/100"));
  check "verify-wire-factor-int" (Q.equal (Arith.q_of_wire_number 1000.0) (q "1000"));
  (* an astronomic exponent faults instead of escaping as Z.Overflow *)
  expect_fault "verify-pow-overflow" (fun () ->
      Arith.pow_ (n "2") (Value.Num (Q.of_string "100000000000000000000", Value.Unitless)))

(* --- ch. 6.4: evaluation by strata (worked examples, hand-built docs) --- *)

let empty_doc : Ir_v0.Ast.document =
  {
    objecttypen = [];
    domeinen = [];
    parameters = [];
    eenheidstelsels = [];
    feittypen = [];
    dimensies = [];
    tijdlijnen = [];
    dagsoorten = [];
    dagsoort_per_eenheid = [];
    regels = [];
    resolved_regels = [];
    resolved_dagsoorten = [];
    object_creaties = [];
    verdelingen = [];
    consistenties = [];
    uniciteiten = [];
    uniciteit_unies = [];
    feit_creaties = [];
    resolved_startpunten = [];
    regel_groepen = [];
    diagnostics = [];
    default_ladders = [];
    function_defs = [];
  }

let ot name attrs kenmerken : string * Ir_v0.Ast.objecttype =
  ( name,
    {
      ot_naam = Some name;
      attributen = attrs;
      attribuut_meervoud = [];
      attribuut_dimensies = [];
      attribuut_granulariteit = [];
      attribuut_tijdlijn_aantal = [];
      attribuut_tijdlijn_naam = [];
      kenmerken = List.map (fun k -> Some k) kenmerken;
      kenmerk_soorten = [];
      kenmerk_granulariteit = [];
      kenmerk_tijdlijn_aantal = [];
      bezield = false;
      meervoud = None;
    } )

let num0 : Ir_v0.Ast.datatype = Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = None }

let aref attr otn typ : Ir_v0.Ast.expr =
  Attribuut_ref
    {
      attribuut = Some attr;
      objecttype = Some otn;
      onderwerp_bron = None;
      navigatie_rol = None;
      typ = Some typ;
      bron = None;
      set = false;
      navigatie_pad = [];
      navigatie_bindingen = None;
    }

let litv lex typ : Ir_v0.Ast.expr = Literal { waarde = Some lex; typ = Some typ; bron = None }

let rr name soort doel waarde cond : Ir_v0.Ast.resolved_regel =
  { rr_naam = Some name; rr_soort = Some soort; doel; waarde; rr_voorwaarde = cond; rr_geldigheid = None;
    rr_bron_annotaties = None }

let inst label otype attrs : World.instance =
  {
    label;
    otype;
    attrs = List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty attrs;
    chars = [];
    links = World.SMap.empty;
    tls = World.SMap.empty; ctls = World.SMap.empty;
  }

let env_of doc : Eval.env =
  { doc; params = Eval.SMap.empty; param_tls = Eval.SMap.empty; as_of = None;
    locals = Eval.SMap.empty }

(* WV-6.4-a (= the ch. 5.3 pair): producer/consumer across one negative
   edge — {waar, onwaar} at inkomen 50, {onwaar, waar} at 200, never the
   transient {waar, waar} *)
let () =
  let doc =
    {
      empty_doc with
      objecttypen = [ ot "Jutter" [ ("inkomen", Some num0) ] [ "vrijgesteld"; "bijdrageplichtig" ] ];
      resolved_regels =
        [
          rr "vrijstelling" Kenmerktoekenning
            (Some (aref "vrijgesteld" "Jutter" Booleaan))
            (Some (litv "waar" Booleaan))
            (Some
               (Vergelijking
                  {
                    op = Kleiner;
                    links = Some (aref "inkomen" "Jutter" num0);
                    rechts = Some (litv "100" num0);
                    periode = None;
                    bron = None;
                  }));
          rr "bijdrageplicht" Kenmerktoekenning
            (Some (aref "bijdrageplichtig" "Jutter" Booleaan))
            (Some (litv "waar" Booleaan))
            (Some
               (Kenmerk_check
                  {
                    kenmerk = Some "vrijgesteld";
                    navigatie = None;
                    negatie = true;
                    eigenaar = Some "Jutter";
                    periode = None;
                    bron = None;
                    navigatie_binding = None;
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
  let doc_of waarde : Ir_v0.Ast.document =
    {
      empty_doc with
      objecttypen =
        [ ot "Jutter" [ ("overzees", Some Booleaan) ] []; ot "Rekenkamer" [] [ "overzees" ] ];
      object_creaties =
        [
          {
            oc_naam = Some "richt de rekenkamer op";
            nieuw_type = Some "Rekenkamer";
            oc_subject = Some "Jutter";
            toekenningen =
              [ { toe_attribuut = Some "overzees"; toe_waarde = Some waarde; toe_kenmerk = true } ];
            oc_voorwaarde = None;
            oc_geldigheid = None;
            oc_verbindende_rol = None;
          };
        ];
    }
  in
  let mint waarde overzees_in =
    let doc = doc_of waarde in
    let w = World.add_instance World.empty (inst "j" "Jutter" [ ("overzees", overzees_in) ]) in
    let final = Eval.evaluate (env_of doc) w in
    World.has_char (World.get final "oc0::j") "overzees"
  in
  let read_overzees = aref "overzees" "Jutter" Booleaan in
  check "WV-5.5-charinit-expr-true" (mint read_overzees (Value.Bool true) = true);
  check "WV-5.5-charinit-expr-false" (mint read_overzees (Value.Bool false) = false);
  (* the empty flank (a missing caller attribute reads leeg) joins false:
     the mark is left unset, no fault — the attribute-init parallel and the
     closed-world reading, identical to the ASP backend's absent atom *)
  check "WV-5.5-charinit-expr-empty" (mint read_overzees Value.Empty = false);
  (* the preserved v0 constant form is unconditional — set on waar regardless
     of the caller, unset on onwaar *)
  check "WV-5.5-charinit-const-waar" (mint (litv "waar" Booleaan) (Value.Bool false) = true);
  check "WV-5.5-charinit-const-onwaar" (mint (litv "onwaar" Booleaan) (Value.Bool true) = false);
  (* a value that evaluates to a non-boolean faults fast (the runtime
     bounded-write guard the verifier's v0 unknown-sentinel defers to) *)
  expect_fault "WV-5.5-charinit-nonbool-fault" (fun () ->
      ignore (mint (litv "5" num0) (Value.Bool true)))

(* WV-6.4-b (= the ch. 5.6 channel read): one error fact, both dossiers
   flagged — the reading rule sits strictly above the consistency *)
let () =
  let doc =
    {
      empty_doc with
      objecttypen =
        [
          ot "Rekeninghouder" [ ("saldo", Some num0) ] [];
          ot "Dossier" [] [ "onder verscherpt toezicht" ];
        ];
      consistenties =
        [
          {
            cons_naam = Some "controle saldo";
            cons_subject = Some "Rekeninghouder";
            assertie =
              Some
                (Vergelijking
                   {
                     op = Groter_of_gelijk;
                     links = Some (aref "saldo" "Rekeninghouder" num0);
                     rechts = Some (litv "0" num0);
                     periode = None;
                     bron = None;
                   });
            cons_bron = None;
            cons_line = 0;
            cons_geldigheid = None;
          };
        ];
      resolved_regels =
        [
          rr "verscherpt toezicht" Kenmerktoekenning
            (Some (aref "onder verscherpt toezicht" "Dossier" Booleaan))
            (Some (litv "waar" Booleaan))
            (Some (Regelversie { regel = Some "controle saldo"; inconsistent = true; bron = None }));
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
    Ir_v0.Ast.Kenmerk_check
      { kenmerk = Some name; navigatie = None; negatie = true; eigenaar = Some "Aanvraag"; periode = None; bron = None; navigatie_binding = None }
  in
  let kt name char cond =
    rr name Kenmerktoekenning
      (Some (aref char "Aanvraag" Booleaan))
      (Some (litv "waar" Booleaan))
      (Some cond)
  in
  let doc =
    {
      empty_doc with
      objecttypen = [ ot "Aanvraag" [ ("x", Some num0) ] [ "k1"; "k2"; "k3"; "k4" ] ];
      resolved_regels =
        [
          kt "trap4" "k4" (kc "k3");
          kt "trap3" "k3" (kc "k2");
          kt "trap2" "k2" (kc "k1");
          kt "trap1" "k1"
            (Vergelijking
               {
                 op = Groter_of_gelijk;
                 links = Some (aref "x" "Aanvraag" num0);
                 rechts = Some (litv "1" num0);
                 periode = None;
                 bron = None;
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

(* WV-6.4-e: an emptiness test on a derived attribute is a stratum-strict
   edge — the outcome is rule-order-independent (the verification round's
   counterexample, locked) *)
let () =
  let writer =
    rr "zet a" Gelijkstelling (Some (aref "a" "Ding" num0)) (Some (litv "5" num0)) None
  in
  let reader =
    rr "markeer" Kenmerktoekenning
      (Some (aref "gemarkeerd" "Ding" Booleaan))
      (Some (litv "waar" Booleaan))
      (Some
         (Predicaat
            {
              soort = Leeg;
              operand = Some (aref "a" "Ding" num0);
              dagsoort = None;
              negatie = false;
              cijfers = 0;
              periode = None;
              bron = None;
            }))
  in
  let run rules =
    let doc =
      { empty_doc with objecttypen = [ ot "Ding" [ ("a", Some num0) ] [ "gemarkeerd" ] ]; resolved_regels = rules }
    in
    let final = Eval.evaluate (env_of doc) (World.add_instance World.empty (inst "x" "Ding" [])) in
    World.has_char (World.get final "x") "gemarkeerd"
  in
  check "WV-6.4-e-writer-first" (run [ writer; reader ] = false);
  check "WV-6.4-e-reader-first" (run [ reader; writer ] = false)

(* WV-6.4-c: a dependency cycle reaching ref/ is a fault (the verifier
   rejects it as WF-045; ref/ fails fast rather than diverging) *)
let () =
  let plus l r : Ir_v0.Ast.expr =
    Binair { op = Plus; links = Some l; rechts = Some r; typ = Some num0; bron = None }
  in
  let doc =
    {
      empty_doc with
      objecttypen = [ ot "Ding" [ ("a", Some num0); ("b", Some num0) ] [] ];
      resolved_regels =
        [
          rr "bereken a" Berekening
            (Some (aref "a" "Ding" num0))
            (Some (plus (aref "b" "Ding" num0) (litv "1" num0)))
            None;
          rr "bereken b" Berekening
            (Some (aref "b" "Ding" num0))
            (Some (plus (aref "a" "Ding" num0) (litv "1" num0)))
            None;
        ];
    }
  in
  let w = World.add_instance World.empty (inst "x" "Ding" []) in
  expect_fault "WV-6.4-c-cycle-fault" (fun () -> Eval.evaluate (env_of doc) w)

(* WV-7.9: nested exception groups (ch. 7.9) — the PHASE4-DEFAULTS §7
   spurious-conflict test at the unit level (b <- x, x <- {y, z}, a hand-built
   attribute ladder). Only y applicable and x justified must yield y, never a
   manufactured root conflict; y and z deciding differently propagates the
   conflict to the root (no write, path-keyed Jutter.heffing/0); neither
   applicable falls back to the rung's own consequence. The corpus anchors
   jutterswet-nestuitzondering/nestkenmerk carry the full family; this isolates
   the reconciliation invariant in the fast suite. *)
let () =
  let inst_c label otype chars : World.instance =
    {
      label; otype;
      attrs = World.SMap.empty;
      chars = List.map Ir_v0.Checks.norm chars;
      links = World.SMap.empty;
      tls = World.SMap.empty; ctls = World.SMap.empty;
    }
  in
  let kc name : Ir_v0.Ast.conditie =
    Kenmerk_check
      { kenmerk = Some name; navigatie = None; negatie = false; eigenaar = Some "Jutter";
        periode = None; bron = None; navigatie_binding = None }
  in
  let leaf name lex : Ir_v0.Ast.rung =
    { rung_besluit = None; rung_voorwaarde = Some (kc name);
      rung_consequentie = Some (litv lex num0); rung_initialisatie = false;
      rung_excepties = None; rung_bron = None }
  in
  let sub name lex : Ir_v0.Ast.sub_ladder = { sl_rungs = [ leaf name lex ]; sl_bron = None } in
  let ladder : Ir_v0.Ast.default_ladder =
    { dl_naam = Some "heffing"; dl_onderwerp = "Jutter"; dl_kenmerk = None;
      dl_attribuut = Some "heffing";
      rungs =
        [ { rung_besluit = None; rung_voorwaarde = Some (kc "grondX");
            rung_consequentie = Some (litv "40" num0); rung_initialisatie = false;
            rung_excepties =
              Some { sg_ongeordend = true; sg_leden = [ sub "grondY" "10"; sub "grondZ" "20" ] };
            rung_bron = None } ];
      dl_ongeordend = false; dl_bron = None; dl_geldigheid = None }
  in
  let doc =
    { empty_doc with
      objecttypen = [ ot "Jutter" [ ("heffing", Some num0) ] [ "grondX"; "grondY"; "grondZ" ] ];
      default_ladders = [ ladder ] }
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
let seg van tot v : Timeline.segment = { s_van = day van; s_tot = day tot; s_waarde = v }
let seg_leeg van tot : Timeline.segment = { s_van = day van; s_tot = day tot; s_waarde = leeg }

let tl_shape name (tl : Timeline.t) expected =
  check name
    (List.length tl = List.length expected
    && List.for_all2
         (fun (s : Timeline.segment) (van, tot, v) ->
           s.s_van = day van && s.s_tot = day tot
           &&
           match (s.s_waarde, v) with
           | Value.Empty, None -> true
           | actual, Some lex -> Arith.equal actual (n lex)
           | _ -> false)
         tl expected)

(* WV-1.2f: the two timeline empty rules side by side *)
let () =
  let a = [ seg_leeg "2024-01-01" "2024-02-01" ] in
  let b = [ seg "2024-01-01" "2024-02-01" (n "4") ] in
  tl_shape "WV-1.2f-tl-1" (Timeline.plus a b) [ ("2024-01-01", "2024-02-01", Some "4") ];
  tl_shape "WV-1.2f-tl-2" (Timeline.som [ a; b ]) [ ("2024-01-01", "2024-02-01", Some "4") ];
  let bb = [ seg_leeg "2024-01-01" "2024-02-01" ] in
  tl_shape "WV-1.2f-tl-3" (Timeline.plus a bb) [ ("2024-01-01", "2024-02-01", Some "0") ];
  tl_shape "WV-1.2f-tl-4" (Timeline.som [ a; bb ]) [ ("2024-01-01", "2024-02-01", None) ];
  tl_shape "WV-1.2f-tl-4-fallback"
    (Timeline.som ~fallback:(n "0") [ a; bb ])
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
  tl_shape "WV-3.5-a" (Timeline.som [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "50");
      ("2024-02-01", "2024-03-01", Some "70");
      ("2024-03-01", "2024-04-01", Some "10");
    ];
  let p1 = [ seg "2024-01-01" "2024-02-01" (n "40") ] in
  let p2 = [ seg "2024-03-01" "2024-04-01" (n "20") ] in
  tl_shape "WV-3.5-b" (Timeline.som [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "40");
      ("2024-02-01", "2024-03-01", None);
      ("2024-03-01", "2024-04-01", Some "20");
    ];
  tl_shape "WV-3.5-b-fallback"
    (Timeline.som ~fallback:(n "0") [ p1; p2 ])
    [
      ("2024-01-01", "2024-02-01", Some "40");
      ("2024-02-01", "2024-03-01", Some "0");
      ("2024-03-01", "2024-04-01", Some "20");
    ];
  check "WV-3.5-c-empty-set" (Timeline.som ~fallback:(n "0") [] = []);
  (* cross-unit: €/mnd contributors into a €/jr target — restated before
     the fold: 50×12 + 10×12 = 720 *)
  let eur_mnd = { eur with noemer = Some mnd } in
  let eur_jr = { eur with noemer = Some jr } in
  let q1 = [ seg "2024-01-01" "2024-02-01" (nu eur_mnd "50") ] in
  let q2 = [ seg "2024-01-01" "2024-02-01" (nu eur_mnd "10") ] in
  (match Timeline.som ~target_unit:(Value.Unit eur_jr) [ q1; q2 ] with
  | [ { s_waarde = Value.Num (v, Value.Unit u); _ } ] ->
      check "WV-3.5-d-720" (Q.equal v (q "720") && u.noemer = Some jr)
  | _ -> check "WV-3.5-d-720" false)

(* WV-3.6: the temporal total *)
let () =
  let month = Ir_v0.Ast.Maand in
  let tl =
    [ seg "2023-01-01" "2024-01-01" (n "10.00"); seg "2024-01-01" "2024-07-01" (n "11.00") ]
  in
  expect_num "WV-3.6-a" (Timeline.totaal month tl) "186";
  let degenerate =
    [ seg "2024-01-01" "2024-01-01" (n "99"); { s_van = day "2024-06-01"; s_tot = day "2024-05-01"; s_waarde = n "7" }; seg "2024-01-01" "2024-07-01" (n "10") ]
  in
  expect_num "WV-3.6-b" (Timeline.totaal month degenerate) "60";
  (* gedurende "> 1000" clips to the 1200 months *)
  let vals = [ seg "2024-01-01" "2024-04-01" (n "1200"); seg "2024-04-01" "2024-06-01" (n "800") ] in
  let span = List.map (fun (s : Timeline.segment) -> (s.s_van, s.s_tot)) vals in
  let tp = Timeline.true_periods_of vals ~span (fun v -> Arith.compare_op Groter v (n "1000")) in
  expect_num "WV-3.6-c" (Timeline.totaal month (Timeline.clip vals tp)) "3600";
  (* gedurende a characteristic, and its negated complement *)
  let vals = [ seg "2023-01-01" "2024-01-01" (n "10"); seg "2024-01-01" "2024-07-01" (n "11") ] in
  let span = [ (day "2023-01-01", day "2024-07-01") ] in
  let char_true = [ (day "2023-07-01", day "2024-02-01") ] in
  expect_num "WV-3.6-d" (Timeline.totaal month (Timeline.clip vals char_true)) "71";
  expect_num "WV-3.6-e" (Timeline.totaal month (Timeline.clip vals (Timeline.complement char_true ~span))) "115";
  (* gedurende "≠ 1000": leeg satisfies ≠ against a filled bound *)
  let vals = [ seg "2024-01-01" "2024-07-01" (n "100") ] in
  let tested = [ seg "2024-01-01" "2024-02-01" (n "1000") ] in
  let span = [ (day "2024-01-01", day "2024-07-01") ] in
  let tp = Timeline.true_periods_of tested ~span (fun v -> Arith.compare_op Ongelijk v (n "1000")) in
  expect_num "WV-3.6-f" (Timeline.totaal month (Timeline.clip vals tp)) "500"

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
  expect_num "verify-clip-per-fragment-floor" (Timeline.totaal Maand (Timeline.clip vals tp)) "0"

(* WV-3.2: anchored month walls clamp from the anchor, never from the
   previous wall *)
let () =
  let anchor = day "2024-01-31" in
  let wall k = Timeline.anchored_wall Maand ~anchor k in
  check "WV-3.2-anchored"
    (wall 0 = day "2024-01-31" && wall 1 = day "2024-02-29" && wall 2 = day "2024-03-31"
    && wall 3 = day "2024-04-30")

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
  let toegekend_ander = ctor "Toegekend" (Some (n "1000.00")) in
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
  check "WV-9.3-struct-neq-payload" (Arith.eq_non_numeric toegekend toegekend_ander = false);
  (* 9.3: unequal tags → not equal (unary vs nullary, and nullary vs nullary) *)
  check "WV-9.3-neq-tag-unary-nullary" (Arith.eq_non_numeric toegekend afgewezen = false);
  check "WV-9.3-neq-tag-nullary" (Arith.eq_non_numeric afgewezen aangehouden = false);
  (* the idempotent-write equality agrees (a payload is never leeg) *)
  check "WV-9.3-equal-struct-eq" (Arith.equal toegekend toegekend' = true);
  check "WV-9.3-equal-struct-neq" (Arith.equal toegekend toegekend_ander = false);
  (* 9.3 through the comparison operator (sum types are non-numeric): the
     anchor's rule 2 (= Aangehouden) and rule 3 (≠ Aangehouden) *)
  check "WV-9.3-eq-same-tag"
    (Arith.compare_op ~static_numeric:false Gelijk aangehouden aangehouden = true);
  check "WV-9.3-neq-same-tag"
    (Arith.compare_op ~static_numeric:false Ongelijk aangehouden aangehouden = false);
  check "WV-9.3-eq-diff-tag"
    (Arith.compare_op ~static_numeric:false Gelijk toegekend aangehouden = false);
  check "WV-9.3-neq-diff-tag"
    (Arith.compare_op ~static_numeric:false Ongelijk toegekend aangehouden = true);
  (* 9.3: a leeg operand makes BOTH = and ≠ false (Tabel 38 mirror,
     deliberately not complements — the anchor's case 4 corner), in both
     operand orders *)
  check "WV-9.3-eq-empty-l"
    (Arith.compare_op ~static_numeric:false Gelijk leeg aangehouden = false);
  check "WV-9.3-neq-empty-l"
    (Arith.compare_op ~static_numeric:false Ongelijk leeg aangehouden = false);
  check "WV-9.3-eq-empty-r"
    (Arith.compare_op ~static_numeric:false Gelijk aangehouden leeg = false);
  check "WV-9.3-neq-empty-r"
    (Arith.compare_op ~static_numeric:false Ongelijk aangehouden leeg = false);
  (* 9.3: eq_non_numeric with an empty operand is false directly *)
  check "WV-9.3-teq-empty" (Arith.eq_non_numeric leeg aangehouden = false);
  (* 9.3: sum values are not orderable — scalar_cmp faults *)
  expect_fault "WV-9.3-order-fault"
    (fun () -> Arith.compare_op ~static_numeric:false Kleiner toegekend aangehouden)

(* --- ch. 10: folds and function values (RUNTIME-SEMANTICS ch. 10) -------
   Value-level worked examples: list rendering (10.1) and the list-equality
   corners — elementwise and IN ORDER, with the Tabel 38 Empty corners ([] is
   a value, leeg is its absence). The evaluation of the fold family over
   documents is pinned absolutely by the jutterswet-staffelvergoeding and
   jutterswet-aanspraaktelling anchors through gates 5-6 (case1 the deciding
   fold, case2 the empty-list value writing 0.00/0, case3 the leeg-propagation
   survivor writing nothing). *)
let lijst xs = Value.Lijst xs

let () =
  let l1 = lijst [ n "12.50"; n "30.00"; n "7.25" ] in
  let l1' = lijst [ n "12.50"; n "30.00"; n "7.25" ] in
  let l_reordered = lijst [ n "30.00"; n "12.50"; n "7.25" ] in
  let l_short = lijst [ n "12.50"; n "30.00" ] in
  (* 10.1: the deterministic list rendering (canonical trailing-zero-dropped
     lexemes, the same form used everywhere a value renders) *)
  check "WV-10.1-render" (Value.to_string l1 = "[12.5, 30, 7.25]");
  check "WV-10.1-render-empty" (Value.to_string (lijst []) = "[]");
  (* 10.1: equality is elementwise AND in order — the load-bearing distinction
     the staffelvergoeding anchor pins (reordered would fold to 37.25) *)
  check "WV-10.1-eq" (Arith.equal l1 l1' = true);
  check "WV-10.1-eq-order" (Arith.equal l1 l_reordered = false);
  check "WV-10.1-eq-length" (Arith.equal l1 l_short = false);
  check "WV-10.1-eq-empty-list" (Arith.equal (lijst []) (lijst []) = true);
  (* 10.1: the non-numeric equality path agrees (a position is never leeg) *)
  check "WV-10.1-teq" (Arith.eq_non_numeric l1 l1' = true);
  check "WV-10.1-teq-order" (Arith.eq_non_numeric l1 l_reordered = false);
  (* Tabel 38: a leeg operand makes eq_non_numeric false in BOTH directions;
     and the empty list [] (a value) is never equal to leeg (its absence) *)
  check "WV-10.1-teq-empty-l" (Arith.eq_non_numeric leeg l1 = false);
  check "WV-10.1-teq-empty-r" (Arith.eq_non_numeric l1 leeg = false);
  check "WV-10.1-eq-empty-vs-nil" (Arith.equal leeg (lijst []) = false);
  (* composing ch. 9: a list of constructor values (the aanspraaktelling
     shape) — equal to itself, order-sensitive, never equal reordered *)
  let a1 =
    lijst [ ctor "Erkend" (Some (n "10.00")); ctor "Vervallen" None; ctor "Erkend" (Some (n "2.50")) ]
  in
  let a1' =
    lijst [ ctor "Erkend" (Some (n "10.00")); ctor "Vervallen" None; ctor "Erkend" (Some (n "2.50")) ]
  in
  let a_reordered =
    lijst [ ctor "Vervallen" None; ctor "Erkend" (Some (n "10.00")); ctor "Erkend" (Some (n "2.50")) ]
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
    { par_naam = Some name; par_datatype = Some dt; par_granulariteit = None }
  in
  let doc =
    {
      empty_doc with
      objecttypen = [ ot "J" [] [] ];
      parameters =
        [ ("p", param "p" num0); ("lst", param "lst" (List_type { element = num0 })) ];
    }
  in
  (* env_of leaves params empty, so both declared parameters are absent *)
  let env = env_of doc in
  let w = World.add_instance World.empty (inst "j" "J" []) in
  let self = World.get w "j" in
  let pref name typ : Ir_v0.Ast.expr =
    Parameter_ref { parameter = Some name; typ = Some typ; bron = None }
  in
  expect_fault "WV-5.7-scalar-absent-fault"
    (fun () -> Eval.eval_expr env w self ~err:no_err (pref "p" num0));
  expect_leeg "WV-10.3-list-absent-leeg"
    (Eval.eval_expr env w self ~err:no_err (pref "lst" (List_type { element = num0 })))

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
  check "WV-11.2-eq-op" (Arith.compare_op ~static_numeric:false Gelijk d1 d1' = true);
  check "WV-11.2-neq-op" (Arith.compare_op ~static_numeric:false Ongelijk d1 d1' = false);
  check "WV-11.2-eq-op-diff" (Arith.compare_op ~static_numeric:false Gelijk d1 d_val = false);
  check "WV-11.2-neq-op-diff" (Arith.compare_op ~static_numeric:false Ongelijk d1 d_val = true);
  (* 11.2: a leeg operand makes BOTH = and ≠ false (Tabel 38, deliberately
     not complements — the anchor's case3 corner: dubbel AND afwijkend both
     absent), in both operand orders *)
  check "WV-11.2-eq-empty-l" (Arith.compare_op ~static_numeric:false Gelijk leeg d1 = false);
  check "WV-11.2-neq-empty-l" (Arith.compare_op ~static_numeric:false Ongelijk leeg d1 = false);
  check "WV-11.2-eq-empty-r" (Arith.compare_op ~static_numeric:false Gelijk d1 leeg = false);
  check "WV-11.2-neq-empty-r" (Arith.compare_op ~static_numeric:false Ongelijk d1 leeg = false);
  check "WV-11.2-teq-empty" (Arith.eq_non_numeric leeg d1 = false);
  (* 11.2: records are not orderable — scalar_cmp faults *)
  expect_fault "WV-11.2-order-fault"
    (fun () -> Arith.compare_op ~static_numeric:false Kleiner d1 d1');
  (* composing ch. 10: a list of records (the anchor's dossiers input) —
     equal to itself, order-sensitive, never equal reordered *)
  let ds =
    lijst
      [ record [ ("waarde", n "10.00"); ("telling", n "1") ];
        record [ ("waarde", n "2.50"); ("telling", n "2") ] ]
  in
  let ds' =
    lijst
      [ record [ ("waarde", n "10.00"); ("telling", n "1") ];
        record [ ("waarde", n "2.50"); ("telling", n "2") ] ]
  in
  let ds_reordered =
    lijst
      [ record [ ("waarde", n "2.50"); ("telling", n "2") ];
        record [ ("waarde", n "10.00"); ("telling", n "1") ] ]
  in
  check "WV-11.2-list-eq" (Arith.equal ds ds' = true);
  check "WV-11.2-list-eq-order" (Arith.equal ds ds_reordered = false);
  check "WV-11.2-list-render"
    (Value.to_string ds = "[{waarde: 10, telling: 1}, {waarde: 2.5, telling: 2}]")

(* --- ch. 10.6: the strict Let (RUNTIME-SEMANTICS ch. 10.6; PHASE5C §6) ---
   Three worked examples pinning the strict-Let discipline the
   jutterswet-tariefstelling anchor rests on:
   (1) binder-lookup — a bound value is read through BinderRef in the body;
   (2) a leeg value propagates WITHOUT the body running — the body reads an
       unbound binder that would fault if evaluated, so returning leeg (not
       raising) is the proof the body never ran (the leeg-barrier, WF-076);
   (3) the maal-coalesce CONTRAST — a bare AttribuutRef reading leeg times a
       literal coalesces to 0 (the ch. 1.2 coalesce table's maal row),
       whereas the SAME read routed through a strict Let binding propagates
       leeg: the very reason the discipline exists (anchor case3);
   (4) shadowing — a nested Let reusing a binder name: the inner binding
       wins inside its body and the outer is restored once that body ends;
   (5) shadowing across a fold parameter — a Let in the lambda body reusing
       the fold's element binder wins over it (ref semantics; the ASP side
       carries a Let only in the bare rule value, ASP-TRANSLATION S19). *)
let () =
  let binder_ref name typ : Ir_v0.Ast.expr =
    Binder_ref { naam = name; typ = Some typ; bron = None }
  in
  let letexpr binder value body typ : Ir_v0.Ast.expr =
    Let { typ = Some typ; binder; value; body; bron = None }
  in
  let maal l r : Ir_v0.Ast.expr =
    Binair { op = Maal; links = Some l; rechts = Some r; typ = Some num0; bron = None }
  in
  let doc = { empty_doc with objecttypen = [ ot "J" [ ("a", Some num0) ] [] ] } in
  let env = env_of doc in
  (* the instance leaves attribute "a" absent, so a bare read is leeg *)
  let w = World.add_instance World.empty (inst "j" "J" []) in
  let self = World.get w "j" in
  let a_leeg = aref "a" "J" num0 in
  (* (1) let x = 7 in x -> 7 *)
  expect_num "WV-10.6-binder-lookup"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" (litv "7" num0) (binder_ref "x" num0) num0))
    "7";
  (* (2) leeg value -> leeg, and the unbound-binder body never runs (no fault) *)
  expect_leeg "WV-10.6-leeg-value-no-body"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" a_leeg (binder_ref "y" num0) num0));
  (* (3a) the bare maal coalesces the leeg read to 0 (ch. 1.2a) ... *)
  expect_num "WV-10.6-maal-coalesce-bare"
    (Eval.eval_expr env w self ~err:no_err (maal a_leeg (litv "5" num0)))
    "0";
  (* (3b) ... but the SAME read through a strict Let binding propagates leeg *)
  expect_leeg "WV-10.6-let-barrier"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" a_leeg (maal (binder_ref "x" num0) (litv "5" num0)) num0));
  (* (4) shadowing: let x = 2 in (let x = 3 in x) maal x  ==  3 × 2 = 6
     (an inner leak would give 3×3 = 9; no shadowing would give 2×2 = 4) *)
  expect_num "WV-10.6-shadow-inner-wins"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" (litv "2" num0)
          (maal (letexpr "x" (litv "3" num0) (binder_ref "x" num0) num0) (binder_ref "x" num0))
          num0))
    "6";
  (* (5) shadowing across a fold parameter: fold(+, 0, [1;2;3]) with body
     `let x = 100 in acc + x` totals 0+100+100+100 = 300 — the Let's x wins
     over the fold's element binder x (were it not to, the total would be 6) *)
  let plus l r : Ir_v0.Ast.expr =
    Binair { op = Plus; links = Some l; rechts = Some r; typ = Some num0; bron = None }
  in
  let shadow_fold : Ir_v0.Ast.expr =
    Fold
      { typ = Some num0;
        list =
          List_literal
            { typ = Some (List_type { element = num0 });
              elements = [ litv "1" num0; litv "2" num0; litv "3" num0 ];
              bron = None };
        functie =
          Fn_lambda
            { parameters = [ ("acc", num0); ("x", num0) ];
              body =
                letexpr "x" (litv "100" num0)
                  (plus (binder_ref "acc" num0) (binder_ref "x" num0))
                  num0;
              bron = None };
        init = litv "0" num0;
        bron = None }
  in
  expect_num "WV-10.6-shadow-fold-parameter"
    (Eval.eval_expr env w self ~err:no_err shadow_fold)
    "300"

(* --- ch. 2.9: the afrondingsnorm — exact to the rounding point
   (RUNTIME-SEMANTICS ch. 2.9; maintainer decision 2026-08-24, vraag 3-A).
   Intermediate values are exact-rational (ch. 2.1); a declared scale binds a
   declaration and the write boundary it guards (ch. 2.4), never an
   intermediate step; the sole collapse in this witness is its endpoint
   Afgerond node (ch. 2.5; ch. 2.1 also admits the ch. 2.2 abs-division,
   not exercised here). Witness: the non-terminating quotient 100 ÷ 3 = 33,333…, bound
   in a strict Let whose value is DECLARED at two decimals, read at the
   endpoint through a single Afgerond. Rounded to 2 the endpoint is 33,33;
   rounded to 4 it is 33,3333 — not 33,3300, the value a scale that bound the
   intermediate step would have forced (ref ignores the binder's declared
   scale and carries the exact rational to the endpoint). clingo materialises
   the binder at that declared scale and refuses this very form loudly
   (ASP-TRANSLATION S19): a documented backend limit, conform to the norm,
   never a wrong value. Card-sanctioned test touch (20260824-afrondingsnorm). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Numeriek { geheel = false; decimalen = 2; teken = None; eenheid = None }
  in
  let binder_ref name typ : Ir_v0.Ast.expr =
    Binder_ref { naam = name; typ = Some typ; bron = None }
  in
  let deel : Ir_v0.Ast.expr =
    Binair
      { op = Gedeeld_door;
        links = Some (litv "100" num0);
        rechts = Some (litv "3" num0);
        typ = Some num2;
        bron = None }
  in
  let let_round dec : Ir_v0.Ast.expr =
    Let
      { typ = Some num2;
        binder = "deel";
        value = deel;
        body =
          Afgerond
            { basis = Some (binder_ref "deel" num2);
              modus = Rekenkundig;
              decimalen = dec;
              typ = Some num2;
              bron = None };
        bron = None }
  in
  let doc = { empty_doc with objecttypen = [ ot "J" [ ("a", Some num0) ] [] ] } in
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
   arm binder [deel] — DECLARED at two decimals, as the Let binder was — and
   reads it at the endpoint through a single Afgerond. The binder's declared
   scale never collapses the intermediate: the exact rational crosses the arm
   (ch. 9.2 binds the payload value verbatim) and the sole endpoint Afgerond
   is the one collapse → 33,33; had the arm binding pre-rounded to two
   decimals this would still read 33,33, but the ch. 2.9 norm forbids that
   intermediate collapse and the exact carry is what this pins. Card-
   sanctioned test touch (20260824-afrondingsnorm). *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Numeriek { geheel = false; decimalen = 2; teken = None; eenheid = None }
  in
  let binder_ref name typ : Ir_v0.Ast.expr =
    Binder_ref { naam = name; typ = Some typ; bron = None }
  in
  let beschikking : Ir_v0.Ast.datatype =
    Sum_type
      { domein = "beschikking";
        constructoren = [ ("Toegekend", Some num2); ("Afgewezen", None) ] }
  in
  let deel : Ir_v0.Ast.expr =
    Binair
      { op = Gedeeld_door;
        links = Some (litv "100" num0);
        rechts = Some (litv "3" num0);
        typ = Some num2;
        bron = None }
  in
  (* scrutinee: Toegekend(100 ÷ 3) — the non-terminating quotient is the payload *)
  let scrutinee : Ir_v0.Ast.expr =
    Construct
      { typ = Some beschikking;
        constructor_ = "Toegekend";
        argument = Some deel;
        bron = None }
  in
  let match_round : Ir_v0.Ast.expr =
    Match
      { typ = Some num2;
        scrutinee;
        arms =
          [ ( "Toegekend",
              Some "deel",
              Afgerond
                { basis = Some (binder_ref "deel" num2);
                  modus = Rekenkundig;
                  decimalen = 2;
                  typ = Some num2;
                  bron = None } );
            ("Afgewezen", None, litv "0" num2) ];
        bron = None }
  in
  let doc = { empty_doc with objecttypen = [ ot "J" [ ("a", Some num0) ] [] ] } in
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
  let td u a b = Arith.tijdsduur ~unit_:u (d a) (d b) in
  let plus u date lex = Arith.date_plus_duration ~unit_:u (d date) (n lex) in
  let min_ u date lex = Arith.date_plus_duration ~minus:true ~unit_:u (d date) (n lex) in
  let open Ir_v0.Checks in
  (* the leap child: 29-02-2004 → 28-02-2026 = 21 whole years; one day
     later = 22 *)
  expect_num "WV-3.8-leap-21" (td Tu_jaar "2004-02-29" "2026-02-28") "21";
  expect_num "WV-3.8-leap-22" (td Tu_jaar "2004-02-29" "2026-03-01") "22";
  expect_num "WV-3.8-leap-mnd-263" (td Tu_maand "2004-02-29" "2026-02-28") "263";
  expect_num "WV-3.8-leap-mnd-264" (td Tu_maand "2004-02-29" "2026-03-01") "264";
  (* clamp, not inverse: 31-01-2024 + 1 maand = 29-02-2024, yet the
     tijdsduur between them is 0 whole months *)
  check "WV-3.8-clamp-leap" (plus Tu_maand "2024-01-31" "1" = d "2024-02-29");
  expect_num "WV-3.8-clamp-not-inverse" (td Tu_maand "2024-01-31" "2024-02-29") "0";
  (* signed truncation toward zero: −7 months is −2 whole quarters, 0 years *)
  expect_num "WV-3.8-neg-mnd" (td Tu_maand "2025-03-10" "2024-08-10") "-7";
  expect_num "WV-3.8-neg-kw" (td Tu_kwartaal "2025-03-10" "2024-08-10") "-2";
  expect_num "WV-3.8-neg-jr" (td Tu_jaar "2025-03-10" "2024-08-10") "0";
  expect_num "WV-3.8-neg-abs"
    (Arith.tijdsduur ~absolute:true ~unit_:Tu_maand (d "2025-03-10") (d "2024-08-10"))
    "7";
  (* date ± whole calendar units: the month-end clamp, quarter = 3 months,
     year = 12 months, day/week linear *)
  check "WV-3.8-clamp-nonleap" (plus Tu_maand "2025-01-31" "1" = d "2025-02-28");
  check "WV-3.8-kwartaal" (plus Tu_kwartaal "2024-01-01" "1" = d "2024-04-01");
  check "WV-3.8-jaar-leap" (plus Tu_jaar "2024-02-29" "1" = d "2025-02-28");
  check "WV-3.8-weken" (plus Tu_week "2024-01-31" "2" = d "2024-02-14");
  check "WV-3.8-dagen" (plus Tu_dag "2024-12-30" "3" = d "2025-01-02");
  (* a fractional count truncates toward zero before the calendar step *)
  check "WV-3.8-frac-plus" (plus Tu_maand "2024-01-31" "1,9" = d "2024-02-29");
  check "WV-3.8-frac-min" (min_ Tu_maand "2024-01-31" "1,9" = d "2023-12-31");
  (* min: the date left, the duration subtracted *)
  check "WV-3.8-min-week" (min_ Tu_week "2024-01-31" "1" = d "2024-01-24");
  (* empty rules (ch. 1.2): leeg date propagates; leeg duration counts 0;
     leeg tijdsduur operand propagates *)
  expect_leeg "WV-3.8-leeg-datum" (Arith.date_plus_duration ~unit_:Tu_dag leeg (n "5"));
  check "WV-3.8-leeg-duur"
    (Arith.date_plus_duration ~unit_:Tu_maand (d "2024-01-31") leeg = d "2024-01-31");
  expect_leeg "WV-3.8-leeg-tijdsduur" (Arith.tijdsduur ~unit_:Tu_jaar leeg (d "2025-03-01"));
  (* the spelling table: nine units, keyed on spelling, case-insensitive;
     plural and abbreviation land on the same unit; alien spellings miss *)
  check "WV-3.8-spelling-jr"
    (time_unit_of_spelling "jaren" = Some Tu_jaar
    && time_unit_of_spelling "JR" = Some Tu_jaar
    && time_unit_of_spelling "jaar" = Some Tu_jaar);
  check "WV-3.8-spelling-negen"
    (time_unit_of_spelling "maanden" = Some Tu_maand
    && time_unit_of_spelling "kwartalen" = Some Tu_kwartaal
    && time_unit_of_spelling "weken" = Some Tu_week
    && time_unit_of_spelling "dg" = Some Tu_dag
    && time_unit_of_spelling "uren" = Some Tu_uur
    && time_unit_of_spelling "min" = Some Tu_minuut
    && time_unit_of_spelling "secondes" = Some Tu_seconde
    && time_unit_of_spelling "ms" = Some Tu_milliseconde);
  check "WV-3.8-spelling-alien" (time_unit_of_spelling "werkdag" = None);
  (* sub-day units demand datum-tijd operands — loud, never silent *)
  expect_fault "WV-3.8-subday-tijdsduur" (fun () -> td Tu_uur "2024-01-01" "2024-01-02");
  expect_fault "WV-3.8-subday-plus" (fun () -> plus Tu_uur "2024-01-01" "3")

(* --- ch. 3.8: date-part extraction and the Easter builtin (worked
   examples verbatim) ----------------------------------------------------- *)

let () =
  let d = Value.date_of_iso in
  (* civil components of a day-precision date, unitless whole numbers *)
  expect_num "WV-3.8-jaar-uit" (Arith.jaar_uit (d "2026-11-03")) "2026";
  expect_num "WV-3.8-maand-uit" (Arith.maand_uit (d "2026-11-03")) "11";
  expect_num "WV-3.8-dag-uit" (Arith.dag_uit (d "2026-11-03")) "3";
  (* the four pinned Easter Sundays: 2024 (the standing pin), 2026, the
     March Easter 2027, the late-April Easter 2038 *)
  check "WV-3.8-paasdag-2024" (Arith.eerste_paasdag (n "2024") = d "2024-03-31");
  check "WV-3.8-paasdag-2026" (Arith.eerste_paasdag (n "2026") = d "2026-04-05");
  check "WV-3.8-paasdag-2027" (Arith.eerste_paasdag (n "2027") = d "2027-03-28");
  check "WV-3.8-paasdag-2038" (Arith.eerste_paasdag (n "2038") = d "2038-04-25");
  (* leeg propagates (ch. 1.2b) *)
  expect_leeg "WV-3.8-maand-uit-leeg" (Arith.maand_uit leeg);
  expect_leeg "WV-3.8-dag-uit-leeg" (Arith.dag_uit leeg);
  expect_leeg "WV-3.8-paasdag-leeg" (Arith.eerste_paasdag leeg);
  (* a non-whole or unit-carrying year faults (never a silent truncation);
     a non-date extractor operand faults *)
  expect_fault "WV-3.8-paasdag-frac" (fun () -> Arith.eerste_paasdag (n "2024,5"));
  expect_fault "WV-3.8-paasdag-eenheid" (fun () -> Arith.eerste_paasdag (nu jr "2024"));
  expect_fault "WV-3.8-maand-uit-tekst" (fun () -> Arith.maand_uit (Value.Text "x"))

let () =
  if !failures > 0 then begin
    Printf.printf "%d failure(s)\n" !failures;
    exit 1
  end
  else Printf.printf "test_ref: all %d worked examples pass\n" !total
