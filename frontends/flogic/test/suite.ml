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

let mandate = "OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md"

let () =
  let frontend, verifier, refx, clingo =
    match Sys.argv with
    | [| _; a; b; c; d |] -> (a, b, c, d)
    | _ ->
        prerr_endline "usage: suite <frontend> <verifier> <ref> <clingo>";
        exit 2
  in
  let passing =
    [ ("overerving.flogic",
       [ "cases/overerving_heel.json"; "cases/overerving_gewond.json" ]);
      ("aggregatie.flogic",
       [ "cases/aggregatie_twee.json"; "cases/aggregatie_leeg.json" ]) ]
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
      ("refuse_tweede_ouder.flogic", "WF-106");
      ("refuse_tweede_ouder.flogic", "RechtOpUitkeringWazo and RechtUitkeringZw,");
      (* the needle carries the pair AND its parents: a bare
         "RechtUitkeringZw" is a prefix of "RechtUitkeringZwWazo" and would
         pass on the parent name alone *)
      ("refuse_tweede_ouder.flogic", "RechtOpUitkering and RechtUitkeringZwWazo");
      ("refuse_ongebalkt.flogic", "unbarred class-level frame");
      ("refuse_hilog.flogic", "HiLog and meta-programming");
      ("refuse_defeasible.flogic", "\\overrides");
      ("refuse_naf.flogic", "\\naf");
      ("refuse_neg.flogic", "\\neg");
      ("refuse_monotoon.flogic", "setsemantics");
      ("refuse_instantie.flogic", "uchar-case-v0");
      ("refuse_predicaat.flogic", "no predicate channel");
      ("refuse_floraeen.flogic", "*->");
      ("refuse_decimal.flogic", "\\decimal");
      ("refuse_tweede_basis.flogic", "two unconditional definitions");
      ("refuse_aggregaat.flogic", "sum and count only");
      ("refuse_rolsignatuur.flogic", "declare a fact type");
      (* a rule reaches the wire by name: WF-020 filters nulls out and never
         looks at a defaults group's name, so both of these have to refuse
         at the frontend or not at all *)
      ("refuse_naamloos.flogic", "a rule with no id");
      ("refuse_naamloze_basis.flogic", "has no id");
      ("refuse_id_zonder_definitie.flogic", "which is a subclass fact");
      ("refuse_dubbele_naam.flogic", "is already used at line");
      ("refuse_argumentatietheorie.flogic", "argumentation-theory");
      ("refuse_dubbel_lid.flogic", "declares the member hoogte twice");
      ("refuse_onbekende_filler.flogic", "not a declared object type") ]
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
        (not (contains ~needle:"formaat" (try
                let ic = open_in out in
                let s = read_all ic in
                close_in ic; s
              with Sys_error _ -> "")))
        "the frontend wrote an IR document for a refused source")
    refusals;
  Printf.printf "\n%d checks, %d failures\n%!" !ntests !failures;
  if !failures > 0 then exit 1
