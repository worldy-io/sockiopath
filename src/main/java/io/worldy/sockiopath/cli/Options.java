package io.worldy.sockiopath.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import java.util.Optional;

record Options(
        boolean server,
        boolean client,
        String webSocketHost,
        int webSocketPort,
        String udpHost,
        int udpPort
) {
    static Options parse(String[] args) {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("help", "help", false, "print this message");
        options.addOption(OPTION_WEB_SOCKET_PORT);
        options.addOption(OPTION_WEB_SOCKET_PORT);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                formatter.printHelp(HELP_MESSAGE, options);
                System.exit(0);
            }

            boolean serverMode = Optional.ofNullable(
                    cmd.getOptionValue(OPTION_SERVER_MODE.getLongOpt())
            ).map(Boolean::parseBoolean).orElse(true);

            boolean clientMode = Optional.ofNullable(
                    cmd.getOptionValue(OPTION_SERVER_MODE.getLongOpt())
            ).map(Boolean::parseBoolean).orElse(true);

            String host = Optional.ofNullable(
                    cmd.getOptionValue(OPTION_HOST.getLongOpt())
            ).orElse(DEFAULT_HOST);

            String webSocketHost = Optional.ofNullable(
                    cmd.getOptionValue(OPTION_WEB_SOCKET_HOST.getLongOpt())
            ).orElse(host);

            String udpHost = Optional.ofNullable(
                    cmd.getOptionValue(OPTION_UPD_HOST.getLongOpt())
            ).orElse(host);

            int webSocketPort = Optional.ofNullable(
                            cmd.getOptionValue(OPTION_WEB_SOCKET_PORT.getLongOpt()))
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_WEB_SOCKET_PORT);

            int udpPort = Optional.ofNullable(
                            cmd.getOptionValue(OPTION_UDP_PORT.getLongOpt()))
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_UDP_PORT);

            return new Options(serverMode, clientMode, webSocketHost, webSocketPort, udpHost, udpPort);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(HELP_MESSAGE, options);
            System.exit(1);
        }
        return null;
    }

    private static String DEFAULT_HOST = "localhost";

    private static int DEFAULT_WEB_SOCKET_PORT = 4200;
    private static int DEFAULT_UDP_PORT = 4242;

    private static final String HELP_MESSAGE = "Sockiopath [WebSocket and UDP client/service]";

    private static final Option OPTION_SERVER_MODE = new Option(
            "s",
            "server",
            true,
            "Flag to turn on/off server mode. The default on."
    );

    private static final Option OPTION_CLIENT_MODE = new Option(
            "c",
            "client",
            true,
            "Flag to turn on/off client mode. The default off."
    );

    private static final Option OPTION_UDP_PORT = new Option(
            "udpPort",
            "udpPort",
            true,
            "The port to use for UDP.  Default: " + DEFAULT_UDP_PORT
    );

    private static final Option OPTION_WEB_SOCKET_PORT = new Option(
            "wsPort",
            "webSocketPort",
            true,
            "The port to use for WebSockets.  Default: " + DEFAULT_WEB_SOCKET_PORT
    );

    private static final Option OPTION_HOST = new Option(
            "h",
            "host",
            true,
            "The host to use for WebSockets and UDP.  Default: " + DEFAULT_HOST
    );

    private static final Option OPTION_UPD_HOST = new Option(
            "udpHost",
            "udpHost",
            true,
            "The host to use for UDP.  Default: " + DEFAULT_HOST
    );

    private static final Option OPTION_WEB_SOCKET_HOST = new Option(
            "wsHost",
            "webSocketHost",
            true,
            "The host to use for WebSockets.  Default: " + DEFAULT_HOST
    );
}
