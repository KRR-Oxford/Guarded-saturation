package uk.ac.ox.cs;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import uk.ac.ox.cs.gsat.App;

/**
 * Unit tests for the GSat class from the ISG Ontology Repository
 * http://www.cs.ox.ac.uk/isg/ontologies
 * 
 * @author Stefano
 */
public class OWLTest {

	private static final String baseChaseBench = "test" + File.separator + "OWL" + File.separator;

	// @Test
	public void galenModule1NoFunctionality() {

		String path = baseChaseBench + "00033.owl";

		int executeChaseBenchScenario = App.fromOWL(path);

		assertEquals(12, executeChaseBenchScenario);

	}

	@Test
	public void vicodiTimeDeleted() {

		String path = baseChaseBench + "00780.owl";

		int executeChaseBenchScenario = App.fromOWL(path);

		assertEquals(223, executeChaseBenchScenario);

	}

	// @Test
	public void GardinerCorpusHttp___www_daml_ecs_soton_ac_uk_ont_currency_daml() {

		String path = baseChaseBench + "00198.owl";

		int executeChaseBenchScenario = App.fromOWL(path);

		assertEquals(12, executeChaseBenchScenario);

	}

}
