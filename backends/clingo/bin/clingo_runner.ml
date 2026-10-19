(* Gate-6 runner: translate one (ir.json, case.json) pair to ASP, run
   clingo, and check the stable model against the case's expectations —
   the same absolute expectations gate 5 pins for ref/. On an
   expectedFault case (corpus/red) the pair must instead fail
   observably — a case-reader refusal at LOAD, a translation fault, or
   UNSAT — with the pinned fragment in the report; an UNSAT is
   diagnosed by re-running without the global integrity constraint so
   the report names the viol/2 guard that fired, and the fragment can
   pin the REASON, not just the failure.
   Exit 0: every expectation matches, or the expected fault is
   observed. Exit 1: mismatch, translation/evaluation fault, an
   expected fault that does not occur, or a fault report without the
   fragment. Exit 2: an unpinned load error, or clingo unavailable. *)

(* the solver invocation itself lives in the library (Clingo_backend.Solve),
   so this runner and the backend's own suite call clingo the SAME way —
   one source of truth, or the suite could pin what the gate never runs *)
let run_clingo = Clingo_backend.Solve.run_clingo

(* the viol/2 witnesses of an UNSAT program, diagnosed in the library so the
   backend's suite reads the very witnesses this report prints *)
let diagnostic = Clingo_backend.Solve.diagnostic

(* run clingo on [program]; [`Sat json] with the outf2 payload, or
   [`Unsat (msg, codes)] with the diagnosed report and the fault kinds of its
   witnesses with their parameters, in report order, each pair once
   ([Translate.viol_code]) *)
let solve program =
  match run_clingo program with
  | out, Unix.WEXITED (10 | 30) -> `Sat out (* clingo: SAT / SAT+shown *)
  | _, Unix.WEXITED 127 ->
      prerr_endline "CLINGO NOT FOUND on PATH";
      exit 2
  | _, Unix.WEXITED 20 ->
      let witnesses = diagnostic program in
      let reason =
        match witnesses with
        | [] -> "no viol/2 witness (the input is not stratified?)"
        | vs -> String.concat "; " vs
      in
      let codes =
        List.fold_left
          (fun acc w ->
            match Clingo_backend.Translate.viol_code w with
            | Some c when not (List.mem c acc) -> acc @ [ c ]
            | _ -> acc)
          [] witnesses
      in
      `Unsat (Printf.sprintf "clingo: UNSATISFIABLE — a fail-fast guard fired: %s" reason, codes)
  | out, Unix.WEXITED c ->
      Printf.eprintf "clingo exited %d:\n%s\n" c out;
      exit 1
  | _, _ ->
      prerr_endline "clingo killed";
      exit 2

(* the report with its codes at the head, "EF-005: clingo: UNSATISFIABLE — …" *)
let with_codes codes msg =
  let names = List.fold_left (fun acc (c, _) -> if List.mem c acc then acc else acc @ [ c ]) [] codes in
  match names with [] -> msg | _ -> String.concat ", " names ^ ": " ^ msg

(* The machine lines of the red path, as ref's runner prints them: one CODE line
   per code this executor observed with its parameters (a JSON object of
   strings), in report order, and the case's own expectedFault code and params as
   the shared case reader decoded them; "-" stands for no code. *)
let print_codes ~observed ~(pinned : Uchar_ref.Case.expected_fault) =
  (match observed with
  | [] -> print_endline "CODE -"
  | cs ->
      List.iter (fun (c, ps) -> Printf.printf "CODE %s %s\n" c (Uchar_ref.Case.params_json ps)) cs);
  Printf.printf "CODE-PINNED %s %s\n" pinned.ef_code (Uchar_ref.Case.params_json pinned.ef_params)

(* the verdict on an observed fault: some observed code is the pinned one, with
   the pinned parameters among its own; else the whole report is shown *)
let verdict ~(pinned : Uchar_ref.Case.expected_fault) ~observed ~what report =
  print_codes ~observed ~pinned;
  if List.exists (fun (code, params) -> Uchar_ref.Case.pin_matches pinned ~code:(Some code) ~params) observed
  then Printf.printf "clingo: expected fault observed%s (%s)\n" what pinned.ef_code
  else (
    Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %s %s in:\n%s\n" pinned.ef_code
      (Uchar_ref.Case.params_json pinned.ef_params) report;
    exit 1)

let () =
  match Sys.argv with
  | [| _; ir_path; case_path |] -> (
      try
        (* Split reading, exactly as ref/bin/runner.ml does it: the
           pinned fragment sits in the same file whose INPUT may be
           unreadable, so the contract field is read on its own, before
           the world. Both executors must agree on a load refusal the
           same way they agree on a fault, or the red zone cannot hold
           the class at all. *)
        let pinned = Uchar_ref.Case.expected_fault_of_file case_path in
        let doc = Ir_v0.Loader.load_file ir_path in
        let case =
          try Uchar_ref.Case.load_file doc case_path
          with Uchar_ref.Case.Error { code; params; text = msg } -> (
            match pinned with
            | None ->
                Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code msg);
                exit 2
            | Some pinned ->
                verdict ~pinned
                  ~observed:(match code with Some c -> [ (c, params) ] | None -> [])
                  ~what:" at load"
                  ("CASE ERROR: " ^ Uchar_ref.Value.report ~code msg);
                exit 0)
        in
        match case.expected_fault with
        | Some pinned -> (
            (* the red path: translation or solving MUST fail with the
               pinned code — never a silent model *)
            let observed =
              match
                let program = Clingo_backend.Translate.translate doc case in
                solve program
              with
              | `Sat _ -> None
              | `Unsat (msg, codes) -> Some (with_codes codes msg, codes)
              | exception Uchar_ref.Value.Fault { code; params; text } ->
                  Some
                    ( "TRANSLATION FAULT: " ^ Uchar_ref.Value.report ~code text,
                      match code with Some c -> [ (c, params) ] | None -> [] )
            in
            match observed with
            | None ->
                prerr_endline "EXPECTED FAULT DID NOT OCCUR: clingo found a model";
                exit 1
            | Some (msg, codes) -> verdict ~pinned ~observed:codes ~what:"" msg)
        | None -> (
            let program = Clingo_backend.Translate.translate doc case in
            let out =
              match solve program with
              | `Sat out -> out
              | `Unsat (msg, codes) ->
                  prerr_endline (with_codes codes msg);
                  exit 1
            in
            let model = Clingo_backend.Model.model_of_outf2 (Yojson.Safe.from_string out) in
            match Clingo_backend.Model.check doc case model with
            | [] ->
                Printf.printf "clingo: PASS (%d expectations)\n" (List.length case.expected)
            | mismatches ->
                List.iter (fun m -> Printf.printf "MISMATCH: %s\n" m) mismatches;
                exit 1)
      with
      | Uchar_ref.Value.Fault { code; text; _ } ->
          Printf.eprintf "TRANSLATION FAULT: %s\n" (Uchar_ref.Value.report ~code text);
          exit 1
      | Uchar_ref.Case.Error { code; text; _ } ->
          Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code text);
          exit 2
      | Ir_v0.Loader.Error (path, msg) ->
          Printf.eprintf "IR LOAD ERROR at %s: %s\n" (if path = "" then "<root>" else path) msg;
          exit 2
      | Sys_error e | Yojson.Json_error e ->
          Printf.eprintf "LOAD ERROR: %s\n" e;
          exit 2)
  | _ ->
      prerr_endline "usage: clingo_runner <ir.json> <case.json>";
      exit 2
