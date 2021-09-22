package hiro116s.simulator.model;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Dataset {
    final List<EvaluationResults> resultsAll;
    final Map<Long, Double> maxScoresBySeed;

    private Dataset(List<EvaluationResults> resultsAll, Map<Long, Double> maxScoresBySeed) {
        this.resultsAll = resultsAll;
        this.maxScoresBySeed = maxScoresBySeed;
    }

    public static Dataset create(List<EvaluationResults> resultsAll) {
        final Map<Long, Double> maxScoresBySeed = new HashMap<>();
        for (final EvaluationResults results : resultsAll) {
            for (final Result result : results.results) {
                maxScoresBySeed.put(result.seed, Math.max(maxScoresBySeed.getOrDefault(result.seed, Double.MIN_VALUE), result.parsedData.score));
            }
        }
        return new Dataset(resultsAll, maxScoresBySeed);
    }

    public Dataset groupBy(final String key) {
        return new Dataset(
                resultsAll.stream()
                        .flatMap(r -> r.groupBy(key))
                        .collect(Collectors.toList()),
                maxScoresBySeed
        );
    }

    public Dataset groupBy(final String... keys) {
        Dataset cur = this;
        for (final String key : keys) {
            cur = cur.groupBy(key);
        }
        return cur;
    }

    public String format() {
        final List<EvaluationResults> sortedResults = new ArrayList<>(resultsAll);
        sortedResults.sort(Comparator.comparingDouble(r -> -r.evalScore(maxScoresBySeed)));
        final StringBuilder res = new StringBuilder();
        res.append(String.format("Max score: %7f\n", resultsAll.get(0).results.stream().map(r -> r.parsedData.maxScore).reduce(0.0, Double::sum)));
        res.append(sortedResults.stream()
                .map(results -> String.format("%s %s %7f", results.filePath, results.params.entrySet(), results.evalScore(maxScoresBySeed)))
                .collect(Collectors.joining("\n")));
        return res.toString();
    }
}
