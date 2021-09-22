package hiro116s.simulator.dataset.fetcher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.EvaluationResults;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.Result;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamoDbDatasetFetcher implements DatasetFetcher {
    private static final Gson gson = new GsonBuilder().create();

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String contestName;

    public DynamoDbDatasetFetcher(DynamoDbClient dynamoDbClient, String tableName, String contestName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.contestName = contestName;
    }

    @Override
    public Dataset fetch() throws IOException {
        final QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("contest_name = :contest_name")
                .expressionAttributeValues(
                        ImmutableMap.of(":contest_name", AttributeValue.builder().s(contestName).build()))
                .build());
        final ImmutableList.Builder<EvaluationResults> builder = ImmutableList.builder();
        final Map<String, List<Map<String, AttributeValue>>> itemsByEvaluationId = response.items().stream()
                .collect(Collectors.groupingBy(map -> map.get("evaluation_id").s().split("#")[0]));
        itemsByEvaluationId.forEach((evaluationId, maps) ->
                builder.add(new EvaluationResults(evaluationId, maps.stream()
                        .map(m -> new Result(
                                Long.parseLong(m.get("evaluation_id").s().split("#")[2]),
                                gson.fromJson(m.get("data").s(), ParsedData.class)))
                        .collect(Collectors.toList())))
        );
        return Dataset.create(builder.build());
    }

    private EvaluationResults toEvaluationResults(final Map<String, AttributeValue> map) {
        // final String filePath = map.get("evaluation_id");
        // EvaluationResults evaluationResults = new EvaluationResults();
        return null;
    }
}
