package dev.uchar.regelspraak.loader;

import dev.uchar.regelspraak.refusal.Reason;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.ContractValidator;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader round-trip witness (card acceptance): loader(emit(model)) reaches a valid transpiler
 * input. The corpus {@code ir.json} files ARE emit(model) — the frontend's IrEmitter dump of a
 * resolved model — so loading one and building the exec-model transpiler over the result witnesses
 * that {@link IrLoader} faithfully inverts the emitter into a model the transpiler accepts and wholly
 * lowers; since card tms-omschakeling that is the transpiler of the exec-model branch. Pinned on THREE
 * structurally-different goldens: scalar arithmetic, a date computation, and a factType-role navigation. (The 41-golden
 * played set of gate 12 is the stronger end-to-end witness — those loaded models fire to the exact pinned ref≡clingo
 * values.)
 */
class RoundTripWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void arithmetic_golden_loads_and_lowers() throws Exception {
        witness("anchor/jutterswet-jutheffing");
    }

    @Test void date_golden_loads_and_lowers() throws Exception {
        witness("anchor/jutterswet-jutleeftijd");
    }

    @Test void navigation_golden_loads_and_lowers() throws Exception {
        witness("synth-navigatiepremie");
    }

    private void witness(String golden) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());

        // the whole load: since L2 the relations a read walks ride beside the model, as bindings
        IrLoader.Loaded loaded = IrLoader.loadAll(JSON.readTree(Files.readString(ir)));
        RegelSpraakModel model = loaded.model();
        // the loader reconstructed a non-empty model with resolved rules — the emitter's inverse.
        assertFalse(model.objecttypen.isEmpty(), golden + ": loaded objecttypen");
        assertFalse(model.resolvedRegels.isEmpty(), golden + ": loaded resolvedRegels");

        // the loaded model is a valid transpiler input: beans generate, the contract holds, and
        // the truth-maintenance back-end wholly lowers it (no marker/skip) — loader(emit(model)) round-trips.
        Map<String, String> sources = FactModelGenerator.generate(model);
        ClassLoader cl = BeanCompiler.compile(sources);
        List<Reason> violations = ContractValidator.validate(model, cl);
        assertTrue(violations.isEmpty(), golden + ": bean contract holds — " + violations);

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            TmsTranspiler et = TmsTranspiler.build(model, cl, loaded.prioritizedDefaults(),
                    loaded.roleBindings(), loaded.lets(), loaded.sourcePaths(), loaded.navigationBindings());
            for (Coverage d : et.coverage())
                assertTrue(d.lowered(), golden + ": construct lowered — " + d.key() + " " + d.verdict());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
