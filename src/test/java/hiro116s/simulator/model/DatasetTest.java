package hiro116s.simulator.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hiro116s.simulator.model.ParsedData.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetTest {
    @Test
    void test() {
        Dataset dataset = Dataset.create(ImmutableList.of(
                new EvaluationResults("path1", ImmutableList.of(
                        new Result(1L, "a", new ParsedData(1.0, Status.OK, ImmutableMap.of(
                                "N", 1,
                                "M", 1,
                                "X", 10
                        ))),
                        new Result(2L, "b", new ParsedData(10.0, Status.OK, ImmutableMap.of(
                                "N", 1,
                                "M", 2
                        ))),
                        new Result(3L, "c", new ParsedData(100.0, Status.OK, ImmutableMap.of(
                                "N", 2,
                                "M", 2
                        )))
                ))
        ));
        assertEquals(
                "path1 [N=2] 100.000000\n" +
                "path1 [N=1] 11.000000", dataset.groupBy("N").format());
        assertEquals(
                "path1 [M=2] 110.000000\n" +
                "path1 [M=1] 1.000000", dataset.groupBy("M").format());
        assertEquals(
                "path1 [X=N/A] 110.000000\n" +
                "path1 [X=10] 1.000000", dataset.groupBy("X").format());
        assertEquals(
                "path1 [N=2, M=2] 100.000000\n" +
                "path1 [N=1, M=2] 10.000000\n" +
                "path1 [N=1, M=1] 1.000000", dataset.groupBy("N", "M").format());
    }
}