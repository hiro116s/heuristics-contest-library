package hiro116s.simulator.option;

import com.google.common.collect.ImmutableList;
import hiro116s.simulator.model.CommandTemplate;
import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandTemplateOptionHandlerTest {
    private static class TestBean {
        @Option(name = "--opt", handler = CommandTemplateOptionHandler.class)
        private CommandTemplate commandTemplate = null;

        @Argument
        private String rest[];
    }

    @Test
    void testParseArguments_noOpt() throws CmdLineException {
        final TestBean bean = new TestBean();
        final CmdLineParser parser = new CmdLineParser(bean);
        parser.parseArgument("test1", "test2", "test3");

        assertNull(bean.commandTemplate);
        assertEquals(Arrays.asList("test1", "test2", "test3"), Arrays.asList(bean.rest));
    }

    @Test
    void testParseArguments_optOk() throws CmdLineException {
        final TestBean bean = new TestBean();
        final CmdLineParser parser = new CmdLineParser(bean);
        parser.parseArgument("other1", "--opt", "java X --exec 'java Y' --seed $SEED", "other2");

        assertEquals(
                ImmutableList.of("java", "X", "--exec", "java Y", "--seed", "1"),
                bean.commandTemplate.build(1));
        assertEquals(Arrays.asList("other1", "other2"), Arrays.asList(bean.rest));
    }

    @Test
    void testParseArguments_optOk2() throws CmdLineException {
        final TestBean bean = new TestBean();
        final CmdLineParser parser = new CmdLineParser(bean);
        parser.parseArgument("other1", "--opt", "java X --exec \"java Y\" --seed $SEED", "other2");

        assertEquals(
                ImmutableList.of("java", "X", "--exec", "java Y", "--seed", "1"),
                bean.commandTemplate.build(1));
        assertEquals(Arrays.asList("other1", "other2"), Arrays.asList(bean.rest));
    }

    @Test
    void testParseArguments_optOk3() throws CmdLineException {
        final TestBean bean = new TestBean();
        final CmdLineParser parser = new CmdLineParser(bean);
        parser.parseArgument("other1", "--opt", "java X --exec java Y --seed $SEED", "other2");

        assertEquals(
                ImmutableList.of("java", "X", "--exec", "java", "Y", "--seed", "4"),
                bean.commandTemplate.build(4));
        assertEquals(Arrays.asList("other1", "other2"), Arrays.asList(bean.rest));
    }
}