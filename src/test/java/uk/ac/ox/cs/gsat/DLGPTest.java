package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
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

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		App.logger.addHandler(handlerObj);
		App.logger.setLevel(Level.WARNING);
		App.logger.setUseParentHandlers(false);
	}

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
