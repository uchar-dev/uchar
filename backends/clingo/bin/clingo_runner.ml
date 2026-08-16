(* Gate-6 runner: translate one (ir.json, case.json) pair to ASP, run
   clingo, and check the stable model against the case's expectations —
   the same absolute expectations gate 5 pins for ref/. Exit 0: every
   expectation matches. Exit 1: mismatch or translation/evaluation
   fault. Exit 2: load error or clingo unavailable. *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do
       Buffer.add_channel b ic 1
     done
   with End_of_file -> ());
  Buffer.contents b

let () =
  match Sys.argv with
  | [| _; ir_path; case_path |] -> (
      try
        let doc = Ir_v0.Loader.load_file ir_path in
        let case = Uchar_ref.Case.load_file doc case_path in
        let program = Clingo_backend.Translate.translate doc case in
        let tmp = Filename.temp_file "uchar-clingo" ".lp" in
        let oc = open_out tmp in
        output_string oc program;
        close_out oc;
        let ic = Unix.open_process_in (Printf.sprintf "clingo --outf=2 %s" (Filename.quote tmp)) in
        let out = read_all ic in
        let status = Unix.close_process_in ic in
        Sys.remove tmp;
        (match status with
        | Unix.WEXITED (10 | 30) -> () (* clingo: SAT / SAT+shown *)
        | Unix.WEXITED 127 ->
            prerr_endline "CLINGO NOT FOUND on PATH";
            exit 2
        | Unix.WEXITED c ->
            Printf.eprintf "clingo exited %d:\n%s\n" c out;
            exit 1
        | _ ->
            prerr_endline "clingo killed";
            exit 2);
        let model = Clingo_backend.Model.model_of_outf2 (Yojson.Safe.from_string out) in
        match Clingo_backend.Model.check doc case model with
        | [] ->
            Printf.printf "clingo: PASS (%d expectations)\n" (List.length case.expected)
        | mismatches ->
            List.iter (fun m -> Printf.printf "MISMATCH: %s\n" m) mismatches;
            exit 1
      with
      | Uchar_ref.Value.Fault msg ->
          Printf.eprintf "TRANSLATION FAULT: %s\n" msg;
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
      prerr_endline "usage: clingo_runner <ir.json> <case.json>";
      exit 2
