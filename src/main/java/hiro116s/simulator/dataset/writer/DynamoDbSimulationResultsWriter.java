package hiro116s.simulator.dataset.writer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hiro116s.simulator.model.SimulationResults;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.io.IOException;
import java.util.stream.Collectors;

public class DynamoDbSimulationResultsWriter implements SimulationResultsWriter {
    private static final Gson gson = new GsonBuilder().create();

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String contestName;
    private final String currentTimeRaw;
    private final String gitCommitHash;

    public DynamoDbSimulationResultsWriter(DynamoDbClient dynamoDbClient, String tableName, String contestName, String currentTimeRaw, String gitCommitHash) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.contestName = contestName;
        this.currentTimeRaw = currentTimeRaw;
        this.gitCommitHash = gitCommitHash;
    }

    @Override
    public void write(SimulationResults simulationResults) {
        dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(ImmutableMap.of(
                        tableName, simulationResults.getResults().stream()
                                .map(r -> WriteRequest.builder()
                                        .putRequest(PutRequest.builder()
                                                .item(ImmutableMap.of(
                                                        "contest_name", AttributeValue.builder().s(contestName).build(),
                                                        "evaluation_id", AttributeValue.builder().s(String.format("%s#%s#%d", currentTimeRaw, gitCommitHash, r.seed)).build(),
                                                        "data", AttributeValue.builder().s(gson.toJson(r.parsedData)).build()))
                                                .build())
                                        .build())
                                .collect(Collectors.toList())))
                .build());

    }
}
