package hiro116s.simulator.simulator;

import com.google.common.collect.ImmutableMap;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.ImmutableCommandTemplate;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.SimulationResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineSimulatorTest {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(Long.MAX_VALUE);

    @TempDir
    Path tempDir;

    @Test
    void simulate() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("echo Score = $SEED 1>&2")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", DEFAULT_TIMEOUT);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void simulate2() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("echo Score = $SEED\\\\nParam:M = 1\\\\nParam:N = 2\\\\nParam:hoge = fuga 1>&2")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", DEFAULT_TIMEOUT);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(ImmutableMap.of(
                "M", 1L,
                "N", 2L,
                "hoge", "fuga"
        ), actual.getResults().get(0).parsedData.params);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void verifyStdoutIsWrittenToFile() throws IOException {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("echo abc && echo Score = $SEED 1>&2")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", DEFAULT_TIMEOUT);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertTrue(actual.getResults().get(0).parsedData.params.isEmpty());
        assertEquals(1, actual.getResults().get(0).seed);

        // stdout contents verification
        assertEquals("abc\n", Files.readString(Path.of(tempDir.toString(), "1.txt")));
    }

    @Test
    void withRedirectInput(@TempDir final Path tempDir) throws Exception {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("cat < " + tempDir + "/in$SEED.txt 1>&2")
                .build();

        Files.writeString(Paths.get(tempDir.toString(), "in1.txt"), "Score = 1\n", StandardOpenOption.CREATE_NEW);
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", DEFAULT_TIMEOUT);
        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void overTimeout() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("sleep $SEED")  // Sleep $SEED seconds for testing purpose
                .build();

        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", Duration.ofMillis(990L));
        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(ParsedData.TIMEOUT_DATA, actual.getResults().get(0).parsedData);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void withinTimeout() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("sh")
                .addCommandTemplate("-c")
                .addCommandTemplate("sleep 1 && echo Score = $SEED 1>&2")
                .build();

        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), new OutputLineProcessor(false), null, "a", Duration.ofMillis(1315L));
        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }
}