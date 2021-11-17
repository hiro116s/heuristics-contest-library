package hiro116s.simulator.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationResultsTest {
    @Test
    void toJsonString() throws Exception {
        final SimulationResults simulationResults = new SimulationResults(ImmutableList.of(
                new Result(1L, "a", new ParsedData(1.0, 1.0, ImmutableMap.of(
                        "N", 1,
                        "M", 1,
                        "X", 10
                ))),
                new Result(2L, "b", new ParsedData(10.0, 10.0, ImmutableMap.of(
                        "N", 1,
                        "M", 2
                ))),
                new Result(3L, "c", new ParsedData(100.0, 100.0, ImmutableMap.of(
                        "N", 2,
                        "M", 2
                )))
        ));
        assertEquals(
                "{\"seed\":1,\"simulationId\":\"a\",\"parsedData\":{\"score\":1.0,\"maxScore\":1.0,\"ratio\":100.0,\"params\":{\"N\":1,\"M\":1,\"X\":10}}}\n" +
                        "{\"seed\":2,\"simulationId\":\"b\",\"parsedData\":{\"score\":10.0,\"maxScore\":10.0,\"ratio\":100.0,\"params\":{\"N\":1,\"M\":2}}}\n" +
                        "{\"seed\":3,\"simulationId\":\"c\",\"parsedData\":{\"score\":100.0,\"maxScore\":100.0,\"ratio\":100.0,\"params\":{\"N\":2,\"M\":2}}}",
                simulationResults.toJsonString());
    }
}