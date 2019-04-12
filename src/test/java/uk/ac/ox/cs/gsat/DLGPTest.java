package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.App;

/**
 * Unit tests for the GSat class from the ISG Ontology Repository
 * http://www.cs.ox.ac.uk/isg/ontologies
 * 
 * @author Stefano
 */
public class DLGPTest {

	private static final String baseChaseBench = "test" + File.separator + "DLGP" + File.separator;

	@Test
	public void animals() {

		String path = baseChaseBench + "animals.dlp";

		assertEquals(16, App.fromDLGP(path));

	}

	@Test
	public void A() {

		String path = baseChaseBench + "A.dlp";

		assertEquals(76, App.fromDLGP(path));

	}

}
