(* The oracle runner: replay one uchar-case-v0 file against its ir.json.
   Exit 0: every expectation matches exactly — or, on an expectedFault
   case (corpus/rood), the LOADING OR the evaluation faults and the
   report contains the pinned fragment. Exit 1: a mismatch, an
   unexpected fault, a fault that does not occur, or a fault report
   without the fragment. Exit 2: a load or cross-check error that no
   expectedFault pins.

   With --noop the evaluation is skipped and the expectations are
   checked against the raw input world — the gate-7 guard: a corpus a
   do-nothing executor satisfies has stopped testing anything.

   With --bottom-sentence the case's PINS are read and compared against a list
   of refusal sentences; no ir.json, no world, no evaluation. See
   [bottom_sentence] below. *)

(* --- the bottom-sentence fence, gate 8's fourth leg ----------------------------
   A red pin must cite a refusal that NAMES the form it refuses, never a
   fall-through arm's message, which is the same sentence for every form
   that arm catches. The gate owns the reason-carrying sentence list; this
   mode owns the two things that must not be approximated in shell:
   DECODING the fragment (a JSON string may spell any character as an
   escape — \u00XX, or a backslashed quote — and a text scrape would
   compare the escape instead of the character it stands for) and
   COMPARING it — with [Case.contains], the very matcher the
   red path above runs a fault report through, so the fence tests exactly
   the relation the pin will have at replay.

     runner --bottom-sentence <sentences-file> <case.json>

   stdout: "PINS <n>", then one "HIT <key> <line> <fragment>" per
   citation, the fragment printed %S so a control character in it cannot
   break the line the gate reads back. A case declaring no readable
   expectedFault exits 2: the fence may not be hollowed out by silence. *)
let bottom_sentence sentences_path case_path =
  let sentences =
    let ic = open_in_bin sentences_path in
    let rec go acc n =
      match input_line ic with
      | line -> go ((n, line) :: acc) (n + 1)
      | exception End_of_file ->
          close_in ic;
          List.rev acc
    in
    go [] 1
  in
  if sentences = [] then (
    Printf.eprintf "BOTTOM_SENTENCE: %s lists no sentence — an empty fence is never green\n"
      sentences_path;
    exit 2);
  match
    try Uchar_ref.Case.expected_fault_of_file case_path with
    | Uchar_ref.Case.Error msg | Sys_error msg | Yojson.Json_error msg ->
        Printf.eprintf "BOTTOM_SENTENCE: %s is unreadable: %s\n" case_path msg;
        exit 2
  with
  | None ->
      Printf.eprintf "BOTTOM_SENTENCE: %s declares no expectedFault to check\n" case_path;
      exit 2
  | Some { ef_ref; ef_clingo } ->
      print_endline "PINS 2";
      List.iter
        (fun (key, frag) ->
          List.iter
            (fun (n, sentence) ->
              (* an empty line would match everything (contains ~needle:"" is
                 true), so it is skipped rather than allowed to indict the corpus *)
              if
                sentence <> ""
                && (Uchar_ref.Case.contains ~needle:frag sentence
                   || Uchar_ref.Case.contains ~needle:sentence frag)
              then Printf.printf "HIT %s %d %S\n" key n frag)
            sentences)
        [ ("ref", ef_ref); ("clingo", ef_clingo) ]

let () =
  let noop, ir_path, case_path =
    match Sys.argv with
    | [| _; "--bottom-sentence"; sentences; case |] ->
        bottom_sentence sentences case;
        exit 0
    | [| _; "--noop"; ir; case |] -> (true, ir, case)
    | [| _; ir; case |] -> (false, ir, case)
    | _ ->
        prerr_endline
          "usage: runner [--noop] <ir.json> <case.json> | runner --bottom-sentence <sentences> <case.json>";
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
      with Uchar_ref.Case.Error msg -> (
        match pinned with
        | None ->
            Printf.eprintf "CASE ERROR: %s\n" msg;
            exit 2
        | Some { ef_ref; _ } ->
            (* the red path's LOAD arm: the case reader refused, and
               that refusal is as observable a failure as a fault at
               evaluation — it is matched against the same pin *)
            let report = "CASE ERROR: " ^ msg in
            if Uchar_ref.Case.contains ~needle:ef_ref report then (
              Printf.printf "oracle: expected fault observed at load (%s)\n" ef_ref;
              exit 0)
            else (
              Printf.eprintf "EXPECTED FAULT MISMATCH: wanted %S in:\n%s\n" ef_ref report;
              exit 1))
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
    | Some { ef_ref; _ } -> (
        (* the red path's EVALUATION arm (the load arm is above): the
           run MUST fault, observably, with the pinned fragment in the
           report — never a silent success *)
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
