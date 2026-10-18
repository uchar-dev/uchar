(* Gate-6 runner: translate one (ir.json, case.json) pair to ASP, run
   clingo, and check the stable model against the case's expectations —
   the same absolute expectations gate 5 pins for ref/. On an
   expectedFault case (corpus/rood) the pair must instead fail
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
   [`Unsat (msg, codes)] with the diagnosed report the expectedFault fragment
   matches and the fault kinds of its witnesses, in report order, each once
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
let with_codes codes msg = match codes with [] -> msg | cs -> String.concat ", " cs ^ ": " ^ msg

(* The two machine lines of the red path, as ref's runner prints them: the codes
   this executor observed, comma-separated, and the case's own expectedFault.code
   as the shared case reader decoded it; "-" stands for none. *)
let print_codes ~observed ~pinned =
  Printf.printf "CODE %s\nCODE-PINNED %s\n"
    (match observed with [] -> "-" | cs -> String.concat "," cs)
    (Option.value pinned ~default:"-")

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
          with Uchar_ref.Case.Error { code; text = msg } -> (
            match pinned with
            | None ->
                Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code msg);
                exit 2
            | Some { ef_clingo; ef_code; _ } ->
                let report = "CASE ERROR: " ^ Uchar_ref.Value.report ~code msg in
                print_codes ~observed:(Option.to_list code) ~pinned:ef_code;
                if Uchar_ref.Case.contains ~needle:ef_clingo report then (
                  Printf.printf "clingo: expected fault observed at load (%s)\n" ef_clingo;
                  exit 0)
                else (
                  Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %S in:\n%s\n" ef_clingo report;
                  exit 1))
        in
        match case.expected_fault with
        | Some { ef_clingo; ef_code; _ } -> (
            (* the red path: translation or solving MUST fail with the
               pinned fragment — never a silent model *)
            let observed =
              match
                let program = Clingo_backend.Translate.translate doc case in
                solve program
              with
              | `Sat _ -> None
              | `Unsat (msg, codes) -> Some (with_codes codes msg, codes)
              | exception Uchar_ref.Value.Fault { code; text } ->
                  Some ("TRANSLATION FAULT: " ^ Uchar_ref.Value.report ~code text, Option.to_list code)
            in
            match observed with
            | None ->
                prerr_endline "EXPECTED FAULT DID NOT OCCUR: clingo found a model";
                exit 1
            | Some (msg, codes) ->
                print_codes ~observed:codes ~pinned:ef_code;
                if Uchar_ref.Case.contains ~needle:ef_clingo msg then
                  Printf.printf "clingo: expected fault observed (%s)\n" ef_clingo
                else (
                  Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %S in:\n%s\n" ef_clingo msg;
                  exit 1))
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
      | Uchar_ref.Value.Fault { code; text } ->
          Printf.eprintf "TRANSLATION FAULT: %s\n" (Uchar_ref.Value.report ~code text);
          exit 1
      | Uchar_ref.Case.Error { code; text } ->
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
