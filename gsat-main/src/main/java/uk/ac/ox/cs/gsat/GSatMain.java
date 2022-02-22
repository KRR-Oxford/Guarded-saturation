package uk.ac.ox.cs.gsat;

import java.util.Arrays;

import uk.ac.ox.cs.gsat.regression.Regression;

/**
 * Main entry point for GSat
 *
 */
public class GSatMain {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            printHelp();
        }
        else {
            String[] new_args = Arrays.copyOfRange(args, 1, args.length);
            switch (args[0]) {
            case "saturation":
                Saturator.main(new_args);
                break;
            case "materialization":
                Materialization.main(new_args);
                break;
            case "regression":
                Regression.main(new_args);
                break;
            default:
                printHelp();
                break;
            }
        }

    }

    private static void printHelp() {
        String sb = "Usage: GSatMain [action] [args]\n" +
            "\twhere action is one of:\n" +
            "\t\tsaturation\n" +
            "\t\tmaterialization\n" +
            "\t\tregression\n" +
            "\tand args are arguments for the selected action\n";
        System.out.println(sb);
    }

}
