package hiro116s.simulator;

import com.google.common.base.Preconditions;
import hiro116s.simulator.dataset.fetcher.DatasetFetcher;
import hiro116s.simulator.dataset.fetcher.DynamoDbDatasetFetcher;
import hiro116s.simulator.dataset.fetcher.FileDatasetFetcher;
import hiro116s.simulator.dataset.fetcher.S3DatasetFetcher;
import hiro116s.simulator.model.Dataset;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

public class MarathonCodeEvaluator {
    private final String groupByKeys;
    private final DatasetFetcher datasetFetcher;

    public MarathonCodeEvaluator(String groupByKeys, DatasetFetcher datasetFetcher) {
        this.groupByKeys = groupByKeys;
        this.datasetFetcher = datasetFetcher;
    }

    public static void main(String[] args) throws IOException {
        final Arguments arguments = parseArgs(args);
        final DatasetFetcher datasetFetcher = arguments.dataSource.toDataFetcher(arguments);
        new MarathonCodeEvaluator(arguments.groupByKeys, datasetFetcher).run();
    }

    private void run() throws IOException {
        final Dataset dataset = datasetFetcher.fetch();
        System.out.println(dataset.groupBy(groupByKeys.split(",")).format());
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

        @Option(name = "--showOnlyFilename")
        private boolean showOnlyFilename = true;

        @Option(name = "--source", usage = "One of LOCAL,S3,DYNAMO_DB", handler = DataSourceOptionHandler.class)
        private DataSource dataSource = DataSource.LOCAL;

        @Option(name = "--s3bucket", usage = "s3 bucket name")
        private String s3BucketName = null;

        @Option(name = "--s3CacheEnabled")
        private boolean s3CacheEnabled = false;

        @Option(name = "--groupByKeys", usage = "parameter keys for grouping by as comma-separated values")
        private String groupByKeys = "";

        @Option(name = "--contestName", usage = "contest name")
        private String contestName = null;

        @Option(name = "--dynamoDbTableName", usage = "dynamoDB table name")
        private String dynamoDbTableName = MarathonCodeSimulator.DYNAMO_DB_TABLE_NAME;

        public void validate() {
            if (dataSource == DataSource.LOCAL) {
                Preconditions.checkArgument(logInputDir != null);
            } else if (dataSource == DataSource.S3) {
                Preconditions.checkArgument(s3BucketName != null && contestName != null);
            } else if (dataSource == DataSource.DYNAMO_DB_PROD) {
                Preconditions.checkArgument(contestName != null);
            } else {
                throw new IllegalArgumentException();
            }
        }

        public DynamoDbClient dynamoDBClient() {
            if (dataSource == DataSource.DYNAMO_DB_LOCAL) {
                return DynamoDbClient.builder()
                        .httpClientBuilder(ApacheHttpClient.builder())
                        .endpointOverride(URI.create("http://localhost:8000/"))
                        .build();
            } else if (dataSource == DataSource.DYNAMO_DB_PROD) {
                return DynamoDbClient.create();
            } else {
                throw new IllegalArgumentException("DataSource must be one of DYNAMODB_PROD or DYNAMO_DB_LOCA to instantiate dynamoDbClient");
            }
        }
    }

    private enum DataSource {
        LOCAL(arguments -> new FileDatasetFetcher(arguments.logInputDir)),
        S3(arguments -> new S3DatasetFetcher(arguments.s3BucketName, arguments.contestName, arguments.s3CacheEnabled)),
        DYNAMO_DB_PROD(arguments -> new DynamoDbDatasetFetcher(arguments.dynamoDBClient(), arguments.dynamoDbTableName, arguments.contestName)),
        DYNAMO_DB_LOCAL(arguments -> new DynamoDbDatasetFetcher(arguments.dynamoDBClient(), arguments.dynamoDbTableName, arguments.contestName));

        private final Function<Arguments, DatasetFetcher> converter;

        DataSource(Function<Arguments, DatasetFetcher> converter) {
            this.converter = converter;
        }

        public DatasetFetcher toDataFetcher(Arguments arguments) {
            return converter.apply(arguments);
        }
    }

    public static class DataSourceOptionHandler extends OneArgumentOptionHandler<DataSource> {
        public DataSourceOptionHandler(CmdLineParser parser, OptionDef option, Setter<DataSource> setter) {
            super(parser, option, setter);
        }

        @Override
        protected DataSource parse(String argument) throws NumberFormatException {
            return DataSource.valueOf(argument);
        }
    }
}
