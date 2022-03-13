package hiro116s.simulator.simulator;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.CommandTemplate;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.SimulationResults;

import javax.annotation.CheckForNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CommandLineSimulator implements Simulator {
    private final long seed;

    private final CommandTemplate commandTemplate;

    private final File outputDirectory;

    private final OutputLineProcessor outputLineProcessor;

    @CheckForNull
    private final File processRootDirectory;

    private final String simulationId;

    private final Duration timeout;

    public CommandLineSimulator(long seed,
                                final CommandTemplate commandTemplate,
                                final File outputDirectory,
                                final OutputLineProcessor outputLineProcessor,
                                @CheckForNull final File processRootDirectory,
                                final String simulationId,
                                final Duration timeout) {
        this.seed = seed;
        this.commandTemplate = commandTemplate;
        this.outputDirectory = outputDirectory;
        this.outputLineProcessor = outputLineProcessor;
        this.processRootDirectory = processRootDirectory;
        this.simulationId = simulationId;
        this.timeout = timeout;
    }

    @Override
    public SimulationResults simulate() {
        // TODO: Use log4j
        System.out.println("Start seed " + seed);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final ProcessBuilder processBuilder = new ProcessBuilder(commandTemplate.build(seed))
                .redirectOutput(Paths.get(outputDirectory.getPath(), String.format("%d.txt", seed)).toFile());
        commandTemplate.inRedirectFilePathOrEmpty(seed).ifPresent(
                redirectFilePath -> processBuilder.redirectInput(ProcessBuilder.Redirect.from(new File(redirectFilePath)))
        );
        Optional.ofNullable(processRootDirectory).ifPresent(processBuilder::directory);

        final Process exec;
        try {
            exec = processBuilder.start();
            final ParsedData parsedData = readParsedDataOrTimeout(exec);
            System.out.println(String.format("End seed %d, elapsed time: %d ms", seed, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
            return new SimulationResults(Lists.newArrayList(new Result(seed, simulationId, parsedData)));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ParsedData readParsedDataOrTimeout(Process exec) throws IOException {
        final long startTime = System.currentTimeMillis();
        try (final InputStream errorStream = exec.getErrorStream();
             final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream))) {
            final char[] buffer = new char[1024];
            final StringBuilder sb = new StringBuilder();
            while (true) {
                while (errorStream.available() <= 0) {
                    if (!exec.isAlive()) {
                        // TODO: This is not optimized for long string.
                        for (String s : sb.toString().split("\n")) {
                            outputLineProcessor.processLine(s);
                        }
                        return outputLineProcessor.getResult();
                    }
                    final long elapsedTimeMs = System.currentTimeMillis() - startTime;
                    if (elapsedTimeMs > timeout.toMillis()) {
                        return ParsedData.TIMEOUT_DATA;
                    }
                    try {
                        Thread.sleep(0L, 100000 /* 100 us */);
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                int n = bufferedReader.read(buffer);
                sb.append(String.valueOf(buffer, 0, n));
            }
        }
    }
}
