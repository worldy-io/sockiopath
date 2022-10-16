package io.worldy.sockiopath.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OptionsTest {

    @Test
    void parseHelpTest() {
        String[] args = {"-help"};
        HelpFormatter helpFormatter = Mockito.mock(HelpFormatter.class);
        Options.parse(args, helpFormatter);
        Mockito.verify(helpFormatter, Mockito.times(1)).printHelp("Sockiopath [WebSocket and UDP client/service]", Options.DEFAULT_OPTIONS);
    }

    @Test
    void parseBadInputTest() {
        org.apache.commons.cli.Options requiredOptions = new org.apache.commons.cli.Options();
        Option requiredOption = new Option(
                "req",
                "requiredOption",
                true,
                "A mocked broken option."
        );
        requiredOption.setRequired(true);
        requiredOptions.addOption(requiredOption);


        String[] args = {};
        HelpFormatter helpFormatter = Mockito.mock(HelpFormatter.class);
        Options.parse(args, helpFormatter, requiredOptions);
        Mockito.verify(helpFormatter, Mockito.times(1))
                .printHelp("Sockiopath [WebSocket and UDP client/service]", requiredOptions);
    }
}