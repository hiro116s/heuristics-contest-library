package hiro116s.simulator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

public class SimulationResults {
    private final List<Result> results;

    public SimulationResults(List<Result> results) {
        this.results = results;
    }

    public void merge(final SimulationResults simulationResults) {
        this.results.addAll(simulationResults.results);
    }

    public List<Result> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "SimulationResults{" +
                "results=" + results +
                '}';
    }

    public String toJsonString() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append(objectMapper.writeValueAsString(results.get(i)));
            if (i != results.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
