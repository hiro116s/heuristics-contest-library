package hiro116s.simulator.model;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class CommandTemplate {
    public abstract List<String> commandTemplate();

    @Value.Check
    public void validate() {
        Preconditions.checkArgument(commandTemplate().stream().anyMatch(PlaceHolder.SEED.name::equals), "At least one parameter must be " + PlaceHolder.SEED.name);
    }

    public List<String> build(final long seed) {
        return commandTemplate().stream()
                .map(s -> {
                    if (PlaceHolder.SEED.name.equals(s)) {
                        return Long.toString(seed);
                    } else {
                        return s;
                    }
                }).collect(Collectors.toList());
    }

    private enum PlaceHolder {
        SEED("$SEED");

        private final String name;

        PlaceHolder(String name) {
            this.name = name;
        }
    }
}
