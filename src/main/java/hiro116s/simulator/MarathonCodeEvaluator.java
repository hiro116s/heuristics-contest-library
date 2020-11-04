package hiro116s.simulator;

import hiro116s.simulator.dataset.fetcher.DatasetFetcher;
import hiro116s.simulator.dataset.fetcher.FileDatasetFetcher;
import hiro116s.simulator.dataset.fetcher.S3DatasetFetcher;
import hiro116s.simulator.model.Dataset;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

public class MarathonCodeEvaluator {
    private final Arguments arguments;
    private final DatasetFetcher datasetFetcher;

    public MarathonCodeEvaluator(Arguments arguments, DatasetFetcher datasetFetcher) {
        this.arguments = arguments;
        this.datasetFetcher = datasetFetcher;
    }

    public static void main(String[] args) throws IOException {
        final Arguments arguments = parseArgs(args);
        final DatasetFetcher datasetFetcher = arguments.readFromS3 ?
                new S3DatasetFetcher(arguments.s3BucketName, arguments.s3KeyPrefix, arguments.showOnlyFilename) :
                new FileDatasetFetcher(arguments.logInputDir, arguments.showOnlyFilename);
        new MarathonCodeEvaluator(arguments, datasetFetcher).run();
    }

    private void run() throws IOException {
        final Dataset dataset = datasetFetcher.fetch();
        dataset.show();
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
        @Option(name = "--logInputDir")
        private String logInputDir = "./src/main/java/log";

        @Option(name = "--show-only-filename")
        private boolean showOnlyFilename = true;

        @Option(name = "--s3", usage = "s3 option")
        private boolean readFromS3 = true;

        @Option(name = "--s3bucket", usage = "s3 bucket name")
        private String s3BucketName = "hiro116s.s3bucket.jp";

        @Option(name = "--s3prefix", usage = "s3 key prefix")
        private String s3KeyPrefix = "SoccerTournament/log";
    }
}
