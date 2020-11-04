package hiro116s.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
