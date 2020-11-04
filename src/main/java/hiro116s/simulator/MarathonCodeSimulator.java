package hiro116s.simulator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import hiro116s.simulator.model.Result;
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
import java.util.stream.LongStream;

/**
 * Tool to evaluate the score in the heuristic contest.
 */
public class MarathonCodeSimulator {
    private static final String TESTER_NAME = "SoccerTournamentTester";
    private static final String MAIN_NAME = "SoccerTournament";
    private static final String MAIN_PACKAGE_NAME = "hiro116s.main";

    private final ExecutorService executorService;
    private final Arguments arguments;

    public MarathonCodeSimulator(final Arguments arguments) {
        executorService = Executors.newFixedThreadPool(arguments.numThreads);
        this.arguments = arguments;
    }

    public static void main(String[] args) {
        final Arguments arguments = parseArgs(args);
        new MarathonCodeSimulator(arguments).run();
    }

    private void run() {
        final Runtime runtime = Runtime.getRuntime();

        final List<Future<Result>> futures = new ArrayList<>();
        LongStream.range(1, arguments.maxSeed + 1).forEach(seed -> {
            futures.add(executorService.submit(() -> {
                final Stopwatch stopwatch = Stopwatch.createStarted();
                System.out.println(String.format("Start seed %d", seed));
                final Process exec = runtime.exec(toCommand(seed));
                try (final InputStreamReader inputStreamReader = new InputStreamReader(exec.getInputStream())) {
                    final ParsedData parsedData = CharStreams.readLines(inputStreamReader, new OutputLineProcessor());
                    System.out.println(String.format("End seed %d, elapsed time: %d ms", seed, stopwatch.elapsed(TimeUnit.MILLISECONDS)));
                    return new Result(seed, parsedData);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        });

        final List<Result> results = new ArrayList<>();
        try {
            for (final Future<Result> future : futures) {
                Result result = future.get();
                results.add(result);
                System.out.println(result);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }

        final String logFileName = String.format("%s-%s%s.log",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                getGitCommitHash(runtime),
                arguments.additionalNote
        );
        final String logFilePath = String.format("%s/%s",
                arguments.logOutputDir,
                logFileName
        );
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath))) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            bw.write(jsonString);
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

    private String getGitCommitHash(final Runtime runtime) {
        try {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(runtime.exec("git rev-parse HEAD").getInputStream())) {
                return CharStreams.readLines(inputStreamReader).get(0).substring(0, 6);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    private class OutputLineProcessor implements LineProcessor<ParsedData> {
        private double score = -1;
        private double maxScore = -1;

        @Override
        public boolean processLine(String line) throws IOException {
            if (line.startsWith("Score")) {
                final String[] ws = line.split(" = ");
                score = Double.parseDouble(ws[1]);
            } else if (line.startsWith("Max")) {
                final String[] ws = line.split(" = ");
                maxScore = Double.parseDouble(ws[1]);
            } else if (line.startsWith("LOG")) {
                System.out.println(line);
            }
            return true;
        }

        @Override
        public ParsedData getResult() {
            Preconditions.checkArgument(score != -1, "No score statement");
            Preconditions.checkArgument(maxScore != -1, "No maxScore statement");
            return new ParsedData(score, maxScore);
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
