package hiro116s.simulator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MarathonCodeEvaluator {
    private static final TypeReference<List<Result>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final Arguments arguments;
    private final DatasetFetcher datasetFetcher;

    public MarathonCodeEvaluator(Arguments arguments, DatasetFetcher datasetFetcher) {
        this.arguments = arguments;
        this.datasetFetcher = datasetFetcher;
    }

    public static void main(String[] args) throws IOException {
        final Arguments arguments = parseArgs(args);
        final DatasetFetcher datasetFetcher = arguments.readFromS3 ? new S3DatasetFetcher(arguments) : new FileDatasetFetcher(arguments);
        new MarathonCodeEvaluator(arguments, datasetFetcher).run();
    }

    private void run() throws IOException {
        final Dataset dataset = datasetFetcher.fetch();
        dataset.show();
    }

    private static class Dataset {
        final List<Results> resultsAll;

        public Dataset(List<Results> resultsAll) {
            this.resultsAll = resultsAll;
        }

        public void show() {
            final Map<Long, Double> maxScoresBySeed = new HashMap<>();
            for (final Results results : resultsAll) {
                for (final Result result : results.results) {
                    maxScoresBySeed.put(result.seed, Math.max(maxScoresBySeed.getOrDefault(result.seed, Double.MIN_VALUE), result.parsedData.score));
                }
            }
            List<Results> sortedResults = new ArrayList<>(resultsAll);
            sortedResults.sort(Comparator.comparingDouble(r -> -r.evalScore(maxScoresBySeed)));
            for (Results results : sortedResults) {
                System.out.println(results.filePath + " " + results.evalScore(maxScoresBySeed));
            }
        }
    }

    private static class Results {
        final String filePath;
        final List<Result> results;

        public Results(String filePath, List<Result> results) {
            this.filePath = filePath;
            this.results = results;
        }

        @Override
        public String toString() {
            return "Results{" +
                    "filePath='" + filePath + '\'' +
                    ", results=" + results +
                    '}';
        }

        public double evalScore(Map<Long, Double> maxScoresBySeed) {
            double res = 0;
            for (final Result result : results) {
                res += result.parsedData.score / maxScoresBySeed.get(result.seed);
            }
            return res;
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

    private interface DatasetFetcher {
        Dataset fetch() throws IOException;
    }

    private static class FileDatasetFetcher implements DatasetFetcher {
        private final Arguments arguments;

        public FileDatasetFetcher(Arguments arguments) {
            this.arguments = arguments;
        }

        @Override
        public Dataset fetch() throws IOException {
            final File rootLogDir = new File(arguments.logInputDir);
            final ImmutableList.Builder<Results> builder = ImmutableList.builder();
            for (final File file : Files.fileTraverser().breadthFirst(rootLogDir)) {
                if (file.isDirectory()) {
                    continue;
                }
                final Results results = new Results(
                        arguments.showOnlyFilename ? file.getName() : file.getPath(),
                        new ObjectMapper().readValue(file, TYPE_REFERENCE)
                );
                builder.add(results);
            }
            return new Dataset(builder.build());
        }
    }

    private static class S3DatasetFetcher implements DatasetFetcher {
        private final Arguments arguments;

        public S3DatasetFetcher(Arguments arguments) {
            this.arguments = arguments;
        }

        @Override
        public Dataset fetch() throws IOException {
            final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.AP_NORTHEAST_1)
                    .build();
            if (!s3.doesBucketExistV2(arguments.s3BucketName)) {
                throw new AmazonS3Exception("No such bucket: " + arguments.s3BucketName);
            }
            final List<S3ObjectSummary> objectSummaries = s3.listObjectsV2(arguments.s3BucketName, arguments.s3KeyPrefix).getObjectSummaries();
            final ImmutableList.Builder<Results> builder = ImmutableList.builder();
            for (final S3ObjectSummary objectSummary : objectSummaries) {
                if (objectSummary.getKey().endsWith("/")) {
                    // Skip type=directory
                    continue;
                }
                try (final S3ObjectInputStream is = s3.getObject(arguments.s3BucketName, objectSummary.getKey()).getObjectContent();
                     final InputStreamReader isr = new InputStreamReader(is)) {
                    final Results results = new Results(getFilePath(objectSummary.getKey()), new ObjectMapper().readValue(isr, TYPE_REFERENCE));
                    builder.add(results);
                }
            }
            return new Dataset(builder.build());
        }

        private String getFilePath(String key) {
            if (arguments.showOnlyFilename) {
                final String[] ws = key.split("/");
                return ws[ws.length - 1];
            } else {
                return key;
            }
        }
    }
}
