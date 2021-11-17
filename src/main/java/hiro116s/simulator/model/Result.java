package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Result {
    public long seed;
    public String simulationId;
    public ParsedData parsedData;

    public Result() {
    }

    public Result(long seed, String simulationId, ParsedData parsedData) {
        this.seed = seed;
        this.simulationId = simulationId;
        this.parsedData = parsedData;
    }

    @Override
    public String toString() {
        return "Result{" +
                "seed=" + seed +
                ", simulationId='" + simulationId + '\'' +
                ", parsedData=" + parsedData +
                '}';
    }
}
