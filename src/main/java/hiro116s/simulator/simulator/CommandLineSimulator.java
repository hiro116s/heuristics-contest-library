package hiro116s.simulator.simulator;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.CommandTemplate;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.SimulationResults;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommandLineSimulator implements Simulator {
    private final long seed;

    private final CommandTemplate commandTemplate;

    @CheckForNull
    private final File directory;

    public CommandLineSimulator(long seed,
                                final CommandTemplate commandTemplate,
                                @CheckForNull final File directory) {
        this.seed = seed;
        this.commandTemplate = commandTemplate;
        this.directory = directory;
    }

    @Override
    public SimulationResults simulate() {
        // TODO: Use log4j
        System.out.println("Start seed " + seed);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final ProcessBuilder processBuilder = new ProcessBuilder(commandTemplate.build(seed))
                .redirectErrorStream(true);
        commandTemplate.inRedirectFilePathOrEmpty(seed).ifPresent(
                redirectFilePath -> processBuilder.redirectInput(ProcessBuilder.Redirect.from(new File(redirectFilePath)))
        );
        Optional.ofNullable(directory).ifPresent(processBuilder::directory);

        final Process exec;
        try {
            exec = processBuilder.start();
            try (final InputStreamReader inputStreamReader = new InputStreamReader(exec.getInputStream())) {
                final ParsedData parsedData = CharStreams.readLines(inputStreamReader, new OutputLineProcessor());
                System.out.println(String.format("End seed %d, elapsed time: %d ms", seed, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
                return new SimulationResults(Lists.newArrayList(new Result(seed, parsedData)));
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
