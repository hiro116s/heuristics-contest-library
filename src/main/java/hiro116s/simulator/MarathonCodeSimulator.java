package hiro116s.simulator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import hiro116s.simulator.dataset.writer.CompositeSimulationResultsWriter;
import hiro116s.simulator.dataset.writer.DynamoDbSimulationResultsWriter;
import hiro116s.simulator.dataset.writer.FileSimulationResultsWriter;
import hiro116s.simulator.dataset.writer.S3SimulationResultsWriter;
import hiro116s.simulator.dataset.writer.SimulationResultsWriter;
import hiro116s.simulator.lineprocessor.OutputLineProcessor;
import hiro116s.simulator.model.CommandTemplate;
import hiro116s.simulator.model.SimulationResults;
import hiro116s.simulator.option.CommandTemplateOptionHandler;
import hiro116s.simulator.simulator.CommandLineSimulator;
import hiro116s.simulator.simulator.ConcurrentCommandLineSimulator;
import hiro116s.simulator.simulator.Simulator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FileOptionHandler;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Tool to evaluate the score in the heuristic contest.
 */
public class MarathonCodeSimulator {
    private static final String CURRENT_TIME_RAW = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    static final String DYNAMO_DB_TABLE_NAME = "contest_scores";

    private final Simulator simulator;
    private final SimulationResultsWriter simulationResultsWriter;

    public MarathonCodeSimulator(Simulator simulator, SimulationResultsWriter simulationResultsWriter) {
        this.simulator = simulator;
        this.simulationResultsWriter = simulationResultsWriter;
    }

    public static void main(String[] args) throws IOException {
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
                        seed -> new CommandLineSimulator(seed, arguments.commandTemplate, arguments.stdoutDir, new OutputLineProcessor(arguments.debugMode), arguments.directory, arguments.getSimulationId(), Duration.ofMillis(arguments.timeoutMs))),
                createSimulationResultsWriter(arguments)
        ).run();
    }

    private static SimulationResultsWriter createSimulationResultsWriter(final Arguments arguments) {
        final String gitCommitHash = arguments.getGitCommitHash();
        final String logFileName = arguments.getSimulationId();
        final String logFilePath = String.format("%s/%s",
                arguments.logOutputDir.getPath(),
                logFileName
        );
        final ImmutableList.Builder<SimulationResultsWriter> builder = ImmutableList.<SimulationResultsWriter>builder()
                .add(new FileSimulationResultsWriter(logFilePath));
        if (arguments.shouldUploadToS3) {
            builder.add(new S3SimulationResultsWriter(
                    AmazonS3ClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_1).build(),
                    arguments.s3BucketName,
                    String.format("%s/log/%s", arguments.contestName, logFileName)
            ));
        }
        if (arguments.dynamoDbUpdateType != DynamoDbUpdateType.NONE) {
            builder.add(new DynamoDbSimulationResultsWriter(arguments.dynamoDBClient(), arguments.dynamoDbTableName, arguments.contestName, CURRENT_TIME_RAW, gitCommitHash));
        }
        return new CompositeSimulationResultsWriter(builder.build());
    }

    private void run() throws IOException {
        final SimulationResults results = simulator.simulate();
        simulationResultsWriter.write(results);
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

        @Option(name = "--stdoutDir", usage = "standard output directory", handler = FileOptionHandler.class)
        private File stdoutDir = new File("./stdout");

        @Option(name = "--additionalNote", usage = "additional note for file name")
        private String additionalNote = "";

        @Option(name = "--gitCommitHash", usage = "Git commit hash used for file name of log output")
        private String gitCommitHash = null;

        @Option(name = "--timeout", usage = "Timeout value (milliseconds)")
        public long timeoutMs = Long.MAX_VALUE;

        private String getGitCommitHash() {
            if (gitCommitHash != null) {
                return gitCommitHash;
            }
            try {
                // TODO: Use JGit library
                final Runtime runtime = Runtime.getRuntime();
                try (final InputStreamReader inputStreamReader = new InputStreamReader(runtime.exec("git rev-parse HEAD").getInputStream())) {
                    return CharStreams.readLines(inputStreamReader).get(0).substring(0, 6);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Option(name = "--s3", usage = "s3 option")
        private boolean shouldUploadToS3 = false;

        @Option(name = "--s3bucket", usage = "s3 bucket name")
        private String s3BucketName = "hiro116s.s3bucket.jp";

        @Option(name = "--contestName", usage = "contest name")
        private String contestName = null;

        @Option(name = "--commandTemplate",
                usage = "Command template which you want to use for simulation.  A template array must include at least $SEED value",
                handler = CommandTemplateOptionHandler.class,
                required = true)
        private CommandTemplate commandTemplate;

        @Option(name = "--commandDirectory", usage = "Directory on which you want to run the simulation", handler = FileOptionHandler.class)
        private File directory = null;

        @Option(name = "--debugMode", usage = "debug mode to output all standard error output")
        private boolean debugMode = false;

        public void validate() {
            if (shouldUploadToS3) {
                Preconditions.checkArgument(s3BucketName != null && contestName != null, "--s3KeyName and --s3Bucket option must be set with --s3 option");
            }
            if (dynamoDbUpdateType != DynamoDbUpdateType.NONE) {
                Preconditions.checkArgument(contestName != null, "contestName option must be set with --dynamo {LOCAL,PRODUCTION} option");
            }
        }

        @Option(name = "--dynamo",
                usage = "NONE: DynamoDB won't be updated\n" +
                        "LOCAL: Lolcal DynamoDB ('http://localhost:8000') will be accessed\n" +
                        "PRODUCTION: DyanmoDB will be accessed using default aws client config defined in ~/.aws/config",
                handler = DynamoDbUpdateTypeOptionHandler.class)
        private DynamoDbUpdateType dynamoDbUpdateType = DynamoDbUpdateType.NONE;

        @Option(name = "--dynamoDbTableName", usage = "dynamoDB table name")
        private String dynamoDbTableName = DYNAMO_DB_TABLE_NAME;

        public DynamoDbClient dynamoDBClient() {
            if (dynamoDbUpdateType == DynamoDbUpdateType.LOCAL) {
                return DynamoDbClient.builder()
                        .httpClientBuilder(ApacheHttpClient.builder())
                        .endpointOverride(URI.create("http://localhost:8000/"))
                        .build();
            } else if (dynamoDbUpdateType == DynamoDbUpdateType.PRODUCTION) {
                return DynamoDbClient.create();
            } else {
                throw new IllegalArgumentException("DynamoDbUpdateType is NONE, so dynamoDBClient can't be instantiated.");
            }
        }

        public String getSimulationId() {
            return String.format("%s-%s%s.log",
                    CURRENT_TIME_RAW,
                    getGitCommitHash(),
                    additionalNote
            );
        }
    }

    private enum DynamoDbUpdateType {
        NONE,
        LOCAL,
        PRODUCTION
    }

    public static class DynamoDbUpdateTypeOptionHandler extends OneArgumentOptionHandler<DynamoDbUpdateType> {
        public DynamoDbUpdateTypeOptionHandler(CmdLineParser parser, OptionDef option, Setter<DynamoDbUpdateType> setter) {
            super(parser, option, setter);
        }

        @Override
        protected DynamoDbUpdateType parse(String argument) throws NumberFormatException {
            return DynamoDbUpdateType.valueOf(argument);
        }
    }
}
