package hiro116s.simulator.lineprocessor;


import com.google.common.base.Preconditions;
import com.google.common.io.LineProcessor;
import hiro116s.simulator.model.ParsedData;
import hiro116s.simulator.model.ParsedData.Status;

import java.util.HashMap;
import java.util.Map;

import static hiro116s.simulator.model.ParsedData.NO_FIELD;

public class OutputLineProcessor implements LineProcessor<ParsedData> {
    private double score = NO_FIELD;
    private Map<String, Object> params = new HashMap<>();

    private final boolean debugMode;

    public OutputLineProcessor(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public boolean processLine(String line) {
        if (debugMode) {
            System.out.println(line);
        }
        if (line.startsWith("Score")) {
            final String[] ws = line.split(" = ");
            score = Double.parseDouble(ws[1]);
        } else if (line.startsWith("Param:")) {
            final String[] ws = line.replace("Param:", "").split(" = ");
            Preconditions.checkArgument(!params.containsKey(ws[0]));
            String key = ws[0].trim();
            String value = ws[1].trim();
            if (value.chars().allMatch(Character::isDigit)) {
                params.put(key, Long.valueOf(value));
            } else {
                params.put(key, value);
            }
        } else if (line.startsWith("LOG:")) {
            // TODO: Instead of checking prefix, it would be better to add a flag in instance variable
            System.out.println(line);
        }
        return true;
    }

    @Override
    public ParsedData getResult() {
        if (score == NO_FIELD) {
            System.out.println("No score statement");
            return new ParsedData(-1, Status.NO_SCORE_STATEMENT, params);
        } else {
            return new ParsedData(score, Status.OK, params);
        }
    }
}
