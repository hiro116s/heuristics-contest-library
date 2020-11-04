package hiro116s.simulator.model;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dataset {
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
