package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The positions of a loaded document read alike from every thread: threads that ask {@link NodePositions#subtree} of a
 * freshly loaded document at the same moment, from one barrier, each get the nodes one thread alone gets. A consumer
 * that explains a session (the decision service of the distribution) numbers the nodes of a form by this table, per
 * request, on a table it loaded once; requests run side by side. Measured before the table was complete at the end of
 * the load: {@code subtree} built its pre-order at its first call, and a thread that came in while another sorted it
 * searched an unsorted list and got too few nodes, in 174 of the 200 rounds of this test.
 */
class PositionsAcrossThreadsWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final int THREADS = 8, ROUNDS = 200;

    @Test void threads_that_ask_the_positions_of_a_fresh_load_at_once_get_what_one_thread_gets() throws Exception {
        JsonNode document = JSON.readTree(Files.readString(GOLDEN.resolve("jutterswet-volledig-gegeven").resolve("ir.json")));
        List<List<String>> alone = subtrees(IrLoader.loadWithPositions(document));
        assertTrue(alone.size() >= 10, "roots: " + alone.size());
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        int differing = 0;
        List<String> first = new ArrayList<>();
        try {
            for (int round = 0; round < ROUNDS; round++) {
                IrLoader.Positioned fresh = IrLoader.loadWithPositions(document);
                CyclicBarrier start = new CyclicBarrier(THREADS);
                List<Future<List<List<String>>>> answers = new ArrayList<>();
                for (int t = 0; t < THREADS; t++)
                    answers.add(pool.submit(() -> {
                        start.await(10, TimeUnit.SECONDS);
                        return subtrees(fresh);
                    }));
                boolean differs = false;
                for (Future<List<List<String>>> a : answers) {
                    String answer;
                    try {
                        List<List<String>> got = a.get(30, TimeUnit.SECONDS);
                        answer = got.equals(alone) ? null : "another answer";
                    } catch (java.util.concurrent.ExecutionException e) {
                        answer = String.valueOf(e.getCause());
                    }
                    if (answer != null) {
                        differs = true;
                        if (first.size() < 3) first.add("round " + round + ": " + answer);
                    }
                }
                if (differs) differing++;
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(0, differing, "rounds of " + ROUNDS + " in which a thread got another answer than one thread alone: "
                + first);
    }

    /** Per root of the document (the condition and the value of every rule, the condition and the consequence of every
     *  default), the pointers of {@link NodePositions#subtree}, in its order. */
    private static List<List<String>> subtrees(IrLoader.Positioned p) {
        List<Object> roots = new ArrayList<>();
        for (ResolvedRule r : p.loaded().model().resolvedRules) {
            if (r.condition() != null) roots.add(r.condition());
            if (r.value() != null) roots.add(r.value());
        }
        for (PrioritizedDefaults g : p.loaded().prioritizedDefaults())
            for (Default d : g.defaults()) {
                if (d.condition() != null) roots.add(d.condition());
                if (d.consequence() != null) roots.add(d.consequence());
            }
        List<List<String>> out = new ArrayList<>();
        for (Object root : roots)
            out.add(p.positions().subtree(root).stream().map(n -> p.positions().of(n).pointer()).toList());
        return out;
    }
}
