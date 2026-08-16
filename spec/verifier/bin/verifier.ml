let () =
  match Sys.argv with
  | [| _; file |] -> (
      try
        let doc = Ir_v0.Loader.load_file file in
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
  | _ ->
      prerr_endline "usage: verifier <ir.json>";
      exit 2
