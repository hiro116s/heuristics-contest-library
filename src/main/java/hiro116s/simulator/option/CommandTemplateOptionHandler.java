package hiro116s.simulator.option;

import hiro116s.simulator.model.CommandTemplate;
import hiro116s.simulator.model.ImmutableCommandTemplate;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import java.util.ArrayList;
import java.util.List;

public class CommandTemplateOptionHandler extends OneArgumentOptionHandler<CommandTemplate> {
    public CommandTemplateOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super CommandTemplate> setter) {
        super(parser, option, setter);
    }

    @Override
    protected CommandTemplate parse(String param) throws NumberFormatException {
        final List<String> commandTemplate = new ArrayList<>();
        final char DEFAULT_END_CHAR = ' ';
        char endChar = DEFAULT_END_CHAR;
        final StringBuilder cur = new StringBuilder();
        for (int i = 0; i < param.length(); i++) {
            char ch = param.charAt(i);
            if (ch == endChar) {
                if (cur.length() != 0) {
                    commandTemplate.add(cur.toString());
                }
                cur.setLength(0);
                endChar = DEFAULT_END_CHAR;
            } else if (ch == '\"' || ch == '\'') {
                endChar = ch;
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() != 0) {
            commandTemplate.add(cur.toString());
        }
        return ImmutableCommandTemplate.builder().addAllCommandTemplate(commandTemplate).build();
    }

    @Override
    public String getDefaultMetaVariable() {
        return Messages.DEFAULT_META_STRING_ARRAY_OPTION_HANDLER.format();
    }
}
