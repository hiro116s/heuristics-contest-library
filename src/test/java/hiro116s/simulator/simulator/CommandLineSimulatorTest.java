package hiro116s.simulator.simulator;

import hiro116s.simulator.model.ImmutableCommandTemplate;
import hiro116s.simulator.model.SimulationResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineSimulatorTest {
    @Test
    void simulate() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("echo")
                .addCommandTemplate("Score = $SEED")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, null);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void simulate2() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("echo")
                .addCommandTemplate("Score = ")
                .addCommandTemplate("$SEED")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, null);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void withRedirectInput(@TempDir final Path tempDir) throws Exception {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate("cat")
                .addCommandTemplate("<")
                .addCommandTemplate(tempDir + "/in$SEED.txt")
                .build();

        Files.writeString(Paths.get(tempDir.toString(), "in1.txt"), "Score = 1\n", StandardOpenOption.CREATE_NEW);
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, null);
        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }
}