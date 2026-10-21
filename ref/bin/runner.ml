(* The oracle runner: replay one uchar-case-v0 file against its ir.json.
   Exit 0: every expectation matches exactly — or, on an expectedFault
   case (corpus/red), the LOADING OR the evaluation faults with the pinned
   code and parameters. Exit 1: a mismatch, an unexpected fault, a fault that
   does not occur, or a fault of another code or parameters (the whole report
   then goes to stderr). Exit 2: a load or cross-check error that no
   expectedFault pins.

   With --noop the evaluation is skipped and the expectations are
   checked against the raw input world — the gate-7 guard: a corpus a
   do-nothing executor satisfies has stopped testing anything. With --trace
   the runner prints the readings of the conditions at the fixpoint ([trace]
   below), which gate 17 holds to the Drools backend's. *)

(* The two machine lines of the red path, which gate 8 reads: the code and
   parameters this executor observed (RUNTIME-SEMANTICS ch. 5.7b), and the case's
   own expectedFault code and params as the case reader decoded them. The runner
   decodes them, never the shell: a JSON string may spell any character as an
   escape. "-" stands for no code. Printed whenever the red path observed a fault,
   before the verdict. *)
let print_codes ~observed ~params ~(pinned : Uchar_ref.Case.expected_fault) =
  let line tag code params =
    match code with
    | None -> Printf.printf "%s -\n" tag
    | Some c -> Printf.printf "%s %s %s\n" tag c (Uchar_ref.Case.params_json params)
  in
  line "CODE" observed params;
  line "CODE-PINNED" (Some pinned.ef_code) pinned.ef_params

(* the verdict on an observed fault: the pin matches, or the whole report is shown *)
let verdict ~(pinned : Uchar_ref.Case.expected_fault) ~code ~params ~what report =
  print_codes ~observed:code ~params ~pinned;
  if Uchar_ref.Case.pin_matches pinned ~code ~params then
    Printf.printf "oracle: expected fault observed%s (%s)\n" what pinned.ef_code
  else begin
    Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %s %s in:\n%s\n" pinned.ef_code
      (Uchar_ref.Case.params_json pinned.ef_params) report;
    exit 1
  end

(* --trace: the readings of the conditions at the fixpoint of a golden case
   (RUNTIME-SEMANTICS ch. 5.12), one line per reading and per node in the
   canonical text, each after "reading ", sorted; it compares no expectation.
   Exit 0 with the readings; 1 when the evaluation faults; 2 on a load error
   or a case that pins a fault, which has no fixpoint to read. *)
let trace ir_path case_path =
  try
    let doc = Ir_v0.Loader.load_file ir_path in
    let case = Uchar_ref.Case.load_file doc case_path in
    if case.expected_fault <> None then (
      prerr_endline "--trace reads the fixpoint of a golden case; an expectedFault case has none";
      exit 2);
    let env =
      {
        Uchar_ref.Eval.doc;
        params = case.params;
        param_tls = case.param_tls;
        as_of = case.as_of;
        locals = Uchar_ref.World.SMap.empty;
        btypes = Uchar_ref.World.SMap.empty;
        subj = None;
      }
    in
    let final = Uchar_ref.Eval.evaluate env case.world in
    let readings =
      Uchar_ref.Reading.readings (Yojson.Safe.from_file ir_path) env ~world0:case.world ~world:final
    in
    List.iter (fun l -> print_endline ("reading " ^ l)) (Uchar_ref.Reading.lines env ~world:final readings)
  with
  | Uchar_ref.Value.Fault { code; text; _ } ->
      Printf.eprintf "EVALUATION FAULT: %s\n" (Uchar_ref.Value.report ~code text);
      exit 1
  | Uchar_ref.Case.Error { code; text; _ } ->
      Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code text);
      exit 2
  | Ir_v0.Loader.Error (path, msg) ->
      Printf.eprintf "IR LOAD ERROR at %s: %s\n" (if path = "" then "<root>" else path) msg;
      exit 2
  | Sys_error e | Yojson.Json_error e ->
      Printf.eprintf "LOAD ERROR: %s\n" e;
      exit 2

let () =
  (match Sys.argv with
  | [| _; "--trace"; ir; case |] ->
      trace ir case;
      exit 0
  | _ -> ());
  let noop, ir_path, case_path =
    match Sys.argv with
    | [| _; "--noop"; ir; case |] -> (true, ir, case)
    | [| _; ir; case |] -> (false, ir, case)
    | _ ->
        prerr_endline
          "usage: runner [--noop | --trace] <ir.json> <case.json>";
        exit 2
  in
  try
    (* Split reading. The pinned fragment sits in the same file whose
       INPUT may be unreadable, so the contract field is read on its
       own, before the world. Read the other way round — the field
       parsed behind the world it is supposed to describe — a refusal
       from the case reader could only ever crash on exit 2, and the
       whole class of load faults both executors agree on stayed
       unpinnable. *)
    let pinned = Uchar_ref.Case.expected_fault_of_file case_path in
    if noop && pinned <> None then (
      prerr_endline "--noop is a golden-corpus guard; an expectedFault case has no use for it";
      exit 2);
    let doc = Ir_v0.Loader.load_file ir_path in
    let case =
      try Uchar_ref.Case.load_file doc case_path
      with Uchar_ref.Case.Error { code; params; text = msg } -> (
        match pinned with
        | None ->
            Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code msg);
            exit 2
        | Some pinned ->
            (* the red path's LOAD arm: the case reader refused, and
               that refusal is as observable a failure as a fault at
               evaluation — it is matched against the same pin *)
            verdict ~pinned ~code ~params ~what:" at load"
              ("CASE ERROR: " ^ Uchar_ref.Value.report ~code msg);
            exit 0)
    in
    let env =
      {
        Uchar_ref.Eval.doc;
        params = case.params;
        param_tls = case.param_tls;
        as_of = case.as_of;
        locals = Uchar_ref.World.SMap.empty;
        btypes = Uchar_ref.World.SMap.empty; subj = None;
      }
    in
    match case.expected_fault with
    | Some pinned -> (
        (* the red path's EVALUATION arm (the load arm is above): the
           run MUST fault, observably, as pinned — never a silent success *)
        match Uchar_ref.Eval.evaluate env case.world with
        | (_ : Uchar_ref.World.t) ->
            prerr_endline "EXPECTED FAULT DID NOT OCCUR: evaluation succeeded";
            exit 1
        | exception Uchar_ref.Value.Fault { code; params; text } ->
            verdict ~pinned ~code ~params ~what:"" (Uchar_ref.Value.report ~code text))
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
  | Uchar_ref.Value.Fault { code; text; _ } ->
      Printf.eprintf "EVALUATION FAULT: %s\n" (Uchar_ref.Value.report ~code text);
      exit 1
  | Uchar_ref.Case.Error { code; text; _ } ->
      Printf.eprintf "CASE ERROR: %s\n" (Uchar_ref.Value.report ~code text);
      exit 2
  | Ir_v0.Loader.Error (path, msg) ->
      Printf.eprintf "IR LOAD ERROR at %s: %s\n" (if path = "" then "<root>" else path) msg;
      exit 2
  | Sys_error e | Yojson.Json_error e ->
      Printf.eprintf "LOAD ERROR: %s\n" e;
      exit 2
