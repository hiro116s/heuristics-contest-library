package hiro116s.simulator.dataset.fetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.EvaluationResults;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileDatasetFetcher implements DatasetFetcher {
    private static final TypeReference<List<Result>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final String logInputDir;

    public FileDatasetFetcher(String logInputDir) {
        this.logInputDir = logInputDir;
    }

    @Override
    public Dataset fetch() throws IOException {
        final File rootLogDir = new File(logInputDir);
        final ImmutableList.Builder<EvaluationResults> builder = ImmutableList.builder();
        for (final File file : Files.fileTraverser().breadthFirst(rootLogDir)) {
            if (file.isDirectory()) {
                continue;
            }
            builder.add(new EvaluationResults(file.getName(), new ObjectMapper().readValue(file, TYPE_REFERENCE)));
        }
        return Dataset.create(builder.build());
    }
}
