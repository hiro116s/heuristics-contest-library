package hiro116s.simulator.dataset.fetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import hiro116s.simulator.model.Dataset;
import hiro116s.simulator.model.Result;
import hiro116s.simulator.model.EvaluationResults;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileDatasetFetcher implements DatasetFetcher {
    private static final TypeReference<Result> TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            builder.add(new EvaluationResults(file.getName(), readResults(file)));
        }
        return Dataset.create(builder.build());
    }

    List<Result> readResults(final File file) throws IOException {
        final List<Result> result = new ArrayList<>();
        try (final FileInputStream fs = new FileInputStream(file);
             final BufferedReader br = new BufferedReader(new InputStreamReader(fs))) {
            String line = br.readLine();
            while (line != null) {
                result.add(OBJECT_MAPPER.readValue(line, TYPE_REFERENCE));
                line = br.readLine();
            }
        }
        return result;
    }

}
