package hiro116s.simulator.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hiro116s.simulator.model.ParsedData.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationResultsTest {
    @Test
    void toJsonString() throws Exception {
        final SimulationResults simulationResults = new SimulationResults(ImmutableList.of(
                new Result(1L, "a", new ParsedData(1.0, Status.OK, ImmutableMap.of(
                        "N", 1,
                        "M", 1,
                        "X", 10
                ))),
                new Result(2L, "b", new ParsedData(-1, Status.NO_SCORE_STATEMENT, ImmutableMap.of(
                        "N", 1,
                        "M", 2
                ))),
                new Result(3L, "c", new ParsedData(100.0, Status.OK, ImmutableMap.of(
                        "N", 2,
                        "M", 2
                ))),
                new Result(4L, "d", ParsedData.TIMEOUT_DATA)
        ));
        assertEquals(
                "{\"seed\":1,\"simulationId\":\"a\",\"parsedData\":{\"score\":1.0,\"status\":\"OK\",\"params\":{\"N\":1,\"M\":1,\"X\":10}}}\n" +
                        "{\"seed\":2,\"simulationId\":\"b\",\"parsedData\":{\"score\":-1.0,\"status\":\"NO_SCORE_STATEMENT\",\"params\":{\"N\":1,\"M\":2}}}\n" +
                        "{\"seed\":3,\"simulationId\":\"c\",\"parsedData\":{\"score\":100.0,\"status\":\"OK\",\"params\":{\"N\":2,\"M\":2}}}\n" +
                        "{\"seed\":4,\"simulationId\":\"d\",\"parsedData\":{\"score\":-1.0,\"status\":\"TIMEOUT\",\"params\":{}}}",
                simulationResults.toJsonString());
    }
}