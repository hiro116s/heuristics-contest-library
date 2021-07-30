package hiro116s.simulator.dataset.writer;

import hiro116s.simulator.model.SimulationResults;

import java.io.IOException;

public interface SimulationResultsWriter {
    void write(final SimulationResults simulationResults) throws IOException;
}
