package hiro116s.simulator.simulator;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.SimulationResults;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class CommandLineSimulator implements Simulator {
    private static final String TESTER_NAME = "SoccerTournamentTester";
    private static final String MAIN_NAME = "SoccerTournament";
    private static final String MAIN_PACKAGE_NAME = "hiro116s.main";

    private final long seed;
    private final Runtime runtime;

    public CommandLineSimulator(long seed, Runtime runtime) {
        this.seed = seed;
        this.runtime = runtime;
    }

    @Override
    public SimulationResults simulate() {
        // TODO: Use log4j
        System.out.println("Start seed " + seed);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final Process exec;
        try {
            exec = runtime.exec(toCommand(seed));
            try (final InputStreamReader inputStreamReader = new InputStreamReader(exec.getInputStream())) {
                final ParsedData parsedData = CharStreams.readLines(inputStreamReader, new OutputLineProcessor());
                System.out.println(String.format("End seed %d, elapsed time: %d ms", seed, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
                return new SimulationResults(Lists.newArrayList(new Result(seed, parsedData)));
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: Change it.
    private static String[] toCommand(long seed) {
        return new String[]{
                "java",
                TESTER_NAME,
                "-exec",
                String.format("java %s.%s", MAIN_PACKAGE_NAME, MAIN_NAME),
                "-seed",
                String.valueOf(seed)
        };
    }

}
