let () =
  match Sys.argv with
  | [| _; file |] -> (
      try
        (* the verifier loads what its rules judge (Loader.reader) *)
        let doc = Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier file in
        match Ir_v0.Checks.all doc with
        | [] ->
            print_endline "verifier: PASS"
        | violations ->
            List.iter
              (fun (x : Ir_v0.Checks.violation) ->
                Printf.printf "%s FAIL %s: %s\n" x.wf x.at x.reason)
              violations;
            exit 1
      with
      | Ir_v0.Loader.Error (path, msg) ->
          Printf.eprintf "LOAD ERROR at %s: %s\n"
            (if path = "" then "<root>" else path) msg;
          exit 2
      | Sys_error e | Yojson.Json_error e ->
          Printf.eprintf "LOAD ERROR: %s\n" e;
          exit 2)
  | [| _; "--dependency-edges"; file |] -> (
      (* the dependency graph of ch. 5, printed whether or not the
         document is well-formed: a reader that must agree with the graph
         compares it here (Checks.dependency_edges) *)
      try
        let doc = Ir_v0.Loader.load_file ~reader:Ir_v0.Loader.Verifier file in
        List.iter print_endline (Ir_v0.Checks.dependency_edges doc)
      with
      | Ir_v0.Loader.Error (path, msg) ->
          Printf.eprintf "LOAD ERROR at %s: %s\n"
            (if path = "" then "<root>" else path) msg;
          exit 2
      | Sys_error e | Yojson.Json_error e ->
          Printf.eprintf "LOAD ERROR: %s\n" e;
          exit 2)
  | _ ->
      prerr_endline "usage: verifier <ir.json> | verifier --dependency-edges <ir.json>";
      exit 2
