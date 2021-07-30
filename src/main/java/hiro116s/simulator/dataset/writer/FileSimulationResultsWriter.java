package hiro116s.simulator.dataset.writer;

import hiro116s.simulator.model.SimulationResults;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileSimulationResultsWriter implements SimulationResultsWriter {
    private final String logFilePath;

    public FileSimulationResultsWriter(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    @Override
    public void write(SimulationResults simulationResults) throws IOException {
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath))) {
            bw.write(simulationResults.toJsonString());
        }
    }
}
