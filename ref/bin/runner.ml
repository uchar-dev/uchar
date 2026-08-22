(* The oracle runner: replay one uchar-case-v0 file against its ir.json.
   Exit 0: every expectation matches exactly — or, on an expectedFault
   case (corpus/rood), the evaluation faults and the report contains the
   pinned fragment. Exit 1: a mismatch, an unexpected fault, a fault
   that does not occur, or a fault report without the fragment. Exit 2:
   a load or cross-check error.

   With --noop the evaluation is skipped and the expectations are
   checked against the raw input world — the gate-7 guard: a corpus a
   do-nothing executor satisfies has stopped testing anything. *)

let () =
  let noop, ir_path, case_path =
    match Sys.argv with
    | [| _; "--noop"; ir; case |] -> (true, ir, case)
    | [| _; ir; case |] -> (false, ir, case)
    | _ ->
        prerr_endline "usage: runner [--noop] <ir.json> <case.json>";
        exit 2
  in
  try
    let doc = Ir_v0.Loader.load_file ir_path in
    let case = Uchar_ref.Case.load_file doc case_path in
    let env =
      {
        Uchar_ref.Eval.doc;
        params = case.params;
        param_tls = case.param_tls;
        as_of = case.as_of;
        locals = Uchar_ref.World.SMap.empty;
      }
    in
    match case.expected_fault with
    | Some { ef_ref; _ } -> (
        if noop then (
          prerr_endline "--noop is a golden-corpus guard; an expectedFault case has no use for it";
          exit 2);
        (* the red path: evaluation MUST fault, observably, with the
           pinned fragment in the report — never a silent success *)
        match Uchar_ref.Eval.evaluate env case.world with
        | (_ : Uchar_ref.World.t) ->
            prerr_endline "EXPECTED FAULT DID NOT OCCUR: evaluation succeeded";
            exit 1
        | exception Uchar_ref.Value.Fault msg ->
            if Uchar_ref.Case.contains ~needle:ef_ref msg then
              Printf.printf "oracle: expected fault observed (%s)\n" ef_ref
            else begin
              Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %S in:\n%s\n" ef_ref msg;
              exit 1
            end)
    | None -> (
        let final = if noop then case.world else Uchar_ref.Eval.evaluate env case.world in
        match Uchar_ref.Case.check case final with
        | [] ->
            Printf.printf "oracle: PASS (%d expectations%s)\n" (List.length case.expected)
              (if noop then ", NO-OP" else "")
        | mismatches ->
            List.iter (fun m -> Printf.printf "MISMATCH: %s\n" m) mismatches;
            exit 1)
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
      exit 2
