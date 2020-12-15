package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the GSat class from the ISG Ontology Repository
 * http://www.cs.ox.ac.uk/isg/ontologies
 * 
 * @author Stefano
 */
public class OWLTest {

	private static final String baseChaseBench = "test" + File.separator + "OWL" + File.separator;

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		App.logger.addHandler(handlerObj);
		App.logger.setLevel(Level.WARNING);
		App.logger.setUseParentHandlers(false);
	}

	@Disabled("Disabled because it is too slow")
	@Test
	public void galenModule1NoFunctionality() {

		String path = baseChaseBench + "00033.owl";

		ExecutionOutput executeChaseBenchScenario = App.fromOWL(path, "");

		assertEquals(12, executeChaseBenchScenario.getGuardedSaturation().size());

	}

	@Test
	public void vicodiTimeDeleted() {

		String path = baseChaseBench + "00780.owl";

		ExecutionOutput executeChaseBenchScenario = App.fromOWL(path, "");

		assertEquals(223, executeChaseBenchScenario.getGuardedSaturation().size());

	}

	@Test
	public void GardinerCorpusHttp___www_daml_ecs_soton_ac_uk_ont_currency_daml() {

		String path = baseChaseBench + "00198.owl";

		ExecutionOutput executeChaseBenchScenario = App.fromOWL(path, "");

		assertEquals(86, executeChaseBenchScenario.getGuardedSaturation().size());

	}

	@Test
	public void GardinerCorpus_http___protege_stanford_edu_plugins_owl_owl_library_travel_owl_2009_02_13() {

		String path = baseChaseBench + "00120.owl";

		ExecutionOutput executeChaseBenchScenario = App.fromOWL(path, "");

		assertEquals(72, executeChaseBenchScenario.getGuardedSaturation().size());

	}

}
