package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ParsedData {
    public static final double NO_FIELD = -1.0;

    public double score;
    public double maxScore;
    public double ratio;

    public ParsedData() {
    }

    public ParsedData(double score, double maxScore) {
        this.score = score;
        this.maxScore = maxScore;
        this.ratio = maxScore == NO_FIELD ? NO_FIELD : 100. * (score / maxScore);
    }

    @Override
    public String toString() {
        return "ParsedData{" +
                "score=" + score +
                ", maxScore=" + maxScore +
                ", ratio=" + ratio +
                '}';
    }
}