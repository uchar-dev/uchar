(* ONE clingo invocation, in ONE place.

   The gate-6 runner (bin/clingo_runner.ml) and the backend's own suite
   (test/test_clingo.ml) both have to reach the solver, and each growing
   its own way of calling it is how a suite comes to pin something the
   gate never runs. So the invocation lives here, in the library both
   consume, and the callers differ only in what they do with the result:
   the runner diagnoses UNSAT against a case's expectedFault, the suite
   checks a frame's expectations the way [ref_matches] checks ref's. *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do
       Buffer.add_channel b ic 1
     done
   with End_of_file -> ());
  Buffer.contents b

(* one clingo invocation on [text]; the raw outf2 payload and exit.
   Exit codes the callers read: 10/30 SAT (30 = SAT with shown atoms),
   20 UNSAT, 127 clingo not on PATH — never silently swallowed. *)
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
