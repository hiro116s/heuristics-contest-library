package hiro116s.simulator.dataset.writer;

import hiro116s.simulator.model.SimulationResults;

import java.io.IOException;
import java.util.Collection;

public class CompositeSimulationResultsWriter implements SimulationResultsWriter {
    private final Collection<SimulationResultsWriter> writers;

    public CompositeSimulationResultsWriter(Collection<SimulationResultsWriter> writers) {
        this.writers = writers;
    }

    @Override
    public void write(SimulationResults simulationResults) throws IOException {
        for (final SimulationResultsWriter writer : writers) {
            writer.write(simulationResults);
        }
    }
}
