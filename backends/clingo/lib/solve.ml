(* ONE clingo invocation, in ONE place.

   The gate-6 runner (bin/clingo_runner.ml) and the backend's own suite
   (test/test_clingo.ml) both have to reach the solver, and each growing
   its own way of calling it is how a suite comes to pin something the
   gate never runs. So the invocation lives here, in the library both
   consume, and the callers differ only in what they do with the result:
   the runner matches an UNSAT diagnosis against a case's expectedFault, the
   suite checks a frame's expectations the way [ref_matches] checks ref's.
   The diagnosis itself lives here too ([diagnostic]). *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do
       Buffer.add_channel b ic 1
     done
   with End_of_file -> ());
  Buffer.contents b

(* one clingo invocation on [text]; the raw outf2 payload and exit.
   clingo is asked for two models (-n 2): by default it stops at the first,
   and a program with a second one — a document that is not stratified — was
   answered with whichever came first. With two asked for, the second arrives
   as a second witness and [Model.one_model] refuses it.
   Exit codes the callers read: 10/30 SAT (30 = the search space is
   exhausted, so the witnesses are every model there is), 20 UNSAT, 127
   clingo not on PATH — never silently swallowed. *)
let run_clingo text =
  let tmp = Filename.temp_file "uchar-clingo" ".lp" in
  let oc = open_out tmp in
  output_string oc text;
  close_out oc;
  let ic = Unix.open_process_in (Printf.sprintf "clingo --outf=2 -n 2 %s" (Filename.quote tmp)) in
  let out = read_all ic in
  let status = Unix.close_process_in ic in
  Sys.remove tmp;
  (out, status)

(* the viol/2 witnesses of an UNSAT program: re-run with the global integrity
   constraint stripped and the witnesses shown. Shared by the gate-6 runner,
   whose report the red corpus pins, and the backend's suite, which reads the
   same witnesses — one diagnosis, or the suite could pin a reason the gate
   never prints. A witness string is rendered with gringo's string escapes
   undone ([Model.unescape_str]), so a reason that quotes a declared name
   reads as ref prints it; a reason without escapes — every one before the
   shared parameter fail-fast — comes out byte for byte as before. *)
let diagnostic program =
  let relaxed =
    String.concat "\n"
      (List.filter
         (fun l -> String.trim l <> ":- viol(_,_).")
         (String.split_on_char '\n' program))
    ^ "\n#show viol/2.\n"
  in
  (* the start of clingo's output, so a fault shows what came back *)
  let head out = if String.length out <= 300 then out else String.sub out 0 300 ^ " ..." in
  (* A run this function cannot read faults, never answers with no witness:
     the runner would report an UNSAT whose guard it cannot name, and a
     caller that pins a reason would see none instead of the failure. *)
  match run_clingo relaxed with
  | out, Unix.WEXITED (10 | 30) -> (
      let unreadable () =
        Uchar_ref.Value.fault "the diagnosis run of clingo gave unreadable JSON: %s" (head out)
      in
      (* the witnesses of the relaxed program's ONE model: read off the first
         of two, the diagnosis would name the guards of one model and keep
         quiet about the other's *)
      let witnesses =
        match Yojson.Safe.from_string out with
        | `Assoc kvs -> (
            match List.assoc_opt "Call" kvs with
            | Some (`List (`Assoc ckvs :: _)) -> (
                match List.assoc_opt "Witnesses" ckvs with Some (`List ws) -> ws | _ -> unreadable ())
            | _ -> unreadable ())
        | _ -> unreadable ()
        | exception Yojson.Json_error _ -> unreadable ()
      in
      match Model.one_model witnesses with
      | `Assoc wkvs -> (
          match List.assoc_opt "Value" wkvs with
          | Some (`List atoms) ->
              List.filter_map
                (function
                  | `String s when String.length s >= 5 && String.sub s 0 5 = "viol(" ->
                      Some (Model.unescape_str s)
                  | _ -> None)
                atoms
          | _ -> unreadable ())
      | _ -> unreadable ())
  | _, Unix.WEXITED 20 ->
      (* no model even without the guard constraint, so no guard fired: the
         program itself has none (an odd cycle through a negation), refused
         as a program without exactly one stable model *)
      Model.one_model []
  | out, Unix.WEXITED c -> Uchar_ref.Value.fault "the diagnosis run of clingo exited %d: %s" c (head out)
  | out, (Unix.WSIGNALED n | Unix.WSTOPPED n) ->
      Uchar_ref.Value.fault "the diagnosis run of clingo was ended by signal %d: %s" n (head out)
