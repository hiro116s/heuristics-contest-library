package hiro116s.simulator.dataset.fetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.Results;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileDatasetFetcher implements DatasetFetcher {
    private static final TypeReference<List<Result>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final String logInputDir;
    private final boolean shouldShowOnlyFileName;

    public FileDatasetFetcher(String logInputDir, boolean shouldShowOnlyFileName) {
        this.logInputDir = logInputDir;
        this.shouldShowOnlyFileName = shouldShowOnlyFileName;
    }

    @Override
    public Dataset fetch() throws IOException {
        final File rootLogDir = new File(logInputDir);
        final ImmutableList.Builder<Results> builder = ImmutableList.builder();
        for (final File file : Files.fileTraverser().breadthFirst(rootLogDir)) {
            if (file.isDirectory()) {
                continue;
            }
            final Results results = new Results(
                    shouldShowOnlyFileName ? file.getName() : file.getPath(),
                    new ObjectMapper().readValue(file, TYPE_REFERENCE)
            );
            builder.add(results);
        }
        return new Dataset(builder.build());
    }
}
