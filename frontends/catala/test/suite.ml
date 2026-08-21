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
      ("geld.catala_en", [ "cases/geld_1.json"; "cases/geld_2.json" ]) ]
  in
  List.iter (fun (src, cases) -> run_passing ~ref_only:false src cases) passing;
  (* ref-only passing programs: clingo cannot translate a date literal in a
     term position (dates are civil day numbers from case input, not IR
     literals) — a recorded backend limit, not a plugin bug. *)
  let passing_ref_only =
    [ ("datum.catala_en", [ "cases/datum_najaar.json"; "cases/datum_voorjaar.json" ]) ]
  in
  List.iter (fun (src, cases) -> run_passing ~ref_only:true src cases) passing_ref_only;
  (* refusal programs: source -> expected reason fragment *)
  let refusals =
    [ ("refuse_context.catala_en", "context");
      ("refuse_subscope.catala_en", "subscope");
      ("refuse_subroot.catala_en", "sub-root") ]
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
