package hiro116s.simulator.simulator;

import com.google.common.collect.ImmutableMap;
import hiro116s.simulator.model.ImmutableCommandTemplate;
import hiro116s.simulator.model.SimulationResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineSimulatorTest {
    private static final Set<PosixFilePermission> PERMISSION_755 = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE
    );

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() throws IOException {
        putNewCommand("echoerr", "echo \"$@\" 1>&2");
        putNewCommand("caterr", "cat 1>&2");
    }

    private void putNewCommand(String commandName, String command) throws IOException {
        final Path commandPath = Files.writeString(Paths.get(tempDir.toString(), commandName), command, StandardOpenOption.CREATE_NEW);
        Files.setPosixFilePermissions(commandPath, PERMISSION_755);
    }

    @Test
    void simulate() throws IOException {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate(tempDir.toString() + "/echoerr")
                .addCommandTemplate("Score = $SEED")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), null);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void simulate2() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate(tempDir.toString() + "/echoerr")
                .addCommandTemplate("Score = ")
                .addCommandTemplate("$SEED")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), null);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }

    @Test
    void simulate3() {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate(tempDir.toString() + "/echoerr")
                .addCommandTemplate("Score = $SEED")
                .addCommandTemplate("\nParam:M = 1")
                .addCommandTemplate("\nParam:N = 2")
                .addCommandTemplate("\nParam:hoge = fuga")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), null);

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
        putNewCommand("test", String.format("echo abc; %s/echoerr Score = 1", tempDir.toString()));
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate(tempDir.toString() + "/test")
                .addCommandTemplate("$SEED")
                .build();
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), null);

        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertTrue(actual.getResults().get(0).parsedData.params.isEmpty());
        assertEquals(1, actual.getResults().get(0).seed);

        // stdout contents verification
        assertEquals("abc\n", Files.readString(Path.of(tempDir.toString(), "1.txt")));
    }

    @Test
    void withRedirectInput() throws Exception {
        final ImmutableCommandTemplate commandTemplate = ImmutableCommandTemplate.builder()
                .addCommandTemplate(tempDir.toString() + "/caterr")
                .addCommandTemplate("<")
                .addCommandTemplate(tempDir + "/in$SEED.txt")
                .build();

        Files.writeString(Paths.get(tempDir.toString(), "in1.txt"), "Score = 1\n", StandardOpenOption.CREATE_NEW);
        final CommandLineSimulator simulator = new CommandLineSimulator(1L, commandTemplate, tempDir.toFile(), null);
        final SimulationResults actual = simulator.simulate();
        assertEquals(1, actual.getResults().size());
        assertEquals(1, actual.getResults().get(0).parsedData.score);
        assertEquals(1, actual.getResults().get(0).seed);
    }
}