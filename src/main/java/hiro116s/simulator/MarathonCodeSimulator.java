package hiro116s.simulator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import hiro116s.simulator.model.CommandTemplate;
import hiro116s.simulator.model.SimulationResults;
import hiro116s.simulator.option.CommandTemplateOptionHandler;
import hiro116s.simulator.simulator.CommandLineSimulator;
import hiro116s.simulator.simulator.ConcurrentCommandLineSimulator;
import hiro116s.simulator.simulator.Simulator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FileOptionHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Tool to evaluate the score in the heuristic contest.
 */
public class MarathonCodeSimulator {
    private final Simulator simulator;
    private final Arguments arguments;

    public MarathonCodeSimulator(final Simulator simulator, final Arguments arguments) {
        this.simulator = simulator;
        this.arguments = arguments;
    }

    public static void main(String[] args) {
        final Arguments arguments = parseArgs(args);
        try {
            Files.createDirectories(arguments.stdoutDir.toPath());
            Files.createDirectories(arguments.logOutputDir.toPath());
        } catch (IOException e) {
            System.err.println("Failed to create required directories");
            e.printStackTrace();
            System.exit(1);
        }

        new MarathonCodeSimulator(
                ConcurrentCommandLineSimulator.create(
                        arguments.numThreads,
                        LongStream.rangeClosed(arguments.minSeed, arguments.maxSeed).boxed().collect(Collectors.toList()),
                        seed -> new CommandLineSimulator(seed, arguments.commandTemplate, arguments.stdoutDir, arguments.directory)),
                arguments
        ).run();
    }

    private void run() {
        final SimulationResults results = simulator.simulate();

        final String logFileName = String.format("%s-%s%s.log",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                getGitCommitHash(),
                arguments.additionalNote
        );
        final String logFilePath = String.format("%s/%s",
                arguments.logOutputDir.getPath(),
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
            s3.putObject(arguments.s3BucketName, arguments.s3KeyName + "/log/" + logFileName, new File(logFilePath));
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
            parser.printUsage(System.err);
            System.exit(1);
        }
        arguments.validate();
        return arguments;
    }

    public static class Arguments {
        @Option(name = "--numThreads", usage = "Number of threads")
        private int numThreads = 1;

        @Option(name = "--minSeed", usage = "min seed")
        private int minSeed = 1;

        @Option(name = "--maxSeed", usage = "max seed")
        private int maxSeed = 100;

        @Option(name = "--logOutputDir", usage = "log output directory", handler = FileOptionHandler.class)
        private File logOutputDir = new File("./log");

        @Option(name = "--stdoutDir", usage = "log output directory", handler = FileOptionHandler.class)
        private File stdoutDir = new File("./stdout");

        @Option(name = "--additionalNote", usage = "additional note for file name")
        private String additionalNote = "";

        @Option(name = "--s3", usage = "s3 option")
        private boolean shouldUploadToS3 = false;

        @Option(name = "--s3bucket", usage = "s3 bucket name")
        private String s3BucketName = "hiro116s.s3bucket.jp";

        @Option(name = "--s3KeyName", usage = "s3 key name")
        private String s3KeyName;

        @Option(name = "--commandTemplate",
                usage = "Command template which you want to use for simulation.  A template array must include at least $SEED value",
                handler = CommandTemplateOptionHandler.class,
                required = true)
        private CommandTemplate commandTemplate;

        @Option(name = "--commandDirectory", usage = "Directory on which you want to run the simulation", handler = FileOptionHandler.class)
        private File directory = null;

        public void validate() {
            if (shouldUploadToS3) {
                Preconditions.checkArgument(s3BucketName != null && s3KeyName != null, "--s3KeyName and --s3Bucket option must be set with --s3 option");
            }
        }
    }
}
