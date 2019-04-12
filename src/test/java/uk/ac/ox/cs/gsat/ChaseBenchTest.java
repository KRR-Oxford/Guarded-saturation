package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.App;
import uk.ac.ox.cs.gsat.SolverOutput;

/**
 * Unit tests for the GSat class from ChaseBench scenarios
 * 
 * @author Stefano
 */
@DisplayName("Tests from ChaseBench scenarios")
public class ChaseBenchTest {

        private static final String baseChaseBench = "test" + File.separator + "ChaseBench" + File.separator
                        + "scenarios" + File.separator;

        @Test
        @DisplayName("Correctness tests")
        public void correctness() {

                String fact_querySize = "";

                int[] output_vals = { 145, 200, 326, 2821, 0, 18 };
                int[] errors_vals = { 0, 0, 0, 0, 0, 0 };
                int count = 0;
                for (String baseTest : new String[] { "tgds", "tgds5", "tgdsEgds", "tgdsEgdsLarge", "vldb2010",
                                "weak" }) {

                        String basePath = baseChaseBench + "correctness" + File.separator + baseTest + File.separator;

                        SolverOutput executeChaseBenchScenario = App.executeChaseBenchScenario(baseTest, basePath,
                                        fact_querySize, true);

                        assertEquals(output_vals[count], executeChaseBenchScenario.getOutput().length());
                        assertEquals(errors_vals[count], executeChaseBenchScenario.getErrors().length());

                        count++;

                }

        }

        @Test
        @DisplayName("Testing Deep 100")
        public void deep_100() {

                String baseTest = "deep";

                String basePath = baseChaseBench + baseTest + File.separator + "100" + File.separator;

                String fact_querySize = "";

                SolverOutput executeChaseBenchScenario = App.executeChaseBenchScenario(baseTest, basePath,
                                fact_querySize, true);

                assertEquals(1833, executeChaseBenchScenario.getOutput().length());
                assertEquals(0, executeChaseBenchScenario.getErrors().length());

        }

        @Test
        @DisplayName("Testing Doctors 10k")
        public void doctors_10k() {

                String baseTest = "doctors";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "10k";

                SolverOutput executeChaseBenchScenario = App.executeChaseBenchScenario(baseTest, basePath,
                                fact_querySize, true);

                assertEquals(49690, executeChaseBenchScenario.getOutput().length());
                assertEquals(0, executeChaseBenchScenario.getErrors().length());

        }

        @Test
        @DisplayName("Testing LUBM 001")
        public void LUBM_001() {

                String baseTest = "LUBM";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "";

                SolverOutput executeChaseBenchScenario = App.executeChaseBenchScenario(baseTest, basePath,
                                fact_querySize, true);

                assertEquals(12306747, executeChaseBenchScenario.getOutput().length());
                assertEquals(0, executeChaseBenchScenario.getErrors().length());

        }

}