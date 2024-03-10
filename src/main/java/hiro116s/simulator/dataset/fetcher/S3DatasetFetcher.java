package hiro116s.simulator.dataset.fetcher;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import hiro116s.simulator.dataset.writer.FileSimulationResultsWriter;
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.EvaluationResults;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.SimulationResults;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class S3DatasetFetcher implements DatasetFetcher {
    private static final String s3CachePath = "./.marathon-code-evaluator/cache";
    private static final TypeReference<Result> TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String s3BucketName;
    private final String s3KeyPrefix;
    private final boolean s3CacheEnabled;

    public S3DatasetFetcher(String s3BucketName, String s3KeyPrefix, boolean s3CacheEnabled) {
        this.s3BucketName = s3BucketName;
        this.s3KeyPrefix = s3KeyPrefix;
        this.s3CacheEnabled = s3CacheEnabled;
    }

    @Override
    public Dataset fetch() throws IOException {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();
        if (!s3.doesBucketExistV2(s3BucketName)) {
            throw new AmazonS3Exception("No such bucket: " + s3BucketName);
        }
        final List<S3ObjectSummary> objectSummaries = s3.listObjectsV2(s3BucketName, s3KeyPrefix + "/log").getObjectSummaries();
        if (s3CacheEnabled) {
            try {
                Files.createDirectories(Path.of(s3CachePath, s3KeyPrefix, "log"));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create required directories", e);
            }
        }
        final ImmutableList.Builder<EvaluationResults> builder = ImmutableList.builder();

        for (final S3ObjectSummary objectSummary : objectSummaries) {
            if (objectSummary.getKey().endsWith("/")) {
                // Skip type=directory
                continue;
            }
            final List<Result> rawResults;
            if (s3CacheEnabled) {
                final File file = new File(s3CachePath + "/" + objectSummary.getKey());
                if (file.exists()) {
                    rawResults = new FileDatasetFetcher(s3CachePath).readResults(file);
                } else {
                    rawResults = readResultsFromS3(s3, objectSummary.getKey());
                    final FileSimulationResultsWriter fileSimulationResultsWriter = new FileSimulationResultsWriter(file.getPath());
                    fileSimulationResultsWriter.write(new SimulationResults(rawResults));
                }
            } else {
                rawResults = readResultsFromS3(s3, objectSummary.getKey());
            }
            final EvaluationResults results = new EvaluationResults(getFilePath(objectSummary.getKey()), rawResults);
            builder.add(results);
        }
        return Dataset.create(builder.build());
    }

    private List<Result> readResultsFromS3(final AmazonS3 s3, String key) throws IOException {
        final List<Result> result = new ArrayList<>();
        try (final S3ObjectInputStream is = s3.getObject(s3BucketName, key).getObjectContent();
             final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line = br.readLine();
            while (line != null) {
                result.add(OBJECT_MAPPER.readValue(line, TYPE_REFERENCE));
                line = br.readLine();
            }
        }
        return result;
    }

    private String getFilePath(String key) {
        final String[] ws = key.split("/");
        return ws[ws.length - 1];
    }
}