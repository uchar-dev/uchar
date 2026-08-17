(* Gate-6 runner: translate one (ir.json, case.json) pair to ASP, run
   clingo, and check the stable model against the case's expectations —
   the same absolute expectations gate 5 pins for ref/. On an
   expectedFault case (corpus/rood) the pair must instead fail
   observably — a translation fault or UNSAT — with the pinned fragment
   in the report; an UNSAT is diagnosed by re-running without the global
   integrity constraint so the report names the viol/2 guard that
   fired, and the fragment can pin the REASON, not just the failure.
   Exit 0: every expectation matches, or the expected fault is
   observed. Exit 1: mismatch, translation/evaluation fault, an
   expected fault that does not occur, or a fault report without the
   fragment. Exit 2: load error or clingo unavailable. *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do
       Buffer.add_channel b ic 1
     done
   with End_of_file -> ());
  Buffer.contents b

(* one clingo invocation on [text]; the raw outf2 payload and exit *)
let run_clingo text =
  let tmp = Filename.temp_file "uchar-clingo" ".lp" in
  let oc = open_out tmp in
  output_string oc text;
  close_out oc;
  let ic = Unix.open_process_in (Printf.sprintf "clingo --outf=2 %s" (Filename.quote tmp)) in
  let out = read_all ic in
  let status = Unix.close_process_in ic in
  Sys.remove tmp;
  (out, status)

(* the viol/2 witnesses of an UNSAT program: re-run with the global
   integrity constraint stripped and the witnesses shown *)
let diagnose program =
  let relaxed =
    String.concat "\n"
      (List.filter
         (fun l -> String.trim l <> ":- viol(_,_).")
         (String.split_on_char '\n' program))
    ^ "\n#show viol/2.\n"
  in
  match run_clingo relaxed with
  | out, Unix.WEXITED (10 | 30) -> (
      try
        match Yojson.Safe.from_string out with
        | `Assoc kvs -> (
            match List.assoc_opt "Call" kvs with
            | Some (`List (call :: _)) -> (
                match call with
                | `Assoc ckvs -> (
                    match List.assoc_opt "Witnesses" ckvs with
                    | Some (`List (`Assoc wkvs :: _)) -> (
                        match List.assoc_opt "Value" wkvs with
                        | Some (`List atoms) ->
                            List.filter_map
                              (function
                                | `String s
                                  when String.length s >= 5 && String.sub s 0 5 = "viol(" ->
                                    Some s
                                | _ -> None)
                              atoms
                        | _ -> [])
                    | _ -> [])
                | _ -> [])
            | _ -> [])
        | _ -> []
      with _ -> [])
  | _ -> []

(* run clingo on [program]; [`Sat json] with the outf2 payload, or
   [`Unsat msg] with the diagnosed report the expectedFault fragment
   matches *)
let solve program =
  match run_clingo program with
  | out, Unix.WEXITED (10 | 30) -> `Sat out (* clingo: SAT / SAT+shown *)
  | _, Unix.WEXITED 127 ->
      prerr_endline "CLINGO NOT FOUND on PATH";
      exit 2
  | _, Unix.WEXITED 20 ->
      let reason =
        match diagnose program with
        | [] -> "no viol/2 witness (the input is not stratified?)"
        | vs -> String.concat "; " vs
      in
      `Unsat (Printf.sprintf "clingo: UNSATISFIABLE — a fail-fast guard fired: %s" reason)
  | out, Unix.WEXITED c ->
      Printf.eprintf "clingo exited %d:\n%s\n" c out;
      exit 1
  | _, _ ->
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
