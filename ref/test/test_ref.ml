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

(* The last worked example that REPORTED. A fault raised while computing the
   next example's value escapes before its own [check] ever runs, so the binary
   used to die on a bare "Fatal error: exception Value.Fault" with no clue which
   example was in flight — measured on the mutant harness, which killed two
   mutants with zero report lines. The handler below turns that into a named
   report. Nothing here changes an assertion or its verdict: [laatste] is a
   write-only breadcrumb and the handler runs only on the way out. *)
let laatste = ref "<nog geen voorbeeld gerapporteerd>"

let check name ok =
  incr total;
  laatste := name;
  if not ok then begin
    incr failures;
    Printf.printf "FAIL %s\n" name
  end

let () =
  Printexc.set_uncaught_exception_handler (fun e bt ->
      (match e with
      | Value.Fault m ->
          Printf.printf
            "FAIL <onafgevangen fault> na het laatst gerapporteerde voorbeeld %S\n  fault: %s\n"
            !laatste m
      | e ->
          Printf.printf "FAIL <onafgevangen uitzondering> na het laatst gerapporteerde voorbeeld %S\n  %s\n"
            !laatste (Printexc.to_string e));
      Printexc.print_raw_backtrace stdout bt;
      flush stdout)

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
   check "WV-1.2d-date-neq-2" (cmp Ongelijk leeg leeg = false));
  (* Tabel 44 (§5.6): inequality is uniform over EVERY datatype — exactly
     one empty operand → waar, both empty → onwaar. Text, enum and boolean
     each mirror the numeric one-empty cell (WV-1.2d-neq-1 above); equality
     stays any-empty → onwaar (§5 Tabel 38). static_numeric is false so a
     value-carrying pair takes the non-numeric route; two empties are
     typeless at runtime, so the shared both-empty cell is checked once. *)
  let nn op a b = Arith.compare_op ~static_numeric:false op a b in
  check "WV-1.2d-txt-neq-one-l" (nn Ongelijk leeg (Value.Text "zeevondst") = true);
  check "WV-1.2d-txt-neq-one-r" (nn Ongelijk (Value.Text "zeevondst") leeg = true);
  check "WV-1.2d-txt-neq-val" (nn Ongelijk (Value.Text "a") (Value.Text "b") = true);
  check "WV-1.2d-txt-neq-eqval" (nn Ongelijk (Value.Text "a") (Value.Text "a") = false);
  check "WV-1.2d-enum-neq-one-l" (nn Ongelijk leeg (Value.Enum "gaaf") = true);
  check "WV-1.2d-enum-neq-val" (nn Ongelijk (Value.Enum "gaaf") (Value.Enum "wrakhout") = true);
  check "WV-1.2d-bool-neq-one-l" (nn Ongelijk leeg (Value.Bool true) = true);
  check "WV-1.2d-bool-neq-one-r" (nn Ongelijk (Value.Bool true) leeg = true);
  check "WV-1.2d-nn-neq-both" (nn Ongelijk leeg leeg = false);
  (* equality is untouched: any empty operand → onwaar for the same operands *)
  check "WV-1.2d-txt-eq-one" (nn Gelijk leeg (Value.Text "zeevondst") = false);
  check "WV-1.2d-enum-eq-one" (nn Gelijk leeg (Value.Enum "gaaf") = false);
  check "WV-1.2d-bool-eq-one" (nn Gelijk leeg (Value.Bool true) = false)

(* --- ch. 1.2e: aggregations (the §5.8.2/§4.20 split) -------------------- *)

let () =
  expect_num "WV-1.2e-som-1" (Arith.agg_som ~fallback:None [ n "5"; leeg; n "3" ]) "8";
  expect_leeg "WV-1.2e-som-2" (Arith.agg_som ~fallback:None [ leeg; leeg ]);
  expect_num "WV-1.2e-som-3" (Arith.agg_som ~fallback:(Some (n "0")) [ leeg; leeg ]) "0";
  expect_num "WV-1.2e-totaal-1" (Arith.agg_totaal []) "0";
  expect_num "WV-1.2e-aantal-1" (Arith.agg_aantal []) "0";
  expect_num "WV-1.2e-min-1" (Arith.agg_min [ n "5"; leeg; n "3" ]) "3";
  expect_leeg "WV-1.2e-min-2" (Arith.agg_min [ leeg; leeg ]);
  (* eerste/laatste are ORDERING folds (ch. 1.2e): least/earliest and
     greatest/latest over the operand values, never a positional pick *)
  expect_num "WV-1.2e-eerste-1" (Arith.agg_eerste [ n "5"; leeg; n "3" ]) "3";
  expect_num "WV-1.2e-laatste-1" (Arith.agg_laatste [ n "5"; leeg; n "3" ]) "5";
  check "WV-1.2e-eerste-2"
    (Arith.agg_eerste [ Value.date_of_iso "2026-06-21"; Value.date_of_iso "2026-05-01" ]
    = Value.date_of_iso "2026-05-01");
  check "WV-1.2e-laatste-2"
    (Arith.agg_laatste [ Value.date_of_iso "2026-06-21"; leeg ]
    = Value.date_of_iso "2026-06-21");
  expect_leeg "WV-1.2e-eerste-3" (Arith.agg_eerste [ leeg; leeg ]);
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
let kw = unit_ ~canoniek:"mnd" ~factor:3.0 "kw"

(* a declared month-sized unit that is NOT spelled as a calendar unit: the
   ch. 3.6 integration cancels it through the declared factor, never
   through the spelling table *)
let bm = unit_ ~canoniek:"mnd" "bm"

(* a day unit in its OWN canonical chain: the declarations connect it to
   nothing month-sized, so nothing may convert it *)
let dg = unit_ ~canoniek:"dg" "dg"
let kg = unit_ ~canoniek:"kg" "kg"
let eur = unit_ ~canoniek:"EUR" "€"
let cent = unit_ ~canoniek:"EUR" ~factor:0.01 "ct"
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
  | _ -> check "ch2.6-add-restate" false);
  (* a unit fault must NAME the two units apart: a rate and its numerator
     both printed as the numerator's abbreviation, so every mismatch
     between them read "€ vs €" and said nothing *)
  check "verify-show-unit-noemer"
    (Arith.show_unit (Value.Unit eur_per_jr) = "€/jr" && Arith.show_unit (Value.Unit eur) = "€");
  (match Arith.restate_into (Value.Unit eur) (q "1") (Value.Unit eur_per_jr) with
  | exception Value.Fault m ->
      check "verify-show-unit-in-fault" (m = "incompatible units: € vs €/jr")
  | _ -> check "verify-show-unit-in-fault" false)

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
   no canonical target: measured on the wf092-factor-zonder-canoniek
   negatief, ref and the ASP backend both replayed a clean 2800 while a
   third executor computed 302.5 — nothing refused. Both now fault by name,
   so no executor computes quietly on a shape the verifier rejects. *)
let mnd_kaal : Ir_v0.Ast.eenheid =
  { afkorting = Some "mnd"; stelsel = Some "test"; canoniek = None;
    factor_naar_canoniek = None; noemer = None }

let jr_half : Ir_v0.Ast.eenheid = { jr with factor_naar_canoniek = None }
let mnd_half_b : Ir_v0.Ast.eenheid = { mnd with canoniek = None }

let () =
  (* the bare form: unchanged, factor 1 *)
  check "verify-wf092-kaal-is-factor-1" (Q.equal (Arith.factor mnd_kaal) Q.one);
  (match Arith.add (nu mnd_kaal "2") (nu mnd_kaal "12") with
  | Value.Num (v, Value.Unit u) ->
      check "verify-wf092-kaal-telt-op" (Q.equal v (q "14") && u.afkorting = Some "mnd")
  | _ -> check "verify-wf092-kaal-telt-op" false);
  (* …and it is a canonical TARGET, not an island: `jr` declares canonical
     "mnd", the bare unit spells "mnd" as its abbreviation, so they are
     convertible and 2 jr restates on jr's own factor 12 *)
  (match Arith.add (nu mnd_kaal "3") (nu jr "2") with
  | Value.Num (v, Value.Unit u) ->
      check "verify-wf092-kaal-is-canoniek-doel"
        (Q.equal v (q "27") && u.afkorting = Some "mnd")
  | _ -> check "verify-wf092-kaal-is-canoniek-doel" false);
  (* half-filled (a): a loud fault naming the unit and its target *)
  (match Arith.factor jr_half with
  | exception Value.Fault m ->
      check "verify-wf092-halfvorm-faalt-luid"
        (m = "unit jr names canonical unit \"mnd\" but carries no conversion factor into it (WF-092)")
  | _ -> check "verify-wf092-halfvorm-faalt-luid" false);
  (* and it faults where it would otherwise have lied: the restatement
     that used to read the missing factor as 1 (2 jr + 12 mnd = 3 jr) *)
  expect_fault "verify-wf092-halfvorm-faalt-in-omrekening" (fun () ->
      Arith.add (nu jr_half "2") (nu mnd "12"));
  (* half-filled (b), the mirror: a factor with nothing to convert into.
     Symmetric to (a) and equally loud — the arm that used to answer the
     factor question with the stray factor itself, unasked what it converts
     into *)
  (match Arith.factor mnd_half_b with
  | exception Value.Fault m ->
      check "verify-wf092-halfvorm-b-faalt-luid"
        (m
       = "unit mnd carries conversion factor 1 but names no canonical unit to convert into (WF-092)")
  | _ -> check "verify-wf092-halfvorm-b-faalt-luid" false);
  expect_fault "verify-wf092-halfvorm-b-faalt-in-omrekening" (fun () ->
      Arith.add (nu mnd_half_b "12") (nu jr "2"))

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
    prioritized_defaults = [];
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
      links = World.SMap.empty;
      tls = World.SMap.empty; ctls = World.SMap.empty;
    }
  in
  let kc name : Ir_v0.Ast.conditie =
    Kenmerk_check
      { kenmerk = Some name; navigatie = None; negatie = false; eigenaar = Some "Jutter";
        periode = None; bron = None; navigatie_binding = None }
  in
  let leaf name lex : Ir_v0.Ast.default =
    { default_besluit = None; default_voorwaarde = Some (kc name);
      default_consequentie = Some (litv lex num0); default_initialisatie = false;
      default_excepties = None; default_bron = None }
  in
  let sub name lex : Ir_v0.Ast.exception_group = { eg_defaults = [ leaf name lex ]; eg_bron = None } in
  let defaults : Ir_v0.Ast.prioritized_defaults =
    { pd_naam = Some "heffing"; pd_onderwerp = "Jutter"; pd_kenmerk = None;
      pd_attribuut = Some "heffing";
      defaults =
        [ { default_besluit = None; default_voorwaarde = Some (kc "grondX");
            default_consequentie = Some (litv "40" num0); default_initialisatie = false;
            default_excepties =
              Some { egs_ongeordend = true; egs_leden = [ sub "grondY" "10"; sub "grondZ" "20" ] };
            default_bron = None } ];
      pd_ongeordend = false; pd_bron = None; pd_geldigheid = None }
  in
  let doc =
    { empty_doc with
      objecttypen = [ ot "Jutter" [ ("heffing", Some num0) ] [ "grondX"; "grondY"; "grondZ" ] ];
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
  expect_num "WV-3.6-f" (Timeline.totaal month (Timeline.clip vals tp)) "500";
  (* the explicit rate operand: 10 €/mnd across 2024 integrates to 120 €,
     denominator cancelled against the grid's own unit *)
  let eur_per_mnd = { eur with noemer = Some mnd } in
  let jaar_2024 v = [ seg "2024-01-01" "2025-01-01" v ] in
  let grid u = Ok u in
  (match Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu eur_per_mnd "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g" (Q.equal v (q "120") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-g (got " ^ Value.to_string other ^ ")") false);
  (* a plain € value on the same per-month timeline keeps the plain rule:
     the same 120, and no denominator to cancel *)
  (match Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu eur "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-plain-unit"
        (Q.equal v (q "120") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-g-plain-unit (got " ^ Value.to_string other ^ ")") false);
  (* the three readings of the SAME grid: unitless, plain €, and the €/mnd
     rate. The plain rule and the €/mnd integration agree on the NUMBER
     (12×10); the claim only holds where the denominator matches the grid
     one-for-one — WV-3.6-g-vreemde-noemer below is the counter-example. *)
  check "WV-3.6-g-getallen-identiek"
    (List.for_all
       (fun v ->
         match Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 v) with
         | Value.Num (got, _) -> Q.equal got (q "120")
         | _ -> false)
       [ n "10"; nu eur "10"; nu eur_per_mnd "10" ]);
  (* the FOREIGN but CONVERTIBLE denominator integrates EXACTLY through the
     declared factor: 12 months IS one year, so 10 €/jr over 2024 is 10 €,
     not 120 (maintainer decision 2026-09-04) *)
  (match Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu eur_per_jr "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-jaarnoemer-exact"
        (Q.equal v (q "10") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-g-jaarnoemer-exact (got " ^ Value.to_string other ^ ")") false);
  (* a month-sized denominator that is NOT a calendar spelling (factor 1 to
     the same canonical month): the declared factor carries it, the
     spelling table never enters *)
  let eur_per_bm = { eur with noemer = Some bm } in
  expect_num "WV-3.6-g-bm-jaar"
    (Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu eur_per_bm "10")))
    "120";
  expect_num "WV-3.6-g-bm-bucket"
    (Timeline.totaal ~grid_unit:(grid mnd)
       month
       [ seg "2024-01-01" "2024-02-01" (nu eur_per_bm "10") ])
    "10";
  (* a COARSER grid: 10 €/mnd over 2024 on a per-quarter grid is 4 quarters
     × 3 months × 10 = 120 €, exactly the per-month reading *)
  (match Timeline.totaal ~grid_unit:(grid kw) Kwartaal (jaar_2024 (nu eur_per_mnd "10")) with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-g-kwartaalrooster"
        (Q.equal v (q "120") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-g-kwartaalrooster (got " ^ Value.to_string other ^ ")") false);
  (* a denominator the declarations connect to nothing month-sized is
     refused loudly, and the refusal NAMES BOTH units — the value's and
     the grid's *)
  let eur_per_dg = { eur with noemer = Some dg } in
  (match Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu eur_per_dg "10")) with
  | exception Value.Fault m ->
      check "WV-3.6-g-vreemde-noemer"
        (m
        = "the temporal total of a €/dg value cannot integrate over a mnd grid: the \
           declarations connect no conversion between its denominator and the grid unit \
           (ch. 3.6)")
  | _ -> check "WV-3.6-g-vreemde-noemer" false);
  (* a denominator that names no time unit at all is refused the same way *)
  expect_fault "WV-3.6-g-noemer-zonder-tijdseenheid" (fun () ->
      Timeline.totaal ~grid_unit:(grid mnd) month (jaar_2024 (nu { eur with noemer = Some kg } "10")));
  (* no grid unit to integrate against: the refusal carries the lookup's
     own reason, and it bites the RATE operand only — the plain and
     unitless readings above total in a document that declares no unit *)
  expect_fault "WV-3.6-g-geen-roostereenheid" (fun () ->
      Timeline.totaal ~grid_unit:(Error "the document declares no mnd unit") month
        (jaar_2024 (nu eur_per_mnd "10")));
  expect_num "WV-3.6-g-geen-roostereenheid-plain"
    (Timeline.totaal ~grid_unit:(Error "the document declares no mnd unit") month
       (jaar_2024 (n "10")))
    "120";
  (* segments of ONE timeline carrying DIFFERENT units: the collapse sums
     the per-segment products with the ordinary ch. 2.6 addition, so a
     convertible sister unit is restated EXACTLY (120 € + 500 ct = 125 €)
     and a foreign one is refused loudly; only the unitless side adopts
     silently. The case loader attaches the declaration's ONE unit to
     every segment, so no case can reach these shapes. *)
  let ct_per_mnd = { cent with noemer = Some mnd } in
  expect_num "WV-3.6-g-segmentmenging-convertibel"
    (Timeline.totaal ~grid_unit:(grid mnd) month
       [ seg "2024-01-01" "2025-01-01" (nu eur_per_mnd "10");
         seg "2025-01-01" "2025-02-01" (nu ct_per_mnd "500") ])
    "125";
  expect_fault "WV-3.6-g-segmentmenging-niet-convertibel" (fun () ->
      Timeline.totaal ~grid_unit:(grid mnd) month
        [ seg "2024-01-01" "2024-02-01" (nu eur "10");
          seg "2024-02-01" "2024-03-01" (nu meter "3") ]);
  expect_num "WV-3.6-g-segmentmenging-unitless-adopteert"
    (Timeline.totaal ~grid_unit:(grid mnd) month
       [ seg "2024-01-01" "2024-02-01" (n "10");
         seg "2024-02-01" "2024-03-01" (nu eur "5") ])
    "15"

(* the grid unit is looked up in the DOCUMENT's declared unit systems
   (ch. 3.6 / ch. 3.8 spelling table), once per fold: present, absent, and
   ambiguous each have their own answer — never a first-match guess *)
let () =
  let stelsel name eenheden : string * Ir_v0.Ast.eenheidstelsel =
    ( name,
      { es_naam = Some name;
        eenheden =
          List.map
            (fun (e : Ir_v0.Ast.eenheid) -> (Option.value e.afkorting ~default:"?", Some e))
            eenheden;
        aliassen = [] } )
  in
  let doc_of stelsels = { empty_doc with eenheidstelsels = stelsels } in
  (match Timeline.grid_unit (doc_of [ stelsel "Munt" [ eur ]; stelsel "Tijd" [ mnd; jr ] ]) Maand with
  | Ok u -> check "WV-3.6-rooster-lookup" (u.afkorting = Some "mnd")
  | Error _ -> check "WV-3.6-rooster-lookup" false);
  (match Timeline.grid_unit (doc_of [ stelsel "Munt" [ eur ]; stelsel "Tijd" [ mnd; jr ] ]) Jaar with
  | Ok u -> check "WV-3.6-rooster-lookup-jaar" (u.afkorting = Some "jr")
  | Error _ -> check "WV-3.6-rooster-lookup-jaar" false);
  (match Timeline.grid_unit (doc_of [ stelsel "Munt" [ eur ] ]) Maand with
  | Error _ -> check "WV-3.6-rooster-ongedeclareerd" true
  | Ok _ -> check "WV-3.6-rooster-ongedeclareerd" false);
  (* two systems spelling the same grid unit with different canonical
     chains: no single grid, so the lookup refuses instead of picking *)
  let mnd_in_dagen = unit_ ~canoniek:"dg" ~factor:30.0 "mnd" in
  (match Timeline.grid_unit (doc_of [ stelsel "A" [ mnd ]; stelsel "B" [ mnd_in_dagen ] ]) Maand with
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
  let eur_per_mnd = { eur with noemer = Some mnd } in
  let num_eur_mnd : Ir_v0.Ast.datatype =
    Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = Some eur_per_mnd }
  in
  let num_eur : Ir_v0.Ast.datatype =
    Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = Some eur }
  in
  let vondst =
    let nm, o = ot "Vondst" [ ("bewaartarief", Some num_eur_mnd) ] [] in
    (nm, { o with attribuut_granulariteit = [ ("bewaartarief", Some Ir_v0.Ast.Maand) ] })
  in
  (* a SAME-NAMED temporal attribute at a DIFFERENT granularity elsewhere:
     the fold takes the grid off the ELEMENT type's declaration, never off
     the name *)
  let kist =
    let nm, o = ot "Kist" [ ("bewaartarief", Some num_eur_mnd) ] [] in
    (nm, { o with attribuut_granulariteit = [ ("bewaartarief", Some Ir_v0.Ast.Jaar) ] })
  in
  let stelsels : (string * Ir_v0.Ast.eenheidstelsel) list =
    [ ("Munt", { es_naam = Some "Munt"; eenheden = [ ("€", Some eur) ]; aliassen = [] });
      ("Tijd", { es_naam = Some "Tijd"; eenheden = [ ("mnd", Some mnd) ]; aliassen = [] }) ]
  in
  let doc =
    {
      empty_doc with
      eenheidstelsels = stelsels;
      objecttypen = [ ot "Jutter" [ ("bewaarlast", Some num_eur) ] []; vondst; kist ];
      feittypen =
        [ ( "bewaring",
            {
              ft_naam = Some "bewaring";
              wederkerig = false;
              rollen =
                [
                  { rol_naam = Some "bewarende jutter"; rol_objecttype_naam = Some "Jutter";
                    rol_meervoud = None; rol_meervoudig = false };
                  { rol_naam = Some "vondst"; rol_objecttype_naam = Some "Vondst";
                    rol_meervoud = Some "vondsten"; rol_meervoudig = true };
                ];
            } ) ];
    }
  in
  let fold kind : Ir_v0.Ast.expr =
    Functie
      { kind;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some "bewaartarief"; objecttype = Some "Vondst";
                onderwerp_bron = None; navigatie_rol = None; typ = Some num_eur_mnd;
                bron = None; set = true; navigatie_pad = []; navigatie_bindingen = None } ];
        element_objecttype = Some "Vondst"; leegwaarde = None; filter = None;
        dimensie_selectie = None; typ = Some num_eur; bron = None; granulariteit = None;
        bron_rol = None }
  in
  let tl_of segs label = { (inst label "Vondst" []) with
                           tls = World.SMap.add "bewaartarief" segs World.SMap.empty } in
  let jutter labels =
    { (inst "j1" "Jutter" []) with
      links = World.SMap.add "vondsten" labels World.SMap.empty }
  in
  let world_of j members = List.fold_left World.add_instance (World.add_instance World.empty j) members in
  let v1 = tl_of [ seg "2024-01-01" "2025-01-01" (nu eur_per_mnd "10") ] "v1" in
  let v2 = tl_of [ seg "2024-01-01" "2024-07-01" (nu eur_per_mnd "5") ] "v2" in
  let v3 = inst "v3" "Vondst" [] in
  let w = world_of (jutter [ "v1"; "v2"; "v3" ]) [ v1; v2; v3 ] in
  let env = env_of doc in
  let run w kind = Eval.eval_expr env w (World.get w "j1") ~err:no_err (fold kind) in
  (match run w Totaal with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-h" (Q.equal v (q "150") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-h (got " ^ Value.to_string other ^ ")") false);
  (* de som van over the same shape folds identically inside *)
  (match run w Som with
  | Value.Num (v, Value.Unit u) ->
      check "WV-3.6-h-som" (Q.equal v (q "150") && u.afkorting = Some "€" && u.noemer = None)
  | other -> check ("WV-3.6-h-som (got " ^ Value.to_string other ^ ")") false);
  (* every ranking or counting fold over the same shape is refused: there
     is no defined moment of the timeline to rank or count at *)
  List.iter
    (fun (label, kind) -> expect_fault ("WV-3.6-h-rangschikking-" ^ label) (fun () -> run w kind))
    [ ("minimum", Ir_v0.Ast.Minimum); ("maximum", Maximum); ("eerste", Eerste);
      ("laatste", Laatste); ("aantal", Aantal) ];
  (* the convention of this fold: a member carrying the asOfDate-bucket
     SCALAR brings no horizon of its own, so the integration cannot run —
     refused loudly, NAMING the member (ch. 8.4's "needs the one, finds
     the other"), never silently bucketed at the session date *)
  let bucket_lid label =
    { (inst label "Vondst" []) with
      attrs = World.SMap.add "bewaartarief" (nu eur_per_mnd "10") World.SMap.empty }
  in
  let w_bucket = world_of (jutter [ "v9" ]) [ bucket_lid "v9" ] in
  (match run w_bucket Totaal with
  | exception Value.Fault m ->
      check "WV-3.6-h-bucketlid"
        (m
        = "the per-element temporal total of \"bewaartarief\" over \"v9\": the case supplies an \
           asOfDate-bucket scalar, which carries no period of its own; a set fold needs each \
           member's own timeline (ch. 3.6/§7.1, ch. 8.4)")
  | _ -> check "WV-3.6-h-bucketlid" false);
  (* mixing a full timeline with a horizonless bucket member fails on the
     bucket member alone — timelines stay admissible beside it *)
  let w_menging = world_of (jutter [ "v1"; "v9" ]) [ v1; bucket_lid "v9" ] in
  expect_fault "WV-3.6-h-menging" (fun () -> run w_menging Totaal);
  (* an ABSENT member contributes nothing: all-absent totals 0 (the §4.20
     unconditional 0), while de som van keeps its leeg rule outside *)
  let w_afwezig = world_of (jutter [ "v3"; "v4" ]) [ v3; inst "v4" "Vondst" [] ] in
  expect_num "WV-3.6-h-afwezig-totaal" (run w_afwezig Totaal) "0";
  expect_leeg "WV-3.6-h-afwezig-som" (run w_afwezig Som);
  (* the empty set: the same two answers *)
  let w_leeg = world_of (jutter []) [] in
  expect_num "WV-3.6-h-lege-set-totaal" (run w_leeg Totaal) "0";
  expect_leeg "WV-3.6-h-lege-set-som" (run w_leeg Som);
  (* the same attribute NAME at a different granularity on another object
     type leaves this fold untouched: 150 stays *)
  expect_num "WV-3.6-h-eigen-granulariteit" (run w Totaal) "150"

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
  (* 9.3: a leeg operand makes `=` false (Tabel 38, equality untouched) but
     `≠` WAAR (Tabel 44, uniform inequality — the ratified 2026-08-28 change;
     the anchor's case 4 now fires afgehandeld), in both operand orders *)
  check "WV-9.3-eq-empty-l"
    (Arith.compare_op ~static_numeric:false Gelijk leeg aangehouden = false);
  check "WV-9.3-neq-empty-l"
    (Arith.compare_op ~static_numeric:false Ongelijk leeg aangehouden = true);
  check "WV-9.3-eq-empty-r"
    (Arith.compare_op ~static_numeric:false Gelijk aangehouden leeg = false);
  check "WV-9.3-neq-empty-r"
    (Arith.compare_op ~static_numeric:false Ongelijk aangehouden leeg = true);
  (* 9.3: both operands leeg → `≠` onwaar (Tabel 44 both-empty cell) *)
  check "WV-9.3-neq-both-empty"
    (Arith.compare_op ~static_numeric:false Ongelijk leeg leeg = false);
  (* 9.3: eq_non_numeric with an empty operand is false directly *)
  check "WV-9.3-teq-empty" (Arith.eq_non_numeric leeg aangehouden = false);
  (* 9.3: sum values are not orderable — scalar_cmp faults *)
  expect_fault "WV-9.3-order-fault"
    (fun () -> Arith.compare_op ~static_numeric:false Kleiner toegekend aangehouden)

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
    Numeriek { geheel = false; decimalen = 2; teken = None; eenheid = None }
  in
  let keurklasse : Ir_v0.Ast.datatype =
    Enumeratie
      { domein = Some "keurklasse"; waarden = [ Some "gaaf"; Some "beschadigd"; Some "wrakhout" ] }
  in
  let enum_match : Ir_v0.Ast.expr =
    Match
      { typ = Some num2;
        scrutinee = aref "staat" "Vondst" keurklasse;
        arms =
          [ ("gaaf", None, litv "10.00" num2);
            ("beschadigd", None, litv "4.00" num2);
            ("wrakhout", None, litv "1.00" num2) ];
        bron = None }
  in
  let doc =
    { empty_doc with
      domeinen = [ ("keurklasse", Some keurklasse) ];
      objecttypen =
        [ ot "Vondst" [ ("staat", Some keurklasse); ("waarde", Some num2) ] [] ] }
  in
  let env = env_of doc in
  let eval staat =
    let attrs = match staat with None -> [] | Some s -> [ ("staat", Value.Enum s) ] in
    let w = World.add_instance World.empty (inst "V1" "Vondst" attrs) in
    Eval.eval_expr env w (World.get w "V1") ~err:no_err enum_match
  in
  expect_num "WV-9.2-enum-gaaf" (eval (Some "gaaf")) "10.00";
  expect_num "WV-9.2-enum-beschadigd" (eval (Some "beschadigd")) "4.00";
  expect_num "WV-9.2-enum-wrakhout" (eval (Some "wrakhout")) "1.00";
  (* the label match is ci — the norm WF-063 checked the arms under *)
  expect_num "WV-9.2-enum-ci" (eval (Some "GAAF")) "10.00";
  (* step 1: leeg in, leeg out — no implicit leeg-arm, no coalesce *)
  expect_leeg "WV-9.2-enum-leeg" (eval None)

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
  (* 11.2: a leeg operand makes `=` false (Tabel 38, equality untouched) but
     `≠` WAAR (Tabel 44, uniform inequality — the ratified 2026-08-28 change;
     the anchor's case3 keeps dubbel absent yet now fires afwijkend), in both
     operand orders; both operands leeg → `≠` onwaar *)
  check "WV-11.2-eq-empty-l" (Arith.compare_op ~static_numeric:false Gelijk leeg d1 = false);
  check "WV-11.2-neq-empty-l" (Arith.compare_op ~static_numeric:false Ongelijk leeg d1 = true);
  check "WV-11.2-eq-empty-r" (Arith.compare_op ~static_numeric:false Gelijk d1 leeg = false);
  check "WV-11.2-neq-empty-r" (Arith.compare_op ~static_numeric:false Ongelijk d1 leeg = true);
  check "WV-11.2-neq-both-empty" (Arith.compare_op ~static_numeric:false Ongelijk leeg leeg = false);
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
       a faulting value under an unread binder does not fault, the same value
       under a read binder does, a leeg value read only in the branch the
       condition does NOT choose leaves the chosen branch standing, and the
       need is evaluated exactly ONCE however often it is read. *)
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
  let doc =
    { empty_doc with
      objecttypen = [ ot "J" [ ("a", Some num0); ("g", Some num0); ("d", Some num0) ] [] ] }
  in
  let env = env_of doc in
  (* the instance leaves "a" and "d" absent (a bare read is leeg) and supplies
     "g" = 100 — the filled dividend witness (iv)'s channel deposit needs *)
  let w = World.add_instance World.empty (inst "j" "J" [ ("g", n "100") ]) in
  let self = World.get w "j" in
  let a_leeg = aref "a" "J" num0 in
  (* (1) let x = 7 in x -> 7 *)
  expect_num "WV-10.6-binder-lookup"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" (litv "7" num0) (binder_ref "x" num0) num0))
    "7";
  (* (2a) clause 2 — a leeg value under a READ binder: the barrier fires at the
     read and the WHOLE Let is leeg (so a body read never sees leeg) *)
  expect_leeg "WV-10.6-leeg-value-gelezen-binder"
    (Eval.eval_expr env w self ~err:no_err
       (letexpr "x" a_leeg (binder_ref "x" num0) num0));
  (* (2b) clause 3 — the SAME leeg value under an UNREAD binder: nothing is
     evaluated and the body's outcome IS the Let's. Under the strict binding
     this rule stated before the L unification, this was leeg. *)
  expect_num "WV-10.6-leeg-value-ongelezen-binder"
    (Eval.eval_expr env w self ~err:no_err (letexpr "x" a_leeg (litv "42" num0) num0))
    "42";
  (* (3a) the bare maal coalesces the leeg read to 0 (ch. 1.2a) ... *)
  expect_num "WV-10.6-maal-coalesce-bare"
    (Eval.eval_expr env w self ~err:no_err (maal a_leeg (litv "5" num0)))
    "0";
  (* (3b) ... but the SAME read through a Let binding propagates leeg — the
     barrier at the READ (ch. 10.6 clause 2) *)
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
    "300";
  (* --- the four L witnesses (ref ≡ clingo differentially on each) --------- *)
  let deel_door_nul : Ir_v0.Ast.expr =
    Binair
      { op = Gedeeld_door; links = Some (litv "100" num0); rechts = Some (litv "0" num0);
        typ = Some num0; bron = None }
  in
  (* (i) clause 3 with a FAULTING value: `let q = 100 / 0 in 42` never
     evaluates the division, so it yields 42 rather than faulting *)
  expect_num "WV-10.6-ongelezen-faultende-value"
    (Eval.eval_expr env w self ~err:no_err (letexpr "q" deel_door_nul (litv "42" num0) num0))
    "42";
  (* (ii) the same value READ: the fault surfaces, fail-fast *)
  expect_fault "WV-10.6-gelezen-faultende-value" (fun () ->
      Eval.eval_expr env w self ~err:no_err
        (letexpr "q" deel_door_nul (binder_ref "q" num0) num0));
  (* (iii) a leeg value whose binder is read ONLY in the branch the condition
     does not choose: `let k = 2 in let x = a in if k > 1 then 7 else x maal 2`
     -> 7. The condition is read on EVERY branch (k), the else-branch operand on
     none — under the strict binding the whole Let was leeg here. *)
  let groter l r : Ir_v0.Ast.conditie =
    Vergelijking { op = Groter; links = Some l; rechts = Some r; periode = None; bron = None }
  in
  let probe : Ir_v0.Ast.expr =
    letexpr "k" (litv "2" num0)
      (letexpr "x" a_leeg
         (If_then_else
            { typ = Some num0;
              condition = groter (binder_ref "k" num0) (litv "1" num0);
              then_ = litv "7" num0;
              else_ = maal (binder_ref "x" num0) (litv "2" num0);
              bron = None })
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
  let deel_lege_deler_gecoalesceerd : Ir_v0.Ast.expr =
    plus
      (Binair
         { op = Gedeeld_door; links = Some (aref "g" "J" num0); rechts = Some (aref "d" "J" num0);
           typ = Some num0; bron = None })
      (litv "0" num0)
  in
  let twice, n_twice =
    with_errs (fun err ->
        Eval.eval_expr env w self ~err
          (letexpr "q" deel_lege_deler_gecoalesceerd
             (plus (binder_ref "q" num0) (binder_ref "q" num0)) num0))
  in
  let once, n_once =
    with_errs (fun err ->
        Eval.eval_expr env w self ~err
          (letexpr "q" deel_lege_deler_gecoalesceerd (binder_ref "q" num0) num0))
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

(* --- ch. 3.8: the datum-tijd millisecond layer (worked examples
   verbatim) --------------------------------------------------------------- *)

let () =
  let dt s = Value.Date_time (Case.ms_of_datetime s) in
  let plus u date lex = Arith.date_plus_duration ~unit_:u (dt date) (n lex) in
  let min_ u date lex = Arith.date_plus_duration ~minus:true ~unit_:u (dt date) (n lex) in
  let td u a b = Arith.tijdsduur ~unit_:u (dt a) (dt b) in
  let open Ir_v0.Checks in
  (* sub-day linearity, across the leap day *)
  check "WV-3.8-dt-uur" (plus Tu_uur "2024-02-28T23:30:00" "26" = dt "2024-03-01T01:30:00");
  check "WV-3.8-dt-minuut"
    (min_ Tu_minuut "2024-03-01T00:15:00" "30" = dt "2024-02-29T23:45:00");
  check "WV-3.8-dt-ms"
    (plus Tu_milliseconde "2024-01-01T00:00:00" "1500" = dt "2024-01-01T00:00:01.500");
  (* calendar units act on the civil day part, preserving the time-of-day *)
  check "WV-3.8-dt-klem" (plus Tu_maand "2024-01-31T09:30:00" "1" = dt "2024-02-29T09:30:00");
  check "WV-3.8-dt-maand" (plus Tu_maand "2024-02-28T23:30:00" "1" = dt "2024-03-28T23:30:00");
  check "WV-3.8-dt-weken" (plus Tu_week "2024-02-28T23:30:00" "2" = dt "2024-03-13T23:30:00");
  (* the fractional count truncates toward zero before the step, as at day
     precision; the pre-epoch split keeps the time-of-day non-negative *)
  check "WV-3.8-dt-frac" (plus Tu_uur "2024-02-28T23:30:00" "1,9" = dt "2024-02-29T00:30:00");
  check "WV-3.8-dt-pre-epoch"
    (plus Tu_dag "1969-12-31T23:30:00" "1" = dt "1970-01-01T23:30:00");
  (* the borrow corner: the time of day counts back before the whole-unit
     count — hours stay linear, days/months borrow *)
  expect_num "WV-3.8-dt-borrow-uren" (td Tu_uur "2024-02-28T23:30:00" "2024-03-01T10:15:00") "34";
  expect_num "WV-3.8-dt-borrow-dagen" (td Tu_dag "2024-02-28T23:30:00" "2024-03-01T10:15:00") "1";
  expect_num "WV-3.8-dt-borrow-maand"
    (td Tu_maand "2024-01-31T10:00:00" "2024-03-31T09:59:00") "1";
  expect_num "WV-3.8-dt-geen-borrow"
    (td Tu_maand "2024-01-31T10:00:00" "2024-03-31T10:00:00") "2";
  (* a negative span; the absolute variant drops the sign *)
  expect_num "WV-3.8-dt-neg-uren" (td Tu_uur "2024-03-01T10:15:00" "2024-02-28T23:30:00") "-34";
  expect_num "WV-3.8-dt-neg-dagen" (td Tu_dag "2024-03-01T10:15:00" "2024-02-28T23:30:00") "-1";
  expect_num "WV-3.8-dt-abs-uren"
    (Arith.tijdsduur ~absolute:true ~unit_:Tu_uur
       (dt "2024-03-01T10:15:00") (dt "2024-02-28T23:30:00"))
    "34";
  expect_num "WV-3.8-dt-abs-dagen"
    (Arith.tijdsduur ~absolute:true ~unit_:Tu_dag
       (dt "2024-03-01T10:15:00") (dt "2024-02-28T23:30:00"))
    "1";
  (* empty rules (ch. 1.2): leeg datetime propagates; leeg duration counts
     0; leeg tijdsduur operand propagates *)
  expect_leeg "WV-3.8-dt-leeg-datum" (Arith.date_plus_duration ~unit_:Tu_uur leeg (n "26"));
  check "WV-3.8-dt-leeg-duur"
    (Arith.date_plus_duration ~unit_:Tu_uur (dt "2024-02-28T23:30:00") leeg
    = dt "2024-02-28T23:30:00");
  expect_leeg "WV-3.8-dt-leeg-tijdsduur"
    (Arith.tijdsduur ~unit_:Tu_uur leeg (dt "2024-03-01T10:15:00"));
  (* mixed precision is ill-formed (WF-038) — fail fast behind the verifier *)
  expect_fault "WV-3.8-dt-mix" (fun () ->
      Arith.tijdsduur ~unit_:Tu_dag (Value.date_of_iso "2024-02-28") (dt "2024-03-01T10:15:00"));
  (* a step that leaves the representable ms range faults, never a silent
     wrap-around (v0's LocalDateTime.plus raises DateTimeException) *)
  expect_fault "WV-3.8-dt-bereik-uur" (fun () ->
      plus Tu_uur "2024-01-01T00:00:00" "2000000000000");
  expect_fault "WV-3.8-dt-bereik-neg" (fun () ->
      min_ Tu_uur "2024-01-01T00:00:00" "2000000000000");
  expect_fault "WV-3.8-dt-bereik-dag" (fun () ->
      plus Tu_dag "2024-01-01T00:00:00" "60000000000")

(* --- ch. 3.8 through Eval: the datum-tijd routing ------------------------
   The widened WF-035/WF-038 fences in eval.ml admit a Datum_tijd operand
   with a sub-day unit into the millisecond layer — and still refuse that
   unit on a day-precision date. The Arith checks above cannot see this
   routing (they call the layer directly), and the datum-tijd embargo
   admits no corpus golden, so these IR-shaped expressions through
   Eval.eval_expr are the only executable pin on the eval wiring
   ("Executed in ref only", RUNTIME-SEMANTICS ch. 3.8). *)
let () =
  let uur = unit_ ~canoniek:"uur" "u" in
  let num_uur : Ir_v0.Ast.datatype =
    Numeriek { geheel = true; decimalen = 0; teken = None; eenheid = Some uur }
  in
  let doc =
    {
      empty_doc with
      objecttypen =
        [ ot "Vondst"
            [ ("melding", Some Datum_tijd); ("berging", Some Datum_tijd);
              ("dagtekening", Some Datum_in_dagen) ]
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
  let plus_uren attr typ lex : Ir_v0.Ast.expr =
    Binair
      { op = Plus; links = Some (aref attr "Vondst" typ);
        rechts = Some (litv lex num_uur); typ = Some typ; bron = None }
  in
  let tijdsduur_uren a typ_a b typ_b : Ir_v0.Ast.expr =
    Functie
      { kind = Tijdsduur;
        argumenten = [ aref a "Vondst" typ_a; aref b "Vondst" typ_b ];
        element_objecttype = None; leegwaarde = None; filter = None;
        dimensie_selectie = None; typ = Some num_uur; bron = None;
        granulariteit = None; bron_rol = None }
  in
  (* the widened WF-035 fence: a datum-tijd operand rides its sub-day unit
     into the ms layer (WV-3.8-dt-uur, through the evaluator) *)
  check "WV-3.8-dt-eval-plus-uur"
    (ev (plus_uren "melding" Datum_tijd "26") = dt "2024-03-01T01:30:00");
  (* ... and still stands for a day-precision date (WF-035) *)
  expect_fault "WV-3.8-dt-eval-wf035" (fun () ->
      ev (plus_uren "dagtekening" Datum_in_dagen "26"));
  (* the widened WF-038 fence: sub-day tijdsduur over two datetimes reaches
     the linear hour count (WV-3.8-dt-borrow-uren, through the evaluator) *)
  expect_num "WV-3.8-dt-eval-tijdsduur"
    (ev (tijdsduur_uren "melding" Datum_tijd "berging" Datum_tijd))
    "34";
  (* ... and still refuses a day-precision operand (WF-038) *)
  expect_fault "WV-3.8-dt-eval-wf038" (fun () ->
      ev (tijdsduur_uren "dagtekening" Datum_in_dagen "berging" Datum_tijd))

(* --- ch. 3.4a: the self-denoting fold source (WF-083) ------------------- *)

(* WV-3.4a-zelfbron: "het aantal vondsten van de aanmelder" — the fold
   source names the fold subject by its OWN role name; navigatieRol
   carries the written role, presentation, not navigation. A Jutter
   linked to two Vondsten — one with gewicht 2,50, one whose gewicht is
   leeg — counts 2 (instances, not values: the ch. 1.2 aggregation row,
   a count never empty); a Jutter with zero links counts 0, never leeg.
   An away-pointing navigatieRol keeps the loud unsupported-shape fault. *)
let () =
  let num2 : Ir_v0.Ast.datatype =
    Numeriek { geheel = false; decimalen = 2; teken = None; eenheid = None }
  in
  let doc =
    {
      empty_doc with
      objecttypen =
        [ ot "Jutter" [ ("aantal eigen vondsten", Some num0) ] [];
          ot "Vondst" [ ("gewicht", Some num2) ] [] ];
      feittypen =
        [ ( "vondstaanmelding",
            {
              ft_naam = Some "vondstaanmelding";
              wederkerig = false;
              rollen =
                [
                  { rol_naam = Some "aanmelder"; rol_objecttype_naam = Some "Jutter";
                    rol_meervoud = None; rol_meervoudig = false };
                  { rol_naam = Some "vondst"; rol_objecttype_naam = Some "Vondst";
                    rol_meervoud = Some "vondsten"; rol_meervoudig = true };
                ];
            } ) ];
    }
  in
  let count_source nav : Ir_v0.Ast.expr =
    Attribuut_ref
      {
        attribuut = Some "vondsten";
        objecttype = Some "Jutter";
        onderwerp_bron = None;
        navigatie_rol = Some nav;
        typ = Some (Datatype_onbekend { reden = None });
        bron = None;
        set = false;
        navigatie_pad = [];
        navigatie_bindingen = None;
      }
  in
  let aantal nav : Ir_v0.Ast.expr =
    Functie
      { kind = Aantal; argumenten = [ count_source nav ];
        element_objecttype = Some "Vondst"; leegwaarde = None; filter = None;
        dimensie_selectie = None; typ = Some num0; bron = None;
        granulariteit = None; bron_rol = None }
  in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance
            (World.add_instance World.empty
               { (inst "j1" "Jutter" []) with
                 links = World.SMap.add "vondsten" [ "v1"; "v2" ] World.SMap.empty })
            (inst "j2" "Jutter" []))
         (inst "v1" "Vondst" [ ("gewicht", n "2.50") ]))
      (inst "v2" "Vondst" [])
  in
  let ev label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  expect_num "WV-3.4a-zelfbron-twee" (ev "j1" (aantal "aanmelder")) "2";
  expect_num "WV-3.4a-zelfbron-nul" (ev "j2" (aantal "aanmelder")) "0";
  expect_fault "WV-3.4a-zelfbron-weg" (fun () -> ev "j1" (aantal "vondst"))

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
      objecttypen =
        [ ot "Wachter"
            [ ("rangnummer", Some num0); ("voorliggerrang", Some num0);
              ("opvolgeraantal", Some num0) ]
            [] ];
      feittypen =
        [ ( "ketenband",
            {
              ft_naam = Some "ketenband";
              wederkerig = false;
              rollen =
                [
                  { rol_naam = Some "voorligger"; rol_objecttype_naam = Some "Wachter";
                    rol_meervoud = Some "voorliggers"; rol_meervoudig = false };
                  { rol_naam = Some "opvolger"; rol_objecttype_naam = Some "Wachter";
                    rol_meervoud = Some "opvolgers"; rol_meervoudig = true };
                ];
            } ) ];
    }
  in
  let voorliggerrang : Ir_v0.Ast.expr =
    Attribuut_ref
      {
        attribuut = Some "rangnummer";
        objecttype = Some "Wachter";
        onderwerp_bron = None;
        navigatie_rol = None;
        typ = Some num0;
        bron = None;
        set = false;
        navigatie_pad = [ Some "voorligger" ];
        navigatie_bindingen = Some [ { rb_feittype = "ketenband"; rb_rol = "voorligger" } ];
      }
  in
  let opvolgeraantal : Ir_v0.Ast.expr =
    Functie
      {
        kind = Aantal;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some "opvolgers"; objecttype = Some "Wachter";
                onderwerp_bron = None; navigatie_rol = None;
                typ = Some (Datatype_onbekend { reden = None }); bron = None; set = false;
                navigatie_pad = []; navigatie_bindingen = None } ];
        element_objecttype = Some "Wachter"; leegwaarde = None; filter = None;
        dimensie_selectie = None; typ = Some num0; bron = None; granulariteit = None;
        bron_rol = Some { rb_feittype = "ketenband"; rb_rol = "opvolger" };
      }
  in
  let env = env_of doc in
  let wachter label rang links =
    { (inst label "Wachter" [ ("rangnummer", n rang) ]) with
      World.links =
        List.fold_left (fun m (k, v) -> World.SMap.add k v m) World.SMap.empty links }
  in
  (* the loader's own pass, on the links a case supplies *)
  let geleverd supplies =
    Case.materialise_relations doc (List.fold_left World.add_instance World.empty supplies)
  in
  let beide =
    geleverd
      [ wachter "W1" "10" [ ("opvolgers", [ "W2"; "W3" ]) ];
        wachter "W2" "20" [ ("voorligger", [ "W1" ]) ];
        wachter "W3" "30" [ ("voorligger", [ "W1" ]) ] ]
  in
  let naar_veel =
    geleverd
      [ wachter "W1" "10" [ ("opvolgers", [ "W2"; "W3" ]) ];
        wachter "W2" "20" []; wachter "W3" "30" [] ]
  in
  let naar_een =
    geleverd
      [ wachter "W1" "10" [];
        wachter "W2" "20" [ ("voorligger", [ "W1" ]) ];
        wachter "W3" "30" [ ("voorligger", [ "W1" ]) ] ]
  in
  let ev w label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  List.iter
    (fun (rij, w) ->
      expect_leeg (rij ^ "-voorliggerrang-W1") (ev w "W1" voorliggerrang);
      expect_num (rij ^ "-voorliggerrang-W2") (ev w "W2" voorliggerrang) "10";
      expect_num (rij ^ "-voorliggerrang-W3") (ev w "W3" voorliggerrang) "10";
      expect_num (rij ^ "-opvolgeraantal-W1") (ev w "W1" opvolgeraantal) "2";
      expect_num (rij ^ "-opvolgeraantal-W2") (ev w "W2" opvolgeraantal) "0";
      expect_num (rij ^ "-opvolgeraantal-W3") (ev w "W3" opvolgeraantal) "0")
    [ ("WV-5.7a-beide", beide); ("WV-5.7a-naar-veel", naar_veel);
      ("WV-5.7a-naar-een", naar_een) ];
  (* the WF-013 flank: a reverse leg that collides with a supplied to-one
     link is a load error, never a silent overwrite *)
  check "WV-5.7a-botsende-terugpoot"
    (try
       ignore
         (geleverd
            [ wachter "W1" "10" [ ("opvolgers", [ "W2" ]) ];
              wachter "W2" "20" [ ("voorligger", [ "W3" ]) ];
              wachter "W3" "30" [] ]);
       false
     with Case.Error m -> Case.contains ~needle:"WF-013" m)

(* --- ch. 5.8 rule 4: the zero-hop self-reference (WF-053) --------------- *)

(* WV-5.8-4: the `jutterswet-aanmelderkrediet` worked example. Jutters
   fill the role aanmelder (vondstaanmelding) and the reciprocal role
   strandmaat (strandmaatschap). "het strandtegoed van de aanmelder
   plus 2" denotes the subject itself — zero hops, the own attribute,
   unconditionally (J3 carries no links at all and still reads 9).
   "het strandtegoed van zijn strandmaat" resolves as a far role FIRST
   (the reciprocal case — never a self-read): J1 reads J2's 25, J3
   reads leeg (rule 1, absent link). A same-typed reference naming a
   role the Jutter does NOT fill ("van de vondst") keeps the loud
   fault — the WF-053 narrowing. The navigated characteristic check is
   out of rule 4's scope: a self-denoting name there faults loudly. *)
let () =
  let doc =
    {
      empty_doc with
      objecttypen =
        [ ot "Jutter"
            [ ("strandtegoed", Some num0); ("meldkrediet", Some num0);
              ("maatdeling", Some num0) ]
            [];
          ot "Vondst" [ ("gewicht", Some num0) ] [] ];
      feittypen =
        [ ( "vondstaanmelding",
            {
              ft_naam = Some "vondstaanmelding";
              wederkerig = false;
              rollen =
                [
                  { rol_naam = Some "aanmelder"; rol_objecttype_naam = Some "Jutter";
                    rol_meervoud = None; rol_meervoudig = false };
                  { rol_naam = Some "vondst"; rol_objecttype_naam = Some "Vondst";
                    rol_meervoud = Some "vondsten"; rol_meervoudig = true };
                ];
            } );
          ( "strandmaatschap",
            {
              ft_naam = Some "strandmaatschap";
              wederkerig = true;
              rollen =
                [
                  { rol_naam = Some "strandmaat"; rol_objecttype_naam = Some "Jutter";
                    rol_meervoud = Some "strandmaten"; rol_meervoudig = false };
                ];
            } ) ];
    }
  in
  let nav_read rol : Ir_v0.Ast.expr =
    Attribuut_ref
      {
        attribuut = Some "strandtegoed";
        objecttype = Some "Jutter";
        onderwerp_bron = None;
        navigatie_rol = Some rol;
        typ = Some num0;
        bron = None;
        set = false;
        navigatie_pad = [];
        navigatie_bindingen = None;
      }
  in
  let plus2 : Ir_v0.Ast.expr =
    Binair
      { op = Plus; links = Some (nav_read "aanmelder"); rechts = Some (litv "2" num0);
        typ = Some num0; bron = None }
  in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance World.empty
            { (inst "j1" "Jutter" [ ("strandtegoed", n "10") ]) with
              links = World.SMap.add "strandmaat" [ "j2" ] World.SMap.empty })
         { (inst "j2" "Jutter" [ ("strandtegoed", n "25") ]) with
           links = World.SMap.add "strandmaat" [ "j1" ] World.SMap.empty })
      (inst "j3" "Jutter" [ ("strandtegoed", n "7") ])
  in
  let ev label e = Eval.eval_expr env w (World.get w label) ~err:no_err e in
  expect_num "WV-5.8-4-zelf-j1" (ev "j1" plus2) "12";
  expect_num "WV-5.8-4-zelf-j2" (ev "j2" plus2) "27";
  expect_num "WV-5.8-4-zelf-j3-onvoorwaardelijk" (ev "j3" plus2) "9";
  expect_num "WV-5.8-4-maat-j1" (ev "j1" (nav_read "strandmaat")) "25";
  expect_num "WV-5.8-4-maat-j2" (ev "j2" (nav_read "strandmaat")) "10";
  expect_leeg "WV-5.8-4-maat-j3-leeg" (ev "j3" (nav_read "strandmaat"));
  expect_fault "WV-5.8-4-vreemde-rol" (fun () -> ev "j1" (nav_read "vondst"));
  expect_fault "WV-5.8-4-kenmerkcheck-uitgesteld" (fun () ->
      Eval.eval_cond env w (World.get w "j1") ~err:no_err
        (Kenmerk_check
           { kenmerk = Some "betrouwbaar"; navigatie = Some "aanmelder"; negatie = false;
             eigenaar = None; periode = None; bron = None; navigatie_binding = None }))

(* --- ch. 3.4a: the type-pair fallback for the fold-source name ---------- *)

(* WV-3.4a-typenpaar: the `jutterswet-kaveltelling` worked example. The
   role duinkavel (kaveltoewijzing) declares no plural, and the written
   source "duinkavels" resolves nowhere — the unique Jutter/Duinkavel
   fact type is the source: j1 counts its three Duinkavels. The written
   singular "vat" resolves by declared role name (vatberging, declared
   plural "vaten"): j1 counts its two Vaten. A Jutter with no links
   counts 0 and 0. With a second fact type connecting Jutter to
   Duinkavel the unresolved written form keeps the loud fault — the
   fallback never guesses between two relations. *)
let () =
  let base_ft : (string * Ir_v0.Ast.feittype) list =
    [ ( "kaveltoewijzing",
        {
          ft_naam = Some "kaveltoewijzing";
          wederkerig = false;
          rollen =
            [
              { rol_naam = Some "pachter"; rol_objecttype_naam = Some "Jutter";
                rol_meervoud = None; rol_meervoudig = false };
              { rol_naam = Some "duinkavel"; rol_objecttype_naam = Some "Duinkavel";
                rol_meervoud = None; rol_meervoudig = true };
            ];
        } );
      ( "vatberging",
        {
          ft_naam = Some "vatberging";
          wederkerig = false;
          rollen =
            [
              { rol_naam = Some "berger"; rol_objecttype_naam = Some "Jutter";
                rol_meervoud = None; rol_meervoudig = false };
              { rol_naam = Some "vat"; rol_objecttype_naam = Some "Vat";
                rol_meervoud = Some "vaten"; rol_meervoudig = true };
            ];
        } ) ]
  in
  let doc =
    {
      empty_doc with
      objecttypen =
        [ ot "Jutter" [ ("aantal duinkavels", Some num0); ("aantal geborgen vaten", Some num0) ] [];
          ot "Duinkavel" [] []; ot "Vat" [] [] ];
      feittypen = base_ft;
    }
  in
  let count_source name : Ir_v0.Ast.expr =
    Attribuut_ref
      {
        attribuut = Some name; objecttype = Some "Jutter"; onderwerp_bron = None;
        navigatie_rol = None; typ = Some (Datatype_onbekend { reden = None });
        bron = None; set = false; navigatie_pad = []; navigatie_bindingen = None;
      }
  in
  let aantal name el : Ir_v0.Ast.expr =
    Functie
      { kind = Aantal; argumenten = [ count_source name ];
        element_objecttype = Some el; leegwaarde = None; filter = None;
        dimensie_selectie = None; typ = Some num0; bron = None;
        granulariteit = None; bron_rol = None }
  in
  let w =
    List.fold_left World.add_instance World.empty
      [ { (inst "j1" "Jutter" []) with
          links =
            World.SMap.add "duinkavel" [ "k1"; "k2"; "k3" ]
              (World.SMap.add "vaten" [ "t1"; "t2" ] World.SMap.empty) };
        inst "j2" "Jutter" [];
        inst "k1" "Duinkavel" []; inst "k2" "Duinkavel" []; inst "k3" "Duinkavel" [];
        inst "t1" "Vat" []; inst "t2" "Vat" [] ]
  in
  let ev doc label e = Eval.eval_expr (env_of doc) w (World.get w label) ~err:no_err e in
  expect_num "WV-3.4a-typenpaar-kavels" (ev doc "j1" (aantal "duinkavels" "Duinkavel")) "3";
  expect_num "WV-3.4a-typenpaar-vat-enkelvoud" (ev doc "j1" (aantal "vat" "Vat")) "2";
  expect_num "WV-3.4a-typenpaar-kavels-nul" (ev doc "j2" (aantal "duinkavels" "Duinkavel")) "0";
  expect_num "WV-3.4a-typenpaar-vat-nul" (ev doc "j2" (aantal "vat" "Vat")) "0";
  let doc2 =
    { doc with
      feittypen =
        base_ft
        @ [ ( "kavelpacht",
              {
                ft_naam = Some "kavelpacht";
                wederkerig = false;
                rollen =
                  [
                    { rol_naam = Some "pachtheer"; rol_objecttype_naam = Some "Jutter";
                      rol_meervoud = None; rol_meervoudig = false };
                    { rol_naam = Some "pachtkavel"; rol_objecttype_naam = Some "Duinkavel";
                      rol_meervoud = None; rol_meervoudig = true };
                  ];
              } ) ] }
  in
  expect_fault "WV-3.4a-typenpaar-ambigu" (fun () ->
      ev doc2 "j1" (aantal "duinkavels" "Duinkavel"))

(* --- ch. 5.8a: the near-side rolcheck ---------------------------------- *)

(* WV-5.8a: the `jutterswet-wachtindeling` worked example. Feittype
   wachtindeling relates the to-many role wachtjutter (Jutter) to the
   to-one role toegewezen wachtpost (Wachtpost). The subject fills
   wachtjutter iff its toegewezen-wachtpost reference is set — a null-test
   on that connecting reference. J1 is assigned post W1, J2 to none: the
   positive rolcheck holds for J1, not J2; the "is geen" negation flips it
   (unlike the navigated characteristic check of ch. 5.8 rule 3, which is
   asymmetric). A to-many connecting reference is v0's marker, refused
   loudly. *)
let () =
  let ft_wacht rechts_meervoudig : string * Ir_v0.Ast.feittype =
    ( "wachtindeling",
      {
        ft_naam = Some "wachtindeling";
        wederkerig = false;
        rollen =
          [
            { rol_naam = Some "wachtjutter"; rol_objecttype_naam = Some "Jutter";
              rol_meervoud = Some "wachtjutters"; rol_meervoudig = true };
            { rol_naam = Some "toegewezen wachtpost"; rol_objecttype_naam = Some "Wachtpost";
              rol_meervoud = (if rechts_meervoudig then Some "wachtposten" else None);
              rol_meervoudig = rechts_meervoudig };
          ];
      } )
  in
  let doc =
    { empty_doc with
      objecttypen = [ ot "Jutter" [] [ "wachtdoend"; "vrijgesteld" ]; ot "Wachtpost" [] [] ];
      feittypen = [ ft_wacht false ] }
  in
  let env = env_of doc in
  let w =
    World.add_instance
      (World.add_instance
         (World.add_instance World.empty (inst "w1" "Wachtpost" []))
         { (inst "j1" "Jutter" []) with
           links = World.SMap.add "toegewezen wachtpost" [ "w1" ] World.SMap.empty })
      (inst "j2" "Jutter" [])
  in
  let rolcheck negatie : Ir_v0.Ast.conditie =
    Rollidmaatschap
      { rol = Some "wachtjutter"; ander_te_filler = Some "Wachtpost"; negatie; bron = None }
  in
  let ec label c = Eval.eval_cond env w (World.get w label) ~err:no_err c in
  check "WV-5.8a-vervult-j1" (ec "j1" (rolcheck false));
  check "WV-5.8a-vervult-j2-niet" (not (ec "j2" (rolcheck false)));
  check "WV-5.8a-geen-j1-niet" (not (ec "j1" (rolcheck true)));
  check "WV-5.8a-geen-j2" (ec "j2" (rolcheck true));
  let doc_mm = { doc with feittypen = [ ft_wacht true ] } in
  expect_fault "WV-5.8a-naar-veel-refuseert" (fun () ->
      Eval.eval_cond (env_of doc_mm) w (World.get w "j1") ~err:no_err (rolcheck false))

(* --- ch. 5.8b: the characteristic check's owner (WF-100) ----------------
   The RUNTIME-SEMANTICS ch. 5.8b worked example, verbatim. `Vondst` and
   `Jutter` both declare a characteristic named `gekeurd`; V1 carries it and
   J1 does not, so the two readings are distinguishable by value and not only
   by name. Three owners over the same subject:

     eigenaar absent    -> reads V1's own `gekeurd`  = true
     eigenaar "Vondst"  -> the same, the field naming the subject's own type
     eigenaar "Jutter"  -> FAULT, never J1's value and never V1's

   The third line is the decision (2026-09-10): the field is authoritative, so
   an owner that is not the subject's type is not a decoration to be read past.
   Measured before the rule, on this document: the check answered `true` — V1's
   value under J1's name. The corpus cannot hold this witness because the
   document it needs is exactly the one both executors now refuse.

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
   `Wachter`): verifier PASS before AND after — the cross is correctly out of
   scope — `ref` and the ASP backend PASS (10) before and faulting after, and
   both Drools branches refusing all along (a transpiler SKIP on one branch, a
   `TODO kenmerk 'risicopost' hoort bij Post` marker on the other). So after
   this rule that form has NO answering executor at all. That is deliberate, and
   this is its machine-checked witness. *)

let () =
  let doc =
    { empty_doc with
      objecttypen = [ ot "Vondst" [] [ "gekeurd" ]; ot "Jutter" [] [ "gekeurd" ] ] }
  in
  let w =
    List.fold_left World.add_instance World.empty
      [ { (inst "v1" "Vondst" []) with chars = [ "gekeurd" ] }; inst "j1" "Jutter" [] ]
  in
  let kc eigenaar : Ir_v0.Ast.conditie =
    Kenmerk_check
      { kenmerk = Some "gekeurd"; navigatie = None; negatie = false; eigenaar; periode = None;
        bron = None; navigatie_binding = None }
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
  let kc_nav eigenaar : Ir_v0.Ast.conditie =
    Kenmerk_check
      { kenmerk = Some "gekeurd"; navigatie = Some "vinder"; negatie = false; eigenaar;
        periode = None; bron = None; navigatie_binding = None }
  in
  match ec "v1" (kc_nav (Some "Jutter")) with
  | exception Value.Fault m ->
      check ("WV-5.8b-genavigeerde-vreemde-eigenaar-weigert (got: " ^ m ^ ")")
        (Case.contains ~needle:"is hosted on \"Jutter\" while the subject is \"Vondst\"" m)
  | _ -> check "WV-5.8b-genavigeerde-vreemde-eigenaar-weigert (no fault at all)" false

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
   a heeft/2 fact a temporal characteristic can never have, answering leeg
   on a/b/c without a word. The Drools backend answered all four rows
   correctly all along, so the corpus golden `synth-gehelejaarkenmerk`
   closes the square on exactly these values.

   The fixture below carries NO `kenmerk_tijdlijn_aantal` entry, and that is
   load-bearing rather than tidy. The emitter writes that key only for a
   count OTHER than 1, so a frontend-produced document never carries it at
   1; a hand-built one that does flips both Drools branches into a refusal
   (their gate is `containsKey`, not "two or more") and makes the form look
   unsupported when it is not. That is precisely the mismeasurement this
   card first reported and then withdrew — see
   `backlog/new/20260911-kenmerktijdlijnaantal-poort.md`.

   Row a is the one that cannot be reached through the timeline fold alone —
   it is about the NAVIGATION, not about any timeline — which is why these
   run through [Eval.eval_cond] over a document and a world rather than over
   [Timeline.geheel_periode_kenmerk] directly. *)

let () =
  let ligplaats : string * Ir_v0.Ast.feittype =
    ( "ligplaats",
      {
        ft_naam = Some "ligplaats";
        wederkerig = false;
        rollen =
          [
            { rol_naam = Some "werf"; rol_objecttype_naam = Some "Werf"; rol_meervoud = None;
              rol_meervoudig = false };
            { rol_naam = Some "schuit"; rol_objecttype_naam = Some "Schuit";
              rol_meervoud = Some "schuiten"; rol_meervoudig = true };
          ];
      } )
  in
  let werf = ot "Werf" [] [ "bedrijvig" ] in
  let werf =
    ( fst werf, { (snd werf) with kenmerk_granulariteit = [ ("bedrijvig", Some Maand) ] } )
  in
  let doc =
    { empty_doc with objecttypen = [ werf; ot "Schuit" [] [] ]; feittypen = [ ligplaats ] }
  in
  let dag iso = match Value.date_of_iso iso with Value.Date d -> d | _ -> assert false in
  let periode van tot = (dag van, dag tot) in
  (* one world per row: the Werf's true-periods, and whether S1 links to it *)
  let wereld ~link perioden =
    let w1 =
      { (inst "W1" "Werf" []) with
        ctls =
          (match perioden with
          | [] -> World.SMap.empty
          | ps -> World.SMap.add "bedrijvig" ps World.SMap.empty) }
    in
    let s1 =
      { (inst "S1" "Schuit" []) with
        links = (if link then World.SMap.add "werf" [ "W1" ] World.SMap.empty else World.SMap.empty) }
    in
    List.fold_left World.add_instance World.empty [ w1; s1 ]
  in
  let cond ?(jaar = true) ?(niet = false) ?(negatie = false) () : Ir_v0.Ast.conditie =
    Kenmerk_check
      { kenmerk = Some "bedrijvig"; navigatie = Some "werf"; negatie; eigenaar = None;
        periode = Some { gp_jaar = jaar; gp_niet = niet }; bron = None; navigatie_binding = None }
  in
  let ec ~link perioden c =
    let w = wereld ~link perioden in
    Eval.eval_cond (env_of doc) w (World.get w "S1") ~err:no_err c
  in
  let heel_2026 = [ periode "2026-01-01" "2027-01-01" ] in
  let jan_jul = [ periode "2026-01-01" "2026-07-01" ] in
  let half_jan = [ periode "2026-01-15" "2026-03-01" ] in
  (* the table: a null timeline and a segmentless one both touch no bucket *)
  check "WV-3.9-kenmerk-a-geen-koppeling" (ec ~link:false [] (cond ()));
  check "WV-3.9-kenmerk-b-geen-tijdlijn" (ec ~link:true [] (cond ()));
  check "WV-3.9-kenmerk-c-heel-jaar" (ec ~link:true heel_2026 (cond ()));
  check "WV-3.9-kenmerk-d-half-jaar" (not (ec ~link:true jan_jul (cond ())));
  (* the QUALIFIER's negation complements each verdict — the clause's
     "the qualifier's negation then fails" on the two vacuous rows *)
  check "WV-3.9-kenmerk-niet-a" (not (ec ~link:false [] (cond ~niet:true ())));
  check "WV-3.9-kenmerk-niet-b" (not (ec ~link:true [] (cond ~niet:true ())));
  check "WV-3.9-kenmerk-niet-c" (not (ec ~link:true heel_2026 (cond ~niet:true ())));
  check "WV-3.9-kenmerk-niet-d" (ec ~link:true jan_jul (cond ~niet:true ()));
  (* the INNER negation: same touched buckets, complementary satisfied days *)
  check "WV-3.9-kenmerk-neg-a" (ec ~link:false [] (cond ~negatie:true ()));
  check "WV-3.9-kenmerk-neg-b" (ec ~link:true [] (cond ~negatie:true ()));
  check "WV-3.9-kenmerk-neg-c" (not (ec ~link:true heel_2026 (cond ~negatie:true ())));
  check "WV-3.9-kenmerk-neg-d" (not (ec ~link:true jan_jul (cond ~negatie:true ())));
  (* the MONTH grid: the qualifier's own, never the characteristic's
     declared granularity — six full months pass where the year bucket failed *)
  check "WV-3.9-kenmerk-maand-d" (ec ~link:true jan_jul (cond ~jaar:false ()));
  check "WV-3.9-kenmerk-maand-deelmaand"
    (not (ec ~link:true half_jan (cond ~jaar:false ())));
  check "WV-3.9-kenmerk-jaar-deelmaand" (not (ec ~link:true half_jan (cond ())));
  (* the fence WF-043 does not reach on the navigated form: a non-temporal
     characteristic would fold over an always-empty period set and pass *)
  let doc_niet_temporeel =
    { doc with objecttypen = [ ot "Werf" [] [ "bedrijvig" ]; ot "Schuit" [] [] ] }
  in
  (match
     let w = wereld ~link:true heel_2026 in
     Eval.eval_cond (env_of doc_niet_temporeel) w (World.get w "S1") ~err:no_err (cond ())
   with
  | exception Value.Fault m ->
      check ("WV-3.9-kenmerk-niet-tijdsafhankelijk (got: " ^ m ^ ")")
        (Case.contains ~needle:"is not a time-dependent characteristic of \"Werf\"" m)
  | _ -> check "WV-3.9-kenmerk-niet-tijdsafhankelijk (no fault at all)" false);
  (* the fold itself, with the chapter's own day counts *)
  check "WV-3.9-fold-leeg" (Timeline.geheel_periode_kenmerk Jaar ~negatie:false []);
  check "WV-3.9-fold-vol" (Timeline.geheel_periode_kenmerk Jaar ~negatie:false heel_2026);
  check "WV-3.9-fold-deel" (not (Timeline.geheel_periode_kenmerk Jaar ~negatie:false jan_jul))

(* --- WF-095: the datum-tijd lexeme (IR-SPEC ch. 4.1) --------------------
   The corpus golden pins what the READER answers; these pin the reader's
   own fence, which no corpus document can reach — a case file that carries
   an ill-formed lexeme cannot exist inside gate 4, and an ill-formed
   LITERAL cannot exist inside gate 3. The witness for the fence therefore
   lives here. *)

let dt_ms name lex want =
  match Value.datetime_of_lexeme lex with
  | Value.Date_time got -> check (name ^ " (got " ^ string_of_int got ^ ")") (got = want)
  | other -> check (name ^ " (got " ^ Value.to_string other ^ ")") false

let () =
  (* the hand-computed anchor: 2026-03-15 is civil day 20 527, noon is
     43 200 000 ms into it (jutterswet-meldstip's NOTE.md carries the
     derivation) *)
  let noon = (20527 * 86_400_000) + 43_200_000 in
  check "WF-095-anchor-is-1773576000000" (noon = 1_773_576_000_000);
  (* ONE instant, TWO spellings — every reader in the house admits both *)
  dt_ms "WF-095-canoniek-met-fractie" "2026-03-15T12:00:00.000" noon;
  dt_ms "WF-095-canoniek-zonder-fractie" "2026-03-15T12:00:00" noon;
  dt_ms "WF-095-snippet" "15-03-2026 12:00:00.000" noon;
  dt_ms "WF-095-snippet-zonder-fractie" "15-03-2026 12:00:00" noon;
  dt_ms "WF-095-snippet-dedato-prefix" "dd. 15-03-2026 12:00:00.000" noon;
  dt_ms "WF-095-snippet-smalle-velden" "5-3-2026 12:00:00"
    ((20517 * 86_400_000) + 43_200_000);
  (* the fraction is WHOLE MILLISECONDS, never a positional fraction *)
  dt_ms "WF-095-fractie-is-milliseconden" "2026-03-15T12:00:00.500" (noon + 500);
  dt_ms "WF-095-fractie-001" "2026-03-15T12:00:00.001" (noon + 1);
  (* and everything outside the two spellings is refused, per field class *)
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
  (* the CASE wire is narrower than the literal wire: uchar-case-v0 carries
     the canonical spelling only (its schema pattern), so the shared case
     reader takes that arm alone — the snippet is a LITERAL form *)
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
   outcome stands, so a member the outcome no longer reads raises NO fault. Both
   ch. 5.10 tables verbatim (corpus goldens jutterswet-ploegaandeel and
   jutterswet-wachtdeling): member 1 tests the divisor, member 2 divides by it. *)
let () =
  let deling : Ir_v0.Ast.expr =
    Afgerond
      { basis =
          Some
            (Binair
               { op = Gedeeld_door;
                 links = Some (aref "vangstwaarde" "Wacht" num0);
                 rechts = Some (aref "ploegomvang" "Wacht" num0);
                 typ = Some num0; bron = None });
        modus = Naar_beneden; decimalen = 0; typ = Some num0; bron = None }
  in
  let cmp op links rechts : Ir_v0.Ast.conditie =
    Vergelijking { op; links = Some links; rechts = Some rechts; periode = None; bron = None }
  in
  let lid1 op = cmp op (aref "ploegomvang" "Wacht" num0) (litv "0" num0) in
  let lid2 = cmp Groter deling (litv "0" num0) in
  let doc_of kwantor drempel op1 : Ir_v0.Ast.document =
    { empty_doc with
      objecttypen =
        [ ot "Wacht"
            [ ("ploegomvang", Some num0); ("vangstwaarde", Some num0); ("merk", Some num0) ] [] ];
      resolved_regels =
        [ rr "merk" Gelijkstelling
            (Some (aref "merk" "Wacht" num0))
            (Some (litv "1" num0))
            (Some (Samengesteld { kwantor; drempel; onderdelen = [ lid1 op1; lid2 ]; bron = None }));
        ];
    }
  in
  let run doc ploeg vangst =
    let w =
      World.add_instance World.empty
        (inst "w" "Wacht" [ ("ploegomvang", n ploeg); ("vangstwaarde", n vangst) ])
    in
    World.attr (World.get (Eval.evaluate (env_of doc) w) "w") "merk"
  in
  (* table 1 — ALLE, member 1 "ploegomvang ongelijk aan 0" *)
  let alle = doc_of Alle 0 Ongelijk in
  expect_leeg "WV-5.10-alle-deler-0-geen-fault" (run alle "0" "100");
  expect_num "WV-5.10-alle-deler-4" (run alle "4" "100") "1";
  expect_leeg "WV-5.10-alle-deler-7-lid2-beslist" (run alle "7" "3");
  (* table 2 — the SAME two members, member 1 "ploegomvang gelijk aan 0",
     under GEEN (the stop fixes the outcome false) and TEN MINSTE 1 (the same
     stop fixes it true): one trigger, opposite verdicts *)
  let geen = doc_of Geen 0 Gelijk and minste = doc_of Ten_minste 1 Gelijk in
  expect_leeg "WV-5.10-geen-deler-0-geen-fault" (run geen "0" "100");
  expect_leeg "WV-5.10-geen-deler-4" (run geen "4" "100");
  expect_num "WV-5.10-geen-deler-7" (run geen "7" "3") "1";
  expect_num "WV-5.10-tenminste-deler-0-geen-fault" (run minste "0" "100") "1";
  expect_num "WV-5.10-tenminste-deler-4" (run minste "4" "100") "1";
  expect_leeg "WV-5.10-tenminste-deler-7" (run minste "7" "3");
  (* the clause is load-bearing: WITHOUT the stop, member 2 is read on a zero
     divisor and the evaluation faults instead of answering. The one-member
     composite isolates that member: no prefix can settle the outcome, so it IS
     read, and the fault is the pre-clause behaviour of every table row above. *)
  let bloot : Ir_v0.Ast.document =
    { (doc_of Alle 0 Ongelijk) with
      resolved_regels =
        [ rr "merk" Gelijkstelling
            (Some (aref "merk" "Wacht" num0))
            (Some (litv "1" num0))
            (Some (Samengesteld { kwantor = Alle; drempel = 0; onderdelen = [ lid2 ]; bron = None }));
        ];
    }
  in
  expect_fault "WV-5.10-ongepoort-lid-fault-wel" (fun () -> run bloot "0" "100");
  (* the OUT-OF-REACH trigger stops too: TEN MINSTE 2 over two members whose
     first does not hold can no longer reach 2, so member 2 stays unread even
     though no hit has been counted *)
  let minste2 = doc_of Ten_minste 2 Ongelijk in
  expect_leeg "WV-5.10-tenminste-2-onhaalbaar-geen-fault" (run minste2 "0" "100");
  (* ---- one example per trigger row, each pinning the FAULT's absence or
     presence rather than only the verdict. Without these the trigger table is
     spec text with no artifact: an executor may drop, add or defer a row and
     every gate still passes. Each row below was measured against an arm-exact
     ref mutant that changes exactly that row. ---- *)
  let doc_leden kwantor drempel onderdelen : Ir_v0.Ast.document =
    { empty_doc with
      objecttypen =
        [ ot "Wacht"
            [ ("ploegomvang", Some num0); ("vangstwaarde", Some num0); ("merk", Some num0) ] [] ];
      resolved_regels =
        [ rr "merk" Gelijkstelling
            (Some (aref "merk" "Wacht" num0))
            (Some (litv "1" num0))
            (Some (Samengesteld { kwantor; drempel; onderdelen; bron = None }));
        ];
    }
  in
  let waar_ploeg0 = lid1 Gelijk in
  let waar_vangst = cmp Groter (aref "vangstwaarde" "Wacht" num0) (litv "0" num0) in
  let faultend = lid2 in
  (* THE STOP IS CHECKED BEFORE THE MEMBER, NOT AFTER. At TEN MINSTE with a
     threshold ABOVE the member count the out-of-reach trigger fires on the
     EMPTY prefix, so NOTHING is read — a checker that evaluates first and asks
     afterwards reads the faulting member 1 and dies. *)
  expect_leeg "WV-5.10-drempel-boven-ledental-leest-niets"
    (run (doc_leden Ten_minste 3 [ faultend; waar_ploeg0 ]) "0" "100");
  (* AT-MOST'S TRUE DIRECTION IS A TRIGGER (maintainer decision 2026-09-07;
     this replaces the earlier boundary that excluded it). With d >= n the
     outcome can no longer become false, so it STANDS on the empty prefix and
     NOTHING is read — the faulting member 1 included. Vacuously true. *)
  expect_num "WV-5.10-atmost-vacuum-waar-leest-niets"
    (run (doc_leden Ten_hoogste 2 [ faultend; waar_ploeg0 ]) "0" "100") "1";
  (* the same trigger below the member count: after two non-holding members of
     four, two remain and the threshold is 2, so hits can no longer EXCEED it
     and the outcome stands true — the faulting third member stays unread *)
  expect_num "WV-5.10-atmost-waar-stop-onder-ledental"
    (run (doc_leden Ten_hoogste 2 [ lid1 Groter; lid1 Groter; faultend; waar_ploeg0 ]) "0" "100")
    "1";
  (* AT-MOST'S FALSE DIRECTION IS a trigger: two hits past a threshold of 1
     settle the outcome, and the faulting third member is never read. *)
  expect_leeg "WV-5.10-atmost-overschrijding-stopt"
    (run (doc_leden Ten_hoogste 1 [ waar_ploeg0; waar_vangst; faultend ]) "0" "100");
  (* PRECIES' OVER-THRESHOLD TRIGGER, likewise: hits 2 against a threshold of 1
     settle it false and the faulting third member stays unread. *)
  expect_leeg "WV-5.10-precies-overschrijding-stopt"
    (run (doc_leden Precies 1 [ waar_ploeg0; waar_vangst; faultend ]) "0" "100");
  (* PRECIES' OUT-OF-REACH TRIGGER: a threshold above the member count settles
     it false on the empty prefix, so nothing is read. *)
  expect_leeg "WV-5.10-precies-onhaalbaar-leest-niets"
    (run (doc_leden Precies 3 [ faultend; waar_ploeg0 ]) "0" "100")

(* --- ch. 5.11: operand order — one pinned pair per visible sweep row ----
   Each row below is TWO worked examples over one shape: two operand
   positions, each carrying an expression that FAULTS with a message of its
   own, swapped between the pair. Which message surfaces is decided by the
   evaluation order and by nothing else — both positions are reachable under
   either order, both faults are available, and no value is ever produced. An
   executor that walks the node's operands the other way fails BOTH examples
   of the row, which is what the corpus witnesses alone did not give: the
   golden and the two red cases pin `Binair`, `Begrensd` and tijdsduur, and
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
    | exception Value.Fault m -> check (name ^ " (got: " ^ m ^ ")") (contains m frag)
    | _ -> check (name ^ " (no fault at all)") false
  in
  (* the two distinguishable faults *)
  let boom : Ir_v0.Ast.expr =
    Binair
      { op = Gedeeld_door; links = Some (litv "1" num0); rechts = Some (litv "0" num0);
        typ = Some num0; bron = None }
  in
  let pit : Ir_v0.Ast.expr = Expr_onopgelost { typ = Some num0; bron = None } in
  let bm = "division by numeric zero" and pm = "Onopgelost reached in evaluation" in
  let lijst : Ir_v0.Ast.datatype = List_type { element = num0 } in
  let paardom : Ir_v0.Ast.datatype =
    Struct_type { domein = "paar"; velden = [ ("een", num0); ("twee", num0) ] }
  in
  let doc_of fds cond waarde : Ir_v0.Ast.document =
    { empty_doc with
      objecttypen = [ ot "Vangst" [ ("teller", Some num0); ("uitkomst", Some num0) ] [] ];
      function_defs = fds;
      resolved_regels =
        [ rr "uitkomst van een vangst" Gelijkstelling
            (Some (aref "uitkomst" "Vangst" num0))
            (Some waarde) cond ];
    }
  in
  let wereld () = World.add_instance World.empty (inst "v" "Vangst" [ ("teller", n "4") ]) in
  let run_v waarde () = ignore (Eval.evaluate (env_of (doc_of [] None waarde)) (wereld ())) in
  let run_f fds waarde () = ignore (Eval.evaluate (env_of (doc_of fds None waarde)) (wereld ())) in
  let run_c cond () =
    ignore (Eval.evaluate (env_of (doc_of [] (Some cond) (litv "1" num0))) (wereld ()))
  in
  (* row: Vergelijking — links before rechts *)
  let cmp op l r : Ir_v0.Ast.conditie =
    Vergelijking { op; links = Some l; rechts = Some r; periode = None; bron = None }
  in
  expect_msg "WV-5.11-vergelijking-links-eerst" (run_c (cmp Gelijk boom pit)) bm;
  expect_msg "WV-5.11-vergelijking-rechts-later" (run_c (cmp Gelijk pit boom)) pm;
  (* row: aggregation — the contributors in wire order, and BEFORE the
     leegwaarde. The stand-in is the LAST operand of the node, not the first. *)
  let som args leeg : Ir_v0.Ast.expr =
    Functie
      { kind = Som; argumenten = args; element_objecttype = None; leegwaarde = leeg;
        filter = None; dimensie_selectie = None; typ = Some num0; bron = None;
        granulariteit = None; bron_rol = None }
  in
  expect_msg "WV-5.11-som-bijdrager0-eerst" (run_v (som [ boom; pit ] None)) bm;
  expect_msg "WV-5.11-som-bijdrager1-later" (run_v (som [ pit; boom ] None)) pm;
  expect_msg "WV-5.11-som-bijdragers-voor-leegwaarde" (run_v (som [ boom ] (Some pit))) bm;
  expect_msg "WV-5.11-som-leegwaarde-na-bijdragers" (run_v (som [ pit ] (Some boom))) pm;
  (* and the leegwaarde is evaluated UNCONDITIONALLY: a filled contributor
     leaves the stand-in unused, never unevaluated. Its faults and its §9.5
     deposits happen whether or not its value is read (ch. 5.11). *)
  expect_msg "WV-5.11-som-leegwaarde-onvoorwaardelijk"
    (run_v (som [ litv "5" num0 ] (Some boom))) bm;
  (* row: concatenatie and TekstReeks — part order *)
  let concat args : Ir_v0.Ast.expr =
    Functie
      { kind = Concatenatie; argumenten = args; element_objecttype = None; leegwaarde = None;
        filter = None; dimensie_selectie = None; typ = Some Tekst; bron = None;
        granulariteit = None; bron_rol = None }
  in
  expect_msg "WV-5.11-concat-deel0-eerst" (run_v (concat [ boom; pit ])) bm;
  expect_msg "WV-5.11-concat-deel1-later" (run_v (concat [ pit; boom ])) pm;
  let reeks a b : Ir_v0.Ast.expr =
    Tekst_reeks
      { delen = [ Ingevoegd { expr = Some a }; Ingevoegd { expr = Some b } ];
        typ = Some Tekst; bron = None }
  in
  expect_msg "WV-5.11-tekstreeks-deel0-eerst" (run_v (reeks boom pit)) bm;
  expect_msg "WV-5.11-tekstreeks-deel1-later" (run_v (reeks pit boom)) pm;
  (* row: ListLiteral — element order *)
  let lijstlit a b : Ir_v0.Ast.expr =
    List_literal { typ = Some lijst; elements = [ a; b ]; bron = None }
  in
  expect_msg "WV-5.11-listliteral-element0-eerst" (run_v (lijstlit boom pit)) bm;
  expect_msg "WV-5.11-listliteral-element1-later" (run_v (lijstlit pit boom)) pm;
  (* row: StructLiteral — declared field order (WF-073) *)
  let structlit a b : Ir_v0.Ast.expr =
    Struct_literal { typ = Some paardom; velden = [ ("een", a); ("twee", b) ]; bron = None }
  in
  expect_msg "WV-5.11-structliteral-veld0-eerst" (run_v (structlit boom pit)) bm;
  expect_msg "WV-5.11-structliteral-veld1-later" (run_v (structlit pit boom)) pm;
  (* row: Invoke — argument order (the body runs only after all of them) *)
  let fds =
    [ ({ fd_naam = "kies"; fd_parameters = [ ("een", num0); ("twee", num0) ]; fd_result = num0;
         fd_body = litv "1" num0; fd_bron = None } : Ir_v0.Ast.function_def) ]
  in
  let roep a b : Ir_v0.Ast.expr =
    Invoke { typ = Some num0; functie = "kies"; arguments = [ a; b ]; bron = None }
  in
  expect_msg "WV-5.11-invoke-arg0-eerst" (run_f fds (roep boom pit)) bm;
  expect_msg "WV-5.11-invoke-arg1-later" (run_f fds (roep pit boom)) pm;
  (* row: Fold — the `list` operand before the `init` operand *)
  let vouw lst ini : Ir_v0.Ast.expr =
    Fold
      { typ = Some num0; list = lst; init = ini;
        functie =
          Fn_lambda
            { parameters = [ ("acc", num0); ("x", num0) ];
              body = Binder_ref { naam = "acc"; typ = Some num0; bron = None }; bron = None };
        bron = None }
  in
  expect_msg "WV-5.11-fold-list-voor-init" (run_v (vouw boom pit)) bm;
  expect_msg "WV-5.11-fold-init-na-list" (run_v (vouw pit boom)) pm;
  (* row: MapList and FilterList — the elements in list order. A fold body is
     CLOSED (WF-069), so no binder from an enclosing Let is in scope and the
     barrier device is unavailable; the body branches on the element instead,
     so the two elements fault differently and the surviving message names the
     element that ran first. *)
  let tak : Ir_v0.Ast.expr =
    If_then_else
      { typ = Some num0;
        condition =
          cmp Groter (Binder_ref { naam = "x"; typ = Some num0; bron = None }) (litv "0" num0);
        then_ = boom; else_ = pit; bron = None }
  in
  let paar a b : Ir_v0.Ast.expr =
    List_literal { typ = Some lijst; elements = [ litv a num0; litv b num0 ]; bron = None }
  in
  let kaart l : Ir_v0.Ast.expr =
    Map_list
      { typ = Some lijst; list = l;
        functie = Fn_lambda { parameters = [ ("x", num0) ]; body = tak; bron = None };
        bron = None }
  in
  expect_msg "WV-5.11-maplist-element0-eerst" (run_v (kaart (paar "1" "0"))) bm;
  expect_msg "WV-5.11-maplist-element1-later" (run_v (kaart (paar "0" "1"))) pm;
  let zeef l : Ir_v0.Ast.expr =
    Filter_list
      { typ = Some lijst; list = l; binder = "x";
        condition = cmp Groter tak (litv "0" num0); bron = None }
  in
  expect_msg "WV-5.11-filterlist-element0-eerst" (run_v (zeef (paar "1" "0"))) bm;
  expect_msg "WV-5.11-filterlist-element1-later" (run_v (zeef (paar "0" "1"))) pm;
  (* row: the aggregation's INSTANCE fold — the filter per member, members in
     fold order. The listed-`argumenten` branch above cannot reach this: an
     instance fold takes its contributors from a role, and its `filter` runs
     once per MEMBER with that member as the condition's subject. So the
     branching trick moves into the filter and the members carry the switch —
     `merk` = 1 selects the division, `merk` = 0 the marker, and which member
     runs first decides which fault survives. Both filtered walks are pinned:
     the `Instances` one an `aantal` takes, and the `Values` one a `som` over
     a set-read takes. Measured: without this pair, reversing BOTH member
     walks passes every gate and every other example here. *)
  let ftn : (string * Ir_v0.Ast.feittype) list =
    [ ( "deelname",
        { ft_naam = Some "deelname"; wederkerig = false;
          rollen =
            [ { rol_naam = Some "vangst"; rol_objecttype_naam = Some "Vangst";
                rol_meervoud = None; rol_meervoudig = false };
              { rol_naam = Some "deelvangst"; rol_objecttype_naam = Some "Deelvangst";
                rol_meervoud = Some "deelvangsten"; rol_meervoudig = true } ]; } ) ]
  in
  let leden_doc : Ir_v0.Ast.document =
    { empty_doc with
      objecttypen =
        [ ot "Vangst" [ ("teller", Some num0); ("uitkomst", Some num0) ] [];
          ot "Deelvangst" [ ("merk", Some num0); ("bedrag", Some num0) ] [] ];
      feittypen = ftn }
  in
  (* the filter faults one way on a member with merk > 0 and the other way on
     a member with merk = 0 *)
  let lid_filter : Ir_v0.Ast.conditie =
    cmp Groter
      (If_then_else
         { typ = Some num0;
           condition = cmp Groter (aref "merk" "Deelvangst" num0) (litv "0" num0);
           then_ = boom; else_ = pit; bron = None })
      (litv "0" num0)
  in
  let leden_wereld m1 m2 =
    List.fold_left World.add_instance World.empty
      [ { (inst "v" "Vangst" []) with
          links = World.SMap.add "deelvangsten" [ "d1"; "d2" ] World.SMap.empty };
        inst "d1" "Deelvangst" [ ("merk", n m1); ("bedrag", n "1") ];
        inst "d2" "Deelvangst" [ ("merk", n m2); ("bedrag", n "1") ] ]
  in
  let run_leden e m1 m2 () =
    let w = leden_wereld m1 m2 in
    ignore (Eval.eval_expr (env_of leden_doc) w (World.get w "v") ~err:no_err e)
  in
  (* the `Instances` walk: an aantal over the bare role *)
  let telling : Ir_v0.Ast.expr =
    Functie
      { kind = Aantal;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some "deelvangsten"; objecttype = Some "Vangst";
                onderwerp_bron = None; navigatie_rol = None;
                typ = Some (Datatype_onbekend { reden = None }); bron = None; set = false;
                navigatie_pad = []; navigatie_bindingen = None } ];
        element_objecttype = Some "Deelvangst"; leegwaarde = None; filter = Some lid_filter;
        dimensie_selectie = None; typ = Some num0; bron = None; granulariteit = None;
        bron_rol = None }
  in
  expect_msg "WV-5.11-instantiefold-lid0-eerst" (run_leden telling "1" "0") bm;
  expect_msg "WV-5.11-instantiefold-lid1-later" (run_leden telling "0" "1") pm;
  (* the `Values` walk: a som over the members' set-read attribute *)
  let ledensom : Ir_v0.Ast.expr =
    Functie
      { kind = Som;
        argumenten =
          [ Attribuut_ref
              { attribuut = Some "bedrag"; objecttype = Some "Deelvangst"; onderwerp_bron = None;
                navigatie_rol = None; typ = Some num0; bron = None; set = true;
                navigatie_pad = []; navigatie_bindingen = None } ];
        element_objecttype = Some "Deelvangst"; leegwaarde = None; filter = Some lid_filter;
        dimensie_selectie = None; typ = Some num0; bron = None; granulariteit = None;
        bron_rol = None }
  in
  expect_msg "WV-5.11-instantiesom-lid0-eerst" (run_leden ledensom "1" "0") bm;
  expect_msg "WV-5.11-instantiesom-lid1-later" (run_leden ledensom "0" "1") pm

let () =
  if !failures > 0 then begin
    Printf.printf "%d failure(s)\n" !failures;
    exit 1
  end
  else Printf.printf "test_ref: all %d worked examples pass\n" !total
