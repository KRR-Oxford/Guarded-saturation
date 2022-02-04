package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the GSat class from ChaseBench scenarios
 * 
 * @author Stefano
 */
@DisplayName("Tests from ChaseBench scenarios")
public class ChaseBenchTest {

        private static final String baseChaseBench = "test" + File.separator + "ChaseBench" + File.separator
                        + "scenarios" + File.separator;

        @BeforeAll
        static void initAll() {
                Handler handlerObj = new ConsoleHandler();
                handlerObj.setLevel(Level.WARNING);
                App.logger.addHandler(handlerObj);
                App.logger.setLevel(Level.WARNING);
                App.logger.setUseParentHandlers(false);

        }

        private void fromChaseBench(String baseTest, String basePath, String fact_querySize, int output_val,
                        int errors_val, int lines_val) {

                ExecutionOutput executeChaseBenchScenario = App.executeChaseBenchScenario(baseTest, basePath,
                                fact_querySize);

                if (!Configuration.isSaturationOnly()) {
                        assertEquals(output_val, executeChaseBenchScenario.getSolverOutput().getOutput().length());
                        assertEquals(errors_val, executeChaseBenchScenario.getSolverOutput().getErrors().length());
                        assertEquals(lines_val, executeChaseBenchScenario.getSolverOutput().getNumberOfLinesOutput());
                }

        }

        @Test
        @DisplayName("Correctness tests")
        public void correctness() {

                String fact_querySize = "";

                int[] output_vals = { 145, 200, 326, 2821, 0, 18 };
                int[] errors_vals = { 0, 0, 0, 0, 0, 0 };
                int[] lines_vals = { 7, 10, 14, 163, 1, 1 };
                int count = 0;
                for (String baseTest : new String[] { "tgds", "tgds5", "tgdsEgds", "tgdsEgdsLarge", "vldb2010",
                                "weak" }) {

                        String basePath = baseChaseBench + "correctness" + File.separator + baseTest + File.separator;

                        fromChaseBench(baseTest, basePath, fact_querySize, output_vals[count], errors_vals[count],
                                        lines_vals[count]);

                        count++;

                }

        }

        @Test
        @DisplayName("Testing Deep 100")
        public void deep_100() {

                String baseTest = "deep";

                String basePath = baseChaseBench + baseTest + File.separator + "100" + File.separator;

                String fact_querySize = "";

                fromChaseBench(baseTest, basePath, fact_querySize, 1833, 0, 62);

        }

        @Test
        @DisplayName("Testing Doctors 10k")
        public void doctors_10k() {

                String baseTest = "doctors";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "10k";

                fromChaseBench(baseTest, basePath, fact_querySize, 49690, 0, 837);

        }

        @Test
        @DisplayName("Testing LUBM 001")
        public void LUBM_001() {

                String baseTest = "LUBM";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "";

                fromChaseBench(baseTest, basePath, fact_querySize, 11501995, 0, 138254);

        }

}
