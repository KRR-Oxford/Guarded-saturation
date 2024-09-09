package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.api.Materializer;
import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.api.io.Parser;
import uk.ac.ox.cs.gsat.api.io.ParserResult;
import uk.ac.ox.cs.gsat.fol.TGD;
import uk.ac.ox.cs.gsat.io.ParserFactory;
import uk.ac.ox.cs.gsat.mat.MaterializerFactory;

/**
 * Unit tests for the GSat class from ChaseBench scenarios
 * 
 * @author Stefano
 */
@DisplayName("Tests from ChaseBench scenarios")
public class ChaseBenchTest {

        private static final String baseChaseBench = "test" + File.separator + "ChaseBench" + File.separator
                        + "scenarios" + File.separator;

            private static final SaturationProcessConfiguration satConfig = new SaturationProcessConfiguration();
        private static final MaterializationConfiguration matConfig = new MaterializationConfiguration();
        private static final String outputPath = ".output-test.txt";

        @BeforeAll
        static void initAll() {
                Handler handlerObj = new ConsoleHandler();
                handlerObj.setLevel(Level.WARNING);
                Log.GLOBAL.addHandler(handlerObj);
                Log.GLOBAL.setLevel(Level.WARNING);
                Log.GLOBAL.setUseParentHandlers(false);

        }

        private void fromChaseBench(String basePath, String fact_querySize, int lines_val) throws Exception {

            SaturationProcess saturationProcess = new CoreSaturationProcess(satConfig);
            Collection<? extends TGD> saturation = saturationProcess.saturate(basePath);
            Parser parser = ParserFactory.instance().create(TGDFileFormat.CHASE_BENCH, false, true);
            ParserResult parsedScenario = parser.parse(basePath);
            matConfig.setSolverOptionsGrounding("--t --no-facts --check-edb-duplication");
            Materializer materializer = MaterializerFactory.create(matConfig);
            materializer.init();
            long matSize = materializer.materialize(parsedScenario, saturation, outputPath);

            //assertEquals(output_val, executeChaseBenchScenario.getSolverOutput().getOutput().length());
            assertEquals(lines_val, matSize);
        }

        @Test
        @DisplayName("Correctness tests")
        public void correctness() throws Exception {

                String fact_querySize = "";

                int[] lines_vals = { 7, 10, 14, 163, 1, 1 };
                int count = 0;
                for (String baseTest : new String[] { "tgds", "tgds5", "tgdsEgds", "tgdsEgdsLarge", "vldb2010",
                                "weak" }) {

                        String basePath = baseChaseBench + "correctness" + File.separator + baseTest + File.separator;

                        fromChaseBench(basePath, fact_querySize, lines_vals[count]);

                        count++;

                }

        }

        @Test
        @DisplayName("Testing Deep 100")
        public void deep_100() throws Exception {

                String baseTest = "deep";

                String basePath = baseChaseBench + baseTest + File.separator + "100" + File.separator;

                String fact_querySize = "";

                fromChaseBench(basePath, fact_querySize, 62);

        }
    // TODO: require the support of fact size parameters for the ChaseBench parameters
    @Disabled
        @Test
        @DisplayName("Testing Doctors 10k")
        public void doctors_10k() throws Exception {

                String baseTest = "doctors";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "10k";

                fromChaseBench(basePath, fact_querySize, 837);

        }

        @Test
        @DisplayName("Testing LUBM 001")
        public void LUBM_001() throws Exception {

                String baseTest = "LUBM";

                String basePath = baseChaseBench + baseTest + File.separator;

                String fact_querySize = "";

                fromChaseBench(basePath, fact_querySize, 138254);

        }

}
