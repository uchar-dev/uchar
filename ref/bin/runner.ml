(* The oracle runner: replay one uchar-case-v0 file against its ir.json.
   Exit 0: every expectation matches exactly. Exit 1: a mismatch or an
   evaluation fault. Exit 2: a load or cross-check error. *)

let () =
  match Sys.argv with
  | [| _; ir_path; case_path |] -> (
      try
        let doc = Ir_v0.Loader.load_file ir_path in
        let case = Uchar_ref.Case.load_file doc case_path in
        let env =
          {
            Uchar_ref.Eval.doc;
            params = case.params;
            as_of = case.as_of;
          }
        in
        let final = Uchar_ref.Eval.evaluate env case.world in
        match Uchar_ref.Case.check case final with
        | [] ->
            Printf.printf "oracle: PASS (%d expectations)\n" (List.length case.expected)
        | mismatches ->
            List.iter (fun m -> Printf.printf "MISMATCH: %s\n" m) mismatches;
            exit 1
      with
      | Uchar_ref.Value.Fault msg ->
          Printf.eprintf "EVALUATION FAULT: %s\n" msg;
          exit 1
      | Uchar_ref.Case.Error msg ->
          Printf.eprintf "CASE ERROR: %s\n" msg;
          exit 2
      | Ir_v0.Loader.Error (path, msg) ->
          Printf.eprintf "IR LOAD ERROR at %s: %s\n" (if path = "" then "<root>" else path) msg;
          exit 2
      | Sys_error e | Yojson.Json_error e ->
          Printf.eprintf "LOAD ERROR: %s\n" e;
          exit 2)
  | _ ->
      prerr_endline "usage: runner <ir.json> <case.json>";
      exit 2
