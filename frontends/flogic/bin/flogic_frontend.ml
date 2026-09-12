(* The F-logic frontend's command line: one source in, one IR document out.

   A refusal leaves NO output file and exits 2 with its message on stderr —
   a partial or guessed document would be worse than nothing. *)

let usage = "usage: flogic_frontend <source.flogic> <out.json>"

let () =
  match Sys.argv with
  | [| _; src; out |] -> (
      try
        let doc = Flogic_frontend_lib.Lower.of_file src in
        Flogic_frontend_lib.Emit.to_file out doc
      with
      | Flogic_frontend_lib.Syntax.Refusal msg ->
          prerr_endline ("flogic: REFUSED " ^ src ^ ": " ^ msg);
          exit 2
      | Sys_error msg ->
          prerr_endline ("flogic: " ^ msg);
          exit 2
      (* The serialiser's fail-fast arms and any internal invariant land
         here rather than as a bare uncaught exception: an operator reading
         the output must see WHICH channel failed. A FATAL is this
         frontend's own bug, not the source's — hence a separate prefix
         from REFUSED, and exit 3. *)
      | Failure msg | Invalid_argument msg ->
          prerr_endline ("flogic: FATAL " ^ src ^ ": " ^ msg);
          exit 3
      | Not_found ->
          prerr_endline ("flogic: FATAL " ^ src ^ ": an internal lookup failed");
          exit 3)
  | _ ->
      prerr_endline usage;
      exit 2
