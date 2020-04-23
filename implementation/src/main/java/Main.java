import Configuration.ConfigCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import static java.lang.System.exit;

public class Main {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        int exitcode = new CommandLine(new ConfigCommand()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        exit(exitcode);
    }
}
