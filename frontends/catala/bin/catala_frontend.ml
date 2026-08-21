(* The Catala frontend plugin CLI:  catala_frontend <input.catala_en> <out-ir.json>

   Exit 0: IR emitted. Exit 3: a loud refusal (a construct outside the
   decided 5a fragment) — the reason is on stderr. Exit 2: usage / a
   Catala compile error. *)

module T = Catala_frontend_lib.Translate

let () =
  match Sys.argv with
  | [| _; input; output |] -> (
      try T.run ~input ~output
      with
      | T.Refuse msg ->
          Printf.eprintf "catala_frontend: refused: %s\n" msg;
          exit 3
      | e ->
          Printf.eprintf "catala_frontend: %s\n" (Printexc.to_string e);
          exit 2)
  | _ ->
      prerr_endline "usage: catala_frontend <input.catala_en> <out-ir.json>";
      exit 2
