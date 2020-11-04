package hiro116s.simulator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class SimulationResults {
    private final List<Result> results;

    public SimulationResults(List<Result> results) {
        this.results = results;
    }

    public void merge(final SimulationResults simulationResults) {
        this.results.addAll(simulationResults.results);
    }

    @Override
    public String toString() {
        return "SimulationResults{" +
                "results=" + results +
                '}';
    }

    public String toJsonString() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
    }
}
