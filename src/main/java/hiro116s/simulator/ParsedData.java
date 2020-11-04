package hiro116s.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ParsedData {
    public double score;
    public double maxScore;
    public double ratio;

    public ParsedData() {
    }

    public ParsedData(double score, double maxScore) {
        this.score = score;
        this.maxScore = maxScore;
        this.ratio = 100. * (score / maxScore);
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