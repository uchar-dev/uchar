let () =
  match Sys.argv with
  | [| _; file |] ->
      let _doc = Ir_v0.Loader.load_file file in
      print_endline "loaded"
  | _ -> prerr_endline "usage: runner <ir.json>"; exit 2
