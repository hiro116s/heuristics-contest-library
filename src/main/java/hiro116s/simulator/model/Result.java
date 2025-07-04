package hiro116s.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = {"errString"}, ignoreUnknown=true)
public class Result {
    public long seed;
    public String simulationId;
    public ParsedData parsedData;
    public String errString;

    public Result() {
    }

    public Result(long seed, String simulationId, ParsedData parsedData) {
        this(seed, simulationId, parsedData, "");
    }

    public Result(long seed, String simulationId, ParsedData parsedData, String errString) {
        this.seed = seed;
        this.simulationId = simulationId;
        this.parsedData = parsedData;
        this.errString = errString;
    }

    @Override
    public String toString() {
        return "Result{" +
                "seed=" + seed +
                ", simulationId='" + simulationId + '\'' +
                ", parsedData=" + parsedData +
                ", errString='" + errString + '\'' +
                '}';
    }
}
