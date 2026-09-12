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
          exit 2)
  | _ ->
      prerr_endline usage;
      exit 2
