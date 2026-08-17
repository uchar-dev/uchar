(* Gate-6 runner: translate one (ir.json, case.json) pair to ASP, run
   clingo, and check the stable model against the case's expectations —
   the same absolute expectations gate 5 pins for ref/. On an
   expectedFault case (corpus/rood) the pair must instead fail
   observably — a translation fault or UNSAT — with the pinned fragment
   in the report. Exit 0: every expectation matches, or the expected
   fault is observed. Exit 1: mismatch, translation/evaluation fault,
   an expected fault that does not occur, or a fault report without the
   fragment. Exit 2: load error or clingo unavailable. *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do
       Buffer.add_channel b ic 1
     done
   with End_of_file -> ());
  Buffer.contents b

(* run clingo on [program]; [`Sat json] with the outf2 payload, or
   [`Unsat msg] with the report the expectedFault fragment matches *)
let solve program =
  let tmp = Filename.temp_file "uchar-clingo" ".lp" in
  let oc = open_out tmp in
  output_string oc program;
  close_out oc;
  let ic = Unix.open_process_in (Printf.sprintf "clingo --outf=2 %s" (Filename.quote tmp)) in
  let out = read_all ic in
  let status = Unix.close_process_in ic in
  Sys.remove tmp;
  match status with
  | Unix.WEXITED (10 | 30) -> `Sat out (* clingo: SAT / SAT+shown *)
  | Unix.WEXITED 127 ->
      prerr_endline "CLINGO NOT FOUND on PATH";
      exit 2
  | Unix.WEXITED 20 ->
      `Unsat
        "clingo: UNSATISFIABLE — a generated fail-fast constraint fired (negative or empty \
         allocation operand, ch. 4.4; a to-one second filler, ch. 5.5) or the input is not \
         stratified"
  | Unix.WEXITED c ->
      Printf.eprintf "clingo exited %d:\n%s\n" c out;
      exit 1
  | _ ->
      prerr_endline "clingo killed";
      exit 2

let () =
  match Sys.argv with
  | [| _; ir_path; case_path |] -> (
      try
        let doc = Ir_v0.Loader.load_file ir_path in
        let case = Uchar_ref.Case.load_file doc case_path in
        match case.expected_fault with
        | Some { ef_clingo; _ } -> (
            (* the red path: translation or solving MUST fail with the
               pinned fragment — never a silent model *)
            let observed =
              match
                let program = Clingo_backend.Translate.translate doc case in
                solve program
              with
              | `Sat _ -> None
              | `Unsat msg -> Some msg
              | exception Uchar_ref.Value.Fault msg -> Some ("TRANSLATION FAULT: " ^ msg)
            in
            match observed with
            | None ->
                prerr_endline "EXPECTED FAULT DID NOT OCCUR: clingo found a model";
                exit 1
            | Some msg when Uchar_ref.Case.contains ~needle:ef_clingo msg ->
                Printf.printf "clingo: expected fault observed (%s)\n" ef_clingo
            | Some msg ->
                Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %S in:\n%s\n" ef_clingo msg;
                exit 1)
        | None -> (
            let program = Clingo_backend.Translate.translate doc case in
            let out =
              match solve program with
              | `Sat out -> out
              | `Unsat msg ->
                  prerr_endline msg;
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
