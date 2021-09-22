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
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.EvaluationResults;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class S3DatasetFetcher implements DatasetFetcher {
    private static final TypeReference<List<Result>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final String s3BucketName;
    private final String s3KeyPrefix;

    public S3DatasetFetcher(String s3BucketName, String s3KeyPrefix) {
        this.s3BucketName = s3BucketName;
        this.s3KeyPrefix = s3KeyPrefix;
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
        final ImmutableList.Builder<EvaluationResults> builder = ImmutableList.builder();
        for (final S3ObjectSummary objectSummary : objectSummaries) {
            if (objectSummary.getKey().endsWith("/")) {
                // Skip type=directory
                continue;
            }
            try (final S3ObjectInputStream is = s3.getObject(s3BucketName, objectSummary.getKey()).getObjectContent();
                 final InputStreamReader isr = new InputStreamReader(is)) {
                final EvaluationResults results = new EvaluationResults(getFilePath(objectSummary.getKey()), new ObjectMapper().readValue(isr, TYPE_REFERENCE));
                builder.add(results);
            }
        }
        return Dataset.create(builder.build());
    }

    private String getFilePath(String key) {
        final String[] ws = key.split("/");
        return ws[ws.length - 1];
    }
}