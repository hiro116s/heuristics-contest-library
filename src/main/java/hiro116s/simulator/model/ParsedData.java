package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ParsedData {
    public static final ParsedData TIMEOUT_DATA = new ParsedData(-2, ImmutableMap.of());

    public static final double NO_FIELD = -1.0;

    public double score;
    public Map<String, Object> params;

    public ParsedData() {
    }

    public ParsedData(double score, Map<String, Object> params) {
        this.score = score;
        this.params = params;
    }

    @Override
    public String toString() {
        return "ParsedData{" +
                "score=" + score +
                ", params=" + params +
                '}';
    }
}