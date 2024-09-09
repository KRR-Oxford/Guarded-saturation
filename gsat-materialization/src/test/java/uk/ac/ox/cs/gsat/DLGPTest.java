package uk.ac.ox.cs.gsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ox.cs.gsat.api.SaturationProcess;
import uk.ac.ox.cs.gsat.fol.TGD;

/**
 * Unit tests for the GSat class from the ISG Ontology Repository
 * http://www.cs.ox.ac.uk/isg/ontologies
 * 
 * @author Stefano
 */
public class DLGPTest {

	private static final String baseChaseBench = "test" + File.separator + "DLGP" + File.separator;
    private static final SaturationProcessConfiguration config = new SaturationProcessConfiguration();
    private static final SaturationProcess saturationProcess = new CoreSaturationProcess(config);

	@BeforeAll
	static void initAll() {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.WARNING);
		Log.GLOBAL.addHandler(handlerObj);
		Log.GLOBAL.setLevel(Level.WARNING);
		Log.GLOBAL.setUseParentHandlers(false);

	}

	@Test
	public void animals() throws Exception {

		String path = baseChaseBench + "animals.dlp";

		assertEquals(16, saturationProcess.saturate(path).size());

	}

	@Test
	public void A() throws Exception {

		String path = baseChaseBench + "A.dlp";
        config.setSubsumptionMethod("disabled");
        Collection<? extends TGD> sat = saturationProcess.saturate(path);

        
		assertEquals(76, sat.size());

	}

	@Test
	public void IMDB() throws Exception {

		String path = baseChaseBench + "imdb.dlgp";

		assertEquals(88, saturationProcess.saturate(path).size());
	}

}
