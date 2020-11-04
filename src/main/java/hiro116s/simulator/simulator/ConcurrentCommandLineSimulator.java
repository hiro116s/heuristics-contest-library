package hiro116s.simulator.simulator;

import hiro116s.simulator.model.SimulationResults;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentCommandLineSimulator implements Simulator, Closeable {
    private final List<Long> seeds;
    private final ExecutorService executorService;
    private final SimulatorGenerator internalSimulatorGenerator;

    public ConcurrentCommandLineSimulator(List<Long> seeds,
                                          ExecutorService executorService,
                                          SimulatorGenerator internalSimulatorGenerator) {
        this.seeds = seeds;
        this.executorService = executorService;
        this.internalSimulatorGenerator = internalSimulatorGenerator;
    }

    public static Simulator create(int numThreads, List<Long> seeds, final SimulatorGenerator simulatorGenerator) {
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        return new ConcurrentCommandLineSimulator(seeds, executorService, simulatorGenerator);
    }

    @Override
    public SimulationResults simulate() {
        final List<Future<SimulationResults>> futures = new ArrayList<>();
        seeds.forEach(seed -> futures.add(executorService.submit(() -> {
            final Simulator internal = internalSimulatorGenerator.generate(seed);
            return internal.simulate();
        })));

        final SimulationResults results = new SimulationResults(new ArrayList<>());
        try {
            for (final Future<SimulationResults> future : futures) {
                SimulationResults result = future.get();
                results.merge(result);
            }
            return results;
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    public interface SimulatorGenerator {
        Simulator generate(final long seed);
    }
}
