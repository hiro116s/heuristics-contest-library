package hiro116s.simulator.model;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvaluationResults {
    final String filePath;
    final List<Result> results;
    final Map<String, Object> params;

    public EvaluationResults(String filePath, List<Result> results) {
        this(filePath, results, Collections.emptyMap());
    }

    EvaluationResults(String filePath, List<Result> results, Map<String, Object> params) {
        this.filePath = filePath;
        this.results = results;
        this.params = params;
    }

    public Stream<EvaluationResults> groupBy(final String key) {
        return results.stream()
                .collect(Collectors.groupingBy(r -> r.parsedData.params.getOrDefault(key, "N/A")))
                .entrySet()
                .stream()
                .map(e -> new EvaluationResults(filePath, e.getValue(), ImmutableMap.<String, Object>builder()
                        .putAll(params)
                        .put(key, e.getKey())
                        .build()));
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
            res += result.parsedData.score;// / maxScoresBySeed.get(result.seed);
        }
        return res;
    }
}
