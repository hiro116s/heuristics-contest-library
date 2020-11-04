package hiro116s.simulator.dataset.fetcher;

import hiro116s.simulator.model.Dataset;

import java.io.IOException;

public interface DatasetFetcher {
    Dataset fetch() throws IOException;
}