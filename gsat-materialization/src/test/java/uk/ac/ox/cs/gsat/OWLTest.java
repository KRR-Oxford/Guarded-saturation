package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.api.SaturationProcess;

/**
 * Unit tests for the GSat class from the ISG Ontology Repository
 * http://www.cs.ox.ac.uk/isg/ontologies
 * 
 * @author Stefano
 */
public class OWLTest {

	private static final String baseOWL = "test" + File.separator + "OWL" + File.separator;
    private static final SaturationProcess saturationProcess = new CoreSaturationProcess(
            new SaturationProcessConfiguration());

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		Log.GLOBAL.addHandler(handlerObj);
		Log.GLOBAL.setLevel(Level.WARNING);
		Log.GLOBAL.setUseParentHandlers(false);

	}

	@Disabled("Disabled because it is too slow")
	@Test
	public void galenModule1NoFunctionality() throws Exception {

		String path = baseOWL + "00033.owl";

		assertEquals(12, saturationProcess.saturate(path).size());

	}

	@Test
	public void vicodiTimeDeleted() throws Exception {

		String path = baseOWL + "00780.owl";

		assertEquals(223, saturationProcess.saturate(path).size());

	}

	@Test
	public void GardinerCorpusHttp___www_daml_ecs_soton_ac_uk_ont_currency_daml() throws Exception {

		String path = baseOWL + "00198.owl";

		assertEquals(85, saturationProcess.saturate(path).size());

	}

	@Test
	public void GardinerCorpus_http___protege_stanford_edu_plugins_owl_owl_library_travel_owl_2009_02_13() throws Exception {

		String path = baseOWL + "00120.owl";

		assertEquals(61, saturationProcess.saturate(path).size());

	}

}
