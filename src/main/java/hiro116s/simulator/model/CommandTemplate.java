package hiro116s.simulator.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class CommandTemplate {
    public abstract List<String> commandTemplate();

    @Value.Check
    public void validate() {
        Preconditions.checkArgument(commandTemplate().stream().anyMatch(s -> s.contains(PlaceHolder.SEED.name)), "At least one parameter must include " + PlaceHolder.SEED.name + ", " + commandTemplate());
    }

    public List<String> build(final long seed) {
        final ImmutableList.Builder<String> buidler = ImmutableList.builder();
        for (int i = 0; i < commandTemplate().size(); i++) {
            if (commandTemplate().get(i).equals("<")) {
                break;
            }
            buidler.add(commandTemplate().get(i));
        }
        return buidler.build().stream()
                .map(s -> convertArg(s, seed))
                .collect(Collectors.toList());
    }

    public Optional<String> inRedirectFilePathOrEmpty(final long seed) {
        for (int i = 0; i < commandTemplate().size() - 1; i++) {
            if (commandTemplate().get(i).equals("<")) {
                return Optional.of(convertArg(commandTemplate().get(i + 1), seed));
            }
        }
        return Optional.empty();
    }

    private static String convertArg(final String s, final long seed) {
        if (s.contains(PlaceHolder.SEED.name)) {
            return s.replace(PlaceHolder.SEED.name, Long.toString(seed));
        } else {
            return s;
        }
    }

    private enum PlaceHolder {
        SEED("$SEED");

        private final String name;

        PlaceHolder(String name) {
            this.name = name;
        }
    }
}
