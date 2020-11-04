package hiro116s.simulator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.io.CharStreams;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.SimulationResults;
import hiro116s.simulator.simulator.CommandLineSimulator;
import hiro116s.simulator.simulator.ConcurrentCommandLineSimulator;
import hiro116s.simulator.simulator.Simulator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Tool to evaluate the score in the heuristic contest.
 */
public class MarathonCodeSimulator {
    private static final String MAIN_NAME = "SoccerTournament";

    private final Simulator simulator;
    private final Arguments arguments;

    public MarathonCodeSimulator(final Arguments arguments) {
        this.simulator = ConcurrentCommandLineSimulator.create(
                arguments.numThreads,
                LongStream.rangeClosed(arguments.minSeed, arguments.maxSeed).boxed().collect(Collectors.toList()),
                seed -> new CommandLineSimulator(seed, Runtime.getRuntime())
        );
        this.arguments = arguments;
    }

    public static void main(String[] args) {
        final Arguments arguments = parseArgs(args);
        new MarathonCodeSimulator(arguments).run();
    }

    private void run() {
        final SimulationResults results = simulator.simulate();

        final String logFileName = String.format("%s-%s%s.log",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                getGitCommitHash(),
                arguments.additionalNote
        );
        final String logFilePath = String.format("%s/%s",
                arguments.logOutputDir,
                logFileName
        );
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath))) {
            bw.write(results.toJsonString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (arguments.shouldUploadToS3) {
            final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.AP_NORTHEAST_1)
                    .build();
            if (!s3.doesBucketExistV2(arguments.s3BucketName)) {
                s3.createBucket(arguments.s3BucketName);
            }
            s3.putObject(arguments.s3BucketName, MAIN_NAME + "/log/" + logFileName, new File(logFilePath));
        }
    }

    // TODO: Use JGit library
    private String getGitCommitHash() {
        try {
            final Runtime runtime = Runtime.getRuntime();
            try (final InputStreamReader inputStreamReader = new InputStreamReader(runtime.exec("git rev-parse HEAD").getInputStream())) {
                return CharStreams.readLines(inputStreamReader).get(0).substring(0, 6);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Arguments parseArgs(final String[] args) {
        final Arguments arguments = new Arguments();
        final CmdLineParser parser = new CmdLineParser(arguments);
        try {
            parser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e);
            System.exit(1);
        }
        return arguments;
    }

    private static class Arguments {
        @Option(name = "--numThreads")
        private int numThreads = 1;

        @Option(name = "--minSeed")
        private int minSeed = 1;

        @Option(name = "--maxSeed")
        private int maxSeed = 100;

        @Option(name = "--logOutputDir")
        private String logOutputDir = "./log";

        @Option(name = "--additionalNote", usage = "additional note for file name")
        private String additionalNote = "";

        @Option(name = "--s3", usage = "s3 option")
        private boolean shouldUploadToS3 = false;

        @Option(name = "--s3bucket", usage = "s3 bucket name")
        private String s3BucketName = "hiro116s.s3bucket.jp";
    }
}
