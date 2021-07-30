package hiro116s.simulator.dataset.writer;

import com.amazonaws.services.s3.AmazonS3;
import hiro116s.simulator.model.SimulationResults;

import java.io.IOException;

public class S3SimulationResultsWriter implements SimulationResultsWriter {
    private final AmazonS3 s3;
    private final String s3BucketName;
    private final String s3Key;

    public S3SimulationResultsWriter(AmazonS3 s3, String s3BucketName, String s3Key) {
        this.s3 = s3;
        this.s3BucketName = s3BucketName;
        this.s3Key = s3Key;
    }

    @Override
    public void write(SimulationResults simulationResults) throws IOException {
        if (!s3.doesBucketExistV2(s3BucketName)) {
            s3.createBucket(s3BucketName);
        }
        s3.putObject(s3BucketName, s3Key, simulationResults.toJsonString());
    }
}
