package hiro116s.simulator.model;

import java.util.List;
import java.util.Map;

public class EvaluationResults {
    final String filePath;
    final List<Result> results;

    public EvaluationResults(String filePath, List<Result> results) {
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
            res += result.parsedData.score;// / maxScoresBySeed.get(result.seed);
        }
        return res;
    }
}
