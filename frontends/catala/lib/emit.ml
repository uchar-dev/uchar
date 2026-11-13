(* The plugin's serialiser is the shared one of the [uchar-ir-v0] library
   ([Ir_v0.Emit]); its refusals name the plugin "the skeleton". *)
include Ir_v0.Emit.Make (struct
  let producer = "skeleton"
end)
