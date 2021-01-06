package hiro116s.simulator.model;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dataset {
    final List<EvaluationResults> resultsAll;

    public Dataset(List<EvaluationResults> resultsAll) {
        this.resultsAll = resultsAll;
    }

    public void show() {
        final Map<Long, Double> maxScoresBySeed = new HashMap<>();
        for (final EvaluationResults results : resultsAll) {
            for (final Result result : results.results) {
                maxScoresBySeed.put(result.seed, Math.max(maxScoresBySeed.getOrDefault(result.seed, Double.MIN_VALUE), result.parsedData.score));
            }
        }
        List<EvaluationResults> sortedResults = new ArrayList<>(resultsAll);
        sortedResults.sort(Comparator.comparingDouble(r -> -r.evalScore(maxScoresBySeed)));
        System.out.printf("Max score: %7f\n", resultsAll.get(0).results.stream().map(r -> r.parsedData.maxScore).reduce(0.0, Double::sum));
        for (EvaluationResults results : sortedResults) {
            System.out.printf("%s %7f\n", results.filePath, results.evalScore(maxScoresBySeed));
        }
    }
}
