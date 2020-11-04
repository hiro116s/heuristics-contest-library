package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hiro116s.simulator.ParsedData;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Result {
    public long seed;
    public ParsedData parsedData;

    public Result() {
    }

    public Result(long seed, ParsedData parsedData) {
        this.seed = seed;
        this.parsedData = parsedData;
    }

    @Override
    public String toString() {
        return "Result{" +
                "seed=" + seed +
                ", parsedData=" + parsedData +
                '}';
    }
}
