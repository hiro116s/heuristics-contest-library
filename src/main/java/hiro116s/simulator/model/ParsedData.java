package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ParsedData {
    public static final ParsedData TIMEOUT_DATA = new ParsedData(-1, Status.TIMEOUT, ImmutableMap.of());

    public static final double NO_FIELD = -1.0;

    public double score;
    public Status status;
    public Map<String, Object> params;

    public ParsedData() {
    }

    public ParsedData(double score, Status status, Map<String, Object> params) {
        this.score = score;
        this.status = status;
        this.params = params;
    }

    @Override
    public String toString() {
        return "ParsedData{" +
                "score=" + score +
                ", status=" + status +
                ", params=" + params +
                '}';
    }

    public enum Status {
        OK,
        NO_SCORE_STATEMENT,
        TIMEOUT
    }
}